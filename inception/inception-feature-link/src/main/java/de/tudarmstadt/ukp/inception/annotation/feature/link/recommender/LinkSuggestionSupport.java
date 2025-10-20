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
package de.tudarmstadt.ukp.inception.annotation.feature.link.recommender;

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static org.apache.uima.fit.util.CasUtil.select;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

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
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.recommender.ArcSuggestionSupport_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionRenderer;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportQuery;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LinkPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LinkSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Position;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.ExtractionContext;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;

public class LinkSuggestionSupport
    extends ArcSuggestionSupport_ImplBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "LINK";

    private final FeatureSupportRegistry featureSupportRegistry;

    public LinkSuggestionSupport(RecommendationService aRecommendationService,
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
        if (!SpanLayerSupport.TYPE.equals(aContext.layer().getType())) {
            return false;
        }

        var feature = aContext.feature();
        if (feature.getLinkMode() == LinkMode.WITH_ROLE) {
            return true;
        }

        return false;
    }

    @Override
    public Optional<AnnotationBaseFS> acceptSuggestion(String aSessionOwner,
            SourceDocument aDocument, String aDataOwner, CAS aCas, TypeAdapter aAdapter,
            AnnotationFeature aFeature, Predictions aPredictions, AnnotationSuggestion aSuggestion,
            LearningRecordChangeLocation aLocation, LearningRecordUserAction aAction)
        throws AnnotationException
    {
        if (aSuggestion instanceof LinkSuggestion suggestion
                && aAdapter instanceof SpanAdapter adapter) {
            return Optional.of(acceptSuggestion(aSessionOwner, aDocument, aDataOwner, aCas,
                    aFeature, aLocation, aAction, suggestion, adapter));
        }

        return Optional.empty();
    }

    private AnnotationFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, AnnotationFeature aFeature,
            LearningRecordChangeLocation aLocation, LearningRecordUserAction aAction,
            LinkSuggestion suggestion, SpanAdapter aAdapter)
        throws AnnotationException
    {
        var sourceBegin = suggestion.getPosition().getSourceBegin();
        var sourceEnd = suggestion.getPosition().getSourceEnd();
        var linkHostType = aAdapter.getAnnotationType(aCas)
                .orElseThrow(() -> new IllegalStateException(
                        "Type [" + aAdapter.getAnnotationTypeName() + "] not found in target CAS"));

        // Check if there is already a link host
        var linkHostCandidates = aCas.<Annotation> select(linkHostType).at(sourceBegin, sourceEnd)
                .limit(2).toList();
        Annotation linkHost = null;
        if (linkHostCandidates.size() > 1) {
            LOG.warn("Found multiple link host candidates, using first one...");
            linkHost = linkHostCandidates.get(0);
        }
        else if (!linkHostCandidates.isEmpty()) {
            linkHost = linkHostCandidates.get(0);
        }

        // Check if there are valid slot fillers
        var targetBegin = suggestion.getPosition().getTargetBegin();
        var targetEnd = suggestion.getPosition().getTargetEnd();
        var slotFillerType = CasUtil.getType(aCas, aFeature.getType());
        var slotFillerCandidates = aCas.<Annotation> select(slotFillerType)
                .at(targetBegin, targetEnd).limit(2).toList();
        Annotation slotFiller = null;
        if (slotFillerCandidates.size() > 1) {
            LOG.warn("Found multiple slot filler candidates, using first one...");
            slotFiller = slotFillerCandidates.get(0);
        }
        else if (!slotFillerCandidates.isEmpty()) {
            slotFiller = slotFillerCandidates.get(0);
        }

        try (var eventBatch = aAdapter.batchEvents()) {
            if (linkHost == null || slotFiller == null) {
                var msg = "Cannot find link host or slot filler to establish link between";
                LOG.error(msg);
                throw new IllegalStateException(msg);
            }

            var annotationCreated = false;

            List<LinkWithRoleModel> oldLinks = aAdapter.getFeatureValue(aFeature, linkHost);
            try {
                var newLinks = new ArrayList<>(oldLinks);
                newLinks.add(LinkWithRoleModel.builder() //
                        .withLabel(suggestion.getLabel()) //
                        .withRole(suggestion.getLabel()) //
                        .withTarget(slotFiller) //
                        .build());
                aAdapter.setFeatureValue(aDocument, aDataOwner, aCas, linkHost.getAddress(),
                        aFeature, newLinks);

                annotationCreated = true;
            }
            catch (Exception e) {
                if (annotationCreated) {
                    aAdapter.setFeatureValue(aDocument, aDataOwner, aCas, linkHost.getAddress(),
                            aFeature, oldLinks);
                }
                throw e;
            }

            hideSuggestion(suggestion, aAction);
            recordAndPublishAcceptance(aSessionOwner, aDocument, aDataOwner, aAdapter, aFeature,
                    suggestion, linkHost, aLocation, aAction);

            eventBatch.commit();
            return linkHost;
        }
    }

    @Override
    protected MultiValuedMap<Position, AnnotationFS> groupAnnotationsInWindow(CAS aCas,
            TypeAdapter aAdapter, int aWindowBegin, int aWindowEnd)
    {
        var adapter = (SpanAdapter) aAdapter;

        var type = adapter.getAnnotationType(aCas).orElseThrow(() -> new IllegalStateException(
                "Type [" + adapter.getAnnotationTypeName() + "] not found in target CAS"));

        var annotationsInWindow = getAnnotationsInWindow(aCas, type, aWindowBegin, aWindowEnd);

        var linkFeatures = adapter.listFeatures().stream() //
                .filter(f -> f.getLinkMode() == LinkMode.WITH_ROLE) //
                .toList();

        var groupedAnnotations = new ArrayListValuedHashMap<Position, AnnotationFS>();
        for (var source : annotationsInWindow) {
            for (var linkFeature : linkFeatures) {
                var links = (List<LinkWithRoleModel>) adapter.getFeatureValue(linkFeature, source);

                for (var link : links) {
                    var slotFiller = selectAnnotationByAddr(aCas, link.targetAddr);
                    var linkPosition = new LinkPosition(linkFeature.getName(), source.getBegin(),
                            source.getEnd(), slotFiller.getBegin(), slotFiller.getEnd());

                    groupedAnnotations.put(linkPosition, source);
                }
            }
        }

        return groupedAnnotations;
    }

    @Override
    public Optional<SuggestionRenderer> getRenderer()
    {
        return Optional.of(new LinkSuggestionRenderer(recommendationService, schemaService,
                featureSupportRegistry));
    }

    @Override
    public List<AnnotationSuggestion> extractSuggestions(ExtractionContext ctx)
    {
        var adapter = schemaService.getAdapter(ctx.getLayer());

        var adapterCache = new HashMap<String, TypeAdapter>();

        var result = new ArrayList<AnnotationSuggestion>();
        for (var predictedFS : ctx.getPredictionCas().select(ctx.getPredictedType())) {
            if (!predictedFS.getBooleanValue(ctx.getPredictionFeature())) {
                continue;
            }

            var feature = ctx.getRecommender().getFeature();
            List<LinkWithRoleModel> links = adapter.getFeatureValue(feature, predictedFS);
            if (links.isEmpty()) {
                continue;
            }

            var source = (AnnotationFS) predictedFS;
            var link = links.get(0);
            var target = selectAnnotationByAddr(ctx.getPredictionCas(), link.targetAddr);
            var targetAdapter = adapterCache.computeIfAbsent(target.getType().getName(),
                    $ -> schemaService.findAdapter(ctx.getRecommender().getProject(), target));

            var originalSource = findEquivalentSpan(adapter, ctx.getOriginalCas(), source);
            var originalTarget = findEquivalentSpan(targetAdapter, ctx.getOriginalCas(), target);
            if (originalSource.isEmpty() || originalTarget.isEmpty()) {
                LOG.debug("Unable to find owner or slot filler of predicted link in original CAS");
                continue;
            }

            var autoAcceptMode = getAutoAcceptMode(predictedFS, ctx.getModeFeature());
            var score = predictedFS.getDoubleValue(ctx.getScoreFeature());
            var scoreExplanation = predictedFS.getStringValue(ctx.getScoreExplanationFeature());
            var correction = predictedFS.getBooleanValue(ctx.getCorrectionFeature());
            var correctionExplanation = predictedFS
                    .getStringValue(ctx.getCorrectionExplanationFeature());
            var position = new LinkPosition(feature.getName(), originalSource.get(),
                    originalTarget.get());

            var suggestion = LinkSuggestion.builder() //
                    .withId(LinkSuggestion.NEW_ID) //
                    .withGeneration(ctx.getGeneration()) //
                    .withRecommender(ctx.getRecommender()) //
                    .withDocument(ctx.getDocument()) //
                    .withPosition(position) //
                    .withLabel(link.role) //
                    .withUiLabel(link.role) //
                    .withCorrection(correction) //
                    .withCorrectionExplanation(correctionExplanation) //
                    .withScore(score) //
                    .withScoreExplanation(scoreExplanation) //
                    .withAutoAcceptMode(autoAcceptMode) //
                    .build();
            result.add(suggestion);
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
