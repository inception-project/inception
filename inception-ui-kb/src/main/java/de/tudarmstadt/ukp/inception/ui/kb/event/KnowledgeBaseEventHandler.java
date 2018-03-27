/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.ui.kb.event;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.wicket.event.IEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxPayloadCallback;
import de.tudarmstadt.ukp.inception.ui.kb.EventListeningPanel;

/**
 * This class receives Wicket {@link IEvent}s which are dispatched to event-payload-specific
 * {@link Consumer}s. While this class does not avoid {@code instanceof}-switch constructs for
 * Wicket events completely, it at least keeps the ugly code confined in a single class (in this
 * class).
 */
public class KnowledgeBaseEventHandler
    implements Serializable
{
    private static final long serialVersionUID = -870324433332005590L;

    private static final Logger LOG = LoggerFactory.getLogger(EventListeningPanel.class);

    protected Map<Class<? extends AjaxEvent>, AjaxPayloadCallback> callbacks;

    public KnowledgeBaseEventHandler()
    {
        callbacks = new HashMap<>();
    }

    public <T extends AjaxEvent> void addCallback(Class<T> clazz, AjaxPayloadCallback<T> callback)
    {
        callbacks.put(clazz, callback);
    }

    /**
     * Dispatches a given {@code IEvent} to the respective {@code AjaxPayloadCallback}. If there is
     * no {@code AjaxPayloadCallback} for a certain event, no action is performed.
     * 
     * @param event
     *            a Wicket {@code IEvent<?>}
     */
    public void onEvent(IEvent<?> event)
    {
        callbacks.entrySet().stream().filter(entry -> entry.getKey().isInstance(event.getPayload()))
                .map(entry -> entry.getValue()).findAny().ifPresent(callback -> {
                    AjaxEvent ajEvent = (AjaxEvent) event.getPayload();
                    try {
                        callback.accept(ajEvent.getTarget(), ajEvent);
                    }
                    catch (Exception e) {
                        LOG.error("Exception while processing event.", e);
                    }
                });
    }
}
