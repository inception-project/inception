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
package de.tudarmstadt.ukp.inception.documents.exporters;

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.DOCUMENT_FOLDER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.SOURCE_FOLDER;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.createDirectories;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentServiceAutoConfiguration#sourceDocumentExporter}.
 * </p>
 */
public class SourceDocumentExporter
    implements ProjectExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DocumentService documentService;
    private final DocumentStorageService documentStorageService;
    private final RepositoryProperties repositoryProperties;

    public SourceDocumentExporter(DocumentService aDocumentService,
            DocumentStorageService aDocumentStorageService,
            RepositoryProperties aRepositoryProperties)
    {
        documentService = aDocumentService;
        repositoryProperties = aRepositoryProperties;
        documentStorageService = aDocumentStorageService;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
        throws IOException, ProjectExportException, InterruptedException
    {
        exportSourceDocuments(aRequest.getProject(), aExProject);
        exportSourceDocumentContents(aRequest, aMonitor, aExProject, aStage);
    }

    private void exportSourceDocuments(Project aProject, ExportedProject exProject)
    {
        var sourceDocuments = new ArrayList<ExportedSourceDocument>();

        // add source documents to a project
        var documents = documentService.listSourceDocuments(aProject);
        for (var sourceDocument : documents) {
            ExportedSourceDocument exDocument = new ExportedSourceDocument();
            exDocument.setFormat(sourceDocument.getFormat());
            exDocument.setName(sourceDocument.getName());
            exDocument.setState(sourceDocument.getState());
            exDocument.setTimestamp(sourceDocument.getTimestamp());
            exDocument.setCreated(sourceDocument.getCreated());
            exDocument.setUpdated(sourceDocument.getUpdated());

            sourceDocuments.add(exDocument);
        }

        exProject.setSourceDocuments(sourceDocuments);
    }

    private void exportSourceDocumentContents(FullProjectExportRequest aRequest,
            ProjectExportTaskMonitor aMonitor, ExportedProject aExProject, ZipOutputStream aStage)
        throws IOException, ProjectExportException, InterruptedException
    {
        var project = aRequest.getProject();
        // Get all the source documents from the project
        var documents = documentService.listSourceDocuments(project);
        int i = 1;
        for (var sourceDocument : documents) {
            // check if the export has been cancelled
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            try {
                ProjectExporter.writeEntry(aStage, SOURCE_FOLDER + "/" + sourceDocument.getName(),
                        os -> {
                            try (var is = Files.newInputStream(documentStorageService
                                    .getSourceDocumentFile(sourceDocument).toPath())) {
                                is.transferTo(os);
                            }
                        });

                aMonitor.setProgress((int) Math.ceil(((double) i) / documents.size() * 10.0));
                LOG.info("Exported content for source document {}/{}: {} in {}", i,
                        documents.size(), sourceDocument, project);
                i++;
            }
            catch (FileNotFoundException e) {
                LOG.error("Source file [{}] related to project couldn't be located in repository",
                        sourceDocument.getName(), ExceptionUtils.getRootCause(e));
                aMonitor.addMessage(LogMessage.error(this,
                        "Source file [%s] related to project couldn't be located in repository",
                        sourceDocument.getName()));
                throw new ProjectExportException(
                        "Couldn't find some source file(s) related to project");
            }
        }
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        var start = currentTimeMillis();

        importSourceDocuments(aExProject, aProject);
        importSourceDocumentContents(aZip, aProject);

        LOG.info("Imported [{}] source documents into aProject ({})",
                aExProject.getSourceDocuments().size(), aProject,
                formatDurationWords(currentTimeMillis() - start, true, true));
    }

    /**
     * Create s {@link SourceDocument} from the exported {@link SourceDocument}
     * 
     * @param aImportedProjectSetting
     *            the exported project.
     * @param aImportedProject
     *            the project.
     * @throws IOException
     *             if an I/O error occurs.
     */
    private void importSourceDocuments(ExportedProject aImportedProjectSetting,
            Project aImportedProject)
        throws IOException
    {
        for (var importedSourceDocument : aImportedProjectSetting.getSourceDocuments()) {
            var sourceDocument = new SourceDocument();
            sourceDocument.setFormat(importedSourceDocument.getFormat());
            sourceDocument.setName(importedSourceDocument.getName());
            sourceDocument.setState(importedSourceDocument.getState());
            sourceDocument.setProject(aImportedProject);
            sourceDocument.setTimestamp(importedSourceDocument.getTimestamp());
            sourceDocument.setCreated(importedSourceDocument.getCreated());
            sourceDocument.setUpdated(importedSourceDocument.getUpdated());

            documentService.createSourceDocument(sourceDocument);
        }
    }

    /**
     * copy source document files from the exported source documents
     * 
     * @param zip
     *            the ZIP file.
     * @param aProject
     *            the project.
     * @throws IOException
     *             if an I/O error occurs.
     */
    private void importSourceDocumentContents(ZipFile zip, Project aProject) throws IOException
    {
        // Query once for all the documents to avoid hitting the DB in the loop below
        var docs = documentService.listSourceDocuments(aProject).stream()
                .collect(toMap(SourceDocument::getName, identity()));

        // Create the folder structure for the project. This saves time over waiting for the
        // mkdirs in FastIOUtils.copy to kick in.
        var docRoot = Paths.get(repositoryProperties.getPath().getAbsolutePath(), PROJECT_FOLDER,
                aProject.getId().toString(), DOCUMENT_FOLDER);
        createDirectories(docRoot);

        for (var doc : docs.values()) {
            createDirectories(
                    docRoot.resolve(doc.getId().toString()).resolve(ProjectService.SOURCE_FOLDER));
        }

        int n = 0;
        for (var entries = zip.entries(); entries.hasMoreElements();) {
            var entry = entries.nextElement();

            if (entry.isDirectory()) {
                continue;
            }

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            var entryName = ProjectExporter.normalizeEntryName(entry);

            if (entryName.startsWith(ProjectService.SOURCE_FOLDER)) {
                var fileName = FilenameUtils.getName(entryName);
                if (fileName.trim().isEmpty()) {
                    continue;
                }

                var sourceDocument = docs.get(fileName);
                documentStorageService.writeSourceDocumentFile(sourceDocument,
                        zip.getInputStream(entry));

                n++;
                LOG.info("Imported content for source document {}/{}: {} in {}", n, docs.size(),
                        sourceDocument, aProject);
            }
        }
    }
}
