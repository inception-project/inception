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
package de.tudarmstadt.ukp.inception.log;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
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

import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.log.adapter.GenericEventAdapter;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingProperties;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.support.spring.StartupProgressInfoEvent;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link EventLoggingAutoConfiguration#eventLoggingListener}.
 * </p>
 */
public class EventLoggingListener
    implements DisposableBean
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<EventLoggingAdapter<?>> adapterProxy;
    private List<EventLoggingAdapter<?>> adapters;

    private final Map<Class<?>, EventLoggingAdapter<?>> adapterCache;

    private final EventRepository repo;

    private final ScheduledExecutorService scheduler;

    private final Deque<LoggedEvent> queue;

    private final EventLoggingProperties properties;

    private volatile boolean flushing = false;

    public EventLoggingListener(@Autowired EventRepository aRepo,
            @Lazy @Autowired(required = false) List<EventLoggingAdapter<?>> aAdapters,
            EventLoggingProperties aProperties)
    {
        repo = aRepo;
        adapterProxy = aAdapters;
        adapterCache = new HashedMap<>();
        properties = aProperties;

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
                log.debug("Found event logging adapter: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }

        log.info("Found [{}] event logging adapters", exts.size());

        adapters = unmodifiableList(exts);
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
        if (aEvent instanceof StartupProgressInfoEvent) {
            return;
        }

        Optional<EventLoggingAdapter<ApplicationEvent>> adapter = getAdapter(aEvent);

        if (adapter.isPresent()) {
            EventLoggingAdapter<ApplicationEvent> a = adapter.get();

            if (properties.getExcludeEvents().contains(a.getEvent(aEvent))) {
                return;
            }

            LoggedEvent e;

            try {
                e = a.toLoggedEvent(aEvent);
            }
            catch (Exception ex) {
                log.error("Unable to log event [{}]", aEvent, ex);
                return;
            }

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
