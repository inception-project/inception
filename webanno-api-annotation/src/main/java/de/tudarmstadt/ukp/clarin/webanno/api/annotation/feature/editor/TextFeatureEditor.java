/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.widget.tooltip.TooltipBehavior;
import com.googlecode.wicket.kendo.ui.KendoUIBehavior;
import com.googlecode.wicket.kendo.ui.form.TextField;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBoxBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.StyledComboBox;

public class TextFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = 7763348613632105600L;

    private static final Logger LOG = LoggerFactory.getLogger(TextFeatureEditor.class);

    /**
     * Function to return tooltip using jquery
     * Docs for the JQuery tooltip widget that we configure below:
     * https://api.jqueryui.com/tooltip/
     */
    protected static final String FUNCTION_FOR_TOOLTIP = "function() { return "
        + "'<div class=\"tooltip-title\">'+($(this).text() "
        + "? $(this).text() : 'no title')+'</div>"
        + "<div class=\"tooltip-content tooltip-pre\">'+($(this).attr('title') "
        + "? $(this).attr('title') : 'no description' )+'</div>' }";

    @SuppressWarnings("rawtypes")
    private AbstractTextComponent field;
    private boolean hideUnconstrainedFeature;
    
    public TextFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel)
    {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));
        // Checks whether hide un-constraint feature is enabled or not
        hideUnconstrainedFeature = getModelObject().feature.isHideUnconstraintFeature();
        add(new Label("feature", getModelObject().feature.getUiName()));
        add(field = createFieldComboBox());
        add(createConstraintsInUseIndicatorContainer());
    }

    private AbstractTextComponent createFieldComboBox()
    {
        AbstractTextComponent field;
        if (getModelObject().feature.getTagset() != null) {
            field = new StyledComboBox<Tag>("value", PropertyModel.of(getModel(), "tagset")) {
                private static final long serialVersionUID = -1735694425658462932L;

                @Override
                protected void onInitialize()
                {
                    super.onInitialize();
                    
                    // Ensure proper order of the initializing JS header items: first combo box
                    // behavior (in super.onInitialize()), then tooltip.
                    Options options = new Options(DescriptionTooltipBehavior.makeTooltipOptions());
                    options.set("content", FUNCTION_FOR_TOOLTIP);
                    add(new TooltipBehavior("#" + getMarkupId() + "_listbox *[title]",
                            options)
                    {
                        private static final long serialVersionUID = 1854141593969780149L;

                        @Override
                        protected String $()
                        {
                            // REC: It takes a moment for the KendoDatasource to load the data and
                            // for the Combobox to render the hidden dropdown. I did not find
                            // a way to hook into this process and to get notified when the
                            // data is available in the dropdown, so trying to handle this
                            // with a slight delay hoping that all is set up after 1 second.
                            return "try {setTimeout(function () { " + super.$()
                                    + " }, 1000); } catch (err) {}; ";
                        }
                    });
                }
                
                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    
                    // Trigger a re-loading of the tagset from the server as constraints may have
                    // changed the ordering
                    AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                    if (target != null) {
                        LOG.trace("onInitialize() requesting datasource re-reading");
                        target.appendJavaScript(
                                String.format("var $w = %s; if ($w) { $w.dataSource.read(); }",
                                        KendoUIBehavior.widget(this, ComboBoxBehavior.METHOD)));
                    }
                }
            };
        }
        else {
            field = new TextField<String>("value");
        }

        // Ensure that markup IDs of feature editor focus components remain constant across
        // refreshes of the feature editor panel. This is required to restore the focus.
        field.setOutputMarkupId(true);
        field.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
        return field;
    }

    private Component createConstraintsInUseIndicatorContainer()
    {
        // Shows whether constraints are triggered or not
        // also shows state of constraints use.
        return new WebMarkupContainer("textIndicator")
        {
            private static final long serialVersionUID = 4346767114287766710L;

            @Override
            public boolean isVisible()
            {
                return getModelObject().indicator.isAffected();
            }
        }.add(new AttributeAppender("class", new Model<String>()
        {
            private static final long serialVersionUID = -7683195283137223296L;

            @Override
            public String getObject()
            {
                // adds symbol to indicator
                return getModelObject().indicator.getStatusSymbol();
            }
        })).add(new AttributeAppender("style", new Model<String>()
        {
            private static final long serialVersionUID = -5255873539738210137L;

            @Override
            public String getObject()
            {
                // adds color to indicator
                return "; color: " + getModelObject().indicator.getStatusColor();
            }
        }));
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        LOG.trace("TextFeatureEditor(path: " + getPageRelativePath() + ", "
                + getModelObject().feature.getUiName() + ": " + getModelObject().value + ")");
    }

    @Override
    public Component getFocusComponent()
    {
        return field;
    }

    /**
     * Hides feature if "Hide un-constraint feature" is enabled and constraint rules are applied and
     * feature doesn't match any constraint rule
     */
    @Override
    public void onConfigure()
    {
        // if enabled and constraints rule execution returns anything other than green
        setVisible(!hideUnconstrainedFeature || (getModelObject().indicator.isAffected()
                && getModelObject().indicator.getStatusColor().equals("green")));
    }
}
