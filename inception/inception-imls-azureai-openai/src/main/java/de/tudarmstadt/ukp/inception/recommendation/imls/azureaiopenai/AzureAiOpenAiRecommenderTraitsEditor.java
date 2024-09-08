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
package de.tudarmstadt.ukp.inception.recommendation.imls.azureaiopenai;

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.AbstractTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.prompt.PromptingModeSelect;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.response.ExtractionModeSelect;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;

public class AzureAiOpenAiRecommenderTraitsEditor
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean RecommendationEngineFactory<AzureAiOpenAiRecommenderTraits> toolFactory;

    private final CompoundPropertyModel<AzureAiOpenAiRecommenderTraits> traits;

    private final ApiKeyAuthenticationTraitsEditor authenticationTraitsEditor;

    public AzureAiOpenAiRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender,
            IModel<List<Preset>> aPresets)
    {
        super(aId, aRecommender);

        setOutputMarkupId(true);

        traits = CompoundPropertyModel.of(toolFactory.readTraits(aRecommender.getObject()));

        var form = new Form<AzureAiOpenAiRecommenderTraits>(MID_FORM, traits)
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

        var presetSelect = new DropDownChoice<Preset>("preset");
        presetSelect.setModel(Model.of());
        presetSelect.setChoiceRenderer(new ChoiceRenderer<>("name"));
        presetSelect.setChoices(aPresets);
        presetSelect.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                _target -> applyPreset(form, presetSelect.getModelObject(), _target)));
        form.add(presetSelect);

        if (!(traits.getObject().getAuthentication() instanceof ApiKeyAuthenticationTraits)) {
            traits.getObject().setAuthentication(new ApiKeyAuthenticationTraits());
        }

        form.add(new TextField<String>("url"));
        authenticationTraitsEditor = new ApiKeyAuthenticationTraitsEditor("authentication",
                Model.of((ApiKeyAuthenticationTraits) traits.getObject().getAuthentication()));
        form.add(authenticationTraitsEditor);
        form.add(new TextArea<String>("prompt"));
        var markdownLabel = new MarkdownLabel("promptHints",
                LoadableDetachableModel.of(this::getPromptHints));
        markdownLabel.setOutputMarkupId(true);
        form.add(markdownLabel);
        form.add(new PromptingModeSelect("promptingMode")
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        _target -> _target.add(markdownLabel))));
        form.add(new ExtractionModeSelect("extractionMode", traits.bind("extractionMode"),
                getModel()));
        form.add(new AzureAiOpenAiResponseFormatSelect("format"));
        add(form);
    }

    private void applyPreset(Form<AzureAiOpenAiRecommenderTraits> aForm, Preset aPreset,
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
}
