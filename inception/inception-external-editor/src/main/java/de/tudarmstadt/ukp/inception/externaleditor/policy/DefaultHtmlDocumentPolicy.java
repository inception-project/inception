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
package de.tudarmstadt.ukp.inception.externaleditor.policy;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.WatchedResourceFile;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollectionBuilder;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollectionIOUtils;

public class DefaultHtmlDocumentPolicy
{
    static final String DEFAULT_POLICY_YAML = "default-policy.yaml";

    private static final Pattern HTML_TITLE = Pattern
            .compile("[\\p{L}\\p{N}\\s\\-_',:\\[\\]!\\./\\\\\\(\\)&]*");
    private static final Pattern HTML_CLASS = Pattern.compile("[a-zA-Z0-9\\s,\\-_]+");

    private final PolicyCollection defaultPolicy;

    private final WatchedResourceFile<PolicyCollection> policyOverrideFile;

    public DefaultHtmlDocumentPolicy() throws IOException
    {
        defaultPolicy = makeDefaultPolicy();

        var appHome = SettingsUtil.getApplicationHome();
        policyOverrideFile = new WatchedResourceFile<>(
                new File(appHome, DEFAULT_POLICY_YAML).toPath(),
                PolicyCollectionIOUtils::loadPolicies);
    }

    private PolicyCollection makeDefaultPolicy()
    {
        return PolicyCollectionBuilder.caseInsensitive() //
                .allowElements("html", "head", "body", "title") //
                // Content sectioning
                .allowElements("address", "article", "aside", "footer", "header", "h1", "h2", "h3",
                        "h4", "h5", "h6", "main", "section") //
                // Text content
                .allowElements("blockquote", "dd", "div", "dl", "dt", "figcaption", "figure", "hr",
                        "li", "menu", "ol", "p", "pre", "ul") //
                // Inline text semantics
                .allowElements("a", "abbr", "b", "bdi", "bdo", "br", "cite", "code", "data", "dfn",
                        "em", "i", "kbd", "mark", "q", "rp", "rt", "ruby", "s", "samp", "small",
                        "span", "strong", "sub", "sup", "time", "u", "var", "wbr")
                // Demarcating edits
                .allowElements("del", "ins")
                // Table content
                .allowElements("caption", "col", "colgroup", "table", "tbody", "td", "tfoot", "th",
                        "thead", "tr")
                // Common attributes
                .allowAttributes("title").matching(HTML_TITLE).globally() //
                .allowAttributes("class").matching(HTML_CLASS).globally() //
                .build();
    }

    public PolicyCollection getPolicy() throws IOException
    {
        return policyOverrideFile.get().orElse(defaultPolicy);
    }
}
