/*
 * Copyright 2017
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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;

public abstract class FeatureEditor
    extends Panel
{
    private static final long serialVersionUID = -7275181609671919722L;

    protected static final String MID_FEATURE = "feature";
    protected static final String MID_VALUE = "value";

    private MarkupContainer owner;
    
    /**
     * @param aId
     *            the component ID.
     * @param aOwner
     *            an enclosing component which may contain other feature editors. If actions are
     *            performed which may affect other feature editors, e.g because of constraints
     *            rules, then these need to be re-rendered. This is done by requesting a
     *            re-rendering of the enclosing component.
     * @param aModel
     *            provides access to the state of the feature being edited.
     */
    public FeatureEditor(String aId, MarkupContainer aOwner, IModel<FeatureState> aModel)
    {
        super(aId, aModel);
        owner = aOwner;
        
        add(createLabel());
    }

    public MarkupContainer getOwner()
    {
        return owner;
    }
    
    public Component getLabelComponent()
    {
        return get("feature");
    }

    public IModel<FeatureState> getModel()
    {
        return (IModel<FeatureState>) getDefaultModel();
    }
    
    public FeatureState getModelObject()
    {
        return (FeatureState) getDefaultModelObject();
    }
    
    private Component createLabel()
    {
        return new Label(MID_FEATURE, getModelObject().feature.getUiName());
    }
    
    abstract public Component getFocusComponent();
}
