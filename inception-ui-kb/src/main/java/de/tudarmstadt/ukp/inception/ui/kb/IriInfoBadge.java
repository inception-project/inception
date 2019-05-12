/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.ui.kb;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.widget.tooltip.TooltipBehavior;

public class IriInfoBadge extends Panel
{
    private static final long serialVersionUID = 1L;

    private TooltipBehavior tip;
    
    public IriInfoBadge(String aId, IModel<String> aModel)
    {
        super(aId, aModel);
        
        WebMarkupContainer iri = new WebMarkupContainer("iri"); 
        
        tip = new TooltipBehavior();
        tip.setOption("autoHide", false);
        tip.setOption("showOn", Options.asString("click"));
        iri.add(tip);
        
        add(iri);
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        tip.setOption("content", Options.asString(getModelObject()));
    }
    
    public String getModelObject()
    {
        return (String) getDefaultModelObject();
    }
}
