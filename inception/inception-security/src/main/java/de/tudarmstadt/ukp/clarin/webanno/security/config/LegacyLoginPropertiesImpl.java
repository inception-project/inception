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
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * @deprecated Deprecated since INCEpTION v25.0. Has moved to {@link LoginPropertiesImpl}
 */
@Deprecated
@ConfigurationProperties("login")
public class LegacyLoginPropertiesImpl
{
    private long maxConcurrentSessions;
    private String message;

    @DeprecatedConfigurationProperty( //
            reason = "property has moved", //
            replacement = "security.login.max-concurrent-sessions")
    @Deprecated
    public long getMaxConcurrentSessions()
    {
        return maxConcurrentSessions;
    }

    @Deprecated
    public void setMaxConcurrentSessions(long aMaxConcurrentSessions)
    {
        maxConcurrentSessions = aMaxConcurrentSessions;
    }

    @DeprecatedConfigurationProperty( //
            reason = "property has moved", //
            replacement = "security.login.message")
    @Deprecated
    public String getMessage()
    {
        return message;
    }

    @Deprecated
    public void setMessage(String aMessage)
    {
        message = aMessage;
    }
}
