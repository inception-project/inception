/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.inception.conceptlinking.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getDocumentUri;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentences;
import static org.apache.uima.fit.util.CasUtil.getAnnotationType;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.indexCovered;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;

public class NamedEntityLinker
    implements RecommendationEngine
{
    private Recommender recommender;
    private NamedEntityLinkerTraits traits;
    
    private KnowledgeBaseService kbService;
    private ConceptLinkingService clService;
    private FeatureSupportRegistry fsRegistry;
    private ConceptFeatureTraits featureTraits;

    public static final Key<Collection<ImmutablePair<String, Collection<AnnotationFS>>>> KEY_MODEL
        = new Key<>("model");

    public NamedEntityLinker(Recommender aRecommender, NamedEntityLinkerTraits aTraits,
            KnowledgeBaseService aKbService, ConceptLinkingService aClService,
            FeatureSupportRegistry aFsRegistry, ConceptFeatureTraits aFeatureTraits)
    {
        recommender = aRecommender;
        traits = aTraits;
        kbService = aKbService;
        clService = aClService;
        fsRegistry = aFsRegistry;
        featureTraits = aFeatureTraits;
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasList)
    {
        Collection<ImmutablePair<String, Collection<AnnotationFS>>> nameSamples =
            extractNamedEntities(aCasList);
        aContext.put(KEY_MODEL, nameSamples);
        aContext.markAsReadyForPrediction();
    }

    private Collection<ImmutablePair<String, Collection<AnnotationFS>>> extractNamedEntities(
        List<CAS> aCasList)
    {
        Type tokenType = org.apache.uima.fit.util.CasUtil
            .getType(aCasList.get(0), recommender.getLayer().getName());
        Feature feature = tokenType.getFeatureByBaseName(recommender.getFeature().getName());

        Collection<ImmutablePair<String, Collection<AnnotationFS>>> nameSamples = new HashSet<>();
        for (CAS cas : aCasList) {
            Collection<AnnotationFS> namesPerDocument = new ArrayList<>();
            Type sentenceType = getType(cas, Sentence.class);

            Map<AnnotationFS, List<AnnotationFS>> sentences = indexCovered(cas, sentenceType,
                    tokenType);
            for (Map.Entry<AnnotationFS, List<AnnotationFS>> e : sentences.entrySet()) {
                Collection<AnnotationFS> tokens = e.getValue().stream()
                    // If the identifier has not been set
                    .filter(a -> a.getStringValue(feature) == null)
                    .collect(Collectors.toSet());
                namesPerDocument.addAll(tokens);
            }

            // TODO #176 use the document Id once it is available in the CAS
            nameSamples.add(
                new ImmutablePair<>(getDocumentUri(cas), namesPerDocument));
        }
        return nameSamples;
    }

    // TODO #176 use the document Id once it is available in the CAS
    private boolean isNamedEntity(RecommenderContext aContext, AnnotationFS token,
            String aDocumentUri)
        throws RecommendationException
    {
        Collection<ImmutablePair<String, Collection<AnnotationFS>>> model = aContext.get(KEY_MODEL)
                .orElseThrow(() -> new RecommendationException(
                        "Key [" + KEY_MODEL + "] not found in context"));
        
        return model.stream().anyMatch(pair -> pair.getLeft().equals(aDocumentUri)
        && pair.getRight().stream().anyMatch(t -> t.getBegin() == token.getBegin()));
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        Type tokenType = getType(aCas, Token.class);

        for (AnnotationFS sentence : selectSentences(aCas)) {
            List<AnnotationFS> tokenAnnotations = selectCovered(tokenType, sentence);
            predictSentence(aContext, tokenAnnotations, aCas);
        }
    }

    private void predictSentence(RecommenderContext aContext, List<AnnotationFS> aTokenAnnotations,
            CAS aCas)
        throws RecommendationException
    {
        int sentenceIndex = 0;
        while (sentenceIndex < aTokenAnnotations.size() - 1) {
            String documentUri = getDocumentUri(aCas);
            AnnotationFS token = aTokenAnnotations.get(sentenceIndex);

            if (isNamedEntity(aContext, token, documentUri)) {
                StringBuilder coveredText = new StringBuilder(token.getCoveredText());
                int begin = token.getBegin();
                int end = token.getEnd();

                AnnotationFS nextTokenObject = aTokenAnnotations.get(sentenceIndex + 1);
                // Checking whether the next TokenObject is a NE
                // and whether the sentenceIndex for the next TokenObject is still
                // in the range of the sentence
                while (isNamedEntity(aContext, nextTokenObject, documentUri)
                    && sentenceIndex + 1 < aTokenAnnotations.size() - 1) {
                    coveredText.append(" ").append(nextTokenObject.getCoveredText());
                    end = nextTokenObject.getEnd();
                    sentenceIndex++;
                    nextTokenObject = aTokenAnnotations.get(sentenceIndex + 1);
                }
                predictToken(coveredText.toString(), begin, end, aCas);

            }
            sentenceIndex++;
        }
    }

    private void predictToken(String aCoveredText, int aBegin, int aEnd, CAS aCas)
    {
        List<KBHandle> handles = new ArrayList<>();

        AnnotationFeature feat = recommender.getFeature();
        FeatureSupport<ConceptFeatureTraits> fs = fsRegistry.getFeatureSupport(feat);
        ConceptFeatureTraits conceptFeatureTraits = fs.readTraits(feat);

        if (conceptFeatureTraits.getRepositoryId() != null) {
            Optional<KnowledgeBase> kb = kbService.getKnowledgeBaseById(recommender.getProject(),
                    conceptFeatureTraits.getRepositoryId());
            if (kb.isPresent() && kb.get().isSupportConceptLinking()) {
                handles.addAll(readCandidates(kb.get(), aCoveredText, aBegin, aCas));
            }
        } else {
            for (KnowledgeBase kb : kbService.getEnabledKnowledgeBases(recommender.getProject())) {
                if (kb.isSupportConceptLinking()) {
                    handles.addAll(readCandidates(kb, aCoveredText, aBegin, aCas));
                }
            }
        }

        Type predictionType = getAnnotationType(aCas, PredictedSpan.class);

        Feature labelFeature = predictionType.getFeatureByBaseName("label");

        for (KBHandle prediction : handles.stream().limit(recommender.getMaxRecommendations())
            .collect(Collectors.toList())) {
            AnnotationFS annotation = aCas.createAnnotation(predictionType, aBegin, aEnd);
            annotation.setStringValue(labelFeature, prediction.getIdentifier());
            aCas.addFsToIndexes(annotation);
        }
    }

    private List<KBHandle> readCandidates(KnowledgeBase kb, String aCoveredText, int aBegin,
            CAS aCas)
    {
        return kbService.read(kb, (conn) -> clService.disambiguate(kb, featureTraits.getScope(),
                featureTraits.getAllowedValueType(), null, aCoveredText, aBegin, aCas));
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        throw new UnsupportedOperationException("Evaluation not supported");
    }
}
