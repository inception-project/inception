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
package de.tudarmstadt.ukp.inception.support.kendo;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.widget.splitter.ISplitterListener;
import org.wicketstuff.kendo.ui.widget.splitter.SplitterAdapter;
import org.wicketstuff.kendo.ui.widget.splitter.SplitterBehavior;

/**
 * A {@link SplitterBehavior} that additionally reports pane size changes back to the server via an
 * Ajax callback. Subclasses implement {@link #onResize(AjaxRequestTarget, double[])} to react.
 * <p>
 * Sizes are reported as fractional percentages of the splitter's total width (or height), one
 * entry per pane, in DOM order. The callback is invoked only when the size vector actually changes
 * compared to the previous report, so layout reflows do not generate spurious requests.
 */
public abstract class AjaxSplitterBehavior
    extends SplitterBehavior
{
    private static final long serialVersionUID = 1L;

    public enum Orientation
    {
        HORIZONTAL, VERTICAL;

        String optionValue()
        {
            return name().toLowerCase();
        }
    }

    private final Orientation orientation;
    private AbstractDefaultAjaxBehavior resizeBehavior;

    public AjaxSplitterBehavior(String aSelector, Orientation aOrientation, Options aOptions)
    {
        this(aSelector, aOrientation, aOptions, new SplitterAdapter());
    }

    public AjaxSplitterBehavior(String aSelector, Orientation aOrientation, Options aOptions,
            ISplitterListener aListener)
    {
        super(aSelector, withOrientation(aOptions, aOrientation), aListener);
        orientation = aOrientation;
    }

    private static Options withOrientation(Options aOptions, Orientation aOrientation)
    {
        var options = aOptions != null ? aOptions : new Options();
        options.set("orientation", Options.asString(aOrientation.optionValue()));
        return options;
    }

    @Override
    public void bind(Component aComponent)
    {
        super.bind(aComponent);

        aComponent.setOutputMarkupId(true);

        resizeBehavior = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
                var raw = aComponent.getRequest().getRequestParameters()
                        .getParameterValue("sizes").toString("");
                if (raw.isEmpty()) {
                    return;
                }

                var parts = raw.split(",");
                var sizes = new double[parts.length];
                try {
                    for (int i = 0; i < parts.length; i++) {
                        sizes[i] = Double.parseDouble(parts[i]);
                    }
                }
                catch (NumberFormatException e) {
                    return;
                }

                onResize(aTarget, sizes);
            }
        };
        aComponent.add(resizeBehavior);
    }

    /**
     * Triggered when the user finishes dragging a splitter pane handle.
     *
     * @param aTarget
     *            the Ajax request target
     * @param aSizesPercent
     *            percentage size of each pane (0-100), in DOM order
     */
    protected abstract void onResize(AjaxRequestTarget aTarget, double[] aSizesPercent);

    /**
     * Schedule destruction of the Kendo splitter widget bound to this behavior. Call this before
     * replacing the splitter's DOM in an Ajax response so the lingering widget reference does not
     * leave the new pane elements briefly unstyled during the swap.
     */
    public void destroy(AjaxRequestTarget aTarget)
    {
        aTarget.prependJavaScript(
                String.format("destroyInceptionAjaxSplitter(%s);", Options.asString(selector)));
    }

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        super.renderHead(aComponent, aResponse);

        aResponse.render(JavaScriptHeaderItem.forReference(AjaxSplitterJavaScriptReference.get()));

        var script = String.format("initInceptionAjaxSplitter(%s, %s, %s);",
                Options.asString(selector),
                Options.asString(resizeBehavior.getCallbackUrl()),
                Options.asString(orientation.optionValue()));
        aResponse.render(OnDomReadyHeaderItem.forScript(script));
    }
}
