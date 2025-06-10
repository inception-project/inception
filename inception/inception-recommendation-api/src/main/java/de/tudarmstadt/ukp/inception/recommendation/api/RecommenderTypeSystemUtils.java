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
package de.tudarmstadt.ukp.inception.recommendation.api;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_CORRECTION_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_CORRECTION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_SUFFIX;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.TypeSystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;

public class RecommenderTypeSystemUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static CAS makePredictionCas(CAS aOriginalCas, AnnotationFeature... aFeatures)
        throws ResourceInitializationException
    {
        var tsd = TypeSystemUtil.typeSystem2TypeSystemDescription(aOriginalCas.getTypeSystem());
        RecommenderTypeSystemUtils.addPredictionFeaturesToTypeSystem(tsd, asList(aFeatures));
        var predictionCas = CasFactory.createCas(tsd);
        predictionCas.setDocumentText(aOriginalCas.getDocumentText());
        CasCopier.copyCas(aOriginalCas, predictionCas, false);
        return predictionCas;
    }

    public static void addPredictionFeaturesToTypeSystem(TypeSystemDescription tsd,
            List<AnnotationFeature> features)
    {
        for (var feature : features) {
            var td = tsd.getType(feature.getLayer().getName());
            if (td == null) {
                if (!WebAnnoConst.CHAIN_TYPE.equals(feature.getLayer().getType())) {
                    LOG.trace("Could not monkey patch feature {} because type for layer {} was not "
                            + "found in the type system", feature, feature.getLayer());
                }
                continue;
            }

            td.addFeature(feature.getName() + FEATURE_NAME_SCORE_SUFFIX, "Score", TYPE_NAME_DOUBLE);

            td.addFeature(feature.getName() + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX,
                    "Score explanation feature", TYPE_NAME_STRING);

            td.addFeature(feature.getName() + FEATURE_NAME_CORRECTION_SUFFIX, "Correction flag",
                    TYPE_NAME_BOOLEAN);

            td.addFeature(feature.getName() + FEATURE_NAME_CORRECTION_EXPLANATION_SUFFIX,
                    "Correction explanation", TYPE_NAME_STRING);

            td.addFeature(feature.getName() + FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX,
                    "Suggestion mode", TYPE_NAME_STRING);
        }

        var layers = features.stream().map(AnnotationFeature::getLayer).distinct().toList();
        for (var layer : layers) {
            var td = tsd.getType(layer.getName());
            if (td == null) {
                LOG.trace("Could not monkey patch layer {} because its type was not found in "
                        + "the type system", layer);
                continue;
            }

            td.addFeature(FEATURE_NAME_IS_PREDICTION, "Is Prediction", TYPE_NAME_BOOLEAN);
        }
    }
}
