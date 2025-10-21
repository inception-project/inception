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

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Strings.CS;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.wicket.RestartResponseException;
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

import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.security.oauth.OAuth2Adapter;
import de.tudarmstadt.ukp.inception.security.saml.Saml2Adapter;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;

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

    private @SpringBean UserDao userService;
    private @SpringBean ProjectService projectService;
    private @SpringBean OAuth2Adapter oAuth2Adapter;
    private @SpringBean Saml2Adapter saml2Adapter;

    private DropDownChoice<Realm> realm;
    private LambdaAjaxLink createButton;
    private UserTable table;
    private UserDetailPanel details;

    private IModel<User> selectedUser;

    public ManageUsersPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        selectedUser = Model.of();

        checkAccess(aPageParameters);

        commonInit();
    }

    private void checkAccess(final PageParameters aPageParameters)
    {
        var username = aPageParameters.get(PARAM_USER).toOptionalString();

        var currentUser = userService.getCurrentUser();
        var userToOpen = isBlank(username) ? currentUser : userService.get(username);

        // Admins can manage any user
        if (userService.isCurrentUserAdmin()) {
            selectedUser.setObject(userToOpen);
        }
        // Non-admins can only manage themselves if profile self-service is allowed
        else if (userToOpen.equals(currentUser)
                && userService.isProfileSelfServiceAllowed(currentUser)) {
            selectedUser.setObject(currentUser);
        }
        // Other cases are denied
        else {
            // Make sure a user doesn't try to access the profile of another user via the
            // parameter if self-service is turned on.
            denyAccess();
        }
    }

    private void denyAccess()
    {
        getSession().error(format("Access to [%s] denied.", getClass().getSimpleName()));
        throw new RestartResponseException(getApplication().getHomePage());
    }

    private void commonInit()
    {
        var selectPanel = new WebMarkupContainer("selectPanel");
        selectPanel.add(LambdaBehavior.visibleWhen(userService::isCurrentUserAdmin));
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

        // Only allow creating accounts in the local realm
        createButton.add(visibleWhen(() -> realm.getModelObject().getId() == null));
    }

    private List<Realm> listRealms()
    {
        var realms = new ArrayList<Realm>();

        userService.listRealms().stream() //
                .map(_id -> {
                    final CharSequence str = _id;
                    if (CS.startsWith(str, REALM_PROJECT_PREFIX)) {
                        return projectService.getRealm(_id);
                    }
                    else {
                        return new Realm(_id);
                    }
                }).forEach(realms::add);

        // Add the realms from the external authentication providers. Note that multiple providers
        // might use the same registration. E.g. the SAML IdP might be registered as an OAuth and
        // simultaneously as a SAML2 provider. It does not make much sense to be honest, but it
        // is possible.
        oAuth2Adapter.getOAuthClientRegistrations()
                .forEach(reg -> realms.add(Realm.forExternalOAuth(reg)));
        saml2Adapter.getSamlRelyingPartyRegistrations()
                .forEach(((uri, regId) -> realms.add(Realm.forExternalSaml(uri, regId))));

        // If there is a choice, then the local realm should always be a part of it
        if (!realms.isEmpty()) {
            realms.add(Realm.local());
        }

        return realms.stream() //
                .sorted(Realm::compareRealms) //
                .distinct() //
                .collect(toList());
    }

    private List<UserTableRow> listUsers()
    {
        return userService.list().stream() //
                .filter(u -> Objects.equals(u.getRealm(), realm.getModelObject().getId())) //
                .map(UserTableRow::new) //
                .toList();
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

        if (userService.isCurrentUserAdmin()) {
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
}
