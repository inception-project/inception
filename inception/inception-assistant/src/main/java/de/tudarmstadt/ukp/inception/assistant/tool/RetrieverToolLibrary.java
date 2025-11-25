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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.documents.Chunk;
import de.tudarmstadt.ukp.inception.assistant.documents.DocumentContextRetriever;
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse;
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse.Builder;
import de.tudarmstadt.ukp.inception.assistant.model.MReference;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.Tool;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolLibrary;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolParam;

public class RetrieverToolLibrary
    implements ToolLibrary
{
    private static final String TOOL_DESCRIPTION = """
            Retrieves context from the project documents.
            Use when the user requests information.
            Returns a list of text snippets relevant to the specified topic.
            """;
    private static final String PARAM_QUERY_DESCRIPTION = """
            Describes the information or topic the user is interested in.
            """;

    private static final String INSTRUCTIONS = """
            It is absolutely critital to mention the `{{ref::ref-id}}` after each individual information from a source.
            Here is an example:

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

    private final DocumentContextRetriever documentContextRetriever;

    public RetrieverToolLibrary(DocumentContextRetriever aDocumentContextRetriever)
    {
        documentContextRetriever = aDocumentContextRetriever;
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
            value = "context", //
            actor = "Search documents", //
            description = TOOL_DESCRIPTION)
    public MCallResponse.Builder<Map<String, Serializable>> getContext( //
            Project aProject, //
            @ToolParam(value = "query", description = PARAM_QUERY_DESCRIPTION) //
            String aTopic)
        throws IOException
    {
        var chunks = documentContextRetriever.retrieve(aProject, aTopic);

        var references = new LinkedHashMap<Chunk, MReference>();
        var chunkTexts = new ArrayList<Map<String, String>>();
        for (var chunk : chunks) {
            var ref = documentContextRetriever.renderReference(chunk);
            references.put(chunk, ref);

            var data = new LinkedHashMap<String, String>();
            data.put("id", ref.toString());
            data.put("source", chunk.text());
            chunkTexts.add(data);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        Builder<Map<String, Serializable>> callResponse = (Builder) MCallResponse
                .builder(List.class);
        callResponse.withReferences(references.values());
        callResponse.withPayload(Map.of( //
                "instructions", INSTRUCTIONS, //
                "data", chunkTexts));
        return callResponse;
    }
}
