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
package de.tudarmstadt.ukp.inception.export;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.COREFERENCE_TYPE_FEATURE;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayerReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

/**
 * This class contains Utility methods that can be used in Project settings.
 */
public class LayerImportExportUtils
{
    public static String exportLayerToJson(AnnotationSchemaService aSchemaService,
            AnnotationLayer layer)
        throws IOException
    {
        var exLayers = new ArrayList<ExportedAnnotationLayer>();

        var exMainLayer = exportLayerDetails(null, null, layer, aSchemaService);
        exLayers.add(exMainLayer);

        // If the layer is attached to another layer, then we also have to export
        // that, otherwise we would be missing it during re-import.
        if (layer.getAttachType() != null) {
            var attachLayer = layer.getAttachType();
            var exAttachLayer = exportLayerDetails(null, null, attachLayer, aSchemaService);
            exMainLayer
                    .setAttachType(new ExportedAnnotationLayerReference(exAttachLayer.getName()));
            exLayers.add(exAttachLayer);
        }

        return JSONUtil.toPrettyJsonString(exLayers);
    }

    /**
     * Read Tag and Tag Description. A line has a tag name and a tag description separated by a TAB
     * 
     * @param aLineSeparatedTags
     *            the line.
     * @return the parsed line.
     */
    public static Map<String, String> getTagSetFromFile(String aLineSeparatedTags)
    {
        var tags = new LinkedHashMap<String, String>();
        var st = new StringTokenizer(aLineSeparatedTags, "\n");
        while (st.hasMoreTokens()) {
            var stTag = new StringTokenizer(st.nextToken(), "\t");
            var tag = stTag.nextToken();
            var description = stTag.hasMoreTokens() ? stTag.nextToken() : tag;
            tags.put(tag.trim(), description);
        }
        return tags;
    }

    @Deprecated
    private static void createTagSet(TagSet aTagSet, ExportedTagSet aExTagSet, Project aProject,
            AnnotationSchemaService aAnnotationService)
        throws IOException
    {
        aTagSet.setCreateTag(aExTagSet.isCreateTag());
        aTagSet.setDescription(aExTagSet.getDescription());
        aTagSet.setLanguage(aExTagSet.getLanguage());
        aTagSet.setName(aExTagSet.getName());
        aTagSet.setProject(aProject);
        aAnnotationService.createTagSet(aTagSet);

        for (var exTag : aExTagSet.getTags()) {
            // do not duplicate tag
            if (aAnnotationService.existsTag(exTag.getName(), aTagSet)) {
                continue;
            }
            Tag tag = new Tag();
            tag.setDescription(exTag.getDescription());
            tag.setTagSet(aTagSet);
            tag.setName(exTag.getName());
            aAnnotationService.createTag(tag);
        }
    }

    @Deprecated
    private static void setLayer(AnnotationSchemaService aAnnotationService, AnnotationLayer aLayer,
            ExportedAnnotationLayer aExLayer, Project aProject)
        throws IOException
    {
        aLayer.setBuiltIn(aExLayer.isBuiltIn());
        aLayer.setReadonly(aExLayer.isReadonly());
        aLayer.setCrossSentence(aExLayer.isCrossSentence());
        aLayer.setDescription(aExLayer.getDescription());
        aLayer.setEnabled(aExLayer.isEnabled());

        if (aExLayer.getAnchoringMode() == null) {
            // This allows importing old projects which did not have the anchoring mode yet
            aLayer.setAnchoringMode(aExLayer.isLockToTokenOffset(), aExLayer.isMultipleTokens());
        }
        else {
            aLayer.setAnchoringMode(aExLayer.getAnchoringMode());
        }

        if (aExLayer.getOverlapMode() == null) {
            // This allows importing old projects which did not have the overlap mode yet
            aLayer.setOverlapMode(aExLayer.isAllowStacking() ? ANY_OVERLAP : OVERLAP_ONLY);
        }
        else {
            aLayer.setOverlapMode(aExLayer.getOverlapMode());
        }

        aLayer.setValidationMode(aExLayer.getValidationMode() != null ? aExLayer.getValidationMode()
                : ValidationMode.NEVER);
        aLayer.setLinkedListBehavior(aExLayer.isLinkedListBehavior());
        aLayer.setUiName(aExLayer.getUiName());
        aLayer.setName(aExLayer.getName());
        aLayer.setProject(aProject);
        aLayer.setType(aExLayer.getType());
        aLayer.setTraits(aExLayer.getTraits());

        aAnnotationService.createOrUpdateLayer(aLayer);
    }

    public static AnnotationLayer importLayerFile(AnnotationSchemaService annotationService,
            User user, Project project, InputStream aIS)
        throws IOException
    {
        var text = IOUtils.toString(aIS, "UTF-8");

        var exLayers = JSONUtil.getObjectMapper().readValue(text, ExportedAnnotationLayer[].class);

        // First import the layers but without setting the attach-layers/features
        var exLayersMap = new HashMap<String, ExportedAnnotationLayer>();
        var layersMap = new HashMap<String, AnnotationLayer>();
        for (var exLayer : exLayers) {
            var layer = createLayer(annotationService, project, exLayer);
            layersMap.put(layer.getName(), layer);
            exLayersMap.put(layer.getName(), exLayer);
        }

        // Second fill in the attach-layer and attach-feature information
        for (var layer : layersMap.values()) {
            var exLayer = exLayersMap.get(layer.getName());
            if (exLayer.getAttachType() != null) {
                layer.setAttachType(layersMap.get(exLayer.getAttachType().getName()));
            }
            if (exLayer.getAttachFeature() != null) {
                var attachLayer = annotationService.findLayer(project,
                        exLayer.getAttachType().getName());
                var attachFeature = annotationService
                        .getFeature(exLayer.getAttachFeature().getName(), attachLayer);
                layer.setAttachFeature(attachFeature);
            }
            annotationService.createOrUpdateLayer(layer);
        }

        return layersMap.get(exLayers[0].getName());
    }

    private static AnnotationLayer createLayer(AnnotationSchemaService annotationService,
            Project project, ExportedAnnotationLayer aExLayer)
        throws IOException
    {
        AnnotationLayer layer;

        if (annotationService.existsLayer(aExLayer.getName(), aExLayer.getType(), project)) {
            layer = annotationService.findLayer(project, aExLayer.getName());
            setLayer(annotationService, layer, aExLayer, project);
        }
        else {
            layer = new AnnotationLayer();
            setLayer(annotationService, layer, aExLayer, project);
        }

        for (var exfeature : aExLayer.getFeatures()) {
            var exTagset = exfeature.getTagSet();
            TagSet tagSet = null;
            if (exTagset != null && annotationService.existsTagSet(exTagset.getName(), project)) {
                tagSet = annotationService.getTagSet(exTagset.getName(), project);
                createTagSet(tagSet, exTagset, project, annotationService);
            }
            else if (exTagset != null) {
                tagSet = new TagSet();
                createTagSet(tagSet, exTagset, project, annotationService);
            }
            if (annotationService.existsFeature(exfeature.getName(), layer)) {
                AnnotationFeature feature = annotationService.getFeature(exfeature.getName(),
                        layer);
                feature.setTagset(tagSet);
                setFeature(annotationService, feature, exfeature, project);
                continue;
            }
            var feature = new AnnotationFeature();
            feature.setLayer(layer);
            feature.setTagset(tagSet);
            setFeature(annotationService, feature, exfeature, project);
        }

        return layer;
    }

    @Deprecated
    private static void setFeature(AnnotationSchemaService aAnnotationService,
            AnnotationFeature aFeature, ExportedAnnotationFeature aExFeature, Project aProject)
    {
        var isItChainedLayer = ChainLayerSupport.TYPE.equals(aFeature.getLayer().getType());
        if (isItChainedLayer && (COREFERENCE_TYPE_FEATURE.equals(aExFeature.getName())
                || COREFERENCE_RELATION_FEATURE.equals(aExFeature.getName()))) {
            aFeature.setType(CAS.TYPE_NAME_STRING);
        }
        else if (Token._TypeName.equals(aFeature.getLayer().getName())
                && Token._FeatName_morph.equals(aExFeature.getName())
                && Lemma._TypeName.equals(aExFeature.getType())) {
            // See https://github.com/inception-project/inception/issues/3080
            aFeature.setType(MorphologicalFeatures._TypeName);
        }
        else {
            aFeature.setType(aExFeature.getType());
        }

        aFeature.setDescription(aExFeature.getDescription());
        aFeature.setEnabled(aExFeature.isEnabled());
        aFeature.setVisible(aExFeature.isVisible());
        aFeature.setUiName(aExFeature.getUiName());
        aFeature.setProject(aProject);
        aFeature.setName(aExFeature.getName());
        aFeature.setRemember(aExFeature.isRemember());
        aFeature.setRequired(aExFeature.isRequired());
        aFeature.setHideUnconstraintFeature(aExFeature.isHideUnconstraintFeature());
        aFeature.setIncludeInHover(aExFeature.isIncludeInHover());
        aFeature.setMode(aExFeature.getMultiValueMode());
        aFeature.setLinkMode(aExFeature.getLinkMode());
        aFeature.setLinkTypeName(aExFeature.getLinkTypeName());
        aFeature.setLinkTypeRoleFeatureName(aExFeature.getLinkTypeRoleFeatureName());
        aFeature.setLinkTypeTargetFeatureName(aExFeature.getLinkTypeTargetFeatureName());
        aFeature.setTraits(aExFeature.getTraits());
        aFeature.setCuratable(aExFeature.isCuratable());
        aFeature.setRank(aExFeature.getRank());

        aAnnotationService.createFeature(aFeature);
    }

    @Deprecated
    public static ExportedAnnotationLayer exportLayerDetails(
            Map<AnnotationLayer, ExportedAnnotationLayer> aLayerToExLayer,
            Map<AnnotationFeature, ExportedAnnotationFeature> aFeatureToExFeature,
            AnnotationLayer aLayer, AnnotationSchemaService aAnnotationService)
    {
        var exLayer = new ExportedAnnotationLayer();
        exLayer.setAllowStacking(aLayer.isAllowStacking());
        exLayer.setLockToTokenOffset(SINGLE_TOKEN.equals(aLayer.getAnchoringMode()));
        exLayer.setMultipleTokens(TOKENS.equals(aLayer.getAnchoringMode()));
        exLayer.setBuiltIn(aLayer.isBuiltIn());
        exLayer.setReadonly(aLayer.isReadonly());
        exLayer.setCrossSentence(aLayer.isCrossSentence());
        exLayer.setDescription(aLayer.getDescription());
        exLayer.setEnabled(aLayer.isEnabled());
        exLayer.setOverlapMode(aLayer.getOverlapMode());
        exLayer.setAnchoringMode(aLayer.getAnchoringMode());
        exLayer.setValidationMode(aLayer.getValidationMode());
        exLayer.setLinkedListBehavior(aLayer.isLinkedListBehavior());
        exLayer.setName(aLayer.getName());
        exLayer.setProjectId(aLayer.getProject().getId());
        exLayer.setType(aLayer.getType());
        exLayer.setUiName(aLayer.getUiName());
        exLayer.setTraits(aLayer.getTraits());

        if (aLayerToExLayer != null) {
            aLayerToExLayer.put(aLayer, exLayer);
        }

        var exFeatures = new ArrayList<ExportedAnnotationFeature>();
        for (var feature : aAnnotationService.listAnnotationFeature(aLayer)) {
            var exFeature = new ExportedAnnotationFeature();
            exFeature.setDescription(feature.getDescription());
            exFeature.setEnabled(feature.isEnabled());
            exFeature.setRemember(feature.isRemember());
            exFeature.setRequired(feature.isRequired());
            exFeature.setHideUnconstraintFeature(feature.isHideUnconstraintFeature());
            exFeature.setName(feature.getName());
            exFeature.setProjectId(feature.getProject().getId());
            exFeature.setType(feature.getType());
            exFeature.setUiName(feature.getUiName());
            exFeature.setVisible(feature.isVisible());
            exFeature.setIncludeInHover(feature.isIncludeInHover());
            exFeature.setMultiValueMode(feature.getMultiValueMode());
            exFeature.setLinkMode(feature.getLinkMode());
            exFeature.setLinkTypeName(feature.getLinkTypeName());
            exFeature.setLinkTypeRoleFeatureName(feature.getLinkTypeRoleFeatureName());
            exFeature.setLinkTypeTargetFeatureName(feature.getLinkTypeTargetFeatureName());
            exFeature.setTraits(feature.getTraits());
            exFeature.setCuratable(feature.isCuratable());
            exFeature.setRank(feature.getRank());

            if (feature.getTagset() != null) {
                var tagSet = feature.getTagset();
                var exTagSet = new ExportedTagSet();
                exTagSet.setDescription(tagSet.getDescription());
                exTagSet.setLanguage(tagSet.getLanguage());
                exTagSet.setName(tagSet.getName());
                exTagSet.setCreateTag(tagSet.isCreateTag());

                var exportedTags = new ArrayList<ExportedTag>();
                for (var tag : aAnnotationService.listTags(tagSet)) {
                    var exTag = new ExportedTag();
                    exTag.setDescription(tag.getDescription());
                    exTag.setName(tag.getName());
                    exportedTags.add(exTag);
                }
                exTagSet.setTags(exportedTags);
                exFeature.setTagSet(exTagSet);
            }
            exFeatures.add(exFeature);
            if (aFeatureToExFeature != null) {
                aFeatureToExFeature.put(feature, exFeature);
            }
        }

        exLayer.setFeatures(exFeatures);
        return exLayer;
    }
}
