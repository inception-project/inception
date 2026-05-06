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

/**
 * Top-level {@code auth.*} configuration properties. The primary purpose of this class is to bind
 * {@code auth.mode} to a typed enum so that Spring Boot's property binder rejects unsupported
 * values early — before any bean wiring takes place — with a descriptive error message.
 */
@ConfigurationProperties("auth")
public class AuthProperties
{
    /**
     * Authentication mode. Determines which authentication provider is activated.
     * <ul>
     * <li>{@code database} (default) – built-in database authentication</li>
     * <li>{@code preauth} – external pre-authentication (e.g. Shibboleth)</li>
     * </ul>
     */
    private AuthMode mode = AuthMode.DATABASE;

    public AuthMode getMode()
    {
        return mode;
    }

    public void setMode(AuthMode aMode)
    {
        mode = aMode;
    }

    public enum AuthMode
    {
        /** Built-in database authentication (default). */
        DATABASE,

        /** External pre-authentication, e.g. via Shibboleth / reverse-proxy headers. */
        PREAUTH;
    }
}
