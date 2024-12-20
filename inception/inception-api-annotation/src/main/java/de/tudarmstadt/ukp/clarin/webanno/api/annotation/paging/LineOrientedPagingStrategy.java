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

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.paging.Unit;

public class LineOrientedPagingStrategy
    extends PagingStrategy_ImplBase
{
    private static final long serialVersionUID = -991967885210129525L;

    static final String CR = "\r"; // carriage return (CR) (classic Mac)
    static final String LF = "\n"; // line feed (LF) (Unix)
    static final String CRLF = "\r\n"; // CRLF (Windows)
    static final String NEL = "\u0085"; // Next Line (NEL)
    static final String LINE_SEPARATOR = "\u2028"; // Line Separator
    static final String PARAGRAPH_SEPARATOR = "\u2029"; // Paragraph Separator

    // Mind that CRLF must come before CR and LF here so it matches as a unit!
    static final List<String> LINE_SEPARATORS = asList(CRLF, CR, LF, NEL, LINE_SEPARATOR,
            PARAGRAPH_SEPARATOR);

    static final Pattern LINE_PATTERN = compile("[^" + join("", LINE_SEPARATORS) + "]+" //
            + "|" + join("|", LINE_SEPARATORS));
    static final Pattern LINE_SPLITTER_PATTERN = compile(LINE_SEPARATORS.stream() //
            .map(Pattern::quote) //
            .collect(joining("|")));

    @Override
    public List<Unit> units(CAS aCas, int aFirstIndex, int aLastIndex)
    {
        var text = aCas.getDocumentText();
        var matcher = LINE_SPLITTER_PATTERN.matcher(text);

        var unitStart = 0;
        var unitEnd = 0;
        var index = 1;

        var units = new ArrayList<Unit>();
        while (matcher.find()) {
            unitEnd = matcher.start();
            units.add(new Unit(index, unitStart, unitEnd));
            unitStart = matcher.end();
            index++;
        }

        if (unitStart < text.length()) {
            if (!text.substring(unitStart).isBlank()) {
                units.add(new Unit(index, unitStart, text.length()));
            }
        }

        return units;
    }

    @Override
    public Component createPositionLabel(String aId, IModel<AnnotatorState> aModel)
    {
        var label = new Label(aId, () -> {
            var state = aModel.getObject();
            return String.format("%d-%d / %d lines [doc %d / %d]", state.getFirstVisibleUnitIndex(),
                    state.getLastVisibleUnitIndex(), state.getUnitCount(),
                    state.getDocumentIndex() + 1, state.getNumberOfDocuments());
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
