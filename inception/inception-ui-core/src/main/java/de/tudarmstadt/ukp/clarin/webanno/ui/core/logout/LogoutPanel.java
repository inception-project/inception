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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.logout;

import static de.tudarmstadt.ukp.clarin.webanno.security.WicketSecurityUtils.getAutoLogoutTime;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.apache.wicket.Component;
import org.apache.wicket.devutils.stateless.StatelessComponent;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.LoginProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.config.PreauthenticationProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.ApplicationSession;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.login.LoginPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.users.ManageUsersPage;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaStatelessLink;

@StatelessComponent
public class LogoutPanel
    extends Panel
{
    private static final long serialVersionUID = 3725185820083021070L;

    private @SpringBean PreauthenticationProperties preauthenticationProperties;
    private @SpringBean LoginProperties securityProperties;
    private @SpringBean UserDao userRepository;

    public LogoutPanel(String id, IModel<User> aUser)
    {
        super(id, aUser);

        var logoutLink = new LambdaStatelessLink("logout", this::actionLogout);
        add(logoutLink);

        var logoutTimer = new WebMarkupContainer("logoutTimer");
        logoutTimer.add(visibleWhen(() -> getAutoLogoutTime() > 0));
        add(logoutTimer);

        var profileLinkParameters = new PageParameters().add(ManageUsersPage.PARAM_USER,
                getModel().map(User::getUsername).orElse("").getObject());
        var profileLink = new BookmarkablePageLink<>("profile", ManageUsersPage.class,
                profileLinkParameters);
        profileLink.add(enabledWhen(
                () -> userRepository.isProfileSelfServiceAllowed(getModel().getObject())));
        profileLink.add(visibleWhen(getModel().isPresent()));
        profileLink.add(new Label("username", getModel().map(User::getUiName)));
        add(profileLink);

        add(visibleWhen(
                () -> ApplicationSession.exists() && ApplicationSession.get().isSignedIn()));
    }

    @SuppressWarnings("unchecked")
    public IModel<User> getModel()
    {
        return (IModel<User>) getDefaultModel();
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(new PriorityHeaderItem(JavaScriptHeaderItem.forReference(
                getApplication().getJavaScriptLibrarySettings().getJQueryReference())));

        aResponse.render(
                JavaScriptHeaderItem.forReference(LogoutTimerJavascriptResourceReference.get()));

        int timeout = getAutoLogoutTime();
        if (timeout > 0) {
            aResponse.render(JavaScriptHeaderItem.forScript(
                    "$(document).ready(function() { new LogoutTimer(" + timeout + "); });",
                    "webAnnoAutoLogout"));
        }
    }

    private void actionLogout()
    {
        actionLogout(this, preauthenticationProperties, securityProperties);
    }

    public static void actionLogout(Component aOwner,
            PreauthenticationProperties aPreauthProperties, LoginProperties aSecProperties)
    {
        // It would be nicer if we could just use the default Spring Security logout
        // mechanism by making the logout button a link to `/logout`, but since
        // we want to perform extra stuff afterwards, we currently use this way.
        //
        // The alternative would be to register a custom LogoutSuccessHandler with
        // Spring Security which would do our special logout redirection behavior

        ApplicationSession.get().signOut();

        if (aPreauthProperties.getLogoutUrl().isPresent()) {
            throw new RedirectToUrlException(aPreauthProperties.getLogoutUrl().get());
        }

        if (isNotBlank(aSecProperties.getAutoLogin())) {
            var parameters = new PageParameters();
            parameters.set(LoginPage.PARAM_SKIP_AUTO_LOGIN, true);
            aOwner.setResponsePage(LoginPage.class, parameters);
            return;
        }

        aOwner.setResponsePage(aOwner.getApplication().getHomePage());
    }
}
