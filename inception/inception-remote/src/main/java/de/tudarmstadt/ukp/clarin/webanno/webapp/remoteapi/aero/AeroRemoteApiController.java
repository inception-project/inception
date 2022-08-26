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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentences;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectTokens;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RMessageLevel.ERROR;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RMessageLevel.INFO;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import javax.persistence.NoResultException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.Sofa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebAnnoTsv3FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.AccessForbiddenException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.IllegalObjectStateException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.IncompatibleDocumentException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.ObjectExistsException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.ObjectNotFoundException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.RemoteApiException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.UnsupportedFormatException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RAnnotation;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RPermission;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RProject;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.export.ImportUtil;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RemoteApiAutoConfiguration#aeroRemoteApiController}.
 * </p>
 */
@ConditionalOnWebApplication
@RequestMapping(AeroRemoteApiController.API_BASE)
public class AeroRemoteApiController
{
    public static final String API_BASE = "/api/aero/v1";

    private static final String PROJECTS = "projects";
    private static final String DOCUMENTS = "documents";
    private static final String ANNOTATIONS = "annotations";
    private static final String CURATION = "curation";
    private static final String IMPORT = "import";
    private static final String EXPORT = "export.zip";
    private static final String STATE = "state";
    private static final String PERMISSIONS = "permissions";

    private static final String PARAM_FILE = "file";
    private static final String PARAM_CONTENT = "content";
    private static final String PARAM_NAME = "name";
    private static final String PARAM_TITLE = "title";
    private static final String PARAM_FORMAT = "format";
    private static final String PARAM_STATE = "state";
    private static final String PARAM_CREATOR = "creator";
    private static final String PARAM_PROJECT_ID = "projectId";
    private static final String PARAM_ANNOTATOR_ID = "userId";
    private static final String PARAM_DOCUMENT_ID = "documentId";
    private static final String PARAM_CREATE_MISSING_USERS = "createMissingUsers";
    private static final String PARAM_ROLES = "roles";

    private static final String VAL_ORIGINAL = "ORIGINAL";

    // private static final String PROP_ID = "id";
    // private static final String PROP_NAME = "name";
    // private static final String PROP_STATE = "state";
    // private static final String PROP_USER = "user";
    // private static final String PROP_TIMESTAMP = "user";

    private static final String FORMAT_DEFAULT = "text";

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private @Autowired DocumentService documentService;
    private @Autowired CurationDocumentService curationService;
    private @Autowired ProjectService projectService;
    private @Autowired DocumentImportExportService importExportService;
    private @Autowired UserDao userRepository;
    private @Autowired ProjectExportService exportService;

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

        return ResponseEntity.status(aException.getStatus()).contentType(APPLICATION_JSON)
                .body(new RResponse<>(ERROR, aException.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<RResponse<Void>> handleException(Exception aException) throws IOException
    {
        LOG.error(aException.getMessage(), aException);

        return ResponseEntity.status(INTERNAL_SERVER_ERROR).contentType(APPLICATION_JSON)
                .body(new RResponse<>(ERROR, "Internal server error: " + aException.getMessage()));
    }

    private User getCurrentUser() throws ObjectNotFoundException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return getUser(username);
    }

    private User getUser(String aUserId) throws ObjectNotFoundException
    {
        User user = userRepository.get(aUserId);
        if (user == null) {
            throw new ObjectNotFoundException("User [" + aUserId + "] not found.");
        }
        return user;
    }

    private Project getProject(long aProjectId)
        throws ObjectNotFoundException, AccessForbiddenException
    {
        // Get current user - this will throw an exception if the current user does not exit
        User user = getCurrentUser();

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
                "User [" + user.getUsername() + "] is not allowed to access project [" + aProjectId
                        + "]",
                projectService.hasRole(user, project, MANAGER)
                        || userRepository.isAdministrator(user));

        return project;
    }

    private SourceDocument getDocument(Project aProject, long aDocumentId)
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

    private AnnotationDocument getAnnotation(SourceDocument aDocument, String aUser,
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

    private void assertPermission(String aMessage, boolean aHasAccess)
        throws AccessForbiddenException
    {
        if (!aHasAccess) {
            throw new AccessForbiddenException(aMessage);
        }
    }

    @Operation(summary = "List the projects accessible by the authenticated user")
    @GetMapping(value = ("/" + PROJECTS), produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<List<RProject>>> projectList() throws Exception
    {
        // Get current user - this will throw an exception if the current user does not exit
        User user = getCurrentUser();

        // Get projects with permission
        List<Project> accessibleProjects = projectService.listAccessibleProjects(user);

        // Collect all the projects
        List<RProject> projectList = new ArrayList<>();
        for (Project project : accessibleProjects) {
            projectList.add(new RProject(project));
        }
        return ResponseEntity.ok(new RResponse<>(projectList));
    }

    @Operation(summary = "Create a new project")
    @PostMapping(//
            value = ("/" + PROJECTS), //
            consumes = { ALL_VALUE, MULTIPART_FORM_DATA_VALUE }, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RProject>> projectCreate( //
            @RequestParam(PARAM_NAME) String aSlug, //
            @RequestParam(PARAM_TITLE) Optional<String> aName, //
            @RequestParam(PARAM_CREATOR) Optional<String> aCreator, //
            UriComponentsBuilder aUcb)
        throws Exception
    {
        // Get current user - this will throw an exception if the current user does not exit
        User user = getCurrentUser();

        // Check for the access
        assertPermission("User [" + user.getUsername() + "] is not allowed to create projects",
                userRepository.isProjectCreator(user) || userRepository.isAdministrator(user));

        // Check if the user can create projects for another user
        assertPermission(
                "User [" + user.getUsername() + "] is not allowed to create projects for user ["
                        + aCreator.orElse("<unspecified>") + "]",
                userRepository.isAdministrator(user)
                        || (aCreator.isPresent() && aCreator.get().equals(user.getUsername())));

        // Existing project
        if (projectService.existsProjectWithSlug(aSlug)) {
            throw new ObjectExistsException(
                    "A project with URL slug [" + aSlug + "] already exists");
        }

        // Create the project and initialize tags
        LOG.info("Creating project [{}]", aSlug);
        Project project = new Project();
        project.setSlug(aSlug);
        project.setName(aName.orElse(aSlug));
        project.setScriptDirection(ScriptDirection.LTR);
        project.setState(ProjectState.NEW);
        projectService.createProject(project);
        projectService.initializeProject(project);

        // Create permission for the project creator
        String owner = aCreator.isPresent() ? aCreator.get() : user.getUsername();
        projectService.assignRole(project, owner, MANAGER, CURATOR, ANNOTATOR);

        RResponse<RProject> response = new RResponse<>(new RProject(project));
        return ResponseEntity.created(aUcb.path(API_BASE + "/" + PROJECTS + "/{id}")
                .buildAndExpand(project.getId()).toUri()).body(response);
    }

    @Operation(summary = "Get information about a project")
    @GetMapping(value = ("/" + PROJECTS + "/{" + PARAM_PROJECT_ID
            + "}"), produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RProject>> projectRead(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        return ResponseEntity.ok(new RResponse<>(new RProject(project)));
    }

    @Operation(summary = "Delete an existing project")
    @DeleteMapping(value = ("/" + PROJECTS + "/{" + PARAM_PROJECT_ID
            + "}"), produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<Void>> projectDelete(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        projectService.removeProject(project);

        return ResponseEntity.ok(new RResponse<>(INFO, "Project [" + aProjectId + "] deleted."));
    }

    @Operation(summary = "Import a previously exported project")
    @PostMapping(//
            value = ("/" + PROJECTS + "/" + IMPORT), //
            consumes = MULTIPART_FORM_DATA_VALUE, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RProject>> projectImport(
            @RequestParam(name = PARAM_CREATE_MISSING_USERS, defaultValue = "false") boolean aCreateMissingUsers, //
            @RequestPart(PARAM_FILE) MultipartFile aFile)
        throws Exception
    {
        // Get current user - this will throw an exception if the current user does not exit
        User user = getCurrentUser();

        // Check for the access
        assertPermission("User [" + user.getUsername() + "] is not allowed to import projects",
                userRepository.isAdministrator(user));

        Project importedProject;
        File tempFile = File.createTempFile("webanno-training", null);
        try (InputStream is = new BufferedInputStream(aFile.getInputStream());
                OutputStream os = new FileOutputStream(tempFile);) {
            if (!ZipUtils.isZipStream(is)) {
                throw new UnsupportedFormatException("Invalid ZIP file");
            }

            IOUtils.copyLarge(is, os);

            if (!ImportUtil.isZipValidWebanno(tempFile)) {
                throw new UnsupportedFormatException("Incompatible to webanno ZIP file");
            }

            ProjectImportRequest request = new ProjectImportRequest(aCreateMissingUsers);
            importedProject = exportService.importProject(request, new ZipFile(tempFile));
        }
        finally {
            tempFile.delete();
        }

        return ResponseEntity.ok(new RResponse<>(new RProject(importedProject)));
    }

    @Operation(summary = "Export a project to a ZIP file")
    @GetMapping(value = ("/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + EXPORT), produces = {
            "application/zip", APPLICATION_JSON_VALUE })
    public ResponseEntity<InputStreamResource> projectExport(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @RequestParam(value = PARAM_FORMAT) Optional<String> aFormat)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        // Check if the format is supported
        if (aFormat.isPresent()) {
            importExportService.getWritableFormatById(aFormat.get())
                    .orElseThrow(() -> new UnsupportedFormatException(
                            "Format [%s] cannot be exported. Exportable formats are %s.",
                            aFormat.get(),
                            importExportService.getWritableFormats().stream()
                                    .map(FormatSupport::getId).sorted().collect(Collectors.toList())
                                    .toString()));
        }

        FullProjectExportRequest request = new FullProjectExportRequest(project,
                aFormat.orElse(WebAnnoTsv3FormatSupport.ID), true);
        ProjectExportTaskMonitor monitor = new ProjectExportTaskMonitor(project, null,
                "report-export");
        File exportedFile = exportService.exportProject(request, monitor);

        // Turn the file into a resource and auto-delete the file when the resource closes the
        // stream.
        InputStreamResource result = new InputStreamResource(new FileInputStream(exportedFile)
        {
            @Override
            public void close() throws IOException
            {
                super.close();
                FileUtils.forceDelete(exportedFile);
            }
        });

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.valueOf("application/zip"));
        httpHeaders.setContentLength(exportedFile.length());
        httpHeaders.set("Content-Disposition",
                "attachment; filename=\"" + exportedFile.getName() + "\"");

        return new ResponseEntity<>(result, httpHeaders, HttpStatus.OK);
    }

    @Operation(summary = "List documents in a project")
    @GetMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/"
            + DOCUMENTS, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<List<RDocument>>> documentList(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        List<SourceDocument> documents = documentService.listSourceDocuments(project);

        List<RDocument> documentList = new ArrayList<>();
        for (SourceDocument document : documents) {
            documentList.add(new RDocument(document));
        }

        return ResponseEntity.ok(new RResponse<>(documentList));
    }

    @Operation(summary = "Create a new document in a project")
    @PostMapping(//
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS, //
            consumes = MULTIPART_FORM_DATA_VALUE, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RDocument>> documentCreate(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @RequestParam(PARAM_CONTENT) MultipartFile aFile,
            @RequestParam(PARAM_NAME) String aName, @RequestParam(PARAM_FORMAT) String aFormat,
            @RequestParam(PARAM_STATE) Optional<String> aState, UriComponentsBuilder aUcb)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        // Check if the format is supported
        if (!importExportService.getReadableFormatById(aFormat).isPresent()) {
            throw new UnsupportedFormatException(
                    "Format [%s] not supported. Acceptable formats are %s.", aFormat,
                    importExportService.getReadableFormats().stream().map(FormatSupport::getId)
                            .sorted().collect(Collectors.toList()));
        }

        // Meta data entry to the database
        SourceDocument document = new SourceDocument();
        document.setProject(project);
        document.setName(aName);
        document.setFormat(aFormat);

        // Set state if one was provided
        if (aState.isPresent()) {
            SourceDocumentState state = parseSourceDocumentState(aState.get());
            switch (state) {
            case NEW: // fallthrough
            case ANNOTATION_IN_PROGRESS: // fallthrough
            case ANNOTATION_FINISHED: // fallthrough
                document.setState(state);
                documentService.createSourceDocument(document);
                break;
            case CURATION_IN_PROGRESS: // fallthrough
            case CURATION_FINISHED:
            default:
                throw new IllegalObjectStateException(
                        "State [%s] not valid when uploading a document.", aState.get());
            }
        }

        // Import source document to the project repository folder
        try (InputStream is = aFile.getInputStream()) {
            documentService.uploadSourceDocument(is, document);
        }

        RResponse<RDocument> rDocument = new RResponse<>(new RDocument(document));

        if (aState.isPresent()) {
            rDocument.addMessage(INFO, "State of document [" + document.getId() + "] set to ["
                    + sourceDocumentStateToString(document.getState()) + "]");
        }

        return ResponseEntity
                .created(aUcb.path(API_BASE + "/" + PROJECTS + "/{pid}/" + DOCUMENTS + "/{did}")
                        .buildAndExpand(project.getId(), document.getId()).toUri())
                .body(rDocument);
    }

    @Operation(summary = "Get a document from a project")
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = byte.class))))
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}", //
            produces = { APPLICATION_OCTET_STREAM_VALUE, APPLICATION_JSON_VALUE })
    public ResponseEntity<?> documentRead(@PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @RequestParam(value = PARAM_FORMAT) Optional<String> aFormat)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        SourceDocument doc = getDocument(project, aDocumentId);

        boolean originalFile;
        String formatId;
        if (aFormat.isPresent()) {
            if (VAL_ORIGINAL.equals(aFormat.get())) {
                formatId = doc.getFormat();
                originalFile = true;
            }
            else {
                formatId = aFormat.get();
                originalFile = doc.getFormat().equals(formatId);
            }
        }
        else {
            formatId = doc.getFormat();
            originalFile = true;
        }

        if (originalFile) {
            // Export the original file - no temporary file created here, we export directly from
            // the file system
            File docFile = documentService.getSourceDocumentFile(doc);
            FileSystemResource resource = new FileSystemResource(docFile);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentLength(resource.contentLength());
            httpHeaders.set("Content-Disposition",
                    "attachment; filename=\"" + doc.getName() + "\"");
            return new ResponseEntity<org.springframework.core.io.Resource>(resource, httpHeaders,
                    OK);
        }

        // Export a converted file - here we first export to a local temporary file and then
        // send that back to the client

        // Check if the format is supported
        FormatSupport format = importExportService.getWritableFormatById(formatId)
                .orElseThrow(() -> new UnsupportedFormatException(
                        "Format [%s] cannot be exported. Exportable formats are %s.", formatId,
                        importExportService.getWritableFormats().stream().map(FormatSupport::getId)
                                .sorted().collect(Collectors.toList()).toString()));

        // Create a temporary export file from the annotations
        CAS cas = documentService.createOrReadInitialCas(doc);

        File exportedFile = null;
        try {
            // Load the converted file into memory
            exportedFile = importExportService.exportCasToFile(cas, doc, doc.getName(), format);
            byte[] resource = FileUtils.readFileToByteArray(exportedFile);

            // Send it back to the client
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentLength(resource.length);
            httpHeaders.set("Content-Disposition",
                    "attachment; filename=\"" + exportedFile.getName() + "\"");

            return new ResponseEntity<>(resource, httpHeaders, OK);
        }
        finally {
            if (exportedFile != null) {
                FileUtils.forceDelete(exportedFile);
            }
        }
    }

    @Operation(summary = "Delete a document from a project")
    @DeleteMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}", //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<Void>> documentDelete(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        SourceDocument doc = getDocument(project, aDocumentId);
        documentService.removeSourceDocument(doc);

        return ResponseEntity.ok(new RResponse<>(INFO,
                "Document [" + aDocumentId + "] deleted from project [" + aProjectId + "]."));
    }

    @Operation(summary = "List annotations of a document in a project")
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<List<RAnnotation>>> annotationsList(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        SourceDocument doc = getDocument(project, aDocumentId);

        List<AnnotationDocument> annotations = documentService.listAnnotationDocuments(doc);

        List<RAnnotation> annotationList = new ArrayList<>();
        for (AnnotationDocument annotation : annotations) {
            annotationList.add(new RAnnotation(annotation));
        }

        return ResponseEntity.ok(new RResponse<>(annotationList));
    }

    @Operation(summary = "Update annotation state for a document in a project (non-AERO)")
    @PostMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{" //
                    + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS + "/{" + PARAM_ANNOTATOR_ID + "}/"
                    + STATE, //
            consumes = ALL_VALUE, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RAnnotation>> annotationsUpdateState(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @PathVariable(PARAM_ANNOTATOR_ID) String aAnnotatorId,
            @RequestParam(name = PARAM_STATE) Optional<String> aState)
        throws Exception
    {
        Project project = getProject(aProjectId);
        SourceDocument document = getDocument(project, aDocumentId);

        AnnotationDocument anno = getAnnotation(document, aAnnotatorId, false);
        documentService.setAnnotationDocumentState(anno,
                parseAnnotationDocumentState(aState.get()));
        documentService.createAnnotationDocument(anno);

        RResponse<RAnnotation> response = new RResponse<>(new RAnnotation(anno));
        response.addMessage(INFO,
                "State of annotations of user [" + aAnnotatorId + "] on document ["
                        + document.getId() + "] set to ["
                        + annotationDocumentStateToString(anno.getState()) + "]");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create or update annotations for a document in a project")
    @PostMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{" //
                    + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS + "/{" + PARAM_ANNOTATOR_ID + "}", //
            consumes = MULTIPART_FORM_DATA_VALUE, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RAnnotation>> annotationsCreate(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @PathVariable(PARAM_ANNOTATOR_ID) String aAnnotatorId,
            @RequestPart(PARAM_CONTENT) MultipartFile aFile,
            @RequestParam(PARAM_FORMAT) Optional<String> aFormat,
            @RequestParam(PARAM_STATE) Optional<String> aState, UriComponentsBuilder aUcb)
        throws Exception
    {
        User annotator = getUser(aAnnotatorId);
        Project project = getProject(aProjectId);
        SourceDocument document = getDocument(project, aDocumentId);
        AnnotationDocument anno = getAnnotation(document, aAnnotatorId, true);

        CAS annotationCas = createCompatibleCas(aProjectId, aDocumentId, aFile, aFormat);

        // If they are compatible, then we can store the new annotations
        documentService.writeAnnotationCas(annotationCas, document, annotator, false);

        // Set state if one was provided
        if (aState.isPresent()) {
            anno.setState(parseAnnotationDocumentState(aState.get()));
            documentService.createAnnotationDocument(anno);
        }

        RResponse<RAnnotation> response = new RResponse<>(new RAnnotation(anno));

        if (aState.isPresent()) {
            response.addMessage(INFO,
                    "State of annotations of user [" + aAnnotatorId + "] on document ["
                            + document.getId() + "] set to ["
                            + annotationDocumentStateToString(anno.getState()) + "]");
        }

        return ResponseEntity.created(aUcb
                .path(API_BASE + "/" + PROJECTS + "/{pid}/" + DOCUMENTS + "/{did}/" + ANNOTATIONS
                        + "/{aid}")
                .buildAndExpand(project.getId(), document.getId(), annotator.getUsername()).toUri())
                .body(response);
    }

    @Operation(summary = "Get annotations of a document in a project")
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = byte.class))))
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS + "/{" + PARAM_ANNOTATOR_ID + "}", //
            produces = { APPLICATION_OCTET_STREAM_VALUE, APPLICATION_JSON_VALUE })
    public ResponseEntity<byte[]> annotationsRead(@PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @PathVariable(PARAM_ANNOTATOR_ID) String aAnnotatorId,
            @RequestParam(value = PARAM_FORMAT) Optional<String> aFormat)
        throws Exception
    {
        return readAnnotation(aProjectId, aDocumentId, aAnnotatorId, Mode.ANNOTATION, aFormat);
    }

    @Operation(summary = "Delete a user's annotations of one document from a project")
    @DeleteMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS + "/{" + PARAM_ANNOTATOR_ID + "}", //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<Void>> annotationsDelete(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @PathVariable(PARAM_ANNOTATOR_ID) String aAnnotatorId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        SourceDocument doc = getDocument(project, aDocumentId);
        AnnotationDocument anno = getAnnotation(doc, aAnnotatorId, false);
        documentService.removeAnnotationDocument(anno);
        documentService.deleteAnnotationCas(anno);

        return ResponseEntity
                .ok(new RResponse<>(INFO, "Annotations of user [" + aAnnotatorId + "] on document ["
                        + aDocumentId + "] deleted from project [" + aProjectId + "]."));
    }

    @Operation(summary = "Create curation for a document in a project")
    @PostMapping(//
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + //
                    "/{" + PARAM_DOCUMENT_ID + "}/" + CURATION, //
            consumes = MULTIPART_FORM_DATA_VALUE, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RAnnotation>> curationCreate(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @RequestPart(value = PARAM_CONTENT) MultipartFile aFile,
            @RequestParam(PARAM_FORMAT) Optional<String> aFormat,
            @RequestParam(PARAM_STATE) Optional<String> aState, UriComponentsBuilder aUcb)
        throws Exception
    {
        Project project = getProject(aProjectId);
        SourceDocument document = getDocument(project, aDocumentId);

        CAS annotationCas = createCompatibleCas(aProjectId, aDocumentId, aFile, aFormat);

        // If they are compatible, then we can store the new annotations
        curationService.writeCurationCas(annotationCas, document, false);

        AnnotationDocumentState resultState = AnnotationDocumentState.IN_PROGRESS;
        if (aState.isPresent()) {
            SourceDocumentState state = parseSourceDocumentState(aState.get());
            switch (state) {
            case CURATION_IN_PROGRESS:
                resultState = AnnotationDocumentState.IN_PROGRESS;
                document.setState(state);
                documentService.createSourceDocument(document);
                break;
            case CURATION_FINISHED:
                resultState = AnnotationDocumentState.FINISHED;
                document.setState(state);
                documentService.createSourceDocument(document);
                break;
            case NEW: // fallthrough
            case ANNOTATION_IN_PROGRESS: // fallthrough
            case ANNOTATION_FINISHED: // fallthrough
            default:
                throw new IllegalObjectStateException(
                        "State [%s] not valid when uploading a curation.", aState.get());
            }
        }
        else {
            document.setState(SourceDocumentState.CURATION_IN_PROGRESS);
            documentService.createSourceDocument(document);
        }

        RResponse<RAnnotation> response = new RResponse<>(
                new RAnnotation(WebAnnoConst.CURATION_USER, resultState, new Date()));
        return ResponseEntity.created(
                aUcb.path(API_BASE + "/" + PROJECTS + "/{pid}/" + DOCUMENTS + "/{did}/" + CURATION)
                        .buildAndExpand(project.getId(), document.getId()).toUri())
                .body(response);
    }

    @Operation(summary = "Get curated annotations of a document in a project")
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = byte.class))))
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}/" + CURATION, //
            produces = { APPLICATION_OCTET_STREAM_VALUE, APPLICATION_JSON_VALUE })
    public ResponseEntity<byte[]> curationRead(@PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId,
            @RequestParam(value = PARAM_FORMAT) Optional<String> aFormat)
        throws Exception
    {
        return readAnnotation(aProjectId, aDocumentId, WebAnnoConst.CURATION_USER, Mode.CURATION,
                aFormat);
    }

    @Operation(summary = "Delete a user's annotations of one document from a project")
    @DeleteMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}/" + CURATION, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<Void>> curationDelete(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aDocumentId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        SourceDocument doc = getDocument(project, aDocumentId);
        curationService.deleteCurationCas(doc);

        // If we delete the curation, it cannot be any longer in-progress or finished. The best
        // guess is that we set the state back to annotation-in-progress.
        switch (doc.getState()) {
        case CURATION_IN_PROGRESS: // Fall-through
        case CURATION_FINISHED:
            doc.setState(SourceDocumentState.ANNOTATION_IN_PROGRESS);
            documentService.createSourceDocument(doc);
            break;
        default:
            // Nothing to do
        }

        return ResponseEntity.ok(new RResponse<>(INFO, "Curated annotations for document ["
                + aDocumentId + "] deleted from project [" + aProjectId + "]."));
    }

    private ResponseEntity<byte[]> readAnnotation(long aProjectId, long aDocumentId,
            String aAnnotatorId, Mode aMode, Optional<String> aFormat)
        throws RemoteApiException, ClassNotFoundException, IOException, UIMAException
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        SourceDocument doc = getDocument(project, aDocumentId);

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
        FormatSupport format = importExportService.getWritableFormatById(formatId)
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
                    format, doc.getName(), Mode.ANNOTATION);
            resource = FileUtils.readFileToByteArray(exportedAnnoFile);
        }
        finally {
            if (exportedAnnoFile != null) {
                FileUtils.forceDelete(exportedAnnoFile);
            }
        }

        String filename = FilenameUtils.removeExtension(doc.getName());
        filename += "-" + aAnnotatorId;
        // Actually, exportedAnnoFile cannot be null here - the warning can be ignored.
        filename += "." + FilenameUtils.getExtension(exportedAnnoFile.getName());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentLength(resource.length);
        httpHeaders.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        return new ResponseEntity<>(resource, httpHeaders, OK);
    }

    private CAS createCompatibleCas(long aProjectId, long aDocumentId, MultipartFile aFile,
            Optional<String> aFormatId)
        throws RemoteApiException, ClassNotFoundException, IOException, UIMAException
    {
        Project project = getProject(aProjectId);
        SourceDocument document = getDocument(project, aDocumentId);

        // Check if the format is supported
        String format = aFormatId.orElse(FORMAT_DEFAULT);
        if (!importExportService.getReadableFormatById(format).isPresent()) {
            throw new UnsupportedFormatException(
                    "Format [%s] not supported. Acceptable formats are %s.", format,
                    importExportService.getReadableFormats().stream().map(FormatSupport::getId)
                            .sorted().collect(Collectors.toList()));
        }

        // Convert the uploaded annotation document into a CAS
        File tmpFile = null;
        CAS annotationCas;
        try {
            tmpFile = File.createTempFile("upload", ".bin");
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
        CAS initialCas = documentService.createOrReadInitialCas(document);
        String initialText = initialCas.getDocumentText();
        String annotationText = annotationCas.getDocumentText();

        // If any of the texts contains tailing line breaks, we ignore that. We assume at the moment
        // that nobody will have created annotations over that trailing line breaks.
        initialText = StringUtils.chomp(initialText);
        annotationText = StringUtils.chomp(annotationText);

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

        // Just in case we really had to chomp off a trailing line break from the annotation CAS,
        // make sure we copy over the proper text from the initial CAS
        // NOT AT HOME THIS YOU SHOULD TRY
        // SETTING THE SOFA STRING FORCEFULLY FOLLOWING THE DARK SIDE IS!
        forceOverwriteSofa(annotationCas, initialCas.getDocumentText());

        Collection<AnnotationFS> annotationSentences = selectSentences(annotationCas);
        Collection<AnnotationFS> initialSentences = selectSentences(initialCas);
        if (annotationSentences.size() != initialSentences.size()) {
            throw new IncompatibleDocumentException(
                    "Expected [%d] sentences, but annotation document contains [%d] sentences.",
                    initialSentences.size(), annotationSentences.size());
        }
        assertCompatibleOffsets(initialSentences, annotationSentences);

        Collection<AnnotationFS> annotationTokens = selectTokens(annotationCas);
        Collection<AnnotationFS> initialTokens = selectTokens(initialCas);
        if (annotationTokens.size() != initialTokens.size()) {
            throw new IncompatibleDocumentException(
                    "Expected [%d] sentences, but annotation document contains [%d] sentences.",
                    initialSentences.size(), annotationSentences.size());
        }
        assertCompatibleOffsets(initialTokens, annotationTokens);

        return annotationCas;
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

    private static void forceOverwriteSofa(CAS aCas, String aValue)
    {
        try {
            Sofa sofa = (Sofa) aCas.getSofa();
            MethodHandle _FH_sofaString = (MethodHandle) FieldUtils.readField(sofa,
                    "_FH_sofaString", true);
            Method method = MethodUtils.getMatchingMethod(Sofa.class, "wrapGetIntCatchException",
                    MethodHandle.class);
            int adjOffset;
            try {
                method.setAccessible(true);
                adjOffset = (int) method.invoke(null, _FH_sofaString);
            }
            finally {
                method.setAccessible(false);
            }
            sofa._setStringValueNcWj(adjOffset, aValue);
        }
        catch (Exception e) {
            throw new IllegalStateException("Cannot force-update SofA string", e);
        }
    }

    public static SourceDocumentState parseSourceDocumentState(String aState)
    {
        if (aState == null) {
            return null;
        }

        switch (aState) {
        case "NEW":
            return SourceDocumentState.NEW;
        case "ANNOTATION-IN-PROGRESS":
            return SourceDocumentState.ANNOTATION_IN_PROGRESS;
        case "ANNOTATION-COMPLETE":
            return SourceDocumentState.ANNOTATION_FINISHED;
        case "CURATION-COMPLETE":
            return SourceDocumentState.CURATION_FINISHED;
        case "CURATION-IN-PROGRESS":
            return SourceDocumentState.CURATION_IN_PROGRESS;
        default:
            throw new IllegalArgumentException("Unknown source document state [" + aState + "]");
        }
    }

    public static String projectStateToString(ProjectState aState)
    {
        if (aState == null) {
            return null;
        }

        switch (aState) {
        case NEW:
            return "NEW";
        case ANNOTATION_IN_PROGRESS:
            return "ANNOTATION-IN-PROGRESS";
        case ANNOTATION_FINISHED:
            return "ANNOTATION-COMPLETE";
        case CURATION_FINISHED:
            return "CURATION-COMPLETE";
        case CURATION_IN_PROGRESS:
            return "CURATION-IN-PROGRESS";
        default:
            throw new IllegalArgumentException("Unknown project state [" + aState + "]");
        }
    }

    public static String sourceDocumentStateToString(SourceDocumentState aState)
    {
        if (aState == null) {
            return null;
        }

        switch (aState) {
        case NEW:
            return "NEW";
        case ANNOTATION_IN_PROGRESS:
            return "ANNOTATION-IN-PROGRESS";
        case ANNOTATION_FINISHED:
            return "ANNOTATION-COMPLETE";
        case CURATION_FINISHED:
            return "CURATION-COMPLETE";
        case CURATION_IN_PROGRESS:
            return "CURATION-IN-PROGRESS";
        default:
            throw new IllegalArgumentException("Unknown source document state [" + aState + "]");
        }
    }

    public static AnnotationDocumentState parseAnnotationDocumentState(String aState)
    {
        if (aState == null) {
            return null;
        }

        switch (aState) {
        case "NEW":
            return AnnotationDocumentState.NEW;
        case "COMPLETE":
            return AnnotationDocumentState.FINISHED;
        case "LOCKED":
            return AnnotationDocumentState.IGNORE;
        case "IN-PROGRESS":
            return AnnotationDocumentState.IN_PROGRESS;
        default:
            throw new IllegalArgumentException(
                    "Unknown annotation document state [" + aState + "]");
        }
    }

    public static String annotationDocumentStateToString(AnnotationDocumentState aState)
    {
        if (aState == null) {
            return null;
        }

        switch (aState) {
        case NEW:
            return "NEW";
        case FINISHED:
            return "COMPLETE";
        case IGNORE:
            return "LOCKED";
        case IN_PROGRESS:
            return "IN-PROGRESS";
        default:
            throw new IllegalArgumentException(
                    "Unknown annotation document state [" + aState + "]");
        }
    }

    @Operation(summary = "List all permissions in the given project (non-AERO)")
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + PERMISSIONS, //
            produces = { APPLICATION_JSON_VALUE })
    public ResponseEntity<RResponse<List<RPermission>>> permissionRead(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        User user = getCurrentUser();

        // Check for the access
        assertPermission(
                "User [" + user.getUsername() + "] is not allowed to list project permissions",
                projectService.hasRole(user, project, MANAGER)
                        || userRepository.isAdministrator(user));

        var permissions = projectService.getProjectPermissions(project).stream()
                .map(RPermission::new) //
                .collect(toList());

        return ResponseEntity.ok(new RResponse<>(permissions));
    }

    @Operation(summary = "List permissions for a user in the given project (non-AERO)")
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + PERMISSIONS + "/{"
                    + PARAM_ANNOTATOR_ID + "}", //
            produces = { APPLICATION_JSON_VALUE })
    public ResponseEntity<RResponse<List<RPermission>>> permissionRead(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_ANNOTATOR_ID) String aSubjectUser)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        User user = getCurrentUser();

        // Check for the access
        assertPermission(
                "User [" + user.getUsername() + "] is not allowed to list project permissions",
                projectService.hasRole(user, project, MANAGER)
                        || userRepository.isAdministrator(user));

        User subjectUser = getUser(aSubjectUser);

        var permissions = projectService.listProjectPermissionLevel(subjectUser, project).stream()
                .map(RPermission::new) //
                .collect(toList());

        return ResponseEntity.ok(new RResponse<>(permissions));
    }

    @Operation(summary = "Assign roles to a user in the given project (non-AERO)")
    @PostMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + PERMISSIONS + "/{"
                    + PARAM_ANNOTATOR_ID + "}", //
            produces = { APPLICATION_JSON_VALUE })
    public ResponseEntity<RResponse<List<RPermission>>> permissionCreate(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_ANNOTATOR_ID) String aSubjectUser,
            @RequestParam(PARAM_ROLES) List<String> aRoles)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        User user = getCurrentUser();

        // Check for the access
        assertPermission(
                "User [" + user.getUsername() + "] is not allowed to manage project permissions",
                projectService.hasRole(user, project, MANAGER)
                        || userRepository.isAdministrator(user));

        User subjectUser = getUser(aSubjectUser);

        var roles = aRoles.stream().map(PermissionLevel::valueOf).toArray(PermissionLevel[]::new);

        projectService.assignRole(project, subjectUser, roles);

        var permissions = projectService.listProjectPermissionLevel(subjectUser, project).stream()
                .map(RPermission::new) //
                .collect(toList());

        return ResponseEntity.ok(new RResponse<>(permissions));
    }

    @Operation(summary = "Revoke roles to a user in the given project (non-AERO)")
    @DeleteMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + PERMISSIONS + "/{"
                    + PARAM_ANNOTATOR_ID + "}", //
            produces = { APPLICATION_JSON_VALUE })
    public ResponseEntity<RResponse<List<RPermission>>> permissionDelete(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_ANNOTATOR_ID) String aSubjectUser,
            @RequestParam(PARAM_ROLES) List<String> aRoles)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        Project project = getProject(aProjectId);

        User user = getCurrentUser();

        // Check for the access
        assertPermission(
                "User [" + user.getUsername() + "] is not allowed to manage project permissions",
                projectService.hasRole(user, project, MANAGER)
                        || userRepository.isAdministrator(user));

        User subjectUser = getUser(aSubjectUser);

        var roles = aRoles.stream().map(PermissionLevel::valueOf).toArray(PermissionLevel[]::new);

        projectService.revokeRole(project, subjectUser, roles);

        var permissions = projectService.listProjectPermissionLevel(subjectUser, project).stream()
                .map(RPermission::new) //
                .collect(toList());

        return ResponseEntity.ok(new RResponse<>(permissions));
    }
}
