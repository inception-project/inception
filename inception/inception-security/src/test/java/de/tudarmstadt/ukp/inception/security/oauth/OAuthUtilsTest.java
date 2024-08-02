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

package de.tudarmstadt.ukp.inception.security.oauth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class OAuthUtilsTest
{

    private static final String USERNAME = "ThatGuy";
    private static final String OAUTH2_GROUP_USER = "/INCEPTION_USER";
    private static final String OAUTH2_GROUP_PROJECT_CREATOR = "/INCEPTION_PROJECT_CREATOR";
    private static final String OAUTH2_GROUP_ADMIN = "/INCEPTION_ADMIN";
    private static final String OAUTH2_GROUP_REMOTE = "/INCEPTION_REMOTE";

    User testUser;
    
    @BeforeEach
    void setup()
    {
        testUser = new User();
        testUser.setUsername(USERNAME);
        
        System.setProperty("inception.home", "src/test/resources");
        
    }

    @Test
    void thatAdminRoleIsGivenIfMatchingGroupFound()
    {
        ArrayList<String> userOAuth2Groups = new ArrayList<>();
        userOAuth2Groups.add(OAUTH2_GROUP_ADMIN);
        
        Set<Role> userRoles = OauthUtils.getOAuth2UserRoles(testUser, userOAuth2Groups);
        
        assertTrue(userRoles.contains(Role.ROLE_ADMIN));
    }
    
    @Test
    void thatUserRoleIsGivenIfMatchingGroupFound()
    {
        ArrayList<String> userOAuth2Groups = new ArrayList<>();
        userOAuth2Groups.add(OAUTH2_GROUP_USER);
        
        Set<Role> userRoles = OauthUtils.getOAuth2UserRoles(testUser, userOAuth2Groups);
        
        assertTrue(userRoles.contains(Role.ROLE_USER));
    }
    
    @Test
    void thatProjectCreatorRoleIsGivenIfMatchingGroupFound()
    {
        ArrayList<String> userOAuth2Groups = new ArrayList<>();
        userOAuth2Groups.add(OAUTH2_GROUP_PROJECT_CREATOR);
        
        Set<Role> userRoles = OauthUtils.getOAuth2UserRoles(testUser, userOAuth2Groups);
        
        assertTrue(userRoles.contains(Role.ROLE_PROJECT_CREATOR));
    }
    
    @Test
    void thatRemoteRoleIsGivenIfMatchingGroupFound()
    {
        ArrayList<String> userOAuth2Groups = new ArrayList<>();
        userOAuth2Groups.add(OAUTH2_GROUP_REMOTE);
        
        Set<Role> userRoles = OauthUtils.getOAuth2UserRoles(testUser, userOAuth2Groups);
        
        assertTrue(userRoles.contains(Role.ROLE_REMOTE));
    }

}
