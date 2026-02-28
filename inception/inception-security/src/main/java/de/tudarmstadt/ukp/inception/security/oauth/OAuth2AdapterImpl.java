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

import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.NO_PASSWORD;
import static java.util.stream.Collectors.joining;
import static org.springframework.security.oauth2.core.OAuth2ErrorCodes.ACCESS_DENIED;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ResolvableType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;

import de.tudarmstadt.ukp.clarin.webanno.security.OverridableUserDetailsManager;
import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.security.config.SecurityOAuthRolesProperties;

public class OAuth2AdapterImpl
    implements OAuth2Adapter
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private DefaultOAuth2UserService oauth2UserService = new DefaultOAuth2UserService();
    private OidcUserService oidcUserService = new OidcUserService();

    private final UserDao userRepository;
    private final OverridableUserDetailsManager userDetailsManager;
    private final Optional<ClientRegistrationRepository> clientRegistrationRepository;
    private final SecurityOAuthRolesProperties oAuth2Properties;

    public OAuth2AdapterImpl(@Lazy UserDao aUserRepository,
            @Lazy OverridableUserDetailsManager aUserDetailsManager,
            @Lazy Optional<ClientRegistrationRepository> aClientRegistrationRepository,
            SecurityOAuthRolesProperties aOAuth2Properties)
    {
        userRepository = aUserRepository;
        userDetailsManager = aUserDetailsManager;
        clientRegistrationRepository = aClientRegistrationRepository;
        oAuth2Properties = aOAuth2Properties;
    }

    @Override
    public OAuth2User loadUserOAuth2User(OAuth2UserRequest userRequest)
    {
        var externalUser = oauth2UserService.loadUser(userRequest);
        var user = fetchOrMaterializeUser(userRequest, externalUser);
        var authorities = loadAuthorities(externalUser, user);
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        return new DefaultOAuth2User(authorities, externalUser.getAttributes(),
                userNameAttributeName);
    }

    @Override
    public OidcUser loadOidcUser(OidcUserRequest userRequest)
    {
        var externalUser = oidcUserService.loadUser(userRequest);
        var user = fetchOrMaterializeUser(userRequest, externalUser);
        var authorities = loadAuthorities(externalUser, user);
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        return new DefaultOidcUser(authorities, externalUser.getIdToken(),
                externalUser.getUserInfo(), userNameAttributeName);
    }

    @Override
    public AbstractAuthenticationToken validateToken(AbstractAuthenticationToken aToken,
            String aRealm)
    {
        User u = userRepository.get(aToken.getName());
        if (u == null) {
            LOG.info("Prevented login of non-existing user [{}]", aToken.getName());
            OAuth2Error oauth2Error = new OAuth2Error(ACCESS_DENIED, "User does not exist", null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }

        denyAccessToDeactivatedUsers(u);
        denyAccessOfRealmsDoNotMatch(aRealm, u);
        return aToken;
    }

    @Override
    public Collection<GrantedAuthority> loadAuthorities(Jwt jwt, String aRealm,
            String aUserNameAttribute)
    {
        var authorities = new LinkedHashSet<GrantedAuthority>();
        var userName = jwt.getClaimAsString(aUserNameAttribute);
        authorities.addAll(userDetailsManager.loadUserAuthorities(userName));
        return authorities;
    }

    private LinkedHashSet<GrantedAuthority> loadAuthorities(OAuth2User externalUser, User user)
    {
        var authorities = new LinkedHashSet<GrantedAuthority>();
        authorities.addAll(userDetailsManager.loadUserAuthorities(user.getUsername()));
        authorities.addAll(externalUser.getAuthorities());
        return authorities;
    }

    private User fetchOrMaterializeUser(OAuth2UserRequest userRequest, OAuth2User user)
    {
        String username = user.getName();

        denyAccessToUsersWithIllegalUsername(username);

        var realm = Realm.REALM_EXTERNAL_PREFIX
                + userRequest.getClientRegistration().getRegistrationId();

        User u = userRepository.get(username);
        if (u != null) {
            denyAccessToDeactivatedUsers(u);
            denyAccessOfRealmsDoNotMatch(realm, u);

            // Only sync roles from OAuth2 group mappings when the feature is enabled.
            // Otherwise, manually assigned roles would be silently overwritten on every login.
            if (oAuth2Properties.isEnabled()) {
                u.setRoles(getOAuth2UserRoles(u, user));
                userRepository.update(u);
            }

            return u;
        }

        return materializeUser(user, username, realm);
    }

    private User materializeUser(OAuth2User user, String username, String realm)
    {
        var u = new User();
        u.setUsername(username);
        u.setPassword(NO_PASSWORD);
        u.setEnabled(true);
        u.setRealm(realm);

        u.setRoles(getOAuth2UserRoles(u, user));

        String email = user.getAttribute("email");
        if (email != null) {
            var emailNameValidationResult = userRepository.validateEmail(email);
            if (!emailNameValidationResult.isEmpty()) {
                throw new IllegalArgumentException(emailNameValidationResult.get(0).getMessage());
            }

            u.setEmail(email);
        }

        String uiName = user.getAttribute("name");
        if (uiName != null) {
            var uiNameNameValidationResult = userRepository.validateUiName(uiName);
            if (!uiNameNameValidationResult.isEmpty()) {
                throw new IllegalArgumentException(uiNameNameValidationResult.get(0).getMessage());
            }

            u.setUiName(uiName);
        }

        userRepository.create(u);

        return u;
    }

    Set<Role> getOAuth2UserRoles(User aUser, OAuth2User aOAuth2User) throws AccessDeniedException
    {
        Set<Role> roles = new HashSet<>();

        if (!oAuth2Properties.isEnabled()) {
            roles.add(Role.ROLE_USER);
            return roles;
        }

        var rawClaim = aOAuth2User.getAttribute(oAuth2Properties.getClaim());

        if (!(rawClaim instanceof Collection<?> oauth2groups) || oauth2groups.isEmpty()) {
            LOG.warn("OAuth2 roles mapping is enabled, but user [{}] doesn't have any roles, "
                    + "or the corresponding claim is empty", aUser.getUsername());
            throw new AccessDeniedException(
                    "OAuth2 roles mapping is enabled, but user [" + aUser.getUsername()
                            + "] doesn't have any roles, or the corresponding claim is empty");
        }

        oauth2groups.stream() //
                .filter(String.class::isInstance) //
                .map(String.class::cast) //
                .forEach(group -> matchOAuth2GroupToRole(group, roles));

        if (roles.isEmpty()) {
            LOG.warn("User [{}] doesn't belong to any role", aUser.getUsername());
            throw new AccessDeniedException(
                    "User [" + aUser.getUsername() + "] doesn't belong to any role");
        }

        return roles;
    }

    private void matchOAuth2GroupToRole(String aOAuth2Group, Set<Role> aUserRoles)
    {
        if (StringUtils.equals(aOAuth2Group, oAuth2Properties.getAdmin())) {
            aUserRoles.add(Role.ROLE_ADMIN);
        }

        if (StringUtils.equals(aOAuth2Group, oAuth2Properties.getUser())) {
            aUserRoles.add(Role.ROLE_USER);
        }

        if (StringUtils.equals(aOAuth2Group, oAuth2Properties.getProjectCreator())) {
            aUserRoles.add(Role.ROLE_PROJECT_CREATOR);
        }

        if (StringUtils.equals(aOAuth2Group, oAuth2Properties.getRemote())) {
            aUserRoles.add(Role.ROLE_REMOTE);
        }
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

    /*
     * Code adapted from {@link
     * org.springframework.security.config.annotation.web.configurers.oauth2.client.
     * OAuth2LoginConfigurer.getLoginLinks()}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<ClientRegistration> getOAuthClientRegistrations()
    {
        if (clientRegistrationRepository.isEmpty()) {
            return Collections.emptyList();
        }

        // We cannot use @SpringBean here because that returns a proxy that
        // ResolvableType.forInstance below won't be able to resolve.
        Iterable<ClientRegistration> clientRegistrations = null;
        ResolvableType type = ResolvableType.forInstance(clientRegistrationRepository.get())
                .as(Iterable.class);

        if (type != ResolvableType.NONE
                && ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
            clientRegistrations = (Iterable<ClientRegistration>) clientRegistrationRepository.get();
        }

        if (clientRegistrations == null) {
            return Collections.emptyList();
        }

        var registrations = new ArrayList<ClientRegistration>();
        clientRegistrations.forEach(registrations::add);
        return registrations;
    }
}
