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
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarProperties;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class, showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
@EnableAutoConfiguration
@EntityScan({ //
        "de.tudarmstadt.ukp.inception.curation", //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@ExtendWith(MockitoExtension.class)
public class CurationSidebarServiceTest
{
    private CurationSidebarService sut;

    private @Autowired TestEntityManager testEntityManager;

    private @MockitoBean DocumentService documentService;
    private @MockitoBean SessionRegistry sessionRegistry;
    private @MockitoBean ProjectService projectService;
    private @MockitoBean UserDao userRegistry;
    private @MockitoBean CasStorageService casStorageService;
    private @MockitoBean CurationService curationService;
    private @MockitoBean CurationMergeService curationMergeService;
    private @MockitoBean CurationSidebarProperties curationSidebarProperties;
    private @MockitoBean CurationDocumentService curationDocumentService;

    private Project testProject;
    private SourceDocument testDocument;
    private User beate;
    private User kevin;

    @BeforeEach
    public void setUp() throws Exception
    {
        sut = new CurationSidebarServiceImpl(testEntityManager.getEntityManager(), documentService,
                sessionRegistry, projectService, userRegistry, casStorageService, curationService,
                curationMergeService, curationSidebarProperties, curationDocumentService);

        // create users
        var current = new User("current", ROLE_USER);
        beate = new User("beate", ROLE_USER);
        kevin = new User("kevin", ROLE_USER);
        testEntityManager.persist(current);
        testEntityManager.persist(beate);
        testEntityManager.persist(kevin);

        // create project
        testProject = new Project("test-project");
        testEntityManager.persist(testProject);
        testEntityManager.persist(new ProjectPermission(testProject, "beate", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "kevin", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "beate", CURATOR));

        // create source document
        testDocument = new SourceDocument("testDoc", testProject, "text");
        testEntityManager.persist(testDocument);

        sut.setSelectedUsers("current", testProject.getId(), asList(kevin, beate));
    }

    @AfterEach
    public void tearDown()
    {
        testEntityManager.clear();
    }

    @Test
    public void listUsersReadyForCuration_NoFinishedUsers()
    {
        var finishedUsers = sut.listUsersReadyForCuration("current", testProject, testDocument);

        assertThat(finishedUsers).isEmpty();
    }

    @SpringBootConfiguration
    public static class SpringConfig
    {
        // No content
    }
}
