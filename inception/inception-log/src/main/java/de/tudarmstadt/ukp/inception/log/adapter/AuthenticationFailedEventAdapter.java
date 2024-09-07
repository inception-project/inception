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

import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFailedEventAdapter
    implements EventLoggingAdapter<AbstractAuthenticationFailureEvent>
{
    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return AbstractAuthenticationFailureEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public String getEvent(AbstractAuthenticationFailureEvent aEvent)
    {
        return aEvent.getClass().getSimpleName();
    }

    @Override
    public String getUser(AbstractAuthenticationFailureEvent aEvent)
    {
        return aEvent.getAuthentication().getName();
    }
}
