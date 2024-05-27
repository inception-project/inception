/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.inception.search;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link SearchServiceAutoConfiguration#featureIndexingSupportRegistry}.
 * </p>
 */
public class FeatureIndexingSupportRegistryImpl
    implements FeatureIndexingSupportRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<FeatureIndexingSupport> indexingSupportsProxy;

    private List<FeatureIndexingSupport> indexingSupports;

    private final Map<Long, FeatureIndexingSupport> supportCache = new HashMap<>();

    public FeatureIndexingSupportRegistryImpl(
            @Lazy @Autowired(required = false) List<FeatureIndexingSupport> aIndexingSupports)
    {
        indexingSupportsProxy = aIndexingSupports;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    public void init()
    {
        List<FeatureIndexingSupport> fsp = new ArrayList<>();

        if (indexingSupportsProxy != null) {
            fsp.addAll(indexingSupportsProxy);
            AnnotationAwareOrderComparator.sort(fsp);

            for (FeatureIndexingSupport fs : fsp) {
                log.debug("Found feature indexing support: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }

        BaseLoggers.BOOT_LOG.info("Found [{}] feature indexing supports", fsp.size());

        indexingSupports = unmodifiableList(fsp);
    }

    @Override
    public List<FeatureIndexingSupport> getFeatureSupports()
    {
        return indexingSupports;
    }

    @Override
    public Optional<FeatureIndexingSupport> getIndexingSupport(AnnotationFeature aFeature)
    {
        FeatureIndexingSupport support = null;

        if (aFeature.getId() != null) {
            support = supportCache.get(aFeature.getId());
        }

        if (support == null) {
            for (FeatureIndexingSupport s : getFeatureSupports()) {
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

        return Optional.ofNullable(support);
    }
}
