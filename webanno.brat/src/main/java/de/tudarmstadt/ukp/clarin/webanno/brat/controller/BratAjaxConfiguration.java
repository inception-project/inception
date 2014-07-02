/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst.COREFERENCE_TYPE_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst.RELATION_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.EntityType;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.RelationType;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 *
 * Configuring The brat JSON collection information responses getting tags from DB
 *
 * @author Seid Muhie Yimam
 *
 */
public class BratAjaxConfiguration
{
    private static final String AMAZON = "#3B7A57";
    private static final String NO_COLOR = "";
    private static final String CHOCOLATE = "chocolate";
    private static final String BLUEVIOLET = "blueviolet";
    private static final String MAROON = "maroon";
    private static final String NAVY = "navy";
    private static final String FUCHSIA = "fuchsia";
    private static final String ORANGE = "orange";
    private static final String MAGENTA = "magenta";
    private static final String DARKGREEN = "darkgreen";
    private static final String DARKORCHID = "darkorchid";
    private static final String BROWN = "brown";
    private static final String LIGHTGREEN = "#4B5320";
    private static final String SIENNA = "Sienna ";
    private static final String GOLD = "gold";
    private static final String DEEPPINK = "deeppink";
    private static final String DEEPSKYBLUE = "deepskyblue";
    private static final String DARKORANGE = "darkorange";
    private static final String CORAL = "coral";
    private static final String PINK = "pink";
    private static final String BLACK = "black";
    private static final String CYAN = "cyan";
    private static final String RED = "red";
    private static final String YELLOW = "yellow";
    private static final String BLUE = "blue";
    private static final String GREEN = "green";

    private final List<String> fGColors = Arrays.asList(YELLOW, RED, PINK, BLACK, CORAL,
            DARKORANGE, DEEPSKYBLUE, FUCHSIA, NAVY, MAROON);
    private final List<String> bGColors = Arrays.asList(MAGENTA, LIGHTGREEN, BROWN, SIENNA,
            DARKGREEN, DARKORCHID, DEEPPINK, GOLD, GREEN, ORANGE);
    private final List<String> bDColors = Arrays.asList(AMAZON, BLUEVIOLET, CHOCOLATE, CYAN,
            DARKGREEN, DARKORCHID, DEEPPINK, GOLD, MAGENTA, ORANGE);

    /**
     * Get all the tags from the database, differentiate as pos,dependency, named entity... and
     * build the entity/relation types accordingly.
     * {@link de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS} is an {@link EntityType}
     * where {@link Dependency} is a child {@link EntityType} ({@code arc}). {@link NamedEntity} is
     * an {@link EntityType} without a child (no {@code arc}). {@link CoreferenceChain} is
     * {@link EntityType} with a child {@link CoreferenceLink}s.
     *
     * @return {@link Set<{@link EntityType }>}
     */
    public Set<EntityType> buildEntityTypes(List<AnnotationLayer> aLayers,
            AnnotationService aAnnotationService, boolean aStaticColor)
    {
        // Scan through the layers once to remember which layers attach to which layers
        Map<AnnotationLayer, AnnotationLayer> attachingLayers = new LinkedHashMap<AnnotationLayer, AnnotationLayer>();
        for (AnnotationLayer layer : aLayers) {
            if (layer.getType().equals(CHAIN_TYPE)) {
                attachingLayers.put(layer, layer);
            }
            else if (layer.getType().equals(RELATION_TYPE)) {
                // FIXME This implies that at most one relation layer can attach to a span layer
                attachingLayers.put(layer.getAttachType(), layer);
            }
        }

        // Now build the actual configuration
        Set<EntityType> entityTypes = new LinkedHashSet<EntityType>();
        int i = 0;
        for (AnnotationLayer layer : aLayers) {
            i = configCollection(aAnnotationService, aStaticColor, entityTypes, i, layer,
                    attachingLayers.get(layer));
        }

        return entityTypes;
    }

    private int configCollection(AnnotationService aAnnotationService, boolean aStaticColor,
            Set<EntityType> entityTypes, int i, AnnotationLayer aSpanLayer,
            AnnotationLayer aRelationLayer)
    {
        if (i == bGColors.size()) {
            i = 0;// recycle
        }
        List<List<String>> spanTags = new ArrayList<List<String>>();
        List<List<String>> relationTags = new ArrayList<List<String>>();

        for (AnnotationFeature feature : aAnnotationService.listAnnotationFeature(aSpanLayer)) {
            if (!(feature.isEnabled() || feature.isEnabled())) {
                continue;
            }
            List<String> tags = new ArrayList<String>();
            if (feature.getName().equals(COREFERENCE_TYPE_FEATURE) && feature.getTagset() != null) {
                for (Tag tag : aAnnotationService.listTags(feature.getTagset())) {
                    tags.add(tag.getName());
                }
                spanTags.add(tags);
            }
            else if (feature.getName().equals(COREFERENCE_RELATION_FEATURE)
                    && feature.getTagset() != null) {
                for (Tag tag : aAnnotationService.listTags(feature.getTagset())) {
                    tags.add(tag.getName());
                }
                relationTags.add(tags);
            }
            else if (feature.getTagset() != null) {
                for (Tag tag : aAnnotationService.listTags(feature.getTagset())) {
                    tags.add(tag.getName());
                }
                spanTags.add(tags);
            }
        }

        if (aSpanLayer.isBuiltIn() && aSpanLayer.getName().equals(POS.class.getName())) {
            aRelationLayer = aAnnotationService.getLayer(Dependency.class.getName(),
                    aSpanLayer.getProject());
        }

        if (aRelationLayer != null && !aRelationLayer.getType().equals(CHAIN_TYPE)) {
            for (AnnotationFeature feature : aAnnotationService
                    .listAnnotationFeature(aRelationLayer)) {
                if (!(feature.isEnabled() || feature.isEnabled())) {
                    continue;
                }
                List<String> tags = new ArrayList<String>();
                if (feature.getTagset() != null) {
                    for (Tag tag : aAnnotationService.listTags(feature.getTagset())) {
                        tags.add(tag.getName());
                    }
                    relationTags.add(tags);
                }
            }
        }

        EntityType entityType;
        List<EntityType> tagLists;
        if (aSpanLayer.isBuiltIn() && aSpanLayer.getName().equals(POS.class.getName())) {
            tagLists = getChildren(aSpanLayer, aRelationLayer.getId() + "_",
                    concatTagsPerFeature(spanTags), concatTagsPerFeature(relationTags), "red",
                    "yellow", "blue", "green", aStaticColor);

            entityType = new EntityType(aSpanLayer.getId() + "", aSpanLayer.getId() + "", true, "",
                    "red", "blue", "blue", new ArrayList<String>(), tagLists,
                    new ArrayList<String>(), new ArrayList<RelationType>(), aStaticColor);

        }
        else if (aSpanLayer.isBuiltIn() && aSpanLayer.getName().equals(NamedEntity.class.getName())) {

            tagLists = getChildren(aSpanLayer, aRelationLayer == null ? "" : aRelationLayer.getId()
                    + "_", concatTagsPerFeature(spanTags), concatTagsPerFeature(relationTags),
                    "black", "cyan", "green", "", aStaticColor);

            entityType = new EntityType(aSpanLayer.getId() + "", aSpanLayer.getId() + "", true, "",
                    "black", "cyan", "green", new ArrayList<String>(), tagLists,
                    new ArrayList<String>(), new ArrayList<RelationType>(), aStaticColor);

        }
        else if (aSpanLayer.isBuiltIn() && aSpanLayer.getName().equals(Lemma.class.getName())) {

            tagLists = getChildren(aSpanLayer, aRelationLayer == null ? "" : aRelationLayer.getId()
                    + "_", concatTagsPerFeature(spanTags), concatTagsPerFeature(relationTags), "",
                    "", "", "", aStaticColor);

            entityType = new EntityType(aSpanLayer.getId() + "", aSpanLayer.getId() + "", true, "",
                    "", "", "", new ArrayList<String>(), tagLists, new ArrayList<String>(),
                    new ArrayList<RelationType>(), aStaticColor);

        }

        // custom layers
        else {
            tagLists = getChildren(aSpanLayer, aRelationLayer == null ? "" : aRelationLayer.getId()
                    + "_", concatTagsPerFeature(spanTags), concatTagsPerFeature(relationTags),
                    fGColors.get(i), bGColors.get(i), bDColors.get(i), bDColors.get(i),
                    aStaticColor);

            entityType = new EntityType(aSpanLayer.getId() + "", aSpanLayer.getId() + "", true, "",
                    fGColors.get(i), bGColors.get(i), bDColors.get(i), new ArrayList<String>(),
                    tagLists, new ArrayList<String>(), new ArrayList<RelationType>(), aStaticColor);
            i++;
        }
        if (tagLists.size() > 0) {
            entityTypes.add(entityType);
        }
        return i;
    }

    /**
     * returns {@link EntityType} which will be used as a child {@link EntityType} which are
     * properties/labels of an arc
     *
     * @param spansList
     *            list of possible {@code spans } to be used as {@code targets}.
     * @param aArcList
     *            all {@code arc labels} used as label on the {@code arc}
     * @param aFgColor
     *            foreground color of the arc
     * @param aBgcolor
     *            background color of the arc
     * @param aBorderColoer
     *            border color of the arc
     * @return {@link List< {@link EntityType }>}
     */
    private List<EntityType> getChildren(AnnotationLayer aLayer, String aChildPrefix,
            List<String> spansList, List<String> aArcList, String aFgColor, String aBgcolor,
            String aBorderColoer, String aArcColor, boolean aStaticColor)
    {
        List<EntityType> children = new ArrayList<EntityType>();

        String prefix = aLayer.getId() + "_";
        List<String> arcTargets = new ArrayList<String>();
        for (String span : spansList) {
            arcTargets.add(prefix + span);
        }
        for (String span : spansList) {
            List<RelationType> arcs = new ArrayList<RelationType>();
            Iterator<String> arcTypesResultIterator = aArcList.iterator();
            while (arcTypesResultIterator.hasNext()) {
                String arcLabels = arcTypesResultIterator.next();

                // 12 classes of colors to differentiate Co-reference chains
                if (aLayer.getType().equals("chain")) {

                    String[] colors = new String[] { "#00FF00", "#0000A0", "#FF0000", "#800080 ",
                            "#F000FF", "#00FFFF ", "#FF00FF ", "#8D38C9", "#8D38C9", "#736AFF",
                            "#C11B17", "#800000" };
                    int i = 1;
                    for (String color : colors) {
                        RelationType arc = new RelationType(color, "none",
                                Arrays.asList(arcLabels), i + "_" + aChildPrefix + arcLabels,
                                arcTargets, "");
                        arcs.add(arc);
                        i++;
                    }
                }
                else {
                    RelationType arc = new RelationType(aArcColor, "triangle,5",
                            Arrays.asList(arcLabels), aChildPrefix + arcLabels, arcTargets, "");
                    arcs.add(arc);
                }
            }

            EntityType entityTpe = new EntityType(span, prefix + span, false, "", aFgColor,
                    aBgcolor, aBorderColoer, Arrays.asList(span), new ArrayList<EntityType>(),
                    new ArrayList<String>(), arcs, aStaticColor);
            children.add(entityTpe);
        }
        return children;
    }

    private List<String> concatTagsPerFeature(List<List<String>> list)
    {
        ArrayList<String> allTags = new ArrayList<String>();
        int len = list.size();
        if (len == 0) {
            return allTags;
        }
        if (len == 1) {
            allTags.addAll(list.get(0));
            return allTags;
        }
        List<String> thisTags = list.get(0);
        List<List<String>> tags = new ArrayList<List<String>>();
        tags.addAll(list.subList(1, len));
        List<String> retTags = concatTagsPerFeature(tags);
        for (String tag : thisTags) {
            for (String thisTag : retTags) {
                allTags.add(tag + " | " + thisTag);
                allTags.add(tag + " | " + " ");
                allTags.add(" " + " | " + thisTag);
                allTags.add(" " + " | " + " ");
            }
        }
        return allTags;
    }

    // private int configCollection2(AnnotationService aAnnotationService, boolean aStaticColor,
    // Set<EntityType> aEntityTypes, int i, AnnotationLayer aLayer,
    // AnnotationLayer aAttachingLayer)
    // {
    // if (i == bGColors.size()) {
    // i = 0;// recycle
    // }
    // // List<List<String>> spanTags = new ArrayList<List<String>>();
    // // List<List<String>> relationTags = new ArrayList<List<String>>();
    // //
    // // for (AnnotationFeature feature : aAnnotationService.listAnnotationFeature(aLayer)) {
    // // if (!feature.isEnabled()) {
    // // continue;
    // // }
    // // List<String> tags = new ArrayList<String>();
    // // if (feature.getName().equals(COREFERENCE_TYPE_FEATURE) && feature.getTagset() != null) {
    // // for (Tag tag : aAnnotationService.listTags(feature.getTagset())) {
    // // tags.add(tag.getName());
    // // }
    // // spanTags.add(tags);
    // // }
    // // else if (feature.getName().equals(COREFERENCE_RELATION_FEATURE)
    // // && feature.getTagset() != null) {
    // // for (Tag tag : aAnnotationService.listTags(feature.getTagset())) {
    // // tags.add(tag.getName());
    // // }
    // // relationTags.add(tags);
    // // }
    // // else if (feature.getTagset() != null) {
    // // for (Tag tag : aAnnotationService.listTags(feature.getTagset())) {
    // // tags.add(tag.getName());
    // // }
    // // spanTags.add(tags);
    // // }
    // // }
    // //
    // // FIXME This is a hack! Actually we should check the type of the attachFeature when
    // // determine which layers attach to with other layers. Currently we only use attachType,
    // // but do not follow attachFeature if it is set.
    // if (aLayer.isBuiltIn() && aLayer.getName().equals(POS.class.getName())) {
    // aAttachingLayer = aAnnotationService.getLayer(Dependency.class.getName(), RELATION_TYPE,
    // aLayer.getProject());
    // }
    // //
    // // if (aAttachingLayer.getId() != 0 && !aAttachingLayer.getType().equals(CHAIN_TYPE)) {
    // // for (AnnotationFeature feature :
    // aAnnotationService.listAnnotationFeature(aAttachingLayer)) {
    // // if (!feature.isEnabled()) {
    // // continue;
    // // }
    // // List<String> tags = new ArrayList<String>();
    // // if (feature.getTagset() != null) {
    // // for (Tag tag : aAnnotationService.listTags(feature.getTagset())) {
    // // tags.add(tag.getName());
    // // }
    // // relationTags.add(tags);
    // // }
    // // }
    // // }
    //
    // String bratTypeName = aLayer.getId() + "_" + aLayer.getName();
    //
    // EntityType entityType;
    // // List<EntityType> tagLists = new ArrayList<EntityType>();
    // if (aLayer.isBuiltIn() && aLayer.getName().equals(POS.class.getName())) {
    // // tagLists = getChildren(aSpanLayer, aRelationLayer.getId() + "_",
    // // concatTagsPerFeature(spanTags), concatTagsPerFeature(relationTags), RED,
    // // YELLOW, BLUE, GREEN, aStaticColor);
    // //
    // // entityType = new EntityType(aSpanLayer.getId() + "", aSpanLayer.getId() + "", true, "",
    // // RED, BLUE, BLUE, new ArrayList<String>(), tagLists,
    // // new ArrayList<String>(), new ArrayList<RelationType>(), aStaticColor);
    // entityType = new EntityType(aLayer.getName(), bratTypeName, true, "",
    // RED, BLUE, BLUE, null /* labels */, null /* children */, null /* attributes */,
    // null /* arcs */, aStaticColor);
    // }
    // else if (aLayer.isBuiltIn() && aLayer.getName().equals(NamedEntity.class.getName())) {
    //
    // // tagLists = getChildren(aSpanLayer,
    // // aRelationLayer.getId() == 0 ? "" : aRelationLayer.getId() + "_",
    // // concatTagsPerFeature(spanTags), concatTagsPerFeature(relationTags), BLACK,
    // // CYAN, GREEN, "", aStaticColor);
    // //
    // // entityType = new EntityType(aSpanLayer.getId() + "", aSpanLayer.getId() + "", true, "",
    // // BLACK, CYAN, GREEN, new ArrayList<String>(), tagLists,
    // // new ArrayList<String>(), new ArrayList<RelationType>(), aStaticColor);
    //
    // entityType = new EntityType(aLayer.getName(), bratTypeName, true, "",
    // BLACK, CYAN, GREEN, null /* labels */, null /* children */, null /* attributes */,
    // null /* arcs */, aStaticColor);
    // }
    // else if (aLayer.isBuiltIn() && aLayer.getName().equals(Lemma.class.getName())) {
    //
    // // tagLists = getChildren(aSpanLayer,
    // // aRelationLayer.getId() == 0 ? "" : aRelationLayer.getId() + "_",
    // // concatTagsPerFeature(spanTags), concatTagsPerFeature(relationTags), "", "", "",
    // // "", aStaticColor);
    // //
    // // entityType = new EntityType(aSpanLayer.getId() + "", aSpanLayer.getId() + "", true, "",
    // // "", "", "", new ArrayList<String>(), tagLists, new ArrayList<String>(),
    // // new ArrayList<RelationType>(), aStaticColor);
    //
    // entityType = new EntityType(aLayer.getName(), bratTypeName, true, "",
    // NO_COLOR, NO_COLOR, NO_COLOR, null /* labels */, null /* children */,
    // null /* attributes */, null /* arcs */, aStaticColor);
    // }
    //
    // // custom layers
    // else {
    // // tagLists = getChildren(aSpanLayer,
    // // aRelationLayer.getId() == 0 ? "" : aRelationLayer.getId() + "_",
    // // concatTagsPerFeature(spanTags), concatTagsPerFeature(relationTags),
    // // fGColors.get(i), bGColors.get(i), bDColors.get(i), bDColors.get(i),
    // // aStaticColor);
    // //
    // // entityType = new EntityType(aSpanLayer.getId() + "", aSpanLayer.getId() + "", true, "",
    // // fGColors.get(i), bGColors.get(i), bDColors.get(i), new ArrayList<String>(),
    // // tagLists, new ArrayList<String>(), new ArrayList<RelationType>(), aStaticColor);
    //
    // entityType = new EntityType(aLayer.getName(), bratTypeName, true, "",
    // fGColors.get(i), bGColors.get(i), bDColors.get(i), null /* labels */,
    // null /* children */, null /* attributes */, null /* arcs */, aStaticColor);
    // i++;
    // }
    //
    // if (aAttachingLayer != null) {
    // String attachingLayerBratTypeName = aAttachingLayer.getId() + "_"
    // + aAttachingLayer.getName();
    // List<RelationType> arcs = new ArrayList<RelationType>();
    // RelationType arc = new RelationType(bDColors.get(i), "triangle,5", null /* labels */,
    // attachingLayerBratTypeName, asList(bratTypeName), "");
    // arcs.add(arc);
    // entityType.setArcs(arcs);
    // }
    //
    // // if (entityType.getChildren() == null || tagLists.size() > 0) {
    // aEntityTypes.add(entityType);
    // // }
    // return i;
    // }
}
