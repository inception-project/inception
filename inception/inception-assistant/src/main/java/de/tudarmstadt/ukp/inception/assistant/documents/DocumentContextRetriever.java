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

import static com.nimbusds.oauth2.sdk.util.StringUtils.isNotBlank;
import static de.tudarmstadt.ukp.inception.assistant.model.MAssistantChatRoles.SYSTEM;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Locale.ROOT;

import java.util.List;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.model.MAssistantTextMessage;
import de.tudarmstadt.ukp.inception.assistant.retriever.Retriever;

@Order(2000)
public class DocumentContextRetriever
    implements Retriever
{
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
    public List<MAssistantTextMessage> retrieve(String aSessionOwner, Project aProject,
            MAssistantTextMessage aMessage)
    {
        var chunks = documentQueryService.query(aProject, aMessage.message(), 10,
                properties.getEmbedding().getChunkScoreThreshold());

        var body = new StringBuilder();
        for (var chunk : chunks) {
            if (isNotBlank(chunk.documentName())) {
                body.append("Document: `");
                body.append(chunk.documentName());
                body.append("`\n");
            }
            if (isNotBlank(chunk.section())) {
                body.append("Section: `");
                body.append(chunk.section());
                body.append("`\n");
            }
            if (chunk.score() > 0.0) {
                body.append("Score: `");
                body.append(format(ROOT, "%.2f", chunk.score()));
                body.append("`\n");
            }
            body.append("```context\n");
            body.append(chunk.text());
            body.append("\n```\n\n");
        }

        if (body.isEmpty()) {
            return emptyList();
        }

        return asList(MAssistantTextMessage.builder() //
                .withRole(SYSTEM).internal() //
                .withMessage("""
                             Use the context information from the following documents to respond.
                             The source of this information are the authors of the documents.
                             
                             
                             """ + body.toString()) //
                .build());
    }
}
