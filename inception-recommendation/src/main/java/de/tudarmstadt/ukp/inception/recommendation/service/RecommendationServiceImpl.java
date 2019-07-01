/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSingleFsAt;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_OVERLAP;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_SKIPPED;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.DocumentOpenedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterAnnotationUpdateEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentResetEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.StopWatch;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderDeletedEvent;
import de.tudarmstadt.ukp.inception.recommendation.tasks.SelectionTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.TrainingTask;
import de.tudarmstadt.ukp.inception.recommendation.util.OverlapIterator;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.Task;

/**
 * The implementation of the RecommendationService.
 */
@Component(RecommendationService.SERVICE_NAME)
public class RecommendationServiceImpl
    implements RecommendationService
{
    private final Logger log = LoggerFactory.getLogger(getClass());


    private static final double NO_SCORE = 0.0;
    private static final int TRAININGS_PER_SELECTION = 5;

    private @PersistenceContext EntityManager entityManager;
    
    private final SessionRegistry sessionRegistry;
    private final UserDao userRepository;
    private final RecommenderFactoryRegistry recommenderFactoryRegistry;
    private final SchedulingService schedulingService;
    private final AnnotationSchemaService annoService;
    private final DocumentService documentService;
    private final LearningRecordService learningRecordService;
    
    private final ConcurrentMap<Pair<User, Project>, AtomicInteger> trainingTaskCounter;
    private final ConcurrentMap<RecommendationStateKey, RecommendationState> states;

    @Autowired
    public RecommendationServiceImpl(SessionRegistry aSessionRegistry, UserDao aUserRepository,
            RecommenderFactoryRegistry aRecommenderFactoryRegistry,
            SchedulingService aSchedulingService, AnnotationSchemaService aAnnoService,
            DocumentService aDocumentService, LearningRecordService aLearningRecordService)
    {
        sessionRegistry = aSessionRegistry;
        userRepository = aUserRepository;
        recommenderFactoryRegistry = aRecommenderFactoryRegistry;
        schedulingService = aSchedulingService;
        annoService = aAnnoService;
        documentService = aDocumentService;
        learningRecordService = aLearningRecordService;
        
        trainingTaskCounter = new ConcurrentHashMap<>();
        states = new ConcurrentHashMap<>();
    }

    public RecommendationServiceImpl(SessionRegistry aSessionRegistry, UserDao aUserRepository,
            RecommenderFactoryRegistry aRecommenderFactoryRegistry,
            SchedulingService aSchedulingService, AnnotationSchemaService aAnnoService,
            DocumentService aDocumentService, LearningRecordService aLearningRecordService,
            EntityManager aEntityManager)
    {
        this(aSessionRegistry, aUserRepository, aRecommenderFactoryRegistry, aSchedulingService,
                aAnnoService, aDocumentService, aLearningRecordService);
        
        entityManager = aEntityManager;
    }

    public RecommendationServiceImpl(EntityManager aEntityManager)
    {
        this(null, null, null, null, null, null, null);

        entityManager = aEntityManager;
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
    public void setActiveRecommenders(User aUser, AnnotationLayer aLayer,
            List<Recommender> aRecommenders)
    {
        RecommendationState state = getState(aUser.getUsername(), aLayer.getProject());
        synchronized (state) {
            MultiValuedMap<AnnotationLayer, Recommender> activeRecommenders = state
                    .getActiveRecommenders();
            activeRecommenders.remove(aLayer);
            activeRecommenders.putAll(aLayer, aRecommenders);
        }
    }
    
    @Override
    public List<Recommender> getActiveRecommenders(User aUser, AnnotationLayer aLayer)
    {
        RecommendationState state = getState(aUser.getUsername(), aLayer.getProject());
        synchronized (state) {
            MultiValuedMap<AnnotationLayer, Recommender> activeRecommenders = 
                    state.getActiveRecommenders();
            return new ArrayList<>(activeRecommenders.get(aLayer));
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
        String query = 
                "SELECT DISTINCT r.layer " +
                "FROM Recommender r " +
                "WHERE r.project = :project AND r.enabled = :enabled " +
                "ORDER BY r.layer.name ASC";

        return entityManager.createQuery(query, AnnotationLayer.class)
                .setParameter("project", aProject).setParameter("enabled", true).getResultList();
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
        String query = String.join("\n",
                "SELECT COUNT(*)",
                "FROM Recommender ",
                "WHERE name = :name ",
                "AND project = :project");

        long count = entityManager
                .createQuery(query, Long.class)
                .setParameter("name", aName)
                .setParameter("project", aProject)
                .getSingleResult();
        
        return count > 0;
    }

    @Override
    @Transactional
    public Optional<Recommender> getRecommender(Project aProject, String aName)
    {
        String query = String.join("\n",
                "FROM Recommender ",
                "WHERE name = :name ",
                "AND project = :project");

        return entityManager
                .createQuery(query, Recommender.class)
                .setParameter("name", aName)
                .setParameter("project", aProject)
                .getResultStream()
                .findFirst();
    }
    
    @Override
    @Transactional
    public Optional<Recommender> getEnabledRecommender(long aRecommenderId)
    {
        String query = String.join("\n",
                "FROM Recommender WHERE ",
                "id = :id AND ",
                "enabled = :enabled" );

        return entityManager.createQuery(query, Recommender.class)
                .setParameter("id", aRecommenderId)
                .setParameter("enabled", true)
                .getResultStream()
                .findFirst();
    }
    
    @Override
    @Transactional
    public List<Recommender> listEnabledRecommenders(AnnotationLayer aLayer)
    {
        String query = String.join("\n",
                "FROM Recommender WHERE ",
                "project = :project AND",
                "layer = :layer AND",
                "enabled = :enabled",
                "ORDER BY name ASC" );

        return entityManager.createQuery(query, Recommender.class)
                .setParameter("project", aLayer.getProject())
                .setParameter("layer", aLayer)
                .setParameter("enabled", true)
                .getResultList();
    }
    
    @Override
    @Transactional
    public List<Recommender> listEnabledRecommenders(Project aProject)
    {
        String query = String.join("\n",
                "FROM Recommender WHERE",
                "project = :project AND",
                "enabled = :enabled",
                "ORDER BY name ASC" );

        return entityManager.createQuery(query, Recommender.class)
                .setParameter("project", aProject)
                .setParameter("enabled", true)
                .getResultList();
    }

    @Override
    @Transactional
    public List<Recommender> listRecommenders(AnnotationLayer aLayer)
    {
        String query = String.join("\n",
                "FROM Recommender WHERE ",
                "layer = :layer",
                "ORDER BY name ASC" );
        
        return entityManager.createQuery(query, Recommender.class)
                .setParameter("layer", aLayer)
                .getResultList();
    }

    /**
     * This is called whenever a document is opened (because of the implicit CAS upgrade and saving
     * of the CAS that happens when a document is opened) as well as when any updates to annotations
     * are made. Therefore, we do not need an extra event listener for {@link DocumentOpenedEvent}
     */
    @EventListener
    public void afterAnnotationUpdate(AfterAnnotationUpdateEvent aEvent)
    {
        triggerTrainingAndClassification(aEvent.getDocument().getUser(),
                aEvent.getDocument().getProject(), "AfterAnnotationUpdateEvent");
    }

    @EventListener
    public void onRecommenderDelete(RecommenderDeletedEvent aEvent)
    {
        RecommendationState state = getState(aEvent.getUser(), aEvent.getProject());
        synchronized (state) {
            state.removePredictions(aEvent.getRecommender());
        }
        triggerTrainingAndClassification(aEvent.getUser(), aEvent.getProject(),
                "RecommenderDeletedEvent");
    }
    
    private void triggerTrainingAndClassification(String aUser, Project aProject, String aEventName)
    {
        User user = userRepository.get(aUser);

        // Update the task count
        Pair<User, Project> key = new ImmutablePair<>(user, aProject);
        AtomicInteger count = trainingTaskCounter.computeIfAbsent(key,
            _key -> new AtomicInteger(0));

        // If it is time for a selection task, we just start a selection task.
        // The selection task then will start the training once its finished,
        // i.e. we do not start it here.
        if (count.getAndIncrement() % TRAININGS_PER_SELECTION == 0) {
            Task task = new SelectionTask(aProject, user, aEventName);
            schedulingService.enqueue(task);
        } else {
            Task task = new TrainingTask(user, aProject, aEventName);
            schedulingService.enqueue(task);
        }
    }
    
    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onApplicationEvent(SessionDestroyedEvent event)
    {
        SessionInformation info = sessionRegistry.getSessionInformation(event.getId());
        // Could be an anonymous session without information.
        if (info != null) {
            String username = (String) info.getPrincipal();
            clearState(username);
            schedulingService.stopAllTasksForUser(username);
        }
    }

    @EventListener
    public void afterDocumentReset(AfterDocumentResetEvent aEvent)
    {
        String userName = aEvent.getDocument().getUser();
        Project project = aEvent.getDocument().getProject();
        clearState(userName);
        triggerTrainingAndClassification(userName, project, "AfterDocumentResetEvent");
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
    public RecommendationEngineFactory getRecommenderFactory(Recommender aRecommender)
    {
        return recommenderFactoryRegistry.getFactory(aRecommender.getTool());
    }
    
    private RecommendationState getState(String aUsername, Project aProject)
    {
        synchronized (states) {
            return states.computeIfAbsent(new RecommendationStateKey(aUsername, aProject), (v) -> 
                    new RecommendationState());
        }
    }
    
    private void clearState(String aUsername)
    {
        Validate.notNull(aUsername, "Username must be specified");
        
        synchronized (states) {
            states.keySet().removeIf(key -> aUsername.equals(key.getUser()));
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
    public RecommenderContext getContext(User aUser, Recommender aRecommender)
    {
        RecommendationState state = getState(aUser.getUsername(), aRecommender.getProject());
        synchronized (state) {
            return state.getContext(aRecommender);
        }
    }
    
    @Override
    public int upsertFeature(AnnotationSchemaService annotationService, SourceDocument aDocument,
            String aUsername, CAS aCas, AnnotationLayer layer, AnnotationFeature aFeature,
            String aValue, int aBegin, int aEnd)
        throws AnnotationException
    {
        // The feature of the predicted label
        SpanAdapter adapter = (SpanAdapter) annotationService.getAdapter(layer);
        
        // Check if there is already an annotation of the target type at the given location
        Type type = CasUtil.getType(aCas, adapter.getAnnotationTypeName());
        AnnotationFS annoFS = selectSingleFsAt(aCas, type, aBegin, aEnd);
        int address;
        if (annoFS != null) {
            // ... if yes, then we update the feature on the existing annotation
            address = getAddr(annoFS);
        }
        else {
            // ... if not, then we create a new annotation - this also takes care of attaching to 
            // an annotation if necessary
            address = getAddr(adapter.add(aDocument, aUsername, aCas, aBegin, aEnd));
        }

        // Update the feature value
        adapter.setFeatureValue(aDocument, aUsername, aCas, address, aFeature, aValue);
        
        return address;
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
     * We are assuming that the user is actively working on one project at a time.
     * Otherwise, the RecommendationUserState might take up a lot of memory.
     */
    private static class RecommendationState
    {
        private Preferences preferences = new Preferences();
        private MultiValuedMap<AnnotationLayer, Recommender> activeRecommenders = 
                new HashSetValuedHashMap<>();
        private Map<Recommender, RecommenderContext> contexts = new ConcurrentHashMap<>();
        private Predictions activePredictions;
        private Predictions incomingPredictions;
        
        public Preferences getPreferences()
        {
            return preferences;
        }

        public void setPreferences(Preferences aPreferences)
        {
            preferences = aPreferences;
        }

        public MultiValuedMap<AnnotationLayer, Recommender> getActiveRecommenders()
        {
            return activeRecommenders;
        }

        public void setActiveRecommenders(
            MultiValuedMap<AnnotationLayer, Recommender> aActiveRecommenders)
        {
            activeRecommenders = aActiveRecommenders;
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
            if (incomingPredictions != null) {
                activePredictions = incomingPredictions;
                incomingPredictions = null;
                return true;
            }
            else {
                return false;
            }
        }
        
        /**
         * Returns the context for the given recommender or creates a new one if there is none so
         * far.
         */
        public RecommenderContext getContext(Recommender aRecommender)
        {
            Validate.notNull(aRecommender, "Recommender must be specified");
            
            return contexts.computeIfAbsent(aRecommender, (v) -> new RecommenderContext());
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

            // Remove from activeRecommenders map.
            // We have to do this, otherwise training and prediction continues for the
            // recommender when a new task is triggered.
            MultiValuedMap<AnnotationLayer, Recommender> newActiveRecommenders = 
                    new HashSetValuedHashMap<>();
            MapIterator<AnnotationLayer, Recommender> it = activeRecommenders.mapIterator();

            while (it.hasNext()) {
                AnnotationLayer layer = it.next();
                Recommender rec = it.getValue();
                if (!rec.equals(aRecommender)) {
                    newActiveRecommenders.put(layer, rec);
                }
            }
            
            setActiveRecommenders(newActiveRecommenders);
        }
    }
    
    @Override
    public Predictions computePredictions(User aUser, Project aProject,
                                          List<SourceDocument> aDocuments)
    {
        String username = aUser.getUsername();
        Predictions predictions = new Predictions(aUser, aProject);

        nextDocument: for (SourceDocument document : aDocuments) {
            Optional<CAS> originalCas = Optional.empty();
            CAS predictionCas = null;
            nextLayer: for (AnnotationLayer layer : annoService
                    .listAnnotationLayer(document.getProject())) {
                if (!layer.isEnabled()) {
                    continue nextLayer;
                }

                List<Recommender> recommenders = getActiveRecommenders(aUser, layer);

                nextRecommender: for (Recommender r : recommenders) {
                    
                    // Make sure we have the latest recommender config from the DB - the one from
                    // the active recommenders list may be outdated
                    Recommender recommender;

                    try {
                        recommender = getRecommender(r.getId());
                    }
                    catch (NoResultException e) {
                        log.info("[{}][{}]: Recommender no longer available... skipping",
                                username, r.getName());
                        continue nextRecommender;
                    }

                    if (!recommender.isEnabled()) {
                        log.debug("[{}][{}]: Disabled - skipping", username, r.getName());
                        continue nextRecommender;
                    }

                    RecommenderContext ctx = getContext(aUser, recommender);

                    if (!ctx.isClosed()) {
                        log.info("Context for recommender [{}]({}) for user [{}] on document "
                                + "[{}]({}) in project [{}]({}) is not ready yet - skipping recommender",
                                recommender.getName(), recommender.getId(), username,
                                document.getName(), document.getId(),
                                document.getProject().getName(), document.getProject().getId());
                        continue nextRecommender;
                    }

                    RecommendationEngineFactory<?> factory = getRecommenderFactory(recommender);
                    
                    // Check that configured layer and feature are accepted 
                    // by this type of recommender
                    if (!factory.accepts(recommender.getLayer(), recommender.getFeature())) {
                        log.info("[{}][{}]: Recommender configured with invalid layer or feature "
                                + "- skipping recommender", username, r.getName());
                        continue nextRecommender;
                    }

                    // We lazily load the CAS only at this point because that allows us to skip
                    // loading the CAS entirely if there is no enabled layer or recommender.
                    // If the CAS cannot be loaded, then we skip to the next document.
                    if (!originalCas.isPresent()) {
                        try {
                            originalCas = Optional.of(documentService.readAnnotationCas(document,
                                    username));
                        }
                        catch (IOException e) {
                            log.error(
                                    "Cannot read annotation CAS for user [{}] of document "
                                            + "[{}]({}) in project [{}]({}) - skipping document",
                                    username, document.getName(), document.getId(),
                                    document.getProject().getName(), document.getProject().getId(),
                                    e);
                            continue nextDocument;
                        }
                        try {
                            annoService.upgradeCasIfRequired(originalCas.get(), document,
                                    username);
                        }
                        catch (UIMAException | IOException e) {
                            log.error(
                                    "Cannot upgrade annotation CAS for user [{}] of document "
                                            + "[{}]({}) in project [{}]({}) - skipping document",
                                    username, document.getName(), document.getId(),
                                    document.getProject().getName(), document.getProject().getId(),
                                    e);
                            continue nextDocument;
                        }

                        try {
                            predictionCas = cloneCAS(originalCas.get());
                            monkeyPatchTypeSystem(aProject, predictionCas);
                        }
                        catch (UIMAException | IOException e) {
                            log.error("Cannot create prediction CAS for user [{}] of document "
                                    + "[{}]({}) in project [{}]({}) - skipping document",
                                    username, document.getName(), document.getId(),
                                    document.getProject().getName(), document.getProject().getId(),
                                    e);
                            continue nextDocument;
                        }
                    }

                    try {
                        RecommendationEngine recommendationEngine = factory.build(recommender, ctx);

                        if (!recommendationEngine.isReadyForPrediction(ctx)) {
                            log.info("Recommender context [{}]({}) for user [{}] in project "
                                    + "[{}]({}) is not ready for prediction - skipping recommender",
                                    recommender.getName(), recommender.getId(), username,
                                    document.getProject().getName(), document.getProject().getId());
                            continue nextRecommender;
                        }

                        // Perform the actual prediction
                        recommendationEngine.predict(ctx, predictionCas);

                        // Extract the suggestions from the data which the recommender has written 
                        // into the CAS
                        List<AnnotationSuggestion> suggestions = extractSuggestions(aUser,
                                predictionCas,
                                document, recommender);
                        
                        // Calculate the visibility of the suggestions. This happens via the 
                        // original CAS which contains only the manually created annotations and 
                        // *not* the suggestions.
                        Collection<SuggestionGroup> groups = SuggestionGroup.group(suggestions);
                        calculateVisibility(originalCas.get(), username, layer,
                                groups, 0, originalCas.get().getDocumentText().length());

                        predictions.putPredictions(layer.getId(), suggestions);
                    }
                    catch (Throwable e) {
                        log.error(
                                "Error applying recommender [{}]({}) for user [{}] to document "
                                        + "[{}]({}) in project [{}]({}) - skipping recommender",
                                recommender.getName(), recommender.getId(), username,
                                document.getName(), document.getId(),
                                document.getProject().getName(), document.getProject().getId(), e);
                        continue nextRecommender;
                    }
                }
            }
        }

        return predictions;
    }

    private List<AnnotationSuggestion> extractSuggestions(User aUser, CAS aCas,
                                                          SourceDocument aDocument,
                                                          Recommender aRecommender)
    {
        String typeName = aRecommender.getLayer().getName();
        String featureName = aRecommender.getFeature().getName();

        Type predictedType = CasUtil.getType(aCas, typeName);
        Feature predictedFeature = predictedType.getFeatureByBaseName(featureName);
        Feature scoreFeature = predictedType.getFeatureByBaseName(featureName + "_score");
        Feature predictionFeature = predictedType.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);

        int predictionCount = 0;

        Type tokenType = getType(aCas, Token.class);

        List<AnnotationSuggestion> result = new ArrayList<>();
        int id = 0;
        for (AnnotationFS annotationFS : CasUtil.select(aCas, predictedType)) {
            if (!annotationFS.getBooleanValue(predictionFeature)) {
                continue;
            }

            List<AnnotationFS> tokens = CasUtil.selectCovered(tokenType, annotationFS);
            AnnotationFS firstToken = tokens.get(0);
            AnnotationFS lastToken = tokens.get(tokens.size() - 1);

            String label = annotationFS.getFeatureValueAsString(predictedFeature);
            double score = annotationFS.getDoubleValue(scoreFeature);
            String name = aRecommender.getName();

            AnnotationSuggestion ao = new AnnotationSuggestion(id, aRecommender.getId(), name,
                    aRecommender.getLayer().getId(), featureName, aDocument.getName(),
                    firstToken.getBegin(), lastToken.getEnd(), annotationFS.getCoveredText(), label,
                    label, score);

            result.add(ao);
            id++;

            predictionCount++;
        }

        log.debug(
                "[{}]({}) for user [{}] on document "
                        + "[{}]({}) in project [{}]({}) generated {} predictions.",
                aRecommender.getName(), aRecommender.getId(), aUser.getUsername(),
                aDocument.getName(), aDocument.getId(), aRecommender.getProject().getName(),
                aRecommender.getProject().getId(), predictionCount);

        return result;
    }
    
    /**
     * Goes through all AnnotationObjects and determines the visibility of each one
     */
    @Override
    public void calculateVisibility(CAS aCas, String aUser, AnnotationLayer aLayer,
            Collection<SuggestionGroup> aRecommendations, int aWindowBegin, int aWindowEnd)
    {
        // Collect all annotations of the given layer within the view window
        Type type = CasUtil.getType(aCas, aLayer.getName());
        List<AnnotationFS> annotationsInWindow = select(aCas, type).stream()
                .filter(fs -> aWindowBegin <= fs.getBegin() && fs.getEnd() <= aWindowEnd)
                .collect(toList());

        // Collect all suggestions of the given layer within the view window
        List<SuggestionGroup> suggestionsInWindow = aRecommendations.stream()
                // Only suggestions for the given layer
                .filter(group -> group.getLayerId() == aLayer.getId())
                // ... and in the given window
                .filter(group -> {
                    Offset offset = group.getOffset();
                    return aWindowBegin <= offset.getBegin() && offset.getEnd() <= aWindowEnd;
                }).collect(toList());

        // Get all the skipped/rejected entries for the current layer
        List<LearningRecord> recordedAnnotations = learningRecordService.listRecords(aUser,
                aLayer);

        for (AnnotationFeature feature : annoService.listAnnotationFeature(aLayer)) {
            Feature feat = type.getFeatureByBaseName(feature.getName());

            // Reduce the annotations to the ones which have a non-null feature value. We need to
            // use a multi-valued map here because there may be multiple annotations at a
            // given position.
            MultiValuedMap<Offset, AnnotationFS> annotations = new ArrayListValuedHashMap<>();
            annotationsInWindow.stream()
                    .forEach(fs -> annotations.put(new Offset(fs.getBegin(), fs.getEnd()), fs));
            // We need to constructed a sorted list of the keys for the OverlapIterator below
            List<Offset> sortedAnnotationKeys = new ArrayList<>(annotations.keySet());
            sortedAnnotationKeys
                    .sort(comparingInt(Offset::getBegin).thenComparingInt(Offset::getEnd));

            // Reduce the suggestions to the ones for the given feature. We can use the tree here
            // since we only have a single SuggestionGroup for every position
            Map<Offset, SuggestionGroup> suggestions = new TreeMap<>(
                    comparingInt(Offset::getBegin).thenComparingInt(Offset::getEnd));
            suggestionsInWindow.stream()
                    .filter(group -> group.getFeature().equals(feature.getName()))
                    .forEach(group -> suggestions.put(group.getOffset(), group));

            // If there are no suggestions or no annotations, there is nothing to do here
            if (suggestions.isEmpty() || annotations.isEmpty()) {
                continue;
            }

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
                    SuggestionGroup group = suggestions.get(oi.getA());
                    for (AnnotationFS annotation : annotations.get(oi.getB())) {
                        String label = annotation.getFeatureValueAsString(feat);
                        for (AnnotationSuggestion suggestion : group) {
                            if (!aLayer.isAllowStacking()
                                    || (label != null && label.equals(suggestion.getLabel()))
                                    || suggestion.getLabel() == null) {
                                suggestion.hide(FLAG_OVERLAP);
                            }
                        }
                    }

                    // Do not want to process the group again since the relevant annotations are
                    // already hidden
                    oi.ignoraA();
                }
                oi.step();
            }

            // Anything that was not hidden so far might still have been rejected
            suggestions.values().stream().flatMap(SuggestionGroup::stream)
                    .filter(AnnotationSuggestion::isVisible)
                    .forEach(suggestion -> hideSuggestionsRejectedOrSkipped(suggestion,
                            recordedAnnotations));
        }
    }

    private void hideSuggestionsRejectedOrSkipped(AnnotationSuggestion aSuggestion,
            List<LearningRecord> aRecordedRecommendations)
    {
        // If it was rejected or skipped, hide it
        for (LearningRecord record : aRecordedRecommendations) {
            boolean isAtTheSamePlace = record.getOffsetCharacterBegin() == aSuggestion
                    .getBegin() && record.getOffsetCharacterEnd() == aSuggestion.getEnd();
            if (isAtTheSamePlace && aSuggestion.labelEquals(record.getAnnotation())) {
                switch (record.getUserAction()) {
                case REJECTED:
                    aSuggestion.hide(FLAG_REJECTED);
                    break;
                case SKIPPED:
                    aSuggestion.hide(FLAG_SKIPPED);
                    break;
                default:
                    // Nothing to do for the other cases. ACCEPTED annotation are filtered out
                    // because the overlap with a created annotation and the same for CORRECTED
                }
                return;
            }
        }
    }


    
    private CAS cloneCAS(CAS aCAS) throws ResourceInitializationException, CASException
    {
        CAS clone = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
        
        CASCompleteSerializer ser = Serialization.serializeCASComplete((CASMgr) aCAS);
        Serialization.deserializeCASComplete(ser, (CASMgr) clone);
        
        // Make sure JCas is properly initialized too
        clone.getJCas();
        
        return clone;
    }

    public void monkeyPatchTypeSystem(Project aProject, CAS aCas)
            throws UIMAException, IOException {

        try (StopWatch watch = new StopWatch(log, "adding score features")) {
            TypeSystemDescription tsd = annoService.getFullProjectTypeSystem(aProject);

            for (AnnotationLayer layer : annoService.listAnnotationLayer(aProject)) {
                TypeDescription td = tsd.getType(layer.getName());

                if (td == null) {
                    log.debug("Could not monkey patch type [{}]", layer.getName());
                    continue;
                }

                for (FeatureDescription feature : td.getFeatures()) {
                    String scoreFeatureName = feature.getName() + "_score";
                    td.addFeature(scoreFeatureName, "Score feature", CAS.TYPE_NAME_DOUBLE);
                }

                td.addFeature(FEATURE_NAME_IS_PREDICTION, "Is Prediction", CAS.TYPE_NAME_BOOLEAN);
            }

            annoService.upgradeCas(aCas, tsd);
        }
    }
}
