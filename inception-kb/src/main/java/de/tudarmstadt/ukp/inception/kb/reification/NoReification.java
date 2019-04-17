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

import static java.util.Collections.singleton;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import de.tudarmstadt.ukp.inception.kb.InceptionValueMapper;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder;

public class NoReification
    implements ReificationStrategy
{
    private final InceptionValueMapper valueMapper = new InceptionValueMapper();
    
    @Override
    public List<KBStatement> listStatements(RepositoryConnection aConnection, KnowledgeBase aKB,
            KBHandle aItem, boolean aAll)
    {
        return SPARQLQueryBuilder.forItems(aKB)
                .withIdentifier(aItem.getIdentifier())
                .asStatements(aConnection, aAll);
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
        Set<Statement> newTriples = singleton(vf.createStatement(
                vf.createIRI(aStatement.getInstance().getIdentifier()), 
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
}
