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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil.getUiLabelText;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRules;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRulesTrait;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.Unit;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VAnnotationMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSentenceMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VTextMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotationEditor;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorProperties;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.AnnotationComment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.AnnotationMarker;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Comment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.EntityType;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Normalization;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.RelationType;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.SentenceComment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.SentenceMarker;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.TextMarker;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Render documents using brat. This class converts a UIMA annotation representation into the object
 * model used by brat. The result can be converted to JSON that the browser-side brat SVG renderer
 * can then use.
 */
public class BratRenderer
{
    private static final Logger LOG = LoggerFactory.getLogger(BratAnnotationEditor.class);

    private static final boolean DEBUG = false;

    private final AnnotationSchemaService schemaService;
    private final ColoringService coloringService;
    private final BratAnnotationEditorProperties properties;

    public BratRenderer(AnnotationSchemaService aSchemaService, ColoringService aColoringService,
            BratAnnotationEditorProperties aProperties)
    {
        schemaService = aSchemaService;
        coloringService = aColoringService;
        properties = aProperties;
    }

    public void render(GetDocumentResponse aResponse, AnnotatorState aState, VDocument aVDoc,
            CAS aCas)
    {
        render(aResponse, aState, aVDoc, aCas, null);
    }

    /**
     * Convert the visual representation to the brat representation.
     *
     * @param aResponse
     *            the response.
     * @param aState
     *            the annotator model.
     * @param aCas
     *            the CAS.
     */
    public void render(GetDocumentResponse aResponse, AnnotatorState aState, VDocument aVDoc,
            CAS aCas, ColoringStrategy aColoringStrategy)
    {
        aResponse.setRtlMode(ScriptDirection.RTL.equals(aState.getScriptDirection()));
        aResponse.setFontZoom(aState.getPreferences().getFontZoom());

        // Render invisible baseline annotations (sentence, tokens)
        renderText(aCas, aResponse, aState);
        // The rows need to be rendered first because we use the row boundaries to split
        // cross-row spans into multiple ranges
        renderUnitsAsRows(aResponse, aState);

        renderTokens(aCas, aResponse, aState);

        Map<AnnotationFS, Integer> sentenceIndexes = null;

        // Render visible (custom) layers
        Map<String[], Queue<String>> colorQueues = new HashMap<>();
        for (AnnotationLayer layer : aState.getAllAnnotationLayers()) {
            ColoringStrategy coloringStrategy = aColoringStrategy != null ? aColoringStrategy
                    : coloringService.getStrategy(layer, aState.getPreferences(), colorQueues);

            // If the layer is not included in the rendering, then we skip here - but only after
            // we have obtained a coloring strategy for this layer and thus secured the layer
            // color. This ensures that the layer colors do not change depending on the number
            // of visible layers.
            if (!aVDoc.getAnnotationLayers().contains(layer)) {
                continue;
            }

            TypeAdapter typeAdapter = schemaService.getAdapter(layer);

            ColoringRules coloringRules = typeAdapter.getTraits(ColoringRulesTrait.class)
                    .map(ColoringRulesTrait::getColoringRules).orElse(null);

            for (VSpan vspan : aVDoc.spans(layer.getId())) {
                List<Offsets> offsets = vspan.getRanges().stream()
                        .flatMap(range -> split(aResponse.getSentenceOffsets(),
                                aCas.getDocumentText().substring(aState.getWindowBeginOffset(),
                                        aState.getWindowEndOffset()),
                                range.getBegin(), range.getEnd()).stream())
                        .collect(toList());

                String labelText = getUiLabelText(typeAdapter, vspan);

                String color = coloringStrategy.getColor(vspan, labelText, coloringRules);

                Entity entity = new Entity(vspan.getVid(), vspan.getType(), offsets, labelText,
                        color);
                if (!layer.isShowTextInHover()) {
                    // If the layer is configured not to display the span text in the popup, then
                    // we simply set the popup to the empty string here.
                    // FIXME: This is obviously not optimal because it is a layer-level setting and
                    // we send it at the annotation level here. It would be better to send this
                    // along with the layer configuration (would save 4-5 characters in the server
                    // response for *every annotation on this layer*!)
                    entity.getAttributes().setHoverText("");
                }

                aResponse.addEntity(new Entity(vspan.getVid(), vspan.getType(), offsets, labelText,
                        color, vspan.isActionButtons()));

                vspan.getLazyDetails().stream()
                        .map(d -> new Normalization(vspan.getVid(), d.getFeature(), d.getQuery()))
                        .forEach(aResponse::addNormalization);
            }

            for (VArc varc : aVDoc.arcs(layer.getId())) {
                String bratLabelText = getUiLabelText(typeAdapter, varc);
                String color = coloringStrategy.getColor(varc, bratLabelText, coloringRules);

                Relation arc = new Relation(varc.getVid(), varc.getType(),
                        getArgument(varc.getSource(), varc.getTarget()), bratLabelText, color);
                aResponse.addRelation(arc);

                varc.getLazyDetails().stream()
                        .map(d -> new Normalization(varc.getVid(), d.getFeature(), d.getQuery()))
                        .forEach(aResponse::addNormalization);
            }
        }

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
            if (!vcomment.getVid().isSynthetic()
                    && ((fs = selectAnnotationByAddr(aCas, vcomment.getVid().getId())) != null
                            && fs.getType().getName().equals(Sentence.class.getName()))) {
                // Lazily fetching the sentences because we only need them for the comments
                if (sentenceIndexes == null) {
                    sentenceIndexes = new HashMap<>();
                    int i = 1;
                    for (AnnotationFS s : select(aCas, getType(aCas, Sentence.class))) {
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
                VTextMarker marker = (VTextMarker) vmarker;
                aResponse.addMarker(
                        new TextMarker(marker.getType(), marker.getBegin(), marker.getEnd()));
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

    public void renderText(CAS aCas, GetDocumentResponse aResponse, AnnotatorState aState)
    {
        int windowBegin = aState.getWindowBeginOffset();
        int windowEnd = aState.getWindowEndOffset();

        String visibleText = aCas.getDocumentText().substring(windowBegin, windowEnd);
        visibleText = sanitizeVisibleText(visibleText);
        aResponse.setText(visibleText);
    }

    public static void renderTokens(CAS aCas, GetDocumentResponse aResponse, AnnotatorState aState)
    {
        int winBegin = aState.getWindowBeginOffset();
        int winEnd = aState.getWindowEndOffset();
        Type tokenType = CasUtil.getType(aCas, Token.class);

        List<AnnotationFS> tokens = selectCovered(aCas, tokenType, winBegin, winEnd);
        for (AnnotationFS fs : tokens) {
            // attach type such as POS adds non-existing token element for ellipsis annotation
            if (fs.getBegin() == fs.getEnd()) {
                continue;
            }

            split(aResponse.getSentenceOffsets(), fs.getCoveredText(), fs.getBegin() - winBegin,
                    fs.getEnd() - winBegin).forEach(range -> {
                        aResponse.addToken(range.getBegin(), range.getEnd());
                        if (DEBUG) {
                            aResponse.addEntity(new Entity(new VID(fs), "Token",
                                    new Offsets(range.getBegin(), range.getEnd()),
                                    fs.getCoveredText(), "#d9d9d9"));
                        }
                    });
        }
    }

    public static void renderUnitsAsRows(GetDocumentResponse aResponse, AnnotatorState aState)
    {
        int windowBegin = aState.getWindowBeginOffset();

        aResponse.setSentenceNumberOffset(aState.getFirstVisibleUnitIndex());

        // Render sentences
        int sentIdx = aResponse.getSentenceNumberOffset();
        for (Unit unit : aState.getVisibleUnits()) {
            aResponse.addSentence(unit.getBegin() - windowBegin, unit.getEnd() - windowBegin);

            // If there is a sentence ID, then make it accessible to the user via a sentence-level
            // comment.
            if (isNotBlank(unit.getId())) {
                aResponse.addComment(new SentenceComment(sentIdx, Comment.ANNOTATOR_NOTES,
                        String.format("Sentence ID: %s", unit.getId())));
            }

            sentIdx++;
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
    public static List<Offsets> split(List<Offsets> aRows, String aText, int aBegin, int aEnd)
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
                .filter(span -> span.getBegin() <= aBegin && aBegin < span.getEnd()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Position [" + aBegin + "] is not in any row"));

        // Zero-width annotations that are on the boundary of two directly
        // adjacent sentences (i.e. without whitespace between them) are considered
        // to be at the end of the first sentence rather than at the beginning of the
        // second sentence.
        Offsets endRow = aRows.stream()
                .filter(span -> span.getBegin() <= aEnd && aEnd <= span.getEnd()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Position [" + aEnd + "] is not in any row"));

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

    /**
     * Generates brat type definitions from the WebAnno layer definitions.
     *
     * @param aProject
     *            the project to which the layers belong
     * @param aAnnotationLayers
     *            the layers
     * @param aAnnotationService
     *            the annotation service
     * @return the brat type definitions
     */
    public static Set<EntityType> buildEntityTypes(Project aProject,
            List<AnnotationLayer> aAnnotationLayers, AnnotationSchemaService aAnnotationService)
    {
        // Sort layers
        List<AnnotationLayer> layers = new ArrayList<>(aAnnotationLayers);
        layers.sort(Comparator.comparing(AnnotationLayer::getName));

        // Look up all the features once to avoid hammering the database in the loop below
        Map<AnnotationLayer, List<AnnotationFeature>> layerToFeatures = aAnnotationService
                .listSupportedFeatures(aProject).stream()
                .collect(groupingBy(AnnotationFeature::getLayer));

        // Now build the actual configuration
        Set<EntityType> entityTypes = new LinkedHashSet<>();
        for (AnnotationLayer layer : layers) {
            EntityType entityType = configureEntityType(layer);

            List<RelationType> arcs = new ArrayList<>();

            // For link features, we also need to configure the arcs, even though there is no arc
            // layer here.
            boolean hasLinkFeatures = false;
            for (AnnotationFeature f : layerToFeatures.computeIfAbsent(layer, k -> emptyList())) {
                if (!LinkMode.NONE.equals(f.getLinkMode())) {
                    hasLinkFeatures = true;
                    break;
                }
            }

            if (hasLinkFeatures) {
                String bratTypeName = TypeUtil.getUiTypeName(layer);
                arcs.add(new RelationType(layer.getName(), layer.getUiName(), bratTypeName,
                        bratTypeName, null, "triangle,5", "3,3"));
            }

            // Styles for the remaining relation and chain layers
            for (AnnotationLayer attachingLayer : getAttachingLayers(layer, layers,
                    aAnnotationService)) {
                arcs.add(configureRelationType(layer, attachingLayer));
            }

            entityType.setArcs(arcs);
            entityTypes.add(entityType);
        }

        return entityTypes;
    }

    /**
     * Scan through the layers once to remember which layers attach to which layers.
     */
    private static List<AnnotationLayer> getAttachingLayers(AnnotationLayer aTarget,
            List<AnnotationLayer> aLayers, AnnotationSchemaService aAnnotationService)
    {
        List<AnnotationLayer> attachingLayers = new ArrayList<>();

        // Chains always attach to themselves
        if (CHAIN_TYPE.equals(aTarget.getType())) {
            attachingLayers.add(aTarget);
        }

        // FIXME This is a hack! Actually we should check the type of the attachFeature when
        // determine which layers attach to with other layers. Currently we only use attachType,
        // but do not follow attachFeature if it is set.
        if (aTarget.isBuiltIn() && aTarget.getName().equals(POS.class.getName())) {
            attachingLayers.add(
                    aAnnotationService.findLayer(aTarget.getProject(), Dependency.class.getName()));
        }

        // Custom layers
        for (AnnotationLayer l : aLayers) {
            if (aTarget.equals(l.getAttachType())) {
                attachingLayers.add(l);
            }
        }

        return attachingLayers;
    }

    private static EntityType configureEntityType(AnnotationLayer aLayer)
    {
        String bratTypeName = TypeUtil.getUiTypeName(aLayer);
        return new EntityType(aLayer.getName(), aLayer.getUiName(), bratTypeName);
    }

    private static RelationType configureRelationType(AnnotationLayer aLayer,
            AnnotationLayer aAttachingLayer)
    {
        String attachingLayerBratTypeName = TypeUtil.getUiTypeName(aAttachingLayer);

        // // FIXME this is a hack because the chain layer consists of two UIMA types, a "Chain"
        // // and a "Link" type. ChainAdapter always seems to use "Chain" but some places also
        // // still use "Link" - this should be cleaned up so that knowledge about "Chain" and
        // // "Link" types is local to the ChainAdapter and not known outside it!
        // if (aLayer.getType().equals(CHAIN_TYPE)) {
        // attachingLayerBratTypeName += ChainAdapter.LINK;
        // }

        // Handle arrow-head styles depending on linkedListBehavior
        String arrowHead;
        if (aLayer.getType().equals(CHAIN_TYPE) && !aLayer.isLinkedListBehavior()) {
            arrowHead = "none";
        }
        else {
            arrowHead = "triangle,5";
        }

        String dashArray;
        switch (aLayer.getType()) {
        case CHAIN_TYPE:
            dashArray = "5,1";
            break;
        default:
            dashArray = "";
            break;
        }

        String bratTypeName = TypeUtil.getUiTypeName(aLayer);
        return new RelationType(aAttachingLayer.getName(), aAttachingLayer.getUiName(),
                attachingLayerBratTypeName, bratTypeName, null, arrowHead, dashArray);
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

    private String sanitizeVisibleText(String aText)
    {
        // NBSP is recognized by Firefox as a proper addressable character in
        // SVGText.getNumberOfChars()
        char whiteplaceReplacementChar = '\u00A0'; // NBSP
        if (isNotEmpty(properties.getWhiteSpaceReplacementCharacter())) {
            whiteplaceReplacementChar = properties.getWhiteSpaceReplacementCharacter().charAt(0);
        }

        char[] chars = aText.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
            // Replace newline characters before sending to the client to avoid
            // rendering glitches in the client-side brat rendering code
            case '\n':
            case '\r':
                chars[i] = ' ';
                break;
            // Some browsers (e.g. Firefox) do not count invisible chars in some functions
            // (e.g. SVGText.getNumberOfChars()) and this causes trouble. See:
            //
            // - https://github.com/webanno/webanno/issues/307
            // - https://github.com/inception-project/inception/issues/1849
            //
            // To avoid this, we replace the chars with a visible whitespace character before
            // sending the data to the browser. Hopefully this makes sense.
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
                chars[i] = whiteplaceReplacementChar;
                break;
            default:
                // Nothing to do
            }
        }

        return new String(chars);
    }
}
