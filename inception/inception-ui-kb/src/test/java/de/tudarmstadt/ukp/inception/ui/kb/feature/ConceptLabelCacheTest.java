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
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.ui.kb.feature.ConceptLabelCache.Key;

@ExtendWith(MockitoExtension.class)
public class ConceptLabelCacheTest
{
    private @Mock KnowledgeBaseService kbService;
    private ConceptLabelCache sut;

    private Project project;
    private AnnotationFeature feature;

    @BeforeEach
    public void setUp()
    {
        sut = new ConceptLabelCache(kbService, new KnowledgeBasePropertiesImpl());

        project = Project.builder() //
                .withId(1L) //
                .withName("test-project") //
                .build();
        feature = AnnotationFeature.builder() //
                .withProject(project) //
                .build();
    }

    @Test
    public void getAll_resolvesViaBulkReadHandles_andReturnsHandlePerKey() throws Exception
    {
        var k1 = Key.of(feature, null, "id:1");
        var k2 = Key.of(feature, null, "id:2");

        when(kbService.readHandles(eq(project), any())).thenReturn(Map.of( //
                "id:1", new KBHandle("id:1", "Alpha"), //
                "id:2", new KBHandle("id:2", "Beta")));

        var result = sut.getAll(asList(k1, k2));

        assertThat(result.get(k1)).extracting(KBHandle::getName).isEqualTo("Alpha");
        assertThat(result.get(k2)).extracting(KBHandle::getName).isEqualTo("Beta");
        verify(kbService, times(1)).readHandles(eq(project), any());
        verify(kbService, never()).readHandle(any(Project.class), anyString());
    }

    @Test
    public void getAll_secondCallWithSameKeys_doesNotHitDB() throws Exception
    {
        var k1 = Key.of(feature, null, "id:1");

        when(kbService.readHandles(eq(project), any())) //
                .thenReturn(Map.of("id:1", new KBHandle("id:1", "Alpha")));

        sut.getAll(asList(k1));
        sut.getAll(asList(k1));

        // Second getAll should hit the per-key cache, not re-query.
        verify(kbService, times(1)).readHandles(eq(project), any());
    }

    @Test
    public void getAll_followedByGet_servesFromCache() throws Exception
    {
        var k1 = Key.of(feature, null, "id:1");

        when(kbService.readHandles(eq(project), any())) //
                .thenReturn(Map.of("id:1", new KBHandle("id:1", "Alpha")));

        sut.getAll(asList(k1));
        var single = sut.get(feature, null, "id:1");

        assertThat(single.getName()).isEqualTo("Alpha");
        verify(kbService, times(1)).readHandles(eq(project), any());
        verify(kbService, never()).readHandle(any(Project.class), anyString());
    }

    @Test
    public void getAll_partialMisses_onlyBulkLoadsTheMissing() throws Exception
    {
        var k1 = Key.of(feature, null, "id:1");
        var k2 = Key.of(feature, null, "id:2");
        var k3 = Key.of(feature, null, "id:3");

        when(kbService.readHandles(eq(project), any())) //
                .thenReturn(Map.of( //
                        "id:1", new KBHandle("id:1", "Alpha"), //
                        "id:2", new KBHandle("id:2", "Beta")));
        sut.getAll(asList(k1, k2));

        when(kbService.readHandles(eq(project), any())) //
                .thenReturn(Map.of("id:3", new KBHandle("id:3", "Gamma")));
        sut.getAll(asList(k1, k2, k3));

        // Two distinct bulk loads — first for {1,2}, second for {3} only.
        verify(kbService, times(2)).readHandles(eq(project), any());
    }

    @Test
    public void getAll_missingHandle_returnsStubWithIdentifier() throws Exception
    {
        var k1 = Key.of(feature, null, "id:missing");

        // Mirror readHandles' "missing ids get a stub with no name" semantic.
        when(kbService.readHandles(eq(project), any())) //
                .thenReturn(Map.of("id:missing", new KBHandle("id:missing")));

        var result = sut.getAll(asList(k1));

        assertThat(result.get(k1)).isNotNull();
        assertThat(result.get(k1).getIdentifier()).isEqualTo("id:missing");
    }

    @Test
    public void getAll_labellessHandleWithDescription_preservesDescription() throws Exception
    {
        var k1 = Key.of(feature, null, "id:1");

        var labelless = KBHandle.builder() //
                .withIdentifier("id:1") //
                .withDescription("A described but unlabeled entity") //
                .build();
        when(kbService.readHandles(eq(project), any())) //
                .thenReturn(Map.of("id:1", labelless));

        var result = sut.getAll(asList(k1));

        assertThat(result.get(k1).getName()).isNull();
        assertThat(result.get(k1).getDescription()).isEqualTo("A described but unlabeled entity");
    }

    @Test
    public void getAll_groupsByRepositoryId_oneBatchPerGroup() throws Exception
    {
        var repoA = "kb-a";
        var repoB = "kb-b";
        var k1 = Key.of(feature, repoA, "id:1");
        var k2 = Key.of(feature, repoA, "id:2");
        var k3 = Key.of(feature, repoB, "id:3");

        when(kbService.getKnowledgeBaseById(eq(project), anyString())) //
                .thenReturn(java.util.Optional.empty());

        sut.getAll(asList(k1, k2, k3));

        // Two groups -> two getKnowledgeBaseById lookups (one per (project, repoId)).
        verify(kbService, times(1)).getKnowledgeBaseById(eq(project), eq(repoA));
        verify(kbService, times(1)).getKnowledgeBaseById(eq(project), eq(repoB));
        verify(kbService, never()).readHandles(eq(project), any());
    }
}
