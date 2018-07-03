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
package de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.util.ListUtil;

/**
 * This consumer predicts new annotations for a given annotation layer, if a classification tool for
 * this layer was selected previously.
 */
public class PredictionTask
    extends Task
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private @Autowired AnnotationSchemaService annoService;
    private @Autowired RecommendationService recommendationService;
    private @Autowired DocumentService documentService;
    private @Autowired LearningRecordService learningRecordService;
    
    public PredictionTask(User aUser, Project aProject)
    {
        super(aProject, aUser);
    }

    @Override
    public void run()
    {
        User user = getUser();

        Predictions model = new Predictions(getProject(), getUser()); 

        for (AnnotationLayer layer : annoService.listAnnotationLayer(getProject())) {
            if (!layer.isEnabled()) {
                continue;
            }
            
            List<Recommender> recommenders = recommendationService.getActiveRecommenders(user,
                    layer);

            if (recommenders.isEmpty()) {
                log.debug("[{}][{}]: No active recommenders, skipping prediction.",
                        user.getUsername(), layer.getUiName());
                continue;
            }
            
            for (Recommender recommender : recommenders) {
                long startTime = System.currentTimeMillis();
                
                ClassificationTool<?> ct = recommendationService.getTool(recommender,
                        recommendationService.getMaxSuggestions(user));
                Classifier<?> classifier = ct.getClassifier();

                classifier.setUser(getUser());
                classifier.setProject(getProject());
                classifier.setModel(recommendationService.getTrainedModel(user, recommender));
    
                List<AnnotationObject> predictions = new ArrayList<>();
                
                log.info("[{}][{}]: Predicting labels...", user.getUsername(),
                        recommender.getName());
                
                List<AnnotationDocument> docs = documentService
                        .listAnnotationDocuments(layer.getProject(), user);

                // TODO Reading the CAS is time-intensive, better parallelize it.
                for (AnnotationDocument doc : docs) {
                    try {
                        JCas jcas = documentService.readAnnotationCas(doc);
                        List<AnnotationObject> documentPredictions = filterPredictions(jcas, doc,
                            layer, classifier);

                        predictions.addAll(documentPredictions);
                    }
                    catch (IOException e) {
                        log.error("Cannot read annotation CAS.", e);
                    }
                }
      
                // Tell the predictions who created them
                predictions.forEach(token -> token.setRecommenderId(recommender.getId()));

                if (predictions.isEmpty()) {
                    log.info("[{}][{}]: No prediction data.", user.getUsername(),
                            recommender.getName());
                    return;
                }
                
                model.putPredictions(layer.getId(), predictions);
                
                log.info("[{}][{}]: Prediction complete ({} ms)", user.getUsername(),
                        recommender.getName(), (System.currentTimeMillis() - startTime));
            }
        }
        
        recommendationService.putIncomingPredictions(getUser(), getProject(), model);
    }

    private List<AnnotationObject> filterPredictions(JCas aJcas, AnnotationDocument aDoc,
        AnnotationLayer aLayer, Classifier aClassifier)
    {
        List<List<AnnotationObject>> recommendations = Predictions.getPredictions(
            aClassifier.predict(aJcas, aLayer));

        List<LearningRecord> recordedAnnotations = learningRecordService
            .getAllRecordsByDocumentAndUserAndLayer(aDoc.getDocument(), getUser().getUsername(),
                aLayer);

        for (List<AnnotationObject> token : recommendations) {
            for (AnnotationObject ao : token) {
                boolean hasNoAnnotation = ao.getLabel() == null;
                boolean isOverlappingForFeature = isOverlappingForFeature(ao.getOffset(),
                    ao.getFeature(), aJcas, aLayer);
                boolean isRejected = isRejected(recordedAnnotations, ao);

                if (hasNoAnnotation || isOverlappingForFeature || isRejected) {
                    ao.setVisible(false);
                }
                else {
                    ao.setVisible(true);
                }
            }
        }

        return ListUtil.flattenList(recommendations);
    }

    /**
     * Check if there is already an existing annotation overlapping the prediction
     *
     */
    private boolean isOverlappingForFeature(Offset aOffset, String aFeatureName, JCas aJcas,
        AnnotationLayer aLayer)
    {
        Type type = CasUtil.getType(aJcas.getCas(), aLayer.getName());
        AnnotationFS annoFS = WebAnnoCasUtil.selectSingleFsAt(aJcas,
            type, aOffset.getBeginCharacter(), aOffset.getEndCharacter());
        Feature feature = type.getFeatureByBaseName(aFeatureName);
        return annoFS.getFeatureValue(feature) != null;
    }

    private boolean isRejected(List<LearningRecord> recordedRecommendations, AnnotationObject ao)
    {
        for (LearningRecord record : recordedRecommendations) {
            if (record.getOffsetCharacterBegin() == ao.getOffset().getBeginCharacter()
                && record.getOffsetCharacterEnd() == ao.getOffset().getEndCharacter()
                && record.getAnnotation().equals(ao.getLabel())
                && record.getUserAction().equals(LearningRecordUserAction.REJECTED)) {
                return true;
            }
        }
        return false;
    }
}
