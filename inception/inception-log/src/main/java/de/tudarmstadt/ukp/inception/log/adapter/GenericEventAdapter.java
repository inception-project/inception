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
package de.tudarmstadt.ukp.inception.log.adapter;

import java.util.Set;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.security.access.event.AbstractAuthorizationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authorization.event.AuthorizationEvent;
import org.springframework.security.core.session.SessionCreationEvent;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

public class GenericEventAdapter
    implements EventLoggingAdapter<ApplicationEvent>
{
    public static final GenericEventAdapter INSTANCE = new GenericEventAdapter();

    private static final Set<Class<?>> UNLOGGED_EVENTS = Set.of( //
            ApplicationContextEvent.class, //
            ServletRequestHandledEvent.class, //
            SessionCreationEvent.class, //
            SessionDestroyedEvent.class, //
            AuthorizationEvent.class, //
            AbstractAuthorizationEvent.class, //
            AbstractAuthenticationEvent.class, //
            WebServerInitializedEvent.class, //
            // Websocket events
            SessionConnectedEvent.class, //
            SessionConnectEvent.class, //
            SessionDisconnectEvent.class, //
            SessionSubscribeEvent.class, //
            SessionUnsubscribeEvent.class);

    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return ApplicationEvent.class.isAssignableFrom(aEvent)
                && UNLOGGED_EVENTS.stream().noneMatch($ -> $.isAssignableFrom(aEvent));
    }
}
