/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class BratAttributeDecl
{
    private final String name;
    private final Set<String> targetTypes;
    private final Set<String> values = new LinkedHashSet<>();

    public BratAttributeDecl(String aName, String... aTargetTypes)
    {
        this(aName, aTargetTypes == null ? null : Arrays.asList(aTargetTypes));
    }

    public BratAttributeDecl(String aName, Collection<String> aTargetTypes)
    {
        name = aName;
        targetTypes = aTargetTypes == null ? Collections.emptySet()
                : new LinkedHashSet<>(aTargetTypes);
    }

    public String getName()
    {
        return name;
    }

    public Set<String> getTargetTypes()
    {
        return targetTypes;
    }

    public Set<String> getValues()
    {
        return values;
    }

    public void addValue(String aValue)
    {
        values.add(aValue);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append('\t');
        sb.append("Arg:");
        sb.append(StringUtils.join(targetTypes, "|"));
        if (!values.isEmpty()) {
            sb.append(", Value:");
            sb.append(StringUtils.join(values, "|"));
        }
        return sb.toString();
    }
}
