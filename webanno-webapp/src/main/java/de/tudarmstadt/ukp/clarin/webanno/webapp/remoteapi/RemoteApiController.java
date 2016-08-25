/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.InvalidFileNameException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.ProjectHelperRepository;
import org.apache.uima.UIMAException;
import org.apache.wicket.ajax.json.JSONArray;
import org.apache.wicket.ajax.json.JSONObject;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateType;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv3Writer;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2012Writer;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextWriter;

/**
 * Expose some functions of WebAnno via a RESTful remote API.
 *
 */
@Controller
public class RemoteApiController
{
    public static final String MIME_TYPE_XML = "application/xml";
    public static final String PRODUCES_JSON = "application/json";
    public static final String PRODUCES_XML = "application/xml";
    public static final String CONSUMES_URLENCODED = "application/x-www-form-urlencoded";
    public static final String META_INF = "META-INF/";

    @Resource(name = "documentRepository")
    private RepositoryService projectRepository;

    @Resource(name = "annotationService")
    private AnnotationService annotationService;

    @Resource(name = "userRepository")
    private UserDao userRepository;

    private final Log LOG = LogFactory.getLog(getClass());

    /**
     * Create a new project.
     *
     * To test when running in Eclipse, use the Linux "curl" command.
     *
     * curl -v -F 'file=@test.zip' -F 'name=Test' -F 'filetype=tcf'
     * 'http://USERNAME:PASSWORD@localhost:8080/webanno-webapp/api/projects'
     *
     * @param aName
     *            the name of the project to create.
     * @param aFileType
     *            the type of the files contained in the ZIP. The possible file types are configured
     *            in the formats.properties configuration file of WebAnno.
     * @param aFile
     *            a ZIP file containing the project data.
     * @throws Exception if there was an error.
     */
    @RequestMapping(value = "/projects", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)    
    @ResponseBody String  createProject(@RequestParam("file") MultipartFile aFile,
            @RequestParam("name") String aName, @RequestParam("filetype") String aFileType)
        throws Exception
    {
        LOG.info("Creating project [" + aName + "]");

        if (!ZipUtils.isZipStream(aFile.getInputStream())) {
            throw new InvalidFileNameException("", "is an invalid Zip file");
        }

        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        Project project = null;

        // Configure project
        if (!projectRepository.existsProject(aName)) {
            project = new Project();
            project.setName(aName);

            // Create the project and initialize tags
            projectRepository.createProject(project, user);
            annotationService.initializeTypesForProject(project, user);
            // Create permission for this user
            ProjectPermission permission = new ProjectPermission();
            permission.setLevel(PermissionLevel.ADMIN);
            permission.setProject(project);
            permission.setUser(username);
            projectRepository.createProjectPermission(permission);

            permission = new ProjectPermission();
            permission.setLevel(PermissionLevel.USER);
            permission.setProject(project);
            permission.setUser(username);
            projectRepository.createProjectPermission(permission);
        }
        // Existing project
        else {
            throw new IOException("The project with name [" + aName + "] exists");
        }

        // Iterate through all the files in the ZIP

        // If the current filename does not start with "." and is in the root folder of the ZIP,
        // import it as a source document
        File zimpFile = File.createTempFile(aFile.getOriginalFilename(), ".zip");
        aFile.transferTo(zimpFile);
        ZipFile zip = new ZipFile(zimpFile);

        for (Enumeration<?> zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            //
            // Get ZipEntry which is a file or a directory
            //
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // If it is the zip name, ignore it
            if ((FilenameUtils.removeExtension(aFile.getOriginalFilename()) + "/").equals(entry
                    .toString())) {
                continue;
            }
            // IF the current filename is META-INF/webanno/source-meta-data.properties store it
            // as
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

                uploadSourceDocument(zip, entry, project, user, aFileType);
            }

        }
                
        LOG.info("Successfully created project [" + aName + "] for user [" + username + "]");        
        
        JSONObject projectJSON = new JSONObject();
        long pId = projectRepository.getProject(aName).getId();        
        projectJSON.append(aName, pId);
        return projectJSON.toString();
        
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
    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    public @ResponseBody String listProject()
        throws Exception
    {
        List<Project> accessibleProjects;
        JSONObject returnJSONObj = new JSONObject();

        // Get username
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        LOG.info("Fetch project list for [" + username + "]");
        User user = userRepository.get(username);

        // Get projects with permission
        accessibleProjects = projectRepository.listAccessibleProjects();

        // Add permissions for each project into JSON array and store in json
        // object
        for (Project project : accessibleProjects) {
            String projectId = Long.toString(project.getId());
            List<ProjectPermission> projectPermissions = projectRepository
                    .listProjectPermisionLevel(user, project);
            JSONArray permissionArr = new JSONArray();
            JSONObject projectJSON = new JSONObject();

            for (ProjectPermission p : projectPermissions) {
                permissionArr.put(p.getLevel().getName().toString());
            }
            projectJSON.put(project.getName(), permissionArr);
            returnJSONObj.put(projectId, projectJSON);
        }
        return returnJSONObj.toString();
    }

    /**
     * Delete a project where user has a ADMIN role
     * 
     * To test when running in Eclipse, use the Linux "curl" command.
     * 
     * curl -v -X DELETE
     * 'http://USERNAME:PASSOWRD@localhost:8080/webanno-webapp/api/projects/{aProjectId}'
     * 
     * @param aProjectId
     *            The id of the project.
     * @throws Exception
     *             if there was an error.
     */
    @RequestMapping(value = "/projects/{aProjectId}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.NO_CONTENT) void deleteProject(@PathVariable long aProjectId)
        throws Exception
    {
        LOG.info("Deleting project [" + aProjectId + "]");

        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        Project project = projectRepository.getProject(aProjectId);

        // Check for the access
        boolean hasAccess = projectRepository.existsProjectPermissionLevel(user, project,
                PermissionLevel.ADMIN);

        if (hasAccess) {
            // remove project is user has admin access
            projectRepository.removeProject(project, user);
            LOG.info("Successfully deleted project [" + aProjectId + "]");
        }
        else {
            throw new PermissionDeniedDataAccessException(
                    "Not enough permission on project : [" + aProjectId + "]",
                    new Throwable("user:" + username));
        }

    }

    /**
     * Show source documents in given project where user has ADMIN access
     * 
     * http://USERNAME:PASSWORD@localhost:8080/webanno-webapp/api/projects/{aProjectId}/sourcedocs
     * 
     * @param aProjectId
     * @return JSON with {@link SourceDocument} : id
     * @throws Exception
     */

    @RequestMapping(value = "/projects/{aProjectId}/sourcedocs", method = RequestMethod.GET)
    @ResponseBody
    public String showSourceDocuments(@PathVariable long aProjectId)
        throws Exception
    {
        JSONObject sourceDocumentJSON = new JSONObject();
        Project project = projectRepository.getProject(aProjectId);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);

        boolean hasAccess = projectRepository.existsProjectPermissionLevel(user, project,
                PermissionLevel.ADMIN);
        if (hasAccess) {
            List<SourceDocument> srcDocumentList = projectRepository.listSourceDocuments(project);
            for (SourceDocument s : srcDocumentList) {
                sourceDocumentJSON.put(s.getName(), s.getId());
            }
        }
        else {
            throw new PermissionDeniedDataAccessException(
                    "Not enough permission on project : [" + aProjectId + "]",
                    new Throwable("user:" + username));
        }
        return sourceDocumentJSON.toString();
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
     * @throws Exception
     *             if there was an error.
     */
    @RequestMapping(value = "/projects/{aProjectId}/sourcedocs/{aSourceDocumentId}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.NO_CONTENT) void deleteDocument(@PathVariable long aProjectId,
            @PathVariable long aSourceDocumentId)
                throws Exception
    {
        Project project = projectRepository.getProject(aProjectId);
        SourceDocument sourceDocument = projectRepository.getSourceDocument(aProjectId,
                aSourceDocumentId);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        boolean hasAccess = projectRepository.existsProjectPermissionLevel(user, project,
                PermissionLevel.ADMIN);
        LOG.info("Deleting document [" + project.getName() + "]");

        // remove document if hasAccess
        if (hasAccess) {
            projectRepository.removeSourceDocument(sourceDocument);
            LOG.info("Successfully deleted project : [" + aProjectId + "]");
        }
        else {

            throw new PermissionDeniedDataAccessException(
                    "Not enough permission on project : [" + aProjectId + "]",
                    new Throwable("user:" + username));
        }
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
    @RequestMapping(value = "/projects/{aProjectId}/sourcedocs/", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody String uploadDocumentFile(@RequestParam("file") MultipartFile aFile,
            @RequestParam("filetype") String aFileType, @PathVariable long aProjectId)
                throws Exception
    {

        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        JSONObject returnJSON = new JSONObject();
        // Get project
        Project project = projectRepository.getProject(aProjectId);

        // Check for the access
        boolean hasAccess = projectRepository.existsProjectPermissionLevel(user, project,
                PermissionLevel.ADMIN);

        if (hasAccess) {
            // Check if file already present or not
            boolean isDocumentPresent = projectRepository.existsSourceDocument(project,
                    aFile.getOriginalFilename());
            if (!isDocumentPresent) {
                InputStream is = aFile.getInputStream();
                uploadSourceDocumentFile(is,aFile.getOriginalFilename(), project, user, aFileType);
                // add id of added source document in return json string
                returnJSON.put("id", projectRepository.getSourceDocument(project, aFile.getOriginalFilename()).getId());
                is.close();                
            }         
            else {
                throw new IOException("The source document with name ["
                        + aFile.getOriginalFilename() + "] exists");
            }
        }
        else {
            throw new PermissionDeniedDataAccessException(
                    "Not ADMIN permission on project : [" + aProjectId + "]",
                    new Throwable("user:" + username));
        }
        return returnJSON.toString();
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
    @RequestMapping(value = "/projects/{aProjectId}/sourcedocs/{aSourceDocumentId}/annos", method = RequestMethod.GET)
    public @ResponseBody String listAnnotationDocument(@PathVariable long aProjectId,
            @PathVariable long aSourceDocumentId)
                throws Exception
    {
        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        JSONObject returnJSON = new JSONObject();
        // Get project
        Project project = projectRepository.getProject(aProjectId);
        SourceDocument srcDocument = projectRepository.getSourceDocument(aProjectId,
                aSourceDocumentId);

        // Check for the access
        boolean hasAccess = projectRepository.existsProjectPermissionLevel(user, project,
                PermissionLevel.ADMIN);

        if (hasAccess) {
            List<AnnotationDocument> annList = projectRepository
                    .listAllAnnotationDocuments(srcDocument);
            for (AnnotationDocument annDoc : annList) {
                returnJSON.put(annDoc.getUser(),annDoc.getName() );
            }
        }
        else {
            throw new PermissionDeniedDataAccessException(
                    "Not ADMIN permission on project : [" + aProjectId + "]",
                    new Throwable("user:" + username));
        }
        return returnJSON.toString();
    }

    /**
     * Download annotation document with requested parameters
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
     * @param format
     *            Export format.
     * @throws Exception
     *             if there was an error.
     */
    @RequestMapping(value = "/projects/{aProjectId}/sourcedocs/{aSourceDocumentId}/annos/{annotatorName}", method = RequestMethod.GET)
    public void getAnnotationDocument(HttpServletResponse response, @PathVariable long aProjectId,
            @PathVariable Long aSourceDocumentId, @PathVariable String annotatorName,
            @RequestParam(value = "format", required = false) String format)
                throws Exception
    {

        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        Project project = projectRepository.getProject(aProjectId);

        // Check for the access
        boolean hasAdminAccess = projectRepository.existsProjectPermissionLevel(user, project,
                PermissionLevel.ADMIN);

        // if hasAccess and annotator exist
        if (hasAdminAccess && userRepository.exists(annotatorName)) {

            User annotator = userRepository.get(annotatorName);
            // Get source document
            SourceDocument srcDoc = projectRepository.getSourceDocument(aProjectId,
                    aSourceDocumentId);

            // Class writerClassValue = Class.forName(format);

            AnnotationDocument annDoc = projectRepository.getAnnotationDocument(srcDoc, annotator);

            String formatId;
            if (format == null) {
                formatId = srcDoc.getFormat();
            }
            else {
                formatId = format;
            }
            Class<?> writer = projectRepository.getWritableFormats().get(formatId);
            if (writer == null) {
                String msg = "[" + srcDoc.getName() + "] No writer found for format [" + formatId
                        + "] - exporting as WebAnno TSV instead.";
                LOG.info(msg);
                writer = WebannoTsv3Writer.class;
            }

            // Temporary file of annotation document
            File downloadableFile = projectRepository.exportAnnotationDocument(srcDoc,
                    annotatorName, writer, annDoc.getName(), Mode.ANNOTATION);

            try {

                // Set mime type
                String mimeType = URLConnection
                        .guessContentTypeFromName(downloadableFile.getName());
                if (mimeType == null) {
                    LOG.info("mimetype is not detectable, will take default");
                    mimeType = "application/octet-stream";
                }

                // Set response
                response.setContentType(mimeType);
                response.setContentType("application/force-download");
                response.setHeader("Content-Disposition",
                        String.format("inline; filename=\"" + downloadableFile.getName() + "\""));
                response.setContentLength((int) downloadableFile.length());
                InputStream inputStream = new BufferedInputStream(
                        new FileInputStream(downloadableFile));
                FileCopyUtils.copy(inputStream, response.getOutputStream());
            }
            catch (Exception e) {
                LOG.info("Exception occured" + e.getMessage());
            }
            finally {
                if (downloadableFile.exists()) {
                    downloadableFile.delete();
                }
            }

        }
        else {
            throw new PermissionDeniedDataAccessException(
                    "Not enough permission on project : [" + aProjectId
                            + "] or no user defined as [" + annotatorName + "]",
                    new Throwable("user:" + user));
        }
    }

    private void uploadSourceDocumentFile(InputStream is, String name, Project project, User user, String aFileType)
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
            projectRepository.createSourceDocument(document, user);
            // Import source document to the project repository folder
            projectRepository.uploadSourceDocument(is, document);
        }

    }
    private void uploadSourceDocument(ZipFile zip, ZipEntry entry, Project project, User user,
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
        projectRepository.createSourceDocument(document, user);
        // Import source document to the project repository folder
        projectRepository.uploadSourceDocument(zipStream, document);
    }
}
