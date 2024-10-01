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
package de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt;

import static de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.ChatGptRecommenderTraits.CEREBRAS_API_URL;
import static de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.ChatGptRecommenderTraits.GROQ_API_URL;
import static de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.ChatGptRecommenderTraits.LOCAL_OLLAMA_API_URL;
import static de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.ChatGptRecommenderTraits.OPENAI_API_URL;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.wrapInTryCatch;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
import de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.client.ChatGptClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.client.ChatGptModel;
import de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.client.ListModelsRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.prompt.PromptingModeSelect;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.response.ExtractionModeSelect;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;

public class ChatGptRecommenderTraitsEditor
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean RecommendationEngineFactory<ChatGptRecommenderTraits> toolFactory;
    private @SpringBean InteractiveRecommenderProperties properties;

    private final CompoundPropertyModel<ChatGptRecommenderTraits> traits;

    private final ApiKeyAuthenticationTraitsEditor authenticationTraitsEditor;

    public ChatGptRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender,
            IModel<List<Preset>> aPresets)
    {
        super(aId, aRecommender);

        setOutputMarkupId(true);

        traits = CompoundPropertyModel.of(toolFactory.readTraits(aRecommender.getObject()));

        var form = new Form<ChatGptRecommenderTraits>(MID_FORM, traits)
        {
            private static final long serialVersionUID = -1;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                authenticationTraitsEditor.commit();
                toolFactory.writeTraits(aRecommender.getObject(), traits.getObject());
            }
        };
        form.setOutputMarkupPlaceholderTag(true);

        var modelsModel = LoadableDetachableModel.of(this::listModels);
        var model = new ComboBox<String>("model", modelsModel);
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

        if (!(traits.getObject().getAuthentication() instanceof ApiKeyAuthenticationTraits)) {
            traits.getObject().setAuthentication(new ApiKeyAuthenticationTraits());
        }

        var comboBox = new ComboBox<String>("url", asList(//
                OPENAI_API_URL, //
                CEREBRAS_API_URL, //
                GROQ_API_URL, //
                LOCAL_OLLAMA_API_URL));
        comboBox.setOutputMarkupId(true);
        comboBox.setRequired(true);
        form.add(comboBox);

        authenticationTraitsEditor = new ApiKeyAuthenticationTraitsEditor("authentication",
                Model.of((ApiKeyAuthenticationTraits) traits.getObject().getAuthentication()));
        form.add(authenticationTraitsEditor);

        var promptContainer = new WebMarkupContainer("promptContainer");
        promptContainer.setOutputMarkupPlaceholderTag(true);
        promptContainer.add(visibleWhenNot(traits.map(ChatGptRecommenderTraits::isInteractive)));
        form.add(promptContainer);

        var presetSelect = new DropDownChoice<Preset>("preset");
        presetSelect.setModel(Model.of());
        presetSelect.setChoiceRenderer(new ChoiceRenderer<>("name"));
        presetSelect.setChoices(aPresets);
        presetSelect.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                _target -> applyPreset(form, presetSelect.getModelObject(), _target)));
        promptContainer.add(presetSelect);

        promptContainer.add(new TextArea<String>("prompt"));
        var markdownLabel = new MarkdownLabel("promptHints",
                LoadableDetachableModel.of(this::getPromptHints));
        markdownLabel.setOutputMarkupId(true);
        promptContainer.add(markdownLabel);
        promptContainer.add(new PromptingModeSelect("promptingMode")
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        _target -> _target.add(markdownLabel))));
        promptContainer.add(new ExtractionModeSelect("extractionMode",
                traits.bind("extractionMode"), getModel()));
        promptContainer.add(new ChatGptResponseFormatSelect("format"));

        form.add(new CheckBox("interactive") //
                .setOutputMarkupId(true) //
                .add(visibleWhen(properties::isEnabled)) //
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        $ -> $.add(promptContainer))));

        add(form);
    }

    private void applyPreset(Form<ChatGptRecommenderTraits> aForm, Preset aPreset,
            AjaxRequestTarget aTarget)
    {
        if (aPreset != null) {
            var settings = traits.getObject();
            settings.setPrompt(aPreset.getPrompt());
            settings.setExtractionMode(aPreset.getExtractionMode());
            settings.setFormat(aPreset.getFormat());
            settings.setPromptingMode(aPreset.getPromptingMode());
        }
        aTarget.add(aForm);
    }

    private String getPromptHints()
    {
        return traits.getObject().getPromptingMode().getHints();
    }

    private List<String> listModels()
    {
        var url = traits.map(ChatGptRecommenderTraits::getUrl).orElse(null).getObject();
        var apiKey = ((ApiKeyAuthenticationTraits) traits
                .map(ChatGptRecommenderTraits::getAuthentication).orElse(null).getObject())
                        .getApiKey();

        if (!new UrlValidator(new String[] { "http", "https" }).isValid(url) || isBlank(apiKey)) {
            return Collections.emptyList();
        }

        var client = new ChatGptClientImpl();
        try {
            return client.listModels(url, ListModelsRequest.builder() //
                    .withApiKey(apiKey) //
                    .build()).stream() //
                    .map(ChatGptModel::getId) //
                    .toList();
        }
        catch (IOException e) {
            return Collections.emptyList();
        }
    }
}
