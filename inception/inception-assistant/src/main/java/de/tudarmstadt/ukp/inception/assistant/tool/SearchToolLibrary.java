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
package de.tudarmstadt.ukp.inception.assistant.tool;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.documents.Chunk;
import de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryService;
import de.tudarmstadt.ukp.inception.assistant.documents.SearchResult;
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse;
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse.Builder;
import de.tudarmstadt.ukp.inception.assistant.model.MReference;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationEditorContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.Tool;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolLibrary;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolParam;

public class SearchToolLibrary
    implements ToolLibrary
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String TOOL_DESCRIPTION = """
            Retrieves context from the project documents.
            Use when the user requests information.
            Returns a list of text snippets relevant to the specified topic.
            """;
    private static final String PARAM_QUERY_DESCRIPTION = """
            Describes the information or topic the user is interested in.
            """;

    private static final String INSTRUCTIONS = """
            When using information returned by a search tool, it is absolutely critital to mention the `{{ref::ref-id}}`
            after each individual information from a source. Here is an example:

            Input:
            {
              "id": "{{ref::917}}"
              "source": "The Eiffel Tower is located in Paris, France.",
            }
            {
              "id": "{{ref::735}}"
              "source": "It is one of the most famous landmarks in the world.",
            }
            {
              "id": "{{ref::582}}"
              "source": The Eiffel Tower was built from 1887 to 1889.",
            }

            Response:
            The Eiffel Tower is a famous landmark located in Paris, France {{ref::917}} {{ref::735}}.
            It was built from 1887 to 1889 {{ref::582}}.
            """;

    private final DocumentQueryService documentQueryService;
    private final AssistantProperties properties;

    public SearchToolLibrary(AssistantProperties aProperties,
            DocumentQueryService aDocumentQueryService)
    {
        properties = aProperties;
        documentQueryService = aDocumentQueryService;
    }

    @Override
    public Collection<String> getSystemPrompts(Project aProject)
    {
        return asList(INSTRUCTIONS);
    }

    @Override
    public String getId()
    {
        return getClass().getName();
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return true;
    }

    @Tool( //
            value = "semantic_search", //
            actor = "Semantic search", //
            description = TOOL_DESCRIPTION)
    public MCallResponse.Builder<?> semanticSearch( //
            Project aProject, //
            @ToolParam(value = "query", description = PARAM_QUERY_DESCRIPTION) //
            String aTopic)
        throws IOException
    {
        var result = documentQueryService.semanticQuery(aProject, aTopic,
                properties.getDocumentIndex().getMaxChunks(),
                properties.getDocumentIndex().getMinScore());

        List<Chunk> chunks = result != null ? result.matches() : List.<Chunk> of();

        var references = new LinkedHashMap<Chunk, MReference>();
        var chunkTexts = new ArrayList<Map<String, String>>();
        for (var chunk : chunks) {
            var ref = chunk.toReference();
            references.put(chunk, ref);

            var data = new LinkedHashMap<String, String>();
            data.put("id", ref.toString());
            data.put("source", chunk.text());
            chunkTexts.add(data);
        }

        if (chunkTexts.isEmpty()) {
            return MCallResponse.builder(String.class) //
                    .withActor("Searching (semantic): " + aTopic) //
                    .withPayload("The search has yielded no results.");
        }

        Builder<Map<String, Serializable>> callResponse = MCallResponse.builder();
        callResponse.withActor("Searching (semantic): " + aTopic);
        callResponse.withReferences(references.values());
        callResponse.withPayload(Map.of( //
                "matches", chunkTexts, //
                "truncated", result != null ? result.truncated().orElse(false) : false));
        return callResponse;
    }

    @Tool( //
            value = "keyword_search", //
            actor = "Keyword search", //
            description = TOOL_DESCRIPTION)
    public MCallResponse.Builder<?> keywordSearch( //
            AnnotationEditorContext aContext, //
            @ToolParam(value = "query", description = PARAM_QUERY_DESCRIPTION) //
            String aQuery,
            @ToolParam(value = "from", description = "Index of first result (1-based)") //
            Integer aFrom, //
            @ToolParam(value = "to", description = "Index of last result (1-based)") //
            Integer aTo)
        throws IOException
    {
        // Prefer the assistant's document index for keyword queries so tokenization
        // and stored fields are consistent with production indexing.
        var maxChunks = properties.getDocumentIndex().getMaxChunks();

        SearchResult result;
        List<Chunk> chunks;
        try {
            result = documentQueryService.keywordQuery(aContext.getProject(), aQuery, maxChunks);
            chunks = result != null ? result.matches() : emptyList();
        }
        catch (Exception e) {
            LOG.error("Keyword search failed", e);
            return MCallResponse.builder(String.class) //
                    .withActor("Searching (keyword): " + aQuery) //
                    .withPayload("The search has failed.");
        }

        // If limited to a specific document, filter results
        if (aContext.getDocument() != null) {
            var docId = aContext.getDocument().getId();
            chunks = chunks.stream().filter(c -> c.documentId() == docId).toList();
        }

        // Apply from/to pagination (preserve original behavior)
        var fromIdx = (aFrom != null && aFrom > 0) ? aFrom - 1 : 0;
        var toExclusive = chunks.size();
        if (aTo != null && aTo > 0) {
            int computedLimit;
            if (aFrom != null && aFrom > 0) {
                // inclusive range [from, to] -> count = to - from + 1
                computedLimit = aTo - aFrom + 1;
            }
            else {
                // legacy behavior: if only 'to' provided, treat it as a count
                computedLimit = aTo;
            }
            var end = Math.min(chunks.size(), fromIdx + Math.max(computedLimit, 0));
            toExclusive = Math.max(fromIdx, end);
        }
        if (fromIdx > chunks.size()) {
            chunks = List.of();
        }
        else if (!(fromIdx == 0 && toExclusive == chunks.size())) {
            chunks = chunks.subList(fromIdx, toExclusive);
        }

        var references = new LinkedHashMap<Chunk, MReference>();
        var chunkTexts = new ArrayList<Map<String, String>>();
        for (var chunk : chunks) {
            var ref = chunk.toReference();
            references.put(chunk, ref);

            var data = new LinkedHashMap<String, String>();
            data.put("id", ref.toString());
            data.put("source", chunk.text());
            chunkTexts.add(data);
        }

        if (chunkTexts.isEmpty()) {
            return MCallResponse.builder(String.class) //
                    .withActor("Searching (keyword): " + aQuery) //
                    .withPayload("The search has yielded no results.");
        }

        Builder<Map<String, Serializable>> callResponse = MCallResponse.builder();
        callResponse.withActor("Searching (keyword): " + aQuery);
        callResponse.withReferences(references.values());
        callResponse.withPayload(Map.of( //
                "matches", chunkTexts, //
                "totalMatches", result != null ? result.totalMatches().orElse(0) : 0));
        return callResponse;
    }
}
