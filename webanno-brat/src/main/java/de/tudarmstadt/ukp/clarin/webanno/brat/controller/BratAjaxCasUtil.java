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
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Contain Methods for updating CAS Objects directed from brat UI, different utility methods to
 * process the CAS such getting the sentence address, determine page numbers,...
 *
 * @author Seid Muhie Yimam
 *
 */
public class BratAjaxCasUtil
{

    /**
     * Annotation a and annotation b are the same if the have the same address ( used for
     * {@link CoreferenceChain})
     *
     * @param a a FS.
     * @param b a FS.
     * @return if both FSes are the same.
     */
    public static boolean isSame(FeatureStructure a, FeatureStructure b)
    {
        if (a == null || b == null) {
            return false;
        }

        return getAddr(a) == getAddr(b);
    }

    /**
     * Check if the start/end offsets of an annotation belongs to the same sentence.
     * @param aJcas the JCAs.
     * @param aStartOffset the start offset.
     * @param aEndOffset the end offset.
     * @return if start and end offsets are within the same sentence.
     */
    public static boolean isSameSentence(JCas aJcas, int aStartOffset, int aEndOffset)
    {
        for (Sentence sentence : select(aJcas, Sentence.class)) {
            if ((sentence.getBegin() <= aStartOffset && sentence.getEnd() > aStartOffset)
                    && aEndOffset <= sentence.getEnd()) {
                return true;
            }
        }
        return false;
    }

    // public static boolean isSame(Annotation a, Annotation b)
    // {
    // return a.getBegin() == b.getBegin() && a.getEnd() == b.getEnd();
    // }

    // public static boolean isAt(Annotation a, int begin, int end)
    // {
    // return a.getBegin() == begin && a.getEnd() == end;
    // }

    /*
     * public static void deleteCoreference(BratAnnotatorModel aBratAnnotatorModel, String aType,
     * BratAnnotatorUIData aUIData) {
     *
     * CoreferenceChain newChain = new CoreferenceChain(aUIData.getjCas()); boolean found = false;
     *
     * CoreferenceLink originCorefType = selectAnnotationByAddress(aUIData.getjCas(),
     * CoreferenceLink.class, aUIData.getOrigin()); for (CoreferenceChain chain :
     * select(aUIData.getjCas(), CoreferenceChain.class)) { CoreferenceLink link = chain.getFirst();
     *
     * if (found) { break; } while (link != null && !found) { if (link.getBegin() ==
     * originCorefType.getBegin()) { newChain.setFirst(link.getNext()); link.setNext(null); found =
     * true; break; } link = link.getNext(); } } newChain.addToIndexes();
     *
     * // removeInvalidChain(aUIData.getjCas());
     *
     * }
     */

    public static int getAddr(FeatureStructure aFS)
    {
        return ((FeatureStructureImpl) aFS).getAddress();
    }

    public static AnnotationFS selectByAddr(JCas aJCas, int aAddress)
    {
        return selectByAddr(aJCas, AnnotationFS.class, aAddress);
    }

    public static <T extends FeatureStructure> T selectByAddr(JCas aJCas, Class<T> aType,
            int aAddress)
    {
        return aType.cast(aJCas.getLowLevelCas().ll_getFSForRef(aAddress));
    }

    private static <T extends Annotation> T selectSingleAt(JCas aJcas, final Class<T> type,
            int aBegin, int aEnd)
    {
        List<T> covered = selectCovered(aJcas, type, aBegin, aEnd);
        if (covered.isEmpty()) {
            return null;
        }
        else {
            T first = covered.get(0);
            if (first.getBegin() == aBegin && first.getEnd() == aEnd) {
                return first;
            }
            else {
                return null;
            }
        }
    }

    /**
     * Get an annotation using the begin/offsets and its type
     *
     * @param aJcas the JCas.
     * @param aType the type.
     * @param aBegin the begin offset.
     * @param aEnd the end offset.
     * @return the annotation FS.
     */
    public static AnnotationFS selectSingleFsAt(JCas aJcas, Type aType, int aBegin, int aEnd)
    {
        for (AnnotationFS anFS : selectCovered(aJcas.getCas(), aType, aBegin, aEnd)) {
            if (anFS.getBegin() == aBegin && anFS.getEnd() == aEnd) {
                return anFS;
            }
        }
        return null;
    }

    /**
     * Get the sentence for this CAS based on the begin and end offsets. This is basically used to
     * transform sentence address in one CAS to other sentence address for different CAS
     *
     * @param aJcas the JCas.
     * @param aBegin the begin offset.
     * @param aEnd the end offset.
     * @return the sentence.
     */
    public static Sentence selectSentenceAt(JCas aJcas, int aBegin, int aEnd)
    {
        return selectSingleAt(aJcas, Sentence.class, aBegin, aEnd);
    }

    /**
     * Get overlapping annotations where selection overlaps with annotations.<br>
     * Example: if annotation is (5, 13) and selection covered was from (7, 12); the annotation (5,
     * 13) is returned as overlapped selection <br>
     * If multiple annotations are [(3, 8), (9, 15), (16, 21)] and selection covered was from (10,
     * 18), overlapped annotation [(9, 15), (16, 21)] should be returned
     *
     * @param <T>
     *            the JCas type.
     * @param aJCas
     *            a JCas containing the annotation.
     * @param aType
     *            a UIMA type.
     * @param aBegin
     *            begin offset.
     * @param aEnd
     *            end offset.
     * @return a return value.
     */
    public static <T extends Annotation> List<T> selectOverlapping(JCas aJCas,
            final Class<T> aType, int aBegin, int aEnd)
    {

        List<T> annotations = new ArrayList<T>();
        for (T t : select(aJCas, aType)) {
            if (t.getBegin() >= aEnd) {
                break;
            }
            // not yet there
            if (t.getEnd() <= aBegin) {
                continue;
            }
            annotations.add(t);
        }

        return annotations;
    }

    /**
     * Get the internal address of the first sentence annotation from JCAS. This will be used as a
     * reference for moving forward/backward sentences positions
     *
     * @param aJcas
     *            The CAS object assumed to contains some sentence annotations
     * @return the sentence number or -1 if aJcas don't have sentence annotation
     */
    public static int getFirstSentenceAddress(JCas aJcas)
    {
        int firstSentenceAddress = -1;

        for (Sentence selectedSentence : select(aJcas, Sentence.class)) {
            firstSentenceAddress = selectedSentence.getAddress();
            break;
        }
        return firstSentenceAddress;
    }

    public static int getLastSentenceAddress(JCas aJcas)
    {
        int lastSentenceAddress = -1;

        for (Sentence selectedSentence : select(aJcas, Sentence.class)) {
            lastSentenceAddress = selectedSentence.getAddress();
        }
        return lastSentenceAddress;
    }
    /**
     * Get the current sentence based on the anotation begin/end offset
     *
     * @param aJCas the JCas.
     * @param aBegin the begin offset.
     * @param aEnd the end offset.
     * @return the sentence.
     */
    public static Sentence getCurrentSentence(JCas aJCas, int aBegin, int aEnd)
    {
        Sentence currentSentence = null;
        for (Sentence sentence : select(aJCas, Sentence.class)) {
            if (sentence.getBegin() <= aBegin && sentence.getEnd() >= aEnd) {
                currentSentence = sentence;
                break;
            }
        }
        return currentSentence;
    }

    /**
     * Get the last sentence CAS address in the current display window
     *
     * @param aJcas
     *            the JCas.
     * @param aFirstSentenceAddress
     *            the CAS address of the first sentence in the display window
     * @param aWindowSize
     *            the window size
     * @return The address of the last sentence address in the current display window.
     */
    public static int getLastSentenceAddressInDisplayWindow(JCas aJcas, int aFirstSentenceAddress,
            int aWindowSize)
    {
        int count = 0;
        FSIterator<Sentence> si = seekByAddress(aJcas, Sentence.class, aFirstSentenceAddress);
        Sentence s = si.get();
        while (count < aWindowSize) {
            si.moveToNext();
            if (si.isValid()) {
                s = si.get();
            }
            else {
                break;
            }
            count ++;
        }

        return s.getAddress();
    }

    /**
     * Get an iterator position at the annotation with the specified address.
     *
     * @param aJcas
     *            the CAS object
     * @param aType
     *            the expected annotation type
     * @param aAddr
     *            the annotationa address
     * @return the iterator.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T extends Annotation> FSIterator<T> seekByAddress(JCas aJcas, Class<T> aType,
            int aAddr)
    {
        AnnotationIndex<T> idx = (AnnotationIndex) aJcas.getAnnotationIndex(JCasUtil
                .getAnnotationType(aJcas, aType));
        return idx.iterator(selectByAddr(aJcas, aAddr));
    }

    /**
     * Gets the address of the first sentence visible on screen in such a way that the specified
     * focus offset is centered on screen.
     *
     * @param aJcas
     *            the CAS object
     * @param aSentenceAddress
     *            the old sentence address
     * @param aFocosOffset
     *            the actual offset of the sentence.
     * @param aProject
     *            the project.
     * @param aDocument
     *            the document.
     * @param aWindowSize
     *            the window size.
     * @return the ID of the first sentence.
     */
    public static int getSentenceBeginAddress(JCas aJcas, int aSentenceAddress, int aFocosOffset,
            Project aProject, SourceDocument aDocument, int aWindowSize)
    {
        FSIterator<Sentence> si = seekByAddress(aJcas, Sentence.class, aSentenceAddress);

        // Seek the sentence that contains the current focus
        Sentence s = si.get();
        while (si.isValid()) {
            if (s.getEnd() < aFocosOffset) {
                // Focus after current sentence
                si.moveToNext();
            }
            else if (aFocosOffset < s.getBegin()) {
                // Focus before current sentence
                si.moveToPrevious();
            }
            else {
                // Focus must be in current sentence
                break;
            }
            s = si.get();
        }

        // Center sentence
        int count = 0;
        while (si.isValid() && count < (aWindowSize / 2)) {
            si.moveToPrevious();
            if (si.isValid()) {
                s = si.get();
            }
            count++;
        }

        return s.getAddress();
    }

    /**
     * Move to the next page of size display window.
     *
     * @param aJcas
     *            the JCas.
     * @param aCurrenSentenceBeginAddress
     *            The beginning sentence address of the current window.
     * @param aWindowSize
     *            the window size.
     * @return the Beginning address of the next window
     */
    public static int getNextPageFirstSentenceAddress(JCas aJcas,
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
     * @param aJcas the JCas.
     *
     * @param aCurrenSentenceBeginAddress
     *            The beginning address of the current sentence of the display window
     * @param aWindowSize the window size.
     * @return hum?
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

    public static int getLastDisplayWindowFirstSentenceAddress(JCas aJcas, int aWindowSize)
    {
        List<Integer> displayWindowBeginingSentenceAddresses = getDisplayWindowBeginningSentenceAddresses(
                aJcas, aWindowSize);
        return displayWindowBeginingSentenceAddresses.get(displayWindowBeginingSentenceAddresses
                .size() - 1);
    }

    /**
     * Get the total number of sentences
     *
     * @param aJcas the JCas.
     * @return the number of sentences.
     */
    public static int getNumberOfPages(JCas aJcas)
    {
        return select(aJcas, Sentence.class).size();
    }

    /**
     * Returns the beginning address of all pages. This is used properly display<b> Page X of Y </b>
     *
     * @param aJcas the JCas.
     * @param aWindowSize the window size.
     * @return hum?
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
     * Get the ordinal sentence number in the display window. This will be sent to brat so that it
     * will adjust the sentence number to display accordingly
     *
     * @param aJcas the JCas.
     * @param aSentenceAddress the sentence ID.
     * @return the sentence number.
     *
     */
    public static int getFirstSentenceNumber(JCas aJcas, int aSentenceAddress)
    {
        int sentenceNumber = 0;
        for (Sentence sentence : select(aJcas, Sentence.class)) {
            if (sentence.getAddress() == aSentenceAddress) {
                break;
            }
            sentenceNumber++;
        }
        return sentenceNumber;

    }

    /**
     * Get the sentence number at this specific position
     * @param aJcas the JCas.
     * @param aBeginOffset the begin offset.
     * @return the sentence number.
     */
    public static int getSentenceNumber(JCas aJcas, int aBeginOffset)
    {
        int sentenceNumber = 0;
        for (Sentence sentence : select(aJcas, Sentence.class)) {
            if (sentence.getBegin() <= aBeginOffset && aBeginOffset <= sentence.getEnd()) {
                sentenceNumber++;
                break;
            }
            sentenceNumber++;
        }
        return sentenceNumber;
    }

    /**
     * Get Sentence address for this ordinal sentence number. Used to go to specific sentence number
     * @param aJcas the JCas.
     * @param aSentenceNumber the sentence number.
     * @return the ID.
     */
    public static int getSentenceAddress(JCas aJcas, int aSentenceNumber)
    {
        int i = 1;
        int address = 0;
        if (aSentenceNumber < 1) {
            return 0;
        }
        for (Sentence sentence : select(aJcas, Sentence.class)) {
            if (i == aSentenceNumber) {
                address = sentence.getAddress();
                break;
            }
            address = sentence.getAddress();
            i++;
        }
        if (aSentenceNumber > i) {
            return 0;
        }
        return address;
    }

    /**
     * For a span annotation, if a sub-token is selected, display the whole text so that the user is
     * aware of what is being annotated, based on
     * {@link BratAjaxCasUtil#selectOverlapping(JCas, Class, int, int)} ISSUE - Affected text not
     * correctly displayed in annotation dialog (Bug #272)
     *
     * @param aJcas the JCas.
     * @param aBeginOffset the begin offset.
     * @param aEndOffset the end offset.
     * @return the selected text.
     */
    public static String getSelectedText(JCas aJcas, int aBeginOffset, int aEndOffset)
    {
        List<Token> tokens = BratAjaxCasUtil.selectOverlapping(aJcas, Token.class, aBeginOffset,
                aEndOffset);
        StringBuilder seletedTextSb = new StringBuilder();
        for (Token token : tokens) {
            seletedTextSb.append(token.getCoveredText() + " ");
        }
        return seletedTextSb.toString();
    }

    public static <T> T getFeature(FeatureStructure aFS, AnnotationFeature aFeature)
    {
        Feature feature = aFS.getType().getFeatureByBaseName(aFeature.getName());

        // Sanity check
        if (!ObjectUtils.equals(aFeature.getType(), feature.getRange().getName())) {
            throw new IllegalArgumentException("Actual feature type ["
                    + feature.getRange().getName() + "]does not match expected feature type ["
                    + aFeature.getType() + "].");
        }

        switch (aFeature.getType()) {
        case CAS.TYPE_NAME_STRING:
            return (T) aFS.getStringValue(feature);
        case CAS.TYPE_NAME_BOOLEAN:
            return (T) (Boolean) aFS.getBooleanValue(feature);
        case CAS.TYPE_NAME_FLOAT:
            return (T) (Float) aFS.getFloatValue(feature);
        case CAS.TYPE_NAME_INTEGER:
            return (T) (Integer) aFS.getIntValue(feature);
        default:
            throw new IllegalArgumentException("Cannot get value of feature [" + aFeature.getName()
                    + "] with type [" + feature.getRange().getName() + "]");
        }
    }

    /**
     * Set a feature value.
     *
     * @param aFS
     *            the feature structure.
     * @param aFeature
     *            the feature within the annotation whose value to set. If this parameter is
     *            {@code null} then nothing happens.
     * @param aValue
     *            the feature value.
     */
    public static void setFeature(FeatureStructure aFS, AnnotationFeature aFeature, Object aValue)
    {
        if (aFeature == null) {
            return;
        }

        Feature feature = aFS.getType().getFeatureByBaseName(aFeature.getName());

        // Sanity check
        if (ObjectUtils.equals(aFeature.getType(), feature.getRange().getName())) {
            throw new IllegalArgumentException("Actual feature type ["
                    + feature.getRange().getName() + "]does not match expected feature type ["
                    + aFeature.getType() + "].");
        }

        switch (aFeature.getType()) {
        case CAS.TYPE_NAME_STRING:
            aFS.setStringValue(feature, (String) aValue);
            break;
        case CAS.TYPE_NAME_BOOLEAN:
            aFS.setBooleanValue(feature, aValue != null ? (boolean) aValue : false);
            break;
        case CAS.TYPE_NAME_FLOAT:
            aFS.setFloatValue(feature, aValue != null ? (float) aValue : 0.0f);
            break;
        case CAS.TYPE_NAME_INTEGER:
            aFS.setIntValue(feature, aValue != null ? (int) aValue : 0);
            break;
        default:
            throw new IllegalArgumentException("Cannot set value of feature [" + aFeature.getName()
                    + "] with type [" + feature.getRange().getName() + "] to [" + aValue + "]");
        }
    }

    /**
     * Set a feature value.
     *
     * @param aFS
     *            the feature structure.
     * @param aFeatureName
     *            the feature within the annotation whose value to set.
     * @param aValue
     *            the feature value.
     */
    public static void setFeatureFS(FeatureStructure aFS, String aFeatureName,
            FeatureStructure aValue)
    {
        Feature labelFeature = aFS.getType().getFeatureByBaseName(aFeatureName);
        aFS.setFeatureValue(labelFeature, aValue);
    }

    /**
     * Get a feature value.
     *
     * @param aFS
     *            the feature structure.
     * @param aFeatureName
     *            the feature within the annotation whose value to set.
     * @return the feature value.
     */
    public static FeatureStructure getFeatureFS(FeatureStructure aFS, String aFeatureName)
    {
        return aFS.getFeatureValue(aFS.getType().getFeatureByBaseName(aFeatureName));
    }
}
