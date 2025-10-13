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
package de.tudarmstadt.ukp.inception.conceptlinking.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.WITH_ROLE;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_SUPPORTED;
import static de.tudarmstadt.ukp.inception.rendering.model.Range.rangeCoveringAnnotations;
import static de.tudarmstadt.ukp.inception.scheduling.ProgressScope.SCOPE_UNITS;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;

public class NamedEntityLinker
    extends RecommendationEngine
{
    private final NamedEntityLinkerTraits traits;
    private final KnowledgeBaseService kbService;
    private final ConceptLinkingService clService;
    private final FeatureSupportRegistry fsRegistry;
    private final ConceptFeatureTraits featureTraits;
    private final AnnotationSchemaService schemaService;

    public static final Key<Collection<ImmutablePair<String, Collection<AnnotationFS>>>> KEY_MODEL = new Key<>(
            "model");

    public NamedEntityLinker(Recommender aRecommender, NamedEntityLinkerTraits aTraits,
            KnowledgeBaseService aKbService, ConceptLinkingService aClService,
            FeatureSupportRegistry aFsRegistry, ConceptFeatureTraits aFeatureTraits,
            AnnotationSchemaService aSchemaService)
    {
        super(aRecommender);

        traits = aTraits;
        kbService = aKbService;
        clService = aClService;
        fsRegistry = aFsRegistry;
        featureTraits = aFeatureTraits;
        schemaService = aSchemaService;
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        return true;
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasList)
    {
        // Training not required
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        var stackingAllowed = getRecommender().getLayer().isAllowStacking();
        var predictedType = getPredictedType(aCas);
        var predictedFeature = getPredictedFeature(aCas);

        var candidates = aCas.<Annotation> select(predictedType).coveredBy(aBegin, aEnd).asList();

        Annotation prevCandidate = null;
        try (var progress = aContext.getMonitor().openScope(SCOPE_UNITS, candidates.size())) {
            for (var candidate : candidates) {
                progress.update(up -> up.increment());

                if (prevCandidate != null && colocated(candidate, prevCandidate)) {
                    // If we did already do a KB lookup at this position, no need to do one again.
                    // Mind that UIMA provides annotations with the same offsets as a block in
                    // iteration order
                    continue;
                }

                if (candidate.getFeatureValueAsString(predictedFeature) != null
                        && (!stackingAllowed || traits.isEmptyCandidateFeatureRequired())) {
                    // If the candidate feature is already filled, we can skip prediction if
                    // stacking is not allowed or if an empty candidate feature is required
                    continue;
                }

                predictEntity(candidate);

                prevCandidate = candidate;
            }
        }

        return rangeCoveringAnnotations(candidates);
    }

    private void predictEntity(Annotation aEntity)
    {
        var cas = aEntity.getCAS();
        var predictedType = getPredictedType(cas);
        var scoreFeature = getScoreFeature(cas);
        var predictedFeature = getPredictedFeature(cas);
        var isPredictionFeature = getIsPredictionFeature(cas);
        var explanationFeature = getScoreExplanationFeature(cas);

        // If a concept has already been linked to an annotation at the same location as the target
        // candidate, do not propose that concept again.
        // We need to select by begin/end because otherwise the window entity will not be included
        // in the result
        var alreadyLinkedConcepts = new HashSet<String>();
        for (var collocatedEntities : cas.<Annotation> select(aEntity.getType())
                .at(aEntity.getBegin(), aEntity.getEnd())) {
            var conceptId = collocatedEntities.getFeatureValueAsString(predictedFeature);
            if (conceptId != null) {
                alreadyLinkedConcepts.add(conceptId);
            }
        }

        var candidateConcepts = findCandidateConcepts(aEntity);

        var candidatesConsidered = 0;
        for (var candidate : candidateConcepts) {
            candidatesConsidered++;

            if (alreadyLinkedConcepts.contains(candidate.getIdentifier())) {
                // If the concept has already been linked to another annotation, do not offer it
                // again
                continue;
            }

            var annotation = cas.createAnnotation(predictedType, aEntity.getBegin(),
                    aEntity.getEnd());
            annotation.setStringValue(predictedFeature, candidate.getIdentifier());
            annotation.setBooleanValue(isPredictionFeature, true);
            annotation.setDoubleValue(scoreFeature, candidate.getScore());
            annotation.setStringValue(explanationFeature, candidate.getDebugInfo());
            cas.addFsToIndexes(annotation);

            if (candidatesConsidered >= recommender.getMaxRecommendations()) {
                break;
            }
        }
    }

    private List<KBHandle> findCandidateConcepts(Annotation aEntity)
    {
        var conceptFeatureTraits = fsRegistry.readTraits(recommender.getFeature(),
                ConceptFeatureTraits::new);

        var candidates = new ArrayList<KBHandle>();
        if (conceptFeatureTraits.getRepositoryId() != null) {
            var kb = kbService.getKnowledgeBaseById(recommender.getProject(),
                    conceptFeatureTraits.getRepositoryId());
            if (kb.isPresent() && kb.get().isEnabled() && kb.get().isSupportConceptLinking()) {
                candidates.addAll(readCandidateConcepts(kb.get(), aEntity));
            }
        }
        else {
            for (var kb : kbService.getEnabledKnowledgeBases(recommender.getProject())) {
                if (kb.isSupportConceptLinking()) {
                    candidates.addAll(readCandidateConcepts(kb, aEntity));
                }
            }
        }

        return candidates;
    }

    private List<KBHandle> readCandidateConcepts(KnowledgeBase kb, Annotation aEntity)
    {
        var cas = aEntity.getCAS();
        var queryComponents = new ArrayList<Annotation>();
        queryComponents.add(aEntity);

        if (traits.isIncludeLinkTargetsInQuery()) {
            var adapter = schemaService.getAdapter(getRecommender().getLayer());
            if (adapter != null) {
                var linkFeatures = adapter.listFeatures().stream() //
                        .filter(f -> f.getLinkMode() == WITH_ROLE) //
                        .toList();
                for (var f : linkFeatures) {
                    List<LinkWithRoleModel> links = adapter.getFeatureValue(f, aEntity);
                    if (links == null) {
                        continue;
                    }

                    for (var link : links) {
                        var target = selectAnnotationByAddr(cas, link.targetAddr);
                        queryComponents.add(target);
                    }
                }
            }
        }

        var begin = aEntity.getBegin();
        var coveredText = aEntity.getCoveredText();
        var query = queryComponents.stream() //
                .sorted(comparing(Annotation::getBegin)) //
                .map(Annotation::getCoveredText) //
                .collect(joining(" "));

        return clService.disambiguate(kb, featureTraits.getScope(),
                featureTraits.getAllowedValueType(), query, coveredText, begin, cas);
    }

    @Override
    public TrainingCapability getTrainingCapability()
    {
        // We want the predict method to be called repeatedly, so we say training is supported even
        // though we do not react at all to the training process.
        return TRAINING_SUPPORTED;
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        var result = new EvaluationResult();
        result.setEvaluationSkipped(true);
        result.setErrorMsg("NamedEntityLinker does not support evaluation.");
        return result;
    }

    @Override
    public int estimateSampleCount(List<CAS> aCasses)
    {
        return -1;
    }
}
