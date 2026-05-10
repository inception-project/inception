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

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpMethod.GET;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchHighlight;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationAnnotationsFormatSupport;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocument;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocumentHandle;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationProject;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.traits.PubAnnotationProviderTraits;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.EntrezClient;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.model.DocSum;

public class PubAnnotationProvider
    implements ExternalSearchProvider<PubAnnotationProviderTraits>
{
    private static final String EXT_JSON = ".json";

    /** Project-list cache TTL — keep brief so list stays reasonably current. */
    private static final long PROJECT_LIST_TTL_MS = 5 * 60 * 1000L;

    /**
     * Direct lookup syntax: <code>id:&lt;sourcedb&gt;/&lt;sourceid&gt;</code> bypasses the keyword
     * search and returns a single search result for the named document.
     */
    private static final Pattern ID_QUERY = Pattern.compile(
            "\\s*id:\\s*([A-Za-z][A-Za-z0-9_-]*)\\s*/\\s*(\\S+?)\\s*", Pattern.CASE_INSENSITIVE);

    private final EntrezClient entrezClient;
    private final Map<String, CachedProjects> projectListCache = new HashMap<>();

    public PubAnnotationProvider(EntrezClient aEntrezClient)
    {
        entrezClient = aEntrezClient;
    }

    /**
     * Fetch all known PubAnnotation projects from {@code /projects.json}, cached briefly so
     * repeated UI opens don't hammer the API.
     */
    public List<PubAnnotationProject> listProjects(PubAnnotationProviderTraits aTraits)
    {
        var key = aTraits.getUrl();
        var now = System.currentTimeMillis();
        var cached = projectListCache.get(key);
        if (cached != null && (now - cached.fetchedAt) < PROJECT_LIST_TTL_MS) {
            return cached.projects;
        }
        var response = new RestTemplate().getForObject(aTraits.getUrl() + "/projects.json",
                PubAnnotationProject[].class);
        var projects = response != null ? List.of(response) : List.<PubAnnotationProject> of();
        projectListCache.put(key, new CachedProjects(projects, now));
        return projects;
    }

    private record CachedProjects(List<PubAnnotationProject> projects, long fetchedAt) {}

    List<PubAnnotationDocumentHandle> query(PubAnnotationProviderTraits aTraits, String aQuery,
            int aPage, int aPageSize)
    {
        var variables = new HashMap<String, String>();
        variables.put("keywords", aQuery);
        variables.put("page", Integer.toString(aPage));
        variables.put("per", Integer.toString(aPageSize));

        String url;
        if (isNotBlank(aTraits.getProject())) {
            variables.put("project", aTraits.getProject());
            url = aTraits.getUrl()
                    + "/projects/{project}/docs.json?keywords={keywords}&page={page}&per={per}";
        }
        else {
            url = aTraits.getUrl() + "/docs.json?keywords={keywords}&page={page}&per={per}";
        }

        var restTemplate = new RestTemplate();
        var response = restTemplate.exchange(url, GET, null, PubAnnotationDocumentHandle[].class,
                variables);

        var body = response.getBody();
        return body != null ? asList(body) : emptyList();
    }

    private static boolean isNotBlank(String aValue)
    {
        return aValue != null && !aValue.isBlank();
    }

    @Override
    public List<ExternalSearchResult> executeQuery(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aTraits, String aQuery)
    {
        var response = matchesIdQuery(aQuery).orElseGet(() -> query(aTraits, aQuery, 1, 100));

        var documentSummaries = lookupDocumentSummaries(response);

        var results = new ArrayList<ExternalSearchResult>();
        for (var handle : response) {
            DocSum summary = null;
            try {
                summary = documentSummaries
                        .get(Pair.of(handle.getSourceDb(), parseInt(handle.getSourceId())));
            }
            catch (NumberFormatException e) {
                // Non-numeric source ID — no metadata to look up.
            }
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
                    .toList());
            results.add(result);
        }

        return results;
    }

    private static Optional<List<PubAnnotationDocumentHandle>> matchesIdQuery(String aQuery)
    {
        if (aQuery == null) {
            return Optional.empty();
        }
        var m = ID_QUERY.matcher(aQuery);
        if (!m.matches()) {
            return Optional.empty();
        }
        var handle = new PubAnnotationDocumentHandle();
        handle.setSourceDb(m.group(1));
        handle.setSourceId(stripJsonExtension(m.group(2)));
        return Optional.of(List.of(handle));
    }

    private Map<Pair<String, Integer>, DocSum> lookupDocumentSummaries(
            List<PubAnnotationDocumentHandle> response)
    {
        var documentDetails = new HashMap<Pair<String, Integer>, DocSum>();
        var groupedByDb = response.stream()
                .collect(groupingBy(PubAnnotationDocumentHandle::getSourceDb));
        for (var group : groupedByDb.entrySet()) {
            var db = group.getKey();
            int[] ids;
            try {
                ids = group.getValue().stream() //
                        .map(PubAnnotationDocumentHandle::getSourceId) //
                        .mapToInt(Integer::parseInt).toArray();
            }
            catch (NumberFormatException e) {
                // Non-numeric source IDs aren't supported by entrez esummary; skip the lookup
                // and fall through with no metadata for this group.
                continue;
            }
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
        var doc = getAnnotatedDocument(aTraits, aCollectionId, aDocumentId, aTraits.getProject());
        return doc != null && doc.getText() != null ? doc.getText() : "";
    }

    @Override
    public InputStream getDocumentAsStream(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        var bytes = fetchAnnotationsJson(aTraits, aCollectionId, aDocumentId, aTraits.getProject());
        if (bytes == null) {
            throw new IOException("PubAnnotation returned an empty response for " + aCollectionId
                    + "/" + aDocumentId);
        }
        return new ByteArrayInputStream(bytes);
    }

    private byte[] fetchAnnotationsJson(PubAnnotationProviderTraits aTraits, String aCollectionId,
            String aDocumentId, String aProject)
    {
        var variables = new HashMap<String, String>();
        variables.put("collectionId", aCollectionId);
        variables.put("documentId", stripJsonExtension(aDocumentId));

        String url;
        if (aProject != null && !aProject.isBlank()) {
            variables.put("project", aProject);
            url = aTraits.getUrl()
                    + "/projects/{project}/docs/sourcedb/{collectionId}/sourceid/{documentId}/annotations.json";
        }
        else {
            url = aTraits.getUrl()
                    + "/docs/sourcedb/{collectionId}/sourceid/{documentId}/annotations.json";
        }

        return new RestTemplate().getForObject(url, byte[].class, variables);
    }

    /**
     * Search results carry a {@code .json} suffix on the document id (it becomes the imported
     * file's extension in INCEpTION). Strip it here so it doesn't end up inside the API URL path.
     */
    private static String stripJsonExtension(String aDocumentId)
    {
        return aDocumentId != null && aDocumentId.endsWith(EXT_JSON)
                ? aDocumentId.substring(0, aDocumentId.length() - EXT_JSON.length())
                : aDocumentId;
    }

    @Override
    public String getDocumentFormat(DocumentRepository aRepository,
            PubAnnotationProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        return PubAnnotationAnnotationsFormatSupport.ID;
    }

    /**
     * Fetch the annotated document including text plus any denotations, relations and attributes.
     * If {@code aProject} is non-null, the project-scoped endpoint is used (single track, top-level
     * annotations). Otherwise the global endpoint is used (multi-track response).
     */
    public PubAnnotationDocument getAnnotatedDocument(PubAnnotationProviderTraits aTraits,
            String aCollectionId, String aDocumentId, String aProject)
    {
        var variables = new HashMap<String, String>();
        variables.put("collectionId", aCollectionId);
        variables.put("documentId", stripJsonExtension(aDocumentId));

        String url;
        if (aProject != null && !aProject.isBlank()) {
            variables.put("project", aProject);
            url = aTraits.getUrl()
                    + "/projects/{project}/docs/sourcedb/{collectionId}/sourceid/{documentId}/annotations.json";
        }
        else {
            url = aTraits.getUrl()
                    + "/docs/sourcedb/{collectionId}/sourceid/{documentId}/annotations.json";
        }

        var restTemplate = new RestTemplate();
        return restTemplate.getForObject(url, PubAnnotationDocument.class, variables);
    }
}
