/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.log.adapter.GenericEventAdapter;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

@Component
public class EventLoggingListener implements DisposableBean
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<EventLoggingAdapter<?>> adapterProxy;
    private List<EventLoggingAdapter<?>> adapters;

    private final Map<Class<?>, EventLoggingAdapter<?>> adapterCache;

    private final EventRepository repo;

    private final ScheduledExecutorService scheduler;
    
    private final Deque<LoggedEvent> queue;
    
    private volatile boolean flushing = false;

    public EventLoggingListener(
            @Autowired EventRepository aRepo,
            @Lazy @Autowired(required = false) List<EventLoggingAdapter<?>> aAdapters)
    {
        repo = aRepo;
        adapterProxy = aAdapters;
        adapterCache = new HashedMap<>();
        
        queue = new ConcurrentLinkedDeque<>();
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> flush(), 1, 1, TimeUnit.SECONDS);
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
                log.info("Found event logging adapter: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }

        adapters = Collections.unmodifiableList(exts);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> Optional<EventLoggingAdapter<T>> getAdapter(T aEvent)
    {
        // Try to obtain the adapter from the cache
        EventLoggingAdapter<T> adapter = (EventLoggingAdapter) adapterCache.get(aEvent.getClass());

        // This happens for events generated during applications startup.
        if (adapter == null && adapters != null) {
            for (EventLoggingAdapter a : adapters) {
                if (a.accepts(aEvent)) {
                    adapter = a;
                    adapterCache.put(aEvent.getClass(), adapter);
                    break;
                }
            }
        }

        // If no adapter could be found, check if the generic adapter applies
        if (adapter == null && GenericEventAdapter.INSTANCE.accepts(aEvent)) {
            adapter = (EventLoggingAdapter<T>) GenericEventAdapter.INSTANCE;
        }

        return Optional.ofNullable(adapter);
    }
    
    @EventListener
    public void onApplicationEvent(ApplicationEvent aEvent)
    {
        Optional<EventLoggingAdapter<ApplicationEvent>> adapter = getAdapter(aEvent);

        if (adapter.isPresent()) {
            EventLoggingAdapter<ApplicationEvent> a = adapter.get();

            LoggedEvent e = new LoggedEvent();
            e.setCreated(a.getCreated(aEvent));
            e.setEvent(a.getEvent(aEvent));
            e.setUser(a.getUser(aEvent));
            e.setProject(a.getProject(aEvent));
            e.setDocument(a.getDocument(aEvent));
            e.setAnnotator(a.getAnnotator(aEvent));
            e.setDetails(a.getDetails(aEvent));
            // Add to the writing queue which gets flushed regularly by a timer
            queue.add(e);
            
            // If the queue gets too large, force a flush even if the timer is not there yet
            if (queue.size() > 1000) {
                flush();
            }
        }
    }
    
    public void flush()
    {
        // If a flushing process is already in progress, we can abort here already. No need to
        // wait for obtaining a synchronize lock. This also avoids a deadlock situation that was
        // observed when importing huge tagsets with HSQLDB.
        if (flushing) {
            return;
        }
        
        synchronized (queue) {
            try {
                flushing = true;
                
                // Fetch all items that are currently in the queue
                List<LoggedEvent> batch = new ArrayList<>();
                while (!queue.isEmpty()) {
                    batch.add(queue.pop());
                }
                
                // And dump them into the database
                repo.create(batch.toArray(new LoggedEvent[batch.size()]));
            }
            finally {
                flushing = false;
            }
        }
    }

    @Override
    public void destroy() throws Exception
    {
        // Kill the flush scheduler
        scheduler.shutdownNow();
        
        // Make sure and pending events are flushed before the application shuts down
        flush();
    }
}
