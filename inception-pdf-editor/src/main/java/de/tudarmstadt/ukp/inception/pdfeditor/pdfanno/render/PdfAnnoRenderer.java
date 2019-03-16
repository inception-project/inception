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
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.DocumentModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Offset;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfAnnoModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractLine;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Relation;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Span;

public class PdfAnnoRenderer
{
    private static final int WINDOW_SIZE_INCREMENT = 5;

    public static PdfAnnoModel render(AnnotatorState aState, VDocument aVDoc, String aDocumentText,
                                      AnnotationSchemaService aAnnotationService,
                                      PdfExtractFile aPdfExtractFile)
    {
        PdfAnnoModel pdfAnnoModel = new PdfAnnoModel("0.5.0", "0.3.2");
        List<RenderSpan> spans = new ArrayList<>();

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
                spans.add(new RenderSpan(vspan,
                    new Span(vspan.getVid().toString(), labelText, color)));
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

                pdfAnnoModel.addRelation(new Relation(varc.getVid().toString(),
                    varc.getSource().toString(), varc.getTarget().toString(), labelText, color));
            }
        }
        pdfAnnoModel.addSpans(convertToPdfAnnoSpans(spans, aDocumentText, aPdfExtractFile));
        return pdfAnnoModel;
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

    private static List<Span> convertToPdfAnnoSpans(
        List<RenderSpan> aSpans, String aDocumentText, PdfExtractFile aPdfExtractFile)
    {
        List<RenderSpan> spans = new ArrayList<>(aSpans);
        List<RenderSpan> ambiguous = new ArrayList<>();
        List<Span> processed = new ArrayList<>();
        int windowSize = WINDOW_SIZE_INCREMENT;

        do {
            // add context before and after each span
            addContextToSpans(spans, windowSize, aDocumentText);
            // find occurences by using Aho-Corasick algorithm
            Map<String, List<Emit>> occurrenceMap =
                findOccurrences(spans, aPdfExtractFile.getLigaturelessContent());

            for (RenderSpan renderSpan : spans) {
                // get occurrence list for span text with context window
                List<Emit> occurrences = occurrenceMap.get(renderSpan.getTextWithWindow());
                if (occurrences == null || occurrences.isEmpty()) {
                    // if occurrence list is null or empty, no match was found
                    Span span = renderSpan.getSpan();
                    span.setStartPos(-1);
                    span.setEndPos(-1);
                    span.setPage(-1);
                    span.setText(renderSpan.getText());
                    processed.add(span);
                } else if (occurrences.size() == 1) {
                    // if one occurrence was found produce a proper Span for PdfAnno
                    Span span = renderSpan.getSpan();
                    Emit emit = occurrences.get(0);
                    // get begin/end position of the original text within PDFExtract text
                    int begin = emit.getStart() + renderSpan.getWindowBeforeText().length();
                    int end = emit.getEnd() - renderSpan.getWindowAfterText().length();
                    // get according PDFExtract file lines for begin and end of annotation
                    PdfExtractLine firstLine = aPdfExtractFile.getStringPdfExtractLine(begin);
                    PdfExtractLine lastLine = aPdfExtractFile.getStringPdfExtractLine(end);
                    span.setStartPos(firstLine.getPosition());
                    span.setEndPos(lastLine.getPosition());
                    span.setPage(firstLine.getPage());
                    span.setText(renderSpan.getText());
                    processed.add(span);
                } else {
                    // if multiple occurrences found span is ambiguous. add more context and retry
                    ambiguous.add(renderSpan);
                }
            }

            spans = ambiguous;
            ambiguous = new ArrayList<>();
            windowSize += WINDOW_SIZE_INCREMENT;
        } while (!spans.isEmpty());

        return processed;
    }

    /**
     * for each span adds context with given windowsize before and after span.
     */
    private static void addContextToSpans(List<RenderSpan> aSpans, int windowSize,
                                          String text)
    {
        int textLen = text.length();
        for (RenderSpan span : aSpans) {
            int begin = span.getBegin();
            int end = span.getEnd();
            // subtract windowSize from begin and add windowSize to end and stay in bounds
            int windowBegin = begin <= windowSize ? 0 : begin - windowSize;
            int windowEnd = end < textLen - windowSize ? end + windowSize : textLen;
            // get context window before and after annotatedText
            // also remove all whitespaces
            span.setWindowBeforeText(text.substring(windowBegin, begin).replaceAll("\\s", ""));
            span.setWindowAfterText(text.substring(end, windowEnd).replaceAll("\\s", ""));
            // get annotated text and remove whitespaces
            span.setText(text.substring(begin, end).replaceAll("\\s", ""));
            span.setBegin(begin);
            span.setEnd(end);
        }
    }

    /**
     * for a given span list find occurrences of their span text with context.
     * this is achieved by using Aho-Corasick algorithm for good performance.
     */
    private static Map<String, List<Emit>> findOccurrences(List<RenderSpan> aSpans, String aText)
    {
        Map<String, List<Emit>> occurrenceMap = new HashMap<>();
        // build Aho-Corasick Trie
        Trie.TrieBuilder trieBuilder = Trie.builder();
        for (RenderSpan span : aSpans) {
            trieBuilder.addKeyword(span.getTextWithWindow());
        }
        Trie trie = trieBuilder.build();
        // find occurrences
        Collection<Emit> emits = trie.parseText(aText);
        for (Emit emit : emits) {
            occurrenceMap.putIfAbsent(emit.getKeyword(), new ArrayList<>());
            occurrenceMap.get(emit.getKeyword()).add(emit);
        }
        return occurrenceMap;
    }

    public static List<Offset> convertToDocumentOffsets(
        List<Offset> aOffsets, DocumentModel aDocumentModel, PdfExtractFile aPdfExtractFile)
    {
        List<RenderSpan> iterList = new ArrayList<>();
        for (Offset offset : aOffsets) {
            int begin = aPdfExtractFile.getStringIndex(offset.getBegin());
            int end = aPdfExtractFile.getStringIndex(offset.getEnd()) + 1;
            iterList.add(new RenderSpan(new Offset(begin, end)));
        }
        List<RenderSpan> ambiguous = new ArrayList<>();
        List<Offset> processed = new ArrayList<>();
        int windowSize = WINDOW_SIZE_INCREMENT;

        do {
            // add context before and after each span
            addContextToSpans(iterList, windowSize, aPdfExtractFile.getLigaturelessContent());
            // find occurences by using Aho-Corasick algorithm
            Map<String, List<Emit>> occurrenceMap =
                findOccurrences(iterList, aDocumentModel.getWhitespacelessText());

            for (RenderSpan renderSpan : iterList) {
                List<Emit> occurences = occurrenceMap.get(renderSpan.getTextWithWindow());
                if (occurences == null || occurences.size() == 0) {
                    // if occurrence list is null or empty, no match was found
                    processed.add(new Offset(-1, -1));
                } else if (occurences.size() == 1) {
                    // if one occurrence was found produce Offset
                    Emit emit = occurences.get(0);
                    int begin = aDocumentModel.getDocumentIndex(
                        emit.getStart() + renderSpan.getWindowBeforeText().length());
                    int end = aDocumentModel.getDocumentIndex(
                        emit.getEnd() - renderSpan.getWindowAfterText().length()) + 1;
                    processed.add(new Offset(begin, end));
                } else {
                    // if multiple occurrences found span is ambiguous. add more context and retry
                    ambiguous.add(renderSpan);
                }
            }

            iterList = ambiguous;
            ambiguous = new ArrayList<>();
            windowSize += WINDOW_SIZE_INCREMENT;
        } while (!iterList.isEmpty());

        return processed;
    }

    public static Offset convertToDocumentOffset(
        Offset aOffset, DocumentModel aDocumentModel, PdfExtractFile aPdfExtractFile)
    {
        return convertToDocumentOffsets(
            Arrays.asList(aOffset), aDocumentModel, aPdfExtractFile).get(0);
    }
}
