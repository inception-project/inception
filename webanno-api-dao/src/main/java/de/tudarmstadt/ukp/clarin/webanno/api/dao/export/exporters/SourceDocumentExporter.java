/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

@Component
public class SourceDocumentExporter
    implements ProjectExporter
{
    private static final String SOURCE = "source";
    private static final String SOURCE_FOLDER = "/" + SOURCE;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired DocumentService documentService;

    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aStage)
        throws IOException, ProjectExportException
    {
        exportSourceDocuments(aRequest.getProject(), aExProject);
        exportSourceDocumentContents(aRequest, aExProject, aStage);
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
            exDocument.setSentenceAccessed(sourceDocument.getSentenceAccessed());
            exDocument.setCreated(sourceDocument.getCreated());
            exDocument.setUpdated(sourceDocument.getUpdated());

            sourceDocuments.add(exDocument);
        }

        exProject.setSourceDocuments(sourceDocuments);
    }

    private void exportSourceDocumentContents(ProjectExportRequest aRequest,
            ExportedProject aExProject, File aStage)
        throws IOException, ProjectExportException
    {
        Project project = aRequest.getProject();
        File sourceDocumentDir = new File(aStage + SOURCE_FOLDER);
        FileUtils.forceMkdir(sourceDocumentDir);
        // Get all the source documents from the project
        List<SourceDocument> documents = documentService.listSourceDocuments(project);
        int i = 1;
        for (SourceDocument sourceDocument : documents) {
            try {
                FileUtils.copyFileToDirectory(documentService.getSourceDocumentFile(sourceDocument),
                        sourceDocumentDir);
                aRequest.progress = (int) Math.ceil(((double) i) / documents.size() * 10.0);
                i++;
                log.info("Exported content for source document [" + sourceDocument.getId()
                        + "] in project [" + project.getName() + "] with id [" + project.getId()
                        + "]");
            }
            catch (FileNotFoundException e) {
                // error(e.getMessage());
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Source file '");
                errorMessage.append(sourceDocument.getName());
                errorMessage.append("' related to project couldn't be located in repository");
                log.error(errorMessage.toString(), ExceptionUtils.getRootCause(e));
                aRequest.addMessage(errorMessage.toString());
                throw new ProjectExportException(
                        "Couldn't find some source file(s) related to project");
                // continue;

            }
        }
    }
    
    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        importSourceDocuments(aExProject, aProject);
        importSourceDocumentContents(aZip, aProject);
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
            sourceDocument.setSentenceAccessed(importedSourceDocument.getSentenceAccessed());
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
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ProjectExporter.normalizeEntryName(entry);

            if (entryName.startsWith(SOURCE)) {
                String fileName = FilenameUtils.getName(entryName);
                if (fileName.trim().isEmpty()) {
                    continue;
                }
                SourceDocument sourceDocument = documentService.getSourceDocument(aProject,
                        fileName);
                File sourceFilePath = documentService.getSourceDocumentFile(sourceDocument);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), sourceFilePath);

                log.info("Imported content for source document [" + sourceDocument.getId()
                        + "] in project [" + aProject.getName() + "] with id [" + aProject.getId()
                        + "]");
            }
        }
    }
}
