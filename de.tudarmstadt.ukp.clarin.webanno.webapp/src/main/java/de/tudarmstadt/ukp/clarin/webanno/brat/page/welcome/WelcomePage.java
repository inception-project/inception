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
package de.tudarmstadt.ukp.clarin.webanno.brat.page.welcome;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.CurationPage;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.project.ProjectPage;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

public class WelcomePage
    extends ApplicationPageBase
{
    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    AjaxLink projectSettings;
    AjaxLink curation;
    AjaxLink annotation;

    public WelcomePage()
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = projectRepository.getUser(username);

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

            if (projectRepository.listProjectUsers(project).contains(username)
                    && ApplicationUtils.isProjectAdmin(project, projectRepository, user)) {
                add(projectSettings);
                projectSettingAdded = true;
                break;
            }
        }
        if (!projectSettingAdded) {

            add(projectSettings);
            projectSettings.setVisible(false);

        }

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

            if (projectRepository.listProjectUsers(project).contains(username)
                    && ApplicationUtils.isCurator(project, projectRepository, user)) {
                add(curation);
                curatorAdded = true;
                break;
            }

        }

        if (!curatorAdded) {
            add(curation);
            curation.setVisible(false);
        }

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

            if (projectRepository.listProjectUsers(project).contains(username)
                    && ApplicationUtils.isMember(project, projectRepository, user)) {
                add(annotation);
                memberAdded = true;
                break;
            }
        }
        if (!memberAdded) {
            add(annotation);
            annotation.setVisible(false);
            error("You are not member of any projects to annotate or curate");
        }
    }

    private static final long serialVersionUID = -530084892002620197L;
}
