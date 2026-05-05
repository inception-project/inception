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
package de.tudarmstadt.ukp.inception.io.nif;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.assertj.core.api.Assertions.tuple;

import java.io.File;

import org.apache.uima.fit.factory.JCasFactory;
import org.dkpro.core.io.nif.NifReader;
import org.dkpro.core.io.nif.NifWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;

class NifReaderWriterTest
{
    private static final String PREFIX = "urn:inception:";

    @Test
    void nonIriValueIsSkippedWhenNoDefaultPrefix(@TempDir File aTemp) throws Exception
    {
        var cas = createCasWithNamedEntity("LOC", null);

        var writer = createEngine(NifWriter.class, //
                NifWriter.PARAM_TARGET_LOCATION, aTemp);
        writer.process(cas);

        var ttl = contentOf(new File(aTemp, "test.txt.ttl"), UTF_8);
        // No taClassRef / taIdentRef triple because "LOC" is not a valid IRI and no prefix is set.
        assertThat(ttl).doesNotContain("taClassRef").doesNotContain("taIdentRef");
    }

    @Test
    void nonIriValueIsPrefixedWhenDefaultPrefixSet(@TempDir File aTemp) throws Exception
    {
        var cas = createCasWithNamedEntity("LOC", "linked");

        var writer = createEngine(NifWriter.class, //
                NifWriter.PARAM_TARGET_LOCATION, aTemp, //
                NifWriter.PARAM_DEFAULT_CLASS_IRI, PREFIX, //
                NifWriter.PARAM_DEFAULT_IDENTIFIER_IRI, PREFIX);
        writer.process(cas);

        var ttl = contentOf(new File(aTemp, "test.txt.ttl"), UTF_8);
        assertThat(ttl).contains("<" + PREFIX + "LOC>");
        assertThat(ttl).contains("<" + PREFIX + "linked>");
    }

    @Test
    void existingIriValueIsKeptVerbatim(@TempDir File aTemp) throws Exception
    {
        var cas = createCasWithNamedEntity("http://example.org/LOC", "http://example.org/e/1");

        var writer = createEngine(NifWriter.class, //
                NifWriter.PARAM_TARGET_LOCATION, aTemp, //
                NifWriter.PARAM_DEFAULT_CLASS_IRI, PREFIX, //
                NifWriter.PARAM_DEFAULT_IDENTIFIER_IRI, PREFIX);
        writer.process(cas);

        var ttl = contentOf(new File(aTemp, "test.txt.ttl"), UTF_8);
        assertThat(ttl).contains("<http://example.org/LOC>");
        assertThat(ttl).contains("<http://example.org/e/1>");
        assertThat(ttl).doesNotContain(PREFIX);
    }

    @Test
    void readerStripsConfiguredPrefix(@TempDir File aTemp) throws Exception
    {
        var written = createCasWithNamedEntity("LOC", "linked");
        var writer = createEngine(NifWriter.class, //
                NifWriter.PARAM_TARGET_LOCATION, aTemp, //
                NifWriter.PARAM_DEFAULT_CLASS_IRI, PREFIX, //
                NifWriter.PARAM_DEFAULT_IDENTIFIER_IRI, PREFIX);
        writer.process(written);

        var roundTripped = JCasFactory.createJCas();
        var reader = createReader(NifReader.class, //
                NifReader.PARAM_SOURCE_LOCATION, new File(aTemp, "test.txt.ttl"), //
                NifReader.PARAM_STRIP_CLASS_IRI, PREFIX, //
                NifReader.PARAM_STRIP_IDENTIFIER_IRI, PREFIX);
        reader.getNext(roundTripped.getCas());

        assertThat(roundTripped.select(NamedEntity.class).asList()) //
                .extracting(NamedEntity::getValue, NamedEntity::getIdentifier) //
                .containsExactly(tuple("LOC", "linked"));
    }

    @Test
    void readerKeepsIriWhenPrefixDoesNotMatch(@TempDir File aTemp) throws Exception
    {
        var written = createCasWithNamedEntity("LOC", null);
        var writer = createEngine(NifWriter.class, //
                NifWriter.PARAM_TARGET_LOCATION, aTemp, //
                NifWriter.PARAM_DEFAULT_CLASS_IRI, PREFIX);
        writer.process(written);

        var roundTripped = JCasFactory.createJCas();
        var reader = createReader(NifReader.class, //
                NifReader.PARAM_SOURCE_LOCATION, new File(aTemp, "test.txt.ttl"), //
                NifReader.PARAM_STRIP_CLASS_IRI, "urn:somethingelse:");
        reader.getNext(roundTripped.getCas());

        assertThat(roundTripped.select(NamedEntity.class).asList()) //
                .extracting(NamedEntity::getValue) //
                .containsExactly(PREFIX + "LOC");
    }

    @Test
    void roundTripPreservesPlainTagValues(@TempDir File aTemp) throws Exception
    {
        var original = createCasWithNamedEntity("PER", "alice");
        var writer = createEngine(NifWriter.class, //
                NifWriter.PARAM_TARGET_LOCATION, aTemp, //
                NifWriter.PARAM_DEFAULT_CLASS_IRI, PREFIX, //
                NifWriter.PARAM_DEFAULT_IDENTIFIER_IRI, PREFIX);
        writer.process(original);

        var roundTripped = JCasFactory.createJCas();
        var reader = createReader(NifReader.class, //
                NifReader.PARAM_SOURCE_LOCATION, new File(aTemp, "test.txt.ttl"), //
                NifReader.PARAM_STRIP_CLASS_IRI, PREFIX, //
                NifReader.PARAM_STRIP_IDENTIFIER_IRI, PREFIX);
        reader.getNext(roundTripped.getCas());

        assertThat(roundTripped.select(NamedEntity.class).asList()) //
                .extracting(NamedEntity::getValue, NamedEntity::getIdentifier) //
                .containsExactly(tuple("PER", "alice"));
    }

    private static org.apache.uima.jcas.JCas createCasWithNamedEntity(String aValue,
            String aIdentifier)
        throws Exception
    {
        var cas = JCasFactory.createJCas();
        cas.setDocumentText("John lives in Berlin.");

        var dmd = DocumentMetaData.create(cas);
        dmd.setDocumentId("test.txt");
        dmd.setDocumentUri("urn:test.txt");

        var ne = new NamedEntity(cas, 14, 20);
        ne.setValue(aValue);
        if (aIdentifier != null) {
            ne.setIdentifier(aIdentifier);
        }
        ne.addToIndexes();
        return cas;
    }
}
