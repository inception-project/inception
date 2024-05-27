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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.relation.settings;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.DefaultTrainableRecommenderTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.relation.StringMatchingRelationRecommenderTraits;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class StringMatchingRelationRecommenderTraitsEditor
    extends DefaultTrainableRecommenderTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommendationEngineFactory<StringMatchingRelationRecommenderTraits> toolFactory;
    private @SpringBean AnnotationSchemaService annotationSchemaService;

    private final StringMatchingRelationRecommenderTraits traits;

    public StringMatchingRelationRecommenderTraitsEditor(String aId,
            IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        traits = toolFactory.readTraits(aRecommender.getObject());

        Form<StringMatchingRelationRecommenderTraits> form = new Form<>(MID_FORM,
                CompoundPropertyModel.of(Model.of(traits)))
        {
            private static final long serialVersionUID = -3109239654242291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                toolFactory.writeTraits(aRecommender.getObject(), traits);
            }
        };

        DropDownChoice<String> adjunctFeature = new DropDownChoice<>("adjunctFeature");
        AnnotationLayer baseLayer = aRecommender.getObject().getLayer().getAttachType();
        List<String> baseFeatures = annotationSchemaService.listAnnotationFeature(baseLayer)
                .stream() //
                .map(AnnotationFeature::getName) //
                .collect(Collectors.toList());

        adjunctFeature.setChoices(baseFeatures);
        adjunctFeature.setRequired(true);
        form.add(adjunctFeature);

        add(form);
    }
}
