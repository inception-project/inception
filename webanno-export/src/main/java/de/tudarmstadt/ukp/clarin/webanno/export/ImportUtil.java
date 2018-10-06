/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

/**
 * This class contains Utility methods that can be used in Project settings.
 */
public class ImportUtil
{
    public static final String META_INF = "META-INF";
    public static final String SOURCE = "source";
    public static final String TRAIN = "train";
    public static final String ANNOTATION_AS_SERIALISED_CAS = "annotation_ser";
    public static final String CURATION_AS_SERIALISED_CAS = "curation_ser";
    public static final String GUIDELINE = "guideline";
    public static final String EXPORTED_PROJECT = "exportedproject";
    public static final String CONSTRAINTS = "constraints";

    /**
     * Read Tag and Tag Description. A line has a tag name and a tag description separated by a TAB
     * 
     * @param aLineSeparatedTags the line.
     * @return the parsed line.
     */
    public static Map<String, String> getTagSetFromFile(String aLineSeparatedTags)
    {
        Map<String, String> tags = new LinkedHashMap<>();
        StringTokenizer st = new StringTokenizer(aLineSeparatedTags, "\n");
        while (st.hasMoreTokens()) {
            StringTokenizer stTag = new StringTokenizer(st.nextToken(), "\t");
            String tag = stTag.nextToken();
            String description;
            if (stTag.hasMoreTokens()) {
                description = stTag.nextToken();
            }
            else {
                description = tag;
            }
            tags.put(tag.trim(), description);
        }
        return tags;
    }

    /**
     * Check if the zip file is webanno compatible
     * 
     * @param aZipFile the file.
     * @return if it is valid.
     * @throws ZipException if the ZIP file is corrupt.
     * @throws IOException if an I/O error occurs.
     *
     */
    @SuppressWarnings({ "rawtypes" })
    public static boolean isZipValidWebanno(File aZipFile)
        throws IOException
    {

        boolean isZipValidWebanno = false;
        ZipFile zip = new ZipFile(aZipFile);
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().replace("/", "").startsWith(ImportUtil.EXPORTED_PROJECT)
                    && entry.toString().replace("/", "").endsWith(".json")) {
                isZipValidWebanno = true;
                break;
            }
        }
        return isZipValidWebanno;
    }

    @Deprecated
    public static void createTagSet(TagSet aTagSet,
            ExportedTagSet aExTagSet, Project aProject,
            User aUser, AnnotationSchemaService aAnnotationService)
        throws IOException
    {
        aTagSet.setCreateTag(aExTagSet.isCreateTag());
        aTagSet.setDescription(aExTagSet.getDescription());
        aTagSet.setLanguage(aExTagSet.getLanguage());
        aTagSet.setName(aExTagSet.getName());
        aTagSet.setProject(aProject);
        aAnnotationService.createTagSet(aTagSet);

        for (ExportedTag exTag : aExTagSet.getTags()) {
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
    public static void setLayer(AnnotationSchemaService aAnnotationService, AnnotationLayer aLayer,
            ExportedAnnotationLayer aExLayer,
            Project aProject, User aUser)
        throws IOException
    {
        aLayer.setAllowStacking(aExLayer.isAllowStacking());
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
        aLayer.setLinkedListBehavior(aExLayer.isLinkedListBehavior());
        aLayer.setUiName(aExLayer.getUiName());
        aLayer.setName(aExLayer.getName());
        aLayer.setProject(aProject);
        aLayer.setType(aExLayer.getType());
        aAnnotationService.createLayer(aLayer);
    }

    @Deprecated
    public static void setFeature(AnnotationSchemaService aAnnotationService,
            AnnotationFeature aFeature,
            ExportedAnnotationFeature aExFeature,
            Project aProject, User aUser)
    {
        aFeature.setDescription(aExFeature.getDescription());
        aFeature.setEnabled(aExFeature.isEnabled());
        aFeature.setVisible(aExFeature.isVisible());
        aFeature.setUiName(aExFeature.getUiName());
        aFeature.setProject(aProject);
        aFeature.setLayer(aFeature.getLayer());
        boolean isItChainedLayer = aFeature.getLayer().getType().equals(WebAnnoConst.CHAIN_TYPE);
        if (isItChainedLayer && (aExFeature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)
                || aExFeature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE))) {
            aFeature.setType(CAS.TYPE_NAME_STRING);
        }
        else {
            aFeature.setType(aExFeature.getType());
        }
        aFeature.setName(aExFeature.getName());
        aFeature.setRemember(aExFeature.isRemember());
        aFeature.setRequired(aExFeature.isRequired());
        aFeature.setHideUnconstraintFeature(aExFeature.isHideUnconstraintFeature());
        aFeature.setMode(aExFeature.getMultiValueMode());
        aFeature.setLinkMode(aExFeature.getLinkMode());
        aFeature.setLinkTypeName(aExFeature.getLinkTypeName());
        aFeature.setLinkTypeRoleFeatureName(aExFeature.getLinkTypeRoleFeatureName());
        aFeature.setLinkTypeTargetFeatureName(aExFeature.getLinkTypeTargetFeatureName());
        aFeature.setTraits(aExFeature.getTraits());

        aAnnotationService.createFeature(aFeature);
    }
    
    @Deprecated
    public static ExportedAnnotationLayer exportLayerDetails(
            Map<AnnotationLayer, ExportedAnnotationLayer> aLayerToExLayer,
            Map<AnnotationFeature, ExportedAnnotationFeature> aFeatureToExFeature,
            AnnotationLayer aLayer, AnnotationSchemaService aAnnotationService)
    {
        ExportedAnnotationLayer exLayer = new ExportedAnnotationLayer();
        exLayer.setAllowStacking(aLayer.isAllowStacking());
        exLayer.setBuiltIn(aLayer.isBuiltIn());
        exLayer.setReadonly(aLayer.isReadonly());
        exLayer.setCrossSentence(aLayer.isCrossSentence());
        exLayer.setDescription(aLayer.getDescription());
        exLayer.setEnabled(aLayer.isEnabled());
        exLayer.setLockToTokenOffset(aLayer.isLockToTokenOffset());
        exLayer.setMultipleTokens(aLayer.isMultipleTokens());
        exLayer.setAnchoringMode(aLayer.getAnchoringMode());
        exLayer.setLinkedListBehavior(aLayer.isLinkedListBehavior());
        exLayer.setName(aLayer.getName());
        exLayer.setProjectName(aLayer.getProject().getName());
        exLayer.setType(aLayer.getType());
        exLayer.setUiName(aLayer.getUiName());

        if (aLayerToExLayer != null) {
            aLayerToExLayer.put(aLayer, exLayer);
        }

        List<ExportedAnnotationFeature> exFeatures = new ArrayList<>();
        for (AnnotationFeature feature : aAnnotationService.listAnnotationFeature(aLayer)) {
            ExportedAnnotationFeature exFeature = new ExportedAnnotationFeature();
            exFeature.setDescription(feature.getDescription());
            exFeature.setEnabled(feature.isEnabled());
            exFeature.setRemember(feature.isRemember());
            exFeature.setRequired(feature.isRequired());
            exFeature.setHideUnconstraintFeature(feature.isHideUnconstraintFeature());
            exFeature.setName(feature.getName());
            exFeature.setProjectName(feature.getProject().getName());
            exFeature.setType(feature.getType());
            exFeature.setUiName(feature.getUiName());
            exFeature.setVisible(feature.isVisible());
            exFeature.setMultiValueMode(feature.getMultiValueMode());
            exFeature.setLinkMode(feature.getLinkMode());
            exFeature.setLinkTypeName(feature.getLinkTypeName());
            exFeature.setLinkTypeRoleFeatureName(feature.getLinkTypeRoleFeatureName());
            exFeature.setLinkTypeTargetFeatureName(feature.getLinkTypeTargetFeatureName());
            exFeature.setTraits(feature.getTraits());
            
            if (feature.getTagset() != null) {
                TagSet tagSet = feature.getTagset();
                ExportedTagSet exTagSet = new ExportedTagSet();
                exTagSet.setDescription(tagSet.getDescription());
                exTagSet.setLanguage(tagSet.getLanguage());
                exTagSet.setName(tagSet.getName());
                exTagSet.setCreateTag(tagSet.isCreateTag());

                List<ExportedTag> exportedTags = new ArrayList<>();
                for (Tag tag : aAnnotationService.listTags(tagSet)) {
                    ExportedTag exTag = new ExportedTag();
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
