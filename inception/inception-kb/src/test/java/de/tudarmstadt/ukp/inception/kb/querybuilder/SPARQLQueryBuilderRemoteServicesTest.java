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
package de.tudarmstadt.ukp.inception.kb.querybuilder;

import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_FUSEKI;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_WIKIDATA;
import static de.tudarmstadt.ukp.inception.kb.RepositoryType.REMOTE;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.restoreSslVerification;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.suspendSslVerification;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderAsserts.asHandles;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderAsserts.assertThatChildrenOfExplicitRootCanBeRetrieved;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderLocalTestScenarios.assertIsReachable;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderLocalTestScenarios.buildSparqlRepository;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderLocalTestScenarios.initRdfsMapping;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderLocalTestScenarios.initWikidataMapping;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class SPARQLQueryBuilderRemoteServicesTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private KnowledgeBase kb;
    private Repository zbwStw;
    private Repository zbwGnd;
    private Repository wikidata;
    private Repository dbpedia;
    private Repository yago;

    @BeforeEach
    public void setUp()
    {
        suspendSslVerification();

        kb = new KnowledgeBase();
        kb.setDefaultLanguage("en");
        kb.setType(RepositoryType.LOCAL);
        kb.setFullTextSearchIri(null);
        kb.setMaxResults(100);

        initRdfsMapping(kb);

        wikidata = buildSparqlRepository("https://query.wikidata.org/sparql");
        dbpedia = buildSparqlRepository("https://dbpedia.org/sparql");
        yago = buildSparqlRepository("https://yago-knowledge.org/sparql/query");
        // Web: http://zbw.eu/beta/sparql-lab/?endpoint=http://zbw.eu/beta/sparql/stw/query
        zbwStw = buildSparqlRepository("http://zbw.eu/beta/sparql/stw/query");
        // Web: http://zbw.eu/beta/sparql-lab/?endpoint=http://zbw.eu/beta/sparql/gnd/query
        zbwGnd = buildSparqlRepository("http://zbw.eu/beta/sparql/gnd/query");
    }

    @BeforeEach
    public void testWatcher(TestInfo aTestInfo)
    {
        var methodName = aTestInfo.getTestMethod().map(Method::getName).orElse("<unknown>");
        LOG.info("=== {} === {} =====================", methodName, aTestInfo.getDisplayName());

        suspendSslVerification();
    }

    @AfterEach
    public void tearDown()
    {
        restoreSslVerification();
    }

    @Tag("slow")
    @Test
    void thatPropertyQueryListWorks_Wikidata()
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_WIKIDATA.stringValue());
        initWikidataMapping(kb);

        var results = asHandles(wikidata, SPARQLQueryBuilder.forProperties(kb).limit(10));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).hasSize(10);
    }

    @Tag("slow")
    @Test
    void thatPropertyQueryLabelStartingWith_Wikidata()
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_WIKIDATA.stringValue());
        initWikidataMapping(kb);

        var results = asHandles(wikidata,
                SPARQLQueryBuilder.forProperties(kb).withLabelStartingWith("educated"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.toLowerCase().startsWith("educated"));
    }

    /**
     * This query tries to find all <i>humans in the Star Trek universe</i>
     * ({@code http://www.wikidata.org/entity/Q924827}) who are named <i>Amanda</i>. It tests
     * whether the call to {@link SPARQLQueryBuilder#childrenOf(String)} disables the FTS. If the
     * FTS is not disabled, then no result would be returned because there are so many Amandas in
     * Wikidata that the popular ones returned by the FTS do not include any from the Star Trek
     * universe.
     * 
     * @throws Exception
     *             -
     */
    @Tag("slow")
    @Test
    void thatClassQueryLimitedToChildrenDoesNotReturnOutOfScopeResults_Wikidata() throws Exception
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_WIKIDATA.stringValue());
        initWikidataMapping(kb);

        var results = asHandles(wikidata, SPARQLQueryBuilder //
                .forInstances(kb) //
                .childrenOf("http://www.wikidata.org/entity/Q924827") //
                .withLabelStartingWith("Amanda") //
                .retrieveLabel());

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactlyInAnyOrder("http://www.wikidata.org/entity/Q1412447");
    }

    @Tag("slow")
    @Test
    void testWithLabelContainingAnyOf_Wikidata_FTS() throws Exception
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_WIKIDATA.stringValue());
        initWikidataMapping(kb);

        var results = asHandles(wikidata, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelContainingAnyOf("Tower"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.toLowerCase().contains("tower"));
    }

    @Tag("slow")
    @Test
    void testWithLabelContainingAnyOf_Fuseki_FTS() throws Exception
    {
        assertIsReachable(zbwGnd);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_FUSEKI.stringValue());
        kb.setLabelIri(RDFS.LABEL.stringValue());
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF.stringValue());

        var results = asHandles(zbwGnd, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelContainingAnyOf("Schapiro-Frisch", "Stiker-Métral"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel).allMatch(
                label -> label.contains("Schapiro-Frisch") || label.contains("Stiker-Métral"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelStartingWith_Wikidata_FTS() throws Exception
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_WIKIDATA.stringValue());
        initWikidataMapping(kb);

        var results = asHandles(wikidata, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelStartingWith("Barack"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.toLowerCase().startsWith("barack"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelStartingWith_Fuseki_FTS() throws Exception
    {
        assertIsReachable(zbwGnd);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_FUSEKI.stringValue());
        kb.setLabelIri(RDFS.LABEL.stringValue());
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF.stringValue());

        var results = asHandles(zbwGnd, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelStartingWith("Thom"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.toLowerCase().startsWith("thom"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelMatchingExactlyAnyOf_Fuseki_noFTS_STW() throws Exception
    {
        assertIsReachable(zbwStw);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(null);

        var results = asHandles(zbwStw, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelMatchingExactlyAnyOf("Labour"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> "Labour".equals(label));
    }

    @Tag("slow")
    @Test
    public void testWithLabelMatchingExactlyAnyOf_Fuseki_FTS_GND() throws Exception
    {
        assertIsReachable(zbwGnd);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_FUSEKI.stringValue());
        kb.setLabelIri(RDFS.LABEL.stringValue());
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF.stringValue());

        // The label "Gadebusch, Thomas Henricus" is not assigned directly via rdfs:label but rather
        // via a subproperty of it. Thus, this test also checks if the label sub-property support
        // works.
        //
        // <https://d-nb.info/gnd/100136605> gndo:variantNameForThePerson "Gadebusch, Thomas
        // Henricus";
        // gndo:variantNameEntityForThePerson _:node1fhgbdto1x8884759 .
        var results = asHandles(zbwGnd, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelMatchingExactlyAnyOf("Gadebusch, Thomas Henricus"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> "Gadebusch, Thomas Henricus".equals(label));
    }

    @Tag("slow")
    @Test
    public void testWithLabelMatchingExactlyAnyOf_Wikidata_noFTS() throws Exception
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(null);
        initWikidataMapping(kb);

        var results = asHandles(wikidata, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelMatchingExactlyAnyOf("Labour"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> "Labour".equals(label));
    }

    @Tag("slow")
    @Test
    public void testWithPropertyMatchingAnyOf_Wikidata_noFTS() throws Exception
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(null);
        initWikidataMapping(kb);

        var results = asHandles(wikidata, SPARQLQueryBuilder //
                .forProperties(kb) //
                .withLabelMatchingAnyOf("academic"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.toLowerCase().contains("academic"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelMatchingExactlyAnyOf_Wikidata_FTS() throws Exception
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_WIKIDATA.stringValue());
        initWikidataMapping(kb);

        var results = asHandles(wikidata, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelMatchingExactlyAnyOf("Labour"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.equalsIgnoreCase("Labour"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelMatchingExactlyAnyOf_multiple_Wikidata_FTS() throws Exception
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_WIKIDATA.stringValue());
        initWikidataMapping(kb);

        var results = asHandles(wikidata, SPARQLQueryBuilder //
                .forInstances(kb) //
                .withLabelMatchingExactlyAnyOf("Labour", "Tory"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> "Labour".equals(label) || "Tory".equals(label));
    }

    @Tag("slow")
    @Test
    public void thatChildrenOfExplicitRootCanBeRetrieved_DBPedia()
    {
        assertIsReachable(dbpedia);

        kb.setType(REMOTE);

        assertThatChildrenOfExplicitRootCanBeRetrieved(kb, dbpedia,
                "http://www.w3.org/2002/07/owl#Thing", 0);
    }

    @Disabled("YAGO seems to have problem atm 29-04-2023")
    @Tag("slow")
    @Test
    public void thatChildrenOfExplicitRootCanBeRetrieved_YAGO()
    {
        assertIsReachable(yago);

        kb.setType(REMOTE);

        // YAGO has the habit of timing out on some requests. Unfortunately, there is no clear
        // pattern when this happens - might be due to server load on the YAGO side. Thus, to
        // keep the load lower, we only validate 5 children.
        assertThatChildrenOfExplicitRootCanBeRetrieved(kb, yago, "http://schema.org/Thing", 5);
    }

    @Tag("slow")
    @Test
    public void thatParentsCanBeRetrieved_Wikidata()
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        initWikidataMapping(kb);

        var results = asHandles(wikidata, SPARQLQueryBuilder //
                .forClasses(kb) //
                .ancestorsOf("http://www.wikidata.org/entity/Q5") //
                .retrieveLabel());

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .as("Root concept http://www.wikidata.org/entity/Q35120 should be included") //
                .extracting(KBHandle::getIdentifier) //
                .contains("http://www.wikidata.org/entity/Q35120");
    }

    @Tag("slow")
    @Test
    public void thatRootsCanBeRetrieved_DBPedia()
    {
        assertIsReachable(dbpedia);

        kb.setType(REMOTE);

        var results = asHandles(dbpedia, SPARQLQueryBuilder.forClasses(kb).roots().retrieveLabel());

        assertThat(results).isNotEmpty();

        assertThat(results) //
                .extracting(KBHandle::getUiLabel) //
                .contains("Thing");
    }

    @Tag("slow")
    @Test
    public void thatParentsCanBeRetrieved_DBPedia()
    {
        assertIsReachable(dbpedia);

        kb.setType(REMOTE);

        var results = asHandles(dbpedia, SPARQLQueryBuilder //
                .forClasses(kb) //
                .ancestorsOf("http://dbpedia.org/ontology/Organisation") //
                .retrieveLabel());

        assertThat(results).isNotEmpty();

        assertThat(results) //
                .extracting(KBHandle::getName) //
                .contains("agent", "Thing");
    }
}
