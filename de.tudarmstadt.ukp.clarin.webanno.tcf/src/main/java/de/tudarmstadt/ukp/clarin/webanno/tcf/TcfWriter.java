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
import static org.uimafit.util.JCasUtil.select;
import static org.uimafit.util.JCasUtil.selectCovered;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.JCasUtil;

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
import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;
import eu.clarin.weblicht.wlfxb.tc.api.DependencyParsingLayer;
import eu.clarin.weblicht.wlfxb.tc.api.LemmasLayer;
import eu.clarin.weblicht.wlfxb.tc.api.NamedEntitiesLayer;
import eu.clarin.weblicht.wlfxb.tc.api.PosTagsLayer;
import eu.clarin.weblicht.wlfxb.tc.api.Reference;
import eu.clarin.weblicht.wlfxb.tc.api.ReferencesLayer;
import eu.clarin.weblicht.wlfxb.tc.api.SentencesLayer;
import eu.clarin.weblicht.wlfxb.tc.api.TokensLayer;
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
    /**
     * Specify the suffix of output files. Default value <code>.tcf</code>. If the suffix is not
     * needed, provide an empty string as value.
     */
    public static final String PARAM_FILENAME_SUFFIX = "filenameSuffix";
    @ConfigurationParameter(name = PARAM_FILENAME_SUFFIX, mandatory = true, defaultValue = ".tcf")
    private String filenameSuffix;

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        OutputStream docOS = null;
        InputStream docIS = null;
        try {
            docOS = getOutputStream(aJCas, filenameSuffix);

            // Get the original TCF file and preserve it
            DocumentMetaData documentMetadata = DocumentMetaData.get(aJCas);
            /*
             * docIS = new
             * FileInputStream(StringUtils.removeStart(documentMetadata.getDocumentUri(), "file:"));
             */
            URL filePathUrl = new URL(documentMetadata.getDocumentUri());
            docIS = filePathUrl.openStream();
            TextCorpusStored corpus;
            try {
                corpus = casToTcfWriter(docIS, aJCas);
            }
            catch (WLFormatException ex) {
                corpus = casToTcfWriter(aJCas);
            }

            WLData wlData = new WLData(corpus);

            // write the annotated data object into the output stream
            WLDObjector.write(wlData, docOS);
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
     *
     * @param aJcas
     * @return
     */
    public TextCorpusStored casToTcfWriter(JCas aJcas)
    {

        // create TextCorpus object, specifying its language from the aJcas Object
        TextCorpusStored textCorpus = new TextCorpusStored(aJcas.getDocumentLanguage());

        // create text annotation layer and add the string of the text into the layer
        textCorpus.createTextLayer().addText(aJcas.getDocumentText());

        writeToTcf(aJcas, textCorpus);

        return textCorpus;
    }

    /**
     * Merge annotations from CAS into an existing TCF file.
     *
     * @param aIs
     *            the TCF file with an existing annotation layers
     * @param aJcas
     *            an annotated CAS object
     * @return the merged annotation layer in the TCF format
     * @throws ResourceInitializationException
     * @throws AnalysisEngineProcessException
     * @throws WLFormatException
     */
    public TextCorpusStored casToTcfWriter(InputStream aIs, JCas aJcas)
        throws ResourceInitializationException, AnalysisEngineProcessException, WLFormatException
    {
        WLData wLData = WLDObjector.read(aIs);
        TextCorpusStored textCorpus = wLData.getTextCorpus();
        writeToTcf(aJcas, textCorpus);

        return textCorpus;
    }

    /**
     * Add CAS annotations into TCF annotation layers
     *
     * @param aJCas
     * @param aTextCorpus
     */
    @SuppressWarnings("unchecked")
    public static void writeToTcf(JCas aJCas, TextCorpusStored aTextCorpus)
    {
        Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> tokensBeginPositionMap = new HashMap<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token>();
        writeSentence(aJCas, aTextCorpus, tokensBeginPositionMap);
        writeDependency(aJCas, aTextCorpus, tokensBeginPositionMap);
        writeNamedEntity(aJCas, aTextCorpus, tokensBeginPositionMap);
        writeCoreference(aJCas, aTextCorpus, tokensBeginPositionMap);
    }

    private static void writeSentence(JCas aJCas, TextCorpusStored aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        // create tokens annotation layer
        TokensLayer tokensLayer = aTextCorpus.createTokensLayer();

        // create lemma annotation layer
        LemmasLayer lemmasLayer = null;
        if (JCasUtil.exists(aJCas, Lemma.class)) {
            lemmasLayer = aTextCorpus.createLemmasLayer();
        }
        // create POS tag annotation layer
        PosTagsLayer posLayer = null;
        boolean tagSetFound = false;
        if (JCasUtil.exists(aJCas, POS.class)) {

            for (TagsetDescription tagSet : select(aJCas, TagsetDescription.class)) {
                if (tagSet.getLayer().equals(POS.class.getName())) {
                    posLayer = aTextCorpus.createPosTagsLayer(tagSet.getName());
                    tagSetFound = true;
                    break;
                }
            }
            if (!tagSetFound) {
                posLayer = aTextCorpus.createPosTagsLayer("STTS");
            }
        }

        int j = 0;
        for (Token coveredToken : select(aJCas, Token.class)) {
            tokensLayer.addToken(coveredToken.getCoveredText());

            aTokensBeginPositionMap.put(coveredToken.getBegin(), tokensLayer.getToken(j));

            Lemma lemma = coveredToken.getLemma();
            if (lemma != null) {
                String lemmaValue = coveredToken.getLemma().getValue();
                lemmasLayer.addLemma(lemmaValue, tokensLayer.getToken(j));
            }
            POS pos = coveredToken.getPos();
            if (pos != null) {
                String posValue = coveredToken.getPos().getPosValue();
                posLayer.addTag(posValue, tokensLayer.getToken(j));
            }

            j++;
        }

        // if not TCF file, add sentence layer (Sentence is required for BRAT)
        SentencesLayer sentencesLayer = aTextCorpus.getSentencesLayer();
        if (sentencesLayer == null) {
            sentencesLayer = aTextCorpus.createSentencesLayer();
            for (Sentence sentence : select(aJCas, Sentence.class)) {
                List<eu.clarin.weblicht.wlfxb.tc.api.Token> tokens = new ArrayList<eu.clarin.weblicht.wlfxb.tc.api.Token>();
                for (Token token : selectCovered(Token.class, sentence)) {
                    tokens.add(aTokensBeginPositionMap.get(token.getBegin()));
                }
                sentencesLayer.addSentence(tokens);
            }
        }
    }

    private static void writeDependency(JCas aJCas, TextCorpusStored aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        DependencyParsingLayer dependencyParsingLayer = null;
        boolean tagSetFound = false;
        if (JCasUtil.exists(aJCas, Dependency.class)) {

            for (TagsetDescription tagSet : select(aJCas, TagsetDescription.class)) {
                if (tagSet.getLayer().equals(Dependency.class.getName())) {
                    dependencyParsingLayer = aTextCorpus.createDependencyParsingLayer(
                            tagSet.getName(), false, true);
                    tagSetFound = true;
                    break;
                }
            }
            if (!tagSetFound) {
                dependencyParsingLayer = aTextCorpus.createDependencyParsingLayer("tiger", false,
                        true);
            }
        }

            List<eu.clarin.weblicht.wlfxb.tc.api.Dependency> deps = new ArrayList<eu.clarin.weblicht.wlfxb.tc.api.Dependency>();
            for (Dependency d : select(aJCas, Dependency.class)) {
                eu.clarin.weblicht.wlfxb.tc.api.Dependency dependency = dependencyParsingLayer
                        .createDependency(d.getDependencyType(),
                                aTokensBeginPositionMap.get(d.getDependent().getBegin()),
                                aTokensBeginPositionMap.get(d.getGovernor().getBegin()));

                deps.add(dependency);
            }
            if (dependencyParsingLayer != null && deps.size() > 0) {
                dependencyParsingLayer.addParse(deps);
        }
    }

    private static void writeNamedEntity(JCas aJCas, TextCorpusStored aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        NamedEntitiesLayer namedEntitiesLayer = null;
        boolean tagSetFound = false;
        if (JCasUtil.exists(aJCas, NamedEntity.class)) {
            for (TagsetDescription tagSet : select(aJCas, TagsetDescription.class)) {
                if (tagSet.getLayer().equals(NamedEntity.class.getName())) {
                    namedEntitiesLayer = aTextCorpus.createNamedEntitiesLayer(tagSet.getName());
                    tagSetFound = true;
                    break;
                }
            }
            if (!tagSetFound) {
                namedEntitiesLayer = aTextCorpus.createNamedEntitiesLayer("BART");
            }

        }

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

    private static void writeCoreference(JCas aJCas, TextCorpusStored aTextCorpus,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> aTokensBeginPositionMap)
    {
        ReferencesLayer coreferencesLayer = null;
        boolean tagSetFound = false;
        if (JCasUtil.exists(aJCas, CoreferenceChain.class)) {
            for (TagsetDescription tagSet : select(aJCas, TagsetDescription.class)) {
                if (tagSet.getLayer().equals(CoreferenceLink.class.getName())) {
                    coreferencesLayer = aTextCorpus.createReferencesLayer(null, tagSet.getName(),
                            null);
                    tagSetFound = true;
                    break;
                }
            }
            if (!tagSetFound) {
                coreferencesLayer = aTextCorpus.createReferencesLayer(null, "TueBaDz", null);
            }

        }

        for (CoreferenceChain chain : select(aJCas, CoreferenceChain.class)) {
            CoreferenceLink link = chain.getFirst();
            List<Reference> references = new ArrayList<Reference>();

            Reference relation = null;
            Reference reference = null;
            CoreferenceLink relationLink = link.getNext();

            List<eu.clarin.weblicht.wlfxb.tc.api.Token> relationTokens;

            List<eu.clarin.weblicht.wlfxb.tc.api.Token> referenceTokens = null;
            referenceTokens = getListOfTokens(aJCas, link, aTokensBeginPositionMap);

            reference = coreferencesLayer.createReference(link.getReferenceType(), referenceTokens,
                    null);
            if (link.getReferenceRelation() != null
                    && link.getReferenceRelation().equals("expletive")) {
                coreferencesLayer.addRelation(reference, "expletive");
            }
            references.add(reference);
            while (relationLink != null) {

                relationTokens = getListOfTokens(aJCas, relationLink, aTokensBeginPositionMap);

                relation = coreferencesLayer.createReference(relationLink.getReferenceType(),
                        relationTokens, null);
                coreferencesLayer.addRelation(reference, link.getReferenceRelation(), relation);

                references.add(relation);
                // references.add(reference);
                link = relationLink;
                relationLink = relationLink.getNext();

                reference = relation;

            }

            coreferencesLayer.addReferent(references);
        }
    }

    /**
     * in CAS, it is stored using the start/end offsets, in TCF, we should store separate tokens
     * with a token id for each one
     *
     * @param link
     *            the coreference link in CAS annotation
     * @param tokensBeginPositionMap
     *            a Map which store the begin positions of all tokens. This is used to get separate
     *            tokens, the only way to know multiple tokens in a link strat/end offset
     *            annotations.
     * @return
     */

    private static List<eu.clarin.weblicht.wlfxb.tc.api.Token> getListOfTokens(JCas aJcas,
            CoreferenceLink aLink,
            Map<Integer, eu.clarin.weblicht.wlfxb.tc.api.Token> tokensBeginPositionMap)
    {
        List<eu.clarin.weblicht.wlfxb.tc.api.Token> listOfTokens = new ArrayList<eu.clarin.weblicht.wlfxb.tc.api.Token>();
        for (Token token : selectCovered(aJcas, Token.class, aLink.getBegin(), aLink.getEnd())) {
            listOfTokens.add(tokensBeginPositionMap.get(token.getBegin()));
        }
        return listOfTokens;
    }
}
