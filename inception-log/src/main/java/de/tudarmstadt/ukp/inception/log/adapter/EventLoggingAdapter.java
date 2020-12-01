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

import java.util.Date;

import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public interface EventLoggingAdapter<T>
{
    boolean accepts(Object aEvent);

    default String getDetails(T aEvent)
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
        if (aEvent instanceof ApplicationEvent) {
            return new Date(((ApplicationEvent) aEvent).getTimestamp());
        }
        else {
            return new Date();
        }
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
            return "<SYSTEM>";
        }
    }
}
