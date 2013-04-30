/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import static org.uimafit.factory.AnalysisEngineFactory.*;
import static org.uimafit.util.JCasUtil.select;
import static org.uimafit.util.JCasUtil.selectFollowing;
import static org.uimafit.util.JCasUtil.selectPreceding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.JCasFactory;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorUIData;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

/**
 * Contain Methods for updating CAS Objects directed from brat UI, different utility methods to
 * process the CAS such getting the sentence address, determine page numbers,...
 *
 * @author Seid Muhie Yimam
 *
 */
public class BratAjaxCasUtil
{


    public static void updateCoreferenceType(BratAnnotatorModel aBratAnnotatorModel, String aType,
            BratAnnotatorUIData aUIData)
    {
        boolean modify = false;
        CoreferenceLink corefeTypeToAdd = null;

        for (CoreferenceLink link : select(aUIData.getjCas(), CoreferenceLink.class)) {
            if (link.getBegin() == aUIData.getAnnotationOffsetStart()
                    && link.getEnd() == aUIData.getAnnotationOffsetEnd()) {
                corefeTypeToAdd = link;
                modify = !link.getReferenceType().equals(aType);
                break;
            }
            while (link.getNext() != null) {
                if (link.getNext().getBegin() == aUIData.getAnnotationOffsetStart()
                        && link.getNext().getEnd() == aUIData.getAnnotationOffsetEnd()) {
                    corefeTypeToAdd = link.getNext();
                    modify = !link.getNext().getReferenceType().equals(aType);
                    link = link.getNext();
                    break;
                }
                else {
                    link = link.getNext();
                }
            }
        }
        if (corefeTypeToAdd == null) {

            Map<Integer, Integer> offsets = offsets(aUIData.getjCas());

            int startAndEnd[] = getTokenStart(offsets,
                    aUIData.getAnnotationOffsetStart(),
                    aUIData.getAnnotationOffsetEnd());

            aUIData.setAnnotationOffsetStart(startAndEnd[0]);
            aUIData.setAnnotationOffsetEnd(startAndEnd[1]);

            corefeTypeToAdd = new CoreferenceLink(aUIData.getjCas(),
                    aUIData.getAnnotationOffsetStart(),
                    aUIData.getAnnotationOffsetEnd());
            corefeTypeToAdd.setReferenceType(aType);
            corefeTypeToAdd.addToIndexes();
        }

        if (modify) {
            corefeTypeToAdd.setReferenceType(aType);
        }
    }

    // Updating a coreference.
    // CASE 1: Chain does not exist yet
    // CASE 2: Add to the beginning of an existing chain
    // CASE 3: Add to the end of an existing chain
    // CASE 4: Replace a link in an existing chain
    // CASE 4a: we replace the link to the last link -> delete last link
    // CASE 4b: we replace the link to an intermediate link -> chain is cut in two,
    // create new CorefChain pointing to the first link in new chain
    // CASE 5: Add link at the same position as existing -> just update type

    public static void updateCoreferenceRelation(BratAnnotatorModel aBratAnnotatorModel,
            String aRelation, BratAnnotatorUIData aUIData)
    {
        boolean modify = false;

        CoreferenceLink originCoreferenceType = selectAnnotationByAddress(aUIData.getjCas(),
				CoreferenceLink.class, aUIData.getOrigin());
        CoreferenceLink targetCoreferenceType = selectAnnotationByAddress(aUIData.getjCas(),
				CoreferenceLink.class, aUIData.getTarget());

        // Currently support only anaphoric relation
        // Inverse direction
        if (originCoreferenceType.getBegin() > targetCoreferenceType.getBegin()) {
            CoreferenceLink temp = originCoreferenceType;
            originCoreferenceType = targetCoreferenceType;
            targetCoreferenceType = temp;
        }

        CoreferenceLink existingCoreference = null;
        boolean chainExist = false;
        boolean found = false;

        // If the two links are in different chain, merge them!!!
        boolean merge = mergeChain(aUIData.getjCas(), originCoreferenceType, targetCoreferenceType,
                aRelation);

        if (!merge) {
            for (CoreferenceChain chain : select(aUIData.getjCas(), CoreferenceChain.class)) {

                // CASE 2
                if (chain.getFirst() != null && !found
                        && chain.getFirst().getBegin() == targetCoreferenceType.getBegin()) {
                    chain.setFirst(originCoreferenceType);
                    originCoreferenceType.setNext(targetCoreferenceType);
                    originCoreferenceType.setReferenceRelation(aRelation);
                    found = true;
                    break;
                }

                CoreferenceLink link = chain.getFirst();
                CoreferenceLink lastLink = link;

                while (link != null && !found) {
                    // a-> c, b->c = a->b->c
                    if (link.getNext() != null && isAt(link.getNext(), targetCoreferenceType)) {
                        if (link.getBegin() > originCoreferenceType.getBegin()) {
                            originCoreferenceType.setNext(link);

                            if (lastLink == chain.getFirst()) {
                                chain.setFirst(originCoreferenceType);
                            }
                            else {
                                lastLink.setNext(originCoreferenceType);
                            }
                        }
                        else {
                            link.setNext(originCoreferenceType);
                            originCoreferenceType.setNext(targetCoreferenceType);
                        }
                        originCoreferenceType.setReferenceRelation(aRelation);
                        chainExist = true;
                        found = true;
                        break;
                    }
                    // CASE 4a/b
                    if (isAt(link, originCoreferenceType) && link.getNext() != null
                            && !isAt(link.getNext(), targetCoreferenceType)
                            && targetCoreferenceType.getBegin() < link.getNext().getBegin()) {
                        CoreferenceLink tmpLink = link.getNext();
                        String tmpRel = link.getReferenceRelation();
                        link.setNext(targetCoreferenceType);
                        link.setReferenceRelation(aRelation);
                        targetCoreferenceType.setNext(tmpLink);
                        targetCoreferenceType.setReferenceRelation(tmpRel);
                        chainExist = true;
                        found = true;
                        break;
                    }
                    else if (isAt(link, originCoreferenceType) && link.getNext() != null
                            && !isAt(link.getNext(), targetCoreferenceType)) {
                        link = link.getNext();
                        originCoreferenceType = link;
                        continue;
                    }
                    else if (isAt(link, originCoreferenceType) && link.getNext() == null) {
                        link.setNext(targetCoreferenceType);
                        link.setReferenceRelation(aRelation);
                        chainExist = true;
                        found = true;
                        break;
                    }
                    if (isAt(link, originCoreferenceType) && link.getNext() != null
                            && isAt(link.getNext(), targetCoreferenceType)) {
                        modify = !link.getReferenceType().equals(aRelation);
                        existingCoreference = link;
                        chainExist = true;
                        break;
                    }

                    lastLink = link;
                    link = link.getNext();
                }

                // CASE 3
                if (lastLink != null && lastLink.getBegin() == originCoreferenceType.getBegin()) {
                    lastLink.setNext(targetCoreferenceType);
                    lastLink.setReferenceRelation(aRelation);
                    chainExist = true;
                    break;
                }
            }

            if (existingCoreference == null) {

                // CASE 1
                if (!chainExist) {
                    CoreferenceChain chain = new CoreferenceChain(aUIData.getjCas());
                    chain.setFirst(originCoreferenceType);
                    originCoreferenceType.setNext(targetCoreferenceType);
                    originCoreferenceType.setReferenceRelation(aRelation);
                    chain.addToIndexes();
                }
            }
            // CASE 4: only change the relation type, everything same!!!
            else if (modify) {
                existingCoreference.setReferenceRelation(aRelation);
                existingCoreference.addToIndexes();
            }
        }
        // clean unconnected coreference chains
        List<CoreferenceChain> orphanChains = new ArrayList<CoreferenceChain>();
        for (CoreferenceChain chain : select(aUIData.getjCas(), CoreferenceChain.class)) {
            if (chain.getFirst().getNext() == null) {
                orphanChains.add(chain);
            }
        }
        for (CoreferenceChain chain : orphanChains) {
            chain.removeFromIndexes();
        }
    }

    private static boolean mergeChain(JCas aJcas, CoreferenceLink aOrigin, CoreferenceLink aTarget,
            String aRelation)
    {
        boolean inThisChain = false;
        boolean inThatChain = false;
        CoreferenceChain thatChain = null;
        CoreferenceChain thisChain = null;
        for (CoreferenceChain chain : select(aJcas, CoreferenceChain.class)) {
            CoreferenceLink link = chain.getFirst();
            boolean tempInThisChain = false;
            if (link.getNext() != null) { // as each corefType are a chain by creation,
                // it should have a next link to be considered as a chain
                while (link != null) {
                    if (inThisChain) {
                        thatChain = chain;
                        if (isAt(link, aOrigin)) {
                            inThatChain = true;
                            link = link.getNext();

                        }
                        else if (isAt(link, aTarget)) {
                            inThatChain = true;
                            link = link.getNext();

                        }
                        else {
                            link = link.getNext();
                        }
                    }
                    else {
                        thisChain = chain;
                        if (isAt(link, aOrigin)) {
                            tempInThisChain = true;
                            link = link.getNext();
                        }
                        else if (isAt(link, aTarget)) {
                            tempInThisChain = true;
                            link = link.getNext();
                        }
                        else {
                            link = link.getNext();
                        }
                    }
                }
            }
            if (tempInThisChain) {
                inThisChain = true;
            }
        }
        if (inThatChain) {
            thatChain.getFirst();
            thisChain.getFirst();
            // |----------|
            // |---------------|
            /*
             */// |----------------------------|
               // |------------|
               // OR
               // |-------|
               // |-------| ...
               // else{
            Map<Integer, CoreferenceLink> beginRelationMaps = new TreeMap<Integer, CoreferenceLink>();
            for (CoreferenceLink link : thisChain.links()) {
                beginRelationMaps.put(link.getBegin(), link);
            }
            for (CoreferenceLink link : thatChain.links()) {
                beginRelationMaps.put(link.getBegin(), link);
            }
            aOrigin.setReferenceRelation(aRelation);
            beginRelationMaps.put(aOrigin.getBegin(), aOrigin);// update the relation

            Iterator<Integer> it = beginRelationMaps.keySet().iterator();

            CoreferenceChain newChain = new CoreferenceChain(aJcas);
            newChain.setFirst(beginRelationMaps.get(it.next()));
            CoreferenceLink newLink = newChain.getFirst();

            while (it.hasNext()) {
                CoreferenceLink link = beginRelationMaps.get(it.next());
                link.setNext(null);
                newLink.setNext(link);
                newLink.setReferenceRelation(newLink.getReferenceRelation() == null ? aRelation
                        : newLink.getReferenceRelation());
                newLink = newLink.getNext();
            }

            newChain.addToIndexes();
            thisChain.removeFromIndexes();
            thatChain.removeFromIndexes();
        }
        return inThatChain;
    }

    public static boolean isAt(Annotation a, Annotation b)
    {
        return a.getBegin() == b.getBegin() && a.getEnd() == b.getEnd();
    }

    public static boolean isAt(Annotation a, int begin, int end)
    {
        return a.getBegin() == begin && a.getEnd() == end;
    }

    public static void deleteCoreferenceType(JCas aJcas, int ref, String aTyep, int aStart,
            int aEnd)
    {
        CoreferenceLink linktoRemove = selectAnnotationByAddress(aJcas, CoreferenceLink.class, ref);
        CoreferenceLink prevLink = null;
        new ArrayList<CoreferenceChain>();
        for (CoreferenceChain chain : select(aJcas, CoreferenceChain.class)) {
            CoreferenceLink link = chain.getFirst();
            boolean found = false;
            while (link != null) {
                if (link.getBegin() == linktoRemove.getBegin()) {
                    found = true;
                    // Break previous chain pointing to this

                    if (prevLink != null) {
                        prevLink.setNext(null);
                    }
                    // If exist pursue the chain
                    if (link.getNext() != null) {
                        CoreferenceChain newChain = new CoreferenceChain(aJcas);
                        newChain.setFirst(link.getNext());
                        newChain.addToIndexes();
                    }
                    link.setNext(null);
                    break;
                }
                prevLink = link;
                link = link.getNext();
            }
            if (found) {
                break;
            }
        }
        linktoRemove.removeFromIndexes();
        removeInvalidChain(aJcas);
    }

    public static void deleteCoreference(BratAnnotatorModel aBratAnnotatorModel, String aType,
            BratAnnotatorUIData aUIData)
    {

        CoreferenceChain newChain = new CoreferenceChain(aUIData.getjCas());
        boolean found = false;

		CoreferenceLink originCorefType = selectAnnotationByAddress(aUIData.getjCas(),
				CoreferenceLink.class, aUIData.getOrigin());
        for (CoreferenceChain chain : select(aUIData.getjCas(), CoreferenceChain.class)) {
            CoreferenceLink link = chain.getFirst();

            if (found) {
                break;
            }
            while (link != null && !found) {
                if (link.getBegin() == originCorefType.getBegin()) {
                    newChain.setFirst(link.getNext());
                    link.setNext(null);
                    found = true;
                    break;
                }
                link = link.getNext();
            }
        }
        newChain.addToIndexes();

        removeInvalidChain(aUIData.getjCas());

    }

    private static void removeInvalidChain(JCas aJcas)
    {
        // clean unconnected coreference chains
        List<CoreferenceChain> orphanChains = new ArrayList<CoreferenceChain>();
        for (CoreferenceChain chain : select(aJcas, CoreferenceChain.class)) {
            if (chain.getFirst().getNext() == null) {
                orphanChains.add(chain);
            }
        }
        for (CoreferenceChain chain : orphanChains) {
            chain.removeFromIndexes();
        }
    }

    public static <T extends FeatureStructure> T selectAnnotationByAddress(JCas aJCas, Class<T> aType,
            int aAddress)
    {
        return aType.cast(aJCas.getLowLevelCas().ll_getFSForRef(aAddress));
    }

    /**
     * stores, for every tokens, the start and end positions, offsets
     *
     * @param aJcas
     * @return map of tokens begin and end positions
     */
    private static Map<Integer, Integer> offsets(JCas aJcas)
    {
        Map<Integer, Integer> offsets = new HashMap<Integer, Integer>();
        for (Token token : select(aJcas, Token.class)) {
            offsets.put(token.getBegin(), token.getEnd());
        }
        return offsets;
    }

    /**
     * delete a span annotation from the response
     *
     * @param aResponse
     * @param id
     */

    private static int[] getTokenStart(Map<Integer, Integer> aOffset, int aStart, int aEnd)
    {
        Iterator<Integer> it = aOffset.keySet().iterator();
        boolean startFound = false;
        boolean endFound = false;
        while (it.hasNext()) {
            int tokenStart = it.next();
            if (aStart >= tokenStart && aStart <= aOffset.get(tokenStart)) {
                aStart = tokenStart;
                startFound = true;
                if (endFound) {
                    break;
                }
            }
            if (aEnd >= tokenStart && aEnd <= aOffset.get(tokenStart)) {
                aEnd = aOffset.get(tokenStart);
                endFound = true;
                if (startFound) {
                    break;
                }
            }
        }
        return new int[] { aStart, aEnd };
    }

    /**
     * Get the beginning position of a token. This is used for dependency annotations
     */
    private static Map<Integer, Integer> getTokenPosition(JCas aJcas)
    {
        Map<Integer, Integer> tokenBeginPositions = new HashMap<Integer, Integer>();

        for (Token token : select(aJcas, Token.class)) {

            if (token.getPos() != null) {
                tokenBeginPositions.put(token.getPos().getAddress(), token.getBegin());
            }
        }
        return tokenBeginPositions;
    }

    /**
     * Get the token at a given position. This is used for dependency and coreference annotations
     */
    public static Map<Integer, Token> getToken(JCas aJcas)
    {
        Map<Integer, Token> tokens = new HashMap<Integer, Token>();
        for (Token token : select(aJcas, Token.class)) {
            tokens.put(token.getBegin(), token);
        }
        return tokens;
    }

    /**
     * Get the beginning offset of an Annotation
     *
     * @param aJCas
     *            The CAS object
     * @param aRef
     *            the low level address of the annotation in the CAS
     * @return the beginning offset address of an annotation
     */
    // private static <T extends Annotation> T selectFirstAt(JCas aJcas, final Class<T> type
    public static int getAnnotationBeginOffset(JCas aJCas, int aRef)
    {
    	return selectAnnotationByAddress(aJCas, Annotation.class, aRef).getBegin();
    }

    /**
     * Get end offset of an annotation
     *
     * @param aJCas
     *            The CAS object
     * @param aRef
     *            the low level address of the annotation in the CAS
     * @return end position of the annotation
     */
    public static int getAnnotationEndOffset(JCas aJCas, int aRef)
    {
    	return selectAnnotationByAddress(aJCas, Annotation.class, aRef).getEnd();
    }

    /**
     * Get the internal address of the first sentence annotation from JCAS. This will be used as a
     * reference for moving forward/backward sentences positions
     *
     * @param aJcas
     *            The CAS object assumed to contains some sentence annotations
     * @return the sentence number or -1 if aJcas don't have sentence annotation
     */
    public static int getFirstSenetnceAddress(JCas aJcas)
    {
        int firstSentenceAddress = -1;

        for (Sentence selectedSentence : select(aJcas, Sentence.class)) {
            firstSentenceAddress = selectedSentence.getAddress();
            break;
        }
        return firstSentenceAddress;
    }

    public static int getLastSenetnceAddress(JCas aJcas)
    {
        int lastSentenceAddress = -1;

        for (Sentence selectedSentence : select(aJcas, Sentence.class)) {
            lastSentenceAddress = selectedSentence.getAddress();
        }
        return lastSentenceAddress;
    }

    public static int getLastSentenceAddressInDisplayWindow(JCas aJcas, int aRef, int aWindowSize)
    {
        int i = aRef;
        int lastSentenceAddress = getLastSenetnceAddress(aJcas);
        int count = 0;

        while (count <= aWindowSize) {
            i = getFollowingSentenceAddress(aJcas, i);
            if (i >= lastSentenceAddress) {
                return i;
            }
            count++;
        }
        return i;
    }

    /**
     * Get the beginning address of a sentence to be displayed in BRAT.
     *
     * @param aJcas
     *            the CAS object
     * @param aSentenceAddress
     *            the old sentence address
     * @param aOffSet
     *            the actual offset of the sentence.
     * @return
     */
    public static int getSentenceBeginAddress(JCas aJcas, int aSentenceAddress, int aOffSet,
            Project aProject, SourceDocument aDocument, int aWindowSize)
    {
        int i = aSentenceAddress;
        int count = 0;
        while (count <= aWindowSize) {
            count++;
            Sentence sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(i);
            if (sentence.getBegin() <= aOffSet && aOffSet <= sentence.getEnd()) {
                break;
            }
            List<Sentence> precedingSentences = selectFollowing(aJcas, Sentence.class, sentence, 1);
            if (precedingSentences.size() > 0) {
                i = precedingSentences.get(0).getAddress();
            }
        }

        Sentence sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(i);
        List<Sentence> precedingSentences = selectPreceding(aJcas, Sentence.class, sentence,
                aWindowSize / 2);

        if (precedingSentences.size() > 0) {
            return precedingSentences.get(0).getAddress();
        }
        // Selection is on the first sentence
        else {
            return sentence.getAddress();
        }
    }

    /**
     * Move to the next page of size display window.
     *
     * @param aCurrenSentenceBeginAddress
     *            The beginning sentence address of the current window.
     * @return the Beginning address of the next window
     */

    public static int getNextDisplayWindowSentenceBeginAddress(JCas aJcas,
            int aCurrenSentenceBeginAddress, int aWindowSize)
    {
        List<Integer> beginningAddresses = getDisplayWindowBeginningSentenceAddresses(aJcas,
                aWindowSize);

        int beginningAddress = aCurrenSentenceBeginAddress;
        for (int i = 0; i < beginningAddresses.size(); i++) {

            if (i == beginningAddresses.size() - 1) {
                beginningAddress = beginningAddresses.get(i);
                break;
            }
            if (beginningAddresses.get(i) == aCurrenSentenceBeginAddress) {
                beginningAddress = beginningAddresses.get(i + 1);
                break;
            }

            if ((beginningAddresses.get(i) < aCurrenSentenceBeginAddress && beginningAddresses
                    .get(i + 1) > aCurrenSentenceBeginAddress)) {
                beginningAddress = beginningAddresses.get(i + 1);
                break;
            }
        }
        return beginningAddress;

    }

    /**
     * Return the beginning position of the Sentence for the previous display window
     *
     * @param aCurrenSentenceBeginAddress
     *            The beginning address of the current sentence of the display window
     * @return
     */
    public static int getPreviousDisplayWindowSentenceBeginAddress(JCas aJcas,
            int aCurrenSentenceBeginAddress, int aWindowSize)
    {
        List<Integer> beginningAddresses = getDisplayWindowBeginningSentenceAddresses(aJcas,
                aWindowSize);

        int beginningAddress = aCurrenSentenceBeginAddress;
        for (int i = 0; i < beginningAddresses.size() - 1; i++) {
            if (i == 0 && aCurrenSentenceBeginAddress >= beginningAddresses.get(i)
                    && beginningAddresses.get(i + 1) >= aCurrenSentenceBeginAddress) {
                beginningAddress = beginningAddresses.get(i);
                break;
            }
            if (aCurrenSentenceBeginAddress >= beginningAddresses.get(i)
                    && beginningAddresses.get(i + 1) >= aCurrenSentenceBeginAddress) {
                beginningAddress = beginningAddresses.get(i);
                break;
            }
            beginningAddress = beginningAddresses.get(i);
        }
        return beginningAddress;
    }

    /**
     * Get the sentence address of the next sentence
     *
     * @param aJcas
     *            The CAS object
     * @param aRef
     *            The address of the current sentence
     * @return address of the next sentence
     */
    public static int getFollowingSentenceAddress(JCas aJcas, int aRef)
    {
        Sentence sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(aRef);
        List<Sentence> followingSentence = selectFollowing(aJcas, Sentence.class, sentence, 1);
        if (followingSentence.size() > 0) {
            return followingSentence.get(0).getAddress();
        }
        else {
            return aRef;
        }
    }

    public static int getLastDisplayWindowFirstSentenceAddress(JCas aJcas, int aWindowSize)
    {
        List<Integer> displayWindowBeginingSentenceAddresses = getDisplayWindowBeginningSentenceAddresses(
                aJcas, aWindowSize);
        return displayWindowBeginingSentenceAddresses.get(displayWindowBeginingSentenceAddresses
                .size() - 1);
    }

    /**
     * Get the total number of sentences
     */
    public static int getNumberOfPages(JCas aJcas, int aWindowSize)
    {
        return select(aJcas, Sentence.class).size();

    }
    /**
     * Returns the beginning address of all pages. This is used properly display<b> Page X of Y </b>
     */
    public static List<Integer> getDisplayWindowBeginningSentenceAddresses(JCas aJcas,
            int aWindowSize)
    {
        List<Integer> beginningAddresses = new ArrayList<Integer>();
        int j = 0;
        for (Sentence sentence : select(aJcas, Sentence.class)) {
            if (j % aWindowSize == 0) {
                beginningAddresses.add(sentence.getAddress());
            }
            j++;
        }
        return beginningAddresses;

    }

    /**
     * Get the ordinal sentence number for this sentence address
     *
     * @return
     */
    public static int getSentenceNumber(JCas aJcas, int aSentenceAddress)
    {
        int sentenceAddress = 0;
        for (Sentence sentence : select(aJcas, Sentence.class)) {
            if (sentence.getAddress() == aSentenceAddress) {
                break;
            }
            sentenceAddress++;
        }
        return sentenceAddress;

    }

    /**
     * Get Sentence address for this ordinal sentence number. Used to go to specific sentence number
     */
    public static int getSentenceAddress(JCas aJcas, int aSentenceNumber)
    {

        int i = 1;
        int address = 0;
        // Negative numbers entered for page number
        if (aSentenceNumber < 1) {
            return -2;
        }
        for (Sentence sentence : select(aJcas, Sentence.class)) {
            if (i == aSentenceNumber) {
                address = sentence.getAddress();
                break;
            }
            address = sentence.getAddress();
            i++;
        }
        // out of sentence boundary
        if (aSentenceNumber > i) {
            return -2;
        }
        return address;

    }

    /**
     * Get CAS object for the first time, from the source document using the provided reader
     */
    public static JCas getJCasFromFile(File aFile, Class aReader)
        throws UIMAException, IOException
    {
        CAS cas = JCasFactory.createJCas().getCas();

        CollectionReader reader = CollectionReaderFactory.createCollectionReader(aReader,
                ResourceCollectionReaderBase.PARAM_PATH, aFile.getParentFile().getAbsolutePath(),
                ResourceCollectionReaderBase.PARAM_PATTERNS,
                new String[] { "[+]" + aFile.getName() });
        if (!reader.hasNext()) {
            throw new FileNotFoundException("Annotation file [" + aFile.getName()
                    + "] not found in [" + aFile.getPath() + "]");
        }
        reader.getNext(cas);
        JCas jCas = cas.getJCas();
        boolean hasTokens = JCasUtil.exists(jCas, Token.class);
        boolean hasSentences = JCasUtil.exists(jCas, Sentence.class);
        if (!hasTokens || !hasSentences) {
			AnalysisEngine pipeline = createPrimitive(createPrimitiveDescription(
					BreakIteratorSegmenter.class,
					BreakIteratorSegmenter.PARAM_CREATE_TOKENS, !hasTokens,
					BreakIteratorSegmenter.PARAM_CREATE_SENTENCES, !hasSentences));
            pipeline.process(cas.getJCas());
        }
        return jCas;
    }

    /**
     * Get the annotation type, using the request sent from brat.
     * If the request have type POS_NN, the the annotation type is POS
     * @param aType the type sent from brat annotation as request while annotating
     */
    public static String getAnnotationType(String aType)
    {
        String annotationType;
        if (Character.isDigit(aType.charAt(0))) {
            annotationType = aType.substring(0, aType.indexOf("_") + 1).replaceAll("[0-9]+", "");
        }
        else {
            annotationType = aType.substring(0, aType.indexOf("_") + 1);
        }
        return annotationType;
    }

    /**
     * Get the actual value of the annotation type (arc or span value)
    * If the request have type POS_NN, the the actual annotation value is NN
     * @param aType the type sent from brat annotation as request while annotating
     * @return
     */
    public static String getType(String aType)
    {
        String type;
        if (Character.isDigit(aType.charAt(0))) {
            type = aType.substring(aType.indexOf(AnnotationTypeConstant.PREFIX) + 1);
        }
        else {
            type = aType.substring(aType.indexOf(AnnotationTypeConstant.PREFIX) + 1);
        }
        return type;
    }
    /**
     * Check if the start/end offsets of an annotation belongs to the same sentence.
     * @return
     */

    public static boolean offsetsInOneSentences(JCas aJcas, int aStartOffset, int aEndOffset){

        for(Sentence sentence:select(aJcas, Sentence.class)){
            if((sentence.getBegin()<=aStartOffset && sentence.getEnd()>aStartOffset) &&
                    aEndOffset<=sentence.getEnd()){
                return true;
            }
        }
        return false;
    }
}
