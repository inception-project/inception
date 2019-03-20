/*
 * Copyright 2018
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.LegacyRemoteApiController;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroRemoteApiController;

@Configuration
public class RemoteApiConfig
{
    public static final String REMOTE_API_ENABLED_CONDITION = 
            "${remote-api.enabled:false} || ${webanno.remote-api.enable:false}";
    
    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public AeroRemoteApiController aeroRemoteApiController()
    {
        return new AeroRemoteApiController();
    }

    @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
    @Bean
    public LegacyRemoteApiController legacyRemoteApiController()
    {
        return new LegacyRemoteApiController();
    }
}
