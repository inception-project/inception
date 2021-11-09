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

import static java.util.Arrays.asList;
import static org.springframework.messaging.simp.SimpMessageType.DISCONNECT;
import static org.springframework.messaging.simp.SimpMessageType.MESSAGE;
import static org.springframework.messaging.simp.SimpMessageType.SUBSCRIBE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.messaging.access.expression.DefaultMessageSecurityExpressionHandler;

import de.tudarmstadt.ukp.clarin.webanno.security.ExtensiblePermissionEvaluator;

@Configuration
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebsocketSecurityConfig
    extends AbstractSecurityWebSocketMessageBrokerConfigurer
{
    private final DefaultMessageSecurityExpressionHandler handler = new DefaultMessageSecurityExpressionHandler();

    @Autowired
    public WebsocketSecurityConfig(ExtensiblePermissionEvaluator aPermissionEvaluator)
    {
        handler.setPermissionEvaluator(aPermissionEvaluator);
        setMessageExpressionHandler(asList(handler));
    }

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry aSecurityRegistry)
    {
        aSecurityRegistry //
                .expressionHandler(handler)
                // allow everyone to disconnect
                .simpTypeMatchers(DISCONNECT).permitAll()
                // messages other than MESSAGE,SUBSCRIBE are allowed for authenticated users
                .nullDestMatcher().authenticated() //
                // subscribing to logged events is only for admins
                .simpSubscribeDestMatchers("/*/loggedEvents").hasRole("ADMIN")
                .simpSubscribeDestMatchers("/**/project/{project}/**")
                .access("hasPermission(#project, 'Project', 'ANY')")
                // authenticated users can subscribe
                .simpTypeMatchers(SUBSCRIBE).authenticated()
                // authenticated clients can send messages
                .simpTypeMatchers(MESSAGE).authenticated()
                // all other messages are denied (if you later want users to send messages,
                // you need to allow it for specific channels)
                .anyMessage().denyAll();
    }

    // disable check for CSRF token, TODO delete if this becomes available
    @Override
    protected boolean sameOriginDisabled()
    {
        return true;
    }
}
