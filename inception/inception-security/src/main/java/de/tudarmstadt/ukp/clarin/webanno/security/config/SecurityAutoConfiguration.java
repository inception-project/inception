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
import java.util.Optional;

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
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.transaction.PlatformTransactionManager;

import de.tudarmstadt.ukp.clarin.webanno.security.ExtensiblePermissionEvaluator;
import de.tudarmstadt.ukp.clarin.webanno.security.OverridableUserDetailsManager;
import de.tudarmstadt.ukp.clarin.webanno.security.PermissionExtension;
import de.tudarmstadt.ukp.clarin.webanno.security.PermissionExtensionPoint;
import de.tudarmstadt.ukp.clarin.webanno.security.PermissionExtensionPointImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.SuccessfulLoginListener;
import de.tudarmstadt.ukp.clarin.webanno.security.UserAccess;
import de.tudarmstadt.ukp.clarin.webanno.security.UserAccessImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.inception.security.oauth.OAuth2Adapter;
import de.tudarmstadt.ukp.inception.security.oauth.OAuth2AdapterImpl;
import de.tudarmstadt.ukp.inception.security.saml.Saml2Adapter;
import de.tudarmstadt.ukp.inception.security.saml.Saml2AdapterImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.ServletContext;

@Configuration
@EnableConfigurationProperties({ //
        UserProfilePropertiesImpl.class, //
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
            UserProfileProperties aUserProfileProperties,
            @Autowired(required = false) SessionRegistry aSessionRegistry)
    {
        return new UserDaoImpl(entityManager, aSecurityProperties, aUserProfileProperties,
                transactionManager, aSessionRegistry);
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
        var encoderForEncoding = "bcrypt";
        var encoders = new HashMap<String, PasswordEncoder>();
        encoders.put(encoderForEncoding, new BCryptPasswordEncoder());
        var delegatingEncoder = new DelegatingPasswordEncoder(encoderForEncoding, encoders);
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
        var expressionHandler = new DefaultMethodSecurityExpressionHandler();
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

    @Bean
    public Saml2Adapter saml2Adapter(@Lazy ServletContext aContext, @Lazy UserDao aUserRepository,
            @Lazy OverridableUserDetailsManager aUserDetailsManager,
            @Lazy Optional<RelyingPartyRegistrationRepository> aRelyingPartyRegistrationRepository)
    {
        return new Saml2AdapterImpl(aContext, aUserRepository, aUserDetailsManager,
                aRelyingPartyRegistrationRepository);
    }

    @Bean
    public UserAccess userAccess(UserDao aUserService)
    {
        return new UserAccessImpl(aUserService);
    }

    @Bean
    public SuccessfulLoginListener successfulLoginListener(UserDao aUserRepository)
    {
        return new SuccessfulLoginListener(aUserRepository);
    }
}
