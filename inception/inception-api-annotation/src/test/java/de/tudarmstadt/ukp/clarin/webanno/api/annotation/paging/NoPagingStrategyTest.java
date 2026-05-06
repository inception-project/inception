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

import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.TOP;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference;

class NoPagingStrategyTest
{
    private NoPagingStrategy sut;
    private JCas jcas;
    private AnnotatorStateImpl state;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new NoPagingStrategy();
        jcas = JCasFactory.createJCas();
        state = new AnnotatorStateImpl();
        var prefs = new AnnotationPreference();
        prefs.setWindowSize(5);
        state.setPreferences(prefs);
    }

    @Test
    void testUnitsWithText() throws Exception
    {
        var text = "This is a test document with some text.";
        jcas.setDocumentText(text);

        var units = sut.units(jcas.getCas());

        assertThat(units).hasSize(1);
        assertThat(units.get(0).getBegin()).isEqualTo(0);
        assertThat(units.get(0).getEnd()).isEqualTo(text.length());
        assertThat(jcas.getDocumentText().substring(units.get(0).getBegin(), units.get(0).getEnd()))
                .isEqualTo(text);
    }

    @Test
    void testUnitsWithEmptyDocument() throws Exception
    {
        jcas.setDocumentText("");

        var units = sut.units(jcas.getCas());

        assertThat(units).hasSize(1);
        assertThat(units.get(0).getBegin()).isEqualTo(0);
        assertThat(units.get(0).getEnd()).isEqualTo(0);
    }

    @Test
    void testUnitsWithMultilineText() throws Exception
    {
        var text = "Line 1\nLine 2\nLine 3\nLine 4";
        jcas.setDocumentText(text);

        var units = sut.units(jcas.getCas());

        assertThat(units).hasSize(1);
        assertThat(units.get(0).getBegin()).isEqualTo(0);
        assertThat(units.get(0).getEnd()).isEqualTo(text.length());
        assertThat(jcas.getDocumentText().substring(units.get(0).getBegin(), units.get(0).getEnd()))
                .isEqualTo(text);
    }

    @Test
    void testUnitsWithIndexRange() throws Exception
    {
        jcas.setDocumentText("Some text");

        // Index range should be ignored, always returns single unit
        var units = sut.units(jcas.getCas(), 1, 5);

        assertThat(units).hasSize(1);
        assertThat(units.get(0).getBegin()).isEqualTo(0);
        assertThat(units.get(0).getEnd()).isEqualTo(9);
    }

    @Test
    void testUnitIndexIsZero() throws Exception
    {
        jcas.setDocumentText("Test");

        var units = sut.units(jcas.getCas());

        assertThat(units.get(0).getIndex()).isEqualTo(0);
    }

    @Test
    void testUnitsAlwaysReturnsSingleUnit() throws Exception
    {
        jcas.setDocumentText("A very long document with multiple sentences. "
                + "Each sentence could be on a different line. "
                + "But NoPagingStrategy always returns the entire document as one unit.");

        var units = sut.units(jcas.getCas());

        assertThat(units).hasSize(1);
    }

    @Test
    void testUnitCount() throws Exception
    {
        jcas.setDocumentText("Test document");

        assertThat(sut.unitCount(jcas.getCas())).isEqualTo(1);
    }

    @Test
    void testUnitCountWithEmptyDocument() throws Exception
    {
        jcas.setDocumentText("");

        assertThat(sut.unitCount(jcas.getCas())).isEqualTo(1);
    }

    @Test
    void testUnitVidIsNull() throws Exception
    {
        jcas.setDocumentText("Test");

        var units = sut.units(jcas.getCas());

        assertThat(units.get(0).getVid()).isNull();
    }

    @Test
    void testUnitsWithLargeDocument() throws Exception
    {
        var largeText = new StringBuilder();
        for (var i = 0; i < 1000; i++) {
            largeText.append("Line ").append(i).append("\n");
        }
        jcas.setDocumentText(largeText.toString());

        var units = sut.units(jcas.getCas());

        assertThat(units).hasSize(1);
        assertThat(units.get(0).getBegin()).isEqualTo(0);
        assertThat(units.get(0).getEnd()).isEqualTo(largeText.length());
    }

    @Test
    void testMoveToOffsetTop() throws Exception
    {
        state.setPagingStrategy(sut);

        jcas.setDocumentText("Some text in the document");

        // Move to offset within the single unit
        sut.moveToOffset(state, jcas.getCas(), 10, TOP);

        // NoPaging has single unit starting at 0
        assertThat(state.getWindowBeginOffset()).isEqualTo(0);
    }

    @Test
    void testMoveToOffsetCentered() throws Exception
    {
        state.setPagingStrategy(sut);

        jcas.setDocumentText("Some text in the document");

        // Move to offset within the single unit
        sut.moveToOffset(state, jcas.getCas(), 10, CENTERED);

        // NoPagingStrategy has only one unit starting at 0
        assertThat(state.getWindowBeginOffset()).isEqualTo(0);
        assertThat(state.getFocusUnitIndex()).isEqualTo(0);
    }
}
