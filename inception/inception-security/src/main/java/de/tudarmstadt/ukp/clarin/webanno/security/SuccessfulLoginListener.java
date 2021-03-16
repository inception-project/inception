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
package de.tudarmstadt.ukp.clarin.webanno.security;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@Component(SuccessfulLoginListener.SERVICE_NAME)
public class SuccessfulLoginListener
    implements ApplicationListener<ApplicationEvent>
{
    public static final String SERVICE_NAME = "successfulLoginListener";

    private @Autowired UserDao userRepository;

    @Override
    public void onApplicationEvent(ApplicationEvent aEvent)
    {
        if (aEvent instanceof AuthenticationSuccessEvent) {
            AuthenticationSuccessEvent event = (AuthenticationSuccessEvent) aEvent;
            User user = userRepository.get(event.getAuthentication().getName());
            user.setLastLogin(new Date(event.getTimestamp()));
            userRepository.update(user);
        }
    }
}
