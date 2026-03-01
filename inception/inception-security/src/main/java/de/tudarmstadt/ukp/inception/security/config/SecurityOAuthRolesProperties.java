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
package de.tudarmstadt.ukp.inception.security.config;

public interface SecurityOAuthRolesProperties
{
    /**
     * @return whether OAuth2/OIDC group-to-role mapping is enabled. When disabled, all
     *         authenticated users receive {@code ROLE_USER} regardless of their group memberships.
     */
    boolean isEnabled();

    /**
     * @return the name of the OAuth2/OIDC token claim that contains the user's group memberships.
     *         Defaults to {@code groups}.
     */
    String getClaim();

    /**
     * @return the group name that maps to {@code ROLE_ADMIN}.
     */
    String getAdmin();

    /**
     * @return the group name that maps to {@code ROLE_USER}.
     */
    String getUser();

    /**
     * @return the group name that maps to {@code ROLE_PROJECT_CREATOR}.
     */
    String getProjectCreator();

    /**
     * @return the group name that maps to {@code ROLE_REMOTE}.
     */
    String getRemote();
}
