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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.LegacyRemoteApiController;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroRemoteApiController;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhookService;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhooksConfiguration;
import io.swagger.v3.oas.models.info.Info;

@ConditionalOnWebApplication
@Configuration
@EnableConfigurationProperties({ RemoteApiProperties.class, WebhooksConfiguration.class })
public class RemoteApiAutoConfiguration
{
    static final String REMOTE_API_ENABLED_CONDITION = "${remote-api.enabled:false} || ${webanno.remote-api.enable:false}";

    @ConditionalOnExpression(RemoteApiAutoConfiguration.REMOTE_API_ENABLED_CONDITION)
    @Bean
    public AeroRemoteApiController aeroRemoteApiController()
    {
        return new AeroRemoteApiController();
    }

    @ConditionalOnExpression(RemoteApiAutoConfiguration.REMOTE_API_ENABLED_CONDITION)
    @Bean
    public LegacyRemoteApiController legacyRemoteApiController()
    {
        return new LegacyRemoteApiController();
    }

    @ConditionalOnExpression("!(" + REMOTE_API_ENABLED_CONDITION + ")")
    @Bean
    public GroupedOpenApi defaultDocket()
    {
        return GroupedOpenApi.builder().group("disabled") //
                .pathsToExclude("/**") //
                .addOpenApiCustomiser(openApi -> { //
                    openApi.info(new Info() //
                            .title("Remote API disabled") //
                            .description("The remote API is not enabled."));
                })//
                .build();
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public GroupedOpenApi legacyRemoteApiDocket()
    {
        return GroupedOpenApi.builder().group("legacy-v1")
                .pathsToMatch(LegacyRemoteApiController.API_BASE + "/**") //
                .addOpenApiCustomiser(openApi -> { //
                    openApi.info(new Info() //
                            .title("Legacy API") //
                            .version("1"));
                }) //
                .build();
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public GroupedOpenApi areoRemoteApiDocket()
    {
        return GroupedOpenApi.builder().group("aero-v1")
                .pathsToMatch(AeroRemoteApiController.API_BASE + "/**")
                .addOpenApiCustomiser(openApi -> { //
                    openApi.info(new Info() //
                            .title("AERO") //
                            .version("1.0.0")
                            .description(String.join(" ",
                                    "Annotation Editor Remote Operations API. ",
                                    "https://openminted.github.io/releases/aero-spec/1.0.0/omtd-aero/")));
                }) //
                .build();
    }

    @Bean
    public WebhookService webhookService(WebhooksConfiguration aConfiguration,
            RestTemplateBuilder aRestTemplateBuilder)
        throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException
    {
        return new WebhookService(aConfiguration, aRestTemplateBuilder);
    }
}
