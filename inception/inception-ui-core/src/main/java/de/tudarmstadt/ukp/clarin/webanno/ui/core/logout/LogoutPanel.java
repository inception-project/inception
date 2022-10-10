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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;

import javax.servlet.http.HttpSession;

import org.apache.wicket.devutils.stateless.StatelessComponent;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.LoginProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.config.PreauthenticationProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaStatelessLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.ApplicationSession;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.users.ManageUsersPage;

/**
 * A wicket panel for logout.
 */
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
        logoutLink.add(visibleWhen(this::isLogoutEnabled));
        add(logoutLink);

        var logoutTimer = new WebMarkupContainer("logoutTimer");
        logoutTimer.add(visibleWhen(() -> getAutoLogoutTime() > 0 && isLogoutEnabled()));
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

    private boolean isLogoutEnabled()
    {
        // Logout is disabled for external (OAuth2) users if autoLogin is enabled.
        // Pre-authenticated users do not count as external users here as we may have a logout
        // link from the IdP configured
        if (startsWith(getModel().getObject().getRealm(), UserDao.REALM_EXTERNAL_PREFIX)) {
            return isBlank(securityProperties.getAutoLogin());
        }

        return true;
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
        ApplicationSession.get().signOut();

        if (preauthenticationProperties.getLogoutUrl().isPresent()) {
            throw new RedirectToUrlException(preauthenticationProperties.getLogoutUrl().get());
        }
        else {
            setResponsePage(getApplication().getHomePage());
        }
    }

    /**
     * Checks if auto-logout is enabled. For Winstone, we get a max session length of 0, so here it
     * is disabled.
     */
    private int getAutoLogoutTime()
    {
        int duration = 0;
        Request request = RequestCycle.get().getRequest();
        if (request instanceof ServletWebRequest) {
            HttpSession session = ((ServletWebRequest) request).getContainerRequest().getSession();
            if (session != null) {
                duration = session.getMaxInactiveInterval();
            }
        }
        return duration;
    }
}
