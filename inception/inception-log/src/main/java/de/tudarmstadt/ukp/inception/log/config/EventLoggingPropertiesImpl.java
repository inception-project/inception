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
package de.tudarmstadt.ukp.inception.log.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;

import de.tudarmstadt.ukp.inception.annotation.events.BeforeDocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.PreparingToOpenDocumentEvent;
import de.tudarmstadt.ukp.inception.documents.event.AfterCasWrittenEvent;

@ConfigurationProperties("event-logging")
public class EventLoggingPropertiesImpl
    implements EventLoggingProperties
{
    private boolean enabled;

    private Map<String, Boolean> eventCache = new HashMap<>();

    private Set<String> includePatterns = Set.of(".*"); // Default include everything

    private Set<String> excludePatterns = Set.of(AfterCasWrittenEvent.class.getSimpleName(),
            AvailabilityChangeEvent.class.getSimpleName(), "RecommenderTaskNotificationEvent",
            BeforeDocumentOpenedEvent.class.getSimpleName(),
            PreparingToOpenDocumentEvent.class.getSimpleName(), "BrokerAvailabilityEvent",
            "ShutdownDialogAvailableEvent");

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public void setEnabled(boolean aEnabled)
    {
        enabled = aEnabled;
    }

    @Override
    public Set<String> getIncludePatterns()
    {
        return includePatterns;
    }

    @Override
    public void setIncludePatterns(Set<String> aIncludePatterns)
    {
        this.includePatterns = aIncludePatterns;
    }

    @Override
    public Set<String> getExcludePatterns()
    {
        return excludePatterns;
    }

    @Override
    public void setExcludePatterns(Set<String> aExcludePatterns)
    {
        this.excludePatterns = aExcludePatterns;
    }

    @Override
    public boolean shouldLogEvent(String aEventName)
    {
        if (eventCache.containsKey(aEventName)) {
            return eventCache.get(aEventName);
        }

        boolean shouldLog = false;

        if (includePatterns != null && !includePatterns.isEmpty()) {
            shouldLog = includePatterns.stream()
                    .anyMatch(pattern -> Pattern.matches(pattern, aEventName));
        }

        if (shouldLog && excludePatterns != null && !excludePatterns.isEmpty()) {
            shouldLog = excludePatterns.stream()
                    .noneMatch(pattern -> Pattern.matches(pattern, aEventName));
        }

        eventCache.put(aEventName, shouldLog);

        return shouldLog;
    }
}
