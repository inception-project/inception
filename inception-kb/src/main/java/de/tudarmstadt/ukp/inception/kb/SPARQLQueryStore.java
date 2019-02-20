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

import java.util.List;
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
import de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder;

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

    private static final String CANDIDATE_BLACKLIST = String.join(" ", WIKIMEDIA_INTERNAL,
            WIKIMEDIA_PROJECT_PAGE, WIKIMEDIA_CATEGORY, WIKIMEDIA_DISAMBIGUATION_PAGE,
            WIKIMEDIA_LIST_ARTICLE, WIKIMEDIA_TEMPLATE, WIKIMEDIA_NEWS_ARTICLE,
            WIKIMEDIA_NAVIGATIONAL_TEMPLATE);    
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
    
    
    /**
     * Finds items based on their label.
     * 
     * There are several conditions:
     * <ul>
     * <li>The match is case sensitive. It has been tied to use LCASE in combination with FILTER to
     * allow matching the lower cased arguments with the entities from the KB, but that was too
     * time-intensive and lead to timeouts.</li>
     * <li>Items need to actually have a label. I.e. items which do not have a label and for which
     * {@link KBHandle#getUiLabel()} extracts a label from their subject IRI cannot be located.</li>
     * <li>The KB default language needs to match the language of the label.</li>
     * <li>FIXME: Label sub-properties are currently <b>not</b> considered.</li>
     * </ul>
     *
     * @param aTypedString
     *            typed string from the user
     * @param aMention
     *            the marked surface form
     * @param aKb
     *            the Knowledge Base
     * @return a query to retrieve candidate entities
     */
    public static List<KBHandle> searchItemsExactLabelMatch(RepositoryConnection aConn,
        String aTypedString, String aMention, KnowledgeBase aKb)
    {
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(aKb);
        builder.withLabelMatchingExactlyAnyOf(aMention, aTypedString);
        builder.retrieveLabel();
        builder.retrieveDescription();
        return builder.asHandles(aConn, true);
        
//        String query = String.join("\n",
//            SPARQL_PREFIX,
//            "SELECT DISTINCT ?s ((?label) AS ?l) ((?desc) AS ?d) ((?labelGeneral) AS ?lGen) ?descGeneral WHERE",
//            "{",
//            "  VALUES ?label { " + mention + " " + typedString + " } ",
//            "  ?s ?pLABEL ?label .",
//            "  FILTER NOT EXISTS { ",
//            "    VALUES ?topic {" + CANDIDATE_BLACKLIST + "}",
//            "    ?s ?pTYPE ?topic",
//            "  }",
//            optionalLanguageFilteredValue("?pDESCRIPTION", aKb.getDefaultLanguage(), "?s", "?desc"),
//            optionalLanguageFilteredValue("?pDESCRIPTION", null, "?s", "?descGeneral"),
//            //queryForOptionalSubPropertyLabel(labelProperties, aKb.getDefaultLanguage(),"?oItem","?spl"),
//            "}");
//
//        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
//        tupleQuery.setBinding("pLABEL", aKb.getLabelIri());
//        tupleQuery.setBinding("pTYPE", aKb.getTypeIri());
//        tupleQuery.setBinding("pDESCRIPTION", aKb.getDescriptionIri());
    }

//    private static String getFullTextMatchingQueryPartVirtuoso(int aLimit)
//    {
//        return  String.join("\n",
//            "    SELECT DISTINCT ?s ?l ?d WHERE",
//            "    {",
//            "      ?s ?labelIri ?l .",
//            "      ?l ?ftsIri ?query . ",
//            "      OPTIONAL",
//            "      {",
//            "        ?s ?descriptionIri ?d.",
//            "      }",
//            "    }",
//            "    LIMIT " + aLimit);
//    }
//
//    // http://culturecloud.ru/resource/Help:Search#Full_Text_Search
//    private static String getFullTextMatchingQueryPartLucene(int aLimit)
//    {
//        return  String.join("\n",
//            "    SELECT DISTINCT ?s ?l ?d WHERE",
//            "    {",
//            "      ?s search:matches ?match .",
//            "      ?match search:query ?query ;",
//            "             search:property ?labelIri ;",
//            "             search:snippet ?l",
//            "      OPTIONAL",
//            "      {",
//            "        ?s ?descriptionIri ?d.",
//            "      }",
//            "    }",
//            "    LIMIT " + aLimit);
//    }

    /**
     *
     * This query retrieves candidates via full-text matching of their labels and full-text-search
     *
     * @param aString String for which to perform full text search
     * @param aLimit maximum number of results
     * @param aKb the Knowledge Base
     * @return a query to retrieve candidate entities
     */
    public static List<KBHandle> searchItemsStartingWith(RepositoryConnection aConn,
        String aString, int aLimit, KnowledgeBase aKb)
    {
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(aKb);
        builder.withLabelStartingWith(aString);
        builder.retrieveLabel();
        builder.retrieveDescription();
        return builder.asHandles(aConn, true);
        
//        String string = RenderUtils.escape(aString).toLowerCase(Locale.ENGLISH);
//        ValueFactory vf = SimpleValueFactory.getInstance();
//        Literal searchLiteral;
//        String fullTextMatchingString;
//
//        if (aKb.getFullTextSearchIri().equals(IriConstants.FTS_LUCENE)) {
//            fullTextMatchingString = getFullTextMatchingQueryPartLucene(aLimit);
//            // add wildcard '*' to perform wildcard search
//            searchLiteral = vf.createLiteral(string + "*");
//        } else {
//            fullTextMatchingString = getFullTextMatchingQueryPartVirtuoso(aLimit);
//            
//            StringBuilder queryString = new StringBuilder();
//            // Virtuoso requires that strings are quoted if the contain spaces. We just always
//            // quote them to be on the safe side.
//            queryString.append("'");
//            
//            // Strip single quotes and asterisks because they have special semantics
//            String query = string.replace("'", " ").replace("*", " ");
//            queryString.append(query);
//            
//            // If the last token in the query has 4 chars or more, then we add an asterisk to
//            // perform a prefix search. If we try that with less than 4 chars, Virtuoso will
//            // send us back an error.
//            String[] queryTokens = query.split(" ");
//            if (queryTokens[queryTokens.length - 1].length() >= 4) {
//                queryString.append("*");
//            }
//            
//            queryString.append("'");
//            
//            searchLiteral = vf.createLiteral(queryString.toString());
//        }
//
//        String query = String.join("\n", SPARQL_PREFIX, fullTextMatchingString);
//
//        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
//        tupleQuery.setBinding("query", searchLiteral);
//        tupleQuery.setBinding("language", vf.createLiteral((aKb.getDefaultLanguage() != null)
//            ? aKb.getDefaultLanguage() : "en"));
//        tupleQuery.setBinding("labelIri", aKb.getLabelIri());
//        tupleQuery.setBinding("typeIri", aKb.getTypeIri());
//        tupleQuery.setBinding("descriptionIri", aKb.getDescriptionIri());
//        tupleQuery.setBinding("ftsIri", aKb.getFullTextSearchIri());
//        return tupleQuery;
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
