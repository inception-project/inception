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

import java.util.Locale;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.queryrender.RenderUtils;
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
            "PREFIX base:<http://www.wikidata.org/ontology#>");

    private static final String INSTANCES = "<http://wikidata.org/instances>";
    private static final String STATEMENTS = "<http://wikidata.org/statements>";
    private static final String TERMS = "<http://wikidata.org/terms>";

    /*
     * instance of this page are about some Wikimedia-only content and should not refer to external
     * World entities
     */
    private static final String WIKIMEDIA_INTERNAL = "e:Q17442446";

    /*
     * page in various non-article namespaces on a Wikimedia project
     */
    private static final String WIKIMEDIA_PROJECT_PAGE = "e:Q14204246";

    private static final String WIKIMEDIA_DISAMBIGUATION_PAGE = "e:Q4167410";

    private static final String WIKIMEDIA_CATEGORY = "e:Q4167836";

    /*
     * page of a Wikimedia project with a list of something
     */
    private static final String WIKIMEDIA_LIST_ARTICLE = "e:Q13406463";

    private static final String WIKIMEDIA_TEMPLATE = "e:Q11266439";

    private static final String WIKIMEDIA_NEWS_ARTICLE = "e:Q17633526";

    private static final String WIKIMEDIA_NAVIGATIONAL_TEMPLATE = "e:Q11753321";

    private static String getExactMatchingQueryPart(String aString)
    {
        return String.join("\n",
            "    SELECT DISTINCT ?e2 ?description WHERE",
            "    {",
            "     VALUES ?labelpredicate {rdfs:label skos:altLabel}",
            "      {",
            "        ?e2 ?labelpredicate ?" + aString + " @en .",
            "        OPTIONAL",
            "        {",
            "          ?e2 ?descriptionIri ?description.",
            "          FILTER ( lang(?description) = \"en\" )",
            "        }",
            "      }",
            "    }");
    }

    /**
     * This query retrieves candidates via exact matching of their labels and full-text-search
     * It has been tied to use LCASE in combination with FILTER to allow matching the lower cased
     * arguments with the entities from the KB, but that was too time-intensive and lead to
     * timeouts.
     *
     * Therefore, one still needs to type with correct capitalization in order to retrieve the
     * desired result.
     *
     * @param aTypedString typed string from the user
     * @param aMention the marked surface form
     * @param aDescriptionIri KB-specific IRI that indicates a description
     * @return a query to retrieve candidate entities
     */
    public static TupleQuery generateCandidateExactQuery(RepositoryConnection conn,
        String aTypedString, String aMention, IRI aDescriptionIri, IRI aFtsIri)
    {
        aTypedString = RenderUtils.escape(aTypedString);
        aMention = RenderUtils.escape(aMention);

        // Matching user input exactly
        String exactMatchingTypedString = getExactMatchingQueryPart("exactTyped");

        // Match surface form exactly
        String exactMatchingMention = getExactMatchingQueryPart("exactMention");

        String query = String.join("\n",
            SPARQL_PREFIX,
            "SELECT DISTINCT ?e2 ?label ?description WHERE",
            "{",
            "  {",
            exactMatchingTypedString,
            "  } ",
            "  UNION",
            "  {",
            exactMatchingMention,
            "  }",
            "  FILTER EXISTS { ?e2 ?p ?v }",
            "  FILTER NOT EXISTS ",
            "  {",
            "    VALUES ?topic {" + String.join(" ", WIKIMEDIA_INTERNAL,
                WIKIMEDIA_PROJECT_PAGE, WIKIMEDIA_CATEGORY, WIKIMEDIA_DISAMBIGUATION_PAGE,
                WIKIMEDIA_LIST_ARTICLE, WIKIMEDIA_TEMPLATE, WIKIMEDIA_NEWS_ARTICLE,
                WIKIMEDIA_NAVIGATIONAL_TEMPLATE) +
                "}",
            "    ?e2 rdf:type ?topic",
            "  }",
            "  ?e2 rdfs:label ?label.",
            "  FILTER ( lang(?label) = \"en\" )",
            "}");

        ValueFactory vf = SimpleValueFactory.getInstance();

        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        tupleQuery.setBinding("exactTyped", vf.createLiteral(aTypedString));
        tupleQuery.setBinding("exactMention", vf.createLiteral(aMention));

        tupleQuery.setBinding("descriptionIri", aDescriptionIri);
        tupleQuery.setBinding("ftsIri", aFtsIri);
        return tupleQuery;
    }

    private static String getFullTextMatchingQueryPart(String aString, int aLimit) {
        return  String.join("\n",
            "    SELECT DISTINCT ?e2 ?altLabel ?description WHERE",
            "    {",
            "      VALUES ?labelpredicate {rdfs:label skos:altLabel}",
            "      {",
            "        ?e2 ?labelpredicate ?altLabel.",
            "        ?altLabel ?ftsIri '?" + aString + "'. ",
            "        OPTIONAL",
            "        {",
            "          ?e2 ?descriptionIri ?description.",
            "          FILTER ( lang(?description) = \"en\" )",
            "        }",
            "      }",
            "    }",
            "    LIMIT " + aLimit);
    }

    /**
     *
     * This query retrieves candidates via full-text matching of their labels and full-text-search
     *
     * @param aString String for which to perform full text search
     * @param aLimit maximum number of results
     * @param aDescriptionIri KB-specific IRI that indicates a description
     * @return a query to retrieve candidate entities
     */
    public static TupleQuery generateCandidateFullTextQuery(RepositoryConnection conn,
        String aString, int aLimit, IRI aDescriptionIri, IRI aFtsIri)
    {
        aString = RenderUtils.escape(aString).toLowerCase(Locale.ENGLISH);

        String fullTextMatchingString = getFullTextMatchingQueryPart("string", aLimit);

        String query = String.join("\n",
            "DEFINE input:inference 'instances'",
            SPARQL_PREFIX,
            "SELECT DISTINCT ?e2 ?altLabel ?label ?description WHERE",
            "{",
            "  {",
                 fullTextMatchingString,
            "  }",
            "  FILTER EXISTS { ?e2 ?p ?v }",
            "  FILTER NOT EXISTS ",
            "  {",
            "    VALUES ?topic {" + String.join(" ", WIKIMEDIA_INTERNAL,
                WIKIMEDIA_PROJECT_PAGE, WIKIMEDIA_CATEGORY, WIKIMEDIA_DISAMBIGUATION_PAGE,
                WIKIMEDIA_LIST_ARTICLE, WIKIMEDIA_TEMPLATE, WIKIMEDIA_NEWS_ARTICLE,
                WIKIMEDIA_NAVIGATIONAL_TEMPLATE) +
                "}",
            "    ?e2 rdf:type ?topic",
            "  }",
            "  ?e2 rdfs:label ?label.",
            "  FILTER ( lang(?label) = \"en\" )",
            "}");

        ValueFactory vf = SimpleValueFactory.getInstance();

        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        tupleQuery.setBinding("string", vf.createLiteral(aString));
        tupleQuery.setBinding("descriptionIri", aDescriptionIri);
        tupleQuery.setBinding("ftsIri", aFtsIri);
        return tupleQuery;
    }

    /**
     *
     * @param wikidataId wikidataId, e.g. "Q3"
     * @param limit maximum number of results
     * @return a query to retrieve the semantic signature
     */
    public static TupleQuery generateSemanticSignatureQuery(RepositoryConnection conn, String
        wikidataId, int limit)
    {
        ValueFactory vf = SimpleValueFactory.getInstance();
        IRI iri = vf.createIRI("http://www.wikidata.org/entity/" + wikidataId);
        String query = String.join("\n",
            SPARQL_PREFIX,
            "SELECT DISTINCT ?label ?p WHERE ",
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
        tupleQuery.setBinding("e2", iri);
        return tupleQuery;
    }
}
