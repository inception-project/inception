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

import static de.tudarmstadt.ukp.inception.recommendation.api.model.AutoAcceptMode.ON_FIRST_ACCESS;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.AUTO_ACCEPT;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.CORRECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.SKIPPED;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.PageRequestHandlerTracker;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportQuery;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.event.PredictionsSwitchedEvent;
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
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingInstance;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderDeletedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderUpdatedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendersResumedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendersSuspendedEvent;
import de.tudarmstadt.ukp.inception.recommendation.model.DirtySpot;
import de.tudarmstadt.ukp.inception.recommendation.tasks.NonTrainableRecommenderActivationTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.PredictionTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.SelectionTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.TrainingTask;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.logging.LogMessageGroup;
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

    private final EntityManager entityManager;

    private final SessionRegistry sessionRegistry;
    private final UserDao userRepository;
    private final RecommenderFactoryRegistry recommenderFactoryRegistry;
    private final SchedulingService schedulingService;
    private final AnnotationSchemaService schemaService;
    private final ProjectService projectService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PreferencesService preferencesService;
    private final SuggestionSupportRegistry suggestionSupportRegistry;

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

    @Value("${curation.sidebar.enabled:false}")
    private boolean curationSidebarEnabled;

    @Autowired
    public RecommendationServiceImpl(PreferencesService aPreferencesService,
            SessionRegistry aSessionRegistry, UserDao aUserRepository,
            RecommenderFactoryRegistry aRecommenderFactoryRegistry,
            SchedulingService aSchedulingService, AnnotationSchemaService aAnnoService,
            ProjectService aProjectService, EntityManager aEntityManager,
            ApplicationEventPublisher aApplicationEventPublisher,
            SuggestionSupportRegistry aLayerRecommendtionSupportRegistry)
    {
        preferencesService = aPreferencesService;
        sessionRegistry = aSessionRegistry;
        userRepository = aUserRepository;
        recommenderFactoryRegistry = aRecommenderFactoryRegistry;
        schedulingService = aSchedulingService;
        schemaService = aAnnoService;
        projectService = aProjectService;
        entityManager = aEntityManager;
        applicationEventPublisher = aApplicationEventPublisher;
        suggestionSupportRegistry = aLayerRecommendtionSupportRegistry;

        trainingTaskCounter = new ConcurrentHashMap<>();
        states = new ConcurrentHashMap<>();
    }

    public RecommendationServiceImpl(PreferencesService aPreferencesService,
            SessionRegistry aSessionRegistry, UserDao aUserRepository,
            RecommenderFactoryRegistry aRecommenderFactoryRegistry,
            SchedulingService aSchedulingService, AnnotationSchemaService aAnnoService,
            SuggestionSupportRegistry aLayerRecommendtionSupportRegistry,
            EntityManager aEntityManager)
    {
        this(aPreferencesService, aSessionRegistry, aUserRepository, aRecommenderFactoryRegistry,
                aSchedulingService, aAnnoService, (ProjectService) null, aEntityManager, null,
                aLayerRecommendtionSupportRegistry);
    }

    @Deprecated
    @Override
    public boolean isCurationSidebarEnabled()
    {
        return curationSidebarEnabled;
    }

    @Override
    public Predictions getPredictions(String aSessionOwner, Project aProject)
    {
        var state = getState(aSessionOwner, aProject);
        return state.getActivePredictions();
    }

    @Override
    public Predictions getPredictions(User aSessionOwner, Project aProject)
    {
        var state = getState(aSessionOwner.getUsername(), aProject);
        return state.getActivePredictions();
    }

    @Override
    public Predictions getIncomingPredictions(User aSessionOwner, Project aProject)
    {
        var state = getState(aSessionOwner.getUsername(), aProject);
        return state.getIncomingPredictions();
    }

    @Override
    public void putIncomingPredictions(User aSessionOwner, Project aProject,
            Predictions aPredictions)
    {
        var state = getState(aSessionOwner.getUsername(), aProject);
        synchronized (state) {
            state.setIncomingPredictions(aPredictions);
        }
    }

    @Override
    public boolean hasActiveRecommenders(String aSessionOwner, Project aProject)
    {
        var state = getState(aSessionOwner, aProject);
        synchronized (state) {
            return !state.getActiveRecommenders().isEmpty();
        }
    }

    @Override
    public void setEvaluatedRecommenders(User aSessionOwner, AnnotationLayer aLayer,
            List<EvaluatedRecommender> aRecommenders)
    {
        var state = getState(aSessionOwner.getUsername(), aLayer.getProject());
        synchronized (state) {
            state.setEvaluatedRecommenders(aLayer, aRecommenders);
        }
    }

    @Override
    public List<EvaluatedRecommender> getEvaluatedRecommenders(User aSessionOwner,
            AnnotationLayer aLayer)
    {
        var state = getState(aSessionOwner.getUsername(), aLayer.getProject());
        synchronized (state) {
            return new ArrayList<>(state.getEvaluatedRecommenders().get(aLayer));
        }
    }

    @Override
    public Optional<EvaluatedRecommender> getEvaluatedRecommender(User aSessionOwner,
            Recommender aRecommender)
    {
        var state = getState(aSessionOwner.getUsername(), aRecommender.getProject());
        synchronized (state) {
            return state.getEvaluatedRecommenders().get(aRecommender.getLayer()).stream()
                    .filter(r -> r.getRecommender().equals(aRecommender)).findAny();
        }
    }

    @Override
    public List<EvaluatedRecommender> getActiveRecommenders(User aSessionOwner,
            AnnotationLayer aLayer)
    {
        var state = getState(aSessionOwner.getUsername(), aLayer.getProject());
        synchronized (state) {
            return new ArrayList<>(state.getActiveRecommenders().get(aLayer));
        }
    }

    @Override
    public List<EvaluatedRecommender> getActiveRecommenders(User aSessionOwner, Project aProject)
    {
        var state = getState(aSessionOwner.getUsername(), aProject);
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
    @Transactional(readOnly = true)
    public boolean existsRecommender(Project aProject, String aRecommender)
    {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(Recommender.class);

        query.select(cb.count(root)).where(cb.and( //
                cb.equal(root.get(Recommender_.name), aRecommender), //
                cb.equal(root.get(Recommender_.project), aProject)));

        long count = entityManager.createQuery(query).getSingleResult();

        return count > 0;
    }

    @Override
    @Transactional
    public Optional<Recommender> getRecommender(Project aProject, String aRecommender)
    {
        String query = String.join("\n", //
                "FROM Recommender ", //
                "WHERE name = :name ", //
                "AND project = :project");

        return entityManager.createQuery(query, Recommender.class) //
                .setParameter("name", aRecommender) //
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
    @Transactional
    public List<Recommender> listEnabledRecommenders(Project aProject)
    {
        var query = String.join("\n", //
                "FROM Recommender WHERE", //
                "project = :project AND", //
                "enabled = :enabled", //
                "ORDER BY name ASC");

        return entityManager.createQuery(query, Recommender.class) //
                .setParameter("project", aProject) //
                .setParameter("enabled", true) //
                .getResultList() //
                .stream() //
                .filter(rec -> getRecommenderFactory(rec).isPresent()) //
                .toList();
    }

    @Override
    @Transactional
    public List<Recommender> listRecommenders(AnnotationLayer aLayer)
    {
        var query = String.join("\n", //
                "FROM Recommender WHERE ", //
                "layer = :layer", //
                "ORDER BY name ASC");

        return entityManager.createQuery(query, Recommender.class) //
                .setParameter("layer", aLayer) //
                .getResultList() //
                .stream() //
                .filter(rec -> getRecommenderFactory(rec).isPresent()) //
                .toList();
    }

    @Override
    @Transactional
    public List<Recommender> listEnabledRecommenders(AnnotationLayer aLayer)
    {
        var query = String.join("\n", //
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

    @EventListener
    public void onDocumentOpened(DocumentOpenedEvent aEvent)
    {
        var project = aEvent.getDocument().getProject();
        var sessionOwnerName = aEvent.getSessionOwner();

        if (isSuspended(sessionOwnerName, project)) {
            return;
        }

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
                resetState(sessionOwnerName);
            }
        }

        // We want to get predictions from all trained recommenders immediately - be they externally
        // pre-trained or possibly internal recommenders that have been trained due to earlier
        // actions.
        var trigger = aEvent.getClass().getSimpleName();
        if (!predictionSessionExistedOnOpen) {
            activateNonTrainableRecommenersSync(project, sessionOwner, trigger);
        }

        // Check if there are any synchronous recommenders we need to run
        var recommenders = listEnabledRecommenders(aEvent.getDocument().getProject());
        if (!recommenders.isEmpty()) {
            runSynchronousRecommenders(aEvent.getDocument(), aEvent.getDocumentOwner(),
                    recommenders, "onDocumentOpened");
        }

        // Check if we need to wait for the initial recommender run before displaying the document
        // to the user
        var predictionTriggered = runNonTrainableRecommenderSync(doc, predictions, sessionOwner,
                trigger, dataOwner);

        // Is it the first time a document has been opened? If yes, there might be auto-accept
        // suggestions that need to be processed (in particular ones that may have been generated
        // by the non-trainable recommenders triggered above or from already existing predictions
        if (aEvent.getStateBeforeOpening() == AnnotationDocumentState.NEW) {
            autoAcceptOnDocumentOpen(aEvent.getRequestTarget().orElse(null), sessionOwner, doc,
                    ON_FIRST_ACCESS);
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
            schedulingService.enqueue(PredictionTask.builder() //
                    .withSessionOwner(sessionOwner) //
                    .withTrigger("onDocumentOpened") //
                    .withCurrentDocument(doc) //
                    .withDataOwner(dataOwner) //
                    .withSynchronousRecommenders(false) //
                    .build());
        }
    }

    private void activateNonTrainableRecommenersSync(Project project, User sessionOwner,
            String trigger)
    {
        if (sessionOwner == null || project.getId() == null) {
            LOG.trace("Not activating non-trainable recommenders because we are outside a user "
                    + "session or project has not yet been persisted");
            return;
        }

        // Activate all non-trainable recommenders - execute synchronously - blocking
        schedulingService.executeSync(NonTrainableRecommenderActivationTask.builder() //
                .withSessionOwner(sessionOwner) //
                .withProject(project) //
                .withTrigger(trigger) //
                .build());
    }

    private boolean runNonTrainableRecommenderSync(SourceDocument doc, Predictions predictions,
            User aSessionOwner, String trigger, String aDataOwner)
    {
        if (isSuspended(aSessionOwner.getUsername(), doc.getProject())) {
            return false;
        }

        var settings = preferencesService
                .loadDefaultTraitsForProject(KEY_RECOMMENDER_GENERAL_SETTINGS, doc.getProject());
        if (!settings.isWaitForRecommendersOnOpenDocument()) {
            LOG.trace("Not running sync prediction for non-trainable recommenders because the "
                    + "option is not enabled in the project settings");
            return false;
        }

        if (predictions != null && predictions.hasRunPredictionOnDocument(doc)) {
            LOG.trace("Not running sync prediction for non-trainable recommenders as we already "
                    + "have predictions");
            return false;
        }

        LOG.trace("Running sync prediction for non-trainable recommenders");
        schedulingService.executeSync(PredictionTask.builder() //
                .withSessionOwner(aSessionOwner) //
                .withTrigger(trigger) //
                .withCurrentDocument(doc) //
                .withDataOwner(aDataOwner) //
                .withSynchronousRecommenders(false) //
                .build());
        switchPredictions(aSessionOwner.getUsername(), doc.getProject());

        return true;
    }

    private void autoAcceptOnDocumentOpen(AjaxRequestTarget aTarget, User aSessionOwner,
            SourceDocument aDocument, AutoAcceptMode aAutoAcceptMode)
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
        if (!page.isEditable()) {
            return;
        }

        var predictions = getPredictions(aSessionOwner, aDocument.getProject());
        if (predictions == null || predictions.isEmpty()) {
            LOG.trace("Not auto-accepting because no predictions are available");
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

        var accepted = autoAccept(aSessionOwner, aDocument, aAutoAcceptMode, predictions, cas);

        if (accepted > 0) {
            try {
                page.writeEditorCas(cas);
            }
            catch (Exception e) {
                WicketExceptionUtil.handleException(LOG, page, aTarget, e);
            }
        }
    }

    private int autoAccept(User aSessionOwner, SourceDocument aDocument,
            AutoAcceptMode aAutoAcceptMode, Predictions predictions, CAS cas)
    {
        var accepted = 0;
        var recommenderCache = listEnabledRecommenders(aDocument.getProject()).stream()
                .collect(toMap(Recommender::getId, identity()));
        var suggestionSupportCache = new HashMap<SuggestionSupportQuery, Optional<SuggestionSupport>>();

        for (var prediction : predictions.getPredictionsByDocument(aDocument.getId())) {
            if (prediction.getAutoAcceptMode() != aAutoAcceptMode) {
                continue;
            }

            // We do not clear auto-accept for on-first-access predictions because these should be
            // restored after a document reset but they won't be re-generated after a document
            // reset.
            if (prediction.getAutoAcceptMode() != ON_FIRST_ACCESS) {
                prediction.clearAutoAccept();
            }

            var recommender = recommenderCache.get(prediction.getRecommenderId());
            if (recommender == null) {
                continue;
            }

            var suggestionSupport = suggestionSupportCache.computeIfAbsent(
                    SuggestionSupportQuery.of(recommender),
                    suggestionSupportRegistry::findGenericExtension);
            if (suggestionSupport.isEmpty()) {
                continue;
            }

            var feature = recommender.getFeature();
            var adapter = schemaService.getAdapter(recommender.getLayer());
            adapter.silenceEvents();

            try {
                suggestionSupport.get().acceptSuggestion(null, aDocument,
                        aSessionOwner.getUsername(), cas, adapter, feature, predictions, prediction,
                        AUTO_ACCEPT, ACCEPTED);
                accepted++;
            }
            catch (AnnotationException e) {
                LOG.debug("Not auto-accepting suggestion: {}", e.getMessage());
            }
        }

        predictions.log(LogMessage.info(this, "Auto-accepted [%d] suggestions", accepted));
        LOG.debug("Auto-accepted [{}] suggestions", accepted);
        return accepted;
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

        var incrementalTrainingData = new LinkedHashMap<Recommender, List<TrainingInstance>>();
        var sessionOwner = userRepository.getCurrentUser();
        var recommenders = getActiveRecommenders(sessionOwner, aEvent.getProject()).stream() //
                .map(EvaluatedRecommender::getRecommender) //
                .filter(Recommender::isEnabled) //
                .filter(rec -> rec.getLayer() != null) //
                .filter(rec -> rec.getLayer().isEnabled()) //
                .filter(rec -> rec.getFeature().isEnabled()) //
                .filter(rec -> rec.getLayer().equals(aEvent.getLayer())) //
                .toList();

        for (var recommender : recommenders) {
            try {
                var maybeFactory = getRecommenderFactory(recommender);
                if (maybeFactory.isEmpty()) {
                    continue;
                }

                var factory = maybeFactory.get();
                var engine = factory.build(recommender);

                incrementalTrainingData.computeIfAbsent(recommender, $ -> new ArrayList<>()) //
                        .addAll(engine.generateIncrementalTrainingInstances(aEvent));
            }
            catch (Exception e) {
                LOG.warn("Unable to collect incremental training data for active recommender {}",
                        recommender);
                continue;
            }
        }

        dirties.add(new DirtySpot(aEvent, incrementalTrainingData));
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

        var recommenders = listEnabledRecommenders(aEvent.getDocument().getProject());
        if (recommenders.isEmpty()) {
            return;
        }

        runSynchronousRecommenders(aEvent.getDocument().getDocument(),
                aEvent.getDocument().getUser(), recommenders, "onAfterCasWritten");

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

    private void runSynchronousRecommenders(SourceDocument aDocument, String aDataOwner,
            List<Recommender> recommenders, String aTrigger)
    {
        var sessionOwner = userRepository.getCurrentUser();

        if (isSuspended(sessionOwner.getUsername(), aDocument.getProject())) {
            return;
        }

        var syncRecommenders = new ArrayList<Recommender>();
        for (var recommender : recommenders) {
            var factory = getRecommenderFactory(recommender);
            if (factory.map($ -> $.isSynchronous(recommender)).orElse(false)) {
                syncRecommenders.add(recommender);
            }
        }

        if (!syncRecommenders.isEmpty()) {
            schedulingService.executeSync(PredictionTask.builder() //
                    .withSessionOwner(sessionOwner) //
                    .withTrigger(aTrigger) //
                    .withCurrentDocument(aDocument) //
                    .withDataOwner(aDataOwner) //
                    .withRecommender(syncRecommenders.toArray(Recommender[]::new)) //
                    .build());

            var switched = forceSwitchPredictions(sessionOwner.getUsername(),
                    aDocument.getProject());
            if (switched) {
                // Notify other UI components on the page about the prediction switch such that they
                // can also update their state to remain in sync with the new predictions
                applicationEventPublisher.publishEvent(
                        new PredictionsSwitchedEvent(this, sessionOwner.getUsername(), aDocument));
            }
        }
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
    public void onRecommenderUpdated(RecommenderUpdatedEvent aEvent)
    {
        removePredictions(aEvent.getRecommender());

        var sessionOwner = userRepository.getCurrentUser();
        var project = aEvent.getRecommender().getProject();

        activateNonTrainableRecommenersSync(project, sessionOwner, "onRecommenderUpdated");
    }

    @EventListener
    public void onDocumentCreated(AfterDocumentCreatedEvent aEvent)
    {
        // I don't think we need to reset the state when a new document is created. Sure, we can get
        // new training data if the document has been pre-annotated, but we will pick that up during
        // the next training run regularly.
        // resetState(aEvent.getDocument().getProject());
    }

    @EventListener
    public void onDocumentRemoval(BeforeDocumentRemovedEvent aEvent)
    {
        // I think we reset the state here primarily to remove predictions on documents which have
        // been removed. Seems a bit of an overkill though.
        // resetState(aEvent.getDocument().getProject());
        deleteLearningRecords(aEvent.getDocument());
    }

    @EventListener
    public void onLayerConfigurationChangedEvent(LayerConfigurationChangedEvent aEvent)
    {
        // I think we reset the state here to ensure that we do not have any annotation suggestions
        // that contradict the new layer configuration. In particular, since we do not know what
        // actually changed in the schema.
        resetState(aEvent.getProject());
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

    @Override
    public void triggerPrediction(String aSessionOwner, String aEventName, SourceDocument aDocument,
            String aDataOwner)
    {
        if (isSuspended(aSessionOwner, aDocument.getProject())) {
            return;
        }

        var sessionOwner = userRepository.get(aSessionOwner);

        if (sessionOwner == null) {
            return;
        }

        schedulingService.enqueue(PredictionTask.builder() //
                .withSessionOwner(sessionOwner) //
                .withTrigger(aEventName) //
                .withCurrentDocument(aDocument) //
                .withDataOwner(aDataOwner) //
                .build());
    }

    @Override
    public void triggerTrainingAndPrediction(String aSessionOwner, Project aProject,
            String aEventName, SourceDocument aCurrentDocument, String aDataOwner)
    {
        if (isSuspended(aSessionOwner, aProject)) {
            return;
        }

        triggerTraining(aSessionOwner, aProject, aEventName, aCurrentDocument, aDataOwner, false,
                emptySet());
    }

    @Override
    public void triggerSelectionTrainingAndPrediction(String aSessionOwner, Project aProject,
            String aEventName, SourceDocument aCurrentDocument, String aDataOwner)
    {
        if (isSuspended(aSessionOwner, aProject)) {
            return;
        }

        triggerTraining(aSessionOwner, aProject, aEventName, aCurrentDocument, aDataOwner, true,
                emptySet());
    }

    private void triggerTraining(String aSessionOwner, Project aProject, String aEventName,
            SourceDocument aCurrentDocument, String aDataOwner, boolean aForceSelection,
            Set<DirtySpot> aDirties)
    {
        if (isSuspended(aSessionOwner, aProject)) {
            return;
        }

        // Do not trigger training during when viewing others' work
        var sessionOwner = userRepository.get(aSessionOwner);
        if (sessionOwner == null || !sessionOwner.equals(userRepository.getCurrentUser())) {
            return;
        }

        commitIncrementalTrainingData(aSessionOwner, aDirties);

        // Update the task count
        var count = trainingTaskCounter.computeIfAbsent(
                new RecommendationStateKey(aSessionOwner, aProject.getId()),
                _key -> new AtomicInteger(0));

        // If there is no active recommender at all then let's try hard to make one active by
        // re-setting the count and thus force-scheduling a SelectionTask
        if (!hasActiveRecommenders(aSessionOwner, aProject)) {
            count.set(0);
        }

        if (aForceSelection || (count.getAndIncrement() % TRAININGS_PER_SELECTION == 0)) {
            triggerSelectionRun(sessionOwner, aProject, aCurrentDocument, aDataOwner, aEventName);
            return;
        }

        triggerTrainingRun(sessionOwner, aProject, aCurrentDocument, aDataOwner, aEventName);
    }

    private void commitIncrementalTrainingData(String aSessionOwner, Set<DirtySpot> aDirties)
    {
        if (isEmpty(aDirties)) {
            return;
        }

        var aggregatedIncrementalTrainingData = new LinkedHashMap<Recommender, List<TrainingInstance>>();
        for (var dirtySpot : aDirties) {
            for (var entry : dirtySpot.getIncrementalTrainingData().entrySet()) {
                var recommender = entry.getKey();
                aggregatedIncrementalTrainingData
                        .computeIfAbsent(recommender, $ -> new ArrayList<>()) //
                        .addAll(entry.getValue());
            }
        }

        for (var entry : aggregatedIncrementalTrainingData.entrySet()) {
            var recommender = entry.getKey();
            var maybeContext = getContext(aSessionOwner, recommender);
            if (maybeContext.isEmpty()) {
                continue;
            }

            try {
                var maybeFactory = getRecommenderFactory(recommender);
                if (maybeFactory.isEmpty()) {
                    continue;
                }

                var factory = maybeFactory.get();
                var engine = factory.build(recommender);
                engine.putIncrementalTrainingData(maybeContext.get(), entry.getValue());
            }
            catch (Exception e) {
                LOG.warn("Unable to collect incremental training data for active recommender {}",
                        recommender);
                continue;
            }
        }
    }

    private void triggerTrainingRun(User aSessionOwner, Project aProject, SourceDocument aDocument,
            String aDataOwner, String aTrigger)
    {
        if (isSuspended(aSessionOwner.getUsername(), aProject)) {
            return;
        }

        schedulingService.enqueue(TrainingTask.builder() //
                .withSessionOwner(aSessionOwner) //
                .withProject(aProject) //
                .withTrigger(aTrigger) //
                .withCurrentDocument(aDocument) //
                .withDataOwner(aDataOwner) //
                .build());

        var state = getState(aSessionOwner.getUsername(), aProject);
        synchronized (state) {
            int predictions = state.getPredictionsSinceLastEvaluation() + 1;
            state.setPredictionsSinceLastEvaluation(predictions);
            state.setPredictionsUntilNextEvaluation(TRAININGS_PER_SELECTION - predictions - 1);
        }
    }

    private void triggerSelectionRun(User aSessionOwner, Project aProject, SourceDocument aDocument,
            String aDataOwner, String aEventName)
    {
        if (isSuspended(aSessionOwner.getUsername(), aProject)) {
            return;
        }

        // If it is time for a selection task, we just start a selection task.
        // The selection task then will start the training once its finished,
        // i.e. we do not start it here.
        schedulingService.enqueue(SelectionTask.builder() //
                .withSessionOwner(aSessionOwner) //
                .withProject(aProject) //
                .withTrigger(aEventName) //
                .withCurrentDocument(aDocument) //
                .withDataOwner(aDataOwner) //
                .build());

        var state = getState(aSessionOwner.getUsername(), aProject);
        synchronized (state) {
            state.setPredictionsUntilNextEvaluation(TRAININGS_PER_SELECTION - 1);
            state.setPredictionsSinceLastEvaluation(0);
        }
    }

    @Override
    public List<LogMessageGroup> getLog(String aSessionOwner, Project aProject)
    {
        var activePredictions = getState(aSessionOwner, aProject).getActivePredictions();
        var incomingPredictions = getState(aSessionOwner, aProject).getIncomingPredictions();

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
    public void setPredictForAllDocuments(String aSessionOwner, Project aProject,
            boolean aPredictForAllDocuments)
    {
        getState(aSessionOwner, aProject).setPredictForAllDocuments(aPredictForAllDocuments);
    }

    @Override
    public boolean isSuspended(String aSessionOwner, Project aProject)
    {
        return getState(aSessionOwner, aProject).isSuspended();
    }

    @Override
    public void setSuspended(String aSessionOwner, Project aProject, boolean aState)
    {
        var suspended = isSuspended(aSessionOwner, aProject);
        if (suspended == aState) {
            return;
        }

        getState(aSessionOwner, aProject).setSuspended(aState);
        if (aState) {
            applicationEventPublisher
                    .publishEvent(new RecommendersSuspendedEvent(this, aProject, aSessionOwner));
            ;
        }
        else {
            applicationEventPublisher
                    .publishEvent(new RecommendersResumedEvent(this, aProject, aSessionOwner));
            ;
        }
    }

    // Set order so this is handled before session info is removed from sessionRegistry
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
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
        resetState(currentUser);
        deleteLearningRecords(currentDocument, currentUser);
    }

    @Override
    public Preferences getPreferences(User aSessionOwner, Project aProject)
    {
        var state = getState(aSessionOwner.getUsername(), aProject);
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
        if (aRecommender == null) {
            return empty();
        }

        return Optional.ofNullable(recommenderFactoryRegistry.getFactory(aRecommender.getTool()));
    }

    private RecommendationState getState(String aSessionOwner, Project aProject)
    {
        synchronized (states) {
            return states.computeIfAbsent(
                    new RecommendationStateKey(aSessionOwner, aProject.getId()),
                    (v) -> new RecommendationState());
        }
    }

    @Override
    public void resetState(String aSessionOwner)
    {
        Validate.notNull(aSessionOwner, "Username must be specified");

        synchronized (states) {
            states.entrySet().stream() //
                    .filter(e -> aSessionOwner.equals(e.getKey().user()))
                    .forEach(e -> e.getValue().reset());
            trainingTaskCounter.keySet().removeIf(key -> aSessionOwner.equals(key.user()));
        }
    }

    private void clearState(String aSessionOwner)
    {
        Validate.notNull(aSessionOwner, "Username must be specified");

        synchronized (states) {
            states.keySet().removeIf(key -> aSessionOwner.equals(key.user()));
            trainingTaskCounter.keySet().removeIf(key -> aSessionOwner.equals(key.user()));
        }
    }

    private void resetState(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        synchronized (states) {
            states.entrySet().stream() //
                    .filter(e -> Objects.equals(aProject.getId(), e.getKey().projectId()))
                    .forEach(e -> e.getValue().reset());
            trainingTaskCounter.keySet()
                    .removeIf(key -> Objects.equals(aProject.getId(), key.projectId()));
        }
    }

    private void clearState(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        synchronized (states) {
            states.keySet().removeIf(key -> Objects.equals(aProject.getId(), key.projectId()));
            trainingTaskCounter.keySet()
                    .removeIf(key -> Objects.equals(aProject.getId(), key.projectId()));
        }
    }

    private void removePredictions(Recommender aRecommender)
    {
        Validate.notNull(aRecommender, "Recommender must be specified");

        synchronized (states) {
            states.entrySet().stream()
                    .filter(entry -> Objects.equals(aRecommender.getProject().getId(),
                            entry.getKey().projectId()))
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
    public boolean forceSwitchPredictions(String aSessionOwner, Project aProject)
    {
        var state = getState(aSessionOwner, aProject);
        synchronized (state) {
            return state.forceSwitchPredictions();
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
    public void putContext(User aSessionOwner, Recommender aRecommender,
            RecommenderContext aContext)
    {
        var state = getState(aSessionOwner.getUsername(), aRecommender.getProject());
        synchronized (state) {
            state.putContext(aRecommender, aContext);
        }
    }

    @Override
    @Transactional
    public AnnotationFS correctSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, Predictions aPredictions,
            SpanSuggestion aOriginalSuggestion, SpanSuggestion aCorrectedSuggestion,
            LearningRecordChangeLocation aLocation)
        throws AnnotationException
    {
        var layer = schemaService.getLayer(aOriginalSuggestion.getLayerId());
        var feature = schemaService.getFeature(aOriginalSuggestion.getFeature(), layer);

        var originalSuggestionSupport = suggestionSupportRegistry.findGenericExtension(
                SuggestionSupportQuery.of(getRecommender(aOriginalSuggestion)));
        if (originalSuggestionSupport.isPresent()) {
            // If the action was a correction (i.e. suggestion label != annotation value) then
            // generate a rejection for the original value - we do not want the original value to
            // re-appear
            var record = originalSuggestionSupport.get().toLearningRecord(aDocument, aDataOwner,
                    aOriginalSuggestion, feature, REJECTED, aLocation);
            logRecord(aSessionOwner, record);
        }

        var correctedSuggestionSupport = suggestionSupportRegistry.findGenericExtension(
                SuggestionSupportQuery.of(getRecommender(aCorrectedSuggestion)));
        if (correctedSuggestionSupport.isPresent()) {
            var adapter = schemaService.getAdapter(layer);

            return (AnnotationFS) correctedSuggestionSupport
                    .get().acceptSuggestion(aSessionOwner, aDocument, aDataOwner, aCas, adapter,
                            feature, aPredictions, aCorrectedSuggestion, aLocation, CORRECTED)
                    .orElse(null);
        }

        return null;
    }

    @Override
    @Transactional
    public AnnotationBaseFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, Predictions aPredictions, AnnotationSuggestion aSuggestion,
            LearningRecordChangeLocation aLocation)
        throws AnnotationException
    {
        var layer = schemaService.getLayer(aSuggestion.getLayerId());
        var feature = schemaService.getFeature(aSuggestion.getFeature(), layer);
        var adapter = schemaService.getAdapter(layer);
        var recommender = getRecommender(aSuggestion);

        var rls = suggestionSupportRegistry
                .findGenericExtension(SuggestionSupportQuery.of(recommender));

        if (rls.isPresent()) {
            return rls.get().acceptSuggestion(aSessionOwner, aDocument, aDataOwner, aCas, adapter,
                    feature, aPredictions, aSuggestion, aLocation, ACCEPTED).orElse(null);
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

    private static record RecommendationStateKey(String user, long projectId) {}

    /**
     * We are assuming that the user is actively working on one project at a time. Otherwise, the
     * RecommendationUserState might take up a lot of memory.
     */
    private class RecommendationState
    {
        private boolean suspended;
        private Preferences preferences;
        private boolean predictForAllDocuments;

        private MultiValuedMap<AnnotationLayer, EvaluatedRecommender> evaluatedRecommenders;
        private Map<Recommender, RecommenderContext> contexts;
        private Predictions activePredictions;
        private Predictions incomingPredictions;
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

        public void reset()
        {
            evaluatedRecommenders = new HashSetValuedHashMap<>();
            contexts = new ConcurrentHashMap<>();
            activePredictions = null;
            incomingPredictions = null;
            learningRecords = new ConcurrentHashMap<>();
            predictionsSinceLastEvaluation = 0;
            predictionsUntilNextEvaluation = 0;
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
            var active = new HashSetValuedHashMap<AnnotationLayer, EvaluatedRecommender>();

            var i = evaluatedRecommenders.mapIterator();
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
            return switchPredictions(false);
        }

        public boolean forceSwitchPredictions()
        {
            return switchPredictions(true);
        }

        private boolean switchPredictions(boolean aForce)
        {
            // If the predictions have already been switched, do not switch again
            var requestCycle = RequestCycle.get();
            if (!aForce && requestCycle != null) {
                var switched = requestCycle.getMetaData(PredictionSwitchPerformedKey.INSTANCE);
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
            var newEvaluatedRecommenders = //
                    new HashSetValuedHashMap<AnnotationLayer, EvaluatedRecommender>();
            var it = evaluatedRecommenders.mapIterator();
            while (it.hasNext()) {
                var layer = it.next();
                var rec = it.getValue();
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

        public boolean isSuspended()
        {
            return suspended;
        }

        public void setSuspended(boolean aSuspended)
        {
            suspended = aSuspended;
        }

        public void logRecord(LearningRecord aRecord)
        {
            var records = learningRecords.computeIfAbsent(aRecord.getLayer(),
                    $ -> RecommendationServiceImpl.this.loadLearningRecords(aRecord.getUser(),
                            aRecord.getLayer(), 0));
            records.add(0, aRecord);
        }

        public List<LearningRecord> listLearningRecords(String aDataOwner, AnnotationLayer aLayer)
        {
            return learningRecords.computeIfAbsent(aLayer,
                    $ -> RecommendationServiceImpl.this.loadLearningRecords(aDataOwner, aLayer, 0));
        }

        public void removeLearningRecords(String aDataOwner, SourceDocument aDocument)
        {
            for (var records : learningRecords.values()) {
                records.removeIf(r -> Objects.equals(r.getUser(), aDataOwner) && //
                        Objects.equals(r.getSourceDocument(), aDocument));
            }
        }

        public void removeLearningRecords(SourceDocument aDocument)
        {
            for (var records : learningRecords.values()) {
                records.removeIf(r -> Objects.equals(r.getSourceDocument(), aDocument));
            }
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

    @Override
    public <T extends AnnotationSuggestion> void calculateSuggestionVisibility(String aSessionOwner,
            SourceDocument aDocument, CAS aCas, String aDataOwner, AnnotationLayer aLayer,
            Collection<SuggestionGroup<T>> aRecommendations, int aWindowBegin, int aWindowEnd)
    {
        var maybeSuggestion = aRecommendations.stream() //
                .filter(group -> !group.isEmpty()) //
                .flatMap(group -> group.stream()) //
                .findAny();

        if (maybeSuggestion.isEmpty()) {
            return;
        }

        // All suggestions in the group must be of the same type. Even if they come from different
        // recommenders, the recommenders must all be producing the same type of suggestion, so we
        // can happily just take one of them in order to locate the suggestion support.
        var recommender = getRecommender(maybeSuggestion.get());
        var rls = suggestionSupportRegistry
                .findGenericExtension(SuggestionSupportQuery.of(recommender));

        if (rls.isPresent()) {
            rls.get().calculateSuggestionVisibility(aSessionOwner, aDocument, aCas, aDataOwner,
                    aLayer, aRecommendations, aWindowBegin, aWindowEnd);
        }
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
                    .collect(toMap(Project::getId, p -> p));
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
                triggerTraining(key.user(), affectedProjects.get(key.projectId()),
                        "Committed dirty CAS at end of request", currentDocument, dataOwner, false,
                        contextDirties.getValue());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public long countEnabledRecommenders()
    {
        var query = String.join("\n", //
                "FROM Recommender WHERE", //
                "enabled = :enabled");

        var recommenders = entityManager.createQuery(query, Recommender.class) //
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
        var rls = suggestionSupportRegistry
                .findGenericExtension(SuggestionSupportQuery.of(getRecommender(suggestion)));

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
        var rls = suggestionSupportRegistry
                .findGenericExtension(SuggestionSupportQuery.of(getRecommender(suggestion)));

        if (rls.isPresent()) {
            rls.get().skipSuggestion(aSessionOwner, aDocument, aDataOwner, suggestion, aAction);
        }
    }

    @Transactional
    @Override
    @Deprecated
    public void logRecord(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, AnnotationFeature aFeature,
            LearningRecordUserAction aUserAction, LearningRecordChangeLocation aLocation)
    {
        LearningRecord record = null;

        var rls = suggestionSupportRegistry
                .findGenericExtension(SuggestionSupportQuery.of(getRecommender(aSuggestion)));

        if (rls.isPresent()) {
            record = rls.get().toLearningRecord(aDocument, aDataOwner, aSuggestion, aFeature,
                    aUserAction, aLocation);
        }

        if (record == null) {
            throw new IllegalArgumentException(
                    "Unsupported suggestion type [" + aSuggestion.getClass().getName() + "]");
        }

        logRecord(aSessionOwner, record);
    }

    @Transactional
    @Override
    public void logRecord(String aSessionOwner, LearningRecord aRecord)
    {
        if (aSessionOwner != null) {
            var state = getState(aSessionOwner, aRecord.getSourceDocument().getProject());
            synchronized (state) {
                state.removeLearningRecords(aRecord);
                state.logRecord(aRecord);
            }
        }

        deleteLearningRecords(aRecord);
        createLearningRecord(aRecord);
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
            return state.listLearningRecords(aDataOwner, aFeature.getLayer()).stream()
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
            var stream = state.listLearningRecords(aDataOwner, aLayer).stream()
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

    @Override
    @Transactional
    public void createLearningRecords(LearningRecord... aRecords)
    {
        var start = System.currentTimeMillis();
        for (var record : aRecords) {
            LOG.trace("{}", record);
            entityManager.persist(record);
        }
        var duration = System.currentTimeMillis() - start;

        if (aRecords.length > 0 && !LOG.isTraceEnabled()) {
            LOG.debug("... {} learning records stored ... ({}ms)", aRecords.length, duration);
        }
    }

    private void deleteLearningRecords(SourceDocument aDocument)
    {
        synchronized (states) {
            for (var state : states.values()) {
                state.removeLearningRecords(aDocument);
            }
        }

        var sql = "DELETE FROM LearningRecord l where l.sourceDocument = :document";
        entityManager.createQuery(sql) //
                .setParameter("document", aDocument) //
                .executeUpdate();
    }

    private void deleteLearningRecords(SourceDocument aDocument, String aDataOwner)
    {
        var state = getState(aDataOwner, aDocument.getProject());
        synchronized (state) {
            state.removeLearningRecords(aDataOwner, aDocument);
        }

        var sql = "DELETE FROM LearningRecord l where l.sourceDocument = :document and l.user "
                + "= :user";
        entityManager.createQuery(sql) //
                .setParameter("document", aDocument) //
                .setParameter("user", aDataOwner) //
                .executeUpdate();
    }

    @Override
    @Transactional
    public void deleteLearningRecord(LearningRecord aRecord)
    {
        var state = getState(aRecord.getUser(), aRecord.getLayer().getProject());
        synchronized (state) {
            state.removeLearningRecords(aRecord);
        }

        entityManager
                .remove(entityManager.contains(aRecord) ? aRecord : entityManager.merge(aRecord));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSkippedSuggestions(String aSessionOwner, User aDataOwner,
            AnnotationLayer aLayer)
    {
        var sql = String.join("\n", //
                "SELECT COUNT(*) FROM LearningRecord WHERE", //
                "user = :user AND", //
                "layer = :layer AND", //
                "userAction = :action");
        var count = entityManager.createQuery(sql, Long.class) //
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
}
