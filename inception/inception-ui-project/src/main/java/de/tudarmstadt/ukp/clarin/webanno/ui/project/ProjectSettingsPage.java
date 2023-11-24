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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.util.lang.WicketObjects;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectContext;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelFactory;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.detail.ProjectDetailPanel;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapAjaxTabbedPanel;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.inception.support.wicket.ModelChangedVisitor;

/**
 * This is the main page for Project Settings.
 */
@MountPath(value = NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/settings", alt = "/admin/projects")
public class ProjectSettingsPage
    extends ApplicationPageBase
    implements ProjectContext
{
    private static final Logger LOG = LoggerFactory.getLogger(ProjectSettingsPage.class);

    public static final String NEW_PROJECT_ID = "__NEW__";

    private static final long serialVersionUID = -2102136855109258306L;

    // private static final Logger LOG = LoggerFactory.getLogger(ProjectPage.class);

    private @SpringBean ProjectSettingsPanelRegistry projectSettingsPanelRegistry;
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;

    private WebMarkupContainer sidebar;
    private WebMarkupContainer tabContainer;
    private AjaxTabbedPanel<ITab> tabPanel;
    private ProjectSelectionPanel projects;

    private IModel<Project> selectedProject;

    private boolean preSelectedModelMode = false;

    public ProjectSettingsPage()
    {
        super();

        commonInit();
    }

    public ProjectSettingsPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        commonInit();

        preSelectedModelMode = true;

        sidebar.setVisible(false);

        // Fetch project parameter
        StringValue projectParameter = aPageParameters.get(PAGE_PARAM_PROJECT);
        // Check if we are asked to create a new project
        if (projectParameter != null && NEW_PROJECT_ID.equals(projectParameter.toString())) {
            selectedProject.setObject(new Project());
        }
        // Check if we are asked to open an existing project
        else {
            Project project = ProjectPageBase.getProjectFromParameters(this, projectService);

            if (project != null) {
                User user = userRepository.getCurrentUser();

                // Check access to project
                if (!userRepository.isAdministrator(user)
                        && !projectService.hasRole(user, project, MANAGER)) {
                    error("You have no permission to access project [" + project.getId() + "]");
                    setResponsePage(getApplication().getHomePage());
                }

                selectedProject.setObject(project);
            }
            else {
                error("Project [" + projectParameter + "] does not exist");
                setResponsePage(getApplication().getHomePage());
            }
        }
    }

    private void commonInit()
    {
        selectedProject = Model.of();

        sidebar = new WebMarkupContainer("sidebar");
        sidebar.setOutputMarkupId(true);
        add(sidebar);

        tabContainer = new WebMarkupContainer("tabContainer");
        tabContainer.setOutputMarkupPlaceholderTag(true);
        tabContainer.add(visibleWhen(() -> selectedProject.getObject() != null));
        add(tabContainer);

        tabContainer.add(new Label("projectName", PropertyModel.of(selectedProject, "name")));

        tabContainer.add(new LambdaAjaxLink("cancel", this::actionCancel));

        tabPanel = new BootstrapAjaxTabbedPanel<ITab>("tabPanel", makeTabs())
        {
            private static final long serialVersionUID = -7356420977522213071L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();

                setVisible(selectedProject.getObject() != null);
            }
        };
        tabPanel.setOutputMarkupPlaceholderTag(true);
        tabContainer.add(tabPanel);

        projects = new ProjectSelectionPanel("projects", selectedProject);
        projects.setCreateAction(target -> {
            selectedProject.setObject(new Project());
            // Make sure that default values are loaded
            tabPanel.visitChildren(new ModelChangedVisitor(selectedProject));
        });
        projects.setChangeAction(target -> {
            target.add(tabContainer);
            // Make sure that any invalid forms are cleared now that we load the new project.
            // If we do not do this, then e.g. input fields may just continue showing the values
            // they had when they were marked invalid.
            tabPanel.visitChildren(new ModelChangedVisitor(selectedProject));
        });
        sidebar.add(projects);
    }

    @Override
    public Project getProject()
    {
        return selectedProject.getObject();
    }

    private List<ITab> makeTabs()
    {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(Model.of("Details"))
        {
            private static final long serialVersionUID = 6703144434578403272L;

            @Override
            public Panel getPanel(String panelId)
            {
                return new ProjectDetailPanel(panelId, selectedProject);
            }

            @Override
            public boolean isVisible()
            {
                return selectedProject.getObject() != null;
            }
        });

        // Add the project settings panels from the registry
        for (ProjectSettingsPanelFactory psp : projectSettingsPanelRegistry.getPanels()) {
            String path = psp.getPath();
            AbstractTab tab = new AbstractTab(Model.of(psp.getLabel()))
            {
                private static final long serialVersionUID = -1503555976570640065L;

                private ProjectSettingsPanelRegistry getRegistry()
                {
                    // @SpringBean doesn't work here and we cannot keep a reference on the
                    // projectSettingsPanelRegistry either because it is not serializable,
                    // so we have no other chance here than fetching it statically
                    return ApplicationContextProvider.getApplicationContext()
                            .getBean(ProjectSettingsPanelRegistry.class);
                }

                @Override
                public Panel getPanel(String aPanelId)
                {
                    return getRegistry().getPanel(path).createSettingsPanel(aPanelId,
                            selectedProject);
                }

                @Override
                public boolean isVisible()
                {
                    return selectedProject.getObject() != null
                            && selectedProject.getObject().getId() != null
                            && getRegistry().getPanel(path).applies(selectedProject.getObject());
                }
            };
            tabs.add(tab);
        }
        return tabs;
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        // Project not in pre-select mode, so we are on the admin page
        if (!preSelectedModelMode) {
            selectedProject.setObject(null);

            // Reload whole page because master panel also needs to be reloaded.
            aTarget.add(getPage());
            return;
        }

        // Project still in creation and has not been saved yet
        if (getProject().getId() == null) {
            setResponsePage(getApplication().getHomePage());
            return;
        }

        Class<? extends Page> projectDashboard = WicketObjects.resolveClass(
                "de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage");

        PageParameters pageParameters = new PageParameters();
        ProjectPageBase.setProjectPageParameter(pageParameters, getProject());
        setResponsePage(projectDashboard, pageParameters);
    }
}
