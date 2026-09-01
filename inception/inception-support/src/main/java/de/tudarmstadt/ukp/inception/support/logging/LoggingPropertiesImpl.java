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
package de.tudarmstadt.ukp.inception.support.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("logging.request")
public class LoggingPropertiesImpl
    implements LoggingProperties
{
    /**
     * Make the network address of the client from which a request originated available to the
     * logging framework as the {@code remoteAddress} context variable. Note that in many
     * jurisdictions - e.g. under the GDPR in the European Union - the network address of a client
     * counts as personal data.
     */
    private boolean remoteAddress;

    @Override
    public boolean isRemoteAddress()
    {
        return remoteAddress;
    }

    public void setRemoteAddress(boolean aRemoteAddress)
    {
        remoteAddress = aRemoteAddress;
    }
}
