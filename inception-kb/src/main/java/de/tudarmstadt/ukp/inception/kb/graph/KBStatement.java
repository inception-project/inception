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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.URIUtil;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class KBStatement implements Serializable
{
    private static final long serialVersionUID = 6117845741665780184L;

    // Subject
    private KBHandle instance;

    // Predicate
    private KBHandle property;

    // Object
    private Object value;

    // Language
    private String language;

    private boolean inferred;

    private List<Statement> originalStatements = new ArrayList<>();

    public KBStatement()
    {
    }

    public KBStatement(KBStatement other)
    {
        this.inferred = other.inferred;
        this.instance = other.instance;
        this.language = other.language;
        this.originalStatements = other.originalStatements;
        this.property = other.property;
        this.value = other.value;
    }

    public static KBStatement read(KBHandle aInstance, KBHandle aProperty, Statement aStmt)
    {
        DatatypeMapper mapper = new DefaultDatatypeMapper();
        KBStatement kbStmt = new KBStatement();
        kbStmt.originalStatements.add(aStmt);
        kbStmt.setInstance(aInstance);
        kbStmt.setProperty(aProperty);
        if (aStmt.getObject() instanceof Literal) {
            Literal litValue = (Literal) aStmt.getObject();
            kbStmt.setLanguage(litValue.getLanguage().orElse(null));
            kbStmt.setValue((Serializable) mapper.getJavaObject(litValue));
        }
        else if (aStmt.getObject() instanceof IRI) {
            kbStmt.setValue(aStmt.getObject());
        }
        else if (aStmt.getObject() instanceof BNode) {
            kbStmt.setValue(null);
        }
        else {
            throw new IllegalStateException("Unknown object type: " + aStmt.getObject().getClass());
        }
        return kbStmt;
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

    public List<Statement> getOriginalStatements()
    {
        return originalStatements;
    }

    public Statement toStatement(RepositoryConnection conn)
    {
        ValueFactory vf = conn.getValueFactory();
        IRI subject = vf.createIRI(instance.getIdentifier());
        IRI predicate = vf.createIRI(property.getIdentifier());

        Value object;
        if (value instanceof IRI) {
            object = (IRI) value;
        } else if (URIUtil.isValidURIReference((String) value)) {
            object = vf.createIRI((String) value);
        }
        else if (language != null) {
            object = vf.createLiteral((String) value, language);
        }
        else {
            DatatypeMapper mapper = new DefaultDatatypeMapper();
            object = mapper.getRDFValue(value, vf);
        }


        return vf.createStatement(subject, predicate, object);
    }

    public void write(RepositoryConnection conn)
    {
        Statement stmt = toStatement(conn);
        if (!inferred) {

        }
        conn.add(stmt);
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("instance", instance)
            .append("property", property)
            .append("value", value)
            .append("language", language)
            .append("inferred", inferred)
            .append("originalStatements", originalStatements)
            .toString();
    }
}
