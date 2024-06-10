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

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
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
import de.tudarmstadt.ukp.clarin.webanno.project.exporters.ProjectPermissionsExporter;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.preferences.config.PreferencesServiceAutoConfig;
import de.tudarmstadt.ukp.inception.preferences.model.UserProjectPreference;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link PreferencesServiceAutoConfig#userPreferencesExporter}.
 * </p>
 */
public class UserProjectPreferencesExporter
    implements ProjectExporter
{
    private static final String KEY = "user-preferences";
    private static final Logger LOG = LoggerFactory.getLogger(UserProjectPreferencesExporter.class);

    private final PreferencesService preferencesService;
    private final UserDao userRepository;

    @Autowired
    public UserProjectPreferencesExporter(PreferencesService aPreferencesService,
            UserDao aUserRepository)
    {
        preferencesService = aPreferencesService;
        userRepository = aUserRepository;
    }

    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies()
    {
        return asList(ProjectPermissionsExporter.class);
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aFile)
    {
        var project = aRequest.getProject();

        var exportedDefaultPreferences = new ArrayList<ExportedUserProjectPreference>();
        for (var userPreference : preferencesService.listUserPreferencesForProject(project)) {
            var exportedDefaultPreference = new ExportedUserProjectPreference();
            exportedDefaultPreference.setUser(userPreference.getUser().getUsername());
            exportedDefaultPreference.setName(userPreference.getName());
            exportedDefaultPreference.setTraits(userPreference.getTraits());
            exportedDefaultPreferences.add(exportedDefaultPreference);
        }

        aExProject.setProperty(KEY, exportedDefaultPreferences);
        LOG.info("Exported [{}] user preferences for project [{}]",
                exportedDefaultPreferences.size(), project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
    {
        var exportedDefaultPreferences = aExProject.getArrayProperty(KEY,
                ExportedUserProjectPreference.class);

        var importedPreferences = 0;
        var missingUsers = 0;
        for (var exportedDefaultPreference : exportedDefaultPreferences) {
            var user = userRepository.get(exportedDefaultPreference.getUser());
            if (user == null) {
                missingUsers++;
                continue;
            }

            var userPreference = new UserProjectPreference();
            userPreference.setProject(aProject);
            userPreference.setUser(user);
            userPreference.setName(exportedDefaultPreference.getName());
            userPreference.setTraits(exportedDefaultPreference.getTraits());

            preferencesService.saveUserProjectPreference(userPreference);
            importedPreferences++;
        }

        LOG.info("Imported [{}] user preferences for project [{}]", importedPreferences,
                aProject.getName());
        if (missingUsers > 0) {
            LOG.info(
                    "[{}] user preferences for project [{}] were not imported because the users do not exist",
                    missingUsers, aProject.getName());
        }
    }
}
