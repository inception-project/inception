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

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.wrapInTryCatch;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.kendo.ui.KendoUIBehavior;
import org.wicketstuff.kendo.ui.form.combobox.ComboBox;
import org.wicketstuff.kendo.ui.form.combobox.ComboBoxBehavior;

import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.config.InteractiveRecommenderProperties;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.AbstractTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.preset.Preset;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptingModeSelect;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ExtractionModeSelect;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.NoAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;

public abstract class LlmRecommenderTraitsEditor_ImplBase
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_OPTIONS_PANEL = "optionsPanel";
    private static final String MID_PROMPT_CONTAINER = "promptContainer";
    private static final String MID_PROMPT_HINTS = "promptHints";
    private static final String MID_PROMPT = "prompt";
    private static final String MID_EXTRACTION_MODE = "extractionMode";
    private static final String MID_PROMPTING_MODE = "promptingMode";
    private static final String MID_JUSTIFICATION_ENABLED = "justificationEnabled";
    private static final String MID_URL = "url";
    private static final String MID_MODEL = "model";
    private static final String MID_PRESET = "preset";
    private static final String MID_FORM = "form";
    private static final String MID_INTERACTIVE = "interactive";
    private static final String MID_STRUCTURED_OUTPUT_SUPPORTED = "structuredOutputSupported";

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean InteractiveRecommenderProperties properties;

    private final CompoundPropertyModel<LlmRecommenderTraits> traits;

    private final OptionsPanel optionSettingsContainer;
    private final IModel<List<OptionSetting>> optionSettings;
    private final IModel<List<Option<?>>> options;

    private final AuthenticationTraitsEditor<?> authenticationTraitsEditor;

    public LlmRecommenderTraitsEditor_ImplBase(String aId, IModel<Recommender> aRecommender,
            IModel<List<Preset>> aPresets, IModel<List<Option<?>>> aOptions)
    {
        super(aId, aRecommender);
        setOutputMarkupId(true);

        options = aOptions;
        traits = CompoundPropertyModel.of(getToolFactory().readTraits(aRecommender.getObject()));

        var form = new Form<LlmRecommenderTraits>(MID_FORM, traits)
        {
            private static final long serialVersionUID = -1;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                LlmRecommenderTraitsEditor_ImplBase.this.onSubmit();
            }
        };
        form.setOutputMarkupPlaceholderTag(true);
        add(form);

        var modelsModel = LoadableDetachableModel.of(this::listModels);
        var model = new ComboBox<String>(MID_MODEL, modelsModel);
        model.add(LambdaBehavior.onConfigure(() -> {
            // Trigger a re-loading of the model from the server
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

        var comboBox = new ComboBox<String>(MID_URL, listUrls());
        comboBox.setOutputMarkupId(true);
        comboBox.setRequired(true);
        comboBox.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                _target -> _target.add(model)));
        form.add(comboBox);

        var promptContainer = new WebMarkupContainer(MID_PROMPT_CONTAINER);
        promptContainer.setOutputMarkupPlaceholderTag(true);
        promptContainer.add(visibleWhenNot(traits.map(LlmRecommenderTraits::isInteractive)));
        form.add(promptContainer);

        var optionSettingsList = new ArrayList<OptionSetting>();
        for (var e : traits.getObject().getOptions().entrySet()) {
            getOption(e.getKey()).ifPresent(option -> {
                optionSettingsList.add(new OptionSetting(option, e.getValue()));
            });
        }

        optionSettings = new ListModel<>(optionSettingsList);
        optionSettingsContainer = new OptionsPanel(MID_OPTIONS_PANEL, options, optionSettings);
        optionSettingsContainer.setOutputMarkupPlaceholderTag(true);
        form.add(optionSettingsContainer);

        var presetSelect = new DropDownChoice<Preset>(MID_PRESET);
        presetSelect.setModel(Model.of());
        presetSelect.setChoiceRenderer(new ChoiceRenderer<>("name"));
        presetSelect.setChoices(aPresets);
        presetSelect.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                _target -> applyPreset(form, presetSelect.getModelObject(), _target)));
        promptContainer.add(presetSelect);

        promptContainer.add(new TextArea<String>(MID_PROMPT).setOutputMarkupId(true));

        var markdownLabel = new MarkdownLabel(MID_PROMPT_HINTS,
                LoadableDetachableModel.of(this::getPromptHints));
        markdownLabel.setOutputMarkupId(true);
        promptContainer.add(markdownLabel);

        promptContainer.add(new PromptingModeSelect(MID_PROMPTING_MODE)
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        _target -> _target.add(markdownLabel))));

        promptContainer.add(new ExtractionModeSelect(MID_EXTRACTION_MODE,
                traits.bind(MID_EXTRACTION_MODE), getModel()).setOutputMarkupId(true));

        promptContainer.add(new CheckBox(MID_JUSTIFICATION_ENABLED) //
                .setOutputMarkupId(true));

        authenticationTraitsEditor = createAuthenticationTraitsEditor("authentication");
        form.add(authenticationTraitsEditor);

        form.add(new CheckBox(MID_INTERACTIVE) //
                .setOutputMarkupId(true) //
                .add(visibleWhen(properties::isEnabled)) //
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        $ -> $.add(promptContainer))));

        form.add(new CheckBox(MID_STRUCTURED_OUTPUT_SUPPORTED) //
                .setOutputMarkupId(true));
    }

    protected AuthenticationTraitsEditor<?> createAuthenticationTraitsEditor(String aId)
    {
        var editor = new NoAuthenticationTraitsEditor(aId);
        editor.setVisible(false);
        return editor;
    }

    protected void onSubmit()
    {
        authenticationTraitsEditor.commit();
        var optionsMap = new LinkedHashMap<String, Object>();
        for (var optionSetting : optionSettings.getObject()) {
            var option = optionSetting.getOption();
            optionsMap.put(option.getName(), option.parseValue(optionSetting.getValue()));
        }
        traits.getObject().setOptions(optionsMap);
        getToolFactory().writeTraits(getModelObject(), traits.getObject());
    }

    public IModel<LlmRecommenderTraits> getTraits()
    {
        return traits;
    }

    private void applyPreset(Form<LlmRecommenderTraits> aForm, Preset aPreset,
            AjaxRequestTarget aTarget)
    {
        if (aPreset != null) {
            var settings = traits.getObject();
            settings.setPrompt(aPreset.getPrompt());
            settings.setExtractionMode(aPreset.getExtractionMode());
            settings.setPromptingMode(aPreset.getPromptingMode());
        }
        aTarget.add(aForm);
    }

    private String getPromptHints()
    {
        return traits.getObject().getPromptingMode().getHints();
    }

    protected List<String> listModels()
    {
        return emptyList();
    }

    protected List<String> listUrls()
    {
        return emptyList();
    }

    protected Optional<Option<?>> getOption(String aOption)
    {
        return options.getObject().stream() //
                .filter(o -> o.getName().equals(aOption)) //
                .findFirst();
    }

    public abstract RecommendationEngineFactory<LlmRecommenderTraits> getToolFactory();
}
