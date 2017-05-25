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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;

import java.io.Serializable;
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
    void clearAllSelections();

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
     * @param aIndex the index of the focus unit (curation view)
     */
    void setFocusUnitIndex(int aIndex);

    /**
     * @return the index of the focus unit (curation view)
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
}
