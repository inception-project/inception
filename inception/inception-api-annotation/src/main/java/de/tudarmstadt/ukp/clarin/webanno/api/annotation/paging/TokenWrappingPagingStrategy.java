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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.LINE_SPLITTER_PATTERN;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.paging.Unit;

public class TokenWrappingPagingStrategy
    extends PagingStrategy_ImplBase
{
    private static final long serialVersionUID = -3983123604003839467L;

    private int maxLineLength;

    public TokenWrappingPagingStrategy(int aMaxLineLength)
    {
        maxLineLength = aMaxLineLength;
    }

    @Override
    public List<Unit> units(CAS aCas, int aFirstIndex, int aLastIndex)
    {
        var tokenIterator = aCas.select(Token.class).iterator();

        var units = new ArrayList<Unit>();

        int currentUnitStart = 0;
        int currentUnitEnd = 0;
        while (tokenIterator.hasNext()) {
            var currentToken = tokenIterator.next();

            if (currentToken.getBegin() < currentUnitEnd) {
                throw new IllegalStateException(format(
                        "Unable to render: Token at [%d-%d] illegally overlaps with previous token ending at [%d].",
                        currentToken.getBegin(), currentToken.getEnd(), currentUnitEnd));
            }

            // Add units for each of the lines in the gap
            var gap = aCas.getDocumentText().substring(currentUnitEnd, currentToken.getBegin());
            int gapStart = currentUnitEnd;
            var matcher = LINE_SPLITTER_PATTERN.matcher(gap);
            while (matcher.find()) {
                currentUnitEnd = gapStart + matcher.start();
                units.add(new Unit(units.size() + 1, currentUnitStart, currentUnitEnd));
                currentUnitStart = gapStart + matcher.end();
            }

            var unitNonEmpty = (currentUnitEnd - currentUnitStart) > 0;
            var unitFull = unitNonEmpty
                    && ((currentToken.getEnd() - currentUnitStart) > maxLineLength);

            // If the unit is full, finish the unit and start a new one
            if (unitFull) {
                units.add(new Unit(units.size() + 1, currentUnitStart, currentUnitEnd));
                currentUnitStart = -1;
            }

            if (currentUnitStart == -1) {
                currentUnitStart = currentToken.getBegin();
            }

            currentUnitEnd = currentToken.getEnd();
        }

        // Finish current unit
        if (currentUnitEnd - currentUnitStart > 0) {
            units.add(new Unit(units.size() + 1, currentUnitStart, currentUnitEnd));
            currentUnitStart = -1;
        }

        // // Add any line breaks at the end of the document
        // if (aCas.getDocumentText().length() - currentUnitEnd > 0) {
        // var gap = aCas.getDocumentText().substring(currentUnitEnd,
        // aCas.getDocumentText().length());
        // int gapStart = currentUnitEnd;
        // var matcher = LINE_SPLITTER_PATTERN.matcher(gap);
        // while (matcher.find()) {
        // currentUnitEnd = gapStart + matcher.start();
        // if (currentUnitStart == -1) {
        // currentUnitStart = currentUnitEnd;
        // }
        // units.add(new Unit(units.size() + 1, currentUnitStart, currentUnitEnd));
        // currentUnitStart = gapStart + matcher.end();
        // }
        // }

        return units;
    }

    @Override
    public Component createPositionLabel(String aId, IModel<AnnotatorState> aModel)
    {
        Label label = new Label(aId, () -> {
            AnnotatorState state = aModel.getObject();
            return String.format("%d-%d / %d blocks [doc %d / %d]",
                    state.getFirstVisibleUnitIndex(), state.getLastVisibleUnitIndex(),
                    state.getUnitCount(), state.getDocumentIndex() + 1,
                    state.getNumberOfDocuments());
        });
        label.setOutputMarkupPlaceholderTag(true);
        return label;
    }

    @Override
    public DefaultPagingNavigator createPageNavigator(String aId, Page aPage)
    {
        return new DefaultPagingNavigator(aId, (AnnotationPageBase) aPage);
    }
}
