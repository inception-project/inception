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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures;

import java.io.Serializable;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.dkpro.statistics.agreement.IAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public abstract class AgreementMeasureSupport_ImplBase<T extends DefaultAgreementTraits, R extends Serializable, S extends IAnnotationStudy>
    implements AggreementMeasureSupport<T, R, S>
{
    private String id;

    @Override
    public void setBeanName(String aName)
    {
        id = aName;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeature,
            IModel<T> aModel)
    {
        return new DefaultAgreementTraitsEditor<DefaultAgreementTraits>(aId, aFeature,
                (IModel) aModel);
    }

    @Override
    public T createTraits()
    {
        return (T) new DefaultAgreementTraits();
    }
}
