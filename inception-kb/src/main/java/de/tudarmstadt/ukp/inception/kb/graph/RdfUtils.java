/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.kb.graph;

import java.util.Optional;

import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import de.tudarmstadt.ukp.inception.kb.InferencerVariableStore;
import de.tudarmstadt.ukp.inception.kb.IriConstants;

public class RdfUtils
{
    public static Optional<Value> readFirstValue(RepositoryConnection conn, Resource subj, IRI pred,
            Resource... contexts)
    {
        Optional<Statement> statement = readFirst(conn, subj, pred, null);
        if (statement.isPresent()) {
            return Optional.of(statement.get().getObject());
        }
        else {
            return Optional.empty();
        }
    }

    public static Optional<Statement> readFirst(RepositoryConnection conn, Resource subj, IRI pred,
            Value obj, String language)
    {
        try (RepositoryResult<Statement> results =
            getStatements(conn, subj, pred, obj, false, language)) {
            if (results.hasNext()) {
                return Optional.of(results.next());
            }
            else {
                return Optional.empty();
            }
        }
    }

    public static Optional<Statement> readFirst(RepositoryConnection conn, Resource subj, IRI pred,
        Value obj)
    {
        return readFirst(conn, subj, pred, obj, null);
    }

    public static RepositoryResult<Statement> getStatements(RepositoryConnection conn,
        Resource subj, IRI pred, Value obj, boolean includeInferred)
    {
        return getStatementsSparql(conn, subj, pred, obj, 1000, includeInferred, null);
    }

    public static RepositoryResult<Statement> getStatements(RepositoryConnection conn,
        Resource subj, IRI pred, Value obj, boolean includeInferred, String language)
    {
        return getStatementsSparql(conn, subj, pred, obj, 1000, includeInferred, language);
    }

    
    public static RepositoryResult<Statement> getStatementsNormal(RepositoryConnection conn,
            Resource subj, IRI pred, Value obj, boolean includeInferred)
            throws QueryEvaluationException {
        return conn.getStatements(subj, pred, obj, includeInferred);
    }

    public static RepositoryResult<Statement> getStatementsSparql(RepositoryConnection conn,
            Resource subj, IRI pred, Value obj, int aLimit, boolean includeInferred,
            String language)
        throws QueryEvaluationException
    {
        String filter = "";
        if (language != null) {
            filter = "FILTER(LANG(?o) = \"\" || LANGMATCHES(LANG(?o), \"" + NTriplesUtil
                .escapeString(language) + "\")).";
        }
        String QUERY = String.join("\n",
            "SELECT * WHERE { ",
            "?s ?p ?o ",
            filter,
            "} LIMIT 1000");
        
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
        if (subj != null) {
            tupleQuery.setBinding("s", subj);
        }
        if (pred != null) {
            tupleQuery.setBinding("p", pred);
        }
        if (obj != null) {
            tupleQuery.setBinding("o", obj);
        }
        tupleQuery.setIncludeInferred(includeInferred);
        TupleQueryResult result = tupleQuery.evaluate();
        Iteration<Statement, QueryEvaluationException> i1 = 
                new ConvertingIteration<BindingSet, Statement, QueryEvaluationException>(result)
        {
            @Override
            protected Statement convert(BindingSet b) throws QueryEvaluationException
            {
                Resource s = subj == null ? (Resource) b.getValue("s") : subj;
                IRI p = pred == null ? (IRI) b.getValue("p") : pred;
                Value o = obj == null ? b.getValue("o") : obj;

                return SimpleValueFactory.getInstance().createStatement(s, p, o);
            }
        };
        
        ExceptionConvertingIteration<Statement, RepositoryException> i2 = 
                new ExceptionConvertingIteration<Statement, RepositoryException>(i1)
        {
            @Override
            protected RepositoryException convert(Exception aE)
            {
                return new RepositoryException(aE);
            }
        };
        
        return new RepositoryResult<Statement>(i2);
    }
    
    public static RepositoryResult<Statement> getPropertyStatementsSparql(RepositoryConnection conn,
            Resource subj, IRI pred, Value obj, int aLimit, boolean includeInferred,
            String language)
        throws QueryEvaluationException
    {
        
        String filter = "";
        if (language != null) {
            filter = "FILTER(LANG(?o) = \"\" || LANGMATCHES(LANG(?o), \"" + NTriplesUtil
                .escapeString(language) + "\")).";
        }
        String QUERY = String.join("\n",
            InferencerVariableStore.PREFIX_OWL,
            InferencerVariableStore.PREFIX_RDF,
            InferencerVariableStore.PREFIX_RDFS,
            "SELECT * WHERE { ",
            " {?s ?p ?o .}",
            " UNION ",
            " {?s a ?prop .",
            "    VALUES ?prop { rdf:Property owl:ObjectProperty owl:DatatypeProperty owl:AnnotationProperty} }",
            filter,
            "} LIMIT 1000");
        System.out.println(QUERY);
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
        if (subj != null) {
            tupleQuery.setBinding("s", subj);
        }
        if (pred != null) {
            tupleQuery.setBinding("p", pred);
        }
        if (obj != null) {
            tupleQuery.setBinding("o", obj);
        }
        tupleQuery.setIncludeInferred(includeInferred);
        TupleQueryResult result = tupleQuery.evaluate();
        Iteration<Statement, QueryEvaluationException> i1 = 
                new ConvertingIteration<BindingSet, Statement, QueryEvaluationException>(result)
        {
            @Override
            protected Statement convert(BindingSet b) throws QueryEvaluationException
            {
                Resource s = subj == null ? (Resource) b.getValue("s") : subj;
                IRI p = pred == null ? (IRI) b.getValue("p") : pred;
                Value o = obj == null ? b.getValue("o") : obj;

                return SimpleValueFactory.getInstance().createStatement(s, p, o);
            }
        };
        
        ExceptionConvertingIteration<Statement, RepositoryException> i2 = 
                new ExceptionConvertingIteration<Statement, RepositoryException>(i1)
        {
            @Override
            protected RepositoryException convert(Exception aE)
            {
                return new RepositoryException(aE);
            }
        };
        
        return new RepositoryResult<Statement>(i2);
    }
    
    
    
    public static boolean existsStatementsWithSubject(
            RepositoryConnection conn, Resource subj, boolean includeInferred)
    {
        try (RepositoryResult<Statement> stmts = getStatementsSparql(conn, subj, null, null, 1,
                includeInferred, null)) {
            return !Iterations.asList(stmts).isEmpty();
        }
    }
    

    /**
     * Get all statements about the given subject.
     * 
     * @param conn
     *            a repository connection
     * @param subj
     *            the subject resource
     * @param includeInferred
     *            whether to include inferred statements
     * @return all statements with the given subject resource
     */
    public static RepositoryResult<Statement> getStatementsWithSubject(
            RepositoryConnection conn, Resource subj, boolean includeInferred)
    {
        return getStatementsSparql(conn, subj, null, null, 1000, includeInferred, null);
    }
    
    public static boolean isFromImplicitNamespace(KBHandle handle) {
        return IriConstants.IMPLICIT_NAMESPACES.stream()
                .anyMatch(ns -> handle.getIdentifier().startsWith(ns));
    }
}
