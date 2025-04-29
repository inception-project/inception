/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.rendering.editorstate;

import java.io.Serializable;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.paging.PagingStrategy;
import de.tudarmstadt.ukp.inception.rendering.paging.Unit;
import de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;

public interface AnnotatorViewState
    extends Serializable
{
    // ---------------------------------------------------------------------------------------------
    // Editor type
    // ---------------------------------------------------------------------------------------------

    String getEditorFactoryId();

    void setEditorFactoryId(String aId);

    // ---------------------------------------------------------------------------------------------
    // Window of visible annotations
    // ---------------------------------------------------------------------------------------------

    PagingStrategy getPagingStrategy();

    void setPagingStrategy(PagingStrategy aPagingStrategy);

    /**
     * @param aUnit
     *            the first unit in the display window.
     */
    void setFirstVisibleUnit(AnnotationFS aUnit);

    void setPageBegin(CAS aCas, int aOffset);

    void setVisibleUnits(List<Unit> aUnit, int aTotalUnitCount);

    List<Unit> getVisibleUnits();

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
    default void moveToPreviousPage(CAS aCas, FocusPosition aPos)
    {
        getPagingStrategy().moveToPreviousPage(this, aCas, aPos);
    }

    default void moveToNextPage(CAS aCas, FocusPosition aPos)
    {
        getPagingStrategy().moveToNextPage(this, aCas, aPos);
    }

    default void moveToFirstPage(CAS aCas, FocusPosition aPos)
    {
        getPagingStrategy().moveToFirstPage(this, aCas, aPos);
    }

    default void moveToLastPage(CAS aCas, FocusPosition aPos)
    {
        getPagingStrategy().moveToLastPage(this, aCas, aPos);
    }

    default void moveToUnit(CAS aCas, int aIndex, FocusPosition aPos)
    {
        getPagingStrategy().moveToUnit(this, aCas, aIndex, aPos);
    }

    default void moveToOffset(CAS aCas, int aOffset, FocusPosition aPos)
    {
        getPagingStrategy().moveToOffset(this, aCas, aOffset, aPos);
    }

    default void moveToSelection(CAS aCas)
    {
        getPagingStrategy().moveToSelection(this, aCas);
    }

    default void moveForward(CAS aCas)
    {
        getPagingStrategy().moveForward(this, aCas);
    }

    // ---------------------------------------------------------------------------------------------
    // Navigation within the application
    // ---------------------------------------------------------------------------------------------
    SourceDocument getDocument();

    Project getProject();

    // ---------------------------------------------------------------------------------------------
    // Selection
    // ---------------------------------------------------------------------------------------------
    Selection getSelection();

    // ---------------------------------------------------------------------------------------------
    // Preferences
    // ---------------------------------------------------------------------------------------------
    AnnotationPreference getPreferences();
}
