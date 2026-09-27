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
package de.tudarmstadt.ukp.inception.support.wicket;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.wicket.response.filter.IResponseFilter;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-mode check for invisible placeholder tags rendered as {@code <wicket:...>} elements.
 * <p>
 * When a component with {@code setOutputMarkupPlaceholderTag(true)} is invisible, Wicket renders a
 * placeholder using the tag name from the markup - including the {@code wicket} namespace, because
 * {@code Component#renderPlaceholderTag} does not strip wicket tags. A component bound to
 * {@code <wicket:container>} therefore sends a literal
 * {@code <wicket:container hidden="" data-wicket-placeholder="">} to the browser, in development
 * and deployment mode alike. The browser turns it into an unknown element, so any CSS that sets
 * {@code display} on it overrides {@code hidden}.
 * <p>
 * The fix is to host such components in a real element ({@code <span>} or {@code <div>}). This
 * filter only reports offenders; it does not change the response. It matches the placeholder form
 * only, because with {@code stripWicketTags} off (the development-mode default), visible
 * {@code <wicket:...>} tags are legitimately present in the output.
 */
public class WicketTagPlaceholderCheckFilter
    implements IResponseFilter
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final WicketTagPlaceholderCheckFilter INSTANCE = new WicketTagPlaceholderCheckFilter();

    private static final Pattern WICKET_TAG_PLACEHOLDER = Pattern
            .compile("<wicket:[\\w-]+\\s[^>]*data-wicket-placeholder[^>]*>");

    private static final Pattern MARKUP_ID = Pattern.compile("\\sid=\"([^\"]*)\"");

    @Override
    public AppendingStringBuffer filter(AppendingStringBuffer aResponseBuffer)
    {
        for (var tag : findWicketTagPlaceholders(aResponseBuffer)) {
            var id = MARKUP_ID.matcher(tag);
            LOG.warn("Invisible component [{}] rendered its placeholder as a wicket tag: {} - host "
                    + "the component in a real element such as <span> or <div> instead of "
                    + "<wicket:container>", id.find() ? id.group(1) : "?", tag);
        }

        return aResponseBuffer;
    }

    static List<String> findWicketTagPlaceholders(CharSequence aMarkup)
    {
        var tags = new ArrayList<String>();
        var matcher = WICKET_TAG_PLACEHOLDER.matcher(aMarkup);
        while (matcher.find()) {
            tags.add(matcher.group());
        }
        return tags;
    }
}
