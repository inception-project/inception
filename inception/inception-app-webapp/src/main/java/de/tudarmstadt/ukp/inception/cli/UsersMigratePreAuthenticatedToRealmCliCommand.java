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
package de.tudarmstadt.ukp.inception.cli;

import static com.nimbusds.oauth2.sdk.util.CollectionUtils.contains;
import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.EMPTY_PASSWORD;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_REMOTE;
import static de.tudarmstadt.ukp.inception.support.SettingsUtil.getPropApplicationHome;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_AUTH_MODE_EXTERNAL_PREAUTH;
import static org.apache.commons.lang3.ArrayUtils.contains;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@ConditionalOnNotWebApplication
@Component
@Command( //
        name = "migrate-preauthenticated-users", //
        description = { //
                "Migrates all users that do not have a password and that do not have ROLE_REMOTE "
                        + "to the 'preauth' realm. This command should be run once after upgrading "
                        + "from  a version <25.0 to a version >=25.0 or higher on instances that "
                        + "use external pre-authentication." })
public class UsersMigratePreAuthenticatedToRealmCliCommand
    implements Callable<Integer>
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final UserDao userService;
    private final PasswordEncoder passwordEncoder;
    private final ConfigurableEnvironment environment;

    @Option(names = { "--dry-run" }, //
            description = "Print result of migration without actually performing it")
    private boolean dryRun;

    public UsersMigratePreAuthenticatedToRealmCliCommand(UserDao aUserService,
            PasswordEncoder aPasswordEncoder, ConfigurableEnvironment aEnvironment)
    {
        userService = aUserService;
        passwordEncoder = aPasswordEncoder;
        environment = aEnvironment;
    }

    @Override
    public Integer call() throws Exception
    {
        if (!contains(environment.getActiveProfiles(), PROFILE_AUTH_MODE_EXTERNAL_PREAUTH)) {
            LOG.error(
                    "This command is only intended for instances using external preauthentication!");
            LOG.error("If you are sure your instance is using external preauthentication, please "
                    + "ensure the configuration file can be found. Maybe you have to provide the "
                    + "application home path using '-D{}=/YOUR/APP/PATH'",
                    getPropApplicationHome());
            return 1;
        }

        LOG.info("Looking for users with no password to migrate them to the [] realm...",
                Realm.REALM_PREAUTH);
        if (dryRun) {
            LOG.info("Operating in dry-run mode - no changes applied to the database.");
        }

        int seen = 0;
        int updated = 0;
        for (User user : userService.list()) {
            seen++;
            if (user.getRealm() != null) {
                // User already has a realm
                LOG.info("User {} skipped: already in realm [{}]", user, user.getRealm());
                continue;
            }

            if (contains(user.getRoles(), ROLE_REMOTE)) {
                // Pre-authenticated users cannot log in through the remote API. Thus, if a user
                // has `ROLE_REMOTE`, that user is most likely not a pre-authenticated user.
                LOG.info("User {} skipped: has ROLE_REMOTE", user, user.getRealm());
                continue;
            }

            // If the user does not have a password, it is probably an external user
            // It could also be a user that was imported as part of a project import... we'll
            // take the risk and assume that in a pre-authenticated context, these are still
            // probably external users
            if (user.getPassword() == null
                    || passwordEncoder.matches(EMPTY_PASSWORD, user.getPassword())) {
                if (!dryRun) {
                    user.setRealm(Realm.REALM_PREAUTH);
                    userService.update(user);
                }
                LOG.info("User {} updated: moved to realm [{}]", user, Realm.REALM_PREAUTH);
                updated++;
            }
        }

        LOG.info("Migration complete. {} of {} users have been updated ", updated, seen);
        if (dryRun) {
            LOG.info(
                    "Migration was performd in dry-run mode - no changes applied to the database.");
        }

        return 0;
    }
}
