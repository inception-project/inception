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

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfAnnoModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractLine;
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
        }
        return pdfAnnoModel;
    }

    private static Span convertToPdfAnnoSpan(VSpan aVspan, String aColor, String aDocumentText,
                                             PdfExtractFile aPdfExtractFile)
    {
        VRange range = aVspan.getRanges().get(0); // TODO: handle multiple ranges
        // use offset pre and post string to increase uniqueness of annotation text
        int offset = 20;
        int start = range.getBegin() <= offset ? 0 : range.getBegin() - offset;
        int end = range.getEnd() < aDocumentText.length() - offset
            ? range.getEnd() + offset : aDocumentText.length() - 1;

        String annotatedText = aDocumentText.substring(start, end);
        String preText = annotatedText.substring(0,
            (range.getBegin() <= offset ? range.getBegin() - start : offset));
        String postText = annotatedText.substring(annotatedText.length() -
            (range.getEnd() < aDocumentText.length() - offset
                ? offset : aDocumentText.length() - range.getEnd()), annotatedText.length()) ;
        // remove whitespaces as they are not present in PDFExtract text
        String cleanAnnotatedText = annotatedText.replaceAll("\\s", "");

        int index = aPdfExtractFile.getStringContent().indexOf(cleanAnnotatedText);
        if (index < 0) {
            LOG.error("Could not map exisiting annotation with id " + aVspan.getVid().toString());
            return null;
        } else {
            start = index + preText.replaceAll("\\s", "").length() + 1;
            end = index + cleanAnnotatedText.length() - postText.replaceAll("\\s", "").length();
            PdfExtractLine first = aPdfExtractFile.getStringPdfExtractLine(start);
            PdfExtractLine last = aPdfExtractFile.getStringPdfExtractLine(end);
            return new Span(aVspan.getVid().getId(), first.getPage(), aColor,
                cleanAnnotatedText.substring(start - index, end - index).replaceAll("\\s", ""),
                first.getPosition(), last.getPosition());
        }
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
