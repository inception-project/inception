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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.pivot.api.model.PivotReport;
import de.tudarmstadt.ukp.inception.pivot.config.PivotAutoConfiguration;
import de.tudarmstadt.ukp.inception.pivot.report.ReportService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link PivotAutoConfiguration#pivotReportExporter}.
 * </p>
 */
public class PivotReportExporter
    implements ProjectExporter
{
    private static final String KEY = "pivot-reports";
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ReportService reportService;

    public PivotReportExporter(ReportService aReportService)
    {
        reportService = aReportService;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aFile)
    {
        var project = aRequest.getProject();

        var exportedReports = new ArrayList<ExportedPivotReport>();
        for (var report : reportService.listReports(project)) {
            var exportedReport = new ExportedPivotReport();
            exportedReport.setName(report.getName());
            exportedReport.setDescription(report.getDescription());
            exportedReport.setDefinition(report.getDefinition());
            exportedReports.add(exportedReport);
        }

        aExProject.setProperty(KEY, exportedReports);
        LOG.info("Exported [{}] pivot reports for project [{}]", exportedReports.size(),
                project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
    {
        var exportedReports = aExProject.getArrayProperty(KEY, ExportedPivotReport.class);

        var existingNames = new HashSet<String>();
        for (var report : reportService.listReports(aProject)) {
            existingNames.add(report.getName());
        }

        var importedReports = 0;
        var skippedReports = 0;
        for (var exportedReport : exportedReports) {
            // The (name, project) pair is unique - skip rather than fail if a report with the same
            // name already exists in the target project.
            if (!existingNames.add(exportedReport.getName())) {
                skippedReports++;
                continue;
            }

            var report = new PivotReport(aProject, exportedReport.getName());
            report.setDescription(exportedReport.getDescription());
            report.setDefinition(exportedReport.getDefinition());

            reportService.createOrUpdateReport(report);
            importedReports++;
        }

        LOG.info("Imported [{}] pivot reports for project [{}]", importedReports,
                aProject.getName());
        if (skippedReports > 0) {
            LOG.info("[{}] pivot reports for project [{}] were not imported because a report with "
                    + "the same name already exists", skippedReports, aProject.getName());
        }
    }
}
