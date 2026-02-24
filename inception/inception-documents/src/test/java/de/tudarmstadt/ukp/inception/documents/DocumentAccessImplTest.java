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
package de.tudarmstadt.ukp.inception.documents;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentAccessImplTest
{
    private @Mock ProjectService projectService;
    private @Mock UserDao userService;
    private @Mock DocumentService documentService;
    private @Mock Project project;
    private @Mock SourceDocument sourceDocument;

    private @InjectMocks DocumentAccessImpl sut;

    private User user;

    @BeforeEach
    void setUp()
    {
        user = User.builder() //
                .withUsername("alice") //
                .withUiName("alice") //
                .withEnabled(true) //
                .build();
    }

    @Test
    void canView_manager_granted()
    {
        when(userService.get("alice")).thenReturn(user);
        when(projectService.getProject(1L)).thenReturn(project);
        when(projectService.listRoles(project, user)).thenReturn(List.of(MANAGER));

        assertThat(sut.canViewAnnotationDocument("alice", "1", 42L, "someoneElse")).isTrue();
    }

    @Test
    void canView_annotator_own_notBlocked()
    {
        when(userService.get("alice")).thenReturn(user);
        when(projectService.getProjectBySlug("proj")).thenReturn(project);
        when(projectService.listRoles(project, user)).thenReturn(List.of(ANNOTATOR));
        when(documentService.getSourceDocument(project.getId(), 7L)).thenReturn(sourceDocument);
        when(documentService.existsAnnotationDocument(any(SourceDocument.class),
                any(AnnotationSet.class))).thenReturn(false);

        assertThat(sut.canViewAnnotationDocument("alice", "proj", 7L, "alice")).isTrue();
    }

    @Test
    void canView_annotator_blocked_denied()
    {
        when(userService.get("alice")).thenReturn(user);
        when(projectService.getProject(2L)).thenReturn(project);
        when(projectService.listRoles(project, user)).thenReturn(List.of(ANNOTATOR));
        when(documentService.getSourceDocument(project.getId(), 8L)).thenReturn(sourceDocument);
        lenient().when(documentService.existsAnnotationDocument(any(SourceDocument.class),
                any(AnnotationSet.class))).thenReturn(true);
        var aDoc = AnnotationDocument.builder() //
                .withState(IGNORE) //
                .build();
        when(documentService.getAnnotationDocument(any(SourceDocument.class),
                any(AnnotationSet.class))).thenReturn(aDoc);

        assertThat(sut.canViewAnnotationDocument("alice", "2", 8L, "alice")).isFalse();
    }

    @Test
    void assertCanEdit_curator_curationFinished_throws()
    {
        when(projectService.listRoles(project, user)).thenReturn(List.of(CURATOR));
        when(sourceDocument.getProject()).thenReturn(project);
        when(sourceDocument.getState()).thenReturn(CURATION_FINISHED);
        assertThatThrownBy(
                () -> sut.assertCanEditAnnotationDocument(user, sourceDocument, CURATION_USER))
                        .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void canEdit_annotator_curationInProgress_denied()
    {
        when(projectService.listRoles(project, user)).thenReturn(List.of(ANNOTATOR));
        when(sourceDocument.getProject()).thenReturn(project);
        when(sourceDocument.getState()).thenReturn(CURATION_IN_PROGRESS);
        when(documentService.existsAnnotationDocument(any(SourceDocument.class),
                any(AnnotationSet.class))).thenReturn(false);

        assertThatThrownBy(
                () -> sut.assertCanEditAnnotationDocument(user, sourceDocument, user.getUsername()))
                        .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void canEdit_annotator_curationFinished_denied()
    {
        when(projectService.listRoles(project, user)).thenReturn(List.of(ANNOTATOR));
        when(sourceDocument.getProject()).thenReturn(project);
        when(sourceDocument.getState()).thenReturn(CURATION_FINISHED);
        when(documentService.existsAnnotationDocument(any(SourceDocument.class),
                any(AnnotationSet.class))).thenReturn(false);

        assertThatThrownBy(
                () -> sut.assertCanEditAnnotationDocument(user, sourceDocument, user.getUsername()))
                        .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void canEdit_annotator_reopened_duringCurationInProgress_allowed()
    {
        when(projectService.listRoles(project, user)).thenReturn(List.of(ANNOTATOR));
        when(sourceDocument.getProject()).thenReturn(project);
        when(sourceDocument.getState()).thenReturn(CURATION_IN_PROGRESS);
        when(documentService.existsAnnotationDocument(any(SourceDocument.class),
                any(AnnotationSet.class))).thenReturn(true);

        var annDoc = AnnotationDocument.builder() //
                .withState(IN_PROGRESS) // reopened by manager
                .build();

        lenient().when(documentService.getAnnotationDocument(any(SourceDocument.class),
                any(AnnotationSet.class))).thenReturn(annDoc);

        // should not throw
        sut.assertCanEditAnnotationDocument(user, sourceDocument, user.getUsername());
    }

    @Test
    void canEdit_annotator_reopened_duringCurationFinished_allowed()
    {
        when(projectService.listRoles(project, user)).thenReturn(List.of(ANNOTATOR));
        when(sourceDocument.getProject()).thenReturn(project);
        when(sourceDocument.getState()).thenReturn(CURATION_FINISHED);
        lenient().when(documentService.existsAnnotationDocument(any(SourceDocument.class),
                any(AnnotationSet.class))).thenReturn(true);

        var annDoc = AnnotationDocument.builder() //
                .withState(IN_PROGRESS) // reopened by manager
                .build();

        lenient().when(documentService.getAnnotationDocument(any(SourceDocument.class),
                any(AnnotationSet.class))).thenReturn(annDoc);

        // should not throw
        sut.assertCanEditAnnotationDocument(user, sourceDocument, user.getUsername());
    }

    @Test
    void canExport_projectNull_false()
    {
        assertThat(sut.canExportAnnotationDocument(user, null)).isFalse();
    }

    @Test
    void canExport_manager_true()
    {
        when(projectService.hasRole(user, project, MANAGER)).thenReturn(true);
        assertThat(sut.canExportAnnotationDocument(user, project)).isTrue();
    }

    @Test
    void canExport_disabled_false()
    {
        when(projectService.hasRole(user, project, MANAGER)).thenReturn(false);
        when(project.isDisableExport()).thenReturn(true);
        assertThat(sut.canExportAnnotationDocument(user, project)).isFalse();
    }
}
