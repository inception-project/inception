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
package de.tudarmstadt.ukp.inception.project.export.task.curated;

import static de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest.FORMAT_AUTO;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.lang.invoke.MethodHandles.lookup;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.export.task.ProjectExportTask_ImplBase;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.io.ZipUtils;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class CuratedDocumentsProjectExportTask
    extends ProjectExportTask_ImplBase<CuratedDocumentsProjectExportRequest>
{
    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final String CURATION_AS_SERIALISED_CAS = "/curation_ser/";
    private static final String CURATION_FOLDER = "/curation/";

    private @Autowired DocumentService documentService;
    private @Autowired DocumentImportExportService importExportService;

    public CuratedDocumentsProjectExportTask(CuratedDocumentsProjectExportRequest aRequest,
            String aUsername)
    {
        super(aRequest.getProject(), aRequest, aUsername);
    }

    @Override
    public File export(CuratedDocumentsProjectExportRequest aRequest,
            ProjectExportTaskMonitor aMonitor)
        throws ProjectExportException
    {
        var project = aRequest.getProject();
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

            exportCuratedDocuments(aRequest, exportTempDir, false, aMonitor);

            exportFile = new File(exportTempDir.getAbsolutePath() + "_curated_documents.zip");
            ZipUtils.zipFolder(exportTempDir, exportFile);
        }
        catch (Exception e) {
            if (exportFile != null) {
                try {
                    forceDelete(exportTempDir);
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
                    forceDelete(exportTempDir);
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
     * @throws InterruptedException
     *             When another thread wants to interrupt the export process
     */
    private void exportCuratedDocuments(CuratedDocumentsProjectExportRequest aModel, File aCopyDir,
            boolean aIncludeInProgress, ProjectExportTaskMonitor aMonitor)
        throws ProjectExportException, IOException, InterruptedException
    {
        var project = aModel.getProject();

        // Get all the source documents from the project
        var documents = documentService.listSourceDocuments(project);

        // Determine which format to use for export.
        FormatSupport format;
        if (FORMAT_AUTO.equals(aModel.getFormat())) {
            format = importExportService.getFallbackFormat();
        }
        else {
            format = importExportService.getWritableFormatById(aModel.getFormat()).orElseGet(() -> {
                FormatSupport formatSupport = importExportService.getFallbackFormat();
                LOG.info("Format [{}] is not writable - exporting as [{}] instead.",
                        aModel.getFormat(), formatSupport.getName());
                return formatSupport;
            });
        }

        int initProgress = aMonitor.getProgress() - 1;
        int i = 1;
        for (var sourceDocument : documents) {
            // check if the export has been cancelled
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            try (var session = CasStorageSession.openNested()) {
                var curationCasDir = new File(
                        aCopyDir + CURATION_AS_SERIALISED_CAS + sourceDocument.getName());
                FileUtils.forceMkdir(curationCasDir);

                var curationDir = new File(aCopyDir + CURATION_FOLDER + sourceDocument.getName());
                FileUtils.forceMkdir(curationDir);

                // If depending on a InProgress, include only the the curation documents that are
                // finished or also the ones that are in progress
                if ((aIncludeInProgress && SourceDocumentState.CURATION_IN_PROGRESS
                        .equals(sourceDocument.getState()))
                        || SourceDocumentState.CURATION_FINISHED
                                .equals(sourceDocument.getState())) {
                    if (documentService.existsCas(sourceDocument, CURATION_SET)) {
                        // Copy CAS - this is used when importing the project again
                        try (var os = new FileOutputStream(
                                new File(curationDir, CURATION_USER + ".ser"))) {
                            documentService.exportCas(sourceDocument, CURATION_SET, os);
                        }

                        // Copy secondary export format for convenience - not used during import
                        try {
                            var curationFile = importExportService.exportAnnotationDocument(
                                    sourceDocument, WebAnnoConst.CURATION_USER, format,
                                    WebAnnoConst.CURATION_USER, Mode.CURATION);
                            FileUtils.copyFileToDirectory(curationFile, curationDir);
                            forceDelete(curationFile);
                        }
                        catch (Exception e) {
                            // error("Unexpected error while exporting project: " +
                            // ExceptionUtils.getRootCauseMessage(e) );
                            throw new ProjectExportException(
                                    "Aborting due to unrecoverable error while exporting!");
                        }
                    }
                }
            }
            aMonitor.setProgress(
                    initProgress + (int) Math.ceil(((double) i) / documents.size() * 10.0));
            i++;
        }
    }
}
