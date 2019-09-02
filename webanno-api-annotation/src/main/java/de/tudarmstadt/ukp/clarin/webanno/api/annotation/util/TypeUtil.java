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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;

/**
 * Utility Class for {@link TypeAdapter} with static methods such as geting
 * {@link TypeAdapter} based on its {@link CAS} {@link Type}
 */
public final class TypeUtil
{
    private TypeUtil()
    {
        // No instances
    }

    /**
     * Construct the label text used in the brat user interface.
     *
     * @param aAdapter the adapter.
     * @param aFeatures the features.
     * @return the label.
     */
    public static String getUiLabelText(TypeAdapter aAdapter, Map<String, String> aFeatures)
    {
        StringBuilder bratLabelText = new StringBuilder();
        for (Entry<String, String> feature : aFeatures.entrySet()) {
            String label = StringUtils.defaultString(feature.getValue());
            
            if (bratLabelText.length() > 0 && label.length() > 0) {
                bratLabelText.append(TypeAdapter.FEATURE_SEPARATOR);
            }

            bratLabelText.append(label);
        }

        if (bratLabelText.length() > 0) {
            return bratLabelText.toString();
        }
        else {
            // If there are no label features at all, then return the empty string. This avoids
            // NPEs in the coloring strategy, saves a few characters in the brat JSON ("" vs null)
            // and still causes the brat UI JS to fall back to the layer name.
            return "";
        }
    }
    
    /**
     * Construct the hover text used in the brat user interface.
     *
     * @param aAdapter the adapter.
     * @param aHoverFeatures the features.
     * @return the hover text.
     */
    public static String getUiHoverText(TypeAdapter aAdapter, Map<String, String> aHoverFeatures)
    {
        StringBuilder bratHoverText = new StringBuilder();
        if (aHoverFeatures.containsKey("__spantext__")) {
            bratHoverText
                .append("\"")
                .append(StringUtils.defaultString(aHoverFeatures.get("__spantext__")))
                .append("\" ");
        }
        
        boolean featuresToShowAvailable = false;
        for (Entry<String, String> feature : aHoverFeatures.entrySet()) {
            if ("__spantext__".equals(feature.getKey())) {
                continue;
            }
            String text = StringUtils.defaultString(feature.getValue());
            
            if (bratHoverText.length() > 0 && featuresToShowAvailable && text.length() > 0) {
                bratHoverText.append(TypeAdapter.FEATURE_SEPARATOR);
            }

            bratHoverText.append(text);
            featuresToShowAvailable = true;
        }

        if (featuresToShowAvailable) {
            return bratHoverText.toString();
        }
        else {
            // If there are no hover features at all, then use the spantext, which 
            // is the default if no hover text is provided
            return null;
        }
    }
    
    /**
     * Construct the label text used in the brat user interface.
     *
     * @param aAdapter the adapter.
     * @param aFs the annotation.
     * @param aFeatures the features.
     * @return the label.
     */
    public static String getUiLabelText(TypeAdapter aAdapter, FeatureStructure aFs,
            List<AnnotationFeature> aFeatures)
    {
        StringBuilder bratLabelText = new StringBuilder();
        for (AnnotationFeature feature : aFeatures) {

            if (!feature.isEnabled() || !feature.isVisible()
                    || !MultiValueMode.NONE.equals(feature.getMultiValueMode())) {
                continue;
            }

            Feature labelFeature = aFs.getType().getFeatureByBaseName(feature.getName());
            String label = StringUtils.defaultString(aFs.getFeatureValueAsString(labelFeature));
            
            if (bratLabelText.length() > 0 && label.length() > 0) {
                bratLabelText.append(TypeAdapter.FEATURE_SEPARATOR);
            }

            bratLabelText.append(label);
        }

        if (bratLabelText.length() > 0) {
            return bratLabelText.toString();
        }
        else {
            // If there are no label features at all, then use the layer UI name
            return "(" + aAdapter.getLayer().getUiName() + ")";
        }
    }
    
    /**
     * Construct the hover text used in the brat user interface.
     *
     * @param aAdapter the adapter.
     * @param aFs the annotation.
     * @param aFeatures the features.
     * @return the hover text.
     */
    public static String getUiHoverText(TypeAdapter aAdapter, AnnotationFS aFs,
            List<AnnotationFeature> aFeatures)
    {
        StringBuilder bratHoverText = new StringBuilder();
        for (AnnotationFeature feature : aFeatures) {

            if (!feature.isEnabled() || !feature.isIncludeInHover()
                    || !MultiValueMode.NONE.equals(feature.getMultiValueMode())) {
                continue;
            }

            Feature labelFeature = aFs.getType().getFeatureByBaseName(feature.getName());
            String text = StringUtils.defaultString(aFs.getFeatureValueAsString(labelFeature));
            
            if (bratHoverText.length() > 0 && text.length() > 0) {
                bratHoverText.append(TypeAdapter.FEATURE_SEPARATOR);
            }

            bratHoverText.append(text);
        }

        if (bratHoverText.length() > 0) {
            if (aAdapter.getLayer().isShowTextInHover()) {
                return String.format("\"%s\" %s", aFs.getCoveredText(), bratHoverText.toString());
            }
            return bratHoverText.toString();
        }
        else {
            // If there are no label features at all, then use the spantext, which 
            // is the default if no hover text is provided
            return null;
        }
    }

    /**
     * @param aBratTypeName the brat type name.
     * @return the layer ID.
     * @see #getUiTypeName
     */
    public static long getLayerId(String aBratTypeName)
    {
        return Long.parseLong(aBratTypeName.substring(0, aBratTypeName.indexOf("_")));
    }

    public static String getUiTypeName(TypeAdapter aAdapter)
    {
        return aAdapter.getTypeId() + "_" + aAdapter.getAnnotationTypeName();
    }

    public static String getUiTypeName(AnnotationLayer aLayer)
    {
        return aLayer.getId() + "_" + aLayer.getName();
    }
}
