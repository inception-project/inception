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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport.FEATURE_NAME_FIRST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.file.Path;
import java.util.LinkedHashSet;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.util.CasCreationUtils;
import org.apache.wicket.validation.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.config.ChainLayerAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.config.RelationLayerAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.IllegalFeatureValueException;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;

@EnableAutoConfiguration
@DataJpaTest(showSql = false, //
        properties = { //
                "recommender.enabled=false", //
                "spring.main.banner-mode=off" })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@Import({ //
        ConstraintsServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        RelationLayerAutoConfiguration.class, //
        ChainLayerAutoConfiguration.class })
class AnnotationSchemaServiceImplTest
{
    private static final String TAG_NOT_IN_LIST = "TAG-NOT-IN-LIST";

    static @TempDir Path tempFolder;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry)
    {
        registry.add("repository.path", () -> tempFolder.toAbsolutePath().toString());
    }

    private @MockitoBean DocumentService documentService;
    private @MockitoBean DocumentImportExportService documentImportExportService;

    private @Autowired ProjectService projectService;
    private @Autowired UserDao userRepository;
    private @Autowired AnnotationSchemaService sut;

    private User annotator1;
    private Project project;

    @BeforeEach
    public void setup() throws Exception
    {
        annotator1 = userRepository.create(new User("anno1"));

        project = projectService.createProject(new Project("project"));
        projectService.assignRole(project, annotator1, ANNOTATOR);
    }

    @Test
    void thatAddingOutOfTagsetTagOnClosedTagsetTriggersException()
    {
        var tagset = new TagSet(project, "tagset");
        tagset.setCreateTag(false);
        sut.createTagSet(tagset);

        var feature = AnnotationFeature.builder() //
                .withId(1l) //
                .withTagset(tagset) //
                .build();

        assertThatExceptionOfType(IllegalFeatureValueException.class) //
                .isThrownBy(() -> sut.createMissingTag(feature, TAG_NOT_IN_LIST));
    }

    @Test
    void thatAddingOutOfTagsetTagOnOpenTagsetCreatesTag() throws Exception
    {
        var tagset = new TagSet(project, "tagset");
        tagset.setCreateTag(true);
        sut.createTagSet(tagset);

        var feature = AnnotationFeature.builder() //
                .withId(1l) //
                .withTagset(tagset) //
                .build();

        sut.createMissingTag(feature, TAG_NOT_IN_LIST);

        assertThat(sut.existsTag(TAG_NOT_IN_LIST, tagset)).isTrue();
    }

    @Test
    void testCasUpgradePerformsGarbageCollection() throws Exception
    {
        var cas = (CASImpl) CasCreationUtils.createCas();
        try (var a = cas.ll_enableV2IdRefs(true)) {
            var ann = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
            ann.addToIndexes();
            ann.removeFromIndexes();

            var allFSesBefore = new LinkedHashSet<FeatureStructure>();
            cas.walkReachablePlusFSsSorted(allFSesBefore::add, null, null, null);

            assertThat(allFSesBefore) //
                    .as("The annotation that was added and then removed before serialization should be found") //
                    .containsExactly(cas.getSofa(), ann);

            AnnotationSchemaServiceImpl._upgradeCas(cas, cas,
                    UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription());

            var allFSesAfter = new LinkedHashSet<FeatureStructure>();
            cas.walkReachablePlusFSsSorted(allFSesAfter::add, null, null, null);

            assertThat(allFSesAfter) //
                    .as("The annotation that was added and then removed before serialization should not be found") //
                    .containsExactly(cas.getSofa());
        }
    }

    @Test
    void testDocumentNameValidationErrorMessages()
    {
        var spanLayer = AnnotationLayer.builder().withType(SpanLayerSupport.TYPE).build();
        var relationLayer = AnnotationLayer.builder().withType(RelationLayerSupport.TYPE).build();
        var chainLayer = AnnotationLayer.builder().withType(ChainLayerSupport.TYPE).build();

        assertThat(sut.validateFeatureName(
                AnnotationFeature.builder().withName("").withLayer(spanLayer).build())) //
                        .hasSize(1) //
                        .extracting(ValidationError::getMessage).first().asString() //
                        .contains("empty");

        assertThat(sut.validateFeatureName(
                AnnotationFeature.builder().withName(" ").withLayer(spanLayer).build())) //
                        .hasSize(1) //
                        .extracting(ValidationError::getMessage).first().asString() //
                        .contains("empty");

        assertThat(sut.validateFeatureName(
                AnnotationFeature.builder().withName(RelationLayerSupport.FEAT_REL_SOURCE) //
                        .withLayer(relationLayer).build())) //
                                .hasSize(1) //
                                .extracting(ValidationError::getMessage).first().asString() //
                                .contains("reserved feature name");

        assertThat(sut.validateFeatureName(AnnotationFeature.builder().withName(FEATURE_NAME_FIRST)
                .withLayer(chainLayer).build())) //
                        .hasSize(1) //
                        .extracting(ValidationError::getMessage).first().asString() //
                        .contains("reserved feature name");
    }

    @org.junit.jupiter.api.Nested
    class ListAttachedRelationLayersTests
    {
        private AnnotationLayer spanLayer;
        private AnnotationLayer spanLayer1;
        private AnnotationLayer spanLayer2;
        private AnnotationLayer relationLayer;
        private AnnotationLayer genericRelationLayer;
        private AnnotationLayer specificRelationLayer;

        @BeforeEach
        void setupLayers()
        {
            // Common span layers
            spanLayer = AnnotationLayer.builder() //
                    .withName("custom.Span") //
                    .withUiName("Span") //
                    .withType(SpanLayerSupport.TYPE) //
                    .withProject(project) //
                    .build();
            sut.createOrUpdateLayer(spanLayer);

            // Additional span layers for specific tests
            spanLayer1 = AnnotationLayer.builder() //
                    .withName("custom.Span1") //
                    .withUiName("Span1") //
                    .withType(SpanLayerSupport.TYPE) //
                    .withProject(project) //
                    .build();

            spanLayer2 = AnnotationLayer.builder() //
                    .withName("custom.Span2") //
                    .withUiName("Span2") //
                    .withType(SpanLayerSupport.TYPE) //
                    .withProject(project) //
                    .build();

            // Common relation layers
            relationLayer = AnnotationLayer.builder() //
                    .withName("custom.Relation") //
                    .withUiName("Relation") //
                    .withType(RelationLayerSupport.TYPE) //
                    .withProject(project) //
                    .build();

            genericRelationLayer = AnnotationLayer.builder() //
                    .withName("custom.GenericRelation") //
                    .withUiName("Generic Relation") //
                    .withType(RelationLayerSupport.TYPE) //
                    .withProject(project) //
                    .withAttachType(null) //
                    .build();

            specificRelationLayer = AnnotationLayer.builder() //
                    .withName("custom.SpecificRelation") //
                    .withUiName("Specific Relation") //
                    .withType(RelationLayerSupport.TYPE) //
                    .withProject(project) //
                    .build();
        }

        @Test
        void whenNoRelations_returnsEmpty()
        {
            var result = sut.listAttachedRelationLayers(spanLayer);

            assertThat(result).isEmpty();
        }

        @Test
        void whenNonSpanLayer_returnsEmpty()
        {
            sut.createOrUpdateLayer(relationLayer);

            var result = sut.listAttachedRelationLayers(relationLayer);

            assertThat(result).isEmpty();
        }

        @Test
        void withSpecificAttachType_returnsMatchingRelation()
        {
            relationLayer.setAttachType(spanLayer);
            sut.createOrUpdateLayer(relationLayer);

            var result = sut.listAttachedRelationLayers(spanLayer);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("custom.Relation");
        }

        @Test
        void withGenericRelation_returnsGenericRelation()
        {
            sut.createOrUpdateLayer(genericRelationLayer);

            var result = sut.listAttachedRelationLayers(spanLayer);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("custom.GenericRelation");
        }

        @Test
        void withAttachFeatureMatchingLayerName_returnsRelation()
        {
            sut.createOrUpdateLayer(relationLayer);

            var attachFeature = AnnotationFeature.builder() //
                    .withName("attachFeature") //
                    .withUiName("Attach Feature") //
                    .withType("custom.Span") //
                    .withLayer(relationLayer) //
                    .build();
            sut.createFeature(attachFeature);

            relationLayer.setAttachFeature(attachFeature);
            sut.createOrUpdateLayer(relationLayer);

            var result = sut.listAttachedRelationLayers(spanLayer);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("custom.Relation");
        }

        @Test
        void withMultipleRelations_returnsAll()
        {
            specificRelationLayer.setAttachType(spanLayer);
            sut.createOrUpdateLayer(specificRelationLayer);
            sut.createOrUpdateLayer(genericRelationLayer);

            var result = sut.listAttachedRelationLayers(spanLayer);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(AnnotationLayer::getName) //
                    .containsExactlyInAnyOrder("custom.SpecificRelation", "custom.GenericRelation");
        }

        @Test
        void withRelationAttachedToDifferentLayer_doesNotReturnIt()
        {
            sut.createOrUpdateLayer(spanLayer1);
            sut.createOrUpdateLayer(spanLayer2);

            relationLayer.setAttachType(spanLayer2);
            sut.createOrUpdateLayer(relationLayer);

            var result = sut.listAttachedRelationLayers(spanLayer1);

            assertThat(result).isEmpty();
        }

        @Test
        void withRelationsFromDifferentProject_doesNotReturnThem() throws Exception
        {
            var otherProject = projectService.createProject(new Project("other-project"));

            var relationInSameProject = AnnotationLayer.builder() //
                    .withName("custom.Relation1") //
                    .withUiName("Relation1") //
                    .withType(RelationLayerSupport.TYPE) //
                    .withProject(project) //
                    .withAttachType(null) //
                    .build();
            sut.createOrUpdateLayer(relationInSameProject);

            var spanLayerInOtherProject = AnnotationLayer.builder() //
                    .withName("custom.Span") //
                    .withUiName("Span") //
                    .withType(SpanLayerSupport.TYPE) //
                    .withProject(otherProject) //
                    .build();
            sut.createOrUpdateLayer(spanLayerInOtherProject);

            var relationInOtherProject = AnnotationLayer.builder() //
                    .withName("custom.Relation2") //
                    .withUiName("Relation2") //
                    .withType(RelationLayerSupport.TYPE) //
                    .withProject(otherProject) //
                    .withAttachType(null) //
                    .build();
            sut.createOrUpdateLayer(relationInOtherProject);

            var result = sut.listAttachedRelationLayers(spanLayer);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("custom.Relation1");
            assertThat(result.get(0).getProject()).isEqualTo(project);
        }

        @Test
        void returnsResultsOrderedByUiName()
        {
            var relationC = AnnotationLayer.builder() //
                    .withName("custom.RelationC") //
                    .withUiName("C Relation") //
                    .withType(RelationLayerSupport.TYPE) //
                    .withProject(project) //
                    .withAttachType(null) //
                    .build();
            sut.createOrUpdateLayer(relationC);

            var relationA = AnnotationLayer.builder() //
                    .withName("custom.RelationA") //
                    .withUiName("A Relation") //
                    .withType(RelationLayerSupport.TYPE) //
                    .withProject(project) //
                    .withAttachType(null) //
                    .build();
            sut.createOrUpdateLayer(relationA);

            var relationB = AnnotationLayer.builder() //
                    .withName("custom.RelationB") //
                    .withUiName("B Relation") //
                    .withType(RelationLayerSupport.TYPE) //
                    .withProject(project) //
                    .withAttachType(null) //
                    .build();
            sut.createOrUpdateLayer(relationB);

            var result = sut.listAttachedRelationLayers(spanLayer);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(AnnotationLayer::getUiName) //
                    .containsExactly("A Relation", "B Relation", "C Relation");
        }
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        // No beans
    }
}
