/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.clarin.webanno.webapp.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import de.tudarmstadt.ukp.clarin.webanno.security.OverridableUserDetailsManager;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.WebAnnoDaoAuthenticationProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.preauth.ShibbolethRequestHeaderAuthenticationFilter;

// There is no @EnableWebSecurity here because adding that would turn off Spring Boots security
// auto-configuration. Since we are using Spring Boot, it is sufficient to define the 
// WebSecurityConfigurerAdapter and GlobalAuthenticationConfigurerAdapter to customize specific
// aspects of the Spring Boot default security configuration.
//
// Why are the WebSecurityConfigurerAdapter classes inner classes?
//
// REC: Actually, I have first tried to have them as separate top-level classes - but in such a
// configuration, I was unable to get both adapters properly recognized. Whatever I tried, only the
// web UI protection was activated, but the API protection not. The Spring documentation 
// and various examples always implement this using inner classes - and voila - once I also used
// them, it worked. It is a pity though because this way it doesn't seem feasible to put the
// security configuration for the remote API into the remote API module while keeping the web UI
// protection with in a UI module.
@Configuration
public class WebAnnoSecurity
    extends GlobalAuthenticationConfigurerAdapter
{
    private @Value("${auth.preauth.header.principal:remote_user}") String preAuthPrincipalHeader;
    
    @Order(1)
    @Configuration
    public class RemoteApiSecurity
        extends WebSecurityConfigurerAdapter
    {
        private final PasswordEncoder passwordEncoder;
        private final UserDetailsManager userDetailsService;
        
        @Autowired
        public RemoteApiSecurity(PasswordEncoder aPasswordEncoder,
                UserDetailsManager aUserDetailsService)
        {
            passwordEncoder = aPasswordEncoder;
            userDetailsService = aUserDetailsService;
        }
        
        @Override
        protected void configure(HttpSecurity aHttp) throws Exception
        {
            aHttp
                .antMatcher("/api/**")
                .csrf().disable()
                // We hard-wire the internal user DB as the authentication provider here because
                // because the API shouldn't work with external pre-authentication
                .authenticationProvider(remoteApiAuthenticationProvider())
                //.userDetailsService(userDetailsService)
                .authorizeRequests()
                    .anyRequest().access("hasAnyRole('ROLE_REMOTE')")
                .and()
                .httpBasic()
                .and()
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }

        private AuthenticationProvider remoteApiAuthenticationProvider() {
            DaoAuthenticationProvider authProvider = new WebAnnoDaoAuthenticationProvider();
            authProvider.setUserDetailsService(userDetailsService);
            authProvider.setPasswordEncoder(passwordEncoder);
            return authProvider;
        }
    }
    
    @Configuration
    @Profile("auto-mode-builtin")
    public static class WebUiSecurity
        extends WebSecurityConfigurerAdapter
    {
        // Expose the AuthenticationManager using the legacy bean name that is expected by
        // WebAnno's SpringAuthenticatedWebSession. This can be removed when the explicit bean name
        // declaration has been removed from SpringAuthenticatedWebSession.
        @Bean(name = BeanIds.AUTHENTICATION_MANAGER)
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception
        {
            return super.authenticationManagerBean();
        }
        
        @Override
        protected void configure(HttpSecurity aHttp) throws Exception
        {
            aHttp
                .rememberMe()
                .and()
                .csrf().disable()
                .authorizeRequests()
                    .antMatchers("/login.html*").permitAll()
                    // Resources need to be publicly accessible so they don't trigger the login
                    // page. Otherwise it could happen that the user is redirected to a resource
                    // upon login instead of being forwarded to a proper application page.
                    .antMatchers("/favicon.ico").permitAll()
                    .antMatchers("/favicon.png").permitAll()
                    .antMatchers("/assets/**").permitAll()
                    .antMatchers("/images/**").permitAll()
                    .antMatchers("/resources/**").permitAll()
                    .antMatchers("/wicket/resource/**").permitAll()
                    .antMatchers("/swagger-ui.html").access("hasAnyRole('ROLE_REMOTE')")
                    .antMatchers("/admin/**").access("hasAnyRole('ROLE_ADMIN')")
                    .antMatchers("/doc/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
                    .antMatchers("/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
                    .anyRequest().denyAll()
                .and()
                .exceptionHandling()
                    .defaultAuthenticationEntryPointFor(
                            new LoginUrlAuthenticationEntryPoint("/login.html"), 
                            new AntPathRequestMatcher("/**"))
                .and()
                    .headers().frameOptions().sameOrigin();
        }
    }

    @Configuration
    @Profile("auto-mode-preauth")
    public class ShibbolethSecurity
        extends WebSecurityConfigurerAdapter
    {
        private ShibbolethRequestHeaderAuthenticationFilter filter;
        
        @Autowired
        public ShibbolethSecurity(ShibbolethRequestHeaderAuthenticationFilter aFilter)
        {
            filter = aFilter;
        }
        
        // Expose the AuthenticationManager using the legacy bean name that is expected by
        // WebAnno's SpringAuthenticatedWebSession. This can be removed when the explicit bean name
        // declaration has been removed from SpringAuthenticatedWebSession.
        @Bean(name = BeanIds.AUTHENTICATION_MANAGER)
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception
        {
            return super.authenticationManagerBean();
        }
        
        @Override
        protected void configure(HttpSecurity aHttp) throws Exception
        {
            aHttp
                .rememberMe()
                .and()
                .csrf().disable()
                .addFilterBefore(filter, RequestHeaderAuthenticationFilter.class)
                .authorizeRequests()
                    // Resources need to be publicly accessible so they don't trigger the login
                    // page. Otherwise it could happen that the user is redirected to a resource
                    // upon login instead of being forwarded to a proper application page.
                    .antMatchers("/favicon.ico").permitAll()
                    .antMatchers("/favicon.png").permitAll()
                    .antMatchers("/assets/**").permitAll()
                    .antMatchers("/images/**").permitAll()
                    .antMatchers("/resources/**").permitAll()
                    .antMatchers("/wicket/resource/**").permitAll()
                    .antMatchers("/swagger-ui.html").access("hasAnyRole('ROLE_REMOTE')")
                    .antMatchers("/admin/**").access("hasAnyRole('ROLE_ADMIN')")
                    .antMatchers("/doc/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
                    .antMatchers("/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
                    .anyRequest().denyAll()
                .and()
                .exceptionHandling()
                    .authenticationEntryPoint(new Http403ForbiddenEntryPoint())
                .and()
                    .headers().frameOptions().sameOrigin();
        }
    }
    
    @Bean
    @Profile("auto-mode-preauth")
    public ShibbolethRequestHeaderAuthenticationFilter preAuthFilter(
            UserDao aUserRepository,
            UserDetailsManager aUserDetailsService,
            @Lazy AuthenticationManager aAuthenticationManager)
    {
        ShibbolethRequestHeaderAuthenticationFilter filter = 
                new ShibbolethRequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader(preAuthPrincipalHeader);
        filter.setAuthenticationManager(aAuthenticationManager);
        filter.setUserDetailsManager(aUserDetailsService);
        filter.setUserRepository(aUserRepository);
        filter.setExceptionIfHeaderMissing(true);
        return filter;
    }
    
    @Bean(name = "authenticationProvider")
    @Profile("auto-mode-builtin")
    @Autowired
    public DaoAuthenticationProvider internalAuthenticationProvider(
            UserDetailsManager aUserDetailsService,
            PasswordEncoder aPasswordEncoder)
    {
        DaoAuthenticationProvider authProvider = new WebAnnoDaoAuthenticationProvider();
        authProvider.setUserDetailsService(aUserDetailsService);
        authProvider.setPasswordEncoder(aPasswordEncoder);
        return authProvider;
    }
    
    @Bean(name = "authenticationProvider")
    @Profile("auto-mode-preauth")
    public PreAuthenticatedAuthenticationProvider externalAuthenticationProvider(
            UserDetailsManager aUserDetailsService)
    {
        PreAuthenticatedAuthenticationProvider authProvider = 
                new PreAuthenticatedAuthenticationProvider();
        authProvider.setPreAuthenticatedUserDetailsService(
                new UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken>(
                        aUserDetailsService));
        return authProvider;
    }

    @Bean
    @Autowired
    public UserDetailsManager userDetailsService(DataSource aDataSource,
            @Lazy AuthenticationManager aAuthenticationManager)
    {
        OverridableUserDetailsManager manager = new OverridableUserDetailsManager();
        manager.setDataSource(aDataSource);
        manager.setAuthenticationManager(aAuthenticationManager);
        return manager;
    }
    
    // This bean allows the application to access session information. We currently only use this
    // to display the number of active users in the SystemStatusDashlet. However, the LoginPage
    // also accesses this bean in order to manually register the session when the user logs in.
    @Bean
    public SessionRegistry sessionRegistry()
    {
        return new SessionRegistryImpl();
    }
}
