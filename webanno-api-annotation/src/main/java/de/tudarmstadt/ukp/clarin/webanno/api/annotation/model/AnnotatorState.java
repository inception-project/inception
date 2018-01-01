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
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Covers information about the state of the annotation editor component that is relevant across
 * cycles.
 */
public interface AnnotatorState
    extends Serializable
{
    void reset();

    // ---------------------------------------------------------------------------------------------
    // Window of visible annotations
    // ---------------------------------------------------------------------------------------------

    // REC: sentenceNumber/sentenceAddress can probably be dropped in favor of
    // firstSentenceNumber/firstSentenceAddress?

    /**
     * @param aUnit the first unit in the display window.
     */
    void setFirstVisibleUnit(Sentence aUnit);

    /**
     * @param aIndex the 1-based index of the focus unit
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
    // Annotation behavior
    //
    // Control which kinds of annotations are created when an annotation creation action is
    // triggered and also what happens after the annotation has been created (e.g. auto-forward)
    // ---------------------------------------------------------------------------------------------
    boolean isForwardAnnotation();

    void setForwardAnnotation(boolean forwardAnnotation);

    AnnotationLayer getSelectedAnnotationLayer();

    void setSelectedAnnotationLayer(AnnotationLayer selectedAnnotationLayer);

    AnnotationLayer getDefaultAnnotationLayer();

    void setDefaultAnnotationLayer(AnnotationLayer defaultAnnotationLayer);

    // REC: would be very nice if we didn't need the mode - the behaviors specific to annotation,
    // curation, automation, correction, etc. should be local to the respective modules / pages
    Mode getMode();

    // ---------------------------------------------------------------------------------------------
    // Remembered feature values
    //
    // These are optionally used when a new annotation is created to pre-fill feature values using
    // those of the last annotation of the same type. This can be useful when many annotations of
    // the same type with similar feature values need to be created.
    // ---------------------------------------------------------------------------------------------
    void rememberFeatures();

    AnnotationLayer getRememberedSpanLayer();

    AnnotationLayer getRememberedArcLayer();

    Map<AnnotationFeature, Serializable> getRememberedSpanFeatures();

    Map<AnnotationFeature, Serializable> getRememberedArcFeatures();

    void clearRememberedFeatures();

    // ---------------------------------------------------------------------------------------------
    // User
    // ---------------------------------------------------------------------------------------------

    // REC not sure if we need these really... we can fetch the user from the security context.
    // Might be interesting to have if we allow an admin to open another users annotation though.
    User getUser();

    void setUser(User aUser);

    // ---------------------------------------------------------------------------------------------
    // Document
    // ---------------------------------------------------------------------------------------------
    SourceDocument getDocument();

    void setDocument(SourceDocument aDocument, List<SourceDocument> aDocuments);

    int getDocumentIndex();

    int getNumberOfDocuments();

    // ---------------------------------------------------------------------------------------------
    // Project
    // ---------------------------------------------------------------------------------------------
    Project getProject();

    void setProject(Project aProject);
    
    /**
     * Set whether the user should be allowed to switch projects in the annotation editor
     * "open documents" dialog.
     */
    void setProjectLocked(boolean aFlag);
    
    boolean isProjectLocked();

    // REC: we cache the constraints when a document is opened because parsing them takes some time
    ParsedConstraints getConstraints();

    void setConstraints(ParsedConstraints aConstraints);

    // ---------------------------------------------------------------------------------------------
    // Selection
    // ---------------------------------------------------------------------------------------------
    Selection getSelection();

    void setArmedSlot(AnnotationFeature aName, int aIndex);

    boolean isArmedSlot(AnnotationFeature aName, int aIndex);

    void clearArmedSlot();

    boolean isSlotArmed();

    AnnotationFeature getArmedFeature();

    int getArmedSlot();

    // ---------------------------------------------------------------------------------------------
    // Rendering
    // - script direction can be changed by the user at will - it defaults to the direction
    // configured in the project
    // ---------------------------------------------------------------------------------------------
    ScriptDirection getScriptDirection();

    void setScriptDirection(ScriptDirection aScriptDirection);

    void toggleScriptDirection();

    // ---------------------------------------------------------------------------------------------
    // User preferences
    // ---------------------------------------------------------------------------------------------
    AnnotationPreference getPreferences();

    void setPreferences(AnnotationPreference aPreferences);

    List<AnnotationLayer> getAnnotationLayers();

    void setAnnotationLayers(List<AnnotationLayer> aAnnotationLayers);

    // ---------------------------------------------------------------------------------------------
    // Feature value models
    // ---------------------------------------------------------------------------------------------
    List<FeatureState> getFeatureStates();

    FeatureState getFeatureState(AnnotationFeature aFeature);

    // ---------------------------------------------------------------------------------------------
    // Access to transient context
    // ---------------------------------------------------------------------------------------------
    TransientActionContext getAction();

    // ---------------------------------------------------------------------------------------------
    // Navigation within or across a document
    // ---------------------------------------------------------------------------------------------
    default void moveToPreviousDocument(List<SourceDocument> aDocuments)
    {
        // Index of the current source document in the list
        int currentDocumentIndex = aDocuments.indexOf(getDocument());

        // If the first the document
        if (currentDocumentIndex <= 0) {
            throw new IllegalStateException("This is the first document!");
        }

        setDocument(aDocuments.get(currentDocumentIndex - 1), aDocuments);
    }

    default void moveToNextDocument(List<SourceDocument> aDocuments)
    {
        // Index of the current source document in the list
        int currentDocumentIndex = aDocuments.indexOf(getDocument());

        // If the last document
        if (currentDocumentIndex >= aDocuments.size() - 1) {
            throw new IllegalStateException("This is the last document!");
        }

        setDocument(aDocuments.get(currentDocumentIndex + 1), aDocuments);
    }

    default void moveToPreviousPage(JCas aJCas)
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

    default void moveToNextPage(JCas aJCas)
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

    default void moveToFirstPage(JCas aJCas)
    {
        int firstSentenceAddress = WebAnnoCasUtil.getFirstSentenceAddress(aJCas);

        if (firstSentenceAddress == getFirstVisibleUnitAddress()) {
            throw new IllegalStateException("This is first page!");
        }

        Sentence sentence = selectByAddr(aJCas, Sentence.class, firstSentenceAddress);
        setFirstVisibleUnit(sentence);
        setFocusUnitIndex(WebAnnoCasUtil.getSentenceNumber(aJCas, sentence.getBegin()));
    }

    default void moveToLastPage(JCas aJCas)
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

    default void moveToUnit(JCas aJCas, int aIndex)
    {
        List<Sentence> units = new ArrayList<>(select(aJCas, Sentence.class));
        
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
    
    default void moveToOffset(JCas aJCas, int aOffset)
    {
        // Fetch the first sentence on screen or first sentence
        Sentence sentence;
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

    default void moveToSelection(JCas aJCas)
    {
        moveToOffset(aJCas, getSelection().getBegin());
    }

    default void moveForward(JCas aJCas)
    {
        // Fetch the first sentence on screen
        Sentence sentence = selectByAddr(aJCas, Sentence.class, getFirstVisibleUnitAddress());
        // Find the following one
        int address = getNextSentenceAddress(aJCas, sentence);
        // Move to it
        setFirstVisibleUnit(selectByAddr(aJCas, Sentence.class, address));
    }
}
