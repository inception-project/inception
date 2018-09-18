/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.kb.util;

import java.net.URI;
import java.util.ArrayList;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;

public class TestFixtures
{
    private TestEntityManager entityManager;

    public TestFixtures(TestEntityManager aEntityManager)
    {
        entityManager = aEntityManager;
    }

    public Project createProject(String name)
    {
        Project project = new Project();
        project.setName(name);
        return entityManager.persist(project);
    }

    public KnowledgeBase buildKnowledgeBase(Project project, String name, Reification reification)
    {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setProject(project);
        kb.setType(RepositoryType.LOCAL);
        kb.setClassIri(RDFS.CLASS);
        kb.setSubclassIri(RDFS.SUBCLASSOF);
        kb.setTypeIri(RDF.TYPE);
        kb.setLabelIri(RDFS.LABEL);
        kb.setPropertyTypeIri(RDF.PROPERTY);
        kb.setDescriptionIri(RDFS.COMMENT);
        kb.setPropertyLabelIri(RDFS.LABEL);
        kb.setPropertyDescriptionIri(RDFS.COMMENT);
        kb.setExplicitlyDefinedRootConcepts(new ArrayList<>());
        kb.setReification(reification);
        kb.setMaxResults(1000);
        return kb;
    }

    public KBConcept buildConcept()
    {
        KBConcept concept = new KBConcept();
        concept.setName("Concept name");
        concept.setDescription("Concept description");
        return concept;
    }

    public KBConcept buildConceptWithLanguage(String aLanguage)
    {
        KBConcept concept = buildConcept();
        concept.setLanguage(aLanguage);
        return concept;
    }

    public KBProperty buildProperty()
    {
        KBProperty property = new KBProperty();
        property.setDescription("Property description");
        property.setDomain("https://test.schema.com/#domain");
        property.setName("Property name");
        property.setRange("https://test.schema.com/#range");
        property.setLanguage("en");
        return property;
    }

    public KBProperty buildPropertyWithLanguage(String aLanguage)
    {
        KBProperty property = buildProperty();
        property.setLanguage(aLanguage);
        return property;
    }

    public KBInstance buildInstance()
    {
        KBInstance instance = new KBInstance();
        instance.setName("Instance name");
        instance.setDescription("Instance description");
        instance.setType(URI.create("https://test.schema.com/#type"));
        instance.setLanguage("en");
        return instance;
    }

    public KBInstance buildInstanceWithLanguage(String aLanguage)
    {
        KBInstance instance = buildInstance();
        instance.setLanguage(aLanguage);
        return instance;
    }

    public KBStatement buildStatement(KBHandle conceptHandle, KBHandle propertyHandle, String value)
    {
        ValueFactory vf = SimpleValueFactory.getInstance();
        KBStatement statement = new KBStatement(conceptHandle, propertyHandle,
            vf.createLiteral(value));
        return statement;
    }

    public KBQualifier buildQualifier(KBStatement kbStatement, KBHandle propertyHandle,
        String value)
    {
        ValueFactory vf = SimpleValueFactory.getInstance();
        KBQualifier qualifier = new KBQualifier(kbStatement, propertyHandle,
            vf.createLiteral(value));
        return qualifier;
    }
}
