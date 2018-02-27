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
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class KBStatement implements Serializable
{
    private static final long serialVersionUID = 6117845741665780184L;

    // Subject
    private KBHandle instance;

    // Predicate
    private KBHandle property;

    // Object
    private Value value;

    private boolean inferred;

    private List<Statement> originalStatements = new ArrayList<>();

    public KBStatement()
    {
    }

    public KBStatement(KBStatement other)
    {
        this.inferred = other.inferred;
        this.instance = other.instance;
        this.originalStatements = other.originalStatements;
        this.property = other.property;
        this.value = other.value;
    }

    public static KBStatement read(KBHandle aInstance, KBHandle aProperty, Statement aStmt)
    {
        KBStatement kbStmt = new KBStatement();
        kbStmt.originalStatements.add(aStmt);
        kbStmt.setInstance(aInstance);
        kbStmt.setProperty(aProperty);       
        if (aStmt.getObject() instanceof Literal || aStmt.getObject() instanceof IRI) {
            kbStmt.setValue(aStmt.getObject());
        } else if (aStmt.getObject() instanceof BNode) {
            kbStmt.setValue(null);
        } else {
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

    public void setValue(Value aValue)
    {
        value = aValue;
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
        Value object = value;
        return vf.createStatement(subject, predicate, object);
    }

    public void write(RepositoryConnection conn)
    {
        originalStatements.clear();
        Statement stmt = toStatement(conn);
        originalStatements.add(stmt);
        conn.add(stmt);
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("instance", instance)
            .append("property", property)
            .append("value", value)
            .append("inferred", inferred)
            .append("originalStatements", originalStatements)
            .toString();
    }
}
