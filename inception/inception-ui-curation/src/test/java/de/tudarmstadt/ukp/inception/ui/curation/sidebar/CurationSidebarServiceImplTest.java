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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

@ExtendWith(MockitoExtension.class)
class CurationSidebarServiceImplTest
{
    private @Mock DocumentService documentService;
    private @Mock UserDao userRegistry;
    private @Mock CasStorageService casStorageService;
    private @Mock CurationService curationService;
    private @Mock CurationMergeService curationMergeService;
    private @Mock CurationSessionService curationSessionService;
    private @Mock CurationDocumentService curationDocumentService;
    private @Mock AnnotatorState state;

    private CurationSidebarServiceImpl sut;

    @BeforeEach
    void setUp()
    {
        sut = new CurationSidebarServiceImpl(documentService, userRegistry, casStorageService,
                curationService, curationMergeService, curationSessionService,
                curationDocumentService);
    }

    @Test
    void thatMergeIntoFinishedCurationIsRejected()
    {
        var document = new SourceDocument("doc", new Project("project"), "text");
        when(state.getDocument()).thenReturn(document);
        when(curationDocumentService.isCurationFinished(document)).thenReturn(true);

        assertThatExceptionOfType(NotEditableException.class) //
                .isThrownBy(() -> sut.merge(state, new CurationWorkflow(), emptyList(), true));

        verifyNoInteractions(casStorageService, curationMergeService);
    }
}
