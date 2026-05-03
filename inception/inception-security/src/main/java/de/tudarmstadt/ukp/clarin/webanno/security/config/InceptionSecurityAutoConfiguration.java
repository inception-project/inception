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
package de.tudarmstadt.ukp.clarin.webanno.security.config;

import javax.sql.DataSource;

import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.OverridableUserDetailsManager;

@EnableWebSecurity
@EnableMethodSecurity
public class InceptionSecurityAutoConfiguration
{
    @Bean
    public UserDetailsManager userDetailsService(DataSource aDataSource,
            @Lazy AuthenticationConfiguration aAuthenticationConfiguration)
        throws Exception
    {
        return new OverridableUserDetailsManager(aDataSource, aAuthenticationConfiguration);
    }

    @Bean
    public GlobalAuthenticationConfigurerAdapter globalAuthenticationConfigurerAdapter(
            AuthenticationProvider authenticationProvider,
            AuthenticationEventPublisher aAuthenticationEventPublisher)
    {
        return new GlobalAuthenticationConfigurerAdapter()
        {
            @Override
            public void configure(AuthenticationManagerBuilder aAuth)
            {
                aAuth.authenticationProvider(authenticationProvider);
                aAuth.authenticationEventPublisher(aAuthenticationEventPublisher);
            }
        };
    }

    @Bean(name = "authenticationProvider")
    @ConditionalOnProperty(name = "auth.mode", havingValue = "preauth")
    @ConditionalOnMissingBean(name = "authenticationProvider")
    public PreAuthenticatedAuthenticationProvider externalAuthenticationProvider(
            @Lazy UserDetailsManager aUserDetails)
    {
        var authProvider = new PreAuthenticatedAuthenticationProvider();
        authProvider.setPreAuthenticatedUserDetailsService(
                new UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken>(
                        aUserDetails));
        return authProvider;
    }

    @Bean(name = "authenticationProvider")
    @ConditionalOnProperty(name = "auth.mode", havingValue = "database", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "authenticationProvider")
    public DaoAuthenticationProvider internalAuthenticationProvider(
            @Lazy UserDetailsService aUserDetails, PasswordEncoder aPasswordEncoder)
    {
        var authProvider = new InceptionDaoAuthenticationProvider(aUserDetails);
        authProvider.setPasswordEncoder(aPasswordEncoder);
        return authProvider;
    }

    /**
     * Default redaction rules applied wherever a
     * {@link org.springframework.boot.actuate.endpoint.Sanitizer} collects
     * {@link SanitizingFunction} beans &mdash; both Spring Boot's actuator endpoints (e.g.
     * {@code /env}, {@code /configprops}) and our admin Settings page. Mirrors the credential-style
     * key list the actuator used to apply by default before Boot 3 made all sanitization opt-in.
     * Other modules can contribute their own {@link SanitizingFunction} beans to extend redaction.
     */
    @Bean
    public SanitizingFunction credentialSanitizingFunction()
    {
        // ifLikelyCredential already covers keys ending in password/secret/key/token and
        // keys containing "credentials". The extra ifKeyContains entries make api-key
        // matching explicit (in any common spelling) and add bearer/authorization which
        // the default chain does not cover.
        return SanitizingFunction.sanitizeValue() //
                .ifLikelyCredential() //
                .ifKeyContains("apikey", "api-key", "api_key", "bearer", "authorization");
    }

    /**
     * Mask {@code repository.path}: the on-disk INCEpTION data directory leaks deployment-specific
     * filesystem layout, so we redact it from any externally visible surface (actuator endpoints,
     * admin Settings page).
     */
    @Bean
    public SanitizingFunction repositoryPathSanitizingFunction()
    {
        return SanitizingFunction.sanitizeValue() //
                .ifKeyEquals("repository.path");
    }
}
