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
package de.tudarmstadt.ukp.inception.rendering;

import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.NONE;
import static de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType.ERROR;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectByAddr;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ConstraintsEvaluator;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

/**
 * Type renderer for span, arc, and chain annotations
 */
public interface Renderer
{
    TypeAdapter getTypeAdapter();

    /**
     * Render annotations.
     *
     * @param aRequest
     *            The render request.
     * @param aFeatures
     *            the features.
     * @param aResponse
     *            The rendering response.
     */
    void render(RenderRequest aRequest, List<AnnotationFeature> aFeatures, VDocument aResponse);

    /**
     * Render a single annotation .
     *
     * @param aRequest
     *            The render request.
     * @param aFeatures
     *            the features.
     * @param aResponse
     *            The rendering response.
     */
    List<VObject> render(RenderRequest aRequest, List<AnnotationFeature> aFeatures,
            VDocument aResponse, AnnotationFS aFS);

    FeatureSupportRegistry getFeatureSupportRegistry();

    default Map<String, String> renderLabelFeatureValues(TypeAdapter aAdapter, FeatureStructure aFs,
            List<AnnotationFeature> aFeatures)
    {
        var fsr = getFeatureSupportRegistry();
        var features = new LinkedHashMap<String, String>();

        for (var feature : aFeatures) {
            if (!feature.isEnabled() || !feature.isVisible()) {
                continue;
            }

            var maybeFeatureSupport = fsr.findExtension(feature);
            if (maybeFeatureSupport.isEmpty()) {
                continue;
            }

            var featureSupport = maybeFeatureSupport.get();
            if (!featureSupport.isAccessible(feature)) {
                continue;
            }

            var label = defaultString(featureSupport.renderFeatureValue(feature, aFs));

            features.put(feature.getName(), label);
        }

        return features;
    }

    default void renderRequiredFeatureErrors(RenderRequest aRequest,
            List<AnnotationFeature> aFeatures, FeatureStructure aFS, VDocument aResponse)
    {
        var evaluator = new ConstraintsEvaluator();
        var adapter = getTypeAdapter();
        var constraits = aRequest.getConstraints();

        for (var feature : aFeatures) {
            if (!feature.isEnabled()) {
                continue;
            }

            if (evaluator.isHiddenConditionalFeature(constraits, aFS, feature)) {
                continue;
            }

            if (!adapter.isFeatureValueValid(feature, aFS)) {
                aResponse.add(new VComment(VID.of(aFS), ERROR,
                        "Required feature [" + feature.getName() + "] not set."));
            }
        }
    }

    default List<VLazyDetailGroup> lookupLazyDetails(CAS aCas, VID aVid)
    {
        var fsr = getFeatureSupportRegistry();

        var aFs = selectByAddr(aCas, AnnotationFS.class, aVid.getId());

        var details = new ArrayList<VLazyDetailGroup>();
        generateLazyDetailsForFeaturesIncludedInHover(fsr, details, aFs);
        return details;
    }

    default void generateLazyDetailsForFeaturesIncludedInHover(FeatureSupportRegistry fsr,
            List<VLazyDetailGroup> details, AnnotationFS aFs)
    {
        for (var feature : getTypeAdapter().listFeatures()) {
            if (!feature.isEnabled() || !feature.isIncludeInHover()
                    || feature.getMultiValueMode() != NONE) {
                continue;
            }

            var label = fsr.findExtension(feature).orElseThrow().renderFeatureValue(feature, aFs);

            if (isNotBlank(label)) {
                var group = new VLazyDetailGroup();
                group.addDetail(new VLazyDetail(feature.getName(), label));
                details.add(group);
            }
        }
    }
}
