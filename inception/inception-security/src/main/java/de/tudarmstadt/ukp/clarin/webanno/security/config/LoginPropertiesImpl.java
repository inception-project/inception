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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("security.login")
public class LoginPropertiesImpl
    implements LoginProperties
{
    /**
     * Auto-login using the given identity provider. Set this to the registration ID of a SAML or
     * OAuth2 provider configured via {@code spring.security.saml2.relyingparty.registration.*} or
     * {@code spring.security.oauth2.client.*} to bypass the login page and sign users in
     * automatically. Useful for single-sign-on scenarios where only a single identity provider is
     * used. To bypass auto-login (e.g. to sign in via credentials), navigate to
     * {@code .../login.html?skipAutoLogin=true} in a fresh browser session.
     */
    private String autoLogin;

    private long maxConcurrentSessions;

    /**
     * Custom message to appear on the login page, such as a project web-site or annotation
     * guideline link. The message supports markdown syntax.
     */
    private String message;

    public void setAutoLogin(String aAutoLogin)
    {
        autoLogin = aAutoLogin;
    }

    @Override
    public String getAutoLogin()
    {
        return autoLogin;
    }

    @Override
    public long getMaxConcurrentSessions()
    {
        return maxConcurrentSessions;
    }

    public void setMaxConcurrentSessions(long aMaxConcurrentSessions)
    {
        maxConcurrentSessions = aMaxConcurrentSessions;
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    public void setMessage(String aMessage)
    {
        message = aMessage;
    }
}
