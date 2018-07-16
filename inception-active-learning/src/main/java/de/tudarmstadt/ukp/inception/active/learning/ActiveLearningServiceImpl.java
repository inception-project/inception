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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.active.learning.sidebar.RecommendationDifference;
import org.apache.uima.jcas.JCas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
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
    public void putSessionActive(User aUser, boolean aSesscionActive)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        synchronized (state) {
            state.setSessionActive(aSesscionActive);
        }
    }

    @Override
    public boolean getSessionActive(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        boolean sessionActive;
        synchronized (state) {
            sessionActive = state.isSessionActive();
        }
        return sessionActive;
    }

    @Override
    public void putHasUnseenRecommendation(User aUser, boolean aHasUnseenRecommendation)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        synchronized (state) {
            state.setHasUnseenRecommendation(aHasUnseenRecommendation);
        }
    }

    @Override
    public boolean getHasUnseenRecommendation(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        boolean hasUnseenRecommendation;
        synchronized (state) {
            hasUnseenRecommendation = state.isHasUnseenRecommendation();
        }
        return hasUnseenRecommendation;
    }

    @Override
    public void putHasSkippedRecommendation(User aUser, boolean aHasSkippedRecommendation)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        synchronized (state) {
            state.setHasSkippedRecommendation(aHasSkippedRecommendation);
        }
    }

    @Override
    public boolean getHasSkippedRecommendation(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        boolean hasSkippedRecommendation;
        synchronized (state) {
            hasSkippedRecommendation = state.isHasSkippedRecommendation();
        }
        return hasSkippedRecommendation;
    }

    @Override
    public void putDoExistRecommender(User aUser, boolean aDoExistRecommenders)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        synchronized (state) {
            state.setDoExistRecommenders(aDoExistRecommenders);
        }
    }

    @Override
    public boolean getDoExistRecommenders(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        boolean doExistRecommenders;
        synchronized (state) {
            doExistRecommenders = state.isDoExistRecommenders();
        }
        return doExistRecommenders;
    }

    @Override
    public void putCurrentRecommendation(User aUser, AnnotationObject aCurrentRecommendation)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        synchronized (state) {
            state.setCurrentRecommendation(aCurrentRecommendation);
        }
    }

    @Override
    public AnnotationObject getCurrentRecommendation(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        AnnotationObject currentRecommendation;
        synchronized (state) {
            currentRecommendation = state.getCurrentRecommendation();
        }
        return currentRecommendation;
    }

    @Override
    public void putCurrentDifference(User aUser, RecommendationDifference aCurrentDifference)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        synchronized (state) {
            state.setCurrentDifference(aCurrentDifference);
        }
    }

    @Override
    public RecommendationDifference getCurrentDifference(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        RecommendationDifference currentDifference;
        synchronized (state) {
            currentDifference = state.getCurrentDifference();
        }
        return currentDifference;
    }

    @Override
    public void putSelectedLayer(User aUser, AnnotationLayer aSelectedLayer)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        synchronized (state) {
            state.setSelectedLayer(aSelectedLayer);
        }
    }

    @Override
    public AnnotationLayer getAnnotationLayer(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        AnnotationLayer selectedLayer;
        synchronized (state) {
            selectedLayer = state.getSelectedLayer();
        }
        return selectedLayer;
    }

    @Override
    public void putFeatureState(User aUser, FeatureState aFeatureState)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        synchronized (state) {
            state.setFeatureState(aFeatureState);
        }
    }

    @Override
    public FeatureState getFeatureState(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        FeatureState featureState;
        synchronized (state) {
            featureState = state.getFeatureState();
        }
        return featureState;
    }

    private ActiveLearningUserState getState(String aUsername)
    {
        synchronized (states) {
            ActiveLearningUserState state;
            state = states.get(aUsername);
            if (state ==null) {
                state = new ActiveLearningUserState();
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

    private static class ActiveLearningUserState
    {
        //TODO: change them to Map for multiple projects
        private boolean sessionActive = false;
        private boolean hasUnseenRecommendation = false;
        private boolean hasSkippedRecommendation = false;
        private boolean doExistRecommenders = true;
        private AnnotationObject currentRecommendation;
        private RecommendationDifference currentDifference;
        private AnnotationLayer selectedLayer;
        private FeatureState featureState;

        public boolean isSessionActive()
        {
            return sessionActive;
        }

        public void setSessionActive(boolean sessionActive)
        {
            this.sessionActive = sessionActive;
        }

        public boolean isHasUnseenRecommendation()
        {
            return hasUnseenRecommendation;
        }

        public void setHasUnseenRecommendation(boolean hasUnseenRecommendation)
        {
            this.hasUnseenRecommendation = hasUnseenRecommendation;
        }

        public boolean isHasSkippedRecommendation()
        {
            return hasSkippedRecommendation;
        }

        public void setHasSkippedRecommendation(boolean hasSkippedRecommendation)
        {
            this.hasSkippedRecommendation = hasSkippedRecommendation;
        }

        public boolean isDoExistRecommenders()
        {
            return doExistRecommenders;
        }

        public void setDoExistRecommenders(boolean doExistRecommenders)
        {
            this.doExistRecommenders = doExistRecommenders;
        }

        public AnnotationObject getCurrentRecommendation()
        {
            return currentRecommendation;
        }

        public void setCurrentRecommendation(AnnotationObject currentRecommendation)
        {
            this.currentRecommendation = currentRecommendation;
        }

        public RecommendationDifference getCurrentDifference()
        {
            return currentDifference;
        }

        public void setCurrentDifference(RecommendationDifference currentDifference)
        {
            this.currentDifference = currentDifference;
        }

        public AnnotationLayer getSelectedLayer()
        {
            return selectedLayer;
        }

        public void setSelectedLayer(AnnotationLayer selectedLayer)
        {
            this.selectedLayer = selectedLayer;
        }

        public FeatureState getFeatureState()
        {
            return featureState;
        }

        public void setFeatureState(FeatureState featureState)
        {
            this.featureState = featureState;
        }
    }
}
