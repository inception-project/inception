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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectLifecycleAware;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectLifecycleAwareRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;

public class ProjectImportPanel
    extends Panel
{
    private static final long serialVersionUID = 4612767288793876015L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectImportPanel.class);
    
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AutomationService automationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectLifecycleAwareRegistry projectLifecycleAwareRegistry;
    
    private IModel<Project> selectedModel;
    private IModel<Preferences> preferences;
    private FileUploadField fileUpload;

    public ProjectImportPanel(String aId, IModel<Project> aModel)
    {
        super(aId);
        
        preferences = Model.of(new Preferences());
        selectedModel = aModel;
        
        Form<Preferences> form = new Form<>("form", CompoundPropertyModel.of(preferences));
        form.add(new CheckBox("generateUsers"));
        form.add(fileUpload = new FileUploadField("content"));
        fileUpload.setRequired(true);
        form.add(new LambdaAjaxButton<>("import", this::actionImport));
        add(form);
    }

    private void actionImport(AjaxRequestTarget aTarget, Form<Preferences> aForm)
    {
        List<FileUpload> exportedProjects = fileUpload.getFileUploads();
        boolean aGenerateUsers = preferences.getObject().generateUsers;
        // import multiple projects!
        Project importedProject = null;
        for (FileUpload exportedProject : exportedProjects) {
            try {
                // Workaround for WICKET-6425
                File tempFile = File.createTempFile("webanno-training", null);
                try (
                        InputStream is = exportedProject.getInputStream();
                        OutputStream os = new FileOutputStream(tempFile);
                ) {
                    if (!ZipUtils.isZipStream(is)) {
                        error("Invalid ZIP file");
                        return;
                    }
                    IOUtils.copyLarge(is, os);
                    
                    if (!ImportUtil.isZipValidWebanno(tempFile)) {
                        error("Incompatible to webanno ZIP file");
                    }
                    
                    importedProject = importProject(tempFile, aGenerateUsers);
                }
                finally {
                    tempFile.delete();
                }
            }
            catch (Exception e) {
                error("Error Importing Project " + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Error importing project", e);
            }
        }
        
        if (importedProject != null) {
            selectedModel.setObject(importedProject);
            aTarget.add(getPage());
        }
    }
    
    private Project importProject(File aProjectFile, boolean aGenerateUsers) throws Exception
    {
        Project importedProject = new Project();
        ZipFile zip = new ZipFile(aProjectFile);
        InputStream projectInputStream = null;
        for (Enumeration<? extends ZipEntry> zipEnumerate = zip.entries(); zipEnumerate
                .hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().replace("/", "").startsWith(ImportUtil.EXPORTED_PROJECT)
                    && entry.toString().replace("/", "").endsWith(".json")) {
                projectInputStream = zip.getInputStream(entry);
                break;
            }
        }

        // Load the project model from the JSON file
        String text = IOUtils.toString(projectInputStream, "UTF-8");
        de.tudarmstadt.ukp.clarin.webanno.export.model.Project importedProjectSetting = JSONUtil
                .getJsonConverter().getObjectMapper()
                .readValue(text, de.tudarmstadt.ukp.clarin.webanno.export.model.Project.class);

        // Import the project itself
        importedProject = ImportUtil.createProject(importedProjectSetting, projectService);

        // Import additional project things
        projectService.onProjectImport(zip, importedProjectSetting, importedProject);

        // Import missing users
        if (aGenerateUsers) {
            ImportUtil.createMissingUsers(importedProjectSetting, userRepository);
        }

        // Notify all relevant service so that they can initialize themselves for the given
        // project
        for (ProjectLifecycleAware bean : projectLifecycleAwareRegistry.getBeans()) {
            try {
                bean.onProjectImport(zip, importedProjectSetting, importedProject);
            }
            catch (IOException e) {
                throw e;
            }
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        // Import layers
        Map<String, AnnotationFeature> featuresMap = ImportUtil.createLayer(importedProject,
                importedProjectSetting, userRepository, annotationService);
        /*
         * for (TagSet tagset : importedProjectSetting.getTagSets()) {
         * ImportUtil.createTagset(importedProject, tagset, projectRepository, annotationService); }
         */

        // Import source document
        ImportUtil.createSourceDocument(importedProjectSetting, importedProject, documentService);

        // Import Training document
        ImportUtil.createTrainingDocument(importedProjectSetting, importedProject,
                automationService, featuresMap);
        // Import source document content
        ImportUtil.createSourceDocumentContent(zip, importedProject, documentService);
        // Import training document content
        ImportUtil.createTrainingDocumentContent(zip, importedProject, automationService);

        // Import automation settings
        ImportUtil.createMiraTemplate(importedProjectSetting, automationService, featuresMap);

        // Import annotation document content
        ImportUtil.createAnnotationDocument(importedProjectSetting, importedProject,
                documentService);
        // Import annotation document content
        ImportUtil.createAnnotationDocumentContent(zip, importedProject, documentService);

        // Import curation document content
        ImportUtil.createCurationDocumentContent(zip, importedProject, documentService);

        return importedProject;
    }

    static class Preferences implements Serializable
    {
        private static final long serialVersionUID = 3821654370145608038L;
        boolean generateUsers;
    }
}
