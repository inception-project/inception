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

import static de.tudarmstadt.ukp.inception.assistant.documents.DocumentContextRetriever.mergeOverlappingChunks;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class DocumentContextRetrieverTest
{
    @Test
    void testMergingChunks()
    {
        var chunks = new ArrayList<Chunk>();
        chunks.add(Chunk.builder().withBegin(15).withEnd(25).withText("b").build());
        chunks.add(Chunk.builder().withBegin(10).withEnd(20).withText("a").build());

        var mergedChunks = mergeOverlappingChunks(chunks);

        assertThat(mergedChunks) //
                .containsExactly(
                        Chunk.builder().withBegin(10).withEnd(25).withText("a\nb").build());
    }
}
