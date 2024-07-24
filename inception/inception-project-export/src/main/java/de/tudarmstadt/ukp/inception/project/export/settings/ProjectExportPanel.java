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
package de.tudarmstadt.ukp.inception.project.export.settings;

import java.util.List;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportExtension;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportExtensionPoint;

public class ProjectExportPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = -6052972915505762272L;

    private @SpringBean ProjectExportExtensionPoint projectExportExtensionPoint;

    public ProjectExportPanel(String aId, IModel<Project> aModel)
    {
        super(aId, aModel);

        var exporters = new ListView<ProjectExportExtension>("exporters",
                LoadableDetachableModel.of(this::listExtensions))
        {
            private static final long serialVersionUID = 2319141175739179736L;

            @Override
            protected void populateItem(ListItem<ProjectExportExtension> aItem)
            {
                var exporter = aItem.getModelObject();
                aItem.add(exporter.createExporterPanel("exporter",
                        ProjectExportPanel.this.getModel()));
            }
        };
        add(exporters);

        add(new RunningExportsPanel("runningExports", aModel));
    }

    private List<ProjectExportExtension> listExtensions()
    {
        return projectExportExtensionPoint.getExtensions(getModelObject());
    }
}
