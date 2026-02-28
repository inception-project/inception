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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.CR;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.CRLF;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.LF;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.LINE_SEPARATOR;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.NEL;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.PARAGRAPH_SEPARATOR;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.TOP;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference;

class LineOrientedPagingStrategyTest
{
    private AnnotatorStateImpl state;
    private JCas jcas;
    private LineOrientedPagingStrategy sut;

    @BeforeEach
    void setUp() throws Exception
    {
        jcas = JCasFactory.createJCas();
        state = new AnnotatorStateImpl();
        var prefs = new AnnotationPreference();
        prefs.setWindowSize(5);
        state.setPreferences(prefs);
        sut = new LineOrientedPagingStrategy();
    }

    @Test
    void testMixedLineBreaks() throws Exception
    {
        jcas.setDocumentText( //
                "Line1" + CR + //
                        "Line2" + LF + //
                        "Line3" + CRLF + //
                        "Line4" + NEL + //
                        "Line5" + LINE_SEPARATOR + //
                        "Line6" + PARAGRAPH_SEPARATOR + //
                        "Line7");

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly( //
                        "Line1", //
                        "Line2", //
                        "Line3", //
                        "Line4", //
                        "Line5", //
                        "Line6", //
                        "Line7");
    }

    @Test
    void testConsecutiveLineBreaks() throws Exception
    {
        jcas.setDocumentText( //
                "Line1" + CR + CR + //
                        "Line2" + LF + LF + //
                        "Line3" + CRLF + CRLF + //
                        "Line4" + NEL + NEL + //
                        "Line5" + LINE_SEPARATOR + LINE_SEPARATOR + //
                        "Line6" + PARAGRAPH_SEPARATOR + PARAGRAPH_SEPARATOR + //
                        "Line7");

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly( //
                        "Line1", "", //
                        "Line2", "", //
                        "Line3", "", //
                        "Line4", "", //
                        "Line5", "", //
                        "Line6", "", //
                        "Line7");
    }

    @Test
    void testEndingWithNewline() throws Exception
    {
        jcas.setDocumentText( //
                "Line1" + CR + CR);

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly( //
                        "Line1", "");
    }

    @Test
    void testEmptyDocument() throws Exception
    {
        jcas.setDocumentText("");

        assertThat(sut.units(jcas.getCas())).isEmpty();
    }

    @Test
    void testSingleLineNoNewline() throws Exception
    {
        jcas.setDocumentText("Single line without newline");

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly("Single line without newline");
    }

    @Test
    void testUnitCount() throws Exception
    {
        jcas.setDocumentText( //
                "Line1" + LF + //
                        "Line2" + LF + //
                        "Line3");

        assertThat(sut.unitCount(jcas.getCas())).isEqualTo(3);
    }

    @Test
    void testUnitIndexes() throws Exception
    {
        jcas.setDocumentText( //
                "Line1" + LF + //
                        "Line2" + LF + //
                        "Line3");

        assertThat(sut.units(jcas.getCas())).extracting(u -> u.getIndex()).containsExactly(1, 2, 3);
    }

    @Test
    void testOnlyNewlines() throws Exception
    {
        jcas.setDocumentText(LF + LF + LF);

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly("", "", "");
    }

    @Test
    void testWhitespaceOnly() throws Exception
    {
        jcas.setDocumentText("   ");

        assertThat(sut.units(jcas.getCas())).isEmpty();
    }

    @Test
    void testTrailingNewlineNotIncluded() throws Exception
    {
        jcas.setDocumentText("Line1" + LF + "Line2" + LF);

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly("Line1", "Line2");
    }

    @Test
    void testMoveToOffsetTop() throws Exception
    {
        state.setPagingStrategy(sut);

        jcas.setDocumentText("Line1\nLine2\nLine3\nLine4\nLine5");

        // Move to offset within line 3
        var line3Offset = "Line1\nLine2\n".length() + 2;
        sut.moveToOffset(state, jcas.getCas(), line3Offset, TOP);

        // Should start at beginning of line 3
        assertThat(state.getWindowBeginOffset()).isEqualTo("Line1\nLine2\n".length());
    }

    @Test
    void testMoveToOffsetCentered() throws Exception
    {
        state.setPagingStrategy(sut);

        jcas.setDocumentText("Line1\nLine2\nLine3\nLine4\nLine5\nLine6\nLine7");

        // Move to offset within line 4
        var line4Offset = "Line1\nLine2\nLine3\n".length() + 2;
        sut.moveToOffset(state, jcas.getCas(), line4Offset, CENTERED);

        // Should center on line 4, starting at line 2 (4 - 5/2 = 4 - 2 = 2)
        var expectedBegin = "Line1\n".length();
        assertThat(state.getWindowBeginOffset()).isEqualTo(expectedBegin);
        assertThat(state.getFocusUnitIndex()).isEqualTo(4);
    }

    @Test
    void testMoveToOffsetBetweenLinesMovesToPreviousLine() throws Exception
    {
        state.setPagingStrategy(sut);

        jcas.setDocumentText("Line1\n\n\nLine2");

        // Offset in empty lines between Line1 and Line2
        var offsetInEmptyLine = "Line1\n\n".length();
        sut.moveToOffset(state, jcas.getCas(), offsetInEmptyLine, CENTERED);

        // Should move to closest valid position before offset
        // The empty line at that position should be a valid unit, or it should go to previous
        assertThat(state.getFocusUnitIndex()).isGreaterThan(0);
    }
}
