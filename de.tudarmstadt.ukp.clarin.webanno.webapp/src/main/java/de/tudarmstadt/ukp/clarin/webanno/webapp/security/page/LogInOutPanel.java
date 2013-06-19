/*******************************************************************************
 * Copyright 2013
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
package de.tudarmstadt.ukp.clarin.webanno.webapp.security.page;

import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.webapp.page.login.LoginPage;


/**
 * Panel used to log in or log out.
 *
 * @author Richard Eckart de Castilho
 */
public class LogInOutPanel
	extends Panel
{
	private static final long serialVersionUID = 4194429035097444753L;

	public LogInOutPanel(String id)
	{
		super(id);
		initialize();
	}

	public LogInOutPanel(String id, IModel<?> model)
	{
		super(id, model);
		initialize();
	}

	@SuppressWarnings("serial")
	private void initialize()
	{
		// TODO well, is there another way to not anonymously semi-login?
		if (isSignedInAnonymously()) {
			AuthenticatedWebSession.get().signOut();
		}

		add(new Label("usernameLink")
		{
			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				if (AuthenticatedWebSession.get().isSignedIn()) {
					setDefaultModel(new Model<String>(SecurityContextHolder.getContext()
							.getAuthentication().getName()));
				}
				setVisible(AuthenticatedWebSession.get().isSignedIn());
			}
		});

		add(new Link<Void>("login")
		{
			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(!AuthenticatedWebSession.get().isSignedIn());
			}

			@Override
			public void onClick()
			{
				setResponsePage(LoginPage.class);
			}
		});

		add(new Link<Void>("logout")
		{
			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(AuthenticatedWebSession.get().isSignedIn());
			}

			@Override
			public void onClick()
			{
				AuthenticatedWebSession.get().signOut();
				setResponsePage(getApplication().getHomePage());
			}
		});
	}

	private boolean isSignedInAnonymously()
	{
		AuthenticatedWebSession session = AuthenticatedWebSession.get();
		return session.isSignedIn() && session.getRoles().hasRole("ROLE_ANONYMOUS");
	}
}
