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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.uima.jcas.JCas;
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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.DocumentOpenedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterAnnotationUpdateEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentResetEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationToolRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderDeletedEvent;
import de.tudarmstadt.ukp.inception.recommendation.scheduling.RecommendationScheduler;

/**
 * The implementation of the RecommendationService.
 */
@Component(RecommendationService.SERVICE_NAME)
public class RecommendationServiceImpl
    implements RecommendationService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;
    
    private @Autowired SessionRegistry sessionRegistry;
    private @Autowired UserDao userRepository;
    private @Autowired ClassificationToolRegistry classificationToolRegistry;
    private @Autowired RecommendationScheduler scheduler;
    
    private Map<String, RecommendationUserState> states = new ConcurrentHashMap<>();

    @Override
    public Predictions getPredictions(User aUser, Project aProject)
    {
        RecommendationUserState state = getState(aUser.getUsername());
        return state.getActivePredictions(aProject);
    }
    
    @Override
    public Predictions getIncomingPredictions(User aUser, Project aProject)
    {
        RecommendationUserState state = getState(aUser.getUsername());
        Predictions predictions;
        synchronized (state) {
            predictions = state.getIncomingPredictions(aProject);
        }
        return predictions;
    }
    
    @Override
    public void putIncomingPredictions(User aUser, Project aProject, Predictions aPredictions)
    {
        RecommendationUserState state = getState(aUser.getUsername());
        synchronized (state) {
            state.putIncomingPredictions(aProject, aPredictions);
        }
    }
    
    @Override
    public void setActiveRecommenders(User aUser, AnnotationLayer aLayer,
            List<Recommender> aRecommenders)
    {
        RecommendationUserState state = getState(aUser.getUsername());
        MultiValuedMap<AnnotationLayer, Recommender> activeRecommenders = 
                state.getActiveRecommenders();
        synchronized (activeRecommenders) {
            activeRecommenders.putAll(aLayer, aRecommenders);
        }
    }
    
    @Override
    public List<Recommender> getActiveRecommenders(User aUser, AnnotationLayer aLayer)
    {
        RecommendationUserState state = getState(aUser.getUsername());
        MultiValuedMap<AnnotationLayer, Recommender> activeRecommenders = 
                state.getActiveRecommenders();
        List<Recommender> result = new ArrayList<>();
        synchronized (activeRecommenders) {
            result.addAll(activeRecommenders.get(aLayer));
        }
        return result;
    }

    @Override
    @Transactional
    public void createOrUpdateRecommender(Recommender aSettings)
    {
        if (aSettings.getId() == null) {
            entityManager.persist(aSettings);
        }
        else {
            entityManager.merge(aSettings);
        }
    }

    @Override
    @Transactional
    public void deleteRecommender(Recommender aSettings)
    {
        Recommender settings = aSettings;

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
    public List<Recommender> listRecommenders(AnnotationLayer aLayer)
    {
        List<Recommender> settings = entityManager
                .createQuery("FROM Recommender WHERE layer = :layer ORDER BY name ASC",
                        Recommender.class)
                .setParameter("layer", aLayer).getResultList();
        return settings;
    }

    @Override
    public List<String> getAvailableTools(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        return classificationToolRegistry.getAvailableTools(aLayer, aFeature);
    }

    @EventListener
    public void afterAnnotationUpdate(AfterAnnotationUpdateEvent aEvent)
    {
        triggerTrainingAndClassification(aEvent.getDocument().getUser(),
                aEvent.getDocument().getProject());
    }

    @EventListener
    public void onDocumentOpen(DocumentOpenedEvent aEvent)
    {
        triggerTrainingAndClassification(aEvent.getUser(), aEvent.getDocument().getProject());
    }

    @EventListener
    public void onRecommenderDelete(RecommenderDeletedEvent aEvent)
    {
        RecommendationUserState state = getState(aEvent.getUser());
        state.removePredictions(aEvent.getRecommender());
        triggerTrainingAndClassification(aEvent.getUser(), aEvent.getProject());
    }
    
    private void triggerTrainingAndClassification(String aUser, Project aProject)
    {
        User user = userRepository.get(aUser);
        scheduler.enqueueTask(user, aProject);
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
            scheduler.stopAllTasksForUser(username);
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
    public void setMaxSuggestions(User aUser, int aMax)
    {
        RecommendationUserState state = getState(aUser.getUsername());
        state.setMaxPredictions(aMax);
    }
    
    @Override
    public int getMaxSuggestions(User aUser)
    {
        RecommendationUserState state = getState(aUser.getUsername());
        return state.getMaxPredictions();
    }
    
    @Override
    public ClassificationTool<?> getTool(Recommender aSettings, int aMaxPredictions)
    {
        return classificationToolRegistry.createClassificationTool(aSettings, aMaxPredictions);
    }
    
    @Override
    public void storeTrainedModel(User aUser, Recommender aRecommender, Object aModel)
    {
        RecommendationUserState state = getState(aUser.getUsername());
        state.putTrainedModel(aRecommender, aModel);
    }

    @Override
    public Object getTrainedModel(User aUser, Recommender aRecommender)
    {
        RecommendationUserState state = getState(aUser.getUsername());
        return state.getTrainedModel(aRecommender);
    }
    
    private RecommendationUserState getState(String aUsername)
    {
        synchronized (states) {
            RecommendationUserState state;
            state = states.get(aUsername);
            if (state == null) {
                state = new RecommendationUserState();
                states.put(aUsername, state);
            }
            return state;
        }
    }
    
    private void clearState(String aUsername)
    {
        synchronized (states) {
            states.remove(aUsername);
        }
    }
    
    @Override
    public void switchPredictions(User aUser, Project aProject)
    {
        RecommendationUserState state = getState(aUser.getUsername());
        synchronized (state) {
            Predictions incomingPredictions = state.getIncomingPredictions(aProject);
            if (incomingPredictions != null) {
                state.putActivePredictions(aProject, incomingPredictions);
                state.clearIncomingPredictions(aProject);
            }
        }
    }

    public void setFeatureValue(FeatureSupportRegistry fsRegistry, AnnotationFeature aFeature,
        String aPredictedValue, SpanAdapter aAdapter, AnnotatorState aState, JCas aJcas,
        int address)
    {
        String fsId = fsRegistry.getFeatureSupport(aFeature).getId();

        if (fsId.equals("conceptFeatureSupport") || fsId.equals("propertyFeatureSupport")) {
            String uiName = fsRegistry.getFeatureSupport(aFeature)
                .renderFeatureValue(aFeature, aPredictedValue);
            KBHandle kbHandle = new KBHandle(aPredictedValue, uiName);
            aAdapter.setFeatureValue(aState, aJcas, address, aFeature, kbHandle);
        }
        else {
            aAdapter.setFeatureValue(aState, aJcas, address, aFeature, aPredictedValue);
        }
    }

    /**
     * We are assuming that the user is actively working on one project at a time.
     * Otherwise, the RecommendationUserState might take up a lot of memory.
     */
    private static class RecommendationUserState
    {
        private int maxPredictions = 3;
        private MultiValuedMap<AnnotationLayer, Recommender> activeRecommenders = 
                new HashSetValuedHashMap<>();
        private Map<Long, Object> trainedModels = new ConcurrentHashMap<>();
        private Map<Long, Predictions> activePredictions = new ConcurrentHashMap<>();
        private Map<Long, Predictions> incomingPredictions = new ConcurrentHashMap<>();
        
        public MultiValuedMap<AnnotationLayer, Recommender> getActiveRecommenders()
        {
            return activeRecommenders;
        }

        public void setActiveRecommenders(
            MultiValuedMap<AnnotationLayer, Recommender> aActiveRecommenders)
        {
            activeRecommenders = aActiveRecommenders;
        }
        
        public void setMaxPredictions(int aMaxPredictions)
        {
            maxPredictions = aMaxPredictions;
        }
        
        public int getMaxPredictions()
        {
            return maxPredictions;
        }
        
        public void putActivePredictions(Project aProject, Predictions aPredictions)
        {
            activePredictions.put(aProject.getId(), aPredictions);
        }
        
        public Predictions getActivePredictions(Project aProject)
        {
            return activePredictions.get(aProject.getId());
        }
        
        public void putIncomingPredictions(Project aProject, Predictions aPredictions)
        {
            incomingPredictions.put(aProject.getId(), aPredictions);
        }

        public Predictions getIncomingPredictions(Project aProject)
        {
            return incomingPredictions.get(aProject.getId());
        }

        public void clearIncomingPredictions(Project aProject)
        {
            incomingPredictions.remove(aProject.getId());            
        }

        public Object getTrainedModel(Recommender aRecommender)
        {
            return trainedModels.get(aRecommender.getId());
        }
        
        public void putTrainedModel(Recommender aRecommender, Object aModel)
        {
            trainedModels.put(aRecommender.getId(), aModel);
        }

        public void removePredictions(Recommender aRecommender)
        {
            // Remove incoming predictions
            Predictions incoming = incomingPredictions.get(aRecommender.getProject().getId());
            if (incoming != null) {
                incoming.removePredictions(aRecommender.getId());
                incomingPredictions.put(aRecommender.getProject().getId(), incoming);
            }

            // Remove active predictions
            Predictions active = activePredictions.get(aRecommender.getProject().getId());
            if (active != null) {
                active.removePredictions(aRecommender.getId());
                activePredictions.put(aRecommender.getProject().getId(), active);
            }

            // Remove trainedModel
            trainedModels.remove(aRecommender.getId());

            // Remove from activeRecommenders map.
            // We have to do this, otherwise training and prediction continues for the
            // recommender when a new task is triggered.
            MultiValuedMap<AnnotationLayer, Recommender> newActiveRecommenders
                = new HashSetValuedHashMap<>();
            MapIterator<AnnotationLayer, Recommender> it
                = activeRecommenders.mapIterator();

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
}
