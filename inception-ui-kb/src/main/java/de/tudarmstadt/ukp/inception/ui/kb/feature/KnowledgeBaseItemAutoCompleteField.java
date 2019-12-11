/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.StringValue;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.core.renderer.ITextRenderer;
import com.googlecode.wicket.jquery.core.renderer.TextRenderer;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

/**
 * Auto-complete field for accessing a knowledge base.
 * <p>
 * <b>NOTE:</b> This field <b>must</b> be used with a
 * {@link AjaxFormChoiceComponentUpdatingBehavior} and using
 * {@link #getIdentifierDynamicAttributeScript()} otherwise it cannot differentiate between
 * different items that have the same label!!!
 */
public class KnowledgeBaseItemAutoCompleteField
    extends AutoCompleteTextField<KBHandle>
{
    private static final long serialVersionUID = -1276017349261462683L;

    private SerializableFunction<String, List<KBHandle>> choiceProvider;
    private IConverter<KBHandle> converter;
    private List<KBHandle> choiceCache;
    private boolean allowChoiceCache = false;
    
    public KnowledgeBaseItemAutoCompleteField(String aId,
            SerializableFunction<String, List<KBHandle>> aChoiceProvider)
    {
        super(aId, new TextRenderer<KBHandle>("uiLabel"));
        
        Validate.notNull(aChoiceProvider);
        choiceProvider = aChoiceProvider;
        converter = newConverter();
    }

    public KnowledgeBaseItemAutoCompleteField(String aId,
            SerializableFunction<String, List<KBHandle>> aChoiceProvider,
            ITextRenderer<KBHandle> aRenderer)
    {
        super(aId, aRenderer);
        
        Validate.notNull(aChoiceProvider);
        choiceProvider = aChoiceProvider;
        converter = newConverter();
    }

    public KnowledgeBaseItemAutoCompleteField(String aId,
            IModel<KBHandle> aModel,
            SerializableFunction<String, List<KBHandle>> aChoiceProvider)
    {
        super(aId, aModel, new TextRenderer<KBHandle>("uiLabel"));
        
        Validate.notNull(aChoiceProvider);
        choiceProvider = aChoiceProvider;
        converter = newConverter();
    }
    
    @Override
    protected List<KBHandle> getChoices(String aInput)
    {
        if (!allowChoiceCache || choiceCache == null) {
            choiceCache = choiceProvider.apply(aInput);
        }
        return choiceCache;
    }
    
    @Override
    public String[] getInputAsArray()
    {
        // If the web request includes the additional "identifier" parameter which is supposed to
        // contain the IRI of the selected item instead of its label, then we use that as the value.
        WebRequest request = getWebRequest();
        IRequestParameters requestParameters = request.getRequestParameters();
        StringValue identifier = requestParameters
                .getParameterValue(getInputName() + ":identifier");
        
        if (!identifier.isEmpty()) {
            return new String[] { identifier.toString() };
        }
        
        return super.getInputAsArray();
    }
    
    /**
     * When using this input component with an {@link AjaxFormChoiceComponentUpdatingBehavior}, it
     * is necessary to request the identifier of the selected item as an additional dynamic 
     * attribute, otherwise no distinction can be made between two items with the same label!
     */
    public String getIdentifierDynamicAttributeScript()
    {
        return String.join(" ", 
                "var item = $(attrs.event.target).data('kendoAutoComplete').dataItem();",
                "if (item) {",
                "  return [{",
                "    'name': '" + getInputName() + ":identifier', ",
                "    'value': $(attrs.event.target).data('kendoAutoComplete').dataItem().identifier",
                "  }]",
                "}",
                "return [];");
    }
    
    @Override
    public <C> IConverter<C> getConverter(Class<C> aType)
    {
        if (aType != null && aType.isAssignableFrom(this.getType())) {
            return (IConverter<C>) converter;
        }
        
        return super.getConverter(aType);
    }
    
    private IConverter<KBHandle> newConverter()
    {
        return new IConverter<KBHandle>() {

            private static final long serialVersionUID = 1L;

            @Override
            public KBHandle convertToObject(String value, Locale locale)
            {
                if (value == null) {
                    return null;
                }
                
                if (value.equals(getModelValue())) {
                    return getModelObject();
                }
                
                // Check choices only here since fetching choices can take some time. If we already
                // have choices from a previous query, then we use them instead of reloading all
                // the choices. This avoids having to load the choices when opening the dropdown
                // AND when selecting one of the items from it.
                List<KBHandle> choices; 
                try {
                    allowChoiceCache = true;
                    choices = getChoices(value);
                }
                finally {
                    allowChoiceCache = false;
                }
                
                if (choices.isEmpty()) {
                    return null;
                }
                
                // Check if we can find a match by the identifier. The identifier is unique while
                // the same label may appear on multiple items
                for (KBHandle handle : choices) {
                    if (value.equals(handle.getIdentifier())) {
                        return handle;
                    }
                }
                
                // Check labels if there was no match on the identifier
                for (KBHandle handle : choices) {
                    if (value.equals(getRenderer().getText(handle))) {
                        return handle;
                    }
                }
                
                // If there was no match at all, return null
                return null;
            }

            @Override
            public String convertToString(KBHandle value, Locale locale)
            {
                return getRenderer().getText(value);
            }
        };
    } 

    @Override
    public void onConfigure(JQueryBehavior behavior)
    {
        super.onConfigure(behavior);
        
        behavior.setOption("ignoreCase", false);
        behavior.setOption("delay", 500);
        behavior.setOption("animation", false);
        behavior.setOption("footerTemplate",
                Options.asString("#: instance.dataSource.total() # items found"));
        
        // Try to smartly set the width and height of the dropdown
        behavior.setOption("height", "Math.max($(window).height()*0.5,200)");
        behavior.setOption("open", String.join(" ",
                "function(e) {",
                "  e.sender.list.width(Math.max($(window).width()*0.3,300));",
                "}"));
        
        // Reset the values in the dropdown listbox to avoid that when opening the dropdown the next
        // time ALL items with the same label as the selected item appear as selected
        behavior.setOption("filtering", String.join(" ",
                "function(e) {",
                "  e.sender.listView.value([]);",
                "}"));
        
        // We need to explicitly trigger the change event on the input element in order to trigger
        // the Wicket AJAX update (if there is one). If we do not do this, then Kendo will "forget"
        // to trigger a change event if the label of the newly selected item is the same as the 
        // label of the previously selected item!!!
        behavior.setOption("select", String.join(" ",
                "function (e) {",
                "  e.sender.element.trigger('change');",
                "}"));
        
        // Prevent scrolling action from closing the dropdown while the focus is on the input field
        // Use one-third of the browser width but not less than 300 pixels. This is better than 
        // using the Kendo auto-sizing feature because that sometimes doesn't get the width right.
        // The solution we use here is a NASTY hack, but I didn't find any other way to cancel out
        // only the closing triggered by scrolling the browser window without having other adverse
        // side effects such as mouse clicks or enter no longer selecting and closing the dropdown.
        // See: https://github.com/inception-project/inception/issues/1517
        behavior.setOption("close", String.join(" ",
                "function(e) {",
                "  if (new Error().stack.toString().includes('_resize')) {", 
                "    e.preventDefault();",
                "  }",
                "}"));
    }
    
    @Override
    protected IJQueryTemplate newTemplate()
    {
        return new IJQueryTemplate()
        {
            private static final long serialVersionUID = 8656996525796349138L;

            @Override
            public String getText()
            {
                StringBuilder sb = new StringBuilder();
                sb.append("<div>");
                sb.append("  <div class=\"item-title\">");
                sb.append("  # if (data.rank) { #");
                sb.append("  <span class=\"item-rank\">");
                sb.append("    [${ data.rank }]");
                sb.append("  </span>");
                sb.append("  # } #");
                sb.append("    ${ data.uiLabel }");
                sb.append("  </div>");
                sb.append("  <div class=\"item-identifier\">");
                sb.append("    ${ data.identifier }");
                sb.append("  </div>");
                sb.append("  <div class=\"item-description\">");
                sb.append("    ${ data.description }");
                sb.append("  </div>");
                if (DEVELOPMENT.equals(getApplication().getConfigurationType())) {
                    sb.append("  <div class=\"item-description\">");
                    sb.append("    ${ data.debugInfo }");
                    sb.append("  </div>");
                }
                sb.append("</div>");
                return sb.toString();
            }

            @Override
            public List<String> getTextProperties()
            {
                List<String> properties = new ArrayList<>();
                properties.add("identifier");
                properties.add("description");
                properties.add("rank");
                if (DEVELOPMENT.equals(getApplication().getConfigurationType())) {
                    properties.add("debugInfo");
                }
                return properties;
            }
        };
    }
}
