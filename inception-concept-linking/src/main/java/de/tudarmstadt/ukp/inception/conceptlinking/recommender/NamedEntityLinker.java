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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.recommendation.api.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.TokenObject;

public class NamedEntityLinker
    extends Classifier<Object>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private int tokenId = 0;
    private User user;
    private Project project;
    private final String feature = "identifier";

    // Annotations in the project marked as NamedEntity
    private Set<AnnotationObject> nerAnnotations = new HashSet<>();

    // How many predictions for this recommender should be displayed
    private int numPredictions = 3;
    private KnowledgeBaseService kbService;
    private ConceptLinkingService clService;
    private DocumentService documentService;
    private AnnotationSchemaService annoService;
    private FeatureSupportRegistry fsRegistry;

    public NamedEntityLinker(ClassifierConfiguration<Object> aConf, KnowledgeBaseService aKbService,
        ConceptLinkingService aClService, DocumentService aDocService,
        AnnotationSchemaService aAnnoService, FeatureSupportRegistry aFsRegistry)
    {
        super(aConf);
        kbService = aKbService;
        clService = aClService;
        documentService = aDocService;
        annoService = aAnnoService;
        fsRegistry = aFsRegistry;
        conf.setNumPredictions(numPredictions);
    }

    @Override
    public void reconfigure()
    {

    }

    @Override
    public void setModel(Object aModel)
    {
        if (aModel instanceof Set) {
            nerAnnotations = (Set<AnnotationObject>) aModel;
        }
        else {
            log.error("Expected model type: Set<TokenObject> - but was: [{}]",
                aModel != null ? aModel.getClass() : aModel);
        }
    }

    @Override
    public void setUser(User aUser)
    {
        user = aUser;
    }

    @Override
    public void setProject(Project project)
    {
        this.project = project;
    }

    /**
     *
     * @param inputData
     *            All sentences to predict annotations for.
     * @param <T>
     * @return Predicted sentence.
     *         Outer list: Represents a document
     *         Middle list: Represents a sentence
     *         Inner list: Represents a token (predictions for each token)
     */
    @Override
    public <T extends TokenObject> List<List<List<AnnotationObject>>> predictSentences(
        List<List<T>> inputData)
    {
        List<List<List<AnnotationObject>>> result = new ArrayList<>();

        inputData.parallelStream().forEachOrdered(sentence -> {
            List<List<AnnotationObject>> annotatedSentence = new ArrayList<>();
            int sentenceIndex = 0;
            while (sentenceIndex < sentence.size() - 1) {
                TokenObject token = sentence.get(sentenceIndex);
                List<AnnotationObject> word;

                if (isNamedEntity(token)) {
                    StringBuilder coveredText = new StringBuilder(token.getCoveredText());
                    int endCharacter = token.getOffset().getEndCharacter();
                    int endToken = token.getOffset().getEndToken();

                    TokenObject nextTokenObject = sentence.get(sentenceIndex + 1);
                    // Checking whether the next TokenObject is a NE
                    // and whether the sentenceIndex for the next TokenObject is still
                    // in the range of the sentence
                    while (isNamedEntity(nextTokenObject)
                        && sentenceIndex + 1 < sentence.size() - 1) {
                        coveredText.append(" ").append(nextTokenObject.getCoveredText());
                        endCharacter = nextTokenObject.getOffset().getEndCharacter();
                        endToken = nextTokenObject.getOffset().getEndToken();
                        sentenceIndex++;
                        nextTokenObject = sentence.get(sentenceIndex + 1);
                    }

                    token.setCoveredText(coveredText.toString());
                    token.setOffset(new Offset(token.getOffset().getBeginCharacter(), endCharacter,
                        token.getOffset().getBeginToken(), endToken));
                    word = predictToken(token);
                    annotatedSentence.add(word);
                }
                sentenceIndex++;
            }
            result.add(annotatedSentence);
        });
        return result;
    }

    private List<AnnotationObject> predictToken(TokenObject token)
    {
        List<KBHandle> handles = new ArrayList<>();

        AnnotationLayer layer = annoService
            .getLayer("de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity", project);
        AnnotationFeature feat = annoService.getFeature(feature, layer);
        FeatureSupport<ConceptFeatureTraits> fs = fsRegistry.getFeatureSupport(feat);
        ConceptFeatureTraits traits = fs.readTraits(feat);

        if (traits.getRepositoryId() != null) {
            Optional<KnowledgeBase> kb = kbService.getKnowledgeBaseById(project,
                traits.getRepositoryId());
            if (kb.isPresent() && kb.get().isSupportConceptLinking()) {
                handles.addAll(readCandidates(kb.get(), token));
            }
        }
        for (KnowledgeBase kb : kbService.getEnabledKnowledgeBases(project)) {
            if (kb.isSupportConceptLinking()) {
                handles.addAll(readCandidates(kb, token));
            }
        }

        return handles.stream()
            .limit(conf.getNumPredictions())
            .map(h -> new AnnotationObject(token, h.getIdentifier(), h.getDescription(), tokenId++,
                feature, "NamedEntityLinker", conf.getRecommenderId()))
            .collect(Collectors.toList());
    }

    private boolean isNamedEntity(TokenObject token)
    {
        return nerAnnotations.stream()
            .map(AnnotationObject::getTokenObject)
            .anyMatch(t -> t.getOffset().equals(token.getOffset())
                   && t.getDocumentURI().equals(token.getDocumentURI()));
    }

    private List<KBHandle> readCandidates(KnowledgeBase kb, TokenObject token) {
        return kbService.read(kb, (conn) -> {
            SourceDocument doc = documentService
                .getSourceDocument(project, token.getDocumentName());
            AnnotationDocument annoDoc = documentService
                .createOrGetAnnotationDocument(doc, user);
            JCas jCas;
            try {
                jCas = documentService.readAnnotationCas(annoDoc);
                return clService.disambiguate(kb, null, token.getCoveredText(),
                    token.getOffset().getBeginCharacter(), jCas);
            }
            catch (IOException e) {
                log.error("An error occurred while retrieving entity candidates.", e);
                return Collections.emptyList();
            }
        });
    }
}
