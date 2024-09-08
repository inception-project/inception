/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.examples;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.LuceneSailSchema;
import org.eclipse.rdf4j.sail.lucene.impl.LuceneIndex;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * Example code showing how to use the LuceneSail
 *
 * @author sauermann
 */
public class LuceneSailExample
{

    /**
     * Create a lucene sail and use it
     *
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        createSimple();
    }

    /**
     * Create a LuceneSail and add some triples to it, ask a query.
     */
    public static void createSimple() throws Exception
    {
        // create a sesame memory sail
        MemoryStore memoryStore = new MemoryStore();

        // create a lucenesail to wrap the memorystore
        LuceneSail lucenesail = new LuceneSail();
        lucenesail.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneIndex.class.getName());
        // set this parameter to let the lucene index store its data in ram
        lucenesail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
        // set this parameter to store the lucene index on disk
        // lucenesail.setParameter(LuceneSail.LUCENE_DIR_KEY,
        // "./data/mydirectory");

        // wrap memorystore in a lucenesail
        lucenesail.setBaseSail(memoryStore);

        // create a Repository to access the sails
        SailRepository repository = new SailRepository(lucenesail);

        try ( // add some test data, the FOAF ont
                SailRepositoryConnection connection = repository.getConnection()) {
            connection.begin();
            connection.add(LuceneSailExample.class.getResourceAsStream(
                    "/org/openrdf/sail/lucene/examples/foaf.rdfs"), "", RDFFormat.RDFXML);
            connection.commit();

            // search for resources that mention "person"
            String queryString = "PREFIX search:   <" + LuceneSailSchema.NAMESPACE + "> \n"
                    + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
                    + "SELECT * WHERE { \n" + "?subject search:matches ?match . \n"
                    + "?match search:query \"person\" ; \n"
                    + "       search:property ?property ; \n" + "       search:score ?score ; \n"
                    + "       search:snippet ?snippet . \n" + "?subject rdf:type ?type . \n"
                    + "} LIMIT 3 \n" + "BINDINGS ?type { \n"
                    + " (<http://www.w3.org/2002/07/owl#Class>) \n" + "}";
            tupleQuery(queryString, connection);

            // search for property "name" with domain "person"
            queryString = "PREFIX search: <" + LuceneSailSchema.NAMESPACE + "> \n"
                    + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
                    + "SELECT * WHERE { \n" + "?subject rdfs:domain ?domain . \n"
                    + "?subject search:matches ?match . \n" + "?match search:query \"chat\" ; \n"
                    + "       search:score ?score . \n" + "?domain search:matches ?match2 . \n"
                    + "?match2 search:query \"person\" ; \n" + "        search:score ?score2 . \n"
                    + "} LIMIT 5";
            tupleQuery(queryString, connection);

            // search in subquery and filter results
            queryString = "PREFIX search:   <" + LuceneSailSchema.NAMESPACE + "> \n"
                    + "SELECT * WHERE { \n" + "{ SELECT * WHERE { \n"
                    + "  ?subject search:matches ?match . \n"
                    + "  ?match search:query \"person\" ; \n"
                    + "         search:property ?property ; \n"
                    + "         search:score ?score ; \n" + "         search:snippet ?snippet . \n"
                    + "} } \n" + "FILTER(CONTAINS(STR(?subject), \"Person\")) \n" + "} \n" + "";
            tupleQuery(queryString, connection);

            // search for property "homepage" with domain foaf:Person
            queryString = "PREFIX search: <" + LuceneSailSchema.NAMESPACE + "> \n"
                    + "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
                    + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
                    + "CONSTRUCT { ?x rdfs:domain foaf:Person } \n" + "WHERE { \n"
                    + "?x rdfs:domain foaf:Person . \n" + "?x search:matches ?match . \n"
                    + "?match search:query \"homepage\" ; \n"
                    + "       search:property ?property ; \n" + "       search:score ?score ; \n"
                    + "       search:snippet ?snippet . \n" + "} LIMIT 3 \n";
            graphQuery(queryString, connection);
        }
        finally {
            repository.shutDown();
        }
    }

    private static void tupleQuery(String queryString, RepositoryConnection connection)
        throws QueryEvaluationException, RepositoryException, MalformedQueryException
    {
        System.out.println("Running query: \n" + queryString);
        TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        try (TupleQueryResult result = query.evaluate()) {
            // print the results
            System.out.println("Query results:");
            while (result.hasNext()) {
                BindingSet bindings = result.next();
                System.out.println("found match: ");
                for (Binding binding : bindings) {
                    System.out.println("\t" + binding.getName() + ": " + binding.getValue());
                }
            }
        }
    }

    private static void graphQuery(String queryString, RepositoryConnection connection)
        throws RepositoryException, MalformedQueryException, QueryEvaluationException
    {
        System.out.println("Running query: \n" + queryString);
        GraphQuery query = connection.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        try (GraphQueryResult result = query.evaluate()) {
            // print the results
            while (result.hasNext()) {
                Statement stmt = result.next();
                System.out.println("found match: " + stmt.getSubject().stringValue() + "\t"
                        + stmt.getPredicate().stringValue() + "\t"
                        + stmt.getObject().stringValue());
            }
        }

    }
}
