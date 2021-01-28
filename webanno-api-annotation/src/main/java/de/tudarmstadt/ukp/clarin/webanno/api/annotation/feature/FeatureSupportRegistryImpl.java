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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature;

import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint.CachingContextLookupExtensionPoint_ImplBase;

@Component
public class FeatureSupportRegistryImpl
    extends CachingContextLookupExtensionPoint_ImplBase<AnnotationFeature, FeatureSupport<?>>
    implements FeatureSupportRegistry
{
    public FeatureSupportRegistryImpl(
            @Lazy @Autowired(required = false) List<FeatureSupport<?>> aFeatureSupports)
    {
        super(aFeatureSupports, AnnotationFeature::getId);
    }

    @Override
    public <T extends FeatureSupport<?>> T getFeatureSupport(AnnotationFeature aFeature)
    {
        return findExtension(aFeature);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends FeatureSupport<?>> T getFeatureSupport(String aFeatureSupportId)
    {
        return (T) getExtension(aFeatureSupportId);
    }

    @Override
    public List<FeatureType> getAllTypes(AnnotationLayer aLayer)
    {
        List<FeatureType> allTypes = new ArrayList<>();

        for (FeatureSupport<?> featureSupport : getExtensions()) {
            List<FeatureType> types = featureSupport.getSupportedFeatureTypes(aLayer);
            types.stream().forEach(allTypes::add);
        }

        allTypes.sort(comparing(FeatureType::getUiName));

        return allTypes;
    }

    @Override
    public List<FeatureType> getUserSelectableTypes(AnnotationLayer aLayer)
    {
        List<FeatureType> allTypes = new ArrayList<>();

        for (FeatureSupport<?> featureSupport : getExtensions()) {
            List<FeatureType> types = featureSupport.getSupportedFeatureTypes(aLayer);
            types.stream().filter(it -> !it.isInternal()).forEach(allTypes::add);
        }

        allTypes.sort(comparing(FeatureType::getUiName));

        return allTypes;
    }

    @Override
    public FeatureType getFeatureType(AnnotationFeature aFeature)
    {
        if (aFeature.getType() == null) {
            return null;
        }

        // Figure out which feature support provides the given type.
        // If we can find a suitable feature support, then use it to resolve the type to a
        // FeaatureType
        FeatureType featureType = null;
        for (FeatureSupport<?> s : getExtensions()) {
            Optional<FeatureType> ft = s.getFeatureType(aFeature);
            if (ft.isPresent()) {
                featureType = ft.get();
                break;
            }
        }
        return featureType;
    }
}
