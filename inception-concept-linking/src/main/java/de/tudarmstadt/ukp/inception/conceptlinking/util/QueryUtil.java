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

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

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
            "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>");

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
            "    SELECT DISTINCT ?iri ?description WHERE",
            "    {",
            "      ?iri ?labelIri " + aString + " .",
            "      OPTIONAL",
            "      {",
            "        ?iri ?descriptionIri ?description.",
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
     * @param aKb the Knowledge Base
     * @return a query to retrieve candidate entities
     */
    public static TupleQuery generateCandidateExactQuery(RepositoryConnection conn,
        String aTypedString, String aMention, KnowledgeBase aKb)
    {
        aTypedString = RenderUtils.escape(aTypedString);
        aMention = RenderUtils.escape(aMention);

        // Matching user input exactly
        String exactMatchingTypedString = getExactMatchingQueryPart("?exactTyped");

        // Match surface form exactly
        String exactMatchingMention = getExactMatchingQueryPart("?exactMention");

        String query = String.join("\n",
            SPARQL_PREFIX,
            "SELECT DISTINCT ?iri ?label ?description WHERE",
            "{",
            "  {",
            exactMatchingTypedString,
            "  } ",
            "  UNION",
            "  {",
            exactMatchingMention,
            "  }",
            "  FILTER EXISTS { ?iri ?p ?v }",
            "  FILTER NOT EXISTS ",
            "  {",
            "    VALUES ?topic {" + String.join(" ", WIKIMEDIA_INTERNAL,
                WIKIMEDIA_PROJECT_PAGE, WIKIMEDIA_CATEGORY, WIKIMEDIA_DISAMBIGUATION_PAGE,
                WIKIMEDIA_LIST_ARTICLE, WIKIMEDIA_TEMPLATE, WIKIMEDIA_NEWS_ARTICLE,
                WIKIMEDIA_NAVIGATIONAL_TEMPLATE) + "}",
            "    ?iri ?typeIri ?topic",
            "  }",
            "  ?iri ?labelIri ?label.",
            "}");

        ValueFactory vf = SimpleValueFactory.getInstance();

        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        tupleQuery.setBinding("exactTyped", vf.createLiteral(aTypedString));
        tupleQuery.setBinding("exactMention", vf.createLiteral(aMention));
        tupleQuery.setBinding("language", vf.createLiteral((aKb.getDefaultLanguage() != null)
            ? aKb.getDefaultLanguage() : "en"));

        tupleQuery.setBinding("labelIri", aKb.getLabelIri());
        tupleQuery.setBinding("typeIri", aKb.getTypeIri());
        tupleQuery.setBinding("descriptionIri", aKb.getDescriptionIri());
        return tupleQuery;
    }

    private static String getFullTextMatchingQueryPartDefault(int aLimit)
    {
        return  String.join("\n",
            "    SELECT DISTINCT ?iri ?altLabel ?description WHERE",
            "    {",
            "      ?iri ?labelIri ?altLabel.",
            "      ?altLabel ?ftsIri ?string. ",
            "      OPTIONAL",
            "      {",
            "        ?iri ?descriptionIri ?description.",
            "      }",
            "    }",
            "    LIMIT " + aLimit);
    }

    // http://culturecloud.ru/resource/Help:Search#Full_Text_Search
    private static String getFullTextMatchingQueryPartLucene(int aLimit)
    {
        return  String.join("\n",
            "    SELECT DISTINCT ?iri ?altLabel ?description WHERE",
            "    {",
            "      {  ?iri ?a ?concept .",
            "          FILTER NOT EXISTS { ?iri ?labelIri [] }",  // only concepts without label
            "          FILTER regex( str(?iri) , ?string, \"i\") ",  // case-insensitive matching
            "      }",
            "      UNION",
            "      {",
            "        ?iri search:matches [",
            "              search:query ?string ;",
            "              search:property ?labelIri ;",
            "              search:snippet ?altLabel ;",
            "            ]",
            "      }",
            "      OPTIONAL",
            "      {",
            "        ?iri ?descriptionIri ?description.",
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
     * @param aKb the Knowledge Base
     * @return a query to retrieve candidate entities
     */
    public static TupleQuery generateCandidateFullTextQuery(RepositoryConnection conn,
        String aString, int aLimit, KnowledgeBase aKb)
    {
        String string = RenderUtils.escape(aString).toLowerCase(Locale.ENGLISH);
        ValueFactory vf = SimpleValueFactory.getInstance();
        Literal searchLiteral;
        String fullTextMatchingString;

        if (aKb.getFullTextSearchIri().equals(IriConstants.FTS_LUCENE)) {
            fullTextMatchingString = getFullTextMatchingQueryPartLucene(aLimit);
            // add wildcard '*' to perform wildcard search
            searchLiteral = vf.createLiteral(string + "*");
        } else {
            fullTextMatchingString = getFullTextMatchingQueryPartDefault(aLimit);
            searchLiteral = vf.createLiteral(string);
        }

        String query = String.join("\n",
            SPARQL_PREFIX,
            "SELECT DISTINCT ?iri ?altLabel ?label ?description WHERE",
            "{",
            "  {",
                 fullTextMatchingString,
            "  }",
            "  OPTIONAL ",  // makes it possible to get concepts without label
            "  {",
            "    ?iri ?labelIri ?label.",
            "  }",
            "}");


        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        tupleQuery.setBinding("string", searchLiteral);
        tupleQuery.setBinding("language", vf.createLiteral((aKb.getDefaultLanguage() != null)
            ? aKb.getDefaultLanguage() : "en"));
        tupleQuery.setBinding("labelIri", aKb.getLabelIri());
        tupleQuery.setBinding("typeIri", aKb.getTypeIri());
        tupleQuery.setBinding("descriptionIri", aKb.getDescriptionIri());
        tupleQuery.setBinding("ftsIri", aKb.getFullTextSearchIri());
        return tupleQuery;
    }

    /**
     *
     * @param aIri an IRI, e.g. "http://www.wikidata.org/entity/Q3"
     * @param aLimit maximum number of results
     * @param aKb the Knowledge Base
     * @return a query to retrieve the semantic signature
     */
    public static TupleQuery generateSemanticSignatureQuery(RepositoryConnection conn, String aIri,
        int aLimit, KnowledgeBase aKb)
    {
        ValueFactory vf = SimpleValueFactory.getInstance();
        String query = String.join("\n",
            SPARQL_PREFIX,
            "SELECT DISTINCT ?label ?p WHERE ",
            "  {",
            "    { ?other  ?rd ?m . ?m ?p ?iri . }",
            "    UNION",
            "    { ?iri ?p ?m . ?m ?rr ?other . }",
            "    ?other ?labelIri ?label. ",
            "  }",
            " LIMIT " + aLimit);

        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        tupleQuery.setBinding("language", vf.createLiteral((aKb.getDefaultLanguage() != null)
            ? aKb.getDefaultLanguage() : "en"));
        tupleQuery.setBinding("iri", vf.createIRI(aIri));
        tupleQuery.setBinding("labelIri", aKb.getLabelIri());
        return tupleQuery;
    }
}
