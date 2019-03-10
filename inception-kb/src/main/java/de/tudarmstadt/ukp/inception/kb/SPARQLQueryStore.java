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

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public final class SPARQLQueryStore
{
    private final static Logger LOG = LoggerFactory.getLogger(SPARQLQueryStore.class);
    
    public static final String SPARQL_PREFIX = String.join("\n",
            "PREFIX rdf: <" + RDF.NAMESPACE + ">",
            "PREFIX rdfs: <" + RDFS.NAMESPACE + ">",
            "PREFIX owl: <" + OWL.NAMESPACE + ">",
            "PREFIX skos:<" + SKOS.NAMESPACE + ">",
            "PREFIX e:<http://www.wikidata.org/entity/>",
            "PREFIX base:<http://www.wikidata.org/ontology#>",
            "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>");
    
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
     * 
     * @param filterVariable
     *            the variable by which to filer.
     * @return the query fragment.
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
     * adds an OPTIONAL block which looks for a value that is declared with a sub-property of the
     * given property
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
            "    { ?e1  ?rd ?m . ?m ?p ?e2 . }",
            "    UNION",
            "    { ?e2 ?p ?m . ?m ?rr ?e1 . }",
            "    ?e1 ?labelIri ?label. ",
            "  }",
            " LIMIT " + aLimit);

        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        tupleQuery.setBinding("language", vf.createLiteral((aKb.getDefaultLanguage() != null)
            ? aKb.getDefaultLanguage() : "en"));
        tupleQuery.setBinding("e2", vf.createIRI(aIri));
        tupleQuery.setBinding("labelIri", aKb.getLabelIri());
        return tupleQuery;
    }
}
