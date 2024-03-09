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

import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.giffing.wicket.spring.boot.starter.configuration.extensions.core.csrf.CsrfAttacksPreventionProperties;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WebsocketAutoConfiguration#websocketConfig}.
 * </p>
 */
public class WebsocketConfig
    implements WebSocketMessageBrokerConfigurer
{
    public static final String WS_ENDPOINT = "/ws";

    private final CsrfAttacksPreventionProperties csrfProperties;

    public WebsocketConfig()
    {
        csrfProperties = null;
    }

    public WebsocketConfig(CsrfAttacksPreventionProperties aCsrfProperties)
    {
        csrfProperties = aCsrfProperties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry aRegistry)
    {
        // client will use this endpoint to first establish connection
        var registration = aRegistry.addEndpoint(WS_ENDPOINT);

        if (csrfProperties != null) {
            registration.setAllowedOriginPatterns(
                    csrfProperties.getAcceptedOrigins().toArray(String[]::new));
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry aRegistry)
    {
        // broker will send to destinations with this prefix, queue is custom for user-specific
        // channels. client will subscribe to /queue/{subtopic} where subtopic is a specific topic
        // that controller or service will address messages to
        aRegistry.enableSimpleBroker("/queue/", "/topic/");
        // clients should send messages to channels pre-fixed with this
        aRegistry.setApplicationDestinationPrefixes("/app/");
        aRegistry.setUserDestinationPrefix("/user/");
        // messages to clients are by default not ordered, need to explicitly set order here
        aRegistry.setPreservePublishOrder(true);
    }
}
