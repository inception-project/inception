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

import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.AL_SIDEBAR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.CORRECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.SKIPPED;

import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapter;
import de.tudarmstadt.ukp.inception.recommendation.api.LayerRecommendationSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.event.RecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

public class SpanRecommendationSupportImpl
    implements LayerRecommendationSupport<SpanAdapter, SpanSuggestion>

{
    private final RecommendationService recommendationService;
    private final LearningRecordService learningRecordService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public SpanRecommendationSupportImpl(RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        recommendationService = aRecommendationService;
        learningRecordService = aLearningRecordService;
        applicationEventPublisher = aApplicationEventPublisher;
    }

    public AnnotationFS correctSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, SpanAdapter aAdapter, AnnotationFeature aFeature,
            SpanSuggestion aOriginalSuggestion, SpanSuggestion aCorrectedSuggestion,
            LearningRecordChangeLocation aLocation)
        throws AnnotationException
    {
        // If the action was a correction (i.e. suggestion label != annotation value) then generate
        // a rejection for the original value - we do not want the original value to re-appear
        learningRecordService.logRecord(aSessionOwner, aDocument, aDataOwner, aOriginalSuggestion,
                aFeature, REJECTED, AL_SIDEBAR);

        return acceptOrCorrectSuggestion(aSessionOwner, aDocument, aDataOwner, aCas, aAdapter,
                aFeature, aCorrectedSuggestion, aLocation, CORRECTED);
    }

    @Override
    public AnnotationBaseFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, SpanAdapter aAdapter, AnnotationFeature aFeature,
            SpanSuggestion aSuggestion, LearningRecordChangeLocation aLocation,
            LearningRecordUserAction aAction)
        throws AnnotationException
    {
        return acceptOrCorrectSuggestion(aSessionOwner, aDocument, aDataOwner, aCas, aAdapter,
                aFeature, aSuggestion, aLocation, aAction);
    }

    public AnnotationFS acceptOrCorrectSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, SpanAdapter aAdapter, AnnotationFeature aFeature,
            SpanSuggestion aSuggestion, LearningRecordChangeLocation aLocation,
            LearningRecordUserAction aAction)
        throws AnnotationException
    {
        var aBegin = aSuggestion.getBegin();
        var aEnd = aSuggestion.getEnd();
        var aValue = aSuggestion.getLabel();

        var candidates = aCas.<Annotation> select(aAdapter.getAnnotationTypeName()) //
                .at(aBegin, aEnd) //
                .asList();

        var candidateWithEmptyLabel = candidates.stream() //
                .filter(c -> aAdapter.getFeatureValue(aFeature, c) == null) //
                .findFirst();

        AnnotationFS annotation;
        if (candidateWithEmptyLabel.isPresent()) {
            // If there is an annotation where the predicted feature is unset, use it ...
            annotation = candidateWithEmptyLabel.get();
        }
        else if (candidates.isEmpty() || aAdapter.getLayer().isAllowStacking()) {
            // ... if not or if stacking is allowed, then we create a new annotation - this also
            // takes care of attaching to an annotation if necessary
            var newAnnotation = aAdapter.add(aDocument, aDataOwner, aCas, aBegin, aEnd);
            annotation = newAnnotation;
        }
        else {
            // ... if yes and stacking is not allowed, then we update the feature on the existing
            // annotation
            annotation = candidates.get(0);
        }

        recommendationService.commmitAcceptedLabel(aSessionOwner, aDocument, aDataOwner, aCas,
                aAdapter, aFeature, aSuggestion, aValue, annotation, aLocation, aAction);

        return annotation;
    }

    @Override
    public void rejectSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            SpanSuggestion spanSuggestion, LearningRecordChangeLocation aAction)
    {

        // Hide the suggestion. This is faster than having to recalculate the visibility status
        // for the entire document or even for the part visible on screen.
        spanSuggestion.hide(FLAG_TRANSIENT_REJECTED);

        var recommender = recommendationService.getRecommender(spanSuggestion.getVID().getId());
        var feature = recommender.getFeature();
        // Log the action to the learning record
        learningRecordService.logRecord(aSessionOwner, aDocument, aDataOwner, spanSuggestion,
                feature, REJECTED, aAction);

        // Send an application event that the suggestion has been rejected
        applicationEventPublisher.publishEvent(new RecommendationRejectedEvent(this, aDocument,
                aDataOwner, spanSuggestion.getBegin(), spanSuggestion.getEnd(),
                spanSuggestion.getCoveredText(), feature, spanSuggestion.getLabel()));

    }

    @Override
    public void skipSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            SpanSuggestion aSuggestion, LearningRecordChangeLocation aAction)
        throws AnnotationException
    {
        // Hide the suggestion. This is faster than having to recalculate the visibility status
        // for the entire document or even for the part visible on screen.
        aSuggestion.hide(FLAG_SKIPPED);

        var recommender = recommendationService.getRecommender(aSuggestion.getVID().getId());
        var feature = recommender.getFeature();

        // Log the action to the learning record
        learningRecordService.logRecord(aSessionOwner, aDocument, aDataOwner, aSuggestion, feature,
                SKIPPED, aAction);

        // // Send an application event that the suggestion has been rejected
        // applicationEventPublisher.publishEvent(new RecommendationSkippedEvent(this,
        // aDocument,
        // aDataOwner, spanSuggestion.getBegin(), spanSuggestion.getEnd(),
        // spanSuggestion.getCoveredText(), feature, spanSuggestion.getLabel()));
    }
}
