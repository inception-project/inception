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
package de.tudarmstadt.ukp.inception.conceptlinking.util;

import java.util.List;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Contains SPARQL query parts and query builder methods
 */
public class QueryUtil
{

    private static final String SPARQL_PREFIX = String.join("\n",
            "PREFIX e:<http://www.wikidata.org/entity/>",
            "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>",
            "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>",
            "PREFIX base:<http://www.wikidata.org/ontology#>",
            "PREFIX schema: <http://schema.org/>");

    private static final String INSTANCES = "<http://wikidata.org/instances>";
    private static final String STATEMENTS = "<http://wikidata.org/statements>";
    private static final String TERMS = "<http://wikidata.org/terms>";

    /**
     * instance of this page are about some Wikimedia-only content and should not refer to external World entities
     */
    private static final String WIKIMEDIA_INTERNAL = "e:Q17442446";

    /**
     *
     * @param tokens the words spanned by the mention
     * @param limit maximum number of results
     * @return a query to retrieve candidate entities
     */
    public static TupleQuery generateCandidateQuery(RepositoryConnection conn, List<String>
        tokens, int limit)
    {

        String query = String.join("\n",
            "DEFINE input:inference 'instances'",
            SPARQL_PREFIX,
            "SELECT DISTINCT ?e2 ?altLabel ?label WHERE",
            "{",
            "  {",
            "    {",
            "      VALUES ?labelpredicate {rdfs:label skos:altLabel}",
            "      GRAPH " + TERMS,
            "      {",
            "        ?e2 ?labelpredicate ?altLabel.",
            "        ?altLabel bif:contains '?entityLabel'. ",
            "      }",
            "    }",
            "  }",
            "  FILTER EXISTS { GRAPH " + STATEMENTS + " { ?e2 ?p ?v }}",
            "  FILTER NOT EXISTS ",
            "  {",
            "    VALUES ?topic {" + WIKIMEDIA_INTERNAL + "}",
            "    GRAPH " + INSTANCES + " {?e2 rdf:type ?topic}",
            "  }",
            "  BIND (STRLEN(?altLabel) as ?len)",
            "  {",
            "    GRAPH " + STATEMENTS + " { ?e2 rdfs:label ?label. }",
            "    FILTER ( lang(?label) = \"en\" )",
            "  }",
            "}",
            "LIMIT " + limit);

        ValueFactory vf = SimpleValueFactory.getInstance();
        Literal tokensJoined = vf.createLiteral(String.join(" ",tokens));

        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        tupleQuery.setBinding("entityLabel", tokensJoined);
        return tupleQuery;
    }

    /**
     *
     * @param wikidataId wikidataId
     * @param limit maximum number of results
     * @return a query to retrieve the semantic signature
     */
    public static TupleQuery generateSemanticSignatureQuery(RepositoryConnection conn, String
        wikidataId, int limit)
    {

        ValueFactory vf = SimpleValueFactory.getInstance();
        Literal id = vf.createLiteral("e:"+wikidataId);

        String query = String.join("\n",
            SPARQL_PREFIX,
            "SELECT DISTINCT ?label ?p ?e1 WHERE ",
            "  {",
            "    {",
            "      {",
            "        GRAPH " + STATEMENTS,
            "          { ?e1  ?rd ?m . ?m ?p ?e2 . }",
            "      }",
            "      UNION",
            "      {",
            "        GRAPH " + STATEMENTS,
            "          { ?e2 ?p ?m . ?m ?rr ?e1 . }",
            "      }",
            "    }",
            "    {",
            "      GRAPH " + TERMS + " { ?e1 rdfs:label ?label. }",
            "      FILTER ( lang(?label) = \"en\" )",
            "    }",
            "  }",
            " LIMIT " + limit);

        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        tupleQuery.setBinding("e2", id);
        return tupleQuery;
    }

}
