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
package de.tudarmstadt.ukp.clarin.webanno.webapp.security;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.request.Request;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 *  An {@link AuthenticatedWebSession} based on {@link Authentication}
 *
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
        signOut();
        
        boolean authenticated = false;
        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(username, password));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            authenticated = authentication.isAuthenticated();
        }
        catch (AuthenticationException e) {
            log.warn(format("User '%s' failed to login. Reason: %s", username, e.getMessage()));
            authenticated = false;
        }
        return authenticated;
    }

    @Override
    public void signOut()
    {
        super.signOut();
        SecurityContextHolder.clearContext();
    }

    @Override
    public Roles getRoles()
    {
        SecurityContext ctx = SecurityContextHolder.getContext();
        
        Roles roles = new Roles();
        if (ctx.getAuthentication().isAuthenticated()) {
            boolean isAnonymous = false;
            
            Authentication authentication = ctx.getAuthentication();
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                roles.add(authority.getAuthority());
                if ("ROLE_ANONYMOUS".equals(authority.getAuthority())) {
                    isAnonymous = true;
                }
            }
            
            // In case we are already signed in through Spring Security but never signed in to Wicket
            // make sure we also sign in to Wicket - unless we are authenticated as anonymous!
            if (!isSignedIn() && !isAnonymous) {
                signIn(true);
            }
            else if (isSignedIn() && isAnonymous) {
                signOut();
                roles = new Roles();
            }
        }
        return roles;
    }
}
