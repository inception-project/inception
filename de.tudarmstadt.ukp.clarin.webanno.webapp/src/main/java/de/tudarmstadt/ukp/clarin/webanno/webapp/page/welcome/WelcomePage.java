/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome;

import javax.persistence.NoResultException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.ProjectUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.automation.AutomationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.correction.CorrectionPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.crowdsource.CrowdSourcePage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.CurationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.login.LoginPage;
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
    private RepositoryService repository;

    AjaxLink<Void> projectSettings;
    AjaxLink<Void> curation;
    AjaxLink<Void> annotation;
    AjaxLink<Void> monitoring;
    AjaxLink<Void> usremanagement;
    AjaxLink<Void> crowdSource;
    AjaxLink<Void> correction;
    AjaxLink<Void> automation;

    public WelcomePage()
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // if a user is logged recently, session will not expire,
        // This causes a problem, if the data base is re-created while user's session not expired OR
        // the user is deleted while the session is not expired
        User user = null;
        try{
         user = repository.getUser(username);
        }
        // redirect to login page (if no usr is found, admin/admin will be created)
        catch (NoResultException e){
            setResponsePage(LoginPage.class);
        }

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
        for (Project project : repository.listProjects()) {

            if (ProjectUtil.isProjectAdmin(project, repository, user)) {
                add(projectSettings);
                projectSettingAdded = true;
                break;
            }
        }
        if (ProjectUtil.isSuperAdmin(repository, user) && !projectSettingAdded) {
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
        for (Project project : repository.listProjects()) {

            if (ProjectUtil.isCurator(project, repository, user)) {
                add(curation);
                curatorAdded = true;
                break;
            }

        }
        if (ProjectUtil.isSuperAdmin(repository, user) && !projectSettingAdded) {
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
        for (Project project : repository.listProjects()) {

            if (ProjectUtil.isMember(project, repository, user)) {
                add(annotation);
                memberAdded = true;
                break;
            }
        }
        if (ProjectUtil.isSuperAdmin(repository, user) && !projectSettingAdded) {
            add(annotation);
        }
        else if (!memberAdded) {
            add(annotation);
            annotation.setVisible(false);
        }

        // if not either a curator or annotator, display warning message
        if(!memberAdded && !curatorAdded){
            info("You are not member of any projects to annotate or curate");
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

        for (Project project : repository.listProjects()) {

            if (ProjectUtil.isProjectAdmin(project, repository, user)
                    || ProjectUtil.isCurator(project, repository, user)) {
                add(monitoring);
                monitoringAdded = true;
                break;
            }
        }
        if (ProjectUtil.isSuperAdmin(repository, user) && !monitoringAdded) {
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


        // Add crowdsource link
        // Only Admins can see this link

        boolean crowdSourceAdded = false;
        crowdSource = new AjaxLink<Void>("crowdSource")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(CrowdSourcePage.class);
            }
        };

        for (Project project : repository.listProjects()) {

            if (ProjectUtil.isProjectAdmin(project, repository, user)
                    || ProjectUtil.isCurator(project, repository, user)) {
                add(crowdSource);
                crowdSourceAdded = true;
                break;
            }
        }
        if (ProjectUtil.isSuperAdmin(repository, user) && !crowdSourceAdded) {
            add(crowdSource);
        }
        else if (!crowdSourceAdded) {
            add(crowdSource);
            crowdSource.setVisible(false);
        }
        if(repository.isCrowdSourceEnabled()==0){
            crowdSource.setVisible(false);
        }

        // Add correction Link
        // Only Admins or curators can see this link
        boolean correctionAdded = false;
        correction = new AjaxLink<Void>("correction")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(CorrectionPage.class);
            }
        };
        for (Project project : repository.listProjects()) {

            if (ProjectUtil.isCurator(project, repository, user)) {
                add(correction);
                correctionAdded = true;
                break;
            }

        }
        if (ProjectUtil.isSuperAdmin(repository, user) && !projectSettingAdded) {
            add(correction);
        }
        else if (!correctionAdded) {
            add(correction);
            correction.setVisible(false);
        }


    // Add automation Link
    // Only Admins or users can see this link
    boolean automationAdded = false;
    automation = new AjaxLink<Void>("automation")
    {

        private static final long serialVersionUID = 1L;

        @Override
        public void onClick(AjaxRequestTarget target)
        {
            setResponsePage(AutomationPage.class);
        }
    };
    for (Project project : repository.listProjects()) {

        if (ProjectUtil.isMember(project, repository, user)) {
            add(automation);
            automationAdded = true;
            break;
        }

    }
    if (ProjectUtil.isSuperAdmin(repository, user) && !projectSettingAdded) {
        add(automation);
    }
    else if (!automationAdded) {
        add(automation);
        automation.setVisible(false);
    }

}

    private static final long serialVersionUID = -530084892002620197L;
}
