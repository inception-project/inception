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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

public class KBQualifier
    implements Serializable
{
    private static final long serialVersionUID = 4648563545691138244L;

    private KBStatement statement;

    private String language;

    private KBProperty property;

    private Object value;

    private String valueLabel;

    private Set<Statement> originalTriples;

    public KBQualifier(String aKbProperty, Object aValue)
    {
        this(null, new KBProperty(aKbProperty), aValue);
    }

    public KBQualifier(KBStatement aStatement, KBProperty aKbProperty, Object aValue)
    {
        statement = aStatement;
        property = aKbProperty;
        setValue(aValue);
        originalTriples = new HashSet<>();
    }

    public KBQualifier(KBStatement aKbStatement)
    {
        statement = aKbStatement;
        originalTriples = new HashSet<>();
    }

    public KBQualifier(KBQualifier other)
    {
        statement = other.statement;
        property = other.property;
        value = other.value;
        language = other.language;
        originalTriples = other.originalTriples;
    }

    public KBStatement getStatement()
    {
        return statement;
    }

    public void setStatement(KBStatement aKBStatement)
    {
        statement = aKBStatement;
    }

    public KBProperty getProperty()
    {
        return property;
    }

    public void setProperty(KBProperty aKBProperty)
    {
        property = aKBProperty;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue(Object aValue)
    {
        if (aValue instanceof Value) {
            if (aValue instanceof Literal) {
                Literal litValue = (Literal) aValue;
                try {
                    language = litValue.getLanguage().orElse(null);
                    value = new DefaultDatatypeMapper().getJavaObject(litValue);
                }
                catch (Exception e) {
                    value = "ERROR converting [" + litValue + "]: " + e.getMessage();
                }
            }
            else if (aValue instanceof IRI) {
                value = aValue;
            }
            else if (aValue instanceof BNode) {
                value = null;
            }
            else {
                value = "ERROR: Unknown object type: " + aValue.getClass();
            }
        }
        else {
            value = aValue;
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

    public Set<Statement> getOriginalTriples()
    {
        return originalTriples;
    }

    public void setOriginalTriples(Set<Statement> aOriginalStatements)
    {
        originalTriples = aOriginalStatements;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("kbStatement", statement).append("language", language)
                .append("kbProperty", property).append("value", value)
                .append("originalStatements", originalTriples).toString();
    }

    public String getValueLabel()
    {
        return valueLabel;
    }

    public void setValueLabel(String aValueLabel)
    {
        valueLabel = aValueLabel;
    }
}
