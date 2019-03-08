/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_PROJECT_CREATOR;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.ui.core.session.SessionMetaData.CURRENT_PROJECT;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy.authorize;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Classes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.datetime.markup.html.basic.DateLabel;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInputField;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.ImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaStatelessLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.ProjectPage;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;

@MountPath(value = "/projects.html")
public class ProjectsOverviewPage
    extends ApplicationPageBase
{
    private static final String MID_CREATED = "created";
    private static final String MID_NAME = "name";
    private static final String MID_PROJECT_LINK = "projectLink";
    private static final String MID_LABEL = "label";
    private static final String MID_ROLE = "role";
    private static final String MID_ROLE_FILTER = "roleFilter";
    private static final String MID_PROJECTS = "projects";
    private static final String MID_PROJECT = "project";
    private static final String MID_IMPORT_PROJECT_FORM = "importProjectForm";
    private static final String MID_NEW_PROJECT = "newProject";
    private static final String MID_PROJECT_ARCHIVE_UPLOAD = "projectArchiveUpload";
    private static final String MID_LEAVE_PROJECT = "leaveProject";
	private static final String MID_CONFIRM_LEAVE = "confirmLeave";

    private static final long serialVersionUID = -2159246322262294746L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectsOverviewPage.class);
    
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectExportService exportService;
    
    private BootstrapFileInputField fileUpload;
    private WebMarkupContainer projectListContainer;
    private WebMarkupContainer roleFilters;
    private IModel<Set<PermissionLevel>> activeRoleFilters;
    
    public ProjectsOverviewPage()
    {
        add(projectListContainer = createProjectList());
        add(createNewProjectLink());
        add(createImportProjectForm());
        add(roleFilters = createRoleFilters());
        
        activeRoleFilters = Model.ofSet(new HashSet<>());
    }
    
    private LambdaAjaxLink createNewProjectLink()
    {
        LambdaAjaxLink newProjectLink = new LambdaAjaxLink(MID_NEW_PROJECT,
                this::actionCreateProject);
        
        add(newProjectLink);
        
        authorize(newProjectLink, RENDER,
                join(",", ROLE_ADMIN.name(), ROLE_PROJECT_CREATOR.name()));
        
        return newProjectLink;
    }
    
    private Form<Void> createImportProjectForm()
    {
        Form<Void> importProjectForm = new Form<>(MID_IMPORT_PROJECT_FORM);
        
        FileInputConfig config = new FileInputConfig();
        config.initialCaption("Import project archives ...");
        config.allowedFileExtensions(asList("zip"));
        config.showPreview(false);
        config.showUpload(true);
        config.removeIcon("<i class=\"fa fa-remove\"></i>");
        config.uploadIcon("<i class=\"fa fa-upload\"></i>");
        config.browseIcon("<i class=\"fa fa-folder-open\"></i>");
        importProjectForm.add(fileUpload = new BootstrapFileInputField(MID_PROJECT_ARCHIVE_UPLOAD,
                new ListModel<>(), config)
        {
            private static final long serialVersionUID = -6794141937368512300L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                actionImport(aTarget, null);
            }
        });
        
        authorize(importProjectForm, RENDER,
                join(",", ROLE_ADMIN.name(), ROLE_PROJECT_CREATOR.name()));
    
        return importProjectForm;
    }
    
    private WebMarkupContainer createProjectList()
    {
        ListView<Project> listview = new ListView<Project>(MID_PROJECT,
                LoadableDetachableModel.of(this::listProjects))
        {
            private static final long serialVersionUID = -755155675319764642L;

            @Override
            protected void populateItem(ListItem<Project> aItem)
            {
                LambdaStatelessLink projectLink = new LambdaStatelessLink(MID_PROJECT_LINK, () -> 
                        selectProject(aItem.getModelObject()));
                projectLink.add(new Label(MID_NAME, aItem.getModelObject().getName()));
                DateLabel createdLabel = DateLabel.forDatePattern(MID_CREATED, () -> 
                        aItem.getModelObject().getCreated(), "yyyy-MM-dd");
                ConfirmActionDialog confirmLeaveDialog = new ConfirmActionDialog(MID_CONFIRM_LEAVE,
                        "Leave Project");
                confirmLeaveDialog.setConfirmAction((target) -> {
                    // TODO use ProjectServiceImpl (?) to remove user
                });
                AjaxLink<Void> leaveProjectLink = new AjaxLink<Void>(MID_LEAVE_PROJECT)
                {

                    private static final long serialVersionUID = -6496680664449646359L;

                    @Override
                    public void onClick(AjaxRequestTarget target)
                    {
                        confirmLeaveDialog.show(target);
                    }

                };
                createdLabel.add(visibleWhen(() -> createdLabel.getModelObject() != null));
                aItem.add(createdLabel);
                aItem.add(projectLink);
                aItem.add(createRoleBadges(aItem.getModelObject()));
                aItem.add(leaveProjectLink);
                aItem.add(confirmLeaveDialog);
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();

                if (getModelObject().isEmpty()) {
                    warn("There are no projects accessible to you matching the filter criteria.");
                }
            }
        };
        
        WebMarkupContainer projectList = new WebMarkupContainer(MID_PROJECTS);
        projectList.setOutputMarkupPlaceholderTag(true);
        projectList.add(listview);
        
        return projectList;
    }

	private WebMarkupContainer createRoleFilters()
    {
        ListView<PermissionLevel> listview = new ListView<PermissionLevel>(MID_ROLE_FILTER,
                asList(PermissionLevel.values()))
        {
            private static final long serialVersionUID = -4762585878276156468L;

            @Override
            protected void populateItem(ListItem<PermissionLevel> aItem)
            {
                PermissionLevel level = aItem.getModelObject();
                LambdaAjaxLink link = new LambdaAjaxLink("roleFilterLink", _target -> 
                        actionApplyRoleFilter(_target, aItem.getModelObject()));
                link.add(new Label(MID_LABEL, getString(
                        Classes.simpleName(level.getDeclaringClass()) + '.' + level.toString())));
                link.add(new AttributeAppender("class", () -> 
                        activeRoleFilters.getObject().contains(aItem.getModelObject())
                        ? "active" : "", " "));
                aItem.add(link);
            }
        };

        WebMarkupContainer container = new WebMarkupContainer("roleFilters");
        container.setOutputMarkupPlaceholderTag(true);
        container.add(listview);

        return container;
    }

    private ListView<ProjectPermission> createRoleBadges(Project aProject)
    {
        return new ListView<ProjectPermission>(MID_ROLE, projectService
                .listProjectPermissionLevel(userRepository.getCurrentUser(), aProject))
        {
            private static final long serialVersionUID = -96472758076828409L;

            @Override
            protected void populateItem(ListItem<ProjectPermission> aItem)
            {
                PermissionLevel level = aItem.getModelObject().getLevel();
                aItem.add(new Label(MID_LABEL, getString(
                        Classes.simpleName(level.getDeclaringClass()) + '.' + level.toString())));
            }
        };
    }
    
    private void actionApplyRoleFilter(AjaxRequestTarget aTarget, PermissionLevel aPermission)
    {
        Set<PermissionLevel> activeRoles = activeRoleFilters.getObject();
        if (activeRoles.contains(aPermission)) {
            activeRoles.remove(aPermission);
        }
        else {
            activeRoles.add(aPermission);
        }
        
        aTarget.add(projectListContainer, roleFilters);
        aTarget.addChildren(getPage(), IFeedback.class);
    }
    
    private void selectProject(Project aProject)
    {
        Session.get().setMetaData(CURRENT_PROJECT, aProject);
        setResponsePage(ProjectDashboardPage.class);
    }
    
    private List<Project> listProjects()
    {
        User currentUser = userRepository.getCurrentUser();
        
        return projectService.listAccessibleProjects(currentUser)
                .stream()
                .filter(proj -> 
                        // If no filters are selected, all projects are listed
                        activeRoleFilters.getObject().isEmpty() ||
                        // ... otherwise only those projects are listed that match the filter
                        projectService.getProjectPermissionLevels(currentUser, proj)
                                .stream()
                                .anyMatch(activeRoleFilters.getObject()::contains))
                .collect(Collectors.toList());
    }
    
    private void actionCreateProject(AjaxRequestTarget aTarget)
    {
        PageParameters params = new PageParameters();
        params.set(WebAnnoConst.PAGE_PARAM_PROJECT_ID, ProjectPage.NEW_PROJECT_ID);
        setResponsePage(ProjectPage.class, params);        
    }
    
    private void actionImport(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        aTarget.addChildren(getPage(), IFeedback.class);
        
        List<FileUpload> exportedProjects = fileUpload.getFileUploads();
        for (FileUpload exportedProject : exportedProjects) {
            try {
                // Workaround for WICKET-6425
                File tempFile = File.createTempFile("project-import", null);
                try (
                        InputStream is = new BufferedInputStream(exportedProject.getInputStream());
                        OutputStream os = new FileOutputStream(tempFile);
                ) {
                    if (!ZipUtils.isZipStream(is)) {
                        throw new IOException("Invalid ZIP file");
                    }
                    IOUtils.copyLarge(is, os);
                    
                    if (!ImportUtil.isZipValidWebanno(tempFile)) {
                        throw new IOException("ZIP file is not a valid project archive");
                    }
                    
                    ProjectImportRequest request = new ProjectImportRequest(false, false,
                            Optional.of(userRepository.getCurrentUser()));
                    Project importedProject = exportService.importProject(request,
                            new ZipFile(tempFile));
                    
                    success("Imported project: " + importedProject.getName());
                }
                finally {
                    tempFile.delete();
                }
            }
            catch (Exception e) {
                error("Error importing project: " + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Error importing project", e);
            }
        }
        
        // After importing new projects, they should be visible in the overview, but we do not
        // redirect to the imported project. Could do that... maybe could do that if only a single
        // project was imported. Could also highlight the freshly imported projects somehow.
        
        aTarget.add(projectListContainer);
    }
}
