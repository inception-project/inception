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
package de.tudarmstadt.ukp.inception.annotation.layer.relation;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.STACKING_ONLY;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.MultipleSentenceCoveredException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.behavior.RelationCrossSentenceBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.behavior.RelationLayerBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.behavior.RelationOverlapBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@ExtendWith(MockitoExtension.class)
public class RelationAdapterTest
{
    private @Mock ConstraintsService constraintsService;

    private LayerSupportRegistry layerSupportRegistry;
    private FeatureSupportRegistry featureSupportRegistry;
    private Project project;
    private AnnotationLayer depLayer;
    private AnnotationFeature dependencyLayerGovernor;
    private AnnotationFeature dependencyLayerDependent;
    private JCas jcas;
    private SourceDocument document;
    private String username;
    private List<RelationLayerBehavior> behaviors;

    @BeforeEach
    public void setup() throws Exception
    {
        if (jcas == null) {
            jcas = JCasFactory.createJCas();
        }
        else {
            jcas.reset();
        }

        username = "user";

        project = new Project();
        project.setId(1l);

        document = new SourceDocument();
        document.setId(1l);
        document.setProject(project);

        // Set up annotation schema with POS and Dependency
        var tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SpanLayerSupport.TYPE,
                project, true, SINGLE_TOKEN, NO_OVERLAP);
        tokenLayer.setId(1l);
        var tokenLayerPos = new AnnotationFeature(1l, tokenLayer, "pos", POS.class.getName());

        var posLayer = new AnnotationLayer(POS.class.getName(), "POS", SpanLayerSupport.TYPE,
                project, true, SINGLE_TOKEN, NO_OVERLAP);
        posLayer.setId(2l);

        depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency",
                RelationLayerSupport.TYPE, project, true, SINGLE_TOKEN, OVERLAP_ONLY);
        depLayer.setId(3l);
        depLayer.setAttachType(tokenLayer);
        depLayer.setAttachFeature(tokenLayerPos);
        dependencyLayerGovernor = new AnnotationFeature(2l, depLayer, "Governor",
                Token.class.getName());
        dependencyLayerDependent = new AnnotationFeature(3l, depLayer, "Dependent",
                Token.class.getName());

        layerSupportRegistry = new LayerSupportRegistryImpl(asList());
        featureSupportRegistry = mock(FeatureSupportRegistry.class);

        behaviors = asList(new RelationAttachmentBehavior(), new RelationOverlapBehavior(),
                new RelationCrossSentenceBehavior());
    }

    @Test
    public void thatRelationAttachmentBehaviorOnCreateWorks() throws Exception
    {
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .");

        for (var t : select(jcas, Token.class)) {
            var pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        var sut = new RelationAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                depLayer, FEAT_REL_TARGET, FEAT_REL_SOURCE,
                () -> asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors,
                constraintsService);

        var posAnnotations = new ArrayList<>(select(jcas, POS.class));
        var tokens = new ArrayList<>(select(jcas, Token.class));

        var source = posAnnotations.get(0);
        var target = posAnnotations.get(1);

        var dep = sut.add(document, username, source, target, jcas.getCas());

        assertThat(FSUtil.getFeature(dep, FEAT_REL_SOURCE, Token.class)).isEqualTo(tokens.get(0));
        assertThat(FSUtil.getFeature(dep, FEAT_REL_TARGET, Token.class)).isEqualTo(tokens.get(1));
    }

    @Test
    public void thatRelationCrossSentenceBehaviorOnCreateThrowsException() throws Exception
    {
        depLayer.setCrossSentence(false);

        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        for (var t : select(jcas, Token.class)) {
            var pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        var sut = new RelationAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                depLayer, FEAT_REL_TARGET, FEAT_REL_SOURCE,
                () -> asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors,
                constraintsService);

        var posAnnotations = new ArrayList<>(select(jcas, POS.class));

        var source = posAnnotations.get(0);
        var target = posAnnotations.get(posAnnotations.size() - 1);

        assertThatExceptionOfType(MultipleSentenceCoveredException.class)
                .isThrownBy(() -> sut.add(document, username, source, target, jcas.getCas()))
                .withMessageContaining("multiple sentences");
    }

    @Test
    public void thatRelationCrossSentenceBehaviorOnValidateGeneratesErrors() throws Exception
    {
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        for (var t : select(jcas, Token.class)) {
            var pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        var sut = new RelationAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                depLayer, FEAT_REL_TARGET, FEAT_REL_SOURCE,
                () -> asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors,
                constraintsService);

        var posAnnotations = new ArrayList<>(select(jcas, POS.class));

        var source = posAnnotations.get(0);
        var target = posAnnotations.get(posAnnotations.size() - 1);

        depLayer.setCrossSentence(true);
        sut.add(document, username, source, target, jcas.getCas());

        depLayer.setCrossSentence(false);
        assertThat(sut.validate(jcas.getCas())).extracting(Pair::getLeft)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("source", "message")
                .containsExactly(LogMessage.error(null, ""));
    }

    @Test
    public void thatCreatingRelationWorks() throws Exception
    {
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        for (var t : select(jcas, Token.class)) {
            var pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        var sut = new RelationAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                depLayer, FEAT_REL_TARGET, FEAT_REL_SOURCE,
                () -> asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors,
                constraintsService);

        var posAnnotations = new ArrayList<>(select(jcas, POS.class));
        var tokens = new ArrayList<>(select(jcas, Token.class));

        var source = posAnnotations.get(0);
        var target = posAnnotations.get(1);

        var dep1 = sut.add(document, username, source, target, jcas.getCas());

        assertThat(FSUtil.getFeature(dep1, FEAT_REL_SOURCE, Token.class)).isEqualTo(tokens.get(0));
        assertThat(FSUtil.getFeature(dep1, FEAT_REL_TARGET, Token.class)).isEqualTo(tokens.get(1));
    }

    @Test
    public void thatRelationOverlapBehaviorOnCreateWorks() throws Exception
    {
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        for (var t : select(jcas, Token.class)) {
            var pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        var sut = new RelationAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                depLayer, FEAT_REL_TARGET, FEAT_REL_SOURCE,
                () -> asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors,
                constraintsService);

        var posAnnotations = new ArrayList<>(select(jcas, POS.class));

        var source = posAnnotations.get(0);
        var target = posAnnotations.get(1);

        // First annotation should work
        depLayer.setOverlapMode(ANY_OVERLAP);
        sut.add(document, username, source, target, jcas.getCas());

        // Adding another annotation at the same place DOES NOT work
        depLayer.setOverlapMode(NO_OVERLAP);
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.add(document, username, source, target, jcas.getCas()))
                .withMessageContaining("no overlap or stacking");

        depLayer.setOverlapMode(OVERLAP_ONLY);
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.add(document, username, source, target, jcas.getCas()))
                .withMessageContaining("stacking is not allowed");

        // Adding another annotation at the same place DOES work
        depLayer.setOverlapMode(STACKING_ONLY);
        assertThatCode(() -> sut.add(document, username, source, target, jcas.getCas()))
                .doesNotThrowAnyException();

        depLayer.setOverlapMode(ANY_OVERLAP);
        assertThatCode(() -> sut.add(document, username, source, target, jcas.getCas()))
                .doesNotThrowAnyException();
    }

    @Test
    public void thatRelationOverlapBehaviorOnValidateGeneratesErrors() throws Exception
    {
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        for (var t : select(jcas, Token.class)) {
            var pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        var sut = new RelationAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                depLayer, FEAT_REL_TARGET, FEAT_REL_SOURCE,
                () -> asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors,
                constraintsService);

        var posAnnotations = new ArrayList<>(select(jcas, POS.class));

        var source = posAnnotations.get(0);
        var target = posAnnotations.get(1);

        // Create two annotations stacked annotations
        depLayer.setOverlapMode(ANY_OVERLAP);
        sut.add(document, username, source, target, jcas.getCas());
        AnnotationFS rel2 = sut.add(document, username, source, target, jcas.getCas());

        depLayer.setOverlapMode(ANY_OVERLAP);
        assertThat(sut.validate(jcas.getCas())).isEmpty();

        depLayer.setOverlapMode(STACKING_ONLY);
        assertThat(sut.validate(jcas.getCas())).isEmpty();

        depLayer.setOverlapMode(OVERLAP_ONLY);
        assertThat(sut.validate(jcas.getCas())) //
                .extracting(Pair::getLeft)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("source")
                .containsExactly( //
                        LogMessage.error(null, "Stacked relation at [5-7]"),
                        LogMessage.error(null, "Stacked relation at [5-7]"));

        depLayer.setOverlapMode(NO_OVERLAP);
        assertThat(sut.validate(jcas.getCas())) //
                .extracting(Pair::getLeft)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("source")
                .containsExactly( //
                        LogMessage.error(null, "Stacked relation at [5-7]"),
                        LogMessage.error(null, "Stacked relation at [5-7]"));

        // Remove the stacked annotation and introduce one that is purely overlapping
        sut.delete(document, username, jcas.getCas(), VID.of(rel2));
        depLayer.setOverlapMode(ANY_OVERLAP);
        sut.add(document, username, source, posAnnotations.get(2), jcas.getCas());

        depLayer.setOverlapMode(NO_OVERLAP);
        assertThat(sut.validate(jcas.getCas())) //
                .extracting(Pair::getLeft)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("source")
                .containsExactly( //
                        LogMessage.error(null, "Overlapping relation at [5-7]"),
                        LogMessage.error(null, "Overlapping relation at [8-9]"));
    }
}
