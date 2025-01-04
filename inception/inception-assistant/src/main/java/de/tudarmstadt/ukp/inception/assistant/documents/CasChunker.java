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

import static java.lang.Math.floorDiv;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;

import com.knuddels.jtokkit.api.EncodingRegistry;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.support.text.TrimUtils;

public class CasChunker
    implements Chunker<CAS>
{
    private final EncodingRegistry encodingRegistry;
    private final AssistantProperties properties;

    public CasChunker(EncodingRegistry aEncodingRegistry, AssistantProperties aProperties)
    {
        encodingRegistry = aEncodingRegistry;
        properties = aProperties;
    }

    @Override
    public List<Chunk> process(CAS aCas)
    {
        var docText = aCas.getDocumentText();
        
        var encoding = encodingRegistry.getEncoding(properties.getChat().getEncoding())
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown encoding: " + properties.getChat().getEncoding()));
        var limit = floorDiv(properties.getDocumentIndex().getChunkSize() * 90, 100);

        var unitIterator = aCas.select(Sentence.class) //
                .toList().iterator();

        var chunks = new ArrayList<Chunk>();

        Sentence unit = null;
        var chunk = new StringBuilder();
        var chunkBegin = 0;
        var chunkTokens = 0;
        while (unitIterator.hasNext()) {
            unit = unitIterator.next();
            var unitTokens = encoding.countTokensOrdinary(unit.getCoveredText());

            // Start a new chunk if necessary
            if (chunkTokens + unitTokens > limit) {
                if (!chunk.isEmpty()) {
                    chunks.add(buildChunk(docText, chunkBegin, unit));
                }
                chunk.setLength(0);
                chunkBegin = unit.getBegin();
                chunkTokens = 0;
            }

            chunk.append(unit.getCoveredText());
            chunk.append("\n");
            chunkTokens += unitTokens;
        }

        // Add the final chunk (unless empty)
        if (chunk.length() > 0 && unit != null) {
            chunks.add(buildChunk(docText, chunkBegin, unit));
        }

        return chunks;
    }

    private Chunk buildChunk(String docText, int currentChunkBegin, Sentence unit)
    {
        var range = new int[] {currentChunkBegin, unit.getEnd()};
        TrimUtils.trim(docText, range);
        return Chunk.builder() //
                .withText(docText.substring(range[0], range[1])) //
                .withBegin(range[0]) //
                .withEnd(range[1]) //
                .build();
    }
}
