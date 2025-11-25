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

import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.SPEL_IS_ADMIN_ACCOUNT_RECOVERY_MODE;

import java.security.GeneralSecurityException;
import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroAnnotationController;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroCurationController;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroDocumentController;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroKnowledgeBaseController;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroProjectController;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.legacy.LegacyRemoteApiController;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.menubar.SwaggerUiMenuBarItemSupport;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.HttpWebhookDriver;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.KafkaWebhookDriver;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhookDriver;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhookService;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhooksConfiguration;
import de.tudarmstadt.ukp.inception.remoteapi.Controller_ImplBase;
import de.tudarmstadt.ukp.inception.remoteapi.next.AnnotationController;
import de.tudarmstadt.ukp.inception.remoteapi.next.PermissionController;
import de.tudarmstadt.ukp.inception.remoteapi.next.TaskBulkPredictionController;
import de.tudarmstadt.ukp.inception.remoteapi.next.TaskController;
import de.tudarmstadt.ukp.inception.remoteapi.next.UserController;
import io.swagger.v3.oas.models.info.Info;

@ConditionalOnWebApplication
@Configuration
@EnableConfigurationProperties({ RemoteApiProperties.class, WebhooksConfiguration.class })
public class RemoteApiAutoConfiguration
{
    static final String REMOTE_API_ENABLED_CONDITION = //
            "(${remote-api.enabled:false} || ${webanno.remote-api.enable:false}) && !"
                    + SPEL_IS_ADMIN_ACCOUNT_RECOVERY_MODE;

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public AeroProjectController aeroRemoteApiController()
    {
        return new AeroProjectController();
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public PermissionController aeroPermissionController()
    {
        return new PermissionController();
    }

    @ConditionalOnExpression("(" + REMOTE_API_ENABLED_CONDITION + ")"
            + "&& ${remote-api.tasks.enabled:false}")
    @Bean
    public TaskController aeroTaskController()
    {
        return new TaskController();
    }

    @ConditionalOnExpression("(" + REMOTE_API_ENABLED_CONDITION + ")"
            + "&& ${remote-api.tasks.enabled:false}"
            + "&& ${remote-api.tasks.bulk-prediction.enabled:false}")
    @Bean
    public TaskBulkPredictionController aeroBulkPredictionSubmissionController()
    {
        return new TaskBulkPredictionController();
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public AeroDocumentController aeroDocumentController()
    {
        return new AeroDocumentController();
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public AnnotationController annotationController()
    {
        return new AnnotationController();
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public AeroAnnotationController aeroAnnotationController()
    {
        return new AeroAnnotationController();
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public AeroCurationController aeroCurationController()
    {
        return new AeroCurationController();
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public LegacyRemoteApiController legacyRemoteApiController()
    {
        return new LegacyRemoteApiController();
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public AeroKnowledgeBaseController aeroKnowledgeBaseController()
    {
        return new AeroKnowledgeBaseController();
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public UserController userController()
    {
        return new UserController();
    }

    @ConditionalOnExpression("!(" + REMOTE_API_ENABLED_CONDITION + ")")
    @Bean
    public GroupedOpenApi defaultDocket()
    {
        return GroupedOpenApi.builder().group("disabled") //
                .pathsToExclude("/**") //
                .addOpenApiCustomizer(openApi -> { //
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
                .addOpenApiCustomizer(openApi -> { //
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
                .pathsToMatch(Controller_ImplBase.API_BASE + "/**")
                .addOpenApiCustomizer(openApi -> { //
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
            @Lazy @Autowired(required = false) List<WebhookDriver> aExtensions)
        throws GeneralSecurityException
    {
        return new WebhookService(aConfiguration, aExtensions);
    }

    @Bean
    public HttpWebhookDriver httpWebhookDriver(RestTemplateBuilder aRestTemplateBuilder)
        throws GeneralSecurityException
    {
        return new HttpWebhookDriver(aRestTemplateBuilder);
    }

    @Bean
    public KafkaWebhookDriver kafkaWebhookDriver()
    {
        return new KafkaWebhookDriver();
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public SwaggerUiMenuBarItemSupport swaggerUiMenuBarItemSupport()
    {
        return new SwaggerUiMenuBarItemSupport();
    }
}
