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

import static de.tudarmstadt.ukp.inception.kb.IriConstants.hasImplicitNamespace;
import static java.lang.Integer.toHexString;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.core.query.Queries.SELECT;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
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

public class NoReification
    implements ReificationStrategy
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String VAR_SUBJECT_NAME = "subj";
    public static final String VAR_PREDICATE_NAME = "pred";
    public static final String VAR_OBJECT_NAME = "obj";

    private static final Variable VAR_SUBJECT = var("subj");
    private static final Variable VAR_PREDICATE = var("pred");
    private static final Variable VAR_VALUE = var("obj");

    private final InceptionValueMapper valueMapper = new InceptionValueMapper();

    @Override
    public List<KBStatement> listStatements(RepositoryConnection aConnection, KnowledgeBase aKB,
            KBHandle aItem, boolean aAll)
    {
        long startTime = currentTimeMillis();

        SelectQuery query = SELECT(VAR_SUBJECT, VAR_PREDICATE, VAR_VALUE);
        query.where(new ValuesPattern(VAR_SUBJECT, iri(aItem.getIdentifier())),
                VAR_SUBJECT.has(VAR_PREDICATE, VAR_VALUE));
        query.limit(aKB.getMaxResults());

        String queryId = toHexString(query.getQueryString().hashCode());

        log.trace("[{}] Query: {}", queryId, query.getQueryString());

        TupleQuery tupleQuery = aConnection.prepareTupleQuery(query.getQueryString());

        // The only way to tell if a statement was inferred or not is by running the same query
        // twice, once with and once without inference being enabled. Those that are in the
        // first but not in the second were the inferred statements.
        List<Statement> explicitStmts = listStatements(tupleQuery, false);
        List<Statement> allStmts = listStatements(tupleQuery, true);

        Map<String, KBProperty> propertyMap = Queries.fetchProperties(aKB, aConnection,
                allStmts.stream().map(stmt -> stmt.getPredicate().stringValue()).collect(toList()));
        Map<String, KBHandle> labelMap = Queries.fetchLabelsForIriValues(aKB, aConnection,
                allStmts.stream().map(Statement::getObject).collect(toList()));

        List<KBStatement> statements = new ArrayList<>();
        for (Statement stmt : allStmts) {
            Value value = stmt.getObject();
            if (value == null) {
                // Can this really happen?
                log.warn("Property with null value detected.");
                continue;
            }

            if (value instanceof BNode) {
                log.warn("Properties with blank node values are not supported");
                continue;
            }

            if ((!aAll && hasImplicitNamespace(aKB, stmt.getPredicate().stringValue()))) {
                continue;
            }

            KBHandle subject = new KBHandle(stmt.getSubject().stringValue());
            KBProperty predicate = propertyMap.computeIfAbsent(stmt.getPredicate().stringValue(),
                    propertyIri -> new KBProperty(propertyIri));

            KBStatement kbStatement = new KBStatement(null, subject, predicate, value);
            if (value instanceof IRI) {
                kbStatement.setValueLabel(
                        labelMap.computeIfAbsent(value.stringValue(), KBHandle::new).getUiLabel());
            }
            kbStatement.setInferred(!explicitStmts.contains(stmt));
            kbStatement.setOriginalTriples(singleton(stmt));

            statements.add(kbStatement);
        }

        log.debug("[{}] Query returned {} statements in {}ms", queryId, statements.size(),
                currentTimeMillis() - startTime);

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
            aConnection.remove(subStmts);
            aConnection.remove(predStmts);
            aConnection.remove(objStmts);
        }
    }

    @Override
    public void deleteStatement(RepositoryConnection aConnection, KnowledgeBase kb,
            KBStatement aStatement)
    {
        aConnection.remove(aStatement.getOriginalTriples());
        aStatement.setOriginalTriples(Collections.emptySet());
    }

    @Override
    public void upsertStatement(RepositoryConnection aConnection, KnowledgeBase kb,
            KBStatement aStatement)
    {
        ValueFactory vf = aConnection.getValueFactory();
        Set<Statement> newTriples = singleton(
                vf.createStatement(vf.createIRI(aStatement.getInstance().getIdentifier()),
                        vf.createIRI(aStatement.getProperty().getIdentifier()),
                        valueMapper.mapStatementValue(aStatement, vf)));

        upsert(aConnection, aStatement.getOriginalTriples(), newTriples);

        aStatement.setOriginalTriples(newTriples);
    }

    @Override
    public void deleteQualifier(RepositoryConnection aConnection, KnowledgeBase kb,
            KBQualifier oldQualifier)
    {
        throw new NotImplementedException("Qualifiers are not supported.");
    }

    @Override
    public void upsertQualifier(RepositoryConnection aConnection, KnowledgeBase kb,
            KBQualifier aQualifier)
    {
        throw new NotImplementedException("Qualifiers are not supported.");
    }

    @Override
    public List<KBQualifier> listQualifiers(RepositoryConnection aConnection, KnowledgeBase kb,
            KBStatement aStatement)
    {
        throw new NotImplementedException("Qualifiers are not supported.");
    }

    @Override
    public boolean exists(RepositoryConnection aConnection, KnowledgeBase akb,
            KBStatement mockStatement)
    {
        ValueFactory vf = aConnection.getValueFactory();
        String QUERY = "SELECT * WHERE { ?s ?p ?o . }";
        TupleQuery tupleQuery = aConnection.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
        tupleQuery.setBinding("s", vf.createIRI(mockStatement.getInstance().getIdentifier()));
        tupleQuery.setBinding("p", vf.createIRI(mockStatement.getProperty().getIdentifier()));

        InceptionValueMapper mapper = new InceptionValueMapper();
        tupleQuery.setBinding("o", mapper.mapStatementValue(mockStatement, vf));

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            return result.hasNext();
        }
    }

    @Override
    public String generatePropertyIdentifier(RepositoryConnection aConn, KnowledgeBase aKB)
    {
        return generateIdentifier(aConn, aKB);
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

    private List<Statement> listStatements(TupleQuery aQuery, boolean aIncludeInferred)
    {
        aQuery.setIncludeInferred(aIncludeInferred);

        try (TupleQueryResult result = aQuery.evaluate()) {
            ValueFactory vf = SimpleValueFactory.getInstance();

            List<Statement> statements = new ArrayList<>();
            while (result.hasNext()) {
                BindingSet bindings = result.next();
                if (bindings.size() == 0) {
                    continue;
                }

                // LOG.trace("[{}] Bindings: {}", toHexString(hashCode()), bindings);

                Binding subj = bindings.getBinding(VAR_SUBJECT_NAME);
                Binding pred = bindings.getBinding(VAR_PREDICATE_NAME);
                Binding obj = bindings.getBinding(VAR_OBJECT_NAME);

                IRI subject = vf.createIRI(subj.getValue().stringValue());
                IRI predicate = vf.createIRI(pred.getValue().stringValue());
                Statement stmt = vf.createStatement(subject, predicate, obj.getValue());

                // Avoid duplicate statements
                if (!statements.contains(stmt)) {
                    statements.add(stmt);
                }
            }
            return statements;
        }
    }
}
