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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.dashlet;

import static java.util.stream.Collectors.joining;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.session.SessionRegistry;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.Dashlet_ImplBase;

public class SystemStatusDashlet
    extends Dashlet_ImplBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = 1276835215161570732L;

    private @SpringBean SessionRegistry sessionRegistry;
    private @SpringBean UserDao userRepository;
    private @SpringBean SystemStatusService systemStatusService;

    public SystemStatusDashlet(String aId)
    {
        super(aId);
        setOutputMarkupId(true);

        queue(new Label("activeUsers", LoadableDetachableModel.of(() -> getActiveUsers().size())));
        queue(new Label("activeUsersDetail", LoadableDetachableModel.of(
                () -> getActiveUsers().stream().map(Objects::toString).collect(joining(", ")))));
    }

    private List<Object> getActiveUsers()
    {
        return sessionRegistry.getAllPrincipals().stream() //
                .filter($ -> !"anonymousUser".equals($.toString())) //
                .toList();
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(userRepository.isAdministrator(userRepository.getCurrentUser()));
    }
}
