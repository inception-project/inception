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

import static org.apache.uima.fit.util.CasUtil.getAnnotationType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.TokenObject;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;

/**
 * This consumer predicts new annotations for a given annotation layer, if a classification tool for
 * this layer was selected previously.
 */
public class PredictionTask
    extends Task
{
    private static final double NO_SCORE = 0.0;

    private Logger log = LoggerFactory.getLogger(getClass());
    
    private @Autowired AnnotationSchemaService annoService;
    private @Autowired RecommendationService recommendationService;
    private @Autowired DocumentService documentService;
    
    public PredictionTask(User aUser, Project aProject)
    {
        super(aProject, aUser);
    }

    @Override
    public void run()
    {
        User user = getUser();

        Project project = getProject();
        Predictions model = new Predictions(project, getUser());
        List<SourceDocument> documents = documentService.listSourceDocuments(project);

        for (SourceDocument document : documents) {
            JCas jCas;
            try {
                jCas = documentService.readAnnotationCas(document, user.getUsername());
                annoService.upgradeCas(jCas.getCas(), document, user.getUsername());
            } catch (IOException e) {
                log.error(
                        "Cannot read annotation CAS for user [{}] of document "
                                + "[{}]({}) in project [{}]({}) - skipping document",
                        user.getUsername(), document.getName(), document.getId(), project.getName(),
                        project.getId(), e);
                continue;
            } catch (UIMAException e) {
                log.error(
                        "Cannot upgrade annotation CAS for user [{}] of document "
                                + "[{}]({}) in project [{}]({}) - skipping document",
                        user.getUsername(), document.getName(), document.getId(), project.getName(),
                        project.getId(), e);
                continue;
            }

            for (AnnotationLayer layer : annoService.listAnnotationLayer(project)) {
                if (!layer.isEnabled()) {
                    continue;
                }

                List<Recommender> recommenders = recommendationService
                    .getActiveRecommenders(user, layer);

                for (Recommender r : recommenders) {
                    // Make sure we have the latest recommender config from the DB - the one from
                    // the active recommenders list may be outdated
                    Recommender recommender;
                    try {
                        recommender = recommendationService.getRecommender(r.getId());
                    }
                    catch (NoResultException e) {
                        log.info("[{}][{}]: Recommender no longer available... skipping",
                                user.getUsername(), r.getName());
                        continue;
                    }
                    
                    RecommenderContext ctx = recommendationService.getContext(user, recommender);
                    
                    if (!ctx.isReadyForPrediction()) {
                        log.info("Context for recommender [{}]({}) for user [{}] on document "
                                + "[{}]({}) in project [{}]({}) is not ready yet - skipping recommender",
                                recommender.getName(), recommender.getId(), user.getUsername(),
                                document.getName(), document.getId(), project.getName(),
                                project.getId());
                        continue;
                    }
                    
                    RecommendationEngineFactory<?> factory = recommendationService
                            .getRecommenderFactory(recommender);
                    RecommendationEngine recommendationEngine = factory.build(recommender);

                    try {
                        recommendationEngine.predict(ctx, jCas.getCas());
                    }
                    catch (Throwable e) {
                        log.error("Error applying recommender [{}]({}) for user [{}] to document "
                                        + "[{}]({}) in project [{}]({}) - skipping recommender",
                                recommender.getName(), recommender.getId(), user.getUsername(),
                                document.getName(), document.getId(), project.getName(),
                                project.getId(), e);
                        continue;
                    }

                    String predictedTypeName = recommendationEngine.getPredictedType();
                    String predictedFeatureName = recommendationEngine.getPredictedFeature();
                    Optional<String> scoreFeatureName = recommendationEngine.getScoreFeature();
                    Type predictionType = getAnnotationType(jCas.getCas(), predictedTypeName);
                    Feature labelFeature = predictionType
                            .getFeatureByBaseName(predictedFeatureName);
                    Optional<Feature> scoreFeature = scoreFeatureName
                            .map(predictionType::getFeatureByBaseName);

                    List<AnnotationObject> predictions = extractAnnotations(user, jCas.getCas(),
                        predictionType, labelFeature, scoreFeature, document, recommender);
                    model.putPredictions(layer.getId(), predictions);

                    // In order to just extract the annotations for a single recommender, each
                    // recommender undoes the changes applied in `recommendationEngine.predict`

                    removePredictions(jCas.getCas(), predictionType);
                }
            }
        }

        recommendationService.putIncomingPredictions(getUser(), project, model);
    }

    private List<AnnotationObject> extractAnnotations(User aUser, CAS aCas, Type predictionType,
            Feature predictedFeature, Optional<Feature> aScoreFeature, SourceDocument aDocument,
            Recommender aRecommender)
    {
        int predictionCount = 0;
        
        DocumentMetaData dmd = DocumentMetaData.get(aCas);
        String documentUri = dmd.getDocumentUri();
        
        List<AnnotationObject> result = new ArrayList<>();
        int id = 0;
        for (AnnotationFS annotationFS : CasUtil.select(aCas, predictionType)) {
            List<Token> tokens = JCasUtil.selectCovered(Token.class, annotationFS);
            Token firstToken = tokens.get(0);
            Token lastToken = tokens.get(tokens.size() - 1);

            Offset offset = new Offset();
            offset.setBeginCharacter(firstToken.getBegin());
            offset.setEndCharacter(lastToken.getEnd());

            TokenObject to = new TokenObject(offset, annotationFS.getCoveredText(),
                documentUri, aDocument.getName(), id);

            String label = annotationFS.getFeatureValueAsString(predictedFeature);
            double score = aScoreFeature.map(f -> FSUtil.getFeature(annotationFS, f, Double.class))
                    .orElse(NO_SCORE);
            String featurename = aRecommender.getFeature();
            String name = aRecommender.getName();
            AnnotationObject ao = new AnnotationObject(to, label, label, id, featurename, name,
                    score, aRecommender.getId());

            result.add(ao);
            id++;
            
            predictionCount++;
        }
        
        log.error(
                "[{}]({}) for user [{}] on document "
                        + "[{}]({}) in project [{}]({}) generated {} predictions.",
                aRecommender.getName(), aRecommender.getId(), aUser.getUsername(),
                aDocument.getName(), aDocument.getId(), aRecommender.getProject().getName(),
                aRecommender.getProject().getId(), predictionCount);
        
        return result;
    }

    private void removePredictions(CAS aCas, Type aPredictionType)
    {
        for (AnnotationFS fs : CasUtil.select(aCas, aPredictionType)) {
            aCas.removeFsFromIndexes(fs);
        }
    }
}
