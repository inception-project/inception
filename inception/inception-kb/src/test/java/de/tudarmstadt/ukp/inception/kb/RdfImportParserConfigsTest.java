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
package de.tudarmstadt.ukp.inception.kb;

import static de.tudarmstadt.ukp.inception.kb.RdfImportParserConfigs.doctypeTolerantParserConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RdfImportParserConfigsTest
{
    private static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";

    @Test
    void thatInternalDoctypeEntitiesAreAccepted() throws Exception
    {
        var rdf = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE rdf:RDF [ <!ENTITY ex "http://example.org/"> ]>
                <rdf:RDF xmlns:rdf="%s" xmlns:rdfs="%s">
                  <rdf:Description rdf:about="&ex;thing">
                    <rdfs:label>Thing</rdfs:label>
                  </rdf:Description>
                </rdf:RDF>
                """.formatted(RDF_NS, RDFS_NS);

        var model = parse(rdf);

        assertThat(model) //
                .extracting(Statement::getSubject) //
                .extracting(Object::toString) //
                .containsExactly("http://example.org/thing");
    }

    @Test
    void thatExternalEntitiesAreNotResolved(@TempDir Path aTempDir) throws Exception
    {
        var secretFile = aTempDir.resolve("secret.txt");
        Files.writeString(secretFile, "TOP-SECRET-XXE-CANARY", UTF_8);

        var rdf = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE rdf:RDF [ <!ENTITY xxe SYSTEM "%s"> ]>
                <rdf:RDF xmlns:rdf="%s" xmlns:rdfs="%s">
                  <rdf:Description rdf:about="http://example.org/thing">
                    <rdfs:label>&xxe;</rdfs:label>
                  </rdf:Description>
                </rdf:RDF>
                """.formatted(secretFile.toUri(), RDF_NS, RDFS_NS);

        // Rejecting the document outright is fine. What must never happen is that the file
        // contents make it into the parsed model.
        var model = new LinkedHashModel();
        var thrown = catchThrowable(() -> model.addAll(parse(rdf)));

        assertThat(model.toString()) //
                .as("external entity must not be expanded into the model") //
                .doesNotContain("TOP-SECRET-XXE-CANARY");

        if (thrown != null) {
            assertThat(thrown.toString()).doesNotContain("TOP-SECRET-XXE-CANARY");
        }
    }

    private static LinkedHashModel parse(String aRdf) throws IOException
    {
        var model = new LinkedHashModel();
        var parser = Rio.createParser(RDFFormat.RDFXML);
        parser.setParserConfig(doctypeTolerantParserConfig());
        parser.setRDFHandler(new StatementCollector(model));
        try (var is = new ByteArrayInputStream(aRdf.getBytes(UTF_8))) {
            parser.parse(is, "http://example.org/");
        }
        return model;
    }
}
