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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature;

import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public interface FeatureSupportRegistry
{
    String SERVICE_NAME = "featureSupportRegistry";
    
    default List<FeatureType> getAllTypes(AnnotationLayer aLayer)
    {
        List<FeatureType> types = new ArrayList<>();
        for (FeatureSupport featureSupport : getFeatureSupports()) {
            types.addAll(featureSupport.getSupportedFeatureTypes(aLayer));
        }

        types.sort(comparing(FeatureType::getUiName));
        
        return types;
    }

    List<FeatureSupport> getFeatureSupports();

    /**
     * Get the feature support providing the given feature. This method must only be called on
     * completely configured and saved features, not on unsafed features.
     */
    <T> FeatureSupport<T> getFeatureSupport(AnnotationFeature aFeature);
    
    <T extends FeatureSupport<?>> T getFeatureSupport(String aFeatureSupportId);

    FeatureType getFeatureType(AnnotationFeature aFeature);
}
