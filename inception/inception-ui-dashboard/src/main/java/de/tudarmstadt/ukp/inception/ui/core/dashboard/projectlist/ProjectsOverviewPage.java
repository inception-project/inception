/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_PROJECT_CREATOR;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.HtmlElementEvents.INPUT_EVENT;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.HtmlElementEvents.KEYDOWN_EVENT;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.KeyCodes.ENTER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.project.ProjectSettingsPage.NEW_PROJECT_ID;
import static java.lang.String.join;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;
import static org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy.authorize;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Classes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.AjaxProjectImportedEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.ProjectImportPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.ProjectSettingsPage;
import de.tudarmstadt.ukp.inception.annotation.filters.ProjectRoleFilterPanel;
import de.tudarmstadt.ukp.inception.annotation.filters.ProjectRoleFilterStateChanged;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;

@MountPath(value = "/")
public class ProjectsOverviewPage
    extends ApplicationPageBase
{
    private static final String MID_CREATED = "created";
    private static final String MID_NAME = "name";
    private static final String MID_DESCRIPTION = "description";
    private static final String MID_PROJECT_LINK = "projectLink";
    private static final String MID_LABEL = "label";
    private static final String MID_ROLE = "role";
    private static final String MID_ROLE_FILTER = "roleFilter";
    private static final String MID_PROJECTS = "projects";
    private static final String MID_PROJECT = "project";
    private static final String MID_ID = "id";
    private static final String MID_IMPORT_PROJECT_PANEL = "importProjectPanel";
    private static final String MID_NEW_PROJECT = "newProject";
    private static final String MID_LEAVE_PROJECT = "leaveProject";
    private static final String MID_CONFIRM_LEAVE = "confirmLeave";
    private static final String MID_EMPTY_LIST_LABEL = "emptyListLabel";
    private static final String MID_START_TUTORIAL = "startTutorial";
    private static final String MID_IMPORT_PROJECT_BUTTON = "importProjectBtn";
    private static final String MID_PAGING_NAVIGATOR = "pagingNavigator";

    private static final long serialVersionUID = -2159246322262294746L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectsOverviewPage.class);

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectExportService exportService;

    private IModel<List<ProjectEntry>> allAccessibleProjects;
    private IModel<User> currentUser;

    private WebMarkupContainer projectListContainer;
    private DataView<ProjectEntry> projectList;
    private PagingNavigator navigator;
    private ConfirmationDialog confirmLeaveDialog;
    private Label noProjectsNotice;
    private TextField<String> nameFilter;

    private Set<Long> highlightedProjects = new HashSet<>();
    private ProjectListDataProvider dataProvider;

    public ProjectsOverviewPage()
    {
        currentUser = LoadableDetachableModel.of(userRepository::getCurrentUser);
        allAccessibleProjects = LoadableDetachableModel.of(this::loadProjects);

        dataProvider = new ProjectListDataProvider(allAccessibleProjects);

        fastTrackAnnotatorsToProject();

        projectListContainer = new WebMarkupContainer(MID_PROJECTS);
        projectListContainer.add(visibleWhen(() -> dataProvider.size() > 0));
        projectListContainer.setOutputMarkupPlaceholderTag(true);
        queue(projectListContainer);

        queue(projectList = createProjectList(MID_PROJECT, dataProvider));

        queue(navigator = createPagingNavigator(MID_PAGING_NAVIGATOR, projectList));

        noProjectsNotice = new Label(MID_EMPTY_LIST_LABEL,
                LoadableDetachableModel.of(this::getNoProjectsMessage));
        noProjectsNotice.add(visibleWhen(() -> dataProvider.size() == 0));
        noProjectsNotice.setOutputMarkupPlaceholderTag(true);
        queue(noProjectsNotice);

        add(createStartTutorialLink()); // Must be add instead of queue otherwise there is an error
        queue(createProjectCreationGroup());
        queue(createProjectImportGroup());
        queue(new ProjectRoleFilterPanel(MID_ROLE_FILTER,
                () -> dataProvider.getFilterState().getRoles()));

        nameFilter = new TextField<>("nameFilter",
                PropertyModel.of(dataProvider.getFilterState(), "projectName"), String.class);
        nameFilter.setOutputMarkupPlaceholderTag(true);
        nameFilter.add(
                new LambdaAjaxFormComponentUpdatingBehavior(INPUT_EVENT, this::actionApplyFilter)
                        .withDebounce(ofMillis(200)));
        nameFilter.add(new LambdaAjaxFormComponentUpdatingBehavior(KEYDOWN_EVENT, this::actionOpen)
                .withKeyCode(ENTER));
        nameFilter.add(visibleWhen(() -> !allAccessibleProjects.getObject().isEmpty()));
        queue(nameFilter);

        queue(confirmLeaveDialog = new ConfirmationDialog(MID_CONFIRM_LEAVE,
                new StringResourceModel("leaveDialog.title", this),
                new StringResourceModel("leaveDialog.text", this)));
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        WicketUtil.ajaxFallbackFocus(aResponse, nameFilter);
    }

    @OnEvent
    public void onProjectRoleFilterStateChanged(ProjectRoleFilterStateChanged aEvent)
    {

        actionApplyFilter(aEvent.getTarget());
    }

    private void actionApplyFilter(AjaxRequestTarget aTarget)
    {
        // Force navigator to clear internal item count cache
        navigator.detach();
        aTarget.add(projectListContainer, navigator, noProjectsNotice);
    }

    private void actionOpen(AjaxRequestTarget aTarget)
    {
        if (dataProvider.size() == 1) {
            var project = dataProvider.iterator(0, 1).next().getProject();
            PageParameters pageParameters = new PageParameters();
            ProjectPageBase.setProjectPageParameter(pageParameters, project);
            setResponsePage(ProjectDashboardPage.class, pageParameters);
        }
    }

    private BootstrapAjaxPagingNavigator createPagingNavigator(String aId,
            DataView<ProjectEntry> aProjectList)
    {
        var pagingNavigator = new BootstrapAjaxPagingNavigator(aId, aProjectList)
        {
            private static final long serialVersionUID = 853561772299520056L;

            @Override
            protected void onAjaxEvent(AjaxRequestTarget aTarget)
            {
                super.onAjaxEvent(aTarget);
                // aTarget.add(numberOfResults);
            }
        };
        pagingNavigator.setOutputMarkupPlaceholderTag(true);
        pagingNavigator.add(LambdaBehavior
                .onConfigure(() -> pagingNavigator.getPagingNavigation().setViewSize(10)));
        pagingNavigator.add(visibleWhen(() -> aProjectList.getItemCount() > 0));
        return pagingNavigator;
    }

    private WebMarkupContainer createProjectCreationGroup()
    {
        WebMarkupContainer projectCreationGroup = new WebMarkupContainer("projectCreationGroup");
        authorize(projectCreationGroup, RENDER,
                join(",", ROLE_ADMIN.name(), ROLE_PROJECT_CREATOR.name()));
        projectCreationGroup.add(createNewProjectLink());
        projectCreationGroup.add(createQuickProjectCreationDropdown());
        return projectCreationGroup;
    }

    private WebMarkupContainer createProjectImportGroup()
    {
        WebMarkupContainer projectImportGroup = new WebMarkupContainer("projectImportGroup");
        authorize(projectImportGroup, RENDER,
                join(",", ROLE_ADMIN.name(), ROLE_PROJECT_CREATOR.name()));
        projectImportGroup.add(
                new Label(MID_IMPORT_PROJECT_BUTTON, new StringResourceModel("importProject")));
        projectImportGroup.add(new ProjectImportPanel(MID_IMPORT_PROJECT_PANEL, Model.of()));
        return projectImportGroup;
    }

    private boolean canCreateProjects(User aCurrentUser)
    {
        return aCurrentUser.getRoles().contains(ROLE_PROJECT_CREATOR)
                || aCurrentUser.getRoles().contains(Role.ROLE_ADMIN);
    }

    private String getNoProjectsMessage()
    {
        if (!allAccessibleProjects.getObject().isEmpty() && dataProvider.size() == 0) {
            return getString("noProjects.noFilterMatch");
        }

        if (canCreateProjects(currentUser.getObject())) {
            return getString("noProjects.projectCreator");
        }

        return getString("noProjects");
    }

    /**
     * If a user is not an admin or project creator and only has access to a single project then
     * redirect them to their only project immediately. They do not need the project overview page.
     */
    private void fastTrackAnnotatorsToProject()
    {
        if (canCreateProjects(currentUser.getObject())) {
            return;
        }

        List<ProjectEntry> projects = allAccessibleProjects.getObject();
        if (projects.size() == 1) {
            Project soleAccessibleProject = projects.get(0).getProject();
            PageParameters pageParameters = new PageParameters();
            ProjectPageBase.setProjectPageParameter(pageParameters, soleAccessibleProject);
            throw new RestartResponseException(ProjectDashboardPage.class, pageParameters);
        }
    }

    private WebMarkupContainer createQuickProjectCreationDropdown()
    {
        ListView<ProjectInitializer> initializers = new ListView<ProjectInitializer>("templates",
                LambdaModel.of(this::listInitializers))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ProjectInitializer> aItem)
            {
                LambdaAjaxLink link = new LambdaAjaxLink("createProjectUsingTemplate",
                        _target -> actionCreateProject(_target, aItem.getModelObject()));
                link.add(new Label("name", Model.of(aItem.getModelObject().getName())));
                aItem.add(link);
            }
        };

        WebMarkupContainer initializersContainer = new WebMarkupContainer("templatesContainer");
        initializersContainer.setOutputMarkupId(true);
        initializersContainer.add(initializers);
        return initializersContainer;
    }

    private List<ProjectInitializer> listInitializers()
    {
        return projectService.listProjectInitializers().stream()
                .filter(initializer -> initializer instanceof QuickProjectInitializer)
                .sorted(comparing(ProjectInitializer::getName)) //
                .collect(toList());
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

    private LambdaAjaxLink createStartTutorialLink()
    {
        LambdaAjaxLink startTutorialLink = new LambdaAjaxLink(MID_START_TUTORIAL,
                this::startTutorial);
        startTutorialLink.add(visibleWhen(() -> {
            return userRepository.isAdministrator(currentUser.getObject())
                    || userRepository.isProjectCreator(currentUser.getObject());
        }));

        add(startTutorialLink);

        return startTutorialLink;
    }

    private void startTutorial(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(" startTutorial(); ");
    }

    private DataView<ProjectEntry> createProjectList(String aId,
            ProjectListDataProvider aDataProvider)
    {
        return new DataView<ProjectEntry>(aId, aDataProvider, 25)
        {
            private static final long serialVersionUID = -755155675319764642L;

            @Override
            protected void populateItem(Item<ProjectEntry> aItem)
            {
                Project project = aItem.getModelObject().getProject();

                PageParameters pageParameters = new PageParameters();
                ProjectPageBase.setProjectPageParameter(pageParameters, project);
                BookmarkablePageLink<Void> projectLink = new BookmarkablePageLink<>(
                        MID_PROJECT_LINK, ProjectDashboardPage.class, pageParameters);
                projectLink.add(new Label(MID_NAME, aItem.getModelObject().getName()));
                aItem.add(new Label(MID_DESCRIPTION, aItem.getModelObject().getShortDescription())
                        .setEscapeModelStrings(false));

                Label createdLabel = new Label(MID_CREATED,
                        () -> project.getCreated() != null ? formatDate(project.getCreated())
                                : null);
                addActionsDropdown(aItem);
                aItem.add(projectLink);
                createdLabel.add(visibleWhen(() -> createdLabel.getDefaultModelObject() != null));
                aItem.add(createdLabel);
                aItem.add(createRoleBadges(aItem.getModelObject()));
                Label projectId = new Label(MID_ID, () -> project.getId());
                projectId.add(visibleWhen(
                        () -> DEVELOPMENT.equals(getApplication().getConfigurationType())));
                aItem.add(projectId);
                aItem.add(new ClassAttributeModifier()
                {
                    private static final long serialVersionUID = -5391276660500827257L;

                    @Override
                    protected Set<String> update(Set<String> aClasses)
                    {
                        if (highlightedProjects.contains(project.getId())) {
                            aClasses.add("border border-primary");
                        }
                        else {
                            aClasses.remove("border border-primary");
                        }
                        return aClasses;
                    }
                });
            }
        };
    }

    private void addActionsDropdown(ListItem<ProjectEntry> aItem)
    {
        ProjectEntry projectEntry = aItem.getModelObject();
        Project project = projectEntry.getProject();

        WebMarkupContainer container = new WebMarkupContainer("actionDropdown");

        LambdaAjaxLink leaveProjectLink = new LambdaAjaxLink(MID_LEAVE_PROJECT,
                _target -> actionConfirmLeaveProject(_target, project));
        leaveProjectLink.add(visibleWhen(() -> !projectEntry.getLevels().isEmpty()
                && !projectEntry.getLevels().contains(MANAGER)));

        container.add(leaveProjectLink);

        // If there are no active items in the dropdown, then do not show the dropdown. However,
        // to still make it take up the usual space and keep the overview nicely aligned, we use
        // the "invisible" CSS class here instead of telling Wicket to not render the dropdown
        container.add(new CssClassNameAppender(LoadableDetachableModel
                .of(() -> container.streamChildren().anyMatch(Component::isVisible) ? ""
                        : "invisible")));

        aItem.add(container);
    }

    private void actionConfirmLeaveProject(AjaxRequestTarget aTarget, Project aProject)
    {
        confirmLeaveDialog.setConfirmAction((_target) -> {
            projectService.revokeAllRoles(aProject, currentUser.getObject());
            _target.add(projectListContainer);
            _target.addChildren(getPage(), IFeedback.class);
            success("You are no longer a member of project [" + aProject.getName() + "]");
        });
        confirmLeaveDialog.show(aTarget);
    }

    private ListView<PermissionLevel> createRoleBadges(ProjectEntry aProjectEntry)
    {
        var levels = aProjectEntry.getLevels();
        Collections.reverse(levels);
        return new ListView<PermissionLevel>(MID_ROLE, levels)
        {
            private static final long serialVersionUID = -96472758076828409L;

            @Override
            protected void populateItem(ListItem<PermissionLevel> aItem)
            {
                PermissionLevel level = aItem.getModelObject();
                aItem.add(new Label(MID_LABEL, getString(
                        Classes.simpleName(level.getDeclaringClass()) + '.' + level.toString())));
            }
        };
    }

    private void actionCreateProject(AjaxRequestTarget aTarget)
    {
        PageParameters params = new PageParameters();
        params.set(PAGE_PARAM_PROJECT, NEW_PROJECT_ID);
        setResponsePage(ProjectSettingsPage.class, params);
    }

    private void actionCreateProject(AjaxRequestTarget aTarget, ProjectInitializer aInitializer)
    {
        User user = currentUser.getObject();
        aTarget.addChildren(getPage(), IFeedback.class);
        String projectSlug = projectService.deriveSlugFromName(user.getUsername());
        projectSlug = projectService.deriveUniqueSlug(projectSlug);

        try {
            Project project = new Project(projectSlug);
            project.setName(user.getUsername() + " - New project");
            projectService.createProject(project);
            projectService.assignRole(project, user, ANNOTATOR, CURATOR, MANAGER);
            projectService.initializeProject(project, asList(aInitializer));

            PageParameters pageParameters = new PageParameters();
            ProjectPageBase.setProjectPageParameter(pageParameters, project);
            setResponsePage(ProjectDashboardPage.class, pageParameters);
        }
        catch (IOException e) {
            LOG.error("Unable to create project [{}]", projectSlug, e);
            error("Unable to create project [" + projectSlug + "]");
        }
    }

    @OnEvent
    public void onProjectImported(AjaxProjectImportedEvent aEvent)
    {
        List<Project> projects = aEvent.getProjects();

        if (projects.size() > 1) {
            aEvent.getTarget().add(projectListContainer);
            highlightedProjects.clear();
            projects.stream().forEach(p -> highlightedProjects.add(p.getId()));
            projects.stream()
                    .forEach(p -> success("Project [" + p.getName() + "] successfully imported"));
            aEvent.getTarget().addChildren(getPage(), IFeedback.class);
            return;
        }

        if (!projects.isEmpty()) {
            Project project = projects.get(0);
            getSession().success("Project [" + project.getName() + "] successfully imported");
            PageParameters pageParameters = new PageParameters();
            ProjectPageBase.setProjectPageParameter(pageParameters, project);
            setResponsePage(ProjectDashboardPage.class, pageParameters);
        }
    }

    private static String formatDate(Date aTime)
    {
        return new SimpleDateFormat("yyyy-MM-dd").format(aTime);
    }

    private List<ProjectEntry> loadProjects()
    {
        return projectService.listAccessibleProjectsWithPermissions(currentUser.getObject())
                .entrySet().stream() //
                .map(e -> new ProjectEntry(e.getKey(), e.getValue()))
                .sorted(comparing(ProjectEntry::getName)) //
                .collect(toList());
    }
}
