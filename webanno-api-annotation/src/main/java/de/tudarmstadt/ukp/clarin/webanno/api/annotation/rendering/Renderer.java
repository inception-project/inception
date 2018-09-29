/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isRequiredFeatureMissing;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VCommentType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;

/**
 * Type renderer for span, arc, and chain annotations
 */
public interface Renderer
{
    /**
     * Render annotations.
     *
     * @param aJcas
     *            The JCAS object containing annotations
     * @param aFeatures
     *            the features.
     * @param aBuffer
     *            The rendering buffer.
     * @param aState
     *            Annotation editor state.
     */
    void render(JCas aJcas, List<AnnotationFeature> aFeatures, VDocument aBuffer,
            AnnotatorState aState);
    
    FeatureSupportRegistry getFeatureSupportRegistry();

    default Map<String, String> getFeatures(TypeAdapter aAdapter, AnnotationFS aFs,
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
                    fsr.getFeatureSupport(feature).renderFeatureValue(feature, aFs));
            
            features.put(feature.getName(), label);
        }
        
        return features;
    }
    
    default Map<String, String> getHoverFeatures(TypeAdapter aAdapter, AnnotationFS aFs,
            List<AnnotationFeature> aFeatures)
    {
        FeatureSupportRegistry fsr = getFeatureSupportRegistry();
        Map<String, String> hoverfeatures = new LinkedHashMap<>();

        if (aAdapter.getLayer().isShowTextInHover()) {
            hoverfeatures.put("__spantext__", aFs.getCoveredText());
        }

        for (AnnotationFeature feature : aFeatures) {
            if (!feature.isEnabled() || !feature.isIncludeInHover()
                    || !MultiValueMode.NONE.equals(feature.getMultiValueMode())) {
                continue;
            }
            
            String text = defaultString(
                    fsr.getFeatureSupport(feature).renderFeatureValue(feature, aFs));
            
            hoverfeatures.put(feature.getName(), text);
        }
        
        return hoverfeatures;
    }
    
    default void renderRequiredFeatureErrors(List<AnnotationFeature> aFeatures,
            FeatureStructure aFS, VDocument aResponse)
    {
        for (AnnotationFeature f : aFeatures) {
            if (isRequiredFeatureMissing(f, aFS)) {
                aResponse.add(new VComment(new VID(getAddr(aFS)), VCommentType.ERROR,
                        "Required feature [" + f.getName() + "] not set."));
            }
        }
    }
}
