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

import java.util.regex.Pattern;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import com.github.rjeschke.txtmark.Processor;

public class MarkdownUtil
{
    public static final PolicyFactory TERSE_POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    public static final PolicyFactory DEFAULT_POLICY = new HtmlPolicyBuilder() //
            .allowStandardUrlProtocols() //
            // Allow title="..." on any element.
            .allowAttributes("title").globally() //
            // Allow href="..." on <a> elements.
            .allowAttributes("href").onElements("a") //
            // Defeat link spammers.
            .requireRelNofollowOnLinks() //
            // Allow lang= with an alphabetic value on any element.
            .allowAttributes("lang").matching(Pattern.compile("[a-zA-Z]{2,20}")).globally() //
            // The align attribute on <p> elements can have any value below.
            .allowAttributes("align") //
            .matching(true, "center", "left", "right", "justify", "char") //
            .onElements("p") //
            // These elements are allowed.
            .allowElements("a", "p", "div", "i", "b", "em", "blockquote", "tt", "strong", "br",
                    "ul", "ol", "li")
            // Custom slashdot tags.
            // These could be rewritten in the sanitizer using an ElementPolicy.
            .allowElements("quote", "ecode") //
            .toFactory() //
            .and(Sanitizers.IMAGES);

    public static String markdownToTerseHtml(String aMarkdown)
    {
        var html = Processor.process(aMarkdown, true);
        return TERSE_POLICY.sanitize(html);
    }

    public static String markdownToHtml(String aMarkdown)
    {
        var html = Processor.process(aMarkdown, true);
        return DEFAULT_POLICY.sanitize(html);
    }
}
