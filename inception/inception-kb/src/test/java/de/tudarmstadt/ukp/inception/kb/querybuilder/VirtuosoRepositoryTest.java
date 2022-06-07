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

import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_VIRTUOSO;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.restoreSslVerification;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.suspendSslVerification;
import static java.util.Arrays.asList;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderTest.Scenario;

// The repository has to exist and Virtuoso has to be running for this test to run.
// To create the repository, use the following steps:
// Install Virtuoso, e.g. `brew install virtuoso`
// Start Virtuoso, e.g. `virtuoso-t +foreground +configfile /opt/homebrew/Cellar/virtuoso/7.2.7/var/lib/virtuoso/db/virtuoso.ini`
// Go to http://localhost:8890/conductor -> System Admin -> User Accounts -> SPARQL and add account role SPARQL_UPDATE
// Check if FTS is enabled: `SELECT * from DB.DBA.RDF_OBJ_FT_RULES;`
// Enable FTS: `DB.DBA.RDF_OBJ_FT_RULE_ADD (null, null, 'ALL');`
@Disabled("Requires manually setting up a test server")
public class VirtuosoRepositoryTest
{
    private Repository repository;
    private KnowledgeBase kb;

    @BeforeEach
    public void setUp(TestInfo aTestInfo)
    {
        String methodName = aTestInfo.getTestMethod().map(Method::getName).orElse("<unknown>");
        System.out.printf("\n=== %s === %s =====================\n", methodName,
                aTestInfo.getDisplayName());

        suspendSslVerification();

        kb = new KnowledgeBase();
        kb.setDefaultLanguage("en");
        kb.setDefaultDatasetIri("http://testgraph");
        kb.setType(RepositoryType.REMOTE);
        kb.setFullTextSearchIri(FTS_VIRTUOSO.stringValue());
        kb.setMaxResults(100);

        SPARQLQueryBuilderTest.initRdfsMapping(kb);

        repository = SPARQLQueryBuilderTest.buildSparqlRepository("http://localhost:8890/sparql/");

        try (RepositoryConnection conn = repository.getConnection()) {
            var ctx = SimpleValueFactory.getInstance().createIRI(kb.getDefaultDatasetIri());
            conn.clear(ctx);
        }
    }

    @AfterEach
    public void tearDown()
    {
        restoreSslVerification();
    }

    private static List<Arguments> tests() throws Exception
    {
        var exclusions = asList( //
                // Virtuoso does not seem to like the VALUES clause used here. Should not be too
                // bad because a similar query using FTS works and Virtuoso should not be used
                // without FTS anyway
                "testWithLabelMatchingExactlyAnyOf_subproperty_noFTS",
                // Seems to be a limitation in Virtuoso:
                // Virtuoso 37000 Error SP031: SPARQL compiler: Object of transitive triple pattern
                // should be variable or QName or literal, not blank node
                "thatPropertyQueryLimitedToDomainDoesNotReturnOutOfScopeResults",
                // Virtuoso does not like to import the test data for this one
                "testWithLabelStartingWith_OLIA");

        return SPARQLQueryBuilderTest.tests().stream() //
                .filter(scenario -> !exclusions.contains(scenario.name))
                .map(scenario -> Arguments.of(scenario.name, scenario))
                .collect(Collectors.toList());
    }

    @ParameterizedTest(name = "{index}: test {0}")
    @MethodSource("tests")
    public void runTests(String aScenarioName, Scenario aScenario) throws Exception
    {
        aScenario.implementation.accept(repository, kb);
    }
}
