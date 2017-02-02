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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Properties;

import javax.persistence.NoResultException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.model.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.monitoring.page.MonitoringPage;
import de.tudarmstadt.ukp.clarin.webanno.project.page.ProjectPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.core.app.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.automation.AutomationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.correction.CorrectionPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.CurationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.login.LoginPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.security.page.ManageUsersPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.security.preauth.WebAnnoApplicationContextInitializer;

/**
 * A home page for WebAnno: <br>
 * Based on the user's permission, it displays either {@link ProjectPage}, d {@link AnnotationPage}
 * , {@link CurationPage} or {@link MonitoringPage }(since v.2.0)
 *
 *
 */
public class WelcomePage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2487663821276301436L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private AjaxLink<Void> projectSettings;
    private AjaxLink<Void> curation;
    private AjaxLink<Void> annotation;
    private AjaxLink<Void> monitoring;
    private AjaxLink<Void> userManagement;
    private AjaxLink<Void> correction;
    private AjaxLink<Void> automation;

    public WelcomePage()
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // if a user is logged recently, session will not expire,
        // This causes a problem, if the data base is re-created while user's session not expired OR
        // the user is deleted while the session is not expired
        User user = null;
        try {
            user = userRepository.get(username);
        }
        // redirect to login page (if no usr is found, admin/admin will be created)
        catch (NoResultException e) {
            setResponsePage(LoginPage.class);
        }

        // Add Project Setting Link
        // Only Super Admin or Project admins can see this link
        projectSettings = new AjaxLink<Void>("projectSettings")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(ProjectPage.class);
            }
        };
        add(projectSettings);
        projectSettings.setVisible(projectSettingsEnabeled(user));

        // Add curation Link
        // Only project admins or curators can see this link
        curation = new AjaxLink<Void>("curation")
        {
            private static final long serialVersionUID = 3681686831639096179L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(CurationPage.class);
            }
        };
        add(curation);
        curation.setVisible(curationEnabeled(user));

        // Add annotation link
        // Only project admins and annotators can see this link
        annotation = new AjaxLink<Void>("annotation")
        {
            private static final long serialVersionUID = -845758775690774624L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(AnnotationPage.class);
            }
        };
        add(annotation);
        annotation.setVisible(annotationEnabeled(user, Mode.ANNOTATION));

        // Add correction Link
        // Only project admins and annotators can see this link
        correction = new AjaxLink<Void>("correction")
        {
            private static final long serialVersionUID = -3113946217791583714L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(CorrectionPage.class);
            }
        };
        add(correction);
        correction.setVisible(annotationEnabeled(user, Mode.CORRECTION));

        // Add automation Link
        // Only project admins and annotators can see this link
        automation = new AjaxLink<Void>("automation")
        {
            private static final long serialVersionUID = -6527983833667707141L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(AutomationPage.class);
            }
        };
        add(automation);
        automation.setVisible(annotationEnabeled(user, Mode.AUTOMATION));
        
        // if not either a curator or annotator, display warning message
        if (!annotation.isVisible() && !correction.isVisible() && !automation.isVisible()
                && !curation.isVisible()) {
            info("You are not member of any projects to annotate or curate");
        }
        
        // Add monitoring link
        // Only project admins and curators can see this link
        monitoring = new AjaxLink<Void>("monitoring")
        {
            private static final long serialVersionUID = 545914367958126874L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(MonitoringPage.class);
            }
        };
        add(monitoring);
        monitoring.setVisible(monitoringEnabeled(user));

        userManagement = new AjaxLink<Void>("userManagement")
        {
            private static final long serialVersionUID = -4722275335074746935L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                setResponsePage(ManageUsersPage.class);
            }
        };
        // MetaDataRoleAuthorizationStrategy.authorize(userManagement, Component.RENDER, "ROLE_ADMIN");
        // Global admins may always access the user management - normal users only if WebAnno
        // is not running in PREAUTH mode
        add(userManagement);
        List<String> activeProfiles = asList(ApplicationContextProvider.getApplicationContext()
                .getEnvironment().getActiveProfiles());
        Properties settings = SettingsUtil.getSettings();
        userManagement.setVisible(SecurityUtil.isSuperAdmin(repository, user)
                || (!activeProfiles.contains(WebAnnoApplicationContextInitializer.PROFILE_PREAUTH)
                        && "true".equals(
                                settings.getProperty(SettingsUtil.CFG_USER_ALLOW_PROFILE_ACCESS))));
    }

    private boolean projectSettingsEnabeled(User user)
    {
        if (SecurityUtil.isSuperAdmin(repository, user)) {
            return true;
        }

        if (SecurityUtil.isProjectCreator(repository, user)) {
            return true;
        }

        for (Project project : repository.listProjects()) {
            if (SecurityUtil.isProjectAdmin(project, repository, user)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean curationEnabeled(User user)
    {
        for (Project project : repository.listProjects()) {
            if (SecurityUtil.isCurator(project, repository, user)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean annotationEnabeled(User user, Mode mode)
    {
        for (Project project : repository.listProjects()) {
            if (SecurityUtil.isMember(project, repository, user)
                    && mode.equals(project.getMode())) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean monitoringEnabeled(User user)
    {
        for (Project project : repository.listProjects()) {
            if (SecurityUtil.isCurator(project, repository, user)
                    || SecurityUtil.isProjectAdmin(project, repository, user)) {
                return true;
            }
        }
        
        return false;
    }}
