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
package de.tudarmstadt.ukp.clarin.webanno.constraints.export;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
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
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@ExtendWith(MockitoExtension.class)
class ConstraintsExporterTest
{
    private static final String DATA = "data";

    private @TempDir File tempFolder;

    private @Mock ConstraintsService constraintsService;

    private Project sourceProject;
    private Project targetProject;

    private ConstraintsExporter sut;

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

        sut = new ConstraintsExporter(constraintsService);
    }

    @Test
    void thatExportingAndImportingAgainWorks() throws Exception
    {
        when(constraintsService.listConstraintSets(any(Project.class))) //
                .thenReturn(constraintSets(sourceProject));
        when(constraintsService.exportConstraintAsFile(any())) //
                .thenAnswer(call -> constraintSetFile(call.getArgument(0)));

        var exportFile = new File(tempFolder, "export.zip");

        // Export the project
        var exportRequest = new FullProjectExportRequest(sourceProject, null, false);
        var monitor = new ProjectExportTaskMonitor(sourceProject, null, "test",
                exportRequest.getFilenamePrefix());
        var exportedProject = new ExportedProject();

        try (var zos = new ZipOutputStream(new FileOutputStream(exportFile))) {
            sut.exportData(exportRequest, monitor, exportedProject, zos);
        }

        reset(constraintsService);

        // Import the project again
        var constraintSetCaptor = ArgumentCaptor.forClass(ConstraintSet.class);
        doNothing().when(constraintsService)
                .createOrUpdateConstraintSet(constraintSetCaptor.capture());
        var constraintDataCaptured = new ArrayList<String>();
        doAnswer(invocation -> {
            var is = invocation.getArgument(1, InputStream.class);
            constraintDataCaptured.add(IOUtils.toString(is, UTF_8));
            return null;
        }).when(constraintsService).writeConstraintSet(any(), any());

        var importRequest = ProjectImportRequest.builder().build();
        try (var zipFile = new ZipFile(exportFile)) {
            sut.importData(importRequest, targetProject, exportedProject, zipFile);
        }

        assertThat(constraintSetCaptor.getAllValues()) //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id") //
                .containsExactlyElementsOf(constraintSets(targetProject));

        assertThat(constraintDataCaptured) //
                .allMatch(DATA::equals);
    }

    private File constraintSetFile(ConstraintSet aSet) throws IOException
    {
        var file = new File(tempFolder, aSet.getName());
        write(file, DATA, UTF_8);
        return file;
    }

    private List<ConstraintSet> constraintSets(Project aProject)
    {
        var result = new ArrayList<ConstraintSet>();

        for (var i = 1l; i <= 10l; i++) {
            var constraintSet = new ConstraintSet();
            constraintSet.setId(i);
            constraintSet.setProject(aProject);
            constraintSet.setName("set" + i + ".txt");
            result.add(constraintSet);
        }

        return result;
    }
}
