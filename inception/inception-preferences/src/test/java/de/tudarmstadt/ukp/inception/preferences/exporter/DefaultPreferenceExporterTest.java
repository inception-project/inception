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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.preferences.model.DefaultProjectPreference;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@ExtendWith(MockitoExtension.class)
public class DefaultPreferenceExporterTest
{
    private @Mock PreferencesService preferencesService;

    private Project project;

    private DefaultProjectPreferencesExporter sut;

    @BeforeEach
    public void setUp()
    {
        project = new Project();
        project.setId(1l);
        project.setName("Test Project");

        sut = new DefaultProjectPreferencesExporter(preferencesService);
    }

    @Test
    public void thatExportingWorks() throws Exception
    {
        when(preferencesService.listDefaultTraitsForProject(project))
                .thenReturn(defaultPreferences());

        // Export the project and import it again
        ArgumentCaptor<DefaultProjectPreference> captor = runExportImportAndFetchRecommenders();

        // Check that after re-importing the exported projects, they are identical to the original
        assertThat(captor.getAllValues()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactlyInAnyOrderElementsOf(defaultPreferences());
    }

    private ArgumentCaptor<DefaultProjectPreference> runExportImportAndFetchRecommenders()
        throws Exception
    {
        // Export the project
        var exportRequest = new FullProjectExportRequest(project, null, false);
        var monitor = new ProjectExportTaskMonitor(project, null, "test",
                exportRequest.getFilenamePrefix());
        var exportedProject = new ExportedProject();
        var file = mock(ZipOutputStream.class);

        sut.exportData(exportRequest, monitor, exportedProject, file);

        // Import the project again
        ArgumentCaptor<DefaultProjectPreference> captor = ArgumentCaptor
                .forClass(DefaultProjectPreference.class);
        doNothing().when(preferencesService).saveDefaultProjectPreference(captor.capture());

        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(true) //
                .withImportPermissions(true) //
                .build();
        var zipFile = mock(ZipFile.class);
        sut.importData(importRequest, project, exportedProject, zipFile);

        return captor;
    }

    private List<DefaultProjectPreference> defaultPreferences() throws IOException
    {
        return asList( //
                buildDefaultPreference("key_1", Map.of("a", 1, "b", 2)), //
                buildDefaultPreference("key_2", Map.of("x", "X", "y", "Y")), //
                buildDefaultPreference("key_3", Map.of("la", true, "lo", false)));
    }

    private DefaultProjectPreference buildDefaultPreference(String aKey, Object aTraits)
        throws IOException
    {
        DefaultProjectPreference defaultPreference = new DefaultProjectPreference();
        defaultPreference.setProject(project);
        defaultPreference.setName(aKey);
        defaultPreference.setTraits(JSONUtil.toJsonString(aTraits));
        return defaultPreference;
    }
}
