/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.IntStream;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This unit test reproduces issue #41
 *
 * @author Jacek Grzebyta
 */
public class LuceneIndexLocationTest
{

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String luceneIndexPath = "sail-index";

    Sail sail;

    SailRepository repository;

    RepositoryConnection connection;

    private final ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * Set up memory storage located within temporary folder
     *
     */
    @BeforeEach
    public void setUp(@TempDir File dataDir)
    {
        sail = new MemoryStore();

        LuceneSail lucene = new LuceneSail();
        lucene.setBaseSail(sail);
        lucene.setParameter(LuceneSail.LUCENE_DIR_KEY, luceneIndexPath);
        lucene.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS);

        repository = new SailRepository(lucene);
        repository.setDataDir(dataDir);

        try ( // create temporary transaction to load data
                SailRepositoryConnection cnx = repository.getConnection()) {
            cnx.begin();

            IntStream.rangeClosed(0, 50)
                    .forEach(i -> cnx.add(vf.createStatement(vf.createIRI("urn:subject" + i),
                            vf.createIRI("urn:predicate:" + i), vf.createLiteral("Value" + i))));
            cnx.commit();
        }
        connection = repository.getConnection();
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
    }

    /**
     * Check Lucene index location
     *
     */
    @Test
    public void IndexLocationTest()
    {
        File dataDir = repository.getDataDir();
        Path lucenePath = repository.getDataDir().toPath().resolve(luceneIndexPath);

        log.info("Lucene index location: {}", lucenePath);
        assertEquals(dataDir.getAbsolutePath() + File.separator + luceneIndexPath,
                lucenePath.toAbsolutePath().toString());

        assertTrue(lucenePath.toFile().exists());
        assertTrue(lucenePath.toFile().isDirectory());
    }
}
