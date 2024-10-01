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
package de.tudarmstadt.ukp.inception.support.text;

import static de.tudarmstadt.ukp.inception.support.text.TrimUtils.trim;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class TrimUtilsTest
{
    @Test
    public void thatEmptySpanIsTrimmedToEmptySpan()
    {
        int[] span = new int[] { 2, 2 };
        trim("    ", span);
        assertThat(span).containsExactly(2, 2);
    }

    @Test
    public void thatSpanIsTrimmedToEmptySpanStartingAtOriginalStart()
    {
        int[] span = new int[] { 2, 3 };
        trim("    ", span);
        assertThat(span).containsExactly(2, 2);
    }

    @Test
    public void thatLeadingAndTrailingWhitespaceIsRemoved()
    {
        int[] span = new int[] { 0, 4 };
        trim(" ab ", span);
        assertThat(span).containsExactly(1, 3);
    }

    @Test
    public void thatInnerWhitespaceIsRemoved1()
    {
        int[] span = new int[] { 0, 2 };
        trim(" a b ", span);
        assertThat(span).containsExactly(1, 2);
    }

    @Test
    public void thatInnerWhitespaceIsRemoved2()
    {
        int[] span = new int[] { 2, 5 };
        trim(" a b ", span);
        assertThat(span).containsExactly(3, 4);
    }

    @Test
    public void testSingleCharacter()
    {
        int[] span = { 0, 1 };
        trim(".", span);
        assertThat(span).containsExactly(0, 1);
    }

    @Test
    public void testLeadingWhitespace()
    {
        int[] span = { 0, 5 };
        trim(" \t\n\r.", span);
        assertThat(span).containsExactly(4, 5);
    }

    @Test
    public void testTrailingWhitespace()
    {
        int[] span = { 0, 5 };
        trim(". \n\r\t", span);
        assertThat(span).containsExactly(0, 1);
    }

    @Test
    public void testLeadingTrailingWhitespace()
    {
        int[] span = { 0, 9 };
        trim(" \t\n\r. \n\r\t", span);
        assertThat(span).containsExactly(4, 5);
    }

    @Test
    public void testBlankString()
    {
        int[] span = { 1, 2 };
        trim("   ", span);
        assertThat(span).containsExactly(1, 1);
    }

    @Test
    public void testNbsp()
    {
        int[] span = { 0, 2 };
        trim("\u00A0a", span);
        assertThat(span).containsExactly(1, 2);
    }
}
