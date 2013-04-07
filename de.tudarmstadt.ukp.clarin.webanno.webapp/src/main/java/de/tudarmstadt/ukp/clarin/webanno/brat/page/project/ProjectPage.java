/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.page.project;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.NoResultException;

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
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.link.DownloadLink;
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
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.CasToBratJson;
import de.tudarmstadt.ukp.clarin.webanno.brat.support.EntityModel;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSetContent;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSets;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermissions;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

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

    private class ProjectSelectionForm
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
                    ProjectSelectionForm.this.getModelObject().project = null;
                    projectDetailForm.setModelObject(new Project());
                    createProject = true;
                    projectDetailForm.setVisible(true);
                    ProjectSelectionForm.this.setVisible(true);
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
                            List<Authority> authorities = projectRepository.getAuthorities(user);

                            // if global admin, show all projects
                            for (Authority authority : authorities) {
                                if (authority.getRole().equals("ROLE_ADMIN")) {
                                    return allProjects;
                                }
                            }

                            // else only projects she is admin of
                            for (Project project : allProjects) {
                                if (ApplicationUtils.isProjectAdmin(project, projectRepository,
                                        user)) {
                                    allowedProject.add(project);
                                }
                                else {
                                    error("You don't have permission!");
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
                        createProject = false;
                        projectDetailForm.setModelObject(aNewSelection);
                        projectDetailForm.setVisible(true);
                        ProjectSelectionForm.this.setVisible(true);
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
    }

    private class ProjectDetailForm
        extends Form<Project>
    {
        private static final long serialVersionUID = -1L;

        AbstractTab details;
        AbstractTab users;
        AbstractTab tagSets;
        AbstractTab documents;

        public ProjectDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<Project>(new EntityModel<Project>(new Project())));

            List<ITab> tabs = new ArrayList<ITab>();
            tabs.add(details = new AbstractTab(new Model<String>("Project Details"))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectDetailsPanel(panelId);
                }
                @Override
                public boolean isVisible(){
                 return true;

                }
            });

            tabs.add(users = new AbstractTab(new Model<String>("Project Users"))
            {
                private static final long serialVersionUID = 7160734867954315366L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectUsersPanel(panelId);
                }
                @Override
                public boolean isVisible(){
                 if(createProject) {
                    return false;
                }
                 return true;

                }
            });

            tabs.add(documents = new AbstractTab(new Model<String>("Project Documents"))
            {
                private static final long serialVersionUID = 1170760600317199418L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectDocumentsPanel(panelId);
                }
                @Override
                public boolean isVisible(){
                 if(createProject) {
                    return false;
                }
                 return true;

                }
            });

            tabs.add(tagSets = new AbstractTab(new Model<String>("Project TagSets"))
            {
                private static final long serialVersionUID = -3205723896786674220L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectTagSetsPanel(panelId);
                }
                @Override
                public boolean isVisible(){
                 if(createProject) {
                    return false;
                }
                 return true;

                }
            });

            tabs.add(new AbstractTab(new Model<String>("Annotation Guideline"))
            {
                private static final long serialVersionUID = 7887973231065189200L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new AnnotationGuideLinePanel(panelId);
                }
                @Override
                public boolean isVisible(){
                 if(createProject) {
                    return false;
                }
                 return true;

                }
            });

            add(new AjaxTabbedPanel("tabs", tabs));
            ProjectDetailForm.this.setMultiPart(true);
        }
    }

    private ProjectSelectionForm projectSelectionForm;
    private ProjectDetailForm projectDetailForm;
    // Fix for Issue "refresh for "new project" in project configuration (Bug #141) "
    boolean createProject = false;

    public ProjectPage()
    {
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");

        projectDetailForm = new ProjectDetailForm("projectDetailForm");
        projectDetailForm.setVisible(false);

        add(projectSelectionForm);
        add(projectDetailForm);
    }

    private class ProjectDetailsPanel
        extends Panel
    {
        private static final long serialVersionUID = 1118880151557285316L;

        public ProjectDetailsPanel(String id)
        {
            super(id);
            add(new TextField<String>("name").setRequired(true));

            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            // Add check box to enable/disable arc directions of dependency parsing
            add(new CheckBox("reverseDependencyDirection"));
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
                        error("Another project with name [" + project.getName() + "] exists!" + ExceptionUtils.getRootCauseMessage(e));
                        projectExist = true;
                    }
                    // If only the project is new!
                    if (project.getId() == 0 &&!projectExist) {
                        // Check if the project with this name already exist
                        if (projectRepository.existsProject(project.getName())) {
                            error("Project with this name already exist !");
                            LOG.error("Project with this name already exist !");
                        }
                        else if (isProjectNameValid(project.getName())) {
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
                        if (!isProjectNameValid(project.getName())&&!projectExist) {

                            // Maintain already loaded project and selected Users
                            // Hence Illegal Project modification (limited privilege, illegal
                            // project
                            // name,...) preserves the original one

                            String oldProjectName = projectRepository.getProject(project.getId())
                                    .getName();
                            List<User> selectedusers = projectRepository.listProjectUsers(project);

                            project.setName(oldProjectName);
                            project.setUsers(new HashSet<User>(selectedusers));
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

        /**
         * Check if the Project name is valid, SPecial characters are not allowed as a project name
         * as it will conflict with file naming system
         *
         * @param aProjectName
         * @return
         */
        public boolean isProjectNameValid(String aProjectName)
        {
            if (aProjectName.contains("^") || aProjectName.contains("/")
                    || aProjectName.contains("\\") || aProjectName.contains("&")
                    || aProjectName.contains("*") || aProjectName.contains("?")
                    || aProjectName.contains("+") || aProjectName.contains("$")
                    || aProjectName.contains("!") || aProjectName.contains("[")
                    || aProjectName.contains("]")) {
                return false;
            }
            else {
                return true;
            }
        }
    }

    private class ProjectUsersPanel
        extends Panel
    {
        private static final long serialVersionUID = -8668945427924328076L;
        private CheckBoxMultipleChoice<User> users;

        public ProjectUsersPanel(String id)
        {
            super(id);
            add(users = (CheckBoxMultipleChoice<User>) new CheckBoxMultipleChoice<User>("users",
                    new LoadableDetachableModel<List<User>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<User> load()
                        {
                            return projectRepository.listUsers();
                        }
                    }, new ChoiceRenderer<User>("username", "username")).setRequired(true));

            add(new Button("add", new ResourceModel("label"))
            {

                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Project project = projectDetailForm.getModelObject();

                    Set<User> users = project.getUsers();
                    if (project.getId() != 0) {
                        project.setUsers(users);
                        info(users.size() + " users are added");

                        // This is a temporary solution, to set default permission level as
                        // user(annotator)
                        for (User user : users) {
                            List<String> permisionLevels = projectRepository
                                    .listProjectPermisionLevels(user, project);
                            if (permisionLevels.size() == 0) {
                                ProjectPermissions permission = new ProjectPermissions();
                                permission.setLevel("user");
                                permission.setProject(project);
                                permission.setUser(user);
                                try {
                                    projectRepository.createProjectPermission(permission);
                                }
                                catch (IOException e) {
                                    error("Unable to create log file "
                                            + ExceptionUtils.getRootCauseMessage(e));
                                }
                            }
                        }
                        List<String> addedUsers = new ArrayList<String>();
                        for (User selectedUser : users) {
                            addedUsers.add(selectedUser.getUsername());
                        }

                        // remove user permission if user is removed (checkBox unchecked)
                        for (User user : projectRepository.listUsers()) {

                            if (!addedUsers.contains(user.getUsername())) {
                                // User previously have a permission for this project, but removed
                                // now
                                if (projectRepository.listProjectPermisionLevels(user, project)
                                        .size() > 0) {
                                    // Admins should be removed manually, for the time being
                                    if (!projectRepository.getPermisionLevel(user, project).equals(
                                            "admin")) {
                                        try {
                                            projectRepository
                                                    .removeProjectPermission(projectRepository
                                                            .getProjectPermission(user, project));
                                        }
                                        catch (IOException e) {
                                            error("Unable to create log file "
                                                    + ExceptionUtils.getRootCauseMessage(e));
                                        }
                                    }

                                }
                            }
                        }
                    }
                    else if (project.getId() == 0) {
                        error("Project not yet created!");
                        users.clear(); // reset selection
                    }

                }
            });
        }
    }

    private class ProjectDocumentsPanel
        extends Panel
    {
        private static final long serialVersionUID = 2116717853865353733L;
        private ArrayList<String> documents = new ArrayList<String>();
        private ArrayList<String> selectedDocuments = new ArrayList<String>();

        private List<FileUpload> uploadedFiles;
        private FileUploadField fileUpload;

        private ArrayList<String> readableFormats;
        private String selectedFormat;

        private DropDownChoice<String> readableFormatsChoice;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ProjectDocumentsPanel(String id)
        {
            super(id);
            try {
                readableFormats = new ArrayList<String>(projectRepository.getReadableFormats()
                        .keySet());
                selectedFormat = readableFormats.get(0);
            }
            catch (IOException e) {
                error("Properties file not found or key not int the properties file " + ":"
                        + ExceptionUtils.getRootCauseMessage(e));
            }
            catch (ClassNotFoundException e) {
                error("The Class name in the properties is not found " + ":"
                        + ExceptionUtils.getRootCauseMessage(e));
            }
            add(fileUpload = new FileUploadField("content", new Model()));

            add(readableFormatsChoice = new DropDownChoice<String>("readableFormats", new Model(
                    selectedFormat), readableFormats));
            add(new Button("import", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    uploadedFiles = fileUpload.getFileUploads();
                    Project project = projectDetailForm.getModelObject();

                    if (isNotEmpty(uploadedFiles)) {
                        for (FileUpload documentToUpload : uploadedFiles) {
                            InputStream is;
                            try {
                                String fileName = documentToUpload.getClientFileName();
                                File uploadFile = documentToUpload.writeToTempFile();

                                // if getSourceDocument succeeded, it is a duplication!
                                try {

                                    projectRepository.getSourceDocument(fileName, project);
                                    error("Document " + fileName + " already uploaded ! Delete "
                                            + "the document if you want to upload again");
                                    LOG.error("Document " + fileName
                                            + " already uploaded ! Delete "
                                            + "the document if you want to upload again");
                                }
                                // The document is not yet saved!
                                catch (NoResultException ex) {
                                    String username = SecurityContextHolder.getContext()
                                            .getAuthentication().getName();
                                    User user = projectRepository.getUser(username);

                                    SourceDocument document = new SourceDocument();
                                    document.setName(fileName);
                                    document.setProject(project);
                                    document.setFormat(readableFormatsChoice.getModelObject());
                                    projectRepository.createSourceDocument(document, user);
                                    projectRepository.uploadSourceDocument(uploadFile, document,
                                            project.getId(), user);

                                }
                            }
                            catch (Exception e) {
                                error("Error uploading document "
                                        + ExceptionUtils.getRootCauseMessage(e));
                                LOG.error("Error uploading document "
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                        }
                    }
                    else if (isEmpty(uploadedFiles)) {
                        error("No document is selected to upload, please select a document first");
                        LOG.info("No document is selected to upload, please select a document first");
                    }
                    else if (project.getId() == 0) {
                        error("Project not yet created, please save project Details!");
                        LOG.info("Project not yet created, please save project Details!");
                    }

                }
            });

            add(new ListMultipleChoice<String>("documents", new Model(selectedDocuments), documents)
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<String>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<String> load()
                        {
                            Project project = projectDetailForm.getModelObject();
                            documents.clear();
                            if (project.getId() != 0) {
                                for (SourceDocument document : projectRepository
                                        .listSourceDocuments(project)) {
                                    documents.add(document.getName());
                                }
                            }
                            return documents;
                        }
                    });
                }
            });

            add(new Button("remove", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Project project = projectDetailForm.getModelObject();
                    for (String document : selectedDocuments) {
                        try {
                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = projectRepository.getUser(username);
                            projectRepository.removeSourceDocument(
                                    projectRepository.getSourceDocument(document, project), user);
                        }
                        catch (IOException e) {
                            error("Error while removing a document document "
                                    + ExceptionUtils.getRootCauseMessage(e));
                            LOG.error("Error while removing a document document "
                                    + ExceptionUtils.getRootCauseMessage(e));
                        }
                        documents.remove(document);
                    }
                }
            });
        }
    }

    private class ProjectTagSetsPanel
        extends Panel
    {
        private static final long serialVersionUID = 7004037105647505760L;
        private TagSetSelectionForm tagSetSelectionForm;
        private TagSelectionForm tagSelectionForm;
        private TagSetDetailForm tagSetDetailForm;
        private TagDetailForm tagDetailForm;

        private List<FileUpload> uploadedFiles;
        private FileUploadField fileUpload;

        private class TagSetSelectionForm
            extends Form<SelectionModel>
        {
            private static final long serialVersionUID = -1L;

            @SuppressWarnings({ "unchecked" })
            public TagSetSelectionForm(String id)
            {
                // super(id);
                super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

                add(new Button("create", new ResourceModel("label"))
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit()
                    {
                        if (projectDetailForm.getModelObject().getId() == 0) {
                            error("Project not yet created. Please save project details first!");
                        }
                        else {
                            TagSetSelectionForm.this.getModelObject().tagSet = null;
                            tagSetDetailForm.setModelObject(new TagSet());
                            tagSetDetailForm.setVisible(true);
                            tagSelectionForm.setVisible(false);
                            tagDetailForm.setVisible(false);
                        }
                    }
                });

                add(new ListChoice<TagSet>("tagSet")
                {
                    private static final long serialVersionUID = 1L;

                    {
                        setChoices(new LoadableDetachableModel<List<TagSet>>()
                        {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected List<TagSet> load()
                            {
                                Project project = projectDetailForm.getModelObject();
                                if (project.getId() != 0) {
                                    return annotationService.listTagSets(project);
                                }
                                else {
                                    return new ArrayList<TagSet>();
                                }
                            }
                        });
                        setChoiceRenderer(new ChoiceRenderer<TagSet>()
                        {
                            @Override
                            public Object getDisplayValue(TagSet aObject)
                            {
                                return "[" + aObject.getType().getName() + "] " + aObject.getName();
                            }
                        });
                        setNullValid(false);
                    }

                    @Override
                    protected void onSelectionChanged(TagSet aNewSelection)
                    {
                        if (aNewSelection != null) {
                            // TagSetSelectionForm.this.getModelObject().tagSet = new TagSet();
                            tagSetDetailForm.clearInput();
                            tagSetDetailForm.setModelObject(aNewSelection);
                            tagSetDetailForm.setVisible(true);
                            TagSetSelectionForm.this.setVisible(true);
                            tagSelectionForm.setVisible(true);
                            tagDetailForm.setVisible(true);

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
                        return aSelectedValue;
                    }
                });

                add(fileUpload = new FileUploadField("content", new Model()));
                add(new Button("import", new ResourceModel("label"))
                {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit()
                    {
                        uploadedFiles = fileUpload.getFileUploads();
                        Project project = projectDetailForm.getModelObject();
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        User user = projectRepository.getUser(username);

                        if (isNotEmpty(uploadedFiles)) {
                            for (FileUpload tagFile : uploadedFiles) {
                                InputStream tagInputStream;
                                try {
                                    tagInputStream = tagFile.getInputStream();
                                    String text = IOUtils.toString(tagInputStream, "UTF-8");

                                    MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
                                    ExportedTagSets importedTagSet = jsonConverter
                                            .getObjectMapper().readValue(text,
                                                    ExportedTagSets.class);
                                    List<ExportedTagSetContent> importedTagSets = importedTagSet
                                            .getTagSets();

                                    AnnotationType type = null;
                                    List<AnnotationType> types = annotationService.getTypes(
                                            importedTagSets.get(0).getTypeName(), importedTagSets
                                                    .get(0).getType());
                                    if (types.size() == 0) {
                                        type = new AnnotationType();
                                        type.setDescription(importedTagSets.get(0)
                                                .getTypeDescription());
                                        type.setName(importedTagSets.get(0).getTypeName());
                                        type.setType(importedTagSets.get(0).getType());
                                        annotationService.createType(type);
                                    }
                                    else {
                                        type = types.get(0);
                                    }

                                    for (ExportedTagSetContent tagSet : importedTagSets) {
                                        List<TagSet> tagSetInDb = annotationService.getTagSet(type,
                                                project);
                                        if (tagSetInDb.size() == 0) {
                                            TagSet newTagSet = new TagSet();
                                            newTagSet.setDescription(tagSet.getDescription());
                                            newTagSet.setName(tagSet.getName());
                                            newTagSet.setLanguage(tagSet.getLanguage());
                                            newTagSet.setProject(project);
                                            newTagSet.setType(type);
                                            annotationService.createTagSet(newTagSet, user);
                                            for (de.tudarmstadt.ukp.clarin.webanno.export.model.Tag tag : tagSet
                                                    .getTags()) {
                                                Tag newTag = new Tag();
                                                newTag.setDescription(tag.getDescription());
                                                newTag.setName(tag.getName());
                                                newTag.setTagSet(newTagSet);
                                                annotationService.createTag(newTag, user);
                                            }
                                            info("TagSet successfully imported. Refresh page to see the imported TagSet.");
                                        }
                                        else {
                                            error("Tagset" + importedTagSets.get(0).getName()
                                                    + " already exist");
                                        }
                                    }

                                }
                                catch (IOException e) {
                                    error("Error Importing TagSDet "
                                            + ExceptionUtils.getRootCauseMessage(e));
                                    LOG.error("Error Importing TagSDet "
                                            + ExceptionUtils.getRootCauseMessage(e));
                                }
                            }
                        }
                        else if (isEmpty(uploadedFiles)) {
                            error("No Tagset File is selected to upload, please select a document first");
                        }
                        else if (project.getId() == 0) {
                            error("Project not yet created, please save project Details!");
                        }

                    }
                });

            }
        }

        private class SelectionModel
            implements Serializable
        {
            private static final long serialVersionUID = -1L;

            private TagSet tagSet;
            private Tag tag;
        }

        private class TagSetDetailForm
            extends Form<TagSet>
        {
            private static final long serialVersionUID = -1L;
            TagSet tagSet;
            private List<FileUpload> uploadedFiles;
            private FileUploadField fileUpload;

            public TagSetDetailForm(String id)
            {
                super(id, new CompoundPropertyModel<TagSet>(new EntityModel<TagSet>(new TagSet())));
                // super(id);
                add(new TextField<String>("name").setRequired(true));

                add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));

                add(new TextField<String>("language"));

                add(new DropDownChoice<AnnotationType>("type",
                        annotationService.listAnnotationType(), new ChoiceRenderer<AnnotationType>(
                                "name")).setRequired(true));

                add(new Button("save", new ResourceModel("label"))
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit()
                    {
                        TagSet tagSet = TagSetDetailForm.this.getModelObject();

                        if (tagSet.getId() == 0) {
                            try {
                                annotationService.getTagSet(tagSet.getType(),
                                        projectDetailForm.getModelObject());
                                error("Only one tagset per type per project is allowed!");
                                LOG.error("Only one tagset per type per project is allowed!");
                            }
                            catch (NoResultException ex) {

                                String username = SecurityContextHolder.getContext()
                                        .getAuthentication().getName();
                                User user = projectRepository.getUser(username);

                                tagSet.setProject(projectDetailForm.getModelObject());
                                try {
                                    annotationService.createTagSet(tagSet, user);
                                }
                                catch (IOException e) {
                                    error("unable to create Log file while creating the TagSet"
                                            + ":" + ExceptionUtils.getRootCauseMessage(e));
                                }
                                TagSetDetailForm.this.setModelObject(tagSet);
                                tagSelectionForm.setVisible(true);
                                tagDetailForm.setVisible(true);
                            }
                        }
                    }
                });

                add(new Button("remove", new ResourceModel("label"))
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit()
                    {
                        TagSet tagSet = TagSetDetailForm.this.getModelObject();
                        if (tagSet.getId() != 0) {
                            annotationService.removeTagSet(tagSet);
                            TagSetDetailForm.this.setModelObject(null);
                            tagSelectionForm.setVisible(false);
                            tagDetailForm.setVisible(false);
                        }
                        TagSetDetailForm.this.setModelObject(new TagSet());
                    }
                });

                add(fileUpload = new FileUploadField("content", new Model()));
                add(new Button("import", new ResourceModel("label"))
                {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit()
                    {
                        uploadedFiles = fileUpload.getFileUploads();
                        Project project = projectDetailForm.getModelObject();

                        if (isNotEmpty(uploadedFiles)) {
                            for (FileUpload tagFile : uploadedFiles) {
                                InputStream tagInputStream;
                                try {
                                    TagSet tagSet = TagSetDetailForm.this.getModelObject();
                                    if (tagSet.getId() != 0) {
                                        tagInputStream = tagFile.getInputStream();
                                        String text = IOUtils.toString(tagInputStream, "UTF-8");
                                        Map<String, String> mapOfTagsAndDescriptionsFromFile = ApplicationUtils
                                                .getTagsFromText(text);

                                        List<Tag> listOfTagsFromDatabase = annotationService
                                                .listTags(tagSet);
                                        Set<String> listOfTagsFromFile = mapOfTagsAndDescriptionsFromFile
                                                .keySet();

                                        Set<String> listOfTagNamesFromDatabse = new HashSet<String>();

                                        for (Tag tag : listOfTagsFromDatabase) {
                                            listOfTagNamesFromDatabse.add(tag.getName());
                                        }

                                        listOfTagsFromFile.removeAll(listOfTagNamesFromDatabse);

                                        for (String tagName : listOfTagsFromFile) {

                                            String username = SecurityContextHolder.getContext()
                                                    .getAuthentication().getName();
                                            User user = projectRepository.getUser(username);

                                            Tag tag = new Tag();
                                            tag.setDescription(mapOfTagsAndDescriptionsFromFile
                                                    .get(tagName));
                                            tag.setName(tagName);
                                            tag.setTagSet(tagSet);
                                            annotationService.createTag(tag, user);
                                        }
                                    }
                                    else {
                                        error("Please save TagSet first!");
                                    }
                                }
                                catch (Exception e) {
                                    error("Error uploading document "
                                            + ExceptionUtils.getRootCauseMessage(e));
                                    LOG.error("Error uploading document "
                                            + ExceptionUtils.getRootCauseMessage(e));
                                }
                            }
                        }
                        else if (isEmpty(uploadedFiles)) {
                            error("No document is selected to upload, please select a document first");
                            LOG.info("No document is selected to upload, please select a document first");
                        }
                        else if (project.getId() == 0) {
                            error("Project not yet created, please save project Details!");
                            LOG.info("Project not yet created, please save project Details!");
                        }

                    }
                });

                add(new DownloadLink("export", new LoadableDetachableModel<File>()
                {
                    private static final long serialVersionUID = 840863954694163375L;

                    @Override
                    protected File load()
                    {
                        File downloadFile = null;
                        try {
                            downloadFile = File.createTempFile("exportedtagsets", ".json");
                        }
                        catch (IOException e1) {
                            error("Unable to create temporary File!!");

                        }
                        if (projectDetailForm.getModelObject().getId() == 0) {
                            error("Project not yet created. Please save project details first!");
                        }
                        else {
                            List<ExportedTagSetContent> exportedTagSetscontent = new ArrayList<ExportedTagSetContent>();
                            TagSet tagSet = tagSetDetailForm.getModelObject();
                            ExportedTagSetContent exportedTagSetContent = new ExportedTagSetContent();
                            exportedTagSetContent.setDescription(tagSet.getDescription());
                            exportedTagSetContent.setLanguage(tagSet.getLanguage());
                            exportedTagSetContent.setName(tagSet.getName());

                            exportedTagSetContent.setType(tagSet.getType().getType());
                            exportedTagSetContent.setTypeName(tagSet.getType().getName());
                            exportedTagSetContent.setTypeDescription(tagSet.getType()
                                    .getDescription());

                            List<de.tudarmstadt.ukp.clarin.webanno.export.model.Tag> exportedTags = new ArrayList<de.tudarmstadt.ukp.clarin.webanno.export.model.Tag>();
                            for (Tag tag : annotationService.listTags(tagSet)) {
                                de.tudarmstadt.ukp.clarin.webanno.export.model.Tag exportedTag = new de.tudarmstadt.ukp.clarin.webanno.export.model.Tag();
                                exportedTag.setDescription(tag.getDescription());
                                exportedTag.setName(tag.getName());
                                exportedTags.add(exportedTag);

                            }
                            exportedTagSetContent.setTags(exportedTags);
                            exportedTagSetscontent.add(exportedTagSetContent);
                            ExportedTagSets exportedTagSet = new ExportedTagSets();
                            exportedTagSet.setTagSets(exportedTagSetscontent);
                            CasToBratJson exportedTagSets = new CasToBratJson();
                            MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
                            exportedTagSets.setJsonConverter(jsonConverter);

                            try {
                                exportedTagSets.generateBratJson(exportedTagSet, downloadFile);
                            }
                            catch (IOException e) {
                                error("File Path not found or No permision to save the file!");
                            }
                            info("TagSets successfully exported to :"
                                    + downloadFile.getAbsolutePath());

                        }
                        return downloadFile;
                    }
                }).setOutputMarkupId(true));

            }
        }

        private class TagDetailForm
            extends Form<Tag>
        {
            private static final long serialVersionUID = -1L;

            public TagDetailForm(String id)
            {
                super(id, new CompoundPropertyModel<Tag>(new EntityModel<Tag>(new Tag())));
                // super(id);
                add(new TextField<String>("name").setRequired(true));

                add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
                add(new Button("save", new ResourceModel("label"))
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit()
                    {
                        Tag tag = TagDetailForm.this.getModelObject();
                        if (tag.getId() == 0) {
                            tag.setTagSet(tagSetDetailForm.getModelObject());
                            try {
                                annotationService.getTag(tag.getName(),
                                        tagSetDetailForm.getModelObject());
                                error("This tag is already added for this tagset!");
                                LOG.error("This tag is already added for this tagset!");
                            }
                            catch (NoResultException ex) {

                                String username = SecurityContextHolder.getContext()
                                        .getAuthentication().getName();
                                User user = projectRepository.getUser(username);

                                try {
                                    annotationService.createTag(tag, user);
                                }
                                catch (IOException e) {
                                    error("unable to create a log file while creating the Tag "
                                            + ":" + ExceptionUtils.getRootCauseMessage(e));
                                }
                                tagDetailForm.setModelObject(new Tag());
                            }
                        }
                    }
                });

                add(new Button("new", new ResourceModel("label"))
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit()
                    {
                        TagDetailForm.this.setDefaultModelObject(new Tag());
                    }
                });

                add(new Button("remove", new ResourceModel("label"))
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit()
                    {
                        Tag tag = TagDetailForm.this.getModelObject();
                        if (tag.getId() != 0) {
                            tag.setTagSet(tagSetDetailForm.getModelObject());
                            annotationService.removeTag(tag);
                            tagDetailForm.setModelObject(new Tag());
                        }
                        else {
                            TagDetailForm.this.setModelObject(new Tag());
                        }
                    }
                });
            }
        }

        private class TagSelectionForm
            extends Form<SelectionModel>
        {
            private static final long serialVersionUID = -1L;

            public TagSelectionForm(String id)
            {
                // super(id);
                super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

                add(new ListChoice<Tag>("tag")
                {
                    private static final long serialVersionUID = 1L;

                    {
                        setChoices(new LoadableDetachableModel<List<Tag>>()
                        {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected List<Tag> load()
                            {
                                return annotationService
                                        .listTags(tagSetDetailForm.getModelObject());
                            }
                        });
                        setChoiceRenderer(new ChoiceRenderer<Tag>("name", "id"));
                        setNullValid(false);
                    }

                    @Override
                    protected void onSelectionChanged(Tag aNewSelection)
                    {
                        if (aNewSelection != null) {
                            // TagSelectionForm.this.getModelObject().tag = new Tag();
                            tagDetailForm.setModelObject(aNewSelection);
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

        public ProjectTagSetsPanel(String id)
        {
            super(id);
            tagSetSelectionForm = new TagSetSelectionForm("tagSetSelectionForm");

            tagSelectionForm = new TagSelectionForm("tagSelectionForm");
            tagSelectionForm.setVisible(false);

            tagSetDetailForm = new TagSetDetailForm("tagSetDetailForm");
            tagSetDetailForm.setVisible(false);

            tagDetailForm = new TagDetailForm("tagDetailForm");
            tagDetailForm.setVisible(false);

            add(tagSetSelectionForm);
            add(tagSelectionForm);
            add(tagSetDetailForm);
            add(tagDetailForm);
        }
    }

    private class AnnotationGuideLinePanel
        extends Panel
    {
        private static final long serialVersionUID = 2116717853865353733L;
        private ArrayList<String> documents = new ArrayList<String>();
        private ArrayList<String> selectedDocuments = new ArrayList<String>();

        private List<FileUpload> uploadedFiles;
        private FileUploadField fileUpload;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public AnnotationGuideLinePanel(String id)
        {
            super(id);
            add(fileUpload = new FileUploadField("content", new Model()));

            add(new Button("importGuideline", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    uploadedFiles = fileUpload.getFileUploads();
                    Project project = projectDetailForm.getModelObject();

                    if (isNotEmpty(uploadedFiles) && project.getId() != 0) {
                        for (FileUpload guidelineFile : uploadedFiles) {

                            try {
                                File tempFile = guidelineFile.writeToTempFile();
                                // String text = IOUtils.toString(tcfInputStream, "UTF-8");
                                String fileName = guidelineFile.getClientFileName();
                                projectRepository.writeGuideline(project, tempFile, fileName);
                            }
                            catch (IOException e) {
                                error("Unable to write guideline file "
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                        }
                    }
                    else if (isEmpty(uploadedFiles)) {
                        error("No document is selected to upload, please select a document first");
                        LOG.info("No document is selected to upload, please select a document first");
                    }
                    else if (project.getId() == 0) {
                        error("Project not yet created, please save project Details!");
                        LOG.info("Project not yet created, please save project Details!");
                    }

                }
            });

            add(new ListMultipleChoice<String>("documents", new Model(selectedDocuments), documents)
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<String>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<String> load()
                        {
                            Project project = projectDetailForm.getModelObject();
                            documents.clear();
                            if (project.getId() != 0) {
                                documents.addAll(projectRepository
                                        .listAnnotationGuidelineDocument(project));
                            }
                            return documents;
                        }
                    });
                }
            });

            add(new Button("remove", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Project project = projectDetailForm.getModelObject();
                    for (String document : selectedDocuments) {
                        try {

                            projectRepository.removeAnnotationGuideline(project, document);
                        }
                        catch (IOException e) {
                            error("Error while removing a document document "
                                    + ExceptionUtils.getRootCauseMessage(e));
                            LOG.error("Error while removing a document document "
                                    + ExceptionUtils.getRootCauseMessage(e));
                        }
                        documents.remove(document);
                    }
                }
            });
        }
    }
}
