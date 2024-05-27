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

import static de.tudarmstadt.ukp.inception.support.about.ApplicationInformation.normaliseLicense;
import static de.tudarmstadt.ukp.inception.support.about.ApplicationInformation.normaliseSource;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MavenDependency
    implements Serializable
{
    private static final long serialVersionUID = -5898197927835864702L;
    private String name;
    private String url;
    private String organizationUrl;
    private String organizationName;
    private String groupId;
    private String artifactId;
    private String version;
    private List<MavenLicense> licenses;

    public void setName(String aName)
    {
        name = aName;
    }

    public String getName()
    {
        return name;
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

    public void setLicenses(List<MavenLicense> aLicenses)
    {
        licenses = aLicenses;
    }

    public List<MavenLicense> getLicenses()
    {
        return licenses;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String aUrl)
    {
        url = aUrl;
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

    public Dependency toDependency()
    {
        return new AbstractDependency()
        {

            @Override
            public String getUrl()
            {
                return MavenDependency.this.getUrl();
            }

            @Override
            public String getSource()
            {
                return normaliseSource(MavenDependency.this.getOrganizationName());
            }

            @Override
            public String getName()
            {
                return MavenDependency.this.getName();
            }

            @Override
            public String getVersion()
            {
                return MavenDependency.this.getVersion();
            }

            @Override
            public List<String> getLicenses()
            {
                return MavenDependency.this.getLicenses().stream() //
                        .map(l -> normaliseLicense(l.toString())) //
                        .collect(toList());
            }
        };
    }

}
