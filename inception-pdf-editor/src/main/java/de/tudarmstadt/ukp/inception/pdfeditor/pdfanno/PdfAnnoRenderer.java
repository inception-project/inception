/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfAnnoModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractLine;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Relation;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Span;

public class PdfAnnoRenderer
{

    private static final Logger LOG = LoggerFactory.getLogger(PdfAnnoRenderer.class);


    public static PdfAnnoModel render(AnnotatorState aState, VDocument aVDoc, String aDocumentText,
                                      AnnotationSchemaService aAnnotationService,
                                      PdfExtractFile aPdfExtractFile)
    {
        PdfAnnoModel pdfAnnoModel = new PdfAnnoModel("0.5.0", "0.3.2");

        // Render visible (custom) layers
        Map<String[], Queue<String>> colorQueues = new HashMap<>();
        for (AnnotationLayer layer : aAnnotationService.listAnnotationLayer(aState.getProject())) {
            ColoringStrategy coloringStrategy = ColoringStrategy.getStrategy(aAnnotationService,
                layer, aState.getPreferences(), colorQueues);

            // If the layer is not included in the rendering, then we skip here - but only after
            // we have obtained a coloring strategy for this layer and thus secured the layer
            // color. This ensures that the layer colors do not change depending on the number
            // of visible layers.
            if (!aVDoc.getAnnotationLayers().contains(layer)) {
                continue;
            }

            TypeAdapter typeAdapter = aAnnotationService.getAdapter(layer);

            for (VSpan vspan : aVDoc.spans(layer.getId())) {
                String labelText = TypeUtil.getUiLabelText(typeAdapter, vspan.getFeatures());
                String color;
                if (vspan.getColorHint() == null) {
                    color = getColor(vspan, coloringStrategy, labelText);
                } else {
                    color = vspan.getColorHint();
                }

                pdfAnnoModel.addSpan(
                    convertToPdfAnnoSpan(vspan, color, aDocumentText, aPdfExtractFile));
            }

            for (VArc varc : aVDoc.arcs(layer.getId())) {
                String labelText;
                if (varc.getLabelHint() == null) {
                    labelText = TypeUtil.getUiLabelText(typeAdapter, varc.getFeatures());
                }
                else {
                    labelText = varc.getLabelHint();
                }

                String color;
                if (varc.getColorHint() == null) {
                    color = getColor(varc, coloringStrategy, labelText);
                } else {
                    color = varc.getColorHint();
                }

                pdfAnnoModel.addRelation(
                    new Relation(varc.getSource().getId(), varc.getTarget().getId(), color));
            }
        }
        return pdfAnnoModel;
    }

    private static Span convertToPdfAnnoSpan(VSpan aVspan, String aColor, String aDocumentText,
                                             PdfExtractFile aPdfExtractFile)
    {
        int docTextLen = aDocumentText.length();
        int vSpanBegin = aDocumentText.length();
        int vSpanEnd = -1;
        // search for begin of the first range and end of the last range and use those
        for (VRange range : aVspan.getRanges())
        {
            if (range.getBegin() < vSpanBegin) vSpanBegin = range.getBegin();
            if (range.getEnd() > vSpanEnd) vSpanEnd = range.getEnd();
        }

        // use an offset before and after string to find a unique text snippet for an annotation
        // begin with 0 and increase by 1 until a unique text is found
        // offset is limited to length of a quarter of the text
        for (int offset = 0; offset < docTextLen / 4 + 1; offset++)
        {
            // subtract offset from vSpanBegin and add offset to vSpanEnd and stay in bounds
            int offsetBegin = vSpanBegin <= offset ? 0 : vSpanBegin - offset;
            int offsetEnd = vSpanEnd < docTextLen - offset ? vSpanEnd + offset : docTextLen - 1;

            // get annotated text from aDocumentText with offset before and after it
            // also remove all whitespaces because they do not exist in PDFExtract text
            String annotatedText = aDocumentText.substring(offsetBegin, offsetEnd).replaceAll("\\s", "");
            String offsetBeforeText = aDocumentText.substring(offsetBegin, vSpanBegin).replaceAll("\\s", "");
            String offsetAfterText = aDocumentText.substring(vSpanEnd, offsetEnd).replaceAll("\\s", "");

            List<Integer> indices =
                getAllIndices(aPdfExtractFile.getStringContent(), annotatedText);
            if (indices.size() == 0) {
                // if there are no matches end search as adding more characters won't help anymore
                break;
            } else if (indices.size() == 1) {
                int index = indices.get(0);
                // get begin and end position of the original annotationText within PDFExtract text
                int annotationBegin = index + offsetBeforeText.length();
                int annotationEnd = index + annotatedText.length() - offsetAfterText.length() - 1;
                // get according PDFExtract file lines for begin and end of annotation
                PdfExtractLine firstLine = aPdfExtractFile.getStringPdfExtractLine(annotationBegin);
                PdfExtractLine lastLine = aPdfExtractFile.getStringPdfExtractLine(annotationEnd);
                return new Span(aVspan.getVid().getId(), firstLine.getPage(), aColor,
                    annotatedText.substring(annotationBegin - index, annotationEnd - index + 1),
                    firstLine.getPosition(), lastLine.getPosition());
            }
        }
        LOG.error("Could not map existing annotation with id " + aVspan.getVid().toString());
        return null;
    }

    private static List<Integer> getAllIndices(String searchIn, String searchFor)
    {
        List<Integer> indices = new ArrayList<>();
        int index = searchIn.indexOf(searchFor);
        while (index >= 0) {
            indices.add(index);
            index = searchIn.indexOf(searchFor, index + 1);
        }
        return indices;
    }

    private static String getColor(VObject aVObject, ColoringStrategy aColoringStrategy,
                                   String aLabelText)
    {
        String color;
        if (aVObject.getEquivalenceSet() >= 0) {
            // Every chain is supposed to have a different color
            color = ColoringStrategy.PALETTE_NORMAL_FILTERED[aVObject.getEquivalenceSet()
                % ColoringStrategy.PALETTE_NORMAL_FILTERED.length];
        }
        else {
            color = aColoringStrategy.getColor(aVObject.getVid(), aLabelText);
        }
        return color;
    }
}
