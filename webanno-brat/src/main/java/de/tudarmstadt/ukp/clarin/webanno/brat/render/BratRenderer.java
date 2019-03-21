/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.brat.render;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VAnnotationMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSentenceMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VTextMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotationEditor;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.AnnotationComment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.AnnotationMarker;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Comment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.EntityType;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.RelationType;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.SentenceComment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.SentenceMarker;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.TextMarker;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Render documents using brat. This class converts a UIMA annotation representation into the 
 * object model used by brat. The result can be converted to JSON that the browser-side brat SVG
 * renderer can then use.
 */
public class BratRenderer
{
    private static final Logger LOG = LoggerFactory.getLogger(BratAnnotationEditor.class);
    
    private static final boolean DEBUG = false;
    
    public static void render(GetDocumentResponse aResponse, AnnotatorState aState,
            VDocument aVDoc, CAS aCas, AnnotationSchemaService aAnnotationService)
    {
        render(aResponse, aState, aVDoc, aCas, aAnnotationService, null);
    }
    
    /**
     * wrap JSON responses to BRAT visualizer
     *
     * @param aResponse
     *            the response.
     * @param aState
     *            the annotator model.
     * @param aCas
     *            the JCas.
     * @param aAnnotationService
     *            the annotation service.s
     */
    public static void render(GetDocumentResponse aResponse, AnnotatorState aState, VDocument aVDoc,
            CAS aCas, AnnotationSchemaService aAnnotationService,
            ColoringStrategy aColoringStrategy)
    {
        aResponse.setRtlMode(ScriptDirection.RTL.equals(aState.getScriptDirection()));
        aResponse.setFontZoom(aState.getPreferences().getFontZoom());

        // Render invisible baseline annotations (sentence, tokens)
        renderTokenAndSentence(aCas, aResponse, aState);
        
        // Render visible (custom) layers
        Map<String[], Queue<String>> colorQueues = new HashMap<>();
        for (AnnotationLayer layer : aAnnotationService.listAnnotationLayer(aState.getProject())) {
            ColoringStrategy coloringStrategy = aColoringStrategy != null ? aColoringStrategy
                    : ColoringStrategy.getStrategy(aAnnotationService, layer,
                            aState.getPreferences(), colorQueues);
            
            // If the layer is not included in the rendering, then we skip here - but only after
            // we have obtained a coloring strategy for this layer and thus secured the layer
            // color. This ensures that the layer colors do not change depending on the number
            // of visible layers.
            if (!aVDoc.getAnnotationLayers().contains(layer)) {
                continue;
            }

            TypeAdapter typeAdapter = aAnnotationService.getAdapter(layer);
            
            for (VSpan vspan : aVDoc.spans(layer.getId())) {
                List<Offsets> offsets = toOffsets(vspan.getRanges());
                String bratLabelText = TypeUtil.getUiLabelText(typeAdapter, vspan.getFeatures());
                String bratHoverText = TypeUtil.getUiHoverText(typeAdapter, 
                        vspan.getHoverFeatures());
                String color;
                if (vspan.getColorHint() == null) {
                    color = getColor(vspan, coloringStrategy, bratLabelText);
                } else {
                    color = vspan.getColorHint();
                }
                
                if (DEBUG) {
                    bratHoverText = vspan.getOffsets() + "\n" + bratHoverText;
                }
                
                aResponse.addEntity(new Entity(vspan.getVid(), vspan.getType(), offsets,
                        bratLabelText, color, bratHoverText));
            }

            for (VArc varc : aVDoc.arcs(layer.getId())) {
                String bratLabelText;
                if (varc.getLabelHint() == null) {
                    bratLabelText = TypeUtil.getUiLabelText(typeAdapter, varc.getFeatures());
                }
                else {
                    bratLabelText = varc.getLabelHint();
                }
                
                String color;
                if (varc.getColorHint() == null) {
                    color = getColor(varc, coloringStrategy, bratLabelText);
                } else {
                    color = varc.getColorHint();
                }
                aResponse.addRelation(new Relation(varc.getVid(), varc.getType(),
                        getArgument(varc.getSource(), varc.getTarget()), bratLabelText, color));
            }
        }
        
        List<AnnotationFS> sentences = new ArrayList<>(
                select(aCas, getType(aCas, Sentence.class)));
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
            if (
                    !vcomment.getVid().isSynthetic() && 
                    ((fs = selectAnnotationByAddr(aCas, vcomment.getVid().getId())) != null && 
                            fs.getType().getName().equals(Sentence.class.getName()))
            ) {
                int index = sentences.indexOf(fs) + 1;
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
    
    private static List<Offsets> toOffsets(List<VRange> aRanges)
    {
        return aRanges.stream().map(r -> new Offsets(r.getBegin(), r.getEnd()))
                .collect(Collectors.toList());
    }
    
    /**
     * Argument lists for the arc annotation
     */
    private static List<Argument> getArgument(VID aGovernorFs, VID aDependentFs)
    {
        return asList(new Argument("Arg1", aGovernorFs), new Argument("Arg2", aDependentFs));
    }
    
    public static void renderTokenAndSentence(CAS aCas, GetDocumentResponse aResponse,
            AnnotatorState aState)
    {
        int windowBegin = aState.getWindowBeginOffset();
        int windowEnd = aState.getWindowEndOffset();
        
        aResponse.setSentenceNumberOffset(aState.getFirstVisibleUnitIndex());

        Type tokenType = CasUtil.getType(aCas, Token.class);
        Type sentenceType = CasUtil.getType(aCas, Sentence.class);
        
        // Render token + texts
        for (AnnotationFS fs : selectCovered(aCas, tokenType, windowBegin, windowEnd)) {
            // attache type such as POS adds non existing token element for ellipsis annotation
            if (fs.getBegin() == fs.getEnd()) {
                continue;
            }
            aResponse.addToken(fs.getBegin() - windowBegin, fs.getEnd() - windowBegin);
            
            if (DEBUG) {
                aResponse.addEntity(new Entity(new VID(fs), "Token",
                        new Offsets(fs.getBegin() - windowBegin, fs.getEnd() - windowBegin),
                        fs.getCoveredText(), "#d9d9d9",
                        "[" + fs.getBegin() + "-" + fs.getEnd() + "]"));
            }
        }
        
        // Replace newline characters before sending to the client to avoid rendering glitches
        // in the client-side brat rendering code
        String visibleText = aCas.getDocumentText().substring(windowBegin, windowEnd);
        visibleText = StringUtils.replaceEachRepeatedly(visibleText, 
                new String[] { "\n", "\r" }, new String[] { " ", " " });
        aResponse.setText(visibleText);

        // Render Sentence
        int sentIdx = aResponse.getSentenceNumberOffset();
        for (AnnotationFS fs : selectCovered(aCas, sentenceType, windowBegin, windowEnd)) {
            aResponse.addSentence(fs.getBegin() - windowBegin, fs.getEnd()
                    - windowBegin);
            
            // If there is a sentence ID, then make it accessible to the user via a sentence-level
            // comment.
            String sentId = FSUtil.getFeature(fs, "id", String.class);
            if (isNotBlank(sentId)) {
                aResponse.addComment(new SentenceComment(sentIdx, Comment.ANNOTATOR_NOTES, 
                        String.format("Sentence ID: %s", sentId)));
            }

            sentIdx++;
        }
    }
    
    /**
     * Generates brat type definitions from the WebAnno layer definitions.
     *
     * @param aAnnotationLayers
     *            the layers
     * @param aAnnotationService
     *            the annotation service
     * @return the brat type definitions
     */
    public static Set<EntityType> buildEntityTypes(List<AnnotationLayer> aAnnotationLayers,
            AnnotationSchemaService aAnnotationService)
    {
        // Sort layers
        List<AnnotationLayer> layers = new ArrayList<>(aAnnotationLayers);
        layers.sort(Comparator.comparing(AnnotationLayer::getName));

        // Now build the actual configuration
        Set<EntityType> entityTypes = new LinkedHashSet<>();
        for (AnnotationLayer layer : layers) {
            EntityType entityType = configureEntityType(layer);

            List<RelationType> arcs = new ArrayList<>();
            
            // For link features, we also need to configure the arcs, even though there is no arc
            // layer here.
            boolean hasLinkFeatures = false;
            for (AnnotationFeature f : aAnnotationService.listAnnotationFeature(layer)) {
                if (!LinkMode.NONE.equals(f.getLinkMode())) {
                    hasLinkFeatures = true;
                    break;
                }
            }
            if (hasLinkFeatures) {
                String bratTypeName = getBratTypeName(layer);
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
            attachingLayers.add(aAnnotationService.getLayer(Dependency.class.getName(),
                    aTarget.getProject()));
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
        String bratTypeName = getBratTypeName(aLayer);
        return new EntityType(aLayer.getName(), aLayer.getUiName(), bratTypeName);
    }

    private static RelationType configureRelationType(AnnotationLayer aLayer,
            AnnotationLayer aAttachingLayer)
    {
        String attachingLayerBratTypeName = TypeUtil.getUiTypeName(aAttachingLayer);
        // FIXME this is a hack because the chain layer consists of two UIMA types, a "Chain"
        // and a "Link" type. ChainAdapter always seems to use "Chain" but some places also
        // still use "Link" - this should be cleaned up so that knowledge about "Chain" and
        // "Link" types is local to the ChainAdapter and not known outside it!
        if (aLayer.getType().equals(CHAIN_TYPE)) {
            attachingLayerBratTypeName += ChainAdapter.LINK;
        }

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

        String bratTypeName = getBratTypeName(aLayer);
        return new RelationType(aAttachingLayer.getName(), aAttachingLayer.getUiName(),
                attachingLayerBratTypeName, bratTypeName, null, arrowHead, dashArray);
    }

    private static String getBratTypeName(AnnotationLayer aLayer)
    {
        String bratTypeName = TypeUtil.getUiTypeName(aLayer);

        // FIXME this is a hack because the chain layer consists of two UIMA types, a "Chain"
        // and a "Link" type. ChainAdapter always seems to use "Chain" but some places also
        // still use "Link" - this should be cleaned up so that knowledge about "Chain" and
        // "Link" types is local to the ChainAdapter and not known outside it!
        if (aLayer.getType().equals(CHAIN_TYPE)) {
            bratTypeName += ChainAdapter.LINK;
        }
        return bratTypeName;
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
                ti ++;
            }
        }
        return abbr.toString();
    }
}
