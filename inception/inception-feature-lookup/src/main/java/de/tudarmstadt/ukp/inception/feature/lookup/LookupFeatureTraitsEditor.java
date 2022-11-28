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
package de.tudarmstadt.ukp.inception.feature.lookup;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;

public class LookupFeatureTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = 2147578323003887302L;

    private static final String MID_FORM = "form";

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private String featureSupportId;
    private IModel<AnnotationFeature> feature;
    private CompoundPropertyModel<LookupFeatureTraits> traits;

    public LookupFeatureTraitsEditor(String aId, FeatureSupport<LookupFeatureTraits> aFS,
            IModel<AnnotationFeature> aFeature)
    {
        super(aId, aFeature);

        featureSupportId = aFS.getId();
        feature = aFeature;

        traits = CompoundPropertyModel.of(getFeatureSupport().readTraits(feature.getObject()));

        Form<LookupFeatureTraits> form = new Form<LookupFeatureTraits>(MID_FORM, traits)
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onInitialize()
            {
                super.onInitialize();

                TextField<String> remoteUrl = new TextField<>("remoteUrl");
                remoteUrl.setRequired(true);
                remoteUrl.add(new UrlValidator());
                queue(remoteUrl);

                queue(new TextField<>("authorizationToken"));
            }

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                getFeatureSupport().writeTraits(feature.getObject(), traits.getObject());
            }
        };
        form.setOutputMarkupPlaceholderTag(true);
        add(form);
    }

    @SuppressWarnings("unchecked")
    private FeatureSupport<LookupFeatureTraits> getFeatureSupport()
    {
        return (FeatureSupport<LookupFeatureTraits>) featureSupportRegistry
                .getExtension(featureSupportId).orElseThrow();
    }
}
