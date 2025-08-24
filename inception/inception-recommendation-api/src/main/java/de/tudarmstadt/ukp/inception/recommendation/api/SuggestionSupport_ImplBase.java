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

import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_CORRECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.SKIPPED;
import static java.util.Arrays.asList;

import java.util.Objects;

import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.event.RecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.event.RecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AutoAcceptMode;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.rendering.editorstate.SuggestionState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.RecommendableFeatureTrait;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

public abstract class SuggestionSupport_ImplBase
    implements SuggestionSupport, BeanNameAware
{
    protected final RecommendationService recommendationService;
    protected final LearningRecordService learningRecordService;
    protected final ApplicationEventPublisher applicationEventPublisher;
    protected final AnnotationSchemaService schemaService;

    private String id;

    public SuggestionSupport_ImplBase(RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher,
            AnnotationSchemaService aSchemaService)
    {
        recommendationService = aRecommendationService;
        learningRecordService = aLearningRecordService;
        applicationEventPublisher = aApplicationEventPublisher;
        schemaService = aSchemaService;
    }

    @Override
    public void setBeanName(String aName)
    {
        id = aName;
    }

    @Override
    public String getId()
    {
        return id;
    }

    protected final void commitLabel(SourceDocument aDocument, String aDataOwner,
            TypeAdapter aAdapter, AnnotationBaseFS aAnnotation, AnnotationFeature aFeature,
            Predictions aPredictions, AnnotationSuggestion aSuggestion)
        throws AnnotationException
    {
        // Update the feature value
        var address = ICasUtil.getAddr(aAnnotation);
        aAdapter.pushFeatureValue(aDocument, aDataOwner, aAnnotation.getCAS(), address, aFeature,
                aSuggestion.getLabel());

        // Update the suggestion information (if enabled)
        var retainSuggestionInfo = aAdapter
                .getFeatureTraits(aFeature, RecommendableFeatureTrait.class) //
                .map(RecommendableFeatureTrait::isRetainSuggestionInfo) //
                .orElse(false);

        if (retainSuggestionInfo) {
            var featureSupport = aAdapter.getFeatureSupport(aFeature).get();

            // Always push the given suggestion
            featureSupport.pushSuggestions(aDocument, aDataOwner, aAnnotation, aFeature,
                    asList(new SuggestionState(aSuggestion.getRecommenderName(),
                            aSuggestion.getScore(), aSuggestion.getLabel())));

            // If we have context predictions check if any other recommender has also suggested this
            // label and
            // if so, also push those.
            if (aPredictions != null) {
                var group = aPredictions
                        .getGroupedPredictions(AnnotationSuggestion.class, aDocument,
                                aFeature.getLayer(), aSuggestion.getWindowBegin(),
                                aSuggestion.getWindowEnd())
                        .stream() //
                        .filter(g -> g.contains(aSuggestion)) //
                        .findFirst();

                if (!group.isEmpty()) {
                    var suggestions = group.get().stream() //
                            .filter(s -> !s.equals(aSuggestion)) // Already added above
                            .filter(s -> Objects.equals(s.getLabel(), aSuggestion.getLabel()))
                            .map(s -> new SuggestionState(s.getRecommenderName(), s.getScore(),
                                    s.getLabel()))
                            .toList();
                    featureSupport.pushSuggestions(aDocument, aDataOwner, aAnnotation, aFeature,
                            suggestions);
                }
            }
        }
    }

    protected void recordAndPublishAcceptance(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, TypeAdapter aAdapter, AnnotationFeature aFeature,
            AnnotationSuggestion aSuggestion, AnnotationBaseFS annotation,
            LearningRecordChangeLocation aLocation, LearningRecordUserAction aAction)
    {
        // Log the action to the learning record
        if (!aAdapter.isSilenced()) {
            var record = toLearningRecord(aDocument, aDataOwner, aSuggestion, aFeature, aAction,
                    aLocation);
            learningRecordService.logRecord(aSessionOwner, record);

            // Send an application event that the suggestion has been accepted
            aAdapter.publishEvent(() -> new RecommendationAcceptedEvent(this, aDocument, aDataOwner,
                    annotation, aFeature, aSuggestion.getLabel()));
        }
    }

    protected void hideSuggestion(AnnotationSuggestion aSuggestion,
            LearningRecordUserAction aAction)
    {
        // Hide the suggestion. This is faster than having to recalculate the visibility status for
        // the entire document or even for the part visible on screen.
        aSuggestion
                .hide((aAction == ACCEPTED) ? FLAG_TRANSIENT_ACCEPTED : FLAG_TRANSIENT_CORRECTED);
    }

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

    @Override
    public void rejectSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, LearningRecordChangeLocation aLocation)
        throws AnnotationException
    {
        var suggestion = aSuggestion;

        // Hide the suggestion. This is faster than having to recalculate the visibility status
        // for the entire document or even for the part visible on screen.
        suggestion.hide(FLAG_TRANSIENT_REJECTED);

        var recommender = recommendationService.getRecommender(suggestion);
        var feature = recommender.getFeature();
        // Log the action to the learning record
        var record = toLearningRecord(aDocument, aDataOwner, aSuggestion, feature, REJECTED,
                aLocation);
        learningRecordService.logRecord(aSessionOwner, record);

        // Send an application event that the suggestion has been rejected
        applicationEventPublisher.publishEvent(
                new RecommendationRejectedEvent(this, aDocument, aDataOwner, feature, suggestion));
    }

    @Override
    public void skipSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, LearningRecordChangeLocation aLocation)
        throws AnnotationException
    {
        // Hide the suggestion. This is faster than having to recalculate the visibility status
        // for the entire document or even for the part visible on screen.
        aSuggestion.hide(FLAG_SKIPPED);

        var recommender = recommendationService.getRecommender(aSuggestion);
        var feature = recommender.getFeature();

        // Log the action to the learning record
        var record = toLearningRecord(aDocument, aDataOwner, aSuggestion, feature, SKIPPED,
                aLocation);
        learningRecordService.logRecord(aSessionOwner, record);
    }
}
