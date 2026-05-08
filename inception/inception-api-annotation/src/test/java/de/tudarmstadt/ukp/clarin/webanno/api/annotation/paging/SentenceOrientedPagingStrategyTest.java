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
import static org.assertj.core.api.Assertions.tuple;

import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference;
import de.tudarmstadt.ukp.inception.rendering.paging.Unit;

class SentenceOrientedPagingStrategyTest
{
    private SentenceOrientedPagingStrategy sut;
    private JCas jcas;
    private AnnotatorStateImpl state;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new SentenceOrientedPagingStrategy();
        jcas = JCasFactory.createJCas();
        state = new AnnotatorStateImpl();
        var prefs = new AnnotationPreference();
        prefs.setWindowSize(5);
        state.setPreferences(prefs);
    }

    @Test
    void testMultipleSentences() throws Exception
    {
        var builder = new JCasBuilder(jcas);

        builder.add("This is the first sentence.", Sentence.class);
        builder.add(" ");
        builder.add("This is the second sentence.", Sentence.class);
        builder.add(" ");
        builder.add("This is the third sentence.", Sentence.class);
        builder.close();

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly("This is the first sentence.", "This is the second sentence.",
                        "This is the third sentence.");
    }

    @Test
    void testSingleSentence() throws Exception
    {
        var builder = new JCasBuilder(jcas);
        builder.add("This is a single sentence.", Sentence.class);
        builder.close();

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly("This is a single sentence.");
    }

    @Test
    void testEmptyDocument() throws Exception
    {
        jcas.setDocumentText("");

        assertThat(sut.units(jcas.getCas())).isEmpty();
    }

    @Test
    void testUnitsWithIndexRange() throws Exception
    {
        var builder = new JCasBuilder(jcas);

        builder.add("First sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Second sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Third sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Fourth sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Fifth sentence.", Sentence.class);
        builder.close();

        // Test getting units 2-4
        assertThat(sut.units(jcas.getCas(), 2, 4))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly("Second sentence.", "Third sentence.", "Fourth sentence.");
    }

    @Test
    void testUnitsWithFirstIndexOnly() throws Exception
    {
        var builder = new JCasBuilder(jcas);

        builder.add("First sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Second sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Third sentence.", Sentence.class);
        builder.close();

        // Test getting units from index 2 onwards
        assertThat(sut.units(jcas.getCas(), 2, Integer.MAX_VALUE))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly("Second sentence.", "Third sentence.");
    }

    @Test
    void testUnitsWithIndexOutOfBounds() throws Exception
    {
        var builder = new JCasBuilder(jcas);

        builder.add("First sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Second sentence.", Sentence.class);
        builder.close();

        // Test requesting beyond available units
        assertThat(sut.units(jcas.getCas(), 5, 10)).isEmpty();
    }

    @Test
    void testUnitCount() throws Exception
    {
        var builder = new JCasBuilder(jcas);

        builder.add("First sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Second sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Third sentence.", Sentence.class);
        builder.close();

        assertThat(sut.unitCount(jcas.getCas())).isEqualTo(3);
    }

    @Test
    void testUnitIndexes() throws Exception
    {
        var builder = new JCasBuilder(jcas);

        builder.add("First sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Second sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Third sentence.", Sentence.class);
        builder.close();

        assertThat(sut.units(jcas.getCas())).extracting(Unit::getIndex).containsExactly(1, 2, 3);
    }

    @Test
    void testUnitBeginAndEnd() throws Exception
    {
        var builder = new JCasBuilder(jcas);

        builder.add("First.", Sentence.class);
        builder.add(" ");
        builder.add("Second.", Sentence.class);
        builder.close();

        assertThat(sut.units(jcas.getCas())).extracting(Unit::getBegin, Unit::getEnd)
                .containsExactly(tuple(0, 6), tuple(7, 14));
    }

    @Test
    void testSentenceWithId() throws Exception
    {
        var sentence1 = new Sentence(jcas, 0, 6);
        sentence1.setId("sent-1");
        sentence1.addToIndexes();

        var sentence2 = new Sentence(jcas, 7, 14);
        sentence2.setId("sent-2");
        sentence2.addToIndexes();

        jcas.setDocumentText("First. Second.");

        var units = sut.units(jcas.getCas());
        assertThat(units).extracting(Unit::getId).containsExactly("sent-1", "sent-2");
    }

    @Test
    void testSentenceWithoutId() throws Exception
    {
        var builder = new JCasBuilder(jcas);
        builder.add("First sentence.", Sentence.class);
        builder.close();

        var units = sut.units(jcas.getCas());
        assertThat(units).extracting(Unit::getId).containsExactly((String) null);
    }

    @Test
    void testMoveToOffsetTopInFirstSentence() throws Exception
    {
        state.setPagingStrategy(sut);

        var builder = new JCasBuilder(jcas);
        builder.add("First sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Second sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Third sentence.", Sentence.class);
        builder.close();

        // Move to offset within first sentence
        sut.moveToOffset(state, jcas.getCas(), 5, TOP);

        // Should start at beginning of first sentence (unit containing offset 5)
        assertThat(state.getWindowBeginOffset()).isEqualTo(0);
    }

    @Test
    void testMoveToOffsetTopInSecondSentence() throws Exception
    {
        state.setPagingStrategy(sut);

        var builder = new JCasBuilder(jcas);
        builder.add("First sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Second sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Third sentence.", Sentence.class);
        builder.close();

        // Move to offset within second sentence
        var offsetInSecondSentence = "First sentence. ".length() + 5;
        sut.moveToOffset(state, jcas.getCas(), offsetInSecondSentence, TOP);

        // Should start at beginning of second sentence
        assertThat(state.getWindowBeginOffset()).isEqualTo("First sentence. ".length());
    }

    @Test
    void testMoveToOffsetCenteredInMiddleSentence() throws Exception
    {
        state.setPagingStrategy(sut);

        var builder = new JCasBuilder(jcas);

        for (int i = 1; i <= 10; i++) {
            builder.add("Sentence " + i + ".", Sentence.class);
            builder.add(" ");
        }
        builder.close();

        // Move to offset within the 5th sentence
        var fifthSentenceOffset = "Sentence 1. Sentence 2. Sentence 3. Sentence 4. ".length() + 5;
        sut.moveToOffset(state, jcas.getCas(), fifthSentenceOffset, CENTERED);

        // With window size of 5 and centering on sentence 5,
        // we should start at sentence 3 (5 - 5/2 = 5 - 2 = 3)
        var expectedBegin = "Sentence 1. Sentence 2. ".length();
        assertThat(state.getWindowBeginOffset()).isEqualTo(expectedBegin);
        assertThat(state.getFocusUnitIndex()).isEqualTo(5);
    }

    @Test
    void testMoveToOffsetCenteredInFirstSentence() throws Exception
    {
        state.setPagingStrategy(sut);

        var builder = new JCasBuilder(jcas);

        for (int i = 1; i <= 10; i++) {
            builder.add("Sentence " + i + ".", Sentence.class);
            builder.add(" ");
        }
        builder.close();

        // Move to offset within the first sentence
        sut.moveToOffset(state, jcas.getCas(), 5, CENTERED);

        // Should start at the beginning since we can't center before the start
        assertThat(state.getWindowBeginOffset()).isEqualTo(0);
        assertThat(state.getFocusUnitIndex()).isEqualTo(1);
    }

    @Test
    void testMoveToOffsetCenteredInLastSentence() throws Exception
    {
        state.setPagingStrategy(sut);

        var builder = new JCasBuilder(jcas);

        for (int i = 1; i <= 10; i++) {
            builder.add("Sentence " + i + ".", Sentence.class);
            builder.add(" ");
        }
        builder.close();

        // Move to offset within the 10th sentence
        var text = jcas.getDocumentText();
        var lastSentenceStart = text.lastIndexOf("Sentence 10.");
        sut.moveToOffset(state, jcas.getCas(), lastSentenceStart + 5, CENTERED);

        // With window size of 5 and centering on sentence 10,
        // we should start at sentence 8 (10 - 5/2 = 10 - 2 = 8)
        var expectedStart = 8;
        var units = sut.units(jcas.getCas());
        assertThat(state.getWindowBeginOffset()).isEqualTo(units.get(expectedStart - 1).getBegin());
        assertThat(state.getFocusUnitIndex()).isEqualTo(10);
    }

    @Test
    void testMoveToOffsetAtZero() throws Exception
    {
        state.setPagingStrategy(sut);

        var builder = new JCasBuilder(jcas);
        builder.add("First sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Second sentence.", Sentence.class);
        builder.close();

        // Offset 0 is special case - always sets page begin to 0
        sut.moveToOffset(state, jcas.getCas(), 0, TOP);

        assertThat(state.getWindowBeginOffset()).isEqualTo(0);
    }

    @Test
    void testMoveToOffsetInvalidOffsetMovesToLastUnit() throws Exception
    {
        state.setPagingStrategy(sut);

        var builder = new JCasBuilder(jcas);
        builder.add("First sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Second sentence.", Sentence.class);
        builder.close();

        // Try to move to an offset beyond the document
        var offsetBeyondDocument = 1000;
        sut.moveToOffset(state, jcas.getCas(), offsetBeyondDocument, CENTERED);

        // Should move to the last unit (second sentence)
        // With CENTERED mode, window attempts to center on the unit
        // With only 2 sentences and windowSize=5, centering on sentence 2 shows both sentences
        assertThat(state.getFocusUnitIndex()).isEqualTo(2);
        assertThat(state.getWindowBeginOffset()).isEqualTo(0); // Window starts at first sentence
    }

    @Test
    void testMoveToOffsetBetweenSentencesMovesToPreviousUnit() throws Exception
    {
        state.setPagingStrategy(sut);

        var builder = new JCasBuilder(jcas);
        builder.add("First sentence.", Sentence.class);
        builder.add("   "); // Whitespace between sentences
        builder.add("Second sentence.", Sentence.class);
        builder.close();

        // Offset in whitespace between first and second sentence
        var offsetBetweenSentences = "First sentence.".length() + 1;
        sut.moveToOffset(state, jcas.getCas(), offsetBetweenSentences, CENTERED);

        // Should move to the closest valid position BEFORE the offset
        // which is the first sentence
        assertThat(state.getWindowBeginOffset()).isEqualTo(0);
        assertThat(state.getFocusUnitIndex()).isEqualTo(1);
    }

    @Test
    void testMoveToOffsetBeyondDocumentMovesToLastUnit() throws Exception
    {
        state.setPagingStrategy(sut);

        var builder = new JCasBuilder(jcas);
        builder.add("First sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Second sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Third sentence.", Sentence.class);
        builder.close();

        // Offset beyond the document
        var offsetBeyondDocument = jcas.getDocumentText().length() + 100;
        sut.moveToOffset(state, jcas.getCas(), offsetBeyondDocument, CENTERED);

        // Should move to the closest valid position before the offset
        // which is the third (last) sentence
        assertThat(state.getFocusUnitIndex()).isEqualTo(3);
    }

    @Test
    void testMoveToOffsetInTrailingWhitespaceMovesToLastUnit() throws Exception
    {
        state.setPagingStrategy(sut);

        var builder = new JCasBuilder(jcas);
        builder.add("First sentence.", Sentence.class);
        builder.add(" ");
        builder.add("Second sentence.", Sentence.class);
        builder.add("     "); // Trailing whitespace
        builder.close();

        // Offset in trailing whitespace after last sentence
        var offsetInTrailingSpace = "First sentence. Second sentence.".length() + 2;
        sut.moveToOffset(state, jcas.getCas(), offsetInTrailingSpace, CENTERED);

        // Should move to the closest valid position before the offset
        // which is the second (last) sentence
        assertThat(state.getFocusUnitIndex()).isEqualTo(2);
    }

    @Test
    void testMoveToOffsetBeforeFirstUnitMovesToFirstUnit() throws Exception
    {
        state.setPagingStrategy(sut);

        jcas.setDocumentText("   Leading whitespace. ");

        // Create sentence that doesn't start at position 0
        new Sentence(jcas, 3, 23).addToIndexes();

        // Offset in leading whitespace before first sentence
        var offsetBeforeFirstSentence = 1;
        sut.moveToOffset(state, jcas.getCas(), offsetBeforeFirstSentence, CENTERED);

        // No valid position before offset, so should move to first valid position
        assertThat(state.getFocusUnitIndex()).isEqualTo(1);
        assertThat(state.getWindowBeginOffset()).isEqualTo(3);
    }

    @Test
    void testMoveToOffsetBetweenSentencesTop() throws Exception
    {
        state.setPagingStrategy(sut);

        var builder = new JCasBuilder(jcas);
        builder.add("First sentence.", Sentence.class);
        builder.add("   ");
        builder.add("Second sentence.", Sentence.class);
        builder.close();

        // Offset in whitespace between sentences
        var offsetBetweenSentences = "First sentence.".length() + 1;
        sut.moveToOffset(state, jcas.getCas(), offsetBetweenSentences, TOP);

        // With TOP position, should still move to closest valid position before offset
        var firstSentenceStart = 0;
        assertThat(state.getWindowBeginOffset()).isEqualTo(firstSentenceStart);
    }

    @Test
    void testMoveToOffsetInGapBetweenMultipleSentences() throws Exception
    {
        state.setPagingStrategy(sut);

        var builder = new JCasBuilder(jcas);

        for (int i = 1; i <= 5; i++) {
            builder.add("Sentence " + i + ".", Sentence.class);
            builder.add("   ");
        }
        builder.close();

        // Offset in gap between sentence 3 and 4
        var text = jcas.getDocumentText();
        var sentence3End = text.indexOf("Sentence 3.") + "Sentence 3.".length();
        var offsetInGap = sentence3End + 1;

        sut.moveToOffset(state, jcas.getCas(), offsetInGap, CENTERED);

        // Should center on sentence 3 (closest before offset)
        assertThat(state.getFocusUnitIndex()).isEqualTo(3);
    }
}
