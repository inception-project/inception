/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.findWindowStartCenteringOnSelection;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getFirstSentence;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getNextSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public interface AnnotatorViewState
    extends Serializable
{
    // ---------------------------------------------------------------------------------------------
    // Window of visible annotations
    // ---------------------------------------------------------------------------------------------

    // REC: sentenceNumber/sentenceAddress can probably be dropped in favor of
    // firstSentenceNumber/firstSentenceAddress?

    /**
     * @param aUnit
     *            the first unit in the display window.
     */
    void setFirstVisibleUnit(AnnotationFS aUnit);

    /**
     * @param aIndex
     *            the 1-based index of the focus unit
     */
    void setFocusUnitIndex(int aIndex);

    /**
     * @return the 1-based index of the focus unit
     */
    int getFocusUnitIndex();

    /**
     * @return the UIMA address of the first unit in the display window.
     */
    int getFirstVisibleUnitAddress();

    /**
     * @return the begin character offset of the first unit in the display window.
     */
    int getFirstVisibleUnitBegin();

    /**
     * @return the end character offset of the first unit in the display window.
     */
    int getFirstVisibleUnitEnd();

    /**
     * @return the index of the first unit in the display window.
     */
    int getFirstVisibleUnitIndex();

    /**
     * @return the index of the last unit in the display window.
     */
    int getLastVisibleUnitIndex();

    /**
     * @return the total number of units in the document.
     */
    int getUnitCount();

    /**
     * @return the begin character offset of the first unit in the display window.
     */
    int getWindowBeginOffset();

    /**
     * @return the end character offset of the last unit in the display window.
     */
    int getWindowEndOffset();
    
    // ---------------------------------------------------------------------------------------------
    // Rendering
    // - script direction can be changed by the user at will - it defaults to the direction
    // configured in the project
    // ---------------------------------------------------------------------------------------------
    ScriptDirection getScriptDirection();

    void setScriptDirection(ScriptDirection aScriptDirection);

    void toggleScriptDirection();
    
    // ---------------------------------------------------------------------------------------------
    // Navigation within a document
    // ---------------------------------------------------------------------------------------------
    default void moveToPreviousPage(CAS aJCas)
    {
        int firstSentenceAddress = WebAnnoCasUtil.getFirstSentenceAddress(aJCas);

        int previousSentenceAddress = WebAnnoCasUtil.getPreviousDisplayWindowSentenceBeginAddress(
                aJCas, getFirstVisibleUnitAddress(), getPreferences().getWindowSize());
        // Since BratAjaxCasUtil.getPreviousDisplayWindowSentenceBeginAddress returns same
        // address
        // if there are not much sentences to go back to as defined in windowSize
        if (previousSentenceAddress == getFirstVisibleUnitAddress() &&
        // Check whether it's not the beginning of document
                getFirstVisibleUnitAddress() != firstSentenceAddress) {
            previousSentenceAddress = firstSentenceAddress;
        }

        if (getFirstVisibleUnitAddress() == previousSentenceAddress) {
            throw new IllegalStateException("This is First Page!");
        }

        Sentence sentence = selectByAddr(aJCas, Sentence.class, previousSentenceAddress);
        setFirstVisibleUnit(sentence);
        setFocusUnitIndex(WebAnnoCasUtil.getSentenceNumber(aJCas, sentence.getBegin()));
    }

    default void moveToNextPage(CAS aJCas)
    {
        int nextSentenceAddress = WebAnnoCasUtil.getNextPageFirstSentenceAddress(aJCas,
                getFirstVisibleUnitAddress(), getPreferences().getWindowSize());

        if (getFirstVisibleUnitAddress() == nextSentenceAddress) {
            throw new IllegalStateException("This is last page!");
        }

        Sentence sentence = selectByAddr(aJCas, Sentence.class, nextSentenceAddress);
        setFirstVisibleUnit(sentence);
        setFocusUnitIndex(WebAnnoCasUtil.getSentenceNumber(aJCas, sentence.getBegin()));
    }

    default void moveToFirstPage(CAS aJCas)
    {
        int firstSentenceAddress = WebAnnoCasUtil.getFirstSentenceAddress(aJCas);

        if (firstSentenceAddress == getFirstVisibleUnitAddress()) {
            throw new IllegalStateException("This is first page!");
        }

        Sentence sentence = selectByAddr(aJCas, Sentence.class, firstSentenceAddress);
        setFirstVisibleUnit(sentence);
        setFocusUnitIndex(WebAnnoCasUtil.getSentenceNumber(aJCas, sentence.getBegin()));
    }

    default void moveToLastPage(CAS aJCas)
    {
        int lastDisplayWindowBeginingSentenceAddress = WebAnnoCasUtil
                .getLastDisplayWindowFirstSentenceAddress(aJCas, getPreferences().getWindowSize());
        if (lastDisplayWindowBeginingSentenceAddress == getFirstVisibleUnitAddress()) {
            throw new IllegalStateException("This is last page!");
        }

        Sentence sentence = selectByAddr(aJCas, Sentence.class,
                lastDisplayWindowBeginingSentenceAddress);
        setFirstVisibleUnit(sentence);
        setFocusUnitIndex(WebAnnoCasUtil.getSentenceNumber(aJCas, sentence.getBegin()));
    }

    default void moveToUnit(CAS aJCas, int aIndex)
    {
        List<AnnotationFS> units = new ArrayList<>(select(aJCas, getType(aJCas, Sentence.class)));
        
        // Index is 1-based!
        // The code below sets the focus unit index explicitly - see comment on getSentenceNumber
        // in moveToOffset for an explanation. We already know the index here, so no need to
        // calculate it (wrongly) using getSentenceNumber.
        if (aIndex <= 0) {
            moveToOffset(aJCas, units.get(0).getBegin());
            setFocusUnitIndex(1);
        }
        else if (aIndex > units.size()) {
            moveToOffset(aJCas, units.get(units.size() - 1).getBegin());
            setFocusUnitIndex(units.size());
        }
        else {
            moveToOffset(aJCas, units.get(aIndex - 1).getBegin());
            setFocusUnitIndex(aIndex);
        }
    }
    
    default void moveToOffset(CAS aJCas, int aOffset)
    {
        // Fetch the first sentence on screen or first sentence
        AnnotationFS sentence;
        if (getFirstVisibleUnitAddress() > -1) {
            sentence = selectByAddr(aJCas, Sentence.class, getFirstVisibleUnitAddress());
        }
        else {
            sentence = getFirstSentence(aJCas);
        }
        
        // Calculate the first sentence in the window in such a way that the annotation
        // currently selected is in the center of the window
        sentence = findWindowStartCenteringOnSelection(aJCas, sentence, aOffset,
                getProject(), getDocument(), getPreferences().getWindowSize());
        
        // Move to it
        setFirstVisibleUnit(sentence);
        
        // FIXME getSentenceNumber is not a good option... if we aim for the begin offset of the
        // very last unit, then we get (max-units - 1) instead of (max-units). However, this
        // method is used also in curation and I dimly remember that things broke when I tried
        // to fix it. Probably better to move away from it in the long run. -- REC
        setFocusUnitIndex(WebAnnoCasUtil.getSentenceNumber(aJCas, aOffset));
    }

    default void moveToSelection(CAS aJCas)
    {
        moveToOffset(aJCas, getSelection().getBegin());
    }

    default void moveForward(CAS aJCas)
    {
        // Fetch the first sentence on screen
        Sentence sentence = selectByAddr(aJCas, Sentence.class, getFirstVisibleUnitAddress());
        // Find the following one
        int address = getNextSentenceAddress(aJCas, sentence);
        // Move to it
        setFirstVisibleUnit(selectByAddr(aJCas, Sentence.class, address));
    }

    // ---------------------------------------------------------------------------------------------
    // Auxiliary methods
    // ---------------------------------------------------------------------------------------------
    Selection getSelection();
    SourceDocument getDocument();
    Project getProject();
    AnnotationPreference getPreferences();
}
