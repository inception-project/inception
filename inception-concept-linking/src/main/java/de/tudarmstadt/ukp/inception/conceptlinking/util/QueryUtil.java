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

import org.eclipse.rdf4j.model.IRI;

/**
 * Contains SPARQL query parts and query builder methods
 */
public class QueryUtil
{

    private static String SPARQL_INFERENCE_CLAUSE = "DEFINE input:inference 'instances'\n";

    private static String SPARQL_PREFIX = "PREFIX e:<http://www.wikidata.org/entity/>\n"
            + "        PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
            + "        PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
            + "        PREFIX base:<http://www.wikidata.org/ontology#>\n"
            + "        PREFIX schema: <http://schema.org/>\n";

    private static String SPARQL_SELECT = "SELECT DISTINCT %queryvariables% \n WHERE\n";
    
    // TODO add language parameter 
    private static String SPARQL_ENTITY_LABEL =
              "             {\n"
            + "                  {GRAPH <http://wikidata.org/statements> { \n"
            + "                      ?e2 e:P1549s/e:P1549v \"%demonym\"@%language\n"
            + "                      }\n"
            + "                  }\n"
            + "                  UNION\n"
            + "                  {VALUES ?labelpredicate {rdfs:label skos:altLabel}\n"
            + "                      GRAPH <http://wikidata.org/terms> {\n"
            + "                          ?e2 ?labelpredicate ?altLabel. \n"
            + "                          ?altLabel bif:contains '\"%entitylabel\"'@%language. \n"
            + "                      }\n"
            + "                      ?e2 rdf:type <%conceptIri> \n"
            + "                      FILTER ( lang(?altLabel) = \"%language\" )\n"
            + "                  }\n"
            + "              }\n"
            + "    FILTER EXISTS { GRAPH <http://wikidata.org/statements> { ?e2 ?p ?v }}\n"
            + "    FILTER NOT EXISTS {\n"
            + "    VALUES ?topic {e:Q17442446 e:Q18616576 e:Q5707594 e:Q427626 e:Q16521 e:Q11173}\n"
            + "    GRAPH <http://wikidata.org/instances> {?e2 rdf:type ?topic}}\n"
            + "    BIND (STRLEN(?altLabel) as ?len)";
    
    private static String SPARQL_CANONICAL_LABEL_ENTITY = "{\n"
            + "        GRAPH <http://wikidata.org/terms> { ?e2 rdfs:label ?label. }\n"
            + "        FILTER ( lang(?label) = \"%language\" )\n"
            + "   }\n";

    private static String SPARQL_LIMIT = " \n LIMIT ";

    private static String SPARQL_MAP_WIKIPEDIA_ID = "{\n"
            + "         GRAPH <http://wikidata.org/sitelinks> { <%otherkbid%> schema:about ?e2 }\n"
            + "   }\n";

    private static String SPARQL_RELATION_DIRECT = 
            "{GRAPH <http://wikidata.org/statements> { ?e1 ?p ?m . ?m ?rd ?e2 . %restriction% }}\n";

    private static String SPARQL_RELATION_REVERSE = 
            "{GRAPH <http://wikidata.org/statements> { ?e2 ?p ?m . ?m ?rr ?e1 . %restriction% }}\n";

    /**
     *
     * @param tokens the words spanned by the mention
     * @param limit maximum number of results
     * @param conceptIri the concept of which instances should be generated as candidates
     * @param language the language in which the labels should be retrieved
     * @return a query to retrieve candidate entities
     */
    public static String generateCandidateQuery(List<String> tokens, int limit, IRI conceptIri,
            String language)
    {
        String query = SPARQL_INFERENCE_CLAUSE;
        query += SPARQL_PREFIX + "\n";
        query += SPARQL_SELECT + "{";
        String SPARQL_ENTITY_LABEL_INST = (SPARQL_ENTITY_LABEL + SPARQL_CANONICAL_LABEL_ENTITY)
                .replace("%entitylabel",
                        ("'").concat(String.join(" ", tokens).concat("'")).replace("'", ""));
        if (tokens.size() == 1) {
            SPARQL_ENTITY_LABEL_INST = SPARQL_ENTITY_LABEL_INST.replace("%demonym", tokens.get(0));
        }
        else {
            SPARQL_ENTITY_LABEL_INST = SPARQL_ENTITY_LABEL_INST.replace(
                    "{GRAPH <http://wikidata.org/statements> { \n"
             + "                      ?e2 e:P1549s/e:P1549v \"%demonym\"@" + language + "\n"
             + "                      }\n"
             + "                  }\n"
             + "                  UNION\n",
            "");
        }
        
        if (conceptIri != null) {
            SPARQL_ENTITY_LABEL_INST = SPARQL_ENTITY_LABEL_INST
                    .replace("%conceptIri", conceptIri.stringValue());
        } 
        else {
            SPARQL_ENTITY_LABEL_INST = SPARQL_ENTITY_LABEL_INST
                    .replace("?e2 rdf:type <%conceptIri>", "");
        }
        SPARQL_ENTITY_LABEL_INST = SPARQL_ENTITY_LABEL_INST.replace("%language", language);
        
        query += SPARQL_ENTITY_LABEL_INST;
        query += "} \n";
        query += SPARQL_LIMIT + limit;
        String variables = "".concat("?e2 ").concat("?altLabel ").concat("?label");
        query = query.replace("%queryvariables%", variables);
        return query;
    }

    public static String mapWikipediaUrlToWikidataUrlQuery(String wikipediaURL, String language)
    {
        String wikipediaId = wikipediaURL.replace("http://" + language + ".wikipedia.org/wiki/",
                "https://" + language + ".wikipedia.org/wiki/");
        String query = SPARQL_PREFIX + "\n";
        query += SPARQL_SELECT + "{\n";
        query += SPARQL_MAP_WIKIPEDIA_ID.replace("%otherkbid%", wikipediaId);
        query += "\n}";
        query = query.replace("%queryvariables%", "?e2");
        query += SPARQL_LIMIT + 10;
        return query;
    }

    /**
     *
     * @param wikidataId wikidataId
     * @param limit maximum number of results
     * @return a query to retrieve the semantic signature
     */
    public static String generateSemanticSignatureQuery(String wikidataId, int limit)
    {
        String query = SPARQL_PREFIX + "\n";
        query += SPARQL_SELECT + "{\n";
        String semanticSignatureInst = "{\n" + SPARQL_RELATION_DIRECT;
        semanticSignatureInst += "UNION \n" + SPARQL_RELATION_REVERSE + "}\n";
        semanticSignatureInst = semanticSignatureInst.replace("?e2", "e:" + wikidataId);
        semanticSignatureInst = semanticSignatureInst
                .replace(" ?p ?m . ?m ?rd ", " ?rd ?m . ?m ?p ");
        query += semanticSignatureInst;
        query += SPARQL_CANONICAL_LABEL_ENTITY.replace("?e2", "?e1").replace("%language", "en");
        query += "\n}";
        query = query.replace("%queryvariables%", "?label ?p ?e1");
        query = query.replace("%restriction%", "");
        query += SPARQL_LIMIT + limit;
        return query;
    }

}
