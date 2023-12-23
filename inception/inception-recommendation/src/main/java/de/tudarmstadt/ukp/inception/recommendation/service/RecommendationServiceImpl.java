/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.service;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AutoAcceptMode.ON_FIRST_ACCESS;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.AUTO_ACCEPT;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.CORRECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionCapability.PREDICTION_USES_TEXT_ONLY;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_NOT_SUPPORTED;
import static de.tudarmstadt.ukp.inception.recommendation.service.SuggestionExtraction.extractSuggestions;
import static de.tudarmstadt.ukp.inception.rendering.model.Range.rangeCoveringDocument;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.PageRequestHandlerTracker;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer_;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapter;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.event.AfterCasWrittenEvent;
import de.tudarmstadt.ukp.inception.documents.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.inception.documents.event.AfterDocumentResetEvent;
import de.tudarmstadt.ukp.inception.documents.event.BeforeDocumentRemovedEvent;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.api.event.AfterProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderTypeSystemUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AutoAcceptMode;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Progress;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender_;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderDeletedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderTaskNotificationEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderUpdatedEvent;
import de.tudarmstadt.ukp.inception.recommendation.model.DirtySpot;
import de.tudarmstadt.ukp.inception.recommendation.tasks.NonTrainableRecommenderActivationTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.PredictionTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.SelectionTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.TrainingTask;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.TaskMonitor;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.support.StopWatch;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.logging.LogMessageGroup;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

/**
 * The implementation of the RecommendationService.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RecommenderServiceAutoConfiguration#recommendationService}.
 * </p>
 */
public class RecommendationServiceImpl
    implements RecommendationService, LearningRecordService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TRAININGS_PER_SELECTION = 5;

    private static final String PREDICTION_CAS = "predictionCas";

    private final EntityManager entityManager;

    private final SessionRegistry sessionRegistry;
    private final UserDao userRepository;
    private final RecommenderFactoryRegistry recommenderFactoryRegistry;
    private final SchedulingService schedulingService;
    private final AnnotationSchemaService schemaService;
    private final DocumentService documentService;
    private final ProjectService projectService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PreferencesService preferencesService;
    private final SuggestionSupportRegistry layerRecommendtionSupportRegistry;

    private final ConcurrentMap<RecommendationStateKey, AtomicInteger> trainingTaskCounter;
    private final ConcurrentMap<RecommendationStateKey, RecommendationState> states;

    /*
     * Marks users/projects to which annotations were added during this request.
     */
    @SuppressWarnings("serial")
    private static final MetaDataKey<Set<DirtySpot>> DIRTIES = //
            new MetaDataKey<Set<DirtySpot>>()
            {
            };

    /*
     * Marks for which CASes have been saved during this request (probably the ones to which
     * annotations have been added above).
     */
    @SuppressWarnings("serial")
    private static final MetaDataKey<Set<CommittedDocument>> COMMITTED = new MetaDataKey<>()
    {
    };

    @Autowired
    public RecommendationServiceImpl(PreferencesService aPreferencesService,
            SessionRegistry aSessionRegistry, UserDao aUserRepository,
            RecommenderFactoryRegistry aRecommenderFactoryRegistry,
            SchedulingService aSchedulingService, AnnotationSchemaService aAnnoService,
            DocumentService aDocumentService, ProjectService aProjectService,
            EntityManager aEntityManager, ApplicationEventPublisher aApplicationEventPublisher,
            SuggestionSupportRegistry aLayerRecommendtionSupportRegistry)
    {
        preferencesService = aPreferencesService;
        sessionRegistry = aSessionRegistry;
        userRepository = aUserRepository;
        recommenderFactoryRegistry = aRecommenderFactoryRegistry;
        schedulingService = aSchedulingService;
        schemaService = aAnnoService;
        documentService = aDocumentService;
        projectService = aProjectService;
        entityManager = aEntityManager;
        applicationEventPublisher = aApplicationEventPublisher;
        layerRecommendtionSupportRegistry = aLayerRecommendtionSupportRegistry;

        trainingTaskCounter = new ConcurrentHashMap<>();
        states = new ConcurrentHashMap<>();
    }

    public RecommendationServiceImpl(PreferencesService aPreferencesService,
            SessionRegistry aSessionRegistry, UserDao aUserRepository,
            RecommenderFactoryRegistry aRecommenderFactoryRegistry,
            SchedulingService aSchedulingService, AnnotationSchemaService aAnnoService,
            DocumentService aDocumentService,
            SuggestionSupportRegistry aLayerRecommendtionSupportRegistry,
            EntityManager aEntityManager)
    {
        this(aPreferencesService, aSessionRegistry, aUserRepository, aRecommenderFactoryRegistry,
                aSchedulingService, aAnnoService, aDocumentService, (ProjectService) null,
                aEntityManager, null, aLayerRecommendtionSupportRegistry);
    }

    @Override
    public Predictions getPredictions(User aUser, Project aProject)
    {
        var state = getState(aUser.getUsername(), aProject);
        return state.getActivePredictions();
    }

    @Override
    public Predictions getIncomingPredictions(User aUser, Project aProject)
    {
        var state = getState(aUser.getUsername(), aProject);
        return state.getIncomingPredictions();
    }

    @Override
    public void putIncomingPredictions(User aUser, Project aProject, Predictions aPredictions)
    {
        var state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            state.setIncomingPredictions(aPredictions);
        }
    }

    @Override
    public boolean hasActiveRecommenders(String aUser, Project aProject)
    {
        var state = getState(aUser, aProject);
        synchronized (state) {
            return !state.getActiveRecommenders().isEmpty();
        }
    }

    @Override
    public void setEvaluatedRecommenders(User aUser, AnnotationLayer aLayer,
            List<EvaluatedRecommender> aRecommenders)
    {
        var state = getState(aUser.getUsername(), aLayer.getProject());
        synchronized (state) {
            state.setEvaluatedRecommenders(aLayer, aRecommenders);
        }
    }

    @Override
    public List<EvaluatedRecommender> getEvaluatedRecommenders(User aUser, AnnotationLayer aLayer)
    {
        var state = getState(aUser.getUsername(), aLayer.getProject());
        synchronized (state) {
            return new ArrayList<>(state.getEvaluatedRecommenders().get(aLayer));
        }
    }

    @Override
    public Optional<EvaluatedRecommender> getEvaluatedRecommender(User aUser,
            Recommender aRecommender)
    {
        var state = getState(aUser.getUsername(), aRecommender.getProject());
        synchronized (state) {
            return state.getEvaluatedRecommenders().get(aRecommender.getLayer()).stream()
                    .filter(r -> r.getRecommender().equals(aRecommender)).findAny();
        }
    }

    @Override
    public List<EvaluatedRecommender> getActiveRecommenders(User aUser, AnnotationLayer aLayer)
    {
        var state = getState(aUser.getUsername(), aLayer.getProject());
        synchronized (state) {
            return new ArrayList<>(state.getActiveRecommenders().get(aLayer));
        }
    }

    @Override
    public List<EvaluatedRecommender> getActiveRecommenders(User aUser, Project aProject)
    {
        var state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            return new ArrayList<>(state.getActiveRecommenders().values());
        }
    }

    @Override
    @Transactional
    public void createOrUpdateRecommender(Recommender aRecommender)
    {
        if (aRecommender.getId() == null) {
            entityManager.persist(aRecommender);
        }
        else {
            entityManager.merge(aRecommender);
        }

        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(new RecommenderUpdatedEvent(this, aRecommender));
        }
    }

    @Override
    @Transactional
    public void deleteRecommender(Recommender aRecommender)
    {
        var settings = aRecommender;

        if (!entityManager.contains(settings)) {
            settings = entityManager.merge(settings);
        }

        entityManager.remove(settings);

        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(new RecommenderDeletedEvent(this, aRecommender));
        }
    }

    @Override
    @Transactional
    public List<Recommender> listRecommenders(Project aProject)
    {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Recommender.class);
        var root = query.from(Recommender.class);

        query.select(root) //
                .where(cb.equal(root.get(Recommender_.project), aProject)) //
                .orderBy(cb.asc(root.get(Recommender_.name)));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<AnnotationLayer> listLayersWithEnabledRecommenders(Project aProject)
    {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(AnnotationLayer.class);
        var root = query.from(Recommender.class);

        var layerJoin = root.join(Recommender_.layer);
        query.select(layerJoin) //
                .distinct(true) //
                .where(cb.and( //
                        cb.equal(root.get(Recommender_.project), aProject), //
                        cb.equal(root.get(Recommender_.enabled), true))) //
                .orderBy(cb.asc(layerJoin.get(AnnotationLayer_.name)));

        var typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Recommender getRecommender(long aId)
    {
        return entityManager.find(Recommender.class, aId);
    }

    @Override
    public Recommender getRecommender(AnnotationSuggestion aSuggestion)
    {
        return getRecommender(aSuggestion.getVID().getId());
    }

    @Override
    @Transactional
    public boolean existsRecommender(Project aProject, String aName)
    {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(Recommender.class);

        query.select(cb.count(root)).where(cb.and( //
                cb.equal(root.get(Recommender_.name), aName), //
                cb.equal(root.get(Recommender_.project), aProject)));

        long count = entityManager.createQuery(query).getSingleResult();

        return count > 0;
    }

    @Override
    @Transactional
    public Optional<Recommender> getRecommender(Project aProject, String aName)
    {
        String query = String.join("\n", //
                "FROM Recommender ", //
                "WHERE name = :name ", //
                "AND project = :project");

        return entityManager.createQuery(query, Recommender.class) //
                .setParameter("name", aName) //
                .setParameter("project", aProject) //
                .getResultStream() //
                .findFirst();
    }

    @Override
    @Transactional
    public Optional<Recommender> getEnabledRecommender(long aRecommenderId)
    {
        String query = String.join("\n", //
                "FROM Recommender WHERE ", //
                "id = :id AND ", //
                "enabled = :enabled");

        return entityManager.createQuery(query, Recommender.class) //
                .setParameter("id", aRecommenderId) //
                .setParameter("enabled", true) //
                .getResultStream() //
                .findFirst();
    }

    @Override
    @Transactional
    public List<Recommender> listEnabledRecommenders(AnnotationLayer aLayer)
    {
        String query = String.join("\n", //
                "FROM Recommender WHERE ", //
                "project = :project AND", //
                "layer = :layer AND", //
                "enabled = :enabled", //
                "ORDER BY name ASC");

        return entityManager.createQuery(query, Recommender.class) //
                .setParameter("project", aLayer.getProject()) //
                .setParameter("layer", aLayer) //
                .setParameter("enabled", true) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<Recommender> listEnabledRecommenders(Project aProject)
    {
        String query = String.join("\n", //
                "FROM Recommender WHERE", //
                "project = :project AND", //
                "enabled = :enabled", //
                "ORDER BY name ASC");

        return entityManager.createQuery(query, Recommender.class) //
                .setParameter("project", aProject) //
                .setParameter("enabled", true) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<Recommender> listRecommenders(AnnotationLayer aLayer)
    {
        String query = String.join("\n", //
                "FROM Recommender WHERE ", //
                "layer = :layer", //
                "ORDER BY name ASC");

        return entityManager.createQuery(query, Recommender.class) //
                .setParameter("layer", aLayer) //
                .getResultList();
    }

    @EventListener
    public void onDocumentOpened(DocumentOpenedEvent aEvent)
    {
        var project = aEvent.getDocument().getProject();
        var sessionOwnerName = aEvent.getSessionOwner();
        var dataOwner = aEvent.getDocumentOwner();
        var doc = aEvent.getDocument();
        var predictions = getState(sessionOwnerName, project).getActivePredictions();

        var sessionOwner = userRepository.get(sessionOwnerName);
        if (sessionOwner == null) {
            return;
        }

        var predictionSessionExistedOnOpen = false;
        if (predictions != null) {
            if (predictions.getDataOwner().equals(dataOwner)) {
                predictionSessionExistedOnOpen = true;
            }
            else {
                // If the session owner has switched the data they are looking at, we need to
                // clear and rebuild the predictions.
                clearState(sessionOwnerName);
            }
        }

        // We want to get predictions from all trained recommenders immediately - be they externally
        // pre-trained or possibly internal recommenders that have been trained due to earlier
        // actions.
        var trigger = aEvent.getClass().getSimpleName();
        if (!predictionSessionExistedOnOpen) {
            // Activate all non-trainable recommenders - execute synchronously - blocking
            schedulingService.executeSync(
                    new NonTrainableRecommenderActivationTask(sessionOwner, project, trigger));
        }

        // Check if we need to wait for the initial recommender run before displaying the document
        // to the user
        var predictionTriggered = nonTrainableRecommenderRunSync(doc, predictions, sessionOwner,
                trigger, dataOwner);

        // Is it the first time a document has been opened? If yes, there might be auto-accept
        // suggestions that need to be processed (in particular ones that may have been generated
        // by the non-trainable recommenders triggered above or from already existing predictions
        if (aEvent.getStateBeforeOpening() == AnnotationDocumentState.NEW) {
            autoAccept(aEvent.getRequestTarget().orElse(null), sessionOwner, doc, ON_FIRST_ACCESS);
        }

        // Trigger a training and prediction run if there is no prediction state yet
        if (!predictionSessionExistedOnOpen) {
            triggerTrainingAndPrediction(sessionOwnerName, project, trigger, doc, dataOwner);
            return;
        }

        if (predictions != null && predictions.hasRunPredictionOnDocument(aEvent.getDocument())) {
            LOG.debug(
                    "Not scheduling prediction task after document was opened as we already have predictions");
            return;
        }

        // If we already trained, predicted only for the last document and open a new document, we
        // start the predictions so that the user gets recommendations as quickly as possible
        // without any interaction needed
        if (!predictionTriggered) {
            triggerPrediction(sessionOwnerName, trigger, doc, dataOwner);
        }
    }

    private boolean nonTrainableRecommenderRunSync(SourceDocument doc, Predictions predictions,
            User aSessionOwner, String trigger, String aDataOwner)
    {
        if (predictions != null && predictions.hasRunPredictionOnDocument(doc)) {
            LOG.trace("Not running sync prediction for non-trainable recommenders as we already "
                    + "have predictions");
            return false;
        }

        var settings = preferencesService
                .loadDefaultTraitsForProject(KEY_RECOMMENDER_GENERAL_SETTINGS, doc.getProject());
        if (!settings.isWaitForRecommendersOnOpenDocument()) {
            LOG.trace("Not running sync prediction for non-trainable recommenders because the "
                    + "option is not enabled in the project settings");
            return false;
        }

        LOG.trace("Running sync prediction for non-trainable recommenders");
        schedulingService.executeSync(new PredictionTask(aSessionOwner, trigger, doc, aDataOwner));
        switchPredictions(aSessionOwner.getUsername(), doc.getProject());

        return true;
    }

    private void autoAccept(AjaxRequestTarget aTarget, User aUser, SourceDocument aDocument,
            AutoAcceptMode aAutoAcceptMode)
    {
        if (aTarget == null) {
            LOG.trace("Not auto-accepting outside AJAX requests");
            return;
        }

        if (!(aTarget.getPage() instanceof AnnotationPage)) {
            LOG.trace("Not auto-accepting when not triggered through AnnotationPage");
            return;
        }

        var page = (AnnotationPage) aTarget.getPage();

        var predictions = getPredictions(aUser, aDocument.getProject());
        if (predictions == null || predictions.isEmpty()) {
            LOG.trace("Not auto-accepting because no predictions are available");
            return;
        }

        if (!page.isEditable()) {
            return;
        }

        CAS cas;
        try {
            cas = page.getEditorCas();
        }
        catch (IOException e) {
            LOG.error("Not auto-accepting because editor CAS could not be loaded", e);
            return;
        }

        var count = 0;
        for (var prediction : predictions.getPredictionsByDocument(aDocument.getName())) {
            if (prediction.getAutoAcceptMode() != aAutoAcceptMode) {
                continue;
            }

            // We do not clear auto-accept for on-first-access predictions because these should be
            // restored after a document reset but they won't be re-generated after a document
            // reset.
            if (prediction.getAutoAcceptMode() != AutoAcceptMode.ON_FIRST_ACCESS) {
                prediction.clearAutoAccept();
            }

            var layer = schemaService.getLayer(prediction.getLayerId());
            var feature = schemaService.getFeature(prediction.getFeature(), layer);
            var adapter = schemaService.getAdapter(layer);
            adapter.silenceEvents();

            try {
                var rls = layerRecommendtionSupportRegistry.findGenericExtension(prediction);
                if (rls.isPresent()) {
                    rls.get().acceptSuggestion(null, aDocument, aUser.getUsername(), cas, adapter,
                            feature, prediction, AUTO_ACCEPT, ACCEPTED);
                    rls.get().acceptSuggestion(null, aDocument, aUser.getUsername(), cas,
                            (SpanAdapter) adapter, feature, prediction, AUTO_ACCEPT, ACCEPTED);
                    count++;
                }
            }
            catch (AnnotationException e) {
                LOG.debug("Not auto-accepting suggestion: {}", e.getMessage());
            }
        }

        predictions.log(LogMessage.info(this, "Auto-accepted [%d] suggestions", count));
        LOG.debug("Auto-accepted [{}] suggestions", count);

        if (count > 0) {
            try {
                page.writeEditorCas(cas);
            }
            catch (Exception e) {
                WicketExceptionUtil.handleException(LOG, page, aTarget, e);
            }
        }
    }

    /*
     * There can be multiple annotation changes in a single user request. Thus, we do not trigger a
     * training on every action but rather mark the project/user as dirty and trigger the training
     * only when we get a CAS-written event on a dirty project/user.
     */
    @EventListener
    public void onAnnotation(AnnotationEvent aEvent)
    {
        var requestCycle = RequestCycle.get();

        if (requestCycle == null) {
            return;
        }

        var dirties = requestCycle.getMetaData(DIRTIES);
        if (dirties == null) {
            dirties = new HashSet<>();
            requestCycle.setMetaData(DIRTIES, dirties);
        }

        dirties.add(new DirtySpot(aEvent));
    }

    /*
     * We only want to schedule training runs as a reaction to the user performing an action. We
     * don't need to keep training all the time if the user isn't even going to look at the results.
     * Thus, we make use of the Wicket RequestCycle here.
     */
    @EventListener
    public void onAfterCasWritten(AfterCasWrittenEvent aEvent)
    {
        var requestCycle = RequestCycle.get();

        if (requestCycle == null) {
            return;
        }

        if (!existsEnabledRecommender(aEvent.getDocument().getProject())) {
            return;
        }

        var committed = requestCycle.getMetaData(COMMITTED);
        if (committed == null) {
            committed = new HashSet<>();
            requestCycle.setMetaData(COMMITTED, committed);
        }

        var annDoc = aEvent.getDocument();
        committed.add(new CommittedDocument(annDoc));

        var containsTrainingTrigger = false;
        for (var listener : requestCycle.getListeners()) {
            if (listener instanceof TriggerTrainingTaskListener) {
                containsTrainingTrigger = true;
            }
        }

        if (!containsTrainingTrigger) {
            // Hack to figure out which annotations the user is viewing. This obviously works only
            // if the user is viewing annotations through an AnnotationPageBase ... still not a
            // bad guess
            var handler = PageRequestHandlerTracker.getLastHandler(requestCycle);
            if (handler.isPageInstanceCreated()
                    && handler.getPage() instanceof AnnotationPageBase) {
                var state = ((AnnotationPageBase) handler.getPage()).getModelObject();
                requestCycle.getListeners().add(new TriggerTrainingTaskListener(state.getDocument(),
                        state.getUser().getUsername()));
            }
            else {
                // Otherwise use the document from the event... mind that if there are multiple
                // events, we consider only the first one since after that the trigger listener
                // will be in the cycle and we do not add another one.
                // FIXME: This works as long as the user is working on a single document, but not if
                // the user is doing a bulk operation. If a bulk-operation is done, we get multiple
                // AfterCasWrittenEvent and we do not know which of them belongs to the document
                // which the user is currently viewing.
                requestCycle.getListeners().add(
                        new TriggerTrainingTaskListener(annDoc.getDocument(), annDoc.getUser()));
            }
        }
    }

    @EventListener
    public void onRecommenderUpdated(RecommenderUpdatedEvent aEvent)
    {
        clearState(aEvent.getRecommender().getProject());
    }

    @EventListener
    public void onRecommenderDelete(RecommenderDeletedEvent aEvent)
    {
        // When removing a recommender, it is sufficient to delete its predictions from the current
        // state. Since (so far) recommenders do not depend on each other, we wouldn't need to
        // trigger a training rung.
        removePredictions(aEvent.getRecommender());
    }

    @EventListener
    public void onDocumentCreated(AfterDocumentCreatedEvent aEvent)
    {
        clearState(aEvent.getDocument().getProject());
    }

    @EventListener
    public void onDocumentRemoval(BeforeDocumentRemovedEvent aEvent)
    {
        clearState(aEvent.getDocument().getProject());
    }

    @EventListener
    public void onBeforeProjectRemoved(BeforeProjectRemovedEvent aEvent)
    {
        clearState(aEvent.getProject());
    }

    @EventListener
    public void onAfterProjectRemoved(AfterProjectRemovedEvent aEvent)
    {
        clearState(aEvent.getProject());
    }

    @EventListener
    public void onLayerConfigurationChangedEvent(LayerConfigurationChangedEvent aEvent)
    {
        clearState(aEvent.getProject());
    }

    @Override
    public void triggerPrediction(String aUsername, String aEventName, SourceDocument aDocument,
            String aDataOwner)
    {
        var user = userRepository.get(aUsername);

        if (user == null) {
            return;
        }

        schedulingService.enqueue(new PredictionTask(user, aEventName, aDocument, aDataOwner));
    }

    @Override
    public void triggerTrainingAndPrediction(String aSessionOwner, Project aProject,
            String aEventName, SourceDocument aCurrentDocument, String aDataOwner)
    {
        triggerTraining(aSessionOwner, aProject, aEventName, aCurrentDocument, aDataOwner, false,
                null);
    }

    @Override
    public void triggerSelectionTrainingAndPrediction(String aSessionOwner, Project aProject,
            String aEventName, SourceDocument aCurrentDocument, String aDataOwner)
    {
        triggerTraining(aSessionOwner, aProject, aEventName, aCurrentDocument, aDataOwner, true,
                null);
    }

    private void triggerTraining(String aSessionOwner, Project aProject, String aEventName,
            SourceDocument aCurrentDocument, String aDataOwner, boolean aForceSelection,
            Set<DirtySpot> aDirties)
    {
        var user = userRepository.get(aSessionOwner);
        // do not trigger training during when viewing others' work
        if (user == null || !user.equals(userRepository.getCurrentUser())) {
            return;
        }

        // Update the task count
        var count = trainingTaskCounter.computeIfAbsent(
                new RecommendationStateKey(user.getUsername(), aProject),
                _key -> new AtomicInteger(0));

        // If there is no active recommender at all then let's try hard to make one active by
        // re-setting the count and thus force-scheduling a SelectionTask
        if (!hasActiveRecommenders(aSessionOwner, aProject)) {
            count.set(0);
        }

        if (aForceSelection || (count.getAndIncrement() % TRAININGS_PER_SELECTION == 0)) {
            // If it is time for a selection task, we just start a selection task.
            // The selection task then will start the training once its finished,
            // i.e. we do not start it here.
            schedulingService.enqueue(
                    new SelectionTask(user, aProject, aEventName, aCurrentDocument, aDataOwner));

            var state = getState(aSessionOwner, aProject);
            synchronized (state) {
                state.setPredictionsUntilNextEvaluation(TRAININGS_PER_SELECTION - 1);
                state.setPredictionsSinceLastEvaluation(0);
            }

            return;
        }

        schedulingService.enqueue(
                new TrainingTask(user, aProject, aEventName, aCurrentDocument, aDataOwner));

        var state = getState(aSessionOwner, aProject);
        synchronized (state) {
            int predictions = state.getPredictionsSinceLastEvaluation() + 1;
            state.setPredictionsSinceLastEvaluation(predictions);
            state.setPredictionsUntilNextEvaluation(TRAININGS_PER_SELECTION - predictions - 1);
        }
    }

    @Override
    public List<LogMessageGroup> getLog(String aUser, Project aProject)
    {
        var activePredictions = getState(aUser, aProject).getActivePredictions();
        var incomingPredictions = getState(aUser, aProject).getIncomingPredictions();

        var messageSets = new ArrayList<LogMessageGroup>();

        if (activePredictions != null) {
            messageSets.add(
                    new LogMessageGroup("Active (gen. " + activePredictions.getGeneration() + ")",
                            activePredictions.getLog()));
        }

        if (incomingPredictions != null) {
            messageSets.add(new LogMessageGroup(
                    "Incoming (gen. " + incomingPredictions.getGeneration() + ")",
                    incomingPredictions.getLog()));
        }

        return messageSets;
    }

    @Override
    public boolean isPredictForAllDocuments(String aUser, Project aProject)
    {
        return getState(aUser, aProject).isPredictForAllDocuments();
    }

    @Override
    public void setPredictForAllDocuments(String aUser, Project aProject,
            boolean aPredictForAllDocuments)
    {
        getState(aUser, aProject).setPredictForAllDocuments(aPredictForAllDocuments);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onSessionDestroyed(SessionDestroyedEvent event)
    {
        var info = sessionRegistry.getSessionInformation(event.getId());
        // Could be an anonymous session without information.
        if (info == null) {
            return;
        }

        String username = null;
        if (info.getPrincipal() instanceof String) {
            username = (String) info.getPrincipal();
        }

        if (info.getPrincipal() instanceof User) {
            username = ((User) info.getPrincipal()).getUsername();
        }

        if (username != null) {
            clearState(username);
        }
    }

    @Transactional
    @EventListener
    public void afterDocumentReset(AfterDocumentResetEvent aEvent)
    {
        var currentDocument = aEvent.getDocument().getDocument();
        var currentUser = aEvent.getDocument().getUser();
        clearState(currentUser);
        deleteLearningRecords(currentDocument, currentUser);
    }

    @Override
    public Preferences getPreferences(User aUser, Project aProject)
    {
        var state = getState(aUser.getUsername(), aProject);
        return state.getPreferences();
    }

    @Override
    public void setPreferences(User aUser, Project aProject, Preferences aPreferences)
    {
        var state = getState(aUser.getUsername(), aProject);
        state.setPreferences(aPreferences);
    }

    @Override
    public Optional<RecommendationEngineFactory<?>> getRecommenderFactory(Recommender aRecommender)
    {
        return Optional.ofNullable(recommenderFactoryRegistry.getFactory(aRecommender.getTool()));
    }

    private RecommendationState getState(String aUsername, Project aProject)
    {
        synchronized (states) {
            return states.computeIfAbsent(new RecommendationStateKey(aUsername, aProject),
                    (v) -> new RecommendationState());
        }
    }

    @Override
    public void clearState(String aUsername)
    {
        Validate.notNull(aUsername, "Username must be specified");

        synchronized (states) {
            states.keySet().removeIf(key -> aUsername.equals(key.getUser()));
            trainingTaskCounter.keySet().removeIf(key -> aUsername.equals(key.getUser()));
        }
    }

    private void clearState(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        synchronized (states) {
            states.keySet().removeIf(key -> Objects.equals(aProject.getId(), key.getProjectId()));
            trainingTaskCounter.keySet()
                    .removeIf(key -> Objects.equals(aProject.getId(), key.getProjectId()));
        }
    }

    private void removePredictions(Recommender aRecommender)
    {
        Validate.notNull(aRecommender, "Recommender must be specified");

        synchronized (states) {
            states.entrySet().stream()
                    .filter(entry -> Objects.equals(aRecommender.getProject().getId(),
                            entry.getKey().getProjectId()))
                    .forEach(entry -> entry.getValue().removePredictions(aRecommender));
        }
    }

    @Override
    public boolean switchPredictions(String aSessionOwner, Project aProject)
    {
        var state = getState(aSessionOwner, aProject);
        synchronized (state) {
            return state.switchPredictions();
        }
    }

    @Override
    public Optional<RecommenderContext> getContext(String aSessionOwner, Recommender aRecommender)
    {
        var state = getState(aSessionOwner, aRecommender.getProject());
        synchronized (state) {
            return state.getContext(aRecommender);
        }
    }

    @Override
    public void putContext(User aUser, Recommender aRecommender, RecommenderContext aContext)
    {
        var state = getState(aUser.getUsername(), aRecommender.getProject());
        synchronized (state) {
            state.putContext(aRecommender, aContext);
        }
    }

    @Deprecated
    @Override
    @Transactional
    public AnnotationFS correctSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, SpanSuggestion aOriginalSuggestion,
            SpanSuggestion aCorrectedSuggestion, LearningRecordChangeLocation aLocation)
        throws AnnotationException
    {
        var layer = schemaService.getLayer(aOriginalSuggestion.getLayerId());
        var feature = schemaService.getFeature(aOriginalSuggestion.getFeature(), layer);

        var originalRls = layerRecommendtionSupportRegistry
                .findGenericExtension(aOriginalSuggestion);
        if (originalRls.isPresent()) {
            // If the action was a correction (i.e. suggestion label != annotation value) then
            // generate a rejection for the original value - we do not want the original value to
            // re-appear
            logRecord(aSessionOwner, aDocument, aDataOwner, aOriginalSuggestion, feature, REJECTED,
                    aLocation);
        }

        var correctedRls = layerRecommendtionSupportRegistry
                .findGenericExtension(aCorrectedSuggestion);
        if (correctedRls.isPresent()) {
            var adapter = schemaService.getAdapter(layer);

            return (AnnotationFS) originalRls.get().acceptSuggestion(aSessionOwner, aDocument,
                    aDataOwner, aCas, adapter, feature, aCorrectedSuggestion, aLocation, CORRECTED);
        }

        return null;
    }

    @Override
    @Transactional
    public AnnotationBaseFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, AnnotationSuggestion aSuggestion,
            LearningRecordChangeLocation aLocation)
        throws AnnotationException
    {
        var layer = schemaService.getLayer(aSuggestion.getLayerId());
        var feature = schemaService.getFeature(aSuggestion.getFeature(), layer);
        var adapter = schemaService.getAdapter(layer);

        var rls = layerRecommendtionSupportRegistry.findGenericExtension(aSuggestion);

        if (rls.isPresent()) {
            return rls.get().acceptSuggestion(aSessionOwner, aDocument, aDataOwner, aCas, adapter,
                    feature, aSuggestion, aLocation, ACCEPTED);
        }

        return null;
    }

    private static class CommittedDocument
    {
        private final long projectId;
        private final long documentId;
        private final String user;

        public CommittedDocument(AnnotationDocument aDocument)
        {
            projectId = aDocument.getProject().getId();
            documentId = aDocument.getId();
            user = aDocument.getUser();
        }

        public long getDocumentId()
        {
            return documentId;
        }

        @SuppressWarnings("unused")
        public long getProjectId()
        {
            return projectId;
        }

        public String getUser()
        {
            return user;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(documentId, projectId, user);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CommittedDocument other = (CommittedDocument) obj;
            return documentId == other.documentId && projectId == other.projectId
                    && Objects.equals(user, other.user);
        }
    }

    private static class RecommendationStateKey
    {
        private final String user;
        private final long projectId;

        public RecommendationStateKey(String aUser, long aProjectId)
        {
            user = aUser;
            projectId = aProjectId;
        }

        public RecommendationStateKey(String aUser, Project aProject)
        {
            this(aUser, aProject.getId());
        }

        public long getProjectId()
        {
            return projectId;
        }

        public String getUser()
        {
            return user;
        }

        @Override
        public boolean equals(final Object other)
        {
            if (!(other instanceof RecommendationStateKey)) {
                return false;
            }
            RecommendationStateKey castOther = (RecommendationStateKey) other;
            return new EqualsBuilder().append(user, castOther.user)
                    .append(projectId, castOther.projectId).isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder().append(user).append(projectId).toHashCode();
        }
    }

    /**
     * We are assuming that the user is actively working on one project at a time. Otherwise, the
     * RecommendationUserState might take up a lot of memory.
     */
    private class RecommendationState
    {
        private Preferences preferences;
        private MultiValuedMap<AnnotationLayer, EvaluatedRecommender> evaluatedRecommenders;
        private Map<Recommender, RecommenderContext> contexts;
        private Predictions activePredictions;
        private Predictions incomingPredictions;
        private boolean predictForAllDocuments;
        private Map<AnnotationLayer, List<LearningRecord>> learningRecords;
        private int predictionsSinceLastEvaluation;
        private int predictionsUntilNextEvaluation;

        public RecommendationState()
        {
            preferences = new Preferences();
            evaluatedRecommenders = new HashSetValuedHashMap<>();
            contexts = new ConcurrentHashMap<>();
            learningRecords = new ConcurrentHashMap<>();
        }

        public Preferences getPreferences()
        {
            return preferences;
        }

        public void setPreferences(Preferences aPreferences)
        {
            preferences = aPreferences;
        }

        public MultiValuedMap<AnnotationLayer, EvaluatedRecommender> getActiveRecommenders()
        {
            MultiValuedMap<AnnotationLayer, EvaluatedRecommender> active = new HashSetValuedHashMap<>();

            MapIterator<AnnotationLayer, EvaluatedRecommender> i = evaluatedRecommenders
                    .mapIterator();
            while (i.hasNext()) {
                i.next();
                if (i.getValue().isActive()) {
                    active.put(i.getKey(), i.getValue());
                }
            }

            return active;
        }

        public MultiValuedMap<AnnotationLayer, EvaluatedRecommender> getEvaluatedRecommenders()
        {
            return evaluatedRecommenders;
        }

        public void setPredictionsSinceLastEvaluation(int aPredictionsSinceLastEvaluation)
        {
            predictionsSinceLastEvaluation = aPredictionsSinceLastEvaluation;
        }

        public int getPredictionsSinceLastEvaluation()
        {
            return predictionsSinceLastEvaluation;
        }

        public void setPredictionsUntilNextEvaluation(int aPredictionsUntilNextEvaluation)
        {
            predictionsUntilNextEvaluation = aPredictionsUntilNextEvaluation;
        }

        public int getPredictionsUntilNextEvaluation()
        {
            return predictionsUntilNextEvaluation;
        }

        public void setEvaluatedRecommenders(AnnotationLayer aLayer,
                Collection<EvaluatedRecommender> aEvaluations)
        {
            evaluatedRecommenders.remove(aLayer);
            evaluatedRecommenders.putAll(aLayer, aEvaluations);
        }

        public void setEvaluatedRecommenders(
                MultiValuedMap<AnnotationLayer, EvaluatedRecommender> aEvaluatedRecommenders)
        {
            evaluatedRecommenders = aEvaluatedRecommenders;
        }

        public Predictions getActivePredictions()
        {
            return activePredictions;
        }

        public void setIncomingPredictions(Predictions aIncomingPredictions)
        {
            Validate.notNull(aIncomingPredictions, "Predictions must be specified");

            incomingPredictions = aIncomingPredictions;
        }

        public Predictions getIncomingPredictions()
        {
            return incomingPredictions;
        }

        public boolean switchPredictions()
        {
            // If the predictions have already been switched, do not switch again
            RequestCycle requestCycle = RequestCycle.get();
            if (requestCycle != null) {
                Boolean switched = requestCycle.getMetaData(PredictionSwitchPerformedKey.INSTANCE);
                if (switched != null && switched) {
                    return false;
                }
            }

            if (incomingPredictions == null) {
                return false;
            }

            activePredictions = incomingPredictions;
            incomingPredictions = null;

            if (requestCycle != null) {
                requestCycle.setMetaData(PredictionSwitchPerformedKey.INSTANCE, true);
            }

            return true;
        }

        /**
         * @param aRecommender
         *            a recommender of interest
         * @return the context for the given recommender if there is one.
         */
        public Optional<RecommenderContext> getContext(Recommender aRecommender)
        {
            Validate.notNull(aRecommender, "Recommender must be specified");

            return Optional.ofNullable(contexts.get(aRecommender));
        }

        public void putContext(Recommender aRecommender, RecommenderContext aContext)
        {
            Validate.notNull(aRecommender, "Recommender must be specified");
            Validate.notNull(aContext, "Context must be specified");
            Validate.isTrue(aContext.isClosed(), "Context must be closed");

            contexts.put(aRecommender, aContext);
        }

        public void removePredictions(Recommender aRecommender)
        {
            // Remove incoming predictions
            if (incomingPredictions != null) {
                incomingPredictions.removePredictions(aRecommender.getId());
            }

            // Remove active predictions
            if (activePredictions != null) {
                activePredictions.removePredictions(aRecommender.getId());
            }

            // Remove trainedModel
            contexts.remove(aRecommender);

            // Remove from evaluatedRecommenders map.
            // We have to do this, otherwise training and prediction continues for the
            // recommender when a new task is triggered.
            MultiValuedMap<AnnotationLayer, EvaluatedRecommender> newEvaluatedRecommenders = //
                    new HashSetValuedHashMap<>();
            MapIterator<AnnotationLayer, EvaluatedRecommender> it = evaluatedRecommenders
                    .mapIterator();

            while (it.hasNext()) {
                AnnotationLayer layer = it.next();
                EvaluatedRecommender rec = it.getValue();
                if (!rec.getRecommender().equals(aRecommender)) {
                    newEvaluatedRecommenders.put(layer, rec);
                }
            }

            setEvaluatedRecommenders(newEvaluatedRecommenders);
        }

        public boolean isPredictForAllDocuments()
        {
            return predictForAllDocuments;
        }

        public void setPredictForAllDocuments(boolean aPredictForAllDocuments)
        {
            predictForAllDocuments = aPredictForAllDocuments;
        }

        public void logRecord(LearningRecord aRecord)
        {
            var records = learningRecords.computeIfAbsent(aRecord.getLayer(),
                    $ -> RecommendationServiceImpl.this.loadLearningRecords(aRecord.getUser(),
                            aRecord.getLayer(), 0));
            records.add(0, aRecord);
        }

        public List<LearningRecord> listLearningRecords(AnnotationLayer aLayer)
        {
            return learningRecords.getOrDefault(aLayer, Collections.emptyList());
        }

        public void removeLearningRecords(LearningRecord aRecord)
        {
            var records = learningRecords.get(aRecord.getLayer());
            if (records == null) {
                return;
            }

            records.removeIf(r -> Objects.equals(r.getUser(), aRecord.getUser()) && //
                    Objects.equals(r.getSourceDocument(), aRecord.getSourceDocument()) && //
                    r.getOffsetBegin() == aRecord.getOffsetBegin() && //
                    r.getOffsetEnd() == aRecord.getOffsetEnd() && //
                    r.getOffsetBegin2() == aRecord.getOffsetBegin2() && //
                    r.getOffsetEnd2() == aRecord.getOffsetEnd2() && //
                    Objects.equals(r.getAnnotationFeature(), aRecord.getAnnotationFeature()) && //
                    Objects.equals(r.getSuggestionType(), aRecord.getSuggestionType()) && //
                    Objects.equals(r.getAnnotation(), aRecord.getAnnotation()));
        }
    }

    private void computePredictions(LazyCas aOriginalCas,
            EvaluatedRecommender aEvaluatedRecommender, Predictions activePredictions,
            Predictions aPredictions, CAS predictionCas, SourceDocument aDocument,
            User aSessionOwner, int aPredictionBegin, int aPredictionEnd)
        throws IOException
    {
        var project = aDocument.getProject();
        var predictionBegin = aPredictionBegin;
        var predictionEnd = aPredictionEnd;

        // Make sure we have the latest recommender config from the DB - the one
        // from the active recommenders list may be outdated
        Recommender recommender = aEvaluatedRecommender.getRecommender();
        try {
            recommender = getRecommender(recommender.getId());
        }
        catch (NoResultException e) {
            aPredictions.log(LogMessage.info(recommender.getName(),
                    "Recommender no longer available... skipping"));
            LOG.info("{}[{}]: Recommender no longer available... skipping", aSessionOwner,
                    recommender.getName());
            return;
        }

        if (!recommender.isEnabled()) {
            aPredictions.log(
                    LogMessage.info(recommender.getName(), "Recommender disabled... skipping"));
            LOG.debug("{}[{}]: Disabled - skipping", aSessionOwner, recommender.getName());
            return;
        }

        Optional<RecommenderContext> context = getContext(aSessionOwner.getUsername(), recommender);

        if (!context.isPresent()) {
            aPredictions.log(LogMessage.info(recommender.getName(),
                    "Recommender has no context... skipping"));
            LOG.info("No context available for recommender {} for user {} on document {} in " //
                    + "project {} - skipping recommender", recommender, aSessionOwner, aDocument,
                    aDocument.getProject());
            return;
        }

        RecommenderContext ctx = context.get();
        ctx.setUser(aSessionOwner);

        Optional<RecommendationEngineFactory<?>> maybeFactory = getRecommenderFactory(recommender);

        if (maybeFactory.isEmpty()) {
            LOG.warn("{}[{}]: No factory found - skipping recommender", aSessionOwner,
                    recommender.getName());
            return;
        }

        RecommendationEngineFactory<?> factory = maybeFactory.get();

        // Check that configured layer and feature are accepted
        // by this type of recommender
        if (!factory.accepts(recommender.getLayer(), recommender.getFeature())) {
            aPredictions.log(LogMessage.info(recommender.getName(),
                    "Recommender configured with invalid layer or feature... skipping"));
            LOG.info("{}[{}]: Recommender configured with invalid layer or feature "
                    + "- skipping recommender", aSessionOwner, recommender.getName());
            return;
        }

        // We lazily load the CAS only at this point because that allows us to skip
        // loading the CAS entirely if there is no enabled layer or recommender.
        // If the CAS cannot be loaded, then we skip to the next document.
        CAS originalCas = aOriginalCas.get();
        predictionBegin = aPredictionBegin < 0 ? 0 : aPredictionBegin;
        predictionEnd = aPredictionEnd < 0 ? originalCas.getDocumentText().length()
                : aPredictionEnd;

        try {
            RecommendationEngine engine = factory.build(recommender);

            if (!engine.isReadyForPrediction(ctx)) {
                aPredictions.log(LogMessage.info(recommender.getName(),
                        "Recommender context is not ready... skipping"));
                LOG.info("Recommender context {} for user {} in project {} is not ready for " //
                        + "prediction - skipping recommender", recommender, aSessionOwner,
                        aDocument.getProject());

                // If possible, we inherit recommendations from a previous run while
                // the recommender is still busy
                if (activePredictions != null) {
                    inheritSuggestionsAtRecommenderLevel(aPredictions, originalCas, recommender,
                            activePredictions, aDocument, aSessionOwner);
                }

                return;
            }

            cloneAndMonkeyPatchCAS(project, originalCas, predictionCas);

            // If the recommender is not trainable and not sensitive to annotations,
            // we can actually re-use the predictions.
            if (TRAINING_NOT_SUPPORTED == engine.getTrainingCapability()
                    && PREDICTION_USES_TEXT_ONLY == engine.getPredictionCapability()
                    && activePredictions != null
                    && activePredictions.hasRunPredictionOnDocument(aDocument)) {
                inheritSuggestionsAtRecommenderLevel(aPredictions, originalCas,
                        engine.getRecommender(), activePredictions, aDocument, aSessionOwner);
            }
            else {
                generateSuggestions(aPredictions, ctx, engine, activePredictions, aDocument,
                        originalCas, predictionCas, predictionBegin, predictionEnd);
            }
        }
        // Catching Throwable is intentional here as we want to continue the
        // execution even if a particular recommender fails.
        catch (Throwable e) {
            aPredictions.log(LogMessage.error(recommender.getName(), "Failed: %s", e.getMessage()));
            LOG.error("Error applying recommender {} for user {} to document {} in project {} - " //
                    + "skipping recommender", recommender, aSessionOwner, aDocument,
                    aDocument.getProject(), e);

            applicationEventPublisher.publishEvent(RecommenderTaskNotificationEvent
                    .builder(this, project, aSessionOwner.getUsername()) //
                    .withMessage(LogMessage.error(this, "Recommender [%s] failed: %s",
                            recommender.getName(), e.getMessage())) //
                    .build());

            // If there was a previous successful run of the recommender, inherit
            // its suggestions to avoid that all the suggestions of the recommender
            // simply disappear.
            if (activePredictions != null) {
                inheritSuggestionsAtRecommenderLevel(aPredictions, originalCas, recommender,
                        activePredictions, aDocument, aSessionOwner);
            }

            return;
        }
    }

    /**
     * @param aPredictions
     *            the predictions to populate
     * @param aPredictionCas
     *            the re-usable buffer CAS to use when calling recommenders
     * @param aDocument
     *            the current document
     * @param aPredictionBegin
     *            begin of the prediction window (&lt; 0 for 0)
     * @param aPredictionEnd
     *            end of the prediction window (&lt; 0 for document-end)
     * @param aDataOwner
     *            the annotation data owner
     */
    private void computePredictions(Predictions aActivePredictions, Predictions aPredictions,
            CAS aPredictionCas, SourceDocument aDocument, String aDataOwner, int aPredictionBegin,
            int aPredictionEnd)
    {
        var aSessionOwner = aPredictions.getSessionOwner();

        try {
            var recommenders = getActiveRecommenders(aSessionOwner, aDocument.getProject());
            if (recommenders.isEmpty()) {
                aPredictions.log(LogMessage.info(this, "No active recommenders"));
                LOG.trace("[{}]: No active recommenders", aSessionOwner);
                return;
            }

            LazyCas originalCas = new LazyCas(aDocument, aDataOwner);
            for (var recommender : recommenders) {
                var layer = schemaService.getLayer(recommender.getRecommender().getLayer().getId());
                if (!layer.isEnabled()) {
                    continue;
                }

                computePredictions(originalCas, recommender, aActivePredictions, aPredictions,
                        aPredictionCas, aDocument, aSessionOwner, aPredictionBegin, aPredictionEnd);
            }
        }
        catch (IOException e) {
            aPredictions.log(LogMessage.error(this, "Cannot read annotation CAS... skipping"));
            LOG.error(
                    "Cannot read annotation CAS for user {} of document "
                            + "[{}]({}) in project [{}]({}) - skipping document",
                    aSessionOwner, aDocument.getName(), aDocument.getId(),
                    aDocument.getProject().getName(), aDocument.getProject().getId(), e);
            return;
        }

        // When all recommenders have completed on the document, we mark it as "complete"
        aPredictions.markDocumentAsPredictionCompleted(aDocument);
    }

    @Override
    public Predictions computePredictions(User aSessionOwner, Project aProject,
            List<SourceDocument> aDocuments, String aDataOwner, TaskMonitor aMonitor)
    {
        var activePredictions = getPredictions(aSessionOwner, aProject);
        var predictions = activePredictions != null ? new Predictions(activePredictions)
                : new Predictions(aSessionOwner, aDataOwner, aProject);

        try (var casHolder = new PredictionCasHolder()) {
            // Generate new predictions or inherit at the recommender level
            aMonitor.setMaxProgress(aDocuments.size());
            for (SourceDocument document : aDocuments) {
                aMonitor.addMessage(LogMessage.info(this, "%s", document.getName()));
                aMonitor.incrementProgress();
                computePredictions(activePredictions, predictions, casHolder.cas, document,
                        aDataOwner, -1, -1);
            }

            return predictions;
        }
        catch (ResourceInitializationException e) {
            predictions.log(
                    LogMessage.error(this, "Cannot create prediction CAS, stopping predictions!"));
            LOG.error("Cannot create prediction CAS, stopping predictions!");
            return predictions;
        }
    }

    @Override
    public Predictions computePredictions(User aSessionOwner, Project aProject,
            SourceDocument aCurrentDocument, String aDataOwner, List<SourceDocument> aInherit,
            int aPredictionBegin, int aPredictionEnd, TaskMonitor aMonitor)
    {
        aMonitor.setMaxProgress(1);

        var activePredictions = getPredictions(aSessionOwner, aProject);
        var predictions = activePredictions != null ? new Predictions(activePredictions)
                : new Predictions(aSessionOwner, aDataOwner, aProject);

        // Inherit at the document level. If inheritance at a recommender level is possible,
        // this is done below.
        if (activePredictions != null) {
            for (var document : aInherit) {
                inheritSuggestionsAtDocumentLevel(aProject, document, aSessionOwner,
                        activePredictions, predictions);
            }
        }

        try (var casHolder = new PredictionCasHolder()) {
            final CAS predictionCas = casHolder.cas;

            // Generate new predictions or inherit at the recommender level
            computePredictions(activePredictions, predictions, predictionCas, aCurrentDocument,
                    aDataOwner, aPredictionBegin, aPredictionEnd);
        }
        catch (ResourceInitializationException e) {
            predictions.log(
                    LogMessage.error(this, "Cannot create prediction CAS, stopping predictions!"));
            LOG.error("Cannot create prediction CAS, stopping predictions!");
        }

        aMonitor.setProgress(1);

        return predictions;
    }

    /**
     * Extracts existing predictions from the last prediction run so we do not have to recalculate
     * them. This is useful when the engine is not trainable.
     */
    private void inheritSuggestionsAtRecommenderLevel(Predictions predictions, CAS aOriginalCas,
            Recommender aRecommender, Predictions activePredictions, SourceDocument document,
            User aUser)
    {
        var suggestions = activePredictions.getPredictionsByRecommenderAndDocument(aRecommender,
                document.getName());

        if (suggestions.isEmpty()) {
            LOG.debug("{} for user {} on document {} in project {} there " //
                    + "are no inheritable predictions", aRecommender, aUser, document,
                    aRecommender.getProject());
            predictions.log(LogMessage.info(aRecommender.getName(),
                    "No inheritable suggestions from previous run"));
            return;
        }

        LOG.debug("{} for user {} on document {} in project {} inherited {} " //
                + "predictions", aRecommender, aUser, document, aRecommender.getProject(),
                suggestions.size());

        predictions.log(LogMessage.info(aRecommender.getName(),
                "Inherited [%d] predictions from previous run", suggestions.size()));

        predictions.putPredictions(suggestions);
    }

    /**
     * Extracts existing predictions from the last prediction run so we do not have to recalculate
     * them. This is useful when the engine is not trainable.
     */
    private void inheritSuggestionsAtDocumentLevel(Project aProject, SourceDocument aDocument,
            User aUser, Predictions aOldPredictions, Predictions aNewPredictions)
    {
        if (!aOldPredictions.hasRunPredictionOnDocument(aDocument)) {
            return;
        }

        var suggestions = aOldPredictions.getPredictionsByDocument(aDocument.getName());

        LOG.debug("[{}]({}) for user [{}] on document {} in project {} inherited {} predictions",
                "ALL", "--", aUser.getUsername(), aDocument, aProject, suggestions.size());

        aNewPredictions.putPredictions(suggestions);
        aNewPredictions.markDocumentAsPredictionCompleted(aDocument);
    }

    /**
     * Invokes the engine to produce new suggestions.
     */
    void generateSuggestions(Predictions aIncomingPredictions, RecommenderContext aCtx,
            RecommendationEngine aEngine, Predictions aActivePredictions, SourceDocument aDocument,
            CAS aOriginalCas, CAS aPredictionCas, int aPredictionBegin, int aPredictionEnd)
        throws RecommendationException
    {
        var sessionOwner = aIncomingPredictions.getSessionOwner();
        var recommender = aEngine.getRecommender();

        // Perform the actual prediction
        aIncomingPredictions.log(LogMessage.info(recommender.getName(),
                "Generating predictions for layer [%s]...", recommender.getLayer().getUiName()));
        LOG.trace("{}[{}]: Generating predictions for layer [{}]", sessionOwner,
                recommender.getName(), recommender.getLayer().getUiName());
        var predictedRange = aEngine.predict(aCtx, aPredictionCas, aPredictionBegin,
                aPredictionEnd);

        // Extract the suggestions from the data which the recommender has written into the CAS
        var generatedSuggestions = extractSuggestions(aIncomingPredictions.getGeneration(),
                aOriginalCas, aPredictionCas, aDocument, recommender);

        // Reconcile new suggestions with suggestions from previous run
        var reconciliationResult = reconcile(aActivePredictions, aDocument, recommender,
                predictedRange, generatedSuggestions);
        LOG.debug(
                "{} for user {} on document {} in project {} generated {} predictions within range {} (+{}/-{}/={})",
                recommender, sessionOwner, aDocument, recommender.getProject(),
                generatedSuggestions.size(), predictedRange, reconciliationResult.added,
                reconciliationResult.removed, reconciliationResult.aged);
        aIncomingPredictions.log(LogMessage.info(recommender.getName(), //
                "Generated [%d] predictions within range %s (+%d/-%d/=%d)",
                generatedSuggestions.size(), predictedRange, reconciliationResult.added,
                reconciliationResult.removed, reconciliationResult.aged));
        var suggestions = reconciliationResult.suggestions;

        // Inherit suggestions that are outside the range which was predicted. Note that the engine
        // might actually predict a different range from what was requested. If the prediction
        // covers the entire document, we can skip this.
        if (aActivePredictions != null
                && !predictedRange.equals(rangeCoveringDocument(aOriginalCas))) {
            var inheritableSuggestions = aActivePredictions
                    .getPredictionsByRecommenderAndDocument(recommender, aDocument.getName())
                    .stream() //
                    .filter(s -> !s.coveredBy(predictedRange)) //
                    .collect(toList());

            LOG.debug("{} for user {} on document {} in project {} inherited {} " //
                    + "predictions", recommender, sessionOwner, aDocument, recommender.getProject(),
                    inheritableSuggestions.size());
            aIncomingPredictions.log(LogMessage.info(recommender.getName(),
                    "Inherited [%d] predictions from previous run", inheritableSuggestions.size()));

            suggestions.addAll(inheritableSuggestions);
        }

        // Calculate the visibility of the suggestions. This happens via the original CAS which
        // contains only the manually created annotations and *not* the suggestions.
        var groupedSuggestions = SuggestionDocumentGroup.groupByType(suggestions);
        for (var groupEntry : groupedSuggestions.entrySet()) {
            calculateSuggestionVisibility(sessionOwner.getUsername(), aDocument, aOriginalCas,
                    aIncomingPredictions.getDataOwner(), aEngine.getRecommender().getLayer(),
                    groupEntry.getValue(), 0, aOriginalCas.getDocumentText().length());
        }

        aIncomingPredictions.putPredictions(suggestions);
    }

    static ReconciliationResult reconcile(Predictions aActivePredictions, SourceDocument aDocument,
            Recommender recommender, Range predictedRange,
            List<AnnotationSuggestion> aNewProtoSuggesitons)
    {
        if (aActivePredictions == null) {
            return new ReconciliationResult(aNewProtoSuggesitons.size(), 0, 0,
                    aNewProtoSuggesitons);
        }

        var reconciledSuggestions = new LinkedHashSet<AnnotationSuggestion>();
        var addedSuggestions = new ArrayList<AnnotationSuggestion>();
        int agedSuggestionsCount = 0;

        var predictionsByRecommenderAndDocument = aActivePredictions
                .getPredictionsByRecommenderAndDocument(recommender, aDocument.getName());

        var existingSuggestionsByPosition = predictionsByRecommenderAndDocument.stream() //
                .filter(s -> s.coveredBy(predictedRange)) //
                .collect(groupingBy(AnnotationSuggestion::getPosition));

        for (var newSuggestion : aNewProtoSuggesitons) {
            var existingSuggestions = existingSuggestionsByPosition
                    .getOrDefault(newSuggestion.getPosition(), emptyList()).stream() //
                    .filter(s -> Objects.equals(s.getLabel(), newSuggestion.getLabel()) && //
                            s.getScore() == newSuggestion.getScore() && //
                            Objects.equals(s.getScoreExplanation(),
                                    newSuggestion.getScoreExplanation()))
                    .collect(toList());

            if (existingSuggestions.isEmpty()) {
                addedSuggestions.add(newSuggestion);
                reconciledSuggestions.add(newSuggestion);
                continue;
            }

            if (existingSuggestions.size() > 1) {
                LOG.debug("Recommender produced more than one suggestion with the same "
                        + "label, score and score explanation - reconciling with first one");
            }

            var existingSuggestion = existingSuggestions.get(0);
            existingSuggestion.incrementAge();
            agedSuggestionsCount++;
            reconciledSuggestions.add(existingSuggestion);
        }

        var removedSuggestions = predictionsByRecommenderAndDocument.stream() //
                .filter(s -> s.coveredBy(predictedRange)) //
                .filter(s -> !reconciledSuggestions.contains(s)) //
                .collect(toList());

        return new ReconciliationResult(addedSuggestions.size(), removedSuggestions.size(),
                agedSuggestionsCount, new ArrayList<>(reconciledSuggestions));
    }

    @Override
    public <T extends AnnotationSuggestion> void calculateSuggestionVisibility(String aSessionOwner,
            SourceDocument aDocument, CAS aCas, String aDataOwner, AnnotationLayer aLayer,
            Collection<SuggestionGroup<T>> aRecommendations, int aWindowBegin, int aWindowEnd)
    {
        var maybeSuggestion = aRecommendations.stream().filter(group -> !group.isEmpty())
                .flatMap(group -> group.stream()).findAny();

        if (maybeSuggestion.isEmpty()) {
            return;
        }

        var rls = layerRecommendtionSupportRegistry.findGenericExtension(maybeSuggestion.get());

        if (rls.isPresent()) {
            rls.get().calculateSuggestionVisibility(aSessionOwner, aDocument, aCas, aDataOwner,
                    aLayer, aRecommendations, aWindowBegin, aWindowEnd);
        }
    }

    /**
     * Clones the source CAS to the target CAS while adding the features required for encoding
     * predictions to the respective types.
     * 
     * @param aProject
     *            the project to which the CASes belong.
     * @param aSourceCas
     *            the source CAS.
     * @param aTargetCas
     *            the target CAS which is meant to be sent off to a recommender.
     * @return the target CAS which is meant to be sent off to a recommender.
     * @throws UIMAException
     *             if there was a CAS-related error.
     * @throws IOException
     *             if there was a serialization-related errror.
     */
    CAS cloneAndMonkeyPatchCAS(Project aProject, CAS aSourceCas, CAS aTargetCas)
        throws UIMAException, IOException
    {
        try (var watch = new StopWatch(LOG, "adding score features")) {
            var tsd = schemaService.getFullProjectTypeSystem(aProject);
            var features = schemaService.listAnnotationFeature(aProject);

            RecommenderTypeSystemUtils.addPredictionFeaturesToTypeSystem(tsd, features);

            schemaService.upgradeCas(aSourceCas, aTargetCas, tsd);
        }

        return aTargetCas;
    }

    private class TriggerTrainingTaskListener
        implements IRequestCycleListener
    {
        private final SourceDocument currentDocument;
        private final String dataOwner;

        public TriggerTrainingTaskListener(SourceDocument aCurrentDocument, String aDataOwner)
        {
            currentDocument = aCurrentDocument;
            dataOwner = aDataOwner;
        }

        @Override
        public void onEndRequest(RequestCycle cycle)
        {
            var dirties = cycle.getMetaData(DIRTIES);
            var committed = cycle.getMetaData(COMMITTED);

            if (dirties == null || committed == null) {
                return;
            }

            // Any dirties which have not been committed can be ignored
            for (var committedDocument : committed) {
                dirties.removeIf(dirty -> dirty.affectsDocument(committedDocument.getDocumentId(),
                        committedDocument.getUser()));
            }

            // Concurrent action has deleted project, so we can ignore this
            var affectedProjects = dirties.stream() //
                    .map(DirtySpot::getProject) //
                    .distinct() //
                    .collect(toMap(p -> p.getId(), p -> p));
            for (var project : affectedProjects.values()) {
                if (projectService.getProject(project.getId()) == null) {
                    dirties.removeIf(dirty -> dirty.getProject().equals(project));
                }
            }

            var dirtiesByContext = new LinkedHashMap<RecommendationStateKey, Set<DirtySpot>>();
            for (var spot : dirties) {
                var key = new RecommendationStateKey(spot.getUser(), spot.getProject().getId());
                dirtiesByContext.computeIfAbsent(key, k -> new HashSet<>()).add(spot);
            }

            for (var contextDirties : dirtiesByContext.entrySet()) {
                var key = contextDirties.getKey();
                triggerTraining(key.getUser(), affectedProjects.get(key.getProjectId()),
                        "Committed dirty CAS at end of request", currentDocument, dataOwner, false,
                        contextDirties.getValue());
            }
        }
    }

    @Override
    @Transactional
    public boolean existsEnabledRecommender(Project aProject)
    {
        var criteriaBuilder = entityManager.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(Recommender.class);

        var root = criteriaQuery.from(Recommender.class);
        var predicate = criteriaBuilder.and( //
                criteriaBuilder.equal(root.get(Recommender_.enabled), true), //
                criteriaBuilder.equal(root.get(Recommender_.project), aProject));

        criteriaQuery.select(root).where(predicate);

        var recommenders = entityManager.createQuery(criteriaQuery).getResultList();

        return recommenders.stream() //
                .anyMatch(rec -> getRecommenderFactory(rec).isPresent());
    }

    @Override
    @Transactional
    public long countEnabledRecommenders()
    {
        String query = String.join("\n", //
                "FROM Recommender WHERE", //
                "enabled = :enabled");

        List<Recommender> recommenders = entityManager.createQuery(query, Recommender.class) //
                .setParameter("enabled", true) //
                .getResultList();

        return recommenders.stream() //
                .filter(rec -> getRecommenderFactory(rec).isPresent()) //
                .count();
    }

    @Override
    public Progress getProgressTowardsNextEvaluation(User aUser, Project aProject)
    {
        RecommendationState state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            return new Progress(state.getPredictionsSinceLastEvaluation(),
                    state.getPredictionsUntilNextEvaluation());
        }
    }

    @Override
    @Transactional
    public void rejectSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion suggestion, LearningRecordChangeLocation aAction)
        throws AnnotationException
    {
        var rls = layerRecommendtionSupportRegistry.findGenericExtension(suggestion);

        if (rls.isPresent()) {
            rls.get().rejectSuggestion(aSessionOwner, aDocument, aDataOwner, suggestion, aAction);
        }
    }

    @Override
    @Transactional
    public void skipSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion suggestion, LearningRecordChangeLocation aAction)
        throws AnnotationException
    {
        var rls = layerRecommendtionSupportRegistry.findGenericExtension(suggestion);

        if (rls.isPresent()) {
            rls.get().skipSuggestion(aSessionOwner, aDocument, aDataOwner, suggestion, aAction);
        }
    }

    @Transactional
    @Override
    public void logRecord(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, AnnotationFeature aFeature,
            LearningRecordUserAction aUserAction, LearningRecordChangeLocation aLocation)
    {
        LearningRecord record = null;

        var rls = layerRecommendtionSupportRegistry.findGenericExtension(aSuggestion);

        if (rls.isPresent()) {
            record = rls.get().toLearningRecord(aDocument, aDataOwner, aSuggestion, aFeature,
                    aUserAction, aLocation);
        }

        if (record == null) {
            throw new IllegalArgumentException(
                    "Unsupported suggestion type [" + aSuggestion.getClass().getName() + "]");
        }

        if (aSessionOwner != null) {
            var state = getState(aSessionOwner, aDocument.getProject());
            synchronized (state) {
                state.removeLearningRecords(record);
                state.logRecord(record);
            }
        }

        deleteLearningRecords(record);
        createLearningRecord(record);
    }

    private void deleteLearningRecords(LearningRecord aRecord)
    {
        // It doesn't make any sense at all to have duplicate entries in the learning history,
        // so when adding a new entry, we dump any existing entries which basically are the
        // same as the one added. Mind that the actual action performed by the user does not
        // matter since there should basically be only one action in the log for any suggestion,
        // irrespective of what that action is.
        String query = String.join("\n", //
                "DELETE FROM LearningRecord WHERE", //
                "user = :user AND", //
                "sourceDocument = :sourceDocument AND", //
                "offsetBegin = :offsetBegin AND", //
                "offsetEnd = :offsetEnd AND", //
                "offsetBegin2 = :offsetBegin2 AND", //
                "offsetEnd2 = :offsetEnd2 AND", //
                "layer = :layer AND", //
                "annotationFeature = :annotationFeature AND", //
                "suggestionType = :suggestionType AND", //
                "annotation = :annotation");
        entityManager.createQuery(query) //
                .setParameter("user", aRecord.getUser()) //
                .setParameter("sourceDocument", aRecord.getSourceDocument()) //
                .setParameter("offsetBegin", aRecord.getOffsetBegin()) //
                .setParameter("offsetEnd", aRecord.getOffsetEnd()) //
                .setParameter("offsetBegin2", aRecord.getOffsetBegin2()) //
                .setParameter("offsetEnd2", aRecord.getOffsetEnd2()) //
                .setParameter("layer", aRecord.getAnnotationFeature().getLayer()) //
                .setParameter("annotationFeature", aRecord.getAnnotationFeature()) //
                .setParameter("suggestionType", aRecord.getSuggestionType()) //
                .setParameter("annotation", aRecord.getAnnotation()) //
                .executeUpdate();
    }

    @Transactional
    @Override
    public List<LearningRecord> listLearningRecords(Project aProject)
    {
        String sql = "FROM LearningRecord l WHERE l.sourceDocument.project = :project";
        TypedQuery<LearningRecord> query = entityManager.createQuery(sql, LearningRecord.class) //
                .setParameter("project", aProject);
        return query.getResultList();
    }

    @Transactional
    @Override
    public List<LearningRecord> listLearningRecords(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, AnnotationFeature aFeature)
    {
        var state = getState(aSessionOwner, aDocument.getProject());
        synchronized (state) {
            return state.listLearningRecords(aFeature.getLayer()).stream()
                    .filter(r -> Objects.equals(r.getAnnotationFeature(), aFeature)
                            && Objects.equals(r.getSourceDocument(), aDocument)
                            && Objects.equals(r.getUser(), aDataOwner)
                            && r.getUserAction() != LearningRecordUserAction.SHOWN)
                    .toList();
        }
    }

    @Transactional
    @Override
    public List<LearningRecord> listLearningRecords(String aSessionOwner, String aDataOwner,
            AnnotationLayer aLayer, int aLimit)
    {
        var state = getState(aSessionOwner, aLayer.getProject());
        synchronized (state) {
            var stream = state.listLearningRecords(aLayer).stream()
                    .filter(r -> Objects.equals(r.getUser(), aDataOwner)
                            && r.getUserAction() != LearningRecordUserAction.SHOWN);
            if (aLimit > 0) {
                stream = stream.limit(aLimit);
            }
            return stream.toList();
        }
    }

    @Transactional
    @Override
    public List<LearningRecord> listLearningRecords(String aSessionOwner, String aDataOwner,
            AnnotationLayer aLayer)
    {
        return listLearningRecords(aSessionOwner, aDataOwner, aLayer, 0);
    }

    private List<LearningRecord> loadLearningRecords(String aDataOwner, AnnotationLayer aLayer,
            int aLimit)
    {
        LOG.trace("loadLearningRecords({},{}, {})", aDataOwner, aLayer, aLimit);

        String sql = String.join("\n", //
                "FROM LearningRecord l WHERE", //
                "l.user = :user AND", //
                "l.layer = :layer AND", //
                "l.userAction != :action", //
                "ORDER BY l.id desc");
        TypedQuery<LearningRecord> query = entityManager.createQuery(sql, LearningRecord.class) //
                .setParameter("user", aDataOwner) //
                .setParameter("layer", aLayer) //
                // SHOWN records NOT returned
                .setParameter("action", LearningRecordUserAction.SHOWN);

        if (aLimit > 0) {
            query = query.setMaxResults(aLimit);
        }

        return query.getResultList();
    }

    @Override
    @Transactional
    public void createLearningRecord(LearningRecord aLearningRecord)
    {
        entityManager.persist(aLearningRecord);
        entityManager.flush();
    }

    private void deleteLearningRecords(SourceDocument document, String user)
    {
        String sql = "DELETE FROM LearningRecord l where l.sourceDocument = :document and l.user "
                + "= :user";
        entityManager.createQuery(sql) //
                .setParameter("document", document) //
                .setParameter("user", user) //
                .executeUpdate();
    }

    @Override
    @Transactional
    public void deleteLearningRecord(LearningRecord learningRecord)
    {
        entityManager.remove(entityManager.contains(learningRecord) ? learningRecord
                : entityManager.merge(learningRecord));
    }

    @Override
    @Transactional
    public boolean hasSkippedSuggestions(String aSessionOwner, User aDataOwner,
            AnnotationLayer aLayer)
    {
        String sql = String.join("\n", //
                "SELECT COUNT(*) FROM LearningRecord WHERE", //
                "user = :user AND", //
                "layer = :layer AND", //
                "userAction = :action");
        long count = entityManager.createQuery(sql, Long.class) //
                .setParameter("user", aDataOwner.getUsername()) //
                .setParameter("layer", aLayer) //
                .setParameter("action", SKIPPED) //
                .getSingleResult();
        return count > 0;
    }

    @Override
    @Transactional
    public void deleteSkippedSuggestions(String aSessionOwner, User aDataOwner,
            AnnotationLayer aLayer)
    {
        var state = getState(aSessionOwner, aLayer.getProject());
        synchronized (state) {
            state.learningRecords.getOrDefault(aLayer, Collections.emptyList())
                    .removeIf(r -> Objects.equals(r.getUser(), aDataOwner.getUsername())
                            && r.getUserAction() == SKIPPED);
        }

        String sql = String.join("\n", //
                "DELETE FROM LearningRecord WHERE", //
                "user = :user AND", //
                "layer = :layer AND", //
                "userAction = :action");
        entityManager.createQuery(sql) //
                .setParameter("user", aDataOwner.getUsername()) //
                .setParameter("layer", aLayer) //
                .setParameter("action", SKIPPED) //
                .executeUpdate();
    }

    private class LazyCas
    {
        private final SourceDocument document;
        private final String dataOwner;

        private CAS originalCas;

        public LazyCas(SourceDocument aDocument, String aDataOwner)
        {
            document = aDocument;
            dataOwner = aDataOwner;
        }

        public CAS get() throws IOException
        {
            if (originalCas == null) {
                originalCas = documentService.readAnnotationCas(document, dataOwner,
                        AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
            }

            return originalCas;
        }
    }

    private static class PredictionCasHolder
        implements AutoCloseable
    {
        private final CAS cas;

        public PredictionCasHolder() throws ResourceInitializationException
        {
            cas = WebAnnoCasUtil.createCas();
            CasStorageSession.get().add(PREDICTION_CAS, EXCLUSIVE_WRITE_ACCESS, cas);
        }

        @Override
        public void close()
        {
            CasStorageSession.get().remove(cas);
        }
    }

    final record ReconciliationResult(int added, int removed, int aged,
            List<AnnotationSuggestion> suggestions)
    {}
}
