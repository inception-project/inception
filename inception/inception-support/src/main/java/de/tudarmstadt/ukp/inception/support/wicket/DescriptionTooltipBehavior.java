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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.jquery.ui.widget.tooltip.CustomTooltipBehavior;

import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;

public class DescriptionTooltipBehavior
    extends CustomTooltipBehavior
{
    private static final long serialVersionUID = 1L;

    public enum Mode
    {
        DEFAULT, MARKDOWN
    }

    @SuppressWarnings("unused")
    private final String title;

    private final String description;

    private Mode mode = Mode.DEFAULT;

    /**
     * Font Awesome icon class rendered in front of the title, e.g. {@code fa-exclamation-triangle}.
     * Used to repeat the icon shown on the element the tooltip belongs to, so the relation between
     * the two is visible. This is a fixed CSS class rather than free-form markup - the title itself
     * stays escaped because callers pass user-controlled content into it.
     */
    private String titleIcon;

    private String titleIconCssClass;

    public DescriptionTooltipBehavior(String aTitle, String aDescription)
    {
        super(makeTooltipOptions());
        title = aTitle;
        if (StringUtils.isBlank(aDescription)) {
            description = "no description";
        }
        else {
            description = aDescription;
        }
    }

    public void setMode(Mode aMode)
    {
        mode = aMode;
    }

    public Mode getMode()
    {
        return mode;
    }

    /**
     * Renders an icon in front of the tooltip title.
     *
     * @param aIcon
     *            a Font Awesome icon class, e.g. {@code fa-exclamation-triangle}.
     * @param aCssClass
     *            additional CSS classes for the icon, e.g. {@code text-warning}. May be null.
     * @return this behavior, for chaining.
     */
    public DescriptionTooltipBehavior withTitleIcon(String aIcon, String aCssClass)
    {
        titleIcon = aIcon;
        titleIconCssClass = aCssClass;
        return this;
    }

    @Override
    protected WebMarkupContainer newContent(String markupId)
    {
        return new DescriptionTooltipPanel(markupId, Model.of(this));
    }

    public static Options makeTooltipOptions()
    {
        Options options = new Options();
        options.set("position", "{ my: 'center bottom', at: 'center top', of: '.page-footer' }");
        options.set("classes", "{ 'ui-tooltip': 'ui-description-tooltip ui-corner-all shadow' }");
        options.set("show", false);
        options.set("hide", false);
        return options;
    }

    private static class DescriptionTooltipPanel
        extends Panel
    {
        private static final long serialVersionUID = 1L;

        public DescriptionTooltipPanel(String aId, IModel<DescriptionTooltipBehavior> aModel)
        {
            super(aId, CompoundPropertyModel.of(aModel));

            var behavior = aModel.getObject();

            var icon = new WebMarkupContainer("titleIcon");
            icon.setVisible(behavior.titleIcon != null);
            if (behavior.titleIcon != null) {
                icon.add(new AttributeAppender("class", behavior.titleIcon, " "));
                if (behavior.titleIconCssClass != null) {
                    icon.add(new AttributeAppender("class", behavior.titleIconCssClass, " "));
                }
            }
            add(icon);

            add(new Label("title"));
            switch (aModel.getObject().mode) {
            case MARKDOWN: {
                add(new MarkdownLabel("description", aModel.map(d -> d.description)));
                break;
            }
            default: {
                Label label = new Label("description");
                label.add(new AttributeAppender("class", "tooltip-pre", " "));
                add(label);
                break;
            }
            }
        }
    }
}
