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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.progressbar.ProgressBar;
import org.wicketstuff.progressbar.Progression;
import org.wicketstuff.progressbar.ProgressionModel;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.ProjectUtil;
import de.tudarmstadt.ukp.clarin.webanno.export.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.export.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.tcf.TcfWriter;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dao.DaoUtils;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.AJAXDownload;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;

/**
 * A Panel used to add Project Guidelines in a selected {@link Project}
 * 
 * @author Seid Muhie Yimam
 * 
 */
@SuppressWarnings("deprecation")
public class ProjectExportPanel
    extends Panel
{
    private static final long serialVersionUID = 2116717853865353733L;

    private static final String META_INF = "/META-INF";
    public static final String EXPORTED_PROJECT = "exportedproject";
    private static final String SOURCE = "/source";
    private static final String CURATION_AS_SERIALISED_CAS = "/curation_ser/";
    private static final String CURATION = "/curation/";
    private static final String LOG = "/log";
    private static final String GUIDELINE = "/guideline";
    private static final String ANNOTATION_AS_SERIALISED_CAS = "/annotation_ser/";
    private static final String ANNOTATION = "/annotation/";

    private static final String CURATION_USER = "CURATION_USER";
    private static final String CORRECTION_USER = "CORRECTION_USER";

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private int progress = 0;
    private ProgressBar Progress;
    AjaxLink<Void> exportProjectLink;

    private String username;
    private String fileName;
    String downloadedFile;
    String projectName;

    private boolean enabled = true;

    public ProjectExportPanel(String id, final Model<Project> aProjectModel)
    {
        super(id);

        username = SecurityContextHolder.getContext().getAuthentication().getName();

        add(new Button("send", new ResourceModel("label"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible()
            {
                return projectRepository.isRemoteProject(aProjectModel.getObject());

            }

            @Override
            public boolean isEnabled()
            {
                return enabled;

            }

            @Override
            public void onSubmit()
            {

                @SuppressWarnings({ "resource" })
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
                        exportCuratedDocuments(aProjectModel.getObject(), exportTempDir);
                        // copy META_INF contents from the project directory to the export folder
                        FileUtils.copyDirectory(new File(projectRepository.getDir(), "/project/"
                                + aProjectModel.getObject().getId() + META_INF), metaInfDir);

                        DaoUtils.zipFolder(exportTempDir, new File(exportTempDir.getAbsolutePath()
                                + ".zip"));

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
                        exportCuratedDocuments(aProjectModel.getObject(), exportTempDir);
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

            @Override
            public boolean isEnabled()
            {
                return enabled;

            }
        }).setOutputMarkupId(true);

        final AJAXDownload exportProject = new AJAXDownload();

        Progress = new ProgressBar("progress", new ProgressionModel()
        {
            private static final long serialVersionUID = 1971929040248482474L;

            @Override
            protected Progression getProgression()
            {
                return new Progression(progress);
            }
        })
        {
            private static final long serialVersionUID = -6599620911784164177L;

            @Override
            protected void onFinished(AjaxRequestTarget target)
            {

                if (!fileName.equals(downloadedFile)) {
                    exportProject.initiate(target, fileName);
                    downloadedFile = fileName;

                    enabled = true;
                    ProjectPage.visible = true;
                    target.add(ProjectPage.projectSelectionForm.setEnabled(true));
                    target.add(ProjectPage.projectDetailForm);
                }

            }
        };

        Progress.add(exportProject);
        add(Progress);

        add(exportProjectLink = new AjaxLink<Void>("exportProject")
        {
            private static final long serialVersionUID = -5758406309688341664L;

            @Override
            public boolean isEnabled()
            {
                return enabled;

            }

            @Override
            public void onClick(final AjaxRequestTarget target)
            {
                enabled = false;
                ProjectPage.projectSelectionForm.setEnabled(false);
                ProjectPage.visible = false;
                target.add(ProjectExportPanel.this.getPage());
                Progress.start(target);

                new Thread()
                {
                    @Override
                    public void run()
                    {
                        File file = null;
                        try {
                            Thread.sleep(200);
                            file = generateZipFile(aProjectModel, target);
                            fileName = file.getAbsolutePath();
                            projectName = aProjectModel.getObject().getName();
                        }
                        catch (UIMAException e) {
                            error(ExceptionUtils.getRootCause(e));
                        }
                        catch (ClassNotFoundException e) {
                            error(e.getMessage());
                        }
                        catch (IOException e) {
                            error(e.getMessage());
                        }
                        catch (WLFormatException e) {
                            error(e.getMessage());
                        }
                        catch (ZippingException e) {
                            error(e.getMessage());
                        }
                        catch (InterruptedException e) {
                        }
                    }
                }.start();
            }

        });

    }

    public File generateZipFile(final Model<Project> aProjectModel, AjaxRequestTarget target)
        throws IOException, UIMAException, ClassNotFoundException, WLFormatException,
        ZippingException, InterruptedException
    {
        File exportTempDir = null;
        // all metadata and project settings data from the database as JSON file
        File projectSettings = null;
        projectSettings = File.createTempFile(EXPORTED_PROJECT, ".json");
        // Directory to store source documents and annotation documents
        exportTempDir = File.createTempFile("webanno-project", "export");
        exportTempDir.delete();
        exportTempDir.mkdirs();
        if (aProjectModel.getObject().getId() == 0) {
            error("Project not yet created. Please save project details first!");
        }
        else {

            exportProjectSettings(aProjectModel.getObject(), projectSettings, exportTempDir);
            exportSourceDocuments(aProjectModel.getObject(), exportTempDir);
            progress = 20;
            exportAnnotationDocuments(aProjectModel.getObject(), exportTempDir);
            progress = progress + 1;
            exportProjectLog(aProjectModel.getObject(), exportTempDir);
            progress = progress + 1;
            exportGuideLine(aProjectModel.getObject(), exportTempDir);
            progress = progress + 1;
            exportProjectMetaInf(aProjectModel.getObject(), exportTempDir);
            progress = 90;
            exportCuratedDocuments(aProjectModel.getObject(), exportTempDir);
            try {
                DaoUtils.zipFolder(exportTempDir,
                        new File(exportTempDir.getAbsolutePath() + ".zip"));
            }
            catch (Exception e) {
                throw new ZippingException("Unable to Zipp the file");
            }
            progress = 100;
        }
        return new File(exportTempDir.getAbsolutePath() + ".zip");
    }

    /**
     * Copy source documents from the file system of this project to the export folder
     */
    private void exportSourceDocuments(Project aProject, File aCopyDir)
        throws IOException
    {
        File sourceDocumentDir = new File(aCopyDir + SOURCE);
        FileUtils.forceMkdir(sourceDocumentDir);
        // Get all the source documents from the project
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = projectRepository
                .listSourceDocuments(aProject);

        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
            FileUtils.copyFileToDirectory(projectRepository.exportSourceDocument(sourceDocument),
                    sourceDocumentDir);
        }
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
    private void exportCuratedDocuments(Project aProject, File aCopyDir)
        throws FileNotFoundException, UIMAException, IOException, WLFormatException,
        ClassNotFoundException
    {

        // Get all the source documents from the project
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = projectRepository
                .listSourceDocuments(aProject);

        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {

            File curationCasDir = new File(aCopyDir + CURATION_AS_SERIALISED_CAS
                    + sourceDocument.getName());
            FileUtils.forceMkdir(curationCasDir);

            File curationDir = new File(aCopyDir + CURATION + sourceDocument.getName());
            FileUtils.forceMkdir(curationDir);

            // If the curation document is exist (either finished or in progress
            if (sourceDocument.getState().equals(SourceDocumentState.CURATION_FINISHED)
                    || sourceDocument.getState().equals(SourceDocumentState.CURATION_IN_PROGRESS)) {

                File CurationFileAsSerialisedCas = projectRepository.exportserializedCas(
                        sourceDocument, CURATION_USER);
                File curationFile = null;
                if (CurationFileAsSerialisedCas.exists()) {
                    curationFile = projectRepository.exportAnnotationDocument(sourceDocument,
                            username, TcfWriter.class, sourceDocument.getName(), Mode.CURATION);
                }
                // in Case they didn't exist
                if (CurationFileAsSerialisedCas.exists()) {
                    FileUtils.copyFileToDirectory(curationFile, curationDir);
                    FileUtils.copyFileToDirectory(CurationFileAsSerialisedCas, curationCasDir);
                }
            }

            // If this project is a correction project, add the auto-annotated CAS to same folder as
            // CURATION
            if (aProject.getMode().equals(Mode.CORRECTION)) {
                File CorrectionFileAsSerialisedCas = projectRepository.exportserializedCas(
                        sourceDocument, CORRECTION_USER);
                File correctionFile = null;
                if (CorrectionFileAsSerialisedCas.exists()) {
                    correctionFile = projectRepository.exportAnnotationDocument(sourceDocument,
                            username, TcfWriter.class, sourceDocument.getName(), Mode.CORRECTION);
                }
                // in Case they didn't exist
                if (CorrectionFileAsSerialisedCas.exists()) {
                    FileUtils.copyFileToDirectory(correctionFile, curationDir);
                    FileUtils.copyFileToDirectory(CorrectionFileAsSerialisedCas, curationCasDir);
                }
            }
        }
    }

    /**
     * Copy Project logs from the file system of this project to the export folder
     */
    private void exportProjectLog(Project aProject, File aCopyDir)
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
    private void exportGuideLine(Project aProject, File aCopyDir)
        throws IOException
    {
        File guidelineDir = new File(aCopyDir + GUIDELINE);
        FileUtils.forceMkdir(guidelineDir);
        File annotationGuidlines = projectRepository.exportGuidelines(aProject);
        if (annotationGuidlines.exists()) {
            for (File annotationGuideline : annotationGuidlines.listFiles()) {
                FileUtils.copyFileToDirectory(annotationGuideline, guidelineDir);
            }
        }
    }

    /**
     * Copy Project guidelines from the file system of this project to the export folder
     */
    private void exportProjectMetaInf(Project aProject, File aCopyDir)
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
    private void exportAnnotationDocuments(Project aProject, File aCopyDir)
        throws IOException, UIMAException, WLFormatException, ClassNotFoundException
    {
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = projectRepository
                .listSourceDocuments(aProject);
        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
            for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : projectRepository
                    .listAnnotationDocuments(sourceDocument)) {

                // copy annotation document only for ACTIVE users and the state of the annotation
                // document
                // is not NEW/IGNOR
                if (userRepository.get(annotationDocument.getUser()) != null
                        && !annotationDocument.getState().equals(AnnotationDocumentState.NEW)
                        && !annotationDocument.getState().equals(AnnotationDocumentState.IGNORE)) {
                    File annotationDocumentAsSerialisedCasDir = new File(aCopyDir.getAbsolutePath()
                            + ANNOTATION_AS_SERIALISED_CAS + sourceDocument.getName());
                    File annotationDocumentDir = new File(aCopyDir.getAbsolutePath() + ANNOTATION
                            + sourceDocument.getName());

                    FileUtils.forceMkdir(annotationDocumentAsSerialisedCasDir);
                    FileUtils.forceMkdir(annotationDocumentDir);

                    File annotationFileAsSerialisedCas = projectRepository.exportserializedCas(
                            sourceDocument, annotationDocument.getUser());

                    File annotationFile = null;
                    if (annotationFileAsSerialisedCas.exists()) {
                        Class<?> writer = projectRepository.getWritableFormats().get(
                                sourceDocument.getFormat());
                        annotationFile = projectRepository.exportAnnotationDocument(sourceDocument,
                                annotationDocument.getUser(), writer, sourceDocument.getName(),
                                Mode.ANNOTATION);
                    }
                    if (annotationFileAsSerialisedCas.exists()) {
                        FileUtils.copyFileToDirectory(annotationFileAsSerialisedCas,
                                annotationDocumentAsSerialisedCasDir);
                        FileUtils.copyFileToDirectory(annotationFile, annotationDocumentDir);

                    }
                }
            }
            progress = progress + 1;
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

    private void exportProjectSettings(Project aProject, File aProjectSettings, File aExportTempDir)
    {
        de.tudarmstadt.ukp.clarin.webanno.export.model.Project project = new de.tudarmstadt.ukp.clarin.webanno.export.model.Project();
        project.setDescription(aProject.getDescription());
        project.setName(aProject.getName());
        project.setMode(aProject.getMode());

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
                    .listAnnotationDocuments(sourceDocument)) {
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
        ProjectUtil.setJsonConverter(jsonConverter);

        try {
            ProjectUtil.generateJson(project, aProjectSettings);
            FileUtils.copyFileToDirectory(aProjectSettings, aExportTempDir);
        }
        catch (IOException e) {
            error("File Path not found or No permision to save the file!");
        }
    }
}
