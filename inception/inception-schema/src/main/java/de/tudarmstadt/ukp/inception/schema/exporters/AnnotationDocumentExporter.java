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
package de.tudarmstadt.ukp.inception.schema.exporters;

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.ANNOTATION_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.DOCUMENT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest.FORMAT_AUTO;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.io.FastIOUtils.copy;
import static java.lang.Math.ceil;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.documents.exporters.SourceDocumentExporter;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationSchemaServiceAutoConfiguration#annotationDocumentExporter}.
 * </p>
 */
public class AnnotationDocumentExporter
    implements ProjectExporter
{
    private static final String ANNOTATION_ORIGINAL_FOLDER = "/annotation/";
    private static final String ANNOTATION_AS_SERIALISED_CAS = "annotation_ser";
    private static final String ANNOTATION_CAS_FOLDER = "/" + ANNOTATION_AS_SERIALISED_CAS + "/";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DocumentService documentService;
    private final UserDao userRepository;
    private final DocumentImportExportService importExportService;
    private final RepositoryProperties repositoryProperties;

    @Autowired
    public AnnotationDocumentExporter(DocumentService aDocumentService, UserDao aUserRepository,
            DocumentImportExportService aImportExportService,
            RepositoryProperties aRepositoryProperties)
    {
        documentService = aDocumentService;
        userRepository = aUserRepository;
        importExportService = aImportExportService;
        repositoryProperties = aRepositoryProperties;
    }

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
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, File aStage)
        throws UIMAException, ClassNotFoundException, IOException, InterruptedException
    {
        exportAnnotationDocuments(aMonitor, aRequest.getProject(), aExProject);
        exportAnnotationDocumentContents(aRequest, aMonitor, aExProject, aStage);
    }

    private void exportAnnotationDocuments(ProjectExportTaskMonitor aMonitor, Project aProject,
            ExportedProject aExProject)
    {
        List<ExportedAnnotationDocument> annotationDocuments = new ArrayList<>();

        for (AnnotationDocument annotationDocument : documentService
                .listAnnotationDocuments(aProject)) {
            ExportedAnnotationDocument exAnnotationDocument = new ExportedAnnotationDocument();
            exAnnotationDocument.setName(annotationDocument.getName());
            exAnnotationDocument.setState(annotationDocument.getState());
            exAnnotationDocument.setAnnotatorState(annotationDocument.getAnnotatorState());
            exAnnotationDocument.setAnnotatorComment(annotationDocument.getAnnotatorComment());
            exAnnotationDocument.setUser(annotationDocument.getUser());
            exAnnotationDocument.setTimestamp(annotationDocument.getTimestamp());
            exAnnotationDocument.setSentenceAccessed(annotationDocument.getSentenceAccessed());
            exAnnotationDocument.setCreated(annotationDocument.getCreated());
            exAnnotationDocument.setUpdated(annotationDocument.getUpdated());
            annotationDocuments.add(exAnnotationDocument);
        }

        aExProject.setAnnotationDocuments(annotationDocuments);
    }

    private void exportAnnotationDocumentContents(FullProjectExportRequest aRequest,
            ProjectExportTaskMonitor aMonitor, ExportedProject aExProject, File aStage)
        throws UIMAException, ClassNotFoundException, IOException, InterruptedException
    {
        Project project = aRequest.getProject();

        // The export process may store project-related information in this context to ensure it
        // is looked up only once during the bulk operation and the DB is not hit too often.
        Map<Pair<Project, String>, Object> bulkOperationContext = new HashMap<>();

        List<SourceDocument> documents = documentService.listSourceDocuments(project);
        int i = 1;
        int initProgress = aMonitor.getProgress();

        // Create a map containing the annotation documents for each source document. Doing this
        // as one DB access before the main processing to avoid hammering the DB in the loops
        // below.
        Map<SourceDocument, List<AnnotationDocument>> srcToAnnIdx = documentService
                .listAnnotationDocuments(project).stream()
                .collect(groupingBy(doc -> doc.getDocument(), toList()));

        // Cache user lookups to avoid constantly hitting the database
        LoadingCache<String, User> usersCache = Caffeine.newBuilder()
                .build(key -> userRepository.get(key));

        for (SourceDocument srcDoc : documents) {
            // check if the export has been cancelled
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            try (CasStorageSession session = CasStorageSession.openNested()) {
                //
                // Export initial CASes
                //

                // The initial CAS must always be exported to ensure that the converted source
                // document will *always* have the state it had at the time of the initial import.
                // We we do have a reliably initial CAS and instead lazily convert whenever an
                // annotator starts annotating, then we could end up with two annotators having two
                // different versions of their CAS e.g. if there was a code change in the reader
                // component that affects its output.

                // If the initial CAS does not exist yet, it must be created before export.
                if (!documentService.existsInitialCas(srcDoc)) {
                    documentService.createOrReadInitialCas(srcDoc);
                }

                File targetDir = new File(aStage, ANNOTATION_CAS_FOLDER + srcDoc.getName());
                forceMkdir(targetDir);

                try (OutputStream os = new FileOutputStream(
                        new File(targetDir, INITIAL_CAS_PSEUDO_USER + ".ser"))) {
                    documentService.exportCas(srcDoc, INITIAL_CAS_PSEUDO_USER, os);
                }

                log.info("Exported annotation document content for user [{}] for source document "
                        + "{} in project {}", INITIAL_CAS_PSEUDO_USER, srcDoc, project);

                //
                // Export per-user annotation document
                //

                FormatSupport format = null;
                if (aRequest.getFormat() != null) {
                    // Determine which format to use for export
                    String formatId = FORMAT_AUTO.equals(aRequest.getFormat()) ? srcDoc.getFormat()
                            : aRequest.getFormat();

                    format = importExportService.getWritableFormatById(formatId).orElseGet(() -> {
                        FormatSupport fallbackFormat = importExportService.getFallbackFormat();
                        aMonitor.addMessage(LogMessage.warn(this, "Annotation: [%s] No writer "
                                + "found for format [%s] - falling back to exporting as [%s] "
                                + "instead.", srcDoc.getName(), formatId,
                                fallbackFormat.getName()));
                        return fallbackFormat;
                    });
                }

                // Export annotations from regular users
                for (AnnotationDocument annDoc : srcToAnnIdx.computeIfAbsent(srcDoc,
                        key -> emptyList())) {

                    // copy annotation document only for existing users and the state of the
                    // annotation document is not NEW/IGNORE
                    if (usersCache.get(annDoc.getUser()) != null
                            && documentService.existsCas(annDoc)
                            && !annDoc.getState().equals(AnnotationDocumentState.NEW)
                            && !annDoc.getState().equals(AnnotationDocumentState.IGNORE)) {

                        File annSerDir = new File(aStage.getAbsolutePath() + ANNOTATION_CAS_FOLDER
                                + srcDoc.getName());
                        forceMkdir(annSerDir);
                        try (OutputStream os = new FileOutputStream(
                                new File(annSerDir, annDoc.getUser() + ".ser"))) {
                            documentService.exportCas(srcDoc, annDoc.getUser(), os);
                        }

                        if (format != null) {
                            File annDocDir = new File(aStage.getAbsolutePath()
                                    + ANNOTATION_ORIGINAL_FOLDER + srcDoc.getName());
                            File annFile = null;
                            try {
                                annFile = importExportService.exportAnnotationDocument(srcDoc,
                                        annDoc.getUser(), format, annDoc.getUser(), ANNOTATION,
                                        false, bulkOperationContext);
                                forceMkdir(annDocDir);
                                copyFileToDirectory(annFile, annDocDir);
                            }
                            finally {
                                if (annFile != null) {
                                    forceDelete(annFile);
                                }
                            }
                        }

                        log.info("Exported annotation document content for user [{}] for " //
                                + "source document {} in project {}", annDoc.getUser(), srcDoc,
                                project);
                    }
                }
            }

            aMonitor.setProgress(initProgress + (int) ceil(((double) i) / documents.size() * 80.0));
            i++;
        }
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        long start = currentTimeMillis();

        Map<String, SourceDocument> nameToDoc = documentService.listSourceDocuments(aProject)
                .stream().collect(toMap(SourceDocument::getName, identity()));

        importAnnotationDocuments(aExProject, aProject, nameToDoc);
        importAnnotationDocumentContents(aZip, aProject, nameToDoc);

        log.info("Imported [{}] annotation documents for project [{}] ({})",
                aExProject.getSourceDocuments().size(), aExProject.getName(),
                DurationFormatUtils.formatDurationWords(currentTimeMillis() - start, true, true));
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
    private void importAnnotationDocuments(ExportedProject aExProject, Project aProject,
            Map<String, SourceDocument> aNameToDoc)
        throws IOException
    {
        for (ExportedAnnotationDocument exAnnotationDocument : aExProject
                .getAnnotationDocuments()) {
            AnnotationDocument annotationDocument = new AnnotationDocument();
            annotationDocument.setName(exAnnotationDocument.getName());
            annotationDocument.setState(exAnnotationDocument.getState());
            annotationDocument.setAnnotatorState(exAnnotationDocument.getAnnotatorState());
            annotationDocument.setAnnotatorComment(exAnnotationDocument.getAnnotatorComment());
            annotationDocument.setProject(aProject);
            annotationDocument.setUser(exAnnotationDocument.getUser());
            annotationDocument.setTimestamp(exAnnotationDocument.getTimestamp());
            annotationDocument.setDocument(aNameToDoc.get(exAnnotationDocument.getName()));
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
    private void importAnnotationDocumentContents(ZipFile zip, Project aProject,
            Map<String, SourceDocument> aNameToDoc)
        throws IOException
    {
        int n = 0;

        // NOTE: we resort to internal knowledge about the CasStorageService here, but
        // it makes the import quite a bit faster than using DocumentService.getCasFile(...)
        Path docRoot = repositoryProperties.getPath().toPath().resolve(PROJECT_FOLDER)
                .resolve(aProject.getId().toString()).resolve(DOCUMENT_FOLDER);

        Set<SourceDocument> annotationFolderInitialized = new HashSet<>();

        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ProjectExporter.normalizeEntryName(entry);

            if (!entryName.startsWith(ANNOTATION_AS_SERIALISED_CAS + "/")
                    || !entryName.endsWith(".ser")) {
                continue;
            }

            String fileName = entryName.replace(ANNOTATION_AS_SERIALISED_CAS + "/", "");

            if (fileName.trim().isEmpty()) {
                continue;
            }

            // the user annotated the document is file name minus extension (anno1.ser)
            String username = FilenameUtils.getBaseName(fileName).replace(".ser", "");

            // name of the annotation document
            fileName = fileName.replace(FilenameUtils.getName(fileName), "").replace("/", "");
            SourceDocument sourceDocument = aNameToDoc.get(fileName);

            Path annFolder = docRoot.resolve(sourceDocument.getId().toString())
                    .resolve(ANNOTATION_FOLDER);

            // Check if the annotation folder for the given source document has already been
            // created. Using the set to check here is faster than querying the file system.
            if (!annotationFolderInitialized.contains(sourceDocument)) {
                Files.createDirectories(annFolder);
                annotationFolderInitialized.add(sourceDocument);
            }

            copy(zip.getInputStream(entry), annFolder.resolve(username + ".ser").toFile());

            n++;
            log.info(
                    "Imported content for annotation document {}: user [{}] for [{}]({}) in project [{}]({})",
                    n, username, sourceDocument.getName(), sourceDocument.getId(),
                    aProject.getName(), aProject.getId());
        }
    }
}
