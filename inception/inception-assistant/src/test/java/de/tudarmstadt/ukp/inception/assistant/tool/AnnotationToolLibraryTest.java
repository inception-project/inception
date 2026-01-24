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
package de.tudarmstadt.ukp.inception.assistant.tool;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolStatusResponse.Status.ERROR;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolStatusResponse.Status.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.assistant.recommender.AssistantRecommenderFactory;
import de.tudarmstadt.ukp.inception.assistant.tool.SpanSpec;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationEditorContext;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
class AnnotationToolLibraryTest
{
    private @Mock RecommendationService recommendationService;
    private @Mock AnnotationSchemaService schemaService;
    private @Mock UserDao userService;

    private AnnotationToolLibrary sut;

    private Project project;
    private SourceDocument document;
    private AnnotationLayer layer;
    private AnnotationFeature feature;
    private User user;
    private Recommender recommender;

    @BeforeEach
    void setup()
    {
        sut = new AnnotationToolLibrary(recommendationService, schemaService, userService);

        project = Project.builder() //
                .withId(1L) //
                .withName("Test Project") //
                .build();

        document = SourceDocument.builder() //
                .withId(1L) //
                .withProject(project) //
                .withName("test.txt") //
                .build();

        layer = AnnotationLayer.builder() //
                .withId(1L) //
                .withName("NamedEntity") //
                .withType("span") //
                .build();

        feature = AnnotationFeature.builder() //
                .withId(1L) //
                .withName("value") //
                .withType("string") //
                .withLayer(layer) //
                .build();

        user = User.builder() //
                .withUsername("testuser") //
                .build();

        recommender = Recommender.builder() //
                .withId(1L) //
                .withProject(project) //
                .withLayer(layer) //
                .withFeature(feature) //
                .withName("AI Assistant") //
                .withTool(AssistantRecommenderFactory.ID) //
                .withEnabled(true) //
                .build();
    }

    @Test
    void testCreateSpanSuggestions_withNewRecommender() throws Exception
    {
        // Given
        var context = mock(AnnotationEditorContext.class);
        when(context.getProject()).thenReturn(project);
        when(context.getDocument()).thenReturn(document);

        when(userService.getCurrentUser()).thenReturn(user);
        when(schemaService.findLayer(project, "NamedEntity")).thenReturn(layer);
        when(schemaService.listSupportedFeatures(layer)).thenReturn(List.of(feature));
        when(recommendationService.listRecommenders(project)).thenReturn(Collections.emptyList());
        // Simulate createOrUpdateRecommender setting the ID
        doAnswer(inv -> {
            Recommender r = inv.getArgument(0);
            r.setId(99L);
            return null;
        }).when(recommendationService).createOrUpdateRecommender(any(Recommender.class));

        var suggestions = Arrays.asList( //
                createSpanSpec(0, 5, "PER"), //
                createSpanSpec(10, 20, "LOC"));

        // When
        var result = sut.createSpanSuggestions(context, null, "NamedEntity", suggestions);

        // Then
        assertThat(result.status()).isEqualTo(SUCCESS);
        assertThat(result.message()).contains("Created 2 suggestion(s)");

        // Verify recommender was created
        var recommenderCaptor = ArgumentCaptor.forClass(Recommender.class);
        verify(recommendationService).createOrUpdateRecommender(recommenderCaptor.capture());
        var createdRecommender = recommenderCaptor.getValue();
        assertThat(createdRecommender.getName()).isEqualTo("AI Assistant");
        assertThat(createdRecommender.getTool()).isEqualTo(AssistantRecommenderFactory.ID);
        assertThat(createdRecommender.getLayer()).isEqualTo(layer);

        // Verify predictions were queued
        verify(recommendationService).putIncomingPredictions(eq(user), eq(project), any(),
                any(Predictions.class));
    }

    @Test
    void testCreateSpanSuggestions_withExistingRecommender() throws Exception
    {
        // Given
        var context = mock(AnnotationEditorContext.class);
        when(context.getProject()).thenReturn(project);
        when(context.getDocument()).thenReturn(document);

        when(userService.getCurrentUser()).thenReturn(user);
        when(schemaService.findLayer(project, "NamedEntity")).thenReturn(layer);
        when(recommendationService.listRecommenders(project)).thenReturn(List.of(recommender));

        var suggestions = List.of(createSpanSpec(0, 10, "PER"));

        // When
        var result = sut.createSpanSuggestions(context, null, "NamedEntity", suggestions);

        // Then
        assertThat(result.status()).isEqualTo(SUCCESS);

        // Verify recommender was NOT created (reused existing)
        verify(recommendationService, times(0)).createOrUpdateRecommender(any());

        // Verify predictions were queued
        verify(recommendationService).putIncomingPredictions(eq(user), eq(project), any(),
                any(Predictions.class));
    }

    @Test
    void testCreateSpanSuggestions_reconfiguresRecommenderForDifferentLayer() throws Exception
    {
        // Given
        var differentLayer = AnnotationLayer.builder() //
                .withId(2L) //
                .withName("Event") //
                .withType("span") //
                .build();

        var context = mock(AnnotationEditorContext.class);
        when(context.getProject()).thenReturn(project);
        when(context.getDocument()).thenReturn(document);

        when(userService.getCurrentUser()).thenReturn(user);
        when(schemaService.findLayer(project, "Event")).thenReturn(differentLayer);
        when(schemaService.listSupportedFeatures(differentLayer)).thenReturn(List.of(feature));
        when(recommendationService.listRecommenders(project)).thenReturn(List.of(recommender));
        doNothing().when(recommendationService).createOrUpdateRecommender(any());

        var suggestions = List.of(createSpanSpec(0, 10, "MEETING"));

        // When
        var result = sut.createSpanSuggestions(context, null, "Event", suggestions);

        // Then
        assertThat(result.status()).isEqualTo(SUCCESS);

        // Verify recommender was updated with new layer
        verify(recommendationService).createOrUpdateRecommender(recommender);
        assertThat(recommender.getLayer()).isEqualTo(differentLayer);
    }

    @Test
    void testCreateSpanSuggestions_withPredecessorPredictions() throws Exception
    {
        // Given
        var context = mock(AnnotationEditorContext.class);
        when(context.getProject()).thenReturn(project);
        when(context.getDocument()).thenReturn(document);

        when(userService.getCurrentUser()).thenReturn(user);
        when(schemaService.findLayer(project, "NamedEntity")).thenReturn(layer);
        when(recommendationService.listRecommenders(project)).thenReturn(List.of(recommender));

        // Setup existing predictions
        var existingPredictions = new Predictions(user, user.getUsername(), project);
        when(recommendationService.getPredictions(eq(user), eq(project), any()))
                .thenReturn(existingPredictions);

        var suggestions = List.of(createSpanSpec(0, 10, "PER"));

        // When
        var result = sut.createSpanSuggestions(context, null, "NamedEntity", suggestions);

        // Then
        assertThat(result.status()).isEqualTo(SUCCESS);

        // Verify predictions were queued (inheriting from predecessor)
        verify(recommendationService).putIncomingPredictions(eq(user), eq(project), any(),
                any(Predictions.class));
    }

    @Test
    void testCreateSpanSuggestions_invalidLayer() throws Exception
    {
        // Given
        var context = mock(AnnotationEditorContext.class);
        when(context.getProject()).thenReturn(project);
        when(context.getDocument()).thenReturn(document);

        when(userService.getCurrentUser()).thenReturn(user);
        when(schemaService.findLayer(project, "InvalidLayer")).thenReturn(null);

        var suggestions = List.of(createSpanSpec(0, 10, "PER"));

        // When
        var result = sut.createSpanSuggestions(context, null, "InvalidLayer", suggestions);

        // Then
        assertThat(result.status()).isEqualTo(ERROR);
        assertThat(result.message()).contains("Layer 'InvalidLayer' not found");

        // Verify no predictions were queued
        verify(recommendationService, times(0)).putIncomingPredictions(any(), any(), any(), any());
    }

    @Test
    void testCreateSpanSuggestions_threadSafety() throws Exception
    {
        // Given
        var context = mock(AnnotationEditorContext.class);
        when(context.getProject()).thenReturn(project);
        when(context.getDocument()).thenReturn(document);

        when(userService.getCurrentUser()).thenReturn(user);
        when(schemaService.findLayer(project, "NamedEntity")).thenReturn(layer);
        when(schemaService.listSupportedFeatures(layer)).thenReturn(List.of(feature));
        when(recommendationService.listRecommenders(project)).thenReturn(Collections.emptyList());
        // Simulate createOrUpdateRecommender setting the ID
        doAnswer(inv -> {
            Recommender r = inv.getArgument(0);
            r.setId(99L);
            return null;
        }).when(recommendationService).createOrUpdateRecommender(any(Recommender.class));

        var suggestions = List.of(createSpanSpec(0, 10, "PER"));

        // When - simulate concurrent calls
        var threads = new Thread[10];
        var exceptions = new Exception[10];

        for (int i = 0; i < threads.length; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                try {
                    sut.createSpanSuggestions(context, null, "NamedEntity", suggestions);
                }
                catch (Exception e) {
                    exceptions[idx] = e;
                }
            });
        }

        for (var thread : threads) {
            thread.start();
        }

        for (var thread : threads) {
            thread.join();
        }

        // Then - no exceptions should have occurred
        for (var exception : exceptions) {
            assertThat(exception).isNull();
        }
    }

    @Test
    void testSpanSpecCreation()
    {
        // Given
        var spec = createSpanSpec(5, 15, "TEST");

        // Then
        assertThat(spec.begin).isEqualTo(5);
        assertThat(spec.end).isEqualTo(15);
        assertThat(spec.label).isEqualTo("TEST");
    }

    @Test
    void testSpanSpecDeserialization() throws Exception
    {
        // This test verifies that SpanSpec can be properly deserialized from LinkedHashMap,
        // which is how JSON tool parameters arrive from the LLM
        var context = mock(AnnotationEditorContext.class);
        when(context.getProject()).thenReturn(project);
        when(context.getDocument()).thenReturn(document);

        when(userService.getCurrentUser()).thenReturn(user);
        when(schemaService.findLayer(project, "NamedEntity")).thenReturn(layer);
        when(schemaService.listSupportedFeatures(layer)).thenReturn(List.of(feature));
        when(recommendationService.listRecommenders(project)).thenReturn(Collections.emptyList());
        doAnswer(inv -> {
            Recommender r = inv.getArgument(0);
            r.setId(99L);
            return null;
        }).when(recommendationService).createOrUpdateRecommender(any(Recommender.class));

        // Simulate what happens when JSON is deserialized - we get LinkedHashMaps
        // This mimics the actual MToolCall.invoke() behavior which receives Map objects
        java.util.List<java.util.LinkedHashMap<String, Object>> suggestionMaps = Arrays.asList(
                createLinkedHashMapFromSpanSpec(0, 5, "PER"),
                createLinkedHashMapFromSpanSpec(10, 20, "LOC")
        );

        // Convert using Jackson ObjectMapper (same as MToolCall does)
        var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var typeFactory = objectMapper.getTypeFactory();
        var expectedType = typeFactory.constructCollectionType(List.class, SpanSpec.class);
        List<SpanSpec> convertedSuggestions = objectMapper.convertValue(suggestionMaps, expectedType);

        // When - now this should work because we're converting properly
        var result = sut.createSpanSuggestions(context, null, "NamedEntity", convertedSuggestions);

        // Then
        assertThat(result.status()).isEqualTo(SUCCESS);
        assertThat(result.message()).contains("Created 2 suggestion(s)");
    }

    private java.util.LinkedHashMap<String, Object> createLinkedHashMapFromSpanSpec(int begin, int end, String label)
    {
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("begin", begin);
        map.put("end", end);
        map.put("label", label);
        return map;
    }

    private SpanSpec createSpanSpec(int begin, int end, String label)
    {
        var spec = new SpanSpec();
        spec.begin = begin;
        spec.end = end;
        spec.label = label;
        return spec;
    }
}
