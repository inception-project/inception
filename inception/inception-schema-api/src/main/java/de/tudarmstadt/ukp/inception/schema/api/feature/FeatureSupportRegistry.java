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

import java.util.List;
import java.util.Optional;

import org.danekja.java.util.function.serializable.SerializableSupplier;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.support.extensionpoint.ContextLookupExtensionPoint;

public interface FeatureSupportRegistry
    extends ContextLookupExtensionPoint<AnnotationFeature, FeatureSupport<?>>
{
    String SERVICE_NAME = "featureSupportRegistry";

    /**
     * @param aLayer
     *            a layer
     * @return the types of all features the user should be able to create. There can also be
     *         internal types reserved for built-in features. These are not returned.
     */
    List<FeatureType> getAllTypes(AnnotationLayer aLayer);

    /**
     * @param aLayer
     *            a layer
     * @return the types of all features the user should be able to create. There can also be
     *         internal types reserved for built-in features. These are not returned.
     */
    List<FeatureType> getUserSelectableTypes(AnnotationLayer aLayer);

    FeatureType getFeatureType(AnnotationFeature aFeature);

    <T> T readTraits(AnnotationFeature aFeature, SerializableSupplier<T> aIfMissing);

    <T> Optional<FeatureSupport<T>> findExtension(AnnotationFeature aKey);

    /**
     * @return whether the given feature is accessible (that implies that it is supported).
     * @param aFeature
     *            the feature to check.
     * @see de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport#isAccessible(AnnotationFeature)
     */
    boolean isAccessible(AnnotationFeature aFeature);

    /**
     * @return whether the given feature is supported (that does not imply that it is supported).
     * @param aFeature
     *            the feature to check.
     */
    boolean isSupported(AnnotationFeature aFeature);
}
