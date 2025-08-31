/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
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
package de.tudarmstadt.ukp.clarin.webanno.project;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.Project.MAX_PROJECT_SLUG_LENGTH;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.logging.Logging;

@DataJpaTest( //
        excludeAutoConfiguration = LiquibaseAutoConfiguration.class, //
        showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
@EnableAutoConfiguration
@Import({ //
        RepositoryAutoConfiguration.class, //
        SecurityAutoConfiguration.class })
@EntityScan(basePackages = { "de.tudarmstadt.ukp.clarin.webanno.project",
        "de.tudarmstadt.ukp.clarin.webanno.model",
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
public class ProjectServiceImplTest
{
    @TempDir
    File repositoryDir;

    private ProjectService sut;

    private @Autowired TestEntityManager testEntityManager;
    private @Autowired UserDao userService;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired ApplicationEventPublisher applicationEventPublisher;

    private Project testProject;
    private Project testProject2;
    private Project testProjectManagedByKevin;
    private Project testProjectManagedByBeate;
    private User beate;
    private User kevin;
    private User noPermissionUser;

    @BeforeEach
    public void setUp() throws Exception
    {
        repositoryProperties.setPath(repositoryDir);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        sut = new ProjectServiceImpl(userService, applicationEventPublisher, repositoryProperties,
                null, null, testEntityManager.getEntityManager());

        // create users
        beate = new User("beate", Role.ROLE_USER, Role.ROLE_ADMIN);
        kevin = new User("kevin", Role.ROLE_USER);
        noPermissionUser = new User("noPermission", Role.ROLE_USER);
        testEntityManager.persist(beate);
        testEntityManager.persist(noPermissionUser);
        testEntityManager.persist(kevin);

        // create project and projectPermissions for users
        testProject = new Project("test-project");
        testEntityManager.persist(testProject);
        testEntityManager.persist(new ProjectPermission(testProject, "beate", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "kevin", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "beate", CURATOR));

        // create additional project and projectPermissions for users
        testProject2 = new Project("test-project2");
        testEntityManager.persist(testProject2);
        testEntityManager.persist(new ProjectPermission(testProject2, "beate", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject2, "beate", CURATOR));

        testProjectManagedByKevin = new Project("managed-by-kevin");
        testEntityManager.persist(testProjectManagedByKevin);
        testEntityManager
                .persist(new ProjectPermission(testProjectManagedByKevin, "kevin", MANAGER));

        testProjectManagedByBeate = new Project("managed-by-beate");
        testEntityManager.persist(testProjectManagedByBeate);
        testEntityManager
                .persist(new ProjectPermission(testProjectManagedByBeate, "beate", MANAGER));
    }

    @AfterEach
    public void tearDown()
    {
        testEntityManager.clear();
    }

    @Test
    public void thatListAccessibleProjectsWorks()
    {
        assertThat(sut.listAccessibleProjects(beate)) //
                .contains(testProject, testProject2, testProjectManagedByBeate,
                        testProjectManagedByKevin);

        assertThat(sut.listAccessibleProjects(kevin)) //
                .contains(testProject, testProjectManagedByKevin);

        assertThat(sut.listAccessibleProjects(noPermissionUser)) //
                .isEmpty();
    }

    @Test
    public void listProjectUsersWithPermissions_ShouldReturnUsers()
    {
        List<User> foundUsers = sut.listUsersWithAnyRoleInProject(testProject);

        assertThat(foundUsers).containsExactly(beate, kevin);
    }

    @Test
    public void listProjectUsersWithSpecificPermissions_ShouldReturnUsers()
    {
        List<User> foundUsers = sut.listUsersWithRoleInProject(testProject, ANNOTATOR);

        assertThat(foundUsers).containsExactly(beate, kevin);
    }

    @Test
    public void listProjectUsersWithSpecificPermissions_ShouldReturnAUser()
    {
        List<User> foundUsers = sut.listUsersWithRoleInProject(testProject, CURATOR);

        assertThat(foundUsers).containsExactly(beate);
    }

    @Test
    public void listProjectUsersWithSpecificPermissions_ShouldReturnNoUsers()
    {
        List<User> foundUsers = sut.listUsersWithRoleInProject(testProject, MANAGER);

        assertThat(foundUsers).isEmpty();
    }

    @Test
    public void listProjectUsersWithPermissionButNoTableEntry_ShouldNotReturnThisUser()
    {
        testEntityManager.persist(new ProjectPermission(testProject, "ghost", ANNOTATOR));

        List<User> foundUsers = sut.listUsersWithRoleInProject(testProject, ANNOTATOR);

        assertThat(foundUsers).containsExactly(beate, kevin);
    }

    @Test
    public void thatDerivingSlugFromProjectNameWorks()
    {
        assertThat(sut.deriveSlugFromName("This is a test")).isEqualTo("this-is-a-test");
        assertThat(sut.deriveSlugFromName(" This is a test")).isEqualTo("this-is-a-test");
        assertThat(sut.deriveSlugFromName("This is a test ")).isEqualTo("this-is-a-test");
        assertThat(sut.deriveSlugFromName("Nö, mog I net")).isEqualTo("n_-mog-i-net");
        assertThat(sut.deriveSlugFromName("hey 😎 name")).isEqualTo("hey-_-name");
        assertThat(sut.deriveSlugFromName("😎")).isEqualTo("");
        assertThat(sut.deriveSlugFromName("")).isEqualTo("");
        assertThat(sut.deriveSlugFromName(null)).isEqualTo(null);
        assertThat(sut.deriveSlugFromName("x")).isEqualTo("x__");
        assertThat(sut.deriveSlugFromName("1")).isEqualTo("x1_");
    }

    @Test
    public void thatManagesAnyProjectWorks()
    {
        assertThat(sut.managesAnyProject(noPermissionUser)).isFalse();
        assertThat(sut.managesAnyProject(beate)).isTrue();
        assertThat(sut.managesAnyProject(kevin)).isTrue();
    }

    @Test
    public void thatGenerationOfUniqueSlugWorks()
    {
        ProjectService projectSerivce = Mockito.spy(sut);
        when(projectSerivce.deriveUniqueSlug(any())).thenCallRealMethod();

        when(projectSerivce.existsProjectWithSlug(any())).then(answer -> {
            String slug = answer.getArgument(0, String.class);
            char lastChar = slug.charAt(slug.length() - 1);
            return lastChar < '9' || lastChar >= 'A';
        });

        assertThat(projectSerivce.deriveUniqueSlug("this-is-a-test")).isEqualTo("this-is-a-test-9");

        assertThat(projectSerivce.deriveUniqueSlug(repeat('x', MAX_PROJECT_SLUG_LENGTH * 2)))
                .isEqualTo(repeat('x', MAX_PROJECT_SLUG_LENGTH - 2) + "-9")
                .hasSize(MAX_PROJECT_SLUG_LENGTH);
    }

    @Test
    public void thatHasRoleWorks()
    {
        assertThat(sut.hasRole(beate, testProject, ANNOTATOR)).isTrue();
        assertThat(sut.hasRole(beate, testProject, CURATOR)).isTrue();
        assertThat(sut.hasRole(beate, testProject, CURATOR, ANNOTATOR)).isTrue();
        assertThat(sut.hasRole(beate, testProjectManagedByBeate, MANAGER)).isTrue();

        assertThat(sut.hasRole(kevin, testProjectManagedByBeate, MANAGER)).isFalse();

        assertThat(sut.hasRole(noPermissionUser, testProject, ANNOTATOR, CURATOR, MANAGER))
                .isFalse();
        assertThat(sut.hasRole(noPermissionUser, testProject2, ANNOTATOR, CURATOR, MANAGER))
                .isFalse();
        assertThat(sut.hasRole(noPermissionUser, testProjectManagedByBeate, ANNOTATOR, CURATOR,
                MANAGER)).isFalse();
        assertThat(sut.hasRole(noPermissionUser, testProjectManagedByKevin, ANNOTATOR, CURATOR,
                MANAGER)).isFalse();
    }

    @Test
    public void thatHasAnyRoleWorks()
    {
        assertThat(sut.hasAnyRole(beate, testProject)).isTrue();
        assertThat(sut.hasAnyRole(beate, testProject2)).isTrue();
        assertThat(sut.hasAnyRole(beate, testProjectManagedByBeate)).isTrue();
        assertThat(sut.hasAnyRole(beate, testProjectManagedByKevin)).isFalse();
    }

    @Test
    void thatAssigningAndRevokingRolesWorks()
    {
        sut.revokeAllRoles(testProject, beate);
        assertThat(sut.listRoles(testProject, beate)).isEmpty();

        sut.assignRole(testProject, beate, MANAGER);
        assertThat(sut.listRoles(testProject, beate)) //
                .containsExactlyInAnyOrder(MANAGER);

        sut.assignRole(testProject, beate, ANNOTATOR, CURATOR);
        assertThat(sut.listRoles(testProject, beate)) //
                .containsExactlyInAnyOrder(MANAGER, ANNOTATOR, CURATOR);

        sut.revokeRole(testProject, beate, CURATOR);
        assertThat(sut.listRoles(testProject, beate)) //
                .containsExactlyInAnyOrder(MANAGER, ANNOTATOR);

        sut.revokeRole(testProject, beate, CURATOR); // Yes, we try it a second time
        assertThat(sut.listRoles(testProject, beate)) //
                .containsExactlyInAnyOrder(MANAGER, ANNOTATOR);

        sut.revokeRole(testProject, beate, MANAGER, ANNOTATOR);
        assertThat(sut.listRoles(testProject, beate)).isEmpty();
    }

    @SpringBootConfiguration
    public static class SpringConfig
    {
        // No content
    }
}
