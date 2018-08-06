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
package de.tudarmstadt.ukp.inception.kb;

public class SPARQLQueryStore
{   
    public static String aLimit = "1000";
    
    public static final String SPARQL_PREFIX = String.join("\n",
            "PREFIX e:<http://www.wikidata.org/entity/>",
            "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>",
            "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
            "PREFIX owl:<http://www.w3.org/2002/07/owl#>",
            "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>",
            "PREFIX base:<http://www.wikidata.org/ontology#>",
            "PREFIX schema: <http://schema.org/>");
    
    // Query to list properties from KnowledgeBase
    public static String PROPERTYLIST_QUERY = String.join("\n"
            , SPARQL_PREFIX
            , "SELECT DISTINCT ?s ?l WHERE {"
            , "  { ?s ?pTYPE ?oPROPERTY .}"
            , "  UNION "
            , "  { ?s a ?prop" 
            , "    VALUES ?prop { rdf:Property owl:ObjectProperty owl:DatatypeProperty owl:AnnotationProperty} }"
            , "  OPTIONAL {"
            , "    ?s ?pLABEL ?l ."
            , "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\"))"
            , "  }"
            , "}"
            , "LIMIT 10000");  
    
    
}
