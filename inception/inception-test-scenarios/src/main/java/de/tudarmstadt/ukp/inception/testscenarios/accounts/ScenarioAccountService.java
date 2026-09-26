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
package de.tudarmstadt.ukp.inception.testscenarios.accounts;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;

import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.testscenarios.config.InceptionTestScenariosAutoConfiguration;

/**
 * The only place that creates the {@link ScenarioAccount scenario accounts}.
 * <p>
 * <b>This class is exposed as a Spring Component via
 * {@link InceptionTestScenariosAutoConfiguration#scenarioAccountService}.</b>
 */
public class ScenarioAccountService
{
    /**
     * The password of every scenario account.
     */
    public static final String PASSWORD = "admin";

    /**
     * bcrypt hash of {@link #PASSWORD}, with the {@code {bcrypt}} encoder id the
     * {@code DelegatingPasswordEncoder} needs. {@code UserDao.create()} persists the entity as it
     * stands and does no encoding, so a plain password would be stored as literal text and the
     * account could never log in. Pre-encoding also sidesteps the 8-character minimum the
     * registration form applies.
     */
    private static final String PASSWORD_HASH = "{bcrypt}"
            + "$2b$10$oTEUFYeS22.xQrZ6sh.PqeoMk9YmR7mWi3wOrYbD3QiFrfNdzt4r.";

    private final UserDao userService;
    private final ProjectService projectService;

    public ScenarioAccountService(UserDao aUserService, ProjectService aProjectService)
    {
        userService = aUserService;
        projectService = aProjectService;
    }

    /**
     * Makes the account a member of the project with the roles the account declares.
     * <p>
     * The account is created if it does not exist yet. An existing account is used as it is - its
     * password and enabled state are left alone, so an administrator can still lock it. It is only
     * accepted if it could have been created here: a local account with no global role besides
     * {@code ROLE_USER}. Anything else is a real account that happens to carry the name - e.g. one
     * materialized by an external login - and must not be pulled into a scenario project.
     *
     * @param aProject
     *            the project to join.
     * @param aAccount
     *            the account.
     * @return the user.
     * @throws IllegalStateException
     *             if an account with the name exists but is not a scenario account.
     */
    public User join(Project aProject, ScenarioAccount aAccount)
    {
        var user = userService.get(aAccount.getUsername());

        if (user == null) {
            user = User.builder() //
                    .withUsername(aAccount.getUsername()) //
                    .withUiName(aAccount.getUiName()) //
                    .withEnabled(true) //
                    .withRoles(ROLE_USER) //
                    .build();
            user.setEncodedPassword(PASSWORD_HASH);
            user = userService.create(user);
        }
        else if (user.getRealm() != null || !Set.of(ROLE_USER).equals(user.getRoles())) {
            throw new IllegalStateException("Account [" + aAccount.getUsername()
                    + "] exists but is not a scenario account (realm [" + user.getRealm()
                    + "], roles " + user.getRoles() + "). Rename or remove it to use the test "
                    + "scenarios.");
        }

        projectService.assignRole(aProject, user,
                aAccount.getProjectRoles().toArray(PermissionLevel[]::new));

        return user;
    }
}
