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

import static de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryServiceImpl.mergeOverlappingDocChunks;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

public class DocumentQueryServiceImplTest
{
    @Test
    public void mergeOverlappingDocChunks_nonOverlapping_preservesAll()
    {
        var chunks = new ArrayList<Chunk>();

        chunks.add(Chunk.builder() //
                .withDocumentId(1) //
                .withDocumentName("doc") //
                .withSection("sec") //
                .withText("first") //
                .withBegin(0) //
                .withEnd(9) //
                .withScore(1.0) //
                .build());

        chunks.add(Chunk.builder() // second chunk, non-overlapping
                .withDocumentId(1) //
                .withDocumentName("doc") //
                .withSection("sec") //
                .withText("second") //
                .withBegin(20) //
                .withEnd(29) //
                .withScore(1.0) //
                .build());

        chunks.add(Chunk.builder() // third chunk, non-overlapping
                .withDocumentId(1) //
                .withDocumentName("doc") //
                .withSection("sec") //
                .withText("third") //
                .withBegin(40) //
                .withEnd(49) //
                .withScore(1.0) //
                .build());

        var merged = mergeOverlappingDocChunks(chunks);

        // With correct behavior we expect all three non-overlapping chunks to remain
        assertEquals(3, merged.size(),
                "Expected three chunks after merging non-overlapping chunks");
        assertEquals(chunks.get(0), merged.get(0), "First chunk should be preserved");
    }

    @Test
    public void mergeOverlappingDocChunks_overlapping_mergesChunks()
    {
        var chunks = new ArrayList<Chunk>();
        chunks.add(Chunk.builder().withBegin(15).withEnd(25).withText("b").build());
        chunks.add(Chunk.builder().withBegin(10).withEnd(20).withText("a").build());

        var mergedChunks = mergeOverlappingDocChunks(chunks);

        assertEquals(1, mergedChunks.size(), "Expected a single merged chunk");
        assertEquals(10, mergedChunks.get(0).begin());
        assertEquals(25, mergedChunks.get(0).end());
        assertEquals("a\nb", mergedChunks.get(0).text());
    }
}
