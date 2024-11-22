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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;

public class OptionsPanel
    extends GenericPanel<List<OptionSetting>>
{
    private static final String MID_REMOVE_OPTION = "removeOption";
    private static final String MID_VALUE = "value";
    private static final String MID_FORM = "form";
    private static final String MID_ADD_OPTION = "addOption";
    private static final String MID_OPTION_SETTINGS = "optionSettings";
    private static final String MID_OPTION = "option";
    private static final String MID_OPTION_SETTINGS_CONTAINER = "optionSettingsContainer";

    private static final long serialVersionUID = 6072424171129000521L;

    public OptionsPanel(String aId, IModel<List<Option<?>>> aOptions,
            IModel<List<OptionSetting>> aModel)
    {
        super(aId, aModel);

        setOutputMarkupId(true);

        var form = new Form<>(MID_FORM, CompoundPropertyModel.of(new OptionSetting()));
        form.add(new DropDownChoice<>(MID_OPTION, aOptions));
        form.add(new LambdaAjaxSubmitLink<OptionSetting>(MID_ADD_OPTION, this::addOptionSetting));

        var optionSettingsContainer = new WebMarkupContainer(MID_OPTION_SETTINGS_CONTAINER);
        optionSettingsContainer.add(createOptionSettingsList(MID_OPTION_SETTINGS, getModel()));
        form.add(optionSettingsContainer);

        add(form);
    }

    private ListView<OptionSetting> createOptionSettingsList(String aId,
            IModel<List<OptionSetting>> aOptionSettings)
    {
        return new ListView<OptionSetting>(aId, aOptionSettings)
        {
            private static final long serialVersionUID = 244305980337592760L;

            @Override
            protected void populateItem(ListItem<OptionSetting> aItem)
            {
                var optionSetting = aItem.getModelObject();

                aItem.add(new Label(MID_OPTION, optionSetting.getOption()));
                aItem.add(new TextField<>(MID_VALUE, PropertyModel.of(optionSetting, MID_VALUE)));
                aItem.add(new LambdaAjaxLink(MID_REMOVE_OPTION,
                        _target -> removeOptionSetting(_target, aItem.getModelObject())));
            }
        };
    }

    private void addOptionSetting(AjaxRequestTarget aTarget, Form<OptionSetting> aForm)
    {
        getModelObject().add(new OptionSetting(aForm.getModel().getObject().getOption(), ""));

        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(this);
    }

    private void removeOptionSetting(AjaxRequestTarget aTarget, OptionSetting aBinding)
    {
        getModelObject().remove(aBinding);
        aTarget.add(this);
    }
}
