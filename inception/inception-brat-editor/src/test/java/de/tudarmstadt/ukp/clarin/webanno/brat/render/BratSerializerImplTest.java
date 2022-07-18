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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
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

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.dkpro.core.io.tcf.TcfReader;
import org.dkpro.core.io.text.TextReader;
import org.dkpro.core.tokit.BreakIteratorSegmenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.BooleanFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.LinkFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.NumberFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.StringFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.SentenceOrientedPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.TokenWrappingPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.ColorRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.LabelRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRendererImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.RenderRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorPropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratSerializerImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class BratSerializerImplTest
{
    private @Mock AnnotationSchemaService schemaService;

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
    public void setup()
    {
        MockitoAnnotations.openMocks(this);

        project = new Project();
        sourceDocument = new SourceDocument("test.txt", project, null);

        tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SPAN_TYPE, null, true,
                SINGLE_TOKEN, NO_OVERLAP);
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

        posLayer = new AnnotationLayer(POS.class.getName(), "POS", SPAN_TYPE, project, true,
                SINGLE_TOKEN, NO_OVERLAP);
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

        FeatureSupportRegistryImpl featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new StringFeatureSupport(), new BooleanFeatureSupport(),
                        new NumberFeatureSupport(), new LinkFeatureSupport(schemaService)));
        featureSupportRegistry.init();

        LayerBehaviorRegistryImpl layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        LayerSupportRegistryImpl layerRegistry = new LayerSupportRegistryImpl(asList(
                new SpanLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry),
                new RelationLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry),
                new ChainLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry)));
        layerRegistry.init();

        when(schemaService.listSupportedLayers(any())).thenReturn(asList(posLayer));
        when(schemaService.listAnnotationLayer(any())).thenReturn(asList(posLayer));
        when(schemaService.listSupportedFeatures(any(Project.class)))
                .thenReturn(asList(posFeature));
        when(schemaService.listAnnotationFeature(any(Project.class)))
                .thenReturn(asList(posFeature));
        when(schemaService.getAdapter(any(AnnotationLayer.class))).then(_call -> {
            AnnotationLayer layer = _call.getArgument(0);
            return layerRegistry.getLayerSupport(layer).createAdapter(layer,
                    () -> asList(posFeature));
        });

        preRenderer = new PreRendererImpl(layerRegistry, schemaService);
        labelRenderer = new LabelRenderer();
        colorRenderer = new ColorRenderer(schemaService, new ColoringServiceImpl(schemaService));
        sut = new BratSerializerImpl(new BratAnnotationEditorPropertiesImpl());
    }

    @Test
    public void thatSentenceOrientedStrategyRenderCorrectly() throws Exception
    {
        String jsonFilePath = "target/test-output/output-sentence-oriented.json";
        String file = "src/test/resources/tcf04-karin-wl.xml";

        CAS cas = JCasFactory.createJCas().getCas();
        CollectionReader reader = createReader(TcfReader.class, TcfReader.PARAM_SOURCE_LOCATION,
                file);
        reader.getNext(cas);
        AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setAllAnnotationLayers(schemaService.listAnnotationLayer(project));
        state.setPagingStrategy(new SentenceOrientedPagingStrategy());
        state.getPreferences().setWindowSize(10);
        state.setFirstVisibleUnit(WebAnnoCasUtil.getFirstSentence(cas));
        state.setProject(project);
        state.setDocument(sourceDocument, asList(sourceDocument));

        RenderRequest request = RenderRequest.builder() //
                .withState(state) //
                .withWindow(state.getWindowBeginOffset(), state.getWindowEndOffset()) //
                .withCas(cas) //
                .withVisibleLayers(schemaService.listAnnotationLayer(project)) //
                .build();

        VDocument vdoc = new VDocument();
        preRenderer.render(vdoc, request);
        labelRenderer.render(vdoc, request);
        colorRenderer.render(vdoc, request);

        GetDocumentResponse response = sut.render(vdoc, request);

        JSONUtil.generatePrettyJson(response, new File(jsonFilePath));

        assertThat(contentOf(new File("src/test/resources/output-sentence-oriented.json"), UTF_8))
                .isEqualToNormalizingNewlines(contentOf(new File(jsonFilePath), UTF_8));
    }

    /**
     * generate brat JSON data for the document
     */
    @Test
    public void thatLineOrientedStrategyRenderCorrectly() throws Exception
    {
        String jsonFilePath = "target/test-output/multiline.json";
        String file = "src/test/resources/multiline.txt";

        CAS cas = JCasFactory.createJCas().getCas();
        CollectionReader reader = createReader(TextReader.class, TextReader.PARAM_SOURCE_LOCATION,
                file);
        reader.getNext(cas);
        AnalysisEngine segmenter = createEngine(BreakIteratorSegmenter.class);
        segmenter.process(cas);
        AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setPagingStrategy(new LineOrientedPagingStrategy());
        state.getPreferences().setWindowSize(10);
        state.setFirstVisibleUnit(WebAnnoCasUtil.getFirstSentence(cas));
        state.setProject(project);
        state.setDocument(sourceDocument, asList(sourceDocument));

        RenderRequest request = RenderRequest.builder() //
                .withState(state) //
                .withWindow(state.getWindowBeginOffset(), state.getWindowEndOffset()) //
                .withCas(cas) //
                .withVisibleLayers(schemaService.listAnnotationLayer(project)) //
                .build();

        VDocument vdoc = new VDocument();
        preRenderer.render(vdoc, request);
        labelRenderer.render(vdoc, request);
        colorRenderer.render(vdoc, request);

        GetDocumentResponse response = sut.render(vdoc, request);

        JSONUtil.generatePrettyJson(response, new File(jsonFilePath));

        assertThat(contentOf(new File("src/test/resources/multiline.json"), UTF_8))
                .isEqualToNormalizingNewlines(contentOf(new File(jsonFilePath), UTF_8));
    }

    /**
     * generate brat JSON data for the document
     */
    @Test
    public void thatTokenWrappingStrategyRenderCorrectly() throws Exception
    {
        String jsonFilePath = "target/test-output/longlines.json";
        String file = "src/test/resources/longlines.txt";

        CAS cas = JCasFactory.createJCas().getCas();
        CollectionReader reader = createReader(TextReader.class, TextReader.PARAM_SOURCE_LOCATION,
                file);
        reader.getNext(cas);
        AnalysisEngine segmenter = createEngine(BreakIteratorSegmenter.class);
        segmenter.process(cas);
        AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setPagingStrategy(new TokenWrappingPagingStrategy(80));
        state.getPreferences().setWindowSize(10);
        state.setFirstVisibleUnit(WebAnnoCasUtil.getFirstSentence(cas));
        state.setProject(project);
        state.setDocument(sourceDocument, asList(sourceDocument));

        RenderRequest request = RenderRequest.builder() //
                .withState(state) //
                .withWindow(state.getWindowBeginOffset(), state.getWindowEndOffset()) //
                .withCas(cas) //
                .withVisibleLayers(schemaService.listAnnotationLayer(project)) //
                .build();

        VDocument vdoc = new VDocument();
        preRenderer.render(vdoc, request);
        labelRenderer.render(vdoc, request);
        colorRenderer.render(vdoc, request);

        GetDocumentResponse response = sut.render(vdoc, request);

        JSONUtil.generatePrettyJson(response, new File(jsonFilePath));

        assertThat(contentOf(new File("src/test/resources/longlines.json"), UTF_8))
                .isEqualToNormalizingNewlines(contentOf(new File(jsonFilePath), UTF_8));
    }
}
