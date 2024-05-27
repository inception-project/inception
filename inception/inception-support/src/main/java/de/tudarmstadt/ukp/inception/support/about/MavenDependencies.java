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
package de.tudarmstadt.ukp.inception.support.about;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MavenDependencies
    implements Serializable
{
    private static final long serialVersionUID = -331660462538156631L;
    private String name;
    private String url;
    private String organizationUrl;
    private String organizationName;
    private String groupId;
    private String artifactId;
    private String version;
    private List<MavenDependency> licenses;
    private List<MavenDependency> dependencies;

    public void setName(String aName)
    {
        name = aName;
    }

    public String getName()
    {
        return name;
    }

    public void setUrl(String aUrl)
    {
        url = aUrl;
    }

    public String getUrl()
    {
        return url;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId(String aGroupId)
    {
        groupId = aGroupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId(String aArtifactId)
    {
        artifactId = aArtifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String aVersion)
    {
        version = aVersion;
    }

    public List<MavenDependency> getDependencies()
    {
        return dependencies;
    }

    public void setDependencies(List<MavenDependency> aDependencies)
    {
        dependencies = aDependencies;
    }

    public String getOrganizationUrl()
    {
        return organizationUrl;
    }

    public void setOrganizationUrl(String aOrganizationUrl)
    {
        organizationUrl = aOrganizationUrl;
    }

    public String getOrganizationName()
    {
        return organizationName;
    }

    public void setOrganizationName(String aOrganizationName)
    {
        organizationName = aOrganizationName;
    }

    public void setLicenses(List<MavenDependency> aLicenses)
    {
        licenses = aLicenses;
    }

    public List<MavenDependency> getLicenses()
    {
        return licenses;
    }
}
