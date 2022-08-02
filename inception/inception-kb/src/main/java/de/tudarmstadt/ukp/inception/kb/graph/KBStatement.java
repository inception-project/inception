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

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class KBStatement
    implements Serializable
{
    private static final long serialVersionUID = 6117845741665780184L;

    private String statementId;

    // Subject
    private KBHandle instance;

    // Predicate
    private KBProperty property;

    // Object
    private Object value;

    private String valueLabel;

    // Language
    private String language;

    private boolean inferred;

    private Set<Statement> originalTriples;

    private List<KBQualifier> qualifiers;

    public KBStatement(String aId, String aInstance)
    {
        this(aId, new KBHandle(aInstance), null, null);
    }

    public KBStatement(String aId, String aInstance, String aProperty, Object aValue,
            String aValueLabel)
    {
        this(aId, new KBHandle(aInstance), new KBProperty(aProperty), aValue);
        setValueLabel(aValueLabel);
    }

    /**
     * @param aId
     *            statement IRI
     * @param aInstance
     *            {@link KBHandle} for the statement instance
     * @param aProperty
     *            {@link KBHandle} for the statement property
     * @param aValue
     *            Defines value for the statement
     */
    public KBStatement(String aId, KBHandle aInstance, KBProperty aProperty, Object aValue)
    {
        statementId = aId;
        instance = aInstance;
        property = aProperty;

        setValue(aValue);

        originalTriples = new HashSet<>();
        qualifiers = new ArrayList<>();
    }

    public KBStatement(KBHandle aInstance, KBProperty aProperty)
    {
        instance = aInstance;
        property = aProperty;
        value = null;
        originalTriples = new HashSet<>();
        qualifiers = new ArrayList<>();
    }

    public KBStatement(KBHandle aInstance)
    {
        this(aInstance, null);
    }

    public KBStatement(KBStatement other)
    {
        this.statementId = other.statementId;
        this.inferred = other.inferred;
        this.instance = other.instance;
        this.language = other.language;
        this.originalTriples = other.originalTriples;
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

    public KBProperty getProperty()
    {
        return property;
    }

    public void setProperty(KBProperty aProperty)
    {
        property = aProperty;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue(Object aValue)
    {
        if (aValue instanceof Value) {
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
        }
        else {
            value = aValue;
        }

        if (value instanceof URI) {
            value = SimpleValueFactory.getInstance().createIRI(((URI) value).toString());
        }
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

    public Set<Statement> getOriginalTriples()
    {
        return originalTriples;
    }

    public void setOriginalTriples(Set<Statement> statements)
    {
        originalTriples = statements;
    }

    public void addQualifier(KBQualifier aQualifier)
    {
        aQualifier.setStatement(this);
        if (!qualifiers.contains(aQualifier)) {
            qualifiers.add(aQualifier);
        }
    }

    public List<KBQualifier> getQualifiers()
    {
        return qualifiers;
    }

    public void setQualifiers(List<KBQualifier> qualifierList)
    {
        qualifiers = qualifierList;
    }

    public String getValueLabel()
    {
        return valueLabel;
    }

    public void setValueLabel(String aValueLabel)
    {
        valueLabel = aValueLabel;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("statementId", statementId).append("instance", instance)
                .append("property", property).append("value", value)
                .append("valueLabel", valueLabel).append("language", language)
                .append("inferred", inferred).append("originalTriples", originalTriples)
                .append("qualifiers", qualifiers).toString();
    }
}
