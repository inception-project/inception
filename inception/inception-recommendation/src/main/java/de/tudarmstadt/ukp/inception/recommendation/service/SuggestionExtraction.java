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
package de.tudarmstadt.ukp.inception.recommendation.service;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.cas.TOP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AutoAcceptMode;
import de.tudarmstadt.ukp.inception.recommendation.api.model.MetadataSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.relation.RelationSuggestionSupport;
import de.tudarmstadt.ukp.inception.recommendation.span.SpanSuggestionSupport;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;

public class SuggestionExtraction
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String AUTO_ACCEPT_ON_FIRST_ACCESS = "on-first-access";

    public static AutoAcceptMode getAutoAcceptMode(FeatureStructure aFS, Feature aModeFeature)
    {
        var autoAcceptMode = AutoAcceptMode.NEVER;
        var autoAcceptFeatureValue = aFS.getStringValue(aModeFeature);
        if (autoAcceptFeatureValue != null) {
            switch (autoAcceptFeatureValue) {
            case AUTO_ACCEPT_ON_FIRST_ACCESS:
                autoAcceptMode = AutoAcceptMode.ON_FIRST_ACCESS;
            }
        }
        return autoAcceptMode;
    }

    public static String[] getPredictedLabels(FeatureStructure predictedFS,
            Feature predictedFeature, boolean isStringMultiValue)
    {
        if (isStringMultiValue) {
            return FSUtil.getFeature(predictedFS, predictedFeature, String[].class);
        }

        return new String[] { predictedFS.getFeatureValueAsString(predictedFeature) };
    }

    static void extractDocumentMetadataSuggestion(ExtractionContext ctx, TOP predictedFS)
    {
        var autoAcceptMode = getAutoAcceptMode(predictedFS, ctx.getModeFeature());
        var labels = getPredictedLabels(predictedFS, ctx.getLabelFeature(), ctx.isMultiLabels());
        var score = predictedFS.getDoubleValue(ctx.getScoreFeature());
        var scoreExplanation = predictedFS.getStringValue(ctx.getScoreExplanationFeature());

        for (var label : labels) {
            var suggestion = MetadataSuggestion.builder() //
                    .withId(MetadataSuggestion.NEW_ID) //
                    .withGeneration(ctx.getGeneration()) //
                    .withRecommender(ctx.getRecommender()) //
                    .withDocumentName(ctx.getDocument().getName()) //
                    .withLabel(label) //
                    .withUiLabel(label) //
                    .withScore(score) //
                    .withScoreExplanation(scoreExplanation) //
                    .withAutoAcceptMode(autoAcceptMode) //
                    .build();
            ctx.getResult().add(suggestion);
        }
    }

    public static List<AnnotationSuggestion> extractSuggestions(int aGeneration, CAS aOriginalCas,
            CAS aPredictionCas, SourceDocument aDocument, Recommender aRecommender)
    {
        var ctx = new ExtractionContext(aGeneration, aRecommender, aDocument, aOriginalCas,
                aPredictionCas);

        for (var predictedFS : aPredictionCas.select(ctx.getPredictedType())) {
            if (!predictedFS.getBooleanValue(ctx.getPredictionFeature())) {
                continue;
            }

            switch (ctx.getLayer().getType()) {
            case SpanLayerSupport.TYPE: {
                SpanSuggestionSupport.extractSuggestion(ctx, predictedFS);
                break;
            }
            case RelationLayerSupport.TYPE: {
                RelationSuggestionSupport.extractSuggestion(ctx, predictedFS);
                break;
            }
            case DocumentMetadataLayerSupport.TYPE: {
                extractDocumentMetadataSuggestion(ctx, predictedFS);
                break;
            }
            default:
                throw new IllegalStateException(
                        "Unsupported layer type [" + ctx.getLayer().getType() + "]");
            }
        }

        return ctx.getResult();
    }
}
