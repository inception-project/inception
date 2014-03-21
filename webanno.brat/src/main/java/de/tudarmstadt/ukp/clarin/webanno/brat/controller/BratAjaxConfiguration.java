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
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
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
    public Set<EntityType> configureVisualizationAndAnnotation(List<TagSet> aTagSets,
            AnnotationService aAnnotationService, boolean aStaticColor)
    {
        Set<EntityType> entityTypes = new HashSet<EntityType>();

        /*
         * List<String> poses = new ArrayList<String>();
         *
         * List<String> dependency = new ArrayList<String>();
         *
         * List<String> namedEntity = new ArrayList<String>();
         *
         * List<String> corefRelType = new ArrayList<String>();
         *
         * List<String> coreference = new ArrayList<String>();
         *
         * for (Tag tag : aTags) { if (tag.getTagSet().getType().getName().equals
         * (AnnotationTypeConstant.POS)) { poses.add(tag.getName()); } else if (tag
         * .getTagSet().getType().getName().equals(AnnotationTypeConstant.DEPENDENCY )) {
         * dependency.add(tag.getName()); } else if (tag.getTagSet().getType
         * ().getName().equals(AnnotationTypeConstant.NAMEDENTITY)) {
         * namedEntity.add(tag.getName()); } else if (tag.getTagSet().getType().getName()
         * .equals(AnnotationTypeConstant.COREFRELTYPE)) { corefRelType.add(tag.getName()); } else
         * if (tag.getTagSet().getType(). getName().equals(AnnotationTypeConstant.COREFERENCE)) {
         * coreference.add(tag.getName()); }
         *
         * }
         *
         * Collections.sort(poses); Collections.sort(dependency); Collections.sort(namedEntity);
         * Collections.sort(coreference); Collections.sort(corefRelType);
         */

        Map<TagSet, Boolean> checkRelation = new HashMap<TagSet, Boolean>();
        Map<TagSet, TagSet> relationLayers = new HashMap<TagSet, TagSet>();

        int i = 0;
        for (TagSet relationTagSet : aTagSets) {
            if (relationTagSet.getFeature() == null || relationTagSet.getLayer() == null) {
                continue;
            }

            if (relationTagSet.getLayer().getType().equals("chain")) {
                if (relationTagSet.getFeature().getName().equals("referenceRelation")) {
                    AnnotationType chainLayer = relationTagSet.getFeature().getLayer();
                    List<AnnotationFeature> chainFeatures = aAnnotationService
                            .listAnnotationFeature(chainLayer);
                    AnnotationFeature linkFeature = null;
                    for (AnnotationFeature feature : chainFeatures) {
                        if (!feature.getName().equals("referenceRelation")) {
                            linkFeature = feature;
                            break;
                        }
                    }
                    TagSet typeTagSet = aAnnotationService.getTagSet(linkFeature,
                            chainLayer.getProject());

                    relationLayers.put(typeTagSet, relationTagSet);
                    checkRelation.put(typeTagSet, true);
                }
            }
            else if (relationTagSet.getFeature().getLayer().getType().equals("relation")) {
                // get the attach feature/or TODO attache type
                // if attachefature is null, get the tagset from attache type
                AnnotationFeature attachFeature = relationTagSet.getFeature().getLayer()
                        .getAttachFeature();
                TagSet attachTagSet = null;
                AnnotationType attachLayer = null;
                for (AnnotationType layer : aAnnotationService.listAnnotationType(relationTagSet
                        .getProject())) {
                    if (layer.getAttachFeature() != null && layer.getType().equals("span")
                            && layer.getAttachFeature().equals(attachFeature)) {
                        attachLayer = layer;
                    }
                }

                if (attachFeature == null) {
                    attachLayer = relationTagSet.getFeature().getLayer().getAttachType();
                }
                // assume a span annotation type a single feature
                List<AnnotationFeature> spanFeatures = aAnnotationService
                        .listAnnotationFeature(attachLayer);
                attachTagSet = aAnnotationService.getTagSet(spanFeatures.get(0),
                        attachLayer.getProject());

                relationLayers.put(attachTagSet, relationTagSet);
                checkRelation.put(attachTagSet, true);

            }
            else if (checkRelation.get(relationTagSet) == null) {
                checkRelation.put(relationTagSet, false);
            }
        }

        for (TagSet tagSet : checkRelation.keySet()) {
            if (checkRelation.get(tagSet) == false) {
                i = configCollection(aAnnotationService, aStaticColor, entityTypes, checkRelation,
                        i, tagSet, new TagSet());
            }
            else {
                i = configCollection(aAnnotationService, aStaticColor, entityTypes, checkRelation,
                        i, tagSet, relationLayers.get(tagSet));
            }

        }
        /*
         * List<EntityType> posChildren = getChildren(AnnotationTypeConstant.POS_PREFIX,
         * AnnotationTypeConstant.DEP_PREFIX, poses, new ArrayList<String>(), "red", "yellow",
         * "blue", "green", aStaticColor); EntityType posType = new
         * EntityType(AnnotationTypeConstant.POS_PARENT, AnnotationTypeConstant.POS_PARENT, true,
         * "", "red", "blue", "blue", new ArrayList<String>(), posChildren, new ArrayList<String>(),
         * new ArrayList<RelationType>(), aStaticColor);
         *
         * if (poses.size() > 0) { entityTypes.add(posType); }
         *
         * List<EntityType> corefChildren = getChildren(AnnotationTypeConstant.COREFRELTYPE_PREFIX,
         * AnnotationTypeConstant.COREFERENCE_PREFIX, corefRelType, coreference, "red", "blue",
         * "blue", "", aStaticColor); EntityType corefType = new
         * EntityType(AnnotationTypeConstant.COREFERENCE_PARENT,
         * AnnotationTypeConstant.COREFERENCE_PARENT, true, "", "red", "blue", "blue", new
         * ArrayList<String>(), corefChildren, new ArrayList<String>(), new
         * ArrayList<RelationType>(), aStaticColor); if (corefRelType.size() > 0) {
         * entityTypes.add(corefType); }
         *
         * List<EntityType> neChildren = getChildren(AnnotationTypeConstant.NAMEDENTITY_PREFIX, "",
         * namedEntity, new ArrayList<String>(), "black", "cyan", "green", "", aStaticColor);
         *
         * EntityType neEntityType = new EntityType(AnnotationTypeConstant.NAMEDENTITY_PARENT,
         * AnnotationTypeConstant.NAMEDENTITY_PARENT, true, "", "black", "cyan", "green", new
         * ArrayList<String>(), neChildren, new ArrayList<String>(), new ArrayList<RelationType>(),
         * aStaticColor);
         *
         * if (namedEntity.size() > 0) { entityTypes.add(neEntityType); }
         */

        return entityTypes;
    }

    private int configCollection(AnnotationService aAnnotationService, boolean aStaticColor,
            Set<EntityType> entityTypes, Map<TagSet, Boolean> checkRelation, int i,
            TagSet aSpanTagSet, TagSet aRelationTagSet)
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
        List<String> spanTags = new ArrayList<String>();
        List<String> relationTags = new ArrayList<String>();

        for (Tag tag : aAnnotationService.listTags(aSpanTagSet)) {
            spanTags.add(tag.getName());
        }

        if (aRelationTagSet.getId() != 0) {
            for (Tag tag : aAnnotationService.listTags(aRelationTagSet)) {
                relationTags.add(tag.getName());
            }
        }

        EntityType entityType;
        List<EntityType> tagLists;
        if (aSpanTagSet.getLayer().isBuiltIn()
                && aSpanTagSet.getLayer().getName().equals(POS.class.getName())) {

            tagLists = getChildren(aSpanTagSet, aRelationTagSet.getId() == 0 ? "" : aRelationTagSet
                    .getFeature().getId() + "$_", spanTags, relationTags, "red", "yellow", "blue",
                    "green", aStaticColor);

            entityType = new EntityType(aSpanTagSet.getFeature().getId() + "", aSpanTagSet
                    .getFeature().getId() + "", true, "", "red", "blue", "blue",
                    new ArrayList<String>(), tagLists, new ArrayList<String>(),
                    new ArrayList<RelationType>(), aStaticColor);

        }
        else if (aSpanTagSet.getLayer().isBuiltIn()
                && aSpanTagSet.getLayer().getName().equals(NamedEntity.class.getName())) {

            tagLists = getChildren(aSpanTagSet, aRelationTagSet.getId() == 0 ? "" : aRelationTagSet
                    .getFeature().getId() + "$_", spanTags, relationTags, "black", "cyan", "green",
                    "", aStaticColor);

            entityType = new EntityType(aSpanTagSet.getFeature().getId() + "", aSpanTagSet
                    .getFeature().getId() + "", true, "", "black", "cyan", "green",
                    new ArrayList<String>(), tagLists, new ArrayList<String>(),
                    new ArrayList<RelationType>(), aStaticColor);

        }
        else if (aSpanTagSet.getLayer().isBuiltIn()
                && aSpanTagSet.getLayer().getName().equals(Lemma.class.getName())) {

            tagLists = getChildren(aSpanTagSet, aRelationTagSet.getId() == 0 ? "" : aRelationTagSet
                    .getFeature().getId() + "$_", spanTags, relationTags, "", "", "", "",
                    aStaticColor);

            entityType = new EntityType(aSpanTagSet.getFeature().getId() + "", aSpanTagSet
                    .getFeature().getId() + "", true, "", "", "", "", new ArrayList<String>(),
                    tagLists, new ArrayList<String>(), new ArrayList<RelationType>(), aStaticColor);

        }

        // custom layers
        else {
            tagLists = getChildren(aSpanTagSet, aRelationTagSet.getId() == 0 ? "" : aRelationTagSet
                    .getFeature().getId() + "$_", spanTags, relationTags, fGColors.get(i),
                    bGColors.get(i), bDColors.get(i), bDColors.get(i), aStaticColor);

            entityType = new EntityType(aSpanTagSet.getFeature().getId() + "", aSpanTagSet
                    .getFeature().getId() + "", true, "", fGColors.get(i), bGColors.get(i),
                    bDColors.get(i), new ArrayList<String>(), tagLists, new ArrayList<String>(),
                    new ArrayList<RelationType>(), aStaticColor);
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
    private List<EntityType> getChildren(TagSet aSpanTagSet, String aChildPrefix,
            List<String> spansList, List<String> aArcList, String aFgColor, String aBgcolor,
            String aBorderColoer, String aArcColor, boolean aStaticColor)
    {
        List<EntityType> children = new ArrayList<EntityType>();

        String prefix = aSpanTagSet.getFeature().getId() + "_";
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
                if (aSpanTagSet.getLayer().getType().equals("chain")) {

                    String[] colors = new String[] { "#00FF00", "#0000A0", "#FF0000", "#800080 ",
                            "#F000FF", "#00FFFF ", "#FF00FF ", "#8D38C9", "#8D38C9", "#736AFF",
                            "#C11B17", "#800000" };
                    int i = 1;
                    for (String color : colors) {
                        RelationType arc = new RelationType(color, "triangle,5",
                                Arrays.asList(arcLabels), i+"_" + aChildPrefix + arcLabels, arcTargets,
                                "");
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

}
