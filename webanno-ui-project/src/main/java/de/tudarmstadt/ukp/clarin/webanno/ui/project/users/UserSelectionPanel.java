/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.users;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.form.multiselect.MultiSelect;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;

class UserSelectionPanel
    extends ListPanel_ImplBase
{
    private static final long serialVersionUID = -9151455840010092452L;

    private @SpringBean ProjectService projectRepository;
    private @SpringBean UserDao userRepository;
    private @SpringBean UserSelectionPanelConfiguration userSelectionPanelConfiguration;

    private IModel<Project> projectModel;
    private IModel<User> userModel;
    
    private OverviewListChoice<User> overviewList;
    private MultiSelect<User> usersToAdd;

    public UserSelectionPanel(String id, IModel<Project> aProject, IModel<User> aUser)
    {
        super(id);
        
        setOutputMarkupId(true);
        
        final boolean hideUsernames = userSelectionPanelConfiguration.isHideUsers();
        final int userNameMinLengthForSuggestions =
                userSelectionPanelConfiguration.getUsersMinLengthCharacters();
        
        projectModel = aProject;
        userModel = aUser;

        overviewList = new OverviewListChoice<>("user");
        overviewList.setChoiceRenderer(makeUserChoiceRenderer());
        overviewList.setModel(userModel);
        overviewList.setChoices(this::listUsersWithPermissions);
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
        add(overviewList);
        
        IModel<Collection<User>> usersToAddModel = new CollectionModel<>(new ArrayList<>());
        Form<Collection<User>> form = new Form<>("form", usersToAddModel);
        add(form);
        usersToAdd = new MultiSelect<User>("usersToAdd") {
            private static final long serialVersionUID = 8231304829756188352L;

            @Override
            public void onConfigure(JQueryBehavior aBehavior)
            {
                super.onConfigure(aBehavior);
                aBehavior.setOption("placeholder", Options.asString(getString("placeholder")));
                if (hideUsernames) {
                    aBehavior.setOption("filter", Options.asString("equals"));
                    aBehavior.setOption("autoClose", true); 
                    aBehavior.setOption("minLength", userNameMinLengthForSuggestions >= 0 ?
                            userNameMinLengthForSuggestions : 2);
                    aBehavior.setOption("enforceMinLength", true);
                }  else {
                    aBehavior.setOption("filter", Options.asString("contains"));
                    aBehavior.setOption("autoClose", false);
                }
            }
        };
        usersToAdd.setModel(usersToAddModel);
        usersToAdd.setChoices(this::listUsersWithoutPermissions);
        usersToAdd.setChoiceRenderer(new ChoiceRenderer<>("username"));
        form.add(usersToAdd);
        form.add(new LambdaAjaxButton<>("add", this::actionAdd));
    }
    
    private IChoiceRenderer<User> makeUserChoiceRenderer()
    {
        return new ChoiceRenderer<User>()
        {
            private static final long serialVersionUID = 4607720784161484145L;

            @Override
            public Object getDisplayValue(User aUser)
            {
                String permissionLevels = projectRepository
                        .getProjectPermissionLevels(aUser, projectModel.getObject()).stream()
                        .map(PermissionLevel::getName).collect(joining(", ", "[", "]"));
                
                return aUser.getUsername() + " " + permissionLevels
                        + (aUser.isEnabled() ? "" : " (login disabled)");
            }
        };
    }

    private List<User> listUsersWithPermissions()
    {
        return projectRepository.listProjectUsersWithPermissions(projectModel.getObject());
    }

    private List<User> listUsersWithoutPermissions()
    {
        List<User> result = new ArrayList<>(userRepository.list());
        result.removeAll(listUsersWithPermissions());
        return result;
    }

    private void actionAdd(AjaxRequestTarget aTarget, Form<List<User>> aForm)
    {
        for (User user : aForm.getModelObject()) {
            projectRepository.createProjectPermission(new ProjectPermission(
                    projectModel.getObject(), user.getUsername(), PermissionLevel.ANNOTATOR));
        }
        
        aForm.getModelObject().clear();
        usersToAdd.getChoicesModel().detach();
        
        aTarget.add(this);
    }
}
