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
package de.tudarmstadt.ukp.inception.recommendation.imls.external.v2.api;

import static de.tudarmstadt.ukp.inception.recommendation.imls.external.util.Fixtures.loadAlaskaCas;

import java.net.URI;

import org.apache.uima.cas.CAS;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

public class ExternalRecommenderV2ApiTest
{
    private ExternalRecommenderV2Api sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        sut = new ExternalRecommenderV2Api(URI.create("http://localhost:8000"));
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        sut = null;
    }

    @Test
    @Ignore
    public void testPredictDocument() throws Exception
    {
        FormatConverter converter = new FormatConverter();
        CAS cas = loadAlaskaCas();
        Document request = converter.documentFromCas(cas,
                "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity", "value", 0);

        Document response = sut.predict("spacy_ner", "test_model", request).get();

        System.out.println(JSONUtil.toPrettyJsonString(response));
    }
}
