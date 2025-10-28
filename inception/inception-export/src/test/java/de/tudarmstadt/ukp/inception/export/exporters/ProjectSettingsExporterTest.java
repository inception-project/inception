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
package de.tudarmstadt.ukp.inception.export.exporters;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

@ExtendWith(MockitoExtension.class)
class ProjectSettingsExporterTest
{
  private @Mock UserDao userService;
  private @Mock ApplicationEventPublisher applicationEventPublisher;
  private @TempDir File tempDir;
  private ProjectService projectService;
  private ProjectSettingsExporter sut;
  private Project sourceProject;

  @BeforeEach
  void setup()
  {
    sourceProject = Project.builder() //
        .withId(1l) //
        .withName("Test Project") //
        .build();
    var repositoryProperties = new RepositoryPropertiesImpl();
    repositoryProperties.setPath(tempDir);
    projectService = new ProjectServiceImpl(userService, applicationEventPublisher,
        repositoryProperties, emptyList(), emptyList(), null);
    sut = new ProjectSettingsExporter(projectService);
  }

  @Test
  void thatExportingAndImportingSettingsWorks() throws Exception
  {
    var exportFile = new File(tempDir, "settings-export.zip");
    var exportRequest = new FullProjectExportRequest(sourceProject, null, false);
    var monitor = new ProjectExportTaskMonitor(sourceProject, null, "test",
        exportRequest.getFilenamePrefix());
    var exportedProject = new ExportedProject();
    try (var zos = new ZipOutputStream(new java.io.FileOutputStream(exportFile))) {
      sut.exportData(exportRequest, monitor, exportedProject, zos);
    }

    assertThat(exportedProject.getApplicationName()).isEqualTo("INCEpTION");
    assertThat(exportedProject.getApplicationVersion()).isEqualTo("unknown");
  }
}
