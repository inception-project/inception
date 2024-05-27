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
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

/**
 * A property in the context of a knowledge base is the predicate in a (subject, predicate, object)
 * triple.
 */
public class KBProperty
    implements KBObject
{
    private static final long serialVersionUID = 5520234069716526008L;

    private String identifier;
    private String name;
    private String description;
    private boolean deprecated;
    private String domain;
    private KnowledgeBase kb;
    private String range;
    private String language;

    private List<Statement> originalStatements = new ArrayList<>();

    public KBProperty()
    {
    }

    public KBProperty(String aIdentifier)
    {
        this(aIdentifier, null);
    }

    public KBProperty(String aIdentifier, String aName)
    {
        this(aIdentifier, aName, null, null);
    }

    public KBProperty(String aIdentifier, String aName, String aDomain, String aRange)
    {
        name = aName;
        identifier = aIdentifier;
        domain = aDomain;
        range = aRange;
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

    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String aDomain)
    {
        domain = aDomain;
    }

    public String getRange()
    {
        return range;
    }

    public void setRange(String aRange)
    {
        range = aRange;
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
                vf.createIRI(aKb.getPropertyTypeIri()));
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
            Statement nameStmt = vf.createStatement(subject,
                    vf.createIRI(aKb.getPropertyLabelIri()), nameLiteral);
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
            Statement descStmt = vf.createStatement(subject,
                    vf.createIRI(aKb.getPropertyDescriptionIri()), descriptionLiteral);
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
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KBProperty that = (KBProperty) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(identifier);
    }
}
