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
package de.tudarmstadt.ukp.inception.log.adapter;

import static de.tudarmstadt.ukp.inception.support.logging.BaseLoggers.BOOT_LOG;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

public class EventLoggingAdapterRegistryImpl
    implements EventLoggingAdapterRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<Class<?>, EventLoggingAdapter<?>> adapterCache;

    private final List<EventLoggingAdapter<?>> adapterProxy;
    private List<EventLoggingAdapter<?>> adapters;

    public EventLoggingAdapterRegistryImpl(
            @Lazy @Autowired(required = false) List<EventLoggingAdapter<?>> aAdapters)
    {
        adapterCache = new HashedMap<>();

        adapterProxy = aAdapters;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    /* package private */ void init()
    {
        List<EventLoggingAdapter<?>> exts = new ArrayList<>();

        if (adapterProxy != null) {
            exts.addAll(adapterProxy);
            AnnotationAwareOrderComparator.sort(exts);

            for (EventLoggingAdapter<?> fs : exts) {
                log.debug("Found event logging adapter: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }

        BOOT_LOG.info("Found [{}] event logging adapters", exts.size());

        adapters = unmodifiableList(exts);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> Optional<EventLoggingAdapter<T>> getAdapter(T aEvent)
    {
        if (aEvent == null) {
            return Optional.empty();
        }

        var eventClass = aEvent.getClass();

        // Try to obtain the adapter from the cache
        var adapter = (EventLoggingAdapter) adapterCache.get(eventClass);

        // This happens for events generated during applications startup.
        if (adapter == null) {
            if (adapters != null) {
                for (var a : adapters) {
                    if (a.accepts(eventClass)) {
                        adapter = a;
                        adapterCache.put(eventClass, adapter);
                        break;
                    }
                }
            }
        }

        // If no adapter could be found, check if the generic adapter applies
        if (adapter == null && GenericEventAdapter.INSTANCE.accepts(eventClass)) {
            adapter = (EventLoggingAdapter<T>) GenericEventAdapter.INSTANCE;
        }

        return Optional.ofNullable(adapter);
    }
}
