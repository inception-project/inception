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
package de.tudarmstadt.ukp.inception.guidelines.exporters;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.guidelines.GuidelinesServiceImpl;

class GuidelinesExporterTest
{
    private static final String GUIDELINE_PDF = "guideline.pdf";
    private static final byte[] PDF = "%PDF-1.4\n%%EOF\n".getBytes(UTF_8);

    private @TempDir File tempDir;

    private GuidelinesServiceImpl guidelinesService;

    private GuidelinesExporter sut;

    private Project sourceProject;
    private Project targetProject;

    @BeforeEach
    void setup()
    {
        sourceProject = Project.builder() //
                .withId(1l) //
                .withName("Test Project") //
                .build();

        targetProject = Project.builder() //
                .withId(2l) //
                .withName("Test Project") //
                .build();

        var repositoryProperties = new RepositoryPropertiesImpl();
        repositoryProperties.setPath(tempDir);

        guidelinesService = new GuidelinesServiceImpl(repositoryProperties);

        sut = new GuidelinesExporter(guidelinesService);
    }

    @Test
    void thatExportingAndImportingAgainWorks() throws Exception
    {
        try (var is = new ByteArrayInputStream(PDF)) {
            guidelinesService.createGuideline(sourceProject, is, GUIDELINE_PDF);
        }

        var exportedProject = new ExportedProject();
        var exportFile = export(exportedProject);
        importInto(exportFile, exportedProject);

        assertThat(guidelinesService.listGuidelines(targetProject)) //
                .containsExactly(GUIDELINE_PDF);
    }

    @Test
    void thatLegacyNonPdfGuidelineIsExportedButSkippedOnImport() throws Exception
    {
        try (var is = new ByteArrayInputStream(PDF)) {
            guidelinesService.createGuideline(sourceProject, is, GUIDELINE_PDF);
        }

        // Files that were stored before guidelines were restricted to PDF
        write(guidelinesService.getGuideline(sourceProject, "legacy.html").toPath(),
                "<script>alert(1)</script>".getBytes(UTF_8));

        var exportedProject = new ExportedProject();
        var exportFile = export(exportedProject);

        try (var zipFile = new ZipFile(exportFile)) {
            assertThat(zipFile.stream().map(e -> e.getName())) //
                    .containsExactlyInAnyOrder("guideline/" + GUIDELINE_PDF,
                            "guideline/legacy.html");
        }

        var importRequest = importInto(exportFile, exportedProject);

        assertThat(guidelinesService.listGuidelines(targetProject)) //
                .containsExactly(GUIDELINE_PDF);
        assertThat(importRequest.getMessages()) //
                .singleElement().asString().contains("legacy.html");
    }

    @Test
    void thatEntriesWithRejectedNamesAreSkippedAndTheRestIsImported() throws Exception
    {
        var zip = new File(tempDir, "entries.zip");
        try (var zos = new ZipOutputStream(new FileOutputStream(zip))) {
            for (var name : new String[] { "guideline/first.pdf", "guideline/bad&name.pdf",
                    "guideline/" + "a".repeat(260) + ".pdf", "guideline/second.pdf",
                    "guideline/sub/nested.pdf", "guideline/sub/", "guideline\\backslash.pdf",
                    "guideline/sub\\nested.pdf" }) {
                zos.putNextEntry(new ZipEntry(name));
                zos.write(PDF);
                zos.closeEntry();
            }
        }

        var importRequest = importInto(zip, new ExportedProject());

        assertThat(guidelinesService.listGuidelines(targetProject)) //
                .containsExactly("first.pdf", "second.pdf");
        // bad&name.pdf, the overlong name and the name with a backslash
        assertThat(importRequest.getMessages()).hasSize(3);
    }

    @Test
    void thatExportingProjectWithoutGuidelinesWorks() throws Exception
    {
        var exportedProject = new ExportedProject();
        var exportFile = export(exportedProject);

        try (var zipFile = new ZipFile(exportFile)) {
            assertThat(zipFile.size()).isZero();
        }
    }

    private File export(ExportedProject aExportedProject) throws Exception
    {
        var exportFile = new File(tempDir, "export.zip");
        var exportRequest = FullProjectExportRequest.builder().withProject(sourceProject).build();
        var monitor = new ProjectExportTaskMonitor(sourceProject, null, "test",
                exportRequest.getFilenamePrefix());

        try (var zos = new ZipOutputStream(new FileOutputStream(exportFile))) {
            sut.exportData(exportRequest, monitor, aExportedProject, zos);
        }

        return exportFile;
    }

    private ProjectImportRequest importInto(File aExportFile, ExportedProject aExportedProject)
        throws Exception
    {
        var importRequest = ProjectImportRequest.builder().build();
        try (var zipFile = new ZipFile(aExportFile)) {
            sut.importData(importRequest, targetProject, aExportedProject, zipFile);
        }
        return importRequest;
    }
}
