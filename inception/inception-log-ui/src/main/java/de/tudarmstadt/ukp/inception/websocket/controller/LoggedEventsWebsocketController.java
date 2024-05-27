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
package de.tudarmstadt.ukp.inception.websocket.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.inception.websocket.model.LoggedEventMessage;

public interface LoggedEventsWebsocketController
{
    /***
     * Push messages on received application events to named user
     * 
     * @param aEvent
     *            an event
     */
    public void onApplicationEvent(ApplicationEvent aEvent);

    /**
     * Return the most recent logged events to the subscribing client
     * 
     * @param aPrincipal
     *            the subscribing client
     * @return the most recent events
     */
    public List<LoggedEventMessage> getMostRecentLoggedEvents(Principal aPrincipal);

    public String handleException(Throwable exception);
}
