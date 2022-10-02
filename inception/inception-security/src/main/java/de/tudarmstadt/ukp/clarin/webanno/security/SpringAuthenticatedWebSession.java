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
package de.tudarmstadt.ukp.clarin.webanno.security;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import de.tudarmstadt.ukp.clarin.webanno.security.config.AuthenticationConfigurationHolder;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;

/**
 * An {@link AuthenticatedWebSession} based on {@link Authentication}
 */
public class SpringAuthenticatedWebSession
    extends AuthenticatedWebSession
{
    private static final long serialVersionUID = 1L;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private @SpringBean AuthenticationConfigurationHolder authenticationConfigurationHolder;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;
    private @SpringBean(required = false) SessionRegistry sessionRegistry;

    public SpringAuthenticatedWebSession(Request request)
    {
        super(request);
        injectDependencies();

        syncSpringSecurityAuthenticationToWicket();
    }

    /**
     * @return Current authenticated web session
     */
    public static SpringAuthenticatedWebSession get()
    {
        return (SpringAuthenticatedWebSession) Session.get();
    }

    private boolean isAcceptableAuthenticationToken(Authentication aAuthentication)
    {
        return aAuthentication instanceof PreAuthenticatedAuthenticationToken
                || aAuthentication instanceof OAuth2AuthenticationToken
                || aAuthentication instanceof UsernamePasswordAuthenticationToken;
    }

    private void injectDependencies()
    {
        Injector.get().inject(this);
    }

    @Override
    public boolean authenticate(String username, String password)
    {
        try {
            Request request = RequestCycle.get().getRequest();
            if (request instanceof ServletWebRequest) {
                HttpServletRequest containerRequest = ((ServletWebRequest) request)
                        .getContainerRequest();

                // Kill current session and create a new one as part of the authentication
                containerRequest.getSession().invalidate();
            }

            Authentication authentication = authenticationConfigurationHolder
                    .getAuthenticationConfiguration().getAuthenticationManager()
                    .authenticate(new UsernamePasswordAuthenticationToken(username, password));

            springSecuritySignIn(authentication);

            return true;
        }
        catch (AuthenticationException e) {
            log.warn("User [{}] failed to login. Reason: {}", username, e.getMessage());
            return false;
        }
        catch (Exception e) {
            log.error("Unexpected error during authentication: ", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Propagate a successful authentication that was already performed by Spring Security to the
     * Wicket Security system.
     */
    public void syncSpringSecurityAuthenticationToWicket()
    {
        // If the a proper (non-anonymous) authentication has already been performed (e.g. via
        // external pre-authentication) then also mark the Wicket session as signed-in.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAcceptableAuthenticationToken(authentication) && authentication.isAuthenticated()) {
            if (!isSignedIn()) {
                signIn(authentication);
            }
        }
        else if (isSignedIn()) {
            signOut();
        }
    }

    private void springSecuritySignIn(Authentication aAuthentication)
    {
        MDC.put(Logging.KEY_USERNAME, aAuthentication.getName());

        SecurityContextHolder.getContext().setAuthentication(aAuthentication);
        log.debug("Stored authentication for user [{}] in security context",
                aAuthentication.getName());

        setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
        log.debug("Stored security context in session");

        bind();

        if (sessionRegistry != null) {
            // Form-based login isn't detected by SessionManagementFilter. Thus handling
            // session registration manually here.
            sessionRegistry.registerNewSession(getId(), aAuthentication.getName());
        }
    }

    public void signIn(Authentication aAuthentication)
    {
        Request request = RequestCycle.get().getRequest();
        if (request instanceof ServletWebRequest) {
            var containerRequest = ((ServletWebRequest) request).getContainerRequest();

            // Kill current session and create a new one as part of the authentication
            containerRequest.getSession().invalidate();
        }

        springSecuritySignIn(aAuthentication);
        signIn(true);

        // If this is called, the authentication object has been created artificially and not via
        // the authenticationManager, so we need to send the login even manually
        applicationEventPublisherHolder.get()
                .publishEvent(new AuthenticationSuccessEvent(aAuthentication));
    }

    @Override
    public void signOut()
    {
        log.debug("Logging out");
        super.signOut();
        SecurityContextHolder.clearContext();
    }

    @Override
    public Roles getRoles()
    {
        if (!isSignedIn()) {
            return new Roles();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Roles roles = new Roles();
        if (authentication != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                roles.add(authority.getAuthority());
            }
        }

        return roles;
    }
}
