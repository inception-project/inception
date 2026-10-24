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
package de.tudarmstadt.ukp.inception.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("instance")
public class InstanceMetadataPropertiesImpl
    implements InstanceMetadataProperties
{
    /**
     * Public contact information of whoever operates this instance, e.g. a URL or an e-mail
     * address. It is sent along with requests to remote services (e.g. in the {@code User-Agent}
     * header of requests to remote knowledge bases such as Wikidata) so that the service operators
     * can get in touch if the instance causes problems.
     */
    private String operatorContact;

    @Override
    public String getOperatorContact()
    {
        return operatorContact;
    }

    public void setOperatorContact(String aOperatorContact)
    {
        operatorContact = aOperatorContact;
    }
}
