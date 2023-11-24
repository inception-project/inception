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
package de.tudarmstadt.ukp.inception.annotation.feature.misc;

import java.util.Map;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.StyleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;

/**
 * Shows whether constraints are triggered or not also shows state of constraints use.
 */
public class ConstraintsInUseIndicator
    extends WebMarkupContainer
{
    private static final long serialVersionUID = 4192615818653618074L;

    public ConstraintsInUseIndicator(String aId, IModel<FeatureState> aModel)
    {
        super(aId, aModel);

        add(LambdaBehavior.visibleWhen(() -> getModelObject().indicator.isAffected()));
        add(new ClassAttributeModifier()
        {
            private static final long serialVersionUID = 4623544241209220039L;

            @Override
            protected Set<String> update(Set<String> aOldClasses)
            {
                aOldClasses.add(getModelObject().indicator.getStatusSymbol());
                return aOldClasses;
            }
        });
        add(new StyleAttributeModifier()
        {
            private static final long serialVersionUID = 3627596292626670610L;

            @Override
            protected Map<String, String> update(Map<String, String> aStyles)
            {
                aStyles.put("color", getModelObject().indicator.getStatusColor());
                return aStyles;
            }
        });
        add(new AttributeModifier("title", getModelObject().indicator.getStatusDescription()));
    }

    public IModel<FeatureState> getModel()
    {
        return (IModel<FeatureState>) getDefaultModel();
    }

    public FeatureState getModelObject()
    {
        return (FeatureState) getDefaultModelObject();
    }
}
