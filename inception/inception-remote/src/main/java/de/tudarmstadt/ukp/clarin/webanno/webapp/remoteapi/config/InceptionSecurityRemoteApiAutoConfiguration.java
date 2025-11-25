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
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.inception.security.oauth.OAuth2Adapter;

@ConditionalOnExpression("!" + SPEL_IS_ADMIN_ACCOUNT_RECOVERY_MODE)
@ConditionalOnWebApplication
public class InceptionSecurityRemoteApiAutoConfiguration
{
    @Order(1)
    @Bean
    public SecurityFilterChain remoteApiFilterChain(PasswordEncoder aPasswordEncoder,
            UserDetailsManager aUserDetailsService, HttpSecurity aHttp,
            RemoteApiProperties aProperties, OAuth2Adapter aOAuth2Handling)
        throws Exception
    {
        // The remote API should always authenticate against the built-in user-database and
        // not e.g. against the external pre-authentication
        var authProvider = new InceptionDaoAuthenticationProvider();
        authProvider.setUserDetailsService(aUserDetailsService);
        authProvider.setPasswordEncoder(aPasswordEncoder);

        aHttp.securityMatcher("/api/**");
        aHttp.csrf().disable();
        aHttp.cors();

        // We hard-wire the internal user DB as the authentication provider here because
        // because the API shouldn't work with external pre-authentication
        aHttp.authenticationProvider(authProvider);

        aHttp.authorizeHttpRequests() //
                .anyRequest().hasAnyRole("REMOTE");

        if (aProperties.getHttpBasic().isEnabled()) {
            aHttp.httpBasic();
        }

        var oauth2Properties = aProperties.getOauth2();
        if (oauth2Properties != null && oauth2Properties.isEnabled()) {
            if (StringUtils.isBlank(oauth2Properties.getRealm())) {
                throw new IllegalArgumentException(
                        "No realm set for remote API OAuth authentication");
            }

            var authCon = new JwtAuthenticationConverter();
            String principalClaimName;
            if (isNotBlank(oauth2Properties.getUserNameAttribute())) {
                principalClaimName = oauth2Properties.getUserNameAttribute();
            }
            else {
                principalClaimName = JwtClaimNames.SUB;
            }
            authCon.setPrincipalClaimName(principalClaimName);

            authCon.setJwtGrantedAuthoritiesConverter(jwt -> aOAuth2Handling.loadAuthorities(jwt,
                    oauth2Properties.getRealm(), principalClaimName));
            aHttp.oauth2ResourceServer().jwt().jwtAuthenticationConverter(jwt -> {
                var token = authCon.convert(jwt);
                aOAuth2Handling.validateToken(token,
                        Realm.REALM_EXTERNAL_PREFIX + oauth2Properties.getRealm());
                return token;
            });
        }

        aHttp.sessionManagement() //
                .sessionCreationPolicy(STATELESS);

        return aHttp.build();
    }
}
