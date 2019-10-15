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
package de.tudarmstadt.ukp.inception.kb.reification;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class NoReificationTest
{
    private static final String TURTLE_PREFIX = String.join("\n",
            "@base <http://example.org/> .",
            "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .",
            "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .",
            "@prefix so: <http://schema.org/> .",
            "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .");
    
    private static final String DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE = String.join("\n",
            "<#green-goblin>",
            "    rdfs:label 'Green Goblin' ;",
            "    rdfs:label 'Green Goblin'@en ;",
            "    rdfs:label 'Grüner Goblin'@de ;",
            "    rdfs:label 'Goblin vert'@fr ;",
            "    rdfs:comment 'Little green monster' ;",
            "    rdfs:comment 'Little green monster'@en ;",
            "    rdfs:comment 'Kleines grünes Monster'@de .",
            "",
            "<#lucky-green>",
            "    rdfs:label 'Lucky Green' ;",
            "    rdfs:label 'Lucky Green'@en ;",
            "    rdfs:comment 'Lucky Irish charm' ;",
            "    rdfs:comment 'Lucky Irish charm'@en .",
            "",
            "<#red-goblin>",
            "    rdfs:label 'Red Goblin' ;",
            "    rdfs:comment 'Little red monster' .");
    
    private KnowledgeBase NRT_kb;
    private Repository rdf4jLocalRepo;
    private ReificationStrategy sut;
    
    @Before
    public void setUp()
    {
	    /**
		 *this is rename method of NamedEntityLinkerTest.java
		 */
        NRT_kb = new KnowledgeBase();
		/**
		 *this is rename method of NamedEntityLinkerTest.java
		 */
        NRT_kb.setDefaultLanguage("en");
		/**
		 *this is rename method of NamedEntityLinkerTest.java
		 */
        NRT_kb.setType(RepositoryType.LOCAL);
		/**
		 *this is rename method of NamedEntityLinkerTest.java
		 */
        NRT_kb.setFullTextSearchIri(null);
		/**
		 *this is rename method of NamedEntityLinkerTest.java
		 */
        NRT_kb.setMaxResults(1000);
        
        initRdfsMapping();
        
        // Local in-memory store - this should be used for most tests because we can
        // a) rely on its availability
        // b) import custom test data
        LuceneSail NRT_lucenesail = new LuceneSail();
        NRT_lucenesail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
        NRT_lucenesail.setBaseSail(new MemoryStore());
        rdf4jLocalRepo = new SailRepository(NRT_lucenesail);
        rdf4jLocalRepo.init();
        
        sut = new NoReification();
    }
    
    @Test
    public void thatItemCanBeObtainedAsStatements() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);
   
        List<KBStatement> result = listStatements(rdf4jLocalRepo,
                new KBHandle("http://example.org/#green-goblin"));
        
        assertThat(result)
                .extracting(stmt -> stmt.getInstance().getIdentifier())
                .allMatch(id -> id.equals("http://example.org/#green-goblin"));
        assertThat(result).hasSize(7);
    }
    
    public List<KBStatement> listStatements(Repository aRepo, KBHandle aItem)
        throws Exception
    {
        try (RepositoryConnection conn = aRepo.getConnection()) {
            long startTime = System.currentTimeMillis();
  
            List<KBStatement> results = sut.listStatements(conn, NRT_kb, aItem, true);
  
            System.out.printf("Results : %d in %dms%n", results.size(),
                    System.currentTimeMillis() - startTime);
            results.forEach(r -> System.out.printf("          %s%n", r));
  
            return results;
        }
    }

    private void importDataFromString(RDFFormat aFormat, String... aRdfData) throws IOException
    {
        String data = String.join("\n", aRdfData);
        
        // Load files into the repository
        try (InputStream is = IOUtils.toInputStream(data, UTF_8)) {
            importData(aFormat, is);
        }
    }
    
    private void importData(RDFFormat aFormat, InputStream aIS) throws IOException
    {
        try (RepositoryConnection conn = rdf4jLocalRepo.getConnection()) {
            // If the RDF file contains relative URLs, then they probably start with a hash.
            // To avoid having two hashes here, we drop the hash from the base prefix configured
            // by the user.
            String prefix = StringUtils.removeEnd(NRT_kb.getBasePrefix(), "#");
            conn.add(aIS, prefix, aFormat);
        }
    }
    
    private void initRdfsMapping()
    {
        ValueFactory vf = SimpleValueFactory.getInstance();
        //this is rename varies of NoReificationTest.java
        NRT_kb.setClassIri(RDFS.CLASS);
        NRT_kb.setSubclassIri(RDFS.SUBCLASSOF);
        NRT_kb.setTypeIri(RDF.TYPE);
        NRT_kb.setLabelIri(RDFS.LABEL);
        NRT_kb.setPropertyTypeIri(RDF.PROPERTY);
        NRT_kb.setDescriptionIri(RDFS.COMMENT);
        // We are intentionally not using RDFS.LABEL here to ensure we can test the label
        // and property label separately
        NRT_kb.setPropertyLabelIri(SKOS.PREF_LABEL);        
        // We are intentionally not using RDFS.COMMENT here to ensure we can test the description
        // and property description separately
        NRT_kb.setPropertyDescriptionIri(vf.createIRI("http://schema.org/description"));
        NRT_kb.setSubPropertyIri(RDFS.SUBPROPERTYOF);
    }
}
