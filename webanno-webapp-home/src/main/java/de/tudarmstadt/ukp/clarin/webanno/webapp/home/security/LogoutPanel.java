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
package de.tudarmstadt.ukp.clarin.webanno.webapp.home.security;

import javax.servlet.http.HttpSession;

import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * A wicket panel for logout.
 */
public class LogoutPanel
    extends Panel
{
    private static final long serialVersionUID = 3725185820083021070L;

    public LogoutPanel(String id)
    {
        super(id);
        initialize();
    }

    public LogoutPanel(String id, IModel<?> model)
    {
        super(id, model);
        initialize();
    }

    @SuppressWarnings("serial")
    private void initialize()
    {
        add(new Label("username").setDefaultModel(new Model<String>(SecurityContextHolder
                .getContext().getAuthentication().getName())));

        add(new Link<Void>("logout")
        {
            @Override
            public void onClick()
            {
                AuthenticatedWebSession.get().signOut();
                setResponsePage(getApplication().getHomePage());
            }
        });
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        setVisible(AuthenticatedWebSession.get().isSignedIn());
    }
    
    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(new PriorityHeaderItem(JavaScriptHeaderItem.forReference(
                getApplication().getJavaScriptLibrarySettings().getJQueryReference())));
        Request request = RequestCycle.get().getRequest();
        if (request instanceof ServletWebRequest) {
            HttpSession session = ((ServletWebRequest) request).getContainerRequest().getSession();
            if (session != null) {
                int timeout = session.getMaxInactiveInterval();
                aResponse.render(JavaScriptHeaderItem.forScript(
                        "$(document).ready(function() { new LogoutTimer("+timeout+"); });", 
                        "webAnnoAutoLogout"));
            }
        }        
    }
}
