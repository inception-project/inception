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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.editor;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;

public abstract class FeatureEditor
    extends Fragment
{
    private static final long serialVersionUID = -7275181609671919722L;

    protected static final String ID_PREFIX = "featureEditorHead";
    
    public FeatureEditor(String aId, String aMarkupId, MarkupContainer aMarkupProvider,
            IModel<FeatureState> aModel)
    {
        super(aId, aMarkupId, aMarkupProvider, aModel);
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
    
    abstract public Component getFocusComponent();
}