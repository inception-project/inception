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

import java.util.List;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.AbstractTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class OllamaRecommenderTraitsEditor
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean RecommendationEngineFactory<OllamaRecommenderTraits> toolFactory;

    private final IModel<OllamaRecommenderTraits> traits;

    public OllamaRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender,
            IModel<List<Preset>> aPresets)
    {
        super(aId, aRecommender);

        setOutputMarkupId(true);

        traits = CompoundPropertyModel.of(toolFactory.readTraits(aRecommender.getObject()));

        Form<OllamaRecommenderTraits> form = new Form<OllamaRecommenderTraits>(MID_FORM, traits)
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

        var presetSelect = new DropDownChoice<Preset>("preset");
        presetSelect.setModel(Model.of());
        presetSelect.setChoiceRenderer(new ChoiceRenderer<>("name"));
        presetSelect.setChoices(aPresets);
        presetSelect.add(new LambdaAjaxFormComponentUpdatingBehavior("change", _target -> {
            var preset = presetSelect.getModelObject();
            if (preset != null) {
                var settings = traits.getObject();
                settings.setPrompt(preset.getPrompt());
                settings.setExtractionMode(preset.getExtractionMode());
                settings.setFormat(preset.getFormat());
                settings.setPromptingMode(preset.getPromptingMode());
                settings.setRaw(preset.isRaw());
            }
            _target.add(form);
        }));
        form.add(presetSelect);

        form.add(new TextField<String>("url"));
        form.add(new TextField<String>("model"));
        form.add(new TextArea<String>("prompt"));
        form.add(new CheckBox("raw").setOutputMarkupPlaceholderTag(true));
        form.add(new PromptingModeSelect("promptingMode"));
        form.add(new ExtractionModeSelect("extractionMode"));
        form.add(new OllamaResponseFormatSelect("format"));

        add(form);
    }
}
