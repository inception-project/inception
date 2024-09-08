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
package de.tudarmstadt.ukp.inception.preferences.exporter;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.preferences.config.PreferencesServiceAutoConfig;
import de.tudarmstadt.ukp.inception.preferences.model.DefaultProjectPreference;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link PreferencesServiceAutoConfig#defaultPreferenceExporter}.
 * </p>
 */
public class DefaultProjectPreferencesExporter
    implements ProjectExporter
{
    private static final String KEY = "default-preferences";
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final PreferencesService preferencesService;

    @Autowired
    public DefaultProjectPreferencesExporter(PreferencesService aPreferencesService)
    {
        preferencesService = aPreferencesService;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
    {
        var project = aRequest.getProject();

        var exportedDefaultPreferences = new ArrayList<>();
        for (var defaultPreference : preferencesService.listDefaultTraitsForProject(project)) {
            var exportedDefaultPreference = new ExportedDefaultProjectPreference();
            exportedDefaultPreference.setName(defaultPreference.getName());
            exportedDefaultPreference.setTraits(defaultPreference.getTraits());
            exportedDefaultPreferences.add(exportedDefaultPreference);
        }

        aExProject.setProperty(KEY, exportedDefaultPreferences);
        int n = exportedDefaultPreferences.size();
        LOG.info("Exported [{}] default preferences for project [{}]", n, project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
    {
        var exportedDefaultPreferences = aExProject.getArrayProperty(KEY,
                ExportedDefaultProjectPreference.class);

        for (var exportedDefaultPreference : exportedDefaultPreferences) {
            var defaultPreference = new DefaultProjectPreference();
            defaultPreference.setProject(aProject);
            defaultPreference.setName(exportedDefaultPreference.getName());
            defaultPreference.setTraits(exportedDefaultPreference.getTraits());

            preferencesService.saveDefaultProjectPreference(defaultPreference);
        }

        int n = exportedDefaultPreferences.length;
        LOG.info("Imported [{}] default preferences for project [{}]", n, aProject.getName());
    }
}
