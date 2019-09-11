/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.kb.querybuilder;

import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

public class WikidataEntitySearchService implements GraphPattern
{
    private static final Prefix WIKIBASE = prefix("wikibase", iri("http://wikiba.se/ontology#"));
    private static final Prefix MWAPI = prefix("mwapi", iri("https://www.mediawiki.org/ontology#API/"));
    private static final Prefix BD = prefix("bd", iri("http://www.bigdata.com/rdf#"));

    private static final Iri WIKIBASE_API = WIKIBASE.iri("api");
    private static final Iri WIKIBASE_ENDPOINT = WIKIBASE.iri("endpoint");
    private static final Iri WIKIBASE_LIMIT = WIKIBASE.iri("limit");
    private static final Iri WIKIBASE_API_OUTPUT_ITEM = WIKIBASE.iri("apiOutputItem");
    
    private static final Iri MWAPI_SERVICE = WIKIBASE.iri("mwapi");
    private static final Iri MWAPI_SEARCH = MWAPI.iri("search");
    private static final Iri MWAPI_LANGUAGE = MWAPI.iri("language");
    private static final Iri MWAPI_ITEM = MWAPI.iri("item");
    
    private static final Iri BD_SERVICE_PARAM = BD.iri("serviceParam");

//  SERVICE wikibase:mwapi {
//      bd:serviceParam wikibase:api "EntitySearch" .
//      bd:serviceParam wikibase:endpoint "www.wikidata.org" .
//      bd:serviceParam mwapi:search $query .
//      bd:serviceParam mwapi:language "en" .
//      ?item wikibase:apiOutputItem mwapi:item .
//      ?label wikibase:apiOutputItem mwapi:label .
//      ?num wikibase:apiOrdinal true .
//  }
    private final Collection<TriplePattern> patterns;

    /**
     * Provides access to the Wikidata entity search.
     * 
     * @param aSubject the variable to which the matching item IRIs are to be bound.
     * @param aQuery the query term. The search does not appear to support wildcards.
     * @param aLanguage the preferred language.
     */
    public WikidataEntitySearchService(Variable aSubject, String aQuery, String aLanguage)
    {
        patterns = new ArrayList<>();
        patterns.add(BD_SERVICE_PARAM.has(WIKIBASE_API, literalOf("EntitySearch")));
        patterns.add(BD_SERVICE_PARAM.has(WIKIBASE_ENDPOINT, literalOf("www.wikidata.org")));
        patterns.add(BD_SERVICE_PARAM.has(WIKIBASE_LIMIT, literalOf("once")));
        patterns.add(BD_SERVICE_PARAM.has(MWAPI_SEARCH, literalOf(aQuery)));
        patterns.add(BD_SERVICE_PARAM.has(MWAPI_LANGUAGE, literalOf(aLanguage)));
        patterns.add(aSubject.has(WIKIBASE_API_OUTPUT_ITEM, MWAPI_ITEM));
    }

    @Override
    public String getQueryString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SERVICE ");
        sb.append(MWAPI_SERVICE.getQueryString());
        sb.append(" { \n");
        for (TriplePattern pattern : patterns) {
            sb.append(pattern.getQueryString());
            sb.append(" \n");
        }
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public boolean isEmpty()
    {
        return false;
    }
}
