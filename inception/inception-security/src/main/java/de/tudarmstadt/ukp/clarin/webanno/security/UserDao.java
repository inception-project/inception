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
package de.tudarmstadt.ukp.clarin.webanno.security;

import static de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider.getApplicationContext;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.security.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

/**
 * Provide methods for user management such as create, update, list users
 */
public interface UserDao
{
    static final String ADMIN_DEFAULT_USERNAME = "admin";
    static final String ADMIN_DEFAULT_PASSWORD = "admin";

    static final String EMPTY_PASSWORD = "";

    static final String REALM_GLOBAL = null;
    static final String REALM_PROJECT_PREFIX = "project:";

    User getCurrentUser();

    /**
     * @return the name of the current user
     */
    String getCurrentUsername();

    boolean isCurrentUserAdmin();

    /**
     * Create a new {@link User}
     * 
     * @param aUser
     *            the user to create.
     * @return the user.
     */
    User create(User aUser);

    /**
     * Update existing {@link User}
     * 
     * @param aUser
     *            the user.
     * @return the user.
     */
    User update(User aUser);

    /**
     * check if a user with this username exists
     * 
     * @param aUsername
     *            the username.
     * @return if the user exists.
     */
    boolean exists(final String aUsername);

    /**
     * delete a user using the username (currently disabled, 1.0.0)
     * 
     * @param aUsername
     *            the username.
     * @return how many users were deleted.
     */
    int delete(String aUsername);

    /**
     * delete this {@link User}
     * 
     * @param aUser
     *            the user.
     */
    void delete(User aUser);

    int deleteAllUsersFromRealm(String aString);

    List<User> listAllUsersFromRealm(String aString);

    /**
     * get a {@link User} using a username
     * 
     * @param aUsername
     *            the username.
     * @return the user.
     */
    User get(String aUsername);

    User getUserByRealmAndUiName(String aRealm, String aUiName);

    /**
     * @return all users in the system
     */
    List<User> list();

    List<User> listEnabledUsers();

    List<User> listDisabledUsers();

    /**
     * @return the roles of a user, globally we will have ROLE_ADMIN and ROLE_USER
     *
     * @param user
     *            the {@link User} object
     */
    List<Authority> listAuthorities(User user);

    /**
     * @param aUser
     *            a user
     * @return if the user has global administrator permissions.
     */
    boolean isAdministrator(User aUser);

    /**
     * @param aUser
     *            a user
     * @return if the user has the permission to create projects.
     */
    boolean isProjectCreator(User aUser);

    Set<String> getRoles(User aUser);

    /**
     * @return the number of enabled users
     */
    long countEnabledUsers();

    List<String> listRealms();

    public static boolean isProfileSelfServiceAllowed()
    {
        // If users are allowed to access their profile information, the also need to access the
        // admin area. Note: access to the users own profile should be handled differently.
        List<String> activeProfiles = asList(
                getApplicationContext().getEnvironment().getActiveProfiles());
        Properties settings = SettingsUtil.getSettings();
        return !activeProfiles.contains("auto-mode-preauth")
                && "true".equals(settings.getProperty(SettingsUtil.CFG_USER_ALLOW_PROFILE_ACCESS));
    }

    boolean hasRole(User aUser, Role aRole);
}
