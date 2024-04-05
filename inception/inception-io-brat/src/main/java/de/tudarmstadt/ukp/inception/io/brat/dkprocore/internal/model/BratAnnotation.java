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

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;

public abstract class BratAnnotation
{
    private final String id;
    private final String type;
    private final Map<String, BratAttribute> attributes = new LinkedHashMap<>();

    public BratAnnotation(String aId, String aType)
    {
        super();
        id = aId;
        type = aType;
    }

    public String getId()
    {
        return id;
    }

    public String getType()
    {
        return type;
    }

    public Collection<BratAttribute> getAttributes()
    {
        return attributes.values();
    }

    public BratAttribute addAttribute(int aId, String aName, String... aValues)
    {
        BratAttribute attr = new BratAttribute(aId, aName, getId(), aValues);
        addAttribute(attr);
        return attr;
    }

    public BratAttribute getAttribute(String aAttribute)
    {
        return attributes.get(aAttribute);
    }

    public void addAttribute(BratAttribute aAttribute)
    {
        String target = aAttribute.getTarget();

        // Check if attribute is already attached
        if (target != null && !target.equals(id)) {
            throw new IllegalArgumentException(
                    "Attribute already attached to annotation [" + target + "]");
        }

        // Attach to current annotation
        if (aAttribute.getId() == null) {
            aAttribute.setTarget(id);
        }

        attributes.put(aAttribute.getName(), aAttribute);
    }

    public abstract void write(JsonGenerator aJG) throws IOException;
}
