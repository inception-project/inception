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
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.dkpro.core.io.tcf.TcfReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.SentenceOrientedPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.ColorRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.LabelRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRendererImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.feature.bool.BooleanFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.number.NumberFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.render.PdfAnnoSerializer;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.schema.service.FeatureSupportRegistryImpl;

/**
 * @deprecated Superseded by the new PDF editor
 */
@Deprecated
@ExtendWith(MockitoExtension.class)
public class PdfAnnoRendererTest
{
    private @Mock AnnotationSchemaService schemaService;

    private Project project;
    private SourceDocument sourceDocument;
    private AnnotationLayer tokenLayer;
    private AnnotationFeature tokenPosFeature;
    private AnnotationLayer posLayer;
    private AnnotationFeature posFeature;

    private PreRenderer preRenderer;
    private LayerSupportRegistryImpl layerRegistry;

    @BeforeEach
    public void setup()
    {
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

        layerRegistry = new LayerSupportRegistryImpl(asList(
                new SpanLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry),
                new RelationLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry),
                new ChainLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry)));
        layerRegistry.init();

        preRenderer = new PreRendererImpl(layerRegistry, schemaService);
    }

    @Test
    public void testRender() throws Exception
    {
        when(schemaService.listAnnotationLayer(any())).thenReturn(asList(posLayer));
        when(schemaService.listAnnotationFeature(any(Project.class)))
                .thenReturn(asList(posFeature));
        when(schemaService.listSupportedFeatures(any(Project.class)))
                .thenReturn(asList(posFeature));
        when(schemaService.getAdapter(any(AnnotationLayer.class))).then(_call -> {
            AnnotationLayer layer = _call.getArgument(0);
            LayerSupport<?, ?> layerSupport = layerRegistry.getLayerSupport(layer);
            return layerSupport.createAdapter(layer,
                    () -> schemaService.listAnnotationFeature(layer));
        });

        String file = "src/test/resources/tcf04-karin-wl.xml";
        String pdftxt = new Scanner(new File("src/test/resources/rendererTestPdfExtract.txt"))
                .useDelimiter("\\Z").next();

        CAS cas = JCasFactory.createJCas().getCas();
        CollectionReader reader = CollectionReaderFactory.createReader(TcfReader.class,
                TcfReader.PARAM_SOURCE_LOCATION, file);
        reader.getNext(cas);

        AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setAllAnnotationLayers(schemaService.listAnnotationLayer(project));
        state.setPagingStrategy(new NoPagingStrategy());
        state.setPageBegin(cas, 0);
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

        new LabelRenderer().render(vdoc, request);

        ColorRenderer colorRenderer = new ColorRenderer(schemaService,
                new ColoringServiceImpl(schemaService));
        colorRenderer.render(vdoc, request);

        PdfExtractFile pdfExtractFile = new PdfExtractFile(pdftxt, new HashMap<>());
        PdfAnnoSerializer renderer = new PdfAnnoSerializer(pdfExtractFile, 0);
        PdfAnnoModel annoFile = renderer.render(vdoc, request);

        assertThat(annoFile.getAnnoFileContent()).isEqualToNormalizingNewlines(
                contentOf(new File("src/test/resources/rendererTestAnnoFile.anno"), UTF_8));
    }

    @Test
    public void testConvertToDocumentOffset() throws Exception
    {
        String file = "src/test/resources/tcf04-karin-wl.xml";
        String pdftxt = new Scanner(new File("src/test/resources/rendererTestPdfExtract.txt"))
                .useDelimiter("\\Z").next();
        PdfExtractFile pdfExtractFile = new PdfExtractFile(pdftxt, new HashMap<>());

        CAS cas = JCasFactory.createJCas().getCas();
        CollectionReader reader = CollectionReaderFactory.createReader(TcfReader.class,
                TcfReader.PARAM_SOURCE_LOCATION, file);
        reader.getNext(cas);

        AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setPagingStrategy(new SentenceOrientedPagingStrategy());
        state.getPreferences().setWindowSize(10);
        state.setProject(project);

        DocumentModel documentModel = new DocumentModel(cas.getDocumentText());
        // List of PDFAnno offsets
        // indices represent line numbers in the PDFExtractFile for the according character
        List<Offset> offsets = new ArrayList<>();
        offsets.add(new Offset(3, 3));
        offsets.add(new Offset(3, 4));
        offsets.add(new Offset(3, 5));
        offsets.add(new Offset(3, 6));
        offsets.add(new Offset(3, 7));
        offsets.add(new Offset(3, 8));
        offsets.add(new Offset(6, 8));
        offsets.add(new Offset(7, 7));
        offsets.add(new Offset(7, 8));
        offsets.add(new Offset(8, 8));
        offsets.add(new Offset(8, 13));
        offsets.add(new Offset(28, 28));
        offsets.add(new Offset(28, 30));
        offsets.add(new Offset(35, 38));
        // convert to offsets for document in INCEpTION
        List<Offset> docOffsets = PdfAnnoSerializer.convertToDocumentOffsets(offsets, documentModel,
                pdfExtractFile);
        List<Offset> expectedOffsets = new ArrayList<>();
        expectedOffsets.add(new Offset(0, 0));
        expectedOffsets.add(new Offset(0, 1));
        expectedOffsets.add(new Offset(0, 2));
        expectedOffsets.add(new Offset(0, 3));
        expectedOffsets.add(new Offset(0, 4));
        expectedOffsets.add(new Offset(0, 6));
        expectedOffsets.add(new Offset(3, 6));
        expectedOffsets.add(new Offset(4, 4));
        expectedOffsets.add(new Offset(4, 6));
        expectedOffsets.add(new Offset(6, 6));
        expectedOffsets.add(new Offset(6, 11));
        expectedOffsets.add(new Offset(29, 29));
        expectedOffsets.add(new Offset(29, 31));
        expectedOffsets.add(new Offset(38, 41));
        assertThat(docOffsets).isEqualTo(expectedOffsets);
    }
}
