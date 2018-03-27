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

import static de.tudarmstadt.ukp.inception.kb.graph.RdfUtils.readFirst;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class KBProperty
    implements KBObject
{
    private static final long serialVersionUID = 5520234069716526008L;
    
    private String identifier;
    private String name;
    private String description;
    private URI domain;
    private URI range;
    private List<Statement> originalStatements = new ArrayList<>();

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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    public URI getDomain()
    {
        return domain;
    }

    public void setDomain(URI aDomain)
    {
        domain = aDomain;
    }

    public URI getRange()
    {
        return range;
    }

    public void setRange(URI aRange)
    {
        range = aRange;
    }
    
    public List<Statement> getOriginalStatements()
    {
        return originalStatements;
    }
    
    public void write(RepositoryConnection aConn)
    {
        ValueFactory vf = aConn.getValueFactory();
        IRI subject = vf.createIRI(identifier);

        originalStatements.clear();

        Statement typeStmt = vf.createStatement(subject, RDF.TYPE, RDF.PROPERTY);
        originalStatements.add(typeStmt);
        aConn.add(typeStmt);

        if (isNotBlank(name)) {
            Statement nameStmt = vf.createStatement(subject, RDFS.LABEL, vf.createLiteral(name));
            originalStatements.add(nameStmt);
            aConn.add(nameStmt);
        }

        if (isNotBlank(description)) {
            Statement descStmt = vf.createStatement(subject, RDFS.COMMENT,
                    vf.createLiteral(description));
            originalStatements.add(descStmt);
            aConn.add(descStmt);
        }

        if (domain != null) {
            Statement domainStmt = vf.createStatement(subject, RDFS.DOMAIN,
                    vf.createLiteral(domain.toString()));
            originalStatements.add(domainStmt);
            aConn.add(domainStmt);
        }

        if (range != null) {
            Statement rangeStmt = vf.createStatement(subject, RDFS.RANGE,
                    vf.createLiteral(range.toString()));
            originalStatements.add(rangeStmt);
            aConn.add(rangeStmt);
        }
    }
    
    public static KBProperty read(RepositoryConnection aConn, Statement aStmt)
    {
        KBProperty kbProp = new KBProperty();
        kbProp.setIdentifier(aStmt.getSubject().stringValue());
        kbProp.originalStatements.add(aStmt);
        
        readFirst(aConn, aStmt.getSubject(), RDFS.LABEL, null).ifPresent((stmt) -> {
            kbProp.setName(stmt.getObject().stringValue());
            kbProp.originalStatements.add(stmt);
        });

        readFirst(aConn, aStmt.getSubject(), RDFS.COMMENT, null).ifPresent((stmt) -> {
            kbProp.setDescription(stmt.getObject().stringValue());
            kbProp.originalStatements.add(stmt);
        });

        readFirst(aConn, aStmt.getSubject(), RDFS.RANGE, null).ifPresent((stmt) -> {
            kbProp.setRange(URI.create(stmt.getObject().stringValue()));
            kbProp.originalStatements.add(stmt);
        });

        readFirst(aConn, aStmt.getSubject(), RDFS.DOMAIN, null).ifPresent((stmt) -> {
            kbProp.setDomain(URI.create(stmt.getObject().stringValue()));
            kbProp.originalStatements.add(stmt);
        });
        
        return kbProp;
    }
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("KBProperty [identifier=");
        builder.append(identifier);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        KBProperty other = (KBProperty) obj;
        if (identifier == null) {
            if (other.identifier != null) {
                return false;
            }
        }
        else if (!identifier.equals(other.identifier)) {
            return false;
        }
        return true;
    }
}
