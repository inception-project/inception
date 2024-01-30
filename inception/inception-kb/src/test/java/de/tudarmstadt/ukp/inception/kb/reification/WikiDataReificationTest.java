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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class WikiDataReificationTest
{
    private KnowledgeBase kb;
    private Repository rdf4jLocalRepo;
    private WikiDataReification sut;

    private static final String TURTLE_PREFIX = String.join("\n", //
            "@prefix wd:  <http://www.wikidata.org/entity/> .", //
            "@prefix wds: <http://www.wikidata.org/entity/statement/> .", //
            "@prefix p:   <http://www.wikidata.org/prop/> .", //
            "@prefix ps:  <http://www.wikidata.org/prop/statement/> .", //
            "@prefix pq:  <http://www.wikidata.org/prop/qualifier/> .");

    private static final String DATA_MONA_LISA = String.join("\n", //
            "wd:Q12418", //
            "    p:P186  wds:statement1 ;", // Mona Lisa: material used : ?statement1
            "    p:P186  wds:statement2 ;", // Mona Lisa: material used : ?statement2
            "    p:P186  wds:statement3 ;", // Mona Lisa: material used : ?statement3
            "    p:P373  wds:statement4 .", // Mona Lisa: commons category: ?statement4
            "", //
            "wds:statement1", //
            "    ps:P186 wd:Q296955 .", // value: oil paint
            "", //
            "wds:statement2", //
            "    ps:P186 wd:Q291034 ;", // value: poplar wood
            "    pq:P518 wd:Q861259 .", // qualifier: applies to part: painting surface
            "", //
            "wds:statement3", //
            "    ps:P186 wd:Q287 ;", // value: wood
            "    pq:P518 wd:Q1737943 ;", // qualifier: applies to part: stretcher bar
            "    pq:P580 1951 .", // qualifier: start time: 1951 (pseudo-syntax)
            "", //
            "wds:statement4", //
            "    ps:P373 'Mona Lisa' ."); // value: 'Mona Lisa'

    @BeforeEach
    public void setUp()
    {
        kb = new KnowledgeBase();
        kb.setDefaultLanguage("en");
        kb.setType(RepositoryType.LOCAL);
        kb.setFullTextSearchIri(null);
        kb.setMaxResults(1000);

        kb.setClassIri("http://www.wikidata.org/entity/Q35120");
        kb.setSubclassIri("http://www.wikidata.org/prop/direct/P279");
        kb.setTypeIri("http://www.wikidata.org/prop/direct/P31");
        kb.setLabelIri("http://www.w3.org/2000/01/rdf-schema#label");
        kb.setPropertyTypeIri("http://www.wikidata.org/entity/Q18616576");
        kb.setDescriptionIri("http://schema.org/description");
        kb.setPropertyLabelIri("http://www.w3.org/2000/01/rdf-schema#label");
        kb.setPropertyDescriptionIri("http://www.w3.org/2000/01/rdf-schema#comment");
        kb.setDeprecationPropertyIri(OWL.DEPRECATED.stringValue());
        kb.setSubPropertyIri("http://www.wikidata.org/prop/direct/P1647");

        // Local in-memory store - this should be used for most tests because we can
        // a) rely on its availability
        // b) import custom test data
        LuceneSail lucenesail = new LuceneSail();
        lucenesail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
        lucenesail.setBaseSail(new MemoryStore());
        rdf4jLocalRepo = new SailRepository(lucenesail);
        rdf4jLocalRepo.init();

        sut = new WikiDataReification();
    }

    @Test
    public void thatAllStatementsCanBeRetrieved() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_MONA_LISA);

        // try (RepositoryConnection conn = rdf4jLocalRepo.getConnection()) {
        // RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, System.out);
        // conn.export(rdfWriter);
        // }

        ValueFactory vf = SimpleValueFactory.getInstance();
        final String STMT1 = "http://www.wikidata.org/entity/statement/statement1";
        final String STMT2 = "http://www.wikidata.org/entity/statement/statement2";
        final String STMT3 = "http://www.wikidata.org/entity/statement/statement3";
        final String STMT4 = "http://www.wikidata.org/entity/statement/statement4";
        final String MONA_LISA = "http://www.wikidata.org/entity/Q12418";
        final String MATERIAL_USED = "http://www.wikidata.org/prop/P186";
        final String COMMONS_CATEGORY = "http://www.wikidata.org/prop/P373";
        final IRI WOOD = vf.createIRI("http://www.wikidata.org/entity/Q287");
        final IRI OIL_PAINT = vf.createIRI("http://www.wikidata.org/entity/Q296955");
        final IRI POPLAR_WOOD = vf.createIRI("http://www.wikidata.org/entity/Q291034");
        final String APPLIES_TO_PART = "http://www.wikidata.org/prop/qualifier/P518";
        final String START_TIME = "http://www.wikidata.org/prop/qualifier/P580";
        final IRI PAINTING_SURFACE = vf.createIRI("http://www.wikidata.org/entity/Q861259");
        final IRI STRETCHER_BAR = vf.createIRI("http://www.wikidata.org/entity/Q1737943");

        List<KBStatement> result;
        try (RepositoryConnection conn = rdf4jLocalRepo.getConnection()) {
            result = sut.listStatements(conn, kb, new KBHandle(MONA_LISA), true);
        }

        KBStatement stmt1 = new KBStatement(STMT1, MONA_LISA, MATERIAL_USED, OIL_PAINT, "Q296955");

        KBStatement stmt2 = new KBStatement(STMT2, MONA_LISA, MATERIAL_USED, POPLAR_WOOD,
                "Q291034");
        stmt2.addQualifier(new KBQualifier(APPLIES_TO_PART, PAINTING_SURFACE));

        KBStatement stmt3 = new KBStatement(STMT3, MONA_LISA, MATERIAL_USED, WOOD, "Q287");
        stmt3.addQualifier(new KBQualifier(APPLIES_TO_PART, STRETCHER_BAR));
        stmt3.addQualifier(new KBQualifier(START_TIME, vf.createLiteral(1951)));

        KBStatement stmt4 = new KBStatement(STMT4, MONA_LISA, COMMONS_CATEGORY,
                vf.createLiteral("Mona Lisa"), null);

        assertThat(result).extracting(stmt -> stmt.getInstance().getIdentifier())
                .allMatch(id -> id.equals(MONA_LISA));
        assertThat(result) //
                .usingElementComparatorIgnoringFields("originalTriples", "qualifiers")
                .containsExactlyInAnyOrder(stmt1, stmt2, stmt3, stmt4);
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
            String prefix = StringUtils.removeEnd(kb.getBasePrefix(), "#");
            conn.add(aIS, prefix, aFormat);
        }
    }
}
