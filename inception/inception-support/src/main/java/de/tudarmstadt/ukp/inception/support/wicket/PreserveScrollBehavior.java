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

import static java.lang.String.format;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;
import static org.wicketstuff.jquery.core.Options.asString;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;

/**
 * Preserves the scroll position of scrollable elements across Wicket AJAX component replacements.
 * <p>
 * When a component is repainted via AJAX, the browser discards the scroll position of every
 * scrollable element in the replaced subtree, because the replacement nodes are newly created. This
 * is most visible when a repaint targets a container far above the element that actually scrolls -
 * e.g. repainting the annotation page's splitter container also throws away the scroll position of
 * the sidebar panels nested inside it.
 * <p>
 * Add this behavior once to a page and mark the elements whose scroll position should survive with
 * the marker CSS class - {@value #DEFAULT_MARKER_CLASS} by default. Marked elements must have a
 * stable markup id, so call {@code setOutputMarkupId(true)} on the corresponding component.
 */
public class PreserveScrollBehavior
    extends Behavior
{
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_MARKER_CLASS = "preserve-scroll";

    private final String markerClass;

    public PreserveScrollBehavior()
    {
        this(DEFAULT_MARKER_CLASS);
    }

    public PreserveScrollBehavior(String aMarkerClass)
    {
        markerClass = aMarkerClass;
    }

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        super.renderHead(aComponent, aResponse);

        aResponse.render(forReference(PreserveScrollJavaScriptReference.get()));

        var script = format("initInceptionPreserveScroll(%s);", asString(markerClass));

        aResponse.render(OnDomReadyHeaderItem.forScript(script));
    }
}
