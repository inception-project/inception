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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.DefaultTrainableRecommenderTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaForm;

public class OpenNlpNerRecommenderTraitsEditor
    extends DefaultTrainableRecommenderTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommendationEngineFactory<OpenNlpNerRecommenderTraits> toolFactory;

    private final CompoundPropertyModel<OpenNlpNerRecommenderTraits> traits;

    public OpenNlpNerRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        traits = new CompoundPropertyModel<>(toolFactory.readTraits(aRecommender.getObject()));

        var form = new LambdaForm<OpenNlpNerRecommenderTraits>(MID_FORM, traits);
        form.onSubmit(
                (t, f) -> toolFactory.writeTraits(aRecommender.getObject(), traits.getObject()));

        var correctionThreshold = new NumberTextField<>("correctionThreshold", Double.class);
        correctionThreshold.setOutputMarkupPlaceholderTag(true);
        correctionThreshold.setMinimum(1.0);
        correctionThreshold.setMaximum(100.0);
        correctionThreshold.setStep(0.01d);
        correctionThreshold
                .add(visibleWhen(traits.map(OpenNlpNerRecommenderTraits::isCorrectionsEnabled)));
        form.add(correctionThreshold);

        var correctionsEnabled = new CheckBox("correctionsEnabled");
        correctionsEnabled.setOutputMarkupId(true);
        correctionsEnabled.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                $ -> $.add(correctionThreshold)));
        form.add(correctionsEnabled);

        add(form);
    }
}
