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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapterRegistry;
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
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EventRepository repo;
    private final ScheduledExecutorService scheduler;
    private final Deque<LoggedEvent> queue;
    private final EventLoggingProperties properties;
    private final EventLoggingAdapterRegistry adapterRegistry;

    private Map<String, Boolean> eventCache = new HashMap<>();

    private volatile boolean flushing = false;

    @Autowired
    public EventLoggingListener(EventRepository aRepo, EventLoggingProperties aProperties,
            EventLoggingAdapterRegistry aAdapterRegistry)
    {
        repo = aRepo;
        properties = aProperties;
        adapterRegistry = aAdapterRegistry;

        queue = new ConcurrentLinkedDeque<>();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> flush(), 1, 1, TimeUnit.SECONDS);
    }

    boolean shouldLogEvent(String aEventName)
    {
        if (eventCache.containsKey(aEventName)) {
            return eventCache.get(aEventName);
        }

        boolean shouldLog;

        if (CollectionUtils.isEmpty(properties.getIncludePatterns())) {
            shouldLog = true;
        }
        else {
            shouldLog = properties.getIncludePatterns().stream()
                    .anyMatch(pattern -> Pattern.matches(pattern, aEventName));
        }

        if (shouldLog && !CollectionUtils.isEmpty(properties.getExcludePatterns())) {
            shouldLog = properties.getExcludePatterns().stream()
                    .noneMatch(pattern -> Pattern.matches(pattern, aEventName));
        }

        eventCache.put(aEventName, shouldLog);

        return shouldLog;
    }

    @EventListener
    public void onApplicationEvent(ApplicationEvent aEvent)
    {
        if (aEvent instanceof StartupProgressInfoEvent) {
            return;
        }

        var maybeAdapter = adapterRegistry.getAdapter(aEvent);

        if (!maybeAdapter.isPresent()) {
            return;
        }

        var adapter = maybeAdapter.get();
        if (!adapter.isLoggable(aEvent)) {
            return;
        }

        if (!shouldLogEvent(adapter.getEvent(aEvent))) {
            return;
        }

        LoggedEvent e;

        try {
            e = adapter.toLoggedEvent(aEvent);
        }
        catch (Exception ex) {
            LOG.error("Unable to log event [{}]", aEvent, ex);
            return;
        }

        // Add to the writing queue which gets flushed regularly by a timer
        queue.add(e);

        // If the queue gets too large, force a flush even if the timer is not there yet
        if (queue.size() > 1000) {
            flush();
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
                var batch = new ArrayList<LoggedEvent>();
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

        try {
            scheduler.awaitTermination(30, SECONDS);
        }
        catch (InterruptedException e) {
            // Ignore
        }
    }
}
