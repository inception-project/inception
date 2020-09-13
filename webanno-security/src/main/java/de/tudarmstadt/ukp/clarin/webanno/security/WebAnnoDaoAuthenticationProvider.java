/*
 * Copyright 2020
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

import static de.tudarmstadt.ukp.clarin.webanno.security.preauth.ShibbolethRequestHeaderAuthenticationFilter.EMPTY_PASSWORD;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

public class WebAnnoDaoAuthenticationProvider extends DaoAuthenticationProvider
{
    @Override
    protected void additionalAuthenticationChecks(UserDetails aUserDetails,
            UsernamePasswordAuthenticationToken aAuthentication)
        throws AuthenticationException
    {
        String presentedPassword = aAuthentication.getCredentials().toString();

        // Users which are created through the pre-auth mechanism end up with an empty password. 
        // So we do not want to accept these blank passwords when we have a non-preauth login.
        if (EMPTY_PASSWORD.equals(presentedPassword)) {

            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"));
        }
        
        super.additionalAuthenticationChecks(aUserDetails, aAuthentication);
    }
}
