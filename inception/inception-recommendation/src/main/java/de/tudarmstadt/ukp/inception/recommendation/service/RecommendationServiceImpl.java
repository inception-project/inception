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

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_OVERLAP;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup.groupsOfType;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_NOT_SUPPORTED;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectAt;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
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

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterCasWrittenEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentResetEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeDocumentRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.StopWatch;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessageGroup;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.TrimUtils;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapter;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Position;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Progress;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderDeletedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderTaskEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderUpdatedEvent;
import de.tudarmstadt.ukp.inception.recommendation.model.DirtySpot;
import de.tudarmstadt.ukp.inception.recommendation.tasks.NonTrainableRecommenderActivationTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.PredictionTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.SelectionTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.TrainingTask;
import de.tudarmstadt.ukp.inception.recommendation.util.OverlapIterator;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationComparisonUtils;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

/**
 * The implementation of the RecommendationService.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RecommenderServiceAutoConfiguration#recommendationService}.
 * </p>
 */
public class RecommendationServiceImpl
    implements RecommendationService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int TRAININGS_PER_SELECTION = 5;

    private static final String PREDICTION_CAS = "predictionCas";

    private final EntityManager entityManager;

    private final SessionRegistry sessionRegistry;
    private final UserDao userRepository;
    private final RecommenderFactoryRegistry recommenderFactoryRegistry;
    private final SchedulingService schedulingService;
    private final AnnotationSchemaService annoService;
    private final DocumentService documentService;
    private final LearningRecordService learningRecordService;
    private final ProjectService projectService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PreferencesService preferencesService;

    private final ConcurrentMap<RecommendationStateKey, AtomicInteger> trainingTaskCounter;
    private final ConcurrentMap<RecommendationStateKey, RecommendationState> states;

    /*
     * Marks user/projects to which annotations were added during this request.
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
            DocumentService aDocumentService, LearningRecordService aLearningRecordService,
            ProjectService aProjectService, EntityManager aEntityManager,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        preferencesService = aPreferencesService;
        sessionRegistry = aSessionRegistry;
        userRepository = aUserRepository;
        recommenderFactoryRegistry = aRecommenderFactoryRegistry;
        schedulingService = aSchedulingService;
        annoService = aAnnoService;
        documentService = aDocumentService;
        learningRecordService = aLearningRecordService;
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
            DocumentService aDocumentService, LearningRecordService aLearningRecordService,
            EntityManager aEntityManager)
    {
        this(aPreferencesService, aSessionRegistry, aUserRepository, aRecommenderFactoryRegistry,
                aSchedulingService, aAnnoService, aDocumentService, aLearningRecordService,
                (ProjectService) null, aEntityManager, null);
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
        Project project = aEvent.getDocument().getProject();
        String username = aEvent.getAnnotator();
        SourceDocument doc = aEvent.getDocument();
        Predictions predictions = getState(username, project).getActivePredictions();

        User user = userRepository.get(username);
        if (user == null) {
            return;
        }

        // We want to get predictions from all trained recommenders immediately - be they externally
        // pre-trained or possibly internal recommenders that have been trained due to earlier
        // actions.
        if (predictions == null) {
            // Activate all non-trainable recommenders - execute synchronously - blocking
            schedulingService.executeSync(new NonTrainableRecommenderActivationTask(user, project,
                    "DocumentOpenedEvent"));
        }

        boolean predictionTriggered = false;
        if (predictions == null || !predictions.hasRunPredictionOnDocument(aEvent.getDocument())) {
            var settings = preferencesService
                    .loadDefaultTraitsForProject(KEY_RECOMMENDER_GENERAL_SETTINGS, project);
            if (settings.isWaitForRecommendersOnOpenDocument()) {
                schedulingService.executeSync(new PredictionTask(user, "DocumentOpenedEvent", doc));
                switchPredictions(user, project);
            }
        }

        // If there already is a state, we just re-use it. We only trigger a new training if no
        // predictions object has been created yet.
        if (predictions == null) {
            triggerTrainingAndPrediction(username, project, "DocumentOpenedEvent", doc);
            return;
        }

        if (predictions.hasRunPredictionOnDocument(aEvent.getDocument())) {
            log.debug(
                    "Not scheduling prediction task after document was opened as we already have predictions");
            return;
        }

        // If we already trained, predicted only for the last document and open a new document, we
        // start the predictions so that the user gets recommendations as quickly as possible
        // without any interaction needed
        if (!predictionTriggered) {
            triggerPrediction(username, "DocumentOpenedEvent", doc);
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

        Set<CommittedDocument> committed = requestCycle.getMetaData(COMMITTED);
        if (committed == null) {
            committed = new HashSet<>();
            requestCycle.setMetaData(COMMITTED, committed);
        }

        committed.add(new CommittedDocument(aEvent.getDocument()));

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
            IPageRequestHandler handler = PageRequestHandlerTracker.getLastHandler(requestCycle);
            if (handler.isPageInstanceCreated()
                    && handler.getPage() instanceof AnnotationPageBase) {
                AnnotatorState state = ((AnnotationPageBase) handler.getPage()).getModelObject();
                requestCycle.getListeners()
                        .add(new TriggerTrainingTaskListener(state.getDocument()));
            }
            else {
                // Otherwise use the document from the event... mind that if there are multiple
                // events, we consider only the first one since after that the trigger listener
                // will be in the cycle and we do not add another one.
                // FIXME: This works as long as the user is working on a single document, but not if
                // the user is doing a bulk operation. If a bulk-operation is done, we get multiple
                // AfterCasWrittenEvent and we do not know which of them belongs to the document
                // which the user is currently viewing.
                requestCycle.getListeners()
                        .add(new TriggerTrainingTaskListener(aEvent.getDocument().getDocument()));
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
    public void triggerPrediction(String aUsername, String aEventName, SourceDocument aDocument)
    {
        User user = userRepository.get(aUsername);
        if (user == null) {
            return;
        }

        schedulingService.enqueue(new PredictionTask(user, aEventName, aDocument));
    }

    @Override
    public void triggerTrainingAndPrediction(String aUser, Project aProject, String aEventName,
            SourceDocument aCurrentDocument)
    {
        triggerRun(aUser, aProject, aEventName, aCurrentDocument, false, null);
    }

    @Override
    public void triggerSelectionTrainingAndPrediction(String aUser, Project aProject,
            String aEventName, SourceDocument aCurrentDocument)
    {
        triggerRun(aUser, aProject, aEventName, aCurrentDocument, true, null);
    }

    private void triggerRun(String aUser, Project aProject, String aEventName,
            SourceDocument aCurrentDocument, boolean aForceSelection, Set<DirtySpot> aDirties)
    {
        User user = userRepository.get(aUser);
        // do not trigger training during when viewing others' work
        if (user == null || !user.equals(userRepository.getCurrentUser())) {
            return;
        }

        // Update the task count
        AtomicInteger count = trainingTaskCounter.computeIfAbsent(
                new RecommendationStateKey(user.getUsername(), aProject),
                _key -> new AtomicInteger(0));

        // If there is no active recommender at all then let's try hard to make one active by
        // re-setting the count and thus force-scheduling a SelectionTask
        if (!hasActiveRecommenders(aUser, aProject)) {
            count.set(0);
        }

        if (aForceSelection || (count.getAndIncrement() % TRAININGS_PER_SELECTION == 0)) {
            // If it is time for a selection task, we just start a selection task.
            // The selection task then will start the training once its finished,
            // i.e. we do not start it here.
            Task task = new SelectionTask(user, aProject, aEventName, aCurrentDocument);
            schedulingService.enqueue(task);

            RecommendationState state = getState(aUser, aProject);
            synchronized (state) {
                state.setPredictionsUntilNextEvaluation(TRAININGS_PER_SELECTION - 1);
                state.setPredictionsSinceLastEvaluation(0);
            }

            return;
        }

        Task task = new TrainingTask(user, aProject, aEventName, aCurrentDocument);
        schedulingService.enqueue(task);

        RecommendationState state = getState(aUser, aProject);
        synchronized (state) {
            int predictions = state.getPredictionsSinceLastEvaluation() + 1;
            state.setPredictionsSinceLastEvaluation(predictions);
            state.setPredictionsUntilNextEvaluation(TRAININGS_PER_SELECTION - predictions - 1);
        }
    }

    @Override
    public List<LogMessageGroup> getLog(String aUser, Project aProject)
    {
        Predictions activePredictions = getState(aUser, aProject).getActivePredictions();
        Predictions incomingPredictions = getState(aUser, aProject).getIncomingPredictions();

        List<LogMessageGroup> messageSets = new ArrayList<>();

        if (activePredictions != null) {
            messageSets.add(new LogMessageGroup("Active", activePredictions.getLog()));
        }

        if (incomingPredictions != null) {
            messageSets.add(new LogMessageGroup("Incoming", incomingPredictions.getLog()));
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

    @EventListener
    public void afterDocumentReset(AfterDocumentResetEvent aEvent)
    {
        String userName = aEvent.getDocument().getUser();
        Project project = aEvent.getDocument().getProject();
        clearState(userName);
        triggerTrainingAndPrediction(userName, project, "AfterDocumentResetEvent",
                aEvent.getDocument().getDocument());
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
    public boolean switchPredictions(User aUser, Project aProject)
    {
        RecommendationState state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            return state.switchPredictions();
        }
    }

    @Override
    public Optional<RecommenderContext> getContext(User aUser, Recommender aRecommender)
    {
        RecommendationState state = getState(aUser.getUsername(), aRecommender.getProject());
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
    public int upsertSpanFeature(AnnotationSchemaService annotationService,
            SourceDocument aDocument, String aUsername, CAS aCas, AnnotationLayer aLayer,
            AnnotationFeature aFeature, String aValue, int aBegin, int aEnd)
        throws AnnotationException
    {
        // The feature of the predicted label
        SpanAdapter adapter = (SpanAdapter) annotationService.getAdapter(aLayer);

        // Check if there is already an annotation of the target type at the given location
        Type type = CasUtil.getType(aCas, adapter.getAnnotationTypeName());
        AnnotationFS annoFS = selectAt(aCas, type, aBegin, aEnd).stream().findFirst().orElse(null);

        int address;
        if (annoFS == null || aLayer.isAllowStacking()) {
            // ... if not or if stacking is allowed, then we create a new annotation - this also
            // takes care of attaching to an annotation if necessary
            address = ICasUtil.getAddr(adapter.add(aDocument, aUsername, aCas, aBegin, aEnd));
        }
        else {
            // ... if yes and stacking is not allowed, then we update the feature on the existing
            // annotation
            address = ICasUtil.getAddr(annoFS);
        }

        // Update the feature value
        adapter.setFeatureValue(aDocument, aUsername, aCas, address, aFeature, aValue);

        return address;
    }

    @Override
    public int upsertRelationFeature(AnnotationSchemaService annotationService,
            SourceDocument aDocument, String aUsername, CAS aCas, AnnotationLayer layer,
            AnnotationFeature aFeature, RelationSuggestion aSuggestion)
        throws AnnotationException
    {
        RelationAdapter adapter = (RelationAdapter) annotationService.getAdapter(layer);

        int sourceBegin = aSuggestion.getPosition().getSourceBegin();
        int sourceEnd = aSuggestion.getPosition().getSourceEnd();
        int targetBegin = aSuggestion.getPosition().getTargetBegin();
        int targetEnd = aSuggestion.getPosition().getTargetEnd();

        // Check if there is already a relation for the given source and target
        Type type = CasUtil.getType(aCas, adapter.getAnnotationTypeName());
        Type attachType = CasUtil.getType(aCas, adapter.getAttachTypeName());

        Feature sourceFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature targetFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        // The begin and end feature of a relation in the CAS are of the dependent/target
        // annotation. See also RelationAdapter::createRelationAnnotation.
        // We use that fact to search for existing relations for this relation suggestion
        List<AnnotationFS> candidates = new ArrayList<>();
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

        AnnotationFS relation = null;
        if (candidates.size() == 1) {
            // One candidate, we just return it
            relation = candidates.get(0);
        }
        else if (candidates.size() == 2) {
            log.warn("Found multiple candidates for upserting relation from suggestion");
            relation = candidates.get(0);
        }

        // We did not find a relation for this suggestion, so we create a new one
        if (relation == null) {
            // FIXME: We get the first match for the (begin, end) span. With stacking, there can
            // be more than one and we need to get the right one then which does not need to be
            // the first. We wait for #2135 to fix this. When stacking is enabled, then also
            // consider creating a new relation instead of upserting an existing one.

            AnnotationFS source = selectAt(aCas, attachType, sourceBegin, sourceEnd).stream()
                    .findFirst().orElse(null);
            AnnotationFS target = selectAt(aCas, attachType, targetBegin, targetEnd).stream()
                    .findFirst().orElse(null);

            if (source == null || target == null) {
                String msg = "Cannot find source or target annotation for upserting relation";
                log.error(msg);
                throw new IllegalStateException(msg);
            }

            relation = adapter.add(aDocument, aUsername, source, target, aCas);
        }

        int address = ICasUtil.getAddr(relation);

        // Update the feature value
        adapter.setFeatureValue(aDocument, aUsername, aCas, address, aFeature,
                aSuggestion.getLabel());

        return address;
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
    private static class RecommendationState
    {
        private Preferences preferences;
        private MultiValuedMap<AnnotationLayer, EvaluatedRecommender> evaluatedRecommenders;
        private Map<Recommender, RecommenderContext> contexts;
        private Predictions activePredictions;
        private Predictions incomingPredictions;
        private boolean predictForAllDocuments;
        private int predictionsSinceLastEvaluation;
        private int predictionsUntilNextEvaluation;

        {
            preferences = new Preferences();
            evaluatedRecommenders = new HashSetValuedHashMap<>();
            contexts = new ConcurrentHashMap<>();
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
         * Returns the context for the given recommender if there is one.
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
    }

    private void computePredictions(LazyCas aOriginalCas,
            EvaluatedRecommender aEvaluatedRecommender, Predictions predictions, CAS predictionCas,
            SourceDocument aDocument, User aUser, int aPredictionBegin, int aPredictionEnd)
        throws IOException
    {
        Project project = aDocument.getProject();
        Predictions activePredictions = getPredictions(aUser, project);
        int predictionBegin = aPredictionBegin;
        int predictionEnd = aPredictionEnd;

        // Make sure we have the latest recommender config from the DB - the one
        // from the active recommenders list may be outdated
        Recommender recommender = aEvaluatedRecommender.getRecommender();
        try {
            recommender = getRecommender(recommender.getId());
        }
        catch (NoResultException e) {
            predictions.log(LogMessage.info(recommender.getName(),
                    "Recommender no longer available... skipping"));
            log.info("{}[{}]: Recommender no longer available... skipping", aUser,
                    recommender.getName());
            return;
        }

        if (!recommender.isEnabled()) {
            predictions.log(
                    LogMessage.info(recommender.getName(), "Recommender disabled... skipping"));
            log.debug("{}[{}]: Disabled - skipping", aUser, recommender.getName());
            return;
        }

        Optional<RecommenderContext> context = getContext(aUser, recommender);

        if (!context.isPresent()) {
            predictions.log(LogMessage.info(recommender.getName(),
                    "Recommender has no context... skipping"));
            log.info("No context available for recommender {} for user {} on document {} in " //
                    + "project {} - skipping recommender", recommender, aUser, aDocument,
                    aDocument.getProject());
            return;
        }

        RecommenderContext ctx = context.get();
        ctx.setUser(aUser);

        Optional<RecommendationEngineFactory<?>> maybeFactory = getRecommenderFactory(recommender);

        if (maybeFactory.isEmpty()) {
            log.warn("{}[{}]: No factory found - skipping recommender", aUser,
                    recommender.getName());
            return;
        }

        RecommendationEngineFactory<?> factory = maybeFactory.get();

        // Check that configured layer and feature are accepted
        // by this type of recommender
        if (!factory.accepts(recommender.getLayer(), recommender.getFeature())) {
            predictions.log(LogMessage.info(recommender.getName(),
                    "Recommender configured with invalid layer or feature... skipping"));
            log.info("{}[{}]: Recommender configured with invalid layer or feature "
                    + "- skipping recommender", aUser, recommender.getName());
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
                predictions.log(LogMessage.info(recommender.getName(),
                        "Recommender context is not ready... skipping"));
                log.info("Recommender context {} for user {} in project {} is not ready for " //
                        + "prediction - skipping recommender", recommender, aUser,
                        aDocument.getProject());

                // If possible, we inherit recommendations from a previous run while
                // the recommender is still busy
                if (activePredictions != null) {
                    inheritSuggestionsAtRecommenderLevel(predictions, originalCas, recommender,
                            activePredictions, aDocument, aUser);
                }

                return;
            }

            cloneAndMonkeyPatchCAS(project, originalCas, predictionCas);

            // If the recommender is not trainable and not sensitive to annotations,
            // we can actually re-use the predictions.
            if (TRAINING_NOT_SUPPORTED == engine.getTrainingCapability()
                    && activePredictions != null
                    && activePredictions.hasRunPredictionOnDocument(aDocument)) {
                inheritSuggestionsAtRecommenderLevel(predictions, originalCas,
                        engine.getRecommender(), activePredictions, aDocument, aUser);
            }
            else {
                generateSuggestions(predictions, ctx, engine, activePredictions, aDocument,
                        originalCas, predictionCas, aUser, predictionBegin, predictionEnd);
            }
        }
        // Catching Throwable is intentional here as we want to continue the
        // execution even if a particular recommender fails.
        catch (Throwable e) {
            predictions.log(LogMessage.error(recommender.getName(), "Failed: %s", e.getMessage()));
            log.error("Error applying recommender {} for user {} to document {} in project {} - " //
                    + "skipping recommender", recommender, aUser, aDocument, aDocument.getProject(),
                    e);

            applicationEventPublisher.publishEvent(new RecommenderTaskEvent(this,
                    aUser.getUsername(), e.getMessage(), recommender));

            // If there was a previous successful run of the recommender, inherit
            // its suggestions to avoid that all the suggestions of the recommender
            // simply disappear.
            if (activePredictions != null) {
                inheritSuggestionsAtRecommenderLevel(predictions, originalCas, recommender,
                        activePredictions, aDocument, aUser);
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
     * @param aUser
     *            the current annotation owner
     * @param aPredictionBegin
     *            begin of the prediction window (&lt; 0 for 0)
     * @param aPredictionEnd
     *            end of the prediction window (&lt; 0 for document-end)
     */
    private void computePredictions(Predictions aPredictions, CAS aPredictionCas,
            SourceDocument aDocument, User aUser, int aPredictionBegin, int aPredictionEnd)
    {
        try {
            List<EvaluatedRecommender> recommenders = getActiveRecommenders(aUser,
                    aDocument.getProject());
            if (recommenders.isEmpty()) {
                aPredictions.log(LogMessage.info(this, "No active recommenders"));
                log.trace("[{}]: No active recommenders", aUser);
                return;
            }

            LazyCas originalCas = new LazyCas(aDocument, aUser);
            for (EvaluatedRecommender r : recommenders) {
                AnnotationLayer layer = annoService.getLayer(r.getRecommender().getLayer().getId());
                if (!layer.isEnabled()) {
                    continue;
                }

                computePredictions(originalCas, r, aPredictions, aPredictionCas, aDocument, aUser,
                        aPredictionBegin, aPredictionEnd);
            }
        }
        catch (IOException e) {
            aPredictions.log(LogMessage.error(this, "Cannot read annotation CAS... skipping"));
            log.error(
                    "Cannot read annotation CAS for user {} of document "
                            + "[{}]({}) in project [{}]({}) - skipping document",
                    aUser, aDocument.getName(), aDocument.getId(), aDocument.getProject().getName(),
                    aDocument.getProject().getId(), e);
            return;
        }

        // When all recommenders have completed on the document, we mark it as "complete"
        aPredictions.markDocumentAsPredictionCompleted(aDocument);
    }

    @Override
    public Predictions computePredictions(User aUser, Project aProject,
            List<SourceDocument> aDocuments)
    {
        try (var casHolder = new PredictionCasHolder()) {
            Predictions predictions = new Predictions(aUser, aProject);
            // Generate new predictions or inherit at the recommender level
            for (SourceDocument document : aDocuments) {
                computePredictions(predictions, casHolder.cas, document, aUser, -1, -1);
            }

            predictions.log(LogMessage.info(this, "Prediction complete"));
            log.debug("Prediction complete");

            return predictions;
        }
        catch (ResourceInitializationException e) {
            Predictions predictions = new Predictions(aUser, aProject);
            predictions.log(
                    LogMessage.error(this, "Cannot create prediction CAS, stopping predictions!"));
            log.error("Cannot create prediction CAS, stopping predictions!");
            return predictions;
        }
    }

    @Override
    public Predictions computePredictions(User aUser, Project aProject,
            SourceDocument aCurrentDocument, List<SourceDocument> aInherit, int aPredictionBegin,
            int aPredictionEnd)
    {
        Predictions predictions = new Predictions(aUser, aProject);
        Predictions activePredictions = getPredictions(aUser, aProject);

        // Inherit at the document level. If inheritance at a recommender level is possible,
        // this is done below.
        if (activePredictions != null) {
            for (SourceDocument document : aInherit) {
                inheritSuggestionsAtDocumentLevel(aProject, document, aUser, activePredictions,
                        predictions);
            }
        }

        try (var casHolder = new PredictionCasHolder()) {
            final CAS predictionCas = casHolder.cas;

            // Generate new predictions or inherit at the recommender level
            computePredictions(predictions, predictionCas, aCurrentDocument, aUser,
                    aPredictionBegin, aPredictionEnd);

            predictions.log(LogMessage.info(this, "Prediction complete"));
            log.debug("Prediction complete");
        }
        catch (ResourceInitializationException e) {
            predictions.log(
                    LogMessage.error(this, "Cannot create prediction CAS, stopping predictions!"));
            log.error("Cannot create prediction CAS, stopping predictions!");
        }

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
        List<AnnotationSuggestion> suggestions = activePredictions
                .getPredictionsByRecommenderAndDocument(aRecommender, document.getName());

        if (suggestions.isEmpty()) {
            log.debug("{} for user {} on document {} in project {} there " //
                    + "are no inheritable predictions", aRecommender, aUser, document,
                    aRecommender.getProject());
            predictions.log(LogMessage.info(aRecommender.getName(),
                    "No inheritable suggestions from previous run"));
            return;
        }

        log.debug("{} for user {} on document {} in project {} inherited {} " //
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

        List<AnnotationSuggestion> suggestions1 = aOldPredictions
                .getPredictionsByDocument(aDocument.getName());

        log.debug("[{}]({}) for user [{}] on document {} in project {} inherited {} predictions",
                "ALL", "--", aUser.getUsername(), aDocument, aProject, suggestions1.size());

        List<AnnotationSuggestion> suggestions = suggestions1;
        aNewPredictions.putPredictions(suggestions);
        aNewPredictions.markDocumentAsPredictionCompleted(aDocument);
    }

    /**
     * Invokes the engine to produce new suggestions.
     */
    void generateSuggestions(Predictions aPredictions, RecommenderContext aCtx,
            RecommendationEngine aEngine, Predictions aActivePredictions, SourceDocument aDocument,
            CAS aOriginalCas, CAS aPredictionCas, User aUser, int aPredictionBegin,
            int aPredictionEnd)
        throws RecommendationException
    {
        Recommender recommender = aEngine.getRecommender();

        aPredictions.log(LogMessage.info(recommender.getName(),
                "Generating predictions for layer [%s]...", recommender.getLayer().getUiName()));
        log.trace("{}[{}]: Generating predictions for layer [{}]", aUser, recommender.getName(),
                recommender.getLayer().getUiName());

        // Perform the actual prediction
        Range predictedRange = aEngine.predict(aCtx, aPredictionCas, aPredictionBegin,
                aPredictionEnd);

        // Extract the suggestions from the data which the recommender has written into the CAS
        var suggestions = extractSuggestions(aOriginalCas, aPredictionCas, aDocument, recommender);

        log.debug(
                "{} for user {} on document {} in project {} generated {} predictions within range {}",
                recommender, aUser, aDocument, recommender.getProject(), suggestions.size(),
                predictedRange);
        aPredictions.log(LogMessage.info(recommender.getName(), //
                "Generated [%d] predictions within range %s", suggestions.size(), predictedRange));

        if (aActivePredictions != null) {
            // Inherit annotations that are outside the range which was predicted. Note that the
            // engine might actually predict a different range from what was requested.
            List<AnnotationSuggestion> inheritableSuggestions = aActivePredictions
                    .getPredictionsByRecommenderAndDocument(recommender, aDocument.getName())
                    .stream().filter(s -> !s.coveredBy(predictedRange)) //
                    .collect(toList());

            log.debug("{} for user {} on document {} in project {} inherited {} " //
                    + "predictions", recommender, aUser, aDocument, recommender.getProject(),
                    inheritableSuggestions.size());
            aPredictions.log(LogMessage.info(recommender.getName(),
                    "Inherited [%d] predictions from previous run", inheritableSuggestions.size()));

            suggestions.addAll(inheritableSuggestions);
        }

        // Calculate the visibility of the suggestions. This happens via the original CAS which
        // contains only the manually created annotations and *not* the suggestions.
        var groupedSuggestions = groupsOfType(SpanSuggestion.class, suggestions);
        calculateSpanSuggestionVisibility(aOriginalCas, aUser.getUsername(),
                aEngine.getRecommender().getLayer(), groupedSuggestions, 0,
                aOriginalCas.getDocumentText().length());

        aPredictions.putPredictions(suggestions);
    }

    private List<AnnotationSuggestion> extractSuggestions(CAS aOriginalCas, CAS aPredictionCas,
            SourceDocument aDocument, Recommender aRecommender)
    {
        AnnotationLayer layer = aRecommender.getLayer();
        String typeName = layer.getName();
        String featureName = aRecommender.getFeature().getName();

        Type predictedType = CasUtil.getType(aPredictionCas, typeName);
        Feature labelFeature = predictedType.getFeatureByBaseName(featureName);
        Feature sourceFeature = predictedType.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature targetFeature = predictedType.getFeatureByBaseName(FEAT_REL_TARGET);
        Feature scoreFeature = predictedType
                .getFeatureByBaseName(featureName + FEATURE_NAME_SCORE_SUFFIX);
        Feature scoreExplanationFeature = predictedType
                .getFeatureByBaseName(featureName + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX);
        Feature predictionFeature = predictedType.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);
        var isMultiLabels = TYPE_NAME_STRING_ARRAY.equals(labelFeature.getRange().getName());

        List<AnnotationSuggestion> result = new ArrayList<>();
        int id = 0;

        for (FeatureStructure predictedFS : aPredictionCas.select(predictedType)) {
            if (!predictedFS.getBooleanValue(predictionFeature)) {
                continue;
            }

            String[] labels = getPredictedLabels(predictedFS, labelFeature, isMultiLabels);
            double score = predictedFS.getDoubleValue(scoreFeature);
            String scoreExplanation = predictedFS.getStringValue(scoreExplanationFeature);

            for (String label : labels) {
                AnnotationSuggestion suggestion;

                switch (layer.getType()) {
                case SPAN_TYPE: {
                    var predictedAnnotation = (Annotation) predictedFS;
                    var targetOffsets = getOffsets(layer, aOriginalCas, predictedAnnotation);

                    if (!targetOffsets.isPresent()) {
                        continue;
                    }

                    suggestion = new SpanSuggestion(id, aRecommender, layer.getId(), featureName,
                            aDocument.getName(), targetOffsets.get(),
                            predictedAnnotation.getCoveredText(), label, label, score,
                            scoreExplanation);
                    break;
                }
                case RELATION_TYPE: {
                    var source = (AnnotationFS) predictedFS.getFeatureValue(sourceFeature);
                    var target = (AnnotationFS) predictedFS.getFeatureValue(targetFeature);

                    var originalSource = findEquivalent(aOriginalCas, source).get();
                    var originalTarget = findEquivalent(aOriginalCas, target).get();

                    suggestion = new RelationSuggestion(id, aRecommender, layer.getId(),
                            featureName, aDocument.getName(), originalSource, originalTarget, label,
                            label, score, scoreExplanation);
                    break;
                }
                default:
                    throw new IllegalStateException(
                            "Unsupport layer type [" + layer.getType() + "]");
                }

                result.add(suggestion);
                id++;
            }
        }

        return result;
    }

    private String[] getPredictedLabels(FeatureStructure predictedFS, Feature predictedFeature,
            boolean isStringMultiValue)
    {
        String[] labels;
        if (isStringMultiValue) {
            labels = FSUtil.getFeature(predictedFS, predictedFeature, String[].class);
        }
        else {
            labels = new String[] { predictedFS.getFeatureValueAsString(predictedFeature) };
        }
        return labels;
    }

    /**
     * Locates an annotation in the given CAS which is equivalent of the provided annotation.
     *
     * @param aOriginalCas
     *            the original CAS.
     * @param aAnnotation
     *            an annotation in the prediction CAS. return the equivalent in the original CAS.
     */
    private Optional<Annotation> findEquivalent(CAS aOriginalCas, AnnotationFS aAnnotation)
    {
        return aOriginalCas.<Annotation> select(aAnnotation.getType())
                .filter(candidate -> AnnotationComparisonUtils.isEquivalentSpanAnnotation(candidate,
                        aAnnotation, null))
                .findFirst();
    }

    /**
     * Calculates the offsets of the given predicted annotation in the original CAS .
     *
     * @param aLayer
     *            the prediction layer definition.
     * @param aOriginalCas
     *            the original CAS.
     * @param aPredictedAnnotation
     *            the predicted annotation.
     * @return the proper offsets.
     */
    private Optional<Offset> getOffsets(AnnotationLayer aLayer, CAS aOriginalCas,
            Annotation aPredictedAnnotation)
    {
        Type tokenType = getType(aOriginalCas, Token.class);
        Type sentenceType = getType(aOriginalCas, Sentence.class);

        int begin;
        int end;
        switch (aLayer.getAnchoringMode()) {
        case CHARACTERS: {
            int[] offsets = { aPredictedAnnotation.getBegin(), aPredictedAnnotation.getEnd() };
            TrimUtils.trim(aPredictedAnnotation.getCAS().getDocumentText(), offsets);
            begin = offsets[0];
            end = offsets[1];
            break;
        }
        case SINGLE_TOKEN: {
            List<Annotation> tokens = aOriginalCas.<Annotation> select(tokenType)
                    .coveredBy(aPredictedAnnotation).limit(2).asList();

            if (tokens.isEmpty()) {
                // This can happen if a recommender uses different token boundaries (e.g. if a
                // remote service performs its own tokenization). We might be smart here by
                // looking for overlapping tokens instead of contained tokens.
                log.trace("Discarding suggestion because no covering token was found: {}",
                        aPredictedAnnotation);
                return Optional.empty();
            }

            if (tokens.size() > 1) {
                // We only want to accept single-token suggestions
                log.trace("Discarding suggestion because only single-token suggestions are "
                        + "accepted: {}", aPredictedAnnotation);
                return Optional.empty();
            }

            AnnotationFS token = tokens.get(0);
            begin = token.getBegin();
            end = token.getEnd();
            break;
        }
        case TOKENS: {
            List<Annotation> tokens = aOriginalCas.<Annotation> select(tokenType)
                    .coveredBy(aPredictedAnnotation).asList();

            if (tokens.isEmpty()) {
                // This can happen if a recommender uses different token boundaries (e.g. if a
                // remote service performs its own tokenization). We might be smart here by
                // looking for overlapping tokens instead of contained tokens.
                log.trace("Discarding suggestion because no covering tokens were found: {}",
                        aPredictedAnnotation);
                return Optional.empty();
            }

            begin = tokens.get(0).getBegin();
            end = tokens.get(tokens.size() - 1).getEnd();
            break;
        }
        case SENTENCES: {
            List<AnnotationFS> sentences = selectCovered(sentenceType, aPredictedAnnotation);
            if (sentences.isEmpty()) {
                // This can happen if a recommender uses different token boundaries (e.g. if a
                // remote service performs its own tokenization). We might be smart here by
                // looking for overlapping sentences instead of contained sentences.
                log.trace("Discarding suggestion because no covering sentences were found: {}",
                        aPredictedAnnotation);
                return Optional.empty();
            }

            begin = sentences.get(0).getBegin();
            end = sentences.get(sentences.size() - 1).getEnd();
            break;
        }
        default:
            throw new IllegalStateException(
                    "Unsupported anchoring mode: [" + aLayer.getAnchoringMode() + "]");
        }

        return Optional.of(new Offset(begin, end));
    }

    /**
     * Goes through all SpanSuggestions and determines the visibility of each one
     */
    @Override
    public void calculateSpanSuggestionVisibility(CAS aCas, String aUser, AnnotationLayer aLayer,
            Collection<SuggestionGroup<SpanSuggestion>> aRecommendations, int aWindowBegin,
            int aWindowEnd)
    {
        log.trace("calculateSpanSuggestionVisibility()");

        Type type = getAnnotationType(aCas, aLayer);
        if (type == null) {
            // The type does not exist in the type system of the CAS. Probably it has not
            // been upgraded to the latest version of the type system yet. If this is the case,
            // we'll just skip.
            return;
        }

        List<AnnotationFS> annotationsInWindow = getAnnotationsInWindow(aCas, type, aWindowBegin,
                aWindowEnd);

        // Collect all suggestions of the given layer within the view window
        List<SuggestionGroup<SpanSuggestion>> suggestionsInWindow = aRecommendations.stream()
                // Only suggestions for the given layer
                .filter(group -> group.getLayerId() == aLayer.getId())
                // ... and in the given window
                .filter(group -> {
                    Offset offset = (Offset) group.getPosition();
                    return aWindowBegin <= offset.getBegin() && offset.getEnd() <= aWindowEnd;
                }) //
                .collect(toList());

        // Get all the skipped/rejected entries for the current layer
        List<LearningRecord> recordedAnnotations = learningRecordService.listRecords(aUser, aLayer);

        for (AnnotationFeature feature : annoService.listSupportedFeatures(aLayer)) {
            Feature feat = type.getFeatureByBaseName(feature.getName());

            if (feat == null) {
                // The feature does not exist in the type system of the CAS. Probably it has not
                // been upgraded to the latest version of the type system yet. If this is the case,
                // we'll just skip.
                return;
            }

            // Reduce the suggestions to the ones for the given feature. We can use the tree here
            // since we only have a single SuggestionGroup for every position
            Map<Offset, SuggestionGroup<SpanSuggestion>> suggestions = new TreeMap<>(
                    comparingInt(Offset::getBegin).thenComparingInt(Offset::getEnd));
            suggestionsInWindow.stream()
                    .filter(group -> group.getFeature().equals(feature.getName())) //
                    .map(group -> {
                        group.showAll(AnnotationSuggestion.FLAG_ALL);
                        return group;
                    }) //
                    .forEach(group -> suggestions.put((Offset) group.getPosition(), group));

            hideSpanSuggestionsThatOverlapWithAnnotations(annotationsInWindow, suggestionsInWindow,
                    feature, feat, suggestions);

            // Anything that was not hidden so far might still have been rejected
            suggestions.values().stream() //
                    .flatMap(SuggestionGroup::stream) //
                    .filter(AnnotationSuggestion::isVisible) //
                    .forEach(suggestion -> hideSuggestionsRejectedOrSkipped(suggestion,
                            recordedAnnotations));
        }
    }

    private void hideSpanSuggestionsThatOverlapWithAnnotations(
            List<AnnotationFS> annotationsInWindow,
            List<SuggestionGroup<SpanSuggestion>> suggestionsInWindow, AnnotationFeature feature,
            Feature feat, Map<Offset, SuggestionGroup<SpanSuggestion>> suggestions)
    {
        // If there are no suggestions or annotations, there is nothing to do here
        if (annotationsInWindow.isEmpty() || suggestions.isEmpty()) {
            return;
        }

        // Reduce the annotations to the ones which have a non-null feature value. We need to
        // use a multi-valued map here because there may be multiple annotations at a
        // given position.
        MultiValuedMap<Offset, AnnotationFS> annotations = new ArrayListValuedHashMap<>();
        annotationsInWindow
                .forEach(fs -> annotations.put(new Offset(fs.getBegin(), fs.getEnd()), fs));

        // We need to constructed a sorted list of the keys for the OverlapIterator below
        List<Offset> sortedAnnotationKeys = new ArrayList<>(annotations.keySet());
        sortedAnnotationKeys.sort(comparingInt(Offset::getBegin).thenComparingInt(Offset::getEnd));

        // This iterator gives us pairs of annotations and suggestions. Note that both lists
        // must be sorted in the same way. The suggestion offsets are sorted because they are
        // the keys in a TreeSet - and the annotation offsets are sorted in the same way
        // manually
        OverlapIterator oi = new OverlapIterator(new ArrayList<>(suggestions.keySet()),
                sortedAnnotationKeys);

        // Bulk-hide any groups that overlap with existing annotations on the current layer
        // and for the current feature
        while (oi.hasNext()) {
            if (oi.getA().overlaps(oi.getB())) {
                // Fetch the current suggestion and annotation
                SuggestionGroup<SpanSuggestion> group = suggestions.get(oi.getA());
                for (AnnotationFS annotation : annotations.get(oi.getB())) {
                    String label = annotation.getFeatureValueAsString(feat);
                    for (SpanSuggestion suggestion : group) {
                        // The suggestion would just create an annotation and not set any
                        // feature
                        if (suggestion.getLabel() == null) {
                            // If there is already an annotation, then we hide any suggestions
                            // that would just trigger the creation of the same annotation and
                            // not set any new feature. This applies whether stacking is allowed
                            // or not.
                            if (suggestion.getBegin() == annotation.getBegin()
                                    && suggestion.getEnd() == annotation.getEnd()) {
                                suggestion.hide(FLAG_OVERLAP);
                                continue;
                            }

                            // If stacking is enabled, we do allow suggestions that create an
                            // annotation with no label, but only if the offsets differ
                            if (feature.getLayer().isAllowStacking()
                                    && (suggestion.getBegin() != annotation.getBegin()
                                            || suggestion.getEnd() != annotation.getEnd())) {
                                suggestion.hide(FLAG_OVERLAP);
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
                            if (label == null) {
                                continue;
                            }

                            // Does the suggested label match the label of an existing
                            // annotation, then we hide
                            if (label.equals(suggestion.getLabel())) {
                                suggestion.hide(FLAG_OVERLAP);
                                continue;
                            }

                            // Would accepting the suggestion create a new annotation but
                            // stacking is not enabled - then we need to hide
                            if (!feature.getLayer().isAllowStacking()) {
                                suggestion.hide(FLAG_OVERLAP);
                                continue;
                            }
                        }
                    }
                }

                // Do not want to process the group again since the relevant annotations are
                // already hidden
                oi.ignoraA();
            }
            oi.step();
        }
    }

    @Override
    public void calculateRelationSuggestionVisibility(CAS aCas, String aUser,
            AnnotationLayer aLayer,
            Collection<SuggestionGroup<RelationSuggestion>> aRecommendations, int aWindowBegin,
            int aWindowEnd)
    {
        Type type = getAnnotationType(aCas, aLayer);

        if (type == null) {
            // The type does not exist in the type system of the CAS. Probably it has not
            // been upgraded to the latest version of the type system yet. If this is the case,
            // we'll just skip.
            return;
        }

        Feature governorFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature dependentFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        if (dependentFeature == null || governorFeature == null) {
            log.warn("Missing Dependent or Governor feature on [{}]", aLayer.getName());
            return;
        }

        List<AnnotationFS> annotationsInWindow = getAnnotationsInWindow(aCas, type, aWindowBegin,
                aWindowEnd);

        // Group annotations by relation position, that is (source, target) address
        MultiValuedMap<Position, AnnotationFS> groupedAnnotations = new ArrayListValuedHashMap<>();
        for (AnnotationFS annotationFS : annotationsInWindow) {
            AnnotationFS source = (AnnotationFS) annotationFS.getFeatureValue(governorFeature);
            AnnotationFS target = (AnnotationFS) annotationFS.getFeatureValue(dependentFeature);

            RelationPosition relationPosition = new RelationPosition(source.getBegin(),
                    source.getEnd(), target.getBegin(), target.getEnd());

            groupedAnnotations.put(relationPosition, annotationFS);
        }

        // Collect all suggestions of the given layer
        List<SuggestionGroup<RelationSuggestion>> groupedSuggestions = aRecommendations.stream()
                .filter(group -> group.getLayerId() == aLayer.getId()) //
                .collect(toList());

        // Get previously rejected suggestions
        MultiValuedMap<Position, LearningRecord> groupedRecordedAnnotations = new ArrayListValuedHashMap<>();
        for (LearningRecord learningRecord : learningRecordService.listRecords(aUser, aLayer)) {
            RelationPosition relationPosition = new RelationPosition(
                    learningRecord.getOffsetSourceBegin(), learningRecord.getOffsetSourceEnd(),
                    learningRecord.getOffsetTargetBegin(), learningRecord.getOffsetTargetEnd());

            groupedRecordedAnnotations.put(relationPosition, learningRecord);
        }

        for (AnnotationFeature feature : annoService.listSupportedFeatures(aLayer)) {
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
                            hideSuggestion(suggestion, learningRecord.getUserAction());
                        }
                    }
                }
            }
        }
    }

    private void hideSuggestion(AnnotationSuggestion aSuggestion, LearningRecordType aAction)
    {
        switch (aAction) {
        case REJECTED:
            aSuggestion.hide(FLAG_REJECTED);
            break;
        case SKIPPED:
            aSuggestion.hide(FLAG_SKIPPED);
            break;
        default:
            // Nothing to do for the other cases.
            // ACCEPTED annotation are filtered out anyway because the overlap with a created
            // annotation and the same for CORRECTED
        }
    }

    private void hideSuggestionsRejectedOrSkipped(SpanSuggestion aSuggestion,
            List<LearningRecord> aRecordedRecommendations)
    {
        // If it was rejected or skipped, hide it
        for (LearningRecord record : aRecordedRecommendations) {
            boolean isAtTheSamePlace = record.getOffsetBegin() == aSuggestion.getBegin()
                    && record.getOffsetEnd() == aSuggestion.getEnd();
            if (isAtTheSamePlace && aSuggestion.labelEquals(record.getAnnotation())) {
                hideSuggestion(aSuggestion, record.getUserAction());
                return;
            }
        }
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
        try (StopWatch watch = new StopWatch(log, "adding score features")) {
            TypeSystemDescription tsd = annoService.getFullProjectTypeSystem(aProject);

            for (AnnotationLayer layer : annoService.listAnnotationLayer(aProject)) {
                TypeDescription td = tsd.getType(layer.getName());

                if (td == null) {
                    log.trace("Could not monkey patch type [{}]", layer.getName());
                    continue;
                }

                for (FeatureDescription feature : td.getFeatures()) {
                    String scoreFeatureName = feature.getName() + FEATURE_NAME_SCORE_SUFFIX;
                    td.addFeature(scoreFeatureName, "Score feature", CAS.TYPE_NAME_DOUBLE);

                    String scoreExplanationFeatureName = feature.getName()
                            + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;
                    td.addFeature(scoreExplanationFeatureName, "Score explanation feature",
                            CAS.TYPE_NAME_STRING);
                }

                td.addFeature(FEATURE_NAME_IS_PREDICTION, "Is Prediction", CAS.TYPE_NAME_BOOLEAN);
            }

            annoService.upgradeCas(aSourceCas, aTargetCas, tsd);
        }

        return aTargetCas;
    }

    private class TriggerTrainingTaskListener
        implements IRequestCycleListener
    {
        private final SourceDocument currentDocument;

        public TriggerTrainingTaskListener(SourceDocument aCurrentDocument)
        {
            currentDocument = aCurrentDocument;
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
                    .collect(toMap(d -> d.getProject().getId(), d -> d.getProject()));
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
                        "Committed dirty CAS at end of request", currentDocument, false,
                        contextDirties.getValue());
            }
        }
    }

    @Override
    public boolean existsEnabledRecommender(Project aProject)
    {
        String query = String.join("\n", //
                "FROM Recommender WHERE", //
                "enabled = :enabled AND", //
                "project = :project");

        List<Recommender> recommenders = entityManager.createQuery(query, Recommender.class) //
                .setParameter("enabled", true) //
                .setParameter("project", aProject) //
                .getResultList();

        return recommenders.stream() //
                .anyMatch(rec -> getRecommenderFactory(rec).isPresent());
    }

    @Override
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

    private class LazyCas
    {
        private final SourceDocument document;
        private final User user;

        private CAS originalCas;

        public LazyCas(SourceDocument aDocument, User aUser)
        {
            document = aDocument;
            user = aUser;
        }

        public CAS get() throws IOException
        {
            if (originalCas == null) {
                originalCas = documentService.readAnnotationCas(document, user.getUsername(),
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
}
