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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.CurationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.monitoring.MonitoringPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.project.ProjectPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.security.page.ManageUsersPage;

/**
 * A home page for WebAnno: <br>
 * Based on the user's permission, it dispplays either {@link ProjectPage}, d {@link AnnotationPage}
 * , {@link CurationPage} or {@link monitoringPage }(since v.2.0)
 *
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 *
 */
public class WelcomePage
    extends ApplicationPageBase
{
    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    AjaxLink projectSettings;
    AjaxLink curation;
    AjaxLink annotation;
    AjaxLink monitoring;
    AjaxLink usremanagement;

    public WelcomePage()
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = projectRepository.getUser(username);

        // Add Project Setting Link
        // Only Super Admin or Project admins can see this link
        boolean projectSettingAdded = false;
        projectSettings = new AjaxLink<Void>("projectSettings")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(ProjectPage.class);
            }
        };
        for (Project project : projectRepository.listProjects()) {

            if (ApplicationUtils.isProjectAdmin(project, projectRepository, user)) {
                add(projectSettings);
                projectSettingAdded = true;
                break;
            }
        }
        if (ApplicationUtils.isSuperAdmin(projectRepository, user) && !projectSettingAdded) {
            add(projectSettings);
        }
        else if (!projectSettingAdded) {

            add(projectSettings);
            projectSettings.setVisible(false);

        }

        // Add curation Link
        // Only Admins or curators can see this link
        boolean curatorAdded = false;
        curation = new AjaxLink<Void>("curation")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(CurationPage.class);
            }
        };
        for (Project project : projectRepository.listProjects()) {

            if (ApplicationUtils.isCurator(project, projectRepository, user)) {
                add(curation);
                curatorAdded = true;
                break;
            }

        }
        if (ApplicationUtils.isSuperAdmin(projectRepository, user) && !projectSettingAdded) {
            add(curation);
        }
        else if (!curatorAdded) {
            add(curation);
            curation.setVisible(false);
        }

        // Add annotation link
        // Only Admins or annotators can see this link
        annotation = new AjaxLink<Void>("annotation")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(AnnotationPage.class);
            }
        };

        boolean memberAdded = false;
        for (Project project : projectRepository.listProjects()) {

            if (ApplicationUtils.isMember(project, projectRepository, user)) {
                add(annotation);
                memberAdded = true;
                break;
            }
        }
        if (ApplicationUtils.isSuperAdmin(projectRepository, user) && !projectSettingAdded) {
            add(annotation);
        }
        else if (!memberAdded) {
            add(annotation);
            annotation.setVisible(false);
            error("You are not member of any projects to annotate or curate");
        }

        // Add monitoring link
        // Only Admins can see this link

        boolean monitoringAdded = false;
        monitoring = new AjaxLink<Void>("monitoring")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(MonitoringPage.class);
            }
        };

        for (Project project : projectRepository.listProjects()) {

            if (ApplicationUtils.isProjectAdmin(project, projectRepository, user)
                    || ApplicationUtils.isCurator(project, projectRepository, user)) {
                add(monitoring);
                monitoringAdded = true;
                break;
            }
        }
        if (ApplicationUtils.isSuperAdmin(projectRepository, user) && !monitoringAdded) {
            add(monitoring);
        }
        else if (!monitoringAdded) {

            add(monitoring);
            monitoring.setVisible(false);

        }


        usremanagement = new AjaxLink<Void>("usremanagement")
                {
                    private static final long serialVersionUID = 7496156015186497496L;

                    @Override
                    public void onClick(AjaxRequestTarget target)
                    {
                        setResponsePage(ManageUsersPage.class);
                    }
                };
        MetaDataRoleAuthorizationStrategy.authorize(usremanagement, Component.RENDER, "ROLE_ADMIN");
        add(usremanagement);
    }

    private static final long serialVersionUID = -530084892002620197L;
}
