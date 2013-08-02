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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.Component;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.export.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.EntityModel;

/**
 * This is the main page for Project Settings. The Page has Four Panels. The
 * {@link AnnotationGuideLinePanel} is used to update documents to a project. The
 * {@link ProjectDetailsPanel} used for updating Project deatils such as descriptions of a project
 * and name of the Project The {@link ProjectTagSetsPanel} is used to add {@link TagSet} and
 * {@link Tag} details to a Project as well as updating them The {@link ProjectUsersPanel} is used
 * to update {@link User} to a Project
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public class ProjectPage
    extends SettingsPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Log LOG = LogFactory.getLog(ProjectPage.class);

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private ProjectSelectionForm projectSelectionForm;
    private ProjectDetailForm projectDetailForm;
    private ImportProjectForm importProjectForm;

    private boolean createProject = false;

    private RadioChoice<Mode> projectType;

    public ProjectPage()
    {
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");

        projectDetailForm = new ProjectDetailForm("projectDetailForm");
        projectDetailForm.setVisible(false);

        importProjectForm = new ImportProjectForm("importProjectForm");

        add(projectSelectionForm.add(importProjectForm));
        add(projectDetailForm);

        MetaDataRoleAuthorizationStrategy.authorize(importProjectForm, Component.RENDER,
                "ROLE_ADMIN");
    }

    private class ProjectSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        private Button creatProject;
        private ListChoice<Project> projects;

        public ProjectSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(creatProject = new Button("create", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    ProjectSelectionForm.this.getModelObject().project = null;
                    projectDetailForm.setModelObject(new Project());
                    createProject = true;
                    projectDetailForm.setVisible(true);
                    ProjectSelectionForm.this.setVisible(true);
                    if (projectType != null) {
                        projectType.setEnabled(createProject);
                    }
                }
            });

            MetaDataRoleAuthorizationStrategy.authorize(creatProject, Component.RENDER,
                    "ROLE_ADMIN");

            add(projects = new ListChoice<Project>("project")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<Project>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<Project> load()
                        {
                            List<Project> allowedProject = new ArrayList<Project>();

                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = projectRepository.getUser(username);

                            List<Project> allProjects = projectRepository.listProjects();
                            List<Authority> authorities = projectRepository.listAuthorities(user);

                            // if global admin, show all projects
                            for (Authority authority : authorities) {
                                if (authority.getRole().equals("ROLE_ADMIN")) {
                                    ApplicationUtils.sortProjects(allProjects);
                                    return allProjects;
                                }
                            }

                            // else only projects she is admin of
                            for (Project project : allProjects) {
                                if (ApplicationUtils.isProjectAdmin(project, projectRepository,
                                        user)) {
                                    allowedProject.add(project);
                                }
                            }
                            ApplicationUtils.sortProjects(allowedProject);
                            return allowedProject;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Project>("name"));
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(Project aNewSelection)
                {
                    if (aNewSelection != null) {
                        createProject = false;
                        projectDetailForm.setModelObject(aNewSelection);
                        projectDetailForm.setVisible(true);
                        ProjectSelectionForm.this.setVisible(true);
                    }
                    if (projectType != null) {
                        projectType.setEnabled(createProject);
                    }
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
        }
    }

    static private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private Project project;
        private List<String> documents;
        private List<String> permissionLevels;
        private User user;
    }

    private class ImportProjectForm
        extends Form<Void>
    {

        private FileUploadField fileUpload;
        private FileUpload uploadedFile;

        public ImportProjectForm(String id)
        {
            super(id);
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

                                File zipFfile = uploadedFile.writeToTempFile();
                                if (ApplicationUtils.isZipValidWebanno(zipFfile)) {
                                    ZipFile zip = new ZipFile(zipFfile);
                                    InputStream projectInputStream = null;
                                    for (Enumeration zipEnumerate = zip.entries(); zipEnumerate
                                            .hasMoreElements();) {
                                        ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
                                        if (entry.toString().replace("/", "")
                                                .startsWith(ApplicationUtils.EXPORTED_PROJECT)
                                                && entry.toString().replace("/", "")
                                                        .endsWith(".json")) {
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

                                    Project importedProject = ApplicationUtils.createProject(
                                            importedProjectSetting, projectRepository);
                                    ApplicationUtils.createSourceDocument(importedProjectSetting,
                                            importedProject, projectRepository);
                                    ApplicationUtils.createAnnotationDocument(
                                            importedProjectSetting, importedProject,
                                            projectRepository);
                                    ApplicationUtils.createProjectPermission(
                                            importedProjectSetting, importedProject,
                                            projectRepository);
                                    for (TagSet tagset : importedProjectSetting.getTagSets()) {
                                        ApplicationUtils.createTagset(importedProject, tagset,
                                                projectRepository, annotationService);
                                    }
                                    // add source document content
                                    ApplicationUtils.createSourceDocumentContent(zip,
                                            importedProject, projectRepository);
                                    // add annotation document content
                                    ApplicationUtils.createAnnotationDocumentContent(zip,
                                            importedProject, projectRepository);
                                    // create curation document content
                                    ApplicationUtils.createCurationDocumentContent(zip,
                                            importedProject, projectRepository);
                                    // create project log
                                    ApplicationUtils.createProjectLog(zip, importedProject,
                                            projectRepository);
                                    // create project guideline
                                    ApplicationUtils.createProjectGuideline(zip, importedProject,
                                            projectRepository);
                                    // cretae project META-INF
                                    ApplicationUtils.createProjectMetaInf(zip, importedProject,
                                            projectRepository);
                                }
                                else {
                                    error("Incompatible to webanno ZIP file");
                                }
                            }
                            else {
                                error("Invalid ZIP file");
                            }
                        }
                        catch (IOException e) {
                            error("Error Importing Project "
                                    + ExceptionUtils.getRootCauseMessage(e));
                        }
                    }
                    else if (uploadedFile == null) {
                        error("Please choose appropriate project in zip format");
                    }
                }
            });
        }

    }

    private class ProjectDetailForm
        extends Form<Project>
    {
        private static final long serialVersionUID = -1L;

        AbstractTab details;
        AbstractTab users;
        AbstractTab tagSets;
        AbstractTab documents;
        AjaxTabbedPanel allTabs;

        public ProjectDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<Project>(new EntityModel<Project>(new Project())));

            List<ITab> tabs = new ArrayList<ITab>();
            tabs.add(details = new AbstractTab(new Model<String>("Details"))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectDetailsPanel(panelId);
                }
            });

            tabs.add(users = new AbstractTab(new Model<String>("Users"))
            {
                private static final long serialVersionUID = 7160734867954315366L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectUsersPanel(panelId, project);
                }

                @Override
                public boolean isVisible()
                {
                    return !createProject;
                }
            });

            tabs.add(documents = new AbstractTab(new Model<String>("Documents"))
            {
                private static final long serialVersionUID = 1170760600317199418L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectDocumentsPanel(panelId, project);
                }

                @Override
                public boolean isVisible()
                {
                    return !createProject;
                }
            });

            tabs.add(tagSets = new AbstractTab(new Model<String>("Layers"))
            {
                private static final long serialVersionUID = 3274065112505097898L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new EmptyPanel(panelId);
                }

                @Override
                public boolean isVisible()
                {
                    return !createProject;
                }
            });

            tabs.add(tagSets = new AbstractTab(new Model<String>("Tagsets"))
            {
                private static final long serialVersionUID = -3205723896786674220L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectTagSetsPanel(panelId, project);
                }

                @Override
                public boolean isVisible()
                {
                    return !createProject;
                }
            });

            tabs.add(new AbstractTab(new Model<String>("Guidelines"))
            {
                private static final long serialVersionUID = 7887973231065189200L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new AnnotationGuideLinePanel(panelId, project);
                }

                @Override
                public boolean isVisible()
                {
                    return !createProject;
                }
            });

            tabs.add(new AbstractTab(new Model<String>("Export/Import"))
            {

                private static final long serialVersionUID = 788812791376373350L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ExportPanel(panelId, project);
                }

                @Override
                public boolean isVisible()
                {
                    return !createProject;

                }
            });

            add(allTabs = new AjaxTabbedPanel("tabs", tabs));
            ProjectDetailForm.this.setMultiPart(true);
        }

        // Update the project mode, that will be shared among TABS
        // Better way of sharing data
        // http://stackoverflow.com/questions/6532178/wicket-persistent-object-between-panels
        Model<Project> project = new Model<Project>()
        {
            private static final long serialVersionUID = -6394439155356911110L;

            @Override
            public Project getObject()
            {
                return projectDetailForm.getModelObject();
            }
        };
    }

    private class ProjectDetailsPanel
        extends Panel
    {
        private static final long serialVersionUID = 1118880151557285316L;

        @SuppressWarnings("unchecked")
        public ProjectDetailsPanel(String id)
        {
            super(id);
            add(new TextField<String>("name").setRequired(true));

            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            // Add check box to enable/disable arc directions of dependency parsing
            add(new CheckBox("reverseDependencyDirection"));
            add(projectType = (RadioChoice<Mode>) new RadioChoice<Mode>("mode",
                    Arrays.asList(new Mode[] { Mode.ANNOTATION, Mode.CORRECTION }))
                    .setEnabled(createProject));
            add(new Button("save", new ResourceModel("label"))
            {

                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Project project = projectDetailForm.getModelObject();
                    boolean projectExist = false;
                    try {
                        projectRepository.existsProject(project.getName());
                    }
                    catch (Exception e) {
                        error("Another project with name [" + project.getName() + "] exists!"
                                + ExceptionUtils.getRootCauseMessage(e));
                        projectExist = true;
                    }
                    // If only the project is new!
                    if (project.getId() == 0 && !projectExist) {
                        // Check if the project with this name already exist
                        if (projectRepository.existsProject(project.getName())) {
                            error("Project with this name already exist !");
                            LOG.error("Project with this name already exist !");
                        }
                        else if (ApplicationUtils.isNameValid(project.getName())) {
                            try {
                                String username = SecurityContextHolder.getContext()
                                        .getAuthentication().getName();
                                User user = projectRepository.getUser(username);
                                projectRepository.createProject(project, user);
                                annotationService.initializeTypesForProject(project, user);
                                projectDetailForm.setVisible(true);
                            }
                            catch (IOException e) {
                                error("Project repository path not found " + ":"
                                        + ExceptionUtils.getRootCauseMessage(e));
                                LOG.error("Project repository path not found " + ":"
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                        }
                        else {
                            error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
                            LOG.error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
                        }
                    }
                    // This is updating Project details
                    else {
                        // Invalid Project name, restore
                        if (!ApplicationUtils.isNameValid(project.getName()) && !projectExist) {

                            // Maintain already loaded project and selected Users
                            // Hence Illegal Project modification (limited privilege, illegal
                            // project
                            // name,...) preserves the original one

                            String oldProjectName = projectRepository.getProject(project.getId())
                                    .getName();

                            project.setName(oldProjectName);
                            error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
                            LOG.error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
                        }
                    }
                    createProject = false;
                }
            });
            add(new Button("remove", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Project project = projectDetailForm.getModelObject();
                    if (project.getId() != 0) {
                        try {
                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = projectRepository.getUser(username);

                            projectRepository.removeProject(projectDetailForm.getModelObject(),
                                    user);
                            projectDetailForm.setVisible(false);
                        }
                        catch (IOException e) {
                            LOG.error("Unable to remove project :"
                                    + ExceptionUtils.getRootCauseMessage(e));
                            error("Unable to remove project " + ":"
                                    + ExceptionUtils.getRootCauseMessage(e));
                        }

                    }
                }
            });
        }
    }
}
