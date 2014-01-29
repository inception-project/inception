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

import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * A wicket panel for logout.
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 *
 */
public class LogoutPanel
    extends Panel
{
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
        add(new Link("logout")
        {
            @Override
            public void onClick()
            {
                AuthenticatedWebSession.get().signOut();
                setResponsePage(getApplication().getHomePage());
            }
        });
    }
}
