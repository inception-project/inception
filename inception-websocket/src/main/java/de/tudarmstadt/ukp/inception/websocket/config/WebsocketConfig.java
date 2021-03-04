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
package de.tudarmstadt.ukp.inception.websocket.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.websocket.LoggedEventMessageService;
import de.tudarmstadt.ukp.inception.websocket.LoggedEventMessageServiceImpl;

@Configuration
@EnableWebSocketMessageBroker
public class WebsocketConfig
    implements WebSocketMessageBrokerConfigurer
{

    public static final String WS_ENDPOINT = "/ws-endpoint";

    @Override
    public void registerStompEndpoints(StompEndpointRegistry aRegistry)
    {
        aRegistry.addEndpoint(WS_ENDPOINT); // client will use this endpoint to first establish connection
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry aRegistry)
    {
        aRegistry.enableSimpleBroker("/queue"); // broker will send to destinations with this
                                                // prefix,
                                                // queue is custom for user-specific channels.
                                                // client will subscribe to /queue/{subtopic} where
                                                // subtopic is a specific topic that
                                                // controller or service will address messages to
//        aRegistry.setApplicationDestinationPrefixes("/app"); // clients should send messages to
//                                                             // channels pre-fixed with this
        aRegistry.setPreservePublishOrder(true); // messages to clients are by default not ordered,
                                                 // need to explicitly set order here
    }

    @Bean
    @Autowired
    public LoggedEventMessageService loggedEventMessageService(SimpMessagingTemplate aMsgTemplate, 
            @Lazy @Autowired List<EventLoggingAdapter<?>> aAdapters, 
            DocumentService aDocService, ProjectService aProjectService) {
        return new LoggedEventMessageServiceImpl(aMsgTemplate, aAdapters, aDocService, aProjectService);
    }
    
}
