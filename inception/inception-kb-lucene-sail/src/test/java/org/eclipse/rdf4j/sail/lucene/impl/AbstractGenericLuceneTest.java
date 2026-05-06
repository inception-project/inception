/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.MATCHES;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.PROPERTY;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.QUERY;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SCORE;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SNIPPET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.concurrent.locks.Properties;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.LuceneSailSchema;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Timeout(value = 10, unit = TimeUnit.MINUTES)
public abstract class AbstractGenericLuceneTest
{
    protected static final ValueFactory vf = SimpleValueFactory.getInstance();

    public static final String QUERY_STRING;

    public static final IRI SUBJECT_1 = vf.createIRI("urn:subject1");

    public static final IRI SUBJECT_2 = vf.createIRI("urn:subject2");

    public static final IRI SUBJECT_3 = vf.createIRI("urn:subject3");

    public static final IRI SUBJECT_4 = vf.createIRI("urn:subject4");

    public static final IRI SUBJECT_5 = vf.createIRI("urn:subject5");

    public static final IRI CONTEXT_1 = vf.createIRI("urn:context1");

    public static final IRI CONTEXT_2 = vf.createIRI("urn:context2");

    public static final IRI CONTEXT_3 = vf.createIRI("urn:context3");

    public static final IRI PREDICATE_1 = vf.createIRI("urn:predicate1");

    public static final IRI PREDICATE_2 = vf.createIRI("urn:predicate2");

    public static final IRI PREDICATE_3 = vf.createIRI("urn:predicate3");

    static final Logger LOG = LoggerFactory.getLogger(AbstractGenericLuceneTest.class);

    private final Random random = new Random(43252333);

    protected LuceneSail sail;

    protected Repository repository;

    protected RepositoryConnection connection;

    static {
        StringBuilder buffer = new StringBuilder();
        buffer.append("SELECT ?Subject ?Score ");
        buffer.append("WHERE { ?Subject <" + MATCHES + "> [ ");
        buffer.append(" <" + QUERY + "> ?Query; ");
        buffer.append(" <" + SCORE + "> ?Score ].} ");
        QUERY_STRING = buffer.toString();
    }

    protected abstract void configure(LuceneSail sail) throws IOException;

    @BeforeEach
    public void setUp() throws Exception
    {
        // set logging, uncomment this to get better logging for debugging
        // org.apache.log4j.BasicConfigurator.configure();

        // setup a LuceneSail
        MemoryStore memoryStore = new MemoryStore();
        // enable lock tracking
        Properties.setLockTrackingEnabled(true);
        sail = new LuceneSail();
        configure(sail);
        sail.setBaseSail(memoryStore);

        // create a Repository wrapping the LuceneSail
        repository = new SailRepository(sail);

        // add some statements to it
        connection = repository.getConnection();
        connection.begin();
        connection.add(SUBJECT_1, PREDICATE_1, vf.createLiteral("one"));
        connection.add(SUBJECT_1, PREDICATE_1, vf.createLiteral("five"));
        connection.add(SUBJECT_1, PREDICATE_2, vf.createLiteral("two"));
        connection.add(SUBJECT_2, PREDICATE_1, vf.createLiteral("one"));
        connection.add(SUBJECT_2, PREDICATE_2, vf.createLiteral("three"));
        connection.add(SUBJECT_3, PREDICATE_1, vf.createLiteral("four"));
        connection.add(SUBJECT_3, PREDICATE_2, vf.createLiteral("one"));
        connection.add(SUBJECT_3, PREDICATE_3, SUBJECT_1);
        connection.add(SUBJECT_3, PREDICATE_3, SUBJECT_2);
        connection.commit();
    }

    @AfterEach
    public void tearDown() throws RepositoryException
    {
        try {
            if (connection != null) {
                connection.close();
            }
        }
        finally {
            if (repository != null) {
                repository.shutDown();
            }
        }
        Properties.setLockTrackingEnabled(false);
    }

    @Test
    public void testComplexQueryTwo()
        throws MalformedQueryException, RepositoryException, QueryEvaluationException
    {
        // prepare the query
        StringBuilder buffer = new StringBuilder();
        buffer.append("SELECT ?Resource ?Matching ?Score ");
        buffer.append("WHERE { ?Resource <" + PREDICATE_3 + "> ?Matching .\n");
        buffer.append("        ?Matching <" + MATCHES + "> [ \n ");
        buffer.append("                                     <" + QUERY + "> \"two\"; \n");
        buffer.append("                                     <" + SCORE + "> ?Score ]. }");
        String q = buffer.toString();

        // fire a query for all subjects with a given term
        TupleQuery query = connection.prepareTupleQuery(q);

        // check the results
        try (TupleQueryResult result = query.evaluate()) {
            // check the results
            assertTrue(result.hasNext());
            BindingSet bindings = result.next();
            assertEquals(SUBJECT_3, (IRI) bindings.getValue("Resource"));
            assertEquals(SUBJECT_1, (IRI) bindings.getValue("Matching"));
            assertNotNull(bindings.getValue("Score"));

            assertFalse(result.hasNext());
        }
    }

    private void evaluate(String[] queries, ArrayList<List<Map<String, String>>> expectedResults)
        throws MalformedQueryException, RepositoryException, QueryEvaluationException
    {
        for (int queryID = 0; queryID < queries.length; queryID++) {
            String sparql = queries[queryID];
            List<Map<String, String>> expectedResultSet = expectedResults.get(queryID);

            // fire the query
            TupleQuery query = connection.prepareTupleQuery(sparql);
            int actualResults;
            Set<Integer> matched;
            // check the results
            try (TupleQueryResult tqr = query.evaluate()) {
                // check the results
                actualResults = 0;
                matched = new HashSet<>();
                while (tqr.hasNext()) {
                    BindingSet bs = tqr.next();
                    actualResults++;

                    boolean matches;
                    for (int resultSetID = 0; resultSetID < expectedResultSet
                            .size(); resultSetID++) {
                        // ignore results that matched before
                        if (matched.contains(resultSetID)) {
                            continue;
                        }

                        // assume it matches
                        matches = true;

                        // get the result we compare with now
                        Map<String, String> expectedResult = new HashMap<>(
                                expectedResultSet.get(resultSetID));

                        // get all var names
                        Collection<String> vars = new ArrayList<>(expectedResult.keySet());

                        // check if all actual results are expected
                        for (String var : vars) {
                            String expectedVal = expectedResult.get(var);
                            Value actualVal = bs.getValue(var);

                            if (expectedVal == null) {
                                // don't care about the actual value, as long as there is
                                // one
                                if (actualVal == null) {
                                    matches = false;
                                    break;
                                }
                            }
                            else {
                                // compare the values
                                if ((actualVal == null)
                                        || (expectedVal.compareTo(actualVal.stringValue()) != 0)) {
                                    matches = false;
                                    break;
                                }
                            }

                            // remove the matched result so that we do not match it twice
                            expectedResult.remove(var);
                        }

                        // check if expected results were existing
                        if (!expectedResult.isEmpty()) {
                            matches = false;
                        }

                        if (matches) {
                            matched.add(resultSetID);
                            break;
                        }
                    }
                }
            }

            // the number of matched expected results must be equal to the number
            // of actual results
            assertEquals(expectedResultSet.size(), matched.size(),
                    "How many expected results were retrieved for query #" + queryID + "?");
            assertEquals(expectedResultSet.size(), actualResults,
                    "How many actual results were retrieved for query #" + queryID + "?");
        }
    }

    @Test
    public void testPredicateLuceneQueries()
        throws MalformedQueryException, RepositoryException, QueryEvaluationException
    {
        // prepare the query
        String[] queries = new String[] {
                "SELECT ?Resource ?Score ?Snippet \n" + "WHERE { " + "  ?Resource <" + MATCHES
                        + "> [ \n" + "                                 <" + QUERY + "> \"one\"; \n"
                        + "                                 <" + SCORE + "> ?Score; \n"
                        + "                                 <" + SNIPPET + "> ?Snippet ] }\n",
                "SELECT ?Resource ?Score ?Snippet \n" + "WHERE { " + "  ?Resource <" + MATCHES
                        + "> [ \n" + "                                 <" + QUERY + "> \"five\"; \n"
                        + "                                 <" + SCORE + "> ?Score; \n"
                        + "                                 <" + SNIPPET + "> ?Snippet ] }\n" };

        ArrayList<List<Map<String, String>>> allResults = new ArrayList<>();

        // create a new result set
        ArrayList<Map<String, String>> resultSet = new ArrayList<>();

        // one possible result
        Map<String, String> result1 = new HashMap<>();
        result1.put("Resource", SUBJECT_1.stringValue());
        result1.put("Score", null); // null means: ignore the value
        result1.put("Snippet", "<B>one</B>");
        resultSet.add(result1);

        // another possible result
        Map<String, String> result2 = new HashMap<>();
        result2.put("Resource", SUBJECT_2.stringValue());
        result2.put("Score", null); // null means: ignore the value
        result2.put("Snippet", "<B>one</B>");
        resultSet.add(result2);

        // another possible result
        Map<String, String> result3 = new HashMap<>();
        result3.put("Resource", SUBJECT_3.stringValue());
        result3.put("Score", null); // null means: ignore the value
        result3.put("Snippet", "<B>one</B>");
        resultSet.add(result3);

        // add the results of for the first query
        allResults.add(resultSet);

        // recreate a result set
        resultSet = new ArrayList<>();

        // one possible result
        Map<String, String> result = new HashMap<>();
        result.put("Resource", SUBJECT_1.stringValue());
        result.put("Score", null); // null means: ignore the value
        result.put("Snippet", "<B>five</B>");
        resultSet.add(result);

        // add the results of for the first query
        allResults.add(resultSet);

        evaluate(queries, allResults);
    }

    @Test
    public void testSnippetQueries()
        throws MalformedQueryException, RepositoryException, QueryEvaluationException
    {
        // prepare the query
        // search for the term "one", but only in predicate 1
        StringBuilder buffer = new StringBuilder();
        buffer.append("SELECT ?Resource ?Score \n");
        buffer.append("WHERE { \n");
        buffer.append("  ?Resource <" + MATCHES + "> [\n ");
        buffer.append("                          <" + QUERY + "> \"one\";\n");
        buffer.append("                          <" + PROPERTY + "> <" + PREDICATE_1 + ">;\n ");
        buffer.append("                          <" + SCORE + "> ?Score ]. } ");
        String q = buffer.toString();

        // fire the query
        TupleQuery query = connection.prepareTupleQuery(q);
        try (TupleQueryResult result = query.evaluate()) {

            // check the results
            BindingSet bindings;

            // the first result is subject 1 and has a score
            int results = 0;
            Set<IRI> expectedSubject = new HashSet<>();
            expectedSubject.add(SUBJECT_1);
            expectedSubject.add(SUBJECT_2);
            while (result.hasNext()) {
                results++;
                bindings = result.next();

                // the resource should be among the set of expected subjects, if so,
                // remove it from the set
                assertTrue(expectedSubject.remove(bindings.getValue("Resource")));

                // there should be a score
                assertNotNull(bindings.getValue("Score"));
            }

            // there should have been only 2 results
            assertEquals(2, results);

            result.close();
        }
    }

    /**
     * Test if the snippets do not accidentially come from the "text" field while we actually expect
     * them to come from the predicate field.
     */
    @Test
    public void testSnippetLimitedToPredicate()
        throws MalformedQueryException, RepositoryException, QueryEvaluationException
    {
        try (RepositoryConnection localConnection = repository.getConnection()) {
            localConnection.begin();
            // we use the string 'charly' as test-case. the snippets should contain
            // "come" and "unicorn"
            // and 'poor' should not be returned if we limit on predicate1
            // and watch http://www.youtube.com/watch?v=Q5im0Ssyyus like 25mio others
            localConnection.add(SUBJECT_1, PREDICATE_1,
                    vf.createLiteral("come charly lets go to candy mountain"));
            localConnection.add(SUBJECT_1, PREDICATE_1,
                    vf.createLiteral("but the unicorn charly said to goaway"));
            localConnection.add(SUBJECT_1, PREDICATE_2,
                    vf.createLiteral("there was poor charly without a kidney"));
            localConnection.commit();
        }

        // prepare the query
        // search for the term "charly", but only in predicate 1
        StringBuilder buffer = new StringBuilder();
        buffer.append("SELECT ?Resource ?Snippet ?Score \n");
        buffer.append("WHERE { \n");
        buffer.append("  ?Resource <" + MATCHES + "> [\n ");
        buffer.append("                          <" + QUERY + "> \"charly\";\n");
        buffer.append("                          <" + PROPERTY + "> <" + PREDICATE_1 + ">;\n ");
        buffer.append("                          <" + SNIPPET + "> ?Snippet; ");
        buffer.append("                          <" + SCORE + "> ?Score ]. } ");
        String q = buffer.toString();

        // fire the query
        TupleQuery query = connection.prepareTupleQuery(q);
        try (TupleQueryResult result = query.evaluate()) {

            // check the results
            BindingSet bindings;

            // the first result is subject 1 and has a score
            int results = 0;
            Set<String> expectedSnippetPart = new HashSet<>();
            expectedSnippetPart.add("come");
            expectedSnippetPart.add("unicorn");
            String notexpected = "poor";
            while (result.hasNext()) {
                results++;
                bindings = result.next();

                // the resource should be among the set of expected subjects, if so,
                // remove it from the set
                String snippet = ((Literal) bindings.getValue("Snippet")).stringValue();
                boolean foundexpected = false;
                for (Iterator<String> i = expectedSnippetPart.iterator(); i.hasNext();) {
                    String expected = i.next();
                    if (snippet.contains(expected)) {
                        foundexpected = true;
                        i.remove();
                    }
                }
                if (snippet.contains(notexpected)) {
                    fail("snippet '" + snippet + "' contained value '" + notexpected
                            + "' from predicate " + PREDICATE_2);
                }
                if (!foundexpected) {
                    fail("did not find any of the expected strings " + expectedSnippetPart
                            + " in the snippet " + snippet);
                }

                // there should be a score
                assertNotNull(bindings.getValue("Score"));
            }

            // we found all
            assertTrue(expectedSnippetPart.isEmpty(),
                    "These were expected but not found: " + expectedSnippetPart);

            assertEquals(2, results, "there should have been 2 results");
        }
    }

    @Test
    public void testCharlyTerm()
    {

        try (RepositoryConnection localConnection = repository.getConnection()) {
            localConnection.begin();
            // we use the string 'charly' as test-case. the snippets should contain
            // "come" and "unicorn"
            // and 'poor' should not be returned if we limit on predicate1
            // and watch http://www.youtube.com/watch?v=Q5im0Ssyyus like 25mio others
            localConnection.add(SUBJECT_1, PREDICATE_1,
                    vf.createLiteral("come charly lets go to candy mountain"));
            localConnection.add(SUBJECT_1, PREDICATE_1,
                    vf.createLiteral("but the unicorn charly said to goaway"));
            localConnection.add(SUBJECT_1, PREDICATE_2,
                    vf.createLiteral("there was poor charly without a kidney"));
            localConnection.commit();
        }
        // search for the term "charly" in all predicates
        StringBuilder buffer = new StringBuilder();
        buffer.append("SELECT ?Resource ?Snippet ?Score \n");
        buffer.append("WHERE { \n");
        buffer.append("  ?Resource <" + MATCHES + "> [\n ");
        buffer.append("                          <" + QUERY + "> \"charly\";\n");
        buffer.append("                          <" + SNIPPET + "> ?Snippet; ");
        buffer.append("                          <" + SCORE + "> ?Score ]. } ");
        String q = buffer.toString();

        // fire the query
        TupleQuery query = connection.prepareTupleQuery(q);
        try (TupleQueryResult result = query.evaluate()) {

            // check the results
            BindingSet bindings;

            // the first result is subject 1 and has a score
            int results = 0;
            Set<String> expectedSnippetPart = new HashSet<>();
            expectedSnippetPart.add("come");
            expectedSnippetPart.add("unicorn");
            expectedSnippetPart.add("poor");

            while (result.hasNext()) {
                results++;
                bindings = result.next();

                // the resource should be among the set of expected subjects, if so,
                // remove it from the set
                String snippet = ((Literal) bindings.getValue("Snippet")).stringValue();
                boolean foundexpected = false;
                for (Iterator<String> i = expectedSnippetPart.iterator(); i.hasNext();) {
                    String expected = i.next();
                    if (snippet.contains(expected)) {
                        foundexpected = true;
                        i.remove();
                    }
                }
                if (!foundexpected) {
                    fail("did not find any of the expected strings " + expectedSnippetPart
                            + " in the snippet " + snippet);
                }

                // there should be a score
                assertNotNull(bindings.getValue("Score"));
            }

            // we found all
            assertTrue(expectedSnippetPart.isEmpty(),
                    "These were expected but not found: " + expectedSnippetPart);

            assertEquals(3, results, "there should have been 3 results");
        }
    }

    @Test
    public void testGraphQuery()
        throws QueryEvaluationException, MalformedQueryException, RepositoryException
    {
        IRI score = vf.createIRI(LuceneSailSchema.NAMESPACE + "score");
        StringBuilder query = new StringBuilder();

        // here we would expect two links from SUBJECT3 to SUBJECT1 and SUBJECT2
        // and one link from SUBJECT3 to its score
        query.append("PREFIX lucenesail: <" + LuceneSailSchema.NAMESPACE + "> \n");
        query.append("CONSTRUCT { \n");
        query.append("    ?r <" + PREDICATE_3 + "> ?r2 ; \n");
        query.append("       <" + score + "> ?s . }\n");
        query.append("WHERE {\n");
        query.append("    ?r lucenesail:matches ?match. ?match lucenesail:query \"four\"; \n");
        query.append("                                         lucenesail:score ?s . \n");
        query.append("    ?r <" + PREDICATE_3 + "> ?r2 } \n");

        int r = 0;
        int n = 0;
        GraphQuery gq = connection.prepareGraphQuery(query.toString());
        try (GraphQueryResult result = gq.evaluate()) {
            while (result.hasNext()) {
                Statement statement = result.next();
                n++;

                if (statement.getSubject().equals(SUBJECT_3)
                        && statement.getPredicate().equals(PREDICATE_3)
                        && statement.getObject().equals(SUBJECT_1)) {
                    r |= 1;
                    continue;
                }
                if (statement.getSubject().equals(SUBJECT_3)
                        && statement.getPredicate().equals(PREDICATE_3)
                        && statement.getObject().equals(SUBJECT_2)) {
                    r |= 2;
                    continue;
                }
                if (statement.getSubject().equals(SUBJECT_3)
                        && statement.getPredicate().equals(score)) {
                    r |= 4;
                    continue;
                }
            }

            assertEquals(3, n);
            assertEquals(7, r);

        }

    }

    @Test
    public void testQueryWithSpecifiedSubject()
        throws RepositoryException, MalformedQueryException, QueryEvaluationException
    {
        // fire a query with the subject pre-specified
        TupleQuery query = connection.prepareTupleQuery(QUERY_STRING);
        query.setBinding("Subject", SUBJECT_1);
        query.setBinding("Query", vf.createLiteral("one"));
        // check that this subject and only this subject is returned
        try (TupleQueryResult result = query.evaluate()) {
            // check that this subject and only this subject is returned
            assertTrue(result.hasNext());
            BindingSet bindings = result.next();
            assertEquals(SUBJECT_1, (IRI) bindings.getValue("Subject"));
            assertNotNull(bindings.getValue("Score"));
            assertFalse(result.hasNext());
        }
    }

    @Test
    public void testUnionQuery()
        throws RepositoryException, MalformedQueryException, QueryEvaluationException
    {
        String queryStr = "";
        queryStr += "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#> ";
        queryStr += "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ";
        queryStr += "SELECT DISTINCT ?result { ";
        queryStr += "{ ?result search:matches ?match1 . ";
        queryStr += "  ?match1 search:query 'one' ; ";
        queryStr += "          search:property <urn:predicate1> . }";
        queryStr += " UNION ";
        queryStr += "{ ?result search:matches ?match2 . ";
        queryStr += "  ?match2 search:query 'one' ; ";
        queryStr += "          search:property <urn:predicate2> . } ";
        queryStr += "} ";

        // fire a query with the subject pre-specified
        TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
        try (TupleQueryResult result = query.evaluate()) {
            assertThat(result.hasNext()).isTrue();
        }
    }

    /**
     * Tests adding data to two contexts (graphs).
     *
     */
    @Test
    public void testContextHandling()
    {
        connection.add(SUBJECT_4, PREDICATE_1, vf.createLiteral("sfourponecone"), CONTEXT_1);
        connection.add(SUBJECT_4, PREDICATE_2, vf.createLiteral("sfourptwocone"), CONTEXT_1);
        connection.add(SUBJECT_5, PREDICATE_1, vf.createLiteral("sfiveponecone"), CONTEXT_1);
        connection.add(SUBJECT_5, PREDICATE_1, vf.createLiteral("sfiveponectwo"), CONTEXT_2);
        connection.add(SUBJECT_5, PREDICATE_2, vf.createLiteral("sfiveptwoctwo"), CONTEXT_2);
        connection.commit();
        // connection.close();
        // connection = repository.getConnection();
        // connection.setAutoCommit(false);
        // test querying
        assertQueryResult("sfourponecone", PREDICATE_1, SUBJECT_4);
        assertQueryResult("sfourptwocone", PREDICATE_2, SUBJECT_4);
        assertQueryResult("sfiveponecone", PREDICATE_1, SUBJECT_5);
        assertQueryResult("sfiveponectwo", PREDICATE_1, SUBJECT_5);
        assertQueryResult("sfiveptwoctwo", PREDICATE_2, SUBJECT_5);
        // blind test to see if this method works:
        assertNoQueryResult("johannesgrenzfurthner");
        // remove a context
        connection.clear(CONTEXT_1);
        connection.commit();
        assertNoQueryResult("sfourponecone");
        assertNoQueryResult("sfourptwocone");
        assertNoQueryResult("sfiveponecone");
        assertQueryResult("sfiveponectwo", PREDICATE_1, SUBJECT_5);
        assertQueryResult("sfiveptwoctwo", PREDICATE_2, SUBJECT_5);
    }

    /**
     * we experienced problems with the NULL context and lucenesail in August 2008
     *
     */
    @Test
    public void testNullContextHandling()
    {
        connection.add(SUBJECT_4, PREDICATE_1, vf.createLiteral("sfourponecone"));
        connection.add(SUBJECT_4, PREDICATE_2, vf.createLiteral("sfourptwocone"));
        connection.add(SUBJECT_5, PREDICATE_1, vf.createLiteral("sfiveponecone"));
        connection.add(SUBJECT_5, PREDICATE_1, vf.createLiteral("sfiveponectwo"), CONTEXT_2);
        connection.add(SUBJECT_5, PREDICATE_2, vf.createLiteral("sfiveptwoctwo"), CONTEXT_2);
        connection.commit();
        // connection.close();
        // connection = repository.getConnection();
        // connection.setAutoCommit(false);
        // test querying
        assertQueryResult("sfourponecone", PREDICATE_1, SUBJECT_4);
        assertQueryResult("sfourptwocone", PREDICATE_2, SUBJECT_4);
        assertQueryResult("sfiveponecone", PREDICATE_1, SUBJECT_5);
        assertQueryResult("sfiveponectwo", PREDICATE_1, SUBJECT_5);
        assertQueryResult("sfiveptwoctwo", PREDICATE_2, SUBJECT_5);
        // blind test to see if this method works:
        assertNoQueryResult("johannesgrenzfurthner");
        // remove a context
        connection.clear((Resource) null);
        connection.commit();
        assertNoQueryResult("sfourponecone");
        assertNoQueryResult("sfourptwocone");
        assertNoQueryResult("sfiveponecone");
        assertQueryResult("sfiveponectwo", PREDICATE_1, SUBJECT_5);
        assertQueryResult("sfiveptwoctwo", PREDICATE_2, SUBJECT_5);
    }

    @Test
    public void testFuzzyQuery()
        throws MalformedQueryException, RepositoryException, QueryEvaluationException
    {
        // prepare the query
        // search for the term "one" with 80% fuzzyness
        StringBuilder buffer = new StringBuilder();
        buffer.append("SELECT ?Resource ?Score \n");
        buffer.append("WHERE { \n");
        buffer.append("  ?Resource <" + MATCHES + "> [\n ");
        buffer.append("                          <" + QUERY + "> \"one~0.8\";\n");
        buffer.append("                          <" + SCORE + "> ?Score ]. } ");
        String q = buffer.toString();

        // fire the query
        TupleQuery query = connection.prepareTupleQuery(q);
        // check the results
        try (TupleQueryResult result = query.evaluate()) {
            // check the results
            BindingSet bindings;

            // the first result is subject 1 and has a score
            int results = 0;
            Set<IRI> expectedSubject = new HashSet<>();
            expectedSubject.add(SUBJECT_1);
            expectedSubject.add(SUBJECT_2);
            expectedSubject.add(SUBJECT_3);
            while (result.hasNext()) {
                results++;
                bindings = result.next();

                // the resource should be among the set of expected subjects, if so,
                // remove it from the set
                assertTrue(expectedSubject.remove((IRI) bindings.getValue("Resource")));

                // there should be a score
                assertNotNull(bindings.getValue("Score"));
            }

            // there should have been 3 results
            assertEquals(3, results);
        }
    }

    /**
     * Checks if reindexing does not corrupt the new index and if complex query still is evaluated
     * properly.
     *
     */
    @Test
    public void testReindexing()
    {
        sail.reindex();
        testComplexQueryTwo();
    }

    @Test
    public void testPropertyVar()
        throws MalformedQueryException, RepositoryException, QueryEvaluationException
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("SELECT ?Resource ?Property \n");
        buffer.append("WHERE { \n");
        buffer.append("  ?Resource <" + MATCHES + "> [\n ");
        buffer.append("                          <" + QUERY + "> \"one\";\n");
        buffer.append("                          <" + PROPERTY + "> ?Property ]. } ");
        String q = buffer.toString();

        // fire the query
        TupleQuery query = connection.prepareTupleQuery(q);
        try (TupleQueryResult result = query.evaluate()) {
            int results = 0;
            Map<IRI, IRI> expectedSubject = new HashMap<>();
            expectedSubject.put(SUBJECT_1, PREDICATE_1);
            expectedSubject.put(SUBJECT_2, PREDICATE_1);
            expectedSubject.put(SUBJECT_3, PREDICATE_2);
            while (result.hasNext()) {
                results++;
                BindingSet bindings = result.next();

                // the resource should be among the set of expected subjects, if so,
                // remove it from the set
                Value subject = bindings.getValue("Resource");
                IRI expectedProperty = expectedSubject.remove(subject);
                assertEquals(expectedProperty, bindings.getValue("Property"),
                        "For subject " + subject);
            }

            // there should have been 3 results
            assertEquals(3, results);
        }
    }

    @Test
    public void testMultithreadedAdd() throws InterruptedException
    {
        int numThreads = 3;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(numThreads);
        final Set<Throwable> exceptions = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < numThreads; i++) {
            new Thread(new Runnable()
            {

                private final long iterationCount = 10 + Math.round(random.nextDouble() * 100);

                @Override
                public void run()
                {
                    try (RepositoryConnection con = repository.getConnection()) {
                        startLatch.await();
                        for (long i = 0; i < iterationCount; i++) {
                            con.add(vf.createIRI("ex:" + i), vf.createIRI("ex:prop" + i % 3),
                                    vf.createLiteral(i));
                        }
                    }
                    catch (Throwable e) {
                        exceptions.add(e);
                        throw new AssertionError(e);
                    }
                    finally {
                        endLatch.countDown();
                    }
                }
            }).start();
        }
        startLatch.countDown();
        endLatch.await();
        for (Throwable e : exceptions) {
            e.printStackTrace(System.err);
        }
        assertEquals(0, exceptions.size(),
                "Exceptions occurred during testMultithreadedAdd, see stacktraces above");
    }

    @Test
    public void testIndexWriterState()
    {
        final String brokenTrig = "{ broken }";
        RepositoryConnection conn = repository.getConnection();
        try (StringReader sr = new StringReader(brokenTrig)) {
            conn.add(sr, "http://example.org/", RDFFormat.TRIG);
        }
        catch (Exception e) {
            // expected parse exception
            LOG.debug("Parse exception: {}", e.getMessage());
        }
        conn.close();
        conn = repository.getConnection();
        conn.clear(); // make sure this can be executed multiple times
        conn.add(FOAF.PERSON, RDFS.LABEL, SimpleValueFactory.getInstance().createLiteral("abc"));
        conn.close();
    }

    protected void assertQueryResult(String literal, IRI predicate, Resource resultUri)
    {
        // fire a query for all subjects with a given term
        String queryString = "SELECT ?Resource " + "WHERE { ?Resource <" + MATCHES + "> [ " + " <"
                + QUERY + "> \"" + literal + "\" ]. } ";
        TupleQuery query = connection.prepareTupleQuery(queryString);
        try (TupleQueryResult result = query.evaluate()) {
            // check the result
            assertTrue(result.hasNext(), "query for literal '" + literal
                    + " did not return any results, expected was " + resultUri);
            BindingSet bindings = result.next();
            assertEquals(resultUri, bindings.getValue("Resource"),
                    "query for literal '" + literal + " did not return the expected resource");
            assertFalse(result.hasNext());
        }
    }

    protected void assertNoQueryResult(String literal)
    {
        // fire a query for all subjects with a given term
        String queryString = "SELECT ?Resource " + "WHERE { ?Resource <" + MATCHES + "> [ " + " <"
                + QUERY + "> \"" + literal + "\" ]. } ";
        TupleQuery query = connection.prepareTupleQuery(queryString);
        try (TupleQueryResult result = query.evaluate()) {
            // check the result
            assertFalse(result.hasNext(), "query for literal '" + literal
                    + " did return results, which was not expected.");
        }
    }

}
