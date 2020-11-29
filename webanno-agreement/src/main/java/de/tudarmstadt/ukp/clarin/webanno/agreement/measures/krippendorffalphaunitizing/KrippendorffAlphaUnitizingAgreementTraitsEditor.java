/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalphaunitizing;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraitsEditor;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class KrippendorffAlphaUnitizingAgreementTraitsEditor
    extends DefaultAgreementTraitsEditor<KrippendorffAlphaUnitizingAgreementTraits>
{
    private static final long serialVersionUID = 7780019891761754494L;

    public KrippendorffAlphaUnitizingAgreementTraitsEditor(String aId,
            IModel<AnnotationFeature> aFeature,
            IModel<KrippendorffAlphaUnitizingAgreementTraits> aModel)
    {
        super(aId, aFeature, aModel);

        getForm().add(new CheckBox("excludeIncomplete").setOutputMarkupId(true));
    }

    @Override
    public KrippendorffAlphaUnitizingAgreementTraits getModelObject()
    {
        return (KrippendorffAlphaUnitizingAgreementTraits) getDefaultModelObject();
    }
}
