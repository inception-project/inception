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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocument;

public class PubAnnotationDtoDeserializationTest
{
    @Test
    public void thatTrackedDocumentDeserializes() throws Exception
    {
        var doc = readDocument("/samplePubAnnotation.json");

        assertThat(doc.getSourceDb()).isEqualTo("PubMed");
        assertThat(doc.getSourceId()).isEqualTo("25314077");
        assertThat(doc.getText()).startsWith("Cancer-selective targeting");

        assertThat(doc.getTracks()).hasSize(2);

        var track0 = doc.getTracks().get(0);
        assertThat(track0.getProject()).isEqualTo("Inflammaging");
        assertThat(track0.getDenotations()).hasSize(2);
        assertThat(track0.getDenotations().get(0).getSpans()).hasSize(1);
        assertThat(track0.getDenotations().get(0).getSpans().get(0).getBegin()).isEqualTo(0);
        assertThat(track0.getDenotations().get(0).getSpans().get(0).getEnd()).isEqualTo(86);

        var track1 = doc.getTracks().get(1);
        assertThat(track1.getProject()).isEqualTo("PubmedHPO");

        // Continuous span denotation
        assertThat(track1.getDenotations().get(0).getSpans()).hasSize(1);

        // Discontinuous span denotation (bagging model)
        var discontinuous = track1.getDenotations().get(1);
        assertThat(discontinuous.getSpans()).hasSize(2);
        assertThat(discontinuous.getSpans().get(0).getBegin()).isEqualTo(169);
        assertThat(discontinuous.getSpans().get(1).getBegin()).isEqualTo(178);

        assertThat(track1.getRelations()).hasSize(1);
        assertThat(track1.getRelations().get(0).getSubject()).isEqualTo("T1");
        assertThat(track1.getRelations().get(0).getPredicate()).isEqualTo("associatedWith");
        assertThat(track1.getRelations().get(0).getObject()).isEqualTo("T2");

        assertThat(track1.getAttributes()).hasSize(2);
        assertThat(track1.getAttributes().get(0).getObject()).isEqualTo("Q15306");
        assertThat(track1.getAttributes().get(1).getObject()).isEqualTo(true);
    }

    @Test
    public void thatProjectScopedDocumentDeserializes() throws Exception
    {
        var doc = readDocument("/samplePubAnnotationProjectScoped.json");

        assertThat(doc.getProject()).isEqualTo("GO-BP");
        assertThat(doc.getTracks()).isNull();

        assertThat(doc.getDenotations()).hasSize(2);
        assertThat(doc.getDenotations().get(0).getId()).isEqualTo("T1");
        assertThat(doc.getDenotations().get(0).getObject()).isEqualTo("Protein");

        assertThat(doc.getRelations()).hasSize(1);
        assertThat(doc.getRelations().get(0).getPredicate()).isEqualTo("interactWith");

        assertThat(doc.getAttributes()).hasSize(2);
        assertThat(doc.getAttributes().get(1).getObject()).isEqualTo(true);
    }

    private PubAnnotationDocument readDocument(String aResource) throws Exception
    {
        var mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream(aResource)) {
            assertNotNull(is, "Missing test resource " + aResource);
            return mapper.readValue(is, PubAnnotationDocument.class);
        }
    }
}
