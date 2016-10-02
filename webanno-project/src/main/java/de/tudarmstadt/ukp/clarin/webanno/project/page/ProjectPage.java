/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.project.page;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.automation.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.automation.project.ProjectMiraTemplatePanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.webapp.home.page.ApplicationPageBase;

/**
 * This is the main page for Project Settings. The Page has Four Panels. The
 * {@link AnnotationGuideLinePanel} is used to update documents to a project. The
 * {@code ProjectDetailsPanel} used for updating Project details such as descriptions of a project
 * and name of the Project The {@link ProjectTagSetsPanel} is used to add {@link TagSet} and
 * {@link Tag} details to a Project as well as updating them The {@link ProjectUsersPanel} is used
 * to update {@link User} to a Project
 *
 *
 */
@MountPath("/projectsetting.html")
public class ProjectPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Log LOG = LogFactory.getLog(ProjectPage.class);

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "automationService")
    private AutomationService automationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    public static ProjectSelectionForm projectSelectionForm;
    public static ProjectDetailForm projectDetailForm;
    private final ImportProjectForm importProjectForm;

    private RadioChoice<Mode> projectType;
    public static boolean visible = true;

    public ProjectPage()
    {
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");

        projectDetailForm = new ProjectDetailForm("projectDetailForm");
        projectDetailForm.setOutputMarkupPlaceholderTag(true);
        projectDetailForm.setVisible(false);

        importProjectForm = new ImportProjectForm("importProjectForm");

        add(projectSelectionForm);
        add(importProjectForm);
        add(projectDetailForm);

        MetaDataRoleAuthorizationStrategy.authorize(importProjectForm, Component.RENDER,
                "ROLE_ADMIN");
    }

    class ProjectSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        private Button createProject;

        public ProjectSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(createProject = new Button("create", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    projectDetailForm.setModelObject(new Project());
                    projectDetailForm.setVisible(true);
                    ProjectSelectionForm.this.setModelObject(new SelectionModel());
                    if (projectType != null) {
                        projectType.setEnabled(true);
                    }
                }
            });

            MetaDataRoleAuthorizationStrategy.authorize(
                    createProject,
                    Component.RENDER,
                    StringUtils.join(new String[] { Role.ROLE_ADMIN.name(),
                            Role.ROLE_PROJECT_CREATOR.name() }, ","));

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
                            return repository.listAccessibleProjects();
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Project>("name"));
                    setNullValid(false);
                    
                    add(new OnChangeAjaxBehavior()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget aTarget)
                        {
                            if (getModelObject() != null) {
                                projectDetailForm.setModelObject(getModelObject());
                                projectDetailForm.setVisible(true);
                                
                                projectDetailForm.allTabs.setSelectedTab(0);
                                aTarget.add(projectDetailForm);
                            }
                            if (projectType != null) {
                                projectType.setEnabled(false);
                            }
                        }
                    });
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
        extends Form<ImportProjectModel>
    {
        private static final long serialVersionUID = -6361609153142402692L;
        private FileUploadField fileUpload;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ImportProjectForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new ImportProjectModel()));
            add(new CheckBox("generateUsers"));
            add(fileUpload = new FileUploadField("content", new Model()));

            add(new Button("importProject", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    List<FileUpload> exportedProjects = fileUpload.getFileUploads();
                    if (isEmpty(exportedProjects)) {
                        error("Please choose appropriate project/s in zip format");
                    }
                    else {
                        actionImportProject(exportedProjects,
                                ImportProjectForm.this.getModelObject().generateUsers);
                    }
                }
            });
        }
    }

    private class ImportProjectModel
        implements Serializable
    {
        private static final long serialVersionUID = -5858027181097577052L;

        boolean generateUsers = true;
    }

    public class ProjectDetailForm
        extends Form<Project>
    {
        private static final long serialVersionUID = -1L;

        private AjaxTabbedPanel<ITab> allTabs;

        public ProjectDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<Project>(new EntityModel<Project>(new Project())));
            add(allTabs = makeTabs());
            ProjectDetailForm.this.setMultiPart(true);
        }
        
        private AjaxTabbedPanel<ITab> makeTabs()
        {
            List<ITab> tabs = new ArrayList<>();
            tabs.add(new AbstractTab(Model.of("Details"))
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

            tabs.add(new AbstractTab(Model.of("Users"))
            {
                private static final long serialVersionUID = 7160734867954315366L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectUsersPanel(panelId, ProjectDetailForm.this.getModel());
                }

                @Override
                public boolean isVisible()
                {
                    return ProjectDetailForm.this.getModelObject().getId() != 0 && visible;
                }
            });

            tabs.add(new AbstractTab(Model.of("Documents"))
            {
                private static final long serialVersionUID = 1170760600317199418L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectDocumentsPanel(panelId, ProjectDetailForm.this.getModel());
                }

                @Override
                public boolean isVisible()
                {
                    return ProjectDetailForm.this.getModelObject().getId() != 0 && visible;
                }
            });

            tabs.add(new AbstractTab(Model.of("Layers"))
            {
                private static final long serialVersionUID = 3274065112505097898L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectLayersPanel(panelId, ProjectDetailForm.this.getModel());
                }

                @Override
                public boolean isVisible()
                {
                    return ProjectDetailForm.this.getModelObject().getId() != 0 && visible;
                }
            });

            tabs.add(new AbstractTab(Model.of("Tagsets"))
            {
                private static final long serialVersionUID = -3205723896786674220L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectTagSetsPanel(panelId, ProjectDetailForm.this.getModel());
                }

                @Override
                public boolean isVisible()
                {
                    return ProjectDetailForm.this.getModelObject().getId() != 0 && visible;
                }
            });

            tabs.add(new AbstractTab(Model.of("Guidelines"))
            {
                private static final long serialVersionUID = 7887973231065189200L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new AnnotationGuideLinePanel(panelId, ProjectDetailForm.this.getModel());
                }

                @Override
                public boolean isVisible()
                {
                    return ProjectDetailForm.this.getModelObject().getId() != 0 && visible;
                }
            });

            tabs.add(new AbstractTab(Model.of("Constraints"))
            {
                private static final long serialVersionUID = 8910455936756021733L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectConstraintsPanel(panelId, ProjectDetailForm.this.getModel());
                }

                @Override
                public boolean isVisible()
                {
                    return ProjectDetailForm.this.getModelObject().getId() != 0 && visible;
                }
            });

            tabs.add(new AbstractTab(Model.of("Export"))
            {
                private static final long serialVersionUID = 788812791376373350L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectExportPanel(panelId, ProjectDetailForm.this.getModel());
                }

                @Override
                public boolean isVisible()
                {
                    return ProjectDetailForm.this.getModelObject().getId() != 0;
                }
            });

            tabs.add(new AbstractTab(new Model<String>("Automation"))
            {
                private static final long serialVersionUID = 788812791376373350L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectMiraTemplatePanel(panelId, ProjectDetailForm.this.getModel());
                }

                @Override
                public boolean isVisible()
                {
                    Project project = ProjectDetailForm.this.getModelObject();
                    return project.getId() != 0 && project.getMode().equals(Mode.AUTOMATION)
                            && visible;
                }
            });
            AjaxTabbedPanel<ITab> allTabs = new AjaxTabbedPanel<ITab>("tabs", tabs);
            allTabs.setOutputMarkupPlaceholderTag(true);
            allTabs.setOutputMarkupId(true);
            return allTabs;
        }
    }
    
    private class ProjectDetailsPanel
        extends Panel
    {
        private static final long serialVersionUID = 1118880151557285316L;

        @SuppressWarnings("unchecked")
        public ProjectDetailsPanel(String id)
        {
            super(id);
            TextField<String> projectNameTextField = new TextField<String>("name");
            projectNameTextField.setRequired(true);
            add(projectNameTextField);

            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));

            add(projectType = (RadioChoice<Mode>) new RadioChoice<Mode>("mode",
                    Arrays.asList(new Mode[] { Mode.ANNOTATION, Mode.AUTOMATION, Mode.CORRECTION }))
                    .setEnabled(projectDetailForm.getModelObject().getId() == 0));

            add(new DropDownChoice<ScriptDirection>("scriptDirection",
                    Arrays.asList(ScriptDirection.values())));

            add(new Button("save", new ResourceModel("label"))
            {

                private static final long serialVersionUID = 1L;
                
                
                @Override
				public void validate() {
					super.validate();
						if (!ImportUtil.isNameValid(projectNameTextField.getInput())) {
							error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
							LOG.error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
						}
						if (projectNameTextField.getModelObject()!=null && repository.existsProject(projectNameTextField.getInput())
								&& !projectNameTextField.getInput().equals(projectNameTextField.getModelObject())) {
							error("Another project with same name exists. Please try a different name");
						} 
					
				}


				@Override
                public void onSubmit()
                {
                    Project project = projectDetailForm.getModelObject();
                    if (!ImportUtil.isNameValid(project.getName())) {

                        // Maintain already loaded project and selected Users
                        // Hence Illegal Project modification (limited
                        // privilege, illegal
                        // project
                        // name,...) preserves the original one
                        if (project.getId() != 0) {
                            project.setName(ImportUtil.validName(project.getName()));
                        }
                        error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
                        LOG.error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
                        return;
                    }
//                    if (repository.existsProject(project.getName()) && project.getId() == 0) {
//                        error("Another project with name [" + project.getName() + "] exists");
//                        return;
//                    }

                    if (repository.existsProject(project.getName()) && project.getId() != 0) {
                        error("project updated with name [" + project.getName() + "]");
                        return;
                    }
                    try {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        User user = userRepository.get(username);
                        repository.createProject(project, user);

                        repository.createProjectPermission(new ProjectPermission(project,
                                username, PermissionLevel.ADMIN));
                        repository.createProjectPermission(new ProjectPermission(project,
                                username, PermissionLevel.CURATOR));
                        repository.createProjectPermission(
                                new ProjectPermission(project, username, PermissionLevel.USER));

                        annotationService.initializeTypesForProject(project, user);
                        projectDetailForm.setVisible(true);
                        SelectionModel selectionModel = new SelectionModel();
                        selectionModel.project = project;
                        projectSelectionForm.setModelObject(selectionModel);
                    }
                    catch (IOException e) {
                        error("Project repository path not found " + ":"
                                + ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Project repository path not found " + ":"
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            });
            
            Button removeProjectButton = new Button("remove", new ResourceModel("label"))
            {

                private static final long serialVersionUID = 3822752631615693388L;

                @Override
                public void onSubmit()
                {
                    Project project = projectDetailForm.getModelObject();
                    if (project.getId() == 0) {

                    }
                    try {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        User user = userRepository.get(username);

                        // BEGIN: Remove automation stuff
                        for (MiraTemplate template : automationService.listMiraTemplates(project)) {
                            automationService.removeMiraTemplate(template);
                        }

                        for (SourceDocument document : automationService
                                .listTabSepDocuments(project)) {
                            repository.removeSourceDocument(document);
                        }
                        // END: Remove automation stuff

                        repository.removeProject(project, user);
                        projectDetailForm.setVisible(false);
                    }
                    catch (IOException e) {
                        LOG.error("Unable to remove project :"
                                + ExceptionUtils.getRootCauseMessage(e));
                        error("Unable to remove project " + ":"
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            };
            
            // Add check to prevent accidental delete operation
            removeProjectButton.add(new AttributeModifier("onclick",
                    "if(!confirm('Do you really want to delete this Project and all of its contents?')) return false;"));
            
            add(removeProjectButton);
 
//            add(new Button("remove", new ResourceModel("label"))
//            {
//                private static final long serialVersionUID = 1L;
//
//                @Override
//                public void onSubmit()
//                {
//                    Project project = projectDetailForm.getModelObject();
//                    if (project.getId() == 0) {
//
//                    }
//                    try {
//                        String username = SecurityContextHolder.getContext().getAuthentication()
//                                .getName();
//                        User user = userRepository.get(username);
//
//                        // BEGIN: Remove automation stuff
//                        for (MiraTemplate template : automationService.listMiraTemplates(project)) {
//                            automationService.removeMiraTemplate(template);
//                        }
//
//                        for (SourceDocument document : automationService
//                                .listTabSepDocuments(project)) {
//                            repository.removeSourceDocument(document);
//                        }
//                        // END: Remove automation stuff
//
//                        repository.removeProject(project, user);
//                        projectDetailForm.setVisible(false);
//                    }
//                    catch (IOException e) {
//                        LOG.error("Unable to remove project :"
//                                + ExceptionUtils.getRootCauseMessage(e));
//                        error("Unable to remove project " + ":"
//                                + ExceptionUtils.getRootCauseMessage(e));
//                    }
//                }
//            });
            add(new Button("cancel", new ResourceModel("label")) {
                private static final long serialVersionUID = 1L;
                
                {
                    // Avoid saving data
                    setDefaultFormProcessing(false);
                    setVisible(true);
                }
                
                @Override
                public void onSubmit()
                {
//                    projectSelectionForm.setModel(null);
//                    projectDetailForm.setVisible(false);
                    projectDetailForm.setModelObject(new Project());
                }
            });
        }
    }

    private void actionImportProject(List<FileUpload> exportedProjects, boolean aGenerateUsers)
    {
        Project importedProject = new Project();
        // import multiple projects!
        for (FileUpload exportedProject : exportedProjects) {
            InputStream tagInputStream;
            try {
                tagInputStream = exportedProject.getInputStream();
                if (!ZipUtils.isZipStream(tagInputStream)) {
                    error("Invalid ZIP file");
                    return;
                }
                File zipFfile = exportedProject.writeToTempFile();
                if (!ImportUtil.isZipValidWebanno(zipFfile)) {
                    error("Incompatible to webanno ZIP file");
                }
                ZipFile zip = new ZipFile(zipFfile);
                InputStream projectInputStream = null;
                for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
                    ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
                    if (entry.toString().replace("/", "").startsWith(ImportUtil.EXPORTED_PROJECT)
                            && entry.toString().replace("/", "").endsWith(".json")) {
                        projectInputStream = zip.getInputStream(entry);
                        break;
                    }
                }

                // projectInputStream =
                // uploadedFile.getInputStream();
                String text = IOUtils.toString(projectInputStream, "UTF-8");
                de.tudarmstadt.ukp.clarin.webanno.model.export.Project importedProjectSetting = JSONUtil
                        .getJsonConverter()
                        .getObjectMapper()
                        .readValue(text,
                                de.tudarmstadt.ukp.clarin.webanno.model.export.Project.class);

                importedProject = ImportUtil.createProject(importedProjectSetting, repository,
                        userRepository);

                Map<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature, AnnotationFeature> featuresMap = ImportUtil
                        .createLayer(importedProject, importedProjectSetting, userRepository,
                                annotationService);
                ImportUtil.createSourceDocument(importedProjectSetting, importedProject,
                        repository, userRepository, featuresMap);
                ImportUtil.createMiraTemplate(importedProjectSetting, automationService,
                        featuresMap);
                ImportUtil.createCrowdJob(importedProjectSetting, repository, importedProject);

                ImportUtil.createAnnotationDocument(importedProjectSetting, importedProject,
                        repository);
                ImportUtil.createProjectPermission(importedProjectSetting, importedProject,
                        repository, aGenerateUsers, userRepository);
                /*
                 * for (TagSet tagset : importedProjectSetting.getTagSets()) {
                 * ImportUtil.createTagset(importedProject, tagset, projectRepository,
                 * annotationService); }
                 */
                // add source document content
                ImportUtil.createSourceDocumentContent(zip, importedProject, repository);
                // add annotation document content
                ImportUtil.createAnnotationDocumentContent(zip, importedProject, repository);
                // create curation document content
                ImportUtil.createCurationDocumentContent(zip, importedProject, repository);
                // create project log
                ImportUtil.createProjectLog(zip, importedProject, repository);
                // create project guideline
                ImportUtil.createProjectGuideline(zip, importedProject, repository);
                // create project META-INF
                ImportUtil.createProjectMetaInf(zip, importedProject, repository);
                // create project constraint
                ImportUtil.createProjectConstraint(zip, importedProject, repository);
            }
            catch (Exception e) {
                error("Error Importing Project " + ExceptionUtils.getRootCauseMessage(e));
            }
        }
        projectDetailForm.setModelObject(importedProject);
        SelectionModel selectedProjectModel = new SelectionModel();
        selectedProjectModel.project = importedProject;
        projectSelectionForm.setModelObject(selectedProjectModel);
        projectDetailForm.setVisible(true);
        RequestCycle.get().setResponsePage(getPage());
    }
}
