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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
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
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderDeletedEvent;
import de.tudarmstadt.ukp.inception.recommendation.tasks.SelectionTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.TrainingTask;
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

    private static final int TRAININGS_PER_SELECTION = 5;

    private @PersistenceContext EntityManager entityManager;
    
    private @Autowired SessionRegistry sessionRegistry;
    private @Autowired UserDao userRepository;
    private @Autowired RecommenderFactoryRegistry recommenderFactoryRegistry;
    private @Autowired SchedulingService schedulingService;
    
    @Value("${show.learning.curve.diagram:false}")
    public Boolean showLearningCurveDiagram;

    private final ConcurrentMap<Pair<User, Project>, AtomicInteger> trainingTaskCounter;
    private final ConcurrentMap<RecommendationStateKey, RecommendationState> states;

    public RecommendationServiceImpl()
    {
        trainingTaskCounter = new ConcurrentHashMap<>();
        states = new ConcurrentHashMap<>();
    }
    
    public RecommendationServiceImpl(EntityManager entityManager)
    {
        this();
        this.entityManager = entityManager;
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
    public List<Recommender> getEnabledRecommenders(Long aRecommenderId)
    {
        String query = String.join("\n",
                "FROM Recommender WHERE ",
                "id = :id AND ",
                "enabled = :enabled" );

        return entityManager.createQuery(query, Recommender.class)
                .setParameter("id", aRecommenderId)
                .setParameter("enabled", true)
                .getResultList();
    }
    
    @Override
    public List<Recommender> listEnabledRecommenders(Project aProject)
    {
        String query = String.join("\n",
                "FROM Recommender WHERE ",
                "project = :project AND ",
                "enabled = :enabled" );

        return entityManager.createQuery(query, Recommender.class)
                .setParameter("project", aProject)
                .setParameter("enabled", true)
                .getResultList();
    }

    @Override
    @Transactional
    public List<Recommender> listRecommenders(AnnotationLayer aLayer)
    {
        List<Recommender> settings = entityManager
                .createQuery("FROM Recommender WHERE layer = :layer ORDER BY name ASC",
                        Recommender.class)
                .setParameter("layer", aLayer).getResultList();
        return settings;
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
                aEvent.getDocument().getProject());
    }

    @EventListener
    public void onRecommenderDelete(RecommenderDeletedEvent aEvent)
    {
        RecommendationState state = getState(aEvent.getUser(), aEvent.getProject());
        synchronized (state) {
            state.removePredictions(aEvent.getRecommender());
        }
        triggerTrainingAndClassification(aEvent.getUser(), aEvent.getProject());
    }
    
    private void triggerTrainingAndClassification(String aUser, Project aProject)
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
            Task task = new SelectionTask(aProject, user);
            schedulingService.enqueue(task);
        } else {
            Task task = new TrainingTask(user, aProject);
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
        triggerTrainingAndClassification(userName, project);
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
            String aUsername, JCas aJCas, AnnotationLayer layer, AnnotationFeature aFeature,
            String aValue, int aBegin, int aEnd)
        throws AnnotationException
    {
        // The feature of the predicted label
        SpanAdapter adapter = (SpanAdapter) annotationService.getAdapter(layer);
        
        // Check if there is already an annotation of the target type at the given location
        Type type = CasUtil.getType(aJCas.getCas(), adapter.getAnnotationTypeName());
        AnnotationFS annoFS = selectSingleFsAt(aJCas, type, aBegin, aEnd);
        int address;
        if (annoFS != null) {
            // ... if yes, then we update the feature on the existing annotation
            address = getAddr(annoFS);
        }
        else {
            // ... if not, then we create a new annotation - this also takes care of attaching to 
            // an annotation if necessary
            address = getAddr(adapter.add(aDocument, aUsername, aJCas, aBegin, aEnd));
        }

        // Update the feature value
        adapter.setFeatureValue(aDocument, aUsername, aJCas, address, aFeature, aValue);
        
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
    public Boolean showLearningCurveDiagram() {
        return showLearningCurveDiagram;
    }
}
