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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest.FORMAT_AUTO;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UIMAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebAnnoTsv3FormatSupport;

@Component
public class AnnotationDocumentExporter
    implements ProjectExporter
{
    private static final String ANNOTATION_ORIGINAL_FOLDER = "/annotation/";
    private static final String ANNOTATION_AS_SERIALISED_CAS = "annotation_ser";
    private static final String ANNOTATION_CAS_FOLDER = "/" + ANNOTATION_AS_SERIALISED_CAS + "/";
    
    private static final String CORRECTION_USER = "CORRECTION_USER";
    private static final String CURATION_AS_SERIALISED_CAS = "/annotation_ser/";
    private static final String CURATION_FOLDER = "/curation/";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private @Autowired DocumentService documentService;
    private @Autowired UserDao userRepository;
    private @Autowired ImportExportService importExportService;
    
    @Override
    public List<Class<? extends ProjectExporter>> getExportDependencies()
    {
        return asList(SourceDocumentExporter.class);
    }
    
    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies()
    {
        return asList(SourceDocumentExporter.class);
    }
    
    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aStage)
        throws UIMAException, ClassNotFoundException, IOException
    {
        exportAnnotationDocuments(aRequest.getProject(), aExProject);
        exportAnnotationDocumentContents(aRequest, aExProject, aStage);
    }
    
    private void exportAnnotationDocuments(Project aProject, ExportedProject aExProject)
    {
        List<ExportedAnnotationDocument> annotationDocuments = new ArrayList<>();

        // add source documents to a project
        List<SourceDocument> documents = documentService.listSourceDocuments(aProject);
        for (SourceDocument sourceDocument : documents) {
            // add annotation document to Project
            for (AnnotationDocument annotationDocument : documentService
                    .listAnnotationDocuments(sourceDocument)) {
                ExportedAnnotationDocument exAnnotationDocument = new ExportedAnnotationDocument();
                exAnnotationDocument.setName(annotationDocument.getName());
                exAnnotationDocument.setState(annotationDocument.getState());
                exAnnotationDocument.setUser(annotationDocument.getUser());
                exAnnotationDocument.setTimestamp(annotationDocument.getTimestamp());
                exAnnotationDocument.setSentenceAccessed(annotationDocument.getSentenceAccessed());
                exAnnotationDocument.setCreated(annotationDocument.getCreated());
                exAnnotationDocument.setUpdated(annotationDocument.getUpdated());
                annotationDocuments.add(exAnnotationDocument);
            }
        }

        aExProject.setAnnotationDocuments(annotationDocuments);
    }

    private void exportAnnotationDocumentContents(ProjectExportRequest aRequest,
            ExportedProject aExProject, File aStage)
        throws UIMAException, ClassNotFoundException, IOException
    {
        Project project = aRequest.getProject();
        
        List<SourceDocument> documents = documentService.listSourceDocuments(project);
        int i = 1;
        int initProgress = aRequest.progress;
        for (SourceDocument sourceDocument : documents) {
            //
            // Export initial CASes
            //
            
            // The initial CAS must always be exported to ensure that the converted source document
            // will *always* have the state it had at the time of the initial import. We we do have
            // a reliably initial CAS and instead lazily convert whenever an annotator starts
            // annotating, then we could end up with two annotators having two different versions of
            // their CAS e.g. if there was a code change in the reader component that affects its
            // output.

            // If the initial CAS does not exist yet, it must be created before export.
            documentService.createOrReadInitialCas(sourceDocument);
            
            File targetDir = new File(aStage, ANNOTATION_CAS_FOLDER + sourceDocument.getName());
            FileUtils.forceMkdir(targetDir);
            
            File initialCasFile = documentService.getCasFile(sourceDocument,
                    INITIAL_CAS_PSEUDO_USER);
            
            FileUtils.copyFileToDirectory(initialCasFile, targetDir);
            
            log.info("Exported annotation document content for user [" + INITIAL_CAS_PSEUDO_USER
                    + "] for source document [" + sourceDocument.getId() + "] in project ["
                    + project.getName() + "] with id [" + project.getId() + "]");

            //
            // Export per-user annotation document
            // 
            
            // Determine which format to use for export
            String formatId = FORMAT_AUTO.equals(aRequest.getFormat()) ? sourceDocument.getFormat()
                    : aRequest.getFormat();
            
            FormatSupport format = importExportService.getWritableFormatById(formatId)
                    .orElseGet(() -> {
                        aRequest.addMessage(LogMessage.error(this,"[%s] No writer found for "
                                + "format [%s] - exporting as WebAnno TSV instead.",
                                sourceDocument.getName(), aRequest.getFormat()));
                        return new WebAnnoTsv3FormatSupport();
                    });

            // Export annotations from regular users
            for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : 
                    documentService.listAnnotationDocuments(sourceDocument)) {
                // copy annotation document only for ACTIVE users and the state of the 
                // annotation document is not NEW/IGNORE
                if (
                        userRepository.get(annotationDocument.getUser()) != null && 
                        !annotationDocument.getState().equals(AnnotationDocumentState.NEW) && 
                        !annotationDocument.getState().equals(AnnotationDocumentState.IGNORE)
                ) {
                    File annotationDocumentAsSerialisedCasDir = new File(
                            aStage.getAbsolutePath() + ANNOTATION_CAS_FOLDER
                                    + sourceDocument.getName());
                    File annotationDocumentDir = new File(aStage.getAbsolutePath()
                            + ANNOTATION_ORIGINAL_FOLDER + sourceDocument.getName());

                    FileUtils.forceMkdir(annotationDocumentAsSerialisedCasDir);
                    FileUtils.forceMkdir(annotationDocumentDir);

                    File annotationFileAsSerialisedCas = documentService.getCasFile(
                            sourceDocument, annotationDocument.getUser());

                    File annotationFile = null;
                    if (annotationFileAsSerialisedCas.exists()) {
                        annotationFile = importExportService.exportAnnotationDocument(
                                sourceDocument, annotationDocument.getUser(), format,
                                annotationDocument.getUser(), Mode.ANNOTATION, false);
                    }
                    
                    if (annotationFileAsSerialisedCas.exists()) {
                        FileUtils.copyFileToDirectory(annotationFileAsSerialisedCas,
                                annotationDocumentAsSerialisedCasDir);
                        FileUtils.copyFileToDirectory(annotationFile, annotationDocumentDir);
                        FileUtils.forceDelete(annotationFile);
                    }
                    
                    log.info("Exported annotation document content for user ["
                            + annotationDocument.getUser() + "] for source document ["
                            + sourceDocument.getId() + "] in project [" + project.getName()
                            + "] with id [" + project.getId() + "]");
                }
            }
            
            // BEGIN FIXME #1224 CURATION_USER and CORRECTION_USER files should be exported in
            // annotation_ser
            // If this project is a correction project, add the auto-annotated CAS to same
            // folder as CURATION_FOLDER
            if (WebAnnoConst.PROJECT_TYPE_AUTOMATION.equals(project.getMode())
                    || WebAnnoConst.PROJECT_TYPE_CORRECTION.equals(project.getMode())) {
                File correctionCasFile = documentService.getCasFile(sourceDocument,
                        CORRECTION_USER);
                if (correctionCasFile.exists()) {
                    // Copy CAS - this is used when importing the project again
                    File curationCasDir = new File(aStage + CURATION_AS_SERIALISED_CAS
                            + sourceDocument.getName());
                    FileUtils.forceMkdir(curationCasDir);
                    FileUtils.copyFileToDirectory(correctionCasFile, curationCasDir);
                    
                    // Copy secondary export format for convenience - not used during import
                    File curationDir = new File(
                            aStage + CURATION_FOLDER + sourceDocument.getName());
                    FileUtils.forceMkdir(curationDir);
                    File correctionFile = importExportService.exportAnnotationDocument(
                            sourceDocument, CORRECTION_USER, format, CORRECTION_USER,
                            Mode.CORRECTION);
                    FileUtils.copyFileToDirectory(correctionFile, curationDir);
                    FileUtils.forceDelete(correctionFile);
                }
            }
            // END FIXME #1224 CURATION_USER and CORRECTION_USER files should be exported in
            // annotation_ser
            
            aRequest.progress = initProgress
                    + (int) Math.ceil(((double) i) / documents.size() * 80.0);
            i++;
        }
    }
    
    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        importAnnotationDocuments(aExProject, aProject);
        importAnnotationDocumentContents(aZip, aProject);
    }
    
    /**
     * Create {@link de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument} from exported
     * {@link ExportedAnnotationDocument}
     * 
     * @param aExProject
     *            the imported project.
     * @param aProject
     *            the project.
     * @throws IOException
     *             if an I/O error occurs.
     */
    private void importAnnotationDocuments(ExportedProject aExProject, Project aProject)
        throws IOException
    {
        for (ExportedAnnotationDocument exAnnotationDocument : aExProject
                .getAnnotationDocuments()) {
            AnnotationDocument annotationDocument = new AnnotationDocument();
            annotationDocument.setName(exAnnotationDocument.getName());
            annotationDocument.setState(exAnnotationDocument.getState());
            annotationDocument.setProject(aProject);
            annotationDocument.setUser(exAnnotationDocument.getUser());
            annotationDocument.setTimestamp(exAnnotationDocument.getTimestamp());
            annotationDocument.setDocument(documentService.getSourceDocument(aProject,
                    exAnnotationDocument.getName()));
            annotationDocument.setSentenceAccessed(exAnnotationDocument.getSentenceAccessed());
            annotationDocument.setCreated(exAnnotationDocument.getCreated());
            annotationDocument.setUpdated(exAnnotationDocument.getUpdated());
            documentService.createAnnotationDocument(annotationDocument);
        }
    }
    
    /**
     * copy annotation documents (serialized CASs) from the exported project
     * 
     * @param zip
     *            the ZIP file.
     * @param aProject
     *            the project.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @SuppressWarnings("rawtypes")
    private void importAnnotationDocumentContents(ZipFile zip, Project aProject) throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ProjectExporter.normalizeEntryName(entry);

            if (entryName.startsWith(ANNOTATION_AS_SERIALISED_CAS + "/")) {
                String fileName = entryName.replace(ANNOTATION_AS_SERIALISED_CAS + "/", "");

                if (fileName.trim().isEmpty()) {
                    continue;
                }

                // the user annotated the document is file name minus extension (anno1.ser)
                String username = FilenameUtils.getBaseName(fileName).replace(".ser", "");

                // name of the annotation document
                fileName = fileName.replace(FilenameUtils.getName(fileName), "").replace("/", "");
                SourceDocument sourceDocument = documentService.getSourceDocument(aProject,
                        fileName);
                File annotationFilePath = documentService.getCasFile(sourceDocument, username);

                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), annotationFilePath);

                log.info("Imported annotation document content for user [" + username
                        + "] for source document [" + sourceDocument.getId() + "] in project ["
                        + aProject.getName() + "] with id [" + aProject.getId() + "]");
            }
        }
    }
}
