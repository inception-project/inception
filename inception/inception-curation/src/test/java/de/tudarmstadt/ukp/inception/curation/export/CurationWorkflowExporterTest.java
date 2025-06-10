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
package de.tudarmstadt.ukp.inception.curation.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

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
import de.tudarmstadt.ukp.inception.curation.merge.strategy.ThresholdBasedMergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.ThresholdBasedMergeStrategyFactoryImpl;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.ThresholdBasedMergeStrategyTraits;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;

@ExtendWith(MockitoExtension.class)
public class CurationWorkflowExporterTest
{
    private @Mock CurationService curationService;
    private Project sourceProject;
    private Project targetProject;

    private CurationWorkflowExporter sut;

    private ThresholdBasedMergeStrategyFactory factory;

    @BeforeEach
    public void setUp()
    {
        sourceProject = Project.builder() //
                .withId(1l) //
                .withName("Test Project") //
                .build();

        targetProject = Project.builder() //
                .withId(2l) //
                .withName("Test Project") //
                .build();

        sut = new CurationWorkflowExporter(curationService);

        factory = new ThresholdBasedMergeStrategyFactoryImpl();
    }

    @Test
    public void thatExportingWorks() throws Exception
    {
        when(curationService.readOrCreateCurationWorkflow(any()))
                .thenReturn(curationWorkflow(sourceProject));

        // Export the project
        var exportRequest = new FullProjectExportRequest(sourceProject, null, false);
        var monitor = new ProjectExportTaskMonitor(sourceProject, null, "test",
                exportRequest.getFilenamePrefix());
        var exportedProject = new ExportedProject();
        var stage = mock(ZipOutputStream.class);

        sut.exportData(exportRequest, monitor, exportedProject, stage);

        reset(curationService);

        // Import the project again
        var captor = ArgumentCaptor.forClass(CurationWorkflow.class);
        doNothing().when(curationService).createOrUpdateCurationWorkflow(captor.capture());

        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(true) //
                .withImportPermissions(true) //
                .build();
        var zipFile = mock(ZipFile.class);
        sut.importData(importRequest, targetProject, exportedProject, zipFile);

        // Export the project and import it again
        // Check that after re-importing the exported projects, they are identical to the original
        assertThat(captor.getAllValues()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactly(curationWorkflow(targetProject));
    }

    private CurationWorkflow curationWorkflow(Project aProject)
    {
        var curationWorkflow = new CurationWorkflow();
        curationWorkflow.setProject(aProject);
        curationWorkflow.setMergeStrategy(factory.getId());
        factory.writeTraits(curationWorkflow, new ThresholdBasedMergeStrategyTraits());
        return curationWorkflow;
    }
}
