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

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocument;

public class PubAnnotationDtoDeserializationTest
{
    @Test
    public void thatSampleJsonDeserializes() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = getClass().getResourceAsStream("/samplePubAnnotation.json")) {
            assertNotNull(is, "Missing test resource samplePubAnnotation.json");

            PubAnnotationDocument doc = mapper.readValue(is, PubAnnotationDocument.class);

            assertEquals("PubMed", doc.getSourceDb());
            assertEquals("25314077", doc.getSourceId());
            assertNotNull(doc.getText());
            assertTrue(doc.getText().startsWith("Cancer-selective targeting"));

            assertNotNull(doc.getTracks());
            assertEquals(2, doc.getTracks().size());
            assertNotNull(doc.getTracks().get(0).getDenotations());
            assertTrue(doc.getTracks().get(0).getDenotations().size() > 0);
        }
    }
}
