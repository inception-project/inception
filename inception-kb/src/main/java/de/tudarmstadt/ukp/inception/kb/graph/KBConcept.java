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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KBConcept
    implements KBObject
{
    private static final long serialVersionUID = -7289788139970822316L;

    private String identifier;
    private String name;
    private String description;

    /* Commented out until the functionality which uses them is actually implemented
    private static final IRI CLOSED;
    private boolean closed;

    private static final IRI ABSTRACT;
    private boolean abstractClass;
    */

    private List<Statement> originalStatements = new ArrayList<>();

    /* Commented out until the functionality which uses them is actually implemented
    static {
        ValueFactory factory = SimpleValueFactory.getInstance();
        CLOSED = factory.createIRI(IriConstants.INCEPTION_SCHEMA_NAMESPACE, "closed");
        ABSTRACT = factory.createIRI(IriConstants.INCEPTION_SCHEMA_NAMESPACE, "abstract");
    }
    */

    public KBConcept()
    {
        super();
    }

    public KBConcept(String aName)
    {
        name = aName;
    }

    public KBConcept(String aIdentifier, String aName)
    {
        identifier = aIdentifier;
        name = aName;
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

    /* Commented out until the functionality which uses them is actually implemented
    public void setAbstract(boolean aValue)
    {
        abstractClass = aValue;
    }
    */

    /**
     * Whether a class can have instances or not. E.g. classes that just serve as headings in a
     * hierarchical resource should not have instances.
     */
    /* Commented out until the functionality which uses them is actually implemented
    public boolean isAbstract()
    {
        return abstractClass;
    }
    */

    /* Commented out until the functionality which uses them is actually implemented
    public boolean isClosed()
    {
        return closed;
    }
    */

    /* Commented out until the functionality which uses them is actually implemented
    public void setClosed(boolean aClosed)
    {
        closed = aClosed;
    }
    */

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    public List<Statement> getOriginalStatements()
    {
        return originalStatements;
    }

    public void write(RepositoryConnection aConn, KnowledgeBase kb)
    {
        ValueFactory vf = aConn.getValueFactory();
        IRI subject = vf.createIRI(identifier);

        originalStatements.clear();

        Statement typeStmt = vf.createStatement(subject, kb.getTypeIri(), kb.getClassIri());
        originalStatements.add(typeStmt);
        aConn.add(typeStmt);

        if (isNotBlank(name)) {
            Statement nameStmt = vf.createStatement(subject, RDFS.LABEL, vf.createLiteral(name));
            originalStatements.add(nameStmt);
            aConn.add(nameStmt);
        }

        if (isNotBlank(description)) {
            Statement descStmt = vf
                .createStatement(subject, RDFS.COMMENT, vf.createLiteral(description));
            originalStatements.add(descStmt);
            aConn.add(descStmt);
        }

        /* Commented out until the functionality which uses them is actually implemented
        Statement closedStmt = vf.createStatement(subject, CLOSED, vf.createLiteral(closed));
        originalStatements.add(closedStmt);
        aConn.add(closedStmt);

        Statement abstractStmt = vf
            .createStatement(subject, ABSTRACT, vf.createLiteral(abstractClass));
        originalStatements.add(abstractStmt);
        aConn.add(abstractStmt);
        */
    }

    public static KBConcept read(RepositoryConnection aConn, Resource aSubject)
    {
        KBConcept kbConcept = new KBConcept();
        kbConcept.setIdentifier(aSubject.stringValue());

        readFirst(aConn, aSubject, RDFS.LABEL, null).ifPresent((stmt) -> {
            kbConcept.setName(stmt.getObject().stringValue());
            kbConcept.originalStatements.add(stmt);
        });

        readFirst(aConn, aSubject, RDFS.COMMENT, null).ifPresent((stmt) -> {
            kbConcept.setDescription(stmt.getObject().stringValue());
            kbConcept.originalStatements.add(stmt);
        });

        /* Commented out until the functionality which uses them is actually implemented
        readFirst(aConn, aStmt.getSubject(), CLOSED, null).ifPresent((stmt) -> {
            kbConcept.setClosed(((Literal) stmt.getObject()).booleanValue());
            kbConcept.originalStatements.add(stmt);
        });

        readFirst(aConn, aStmt.getSubject(), ABSTRACT, null).ifPresent((stmt) -> {
            kbConcept.setAbstract(((Literal) stmt.getObject()).booleanValue());
            kbConcept.originalStatements.add(stmt);
        });
        */

        return kbConcept;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("KBConcept [identifier=");
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
        KBConcept other = (KBConcept) obj;
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
