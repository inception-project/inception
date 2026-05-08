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

import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.evaluation.TupleFunctionEvaluationMode;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MultiParamTest
{
    private static final String NAMESPACE = "http://example.org/";
    private static final String PREFIXES = joinLines(
            "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>",
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
            "PREFIX ex: <" + NAMESPACE + ">");

    private static IRI iri(String name)
    {
        return Values.iri(NAMESPACE + name);
    }

    private static String joinLines(String... lines)
    {
        return String.join(" \n", lines);
    }

    private static final IRI elem1 = iri("elem1");
    private static final IRI elem2 = iri("elem2");
    private static final IRI elem3 = iri("elem3");
    private static final IRI elem4 = iri("elem4");
    private static final IRI elem5 = iri("elem5");
    private static final IRI elem6 = iri("elem6");
    private static final IRI elem7 = iri("elem7");

    private static final IRI p1 = iri("p1");
    private static final IRI p2 = iri("p2");
    private static final IRI p3 = iri("p3");

    @TempDir
    Path tmpFolder;

    LuceneSail luceneSail;
    SailRepository repository;
    SailRepositoryConnection conn;

    @BeforeEach
    public void setup() throws IOException
    {
        MemoryStore memoryStore = new MemoryStore();
        // sail with the ex:text1 filter
        luceneSail = new LuceneSail();
        luceneSail.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS);
        luceneSail.setEvaluationMode(TupleFunctionEvaluationMode.NATIVE);
        luceneSail.setBaseSail(memoryStore);
        luceneSail.setDataDir(tmpFolder.toFile());
        repository = new SailRepository(luceneSail);
        repository.init();

        // add test elements
        conn = repository.getConnection();
        conn.begin();

        conn.add(elem1, p1, literal("aaa"));
        conn.add(elem1, p2, literal("bbb"));
        conn.add(elem1, p3, literal("ccc"));

        conn.add(elem2, p1, literal("aaa"));
        conn.add(elem2, p2, literal("ddd"));
        conn.add(elem2, p3, literal("ccc"));

        conn.add(elem3, p1, literal("ddd"));
        conn.add(elem3, p2, literal("bbb"));
        conn.add(elem3, p3, literal("fff"));

        conn.add(elem4, p1, literal("ddd"));
        conn.add(elem4, p2, literal("ggg"));
        conn.add(elem4, p3, literal("ccc"));

        conn.add(elem5, p1, literal("hhh"));
        conn.add(elem5, p2, literal("eee"));
        conn.add(elem5, p3, literal("aaa"));

        conn.add(elem6, p1, literal("iii zzz yyy"));
        conn.add(elem6, p2, literal("jjj zzz"));
        conn.add(elem6, p3, literal("kkk"));

        conn.add(elem7, p1, literal("iii zzz"));
        conn.add(elem7, p2, literal("jjj zzz yyy"));
        conn.add(elem7, p3, literal("kkk"));

        conn.commit();
    }

    @AfterEach
    public void complete()
    {
        try {
            conn.close();
        }
        finally {
            repository.shutDown();
        }
    }

    @Test
    public void testPredicateSimple()
    {
        try (TupleQueryResult result = conn.prepareTupleQuery(joinLines(PREFIXES, "SELECT * {",
                "  ?subj search:matches [", "    search:query \"aaa\"", "  ]", "}")).evaluate()) {
            Set<String> values = new HashSet<>(
                    Set.of(elem1.toString(), elem2.toString(), elem5.toString()));

            while (result.hasNext()) {
                Value next = result.next().getValue("subj");
                assertTrue(values.remove(next.toString()), "unknown value: " + next);
            }
            assertTrue(values.isEmpty(), "missing value" + values);
        }
    }

    @Test
    public void testPredicateMulti()
    {
        try (TupleQueryResult result = conn
                .prepareTupleQuery(joinLines(PREFIXES, "SELECT * {", "  ?subj search:matches [",
                        "    search:query [ ", "       search:query \"aaa\"", "    ]", "  ]", "}"))
                .evaluate()) {
            Set<String> values = new HashSet<>(
                    Set.of(elem1.toString(), elem2.toString(), elem5.toString()));

            while (result.hasNext()) {
                Value next = result.next().getValue("subj");
                assertTrue(values.remove(next.toString()), "unknown value: " + next);
            }
            assertTrue(values.isEmpty(), "missing value" + values);
        }
    }

    @Test
    public void testMultiPredicate()
    {
        try (TupleQueryResult result = conn.prepareTupleQuery(joinLines(PREFIXES, "SELECT * {",
                "  ?subj search:matches [", "    search:query [ ", "       search:query \"aaa\" ;",
                "       search:property ex:p1", "    ]", "  ]", "}")).evaluate()) {
            Set<String> values = new HashSet<>(Set.of(elem1.toString(), elem2.toString()));

            while (result.hasNext()) {
                Value next = result.next().getValue("subj");
                assertTrue(values.remove(next.toString()), "unknown value: " + next);
            }
            assertTrue(values.isEmpty(), "missing value" + values);
        }
    }

    @Test
    public void testMultiQuery()
    {
        try (TupleQueryResult result = conn.prepareTupleQuery(
                joinLines(PREFIXES, "SELECT * {", "  ?subj search:matches [", "    search:query",
                        "    [", "       search:query \"aaa\" ;", "       search:property ex:p1",
                        "    ] , [", "       search:query \"bbb\" ;",
                        "       search:property ex:p2", "    ]", "  ]", "}"))
                .evaluate()) {
            Set<String> values = new HashSet<>(
                    Set.of(elem1.toString(), elem2.toString(), elem3.toString()));

            while (result.hasNext()) {
                BindingSet binding = result.next();
                Value next = binding.getValue("subj");
                assertTrue(values.remove(next.toString()), "unknown value: " + next);
            }
            assertTrue(values.isEmpty(), "missing value" + values);
        }
    }

    @Test
    public void testMultiSnippetQuery()
    {
        try (TupleQueryResult result = conn.prepareTupleQuery(
                joinLines(PREFIXES, "SELECT * {", "  ?subj search:matches [", "    search:query",
                        "    [", "       search:query \"aaa\" ;", "       search:property ex:p1 ;",
                        "       search:snippet ?sp1 ;", "    ] , [",
                        "       search:query \"bbb\" ;", "       search:property ex:p2 ;",
                        "       search:snippet ?sp2 ;", "    ]", "  ]", "}"))
                .evaluate()) {
            Set<String> values = new HashSet<>(Set.of(elem1 + ":\"<B>aaa</B>\":\"<B>bbb</B>\"",
                    elem2 + ":\"<B>aaa</B>\":null", elem3 + ":null:\"<B>bbb</B>\""));

            while (result.hasNext()) {
                BindingSet bindings = result.next();
                Value next = bindings.getValue("subj");
                Value snippet1 = bindings.getValue("sp1");
                Value snippet2 = bindings.getValue("sp2");
                String obj = next + ":" + snippet1 + ":" + snippet2;
                assertTrue(values.remove(obj), "unknown value: " + obj);
            }
            assertTrue(values.isEmpty(), "missing value" + values);
        }
    }

    @Test
    public void testMultiOrderQuery()
    {
        try (TupleQueryResult result = conn.prepareTupleQuery(
                joinLines(PREFIXES, "SELECT * {", "  ?subj search:matches [", "    search:query",
                        "    [", "       search:query \"iii\" ;", "       search:property ex:p1 ;",
                        "       search:boost 0.2 ;", "    ] , [", "       search:query \"jjj\" ;",
                        "       search:property ex:p2 ;", "       search:boost 0.8 ;", "    ] ;",
                        "    search:score ?score", "  ]", "}"))
                .evaluate()) {
            String[] values = new String[] { elem6.toString(), elem7.toString() };
            Iterator<String> it = Arrays.stream(values).iterator();

            while (result.hasNext()) {
                if (!it.hasNext()) {
                    do {
                        System.out.println(result.next());
                    }
                    while (result.hasNext());
                    fail("too many binding");
                }
                BindingSet bindings = result.next();
                String exceptedValue = it.next();
                Value next = bindings.getValue("subj");
                assertEquals(exceptedValue, next.toString());
            }
            if (it.hasNext()) {
                do {
                    System.out.println(it.next());
                }
                while (it.hasNext());
                fail();
            }
        }
        try (TupleQueryResult result = conn.prepareTupleQuery(
                joinLines(PREFIXES, "SELECT * {", "  ?subj search:matches [", "    search:query",
                        "    [", "       search:query \"iii\" ;", "       search:property ex:p1 ;",
                        "       search:boost 0.8 ;", "    ] , [", "       search:query \"jjj\" ;",
                        "       search:property ex:p2 ;", "       search:boost 0.2 ;", "    ] ;",
                        "    search:score ?score", "  ]", "}"))
                .evaluate()) {
            String[] values = new String[] { elem7.toString(), elem6.toString() };
            Iterator<String> it = Arrays.stream(values).iterator();

            while (result.hasNext()) {
                if (!it.hasNext()) {
                    do {
                        System.out.println(result.next());
                    }
                    while (result.hasNext());
                    fail("too many binding");
                }
                BindingSet bindings = result.next();
                String exceptedValue = it.next();
                Value next = bindings.getValue("subj");
                assertEquals(exceptedValue, next.toString());
            }
            if (it.hasNext()) {
                do {
                    System.out.println(it.next());
                }
                while (it.hasNext());
                fail();
            }
        }
    }

    @Test
    public void testMultiOrderSnippetQuery()
    {
        try (TupleQueryResult result = conn.prepareTupleQuery(
                joinLines(PREFIXES, "SELECT * {", "  ?subj search:matches [", "    search:query",
                        "    [", "       search:query \"iii\" ;", "       search:property ex:p1 ;",
                        "       search:boost 0.2 ;", "       search:snippet ?sp1 ;", "    ] , [",
                        "       search:query \"jjj\" ;", "       search:property ex:p2 ;",
                        "       search:boost 0.8 ;", "       search:snippet ?sp2 ;", "    ] ;",
                        "    search:score ?score", "  ]", "}"))
                .evaluate()) {
            String[] values = new String[] { elem6 + ":<B>iii</B> zzz yyy:<B>jjj</B> zzz",
                    elem7 + ":<B>iii</B> zzz:<B>jjj</B> zzz yyy" };
            Iterator<String> it = Arrays.stream(values).iterator();

            while (result.hasNext()) {
                if (!it.hasNext()) {
                    do {
                        System.out.println(result.next());
                    }
                    while (result.hasNext());
                    fail("too many binding");
                }
                String exceptedValue = it.next();
                BindingSet bindings = result.next();
                Value snippetValue1 = bindings.getValue("sp1");
                String snippet1 = snippetValue1 == null ? "" : snippetValue1.stringValue();
                Value snippetValue2 = bindings.getValue("sp2");
                String snippet2 = snippetValue2 == null ? "" : snippetValue2.stringValue();
                Value next = bindings.getValue("subj");
                String actualValue = next + ":" + snippet1 + ":" + snippet2;
                assertEquals(exceptedValue, actualValue);
            }
            if (it.hasNext()) {
                do {
                    System.out.println(it.next());
                }
                while (it.hasNext());
                fail();
            }
        }
        try (TupleQueryResult result = conn.prepareTupleQuery(
                joinLines(PREFIXES, "SELECT * {", "  ?subj search:matches [", "    search:query",
                        "    [", "       search:query \"iii\" ;", "       search:property ex:p1 ;",
                        "       search:boost 0.8 ;", "       search:snippet ?sp1 ;", "    ] , [",
                        "       search:query \"jjj\" ;", "       search:property ex:p2 ;",
                        "       search:boost 0.2 ;", "       search:snippet ?sp2 ;", "    ] ;",
                        "    search:score ?score", "  ]", "}"))
                .evaluate()) {
            String[] values = new String[] { elem7 + ":<B>iii</B> zzz:<B>jjj</B> zzz yyy",
                    elem6 + ":<B>iii</B> zzz yyy:<B>jjj</B> zzz" };
            Iterator<String> it = Arrays.stream(values).iterator();

            while (result.hasNext()) {
                if (!it.hasNext()) {
                    do {
                        System.out.println(result.next());
                    }
                    while (result.hasNext());
                    fail("too many binding");
                }
                BindingSet bindings = result.next();
                String exceptedValue = it.next();
                Value snippetValue1 = bindings.getValue("sp1");
                String snippet1 = snippetValue1 == null ? "" : snippetValue1.stringValue();
                Value snippetValue2 = bindings.getValue("sp2");
                String snippet2 = snippetValue2 == null ? "" : snippetValue2.stringValue();
                Value next = bindings.getValue("subj");
                String actualValue = next + ":" + snippet1 + ":" + snippet2;
                assertEquals(exceptedValue, actualValue);
            }
            if (it.hasNext()) {
                do {
                    System.out.println(it.next());
                }
                while (it.hasNext());
                fail();
            }
        }
    }
}
