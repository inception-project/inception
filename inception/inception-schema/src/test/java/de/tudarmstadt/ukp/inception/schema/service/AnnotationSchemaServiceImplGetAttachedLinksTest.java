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
import static de.tudarmstadt.ukp.inception.schema.api.AttachedAnnotation.Direction.OUTGOING;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static de.tudarmstadt.ukp.inception.support.uima.FeatureStructureBuilder.buildFS;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AttachedAnnotation;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;

class AnnotationSchemaServiceImplGetAttachedLinksTest
{
    private static final String LINK_HOST_TYPE = "webanno.custom.LinkHost";
    private static final String LINK_TYPE = "webanno.custom.LinkType";
    private static final String LINKS_FEATURE = "links";
    private static final String ROLE_FEATURE = "role";
    private static final String TARGET_FEATURE = "target";

    private AnnotationSchemaServiceImpl sut;
    private Project project;
    private AnnotationLayer linkHostLayer;
    private AnnotationLayer namedEntityLayer;
    private AnnotationFeature linkFeature;
    private JCas jcas;

    @BeforeEach
    void setUp() throws Exception
    {
        project = Project.builder().withId(1l).withName("Test Project").build();

        // Create link host layer
        linkHostLayer = AnnotationLayer.builder() //
                .withName(LINK_HOST_TYPE) //
                .withType(SpanLayerSupport.TYPE) //
                .withProject(project) //
                .build();

        // Create named entity layer
        namedEntityLayer = AnnotationLayer.builder() //
                .forJCasClass(NamedEntity.class) //
                .withName("Named Entity") //
                .withType(SpanLayerSupport.TYPE) //
                .withProject(project) //
                .build();

        // Create link feature with ARRAY + WITH_ROLE configuration
        linkFeature = AnnotationFeature.builder() //
                .withLayer(linkHostLayer) //
                .withName(LINKS_FEATURE) //
                .withType(NamedEntity.class.getName()) // Type of the link target
                .withMultiValueMode(MultiValueMode.ARRAY) //
                .withLinkMode(LinkMode.WITH_ROLE) //
                .withLinkTypeName(LINK_TYPE) // Type of the link structure itself
                .withLinkTypeRoleFeatureName(ROLE_FEATURE) //
                .withLinkTypeTargetFeatureName(TARGET_FEATURE) //
                .build();

        // Create type system with link support
        var typeSystem = createMultiLinkWithRoleTestTypeSystem();
        jcas = JCasFactory.createJCas(typeSystem);

        // Set up proper registries
        var featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new LinkFeatureSupport(null)));
        featureSupportRegistry.init();

        var layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        var layerSupportRegistry = new LayerSupportRegistryImpl(asList(new SpanLayerSupportImpl(
                featureSupportRegistry, null, layerBehaviorRegistry, null)));
        layerSupportRegistry.init();

        // Create service with spy to mock only database calls
        sut = Mockito.spy(new AnnotationSchemaServiceImpl(layerSupportRegistry,
                featureSupportRegistry, null, null, null));

        // Mock the database methods to return our link feature
        doReturn(asList(linkFeature)).when(sut).listAttachedLinkFeatures(any());
        doReturn(asList(linkFeature)).when(sut).listSupportedFeatures(linkHostLayer);
        doReturn(asList()).when(sut).listSupportedFeatures(namedEntityLayer);
    }

    private TypeSystemDescription createMultiLinkWithRoleTestTypeSystem() throws Exception
    {
        var typeSystems = new ArrayList<TypeSystemDescription>();

        var tsd = new TypeSystemDescription_impl();

        // Link type
        var linkTD = tsd.addType(LINK_TYPE, "", CAS.TYPE_NAME_TOP);
        linkTD.addFeature(ROLE_FEATURE, "", CAS.TYPE_NAME_STRING);
        linkTD.addFeature(TARGET_FEATURE, "", CAS.TYPE_NAME_ANNOTATION);

        // Link host
        var hostTD = tsd.addType(LINK_HOST_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        hostTD.addFeature(LINKS_FEATURE, "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);

        typeSystems.add(tsd);
        typeSystems.add(createTypeSystemDescription());

        return mergeTypeSystems(typeSystems);
    }

    @Test
    void testGetAttachedLinks_whenNoLinks_returnsEmpty() throws Exception
    {
        // Given a target annotation with no incoming links
        jcas.setDocumentText("This is a test.");
        var target = buildAnnotation(jcas, NamedEntity.class) //
                .on("test") //
                .buildAndAddToIndexes();

        // When checking for attached links
        var result = sut.getAttachedLinks(namedEntityLayer, target);

        // Then no links should be found
        assertThat(result).isEmpty();
    }

    @Test
    void testGetAttachedLinks_withSingleLink_returnsTheLink() throws Exception
    {
        // Given a target annotation and a host with a link to it
        jcas.setDocumentText("This is a test.");

        var target = buildAnnotation(jcas, NamedEntity.class) //
                .on("test") //
                .buildAndAddToIndexes();

        var link = createLink(jcas, "role1", target);

        var host = buildAnnotation(jcas, LINK_HOST_TYPE) //
                .at(0, 4) // "This"
                .withFeature(LINKS_FEATURE, asList(link)) //
                .buildAndAddToIndexes();

        // When checking for attached links
        var result = sut.getAttachedLinks(namedEntityLayer, target);

        // Then one incoming link should be found
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEndpoint()).isEqualTo(host);
        assertThat(result.get(0).getDirection()).isEqualTo(INCOMING);
    }

    @Test
    void testGetAttachedLinks_withMultipleLinksFromSameHost_returnsSingleAttachment()
        throws Exception
    {
        // Given a target and a host with multiple links to the same target
        jcas.setDocumentText("This is a test.");

        var target = buildAnnotation(jcas, NamedEntity.class) //
                .on("test") //
                .buildAndAddToIndexes();

        var link1 = createLink(jcas, "role1", target);
        var link2 = createLink(jcas, "role2", target);

        var host = buildAnnotation(jcas, LINK_HOST_TYPE) //
                .at(0, 4) // "This"
                .withFeature(LINKS_FEATURE, asList(link1, link2)) //
                .buildAndAddToIndexes();

        // When checking for attached links
        var result = sut.getAttachedLinks(namedEntityLayer, target);

        // Then should return two attachments (one for each link)
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEndpoint()).isEqualTo(host);
        assertThat(result.get(1).getEndpoint()).isEqualTo(host);
    }

    @Test
    void testGetAttachedLinks_withLinksFromMultipleHosts_returnsAllHosts() throws Exception
    {
        // Given a target with links from multiple hosts
        jcas.setDocumentText("This is a test.");

        var target = buildAnnotation(jcas, NamedEntity.class) //
                .on("test") //
                .buildAndAddToIndexes();

        var link1 = createLink(jcas, "role1", target);
        var host1 = buildAnnotation(jcas, LINK_HOST_TYPE) //
                .at(0, 4) // "This"
                .withFeature(LINKS_FEATURE, asList(link1)) //
                .buildAndAddToIndexes();

        var link2 = createLink(jcas, "role2", target);
        var host2 = buildAnnotation(jcas, LINK_HOST_TYPE) //
                .at(5, 7) // "is"
                .withFeature(LINKS_FEATURE, asList(link2)) //
                .buildAndAddToIndexes();

        // When checking for attached links
        var result = sut.getAttachedLinks(namedEntityLayer, target);

        // Then should return both hosts
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AttachedAnnotation::getEndpoint) //
                .containsExactlyInAnyOrder(host1, host2);
    }

    @Test
    void testGetAttachedLinks_withLinkToOtherTarget_returnsEmpty() throws Exception
    {
        // Given a target and a host with links to a different target
        jcas.setDocumentText("This is a test.");

        var target = buildAnnotation(jcas, NamedEntity.class) //
                .on("test") //
                .buildAndAddToIndexes();

        var otherTarget = buildAnnotation(jcas, NamedEntity.class) //
                .on("is") //
                .buildAndAddToIndexes();

        var link = createLink(jcas, "role1", otherTarget);

        var host = buildAnnotation(jcas, LINK_HOST_TYPE) //
                .at(0, 4) // "This"
                .withFeature(LINKS_FEATURE, asList(link)) //
                .buildAndAddToIndexes();

        // When checking for attached links
        var result = sut.getAttachedLinks(namedEntityLayer, target);

        // Then no links should be found
        assertThat(result).isEmpty();
    }

    @Test
    void testGetAttachedLinks_withEmptyLinkArray_returnsEmpty() throws Exception
    {
        // Given a target and a host with an empty link array
        jcas.setDocumentText("This is a test.");

        var target = buildAnnotation(jcas, NamedEntity.class) //
                .on("test") //
                .buildAndAddToIndexes();

        var host = buildAnnotation(jcas, LINK_HOST_TYPE) //
                .at(0, 4) // "This"
                .withFeature(LINKS_FEATURE, new ArrayList<>()) //
                .buildAndAddToIndexes();

        // When checking for attached links
        var result = sut.getAttachedLinks(namedEntityLayer, target);

        // Then no links should be found
        assertThat(result).isEmpty();
    }

    @Test
    void testGetAttachedLinks_withOutgoingLinks_returnsTargets() throws Exception
    {
        // Given a link host with outgoing links to other annotations
        jcas.setDocumentText("This is a test.");

        var target1 = buildAnnotation(jcas, NamedEntity.class) //
                .on("is") //
                .buildAndAddToIndexes();

        var target2 = buildAnnotation(jcas, NamedEntity.class) //
                .on("test") //
                .buildAndAddToIndexes();

        var link1 = createLink(jcas, "role1", target1);
        var link2 = createLink(jcas, "role2", target2);

        var host = buildAnnotation(jcas, LINK_HOST_TYPE) //
                .at(0, 4) // "This"
                .withFeature(LINKS_FEATURE, asList(link1, link2)) //
                .buildAndAddToIndexes();

        // When checking for attached links on the host itself
        var result = sut.getAttachedLinks(linkHostLayer, host);

        // Then should find the outgoing link targets
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AttachedAnnotation::getEndpoint) //
                .containsExactlyInAnyOrder(target1, target2);
    }

    private FeatureStructure createLink(JCas aJCas, String aRole, NamedEntity aTarget)
    {
        return buildFS(aJCas.getCas(), LINK_TYPE) //
                .withFeature(ROLE_FEATURE, aRole) //
                .withFeature(TARGET_FEATURE, aTarget) //
                .buildAndAddToIndexes();
    }

    @Test
    void testGetAttachedLinks_withGenericLinkTypeAndIncomingLinks_returnsTheLinks() throws Exception
    {
        // Given a link feature configured to accept any annotation type as target
        var genericLinkFeature = AnnotationFeature.builder() //
                .withLayer(linkHostLayer) //
                .withName(LINKS_FEATURE) //
                .withType(CAS.TYPE_NAME_ANNOTATION) // Can link to any annotation type
                .withMultiValueMode(MultiValueMode.ARRAY) //
                .withLinkMode(LinkMode.WITH_ROLE) //
                .withLinkTypeName(LINK_TYPE) //
                .withLinkTypeRoleFeatureName(ROLE_FEATURE) //
                .withLinkTypeTargetFeatureName(TARGET_FEATURE) //
                .build();

        // Update mocks to use generic link feature
        doReturn(asList(genericLinkFeature)).when(sut).listAttachedLinkFeatures(any());
        doReturn(asList(genericLinkFeature)).when(sut).listSupportedFeatures(linkHostLayer);

        // Given a NamedEntity target and a host with a link to it
        jcas.setDocumentText("This is a test.");

        var target = buildAnnotation(jcas, NamedEntity.class) //
                .on("test") //
                .buildAndAddToIndexes();

        var link = createLink(jcas, "role1", target);

        var host = buildAnnotation(jcas, LINK_HOST_TYPE) //
                .at(0, 4) // "This"
                .withFeature(LINKS_FEATURE, asList(link)) //
                .buildAndAddToIndexes();

        // When checking for attached links
        var result = sut.getAttachedLinks(namedEntityLayer, target);

        // Then one incoming link should be found
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEndpoint()).isEqualTo(host);
        assertThat(result.get(0).getDirection()).isEqualTo(INCOMING);
    }

    @Test
    void testGetAttachedLinks_withGenericLinkTypeAndDifferentTargetTypes_returnsAllLinks()
        throws Exception
    {
        // Given a link feature configured to accept any annotation type as target
        var genericLinkFeature = AnnotationFeature.builder() //
                .withLayer(linkHostLayer) //
                .withName(LINKS_FEATURE) //
                .withType(CAS.TYPE_NAME_ANNOTATION) // Can link to any annotation type
                .withMultiValueMode(MultiValueMode.ARRAY) //
                .withLinkMode(LinkMode.WITH_ROLE) //
                .withLinkTypeName(LINK_TYPE) //
                .withLinkTypeRoleFeatureName(ROLE_FEATURE) //
                .withLinkTypeTargetFeatureName(TARGET_FEATURE) //
                .build();

        // Update mocks to use generic link feature
        doReturn(asList(genericLinkFeature)).when(sut).listAttachedLinkFeatures(any());
        doReturn(asList(genericLinkFeature)).when(sut).listSupportedFeatures(linkHostLayer);

        // Create POS layer for testing with different annotation types
        var posLayer = AnnotationLayer.builder() //
                .forJCasClass(POS.class) //
                .withName("POS") //
                .withType(SpanLayerSupport.TYPE) //
                .withProject(project) //
                .build();

        // Given targets of different types (NamedEntity and POS)
        jcas.setDocumentText("This is a test.");

        var neTarget = buildAnnotation(jcas, NamedEntity.class) //
                .on("test") //
                .buildAndAddToIndexes();

        var posTarget = buildAnnotation(jcas, POS.class) //
                .on("is") //
                .buildAndAddToIndexes();

        var linkToNE = buildFS(jcas.getCas(), LINK_TYPE) //
                .withFeature(ROLE_FEATURE, "role1") //
                .withFeature(TARGET_FEATURE, neTarget) //
                .buildAndAddToIndexes();

        var linkToPOS = buildFS(jcas.getCas(), LINK_TYPE) //
                .withFeature(ROLE_FEATURE, "role2") //
                .withFeature(TARGET_FEATURE, posTarget) //
                .buildAndAddToIndexes();

        var host = buildAnnotation(jcas, LINK_HOST_TYPE) //
                .at(0, 4) // "This"
                .withFeature(LINKS_FEATURE, asList(linkToNE, linkToPOS)) //
                .buildAndAddToIndexes();

        // When checking for attached links on the NamedEntity target
        var neResult = sut.getAttachedLinks(namedEntityLayer, neTarget);

        // Then should find the link to the NamedEntity
        assertThat(neResult).hasSize(1);
        assertThat(neResult.get(0).getEndpoint()).isEqualTo(host);

        // When checking for attached links on the POS target
        doReturn(asList()).when(sut).listSupportedFeatures(posLayer);
        var posResult = sut.getAttachedLinks(posLayer, posTarget);

        // Then should find the link to the POS
        assertThat(posResult).hasSize(1);
        assertThat(posResult.get(0).getEndpoint()).isEqualTo(host);
    }

    @Test
    void testGetAttachedLinks_withGenericLinkTypeAndOutgoingLinks_returnsTargets() throws Exception
    {
        // Given a link feature configured to accept any annotation type as target
        var genericLinkFeature = AnnotationFeature.builder() //
                .withLayer(linkHostLayer) //
                .withName(LINKS_FEATURE) //
                .withType(CAS.TYPE_NAME_ANNOTATION) // Can link to any annotation type
                .withMultiValueMode(MultiValueMode.ARRAY) //
                .withLinkMode(LinkMode.WITH_ROLE) //
                .withLinkTypeName(LINK_TYPE) //
                .withLinkTypeRoleFeatureName(ROLE_FEATURE) //
                .withLinkTypeTargetFeatureName(TARGET_FEATURE) //
                .build();

        // Update mocks to use generic link feature
        doReturn(asList(genericLinkFeature)).when(sut).listAttachedLinkFeatures(any());
        doReturn(asList(genericLinkFeature)).when(sut).listSupportedFeatures(linkHostLayer);

        // Given targets of different types (NamedEntity and POS)
        jcas.setDocumentText("This is a test.");

        var neTarget = buildAnnotation(jcas, NamedEntity.class) //
                .on("is") //
                .buildAndAddToIndexes();

        var posTarget = buildAnnotation(jcas, POS.class) //
                .on("test") //
                .buildAndAddToIndexes();

        var linkToNE = buildFS(jcas.getCas(), LINK_TYPE) //
                .withFeature(ROLE_FEATURE, "role1") //
                .withFeature(TARGET_FEATURE, neTarget) //
                .buildAndAddToIndexes();

        var linkToPOS = buildFS(jcas.getCas(), LINK_TYPE) //
                .withFeature(ROLE_FEATURE, "role2") //
                .withFeature(TARGET_FEATURE, posTarget) //
                .buildAndAddToIndexes();

        var host = buildAnnotation(jcas, LINK_HOST_TYPE) //
                .at(0, 4) // "This"
                .withFeature(LINKS_FEATURE, asList(linkToNE, linkToPOS)) //
                .buildAndAddToIndexes();

        // When checking for attached links on the host itself
        var result = sut.getAttachedLinks(linkHostLayer, host);

        // Then should find all outgoing link targets regardless of their types
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AttachedAnnotation::getEndpoint) //
                .containsExactlyInAnyOrder(neTarget, posTarget);
        assertThat(result).allMatch(a -> a.getDirection() == OUTGOING);
    }
}
