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
package de.tudarmstadt.ukp.inception.security.saml;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider.ResponseToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;

import de.tudarmstadt.ukp.clarin.webanno.security.OverridableUserDetailsManager;
import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.security.preauth.PreAuthUtils;
import jakarta.servlet.ServletContext;

public class Saml2AdapterImpl
    implements Saml2Adapter
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String AUTHENTICATION_REQUEST_URI = "/saml2/authenticate/";

    private final ServletContext context;
    private final UserDao userRepository;
    private final OverridableUserDetailsManager userDetailsManager;
    private final Optional<RelyingPartyRegistrationRepository> relyingPartyRegistrationRepository;

    public Saml2AdapterImpl(@Lazy ServletContext aContext, @Lazy UserDao aUserRepository,
            @Lazy OverridableUserDetailsManager aUserDetailsManager,
            @Lazy Optional<RelyingPartyRegistrationRepository> aRelyingPartyRegistrationRepository)
    {
        context = aContext;
        userRepository = aUserRepository;
        userDetailsManager = aUserDetailsManager;
        relyingPartyRegistrationRepository = aRelyingPartyRegistrationRepository;
    }

    /*
     * Code adapted from {@code
     * org.springframework.security.config.annotation.web.configurers.saml2.Saml2LoginConfigurer#
     * getIdentityProviderUrlMap(String, RelyingPartyRegistrationRepository)}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getSamlRelyingPartyRegistrations()
    {
        if (relyingPartyRegistrationRepository.isEmpty()) {
            return emptyMap();
        }

        var idps = new LinkedHashMap<String, String>();

        if (relyingPartyRegistrationRepository.get() instanceof Iterable repo) {
            var base = removeEnd(context.getContextPath(), "/") + AUTHENTICATION_REQUEST_URI;
            repo.forEach(it -> {
                if (it instanceof RelyingPartyRegistration p) {
                    idps.put(base + p.getRegistrationId(), p.getRegistrationId());
                }
            });
        }

        return idps;
    }

    @Override
    public Saml2Authentication process(ResponseToken aToken, Saml2Authentication aAuthentication)
    {
        return process(aAuthentication,
                aToken.getToken().getRelyingPartyRegistration().getRegistrationId());
    }

    public Saml2Authentication process(Saml2Authentication aAuthentication, String aRegistrationId)
    {
        User u = loadSamlUser(aAuthentication.getName(), aRegistrationId);

        Set<GrantedAuthority> allAuthorities = new HashSet<>();
        u.getRoles().stream() //
                .map(r -> new SimpleGrantedAuthority(r.toString())) //
                .forEach(allAuthorities::add);
        allAuthorities.addAll(userDetailsManager.loadUserAuthorities(u.getUsername()));

        return new Saml2Authentication((AuthenticatedPrincipal) aAuthentication.getPrincipal(),
                aAuthentication.getSaml2Response(), allAuthorities);
    }

    @Override
    public User loadSamlUser(String aUsername, String aRegistrationId)
    {
        var realm = Realm.REALM_EXTERNAL_PREFIX + aRegistrationId;

        denyAccessToUsersWithIllegalUsername(aUsername);

        User u = userRepository.get(aUsername);
        if (u != null) {
            denyAccessToDeactivatedUsers(u);
            denyAccessOfRealmsDoNotMatch(realm, u);
            return u;
        }

        var user = materializeUser(aUsername, realm);
        return user;
    }

    private User materializeUser(String username, String realm)
    {
        var u = new User();
        u.setUsername(username);
        u.setPassword(UserDao.EMPTY_PASSWORD);
        u.setEnabled(true);
        u.setRealm(realm);
        u.setRoles(PreAuthUtils.getPreAuthenticationNewUserRoles(u));
        userRepository.create(u);

        return u;
    }

    private void denyAccessToUsersWithIllegalUsername(String aUsername)
    {
        var userNameValidationResult = userRepository.validateUsername(aUsername);
        if (!userNameValidationResult.isEmpty()) {
            String messages = userNameValidationResult.stream() //
                    .map(ValidationError::getMessage) //
                    .collect(joining("\n- ", "\n- ", ""));
            LOG.warn("Prevented login of user [{}] with illegal username: {}", aUsername, messages);
            throw new BadCredentialsException("Illegal username");
        }
    }

    private void denyAccessOfRealmsDoNotMatch(String aExpectedRealm, User aUser)
    {
        if (!aExpectedRealm.equals(aUser.getRealm())) {
            LOG.warn("Prevented login of user {} from realm [{}] via realm [{}]", aUser,
                    aUser.getRealm(), aExpectedRealm);
            throw new BadCredentialsException("Realm mismatch");
        }
    }

    private void denyAccessToDeactivatedUsers(User aUser)
    {
        if (!aUser.isEnabled()) {
            LOG.warn("Prevented login of locally deactivated user {}", aUser);
            throw new DisabledException("User deactivated");
        }
    }
}
