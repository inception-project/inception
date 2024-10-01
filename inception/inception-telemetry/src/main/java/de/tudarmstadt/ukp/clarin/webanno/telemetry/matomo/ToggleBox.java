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
package de.tudarmstadt.ukp.clarin.webanno.telemetry.matomo;

import static java.util.Arrays.asList;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.form.dropdown.DropDownList;

public class ToggleBox
    extends DropDownList<Boolean>
{
    private static final long serialVersionUID = 5403444699767988141L;

    public ToggleBox(String aId)
    {
        super(aId);

        setOutputMarkupId(true);

        setRequired(true);
        setNullValid(false);
        setChoices(asList(false, true));
        setChoiceRenderer(new IChoiceRenderer<Boolean>()
        {
            private static final long serialVersionUID = -180513167658049798L;

            @Override
            public String getDisplayValue(Boolean aObject)
            {
                if (aObject == null) {
                    return null;
                }

                return aObject ? "enabled" : "disabled";
            }

            @Override
            public String getIdValue(Boolean aObject, int aIndex)
            {
                if (aObject == null) {
                    return null;
                }

                return aObject ? "true" : "false";
            }
        });
        setEscapeModelStrings(false); // SAFE - ONLY RENDERING A FEW HARD-CODED CHOICES WITH ICONS
    }

    @Override
    public void onConfigure(JQueryBehavior aBehavior)
    {
        super.onConfigure(aBehavior);

        var template = Options.asString(String.join("\n", //
                "# if (data.value == 'false') { #", //
                "<i class='fa fa-ban'></i> Disabled", //
                "# } else if (data.value == 'true') { #", //
                "<i class='fa fa-check'></i> Enabled", //
                "# } else { #", //
                "<i class='fa fa-question'></i> Choose...", //
                "# } #"));

        aBehavior.setOption("template", template);
        aBehavior.setOption("valueTemplate", template);
    }
}
