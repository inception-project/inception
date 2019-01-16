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

import java.util.Set;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public final class SPARQLQueryStore
{

    public static final String SPARQL_PREFIX = String.join("\n",
            "PREFIX rdf: <" + RDF.NAMESPACE + ">",
            "PREFIX rdfs: <" + RDFS.NAMESPACE + ">",
            "PREFIX owl: <" + OWL.NAMESPACE + ">");

    /**
     * Return formatted String for the OPTIONAL part of SPARQL query for language and description
     * filter
     * 
     * @param aProperty
     *            The property IRI for the optional filter
     * @param aLanguage
     *            The variable indicating the language
     * @param variable
     *            The variable for the IRI like '?s'
     * @param filterVariable
     *            The variable for the language (?l) and description (?d)
     * @return String format for OPTIONAL part of SPARQL query
     */
    private static final String optionalLanguageFilteredValue(String aProperty, String aLanguage,
            String variable, String filterVariable)
    {
        StringBuilder fragment = new StringBuilder();
        
        fragment.append("  OPTIONAL {\n");
        fragment.append("    " + variable + " ").append(aProperty).append(" ")
                .append(filterVariable).append(" .\n");
        
        fragment.append(languageFilter(filterVariable, aLanguage));

        fragment.append(" }\n");
        return fragment.toString();
    }

    /**
     * Returns formatted string that filters the given variable for the given language
     * @param filterVariable
     * @return
     */
    private static final String languageFilter(String filterVariable, String aLanguage) {
        StringBuilder fragment = new StringBuilder();

        if (aLanguage != null) {
            // If a certain language is specified, we look exactly for that
            String escapedLang = NTriplesUtil.escapeString(aLanguage);
            fragment.append("    FILTER(LANGMATCHES(LANG(").append(filterVariable).append("), \"").append(escapedLang).append("\"))\n");
        }
        else {
            // If no language is specified, we look for statements without a language as otherwise
            // we might easily run into trouble on multi-lingual resources where we'd get all the
            // labels in all the languages being retrieved if we simply didn't apply any filter.
            fragment.append("    FILTER(LANG(").append(filterVariable).append(") = \"\")\n");
        }

        return fragment.toString();
    }
  
    /**
     * adds an OPTIONAL block which looks for a value that is declared with a
     * subproperty of the given property
     */
    private static final String queryForOptionalSubPropertyLabel(Set<KBHandle> labelProperties,
            String aLanguage, String variable, String filterVariable)
    {
        StringBuilder fragment = new StringBuilder();
        for (KBHandle label : labelProperties) {
            fragment.append(optionalLanguageFilteredValue("<" + label.getIdentifier() + ">",
                    aLanguage, variable, filterVariable));
            fragment.append(optionalLanguageFilteredValue("<" + label.getIdentifier() + ">", null,
                    variable, filterVariable));
        }
        return fragment.toString();
    }

    /** 
     * Query to list all concepts from a knowledge base.
     */
    public static final String queryForAllConceptList(KnowledgeBase aKB,
            Set<KBHandle> labelProperties)
    {
        return String.join("\n"
                , SPARQL_PREFIX
                , "SELECT DISTINCT ?s ?l ((?labelGeneral) AS ?lGen) ?spl WHERE { "
                , "  { ?s ?pTYPE ?oCLASS . } "
                , "  UNION { ?someSubClass ?pSUBCLASS ?s . } ."
                , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?s","?l")
                , optionalLanguageFilteredValue("?pLABEL", null,"?s","?labelGeneral")
                , queryForOptionalSubPropertyLabel(labelProperties, aKB.getDefaultLanguage(),"?s","?spl")
                , "}"
                , "LIMIT " + aKB.getMaxResults());
    }
    
    /** 
     * Query to list all instances from a knowledge base.
     */
    public static final String listInstances(KnowledgeBase aKB, Set<KBHandle> labelProperties)
    {
        return String.join("\n"
                , SPARQL_PREFIX
                , "SELECT DISTINCT ?s ?l ((?labelGeneral) AS ?lGen) ?spl WHERE {"
                , "  ?s ?pTYPE ?oPROPERTY ."
                , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?s","?l")
                , optionalLanguageFilteredValue("?pLABEL", null,"?s","?labelGeneral")
                , queryForOptionalSubPropertyLabel(labelProperties, aKB.getDefaultLanguage(),"?s","?spl")
                , "}"
                , "LIMIT " + aKB.getMaxResults());
    }
    
    
    /** 
     * Query to list root concepts from a knowledge base.
     */
    public static final String listRootConcepts(KnowledgeBase aKB, Set<KBHandle> labelProperties)
    {
        return String.join("\n"
                , SPARQL_PREFIX    
                , "SELECT ?s (MIN(?label) AS ?l) (MIN(?labelGeneral) AS ?lGen) (MIN(?splabel) AS ?spl) WHERE { "
                , "  { ?s ?pTYPE ?oCLASS . } "
                , "  UNION { ?someSubClass ?pSUBCLASS ?s . } ."
                , "  FILTER NOT EXISTS { "
                , "    ?s ?pSUBCLASS ?otherSub . "
                , "    FILTER (?s != ?otherSub) }"
                , "  FILTER NOT EXISTS { "
                , "    ?s owl:intersectionOf ?list . }"
                , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?s","?label")
                , optionalLanguageFilteredValue("?pLABEL", null,"?s","?labelGeneral")
                , queryForOptionalSubPropertyLabel(labelProperties, aKB.getDefaultLanguage(),"?s","?splabel")
                , "} GROUP BY ?s"
                , "LIMIT " + aKB.getMaxResults());    }
    
    /** 
     * Query to list child concepts from a knowledge base.
     */
    public static final String listChildConcepts(KnowledgeBase aKB, Set<KBHandle> labelProperties)
    {
        return String.join("\n"
                , SPARQL_PREFIX    
                , "SELECT ?s (MIN(?label) AS ?l) (MIN(?labelGeneral) AS ?lGen) (MIN(?splabel) AS ?spl) WHERE { "
                , "  {?s ?pSUBCLASS ?oPARENT . }" 
                , "  UNION { ?s ?pTYPE ?oCLASS ."
                , "    ?s owl:intersectionOf ?list . "
                , "    FILTER EXISTS { ?list rdf:rest*/rdf:first ?oPARENT} }"
                , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?s","?label")
                , optionalLanguageFilteredValue("?pLABEL", null,"?s","?labelGeneral")
                , queryForOptionalSubPropertyLabel(labelProperties, aKB.getDefaultLanguage(),"?s","?splabel")
                , "} GROUP BY ?s"
                , "LIMIT " + aKB.getMaxResults());
    }

    /** 
     * Query to check if a concept has child concepts. It returns the ID of the first child concept
     * if it exists. 
     */
    public static final String hasChildConcepts(KnowledgeBase aKB)
    {
        return String.join("\n"
                , SPARQL_PREFIX
                , "SELECT ?s WHERE { "
                , "  {"
                , "    ?s ?pSUBCLASS ?oPARENT ." 
                , "  } UNION { "
                , "    ?s ?pTYPE ?oCLASS ."
                , "    ?s owl:intersectionOf ?list . "
                , "    FILTER EXISTS { ?list rdf:rest*/rdf:first ?oPARENT }"
                , "  }"
                , "}"
                , "LIMIT 1");
    }

    /** 
     * Query to read concept from a knowledge base.
     */
    public static final String readConcept(KnowledgeBase aKB, int limit,
            Set<KBHandle> labelProperties)
    {
        return String.join("\n"
            , SPARQL_PREFIX    
            , "SELECT ?oItem ((?label) AS ?l) ((?desc) AS ?d) ((?labelGeneral) AS ?lGen) ?descGeneral ?spl WHERE { "
            , "  { ?oItem ?pTYPE ?oCLASS . } "
            , "  UNION {?someSubClass ?pSUBCLASS ?oItem . } "
            , "  UNION {?oItem ?pSUBCLASS ?oPARENT . }" 
            , "  UNION {?oItem ?pTYPE ?oCLASS ."
            , "    ?oItem owl:intersectionOf ?list . "
            , "    FILTER EXISTS { ?list rdf:rest*/rdf:first ?oPARENT} }"
            , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?oItem","?label")
            , optionalLanguageFilteredValue("?pDESCRIPTION", aKB.getDefaultLanguage(),"?oItem","?desc")
            , optionalLanguageFilteredValue("?pLABEL", null,"?oItem","?labelGeneral")
            , optionalLanguageFilteredValue("?pDESCRIPTION", null,"?oItem","?descGeneral")
            , queryForOptionalSubPropertyLabel(labelProperties, aKB.getDefaultLanguage(),"?oItem","?spl")
            , "} "
            , "LIMIT " + limit);
    }

    /**
     * Query to read an instance from a knowledge base
     */
    public static final String readInstance(KnowledgeBase aKB, int limit,
        Set<KBHandle> labelProperties)
    {
        return String.join("\n"
            , SPARQL_PREFIX
            , "SELECT ?concept ?oItem ((?label) AS ?l) ((?desc) AS ?d) ((?labelGeneral) AS ?lGen) ?descGeneral ?spl WHERE { "
            , "  { ?oItem ?pTYPE ?oConcept . } "
            , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?oItem","?label")
            , optionalLanguageFilteredValue("?pDESCRIPTION", aKB.getDefaultLanguage(),"?oItem","?desc")
            , optionalLanguageFilteredValue("?pLABEL", null,"?oItem","?labelGeneral")
            , optionalLanguageFilteredValue("?pDESCRIPTION", null,"?oItem","?descGeneral")
            , queryForOptionalSubPropertyLabel(labelProperties, aKB.getDefaultLanguage(),"?oItem","?spl")
            , "} "
            , "LIMIT " + limit);
    }

    /**
     * Query to read a propery from a knowledge base
     */
    public static final String readProperty(KnowledgeBase aKB, int limit,
        Set<KBHandle> labelProperties)
    {
        return String.join("\n"
            , SPARQL_PREFIX
            , "SELECT DISTINCT ?s ?l ((?labelGeneral) AS ?lGen) ((?desc) AS ?d) ?descGeneral ?spl ?dom ?range WHERE {"
            , "  { ?s ?pTYPE ?oPROPERTY .}"
            , "  UNION "
            , "  { ?s a ?prop"
            , "    VALUES ?prop { rdf:Property owl:ObjectProperty owl:DatatypeProperty owl:AnnotationProperty }"
            , "  }"
            , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?s","?l")
            , optionalLanguageFilteredValue("?pLABEL", null,"?s","?labelGeneral")
            , optionalLanguageFilteredValue("?pDESCRIPTION", aKB.getDefaultLanguage(),"?s","?desc")
            , optionalLanguageFilteredValue("?pDESCRIPTION", null,"?s","?descGeneral")
            , queryForOptionalSubPropertyLabel(labelProperties, aKB.getDefaultLanguage(),"?s","?spl")
            , optionalLanguageFilteredValue("?pDOMAIN", null, "?s", "?dom")
            , optionalLanguageFilteredValue("?pRANGE", null, "?s", "?range")
            , "}"
            , "LIMIT " + limit);
    }

    /** 
     * Query to read concept label and description from a knowledge base.
     */
    public static final String readLabelWithoutLanguage(KnowledgeBase aKB, int limit, boolean label,
            boolean desc)
    {
        StringBuilder query = new StringBuilder();
        query.append(SPARQL_PREFIX + "\n" +
                "SELECT ?oItem ((?label) AS ?l) ((?desc) AS ?d) ?lGen ?descGeneral WHERE { "
                + "\n"
                + "{?oItem ?p ?o . }"
                + "\n"
                + "UNION"
                + "{?s ?p ?oItem . }");
        if (label) {
            query.append("\n" + optionalLanguageFilteredValue("?pLABEL", null, "?oItem", "?lGen"));
        }
        if (desc) {
            query.append("\n" +
                    optionalLanguageFilteredValue("?pDESCRIPTION", null, "?oItem", "?descGeneral"));
        }
        query.append("\n" + "} " + "\n" + "LIMIT " + limit);        
        return query.toString();
    }
    
    
    /** 
     * Query to list properties from a knowledge base.
     */
    public static final String queryForPropertyList(KnowledgeBase aKB,
        Set<KBHandle> labelProperties)
    {
        return String.join("\n"
                , SPARQL_PREFIX
                , "SELECT DISTINCT ?s ?l ((?labelGeneral) AS ?lGen) ?spl WHERE {"
                , "  { ?s ?pTYPE ?oPROPERTY .}"
                , "  UNION "
                , "  { ?s a ?prop" 
                , "    VALUES ?prop { rdf:Property owl:ObjectProperty owl:DatatypeProperty owl:AnnotationProperty }"
                , "  }"
                , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?s","?l")
                , optionalLanguageFilteredValue("?pLABEL", null,"?s","?labelGeneral")
                , queryForOptionalSubPropertyLabel(labelProperties, aKB.getDefaultLanguage(),"?s","?spl")
            , "}"
                , "LIMIT " + aKB.getMaxResults());
    }
        
    /** 
     * Query to get sub property from a knowledge base.
     */
    public static final String getSubProperty(KnowledgeBase aKB)
    {
        return String.join("\n"
                , SPARQL_PREFIX    
                , "SELECT ?s WHERE { "
                , "  ?s ?pSUBPROPERTY ?oItem. " 
                , "}"
                , "LIMIT " + aKB.getMaxResults());
    }
    
    
    /**
     * Query to get property specific domain elements including properties which do not have a 
     * domain specified.
     */
    public static final String queryForPropertyListWithDomain(KnowledgeBase aKB,
        Set<KBHandle> labelProperties)
    {
        return String.join("\n"
                , SPARQL_PREFIX
                , "SELECT DISTINCT ?s ?l ((?labelGeneral) AS ?lGen) WHERE {"
                , "{  ?s rdfs:domain/(owl:unionOf/rdf:rest*/rdf:first)* ?aDomain }"
                , " UNION "
                , "{ ?s a ?prop "
                , "    VALUES ?prop { rdf:Property owl:ObjectProperty owl:DatatypeProperty owl:AnnotationProperty} "
                , "    FILTER NOT EXISTS {  ?s rdfs:domain/(owl:unionOf/rdf:rest*/rdf:first)* ?x } }"
                , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?s","?l")
                , optionalLanguageFilteredValue("?pLABEL", null,"?s","?labelGeneral")
                , queryForOptionalSubPropertyLabel(labelProperties, aKB.getDefaultLanguage(),"?s","?spl")
                , "}"
                , "LIMIT " + aKB.getMaxResults());
    }
    
    /**
     * Query to get property specific range elements
     */
    public static final String queryForPropertySpecificRange(KnowledgeBase aKB)
    {
        return String.join("\n"
                , SPARQL_PREFIX
                , "SELECT DISTINCT ?s ?l ((?labelGeneral) AS ?lGen) WHERE {"
                , "  ?aProperty rdfs:range/(owl:unionOf/rdf:rest*/rdf:first)* ?s "
                , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?s","?l")
                , optionalLanguageFilteredValue("?pLABEL", null,"?s","?labelGeneral")
                , "}"
                , "LIMIT " + aKB.getMaxResults());

    }
    
    /**
     *  Query to retrieve super class concept for a concept
     */
    public static final String queryForParentConcept(KnowledgeBase aKB)
    {
        return String.join("\n"
                , SPARQL_PREFIX
                , "SELECT DISTINCT ?s ?l ((?labelGeneral) AS ?lGen) WHERE { "
                , "   {?oChild ?pSUBCLASS ?s . }"
                , "   UNION { ?s ?pTYPE ?oCLASS ."
                , "     ?oChild owl:intersectionOf ?list . "
                , "     FILTER EXISTS {?list rdf:rest*/rdf:first ?s. } }"
                , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?s","?l")
                , optionalLanguageFilteredValue("?pLABEL", null,"?s","?labelGeneral")
            , "}");

    }
    
    /**
     *  Query to retrieve concept for an instance
     */
    public static final String queryForConceptForInstance(KnowledgeBase aKB)
    {
        return String.join("\n"
                , SPARQL_PREFIX
                , "SELECT DISTINCT ?s ?l ?lGen WHERE {"
                , "  ?pInstance ?pTYPE ?s ."
                , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?s","?l")
                , optionalLanguageFilteredValue("?pLABEL", null,"?s","?labelGeneral")
                , "}");

    }

    /**
     * General query for a statement where the object value is language filtered.
     */
    public static final String queryForStatementLanguageFiltered(KnowledgeBase aKB,
        String aLanguage)
    {
        return String.join("\n"
            , "SELECT * WHERE { "
            , "  ?s ?p ?o "
            , languageFilter("?o", aLanguage)
            , "}"
            , "LIMIT " + aKB.getMaxResults());
    }
}
