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
package de.tudarmstadt.ukp.clarin.webanno.text;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

class PretokenizedLineOrientedTextReaderTest
{
    @Test
    void test() throws Exception
    {
        var doc = JCasFactory.createJCas();

        var reader = createReader(PretokenizedLineOrientedTextReader.class,
                PretokenizedLineOrientedTextReader.PARAM_SOURCE_LOCATION, "LICENSE.txt");

        reader.getNext(doc.getCas());

        assertThat(select(doc, Sentence.class)).hasSize(169);
        assertThat(select(doc, Token.class)).hasSize(1581);
    }

    @Test
    public void testSpaceSeparatedNoFinalLineBreak() throws Exception
    {
        var doc = JCasFactory.createJCas();

        var reader = createReader(PretokenizedLineOrientedTextReader.class,
                PretokenizedLineOrientedTextReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/text/space-separated-no-final-line-break.txt");

        reader.getNext(doc.getCas());

        assertThat(select(doc, Sentence.class)) //
                .extracting(s -> s.getCoveredText()) //
                .containsExactly("1 2 3", "4 5 6", "7 8 9");

        assertThat(select(doc, Token.class)) //
                .extracting(t -> t.getCoveredText()) //
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    public void testTextWithPunctuation() throws Exception
    {
        var doc = JCasFactory.createJCas();

        var reader = createReader(PretokenizedLineOrientedTextReader.class,
                PretokenizedLineOrientedTextReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/text/text-with-punctuation.txt");

        reader.getNext(doc.getCas());

        assertThat(select(doc, Sentence.class)) //
                .extracting(s -> s.getCoveredText()) //
                .containsExactly("Yesterday, I left the house.");

        assertThat(select(doc, Token.class)) //
                .extracting(t -> t.getCoveredText()) //
                .containsExactly("Yesterday,", "I", "left", "the", "house.");
    }

    @Test
    void obfuscatedFileIsReadIntoCas() throws Exception
    {
        var fs = new TextFormatSupport();

        var original = "My number is 555-1234.";

        try (var in = new ByteArrayInputStream(original.getBytes(UTF_8));
                var ob = fs.obfuscate(null, in)) {

            var obBytes = ob.readAllBytes();

            var tmp = File.createTempFile("text-format-support-test", ".txt");
            tmp.deleteOnExit();
            try (var out = new FileOutputStream(tmp)) {
                out.write(obBytes);
            }

            var jcas = JCasFactory.createJCas();
            fs.read(new SourceDocument(), jcas.getCas(), tmp);

            var casText = jcas.getDocumentText();
            assertThat(casText).isEqualTo(new String(obBytes, UTF_8));
        }
    }
}
