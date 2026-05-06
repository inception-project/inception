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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Objects;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.LayerFactory;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanMovedEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;

@ExtendWith(MockitoExtension.class)
class TokenAttachedSpanChangeListenerTest
{
    private @Mock AnnotationSchemaService schemaService;
    private @Mock ConstraintsService constraintsService;

    private Project project;
    private SourceDocument doc;

    private AnnotationLayer tokenLayer;
    private AnnotationLayer namedEntityLayer;
    private AnnotationLayer posLayer;
    private AnnotationFeature tokenPosFeature;

    private TokenAttachedSpanChangeListener sut;

    private JCas cas;

    private LayerSupportRegistryImpl layerRegistry;
    protected List<AnnotationFeature> features;
    private List<AnnotationLayer> layers;

    @BeforeEach
    void setup() throws Exception
    {
        cas = JCasFactory.createJCas();

        sut = new TokenAttachedSpanChangeListener(schemaService);

        project = Project.builder() //
                .build();

        doc = SourceDocument.builder() //
                .withProject(project) //
                .build();

        tokenLayer = LayerFactory.tokenLayer(project).build();

        tokenPosFeature = AnnotationFeature.builder() //
                .withLayer(tokenLayer) //
                .withName(Token._FeatName_pos) //
                .withType(POS._TypeName) //
                .build();

        namedEntityLayer = LayerFactory.namedEntityLayer(project).build();

        posLayer = LayerFactory.partOfSpeechLayer(project, tokenPosFeature).build();

        layers = asList(tokenLayer, namedEntityLayer, posLayer);
        features = asList(tokenPosFeature);

        var featureSupportRegistry = new FeatureSupportRegistryImpl(asList());
        featureSupportRegistry.init();

        var layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        layerRegistry = new LayerSupportRegistryImpl(asList(new SpanLayerSupportImpl(
                featureSupportRegistry, null, layerBehaviorRegistry, constraintsService)));
        layerRegistry.init();

        lenient().when(schemaService.listAnnotationLayer(project))
                .thenReturn(asList(tokenLayer, namedEntityLayer, posLayer));

        lenient().when(schemaService.getAdapter(any())).thenAnswer(call -> {
            var layer = call.getArgument(0, AnnotationLayer.class);
            var support = layerRegistry.findExtension(layer).get();
            return support.createAdapter(layer, () -> features);
        });

        lenient().when(schemaService.listAttachedSpanFeatures(any())).thenAnswer(call -> {
            var layer = call.getArgument(0, AnnotationLayer.class);
            return layers.stream() //
                    .filter(l -> Objects.equals(l.getAttachType(), layer)) //
                    .map(AnnotationLayer::getAttachFeature) //
                    .toList();
        });
    }

    @Nested
    class TokenAnchoredAnnotations
    {
        @Test
        void testSplitWithinSentenceNoSentenceCrossing()
        {
            cas.setDocumentText("1 2 3 4");

            var s = new Sentence(cas, 0, cas.getDocumentText().length());
            var ne = new NamedEntity(cas, 0, 3);
            var token1 = new Token(cas, 0, 1);
            var token2 = new Token(cas, 2, 5);
            asList(s, token1, token2, ne).forEach(cas::addFsToIndexes);

            namedEntityLayer.setCrossSentence(false);

            sut.onSpanMovedEvent(new SpanMovedEvent(this, doc, "user", tokenLayer, token2, 2, 3));

            assertThat(ne.getBegin()).isEqualTo(token1.getBegin()).isEqualTo(0);
            assertThat(ne.getEnd()).isEqualTo(token2.getEnd()).isEqualTo(5);
        }

        @Test
        void testMergeAtEndOfSentenceNoSentenceCrossing()
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 3);
            var s2 = new Sentence(cas, 4, 7);
            var ne = new NamedEntity(cas, 0, 3);
            var token1 = new Token(cas, 0, 1);
            var token2 = new Token(cas, 2, 5);
            asList(s1, s2, token1, token2, ne).forEach(cas::addFsToIndexes);

            namedEntityLayer.setCrossSentence(false);

            sut.onSpanMovedEvent(new SpanMovedEvent(this, doc, "user", tokenLayer, token2, 2, 3));

            assertThat(ne.getBegin()).isEqualTo(token1.getBegin()).isEqualTo(0);
            assertThat(ne.getEnd()).isEqualTo(token2.getEnd()).isEqualTo(5);
        }
    }

    @Test
    void testAdjustSingleTokenAnchoredAnnotations()
    {
        cas.setDocumentText("1 2 3 4");

        var pos = new POS(cas, 0, 1);
        var token = new Token(cas, 0, 3);
        asList(pos, token).forEach(cas::addFsToIndexes);

        sut.onSpanMovedEvent(new SpanMovedEvent(this, doc, "user", tokenLayer, token, 0, 1));

        assertThat(pos.getBegin()).isEqualTo(token.getBegin()).isEqualTo(0);
        assertThat(pos.getEnd()).isEqualTo(token.getEnd()).isEqualTo(3);
    }

    @Test
    void testAdjustAttachedAnnotations()
    {
        cas.setDocumentText("1 2 3 4");

        var pos = new POS(cas, 0, 1);
        var token = new Token(cas, 0, 3);
        token.setPos(pos);
        asList(pos, token).forEach(cas::addFsToIndexes);

        sut.onSpanMovedEvent(new SpanMovedEvent(this, doc, "user", tokenLayer, token, 0, 1));

        assertThat(pos.getBegin()).isEqualTo(token.getBegin()).isEqualTo(0);
        assertThat(pos.getEnd()).isEqualTo(token.getEnd()).isEqualTo(3);
    }
}
