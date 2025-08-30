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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.users;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.KendoDataSource;
import org.wicketstuff.kendo.ui.form.multiselect.lazy.MultiSelect;
import org.wicketstuff.kendo.ui.renderer.ChoiceRenderer;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.inception.support.wicket.OverviewListChoice;

class UserSelectionPanel
    extends ListPanel_ImplBase
{
    private static final long serialVersionUID = -9151455840010092452L;

    private @SpringBean ProjectService projectRepository;
    private @SpringBean UserDao userRepository;
    private @SpringBean UserSelectionPanelConfiguration config;

    private IModel<Project> projectModel;
    private IModel<ProjectUserPermissions> userModel;

    private OverviewListChoice<ProjectUserPermissions> overviewList;
    private MultiSelect<User> usersToAdd;
    private Map<String, User> recentUsers;

    public UserSelectionPanel(String id, IModel<Project> aProject,
            IModel<ProjectUserPermissions> aUser)
    {
        super(id);

        setOutputMarkupId(true);

        projectModel = aProject;
        userModel = aUser;
        recentUsers = new HashMap<>();

        overviewList = new OverviewListChoice<>("user");
        overviewList
                .setChoiceRenderer(new ProjectUserPermissionChoiceRenderer().setShowRoles(true));
        overviewList.setModel(userModel);
        overviewList.setChoices(this::listUsersWithPermissions);
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
        add(overviewList);

        IModel<Collection<User>> usersToAddModel = new CollectionModel<>(new ArrayList<>());
        var form = new Form<>("form", usersToAddModel);
        add(form);

        var userRenderer = new ChoiceRenderer<User>("username")
        {
            private static final long serialVersionUID = -2386864570904752307L;

            @Override
            public String getText(User user, String expression)
            {
                var builder = new StringBuilder();
                builder.append(user.getUiName());
                if (!user.getUsername().equals(user.getUiName())) {
                    builder.append(" (");
                    builder.append(user.getUsername());
                    builder.append(")");
                }

                String text = builder.toString();
                recentUsers.put(text, user);
                return text;
            };
        };

        usersToAdd = new MultiSelect<User>("usersToAdd", userRenderer)
        {
            private static final long serialVersionUID = 8231304829756188352L;

            @Override
            protected void onConfigure(KendoDataSource aDataSource)
            {
                // This ensures that we get the user input in getChoices
                aDataSource.set("serverFiltering", true);
            }

            @Override
            public void onConfigure(JQueryBehavior aBehavior)
            {
                super.onConfigure(aBehavior);
                aBehavior.setOption("placeholder", Options.asString(getString("placeholder")));
                aBehavior.setOption("filter", Options.asString("contains"));
                aBehavior.setOption("autoClose", false);
            }

            @Override
            public List<User> getChoices(String aInput)
            {
                var result = new ArrayList<User>();

                if (config.isHideUsers()) {
                    // only offer the user matching what the input entered into the field
                    if (isNotBlank(aInput)) {
                        var user = userRepository.get(aInput);
                        if (user != null) {
                            result.add(user);
                        }
                    }
                }
                else {
                    // offer all enabled users matching the input
                    userRepository.listEnabledUsers().stream()
                            .filter(user -> aInput == null || user.getUsername().contains(aInput)
                                    || user.getUiName().contains(aInput))
                            .forEach(result::add);
                }

                if (!result.isEmpty()) {
                    result.removeAll(projectRepository
                            .listProjectUsersWithPermissions(projectModel.getObject()));
                }

                return result;
            }

            @Override
            public void convertInput()
            {
                if (!config.isHideUsers()) {
                    super.convertInput();
                    return;
                }

                var values = this.getInputAsArray();
                if (values == null) {
                    return;
                }

                List<User> result = new ArrayList<>();
                for (var value : values) {
                    if (isBlank(value)) {
                        continue;
                    }

                    // We get the formatted user (possibly with display name) back, so we cannot
                    // look up the user directly in the repository and use the list of recently
                    // formatted users instead to perform the lookup
                    User user = recentUsers.get(value);
                    if (user != null) {
                        result.add(user);
                    }
                }
                this.setConvertedInput(result);
            }
        };
        usersToAdd.setModel(usersToAddModel);
        form.add(usersToAdd);

        form.add(new LambdaAjaxButton<>("add", this::actionAdd));
        form.add(new LambdaAjaxButton<>("new", this::actionNew));
    }

    private List<ProjectUserPermissions> listUsersWithPermissions()
    {
        return projectRepository.listProjectUserPermissions(projectModel.getObject());
    }

    private void actionNew(AjaxRequestTarget aTarget, Form<List<User>> aForm)
    {
    }

    private void actionAdd(AjaxRequestTarget aTarget, Form<List<User>> aForm)
    {
        for (var user : aForm.getModelObject()) {
            projectRepository.assignRole(projectModel.getObject(), user, ANNOTATOR);
        }

        aForm.getModelObject().clear();

        aTarget.add(this);
    }
}
