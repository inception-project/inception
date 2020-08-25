/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.clarin.webanno.project;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@RunWith(SpringRunner.class)
@DataJpaTest
public class ProjectServiceImplTest
{
    private ProjectService sut;

    @Autowired
    private TestEntityManager testEntityManager;
        
    private Project testProject;
    private Project testProject2;
    private User beate;
    private User kevin;
    
    @Before
    public void setUp() throws Exception
    {
        sut = new ProjectServiceImpl(null, null, null, null, testEntityManager.getEntityManager());
        
        //create users
        beate = new User("beate", Role.ROLE_USER, Role.ROLE_ADMIN);
        kevin = new User("kevin", Role.ROLE_USER);
        User noPermissionUser = new User("noPermission", Role.ROLE_USER);
        testEntityManager.persist(beate);
        testEntityManager.persist(noPermissionUser);
        testEntityManager.persist(kevin);
        
        //create project and projectPermissions for users
        testProject = new Project("testProject");
        testEntityManager.persist(testProject);
        testEntityManager.persist(new ProjectPermission(testProject, "beate", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "kevin", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "beate", CURATOR));
        
        //create additional project and projectPermissions for users
        testProject2 = new Project("testProject2");
        testEntityManager.persist(testProject2);
        testEntityManager.persist(new ProjectPermission(testProject2, "beate", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject2, "beate", CURATOR));
    }
    
    @After
    public void tearDown() {
        testEntityManager.clear();
    }
    
    @SpringBootConfiguration
    @EnableAutoConfiguration 
    @EntityScan(
            basePackages = {
                "de.tudarmstadt.ukp.clarin.webanno.project",
                "de.tudarmstadt.ukp.clarin.webanno.model",
                "de.tudarmstadt.ukp.clarin.webanno.security.model" 
    })
    public static class SpringConfig {
        // No content
    }
    
    @Test
    public void listProjectsForAgreement_ShouldReturnOneProject()
    {
        List<Project> foundProjects = sut.listProjectsForAgreement();
        
        assertThat(foundProjects).containsExactly(testProject);
    }
    
    @Test
    public void listProjectUsersWithPermissions_ShouldReturnUsers() {
        List<User> foundUsers = sut.listProjectUsersWithPermissions(testProject);
        
        assertThat(foundUsers).containsExactly(beate, kevin);
    }

    @Test
    public void listProjectUsersWithSpecificPermissions_ShouldReturnUsers()
    {
        List<User> foundUsers = sut.listProjectUsersWithPermissions(testProject, ANNOTATOR);

        assertThat(foundUsers).containsExactly(beate, kevin);
    }

    @Test
    public void listProjectUsersWithSpecificPermissions_ShouldReturnAUser()
    {
        List<User> foundUsers = sut.listProjectUsersWithPermissions(testProject, CURATOR);

        assertThat(foundUsers).containsExactly(beate);
    }

    @Test
    public void listProjectUsersWithSpecificPermissions_ShouldReturnNoUsers()
    {
        List<User> foundUsers = sut.listProjectUsersWithPermissions(testProject, MANAGER);

        assertThat(foundUsers).isEmpty();
    }

    @Test
    public void listProjectUsersWithPermissionButNoTableEntry_ShouldNotReturnThisUser()
    {
        testEntityManager.persist(new ProjectPermission(testProject, "ghost", ANNOTATOR));

        List<User> foundUsers = sut.listProjectUsersWithPermissions(testProject, ANNOTATOR);

        assertThat(foundUsers).containsExactly(beate, kevin);
    }
}
