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
package de.tudarmstadt.ukp.inception.kb.yaml;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KnowledgeBaseInfo
    implements Serializable
{
    private static final long serialVersionUID = -2667645577002890141L;

    @JsonProperty("description")
    private String description;

    @JsonProperty("host-institution-name")
    private String hostInstitutionName;

    @JsonProperty("author-name")
    private String authorName;

    @JsonProperty("website-url")
    private String websiteUrl;

    @JsonProperty("license-name")
    private String licenseName;

    @JsonProperty("license-url")
    private String licenseUrl;

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    public String getHostInstitutionName()
    {
        return hostInstitutionName;
    }

    public void setHostInstitutionName(String aHostInstitutionName)
    {
        hostInstitutionName = aHostInstitutionName;
    }

    public String getAuthorName()
    {
        return authorName;
    }

    public void setAuthorName(String aAuthorName)
    {
        authorName = aAuthorName;
    }

    public String getWebsiteUrl()
    {
        return websiteUrl;
    }

    public void setWebsiteUrl(String aWebsiteURL)
    {
        websiteUrl = aWebsiteURL;
    }

    public String getLicenseName()
    {
        return licenseName;
    }

    public void setLicenseName(String aLicenseName)
    {
        licenseName = aLicenseName;
    }

    public String getLicenseUrl()
    {
        return licenseUrl;
    }

    public void setLicenseUrl(String aLicenseUrl)
    {
        licenseUrl = aLicenseUrl;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnowledgeBaseInfo that = (KnowledgeBaseInfo) o;
        return Objects.equals(description, that.description)
                && Objects.equals(hostInstitutionName, that.hostInstitutionName)
                && Objects.equals(authorName, that.authorName)
                && Objects.equals(websiteUrl, that.websiteUrl)
                && Objects.equals(licenseName, that.licenseName)
                && Objects.equals(licenseUrl, that.licenseUrl);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(description, hostInstitutionName, authorName, websiteUrl, licenseName,
                licenseUrl);
    }
}
