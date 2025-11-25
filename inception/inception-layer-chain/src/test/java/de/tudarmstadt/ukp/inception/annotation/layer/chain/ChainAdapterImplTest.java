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
package de.tudarmstadt.ukp.inception.annotation.layer.chain;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.STACKING_ONLY;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.util.List;

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
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanAnchoringModeBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanCrossSentenceBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanOverlapBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerBehavior;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;

@ExtendWith(MockitoExtension.class)
public class ChainAdapterImplTest
{
    private @Mock ConstraintsService constraintsService;

    private LayerSupportRegistry layerSupportRegistry;
    private FeatureSupportRegistry featureSupportRegistry;
    private Project project;
    private AnnotationLayer corefLayer;
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

        corefLayer = new AnnotationLayer(
                substring(CoreferenceChain.class.getName(), 0,
                        CoreferenceChain.class.getName().length() - ChainAdapter.CHAIN.length()),
                "Coreference", ChainLayerSupport.TYPE, project, true, TOKENS, ANY_OVERLAP);
        corefLayer.setId(1l);

        layerSupportRegistry = new LayerSupportRegistryImpl(asList());
        featureSupportRegistry = mock(FeatureSupportRegistry.class);

        behaviors = asList(new SpanOverlapBehavior(), new SpanCrossSentenceBehavior(),
                new SpanAnchoringModeBehavior());
    }

    @Test
    public void thatSpanCrossSentenceBehaviorOnCreateThrowsException()
    {
        corefLayer.setCrossSentence(false);

        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        var sut = new ChainAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                corefLayer, () -> asList(), behaviors, constraintsService);

        assertThatExceptionOfType(MultipleSentenceCoveredException.class)
                .isThrownBy(() -> sut.handle(CreateSpanAnnotationRequest.builder() //
                        .withDocument(document, username, jcas.getCas()) //
                        .withRange(0, jcas.getDocumentText().length()) //
                        .build()))
                .withMessageContaining("covers multiple sentences");
    }

    @Test
    public void thatSpanOverlapBehaviorOnCreateWorks() throws AnnotationException
    {
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .");

        var sut = new ChainAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                corefLayer, () -> asList(), behaviors, constraintsService);

        // First time should work
        corefLayer.setOverlapMode(ANY_OVERLAP);
        sut.handle(CreateSpanAnnotationRequest.builder() //
                .withDocument(document, username, jcas.getCas()) //
                .withRange(0, 1) //
                .build());

        // Adding another annotation at the same place DOES NOT work
        corefLayer.setOverlapMode(NO_OVERLAP);
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.handle(CreateSpanAnnotationRequest.builder() //
                        .withDocument(document, username, jcas.getCas()) //
                        .withRange(0, 1) //
                        .build()))
                .withMessageContaining("no overlap or stacking");

        corefLayer.setOverlapMode(OVERLAP_ONLY);
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.handle(CreateSpanAnnotationRequest.builder() //
                        .withDocument(document, username, jcas.getCas()) //
                        .withRange(0, 1) //
                        .build()))
                .withMessageContaining("stacking is not allowed");

        // Adding another annotation at the same place DOES work
        corefLayer.setOverlapMode(STACKING_ONLY);
        assertThatCode(() -> sut.handle(CreateSpanAnnotationRequest.builder() //
                .withDocument(document, username, jcas.getCas()) //
                .withRange(0, 1) //
                .build())).doesNotThrowAnyException();

        corefLayer.setOverlapMode(ANY_OVERLAP);
        assertThatCode(() -> sut.handle(CreateSpanAnnotationRequest.builder() //
                .withDocument(document, username, jcas.getCas()) //
                .withRange(0, 1) //
                .build())).doesNotThrowAnyException();
    }

    @Test
    public void thatSpanAnchoringAndStackingBehaviorsWorkInConcert() throws AnnotationException
    {
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .");

        var sut = new ChainAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                corefLayer, () -> asList(), behaviors, constraintsService);

        // First time should work - we annotate the whole word "This"
        corefLayer.setOverlapMode(ANY_OVERLAP);
        sut.handle(CreateSpanAnnotationRequest.builder() //
                .withDocument(document, username, jcas.getCas()) //
                .withRange(0, 4) //
                .build());

        // Adding another annotation at the same place DOES NOT work
        // Here we annotate "T" but it should be expanded to "This"
        corefLayer.setOverlapMode(NO_OVERLAP);
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.handle(CreateSpanAnnotationRequest.builder() //
                        .withDocument(document, username, jcas.getCas()) //
                        .withRange(0, 1) //
                        .build()))
                .withMessageContaining("no overlap or stacking");

        corefLayer.setOverlapMode(OVERLAP_ONLY);
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.handle(CreateSpanAnnotationRequest.builder() //
                        .withDocument(document, username, jcas.getCas()) //
                        .withRange(0, 1) //
                        .build()))
                .withMessageContaining("stacking is not allowed");

        // Adding another annotation at the same place DOES work
        // Here we annotate "T" but it should be expanded to "This"
        corefLayer.setOverlapMode(STACKING_ONLY);
        assertThatCode(() -> sut.handle(CreateSpanAnnotationRequest.builder() //
                .withDocument(document, username, jcas.getCas()) //
                .withRange(0, 1) //
                .build())).doesNotThrowAnyException();

        corefLayer.setOverlapMode(ANY_OVERLAP);
        assertThatCode(() -> sut.handle(CreateSpanAnnotationRequest.builder() //
                .withDocument(document, username, jcas.getCas()) //
                .withRange(0, 1) //
                .build())).doesNotThrowAnyException();
    }
}
