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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;
import static org.eclipse.rdf4j.sail.lucene.LuceneSail.FUZZY_PREFIX_LENGTH_KEY;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LuceneFuzzinessPrefixTest
{
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

    private LuceneSail sail;
    private MemoryStore memoryStore;
    private SailRepository repository;
    @TempDir
    private File dataDir;

    @BeforeEach
    public void setup()
    {
        memoryStore = new MemoryStore();
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

        add(VF.createStatement(iri("element1"), iri("text"), VF.createLiteral("eclipse")),
                VF.createStatement(iri("element2"), iri("text"), VF.createLiteral("foundation")),
                VF.createStatement(iri("element3"), iri("text"), VF.createLiteral("ide")));
    }

    private void add(Statement... statements)
    {
        try (SailRepositoryConnection connection = repository.getConnection()) {
            for (Statement stmt : statements) {
                connection.add(stmt);
            }
        }
    }

    @Test
    public void testFuzzinessPrefixLength_default()
    {
        // Arrange
        initSail();

        // Act
        List<String> results = executeQuery();

        // Assert
        assertThat(results).containsExactlyInAnyOrder("element1");
    }

    @Test
    public void testFuzzinessPrefixLength_custom()
    {
        // Arrange
        sail.setParameter(FUZZY_PREFIX_LENGTH_KEY, "1");
        initSail();

        // Act
        List<String> results = executeQuery();

        // Assert
        assertThat(results).containsExactlyInAnyOrder("element1");
    }

    @Test
    public void testFuzzinessPrefixLength_custom_shouldExcludeResult()
    {
        // Arrange
        sail.setParameter(FUZZY_PREFIX_LENGTH_KEY, "2");
        initSail();

        // Act
        List<String> results = executeQuery();

        // Assert
        assertThat(results).isEmpty();
    }

    private List<String> executeQuery()
    {
        List<String> results = new ArrayList<>();
        try (SailRepositoryConnection connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(SPARQL,
                    PREFIXES + "\n"
                            + joinLines("SELECT ?result {", "  ?result search:matches ?match .",
                                    "  ?match search:query 'eXlipse~1' .", "}"));

            try (TupleQueryResult result = query.evaluate()) {
                for (BindingSet set : result) {
                    String element = set.getValue("result").stringValue()
                            .substring(NAMESPACE.length());
                    results.add(element);
                }
            }
        }
        return results;
    }
}
