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
import static de.tudarmstadt.ukp.inception.support.text.TrimUtils.trim;
import static java.lang.Character.isWhitespace;
import static java.lang.Character.toTitleCase;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.uima.cas.text.AnnotationPredicates.covering;
import static org.apache.uima.cas.text.AnnotationPredicates.overlappingAtBegin;
import static org.apache.uima.cas.text.AnnotationPredicates.overlappingAtEnd;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.lang.invoke.MethodHandles;
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

import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorProperties;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.AnnotationComment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.AnnotationMarker;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Comment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.SentenceComment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.SentenceMarker;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.TextMarker;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VAnnotationMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSentenceMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VTextMarker;
import de.tudarmstadt.ukp.inception.support.text.TextUtils;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

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
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
        var aResponse = new GetDocumentResponse();
        aResponse.setRtlMode(RTL == aRequest.getState().getScriptDirection());
        aResponse.setFontZoom(aRequest.getState().getPreferences().getFontZoom());
        aResponse.setWindowBegin(aVDoc.getWindowBegin());
        aResponse.setWindowEnd(aVDoc.getWindowEnd());

        renderText(aVDoc, aResponse, aRequest);

        // The rows need to be rendered first because we use the row boundaries to split
        // cross-row spans into multiple ranges
        renderBratRowsFromUnits(aResponse, aRequest);

        renderBratTokensFromText(aResponse, aRequest, aVDoc);

        renderLayers(aResponse, aVDoc);

        renderComments(aResponse, aVDoc, aRequest);

        renderMarkers(aResponse, aVDoc);

        return aResponse;
    }

    private void renderLayers(GetDocumentResponse aResponse, VDocument aVDoc)
    {
        for (var layer : aVDoc.getAnnotationLayers()) {
            for (var vspan : aVDoc.spans(layer.getId())) {
                var offsets = vspan.getRanges().stream() //
                        .flatMap(range -> split(aResponse.getSentenceOffsets(), aVDoc.getText(),
                                aVDoc.getWindowBegin(), range.getBegin(), range.getEnd()).stream())
                        .map(range -> {
                            var span = new int[] { range.getBegin(), range.getEnd() };
                            trim(aResponse.getText(), span);
                            range.setBegin(span[0]);
                            range.setEnd(span[1]);
                            return range;
                        }) //
                        .toList();

                var entity = new Entity(vspan.getVid(), getBratTypeName(vspan.getLayer()), offsets,
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
                entity.getAttributes().setScore(vspan.getScore());

                aResponse.addEntity(entity);
            }

            for (var varc : aVDoc.arcs(layer.getId())) {
                var labelHint = StringUtils.defaultIfBlank(varc.getLabelHint(),
                        "(" + layer.getUiName() + ")");
                var arc = new Relation(varc.getVid(), getBratTypeName(varc.getLayer()),
                        getArgument(varc.getSource(), varc.getTarget()), labelHint,
                        varc.getColorHint());
                aResponse.addRelation(arc);
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
        for (var vcomment : aVDoc.comments()) {
            var type = switch (vcomment.getCommentType()) {
            case ERROR -> AnnotationComment.ANNOTATION_ERROR;
            case INFO -> AnnotationComment.ANNOTATOR_NOTES;
            default -> AnnotationComment.ANNOTATOR_NOTES;
            };

            AnnotationFS fs;
            if (!vcomment.getVid().isSynthetic() && ((fs = ICasUtil.selectAnnotationByAddr(cas,
                    vcomment.getVid().getId())) != null
                    && fs.getType().getName().equals(Sentence.class.getName()))) {
                // Lazily fetching the sentences because we only need them for the comments
                if (sentenceIndexes == null) {
                    sentenceIndexes = new HashMap<>();
                    var i = 1;
                    for (var s : select(cas, getType(cas, Sentence.class))) {
                        sentenceIndexes.put(s, i);
                        i++;
                    }
                }

                var index = sentenceIndexes.get(fs);
                aResponse.addComment(
                        new SentenceComment(VID.of(fs), index, type, vcomment.getComment()));
            }
            else {
                aResponse.addComment(
                        new AnnotationComment(vcomment.getVid(), type, vcomment.getComment()));
            }
        }
    }

    private void renderMarkers(GetDocumentResponse aResponse, VDocument aVDoc)
    {
        for (var vmarker : aVDoc.getMarkers()) {
            if (vmarker instanceof VAnnotationMarker marker) {
                aResponse.addMarker(new AnnotationMarker(vmarker.getType(), marker.getVid()));
            }
            else if (vmarker instanceof VSentenceMarker marker) {
                aResponse.addMarker(new SentenceMarker(vmarker.getType(), marker.getIndex()));
            }
            else if (vmarker instanceof VTextMarker marker) {
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
        if (!aRequest.isIncludeText() || isEmpty(aVDoc.getText())) {
            return;
        }

        var visibleText = aVDoc.getText();
        char replacementChar = 0;
        if (isNotEmpty(properties.getWhiteSpaceReplacementCharacter())) {
            replacementChar = properties.getWhiteSpaceReplacementCharacter().charAt(0);
        }

        visibleText = TextUtils.sanitizeVisibleText(visibleText, replacementChar);
        aResponse.setText(visibleText);
    }

    private void renderBratTokensFromText(GetDocumentResponse aResponse, RenderRequest aRequest,
            VDocument aVDoc)
    {
        if (isEmpty(aVDoc.getText())) {
            return;
        }

        // Collect additional split points based on where two tokens are directly adjacent
        var extraSplits = new ArrayList<Integer>();
        if (aRequest.getCas() != null) {
            var tokenIterator = aRequest.getCas().select(Token.class) //
                    .coveredBy(aVDoc.getWindowBegin(), aVDoc.getWindowEnd()) //
                    .iterator();
            if (tokenIterator.hasNext()) {
                var prevToken = tokenIterator.next();
                while (tokenIterator.hasNext()) {
                    var token = tokenIterator.next();
                    if (prevToken.getEnd() == token.getBegin()) {
                        extraSplits.add(token.getBegin() - aVDoc.getWindowBegin());
                    }
                    prevToken = token;
                }
            }
        }

        var extraSplitIterator = extraSplits.listIterator();
        var bratTokenOffsets = new ArrayList<Offsets>();
        var visibleText = aVDoc.getText();
        var bi = BreakIterator.getWordInstance(Locale.ROOT);
        bi.setText(visibleText);
        var last = bi.first();
        var cur = bi.next();
        while (cur != BreakIterator.DONE) {
            var offsets = new int[] { last, cur };
            trim(visibleText, offsets);
            if (offsets[0] < offsets[1]) {

                // The idea here is that if somebody has created a token boundary inside a word, it
                // may look better if brat would be able to pull the word apart. This, however, only
                // works if for brat there is a token boundary at this location.
                while (extraSplitIterator.hasNext()) {
                    var candidateSplit = extraSplitIterator.next();
                    if (candidateSplit < offsets[0]) {
                        continue;
                    }

                    if (candidateSplit > offsets[1]) {
                        extraSplitIterator.previous();
                        break;
                    }

                    if (covering(offsets[0], offsets[1], candidateSplit, candidateSplit)) {
                        if (offsets[0] < candidateSplit) {
                            bratTokenOffsets.add(new Offsets(offsets[0], candidateSplit));
                        }
                        offsets[0] = candidateSplit;
                        continue;
                    }
                }

                if (offsets[0] < offsets[1]) {
                    bratTokenOffsets.add(new Offsets(offsets[0], offsets[1]));
                }
            }
            last = cur;
            cur = bi.next();
        }

        var rows = aResponse.getSentenceOffsets();
        for (var offsets : bratTokenOffsets) {
            var ranges = split(rows, visibleText, aVDoc.getWindowBegin(), offsets.getBegin(),
                    offsets.getEnd());
            for (var range : ranges) {
                aResponse.addToken(range.getBegin(), range.getEnd());
            }
        }
    }

    private void renderBratRowsFromUnits(GetDocumentResponse aResponse, RenderRequest aRequest)
    {
        var windowBegin = aRequest.getWindowBeginOffset();

        aResponse.setSentenceNumberOffset(aRequest.getState().getFirstVisibleUnitIndex());

        // Render sentences
        var unitNum = aResponse.getSentenceNumberOffset();
        for (var unit : aRequest.getState().getVisibleUnits()) {
            aResponse.addSentence(unit.getBegin() - windowBegin, unit.getEnd() - windowBegin);

            // If there is a sentence ID, then make it accessible to the user via a sentence-level
            // comment.
            if (isNotBlank(unit.getId())) {
                aResponse.addComment(new SentenceComment(unit.getVid(), unitNum,
                        Comment.ANNOTATOR_NOTES, String.format("Sentence ID: %s", unit.getId())));
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
    static List<Offsets> split(List<Offsets> aRows, String aText, int aWindowBegin, int aBegin,
            int aEnd)
    {
        // Zero-width spans never need to be split
        if (aBegin == aEnd) {
            return asList(new Offsets(aBegin, aEnd));
        }

        // If the annotation extends across the row boundaries, create multiple ranges for the
        // annotation, one for every row. Note that in UIMA annotations are half-open intervals
        // [begin,end) so that a begin offset must always be smaller than the end of a covering
        // annotation to be considered properly covered.
        var beginRow = aRows.stream() //
                .filter(row -> covering(row.getBegin(), row.getEnd(), aBegin, aBegin)) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException(
                        "Start position of range [" + (aWindowBegin + aBegin) + "-"
                                + (aWindowBegin + aEnd) + "] is not part of any visible row. In a "
                                + "sentence-based editor, this is most likely caused by "
                                + "annotations outside sentences."));

        // Zero-width annotations that are on the boundary of two directly adjacent sentences (i.e.
        // without whitespace between them) are considered to be at the end of the first sentence
        // rather than at the beginning of the second sentence.
        var endRow = aRows.stream() //
                .filter(row -> covering(row.getBegin(), row.getEnd(), aEnd, aEnd)) //
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

        var coveredRows = aRows.subList(aRows.indexOf(beginRow), aRows.indexOf(endRow) + 1);

        var segments = new ArrayList<Offsets>();
        for (var row : coveredRows) {
            int[] segment;

            if (covering(aBegin, aEnd, row.getBegin(), row.getEnd())) {
                segment = new int[] { row.getBegin(), row.getEnd() };
            }
            else if (overlappingAtBegin(row.getBegin(), row.getEnd(), aBegin, aEnd)) {
                segment = new int[] { aBegin, row.getEnd() };
            }
            else if (overlappingAtEnd(row.getBegin(), row.getEnd(), aBegin, aEnd)) {
                segment = new int[] { row.getBegin(), aEnd };
            }
            else {
                continue;
            }

            trim(aText, segment);

            var segmentOffsets = new Offsets(segment[0], segment[1]);
            if (!segmentOffsets.isEmpty()) {
                segments.add(segmentOffsets);
            }
        }

        return segments;
    }

    public static String abbreviate(String aName)
    {
        if (aName == null || aName.length() < 3) {
            return aName;
        }

        var abbr = new StringBuilder();
        var ti = 0;
        var capitalizeNext = true;
        for (int i = 0; i < aName.length(); i++) {
            int ch = aName.charAt(i);

            if (isWhitespace(ch)) {
                capitalizeNext = true;
                ti = 0;
            }
            else {
                if (ti < 3) {
                    if (capitalizeNext) {
                        ch = toTitleCase(ch);
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
}
