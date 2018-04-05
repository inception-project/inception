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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

@Component
public class FeatureSupportRegistryImpl
    implements FeatureSupportRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<FeatureSupport> featureSupportsProxy;
    
    private List<FeatureSupport> featureSupports;
    
    private final Map<Long, FeatureSupport> supportCache = new HashMap<>();

    public FeatureSupportRegistryImpl(
            @Lazy @Autowired(required = false) List<FeatureSupport> aFeatureSupports)
    {
        featureSupportsProxy = aFeatureSupports;
    }
    
    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }
    
    public void init()
    {
        List<FeatureSupport> fsp = new ArrayList<>();

        if (featureSupportsProxy != null) {
            fsp.addAll(featureSupportsProxy);
            AnnotationAwareOrderComparator.sort(fsp);
        
            for (FeatureSupport<?> fs : fsp) {
                log.info("Found feature support: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }
        
        featureSupports = Collections.unmodifiableList(fsp);
    }
    
    @Override
    public List<FeatureSupport> getFeatureSupports()
    {
        return featureSupports;
    }
    
    @Override
    public FeatureSupport getFeatureSupport(AnnotationFeature aFeature)
    {
        // This method is called often during rendering, so we try to make it fast by caching
        // the supports by feature. Since the set of annotation features is relatively stable,
        // this should not be a memory leak - even if we don't remove entries if annotation
        // features would be deleted from the DB.
        FeatureSupport support = null;
        
        if (aFeature.getId() != null) {
            support = supportCache.get(aFeature.getId());
        }
        
        if (support == null) {
            for (FeatureSupport<?> s : getFeatureSupports()) {
                if (s.accepts(aFeature)) {
                    support = s;
                    if (aFeature.getId() != null) {
                        // Store feature in the cache, but only when it has an ID, i.e. it has
                        // actually been saved.
                        supportCache.put(aFeature.getId(), s);
                    }
                    break;
                }
            }
        }
        
        if (support == null) {
            throw new IllegalArgumentException("Unsupported feature: [" + aFeature.getName() + "]");
        }
        
        return support;
    }
    
    @Override
    public FeatureSupport getFeatureSupport(String aId)
    {
        return getFeatureSupports().stream().filter(fs -> fs.getId().equals(aId)).findFirst()
                .orElse(null);
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
        for (FeatureSupport<?> s : getFeatureSupports()) {
            Optional<FeatureType> ft = s.getFeatureType(aFeature);
            if (ft.isPresent()) {
                featureType = ft.get();
                break;
            }
        }
        return featureType;
    }
}
