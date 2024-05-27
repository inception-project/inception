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

import java.io.IOException;
import java.util.Set;

import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.log.model.SessionDetails;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Component
public class HttpSessionDestroyedEventAdapter
    implements EventLoggingAdapter<HttpSessionDestroyedEvent>
{
    private static final Set<String> IGNORE_USERS = Set.of(ANONYMOUS_USER, SYSTEM_USER);

    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return HttpSessionDestroyedEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public boolean isLoggable(HttpSessionDestroyedEvent aEvent)
    {
        var user = getUser((HttpSessionDestroyedEvent) aEvent);
        if (IGNORE_USERS.contains(user)) {
            return false;
        }

        return true;
    }

    @Override
    public String getEvent(HttpSessionDestroyedEvent aEvent)
    {
        return "UserSessionEndedEvent";
    }

    @Override
    public String getUser(HttpSessionDestroyedEvent aEvent)
    {
        if (aEvent.getSecurityContexts().isEmpty()) {
            return EventLoggingAdapter.super.getUser(aEvent);
        }

        return aEvent.getSecurityContexts().get(0).getAuthentication().getName();
    }

    @Override
    public String getDetails(HttpSessionDestroyedEvent aEvent) throws IOException
    {
        var age = (System.currentTimeMillis() - aEvent.getSession().getLastAccessedTime()) / 1000;

        var details = new SessionDetails(aEvent.getId());
        details.setDuration(
                aEvent.getSession().getLastAccessedTime() - aEvent.getSession().getCreationTime());
        if (age > aEvent.getSession().getMaxInactiveInterval()) {
            details.setExpiredAfterInactivity(age * 1000);
        }

        return JSONUtil.toJsonString(details);
    }
}
