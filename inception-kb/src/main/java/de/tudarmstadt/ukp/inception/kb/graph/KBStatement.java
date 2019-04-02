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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KBStatement implements Serializable
{
    private static final long serialVersionUID = 6117845741665780184L;

    private String statementId;
    // Subject
    private KBHandle instance;

    // Predicate
    private KBHandle property;

    // Object
    private Object value;

    // Language
    private String language;

    private boolean inferred;

    private Set<Statement> originalStatements;

    private List<KBQualifier> qualifiers;

    /**
     * Call {@link KnowledgeBaseService#initStatement(KnowledgeBase, KBStatement)}
     * after constructing this in order to allow upserting.
     * @param aInstance {@link KBHandle} for the statement instance
     * @param aProperty {@link KBHandle} for the statement property
     * @param aValue Defines value for the statement
     */
    public KBStatement(KBHandle aInstance, KBHandle aProperty, Value aValue)
    {
        instance = aInstance;
        property = aProperty;

        DatatypeMapper mapper = new DefaultDatatypeMapper();
        if (aValue instanceof Literal) {
            Literal litValue = (Literal) aValue;
            language = litValue.getLanguage().orElse(null);
            value = mapper.getJavaObject(litValue);
        }
        else if (aValue instanceof IRI) {
            value = aValue;
        }
        else if (aValue instanceof BNode) {
            value = null;
        }
        else {
            throw new IllegalStateException("Unknown object type: " + aValue.getClass());
        }

        originalStatements = new HashSet<>();

        qualifiers = new ArrayList<>();
    }

    public KBStatement(KBHandle aInstance, KBHandle aProperty)
    {
        instance = aInstance;
        property = aProperty;
        value = null;
        originalStatements = new HashSet<>();
        qualifiers = new ArrayList<>();
    }

    public KBStatement(KBStatement other)
    {
        this.statementId = other.statementId;
        this.inferred = other.inferred;
        this.instance = other.instance;
        this.language = other.language;
        this.originalStatements = other.originalStatements;
        this.property = other.property;
        this.value = other.value;
        qualifiers = other.qualifiers;
    }

    public String getStatementId()
    {
        return statementId;
    }

    public void setStatementId(String aStatementId)
    {
        statementId = aStatementId;
    }

    public KBHandle getInstance()
    {
        return instance;
    }

    public void setInstance(KBHandle aInstance)
    {
        instance = aInstance;
    }

    public KBHandle getProperty()
    {
        return property;
    }

    public void setProperty(KBHandle aProperty)
    {
        property = aProperty;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue(Object aValue)
    {
        value = aValue;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String aLanguage)
    {
        language = aLanguage;
    }

    public boolean isInferred()
    {
        return inferred;
    }

    public void setInferred(boolean aInferred)
    {
        inferred = aInferred;
    }

    public Set<Statement> getOriginalStatements()
    {
        return originalStatements;
    }

    public void setOriginalStatements(Set<Statement> statements)
    {
        originalStatements = statements;
    }

    public void addQualifier(KBQualifier aQualifier)
    {
        qualifiers.add(aQualifier);
    }

    public List<KBQualifier> getQualifiers()
    {
        return qualifiers;
    }

    public void setQualifiers(List<KBQualifier> qualifierList)
    {
        qualifiers = qualifierList;
    }

    @Override public String toString()
    {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE).append("instance", instance)
                .append("property", property).append("value", value).append("language", language)
                .append("inferred", inferred).append("originalStatements", originalStatements)
                .toString();
    }
}
