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

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.DOCUMENT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.support.io.FastIOUtils.copy;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.createDirectory;
import static java.util.function.Function.identity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentServiceAutoConfiguration#sourceDocumentExporter}.
 * </p>
 */
public class SourceDocumentExporter
    implements ProjectExporter
{
    private static final String SOURCE_FOLDER = "source";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DocumentService documentService;
    private final RepositoryProperties repositoryProperties;

    public SourceDocumentExporter(DocumentService aDocumentService,
            RepositoryProperties aRepositoryProperties)
    {
        documentService = aDocumentService;
        repositoryProperties = aRepositoryProperties;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, File aStage)
        throws IOException, ProjectExportException, InterruptedException
    {
        exportSourceDocuments(aRequest.getProject(), aExProject);
        exportSourceDocumentContents(aRequest, aMonitor, aExProject, aStage);
    }

    private void exportSourceDocuments(Project aProject, ExportedProject exProject)
    {
        List<ExportedSourceDocument> sourceDocuments = new ArrayList<>();

        // add source documents to a project
        List<SourceDocument> documents = documentService.listSourceDocuments(aProject);
        for (SourceDocument sourceDocument : documents) {
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
            ProjectExportTaskMonitor aMonitor, ExportedProject aExProject, File aStage)
        throws IOException, ProjectExportException, InterruptedException
    {
        Project project = aRequest.getProject();
        File sourceDocumentDir = new File(aStage, SOURCE_FOLDER);
        FileUtils.forceMkdir(sourceDocumentDir);
        // Get all the source documents from the project
        List<SourceDocument> documents = documentService.listSourceDocuments(project);
        int i = 1;
        for (SourceDocument sourceDocument : documents) {
            // check if the export has been cancelled
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            try {
                FileUtils.copyFileToDirectory(documentService.getSourceDocumentFile(sourceDocument),
                        sourceDocumentDir);
                aMonitor.setProgress((int) Math.ceil(((double) i) / documents.size() * 10.0));
                i++;
                log.info("Exported content for source document [" + sourceDocument.getId()
                        + "] in project [" + project.getName() + "] with id [" + project.getId()
                        + "]");
            }
            catch (FileNotFoundException e) {
                log.error("Source file [{}] related to project couldn't be located in repository",
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
        long start = currentTimeMillis();

        importSourceDocuments(aExProject, aProject);
        importSourceDocumentContents(aZip, aProject);

        log.info("Imported [{}] source documents for project [{}] ({})",
                aExProject.getSourceDocuments().size(), aExProject.getName(),
                DurationFormatUtils.formatDurationWords(currentTimeMillis() - start, true, true));
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
        for (ExportedSourceDocument importedSourceDocument : aImportedProjectSetting
                .getSourceDocuments()) {
            SourceDocument sourceDocument = new SourceDocument();
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
    @SuppressWarnings("rawtypes")
    private void importSourceDocumentContents(ZipFile zip, Project aProject) throws IOException
    {
        // Query once for all the documents to avoid hitting the DB in the loop below
        Map<String, SourceDocument> docs = documentService.listSourceDocuments(aProject).stream()
                .collect(Collectors.toMap(SourceDocument::getName, identity()));

        // Create the folder structure for the project. This saves time over waiting for the
        // mkdirs in FastIOUtils.copy to kick in.
        Path docRoot = Paths.get(repositoryProperties.getPath().getAbsolutePath(), PROJECT_FOLDER,
                aProject.getId().toString(), DOCUMENT_FOLDER);
        Files.createDirectories(docRoot);
        for (SourceDocument doc : docs.values()) {
            Path docFolder = docRoot.resolve(doc.getId().toString());
            createDirectory(docFolder);
            Path sourceDocFolder = docFolder.resolve(SOURCE_FOLDER);
            createDirectory(sourceDocFolder);
        }

        int n = 0;
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ProjectExporter.normalizeEntryName(entry);

            if (entryName.startsWith(SOURCE_FOLDER)) {
                String fileName = FilenameUtils.getName(entryName);
                if (fileName.trim().isEmpty()) {
                    continue;
                }

                SourceDocument sourceDocument = docs.get(fileName);
                File sourceFilePath = documentService.getSourceDocumentFile(sourceDocument);
                copy(zip.getInputStream(entry), sourceFilePath);

                n++;
                log.info("Imported content for source document {}/{}: [{}]({}) in project [{}]({})",
                        n, docs.size(), sourceDocument.getName(), sourceDocument.getId(),
                        aProject.getName(), aProject.getId());
            }
        }
    }
}
