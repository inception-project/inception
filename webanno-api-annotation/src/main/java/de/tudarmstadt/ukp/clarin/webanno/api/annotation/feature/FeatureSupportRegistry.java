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

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint.ContextLookupExtensionPoint;

public interface FeatureSupportRegistry
    extends ContextLookupExtensionPoint<AnnotationFeature, FeatureSupport<?>>
{
    String SERVICE_NAME = "featureSupportRegistry";

    /**
     * Get the types of all features the user should be able to create. There can also be internal
     * types reserved for built-in features. These are not returned.
     */
    List<FeatureType> getAllTypes(AnnotationLayer aLayer);
    
    /**
     * Get the types of all features the user should be able to create. There can also be internal
     * types reserved for built-in features. These are not returned.
     */
    List<FeatureType> getUserSelectableTypes(AnnotationLayer aLayer);

    FeatureType getFeatureType(AnnotationFeature aFeature);
    
    /**
     * @deprecated Use {@link #findExtension(AnnotationFeature)} instead;
     */
    @Deprecated
    <T extends FeatureSupport<?>> T getFeatureSupport(AnnotationFeature aFeature);
    
    /**
     * @deprecated Use {@link #getExtension(String)} instead;
     */
    @Deprecated
    <T extends FeatureSupport<?>> T getFeatureSupport(String aFeatureSupportId);
}
