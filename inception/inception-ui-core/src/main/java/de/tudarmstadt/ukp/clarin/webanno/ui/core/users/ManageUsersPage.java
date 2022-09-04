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

import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.isProfileSelfServiceAllowed;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

/**
 * Manage Application wide Users.
 */
@MountPath("/users.html")
public class ManageUsersPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    public static final String REALM_PROJECT_PREFIX = "project:";

    public static final String PARAM_USER = "user";

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;

    private DropDownChoice<Realm> realm;
    private LambdaAjaxLink createButton;
    private UserTable table;
    private UserDetailPanel details;

    private IModel<User> selectedUser;

    public ManageUsersPage()
    {
        commonInit();

        // If the user is not an admin, then pre-load the current user to allow self-service
        // editing of the profile
        if (!userRepository.isCurrentUserAdmin() && isProfileSelfServiceAllowed()) {
            selectedUser.setObject(userRepository.getCurrentUser());
        }
    }

    public ManageUsersPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        commonInit();

        String username = aPageParameters.get(PARAM_USER).toOptionalString();
        User user = null;
        if (StringUtils.isNotBlank(username)) {
            user = userRepository.get(username);
        }

        if (userRepository.isCurrentUserAdmin()) {
            selectedUser.setObject(user);
        }
        else if (user != null && isProfileSelfServiceAllowed()
                && userRepository.getCurrentUsername().equals(user.getUsername())) {
            selectedUser.setObject(userRepository.getCurrentUser());
        }
        else {
            // Make sure a user doesn't try to access the profile of another user via the
            // parameter if self-service is turned on.
            setResponsePage(getApplication().getHomePage());
        }
    }

    private void commonInit()
    {
        // If the user is not an admin and self-service is not allowed, go back to the main page
        if (!userRepository.isCurrentUserAdmin() && !isProfileSelfServiceAllowed()) {
            setResponsePage(getApplication().getHomePage());
        }

        selectedUser = Model.of();

        var selectPanel = new WebMarkupContainer("selectPanel");
        selectPanel.add(LambdaBehavior.visibleWhen(userRepository::isCurrentUserAdmin));
        queue(selectPanel);

        realm = new DropDownChoice<>("realm");
        realm.setChoices(LoadableDetachableModel.of(this::listRealms));
        realm.setChoiceRenderer(new ChoiceRenderer<>("name"));
        realm.setModel(Model.of(realm.getChoicesModel().getObject().get(0)));
        realm.setOutputMarkupId(true);
        realm.add(visibleWhen(() -> realm.getChoicesModel().getObject().size() > 1));
        realm.add(LambdaAjaxFormComponentUpdatingBehavior.onUpdate("change", _target -> {
            table.getDataProvider().refresh();
            _target.add(table, createButton);
        }));
        queue(realm);

        table = new UserTable("users", selectedUser, LoadableDetachableModel.of(this::listUsers));
        table.setOutputMarkupPlaceholderTag(true);
        queue(table);

        details = new UserDetailPanel("details", selectedUser);
        details.setOutputMarkupPlaceholderTag(true);
        details.add(visibleWhen(selectedUser.map(Objects::nonNull)));
        queue(details);

        createButton = new LambdaAjaxLink("create", this::actionCreate);
        createButton.setOutputMarkupPlaceholderTag(true);
        queue(createButton);

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
        return userRepository.list().stream() //
                .filter(u -> Objects.equals(u.getRealm(), realm.getModelObject().getId())) //
                .collect(toList());
    }

    @OnEvent
    public void onSelectUser(SelectUserEvent aEvent)
    {
        details.setCreatingNewUser(false);
        selectedUser.setObject(aEvent.getUser());
        // Get the inner table for refresh to avoid a reset of the scrolling position
        aEvent.getTarget().add(table.getInnerTable(), details);
    }

    @OnEvent
    public void onUserSaved(UserSavedEvent aEvent)
    {
        table.getDataProvider().refresh();

        if (userRepository.isCurrentUserAdmin()) {
            selectedUser.setObject(null);
        }

        aEvent.getTarget().add(table.getInnerTable(), details);
    }

    private void actionCreate(AjaxRequestTarget aTarget)
    {
        User user = new User();
        user.setEnabled(true);
        user.setRoles(Set.of(ROLE_USER));
        selectedUser.setObject(user);
        details.setCreatingNewUser(true);
        aTarget.add(table.getInnerTable(), details);
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
