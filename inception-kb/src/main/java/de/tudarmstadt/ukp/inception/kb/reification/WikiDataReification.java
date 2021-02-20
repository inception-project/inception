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
package de.tudarmstadt.ukp.inception.kb.reification;

import static java.lang.Integer.toHexString;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.and;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.function;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.or;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.STR;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.STRSTARTS;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.core.query.Queries.SELECT;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.object;

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
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
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
import de.tudarmstadt.ukp.inception.kb.querybuilder.Queries;
import de.tudarmstadt.ukp.inception.kb.querybuilder.ValuesPattern;

/**
 * <pre>
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
 * </pre>
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
    private static final String PREFIX_WDS = "http://www.wikidata.org/entity/statement/";

    private static final Variable VAR_SUBJECT = var("s");
    private static final Variable VAR_PRED1 = var("p1");
    private static final Variable VAR_STATEMENT = var("st");
    private static final Variable VAR_PRED2 = var("p2");
    private static final Variable VAR_VALUE = var("v");

    private final InceptionValueMapper valueMapper = new InceptionValueMapper();

    @Override
    public List<KBStatement> listStatements(RepositoryConnection aConnection, KnowledgeBase aKB,
            KBHandle aItem, boolean aAll)
    {
        long startTime = currentTimeMillis();

        SelectQuery query = SELECT(VAR_SUBJECT, VAR_PRED1, VAR_STATEMENT, VAR_PRED2, VAR_VALUE);
        query.where(new ValuesPattern(VAR_SUBJECT, iri(aItem.getIdentifier())), GraphPatterns
                .and(VAR_SUBJECT.has(VAR_PRED1, VAR_STATEMENT),
                        VAR_STATEMENT.has(VAR_PRED2, VAR_VALUE))
                .filter(and(function(STRSTARTS, function(STR, VAR_PRED1), literalOf(PREFIX_PROP)),
                        or(function(STRSTARTS, function(STR, VAR_PRED2),
                                literalOf(PREFIX_PROP_STATEMENT)),
                                function(STRSTARTS, function(STR, VAR_PRED2),
                                        literalOf(PREFIX_PROP_QUALIFIER))).parenthesize())));
        query.limit(aKB.getMaxResults());

        String queryId = toHexString(query.getQueryString().hashCode());

        log.trace("[{}] Query: {}", queryId, query.getQueryString());

        TupleQuery tupleQuery = aConnection.prepareTupleQuery(query.getQueryString());

        ValueFactory vf = SimpleValueFactory.getInstance();
        Map<Statement, KBStatement> statements = new LinkedHashMap<>();
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindings = result.next();

                log.trace("[{}] Bindings: {}", queryId, bindings);

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
                    statement.setProperty(new KBProperty(pred1.stringValue()));
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
                    // Qualifier - the property we add here is temporary and will be replaced
                    // with a richer property below
                    KBQualifier qualifier = new KBQualifier(statement,
                            new KBProperty(pred2.stringValue()), value);

                    // Store the secondary original triples in the qualifier
                    qualifier.getOriginalTriples().add(secStatement);

                    statement.addQualifier(qualifier);
                }
            }
        }

        // For all values of KBStatements and KBQualifiers collect the property IRIs (as strings)
        Set<String> propertyIris = new HashSet<>();
        statements.values().stream().map(stmt -> stmt.getProperty().getIdentifier())
                .forEach(propertyIris::add);
        statements.values().stream().flatMap(stmt -> stmt.getQualifiers().stream())
                .map(qualifier -> qualifier.getProperty().getIdentifier())
                .forEach(propertyIris::add);

        Map<String, KBProperty> propertyMap = Queries.fetchProperties(aKB, aConnection,
                propertyIris);

        // For all values of KBStatements and KBQualifiers that are IRIs, collect the IRIs so we
        // can resolve them to their label
        Set<Object> iriValues = new HashSet<>();
        statements.values().stream() //
                .map(stmt -> stmt.getValue()) //
                .filter(value -> value instanceof IRI) //
                .map(value -> (IRI) value) //
                .forEach(iriValues::add);
        statements.values().stream() //
                .flatMap(stmt -> stmt.getQualifiers().stream()) //
                .map(KBQualifier::getValue) //
                .filter(value -> value instanceof IRI) //
                .map(value -> (IRI) value) //
                .forEach(iriValues::add);

        Map<String, KBHandle> labelMap = Queries.fetchLabelsForIriValues(aKB, aConnection,
                iriValues);

        // Fill in property information and labels
        for (KBStatement stmt : statements.values()) {
            // Fill in property information in statement
            KBProperty property = propertyMap.computeIfAbsent(stmt.getProperty().getIdentifier(),
                    _it -> new KBProperty(_it));
            stmt.setProperty(property);

            // Fill in label information for IRI-valued statements
            Object value = stmt.getValue();
            if (value instanceof IRI) {
                stmt.setValueLabel(labelMap
                        .computeIfAbsent(((IRI) value).stringValue(), KBHandle::new).getUiLabel());
            }

            for (KBQualifier qualifier : stmt.getQualifiers()) {
                // Fill in property information in qualifier
                KBProperty qualifierProperty = propertyMap.computeIfAbsent(
                        qualifier.getProperty().getIdentifier(), _it -> new KBProperty(_it));
                qualifier.setProperty(qualifierProperty);

                // Fill in label information for IRI-valued qualifiers
                Object qualifierValue = qualifier.getValue();
                if (qualifierValue instanceof IRI) {
                    qualifier.setValueLabel(labelMap
                            .computeIfAbsent(((IRI) qualifierValue).stringValue(), KBHandle::new)
                            .getUiLabel());
                }
            }
        }

        log.debug("[{}] Query returned {} statements in {}ms", queryId, statements.size(),
                currentTimeMillis() - startTime);

        return statements.values().stream().collect(Collectors.toList());
    }

    private List<Statement> getStatementsById(RepositoryConnection aConnection, KnowledgeBase kb,
            String aStatementId)
    {
        ValueFactory vf = aConnection.getValueFactory();

        String QUERY = String.join("\n", //
                "SELECT DISTINCT ?s ?p ?ps ?o WHERE {", //
                "  ?s  ?p  ?id .", //
                "  ?id ?ps ?o .", //
                "  FILTER(STRSTARTS(STR(?ps), STR(?ps_ns)))", //
                "}", //
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

        String QUERY = String.join("\n", //
                "SELECT DISTINCT ?p ?o WHERE {", //
                "  ?id ?p ?o .", //
                "}", //
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

                // just remove the stmt in case it is a non reified triple
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
                .flatMap(qualifier -> qualifier.getOriginalTriples().stream()).collect(toList()));

        // Clear original triples
        aStatement.setOriginalTriples(emptySet());
        aStatement.getQualifiers().forEach(qualifier -> qualifier.setOriginalTriples(emptySet()));
    }

    @Override
    public void upsertStatement(RepositoryConnection aConnection, KnowledgeBase aKB,
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
        IRI pred1 = vf.createIRI(aStatement.getProperty().getIdentifier());
        Resource stmt = aStatement.getStatementId() != null
                ? vf.createIRI(aStatement.getStatementId())
                : vf.createIRI(generateStatementIdentifier(aConnection, aKB));
        // : vf.createBNode();
        IRI pred2 = vf.createIRI(propStatementIri);
        Value value = valueMapper.mapStatementValue(aStatement, vf);

        // Generate all the triples that are to be stored by this statement
        // Add primary and secondary triple
        Set<Statement> statements = new HashSet<>();
        statements.add(vf.createStatement(subject, pred1, stmt));
        statements.add(vf.createStatement(stmt, pred2, value));

        // Delete all original triples except the ones which we would re-create anyway
        upsert(aConnection, aStatement.getOriginalTriples(), statements);

        // Update the original triples and the statement ID in the statement
        aStatement.setStatementId(stmt.stringValue());
        aStatement.setOriginalTriples(statements);
    }

    @Override
    public void deleteQualifier(RepositoryConnection aConnection, KnowledgeBase kb,
            KBQualifier aQualifier)
    {
        if (aQualifier.getStatement().getStatementId() == null) {
            log.error("No statementId");
        }
        else {
            RepositoryConnection conn = aConnection;
            conn.remove(aQualifier.getOriginalTriples());

            aQualifier.getStatement().getQualifiers().remove(aQualifier);
            aQualifier.setOriginalTriples(Collections.emptySet());
        }
    }

    @Override
    public void upsertQualifier(RepositoryConnection aConnection, KnowledgeBase kb,
            KBQualifier aQualifier)
    {
        // According to the Wikidata reification scheme, the predicate of the property prefix is
        // replaced when the property is used as a qualifier, e.g.
        // p:P186 -> pq:P186
        String propStatementIri = aQualifier.getProperty().getIdentifier().replace(PREFIX_PROP,
                PREFIX_PROP_QUALIFIER);

        ValueFactory vf = aConnection.getValueFactory();
        Set<Statement> newTriples = singleton(vf.createStatement(
                vf.createIRI(aQualifier.getStatement().getStatementId()),
                vf.createIRI(propStatementIri), valueMapper.mapQualifierValue(aQualifier, vf)));

        upsert(aConnection, aQualifier.getOriginalTriples(), newTriples);

        aQualifier.getStatement().addQualifier(aQualifier);
        aQualifier.setOriginalTriples(newTriples);
    }

    @Override
    public List<KBQualifier> listQualifiers(RepositoryConnection aConnection, KnowledgeBase kb,
            KBStatement aStatement)
    {
        SelectQuery query = SELECT(VAR_STATEMENT, VAR_PRED2, VAR_VALUE);
        query.where(new ValuesPattern(VAR_STATEMENT, iri(aStatement.getStatementId())),
                GraphPatterns.and(VAR_STATEMENT.has(VAR_PRED2, VAR_VALUE).filter(function(STRSTARTS,
                        function(STR, VAR_PRED2), literalOf(PREFIX_PROP_QUALIFIER)))));
        query.limit(kb.getMaxResults());

        String queryId = toHexString(query.getQueryString().hashCode());

        log.trace("[{}] Query: {}", queryId, query.getQueryString());

        TupleQuery tupleQuery = aConnection.prepareTupleQuery(query.getQueryString());

        ValueFactory vf = SimpleValueFactory.getInstance();
        List<KBQualifier> qualifiers = new ArrayList<>();
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindings = result.next();

                log.trace("[{}] Bindings: {}", queryId, bindings);

                Resource stmt = (Resource) bindings.getBinding("st").getValue();
                IRI pred2 = (IRI) bindings.getBinding("p2").getValue();
                Value value = bindings.getBinding("v").getValue();

                // Statement representing the secondary triple for the property value and/or
                // qualifiers with the value in the object position
                Statement secStatement = vf.createStatement(stmt, pred2, value);

                // Qualifier
                KBQualifier qualifier = new KBQualifier(aStatement,
                        new KBProperty(pred2.stringValue()), value);

                // Store the secondary original triples in the qualifier
                qualifier.getOriginalTriples().add(secStatement);
                aStatement.addQualifier(qualifier);

                qualifiers.add(qualifier);
            }

            return qualifiers;
        }
    }

    @Override
    public boolean exists(RepositoryConnection aConnection, KnowledgeBase akb,
            KBStatement aStatement)
    {
        // According to the Wikidata reification scheme, the predicate of the secondary triple
        // corresponds to the predicate of the primary triple with the prefix replaced, e.g.
        // p:P186 -> ps:P186
        String propStatementIri = aStatement.getProperty().getIdentifier().replace(PREFIX_PROP,
                PREFIX_PROP_STATEMENT);

        Iri subject = iri(aStatement.getInstance().getIdentifier());
        Iri pred1 = iri(aStatement.getProperty().getIdentifier());
        Variable stmt = var("stmt");
        Iri pred2 = iri(propStatementIri);
        RdfObject value = object(
                valueMapper.mapStatementValue(aStatement, SimpleValueFactory.getInstance()));

        String query = String.join("\n", //
                "SELECT * { BIND ( EXISTS {", //
                subject.has(pred1, stmt).getQueryString(), //
                stmt.has(pred2, value).getQueryString(), //
                "} AS ?result ) }");

        TupleQuery tupleQuery = aConnection.prepareTupleQuery(SPARQL, query);

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            return ((BooleanLiteral) result.next().getBinding("result").getValue()).booleanValue();
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

    private String generateStatementIdentifier(RepositoryConnection aConn, KnowledgeBase aKB)
    {
        return PREFIX_WDS + aConn.getValueFactory().createBNode();
    }

    private String generateIdentifier(RepositoryConnection aConn, KnowledgeBase aKB)
    {
        ValueFactory vf = aConn.getValueFactory();
        // default value of basePrefix is IriConstants.INCEPTION_NAMESPACE
        return aKB.getBasePrefix() + vf.createBNode().getID();
    }
}
