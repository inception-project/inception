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
package de.tudarmstadt.ukp.inception.support.xml.sanitizer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.inception.support.xml.ContentHandlerAdapter;

//CHECKSTYLE:OFF
class IllegalXmlCharacterSanitizingContentHandlerTest
{
    @Test
    void testXml10() throws Exception
    {
        var stringCollector = new ContentToString();
        var adapter = new IllegalXmlCharacterSanitizingContentHandler(stringCollector);
        adapter.setXml11(false);
        adapter.setReplacementChar('\uFFFD');

        char[] input = { '\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006',
                '\u0007', '\u0008', '\u0009', '\n', '\u000b', '\u000c', '\r', '\u000e', '\u000f',
                '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016', '\u0017',
                '\u0018', '\u0019', '\u001a', '\u001b', '\u001c', '\u001d', '\u001e', '\u001f',
                '\u0020', '\uD800' };
        adapter.characters(input, 0, input.length);
        assertThat(stringCollector.toString().toCharArray()).hasSameSizeAs(input);
        assertThat(stringCollector.toString().toCharArray()).containsExactly('\uFFFD', '\uFFFD',
                '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\u0009',
                '\n', '\uFFFD', '\uFFFD', '\r', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD',
                '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD',
                '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\u0020', '\uFFFD');
    }

    @Test
    void testXml11() throws Exception
    {
        var stringCollector = new ContentToString();
        var adapter = new IllegalXmlCharacterSanitizingContentHandler(stringCollector);
        adapter.setXml11(true);
        adapter.setReplacementChar('\uFFFD');

        char[] input = { '\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006',
                '\u0007', '\u0008', '\u0009', '\n', '\u000b', '\u000c', '\r', '\u000e', '\u000f',
                '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016', '\u0017',
                '\u0018', '\u0019', '\u001a', '\u001b', '\u001c', '\u001d', '\u001e', '\u001f',
                '\u0020', '\uD800' };
        adapter.characters(input, 0, input.length);
        assertThat(stringCollector.toString().toCharArray()).hasSameSizeAs(input);
        assertThat(stringCollector.toString().toCharArray()).containsExactly('\uFFFD', '\u0001',
                '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007', '\u0008', '\u0009',
                '\n', '\u000b', '\u000c', '\r', '\u000e', '\u000f', '\u0010', '\u0011', '\u0012',
                '\u0013', '\u0014', '\u0015', '\u0016', '\u0017', '\u0018', '\u0019', '\u001a',
                '\u001b', '\u001c', '\u001d', '\u001e', '\u001f', '\u0020', '\uFFFD');
    }

    @Test
    void testWithSurrogate() throws Exception
    {
        var stringCollector = new ContentToString();
        var adapter = new IllegalXmlCharacterSanitizingContentHandler(stringCollector);
        adapter.setXml11(false);
        adapter.setReplacementChar('\uFFFD');

        var input = "🙋🏽‍♀️";
        adapter.characters(input);
        assertThat(stringCollector.toString()).hasSameSizeAs(input);
        assertThat(stringCollector.toString()).isEqualTo("🙋🏽‍♀️");
    }

    @Test
    void testWithBrokenSurrogate() throws Exception
    {
        var stringCollector = new ContentToString();
        var adapter = new IllegalXmlCharacterSanitizingContentHandler(stringCollector);
        adapter.setXml11(false);
        adapter.setReplacementChar('\uFFFD');

        char[] input = { '\ude4b', '\ud83d' };
        adapter.characters(input, 0, input.length);
        assertThat(stringCollector.toString()).hasSameSizeAs(input);
        assertThat(stringCollector.toString().toCharArray()).containsExactly('\uFFFD', '\uFFFD');
    }

    private static class ContentToString
        extends ContentHandlerAdapter
    {
        private final StringBuilder text = new StringBuilder();

        @Override
        public void startDocument() throws SAXException
        {
            text.setLength(0);
        }

        @Override
        public void characters(char[] aCh, int aStart, int aLength) throws SAXException
        {
            text.append(aCh, aStart, aLength);
        }

        @Override
        public void ignorableWhitespace(char[] aCh, int aStart, int aLength) throws SAXException
        {
            text.append(aCh, aStart, aLength);
        }

        @Override
        public String toString()
        {
            return text.toString();
        }
    }
}
