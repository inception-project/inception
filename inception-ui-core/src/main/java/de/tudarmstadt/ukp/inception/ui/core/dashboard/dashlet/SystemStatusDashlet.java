/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.session.SessionRegistry;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;

public class SystemStatusDashlet
    extends Dashlet_ImplBase
{
    private static final long serialVersionUID = 1276835215161570732L;

    private @SpringBean SessionRegistry sessionRegistry;
    private @SpringBean UserDao userRepository;

    public SystemStatusDashlet(String aId)
    {
        super(aId);

        add(new Label("activeUsers",
                LoadableDetachableModel.of(() -> sessionRegistry.getAllPrincipals().size())));
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        setVisible(userRepository.isAdministrator(userRepository.getCurrentUser()));
    }
}
