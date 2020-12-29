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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ResourceReference;

public abstract class SidebarTab
    extends AbstractTab
{
    private static final long serialVersionUID = -3205381571000021331L;

    private ResourceReference icon;

    public SidebarTab(IModel<String> aTitle, ResourceReference aIcon)
    {
        super(aTitle);
        icon = aIcon;
    }

    public ResourceReference getIcon()
    {
        return icon;
    }
}
