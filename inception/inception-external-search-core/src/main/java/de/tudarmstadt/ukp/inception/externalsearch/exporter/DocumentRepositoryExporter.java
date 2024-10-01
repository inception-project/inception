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
package de.tudarmstadt.ukp.inception.externalsearch.exporter;

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
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.config.ExternalSearchAutoConfiguration;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ExternalSearchAutoConfiguration#documentRepositoryExporter(ExternalSearchService)}.
 * </p>
 */
public class DocumentRepositoryExporter
    implements ProjectExporter
{
    private static final String KEY = "external_search";
    private static final Logger LOG = LoggerFactory.getLogger(DocumentRepositoryExporter.class);

    private final ExternalSearchService externalSearchService;

    @Autowired
    public DocumentRepositoryExporter(ExternalSearchService aExternalSearchService)
    {
        externalSearchService = aExternalSearchService;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aFile)
    {
        var project = aRequest.getProject();
        List<ExportedDocumentRepository> exportedDocumentRepositories = new ArrayList<>();
        for (var documentRepository : externalSearchService.listDocumentRepositories(project)) {
            ExportedDocumentRepository exportedDocumentRepository = new ExportedDocumentRepository();
            exportedDocumentRepository.setId(documentRepository.getId());
            exportedDocumentRepository.setName(documentRepository.getName());
            exportedDocumentRepository.setProperties(documentRepository.getProperties());
            exportedDocumentRepository.setType(documentRepository.getType());
            exportedDocumentRepositories.add(exportedDocumentRepository);
        }
        aExProject.setProperty(KEY, exportedDocumentRepositories);
        int n = exportedDocumentRepositories.size();
        LOG.info("Exported [{}] document repositories for project [{}]", n, project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
    {
        var exportedDocumentRepositories = aExProject.getArrayProperty(KEY,
                ExportedDocumentRepository.class);

        for (var exportedDocumentRepository : exportedDocumentRepositories) {
            var documentRepository = new DocumentRepository();
            documentRepository.setName(exportedDocumentRepository.getName());
            documentRepository.setProperties(exportedDocumentRepository.getProperties());
            documentRepository.setType(exportedDocumentRepository.getType());
            documentRepository.setProject(aProject);
            externalSearchService.createOrUpdateDocumentRepository(documentRepository);
        }

        int n = exportedDocumentRepositories.length;
        LOG.info("Imported [{}] document repositories for project [{}]", n, aProject.getName());
    }
}
