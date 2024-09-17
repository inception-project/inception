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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama;

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.wrapInTryCatch;
import static java.lang.String.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;
import org.wicketstuff.kendo.ui.KendoUIBehavior;
import org.wicketstuff.kendo.ui.form.combobox.ComboBox;
import org.wicketstuff.kendo.ui.form.combobox.ComboBoxBehavior;

import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.config.InteractiveRecommenderProperties;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.AbstractTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaGenerateRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaModel;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.option.Option;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.option.OptionSetting;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.prompt.PromptingModeSelect;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.response.ExtractionModeSelect;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;

public class OllamaRecommenderTraitsEditor
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_OPTION_SETTINGS = "optionSettings";
    private static final String MID_OPTION_SETTINGS_CONTAINER = "optionSettingsContainer";
    private static final String MID_PROMPT_HINTS = "promptHints";
    private static final String MID_RAW = "raw";
    private static final String MID_PROMPT = "prompt";
    private static final String MID_OPTION_SETTINGS_FORM = "optionSettingsForm";
    private static final String MID_ADD_OPTION = "addOption";
    private static final String MID_OPTION = "option";
    private static final String MID_FORMAT = "format";
    private static final String MID_EXTRACTION_MODE = "extractionMode";
    private static final String MID_PROMPTING_MODE = "promptingMode";
    private static final String MID_URL = "url";
    private static final String MID_MODEL = "model";
    private static final String MID_PRESET = "preset";
    private static final String MID_FORM = "form";

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean RecommendationEngineFactory<OllamaRecommenderTraits> toolFactory;
    private @SpringBean InteractiveRecommenderProperties properties;

    private final CompoundPropertyModel<OllamaRecommenderTraits> traits;

    private final WebMarkupContainer optionSettingsContainer;
    private final IModel<List<OptionSetting>> optionSettings;

    public OllamaRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender,
            IModel<List<Preset>> aPresets)
    {
        super(aId, aRecommender);

        setOutputMarkupId(true);

        traits = CompoundPropertyModel.of(toolFactory.readTraits(aRecommender.getObject()));

        var form = new Form<OllamaRecommenderTraits>(MID_FORM, traits)
        {
            private static final long serialVersionUID = -1;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                toolFactory.writeTraits(aRecommender.getObject(), traits.getObject());
            }
        };
        form.setOutputMarkupPlaceholderTag(true);

        var modelsModel = LoadableDetachableModel.of(this::listModels);
        var model = new ComboBox<String>(MID_MODEL, modelsModel);
        model.add(LambdaBehavior.onConfigure(() -> {
            // Trigger a re-loading of the tagset from the server as constraints may have
            // changed the ordering
            modelsModel.detach();
            var target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target.isPresent()) {
                target.get().appendJavaScript(wrapInTryCatch(format( //
                        "var $w = %s; if ($w) { $w.dataSource.read(); }",
                        KendoUIBehavior.widget(this, ComboBoxBehavior.METHOD))));
            }
        }));
        model.setOutputMarkupId(true);
        form.add(model);

        form.add(new TextField<String>(MID_URL).add(new LambdaAjaxFormComponentUpdatingBehavior(
                CHANGE_EVENT, _target -> _target.add(model))));

        var promptContainer = new WebMarkupContainer("promptContainer");
        promptContainer.setOutputMarkupPlaceholderTag(true);
        promptContainer.add(visibleWhenNot(traits.map(OllamaRecommenderTraits::isInteractive)));
        form.add(promptContainer);

        var presetSelect = new DropDownChoice<Preset>(MID_PRESET);
        presetSelect.setModel(Model.of());
        presetSelect.setChoiceRenderer(new ChoiceRenderer<>("name"));
        presetSelect.setChoices(aPresets);
        presetSelect.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                _target -> applyPreset(form, presetSelect.getModelObject(), _target)));
        promptContainer.add(presetSelect);

        promptContainer.add(new TextArea<String>(MID_PROMPT));
        promptContainer.add(new CheckBox(MID_RAW).setOutputMarkupPlaceholderTag(true));
        var markdownLabel = new MarkdownLabel(MID_PROMPT_HINTS,
                LoadableDetachableModel.of(this::getPromptHints));
        markdownLabel.setOutputMarkupId(true);
        promptContainer.add(markdownLabel);
        promptContainer.add(new PromptingModeSelect(MID_PROMPTING_MODE)
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        _target -> _target.add(markdownLabel))));
        promptContainer.add(new ExtractionModeSelect(MID_EXTRACTION_MODE,
                traits.bind(MID_EXTRACTION_MODE), getModel()));
        promptContainer.add(new OllamaResponseFormatSelect(MID_FORMAT));
        add(form);

        var optionSettingsForm = new Form<>(MID_OPTION_SETTINGS_FORM,
                CompoundPropertyModel.of(new OptionSetting()));
        optionSettingsForm.setVisibilityAllowed(false); // FIXME Not quite ready yet
        promptContainer.add(optionSettingsForm);

        optionSettingsForm.add(
                new DropDownChoice<Option<?>>(MID_OPTION, OllamaGenerateRequest.getAllOptions()));
        optionSettingsForm.add(
                new LambdaAjaxSubmitLink<OptionSetting>(MID_ADD_OPTION, this::addOptionSetting));

        optionSettingsContainer = new WebMarkupContainer(MID_OPTION_SETTINGS_CONTAINER);
        optionSettingsContainer.setOutputMarkupPlaceholderTag(true);
        optionSettingsForm.add(optionSettingsContainer);

        optionSettings = new ListModel<>(traits.getObject().getOptions().entrySet().stream()
                .map(e -> new OptionSetting(e.getKey(), String.valueOf(e.getValue())))
                .collect(Collectors.toCollection(ArrayList::new)));

        optionSettingsContainer.add(createOptionSettingsList(MID_OPTION_SETTINGS, optionSettings));

        form.add(new CheckBox("interactive") //
                .setOutputMarkupId(true) //
                .add(visibleWhen(properties::isEnabled)) //
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        $ -> $.add(promptContainer))));
    }

    private void applyPreset(Form<OllamaRecommenderTraits> aForm, Preset aPreset,
            AjaxRequestTarget aTarget)
    {
        if (aPreset != null) {
            var settings = traits.getObject();
            settings.setPrompt(aPreset.getPrompt());
            settings.setExtractionMode(aPreset.getExtractionMode());
            settings.setFormat(aPreset.getFormat());
            settings.setPromptingMode(aPreset.getPromptingMode());
            settings.setRaw(aPreset.isRaw());
        }
        aTarget.add(aForm);
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
                aItem.add(new TextField<>("value", PropertyModel.of(optionSetting, "value")));
                aItem.add(new LambdaAjaxLink("removeOption",
                        _target -> removeOptionSetting(_target, aItem.getModelObject())));
            }
        };
    }

    private void addOptionSetting(AjaxRequestTarget aTarget, Form<OptionSetting> aForm)
    {
        optionSettings.getObject()
                .add(new OptionSetting(aForm.getModel().getObject().getOption(), ""));

        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(optionSettingsContainer);
    }

    private void removeOptionSetting(AjaxRequestTarget aTarget, OptionSetting aBinding)
    {
        optionSettings.getObject().remove(aBinding);
        aTarget.add(optionSettingsContainer);
    }

    private String getPromptHints()
    {
        return traits.getObject().getPromptingMode().getHints();
    }

    private List<String> listModels()
    {
        var url = traits.map(OllamaRecommenderTraits::getUrl).orElse(null).getObject();
        if (!new UrlValidator(new String[] { "http", "https" }).isValid(url)) {
            return Collections.emptyList();
        }

        var client = new OllamaClientImpl();
        try {
            return client.listModels(url).stream().map(OllamaModel::getName).toList();
        }
        catch (IOException e) {
            return Collections.emptyList();
        }
    }
}
