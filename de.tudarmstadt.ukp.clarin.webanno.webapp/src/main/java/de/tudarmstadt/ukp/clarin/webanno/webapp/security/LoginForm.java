/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.CompoundPropertyModel;
/**
 * A login form.
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 *
 */
public class LoginForm
    extends Form<LoginForm>
{
    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(LoginForm.class);
    private String username;
    private String password;

    public LoginForm(String id)
    {
        super(id);
        setModel(new CompoundPropertyModel<LoginForm>(this));
        add(new RequiredTextField<String>("username"));
        add(new PasswordTextField("password"));
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
        if (!continueToOriginalDestination()) {
            setResponsePage(getApplication().getHomePage());
        }
    }
}
