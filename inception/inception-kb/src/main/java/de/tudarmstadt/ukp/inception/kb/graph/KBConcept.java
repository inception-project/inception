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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
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
    private boolean deprecated;
    private KnowledgeBase kb;
    private String language;

    /* Commented out until the functionality which uses them is actually implemented
    // @formatter:off
    private static final IRI CLOSED;
    private boolean closed;

    private static final IRI ABSTRACT;
    private boolean abstractClass;
    // @formatter:on
    */

    private List<Statement> originalStatements = new ArrayList<>();

    /* Commented out until the functionality which uses them is actually implemented
    // @formatter:off
    static {
        ValueFactory factory = SimpleValueFactory.getInstance();
        CLOSED = factory.createIRI(IriConstants.INCEPTION_SCHEMA_NAMESPACE, "closed");
        ABSTRACT = factory.createIRI(IriConstants.INCEPTION_SCHEMA_NAMESPACE, "abstract");
    }
    // @formatter:on
    */

    public KBConcept()
    {
        super();
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
    // @formatter:off
    public void setAbstract(boolean aValue)
    {
        abstractClass = aValue;
    }
    // @formatter:on
    */

    /**
     * Whether a class can have instances or not. E.g. classes that just serve as headings in a
     * hierarchical resource should not have instances.
     */
    /* Commented out until the functionality which uses them is actually implemented
    // @formatter:off
    public boolean isAbstract()
    {
        return abstractClass;
    }
    // @formatter:on
    */

    /* Commented out until the functionality which uses them is actually implemented
    // @formatter:off
    public boolean isClosed()
    {
        return closed;
    }
    // @formatter:on
    */

    /* Commented out until the functionality which uses them is actually implemented
    // @formatter:off
    public void setClosed(boolean aClosed)
    {
        closed = aClosed;
    }
    // @formatter:on
    */

    /**
     * @return Gives description for the concept
     */
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

    public void setDeprecated(boolean aDeprecated)
    {
        deprecated = aDeprecated;
    }

    @Override
    public boolean isDeprecated()
    {
        return deprecated;
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

    public void write(RepositoryConnection aConn, KnowledgeBase aKb)
    {
        ValueFactory vf = aConn.getValueFactory();
        IRI subject = vf.createIRI(identifier);

        originalStatements.clear();

        Statement typeStmt = vf.createStatement(subject, vf.createIRI(aKb.getTypeIri()),
                vf.createIRI(aKb.getClassIri()));
        originalStatements.add(typeStmt);
        aConn.add(typeStmt);

        if (isNotBlank(name)) {
            Literal nameLiteral;
            if (language != null) {
                nameLiteral = vf.createLiteral(name, language);
            }
            else if (aKb.getDefaultLanguage() != null) {
                nameLiteral = vf.createLiteral(name, aKb.getDefaultLanguage());
            }
            else {
                nameLiteral = vf.createLiteral(name);
            }
            Statement nameStmt = vf.createStatement(subject, vf.createIRI(aKb.getLabelIri()),
                    nameLiteral);
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
            Statement descStmt = vf.createStatement(subject, vf.createIRI(aKb.getDescriptionIri()),
                    descriptionLiteral);
            originalStatements.add(descStmt);
            aConn.add(descStmt);
        }

        /* Commented out until the functionality which uses them is actually implemented
        // @formatter:off
        Statement closedStmt = vf.createStatement(subject, CLOSED, vf.createLiteral(closed));
        originalStatements.add(closedStmt);
        aConn.add(closedStmt);

        Statement abstractStmt = vf
            .createStatement(subject, ABSTRACT, vf.createLiteral(abstractClass));
        originalStatements.add(abstractStmt);
        aConn.add(abstractStmt);
    // @formatter:on
        */
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("identifier", identifier).append("name", name)
                .append("language", language).append("description", description).toString();
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
