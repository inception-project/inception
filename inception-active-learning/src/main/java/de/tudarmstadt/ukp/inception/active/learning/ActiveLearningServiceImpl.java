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
    public void setSessionActive(User aUser, boolean aSesscionActive)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        state.setSessionActive(aSesscionActive);
    }

    @Override
    public boolean isSessionActive(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        return state.isSessionActive();
    }

    @Override
    public void setHasUnseenRecommendation(User aUser, boolean aHasUnseenRecommendation)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        state.setHasUnseenRecommendation(aHasUnseenRecommendation);
    }

    @Override
    public boolean isHasUnseenRecommendation(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        return state.isHasUnseenRecommendation();
    }

    @Override
    public void setHasSkippedRecommendation(User aUser, boolean aHasSkippedRecommendation)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        state.setHasSkippedRecommendation(aHasSkippedRecommendation);
    }

    @Override
    public boolean isHasSkippedRecommendation(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        return state.isHasSkippedRecommendation();
    }

    @Override
    public void setDoExistRecommender(User aUser, boolean aDoExistRecommenders)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        state.setDoExistRecommenders(aDoExistRecommenders);
    }

    @Override
    public boolean isDoExistRecommender(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        return state.isDoExistRecommenders();
    }

    @Override
    public void setCurrentRecommendation(User aUser, AnnotationObject aCurrentRecommendation)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        state.setCurrentRecommendation(aCurrentRecommendation);
    }

    @Override
    public AnnotationObject getCurrentRecommendation(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        return state.getCurrentRecommendation();
    }

    @Override
    public void setCurrentDifference(User aUser, RecommendationDifference aCurrentDifference)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        state.setCurrentDifference(aCurrentDifference);
    }

    @Override
    public RecommendationDifference getCurrentDifference(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        return state.getCurrentDifference();
    }

    @Override
    public void setSelectedLayer(User aUser, AnnotationLayer aSelectedLayer)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        state.setSelectedLayer(aSelectedLayer);
    }

    @Override
    public AnnotationLayer getSelectedLayer(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        return state.getSelectedLayer();
    }

    @Override
    public void setActiveLearningRecommender(User aUser, ActiveLearningRecommender
        aActiveLearningRecommender)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        state.setActiveLearningRecommender(aActiveLearningRecommender);
    }

    @Override
    public ActiveLearningRecommender getActiveLearningRecommender(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        return state.getActiveLearningRecommender();
    }

    @Override
    public void setLearnSkippedRecommendationTime(User aUser, Date
        aLearnSkippedRecommendationTime)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        state.setLearnSkippedRecommendationTime(aLearnSkippedRecommendationTime);
    }

    @Override
    public Date getLearnSkippedRecommendationTime(User aUser)
    {
        ActiveLearningUserState state = getState(aUser.getUsername());
        return state.getLearnSkippedRecommendationTime();
    }

    private ActiveLearningUserState getState(String aUsername)
    {
        ActiveLearningUserState state;
        state = states.get(aUsername);
        if (state == null) {
            state = new ActiveLearningUserState();
            states.put(aUsername, state);
        }
        return state;
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
        private boolean sessionActive = false;
        private boolean hasUnseenRecommendation = false;
        private boolean hasSkippedRecommendation = false;
        private boolean doExistRecommenders = true;
        private AnnotationObject currentRecommendation;
        private RecommendationDifference currentDifference;
        private AnnotationLayer selectedLayer;
        private ActiveLearningRecommender activeLearningRecommender;
        private Date learnSkippedRecommendationTime;

        private boolean isSessionActive()
        {
            return sessionActive;
        }

        private void setSessionActive(boolean sessionActive)
        {
            this.sessionActive = sessionActive;
        }

        private boolean isHasUnseenRecommendation()
        {
            return hasUnseenRecommendation;
        }

        private void setHasUnseenRecommendation(boolean hasUnseenRecommendation)
        {
            this.hasUnseenRecommendation = hasUnseenRecommendation;
        }

        private boolean isHasSkippedRecommendation()
        {
            return hasSkippedRecommendation;
        }

        private void setHasSkippedRecommendation(boolean hasSkippedRecommendation)
        {
            this.hasSkippedRecommendation = hasSkippedRecommendation;
        }

        private boolean isDoExistRecommenders()
        {
            return doExistRecommenders;
        }

        private void setDoExistRecommenders(boolean doExistRecommenders)
        {
            this.doExistRecommenders = doExistRecommenders;
        }

        private AnnotationObject getCurrentRecommendation()
        {
            return currentRecommendation;
        }

        private void setCurrentRecommendation(AnnotationObject currentRecommendation)
        {
            this.currentRecommendation = currentRecommendation;
        }

        private RecommendationDifference getCurrentDifference()
        {
            return currentDifference;
        }

        private void setCurrentDifference(RecommendationDifference currentDifference)
        {
            this.currentDifference = currentDifference;
        }

        private AnnotationLayer getSelectedLayer()
        {
            return selectedLayer;
        }

        private void setSelectedLayer(AnnotationLayer selectedLayer)
        {
            this.selectedLayer = selectedLayer;
        }

        private ActiveLearningRecommender getActiveLearningRecommender()
        {
            return activeLearningRecommender;
        }

        private void setActiveLearningRecommender(
            ActiveLearningRecommender activeLearningRecommender)
        {
            this.activeLearningRecommender = activeLearningRecommender;
        }

        private Date getLearnSkippedRecommendationTime()
        {
            return learnSkippedRecommendationTime;
        }

        private void setLearnSkippedRecommendationTime(Date learnSkippedRecommendationTime)
        {
            this.learnSkippedRecommendationTime = learnSkippedRecommendationTime;
        }
    }
}
