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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.DefaultTrainableRecommenderTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;

public class OpenNlpDoccatRecommenderTraitsEditor
    extends DefaultTrainableRecommenderTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommenderFactoryRegistry recommenderFactoryRegistry;

    private final OpenNlpDoccatRecommenderTraits traits;

    public OpenNlpDoccatRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        var toolFactory = (RecommendationEngineFactory<OpenNlpDoccatRecommenderTraits>) recommenderFactoryRegistry
                .getFactory(aRecommender.getObject().getTool());

        traits = toolFactory.readTraits(aRecommender.getObject());

        var form = new Form<OpenNlpDoccatRecommenderTraits>(MID_FORM,
                new CompoundPropertyModel<>(traits))
        {
            private static final long serialVersionUID = -3109239605742291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                toolFactory.writeTraits(aRecommender.getObject(), traits);
            }
        };

        NumberTextField<Integer> iterations = new NumberTextField<>("iterations", Integer.class);
        iterations.setMinimum(1);
        iterations.setMaximum(100);
        form.add(iterations);

        NumberTextField<Integer> cutoff = new NumberTextField<>("cutoff", Integer.class);
        cutoff.setMinimum(1);
        cutoff.setMaximum(100_000);
        form.add(cutoff);

        add(form);
    }
}
