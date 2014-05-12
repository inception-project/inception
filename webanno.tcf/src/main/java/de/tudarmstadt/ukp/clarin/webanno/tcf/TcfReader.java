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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import eu.clarin.weblicht.wlfxb.io.TextCorpusStreamed;
import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;
import eu.clarin.weblicht.wlfxb.tc.api.DependencyParse;
import eu.clarin.weblicht.wlfxb.tc.api.Reference;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;

/**
 * Reader for the WebLicht TCF format. It reads all the available annotation Layers from the TCF
 * file and convert it to a CAS annotations. The TCF data do not have begin/end offsets for all of
 * its annotations which is required in CAS annotation. Hence, addresses are manually calculated per
 * tokens and stored in a map (token_id, token(CAS object)) where later we get can get the offset
 * from the token
 *
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 */
public class TcfReader
    extends JCasResourceCollectionReader_ImplBase
{
    int j = 0;

    @Override
    public void getNext(JCas aJCas)
        throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);

        InputStream is = null;
        try {
            is = new BufferedInputStream(res.getInputStream());
            WLData wLData = WLDObjector.read(is);
            TextCorpusStored aCorpusData = wLData.getTextCorpus();
            convertToCas(aJCas, aCorpusData);
        }
        catch (WLFormatException e) {
            throw new CollectionException(e);
        }
        finally {
            closeQuietly(is);

        }
    }

    private void convertToCas(JCas aJCas, TextCorpusStored aCorpusData)
    {
        convertText(aJCas, aCorpusData);
        Map<String, Token> tokens = convertTokens(aJCas, aCorpusData);
        if (tokens.size() > 0) {

            convertPos(aJCas, aCorpusData, tokens);

            convertLemma(aJCas, aCorpusData, tokens);

            convertSentences(aJCas, aCorpusData, tokens);

            convertDependencies(aJCas, aCorpusData, tokens);

            convertNamedEntities(aJCas, aCorpusData, tokens);

            convertCoreference(aJCas, aCorpusData, tokens);
        }
    }

    /**
     * This method builds texts from the {@link eu.clarin.weblicht.wlfxb.tc.api.Token} annotation
     * layer. The getText Method of {@link TextCorpusStreamed} is not used as some tokens, such as
     * special characters represented differently than in the original text.
     */
    private void convertText(JCas aJCas, TextCorpusStored aCorpusData)
    {
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < aCorpusData.getTokensLayer().size(); i++) {
            eu.clarin.weblicht.wlfxb.tc.api.Token token = aCorpusData.getTokensLayer().getToken(i);
            text.append(token.getString() + " ");
        }
        aJCas.setDocumentText(text.toString());
        aJCas.setDocumentLanguage(aCorpusData.getLanguage());
    }

    /**
     * Convert TCF Tokens Layer to CAS Token Annotation.
     * 
     * @return returns {@code Map} of (token_id, Token), for later references
     */
    private Map<String, Token> convertTokens(JCas aJCas, TextCorpusStored aCorpusData)
    {
        if (aCorpusData.getTokensLayer() == null) {
            // No layer to read from.
            return new HashMap<String, Token>();
        }

        String text = aJCas.getDocumentText();

        Token outToken;
        int tokenBeginPosition = 0;
        int tokenEndPosition;
        Map<String, Token> tokens = new HashMap<String, Token>();

        for (int i = 0; i < aCorpusData.getTokensLayer().size(); i++) {

            eu.clarin.weblicht.wlfxb.tc.api.Token token = aCorpusData.getTokensLayer().getToken(i);

            tokenBeginPosition = text.indexOf(token.getString(), tokenBeginPosition);
            tokenEndPosition = text.indexOf(token.getString(), tokenBeginPosition)
                    + token.getString().length();

            outToken = new Token(aJCas, tokenBeginPosition, tokenEndPosition);
            outToken.addToIndexes();

            tokens.put(token.getID(), outToken);
            tokenBeginPosition = tokenEndPosition;
        }
        return tokens;
    }

    private void convertPos(JCas aJCas, TextCorpusStored aCorpusData, Map<String, Token> aTokens)
    {
        if (aCorpusData.getPosTagsLayer() == null) {
            return;
        }
        for (int i = 0; i < aCorpusData.getPosTagsLayer().size(); i++) {
            eu.clarin.weblicht.wlfxb.tc.api.Token[] posTokens = aCorpusData.getPosTagsLayer()
                    .getTokens(aCorpusData.getPosTagsLayer().getTag(i));
            String value = aCorpusData.getPosTagsLayer().getTag(i).getString();

            POS outPos = new POS(aJCas);

            outPos.setBegin(aTokens.get(posTokens[0].getID()).getBegin());
            outPos.setEnd(aTokens.get(posTokens[0].getID()).getEnd());
            outPos.setPosValue(value);
            outPos.addToIndexes();

            // Set the POS to the token
            aTokens.get(posTokens[0].getID()).setPos(outPos);
        }
    }

    private void convertLemma(JCas aJCas, TextCorpusStored aCorpusData, Map<String, Token> aTokens)
    {
        if (aCorpusData.getLemmasLayer() == null) {
            return;
        }
        for (int i = 0; i < aCorpusData.getLemmasLayer().size(); i++) {
            eu.clarin.weblicht.wlfxb.tc.api.Token[] lemmaTokens = aCorpusData.getLemmasLayer()
                    .getTokens(aCorpusData.getLemmasLayer().getLemma(i));
            String value = aCorpusData.getLemmasLayer().getLemma(i).getString();

            Lemma outLemma = new Lemma(aJCas);

            outLemma.setBegin(aTokens.get(lemmaTokens[0].getID()).getBegin());
            outLemma.setEnd(aTokens.get(lemmaTokens[0].getID()).getEnd());
            outLemma.setValue(value);
            outLemma.addToIndexes();

            // Set the lemma to the token
            aTokens.get(lemmaTokens[0].getID()).setLemma(outLemma);
        }

    }

    private void convertSentences(JCas aJCas, TextCorpusStored aCorpusData,
            Map<String, Token> aTokens)
    {
        if (aCorpusData.getSentencesLayer() == null) {
            // No layer to read from.
            return;
        }

        for (int i = 0; i < aCorpusData.getSentencesLayer().size(); i++) {
            eu.clarin.weblicht.wlfxb.tc.api.Token[] sentencesTokens = aCorpusData
                    .getSentencesLayer().getTokens(aCorpusData.getSentencesLayer().getSentence(i));

            Sentence outSentence = new Sentence(aJCas);

            outSentence.setBegin(aTokens.get(sentencesTokens[0].getID()).getBegin());
            outSentence.setEnd(aTokens.get(sentencesTokens[sentencesTokens.length - 1].getID())
                    .getEnd());
            outSentence.addToIndexes();
        }
    }

    private void convertDependencies(JCas aJCas, TextCorpusStored aCorpusData,
            Map<String, Token> aTokens)
    {
        if (aCorpusData.getDependencyParsingLayer() == null) {
            // No layer to read from.
            return;
        }

        for (int i = 0; i < aCorpusData.getDependencyParsingLayer().size(); i++) {
            DependencyParse dependencyParse = aCorpusData.getDependencyParsingLayer().getParse(i);
            for (eu.clarin.weblicht.wlfxb.tc.api.Dependency dependency : dependencyParse
                    .getDependencies()) {

                eu.clarin.weblicht.wlfxb.tc.api.Token[] governorTokens = aCorpusData
                        .getDependencyParsingLayer().getGovernorTokens(dependency);
                eu.clarin.weblicht.wlfxb.tc.api.Token[] dependentTokens = aCorpusData
                        .getDependencyParsingLayer().getDependentTokens(dependency);

                POS dependentPos = aTokens.get(dependentTokens[0].getID()).getPos();

                // For dependency annotations in the TCF file without POS, add as a default POS --
                if (dependentPos == null) {
                    getUimaContext().getLogger().log(Level.INFO,
                            "There is no pos for this token, added is -- as a pos");
                    dependentPos = new POS(aJCas);
                    dependentPos.setBegin(aTokens.get(dependentTokens[0].getID()).getBegin());
                    dependentPos.setEnd(aTokens.get(dependentTokens[0].getID()).getEnd());
                    dependentPos.setPosValue("--");
                    dependentPos.addToIndexes();
                    aTokens.get(dependentTokens[0].getID()).setPos(dependentPos);
                }
                if (governorTokens != null) {
                    POS governerPos = aTokens.get(governorTokens[0].getID()).getPos();
                    if (governerPos == null) {
                        if (dependency.getFunction().equals("ROOT")) {
                            // do nothing
                        }
                        else {
                            getUimaContext().getLogger().log(Level.INFO,
                                    "There is no pos for this token, added is -- as a pos");
                            governerPos = new POS(aJCas);
                            governerPos.setBegin(aTokens.get(governorTokens[0].getID()).getBegin());
                            governerPos.setEnd(aTokens.get(governorTokens[0].getID()).getEnd());
                            governerPos.setPosValue("--");
                            governerPos.addToIndexes();
                            aTokens.get(governorTokens[0].getID()).setPos(governerPos);
                        }
                    }
                }
                else {
                    governorTokens = dependentTokens;
                }
                Dependency outDependency = new Dependency(aJCas);
                // if span A has (start,end)= (20, 26) and B has (start,end)= (30, 36)
                // arc drawn from A to B, dependency will have (start, end) = (20, 36)
                // arc drawn from B to A, still dependency will have (start, end) = (20, 36)
                if(aTokens.get(dependentTokens[0].getID()).getBegin()<aTokens.get(governorTokens[0].getID()).getBegin()){
                    outDependency.setBegin(aTokens.get(dependentTokens[0].getID()).getBegin());
                    outDependency.setEnd(aTokens.get(governorTokens[0].getID()).getEnd());
                }
                else{
                    outDependency.setBegin(aTokens.get(governorTokens[0].getID()).getBegin());
                    outDependency.setEnd(aTokens.get(dependentTokens[0].getID()).getEnd());
                }

                outDependency.setDependencyType(dependency.getFunction());
                outDependency.setGovernor(aTokens.get(governorTokens[0].getID()));
                outDependency.setDependent(dependency.getFunction().equals("ROOT") ? aTokens
                        .get(governorTokens[0].getID()) : aTokens.get(dependentTokens[0].getID()));
                outDependency.addToIndexes();

            }
        }
    }

    private void convertNamedEntities(JCas aJCas, TextCorpusStored aCorpusData,
            Map<String, Token> aTokens)
    {
        if (aCorpusData.getNamedEntitiesLayer() == null) {
            // No layer to read from.
            return;
        }

        for (int i = 0; i < aCorpusData.getNamedEntitiesLayer().size(); i++) {
            // get the named entity
            eu.clarin.weblicht.wlfxb.tc.api.NamedEntity entity = aCorpusData
                    .getNamedEntitiesLayer().getEntity(i);

            eu.clarin.weblicht.wlfxb.tc.api.Token[] namedEntityTokens = aCorpusData
                    .getNamedEntitiesLayer().getTokens(entity);

            NamedEntity outNamedEntity = new NamedEntity(aJCas);

            outNamedEntity.setBegin(getOffsets(namedEntityTokens, aTokens)[0]);
            outNamedEntity.setEnd(getOffsets(namedEntityTokens, aTokens)[1]);
            outNamedEntity.setValue(entity.getType());
            outNamedEntity.addToIndexes();
        }

    }

    /**
     * Correferences in CAS should be represented {@link CoreferenceChain} and
     * {@link CoreferenceLink}. The TCF representation Uses <b> rel </b> and <b>target </b> to build
     * chains. Example: </br><i>
     * {@literal  <entity><reference ID="rc_0" tokenIDs="t_0" mintokIDs="t_0" type="nam"/> } </br>
     * {@literal <reference ID="rc_1" tokenIDs="t_6" mintokIDs="t_6" type="pro.per3" rel="anaphoric" target="rc_0"/></entity>
     * }</i> </br> The first phase of conversion is getting all <b>references</b> and
     * <b>targets</b> alongside the <b>type</b> and <b>relations in different maps</b> <br>
     * Second, an iteration is made through all the maps and the {@link CoreferenceChain} and
     * {@link CoreferenceLink} annotations are constructed.
     */
    private void convertCoreference(JCas aJCas, TextCorpusStored aCorpusData,
            Map<String, Token> aTokens)
    {
        if (aCorpusData.getReferencesLayer() == null) {
            // No layer to read from.
            return;
        }
        for (int i = 0; i < aCorpusData.getReferencesLayer().size(); i++) {
            eu.clarin.weblicht.wlfxb.tc.api.ReferencedEntity entity = aCorpusData
                    .getReferencesLayer().getReferencedEntity(i);

            Map<Integer, CoreferenceLink> referencesMap = new TreeMap<Integer, CoreferenceLink>();
            storeReferencesAndTargetsInMap(referencesMap, entity, aCorpusData, aTokens, aJCas);

            CoreferenceChain chain = new CoreferenceChain(aJCas);
            CoreferenceLink link = null;
            for (Integer address : referencesMap.keySet()) {
                if (chain.getFirst() == null) {
                    chain.setFirst(referencesMap.get(address));
                    link = chain.getFirst();
                    chain.addToIndexes();
                }
                else {
                    link.setNext(referencesMap.get(address));
                    if (link.getReferenceRelation() == null) {
                        link.setReferenceRelation(referencesMap.get(address).getReferenceRelation());
                    }
                    link = link.getNext();
                    link.addToIndexes();
                }
            }
        }
    }

    private void storeReferencesAndTargetsInMap(Map<Integer, CoreferenceLink> aReferencesMap,
            eu.clarin.weblicht.wlfxb.tc.api.ReferencedEntity entity, TextCorpusStored aCorpusData,
            Map<String, Token> aTokens, JCas aJcas)
    {
        for (Reference reference : entity.getReferences()) {
            StringBuilder sbTokens = new StringBuilder();
            for (eu.clarin.weblicht.wlfxb.tc.api.Token token : aCorpusData.getReferencesLayer()
                    .getTokens(reference)) {
                sbTokens.append(token.getID() + " ");
            }

            String[] referenceTokens = sbTokens.toString().split(" ");
            int begin = getOffsets(referenceTokens, aTokens)[0];
            int end = getOffsets(referenceTokens, aTokens)[1];

            CoreferenceLink link = new CoreferenceLink(aJcas);
            link.setBegin(begin);
            link.setEnd(end);
            String referencesType = reference.getType() == null ? "nam" : reference.getType();
            link.setReferenceType(referencesType);
            if (reference.getRelation() != null) {
                link.setReferenceRelation(reference.getRelation());
            }
            link.addToIndexes();
            aReferencesMap.put(link.getAddress(), link);

        }
    }

    /**
     * Get the start and end offsets of a span annotation
     * 
     * @param aSpanTokens
     *            list of span {@link eu.clarin.weblicht.wlfxb.tc.api.Token}s
     * @param aAllTokens
     *            all available tokens in the file
     */
    private int[] getOffsets(eu.clarin.weblicht.wlfxb.tc.api.Token[] aSpanTokens,
            Map<String, Token> aAllTokens)
    {
        List<Integer> beginPositions = new ArrayList<Integer>();
        List<Integer> endPositions = new ArrayList<Integer>();
        for (eu.clarin.weblicht.wlfxb.tc.api.Token token : aSpanTokens) {
            beginPositions.add(aAllTokens.get(token.getID()).getBegin());
            endPositions.add(aAllTokens.get(token.getID()).getEnd());
        }
        return new int[] { (Collections.min(beginPositions)), (Collections.max(endPositions)) };
    }

    /**
     * Get the start and end offsets of a span annotation
     * 
     * @param aSpanTokens
     *            list of span token ids. [t_3,_t_5, t_1]
     * @param aAllTokens
     *            all available tokens in the file
     */
    private int[] getOffsets(String[] aSpanTokens, Map<String, Token> aAllTokens)
    {
        List<Integer> beginPositions = new ArrayList<Integer>();
        List<Integer> endPositions = new ArrayList<Integer>();
        for (String token : aSpanTokens) {
            beginPositions.add(aAllTokens.get(token).getBegin());
            endPositions.add(aAllTokens.get(token).getEnd());
        }
        return new int[] { (Collections.min(beginPositions)), (Collections.max(endPositions)) };
    }
}
