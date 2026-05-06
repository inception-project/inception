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
package de.tudarmstadt.ukp.inception.conceptlinking.recommender;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.AbstractTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaForm;

public class NamedEntityLinkerTraitsEditor
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommendationEngineFactory<NamedEntityLinkerTraits> toolFactory;
    private @SpringBean AnnotationSchemaService schemaService;

    private final NamedEntityLinkerTraits traits;

    public NamedEntityLinkerTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        traits = toolFactory.readTraits(aRecommender.getObject());

        var form = new LambdaForm<>(MID_FORM, CompoundPropertyModel.of(Model.of(traits)));
        form.onSubmit(this::actionSubmit);
        queue(form);

        queue(new CheckBox("emptyCandidateFeatureRequired") //
                .add(visibleWhen(aRecommender.map(Recommender::getLayer)
                        .map(AnnotationLayer::isAllowStacking))) //
                .setOutputMarkupPlaceholderTag(true));

        queue(new CheckBox("includeLinkTargetsInQuery") //
                .add(visibleWhen(this::hasLinkFeatures)) //
                .setOutputMarkupPlaceholderTag(true));

        queue(new CheckBox("synchronous") //
                .setOutputMarkupPlaceholderTag(true));
    }

    private void actionSubmit(AjaxRequestTarget aTarget, Form<NamedEntityLinkerTraits> aForm)
    {
        toolFactory.writeTraits(getModelObject(), aForm.getModelObject());
    }

    private boolean hasLinkFeatures()
    {
        return schemaService.listAnnotationFeature(getModelObject().getLayer()).stream()
                .anyMatch(f -> f.getLinkMode() == LinkMode.WITH_ROLE);
    }
}
