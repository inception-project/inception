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
package de.tudarmstadt.ukp.inception.kb.reification;

import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class NoReificationTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String TURTLE_PREFIX = String.join("\n", //
            "@base <http://example.org/> .", //
            "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .", //
            "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .", //
            "@prefix so: <http://schema.org/> .", //
            "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .");

    private static final String DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE = String.join("\n", //
            "<#green-goblin>", //
            "    rdfs:label 'Green Goblin' ;", //
            "    rdfs:label 'Green Goblin'@en ;", //
            "    rdfs:label 'Grüner Goblin'@de ;", //
            "    rdfs:label 'Goblin vert'@fr ;", //
            "    rdfs:comment 'Little green monster' ;", //
            "    rdfs:comment 'Little green monster'@en ;", //
            "    rdfs:comment 'Kleines grünes Monster'@de .", //
            "", //
            "<#lucky-green>", //
            "    rdfs:label 'Lucky Green' ;", //
            "    rdfs:label 'Lucky Green'@en ;", //
            "    rdfs:comment 'Lucky Irish charm' ;", //
            "    rdfs:comment 'Lucky Irish charm'@en .", //
            "", //
            "<#red-goblin>", //
            "    rdfs:label 'Red Goblin' ;", //
            "    rdfs:comment 'Little red monster' .");

    private KnowledgeBase kb;
    private Repository rdf4jLocalRepo;
    private ReificationStrategy sut;

    @BeforeEach
    public void setUp()
    {
        kb = new KnowledgeBase();
        kb.setDefaultLanguage("en");
        kb.setType(RepositoryType.LOCAL);
        kb.setFullTextSearchIri(null);
        kb.setMaxResults(1000);

        initRdfsMapping();

        // Local in-memory store - this should be used for most tests because we can
        // a) rely on its availability
        // b) import custom test data
        LuceneSail lucenesail = new LuceneSail();
        lucenesail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
        lucenesail.setBaseSail(new MemoryStore());
        rdf4jLocalRepo = new SailRepository(lucenesail);
        rdf4jLocalRepo.init();

        sut = new NoReification();
    }

    @Test
    public void thatItemCanBeObtainedAsStatements() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);

        var result = listStatements(rdf4jLocalRepo,
                new KBHandle("http://example.org/#green-goblin"));

        assertThat(result) //
                .extracting(stmt -> stmt.getInstance().getIdentifier()) //
                .allMatch(id -> id.equals("http://example.org/#green-goblin"));
        assertThat(result).hasSize(7);
    }

    public List<KBStatement> listStatements(Repository aRepo, KBHandle aItem) throws Exception
    {
        try (var conn = aRepo.getConnection()) {
            var startTime = currentTimeMillis();

            var results = sut.listStatements(conn, kb, aItem, true);

            var buf = new StringWriter();
            var out = new PrintWriter(buf);
            out.printf("Results : %d in %dms%n", results.size(), currentTimeMillis() - startTime);
            results.forEach(r -> out.printf("          %s%n", r));
            LOG.info("{}", buf);

            return results;
        }
    }

    private void importDataFromString(RDFFormat aFormat, String... aRdfData) throws IOException
    {
        var data = String.join("\n", aRdfData);

        // Load files into the repository
        try (var is = IOUtils.toInputStream(data, UTF_8)) {
            importData(aFormat, is);
        }
    }

    private void importData(RDFFormat aFormat, InputStream aIS) throws IOException
    {
        try (var conn = rdf4jLocalRepo.getConnection()) {
            // If the RDF file contains relative URLs, then they probably start with a hash.
            // To avoid having two hashes here, we drop the hash from the base prefix configured
            // by the user.
            var prefix = StringUtils.removeEnd(kb.getBasePrefix(), "#");
            conn.add(aIS, prefix, aFormat);
        }
    }

    private void initRdfsMapping()
    {
        kb.setClassIri(RDFS.CLASS.stringValue());
        kb.setSubclassIri(RDFS.SUBCLASSOF.stringValue());
        kb.setTypeIri(RDF.TYPE.stringValue());
        kb.setLabelIri(RDFS.LABEL.stringValue());
        kb.setPropertyTypeIri(RDF.PROPERTY.stringValue());
        kb.setDescriptionIri(RDFS.COMMENT.stringValue());
        // We are intentionally not using RDFS.LABEL here to ensure we can test the label
        // and property label separately
        kb.setPropertyLabelIri(SKOS.PREF_LABEL.stringValue());
        // We are intentionally not using RDFS.COMMENT here to ensure we can test the description
        // and property description separately
        kb.setPropertyDescriptionIri("http://schema.org/description");
        kb.setDeprecationPropertyIri(OWL.DEPRECATED.stringValue());
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF.stringValue());
    }
}
