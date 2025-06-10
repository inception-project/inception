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
package de.tudarmstadt.ukp.inception.kb.yaml;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;

public class KnowledgeBaseProfile
    implements Serializable
{
    public static final String KNOWLEDGEBASE_PROFILES_YAML = "knowledgebase-profiles.yaml";
    private static final long serialVersionUID = -2684575269500649910L;

    @JsonProperty("name")
    private String name;

    @JsonProperty("disabled")
    private boolean disabled = false;

    @JsonProperty("type")
    private RepositoryType type;

    @JsonProperty("access")
    private KnowledgeBaseAccess access;

    @JsonProperty("mapping")
    private KnowledgeBaseMapping mapping = new KnowledgeBaseMapping();

    @JsonProperty("root-concepts")
    private List<String> rootConcepts;

    @JsonProperty("additional-matching-properties")
    private List<String> additionalMatchingProperties;

    @JsonProperty("additional-languages")
    private List<String> additionalLanguages;

    @JsonProperty("info")
    private KnowledgeBaseInfo info;

    @JsonProperty("reification")
    private Reification reification = Reification.NONE;

    @JsonProperty("default-language")
    private String defaultLanguage;

    @JsonProperty("default-dataset")
    private String defaultDataset;

    public KnowledgeBaseProfile()
    {
    }

    public KnowledgeBaseProfile( //
            @JsonProperty("name") String aName, //
            @JsonProperty("disabled") boolean aDisabled, //
            @JsonProperty("type") RepositoryType aType, //
            @JsonProperty("access") KnowledgeBaseAccess aAccess, //
            @JsonProperty("mapping") KnowledgeBaseMapping aMapping, //
            @JsonProperty("root-concepts") List<String> aRootConcepts, //
            @JsonProperty("info") KnowledgeBaseInfo aInfo, //
            @JsonProperty("reification") Reification aReification, //
            @JsonProperty("default-language") String aDefaultLanguage, //
            @JsonProperty("default-dataset") String aDefaultDataset)
    {
        name = aName;
        disabled = aDisabled;
        type = aType;
        access = aAccess;
        mapping = aMapping;
        rootConcepts = aRootConcepts;
        info = aInfo;
        defaultLanguage = aDefaultLanguage;

        if (aReification != null) {
            reification = aReification;
        }

        if (aDefaultDataset != null) {
            defaultDataset = aDefaultDataset;
        }
    }

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled(boolean aDisabled)
    {
        disabled = aDisabled;
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
        return rootConcepts != null ? rootConcepts : emptyList();
    }

    public void setRootConcepts(List<String> aRootConcepts)
    {
        rootConcepts = aRootConcepts;
    }

    public List<String> getAdditionalMatchingProperties()
    {
        return additionalMatchingProperties;
    }

    public void setAdditionalMatchingProperties(List<String> aProperties)
    {
        additionalMatchingProperties = aProperties;
    }

    public List<String> getAdditionalLanguages()
    {
        return additionalLanguages;
    }

    public void setAdditionalLanguages(List<String> aLanguages)
    {
        additionalLanguages = aLanguages;
    }

    public KnowledgeBaseInfo getInfo()
    {
        return info;
    }

    public void setInfo(KnowledgeBaseInfo aInfo)
    {
        info = aInfo;
    }

    public Reification getReification()
    {
        if (reification == null) {
            return Reification.NONE;
        }

        return reification;
    }

    public void setReification(Reification aReification)
    {
        reification = aReification;
    }

    public String getDefaultLanguage()
    {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String aDefaultLanguage)
    {
        defaultLanguage = aDefaultLanguage;
    }

    public String getDefaultDataset()
    {
        return defaultDataset;
    }

    public void setDefaultDataset(String aDefaultDataset)
    {
        defaultDataset = aDefaultDataset;
    }

    /**
     * Reads knowledge base profiles from a YAML file and stores them in a HashMap with the key that
     * is defined in the file and a corresponding {@link KnowledgeBaseProfile} object as value
     * 
     * @return a HashMap with the knowledge base profiles
     * @throws IOException
     *             if an error occurs when reading the file
     */
    public static Map<String, KnowledgeBaseProfile> readKnowledgeBaseProfiles() throws IOException
    {
        try (var r = new InputStreamReader(
                KnowledgeBaseProfile.class.getResourceAsStream(KNOWLEDGEBASE_PROFILES_YAML),
                UTF_8)) {
            var mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(r, new TypeReference<HashMap<String, KnowledgeBaseProfile>>()
            {
            });
        }
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
        KnowledgeBaseProfile that = (KnowledgeBaseProfile) o;
        return Objects.equals(name, that.name) //
                && Objects.equals(disabled, that.disabled) //
                && Objects.equals(access, that.access) //
                && Objects.equals(mapping, that.mapping) //
                && Objects.equals(type, that.type) //
                && Objects.equals(rootConcepts, that.rootConcepts) //
                && Objects.equals(info, that.info) //
                && Objects.equals(reification, that.reification) //
                && Objects.equals(defaultLanguage, that.defaultLanguage) //
                && Objects.equals(defaultDataset, that.defaultDataset) //
                && Objects.equals(additionalMatchingProperties, that.additionalMatchingProperties) //
                && Objects.equals(additionalLanguages, that.additionalLanguages);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, disabled, type, access, mapping, rootConcepts, info, reification,
                defaultLanguage, defaultDataset, additionalMatchingProperties, additionalLanguages);
    }

    @Override
    public String toString()
    {
        return "[" + name + "]";
    }
}
