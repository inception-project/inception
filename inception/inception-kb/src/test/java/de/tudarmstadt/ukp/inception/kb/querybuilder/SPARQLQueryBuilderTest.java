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
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_VIRTUOSO;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_WIKIDATA;
import static de.tudarmstadt.ukp.inception.kb.RepositoryType.REMOTE;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.newPerThreadSslCheckingHttpClientBuilder;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.restoreSslVerification;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.suspendSslVerification;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.sanitizeQueryString_FTS;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderAsserts.asHandles;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderAsserts.assertThatChildrenOfExplicitRootCanBeRetrieved;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderAsserts.exists;
import static de.tudarmstadt.ukp.inception.kb.util.TestFixtures.isReachable;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.eclipse.rdf4j.rio.RDFFormat.RDFXML;
import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableBiConsumer;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class SPARQLQueryBuilderTest
{
    static final String TURTLE_PREFIX = String.join("\n", //
            "@base <http://example.org/> .", //
            "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .", //
            "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .", //
            "@prefix so: <http://schema.org/> .", //
            "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .");

    static final String DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE = contentOf(
            new File("src/test/resources/turtle/data_labels_and_descriptions_with_language.ttl"),
            UTF_8);

    static final String DATA_LABELS_WITHOUT_LANGUAGE = String.join("\n", //
            "<#green-goblin>", //
            "    rdfs:label 'Green Goblin' .", //
            "", //
            "<#lucky-green>", //
            "    rdfs:label 'Lucky Green' .", //
            "", //
            "<#red-goblin>", //
            "    rdfs:label 'Red Goblin' .");

    static final String DATA_MULTIPLE_LABELS = String.join("\n", //
            "<#example>", //
            "    rdfs:label 'specimen' ;", //
            "    rdfs:label 'sample' ;", //
            "    rdfs:label 'instance' ;", //
            "    rdfs:label 'case'  .");

    static final String DATA_ADDITIONAL_SEARCH_PROPERTIES = contentOf(
            new File("src/test/resources/turtle/data_additional_search_properties.ttl"), UTF_8);

    static final String LABEL_SUBPROPERTY = String.join("\n", //
            "<#sublabel>", //
            "    rdfs:subPropertyOf rdfs:label .", //
            "", //
            "<#green-goblin>", //
            "    <#sublabel> 'Green Goblin' .");

    /**
     * This dataset contains a hierarchy of classes and instances with a naming scheme. There is an
     * implicit and an explicit root class. All classes have "class" in their name. Subclasses start
     * with "subclass" and then a number. Instances start with the number of the class to which they
     * belong followed by a number.
     */
    static final String DATA_CLASS_RDFS_HIERARCHY = String.join("\n", //
            "<#explicitRoot>", //
            "    rdf:type rdfs:Class .", //
            "<#subclass1>", //
            "    rdf:type rdfs:Class ;", //
            "    rdfs:subClassOf <#explicitRoot> .", //
            "<#subclass1-1>", //
            "    rdfs:subClassOf <#subclass1> .", //
            "<#subclass1-1-1>", //
            "    rdfs:subClassOf <#subclass1-1> .", //
            "<#subclass2>", //
            "    rdfs:subClassOf <#explicitRoot> .", //
            "<#subclass3>", //
            "    rdfs:subClassOf <#implicitRoot> .", //
            "<#0-instance-1>", //
            "    rdf:type <#root> .", //
            "<#1-instance-1>", //
            "    rdf:type <#subclass1> .", //
            "<#2-instance-2>", //
            "    rdf:type <#subclass2> .", //
            "<#3-instance-3>", //
            "    rdf:type <#subclass3> .", //
            "<#1-1-1-instance-4>", //
            "    rdf:type <#subclass1-1-1> .");

    /**
     * This dataset contains properties, some in a hierarchical relationship. There is again a
     * naming scheme: all properties have "property" in their name. Subproperties start with
     * "subproperty" and then a number. The dataset also contains some non-properties to be able to
     * ensure that queries limited to properties do not return non-properties.
     */
    static final String DATA_PROPERTIES = String.join("\n", //
            "<#explicitRoot>", //
            "    rdf:type rdfs:Class .", //
            "<#property-1>", //
            "    rdf:type rdf:Property ;", //
            "    skos:prefLabel 'Property 1' ;", //
            "    so:description 'Property One' ;", //
            "    rdfs:domain <#explicitRoot> ;", //
            "    rdfs:range xsd:string .", //
            "<#property-2>", //
            "    rdf:type rdf:Property ;", //
            "    skos:prefLabel 'Property 2' ;", //
            "    so:description 'Property Two' ;", //
            "    rdfs:domain <#subclass1> ;", //
            "    rdfs:range xsd:Integer .", //
            "<#property-3>", //
            "    rdf:type rdf:Property ;", //
            "    skos:prefLabel 'Property 3' ;", //
            "    so:description 'Property Three' .", //
            "<#subproperty-1-1>", //
            "    rdfs:subPropertyOf <#property-1> ;", //
            "    skos:prefLabel 'Subproperty 1-1' ;", //
            "    so:description 'Property One-One' .", //
            "<#subproperty-1-1-1>", //
            "    rdfs:subPropertyOf <#subproperty-1-1> ;", //
            "    skos:prefLabel 'Subproperty 1-1-1' ;", //
            "    so:description 'Property One-One-One' .", //
            "<#subclass1>", //
            "    rdf:type rdfs:Class ;", //
            "    rdfs:subClassOf <#explicitRoot> ;", //
            "    <#implicit-property-1> 'value1' .");

    private KnowledgeBase kb;
    private Repository ukpVirtuosoRepo;
    private Repository zbwStw;
    private Repository zbwGnd;
    private Repository wikidata;
    private Repository dbpedia;
    private Repository yago;
    private Repository hucit;

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

        ukpVirtuosoRepo = buildSparqlRepository(
                "http://knowledgebase.ukp.informatik.tu-darmstadt.de:8890/sparql");
        wikidata = buildSparqlRepository("https://query.wikidata.org/sparql");
        dbpedia = buildSparqlRepository("http://de.dbpedia.org/sparql");
        yago = buildSparqlRepository("https://yago-knowledge.org/sparql/query");
        hucit = buildSparqlRepository("http://nlp.dainst.org:8888/sparql");
        // Web: http://zbw.eu/beta/sparql-lab/?endpoint=http://zbw.eu/beta/sparql/stw/query
        zbwStw = buildSparqlRepository("http://zbw.eu/beta/sparql/stw/query");
        // Web: http://zbw.eu/beta/sparql-lab/?endpoint=http://zbw.eu/beta/sparql/gnd/query
        zbwGnd = buildSparqlRepository("http://zbw.eu/beta/sparql/gnd/query");
    }

    @BeforeEach
    public void testWatcher(TestInfo aTestInfo)
    {
        String methodName = aTestInfo.getTestMethod().map(Method::getName).orElse("<unknown>");
        System.out.printf("\n=== %s === %s =====================\n", methodName,
                aTestInfo.getDisplayName());

        suspendSslVerification();
    }

    @AfterEach
    public void tearDown()
    {
        restoreSslVerification();
    }

    static List<Scenario> tests() throws Exception
    {
        return asList( //
                new Scenario("testWithLabelStartingWith_withLanguage_FTS_1",
                        SPARQLQueryBuilderTest::testWithLabelStartingWith_withLanguage_FTS_1),
                new Scenario("testWithLabelStartingWith_withLanguage_FTS_2",
                        SPARQLQueryBuilderTest::testWithLabelStartingWith_withLanguage_FTS_2),
                new Scenario("testWithLabelStartingWith_withLanguage_FTS_3",
                        SPARQLQueryBuilderTest::testWithLabelStartingWith_withLanguage_FTS_3),
                new Scenario("testWithLabelStartingWith_withLanguage_FTS_4",
                        SPARQLQueryBuilderTest::testWithLabelStartingWith_withLanguage_FTS_4),
                new Scenario("testWithLabelStartingWith_withLanguage_noFTS",
                        SPARQLQueryBuilderTest::testWithLabelStartingWith_withLanguage_noFTS),
                new Scenario("testWithLabelContainingAnyOf_pets_ttl_noFTS",
                        SPARQLQueryBuilderTest::testWithLabelContainingAnyOf_pets_ttl_noFTS),
                new Scenario("thatRootsCanBeRetrieved_ontolex",
                        SPARQLQueryBuilderTest::thatRootsCanBeRetrieved_ontolex),
                new Scenario("testWithLabelContainingAnyOf_withLanguage_noFTS",
                        SPARQLQueryBuilderTest::testWithLabelContainingAnyOf_withLanguage_noFTS),
                new Scenario("testWithLabelContainingAnyOf_withLanguage",
                        SPARQLQueryBuilderTest::testWithLabelContainingAnyOf_withLanguage),
                new Scenario("testWithLabelMatchingAnyOf_withLanguage",
                        SPARQLQueryBuilderTest::testWithLabelMatchingAnyOf_withLanguage),
                new Scenario("testWithLabelMatchingAnyOf_withLanguage_noFTS",
                        SPARQLQueryBuilderTest::testWithLabelMatchingAnyOf_withLanguage_noFTS),
                new Scenario("testWithLabelStartingWith_withoutLanguage",
                        SPARQLQueryBuilderTest::testWithLabelStartingWith_withoutLanguage),
                new Scenario("testWithLabelStartingWith_withoutLanguage_noFTS",
                        SPARQLQueryBuilderTest::testWithLabelStartingWith_withoutLanguage_noFTS),
                new Scenario("testWithLabelMatchingExactlyAnyOf_subproperty",
                        SPARQLQueryBuilderTest::testWithLabelMatchingExactlyAnyOf_subproperty),
                new Scenario("testWithLabelMatchingExactlyAnyOf_subproperty_noFTS",
                        SPARQLQueryBuilderTest::testWithLabelMatchingExactlyAnyOf_subproperty_noFTS),
                new Scenario("testWithLabelMatchingExactlyAnyOf_withLanguage_noFTS",
                        SPARQLQueryBuilderTest::testWithLabelMatchingExactlyAnyOf_withLanguage_noFTS),
                new Scenario("testWithLabelMatchingExactlyAnyOf_withLanguage",
                        SPARQLQueryBuilderTest::testWithLabelMatchingExactlyAnyOf_withLanguage),
                new Scenario("thatExistsReturnsTrueWhenDataQueriedForExists",
                        SPARQLQueryBuilderTest::thatExistsReturnsTrueWhenDataQueriedForExists),
                new Scenario("thatOnlyLabelsAndDescriptionsWithNoLanguageAreRetrieved",
                        SPARQLQueryBuilderTest::thatOnlyLabelsAndDescriptionsWithNoLanguageAreRetrieved),
                new Scenario("thatLabelsAndDescriptionsWithLanguageArePreferred",
                        SPARQLQueryBuilderTest::thatLabelsAndDescriptionsWithLanguageArePreferred),
                new Scenario("thatSearchOverMultipleLabelsWorks",
                        SPARQLQueryBuilderTest::thatSearchOverMultipleLabelsWorks),
                new Scenario("thatMatchingAgainstAdditionalSearchPropertiesWorks",
                        SPARQLQueryBuilderTest::thatMatchingAgainstAdditionalSearchPropertiesWorks),
                new Scenario("thatExistsReturnsFalseWhenDataQueriedForDoesNotExist",
                        SPARQLQueryBuilderTest::thatExistsReturnsFalseWhenDataQueriedForDoesNotExist),
                new Scenario("thatExplicitClassCanBeRetrievedByItsIdentifier",
                        SPARQLQueryBuilderTest::thatExplicitClassCanBeRetrievedByItsIdentifier),
                new Scenario("thatImplicitClassCanBeRetrievedByItsIdentifier",
                        SPARQLQueryBuilderTest::thatImplicitClassCanBeRetrievedByItsIdentifier),
                new Scenario("thatNonClassCannotBeRetrievedByItsIdentifier",
                        SPARQLQueryBuilderTest::thatNonClassCannotBeRetrievedByItsIdentifier),
                new Scenario("thatCanRetrieveItemInfoForIdentifier",
                        SPARQLQueryBuilderTest::thatCanRetrieveItemInfoForIdentifier),
                new Scenario("thatAllPropertiesCanBeRetrieved",
                        SPARQLQueryBuilderTest::thatAllPropertiesCanBeRetrieved),
                new Scenario("thatPropertyQueryLimitedToDescendantsDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatPropertyQueryLimitedToDescendantsDoesNotReturnOutOfScopeResults),
                new Scenario("thatPropertyQueryLimitedToChildrenDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatPropertyQueryLimitedToChildrenDoesNotReturnOutOfScopeResults),
                new Scenario("thatPropertyQueryLimitedToDomainDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatPropertyQueryLimitedToDomainDoesNotReturnOutOfScopeResults),
                new Scenario("thatQueryLimitedToRootClassesDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatQueryLimitedToRootClassesDoesNotReturnOutOfScopeResults),
                new Scenario("thatQueryWithExplicitRootClassDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatQueryWithExplicitRootClassDoesNotReturnOutOfScopeResults),
                new Scenario("thatNonRootClassCanBeUsedAsExplicitRootClass",
                        SPARQLQueryBuilderTest::thatNonRootClassCanBeUsedAsExplicitRootClass),
                new Scenario("thatQueryLimitedToClassesDoesNotReturnInstances",
                        SPARQLQueryBuilderTest::thatQueryLimitedToClassesDoesNotReturnInstances),
                new Scenario("thatQueryLimitedToInstancesDoesNotReturnClasses",
                        SPARQLQueryBuilderTest::thatQueryLimitedToInstancesDoesNotReturnClasses),
                new Scenario("thatClassQueryLimitedToAnchestorsDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatClassQueryLimitedToAnchestorsDoesNotReturnOutOfScopeResults),
                new Scenario("thatClassQueryLimitedToParentsDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatClassQueryLimitedToParentsDoesNotReturnOutOfScopeResults),
                new Scenario("thatClassQueryLimitedToChildrenDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatClassQueryLimitedToChildrenDoesNotReturnOutOfScopeResults),
                new Scenario("thatClassQueryLimitedToDescendantsDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatClassQueryLimitedToDescendantsDoesNotReturnOutOfScopeResults),
                new Scenario("thatInstanceQueryLimitedToParentsDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatInstanceQueryLimitedToParentsDoesNotReturnOutOfScopeResults),
                new Scenario("thatInstanceQueryLimitedToAnchestorsDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatInstanceQueryLimitedToAnchestorsDoesNotReturnOutOfScopeResults),
                new Scenario("thatInstanceQueryLimitedToChildrenDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatInstanceQueryLimitedToChildrenDoesNotReturnOutOfScopeResults),
                new Scenario("thatInstanceQueryLimitedToDescendantsDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatInstanceQueryLimitedToDescendantsDoesNotReturnOutOfScopeResults),
                new Scenario("thatItemQueryLimitedToChildrenDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatItemQueryLimitedToChildrenDoesNotReturnOutOfScopeResults),
                new Scenario("thatItemQueryLimitedToDescendantsDoesNotReturnOutOfScopeResults",
                        SPARQLQueryBuilderTest::thatItemQueryLimitedToDescendantsDoesNotReturnOutOfScopeResults),
                new Scenario("testWithLabelStartingWith_OLIA",
                        SPARQLQueryBuilderTest::testWithLabelStartingWith_OLIA)

        );
    }

    static class Scenario
    {
        final String name;
        final FailableBiConsumer<Repository, KnowledgeBase, Exception> implementation;

        public Scenario(String aName,
                FailableBiConsumer<Repository, KnowledgeBase, Exception> aImplementation)
        {
            name = aName;
            implementation = aImplementation;
        }
    }

    static Repository buildSparqlRepository(String aUrl)
    {
        SPARQLRepository repo = new SPARQLRepository(aUrl);
        repo.setHttpClient(newPerThreadSslCheckingHttpClientBuilder().build());
        repo.init();
        return repo;
    }

    static Repository buildSparqlRepository(String aQueryUrl, String aUpdateUrl)
    {
        SPARQLRepository repo = new SPARQLRepository(aQueryUrl, aUpdateUrl);
        repo.setHttpClient(newPerThreadSslCheckingHttpClientBuilder().build());
        repo.init();
        return repo;
    }

    /**
     * Checks that {@code SPARQLQueryBuilder#exists(RepositoryConnection, boolean)} can return
     * {@code true} by querying for a list of all classes in {@link #DATA_CLASS_RDFS_HIERARCHY}
     * which contains a number of classes.
     * 
     * @throws Exception
     *             -
     */
    static void thatExistsReturnsTrueWhenDataQueriedForExists(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        boolean result = exists(aRepository, SPARQLQueryBuilder.forClasses(aKB));

        assertThat(result).isTrue();
    }

    /**
     * If the KB has no default language set, then only labels and descriptions with no language at
     * all should be returned.
     * 
     * @throws Exception
     *             -
     */
    static void thatOnlyLabelsAndDescriptionsWithNoLanguageAreRetrieved(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        aKB.setDefaultLanguage(null);

        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withIdentifier("http://example.org/#green-goblin") //
                .retrieveLabel() //
                .retrieveDescription());

        assertThat(results).isNotEmpty();
        assertThat(results)
                .usingRecursiveFieldByFieldElementComparatorOnFields("identifier", "name",
                        "description", "language")
                .containsExactlyInAnyOrder(new KBHandle("http://example.org/#green-goblin",
                        "Green Goblin", "Little green monster"));
    }

    /**
     * If the KB has a default language set, then labels/descriptions in that language should be
     * preferred it is permitted to fall back to labels/descriptions without any language. The
     * dataset contains only labels for French but no descriptions, so it should fall back to
     * returning the description without any language.
     * 
     * @throws Exception
     *             -
     */
    static void thatLabelsAndDescriptionsWithLanguageArePreferred(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        // The dataset contains only labels for French but no descriptions
        aKB.setDefaultLanguage("fr");

        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withIdentifier("http://example.org/#green-goblin") //
                .retrieveLabel() //
                .retrieveDescription());

        assertThat(results).isNotEmpty();
        assertThat(results)
                .usingRecursiveFieldByFieldElementComparatorOnFields("identifier", "name",
                        "description", "language")
                .containsExactlyInAnyOrder(new KBHandle("http://example.org/#green-goblin",
                        "Goblin vert", "Little green monster", "fr"));
    }

    static void thatSearchOverMultipleLabelsWorks(Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_MULTIPLE_LABELS);

        for (String term : asList("specimen", "sample", "instance", "case")) {
            List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                    .forItems(aKB) //
                    .withLabelMatchingAnyOf(term) //
                    .retrieveLabel());

            assertThat(results)
                    .usingRecursiveFieldByFieldElementComparatorOnFields("identifier", "name")
                    .containsExactlyInAnyOrder(new KBHandle("http://example.org/#example", term));
        }
    }

    static void thatMatchingAgainstAdditionalSearchPropertiesWorks(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        aKB.setLabelIri("http://www.w3.org/2000/01/rdf-schema#prefLabel");
        aKB.setAdditionalMatchingProperties(asList("http://www.w3.org/2000/01/rdf-schema#label"));

        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX,
                DATA_ADDITIONAL_SEARCH_PROPERTIES);

        for (String term : asList("specimen", "sample", "instance", "case")) {
            List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                    .forItems(aKB) //
                    .withLabelMatchingAnyOf(term) //
                    .retrieveLabel());

            assertThat(results)
                    .usingRecursiveFieldByFieldElementComparatorOnFields("identifier", "name")
                    .containsExactlyInAnyOrder(
                            new KBHandle("http://example.org/#example", "specimen"));
        }
    }

    /**
     * Checks that {@code SPARQLQueryBuilder#exists(RepositoryConnection, boolean)} can return
     * {@code false} by querying for the parent of a root class in
     * {@link #DATA_CLASS_RDFS_HIERARCHY} which does not exist.
     * 
     * @throws Exception
     *             -
     */
    static void thatExistsReturnsFalseWhenDataQueriedForDoesNotExist(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        boolean result = exists(aRepository,
                SPARQLQueryBuilder.forClasses(aKB).parentsOf("http://example.org/#explicitRoot"));

        assertThat(result).isFalse();
    }

    /**
     * Checks that an explicitly defined class can be retrieved using its identifier.
     * 
     * @throws Exception
     *             -
     */
    static void thatExplicitClassCanBeRetrievedByItsIdentifier(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        boolean result = exists(aRepository, SPARQLQueryBuilder.forClasses(aKB)
                .withIdentifier("http://example.org/#explicitRoot"));

        assertThat(result).isTrue();
    }

    /**
     * Checks that an implicitly defined class can be retrieved using its identifier.
     * 
     * @throws Exception
     *             -
     */
    static void thatImplicitClassCanBeRetrievedByItsIdentifier(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        boolean result = exists(aRepository, SPARQLQueryBuilder.forClasses(aKB)
                .withIdentifier("http://example.org/#implicitRoot"));

        assertThat(result).isTrue();
    }

    /**
     * Checks that a either explicitly nor implicitly defined class can be retrieved using its
     * identifier.
     * 
     * @throws Exception
     *             -
     */
    static void thatNonClassCannotBeRetrievedByItsIdentifier(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        boolean result = exists(aRepository, SPARQLQueryBuilder.forClasses(aKB)
                .withIdentifier("http://example.org/#DoesNotExist"));

        assertThat(result).isFalse();
    }

    /**
     * Checks that item information can be obtained for a given subject.
     * 
     * @throws Exception
     *             -
     */
    static void thatCanRetrieveItemInfoForIdentifier(Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withIdentifier("http://example.org/#red-goblin") //
                .retrieveLabel() //
                .retrieveDescription());

        assertThat(results).isNotEmpty();
        assertThat(results)
                .usingRecursiveFieldByFieldElementComparatorOnFields("identifier", "name",
                        "description", "language")
                .containsExactlyInAnyOrder(new KBHandle("http://example.org/#red-goblin",
                        "Red Goblin", "Little red monster"));
    }

    @SuppressWarnings("deprecation")
    static void thatAllPropertiesCanBeRetrieved(Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_PROPERTIES);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forProperties(aKB) //
                .retrieveLabel() //
                .retrieveDescription() //
                .retrieveDomainAndRange());

        assertThat(results).isNotEmpty();
        assertThat(results)
                .usingRecursiveFieldByFieldElementComparatorOnFields("identifier", "name",
                        "description", "range", "domain")
                .containsExactlyInAnyOrder(
                        new KBHandle("http://example.org/#property-1", "Property 1", "Property One",
                                null, "http://example.org/#explicitRoot",
                                "http://www.w3.org/2001/XMLSchema#string"),
                        new KBHandle("http://example.org/#property-2", "Property 2", "Property Two",
                                null, "http://example.org/#subclass1",
                                "http://www.w3.org/2001/XMLSchema#Integer"),
                        new KBHandle("http://example.org/#property-3", "Property 3",
                                "Property Three"),
                        new KBHandle("http://example.org/#subproperty-1-1", "Subproperty 1-1",
                                "Property One-One"),
                        new KBHandle("http://example.org/#subproperty-1-1-1", "Subproperty 1-1-1",
                                "Property One-One-One"));
    }

    static void thatPropertyQueryLimitedToDescendantsDoesNotReturnOutOfScopeResults(
            Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_PROPERTIES);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forProperties(aKB) //
                .descendantsOf("http://example.org/#property-1"));

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactlyInAnyOrder("http://example.org/#subproperty-1-1",
                        "http://example.org/#subproperty-1-1-1");
    }

    @Tag("slow")
    @Test
    public void thatPropertyQueryListWorks_Wikidata()
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_WIKIDATA.stringValue());
        initWikidataMapping();

        List<KBHandle> results = asHandles(wikidata,
                SPARQLQueryBuilder.forProperties(kb).limit(10));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).hasSize(10);
    }

    @Tag("slow")
    @Test
    public void thatPropertyQueryLabelStartingWith_Wikidata()
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_WIKIDATA.stringValue());
        initWikidataMapping();

        List<KBHandle> results = asHandles(wikidata,
                SPARQLQueryBuilder.forProperties(kb).withLabelStartingWith("educated"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.toLowerCase().startsWith("educated"));
    }

    static void thatPropertyQueryLimitedToChildrenDoesNotReturnOutOfScopeResults(
            Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_PROPERTIES);

        List<KBHandle> results = asHandles(aRepository,
                SPARQLQueryBuilder.forProperties(aKB).childrenOf("http://example.org/#property-1"));

        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getIdentifier)
                .containsExactlyInAnyOrder("http://example.org/#subproperty-1-1");
    }

    static void thatPropertyQueryLimitedToDomainDoesNotReturnOutOfScopeResults(
            Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_PROPERTIES);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder.forProperties(aKB)
                .matchingDomain("http://example.org/#subclass1"));

        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getIdentifier).containsExactlyInAnyOrder(
                // property-1 is inherited by #subclass1 from #explicitRoot
                "http://example.org/#property-1",
                // property-2 is declared on #subclass1
                "http://example.org/#property-2",
                // property-3 defines no domain
                "http://example.org/#property-3");
        // other properties all either define or inherit an incompatible domain
    }

    static void thatQueryLimitedToRootClassesDoesNotReturnOutOfScopeResults(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forClasses(aKB) //
                .roots());

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getUiLabel) //
                .containsExactlyInAnyOrder("explicitRoot", "implicitRoot");
    }

    static void thatQueryWithExplicitRootClassDoesNotReturnOutOfScopeResults(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        aKB.setRootConcepts(asList("http://example.org/#implicitRoot"));

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder.forClasses(aKB).roots());

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getUiLabel) //
                .containsExactlyInAnyOrder("implicitRoot");
    }

    static void thatNonRootClassCanBeUsedAsExplicitRootClass(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        aKB.setRootConcepts(
                asList("http://example.org/#implicitRoot", "http://example.org/#subclass2"));

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder.forClasses(aKB).roots());

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getUiLabel) //
                .containsExactlyInAnyOrder("implicitRoot", "subclass2");
    }

    static void thatQueryLimitedToClassesDoesNotReturnInstances(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder.forClasses(aKB));

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getUiLabel) //
                .noneMatch(label -> label.contains("instance"));
    }

    static void thatQueryLimitedToInstancesDoesNotReturnClasses(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder.forInstances(aKB));

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getUiLabel) //
                .noneMatch(label -> label.contains("class"));
    }

    static void thatClassQueryLimitedToAnchestorsDoesNotReturnOutOfScopeResults(
            Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forClasses(aKB) //
                .ancestorsOf("http://example.org/#subclass1-1-1"));

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactlyInAnyOrder("http://example.org/#explicitRoot",
                        "http://example.org/#subclass1", "http://example.org/#subclass1-1");
    }

    static void thatClassQueryLimitedToParentsDoesNotReturnOutOfScopeResults(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forClasses(aKB) //
                .parentsOf("http://example.org/#subclass1-1"));

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactlyInAnyOrder("http://example.org/#subclass1");
    }

    static void thatClassQueryLimitedToChildrenDoesNotReturnOutOfScopeResults(
            Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forClasses(aKB) //
                .childrenOf("http://example.org/#subclass1"));

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactlyInAnyOrder("http://example.org/#subclass1-1");
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
    public void thatClassQueryLimitedToChildrenDoesNotReturnOutOfScopeResults_Wikidata()
        throws Exception
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_WIKIDATA.stringValue());
        initWikidataMapping();

        List<KBHandle> results = asHandles(wikidata, SPARQLQueryBuilder //
                .forInstances(kb) //
                .childrenOf("http://www.wikidata.org/entity/Q924827") //
                .withLabelStartingWith("Amanda") //
                .retrieveLabel());

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactlyInAnyOrder("http://www.wikidata.org/entity/Q1412447");
    }

    static void thatClassQueryLimitedToDescendantsDoesNotReturnOutOfScopeResults(
            Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forClasses(aKB) //
                .descendantsOf("http://example.org/#subclass1"));

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactlyInAnyOrder("http://example.org/#subclass1-1",
                        "http://example.org/#subclass1-1-1");
    }

    static void thatInstanceQueryLimitedToParentsDoesNotReturnOutOfScopeResults(
            Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forClasses(aKB) //
                .parentsOf("http://example.org/#1-1-1-instance-4"));

        assertThat(results).isNotEmpty(); //
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactlyInAnyOrder("http://example.org/#subclass1-1-1");
    }

    static void thatInstanceQueryLimitedToAnchestorsDoesNotReturnOutOfScopeResults(
            Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forClasses(aKB) //
                .ancestorsOf("http://example.org/#1-1-1-instance-4"));

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactlyInAnyOrder("http://example.org/#explicitRoot",
                        "http://example.org/#subclass1", "http://example.org/#subclass1-1",
                        "http://example.org/#subclass1-1-1");
    }

    static void thatInstanceQueryLimitedToChildrenDoesNotReturnOutOfScopeResults(
            Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forInstances(aKB) //
                .childrenOf("http://example.org/#subclass1"));

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactlyInAnyOrder("http://example.org/#1-instance-1");
    }

    static void thatInstanceQueryLimitedToDescendantsDoesNotReturnOutOfScopeResults(
            Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forInstances(aKB) //
                .descendantsOf("http://example.org/#subclass1"));

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .allMatch(label -> label.matches("http://example.org/#1(-1)*-instance-.*"));
    }

    static void thatItemQueryLimitedToChildrenDoesNotReturnOutOfScopeResults(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .childrenOf("http://example.org/#subclass1"));

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactlyInAnyOrder( //
                        "http://example.org/#1-instance-1", "http://example.org/#subclass1-1");
    }

    static void thatItemQueryLimitedToDescendantsDoesNotReturnOutOfScopeResults(
            Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder.forItems(aKB) //
                .descendantsOf("http://example.org/#subclass1"));

        assertThat(results).isNotEmpty();
        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .allMatch(label -> label.matches("http://example.org/#1(-1)*-instance-.*")
                        || label.startsWith("http://example.org/#subclass1-"));
    }

    static void testWithLabelMatchingAnyOf_withLanguage_noFTS(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        aKB.setFullTextSearchIri(null);

        testWithLabelMatchingAnyOf_withLanguage(aRepository, aKB);
    }

    static void testWithLabelMatchingAnyOf_withLanguage(Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withLabelMatchingAnyOf("Gobli"));

        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.contains("Goblin"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).usingRecursiveFieldByFieldElementComparatorOnFields("identifier",
                "name", "language").containsExactlyInAnyOrder(
                        new KBHandle("http://example.org/#red-goblin", "Red Goblin"), new KBHandle(
                                "http://example.org/#green-goblin", "Green Goblin", null, "en"));
    }

    static void testWithLabelContainingAnyOf_withLanguage_noFTS(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        aKB.setFullTextSearchIri(null);

        testWithLabelContainingAnyOf_withLanguage(aRepository, aKB);
    }

    static void testWithLabelContainingAnyOf_withLanguage(Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withLabelContainingAnyOf("Goblin"));

        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.contains("Goblin"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).usingRecursiveFieldByFieldElementComparatorOnFields("identifier",
                "name", "language").containsExactlyInAnyOrder(
                        new KBHandle("http://example.org/#red-goblin", "Red Goblin"), new KBHandle(
                                "http://example.org/#green-goblin", "Green Goblin", null, "en"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelContainingAnyOf_Virtuoso_withLanguage_FTS() throws Exception
    {
        assertIsReachable(ukpVirtuosoRepo);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_VIRTUOSO.stringValue());

        List<KBHandle> results = asHandles(ukpVirtuosoRepo, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelContainingAnyOf("Tower"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.toLowerCase().contains("tower"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelContainingAnyOf_Wikidata_FTS() throws Exception
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_WIKIDATA.stringValue());
        initWikidataMapping();

        List<KBHandle> results = asHandles(wikidata, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelContainingAnyOf("Tower"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.toLowerCase().contains("tower"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelContainingAnyOf_Fuseki_FTS() throws Exception
    {
        assertIsReachable(zbwGnd);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_FUSEKI.stringValue());
        kb.setLabelIri(RDFS.LABEL.stringValue());
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF.stringValue());

        List<KBHandle> results = asHandles(zbwGnd, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelContainingAnyOf("Schapiro-Frisch", "Stiker-Métral"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel).allMatch(
                label -> label.contains("Schapiro-Frisch") || label.contains("Stiker-Métral"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelContainingAnyOf_classes_HUCIT_FTS() throws Exception
    {
        assertIsReachable(hucit);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_VIRTUOSO.stringValue());

        List<KBHandle> results = asHandles(hucit, SPARQLQueryBuilder //
                .forClasses(kb) //
                .withLabelContainingAnyOf("work"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.toLowerCase().contains("work"));
    }

    static void testWithLabelMatchingExactlyAnyOf_withLanguage_noFTS(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        aKB.setFullTextSearchIri(null);

        testWithLabelMatchingExactlyAnyOf_withLanguage(aRepository, aKB);
    }

    static void testWithLabelMatchingExactlyAnyOf_withLanguage(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withLabelMatchingExactlyAnyOf("Green Goblin"));

        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.equals("Green Goblin"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingRecursiveFieldByFieldElementComparatorOnFields("identifier", "name",
                        "language")
                .containsExactlyInAnyOrder(new KBHandle("http://example.org/#green-goblin",
                        "Green Goblin", null, "en"));
    }

    static void testWithLabelMatchingExactlyAnyOf_subproperty_noFTS(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        aKB.setFullTextSearchIri(null);

        testWithLabelMatchingExactlyAnyOf_subproperty(aRepository, aKB);
    }

    static void testWithLabelMatchingExactlyAnyOf_subproperty(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        aKB.setSubPropertyIri(RDFS.SUBPROPERTYOF.stringValue());
        aKB.setLabelIri(RDFS.LABEL.stringValue());

        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, LABEL_SUBPROPERTY);

        // The label "Green Goblin" is not assigned directly via rdfs:label but rather via a
        // subproperty of it. Thus, this test also checks if the label sub-property support works.
        List<KBHandle> results = asHandles(aRepository,
                SPARQLQueryBuilder.forItems(aKB).withLabelMatchingExactlyAnyOf("Green Goblin"));

        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.equals("Green Goblin"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).usingRecursiveFieldByFieldElementComparatorOnFields("identifier",
                "name", "language").containsExactlyInAnyOrder(
                        new KBHandle("http://example.org/#green-goblin", "Green Goblin"));
    }

    static void testWithLabelStartingWith_withoutLanguage_noFTS(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        aKB.setFullTextSearchIri(null);

        testWithLabelStartingWith_withoutLanguage(aRepository, aKB);
    }

    static void testWithLabelStartingWith_withoutLanguage(Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX, DATA_LABELS_WITHOUT_LANGUAGE);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withLabelStartingWith("Green"));

        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Green"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).usingRecursiveFieldByFieldElementComparatorOnFields("identifier",
                "name", "language").containsExactlyInAnyOrder(
                        new KBHandle("http://example.org/#green-goblin", "Green Goblin"));
    }

    static void testWithLabelStartingWith_withLanguage_noFTS(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        aKB.setFullTextSearchIri(null);

        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withLabelStartingWith("Green Goblin"));

        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Green"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingRecursiveFieldByFieldElementComparatorOnFields("identifier", "name",
                        "language")
                .containsExactlyInAnyOrder(new KBHandle("http://example.org/#green-goblin",
                        "Green Goblin", null, "en"));
    }

    static void testWithLabelStartingWith_withLanguage_FTS_1(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);

        // Single word - actually, we add a wildcard here so anything that starts with "Green"
        // would also be matched
        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withLabelStartingWith("Green"));

        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Green"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingRecursiveFieldByFieldElementComparatorOnFields("identifier", "name",
                        "language")
                .containsExactlyInAnyOrder(new KBHandle("http://example.org/#green-goblin",
                        "Green Goblin", null, "en"));
    }

    static void testWithLabelStartingWith_withLanguage_FTS_2(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);

        // Two words with the second being very short - this is no problem for the LUCENE FTS
        // and we simply add a wildcard to match "Green Go*"
        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withLabelStartingWith("Green Go"));

        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Green Go"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingRecursiveFieldByFieldElementComparatorOnFields("identifier", "name",
                        "description", "language")
                .containsExactlyInAnyOrder(new KBHandle("http://example.org/#green-goblin",
                        "Green Goblin", null, "en"));
    }

    static void testWithLabelStartingWith_withLanguage_FTS_3(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);

        // Two words with the second being very short and a space following - in this case we
        // assume that the user is in fact searching for "Barack Ob" and do either drop the
        // last element nor add a wildcard
        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withLabelStartingWith("Green Go "));

        assertThat(results).isEmpty();
    }

    static void testWithLabelStartingWith_withLanguage_FTS_4(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        importDataFromString(aRepository, aKB, TURTLE, TURTLE_PREFIX,
                DATA_LABELS_AND_DESCRIPTIONS_WITH_LANGUAGE);

        // Two words with the second being very short - this is no problem for the LUCENE FTS
        // and we simply add a wildcard to match "Green Go*"
        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withLabelStartingWith("Green     Go"));

        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Green Go"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingRecursiveFieldByFieldElementComparatorOnFields("identifier", "name",
                        "description", "language")
                .containsExactlyInAnyOrder(new KBHandle("http://example.org/#green-goblin",
                        "Green Goblin", null, "en"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelStartingWith_Virtuoso_withLanguage_FTS_1() throws Exception
    {
        assertIsReachable(ukpVirtuosoRepo);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_VIRTUOSO.stringValue());

        // Single word - actually, we add a wildcard here so anything that starts with "Barack"
        // would also be matched
        List<KBHandle> results = asHandles(ukpVirtuosoRepo, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelStartingWith("Barack"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Barack"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelStartingWith_Virtuoso_withLanguage_FTS_2() throws Exception
    {
        assertIsReachable(ukpVirtuosoRepo);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_VIRTUOSO.stringValue());

        // Two words with the second being very short - in this case, we drop the very short word
        // so that the user doesn't stop getting suggestions while writing because Virtuoso doesn't
        // do wildcards on words shorter than 4 characters
        List<KBHandle> results = asHandles(ukpVirtuosoRepo, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelStartingWith("Barack Ob"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Barack"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelStartingWith_Virtuoso_withLanguage_FTS_3() throws Exception
    {
        assertIsReachable(ukpVirtuosoRepo);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_VIRTUOSO.stringValue());

        // Two words with the second being very short and a space following - in this case we
        // assmume that the user is in fact searching for "Barack Ob" and do either drop the
        // last element nor add a wildcard
        List<KBHandle> results = asHandles(ukpVirtuosoRepo, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelStartingWith("Barack Ob "));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Barack"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelStartingWith_Virtuoso_withLanguage_FTS_4() throws Exception
    {
        assertIsReachable(ukpVirtuosoRepo);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_VIRTUOSO.stringValue());

        // Two words with the second being 4+ chars - we add a wildcard here so anything
        // starting with "Barack Obam" should match
        List<KBHandle> results = asHandles(ukpVirtuosoRepo, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelStartingWith("Barack Obam"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Barack Obam"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelStartingWith_Wikidata_FTS() throws Exception
    {
        assertIsReachable(wikidata);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_WIKIDATA.stringValue());
        initWikidataMapping();

        List<KBHandle> results = asHandles(wikidata, SPARQLQueryBuilder //
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

        List<KBHandle> results = asHandles(zbwGnd, SPARQLQueryBuilder //
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

        List<KBHandle> results = asHandles(zbwStw, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelMatchingExactlyAnyOf("Labour"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> "Labour".equals(label));
    }

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
        List<KBHandle> results = asHandles(zbwGnd, SPARQLQueryBuilder //
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
        initWikidataMapping();

        List<KBHandle> results = asHandles(wikidata, SPARQLQueryBuilder //
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
        initWikidataMapping();

        List<KBHandle> results = asHandles(wikidata, SPARQLQueryBuilder //
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
        initWikidataMapping();

        List<KBHandle> results = asHandles(wikidata, SPARQLQueryBuilder //
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
        initWikidataMapping();

        List<KBHandle> results = asHandles(wikidata, SPARQLQueryBuilder //
                .forInstances(kb) //
                .withLabelMatchingExactlyAnyOf("Labour", "Tory"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> "Labour".equals(label) || "Tory".equals(label));
    }

    @Tag("slow")
    @Test
    public void testWithLabelMatchingExactlyAnyOf_Virtuoso_withLanguage_FTS() throws Exception
    {
        assertIsReachable(ukpVirtuosoRepo);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(FTS_VIRTUOSO.stringValue());

        List<KBHandle> results = asHandles(ukpVirtuosoRepo, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelMatchingExactlyAnyOf("Green Goblin"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> "Green Goblin".equals(label));
    }

    @Tag("slow")
    @Test
    public void testWithLabelStartingWith_HUCIT_noFTS() throws Exception
    {
        assertIsReachable(hucit);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(null);

        List<KBHandle> results = asHandles(hucit, SPARQLQueryBuilder //
                .forItems(kb) //
                .withLabelStartingWith("Achilles"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Achilles"));
    }

    @Tag("slow")
    @Test
    public void testWithLabelStartingWith_onlyDescendants_HUCIT_noFTS() throws Exception
    {
        assertIsReachable(hucit);

        kb.setType(REMOTE);
        kb.setFullTextSearchIri(null);

        List<KBHandle> results = asHandles(hucit, SPARQLQueryBuilder //
                .forInstances(kb) //
                .descendantsOf("http://erlangen-crm.org/efrbroo/F1_Work") //
                .withLabelStartingWith("Achilles"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Achilles"));
    }

    static void testWithLabelStartingWith_OLIA(Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        aKB.setLabelIri("http://purl.org/olia/system.owl#hasTag");

        importDataFromFile(aRepository, aKB, "src/test/resources/data/penn.owl");

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forInstances(aKB) //
                .withLabelStartingWith("N"));

        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel).containsExactlyInAnyOrder("NN", "NNP",
                "NNPS", "NNS");
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
        initWikidataMapping();

        List<KBHandle> results = asHandles(wikidata, SPARQLQueryBuilder //
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

        List<KBHandle> results = asHandles(dbpedia,
                SPARQLQueryBuilder.forClasses(kb).roots().retrieveLabel());

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

        List<KBHandle> results = asHandles(dbpedia, SPARQLQueryBuilder //
                .forClasses(kb) //
                .ancestorsOf("http://dbpedia.org/ontology/Organisation") //
                .retrieveLabel());

        assertThat(results).isNotEmpty();

        assertThat(results) //
                .extracting(KBHandle::getName) //
                .contains("agent", "Thing");
    }

    static void testWithLabelContainingAnyOf_pets_ttl_noFTS(Repository aRepository,
            KnowledgeBase aKB)
        throws Exception
    {
        aKB.setFullTextSearchIri(null);

        importDataFromFile(aRepository, aKB, "src/test/resources/data/pets.ttl");

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forItems(aKB) //
                .withLabelContainingAnyOf("Socke"));

        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.contains("Socke"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).usingRecursiveFieldByFieldElementComparatorOnFields("identifier",
                "name", "language")
                .containsExactlyInAnyOrder(new KBHandle("http://mbugert.de/pets#socke", "Socke"));
    }

    static void thatRootsCanBeRetrieved_ontolex(Repository aRepository, KnowledgeBase aKB)
        throws Exception
    {
        importDataFromFile(aRepository, aKB,
                "src/test/resources/data/wordnet-ontolex-ontology.owl");

        initOwlMapping(aKB);

        List<KBHandle> results = asHandles(aRepository, SPARQLQueryBuilder //
                .forClasses(aKB) //
                .roots() //
                .retrieveLabel());

        assertThat(results).isNotEmpty();

        assertThat(results).extracting(KBHandle::getUiLabel).contains("Adjective position",
                "Lexical domain", "Part of speech", "Phrase type", "Synset");
    }

    @Test
    public void thatLineBreaksAreSanitized() throws Exception
    {
        assertThat(sanitizeQueryString_FTS("Green\n\rGoblin")).isEqualTo("Green Goblin");
    }

    static void importDataFromFile(Repository aRepository, KnowledgeBase aKB, String aFilename)
        throws IOException
    {
        // Detect the file format
        RDFFormat format = Rio.getParserFormatForFileName(aFilename).orElse(RDFXML);

        System.out.printf("Loading %s data fron %s%n", format, aFilename);

        // Load files into the repository
        try (InputStream is = new FileInputStream(aFilename)) {
            importData(aRepository, aKB, format, is);
        }
    }

    static void importDataFromString(Repository aRepository, KnowledgeBase aKB, RDFFormat aFormat,
            String... aRdfData)
        throws IOException
    {
        String data = String.join("\n", aRdfData);

        // Load files into the repository
        try (InputStream is = IOUtils.toInputStream(data, UTF_8)) {
            importData(aRepository, aKB, aFormat, is);
        }
    }

    static void importData(Repository aRepository, KnowledgeBase aKB, RDFFormat aFormat,
            InputStream aIS)
        throws IOException
    {
        try (RepositoryConnection conn = aRepository.getConnection()) {
            // If the RDF file contains relative URLs, then they probably start with a hash.
            // To avoid having two hashes here, we drop the hash from the base prefix configured
            // by the user.
            String prefix = StringUtils.removeEnd(aKB.getBasePrefix(), "#");
            if (aKB.getDefaultDatasetIri() != null) {
                var ctx = SimpleValueFactory.getInstance().createIRI(aKB.getDefaultDatasetIri());
                conn.add(aIS, prefix, aFormat, ctx);
            }
            else {
                conn.add(aIS, prefix, aFormat);
            }
        }
    }

    static void initRdfsMapping(KnowledgeBase aKB)
    {
        aKB.setClassIri(RDFS.CLASS.stringValue());
        aKB.setSubclassIri(RDFS.SUBCLASSOF.stringValue());
        aKB.setTypeIri(RDF.TYPE.stringValue());
        aKB.setLabelIri(RDFS.LABEL.stringValue());
        aKB.setPropertyTypeIri(RDF.PROPERTY.stringValue());
        aKB.setDescriptionIri(RDFS.COMMENT.stringValue());
        // We are intentionally not using RDFS.LABEL here to ensure we can test the label
        // and property label separately
        aKB.setPropertyLabelIri(SKOS.PREF_LABEL.stringValue());
        // We are intentionally not using RDFS.COMMENT here to ensure we can test the description
        // and property description separately
        aKB.setPropertyDescriptionIri("http://schema.org/description");
        aKB.setSubPropertyIri(RDFS.SUBPROPERTYOF.stringValue());
    }

    static void initOwlMapping(KnowledgeBase aKB)
    {
        aKB.setClassIri(OWL.CLASS.stringValue());
        aKB.setSubclassIri(RDFS.SUBCLASSOF.stringValue());
        aKB.setTypeIri(RDF.TYPE.stringValue());
        aKB.setLabelIri(RDFS.LABEL.stringValue());
        aKB.setPropertyTypeIri(RDF.PROPERTY.stringValue());
        aKB.setDescriptionIri(RDFS.COMMENT.stringValue());
        aKB.setPropertyLabelIri(RDF.PROPERTY.stringValue());
        aKB.setPropertyDescriptionIri(RDFS.COMMENT.stringValue());
        aKB.setSubPropertyIri(RDFS.SUBPROPERTYOF.stringValue());
    }

    private void initWikidataMapping()
    {
        kb.setClassIri("http://www.wikidata.org/entity/Q35120");
        kb.setSubclassIri("http://www.wikidata.org/prop/direct/P279");
        kb.setTypeIri("http://www.wikidata.org/prop/direct/P31");
        kb.setLabelIri("http://www.w3.org/2000/01/rdf-schema#label");
        kb.setPropertyTypeIri("http://www.wikidata.org/entity/Q18616576");
        kb.setDescriptionIri("http://schema.org/description");
        kb.setPropertyLabelIri("http://www.w3.org/2000/01/rdf-schema#label");
        kb.setPropertyDescriptionIri("http://www.w3.org/2000/01/rdf-schema#comment");
        kb.setSubPropertyIri("http://www.wikidata.org/prop/direct/P1647");
    }

    public static void assertIsReachable(Repository aRepository)
    {
        if (!(aRepository instanceof SPARQLRepository)) {
            return;
        }

        SPARQLRepository sparqlRepository = (SPARQLRepository) aRepository;

        assumeTrue(isReachable(sparqlRepository.toString()),
                "Remote repository at [" + sparqlRepository + "] is not reachable");
    }
}
