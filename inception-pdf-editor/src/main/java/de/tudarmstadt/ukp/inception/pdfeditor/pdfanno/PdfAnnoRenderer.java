/*
 * Copyright 2017
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

import org.apache.uima.jcas.JCas;
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
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.AnnoFile;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractLine;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Span;

public class PdfAnnoRenderer
{

    private static final Logger LOG = LoggerFactory.getLogger(PdfAnnoRenderer.class);


    public static AnnoFile render(AnnotatorState aState, VDocument aVDoc, JCas aJCas,
                                  AnnotationSchemaService aAnnotationService,
                                  PdfExtractFile pdfExtractFile)
    {
        AnnoFile annoFile = new AnnoFile("0.5.0", "0.3.2");

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

                annoFile.addSpan(convertToPdfAnnotation(vspan, color, aJCas, pdfExtractFile));
            }
        }
        return annoFile;
    }

    private static Span convertToPdfAnnotation(VSpan vspan, String color, JCas aJCas,
                                                     PdfExtractFile pdfExtractFile)
    {
        String docText = aJCas.getDocumentText();
        VRange range = vspan.getRanges().get(0); // TODO: handle multiple ranges
        // use offset pre and post string to increase uniqueness of annotation text
        int offset = 20;
        int start = range.getBegin() <= offset ? 0 : range.getBegin() - offset;
        int end = range.getEnd() < docText.length() - offset
            ? range.getEnd() + offset : docText.length() - 1;

        String annotatedText = docText.substring(start, end);
        String preText = annotatedText.substring(0,
            (range.getBegin() <= offset ? range.getBegin() - start : offset));
        String postText = annotatedText.substring(annotatedText.length() -
            (range.getEnd() < docText.length() - offset
                ? offset : docText.length() - range.getEnd()), annotatedText.length()) ;
        // remove whitespaces as they are not present in PDFExtract text
        String cleanAnnotatedText = annotatedText.replaceAll("\\s", "");

        int index = pdfExtractFile.getStrContent().indexOf(cleanAnnotatedText);
        if (index < 0) {
            LOG.error("Could not map exisiting annotation with id " + vspan.getVid().toString());
            return null;
        } else {
            start = index + preText.replaceAll("\\s", "").length() + 1;
            end = index + cleanAnnotatedText.length() - postText.replaceAll("\\s", "").length();
            PdfExtractLine first = pdfExtractFile.getPdfExtractLine(start);
            PdfExtractLine last = pdfExtractFile.getPdfExtractLine(end);
            return new Span(vspan.getVid().getId(), first.getPage(), color,
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
