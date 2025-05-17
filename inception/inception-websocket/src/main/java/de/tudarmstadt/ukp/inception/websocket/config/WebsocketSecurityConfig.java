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

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.MessageExpressionAuthorizationManager.expression;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_USER;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_DOCUMENT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_USER;
import static org.springframework.messaging.simp.SimpMessageType.DISCONNECT;
import static org.springframework.messaging.simp.SimpMessageType.SUBSCRIBE;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.expression.DefaultMessageSecurityExpressionHandler;
import org.springframework.security.messaging.access.expression.MessageAuthorizationContextSecurityExpressionHandler;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

import de.tudarmstadt.ukp.clarin.webanno.security.ExtensiblePermissionEvaluator;

@ConditionalOnWebApplication
@Configuration
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableWebSocketSecurity
public class WebsocketSecurityConfig
{
    @Bean
    AuthorizationManager<Message<?>> authorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages,
            ExtensiblePermissionEvaluator aEval, ApplicationContext aContext)
    {
        final var annotationEditorTopic = "/**" + TOPIC_ELEMENT_PROJECT + "{" + PARAM_PROJECT + "}"
                + TOPIC_ELEMENT_DOCUMENT + "{" + PARAM_DOCUMENT + "}" + TOPIC_ELEMENT_USER + "{"
                + PARAM_USER + "}/**";

        final var recommenderEventsTopic = "/**" + TOPIC_ELEMENT_PROJECT + "{" + PARAM_PROJECT + "}"
                + TOPIC_ELEMENT_USER + "{" + PARAM_USER + "}/**";

        var msgSecurityExpressionHandler = new DefaultMessageSecurityExpressionHandler();
        msgSecurityExpressionHandler.setApplicationContext(aContext);
        msgSecurityExpressionHandler.setPermissionEvaluator(aEval);
        var mah = new MessageAuthorizationContextSecurityExpressionHandler(
                msgSecurityExpressionHandler);

        // @formatter:off
        messages //
            //.expressionHandler(handler)
            // allow everyone to disconnect
            .simpTypeMatchers(DISCONNECT).permitAll()
            // messages other than MESSAGE,SUBSCRIBE are allowed for authenticated users
            .nullDestMatcher().authenticated() //
            .simpSubscribeDestMatchers("/*/errors*").hasRole("USER")
            .simpSubscribeDestMatchers("/*/scheduler/user").hasRole("USER")
            .simpSubscribeDestMatchers("/*/scheduler" + TOPIC_ELEMENT_PROJECT + "{" + PARAM_PROJECT + "}")
                .access(expression(mah, "@projectAccess.canManageProject(#" + PARAM_PROJECT + ")"))
            .simpSubscribeDestMatchers("/*" + NS_PROJECT + "/{" + PARAM_PROJECT + "}/exports")
                .access(expression(mah, "@projectAccess.canManageProject(#" + PARAM_PROJECT + ")"))
            .simpSubscribeDestMatchers(annotationEditorTopic)
                .access(expression(mah, "@documentAccess.canViewAnnotationDocument(#" + PARAM_PROJECT + 
                        ", #" + PARAM_DOCUMENT + ", #" + PARAM_USER + ")"))
            .simpSubscribeDestMatchers(recommenderEventsTopic)
                .access(expression(mah, "@projectAccess.canAccessProject(#" + PARAM_PROJECT + ") and "
                        + "@userAccess.isUser(#" + PARAM_USER + ")"))
            .simpDestMatchers("/*/assistant" + TOPIC_ELEMENT_PROJECT + "{" + PARAM_PROJECT + "}")
                .access(expression(mah, "@projectAccess.canAccessProject(#" + PARAM_PROJECT + ")"))
            // authenticated users can subscribe
            .simpTypeMatchers(SUBSCRIBE).authenticated()
            // authenticated clients can send messages
            .simpMessageDestMatchers(annotationEditorTopic)
                .access(expression(mah, "@documentAccess.canEditAnnotationDocument(#" + PARAM_PROJECT + 
                    ", #" + PARAM_DOCUMENT + ", #" + PARAM_USER + ")"))
            // permissions for export canceling are currently managed in the controller
            .simpMessageDestMatchers("/**/export/*/cancel").hasRole("USER")
            // all other messages are denied (if you later want users to send messages,
            // you need to allow it for specific channels)
            .anyMessage().denyAll();
        // @formatter:on

        return messages.build();
    }
}
