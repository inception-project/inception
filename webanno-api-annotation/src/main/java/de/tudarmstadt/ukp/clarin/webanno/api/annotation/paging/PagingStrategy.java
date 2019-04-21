/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.FocusPosition.CENTERED;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorViewState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;

public interface PagingStrategy
    extends Serializable
{
    /**
     * Returns the units in a given index range (i.e. nth-unit, not character offsets). The index
     * range is 1-based. The returned list is automatically capped to the maximum number of units
     * available. Thus it is possible to use {@code units(0, Integer.MAX_VALUE)} to return all
     * units.
     */
    List<Unit> units(CAS aCas, int aFirstIndex, int aLastIndex);
    
    Component createPositionLabel(String aId, IModel<AnnotatorState> aModel);
    
    DefaultPagingNavigator createPageNavigator(String aId, AnnotationPageBase aPage);

    default void moveToOffset(AnnotatorViewState aState, CAS aCas, int aOffset, FocusPosition aPos)
    {
        switch (aPos) {
        case TOP: {
            aState.setPageBegin(aCas, aOffset);
            break;
        }
        case CENTERED: {
            List<Unit> units = units(aCas);
            
            // Find the unit containing the given offset
            Unit unit = units.stream()
                    .filter(u -> u.getBegin() <= aOffset && aOffset <= u.getEnd())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No unit contains character offset [" + aOffset + "]") );

            // How many rows to display before the unit such that the unit is centered?
            int rowsInPageBeforeUnit = aState.getPreferences().getWindowSize() / 2;
            // The -1 below is because unit.getIndex() is 1-based
            Unit firstUnit = units.get(Math.max(0, unit.getIndex() - rowsInPageBeforeUnit - 1));
            
            aState.setPageBegin(aCas, firstUnit.getBegin());
            aState.setFocusUnitIndex(unit.getIndex());
            break;
        }
        default:
            throw new IllegalArgumentException("Unknown focus positon: [" + aPos + "]");
        }
    }    
    default void recalculatePage(AnnotatorViewState aState, CAS aCas)
    {
        aState.setPageBegin(aCas, aState.getWindowBeginOffset());
    }
    
    default List<Unit> units(CAS aCas)
    {
        return units(aCas, 0, Integer.MAX_VALUE);
    }
    
    /**
     * Returns the total number of units.
     */
    default int unitCount(CAS aCas)
    {
        return units(aCas).size();
    }
    
    /**
     * Get the unit with the given index. The index is 1-based. If the index is smaller than 1, the
     * first unit is returned. If the index is greater than the number of units available, the last
     * unit is returned.
     */
    default Unit unitAtIndex(CAS aCas, int aIndex)
    {
        int index = aIndex;
        
        if (index < 1) {
            index = 1;
        }
        
        List<Unit> units = units(aCas);
        
        if (index > units.size()) {
            index = units.size();
        }
        
        return units.get(index - 1);
    }
    
    default List<Unit> unitsStartingAtOffset(CAS aCas, int aOffset, int aCount)
    {
        return units(aCas).stream()
                .filter(unit -> unit.getBegin() >= aOffset)
                .limit(aCount)
                .collect(Collectors.toList());
    }
    
    /**
     * @param aIndex index of the unit to move to (1-based)
     */
    default void moveToUnit(AnnotatorViewState aState, CAS aCas, int aIndex, FocusPosition aPos)
    {
        Unit unit = unitAtIndex(aCas, aIndex);
        moveToOffset(aState, aCas, unit.getBegin(), aPos);
    }
    
    default void moveToPreviousPage(AnnotatorViewState aState, CAS aCas, FocusPosition aPos)
    {
        moveToUnit(aState, aCas,
                aState.getFocusUnitIndex() - aState.getPreferences().getWindowSize(), aPos);
    }

    default void moveToNextPage(AnnotatorViewState aState, CAS aCas, FocusPosition aPos)
    {
        moveToUnit(aState, aCas,
                aState.getFocusUnitIndex() + aState.getPreferences().getWindowSize(), aPos);
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
