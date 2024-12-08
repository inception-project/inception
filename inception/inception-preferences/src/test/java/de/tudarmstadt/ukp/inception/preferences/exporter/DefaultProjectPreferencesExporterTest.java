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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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

@ExtendWith(MockitoExtension.class)
class DefaultProjectPreferencesExporterTest
{
    private @TempDir File tempFolder;

    private @Mock PreferencesService preferencesService;

    private Project sourceProject;
    private Project targetProject;

    private DefaultProjectPreferencesExporter sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        sourceProject = Project.builder() //
                .withId(1l) //
                .withName("Test Project") //
                .build();

        targetProject = Project.builder() //
                .withId(2l) //
                .withName("Test Project") //
                .build();

        sut = new DefaultProjectPreferencesExporter(preferencesService);
    }

    @Test
    void thatExportingAndImportingAgainWorks() throws Exception
    {
        when(preferencesService.listDefaultTraitsForProject(any(Project.class))) //
                .thenReturn(defaultTraits(sourceProject));

        var exportFile = new File(tempFolder, "export.zip");

        // Export the project
        var exportRequest = new FullProjectExportRequest(sourceProject, null, false);
        var monitor = new ProjectExportTaskMonitor(sourceProject, null, "test",
                exportRequest.getFilenamePrefix());
        var exportedProject = new ExportedProject();

        try (var zos = new ZipOutputStream(new FileOutputStream(exportFile))) {
            sut.exportData(exportRequest, monitor, exportedProject, zos);
        }

        reset(preferencesService);

        // Import the project again
        var captor = ArgumentCaptor.forClass(DefaultProjectPreference.class);
        doNothing().when(preferencesService).saveDefaultProjectPreference(captor.capture());

        var importRequest = ProjectImportRequest.builder().build();
        try (var zipFile = new ZipFile(exportFile)) {
            sut.importData(importRequest, targetProject, exportedProject, zipFile);
        }

        assertThat(captor.getAllValues()) //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id") //
                .containsExactlyElementsOf(defaultTraits(targetProject));
    }

    private List<DefaultProjectPreference> defaultTraits(Project aProject)
    {
        var result = new ArrayList<DefaultProjectPreference>();

        for (var i = 1l; i <= 10; i++) {
            var pref = new DefaultProjectPreference();
            pref.setId(i);
            pref.setProject(aProject);
            pref.setName("pref");
            pref.setTraits("traits");
            result.add(pref);
        }

        return result;
    }
}
