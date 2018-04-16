/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.export;

import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest.FORMAT_AUTO;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv3XWriter;

public class ExportUtil
{
    private static final Logger LOG = LoggerFactory.getLogger(ExportUtil.class);
    
    private static final String ANNOTATION_ORIGINAL_FOLDER = "/annotation/";
    private static final String CONSTRAINTS = "/constraints/";
    private static final String LOG_FOLDER = "/" + ProjectService.LOG_FOLDER;
    private static final String GUIDELINES_FOLDER = "/" + ImportUtil.GUIDELINE;
    private static final String ANNOTATION_CAS_FOLDER = "/"
            + ImportUtil.ANNOTATION_AS_SERIALISED_CAS + "/";
    private static final String META_INF = "/" + ImportUtil.META_INF;
    private static final String SOURCE_FOLDER = "/" + ImportUtil.SOURCE;
    private static final String TRAIN_FOLDER = "/" + ImportUtil.TRAIN;
    private static final String CORRECTION_USER = "CORRECTION_USER";
    private static final String CURATION_AS_SERIALISED_CAS = "/"
            + ImportUtil.CURATION_AS_SERIALISED_CAS + "/";
    private static final String CURATION_FOLDER = "/curation/";

    /**
     * Copy, if exists, curation documents to a folder that will be exported as Zip file
     * 
     * @param aCopyDir
     *            The folder where curated documents are copied to be exported as Zip File
     */
    @Deprecated
    public static void exportCuratedDocuments(DocumentService documentService,
            ImportExportService importExportService, ProjectExportRequest aModel, File aCopyDir,
            boolean aIncludeInProgress)
        throws UIMAException, IOException, ClassNotFoundException,
        ProjectExportException
    {
        Project project = aModel.getProject();
        
        // Get all the source documents from the project
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = documentService
                .listSourceDocuments(project);

        // Determine which format to use for export.
        Class<?> writer;
        if (FORMAT_AUTO.equals(aModel.getFormat())) {
            writer = WebannoTsv3XWriter.class;
        }
        else {
            writer = importExportService.getWritableFormats().get(
                    importExportService.getWritableFormatId(aModel.getFormat()));
            if (writer == null) {
                writer = WebannoTsv3XWriter.class;
            }
        }
        
        int initProgress = aModel.progress - 1;
        int i = 1;
        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
            File curationCasDir = new File(aCopyDir + CURATION_AS_SERIALISED_CAS
                    + sourceDocument.getName());
            FileUtils.forceMkdir(curationCasDir);

            File curationDir = new File(aCopyDir + CURATION_FOLDER + sourceDocument.getName());
            FileUtils.forceMkdir(curationDir);

            // If depending on aInProgress, include only the the curation documents that are
            // finished or also the ones that are in progress
            if (
                (aIncludeInProgress && 
                    SourceDocumentState.CURATION_IN_PROGRESS.equals(sourceDocument.getState())) ||
                SourceDocumentState.CURATION_FINISHED.equals(sourceDocument.getState())
            ) {
                File curationCasFile = documentService.getCasFile(sourceDocument,
                        WebAnnoConst.CURATION_USER);
                if (curationCasFile.exists()) {
                    // Copy CAS - this is used when importing the project again
                    FileUtils.copyFileToDirectory(curationCasFile, curationCasDir);

                    // Copy secondary export format for convenience - not used during import
                    try {
                        File curationFile = importExportService.exportAnnotationDocument(
                                sourceDocument, WebAnnoConst.CURATION_USER, writer,
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

            aModel.progress = initProgress
                    + (int) Math.ceil(((double) i) / documents.size() * 10.0);
            i++;
        }
    }
}
