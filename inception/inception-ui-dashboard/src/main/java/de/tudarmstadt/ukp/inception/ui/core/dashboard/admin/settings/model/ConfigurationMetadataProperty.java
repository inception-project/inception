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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurationMetadataProperty
{
    private String name;
    private String type;
    private String description;
    private String sourceType;
    private Object defaultValue;
    private boolean deprecated;
    private Deprecation deprecation;

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String aSourceType)
    {
        sourceType = aSourceType;
    }

    public Object getDefaultValue()
    {
        return defaultValue;
    }

    public void setDefaultValue(Object aDefaultValue)
    {
        defaultValue = aDefaultValue;
    }

    public boolean isDeprecated()
    {
        return deprecated || deprecation != null;
    }

    public void setDeprecated(boolean aDeprecated)
    {
        deprecated = aDeprecated;
    }

    public Deprecation getDeprecation()
    {
        return deprecation;
    }

    public void setDeprecation(Deprecation aDeprecation)
    {
        deprecation = aDeprecation;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Deprecation
    {
        private String level;
        private String reason;
        private String replacement;
        private String since;

        public String getLevel()
        {
            return level;
        }

        public void setLevel(String aLevel)
        {
            level = aLevel;
        }

        public String getReason()
        {
            return reason;
        }

        public void setReason(String aReason)
        {
            reason = aReason;
        }

        public String getReplacement()
        {
            return replacement;
        }

        public void setReplacement(String aReplacement)
        {
            replacement = aReplacement;
        }

        public String getSince()
        {
            return since;
        }

        public void setSince(String aSince)
        {
            since = aSince;
        }
    }
}
