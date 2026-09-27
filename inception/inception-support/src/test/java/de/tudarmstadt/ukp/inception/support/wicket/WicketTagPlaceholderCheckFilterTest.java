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

import static de.tudarmstadt.ukp.inception.support.wicket.WicketTagPlaceholderCheckFilter.findWicketTagPlaceholders;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.wicket.util.string.AppendingStringBuffer;
import org.junit.jupiter.api.Test;

class WicketTagPlaceholderCheckFilterTest
{
    @Test
    void thatWicketTagPlaceholderIsFound()
    {
        // Exactly what Component#renderPlaceholderTag writes for an invisible component bound to
        // <wicket:container>
        var markup = "<div><wicket:container id=\"documentStatusBadges1a\" hidden=\"\" "
                + "data-wicket-placeholder=\"\"></wicket:container></div>";

        assertThat(findWicketTagPlaceholders(markup)) //
                .containsExactly("<wicket:container id=\"documentStatusBadges1a\" hidden=\"\" "
                        + "data-wicket-placeholder=\"\">");
    }

    @Test
    void thatPlaceholderInRealElementIsIgnored()
    {
        var markup = "<span id=\"documentStatusBadges1a\" hidden=\"\" "
                + "data-wicket-placeholder=\"\"></span>";

        assertThat(findWicketTagPlaceholders(markup)).isEmpty();
    }

    @Test
    void thatVisibleWicketTagIsIgnored()
    {
        // With stripWicketTags off (the development-mode default), visible wicket tags are
        // rendered as-is and are not a defect
        var markup = "<wicket:container wicket:id=\"badges\"><span>1/3 incomplete</span>"
                + "</wicket:container><wicket:panel><div data-wicket-placeholder></div>"
                + "</wicket:panel>";

        assertThat(findWicketTagPlaceholders(markup)).isEmpty();
    }

    @Test
    void thatAllPlaceholdersInAjaxResponseAreFound()
    {
        var markup = "<ajax-response>" //
                + "<component id=\"a1\"><![CDATA[<wicket:container id=\"a1\" hidden=\"\" "
                + "data-wicket-placeholder=\"\"></wicket:container>]]></component>"
                + "<component id=\"b2\"><![CDATA[<wicket:enclosure id=\"b2\" hidden=\"\" "
                + "data-wicket-placeholder=\"\"></wicket:enclosure>]]></component>"
                + "</ajax-response>";

        assertThat(findWicketTagPlaceholders(markup)).hasSize(2);
    }

    @Test
    void thatFilterLeavesResponseUnchanged()
    {
        var markup = "<wicket:container id=\"a1\" hidden=\"\" data-wicket-placeholder=\"\">"
                + "</wicket:container>";
        var buffer = new AppendingStringBuffer(markup);

        var result = WicketTagPlaceholderCheckFilter.INSTANCE.filter(buffer);

        assertThat(result).isSameAs(buffer);
        assertThat(result.toString()).isEqualTo(markup);
    }
}
