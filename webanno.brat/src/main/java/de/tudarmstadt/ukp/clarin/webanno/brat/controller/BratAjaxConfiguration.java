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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst.*;

/**
 *
 * Configuring The brat JSON collection information responses getting tags from DB
 *
 * @author Seid Muhie Yimam
 *
 */
public class BratAjaxConfiguration
{

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
    public Set<EntityType> configureVisualizationAndAnnotation(List<AnnotationLayer> aLayers,
            AnnotationService aAnnotationService, boolean aStaticColor)
    {
        Set<EntityType> entityTypes = new HashSet<EntityType>();

        Map<AnnotationLayer, Boolean> checkRelation = new HashMap<AnnotationLayer, Boolean>();
        Map<AnnotationLayer, AnnotationLayer> relationLayers = new HashMap<AnnotationLayer, AnnotationLayer>();

        int i = 0;
        for (AnnotationLayer relationLayer : aLayers) {

            if (relationLayer.getType().equals(CHAIN_TYPE)) {
                relationLayers.put(relationLayer, relationLayer);
                checkRelation.put(relationLayer, true);
            }
            else if (relationLayer.getType().equals(RELATION_TYPE)) {
                relationLayers.put(relationLayer.getAttachType(), relationLayer);
                checkRelation.put(relationLayer.getAttachType(), true);

            }
            else if (checkRelation.get(relationLayer) == null) {
                checkRelation.put(relationLayer, false);
            }
        }

        for (AnnotationLayer layer : checkRelation.keySet()) {
            if (checkRelation.get(layer) == false) {
                i = configCollection(aAnnotationService, aStaticColor, entityTypes, i, layer,
                        new AnnotationLayer());
            }
            else {
                i = configCollection(aAnnotationService, aStaticColor, entityTypes, i, layer,
                        relationLayers.get(layer));
            }

        }

        return entityTypes;
    }

    private int configCollection(AnnotationService aAnnotationService, boolean aStaticColor,
            Set<EntityType> entityTypes, int i, AnnotationLayer aSpanLayer,
            AnnotationLayer aRelationLayer)
    {

        List<String> fGColors = new ArrayList<String>(
                Arrays.asList(new String[] { "yellow", "red", "pink", "black", "coral",
                        "darkorange", "deepskyblue", "fuchsia", "navy", "maroon" }));
        List<String> bGColors = new ArrayList<String>(Arrays.asList(new String[] { "magenta",
                "lightgreen", "brown", "Sienna ", "darkgreen", "darkorchid", "deeppink", "gold",
                "green", "orange" }));
        List<String> bDColors = new ArrayList<String>(Arrays.asList(new String[] { "aqua",
                "blueviolet", "chocolate", "cyan", "darkgreen", "darkorchid", "deeppink", "gold",
                "magenta", "orange" }));

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
            if (feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)
                    && feature.getTagset() != null) {
                for (Tag tag : aAnnotationService.listTags(feature.getTagset())) {
                    tags.add(tag.getName());
                }
                spanTags.add(tags);
            }
            else if (feature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE)
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
            aRelationLayer = aAnnotationService.getLayer(Dependency.class.getName(), RELATION_TYPE,
                    aSpanLayer.getProject());
        }

        if (aRelationLayer.getId() != 0
                && !aRelationLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
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

            tagLists = getChildren(aSpanLayer,
                    aRelationLayer.getId() == 0 ? "" : aRelationLayer.getId() + "_",
                    concatTagsPerFeature(spanTags), concatTagsPerFeature(relationTags), "black",
                    "cyan", "green", "", aStaticColor);

            entityType = new EntityType(aSpanLayer.getId() + "", aSpanLayer.getId() + "", true, "",
                    "black", "cyan", "green", new ArrayList<String>(), tagLists,
                    new ArrayList<String>(), new ArrayList<RelationType>(), aStaticColor);

        }
        else if (aSpanLayer.isBuiltIn() && aSpanLayer.getName().equals(Lemma.class.getName())) {

            tagLists = getChildren(aSpanLayer,
                    aRelationLayer.getId() == 0 ? "" : aRelationLayer.getId() + "_",
                    concatTagsPerFeature(spanTags), concatTagsPerFeature(relationTags), "", "", "",
                    "", aStaticColor);

            entityType = new EntityType(aSpanLayer.getId() + "", aSpanLayer.getId() + "", true, "",
                    "", "", "", new ArrayList<String>(), tagLists, new ArrayList<String>(),
                    new ArrayList<RelationType>(), aStaticColor);

        }

        // custom layers
        else {
            tagLists = getChildren(aSpanLayer,
                    aRelationLayer.getId() == 0 ? "" : aRelationLayer.getId() + "_",
                    concatTagsPerFeature(spanTags), concatTagsPerFeature(relationTags),
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

}
