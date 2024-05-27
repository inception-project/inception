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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_PROJECT_CREATOR;
import static org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy.authorize;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.inception.support.wicket.OverviewListChoice;

class ProjectSelectionPanel
    extends ListPanel_ImplBase
{
    private static final long serialVersionUID = -1L;

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private OverviewListChoice<Project> overviewList;
    private LambdaAjaxLink createLink;
    private ProjectImportPanel importProjectPanel;

    public ProjectSelectionPanel(String id, IModel<Project> aModel)
    {
        super(id);

        overviewList = new OverviewListChoice<>("project");
        overviewList.setChoiceRenderer(new ChoiceRenderer<>("name"));
        overviewList.setModel(aModel);
        overviewList.setChoices(LoadableDetachableModel.of(this::listProjects));
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
        add(overviewList);

        add(createLink = new LambdaAjaxLink("create", this::actionCreate));
        MetaDataRoleAuthorizationStrategy.authorize(createLink, Component.RENDER, StringUtils.join(
                new String[] { Role.ROLE_ADMIN.name(), Role.ROLE_PROJECT_CREATOR.name() }, ","));

        importProjectPanel = new ProjectImportPanel("importPanel", aModel);
        add(importProjectPanel);
        authorize(importProjectPanel, Component.RENDER,
                String.join(",", ROLE_ADMIN.name(), ROLE_PROJECT_CREATOR.name()));
    }

    private List<Project> listProjects()
    {
        return projectService.listManageableProjects(userRepository.getCurrentUser());
    }
}
