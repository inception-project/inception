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
package de.tudarmstadt.ukp.inception.experimental.api.websocket;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


@Configuration
@ConditionalOnProperty(prefix = "ui.editor.experimental", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableWebSocketMessageBroker
public class WebsocketConfig
    implements WebSocketMessageBrokerConfigurer
{
    public static final String WS_ENDPOINT = "/ws";

    @Override
    public void registerStompEndpoints(StompEndpointRegistry aRegistry)
    {
        aRegistry.addEndpoint(WS_ENDPOINT).setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry aRegistry)
    {
        aRegistry.enableSimpleBroker("/queue", "/topic");
        aRegistry.setApplicationDestinationPrefixes("/app");
    }

}
