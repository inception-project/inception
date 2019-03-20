/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config;

import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiConfig.REMOTE_API_ENABLED_CONDITION;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.LegacyRemoteApiController;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroRemoteApiController;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2 // Loads the spring beans required by the framework
public class SwaggerConfig
{
    /*
     * Just do avoid Springfox to auto-scan for all APIs 
     */
    @ConditionalOnExpression("!(" + REMOTE_API_ENABLED_CONDITION + ")")
    @Bean
    public Docket defaultDocket()
    {
        ApiSelectorBuilder builder = new Docket(DocumentationType.SWAGGER_2).select();
        builder.paths(path -> false);
        return builder.build()
                .groupName("Remote API disbled")
                .apiInfo(new ApiInfoBuilder()
                        .title("Remote API disabled")
                        .description(String.join(" ",
                                "The remote API is disabled."))
                        .license("")
                        .licenseUrl("")
                .build());
    }
    
    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public Docket legacyRemoteApiDocket()
    {
        ApiSelectorBuilder builder = new Docket(DocumentationType.SWAGGER_2).select();
        builder.paths(path -> path.matches(LegacyRemoteApiController.API_BASE + "/.*"));
        return builder.build()
                .groupName("Legacy API")
                .genericModelSubstitutes(Optional.class);
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public Docket areoRemoteApiDocket()
    {
        ApiSelectorBuilder builder = new Docket(DocumentationType.SWAGGER_2).select();
        builder.paths(path -> path.matches(AeroRemoteApiController.API_BASE + "/.*"));
        return builder.build()
                .groupName("AERO API")
                .apiInfo(new ApiInfoBuilder()
                        .title("AERO")
                        .version("1.0.0") 
                        .description(String.join(" ",
                                "Annotation Editor Remote Operations API. ",
                                "https://openminted.github.io/releases/aero-spec/1.0.0/omtd-aero/"))
                        .license("Apache License 2.0")
                        .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        .build())
                .genericModelSubstitutes(Optional.class);
    }
}
