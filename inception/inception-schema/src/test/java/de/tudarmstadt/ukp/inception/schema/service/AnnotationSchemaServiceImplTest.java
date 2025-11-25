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

import java.io.File;
import java.util.LinkedHashSet;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.util.CasCreationUtils;
import org.apache.wicket.validation.ValidationError;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.FileSystemUtils;

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
@DataJpaTest( //
        excludeAutoConfiguration = LiquibaseAutoConfiguration.class, //
        showSql = false, //
        properties = { //
                "recommender.enabled=false", //
                "spring.main.banner-mode=off", //
                "repository.path=" + AnnotationSchemaServiceImplTest.TEST_OUTPUT_FOLDER })
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

    static final String TEST_OUTPUT_FOLDER = "target/test-output/AnnotationSchemaServiceImplTest";

    private @MockitoBean DocumentService documentService;
    private @MockitoBean DocumentImportExportService documentImportExportService;

    private @Autowired ProjectService projectService;
    private @Autowired UserDao userRepository;
    private @Autowired AnnotationSchemaService sut;

    private User annotator1;
    private Project project;

    @BeforeAll
    public static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

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

    @SpringBootConfiguration
    public static class TestContext
    {
        // No beans
    }
}
