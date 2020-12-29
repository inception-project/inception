/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor;

import org.apache.uima.cas.CAS;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.form.NumberTextField;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;

public class NumberFeatureEditor<T extends Number>
    extends FeatureEditor
{
    private static final long serialVersionUID = -2426303638953208057L;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    @SuppressWarnings("rawtypes")
    private final NumberTextField field;

    public NumberFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel,
            NumberFeatureTraits aTraits)
    {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));

        switch (getModelObject().feature.getType()) {
        case CAS.TYPE_NAME_INTEGER: {
            Options options = new Options();
            options.set("format", "'n0'");
            field = new NumberTextField<>("value", Integer.class, options);
            if (aTraits.isLimited()) {
                field.setMinimum(aTraits.getMinimum());
                field.setMaximum(aTraits.getMaximum());
            }
            break;
        }
        case CAS.TYPE_NAME_FLOAT: {
            field = new NumberTextField<>("value", Float.class);
            if (aTraits.isLimited()) {
                field.setMinimum(aTraits.getMinimum().floatValue());
                field.setMaximum(aTraits.getMaximum().floatValue());
            }
            break;
        }
        default:
            throw new IllegalArgumentException("Type [" + getModelObject().feature.getType()
                    + "] cannot be rendered as a numeric input field");
        }

        add(field);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public NumberTextField getFocusComponent()
    {
        return field;
    }
}
