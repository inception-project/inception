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
package de.tudarmstadt.ukp.inception.pdfeditor2.format;

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.Strings;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationHighlight;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.json.jsoncas2.JsonCas2Serializer;
import org.apache.uima.json.jsoncas2.mode.FeatureStructuresMode;
import org.apache.uima.json.jsoncas2.mode.OffsetConversionMode;
import org.apache.uima.json.jsoncas2.mode.SofaMode;
import org.apache.uima.json.jsoncas2.mode.TypeSystemMode;
import org.apache.uima.json.jsoncas2.ref.FullyQualifiedTypeRefGenerator;
import org.apache.uima.json.jsoncas2.ref.SequentialIdRefGenerator;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.TypeSystemUtil;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;
import org.dkpro.core.api.pdf.type.PdfPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VModel;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VPage;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderingPipeline;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

public class VisualPdfWriter
    extends JCasFileWriter_ImplBase
{
    static final String CAS_JSON_0_4 = "cas-json-0.4";
    static final String CAS_XMI_XML_1_0 = "cas-xmi-xml-1.0";

    static final PDColor BLACK = new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE);

    /**
     * ID of the project to which the documents being written belong.
     */
    public static final String PARAM_PROJECT_ID = "projectId";
    @ConfigurationParameter(name = PARAM_PROJECT_ID)
    private long projectId;
    private Project project;

    public static final String PARAM_EMBEDDED_CAS_FORMAT = "embeddedCasFormat";
    @ConfigurationParameter(name = PARAM_EMBEDDED_CAS_FORMAT, mandatory = false)
    private String embeddedCasFormat;

    private @Autowired ProjectService projectService;
    private @Autowired AnnotationSchemaService schemaService;
    private @Autowired DocumentService documentService;
    private @Autowired DocumentStorageService documentStorageService;
    private @Autowired RenderingPipeline renderingPipeline;
    private @Autowired UserDao userService;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

        ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory()
                .autowireBean(this);

        project = projectService.getProject(projectId);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException
    {
        var dmd = DocumentMetaData.get(aJCas);
        var documentName = dmd.getDocumentTitle();
        var dataOwner = userService.get(dmd.getDocumentId());
        var sourceDocument = documentService.getSourceDocument(project, documentName);
        var sourceFile = documentStorageService.getSourceDocumentFile(sourceDocument);
        var now = Calendar.getInstance();

        try (var document = Loader.loadPDF(sourceFile)) {

            if (CAS_XMI_XML_1_0.equals(embeddedCasFormat)) {
                var tsBytes = writeTypeSystem(aJCas);
                addEmbeddedFile(now, document, tsBytes, "typesystem.xml.gz", "application/gzip",
                        "UIMA Type System Description XML (gzipped)", "Data");

                var xmiBytes = writeXmi(aJCas);
                addEmbeddedFile(now, document, xmiBytes, "annotations.xmi.gz", "application/gzip",
                        "UIMA CAS in XMI format (gzipped)", "Data");
            }

            if (CAS_JSON_0_4.equals(embeddedCasFormat)) {
                var jsonBytes = writeJson(aJCas);
                addEmbeddedFile(now, document, jsonBytes, "annotation.json.gz", "application/gzip",
                        "UIMA CAS in JSON format (gzipped)", "Data");
            }

            var vModel = VisualPdfReader.visualModelFromCas(aJCas.getCas(),
                    aJCas.select(PdfPage.class).asList());

            renderAnnotations(aJCas, document, vModel, sourceDocument, dataOwner);

            document.save(getOutputStream(aJCas, ".pdf"));
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void addEmbeddedFile(Calendar now, PDDocument document, byte[] tsBytes, String filename,
            String mediaType, String description, String relationship)
        throws IOException
    {
        // Create embedded file
        var embeddedFile = new PDEmbeddedFile(document, new ByteArrayInputStream(tsBytes));
        embeddedFile.setSubtype(mediaType);
        embeddedFile.setSize(tsBytes.length);
        embeddedFile.setCreationDate(now);

        // File specification
        var fs = new PDComplexFileSpecification();
        fs.setFile(filename);
        fs.setEmbeddedFile(embeddedFile);
        fs.setFileDescription(description);
        fs.getCOSObject().setName("AFRelationship", relationship);

        // Get or create embedded files name tree
        var names = document.getDocumentCatalog().getNames();
        if (names == null) {
            names = new PDDocumentNameDictionary(document.getDocumentCatalog());
            document.getDocumentCatalog().setNames(names);
        }

        var efTree = names.getEmbeddedFiles();
        if (efTree == null) {
            efTree = new PDEmbeddedFilesNameTreeNode();
            names.setEmbeddedFiles(efTree);
        }

        var filesMap = efTree.getNames();
        if (filesMap == null) {
            filesMap = new HashMap<>();
        }
        else {
            filesMap = new HashMap<>(filesMap);
        }
        filesMap.put(filename, fs);
        efTree.setNames(filesMap);
    }

    private void renderAnnotations(JCas aJCas, PDDocument doc, VModel vModel,
            SourceDocument aSourceDocument, User aDataOwner)
        throws IOException
    {
        var layers = schemaService.listAnnotationLayer(project).stream() //
                .filter(AnnotationLayer::isEnabled) //
                .toList();

        var request = RenderRequest.builder() //
                .withDocument(aSourceDocument, aDataOwner) //
                .withSessionOwner(userService.getCurrentUser()) //
                .withCas(aJCas.getCas()) //
                .withWindow(0, Integer.MAX_VALUE) //
                .withText(false) //
                .withClipSpans(false) //
                .withClipArcs(false) //
                .withLongArcs(false) //
                .withAllLayers(layers) //
                .withVisibleLayers(layers) //
                .build();

        var vDoc = renderingPipeline.render(request);

        var colorCache = new HashMap<String, PDColor>();
        for (var span : vDoc.getSpans().values()) {
            var pdColor = colorCache.computeIfAbsent(span.getColorHint(),
                    color -> parseHexColor(color));
            for (var range : span.getRanges()) {
                createHighlight(vModel, doc, range.getBegin(), range.getEnd(), pdColor,
                        aDataOwner.getUiName(), span.getLabelHint());
            }
        }
    }

    private byte[] writeJson(JCas aJCas) throws IOException
    {
        var jcs = new JsonCas2Serializer();
        jcs.setFsMode(FeatureStructuresMode.AS_ARRAY);
        jcs.setSofaMode(SofaMode.AS_REGULAR_FEATURE_STRUCTURE);
        jcs.setTypeRefGeneratorSupplier(FullyQualifiedTypeRefGenerator::new);
        jcs.setIdRefGeneratorSupplier(SequentialIdRefGenerator::new);
        jcs.setOffsetConversionMode(OffsetConversionMode.UTF_16);
        jcs.setTypeSystemMode(TypeSystemMode.MINIMAL);
        var bos = new ByteArrayOutputStream();
        try (var gzipos = new GZIPOutputStream(bos)) {
            jcs.serialize(aJCas.getCas(), gzipos);
        }
        var jsonBytes = bos.toByteArray();
        return jsonBytes;
    }

    private byte[] writeXmi(JCas aJCas) throws IOException
    {
        var bos = new ByteArrayOutputStream();
        try (var gzipos = new GZIPOutputStream(bos)) {
            CasIOUtils.save(aJCas.getCas(), gzipos, SerialFormat.XMI);
        }
        return bos.toByteArray();

    }

    private byte[] writeTypeSystem(JCas aJCas) throws IOException
    {
        try {
            var bos = new ByteArrayOutputStream();
            try (var gzipos = new GZIPOutputStream(bos)) {
                TypeSystemUtil.typeSystem2TypeSystemDescription(aJCas.getTypeSystem())
                        .toXML(gzipos);
            }
            return bos.toByteArray();
        }
        catch (CASRuntimeException | SAXException e) {
            throw new IOException("Failed to write type system", e);
        }
    }

    private void createHighlight(VModel aVModel, PDDocument aDoc, int aBegin, int aEnd,
            PDColor aColor, String aTitlePopup, String aContents)
        throws IOException
    {
        var rectanglesByPage = mergeRectangles(aVModel, aBegin, aEnd);
        for (var pageAndRectangles : rectanglesByPage.entrySet()) {
            var page = pageAndRectangles.getKey();

            var pdfPage = aDoc.getPage(page.getIndex());
            var pageHeight = pdfPage.getMediaBox().getHeight();
            var annotations = pdfPage.getAnnotations();

            var rectangles = pageAndRectangles.getValue();
            for (var rectangle : rectangles) {
                var highlight = new PDAnnotationHighlight();
                highlight.setTitlePopup(aTitlePopup);
                highlight.setContents(aContents);
                highlight.setColor(aColor);
                highlight.setConstantOpacity(0.3f);
                var rect = new PDRectangle(rectangle.getLowerLeftX(),
                        pageHeight - rectangle.getUpperRightY(),
                        rectangle.getUpperRightX() - rectangle.getLowerLeftX(),
                        rectangle.getUpperRightY() - rectangle.getLowerLeftY());
                highlight.setRectangle(rect);
                var quads = new float[] { //
                        rect.getLowerLeftX(), rect.getUpperRightY(), //
                        rect.getUpperRightX(), rect.getUpperRightY(), //
                        rect.getLowerLeftX(), rect.getLowerLeftY(), //
                        rect.getUpperRightX(), rect.getLowerLeftY() };
                highlight.setQuadPoints(quads);
                highlight.constructAppearances(aDoc);
                annotations.add(highlight);
            }
        }
    }

    private static PDColor parseHexColor(String aColor)
    {
        if (aColor == null || aColor.isEmpty()) {
            return BLACK;
        }

        var color = Strings.CS.removeStart(aColor, "#");

        int r, g, b;

        if (color.length() == 3) {
            // Expand shorthand format to full form, e.g. "abc" -> "aabbcc"
            r = parseInt(color.substring(0, 1) + color.substring(0, 1), 16);
            g = parseInt(color.substring(1, 2) + color.substring(1, 2), 16);
            b = parseInt(color.substring(2, 3) + color.substring(2, 3), 16);
        }
        else {
            r = parseInt(color.substring(0, 2), 16);
            g = parseInt(color.substring(2, 4), 16);
            b = parseInt(color.substring(4, 6), 16);
        }

        return new PDColor(new float[] { r / 255.0f, g / 255.0f, b / 255.0f },
                PDDeviceRGB.INSTANCE);
    }

    private Map<VPage, List<BoundingBox>> mergeRectangles(VModel vModel, int begin, int end)
    {
        final var baseError = 2.5d; // px
        var rectanglesByPage = new LinkedHashMap<VPage, List<BoundingBox>>();

        for (var page : vModel.getPages()) {
            if (!overlapping(begin, end, page.getBegin(), page.getEnd())) {
                continue;
            }

            BoundingBox rectangle = null;
            for (var chunk : page.getChunks()) {
                if (!overlapping(begin, end, chunk.getBegin(), chunk.getEnd())) {
                    continue;
                }

                for (var glyph : chunk.getGlyphs()) {
                    if (!overlapping(begin, end, glyph.getBegin(), glyph.getEnd())) {
                        continue;
                    }

                    var d = chunk.getDir();
                    var x = (d == 0 || d == 180) ? glyph.getBase() : chunk.getX();
                    var y = (d == 0 || d == 180) ? chunk.getY() : glyph.getBase();
                    var w = (d == 0 || d == 180) ? glyph.getExtent() : chunk.getW();
                    var h = (d == 0 || d == 180) ? chunk.getH() : glyph.getExtent();

                    var error = min(baseError, h);

                    if (rectangle != null && rectangle.getLowerLeftY() == y
                            && withinMargin(y, rectangle.getLowerLeftY(), error)) {
                        rectangle.setLowerLeftX(min(rectangle.getLowerLeftX(), x));
                        rectangle.setLowerLeftY(min(rectangle.getLowerLeftY(), y));
                        rectangle.setUpperRightX(max(rectangle.getUpperRightX(), x + w));
                        rectangle.setUpperRightY(max(rectangle.getUpperRightY(), y + h));
                    }
                    else {
                        rectangle = new BoundingBox(x, y, x + w, y + h);
                        var rectangles = rectanglesByPage.computeIfAbsent(page,
                                $ -> new ArrayList<>());
                        rectangles.add(rectangle);
                    }
                }
            }
        }

        return rectanglesByPage;
    }

    static boolean withinMargin(double x, double base, double margin)
    {

        return (base - margin) <= x && x <= (base + margin);
    }
}
