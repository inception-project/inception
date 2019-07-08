/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.security;

import javax.servlet.http.HttpSession;

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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

/**
 *  An {@link AuthenticatedWebSession} based on {@link Authentication}
 */
public class SpringAuthenticatedWebSession
    extends AuthenticatedWebSession
{
    private static final long serialVersionUID = 1L;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @SpringBean(name = "org.springframework.security.authenticationManager")
    private AuthenticationManager authenticationManager;

    public SpringAuthenticatedWebSession(Request request)
    {
        super(request);
        injectDependencies();
        ensureDependenciesNotNull();
        
        // If the a proper (non-anonymous) authentication has already been performed (e.g. via
        // external pre-authentication) then also mark the Wicket session as signed-in.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (
                authentication != null && 
                authentication.isAuthenticated() && 
                authentication instanceof PreAuthenticatedAuthenticationToken
                //!(authentication instanceof AnonymousAuthenticationToken && !isSignedIn())
        ) {
            signIn(true);
        }
    }

    private void ensureDependenciesNotNull()
    {
        if (authenticationManager == null) {
            throw new IllegalStateException("AdminSession requires an authenticationManager.");
        }
    }

    private void injectDependencies()
    {
        Injector.get().inject(this);
    }

    @Override
    public boolean authenticate(String username, String password)
    {
        // If already signed in (in Spring Security), then sign out there first
        // signOut();
        
        try {
            // Kill current session and create a new one as part of the authentication
            ((ServletWebRequest) RequestCycle.get().getRequest()).getContainerRequest().getSession()
                    .invalidate();
            
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(username, password));

            MDC.put(Logging.KEY_USERNAME, username);
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Stored authentication for user [{}] in security context",
                    authentication.getName());
            
            HttpSession session = ((ServletWebRequest) RequestCycle.get().getRequest())
                    .getContainerRequest().getSession();
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
            log.debug("Stored security context in session");
            
            return true;
        }
        catch (AuthenticationException e) {
            log.warn("User [{}] failed to login. Reason: {}", username, e.getMessage());
            return false;
        }
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
        Roles roles = new Roles();
        if (isSignedIn()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                roles.add(authority.getAuthority());
            }
        }
        return roles;
    }
}
