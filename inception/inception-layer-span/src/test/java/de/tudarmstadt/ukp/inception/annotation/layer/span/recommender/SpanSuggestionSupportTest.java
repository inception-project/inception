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
package de.tudarmstadt.ukp.inception.annotation.layer.span.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.STACKING_ONLY;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.MAIN_EDITOR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.ACCEPTED;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
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
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanAnchoringModeBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanCrossSentenceBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanOverlapBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapterImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;

/**
 * Characterizes {@code SpanSuggestionSupport.acceptSuggestion} so the existing branching
 * (create-new vs. upsert vs. fill-empty) is locked in against future regressions — particularly the
 * rule that, with stacking enabled, accepting a different-label suggestion creates a new stacked
 * annotation rather than overwriting an existing one.
 */
@ExtendWith(MockitoExtension.class)
public class SpanSuggestionSupportTest
{
    private @Mock ConstraintsService constraintsService;
    private @Mock LearningRecordService learningRecordService;
    private @Mock AnnotationSchemaService schemaService;
    private @Mock FeatureSupportRegistry featureSupportRegistry;
    private @Mock(name = "primitiveFeatureSupport") FeatureSupport<Void> featureSupport;

    private LayerSupportRegistry layerSupportRegistry;
    private Project project;
    private SourceDocument document;
    private String username;
    private AnnotationLayer neLayer;
    private AnnotationFeature valueFeature;
    private List<SpanLayerBehavior> behaviors;
    private JCas jcas;
    private SpanAdapterImpl adapter;
    private SpanSuggestionSupport sut;
    private Recommender recommender;

    @BeforeEach
    public void setup() throws Exception
    {
        jcas = JCasFactory.createJCas();
        username = "user";

        project = new Project();
        project.setId(1L);

        document = new SourceDocument();
        document.setId(1L);
        document.setProject(project);

        neLayer = new AnnotationLayer(NamedEntity.class.getName(), "NE", SpanLayerSupport.TYPE,
                project, true, TOKENS, OVERLAP_ONLY);
        neLayer.setId(1L);

        valueFeature = new AnnotationFeature(2L, neLayer, "value", CAS.TYPE_NAME_STRING);

        recommender = new Recommender("test-recommender", neLayer);
        recommender.setId(99L);
        recommender.setFeature(valueFeature);

        layerSupportRegistry = new LayerSupportRegistryImpl(emptyList());

        behaviors = asList(new SpanOverlapBehavior(), new SpanCrossSentenceBehavior(),
                new SpanAnchoringModeBehavior());

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
        // SpanSuggestionSupport reads via aAdapter.getFeatureValue to detect empty-label
        // candidates, so the mock must reflect what's actually in the CAS.
        lenient().when(featureSupport.getFeatureValue(any(), any())).thenAnswer(inv -> {
            AnnotationFeature feat = inv.getArgument(0);
            org.apache.uima.cas.FeatureStructure fs = inv.getArgument(1);
            var f = fs.getType().getFeatureByBaseName(feat.getName());
            return f == null ? null : fs.getStringValue(f);
        });

        @SuppressWarnings({ "unchecked", "rawtypes" })
        var asWild = (Optional) Optional.of(featureSupport);
        lenient().when(featureSupportRegistry.findExtension(any(AnnotationFeature.class)))
                .thenReturn(asWild);

        // Pass null event publisher → adapter is silenced → recommendation event + learning
        // record are skipped.
        adapter = new SpanAdapterImpl(layerSupportRegistry, featureSupportRegistry, null, neLayer,
                () -> asList(valueFeature), behaviors, constraintsService);

        sut = new SpanSuggestionSupport(null, learningRecordService, null, schemaService,
                featureSupportRegistry, () -> false);

        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .");
    }

    @Test
    public void thatAcceptingSuggestionAtUnoccupiedPosition_createsNewSpan() throws Exception
    {
        sut.acceptSuggestion("session", document, username, jcas.getCas(), adapter, valueFeature,
                null, spanSuggestion(0, 4, "PER", false), MAIN_EDITOR, ACCEPTED);

        assertThat(select(jcas, NamedEntity.class)) //
                .extracting(NamedEntity::getValue) //
                .containsExactly("PER");
    }

    @Test
    public void thatAcceptingSuggestionFillsEmptyLabelOnExistingSpan() throws Exception
    {
        addNamedEntity(0, 4, null);

        sut.acceptSuggestion("session", document, username, jcas.getCas(), adapter, valueFeature,
                null, spanSuggestion(0, 4, "PER", false), MAIN_EDITOR, ACCEPTED);

        assertThat(select(jcas, NamedEntity.class)) //
                .extracting(NamedEntity::getValue) //
                .containsExactly("PER");
    }

    @Test
    public void thatAcceptingSuggestionAtOccupiedPosition_withoutStacking_updatesExistingLabel()
        throws Exception
    {
        neLayer.setOverlapMode(OVERLAP_ONLY);
        addNamedEntity(0, 4, "ORG");

        sut.acceptSuggestion("session", document, username, jcas.getCas(), adapter, valueFeature,
                null, spanSuggestion(0, 4, "PER", false), MAIN_EDITOR, ACCEPTED);

        assertThat(select(jcas, NamedEntity.class)) //
                .extracting(NamedEntity::getValue) //
                .containsExactly("PER");
    }

    @Test
    public void thatAcceptingSuggestionAtOccupiedPosition_withStacking_createsNewSpan()
        throws Exception
    {
        neLayer.setOverlapMode(STACKING_ONLY);
        addNamedEntity(0, 4, "ORG");

        sut.acceptSuggestion("session", document, username, jcas.getCas(), adapter, valueFeature,
                null, spanSuggestion(0, 4, "PER", false), MAIN_EDITOR, ACCEPTED);

        assertThat(select(jcas, NamedEntity.class)) //
                .extracting(NamedEntity::getValue) //
                .containsExactlyInAnyOrder("ORG", "PER");
    }

    private NamedEntity addNamedEntity(int begin, int end, String value)
    {
        var ne = new NamedEntity(jcas, begin, end);
        if (value != null) {
            ne.setValue(value);
        }
        ne.addToIndexes();
        return ne;
    }

    private SpanSuggestion spanSuggestion(int begin, int end, String label, boolean correction)
    {
        return SpanSuggestion.builder() //
                .withId(1) //
                .withRecommender(recommender) //
                .withDocument(document) //
                .withLayer(neLayer) //
                .withFeature(valueFeature) //
                .withPosition(begin, end) //
                .withCoveredText("text") //
                .withLabel(label) //
                .withUiLabel(label) //
                .withScore(0.9) //
                .withCorrection(correction) //
                .build();
    }
}
