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

import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.SPARQLQueryStore;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class RdfUtils
{
    public static Optional<Statement> readFirst(RepositoryConnection conn, Resource subj, IRI pred,
            Value obj, String language, KnowledgeBase aKB)
    {
        try (RepositoryResult<Statement> results =
            getStatementsSparql(conn, subj, pred, obj, false, language, aKB)) {
            if (results.hasNext()) {
                return Optional.of(results.next());
            }
            else {
                return Optional.empty();
            }
        }
    }

    public static Optional<Statement> readFirst(RepositoryConnection conn, Resource subj, IRI pred,
        Value obj, KnowledgeBase aKB)
    {
        return readFirst(conn, subj, pred, obj, null, aKB);
    }

    public static RepositoryResult<Statement> getStatementsSparql(RepositoryConnection conn,
            Resource subj, IRI pred, Value obj, boolean includeInferred,
            String language, KnowledgeBase aKB)
        throws QueryEvaluationException
    {

        String QUERY = SPARQLQueryStore.queryForStatementLanguageFiltered(aKB, language);
        
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

        ExceptionConvertingIteration eci = getExceptionConvertingIteration(result, "s", "p", "o");

        return new RepositoryResult<Statement>(eci);
    }

    private static ExceptionConvertingIteration getExceptionConvertingIteration(
        TupleQueryResult aResult, String aSubjBinding, String aPredBinding, String aObjBinding)
    {
        Iteration<Statement, QueryEvaluationException> i1 = new ConvertingIteration<>(aResult)
        {
            @Override protected Statement convert(BindingSet b) throws QueryEvaluationException
            {
                Resource s = (Resource) b.getValue(aSubjBinding);
                IRI p = (IRI) b.getValue(aPredBinding);
                Value o = b.getValue(aObjBinding);

                return SimpleValueFactory.getInstance().createStatement(s, p, o);
            }
        };

        ExceptionConvertingIteration<Statement, RepositoryException> i2 =
            new ExceptionConvertingIteration<>(i1)
        {
            @Override protected RepositoryException convert(Exception aE)
            {
                return new RepositoryException(aE);
            }
        };

        return i2;
    }
    
    public static boolean isFromImplicitNamespace(KBHandle handle) {
        return IriConstants.IMPLICIT_NAMESPACES.stream()
                .anyMatch(ns -> handle.getIdentifier().startsWith(ns));
    }
}
