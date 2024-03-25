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
package de.tudarmstadt.ukp.inception.support.markdown;

import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.HtmlStreamEventProcessor;
import org.owasp.html.HtmlStreamEventReceiver;
import org.owasp.html.HtmlStreamEventReceiverWrapper;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import com.github.rjeschke.txtmark.Processor;

public class MarkdownUtil
{
    public static final PolicyFactory STYLED_EXTERNAL_LINKS = new HtmlPolicyBuilder() //
            .withPostprocessor(new Postprocessor()) //
            .toFactory();

    public static final PolicyFactory TERSE_POLICY = Sanitizers.FORMATTING //
            .and(Sanitizers.LINKS) //
            .and(STYLED_EXTERNAL_LINKS);

    private static final String FONT_AWESOME_CLASS = "fa[\\w-]*";
    private static final String BOOTSTRAP_COLOR_CLASS = "text-(?:success|warning|danger)";
    public static final PolicyFactory DEFAULT_POLICY = new HtmlPolicyBuilder() //
            // Allow title="..." on any element.
            .allowAttributes("title").globally() //
            // Defeat link spammers.
            .requireRelNofollowOnLinks() //
            // Allow lang= with an alphabetic value on any element.
            .allowAttributes("lang").matching(Pattern.compile("[a-zA-Z]{2,20}")).globally() //
            // The align attribute on <p> elements can have any value below.
            .allowAttributes("align") //
            .matching(true, "center", "left", "right", "justify", "char") //
            .onElements("caption", "col", "colgroup", "hr", "img", "table", "tbody", "td", "tfoot",
                    "th", "thead", "tr") //
            // Allow a bit of font-awesome
            .allowAttributes("class") //
            .matching(compile(
                    "(?:\\b(?:" + FONT_AWESOME_CLASS + "|" + BOOTSTRAP_COLOR_CLASS + ")\\b\\s*)+")) //
            .onElements("i")
            // These elements are allowed.
            .allowCommonBlockElements() //
            .allowCommonInlineFormattingElements() //
            .allowElements("table", "th", "td", "tr", "tbody", "thead", "col", "colgroup",
                    "caption") //
            .toFactory() //
            .and(Sanitizers.LINKS) //
            .and(Sanitizers.IMAGES) //
            .and(STYLED_EXTERNAL_LINKS);

    public static String markdownToTerseHtml(String aMarkdown)
    {
        if (aMarkdown == null) {
            return null;
        }

        var html = Processor.process(aMarkdown, true);
        return TERSE_POLICY.sanitize(html);
    }

    public static String markdownToHtml(String aMarkdown)
    {
        if (aMarkdown == null) {
            return null;
        }

        var html = Processor.process(aMarkdown, true);
        return DEFAULT_POLICY.sanitize(html);
    }

    private static final class Postprocessor
        implements HtmlStreamEventProcessor
    {

        @Override
        public HtmlStreamEventReceiver wrap(HtmlStreamEventReceiver sink)
        {
            return new OpenExternalLinksInNewWindow(sink);
        }
    }

    static class OpenExternalLinksInNewWindow
        extends HtmlStreamEventReceiverWrapper
    {
        private Stack<Boolean> stack = new Stack<>();

        OpenExternalLinksInNewWindow(HtmlStreamEventReceiver underlying)
        {
            super(underlying);
        }

        @Override
        public void openTag(String elementName, List<String> attribs)
        {
            var externalLink = false;
            var attribsCopy = new ArrayList<>(attribs);
            for (int i = attribsCopy.size() - 2; i >= 0; i -= 2) {
                if ("href".equalsIgnoreCase(attribs.get(i))) {
                    var link = attribs.get(i + 1);
                    if (StringUtils.contains(link, "://")) {
                        externalLink = true;
                        break;
                    }
                }

                if ("target".equalsIgnoreCase(attribs.get(i))) {
                    attribs.remove(i + 1);
                    attribs.remove(i);
                }
            }

            if (externalLink) {
                attribs.add("target");
                attribs.add("_blank");
            }
            stack.push(externalLink);

            underlying.openTag(elementName, attribs);
        }

        @Override
        public void closeTag(String elementName)
        {
            underlying.closeTag(elementName);
            var externalLink = stack.pop();
            if (externalLink) {
                underlying.openTag("i",
                        asList("class", "ms-1 small fas fa-external-link-alt text-muted"));
                underlying.closeTag("i");
            }
        }
    }
}
