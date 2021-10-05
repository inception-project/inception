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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.model.IModel;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;

public abstract class SidebarTab
    extends AbstractTab
{
    private static final long serialVersionUID = -3205381571000021331L;

    private final IconType icon;
    private final String factoryId;

    public SidebarTab(IModel<String> aTitle, IconType aIcon, String aFactoryId)
    {
        super(aTitle);
        icon = aIcon;
        factoryId = aFactoryId;
    }

    public IconType getIcon()
    {
        return icon;
    }

    public String getFactoryId()
    {
        return factoryId;
    }
}
