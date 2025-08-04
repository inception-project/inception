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
package de.tudarmstadt.ukp.clarin.webanno.brat.render;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.dkpro.core.io.tcf.TcfReader;
import org.dkpro.core.io.text.TextReader;
import org.dkpro.core.tokit.BreakIteratorSegmenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.SentenceOrientedPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.TokenWrappingPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.ColorRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.LabelRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRendererImpl;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorPropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.feature.bool.BooleanFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.number.NumberFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.ChainLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@ExtendWith(MockitoExtension.class)
class BratSerializerImplTest
{
    private @Mock ConstraintsService constraintsService;
    private @Mock AnnotationSchemaService schemaService;
    private LayerSupportRegistryImpl layerRegistry;

    private Project project;

    private SourceDocument sourceDocument;
    private AnnotationLayer tokenLayer;
    private AnnotationFeature tokenPosFeature;
    private AnnotationLayer posLayer;
    private AnnotationFeature posFeature;

    private PreRenderer preRenderer;
    private LabelRenderer labelRenderer;
    private ColorRenderer colorRenderer;

    private BratSerializerImpl sut;

    @BeforeEach
    void setup()
    {
        project = new Project();
        sourceDocument = new SourceDocument("test.txt", project, null);

        tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SpanLayerSupport.TYPE,
                null, true, SINGLE_TOKEN, NO_OVERLAP);
        tokenLayer.setId(1l);

        tokenPosFeature = new AnnotationFeature();
        tokenPosFeature.setId(1l);
        tokenPosFeature.setName("pos");
        tokenPosFeature.setEnabled(true);
        tokenPosFeature.setType(POS.class.getName());
        tokenPosFeature.setUiName("pos");
        tokenPosFeature.setLayer(tokenLayer);
        tokenPosFeature.setProject(project);
        tokenPosFeature.setVisible(true);

        posLayer = new AnnotationLayer(POS.class.getName(), "POS", SpanLayerSupport.TYPE, project,
                true, SINGLE_TOKEN, NO_OVERLAP);
        posLayer.setId(2l);
        posLayer.setAttachType(tokenLayer);
        posLayer.setAttachFeature(tokenPosFeature);

        posFeature = new AnnotationFeature();
        posFeature.setId(2l);
        posFeature.setName("PosValue");
        posFeature.setEnabled(true);
        posFeature.setType(CAS.TYPE_NAME_STRING);
        posFeature.setUiName("PosValue");
        posFeature.setLayer(posLayer);
        posFeature.setProject(project);
        posFeature.setVisible(true);

        var featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new StringFeatureSupport(), new BooleanFeatureSupport(),
                        new NumberFeatureSupport(), new LinkFeatureSupport(schemaService)));
        featureSupportRegistry.init();

        var layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        layerRegistry = new LayerSupportRegistryImpl(asList(
                new SpanLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService),
                new RelationLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService),
                new ChainLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService)));
        layerRegistry.init();

        when(schemaService.listAnnotationLayer(any())).thenReturn(asList(posLayer));
        when(schemaService.listSupportedFeatures(any(Project.class)))
                .thenReturn(asList(posFeature));
        when(schemaService.listAnnotationFeature(any(Project.class)))
                .thenReturn(asList(posFeature));

        preRenderer = new PreRendererImpl(layerRegistry, schemaService);
        labelRenderer = new LabelRenderer();
        colorRenderer = new ColorRenderer(schemaService, new ColoringServiceImpl(schemaService));
        sut = new BratSerializerImpl(new BratAnnotationEditorPropertiesImpl());
    }

    @Test
    void thatSentenceOrientedStrategyRenderCorrectly() throws Exception
    {
        when(schemaService.getAdapter(any(AnnotationLayer.class))).then(_call -> {
            AnnotationLayer layer = _call.getArgument(0);
            return layerRegistry.getLayerSupport(layer).createAdapter(layer,
                    () -> asList(posFeature));
        });

        var jsonFilePath = "target/test-output/paging-sentence-oriented.json";
        var file = "src/test/resources/tcf04-karin-wl.xml";

        var cas = JCasFactory.createJCas().getCas();
        var reader = createReader(TcfReader.class, TcfReader.PARAM_SOURCE_LOCATION, file);
        reader.getNext(cas);
        var state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setAllAnnotationLayers(schemaService.listAnnotationLayer(project));
        state.setPagingStrategy(new SentenceOrientedPagingStrategy());
        state.getPreferences().setWindowSize(10);
        state.setFirstVisibleUnit(getFirstSentence(cas));
        state.setProject(project);
        state.setDocument(sourceDocument, asList(sourceDocument));

        var request = RenderRequest.builder() //
                .withState(state) //
                .withWindow(state.getWindowBeginOffset(), state.getWindowEndOffset()) //
                .withCas(cas) //
                .withVisibleLayers(schemaService.listAnnotationLayer(project)) //
                .build();

        var vdoc = new VDocument();
        preRenderer.render(vdoc, request);
        labelRenderer.render(vdoc, request);
        colorRenderer.render(vdoc, request);

        var response = sut.render(vdoc, request);

        JSONUtil.generatePrettyJson(response, new File(jsonFilePath));

        assertThat(contentOf(new File("src/test/resources/paging-sentence-oriented.json"), UTF_8))
                .isEqualToNormalizingNewlines(contentOf(new File(jsonFilePath), UTF_8));
    }

    @Test
    void thatLineOrientedStrategyRenderCorrectly() throws Exception
    {
        var jsonFilePath = "target/test-output/paging-line-oriented.json";
        var file = "src/test/resources/paging-line-oriented.txt";

        var cas = JCasFactory.createJCas().getCas();
        var reader = createReader(TextReader.class, TextReader.PARAM_SOURCE_LOCATION, file);
        reader.getNext(cas);
        var segmenter = createEngine(BreakIteratorSegmenter.class);
        segmenter.process(cas);
        var state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setPagingStrategy(new LineOrientedPagingStrategy());
        state.getPreferences().setWindowSize(10);
        state.setFirstVisibleUnit(getFirstSentence(cas));
        state.setProject(project);
        state.setDocument(sourceDocument, asList(sourceDocument));

        var request = RenderRequest.builder() //
                .withState(state) //
                .withWindow(state.getWindowBeginOffset(), state.getWindowEndOffset()) //
                .withCas(cas) //
                .withVisibleLayers(schemaService.listAnnotationLayer(project)) //
                .build();

        var vdoc = new VDocument();
        preRenderer.render(vdoc, request);
        labelRenderer.render(vdoc, request);
        colorRenderer.render(vdoc, request);

        var response = sut.render(vdoc, request);

        JSONUtil.generatePrettyJson(response, new File(jsonFilePath));

        assertThat(contentOf(new File(jsonFilePath), UTF_8)).isEqualToNormalizingNewlines(
                contentOf(new File("src/test/resources/paging-line-oriented.json"), UTF_8));
    }

    @Test
    void thatTokenWrappingStrategyRenderCorrectly() throws Exception
    {
        var jsonFilePath = "target/test-output/paging-token-wrapping.json";
        var file = "src/test/resources/paging-token-wrapping.txt";

        var cas = JCasFactory.createJCas().getCas();
        var reader = createReader(TextReader.class, TextReader.PARAM_SOURCE_LOCATION, file);
        reader.getNext(cas);
        var segmenter = createEngine(BreakIteratorSegmenter.class);
        segmenter.process(cas);
        var state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setPagingStrategy(new TokenWrappingPagingStrategy(80));
        state.getPreferences().setWindowSize(10);
        state.setFirstVisibleUnit(getFirstSentence(cas));
        state.setProject(project);
        state.setDocument(sourceDocument, asList(sourceDocument));

        var request = RenderRequest.builder() //
                .withState(state) //
                .withWindow(state.getWindowBeginOffset(), state.getWindowEndOffset()) //
                .withCas(cas) //
                .withVisibleLayers(schemaService.listAnnotationLayer(project)) //
                .build();

        var vdoc = new VDocument();
        preRenderer.render(vdoc, request);
        labelRenderer.render(vdoc, request);
        colorRenderer.render(vdoc, request);

        var response = sut.render(vdoc, request);

        JSONUtil.generatePrettyJson(response, new File(jsonFilePath));

        assertThat(contentOf(new File("src/test/resources/paging-token-wrapping.json"), UTF_8))
                .isEqualToNormalizingNewlines(contentOf(new File(jsonFilePath), UTF_8));
    }

    static AnnotationFS getFirstSentence(CAS aCas)
    {
        return aCas.select(Sentence.class).nullOK().get();
    }
}
