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
package de.tudarmstadt.ukp.inception.externalsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.inception.externalsearch.config.ExternalSearchAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;

/**
 * Implementation of the external search provider registry API.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ExternalSearchAutoConfiguration#externalSearchService}.
 * </p>
 */
public class ExternalSearchProviderRegistryImpl
    implements ExternalSearchProviderRegistry, BeanPostProcessor
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<ExternalSearchProviderFactory<?>> providersProxy;

    private List<ExternalSearchProviderFactory<?>> providers;

    public ExternalSearchProviderRegistryImpl(
            @Lazy @Autowired(required = false) List<ExternalSearchProviderFactory<?>> aProviders)
    {
        providersProxy = aProviders;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    /* package private */ void init()
    {
        List<ExternalSearchProviderFactory<?>> exts = new ArrayList<>();

        if (providersProxy != null) {
            exts.addAll(providersProxy);
            AnnotationAwareOrderComparator.sort(exts);

            for (ExternalSearchProviderFactory<?> fs : exts) {
                log.debug("Found external search provider: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }

        BaseLoggers.BOOT_LOG.info("Found [{}] external search providers", exts.size());

        providers = Collections.unmodifiableList(exts);
    }

    @Override
    public List<ExternalSearchProviderFactory<?>> getExternalSearchProviderFactories()
    {
        return providers;
    }

    @Override
    public ExternalSearchProviderFactory<?> getExternalSearchProviderFactory(String aId)
    {
        if (aId == null) {
            return null;
        }
        else {
            return providers.stream() //
                    .filter(f -> aId.equals(f.getBeanName())) //
                    .findFirst() //
                    .orElse(null);
        }
    }

    @Override
    public ExternalSearchProviderFactory<?> getDefaultExternalSearchProviderFactory()
    {
        return getExternalSearchProviderFactories().get(0);
    }
}
