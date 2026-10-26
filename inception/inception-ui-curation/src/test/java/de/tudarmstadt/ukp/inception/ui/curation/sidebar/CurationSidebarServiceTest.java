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
import static de.tudarmstadt.ukp.inception.curation.model.CurationSessionPreferences.KEY_CURATION_SESSION;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationSessionServiceImpl;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesServiceImpl;
import de.tudarmstadt.ukp.inception.preferences.model.UserProjectPreference;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

@DataJpaTest(showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
@EnableAutoConfiguration
@EntityScan({ //
        "de.tudarmstadt.ukp.inception.curation", //
        "de.tudarmstadt.ukp.inception.preferences.model", //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
public class CurationSidebarServiceTest
{
    private static final String KEVIN = "kevin";
    private static final String BEATE = "beate";
    private static final String OTHER = "other";
    private static final String CURRENT = "current";

    private CurationSessionService sut;

    private @Autowired TestEntityManager testEntityManager;

    // Only stubbed to hand out the persisted users and project
    private @MockitoBean ProjectService projectService;
    private @MockitoBean UserDao userRegistry;
    private @MockitoBean DocumentService documentService;
    private @MockitoBean CurationDocumentService curationDocumentService;

    private Project testProject;
    private Project otherProject;
    private SourceDocument testDocument;
    private User currentUser;
    private User beateUser;
    private User kevinUser;
    private AnnotationSet beate;
    private AnnotationSet kevin;

    @BeforeEach
    public void setUp() throws Exception
    {
        // create users
        currentUser = new User(CURRENT, ROLE_USER);
        var otherUser = new User(OTHER, ROLE_USER);
        beateUser = new User(BEATE, ROLE_USER);
        kevinUser = new User(KEVIN, ROLE_USER);
        testEntityManager.persist(currentUser);
        testEntityManager.persist(otherUser);
        testEntityManager.persist(beateUser);
        testEntityManager.persist(kevinUser);
        beate = AnnotationSet.forUser(beateUser);
        kevin = AnnotationSet.forUser(kevinUser);

        // create project
        testProject = new Project("test-project");
        testEntityManager.persist(testProject);
        testEntityManager.persist(new ProjectPermission(testProject, BEATE, ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, KEVIN, ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, BEATE, CURATOR));

        otherProject = new Project("other-project");
        testEntityManager.persist(otherProject);

        // create source document
        testDocument = new SourceDocument("testDoc", testProject, "text");
        testEntityManager.persist(testDocument);

        when(userRegistry.get(CURRENT)).thenReturn(currentUser);
        when(userRegistry.get(OTHER)).thenReturn(otherUser);
        when(projectService.getProject(testProject.getId())).thenReturn(testProject);
        when(projectService.getProject(otherProject.getId())).thenReturn(otherProject);

        sut = newService();
    }

    @AfterEach
    public void tearDown()
    {
        testEntityManager.clear();
    }

    private CurationSessionService newService()
    {
        return new CurationSessionServiceImpl(
                new PreferencesServiceImpl(testEntityManager.getEntityManager()), projectService,
                userRegistry, curationDocumentService, documentService);
    }

    private void restart()
    {
        testEntityManager.flush();
        testEntityManager.clear();
        sut = newService();
    }

    @Test
    public void thatEverybodyIsSelectedByDefault()
    {
        assertThat(sut.getSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate, kevin)))
                .containsExactly(beate, kevin);
        assertThat(sut.isShowAll(CURRENT, testProject.getId())).isFalse();
        assertThat(sut.isShowScore(CURRENT, testProject.getId())).isTrue();
    }

    @Test
    public void thatSettingsAreStoredWhenTheyChange()
    {
        sut.setSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate, kevin),
                List.of(beate));
        sut.setShowAll(CURRENT, testProject.getId(), true);
        sut.setShowScore(CURRENT, testProject.getId(), false);

        restart();

        assertThat(sut.getSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate, kevin)))
                .containsExactly(beate);
        assertThat(sut.isShowAll(CURRENT, testProject.getId())).isTrue();
        assertThat(sut.isShowScore(CURRENT, testProject.getId())).isFalse();
    }

    @Test
    public void thatDeselectedDataOwnersAreNotReadyForCuration()
    {
        when(curationDocumentService.listCuratableUsers(testDocument))
                .thenReturn(asList(beateUser, kevinUser));
        sut.setSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate, kevin),
                List.of(beate));

        restart();

        assertThat(sut.listDataOwnersReadyForCuration(CURRENT, testProject, testDocument))
                .containsExactly(beate);
    }

    @Test
    public void thatADeselectedDataOwnerCanBeSelectedAgain()
    {
        sut.setSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate, kevin),
                List.of(beate));

        restart();

        sut.setSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate, kevin),
                List.of(beate, kevin));

        restart();

        assertThat(sut.getSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate, kevin)))
                .containsExactly(beate, kevin);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void thatSettingsStoredBeforeTheDisplayOptionsExistedAreStillRead()
    {
        // Before the display options were stored, only the curation target and the deselected
        // data owners were written to the preferences
        var stored = new UserProjectPreference();
        stored.setUser(currentUser);
        stored.setProject(testProject);
        stored.setName(KEY_CURATION_SESSION.getName());
        stored.setTraits("{\"curationTarget\":\"target\",\"deselectedDataOwners\":[\"kevin\"]}");
        testEntityManager.persist(stored);

        restart();

        assertThat(sut.getSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate, kevin)))
                .containsExactly(beate);
        assertThat(sut.isShowAll(CURRENT, testProject.getId())).isFalse();
        assertThat(sut.isShowScore(CURRENT, testProject.getId())).isTrue();

        // Changing a setting keeps the parts of the stored value that are not changed
        sut.setShowAll(CURRENT, testProject.getId(), true);

        restart();

        assertThat(sut.getSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate, kevin)))
                .containsExactly(beate);
        assertThat(new PreferencesServiceImpl(testEntityManager.getEntityManager())
                .loadTraitsForUserAndProject(KEY_CURATION_SESSION, currentUser, testProject)
                .getCurationTarget()).isEqualTo("target");
    }

    @Test
    public void thatDataOwnersOutsideTheCandidatesKeepTheirState()
    {
        sut.setSelectedDataOwners(CURRENT, testProject.getId(), List.of(kevin), emptyList());
        sut.setSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate), List.of(beate));

        restart();

        assertThat(sut.getSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate, kevin)))
                .containsExactly(beate);
    }

    @Test
    public void thatSettingsOfOneSessionOwnerDoNotAffectAnother()
    {
        sut.setSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate, kevin),
                List.of(beate));
        sut.setShowAll(CURRENT, testProject.getId(), true);
        sut.setShowScore(CURRENT, testProject.getId(), false);

        restart();

        assertThat(sut.getSelectedDataOwners(OTHER, testProject.getId(), List.of(beate, kevin)))
                .containsExactly(beate, kevin);
        assertThat(sut.isShowAll(OTHER, testProject.getId())).isFalse();
        assertThat(sut.isShowScore(OTHER, testProject.getId())).isTrue();
    }

    @Test
    public void thatSettingsInOneProjectDoNotAffectAnother()
    {
        sut.setSelectedDataOwners(CURRENT, testProject.getId(), List.of(beate, kevin),
                List.of(beate));
        sut.setShowAll(CURRENT, testProject.getId(), true);
        sut.setShowScore(CURRENT, testProject.getId(), false);

        restart();

        assertThat(sut.getSelectedDataOwners(CURRENT, otherProject.getId(), List.of(beate, kevin)))
                .containsExactly(beate, kevin);
        assertThat(sut.isShowAll(CURRENT, otherProject.getId())).isFalse();
        assertThat(sut.isShowScore(CURRENT, otherProject.getId())).isTrue();
    }

    @SpringBootConfiguration
    public static class SpringConfig
    {
        // No content
    }
}
