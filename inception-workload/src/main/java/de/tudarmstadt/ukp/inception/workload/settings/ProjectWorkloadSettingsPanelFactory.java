/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.inception.workload.settings;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelFactory;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagerAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WorkloadManagerAutoConfiguration#projectWorkloadSettingsPanelFactory}.
 * </p>
 */
@Order(300)
public class ProjectWorkloadSettingsPanelFactory
    implements ProjectSettingsPanelFactory
{
    @Override
    public String getPath()
    {
        return "/workload";
    }

    @Override
    public String getLabel()
    {
        return "Workload";
    }

    @Override
    public Panel createSettingsPanel(String aID, final IModel<Project> aProjectModel)
    {
        return new WorkloadSettingsPanel(aID, aProjectModel);
    }
}
