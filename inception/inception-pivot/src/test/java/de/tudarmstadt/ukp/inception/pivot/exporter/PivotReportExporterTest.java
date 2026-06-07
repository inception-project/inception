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
package de.tudarmstadt.ukp.inception.pivot.exporter;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
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
import de.tudarmstadt.ukp.inception.pivot.api.model.PivotReport;
import de.tudarmstadt.ukp.inception.pivot.report.ReportService;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@ExtendWith(MockitoExtension.class)
public class PivotReportExporterTest
{
    private @Mock ReportService reportService;

    private Project sourceProject;
    private Project targetProject;

    private PivotReportExporter sut;

    @BeforeEach
    public void setUp()
    {
        sourceProject = new Project();
        sourceProject.setId(1l);
        sourceProject.setName("Source Project");
        sourceProject.setSlug("source-project");

        targetProject = new Project();
        targetProject.setId(2l);
        targetProject.setName("Target Project");
        targetProject.setSlug("target-project");

        sut = new PivotReportExporter(reportService);
    }

    @Test
    public void thatExportingAndImportingWorks() throws Exception
    {
        when(reportService.listReports(sourceProject)).thenReturn(reports(sourceProject));
        when(reportService.listReports(targetProject)).thenReturn(emptyList());

        var captor = runExportImport();

        assertThat(captor.getAllValues()) //
                .extracting(PivotReport::getName, PivotReport::getDescription,
                        PivotReport::getDefinition, PivotReport::getProject) //
                .containsExactly( //
                        tuple("Report A", "First report", "{\"a\":1}", targetProject), //
                        tuple("Report B", null, "{\"b\":2}", targetProject));
    }

    @Test
    public void thatImportingSkipsExistingReportsByName() throws Exception
    {
        when(reportService.listReports(sourceProject)).thenReturn(reports(sourceProject));
        when(reportService.listReports(targetProject))
                .thenReturn(asList(buildReport(targetProject, "Report A", null, "{}")));

        var captor = runExportImport();

        // "Report A" already exists in the target project and must be skipped.
        assertThat(captor.getAllValues()) //
                .extracting(PivotReport::getName) //
                .containsExactly("Report B");
    }

    @Test
    public void thatDefinitionIsEmbeddedAsInlineJson() throws Exception
    {
        var exported = new ExportedPivotReport();
        exported.setName("Report A");
        exported.setDefinition("{\"a\":1}");

        var json = JSONUtil.toJsonString(exported);

        // The definition must be embedded as a real JSON object - not as an escaped string.
        assertThat(json).contains("\"definition\":{\"a\":1}");
        assertThat(json).doesNotContain("\"definition\":\"{");
    }

    @Test
    public void thatBlankDefinitionIsOmitted() throws Exception
    {
        var exported = new ExportedPivotReport();
        exported.setName("Report A");
        exported.setDefinition("");

        // An empty definition would render invalid JSON if written raw - it must be omitted.
        assertThat(JSONUtil.toJsonString(exported)).doesNotContain("definition");
    }

    private ArgumentCaptor<PivotReport> runExportImport() throws Exception
    {
        var exportRequest = FullProjectExportRequest.builder().withProject(sourceProject).build();
        var monitor = new ProjectExportTaskMonitor(sourceProject, null, "test",
                exportRequest.getFilenamePrefix());
        var exportedProject = new ExportedProject();
        var file = mock(ZipOutputStream.class);

        sut.exportData(exportRequest, monitor, exportedProject, file);

        var captor = ArgumentCaptor.forClass(PivotReport.class);
        doNothing().when(reportService).createOrUpdateReport(captor.capture());

        var importRequest = ProjectImportRequest.builder().build();
        sut.importData(importRequest, targetProject, exportedProject, mock(ZipFile.class));

        return captor;
    }

    private List<PivotReport> reports(Project aProject)
    {
        return asList( //
                buildReport(aProject, "Report A", "First report", "{\"a\":1}"), //
                buildReport(aProject, "Report B", null, "{\"b\":2}"));
    }

    private PivotReport buildReport(Project aProject, String aName, String aDescription,
            String aDefinition)
    {
        var report = new PivotReport(aProject, aName);
        report.setDescription(aDescription);
        report.setDefinition(aDefinition);
        return report;
    }
}
