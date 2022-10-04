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
package de.tudarmstadt.ukp.inception.curation.export;

import static de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest.FORMAT_AUTO;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static java.lang.Math.ceil;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FilenameUtils.getExtension;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.exporters.SourceDocumentExporter;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationServiceAutoConfiguration#curatedDocumentsExporter}.
 * </p>
 */
public class CuratedDocumentsExporter
    implements ProjectExporter
{
    private static final String CURATION_FOLDER = "/curation/";
    private static final String CURATION_AS_SERIALISED_CAS = "curation_ser";
    private static final String CURATION_CAS_FOLDER = "/" + CURATION_AS_SERIALISED_CAS + "/";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DocumentService documentService;
    private final DocumentImportExportService importExportService;

    @Autowired
    public CuratedDocumentsExporter(DocumentService aDocumentService,
            DocumentImportExportService aImportExportService)
    {
        documentService = aDocumentService;
        importExportService = aImportExportService;
    }

    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies()
    {
        return asList(SourceDocumentExporter.class);
    }

    /**
     * Copy, if exists, curation documents to a folder that will be exported as Zip file
     * 
     * @param aStage
     *            The folder where curated documents are copied to be exported as Zip File
     */
    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, File aStage)
        throws Exception
    {
        Project project = aRequest.getProject();

        // The export process may store project-related information in this context to ensure it
        // is looked up only once during the bulk operation and the DB is not hit too often.
        Map<Pair<Project, String>, Object> bulkOperationContext = new HashMap<>();

        // Get all the source documents from the project
        List<SourceDocument> documents = documentService.listSourceDocuments(project);

        int initProgress = aMonitor.getProgress() - 1;
        int i = 1;
        for (SourceDocument sourceDocument : documents) {
            try (CasStorageSession session = CasStorageSession.openNested()) {
                File curationCasDir = new File(aStage,
                        CURATION_CAS_FOLDER + sourceDocument.getName());
                forceMkdir(curationCasDir);

                File curationDir = new File(aStage, CURATION_FOLDER + sourceDocument.getName());
                forceMkdir(curationDir);

                // If depending on aInProgress, include only the the curation documents that are
                // finished or also the ones that are in progress
                if (documentService.existsCas(sourceDocument, CURATION_USER)
                        && (aRequest.isIncludeInProgress()
                                && CURATION_IN_PROGRESS.equals(sourceDocument.getState()))
                        || CURATION_FINISHED.equals(sourceDocument.getState())) {
                    // Copy CAS - this is used when importing the project again
                    exportSerializedCas(sourceDocument, curationCasDir);

                    // Determine which format to use for export
                    if (aRequest.getFormat() != null) {
                        String formatId = FORMAT_AUTO.equals(aRequest.getFormat())
                                ? sourceDocument.getFormat()
                                : aRequest.getFormat();

                        FormatSupport format = importExportService.getWritableFormatById(formatId)
                                .orElseGet(() -> {
                                    var fallbackFormat = importExportService.getFallbackFormat();
                                    aMonitor.addMessage(LogMessage.warn(this,
                                            "Curation: %s No writer found for original format [%s] "
                                                    + "- exporting as [%s] instead.",
                                            sourceDocument, formatId, fallbackFormat.getName()));
                                    return fallbackFormat;
                                });

                        // Copy secondary export format for convenience - not used during import
                        exportAdditionalFormat(bulkOperationContext, sourceDocument, curationDir,
                                format);
                    }
                }
            }
            aMonitor.setProgress(initProgress + (int) ceil(((double) i) / documents.size() * 10.0));
            i++;
        }
    }

    private void exportAdditionalFormat(Map<Pair<Project, String>, Object> bulkOperationContext,
            SourceDocument srcDoc, File curationDir, FormatSupport format)
        throws ProjectExportException, IOException, ClassNotFoundException, UIMAException
    {
        File curationFile = null;
        try {
            curationFile = importExportService.exportAnnotationDocument(srcDoc, CURATION_USER,
                    format, CURATION_USER, CURATION, true, bulkOperationContext);
            var filename = CURATION_USER + "." + getExtension(curationFile.getName());
            FileUtils.copyFile(curationFile, new File(curationDir, filename));
        }
        finally {
            if (curationFile != null) {
                forceDelete(curationFile);
            }
        }
    }

    private void exportSerializedCas(SourceDocument sourceDocument, File curationCasDir)
        throws IOException, FileNotFoundException
    {
        try (OutputStream os = new FileOutputStream(
                new File(curationCasDir, CURATION_USER + ".ser"))) {
            documentService.exportCas(sourceDocument, CURATION_USER, os);
        }
    }

    /**
     * Copy curation documents from the exported project
     * 
     * @param aZip
     *            the ZIP file.
     * @param aProject
     *            the project.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        for (Enumeration<? extends ZipEntry> zipEnumerate = aZip.entries(); zipEnumerate
                .hasMoreElements();) {
            ZipEntry entry = zipEnumerate.nextElement();
            if (entry.isDirectory()) {
                log.trace("Skipping ZIP entry that is a directory: [{}]", entry.getName());
                continue;
            }

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ProjectExporter.normalizeEntryName(entry);

            if (!entryName.startsWith(CURATION_AS_SERIALISED_CAS + "/")) {
                log.trace("Skipping ZIP entry that is not a curation CAS: [{}]", entry.getName());
                continue;
            }

            log.trace("Importing curation CAS from: [{}]", entry.getName());

            String fileName = entryName.replace(CURATION_AS_SERIALISED_CAS + "/", "");
            // the user annotated the document is file name minus extension
            // (anno1.ser)
            String username = FilenameUtils.getBaseName(fileName).replace(".ser", "");

            // COMPATIBILITY NOTE: One might ask oneself why we extract the filename when it should
            // always be CURATION_USER. The reason is compatibility:
            // - Util WebAnno 3.4.x, the CORRECTION_USER CAS was exported to 'curation' and
            // 'curation_ser'. So for projects exported from this version, the
            // CuratedDocumentsExporter takes care of importing the CORRECTION_USER CASes.
            // - Since WebAnno 3.5.x, the CORRECTION_USER CAS is exported to 'annotation' and
            // 'annotation_ser'. So for projects exported from this version, the
            // AnnotationDocumentExporter takes care of importing the CORRECTION_USER CASes!

            // name of the annotation document
            fileName = fileName.replace(FilenameUtils.getName(fileName), "").replace("/", "");
            if (fileName.trim().isEmpty()) {
                continue;
            }
            SourceDocument sourceDocument = documentService.getSourceDocument(aProject, fileName);

            try (InputStream is = aZip.getInputStream(entry)) {
                documentService.importCas(sourceDocument, username, is);
            }

            log.info("Imported curation document content for user [" + username
                    + "] for source document [" + sourceDocument.getId() + "] in project ["
                    + aProject.getName() + "] with id [" + aProject.getId() + "]");
        }
    }
}
