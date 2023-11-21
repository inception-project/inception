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
package de.tudarmstadt.ukp.clarin.webanno.telemetry.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.transaction.PlatformTransactionManager;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryService;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetrySupport;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.identity.InstanceIdentityService;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.matomo.MatomoTelemetrySupport;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.matomo.MatomoTelemetrySupportImpl;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.ui.TelemetryFooterItem;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.ui.TelemetrySettingsInterceptor;

@ConditionalOnWebApplication
@Configuration
@EnableConfigurationProperties({ TelemetryServicePropertiesImpl.class,
        MatomoTelemetryServicePropertiesImpl.class })
@ConditionalOnProperty(prefix = "telemetry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TelemetryServiceAutoConfiguration
{
    @Bean
    public TelemetryService telemetryService(
            @Lazy @Autowired(required = false) List<TelemetrySupport<?>> aTelemetrySupports,
            ApplicationEventPublisher aEventPublisher, TelemetryServiceProperties aProperties,
            PlatformTransactionManager aTransactionManager)
    {
        return new TelemetryServiceImpl(aTelemetrySupports, aEventPublisher, aProperties,
                aTransactionManager);
    }

    @Bean
    public TelemetrySettingsInterceptor telemetrySettingsInterceptor(UserDao aUserService,
            TelemetryService aTelemetryService)
    {
        return new TelemetrySettingsInterceptor(aUserService, aTelemetryService);
    }

    @Bean
    public MatomoTelemetrySupport matomoTelemetrySupport(TelemetryService aTelemetryService,
            InstanceIdentityService aIdentityService, UserDao aUserDao,
            SessionRegistry aSessionRegistry, MatomoTelemetryServiceProperties aMatomoProperties,
            @Value("${spring.application.name}") String aApplicationName)
    {
        return new MatomoTelemetrySupportImpl(aTelemetryService, aIdentityService, aUserDao,
                aSessionRegistry, aMatomoProperties, aApplicationName);
    }

    @Bean
    public TelemetryFooterItem telemetryFooterItem()
    {
        return new TelemetryFooterItem();
    }
}
