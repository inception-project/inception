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

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;

import de.tudarmstadt.ukp.inception.annotation.events.BeforeDocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.PreparingToOpenDocumentEvent;
import de.tudarmstadt.ukp.inception.documents.event.AfterCasWrittenEvent;

@ConfigurationProperties("event-logging")
public class EventLoggingPropertiesImpl
    implements EventLoggingProperties
{
    /**
     * If enabled, application events are persisted to the event log so they can be reviewed later
     * (e.g. for auditing or analytics).
     */
    private boolean enabled = EventLoggingProperties.DEFAULT_ENABLED;

    /**
     * Simple names of event classes to include in the event log. If empty, all events are included
     * (subject to {@link #excludePatterns}).
     */
    private Set<String> includePatterns = Collections.emptySet(); // Default include everything

    /**
     * Simple names of event classes to exclude from the event log. Applied after
     * {@link #includePatterns} and used to suppress high-volume or uninteresting events.
     */
    private Set<String> excludePatterns = Set.of( //
            AfterCasWrittenEvent.class.getSimpleName(), //
            AvailabilityChangeEvent.class.getSimpleName(), //
            "RecommenderTaskNotificationEvent", //
            BeforeDocumentOpenedEvent.class.getSimpleName(), //
            PreparingToOpenDocumentEvent.class.getSimpleName(), //
            "BrokerAvailabilityEvent", //
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
}
