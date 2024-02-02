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
        String eventAfterCasWritten = AfterCasWrittenEvent.class.getSimpleName();
        String eventAvailabilityChange = AvailabilityChangeEvent.class.getSimpleName();
        String eventRecommenderTaskNotification = "RecommenderTaskNotificationEvent";
        String eventBeforeDocumentOpened = BeforeDocumentOpenedEvent.class.getSimpleName();
        String eventPreparingToOpenDocument = PreparingToOpenDocumentEvent.class.getSimpleName();
        String eventBrokerAvailability = "BrokerAvailabilityEvent";
        String eventShutdownDialogAvailable = "ShutdownDialogAvailableEvent";

        assertThat(listener.shouldLogEvent(eventAfterCasWritten)).isFalse();
        assertThat(listener.shouldLogEvent(eventAvailabilityChange)).isFalse();
        assertThat(listener.shouldLogEvent(eventRecommenderTaskNotification)).isFalse();
        assertThat(listener.shouldLogEvent(eventBeforeDocumentOpened)).isFalse();
        assertThat(listener.shouldLogEvent(eventPreparingToOpenDocument)).isFalse();
        assertThat(listener.shouldLogEvent(eventBrokerAvailability)).isFalse();
        assertThat(listener.shouldLogEvent(eventShutdownDialogAvailable)).isFalse();
    }

    @Test
    public void shouldLogEvent_EventNotInExcludeLists_ReturnsTrue()
    {
        String eventAfterDocumentOpened = "AfterDocumentOpenedEvent";

        assertThat(listener.shouldLogEvent(eventAfterDocumentOpened)).isTrue();
        assertThat(properties.getExcludePatterns()).doesNotContain(eventAfterDocumentOpened);
    }

    @Test
    public void shouldLogEvent_setExcludeWorksAndEventsGetExcludedTrue()
    {
        properties.setExcludePatterns(Set.of("AfterDocumentOpenedEvent"));

        String eventAfterDocumentOpened = "AfterDocumentOpenedEvent";

        assertThat(listener.shouldLogEvent(eventAfterDocumentOpened)).isFalse();
    }

    @Test
    public void shouldLogEvent_setIncludeWorksAndOnlyEventsSetIncludedWork()
    {
        properties.setIncludePatterns(Set.of("AfterDocumentOpenedEvent"));

        String eventAfterDocumentOpened = "AfterDocumentOpenedEvent";
        String eventDocumentStateChanged = "DocumentStateChangedEvent";

        assertThat(listener.shouldLogEvent(eventAfterDocumentOpened)).isTrue();
        assertThat(listener.shouldLogEvent(eventDocumentStateChanged)).isFalse();
    }

}
