/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.recommendation.service;

import static java.util.Collections.unmodifiableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RecommenderServiceAutoConfiguration#recommenderFactoryRegistry}.
 * </p>
 */
public class RecommenderFactoryRegistryImpl
    implements RecommenderFactoryRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<RecommendationEngineFactory<?>> extensionsProxy;

    private Map<String, RecommendationEngineFactory<?>> extensions;

    public RecommenderFactoryRegistryImpl(
            @Lazy @Autowired(required = false) List<RecommendationEngineFactory<?>> aExtensions)
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
        Map<String, RecommendationEngineFactory<?>> exts = new HashMap<>();

        if (extensionsProxy != null) {
            for (RecommendationEngineFactory<?> ext : extensionsProxy) {
                log.debug("Found recommendation engine: {}",
                        ClassUtils.getAbbreviatedName(ext.getClass(), 20));
                exts.put(ext.getId(), ext);
            }
        }

        log.info("Found [{}] recommendation engines", exts.size());

        extensions = unmodifiableMap(exts);
    }

    @Override
    public List<RecommendationEngineFactory<?>> getAllFactories()
    {
        List<RecommendationEngineFactory<?>> factories = new ArrayList<>();
        factories.addAll(extensions.values());
        return Collections.unmodifiableList(factories);
    }

    @Override
    public List<RecommendationEngineFactory<?>> getFactories(AnnotationLayer aLayer,
            AnnotationFeature aFeature)
    {
        return extensions.values().stream().filter(factory -> factory.accepts(aLayer, aFeature))
                .sorted(Comparator.comparing(RecommendationEngineFactory::getName))
                .collect(Collectors.toList());
    }

    @Override
    public RecommendationEngineFactory<?> getFactory(String aId)
    {
        return extensions.get(aId);
    }
}
