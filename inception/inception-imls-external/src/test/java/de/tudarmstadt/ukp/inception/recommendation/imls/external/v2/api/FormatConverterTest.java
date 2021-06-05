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

import static de.tudarmstadt.ukp.inception.recommendation.imls.external.util.Fixtures.loadSmallCas;
import static de.tudarmstadt.ukp.inception.recommendation.imls.external.util.Fixtures.loadSmallDocument;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.cas.CAS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FormatConverterTest
{

    private FormatConverter sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        sut = new FormatConverter();
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        sut = null;
    }

    @Test
    public void testFromCas() throws Exception
    {
        CAS cas = loadSmallCas();
        Document expected = loadSmallDocument();

        Document document = sut.fromCas(cas, 23,
                "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity", "value");

        assertThat(document).isEqualTo(expected);

    }

}
