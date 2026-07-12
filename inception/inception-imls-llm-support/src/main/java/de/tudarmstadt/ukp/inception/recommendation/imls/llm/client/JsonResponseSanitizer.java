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

import java.util.regex.Pattern;

/**
 * Cleans up model chat content that was requested as JSON but came back wrapped in a Markdown code
 * fence. Some models (observed with qwen3.5 in Ollama's JSON mode) emit
 * <code>```json\n{...}\n```</code> even when a JSON response format or schema was requested, which
 * makes a downstream {@code readTree}/{@code readValue} of the content fail. The adapters call this
 * only when JSON output was requested, so ordinary prose that happens to contain a fenced block is
 * never touched.
 */
public final class JsonResponseSanitizer
{
    // Optional leading fence (``` or ~~~) with an optional language tag on the same line, and the
    // matching trailing fence. DOTALL so the JSON body may span multiple lines; the body is
    // captured reluctantly so a trailing fence is not swallowed into it.
    private static final Pattern FENCED = Pattern.compile( //
            "^\\s*(?:```|~~~)[ \\t]*[a-zA-Z0-9_-]*[ \\t]*\\R(.*?)\\R?[ \\t]*(?:```|~~~)\\s*$", //
            Pattern.DOTALL);

    private JsonResponseSanitizer()
    {
        // No instances
    }

    /**
     * Strips a single surrounding Markdown code fence from {@code aContent} if the whole string is
     * exactly one fenced block; otherwise returns {@code aContent} unchanged. {@code null} is
     * returned as-is.
     *
     * @param aContent
     *            the raw model content, possibly fence-wrapped
     * @return the unwrapped content, or the original when there is no enclosing fence
     */
    public static String stripCodeFence(String aContent)
    {
        if (aContent == null) {
            return null;
        }

        var matcher = FENCED.matcher(aContent);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        return aContent;
    }

    /**
     * Convenience guard for the common adapter case: only unwrap the fence when JSON output was
     * actually requested. Some models wrap JSON-mode output in a Markdown code fence; unwrapping it
     * lets downstream JSON parsing of the content succeed. When {@code aJsonRequested} is
     * {@code false} the content is returned untouched, so ordinary prose that happens to contain a
     * fenced block is never altered.
     *
     * @param aJsonRequested
     *            whether the caller requested JSON output (see
     *            {@link ChatOptions#isJsonRequested()})
     * @param aContent
     *            the raw model content, possibly fence-wrapped
     * @return the unwrapped content when JSON was requested and a fence was present; otherwise
     *         {@code aContent} unchanged
     */
    public static String stripCodeFenceIf(boolean aJsonRequested, String aContent)
    {
        return aJsonRequested ? stripCodeFence(aContent) : aContent;
    }
}
