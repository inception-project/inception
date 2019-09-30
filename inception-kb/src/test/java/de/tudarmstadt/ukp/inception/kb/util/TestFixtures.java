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

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.junit.Assume;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
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
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);
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
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF);
        kb.setFullTextSearchIri(IriConstants.FTS_LUCENE);
        // Intentionally using different IRIs for label/description and property-label/description
        // to detect cases where we accidentally construct queries using the wrong mapping, e.g.
        // querying for properties with the class label.
        kb.setPropertyLabelIri(SKOS.PREF_LABEL);
        kb.setPropertyDescriptionIri(SKOS.DEFINITION);
        kb.setRootConcepts(new ArrayList<>());
        kb.setReification(reification);
        kb.setMaxResults(1000);
        kb.setDefaultLanguage("en");
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

    public KBStatement buildStatement(KBHandle conceptHandle, KBProperty aProperty, String value)
    {
        ValueFactory vf = SimpleValueFactory.getInstance();
        KBStatement statement = new KBStatement(null, conceptHandle, aProperty,
                vf.createLiteral(value));
        return statement;
    }

    public KBQualifier buildQualifier(KBStatement kbStatement, KBProperty propertyHandle,
        String value)
    {
        ValueFactory vf = SimpleValueFactory.getInstance();
        KBQualifier qualifier = new KBQualifier(kbStatement, propertyHandle,
            vf.createLiteral(value));
        return qualifier;
    }

    public static boolean isReachable(String aUrl)
    {
        try {
            URL url = new URL(aUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(2500);
            con.setReadTimeout(2500);
            con.setRequestProperty("Content-Type", "application/sparql-query");
            int status = con.getResponseCode();
            
            if (status == HTTP_MOVED_TEMP || status == HTTP_MOVED_PERM) {
                String location = con.getHeaderField("Location");
                return isReachable(location);
            }
        }
        catch (Exception e) {
            return false;
        }
        
        SPARQLRepository r = new SPARQLRepository(aUrl);
        r.init();
        try (RepositoryConnection conn = r.getConnection()) {
            TupleQuery query = conn.prepareTupleQuery("SELECT ?v WHERE { BIND (true AS ?v)}");
            query.setMaxExecutionTime(5);
            try (TupleQueryResult result = query.evaluate()) {
                return true;
            }
        }
        catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Tries to connect to the given endpoint url and assumes that the connection is successful with
     * {@link org.junit.Assume#assumeTrue(String, boolean)}
     * 
     * @param aEndpointURL
     *            the url to check
     */
    public void assumeEndpointIsAvailable(String aEndpointURL)
    {
        Assume.assumeTrue("Remote repository at [" + aEndpointURL + "] is not reachable",
                isReachable(aEndpointURL));
    }
}
