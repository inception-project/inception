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
package de.tudarmstadt.ukp.inception.annotation.feature.number;

import org.apache.uima.cas.CAS;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.form.NumberTextField;

import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

public class NumberFeatureEditor<T extends Number>
    extends FeatureEditor
{
    private static final long serialVersionUID = -2426303638953208057L;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    @SuppressWarnings("rawtypes")
    private final NumberTextField field;

    @SuppressWarnings("unchecked")
    public NumberFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel,
            NumberFeatureTraits aTraits)
    {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));

        switch (getModelObject().feature.getType()) {
        case CAS.TYPE_NAME_INTEGER: {
            Options options = new Options();
            options.set("format", "'#'");
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
