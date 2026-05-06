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
package de.tudarmstadt.ukp.inception.annotation.feature.multistring;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.UimaPrimitiveFeatureSupport_ImplBase;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.RecommendableFeatureTrait;
import de.tudarmstadt.ukp.inception.schema.api.feature.RetainSuggestionInfoPanel;

public class MultiValueStringFeatureTraitsEditor
    extends GenericPanel<AnnotationFeature>
{
    private static final long serialVersionUID = -9082045435380184514L;

    private static final String MID_FORM = "form";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private String featureSupportId;
    private IModel<AnnotationFeature> feature;
    private CompoundPropertyModel<MultiValueStringFeatureTraits> traits;

    public MultiValueStringFeatureTraitsEditor(String aId,
            UimaPrimitiveFeatureSupport_ImplBase<MultiValueStringFeatureTraits> aFS,
            IModel<AnnotationFeature> aFeature)
    {
        super(aId, aFeature);

        featureSupportId = aFS.getId();
        feature = aFeature;

        traits = CompoundPropertyModel.of(getFeatureSupport().readTraits(feature.getObject()));

        var form = new Form<MultiValueStringFeatureTraits>(MID_FORM, traits)
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();

                getFeatureSupport().writeTraits(feature.getObject(), traits.getObject());
            }
        };
        form.setOutputMarkupPlaceholderTag(true);
        add(form);

        var tagset = new DropDownChoice<TagSet>("tagset");
        tagset.setOutputMarkupPlaceholderTag(true);
        tagset.setChoiceRenderer(new ChoiceRenderer<>("name"));
        tagset.setNullValid(true);
        tagset.setModel(PropertyModel.of(aFeature, "tagset"));
        tagset.setChoices(LoadableDetachableModel
                .of(() -> annotationService.listTagSets(aFeature.getObject().getProject())));
        form.add(tagset);

        form.add(new RetainSuggestionInfoPanel("retainSuggestionInfo", aFeature,
                traits.map(RecommendableFeatureTrait.class::cast)));
    }

    @SuppressWarnings("unchecked")
    private UimaPrimitiveFeatureSupport_ImplBase<MultiValueStringFeatureTraits> getFeatureSupport()
    {
        return (UimaPrimitiveFeatureSupport_ImplBase<MultiValueStringFeatureTraits>) featureSupportRegistry
                .getExtension(featureSupportId).orElseThrow();
    }
}
