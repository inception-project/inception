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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElgCatalogEntity
    implements Serializable
{
    private static final long serialVersionUID = 6132758139094707551L;

    private @JsonProperty("id") long id;
    private @JsonProperty("resource_name") String resourceName;
    private @JsonProperty("resource_short_name") List<String> resourceShortNames;
    private @JsonProperty("resource_type") String resourceType;
    private @JsonProperty("version") String version;
    private @JsonProperty("description") String description;
    private @JsonProperty("detail") String detailUrl;
    private @JsonProperty("condition_of_use") List<String> conditionsOfUse;
    private @JsonProperty("languages") List<String> languages;
    private @JsonProperty("functions") List<String> functions;
    private @JsonProperty("intended_applications") List<String> intendedApplications;
    private @JsonProperty("service_execution_count") long serviceExecutionCount;
    private @JsonProperty("elg_compatible_service") boolean elgCompatibleService;

    public long getId()
    {
        return id;
    }

    public void setId(long aId)
    {
        id = aId;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public void setResourceName(String aResourceName)
    {
        resourceName = aResourceName;
    }

    public List<String> getResourceShortNames()
    {
        return resourceShortNames;
    }

    public void setResourceShortNames(List<String> aResourceShortNames)
    {
        resourceShortNames = aResourceShortNames;
    }

    public String getResourceType()
    {
        return resourceType;
    }

    public void setResourceType(String aResourceType)
    {
        resourceType = aResourceType;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String aVersion)
    {
        version = aVersion;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    public String getDetailUrl()
    {
        return detailUrl;
    }

    public void setDetailUrl(String aDetailUrl)
    {
        detailUrl = aDetailUrl;
    }

    public List<String> getConditionsOfUse()
    {
        return conditionsOfUse;
    }

    public void setConditionsOfUse(List<String> aConditionsOfUse)
    {
        conditionsOfUse = aConditionsOfUse;
    }

    public List<String> getLanguages()
    {
        return languages;
    }

    public void setLanguages(List<String> aLanguages)
    {
        languages = aLanguages;
    }

    public List<String> getFunctions()
    {
        return functions;
    }

    public void setFunctions(List<String> aFunctions)
    {
        functions = aFunctions;
    }

    public List<String> getIntendedApplications()
    {
        return intendedApplications;
    }

    public void setIntendedApplications(List<String> aIntendedApplications)
    {
        intendedApplications = aIntendedApplications;
    }

    public long getServiceExecutionCount()
    {
        return serviceExecutionCount;
    }

    public void setServiceExecutionCount(long aServiceExecutionCount)
    {
        serviceExecutionCount = aServiceExecutionCount;
    }

    public boolean isElgCompatibleService()
    {
        return elgCompatibleService;
    }

    public void setElgCompatibleService(boolean aElgCompatibleService)
    {
        elgCompatibleService = aElgCompatibleService;
    }
}
