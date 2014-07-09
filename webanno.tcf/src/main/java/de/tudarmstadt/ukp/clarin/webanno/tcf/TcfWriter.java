/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.tcf;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.uima.fit.util.JCasUtil.exists;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import eu.clarin.weblicht.wlfxb.io.TextCorpusStreamedWithReplaceableLayers;
import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;
import eu.clarin.weblicht.wlfxb.tc.api.DependencyParsingLayer;
import eu.clarin.weblicht.wlfxb.tc.api.LemmasLayer;
import eu.clarin.weblicht.wlfxb.tc.api.NamedEntitiesLayer;
import eu.clarin.weblicht.wlfxb.tc.api.PosTagsLayer;
import eu.clarin.weblicht.wlfxb.tc.api.Reference;
import eu.clarin.weblicht.wlfxb.tc.api.ReferencesLayer;
import eu.clarin.weblicht.wlfxb.tc.api.SentencesLayer;
import eu.clarin.weblicht.wlfxb.tc.api.TextCorpus;
import eu.clarin.weblicht.wlfxb.tc.api.TokensLayer;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusLayerTag;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;

/**
 * Writer for the WebLicht TCF format.
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 */
public class TcfWriter
    extends JCasFileWriter_ImplBase
{
    private static final String REL_TYPE_EXPLETIVE = "expletive";
    
    /**
     * Specify the suffix of output files. Default value <code>.tcf</code>. If the suffix is not
     * needed, provide an empty string as value.
     */
    public static final String PARAM_FILENAME_SUFFIX = "filenameSuffix";
    @ConfigurationParameter(name = PARAM_FILENAME_SUFFIX, mandatory = true, defaultValue = ".tcf")
    private String filenameSuffix;

    /**
     * If there are no annotations for a particular layer in the CAS, preserve any potentially
     * existing annotations in the original TCF.<br>
     * Default: {@code false}
     */
    public static final String PARAM_PRESERVE_IF_EMPTY = "preserveIfEmpty";
    @ConfigurationParameter(name = PARAM_PRESERVE_IF_EMPTY, mandatory = true, defaultValue = "false")
    private boolean preserveIfEmpty;
    
    /**
     * Merge with source TCF file if one is available.<br>
     * Default: {@code true}
     */
    public static final String PARAM_MERGE = "merge";
    @ConfigurationParameter(name = PARAM_MERGE, mandatory = true, defaultValue = "true")
    private boolean merge;

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        OutputStream docOS = null;
        InputStream docIS = null;
        try {
            docOS = getOutputStream(aJCas, filenameSuffix);

            boolean writeWithoutMerging = true;
            if (merge) {
                // Get the original TCF file and preserve it
                DocumentMetaData documentMetadata = DocumentMetaData.get(aJCas);
                URL filePathUrl = new URL(documentMetadata.getDocumentUri());
                try {
                    docIS = filePathUrl.openStream();
                    
                    try {
                        getLogger().debug("Merging with [" + documentMetadata.getDocumentUri() + "]");
                        casToTcfWriter(docIS, aJCas, docOS);
                        writeWithoutMerging = false;
                    }
                    // See https://github.com/weblicht/wlfxb/issues/7
//                    catch (WLFormatException ex) {
//                        getLogger().debug("No source file to merge with: " + ex.getMessage());
//                    }
                    // Workaround: catch all exceptions
                    catch (Exception ex) {
                        getLogger().debug("Source file is not TCF: " + ex.getMessage());
                    }
                }
                catch (IOException e) {
                    getLogger().debug("Cannot open source file to merge with: " + e.getMessage());
                }
            }
            else {
                getLogger().debug("Merging disabled");
            }
            
            // If merging failed or is disabled, go on without merging
            if (writeWithoutMerging) {
                casToTcfWriter(aJCas, docOS);
            }
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
        finally {
            closeQuietly(docOS);
            closeQuietly(docIS);
        }
    }

    /**
     * Create TCF File from scratch
     */
    public void casToTcfWriter(JCas aJCas, OutputStream aOs) throws WLFormatException
    {
        // create TextCorpus object, specifying its language from the aJcas Object
        TextCorpusStored textCorpus = new TextCorpusStored(aJCas.getDocumentLanguage());

        // create text annotation layer and add the string of the text into the layer
        textCorpus.createTextLayer().addText(aJCas.getDocumentText());

        write(aJCas, textCorpus);

        // write the annotated data object into the output stream
        WLData wldata = new WLData(textCorpus);
        WLDObjector.write(wldata, aOs);
    }

    /**
     * Merge annotations from CAS into an existing TCF file.
     *
     * @param aIs
     *            the TCF file with an existing annotation layers
     * @param aJCas
     *            an annotated CAS object
     */
    public void casToTcfWriter(InputStream aIs, JCas aJCas, OutputStream aOs)
        throws ResourceInitializationException, AnalysisEngineProcessException, WLFormatException
    {
        // If these layers are present in the TCF file, we use them from there, otherwise
        // we generate them
        EnumSet<TextCorpusLayerTag> layersToRead = EnumSet.of(
                TextCorpusLayerTag.TOKENS,
                TextCorpusLayerTag.SENTENCES);
        
        // If we have annotations for these layers in the CAS, we rewrite those layers. 
        List<TextCorpusLayerTag> layersToReplace = new ArrayList<TextCorpusLayerTag>();
        if (exists(aJCas, POS.class) || !preserveIfEmpty) {
            layersToReplace.add(TextCorpusLayerTag.POSTAGS);
        }
        if (exists(aJCas, Lemma.class) || !preserveIfEmpty) {
            layersToReplace.add(TextCorpusLayerTag.LEMMAS);
        }
        if (exists(aJCas, NamedEntity.class) || !preserveIfEmpty) {
            layersToReplace.add(TextCorpusLayerTag.NAMED_ENTITIES);
        }
        if (exists(aJCas, Dependency.class) || !preserveIfEmpty) {
            layersToReplace.add(TextCorpusLayerTag.PARSING_DEPENDENCY);
        }
        if (exists(aJCas, CoreferenceChain.class) || !preserveIfEmpty) {
            layersToReplace.add(TextCorpusLayerTag.REFERENCES);
        }
                
        TextCorpusStreamedWithReplaceableLayers textCorpus = null;
        try {
            textCorpus = new TextCorpusStreamedWithReplaceableLayers(
                aIs, layersToRead, EnumSet.copyOf(layersToReplace), aOs);
        
            write(aJCas, textCorpus);
        }
        finally {
            if (textCorpus != null) {
                try {
                    textCorpus.close();
                }
                catch (IOException e) {
                    // Ignore exception while closing
                }
            }
        }
    }

    private void write(JCas aJCas, TextCorpus aTextCorpus)
    {
        Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> tokensBeginPositionMap;
        tokensBeginPositionMap = writeTokens(aJCas, aTextCorpus);
        writeSentence(aJCas, aTextCorpus, tokensBeginPositionMap);
        writePosTags(aJCas, aTextCorpus, tokensBeginPositionMap);
        writeLemmas(aJCas, aTextCorpus, tokensBeginPositionMap);
        writeDependency(aJCas, aTextCorpus, tokensBeginPositionMap);
        writeNamedEntity(aJCas, aTextCorpus, tokensBeginPositionMap);
        writeCoreference(aJCas, aTextCorpus, tokensBeginPositionMap);
    }

    private Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> writeTokens(JCas aJCas,
            TextCorpus aTextCorpus)
    {
        boolean tokensLayerCreated = false;
        
        // Create tokens layer if it does not exist
        TokensLayer tokensLayer = aTextCorpus.getTokensLayer();
        if (tokensLayer == null) {
            tokensLayer = aTextCorpus.createTokensLayer();
            tokensLayerCreated = true;
            getLogger().debug("Layer [" + TextCorpusLayerTag.TOKENS.getXmlName() + "]: created");
        }
        else {
            getLogger().debug("Layer [" + TextCorpusLayerTag.TOKENS.getXmlName() + "]: found");
        }
        
        
        Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> tokensBeginPositionMap = 
                new HashMap<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token>();

        int j = 0;
        for (Token token : select(aJCas, Token.class)) {
            if (tokensLayerCreated) {
                tokensLayer.addToken(token.getCoveredText());
            }

            tokensBeginPositionMap.put(token.getBegin(), tokensLayer.getToken(j));
            j++;
        }
        
        return tokensBeginPositionMap;
    }

    private void writePosTags(JCas aJCas, TextCorpus aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        if (!JCasUtil.exists(aJCas, POS.class)) {
            // Do nothing if there are no part-of-speech tags in the CAS
            getLogger().debug("Layer [" + TextCorpusLayerTag.POSTAGS.getXmlName() + "]: empty");
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
        
        getLogger().debug("Layer [" + TextCorpusLayerTag.POSTAGS.getXmlName() + "]: created");
        
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
    
    private void writeLemmas(JCas aJCas, TextCorpus aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        if (!JCasUtil.exists(aJCas, Lemma.class)) {
            // Do nothing if there are no lemmas in the CAS
            getLogger().debug("Layer [" + TextCorpusLayerTag.LEMMAS.getXmlName() + "]: empty");
            return;
        }
        
        // Tokens layer must already exist
        TokensLayer tokensLayer = aTextCorpus.getTokensLayer();
        
        // create lemma annotation layer
        LemmasLayer lemmasLayer = aTextCorpus.createLemmasLayer();

        getLogger().debug("Layer [" + TextCorpusLayerTag.LEMMAS.getXmlName() + "]: created");

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
    
    private void writeSentence(JCas aJCas, TextCorpus aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        // if not TCF file, add sentence layer (Sentence is required for BRAT)
        SentencesLayer sentencesLayer = aTextCorpus.getSentencesLayer();
        if (sentencesLayer != null) {
            getLogger().debug("Layer [" + TextCorpusLayerTag.SENTENCES.getXmlName() + "]: found");
            return;
        }

        sentencesLayer = aTextCorpus.createSentencesLayer();

        getLogger().debug("Layer [" + TextCorpusLayerTag.SENTENCES.getXmlName() + "]: created");

        for (Sentence sentence : select(aJCas, Sentence.class)) {
            List<eu.clarin.weblicht.wlfxb.tc.api.Token> tokens = new ArrayList<eu.clarin.weblicht.wlfxb.tc.api.Token>();
            for (Token token : selectCovered(Token.class, sentence)) {
                tokens.add(aTokensBeginPositionMap.get(token.getBegin()));
            }
            sentencesLayer.addSentence(tokens);
        }
    }

    private void writeDependency(JCas aJCas, TextCorpus aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        if (!JCasUtil.exists(aJCas, Dependency.class)) {
            // Do nothing if there are no dependencies in the CAS
            getLogger().debug("Layer [" + TextCorpusLayerTag.PARSING_DEPENDENCY.getXmlName() + "]: empty");
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
        
        dependencyParsingLayer = aTextCorpus.createDependencyParsingLayer(tagSetName, false, true);

        getLogger().debug("Layer [" + TextCorpusLayerTag.PARSING_DEPENDENCY.getXmlName() + "]: created");
        
        List<eu.clarin.weblicht.wlfxb.tc.api.Dependency> deps = new ArrayList<eu.clarin.weblicht.wlfxb.tc.api.Dependency>();
        for (Sentence s : select(aJCas, Sentence.class)) {
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

    private void writeNamedEntity(JCas aJCas, TextCorpus aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        if (!JCasUtil.exists(aJCas, NamedEntity.class)) {
            // Do nothing if there are no named entities in the CAS
            getLogger().debug("Layer [" + TextCorpusLayerTag.NAMED_ENTITIES.getXmlName() + "]: empty");
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

        getLogger().debug("Layer [" + TextCorpusLayerTag.NAMED_ENTITIES.getXmlName() + "]: created");
        
        for (NamedEntity namedEntity : select(aJCas, NamedEntity.class)) {
            List<Token> tokensInCas = selectCovered(aJCas, Token.class, namedEntity.getBegin(),
                    namedEntity.getEnd());
            List<eu.clarin.weblicht.wlfxb.tc.api.Token> tokensInTcf = new ArrayList<eu.clarin.weblicht.wlfxb.tc.api.Token>();
            for (Token token : tokensInCas) {
                tokensInTcf.add(aTokensBeginPositionMap.get(token.getBegin()));
            }
            namedEntitiesLayer.addEntity(namedEntity.getValue(), tokensInTcf);
        }
    }

    private void writeCoreference(JCas aJCas, TextCorpus aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        if (!JCasUtil.exists(aJCas, CoreferenceChain.class)) {
            // Do nothing if there are no coreference chains in the CAS
            getLogger().debug("Layer [" + TextCorpusLayerTag.REFERENCES.getXmlName() + "]: empty");
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
        
        getLogger().debug("Layer [" + TextCorpusLayerTag.REFERENCES.getXmlName() + "]: created");

        for (CoreferenceChain chain : select(aJCas, CoreferenceChain.class)) {
            CoreferenceLink prevLink = null;
            Reference prevRef = null;
            List<Reference> refs = new ArrayList<Reference>();
            for (CoreferenceLink link : chain.links()) {
                // Get covered tokens
                List<eu.clarin.weblicht.wlfxb.tc.api.Token> tokens = new ArrayList<eu.clarin.weblicht.wlfxb.tc.api.Token>();
                for (Token token : selectCovered(Token.class, link)) {
                    tokens.add(aTokensBeginPositionMap.get(token.getBegin()));
                }
                
                // Create current reference
                Reference ref = coreferencesLayer.createReference(link.getReferenceType(), tokens, null);

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
