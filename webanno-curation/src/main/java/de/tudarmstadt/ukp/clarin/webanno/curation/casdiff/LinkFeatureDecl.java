/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

public class LinkFeatureDecl
{
    private final String name;
    private final String roleFeature;
    private final String targetFeature;
    
    public LinkFeatureDecl(String aName, String aRoleFeature, String aTargetFeature)
    {
        name = aName;
        roleFeature = aRoleFeature;
        targetFeature = aTargetFeature;
    }

    public String getName()
    {
        return name;
    }

    public String getRoleFeature()
    {
        return roleFeature;
    }

    public String getTargetFeature()
    {
        return targetFeature;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("LinkFeatureDecl [name=");
        builder.append(getName());
        if (getRoleFeature() != null) {
            builder.append(", roleFeature=");
            builder.append(getRoleFeature());
        }
        if (getTargetFeature() != null) {
            builder.append(", targetFeature=");
            builder.append(getTargetFeature());
        }
        builder.append("]");
        return builder.toString();
    }
}
