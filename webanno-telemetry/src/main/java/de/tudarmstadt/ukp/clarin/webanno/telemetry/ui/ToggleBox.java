/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.telemetry.ui;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkboxx.CheckBoxX;

public class ToggleBox
    extends CheckBoxX
{
    private static final long serialVersionUID = 5403444699767988141L;

    public ToggleBox(String aId)
    {
        super(aId);
        
        getConfig().withIconNull("<i class=\"fa fa-question\"></i> Click to choose...");
        getConfig().withIconChecked("<i class=\"fa fa-check\"></i> Enabled");
        getConfig().withIconUnchecked("<i class=\"fa fa-ban\"></i> Disabled");
        getConfig().withEnclosedLabel(false);
        
        add(new ChoiceRequiredValidator());
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        getConfig().withThreeState(ToggleBox.this.getModelObject() == null);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
        aResponse.render(CssHeaderItem.forCSS(
                ".checkboxx-toggle-button .cbx-md { width: 10em; }", 
                "ToggleBox"));
        aResponse.render(OnDomReadyHeaderItem.forScript(coloringScript()));
    }
    
    @Override
    protected void onChange(Boolean aValue, AjaxRequestTarget aTarget)
    {
        if (aValue != null) {
            getConfig().withThreeState(false);
            aTarget.add(this);
        }
        
        aTarget.appendJavaScript(coloringScript());
    }

    private String coloringScript()
    {
        return String.join("\n",
                "$('.checkboxx-toggle-button .fa-question').each((idx, item) => "
                + "$(item).closest('.cbx-container').css('background-color', '#fff3cd'))",
                "$('.checkboxx-toggle-button .fa-check').each((idx, item) => "
                + "$(item).closest('.cbx-container').css('background-color', '#d4edda'))",
                "$('.checkboxx-toggle-button .fa-ban').each((idx, item) => "
                + "$(item).closest('.cbx-container').css('background-color', '#f8d7da'))"
                );
    }

    private class ChoiceRequiredValidator
        implements INullAcceptingValidator<Boolean>
    {
        private static final long serialVersionUID = -267638869839503827L;

        @Override
        public void validate(IValidatable<Boolean> aValidatable)
        {
            if (aValidatable.getValue() == null) {
                aValidatable.error(new ValidationError("You need to make a choice."));
            }
        }
    }
}
