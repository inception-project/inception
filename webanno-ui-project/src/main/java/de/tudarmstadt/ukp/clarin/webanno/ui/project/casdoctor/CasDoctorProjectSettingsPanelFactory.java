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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.casdoctor;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelFactory;

@Component
@Order(500)
public class CasDoctorProjectSettingsPanelFactory
    implements ProjectSettingsPanelFactory
{
    @Override
    public String getPath()
    {
        return "/cas-doctor";
    }

    @Override
    public String getLabel()
    {
        return "CAS Doctor";
    }

    @Override
    public Panel createSettingsPanel(String aID, final IModel<Project> aProjectModel)
    {
        return new ProjectCasDoctorPanel(aID, aProjectModel);
    }
}
