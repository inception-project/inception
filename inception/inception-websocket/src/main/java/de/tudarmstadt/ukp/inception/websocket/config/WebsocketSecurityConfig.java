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
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_USER;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_DOCUMENT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_USER;
import static java.util.Arrays.asList;
import static org.springframework.messaging.simp.SimpMessageType.DISCONNECT;
import static org.springframework.messaging.simp.SimpMessageType.SUBSCRIBE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.access.expression.AbstractSecurityExpressionHandler;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.messaging.access.expression.DefaultMessageSecurityExpressionHandler;

import de.tudarmstadt.ukp.clarin.webanno.security.ExtensiblePermissionEvaluator;

@Configuration
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebsocketSecurityConfig
    extends AbstractSecurityWebSocketMessageBrokerConfigurer
{
    private final AbstractSecurityExpressionHandler<Message<Object>> handler = //
            new DefaultMessageSecurityExpressionHandler<>();

    @Autowired
    public WebsocketSecurityConfig(ApplicationContext aContext,
            ExtensiblePermissionEvaluator aPermissionEvaluator)
    {
        handler.setPermissionEvaluator(aPermissionEvaluator);
        handler.setApplicationContext(aContext);
        setMessageExpressionHandler(asList(handler));
    }

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry aSecurityRegistry)
    {
        final var annotationEditorTopic = "/**" + TOPIC_ELEMENT_PROJECT + "{" + PARAM_PROJECT + "}"
                + TOPIC_ELEMENT_DOCUMENT + "{" + PARAM_DOCUMENT + "}" + TOPIC_ELEMENT_USER + "{"
                + PARAM_USER + "}/**";

        // @formatter:off
        aSecurityRegistry //
            .expressionHandler(handler)
            // allow everyone to disconnect
            .simpTypeMatchers(DISCONNECT).permitAll()
            // messages other than MESSAGE,SUBSCRIBE are allowed for authenticated users
            .nullDestMatcher().authenticated() //
            // subscribing to logged events is only for admins
            .simpSubscribeDestMatchers("/*/loggedEvents").hasRole("ADMIN")
            .simpSubscribeDestMatchers("/*" + NS_PROJECT + "/{" + PARAM_PROJECT + "}/exports")
                .access("@projectAccess.canManageProject(#" + PARAM_PROJECT + ")")
            .simpSubscribeDestMatchers(annotationEditorTopic)
                .access("@documentAccess.canViewAnnotationDocument(#" + PARAM_PROJECT + 
                        ", #" + PARAM_DOCUMENT + ", #" + PARAM_USER + ")")
            // authenticated users can subscribe
            .simpTypeMatchers(SUBSCRIBE).authenticated()
            // authenticated clients can send messages
            .simpMessageDestMatchers(annotationEditorTopic)
                .access("@documentAccess.canEditAnnotationDocument(#" + PARAM_PROJECT + 
                    ", #" + PARAM_DOCUMENT + ", #" + PARAM_USER + ")")
            // permissions for export canceling are currently managed in the controller
            .simpMessageDestMatchers("/**/export/*/cancel").hasRole("USER")
            // all other messages are denied (if you later want users to send messages,
            // you need to allow it for specific channels)
            .anyMessage().denyAll();
        // @formatter:on
    }

    // disable check for CSRF token, TODO delete if this becomes available
    @Override
    protected boolean sameOriginDisabled()
    {
        return true;
    }
}
