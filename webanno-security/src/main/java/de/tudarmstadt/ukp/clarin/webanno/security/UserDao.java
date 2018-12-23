/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.security;

import java.util.List;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.security.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

/**
 * Provide methods for user management such as create, update, list users
 */
public interface UserDao
{
    User getCurrentUser();
    
    /**
     * Create a new {@link User}
     * 
     * @param aUser
     *            the user to create.
     */
    void create(User aUser);

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

    /**
     * get a {@link User} using a username
     * 
     * @param aUsername
     *            the username.
     * @return the user.
     */
    User get(String aUsername);

    /**
     * get all users in the system
     * 
     * @return the users.
     */
    List<User> list();
    
    /**
     * Returns a role of a user, globally we will have ROLE_ADMIN and ROLE_USER
     *
     * @param user
     *            the {@link User} object
     * @return the roles.
     */
    List<Authority> listAuthorities(User user);

    /**
     * Check if the user has global administrator permissions.
     */
    public boolean isAdministrator(User aUser);

    /**
     * Check if the user has the permission to create projects.
     */
    public boolean isProjectCreator(User aUser);

    public Set<String> getRoles(User aUser);
}
