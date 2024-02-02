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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.availability.AvailabilityChangeEvent;

import de.tudarmstadt.ukp.inception.annotation.events.BeforeDocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.PreparingToOpenDocumentEvent;
import de.tudarmstadt.ukp.inception.documents.event.AfterCasWrittenEvent;

class EventLoggingPropertiesImplTest
{

    private EventLoggingPropertiesImpl properties;

    @BeforeEach
    public void setUp()
    {
        properties = new EventLoggingPropertiesImpl();
    }

    @Test
    public void shouldLogEvent_defaultExcludeInternalList_ReturnsFalse()
    {
        // Test events
        String eventAfterCasWritten = AfterCasWrittenEvent.class.getSimpleName();
        String eventAvailabilityChange = AvailabilityChangeEvent.class.getSimpleName();
        String eventRecommenderTaskNotification = "RecommenderTaskNotificationEvent";
        String eventBeforeDocumentOpened = BeforeDocumentOpenedEvent.class.getSimpleName();
        String eventPreparingToOpenDocument = PreparingToOpenDocumentEvent.class.getSimpleName();
        String eventBrokerAvailability = "BrokerAvailabilityEvent";
        String eventShutdownDialogAvailable = "ShutdownDialogAvailableEvent";

        // Assert
        assertThat(properties.shouldLogEvent(eventAfterCasWritten)).isFalse();
        assertThat(properties.shouldLogEvent(eventAvailabilityChange)).isFalse();
        assertThat(properties.shouldLogEvent(eventRecommenderTaskNotification)).isFalse();
        assertThat(properties.shouldLogEvent(eventBeforeDocumentOpened)).isFalse();
        assertThat(properties.shouldLogEvent(eventPreparingToOpenDocument)).isFalse();
        assertThat(properties.shouldLogEvent(eventBrokerAvailability)).isFalse();
        assertThat(properties.shouldLogEvent(eventShutdownDialogAvailable)).isFalse();
    }

    @Test
    public void shouldLogEvent_EventNotInExcludeLists_ReturnsTrue()
    {
        // Test events
        String eventAfterDocumentOpened = "AfterDocumentOpenedEvent";

        // Assert
        assertThat(properties.shouldLogEvent(eventAfterDocumentOpened)).isTrue();
        assertThat(properties.getExcludePatterns()).doesNotContain(eventAfterDocumentOpened);
    }

    @Test
    public void shouldLogEvent_setExcludeWorksAndEventsGetExcludedTrue()
    {
        // Set exclude patterns
        properties.setExcludePatterns(Set.of("AfterDocumentOpenedEvent"));

        // Test events
        String eventAfterDocumentOpened = "AfterDocumentOpenedEvent";

        // Assert
        assertThat(properties.shouldLogEvent(eventAfterDocumentOpened)).isFalse();
    }

    @Test
    public void shouldLogEvent_setIncludeWorksAndOnlyEventsSetIncludedWork()
    {
        // Set include patterns
        properties.setIncludePatterns(Set.of("AfterDocumentOpenedEvent"));

        // Test events
        String eventAfterDocumentOpened = "AfterDocumentOpenedEvent";
        String eventDocumentStateChanged = "DocumentStateChangedEvent";

        // Assert
        assertThat(properties.shouldLogEvent(eventAfterDocumentOpened)).isTrue();
        assertThat(properties.shouldLogEvent(eventDocumentStateChanged)).isFalse();
    }

}
