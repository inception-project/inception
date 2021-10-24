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

package de.tudarmstadt.ukp.inception.ui.curation.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class, showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
@EnableAutoConfiguration
@EntityScan({ //
        "de.tudarmstadt.ukp.inception.curation", //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
public class CurationServiceTest
{
    private CurationSidebarService sut;

    @Autowired
    private TestEntityManager testEntityManager;

    private Project testProject;
    private SourceDocument testDocument;
    private User beate;
    private User kevin;

    @BeforeEach
    public void setUp() throws Exception
    {
        sut = new CurationSidebarServiceImpl(testEntityManager.getEntityManager(), null, null, null,
                null, null);

        // create users
        User current = new User("current", Role.ROLE_USER);
        beate = new User("beate", Role.ROLE_USER);
        kevin = new User("kevin", Role.ROLE_USER);
        testEntityManager.persist(current);
        testEntityManager.persist(beate);
        testEntityManager.persist(kevin);

        // create project
        testProject = new Project("test-project");
        testEntityManager.persist(testProject);
        testEntityManager.persist(new ProjectPermission(testProject, "beate", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "kevin", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "beate", CURATOR));

        // create sourcedocument
        testDocument = new SourceDocument("testDoc", testProject, "text");
        testEntityManager.persist(testDocument);

        // add selected users to sut
        List<User> selectedUsers = new ArrayList<>();
        selectedUsers.add(kevin);
        selectedUsers.add(beate);
        sut.updateUsersSelectedForCuration("current", testProject.getId(), selectedUsers);
    }

    @AfterEach
    public void tearDown()
    {
        testEntityManager.clear();
    }

    @Test
    public void listUsersReadyForCuration_ShouldReturnFinishedUsers()
    {
        // create finished annotation documents
        AnnotationDocument annoDoc1 = new AnnotationDocument("beate", testDocument);
        annoDoc1.setState(AnnotationDocumentState.FINISHED);
        AnnotationDocument annoDoc2 = new AnnotationDocument("kevin", testDocument);
        annoDoc2.setState(AnnotationDocumentState.FINISHED);
        testEntityManager.persist(annoDoc1);
        testEntityManager.persist(annoDoc2);

        List<User> finishedUsers = sut.listUsersReadyForCuration("current", testProject,
                testDocument);

        assertThat(finishedUsers).containsExactly(beate, kevin);
    }

    @Test
    public void listFinishedUsers_ShouldReturnFinishedUsers()
    {
        // create finished annotation documents
        AnnotationDocument annoDoc1 = new AnnotationDocument("beate", testDocument);
        annoDoc1.setState(AnnotationDocumentState.FINISHED);
        AnnotationDocument annoDoc2 = new AnnotationDocument("kevin", testDocument);
        annoDoc2.setState(AnnotationDocumentState.FINISHED);
        testEntityManager.persist(annoDoc1);
        testEntityManager.persist(annoDoc2);

        List<User> finishedUsers = sut.listFinishedUsers(testProject, testDocument);

        assertThat(finishedUsers).containsExactly(beate, kevin);
    }

    @Test
    public void listUsersReadyForCuration_NoFinishedUsers()
    {
        List<User> finishedUsers = sut.listUsersReadyForCuration("current", testProject,
                testDocument);

        assertThat(finishedUsers).isEmpty();
    }

    @SpringBootConfiguration
    public static class SpringConfig
    {
        // No content
    }
}
