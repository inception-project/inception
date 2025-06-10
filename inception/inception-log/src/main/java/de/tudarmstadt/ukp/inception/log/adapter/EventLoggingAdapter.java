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

import java.util.Date;

import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

public interface EventLoggingAdapter<T>
{
    public static final String SYSTEM_USER = "<SYSTEM>";
    public static final String ANONYMOUS_USER = "anonymousUser";

    boolean accepts(Class<?> aEvent);

    default boolean isLoggable(T aEvent)
    {
        return true;
    }

    default String getDetails(T aEvent) throws Exception
    {
        return null;
    }

    default long getProject(T aEvent)
    {
        return -1;
    }

    default long getDocument(T aEvent)
    {
        return -1;
    }

    default String getAnnotator(T aEvent)
    {
        return null;
    }

    default Date getCreated(T aEvent)
    {
        if (aEvent instanceof ApplicationEvent event) {
            return new Date(event.getTimestamp());
        }

        return new Date();
    }

    default String getEvent(T aEvent)
    {
        return aEvent.getClass().getSimpleName();
    }

    default String getUser(T aEvent)
    {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null) {
            return context.getAuthentication().getName();
        }
        else {
            return SYSTEM_USER;
        }
    }

    default LoggedEvent toLoggedEvent(T aEvent) throws Exception
    {
        var e = new LoggedEvent();
        e.setCreated(getCreated(aEvent));
        e.setEvent(getEvent(aEvent));
        e.setUser(getUser(aEvent));
        e.setProject(getProject(aEvent));
        e.setDocument(getDocument(aEvent));
        e.setAnnotator(getAnnotator(aEvent));
        e.setDetails(getDetails(aEvent));
        return e;
    }
}
