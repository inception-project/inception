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
package de.tudarmstadt.ukp.inception.annotation.feature.string;

import java.util.Optional;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.widget.tooltip.TooltipBehavior;
import com.googlecode.wicket.kendo.ui.KendoUIBehavior;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBoxBehavior;

import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.StyledComboBox;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;

public class ClassicKendoComboboxTextFeatureEditor
    extends TextFeatureEditorBase
{
    private static final long serialVersionUID = 8686646370500180943L;

    /**
     * Function to return tooltip using jquery Docs for the JQuery tooltip widget that we configure
     * below: https://api.jqueryui.com/tooltip/
     */
    public static final String FUNCTION_FOR_TOOLTIP = "function() { return "
            + "'<div class=\"tooltip-title\">'+($(this).text() "
            + "? $(this).text() : 'no title')+'</div>"
            + "<div class=\"tooltip-content tooltip-pre\">'+($(this).attr('title') "
            + "? $(this).attr('title') : 'no description' )+'</div>' }";

    public ClassicKendoComboboxTextFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel)
    {
        super(aId, aItem, aModel);
    }

    @Override
    protected AbstractTextComponent<String> createInputField()
    {
        return new StyledComboBox<Tag>("value", PropertyModel.of(getModel(), "tagset"))
        {
            private static final long serialVersionUID = -1735694425658462932L;

            @Override
            protected void onInitialize()
            {
                super.onInitialize();

                // Ensure proper order of the initializing JS header items: first combo box
                // behavior (in super.onInitialize()), then tooltip.
                Options options = new Options(DescriptionTooltipBehavior.makeTooltipOptions());
                options.set("content", FUNCTION_FOR_TOOLTIP);
                add(new TooltipBehavior("#" + getMarkupId() + "_listbox *[title]", options)
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
                Optional<AjaxRequestTarget> target = RequestCycle.get()
                        .find(AjaxRequestTarget.class);
                if (target.isPresent()) {
                    target.get()
                            .appendJavaScript(WicketUtil.wrapInTryCatch(String.format(
                                    "var $w = %s; if ($w) { $w.dataSource.read(); }",
                                    KendoUIBehavior.widget(this, ComboBoxBehavior.METHOD))));
                }
            }
        };
    }
}
