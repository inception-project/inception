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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar.render;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.CurationEditorExtension;

/**
 * The curation extension can be requested by the client, e.g. through the DIAM websocket, so
 * whether the curation overlay is drawn must depend on the viewer's role.
 */
@ExtendWith(MockitoExtension.class)
class CurationSidebarRendererAcceptsTest
{
    private @Mock ProjectService projectService;

    private CurationSidebarRenderer sut;

    private Project project;
    private SourceDocument document;
    private User curator;
    private User annotator;
    private CAS cas;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new CurationSidebarRenderer(null, null, null, null, null, projectService);

        project = Project.builder().withId(1l).withName("project").build();
        document = SourceDocument.builder().withId(1l).withProject(project).withName("doc").build();
        curator = User.builder().withUsername("curator").build();
        annotator = User.builder().withUsername("annotator").build();
        cas = CasFactory.createText("This is a test.");
    }

    @Test
    void thatACuratorGetsTheCurationOverlay()
    {
        when(projectService.hasRole(curator, project, CURATOR)).thenReturn(true);

        assertThat(sut.accepts(curationRequest(curator, curator))).isTrue();
    }

    @Test
    void thatANonCuratorRequestingTheCurationExtensionDoesNotGetTheCurationOverlay()
    {
        when(projectService.hasRole(annotator, project, CURATOR)).thenReturn(false);

        assertThat(sut.accepts(curationRequest(annotator, annotator))).isFalse();
    }

    @Test
    void thatTheRoleOfTheViewerDecidesAndNotThatOfTheAnnotationUser()
    {
        // E.g. an update pushed to an annotator's editor after a curator changed the document.
        // Lenient because a correct implementation never asks for the curator's role.
        lenient().when(projectService.hasRole(curator, project, CURATOR)).thenReturn(true);
        when(projectService.hasRole(annotator, project, CURATOR)).thenReturn(false);

        assertThat(sut.accepts(curationRequest(annotator, curator))).isFalse();
    }

    @Test
    void thatARequestWithoutAViewerDoesNotGetTheCurationOverlay()
    {
        // E.g. a background render of the curation document. Every user counts as a curator here,
        // so the request can only be refused because it has no viewer. Lenient because a correct
        // implementation does not even need to ask for a role.
        lenient().when(projectService.hasRole(nullable(User.class), eq(project), eq(CURATOR)))
                .thenReturn(true);

        assertThat(sut.accepts(curationRequest(null, curator))).isFalse();
    }

    private RenderRequest curationRequest(User aViewer, User aAnnotationUser)
    {
        return RenderRequest.builder() //
                .withSessionOwner(aViewer) //
                .withDocument(document, aAnnotationUser) //
                .withCas(cas) //
                .withWindow(0, cas.getDocumentText().length()) //
                .withEnabledExtensions(asList(CurationEditorExtension.EXTENSION_ID)) //
                .build();
    }
}
