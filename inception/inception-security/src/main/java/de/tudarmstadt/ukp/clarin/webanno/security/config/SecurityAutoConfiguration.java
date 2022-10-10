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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.transaction.PlatformTransactionManager;

import de.tudarmstadt.ukp.clarin.webanno.security.ExtensiblePermissionEvaluator;
import de.tudarmstadt.ukp.clarin.webanno.security.OverridableUserDetailsManager;
import de.tudarmstadt.ukp.clarin.webanno.security.PermissionExtension;
import de.tudarmstadt.ukp.clarin.webanno.security.PermissionExtensionPoint;
import de.tudarmstadt.ukp.clarin.webanno.security.PermissionExtensionPointImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.inception.security.oauth.OAuth2Adapter;
import de.tudarmstadt.ukp.inception.security.oauth.OAuth2AdapterImpl;

@Configuration
@EnableConfigurationProperties({ //
        LegacyLoginPropertiesImpl.class, //
        LoginPropertiesImpl.class, //
        SecurityPropertiesImpl.class, //
        PreauthenticationPropertiesImpl.class })
public class SecurityAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;
    private @Autowired(required = false) PlatformTransactionManager transactionManager;

    @Bean("userRepository")
    public UserDao userService(SecurityProperties aSecurityProperties,
            @Autowired(required = false) SessionRegistry aSessionRegistry,
            OAuth2Adapter aOAuth2Adapter)
    {
        return new UserDaoImpl(entityManager, aSecurityProperties, transactionManager,
                aSessionRegistry);
    }

    @Bean
    public SessionRegistry sessionRegistry()
    {
        return new SessionRegistryImpl();
    }

    // The WebAnno User model class picks this bean up by name!
    @Bean
    public PasswordEncoder passwordEncoder()
    {
        // Set up a DelegatingPasswordEncoder which decodes legacy passwords using the
        // StandardPasswordEncoder but encodes passwords using the modern BCryptPasswordEncoder
        String encoderForEncoding = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(encoderForEncoding, new BCryptPasswordEncoder());
        DelegatingPasswordEncoder delegatingEncoder = new DelegatingPasswordEncoder(
                encoderForEncoding, encoders);
        // Decode legacy passwords without encoder ID using the StandardPasswordEncoder
        delegatingEncoder.setDefaultPasswordEncoderForMatches(new StandardPasswordEncoder());
        return delegatingEncoder;
    }

    @Bean
    public PermissionExtensionPoint permissionExtensionPoint(
            @Lazy @Autowired(required = false) List<PermissionExtension<?, ?>> aExtensions)
    {
        return new PermissionExtensionPointImpl(aExtensions);
    }

    @Bean
    public ExtensiblePermissionEvaluator extensiblePermissionEvaluator(
            PermissionExtensionPoint aPermissionExtensionPoint)
    {
        return new ExtensiblePermissionEvaluator(aPermissionExtensionPoint);
    }

    @Bean
    public MethodSecurityExpressionHandler defaultMethodSecurityExpressionHandler(
            ApplicationContext aContext, ExtensiblePermissionEvaluator aEvaluator)
    {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setApplicationContext(aContext);
        expressionHandler.setPermissionEvaluator(aEvaluator);
        return expressionHandler;
    }

    @Bean
    public OAuth2Adapter oAuth2Adapter(@Lazy UserDao aUserRepository,
            @Lazy OverridableUserDetailsManager aUserDetailsManager,
            @Lazy Optional<ClientRegistrationRepository> aClientRegistrationRepository)
    {
        return new OAuth2AdapterImpl(aUserRepository, aUserDetailsManager,
                aClientRegistrationRepository);
    }
}
