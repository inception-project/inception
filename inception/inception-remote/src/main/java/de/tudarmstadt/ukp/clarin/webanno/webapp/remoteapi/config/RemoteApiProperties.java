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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("remote-api")
public class RemoteApiProperties
{
    private boolean enabled = false;
    private HttpBasicProperties httpBasic = new HttpBasicProperties();
    private OAuth2Properties oauth2 = null;

    public boolean isEnabled()
    {
        boolean enabledViaLegacySystemProperty = "true"
                .equals(System.getProperty("webanno.remote-api.enable"));

        return enabled || enabledViaLegacySystemProperty;
    }

    public void setEnabled(boolean aRemoteApiEnabled)
    {
        enabled = aRemoteApiEnabled;
    }

    public HttpBasicProperties getHttpBasic()
    {
        return httpBasic;
    }

    public void setHttpBasic(HttpBasicProperties aHttpBasic)
    {
        httpBasic = aHttpBasic;
    }

    public OAuth2Properties getOauth2()
    {
        return oauth2;
    }

    public void setOauth2(OAuth2Properties aOauth2)
    {
        oauth2 = aOauth2;
    }

    public static class HttpBasicProperties
    {
        private boolean enabled = true;

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean aEnabled)
        {
            enabled = aEnabled;
        }
    }

    public static class OAuth2Properties
    {
        private boolean enabled = false;
        private String userNameAttribute;
        private String realm;

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean aEnabled)
        {
            enabled = aEnabled;
        }

        public String getUserNameAttribute()
        {
            return userNameAttribute;
        }

        public void setUserNameAttribute(String aUserNameAttribute)
        {
            userNameAttribute = aUserNameAttribute;
        }

        public void setRealm(String aRealm)
        {
            realm = aRealm;
        }

        public String getRealm()
        {
            return realm;
        }
    }
}
