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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.constraints;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelFactory;

@Component
@Order(ConstraintsProjectSettingsPanelFactory.ORDER)
public class ConstraintsProjectSettingsPanelFactory
    implements ProjectSettingsPanelFactory
{
    public static final int ORDER = 5000;

    @Override
    public String getPath()
    {
        return "/constraints";
    }

    @Override
    public String getLabel()
    {
        return new ResourceModel("constraintsprojectsettings.panel.factory.label").getObject();
    }

    @Override
    public Panel createSettingsPanel(String aID, final IModel<Project> aProjectModel)
    {
        return new ProjectConstraintsPanel(aID, aProjectModel);
    }
}
