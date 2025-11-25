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
package de.tudarmstadt.ukp.inception.annotation.layer.span;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.STACKING_ONLY;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.MultipleSentenceCoveredException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanAnchoringModeBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanCrossSentenceBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanOverlapBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@ExtendWith(MockitoExtension.class)
public class SpanAdapterTest
{
    private @Mock ConstraintsService constraintsService;
    private @Mock FeatureSupportRegistry featureSupportRegistry;

    private LayerSupportRegistry layerSupportRegistry;
    private Project project;
    private AnnotationLayer neLayer;
    private JCas jcas;
    private SourceDocument document;
    private String username;
    private List<SpanLayerBehavior> behaviors;

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

        neLayer = new AnnotationLayer(NamedEntity.class.getName(), "NE", SpanLayerSupport.TYPE,
                project, true, TOKENS, ANY_OVERLAP);
        neLayer.setId(1l);

        layerSupportRegistry = new LayerSupportRegistryImpl(asList());

        behaviors = asList(new SpanOverlapBehavior(), new SpanCrossSentenceBehavior(),
                new SpanAnchoringModeBehavior());
    }

    @Test
    public void thatSpanCrossSentenceBehaviorOnCreateThrowsException()
    {
        neLayer.setCrossSentence(false);

        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        var sut = new SpanAdapterImpl(layerSupportRegistry, featureSupportRegistry, null, neLayer,
                () -> asList(), behaviors, constraintsService);

        assertThatExceptionOfType(MultipleSentenceCoveredException.class)
                .isThrownBy(() -> sut.add(document, username, jcas.getCas(), 0,
                        jcas.getDocumentText().length()))
                .withMessageContaining("covers multiple sentences");
    }

    @Test
    public void thatSpanCrossSentenceBehaviorOnValidateReturnsErrorMessage()
        throws AnnotationException
    {
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        var sut = new SpanAdapterImpl(layerSupportRegistry, featureSupportRegistry, null, neLayer,
                () -> asList(), behaviors, constraintsService);

        // Add two annotations
        neLayer.setCrossSentence(true);
        sut.add(document, username, jcas.getCas(), 0, jcas.getDocumentText().length());

        // Validation fails
        neLayer.setCrossSentence(false);
        assertThat(sut.validate(jcas.getCas())) //
                .extracting(Pair::getLeft)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("source", "message")
                .containsExactly(LogMessage.error(null, ""));
    }

    @Test
    public void thatSpanOverlapBehaviorOnCreateWorks() throws AnnotationException
    {
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .");

        var sut = new SpanAdapterImpl(layerSupportRegistry, featureSupportRegistry, null, neLayer,
                () -> asList(), behaviors, constraintsService);

        // First time should work
        neLayer.setOverlapMode(ANY_OVERLAP);
        sut.add(document, username, jcas.getCas(), 0, 1);

        // Adding another annotation at the same place DOES NOT work
        neLayer.setOverlapMode(NO_OVERLAP);
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.add(document, username, jcas.getCas(), 0, 1))
                .withMessageContaining("no overlap or stacking");

        neLayer.setOverlapMode(OVERLAP_ONLY);
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.add(document, username, jcas.getCas(), 0, 1))
                .withMessageContaining("stacking is not allowed");

        // Adding another annotation at the same place DOES work
        neLayer.setOverlapMode(STACKING_ONLY);
        assertThatCode(() -> sut.add(document, username, jcas.getCas(), 0, 1))
                .doesNotThrowAnyException();

        neLayer.setOverlapMode(ANY_OVERLAP);
        assertThatCode(() -> sut.add(document, username, jcas.getCas(), 0, 1))
                .doesNotThrowAnyException();
    }

    @Test
    public void thatSpanOverlapBehaviorOnValidateGeneratesErrors() throws AnnotationException
    {
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .");

        var sut = new SpanAdapterImpl(layerSupportRegistry, featureSupportRegistry, null, neLayer,
                () -> asList(), behaviors, constraintsService);

        // Add two annotations
        neLayer.setOverlapMode(ANY_OVERLAP);
        sut.add(document, username, jcas.getCas(), 0, 1);
        sut.add(document, username, jcas.getCas(), 0, 1);

        // Validation succeeds
        neLayer.setOverlapMode(ANY_OVERLAP);
        assertThat(sut.validate(jcas.getCas())).isEmpty();

        neLayer.setOverlapMode(STACKING_ONLY);
        assertThat(sut.validate(jcas.getCas())).isEmpty();

        // Validation fails
        neLayer.setOverlapMode(OVERLAP_ONLY);
        assertThat(sut.validate(jcas.getCas())) //
                .extracting(Pair::getLeft)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("source")
                .containsExactly( //
                        LogMessage.error(null, "Stacked annotation at [0-4]"),
                        LogMessage.error(null, "Stacked annotation at [0-4]"));

        neLayer.setOverlapMode(NO_OVERLAP);
        assertThat(sut.validate(jcas.getCas()))//
                .extracting(Pair::getLeft)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("source")
                .containsExactly( //
                        LogMessage.error(null, "Stacked annotation at [0-4]"),
                        LogMessage.error(null, "Stacked annotation at [0-4]"));
    }

    @Test
    public void thatSpanAnchoringAndOverlapBehaviorsWorkInConcert() throws AnnotationException
    {
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .");

        var sut = new SpanAdapterImpl(layerSupportRegistry, featureSupportRegistry, null, neLayer,
                () -> asList(), behaviors, constraintsService);

        // First time should work - we annotate the whole word "This"
        neLayer.setOverlapMode(ANY_OVERLAP);
        sut.add(document, username, jcas.getCas(), 0, 4);

        // Adding another annotation at the same place DOES NOT work
        neLayer.setOverlapMode(NO_OVERLAP);
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.add(document, username, jcas.getCas(), 0, 1))
                .withMessageContaining("no overlap or stacking");

        neLayer.setOverlapMode(OVERLAP_ONLY);
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.add(document, username, jcas.getCas(), 0, 1))
                .withMessageContaining("stacking is not allowed");

        // Adding another annotation at the same place DOES work
        neLayer.setOverlapMode(STACKING_ONLY);
        assertThatCode(() -> sut.add(document, username, jcas.getCas(), 0, 1))
                .doesNotThrowAnyException();

        neLayer.setOverlapMode(ANY_OVERLAP);
        assertThatCode(() -> sut.add(document, username, jcas.getCas(), 0, 1))
                .doesNotThrowAnyException();
    }

    @Test
    public void thatAdjacentAnnotationsDoNotOverlap() throws AnnotationException
    {
        jcas.setDocumentText("Test.");
        new Sentence(jcas, 0, 5).addToIndexes();
        new Token(jcas, 0, 4).addToIndexes();
        new Token(jcas, 4, 5).addToIndexes();
        new NamedEntity(jcas, 0, 4).addToIndexes();
        new NamedEntity(jcas, 4, 5).addToIndexes();

        var sut = new SpanAdapterImpl(layerSupportRegistry, featureSupportRegistry, null, neLayer,
                () -> asList(), behaviors, constraintsService);

        neLayer.setOverlapMode(NO_OVERLAP);
        assertThat(sut.validate(jcas.getCas())).isEmpty();
    }
}
