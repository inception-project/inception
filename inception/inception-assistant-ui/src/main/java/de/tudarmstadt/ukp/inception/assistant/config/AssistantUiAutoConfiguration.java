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
package de.tudarmstadt.ukp.inception.assistant.config;

import static de.tudarmstadt.ukp.inception.websocket.config.MessageExpressionAuthorizationManager.expression;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_PROJECT;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.assistant.contextmenu.CheckAnnotationContextMenuItem;
import de.tudarmstadt.ukp.inception.assistant.sidebar.AssistantSidebarFactory;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.websocket.security.StompSecurityConfigurer;

@ConditionalOnWebApplication
@Configuration
@ConditionalOnProperty(prefix = "assistant", name = "enabled", havingValue = "true", matchIfMissing = false)
public class AssistantUiAutoConfiguration
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Bean
    public AssistantSidebarFactory assistantSidebarFactory()
    {
        return new AssistantSidebarFactory();
    }

    @Bean
    public CheckAnnotationContextMenuItem checkAnnotationContextMenuItem(
            SchedulingService aSchedulingService, AssistantSidebarFactory aAssistantSidebarFactory,
            AnnotationSchemaService aSchemaService, UserDao aUserService)
    {
        return new CheckAnnotationContextMenuItem(aSchedulingService, aAssistantSidebarFactory,
                aSchemaService, aUserService);
    }

    @Bean
    public StompSecurityConfigurer assistantWebsocketSecurity()
    {
        return (aBuilder, aMAH) -> {
            LOG.debug("Configuring websocket security for assistant controller");

            aBuilder.simpDestMatchers(
                    "/*/assistant" + TOPIC_ELEMENT_PROJECT + "{" + PARAM_PROJECT + "}")
                    .access(expression(aMAH,
                            "@projectAccess.canAccessProject(#" + PARAM_PROJECT + ")"));
        };
    }
}
