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
package de.tudarmstadt.ukp.inception.annotation.layer.document.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.MAIN_EDITOR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.ACCEPTED;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.document.DocumentMetadataLayerAdapterImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.document.DocumentMetadataLayerBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerTraits;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.MetadataSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;

/**
 * Locks in {@code MetadataSuggestionSupport.acceptSuggestion} branching: empty-label fill,
 * stacked-create when the layer is not a singleton, and overwrite-existing when it is.
 */
@ExtendWith(MockitoExtension.class)
public class MetadataSuggestionSupportTest
{
    private static final String META_TYPE = "test.MetaTag";
    private static final String FEATURE = "value";

    private @Mock ConstraintsService constraintsService;
    private @Mock LearningRecordService learningRecordService;
    private @Mock AnnotationSchemaService schemaService;
    private @Mock FeatureSupportRegistry featureSupportRegistry;
    private @Mock LayerSupportRegistry layerSupportRegistry;
    private @Mock(name = "primitiveFeatureSupport") FeatureSupport<Void> featureSupport;
    private @Mock DocumentMetadataLayerSupport layerSupport;

    private Project project;
    private SourceDocument document;
    private String username;
    private AnnotationLayer metaLayer;
    private AnnotationFeature valueFeature;
    private DocumentMetadataLayerTraits layerTraits;
    private List<DocumentMetadataLayerBehavior> behaviors;
    private JCas jcas;
    private DocumentMetadataLayerAdapterImpl adapter;
    private MetadataSuggestionSupport sut;
    private Recommender recommender;

    @BeforeEach
    public void setup() throws Exception
    {
        username = "user";

        project = new Project();
        project.setId(1L);

        document = new SourceDocument();
        document.setId(1L);
        document.setProject(project);

        var tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
        var td = tsd.addType(META_TYPE, "", CAS.TYPE_NAME_ANNOTATION_BASE);
        td.addFeature(FEATURE, "", CAS.TYPE_NAME_STRING);
        td.addFeature("i7n_uiOrder", "", CAS.TYPE_NAME_INTEGER);
        jcas = JCasFactory.createJCas(tsd);
        jcas.setDocumentText("This is a test .");

        metaLayer = new AnnotationLayer(META_TYPE, "Meta", DocumentMetadataLayerSupport.TYPE,
                project, true, SINGLE_TOKEN, OVERLAP_ONLY);
        metaLayer.setId(1L);

        valueFeature = new AnnotationFeature(2L, metaLayer, FEATURE, CAS.TYPE_NAME_STRING);

        recommender = new Recommender("test-recommender", metaLayer);
        recommender.setId(99L);
        recommender.setFeature(valueFeature);

        layerTraits = new DocumentMetadataLayerTraits();

        // Adapter delegates getTraits → layerSupportRegistry.getLayerSupport(layer).readTraits(...)
        @SuppressWarnings({ "unchecked", "rawtypes" })
        var layerSupportWild = (LayerSupport) layerSupport;
        lenient().when(layerSupportRegistry.getLayerSupport(any(AnnotationLayer.class)))
                .thenReturn(layerSupportWild);
        lenient().when(layerSupport.readTraits(any(AnnotationLayer.class)))
                .thenAnswer(inv -> layerTraits);

        behaviors = emptyList();

        lenient().when(featureSupport.accepts(any())).thenReturn(true);
        lenient().doAnswer(inv -> {
            CAS cas = inv.getArgument(0);
            AnnotationFeature feat = inv.getArgument(1);
            int addr = inv.getArgument(2);
            Object val = inv.getArgument(3);
            var fs = selectFsByAddr(cas, addr);
            fs.setStringValue(fs.getType().getFeatureByBaseName(feat.getName()), (String) val);
            return null;
        }).when(featureSupport).pushFeatureValue(any(), any(), anyInt(), any());
        lenient().when(featureSupport.getFeatureValue(any(), any())).thenAnswer(inv -> {
            AnnotationFeature feat = inv.getArgument(0);
            FeatureStructure fs = inv.getArgument(1);
            var f = fs.getType().getFeatureByBaseName(feat.getName());
            return f == null ? null : fs.getStringValue(f);
        });

        @SuppressWarnings({ "unchecked", "rawtypes" })
        var asWild = (Optional) Optional.of(featureSupport);
        lenient().when(featureSupportRegistry.findExtension(any(AnnotationFeature.class)))
                .thenReturn(asWild);

        adapter = new DocumentMetadataLayerAdapterImpl(layerSupportRegistry, featureSupportRegistry,
                null, metaLayer, () -> asList(valueFeature), constraintsService, behaviors);

        sut = new MetadataSuggestionSupport(null, learningRecordService, null, schemaService);
    }

    @Test
    public void thatAcceptingSuggestionWithoutExisting_createsNewAnnotation() throws Exception
    {
        sut.acceptSuggestion("session", document, username, jcas.getCas(), adapter, valueFeature,
                null, suggestion("topicA"), MAIN_EDITOR, ACCEPTED);

        assertThat(metaValues()).containsExactly("topicA");
    }

    @Test
    public void thatAcceptingSuggestionFillsEmptyLabelOnExistingAnnotation() throws Exception
    {
        addMeta(null);

        sut.acceptSuggestion("session", document, username, jcas.getCas(), adapter, valueFeature,
                null, suggestion("topicA"), MAIN_EDITOR, ACCEPTED);

        assertThat(metaValues()).containsExactly("topicA");
    }

    @Test
    public void thatAcceptingSuggestion_nonSingleton_createsAdditionalAnnotation() throws Exception
    {
        layerTraits.setSingleton(false);
        addMeta("topicA");

        sut.acceptSuggestion("session", document, username, jcas.getCas(), adapter, valueFeature,
                null, suggestion("topicB"), MAIN_EDITOR, ACCEPTED);

        assertThat(metaValues()).containsExactlyInAnyOrder("topicA", "topicB");
    }

    @Test
    public void thatAcceptingSuggestion_singleton_overwritesExistingAnnotation() throws Exception
    {
        layerTraits.setSingleton(true);
        addMeta("topicA");

        sut.acceptSuggestion("session", document, username, jcas.getCas(), adapter, valueFeature,
                null, suggestion("topicB"), MAIN_EDITOR, ACCEPTED);

        assertThat(metaValues()).containsExactly("topicB");
    }

    private void addMeta(String value)
    {
        var type = jcas.getCas().getTypeSystem().getType(META_TYPE);
        var fs = jcas.getCas().createFS(type);
        if (value != null) {
            fs.setStringValue(type.getFeatureByBaseName(FEATURE), value);
        }
        fs.setIntValue(type.getFeatureByBaseName("i7n_uiOrder"), 1);
        jcas.getCas().addFsToIndexes(fs);
    }

    private List<String> metaValues()
    {
        var type = jcas.getCas().getTypeSystem().getType(META_TYPE);
        return jcas.getCas().select(type) //
                .map(fs -> fs.getStringValue(type.getFeatureByBaseName(FEATURE))) //
                .toList();
    }

    private MetadataSuggestion suggestion(String label)
    {
        return MetadataSuggestion.builder() //
                .withId(1) //
                .withRecommender(recommender) //
                .withDocument(document) //
                .withLayer(metaLayer) //
                .withFeature(valueFeature) //
                .withLabel(label) //
                .withUiLabel(label) //
                .withScore(0.9) //
                .build();
    }
}
