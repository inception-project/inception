/*
 * Copyright 2018
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

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;

public abstract class TextFeatureEditorBase
    extends FeatureEditor
{
    private static final long serialVersionUID = -3499366171559879681L;

    private static final Logger LOG = LoggerFactory.getLogger(TextFeatureEditorBase.class);
    
    @SuppressWarnings("rawtypes")
    private FormComponent field;
    private boolean hideUnconstrainedFeature;
    
    public TextFeatureEditorBase(String aId, MarkupContainer aItem, IModel<FeatureState> aModel)
    {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));
        
        field = createInputField();
        add(field);
        
        // Checks whether hide un-constraint feature is enabled or not
        hideUnconstrainedFeature = getModelObject().feature.isHideUnconstraintFeature();
        add(createConstraintsInUseIndicatorContainer());
    }

    protected abstract FormComponent createInputField();

    private Component createConstraintsInUseIndicatorContainer()
    {
        // Shows whether constraints are triggered or not
        // also shows state of constraints use.
        return new WebMarkupContainer("textIndicator")
        {
            private static final long serialVersionUID = 4346767114287766710L;

            @Override
            public boolean isVisible()
            {
                return getModelObject().indicator.isAffected();
            }
        }.add(new AttributeAppender("class", new Model<String>()
        {
            private static final long serialVersionUID = -7683195283137223296L;

            @Override
            public String getObject()
            {
                // adds symbol to indicator
                return getModelObject().indicator.getStatusSymbol();
            }
        })).add(new AttributeAppender("style", new Model<String>()
        {
            private static final long serialVersionUID = -5255873539738210137L;

            @Override
            public String getObject()
            {
                // adds color to indicator
                return "; color: " + getModelObject().indicator.getStatusColor();
            }
        }));
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        LOG.trace("TextFeatureEditor(path: " + getPageRelativePath() + ", "
                + getModelObject().feature.getUiName() + ": " + getModelObject().value + ")");
    }

    @Override
    public Component getFocusComponent()
    {
        return field;
    }

    /**
     * Hides feature if "Hide un-constraint feature" is enabled and constraint rules are applied and
     * feature doesn't match any constraint rule
     */
    @Override
    public void onConfigure()
    {
        super.onConfigure();
        
        // if enabled and constraints rule execution returns anything other than green
        setVisible(!hideUnconstrainedFeature || (getModelObject().indicator.isAffected()
                && getModelObject().indicator.getStatusColor().equals("green")));
    }
}
