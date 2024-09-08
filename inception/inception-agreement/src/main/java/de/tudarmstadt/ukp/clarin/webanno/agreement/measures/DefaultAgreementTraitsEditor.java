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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;

public class DefaultAgreementTraitsEditor<T extends DefaultAgreementTraits>
    extends Panel
{
    private static final long serialVersionUID = 7780019891761754494L;

    private final DropDownChoice<LinkFeatureMultiplicityMode> linkCompareBehaviorDropDown;

    private final Form<T> form;

    public DefaultAgreementTraitsEditor(String aId, IModel<AnnotationFeature> aFeature,
            IModel<T> aModel)
    {
        super(aId, aModel);

        form = new Form<T>("form", CompoundPropertyModel.of(aModel))
        {
            private static final long serialVersionUID = -1422265935439298212L;

            @Override
            protected void onSubmit()
            {
                // TODO Auto-generated method stub
                super.onSubmit();
            }
        };

        linkCompareBehaviorDropDown = new DropDownChoice<>("linkCompareBehavior",
                asList(LinkFeatureMultiplicityMode.values()), new EnumChoiceRenderer<>(this));
        linkCompareBehaviorDropDown.add(
                visibleWhen(aFeature.map(f -> LinkMode.NONE != f.getLinkMode()).orElse(false)));
        linkCompareBehaviorDropDown.setOutputMarkupPlaceholderTag(true);
        form.add(linkCompareBehaviorDropDown);

        form.add(new CheckBox("limitToFinishedDocuments").setOutputMarkupId(true));

        add(form);
    }

    @SuppressWarnings("unchecked")
    public T getModelObject()
    {
        return (T) getDefaultModelObject();
    }

    protected Form<T> getForm()
    {
        return form;
    }
}
