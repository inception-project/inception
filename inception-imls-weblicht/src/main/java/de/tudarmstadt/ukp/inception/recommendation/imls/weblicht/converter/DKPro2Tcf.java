/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.converter;

import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;
import de.tudarmstadt.ukp.dkpro.core.api.transform.type.SofaChangeAnnotation;
import eu.clarin.weblicht.wlfxb.tc.api.CorrectionOperation;
import eu.clarin.weblicht.wlfxb.tc.api.DependencyParsingLayer;
import eu.clarin.weblicht.wlfxb.tc.api.LemmasLayer;
import eu.clarin.weblicht.wlfxb.tc.api.NamedEntitiesLayer;
import eu.clarin.weblicht.wlfxb.tc.api.OrthographyLayer;
import eu.clarin.weblicht.wlfxb.tc.api.PosTagsLayer;
import eu.clarin.weblicht.wlfxb.tc.api.Reference;
import eu.clarin.weblicht.wlfxb.tc.api.ReferencesLayer;
import eu.clarin.weblicht.wlfxb.tc.api.SentencesLayer;
import eu.clarin.weblicht.wlfxb.tc.api.TextCorpus;
import eu.clarin.weblicht.wlfxb.tc.api.TokensLayer;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusLayerTag;

public class DKPro2Tcf
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final String REL_TYPE_EXPLETIVE = "expletive";
    
    public void convert(JCas aJCas, TextCorpus textCorpus)
    {
        write(aJCas, textCorpus);
    }
    
    public void write(JCas aJCas, TextCorpus aTextCorpus)
    {
        Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> tokensBeginPositionMap;
        tokensBeginPositionMap = writeTokens(aJCas, aTextCorpus);
        writeSentence(aJCas, aTextCorpus, tokensBeginPositionMap);
        writePosTags(aJCas, aTextCorpus, tokensBeginPositionMap);
        writeLemmas(aJCas, aTextCorpus, tokensBeginPositionMap);
        writeOrthograph(aJCas, aTextCorpus);
        writeDependency(aJCas, aTextCorpus, tokensBeginPositionMap);
        writeNamedEntity(aJCas, aTextCorpus, tokensBeginPositionMap);
        writeCoreference(aJCas, aTextCorpus, tokensBeginPositionMap);
    }

    public Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> writeTokens(JCas aJCas,
            TextCorpus aTextCorpus)
    {
        boolean tokensLayerCreated = false;
        
        // Create tokens layer if it does not exist
        TokensLayer tokensLayer = aTextCorpus.getTokensLayer();
        if (tokensLayer == null) {
            tokensLayer = aTextCorpus.createTokensLayer();
            tokensLayerCreated = true;
            log.debug("Layer [{}]: created", TextCorpusLayerTag.TOKENS.getXmlName());
        }
        else {
            log.debug("Layer [{}]: found", TextCorpusLayerTag.TOKENS.getXmlName());
        }
        
        
        Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> tokensBeginPositionMap =
                new HashMap<>();

        int j = 0;
        for (Token token : select(aJCas, Token.class)) {
            if (tokensLayerCreated) {
                if (token.getId() != null) {
                    // Assuming all of the tokens have IDs ...
                    tokensLayer.addToken(token.getCoveredText(), token.getBegin(), token.getEnd(),
                            token.getId());
                }
                else {
                    // Assuming none of the tokens have IDs ...
                    tokensLayer.addToken(token.getCoveredText(), token.getBegin(), token.getEnd());
                }
            }

            tokensBeginPositionMap.put(token.getBegin(), tokensLayer.getToken(j));
            j++;
        }
        
        return tokensBeginPositionMap;
    }

    public void writePosTags(JCas aJCas, TextCorpus aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        if (!JCasUtil.exists(aJCas, POS.class)) {
            // Do nothing if there are no part-of-speech tags in the CAS
            log.debug("Layer [{}]: empty", TextCorpusLayerTag.POSTAGS.getXmlName());
            return;
        }

        // Tokens layer must already exist
        TokensLayer tokensLayer = aTextCorpus.getTokensLayer();
        
        // create POS tag annotation layer
        String posTagSet = "STTS";
        for (TagsetDescription tagSet : select(aJCas, TagsetDescription.class)) {
            if (tagSet.getLayer().equals(POS.class.getName())) {
                posTagSet = tagSet.getName();
                break;
            }
        }
        
        PosTagsLayer posLayer = aTextCorpus.createPosTagsLayer(posTagSet);
        
        log.debug("Layer [{}]: created", TextCorpusLayerTag.POSTAGS.getXmlName());
        
        int j = 0;
        for (Token coveredToken : select(aJCas, Token.class)) {
            POS pos = coveredToken.getPos();

            if (pos != null && posLayer != null ) {
                String posValue = coveredToken.getPos().getPosValue();
                posLayer.addTag(posValue, tokensLayer.getToken(j));
            }

            j++;
        }
    }
    
    public void writeLemmas(JCas aJCas, TextCorpus aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        if (!JCasUtil.exists(aJCas, Lemma.class)) {
            // Do nothing if there are no lemmas in the CAS
            log.debug("Layer [{}]: empty", TextCorpusLayerTag.LEMMAS.getXmlName());
            return;
        }
        
        // Tokens layer must already exist
        TokensLayer tokensLayer = aTextCorpus.getTokensLayer();
        
        // create lemma annotation layer
        LemmasLayer lemmasLayer = aTextCorpus.createLemmasLayer();

        log.debug("Layer [{}]: created", TextCorpusLayerTag.LEMMAS.getXmlName());

        int j = 0;
        for (Token coveredToken : select(aJCas, Token.class)) {
            Lemma lemma = coveredToken.getLemma();
            if (lemma != null && lemmasLayer != null) {
                String lemmaValue = coveredToken.getLemma().getValue();
                lemmasLayer.addLemma(lemmaValue, tokensLayer.getToken(j));
            }
            j++;
        }
        
    }
    
    public void writeOrthograph(JCas aJCas, TextCorpus aTextCorpus) {
        if (!JCasUtil.exists(aJCas, SofaChangeAnnotation.class)) {
            // Do nothing if there are no SofaChangeAnnotation layer
            // (Which is equivalent to Orthography layer in TCF) in the CAS
            log.debug("Layer [{}]: empty", TextCorpusLayerTag.ORTHOGRAPHY.getXmlName());
            return;
        }

        // Tokens layer must already exist
        TokensLayer tokensLayer = aTextCorpus.getTokensLayer();

        // create orthographyLayer annotation layer
        OrthographyLayer orthographyLayer = aTextCorpus.createOrthographyLayer();

        log.debug("Layer [{}]: created", TextCorpusLayerTag.ORTHOGRAPHY.getXmlName());

        int j = 0;
        for (Token token : select(aJCas, Token.class)) {
            List<SofaChangeAnnotation> scas = selectCovered(aJCas, SofaChangeAnnotation.class,
                    token.getBegin(), token.getEnd());
            if (scas.size() > 0 && orthographyLayer != null) {
                SofaChangeAnnotation change = scas.get(0);
                
                orthographyLayer.addCorrection(scas.get(0).getValue(), tokensLayer.getToken(j),
                        Optional.ofNullable(change.getOperation()).map(CorrectionOperation::valueOf)
                                .orElse(null));
            }
            j++;
        }

    }
    
    public void writeSentence(JCas aJCas, TextCorpus aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        // if not TCF file, add sentence layer (Sentence is required for BRAT)
        SentencesLayer sentencesLayer = aTextCorpus.getSentencesLayer();
        if (sentencesLayer != null) {
            log.debug("Layer [{}]: found", TextCorpusLayerTag.SENTENCES.getXmlName());
            return;
        }

        sentencesLayer = aTextCorpus.createSentencesLayer();

        log.debug("Layer [{}]: created", TextCorpusLayerTag.SENTENCES.getXmlName());

        for (Sentence sentence : select(aJCas, Sentence.class)) {
            List<eu.clarin.weblicht.wlfxb.tc.api.Token> tokens = new ArrayList<>();
            for (Token token : selectCovered(Token.class, sentence)) {
                tokens.add(aTokensBeginPositionMap.get(token.getBegin()));
            }
            sentencesLayer.addSentence(tokens);
        }
    }

    public void writeDependency(JCas aJCas, TextCorpus aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        if (!JCasUtil.exists(aJCas, Dependency.class)) {
            // Do nothing if there are no dependencies in the CAS
            log.debug("Layer [{}]: empty", TextCorpusLayerTag.PARSING_DEPENDENCY.getXmlName());
            return;
        }

        DependencyParsingLayer dependencyParsingLayer = null;
        String tagSetName = "tiger";
        for (TagsetDescription tagSet : select(aJCas, TagsetDescription.class)) {
            if (tagSet.getLayer().equals(Dependency.class.getName())) {
                tagSetName = tagSet.getName();
                break;
            }
        }
        
        Optional<Dependency> hasNonBasic = select(aJCas, Dependency.class).stream()
            .filter(dep -> dep.getFlavor() != null && 
                    !DependencyFlavor.BASIC.equals(dep.getFlavor()))
            .findAny();
        
        dependencyParsingLayer = aTextCorpus.createDependencyParsingLayer(tagSetName,
                hasNonBasic.isPresent(), true);

        log.debug("Layer [{}]: created", TextCorpusLayerTag.PARSING_DEPENDENCY.getXmlName());
        
        for (Sentence s : select(aJCas, Sentence.class)) {
            List<eu.clarin.weblicht.wlfxb.tc.api.Dependency> deps = new ArrayList<>();
            for (Dependency d : selectCovered(Dependency.class, s)) {
                eu.clarin.weblicht.wlfxb.tc.api.Dependency dependency = dependencyParsingLayer
                        .createDependency(d.getDependencyType(),
                                aTokensBeginPositionMap.get(d.getDependent().getBegin()),
                                aTokensBeginPositionMap.get(d.getGovernor().getBegin()));

                deps.add(dependency);
            }
            if (deps.size() > 0) {
                dependencyParsingLayer.addParse(deps);
            }
        }
    }

    public void writeNamedEntity(JCas aJCas, TextCorpus aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        if (!JCasUtil.exists(aJCas, NamedEntity.class)) {
            // Do nothing if there are no named entities in the CAS
            log.debug("Layer [{}]: empty", TextCorpusLayerTag.NAMED_ENTITIES.getXmlName());
            return;
        }
        
        String tagSetName = "BART";
        for (TagsetDescription tagSet : select(aJCas, TagsetDescription.class)) {
            if (tagSet.getLayer().equals(NamedEntity.class.getName())) {
                tagSetName = tagSet.getName();
                break;
            }
        }

        NamedEntitiesLayer namedEntitiesLayer = aTextCorpus.createNamedEntitiesLayer(tagSetName);

        log.debug("Layer [{}]: created", TextCorpusLayerTag.NAMED_ENTITIES.getXmlName());
        
        for (NamedEntity namedEntity : select(aJCas, NamedEntity.class)) {
            List<Token> tokensInCas = selectCovered(aJCas, Token.class, namedEntity.getBegin(),
                    namedEntity.getEnd());
            List<eu.clarin.weblicht.wlfxb.tc.api.Token> tokensInTcf = new ArrayList<>();
            for (Token token : tokensInCas) {
                tokensInTcf.add(aTokensBeginPositionMap.get(token.getBegin()));
            }
            namedEntitiesLayer.addEntity(namedEntity.getValue(), tokensInTcf);
        }
    }

    public void writeCoreference(JCas aJCas, TextCorpus aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        if (!JCasUtil.exists(aJCas, CoreferenceChain.class)) {
            // Do nothing if there are no coreference chains in the CAS
            log.debug("Layer [{}]: empty", TextCorpusLayerTag.REFERENCES.getXmlName());
            return;
        }
        
        String tagSetName = "TueBaDz";
        for (TagsetDescription tagSet : select(aJCas, TagsetDescription.class)) {
            if (tagSet.getLayer().equals(CoreferenceLink.class.getName())) {
                tagSetName = tagSet.getName();
                break;
            }
        }
        
        ReferencesLayer coreferencesLayer = aTextCorpus.createReferencesLayer(null, tagSetName,
                null);
        
        log.debug("Layer [{}]: created", TextCorpusLayerTag.REFERENCES.getXmlName());

        // Sort by begin to provide a more-or-less stable order for the unit tests
        List<CoreferenceChain> chains = select(aJCas, CoreferenceChain.class)
                .stream()
                .filter(chain -> chain.getFirst() != null)
                .sorted((a, b) -> a.getFirst().getBegin() - b.getFirst().getBegin())
                .collect(Collectors.toList());
        
        for (CoreferenceChain chain : chains) {
            CoreferenceLink prevLink = null;
            Reference prevRef = null;
            List<Reference> refs = new ArrayList<>();
            for (CoreferenceLink link : chain.links()) {
                // Get covered tokens
                List<eu.clarin.weblicht.wlfxb.tc.api.Token> tokens = new ArrayList<>();
                for (Token token : selectCovered(Token.class, link)) {
                    tokens.add(aTokensBeginPositionMap.get(token.getBegin()));
                }
                
                // Create current reference
                Reference ref = coreferencesLayer.createReference(link.getReferenceType(), tokens,
                        null);

                // Special handling for expletive relations
                if (REL_TYPE_EXPLETIVE.equals(link.getReferenceRelation())) {
                    coreferencesLayer.addRelation(ref, REL_TYPE_EXPLETIVE);
                    // if the relation is expletive, then there must not be a next element in the
                    // chain, so we bail out here.
                    continue; 
                }
                
                // Create relation between previous and current reference
                if (prevLink != null) {
                    coreferencesLayer.addRelation(prevRef, prevLink.getReferenceRelation(), ref);
                }
                
                prevLink = link;
                prevRef = ref;
                refs.add(ref);
            }
            coreferencesLayer.addReferent(refs);
        }
    }
}
