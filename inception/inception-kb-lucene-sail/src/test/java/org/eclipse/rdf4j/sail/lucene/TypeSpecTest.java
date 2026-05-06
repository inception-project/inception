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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.Set;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class TypeSpecTest
{

    private static final Logger LOG = LoggerFactory.getLogger(TypeSpecTest.class);
    private static final ValueFactory VF = SimpleValueFactory.getInstance();
    private static final String EX_NS = "http://example.org/";
    private static final String PREDICATE_TYPEOF = EX_NS + "typeof";
    private static final String PREDICATE_TEXT = EX_NS + "text";

    private static Statement typeof(String subject)
    {
        return VF.createStatement(VF.createIRI(EX_NS + subject), VF.createIRI(PREDICATE_TYPEOF),
                VF.createIRI(EX_NS + "type1"));
    }

    private static Statement typeRDF(String subject)
    {
        return VF.createStatement(VF.createIRI(EX_NS + subject), RDF.TYPE,
                VF.createIRI(EX_NS + "type2"));
    }

    private static Statement literal(String subject, String value)
    {
        return VF.createStatement(VF.createIRI(EX_NS + subject), VF.createIRI(PREDICATE_TEXT),
                VF.createLiteral(value));
    }

    LuceneSail sail;
    MemoryStore memoryStore;
    SailRepository repository;
    @TempDir
    File dataDir;

    @BeforeEach
    public void setup()
    {
        memoryStore = new MemoryStore();
        // enable lock tracking
        sail = new LuceneSail();
        sail.setParameter(LuceneSail.LUCENE_DIR_KEY, "lucene-index");
        sail.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS);
    }

    private void initSail()
    {
        sail.setBaseSail(memoryStore);
        repository = new SailRepository(sail);
        repository.setDataDir(dataDir);
        repository.init();
    }

    private void add(Statement... statements)
    {
        try (SailRepositoryConnection connection = repository.getConnection()) {
            connection.begin();
            for (Statement s : statements) {
                connection.add(s);
            }
            connection.commit();
        }
    }

    private void remove(Statement... statements)
    {
        try (SailRepositoryConnection connection = repository.getConnection()) {
            connection.begin();
            for (Statement s : statements) {
                connection.remove(s);
            }
            connection.commit();
        }
    }

    private void addRemove(Statement[] toAdd, Statement[] toRemove)
    {
        try (SailRepositoryConnection connection = repository.getConnection()) {
            connection.begin();
            for (Statement s : toAdd) {
                connection.add(s);
            }
            for (Statement s : toRemove) {
                connection.remove(s);
            }
            connection.commit();
        }
    }

    /**
     * assert the repository only contains the excepted documents
     *
     * @param exceptedDocuments
     *            the excepted documents
     */
    private void assertQuery(String... exceptedDocuments)
    {
        try (SailRepositoryConnection connection = repository.getConnection()) {
            String queryStr = "";
            queryStr += "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#> ";
            queryStr += "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ";
            queryStr += "SELECT DISTINCT ?result { ";
            queryStr += "  ?result search:matches ?match . ";
            queryStr += "  ?match search:query 'text' . }";

            Set<String> exceptedDocSet = Sets.newHashSet(exceptedDocuments);

            // fire a query with the subject pre-specified
            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    BindingSet set = result.next();
                    String element = set.getValue("result").stringValue().substring(EX_NS.length());
                    if (!exceptedDocSet.remove(element)) {
                        LOG.error("Docs: " + exceptedDocSet);
                        LOG.error("Remaining:");
                        while (result.hasNext()) {
                            set = result.next();
                            LOG.error("- {}",
                                    set.getValue("result").stringValue().substring(EX_NS.length()));
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
        // no config to add
        initSail();

        // initial data
        add(literal("aaa", "text aaa"), literal("bbb", "text bbb"), literal("ccc", "text ccc"),
                typeof("bbb"), typeof("eee"));

        assertQuery("aaa", "bbb", "ccc");

        // test backtrace of add(aaa, typeof, type1) -> (aaa, text, "text aaa") add
        add(typeof("aaa"));

        assertQuery("aaa", "bbb", "ccc");

        // test backtrace of remove(bbb, typeof, type1) -> (bbb, text, "text bbb") removed
        remove(typeof("bbb"));

        assertQuery("aaa", "bbb", "ccc");

        // test add without calling sail (ddd, text, "text ddd") add
        add(typeof("ddd"), literal("ddd", "text ddd"));

        assertQuery("aaa", "bbb", "ccc", "ddd");

        // test add with calling sail (eee, text, "text eee") add
        add(literal("eee", "text eee"));

        assertQuery("aaa", "bbb", "ccc", "ddd", "eee");

        // test adding and remove typeof in the same addRemove (eee, text, "text eee") shouldn't be
        // added
        addRemove(
                // add
                new Statement[] { typeof("fff"), literal("fff", "text fff") },
                // remove
                new Statement[] { typeof("fff") });

        assertQuery("aaa", "bbb", "ccc", "ddd", "eee", "fff");

        remove(literal("aaa", "text aaa"));

        assertQuery("bbb", "ccc", "ddd", "eee", "fff");
    }

    @Test
    public void typeTest()
    {
        sail.setParameter(LuceneSail.INDEXEDTYPES,
                (PREDICATE_TYPEOF + "=" + EX_NS + "type1").replaceAll("[:]", "\\\\:"));
        initSail();

        // initial data
        add(literal("aaa", "text aaa"), literal("bbb", "text bbb"), literal("ccc", "text ccc"),
                typeof("bbb"), typeof("eee"));

        assertQuery("bbb");

        // test backtrace of add(aaa, typeof, type1) -> (aaa, text, "text aaa") add
        add(typeof("aaa"));

        assertQuery("aaa", "bbb");

        // test backtrace of remove(bbb, typeof, type1) -> (bbb, text, "text bbb") removed
        remove(typeof("bbb"));

        assertQuery("aaa");

        // test add without calling sail (ddd, text, "text ddd") add
        add(typeof("ddd"), literal("ddd", "text ddd"));

        assertQuery("aaa", "ddd");

        // test add with calling sail (eee, text, "text eee") add
        add(literal("eee", "text eee"));

        assertQuery("aaa", "ddd", "eee");

        // test adding and remove typeof in the same addRemove (eee, text, "text eee") shouldn't be
        // added
        addRemove(
                // add
                new Statement[] { typeof("fff"), literal("fff", "text fff") },
                // remove
                new Statement[] { typeof("fff") });

        assertQuery("aaa", "ddd", "eee");

        remove(literal("aaa", "text aaa"));

        assertQuery("ddd", "eee");
    }

    @Test
    public void typePartialModeTest()
    {
        sail.setParameter(LuceneSail.INDEXEDTYPES,
                (PREDICATE_TYPEOF + "=" + EX_NS + "type1").replaceAll("[:]", "\\\\:"));
        sail.setIndexBacktraceMode(TypeBacktraceMode.PARTIAL);
        initSail();

        // initial data
        add(literal("aaa", "text aaa"), literal("bbb", "text bbb"), literal("ccc", "text ccc"),
                typeof("bbb"), typeof("eee"));

        assertQuery("bbb");

        // test backtrace of add(aaa, typeof, type1) -> (aaa, text, "text aaa") add
        add(typeof("aaa"));

        assertQuery("bbb");

        // test backtrace of remove(bbb, typeof, type1) -> (bbb, text, "text bbb") removed
        remove(typeof("bbb"));

        assertQuery("bbb");

        // test add without calling sail (ddd, text, "text ddd") add
        add(typeof("ddd"), literal("ddd", "text ddd"));

        assertQuery("bbb", "ddd");

        // test add with calling sail (eee, text, "text eee") add
        add(literal("eee", "text eee"));

        assertQuery("bbb", "ddd", "eee");

        // test adding and remove typeof in the same addRemove (eee, text, "text eee") shouldn't be
        // added
        addRemove(
                // add
                new Statement[] { typeof("fff"), literal("fff", "text fff") },
                // remove
                new Statement[] { typeof("fff") });

        assertQuery("bbb", "ddd", "eee");

        remove(literal("aaa", "text aaa"));

        assertQuery("bbb", "ddd", "eee");
    }

    @Test
    public void typeRDFTest()
    {
        sail.setParameter(LuceneSail.INDEXEDTYPES,
                ("a=" + EX_NS + "type2").replaceAll("[:]", "\\\\:"));
        initSail();

        // initial data
        add(literal("aaa", "text aaa"), literal("bbb", "text bbb"), literal("ccc", "text ccc"),
                typeRDF("bbb"), typeRDF("eee"));

        assertQuery("bbb");

        // test backtrace of add(aaa, typeof, type1) -> (aaa, text, "text aaa") add
        add(typeRDF("aaa"));

        assertQuery("aaa", "bbb");

        // test backtrace of remove(bbb, typeof, type1) -> (bbb, text, "text bbb") removed
        remove(typeRDF("bbb"));

        assertQuery("aaa");

        // test add without calling sail (ddd, text, "text ddd") add
        add(typeRDF("ddd"), literal("ddd", "text ddd"));

        assertQuery("aaa", "ddd");

        // test add with calling sail (eee, text, "text eee") add
        add(literal("eee", "text eee"));

        assertQuery("aaa", "ddd", "eee");

        // test adding and remove typeof in the same addRemove (eee, text, "text eee") shouldn't be
        // added
        addRemove(
                // add
                new Statement[] { typeRDF("fff"), literal("fff", "text fff") },
                // remove
                new Statement[] { typeRDF("fff") });

        assertQuery("aaa", "ddd", "eee");

        remove(literal("aaa", "text aaa"));

        assertQuery("ddd", "eee");
    }
}
