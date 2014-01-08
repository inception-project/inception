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
import de.tudarmstadt.ukp.clarin.webanno.brat.project.ProjectUtil;
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

    public static ProjectSelectionForm projectSelectionForm;
    public static ProjectDetailForm projectDetailForm;
    private ImportProjectForm importProjectForm;

    private RadioChoice<Mode> projectType;
    public static boolean visible = true;

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

    class ProjectSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        private Button creatProject;

        public ProjectSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(creatProject = new Button("create", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    projectDetailForm.setModelObject(new Project());
                    projectDetailForm.setVisible(true);
                    ProjectSelectionForm.this.setVisible(true);
                    if (projectType != null) {
                        projectType.setEnabled(true);
                    }
                }
            });

            MetaDataRoleAuthorizationStrategy.authorize(creatProject, Component.RENDER,
                    "ROLE_ADMIN");

            add(new ListChoice<Project>("project")
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
                                if (authority.getAuthority().equals("ROLE_ADMIN")) {
                                    return allProjects;
                                }
                            }

                            // else only projects she is admin of
                            for (Project project : allProjects) {
                                if (ProjectUtil.isProjectAdmin(project, projectRepository, user)) {
                                    allowedProject.add(project);
                                }
                            }
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
                        projectDetailForm.setModelObject(aNewSelection);
                        projectDetailForm.setVisible(true);
                        ProjectSelectionForm.this.setVisible(true);
                    }
                    if (projectType != null) {
                        projectType.setEnabled(false);
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

    static public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        public Project project;
        public List<String> documents;
        public List<String> permissionLevels;
        public User user;
    }

    private class ImportProjectForm
        extends Form<Void>
    {
        private static final long serialVersionUID = -6361609153142402692L;
        private FileUploadField fileUpload;
        private FileUpload uploadedFile;

        @SuppressWarnings({ "unchecked", "rawtypes" })
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
                    if (uploadedFile == null) {
                        error("Please choose appropriate project in zip format");
                        return;
                    }
                    try {
                        if (!ProjectUtil.isZipStream(uploadedFile.getInputStream())) {
                            error("Invalid ZIP file");
                            return;
                        }
                        File zipFfile = uploadedFile.writeToTempFile();
                        if (!ProjectUtil.isZipValidWebanno(zipFfile)) {
                            error("Incompatible to webanno ZIP file");
                        }
                        ZipFile zip = new ZipFile(zipFfile);
                        InputStream projectInputStream = null;
                        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate
                                .hasMoreElements();) {
                            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
                            if (entry.toString().replace("/", "")
                                    .startsWith(ProjectUtil.EXPORTED_PROJECT)
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

                        Project importedProject = ProjectUtil.createProject(importedProjectSetting,
                                projectRepository);
                        ProjectUtil.createSourceDocument(importedProjectSetting, importedProject,
                                projectRepository);
                        ProjectUtil.createAnnotationDocument(importedProjectSetting,
                                importedProject, projectRepository);
                        ProjectUtil.createProjectPermission(importedProjectSetting,
                                importedProject, projectRepository);
                        for (TagSet tagset : importedProjectSetting.getTagSets()) {
                            ProjectUtil.createTagset(importedProject, tagset, projectRepository,
                                    annotationService);
                        }
                        // add source document content
                        ProjectUtil.createSourceDocumentContent(zip, importedProject,
                                projectRepository);
                        // add annotation document content
                        ProjectUtil.createAnnotationDocumentContent(zip, importedProject,
                                projectRepository);
                        // create curation document content
                        ProjectUtil.createCurationDocumentContent(zip, importedProject,
                                projectRepository);
                        // create project log
                        ProjectUtil.createProjectLog(zip, importedProject, projectRepository);
                        // create project guideline
                        ProjectUtil.createProjectGuideline(zip, importedProject, projectRepository);
                        // cretae project META-INF
                        ProjectUtil.createProjectMetaInf(zip, importedProject, projectRepository);
                    }
                    catch (IOException e) {
                        error("Error Importing Project " + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            });
        }

    }

    public class ProjectDetailForm
        extends Form<Project>
    {
        private static final long serialVersionUID = -1L;

        AbstractTab details;
        AbstractTab users;
        AbstractTab tagSets;
        AbstractTab documents;
        @SuppressWarnings("rawtypes")
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

                @Override
                public boolean isVisible()
                {
                    return visible;
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
                    return project.getObject().getId() != 0 && visible;
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
                    return project.getObject().getId() != 0 && visible;
                }
            });

            // not used in 1.0.0 release
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
                    return false;
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
                    return project.getObject().getId() != 0 && visible;
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
                    return project.getObject().getId() != 0 && visible;
                }
            });

            tabs.add(new AbstractTab(new Model<String>("Export/Import"))
            {

                private static final long serialVersionUID = 788812791376373350L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectExportPanel(panelId, project);
                }

                @Override
                public boolean isVisible()
                {
                    return project.getObject().getId() != 0;

                }
            });

            add(allTabs = new AjaxTabbedPanel<ITab>("tabs", tabs));
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

            add(projectType = (RadioChoice<Mode>) new RadioChoice<Mode>("mode",
                    Arrays.asList(new Mode[] { Mode.ANNOTATION, Mode.AUTOMATION, Mode.CORRECTION }))
                    .setEnabled(projectDetailForm.getModelObject().getId() == 0));
            add(new Button("save", new ResourceModel("label"))
            {

                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Project project = projectDetailForm.getModelObject();
                    if (!ProjectUtil.isNameValid(project.getName())) {

                        // Maintain already loaded project and selected Users
                        // Hence Illegal Project modification (limited privilege, illegal
                        // project
                        // name,...) preserves the original one
                        if (project.getId() != 0) {
                            project.setName(ProjectUtil.validName(project.getName()));
                        }
                        error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
                        LOG.error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
                        return;
                    }
                    if (projectRepository.existsProject(project.getName()) && project.getId() == 0) {
                        error("Another project with name [" + project.getName() + "] exists");
                        return;
                    }

                    if (projectRepository.existsProject(project.getName()) && project.getId() != 0) {
                        error("project updated with name [" + project.getName() + "]");
                        return;
                    }
                    try {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
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
            });
            add(new Button("remove", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Project project = projectDetailForm.getModelObject();
                    if (project.getId() == 0) {

                    }
                    try {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        User user = projectRepository.getUser(username);

                        projectRepository.removeProject(projectDetailForm.getModelObject(), user);
                        projectDetailForm.setVisible(false);
                    }
                    catch (IOException e) {
                        LOG.error("Unable to remove project :"
                                + ExceptionUtils.getRootCauseMessage(e));
                        error("Unable to remove project " + ":"
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            });
        }
    }
}
