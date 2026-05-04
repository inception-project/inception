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

import static de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings.JavadocToMarkdownConverter.descriptionToMarkdown;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JavadocToMarkdownConverterTest
{
    @Test
    void thatNullAndEmptyArePassedThrough()
    {
        assertThat(descriptionToMarkdown(null)).isNull();
        assertThat(descriptionToMarkdown("")).isEmpty();
    }

    @Test
    void thatPlainTextIsUnchanged()
    {
        assertThat(descriptionToMarkdown("Just some text.")).isEqualTo("Just some text.");
    }

    @Test
    void thatCodeTagBecomesBackticks()
    {
        assertThat(descriptionToMarkdown("Use {@code foo} here.")).isEqualTo("Use `foo` here.");
    }

    @Test
    void thatCodeTagWithNestedBracesIsHandled()
    {
        assertThat(
                descriptionToMarkdown("QName {@code {http://www.w3.org/1998/Math/MathML}math} ok."))
                        .isEqualTo("QName `{http://www.w3.org/1998/Math/MathML}math` ok.");
    }

    @Test
    void thatMultipleNestedBracesAreHandled()
    {
        assertThat(descriptionToMarkdown("{@code a{b{c}d}e}")).isEqualTo("`a{b{c}d}e`");
    }

    @Test
    void thatLinkTagBecomesBackticks()
    {
        assertThat(descriptionToMarkdown("See {@link Foo#bar}.")).isEqualTo("See `Foo#bar`.");
    }

    @Test
    void thatLinkplainTagBecomesBackticks()
    {
        assertThat(descriptionToMarkdown("See {@linkplain Foo bar baz}."))
                .isEqualTo("See `Foo bar baz`.");
    }

    @Test
    void thatLiteralTagStripsBraces()
    {
        assertThat(descriptionToMarkdown("Value is {@literal hello}."))
                .isEqualTo("Value is hello.");
    }

    @Test
    void thatMultipleTagsOnSameLineAreReplaced()
    {
        assertThat(descriptionToMarkdown("{@code a} and {@code b}")).isEqualTo("`a` and `b`");
    }

    @Test
    void thatUnbalancedTagIsLeftAlone()
    {
        // No closing brace - the original text is preserved as-is.
        assertThat(descriptionToMarkdown("Stray {@code never closed"))
                .isEqualTo("Stray {@code never closed");
    }

    @Test
    void thatBrIsConvertedToNewline()
    {
        assertThat(descriptionToMarkdown("a<br>b<br/>c<BR />d")).isEqualTo("a\nb\nc\nd");
    }

    @Test
    void thatParagraphIsConvertedToBlankLine()
    {
        assertThat(descriptionToMarkdown("a<p>b</p>")).isEqualTo("a\n\nb");
    }

    @Test
    void thatListItemsAreConvertedToBullets()
    {
        assertThat(descriptionToMarkdown("Items:<ul><li>one</li><li>two</li></ul>"))
                .isEqualTo("Items:\n- one\n- two\n");
    }

    @Test
    void thatCodeContentSurvivesHtmlStripping()
    {
        assertThat(descriptionToMarkdown("Use {@code List<String>} here."))
                .isEqualTo("Use `List<String>` here.");
    }

    @Test
    void thatLiteralContentSurvivesHtmlStripping()
    {
        assertThat(descriptionToMarkdown("Value is {@literal <foo>&<bar>}."))
                .isEqualTo("Value is <foo>&<bar>.");
    }

    @Test
    void thatRemainingTagsAreStripped()
    {
        assertThat(descriptionToMarkdown("Hello <b>world</b>!")).isEqualTo("Hello world!");
    }

    @Test
    void thatHtmlEntitiesAreDecoded()
    {
        assertThat(descriptionToMarkdown("&lt;a&gt; &amp; &quot;b&quot; &apos;c&apos;"))
                .isEqualTo("<a> & \"b\" 'c'");
    }

    @Test
    void thatAmpEntityIsDecodedLast()
    {
        // &amp;lt; should decode to &lt; (not <), since &amp; is processed last
        assertThat(descriptionToMarkdown("&amp;lt;")).isEqualTo("&lt;");
    }

    @Test
    void thatTagPrefixDoesNotMatchLongerTagName()
    {
        // Ensure {@codex ...} is not matched as {@code ...}
        assertThat(descriptionToMarkdown("{@codex foo}")).isEqualTo("{@codex foo}");
    }
}
