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
package de.tudarmstadt.ukp.clarin.webanno.telemetry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This class is exposed as a Spring Component via {@link TelemetryServiceAutoConfiguration}.
 */
@ConfigurationProperties("telemetry.matomo")
public class MatomoTelemetryServicePropertiesImpl
    implements MatomoTelemetryServiceProperties
{
    private String serverScheme = DEFAULT_SERVER_SCHEME;
    private String serverHost = DEFAULT_SERVER_HOST;
    private String serverPath = DEFAULT_SERVER_PATH;
    private int siteId = DEFAULT_SITE_ID;
    private String context = DEFAULT_CONTEXT;

    @Override
    public String getServerScheme()
    {
        return serverScheme;
    }

    public void setServerScheme(String aServerScheme)
    {
        serverScheme = aServerScheme;
    }

    @Override
    public String getServerHost()
    {
        return serverHost;
    }

    public void setServerHost(String aServerHost)
    {
        serverHost = aServerHost;
    }

    @Override
    public String getServerPath()
    {
        return serverPath;
    }

    public void setServerPath(String aServerPath)
    {
        serverPath = aServerPath;
    }

    @Override
    public int getSiteId()
    {
        return siteId;
    }

    public void setSiteId(int aSiteId)
    {
        siteId = aSiteId;
    }

    @Override
    public String getContext()
    {
        return context;
    }

    public void setContext(String aContext)
    {
        context = aContext;
    }
}
