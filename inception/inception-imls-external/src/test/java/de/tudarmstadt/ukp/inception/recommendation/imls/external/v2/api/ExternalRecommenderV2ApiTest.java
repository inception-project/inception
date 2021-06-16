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
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.uima.cas.CAS;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

@Ignore
public class ExternalRecommenderV2ApiTest
{
    private ExternalRecommenderV2Api sut;

    private static final String CLASSIFIER_ID = "spacy_ner";

    @BeforeEach
    public void setUp()
    {
        sut = new ExternalRecommenderV2Api(URI.create("http://localhost:8000"));
    }

    @AfterEach
    public void tearDown()
    {
        sut = null;
    }

    // Dataset

    @Test
    public void testCreateDataset()
    {
        throw new NotImplementedException();
    }

    @Test
    public void testDeleteDataset()
    {
        throw new NotImplementedException();
    }

    // Documents

    @Test
    public void testListDocumentsInDataset() throws Exception
    {
        String datasetId = "test_dataset";
        DocumentList result = sut.listDocumentsInDataset(datasetId);

        assertThat(result.getNames()).isNotEmpty();
        assertThat(result.getNames()).hasSameSizeAs(result.getVersions());
    }

    @Test
    public void testAddDocumentToDataset()
    {
        throw new NotImplementedException();
    }

    @Test
    public void testDeleteDocumentFromDataset()
    {
        throw new NotImplementedException();
    }

    // Classifier

    @Test
    public void testGetAvailableClassifiers()
    {
        throw new NotImplementedException();
    }

    @Test
    public void testGetClassifierInfo() throws Exception
    {
        ClassifierInfo classifierInfo = sut.getClassifierInfo(CLASSIFIER_ID);

        assertThat(classifierInfo.getName()).isEqualTo(CLASSIFIER_ID);
    }

    // Train/Predict

    @Test
    public void testTrainOnDataset()
    {
        throw new NotImplementedException();
    }

    @Test
    public void testPredictDocument() throws Exception
    {
        FormatConverter converter = new FormatConverter();
        CAS cas = loadAlaskaCas();
        Document request = converter.documentFromCas(cas,
                "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity", "value", 0);

        Document response = sut.predict(CLASSIFIER_ID, "test_model", request);

        System.out.println(JSONUtil.toPrettyJsonString(response));
    }
}
