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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.junit.jupiter.api.Test;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingType;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

class CasChunkerTest
{
    @Test
    void testChunking() throws Exception
    {
        var encodingRegistry = Encodings.newLazyEncodingRegistry();

        var sut = new CasChunker(encodingRegistry.getEncoding(EncodingType.CL100K_BASE), 5, 0);

        var cas = JCasFactory.createJCas();

        var text = """
                This is sentence 1.
                This is sentence 2.
                This is sentence 3.
                This is sentence 4.
                This is sentence 5.
                This is sentence 6.
                This is sentence 7.
                This is sentence 8.
                This is sentence 9.
                This is sentence 10.
                """;
        var builder = TokenBuilder.create(Token.class, Sentence.class);
        builder.buildTokens(cas, text);

        var chunks = sut.process(cas.getCas());

        assertThat(chunks) //
                .extracting(Chunk::text) //
                .containsExactly( //
                        "This is sentence 1.", //
                        "This is sentence 2.", //
                        "This is sentence 3.", //
                        "This is sentence 4.", //
                        "This is sentence 5.", //
                        "This is sentence 6.", //
                        "This is sentence 7.", //
                        "This is sentence 8.", //
                        "This is sentence 9.", //
                        "This is sentence 10.");
    }

    @Test
    void testChunkingWithReasonableOverlap() throws Exception
    {
        var encodingRegistry = Encodings.newLazyEncodingRegistry();

        var sut = new CasChunker(encodingRegistry.getEncoding(EncodingType.CL100K_BASE), 20, 1);

        var cas = JCasFactory.createJCas();

        var text = """
                This is sentence 1.
                This is sentence 2.
                This is sentence 3.
                This is sentence 4.
                This is sentence 5.
                This is sentence 6.
                """;
        var builder = TokenBuilder.create(Token.class, Sentence.class);
        builder.buildTokens(cas, text);

        var chunks = sut.process(cas.getCas());

        assertThat(chunks) //
                .extracting(Chunk::text) //
                .containsExactly( //
                        "This is sentence 1.\nThis is sentence 2.\nThis is sentence 3.", //
                        "This is sentence 3.\nThis is sentence 4.\nThis is sentence 5.", //
                        "This is sentence 5.\nThis is sentence 6.");
    }

    @Test
    void thatWithTooMuchOverlapWeStillAlwaysMoveForward() throws Exception
    {
        var encodingRegistry = Encodings.newLazyEncodingRegistry();

        var sut = new CasChunker(encodingRegistry.getEncoding(EncodingType.CL100K_BASE), 20, 4);

        var cas = JCasFactory.createJCas();

        var text = """
                This is sentence 1.
                This is sentence 2.
                This is sentence 3.
                This is sentence 4.
                This is sentence 5.
                This is sentence 6.
                """;
        var builder = TokenBuilder.create(Token.class, Sentence.class);
        builder.buildTokens(cas, text);

        var chunks = sut.process(cas.getCas());

        assertThat(chunks) //
                .extracting(Chunk::text) //
                .containsExactly( //
                        "This is sentence 1.\nThis is sentence 2.\nThis is sentence 3.", //
                        "This is sentence 2.\nThis is sentence 3.\nThis is sentence 4.", //
                        "This is sentence 3.\nThis is sentence 4.\nThis is sentence 5.", //
                        "This is sentence 4.\nThis is sentence 5.\nThis is sentence 6.");
    }
}
