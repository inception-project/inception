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
package de.tudarmstadt.ukp.inception.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@Component
@ConditionalOnProperty({"websocket.enabled", "websocket.loggedevent.enabled"})
public class LoggedEventMessageSubscriptionInterceptor implements InboundChannelInterceptor
{
    private final static Logger log = LoggerFactory.getLogger(LoggedEventMessageSubscriptionInterceptor.class);
            
    private UserDao userRepo;
    
    public LoggedEventMessageSubscriptionInterceptor(@Autowired UserDao aUserRepo) {
        userRepo = aUserRepo;
    }

    @Override
    public Message<?> preSend(Message<?> aMessage, MessageChannel aChannel)
    {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(aMessage, StompHeaderAccessor.class);

        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                || !LoggedEventMessageControllerImpl.LOGGED_EVENTS_TOPIC.equals(accessor.getDestination())) {
            return aMessage;
        }
        
        String subscribingUsername = accessor.getUser().getName();
        User subscribingUser = userRepo.get(subscribingUsername);
        if (subscribingUser == null || !userRepo.isAdministrator(subscribingUser)) {
            // only logging server-side, will not inform client; 
            // throwing exception would do this (with current configuration
            // in controller), but also close the connection
            log.debug(String.format("User %s is not permitted to subscribe to channel: %s", subscribingUsername,
                    accessor.getDestination()));
            return null;
        }
        
        return aMessage;
    }

}
