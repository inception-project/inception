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
package de.tudarmstadt.ukp.inception.sharing;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.sharing.AcceptInvitePage.PAGE_PARAM_INVITE_ID;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;

@MountPath(value = NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/join-project/${"
        + PAGE_PARAM_INVITE_ID + "}")
public class AcceptInvitePage
    extends ProjectPageBase
{
    private static final long serialVersionUID = 5160703195387357692L;

    public static final String PAGE_PARAM_INVITE_ID = "i";

    private @SpringBean InviteService inviteService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private Project project;
    private String token;

    public AcceptInvitePage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        project = getProject();

        // Compare invite param to invite id in db, if correct add user to project
        token = aPageParameters.get(PAGE_PARAM_INVITE_ID).toOptionalString();

        IModel<Boolean> invitationIsValid = LoadableDetachableModel.of(this::checkInvitation);

        WebMarkupContainer expirationNotice = new WebMarkupContainer("expirationNotice");
        expirationNotice.add(visibleWhen(() -> !invitationIsValid.orElse(false).getObject()));
        add(expirationNotice);

        WebMarkupContainer acceptInvitationPanel = new WebMarkupContainer("acceptInvitationPanel");
        acceptInvitationPanel.add(visibleWhen(() -> invitationIsValid.orElse(false).getObject()));
        acceptInvitationPanel.add(new LambdaAjaxLink("join", this::actionJoinProject));
        acceptInvitationPanel.add(new Label("project", PropertyModel.of(project, "name")));
        add(acceptInvitationPanel);
    }

    private boolean checkInvitation()
    {
        return project != null && inviteService.isValidInviteLink(project, token);
    }

    private void actionJoinProject(AjaxRequestTarget aTarget)
    {
        if (!inviteService.isValidInviteLink(project, token)) {
            error("Invitation has expired.");
            return;
        }

        User currentUser = userRepository.getCurrentUser();

        if (!projectService.existsProjectPermissionLevel(currentUser, project, ANNOTATOR)) {
            projectService.createProjectPermission(
                    new ProjectPermission(project, currentUser.getUsername(), ANNOTATOR));
            getSession().success("You have successfully joined the project.");
        }
        else {
            getSession().info("You were already an annotator on this project.");
        }

        PageParameters parameters = new PageParameters();
        parameters.set(ProjectDashboardPage.PAGE_PARAM_PROJECT, project.getId());
        getRequestCycle().setResponsePage(ProjectDashboardPage.class, parameters);
    }
}
