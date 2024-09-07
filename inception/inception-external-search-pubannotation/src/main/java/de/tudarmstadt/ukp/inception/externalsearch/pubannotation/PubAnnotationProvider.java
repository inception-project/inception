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

import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocumentSection.SPRING_LIST_TYPE_REF;
import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpMethod.GET;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchHighlight;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationSectionsFormatSupport;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocumentHandle;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocumentSection;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.traits.PubAnnotationProviderTraits;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.EntrezClient;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.model.DocSum;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class PubAnnotationProvider
    implements ExternalSearchProvider<PubAnnotationProviderTraits>
{
    private static final String EXT_JSON = ".json";

    private final EntrezClient entrezClient;

    public PubAnnotationProvider(EntrezClient aEntrezClient)
    {
        entrezClient = aEntrezClient;
    }

    List<PubAnnotationDocumentHandle> query(PubAnnotationProviderTraits aTraits, String aQuery,
            int aPage, int aPageSize)
    {
        var variables = new HashMap<String, String>();
        variables.put("keywords", aQuery);
        variables.put("page", Integer.toString(aPage));
        variables.put("per", Integer.toString(aPageSize));

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<PubAnnotationDocumentHandle[]> response = restTemplate.exchange(
                aTraits.getUrl() + "/docs.json?keywords={keywords}&page={page}&per={per}", GET,
                null, PubAnnotationDocumentHandle[].class, variables);

        return asList(response.getBody());
    }

    @Override
    public List<ExternalSearchResult> executeQuery(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aTraits, String aQuery)
    {
        var response = query(aTraits, aQuery, 1, 100);

        var documentSummaries = lookupDocumentSummaries(response);

        var results = new ArrayList<ExternalSearchResult>();
        for (var handle : response) {
            var summary = documentSummaries
                    .get(Pair.of(handle.getSourceDb(), parseInt(handle.getSourceId())));
            var result = new ExternalSearchResult(aDocumentRepository, handle.getSourceDb(),
                    handle.getSourceId() + EXT_JSON);
            result.setOriginalSource(summary != null ? summary.source() : handle.getSourceDb());
            result.setDocumentTitle(
                    summary != null ? summary.title() : handle.getSourceId() + EXT_JSON);
            if (summary != null) {
                result.setTimestamp(summary.date());
            }
            result.setHighlights(handle.getHighlights().stream() //
                    .map(ExternalSearchHighlight::new) //
                    .collect(Collectors.toList()));
            results.add(result);
        }

        return results;
    }

    private Map<Pair<String, Integer>, DocSum> lookupDocumentSummaries(
            List<PubAnnotationDocumentHandle> response)
    {
        var documentDetails = new HashMap<Pair<String, Integer>, DocSum>();
        var groupedByDb = response.stream()
                .collect(groupingBy(PubAnnotationDocumentHandle::getSourceDb));
        for (var group : groupedByDb.entrySet()) {
            var db = group.getKey();
            var ids = group.getValue().stream() //
                    .map(PubAnnotationDocumentHandle::getSourceId) //
                    .mapToInt(Integer::parseInt).toArray();
            var summaries = entrezClient.esummary(db, ids).getDocSumaries();
            documentDetails.putAll(summaries.stream()
                    .collect(toMap(summary -> Pair.of(db, summary.getId()), identity())));
        }
        return documentDetails;
    }

    @Deprecated
    @Override
    public ExternalSearchResult getDocumentResult(DocumentRepository aRepository,
            PubAnnotationProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        return new ExternalSearchResult(aRepository, aCollectionId, aDocumentId);
    }

    @Override
    public String getDocumentText(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aTraits, String aCollectionId, String aDocumentId)
    {
        return getSections(aDocumentRepository, aTraits, aCollectionId, aDocumentId).stream()
                .map(PubAnnotationDocumentSection::getText) //
                .collect(Collectors.joining("\n\n"));
    }

    @Override
    public InputStream getDocumentAsStream(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        var json = JSONUtil.toJsonString(
                getSections(aDocumentRepository, aTraits, aCollectionId, aDocumentId));

        return IOUtils.toInputStream(json, UTF_8);
    }

    private List<PubAnnotationDocumentSection> getSections(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aTraits, String aCollectionId, String aDocumentId)
    {
        var variables = new HashMap<String, String>();
        variables.put("collectionId", aCollectionId);
        variables.put("documentId", aDocumentId);

        var restTemplate = new RestTemplate();

        var url = aTraits.getUrl() + "/docs/sourcedb/{collectionId}/sourceid/{documentId}";
        try {
            // If the document has multiple sections, a list is returned...
            var response = restTemplate.exchange(url, GET, null, SPRING_LIST_TYPE_REF, variables);
            return response.getBody();
        }
        catch (RestClientException e) {
            // If the document has as single section, an object is returned...
            PubAnnotationDocumentSection section = restTemplate.getForObject(url,
                    PubAnnotationDocumentSection.class, variables);

            return asList(section);
        }
    }

    @Override
    public String getDocumentFormat(DocumentRepository aRepository,
            PubAnnotationProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        return PubAnnotationSectionsFormatSupport.ID;
    }
}
