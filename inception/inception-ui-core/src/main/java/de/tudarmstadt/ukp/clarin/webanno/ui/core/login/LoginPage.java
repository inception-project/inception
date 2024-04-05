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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.login;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.lang.invoke.MethodHandles;

import org.apache.wicket.devutils.stateless.StatelessComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.session.SessionRegistry;
import org.wicketstuff.annotation.mount.MountPath;

import com.giffing.wicket.spring.boot.context.scan.WicketSignInPage;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.LoginProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityProperties;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

/**
 * The login page.
 */
@WicketSignInPage
@MountPath("/login.html")
@StatelessComponent
public class LoginPage
    extends ApplicationPageBase
{
    public static final String PARAM_SKIP_AUTO_LOGIN = "skipAutoLogin";
    private static final String PARAM_ERROR = "error";

    private static final long serialVersionUID = -333578034707672294L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean UserDao userService;
    private @SpringBean LoginProperties loginProperties;
    private @SpringBean SecurityProperties securityProperties;
    private @SpringBean SessionRegistry sessionRegistry;

    private final LocalLoginPanel localLoginPanel;
    private final InitialAccountCreationPanel initialAccountCreationPanel;
    private final OAuth2LoginPanel oAuth2LoginPanel;
    private final Saml2LoginPanel saml2LoginPanel;

    private final WebMarkupContainer tooManyUsersMessage;
    private final WebMarkupContainer recoveryModeMessage;
    private final IModel<Boolean> tooManyUsers;

    public LoginPage(PageParameters aParameters)
    {
        setStatelessHint(true);
        setVersioned(false);

        tooManyUsers = LoadableDetachableModel.of(this::isTooManyUsers).orElse(false);

        initialAccountCreationPanel = new InitialAccountCreationPanel(
                "initialAccountCreationPanel");
        initialAccountCreationPanel.add(visibleWhen(
                () -> userService.isAdminAccountRecoveryMode() || userService.isEmpty()));
        queue(initialAccountCreationPanel);

        localLoginPanel = new LocalLoginPanel("localLoginPanel", tooManyUsers);
        localLoginPanel.add(visibleWhen(this::isLoginAllowed));
        queue(localLoginPanel);

        oAuth2LoginPanel = new OAuth2LoginPanel("oauth2LoginPanel");
        oAuth2LoginPanel.add(visibleWhen(this::isLoginAllowed));
        queue(oAuth2LoginPanel);

        saml2LoginPanel = new Saml2LoginPanel("saml2LoginPanel");
        saml2LoginPanel.add(visibleWhen(this::isLoginAllowed));
        queue(saml2LoginPanel);

        var skipAutoLogin = aParameters.get(PARAM_SKIP_AUTO_LOGIN).toBoolean(false)
                || tooManyUsers.getObject();

        // Failed OAuth2/SAML call this page with the parameter `?error` so we display a message
        var error = aParameters.getNamedKeys().contains(PARAM_ERROR);
        if (error) {
            error("Login with SSO service failed. You might try logging out of your SSO service "
                    + "before trying to log in here again.");
            skipAutoLogin = true;
        }

        if (!skipAutoLogin && isLoginAllowed()) {
            oAuth2LoginPanel.autoLogin();
            saml2LoginPanel.autoLogin();
        }

        tooManyUsersMessage = new WebMarkupContainer("tooManyUsersMessage");
        tooManyUsersMessage.add(visibleWhen(tooManyUsers));
        queue(tooManyUsersMessage);

        recoveryModeMessage = new WebMarkupContainer("recoveryModeMessage");
        recoveryModeMessage.add(visibleWhen(() -> userService.isAdminAccountRecoveryMode()));
        queue(recoveryModeMessage);
    }

    private boolean isLoginAllowed()
    {
        return !tooManyUsers.getObject() && !userService.isAdminAccountRecoveryMode()
                && !userService.isEmpty();
    }

    /**
     * Check if settings property is set and there will be more users logged in (with current one)
     * than max users allowed.
     */
    private boolean isTooManyUsers()
    {
        var maxUsers = loginProperties.getMaxConcurrentSessions();
        return maxUsers > 0 && sessionRegistry.getAllPrincipals().size() >= maxUsers;
    }
}
