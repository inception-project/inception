/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.service;

import static org.apache.commons.lang3.Validate.notNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationToolFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationToolRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

@Component
public class ClassificationToolRegistryImpl
    implements ClassificationToolRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<ClassificationToolFactory> extensionsProxy;

    private Map<String, ClassificationToolFactory> extensions;

    public ClassificationToolRegistryImpl(
            @Lazy @Autowired(required = false) List<ClassificationToolFactory> aExtensions)
    {
        extensionsProxy = aExtensions;
    }
    
    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }
    
    /* package private */ void init()
    {
        Map<String, ClassificationToolFactory> exts = new HashMap<>();

        if (extensionsProxy != null) {
            for (ClassificationToolFactory ext : extensionsProxy) {
                log.info("Found classification tool: {}",
                        ClassUtils.getAbbreviatedName(ext.getClass(), 20));
                exts.put(ext.getId(), ext);
            }
        }
        
        extensions = Collections.unmodifiableMap(exts);
    }    
    
    @Override
    public ClassificationTool<?> createClassificationTool(Recommender aRecommender,
            int aMaxPredictions)
    {
        notNull(aRecommender);
        
        ClassificationToolFactory factory = extensions.get(aRecommender.getTool());
        if (factory == null) {
            throw new IllegalStateException(
                    "Unknown classification tool [" + aRecommender.getTool() + "]");
        }
        
        return factory.createTool(aRecommender.getId(), aRecommender.getFeature(),
            aRecommender.getLayer(), aMaxPredictions);
    }

    @Override
    public List<String> getAvailableTools(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        List<String> candidates = new ArrayList<>();
        for (ClassificationToolFactory factory : extensions.values()) {
            if (factory.accepts(aLayer, aFeature)) {
                candidates.add(factory.getId());
            }
        }
        
        Collections.sort(candidates);
        
        return candidates;
    }
    
    @Override
    public List<ClassificationToolFactory> getTools(AnnotationLayer aLayer,
            AnnotationFeature aFeature)
    {
        List<ClassificationToolFactory> tools = new ArrayList<>();
        for (ClassificationToolFactory factory : extensions.values()) {
            if (factory.accepts(aLayer, aFeature)) {
                tools.add(factory);
            }
        }
        
        tools.sort(Comparator.comparing(ClassificationToolFactory::getName));
        
        return tools;
    }
    
    @Override
    public ClassificationToolFactory getTool(String aId)
    {
        if (aId == null) {
            return null;
        }
        else {
            return extensions.values().stream().filter(ext -> aId.equals(ext.getId())).findFirst()
                    .orElse(null);
        }
    }
}
