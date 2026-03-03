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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai;

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.AbstractTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.preset.Preset;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptingModeSelect;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ExtractionModeSelect;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;

public class AzureAiOpenAiInteractionPanel
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";
    private static final String MID_PROMPT = "prompt";
    private static final String MID_NAME = "name";
    private static final String MID_PRESET = "preset";
    private static final String MID_EXTRACTION_MODE = "extractionMode";
    private static final String MID_PROMPTING_MODE = "promptingMode";
    private static final String MID_PROMPT_HINTS = "promptHints";
    private static final String MID_JUSTIFICATION_ENABLED = "justificationEnabled";

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean RecommendationEngineFactory<AzureAiOpenAiRecommenderTraits> toolFactory;

    private final CompoundPropertyModel<AzureAiOpenAiRecommenderTraits> traits;

    public AzureAiOpenAiInteractionPanel(String aId, IModel<Recommender> aRecommender,
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
                toolFactory.writeTraits(aRecommender.getObject(), traits.getObject());
            }
        };
        form.setOutputMarkupPlaceholderTag(true);

        var presetSelect = new DropDownChoice<Preset>(MID_PRESET);
        presetSelect.setModel(Model.of());
        presetSelect.setChoiceRenderer(new ChoiceRenderer<>(MID_NAME));
        presetSelect.setChoices(aPresets);
        presetSelect.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                _target -> applyPreset(form, presetSelect.getModelObject(), _target)));
        form.add(presetSelect);

        form.add(new TextArea<String>(MID_PROMPT).setOutputMarkupId(true));

        var markdownLabel = new MarkdownLabel(MID_PROMPT_HINTS,
                LoadableDetachableModel.of(this::getPromptHints));
        markdownLabel.setOutputMarkupId(true);
        form.add(markdownLabel);

        form.add(new PromptingModeSelect(MID_PROMPTING_MODE)
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        _target -> _target.add(markdownLabel))));

        form.add(new ExtractionModeSelect(MID_EXTRACTION_MODE, traits.bind(MID_EXTRACTION_MODE),
                getModel()).setOutputMarkupId(true));

        form.add(new CheckBox(MID_JUSTIFICATION_ENABLED) //
                .setOutputMarkupId(true));

        add(form);
    }

    private void applyPreset(Form<AzureAiOpenAiRecommenderTraits> aForm, Preset aPreset,
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
        var promptingMode = traits.getObject().getPromptingMode();
        return promptingMode != null ? promptingMode.getHints() : null;
    }
}
