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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.InceptionValueMapper;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

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
    private final KnowledgeBaseService kbService;
    private final InceptionValueMapper valueMapper;
    private final ValueFactory vf;

    public WikiDataReification(KnowledgeBaseService aKbService)
    {
        valueMapper = new InceptionValueMapper();
        vf = SimpleValueFactory.getInstance();

        kbService = aKbService;
        kbService.registerImplicitNamespace(PREDICATE_NAMESPACE);
    }

    @Override
    public List<Statement> reify(KnowledgeBase kb, KBStatement aStatement)
    {
        String statementId = aStatement.getStatementId();
        KBHandle instance = aStatement.getInstance();
        KBHandle property = aStatement.getProperty();
        Value value = valueMapper.mapStatementValue(aStatement, vf);

        IRI subject = vf.createIRI(instance.getIdentifier());
        IRI predicate = vf.createIRI(property.getIdentifier());
        Resource id = vf.createBNode(statementId);

        Statement root = vf.createStatement(subject, predicate, id);
        IRI valuePredicate = vf.createIRI(PREDICATE_NAMESPACE, predicate.getLocalName());
        Statement valueStatement = vf.createStatement(id, valuePredicate, value);

        List<Statement> statements = new ArrayList<>();
        statements.add(root);           // S    P   id
        statements.add(valueStatement); // id   p_s V

        for (KBQualifier aQualifier : aStatement.getQualifiers()) {
            List<Statement> qualifierStatement = reifyQualifier(kb, aQualifier);
            aQualifier.setOriginalStatements(qualifierStatement);
        }

        System.out.println(statements);

        return statements;
    }

    private List<Statement> reifyQualifier(KnowledgeBase kb, KBQualifier aQualifier)
    {
        Resource statementId = vf.createBNode(aQualifier.getKbStatement().getStatementId());
        KBHandle qualifierProperty = aQualifier.getKbProperty();
        IRI qualifierPredicate = vf.createIRI(qualifierProperty.getIdentifier());
        Value qualifierValue = valueMapper.mapQualifierValue(aQualifier, vf);

        Statement qualifierStatement = vf
            .createStatement(statementId, qualifierPredicate, qualifierValue);
        List<Statement> originalStatements = new ArrayList<>();
        originalStatements.add(qualifierStatement); //id P V
        return originalStatements;
    }

    @Override
    public List<KBStatement> listStatements(KnowledgeBase kb, KBHandle aInstance,
        boolean aAll)
    {
        String QUERY = String.join("\n",
            "SELECT DISTINCT ?p ?o ?id ?ps ?l WHERE {",
            "  ?s  ?p  ?id .",
            "  ?id ?ps ?o .",
            "  ?p  ?pLABEL ?l.",
            "  FILTER(STRSTARTS(STR(?ps), STR(?ps_ns)))",
            "}",
            "LIMIT 10000");

        IRI instance = vf.createIRI(aInstance.getIdentifier());
        try (RepositoryConnection conn = kbService.getConnection(kb)) {
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("s", instance);
            tupleQuery.setBinding("ps_ns", vf.createIRI(PREDICATE_NAMESPACE));
            tupleQuery.setBinding("pLABEL", RDFS.LABEL);

            tupleQuery.setIncludeInferred(false);
            TupleQueryResult result;

            try {
                result = tupleQuery.evaluate();
            }
            catch (QueryEvaluationException e) {
                log.warn("Listing statements failed.", e);
                return Collections.emptyList();
            }

            List<KBStatement> statements = new ArrayList<>();

            while (result.hasNext()) {
                BindingSet bindings = result.next();
                Binding p = bindings.getBinding("p");
                Binding o = bindings.getBinding("o");
                Binding id = bindings.getBinding("id");
                Binding ps = bindings.getBinding("ps");
                Binding l = bindings.getBinding("l");
                Value value = o.getValue();

                // Fill kbStatement
                KBHandle property = new KBHandle();
                property.setIdentifier(p.getValue().stringValue());
                property.setName(l.getValue().stringValue());
                KBStatement kbStatement = new KBStatement(aInstance, property, value);

                // Recreate original statements
                Resource idResource = vf.createBNode(id.getValue().stringValue());
                kbStatement.setStatementId(idResource.stringValue());
                IRI predicate = vf.createIRI(property.getIdentifier());
                Statement root = vf.createStatement(instance, predicate, idResource);

                IRI valuePredicate = vf.createIRI(ps.getValue().stringValue());
                Statement valueStatement = vf.createStatement(idResource, valuePredicate, value);
                List<Statement> originalStatements = new ArrayList<>();

                originalStatements.add(root);
                originalStatements.add(valueStatement);
                kbStatement.setOriginalStatements(originalStatements);

                List<KBQualifier> qualifiers = listQualifiers(kb, kbStatement);
                kbStatement.setQualifiers(qualifiers);

                statements.add(kbStatement);
            }
            return statements;
        }
    }

    private List<Statement> getStatementsById(KnowledgeBase kb, String aStatementId)
    {
        String QUERY = String
            .join("\n",
                "SELECT DISTINCT ?s ?p ?ps ?o WHERE {",
                "  ?s  ?p  ?id .",
                "  ?id ?ps ?o .",
                "  FILTER(STRSTARTS(STR(?ps), STR(?ps_ns)))",
                "}",
                "LIMIT 10");
        Resource id = vf.createBNode(aStatementId);
        try (RepositoryConnection conn = kbService.getConnection(kb)) {
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
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
    }

    private List<Statement> getQualifiersById(KnowledgeBase kb, String aStatementId)
    {
        String QUERY = String.join("\n",
                "SELECT DISTINCT ?p ?o WHERE {",
            "  ?id ?p ?o .",
            "}",
            "LIMIT 10000");
        Resource id = vf.createBNode(aStatementId);
        try (RepositoryConnection conn = kbService.getConnection(kb)) {
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
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

    }

    @Override
    public void deleteStatement(KnowledgeBase kb, KBStatement aStatement)
    {
        String statementId = aStatement.getStatementId();
        if (statementId != null) {
            kbService.update(kb, (conn) -> {
                conn.remove(getStatementsById(kb, statementId));
                conn.remove(getQualifiersById(kb, statementId));
                aStatement.setOriginalStatements(Collections.emptyList());
                aStatement.setQualifiers(Collections.emptyList());
                return null;
            });
        }
    }

    @Override
    public void upsertStatement(KnowledgeBase kb, KBStatement aStatement)
    {
        kbService.update(kb, (conn) -> {
            KBStatement statement = aStatement;
            String statementId = statement.getStatementId();
            if (statementId != null) {
                //remove old statements by id
                conn.remove(getStatementsById(kb, statement.getStatementId()));
            }
            else {
                statementId = vf.createBNode().stringValue();
            }
            //add new statements
            IRI subject = vf.createIRI(statement.getInstance().getIdentifier());
            IRI predicate = vf.createIRI(statement.getProperty().getIdentifier());
            Resource id = vf.createBNode(statementId);

            Statement root = vf.createStatement(subject, predicate, id);
            IRI valuePredicate = vf.createIRI(PREDICATE_NAMESPACE, predicate.getLocalName());
            Value value = valueMapper.mapStatementValue(statement, vf);
            Statement valueStatement = vf.createStatement(id, valuePredicate, value);

            conn.add(root);
            conn.add(valueStatement);
            aStatement.setStatementId(statementId);

            List<Statement> statements = new ArrayList<>();
            statements.add(root);
            statements.add(valueStatement);
            aStatement.setOriginalStatements(statements);
            return null;
        });
    }

    @Override
    public void addQualifier(KnowledgeBase kb, KBQualifier newQualifier)
    {
        if (newQualifier.getKbStatement().getStatementId() == null) {
            log.error("No statementId");
        }
        else {
            kbService.update(kb, (conn) -> {
                Resource id = vf.createBNode(newQualifier.getKbStatement().getStatementId());
                IRI predicate = vf.createIRI(newQualifier.getKbProperty().getIdentifier());
                Value value = valueMapper.mapQualifierValue(newQualifier, vf);
                Statement qualifierStatement = vf.createStatement(id, predicate, value);
                conn.add(qualifierStatement);

                List<Statement> statements = new ArrayList<>();
                statements.add(qualifierStatement);
                newQualifier.setOriginalStatements(statements);
                newQualifier.getKbStatement().getQualifiers().add(newQualifier);
                return null;
            });
        }
    }

    @Override
    public void deleteQualifier(KnowledgeBase kb, KBQualifier oldQualifier)
    {
        if (oldQualifier.getKbStatement().getStatementId() == null) {
            log.error("No statementId");
        }
        else {
            kbService.update(kb, (conn) -> {
                conn.remove(oldQualifier.getOriginalStatements());

                oldQualifier.getKbStatement().getQualifiers().remove(oldQualifier);
                oldQualifier.setOriginalStatements(Collections.emptyList());
                return null;
            });
        }
    }

    @Override
    public void upsertQualifier(KnowledgeBase kb, KBQualifier aQualifier)
    {
        kbService.update(kb, (conn) -> {
            int index = aQualifier.getQualifierIndexByOriginalStatements();
            List<Statement> statements = reifyQualifier(kb, aQualifier);
            conn.add(statements);
            if (index == -1) {
                aQualifier.setOriginalStatements(statements);
                aQualifier.getKbStatement().getQualifiers().add(aQualifier);
            }
            else {
                conn.remove(aQualifier.getOriginalStatements());
                aQualifier.setOriginalStatements(statements);
                aQualifier.getKbStatement().getQualifiers().set(index, aQualifier);
            }
            return null;
        });
    }

    @Override
    public List<KBQualifier> listQualifiers(KnowledgeBase kb, KBStatement aStatement)
    {
        List<KBQualifier> qualifiers = new ArrayList<>();
        String QUERY = String.join("\n",
            "SELECT DISTINCT ?p ?o ?l WHERE {",
            "  ?id ?p ?o .",
            "  OPTIONAL {",
            "    ?p ?pLABEL ?l .",
            "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\"))",
            "  }",
            "}",
            "LIMIT 10000");
        Resource id = vf.createBNode(aStatement.getStatementId());
        try (RepositoryConnection conn = kbService.getConnection(kb)) {
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("id", id);
            tupleQuery.setBinding("pLABEL", RDFS.LABEL);

            tupleQuery.setIncludeInferred(false);
            TupleQueryResult result;

            try {
                result = tupleQuery.evaluate();
            }
            catch (QueryEvaluationException e) {
                log.warn("No such statementId in knowledge base", e);
                return null;
            }

            while (result.hasNext()) {
                BindingSet bindings = result.next();
                Binding p = bindings.getBinding("p");
                Binding o = bindings.getBinding("o");
                Binding l = bindings.getBinding("l");

                if (!p.getValue().stringValue().contains(PREDICATE_NAMESPACE)) {
                    KBHandle property = new KBHandle();
                    property.setIdentifier(p.getValue().stringValue());
                    property.setName(l.getValue().stringValue());
                    Value value = o.getValue();
                    KBQualifier qualifier = new KBQualifier(aStatement, property, value);

                    IRI predicate = vf.createIRI(p.getValue().stringValue());
                    Value object = o.getValue();
                    Statement qualifierStatement = vf.createStatement(id, predicate, object);

                    List<Statement> statements = new ArrayList<>();
                    statements.add(qualifierStatement);
                    qualifier.setOriginalStatements(statements);

                    qualifiers.add(qualifier);
                }
            }
            return qualifiers;
        }
    }

    @Override
    public boolean statementsMatchSPO(KnowledgeBase akb, KBStatement mockStatement)
    {
        try (RepositoryConnection conn = kbService.getConnection(akb)) {
            ValueFactory vf = conn.getValueFactory();
            String QUERY = String
                .join("\n",
                    "SELECT * WHERE {",
                    "  ?s  ?p  ?id .",
                    "  ?id ?ps ?o .",
                    "  FILTER(STRSTARTS(STR(?ps), STR(?ps_ns)))",
                    "}",
                    "LIMIT 10");
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("s", vf.createIRI(mockStatement.getInstance().getIdentifier()));
            tupleQuery.setBinding("p", vf.createIRI(mockStatement.getProperty().getIdentifier()));

            InceptionValueMapper mapper = new InceptionValueMapper();
            tupleQuery.setBinding("o", mapper.mapStatementValue(mockStatement, vf));
            tupleQuery.setBinding("ps_ns", vf.createIRI(PREDICATE_NAMESPACE));

            try (TupleQueryResult result = tupleQuery.evaluate()) {
                return result.hasNext();
            }
        }
    }
}
