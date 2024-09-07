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
package de.tudarmstadt.ukp.inception.kb.graph;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KBHandle
    implements KBObject
{
    private static final long serialVersionUID = -4284462837460396185L;
    private String identifier;
    private String name;
    private String queryBestMatchTerm;
    private Set<Pair<String, String>> matchTerms;
    private String description;
    private KnowledgeBase kb;
    private String language;
    private boolean deprecated;

    private int rank;
    private double score;
    private String debugInfo;

    // domain and range for cases in which the KBHandle represents a property
    @Deprecated
    private String domain;

    @Deprecated
    private String range;

    private KBHandle(Builder builder)
    {
        identifier = builder.identifier;
        name = builder.name;
        queryBestMatchTerm = builder.queryBestMatchTerm;
        matchTerms = builder.matchTerms;
        description = builder.description;
        kb = builder.kb;
        language = builder.language;
        deprecated = builder.deprecated;
        rank = builder.rank;
        score = builder.score;
        debugInfo = builder.debugInfo;
        domain = builder.domain;
        range = builder.range;
    }

    public KBHandle(KBHandle aOther)
    {
        identifier = aOther.identifier;
        name = aOther.name;
        queryBestMatchTerm = aOther.queryBestMatchTerm;
        matchTerms = aOther.matchTerms;
        description = aOther.description;
        kb = aOther.kb;
        language = aOther.language;
        deprecated = aOther.deprecated;
        rank = aOther.rank;
        score = aOther.score;
        debugInfo = aOther.debugInfo;
        domain = aOther.domain;
        range = aOther.range;
    }

    @Deprecated
    public KBHandle()
    {
        this(null, null);
    }

    @Deprecated
    public KBHandle(String aIdentifier)
    {
        this(aIdentifier, null);
    }

    @Deprecated
    public KBHandle(String aIdentifier, String aLabel)
    {
        this(aIdentifier, aLabel, null, null);
    }

    protected KBHandle(String aIdentifier, String aLabel, String aDescription)
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

    @Deprecated
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

    public void setDeprecated(boolean aDeprecated)
    {
        deprecated = aDeprecated;
    }

    @Override
    public boolean isDeprecated()
    {
        return deprecated;
    }

    @Deprecated
    public String getDomain()
    {
        return domain;
    }

    @Deprecated
    public void setDomain(String aDomain)
    {
        domain = aDomain;
    }

    @Deprecated
    public String getRange()
    {
        return range;
    }

    @Deprecated
    public void setRange(String aRange)
    {
        range = aRange;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
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

    public void addMatchTerm(String aLabel, String aLanguage)
    {
        if (matchTerms == null) {
            matchTerms = new LinkedHashSet<>();
        }

        matchTerms.add(Pair.of(aLabel, aLanguage));
    }

    public Set<Pair<String, String>> getMatchTerms()
    {
        if (matchTerms == null) {
            return emptySet();
        }

        return matchTerms;
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

    public void setQueryBestMatchTerm(String aTerm)
    {
        queryBestMatchTerm = aTerm;
    }

    public String getQueryBestMatchTerm()
    {
        return queryBestMatchTerm;
    }

    public int getRank()
    {
        return rank;
    }

    public void setRank(int aRank)
    {
        rank = aRank;
    }

    public double getScore()
    {
        return score;
    }

    public void setScore(double aScore)
    {
        score = aScore;
    }

    public static KBHandle of(KBObject aObject)
    {
        return aObject.toKBHandle();
    }

    @SuppressWarnings("unchecked")
    public static <T extends KBObject> T convertTo(Class<T> aClass, KBHandle aHandle)
    {
        if (aClass == KBConcept.class) {
            KBConcept concept = new KBConcept();
            concept.setIdentifier(aHandle.getIdentifier());
            concept.setKB(aHandle.getKB());
            concept.setLanguage(aHandle.getLanguage());
            concept.setDescription(aHandle.getDescription());
            concept.setDeprecated(aHandle.isDeprecated());
            concept.setName(aHandle.getName());
            return (T) concept;
        }
        else if (aClass == KBInstance.class) {
            KBInstance instance = new KBInstance();
            instance.setIdentifier(aHandle.getIdentifier());
            instance.setKB(aHandle.getKB());
            instance.setLanguage(aHandle.getLanguage());
            instance.setDescription(aHandle.getDescription());
            instance.setDeprecated(aHandle.isDeprecated());
            instance.setName(aHandle.getName());
            return (T) instance;
        }
        else if (aClass == KBProperty.class) {
            KBProperty property = new KBProperty();
            property.setIdentifier(aHandle.getIdentifier());
            property.setKB(aHandle.getKB());
            property.setLanguage(aHandle.getLanguage());
            property.setDescription(aHandle.getDescription());
            property.setDeprecated(aHandle.isDeprecated());
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

    public static <T extends KBObject> List<T> distinctByIri(List<T> aHandles)
    {
        Map<String, T> hMap = new LinkedHashMap<>();
        for (T h : aHandles) {
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
        if (matchTerms != null && !matchTerms.isEmpty()) {
            builder.append("matchTerms", matchTerms);
        }
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
        if (score != 0.0) {
            builder.append("score", score);
        }
        if (deprecated) {
            builder.append("deprecated", deprecated);
        }
        return builder.toString();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String identifier;
        private String name;
        private String queryBestMatchTerm;
        private Set<Pair<String, String>> matchTerms = Collections.emptySet();
        private String description;
        private KnowledgeBase kb;
        private String language;
        private boolean deprecated;
        private int rank;
        private double score;
        private String debugInfo;
        private String domain;
        private String range;

        private Builder()
        {
        }

        public Builder withIdentifier(String aIdentifier)
        {
            identifier = aIdentifier;
            return this;
        }

        public Builder withName(String aName)
        {
            name = aName;
            return this;
        }

        public Builder withQueryBestMatchTerm(String aQueryBestMatchTerm)
        {
            queryBestMatchTerm = aQueryBestMatchTerm;
            return this;
        }

        public Builder withMatchTerms(Set<Pair<String, String>> aMatchTerms)
        {
            matchTerms = aMatchTerms;
            return this;
        }

        public Builder withDescription(String aDescription)
        {
            description = aDescription;
            return this;
        }

        public Builder withKb(KnowledgeBase aKb)
        {
            this.kb = aKb;
            return this;
        }

        public Builder withLanguage(String aLanguage)
        {
            language = aLanguage;
            return this;
        }

        public Builder withDeprecated(boolean aDeprecated)
        {
            deprecated = aDeprecated;
            return this;
        }

        public Builder withRank(int aRank)
        {
            rank = aRank;
            return this;
        }

        public Builder withScore(double aScore)
        {
            score = aScore;
            return this;
        }

        public Builder withDebugInfo(String aDebugInfo)
        {
            debugInfo = aDebugInfo;
            return this;
        }

        @Deprecated
        public Builder withDomain(String aDomain)
        {
            domain = aDomain;
            return this;
        }

        @Deprecated
        public Builder withRange(String aRange)
        {
            range = aRange;
            return this;
        }

        public KBHandle build()
        {
            return new KBHandle(this);
        }
    }
}
