/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.evaluation.TupleFunctionEvaluationMode;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class LuceneIndexIdFilteringTest
{

    private static final Logger LOG = LoggerFactory.getLogger(LuceneIndexIdFilteringTest.class);
    private static final ValueFactory VF = SimpleValueFactory.getInstance();
    private static final String NAMESPACE = "http://example.org/";
    private static final String PREFIXES = joinLines(
            "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>",
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
            "PREFIX ex: <" + NAMESPACE + ">");

    private static IRI iri(String name)
    {
        return VF.createIRI(NAMESPACE + name);
    }

    private static String joinLines(String... lines)
    {
        return String.join(" \n", lines);
    }

    LuceneSail sailType1, sailType2, sailType3;
    SailRepository repository;

    @BeforeEach
    public void setup(@TempDir File dataDir)
    {
        // sails schema
        // sailType1(LuceneSail) -> sailType2(LuceneSail) -> sailType3(LuceneSail) ->
        // memoryStore(MemoryStore)

        MemoryStore memoryStore = new MemoryStore();

        // sail with the ex:text3 filter
        sailType3 = new LuceneSail();
        sailType3.setParameter(LuceneSail.INDEXEDFIELDS, "index.1=" + NAMESPACE + "text3");
        sailType3.setParameter(LuceneSail.LUCENE_DIR_KEY, "lucene-index3");
        sailType3.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS);
        sailType3.setEvaluationMode(TupleFunctionEvaluationMode.TRIPLE_SOURCE);
        sailType3.setBaseSail(memoryStore);

        // sail with the ex:text2 filter
        sailType2 = new LuceneSail();
        sailType2.setParameter(LuceneSail.INDEXEDFIELDS, "index.1=" + NAMESPACE + "text2");
        sailType2.setParameter(LuceneSail.LUCENE_DIR_KEY, "lucene-index2");
        sailType2.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS);
        sailType2.setEvaluationMode(TupleFunctionEvaluationMode.NATIVE);
        sailType2.setBaseSail(sailType3);

        // sail with the ex:text1 filter
        sailType1 = new LuceneSail();
        sailType1.setParameter(LuceneSail.INDEXEDFIELDS, "index.1=" + NAMESPACE + "text1");
        sailType1.setParameter(LuceneSail.LUCENE_DIR_KEY, "lucene-index1");
        sailType1.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS);
        sailType1.setEvaluationMode(TupleFunctionEvaluationMode.NATIVE);
        sailType1.setBaseSail(sailType2);
        sailType1.setDataDir(dataDir);
    }

    private void initSails()
    {
        repository = new SailRepository(sailType1);
        repository.init();

        // add test elements
        add(VF.createStatement(iri("element1"), iri("text1"), VF.createLiteral("text")),
                VF.createStatement(iri("element2"), iri("text1"), VF.createLiteral("text")),
                VF.createStatement(iri("element2"), iri("text2"), VF.createLiteral("text")),
                VF.createStatement(iri("element3"), iri("text3"), VF.createLiteral("text")));
    }

    private void add(Statement... statements)
    {
        try (SailRepositoryConnection connection = repository.getConnection()) {
            for (Statement stmt : statements) {
                connection.add(stmt);
            }
        }
    }

    private void assertSearchQuery(String queryStr, String... exceptedElements)
    {
        // using a list for duplicates
        List<String> exceptedDocSet = Lists.newArrayList(exceptedElements);

        try (SailRepositoryConnection connection = repository.getConnection()) {
            // fire a query with the subject pre-specified
            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL,
                    PREFIXES + "\n" + queryStr);
            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    BindingSet set = result.next();
                    String element = set.getValue("result").stringValue()
                            .substring(NAMESPACE.length());
                    if (!exceptedDocSet.remove(element)) {
                        LOG.error("Docs: " + exceptedDocSet);
                        LOG.error("Remaining:");
                        while (result.hasNext()) {
                            set = result.next();
                            LOG.error("- {}", set.getValue("result").stringValue()
                                    .substring(NAMESPACE.length()));
                        }
                        fail("The element '" + element + "' was in the index, but wasn't excepted");
                    }
                }
            }

            if (!exceptedDocSet.isEmpty()) {
                fail("Unexpected docs: " + exceptedDocSet);
            }
        }
    }

    @Test
    public void noConfigTest()
    {
        // no config
        initSails();

        assertSearchQuery(joinLines("SELECT ?result {", "  ?result search:matches ?match .",
                "  ?match search:query 'text' .", "}"), "element1", "element2");
    }

    @Test
    public void idConfigTest()
    {
        sailType1.setParameter(LuceneSail.INDEX_ID, NAMESPACE + "lucene1");
        sailType2.setParameter(LuceneSail.INDEX_ID, NAMESPACE + "lucene2");
        sailType3.setParameter(LuceneSail.INDEX_ID, NAMESPACE + "lucene3");
        initSails();

        // try query on index 1
        assertSearchQuery(joinLines("SELECT ?result {", "  ?result search:matches ?match .",
                "  ?match search:indexid ex:lucene1 .", "  ?match search:query 'text' .", "}"),
                "element1", "element2");

        // try query on index 2
        assertSearchQuery(joinLines("SELECT ?result {", "  ?result search:matches ?match .",
                "  ?match search:indexid ex:lucene2 .", "  ?match search:query 'text' .", "}"),
                "element2");

        // try query on index 3
        assertSearchQuery(joinLines("SELECT ?result {", "  ?result search:matches ?match .",
                "  ?match search:indexid ex:lucene3 .", "  ?match search:query 'text' .", "}"),
                "element3");

        // try query on index 2 and 3
        assertSearchQuery(joinLines("SELECT ?result {", "  {",
                "    ?result search:matches ?match .", "    ?match search:indexid ex:lucene2 .",
                "    ?match search:query 'text' .", "  } UNION {",
                "    ?result search:matches ?match2 .", "    ?match2 search:indexid ex:lucene3 .",
                "    ?match2 search:query 'text' .", "  }", "}"), "element2", "element3");

        // try query on index 1 and 2
        assertSearchQuery(joinLines("SELECT ?result {", "  {",
                "    ?result search:matches ?match .", "    ?match search:indexid ex:lucene1 .",
                "    ?match search:query 'text' .", "  } UNION {",
                "    ?result search:matches ?match2 .", "    ?match2 search:indexid ex:lucene2 .",
                "    ?match2 search:query 'text' .", "  }", "}"), "element1", "element2",
                "element2");

        // try query on index 1, 2 and 3
        assertSearchQuery(joinLines("SELECT ?result {", "  {",
                "    ?result search:matches ?match .", "    ?match search:indexid ex:lucene2 .",
                "    ?match search:query 'text' .", "  } UNION {",
                "    ?result search:matches ?match2 .", "    ?match2 search:indexid ex:lucene3 .",
                "    ?match2 search:query 'text' .", "  } UNION {",
                "    ?result search:matches ?match3 .", "    ?match3 search:indexid ex:lucene1 .",
                "    ?match3 search:query 'text' .", "  }", "}"), "element1", "element2",
                "element2", "element3");
    }

    private void assertSearchQuery(String queryStr, boolean union, QueryElement... exceptedElements)
    {
        // using a list for duplicates
        try (SailRepositoryConnection connection = repository.getConnection()) {
            // fire a query with the subject pre-specified
            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL,
                    PREFIXES + "\n" + queryStr);
            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    BindingSet set = result.next();
                    Value elementValue = null;
                    for (QueryElement el : exceptedElements) {
                        // union lines shouldn't be containing multiple results
                        if (union && elementValue != null) {
                            assertNull(set.getValue(el.resultName),
                                    "union test returns multiple results");
                            continue;
                        }
                        elementValue = set.getValue(el.resultName);
                        if (elementValue == null) {
                            continue;
                        }

                        String element = elementValue.stringValue().substring(NAMESPACE.length());
                        if (!el.elements.remove(element)) {
                            LOG.error("Docs: " + el.elements);
                            LOG.error("Remaining:");
                            while (result.hasNext()) {
                                set = result.next();
                                LOG.error("- {}", set);
                            }
                            fail("The element '" + element + "' was in the index " + el.resultName
                                    + ", but wasn't excepted");
                        }
                    }
                    assertNotNull(elementValue, "No element for the set: " + set);
                }
            }

            List<QueryElement> missing = new ArrayList<>();

            // check for missing elements
            for (QueryElement el : exceptedElements) {
                if (!el.elements.isEmpty()) {
                    missing.add(el);
                }
            }

            if (!missing.isEmpty()) {
                fail("Unexpected docs: " + missing);
            }
        }
    }

    private void assertUnionSearchQuery(String queryStr, QueryElement... exceptedElements)
    {
        assertSearchQuery(queryStr, true, exceptedElements);
    }

    private void assertJoinSearchQuery(String queryStr, QueryElement... exceptedElements)
    {
        assertSearchQuery(queryStr, false, exceptedElements);
    }

    @Test
    public void idConfigUnionTest()
    {
        sailType1.setParameter(LuceneSail.INDEX_ID, NAMESPACE + "lucene1");
        sailType2.setParameter(LuceneSail.INDEX_ID, NAMESPACE + "lucene2");
        sailType3.setParameter(LuceneSail.INDEX_ID, NAMESPACE + "lucene3");
        initSails();

        // try query on index 2 and 3
        assertUnionSearchQuery(joinLines("SELECT ?result ?result2 {", "  {",
                "    ?result search:matches ?match .", "    ?match search:indexid ex:lucene2 .",
                "    ?match search:query 'text' .", "  } UNION {",
                "    ?result2 search:matches ?match2 .", "    ?match2 search:indexid ex:lucene3 .",
                "    ?match2 search:query 'text' .", "  }", "}"),
                new QueryElement("result", "element2"), new QueryElement("result2", "element3"));

        // try query on index 1 and 2
        assertUnionSearchQuery(joinLines("SELECT ?result ?result2 {", "  {",
                "    ?result search:matches ?match .", "    ?match search:indexid ex:lucene1 .",
                "    ?match search:query 'text' .", "  } UNION {",
                "    ?result2 search:matches ?match2 .", "    ?match2 search:indexid ex:lucene2 .",
                "    ?match2 search:query 'text' .", "  }", "}"),
                new QueryElement("result", "element1", "element2"),
                new QueryElement("result2", "element2"));

        // try query on index 1, 2 and 3
        assertUnionSearchQuery(joinLines("SELECT ?result ?result2 ?result3 {", "  {",
                "    ?result search:matches ?match .", "    ?match search:indexid ex:lucene1 .",
                "    ?match search:query 'text' .", "  } UNION {",
                "    ?result2 search:matches ?match2 .", "    ?match2 search:indexid ex:lucene2 .",
                "    ?match2 search:query 'text' .", "  } UNION {",
                "    ?result3 search:matches ?match3 .", "    ?match3 search:indexid ex:lucene3 .",
                "    ?match3 search:query 'text' .", "  }", "}"),
                new QueryElement("result", "element1", "element2"),
                new QueryElement("result2", "element2"), new QueryElement("result3", "element3"));
    }

    @Test
    public void idConfigJoinTest()
    {
        sailType1.setParameter(LuceneSail.INDEX_ID, NAMESPACE + "lucene1");
        sailType2.setParameter(LuceneSail.INDEX_ID, NAMESPACE + "lucene2");
        sailType3.setParameter(LuceneSail.INDEX_ID, NAMESPACE + "lucene3");
        initSails();
        add(VF.createStatement(iri("element4"), iri("text2"), VF.createLiteral("text")),
                VF.createStatement(iri("element5"), iri("text3"), VF.createLiteral("text")),
                VF.createStatement(iri("element4"), iri("friend"), iri("element5")));

        // try query on index 2 and 3
        assertJoinSearchQuery(joinLines("SELECT ?result1 ?result2 {", "  {",
                "    ?result1 search:matches ?match .", "    ?match search:indexid ex:lucene2 .",
                "    ?match search:query 'text' .", "", "    ?result2 search:matches ?match2 .",
                "    ?match2 search:indexid ex:lucene3 .", "    ?match2 search:query 'text' .",
                "  }", "}"),
                // twice the elements because we are doing result1 x result2
                new QueryElement("result1", "element2", "element4", "element2", "element4"),
                new QueryElement("result2", "element3", "element5", "element3", "element5"));

        // try query on index 2 and 3 with a join on the result
        assertJoinSearchQuery(joinLines("SELECT ?result1 ?result2 {", "  {",
                "    ?result1 search:matches ?match .", "    ?match search:indexid ex:lucene2 .",
                "    ?match search:query 'text' .", "", "    ?result2 search:matches ?match2 .",
                "    ?match2 search:indexid ex:lucene3 .", "    ?match2 search:query 'text' .", "",
                "    ?result1 ex:friend ?result2 .", "  }", "}"),
                new QueryElement("result1", "element4"), new QueryElement("result2", "element5"));
    }

    static class QueryElement
    {

        List<String> elements;
        String resultName;

        /**
         * a result element of a query, shouldn't be used twice
         *
         * @param resultName
         *            the result object to use
         * @param exceptedElements
         *            the excepted element
         */
        QueryElement(String resultName, String... exceptedElements)
        {
            this.resultName = resultName;
            this.elements = Lists.newArrayList(exceptedElements);
        }

        @Override
        public String toString()
        {
            return elements + ": " + resultName;
        }
    }
}
