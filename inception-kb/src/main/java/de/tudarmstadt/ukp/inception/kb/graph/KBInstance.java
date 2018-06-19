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
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KBInstance
    implements KBObject
{
    private static final long serialVersionUID = -1207958632307301695L;

    private String identifier;
    private String name;
    private String description;
    private URI type;
    private List<Statement> originalStatements = new ArrayList<>();

    public KBInstance()
    {
        // No-args constructor
    }

    public KBInstance(String aIdentifier, String aName)
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

    public URI getType()
    {
        return type;
    }

    public void setType(URI aType)
    {
        type = aType;
    }

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

        Statement typeStmt = vf.createStatement(subject, kb.getTypeIri(),
                vf.createIRI(type.toString()));
        originalStatements.add(typeStmt);
        aConn.add(typeStmt);

        if (isNotBlank(name)) {
            Statement nameStmt = vf.createStatement(subject, kb.getLabelIri(),
                    vf.createLiteral(name));
            originalStatements.add(nameStmt);
            aConn.add(nameStmt);
        }

        if (isNotBlank(description)) {
            Statement descStmt = vf.createStatement(subject, kb.getDescriptionIri(),
                vf.createLiteral(description));
            originalStatements.add(descStmt);
            aConn.add(descStmt);
        }
    }

    public static KBInstance read(RepositoryConnection aConn, Statement aStmt, KnowledgeBase kb)
    {
        KBInstance kbInst = new KBInstance();
        kbInst.setType(URI.create(aStmt.getObject().stringValue()));
        kbInst.setIdentifier(aStmt.getSubject().stringValue());
        kbInst.originalStatements.add(aStmt);

        readFirst(aConn, aStmt.getSubject(), kb.getLabelIri(), null).ifPresent((stmt) -> {
            kbInst.setName(stmt.getObject().stringValue());
            kbInst.originalStatements.add(stmt);
        });

        readFirst(aConn, aStmt.getSubject(), kb.getDescriptionIri(), null).ifPresent((stmt) -> {
            kbInst.setDescription(stmt.getObject().stringValue());
            kbInst.originalStatements.add(stmt);
        });

        return kbInst;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KBInstance instance = (KBInstance) o;
        return Objects.equals(identifier, instance.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("identifier", identifier)
            .append("name", name)
            .append("description", description)
            .append("type", type)
            .append("originalStatements", originalStatements)
            .toString();
    }
}
