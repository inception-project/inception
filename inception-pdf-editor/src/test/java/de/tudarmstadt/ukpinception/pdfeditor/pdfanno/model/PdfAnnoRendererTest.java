/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukpinception.pdfeditor.pdfanno.model;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.PrimitiveUimaFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.SlotFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.SentenceOrientedPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRendererImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.tcf.TcfReader;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.DocumentModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Offset;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfAnnoModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.render.PdfAnnoRenderer;

public class PdfAnnoRendererTest
{

    private @Mock AnnotationSchemaService schemaService;

    private Project project;
    private AnnotationLayer tokenLayer;
    private AnnotationFeature tokenPosFeature;
    private AnnotationLayer posLayer;
    private AnnotationFeature posFeature;

    private PreRenderer preRenderer;

    @Before
    public void setup()
    {
        initMocks(this);

        project = new Project();

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
            asList(new PrimitiveUimaFeatureSupport(),
                new SlotFeatureSupport(schemaService)));
        featureSupportRegistry.init();

        LayerBehaviorRegistryImpl layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        LayerSupportRegistryImpl layerRegistry = new LayerSupportRegistryImpl(asList(
            new SpanLayerSupport(featureSupportRegistry, null, schemaService,
                layerBehaviorRegistry),
            new RelationLayerSupport(featureSupportRegistry, null, schemaService,
                layerBehaviorRegistry),
            new ChainLayerSupport(featureSupportRegistry, null, schemaService,
                layerBehaviorRegistry)));
        layerRegistry.init();

        when(schemaService.listAnnotationLayer(any())).thenReturn(asList(posLayer));
        when(schemaService.listAnnotationFeature(any(AnnotationLayer.class)))
            .thenReturn(asList(posFeature));
        when(schemaService.getAdapter(any(AnnotationLayer.class))).then(_call -> {
            AnnotationLayer layer = _call.getArgument(0);
            return layerRegistry.getLayerSupport(layer).createAdapter(layer);
        });

        preRenderer = new PreRendererImpl(layerRegistry, schemaService);
    }

    /**
     * Tests if anno file is correctly rendered for a given document
     */
    @Test
    public void testRender() throws Exception
    {
        String file = "src/test/resources/tcf04-karin-wl.xml";
        String pdftxt = new Scanner(
            new File("src/test/resources/rendererTestPdfExtract.txt")).useDelimiter("\\Z").next();

        CAS cas = JCasFactory.createJCas().getCas();
        CollectionReader reader = CollectionReaderFactory.createReader(TcfReader.class,
            TcfReader.PARAM_SOURCE_LOCATION, file);
        reader.getNext(cas);

        AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setPagingStrategy(new SentenceOrientedPagingStrategy());
        state.getPreferences().setWindowSize(10);
        state.setProject(project);

        VDocument vdoc = new VDocument();
        preRenderer.render(vdoc, 0, cas.getDocumentText().length(), cas,
                schemaService.listAnnotationLayer(project));

        PdfExtractFile pdfExtractFile = new PdfExtractFile(pdftxt, new HashMap<>());
        PdfAnnoModel annoFile = PdfAnnoRenderer.render(state, vdoc,
            cas.getDocumentText(), schemaService, pdfExtractFile, 0);

        assertThat(annoFile.getAnnoFileContent())
            .isEqualToNormalizingNewlines(contentOf(
                    new File("src/test/resources/rendererTestAnnoFile.anno"), UTF_8));
    }

    /**
     * Tests if given offsets for PDFAnno can be converted to offsets for the document in INCEpTION
     */
    @Test
    public void testConvertToDocumentOffset() throws Exception
    {
        String file = "src/test/resources/tcf04-karin-wl.xml";
        String pdftxt = new Scanner(
            new File("src/test/resources/rendererTestPdfExtract.txt")).useDelimiter("\\Z").next();
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
        // convert to offests for document in INCEpTION
        List<Offset> docOffsets =
            PdfAnnoRenderer.convertToDocumentOffsets(offsets, documentModel, pdfExtractFile);
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
