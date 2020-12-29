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
package de.tudarmstadt.ukp.inception.conceptlinking.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;

public class NamedEntityLinker
    extends RecommendationEngine
{
    private NamedEntityLinkerTraits traits;

    private KnowledgeBaseService kbService;
    private ConceptLinkingService clService;
    private FeatureSupportRegistry fsRegistry;
    private ConceptFeatureTraits featureTraits;

    public static final Key<Collection<ImmutablePair<String, Collection<AnnotationFS>>>> KEY_MODEL = new Key<>(
            "model");

    public NamedEntityLinker(Recommender aRecommender, NamedEntityLinkerTraits aTraits,
            KnowledgeBaseService aKbService, ConceptLinkingService aClService,
            FeatureSupportRegistry aFsRegistry, ConceptFeatureTraits aFeatureTraits)
    {
        super(aRecommender);

        traits = aTraits;
        kbService = aKbService;
        clService = aClService;
        fsRegistry = aFsRegistry;
        featureTraits = aFeatureTraits;
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        return true;
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasList)
    {
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        Type predictedType = getPredictedType(aCas);

        for (AnnotationFS sentence : selectSentences(aCas)) {
            for (AnnotationFS annotation : CasUtil.selectCovered(aCas, predictedType, sentence)) {
                int begin = annotation.getBegin();
                int end = annotation.getEnd();
                predictSingle(annotation.getCoveredText(), begin, end, aCas);
            }
        }
    }

    private void predictSingle(String aCoveredText, int aBegin, int aEnd, CAS aCas)
    {
        List<KBHandle> handles = new ArrayList<>();

        AnnotationFeature feat = recommender.getFeature();
        FeatureSupport<ConceptFeatureTraits> fs = fsRegistry.getFeatureSupport(feat);
        ConceptFeatureTraits conceptFeatureTraits = fs.readTraits(feat);

        if (conceptFeatureTraits.getRepositoryId() != null) {
            Optional<KnowledgeBase> kb = kbService.getKnowledgeBaseById(recommender.getProject(),
                    conceptFeatureTraits.getRepositoryId());
            if (kb.isPresent() && kb.get().isSupportConceptLinking()) {
                handles.addAll(readCandidates(kb.get(), aCoveredText, aBegin, aCas));
            }
        }
        else {
            for (KnowledgeBase kb : kbService.getEnabledKnowledgeBases(recommender.getProject())) {
                if (kb.isSupportConceptLinking()) {
                    handles.addAll(readCandidates(kb, aCoveredText, aBegin, aCas));
                }
            }
        }

        Type predictedType = getPredictedType(aCas);
        Feature scoreFeature = getScoreFeature(aCas);
        Feature predictedFeature = getPredictedFeature(aCas);
        Feature isPredictionFeature = getIsPredictionFeature(aCas);

        for (KBHandle prediction : handles.stream().limit(recommender.getMaxRecommendations())
                .collect(Collectors.toList())) {
            AnnotationFS annotation = aCas.createAnnotation(predictedType, aBegin, aEnd);
            annotation.setStringValue(predictedFeature, prediction.getIdentifier());
            annotation.setBooleanValue(isPredictionFeature, true);
            aCas.addFsToIndexes(annotation);
        }
    }

    private List<KBHandle> readCandidates(KnowledgeBase kb, String aCoveredText, int aBegin,
            CAS aCas)
    {
        return kbService.read(kb, (conn) -> clService.disambiguate(kb, featureTraits.getScope(),
                featureTraits.getAllowedValueType(), null, aCoveredText, aBegin, aCas));
    }

    @Override
    public RecommendationEngineCapability getTrainingCapability()
    {
        return RecommendationEngineCapability.TRAINING_NOT_SUPPORTED;
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        EvaluationResult result = new EvaluationResult();
        result.setEvaluationSkipped(true);
        result.setErrorMsg("NamedEntityLinker does not support evaluation.");
        return result;
    }
}
