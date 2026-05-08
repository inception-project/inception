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
package de.tudarmstadt.ukp.inception.assistant.documents;

import static de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryServiceImpl.makeDocument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantDocumentIndexPropertiesImpl;
import de.tudarmstadt.ukp.inception.assistant.embedding.EmbeddingService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentQueryServiceImplIntegrationTest
{
    private File repoDir;
    private DocumentQueryServiceImpl sut;
    private Project project;

    private @TempDir Path tempDir;
    private @Mock SchedulingService scheduling;
    private @Mock EmbeddingService embedding;
    private @Mock DocumentService docService;

    @BeforeEach
    public void setUp() throws Exception
    {
        repoDir = tempDir.toFile();
        var repoProps = new RepositoryPropertiesImpl();
        repoProps.setPath(repoDir);

        var indexProps = new AssistantDocumentIndexPropertiesImpl();
        when(docService.getSourceDocument(anyLong(), anyLong())).thenAnswer(invocation -> {
            var documentId = invocation.getArgument(1, Long.class);
            var sd = new SourceDocument();
            sd.setId(documentId);
            sd.setName("doc-" + documentId);
            return sd;
        });

        // Configure embedding mock: specific for "d2", default to vector for "d1"/others
        when(embedding.getDimension()).thenReturn(3);
        when(embedding.embed(eq("d2"))).thenReturn(Optional.of(new float[] { 0f, 1f, 0f }));
        when(embedding.embed(anyString())).thenReturn(Optional.of(new float[] { 1f, 0f, 0f }));

        sut = new DocumentQueryServiceImpl(repoProps, indexProps, scheduling, embedding,
                docService);

        project = new Project();
        project.setId(42L);

        try (var idx = sut.borrowIndex(project)) {
            var iw = idx.getIndexWriter();

            var doc1 = makeDocument(1L, 0, 11, new float[] { 1f, 0f, 0f }, "apple orange");
            iw.addDocument(doc1);

            var doc2 = makeDocument(2L, 0, 12, new float[] { 0f, 1f, 0f }, "apple banana");
            iw.addDocument(doc2);

            iw.commit();
        }
    }

    @Test
    public void keywordQueryReturnsMatchingDocument()
    {
        var kw = sut.keywordQuery(project, "orange", 10);
        assertThat(kw.matches()).hasSize(1);
        assertThat(kw.matches().get(0).documentId()).isEqualTo(1L);
    }

    @Test
    public void semanticQueryReturnsTopSemanticMatch()
    {
        var sem = sut.semanticQuery(project, "d1", 10, 0.75);
        assertThat(sem.matches()).hasSize(1);
        assertThat(sem.matches().get(0).documentId()).isEqualTo(1L);
    }

    @Test
    public void hybridQueryReRanksCandidates()
    {
        var hy = sut.hybridQuery(project, "apple d1", 10);
        assertThat(hy.matches()).isNotEmpty();
        assertThat(hy.matches().get(0).documentId()).isEqualTo(1L);
    }
}
