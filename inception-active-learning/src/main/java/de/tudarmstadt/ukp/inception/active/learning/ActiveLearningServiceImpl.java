/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.active.learning;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.uima.jcas.JCas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.DocumentOpenedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSessionCompletedEvent;
import de.tudarmstadt.ukp.inception.active.learning.sidebar.ActiveLearningRecommender;
import de.tudarmstadt.ukp.inception.active.learning.sidebar.RecommendationDifference;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;

@Component
public class ActiveLearningServiceImpl
    implements ActiveLearningService
{
    private final DocumentService documentService;
    private final RecommendationService recommendationService;

    private Map<String, ActiveLearningUserState> states = new ConcurrentHashMap<>();

    @Autowired
    public ActiveLearningServiceImpl(DocumentService aDocumentService,
            RecommendationService aRecommendationService)
    {
        documentService = aDocumentService;
        recommendationService = aRecommendationService;
    }

    @Override
    public List<List<AnnotationObject>> getRecommendationFromRecommendationModel(
            AnnotatorState aState, AnnotationLayer aLayer)
    {
        Predictions model = recommendationService.getPredictions(aState.getUser(),
                aState.getProject());

        if (model == null) {
            return new ArrayList<>();
        }

        // getRecommendationsForThisDocument(model);
        return getRecommendationsForWholeProject(model, aLayer);
    }

//    private List<List<AnnotationObject>> getRecommendationsForThisDocument(AnnotatorState aState,
//            Predictions model, JCas aJcas, AnnotationLayer aLayer)
//    {
//        int windowBegin = 0;
//        int windowEnd = aJcas.getDocumentText().length() - 1;
//        // TODO #176 use the document Id once it it available in the CAS
//        return model.getPredictions(aState.getDocument().getName(), aLayer, windowBegin,
//                windowEnd, aJcas);
//    }

    @Override
    public List<List<AnnotationObject>> getRecommendationsForWholeProject(Predictions model,
            AnnotationLayer aLayer)
    {
        List<List<AnnotationObject>> result = new ArrayList<>();

        Map<String, List<List<AnnotationObject>>> recommendationsMap = model
            .getPredictionsForWholeProject(aLayer, documentService, true);

        Set<String> documentNameSet = recommendationsMap.keySet();

        for (String documentName : documentNameSet) {
            result.addAll(recommendationsMap.get(documentName));
        }

        return result;
    }
    
    public List<AnnotationObject> getFlattenedRecommendationsFromRecommendationModel(JCas aJcas,
            AnnotatorState aState, AnnotationLayer aSelectedLayer)
    {
        int windowBegin = 0;
        int windowEnd = aJcas.getDocumentText().length() - 1;
        Predictions model = recommendationService.getPredictions(aState.getUser(),
                aState.getProject());
        // TODO #176 use the document Id once it it available in the CAS
        return model.getFlattenedPredictions(aState.getDocument().getName(), aSelectedLayer,
            windowBegin, windowEnd, aJcas, true);
    }

    @Override
    public void putSessionActive(User aUser, Project aProject, boolean aSesscionActive)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            state.setSessionActive(aProject, aSesscionActive);
        }
    }

    @Override
    public boolean getSessionActive(User aUser, Project aProject)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        boolean sessionActive;
        synchronized (state) {
            sessionActive = state.isSessionActive(aProject);
        }
        return sessionActive;
    }

    @Override
    public void putHasUnseenRecommendation(User aUser, Project aProject,
        boolean aHasUnseenRecommendation)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            state.setHasUnseenRecommendation(aProject, aHasUnseenRecommendation);
        }
    }

    @Override
    public boolean getHasUnseenRecommendation(User aUser, Project aProject)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        boolean hasUnseenRecommendation;
        synchronized (state) {
            hasUnseenRecommendation = state.isHasUnseenRecommendation(aProject);
        }
        return hasUnseenRecommendation;
    }

    @Override
    public void putHasSkippedRecommendation(User aUser, Project aProject,
        boolean aHasSkippedRecommendation)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            state.setHasSkippedRecommendation(aProject, aHasSkippedRecommendation);
        }
    }

    @Override
    public boolean getHasSkippedRecommendation(User aUser, Project aProject)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        boolean hasSkippedRecommendation;
        synchronized (state) {
            hasSkippedRecommendation = state.isHasSkippedRecommendation(aProject);
        }
        return hasSkippedRecommendation;
    }

    @Override
    public void putDoExistRecommender(User aUser, Project aProject, boolean aDoExistRecommenders)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            state.setDoExistRecommenders(aProject, aDoExistRecommenders);
        }
    }

    @Override
    public boolean getDoExistRecommenders(User aUser, Project aProject)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        boolean doExistRecommenders;
        synchronized (state) {
            doExistRecommenders = state.isDoExistRecommenders(aProject);
        }
        return doExistRecommenders;
    }

    @Override
    public void putCurrentRecommendation(User aUser, Project aProject,
        AnnotationObject aCurrentRecommendation)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            state.setCurrentRecommendation(aProject, aCurrentRecommendation);
        }
    }

    @Override
    public AnnotationObject getCurrentRecommendation(User aUser, Project aProject)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        AnnotationObject currentRecommendation;
        synchronized (state) {
            currentRecommendation = state.getCurrentRecommendation(aProject);
        }
        return currentRecommendation;
    }

    @Override
    public void putCurrentDifference(User aUser, Project aProject,
        RecommendationDifference aCurrentDifference)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            state.setCurrentDifference(aProject, aCurrentDifference);
        }
    }

    @Override
    public RecommendationDifference getCurrentDifference(User aUser, Project aProject)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        RecommendationDifference currentDifference;
        synchronized (state) {
            currentDifference = state.getCurrentDifference(aProject);
        }
        return currentDifference;
    }

    @Override
    public void putSelectedLayer(User aUser, Project aProject, AnnotationLayer aSelectedLayer)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            state.setSelectedLayer(aProject, aSelectedLayer);
        }
    }

    @Override
    public AnnotationLayer getSelectedLayer(User aUser, Project aProject)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        AnnotationLayer selectedLayer;
        synchronized (state) {
            selectedLayer = state.getSelectedLayer(aProject);
        }
        return selectedLayer;
    }

    @Override
    public void putActiveLearningRecommender(User aUser, Project aProject,
        ActiveLearningRecommender aActiveLearningRecommender)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            state.setActiveLearningRecommender(aProject, aActiveLearningRecommender);
        }
    }

    @Override
    public ActiveLearningRecommender getActiveLearningRecommender(User aUser, Project aProject)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        ActiveLearningRecommender activeLearningRecommender;
        synchronized (state) {
            activeLearningRecommender = state.getActiveLearningRecommender(aProject);
        }
        return activeLearningRecommender;
    }

    @Override
    public void putLearnSkippedRecommendationTime(User aUser, Project aProject,
        Date aLearnSkippedRecommendationTime)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        synchronized (state) {
            state.setLearnSkippedRecommendationTime(aProject, aLearnSkippedRecommendationTime);
        }
    }

    @Override
    public Date getLearnSkippedRecommendationTime(User aUser, Project aProject)
    {
        ActiveLearningUserState state = getState(aUser.getUsername(), aProject);
        Date learnSkippedRecommendationTime;
        synchronized (state) {
            learnSkippedRecommendationTime = state.getLearnSkippedRecommendationTime(aProject);
        }
        return learnSkippedRecommendationTime;
    }

    private ActiveLearningUserState getState(String aUsername, Project aProject)
    {
        synchronized (states) {
            ActiveLearningUserState state;
            state = states.get(aUsername);
            if (state == null) {
                state = new ActiveLearningUserState();
                state.initializeActiveLearningUserState(aProject);
                states.put(aUsername, state);
            }
            return state;
        }
    }

    @EventListener
    public void onActiveLearningSessionCompleted(ActiveLearningSessionCompletedEvent aEvent)
    {
        clearState(aEvent.getUser());
    }

    @EventListener
    public void onDocumentOpened(DocumentOpenedEvent aEvent)
    {
        clearState(aEvent.getUser());
    }

    private void clearState(String aUserName)
    {
        synchronized (states) {
            states.remove(aUserName);
        }
    }

    private static class ActiveLearningUserState
    {
        private Map<Long, Boolean> sessionActive = new HashMap<>();
        private Map<Long, Boolean> hasUnseenRecommendation = new HashMap<>();
        private Map<Long, Boolean> hasSkippedRecommendation = new HashMap<>();
        private Map<Long, Boolean> doExistRecommenders = new HashMap<>();
        private Map<Long, AnnotationObject> currentRecommendation = new HashMap<>();
        private Map<Long, RecommendationDifference> currentDifference = new HashMap<>();
        private Map<Long, AnnotationLayer> selectedLayer = new HashMap<>();
        private Map<Long, ActiveLearningRecommender> activeLearningRecommender = new HashMap<>();
        private Map<Long, Date> learnSkippedRecommendationTime = new HashMap<>();

        private void initializeActiveLearningUserState(Project aProject)
        {
            setSessionActive(aProject, false);
            setHasUnseenRecommendation(aProject, false);
            setHasSkippedRecommendation(aProject, false);
            setDoExistRecommenders(aProject, true);
        }

        private boolean isSessionActive(Project aProject)
        {
            return sessionActive.get(aProject.getId());
        }

        private void setSessionActive(Project aProject, boolean aSessionActive)
        {
            sessionActive.put(aProject.getId(), aSessionActive);
        }

        private boolean isHasUnseenRecommendation(Project aProject)
        {
            return hasUnseenRecommendation.get(aProject.getId());
        }

        private void setHasUnseenRecommendation(Project aProject, boolean aHasUnseenRecommendation)
        {
            hasUnseenRecommendation.put(aProject.getId(), aHasUnseenRecommendation);
        }

        private boolean isHasSkippedRecommendation(Project aProject)
        {
            return hasSkippedRecommendation.get(aProject.getId());
        }

        private void setHasSkippedRecommendation(Project aProject,
            boolean aHasSkippedRecommendation)
        {
            hasSkippedRecommendation.put(aProject.getId(), aHasSkippedRecommendation);
        }

        private boolean isDoExistRecommenders(Project aProject)
        {
            return doExistRecommenders.get(aProject.getId());
        }

        private void setDoExistRecommenders(Project aProject, boolean aDoExistRecommenders)
        {
            doExistRecommenders.put(aProject.getId(), aDoExistRecommenders);
        }

        private AnnotationObject getCurrentRecommendation(Project aProject)
        {
            return currentRecommendation.get(aProject.getId());
        }

        private void setCurrentRecommendation(Project aProject,
            AnnotationObject aCurrentRecommendation)
        {
            currentRecommendation.put(aProject.getId(), aCurrentRecommendation);
        }

        private RecommendationDifference getCurrentDifference(Project aProject)
        {
            return currentDifference.get(aProject.getId());
        }

        private void setCurrentDifference(Project aProject,
            RecommendationDifference aCurrentDifference)
        {
            currentDifference.put(aProject.getId(), aCurrentDifference);
        }

        private AnnotationLayer getSelectedLayer(Project aProject)
        {
            return selectedLayer.get(aProject.getId());
        }

        private void setSelectedLayer(Project aProject, AnnotationLayer aSelectedLayer)
        {
            selectedLayer.put(aProject.getId(), aSelectedLayer);
        }

        private ActiveLearningRecommender getActiveLearningRecommender(Project aProject)
        {
            return activeLearningRecommender.get(aProject.getId());
        }

        private void setActiveLearningRecommender(Project aProject,
            ActiveLearningRecommender aActiveLearningRecommender)
        {
            activeLearningRecommender.put(aProject.getId(), aActiveLearningRecommender);
        }

        private Date getLearnSkippedRecommendationTime(Project aProject)
        {
            return learnSkippedRecommendationTime.get(aProject.getId());
        }

        private void setLearnSkippedRecommendationTime(Project aProject,
            Date aLearnSkippedRecommendationTime)
        {
            learnSkippedRecommendationTime.put(aProject.getId(), aLearnSkippedRecommendationTime);
        }
    }
}
