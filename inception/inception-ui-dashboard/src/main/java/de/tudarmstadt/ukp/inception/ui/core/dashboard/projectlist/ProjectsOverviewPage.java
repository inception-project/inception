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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_PROJECT_CREATOR;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.INPUT_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.KEYDOWN_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.KeyCodes.ENTER;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenModelIsNotNull;
import static java.lang.String.join;
import static java.lang.invoke.MethodHandles.lookup;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.containsAny;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;
import static org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy.authorize;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Classes;
import org.slf4j.Logger;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.AjaxProjectImportedEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.ProjectImportPanel;
import de.tudarmstadt.ukp.inception.annotation.filters.ProjectRoleFilterPanel;
import de.tudarmstadt.ukp.inception.annotation.filters.ProjectRoleFilterStateChanged;
import de.tudarmstadt.ukp.inception.annotation.filters.ProjectStateFilterPanel;
import de.tudarmstadt.ukp.inception.annotation.filters.ProjectStateFilterStateChanged;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectAccess;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.support.markdown.TerseMarkdownLabel;
import de.tudarmstadt.ukp.inception.support.wicket.SanitizingHtmlLabel;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLabel;
import de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil;
import de.tudarmstadt.ukp.inception.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.inception.ui.core.config.DashboardProperties;
import de.tudarmstadt.ukp.inception.ui.core.config.ProjectUiProperties;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;

@MountPath(value = "/")
public class ProjectsOverviewPage
    extends ApplicationPageBase
{
    public static final PreferenceKey<ProjectListSortState> KEY_PROJECT_LIST_SORT_MODE = new PreferenceKey<>(
            ProjectListSortState.class, "project-overview/project-list-sort-mode");

    private static final String MID_CREATED = "created";
    private static final String MID_UPDATED = "updated";
    private static final String MID_NAME = "name";
    private static final String MID_DESCRIPTION = "description";
    private static final String MID_PROJECT_LINK = "projectLink";
    private static final String MID_LABEL = "label";
    private static final String MID_ROLE = "role";
    private static final String MID_ROLE_FILTER = "roleFilter";
    private static final String MID_STATE_FILTER = "stateFilter";
    private static final String MID_PROJECTS = "projects";
    private static final String MID_PROJECT = "project";
    private static final String MID_ID = "id";
    private static final String MID_IMPORT_PROJECT_PANEL = "importProjectPanel";
    private static final String MID_NEW_PROJECT = "newProject";
    private static final String MID_LEAVE_PROJECT = "leaveProject";
    private static final String MID_DIALOG = "dialog";
    private static final String MID_EMPTY_LIST_LABEL = "emptyListLabel";
    private static final String MID_START_TUTORIAL = "startTutorial";
    private static final String MID_PAGING_NAVIGATOR = "pagingNavigator";
    private static final String MID_TOGGLE_BULK_CHANGE = "toggleBulkChange";
    private static final String MID_BULK_ACTION_DROPDOWN_BUTTON = "bulkActionDropdownButton";
    private static final String MID_BULK_ACTION_DROPDOWN = "bulkActionDropdown";

    private static final long serialVersionUID = -2159246322262294746L;

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private @SpringBean ProjectAccess projectAccess;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectExportService exportService;
    private @SpringBean DashboardProperties dashboardProperties;
    private @SpringBean ProjectUiProperties projectUiProperties;
    private @SpringBean PreferencesService userPrefService;
    private @SpringBean DocumentService documentService;

    private IModel<List<ProjectEntry>> allAccessibleProjects;
    private IModel<User> currentUser;
    private IModel<ProjectListSortStrategy> sortStrategy;

    private WebMarkupContainer projectListContainer;
    private DataView<ProjectEntry> projectList;
    private PagingNavigator navigator;
    private BootstrapModalDialog dialog;
    private Label noProjectsNotice;
    private TextField<String> queryField;
    private LambdaAjaxLink newProjectLink;
    private LambdaAjaxLink toggleBulkChange;
    private boolean bulkChangeMode = false;
    private Label selectCount;

    private Set<Long> highlightedProjects = new HashSet<>();
    private ProjectListDataProvider dataProvider;

    private LambdaAjaxBehavior openCreateProjectDialogByDefaultBehavior;

    public ProjectsOverviewPage()
    {
        currentUser = LoadableDetachableModel.of(userRepository::getCurrentUser);
        allAccessibleProjects = LoadableDetachableModel.of(this::loadProjects);

        var user = userRepository.getCurrentUser();
        sortStrategy = new LambdaModelAdapter.Builder<ProjectListSortStrategy>() //
                .getting(() -> userPrefService.loadTraitsForUser(KEY_PROJECT_LIST_SORT_MODE,
                        user).strategy)
                .setting(v -> userPrefService.saveTraitsForUser(KEY_PROJECT_LIST_SORT_MODE, user,
                        new ProjectListSortState(v)))
                .build();

        dataProvider = new ProjectListDataProvider(allAccessibleProjects);
        dataProvider.setSort(sortStrategy.getObject().key, sortStrategy.getObject().order);

        fastTrackAnnotatorsToProject();

        projectListContainer = new WebMarkupContainer(MID_PROJECTS);
        projectListContainer.add(visibleWhen(() -> dataProvider.size() > 0));
        projectListContainer.setOutputMarkupPlaceholderTag(true);
        queue(projectListContainer);

        queue(projectList = createProjectList(MID_PROJECT, dataProvider));

        queue(navigator = createPagingNavigator(MID_PAGING_NAVIGATOR, projectList));

        noProjectsNotice = new SanitizingHtmlLabel(MID_EMPTY_LIST_LABEL,
                LoadableDetachableModel.of(this::getNoProjectsMessage));
        noProjectsNotice.add(visibleWhen(() -> dataProvider.size() == 0));
        noProjectsNotice.setOutputMarkupPlaceholderTag(true);
        queue(noProjectsNotice);

        add(createStartTutorialLink()); // Must be add instead of queue otherwise there is an error
        queue(createProjectCreationGroup());
        queue(createProjectImportGroup());
        queue(new ProjectRoleFilterPanel(MID_ROLE_FILTER,
                () -> dataProvider.getFilterState().getRoles()));
        queue(new ProjectStateFilterPanel(MID_STATE_FILTER,
                () -> dataProvider.getFilterState().getStates()));

        var sortOrder = new DropDownChoice<ProjectListSortStrategy>("sortOrder");
        sortOrder.setModel(sortStrategy);
        sortOrder.setChoiceRenderer(new EnumChoiceRenderer<>(sortOrder));
        sortOrder.setChoices(asList(ProjectListSortStrategy.values()));
        sortOrder.setNullValid(false);
        sortOrder.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, _target -> {
            var s = sortOrder.getModelObject();
            dataProvider.setSort(s.key, s.order);
            _target.add(projectListContainer);
        }));
        queue(sortOrder);

        queryField = new TextField<>("query",
                PropertyModel.of(dataProvider.getFilterState(), "query"), String.class);
        queryField.setOutputMarkupPlaceholderTag(true);
        queryField.add(
                new LambdaAjaxFormComponentUpdatingBehavior(INPUT_EVENT, this::actionApplyFilter)
                        .withDebounce(ofMillis(200)));
        queryField.add(new LambdaAjaxFormComponentUpdatingBehavior(KEYDOWN_EVENT, this::actionOpen)
                .withKeyCode(ENTER));
        queryField.add(visibleWhen(() -> !allAccessibleProjects.getObject().isEmpty()));
        queue(queryField);

        dialog = new BootstrapModalDialog(MID_DIALOG);
        dialog.trapFocus();
        queue(dialog);

        openCreateProjectDialogByDefaultBehavior = new LambdaAjaxBehavior(
                this::actionCreateProject);
        add(openCreateProjectDialogByDefaultBehavior);

        var selectionStatePanel = new WebMarkupContainer("selectionStatePanel");
        selectionStatePanel.add(visibleWhen(() -> bulkChangeMode));
        queue(selectionStatePanel);

        queue(new LambdaAjaxLink("selectAll", this::actionSelectAll));
        queue(new LambdaAjaxLink("selectNone", this::actionSelectNone));

        selectCount = new Label("selectCount", LambdaModel.of(() -> {
            return allAccessibleProjects.getObject().stream().filter(e -> e.isSelected()).count()
                    + " / " + allAccessibleProjects.getObject().size();
        }));
        selectCount.setOutputMarkupId(true);
        queue(selectCount);

        var sessionOwner = userRepository.getCurrentUser();
        toggleBulkChange = new LambdaAjaxLink(MID_TOGGLE_BULK_CHANGE, this::actionToggleBulkChange);
        toggleBulkChange.setOutputMarkupId(true);
        toggleBulkChange.setVisibilityAllowed(
                projectUiProperties.getBulkActions().anyActionsAccessible(sessionOwner));
        toggleBulkChange.add(new CssClassNameAppender(LoadableDetachableModel
                .of(() -> bulkChangeMode ? "btn-primary active" : "btn-outline-primary")));
        queue(toggleBulkChange);

        var bulkActionDropdown = new WebMarkupContainer(MID_BULK_ACTION_DROPDOWN);
        bulkActionDropdown.setVisibilityAllowed(
                projectUiProperties.getBulkActions().anyActionsAccessible(sessionOwner));
        bulkActionDropdown.add(visibleWhen(() -> bulkChangeMode));
        queue(bulkActionDropdown);

        var bulkActionDropdownButton = new WebMarkupContainer(MID_BULK_ACTION_DROPDOWN_BUTTON);
        bulkActionDropdownButton.setVisibilityAllowed(
                projectUiProperties.getBulkActions().anyActionsAccessible(sessionOwner));
        bulkActionDropdownButton.add(visibleWhen(() -> bulkChangeMode));
        queue(bulkActionDropdownButton);

        queue(new LambdaAjaxLink("bulkDelete", this::actionBulkDeleteProjects).setVisibilityAllowed(
                projectUiProperties.getBulkActions().getDelete().isAccessible(sessionOwner)));
    }

    private void actionSelectAll(AjaxRequestTarget aTarget)
    {
        dataProvider.iterator(0, dataProvider.size()).forEachRemaining(e -> e.setSelected(true));
        aTarget.add(this);
    }

    private void actionSelectNone(AjaxRequestTarget aTarget)
    {
        dataProvider.iterator(0, dataProvider.size()).forEachRemaining(e -> e.setSelected(false));
        aTarget.add(this);
    }

    private void actionBulkDeleteProjects(AjaxRequestTarget aTarget)
    {
        var selectedProjects = getSelectedProjects().stream().map(ProjectEntry::getProject)
                .toList();

        if (selectedProjects.isEmpty()) {
            info("No projects have been selected");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var dialogContent = new DeleteProjectConfirmationDialogContentPanel(ModalDialog.CONTENT_ID,
                Model.of(selectedProjects));

        if (selectedProjects.size() == 1) {
            dialogContent.setExpectedResponseModel(Model.of(selectedProjects.get(0).getName()));
        }
        else {
            dialogContent.setExpectedResponseModel(Model.of("I am absolutely sure"));
        }
        dialogContent.setConfirmAction($ -> actionConfirmDeleteProjects($, selectedProjects));

        dialog.open(dialogContent, aTarget);
    }

    private List<ProjectEntry> getSelectedProjects()
    {
        return dataProvider.getModel() //
                .map(l -> l.stream().filter(ProjectEntry::isSelected).toList()) //
                .orElse(emptyList()) //
                .getObject();
    }

    private void actionConfirmDeleteProjects(AjaxRequestTarget aTarget,
            List<Project> aSelectedProjects)
    {
        var count = 0;
        for (var project : aSelectedProjects) {
            try {
                projectService.removeProject(project);
                count++;
            }
            catch (IOException e) {
                WicketExceptionUtil.handleException(LOG, this, aTarget, e);
            }
        }

        if (aSelectedProjects.size() == 1) {
            success("Project [" + aSelectedProjects.get(0).getName() + "] has been deleted");
        }
        else {
            success(count + " projects have been deleted");
        }

        aTarget.addChildren(getPage(), IFeedback.class);
        allAccessibleProjects.detach();
        dataProvider.refresh();
        actionApplyFilter(aTarget);
    }

    private void actionToggleBulkChange(AjaxRequestTarget aTarget)
    {
        bulkChangeMode = !bulkChangeMode;
        allAccessibleProjects.detach();
        dataProvider.refresh();
        aTarget.add(this);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        if (shouldOpenCreateProjectDialogWhenPageLoads()) {
            aResponse.render(OnLoadHeaderItem
                    .forScript(openCreateProjectDialogByDefaultBehavior.getCallbackScript()));
        }
        else {
            WicketUtil.ajaxFallbackFocus(aResponse, queryField);
        }
    }

    private boolean shouldOpenCreateProjectDialogWhenPageLoads()
    {
        // If the project is not present or not empty we do not open the dialog
        if (allAccessibleProjects.map(list -> !list.isEmpty()).orElse(true).getObject()) {
            return false;
        }

        // If the user cannot create projects, we do not open the dialog
        if (!projectAccess.canCreateProjects()) {
            return false;
        }

        return true;
    }

    @OnEvent
    public void onProjectRoleFilterStateChanged(ProjectRoleFilterStateChanged aEvent)
    {
        actionApplyFilter(aEvent.getTarget());
    }

    @OnEvent
    public void onProjectStateFilterStateChanged(ProjectStateFilterStateChanged aEvent)
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
        var projectCreationGroup = new WebMarkupContainer("projectCreationGroup");
        authorize(projectCreationGroup, RENDER,
                join(",", ROLE_ADMIN.name(), ROLE_PROJECT_CREATOR.name()));
        projectCreationGroup.add(createNewProjectLink());
        return projectCreationGroup;
    }

    private WebMarkupContainer createProjectImportGroup()
    {
        var projectImportGroup = new WebMarkupContainer("projectImportGroup");
        authorize(projectImportGroup, RENDER,
                join(",", ROLE_ADMIN.name(), ROLE_PROJECT_CREATOR.name()));
        projectImportGroup.add(new ProjectImportPanel(MID_IMPORT_PROJECT_PANEL, Model.of()));
        return projectImportGroup;
    }

    private String getNoProjectsMessage()
    {
        if (!allAccessibleProjects.getObject().isEmpty() && dataProvider.size() == 0) {
            return getString("noProjects.noFilterMatch");
        }

        if (projectAccess.canCreateProjects()) {
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
        if (projectAccess.canCreateProjects()) {
            return;
        }

        var projects = allAccessibleProjects.getObject();
        if (projects.size() == 1) {
            var soleAccessibleProject = projects.get(0).getProject();
            var pageParameters = new PageParameters();
            ProjectPageBase.setProjectPageParameter(pageParameters, soleAccessibleProject);
            throw new RestartResponseException(ProjectDashboardPage.class, pageParameters);
        }
    }

    private LambdaAjaxLink createNewProjectLink()
    {
        newProjectLink = new LambdaAjaxLink(MID_NEW_PROJECT, this::actionCreateProject);

        add(newProjectLink);

        authorize(newProjectLink, RENDER,
                join(",", ROLE_ADMIN.name(), ROLE_PROJECT_CREATOR.name()));

        return newProjectLink;
    }

    private LambdaAjaxLink createStartTutorialLink()
    {
        var startTutorialLink = new LambdaAjaxLink(MID_START_TUTORIAL, this::startTutorial);
        startTutorialLink.setVisibilityAllowed(isTutorialAvailable());
        startTutorialLink.add(visibleWhen(() -> {
            return userRepository.isAdministrator(currentUser.getObject())
                    || userRepository.isProjectCreator(currentUser.getObject());
        }));

        add(startTutorialLink);

        return startTutorialLink;
    }

    private boolean isTutorialAvailable()
    {
        try {
            Class.forName("de.tudarmstadt.ukp.inception.tutorial.TutorialFooterItem");
            return true;
        }
        catch (Exception e) {
            return false;
        }
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
                var project = aItem.getModelObject().getProject();

                var pageParameters = new PageParameters();
                ProjectPageBase.setProjectPageParameter(pageParameters, project);
                var projectLink = new BookmarkablePageLink<Void>(MID_PROJECT_LINK,
                        ProjectDashboardPage.class, pageParameters);
                projectLink.add(new Label(MID_NAME, aItem.getModelObject().getName()));
                aItem.add(new TerseMarkdownLabel(MID_DESCRIPTION,
                        aItem.getModelObject().getShortDescription()));

                aItem.add(new CheckBox("selected") //
                        .setModel(LambdaModel.of(aItem.getModel(), ProjectEntry::isSelected,
                                ProjectEntry::setSelected))
                        .setOutputMarkupId(true) //
                        .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                                _target -> _target.add(selectCount))) //
                        .setVisible(bulkChangeMode));

                addActionsDropdown(aItem);
                aItem.add(projectLink);
                aItem.add(createRoleBadges(aItem.getModelObject()));

                var isAdmin = userRepository.isAdministrator(currentUser.getObject());
                var seeStats = isAdmin || projectService.hasRole(currentUser.getObject(), project,
                        MANAGER, CURATOR);
                if (seeStats) {
                    var stats = documentService.getSourceDocumentStats(project);
                    aItem.add(new ProjectDocumentStatsPanel("stats", Model.of(stats)));
                    aItem.add(new SymbolLabel("stateIcon", stats.getProjectState()));
                }
                else {
                    aItem.add(new EmptyPanel("stats"));
                    aItem.add(new EmptyPanel("stateIcon"));
                }

                var seeManagers = isAdmin
                        || projectService.hasRole(currentUser.getObject(), project, MANAGER);
                if (seeManagers) {
                    var sessionOwner = userRepository.getCurrentUsername();
                    var permissions = projectService.listProjectUserPermissions(project);
                    var comparator = Comparator
                            .comparing((ProjectUserPermissions p) -> p.getUser()
                                    .map(User::isEnabled).orElse(false))
                            .reversed() //
                            .thenComparing(p -> p.render(), String.CASE_INSENSITIVE_ORDER);
                    var managers = permissions.stream() //
                            .filter(p -> p.getRoles().contains(MANAGER)) //
                            .sorted(comparator) //
                            .collect(toCollection(ArrayList::new));

                    String primeManager;
                    var managedByYou = managers.removeIf(p -> p.getUsername().equals(sessionOwner));
                    if (managedByYou) {
                        primeManager = "You";
                    }
                    else if (!managers.isEmpty()) {
                        primeManager = managers.get(0).render();
                        managers.remove(0);
                    }
                    else {
                        primeManager = "-";
                    }

                    var otherManagersNames = managers.stream().map(u -> u.render()).toList();
                    var otherManagersCount = otherManagersNames.isEmpty() ? ""
                            : "+" + otherManagersNames.size();

                    aItem.add(new Label("primeManager", primeManager));

                    var otherManagers = new Label("otherManagers", otherManagersCount);
                    otherManagers.setVisible(!otherManagersNames.isEmpty());
                    otherManagers.add(
                            AttributeModifier.replace("title", join(", ", otherManagersNames)));
                    aItem.add(otherManagers);
                }
                else {
                    aItem.add(new EmptyPanel("primeManager"));
                    aItem.add(new EmptyPanel("otherManagers"));
                }

                var createdLabel = new Label(MID_CREATED,
                        () -> project.getCreated() != null ? formatDate(project.getCreated())
                                : null);
                createdLabel.add(visibleWhenModelIsNotNull(createdLabel));
                aItem.add(createdLabel);

                var updatedLabel = new Label(MID_UPDATED,
                        () -> project.getUpdated() != null ? formatDate(project.getUpdated())
                                : null);
                updatedLabel.add(visibleWhenModelIsNotNull(updatedLabel));
                aItem.add(updatedLabel);

                var projectId = new Label(MID_ID, () -> project.getId());
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
                            aClasses.add("border border-primary bg-light");
                        }
                        else {
                            aClasses.remove("border border-primary bg-light");
                        }
                        return aClasses;
                    }
                });
            }
        };
    }

    private void addActionsDropdown(ListItem<ProjectEntry> aItem)
    {
        var projectEntry = aItem.getModelObject();
        var project = projectEntry.getProject();

        var container = new WebMarkupContainer("actionDropdown");

        var leaveProjectLink = new LambdaAjaxLink(MID_LEAVE_PROJECT,
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
        var dialogContent = new LeaveProjectConfirmationDialogPanel(BootstrapModalDialog.CONTENT_ID,
                Model.of(aProject));

        dialogContent.setConfirmAction((_target) -> {
            projectService.revokeAllRoles(aProject, currentUser.getObject());
            allAccessibleProjects.detach();
            dataProvider.refresh();
            actionApplyFilter(_target);
            _target.addChildren(getPage(), IFeedback.class);
            success("You are no longer a member of project [" + aProject.getName() + "]");
        });

        dialog.open(dialogContent, aTarget);
    }

    private ListView<PermissionLevel> createRoleBadges(ProjectEntry aProjectEntry)
    {
        var levels = aProjectEntry.getLevels();
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
        var dialogContent = new ProjectTemplateSelectionDialogPanel(
                BootstrapModalDialog.CONTENT_ID);
        dialog.open(dialogContent, aTarget);
    }

    @OnEvent
    public void onProjectImported(AjaxProjectImportedEvent aEvent)
    {
        List<Project> projects = aEvent.getProjects();

        if (projects.size() > 1) {
            allAccessibleProjects.detach(); // Ensure that the subsequent refresh gets fresh data
            dataProvider.refresh();
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
        var isAdmin = userRepository.isAdministrator(currentUser.getObject());
        return projectService.listAccessibleProjectsWithPermissions(currentUser.getObject())
                .entrySet().stream() //
                .map(e -> new ProjectEntry(e.getKey(), e.getValue())) //
                .filter(e -> isAdmin || e.getLevels().contains(MANAGER)
                        || containsAny(e.getLevels(), dashboardProperties.getAccessibleByRoles()))
                .sorted(comparing(ProjectEntry::getName)) //
                .collect(toList());
    }
}
