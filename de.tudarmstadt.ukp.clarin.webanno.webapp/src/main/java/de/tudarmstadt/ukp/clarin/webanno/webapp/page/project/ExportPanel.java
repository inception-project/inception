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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.export.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.export.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.tcf.TcfWriter;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dao.DaoUtils;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;

/**
 * A Panel used to add Project Guidelines in a selected {@link Project}
 *
 * @author Seid Muhie Yimam
 *
 */
public class ExportPanel
    extends Panel
{
    private static final long serialVersionUID = 2116717853865353733L;

    private static final String META_INF = "/META-INF";
    private static final String EXPORTED_PROJECT = "exportedproject";
    private static final String SOURCE = "/source";
    private static final String CURATION_AS_SERIALISED_CAS = "/curation_ser";
    private static final String CURATION = "/curation";
    private static final String LOG = "/log";
    private static final String GUIDELINE = "/guideline";
    private static final String ANNOTATION_AS_SERIALISED_CAS = "/annotation_ser/";
    private static final String ANNOTATION = "/annotation/";

    private static final String CURATION_USER = "CURATION_USER";

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private FileUploadField fileUpload;
    private FileUpload uploadedFile;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ExportPanel(String id, final Model<Project> aProjectModel)
    {
        super(id);
        // final Project project = aProjectModel.getObject();

        add(new Button("send", new ResourceModel("label"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible()
            {
                return projectRepository.isRemoteProject(aProjectModel.getObject());

            }

            @Override
            public void onSubmit()
            {

                HttpClient httpclient = new DefaultHttpClient();
                try {
                    HttpPost httppost = new HttpPost(
                            "http://aspra11.informatik.uni-leipzig.de:8080/"
                                    + "TEI-Integration/collection/addAnnotations?user=pws&pass=showcase");

                    File exportTempDir = File.createTempFile("webanno", "export");
                    exportTempDir.delete();
                    exportTempDir.mkdirs();

                    File metaInfDir = new File(exportTempDir + META_INF);
                    FileUtils.forceMkdir(metaInfDir);

                    boolean curationDocumentExist = isCurationDocumentExists(aProjectModel
                            .getObject());

                    if (!curationDocumentExist) {
                        error("No curation document created yet for this document");
                    }
                    else {
                        // copy curated documents into the export folder
                        copyCuratedDocuments(aProjectModel.getObject(), exportTempDir);
                        // copy META_INF contents from the project directory to the export folder
                        FileUtils.copyDirectory(new File(projectRepository.getDir(), "/project/"
                                + aProjectModel.getObject().getId() + META_INF), metaInfDir);

                        DaoUtils.zipFolder(exportTempDir, new File(exportTempDir.getAbsolutePath()
                                + ".zip"));

                        @SuppressWarnings("deprecation")
                        FileEntity reqEntity = new FileEntity(new File(exportTempDir
                                .getAbsolutePath() + ".zip"), "application/octet-stream");

                        httppost.setEntity(reqEntity);

                        HttpResponse response = httpclient.execute(httppost);
                        HttpEntity resEntity = response.getEntity();

                        info(response.getStatusLine().toString());
                        EntityUtils.consume(resEntity);
                    }
                }
                catch (ClientProtocolException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (WLFormatException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (Exception e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                finally {
                    try {
                        httpclient.getConnectionManager().shutdown();
                    }
                    catch (Exception e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                }

            }
        }).setOutputMarkupId(true);

        add(new DownloadLink("export", new LoadableDetachableModel<File>()
        {
            private static final long serialVersionUID = 840863954694163375L;

            @Override
            protected File load()
            {
                File exportFile = null;
                try {
                    File exportTempDir = File.createTempFile("webanno", "export");
                    exportTempDir.delete();
                    exportTempDir.mkdirs();

                    boolean curationDocumentExist = isCurationDocumentExists(aProjectModel
                            .getObject());

                    if (!curationDocumentExist) {
                        error("No curation document created yet for this document");
                    }
                    else {
                        copyCuratedDocuments(aProjectModel.getObject(), exportTempDir);
                        DaoUtils.zipFolder(exportTempDir, new File(exportTempDir.getAbsolutePath()
                                + ".zip"));
                        exportFile = new File(exportTempDir.getAbsolutePath() + ".zip");
                    }
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                catch (Exception e) {
                    error(e.getMessage());
                }

                return exportFile;
            }
        })
        {
            private static final long serialVersionUID = 5630612543039605914L;

            @Override
            public boolean isVisible()
            {
                return isCurationDocumentExists(aProjectModel.getObject());

            }
        }).setOutputMarkupId(true);

        add(new DownloadLink("exportProject", new LoadableDetachableModel<File>()
        {
            private static final long serialVersionUID = 840863954694163375L;

            @Override
            protected File load()
            {
                File exportTempDir = null;
                // all metadata and project settings data from the database as JSON file
                File projectSettings = null;
                try {
                    projectSettings = File.createTempFile(EXPORTED_PROJECT, ".json");
                    // Directory to store source documents and annotation documents
                    exportTempDir = File.createTempFile("webanno-project", "export");
                    exportTempDir.delete();
                    exportTempDir.mkdirs();
                }
                catch (IOException e1) {
                    error("Unable to create temporary File!!");

                }
                if (aProjectModel.getObject().getId() == 0) {
                    error("Project not yet created. Please save project details first!");
                }
                else {
                    try {
                        copyProjectSettings(aProjectModel.getObject(), projectSettings,
                                exportTempDir);
                        copySourceDocuments(aProjectModel.getObject(), exportTempDir);
                        copyAnnotationDocuments(aProjectModel.getObject(), exportTempDir);
                        copyProjectLog(aProjectModel.getObject(), exportTempDir);
                        copyGuideLine(aProjectModel.getObject(), exportTempDir);
                        copyProjectMetaInf(aProjectModel.getObject(), exportTempDir);
                        copyCuratedDocuments(aProjectModel.getObject(), exportTempDir);
                        DaoUtils.zipFolder(exportTempDir, new File(exportTempDir.getAbsolutePath()
                                + ".zip"));
                    }
                    catch (Exception e) {
                        info(e.getMessage());
                    }
                }
                return new File(exportTempDir.getAbsolutePath() + ".zip");
            }
        }).setOutputMarkupId(true));

        add(fileUpload = new FileUploadField("content", new Model()));

        add(new Button("importProject", new ResourceModel("label"))
        {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit()
            {
                uploadedFile = fileUpload.getFileUpload();
                if (uploadedFile != null) {
                    try {
                        if (ApplicationUtils.isZipStream(uploadedFile.getInputStream())) {
                            File file = uploadedFile.writeToTempFile();
                            ZipFile zip = new ZipFile(file);
                            InputStream projectInputStream = null;
                            for (Enumeration zipEnumerate = zip.entries(); zipEnumerate
                                    .hasMoreElements();) {
                                ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
                                if (entry.toString().replace("/", "").startsWith(EXPORTED_PROJECT)
                                        && entry.toString().replace("/", "").endsWith(".json")) {
                                    projectInputStream = zip.getInputStream(entry);
                                    break;
                                }
                            }

                            // projectInputStream = uploadedFile.getInputStream();
                            String text = IOUtils.toString(projectInputStream, "UTF-8");
                            MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
                            de.tudarmstadt.ukp.clarin.webanno.export.model.Project importedProjectSetting = jsonConverter
                                    .getObjectMapper()
                                    .readValue(
                                            text,
                                            de.tudarmstadt.ukp.clarin.webanno.export.model.Project.class);
                            if (projectRepository.existsProject(importedProjectSetting.getName())) {
                                error("Project already exist");
                            }
                            else {
                                Project importedProject = createProject(importedProjectSetting);
                                createSourceDocument(importedProjectSetting, importedProject);
                                createAnnotationDocument(importedProjectSetting, importedProject);
                                createProjectPermission(importedProjectSetting, importedProject);
                                for (TagSet tagset : importedProjectSetting.getTagSets()) {
                                    createTagset(importedProject, tagset);
                                }
                                // add source document content
                                createSourceDocumentContent(zip, importedProject);
                                // add annotation document content
                                createAnnotationDocumentContent(zip, importedProject);
                                // create curation document content
                                createCurationDocumentContent(zip, importedProject);
                                // create project log
                                createProjectLog(zip, importedProject);
                                // create project guideline
                                createProjectGuideline(zip, importedProject);
                                // cretae project META-INF
                                createProjectMetaInf(zip, importedProject);

                            }
                        }
                        else {
                            error("Invalid ZIP file");
                        }
                    }
                    catch (IOException e) {
                        error("Error Importing Project " + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
                else if (uploadedFile == null) {
                    error("Please choose appropriate project in zip format");
                }
            }
        });
    }

    /**
     * Copy, if exists, curation documents to a folder that will be exported as Zip file
     *
     * @param aProject
     *            The {@link Project}
     * @param aCurationDocumentExist
     *            Check if Curation document exists
     * @param aCopyDir
     *            The folder where curated documents are copied to be exported as Zip File
     */
    private void copyCuratedDocuments(Project aProject, File aCopyDir)
        throws FileNotFoundException, UIMAException, IOException, WLFormatException,
        ClassNotFoundException
    {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = projectRepository.getUser(username);

        // Get all the source documents from the project
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = projectRepository
                .listSourceDocuments(aProject);

        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {

            File curationCasDir = new File(aCopyDir + CURATION_AS_SERIALISED_CAS+sourceDocument.getName());
            FileUtils.forceMkdir(curationCasDir);

            File curationDir = new File(aCopyDir + CURATION+sourceDocument.getName());
            FileUtils.forceMkdir(curationDir);

            // If the curation document is exist (either finished or in progress
            if (sourceDocument.getState().equals(SourceDocumentState.CURATION_FINISHED)
                    || sourceDocument.getState().equals(SourceDocumentState.CURATION_IN_PROGRESS)) {

                File CurationFileAsSerialisedCas = projectRepository.exportAnnotationDocument(
                        sourceDocument, aProject, CURATION_USER);
                File curationFile = projectRepository.exportAnnotationDocument(sourceDocument,
                        aProject, user, TcfWriter.class,
                        sourceDocument.getName(), Mode.CURATION);

                FileUtils.copyFileToDirectory(curationFile, curationDir);
                FileUtils.copyFileToDirectory(CurationFileAsSerialisedCas, curationCasDir);
            }
        }
    }

    /**
     * Copy source documents from the file system of this project to the export folder
     */
    private void copySourceDocuments(Project aProject, File aCopyDir)
        throws IOException
    {
        File sourceDocumentDir = new File(aCopyDir + SOURCE);
        FileUtils.forceMkdir(sourceDocumentDir);
        // Get all the source documents from the project
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = projectRepository
                .listSourceDocuments(aProject);

        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
            FileUtils.copyFileToDirectory(
                    projectRepository.exportSourceDocument(sourceDocument, aProject),
                    sourceDocumentDir);
        }
    }

    /**
     * Copy Project logs from the file system of this project to the export folder
     */
    private void copyProjectLog(Project aProject, File aCopyDir)
        throws IOException
    {
        File logDir = new File(aCopyDir + LOG);
        FileUtils.forceMkdir(logDir);
        if (projectRepository.exportProjectLog(aProject).exists()) {
            FileUtils.copyFileToDirectory(projectRepository.exportProjectLog(aProject), logDir);

        }
    }

    /**
     * Copy Project guidelines from the file system of this project to the export folder
     */
    private void copyGuideLine(Project aProject, File aCopyDir)
        throws IOException
    {
        File guidelineDir = new File(aCopyDir + GUIDELINE);
        FileUtils.forceMkdir(guidelineDir);
        File annotationGuidlines = projectRepository.exportGuideLines(aProject);
        if (annotationGuidlines.exists()) {
            for (File annotationGuideline : annotationGuidlines.listFiles()) {
                FileUtils.copyFileToDirectory(annotationGuideline, guidelineDir);
            }
        }
    }

    /**
     * Copy Project guidelines from the file system of this project to the export folder
     */
    private void copyProjectMetaInf(Project aProject, File aCopyDir)
        throws IOException
    {
        File metaInfDir = new File(aCopyDir + META_INF);
        FileUtils.forceMkdir(metaInfDir);
        File metaInf = projectRepository.exportProjectMetaInf(aProject);
        if (metaInf.exists()) {
            FileUtils.copyDirectory(metaInf, metaInfDir);
        }

    }

    /**
     * Copy annotation document as Serialized CAS from the file system of this project to the export
     * folder
     *
     * @throws ClassNotFoundException
     * @throws WLFormatException
     * @throws UIMAException
     */
    private void copyAnnotationDocuments(Project aProject, File aCopyDir)
        throws IOException, UIMAException, WLFormatException, ClassNotFoundException
    {
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = projectRepository
                .listSourceDocuments(aProject);

        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
            for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : projectRepository
                    .listAnnotationDocument(sourceDocument)) {

                File annotationDocumentAsSerialisedCasDir = new File(aCopyDir.getAbsolutePath()
                        + ANNOTATION_AS_SERIALISED_CAS + sourceDocument.getName());
                File annotationDocumentDir = new File(aCopyDir.getAbsolutePath() + ANNOTATION
                        + sourceDocument.getName());

                FileUtils.forceMkdir(annotationDocumentAsSerialisedCasDir);
                FileUtils.forceMkdir(annotationDocumentDir);

                File annotationFileAsSerialisedCas = projectRepository.exportAnnotationDocument(
                        sourceDocument, aProject, annotationDocument.getUser());
                Class writer = projectRepository.getWritableFormats().get(
                        sourceDocument.getFormat());
                File annotationFile = projectRepository.exportAnnotationDocument(sourceDocument,
                        aProject, projectRepository.getUser(annotationDocument.getUser()), writer,
                        sourceDocument.getName(), Mode.ANNOTATION);
                if (annotationFileAsSerialisedCas.exists()) {
                    FileUtils.copyFileToDirectory(annotationFileAsSerialisedCas,
                            annotationDocumentAsSerialisedCasDir);
                    FileUtils.copyFileToDirectory(annotationFile, annotationDocumentDir);

                }
            }

        }

    }

    private boolean isCurationDocumentExists(Project aProject)
    {
        boolean curationDocumentExist = false;
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = projectRepository
                .listSourceDocuments(aProject);

        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {

            // If the curation document is exist (either finished or in progress
            if (sourceDocument.getState().equals(SourceDocumentState.CURATION_FINISHED)
                    || sourceDocument.getState().equals(SourceDocumentState.CURATION_IN_PROGRESS)) {
                curationDocumentExist = true;
                break;
            }
        }
        return curationDocumentExist;
    }

    private void copyProjectSettings(Project aProject, File aProjectSettings, File aExportTempDir)
    {
        de.tudarmstadt.ukp.clarin.webanno.export.model.Project project = new de.tudarmstadt.ukp.clarin.webanno.export.model.Project();
        project.setDescription(aProject.getDescription());
        project.setName(aProject.getName());
        project.setReverse(aProject.isReverseDependencyDirection());

        List<TagSet> tagsets = new ArrayList<TagSet>();
        // add TagSets to the project
        for (de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet : annotationService
                .listTagSets(aProject)) {
            TagSet exportedTagSetContent = new TagSet();
            exportedTagSetContent.setDescription(tagSet.getDescription());
            exportedTagSetContent.setLanguage(tagSet.getLanguage());
            exportedTagSetContent.setName(tagSet.getName());

            exportedTagSetContent.setType(tagSet.getType().getType());
            exportedTagSetContent.setTypeName(tagSet.getType().getName());
            exportedTagSetContent.setTypeDescription(tagSet.getType().getDescription());

            List<de.tudarmstadt.ukp.clarin.webanno.export.model.Tag> exportedTags = new ArrayList<de.tudarmstadt.ukp.clarin.webanno.export.model.Tag>();
            for (Tag tag : annotationService.listTags(tagSet)) {
                de.tudarmstadt.ukp.clarin.webanno.export.model.Tag exportedTag = new de.tudarmstadt.ukp.clarin.webanno.export.model.Tag();
                exportedTag.setDescription(tag.getDescription());
                exportedTag.setName(tag.getName());
                exportedTags.add(exportedTag);
            }
            exportedTagSetContent.setTags(exportedTags);
            tagsets.add(exportedTagSetContent);
        }

        project.setTagSets(tagsets);

        List<SourceDocument> sourceDocuments = new ArrayList<SourceDocument>();
        List<AnnotationDocument> annotationDocuments = new ArrayList<AnnotationDocument>();

        // add source documents to a project
        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : projectRepository
                .listSourceDocuments(aProject)) {
            SourceDocument sourceDocumentToExport = new SourceDocument();
            sourceDocumentToExport.setFormat(sourceDocument.getFormat());
            sourceDocumentToExport.setName(sourceDocument.getName());
            sourceDocumentToExport.setState(sourceDocument.getState());

            // add annotation document to Project
            for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : projectRepository
                    .listAnnotationDocument(sourceDocument)) {
                AnnotationDocument annotationDocumentToExport = new AnnotationDocument();
                annotationDocumentToExport.setName(annotationDocument.getName());
                annotationDocumentToExport.setState(annotationDocument.getState());
                annotationDocumentToExport.setUser(annotationDocument.getUser());
                annotationDocuments.add(annotationDocumentToExport);
            }
            sourceDocuments.add(sourceDocumentToExport);
        }
        project.setSourceDocuments(sourceDocuments);
        project.setAnnotationDocuments(annotationDocuments);

        List<ProjectPermission> projectPermissions = new ArrayList<ProjectPermission>();

        // add project permissions to the project
        for (User user : projectRepository.listProjectUsersWithPermissions(aProject)) {
            for (de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission permission : projectRepository
                    .listProjectPermisionLevel(user, aProject)) {
                ProjectPermission permissionToExport = new ProjectPermission();
                permissionToExport.setLevel(permission.getLevel());
                permissionToExport.setUser(user.getUsername());
                projectPermissions.add(permissionToExport);
            }
        }

        project.setProjectPermissions(projectPermissions);

        MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
        ApplicationUtils.setJsonConverter(jsonConverter);

        try {
            ApplicationUtils.generateJson(project, aProjectSettings);
            FileUtils.copyFileToDirectory(aProjectSettings, aExportTempDir);
        }
        catch (IOException e) {
            error("File Path not found or No permision to save the file!");
        }
    }

    private void createTagset(Project aProjecct, TagSet importedTagSet)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = projectRepository.getUser(username);
        AnnotationType type = null;
        if (!annotationService.existsType(importedTagSet.getTypeName(), importedTagSet.getType())) {
            type = new AnnotationType();
            type.setDescription(importedTagSet.getTypeDescription());
            type.setName(importedTagSet.getTypeName());
            type.setType(importedTagSet.getType());
            annotationService.createType(type);
        }
        else {
            type = annotationService
                    .getType(importedTagSet.getTypeName(), importedTagSet.getType());
        }

        if (importedTagSet != null) {

            de.tudarmstadt.ukp.clarin.webanno.model.TagSet newTagSet = new de.tudarmstadt.ukp.clarin.webanno.model.TagSet();
            newTagSet.setDescription(importedTagSet.getDescription());
            newTagSet.setName(importedTagSet.getName());
            newTagSet.setLanguage(importedTagSet.getLanguage());
            newTagSet.setProject(aProjecct);
            newTagSet.setType(type);
            annotationService.createTagSet(newTagSet, user);
            for (de.tudarmstadt.ukp.clarin.webanno.export.model.Tag tag : importedTagSet.getTags()) {
                Tag newTag = new Tag();
                newTag.setDescription(tag.getDescription());
                newTag.setName(tag.getName());
                newTag.setTagSet(newTagSet);
                annotationService.createTag(newTag, user);
            }
        }
    }

    private Project createProject(de.tudarmstadt.ukp.clarin.webanno.export.model.Project aProject)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = projectRepository.getUser(username);
        Project project = new Project();
        project.setName(aProject.getName());
        project.setDescription(aProject.getDescription());
        project.setReverseDependencyDirection(aProject.isReverse());
        projectRepository.createProject(project, user);
        return project;
    }

    private void createSourceDocument(
            de.tudarmstadt.ukp.clarin.webanno.export.model.Project aImportedProjectSetting,
            Project aImportedProject)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = projectRepository.getUser(username);
        for (SourceDocument importedSourceDocument : aImportedProjectSetting.getSourceDocuments()) {
            de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = new de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument();
            sourceDocument.setFormat(importedSourceDocument.getFormat());
            sourceDocument.setName(importedSourceDocument.getName());
            sourceDocument.setState(importedSourceDocument.getState());
            sourceDocument.setProject(aImportedProject);
            projectRepository.createSourceDocument(sourceDocument, user);
        }
    }

    private void createAnnotationDocument(
            de.tudarmstadt.ukp.clarin.webanno.export.model.Project aImportedProjectSetting,
            Project aImportedProject)
        throws IOException
    {
        for (AnnotationDocument importedAnnotationDocument : aImportedProjectSetting
                .getAnnotationDocuments()) {
            de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument = new de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument();
            annotationDocument.setName(importedAnnotationDocument.getName());
            annotationDocument.setState(importedAnnotationDocument.getState());
            annotationDocument.setProject(aImportedProject);
            annotationDocument.setUser(importedAnnotationDocument.getUser());
            annotationDocument.setDocument(projectRepository.getSourceDocument(
                    importedAnnotationDocument.getName(), aImportedProject));
            projectRepository.createAnnotationDocument(annotationDocument);
        }
    }

    private void createProjectPermission(
            de.tudarmstadt.ukp.clarin.webanno.export.model.Project aImportedProjectSetting,
            Project aImportedProject)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = projectRepository.getUser(username);
        for (ProjectPermission importedPermission : aImportedProjectSetting.getProjectPermissions()) {
            de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission permission = new de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission();
            permission.setLevel(importedPermission.getLevel());
            permission.setProject(aImportedProject);
            permission.setUser(importedPermission.getUser());
            projectRepository.createProjectPermission(permission);
        }
    }

    private void createSourceDocumentContent(ZipFile zip, Project aProject)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().startsWith(SOURCE)) {
                String fileName = entry.toString().replace(SOURCE, "").replace("/", "");
                de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = projectRepository
                        .getSourceDocument(fileName, aProject);
                File sourceFilePath = projectRepository.exportSourceDocument(sourceDocument,
                        aProject);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), sourceFilePath);
            }
        }
    }

    private void createAnnotationDocumentContent(ZipFile zip, Project aProject)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().startsWith(ANNOTATION_AS_SERIALISED_CAS)) {
                String fileName = entry.toString().replace(ANNOTATION_AS_SERIALISED_CAS, "");

                // the user annotated the document is file name minus extension (anno1.ser)
                String username = FilenameUtils.getBaseName(fileName).replace(".ser", "");

                // name of the annotation document
                fileName = fileName.replace(FilenameUtils.getName(fileName), "").replace("/", "");
                de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = projectRepository
                        .getSourceDocument(fileName, aProject);
                File annotationFilePath = projectRepository.exportAnnotationDocument(
                        sourceDocument, aProject, username);

                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), annotationFilePath);
            }
        }
    }

    private void createCurationDocumentContent(ZipFile zip, Project aProject)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().startsWith(CURATION_AS_SERIALISED_CAS)) {
                String fileName = entry.toString().replace(CURATION_AS_SERIALISED_CAS, "");

                // the user annotated the document is file name minus extension (anno1.ser)
                String username = FilenameUtils.getBaseName(fileName).replace(".ser", "");

                // name of the annotation document
                fileName = fileName.replace(FilenameUtils.getName(fileName), "").replace("/", "");
                de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = projectRepository
                        .getSourceDocument(fileName, aProject);
                File annotationFilePath = projectRepository.exportAnnotationDocument(
                        sourceDocument, aProject, username);

                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), annotationFilePath);
            }
        }
    }

    private void createProjectGuideline(ZipFile zip, Project aProject)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().startsWith(GUIDELINE)) {
                File guidelineDir = projectRepository.exportGuideLines(aProject);
                FileUtils.forceMkdir(guidelineDir);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), new File(guidelineDir,
                        FilenameUtils.getName(entry.getName())));
            }
        }
    }

    private void createProjectMetaInf(ZipFile zip, Project aProject)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().startsWith(META_INF)) {
                File metaInfDir = new File(projectRepository.exportProjectMetaInf(aProject),
                        FilenameUtils.getPath(entry.getName().replace(META_INF, "")));
                // where the file reside in the META-INF/... directory
                FileUtils.forceMkdir(metaInfDir);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), new File(metaInfDir,
                        FilenameUtils.getName(entry.getName())));
            }
        }
    }

    private void createProjectLog(ZipFile zip, Project aProject)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().startsWith(LOG)) {
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry),
                        projectRepository.exportProjectLog(aProject));
            }
        }
    }
}
