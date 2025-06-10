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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.legacy;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UIMAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebAnnoTsv3FormatSupport;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.io.ZipUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Expose some functions of WebAnno via a RESTful remote API.
 */
@ConditionalOnExpression("false") // Auto-configured - avoid package scanning
@Controller
@RequestMapping(LegacyRemoteApiController.API_BASE)
public class LegacyRemoteApiController
{
    public static final String API_BASE = "/api/v1";

    private static final String META_INF = "META-INF/";

    private static final String PROJECTS = "projects";
    private static final String DOCUMENTS = "sourcedocs";
    private static final String ANNOTATIONS = "annos";
    private static final String CURATION = "curationdoc";

    private static final String PARAM_PROJECT_ID = "projectId";
    private static final String PARAM_DOCUMENT_ID = "documentId";
    private static final String PARAM_USERNAME = "username";
    private static final String PARAM_FILE = "file";
    private static final String PARAM_FILETYPE = "filetype";
    private static final String PARAM_NAME = "name";
    private static final String PARAM_TITLE = "title";
    private static final String PARAM_FORMAT = "format";

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private @Autowired ProjectService projectRepository;
    private @Autowired DocumentService documentRepository;
    private @Autowired DocumentImportExportService importExportService;
    private @Autowired UserDao userRepository;

    /**
     * Create a new project.
     *
     * To test when running in Eclipse, use the Linux "curl" command.
     *
     * curl -v -F 'file=@test.zip' -F 'name=Test' -F 'filetype=tcf'
     * 'http://USERNAME:PASSWORD@localhost:8080/webanno-webapp/api/projects'
     *
     * @param aSlug
     *            the name of the project to create.
     * @param aFileType
     *            the type of the files contained in the ZIP.
     * @param aFile
     *            a ZIP file containing the project data.
     * @param aName
     *            the desired name of the project (optional)
     * @return JSON object with information about the project
     * @throws Exception
     *             if there was an error.
     */
    @Operation(summary = "Create a new project")
    @PostMapping(//
            value = ("/" + PROJECTS), //
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> projectCreate( //
            @RequestParam(PARAM_FILE) MultipartFile aFile, //
            @RequestParam(PARAM_NAME) String aSlug, //
            @RequestParam(PARAM_TITLE) Optional<String> aName, //
            @RequestParam(PARAM_FILETYPE) String aFileType)
        throws Exception
    {
        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("User [" + username + "] not found.");
        }

        // Check for the access
        boolean hasAccess = userRepository.isProjectCreator(user)
                || userRepository.isAdministrator(user);
        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("User [" + username + "] is not allowed to create projects");
        }

        // Existing project
        if (projectRepository.existsProjectWithSlug(aSlug)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A project with URL slug [" + aSlug + "] already exists");
        }

        // Check archive
        try (InputStream is = new BufferedInputStream(aFile.getInputStream())) {
            if (!ZipUtils.isZipStream(is)) {
                return ResponseEntity.badRequest().body("Invalid ZIP file");
            }
        }

        // Create the project and initialize tags
        LOG.info("Creating project [" + aSlug + "]");
        Project project = new Project();
        project.setSlug(aSlug);
        project.setName(aName.orElse(aSlug));
        project = projectRepository.createProject(project);
        projectRepository.initializeProject(project);

        // Create permission for the project creator
        projectRepository.assignRole(project, user, MANAGER, CURATOR, ANNOTATOR);

        // Iterate through all the files in the ZIP

        // If the current filename does not start with "." and is in the root folder of the ZIP,
        // import it as a source document
        String filename = aFile.getOriginalFilename();
        // temp-file prefix must be at least 3 chars
        filename = StringUtils.rightPad(filename, 3, "_");
        File zipFile = File.createTempFile(filename, ".zip");
        aFile.transferTo(zipFile);
        ZipFile zip = new ZipFile(zipFile);

        for (Enumeration<?> zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            // Get ZipEntry which is a file or a directory
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // If it is the zip name, ignore it
            if ((FilenameUtils.removeExtension(filename) + "/").equals(entry.toString())) {
                continue;
            }
            // IF the current filename is META-INF/webanno/source-meta-data.properties store it as
            // project meta data
            else if (entry.toString().replace("/", "")
                    .equals((META_INF + "webanno/source-meta-data.properties").replace("/", ""))) {
                InputStream zipStream = zip.getInputStream(entry);
                projectRepository.savePropertiesFile(project, zipStream, entry.toString());

            }
            // File not in the Zip's root folder OR not
            // META-INF/webanno/source-meta-data.properties
            else if (StringUtils.countMatches(entry.toString(), "/") > 1) {
                continue;
            }
            // If the current filename does not start with "." and is in the root folder of the
            // ZIP, import it as a source document
            else if (!FilenameUtils.getExtension(entry.toString()).equals("")
                    && !FilenameUtils.getName(entry.toString()).equals(".")) {

                uploadSourceDocument(zip, entry, project, aFileType);
            }
        }

        LOG.info("Successfully created project [" + aSlug + "] for user [" + username + "]");

        JSONObject projectJSON = new JSONObject();
        projectJSON.append(aSlug, project.getId());
        return ResponseEntity.ok(projectJSON.toString());
    }

    /**
     * List all the projects for a given user with their roles
     * 
     * Test when running in Eclipse: Open your browser, paste following URL with appropriate values
     * for username and password:
     * 
     * http://USERNAME:PASSWORD@localhost:8080/webanno-webapp/api/projects
     * 
     * @return JSON string of project where user has access to and respective roles in the project.
     * @throws Exception
     *             if there was an error.
     */
    @Operation(summary = "List all the projects for a given user with their roles")
    @GetMapping(value = ("/" + PROJECTS))
    public ResponseEntity<String> projectList() throws Exception
    {
        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("User [" + username + "] not found.");
        }

        // Get projects with permission
        List<Project> accessibleProjects = projectRepository.listAccessibleProjects(user);

        // Add permissions for each project into JSON array and store in JSON object
        JSONObject returnJSONObj = new JSONObject();
        for (Project project : accessibleProjects) {
            String projectId = Long.toString(project.getId());
            List<ProjectPermission> projectPermissions = projectRepository
                    .listProjectPermissionLevel(user, project);
            JSONArray permissionArr = new JSONArray();
            JSONObject projectJSON = new JSONObject();

            for (ProjectPermission p : projectPermissions) {
                permissionArr.put(p.getLevel().getName());
            }
            projectJSON.put(project.getSlug(), permissionArr);
            returnJSONObj.put(projectId, projectJSON);
        }
        return ResponseEntity.ok(returnJSONObj.toString());
    }

    /**
     * Delete a project where user has a ADMIN role
     * 
     * To test when running in Eclipse, use the Linux "curl" command.
     * 
     * curl -v -X DELETE
     * 'http://USERNAME:PASSWORD@localhost:8080/webanno-webapp/api/projects/{aProjectId}'
     * 
     * @param aProjectId
     *            The id of the project.
     * @return message about the result of the operation
     * @throws Exception
     *             if there was an error.
     */
    @Operation(summary = "Delete a project managed by the given user")
    @DeleteMapping(value = ("/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}"))
    public ResponseEntity<String> projectDelete(@PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("User [" + username + "] not found.");
        }

        // Get project
        Project project;
        try {
            project = projectRepository.getProject(aProjectId);
        }
        catch (NoResultException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project [" + aProjectId + "] not found.");
        }

        // Check for the access
        boolean hasAccess = projectRepository.hasRole(user, project, MANAGER)
                || userRepository.isAdministrator(user);
        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User [" + username
                    + "] is not allowed to access project [" + aProjectId + "]");
        }

        // remove project is user has admin access
        LOG.info("Deleting project [" + aProjectId + "]");
        projectRepository.removeProject(project);
        LOG.info("Successfully deleted project [" + aProjectId + "]");
        return ResponseEntity.ok("Project [" + aProjectId + "] deleted.");
    }

    /**
     * Show source documents in given project where user has ADMIN access
     * 
     * http://USERNAME:PASSWORD@localhost:8080/webanno-webapp/api/projects/{aProjectId}/sourcedocs
     * 
     * @param aProjectId
     *            the project ID
     * @return JSON with {@link SourceDocument} : id
     * @throws Exception
     *             if something went wrong
     */
    @Operation(summary = "Show source documents in a project managed by the given user")
    @GetMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS)
    public ResponseEntity<String> sourceDocumentList(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("User [" + username + "] not found.");
        }

        // Get project
        Project project;
        try {
            project = projectRepository.getProject(aProjectId);
        }
        catch (NoResultException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project [" + aProjectId + "] not found.");
        }

        // Check for the access
        boolean hasAccess = projectRepository.hasRole(user, project, MANAGER)
                || userRepository.isAdministrator(user);
        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User [" + username
                    + "] is not allowed to access project [" + aProjectId + "]");
        }

        List<SourceDocument> srcDocumentList = documentRepository.listSourceDocuments(project);
        JSONArray sourceDocumentJSONArr = new JSONArray();
        for (SourceDocument s : srcDocumentList) {
            JSONObject sourceDocumentJSONObj = new JSONObject();
            sourceDocumentJSONObj.put("id", s.getId());
            sourceDocumentJSONObj.put("name", s.getName());
            sourceDocumentJSONObj.put("state", s.getState());
            sourceDocumentJSONArr.put(sourceDocumentJSONObj);
        }

        return ResponseEntity.ok(sourceDocumentJSONArr.toString());
    }

    /**
     * Delete the source document in project if user has an ADMIN permission
     * 
     * To test when running in Eclipse, use the Linux "curl" command.
     * 
     * curl -v -X DELETE
     * 'http://USERNAME:PASSWORD@localhost:8080/webanno-webapp/api/projects/{aProjectId}/sourcedocs/
     * {aSourceDocumentId}'
     * 
     * @param aProjectId
     *            {@link Project} ID.
     * @param aSourceDocumentId
     *            {@link SourceDocument} ID.
     * @return message about the result of the operation
     * @throws Exception
     *             if there was an error.
     */
    @Operation(summary = "Delete the source document in project managed by the given user")
    @DeleteMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
            + PARAM_DOCUMENT_ID + "}")
    public ResponseEntity<String> sourceDocumentDelete(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aSourceDocumentId)
        throws Exception
    {
        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("User [" + username + "] not found.");
        }

        // Get project
        Project project;
        try {
            project = projectRepository.getProject(aProjectId);
        }
        catch (NoResultException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project [" + aProjectId + "] not found.");
        }

        // Check for the access
        boolean hasAccess = projectRepository.hasRole(user, project, MANAGER)
                || userRepository.isAdministrator(user);
        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User [" + username
                    + "] is not allowed to access project [" + aProjectId + "]");
        }

        LOG.info("Deleting document [" + project.getName() + "]");

        // Get source document
        SourceDocument srcDocument;
        try {
            srcDocument = documentRepository.getSourceDocument(aProjectId, aSourceDocumentId);
        }
        catch (NoResultException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Source document ["
                    + aSourceDocumentId + "] not found in project [" + aProjectId + "] not found.");
        }

        documentRepository.removeSourceDocument(srcDocument);

        LOG.info("Successfully deleted project : [" + aProjectId + "]");

        return ResponseEntity.ok("Source document [" + aSourceDocumentId + "] in project ["
                + aProjectId + "] deleted.");
    }

    /**
     * Upload a source document into project where user has "ADMIN" role
     * 
     * Test when running in Eclipse, use the Linux "curl" command.
     * 
     * curl -v -F 'file=@test.txt' -F 'filetype=text'
     * 'http://USERNAME:PASSWORD@localhost:8080/webanno-webapp/api/projects/{aProjectId}/sourcedocs/
     * '
     * 
     * @param aFile
     *            File for {@link SourceDocument}.
     * @param aProjectId
     *            {@link Project} id.
     * @param aFileType
     *            Extension of the file.
     * @return returns JSON string with id to the created source document.
     * @throws Exception
     *             if there was an error.
     */
    @Operation(summary = "Upload a source document into project managed by the given user")
    @PostMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS, //
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> sourceDocumentCreate(
            @RequestParam(PARAM_FILE) MultipartFile aFile,
            @RequestParam(PARAM_FILETYPE) String aFileType,
            @PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("User [" + username + "] not found.");
        }

        // Get project
        Project project;
        try {
            project = projectRepository.getProject(aProjectId);
        }
        catch (NoResultException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project [" + aProjectId + "] not found.");
        }

        // Check for the access
        boolean hasAccess = projectRepository.hasRole(user, project, MANAGER)
                || userRepository.isAdministrator(user);
        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User [" + username
                    + "] is not allowed to access project [" + aProjectId + "]");
        }

        // Existing project
        if (documentRepository.existsSourceDocument(project, aFile.getOriginalFilename())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    "A document with name [" + aFile.getOriginalFilename() + "] already exists");
        }

        // Check if file already present or not
        try (InputStream is = aFile.getInputStream()) {
            uploadSourceDocumentFile(is, aFile.getOriginalFilename(), project, aFileType);
        }

        // add id of added source document in return json string
        JSONObject returnJSON = new JSONObject();
        returnJSON.put("id",
                documentRepository.getSourceDocument(project, aFile.getOriginalFilename()).getId());
        return ResponseEntity.ok(returnJSON.toString());
    }

    /**
     * List annotation documents for a source document in a projects where user is ADMIN
     * 
     * Test when running in Eclipse: Open your browser, paste following URL with appropriate values:
     * 
     * http://USERNAME:PASSWORD@localhost:8080/webanno-webapp/api/projects/{aProjectId}/sourcedocs/{
     * aSourceDocumentId}/annos
     * 
     * @param aProjectId
     *            {@link Project} ID
     * @param aSourceDocumentId
     *            {@link SourceDocument} ID
     * @return JSON string of all the annotation documents with their projects.
     * @throws Exception
     *             if there was an error.
     */
    @Operation(summary = "List annotation documents for a source document in a projects managed by the given user")
    @GetMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
            + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS)
    public ResponseEntity<String> annotationDocumentList(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aSourceDocumentId)
        throws Exception
    {
        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("User [" + username + "] not found.");
        }

        // Get project
        Project project;
        try {
            project = projectRepository.getProject(aProjectId);
        }
        catch (NoResultException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project [" + aProjectId + "] not found.");
        }

        // Check for the access
        boolean hasAccess = projectRepository.hasRole(user, project, MANAGER)
                || userRepository.isAdministrator(user);
        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User [" + username
                    + "] is not allowed to access project [" + aProjectId + "]");
        }

        // Get source document
        SourceDocument srcDocument;
        try {
            srcDocument = documentRepository.getSourceDocument(aProjectId, aSourceDocumentId);
        }
        catch (NoResultException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Source document ["
                    + aSourceDocumentId + "] not found in project [" + aProjectId + "] not found.");
        }

        List<AnnotationDocument> annList = documentRepository
                .listAllAnnotationDocuments(srcDocument);
        JSONArray annDocArr = new JSONArray();
        for (AnnotationDocument annDoc : annList) {
            if (annDoc.getState().equals(AnnotationDocumentState.FINISHED)
                    || annDoc.getState().equals(AnnotationDocumentState.IN_PROGRESS)) {
                SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ssZ");
                JSONObject annDocObj = new JSONObject();
                annDocObj.put("user", annDoc.getUser());
                annDocObj.put("state", annDoc.getState().getId());
                if (annDoc.getTimestamp() != null) {
                    annDocObj.put("timestamp", sdf.format(annDoc.getTimestamp()));
                }
                annDocArr.put(annDocObj);
            }
        }

        JSONObject returnJSON = new JSONObject();
        returnJSON.put(srcDocument.getName(), annDocArr);
        return ResponseEntity.ok(returnJSON.toString());
    }

    /**
     * Download annotation document with requested parameters.
     * 
     * Test when running in Eclipse: Open your browser, paste following URL with appropriate values:
     *
     * http://USERNAME:PASSWORD@localhost:8080/webanno-webapp/api/projects/{aProjectId}/sourcedocs/{
     * aSourceDocumentId}/annos/{annotatorName}?format="text"
     *
     * @param response
     *            HttpServletResponse.
     * @param aProjectId
     *            {@link Project} ID.
     * @param aSourceDocumentId
     *            {@link SourceDocument} ID.
     * @param annotatorName
     *            {@link User} name.
     * @param aFormatId
     *            Export format.
     * @throws Exception
     *             if there was an error.
     */
    @Operation(summary = "Download annotation document from a projects managed by the given user")
    @GetMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
            + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS + "/{" + PARAM_USERNAME + "}")
    public void annotationDocumentRead(HttpServletResponse response,
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aSourceDocumentId,
            @PathVariable(PARAM_USERNAME) String annotatorName,
            @RequestParam(value = PARAM_FORMAT, required = false) String aFormatId)
        throws Exception
    {
        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        if (user == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(),
                    "User [" + username + "] not found.");
            return;
        }

        // Get project
        Project project;
        try {
            project = projectRepository.getProject(aProjectId);
        }
        catch (NoResultException e) {
            response.sendError(HttpStatus.NOT_FOUND.value(),
                    "Project" + aProjectId + "] not found.");
            return;
        }

        // Check for the access
        boolean hasAccess = projectRepository.hasRole(user, project, MANAGER)
                || userRepository.isAdministrator(user);
        if (!hasAccess) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "User [" + username
                    + "] is not allowed to access project [" + aProjectId + "]");
            return;
        }

        // Get annotator user
        User annotator = userRepository.get(annotatorName);
        if (annotator == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(),
                    "Annotator user [" + annotatorName + "] not found.");
            return;
        }

        // Get source document
        SourceDocument srcDoc;
        try {
            srcDoc = documentRepository.getSourceDocument(aProjectId, aSourceDocumentId);
        }
        catch (NoResultException e) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "Document [" + aSourceDocumentId
                    + "] not found in project [" + aProjectId + "].");
            return;
        }

        // Get annotation document
        AnnotationDocument annDoc;
        try {
            annDoc = documentRepository.getAnnotationDocument(srcDoc, annotator);
        }
        catch (NoResultException e) {
            response.sendError(HttpStatus.NOT_FOUND.value(),
                    "Annotations for user [" + annotatorName + "] not found on document ["
                            + aSourceDocumentId + "] in project [" + aProjectId + "].");
            return;
        }

        String formatId;
        if (aFormatId == null) {
            formatId = srcDoc.getFormat();
        }
        else {
            formatId = aFormatId;
        }

        // Determine the format
        FormatSupport format = importExportService.getWritableFormatById(formatId).orElseGet(() -> {
            FormatSupport formatSupport = importExportService.getFallbackFormat();
            LOG.info("{} Format [{}] is not writable - exporting as [{}] instead.", srcDoc,
                    formatId, formatSupport.getName());
            return formatSupport;
        });

        // Temporary file of annotation document
        File downloadableFile = importExportService.exportAnnotationDocument(srcDoc, annotatorName,
                format, Mode.ANNOTATION);

        try {
            // Set mime type
            String mimeType = URLConnection.guessContentTypeFromName(downloadableFile.getName());
            if (mimeType == null) {
                LOG.info("mimetype is not detectable, will take default");
                mimeType = "application/octet-stream";
            }

            // Set response
            response.setContentType(mimeType);
            response.setContentType("application/force-download");
            response.setHeader("Content-Disposition",
                    "inline; filename=\"" + downloadableFile.getName() + "\"");
            response.setContentLength((int) downloadableFile.length());
            InputStream inputStream = new BufferedInputStream(
                    new FileInputStream(downloadableFile));
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        }
        catch (Exception e) {
            LOG.info("Exception occurred" + e.getMessage());
        }
        finally {
            if (downloadableFile.exists()) {
                downloadableFile.delete();
            }
        }
    }

    /**
     * Download curated document with requested parameters
     * 
     * Test when running in Eclipse: Open your browser, paste following URL with appropriate values:
     *
     * http://USERNAME:PASSWORD@localhost:8080/webanno-webapp/api/projects/{aProjectId}/curationdoc/
     * {aSourceDocumentId}?format=xmi
     * 
     * @param response
     *            HttpServletResponse.
     * @param aProjectId
     *            {@link Project} ID.
     * @param aSourceDocumentId
     *            {@link SourceDocument} ID.
     * @param aFormatId
     *            Export format.
     * @throws Exception
     *             if there was an error.
     */
    @Operation(summary = "Download curated document from a projects managed by the given user")
    @GetMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + CURATION + "/{"
            + PARAM_DOCUMENT_ID + "}")
    public void curationDocumentRead(HttpServletResponse response,
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_DOCUMENT_ID) long aSourceDocumentId,
            @RequestParam(value = PARAM_FORMAT, required = false) String aFormatId)
        throws Exception
    {
        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        if (user == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(),
                    "User [" + username + "] not found.");
            return;
        }

        // Get project
        Project project;
        try {
            project = projectRepository.getProject(aProjectId);
        }
        catch (NoResultException e) {
            response.sendError(HttpStatus.NOT_FOUND.value(),
                    "Project" + aProjectId + "] not found.");
            return;
        }

        // Check for the access
        boolean hasAccess = projectRepository.hasRole(user, project, MANAGER)
                || userRepository.isAdministrator(user);
        if (!hasAccess) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "User [" + username
                    + "] is not allowed to access project [" + aProjectId + "]");
            return;
        }

        // Get source document
        SourceDocument srcDocument;
        try {
            srcDocument = documentRepository.getSourceDocument(aProjectId, aSourceDocumentId);
        }
        catch (NoResultException e) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "Source document [" + aSourceDocumentId
                    + "] not found in project [" + aProjectId + "] not found.");
            return;
        }

        // Check if curation is complete
        if (!SourceDocumentState.CURATION_FINISHED.equals(srcDocument.getState())) {
            response.sendError(HttpStatus.NOT_FOUND.value(),
                    "Curation of source document [" + aSourceDocumentId + "] not yet complete.");
            return;
        }

        String formatId;
        if (aFormatId == null) {
            formatId = srcDocument.getFormat();
        }
        else {
            formatId = aFormatId;
        }

        // Determine the format
        FormatSupport format = importExportService.getWritableFormatById(formatId).orElseGet(() -> {
            LOG.info("[{}] Format [{}] is not writable - exporting as WebAnno TSV3 instead.",
                    srcDocument.getName(), formatId);
            return new WebAnnoTsv3FormatSupport();
        });

        // Temporary file of annotation document
        File downloadableFile = importExportService.exportAnnotationDocument(srcDocument,
                WebAnnoConst.CURATION_USER, format, Mode.CURATION);

        try {
            // Set mime type
            String mimeType = URLConnection.guessContentTypeFromName(downloadableFile.getName());
            if (mimeType == null) {
                LOG.info("mimetype is not detectable, will take default");
                mimeType = "application/octet-stream";
            }

            // Set response
            response.setContentType(mimeType);
            response.setContentType("application/force-download");
            response.setHeader("Content-Disposition",
                    "inline; filename=\"" + downloadableFile.getName() + "\"");
            response.setContentLength((int) downloadableFile.length());
            InputStream inputStream = new BufferedInputStream(
                    new FileInputStream(downloadableFile));
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        }
        catch (Exception e) {
            LOG.info("Exception occurred" + e.getMessage());
        }
        finally {
            if (downloadableFile.exists()) {
                downloadableFile.delete();
            }
        }
    }

    private void uploadSourceDocumentFile(InputStream is, String name, Project project,
            String aFileType)
        throws IOException, UIMAException
    {
        // Check if it is a property file
        if (name.equals("source-meta-data.properties")) {
            projectRepository.savePropertiesFile(project, is, name);
        }
        else {
            SourceDocument document = new SourceDocument();
            document.setName(name);
            document.setProject(project);
            document.setFormat(aFileType);
            // Meta data entry to the database
            // Import source document to the project repository folder
            documentRepository.uploadSourceDocument(is, document);
        }
    }

    private void uploadSourceDocument(ZipFile zip, ZipEntry entry, Project project,
            String aFileType)
        throws IOException, UIMAException
    {
        String fileName = FilenameUtils.getName(entry.toString());

        InputStream zipStream = zip.getInputStream(entry);
        SourceDocument document = new SourceDocument();
        document.setName(fileName);
        document.setProject(project);
        document.setFormat(aFileType);
        // Meta data entry to the database
        // Import source document to the project repository folder
        documentRepository.uploadSourceDocument(zipStream, document);
    }
}
