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

import static org.springframework.messaging.simp.SimpMessageType.DISCONNECT;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.messaging.Message;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.expression.DefaultMessageSecurityExpressionHandler;
import org.springframework.security.messaging.access.expression.MessageAuthorizationContextSecurityExpressionHandler;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

import de.tudarmstadt.ukp.clarin.webanno.security.ExtensiblePermissionEvaluator;
import de.tudarmstadt.ukp.inception.websocket.security.StompSecurityConfigurer;

@ConditionalOnWebApplication
@Configuration
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableWebSocketSecurity
public class WebsocketSecurityConfig
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Bean
    AuthorizationManager<Message<?>> authorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages,
            ExtensiblePermissionEvaluator aEval, ApplicationContext aContext,
            List<StompSecurityConfigurer> aModuleConfigurers)
    {
        var msgSecurityExpressionHandler = new DefaultMessageSecurityExpressionHandler<>();
        msgSecurityExpressionHandler.setApplicationContext(aContext);
        msgSecurityExpressionHandler.setPermissionEvaluator(aEval);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        var mah = new MessageAuthorizationContextSecurityExpressionHandler(
                (SecurityExpressionHandler) msgSecurityExpressionHandler);

        // allow everyone to disconnect
        messages.simpTypeMatchers(DISCONNECT).permitAll();

        // messages other than MESSAGE,SUBSCRIBE are allowed for authenticated users
        messages.nullDestMatcher().authenticated();

        // allow authenticated users with USER role to subscribe to error messages
        messages.simpSubscribeDestMatchers("/user/queue/errors*") //
                .hasRole("USER");

        // Apply module-provided configurers (if any) using the same shared expression handler
        if (aModuleConfigurers != null && !aModuleConfigurers.isEmpty()) {
            LOG.debug("Starting to configure websocket authorization beans...");
            AnnotationAwareOrderComparator.sort(aModuleConfigurers);
            for (var cfg : aModuleConfigurers) {
                cfg.configure(messages, mah);
            }
            LOG.debug("Done configuring websocket authorization beans");
        }

        // all other messages are denied
        messages.anyMessage().denyAll();

        return messages.build();
    }
}
