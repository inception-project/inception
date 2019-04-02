/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.kb.graph;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KBHandle
    implements KBObject
{
    private static final long serialVersionUID = -4284462837460396185L;
    private String identifier;
    private String name;
    private String description;
    private KnowledgeBase kb;
    private String language;
    // domain and range for cases in which the KBHandle represents a property
    private String domain;
    private String range;
    private String debugInfo;

    public KBHandle()
    {
        this(null, null);
    }

    public KBHandle(String aIdentifier)
    {
        this(aIdentifier, null);
    }

    public KBHandle(String aIdentifier, String aLabel)
    {
        this(aIdentifier, aLabel, null);
    }

    public KBHandle(String aIdentifier, String aLabel, String aDescription)
    {
        identifier = aIdentifier;
        name = aLabel;
        description = aDescription;
    }

    public KBHandle(String aIdentifier, String aLabel, String aDescription, String aLanguage)
    {
        identifier = aIdentifier;
        name = aLabel;
        description = aDescription;
        language = aLanguage;
    }

    public KBHandle(String aIdentifier, String aLabel, String aDescription, String aLanguage,
            String aDomain, String aRange)
    {
        identifier = aIdentifier;
        name = aLabel;
        description = aDescription;
        language = aLanguage;
        domain = aDomain;
        range = aRange;
    }

    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String aDomain)
    {
        domain = aDomain;
    }

    public String getRange()
    {
        return range;
    }

    public void setRange(String aRange)
    {
        range = aRange;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    @Override
    public String getIdentifier()
    {
        return identifier;
    }

    @Override
    public void setIdentifier(String aIdentifier)
    {
        identifier = aIdentifier;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String aName)
    {
        name = aName;
    }
    
    @Override
    public KnowledgeBase getKB()
    {
        return kb;
    }

    @Override
    public void setKB(KnowledgeBase akb)
    {
        kb = akb;
    }

    @Override
    public String getLanguage()
    {
        return language;
    }

    @Override
    public void setLanguage(String aLanguage)
    {
        language = aLanguage;
    }
    
    public void setDebugInfo(String aDebugInfo)
    {
        debugInfo = aDebugInfo;
    }
    
    public String getDebugInfo()
    {
        return debugInfo;
    }

    public static KBHandle of(KBObject aObject)
    {
        return new KBHandle(aObject.getIdentifier(), aObject.getUiLabel());
    }

    public static <T extends KBObject> T convertTo(Class<T> aClass, KBHandle aHandle)
    {
        if (aClass == KBConcept.class) {
            KBConcept concept = new KBConcept();
            concept.setIdentifier(aHandle.getIdentifier());
            concept.setKB(aHandle.getKB());
            concept.setLanguage(aHandle.getLanguage());
            concept.setDescription(aHandle.getDescription());
            concept.setName(aHandle.getName());
            return (T) concept;
        }
        else if (aClass == KBInstance.class) {
            KBInstance instance = new KBInstance();
            instance.setIdentifier(aHandle.getIdentifier());
            instance.setKB(aHandle.getKB());
            instance.setLanguage(aHandle.getLanguage());
            instance.setDescription(aHandle.getDescription());
            instance.setName(aHandle.getName());
            return (T) instance;
        }
        else if (aClass == KBProperty.class) {
            KBProperty property = new KBProperty();
            property.setIdentifier(aHandle.getIdentifier());
            property.setKB(aHandle.getKB());
            property.setLanguage(aHandle.getLanguage());
            property.setDescription(aHandle.getDescription());
            property.setName(aHandle.getName());
            property.setRange(aHandle.getRange());
            property.setDomain(aHandle.getDomain());
            return (T) property;
        }
        else if (aClass == KBHandle.class) {
            return (T) aHandle;
        }
        else {
            throw new IllegalArgumentException(
                "Can not convert KBHandle to class " + aClass.getName());
        }
    }

    public static List<KBHandle> distinctByIri(List<KBHandle> aHandles)
    {
        Map<String, KBHandle> hMap = new LinkedHashMap<>();
        for (KBHandle h : aHandles) {
            hMap.put(h.getIdentifier(), h);
        }
        return new ArrayList<>(hMap.values());
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
        KBHandle kbHandle = (KBHandle) o;
        return Objects.equals(identifier, kbHandle.identifier);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(identifier);
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new ToStringBuilder(this, SHORT_PREFIX_STYLE);
        builder.append("identifier", identifier);
        builder.append("name", name);
        if (description != null) {
            builder.append("description", description);
        }
        if (language != null) {
            builder.append("language", language);
        }
        if (domain != null) {
            builder.append("domain", domain);
        }
        if (range != null) {
            builder.append("range", range);
        }
        return builder.toString();
    }
}
