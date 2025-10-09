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

import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_SUPPORTED;
import static de.tudarmstadt.ukp.inception.rendering.model.Range.rangeCoveringAnnotations;
import static de.tudarmstadt.ukp.inception.scheduling.ProgressScope.SCOPE_UNITS;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

public class NamedEntityLinker
    extends RecommendationEngine
{
    private final NamedEntityLinkerTraits traits;
    private final KnowledgeBaseService kbService;
    private final ConceptLinkingService clService;
    private final FeatureSupportRegistry fsRegistry;
    private final ConceptFeatureTraits featureTraits;

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

        var alreadyLinkedConcepts = new HashSet<String>();
        for (var candidate : candidates) {
            var conceptId = candidate.getFeatureValueAsString(predictedFeature);
            if (conceptId != null) {
                alreadyLinkedConcepts.add(conceptId);
            }
        }

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

                predictSingle(candidate.getCoveredText(), candidate.getBegin(), candidate.getEnd(),
                        aCas, alreadyLinkedConcepts);

                prevCandidate = candidate;
            }
        }

        return rangeCoveringAnnotations(candidates);
    }

    private void predictSingle(String aCoveredText, int aBegin, int aEnd, CAS aCas,
            Set<String> aAlreadyLinkedConcepts)
    {
        var candidates = findCandidateConcepts(aCoveredText, aBegin, aCas);
        var predictedType = getPredictedType(aCas);
        var scoreFeature = getScoreFeature(aCas);
        var predictedFeature = getPredictedFeature(aCas);
        var isPredictionFeature = getIsPredictionFeature(aCas);

        var suggestionsCreated = 0;
        for (var candidate : candidates.stream().toList()) {
            if (aAlreadyLinkedConcepts.contains(candidate.getIdentifier())) {
                // If the concept has already been linked to another annotation, do not offer it
                // again
                continue;
            }

            var annotation = aCas.createAnnotation(predictedType, aBegin, aEnd);
            annotation.setStringValue(predictedFeature, candidate.getIdentifier());
            annotation.setBooleanValue(isPredictionFeature, true);
            annotation.setDoubleValue(scoreFeature, candidate.getScore());
            aCas.addFsToIndexes(annotation);
            suggestionsCreated++;
            if (suggestionsCreated >= recommender.getMaxRecommendations()) {
                break;
            }
        }
    }

    private List<KBHandle> findCandidateConcepts(String aCoveredText, int aBegin, CAS aCas)
    {
        var conceptFeatureTraits = fsRegistry.readTraits(recommender.getFeature(),
                ConceptFeatureTraits::new);

        var candidates = new ArrayList<KBHandle>();
        if (conceptFeatureTraits.getRepositoryId() != null) {
            var kb = kbService.getKnowledgeBaseById(recommender.getProject(),
                    conceptFeatureTraits.getRepositoryId());
            if (kb.isPresent() && kb.get().isEnabled() && kb.get().isSupportConceptLinking()) {
                candidates.addAll(readCandidates(kb.get(), aCoveredText, aBegin, aCas));
            }
        }
        else {
            for (var kb : kbService.getEnabledKnowledgeBases(recommender.getProject())) {
                if (kb.isSupportConceptLinking()) {
                    candidates.addAll(readCandidates(kb, aCoveredText, aBegin, aCas));
                }
            }
        }

        return candidates;
    }

    private List<KBHandle> readCandidates(KnowledgeBase kb, String aCoveredText, int aBegin,
            CAS aCas)
    {
        return kbService.read(kb, (conn) -> clService.disambiguate(kb, featureTraits.getScope(),
                featureTraits.getAllowedValueType(), null, aCoveredText, aBegin, aCas));
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
