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
package org.eclipse.rdf4j.sail.lucene.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.eclipse.rdf4j.common.concurrent.locks.Properties;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.sail.evaluation.TupleFunctionEvaluationMode;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LuceneIndexTest
{

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    public static final IRI CONTEXT_1 = vf.createIRI("urn:context1");

    public static final IRI CONTEXT_2 = vf.createIRI("urn:context2");

    public static final IRI CONTEXT_3 = vf.createIRI("urn:context3");

    // create some objects that we will use throughout this test
    IRI subject = vf.createIRI("urn:subj");

    IRI subject2 = vf.createIRI("urn:subj2");

    IRI predicate1 = vf.createIRI("urn:pred1");

    IRI predicate2 = vf.createIRI("urn:pred2");

    Literal object1 = vf.createLiteral("object1");

    Literal object2 = vf.createLiteral("object2");

    Literal object3 = vf.createLiteral("cats");

    Literal object4 = vf.createLiteral("dogs");

    Literal object5 = vf.createLiteral("chicken");

    Statement statement11 = vf.createStatement(subject, predicate1, object1);

    Statement statement12 = vf.createStatement(subject, predicate2, object2);

    Statement statement21 = vf.createStatement(subject2, predicate1, object3);

    Statement statement22 = vf.createStatement(subject2, predicate2, object4);

    Statement statement23 = vf.createStatement(subject2, predicate2, object5);

    Statement statementContext111 = vf.createStatement(subject, predicate1, object1, CONTEXT_1);

    Statement statementContext121 = vf.createStatement(subject, predicate2, object2, CONTEXT_1);

    Statement statementContext211 = vf.createStatement(subject2, predicate1, object3, CONTEXT_1);

    Statement statementContext222 = vf.createStatement(subject2, predicate2, object4, CONTEXT_2);

    Statement statementContext232 = vf.createStatement(subject2, predicate2, object5, CONTEXT_2);

    // add a statement to an index
    ByteBuffersDirectory directory;

    StandardAnalyzer analyzer;

    LuceneIndex index;

    @BeforeEach
    public void setUp() throws Exception
    {
        directory = new ByteBuffersDirectory();
        analyzer = new StandardAnalyzer();
        index = new LuceneIndex(directory, analyzer);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        index.shutDown();
        Properties.setLockTrackingEnabled(false);
    }

    @Test
    public void testAddStatement() throws IOException, ParseException
    {
        // add a statement to an index
        index.begin();
        index.addStatement(statement11);
        index.commit();

        // check that it arrived properly
        DirectoryReader reader = DirectoryReader.open(directory);
        assertEquals(1, reader.numDocs());

        Term term = new Term(SearchFields.URI_FIELD_NAME, subject.toString());
        PostingsEnum docs = termDocs(reader, term);
        assertTrue(next(docs));

        int documentNr = docs.docID();
        Document document = reader.document(documentNr);
        assertEquals(subject.toString(), document.get(SearchFields.URI_FIELD_NAME));
        assertEquals(object1.getLabel(), document.get(predicate1.toString()));

        assertFalse(next(docs));
        reader.close();

        // add another statement
        index.begin();
        index.addStatement(statement12);
        index.commit();

        // See if everything remains consistent. We must create a new IndexReader
        // in order to be able to see the updates
        reader = DirectoryReader.open(directory);
        assertEquals(1, reader.numDocs()); // #docs should *not* have increased

        docs = termDocs(reader, term);
        assertTrue(next(docs));

        documentNr = docs.docID();
        document = reader.document(documentNr);
        assertEquals(subject.toString(), document.get(SearchFields.URI_FIELD_NAME));
        assertEquals(object1.getLabel(), document.get(predicate1.toString()));
        assertEquals(object2.getLabel(), document.get(predicate2.toString()));

        assertFalse(next(docs));

        // see if we can query for these literals
        IndexSearcher searcher = new IndexSearcher(reader);
        QueryParser parser = new QueryParser(SearchFields.TEXT_FIELD_NAME, analyzer);

        Query query = parser.parse(object1.getLabel());
        System.out.println("query=" + query);
        TotalHitCountCollector results = new TotalHitCountCollector();
        searcher.search(query, results);
        assertEquals(1, results.getTotalHits());

        query = parser.parse(object2.getLabel());
        results = new TotalHitCountCollector();
        searcher.search(query, results);
        assertEquals(1, results.getTotalHits());

        reader.close();

        // remove the first statement
        index.begin();
        index.removeStatement(statement11);
        index.commit();

        // check that that statement is actually removed and that the other still
        // exists
        reader = DirectoryReader.open(directory);
        assertEquals(1, reader.numDocs());

        docs = termDocs(reader, term);
        assertTrue(next(docs));

        documentNr = docs.docID();
        document = reader.document(documentNr);
        assertEquals(subject.toString(), document.get(SearchFields.URI_FIELD_NAME));
        assertNull(document.get(predicate1.toString()));
        assertEquals(object2.getLabel(), document.get(predicate2.toString()));

        assertFalse(next(docs));

        reader.close();

        // remove the other statement
        index.begin();
        index.removeStatement(statement12);
        index.commit();

        // check that there are no documents left (i.e. the last Document was
        // removed completely, rather than its remaining triple removed)
        reader = DirectoryReader.open(directory);
        assertEquals(0, reader.numDocs());
        reader.close();
    }

    /**
     * NB: this is a convenient but very slow way of getting termDocs. It is sufficient for testing
     * purposes.
     *
     * @throws IOException
     */
    private static PostingsEnum termDocs(IndexReader reader, Term term) throws IOException
    {
        return MultiTerms.getTermPostingsEnum(reader, term.field(), term.bytes());
    }

    private static boolean next(PostingsEnum docs) throws IOException
    {
        return (docs.nextDoc() != PostingsEnum.NO_MORE_DOCS);
    }

    @Test
    public void testAddMultiple() throws Exception
    {
        // add a statement to an index
        HashSet<Statement> added = new HashSet<>();
        HashSet<Statement> removed = new HashSet<>();
        added.add(statement11);
        added.add(statement12);
        added.add(statement21);
        added.add(statement22);
        index.begin();
        index.addRemoveStatements(added, removed);
        index.commit();

        try ( // check that it arrived properly
                DirectoryReader reader = DirectoryReader.open(directory)) {
            assertEquals(2, reader.numDocs());
        }

        // check the documents
        Document document = index.getDocuments(subject).iterator().next();
        assertEquals(subject.toString(), document.get(SearchFields.URI_FIELD_NAME));
        assertStatement(statement11, document);
        assertStatement(statement12, document);

        document = index.getDocuments(subject2).iterator().next();
        assertEquals(subject2.toString(), document.get(SearchFields.URI_FIELD_NAME));
        assertStatement(statement21, document);
        assertStatement(statement22, document);

        // check if the text field stores all added string values
        Set<String> texts = new HashSet<>();
        texts.add("cats");
        texts.add("dogs");
        // FIXME
        // assertTexts(texts, document);

        // add/remove one
        added.clear();
        removed.clear();
        added.add(statement23);
        removed.add(statement22);
        index.begin();
        index.addRemoveStatements(added, removed);
        index.commit();

        // check doc 2
        document = index.getDocuments(subject2).iterator().next();
        assertEquals(subject2.toString(), document.get(SearchFields.URI_FIELD_NAME));
        assertStatement(statement21, document);
        assertStatement(statement23, document);
        assertNoStatement(statement22, document);

        // check if the text field stores all added and no deleted string values
        texts.remove("dogs");
        texts.add("chicken");
        // FIXME
        // assertTexts(texts, document);

        // TODO: check deletion of the rest

    }

    /**
     * Contexts can only be tested in combination with a sail, as the triples have to be retrieved
     * from the sail
     *
     * @throws Exception
     */
    @Test
    public void testContexts() throws Exception
    {
        // add a sail
        MemoryStore memoryStore = new MemoryStore();
        // enable lock tracking
        org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(true);
        LuceneSail sail = new LuceneSail();
        sail.setBaseSail(memoryStore);
        sail.setLuceneIndex(index);

        // create a Repository wrapping the LuceneSail
        SailRepository repository = new SailRepository(sail);

        try ( // now add the statements through the repo
              // add statements with context
                SailRepositoryConnection connection = repository.getConnection()) {
            connection.begin();
            connection.add(statementContext111, statementContext111.getContext());
            connection.add(statementContext121, statementContext121.getContext());
            connection.add(statementContext211, statementContext211.getContext());
            connection.add(statementContext222, statementContext222.getContext());
            connection.add(statementContext232, statementContext232.getContext());
            connection.commit();

            // check if they are there
            assertStatement(statementContext111);
            assertStatement(statementContext121);
            assertStatement(statementContext211);
            assertStatement(statementContext222);
            assertStatement(statementContext232);

            // delete context 1
            connection.begin();
            connection.clear(new Resource[] { CONTEXT_1 });
            connection.commit();
            assertNoStatement(statementContext111);
            assertNoStatement(statementContext121);
            assertNoStatement(statementContext211);
            assertStatement(statementContext222);
            assertStatement(statementContext232);
        }
        finally {
            // close repo
            repository.shutDown();
        }
    }

    /**
     * Contexts can only be tested in combination with a sail, as the triples have to be retrieved
     * from the sail
     *
     * @throws Exception
     */
    @Test
    public void testContextsRemoveContext2() throws Exception
    {
        // add a sail
        MemoryStore memoryStore = new MemoryStore();
        // enable lock tracking
        org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(true);
        LuceneSail sail = new LuceneSail();
        sail.setBaseSail(memoryStore);
        sail.setLuceneIndex(index);

        // create a Repository wrapping the LuceneSail
        SailRepository repository = new SailRepository(sail);

        try ( // now add the statements through the repo
              // add statements with context
                SailRepositoryConnection connection = repository.getConnection()) {
            connection.begin();
            connection.add(statementContext111, statementContext111.getContext());
            connection.add(statementContext121, statementContext121.getContext());
            connection.add(statementContext211, statementContext211.getContext());
            connection.add(statementContext222, statementContext222.getContext());
            connection.add(statementContext232, statementContext232.getContext());
            connection.commit();

            // check if they are there
            assertStatement(statementContext111);
            assertStatement(statementContext121);
            assertStatement(statementContext211);
            assertStatement(statementContext222);
            assertStatement(statementContext232);

            // delete context 2
            connection.begin();
            connection.clear(new Resource[] { CONTEXT_2 });
            connection.commit();
            assertStatement(statementContext111);
            assertStatement(statementContext121);
            assertStatement(statementContext211);
            assertNoStatement(statementContext222);
            assertNoStatement(statementContext232);
        }
        finally {
            // close repo
            repository.shutDown();
        }
    }

    @Test
    public void testRejectedDatatypes()
    {
        IRI STRING = vf.createIRI("http://www.w3.org/2001/XMLSchema#string");
        IRI FLOAT = vf.createIRI("http://www.w3.org/2001/XMLSchema#float");
        Literal literal1 = vf.createLiteral("hi there");
        Literal literal2 = vf.createLiteral("hi there, too", STRING);
        Literal literal3 = vf.createLiteral("1.0");
        Literal literal4 = vf.createLiteral("1.0", FLOAT);
        assertEquals(true, index.accept(literal1), "Is the first literal accepted?");
        assertEquals(true, index.accept(literal2), "Is the second literal accepted?");
        assertEquals(true, index.accept(literal3), "Is the third literal accepted?");
        assertEquals(false, index.accept(literal4), "Is the fourth literal accepted?");
    }

    @Test
    public void testInstantiatesCustomQueryAnalyzer() throws Exception
    {
        LuceneIndex index = new LuceneIndex();
        java.util.Properties props = new java.util.Properties();
        props.put(LuceneSail.QUERY_ANALYZER_CLASS_KEY, EnglishAnalyzer.class.getName());
        props.put(LuceneSail.ANALYZER_CLASS_KEY, EnglishAnalyzer.class.getName());
        props.put(LuceneSail.LUCENE_RAMDIR_KEY, "true");
        index.initialize(props);

        assertTrue(index.getAnalyzer() instanceof EnglishAnalyzer);
        assertTrue(index.getQueryAnalyzer() instanceof EnglishAnalyzer);
    }

    private void assertStatement(Statement statement) throws Exception
    {
        Document document = index.getDocument(statement.getSubject(), statement.getContext());
        if (document == null) {
            fail("Missing document " + statement.getSubject());
        }
        assertStatement(statement, document);
    }

    private void assertNoStatement(Statement statement) throws Exception
    {
        Document document = index.getDocument(statement.getSubject(), statement.getContext());
        if (document == null) {
            return;
        }
        assertNoStatement(statement, document);
    }

    /**
     * @param statement112
     * @param document
     */
    private void assertStatement(Statement statement, Document document)
    {
        IndexableField[] fields = document
                .getFields(SearchFields.getPropertyField(statement.getPredicate()));
        assertNotNull(fields,
                "field " + statement.getPredicate() + " not found in document " + document);
        for (IndexableField f : fields) {
            if (((Literal) statement.getObject()).getLabel().equals(f.stringValue())) {
                return;
            }
        }
        fail("Statement not found in document " + statement);
    }

    /**
     * @param statement112
     * @param document
     */
    private void assertNoStatement(Statement statement, Document document)
    {
        IndexableField[] fields = document
                .getFields(SearchFields.getPropertyField(statement.getPredicate()));
        if (fields == null) {
            return;
        }
        for (IndexableField f : fields) {
            if (((Literal) statement.getObject()).getLabel().equals(f.stringValue())) {
                fail("Statement should not be found in document " + statement);
            }
        }

    }

    /*
     * private void assertTexts(Set<String> texts, Document document) { Set<String> toFind = new
     * HashSet<String>(texts); Set<String> found = new HashSet<String>(); for(Field field :
     * document.getFields(LuceneIndex.TEXT_FIELD_NAME)) { // is the field value expected and not yet
     * been found? if(toFind.remove(field.stringValue())) { // add it to the found set // (it was
     * already remove from the toFind list in the if clause) found.add(field.stringValue()); } else
     * { assertEquals( "Was the text value '" + field.stringValue() + "' expected to exist?", false,
     * true); } } for(String notFound : toFind) { assertEquals("Was the expected text value '" +
     * notFound + "' found?", true, false); } }
     */

    @Test
    public void geoSparqlQueryTest()
    {
        final String prefix = "http://www.example.org/#";
        final String prefixes = "PREFIX ex: <" + prefix + ">\n" + "PREFIX geof: <" + GEOF.NAMESPACE
                + ">\n" + "PREFIX geo: <" + CoreDatatype.GEO.NAMESPACE + ">\n"
                + "PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/>\n";
        Model data = new TreeModel();

        IRI cp = vf.createIRI(prefix + "cp");
        IRI bm = vf.createIRI(prefix + "bm");
        IRI nkv = vf.createIRI(prefix + "nkv");

        data.add(cp, GEO.AS_WKT,
                vf.createLiteral("Point(4.38436 45.44917)", CoreDatatype.GEO.WKT_LITERAL));
        data.add(bm, GEO.AS_WKT,
                vf.createLiteral("Point(4.38311 45.45423)", CoreDatatype.GEO.WKT_LITERAL));
        data.add(nkv, GEO.AS_WKT,
                vf.createLiteral("Point(4.87306 45.77903)", CoreDatatype.GEO.WKT_LITERAL));
        data.add(vf.createIRI(prefix + "arp"), GEO.AS_WKT,
                vf.createLiteral("Point(2.89271 42.69848)", CoreDatatype.GEO.WKT_LITERAL));

        String polyVill = "POLYGON((4.864712 45.784405, 4.883165 45.787756, 4.889946 45.785781, 4.904881 45.767403, 4.900761 45.765487, 4.872093 45.770995, 4.86454 45.770457, 4.858789 45.770277, 4.859905 45.784644, 4.864712 45.784405))";
        String polySain = "POLYGON((4.380627 45.463983, 4.400539 45.462177, 4.428349 45.436286, 4.399509 45.411346, 4.374447 45.426528, 4.370499 45.450618, 4.380627 45.463983))";

        SailRepository m1 = new SailRepository(new MemoryStore());
        LuceneSail lc = new LuceneSail();
        lc.setBaseSail(new MemoryStore());
        lc.setParameter(LuceneSail.WKT_FIELDS, GEO.AS_WKT.toString());
        lc.setLuceneIndex(index);
        lc.setEvaluationMode(TupleFunctionEvaluationMode.NATIVE);
        SailRepository m2 = new SailRepository(lc);

        // add test data
        Repositories.consume(m1, conn -> conn.add(data));
        Repositories.consume(m2, conn -> conn.add(data));

        lc.reindex();

        Function<TupleQueryResult, Set<Value>> toval = (res) -> {
            Set<Value> list = new HashSet<>();
            while (res.hasNext()) {
                BindingSet next = res.next();
                list.add(next.getValue("v"));
            }
            return list;
        };

        // test queries

        String q0 = prefixes + "SELECT * {\n" + "  ?v geo:asWKT ?loc .\n"
                + "  FILTER(geof:distance(\"Point(4.386914 45.440637)\"^^geo:wktLiteral, ?loc, uom:metre) < 10000) \n"
                + "}\n";
        Set<Value> q0ex = Set.of(bm, cp);

        String q1 = prefixes + "SELECT * {\n" + "  ?v geo:asWKT ?loc .\n"
                + "  FILTER(geof:ehContains(\"" + polySain + "\"^^geo:wktLiteral, ?loc)) \n"
                + "}\n";
        Set<Value> q1ex = Set.of(bm, cp);

        String q2 = prefixes + "SELECT * {\n" + "  ?v geo:asWKT ?loc .\n"
                + "  FILTER(geof:ehContains(\"" + polyVill + "\"^^geo:wktLiteral, ?loc)) \n"
                + "}\n";
        Set<Value> q2ex = Set.of(nkv);

        Set<Value> nlcq0 = Repositories.tupleQuery(m1, q0, toval);
        Set<Value> nlcq1 = Repositories.tupleQuery(m1, q1, toval);
        Set<Value> nlcq2 = Repositories.tupleQuery(m1, q2, toval);

        assertEquals(q0ex, nlcq0);
        assertEquals(q1ex, nlcq1);
        assertEquals(q2ex, nlcq2);

        assertEquals(nlcq0, Repositories.tupleQuery(m2, q0, toval));
        assertEquals(nlcq1, Repositories.tupleQuery(m2, q1, toval));
        assertEquals(nlcq2, Repositories.tupleQuery(m2, q2, toval));
    }
}
