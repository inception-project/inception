/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.kb.querybuilder;

import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_ALLEGRO_GRAPH;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_BLAZEGRAPH;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_FUSEKI;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_NONE;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_RDF4J_LUCENE;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_STARDOG;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_VIRTUOSO;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_WIKIDATA;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.hasImplicitNamespace;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Priority.PRIMARY;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Priority.PRIMARY_RESTRICTIONS;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Priority.SECONDARY;
import static java.lang.Character.isWhitespace;
import static java.lang.Integer.toHexString;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.and;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.function;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.notEquals;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.or;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.CONTAINS;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.LANG;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.LANGMATCHES;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.REGEX;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.STR;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.STRSTARTS;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.dataset;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.from;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.and;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.filterExists;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.filterNotExists;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.optional;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.union;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.bNode;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IllformedLocaleException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPathBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfValue;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

/**
 * Build queries against the KB.
 * 
 * <p>
 * <b>Handling of subclasses: </b> Queries for subclasses return only resources which declare being
 * a class via the class property defined in the KB specification. This means that if the KB is
 * configured to use rdfs:Class but a subclass defines itself using owl:Class, then this subclass is
 * *not* returned. We do presently *not* support mixed schemes in a single KB.
 * </p>
 */
public class SPARQLQueryBuilder
    implements SPARQLVariables, SPARQLQuery, SPARQLQueryPrimaryConditions,
    SPARQLQueryOptionalElements
{
    private final static Logger LOG = LoggerFactory.getLogger(SPARQLQueryBuilder.class);

    public static final Pattern TOKENKIZER_PATTERN = Pattern.compile("\\s+");

    public static final int DEFAULT_LIMIT = 0;

    private static final RdfValue EMPTY_STRING = () -> "\"\"";

    private final Set<Prefix> prefixes = new LinkedHashSet<>();
    private final Set<Projectable> projections = new LinkedHashSet<>();

    private final List<GraphPattern> primaryPatterns = new ArrayList<>();
    private final List<GraphPattern> primaryRestrictions = new ArrayList<>();
    private final List<GraphPattern> secondaryPatterns = new ArrayList<>();

    private boolean labelImplicitlyRetrieved = false;

    private boolean filterUsingRegex = true;

    enum Priority
    {
        PRIMARY, PRIMARY_RESTRICTIONS, SECONDARY
    }

    /**
     * This flag is set internally to indicate whether the query should be skipped and an empty
     * result should always be returned. This can be the case, e.g. if the post-processing of the
     * query string against which to match a label causes the query to become empty.
     */
    private boolean returnEmptyResult = false;

    private final KnowledgeBase kb;
    private final Mode mode;

    private List<String> fallbackLanguages = emptyList();
    private List<String> preResolvedPrefLabelProperties = emptyList();
    private List<String> preResolvedAdditionalMatchingProperties = emptyList();

    /**
     * Case-insensitive mode is a best-effort approach. Depending on the underlying FTS, it may or
     * may not work.
     */
    private boolean caseInsensitive = true;

    private int limitOverride = DEFAULT_LIMIT;

    private boolean includeInferred = true;

    private Set<String> forceDisableFTS = new LinkedHashSet<>();

    /**
     * This flag controls whether we attempt to drop duplicate labels and descriptions on the side
     * of the SPARQL server (true) or whether we try retrieving all labels and descriptions which
     * have either no language or match the KB language and then drop duplicates on our side. Both
     * approaches have benefits and draw-backs. In general, we try server-side reduction to reduce
     * the data being transferred across the wire. However, in some cases we implicitly turn off
     * server side reduction because (SSR) we have not been able to figure out working SSR queries.
     * 
     * Benefits of SSR:
     * <ul>
     * <li>Less data to transfer across the wire</li>
     * <li>LIMIT works accurately (if we drop client side, we may end up with less than LIMIT
     * results)</li>
     * </ul>
     * 
     * Drawbacks of SSR:
     * <ul>
     * <li>More complex queries</li>
     * </ul>
     * 
     * @see #reduceRedundantResults(List)
     */

    static enum Mode
    {
        ITEM, CLASS, INSTANCE, PROPERTY;

        protected Iri getLabelProperty(KnowledgeBase aKb)
        {
            return iri(getLabelPropertyAsString(aKb));
        }

        protected String getLabelPropertyAsString(KnowledgeBase aKb)
        {
            switch (this) {
            case ITEM, CLASS, INSTANCE:
                return aKb.getLabelIri();
            case PROPERTY:
                return aKb.getPropertyLabelIri();
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }
        }

        protected Iri getDescriptionProperty(KnowledgeBase aKb)
        {
            switch (this) {
            case ITEM, CLASS, INSTANCE:
                return iri(aKb.getDescriptionIri());
            case PROPERTY:
                return iri(aKb.getPropertyDescriptionIri());
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }
        }

        protected List<Iri> getAdditionalMatchingProperties(KnowledgeBase aKb)
        {
            switch (this) {
            case ITEM, CLASS, INSTANCE:
                return aKb.getAdditionalMatchingProperties().stream() //
                        .map(Rdf::iri) //
                        .toList();
            case PROPERTY:
                return emptyList();
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }
        }

        /**
         * @see SPARQLQueryPrimaryConditions#descendantsOf(String)
         */
        protected GraphPattern descendentsPattern(KnowledgeBase aKB, Iri aContext)
        {
            var typeOfProperty = iri(aKB.getTypeIri());
            var subClassProperty = iri(aKB.getSubclassIri());
            var subPropertyProperty = iri(aKB.getSubPropertyIri());

            switch (this) {
            case ITEM: {
                var classPatterns = new ArrayList<GraphPattern>();
                classPatterns.add(VAR_SUBJECT.has(
                        PropertyPathBuilder.of(subClassProperty).oneOrMore().build(), aContext));
                classPatterns.add(VAR_SUBJECT.has(PropertyPathBuilder.of(typeOfProperty)
                        .then(subClassProperty).zeroOrMore().build(), aContext));
                if (OWL.CLASS.stringValue().equals(aKB.getClassIri())) {
                    classPatterns.add(VAR_SUBJECT.has(OWL_INTERSECTIONOF_PATH, aContext));
                }

                return union(classPatterns.stream().toArray(GraphPattern[]::new));
            }
            case CLASS: {
                var classPatterns = new ArrayList<GraphPattern>();
                classPatterns.add(VAR_SUBJECT.has(
                        PropertyPathBuilder.of(subClassProperty).oneOrMore().build(), aContext));
                if (OWL.CLASS.stringValue().equals(aKB.getClassIri())) {
                    classPatterns.add(VAR_SUBJECT.has(OWL_INTERSECTIONOF_PATH, aContext));
                }

                return union(classPatterns.stream().toArray(GraphPattern[]::new));
            }
            case INSTANCE:
                return VAR_SUBJECT.has(PropertyPathBuilder.of(typeOfProperty).then(subClassProperty)
                        .zeroOrMore().build(), aContext);
            case PROPERTY:
                return VAR_SUBJECT.has(
                        PropertyPathBuilder.of(subPropertyProperty).oneOrMore().build(), aContext);
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }
        }

        /**
         * @see SPARQLQueryPrimaryConditions#ancestorsOf(String)
         */
        protected GraphPattern ancestorsPattern(KnowledgeBase aKB, Iri aContext)
        {
            var typeOfProperty = iri(aKB.getTypeIri());
            var subClassProperty = iri(aKB.getSubclassIri());
            var subPropertyProperty = iri(aKB.getSubPropertyIri());

            switch (this) {
            case ITEM, CLASS, INSTANCE: {
                var classPatterns = new ArrayList<GraphPattern>();
                classPatterns.add(aContext.has(
                        PropertyPathBuilder.of(subClassProperty).oneOrMore().build(), VAR_SUBJECT));
                classPatterns.add(aContext.has(PropertyPathBuilder.of(typeOfProperty)
                        .then(subClassProperty).zeroOrMore().build(), VAR_SUBJECT));
                if (OWL.CLASS.stringValue().equals(aKB.getClassIri())) {
                    classPatterns.add(aContext.has(OWL_INTERSECTIONOF_PATH, VAR_SUBJECT));
                }

                return union(classPatterns.stream().toArray(GraphPattern[]::new));
            }
            case PROPERTY:
                return aContext.has(PropertyPathBuilder.of(subPropertyProperty).oneOrMore().build(),
                        VAR_SUBJECT);
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }
        }

        /**
         * @see SPARQLQueryPrimaryConditions#childrenOf(String)
         */
        protected GraphPattern childrenPattern(KnowledgeBase aKB, Iri aContext)
        {
            var subPropertyProperty = iri(aKB.getSubPropertyIri());
            var subClassProperty = iri(aKB.getSubclassIri());
            var typeOfProperty = iri(aKB.getTypeIri());

            switch (this) {
            case ITEM: {
                var classPatterns = new ArrayList<GraphPattern>();
                classPatterns
                        .add(VAR_SUBJECT.has(() -> subClassProperty.getQueryString(), aContext));
                classPatterns.add(VAR_SUBJECT.has(typeOfProperty, aContext));
                if (OWL.CLASS.stringValue().equals(aKB.getClassIri())) {
                    classPatterns.add(VAR_SUBJECT.has(OWL_INTERSECTIONOF_PATH, aContext));
                }

                return union(classPatterns.stream().toArray(GraphPattern[]::new));
            }
            case INSTANCE: {
                return VAR_SUBJECT.has(typeOfProperty, aContext);
            }
            case CLASS: {
                // Follow the subclass property and also take into account owl:intersectionOf if
                // using OWL classes
                var classPatterns = new ArrayList<GraphPattern>();
                classPatterns.add(VAR_SUBJECT.has(subClassProperty, aContext));
                if (OWL.CLASS.stringValue().equals(aKB.getClassIri())) {
                    classPatterns.add(VAR_SUBJECT.has(OWL_INTERSECTIONOF_PATH, aContext));
                }

                return union(classPatterns.stream().toArray(GraphPattern[]::new));
            }
            case PROPERTY:
                return VAR_SUBJECT.has(subPropertyProperty, aContext);
            default:
                throw new IllegalStateException("Can only request children of classes");
            }
        }

        /**
         * @see SPARQLQueryPrimaryConditions#parentsOf(String)
         */
        protected GraphPattern parentsPattern(KnowledgeBase aKB, Iri aContext)
        {
            var subClassProperty = iri(aKB.getSubclassIri());
            var subPropertyProperty = iri(aKB.getSubPropertyIri());
            var typeOfProperty = iri(aKB.getTypeIri());

            switch (this) {
            case CLASS: {
                var classPatterns = new ArrayList<GraphPattern>();
                classPatterns.add(aContext.has(subClassProperty, VAR_SUBJECT));
                classPatterns.add(aContext.has(typeOfProperty, VAR_SUBJECT));
                if (OWL.CLASS.stringValue().equals(aKB.getClassIri())) {
                    classPatterns.add(aContext.has(OWL_INTERSECTIONOF_PATH, VAR_SUBJECT));
                }

                return union(classPatterns.stream().toArray(GraphPattern[]::new));
            }
            case PROPERTY:
                return aContext.has(PropertyPathBuilder.of(subPropertyProperty).oneOrMore().build(),
                        VAR_SUBJECT);
            default:
                throw new IllegalStateException(
                        "Can only request classes or properties as parents");
            }
        }

        /**
         * @see SPARQLQueryPrimaryConditions#roots()
         */
        protected GraphPattern rootsPattern(KnowledgeBase aKb)
        {
            var classIri = iri(aKb.getClassIri());
            var subClassProperty = iri(aKb.getSubclassIri());
            var typeOfProperty = iri(aKb.getTypeIri());
            var otherSubclass = var("otherSubclass");

            switch (this) {
            case CLASS: {
                var rootPatterns = new ArrayList<GraphPattern>();

                var rootConcepts = aKb.getRootConcepts();
                if (rootConcepts != null && !rootConcepts.isEmpty()) {
                    rootPatterns.add(new ValuesPattern(VAR_SUBJECT,
                            rootConcepts.stream().map(iri -> iri(iri)).collect(toList())));
                }
                else {
                    var classPatterns = new ArrayList<GraphPattern>();
                    classPatterns.add(VAR_SUBJECT.has(subClassProperty, otherSubclass)
                            .filter(notEquals(VAR_SUBJECT, otherSubclass)));
                    if (OWL.CLASS.stringValue().equals(aKb.getClassIri())) {
                        classPatterns.add(VAR_SUBJECT.has(OWL_INTERSECTIONOF, bNode()));
                    }

                    rootPatterns.add(union(
                            // ... it is explicitly defined as being a class
                            VAR_SUBJECT.has(typeOfProperty, classIri),
                            // ... it is used as the type of some instance
                            // This can be a very slow condition - so we have to skip it
                            // bNode().has(typeOfProperty, VAR_SUBJECT),
                            // ... it has any subclass
                            bNode().has(subClassProperty, VAR_SUBJECT)).filterNotExists(
                                    union(classPatterns.stream().toArray(GraphPattern[]::new))));
                }

                return and(rootPatterns.toArray(GraphPattern[]::new));
            }
            default:
                throw new IllegalStateException("Can only query for root classes");
            }
        }

        protected Iri getDeprecationProperty(KnowledgeBase aKb)
        {
            switch (this) {
            case ITEM, CLASS, INSTANCE, PROPERTY:
                return iri(aKb.getDeprecationPropertyIri());
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }
        }
    }

    /**
     * Retrieve any item from the KB. There is no check if the item looks like a class, instance or
     * property. The IRI and property mapping used in the patters is obtained from the given KB
     * configuration.
     * 
     * @param aKB
     *            the knowledge base to query
     * @return the builder (fluent API)
     */
    public static SPARQLQueryPrimaryConditions forItems(KnowledgeBase aKB)
    {
        return new SPARQLQueryBuilder(aKB, Mode.ITEM);
    }

    /**
     * Retrieve only things that look like classes. Identifiers for classes participate as ID in
     * {@code ID IS-A CLASS-IRI}, {@code X SUBCLASS-OF ID}, {@code ID SUBCLASS-OF X}. The IRI and
     * property mapping used in the patters is obtained from the given KB configuration.
     * 
     * @param aKB
     *            the knowledge base to query
     * @return the builder (fluent API)
     */
    public static SPARQLQueryPrimaryConditions forClasses(KnowledgeBase aKB)
    {
        var builder = new SPARQLQueryBuilder(aKB, Mode.CLASS);
        builder.limitToClasses();
        return builder;
    }

    /**
     * Retrieve instances. Instances do <b>not</b> look like classes. The IRI and property mapping
     * used in the patters is obtained from the given KB configuration.
     * 
     * @param aKB
     *            the knowledge base to query
     * @return the builder (fluent API)
     */
    public static SPARQLQueryPrimaryConditions forInstances(KnowledgeBase aKB)
    {
        var builder = new SPARQLQueryBuilder(aKB, Mode.INSTANCE);
        builder.limitToInstances();
        return builder;
    }

    /**
     * Retrieve properties. The IRI and property mapping used in the patters is obtained from the
     * given KB configuration.
     * 
     * @param aKB
     *            the knowledge base to query
     * @return the builder (fluent API)
     */
    public static SPARQLQueryPrimaryConditions forProperties(KnowledgeBase aKB)
    {
        var builder = new SPARQLQueryBuilder(aKB, Mode.PROPERTY);
        builder.limitToProperties();
        return builder;
    }

    SPARQLQueryBuilder(KnowledgeBase aKB, Mode aMode)
    {
        kb = aKB;
        mode = aMode;

        // The Wikidata search service we are using does not return properties, so we have to do
        // this the old-fashioned way...
        if (Mode.PROPERTY.equals(mode)
                && FTS_WIKIDATA.stringValue().equals(aKB.getFullTextSearchIri())) {
            forceDisableFTS.add("Wikidata property query");
        }

        var appCtx = ApplicationContextProvider.getApplicationContext();
        if (appCtx != null) {
            var props = appCtx.getBean(KnowledgeBaseProperties.class);
            var langs = new LinkedHashSet<>(props.getDefaultFallbackLanguages());
            langs.addAll(aKB.getAdditionalLanguages());
            withFallbackLanguages(langs);
        }
    }

    void addPattern(Priority aPriority, GraphPattern aPattern)
    {
        switch (aPriority) {
        case PRIMARY:
            primaryPatterns.add(aPattern);
            break;
        case PRIMARY_RESTRICTIONS:
            primaryRestrictions.add(aPattern);
            break;
        case SECONDARY:
            secondaryPatterns.add(aPattern);
            break;
        default:
            throw new IllegalArgumentException("Unknown priority: [" + aPriority + "]");
        }
    }

    private void addMatchTermProjections(Collection<Projectable> aProjectables)
    {
        aProjectables.add(VAR_MATCH_TERM);
    }

    private void addPreferredLabelProjections(Collection<Projectable> aProjectables)
    {
        aProjectables.add(VAR_PREF_LABEL);
    }

    private Projectable getDescriptionProjection()
    {
        return VAR_DESC_CANDIDATE;
    }

    private Projectable getDeprecationProjection()
    {
        return VAR_DEPRECATION;
    }

    boolean noResult()
    {
        return returnEmptyResult = true;
    }

    @Override
    public SPARQLQueryPrimaryConditions withPrefLabelProperties(Collection<String> aProps)
    {
        preResolvedPrefLabelProperties = new ArrayList<>(aProps);

        return this;
    }

    @Override
    public SPARQLQueryPrimaryConditions withAdditionalMatchingProperties(Collection<String> aProps)
    {
        preResolvedAdditionalMatchingProperties = new ArrayList<>(aProps);

        return this;
    }

    @Override
    public SPARQLQueryPrimaryConditions withFallbackLanguages(String... aString)
    {
        fallbackLanguages = Stream.of(aString).distinct().toList();

        return this;
    }

    @Override
    public SPARQLQueryPrimaryConditions withFallbackLanguages(Collection<String> aString)
    {
        fallbackLanguages = aString.stream().distinct().toList();

        return this;
    }

    @Override
    public SPARQLQueryOptionalElements includeInferred()
    {
        includeInferred(true);

        return this;
    }

    @Override
    public SPARQLQueryOptionalElements excludeInferred()
    {
        includeInferred(false);

        return this;
    }

    @Override
    public SPARQLQueryOptionalElements includeInferred(boolean aEnabled)
    {
        includeInferred = aEnabled;

        return this;
    }

    @Override
    public SPARQLQueryOptionalElements limit(int aLimit)
    {
        limitOverride = aLimit;
        return this;
    }

    @Override
    public SPARQLQueryOptionalElements caseSensitive()
    {
        caseInsensitive = false;
        return this;
    }

    @Override
    public SPARQLQueryOptionalElements caseSensitive(boolean aEnabled)
    {
        caseInsensitive = !aEnabled;
        return this;
    }

    @Override
    public SPARQLQueryOptionalElements caseInsensitive()
    {
        caseInsensitive = true;
        return this;
    }

    KnowledgeBase getKnowledgeBase()
    {
        return kb;
    }

    boolean isCaseInsensitive()
    {
        return caseInsensitive;
    }

    void addPrefix(Prefix aPrefix)
    {
        prefixes.add(aPrefix);
    }

    void addProjection(Projectable aProjectable)
    {
        projections.add(aProjectable);
    }

    void setReturnEmptyResult(boolean aReturnEmptyResult)
    {
        returnEmptyResult = aReturnEmptyResult;
    }

    /**
     * Generates a pattern which binds all sub-properties of the label property to the given
     * variable.
     */
    GraphPattern bindPrefLabelProperties(Variable aVariable)
    {
        if (!preResolvedPrefLabelProperties.isEmpty()) {
            return bindPrefLabelProperties(aVariable, preResolvedPrefLabelProperties);
        }

        var pLabel = mode.getLabelProperty(kb);
        var pSubProperty = iri(kb.getSubPropertyIri());
        var pattern = aVariable.has(PropertyPathBuilder.of(pSubProperty).zeroOrMore().build(),
                pLabel);
        return optional(pattern);
    }

    /**
     * Generates a pattern which binds all sub-properties of the label property to the given
     * variable.
     */
    private GraphPattern bindPrefLabelProperties(Variable aVariable,
            Collection<String> aPrefLabelProperties)
    {
        return optional(new ValuesPattern(aVariable,
                aPrefLabelProperties.stream().map(Rdf::iri).toArray(RdfValue[]::new)));
    }

    /**
     * Generates a pattern which binds all sub-properties of the matching properties to the given
     * variable.
     */
    GraphPattern bindMatchTermProperties(Variable aVariable)
    {
        if (!preResolvedPrefLabelProperties.isEmpty()) {
            return bindMatchTermProperties(aVariable, preResolvedPrefLabelProperties,
                    preResolvedAdditionalMatchingProperties);
        }

        var pLabel = mode.getLabelProperty(kb);
        var pSubProperty = iri(kb.getSubPropertyIri());
        var pattern = aVariable.has(PropertyPathBuilder.of(pSubProperty).zeroOrMore().build(),
                pLabel);

        var additionalMatchingProperties = mode.getAdditionalMatchingProperties(kb);
        if (additionalMatchingProperties.isEmpty()) {
            // If we only have a single label property, let's make the label optional
            // so that we also get a result for things that might potentially not have
            // any label at all.
            return optional(pattern);
        }

        var patterns = new ArrayList<TriplePattern>();
        patterns.add(pattern);

        for (var pAddSearch : additionalMatchingProperties) {
            patterns.add(aVariable.has(PropertyPathBuilder.of(pSubProperty).zeroOrMore().build(),
                    pAddSearch));
        }

        // If additional label properties are specified, then having a label candidate
        // becomes mandatory, otherwise we get one result for every label property and
        // additional label property and their sub-properties for every concept and that
        // is simply too much.
        return union(patterns.stream().toArray(TriplePattern[]::new));
    }

    private GraphPattern bindMatchTermProperties(Variable aVariable,
            Collection<String> aPrefLabelProperties, Collection<String> aAdditionalLabelProperties)
    {
        if (aAdditionalLabelProperties.isEmpty()) {
            var pattern = new ValuesPattern(aVariable,
                    aPrefLabelProperties.stream().map(Rdf::iri).toArray(RdfValue[]::new));
            return pattern;
        }

        var allProps = new LinkedHashSet<String>();
        allProps.addAll(aPrefLabelProperties);
        allProps.addAll(aAdditionalLabelProperties);

        // If additional label properties are specified, then having a label candidate
        // becomes mandatory, otherwise we get one result for every label property and
        // additional label property and their sub-properties for every concept and that
        // is simply too much.
        return new ValuesPattern(aVariable,
                allProps.stream().map(Rdf::iri).toArray(RdfValue[]::new));
    }

    @Override
    public SPARQLQueryPrimaryConditions withIdentifier(String... aIdentifiers)
    {
        forceDisableFTS.add("identifier query");

        addPattern(PRIMARY, new ValuesPattern(VAR_SUBJECT,
                Arrays.stream(aIdentifiers).map(Rdf::iri).toArray(RdfValue[]::new)));

        return this;
    }

    @Override
    public SPARQLQueryPrimaryConditions matchingDomain(String aIdentifier)
    {
        forceDisableFTS.add("domain query");

        // The original code considered owl:unionOf in the domain definition... we do not do this
        // at the moment, but to see how it was before and potentially restore that behavior, we
        // keep a copy of the old query here.
        // return String.join("\n"
        // , SPARQL_PREFIX
        // , "SELECT DISTINCT ?s ?l ((?labelGeneral) AS ?lGen) WHERE {"
        // , "{ ?s rdfs:domain/(owl:unionOf/rdf:rest*/rdf:first)* ?aDomain }"
        // , " UNION "
        // , "{ ?s a ?prop "
        // , " VALUES ?prop { rdf:Property owl:ObjectProperty owl:DatatypeProperty
        // owl:AnnotationProperty} "
        // , " FILTER NOT EXISTS { ?s rdfs:domain/(owl:unionOf/rdf:rest*/rdf:first)* ?x } }"
        // , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?s","?l")
        // , optionalLanguageFilteredValue("?pLABEL", null,"?s","?labelGeneral")
        // , queryForOptionalSubPropertyLabel(labelProperties, aKB.getDefaultLanguage(),"?s","?spl")
        // , "}"
        // , "LIMIT " + aKB.getMaxResults());

        var subClassProperty = iri(kb.getSubclassIri());
        var subPropertyProperty = iri(kb.getSubPropertyIri());
        var superClass = Rdf.bNode("superClass");

        addPattern(PRIMARY, union(GraphPatterns.and(
                // Find all super-classes of the domain type
                iri(aIdentifier).has(PropertyPathBuilder.of(subClassProperty).zeroOrMore().build(),
                        superClass),
                // Either there is a domain which matches the given one
                VAR_SUBJECT.has(RDFS.DOMAIN, superClass)),
                // ... the property does not define or inherit domain
                isPropertyPattern().and(
                        filterNotExists(VAR_SUBJECT.has(PropertyPathBuilder.of(subPropertyProperty)
                                .zeroOrMore().then(RDFS.DOMAIN).build(), bNode())))));

        return this;
    }

    @Override
    public SPARQLQueryBuilder withLabelMatchingExactlyAnyOf(String... aValues)
    {
        var values = Arrays.stream(aValues) //
                .map(SPARQLQueryBuilder::trimQueryString) //
                .filter(StringUtils::isNotBlank) //
                .toArray(String[]::new);

        if (values.length == 0) {
            noResult();
            return this;
        }

        getAdapter().withLabelMatchingExactlyAnyOf(aValues);

        addMatchTermProjections(projections);
        labelImplicitlyRetrieved = true;

        return this;
    }

    private FtsAdapter getAdapter()
    {
        var ftsMode = getFtsMode();

        if (FTS_RDF4J_LUCENE.equals(ftsMode)) {
            return new FtsAdapterRdf4J(this);
        }

        if (FTS_BLAZEGRAPH.equals(ftsMode)) {
            return new FtsAdapterBlazegraph(this);
        }

        if (FTS_FUSEKI.equals(ftsMode)) {
            return new FtsAdapterFuseki(this);
        }

        if (FTS_VIRTUOSO.equals(ftsMode)) {
            return new FtsAdapterVirtuoso(this);
        }

        if (FTS_STARDOG.equals(ftsMode)) {
            return new FtsAdapterStardog(this);
        }

        if (FTS_WIKIDATA.equals(ftsMode)) {
            return new FtsAdapterWikidata(this);
        }

        if (FTS_ALLEGRO_GRAPH.equals(ftsMode)) {
            return new FtsAdapterAllegroGraph(this);
        }

        if (FTS_NONE.equals(ftsMode) || ftsMode == null) {
            return new FtsAdapterNoFts(this);
        }
        throw new IllegalStateException("Unknown FTS mode: [" + kb.getFullTextSearchIri() + "]");
    }

    private IRI getFtsMode()
    {
        if (kb.getFullTextSearchIri() == null) {
            return FTS_NONE;
        }

        if (isFtsLimited() && !forceDisableFTS.isEmpty()) {
            LOG.debug("FTS force-disabled because it limits its results: {}", forceDisableFTS);
            return FTS_NONE;
        }

        return SimpleValueFactory.getInstance().createIRI(kb.getFullTextSearchIri());
    }

    /**
     * When working with queries using any of {@link #roots()}, @link
     * #withIdentifier(String)}, @link #childrenOf(String)}, {@link #descendantsOf(String)},
     * {@link #parentsOf(String)} or {@link #ancestorsOf(String)}}, the FTS must be disabled. These
     * methods must be called <b>before</b> any label-restricting methods like
     * {@link #withLabelStartingWith(String)} This is because the FTS part of the query pre-filters
     * the potential candidates, but the FTS may not return all candidates. Let's consider a large
     * KB (e.g. Wikidata) and a query for <i>all humans named Amanda in the Star Trek universe</i>
     * (there is a category for <i>humans in the Star Trek universe</i> in Wikidata). First the FTS
     * would try to retrieve all entities named <i>Amanda</i>, but it does not really return all,
     * just the top 50 (which is what Wikidata seems to be hard-coded to despite the documentation
     * for <i>wikidata:limit</i> saying otherwise). None of these Amandas, however, is part of the
     * Star Trek universe, so the final result of the query is empty. Here, the FTS restricts too
     * much and too early. For such cases, we should rely on the scope sufficiently limiting the
     * returned results such that the regex-based filtering does not get too slow.
     * 
     * @returns {@code true} if the FTS does not return all hits but only a e.g. the first 50 or 100
     *          matches.
     */
    private boolean isFtsLimited()
    {
        if (kb.getFullTextSearchIri() == null) {
            return false;
        }

        IRI ftsMode = SimpleValueFactory.getInstance().createIRI(kb.getFullTextSearchIri());
        return FTS_WIKIDATA.equals(ftsMode);
    }

    @Override
    public SPARQLQueryBuilder withLabelMatchingAnyOf(String... aValues)
    {
        var values = Arrays.stream(aValues) //
                .map(SPARQLQueryBuilder::trimQueryString) //
                .filter(StringUtils::isNotBlank) //
                .toArray(String[]::new);

        if (values.length == 0) {
            noResult();
            return this;
        }

        getAdapter().withLabelMatchingAnyOf(aValues);

        addMatchTermProjections(projections);
        labelImplicitlyRetrieved = true;

        return this;
    }

    @Override
    public SPARQLQueryBuilder withLabelContainingAnyOf(String... aValues)
    {
        var values = Arrays.stream(aValues) //
                .map(SPARQLQueryBuilder::trimQueryString) //
                .filter(StringUtils::isNotBlank) //
                .toArray(String[]::new);

        if (values.length == 0) {
            noResult();
            return this;
        }

        getAdapter().withLabelContainingAnyOf(values);

        addMatchTermProjections(projections);
        labelImplicitlyRetrieved = true;

        return this;
    }

    @Override
    public SPARQLQueryBuilder withLabelStartingWith(String aPrefixQuery)
    {
        var value = trimQueryString(aPrefixQuery);

        if (value == null || value.length() == 0) {
            noResult();
            return this;
        }

        getAdapter().withLabelStartingWith(value);

        addMatchTermProjections(projections);
        labelImplicitlyRetrieved = true;

        return this;
    }

    Expression<?> startsWithPattern(Variable aVariable, String aPrefixQuery)
    {
        return matchString(STRSTARTS, aVariable, aPrefixQuery);
    }

    Expression<?> containsPattern(Variable aVariable, String aSubstring)
    {
        return matchString(CONTAINS, aVariable, aSubstring);
    }

    private static String asRegexp(String aValue)
    {
        var value = aValue;
        // Escape metacharacters
        // value = value.replaceAll("[{}()\\[\\].+*?^$\\\\|]", "\\\\\\\\$0");
        // Replace metacharacters with a match for any single char (.+ would be too slow)
        value = value.replaceAll("[{}()\\[\\].+*?^$\\\\|]+", ".");
        // Drop metacharacters
        // value = value.replaceAll("[{}()\\[\\].+*?^$\\\\|]+", " ");
        // Replace consecutive whitespace or control chars with a whitespace matcher
        value = value.replaceAll("[\\p{Space}\\p{Cntrl}]+", "\\\\s+");
        return value;
    }

    Expression<?> equalsPattern(Variable aVariable, String aValue, KnowledgeBase aKB)
    {
        var variable = aVariable;

        var regexFlags = "";
        if (caseInsensitive) {
            regexFlags += "i";
        }

        // Match using REGEX to be resilient against extra whitespace
        // Match exactly
        var value = "^" + asRegexp(aValue) + "$";

        var expressions = new ArrayList<Expression<?>>();
        expressions.add(
                function(REGEX, function(STR, variable), literalOf(value), literalOf(regexFlags)));
        expressions.add(matchKbLanguage(aVariable));

        return and(expressions.toArray(Expression[]::new));
    }

    private Expression<?> matchString(SparqlFunction aFunction, Variable aVariable, String aValue)
    {
        var expressions = new ArrayList<Expression<?>>();

        if (filterUsingRegex) {
            var regexFlags = "";
            if (caseInsensitive) {
                regexFlags += "i";
            }

            String value;
            switch (aFunction) {
            // Match using REGEX to be resilient against extra whitespace
            case STRSTARTS:
                // Match at start
                value = "^" + asRegexp(aValue) + ".*";
                break;
            case CONTAINS:
                // Match anywhere
                value = Stream.of(TOKENKIZER_PATTERN.split(aValue)) //
                        .map(t -> "(?=.*" + asRegexp(t) + ")") //
                        .collect(joining());
                break;
            default:
                throw new IllegalArgumentException(
                        "Only STRSTARTS and CONTAINS are supported, but got [" + aFunction + "]");
            }

            expressions.add(function(REGEX, function(STR, aVariable), literalOf(value),
                    literalOf(regexFlags)));
        }

        expressions.add(matchKbLanguage(aVariable));

        return and(expressions.toArray(Expression[]::new)).parenthesize();
    }

    Expression<?> matchKbLanguage(Variable aVariable)
    {
        var defaultLang = kb.getDefaultLanguage();

        var languages = new ArrayList<Expression<?>>();
        if (defaultLang != null) {
            // Match with default language
            languages.add(function(LANGMATCHES, function(LANG, aVariable), literalOf(defaultLang)));
        }

        for (var lang : fallbackLanguages) {
            languages.add(function(LANGMATCHES, function(LANG, aVariable), literalOf(lang)));
        }

        // Match without language
        languages.add(Expressions.equals(function(LANG, aVariable), EMPTY_STRING));

        return or(languages.toArray(Expression[]::new));
    }

    @Override
    public SPARQLQueryPrimaryConditions roots()
    {
        forceDisableFTS.add("roots query");

        addPattern(PRIMARY, mode.rootsPattern(kb));

        return this;
    }

    @Override
    public SPARQLQueryPrimaryConditions ancestorsOf(String aItemIri)
    {
        forceDisableFTS.add("ancestorOf query");

        Iri contextIri = iri(aItemIri);

        addPattern(PRIMARY, mode.ancestorsPattern(kb, contextIri));

        return this;
    }

    @Override
    public SPARQLQueryPrimaryConditions descendantsOf(String aClassIri)
    {
        forceDisableFTS.add("descendantsOf query");

        var contextIri = iri(aClassIri);

        addPattern(PRIMARY, mode.descendentsPattern(kb, contextIri));

        return this;
    }

    @Override
    public SPARQLQueryPrimaryConditions childrenOf(String aClassIri)
    {
        forceDisableFTS.add("childrenOf query");

        var contextIri = iri(aClassIri);

        addPattern(PRIMARY, mode.childrenPattern(kb, contextIri));

        return this;
    }

    @Override
    public SPARQLQueryPrimaryConditions parentsOf(String aClassIri)
    {
        forceDisableFTS.add("parentsOf query");

        var contextIri = iri(aClassIri);

        addPattern(PRIMARY, mode.parentsPattern(kb, contextIri));

        return this;
    }

    private void limitToClasses()
    {
        var classIri = iri(kb.getClassIri());
        var subClassProperty = iri(kb.getSubclassIri());
        var typeOfProperty = iri(kb.getTypeIri());

        var classPatterns = new ArrayList<GraphPattern>();

        // An item is a class if ...
        // ... it is explicitly defined as being a class
        classPatterns.add(VAR_SUBJECT.has(typeOfProperty, classIri));
        // ... it has any subclass
        classPatterns.add(bNode().has(subClassProperty, VAR_SUBJECT));
        // ... it has any superclass
        classPatterns.add(VAR_SUBJECT.has(subClassProperty, bNode()));
        // ... it is used as the type of some instance
        // This can be a very slow condition - so we have to skip it
        // classPatterns.add(bNode().has(typeOfProperty, VAR_SUBJECT));
        // ... it participates in an owl:intersectionOf
        if (OWL.CLASS.stringValue().equals(kb.getClassIri())) {
            classPatterns.add(VAR_SUBJECT.has(OWL_INTERSECTIONOF_PATH, bNode()));
        }

        addPattern(PRIMARY_RESTRICTIONS,
                filterExists(union(classPatterns.stream().toArray(GraphPattern[]::new))));
    }

    private void limitToInstances()
    {
        var classIri = iri(kb.getClassIri());
        var subClassProperty = iri(kb.getSubclassIri());
        var typeOfProperty = iri(kb.getTypeIri());

        // An item is an instance if ... (make sure to add the LiftableExistsFilter
        // directly to the PRIMARY_RESTRICTIONS and not nested in another pattern
        // so it can be discovered by selectQuery() and be lifted if necessary.
        addPattern(PRIMARY_RESTRICTIONS,
                new PromotableExistsFilter(VAR_SUBJECT.has(typeOfProperty, bNode())));
        // ... it is not explicitly defined as being a class
        addPattern(PRIMARY_RESTRICTIONS,
                filterNotExists(VAR_SUBJECT.has(typeOfProperty, classIri)));
        // ... it does not have any subclass
        addPattern(PRIMARY_RESTRICTIONS,
                filterNotExists(bNode().has(subClassProperty, VAR_SUBJECT)));
        // ... it does not have any superclass
        addPattern(PRIMARY_RESTRICTIONS,
                filterNotExists(VAR_SUBJECT.has(subClassProperty, bNode())));
    }

    private void limitToProperties()
    {
        addPattern(PRIMARY_RESTRICTIONS, filterExists(isPropertyPattern()));
    }

    private GraphPattern isPropertyPattern()
    {
        var propertyIri = iri(kb.getPropertyTypeIri());
        var subPropertyProperty = iri(kb.getSubPropertyIri());
        var typeOfProperty = iri(kb.getTypeIri());
        var pSubClass = iri(kb.getSubclassIri());

        var propertyPatterns = new ArrayList<GraphPattern>();

        // An item is a property if ...
        // ... it is explicitly defined as being a property
        propertyPatterns.add(VAR_SUBJECT.has(typeOfProperty, propertyIri));
        propertyPatterns.add(VAR_SUBJECT.has(PropertyPathBuilder.of(pSubClass).oneOrMore().build(),
                propertyIri));
        // propertyPatterns.add(VAR_SUBJECT.has(typeOfProperty, propertyIri));
        // ... it has any subproperties
        propertyPatterns.add(bNode().has(subPropertyProperty, VAR_SUBJECT));
        // ... it has any superproperties
        propertyPatterns.add(VAR_SUBJECT.has(subPropertyProperty, bNode()));

        // This may be a bit too general... e.g. it takes forever to complete on YAGO
        //// ... or it essentially appears in the predicate position :)
        // propertyPatterns.add(bNode().has(VAR_SUBJECT, bNode()));

        return union(propertyPatterns.stream().toArray(GraphPattern[]::new));
    }

    @Override
    public SPARQLQueryOptionalElements retrieveLabel()
    {
        if (!mode.getAdditionalMatchingProperties(kb).isEmpty()) {
            addPreferredLabelProjections(projections);
            addPattern(SECONDARY, bindPrefLabelProperties(VAR_PREF_LABEL_PROPERTY));
            retrieveOptionalWithLanguage(VAR_PREF_LABEL_PROPERTY, VAR_PREF_LABEL);
        }

        // If the label is already retrieved, do nothing
        if (labelImplicitlyRetrieved) {
            return this;
        }

        addMatchTermProjections(projections);
        addPattern(SECONDARY, bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY));
        retrieveOptionalWithLanguage(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM);

        return this;
    }

    @Override
    public SPARQLQueryOptionalElements retrieveDescription()
    {
        // Retain only the first description
        projections.add(getDescriptionProjection());

        var descProperty = mode.getDescriptionProperty(kb);
        retrieveOptionalWithLanguage(descProperty, VAR_DESC_CANDIDATE);

        return this;
    }

    @Override
    public SPARQLQueryOptionalElements retrieveDeprecation()
    {
        // Retain only the first deprecation statement
        projections.add(getDeprecationProjection());

        var deprecationProperty = mode.getDeprecationProperty(kb);
        retrieveOptionalWithLanguage(deprecationProperty, VAR_DEPRECATION);

        return this;
    }

    private void retrieveOptionalWithLanguage(RdfPredicate aProperty, Variable aVariable)
    {
        var pattern = VAR_SUBJECT.has(aProperty, aVariable) //
                .filter(matchKbLanguage(aVariable));

        // Virtuoso has trouble with multiple OPTIONAL clauses causing results which would
        // normally match to be removed from the results set. Using a UNION seems to address this
        // labelPatterns.forEach(pattern -> addPattern(Priority.SECONDARY, optional(pattern)));
        addPattern(SECONDARY, optional(union(pattern)));
    }

    @Override
    public SPARQLQueryOptionalElements retrieveDomainAndRange()
    {
        projections.add(VAR_RANGE);
        projections.add(VAR_DOMAIN);

        addPattern(SECONDARY, optional(VAR_SUBJECT.has(RDFS.RANGE, VAR_RANGE)));
        addPattern(SECONDARY, optional(VAR_SUBJECT.has(RDFS.DOMAIN, VAR_DOMAIN)));

        return this;
    }

    int getLimit()
    {
        return limitOverride > 0 ? limitOverride : kb.getMaxResults();
    }

    SelectQuery selectQuery()
    {
        // Must add it anyway because we group by it
        projections.add(VAR_SUBJECT);

        var query = Queries.SELECT().distinct();
        prefixes.forEach(query::prefix);
        projections.forEach(query::select);

        // Some KBs do not like queries consisting only of FILTERs, so if we have a filter
        // (in particular a FILTER EXISTS), we can convert that a proper pattern by removing
        // the FILTER EXISTS from it. We only do this if there are no other primary patterns
        // because the FILTER EXISTS pattern would usually be one that could be expensive and
        // if we already have another primary pattern, that is hopefully way cheaper.
        var promotableRestriction = primaryRestrictions.stream().findFirst()
                .filter(pattern -> pattern instanceof PromotableExistsFilter)
                .map(pattern -> (PromotableExistsFilter) pattern);
        if (primaryPatterns.isEmpty() && promotableRestriction.isPresent()) {
            primaryPatterns.add(promotableRestriction.get().getNested());
            primaryRestrictions.remove(promotableRestriction.get());
        }

        // First add the primary patterns and high-level restrictions (e.g. limits to classes or
        // instances) - this is important because Virtuoso has trouble when combining UNIONS,
        // property paths FILTERS and OPTIONALS (which we do a lot). It seems to help when we put
        // the FILTERS together with the primary part of the query into a group.
        // See: https://github.com/openlink/virtuoso-opensource/issues/831
        query.where(() -> SparqlBuilderUtils
                .getBracedString(and(concat(primaryPatterns.stream(), primaryRestrictions.stream())
                        .toArray(GraphPattern[]::new)).getQueryString()));

        // Then add the optional or lower-prio elements
        secondaryPatterns.stream().forEach(query::where);

        if (kb.getDefaultDatasetIri() != null) {
            query.from(dataset(from(iri(kb.getDefaultDatasetIri()))));
        }

        int actualLimit = getLimit();

        // If we do not do a server-side reduce, then we may get two results for every item
        // from the server (one with and one without the language), so we need to double the
        // query limit and cut down results locally later.
        actualLimit = actualLimit * 2;

        query.limit(actualLimit);

        return query;
    }

    @Override
    public List<KBHandle> asHandles(RepositoryConnection aConnection, boolean aAll)
    {
        var startTime = currentTimeMillis();
        var queryId = toHexString(hashCode());

        var queryString = selectQuery().getQueryString();
        LOG.trace("[{}] Query: {}", queryId, queryString);

        if (returnEmptyResult) {
            LOG.debug("[{}] Query was skipped because it would not return any results anyway",
                    queryId);

            return emptyList();
        }

        try {
            var tupleQuery = aConnection.prepareTupleQuery(queryString);
            tupleQuery.setIncludeInferred(includeInferred);
            var results = evaluateListQuery(tupleQuery, aAll);
            results.sort(comparing(KBObject::getUiLabel, CASE_INSENSITIVE_ORDER));

            var duration = currentTimeMillis() - startTime;
            LOG.debug("[{}] Query returned {} results in {}ms {}", queryId, results.size(),
                    duration, duration > 1000 ? "-- SLOW QUERY!" : "");

            if (duration > 1000 && !LOG.isTraceEnabled()) {
                LOG.debug("[{}] Slow query: {}", queryId, queryString);
            }

            return results;
        }
        catch (QueryEvaluationException e) {
            throw new QueryEvaluationException(
                    e.getMessage() + " while running query:\n" + queryString, e);
        }
    }

    /**
     * Execute the query and return {@code true} if the result set is not empty. This internally
     * limits the number of results requested via SPARQL to 1 and should complete faster than
     * retrieving the entire results set and checking whether it is empty.
     * 
     * @param aConnection
     *            a connection to a triple store.
     * @param aAll
     *            True if entities with implicit namespaces (e.g. defined by RDF)
     * @return {@code true} if the result set is not empty.
     */
    @Override
    public boolean exists(RepositoryConnection aConnection, boolean aAll)
    {
        var startTime = currentTimeMillis();
        var queryId = toHexString(hashCode());

        limit(1);

        var query = selectQuery();

        var queryString = query.getQueryString();
        LOG.trace("[{}] Query: {}", queryId, queryString);

        if (returnEmptyResult) {
            LOG.debug("[{}] Query was skipped because it would not return any results anyway",
                    queryId);

            return false;
        }

        try {
            var tupleQuery = aConnection.prepareTupleQuery(queryString);
            var result = !evaluateListQuery(tupleQuery, aAll).isEmpty();

            var duration = currentTimeMillis() - startTime;
            LOG.debug("[{}] Query returned {} in {}ms {}", queryId, result, duration,
                    duration > 1000 ? "-- SLOW QUERY!" : "");

            if (duration > 1000 && !LOG.isTraceEnabled()) {
                LOG.debug("[{}] Slow query: {}", queryId, queryString);
            }

            return result;
        }
        catch (QueryEvaluationException e) {
            throw new QueryEvaluationException(
                    e.getMessage() + " while running query:\n" + queryString, e);
        }
    }

    @Override
    public Optional<KBHandle> asHandle(RepositoryConnection aConnection, boolean aAll)
    {
        var startTime = currentTimeMillis();
        var queryId = toHexString(hashCode());

        limit(1);

        var queryString = selectQuery().getQueryString();
        LOG.trace("[{}] Query: {}", queryId, queryString);

        if (returnEmptyResult) {
            LOG.debug("[{}] Query was skipped because it would not return any results anyway",
                    queryId);
            return Optional.empty();
        }

        try {
            var tupleQuery = aConnection.prepareTupleQuery(queryString);
            tupleQuery.setIncludeInferred(includeInferred);
            var result = evaluateListQuery(tupleQuery, aAll).stream().findFirst();

            var duration = currentTimeMillis() - startTime;
            LOG.debug("[{}] Query returned a result in {}ms {}", queryId, duration,
                    duration > 1000 ? "-- SLOW QUERY!" : "");

            if (duration > 1000 && !LOG.isTraceEnabled()) {
                LOG.debug("[{}] Slow query: {}", queryId, queryString);
            }

            return result;
        }
        catch (QueryEvaluationException e) {
            throw new QueryEvaluationException(
                    e.getMessage() + " while running query:\n" + queryString, e);
        }
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
        try (var result = tupleQuery.evaluate()) {
            var handles = new ArrayList<KBHandle>();
            while (result.hasNext()) {
                var bindings = result.next();
                if (bindings.size() == 0) {
                    continue;
                }

                // LOG.trace("[{}] Bindings: {}", toHexString(hashCode()), bindings);

                var id = bindings.getBinding(VAR_SUBJECT_NAME).getValue().stringValue();
                if (!id.contains(":") || (!aAll && hasImplicitNamespace(kb, id))) {
                    continue;
                }

                var handle = new KBHandle(id);
                handle.setKB(kb);

                extractLabel(handle, bindings);
                extractDescription(handle, bindings);
                extractRange(handle, bindings);
                extractDomain(handle, bindings);
                extractScore(handle, bindings);
                extractDeprecation(handle, bindings);

                handles.add(handle);
            }

            return reduceRedundantResults(handles);
        }
    }

    /**
     * Make sure that each result is only represented once, preferably in the default language.
     */
    private List<KBHandle> reduceRedundantResults(List<KBHandle> aHandles)
    {
        var langPrio = new ArrayList<>();
        if (kb.getDefaultLanguage() != null) {
            langPrio.add(kb.getDefaultLanguage());
        }
        langPrio.addAll(fallbackLanguages);
        langPrio.add(null);

        var cMap = new LinkedHashMap<String, KBHandle>();
        for (var handle : aHandles) {
            var current = cMap.get(handle.getIdentifier());

            // Not recorded yet -> add it
            if (current == null) {
                cMap.put(handle.getIdentifier(), handle);
                continue;
            }

            boolean replace = false;
            // Found one with a label while current one doesn't have one
            if (current.getName() == null && handle.getName() != null) {
                replace = true;
            }
            // Found an exact language match -> use that one instead
            // Note that having a language implies that there is a label!
            else if (langPrio.indexOf(current.getLanguage()) > langPrio
                    .indexOf(handle.getLanguage())) {
                replace = true;
            }

            if (replace) {
                cMap.put(handle.getIdentifier(), handle);
                current.getMatchTerms().forEach(e -> handle.addMatchTerm(e.getKey(), e.getValue()));
            }
            else {
                handle.getMatchTerms().forEach(e -> current.addMatchTerm(e.getKey(), e.getValue()));
            }
        }

        LOG.trace("Input: {}", aHandles);
        LOG.trace("Output: {}", cMap.values());

        return cMap.values().stream().limit(getLimit()).collect(Collectors.toList());
    }

    private void extractLabel(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        var prefLabel = aSourceBindings.getBinding(VAR_PREF_LABEL_NAME);
        var matchTerm = aSourceBindings.getBinding(VAR_MATCH_TERM_NAME);

        // Obtain name either from the pref-label or from the match term if available
        if (prefLabel != null) {
            aTargetHandle.setName(prefLabel.getValue().stringValue());
            extractLanguage(prefLabel).ifPresent(aTargetHandle::setLanguage);
        }
        else if (matchTerm != null) {
            aTargetHandle.setName(matchTerm.getValue().stringValue());
            extractLanguage(matchTerm).ifPresent(aTargetHandle::setLanguage);
        }

        // If we have additional search properties, we need to store the label candidates
        // in the handle as well
        if (!mode.getAdditionalMatchingProperties(kb).isEmpty() && matchTerm != null) {
            var language = extractLanguage(matchTerm).orElse(null);
            aTargetHandle.addMatchTerm(matchTerm.getValue().stringValue(), language);
        }

        // Binding subPropertyLabel = aSourceBindings.getBinding("spl");
        // if (subPropertyLabel != null) {
        // aTargetHandle.setName(subPropertyLabel.getValue().stringValue());
        // if (subPropertyLabel.getValue() instanceof Literal) {
        // Literal literal = (Literal) subPropertyLabel.getValue();
        // literal.getLanguage().ifPresent(aTargetHandle::setLanguage);
        // }
        // }
    }

    private Optional<String> extractLanguage(Binding aBinding)
    {
        if (aBinding != null && aBinding.getValue() instanceof Literal literal) {
            return literal.getLanguage();
        }

        return Optional.empty();
    }

    private void extractDescription(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        var description = aSourceBindings.getBinding(VAR_DESCRIPTION_NAME);
        var descCandidate = aSourceBindings.getBinding(VAR_DESCRIPTION_CANDIDATE_NAME);
        if (description != null) {
            aTargetHandle.setDescription(description.getValue().stringValue());
        }
        else if (descCandidate != null) {
            aTargetHandle.setDescription(descCandidate.getValue().stringValue());
        }
    }

    private void extractDomain(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        var domain = aSourceBindings.getBinding(VAR_DOMAIN_NAME);
        if (domain != null) {
            aTargetHandle.setDomain(domain.getValue().stringValue());
        }
    }

    private void extractRange(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        var range = aSourceBindings.getBinding(VAR_RANGE_NAME);
        if (range != null) {
            aTargetHandle.setRange(range.getValue().stringValue());
        }
    }

    private void extractScore(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        var score = aSourceBindings.getBinding(VAR_SCORE_NAME);
        if (score != null) {
            aTargetHandle.setScore(Double.valueOf(score.getValue().stringValue()));
        }
    }

    private void extractDeprecation(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        var deprecation = aSourceBindings.getBinding(VAR_DEPRECATION_NAME);
        if (deprecation != null) {
            if (deprecation.getValue() instanceof Literal literal) {
                try {
                    aTargetHandle.setDeprecated(literal.booleanValue());
                }
                catch (IllegalArgumentException e) {
                    // Anything other than a falsy value is considered to be true
                    aTargetHandle.setDeprecated(true);
                }
            }
            else {
                aTargetHandle.setDeprecated(true);
            }
        }
    }

    /**
     * Removes leading and trailing space and single quote characters which could cause the query
     * string to escape its quotes in the SPARQL query.
     * 
     * @param aQuery
     *            a query string
     * @return the stripped query string
     */
    static String trimQueryString(String aQuery)
    {
        if (aQuery == null || aQuery.length() == 0) {
            return aQuery;
        }

        boolean trailingSpace = isWhitespace(aQuery.charAt(aQuery.length() - 1));

        int end = aQuery.length();
        while (end > 0 && (isWhitespace(aQuery.charAt(end - 1)) || aQuery.charAt(end - 1) == '\''
                || aQuery.charAt(end - 1) == '"')) {
            end--;
        }

        int begin = 0;
        while (begin < aQuery.length() && (isWhitespace(aQuery.charAt(begin))
                || aQuery.charAt(begin) == '\'' || aQuery.charAt(begin) == '"')) {
            begin++;
        }

        if (begin >= end) {
            return "";
        }

        if (begin > 0 || end < aQuery.length()) {
            if (trailingSpace) {
                return aQuery.substring(begin, end) + ' ';
            }
            else {
                return aQuery.substring(begin, end);
            }
        }

        return aQuery;
    }

    public String sanitizeQueryString_FTS(String aQuery)
    {
        var sanitizedValue = aQuery
                // character classes to replace with a simple space
                .replaceAll("[\\p{Punct}\\p{Space}\\p{Cntrl}[~+*(){}\\[\\]]]+", " ")
                // character classes to remove from the query string
                // \u00AD : SOFT HYPHEN
                .replaceAll("[\\u00AD]", "") //
                .trim();

        // We assume that the FTS is case insensitive and found that some FTSes (i.e.
        // Fuseki) can have trouble matching if they get upper-case query when they
        // internally lower-cased
        if (isCaseInsensitive()) {
            sanitizedValue = toLowerCase(getKnowledgeBase(), sanitizedValue);
        }

        return sanitizedValue.trim();
    }

    public static String convertToFuzzyMatchingQuery(String aQuery, String aOperator)
    {
        var joiner = new StringJoiner(" ");
        var terms = aQuery.split("\\s");
        for (var term : terms) {
            // We only do the fuzzy search if there are few terms because if there are many terms,
            // the search becomes too slow if we do a fuzzy match for each of them.
            if (term.length() > 4 && terms.length <= 3) {
                joiner.add(term + aOperator);
            }
            // REC: excluding terms of 3 or less characters helps reducing the problem that a
            // mention of "Counties of Catherlagh" matches "Anne of Austria", but actually
            // I think this should be handled by stopwords and not be excluding any short words...
            else if (term.length() >= 3) {
                joiner.add(term);
            }
            else {
                // Terms shorter than 3 are ignored
            }
        }

        return joiner.toString();
    }

    public static String convertToRequiredTokenPrefixMatchingQuery(String aQuery,
            String aPrefixOperator, String aSuffixOperator)
    {
        var terms = aQuery.split("\\s");
        var joiner = new StringJoiner(" ");
        for (var term : terms) {
            if (term.length() >= 3) {
                joiner.add(aPrefixOperator + term + aSuffixOperator);
            }
            else {
                // Terms shorter than 3 are ignored
            }
        }

        return joiner.toString();
    }

    public static String toLowerCase(KnowledgeBase kb, String aValue)
    {
        if (aValue == null) {
            return null;
        }

        var locale = Locale.ROOT;
        if (kb != null && kb.getDefaultLanguage() != null) {
            try {
                locale = Locale.forLanguageTag(kb.getDefaultLanguage());
            }
            catch (IllformedLocaleException e) {
                // Ignore
            }
        }

        return aValue.toLowerCase(locale);
    }

    @Override
    public Set<String> resolvePrefLabelProperties(RepositoryConnection aConnection)
    {
        var pLabel = mode.getLabelProperty(kb);
        var properties = new LinkedHashSet<String>();

        resolveSubProperties(aConnection, pLabel, properties);

        return properties;
    }

    @Override
    public Set<String> resolveAdditionalMatchingProperties(RepositoryConnection aConnection)
    {
        var properties = new LinkedHashSet<String>();

        for (var pAddSearch : mode.getAdditionalMatchingProperties(kb)) {
            resolveSubProperties(aConnection, pAddSearch, properties);
        }

        return properties;
    }

    private void resolveSubProperties(RepositoryConnection aConnection, Iri aRootProperty,
            Set<String> properties)
    {
        var varProperty = var("property");
        var pSubProperty = iri(kb.getSubPropertyIri());
        var pattern = varProperty.has(PropertyPathBuilder.of(pSubProperty).zeroOrMore().build(),
                aRootProperty);

        var query = Queries.SELECT().distinct().where(pattern);

        var tupleQuery = aConnection.prepareTupleQuery(query.getQueryString());
        tupleQuery.setIncludeInferred(includeInferred);

        try (var result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                var bindings = result.next();
                if (bindings.isEmpty()) {
                    continue;
                }

                var property = bindings.getBinding(varProperty.getVarName());
                if (property != null) {
                    properties.add(property.getValue().stringValue());
                }
            }
        }
        catch (QueryEvaluationException e) {
            throw new QueryEvaluationException(
                    e.getMessage() + " while running query:\n" + query.getQueryString(), e);
        }
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof SPARQLQueryBuilder)) {
            return false;
        }

        var castOther = (SPARQLQueryBuilder) other;
        var query = selectQuery().getQueryString();
        var otherQuery = castOther.selectQuery().getQueryString();
        return query.equals(otherQuery);
    }

    @Override
    public int hashCode()
    {
        return selectQuery().getQueryString().hashCode();
    }

    @Override
    public void logQueryString(Logger aLog, Level aLevel, String aPrefix)
    {
        Arrays.stream(selectQuery().getQueryString().split("\n"))
                .forEachOrdered(l -> aLog.atLevel(aLevel).log("{}{}", aPrefix, l));
    }

    private static class PromotableExistsFilter
        implements GraphPattern
    {

        private final GraphPattern nested;

        public PromotableExistsFilter(GraphPattern aNested)
        {
            nested = aNested;
        }

        public GraphPattern getNested()
        {
            return nested;
        }

        @Override
        public String getQueryString()
        {
            return "FILTER EXISTS { " + nested.getQueryString() + " } ";
        }
    }
}
