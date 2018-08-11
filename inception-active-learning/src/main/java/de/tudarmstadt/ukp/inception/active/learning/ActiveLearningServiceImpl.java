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

import java.io.Serializable;
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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
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

    private Map<ActiveLearningUserStateKey, ActiveLearningUserState> states = new
        ConcurrentHashMap<>();

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
    public ActiveLearningUserState getState(ActiveLearningUserStateKey aUserStateKey)
    {
        ActiveLearningUserState state;
        state = states.get(aUserStateKey);
        if (state == null) {
            state = new ActiveLearningUserState();
            states.put(aUserStateKey, state);
        }
        return state;
    }

    @Override
    public void setState(ActiveLearningUserStateKey aUserStateKey,
        ActiveLearningServiceImpl.ActiveLearningUserState aState)
    {
        if (aState == null) {
            aState = new ActiveLearningUserState();
        }
        states.put(aUserStateKey, aState);
    }

    @EventListener
    public void onActiveLearningSessionCompleted(ActiveLearningSessionCompletedEvent aEvent)
    {
        clearState(aEvent.getUser());
    }

//    @EventListener
//    public void onDocumentOpened(DocumentOpenedEvent aEvent)
//    {
//        clearState(aEvent.getUser());
//    }
//
    private void clearState(String aUserName)
    {
        synchronized (states) {
            states.remove(aUserName);
        }
    }

    public static class ActiveLearningUserState implements Serializable
    {
        private static final long serialVersionUID = -167705997822964808L;
        private boolean sessionActive = false;
        private boolean hasUnseenRecommendation = false;
        private boolean hasSkippedRecommendation = false;
        private boolean doExistRecommenders = true;
        private AnnotationObject currentRecommendation;
        private RecommendationDifference currentDifference;
        private AnnotationLayer selectedLayer;
        private ActiveLearningRecommender activeLearningRecommender;
        private Date learnSkippedRecommendationTime;

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

        public ActiveLearningRecommender getActiveLearningRecommender()
        {
            return activeLearningRecommender;
        }

        public void setActiveLearningRecommender(
            ActiveLearningRecommender activeLearningRecommender)
        {
            this.activeLearningRecommender = activeLearningRecommender;
        }

        public Date getLearnSkippedRecommendationTime()
        {
            return learnSkippedRecommendationTime;
        }

        public void setLearnSkippedRecommendationTime(Date learnSkippedRecommendationTime)
        {
            this.learnSkippedRecommendationTime = learnSkippedRecommendationTime;
        }
    }

    public static class ActiveLearningUserStateKey implements Serializable
    {
        private static final long serialVersionUID = -2134294656221484540L;
        private String userName;
        private long projectId;

        public ActiveLearningUserStateKey(String aUserName, long aProjectId)
        {
            userName = aUserName;
            projectId = aProjectId;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ActiveLearningUserStateKey that = (ActiveLearningUserStateKey) o;

            if (projectId != that.projectId)
                return false;
            return userName.equals(that.userName);
        }

        @Override
        public int hashCode()
        {
            int result = userName.hashCode();
            result = 31 * result + (int) (projectId ^ (projectId >>> 32));
            return result;
        }
    }
}
