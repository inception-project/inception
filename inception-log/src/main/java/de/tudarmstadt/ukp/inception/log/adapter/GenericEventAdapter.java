/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.log.adapter;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.security.access.event.AbstractAuthorizationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.core.session.SessionCreationEvent;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.web.context.support.ServletRequestHandledEvent;

public class GenericEventAdapter
    implements EventLoggingAdapter<ApplicationEvent>
{
    public static final GenericEventAdapter INSTANCE = new GenericEventAdapter();
    
    @Override
    public boolean accepts(Object aEvent)
    {
        return aEvent instanceof ApplicationEvent && !(
                aEvent instanceof ApplicationContextEvent || 
                aEvent instanceof ServletRequestHandledEvent ||
                aEvent instanceof SessionCreationEvent ||
                aEvent instanceof SessionDestroyedEvent ||
                aEvent instanceof AbstractAuthorizationEvent ||
                aEvent instanceof AbstractAuthenticationEvent ||
                aEvent instanceof WebServerInitializedEvent);
    }
}
