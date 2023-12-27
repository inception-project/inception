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
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_OVERLAP;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_CORRECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AutoAcceptMode.ON_FIRST_ACCESS;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.AL_SIDEBAR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.AUTO_ACCEPT;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.CORRECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup.groupsOfType;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionType.RELATION;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionType.SPAN;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionCapability.PREDICTION_USES_TEXT_ONLY;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_NOT_SUPPORTED;
import static de.tudarmstadt.ukp.inception.rendering.model.Range.rangeCoveringDocument;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.SPAN_TYPE;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectAt;

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
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;
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
import org.springframework.lang.Nullable;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.TrimUtils;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
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
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AutoAcceptMode;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Position;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Progress;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender_;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderDeletedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderTaskNotificationEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderUpdatedEvent;
import de.tudarmstadt.ukp.inception.recommendation.model.DirtySpot;
import de.tudarmstadt.ukp.inception.recommendation.tasks.NonTrainableRecommenderActivationTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.PredictionTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.SelectionTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.TrainingTask;
import de.tudarmstadt.ukp.inception.recommendation.util.OverlapIterator;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.scheduling.TaskMonitor;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationComparisonUtils;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.support.StopWatch;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.logging.LogMessageGroup;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil;

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
    private static final String AUTO_ACCEPT_ON_FIRST_ACCESS = "on-first-access";

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
            EntityManager aEntityManager, ApplicationEventPublisher aApplicationEventPublisher)
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

        trainingTaskCounter = new ConcurrentHashMap<>();
        states = new ConcurrentHashMap<>();
    }

    public RecommendationServiceImpl(PreferencesService aPreferencesService,
            SessionRegistry aSessionRegistry, UserDao aUserRepository,
            RecommenderFactoryRegistry aRecommenderFactoryRegistry,
            SchedulingService aSchedulingService, AnnotationSchemaService aAnnoService,
            DocumentService aDocumentService, EntityManager aEntityManager)
    {
        this(aPreferencesService, aSessionRegistry, aUserRepository, aRecommenderFactoryRegistry,
                aSchedulingService, aAnnoService, aDocumentService, (ProjectService) null,
                aEntityManager, null);
    }

    @Override
    public Predictions getPredictions(User aUser, Project aProject)
    {
        RecommendationState state = getState(aUser.getUsername(), aProject);
        return state.getActivePredictions();
    }

    @Override
    public Predictions getIncomingPredictions(User aUser, Project aProject)
    {
        RecommendationState state = getState(aUser.getUsername(), aProject);
        return state.getIncomingPredictions();
    }

    @Override
    public void putIncomingPredictions(User aUser, Project aProject, Predictions aPredictions)
    {
        RecommendationState state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            state.setIncomingPredictions(aPredictions);
        }
    }

    @Override
    public boolean hasActiveRecommenders(String aUser, Project aProject)
    {
        RecommendationState state = getState(aUser, aProject);
        synchronized (state) {
            return !state.getActiveRecommenders().isEmpty();
        }
    }

    @Override
    public void setEvaluatedRecommenders(User aUser, AnnotationLayer aLayer,
            List<EvaluatedRecommender> aRecommenders)
    {
        RecommendationState state = getState(aUser.getUsername(), aLayer.getProject());
        synchronized (state) {
            state.setEvaluatedRecommenders(aLayer, aRecommenders);
        }
    }

    @Override
    public List<EvaluatedRecommender> getEvaluatedRecommenders(User aUser, AnnotationLayer aLayer)
    {
        RecommendationState state = getState(aUser.getUsername(), aLayer.getProject());
        synchronized (state) {
            return new ArrayList<>(state.getEvaluatedRecommenders().get(aLayer));
        }
    }

    @Override
    public Optional<EvaluatedRecommender> getEvaluatedRecommender(User aUser,
            Recommender aRecommender)
    {
        RecommendationState state = getState(aUser.getUsername(), aRecommender.getProject());
        synchronized (state) {
            return state.getEvaluatedRecommenders().get(aRecommender.getLayer()).stream()
                    .filter(r -> r.getRecommender().equals(aRecommender)).findAny();
        }
    }

    @Override
    public List<EvaluatedRecommender> getActiveRecommenders(User aUser, AnnotationLayer aLayer)
    {
        RecommendationState state = getState(aUser.getUsername(), aLayer.getProject());
        synchronized (state) {
            return new ArrayList<>(state.getActiveRecommenders().get(aLayer));
        }
    }

    @Override
    public List<EvaluatedRecommender> getActiveRecommenders(User aUser, Project aProject)
    {
        RecommendationState state = getState(aUser.getUsername(), aProject);
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
        Recommender settings = aRecommender;

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
        List<Recommender> settings = entityManager
                .createQuery("FROM Recommender WHERE project = :project ORDER BY name ASC",
                        Recommender.class)
                .setParameter("project", aProject).getResultList();
        return settings;
    }

    @Override
    public List<AnnotationLayer> listLayersWithEnabledRecommenders(Project aProject)
    {
        String query = "SELECT DISTINCT r.layer " + "FROM Recommender r "
                + "WHERE r.project = :project AND r.enabled = :enabled "
                + "ORDER BY r.layer.name ASC";

        return entityManager.createQuery(query, AnnotationLayer.class) //
                .setParameter("project", aProject) //
                .setParameter("enabled", true) //
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Recommender getRecommender(long aId)
    {
        return entityManager.find(Recommender.class, aId);
    }

    @Override
    @Transactional
    public boolean existsRecommender(Project aProject, String aName)
    {
        String query = String.join("\n", //
                "SELECT COUNT(*)", "FROM Recommender ", //
                "WHERE name = :name ", //
                "AND project = :project");

        long count = entityManager.createQuery(query, Long.class) //
                .setParameter("name", aName) //
                .setParameter("project", aProject) //
                .getSingleResult();

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

        boolean predictionSessionExistedOnOpen = false;
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
        String trigger = aEvent.getClass().getSimpleName();
        if (!predictionSessionExistedOnOpen) {
            // Activate all non-trainable recommenders - execute synchronously - blocking
            schedulingService.executeSync(
                    new NonTrainableRecommenderActivationTask(sessionOwner, project, trigger));
        }

        // Check if we need to wait for the initial recommender run before displaying the document
        // to the user
        boolean predictionTriggered = nonTrainableRecommenderRunSync(doc, predictions, sessionOwner,
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
                if (prediction instanceof SpanSuggestion) {
                    var spanPrediction = (SpanSuggestion) prediction;
                    acceptOrCorrectSuggestion(null, aDocument, aUser.getUsername(), cas,
                            (SpanAdapter) adapter, feature, spanPrediction, AUTO_ACCEPT, ACCEPTED);
                    count++;
                }

                if (prediction instanceof RelationSuggestion) {
                    var relationPrediction = (RelationSuggestion) prediction;
                    acceptSuggestion(null, aDocument, aUser.getUsername(), cas,
                            (RelationAdapter) adapter, feature, relationPrediction, AUTO_ACCEPT,
                            ACCEPTED);
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
        RequestCycle requestCycle = RequestCycle.get();

        if (requestCycle == null) {
            return;
        }

        Set<DirtySpot> dirties = requestCycle.getMetaData(DIRTIES);
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
        RequestCycle requestCycle = RequestCycle.get();

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

        boolean containsTrainingTrigger = false;
        for (IRequestCycleListener listener : requestCycle.getListeners()) {
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

    @Override
    public void triggerPrediction(String aUsername, String aEventName, SourceDocument aDocument,
            String aDataOwner)
    {
        User user = userRepository.get(aUsername);
        if (user == null) {
            return;
        }

        schedulingService.enqueue(new PredictionTask(user, aEventName, aDocument, aDataOwner));
    }

    @Override
    public void triggerTrainingAndPrediction(String aSessionOwner, Project aProject,
            String aEventName, SourceDocument aCurrentDocument, String aDataOwner)
    {
        triggerRun(aSessionOwner, aProject, aEventName, aCurrentDocument, aDataOwner, false, null);
    }

    @Override
    public void triggerSelectionTrainingAndPrediction(String aSessionOwner, Project aProject,
            String aEventName, SourceDocument aCurrentDocument, String aDataOwner)
    {
        triggerRun(aSessionOwner, aProject, aEventName, aCurrentDocument, aDataOwner, true, null);
    }

    private void triggerRun(String aSessionOwner, Project aProject, String aEventName,
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
            Task task = new SelectionTask(user, aProject, aEventName, aCurrentDocument, aDataOwner);
            schedulingService.enqueue(task);

            var state = getState(aSessionOwner, aProject);
            synchronized (state) {
                state.setPredictionsUntilNextEvaluation(TRAININGS_PER_SELECTION - 1);
                state.setPredictionsSinceLastEvaluation(0);
            }

            return;
        }

        var task = new TrainingTask(user, aProject, aEventName, aCurrentDocument, aDataOwner);
        schedulingService.enqueue(task);

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
        SessionInformation info = sessionRegistry.getSessionInformation(event.getId());
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
        SourceDocument currentDocument = aEvent.getDocument().getDocument();
        String currentUser = aEvent.getDocument().getUser();
        clearState(currentUser);
        deleteLearningRecords(currentDocument, currentUser);
    }

    @Override
    public Preferences getPreferences(User aUser, Project aProject)
    {
        RecommendationState state = getState(aUser.getUsername(), aProject);
        return state.getPreferences();
    }

    @Override
    public void setPreferences(User aUser, Project aProject, Preferences aPreferences)
    {
        RecommendationState state = getState(aUser.getUsername(), aProject);
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
        RecommendationState state = getState(aSessionOwner, aProject);
        synchronized (state) {
            return state.switchPredictions();
        }
    }

    @Override
    public Optional<RecommenderContext> getContext(String aSessionOwner, Recommender aRecommender)
    {
        RecommendationState state = getState(aSessionOwner, aRecommender.getProject());
        synchronized (state) {
            return state.getContext(aRecommender);
        }
    }

    @Override
    public void putContext(User aUser, Recommender aRecommender, RecommenderContext aContext)
    {
        RecommendationState state = getState(aUser.getUsername(), aRecommender.getProject());
        synchronized (state) {
            state.putContext(aRecommender, aContext);
        }
    }

    @Override
    @Transactional
    public AnnotationFS correctSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, SpanAdapter aAdapter, AnnotationFeature aFeature,
            SpanSuggestion aOriginalSuggestion, SpanSuggestion aCorrectedSuggestion,
            LearningRecordChangeLocation aLocation)
        throws AnnotationException
    {
        // If the action was a correction (i.e. suggestion label != annotation value) then generate
        // a rejection for the original value - we do not want the original value to re-appear
        logRecord(aSessionOwner, aDocument, aDataOwner, aOriginalSuggestion, aFeature, REJECTED,
                AL_SIDEBAR);

        return acceptOrCorrectSuggestion(aSessionOwner, aDocument, aDataOwner, aCas, aAdapter,
                aFeature, aCorrectedSuggestion, aLocation, CORRECTED);
    }

    @Override
    @Transactional
    public AnnotationFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, SpanAdapter aAdapter, AnnotationFeature aFeature,
            SpanSuggestion aSuggestion, LearningRecordChangeLocation aLocation)
        throws AnnotationException
    {
        return acceptOrCorrectSuggestion(aSessionOwner, aDocument, aDataOwner, aCas, aAdapter,
                aFeature, aSuggestion, aLocation, ACCEPTED);
    }

    private AnnotationFS acceptOrCorrectSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, SpanAdapter aAdapter, AnnotationFeature aFeature,
            SpanSuggestion aSuggestion, LearningRecordChangeLocation aLocation,
            LearningRecordType aAction)
        throws AnnotationException
    {
        var aBegin = aSuggestion.getBegin();
        var aEnd = aSuggestion.getEnd();
        var aValue = aSuggestion.getLabel();

        var candidates = aCas.<Annotation> select(aAdapter.getAnnotationTypeName()) //
                .at(aBegin, aEnd) //
                .asList();

        var candidateWithEmptyLabel = candidates.stream() //
                .filter(c -> aAdapter.getFeatureValue(aFeature, c) == null) //
                .findFirst();

        AnnotationFS annotation;
        if (candidateWithEmptyLabel.isPresent()) {
            // If there is an annotation where the predicted feature is unset, use it ...
            annotation = candidateWithEmptyLabel.get();
        }
        else if (candidates.isEmpty() || aAdapter.getLayer().isAllowStacking()) {
            // ... if not or if stacking is allowed, then we create a new annotation - this also
            // takes care of attaching to an annotation if necessary
            var newAnnotation = aAdapter.add(aDocument, aDataOwner, aCas, aBegin, aEnd);
            annotation = newAnnotation;
        }
        else {
            // ... if yes and stacking is not allowed, then we update the feature on the existing
            // annotation
            annotation = candidates.get(0);
        }

        commmitAcceptedLabel(aSessionOwner, aDocument, aDataOwner, aCas, aAdapter, aFeature,
                aSuggestion, aValue, annotation, aLocation, aAction);

        return annotation;
    }

    private void commmitAcceptedLabel(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, TypeAdapter aAdapter, AnnotationFeature aFeature,
            AnnotationSuggestion aSuggestion, String aValue, AnnotationFS annotation,
            LearningRecordChangeLocation aLocation, LearningRecordType aAction)
        throws AnnotationException
    {
        // Update the feature value
        aAdapter.setFeatureValue(aDocument, aDataOwner, aCas, ICasUtil.getAddr(annotation),
                aFeature, aValue);

        // Hide the suggestion. This is faster than having to recalculate the visibility status for
        // the entire document or even for the part visible on screen.
        aSuggestion
                .hide((aAction == ACCEPTED) ? FLAG_TRANSIENT_ACCEPTED : FLAG_TRANSIENT_CORRECTED);

        // Log the action to the learning record
        if (!aAdapter.isSilenced()) {
            logRecord(aSessionOwner, aDocument, aDataOwner, aSuggestion, aFeature, aAction,
                    aLocation);

            // Send an application event that the suggestion has been accepted
            aAdapter.publishEvent(() -> new RecommendationAcceptedEvent(this, aDocument, aDataOwner,
                    annotation, aFeature, aSuggestion.getLabel()));
        }
    }

    @Override
    @Transactional
    public AnnotationFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, RelationAdapter aAdapter, AnnotationFeature aFeature,
            RelationSuggestion aSuggestion, LearningRecordChangeLocation aLocation,
            LearningRecordType aAction)
        throws AnnotationException
    {
        var sourceBegin = aSuggestion.getPosition().getSourceBegin();
        var sourceEnd = aSuggestion.getPosition().getSourceEnd();
        var targetBegin = aSuggestion.getPosition().getTargetBegin();
        var targetEnd = aSuggestion.getPosition().getTargetEnd();

        // Check if there is already a relation for the given source and target
        var type = CasUtil.getType(aCas, aAdapter.getAnnotationTypeName());
        var attachType = CasUtil.getType(aCas, aAdapter.getAttachTypeName());

        var sourceFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        var targetFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        // The begin and end feature of a relation in the CAS are of the dependent/target
        // annotation. See also RelationAdapter::createRelationAnnotation.
        // We use that fact to search for existing relations for this relation suggestion
        var candidates = new ArrayList<AnnotationFS>();
        for (AnnotationFS relationCandidate : selectAt(aCas, type, targetBegin, targetEnd)) {
            AnnotationFS source = (AnnotationFS) relationCandidate.getFeatureValue(sourceFeature);
            AnnotationFS target = (AnnotationFS) relationCandidate.getFeatureValue(targetFeature);

            if (source == null || target == null) {
                continue;
            }

            if (source.getBegin() == sourceBegin && source.getEnd() == sourceEnd
                    && target.getBegin() == targetBegin && target.getEnd() == targetEnd) {
                candidates.add(relationCandidate);
            }
        }

        AnnotationFS annotation = null;
        if (candidates.size() == 1) {
            // One candidate, we just return it
            annotation = candidates.get(0);
        }
        else if (candidates.size() == 2) {
            LOG.warn("Found multiple candidates for upserting relation from suggestion");
            annotation = candidates.get(0);
        }

        // We did not find a relation for this suggestion, so we create a new one
        if (annotation == null) {
            // FIXME: We get the first match for the (begin, end) span. With stacking, there can
            // be more than one and we need to get the right one then which does not need to be
            // the first. We wait for #2135 to fix this. When stacking is enabled, then also
            // consider creating a new relation instead of upserting an existing one.

            var source = selectAt(aCas, attachType, sourceBegin, sourceEnd).stream().findFirst()
                    .orElse(null);
            var target = selectAt(aCas, attachType, targetBegin, targetEnd).stream().findFirst()
                    .orElse(null);

            if (source == null || target == null) {
                String msg = "Cannot find source or target annotation for upserting relation";
                LOG.error(msg);
                throw new IllegalStateException(msg);
            }

            annotation = aAdapter.add(aDocument, aDataOwner, source, target, aCas);
        }

        commmitAcceptedLabel(aSessionOwner, aDocument, aDataOwner, aCas, aAdapter, aFeature,
                aSuggestion, aSuggestion.getLabel(), annotation, aLocation, aAction);

        return annotation;
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
        var groupedSuggestions = groupsOfType(SpanSuggestion.class, suggestions);
        calculateSpanSuggestionVisibility(sessionOwner.getUsername(), aDocument, aOriginalCas,
                aIncomingPredictions.getDataOwner(), aEngine.getRecommender().getLayer(),
                groupedSuggestions, 0, aOriginalCas.getDocumentText().length());
        // FIXME calculateRelationSuggestionVisibility?

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

    static List<AnnotationSuggestion> extractSuggestions(int aGeneration, CAS aOriginalCas,
            CAS aPredictionCas, SourceDocument aDocument, Recommender aRecommender)
    {
        var layer = aRecommender.getLayer();
        var featureName = aRecommender.getFeature().getName();
        var typeName = layer.getName();

        var predictedType = CasUtil.getType(aPredictionCas, typeName);
        var labelFeature = predictedType.getFeatureByBaseName(featureName);
        var sourceFeature = predictedType.getFeatureByBaseName(FEAT_REL_SOURCE);
        var targetFeature = predictedType.getFeatureByBaseName(FEAT_REL_TARGET);
        var scoreFeature = predictedType
                .getFeatureByBaseName(featureName + FEATURE_NAME_SCORE_SUFFIX);
        var scoreExplanationFeature = predictedType
                .getFeatureByBaseName(featureName + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX);
        var modeFeature = predictedType
                .getFeatureByBaseName(featureName + FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX);
        var predictionFeature = predictedType.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);
        var isMultiLabels = TYPE_NAME_STRING_ARRAY.equals(labelFeature.getRange().getName());

        var result = new ArrayList<AnnotationSuggestion>();

        var documentText = aOriginalCas.getDocumentText();
        for (var predictedFS : aPredictionCas.select(predictedType)) {
            if (!predictedFS.getBooleanValue(predictionFeature)) {
                continue;
            }

            var autoAcceptMode = getAutoAcceptMode(predictedFS, modeFeature);
            var labels = getPredictedLabels(predictedFS, labelFeature, isMultiLabels);
            var score = predictedFS.getDoubleValue(scoreFeature);
            var scoreExplanation = predictedFS.getStringValue(scoreExplanationFeature);

            switch (layer.getType()) {
            case SPAN_TYPE: {
                var predictedAnnotation = (Annotation) predictedFS;
                var targetOffsets = getOffsets(layer.getAnchoringMode(), aOriginalCas,
                        predictedAnnotation);

                if (targetOffsets.isEmpty()) {
                    continue;
                }

                var offsets = targetOffsets.get();
                var coveredText = documentText.substring(offsets.getBegin(), offsets.getEnd());

                for (var label : labels) {
                    var suggestion = SpanSuggestion.builder() //
                            .withId(RelationSuggestion.NEW_ID) //
                            .withGeneration(aGeneration) //
                            .withRecommender(aRecommender) //
                            .withDocumentName(aDocument.getName()) //
                            .withPosition(offsets) //
                            .withCoveredText(coveredText) //
                            .withLabel(label) //
                            .withUiLabel(label) //
                            .withScore(score) //
                            .withScoreExplanation(scoreExplanation) //
                            .withAutoAcceptMode(autoAcceptMode) //
                            .build();
                    result.add(suggestion);
                }
                break;
            }
            case RELATION_TYPE: {
                var source = (AnnotationFS) predictedFS.getFeatureValue(sourceFeature);
                var target = (AnnotationFS) predictedFS.getFeatureValue(targetFeature);

                var originalSource = findEquivalent(aOriginalCas, source);
                var originalTarget = findEquivalent(aOriginalCas, target);
                if (originalSource.isEmpty() || originalTarget.isEmpty()) {
                    continue;
                }

                var position = new RelationPosition(originalSource.get(), originalTarget.get());

                for (var label : labels) {
                    var suggestion = RelationSuggestion.builder() //
                            .withId(RelationSuggestion.NEW_ID) //
                            .withGeneration(aGeneration) //
                            .withRecommender(aRecommender) //
                            .withDocumentName(aDocument.getName()) //
                            .withPosition(position).withLabel(label) //
                            .withUiLabel(label) //
                            .withScore(score) //
                            .withScoreExplanation(scoreExplanation) //
                            .withAutoAcceptMode(autoAcceptMode) //
                            .build();
                    result.add(suggestion);
                }
                break;
            }
            default:
                throw new IllegalStateException("Unsupported layer type [" + layer.getType() + "]");
            }
        }

        return result;
    }

    private static AutoAcceptMode getAutoAcceptMode(FeatureStructure aFS, Feature aModeFeature)
    {
        var autoAcceptMode = AutoAcceptMode.NEVER;
        var autoAcceptFeatureValue = aFS.getStringValue(aModeFeature);
        if (autoAcceptFeatureValue != null) {
            switch (autoAcceptFeatureValue) {
            case AUTO_ACCEPT_ON_FIRST_ACCESS:
                autoAcceptMode = AutoAcceptMode.ON_FIRST_ACCESS;
            }
        }
        return autoAcceptMode;
    }

    private static String[] getPredictedLabels(FeatureStructure predictedFS,
            Feature predictedFeature, boolean isStringMultiValue)
    {
        if (isStringMultiValue) {
            return FSUtil.getFeature(predictedFS, predictedFeature, String[].class);
        }

        return new String[] { predictedFS.getFeatureValueAsString(predictedFeature) };
    }

    /**
     * Locates an annotation in the given CAS which is equivalent of the provided annotation.
     *
     * @param aOriginalCas
     *            the original CAS.
     * @param aAnnotation
     *            an annotation in the prediction CAS. return the equivalent in the original CAS.
     */
    private static Optional<Annotation> findEquivalent(CAS aOriginalCas, AnnotationFS aAnnotation)
    {
        return aOriginalCas.<Annotation> select(aAnnotation.getType())
                .filter(candidate -> AnnotationComparisonUtils.isEquivalentSpanAnnotation(candidate,
                        aAnnotation, null))
                .findFirst();
    }

    /**
     * Calculates the offsets of the given predicted annotation in the original CAS .
     *
     * @param aMode
     *            the anchoring mode of the target layer
     * @param aOriginalCas
     *            the original CAS.
     * @param aPredictedAnnotation
     *            the predicted annotation.
     * @return the proper offsets.
     */
    static Optional<Offset> getOffsets(AnchoringMode aMode, CAS aOriginalCas,
            Annotation aPredictedAnnotation)
    {
        switch (aMode) {
        case CHARACTERS: {
            return getOffsetsAnchoredOnCharacters(aOriginalCas, aPredictedAnnotation);
        }
        case SINGLE_TOKEN: {
            return getOffsetsAnchoredOnSingleTokens(aOriginalCas, aPredictedAnnotation);
        }
        case TOKENS: {
            return getOffsetsAnchoredOnTokens(aOriginalCas, aPredictedAnnotation);
        }
        case SENTENCES: {
            return getOffsetsAnchoredOnSentences(aOriginalCas, aPredictedAnnotation);
        }
        default:
            throw new IllegalStateException("Unsupported anchoring mode: [" + aMode + "]");
        }
    }

    private static Optional<Offset> getOffsetsAnchoredOnCharacters(CAS aOriginalCas,
            Annotation aPredictedAnnotation)
    {
        int[] offsets = { max(aPredictedAnnotation.getBegin(), 0),
                min(aOriginalCas.getDocumentText().length(), aPredictedAnnotation.getEnd()) };
        TrimUtils.trim(aPredictedAnnotation.getCAS().getDocumentText(), offsets);
        var begin = offsets[0];
        var end = offsets[1];
        return Optional.of(new Offset(begin, end));
    }

    private static Optional<Offset> getOffsetsAnchoredOnSingleTokens(CAS aOriginalCas,
            Annotation aPredictedAnnotation)
    {
        Type tokenType = getType(aOriginalCas, Token.class);
        var tokens = aOriginalCas.<Annotation> select(tokenType) //
                .coveredBy(aPredictedAnnotation) //
                .limit(2).asList();

        if (tokens.isEmpty()) {
            // This can happen if a recommender uses different token boundaries (e.g. if a
            // remote service performs its own tokenization). We might be smart here by
            // looking for overlapping tokens instead of contained tokens.
            LOG.trace("Discarding suggestion because no covering token was found: {}",
                    aPredictedAnnotation);
            return Optional.empty();
        }

        if (tokens.size() > 1) {
            // We only want to accept single-token suggestions
            LOG.trace("Discarding suggestion because only single-token suggestions are "
                    + "accepted: {}", aPredictedAnnotation);
            return Optional.empty();
        }

        AnnotationFS token = tokens.get(0);
        var begin = token.getBegin();
        var end = token.getEnd();
        return Optional.of(new Offset(begin, end));
    }

    private static Optional<Offset> getOffsetsAnchoredOnSentences(CAS aOriginalCas,
            Annotation aPredictedAnnotation)
    {
        var sentences = aOriginalCas.select(Sentence.class) //
                .coveredBy(aPredictedAnnotation) //
                .asList();

        if (sentences.isEmpty()) {
            // This can happen if a recommender uses different token boundaries (e.g. if a
            // remote service performs its own tokenization). We might be smart here by
            // looking for overlapping sentences instead of covered sentences.
            LOG.trace("Discarding suggestion because no covered sentences were found: {}",
                    aPredictedAnnotation);
            return Optional.empty();
        }

        var begin = sentences.get(0).getBegin();
        var end = sentences.get(sentences.size() - 1).getEnd();
        return Optional.of(new Offset(begin, end));
    }

    static Optional<Offset> getOffsetsAnchoredOnTokens(CAS aOriginalCas,
            Annotation aPredictedAnnotation)
    {
        var tokens = aOriginalCas.select(Token.class) //
                .coveredBy(aPredictedAnnotation) //
                .asList();

        if (tokens.isEmpty()) {
            if (aPredictedAnnotation.getBegin() == aPredictedAnnotation.getEnd()) {
                var pos = aPredictedAnnotation.getBegin();
                var allTokens = aOriginalCas.select(Token.class).asList();
                Token prevToken = null;
                for (var token : allTokens) {
                    if (prevToken == null && pos < token.getBegin()) {
                        return Optional.of(new Offset(token.getBegin(), token.getBegin()));
                    }

                    if (token.covering(aPredictedAnnotation)) {
                        return Optional.of(new Offset(pos, pos));
                    }

                    if (prevToken != null && pos < token.getBegin()) {
                        return Optional.of(new Offset(prevToken.getEnd(), prevToken.getEnd()));
                    }

                    prevToken = token;
                }

                if (prevToken != null && pos >= prevToken.getEnd()) {
                    return Optional.of(new Offset(prevToken.getEnd(), prevToken.getEnd()));
                }
            }

            // This can happen if a recommender uses different token boundaries (e.g. if a
            // remote service performs its own tokenization). We might be smart here by
            // looking for overlapping tokens instead of covered tokens.
            LOG.trace("Discarding suggestion because no covered tokens were found: {}",
                    aPredictedAnnotation);
            return Optional.empty();
        }

        var begin = tokens.get(0).getBegin();
        var end = tokens.get(tokens.size() - 1).getEnd();
        return Optional.of(new Offset(begin, end));
    }

    /**
     * Goes through all SpanSuggestions and determines the visibility of each one
     */
    @Override
    public void calculateSpanSuggestionVisibility(String aSessionOwner, SourceDocument aDocument,
            CAS aCas, String aDataOwner, AnnotationLayer aLayer,
            Collection<SuggestionGroup<SpanSuggestion>> aRecommendations, int aWindowBegin,
            int aWindowEnd)
    {
        LOG.trace(
                "calculateSpanSuggestionVisibility() for layer {} on document {} in range [{}, {}]",
                aLayer, aDocument, aWindowBegin, aWindowEnd);

        var type = getAnnotationType(aCas, aLayer);
        if (type == null) {
            // The type does not exist in the type system of the CAS. Probably it has not
            // been upgraded to the latest version of the type system yet. If this is the case,
            // we'll just skip.
            return;
        }

        var annotationsInWindow = getAnnotationsInWindow(aCas, type, aWindowBegin, aWindowEnd);

        // Collect all suggestions of the given layer within the view window
        var suggestionsInWindow = aRecommendations.stream()
                // Only suggestions for the given layer
                .filter(group -> group.getLayerId() == aLayer.getId())
                // ... and in the given window
                .filter(group -> {
                    Offset offset = (Offset) group.getPosition();
                    return aWindowBegin <= offset.getBegin() && offset.getEnd() <= aWindowEnd;
                }) //
                .collect(toList());

        // Get all the skipped/rejected entries for the current layer
        var recordedAnnotations = listLearningRecords(aSessionOwner, aDataOwner, aLayer);

        for (var feature : schemaService.listSupportedFeatures(aLayer)) {
            var feat = type.getFeatureByBaseName(feature.getName());

            if (feat == null) {
                // The feature does not exist in the type system of the CAS. Probably it has not
                // been upgraded to the latest version of the type system yet. If this is the case,
                // we'll just skip.
                return;
            }

            // Reduce the suggestions to the ones for the given feature. We can use the tree here
            // since we only have a single SuggestionGroup for every position
            var suggestions = new TreeMap<Offset, SuggestionGroup<SpanSuggestion>>(
                    comparingInt(Offset::getBegin).thenComparingInt(Offset::getEnd));
            suggestionsInWindow.stream()
                    .filter(group -> group.getFeature().equals(feature.getName())) //
                    .map(group -> {
                        group.showAll(AnnotationSuggestion.FLAG_ALL);
                        return group;
                    }) //
                    .forEach(group -> suggestions.put((Offset) group.getPosition(), group));

            hideSpanSuggestionsThatOverlapWithAnnotations(annotationsInWindow, feature, feat,
                    suggestions);

            // Anything that was not hidden so far might still have been rejected
            suggestions.values().stream() //
                    .flatMap(SuggestionGroup::stream) //
                    .filter(AnnotationSuggestion::isVisible) //
                    .forEach(suggestion -> hideSuggestionsRejectedOrSkipped(suggestion,
                            recordedAnnotations));
        }
    }

    private void hideSpanSuggestionsThatOverlapWithAnnotations(
            List<AnnotationFS> annotationsInWindow, AnnotationFeature feature, Feature feat,
            Map<Offset, SuggestionGroup<SpanSuggestion>> suggestions)
    {
        // If there are no suggestions or annotations, there is nothing to do here
        if (annotationsInWindow.isEmpty() || suggestions.isEmpty()) {
            return;
        }

        // Reduce the annotations to the ones which have a non-null feature value. We need to
        // use a multi-valued map here because there may be multiple annotations at a
        // given position.
        var annotations = new ArrayListValuedHashMap<Offset, AnnotationFS>();
        annotationsInWindow
                .forEach(fs -> annotations.put(new Offset(fs.getBegin(), fs.getEnd()), fs));

        // We need to constructed a sorted list of the keys for the OverlapIterator below
        var sortedAnnotationKeys = new ArrayList<Offset>(annotations.keySet());
        sortedAnnotationKeys.sort(comparingInt(Offset::getBegin).thenComparingInt(Offset::getEnd));

        // This iterator gives us pairs of annotations and suggestions. Note that both lists
        // must be sorted in the same way. The suggestion offsets are sorted because they are
        // the keys in a TreeSet - and the annotation offsets are sorted in the same way manually
        var oi = new OverlapIterator(new ArrayList<>(suggestions.keySet()), sortedAnnotationKeys);

        // Bulk-hide any groups that overlap with existing annotations on the current layer
        // and for the current feature
        var hiddenForOverlap = new ArrayList<AnnotationSuggestion>();
        while (oi.hasNext()) {
            var pair = oi.next();
            var suggestionOffset = pair.getKey();
            var annotationOffset = pair.getValue();
            // Fetch the current suggestion and annotation
            var group = suggestions.get(suggestionOffset);
            for (var annotation : annotations.get(annotationOffset)) {
                var label = annotation.getFeatureValueAsString(feat);
                for (var suggestion : group) {
                    // The suggestion would just create an annotation and not set any
                    // feature
                    boolean colocated = colocated(annotation, suggestion.getBegin(),
                            suggestion.getEnd());
                    if (suggestion.getLabel() == null) {
                        // If there is already an annotation, then we hide any suggestions
                        // that would just trigger the creation of the same annotation and
                        // not set any new feature. This applies whether stacking is allowed
                        // or not.
                        if (colocated) {
                            suggestion.hide(FLAG_OVERLAP);
                            hiddenForOverlap.add(suggestion);
                            continue;
                        }

                        // If stacking is enabled, we do allow suggestions that create an
                        // annotation with no label, but only if the offsets differ
                        if (!(feature.getLayer().isAllowStacking() && !colocated)) {
                            suggestion.hide(FLAG_OVERLAP);
                            hiddenForOverlap.add(suggestion);
                            continue;
                        }
                    }
                    // The suggestion would merge the suggested feature value into an
                    // existing annotation or create a new annotation with the feature if
                    // stacking were enabled.
                    else {
                        // Is the feature still unset in the current annotation - i.e. would
                        // accepting the suggestion merge the feature into it? If yes, we do
                        // not hide
                        if (label == null && colocated) {
                            continue;
                        }

                        // Does the suggested label match the label of an existing annotation
                        // at the same position then we hide
                        if (label != null && label.equals(suggestion.getLabel()) && colocated) {
                            suggestion.hide(FLAG_OVERLAP);
                            hiddenForOverlap.add(suggestion);
                            continue;
                        }

                        // Would accepting the suggestion create a new annotation but
                        // stacking is not enabled - then we need to hide
                        if (!feature.getLayer().isAllowStacking()) {
                            suggestion.hide(FLAG_OVERLAP);
                            hiddenForOverlap.add(suggestion);
                            continue;
                        }
                    }
                }
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Hidden due to overlapping: {}", hiddenForOverlap.size());
            for (var s : hiddenForOverlap) {
                LOG.trace("- {}", s);
            }
        }
    }

    @Override
    public void calculateRelationSuggestionVisibility(String aSessionOwner, CAS aCas, String aUser,
            AnnotationLayer aLayer,
            Collection<SuggestionGroup<RelationSuggestion>> aRecommendations, int aWindowBegin,
            int aWindowEnd)
    {
        var type = getAnnotationType(aCas, aLayer);

        if (type == null) {
            // The type does not exist in the type system of the CAS. Probably it has not
            // been upgraded to the latest version of the type system yet. If this is the case,
            // we'll just skip.
            return;
        }

        var governorFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        var dependentFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        if (dependentFeature == null || governorFeature == null) {
            LOG.warn("Missing Dependent or Governor feature on [{}]", aLayer.getName());
            return;
        }

        var annotationsInWindow = getAnnotationsInWindow(aCas, type, aWindowBegin, aWindowEnd);

        // Group annotations by relation position, that is (source, target) address
        MultiValuedMap<Position, AnnotationFS> groupedAnnotations = new ArrayListValuedHashMap<>();
        for (AnnotationFS annotationFS : annotationsInWindow) {
            var source = (AnnotationFS) annotationFS.getFeatureValue(governorFeature);
            var target = (AnnotationFS) annotationFS.getFeatureValue(dependentFeature);

            var relationPosition = new RelationPosition(source.getBegin(), source.getEnd(),
                    target.getBegin(), target.getEnd());

            groupedAnnotations.put(relationPosition, annotationFS);
        }

        // Collect all suggestions of the given layer
        var groupedSuggestions = aRecommendations.stream()
                .filter(group -> group.getLayerId() == aLayer.getId()) //
                .collect(toList());

        // Get previously rejected suggestions
        MultiValuedMap<Position, LearningRecord> groupedRecordedAnnotations = new ArrayListValuedHashMap<>();
        for (var learningRecord : listLearningRecords(aSessionOwner, aUser, aLayer)) {
            RelationPosition relationPosition = new RelationPosition(
                    learningRecord.getOffsetSourceBegin(), learningRecord.getOffsetSourceEnd(),
                    learningRecord.getOffsetTargetBegin(), learningRecord.getOffsetTargetEnd());

            groupedRecordedAnnotations.put(relationPosition, learningRecord);
        }

        for (AnnotationFeature feature : schemaService.listSupportedFeatures(aLayer)) {
            Feature feat = type.getFeatureByBaseName(feature.getName());

            if (feat == null) {
                // The feature does not exist in the type system of the CAS. Probably it has not
                // been upgraded to the latest version of the type system yet. If this is the case,
                // we'll just skip.
                return;
            }

            for (SuggestionGroup<RelationSuggestion> group : groupedSuggestions) {
                if (!feature.getName().equals(group.getFeature())) {
                    continue;
                }

                group.showAll(AnnotationSuggestion.FLAG_ALL);

                Position position = group.getPosition();

                // If any annotation at this position has a non-null label for this feature,
                // then we hide the suggestion group
                for (AnnotationFS annotationFS : groupedAnnotations.get(position)) {
                    if (annotationFS.getFeatureValueAsString(feat) != null) {
                        for (RelationSuggestion suggestion : group) {
                            suggestion.hide(FLAG_OVERLAP);
                        }
                    }
                }

                // Hide previously rejected suggestions
                for (LearningRecord learningRecord : groupedRecordedAnnotations.get(position)) {
                    for (RelationSuggestion suggestion : group) {
                        if (suggestion.labelEquals(learningRecord.getAnnotation())) {
                            suggestion.hideSuggestion(learningRecord.getUserAction());
                        }
                    }
                }
            }
        }
    }

    static void hideSuggestionsRejectedOrSkipped(SpanSuggestion aSuggestion,
            List<LearningRecord> aRecordedRecommendations)
    {
        aRecordedRecommendations.stream() //
                .filter(r -> Objects.equals(r.getLayer().getId(), aSuggestion.getLayerId())) //
                .filter(r -> Objects.equals(r.getAnnotationFeature().getName(),
                        aSuggestion.getFeature())) //
                .filter(r -> Objects.equals(r.getSourceDocument().getName(),
                        aSuggestion.getDocumentName())) //
                .filter(r -> aSuggestion.labelEquals(r.getAnnotation())) //
                .filter(r -> r.getOffsetBegin() == aSuggestion.getBegin()
                        && r.getOffsetEnd() == aSuggestion.getEnd()) //
                .filter(r -> aSuggestion.hideSuggestion(r.getUserAction())) //
                .findAny();
    }

    @Nullable
    private Type getAnnotationType(CAS aCas, AnnotationLayer aLayer)
    {
        // NOTE: In order to avoid having to upgrade the "original CAS" in computePredictions,this
        // method is implemented in such a way that it gracefully handles cases where the CAS and
        // the project type system are not in sync - specifically the CAS where the project defines
        // layers or features which do not exist in the CAS.

        try {
            return CasUtil.getType(aCas, aLayer.getName());
        }
        catch (IllegalArgumentException e) {
            // Type does not exist in the type system of the CAS. Probably it has not been upgraded
            // to the latest version of the type system yet. If this is the case, we'll just skip.
            return null;
        }
    }

    private List<AnnotationFS> getAnnotationsInWindow(CAS aCas, Type type, int aWindowBegin,
            int aWindowEnd)
    {
        if (type == null) {
            return List.of();
        }

        return select(aCas, type).stream()
                .filter(fs -> aWindowBegin <= fs.getBegin() && fs.getEnd() <= aWindowEnd)
                .collect(toList());
    }

    CAS cloneAndMonkeyPatchCAS(Project aProject, CAS aSourceCas, CAS aTargetCas)
        throws UIMAException, IOException
    {
        try (var watch = new StopWatch(LOG, "adding score features")) {
            var tsd = schemaService.getFullProjectTypeSystem(aProject);
            var features = schemaService.listAnnotationFeature(aProject);

            for (var feature : features) {
                var td = tsd.getType(feature.getLayer().getName());
                if (td == null) {
                    if (!WebAnnoConst.CHAIN_TYPE.equals(feature.getLayer().getType())) {
                        LOG.trace(
                                "Could not monkey patch feature {} because type for layer {} was not "
                                        + "found in the type system",
                                feature, feature.getLayer());
                    }
                    continue;
                }

                var scoreFeatureName = feature.getName() + FEATURE_NAME_SCORE_SUFFIX;
                td.addFeature(scoreFeatureName, "Score feature", TYPE_NAME_DOUBLE);

                var scoreExplanationFeatureName = feature.getName()
                        + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;
                td.addFeature(scoreExplanationFeatureName, "Score explanation feature",
                        TYPE_NAME_STRING);

                var modeFeatureName = feature.getName() + FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX;
                td.addFeature(modeFeatureName, "Suggestion mode", TYPE_NAME_STRING);
            }

            var layers = features.stream().map(AnnotationFeature::getLayer).distinct()
                    .collect(toList());
            for (var layer : layers) {
                var td = tsd.getType(layer.getName());
                if (td == null) {
                    LOG.trace("Could not monkey patch layer {} because its type was not found in "
                            + "the type system", layer);
                    continue;
                }

                td.addFeature(FEATURE_NAME_IS_PREDICTION, "Is Prediction", TYPE_NAME_BOOLEAN);
            }

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
            Set<DirtySpot> dirties = cycle.getMetaData(DIRTIES);
            Set<CommittedDocument> committed = cycle.getMetaData(COMMITTED);

            if (dirties == null || committed == null) {
                return;
            }

            // Any dirties which have not been committed can be ignored
            for (CommittedDocument committedDocument : committed) {
                dirties.removeIf(dirty -> dirty.affectsDocument(committedDocument.getDocumentId(),
                        committedDocument.getUser()));
            }

            // Concurrent action has deleted project, so we can ignore this
            var affectedProjects = dirties.stream() //
                    .map(d -> d.getProject()) //
                    .distinct() //
                    .collect(toMap(p -> p.getId(), p -> p));
            for (Project project : affectedProjects.values()) {
                if (projectService.getProject(project.getId()) == null) {
                    dirties.removeIf(dirty -> dirty.getProject().equals(project));
                }
            }

            Map<RecommendationStateKey, Set<DirtySpot>> dirtiesByContext = new LinkedHashMap<>();
            for (DirtySpot spot : dirties) {
                RecommendationStateKey key = new RecommendationStateKey(spot.getUser(),
                        spot.getProject().getId());
                dirtiesByContext.computeIfAbsent(key, k -> new HashSet<>()).add(spot);
            }

            for (var contextDirties : dirtiesByContext.entrySet()) {
                var key = contextDirties.getKey();
                triggerRun(key.getUser(), affectedProjects.get(key.getProjectId()),
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
    {
        // Hide the suggestion. This is faster than having to recalculate the visibility status for
        // the entire document or even for the part visible on screen.
        suggestion.hide(FLAG_TRANSIENT_REJECTED);

        if (suggestion instanceof SpanSuggestion) {
            var recommender = getRecommender(suggestion.getVID().getId());
            var feature = recommender.getFeature();
            var spanSuggestion = (SpanSuggestion) suggestion;
            // Log the action to the learning record
            logRecord(aSessionOwner, aDocument, aDataOwner, spanSuggestion, feature, REJECTED,
                    aAction);

            // Send an application event that the suggestion has been rejected
            applicationEventPublisher.publishEvent(new RecommendationRejectedEvent(this, aDocument,
                    aDataOwner, spanSuggestion.getBegin(), spanSuggestion.getEnd(),
                    spanSuggestion.getCoveredText(), feature, spanSuggestion.getLabel()));

        }
        else if (suggestion instanceof RelationSuggestion) {
            RelationSuggestion relationSuggestion = (RelationSuggestion) suggestion;
            // TODO: Log rejection
            // TODO: Publish rejection event
        }
    }

    @Override
    @Transactional
    public void skipSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion suggestion, LearningRecordChangeLocation aAction)
    {
        // Hide the suggestion. This is faster than having to recalculate the visibility status for
        // the entire document or even for the part visible on screen.
        suggestion.hide(FLAG_SKIPPED);

        if (suggestion instanceof SpanSuggestion) {
            var recommender = getRecommender(suggestion.getVID().getId());
            var feature = recommender.getFeature();
            var spanSuggestion = (SpanSuggestion) suggestion;
            // Log the action to the learning record
            logRecord(aSessionOwner, aDocument, aDataOwner, spanSuggestion, feature, SKIPPED,
                    aAction);

            // // Send an application event that the suggestion has been rejected
            // applicationEventPublisher.publishEvent(new RecommendationSkippedEvent(this,
            // aDocument,
            // aDataOwner, spanSuggestion.getBegin(), spanSuggestion.getEnd(),
            // spanSuggestion.getCoveredText(), feature, spanSuggestion.getLabel()));

        }
        else if (suggestion instanceof RelationSuggestion) {
            RelationSuggestion relationSuggestion = (RelationSuggestion) suggestion;
            // TODO: Log rejection
            // TODO: Publish rejection event
        }
    }

    @Transactional
    @Override
    public void logRecord(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, AnnotationFeature aFeature,
            LearningRecordType aUserAction, LearningRecordChangeLocation aLocation)
    {
        LearningRecord record = null;
        if (aSuggestion instanceof SpanSuggestion) {
            record = toLearningRecord(aDocument, aDataOwner, (SpanSuggestion) aSuggestion, aFeature,
                    aUserAction, aLocation);
        }
        else if (aSuggestion instanceof RelationSuggestion) {
            record = toLearningRecord(aDocument, aDataOwner, (RelationSuggestion) aSuggestion,
                    aFeature, aUserAction, aLocation);
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

    private LearningRecord toLearningRecord(SourceDocument aDocument, String aUsername,
            SpanSuggestion aSuggestion, AnnotationFeature aFeature, LearningRecordType aUserAction,
            LearningRecordChangeLocation aLocation)
    {
        var record = new LearningRecord();
        record.setUser(aUsername);
        record.setSourceDocument(aDocument);
        record.setUserAction(aUserAction);
        record.setOffsetBegin(aSuggestion.getBegin());
        record.setOffsetEnd(aSuggestion.getEnd());
        record.setOffsetBegin2(-1);
        record.setOffsetEnd2(-1);
        record.setTokenText(aSuggestion.getCoveredText());
        record.setAnnotation(aSuggestion.getLabel());
        record.setLayer(aFeature.getLayer());
        record.setSuggestionType(SPAN);
        record.setChangeLocation(aLocation);
        record.setAnnotationFeature(aFeature);
        return record;
    }

    private LearningRecord toLearningRecord(SourceDocument aDocument, String aDataOwner,
            RelationSuggestion aSuggestion, AnnotationFeature aFeature,
            LearningRecordType aUserAction, LearningRecordChangeLocation aLocation)
    {
        var pos = aSuggestion.getPosition();
        var record = new LearningRecord();
        record.setUser(aDataOwner);
        record.setSourceDocument(aDocument);
        record.setUserAction(aUserAction);
        record.setOffsetBegin(pos.getSourceBegin());
        record.setOffsetEnd(pos.getSourceEnd());
        record.setOffsetBegin2(pos.getTargetBegin());
        record.setOffsetEnd2(pos.getTargetEnd());
        record.setTokenText("");
        record.setAnnotation(aSuggestion.getLabel());
        record.setLayer(aFeature.getLayer());
        record.setSuggestionType(RELATION);
        record.setChangeLocation(aLocation);
        record.setAnnotationFeature(aFeature);
        return record;
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
                            && r.getUserAction() != LearningRecordType.SHOWN)
                    .collect(toUnmodifiableList());
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
                            && r.getUserAction() != LearningRecordType.SHOWN);
            if (aLimit > 0) {
                stream = stream.limit(aLimit);
            }
            return stream.collect(toUnmodifiableList());
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
                .setParameter("action", LearningRecordType.SHOWN); // SHOWN records NOT returned
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
