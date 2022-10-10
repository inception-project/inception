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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.dangerzone;

import org.apache.wicket.Page;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.ProjectSettingsMenuItemBase;
import de.tudarmstadt.ukp.inception.ui.project.dangerzone.ProjectDangerZonePanelFactory;

@Component
@Order(ProjectDangerZonePanelFactory.ORDER)
public class ProjectDangerZoneMenuItem
    extends ProjectSettingsMenuItemBase
{
    @Override
    public String getPath()
    {
        return "/settings/dangerzone";
    }

    @Override
    public IconType getIcon()
    {
        return FontAwesome5IconType.skull_crossbones_s;
    }

    @Override
    public String getLabel()
    {
        return "Danger Zone";
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return ProjectDangerZonePage.class;
    }
}
