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

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

public class SPARQLQueryStore
{   
    public static int aLimit = 1000;

    public static final String SPARQL_PREFIX = String.join("\n",
            "PREFIX rdf: <" + RDF.NAMESPACE + ">",
            "PREFIX rdfs: <" + RDFS.NAMESPACE + ">",
            "PREFIX owl: <" + OWL.NAMESPACE + ">");
    
    // Query to list properties from KnowledgeBase
    public static final String PROPERTYLIST_QUERY = String.join("\n"
            , SPARQL_PREFIX
            , "SELECT DISTINCT ?s ?l ?d WHERE {"
            , "  { ?s ?pTYPE ?oPROPERTY .}"
            , "  UNION "
            , "  { ?s a ?prop" 
            , "    VALUES ?prop { rdf:Property owl:ObjectProperty owl:DatatypeProperty owl:AnnotationProperty} }"
            , "  OPTIONAL {"
            , "    ?s ?pLABEL ?l ."
            , "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\"))"
            , "  }"
            , "  OPTIONAL {"
            , "    ?s ?pDESCRIPTION ?d ."
            , "    FILTER(LANG(?d) = \"\" || LANGMATCHES(LANG(?d), \"en\"))"
            , "  }"
            , "}"
            , "LIMIT " + aLimit);
    
    //Query to get property specific domain elements including properties 
    //which do not have a 
    // domain specified
    public static final String PROPERTYLIST_DOMAIN_DEPENDENT = String.join("\n"
            , SPARQL_PREFIX
            , "SELECT DISTINCT ?s ?l ?d WHERE {"
            , "{  ?s rdfs:domain/(owl:unionOf/rdf:rest*/rdf:first)* ?aDomain }"
            , " UNION "
            , "{ ?s a ?prop "
            , "    VALUES ?prop { rdf:Property owl:ObjectProperty owl:DatatypeProperty owl:AnnotationProperty} "
            , "    FILTER NOT EXISTS {  ?s rdfs:domain/(owl:unionOf/rdf:rest*/rdf:first)* ?x } }"
            , "  OPTIONAL {"
            , "    ?s ?pLABEL ?l ."
            , "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\"))"
            , "  }"
            , "  OPTIONAL {"
            , "    ?s ?pDESCRIPTION ?d ."
            , "    FILTER(LANG(?d) = \"\" || LANGMATCHES(LANG(?d), \"en\"))"
            , "  }"
            , "}"
            , "LIMIT " + aLimit);
    
    //Query to get property specific range elements
    public static final String PROPERTY_SPECIFIC_RANGE = String.join("\n"
            , SPARQL_PREFIX
            , "SELECT DISTINCT ?s ?l ?d WHERE {"
            , "  ?aProperty rdfs:range/(owl:unionOf/rdf:rest*/rdf:first)* ?s "
            , "  OPTIONAL {"
            , "    ?aProperty ?pLABEL ?l ."
            , "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\"))"
            , "  }"
            , "  OPTIONAL {"
            , "    ?s ?pDESCRIPTION ?d ."
            , "    FILTER(LANG(?d) = \"\" || LANGMATCHES(LANG(?d), \"en\"))"
            , "  }"
            , "}"
            , "LIMIT " + aLimit);

    // Query to retrieve super class concept for a concept
    public static final String PARENT_CONCEPT = String.join("\n"
            , SPARQL_PREFIX
            , "SELECT DISTINCT ?s ?l ?d WHERE { "
            , "   {?oChild ?pSUBCLASS ?s . }"
            , "   UNION { ?s ?pTYPE ?oCLASS ."
            , "     ?oChild owl:intersectionOf ?list . "
            , "     FILTER EXISTS {?list rdf:rest*/rdf:first ?s. } }"
            , "   OPTIONAL { "
            , "     ?s ?pLABEL ?l . "
            , "     FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\")) "
            , "   }"
            , "  OPTIONAL {"
            , "    ?s ?pDESCRIPTION ?d ."
            , "    FILTER(LANG(?d) = \"\" || LANGMATCHES(LANG(?d), \"en\"))"
            , "  }"
            , "} ");
    
    
    // Query to retrieve concept for an instance
    public static final String CONCEPT_FOR_INSTANCE = String.join("\n"
            , SPARQL_PREFIX
            , "SELECT DISTINCT ?s ?l ?d WHERE {"
            , "  ?pInstance ?pTYPE ?s ."
            , "  OPTIONAL {"
            , "    ?pInstance ?pLABEL ?l ."
            , "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\"))"
            , "  }"
            , "  OPTIONAL {"
            , "    ?s ?pDESCRIPTION ?d ."
            , "    FILTER(LANG(?d) = \"\" || LANGMATCHES(LANG(?d), \"en\"))"
            , "  }"
            , "}"
            , "LIMIT " + aLimit);
}
