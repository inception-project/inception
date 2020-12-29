/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.support;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;

import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;

public class StyledComboBox<T>
    extends ComboBox<T>
{
    private static final long serialVersionUID = 1L;

    public StyledComboBox(String aId, IModel<List<T>> aChoices)
    {
        super(aId, aChoices);
    }

    public StyledComboBox(String id, IModel<String> model, List<T> choices)
    {
        super(id, model, choices);
    }

    public StyledComboBox(String aId, IModel<String> aModel, IModel<List<T>> aChoices)
    {
        super(aId, aModel, aChoices);
    }

    public StyledComboBox(String string, List<T> choices)
    {
        super(string, choices);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        // This was an attempt to fix the problem that Wicket does not correctly return the
        // focus to ComboBoxes after an AJAX submit/onChange event. Unfortunately, it does
        // not fix the problem.
        // aResponse.render(new OnLoadHeaderItem("$('#" + getMarkupId()
        // + "').data('kendoComboBox').input.attr('id', '" + getMarkupId() + "-vis')"));
    }

    @Override
    protected IJQueryTemplate newTemplate()
    {
        return new IJQueryTemplate()
        {
            private static final long serialVersionUID = 1L;

            /**
             * Marks the reordered entries in bold. Same as text feature editor.
             */
            @Override
            public String getText()
            {
                // Some docs on how the templates work in Kendo, in case we need
                // more fancy dropdowns
                // http://docs.telerik.com/kendo-ui/framework/templates/overview
                return "# if (data.reordered == 'true') { #"
                        + "<div title=\"#: data.description #\"><b>#: data.name #</b></div>\n"
                        + "# } else { #"
                        + "<div title=\"#: data.description #\">#: data.name #</div>\n" + "# } #";
            }

            @Override
            public List<String> getTextProperties()
            {
                return Arrays.asList("name", "description", "reordered");
            }
        };
    }
}
