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
package de.tudarmstadt.ukp.inception.schema.service;

import static de.tudarmstadt.ukp.inception.schema.api.AttachedAnnotation.Direction.INCOMING;
import static de.tudarmstadt.ukp.inception.schema.api.AttachedAnnotation.Direction.LOOP;
import static de.tudarmstadt.ukp.inception.schema.api.AttachedAnnotation.Direction.OUTGOING;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AttachedAnnotation;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;

class AnnotationSchemaServiceImplGetAttachedRelsTest
{
    private static final String CUSTOM_RELATION_TYPE = "webanno.custom.Relation";
    private static final String CUSTOM_SPAN_TYPE = "webanno.custom.Span";
    private static final String GENERIC_RELATION_TYPE = "webanno.custom.GenericRelation";
    private static final String SPAN_TYPE_A = "webanno.custom.SpanA";
    private static final String SPAN_TYPE_B = "webanno.custom.SpanB";
    private static final String SOURCE_FEATURE = "Governor";
    private static final String TARGET_FEATURE = "Dependent";

    private AnnotationSchemaServiceImpl sut;
    private Project project;
    private AnnotationLayer customSpanLayer;
    private AnnotationLayer customRelationLayer;
    private AnnotationLayer genericRelationLayer;
    private AnnotationLayer spanLayerA;
    private AnnotationLayer spanLayerB;
    private JCas jcas;

    @BeforeEach
    void setUp() throws Exception
    {
        project = Project.builder().withId(1l).withName("Test Project").build();

        // Create custom span layer
        customSpanLayer = AnnotationLayer.builder() //
                .withName(CUSTOM_SPAN_TYPE) //
                .withType(SpanLayerSupport.TYPE) //
                .withProject(project) //
                .build();

        // Create custom relation layer that attaches to the custom span layer
        customRelationLayer = AnnotationLayer.builder() //
                .withName(CUSTOM_RELATION_TYPE) //
                .withType(RelationLayerSupport.TYPE) //
                .withProject(project) //
                .withAttachType(customSpanLayer) //
                .build();

        // Create additional span layers for generic relation tests
        spanLayerA = AnnotationLayer.builder() //
                .withName(SPAN_TYPE_A) //
                .withType(SpanLayerSupport.TYPE) //
                .withProject(project) //
                .build();

        spanLayerB = AnnotationLayer.builder() //
                .withName(SPAN_TYPE_B) //
                .withType(SpanLayerSupport.TYPE) //
                .withProject(project) //
                .build();

        // Create generic relation layer (no attachType - can attach to any annotation)
        genericRelationLayer = AnnotationLayer.builder() //
                .withName(GENERIC_RELATION_TYPE) //
                .withType(RelationLayerSupport.TYPE) //
                .withProject(project) //
                .build();

        // Create type system with relation support
        var typeSystem = createRelationTestTypeSystem();
        jcas = JCasFactory.createJCas(typeSystem);

        // Set up proper registries
        var featureSupportRegistry = new FeatureSupportRegistryImpl(asList());
        featureSupportRegistry.init();

        var layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        var layerSupportRegistry = new LayerSupportRegistryImpl(asList(
                new SpanLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry, null),
                new RelationLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        null)));
        layerSupportRegistry.init();

        // Create service with spy to mock only database calls
        sut = Mockito.spy(new AnnotationSchemaServiceImpl(layerSupportRegistry,
                featureSupportRegistry, null, null, null));

        // Mock the database methods to return our relation layers
        doReturn(asList(customRelationLayer)).when(sut).listAttachedRelationLayers(customSpanLayer);
        doReturn(asList(genericRelationLayer)).when(sut).listAttachedRelationLayers(spanLayerA);
        doReturn(asList(genericRelationLayer)).when(sut).listAttachedRelationLayers(spanLayerB);
    }

    private TypeSystemDescription createRelationTestTypeSystem() throws Exception
    {
        var typeSystems = new ArrayList<TypeSystemDescription>();

        var tsd = new TypeSystemDescription_impl();

        // Custom span type
        tsd.addType(CUSTOM_SPAN_TYPE, "", CAS.TYPE_NAME_ANNOTATION);

        // Custom relation type (attaches to specific span type)
        var relationTD = tsd.addType(CUSTOM_RELATION_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        relationTD.addFeature(SOURCE_FEATURE, "", CUSTOM_SPAN_TYPE);
        relationTD.addFeature(TARGET_FEATURE, "", CUSTOM_SPAN_TYPE);

        // Additional span types for generic relation tests
        tsd.addType(SPAN_TYPE_A, "", CAS.TYPE_NAME_ANNOTATION);
        tsd.addType(SPAN_TYPE_B, "", CAS.TYPE_NAME_ANNOTATION);

        // Generic relation type (can attach to any annotation)
        var genericRelationTD = tsd.addType(GENERIC_RELATION_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        genericRelationTD.addFeature(SOURCE_FEATURE, "", CAS.TYPE_NAME_ANNOTATION);
        genericRelationTD.addFeature(TARGET_FEATURE, "", CAS.TYPE_NAME_ANNOTATION);

        typeSystems.add(tsd);
        typeSystems.add(createTypeSystemDescription());

        return mergeTypeSystems(typeSystems);
    }

    @Test
    void testGetAttachedRels_whenNoRelations_returnsEmpty() throws Exception
    {
        // Given a custom span annotation with no relations
        jcas.setDocumentText("This is a test.");
        var span = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("test") //
                .buildAndAddToIndexes();

        // When checking for attached relations
        var result = sut.getAttachedRels(customSpanLayer, span);

        // Then no relations should be found
        assertThat(result).isEmpty();
    }

    @Test
    void testGetAttachedRels_withIncomingRelation_returnsIncoming() throws Exception
    {
        // Given two custom span annotations with a relation from span1 to span2
        jcas.setDocumentText("This is a test.");

        var span1 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("This") //
                .buildAndAddToIndexes();

        var span2 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("is") //
                .buildAndAddToIndexes();

        var relation = buildAnnotation(jcas, CUSTOM_RELATION_TYPE) //
                .at(0, 7) // spans both
                .withFeature(SOURCE_FEATURE, span1) //
                .withFeature(TARGET_FEATURE, span2) //
                .buildAndAddToIndexes();

        // When checking for relations attached to span2
        var result = sut.getAttachedRels(customSpanLayer, span2);

        // Then should find one incoming relation
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRelation()).isEqualTo(relation);
        assertThat(result.get(0).getEndpoint()).isEqualTo(span1);
        assertThat(result.get(0).getDirection()).isEqualTo(INCOMING);
    }

    @Test
    void testGetAttachedRels_withOutgoingRelation_returnsOutgoing() throws Exception
    {
        // Given two custom span annotations with a relation from span1 to span2
        jcas.setDocumentText("This is a test.");

        var span1 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("This") //
                .buildAndAddToIndexes();

        var span2 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("is") //
                .buildAndAddToIndexes();

        var relation = buildAnnotation(jcas, CUSTOM_RELATION_TYPE) //
                .at(0, 7) // spans both
                .withFeature(SOURCE_FEATURE, span1) //
                .withFeature(TARGET_FEATURE, span2) //
                .buildAndAddToIndexes();

        // When checking for relations attached to span1
        var result = sut.getAttachedRels(customSpanLayer, span1);

        // Then should find one outgoing relation
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRelation()).isEqualTo(relation);
        assertThat(result.get(0).getEndpoint()).isEqualTo(span2);
        assertThat(result.get(0).getDirection()).isEqualTo(OUTGOING);
    }

    @Test
    void testGetAttachedRels_withLoopRelation_returnsLoop() throws Exception
    {
        // Given a custom span with a relation to itself
        jcas.setDocumentText("This is a test.");

        var span = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("test") //
                .buildAndAddToIndexes();

        var relation = buildAnnotation(jcas, CUSTOM_RELATION_TYPE) //
                .at(10, 14) //
                .withFeature(SOURCE_FEATURE, span) //
                .withFeature(TARGET_FEATURE, span) //
                .buildAndAddToIndexes();

        // When checking for relations attached to the span
        var result = sut.getAttachedRels(customSpanLayer, span);

        // Then should find one loop relation
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRelation()).isEqualTo(relation);
        assertThat(result.get(0).getEndpoint()).isEqualTo(span);
        assertThat(result.get(0).getDirection()).isEqualTo(LOOP);
    }

    @Test
    void testGetAttachedRels_withMultipleIncomingRelations_returnsAll() throws Exception
    {
        // Given a span with multiple incoming relations
        jcas.setDocumentText("This is a test case.");

        var span1 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("This") //
                .buildAndAddToIndexes();

        var span2 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("is") //
                .buildAndAddToIndexes();

        var span3 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("test") //
                .buildAndAddToIndexes();

        var relation1 = buildAnnotation(jcas, CUSTOM_RELATION_TYPE) //
                .at(0, 7) //
                .withFeature(SOURCE_FEATURE, span1) //
                .withFeature(TARGET_FEATURE, span3) //
                .buildAndAddToIndexes();

        var relation2 = buildAnnotation(jcas, CUSTOM_RELATION_TYPE) //
                .at(5, 14) //
                .withFeature(SOURCE_FEATURE, span2) //
                .withFeature(TARGET_FEATURE, span3) //
                .buildAndAddToIndexes();

        // When checking for relations attached to span3
        var result = sut.getAttachedRels(customSpanLayer, span3);

        // Then should find two incoming relations
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AttachedAnnotation::getRelation) //
                .containsExactlyInAnyOrder(relation1, relation2);
        assertThat(result).extracting(AttachedAnnotation::getEndpoint) //
                .containsExactlyInAnyOrder(span1, span2);
        assertThat(result).allMatch(a -> a.getDirection() == INCOMING);
    }

    @Test
    void testGetAttachedRels_withMultipleOutgoingRelations_returnsAll() throws Exception
    {
        // Given a span with multiple outgoing relations
        jcas.setDocumentText("This is a test case.");

        var span1 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("This") //
                .buildAndAddToIndexes();

        var span2 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("is") //
                .buildAndAddToIndexes();

        var span3 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("test") //
                .buildAndAddToIndexes();

        var relation1 = buildAnnotation(jcas, CUSTOM_RELATION_TYPE) //
                .at(0, 7) //
                .withFeature(SOURCE_FEATURE, span1) //
                .withFeature(TARGET_FEATURE, span2) //
                .buildAndAddToIndexes();

        var relation2 = buildAnnotation(jcas, CUSTOM_RELATION_TYPE) //
                .at(0, 14) //
                .withFeature(SOURCE_FEATURE, span1) //
                .withFeature(TARGET_FEATURE, span3) //
                .buildAndAddToIndexes();

        // When checking for relations attached to span1
        var result = sut.getAttachedRels(customSpanLayer, span1);

        // Then should find two outgoing relations
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AttachedAnnotation::getRelation) //
                .containsExactlyInAnyOrder(relation1, relation2);
        assertThat(result).extracting(AttachedAnnotation::getEndpoint) //
                .containsExactlyInAnyOrder(span2, span3);
        assertThat(result).allMatch(a -> a.getDirection() == OUTGOING);
    }

    @Test
    void testGetAttachedRels_withMixedIncomingAndOutgoingRelations_returnsAll() throws Exception
    {
        // Given a span with both incoming and outgoing relations
        jcas.setDocumentText("This is a test case.");

        var span1 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("This") //
                .buildAndAddToIndexes();

        var span2 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("is") //
                .buildAndAddToIndexes();

        var span3 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("test") //
                .buildAndAddToIndexes();

        // Incoming: span1 -> span2
        var incomingRel = buildAnnotation(jcas, CUSTOM_RELATION_TYPE) //
                .at(0, 7) //
                .withFeature(SOURCE_FEATURE, span1) //
                .withFeature(TARGET_FEATURE, span2) //
                .buildAndAddToIndexes();

        // Outgoing: span2 -> span3
        var outgoingRel = buildAnnotation(jcas, CUSTOM_RELATION_TYPE) //
                .at(5, 14) //
                .withFeature(SOURCE_FEATURE, span2) //
                .withFeature(TARGET_FEATURE, span3) //
                .buildAndAddToIndexes();

        // When checking for relations attached to span2
        var result = sut.getAttachedRels(customSpanLayer, span2);

        // Then should find both relations
        assertThat(result).hasSize(2);

        var incoming = result.stream().filter(a -> a.getDirection() == INCOMING).findFirst()
                .orElseThrow();
        assertThat(incoming.getRelation()).isEqualTo(incomingRel);
        assertThat(incoming.getEndpoint()).isEqualTo(span1);

        var outgoing = result.stream().filter(a -> a.getDirection() == OUTGOING).findFirst()
                .orElseThrow();
        assertThat(outgoing.getRelation()).isEqualTo(outgoingRel);
        assertThat(outgoing.getEndpoint()).isEqualTo(span3);
    }

    @Test
    void testGetAttachedRels_withRelationToOtherAnnotation_returnsEmpty() throws Exception
    {
        // Given a span and a relation that doesn't involve it
        jcas.setDocumentText("This is a test case.");

        var span1 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("This") //
                .buildAndAddToIndexes();

        var span2 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("is") //
                .buildAndAddToIndexes();

        var span3 = buildAnnotation(jcas, CUSTOM_SPAN_TYPE) //
                .on("test") //
                .buildAndAddToIndexes();

        // Relation between span1 and span2 (not involving span3)
        buildAnnotation(jcas, CUSTOM_RELATION_TYPE) //
                .at(0, 7) //
                .withFeature(SOURCE_FEATURE, span1) //
                .withFeature(TARGET_FEATURE, span2) //
                .buildAndAddToIndexes();

        // When checking for relations attached to span3
        var result = sut.getAttachedRels(customSpanLayer, span3);

        // Then no relations should be found
        assertThat(result).isEmpty();
    }

    @Test
    void testGetAttachedRels_withGenericRelationType_incomingFromDifferentLayer() throws Exception
    {
        // Given two spans of different types with a generic relation from spanA to spanB
        jcas.setDocumentText("This is a test.");

        var spanA = buildAnnotation(jcas, SPAN_TYPE_A) //
                .on("This") //
                .buildAndAddToIndexes();

        var spanB = buildAnnotation(jcas, SPAN_TYPE_B) //
                .on("is") //
                .buildAndAddToIndexes();

        var relation = buildAnnotation(jcas, GENERIC_RELATION_TYPE) //
                .at(0, 7) //
                .withFeature(SOURCE_FEATURE, spanA) //
                .withFeature(TARGET_FEATURE, spanB) //
                .buildAndAddToIndexes();

        // When checking for relations attached to spanB
        var result = sut.getAttachedRels(spanLayerB, spanB);

        // Then should find one incoming relation from different layer
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRelation()).isEqualTo(relation);
        assertThat(result.get(0).getEndpoint()).isEqualTo(spanA);
        assertThat(result.get(0).getDirection()).isEqualTo(INCOMING);
    }

    @Test
    void testGetAttachedRels_withGenericRelationType_outgoingToDifferentLayer() throws Exception
    {
        // Given two spans of different types with a generic relation from spanA to spanB
        jcas.setDocumentText("This is a test.");

        var spanA = buildAnnotation(jcas, SPAN_TYPE_A) //
                .on("This") //
                .buildAndAddToIndexes();

        var spanB = buildAnnotation(jcas, SPAN_TYPE_B) //
                .on("is") //
                .buildAndAddToIndexes();

        var relation = buildAnnotation(jcas, GENERIC_RELATION_TYPE) //
                .at(0, 7) //
                .withFeature(SOURCE_FEATURE, spanA) //
                .withFeature(TARGET_FEATURE, spanB) //
                .buildAndAddToIndexes();

        // When checking for relations attached to spanA
        var result = sut.getAttachedRels(spanLayerA, spanA);

        // Then should find one outgoing relation to different layer
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRelation()).isEqualTo(relation);
        assertThat(result.get(0).getEndpoint()).isEqualTo(spanB);
        assertThat(result.get(0).getDirection()).isEqualTo(OUTGOING);
    }

    @Test
    void testGetAttachedRels_withGenericRelationType_sameLayerConnection() throws Exception
    {
        // Given two spans of same type with a generic relation
        jcas.setDocumentText("This is a test.");

        var spanA1 = buildAnnotation(jcas, SPAN_TYPE_A) //
                .on("This") //
                .buildAndAddToIndexes();

        var spanA2 = buildAnnotation(jcas, SPAN_TYPE_A) //
                .on("is") //
                .buildAndAddToIndexes();

        var relation = buildAnnotation(jcas, GENERIC_RELATION_TYPE) //
                .at(0, 7) //
                .withFeature(SOURCE_FEATURE, spanA1) //
                .withFeature(TARGET_FEATURE, spanA2) //
                .buildAndAddToIndexes();

        // When checking for relations attached to spanA1
        var result = sut.getAttachedRels(spanLayerA, spanA1);

        // Then should find one outgoing relation
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRelation()).isEqualTo(relation);
        assertThat(result.get(0).getEndpoint()).isEqualTo(spanA2);
        assertThat(result.get(0).getDirection()).isEqualTo(OUTGOING);
    }

    @Test
    void testGetAttachedRels_withGenericRelationType_multipleCrossLayerRelations() throws Exception
    {
        // Given a span with relations to/from multiple different layer types
        jcas.setDocumentText("This is a test case.");

        var spanA = buildAnnotation(jcas, SPAN_TYPE_A) //
                .on("is") //
                .buildAndAddToIndexes();

        var spanB1 = buildAnnotation(jcas, SPAN_TYPE_B) //
                .on("This") //
                .buildAndAddToIndexes();

        var spanB2 = buildAnnotation(jcas, SPAN_TYPE_B) //
                .on("test") //
                .buildAndAddToIndexes();

        // Incoming: spanB1 -> spanA
        var incomingRel = buildAnnotation(jcas, GENERIC_RELATION_TYPE) //
                .at(0, 7) //
                .withFeature(SOURCE_FEATURE, spanB1) //
                .withFeature(TARGET_FEATURE, spanA) //
                .buildAndAddToIndexes();

        // Outgoing: spanA -> spanB2
        var outgoingRel = buildAnnotation(jcas, GENERIC_RELATION_TYPE) //
                .at(5, 14) //
                .withFeature(SOURCE_FEATURE, spanA) //
                .withFeature(TARGET_FEATURE, spanB2) //
                .buildAndAddToIndexes();

        // When checking for relations attached to spanA
        var result = sut.getAttachedRels(spanLayerA, spanA);

        // Then should find both relations
        assertThat(result).hasSize(2);

        var incoming = result.stream().filter(a -> a.getDirection() == INCOMING).findFirst()
                .orElseThrow();
        assertThat(incoming.getRelation()).isEqualTo(incomingRel);
        assertThat(incoming.getEndpoint()).isEqualTo(spanB1);

        var outgoing = result.stream().filter(a -> a.getDirection() == OUTGOING).findFirst()
                .orElseThrow();
        assertThat(outgoing.getRelation()).isEqualTo(outgoingRel);
        assertThat(outgoing.getEndpoint()).isEqualTo(spanB2);
    }
}
