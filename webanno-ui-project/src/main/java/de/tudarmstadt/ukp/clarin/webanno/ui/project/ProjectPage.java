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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemCondition;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelRegistryService;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelRegistryService.ProjectSettingsPanelDecl;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.detail.ProjectDetailPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.guidelines.AnnotationGuideLinePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.tagsets.ProjectTagSetsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.users.ProjectUsersPanel;

/**
 * This is the main page for Project Settings. The Page has Four Panels. The
 * {@link AnnotationGuideLinePanel} is used to update documents to a project. The
 * {@code ProjectDetailsPanel} used for updating Project details such as descriptions of a project
 * and name of the Project The {@link ProjectTagSetsPanel} is used to add {@link TagSet} and
 * {@link Tag} details to a Project as well as updating them The {@link ProjectUsersPanel} is used
 * to update {@link User} to a Project
 */
@MenuItem(icon = "images/setting_tools.png", label = "Projects", prio = 400)
@MountPath("/projectsetting.html")
public class ProjectPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    // private static final Logger LOG = LoggerFactory.getLogger(ProjectPage.class);

    private @SpringBean ProjectSettingsPanelRegistryService projectSettingsPanelRegistryService;

    private AjaxTabbedPanel<ITab> tabPanel;
    private ProjectSelectionPanel projects;
    private ProjectImportPanel importProjectPanel;

    private IModel<Project> selectedProject;
    
    public ProjectPage()
    {
        selectedProject = Model.of();
        
        tabPanel = new AjaxTabbedPanel<ITab>("tabPanel", makeTabs()) {
            private static final long serialVersionUID = -7356420977522213071L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(selectedProject.getObject() != null);
            }
        };
        tabPanel.setOutputMarkupPlaceholderTag(true);
        tabPanel.setOutputMarkupId(true);
        add(tabPanel);
        
        projects = new ProjectSelectionPanel("projects", selectedProject);
        projects.setCreateAction(target -> selectedProject.setObject(new Project()));
        projects.setChangeAction(target -> { 
            target.add(tabPanel);
            // Make sure that any invalid forms are cleared now that we load the new project.
            // If we do not do this, then e.g. input fields may just continue showing the values
            // they had when they were marked invalid.
            tabPanel.visitChildren(new ModelChangedVisitor(selectedProject));
        });
        add(projects);

        importProjectPanel = new ProjectImportPanel("importPanel", selectedProject);
        add(importProjectPanel);
        MetaDataRoleAuthorizationStrategy.authorize(importProjectPanel, Component.RENDER,
                "ROLE_ADMIN");
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
        for (ProjectSettingsPanelDecl psp : projectSettingsPanelRegistryService.getPanels()) {
            AbstractTab tab = new AbstractTab(Model.of(psp.label)) {
                private static final long serialVersionUID = -1503555976570640065L;

                @Override
                public Panel getPanel(String aPanelId)
                {
                    try {
                        return ConstructorUtils.invokeConstructor(psp.panel, aPanelId,
                                selectedProject);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public boolean isVisible()
                {
                    return selectedProject.getObject() != null
                            && selectedProject.getObject().getId() != 0
                            && psp.condition.applies(selectedProject.getObject());
                }
            };
            tabs.add(tab);
        }
        return tabs;
    }

    /*
     * Only admins and project managers can see this page
     */
    @MenuItemCondition
    public static boolean menuItemCondition(ProjectService aRepo, UserDao aUserRepo)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aUserRepo.get(username);
        return SecurityUtil.projectSettingsEnabeled(aRepo, user);
    }
    
    /**
     * In contrast to {@link Component#sameInnermostModel}, this visitor can not only handle
     * {@link IWrapModel} but also {@link IChainingModel}, e.g. {@link CompoundPropertyModel}
     * which is used in forms.
     */
    private static class ModelChangedVisitor
        implements IVisitor<Component, Void>
    {
        private IModel<?> model;

        public ModelChangedVisitor(IModel<?> aModel)
        {
            model = aModel;
        }

        @Override
        public void component(Component aComponent, IVisit<Void> aVisit)
        {
            if (sameInnermostModel(aComponent, model)) {
                aComponent.modelChanged();
            }
        }

        private boolean sameInnermostModel(Component aComponent, IModel<?> aModel)
        {
            // Get the two models
            IModel<?> thisModel = aComponent.getDefaultModel();

            // If both models are non-null they could be the same
            if (thisModel != null && aModel != null) {
                return innermostModel(thisModel) == innermostModel(aModel);
            }

            return false;
        }

        private IModel<?> innermostModel(IModel<?> aModel)
        {
            IModel<?> nested = aModel;
            while (nested != null) {
                if (nested instanceof IWrapModel) {
                    final IModel<?> next = ((IWrapModel<?>) nested).getWrappedModel();
                    if (nested == next) {
                        throw new WicketRuntimeException(
                                "Model for " + nested + " is self-referential");
                    }
                    nested = next;
                }
                else if (nested instanceof IChainingModel) {
                    final IModel<?> next = ((IChainingModel<?>) nested).getChainedModel();
                    if (nested == next) {
                        throw new WicketRuntimeException(
                                "Model for " + nested + " is self-referential");
                    }
                    nested = next;
                }
                else {
                    break;
                }
            }
            return nested;
        }
    }
}
