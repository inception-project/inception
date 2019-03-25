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

import java.io.Serializable;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.SentencePagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

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
     *
     * @deprecated try using locating the first unit via {@link #getWindowBeginOffset()} instead and
     * then fetch its begin offset.
     */
    @Deprecated
    int getFirstVisibleUnitAddress();

    /**
     * @return the begin character offset of the first unit in the display window.
     * 
     * @deprecated try using locating the first unit via {@link #getWindowBeginOffset()} instead and
     * then fetch its begin offset.
     */
    @Deprecated
    int getFirstVisibleUnitBegin();

    /**
     * @return the end character offset of the first unit in the display window.
     *
     * @deprecated try using locating the first unit via {@link #getWindowBeginOffset()} instead and
     * then fetch its end offset.
     */
    @Deprecated
    int getFirstVisibleUnitEnd();

    /**
     * @return the index of the first unit in the display window.
     */
    int getFirstVisibleUnitIndex();

    /**
     * @return the index of the last unit in the display window.
     * 
     * @deprecated try locating the last visible using using {@link #getWindowEndOffset()} instead
     */
    @Deprecated
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
    default void moveToPreviousPage(CAS aCas)
    {
        new SentencePagingStrategy().moveToPreviousPage(this, aCas);
    }

    default void moveToNextPage(CAS aCas)
    {
        new SentencePagingStrategy().moveToNextPage(this, aCas);
    }

    default void moveToFirstPage(CAS aCas)
    {
        new SentencePagingStrategy().moveToFirstPage(this, aCas);
    }

    default void moveToLastPage(CAS aCas)
    {
        new SentencePagingStrategy().moveToLastPage(this, aCas);
    }

    default void moveToUnit(CAS aCas, int aIndex)
    {
        new SentencePagingStrategy().moveToUnit(this, aCas, aIndex);
    }
    
    default void moveToOffset(CAS aCas, int aOffset)
    {
        new SentencePagingStrategy().moveToOffset(this, aCas, aOffset);
    }

    default void moveToSelection(CAS aCas)
    {
        new SentencePagingStrategy().moveToSelection(this, aCas);
    }

    default void moveForward(CAS aCas)
    {
        new SentencePagingStrategy().moveForward(this, aCas);
    }

    // ---------------------------------------------------------------------------------------------
    // Auxiliary methods
    // ---------------------------------------------------------------------------------------------
    Selection getSelection();
    SourceDocument getDocument();
    Project getProject();
    AnnotationPreference getPreferences();
}
