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
package de.tudarmstadt.ukp.inception.curation.settings;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelFactory;
import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationServiceAutoConfiguration#curationProjectSettingsPanelFactory}.
 * </p>
 */
@Order(CurationProjectSettingsPanelFactory.ORDER)
public class CurationProjectSettingsPanelFactory
    implements ProjectSettingsPanelFactory
{
    public static final int ORDER = 340;

    @Override
    public String getPath()
    {
        return "/curation";
    }

    @Override
    public String getLabel()
    {
        return "Curation";
    }

    @Override
    public Panel createSettingsPanel(String aID, IModel<Project> aProjectModel)
    {
        return new CurationProjectSettingsPanel(aID, aProjectModel);
    }
}
