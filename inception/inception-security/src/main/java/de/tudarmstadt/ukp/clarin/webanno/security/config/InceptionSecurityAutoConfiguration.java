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

import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_AUTH_MODE_EXTERNAL_PREAUTH;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.OverridableUserDetailsManager;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService;

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
            public void configure(AuthenticationManagerBuilder aAuth) throws Exception
            {
                aAuth.authenticationProvider(authenticationProvider);
                aAuth.authenticationEventPublisher(aAuthenticationEventPublisher);
            }
        };
    }

    @Bean(name = "authenticationProvider")
    @Profile(PROFILE_AUTH_MODE_EXTERNAL_PREAUTH)
    public PreAuthenticatedAuthenticationProvider externalAuthenticationProvider(
            @Lazy UserDetailsManager aUserDetails)
    {
        PreAuthenticatedAuthenticationProvider authProvider = new PreAuthenticatedAuthenticationProvider();
        authProvider.setPreAuthenticatedUserDetailsService(
                new UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken>(
                        aUserDetails));
        return authProvider;
    }

    @Bean(name = "authenticationProvider")
    @Profile(DeploymentModeService.PROFILE_AUTH_MODE_DATABASE)
    public DaoAuthenticationProvider internalAuthenticationProvider(
            @Lazy UserDetailsManager aUserDetails, PasswordEncoder aPasswordEncoder)
    {
        DaoAuthenticationProvider authProvider = new InceptionDaoAuthenticationProvider();
        authProvider.setUserDetailsService(aUserDetails);
        authProvider.setPasswordEncoder(aPasswordEncoder);
        return authProvider;
    }
}
