/*
 * Copyright 2020
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

package de.tudarmstadt.ukp.inception.workload.dynamic.page.settings;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;

import java.io.Serializable;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapAjaxTabbedPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.ProjectPage;


@MountPath("/projectsettings.html")
public class ProjectSettingsPage extends ProjectPage implements Serializable
{

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    //Current Project
    private Project currentProject;

    private IModel<Project> selectedProject;

    public ProjectSettingsPage()
    {
        init();
    }

    public ProjectSettingsPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        //Get current Project
        StringValue projectParameter = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        if (getProjectFromParameters(projectParameter).isPresent())
        {
            currentProject = getProjectFromParameters(projectParameter).get();
        } else {
            currentProject = null;
        }
        selectedProject = Model.of();
        Optional<Project> project = getProjectFromParameters(projectParameter);
        if (project.isPresent()) {
            // Check access to project
            if (!projectService.isAdmin(project.get(), userRepository.getCurrentUser())) {
                error("You have no permission to access project [" + project.get().getId() + "]");
                setResponsePage(getApplication().getHomePage());
            }

            selectedProject.setObject(project.get());
        }
        init();
    }

    public void init()
    {
        //Get the tabbed Panel
        BootstrapAjaxTabbedPanel<ITab> tabs = (BootstrapAjaxTabbedPanel<ITab>)get("tabContainer").get("tabPanel");

        //Add a new tab
        tabs.getTabs().add(new AbstractTab(Model.of(getString("workload")))
        {
            @Override
            public WebMarkupContainer getPanel(String aID)
            {
                return new ProjectWorkloadPanel(aID, selectedProject);
            }
        });
    }




    //Return current project, required for several purposes
    private Optional<Project> getProjectFromParameters(StringValue aProjectParam)
    {
        if (aProjectParam == null || aProjectParam.isEmpty())
        {
            return Optional.empty();
        }

        try
        {
            return Optional.of(projectService.getProject(aProjectParam.toLong()));
        }
        catch (NoResultException e)
        {
            return Optional.empty();
        }
    }

}
