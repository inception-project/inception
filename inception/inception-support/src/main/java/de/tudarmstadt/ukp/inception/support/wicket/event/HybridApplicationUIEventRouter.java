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
package de.tudarmstadt.ukp.inception.support.wicket.event;

import java.util.Optional;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Event listener which catches application events and if they are a
 * {@link HybridApplicationUIEvent} and if they were triggered with a UI request, then it also
 * forwards the event to the page in which the event was triggered. The router sends events to the
 * UI before they are send to the application. If the UI code throws an exception, the router
 * catches this and ensures that the application still receives the event.
 */
@Component
public class HybridApplicationUIEventRouter
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Order(-10)
    @EventListener
    public void routeEvent(HybridApplicationUIEvent aEvent)
    {
        try {
            // If the event was not triggered in a UI context, then do not forward the event to the
            // UI
            if (RequestCycle.get() == null) {
                return;
            }

            Optional<AjaxRequestTarget> handler = RequestCycle.get().find(AjaxRequestTarget.class);

            // If the event was not triggered in a UI context, then do not forward the event to the
            // UI
            if (!handler.isPresent()) {
                return;
            }

            // Otherwise, send the event to the page from which the UI event was triggered
            Page page = (Page) handler.get().getPage();
            try {
                page.send(page, Broadcast.BREADTH, aEvent);
            }
            catch (ReplaceHandlerException e) {
                throw e;
            }
            catch (Throwable e) {
                log.error("Exception while processing UI-routed event", e);
                page.error("Exception while processing UI-routed event: " + e.getMessage());
                handler.get().addChildren(page, IFeedback.class);
            }
        }
        catch (ReplaceHandlerException e) {
            throw e;
        }
        catch (Throwable e) {
            log.error("Unable to route event to UI", e);
        }
    }
}
