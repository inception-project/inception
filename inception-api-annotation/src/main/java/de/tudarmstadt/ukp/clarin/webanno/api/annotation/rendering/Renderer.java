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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isRequiredFeatureMissing;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.NONE;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VCommentType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VLazyDetailQuery;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VLazyDetailResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;

/**
 * Type renderer for span, arc, and chain annotations
 */
public interface Renderer
{
    static final String QUERY_LAYER_LEVEL_DETAILS = "#";

    TypeAdapter getTypeAdapter();

    /**
     * Render annotations.
     *
     * @param aCas
     *            The CAS containing annotations
     * @param aFeatures
     *            the features.
     * @param aBuffer
     *            The rendering buffer.
     * @param windowBeginOffset
     *            The start position of the window offset.
     * @param windowEndOffset
     *            The end position of the window offset.
     */
    void render(CAS aCas, List<AnnotationFeature> aFeatures, VDocument aBuffer,
            int windowBeginOffset, int windowEndOffset);

    FeatureSupportRegistry getFeatureSupportRegistry();

    default Map<String, String> renderLabelFeatureValues(TypeAdapter aAdapter, FeatureStructure aFs,
            List<AnnotationFeature> aFeatures)
    {
        FeatureSupportRegistry fsr = getFeatureSupportRegistry();
        Map<String, String> features = new LinkedHashMap<>();

        for (AnnotationFeature feature : aFeatures) {
            if (!feature.isEnabled() || !feature.isVisible()
                    || !MultiValueMode.NONE.equals(feature.getMultiValueMode())) {
                continue;
            }

            String label = defaultString(
                    fsr.findExtension(feature).renderFeatureValue(feature, aFs));

            features.put(feature.getName(), label);
        }

        return features;
    }

    default List<VLazyDetailQuery> getLazyDetails(AnnotationFS aFs,
            List<AnnotationFeature> aFeatures)
    {
        List<VLazyDetailQuery> details = new ArrayList<>();

        boolean tiggerLayerLevelLazyDetails = false;

        FeatureSupportRegistry fsr = getFeatureSupportRegistry();

        for (AnnotationFeature feature : aFeatures) {
            if (!feature.isEnabled()) {
                continue;
            }

            if (feature.isIncludeInHover() && NONE.equals(feature.getMultiValueMode())) {
                tiggerLayerLevelLazyDetails = true;
            }

            details.addAll(fsr.findExtension(feature).getLazyDetails(feature, aFs));
        }

        if (tiggerLayerLevelLazyDetails) {
            details.add(VLazyDetailQuery.LAYER_LEVEL_QUERY);
        }

        return details;
    }

    default void renderRequiredFeatureErrors(List<AnnotationFeature> aFeatures,
            FeatureStructure aFS, VDocument aResponse)
    {
        for (AnnotationFeature f : aFeatures) {
            if (!f.isEnabled()) {
                continue;
            }

            if (isRequiredFeatureMissing(f, aFS)) {
                aResponse.add(new VComment(new VID(getAddr(aFS)), VCommentType.ERROR,
                        "Required feature [" + f.getName() + "] not set."));
            }
        }
    }

    default List<VLazyDetailResult> renderLazyDetails(CAS aCas, VID aVid)
    {
        FeatureSupportRegistry fsr = getFeatureSupportRegistry();

        List<VLazyDetailResult> details = new ArrayList<>();

        AnnotationFS aFs = selectByAddr(aCas, AnnotationFS.class, aVid.getId());

        for (AnnotationFeature feature : getTypeAdapter().listFeatures()) {
            if (!feature.isEnabled() || !feature.isIncludeInHover()
                    || !MultiValueMode.NONE.equals(feature.getMultiValueMode())) {
                continue;
            }

            String text = defaultString(
                    fsr.findExtension(feature).renderFeatureValue(feature, aFs));

            details.add(new VLazyDetailResult(feature.getName(), text));
        }

        return details;
    }
}
