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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_PROJECT_CREATOR;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.wicket.Component;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
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
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapAjaxTabbedPanel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ModelChangedVisitor;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelFactory;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.detail.ProjectDetailPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.guidelines.ProjectGuidelinesPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.tagsets.ProjectTagSetsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.users.ProjectUsersPanel;

/**
 * This is the main page for Project Settings. The Page has Four Panels. The
 * {@link ProjectGuidelinesPanel} is used to update documents to a project. The
 * {@code ProjectDetailsPanel} used for updating Project details such as descriptions of a project
 * and name of the Project The {@link ProjectTagSetsPanel} is used to add {@link TagSet} and
 * {@link Tag} details to a Project as well as updating them The {@link ProjectUsersPanel} is used
 * to update {@link User} to a Project
 */
@MountPath("/projectsetting.html")
public class ProjectPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    // private static final Logger LOG = LoggerFactory.getLogger(ProjectPage.class);

    private @SpringBean ProjectSettingsPanelRegistry projectSettingsPanelRegistry;
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;

    private WebMarkupContainer sidebar;
    private WebMarkupContainer tabContainer;
    private AjaxTabbedPanel<ITab> tabPanel;
    private ProjectSelectionPanel projects;
    private ProjectImportPanel importProjectPanel;

    private IModel<Project> selectedProject;
    
    public ProjectPage()
    {
        super();
        
        commonInit();
    }
    
    public ProjectPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
        
        commonInit();
       
        sidebar.setVisible(false);
        
        User user = userRepository.getCurrentUser();
        
        // Get current project from parameters
        StringValue projectParameter = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        Optional<Project> project = getProjectFromParameters(projectParameter);
        
        if (project.isPresent()) {
            // Check access to project
            if (project != null && !projectService.isAdmin(project.get(), user)) {
                error("You have no permission to access project [" + project.get().getId() + "]");
                setResponsePage(getApplication().getHomePage());
            }
            
            selectedProject.setObject(project.get());
        }
        else {
            error("Project [" + projectParameter + "] does not exist");
            setResponsePage(getApplication().getHomePage());
        }
    }
    
    private void commonInit()
    {
        selectedProject = Model.of();

        sidebar = new WebMarkupContainer("sidebar");
        sidebar.setOutputMarkupId(true);
        add(sidebar);

        tabContainer = new WebMarkupContainer("tabContainer");
        tabContainer.setOutputMarkupId(true);
        add(tabContainer);
        
        tabContainer.add(new Label("projectName", PropertyModel.of(selectedProject, "name")));
        
        tabPanel = new BootstrapAjaxTabbedPanel<ITab>("tabPanel", makeTabs()) {
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

        importProjectPanel = new ProjectImportPanel("importPanel", selectedProject);
        sidebar.add(importProjectPanel);
        MetaDataRoleAuthorizationStrategy.authorize(importProjectPanel, Component.RENDER,
                String.join(",", ROLE_ADMIN.name(), ROLE_PROJECT_CREATOR.name()));    
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
            AbstractTab tab = new AbstractTab(Model.of(psp.getLabel())) {
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
                            && getRegistry().getPanel(path)
                                    .applies(selectedProject.getObject());
                }
            };
            tabs.add(tab);
        }
        return tabs;
    }
    
    
    private Optional<Project> getProjectFromParameters(StringValue projectParam)
    {
        if (projectParam == null || projectParam.isEmpty()) {
            return Optional.empty();
        }
        
        try {
            return Optional.of(projectService.getProject(projectParam.toLong()));
        }
        catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
