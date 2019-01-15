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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.tudarmstadt.ukp.inception.kb.RepositoryType;

public class KnowledgeBaseProfile implements Serializable
{
    private static final String KNOWLEDGEBASE_PROFILES_YAML = "knowledgebase-profiles.yaml";
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

    public static Map<String, KnowledgeBaseProfile> readKnowledgeBaseProfiles()
        throws IOException
    {
        try (Reader r = new InputStreamReader(
            KnowledgeBaseProfile.class.getResourceAsStream(KNOWLEDGEBASE_PROFILES_YAML),
            StandardCharsets.UTF_8)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(r,
                new TypeReference<HashMap<String, KnowledgeBaseProfile>>(){});
        }
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
