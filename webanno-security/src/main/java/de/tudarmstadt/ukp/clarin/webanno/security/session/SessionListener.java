/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
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
package de.tudarmstadt.ukp.clarin.webanno.security.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

/**
 * Listens to HTTP sessions being created and destroyed and (un)registers accordingly in the
 * {@link SessionRegistry}. This is mainly required when using pre-authentication since the login
 * page usually takes care of registering the session.
 */
@Component
public class SessionListener
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SessionRegistry sessionRegistry;

    @Autowired
    public SessionListener(SessionRegistry aSessionRegistry)
    {
        sessionRegistry = aSessionRegistry;
    }

    @EventListener
    public void onEvent(ApplicationEvent e)
    {
        // log.trace(e.toString());
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onSessionCreated(HttpSessionCreatedEvent aEvent)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.trace("Session created for anonymous user [{}]", aEvent.getSession().getId());
            // We don't register anonymous un-authorized sessions.
            // If this were a pre-authenticated session, we'd have an authentication by now.
            // If it is using the form-based login, the login page handles registering the
            // session.
            return;
        }

        String username = authentication.getName();
        log.trace("Session created for user [{}] [{}]", username, aEvent.getSession().getId());
        sessionRegistry.registerNewSession(aEvent.getSession().getId(), username);
    }

    @EventListener
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void onSessionDestroyed(HttpSessionDestroyedEvent aEvent)
    {
        if (aEvent.getSecurityContexts().isEmpty()) {
            log.trace("Session destroyed for anonymous user [{}]", aEvent.getSession().getId());
            return;
        }

        String username = aEvent.getSecurityContexts().stream().findFirst()
                .map(SecurityContext::getAuthentication).map(Authentication::getName)
                .orElse("<UNKNOWN>");
        log.trace("Session destroyed for user [{}] [{}]", username, aEvent.getSession().getId());
        sessionRegistry.removeSessionInformation(aEvent.getSession().getId());
    }
}
