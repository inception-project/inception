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
package de.tudarmstadt.ukp.inception.externalsearch.pubmed;

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.EntrezClient;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.pmcoa.PmcOaClient;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.traits.PubMedProviderTraits;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
@Tag("slow")
public class PubMedProviderTest
{
    protected @Mock AnnotationSchemaService annotationService;

    private EntrezClient entrezClient = new EntrezClient();
    private PmcOaClient pmcOaClient = new PmcOaClient();
    private PubMedCentralProvider sut;
    private DocumentRepository repo;
    private PubMedProviderTraits traits = null;

    @BeforeEach
    public void setup() throws Exception
    {
        Thread.sleep(1000); // Get around API rate limiting
        sut = new PubMedCentralProvider(entrezClient, pmcOaClient, annotationService);
        repo = new DocumentRepository("dummy", null);
    }

    @Test
    public void thatExecuteQueryWorks() throws Exception
    {
        List<ExternalSearchResult> results = sut.executeQuery(repo, traits, "asthma");

        // System.out.println(results);

        assertThat(results).isNotEmpty();
    }

    @Test
    public void thatGetDocumentTextWorks() throws Exception
    {
        when(annotationService.getFullProjectTypeSystem(any()))
                .thenReturn(createTypeSystemDescription());

        String results = sut.getDocumentText(repo, traits, "PMC", "7096989");

        // System.out.println(results);

        assertThat(results)
                .contains("Asthma is the most common inflammatory disease of the lungs.");
    }
}
