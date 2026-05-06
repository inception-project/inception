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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings;

import java.io.Serializable;

/**
 * One row of the admin Settings page: a Spring configuration property enriched with its effective
 * runtime value (already passed through the configured {@code Sanitizer}) and the property source
 * it came from.
 */
public class ConfigurationProperty
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String type;
    private final String description;
    private final String sourceType;
    private final String defaultValue;
    private final String effectiveValue;
    private final String effectiveSource;
    private final boolean deprecated;
    private final String deprecationNote;

    public ConfigurationProperty(String aName, String aType, String aDescription,
            String aSourceType, String aDefaultValue, String aEffectiveValue,
            String aEffectiveSource, boolean aDeprecated, String aDeprecationNote)
    {
        name = aName;
        type = aType;
        description = aDescription;
        sourceType = aSourceType;
        defaultValue = aDefaultValue;
        effectiveValue = aEffectiveValue;
        effectiveSource = aEffectiveSource;
        deprecated = aDeprecated;
        deprecationNote = aDeprecationNote;
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
    }

    public String getDescription()
    {
        return description;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public String getEffectiveValue()
    {
        return effectiveValue;
    }

    public String getEffectiveSource()
    {
        return effectiveSource;
    }

    public boolean isDeprecated()
    {
        return deprecated;
    }

    public String getDeprecationNote()
    {
        return deprecationNote;
    }
}
