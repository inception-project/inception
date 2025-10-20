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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.recommender;

import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionRenderer;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportQuery;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Position;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.ExtractionContext;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

public class RelationSuggestionSupport
    extends ArcSuggestionSupport_ImplBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "RELATION";

    private final FeatureSupportRegistry featureSupportRegistry;

    public RelationSuggestionSupport(RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher,
            AnnotationSchemaService aSchemaService, FeatureSupportRegistry aFeatureSupportRegistry)
    {
        super(aRecommendationService, aLearningRecordService, aApplicationEventPublisher,
                aSchemaService);

        featureSupportRegistry = aFeatureSupportRegistry;
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public boolean accepts(SuggestionSupportQuery aContext)
    {
        if (!RelationLayerSupport.TYPE.equals(aContext.layer().getType())) {
            return false;
        }

        var feature = aContext.feature();
        if (CAS.TYPE_NAME_STRING.equals(feature.getType())
                || CAS.TYPE_NAME_STRING_ARRAY.equals(feature.getType())
                || feature.isVirtualFeature()) {
            return true;
        }

        return false;
    }

    /**
     * Uses the given annotation suggestion to create a new annotation or to update a feature in an
     * existing annotation.
     * 
     * @param aDocument
     *            the source document to which the annotations belong
     * @param aDataOwner
     *            the annotator user to whom the annotations belong
     * @param aCas
     *            the CAS containing the annotations
     * @param aAdapter
     *            an adapter for the layer to upsert
     * @param aFeature
     *            the feature on the layer that should be upserted
     * @param aSuggestion
     *            the suggestion
     * @param aLocation
     *            the location from where the change was triggered
     * @param aAction
     *            whether the annotation was accepted or corrected
     * @return the created/updated annotation.
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    @Override
    public Optional<AnnotationBaseFS> acceptSuggestion(String aSessionOwner,
            SourceDocument aDocument, String aDataOwner, CAS aCas, TypeAdapter aAdapter,
            AnnotationFeature aFeature, Predictions aPredictions, AnnotationSuggestion aSuggestion,
            LearningRecordChangeLocation aLocation, LearningRecordUserAction aAction)
        throws AnnotationException
    {
        if (aSuggestion instanceof RelationSuggestion suggestion
                && aAdapter instanceof RelationAdapter adapter) {
            return Optional.of(acceptSuggestion(aSessionOwner, aDocument, aDataOwner, aCas,
                    aFeature, aPredictions, aLocation, aAction, suggestion, adapter));
        }

        return Optional.empty();
    }

    private AnnotationFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, AnnotationFeature aFeature, Predictions aPredictions,
            LearningRecordChangeLocation aLocation, LearningRecordUserAction aAction,
            RelationSuggestion suggestion, RelationAdapter aAdapter)
        throws AnnotationException
    {
        var sourceBegin = suggestion.getPosition().getSourceBegin();
        var sourceEnd = suggestion.getPosition().getSourceEnd();
        var targetBegin = suggestion.getPosition().getTargetBegin();
        var targetEnd = suggestion.getPosition().getTargetEnd();

        // Check if there is already a relation for the given source and target
        var type = aAdapter.getAnnotationType(aCas).orElseThrow(() -> new IllegalStateException(
                "Type [" + aAdapter.getAnnotationTypeName() + "] not found in target CAS"));
        var attachType = CasUtil.getType(aCas, aAdapter.getAttachTypeName());

        var sourceFeature = type.getFeatureByBaseName(aAdapter.getSourceFeatureName());
        var targetFeature = type.getFeatureByBaseName(aAdapter.getTargetFeatureName());

        // The begin and end feature of a relation in the CAS are of the dependent/target
        // annotation. See also RelationAdapter::createRelationAnnotation.
        // We use that fact to search for existing relations for this relation suggestion
        var candidates = new ArrayList<AnnotationFS>();
        for (var relationCandidate : selectAt(aCas, type, targetBegin, targetEnd)) {
            var source = (AnnotationFS) relationCandidate.getFeatureValue(sourceFeature);
            var target = (AnnotationFS) relationCandidate.getFeatureValue(targetFeature);

            if (source == null || target == null) {
                continue;
            }

            if (colocated(source, sourceBegin, sourceEnd)
                    && colocated(target, targetBegin, targetEnd)) {
                candidates.add(relationCandidate);
            }
        }

        try (var eventBatch = aAdapter.batchEvents()) {
            var annotationCreated = false;
            AnnotationFS annotation = null;
            if (candidates.size() == 1) {
                // One candidate, we just return it
                annotation = candidates.get(0);
            }
            else if (candidates.size() > 1) {
                LOG.warn(
                        "Found multiple candidates for upserting relation from suggestion, using first one");
                annotation = candidates.get(0);
            }

            // We did not find a relation for this suggestion, so we create a new one
            if (annotation == null) {
                // FIXME: We get the first match for the (begin, end) span. With stacking, there can
                // be more than one and we need to get the right one then which does not need to be
                // the first. We wait for #2135 to fix this. When stacking is enabled, then also
                // consider creating a new relation instead of upserting an existing one.

                var source = selectAt(aCas, attachType, sourceBegin, sourceEnd).stream().findFirst()
                        .orElse(null);
                var target = selectAt(aCas, attachType, targetBegin, targetEnd).stream().findFirst()
                        .orElse(null);

                if (source == null || target == null) {
                    String msg = "Cannot find source or target annotation for upserting relation";
                    LOG.error(msg);
                    throw new IllegalStateException(msg);
                }

                annotation = aAdapter.add(aDocument, aDataOwner, source, target, aCas);
                annotationCreated = true;
            }

            try {
                commitLabel(aDocument, aDataOwner, aAdapter, annotation, aFeature, aPredictions,
                        suggestion);
            }
            catch (Exception e) {
                if (annotationCreated) {
                    aAdapter.delete(aDocument, aDataOwner, aCas, VID.of(annotation));
                }
                throw e;
            }

            hideSuggestion(suggestion, aAction);
            recordAndPublishAcceptance(aSessionOwner, aDocument, aDataOwner, aAdapter, aFeature,
                    suggestion, annotation, aLocation, aAction);

            eventBatch.commit();
            return annotation;
        }
    }

    @Override
    protected MultiValuedMap<Position, AnnotationFS> groupAnnotationsInWindow(CAS aCas,
            TypeAdapter aAdapter, int aWindowBegin, int aWindowEnd)
    {
        var adapter = (RelationAdapter) aAdapter;

        var type = adapter.getAnnotationType(aCas).orElseThrow(() -> new IllegalStateException(
                "Type [" + adapter.getAnnotationTypeName() + "] not found in target CAS"));
        var governorFeature = adapter.getSourceFeature(aCas);
        var dependentFeature = adapter.getTargetFeature(aCas);

        if (dependentFeature == null || governorFeature == null) {
            LOG.warn("Missing Dependent or Governor feature on [{}]", type.getName());
            return MultiMapUtils.emptyMultiValuedMap();
        }

        var annotationsInWindow = getAnnotationsInWindow(aCas, type, aWindowBegin, aWindowEnd);
        var groupedAnnotations = new ArrayListValuedHashMap<Position, AnnotationFS>();
        for (var annotationFS : annotationsInWindow) {
            var source = (AnnotationFS) annotationFS.getFeatureValue(governorFeature);
            var target = (AnnotationFS) annotationFS.getFeatureValue(dependentFeature);

            var relationPosition = new RelationPosition(source.getBegin(), source.getEnd(),
                    target.getBegin(), target.getEnd());

            groupedAnnotations.put(relationPosition, annotationFS);
        }

        return groupedAnnotations;
    }

    @Override
    public Optional<SuggestionRenderer> getRenderer()
    {
        return Optional.of(new RelationSuggestionRenderer(recommendationService, schemaService,
                featureSupportRegistry));
    }

    @Override
    public List<AnnotationSuggestion> extractSuggestions(ExtractionContext ctx)
    {
        // TODO Use adapter instead - once the method is no longer static
        var sourceFeature = ctx.getPredictedType().getFeatureByBaseName(FEAT_REL_SOURCE);
        var targetFeature = ctx.getPredictedType().getFeatureByBaseName(FEAT_REL_TARGET);

        var adapterCache = new HashMap<String, TypeAdapter>();

        var result = new ArrayList<AnnotationSuggestion>();
        for (var predictedFS : ctx.getPredictionCas().select(ctx.getPredictedType())) {
            if (!predictedFS.getBooleanValue(ctx.getPredictionFeature())) {
                continue;
            }

            var source = (AnnotationFS) predictedFS.getFeatureValue(sourceFeature);
            var target = (AnnotationFS) predictedFS.getFeatureValue(targetFeature);

            if (source == null || target == null) {
                LOG.debug("Source or target is null, skipping");
                continue;
            }

            var sourceAdapter = adapterCache.computeIfAbsent(source.getType().getName(),
                    $ -> schemaService.findAdapter(ctx.getRecommender().getProject(), source));
            var targetAdapter = adapterCache.computeIfAbsent(target.getType().getName(),
                    $ -> schemaService.findAdapter(ctx.getRecommender().getProject(), target));

            var originalSource = findEquivalentSpan(sourceAdapter, ctx.getOriginalCas(), source);
            var originalTarget = findEquivalentSpan(targetAdapter, ctx.getOriginalCas(), target);
            if (originalSource.isEmpty() || originalTarget.isEmpty()) {
                LOG.debug("Unable to find source or target of predicted relation in original CAS");
                continue;
            }

            var autoAcceptMode = getAutoAcceptMode(predictedFS, ctx.getModeFeature());
            var labels = getPredictedLabels(predictedFS, ctx.getLabelFeature(),
                    ctx.isMultiLabels());
            var score = predictedFS.getDoubleValue(ctx.getScoreFeature());
            var scoreExplanation = predictedFS.getStringValue(ctx.getScoreExplanationFeature());
            var correction = predictedFS.getBooleanValue(ctx.getCorrectionFeature());
            var correctionExplanation = predictedFS
                    .getStringValue(ctx.getCorrectionExplanationFeature());
            var position = new RelationPosition(originalSource.get(), originalTarget.get());

            for (var label : labels) {
                var suggestion = RelationSuggestion.builder() //
                        .withId(RelationSuggestion.NEW_ID) //
                        .withGeneration(ctx.getGeneration()) //
                        .withRecommender(ctx.getRecommender()) //
                        .withDocument(ctx.getDocument()) //
                        .withPosition(position) //
                        .withLabel(label) //
                        .withUiLabel(label) //
                        .withCorrection(correction) //
                        .withCorrectionExplanation(correctionExplanation) //
                        .withScore(score) //
                        .withScoreExplanation(scoreExplanation) //
                        .withAutoAcceptMode(autoAcceptMode) //
                        .build();
                result.add(suggestion);
            }
        }
        return result;
    }

    /**
     * Locates an annotation in the given CAS which is equivalent of the provided annotation.
     *
     * @param aOriginalCas
     *            the original CAS.
     * @param aSpan
     *            an annotation in the prediction CAS. return the equivalent in the original CAS.
     */
    private static Optional<Annotation> findEquivalentSpan(TypeAdapter aAdapter, CAS aOriginalCas,
            AnnotationFS aSpan)
    {
        return aOriginalCas.<Annotation> select(aSpan.getType()) //
                .at(aSpan) //
                .filter(canditeSpan -> aAdapter.isSamePosition(canditeSpan, aSpan)) //
                .findFirst();
    }

    private List<AnnotationFS> getAnnotationsInWindow(CAS aCas, Type type, int aWindowBegin,
            int aWindowEnd)
    {
        return select(aCas, type).stream() //
                .filter(fs -> fs.coveredBy(aWindowBegin, aWindowEnd)) //
                .toList();
    }
}
