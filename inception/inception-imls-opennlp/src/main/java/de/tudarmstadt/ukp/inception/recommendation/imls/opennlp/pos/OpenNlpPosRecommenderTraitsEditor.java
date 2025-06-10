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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos;

import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.DefaultTrainableRecommenderTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaForm;

public class OpenNlpPosRecommenderTraitsEditor
    extends DefaultTrainableRecommenderTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommendationEngineFactory<OpenNlpPosRecommenderTraits> toolFactory;

    private final OpenNlpPosRecommenderTraits traits;

    public OpenNlpPosRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        traits = toolFactory.readTraits(aRecommender.getObject());

        var form = new LambdaForm<OpenNlpPosRecommenderTraits>(MID_FORM,
                new CompoundPropertyModel<>(traits));
        form.onSubmit((t, f) -> toolFactory.writeTraits(aRecommender.getObject(), traits));

        var iterations = new NumberTextField<>("taggedTokensThreshold", Double.class);
        iterations.setMinimum(1.0);
        iterations.setMaximum(100.0);
        form.add(iterations);

        add(form);
    }
}
