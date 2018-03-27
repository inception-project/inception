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

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.DocumentOpenedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterAnnotationUpdateEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.classificationtool.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.model.ClassificationToolRegistry;
import de.tudarmstadt.ukp.inception.recommendation.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.scheduling.RecommendationScheduler;

/**
 * The implementation of the RecommendationService.
 */
@Component(RecommendationService.SERVICE_NAME)
public class RecommendationServiceImpl
    implements RecommendationService
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;
    
    private @Autowired SessionRegistry sessionRegistry;
    private @Autowired AnnotationSchemaService annoService;
    private @Autowired DocumentService docService;
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
            if (predictions == null) {
                predictions = new Predictions(aProject, aUser); 
                state.putIncomingPredictions(aProject, predictions);
            }
        }
        return predictions;
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
    public void afterAnnotationUpdate(AfterAnnotationUpdateEvent aEvent) throws Exception
    {
        triggerTrainingAndClassification(aEvent.getDocument().getUser(),
                aEvent.getDocument().getProject());
    }

    @EventListener
    public void onDocumentOpen(DocumentOpenedEvent aEvent) throws Exception
    {
        triggerTrainingAndClassification(aEvent.getUser(), aEvent.getDocument().getProject());
    }
    
    private void triggerTrainingAndClassification(String aUser, Project aProject)
    {
        User user = userRepository.get(aUser);
        Predictions model = getIncomingPredictions(user, aProject);
        if (model == null) {
            return;
        }
        scheduler.enqueueTask(user, aProject, model);
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
    }
}
