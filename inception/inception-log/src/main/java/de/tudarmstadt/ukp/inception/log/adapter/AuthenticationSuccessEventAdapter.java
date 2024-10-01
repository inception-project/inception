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

import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import de.tudarmstadt.ukp.inception.log.model.SessionDetails;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Component
public class AuthenticationSuccessEventAdapter
    implements EventLoggingAdapter<AuthenticationSuccessEvent>
{
    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return AuthenticationSuccessEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public String getEvent(AuthenticationSuccessEvent aEvent)
    {
        return "UserSessionStartedEvent";
    }

    @Override
    public String getUser(AuthenticationSuccessEvent aEvent)
    {
        return aEvent.getAuthentication().getName();
    }

    @Override
    public String getDetails(AuthenticationSuccessEvent aEvent) throws IOException
    {
        try {
            var sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();

            SessionDetails details = new SessionDetails(sessionId);

            return JSONUtil.toJsonString(details);
        }
        catch (Exception e) {
            // Ignore
        }

        return null;
    }
}
