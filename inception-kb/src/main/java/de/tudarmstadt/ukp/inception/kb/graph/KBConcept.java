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
import static de.tudarmstadt.ukp.inception.kb.graph.RdfUtils.readFirstLabel;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KBConcept
    implements KBObject
{
    private static final long serialVersionUID = -7289788139970822316L;

    private String identifier;
    private String name;
    private String description;
    private KnowledgeBase kb;
    private String language;

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
    
    /**
     * @return Gives description for the concept
     */
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
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
            Literal nameLiteral;
            if (language == null) {
                nameLiteral = vf.createLiteral(name);
            }
            else {
                nameLiteral = vf.createLiteral(name, language);
            }
            Statement nameStmt = vf.createStatement(subject, kb.getLabelIri(), nameLiteral);
            originalStatements.add(nameStmt);
            aConn.add(nameStmt);
        }

        if (isNotBlank(description)) {
            Literal descriptionLiteral;
            if (language == null) {
                descriptionLiteral = vf.createLiteral(description);
            }
            else {
                descriptionLiteral = vf.createLiteral(description, language);
            }
            Statement descStmt = vf
                .createStatement(subject, kb.getDescriptionIri(), descriptionLiteral);
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
    
    public static KBConcept read(RepositoryConnection aConn, Resource aSubject, KnowledgeBase kb)
    {
        KBConcept kbConcept = new KBConcept();
        kbConcept.setIdentifier(aSubject.stringValue());
        kbConcept.setKB(kb);
        readFirstLabel(aConn, kb, aSubject, kb.getDefaultLanguage())
            .ifPresent((stmt) -> {
                kbConcept.setName(stmt.getObject().stringValue());
                kbConcept.originalStatements.add(stmt);
                if (stmt.getObject() instanceof Literal) {
                    Literal literal = (Literal) stmt.getObject();
                    Optional<String> language = literal.getLanguage();
                    language.ifPresent(kbConcept::setLanguage);
                }
            });

        readFirst(aConn, aSubject, kb.getDescriptionIri(), null, kb.getDefaultLanguage())
            .ifPresent((stmt) -> {
                kbConcept.setDescription(stmt.getObject().stringValue());
                kbConcept.originalStatements.add(stmt);
                if (stmt.getObject() instanceof Literal) {
                    Literal literal = (Literal) stmt.getObject();
                    Optional<String> language = literal.getLanguage();
                    language.ifPresent(kbConcept::setLanguage);
                }
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
