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

import static de.tudarmstadt.ukp.clarin.webanno.brat.schema.BratSchemaGeneratorImpl.getBratTypeName;
import static de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection.RTL;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotationEditor;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorProperties;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.AnnotationComment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.AnnotationMarker;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Comment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Normalization;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.SentenceComment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.SentenceMarker;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.TextMarker;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.TrimUtils;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.rendering.paging.Unit;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VAnnotationMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSentenceMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VTextMarker;
import de.tudarmstadt.ukp.inception.support.text.TextUtils;

/**
 * Render documents using brat. This class converts a UIMA annotation representation into the object
 * model used by brat. The result can be converted to JSON that the browser-side brat SVG renderer
 * can then use.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link BratAnnotationEditorAutoConfiguration#bratSerializer}.
 * </p>
 */
public class BratSerializerImpl
    implements BratSerializer
{
    private static final Logger LOG = LoggerFactory.getLogger(BratAnnotationEditor.class);
    public static final String ID = "Brat";

    private final BratAnnotationEditorProperties properties;

    public BratSerializerImpl(BratAnnotationEditorProperties aProperties)
    {
        properties = aProperties;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public GetDocumentResponse render(VDocument aVDoc, RenderRequest aRequest)
    {
        GetDocumentResponse aResponse = new GetDocumentResponse();
        aResponse.setRtlMode(RTL == aRequest.getState().getScriptDirection());
        aResponse.setFontZoom(aRequest.getState().getPreferences().getFontZoom());
        aResponse.setWindowBegin(aVDoc.getWindowBegin());
        aResponse.setWindowEnd(aVDoc.getWindowEnd());

        renderText(aVDoc, aResponse, aRequest);

        // The rows need to be rendered first because we use the row boundaries to split
        // cross-row spans into multiple ranges
        renderBratRowsFromUnits(aResponse, aRequest);

        renderBratTokensFromText(aResponse, aVDoc);

        renderLayers(aResponse, aVDoc);

        renderComments(aResponse, aVDoc, aRequest);

        renderMarkers(aResponse, aVDoc);

        return aResponse;
    }

    private void renderLayers(GetDocumentResponse aResponse, VDocument aVDoc)
    {
        for (AnnotationLayer layer : aVDoc.getAnnotationLayers()) {
            for (VSpan vspan : aVDoc.spans(layer.getId())) {
                List<Offsets> offsets = vspan.getRanges().stream() //
                        .flatMap(range -> split(aResponse.getSentenceOffsets(), aVDoc.getText(),
                                aVDoc.getWindowBegin(), range.getBegin(), range.getEnd()).stream())
                        .map(range -> {
                            int[] span = { range.getBegin(), range.getEnd() };
                            TrimUtils.trim(aResponse.getText(), span);
                            range.setBegin(span[0]);
                            range.setEnd(span[1]);
                            return range;
                        }) //
                        .collect(toList());

                Entity entity = new Entity(vspan.getVid(), getBratTypeName(vspan.getLayer()), offsets,
                        vspan.getLabelHint(), vspan.getColorHint(), vspan.isActionButtons());
                if (!layer.isShowTextInHover()) {
                    // If the layer is configured not to display the span text in the popup, then
                    // we simply set the popup to the empty string here.
                    // FIXME: This is obviously not optimal because it is a layer-level setting and
                    // we send it at the annotation level here. It would be better to send this
                    // along with the layer configuration (would save 4-5 characters in the server
                    // response for *every annotation on this layer*!)
                    entity.getAttributes().setHoverText("");
                }

                entity.getAttributes()
                        .setClippedAtStart(vspan.getRanges().get(0).isClippedAtBegin());
                entity.getAttributes().setClippedAtEnd(
                        vspan.getRanges().get(vspan.getRanges().size() - 1).isClippedAtEnd());

                aResponse.addEntity(entity);

                vspan.getLazyDetails().stream()
                        .map(d -> new Normalization(vspan.getVid(), d.getFeature(), d.getQuery()))
                        .forEach(aResponse::addNormalization);
            }

            for (VArc varc : aVDoc.arcs(layer.getId())) {

                Relation arc = new Relation(varc.getVid(), getBratTypeName(varc.getLayer()),
                        getArgument(varc.getSource(), varc.getTarget()), varc.getLabelHint(),
                        varc.getColorHint());
                aResponse.addRelation(arc);

                varc.getLazyDetails().stream()
                        .map(d -> new Normalization(varc.getVid(), d.getFeature(), d.getQuery()))
                        .forEach(aResponse::addNormalization);
            }
        }
    }

    private void renderComments(GetDocumentResponse aResponse, VDocument aVDoc,
            RenderRequest aRequest)
    {
        CAS cas = aRequest.getCas();

        if (cas == null) {
            // FIXME: In curation, rendering happens at a different time than serializing and
            // we can not keep the CAS around all the time - for the moment, we also do not need
            // the sentence comments on the annotators views in curation, so we ignore this.
            // Eventually, it should be changed so that we do not need the CAS here.
            return;
        }

        Map<AnnotationFS, Integer> sentenceIndexes = null;
        for (VComment vcomment : aVDoc.comments()) {
            String type;
            switch (vcomment.getCommentType()) {
            case ERROR:
                type = AnnotationComment.ANNOTATION_ERROR;
                break;
            case INFO:
                type = AnnotationComment.ANNOTATOR_NOTES;
                break;
            case YIELD:
                type = "Yield";
                break;
            default:
                type = AnnotationComment.ANNOTATOR_NOTES;
                break;
            }

            AnnotationFS fs;
            if (!vcomment.getVid().isSynthetic() && ((fs = ICasUtil.selectAnnotationByAddr(cas,
                    vcomment.getVid().getId())) != null
                    && fs.getType().getName().equals(Sentence.class.getName()))) {
                // Lazily fetching the sentences because we only need them for the comments
                if (sentenceIndexes == null) {
                    sentenceIndexes = new HashMap<>();
                    int i = 1;
                    for (AnnotationFS s : select(cas, getType(cas, Sentence.class))) {
                        sentenceIndexes.put(s, i);
                        i++;
                    }
                }

                int index = sentenceIndexes.get(fs);
                aResponse.addComment(new SentenceComment(index, type, vcomment.getComment()));
            }
            else {
                aResponse.addComment(
                        new AnnotationComment(vcomment.getVid(), type, vcomment.getComment()));
            }
        }
    }

    private void renderMarkers(GetDocumentResponse aResponse, VDocument aVDoc)
    {
        // Render markers
        for (VMarker vmarker : aVDoc.getMarkers()) {
            if (vmarker instanceof VAnnotationMarker) {
                VAnnotationMarker marker = (VAnnotationMarker) vmarker;
                aResponse.addMarker(new AnnotationMarker(vmarker.getType(), marker.getVid()));
            }
            else if (vmarker instanceof VSentenceMarker) {
                VSentenceMarker marker = (VSentenceMarker) vmarker;
                aResponse.addMarker(new SentenceMarker(vmarker.getType(), marker.getIndex()));
            }
            else if (vmarker instanceof VTextMarker) {
                var marker = (VTextMarker) vmarker;
                var range = marker.getRange();
                aResponse.addMarker(
                        new TextMarker(marker.getType(), range.getBegin(), range.getEnd()));
            }
            else {
                LOG.warn("Unknown how to render marker: [" + vmarker + "]");
            }
        }
    }

    /**
     * Argument lists for the arc annotation
     */
    private static List<Argument> getArgument(VID aGovernorFs, VID aDependentFs)
    {
        return asList(new Argument("Arg1", aGovernorFs), new Argument("Arg2", aDependentFs));
    }

    private void renderText(VDocument aVDoc, GetDocumentResponse aResponse, RenderRequest aRequest)
    {
        if (!aRequest.isIncludeText()) {
            return;
        }

        String visibleText = aVDoc.getText();
        char replacementChar = 0;
        if (StringUtils.isNotEmpty(properties.getWhiteSpaceReplacementCharacter())) {
            replacementChar = properties.getWhiteSpaceReplacementCharacter().charAt(0);
        }

        visibleText = TextUtils.sanitizeVisibleText(visibleText, replacementChar);
        aResponse.setText(visibleText);
    }

    private void renderBratTokensFromText(GetDocumentResponse aResponse, VDocument aVDoc)
    {
        List<Offsets> bratTokenOffsets = new ArrayList<>();
        String visibleText = aVDoc.getText();
        BreakIterator bi = BreakIterator.getWordInstance(Locale.ROOT);
        bi.setText(visibleText);
        int last = bi.first();
        int cur = bi.next();
        while (cur != BreakIterator.DONE) {
            Offsets offsets = new Offsets(last, cur);
            trim(visibleText, offsets);
            if (offsets.getBegin() < offsets.getEnd()) {
                bratTokenOffsets.add(offsets);
            }
            last = cur;
            cur = bi.next();
        }

        var rows = aResponse.getSentenceOffsets();
        for (Offsets offsets : bratTokenOffsets) {
            var ranges = split(rows, visibleText, aVDoc.getWindowBegin(), offsets.getBegin(),
                    offsets.getEnd());
            for (var range : ranges) {
                aResponse.addToken(range.getBegin(), range.getEnd());
            }
        }
    }

    private void renderBratRowsFromUnits(GetDocumentResponse aResponse, RenderRequest aRequest)
    {
        int windowBegin = aRequest.getWindowBeginOffset();

        aResponse.setSentenceNumberOffset(aRequest.getState().getFirstVisibleUnitIndex());

        // Render sentences
        int unitNum = aResponse.getSentenceNumberOffset();
        for (Unit unit : aRequest.getState().getVisibleUnits()) {
            aResponse.addSentence(unit.getBegin() - windowBegin, unit.getEnd() - windowBegin);

            // If there is a sentence ID, then make it accessible to the user via a sentence-level
            // comment.
            if (isNotBlank(unit.getId())) {
                aResponse.addComment(new SentenceComment(unitNum, Comment.ANNOTATOR_NOTES,
                        String.format("Sentence ID: %s", unit.getId())));
            }

            unitNum++;
        }
    }

    /**
     * Calculate the ranges for the given span. A single range cannot cross row boundaries. So for
     * spans which cover multiple rows, they are split into multiple ranges.
     * 
     * @param aRows
     *            the row offsets (window-relative positions)
     * @param aBegin
     *            the span begin (window-relative positions)
     * @param aEnd
     *            (window-relative positions)
     * @return list of ranges.
     */
    private List<Offsets> split(List<Offsets> aRows, String aText, int aWindowBegin, int aBegin,
            int aEnd)
    {
        // Zero-width spans never need to be split
        if (aBegin == aEnd) {
            return asList(new Offsets(aBegin, aEnd));
        }

        // If the annotation extends across the row boundaries, create multiple ranges for the
        // annotation, one for every row. Note that in UIMA annotations are
        // half-open intervals [begin,end) so that a begin offset must always be
        // smaller than the end of a covering annotation to be considered properly
        // covered.
        Offsets beginRow = aRows.stream()
                .filter(span -> span.getBegin() <= aBegin && aBegin < span.getEnd()) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException(
                        "Start position of range [" + (aWindowBegin + aBegin) + "-"
                                + (aWindowBegin + aEnd) + "] is not part of any visible row. In a "
                                + "sentence-based editor, this is most likely caused by "
                                + "annotations outside sentences."));

        // Zero-width annotations that are on the boundary of two directly
        // adjacent sentences (i.e. without whitespace between them) are considered
        // to be at the end of the first sentence rather than at the beginning of the
        // second sentence.
        Offsets endRow = aRows.stream()
                .filter(span -> span.getBegin() <= aEnd && aEnd <= span.getEnd()) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException(
                        "End position of range [" + (aWindowBegin + aBegin) + "-"
                                + (aWindowBegin + aEnd) + "] is not part of any visible row. In a "
                                + "sentence-based editor, this is most likely caused by "
                                + "annotations outside sentences."));

        // No need to split
        if (beginRow == endRow) {
            return asList(new Offsets(aBegin, aEnd));
        }

        List<Offsets> coveredRows = aRows.subList(aRows.indexOf(beginRow),
                aRows.indexOf(endRow) + 1);

        List<Offsets> ranges = new ArrayList<>();
        for (Offsets row : coveredRows) {
            Offsets range;

            if (row.getBegin() <= aBegin && aBegin < row.getEnd()) {
                range = new Offsets(aBegin, row.getEnd());
            }
            else if (row.getBegin() <= aEnd && aEnd <= row.getEnd()) {
                range = new Offsets(row.getBegin(), aEnd);
            }
            else {
                range = new Offsets(row.getBegin(), row.getEnd());
            }

            trim(aText, range);

            ranges.add(range);
        }

        return ranges;
    }

    public static String abbreviate(String aName)
    {
        if (aName == null || aName.length() < 3) {
            return aName;
        }

        StringBuilder abbr = new StringBuilder();
        int ti = 0;
        boolean capitalizeNext = true;
        for (int i = 0; i < aName.length(); i++) {
            int ch = aName.charAt(i);

            if (Character.isWhitespace(ch)) {
                capitalizeNext = true;
                ti = 0;
            }
            else {
                if (ti < 3) {
                    if (capitalizeNext) {
                        ch = Character.toTitleCase(ch);
                        capitalizeNext = false;
                    }
                    abbr.append((char) ch);
                }
                ti++;
            }

        }

        if (abbr.length() + 3 >= aName.length()) {
            return aName;
        }
        else {
            abbr.append("...");
        }
        return abbr.toString();
    }

    /**
     * Remove trailing or leading whitespace from the annotation.
     * 
     * @param aText
     *            the text.
     * @param aOffsets
     *            the offsets.
     */
    static private void trim(CharSequence aText, Offsets aOffsets)
    {
        int begin = aOffsets.getBegin();
        int end = aOffsets.getEnd() - 1;

        // Remove whitespace at end
        while ((end > 0) && trimChar(aText.charAt(end))) {
            end--;
        }
        end++;

        // Remove whitespace at start
        while ((begin < end) && trimChar(aText.charAt(begin))) {
            begin++;
        }

        aOffsets.setBegin(begin);
        aOffsets.setEnd(end);
    }

    private static boolean trimChar(final char aChar)
    {
        switch (aChar) {
        case '\n': // Line break
        case '\r': // Carriage return
        case '\t': // Tab
        case '\u2000': // EN QUAD
        case '\u2001': // EM QUAD
        case '\u2002': // EN SPACE
        case '\u2003': // EM SPACE
        case '\u2004': // THREE-PER-EM SPACE
        case '\u2005': // FOUR-PER-EM SPACE
        case '\u2006': // SIX-PER-EM SPACE
        case '\u2007': // FIGURE SPACE
        case '\u2008': // PUNCTUATION SPACE
        case '\u2009': // THIN SPACE
        case '\u200A': // HAIR SPACE
        case '\u200B': // ZERO WIDTH SPACE
        case '\u200C': // ZERO WIDTH NON-JOINER
        case '\u200D': // ZERO WIDTH JOINER
        case '\u200E': // LEFT-TO-RIGHT MARK
        case '\u200F': // RIGHT-TO-LEFT MARK
        case '\u2028': // LINE SEPARATOR
        case '\u2029': // PARAGRAPH SEPARATOR
        case '\u202A': // LEFT-TO-RIGHT EMBEDDING
        case '\u202B': // RIGHT-TO-LEFT EMBEDDING
        case '\u202C': // POP DIRECTIONAL FORMATTING
        case '\u202D': // LEFT-TO-RIGHT OVERRIDE
        case '\u202E': // RIGHT-TO-LEFT OVERRIDE
        case '\u202F': // NARROW NO-BREAK SPACE
        case '\u2060': // WORD JOINER
        case '\u2061': // FUNCTION APPLICATION
        case '\u2062': // INVISIBLE TIMES
        case '\u2063': // INVISIBLE SEPARATOR
        case '\u2064': // INVISIBLE PLUS
        case '\u2065': // <unassigned>
        case '\u2066': // LEFT-TO-RIGHT ISOLATE
        case '\u2067': // RIGHT-TO-LEFT ISOLATE
        case '\u2068': // FIRST STRONG ISOLATE
        case '\u2069': // POP DIRECTIONAL ISOLATE
        case '\u206A': // INHIBIT SYMMETRIC SWAPPING
        case '\u206B': // ACTIVATE SYMMETRIC SWAPPING
        case '\u206C': // INHIBIT ARABIC FORM SHAPING
        case '\u206D': // ACTIVATE ARABIC FORM SHAPING
        case '\u206E': // NATIONAL DIGIT SHAPES
        case '\u206F': // NOMINAL DIGIT SHAPES
        default:
            return Character.isWhitespace(aChar);
        }
    }
}
