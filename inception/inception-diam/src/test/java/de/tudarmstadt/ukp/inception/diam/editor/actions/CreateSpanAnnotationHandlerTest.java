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
package de.tudarmstadt.ukp.inception.diam.editor.actions;

import static de.tudarmstadt.ukp.inception.diam.editor.actions.CreateSpanAnnotationHandler.getRangeFromRequest;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toJsonString;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.wicket.mock.MockRequestParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.inception.diam.model.compact.CompactRange;
import de.tudarmstadt.ukp.inception.diam.model.compact.CompactRangeList;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.paging.Unit;

class CreateSpanAnnotationHandlerTest
{
    private CAS cas;
    private AnnotatorState state;

    @BeforeEach
    void setup() throws Exception
    {
        cas = CasFactory.createText("This is a test.");
        state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setVisibleUnits(asList(new Unit(0, 0, cas.getDocumentText().length())), 1);
    }

    @Test
    void thatSelectionIsClippedToDocumentBoundaries_begin() throws Exception
    {
        var begin = 0;
        var end = 20;

        assertThat(end).isGreaterThan(cas.getDocumentText().length());

        var params = new MockRequestParameters();
        params.setParameterValue(CreateSpanAnnotationHandler.PARAM_OFFSETS,
                toJsonString(new CompactRangeList(new CompactRange(begin, end))));

        var range = getRangeFromRequest(state, params, cas);

        assertThat(range.getBegin()).isEqualTo(0);
        assertThat(range.getEnd()).isEqualTo(cas.getDocumentText().length());
    }

    @Test
    void thatSelectionIsClippedToDocumentBoundaries_end() throws Exception
    {
        var begin = -10;
        var end = 10;

        assertThat(begin).isLessThan(0);
        assertThat(end).isLessThan(cas.getDocumentText().length());

        var params = new MockRequestParameters();
        params.setParameterValue(CreateSpanAnnotationHandler.PARAM_OFFSETS,
                toJsonString(new CompactRangeList(new CompactRange(begin, end))));

        var range = getRangeFromRequest(state, params, cas);

        assertThat(range.getBegin()).isEqualTo(0);
        assertThat(range.getEnd()).isEqualTo(end);
    }
}
