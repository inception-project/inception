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

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
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

    public StyledComboBox(String string, List<T> choices)
    {
        super(string, choices);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        
        add(new Behavior() {
            private static final long serialVersionUID = -5674186692106167407L;
            
            @Override
            public void renderHead(Component aComponent, IHeaderResponse aResponse)
            {
                super.renderHead(aComponent, aResponse);
                
                // Force-remove KendoDataSource header item if there already is one. This allows
                // Wicket to re-declare the datasource for the callback URL of the new instance
                // of this feature editor.
                // This causes all the choices to be transferred again, but at least tags added
                // to open tagsets appear immediately in the dropdown list and constraints
                // apply (hopefully).
                // Note: this must be done here instead of before the call to super such that
                // first the old datasource declarations are removed and then the new one is
                // added and remains in the HTML. Here we rely on the fact that the feature
                // editors have a fixed markup ID (which we also rely on for restoring focus).
                aResponse.render(new PriorityHeaderItem(JavaScriptHeaderItem.forScript(
                        "$('head script[id=kendo-datasource_" +
                        StyledComboBox.this.getMarkupId() + "]').remove();", 
                        null)));
            }
        });
    }
    
    @Override
    protected IJQueryTemplate newTemplate()
    {
        return new IJQueryTemplate()
        {
            private static final long serialVersionUID = 1L;
            /**
             * Marks the reordered entries in bold.
             * Same as text feature editor.
             */
            @Override
            public String getText()
            {
                // Some docs on how the templates work in Kendo, in case we need
                // more fancy dropdowns
                // http://docs.telerik.com/kendo-ui/framework/templates/overview
                StringBuilder sb = new StringBuilder();
                sb.append("# if (data.reordered == 'true') { #");
                sb.append("<div title=\"#: data.description #\"><b>#: data.name #</b></div>\n");
                sb.append("# } else { #");
                sb.append("<div title=\"#: data.description #\">#: data.name #</div>\n");
                sb.append("# } #");
                return sb.toString();
            }

            @Override
            public List<String> getTextProperties()
            {
                return Arrays.asList("name", "description", "reordered");
            }
        };
    }
}