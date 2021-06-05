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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.export;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest.FORMAT_AUTO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebAnnoTsv3FormatSupport;

public class ProjectExportCuratedDocumentsTask
    extends ProjectExportTask_ImplBase
{
    private static final String CURATION_AS_SERIALISED_CAS = "/curation_ser/";
    private static final String CURATION_FOLDER = "/curation/";

    private @Autowired ProjectExportService exportService;
    private @Autowired DocumentService documentService;
    private @Autowired DocumentImportExportService importExportService;

    public ProjectExportCuratedDocumentsTask(ProjectExportTaskHandle aHandle,
            ProjectExportTaskMonitor aMonitor, ProjectExportRequest aRequest, String aUsername)
    {
        super(aHandle, aMonitor, aRequest, aUsername);
    }

    @Override
    public File export(ProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor)
        throws ProjectExportException
    {
        Project project = aRequest.getProject();
        File exportFile = null;
        File exportTempDir = null;
        try {
            exportTempDir = File.createTempFile("webanno", "export");
            exportTempDir.delete();
            exportTempDir.mkdirs();

            boolean curationDocumentExist = documentService.existsCurationDocument(project);

            if (!curationDocumentExist) {
                throw new ProjectExportException(
                        "No curation document created yet for this document");
            }

            ProjectExportRequest request = aRequest;
            request.setProject(project);
            exportCuratedDocuments(request, exportTempDir, false, aMonitor);

            exportFile = new File(exportTempDir.getAbsolutePath() + "_curated_documents.zip");
            ZipUtils.zipFolder(exportTempDir, exportFile);
        }
        catch (Exception e) {
            if (exportFile != null) {
                try {
                    FileUtils.forceDelete(exportTempDir);
                }
                catch (IOException ex) {
                    aMonitor.addMessage(LogMessage.error(this,
                            "Unable to export file after export failed: %s", ex.getMessage()));
                }
            }
            throw new ProjectExportException(e);
        }
        finally {
            if (exportTempDir != null) {
                try {
                    FileUtils.forceDelete(exportTempDir);
                }
                catch (IOException e) {
                    aMonitor.addMessage(LogMessage.error(this, "Unable to delete temp file: %s",
                            e.getMessage()));
                }
            }
        }

        return exportFile;
    }

    /**
     * Copy, if exists, curation documents to a folder that will be exported as Zip file
     * 
     * @param aCopyDir
     *            The folder where curated documents are copied to be exported as Zip File
     */
    private void exportCuratedDocuments(ProjectExportRequest aModel, File aCopyDir,
            boolean aIncludeInProgress, ProjectExportTaskMonitor aMonitor)
        throws ProjectExportException, IOException
    {
        Project project = aModel.getProject();

        // Get all the source documents from the project
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = documentService
                .listSourceDocuments(project);

        // Determine which format to use for export.
        FormatSupport format;
        if (FORMAT_AUTO.equals(aModel.getFormat())) {
            format = new WebAnnoTsv3FormatSupport();
        }
        else {
            format = importExportService.getWritableFormatById(aModel.getFormat()).orElseGet(() -> {
                // LOG.info(
                // "Format [{}] is not writable - exporting as WebAnno TSV3 instead.",
                // aModel.getFormat());
                return new WebAnnoTsv3FormatSupport();
            });
        }

        int initProgress = aMonitor.getProgress() - 1;
        int i = 1;
        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
            File curationCasDir = new File(
                    aCopyDir + CURATION_AS_SERIALISED_CAS + sourceDocument.getName());
            FileUtils.forceMkdir(curationCasDir);

            File curationDir = new File(aCopyDir + CURATION_FOLDER + sourceDocument.getName());
            FileUtils.forceMkdir(curationDir);

            // If depending on aInProgress, include only the the curation documents that are
            // finished or also the ones that are in progress
            if ((aIncludeInProgress
                    && SourceDocumentState.CURATION_IN_PROGRESS.equals(sourceDocument.getState()))
                    || SourceDocumentState.CURATION_FINISHED.equals(sourceDocument.getState())) {
                if (documentService.existsCas(sourceDocument, CURATION_USER)) {
                    // Copy CAS - this is used when importing the project again
                    try (OutputStream os = new FileOutputStream(
                            new File(curationDir, CURATION_USER + ".ser"))) {
                        documentService.exportCas(sourceDocument, CURATION_USER, os);
                    }

                    // Copy secondary export format for convenience - not used during import
                    try {
                        File curationFile = importExportService.exportAnnotationDocument(
                                sourceDocument, WebAnnoConst.CURATION_USER, format,
                                WebAnnoConst.CURATION_USER, Mode.CURATION);
                        FileUtils.copyFileToDirectory(curationFile, curationDir);
                        FileUtils.forceDelete(curationFile);
                    }
                    catch (Exception e) {
                        // error("Unexpected error while exporting project: " +
                        // ExceptionUtils.getRootCauseMessage(e) );
                        throw new ProjectExportException(
                                "Aborting due to unrecoverable error while exporting!");
                    }
                }
            }

            aMonitor.setProgress(
                    initProgress + (int) Math.ceil(((double) i) / documents.size() * 10.0));
            i++;
        }
    }
}
