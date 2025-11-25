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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging;

import static org.apache.wicket.event.Broadcast.BREADTH;

import org.apache.uima.cas.CAS;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorViewState;
import de.tudarmstadt.ukp.inception.rendering.paging.PagingStrategy;
import de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition;
import de.tudarmstadt.ukp.inception.rendering.selection.ScrollToEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;

public abstract class PagingStrategy_ImplBase
    implements PagingStrategy
{
    private static final long serialVersionUID = 928025483609029306L;

    @Override
    public void moveToOffset(AnnotatorViewState aState, CAS aCas, int aOffset, VRange aPingRange,
            FocusPosition aPos)
    {
        switch (aPos) {
        case TOP: {
            aState.setPageBegin(aCas, aOffset);
            fireScrollToEvent(aOffset, aPingRange, aPos);
            break;
        }
        case CENTERED: {
            // Find the unit containing the given offset
            var units = units(aCas);
            var unit = units.stream() //
                    .filter(u -> u.getBegin() <= aOffset && aOffset <= u.getEnd()) //
                    .findFirst() //
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No unit contains character offset [" + aOffset + "]"));

            // How many rows to display before the unit such that the unit is centered?
            var rowsInPageBeforeUnit = aState.getPreferences().getWindowSize() / 2;
            // The -1 below is because unit.getIndex() is 1-based
            var firstUnit = units.get(Math.max(0, unit.getIndex() - rowsInPageBeforeUnit - 1));

            aState.setPageBegin(aCas, firstUnit.getBegin());
            aState.setFocusUnitIndex(unit.getIndex());
            fireScrollToEvent(aOffset, aPingRange, aPos);
            break;
        }
        default:
            throw new IllegalArgumentException("Unknown focus positon: [" + aPos + "]");
        }
    }

    private void fireScrollToEvent(int aOffset, VRange aPingRange, FocusPosition aPos)
    {
        var requestCycle = RequestCycle.get();

        if (requestCycle == null) {
            return;
        }

        var handler = requestCycle.find(IPageRequestHandler.class);
        if (handler.isPresent() && handler.get().isPageInstanceCreated()) {
            var page = (Page) handler.get().getPage();
            var target = requestCycle.find(AjaxRequestTarget.class).orElse(null);
            page.send(page, BREADTH, new ScrollToEvent(target, aOffset, aPingRange, aPos));
        }
    }
}
