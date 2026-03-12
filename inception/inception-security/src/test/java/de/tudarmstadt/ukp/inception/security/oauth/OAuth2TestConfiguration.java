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
package de.tudarmstadt.ukp.inception.security.oauth;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;

import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

/**
 * Shared {@link SpringBootConfiguration} discovered automatically by all {@code @DataJpaTest}
 * classes in this package. Keeping it in a single top-level class prevents the
 * "multiple @SpringBootConfiguration" error that arises when they each define their own nested
 * config.
 */
@SpringBootConfiguration
@AutoConfigurationPackage
public class OAuth2TestConfiguration
{
    @Bean
    ApplicationContextProvider applicationContextProvider()
    {
        return new ApplicationContextProvider();
    }

    @Bean
    AuthenticationEventPublisher authenticationEventPublisher()
    {
        return new DefaultAuthenticationEventPublisher();
    }
}
