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
package de.tudarmstadt.ukp.inception.recommendation.relation;

import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_OVERLAP;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_REJECTED;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionRenderer;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupport_ImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Position;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.service.ExtractionContext;
import de.tudarmstadt.ukp.inception.recommendation.service.SuggestionExtraction;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationComparisonUtils;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

public class RelationSuggestionSupport
    extends SuggestionSupport_ImplBase<RelationSuggestion>
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
    public boolean accepts(AnnotationSuggestion aContext)
    {
        return aContext instanceof RelationSuggestion;
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
    public AnnotationFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, TypeAdapter aAdapter, AnnotationFeature aFeature,
            AnnotationSuggestion aSuggestion, LearningRecordChangeLocation aLocation,
            LearningRecordUserAction aAction)
        throws AnnotationException
    {
        var suggestion = (RelationSuggestion) aSuggestion;
        var adapter = (RelationAdapter) aAdapter;

        var sourceBegin = suggestion.getPosition().getSourceBegin();
        var sourceEnd = suggestion.getPosition().getSourceEnd();
        var targetBegin = suggestion.getPosition().getTargetBegin();
        var targetEnd = suggestion.getPosition().getTargetEnd();

        // Check if there is already a relation for the given source and target
        var type = adapter.getAnnotationType(aCas);
        var attachType = CasUtil.getType(aCas, adapter.getAttachTypeName());

        var sourceFeature = type.getFeatureByBaseName(adapter.getSourceFeatureName());
        var targetFeature = type.getFeatureByBaseName(adapter.getTargetFeatureName());

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

            annotation = adapter.add(aDocument, aDataOwner, source, target, aCas);
        }

        commmitLabel(aSessionOwner, aDocument, aDataOwner, aCas, aAdapter, aFeature, aSuggestion,
                aSuggestion.getLabel(), annotation, aLocation, aAction);

        return annotation;
    }

    @Override
    public void rejectSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, LearningRecordChangeLocation aAction)
    {
        // Hide the suggestion. This is faster than having to recalculate the visibility status
        // for
        // the entire document or even for the part visible on screen.
        aSuggestion.hide(FLAG_TRANSIENT_REJECTED);

        // TODO: See span recommendation support...

    }

    @Override
    public void skipSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, LearningRecordChangeLocation aAction)
        throws AnnotationException
    {
        // Hide the suggestion. This is faster than having to recalculate the visibility status
        // for
        // the entire document or even for the part visible on screen.
        aSuggestion.hide(FLAG_SKIPPED);

        // TODO: Log rejection
        // TODO: Publish rejection event
    }

    @Override
    public <T extends AnnotationSuggestion> void calculateSuggestionVisibility(String aSessionOwner,
            SourceDocument aDocument, CAS aCas, String aUser, AnnotationLayer aLayer,
            Collection<SuggestionGroup<T>> aRecommendations, int aWindowBegin, int aWindowEnd)
    {
        var type = getAnnotationType(aCas, aLayer);

        if (type == null) {
            // The type does not exist in the type system of the CAS. Probably it has not
            // been upgraded to the latest version of the type system yet. If this is the case,
            // we'll just skip.
            return;
        }

        var governorFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        var dependentFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        if (dependentFeature == null || governorFeature == null) {
            LOG.warn("Missing Dependent or Governor feature on [{}]", aLayer.getName());
            return;
        }

        var annotationsInWindow = getAnnotationsInWindow(aCas, type, aWindowBegin, aWindowEnd);

        // Group annotations by relation position, that is (source, target) address
        var groupedAnnotations = new ArrayListValuedHashMap<Position, AnnotationFS>();
        for (var annotationFS : annotationsInWindow) {
            var source = (AnnotationFS) annotationFS.getFeatureValue(governorFeature);
            var target = (AnnotationFS) annotationFS.getFeatureValue(dependentFeature);

            var relationPosition = new RelationPosition(source.getBegin(), source.getEnd(),
                    target.getBegin(), target.getEnd());

            groupedAnnotations.put(relationPosition, annotationFS);
        }

        // Collect all suggestions of the given layer
        var groupedSuggestions = aRecommendations.stream()
                .filter(group -> group.getLayerId() == aLayer.getId()) //
                .map(group -> (SuggestionGroup) group) //
                .collect(toList());

        // Get previously rejected suggestions
        var groupedRecordedAnnotations = new ArrayListValuedHashMap<Position, LearningRecord>();
        for (var learningRecord : learningRecordService.listLearningRecords(aSessionOwner, aUser,
                aLayer)) {
            var relationPosition = new RelationPosition(learningRecord.getOffsetSourceBegin(),
                    learningRecord.getOffsetSourceEnd(), learningRecord.getOffsetTargetBegin(),
                    learningRecord.getOffsetTargetEnd());

            groupedRecordedAnnotations.put(relationPosition, learningRecord);
        }

        for (var feature : schemaService.listSupportedFeatures(aLayer)) {
            var feat = type.getFeatureByBaseName(feature.getName());

            if (feat == null) {
                // The feature does not exist in the type system of the CAS. Probably it has not
                // been upgraded to the latest version of the type system yet. If this is the case,
                // we'll just skip.
                return;
            }

            for (SuggestionGroup<RelationSuggestion> group : groupedSuggestions) {
                if (!feature.getName().equals(group.getFeature())) {
                    continue;
                }

                group.showAll(AnnotationSuggestion.FLAG_ALL);

                var position = group.getPosition();

                // If any annotation at this position has a non-null label for this feature,
                // then we hide the suggestion group
                for (var annotationFS : groupedAnnotations.get(position)) {
                    if (annotationFS.getFeatureValueAsString(feat) != null) {
                        for (RelationSuggestion suggestion : group) {
                            suggestion.hide(FLAG_OVERLAP);
                        }
                    }
                }

                // Hide previously rejected suggestions
                for (var learningRecord : groupedRecordedAnnotations.get(position)) {
                    for (var suggestion : group) {
                        if (suggestion.labelEquals(learningRecord.getAnnotation())) {
                            suggestion.hideSuggestion(learningRecord.getUserAction());
                        }
                    }
                }
            }
        }
    }

    @Nullable
    private Type getAnnotationType(CAS aCas, AnnotationLayer aLayer)
    {
        // NOTE: In order to avoid having to upgrade the "original CAS" in computePredictions,this
        // method is implemented in such a way that it gracefully handles cases where the CAS and
        // the project type system are not in sync - specifically the CAS where the project defines
        // layers or features which do not exist in the CAS.

        try {
            return CasUtil.getAnnotationType(aCas, aLayer.getName());
        }
        catch (IllegalArgumentException e) {
            // Type does not exist in the type system of the CAS. Probably it has not been upgraded
            // to the latest version of the type system yet. If this is the case, we'll just skip.
            return null;
        }
    }

    private List<AnnotationFS> getAnnotationsInWindow(CAS aCas, Type type, int aWindowBegin,
            int aWindowEnd)
    {
        if (type == null) {
            return Collections.emptyList();
        }

        return select(aCas, type).stream() //
                .filter(fs -> fs.coveredBy(aWindowBegin, aWindowEnd)) //
                .toList();
    }

    @Override
    public LearningRecord toLearningRecord(SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, AnnotationFeature aFeature,
            LearningRecordUserAction aUserAction, LearningRecordChangeLocation aLocation)
    {
        var pos = ((RelationSuggestion) aSuggestion).getPosition();
        var record = new LearningRecord();
        record.setUser(aDataOwner);
        record.setSourceDocument(aDocument);
        record.setUserAction(aUserAction);
        record.setOffsetBegin(pos.getSourceBegin());
        record.setOffsetEnd(pos.getSourceEnd());
        record.setOffsetBegin2(pos.getTargetBegin());
        record.setOffsetEnd2(pos.getTargetEnd());
        record.setTokenText("");
        record.setAnnotation(aSuggestion.getLabel());
        record.setLayer(aFeature.getLayer());
        record.setSuggestionType(TYPE);
        record.setChangeLocation(aLocation);
        record.setAnnotationFeature(aFeature);
        return record;
    }

    @Override
    public Optional<SuggestionRenderer> getRenderer()
    {
        return Optional.of(new RelationSuggestionRenderer(recommendationService, schemaService,
                featureSupportRegistry));
    }

    public static void extractSuggestion(ExtractionContext ctx, TOP predictedFS)
    {
        var autoAcceptMode = SuggestionExtraction.getAutoAcceptMode(predictedFS,
                ctx.getModeFeature());
        var labels = SuggestionExtraction.getPredictedLabels(predictedFS, ctx.getLabelFeature(),
                ctx.isMultiLabels());
        var score = predictedFS.getDoubleValue(ctx.getScoreFeature());
        var scoreExplanation = predictedFS.getStringValue(ctx.getScoreExplanationFeature());

        var source = (AnnotationFS) predictedFS.getFeatureValue(ctx.getSourceFeature());
        var target = (AnnotationFS) predictedFS.getFeatureValue(ctx.getTargetFeature());

        var originalSource = findEquivalentSpan(ctx.getOriginalCas(), source);
        var originalTarget = findEquivalentSpan(ctx.getOriginalCas(), target);
        if (originalSource.isEmpty() || originalTarget.isEmpty()) {
            LOG.debug("Unable to find source or target of predicted relation in original CAS");
            return;
        }

        var position = new RelationPosition(originalSource.get(), originalTarget.get());

        for (var label : labels) {
            var suggestion = RelationSuggestion.builder() //
                    .withId(RelationSuggestion.NEW_ID) //
                    .withGeneration(ctx.getGeneration()) //
                    .withRecommender(ctx.getRecommender()) //
                    .withDocumentName(ctx.getDocument().getName()) //
                    .withPosition(position) //
                    .withLabel(label) //
                    .withUiLabel(label) //
                    .withScore(score) //
                    .withScoreExplanation(scoreExplanation) //
                    .withAutoAcceptMode(autoAcceptMode) //
                    .build();
            ctx.getResult().add(suggestion);
        }
    }

    /**
     * Locates an annotation in the given CAS which is equivalent of the provided annotation.
     *
     * @param aOriginalCas
     *            the original CAS.
     * @param aAnnotation
     *            an annotation in the prediction CAS. return the equivalent in the original CAS.
     */
    private static Optional<Annotation> findEquivalentSpan(CAS aOriginalCas,
            AnnotationFS aAnnotation)
    {
        return aOriginalCas.<Annotation> select(aAnnotation.getType()) //
                .at(aAnnotation) //
                .filter(candidate -> AnnotationComparisonUtils.isEquivalentSpanAnnotation(candidate,
                        aAnnotation, null))
                .findFirst();
    }
}
