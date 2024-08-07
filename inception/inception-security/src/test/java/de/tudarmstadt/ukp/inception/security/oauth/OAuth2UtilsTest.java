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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OAuth2Utils.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@TestPropertySource(
        properties = """
            oauth2-groups.enabled=true
            oauth2-groups.admin=/INCEPTION_ADMIN
            oauth2-groups.user=/INCEPTION_USER
            oauth2-groups.project-creator=/INCEPTION_PROJECT_CREATOR
            oauth2-groups.remote=/INCEPTION_REMOTE
        """)
public class OAuth2UtilsTest
{
   
    String USERNAME = "ThatGuy";
    String OAUTH2_GROUP_USER = "/INCEPTION_USER";
    String OAUTH2_GROUP_PROJECT_CREATOR = "/INCEPTION_PROJECT_CREATOR";
    String OAUTH2_GROUP_ADMIN = "/INCEPTION_ADMIN";
    String OAUTH2_GROUP_REMOTE = "/INCEPTION_REMOTE";

    User testUser;
   
    @BeforeEach
    void setup() {
      testUser = new User();
      testUser.setUsername(USERNAME);
    }
    
    @Test
    void thatAdminRoleIsGivenIfMatchingGroupFound()
    {
        ArrayList<String> userOAuth2Groups = new ArrayList<>();
        userOAuth2Groups.add(OAUTH2_GROUP_ADMIN);
        
        Set<Role> userRoles = OAuth2Utils.getOAuth2UserRoles(testUser, userOAuth2Groups);
        
        assertTrue(userRoles.contains(Role.ROLE_ADMIN));
    }
    
    @Test
    void thatUserRoleIsGivenIfMatchingGroupFound()
    {
        ArrayList<String> userOAuth2Groups = new ArrayList<>();
        userOAuth2Groups.add(OAUTH2_GROUP_USER);
        
        Set<Role> userRoles = OAuth2Utils.getOAuth2UserRoles(testUser, userOAuth2Groups);
        
        assertTrue(userRoles.contains(Role.ROLE_USER));
    }
    
    @Test
    void thatProjectCreatorRoleIsGivenIfMatchingGroupFound()
    {
        ArrayList<String> userOAuth2Groups = new ArrayList<>();
        userOAuth2Groups.add(OAUTH2_GROUP_PROJECT_CREATOR);
        
        Set<Role> userRoles = OAuth2Utils.getOAuth2UserRoles(testUser, userOAuth2Groups);
        
        assertTrue(userRoles.contains(Role.ROLE_PROJECT_CREATOR));
    }
    
    @Test
    void thatRemoteRoleIsGivenIfMatchingGroupFound()
    {
        ArrayList<String> userOAuth2Groups = new ArrayList<>();
        userOAuth2Groups.add(OAUTH2_GROUP_REMOTE);
        
        Set<Role> userRoles = OAuth2Utils.getOAuth2UserRoles(testUser, userOAuth2Groups);
        
        assertTrue(userRoles.contains(Role.ROLE_REMOTE));
    }
    
    @Test
    void thatUnauthorizedExceptionIsThrownIfNoRoleIsMapped() {
        
        ArrayList<String> userOAuth2Groups = new ArrayList<>();
        
        try {
            OAuth2Utils.getOAuth2UserRoles(testUser, userOAuth2Groups);
        } catch (AccessDeniedException ade) {
            System.out.println(ade.getClass().getSimpleName() + " was thrown");
            return;
        }
        
        fail("Expected Exception wasn't catched");
    }
}
