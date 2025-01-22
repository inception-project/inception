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

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.tcas.Annotation;

import com.knuddels.jtokkit.api.Encoding;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.support.text.TrimUtils;

public class CasChunker
    implements Chunker<CAS>
{
    private Class<? extends Annotation> unitType = Sentence.class;
    private final Encoding encoding;
    private final int chunkSize;
    private final int unitOverlap;

    public CasChunker(Encoding aEncoding, int aChunkSize, int aUnitOverlap)
    {
        encoding = aEncoding;
        chunkSize = aChunkSize;
        unitOverlap = aUnitOverlap;
    }

    @Override
    public List<Chunk> process(CAS aCas)
    {
        var docText = aCas.getDocumentText();

        var unitIterator = aCas.select(unitType) //
                .toList().listIterator();

        var chunks = new ArrayList<Chunk>();

        Annotation unit = null;
        var chunkText = new StringBuilder();
        var chunkBegin = 0;
        var chunkEnd = 0;
        var chunkTokens = 0;
        while (unitIterator.hasNext()) {
            unit = unitIterator.next();
            var unitTokens = encoding.countTokensOrdinary(unit.getCoveredText());

            // Does the unit still fit into the current chunk? If not, start a new chunk
            if (chunkTokens + unitTokens > chunkSize && !chunkText.isEmpty()) {
                chunks.add(buildChunk(docText, chunkBegin, chunkEnd));

                var rewindCount = 0;
                while (rewindCount <= unitOverlap && unitIterator.hasPrevious()) {
                    rewindCount++;
                    unit = unitIterator.previous();
                    if (unit.getBegin() <= chunkBegin) {
                        // We cannot go back further than the start of the current chunk, otherwise
                        // we would just repeat the entire chunk
                        unit = unitIterator.next();
                        break;
                    }
                }
                unit = unitIterator.next();

                chunkText.setLength(0);
                chunkBegin = unit.getBegin();
                chunkTokens = 0;
                unitTokens = encoding.countTokensOrdinary(unit.getCoveredText());
            }

            chunkText.append(unit.getCoveredText());
            chunkText.append("\n");
            chunkTokens += unitTokens;
            chunkEnd = unit.getEnd();
        }

        // Add the final chunk (unless empty)
        if (chunkText.length() > 0 && unit != null) {
            chunks.add(buildChunk(docText, chunkBegin, unit.getEnd()));
        }

        return chunks;
    }

    private Chunk buildChunk(String docText, int aBegin, int aEnd)
    {
        var range = new int[] { aBegin, aEnd };
        TrimUtils.trim(docText, range);
        return Chunk.builder() //
                .withText(docText.substring(range[0], range[1])) //
                .withBegin(range[0]) //
                .withEnd(range[1]) //
                .build();
    }
}
