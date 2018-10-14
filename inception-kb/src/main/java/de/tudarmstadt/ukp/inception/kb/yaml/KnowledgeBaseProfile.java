/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.kb.yaml;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.inception.kb.RepositoryType;

public class KnowledgeBaseProfile implements Serializable
{
    private static final long serialVersionUID = -2684575269500649910L;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private RepositoryType type;
    
    @JsonProperty("access")
    private KnowledgeBaseAccess access;

    @JsonProperty("mapping")
    private KnowledgeBaseMapping mapping;
    
    @JsonProperty("root-concepts")
    private List<String> rootConcepts;

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public RepositoryType getType()
    {
        return type;
    }

    public void setType(RepositoryType aType)
    {
        type = aType;
    }

    public KnowledgeBaseAccess getAccess()
    {
        return access;
    }

    public void setAccess(KnowledgeBaseAccess aKbAccess)
    {
        access = aKbAccess;
    }

    public KnowledgeBaseMapping getMapping()
    {
        return mapping;
    }

    public void setMapping(KnowledgeBaseMapping aMapping)
    {
        mapping = aMapping;
    }

    public List<String> getRootConcepts()
    {
        return rootConcepts;
    }

    public void setRootConcepts(List<String> rootConcepts)
    {
        this.rootConcepts = rootConcepts;
    }

    @Override public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnowledgeBaseProfile that = (KnowledgeBaseProfile) o;
        return Objects.equals(name, that.name) && Objects.equals(access, that.access)
                && Objects.equals(mapping, that.mapping) && Objects.equals(type, that.type)
                && Objects.equals(rootConcepts, that.rootConcepts);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, type, access, mapping,rootConcepts);
    }
}
