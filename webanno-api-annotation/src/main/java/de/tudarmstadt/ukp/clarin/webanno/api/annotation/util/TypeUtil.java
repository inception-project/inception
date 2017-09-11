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
            // If there are no label features at all, then use the layer UI name
            return "(" + aAdapter.getLayer().getUiName() + ")";
        }
    }
    
    /**
     * Construct the hover text used in the brat user interface.
     *
     * @param aAdapter the adapter.
     * @param aFeatures the features.
     * @return the hover text.
     */
    public static String getUiHoverText(TypeAdapter aAdapter, Map<String, String> aFeatures)
    {
        // TODO FIXME: implement me
        return "";
    }
    
    /**
     * Construct the label text used in the brat user interface.
     *
     * @param aAdapter the adapter.
     * @param aFs the annotation.
     * @param aFeatures the features.
     * @return the label.
     */
    public static String getUiLabelText(TypeAdapter aAdapter, AnnotationFS aFs,
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
        // TODO FIXME: implement me
        return "";
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
