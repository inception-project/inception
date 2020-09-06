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

import java.util.Map;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.StyleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;

public abstract class TextFeatureEditorBase
    extends FeatureEditor
{
    private static final long serialVersionUID = -3499366171559879681L;

    private static final Logger LOG = LoggerFactory.getLogger(TextFeatureEditorBase.class);
    
    @SuppressWarnings("rawtypes")
    private FormComponent field;
    private boolean hideUnconstrainedFeature;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

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
        // Shows whether constraints are triggered or not also shows state of constraints use.
        Component indicator = new WebMarkupContainer("textIndicator");
        indicator.add(LambdaBehavior.visibleWhen(() -> getModelObject().indicator.isAffected()));
        indicator.add(new ClassAttributeModifier()
        {
            private static final long serialVersionUID = 4623544241209220039L;

            @Override
            protected Set<String> update(Set<String> aOldClasses)
            {
                aOldClasses.add(getModelObject().indicator.getStatusSymbol());
                return aOldClasses;
            }
        });
        indicator.add(new StyleAttributeModifier()
        {
            private static final long serialVersionUID = 3627596292626670610L;

            @Override
            protected Map<String, String> update(Map<String, String> aStyles)
            {
                aStyles.put("color", getModelObject().indicator.getStatusColor());
                return aStyles;
            }
        });
        indicator.add(
                new AttributeModifier("title", getModelObject().indicator.getStatusDescription()));
        return indicator;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        LOG.trace("TextFeatureEditor(path: " + getPageRelativePath() + ", "
                + getModelObject().feature.getUiName() + ": " + getModelObject().value + ")");
    }

    @Override
    public FormComponent getFocusComponent()
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

    public StringFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature)
    {
        FeatureSupport<StringFeatureTraits> fs = featureSupportRegistry
                .getFeatureSupport(aAnnotationFeature);
        StringFeatureTraits traits = fs.readTraits(aAnnotationFeature);
        return traits;
    }
}
