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

package de.tudarmstadt.ukp.inception.curation;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@RunWith(SpringRunner.class)
@DataJpaTest
public class CurationServiceTest {
    
    private CurationService sut;
    
    @Autowired
    private TestEntityManager testEntityManager;
        
    private Project testProject;
    private SourceDocument testDocument;
    private User beate;
    private User kevin;
    
    @Before
    public void setUp() throws Exception
    {
        sut = new CurationServiceImpl(testEntityManager.getEntityManager());
        
        //create users
        User current = new User("current", Role.ROLE_USER);
        beate = new User("beate", Role.ROLE_USER);
        kevin = new User("kevin", Role.ROLE_USER);
        testEntityManager.persist(current);
        testEntityManager.persist(beate);
        testEntityManager.persist(kevin);
        
        //create project
        testProject = new Project("testProject");
        testEntityManager.persist(testProject);
        testEntityManager.persist(new ProjectPermission(testProject, "beate", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "kevin", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "beate", CURATOR));
        
        //create sourcedocument
        testDocument = new SourceDocument("testDoc", testProject, "text");
        testEntityManager.persist(testDocument);
        
        // add selected users to sut
        List<User> selectedUsers = new ArrayList<>();
        selectedUsers.add(kevin);
        selectedUsers.add(beate);
        sut.updateUsersSelectedForCuration("current", testProject.getId(), selectedUsers);
    }
    
    @After
    public void tearDown() {
        testEntityManager.clear();
    }

    @Test
    public void listUsersReadyForCuration_ShouldReturnFinishedUsers() {
        //create finished annotationdocuments
        AnnotationDocument annoDoc1 = new AnnotationDocument("testDoc", testProject, "beate",
                testDocument);
        annoDoc1.setState(AnnotationDocumentState.FINISHED);
        AnnotationDocument annoDoc2 = new AnnotationDocument("testDoc", testProject, "kevin",
                testDocument);
        annoDoc2.setState(AnnotationDocumentState.FINISHED);
        testEntityManager.persist(annoDoc1);
        testEntityManager.persist(annoDoc2);
        
        List<User> finishedUsers = sut.listUsersReadyForCuration("current", testProject,
                testDocument);
        
        assertThat(finishedUsers).hasSize(2);
        assertThat(finishedUsers).contains(beate, kevin);
    }
    
    @Test
    public void listUsersReadyForCuration_NoFinishedUsers() {
        List<User> finishedUsers = sut.listUsersReadyForCuration("current", testProject,
                testDocument);
        
        assertThat(finishedUsers).isEmpty();
    }
    
    @SpringBootConfiguration
    @EnableAutoConfiguration 
    @EntityScan(
            basePackages = {
                "de.tudarmstadt.ukp.inception.curation",
                "de.tudarmstadt.ukp.clarin.webanno.model",
                "de.tudarmstadt.ukp.clarin.webanno.security.model" 
    })
    public static class SpringConfig {
        // No content
    }
    
    
}
