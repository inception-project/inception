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
package de.tudarmstadt.ukp.inception.rendering.paging;

import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.CENTERED;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorViewState;
import de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;

public interface PagingStrategy
    extends Serializable
{
    /**
     * @param aCas
     *            CAS currently being edited
     * @param aFirstIndex
     *            index of the first unit to be returned
     * @param aLastIndex
     *            index of the last unit to be returned
     * @return the units in a given index range (i.e. nth-unit, not character offsets). The index
     *         range is 1-based. The returned list is automatically capped to the maximum number of
     *         units available. Thus it is possible to use {@code units(0, Integer.MAX_VALUE)} to
     *         return all units.
     */
    List<Unit> units(CAS aCas, int aFirstIndex, int aLastIndex);

    Component createPositionLabel(String aId, IModel<AnnotatorState> aModel);

    Component createPageNavigator(String aId, Page aPage);

    default void moveToOffset(AnnotatorViewState aState, CAS aCas, int aOffset, FocusPosition aPos)
    {
        moveToOffset(aState, aCas, aOffset, null, aPos);
    }

    void moveToOffset(AnnotatorViewState aState, CAS aCas, int aOffset, VRange aPingRange,
            FocusPosition aPos);

    default void recalculatePage(AnnotatorViewState aState, CAS aCas)
    {
        aState.setPageBegin(aCas, aState.getWindowBeginOffset());
    }

    default List<Unit> units(CAS aCas)
    {
        return units(aCas, 0, Integer.MAX_VALUE);
    }

    /**
     * @param aCas
     *            CAS currently being edited
     * @return the total number of units.
     */
    default int unitCount(CAS aCas)
    {
        return units(aCas).size();
    }

    /**
     * @param aCas
     *            CAS currently being edited
     * @param aIndex
     *            index of the unit to be returned
     * @return the unit with the given index. The index is 1-based. If the index is smaller than 1,
     *         the first unit is returned. If the index is greater than the number of units
     *         available, the last unit is returned.
     */
    default Unit unitAtIndex(CAS aCas, int aIndex)
    {
        int index = aIndex;

        if (index < 1) {
            index = 1;
        }

        var units = units(aCas);

        if (units.isEmpty()) {
            return null;
        }

        if (index > units.size()) {
            index = units.size();
        }

        return units.get(index - 1);
    }

    default List<Unit> unitsStartingAtOffset(CAS aCas, int aOffset, int aCount)
    {
        return units(aCas).stream() //
                .filter(unit -> unit.getBegin() >= aOffset
                        || unit.getBegin() <= aOffset && aOffset < unit.getEnd()) //
                .limit(aCount) //
                .collect(toList());
    }

    /**
     * @param aState
     *            annotator state
     * @param aCas
     *            CAS currently being edited
     * @param aIndex
     *            index of the unit to move to (1-based)
     * @param aPos
     *            focus positioning mode
     */
    default void moveToUnit(AnnotatorViewState aState, CAS aCas, int aIndex, FocusPosition aPos)
    {
        var unit = unitAtIndex(aCas, aIndex);
        if (unit != null) {
            moveToOffset(aState, aCas, unit.getBegin(), aPos);
        }
    }

    default void moveToPreviousPage(AnnotatorViewState aState, CAS aCas, FocusPosition aPos)
    {
        moveToUnit(aState, aCas,
                aState.getFirstVisibleUnitIndex() - aState.getPreferences().getWindowSize(), aPos);
    }

    default void moveToNextPage(AnnotatorViewState aState, CAS aCas, FocusPosition aPos)
    {
        moveToUnit(aState, aCas,
                aState.getFirstVisibleUnitIndex() + aState.getPreferences().getWindowSize(), aPos);
    }

    default void moveToFirstPage(AnnotatorViewState aState, CAS aCas, FocusPosition aPos)
    {
        moveToUnit(aState, aCas, 1, aPos);
    }

    default void moveToLastPage(AnnotatorViewState aState, CAS aCas, FocusPosition aPos)
    {
        moveToUnit(aState, aCas,
                aState.getUnitCount() - aState.getPreferences().getWindowSize() + 1, aPos);
    }

    default void moveToSelection(AnnotatorViewState aState, CAS aCas)
    {
        moveToOffset(aState, aCas, aState.getSelection().getBegin(), CENTERED);
    }

    default void moveForward(AnnotatorViewState aState, CAS aCas)
    {
        moveToUnit(aState, aCas, aState.getFirstVisibleUnitIndex() + 1, CENTERED);
    }
}
