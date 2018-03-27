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

import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class KBStatement<T extends Serializable>
    implements Serializable
{
    private static final long serialVersionUID = 6117845741665780184L;

    // Subject
    private KBHandle instance;
    
    // Predicate
    private KBHandle property;

    // Object
    private T value;
    
    // Language
    private String language;
    
    // Data type
    private String datatype;
    
    private String error;
    
    private boolean inferred;
    
    private List<Statement> originalStatements = new ArrayList<>();
    
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

    public T getValue()
    {
        return value;
    }

    public void setValue(T aValue)
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

    public String getDatatype()
    {
        return datatype;
    }

    public void setDatatype(String aDatatype)
    {
        datatype = aDatatype;
    }

    public String getError()
    {
        return error;
    }

    public void setError(String aError)
    {
        error = aError;
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
    
    public KBStatement() {
    }
    
    public KBStatement(KBStatement<T> other) {
        this.datatype = other.datatype;
        this.error = other.error;
        this.inferred = other.inferred;
        this.instance = other.instance;
        this.language = other.language;
        this.originalStatements = other.originalStatements;
        this.property = other.property;
        this.value = other.value;
    }

    public Statement toStatement(RepositoryConnection conn)
    {
        ValueFactory vf = conn.getValueFactory();
        IRI subject = vf.createIRI(instance.getIdentifier());
        IRI predicate = vf.createIRI(property.getIdentifier());
        
        Value object;
        if (value instanceof IRI) {
            object = (IRI) value;
        }
        else if (language != null) {
            object = vf.createLiteral((String) value, 
                    language);
        }
        else {
            DatatypeMapper mapper = new DefaultDatatypeMapper();
            object = mapper.getRDFValue(value, vf);
        }

        return vf.createStatement(subject, predicate, object);
    }   
    
    public void write(RepositoryConnection conn)
    {
        originalStatements.clear();
        Statement stmt = toStatement(conn);
        originalStatements.add(stmt);
        conn.add(stmt);
    }
    
    public static KBStatement read(KBHandle aInstance, KBHandle aProperty, boolean aInferred,
            Statement aStmt)
    {
        DatatypeMapper mapper = new DefaultDatatypeMapper();
        KBStatement<Serializable> kbStmt = new KBStatement<>();
        kbStmt.originalStatements.add(aStmt);
        kbStmt.setInstance(aInstance);
        kbStmt.setProperty(aProperty);
        kbStmt.setInferred(aInferred);
        if (aStmt.getObject() instanceof Literal) {
            Literal litValue = (Literal) aStmt.getObject();
            kbStmt.setDatatype(litValue.getDatatype().stringValue());
            kbStmt.setLanguage(litValue.getLanguage().orElse(null));
            try {
                kbStmt.setValue((Serializable) mapper.getJavaObject(litValue));
            }
            catch (Exception e) {
                kbStmt.setValue(litValue.stringValue());
                kbStmt.setError(e.getMessage());
            }
        }
        else if (aStmt.getObject() instanceof IRI) {
            kbStmt.setValue(aStmt.getObject());
        }
        else if (aStmt.getObject() instanceof BNode) {
            kbStmt.setValue(null);
            kbStmt.setError("Properties with blank node values are not supported");
        }
        else {
            throw new IllegalStateException("Unknown object type: " + aStmt.getObject().getClass());
        }
        return kbStmt;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("KBStatement [property=");
        builder.append(property);
        builder.append(", value=");
        builder.append(value);
        builder.append(", datatype=");
        builder.append(datatype);
        builder.append("]");
        return builder.toString();
    }
}
