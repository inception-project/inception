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
package de.tudarmstadt.ukp.inception.io.jsoncas;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.json.jsoncas2.JsonCas2Serializer;
import org.dkpro.core.api.io.ResourceCollectionReaderBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UimaJsonCasReaderTest
{
    private @TempDir Path tempDir;

    @Test
    void thatValidDocumentIsRead() throws Exception
    {
        var file = write("valid.json", serializedCas());

        var cas = read(file);

        assertThat(cas.getDocumentText()).isEqualTo("Great food.");
        assertThat(cas.select(Annotation.class).at(0, 5).asList()) //
                .extracting(Annotation::getCoveredText) //
                .containsExactly("Great");
    }

    @Test
    void thatJsonLinesWithOtherRecordsAreRejected() throws Exception
    {
        var file = write("records.jsonl", """
                {"text": "Great food", "label": "+"}
                {"text": "Bad service", "label": "-"}
                """);

        assertThatExceptionOfType(IOException.class) //
                .isThrownBy(() -> read(file)) //
                .withMessageContaining("unexpected top-level field [text]");
    }

    @Test
    void thatUnknownTopLevelFieldIsRejected() throws Exception
    {
        var file = write("unknown-field.json", "{\"id\": 1, " + serializedCas().substring(1));

        assertThatExceptionOfType(IOException.class) //
                .isThrownBy(() -> read(file)) //
                .withMessageContaining("unexpected top-level field [id]");
    }

    @Test
    void thatMultipleDocumentsAreRejected() throws Exception
    {
        var line = serializedCas().replace("\n", " ").replace("\r", " ");
        var file = write("two-cases.jsonl", line + "\n" + line + "\n");

        assertThatExceptionOfType(IOException.class) //
                .isThrownBy(() -> read(file)) //
                .withMessageContaining("more than one JSON value");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = { //
            "{\"%TYPES\": {}, \"%VIEWS\": null}              | [%VIEWS] must be a JSON object", //
            "{\"%TYPES\": {}, \"%FEATURE_STRUCTURES\": null} | [%FEATURE_STRUCTURES] must be a JSON array", //
            "{\"%TYPES\": null, \"%VIEWS\": {}}              | [%TYPES] must be a JSON object", //
            "{\"%HEADER\": null}                           | [%HEADER] must be a JSON object", //
            "{\"%TYPES\": {}, \"%FEATURE_STRUCTURES\": {}}   | [%FEATURE_STRUCTURES] must be a JSON array" })
    void thatTopLevelFieldOfWrongTypeIsRejected(String aJson, String aMessage) throws Exception
    {
        var file = write("wrong-type.json", aJson);

        assertThatExceptionOfType(IOException.class) //
                .isThrownBy(() -> read(file)) //
                .withMessageContaining(aMessage);
    }

    @Test
    void thatNonObjectIsRejected() throws Exception
    {
        var file = write("array.json", "[1, 2, 3]");

        assertThatExceptionOfType(IOException.class) //
                .isThrownBy(() -> read(file)) //
                .withMessageContaining("must start with a JSON object");
    }

    private JCas read(Path aFile)
    {
        // A regression must fail the test rather than hang the build
        return assertTimeoutPreemptively(ofSeconds(30), () -> {
            var reader = createReader(UimaJsonCasReader.class, //
                    ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION, aFile.toString());
            var jcas = JCasFactory.createJCas();
            reader.getNext(jcas.getCas());
            return jcas;
        });
    }

    private Path write(String aName, String aContent) throws IOException
    {
        var file = tempDir.resolve(aName);
        Files.writeString(file, aContent, UTF_8);
        return file;
    }

    private static String serializedCas() throws Exception
    {
        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText("Great food.");
        new Annotation(jcas, 0, 5).addToIndexes();

        var bos = new ByteArrayOutputStream();
        new JsonCas2Serializer().serialize(jcas.getCas(), bos);
        return bos.toString(UTF_8);
    }
}
