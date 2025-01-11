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
package de.tudarmstadt.ukp.inception.assistant.documents;

import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.SYSTEM;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.ChatContext;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.model.MReference;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.assistant.retriever.Retriever;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Order(2000)
public class DocumentContextRetriever
    implements Retriever
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DocumentQueryService documentQueryService;
    private final AssistantProperties properties;

    public DocumentContextRetriever(DocumentQueryService aDocumentQueryService,
            AssistantProperties aProperties)
    {
        documentQueryService = aDocumentQueryService;
        properties = aProperties;
    }

    @Override
    public String getId()
    {
        return DocumentContextRetriever.class.getSimpleName();
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return true;
    }

    @Override
    public List<MTextMessage> retrieve(ChatContext aAssistant, MTextMessage aMessage)
    {
        var project = aAssistant.getProject();
        var chunks = documentQueryService.query(project, aMessage.message(),
                properties.getDocumentIndex().getMaxChunks(),
                properties.getDocumentIndex().getMinScore());

        var references = new LinkedHashMap<Chunk, MReference>();
        var body = new StringBuilder();
        for (var chunk : chunks) {
            var reference = MReference.builder() //
                    //.withId(String.valueOf(references.size() + 1)) //
                    .withId(UUID.randomUUID().toString().substring(0,8)) //
                    .withDocumentId(chunk.documentId()) //
                    .withDocumentName(chunk.documentName()) //
                    .withBegin(chunk.begin()) //
                    .withEnd(chunk.end()) //
                    .withScore(chunk.score()) //
                    .build();
            references.put(chunk, reference);
            renderChunkJson(body, chunk, reference);
        }

        if (body.isEmpty()) {
            return emptyList();
        }

        var msg = MTextMessage.builder() //
                .withActor("Document context retriever") //
                .withRole(SYSTEM).internal() //
                .withReferences(references.values());

        // Works good with qwen72b but not with granite 8b
//        msg.withMessage(join("\n", asList(
//                "The document retriever found the following relevant information in the documents of this project.",
//                "", //
//                body.toString(), "",
//                "It is critical to mention the source of each document text in the form `{{ref::ref-id}}`.")));

        msg.withMessage(join("\n", asList(
                """
                Use the following documents from this project to respond.
                It is absolutely critital to mention the `{{ref::ref-id}}` after each individual information from a document.
                Here is an example of how to include the ref-id:

                Input:
                {
                  "document": "The Eiffel Tower is located in Paris, France.",
                  "ref-id": "917"
                }
                {
                  "document": "It is one of the most famous landmarks in the world.",
                  "ref-id": "735"
                }
                {
                  "document": The Eiffel Tower was built from 1887 to 1889.",
                  "ref-id": "582"
                }
                
                Response:
                The Eiffel Tower is a famous landmark located in Paris, France {{ref::917}} {{ref::735}}.
                It was built from 1887 to 1889 {{ref::582}}.
                
                Now, use the same pattern to process the following document:
                """,
                "", //
                body.toString())));

        return asList(msg.build());
    }

    private void renderChunkJson(StringBuilder body, Chunk chunk, MReference aReference)
    {
        try {
            var data = new LinkedHashMap<String, String>();
            data.put("document", chunk.text());
            data.put("ref-id", aReference.id());
            data.entrySet().removeIf(e -> isBlank(e.getValue()));
            body.append(JSONUtil.toPrettyJsonString(data));
            body.append("\n");
        }
        catch (IOException e) {
            LOG.error("Unable to render chunk", e);
        }
    }
}
