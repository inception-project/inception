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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElgRecommenderTraits
    implements Serializable
{
    private static final long serialVersionUID = 9030733775045245314L;

    private long serviceId;
    private String serviceName;
    private String serviceDetailsUrl;
    private String serviceUrlSync;
    private String serviceUrlAsync;

    public String getServiceName()
    {
        return serviceName;
    }

    public void setServiceName(String aServiceName)
    {
        serviceName = aServiceName;
    }

    public long getServiceId()
    {
        return serviceId;
    }

    public void setServiceId(long aServiceId)
    {
        serviceId = aServiceId;
    }

    public String getServiceUrlSync()
    {
        return serviceUrlSync;
    }

    public void setServiceUrlSync(String aServiceUrlSync)
    {
        serviceUrlSync = aServiceUrlSync;
    }

    public String getServiceUrlAsync()
    {
        return serviceUrlAsync;
    }

    public void setServiceUrlAsync(String aServiceUrlAsync)
    {
        serviceUrlAsync = aServiceUrlAsync;
    }

    public String getServiceDetailsUrl()
    {
        return serviceDetailsUrl;
    }

    public void setServiceDetailsUrl(String aServiceDetailsUrl)
    {
        serviceDetailsUrl = aServiceDetailsUrl;
    }
}
