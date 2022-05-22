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

import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_STARDOG;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.restoreSslVerification;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.suspendSslVerification;

import java.lang.reflect.Method;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

// The repository has to exist and stardoc has to be running for this test to run.
// To create the repository, use the following command:
// stardog-admin db create -n test -o search.enabled=true --
@Ignore("Requires manually setting up a test server")
public class StardogRepositoryTest
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
        kb.setType(RepositoryType.LOCAL);
        kb.setFullTextSearchIri(FTS_STARDOG.stringValue());
        kb.setMaxResults(100);

        SPARQLQueryBuilderTest.initRdfsMapping(kb);

        repository = SPARQLQueryBuilderTest.buildSparqlRepository(
                "http://admin:admin@localhost:5820/test/query",
                "http://admin:admin@localhost:5820/test/update");

        try (RepositoryConnection conn = repository.getConnection()) {
            conn.clear();
        }
    }

    @AfterEach
    public void tearDown()
    {
        restoreSslVerification();
    }

    @Test
    public void testWithLabelStartingWith_withLanguage_FTS_1() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelStartingWith_withLanguage_FTS_1(repository, kb);
    }

    @Test
    public void testWithLabelStartingWith_withLanguage_FTS_2() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelStartingWith_withLanguage_FTS_2(repository, kb);
    }

    @Test
    public void testWithLabelStartingWith_withLanguage_FTS_3() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelStartingWith_withLanguage_FTS_3(repository, kb);
    }

    @Test
    public void testWithLabelStartingWith_RDF4J_withLanguage_noFTS() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelStartingWith_withLanguage_noFTS(repository, kb);
    }

    @Test
    public void testWithLabelContainingAnyOf_pets_ttl_noFTS() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelContainingAnyOf_pets_ttl_noFTS(repository, kb);
    }

    @Test
    public void thatRootsCanBeRetrieved_ontolex() throws Exception
    {
        SPARQLQueryBuilderTest.thatRootsCanBeRetrieved_ontolex(repository, kb);
    }

    @Test
    public void testWithLabelContainingAnyOf_withLanguage_noFTS() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelContainingAnyOf_withLanguage_noFTS(repository, kb);
    }

    @Test
    public void testWithLabelContainingAnyOf_withLanguage_FTS() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelContainingAnyOf_withLanguage(repository, kb);
    }

    @Test
    public void testWithLabelMatchingAnyOf_withLanguage_FTS() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelMatchingAnyOf_withLanguage(repository, kb);
    }

    @Test
    public void testWithLabelMatchingAnyOf_withLanguage_noFTS() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelMatchingAnyOf_withLanguage_noFTS(repository, kb);
    }

    @Test
    public void testWithLabelStartingWith_withoutLanguage_FTS() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelStartingWith_withoutLanguage(repository, kb);
    }

    @Test
    public void testWithLabelStartingWith_withoutLanguage_noFTS() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelStartingWith_withoutLanguage_noFTS(repository, kb);
    }

    @Test
    public void testWithLabelMatchingExactlyAnyOf_subproperty_FTS() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelMatchingExactlyAnyOf_subproperty(repository, kb);
    }

    @Test
    public void testWithLabelMatchingExactlyAnyOf_subproperty_noFTS() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelMatchingExactlyAnyOf_subproperty_noFTS(repository, kb);
    }

    @Test
    public void testWithLabelMatchingExactlyAnyOf_withLanguage_noFTS() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelMatchingExactlyAnyOf_withLanguage_noFTS(repository, kb);
    }

    @Test
    public void testWithLabelMatchingExactlyAnyOf_withLanguage_FTS() throws Exception
    {
        SPARQLQueryBuilderTest.testWithLabelMatchingExactlyAnyOf_withLanguage(repository, kb);
    }
}
