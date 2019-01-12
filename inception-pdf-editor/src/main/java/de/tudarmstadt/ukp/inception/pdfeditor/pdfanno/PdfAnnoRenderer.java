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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.exception.MultipleMatchesFoundException;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.exception.NoMatchFoundException;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfAnnoModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractLine;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Relation;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Span;

public class PdfAnnoRenderer
{

    private static final Logger LOG = LoggerFactory.getLogger(PdfAnnoRenderer.class);

    private static final int WINDOW_SIZE_INCREMENT = 5;


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

                // convert to PDFAnno span. if it is null no match was found
                Span span = convertToPdfAnnoSpan(vspan, color, aDocumentText, aPdfExtractFile);
                if (span != null) {
                    pdfAnnoModel.addSpan(span);
                } else {
                    pdfAnnoModel.addUnmatchedSpan(vspan.getVid().getId());
                }
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

    private static Span convertToPdfAnnoSpan(VSpan aVSpan, String aColor, String aDocumentText,
                                             PdfExtractFile aPdfExtractFile)
    {
        int docTextLen = aDocumentText.length();
        // search for begin of the first range and end of the last range
        int vSpanBegin = aVSpan.getRanges().stream().mapToInt(VRange::getBegin).min().getAsInt();
        int vSpanEnd = aVSpan.getRanges().stream().mapToInt(VRange::getEnd).max().getAsInt();

        // use an context window to find a unique text snippet for an annotation
        // begin with 0 context window size and increase until a unique text snippet is found
        // context window is applied before and after the annotation
        for (int windowSize = 0; windowSize < docTextLen; windowSize += WINDOW_SIZE_INCREMENT)
        {
            // subtract windowSize from vSpanBegin and add windowSize to vSpanEnd and stay in bounds
            int windowBegin = vSpanBegin <= windowSize ? 0 : vSpanBegin - windowSize;
            int windowEnd = vSpanEnd < docTextLen - windowSize
                ? vSpanEnd + windowSize : docTextLen - 1;

            // get annotated text from aDocumentText and context window before and after it
            // also remove all whitespaces because they do not exist in PDFExtract text
            String annotatedText = aDocumentText.substring(vSpanBegin, vSpanEnd).replaceAll("\\s", "");
            String windowBeforeText = aDocumentText.substring(windowBegin, vSpanBegin)
                .replaceAll("\\s", "");
            String windowAfterText = aDocumentText.substring(vSpanEnd, windowEnd).replaceAll("\\s", "");

            try {
                int index = findMatch(aPdfExtractFile.getStringContent(),
                    windowBeforeText + annotatedText + windowAfterText);
                // get begin and end position of the original annotationText within PDFExtract text
                int annotationBegin = index + windowBeforeText.length();
                int annotationEnd = index + windowBeforeText.length() + annotatedText.length() - 1;
                // get according PDFExtract file lines for begin and end of annotation
                PdfExtractLine firstLine = aPdfExtractFile.getStringPdfExtractLine(annotationBegin);
                PdfExtractLine lastLine = aPdfExtractFile.getStringPdfExtractLine(annotationEnd);
                return new Span(aVSpan.getVid().getId(), firstLine.getPage(), aColor, annotatedText,
                    firstLine.getPosition(), lastLine.getPosition());
            } catch (MultipleMatchesFoundException e) {
                // continue and increase context window
                continue;
            } catch (NoMatchFoundException e) {
                // if no match is found stop search here. increasing context won't help
                LOG.error("Could not find a match for existing annotation with id "
                    + aVSpan.getVid().toString());
                break;
            }
        }
        return null;
    }

    /**
     * Searches for a match of a string within another string.
     * @param str string in which a match is searched
     * @param searchFor string that is searched
     * @return index if one and only one is found
     * @throws RuntimeException if there are no or multiple matches found
     */
    private static int findMatch(String str, String searchFor)
        throws NoMatchFoundException, MultipleMatchesFoundException
    {
        int index = str.indexOf(searchFor);
        if (index < 0) {
            throw new NoMatchFoundException("No match found for the searched string");
        } else if (str.indexOf(searchFor, index + 1) >= 0) {
            throw new MultipleMatchesFoundException("Multiple matches found for the searched string");
        }
        return index;
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
