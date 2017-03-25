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

import java.util.Properties;

import javax.servlet.http.HttpSession;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.devutils.stateless.StatelessComponent;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.SavedRequest;

import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
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

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    public LoginPage()
    {
        // If we are already logged in, redirect to the welcome page. This tries to a void a
        // situation where the user tries to access the login page directly and thus the
        // application would redirect the user to the login page after a successful login
        if (!(SecurityContextHolder.getContext()
                .getAuthentication() instanceof AnonymousAuthenticationToken)) {
            throw new RestartResponseException(getApplication().getHomePage());
        }

        setStatelessHint(true);
        setVersioned(false);

        if (userRepository.list().isEmpty()) {
            User admin = new User();
            admin.setUsername(ADMIN_DEFAULT_USERNAME);
            admin.setPassword(ADMIN_DEFAULT_PASSWORD);
            admin.setEnabled(true);
            admin.setRoles(Role.getRoles());
            userRepository.create(admin);

            String msg = "No user accounts have been found. An admin account has been created: "
                    + ADMIN_DEFAULT_USERNAME + "/" + ADMIN_DEFAULT_PASSWORD;
            info(msg);
            LoggerFactory.getLogger(getClass()).info(msg);
        }
        add(new LoginForm("loginForm"));
    }

    private class LoginForm
        extends StatelessForm<LoginForm>
    {
        private static final long serialVersionUID = 1L;
        private String username;
        private String password;

        public LoginForm(String id)
        {
            super(id);
            setModel(new CompoundPropertyModel<LoginForm>(this));
            add(new RequiredTextField<String>("username"));
            add(new PasswordTextField("password"));
            Properties settings = SettingsUtil.getSettings();
            String loginMessage = settings.getProperty(SettingsUtil.CFG_LOGIN_MESSAGE);
            add(new MultiLineLabel("loginMessage", loginMessage).setEscapeModelStrings(false));
        }

        @Override
        protected void onSubmit()
        {
            AuthenticatedWebSession session = AuthenticatedWebSession.get();
            if (session.signIn(username, password)) {
                setDefaultResponsePageIfNecessary();
            }
            else {
                error("Login failed");
            }
        }

        private void setDefaultResponsePageIfNecessary()
        {
            // This does not work because it was Spring Security that intercepted the access, not
            // Wicket
            // continueToOriginalDestination();

            HttpSession session = ((ServletWebRequest) RequestCycle.get().getRequest())
                    .getContainerRequest().getSession(false);
            SavedRequest savedRequest = (SavedRequest) session
                    .getAttribute("SPRING_SECURITY_SAVED_REQUEST");
            if (savedRequest != null) {
                log.debug("Redirecting to saved URL: [{}]", savedRequest.getRedirectUrl());
                throw new RedirectToUrlException(savedRequest.getRedirectUrl());
            }
            else {
                log.debug("Redirecting to welcome page");
                setResponsePage(getApplication().getHomePage());
            }
        }
    }
}
