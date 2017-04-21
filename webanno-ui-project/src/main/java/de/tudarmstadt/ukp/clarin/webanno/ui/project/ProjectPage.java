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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectLifecycleAware;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemCondition;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.NameUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelRegistryService;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelRegistryService.SettingsPanel;

/**
 * This is the main page for Project Settings. The Page has Four Panels. The
 * {@link AnnotationGuideLinePanel} is used to update documents to a project. The
 * {@code ProjectDetailsPanel} used for updating Project details such as descriptions of a project
 * and name of the Project The {@link ProjectTagSetsPanel} is used to add {@link TagSet} and
 * {@link Tag} details to a Project as well as updating them The {@link ProjectUsersPanel} is used
 * to update {@link User} to a Project
 */
@MenuItem(icon="images/setting_tools.png", label="Projects", prio=400)
@MountPath("/projectsetting.html")
public class ProjectPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectPage.class);

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AutomationService automationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectSettingsPanelRegistryService projectSettingsPanelRegistryService;

    public static ProjectSelectionForm projectSelectionForm;
    public static ProjectDetailForm projectDetailForm;
    private ImportProjectForm importProjectForm;

    public static boolean exportInProgress = false;

    public ProjectPage()
    {
        setModel(Model.of(new ProjectPageState()));
        
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm", getModel());

        projectDetailForm = new ProjectDetailForm("projectDetailForm", new Model<>());

        importProjectForm = new ImportProjectForm("importProjectForm");

        add(projectSelectionForm);
        add(importProjectForm);
        add(projectDetailForm);

        MetaDataRoleAuthorizationStrategy.authorize(importProjectForm, Component.RENDER,
                "ROLE_ADMIN");
    }
    
    public void setModel(IModel<ProjectPageState> aModel)
    {
        setDefaultModel(aModel);
    }
    
    @SuppressWarnings("unchecked")
    public IModel<ProjectPageState> getModel()
    {
        return (IModel<ProjectPageState>) getDefaultModel();
    }

    public void setModelObject(ProjectPageState aModel)
    {
        setDefaultModelObject(aModel);
    }
    
    public ProjectPageState getModelObject()
    {
        return (ProjectPageState) getDefaultModelObject();
    }

    class ProjectSelectionForm
        extends Form<ProjectPageState>
    {
        private static final long serialVersionUID = -1L;
        private Button createProject;

        public ProjectSelectionForm(String id, IModel<ProjectPageState> aModel)
        {
            super(id, CompoundPropertyModel.of(aModel));

            add(createProject = new Button("create", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    projectDetailForm.setModelObject(new Project());
                    ProjectSelectionForm.this.getModelObject().project = null;
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
                            String username = SecurityContextHolder.getContext().getAuthentication()
                                    .getName();
                            User user = userRepository.get(username);
                            return projectService.listAccessibleProjects(user);
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
                                projectDetailForm.setModelObject(
                                        ProjectSelectionForm.this.getModelObject().project);
                                projectDetailForm.allTabs.setSelectedTab(0);
                                aTarget.add(projectDetailForm);
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

    static public class ProjectPageState
        implements Serializable
    {
        private static final long serialVersionUID = 502442621850380752L;
        
        public Project project;
        public List<String> documents;
        public List<String> permissionLevels;
    }

    private class ImportProjectForm
        extends Form<ImportProjectFormState>
    {
        private static final long serialVersionUID = -6361609153142402692L;
        private FileUploadField fileUpload;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ImportProjectForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new ImportProjectFormState()));
            add(new CheckBox("generateUsers"));
            add(fileUpload = new FileUploadField("content", new Model()));

            add(new Button("importProject", new StringResourceModel("label"))
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

    private static class ImportProjectFormState
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

        public ProjectDetailForm(String id, IModel<Project> aModel)
        {
            super(id, CompoundPropertyModel.of(aModel));
            add(allTabs = makeTabs());
            setMultiPart(true);
            setOutputMarkupPlaceholderTag(true);
        }
        
        @Override
        protected void onConfigure()
        {
            super.onConfigure();
            setVisible(getModelObject() != null);
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
                    return !exportInProgress;
                }
            });
            
            // Add the project settings panels from the registry
            for (SettingsPanel psp : projectSettingsPanelRegistryService.getPanels()) {
                AbstractTab tab = new AbstractTab(Model.of(psp.label)) {
                    private static final long serialVersionUID = -1503555976570640065L;

                    @Override
                    public Panel getPanel(String aPanelId)
                    {
                        try {
                            ProjectSettingsPanelBase panel = (ProjectSettingsPanelBase) ConstructorUtils
                                    .invokeConstructor(psp.panel, aPanelId, ProjectDetailForm.this.getModel());
                            return panel;
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public boolean isVisible()
                    {
                        IModel<Project> model = ProjectDetailForm.this.getModel();
                        return model.getObject() != null && model.getObject().getId() != 0
                                && psp.condition.applies(model.getObject(), exportInProgress);
                    }
                };
                tabs.add(tab);
            }
            
            AjaxTabbedPanel<ITab> tabsPanel = new AjaxTabbedPanel<ITab>("tabs", tabs);
            tabsPanel.setOutputMarkupPlaceholderTag(true);
            tabsPanel.setOutputMarkupId(true);
            return tabsPanel;
        }
    }
    
    private class ProjectDetailsPanel
        extends Panel
    {
        private static final long serialVersionUID = 1118880151557285316L;

        private ChallengeResponseDialog deleteProjectDialog;
        private LambdaAjaxLink deleteProjectLink;
        private RadioChoice<Mode> projectType;
        
        public ProjectDetailsPanel(String id)
        {
            super(id);
            TextField<String> projectNameTextField = new TextField<String>("name");
            projectNameTextField.setRequired(true);
            add(projectNameTextField);

            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));

            add(projectType = new RadioChoice<Mode>("mode",
                    Arrays.asList(Mode.ANNOTATION, Mode.AUTOMATION, Mode.CORRECTION))
            {
                private static final long serialVersionUID = -8268365384613932108L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    IModel<Project> model = projectDetailForm.getModel();
                    setEnabled(model.getObject() != null && model.getObject().getId() == 0);
                }
            });

            add(new DropDownChoice<ScriptDirection>("scriptDirection",
                    Arrays.asList(ScriptDirection.values())));
            
            add(new CheckBox("disableExport"));

            add(new Button("save", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;
                
                @Override
				public void validate() {
					super.validate();
						if (!NameUtil.isNameValid(projectNameTextField.getInput())) {
							error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
							LOG.error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
						}
						if (projectNameTextField.getModelObject()!=null && projectService.existsProject(projectNameTextField.getInput())
								&& !projectNameTextField.getInput().equals(projectNameTextField.getModelObject())) {
							error("Another project with same name exists. Please try a different name");
						} 
					
				}

				@Override
                public void onSubmit()
                {
                    Project project = projectDetailForm.getModelObject();
                    if (!NameUtil.isNameValid(project.getName())) {

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

                    if (projectService.existsProject(project.getName()) && project.getId() != 0) {
                        error("project updated with name [" + project.getName() + "]");
                        return;
                    }
                    
                    if (project.getId() == 0) {
                        try {
                            String username = SecurityContextHolder.getContext().getAuthentication()
                                    .getName();
                            projectService.createProject(project);

                            projectService.createProjectPermission(new ProjectPermission(project,
                                    username, PermissionLevel.ADMIN));
                            projectService.createProjectPermission(new ProjectPermission(project,
                                    username, PermissionLevel.CURATOR));
                            projectService.createProjectPermission(
                                    new ProjectPermission(project, username, PermissionLevel.USER));

                            annotationService.initializeTypesForProject(project);
                            
                            projectSelectionForm.getModelObject().project = project;
                        }
                        catch (IOException e) {
                            error("Project repository path not found " + ":"
                                    + ExceptionUtils.getRootCauseMessage(e));
                            LOG.error("Project repository path not found " + ":"
                                    + ExceptionUtils.getRootCauseMessage(e));
                        }
                    }
                    else {
                        projectService.updateProject(project);
                    }
                }
            });
            
            IModel<String> projectNameModel = PropertyModel.of(projectDetailForm.getModel(),
                    "name");
            add(deleteProjectDialog = new ChallengeResponseDialog("deleteProjectDialog",
                    new StringResourceModel("DeleteProjectDialog.title", this),
                    new StringResourceModel("DeleteProjectDialog.text", this)
                            .setModel(projectDetailForm.getModel()).setParameters(projectNameModel),
                    projectNameModel));
            add(deleteProjectLink = new LambdaAjaxLink("deleteProjectLink",
                    this::actionDeleteProject) {
                private static final long serialVersionUID = -7483337091365688847L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    Project project = projectDetailForm.getModelObject();
                    setVisible(project != null && project.getId() != 0);
                }
            });
 
            add(new Button("cancel", new StringResourceModel("label")) {
                private static final long serialVersionUID = 1L;
                
                {
                    // Avoid saving data
                    setDefaultFormProcessing(false);
                }
                
                @Override
                public void onSubmit()
                {
                    projectSelectionForm.getModelObject().project = null;
                    projectDetailForm.setModelObject(null);
                }
            });
        }
        
        private void actionDeleteProject(AjaxRequestTarget aTarget)
        {
            deleteProjectDialog.setConfirmAction((target) -> {
                Project project = projectDetailForm.getModelObject();
                try {
                    projectService.removeProject(project);
                    projectDetailForm.setModelObject(null);
                    projectSelectionForm.getModelObject().project = null;
                    target.add(ProjectPage.this);
                }
                catch (IOException e) {
                    LOG.error("Unable to remove project :"
                            + ExceptionUtils.getRootCauseMessage(e));
                    error("Unable to remove project " + ":"
                            + ExceptionUtils.getRootCauseMessage(e));
                    target.addChildren(getPage(), FeedbackPanel.class);
                }
            });
            deleteProjectDialog.show(aTarget);
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

                // Load the project model from the JSON file
                String text = IOUtils.toString(projectInputStream, "UTF-8");
                de.tudarmstadt.ukp.clarin.webanno.model.export.Project importedProjectSetting = JSONUtil
                        .getJsonConverter()
                        .getObjectMapper()
                        .readValue(text,
                                de.tudarmstadt.ukp.clarin.webanno.model.export.Project.class);

                // Import the project itself
                importedProject = ImportUtil.createProject(importedProjectSetting, projectService);
                
                // Import additional project things
                projectService.onProjectImport(zip, importedProjectSetting, importedProject);

                // Import missing users
                if (aGenerateUsers) {
                    ImportUtil.createMissingUsers(importedProjectSetting, userRepository);
                }
                
                // Notify all relevant service so that they can initialize themselves for the given project
                for (ProjectLifecycleAware bean : projectService.getProjectLifecycleAwareBeans()) {
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
                Map<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature, AnnotationFeature> featuresMap = ImportUtil
                        .createLayer(importedProject, importedProjectSetting, userRepository,
                                annotationService);
                /*
                 * for (TagSet tagset : importedProjectSetting.getTagSets()) {
                 * ImportUtil.createTagset(importedProject, tagset, projectRepository,
                 * annotationService); }
                 */
                
                // Import source document 
                ImportUtil.createSourceDocument(importedProjectSetting, importedProject,
                        documentService, featuresMap);
                // Import source document content
                ImportUtil.createSourceDocumentContent(zip, importedProject, documentService);
                
                // Import automation settings
                ImportUtil.createMiraTemplate(importedProjectSetting, automationService,
                        featuresMap);
                
                // Import annotation document content
                ImportUtil.createAnnotationDocument(importedProjectSetting, importedProject,
                        documentService);
                // Import annotation document content
                ImportUtil.createAnnotationDocumentContent(zip, importedProject, documentService);
                
                // Import curation document content
                ImportUtil.createCurationDocumentContent(zip, importedProject, documentService);
            }
            catch (Exception e) {
                error("Error Importing Project " + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Error importing project", e);
            }
        }
        
        projectDetailForm.setModelObject(importedProject);
        ProjectPageState selectedProjectModel = new ProjectPageState();
        selectedProjectModel.project = importedProject;
        projectSelectionForm.setModelObject(selectedProjectModel);
        RequestCycle.get().setResponsePage(getPage());
    }
    
    /**
     * Only admins and project managers can see this page
     */
    @MenuItemCondition
    public static boolean menuItemCondition(ProjectService aRepo, UserDao aUserRepo)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aUserRepo.get(username);
        return SecurityUtil.projectSettingsEnabeled(aRepo, user);
    }
}
