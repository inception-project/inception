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
package de.tudarmstadt.ukp.inception.remoteapi;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RMessageLevel.ERROR;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.forceOverwriteSofa;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectSentences;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectTokens;
import static java.io.File.createTempFile;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartFile;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.AccessForbiddenException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.IllegalNameException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.IncompatibleDocumentException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.ObjectNotFoundException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.RemoteApiException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.UnsupportedFormatException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.persistence.NoResultException;

public abstract class Controller_ImplBase
{
    public static final String API_BASE = "/api/aero/v1";

    public static final String PROJECTS = "projects";
    public static final String DOCUMENTS = "documents";
    public static final String ANNOTATIONS = "annotations";
    public static final String CURATION = "curation";
    public static final String IMPORT = "import";
    public static final String EXPORT = "export.zip";
    public static final String STATE = "state";
    public static final String PERMISSIONS = "permissions";
    public static final String TASKS = "tasks";
    public static final String USERS = "users";
    public static final String KNOWLEDGE_BASES = "kbs";

    public static final String PARAM_FILE = "file";
    public static final String PARAM_CONTENT = "content";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_TITLE = "title";
    public static final String PARAM_FORMAT = "format";
    public static final String PARAM_STATE = "state";
    public static final String PARAM_CREATOR = "creator";
    public static final String PARAM_PROJECT_ID = "projectId";
    public static final String PARAM_ANNOTATOR_ID = "userId";
    public static final String PARAM_DOCUMENT_ID = "documentId";
    public static final String PARAM_CREATE_MISSING_USERS = "createMissingUsers";
    public static final String PARAM_IMPORT_PERMISSIONS = "importPermissions";
    public static final String PARAM_ROLES = "roles";
    public static final String PARAM_TASK_ID = "taskId";
    public static final String PARAM_KNOWLEDGE_BASE_ID = "kbId";

    public static final String VAL_ORIGINAL = "ORIGINAL";

    public static final String FORMAT_DEFAULT = "text";

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected @Autowired DocumentService documentService;
    protected @Autowired ProjectService projectService;
    protected @Autowired UserDao userRepository;
    protected @Autowired DocumentImportExportService importExportService;

    @ExceptionHandler(value = RemoteApiException.class)
    public ResponseEntity<RResponse<Void>> handleException(RemoteApiException aException)
        throws IOException
    {
        if (LOG.isDebugEnabled()) {
            LOG.error(aException.getMessage(), aException);
        }
        else {
            LOG.error(aException.getMessage());
        }

        return ResponseEntity.status(aException.getStatus()) //
                .contentType(APPLICATION_JSON) //
                .body(new RResponse<>(ERROR, aException.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<RResponse<Void>> handleException(Exception aException) throws IOException
    {
        LOG.error(aException.getMessage(), aException);

        return ResponseEntity.status(INTERNAL_SERVER_ERROR) //
                .contentType(APPLICATION_JSON) //
                .body(new RResponse<>(ERROR, "Internal server error: " + aException.getMessage()));
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<RResponse<Void>> handleException(AccessDeniedException aException)
        throws IOException
    {
        if (LOG.isDebugEnabled()) {
            LOG.error(aException.getMessage(), aException);
        }
        else {
            LOG.error(aException.getMessage());
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN) //
                .contentType(APPLICATION_JSON) //
                .body(new RResponse<>(ERROR, aException.getMessage()));
    }

    protected User getSessionOwner() throws ObjectNotFoundException
    {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        return getUser(username);
    }

    protected User getUser(String aUserId) throws ObjectNotFoundException
    {
        var user = userRepository.get(aUserId);
        if (user == null) {
            throw new ObjectNotFoundException("User [" + aUserId + "] not found.");
        }
        return user;
    }

    protected Project getProject(long aProjectId)
        throws ObjectNotFoundException, AccessForbiddenException
    {
        // Get current user - this will throw an exception if the current user does not exit
        var sessionOwner = getSessionOwner();

        // Get project
        Project project;
        try {
            project = projectService.getProject(aProjectId);
        }
        catch (NoResultException e) {
            throw new ObjectNotFoundException("Project [" + aProjectId + "] not found.");
        }

        // Check for the access
        assertPermission(
                "User [" + sessionOwner.getUsername() + "] is not allowed to access project ["
                        + aProjectId + "]",
                projectService.hasRole(sessionOwner, project, MANAGER)
                        || userRepository.isAdministrator(sessionOwner));

        return project;
    }

    protected SourceDocument getDocument(Project aProject, long aDocumentId)
        throws ObjectNotFoundException
    {
        try {
            return documentService.getSourceDocument(aProject.getId(), aDocumentId);
        }
        catch (NoResultException e) {
            throw new ObjectNotFoundException("Document [" + aDocumentId + "] in project ["
                    + aProject.getId() + "] not found.");
        }
    }

    protected AnnotationDocument getAnnotation(SourceDocument aDocument, String aUser,
            boolean aCreateIfMissing)
        throws ObjectNotFoundException
    {
        try {
            if (aCreateIfMissing) {
                return documentService.createOrGetAnnotationDocument(aDocument, getUser(aUser));
            }
            else {
                return documentService.getAnnotationDocument(aDocument, getUser(aUser));
            }
        }
        catch (NoResultException e) {
            throw new ObjectNotFoundException(
                    "Annotation for user [" + aUser + "] on document [" + aDocument.getId()
                            + "] in project [" + aDocument.getProject().getId() + "] not found.");
        }
    }

    protected void assertPermission(String aMessage, boolean aHasAccess)
        throws AccessForbiddenException
    {
        if (!aHasAccess) {
            throw new AccessForbiddenException(aMessage);
        }
    }

    protected CAS createCompatibleCas(long aProjectId, long aDocumentId, MultipartFile aFile,
            Optional<String> aFormatId)
        throws RemoteApiException, ClassNotFoundException, IOException, UIMAException
    {
        var project = getProject(aProjectId);
        var document = getDocument(project, aDocumentId);

        // Check if the format is supported
        var format = aFormatId.orElse(FORMAT_DEFAULT);
        if (!importExportService.getReadableFormatById(format).isPresent()) {
            throw new UnsupportedFormatException(
                    "Format [%s] not supported. Acceptable formats are %s.", format,
                    importExportService.getReadableFormats().stream().map(FormatSupport::getId)
                            .sorted().collect(toList()));
        }

        var originalFilename = isNotBlank(aFile.getOriginalFilename()) //
                ? aFile.getOriginalFilename()
                : document.getName();
        if (!documentService.isValidDocumentName(originalFilename)) {
            throw new IllegalNameException("Illegal document name [%s]", originalFilename);
        }

        // Convert the uploaded annotation document into a CAS
        File tmpFile = null;
        CAS annotationCas;
        try {
            tmpFile = createTempFile("upload", "." + getExtension(originalFilename));
            aFile.transferTo(tmpFile);
            annotationCas = importExportService.importCasFromFile(tmpFile, document, format, null);
        }
        finally {
            if (tmpFile != null) {
                FileUtils.forceDelete(tmpFile);
            }
        }

        // Check if the uploaded file is compatible with the source document. They are compatible
        // if the text is the same and if all the token and sentence annotations have the same
        // offsets.
        var initialCas = documentService.createOrReadInitialCas(document);
        var initialText = initialCas.getDocumentText();
        var annotationText = annotationCas.getDocumentText();

        // If any of the texts contains tailing line breaks, we ignore that. We assume at the moment
        // that nobody will have created annotations over that trailing line breaks.
        initialText = StringUtils.chomp(initialText);
        annotationText = StringUtils.chomp(annotationText);

        assertSameText(initialText, annotationText);

        // Just in case we really had to chomp off a trailing line break from the annotation CAS,
        // make sure we copy over the proper text from the initial CAS
        // NOT AT HOME THIS YOU SHOULD TRY
        // SETTING THE SOFA STRING FORCEFULLY FOLLOWING THE DARK SIDE IS!
        forceOverwriteSofa(annotationCas, initialCas.getDocumentText());

        var annotationSentences = selectSentences(annotationCas);
        var initialSentences = selectSentences(initialCas);
        if (annotationSentences.size() != initialSentences.size()) {
            throw new IncompatibleDocumentException(
                    "Expected [%d] sentences, but annotation document contains [%d] sentences.",
                    initialSentences.size(), annotationSentences.size());
        }
        assertCompatibleOffsets(initialSentences, annotationSentences);

        var annotationTokens = selectTokens(annotationCas);
        var initialTokens = selectTokens(initialCas);
        if (annotationTokens.size() != initialTokens.size()) {
            throw new IncompatibleDocumentException(
                    "Expected [%d] tokens, but annotation document contains [%d] tokens.",
                    initialTokens.size(), annotationTokens.size());
        }
        assertCompatibleOffsets(initialTokens, annotationTokens);

        return annotationCas;
    }

    protected ResponseEntity<byte[]> readAnnotation(long aProjectId, long aDocumentId,
            String aAnnotatorId, Mode aMode, Optional<String> aFormat)
        throws RemoteApiException, ClassNotFoundException, IOException, UIMAException
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var doc = getDocument(project, aDocumentId);

        // Check format
        String formatId;
        if (aFormat.isPresent()) {
            if (VAL_ORIGINAL.equals(aFormat.get())) {
                formatId = doc.getFormat();
            }
            else {
                formatId = aFormat.get();
            }
        }
        else {
            formatId = doc.getFormat();
        }

        // Determine the format
        var format = importExportService.getWritableFormatById(formatId)
                .orElseThrow(() -> new UnsupportedFormatException(
                        "Format [%s] is not writable. Acceptable formats are %s.", formatId,
                        importExportService.getWritableFormats().stream() //
                                .map(FormatSupport::getId) //
                                .sorted().collect(Collectors.toList())));

        // In principle we don't need this call - but it makes sure that we check that the
        // annotation document entry is actually properly set up in the database.
        if (Mode.ANNOTATION.equals(aMode)) {
            getAnnotation(doc, aAnnotatorId, false);
        }

        // Create a temporary export file from the annotations
        File exportedAnnoFile = null;
        byte[] resource;
        try {
            exportedAnnoFile = importExportService.exportAnnotationDocument(doc, aAnnotatorId,
                    format, Mode.ANNOTATION);
            resource = FileUtils.readFileToByteArray(exportedAnnoFile);

            var filename = FilenameUtils.removeExtension(doc.getName());
            filename += "-" + aAnnotatorId;
            // Actually, exportedAnnoFile cannot be null here - the warning can be ignored.
            filename += "." + FilenameUtils.getExtension(exportedAnnoFile.getName());

            var httpHeaders = new HttpHeaders();
            httpHeaders.setContentLength(resource.length);
            httpHeaders.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            return new ResponseEntity<>(resource, httpHeaders, OK);
        }
        finally {
            if (exportedAnnoFile != null) {
                FileUtils.forceDelete(exportedAnnoFile);
            }
        }
    }

    private static <T extends AnnotationFS> void assertCompatibleOffsets(Collection<T> aExpected,
            Collection<T> aActual)
        throws IncompatibleDocumentException
    {
        int unitIndex = 0;
        Iterator<T> asi = aExpected.iterator();
        Iterator<T> isi = aActual.iterator();
        // At this point we know that the number of sentences is the same, so it is ok to check only
        // one of the iterators for hasNext()
        while (asi.hasNext()) {
            T as = asi.next();
            T is = isi.next();
            if (as.getBegin() != is.getBegin() || as.getEnd() != is.getEnd()) {
                throw new IncompatibleDocumentException(
                        "Expected %s [%d] to have range [%d-%d], but instead found range "
                                + "[%d-%d] in annotation document.",
                        is.getType().getShortName(), unitIndex, is.getBegin(), is.getEnd(),
                        as.getBegin(), as.getEnd());
            }
            unitIndex++;
        }
    }

    private static void assertSameText(String initialText, String annotationText)
        throws IncompatibleDocumentException
    {
        if (ObjectUtils.notEqual(initialText, annotationText)) {
            int diffIndex = StringUtils.indexOfDifference(initialText, annotationText);
            String expected = initialText.substring(diffIndex,
                    Math.min(initialText.length(), diffIndex + 20));
            String actual = annotationText.substring(diffIndex,
                    Math.min(annotationText.length(), diffIndex + 20));
            throw new IncompatibleDocumentException(
                    "Text of annotation document does not match text of source document at offset "
                            + "[%d]. Expected [%s] but found [%s].",
                    diffIndex, expected, actual);
        }
    }
}
