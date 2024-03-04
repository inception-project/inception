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

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.uima.UIMAException;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.EntrezClient;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.pmcoa.PmcOaClient;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.traits.PubMedProviderTraits;
import de.tudarmstadt.ukp.inception.io.bioc.BioCFormatSupport;
import de.tudarmstadt.ukp.inception.io.bioc.model.BioCToCas;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class PubMedCentralProvider
    implements ExternalSearchProvider<PubMedProviderTraits>
{
    private static final String PMCID_PREFIX = "PMC";

    public static final String DB_PUB_MED_CENTRAL = "pmc";

    private static final String EXT_XML = ".xml";

    private final PmcOaClient pmcoaClient;
    private final EntrezClient entrezClient;
    private final AnnotationSchemaService schemaService;

    public PubMedCentralProvider(EntrezClient aEntrezClient, PmcOaClient aPmcoaClient,
            AnnotationSchemaService aSchemaService)
    {
        pmcoaClient = aPmcoaClient;
        entrezClient = aEntrezClient;
        schemaService = aSchemaService;
    }

    @Override
    public List<ExternalSearchResult> executeQuery(DocumentRepository aDocumentRepository,
            PubMedProviderTraits aTraits, String aQuery)
    {
        var date = Instant.now().atZone(ZoneOffset.UTC).minus(Duration.ofHours(24));
        var query = aQuery + " AND \"open access\"[filter] AND (\"0001/01/01\"[PubDate] : \""
                + date.get(YEAR) + "/" + date.get(MONTH_OF_YEAR) + "/" + date.get(DAY_OF_MONTH)
                + "\"[PubDate])";

        var searchResponse = entrezClient.esearch(DB_PUB_MED_CENTRAL, query, 0, 100);
        var summaryResponse = entrezClient.esummary(DB_PUB_MED_CENTRAL,
                searchResponse.getIdList().stream().mapToInt(i -> i).toArray());

        var results = new ArrayList<ExternalSearchResult>();
        for (var summary : summaryResponse.getDocSumaries()) {
            var result = new ExternalSearchResult(aDocumentRepository, DB_PUB_MED_CENTRAL,
                    summary.getId() + EXT_XML);
            result.setOriginalUri(
                    "https://www.ncbi.nlm.nih.gov/pmc/articles/" + PMCID_PREFIX + summary.getId());
            result.setOriginalSource(summary.source());
            result.setDocumentTitle(summary.title());
            result.setTimestamp(summary.date());
            results.add(result);
        }

        return results;
    }

    private String stripExtension(String aDocumentId)
    {
        if (aDocumentId != null && aDocumentId.endsWith(EXT_XML)) {
            return aDocumentId.substring(0, aDocumentId.length() - EXT_XML.length());
        }
        return aDocumentId;
    }

    @Deprecated
    @Override
    public ExternalSearchResult getDocumentResult(DocumentRepository aRepository,
            PubMedProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        return new ExternalSearchResult(aRepository, aCollectionId, aDocumentId);
    }

    @Override
    public String getDocumentText(DocumentRepository aDocumentRepository,
            PubMedProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        var biocXml = pmcoaClient.bioc(aTraits, PMCID_PREFIX + stripExtension(aDocumentId));

        try {
            var project = aDocumentRepository.getProject();
            var cas = WebAnnoCasUtil.createCas(schemaService.getFullProjectTypeSystem(project));
            new BioCToCas().parseXml(new ByteArrayInputStream(biocXml), cas.getJCas());
            return cas.getDocumentText();
        }
        catch (UIMAException | JAXBException | XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public InputStream getDocumentAsStream(DocumentRepository aDocumentRepository,
            PubMedProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        var biocXml = pmcoaClient.bioc(aTraits, PMCID_PREFIX + stripExtension(aDocumentId));

        return new ByteArrayInputStream(biocXml);
    }

    @Override
    public String getDocumentFormat(DocumentRepository aRepository, PubMedProviderTraits aTraits,
            String aCollectionId, String aDocumentId)
        throws IOException
    {
        return BioCFormatSupport.ID;
    }
}
