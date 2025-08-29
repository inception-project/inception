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
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.STACKING_ONLY;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanCrossSentenceBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanOverlapBehavior;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;

@ExtendWith(MockitoExtension.class)
public class SpanRendererTest
{
    private @Mock ConstraintsService constraintsService;

    private FeatureSupportRegistry featureSupportRegistry;
    private LayerSupportRegistry layerSupportRegistry;
    private Project project;
    private AnnotationLayer neLayer;
    private JCas jcas;

    @BeforeEach
    public void setup() throws Exception
    {
        if (jcas == null) {
            jcas = JCasFactory.createJCas();
        }
        else {
            jcas.reset();
        }

        project = new Project();
        project.setId(1l);

        neLayer = new AnnotationLayer(NamedEntity.class.getName(), "NE", SPAN_TYPE, project, true,
                TOKENS, ANY_OVERLAP);
        neLayer.setId(1l);

        featureSupportRegistry = mock(FeatureSupportRegistry.class);
        layerSupportRegistry = new LayerSupportRegistryImpl(asList());
    }

    @Test
    public void thatSpanCrossSentenceBehaviorOnRenderGeneratesErrors()
    {
        neLayer.setCrossSentence(false);

        jcas.setDocumentText(StringUtils.repeat("a", 20));

        new Sentence(jcas, 0, 10).addToIndexes();
        new Sentence(jcas, 10, 20).addToIndexes();
        var ne = new NamedEntity(jcas, 5, 15);
        ne.addToIndexes();

        var adapter = new SpanAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                neLayer, () -> asList(), asList(new SpanCrossSentenceBehavior()),
                constraintsService);

        var sut = new SpanRenderer(adapter, layerSupportRegistry, featureSupportRegistry,
                asList(new SpanCrossSentenceBehavior()));

        var request = RenderRequest.builder() //
                .withCas(jcas.getCas()) //
                .withWindow(0, jcas.getCas().getDocumentText().length()) //
                .build();
        var vdoc = new VDocument(jcas.getCas().getDocumentText());
        sut.render(request, asList(), vdoc);

        assertThat(vdoc.comments()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactlyInAnyOrder(new VComment(ne, VCommentType.ERROR,
                        "Crossing sentence boundaries is not permitted."));
    }

    @Test
    public void thatSpanOverlapBehaviorOnRenderGeneratesErrors()
    {
        jcas.setDocumentText(StringUtils.repeat("a", 10));

        new Sentence(jcas, 0, 10).addToIndexes();
        var ne1 = new NamedEntity(jcas, 3, 8);
        ne1.addToIndexes();
        var ne2 = new NamedEntity(jcas, 3, 8);
        ne2.addToIndexes();

        var adapter = new SpanAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                neLayer, () -> asList(), asList(new SpanOverlapBehavior()), constraintsService);

        var sut = new SpanRenderer(adapter, layerSupportRegistry, featureSupportRegistry,
                asList(new SpanOverlapBehavior()));

        var request = RenderRequest.builder() //
                .withCas(jcas.getCas()) //
                .withWindow(0, jcas.getCas().getDocumentText().length()) //
                .build();

        {
            neLayer.setOverlapMode(OverlapMode.NO_OVERLAP);
            var vdoc = new VDocument(jcas.getCas().getDocumentText());
            sut.render(request, asList(), vdoc);
            assertThat(vdoc.comments()) //
                    .usingRecursiveFieldByFieldElementComparator() //
                    .containsExactlyInAnyOrder( //
                            new VComment(ne1, VCommentType.ERROR, "Stacking is not permitted."),
                            new VComment(ne2, VCommentType.ERROR, "Stacking is not permitted."));
        }

        {
            neLayer.setOverlapMode(OVERLAP_ONLY);
            var vdoc = new VDocument(jcas.getCas().getDocumentText());
            sut.render(request, asList(), vdoc);
            assertThat(vdoc.comments()).usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyInAnyOrder(
                            new VComment(ne1, VCommentType.ERROR, "Stacking is not permitted."),
                            new VComment(ne2, VCommentType.ERROR, "Stacking is not permitted."));
        }

        {
            neLayer.setOverlapMode(STACKING_ONLY);
            var vdoc = new VDocument(jcas.getCas().getDocumentText());
            sut.render(request, asList(), vdoc);
            assertThat(vdoc.comments()).isEmpty();
        }

        {
            neLayer.setOverlapMode(ANY_OVERLAP);
            var vdoc = new VDocument(jcas.getCas().getDocumentText());
            sut.render(request, asList(), vdoc);
            assertThat(vdoc.comments()).isEmpty();
        }
    }
}
