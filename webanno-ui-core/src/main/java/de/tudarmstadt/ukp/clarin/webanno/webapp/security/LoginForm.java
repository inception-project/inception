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
package de.tudarmstadt.ukp.clarin.webanno.webapp.security;

import java.util.Properties;

import javax.servlet.http.HttpSession;

import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.savedrequest.SavedRequest;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

/**
 * A login form.
 */
public class LoginForm
    extends StatelessForm<LoginForm>
{
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(LoginForm.class);
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
            LOG.error("Login failed");
        }
    }

    private void setDefaultResponsePageIfNecessary()
    {
        // This does not work because it was Spring Security that intercepted the access, not Wicket
        // continueToOriginalDestination();
        
        HttpSession session = ((ServletWebRequest) RequestCycle.get().getRequest())
                .getContainerRequest().getSession(false);
        SavedRequest savedRequest = (SavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
        if (savedRequest != null) {
            LOG.debug("Redirecting to saved URL: [{}]", savedRequest.getRedirectUrl());
            throw new RedirectToUrlException(savedRequest.getRedirectUrl());
        }
        else {
            LOG.debug("Redirecting to welcome page");
            setResponsePage(getApplication().getHomePage());
        }
    }
}
