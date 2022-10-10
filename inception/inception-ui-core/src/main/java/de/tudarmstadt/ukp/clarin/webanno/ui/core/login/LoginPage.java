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

import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.ADMIN_DEFAULT_PASSWORD;
import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.ADMIN_DEFAULT_USERNAME;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhenNot;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.EnumSet;
import java.util.LinkedHashMap;

import javax.servlet.http.HttpSession;

import org.apache.wicket.NonResettingRestartException;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.devutils.stateless.StatelessComponent;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.parameter.UrlRequestParametersAdapter;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.wicketstuff.annotation.mount.MountPath;

import com.giffing.wicket.spring.boot.context.scan.WicketSignInPage;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.api.SessionMetaData;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.LoginProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
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
    private static final String PARAM_SKIP_AUTP_LOGIN = "skipAutpLogin";
    private static final String PARAM_ERROR = "error";

    private static final String PROP_RESTORE_DEFAULT_ADMIN_ACCOUNT = "restoreDefaultAdminAccount";

    private static final long serialVersionUID = -333578034707672294L;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private @SpringBean UserDao userRepository;
    private @SpringBean LoginProperties loginProperties;
    private @SpringBean SecurityProperties securityProperties;
    private @SpringBean SessionRegistry sessionRegistry;

    private final LoginForm localLoginPanel;
    private final OAuth2LoginPanel oAuth2LoginPanel;

    private final WebMarkupContainer tooManyUsersLabel;
    private final IModel<Boolean> tooManyUsers;

    public LoginPage(PageParameters aParameters)
    {
        setStatelessHint(true);
        setVersioned(false);

        tooManyUsers = LoadableDetachableModel.of(this::isTooManyUsers).orElse(false);

        localLoginPanel = new LoginForm("loginForm");
        localLoginPanel.add(visibleWhen(this::isLoginAllowed));
        queue(localLoginPanel);

        redirectIfAlreadyLoggedIn(); // Must come after the localLoginPanel is initialized!

        oAuth2LoginPanel = new OAuth2LoginPanel("oauth2LoginPanel");
        oAuth2LoginPanel.add(visibleWhen(this::isLoginAllowed));
        queue(oAuth2LoginPanel);

        var skipAutoLogin = aParameters.get(PARAM_SKIP_AUTP_LOGIN).toBoolean(false)
                || tooManyUsers.getObject();
        if (!skipAutoLogin && isLoginAllowed()) {
            oAuth2LoginPanel.autoLogin();
        }

        // Failed OAuth2 call this page with the parameter `?error` so we display a message
        var error = aParameters.getNamedKeys().contains(PARAM_ERROR);
        if (error) {
            error("Login failed");
        }

        tooManyUsersLabel = new WebMarkupContainer("usersLabel");
        tooManyUsersLabel.add(visibleWhen(tooManyUsers));
        queue(tooManyUsersLabel);

        maybeInitializeAdminUser();
    }

    private boolean isLoginAllowed()
    {
        return !tooManyUsers.getObject() && !isAdminAccountRecoveryMode();
    }

    private void maybeInitializeAdminUser()
    {
        // Reset/recreated default admin account if requested
        if (isAdminAccountRecoveryMode()) {
            localLoginPanel.setVisible(false);
            User admin;
            boolean exists;
            if (userRepository.exists(ADMIN_DEFAULT_USERNAME)) {
                admin = userRepository.get(ADMIN_DEFAULT_USERNAME);
                exists = true;
            }
            else {
                admin = new User();
                admin.setUsername(ADMIN_DEFAULT_USERNAME);
                exists = false;
            }
            admin.setPassword(ADMIN_DEFAULT_PASSWORD);
            admin.setEnabled(true);
            admin.setRoles(EnumSet.of(ROLE_ADMIN, ROLE_USER));
            if (exists) {
                userRepository.update(admin);
                String msg = "Default admin account has been reset to the default permissions "
                        + "and credentials: " + ADMIN_DEFAULT_USERNAME + "/"
                        + ADMIN_DEFAULT_PASSWORD + ". Login has been disabled for security "
                        + "reasons. Please restart the application without the password "
                        + "resetting parameter.";
                warn(msg);
                log.info(msg);
            }
            else {
                userRepository.create(admin);
                String msg = "Default admin account has been recreated: " + ADMIN_DEFAULT_USERNAME
                        + "/" + ADMIN_DEFAULT_PASSWORD + ". Login has "
                        + "been disabled for security reasons. Please restart the application "
                        + "without the password resetting parameter.";
                warn(msg);
                log.info(msg);
            }
        }
        // Create admin user if there is no user yet
        else if (userRepository.list().isEmpty()) {
            User admin = new User();
            admin.setUsername(ADMIN_DEFAULT_USERNAME);
            admin.setPassword(ADMIN_DEFAULT_PASSWORD);
            admin.setEnabled(true);
            admin.setRoles(EnumSet.of(ROLE_ADMIN, ROLE_USER));

            // We log this as a warning so the message sticks on the screen. Success and info
            // messages are set to auto-close after a short time.
            String msg = "No user accounts have been found. An admin account has been created: "
                    + ADMIN_DEFAULT_USERNAME + "/" + ADMIN_DEFAULT_PASSWORD;
            warn(msg);

            userRepository.create(admin);
        }
    }

    private boolean isAdminAccountRecoveryMode()
    {
        return System.getProperty(PROP_RESTORE_DEFAULT_ADMIN_ACCOUNT) != null;
    }

    /**
     * Check if settings property is set and there will be more users logged in (with current one)
     * than max users allowed.
     */
    private boolean isTooManyUsers()
    {
        long maxUsers = loginProperties.getMaxConcurrentSessions();
        return maxUsers > 0 && sessionRegistry.getAllPrincipals().size() >= maxUsers;
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        redirectIfAlreadyLoggedIn();
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(CssHeaderItem
                .forReference(new WebjarsCssResourceReference("hover/current/css/hover.css")));

        aResponse.render(CssHeaderItem.forReference(LoginPageCssResourceReference.get()));

        // Capture the URL fragment into a hidden form field so we can use it later when
        // forwarding to the target page after login
        aResponse.render(OnDomReadyHeaderItem.forScript(
                "$('#urlfragment').attr('value', unescape(self.document.location.hash.substring(1)));"));
    }

    private void redirectIfAlreadyLoggedIn()
    {
        // If we are already logged in, redirect to the welcome page. This tries to a void a
        // situation where the user tries to access the login page directly and thus the
        // application would redirect the user to the login page after a successful login
        if (!(SecurityContextHolder.getContext()
                .getAuthentication() instanceof AnonymousAuthenticationToken)) {
            log.debug("Already logged in, forwarding to home page");
            throw new RestartResponseException(getApplication().getHomePage());
        }

        String redirectUrl = getRedirectUrl();
        if (redirectUrl == null) {
            log.debug("Authentication required");
        }
        else {
            log.debug("Authentication required (original URL: [{}])", redirectUrl);
        }
    }

    private class LoginForm
        extends StatelessForm<LoginForm>
    {
        private static final long serialVersionUID = 1L;

        private String username;
        private String password;
        private String urlfragment;

        public LoginForm(String id)
        {
            super(id);
            setModel(new CompoundPropertyModel<>(this));

            add(new RequiredTextField<String>("username").setOutputMarkupId(true));
            add(new PasswordTextField("password").setOutputMarkupId(true));
            add(new HiddenField<>("urlfragment"));
            add(new Button("signInBtn").add(enabledWhenNot(tooManyUsers)));
            add(new Label("loginMessage", loginProperties.getMessage()) //
                    .setEscapeModelStrings(false) //
                    .add(visibleWhen(() -> isNotBlank(loginProperties.getMessage()))));
        }

        @Override
        protected void onSubmit()
        {
            // The redirect URL is stored in the session, so we have to pick it up before the
            // session is reset as part of the login.
            String redirectUrl = getRedirectUrl();

            // We only accept users that are not bound to a particular realm (e.g. to a project)
            User user = userRepository.get(username);
            if (user == null || user.getRealm() != null) {
                error("Login failed");
                return;
            }

            AuthenticatedWebSession session = AuthenticatedWebSession.get();
            if (!session.signIn(username, password)) {
                error("Login failed");
                return;
            }

            log.debug("Login successful");
            setDefaultResponsePageIfNecessary(redirectUrl);
        }

        private void setDefaultResponsePageIfNecessary(String aRedirectUrl)
        {
            // This does not work because it was Spring Security that intercepted the access, not
            // Wicket continueToOriginalDestination();

            if (aRedirectUrl == null || aRedirectUrl.contains(".IBehaviorListener.")
                    || aRedirectUrl.contains("-logoutPanel-")) {
                log.debug("Redirecting to welcome page");
                setResponsePage(getApplication().getHomePage());
            }
            else {
                log.debug("Redirecting to saved URL: [{}]", aRedirectUrl);
                if (isNotBlank(localLoginPanel.urlfragment)
                        && localLoginPanel.urlfragment.startsWith("!")) {
                    Url url = Url.parse("http://dummy?" + localLoginPanel.urlfragment.substring(1));
                    UrlRequestParametersAdapter adapter = new UrlRequestParametersAdapter(url);
                    LinkedHashMap<String, StringValue> params = new LinkedHashMap<>();
                    for (String name : adapter.getParameterNames()) {
                        params.put(name, adapter.getParameterValue(name));
                    }
                    Session.get().setMetaData(SessionMetaData.LOGIN_URL_FRAGMENT_PARAMS, params);
                }
                throw new NonResettingRestartException(aRedirectUrl);
            }
        }
    }

    private String getRedirectUrl()
    {
        String redirectUrl = null;

        HttpSession session = ((ServletWebRequest) RequestCycle.get().getRequest())
                .getContainerRequest().getSession(false);
        if (session != null) {
            SavedRequest savedRequest = (SavedRequest) session
                    .getAttribute("SPRING_SECURITY_SAVED_REQUEST");
            if (savedRequest != null) {
                redirectUrl = savedRequest.getRedirectUrl();
            }
        }

        // There is some kind of bug that logs the user out again if the redirect page is
        // the context root and if that does not end in a slash. To avoid this, we add a slash
        // here. This is rather a hack, but I have no idea why this problem occurs. Figured this
        // out through trial-and-error rather then by in-depth debugging.
        String baseUrl = RequestCycle.get().getUrlRenderer().renderFullUrl(Url.parse(""));
        if (baseUrl.equals(redirectUrl)) {
            redirectUrl += "/";
        }

        // In case there was a URL fragment in the original URL, append it again to the redirect
        // URL.
        if (redirectUrl != null && isNotBlank(localLoginPanel.urlfragment)) {
            redirectUrl += "#" + localLoginPanel.urlfragment;
        }

        return redirectUrl;
    }
}
