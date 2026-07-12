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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.client;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.JsonResponseSanitizer.stripCodeFence;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonResponseSanitizerTest
{
    @Test
    void thatLanguageTaggedFenceIsStripped()
    {
        // The exact shape observed from qwen3.5:4b in Ollama JSON mode.
        var fenced = "```json\n{\n  \"a\": 1,\n  \"b\": 2\n}\n```";

        assertThat(stripCodeFence(fenced)).isEqualTo("{\n  \"a\": 1,\n  \"b\": 2\n}");
    }

    @Test
    void thatPlainFenceIsStripped()
    {
        assertThat(stripCodeFence("```\n{\"a\":1}\n```")).isEqualTo("{\"a\":1}");
    }

    @Test
    void thatTildeFenceIsStripped()
    {
        assertThat(stripCodeFence("~~~json\n{\"a\":1}\n~~~")).isEqualTo("{\"a\":1}");
    }

    @Test
    void thatSurroundingWhitespaceIsTolerated()
    {
        assertThat(stripCodeFence("  \n```json\n{\"a\":1}\n```  \n")).isEqualTo("{\"a\":1}");
    }

    @Test
    void thatUnfencedJsonIsUnchanged()
    {
        assertThat(stripCodeFence("{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void thatContentWithAnInlineFenceIsUnchanged()
    {
        // Not a whole-string fenced block, so it must be left alone.
        var prose = "Here is the answer:\n```json\n{\"a\":1}\n```\nHope that helps!";

        assertThat(stripCodeFence(prose)).isEqualTo(prose);
    }

    @Test
    void thatNullIsReturnedAsIs()
    {
        assertThat(stripCodeFence(null)).isNull();
    }

    @Test
    void thatEmptyStringIsUnchanged()
    {
        assertThat(stripCodeFence("")).isEmpty();
    }
}
