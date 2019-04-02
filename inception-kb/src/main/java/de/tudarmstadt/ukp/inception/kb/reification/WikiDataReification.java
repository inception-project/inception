/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.kb.reification;

import static java.lang.Integer.toHexString;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.and;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.function;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.or;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.STR;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.STRSTARTS;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.InceptionValueMapper;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.querybuilder.ValuesPattern;

/**
 * wd:Q12418 p:P186 ?statement1.    # Mona Lisa: material used: ?statement1
 * ?statement1 ps:P186 wd:Q296955.  # value: oil paint
 *
 * wd:Q12418 p:P186 ?statement2.    # Mona Lisa: material used: ?statement2
 * ?statement2 ps:P186 wd:Q291034.  # value: poplar wood
 * ?statement2 pq:P518 wd:Q861259.  # qualifier: applies to part: painting surface
 *
 * wd:Q12418 p:P186 ?statement3.    # Mona Lisa: material used: ?statement3
 * ?statement3 ps:P186 wd:Q287.     # value: wood
 * ?statement3 pq:P518 wd:Q1737943. # qualifier: applies to part: stretcher bar
 * ?statement3 pq:P580 1951.        # qualifier: start time: 1951 (pseudo-syntax)
 */
public class WikiDataReification
    implements ReificationStrategy
{
    private static final String NAMPESPACE_ROOT = "https://github.com/inception-project";
    private static final String PREDICATE_NAMESPACE = NAMPESPACE_ROOT + "/predicate#";

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final String PREFIX_PROP = "http://www.wikidata.org/prop/P";
    private static final String PREFIX_PROP_STATEMENT = "http://www.wikidata.org/prop/statement/P";
    private static final String PREFIX_PROP_QUALIFIER = "http://www.wikidata.org/prop/qualifier/P";

    
    private final InceptionValueMapper valueMapper = new InceptionValueMapper();

    private Set<Statement> reifyQualifier(KnowledgeBase kb, KBQualifier aQualifier)
    {
        ValueFactory vf = SimpleValueFactory.getInstance();
        
        Resource statementId = vf.createBNode(aQualifier.getKbStatement().getStatementId());
        KBHandle qualifierProperty = aQualifier.getKbProperty();
        IRI qualifierPredicate = vf.createIRI(qualifierProperty.getIdentifier());
        Value qualifierValue = valueMapper.mapQualifierValue(aQualifier, vf);

        Statement qualifierStatement = vf
            .createStatement(statementId, qualifierPredicate, qualifierValue);
        Set<Statement> originalStatements = new HashSet<>();
        originalStatements.add(qualifierStatement); //id P V
        return originalStatements;
    }

    @Override
    public List<KBStatement> listStatements(RepositoryConnection aConnection, KnowledgeBase kb,
            KBHandle aInstance, boolean aAll)
    {
        Variable vSubject = SparqlBuilder.var("s");
        Variable vProp1 = SparqlBuilder.var("p1");
        Variable vProp2 = SparqlBuilder.var("p2");
        Variable vValue = SparqlBuilder.var("v");
        Variable vStmt = SparqlBuilder.var("st");
        
        SelectQuery query = Queries.SELECT(vSubject, vProp1, vStmt, vProp2, vValue);
        query.where(
            new ValuesPattern(vSubject, Rdf.iri(aInstance.getIdentifier())),
            GraphPatterns.and(vSubject.has(vProp1, vStmt),
            vStmt.has(vProp2, vValue)).filter(and(
                function(STRSTARTS, function(STR, vProp1), literalOf(PREFIX_PROP)),or(
                    function(STRSTARTS, function(STR, vProp2), literalOf(PREFIX_PROP_STATEMENT)),
                    function(STRSTARTS, function(STR, vProp2), literalOf(PREFIX_PROP_QUALIFIER)))
                    .parenthesize())));
        query.limit(kb.getMaxResults());
        
        log.trace("[{}] Query: {}", toHexString(hashCode()), query.getQueryString());
        
        TupleQuery tupleQuery = aConnection.prepareTupleQuery(query.getQueryString());
        
        ValueFactory vf = SimpleValueFactory.getInstance();
        Map<Statement, KBStatement> statements = new LinkedHashMap<>();
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindings = result.next();
                
                log.trace("[{}] Bindings: {}", toHexString(hashCode()), bindings);
                
                IRI subject = (IRI) bindings.getBinding("s").getValue();
                IRI pred1 = (IRI) bindings.getBinding("p1").getValue();
                Resource stmt = (Resource) bindings.getBinding("st").getValue();
                IRI pred2 = (IRI) bindings.getBinding("p2").getValue();
                Value value = bindings.getBinding("v").getValue();
                
                // Statement representing the primary triple with the reified statement in the
                // object position
                Statement priStatement = vf.createStatement(subject, pred1, stmt);
                // Statement representing the secondary triple for the property value and/or
                // qualifiers with the value in the object position
                Statement secStatement = vf.createStatement(stmt, pred2, value);
                
                // Fetch existing (partially loaded) statement or create a new one if necessary
                KBStatement statement = statements.get(priStatement);
                if (statement == null) {
                    statement = new KBStatement(stmt.stringValue(), subject.stringValue());
                    statement.setProperty(new KBHandle(pred1.stringValue()));
                    statements.put(priStatement, statement);
                }
                
                // Always store the primary original triple in the statement
                statement.getOriginalTriples().add(priStatement);
                
                // Check if the secondary statement contains the property value or a qualifier
                if (secStatement.getPredicate().stringValue().startsWith(PREFIX_PROP_STATEMENT)) {
                    // Property value
                    statement.setValue(value);
                    
                    // Store the secondary original triple in the statement
                    statement.getOriginalTriples().add(secStatement);
                }
                else if (secStatement.getPredicate().stringValue()
                        .startsWith(PREFIX_PROP_QUALIFIER)) {
                    // Qualifier
                    KBQualifier qualifier = new KBQualifier(statement,
                            new KBHandle(pred2.stringValue()), value);

                    // Store the secondary original triples in the qualifier
                    qualifier.getOriginalStatements().add(secStatement);
                    
                    statement.addQualifier(qualifier);
                }
            }
            
            return statements.values().stream().collect(Collectors.toList());
        }
    }

    private List<Statement> getStatementsById(RepositoryConnection aConnection, KnowledgeBase kb,
            String aStatementId)
    {
        ValueFactory vf = aConnection.getValueFactory();
        
        String QUERY = String
            .join("\n",
                "SELECT DISTINCT ?s ?p ?ps ?o WHERE {",
                "  ?s  ?p  ?id .",
                "  ?id ?ps ?o .",
                "  FILTER(STRSTARTS(STR(?ps), STR(?ps_ns)))",
                "}",
                "LIMIT 10");
        Resource id = vf.createBNode(aStatementId);
        TupleQuery tupleQuery = aConnection.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
        tupleQuery.setBinding("id", id);
        tupleQuery.setBinding("ps_ns", vf.createIRI(PREDICATE_NAMESPACE));

        tupleQuery.setIncludeInferred(false);
        TupleQueryResult result;

        try {
            result = tupleQuery.evaluate();
        }
        catch (QueryEvaluationException e) {
            log.warn("No such statementId in knowledge base", e);
            return null;
        }

        List<Statement> statements = new ArrayList<>();
        while (result.hasNext()) {
            BindingSet bindings = result.next();
            Binding s = bindings.getBinding("s");
            Binding p = bindings.getBinding("p");
            Binding o = bindings.getBinding("o");
            Binding ps = bindings.getBinding("ps");

            IRI instance = vf.createIRI(s.getValue().stringValue());
            IRI predicate = vf.createIRI(p.getValue().stringValue());
            Statement root = vf.createStatement(instance, predicate, id);

            IRI valuePredicate = vf.createIRI(ps.getValue().stringValue());
            Value object = o.getValue();
            Statement valueStatement = vf.createStatement(id, valuePredicate, object);
            statements.add(root);
            statements.add(valueStatement);
        }
        return statements;
    }

    private List<Statement> getQualifiersById(RepositoryConnection aConnection, KnowledgeBase kb,
            String aStatementId)
    {
        ValueFactory vf = aConnection.getValueFactory();
        
        String QUERY = String.join("\n",
                "SELECT DISTINCT ?p ?o WHERE {",
                "  ?id ?p ?o .",
                "}",
                "LIMIT " + kb.getMaxResults());
        Resource id = vf.createBNode(aStatementId);
        TupleQuery tupleQuery = aConnection.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
        tupleQuery.setBinding("id", id);

        tupleQuery.setIncludeInferred(false);
        TupleQueryResult result;

        try {
            result = tupleQuery.evaluate();
        }
        catch (QueryEvaluationException e) {
            log.warn("No such statementId in knowledge base", e);
            return null;
        }

        List<Statement> statements = new ArrayList<>();
        while (result.hasNext()) {
            BindingSet bindings = result.next();
            Binding p = bindings.getBinding("p");
            Binding o = bindings.getBinding("o");

            if (!p.getValue().stringValue().contains(PREDICATE_NAMESPACE)) {
                IRI predicate = vf.createIRI(p.getValue().stringValue());
                Value object = o.getValue();
                Statement qualifierStatement = vf.createStatement(id, predicate, object);
                statements.add(qualifierStatement);
            }
        }
        return statements;
    }
    
    @Override
    public void deleteInstance(RepositoryConnection aConnection, KnowledgeBase kb,
            KBInstance aInstance)
    {
        delete(aConnection, kb, aInstance.getIdentifier());
    }

    @Override
    public void deleteProperty(RepositoryConnection aConnection, KnowledgeBase kb,
            KBProperty aProperty)
    {
        delete(aConnection, kb, aProperty.getIdentifier());
    }

    @Override
    public void deleteConcept(RepositoryConnection aConnection, KnowledgeBase kb,
            KBConcept aConcept)
    {
        delete(aConnection, kb, aConcept.getIdentifier());
    }

    private void delete(RepositoryConnection aConnection, KnowledgeBase kb, String aIdentifier)
    {
        ValueFactory vf = aConnection.getValueFactory();
        IRI iri = vf.createIRI(aIdentifier);
        try (RepositoryResult<Statement> subStmts = aConnection.getStatements(iri, null, null);
             RepositoryResult<Statement> predStmts = aConnection.getStatements(null, iri, null);
             RepositoryResult<Statement> objStmts = aConnection.getStatements(null, null, iri)) {

            while (subStmts.hasNext()) {
                Statement stmt = subStmts.next();
                // if the identifier appears as subject, the stmt id is the object of the triple
                String stmtId = stmt.getObject().stringValue();
                aConnection.remove(getStatementsById(aConnection, kb, stmtId));
                aConnection.remove(getQualifiersById(aConnection, kb, stmtId));

                //just remove the stmt in case it is a non reified triple
                aConnection.remove(stmt);
            }

            while (objStmts.hasNext()) {
                Statement stmt = objStmts.next();
                // if the identifier appears as object, the stmt id is the subject of the triple
                String stmtId = stmt.getSubject().stringValue();

                if (!stmt.getPredicate().stringValue().contains(PREDICATE_NAMESPACE)) {
                    // if this statement is a qualifier or a non reified triple,
                    // delete just this statement
                    aConnection.remove(stmt);
                }
                else {
                    // remove all statements and qualifiers with that id
                    aConnection.remove(getStatementsById(aConnection, kb, stmtId));
                    aConnection.remove(getQualifiersById(aConnection, kb, stmtId));
                }
            }
        }
    }

    @Override
    public void deleteStatement(RepositoryConnection aConnection, KnowledgeBase kb,
            KBStatement aStatement)
    {
        // Delete original triples
        aConnection.remove(aStatement.getOriginalTriples());
        aConnection.remove(aStatement.getQualifiers().stream()
                .flatMap(qualifier -> qualifier.getOriginalStatements().stream())
                .collect(toList()));
        
        // Clear original triples
        aStatement.setOriginalTriples(emptySet());
        aStatement.getQualifiers()
                .forEach(qualifier -> qualifier.setOriginalStatements(emptySet()));
        
//        String statementId = aStatement.getStatementId();
//        if (statementId != null) {
//            RepositoryConnection conn = aConnection;
//            conn.remove(getStatementsById(conn, kb, statementId));
//            conn.remove(getQualifiersById(conn, kb, statementId));
//            aStatement.setOriginalTriples(Collections.emptySet());
//            aStatement.setQualifiers(Collections.emptyList());
//        }
    }

    @Override
    public void upsertStatement(RepositoryConnection aConnection, KnowledgeBase kb,
            KBStatement aStatement)
    {
        if (!aStatement.getProperty().getIdentifier().startsWith(PREFIX_PROP)) {
            throw new IllegalArgumentException(
                    "With WikiDataReification, properties must start " + "with [" + PREFIX_PROP
                            + "] but found [" + aStatement.getProperty().getIdentifier() + "]");
        }
        
        ValueFactory vf = aConnection.getValueFactory();
        
        // According to the Wikidata reification scheme, the predicate of the secondary triple
        // corresponds to the predicate of the primary triple with the prefix replaced, e.g.
        // p:P186 -> ps:P186
        String propStatementIri = aStatement.getProperty().getIdentifier().replace(PREFIX_PROP,
                PREFIX_PROP_STATEMENT);
        
        IRI subject = vf.createIRI(aStatement.getInstance().getIdentifier());
        IRI pred1 =  vf.createIRI(aStatement.getProperty().getIdentifier());
        Resource stmt = aStatement.getStatementId() != null
                ? vf.createIRI(aStatement.getStatementId())
                : vf.createBNode();
        IRI pred2 =  vf.createIRI(propStatementIri);
        Value value = valueMapper.mapStatementValue(aStatement, vf);
        
        // Generate all the triples that are to be stored by this statement
        // Add primary and secondary triple
        Set<Statement> statements = new HashSet<>();
        statements.add(vf.createStatement(subject, pred1, stmt));
        statements.add(vf.createStatement(stmt, pred2, value));
        
//        // Add qualifier triples
//        for (KBQualifier qualifier : aStatement.getQualifiers()) {
//            statements.add(vf.createStatement(stmt,
//                    vf.createIRI(qualifier.getKbProperty().getIdentifier()),
//                    valueMapper.mapQualifierValue(qualifier, vf)));
//        }
//        aStatement.getQualifiers().stream()
//        .flatMap(q -> q.getOriginalStatements().stream())
//        .forEach(statementsToDelete::add);
        
        // Delete all original triples except the ones which we would re-create anyway
        upsert(aConnection, aStatement.getOriginalTriples(), statements);
        
        // Update the original triples in the statement
        aStatement.setOriginalTriples(statements);
    }
    
    @Override
    public void addQualifier(RepositoryConnection aConnection, KnowledgeBase kb,
            KBQualifier newQualifier)
    {
        ValueFactory vf = aConnection.getValueFactory();
        
        if (newQualifier.getKbStatement().getStatementId() == null) {
            log.error("No statementId");
        }
        else {
            RepositoryConnection conn = aConnection;
            Resource id = vf.createBNode(newQualifier.getKbStatement().getStatementId());
            IRI predicate = vf.createIRI(newQualifier.getKbProperty().getIdentifier());
            Value value = valueMapper.mapQualifierValue(newQualifier, vf);
            Statement qualifierStatement = vf.createStatement(id, predicate, value);
            conn.add(qualifierStatement);

            Set<Statement> statements = new HashSet<>();
            statements.add(qualifierStatement);
            newQualifier.setOriginalStatements(statements);
            newQualifier.getKbStatement().getQualifiers().add(newQualifier);
        }
    }

    @Override
    public void deleteQualifier(RepositoryConnection aConnection, KnowledgeBase kb,
            KBQualifier oldQualifier)
    {
        if (oldQualifier.getKbStatement().getStatementId() == null) {
            log.error("No statementId");
        }
        else {
            RepositoryConnection conn = aConnection;
            conn.remove(oldQualifier.getOriginalStatements());

            oldQualifier.getKbStatement().getQualifiers().remove(oldQualifier);
            oldQualifier.setOriginalStatements(Collections.emptySet());
        }
    }

    @Override
    public void upsertQualifier(RepositoryConnection aConnection, KnowledgeBase kb,
            KBQualifier aQualifier)
    {
        int index = aQualifier.getQualifierIndexByOriginalStatements();
        Set<Statement> statements = reifyQualifier(kb, aQualifier);
        aConnection.add(statements);
        if (index == -1) {
            aQualifier.setOriginalStatements(statements);
            aQualifier.getKbStatement().getQualifiers().add(aQualifier);
        }
        else {
            aConnection.remove(aQualifier.getOriginalStatements());
            aQualifier.setOriginalStatements(statements);
            aQualifier.getKbStatement().getQualifiers().set(index, aQualifier);
        }
    }

    @Override
    public List<KBQualifier> listQualifiers(RepositoryConnection aConnection, KnowledgeBase kb,
            KBStatement aStatement)
    {
        ValueFactory vf = aConnection.getValueFactory();
        
        List<KBQualifier> qualifiers = new ArrayList<>();
        String QUERY = String.join("\n", 
                "SELECT DISTINCT ?p ?o WHERE {", 
                "  ?id ?p ?o .", 
                "}",
                "LIMIT " + kb.getMaxResults());
        Resource id = vf.createBNode(aStatement.getStatementId());
        RepositoryConnection conn = aConnection;
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
        tupleQuery.setBinding("id", id);
        tupleQuery.setIncludeInferred(false);

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindings = result.next();
                Binding p = bindings.getBinding("p");
                Binding o = bindings.getBinding("o");

                if (!p.getValue().stringValue().contains(PREDICATE_NAMESPACE)) {
                    KBHandle property = new KBHandle();
                    property.setIdentifier(p.getValue().stringValue());
                    Value value = o.getValue();
                    KBQualifier qualifier = new KBQualifier(aStatement, property, value);

                    IRI predicate = vf.createIRI(p.getValue().stringValue());
                    Value object = o.getValue();
                    Statement qualifierStatement = vf.createStatement(id, predicate, object);

                    Set<Statement> statements = new HashSet<>();
                    statements.add(qualifierStatement);
                    qualifier.setOriginalStatements(statements);

                    qualifiers.add(qualifier);
                }
            }
            return qualifiers;
        }
        catch (QueryEvaluationException e) {
            log.warn("No such statementId in knowledge base", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean exists(RepositoryConnection aConnection, KnowledgeBase akb,
            KBStatement aStatement)
    {
        ValueFactory vf = aConnection.getValueFactory();
        String QUERY = String.join("\n", 
                "SELECT * WHERE {", 
                "  ?s  ?p  ?id .", 
                "  ?id ?ps ?o .",
                "  FILTER(STRSTARTS(STR(?ps), STR(?ps_ns)))", 
                "}", 
                "LIMIT 10");
        TupleQuery tupleQuery = aConnection.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
        tupleQuery.setBinding("s", vf.createIRI(aStatement.getInstance().getIdentifier()));
        tupleQuery.setBinding("p", vf.createIRI(aStatement.getProperty().getIdentifier()));

        InceptionValueMapper mapper = new InceptionValueMapper();
        tupleQuery.setBinding("o", mapper.mapStatementValue(aStatement, vf));
        tupleQuery.setBinding("ps_ns", vf.createIRI(PREDICATE_NAMESPACE));

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            return result.hasNext();
        }
    }
    
    @Override
    public String generatePropertyIdentifier(RepositoryConnection aConn, KnowledgeBase aKB)
    {
        ValueFactory vf = aConn.getValueFactory();
        return PREFIX_PROP + vf.createBNode().getID();
    }
    
    @Override
    public String generateConceptIdentifier(RepositoryConnection aConn, KnowledgeBase aKB)
    {
        return generateIdentifier(aConn, aKB);
    }
    
    @Override
    public String generateInstanceIdentifier(RepositoryConnection aConn, KnowledgeBase aKB)
    {
        return generateIdentifier(aConn, aKB);
    }
    
    private String generateIdentifier(RepositoryConnection aConn, KnowledgeBase aKB)
    {
        ValueFactory vf = aConn.getValueFactory();
        // default value of basePrefix is IriConstants.INCEPTION_NAMESPACE
        return aKB.getBasePrefix() + vf.createBNode().getID();
    }
}
