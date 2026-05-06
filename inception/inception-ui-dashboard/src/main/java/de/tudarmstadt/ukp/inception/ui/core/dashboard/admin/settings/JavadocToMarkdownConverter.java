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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings;

final class JavadocToMarkdownConverter
{
    private JavadocToMarkdownConverter()
    {
        // utility class
    }

    /**
     * Best-effort conversion of common Javadoc constructs in Spring configuration descriptions to
     * Markdown.
     */
    static String descriptionToMarkdown(String aText)
    {
        if (aText == null || aText.isEmpty()) {
            return aText;
        }
        var s = aText;
        // Inline Javadoc tags (balanced-brace aware so {@code {a}b} works)
        s = replaceInlineTag(s, "code", "`", "`");
        s = replaceInlineTag(s, "linkplain", "`", "`");
        s = replaceInlineTag(s, "link", "`", "`");
        s = replaceInlineTag(s, "literal", "", "");
        // Block-level HTML
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");
        s = s.replaceAll("(?i)<p>\\s*", "\n\n");
        s = s.replaceAll("(?i)</p>", "");
        // Lists: turn each <li>...</li> into "- ..." lines
        s = s.replaceAll("(?i)<li>\\s*", "\n- ");
        s = s.replaceAll("(?i)</li>", "");
        s = s.replaceAll("(?is)</?[uo]l>\\s*", "\n");
        // Strip any remaining tags defensively
        s = s.replaceAll("<[^>]+>", "");
        // Decode common HTML entities (decode &amp; last to avoid double-decoding)
        s = s.replace("&lt;", "<") //
                .replace("&gt;", ">") //
                .replace("&quot;", "\"") //
                .replace("&apos;", "'") //
                .replace("&amp;", "&");
        return s;
    }

    static String replaceInlineTag(String aText, String aTag, String aPrefix, String aSuffix)
    {
        var prefix = "{@" + aTag;
        var out = new StringBuilder(aText.length());
        var i = 0;
        while (i < aText.length()) {
            var start = aText.indexOf(prefix, i);
            if (start < 0) {
                out.append(aText, i, aText.length());
                break;
            }
            // Ensure the next character ends the tag name (whitespace or closing brace)
            var afterTag = start + prefix.length();
            if (afterTag >= aText.length() || (!Character.isWhitespace(aText.charAt(afterTag))
                    && aText.charAt(afterTag) != '}')) {
                out.append(aText, i, afterTag);
                i = afterTag;
                continue;
            }
            // Find matching closing brace, accounting for nested braces
            var depth = 1;
            var j = afterTag;
            while (j < aText.length() && depth > 0) {
                var c = aText.charAt(j);
                if (c == '{') {
                    depth++;
                }
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        break;
                    }
                }
                j++;
            }
            if (depth != 0) {
                // Unbalanced - emit verbatim and stop
                out.append(aText, i, aText.length());
                break;
            }
            // Skip leading whitespace after the tag name
            var contentStart = afterTag;
            while (contentStart < j && Character.isWhitespace(aText.charAt(contentStart))) {
                contentStart++;
            }
            out.append(aText, i, start);
            out.append(aPrefix);
            // Escape so the content survives the later HTML stripping. The final
            // entity-decoding pass restores the original characters.
            for (var k = contentStart; k < j; k++) {
                var c = aText.charAt(k);
                switch (c) {
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '&':
                    out.append("&amp;");
                    break;
                default:
                    out.append(c);
                }
            }
            out.append(aSuffix);
            i = j + 1;
        }
        return out.toString();
    }
}
