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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.STACKING_ONLY;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
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

import java.util.ArrayList;
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
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapterImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAttachmentBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.behavior.RelationCrossSentenceBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.behavior.RelationLayerBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.behavior.RelationOverlapBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;

/**
 * Verifies that {@code RelationSuggestionSupport.acceptSuggestion} does not silently overwrite an
 * existing relation when stacking is enabled and the suggestion proposes a different label.
 */
@ExtendWith(MockitoExtension.class)
public class RelationSuggestionSupportTest
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
    private AnnotationLayer depLayer;
    private AnnotationFeature dependencyTypeFeature;
    private Recommender recommender;
    private List<RelationLayerBehavior> behaviors;
    private JCas jcas;
    private RelationAdapterImpl adapter;
    private RelationSuggestionSupport sut;

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

        var tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SpanLayerSupport.TYPE,
                project, true, SINGLE_TOKEN, NO_OVERLAP);
        tokenLayer.setId(1L);
        var tokenLayerPos = new AnnotationFeature(1L, tokenLayer, "pos", POS.class.getName());

        depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency",
                RelationLayerSupport.TYPE, project, true, SINGLE_TOKEN, OVERLAP_ONLY);
        depLayer.setId(3L);
        depLayer.setAttachType(tokenLayer);
        depLayer.setAttachFeature(tokenLayerPos);

        var govFeature = new AnnotationFeature(2L, depLayer, "Governor", Token.class.getName());
        var depFeature = new AnnotationFeature(3L, depLayer, "Dependent", Token.class.getName());
        dependencyTypeFeature = new AnnotationFeature(4L, depLayer, "DependencyType",
                CAS.TYPE_NAME_STRING);

        recommender = new Recommender("test-recommender", depLayer);
        recommender.setId(99L);
        recommender.setFeature(dependencyTypeFeature);

        layerSupportRegistry = new LayerSupportRegistryImpl(emptyList());

        behaviors = asList(new RelationAttachmentBehavior(), new RelationOverlapBehavior(),
                new RelationCrossSentenceBehavior());

        // Make pushFeatureValue actually mutate the CAS so commitLabel sees the new label.
        // Mockito interface mocks do not invoke `default` methods, so we stub it explicitly.
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
        lenient().doAnswer(inv -> {
            AnnotationFeature feat = inv.getArgument(0);
            var fs = (org.apache.uima.cas.FeatureStructure) inv.getArgument(1);
            return fs.getStringValue(fs.getType().getFeatureByBaseName(feat.getName()));
        }).when(featureSupport).getFeatureValue(any(), any());

        @SuppressWarnings({ "unchecked", "rawtypes" })
        var asWild = (Optional) Optional.of(featureSupport);
        lenient().when(featureSupportRegistry.findExtension(any(AnnotationFeature.class)))
                .thenReturn(asWild);

        // Pass null for the event publisher: TypeAdapter#isSilenced is then true and the
        // recommendation-acceptance event + learning record are skipped.
        adapter = new RelationAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                depLayer, FEAT_REL_TARGET, FEAT_REL_SOURCE,
                () -> asList(govFeature, depFeature, dependencyTypeFeature), behaviors,
                constraintsService);

        sut = new RelationSuggestionSupport(null, learningRecordService, null, schemaService,
                featureSupportRegistry);
    }

    @Test
    public void thatAcceptingSuggestionAtUnoccupiedPosition_createsNewRelation() throws Exception
    {
        var tokens = createTokensAndPos();

        sut.acceptSuggestion("session", document, username, jcas.getCas(), adapter,
                dependencyTypeFeature, null,
                relationSuggestion(tokens.get(0), tokens.get(1), "subj", false), MAIN_EDITOR,
                ACCEPTED);

        assertThat(select(jcas, Dependency.class)) //
                .extracting(Dependency::getDependencyType) //
                .containsExactly("subj");
    }

    @Test
    public void thatAcceptingSuggestionAtOccupiedPosition_withoutStacking_updatesExistingLabel()
        throws Exception
    {
        depLayer.setOverlapMode(OVERLAP_ONLY);

        var tokens = createTokensAndPos();
        addRelation(tokens.get(0), tokens.get(1), "obj");

        sut.acceptSuggestion("session", document, username, jcas.getCas(), adapter,
                dependencyTypeFeature, null,
                relationSuggestion(tokens.get(0), tokens.get(1), "subj", false), MAIN_EDITOR,
                ACCEPTED);

        assertThat(select(jcas, Dependency.class)) //
                .extracting(Dependency::getDependencyType) //
                .containsExactly("subj");
    }

    @Test
    public void thatAcceptingSuggestionAtOccupiedPosition_withStacking_createsNewRelation()
        throws Exception
    {
        depLayer.setOverlapMode(STACKING_ONLY);

        var tokens = createTokensAndPos();
        addRelation(tokens.get(0), tokens.get(1), "obj");

        sut.acceptSuggestion("session", document, username, jcas.getCas(), adapter,
                dependencyTypeFeature, null,
                relationSuggestion(tokens.get(0), tokens.get(1), "subj", false), MAIN_EDITOR,
                ACCEPTED);

        assertThat(select(jcas, Dependency.class)) //
                .extracting(Dependency::getDependencyType) //
                .containsExactlyInAnyOrder("obj", "subj");
    }

    @Test
    public void thatAcceptingCorrectionSuggestion_withStacking_updatesExistingRelation()
        throws Exception
    {
        depLayer.setOverlapMode(STACKING_ONLY);

        var tokens = createTokensAndPos();
        addRelation(tokens.get(0), tokens.get(1), "obj");

        sut.acceptSuggestion("session", document, username, jcas.getCas(), adapter,
                dependencyTypeFeature, null,
                relationSuggestion(tokens.get(0), tokens.get(1), "subj", true), MAIN_EDITOR,
                ACCEPTED);

        assertThat(select(jcas, Dependency.class)) //
                .extracting(Dependency::getDependencyType) //
                .containsExactly("subj");
    }

    private List<Token> createTokensAndPos() throws Exception
    {
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .");

        for (var t : select(jcas, Token.class)) {
            var pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        return new ArrayList<>(select(jcas, Token.class));
    }

    private Dependency addRelation(Token source, Token target, String label) throws Exception
    {
        var fs = new Dependency(jcas, target.getBegin(), target.getEnd());
        fs.setGovernor(source);
        fs.setDependent(target);
        fs.setDependencyType(label);
        fs.addToIndexes();
        return fs;
    }

    private RelationSuggestion relationSuggestion(Token source, Token target, String label,
            boolean correction)
    {
        return RelationSuggestion.builder() //
                .withId(1) //
                .withRecommender(recommender) //
                .withDocument(document) //
                .withLayer(depLayer) //
                .withFeature(dependencyTypeFeature) //
                .withPosition(new RelationPosition(source.getBegin(), source.getEnd(),
                        target.getBegin(), target.getEnd())) //
                .withLabel(label) //
                .withUiLabel(label) //
                .withScore(0.9) //
                .withCorrection(correction) //
                .build();
    }
}
