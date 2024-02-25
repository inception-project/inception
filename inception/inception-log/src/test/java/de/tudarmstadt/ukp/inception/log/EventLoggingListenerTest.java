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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.availability.AvailabilityChangeEvent;

import de.tudarmstadt.ukp.inception.annotation.events.BeforeDocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.PreparingToOpenDocumentEvent;
import de.tudarmstadt.ukp.inception.documents.event.AfterCasWrittenEvent;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingPropertiesImpl;

class EventLoggingListenerTest
{

    private EventLoggingListener listener;
    private EventLoggingPropertiesImpl properties;

    @BeforeEach
    void setUp() throws Exception
    {
        properties = new EventLoggingPropertiesImpl();
        listener = new EventLoggingListener(null, properties, null);
    }

    @Test
    public void shouldLogEvent_defaultExcludeInternalList_ReturnsFalse()
    {
        assertThat(listener.shouldLogEvent(AfterCasWrittenEvent.class.getSimpleName())).isFalse();
        assertThat(listener.shouldLogEvent(AvailabilityChangeEvent.class.getSimpleName()))
                .isFalse();
        assertThat(listener.shouldLogEvent("RecommenderTaskNotificationEvent")).isFalse();
        assertThat(listener.shouldLogEvent(BeforeDocumentOpenedEvent.class.getSimpleName()))
                .isFalse();
        assertThat(listener.shouldLogEvent(PreparingToOpenDocumentEvent.class.getSimpleName()))
                .isFalse();
        assertThat(listener.shouldLogEvent("BrokerAvailabilityEvent")).isFalse();
        assertThat(listener.shouldLogEvent("ShutdownDialogAvailableEvent")).isFalse();
    }

    @Test
    public void shouldLogEvent_EventNotInExcludeLists_ReturnsTrue()
    {
        var eventAfterDocumentOpened = "AfterDocumentOpenedEvent";

        assertThat(listener.shouldLogEvent(eventAfterDocumentOpened)).isTrue();
        assertThat(properties.getExcludePatterns()).doesNotContain(eventAfterDocumentOpened);
    }

    @Test
    public void shouldLogEvent_setExcludeWorksAndEventsGetExcludedTrue()
    {
        var excludedEvent = "ExcludedEvent";

        properties.setExcludePatterns(Set.of(excludedEvent));

        assertThat(listener.shouldLogEvent(excludedEvent)).isFalse();
    }

    @Test
    public void shouldLogEvent_setIncludeWorksAndOnlyEventsSetIncludedWork()
    {
        var includedEvent = "IncludedEvent";
        var notIncludedEvent = "NotIncludedEvent";

        properties.setIncludePatterns(Set.of(includedEvent));

        assertThat(listener.shouldLogEvent(includedEvent)).isTrue();
        assertThat(listener.shouldLogEvent(notIncludedEvent)).isFalse();
    }
}
