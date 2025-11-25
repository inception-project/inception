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
import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
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
    public Collection<String> getSystemPrompts()
    {
        return asList(
                """
                        The source context retriever automatically provides you with relevant information from the
                        documents the current project.
                        Use the following sources from this project to respond.
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

                        Now, use the same pattern to process the following sources:
                        """);
    }

    public List<Chunk> retrieve(Project aProject, String aQuery)
    {
        var chunks = documentQueryService.query(aProject, aQuery,
                properties.getDocumentIndex().getMaxChunks(),
                properties.getDocumentIndex().getMinScore());

        return mergeOverlappingChunks(chunks);
    }

    @Override
    public List<MTextMessage> retrieve(Project aProject, MTextMessage aMessage)
    {
        var chunks = retrieve(aProject, aMessage.message());

        var references = new LinkedHashMap<Chunk, MReference>();
        var body = new StringBuilder();
        for (var chunk : chunks) {
            var ref = renderReference(chunk);
            references.put(chunk, ref);
            renderChunkJson(body, chunk, ref);
        }

        if (body.isEmpty()) {
            return asList(MTextMessage.builder() //
                    .withActor("Source context retriever") //
                    .withRole(SYSTEM).internal().ephemeral() //
                    .withMessage(
                            "The source context retriever found no relevant information in the documents of the current project.") //
                    .build());
        }

        var msg = MTextMessage.builder() //
                .withActor("Source context retriever") //
                .withRole(SYSTEM).internal().ephemeral() //
                .withReferences(references.values());

        msg.withMessage(body.toString());

        return asList(msg.build());
    }

    public MReference renderReference(Chunk chunk)
    {
        return MReference.builder() //
                // .withId(String.valueOf(references.size() + 1)) //
                .withId(UUID.randomUUID().toString().substring(0, 8)) //
                .withDocumentId(chunk.documentId()) //
                .withDocumentName(chunk.documentName()) //
                .withBegin(chunk.begin()) //
                .withEnd(chunk.end()) //
                .withScore(chunk.score()) //
                .build();
    }

    private List<Chunk> mergeOverlappingChunks(List<Chunk> aChunks)
    {
        var chunksByDocument = aChunks.stream().collect(groupingBy(Chunk::documentId,
                LinkedHashMap::new, mapping(identity(), toCollection(ArrayList::new))));

        var mergedChunks = new ArrayList<Chunk>();
        for (var docChunkSet : chunksByDocument.values()) {
            mergedChunks.addAll(mergeOverlappingChunks(docChunkSet));
        }

        sort(mergedChunks, comparing(Chunk::score).reversed());

        return mergedChunks;
    }

    /**
     * @return list of overlapping chunks merged (as long as they are in the same section.
     */
    static List<Chunk> mergeOverlappingChunks(ArrayList<Chunk> aDocChunks)
    {
        if (aDocChunks.size() < 2) {
            return aDocChunks;
        }

        sort(aDocChunks, comparing(Chunk::begin));

        var mergedChunks = new ArrayList<Chunk>();
        var docChunkIter = aDocChunks.iterator();
        var prevChunk = docChunkIter.next();
        while (docChunkIter.hasNext()) {
            var chunk = docChunkIter.next();
            if (overlapping(prevChunk.begin(), prevChunk.end(), chunk.begin(), chunk.end())
                    && Objects.equals(prevChunk.section(), chunk.section())) {
                prevChunk = prevChunk.merge(chunk);

                if (!docChunkIter.hasNext()) {
                    mergedChunks.add(prevChunk);
                }
            }
            else {
                prevChunk = chunk;

                mergedChunks.add(prevChunk);
            }
        }

        return mergedChunks;
    }

    public void renderChunkJson(StringBuilder body, Chunk chunk, MReference aReference)
    {
        try {
            var data = new LinkedHashMap<String, String>();
            data.put("id", aReference.toString());
            data.put("source", chunk.text());
            data.entrySet().removeIf(e -> isBlank(e.getValue()));
            body.append(JSONUtil.toPrettyJsonString(data));
            body.append("\n");
        }
        catch (IOException e) {
            LOG.error("Unable to render chunk", e);
        }
    }
}
