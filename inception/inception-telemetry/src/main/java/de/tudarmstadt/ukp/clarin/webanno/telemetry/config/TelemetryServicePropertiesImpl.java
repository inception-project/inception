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
@ConfigurationProperties("telemetry")
public class TelemetryServicePropertiesImpl
    implements TelemetryServiceProperties
{
    private AutoResponse autoRespond;

    @Override
    public AutoResponse getAutoRespond()
    {
        return autoRespond;
    }

    public void setAutoRespond(AutoResponse aAutoRespond)
    {
        autoRespond = aAutoRespond;
    }

    public static enum AutoResponse
    {
        ACCEPT, REJECT
    }
}
