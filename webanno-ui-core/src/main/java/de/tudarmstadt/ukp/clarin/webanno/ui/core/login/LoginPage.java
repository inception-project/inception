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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.login;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.servlet.http.HttpSession;

import org.apache.wicket.NonResettingRestartException;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.devutils.stateless.StatelessComponent;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.parameter.UrlRequestParametersAdapter;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.savedrequest.SavedRequest;

import de.tudarmstadt.ukp.clarin.webanno.api.SessionMetaData;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

/**
 * The login page.
 */
@StatelessComponent
public class LoginPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -333578034707672294L;

    private static final String ADMIN_DEFAULT_USERNAME = "admin";
    private static final String ADMIN_DEFAULT_PASSWORD = "admin";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private @SpringBean UserDao userRepository;
    private @SpringBean(required = false) SessionRegistry sessionRegistry;

    private LoginForm form;
    
    public LoginPage()
    {
        setStatelessHint(true);
        setVersioned(false);
        
        add(form = new LoginForm("loginForm"));
        
        redirectIfAlreadyLoggedIn();

        // Create admin user if there is no user yet
        if (userRepository.list().isEmpty()) {
            User admin = new User();
            admin.setUsername(ADMIN_DEFAULT_USERNAME);
            admin.setPassword(ADMIN_DEFAULT_PASSWORD);
            admin.setEnabled(true);
            admin.setRoles(EnumSet.of(Role.ROLE_ADMIN, Role.ROLE_USER));
            userRepository.create(admin);

            String msg = "No user accounts have been found. An admin account has been created: "
                    + ADMIN_DEFAULT_USERNAME + "/" + ADMIN_DEFAULT_PASSWORD;
            // We log this as a warning so the message sticks on the screen. Success and info
            // messages are set to auto-close after a short time.
            warn(msg);
        }
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        redirectIfAlreadyLoggedIn();
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
            add(new RequiredTextField<String>("username"));
            add(new PasswordTextField("password"));
            add(new HiddenField<>("urlfragment"));
            Properties settings = SettingsUtil.getSettings();
            String loginMessage = settings.getProperty(SettingsUtil.CFG_LOGIN_MESSAGE);
            add(new MultiLineLabel("loginMessage", loginMessage).setEscapeModelStrings(false));
        }

        @Override
        protected void onSubmit()
        {
            AuthenticatedWebSession session = AuthenticatedWebSession.get();
            if (session.signIn(username, password)) {
                log.debug("Login successful");
                if (sessionRegistry != null) {
                    // Form-based login isn't detected by SessionManagementFilter. Thus handling
                    // session registration manually here.
                    HttpSession containerSession = ((ServletWebRequest) RequestCycle.get()
                            .getRequest()).getContainerRequest().getSession(false);
                    sessionRegistry.registerNewSession(containerSession.getId(), username);
                }
                setDefaultResponsePageIfNecessary();
            }
            else {
                error("Login failed");
            }
        }

        private void setDefaultResponsePageIfNecessary()
        {
            // This does not work because it was Spring Security that intercepted the access, not
            // Wicket continueToOriginalDestination();

            String redirectUrl = getRedirectUrl();
            
            if (redirectUrl == null || redirectUrl.contains(".IBehaviorListener.")
                    || redirectUrl.contains("-logoutPanel-")) {
                log.debug("Redirecting to welcome page");
                setResponsePage(getApplication().getHomePage());
            }
            else {
                log.debug("Redirecting to saved URL: [{}]", redirectUrl);
                if (isNotBlank(form.urlfragment) && form.urlfragment.startsWith("!")) {
                    Url url = Url.parse("http://dummy?" + form.urlfragment.substring(1));
                    UrlRequestParametersAdapter adapter = new UrlRequestParametersAdapter(url);
                    LinkedHashMap<String, StringValue> params = new LinkedHashMap<>();
                    for (String name : adapter.getParameterNames()) {
                        params.put(name, adapter.getParameterValue(name));
                    }
                    Session.get().setMetaData(SessionMetaData.LOGIN_URL_FRAGMENT_PARAMS, params);
                }
                throw new NonResettingRestartException(redirectUrl);
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
        if (redirectUrl != null && isNotBlank(form.urlfragment)) {
            redirectUrl += "#" + form.urlfragment;
        }

        return redirectUrl;
    }
}
