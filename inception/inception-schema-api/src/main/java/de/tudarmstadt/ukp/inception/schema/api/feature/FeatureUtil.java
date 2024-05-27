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

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

public class FeatureUtil
{
    /**
     * Set a feature value.
     *
     * @param aFS
     *            the feature structure.
     * @param aFeature
     *            the feature within the annotation whose value to set. If this parameter is
     *            {@code null} then nothing happens.
     * @param aValue
     *            the feature value.
     */
    public static void setFeature(FeatureStructure aFS, AnnotationFeature aFeature, Object aValue)
    {
        if (aFeature == null) {
            return;
        }

        var feature = aFS.getType().getFeatureByBaseName(aFeature.getName());

        if (feature == null) {
            throw new IllegalArgumentException("On [" + aFS.getType().getName() + "] the feature ["
                    + aFeature.getName() + "] does not exist.");
        }

        switch (aFeature.getMultiValueMode()) {
        case NONE: {
            var effectiveType = aFeature.getType();
            if (effectiveType.contains(":")) {
                effectiveType = CAS.TYPE_NAME_STRING;
            }

            // Sanity check
            if (!Objects.equals(effectiveType, feature.getRange().getName())) {
                throw new IllegalArgumentException("On [" + aFS.getType().getName() + "] feature ["
                        + aFeature.getName() + "] actual type [" + feature.getRange().getName()
                        + "] does not match expected feature type [" + effectiveType + "].");
            }

            if (aValue instanceof String stringValue) {
                ICasUtil.setFeature(aFS, feature, stringValue);
                break;
            }

            switch (effectiveType) {
            case CAS.TYPE_NAME_STRING:
                aFS.setStringValue(feature, (String) aValue);
                break;
            case CAS.TYPE_NAME_BOOLEAN:
                aFS.setBooleanValue(feature, aValue != null ? (boolean) aValue : false);
                break;
            case CAS.TYPE_NAME_FLOAT:
                aFS.setFloatValue(feature, aValue != null ? (float) aValue : 0.0f);
                break;
            case CAS.TYPE_NAME_INTEGER:
                aFS.setIntValue(feature, aValue != null ? (int) aValue : 0);
                break;
            default:
                throw new IllegalArgumentException(
                        "Cannot set value of feature [" + aFeature.getName() + "] with type ["
                                + feature.getRange().getName() + "] to [" + aValue + "]");
            }
            break;
        }
        case ARRAY: {
            switch (aFeature.getLinkMode()) {
            case WITH_ROLE: {
                // Get type and features - we need them later in the loop
                setLinkFeature(aFS, aFeature, (List<LinkWithRoleModel>) aValue, feature);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported link mode ["
                        + aFeature.getLinkMode() + "] on feature [" + aFeature.getName() + "]");
            }
            break;
        }
        default:
            throw new IllegalArgumentException("Unsupported multi-value mode ["
                    + aFeature.getMultiValueMode() + "] on feature [" + aFeature.getName() + "]");
        }
    }

    private static void setLinkFeature(FeatureStructure aFS, AnnotationFeature aFeature,
            List<LinkWithRoleModel> aValue, Feature feature)
    {
        Type linkType = aFS.getCAS().getTypeSystem().getType(aFeature.getLinkTypeName());
        Feature roleFeat = linkType.getFeatureByBaseName(aFeature.getLinkTypeRoleFeatureName());
        Feature targetFeat = linkType.getFeatureByBaseName(aFeature.getLinkTypeTargetFeatureName());

        // Create all the links
        // FIXME: actually we could re-use existing link link feature structures
        List<FeatureStructure> linkFSes = new ArrayList<>();

        if (aValue != null) {
            // remove duplicate links
            Set<LinkWithRoleModel> links = new HashSet<>(aValue);
            for (LinkWithRoleModel e : links) {
                // Skip empty slots that have been added where the target has not yet been set
                if (e.targetAddr == -1) {
                    continue;
                }

                FeatureStructure link = aFS.getCAS().createFS(linkType);
                link.setStringValue(roleFeat, e.role);
                link.setFeatureValue(targetFeat, selectFsByAddr(aFS.getCAS(), e.targetAddr));
                linkFSes.add(link);
            }
        }
        setLinkFeatureValue(aFS, feature, linkFSes);

    }

    @SuppressWarnings("unchecked")
    public static void setLinkFeatureValue(FeatureStructure aFS, Feature aFeature,
            List<FeatureStructure> linkFSes)
    {
        if (linkFSes == null || linkFSes.isEmpty()) {
            aFS.setFeatureValue(aFeature, null);
            return;
        }

        // Create a new array if size differs otherwise re-use existing one
        var array = (ArrayFS<FeatureStructure>) ICasUtil.getFeatureFS(aFS, aFeature.getShortName());
        if (array == null || (array.size() != linkFSes.size())) {
            array = aFS.getCAS().createArrayFS(linkFSes.size());
        }

        // Fill in links
        array.copyFromArray(linkFSes.toArray(new FeatureStructure[linkFSes.size()]), 0, 0,
                linkFSes.size());

        aFS.setFeatureValue(aFeature, array);
    }
}
