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

import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_LUCENE;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_NONE;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_VIRTUOSO;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.hasImplicitNamespace;
import static java.lang.Integer.toHexString;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.and;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.function;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.or;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.CONTAINS;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.LANG;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.LANGMATCHES;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.LCASE;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.STR;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.STRSTARTS;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.optional;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.union;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.bNode;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOfLanguage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Operand;
import org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class SPARQLQueryBuilder
{
    private final static Logger LOG = LoggerFactory.getLogger(SPARQLQueryBuilder.class);
    
    public static final String VAR_SUBJECT_NAME = "s";
    public static final String VAR_LABEL_PROPERTY_NAME = "pLabel";
    public static final String VAR_LABEL_NAME = "l";
    public static final String VAR_LABEL_CANDIDATE_NAME = "lc";
    public static final String VAR_DESCRIPTION_NAME = "d";
    
    public static final Variable VAR_SUBJECT = SparqlBuilder.var(VAR_SUBJECT_NAME);
    public static final Variable VAR_LABEL = SparqlBuilder.var(VAR_LABEL_NAME);
    public static final Variable VAR_LABEL_CANDIDATE = SparqlBuilder.var(VAR_LABEL_CANDIDATE_NAME);
    public static final Variable VAR_LABEL_PROPERTY = SparqlBuilder.var(VAR_LABEL_PROPERTY_NAME);
    public static final Variable VAR_DESCRIPTION = SparqlBuilder.var(VAR_DESCRIPTION_NAME);
    public static final Variable VAR_DESC_CANDIDATE = SparqlBuilder.var("dc");

    public static final Prefix PREFIX_LUCENE_SEARCH = SparqlBuilder.prefix("search",
            iri("http://www.openrdf.org/contrib/lucenesail#"));
    public static final Iri LUCENE_QUERY = PREFIX_LUCENE_SEARCH.iri("query");
    public static final Iri LUCENE_PROPERTY = PREFIX_LUCENE_SEARCH.iri("property");
    public static final Iri LUCENE_SCORE = PREFIX_LUCENE_SEARCH.iri("score");
    public static final Iri LUCENE_SNIPPET = PREFIX_LUCENE_SEARCH.iri("snippet");
        
    public static final int PRIO_PRIMARY = 1;
    public static final int PRIO_SECONDARY = 10;
    
    private final Set<Prefix> prefixes = new LinkedHashSet<>();
    private final Set<Projectable> projections = new LinkedHashSet<>();
    private final List<Pair<Integer, GraphPattern>> patterns = new ArrayList<>();
    
    private boolean labelImplicitlyRetrieved = false;
    
    /**
     * This flag is set internally to indicate whether the query should be skipped and an empty
     * result should always be returned. This can be the case, e.g. if the post-processing of
     * the query string against which to match a label causes the query to become empty.
     */
    private boolean returnEmptyResult = false;
    
    private final KnowledgeBase kb;
    private final Mode mode;
    
    /**
     * Case-insensitive mode is a best-effort approach. Depending on the underlying FTS, it may
     * or may not work.
     */
    private boolean caseInsensitive = true;
    
    /**
     * This flag controls whether we attempt to drop duplicate labels and descriptions on the
     * side of the SPARQL server (true) or whether we try retrieving all labels and descriptions
     * which have either no language or match the KB language and then drop duplicates on our
     * side. Both approaches have benefits and draw-backs. In general, we try server-side
     * reduction to reduce the data being transferred across the wire. However, in some cases we
     * implicitly turn off server side reduction because (SSR) we have not been able to figure out
     * working SSR queries.
     * 
     * Benefits of SSR:
     * <ul>
     * <li>Less data to transfer across the wire</li>
     * <li>LIMIT works accurately (if we drop client side, we may end up with less than LIMIT 
     *     results)</li>
     * </ul>
     * 
     * Drawbacks of SSR:
     * <ul>
     * <li>More complex queries</li>
     * </ul>
     * 
     * @see #reduceRedundantResults(List)
     */
    private boolean serverSideReduce = true;
    
    public static enum Mode {
        ITEM, CLASS, INSTANCE, PROPERTY;
        
        public Iri getLabelProperty(KnowledgeBase aKb) {
            switch (this) {
            case ITEM:
                return iri(aKb.getLabelIri().toString());
            case CLASS:
                return iri(aKb.getLabelIri().toString());
            case INSTANCE:
                return iri(aKb.getLabelIri().toString());
            case PROPERTY:
                return iri(aKb.getPropertyLabelIri().toString());
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }
        }

        public Iri getDescriptionProperty(KnowledgeBase aKb) {
            switch (this) {
            case ITEM:
                return iri(aKb.getDescriptionIri().toString());
            case CLASS:
                return iri(aKb.getDescriptionIri().toString());
            case INSTANCE:
                return iri(aKb.getDescriptionIri().toString());
            case PROPERTY:
                return iri(aKb.getPropertyDescriptionIri().toString());
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }
        }
        
        public GraphPattern scopePattern(KnowledgeBase aKB, Iri aScope)
        {
            Iri typeOfProperty = Rdf.iri(aKB.getTypeIri().toString());
            Iri subClassProperty = Rdf.iri(aKB.getSubclassIri().toString());
                        
            switch (this) {
            case ITEM:
                return GraphPatterns.union(
                        VAR_SUBJECT.has(() -> subClassProperty.getQueryString() + "+", aScope),
                        VAR_SUBJECT.has(() -> typeOfProperty.getQueryString() + "/"
                                + subClassProperty.getQueryString() + "*", aScope));
            case CLASS:
                return VAR_SUBJECT.has(() -> subClassProperty.getQueryString() + "+", aScope);
            case INSTANCE:
                return VAR_SUBJECT.has(() -> typeOfProperty.getQueryString() + "/"
                        + subClassProperty.getQueryString() + "*", aScope);
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }            
            
        }
    }
    
    /**
     * Retrieve classes and instances.
     */
    public static SPARQLQueryBuilder forItems(KnowledgeBase aKB)
    {
        return new SPARQLQueryBuilder(aKB, Mode.ITEM);
    }

    /**
     * Retrieve classes.
     */
    public static SPARQLQueryBuilder forClasses(KnowledgeBase aKB)
    {
        SPARQLQueryBuilder builder = new SPARQLQueryBuilder(aKB, Mode.CLASS);
        builder.limitToClasses();
        return builder;
    }

    /**
     * Retrieve instances.
     */
    public static SPARQLQueryBuilder forInstances(KnowledgeBase aKB)
    {
        SPARQLQueryBuilder builder = new SPARQLQueryBuilder(aKB, Mode.INSTANCE);
        builder.limitToInstances();
        return builder;
    }

    /**
     * Retrieve properties.
     */
    public static SPARQLQueryBuilder forProperties(KnowledgeBase aKB)
    {
        return new SPARQLQueryBuilder(aKB, Mode.PROPERTY);
    }

    private SPARQLQueryBuilder(KnowledgeBase aKB, Mode aMode)
    {
        kb = aKB;
        mode = aMode;
    }
    
    private void addPattern(int aPriority, GraphPattern aPattern)
    {
        patterns.add(Pair.of(aPriority, aPattern));
    }
    
    private boolean hasPrimaryPatterns()
    {
        // If we force the query to return an empty result, that means that we intentionally skipped
        // adding a primary query
        return returnEmptyResult
                || patterns.stream().map(Pair::getKey).anyMatch(value -> PRIO_PRIMARY == value);
    }
    
    private Projectable getLabelProjection()
    {
        if (serverSideReduce) {
            return Expressions.min(VAR_LABEL_CANDIDATE).as(VAR_LABEL);
        }
        else {
            return VAR_LABEL_CANDIDATE;
        }
    }

    private Projectable getDescriptionProjection()
    {
        if (serverSideReduce) {
            return Expressions.min(VAR_DESC_CANDIDATE).as(VAR_DESCRIPTION);
        }
        else {
            return VAR_DESC_CANDIDATE;
        }
    }

    public SPARQLQueryBuilder caseSensitive()
    {
        caseInsensitive = false;
        return this;
    }

    public SPARQLQueryBuilder caseSensitive(boolean aEnabled)
    {
        caseInsensitive = !aEnabled;
        return this;
    }

    public SPARQLQueryBuilder caseInsensitive()
    {
        caseInsensitive = true;
        return this;
    }

    /**
     * Generates a pattern which binds all sub-properties of the label property to the given 
     * variable. 
     */
    private GraphPattern bindLabelProperties(Variable aVariable)
    {
        Iri pLabel = mode.getLabelProperty(kb);
        Iri pSubProperty = Rdf.iri(kb.getSubPropertyIri().stringValue()); 
        
        return aVariable.has(() -> pSubProperty.getQueryString() + "*", pLabel);
    }

    /**
     * Find entries where the label matches exactly one of the given values. The match is
     * case-sensitive and it takes the default language of the KB into consideration.
     * 
     * <b>Note:</b> This matching does not make use of any fulltext search capabilities which the
     * triple store might provide.
     * 
     * @param aValues
     *            label values.
     * @return the builder (fluent API)
     */
    public SPARQLQueryBuilder withLabelMatchingExactlyAnyOf(String... aValues)
    {
        if (aValues.length == 0) {
            returnEmptyResult = true;
            return this;
        }
        
        IRI ftsMode = kb.getFullTextSearchIri();
        
        if (FTS_LUCENE.equals(ftsMode)) {
            addPattern(PRIO_PRIMARY, withLabelMatchingExactlyAnyOf_RDF4J_FTS(aValues));
        }
        else if (FTS_VIRTUOSO.equals(ftsMode)) {
            addPattern(PRIO_PRIMARY, withLabelMatchingExactlyAnyOf_Virtuoso_FTS(aValues));
        }
        else if (FTS_NONE.equals(ftsMode) || ftsMode == null) {
            addPattern(PRIO_PRIMARY, withLabelMatchingExactlyAnyOf_No_FTS(aValues));
        }
        else {
            throw new IllegalStateException(
                    "Unknown FTS mode: [" + kb.getFullTextSearchIri() + "]");
        }
        
        // Retain only the first description - do this here since we change the server-side reduce
        // flag above when using Lucene FTS
        projections.add(getLabelProjection());
        labelImplicitlyRetrieved = true;
        
        return this;
    }
    
    private GraphPattern withLabelMatchingExactlyAnyOf_No_FTS(String[] aValues)
    {
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            if (StringUtils.isBlank(value)) {
                continue;
            }
            
            // This works but it is too slow on Wikidata
//            valuePatterns.add(VAR_SUBJECT
//                    .has(pLabel, VAR_LABEL_CANDIDATE)
//                    .filter(equalsPattern(VAR_LABEL_CANDIDATE, value, kb)));

            // RDF4J query builder does not support the VALUES construct yet, so we have to hack it
            // in.
            GraphPattern customGP = new GraphPattern() {
                @Override
                public String getQueryString()
                {
                    String language =  kb.getDefaultLanguage();
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("VALUES (").append(VAR_LABEL_CANDIDATE.getQueryString()).append(") ");
                    sb.append("{ ");
                    sb.append("(").append(literalOf(value).getQueryString()).append(") ");
                    if (language != null) {
                        sb.append("(").append(literalOfLanguage(value, language).getQueryString())
                                .append(") ");
                    }
                    sb.append("} ");
                    sb.append(VAR_SUBJECT.has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                            .getQueryString());
                    return sb.toString();
                }
                
                @Override
                public boolean isEmpty()
                {
                    return false;
                }
                
            };
            
            valuePatterns.add(customGP);
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }
    
    private GraphPattern withLabelMatchingExactlyAnyOf_RDF4J_FTS(String[] aValues)
    {
        prefixes.add(PREFIX_LUCENE_SEARCH);
        
        Iri pLabelFts = iri(IriConstants.FTS_LUCENE.toString());
        
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            if (StringUtils.isBlank(value)) {
                continue;
            }
            
            valuePatterns.add(VAR_SUBJECT
                    .has(pLabelFts,
                            bNode(LUCENE_QUERY, literalOf(value))
                            .andHas(LUCENE_PROPERTY, VAR_LABEL_PROPERTY))
                    .andHas(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                    .filter(equalsPattern(VAR_LABEL_CANDIDATE, value, kb)));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }
    
    private GraphPattern withLabelMatchingExactlyAnyOf_Virtuoso_FTS(String[] aValues)
    {
        Iri pLabelFts = iri(FTS_VIRTUOSO.toString());
        
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            String sanitizedValue = sanitizeQueryStringForFTS(value);
            
            if (StringUtils.isBlank(sanitizedValue)) {
                continue;
            }
                        
            valuePatterns.add(VAR_SUBJECT
                    .has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                    .and(VAR_LABEL_CANDIDATE.has(pLabelFts,literalOf("\"" + sanitizedValue + "\"")))
                    .filter(equalsPattern(VAR_LABEL_CANDIDATE, value, kb)));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }

    /**
     * Find entries where the label starts with the given prefix. If fulltext search capabilities
     * are available, they will be used. Depending on the circumstances, the match may be case
     * sensitive or not.
     * 
     * @param aPrefixQuery
     *            label prefix.
     * @return the builder (fluent API)
     */
    public SPARQLQueryBuilder withLabelStartingWith(String aPrefixQuery)
    {
        if (aPrefixQuery.length() == 0) {
            returnEmptyResult = true;
            return this;
        }
        
        
        IRI ftsMode = kb.getFullTextSearchIri();
        
        if (IriConstants.FTS_LUCENE.equals(ftsMode)) {
            addPattern(PRIO_PRIMARY, withLabelStartingWith_RDF4J_FTS(aPrefixQuery));
        }
        else if (IriConstants.FTS_VIRTUOSO.equals(ftsMode)) {
            addPattern(PRIO_PRIMARY, withLabelStartingWith_Virtuoso_FTS(aPrefixQuery));
        }
        else if (IriConstants.FTS_NONE.equals(ftsMode) || ftsMode == null) {
            addPattern(PRIO_PRIMARY, withLabelStartingWith_No_FTS(aPrefixQuery));
        }
        else {
            throw new IllegalStateException(
                    "Unknown FTS mode: [" + kb.getFullTextSearchIri() + "]");
        }
        
        // Retain only the first description - do this here since we change the server-side reduce
        // flag above when using Lucene FTS
        projections.add(getLabelProjection());
        labelImplicitlyRetrieved = true;
        
        return this;
    }
    
    /**
     * Match any items with a label containing any of the given values.
     * 
     * @param aValues
     *            values to match.
     * @return the builder (fluent API)
     */
    public SPARQLQueryBuilder withLabelContainingAnyOf(String... aValues)
    {
        if (aValues.length == 0) {
            returnEmptyResult = true;
            return this;
        }
        
        IRI ftsMode = kb.getFullTextSearchIri();
        
        if (IriConstants.FTS_LUCENE.equals(ftsMode)) {
            addPattern(PRIO_PRIMARY, withLabelContainingAnyOf_RDF4J_FTS(aValues));
        }
        else if (IriConstants.FTS_VIRTUOSO.equals(ftsMode)) {
            addPattern(PRIO_PRIMARY, withLabelContainingAnyOf_Virtuoso_FTS(aValues));
        }
        else if (IriConstants.FTS_NONE.equals(ftsMode) || ftsMode == null) {
            addPattern(PRIO_PRIMARY, withLabelContainingAnyOf_No_FTS(aValues));
        }
        else {
            throw new IllegalStateException(
                    "Unknown FTS mode: [" + kb.getFullTextSearchIri() + "]");
        }
        
        // Retain only the first description - do this here since we change the server-side reduce
        // flag above when using Lucene FTS
        projections.add(getLabelProjection());
        labelImplicitlyRetrieved = true;
        
        return this;
    }

    private GraphPattern withLabelContainingAnyOf_No_FTS(String... aValues)
    {
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            if (StringUtils.isBlank(value)) {
                continue;
            }
            
            valuePatterns.add(VAR_SUBJECT
                    .has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                    .filter(containsPattern(VAR_LABEL_CANDIDATE, value)));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }
    
    private GraphPattern withLabelContainingAnyOf_RDF4J_FTS(String[] aValues)
    {
        prefixes.add(PREFIX_LUCENE_SEARCH);
        
        Iri pLabelFts = iri(IriConstants.FTS_LUCENE.toString());
        
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            String sanitizedValue = sanitizeQueryStringForFTS(value);

            if (StringUtils.isBlank(sanitizedValue)) {
                continue;
            }

            valuePatterns.add(VAR_SUBJECT
                    .has(pLabelFts,
                            bNode(LUCENE_QUERY, literalOf(sanitizedValue + "*"))
                            .andHas(LUCENE_PROPERTY, VAR_LABEL_PROPERTY))
                    .andHas(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                    .filter(containsPattern(VAR_LABEL_CANDIDATE, value)));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }

    private GraphPattern withLabelContainingAnyOf_Virtuoso_FTS(String[] aValues)
    {
        Iri pLabelFts = iri(FTS_VIRTUOSO.toString());
        
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            String sanitizedValue = sanitizeQueryStringForFTS(value);
            
            if (StringUtils.isBlank(sanitizedValue)) {
                continue;
            }
                        
            valuePatterns.add(VAR_SUBJECT
                    .has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                    .and(VAR_LABEL_CANDIDATE.has(pLabelFts,literalOf("\"" + sanitizedValue + "\"")))
                    .filter(containsPattern(VAR_LABEL_CANDIDATE, value)));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }

    private GraphPattern withLabelStartingWith_No_FTS(String aPrefixQuery)
    {
        if (aPrefixQuery.isEmpty()) {
            returnEmptyResult = true;
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                VAR_SUBJECT.has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                        .filter(startsWithPattern(VAR_LABEL_CANDIDATE, aPrefixQuery)));
    }

    private GraphPattern withLabelStartingWith_Virtuoso_FTS(String aPrefixQuery)
    {
        StringBuilder ftsQueryString = new StringBuilder();
        ftsQueryString.append("\"");
        
        // Strip single quotes and asterisks because they have special semantics
        String sanitizedQuery = sanitizeQueryStringForFTS(aPrefixQuery);
        
        // If the query string entered by the user does not end with a space character, then
        // we assume that the user may not yet have finished writing the word and add a
        // wildcard
        if (!aPrefixQuery.endsWith(" ")) {
            String[] queryTokens = sanitizedQuery.split(" ");
            for (int i = 0; i < queryTokens.length; i++) {
                if (i > 0) {
                    ftsQueryString.append(" ");
                }
                
                // Virtuoso requires that a token has at least 4 characters before it can be 
                // used with a wildcard. If the last token has less than 4 characters, we simply
                // drop it to avoid the user hitting a point where the auto-suggesions suddenly
                // are empty. If the token 4 or more, we add the wildcard.
                if (i == (queryTokens.length - 1)) {
                    if (queryTokens[i].length() >= 4) {
                        ftsQueryString.append(queryTokens[i]);
                        ftsQueryString.append("*");
                    }
                }
                else {
                    ftsQueryString.append(queryTokens[i]);
                }
            }
        }
        else {
            ftsQueryString.append(sanitizedQuery);
        }
        
        ftsQueryString.append("\"");
        
        // If the query string was reduced to nothing, then the query should always return an empty
        // result.
        if (ftsQueryString.length() == 2) {
            returnEmptyResult = true;
        }
        
        Iri pLabelFts = iri(FTS_VIRTUOSO.toString());
        
        // Locate all entries where the label contains the prefix (using the FTS) and then
        // filter them by those which actually start with the prefix.
        return GraphPatterns.and(bindLabelProperties(VAR_LABEL_PROPERTY),
                VAR_SUBJECT.has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                        .and(VAR_LABEL_CANDIDATE.has(pLabelFts,
                                literalOf(ftsQueryString.toString())))
                        .filter(startsWithPattern(VAR_LABEL_CANDIDATE, aPrefixQuery)));
    }

    private GraphPattern withLabelStartingWith_RDF4J_FTS(String aPrefixQuery)
    {
        // REC: Haven't been able to get this to work with server-side reduction, so implicitly
        // turning it off here.
        serverSideReduce = false;
        
        prefixes.add(PREFIX_LUCENE_SEARCH);
        
        String queryString = aPrefixQuery.trim();
        
        if (queryString.isEmpty()) {
            returnEmptyResult = true;
        }

        // If the query string entered by the user does not end with a space character, then
        // we assume that the user may not yet have finished writing the word and add a
        // wildcard
        if (!aPrefixQuery.endsWith(" ")) {
            queryString += "*";
        }

        Iri pLabelFts = iri(IriConstants.FTS_LUCENE.toString());

        // Locate all entries where the label contains the prefix (using the FTS) and then
        // filter them by those which actually start with the prefix.
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                VAR_SUBJECT.has(pLabelFts,bNode(LUCENE_QUERY, literalOf(queryString))
                        .andHas(LUCENE_PROPERTY, VAR_LABEL_PROPERTY))
                        .andHas(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                        .filter(startsWithPattern(VAR_LABEL_CANDIDATE, aPrefixQuery)));
    }

    private Expression<?> startsWithPattern(Variable aVariable, String aPrefixQuery)
    {
        return matchString(STRSTARTS, aVariable, aPrefixQuery);
    }

    private Expression<?> containsPattern(Variable aVariable, String aSubstring)
    {
        return matchString(CONTAINS, aVariable, aSubstring);
    }

    private Expression<?> equalsPattern(Variable aVariable, String aValue,
            KnowledgeBase aKB)
    {
        String language = aKB.getDefaultLanguage();
        
        List<Expression<?>> expressions = new ArrayList<>();
        
        // If case-insensitive mode is enabled, then lower-case the strings
        Operand variable = aVariable;
        String value = aValue;
        if (caseInsensitive) {
            variable = function(LCASE, function(STR, variable));
            value = value.toLowerCase();
        }
        
        // Match with default language
        if (language != null) {
            expressions.add(Expressions.equals(variable, literalOfLanguage(value, language)));
        }
        
        // Match without language
        expressions.add(Expressions.equals(variable, literalOf(value)));
        
        return or(expressions.toArray(new Expression<?>[expressions.size()]));
    }

    private Expression<?> matchString(SparqlFunction aFunction, Variable aVariable,
            String aValue)
    {
        String language = kb.getDefaultLanguage();

        List<Expression<?>> expressions = new ArrayList<>();

        // If case-insensitive mode is enabled, then lower-case the strings
        Operand variable = aVariable;
        String value = aValue;
        if (caseInsensitive) {
            variable = function(LCASE, function(STR, variable));
            value = value.toLowerCase();
        }
        
        // Match with default language
        if (language != null) {
            expressions.add(and(function(aFunction, variable, literalOf(value)),
                    function(LANGMATCHES, function(LANG, aVariable), literalOf(language)))
                            .parenthesize());
        }

        // Match without language
        expressions.add(function(aFunction, variable, literalOf(value)));
        
        return or(expressions.toArray(new Expression<?>[expressions.size()]));
    }

    /**
     * Limits results to items under the given branch of the ontology.
     */
    public void withScope(String aString)
    {
        Iri scopeIri = Rdf.iri(aString);
        
        addPattern(PRIO_PRIMARY, mode.scopePattern(kb, scopeIri));
    }

    private void limitToClasses()
    {
        Iri classIri = Rdf.iri(kb.getClassIri().toString());
        Iri subClassProperty = Rdf.iri(kb.getSubclassIri().toString());
        Iri typeOfProperty = Rdf.iri(kb.getTypeIri().toString());
        Variable someSubClass = SparqlBuilder.var("someSubClass");
        Variable someSuperClass = SparqlBuilder.var("someSuperClass");
        
        // An item is a class if ...
        addPattern(PRIO_SECONDARY, union(new GraphPattern[] {
                // ... it is explicitly defined as being a class
                VAR_SUBJECT.has(typeOfProperty, classIri),
                // ... it has any subclass
                someSubClass.has(subClassProperty, VAR_SUBJECT),
                // ... it has any superclass
                VAR_SUBJECT.has(subClassProperty, someSuperClass)}));
    }
    
    private void limitToInstances()
    {
        Iri classIri = Rdf.iri(kb.getClassIri().toString());
        Iri subClassProperty = Rdf.iri(kb.getSubclassIri().toString());
        Iri typeOfProperty = Rdf.iri(kb.getTypeIri().toString());
        Variable someSubClass = SparqlBuilder.var("someSubClass");
        Variable someSuperClass = SparqlBuilder.var("someSuperClass");
        Variable someType = SparqlBuilder.var("someType");
        
        // An item is a class if ...
        addPattern(PRIO_SECONDARY, VAR_SUBJECT.has(typeOfProperty, someType)
                // ... it is explicitly defined as being a class
                .filterNotExists(VAR_SUBJECT.has(typeOfProperty, classIri))
                // ... it has any subclass
                .filterNotExists(someSubClass.has(subClassProperty, VAR_SUBJECT))
                // ... it has any superclass
                .filterNotExists(VAR_SUBJECT.has(subClassProperty, someSuperClass)));
    }
        
    /**
     * Request that a label be retrieved as part of the query.
     * 
     * @return the builder (fluent API)
     */
    public SPARQLQueryBuilder retrieveLabel()
    {
        // If the label is already retrieved, do nothing
        if (labelImplicitlyRetrieved) {
            return this;
        }
        
        if (!hasPrimaryPatterns()) {
            throw new IllegalStateException("Call a method which adds primary patterns first");
        }
        
        // Retain only the first description
        projections.add(getLabelProjection());
        
        String language = kb.getDefaultLanguage();
        
        Iri labelProperty = mode.getLabelProperty(kb);

        addPattern(PRIO_SECONDARY, bindLabelProperties(VAR_LABEL_PROPERTY));
        
        // Find all labels corresponding to the KB language
        if (language != null) {
            addPattern(PRIO_SECONDARY,
                    optional(VAR_SUBJECT.has(labelProperty, VAR_LABEL_CANDIDATE)
                            .filter(function(LANGMATCHES, function(LANG, VAR_LABEL_CANDIDATE),
                                    literalOf(language)))));
        }

        // Find all descriptions without any language
        addPattern(PRIO_SECONDARY, optional(VAR_SUBJECT.has(labelProperty, VAR_LABEL_CANDIDATE)));
        
        return this;
    }

    /**
     * Request that a description be retrieved as part of the query.
     * 
     * @return the builder (fluent API)
     */
    public SPARQLQueryBuilder retrieveDescription()
    {
        if (!hasPrimaryPatterns()) {
            throw new IllegalStateException("Call a method which adds primary patterns first");
        }
        
        // Retain only the first description
        projections.add(getDescriptionProjection());
        
        String language = kb.getDefaultLanguage();
        Iri descProperty = mode.getDescriptionProperty(kb);
        
        // Find all descriptions corresponding to the KB language
        if (language != null) {
            addPattern(PRIO_SECONDARY, optional(VAR_SUBJECT.has(descProperty, VAR_DESC_CANDIDATE)
                    .filter(function(LANGMATCHES, function(LANG, VAR_DESC_CANDIDATE), 
                            literalOf(language)))));
        }

        // Find all descriptions without any language
        addPattern(PRIO_SECONDARY, optional(VAR_SUBJECT.has(descProperty, VAR_DESC_CANDIDATE)));

        return this;
    }
    
    /**
     * Assemble the current configuration into a SELECT query.
     * 
     * @return SELECT query.
     */
    public SelectQuery selectQuery()
    {
        // Must add it anyway because we group by it
        projections.add(VAR_SUBJECT);

        SelectQuery query = Queries.SELECT().distinct();
        prefixes.forEach(query::prefix);
        projections.forEach(query::select);
        patterns.stream()
                .sorted(Comparator.comparing(Pair::getKey))
                .forEach(prioritizedCondition -> query.where(prioritizedCondition.getValue()));
        
        if (serverSideReduce) {
            query.groupBy(VAR_SUBJECT);
        }
        
        query.limit(kb.getMaxResults());
        
        return query;
    }
    
    /**
     * Execute the query and retrieve the results as {@link KBHandle KBHandles}.
     * 
     * @param aConnection
     *            a connection to a triple store.
     * @param aAll
     *            True if entities with implicit namespaces (e.g. defined by RDF)
     * @return a list of the retrieved handles.
     */
    public List<KBHandle> asHandles(RepositoryConnection aConnection, boolean aAll)
    {
        long startTime = currentTimeMillis();
        String queryId = toHexString(hashCode());

        String queryString = selectQuery().getQueryString();
        LOG.trace("[{}] Query: {}", queryId, queryString);

        List<KBHandle> results;
        if (returnEmptyResult) {
            results = emptyList();
            
            LOG.debug("[{}] Query was skipped because it would not return any results anyway",
                    queryId);
        }
        else {
            TupleQuery tupleQuery = aConnection.prepareTupleQuery(queryString);
            results = evaluateListQuery(tupleQuery, aAll);
            results.sort(Comparator.comparing(KBObject::getUiLabel));
            
            LOG.debug("[{}] Query returned {} results in {}ms", queryId, results.size(),
                    currentTimeMillis() - startTime);
        }

        return results;
    }
    
    /**
     * Method process the Tuple Query Results
     * 
     * @param tupleQuery
     *            Tuple Query Variable
     * @param aAll
     *            True if entities with implicit namespaces (e.g. defined by RDF)
     * @return list of all the {@link KBHandle}
     */
    private List<KBHandle> evaluateListQuery(TupleQuery tupleQuery, boolean aAll)
        throws QueryEvaluationException
    {
        TupleQueryResult result = tupleQuery.evaluate();        
        
        List<KBHandle> handles = new ArrayList<>();
        while (result.hasNext()) {
            BindingSet bindings = result.next();
            if (bindings.size() == 0) {
                continue;
            }
            
            LOG.trace("[{}] Bindings: {}", toHexString(hashCode()), bindings);

            String id = bindings.getBinding(VAR_SUBJECT_NAME).getValue().stringValue();
            if (!id.contains(":") || (!aAll && hasImplicitNamespace(kb, id))) {
                continue;
            }
            
            KBHandle handle = new KBHandle(id);
            handle.setKB(kb);
            
            extractLabel(handle, bindings);
            extractDescription(handle, bindings);
            extractRange(handle, bindings);
            extractDomain(handle, bindings);

            handles.add(handle);
        }
        
        if (serverSideReduce) {
            return handles;
        }
        else {
            return reduceRedundantResults(handles);
        }
    }
    
    /**
     * Make sure that each result is only represented once, preferably in the default language.
     */
    private List<KBHandle> reduceRedundantResults(List<KBHandle> aHandles)
    {
        Map<String, KBHandle> cMap = new LinkedHashMap<>();
        for (KBHandle handle : aHandles) {
            if (!cMap.containsKey(handle.getIdentifier())) {
                cMap.put(handle.getIdentifier(), handle);
            }
            else if (kb.getDefaultLanguage().equals(handle.getLanguage())) {
                cMap.put(handle.getIdentifier(), handle);
            }
        }
        
//        LOG.trace("Input: {}", aHandles);
//        LOG.trace("Output: {}", cMap.values());
        
        return new ArrayList<>(cMap.values());
    }
    
    private void extractLabel(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        // If server-side reduce is used, the label is in VAR_LABEL_NAME
        Binding label = aSourceBindings.getBinding(VAR_LABEL_NAME);
        // If client-side reduce is used, the label is in VAR_LABEL_CANDIDATE_NAME
        Binding labelCandidate = aSourceBindings.getBinding(VAR_LABEL_CANDIDATE_NAME);
        Binding subPropertyLabel = aSourceBindings.getBinding("spl");
        if (label != null) {
            aTargetHandle.setName(label.getValue().stringValue());
            if (label.getValue() instanceof Literal) {
                Literal literal = (Literal) label.getValue();
                literal.getLanguage().ifPresent(aTargetHandle::setLanguage);
            }
        }
        else if (labelCandidate != null) {
            aTargetHandle.setName(labelCandidate.getValue().stringValue());
            if (labelCandidate.getValue() instanceof Literal) {
                Literal literal = (Literal) labelCandidate.getValue();
                literal.getLanguage().ifPresent(aTargetHandle::setLanguage);
            }
        }
        else if (subPropertyLabel != null) {
            aTargetHandle.setName(subPropertyLabel.getValue().stringValue());
            if (subPropertyLabel.getValue() instanceof Literal) {
                Literal literal = (Literal) subPropertyLabel.getValue();
                literal.getLanguage().ifPresent(aTargetHandle::setLanguage);
            }
        }
        else {
            aTargetHandle.setName(aTargetHandle.getUiLabel());
        }
    }
    
    private void extractDescription(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        Binding description = aSourceBindings.getBinding(VAR_DESCRIPTION_NAME);
        Binding descGeneral = aSourceBindings.getBinding("descGeneral");
        if (description != null) {
            aTargetHandle.setDescription(description.getValue().stringValue());
        }
        else if (descGeneral != null) {
            aTargetHandle.setDescription(descGeneral.getValue().stringValue());
        }
    }
    
    private void extractDomain(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        Binding domain = aSourceBindings.getBinding("dom");
        if (domain != null) {
            aTargetHandle.setDomain(domain.getValue().stringValue());
        }
    }

    private void extractRange(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        Binding range = aSourceBindings.getBinding("range");
        if (range != null) {
            aTargetHandle.setRange(range.getValue().stringValue());
        }
    }
    
    private String sanitizeQueryStringForFTS(String aQuery)
    {
        return aQuery.trim().replaceAll("[*\\p{Punct}]", " ").trim();
    }
}
