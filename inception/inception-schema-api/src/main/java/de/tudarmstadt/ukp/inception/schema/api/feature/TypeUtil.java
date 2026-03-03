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
package de.tudarmstadt.ukp.inception.schema.api.feature;

import static java.lang.Long.parseLong;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;

/**
 * Utility Class for {@link TypeAdapter} with static methods such as getting {@link TypeAdapter}
 * based on its {@link CAS} {@link Type}
 */
public final class TypeUtil
{
    private TypeUtil()
    {
        // No instances
    }

    /**
     * Construct the label text used in the user interface.
     *
     * @param aVObject
     *            a visual object.
     * @return the label.
     */
    public static String getUiLabelText(VObject aVObject)
    {
        return getUiLabelText(aVObject.getFeatures());
    }

    /**
     * Construct the label text used in the user interface.
     *
     * @param aAdapter
     *            the adapter (unused).
     * @param aVObject
     *            a visual object.
     * @return the label.
     * @deprecated The adapter argument is no longer needed/used.
     */
    @Deprecated
    public static String getUiLabelText(TypeAdapter aAdapter, VObject aVObject)
    {
        if (aVObject.getLabelHint() != null) {
            return aVObject.getLabelHint();
        }

        return getUiLabelText(aVObject.getFeatures());
    }

    /**
     * Construct the label text used in the user interface.
     *
     * @param aAdapter
     *            the adapter (unused).
     * @param aFeatures
     *            the features.
     * @return the label.
     * @deprecated The adapter argument is no longer needed/used.
     */
    @Deprecated
    public static String getUiLabelText(TypeAdapter aAdapter, Map<String, String> aFeatures)
    {
        return getUiLabelText(aFeatures);
    }

    /**
     * Construct the label text used in the user interface.
     *
     * @param aFeatures
     *            the features.
     * @return the label.
     */
    public static String getUiLabelText(Map<String, String> aFeatures)
    {
        var labelText = new StringBuilder();
        for (Entry<String, String> feature : aFeatures.entrySet()) {
            var label = defaultString(feature.getValue());

            if (labelText.length() > 0 && label.length() > 0) {
                labelText.append(TypeAdapter.FEATURE_SEPARATOR);
            }

            labelText.append(label);
        }

        if (labelText.length() > 0) {
            return labelText.toString();
        }
        else {
            // If there are no label features at all, then return the empty string. This avoids
            // NPEs in the coloring strategy, saves a few characters in the brat JSON ("" vs null)
            // and still causes the brat UI JS to fall back to the layer name.
            return "";
        }
    }

    /**
     * Construct the hover text used in the user interface.
     *
     * @param aAdapter
     *            the adapter.
     * @param aHoverFeatures
     *            the features.
     * @return the hover text.
     */
    public static String getUiHoverText(TypeAdapter aAdapter, Map<String, String> aHoverFeatures)
    {
        StringBuilder bratHoverText = new StringBuilder();
        if (aHoverFeatures.containsKey("__spantext__")) {
            bratHoverText.append("\"").append(defaultString(aHoverFeatures.get("__spantext__")))
                    .append("\" ");
        }

        boolean featuresToShowAvailable = false;
        for (Entry<String, String> feature : aHoverFeatures.entrySet()) {
            if ("__spantext__".equals(feature.getKey())) {
                continue;
            }
            String text = defaultString(feature.getValue());

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
     * @param aAdapter
     *            the adapter.
     * @param aFs
     *            the annotation.
     * @param aFeatures
     *            the features.
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

            var labelFeature = aFs.getType().getFeatureByBaseName(feature.getName());
            var label = defaultString(aFs.getFeatureValueAsString(labelFeature));

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
     * @param aUiTypeName
     *            the brat type name.
     * @return the layer ID.
     * @deprecated This is mostly brat specific and should be removed/replaced by a DIAM mechanism
     */
    @Deprecated
    public static long getLayerId(String aUiTypeName)
    {
        return parseLong(aUiTypeName);
    }
}
