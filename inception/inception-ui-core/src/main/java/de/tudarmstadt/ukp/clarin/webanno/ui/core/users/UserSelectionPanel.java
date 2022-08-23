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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.users;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;

class UserSelectionPanel
    extends ListPanel_ImplBase
{
    private static final long serialVersionUID = -1L;

    public static final String REALM_PROJECT_PREFIX = "project:";

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;

    private final OverviewListChoice<User> overviewList;
    private final DropDownChoice<Realm> realm;
    private final CheckBox showDisabled;
    private final LambdaAjaxLink createButton;

    public UserSelectionPanel(String id, IModel<User> aModel)
    {
        super(id);
        setOutputMarkupPlaceholderTag(true);

        overviewList = new OverviewListChoice<>("user");
        overviewList.setChoiceRenderer(new ChoiceRenderer<User>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(User aUser)
            {
                StringBuilder builder = new StringBuilder();
                builder.append(aUser.getUiName());
                if (!aUser.getUsername().equals(aUser.getUiName())) {
                    builder.append(" (");
                    builder.append(aUser.getUsername());
                    builder.append(")");
                }
                if (!aUser.isEnabled()) {
                    builder.append(" (deactivated)");
                }
                return builder.toString();
            }
        });
        overviewList.setModel(aModel);
        overviewList.setChoices(this::listUsers);
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
        add(overviewList);

        createButton = new LambdaAjaxLink("create", this::actionCreate);
        createButton.setOutputMarkupPlaceholderTag(true);
        add(createButton);

        showDisabled = new CheckBox("showDisabled", Model.of(false));
        showDisabled.add(LambdaAjaxFormComponentUpdatingBehavior.onUpdate("change",
                this::toggleShowDisabled));
        add(showDisabled);

        realm = new DropDownChoice<>("realm");
        realm.setChoices(LoadableDetachableModel.of(this::listRealms));
        realm.setChoiceRenderer(new ChoiceRenderer<>("name"));
        realm.setModel(Model.of(realm.getChoicesModel().getObject().get(0)));
        realm.setOutputMarkupId(true);
        realm.add(visibleWhen(() -> realm.getChoicesModel().getObject().size() > 1));
        realm.add(LambdaAjaxFormComponentUpdatingBehavior.onUpdate("change",
                _target -> _target.add(overviewList, createButton)));
        add(realm);

        // Only allow creating accounts in the global realm
        createButton.add(visibleWhen(() -> realm.getModelObject().getId() == null));
    }

    private List<Realm> listRealms()
    {
        return userRepository.listRealms().stream().map(_id -> {
            if (_id == null) {
                return new Realm(_id, "<GLOBAL>");
            }
            else if (startsWith(_id, REALM_PROJECT_PREFIX)) {
                long projectId = Long.valueOf(substringAfter(_id, REALM_PROJECT_PREFIX));
                Project project = projectService.getProject(projectId);
                if (project != null) {
                    return new Realm(_id, project.getName());
                }
                else {
                    return new Realm(_id, "<Deleted project: " + _id + ">");
                }
            }
            else {
                return new Realm(_id, "<" + _id + ">");
            }
        }).sorted(this::compareRealms).collect(toList());
    }

    private List<User> listUsers()
    {
        List<User> users;

        if (showDisabled.getModelObject()) {
            users = userRepository.listDisabledUsers();
        }
        else {
            users = userRepository.listEnabledUsers();
        }

        return users.stream() //
                .filter(u -> Objects.equals(u.getRealm(), realm.getModelObject().getId())) //
                .sorted(comparing(User::getUiName)) //
                .collect(Collectors.toList());
    }

    private void toggleShowDisabled(AjaxRequestTarget aTarget)
    {
        aTarget.add(overviewList);
    }

    private int compareRealms(Realm aOne, Realm aOther)
    {
        if (aOne.getId() == null && aOther.getId() == null) {
            return 0;
        }

        if (aOne.getId() == null) {
            return -1;
        }

        if (aOther.getId() == null) {
            return 1;
        }

        return StringUtils.compare(aOne.getName(), aOther.getName());
    }
}
