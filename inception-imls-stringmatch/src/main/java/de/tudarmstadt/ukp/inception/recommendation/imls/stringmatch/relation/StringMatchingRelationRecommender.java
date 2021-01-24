/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.relation;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_TARGET;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;

public class StringMatchingRelationRecommender
    extends RecommendationEngine
{
    public static final Key<MultiValuedMap<Pair<String, String>, String>> KEY_MODEL = new Key<>(
            "model");

    private static final String UNKNOWN_LABEL = "unknown";
    private static final String NO_LABEL = "O";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final StringMatchingRelationRecommenderTraits traits;

    public StringMatchingRelationRecommender(Recommender aRecommender,
            StringMatchingRelationRecommenderTraits aTraits)
    {
        super(aRecommender);

        traits = aTraits;
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException
    {
        MultiValuedMap<Pair<String, String>, String> model = new HashSetValuedHashMap<>();

        for (CAS cas : aCasses) {
            Type predictedType = getPredictedType(cas);
            Feature dependentFeature = predictedType.getFeatureByBaseName(FEAT_REL_TARGET);
            Feature governorFeature = predictedType.getFeatureByBaseName(FEAT_REL_SOURCE);
            Feature predictedFeature = getPredictedFeature(cas);
            Feature attachFeature = getAttachFeature(cas);

            for (AnnotationFS relation : select(cas, predictedType)) {
                AnnotationFS dependent = (AnnotationFS) relation.getFeatureValue(dependentFeature);
                AnnotationFS governor = (AnnotationFS) relation.getFeatureValue(governorFeature);

                String relationLabel = relation.getStringValue(predictedFeature);
                String dependentLabel = dependent.getStringValue(attachFeature);
                String governorLabel = governor.getStringValue(attachFeature);

                if (isBlank(relationLabel) || isBlank(dependentLabel) || isBlank(governorLabel)) {
                    continue;
                }

                Pair<String, String> key = Pair.of(dependentLabel, governorLabel);
                model.put(key, relationLabel);
            }
        }

        aContext.put(KEY_MODEL, model);
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        MultiValuedMap<Pair<String, String>, String> model = aContext.get(KEY_MODEL).orElseThrow(
                () -> new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));

        Type sentenceType = getType(aCas, Sentence.class);

        Type predictedType = getPredictedType(aCas);
        Feature dependentFeature = predictedType.getFeatureByBaseName(FEAT_REL_TARGET);
        Feature governorFeature = predictedType.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature predictedFeature = getPredictedFeature(aCas);
        Feature isPredictionFeature = getIsPredictionFeature(aCas);
        Type attachType = getAttachType(aCas);
        Feature attachFeature = getAttachFeature(aCas);

        for (AnnotationFS sentence : select(aCas, sentenceType)) {
            Collection<AnnotationFS> baseAnnotations = selectCovered(attachType, sentence);
            for (AnnotationFS dependent : baseAnnotations) {
                for (AnnotationFS governor : baseAnnotations) {
                    if (dependent.equals(governor)) {
                        continue;
                    }

                    String dependentLabel = dependent.getStringValue(attachFeature);
                    String governorLabel = governor.getStringValue(attachFeature);

                    Pair<String, String> key = Pair.of(dependentLabel, governorLabel);
                    for (String relationLabel : model.get(key)) {
                        AnnotationFS prediction = aCas.createAnnotation(predictedType,
                                governor.getBegin(), governor.getEnd());
                        prediction.setFeatureValue(governorFeature, governor);
                        prediction.setFeatureValue(dependentFeature, dependent);
                        prediction.setStringValue(predictedFeature, relationLabel);
                        prediction.setBooleanValue(isPredictionFeature, true);
                        aCas.addFsToIndexes(prediction);
                    }
                }
            }
        }
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
        throws RecommendationException
    {
        return null;
    }

    private Type getAttachType(CAS aCas)
    {
        String adjunctLayerName = recommender.getLayer().getAttachType().getName();
        return getType(aCas, adjunctLayerName);
    }

    private Feature getAttachFeature(CAS aCas)
    {
        Type attachType = getAttachType(aCas);
        return attachType.getFeatureByBaseName(traits.getAdjunctFeature());
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        return aContext.get(KEY_MODEL).map(Objects::nonNull).orElse(false);
    }
}
