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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
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

    public static KBHandle of(KBObject aObject)
    {
        return new KBHandle(aObject.getIdentifier(), aObject.getUiLabel());
    }

    @Override
    public boolean equals(Object o) {
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
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
            .append("identifier", identifier)
            .append("name", name)
            .toString();
    }
}
