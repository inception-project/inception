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

import static com.github.jsonldjava.shaded.com.google.common.collect.Streams.concat;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_FUSEKI;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_LUCENE;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_NONE;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_VIRTUOSO;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_WIKIDATA;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.hasImplicitNamespace;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.Path.oneOrMore;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.Path.zeroOrMore;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.RdfCollection.collectionOf;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Priority.PRIMARY;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Priority.PRIMARY_RESTRICTIONS;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Priority.SECONDARY;
import static java.lang.Integer.toHexString;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.and;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.function;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.notEquals;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.or;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.CONTAINS;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.LANG;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.LANGMATCHES;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.REGEX;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.STRSTARTS;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.filterExists;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.filterNotExists;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.optional;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.union;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.bNode;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOfLanguage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
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
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode.LabeledBlankNode;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfValue;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

/**
 * Build queries against the KB.
 * 
 * <p>
 * <b>Handling of subclasses: </b>
 * Queries for subclasses return only resources which declare being a class via the class property
 * defined in the KB specification. This means that if the KB is configured to use rdfs:Class but a
 * subclass defines itself using owl:Class, then this subclass is *not* returned. We do presently
 * *not* support mixed schemes in a single KB.
 * </p>
 */
public class SPARQLQueryBuilder
    implements SPARQLQuery, SPARQLQueryPrimaryConditions, SPARQLQueryOptionalElements
{
    private final static Logger LOG = LoggerFactory.getLogger(SPARQLQueryBuilder.class);

    public static final int DEFAULT_LIMIT = 0;
    
    public static final String VAR_SUBJECT_NAME = "subj";
    public static final String VAR_PREDICATE_NAME = "pred";
    public static final String VAR_OBJECT_NAME = "obj";
    public static final String VAR_LABEL_PROPERTY_NAME = "pLabel";
    public static final String VAR_LABEL_NAME = "l";
    public static final String VAR_LABEL_CANDIDATE_NAME = "lc";
    public static final String VAR_DESCRIPTION_NAME = "d";
    public static final String VAR_DESCRIPTION_CANDIDATE_NAME = "dc";
    public static final String VAR_RANGE_NAME = "range";
    public static final String VAR_DOMAIN_NAME = "domain";
    
    public static final Variable VAR_SUBJECT = var(VAR_SUBJECT_NAME);
    public static final Variable VAR_PREDICATE = var(VAR_PREDICATE_NAME);
    public static final Variable VAR_OBJECT = var(VAR_OBJECT_NAME);
    public static final Variable VAR_RANGE = var(VAR_RANGE_NAME);
    public static final Variable VAR_DOMAIN = var(VAR_DOMAIN_NAME);
    public static final Variable VAR_LABEL = var(VAR_LABEL_NAME);
    public static final Variable VAR_LABEL_CANDIDATE = var(VAR_LABEL_CANDIDATE_NAME);
    public static final Variable VAR_LABEL_PROPERTY = var(VAR_LABEL_PROPERTY_NAME);
    public static final Variable VAR_DESCRIPTION = var(VAR_DESCRIPTION_NAME);
    public static final Variable VAR_DESC_CANDIDATE = var(VAR_DESCRIPTION_CANDIDATE_NAME);

    public static final Prefix PREFIX_LUCENE_SEARCH = prefix("search",
            iri("http://www.openrdf.org/contrib/lucenesail#"));
    public static final Iri LUCENE_QUERY = PREFIX_LUCENE_SEARCH.iri("query");
    public static final Iri LUCENE_PROPERTY = PREFIX_LUCENE_SEARCH.iri("property");
    public static final Iri LUCENE_SCORE = PREFIX_LUCENE_SEARCH.iri("score");
    public static final Iri LUCENE_SNIPPET = PREFIX_LUCENE_SEARCH.iri("snippet");

    public static final Prefix PREFIX_FUSEKI_SEARCH = prefix("text",
            iri("http://jena.apache.org/text#"));
    public static final Iri FUSEKI_QUERY = PREFIX_FUSEKI_SEARCH.iri("query");

    public static final Iri OWL_INTERSECTIONOF = iri(OWL.INTERSECTIONOF.stringValue());
    public static final Iri RDF_REST = iri(RDF.REST.stringValue());
    public static final Iri RDF_FIRST = iri(RDF.FIRST.stringValue());
    
    private static final RdfValue EMPTY_STRING = () -> "\"\"";
    
    private final Set<Prefix> prefixes = new LinkedHashSet<>();
    private final Set<Projectable> projections = new LinkedHashSet<>();
    private final List<GraphPattern> primaryPatterns = new ArrayList<>();
    private final List<GraphPattern> primaryRestrictions = new ArrayList<>();
    private final List<GraphPattern> secondaryPatterns = new ArrayList<>();
    
    private boolean labelImplicitlyRetrieved = false;
    
    enum Priority {
        PRIMARY, PRIMARY_RESTRICTIONS, SECONDARY
    }
    
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
    
    private int limitOverride = DEFAULT_LIMIT;
    
    private boolean includeInferred = true;
    
    private boolean forceDisableFTS = false;
    
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
    // This is presently disabled because we cannot guarantee that the MIN operation
    // prefers the label with the language as opposed to "any label".
    private boolean serverSideReduce = false;
    
    private static enum Mode {
        ITEM, CLASS, INSTANCE, PROPERTY;
        
        protected Iri getLabelProperty(KnowledgeBase aKb) {
            switch (this) {
            case ITEM:
                return iri(aKb.getLabelIri());
            case CLASS:
                return iri(aKb.getLabelIri());
            case INSTANCE:
                return iri(aKb.getLabelIri());
            case PROPERTY:
                return iri(aKb.getPropertyLabelIri());
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }
        }

        protected Iri getDescriptionProperty(KnowledgeBase aKb) {
            switch (this) {
            case ITEM:
                return iri(aKb.getDescriptionIri());
            case CLASS:
                return iri(aKb.getDescriptionIri());
            case INSTANCE:
                return iri(aKb.getDescriptionIri());
            case PROPERTY:
                return iri(aKb.getPropertyDescriptionIri());
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }
        }
        
        /**
         * @see SPARQLQueryPrimaryConditions#descendantsOf(String)
         */
        protected GraphPattern descendentsPattern(KnowledgeBase aKB, Iri aContext)
        {
            Iri typeOfProperty = iri(aKB.getTypeIri());
            Iri subClassProperty = iri(aKB.getSubclassIri());
            Iri subPropertyProperty = iri(aKB.getSubPropertyIri());
                        
            switch (this) {
            case ITEM: {
                List<GraphPattern> classPatterns = new ArrayList<>();
                classPatterns.add(VAR_SUBJECT.has(Path.of(oneOrMore(subClassProperty)), aContext));
                classPatterns.add(VAR_SUBJECT
                        .has(Path.of(typeOfProperty, zeroOrMore(subClassProperty)), aContext));
                if (OWL.CLASS.equals(aKB.getClassIri())) {
                    classPatterns.add(VAR_SUBJECT.has(
                            Path.of(OWL_INTERSECTIONOF, zeroOrMore(RDF_REST), RDF_FIRST),
                            aContext));
                }
                
                return GraphPatterns.union(classPatterns.stream().toArray(GraphPattern[]::new));
            }
            case CLASS: {
                List<GraphPattern> classPatterns = new ArrayList<>();
                classPatterns.add(VAR_SUBJECT.has(Path.of(oneOrMore(subClassProperty)), aContext));
                if (OWL.CLASS.equals(aKB.getClassIri())) {
                    classPatterns.add(VAR_SUBJECT.has(
                            Path.of(OWL_INTERSECTIONOF, zeroOrMore(RDF_REST), RDF_FIRST),
                            aContext));
                }

                return GraphPatterns.union(classPatterns.stream().toArray(GraphPattern[]::new));
            }
            case INSTANCE:
                return VAR_SUBJECT.has(Path.of(typeOfProperty, zeroOrMore(subClassProperty)),
                        aContext);
            case PROPERTY:
                return VAR_SUBJECT.has(Path.of(oneOrMore(subPropertyProperty)), aContext);
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }            
        }

        /**
         * @see SPARQLQueryPrimaryConditions#ancestorsOf(String)
         */
        protected GraphPattern ancestorsPattern(KnowledgeBase aKB, Iri aContext)
        {
            Iri typeOfProperty = iri(aKB.getTypeIri());
            Iri subClassProperty = iri(aKB.getSubclassIri());
            Iri subPropertyProperty = iri(aKB.getSubPropertyIri());
                        
            switch (this) {
            case ITEM:
            case CLASS:
            case INSTANCE: {
                List<GraphPattern> classPatterns = new ArrayList<>();
                classPatterns.add(
                        aContext.has(Path.of(oneOrMore(subClassProperty)), VAR_SUBJECT));
                classPatterns.add(aContext
                        .has(Path.of(typeOfProperty, zeroOrMore(subClassProperty)), VAR_SUBJECT));
                if (OWL.CLASS.equals(aKB.getClassIri())) {
                    classPatterns.add(aContext.has(
                            Path.of(OWL_INTERSECTIONOF, zeroOrMore(RDF_REST), RDF_FIRST),
                            VAR_SUBJECT));
                }
                
                return union(classPatterns.stream().toArray(GraphPattern[]::new));
            }
            case PROPERTY:
                return aContext.has(Path.of(oneOrMore(subPropertyProperty)), VAR_SUBJECT);
            default:
                throw new IllegalStateException("Unsupported mode: " + this);
            }            
        }
        
        /**
         * @see SPARQLQueryPrimaryConditions#childrenOf(String)
         */
        protected GraphPattern childrenPattern(KnowledgeBase aKB, Iri aContext)
        {
            Iri subPropertyProperty = iri(aKB.getSubPropertyIri());
            Iri subClassProperty = iri(aKB.getSubclassIri());
            Iri typeOfProperty = iri(aKB.getTypeIri());
                        
            switch (this) {
            case ITEM: {
                List<GraphPattern> classPatterns = new ArrayList<>();
                classPatterns.add(
                        VAR_SUBJECT.has(() -> subClassProperty.getQueryString(), aContext));
                classPatterns.add(VAR_SUBJECT.has(typeOfProperty, aContext));
                if (OWL.CLASS.equals(aKB.getClassIri())) {
                    classPatterns.add(VAR_SUBJECT.has(
                            Path.of(OWL_INTERSECTIONOF, zeroOrMore(RDF_REST), RDF_FIRST),
                            aContext));
                }
                
                return GraphPatterns.union(classPatterns.stream().toArray(GraphPattern[]::new));
            }
            case INSTANCE: {
                return VAR_SUBJECT.has(typeOfProperty, aContext);
            }
            case CLASS: {
                // Follow the subclass property and also take into account owl:intersectionOf if
                // using OWL classes
                List<GraphPattern> classPatterns = new ArrayList<>();
                classPatterns.add(VAR_SUBJECT.has(subClassProperty, aContext));
                if (OWL.CLASS.equals(aKB.getClassIri())) {
                    classPatterns.add(VAR_SUBJECT.has(
                            Path.of(OWL_INTERSECTIONOF, zeroOrMore(RDF_REST), RDF_FIRST),
                            aContext));
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
            Iri subClassProperty = iri(aKB.getSubclassIri());
            Iri subPropertyProperty = iri(aKB.getSubPropertyIri());
            Iri typeOfProperty = iri(aKB.getTypeIri());
             
            switch (this) {
            case CLASS: {
                List<GraphPattern> classPatterns = new ArrayList<>();
                classPatterns.add(aContext.has(subClassProperty, VAR_SUBJECT));
                classPatterns.add(aContext.has(typeOfProperty, VAR_SUBJECT));
                if (OWL.CLASS.equals(aKB.getClassIri())) {
                    classPatterns.add(aContext.has(
                            Path.of(OWL_INTERSECTIONOF, zeroOrMore(RDF_REST), RDF_FIRST),
                            VAR_SUBJECT));
                }
                
                return union(classPatterns.stream().toArray(GraphPattern[]::new));
            }
            case PROPERTY:
                return aContext.has(Path.of(oneOrMore(subPropertyProperty)), VAR_SUBJECT);
            default:
                throw new IllegalStateException("Can only request classes or properties as parents");
            }            
        }

        /**
         * @see SPARQLQueryPrimaryConditions#roots()
         */
        protected GraphPattern rootsPattern(KnowledgeBase aKb)
        {
            Iri classIri = iri(aKb.getClassIri());
            Iri subClassProperty = iri(aKb.getSubclassIri());
            Iri typeOfProperty = iri(aKb.getTypeIri());
            Variable otherSubclass = var("otherSubclass");
            
            switch (this) {
            case CLASS: {
                List<GraphPattern> rootPatterns = new ArrayList<>();

                List<IRI> rootConcepts = aKb.getRootConcepts();
                if (rootConcepts != null && !rootConcepts.isEmpty()) {
                    rootPatterns.add(new ValuesPattern(VAR_SUBJECT, rootConcepts.stream()
                            .map(iri -> iri(iri.stringValue())).collect(Collectors.toList())));
                }
                else {
                    List<GraphPattern> classPatterns = new ArrayList<>();
                    classPatterns.add(VAR_SUBJECT.has(subClassProperty, otherSubclass)
                                            .filter(notEquals(VAR_SUBJECT, otherSubclass)));
                    if (OWL.CLASS.equals(aKb.getClassIri())) {
                        classPatterns.add(VAR_SUBJECT.has(OWL_INTERSECTIONOF, bNode()));
                    }
                    
                    rootPatterns.add(union(
                            // ... it is explicitly defined as being a class
                            VAR_SUBJECT.has(typeOfProperty, classIri),
                            // ... it is used as the type of some instance
                            // This can be a very slow condition - so we have to skip it
                            // bNode().has(typeOfProperty, VAR_SUBJECT),
                            // ... it has any subclass
                            bNode().has(subClassProperty, VAR_SUBJECT) )
                        .filterNotExists(
                            union(classPatterns.stream().toArray(GraphPattern[]::new))));
                }
                
                return GraphPatterns
                        .and(rootPatterns.toArray(new GraphPattern[rootPatterns.size()]));
            }
            default:
                throw new IllegalStateException("Can only query for root classes");
            }            
        }
    }
    
    /**
     * Retrieve any item from the KB. There is no check if the item looks like a class, instance or
     * property. The IRI and property mapping used in the patters is obtained from the given KB
     * configuration.
     */
    public static SPARQLQueryPrimaryConditions forItems(KnowledgeBase aKB)
    {
        return new SPARQLQueryBuilder(aKB, Mode.ITEM);
    }

    /**
     * Retrieve only things that look like classes. Identifiers for classes participate as ID
     * in {@code ID IS-A CLASS-IRI}, {@code X SUBCLASS-OF ID}, {@code ID SUBCLASS-OF X}. The
     * IRI and property mapping used in the patters is obtained from the given KB configuration.
     */
    public static SPARQLQueryPrimaryConditions forClasses(KnowledgeBase aKB)
    {
        SPARQLQueryBuilder builder = new SPARQLQueryBuilder(aKB, Mode.CLASS);
        builder.limitToClasses();
        return builder;
    }

    /**
     * Retrieve instances. Instances do <b>not</b> look like classes. The IRI and property mapping
     * used in the patters is obtained from the given KB configuration.
     */
    public static SPARQLQueryPrimaryConditions forInstances(KnowledgeBase aKB)
    {
        SPARQLQueryBuilder builder = new SPARQLQueryBuilder(aKB, Mode.INSTANCE);
        builder.limitToInstances();
        return builder;
    }

    /**
     * Retrieve properties. The IRI and property mapping used in the patters is obtained from the
     * given KB configuration.
     */
    public static SPARQLQueryPrimaryConditions forProperties(KnowledgeBase aKB)
    {
        SPARQLQueryBuilder builder = new SPARQLQueryBuilder(aKB, Mode.PROPERTY);
        builder.limitToProperties();
        return builder;
    }

    private SPARQLQueryBuilder(KnowledgeBase aKB, Mode aMode)
    {
        kb = aKB;
        mode = aMode;
    }
    
    private void addPattern(Priority aPriority, GraphPattern aPattern)
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
    
    /**
     * Generates a pattern which binds all sub-properties of the label property to the given 
     * variable. 
     */
    private GraphPattern bindLabelProperties(Variable aVariable)
    {
        Iri pLabel = mode.getLabelProperty(kb);
        Iri pSubProperty = iri(kb.getSubPropertyIri()); 
        
        return optional(aVariable.has(Path.of(zeroOrMore(pSubProperty)), pLabel));
    }

    @Override
    public SPARQLQueryPrimaryConditions withIdentifier(String... aIdentifiers)
    {
        forceDisableFTS = true;
        
        addPattern(PRIMARY, new ValuesPattern(VAR_SUBJECT,
                Arrays.stream(aIdentifiers).map(Rdf::iri).toArray(RdfValue[]::new)));
        
        return this;
    }
    
    @Override
    public SPARQLQueryPrimaryConditions matchingDomain(String aIdentifier)
    {
        forceDisableFTS = true;
        
        // The original code considered owl:unionOf in the domain defintion... we do not do this
        // at the moment, but to see how it was before and potentially restore that behavior, we
        // keep a copy of the old query here.
//        return String.join("\n"
//                , SPARQL_PREFIX
//                , "SELECT DISTINCT ?s ?l ((?labelGeneral) AS ?lGen) WHERE {"
//                , "{  ?s rdfs:domain/(owl:unionOf/rdf:rest*/rdf:first)* ?aDomain }"
//                , " UNION "
//                , "{ ?s a ?prop "
//                , "    VALUES ?prop { rdf:Property owl:ObjectProperty owl:DatatypeProperty owl:AnnotationProperty} "
//                , "    FILTER NOT EXISTS {  ?s rdfs:domain/(owl:unionOf/rdf:rest*/rdf:first)* ?x } }"
//                , optionalLanguageFilteredValue("?pLABEL", aKB.getDefaultLanguage(),"?s","?l")
//                , optionalLanguageFilteredValue("?pLABEL", null,"?s","?labelGeneral")
//                , queryForOptionalSubPropertyLabel(labelProperties, aKB.getDefaultLanguage(),"?s","?spl")
//                , "}"
//                , "LIMIT " + aKB.getMaxResults());

        Iri subClassProperty = iri(kb.getSubclassIri());
        Iri subPropertyProperty = iri(kb.getSubPropertyIri());
        LabeledBlankNode superClass = Rdf.bNode("superClass");

        addPattern(PRIMARY, union(
                GraphPatterns.and(
                    // Find all super-classes of the domain type
                    iri(aIdentifier).has(Path.of(zeroOrMore(subClassProperty)), superClass),
                    // Either there is a domain which matches the given one
                    VAR_SUBJECT.has(iri(RDFS.DOMAIN), superClass)),
                // ... the property does not define or inherit domain
                isPropertyPattern().and(filterNotExists(VAR_SUBJECT.has(
                        Path.of(zeroOrMore(subPropertyProperty), iri(RDFS.DOMAIN)), bNode())))));
        
        return this;
    }
    
    @Override
    public SPARQLQueryBuilder withLabelMatchingExactlyAnyOf(String... aValues)
    {
        if (aValues.length == 0) {
            returnEmptyResult = true;
            return this;
        }
        
        IRI ftsMode = forceDisableFTS ? FTS_NONE : kb.getFullTextSearchIri();
        
        if (FTS_LUCENE.equals(ftsMode)) {
            addPattern(PRIMARY, withLabelMatchingExactlyAnyOf_RDF4J_FTS(aValues));
        }
        else if (FTS_FUSEKI.equals(ftsMode)) {
            addPattern(PRIMARY, withLabelMatchingExactlyAnyOf_Fuseki_FTS(aValues));
        }
        else if (FTS_VIRTUOSO.equals(ftsMode)) {
            addPattern(PRIMARY, withLabelMatchingExactlyAnyOf_Virtuoso_FTS(aValues));
        }
        else if (FTS_WIKIDATA.equals(ftsMode)) {
            addPattern(PRIMARY, withLabelMatchingExactlyAnyOf_Wikidata_FTS(aValues));
        }
        else if (FTS_NONE.equals(ftsMode) || ftsMode == null) {
            addPattern(PRIMARY, withLabelMatchingExactlyAnyOf_No_FTS(aValues));
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
        List<RdfValue> values = new ArrayList<>();
        String language =  kb.getDefaultLanguage();
        
        for (String value : aValues) {
            if (StringUtils.isBlank(value)) {
                continue;
            }
            
            if (language != null) {
                values.add(literalOfLanguage(value, language));
            }
            
            values.add(literalOf(value));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                new ValuesPattern(VAR_LABEL_CANDIDATE, values),
                VAR_SUBJECT.has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE));
    }
    
    private GraphPattern withLabelMatchingExactlyAnyOf_RDF4J_FTS(String[] aValues)
    {
        prefixes.add(PREFIX_LUCENE_SEARCH);
        
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            // Strip single quotes and asterisks because they have special semantics
            String sanitizedValue = sanitizeQueryStringForFTS(value);
            
            if (StringUtils.isBlank(sanitizedValue)) {
                continue;
            }
            
            valuePatterns.add(VAR_SUBJECT
                    .has(FTS_LUCENE,
                            bNode(LUCENE_QUERY, literalOf(sanitizedValue))
                            .andHas(LUCENE_PROPERTY, VAR_LABEL_PROPERTY))
                    .andHas(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                    .filter(equalsPattern(VAR_LABEL_CANDIDATE, value, kb)));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }

    private GraphPattern withLabelMatchingExactlyAnyOf_Fuseki_FTS(String[] aValues)
    {
        prefixes.add(PREFIX_FUSEKI_SEARCH);
        
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            String sanitizedValue = sanitizeQueryStringForFTS(value);
            
            if (StringUtils.isBlank(sanitizedValue)) {
                continue;
            }
            
            valuePatterns.add(VAR_SUBJECT
                    .has(FUSEKI_QUERY, collectionOf(VAR_LABEL_PROPERTY, literalOf(sanitizedValue)))
                    .andHas(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                    .filter(equalsPattern(VAR_LABEL_CANDIDATE, value, kb)));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }
    
    private GraphPattern withLabelMatchingExactlyAnyOf_Virtuoso_FTS(String[] aValues)
    {
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            String sanitizedValue = sanitizeQueryStringForFTS(value);
            
            if (StringUtils.isBlank(sanitizedValue)) {
                continue;
            }
                        
            valuePatterns.add(VAR_SUBJECT
                    .has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                    .and(VAR_LABEL_CANDIDATE.has(FTS_VIRTUOSO, 
                            literalOf("\"" + sanitizedValue + "\"")))
                    .filter(equalsPattern(VAR_LABEL_CANDIDATE, value, kb)));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }

    private GraphPattern withLabelMatchingExactlyAnyOf_Wikidata_FTS(String[] aValues)
    {
        // In our KB settings, the language can be unset, but the Wikidata entity search
        // requires a preferred language. So we use English as the default.
        String language = kb.getDefaultLanguage() != null ? kb.getDefaultLanguage() : "en";
        
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            String sanitizedValue = sanitizeQueryStringForFTS(value);
            
            if (StringUtils.isBlank(sanitizedValue)) {
                continue;
            }

            valuePatterns.add(new WikidataEntitySearchService(VAR_SUBJECT, sanitizedValue, language)
                            .and(VAR_SUBJECT.has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                                    .filter(equalsPattern(VAR_LABEL_CANDIDATE, value, kb))));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }

    @Override
    public SPARQLQueryBuilder withLabelContainingAnyOf(String... aValues)
    {
        if (aValues.length == 0) {
            returnEmptyResult = true;
            return this;
        }
        
        IRI ftsMode = forceDisableFTS ? FTS_NONE : kb.getFullTextSearchIri();
        
        if (FTS_LUCENE.equals(ftsMode)) {
            addPattern(PRIMARY, withLabelContainingAnyOf_RDF4J_FTS(aValues));
        }
        else if (FTS_FUSEKI.equals(ftsMode)) {
            addPattern(PRIMARY, withLabelContainingAnyOf_Fuseki_FTS(aValues));
        }
        else if (FTS_VIRTUOSO.equals(ftsMode)) {
            addPattern(PRIMARY, withLabelContainingAnyOf_Virtuoso_FTS(aValues));
        }
        else if (FTS_WIKIDATA.equals(ftsMode)) {
            addPattern(PRIMARY, withLabelContainingAnyOf_Wikidata_FTS(aValues));
        }
        else if (FTS_NONE.equals(ftsMode) || ftsMode == null) {
            addPattern(PRIMARY, withLabelContainingAnyOf_No_FTS(aValues));
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
        
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            String sanitizedValue = sanitizeQueryStringForFTS(value);

            if (StringUtils.isBlank(sanitizedValue)) {
                continue;
            }

            valuePatterns.add(VAR_SUBJECT
                    .has(FTS_LUCENE,
                            bNode(LUCENE_QUERY, literalOf(sanitizedValue + "*"))
                            .andHas(LUCENE_PROPERTY, VAR_LABEL_PROPERTY))
                    .andHas(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                    .filter(containsPattern(VAR_LABEL_CANDIDATE, value)));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }

    private GraphPattern withLabelContainingAnyOf_Fuseki_FTS(String[] aValues)
    {
        prefixes.add(PREFIX_FUSEKI_SEARCH);
        
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            String sanitizedValue = sanitizeQueryStringForFTS(value);

            if (StringUtils.isBlank(sanitizedValue)) {
                continue;
            }

            valuePatterns.add(VAR_SUBJECT
                    .has(FUSEKI_QUERY, collectionOf(VAR_LABEL_PROPERTY, literalOf(sanitizedValue)))
                    .andHas(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                    .filter(containsPattern(VAR_LABEL_CANDIDATE, value)));
        }

        
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }

    private GraphPattern withLabelContainingAnyOf_Virtuoso_FTS(String[] aValues)
    {
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            String sanitizedValue = sanitizeQueryStringForFTS(value);
            
            if (StringUtils.isBlank(sanitizedValue)) {
                continue;
            }
                        
            valuePatterns.add(VAR_SUBJECT
                    .has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                    .and(VAR_LABEL_CANDIDATE.has(FTS_VIRTUOSO, 
                            literalOf("\"" + sanitizedValue + "\"")))
                    .filter(containsPattern(VAR_LABEL_CANDIDATE, value)));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }

    private GraphPattern withLabelContainingAnyOf_Wikidata_FTS(String[] aValues)
    {
        // In our KB settings, the language can be unset, but the Wikidata entity search
        // requires a preferred language. So we use English as the default.
        String language = kb.getDefaultLanguage() != null ? kb.getDefaultLanguage() : "en";
        
        List<GraphPattern> valuePatterns = new ArrayList<>();
        for (String value : aValues) {
            String sanitizedValue = sanitizeQueryStringForFTS(value);
            
            if (StringUtils.isBlank(sanitizedValue)) {
                continue;
            }

            valuePatterns.add(new WikidataEntitySearchService(VAR_SUBJECT, sanitizedValue, language)
                    .and(VAR_SUBJECT.has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                            .filter(containsPattern(VAR_LABEL_CANDIDATE, value))));
        }
        
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                union(valuePatterns.toArray(new GraphPattern[valuePatterns.size()])));
    }

    @Override
    public SPARQLQueryBuilder withLabelStartingWith(String aPrefixQuery)
    {
        if (aPrefixQuery.length() == 0) {
            returnEmptyResult = true;
            return this;
        }
        
        
        IRI ftsMode = forceDisableFTS ? FTS_NONE : kb.getFullTextSearchIri();
        
        if (FTS_LUCENE.equals(ftsMode)) {
            addPattern(PRIMARY, withLabelStartingWith_RDF4J_FTS(aPrefixQuery));
        }
        else if (FTS_FUSEKI.equals(ftsMode)) {
            addPattern(PRIMARY, withLabelStartingWith_Fuseki_FTS(aPrefixQuery));
        }
        else if (FTS_VIRTUOSO.equals(ftsMode)) {
            addPattern(PRIMARY, withLabelStartingWith_Virtuoso_FTS(aPrefixQuery));
        }
        else if (FTS_WIKIDATA.equals(ftsMode)) {
            addPattern(PRIMARY, withLabelStartingWith_Wikidata_FTS(aPrefixQuery));
        }
        else if (FTS_NONE.equals(ftsMode) || ftsMode == null) {
            addPattern(PRIMARY, withLabelStartingWith_No_FTS(aPrefixQuery));
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

    private GraphPattern withLabelStartingWith_Wikidata_FTS(String aPrefix)
    {
        // In our KB settings, the language can be unset, but the Wikidata entity search
        // requires a preferred language. So we use English as the default.
        String language = kb.getDefaultLanguage() != null ? kb.getDefaultLanguage() : "en";
        
        if (aPrefix.isEmpty()) {
            returnEmptyResult = true;
        }
        
        String sanitizedValue = sanitizeQueryStringForFTS(aPrefix);

        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                new WikidataEntitySearchService(VAR_SUBJECT, sanitizedValue, language)
                        .and(VAR_SUBJECT.has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                                .filter(startsWithPattern(VAR_LABEL_CANDIDATE, aPrefix))));
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
        
        // Locate all entries where the label contains the prefix (using the FTS) and then
        // filter them by those which actually start with the prefix.
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                VAR_SUBJECT.has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                        .and(VAR_LABEL_CANDIDATE.has(FTS_VIRTUOSO,
                                literalOf(ftsQueryString.toString())))
                        .filter(startsWithPattern(VAR_LABEL_CANDIDATE, aPrefixQuery)));
    }

    private GraphPattern withLabelStartingWith_RDF4J_FTS(String aPrefixQuery)
    {
        // REC: Haven't been able to get this to work with server-side reduction, so implicitly
        // turning it off here.
        serverSideReduce = false;
        
        prefixes.add(PREFIX_LUCENE_SEARCH);
        
        // Strip single quotes and asterisks because they have special semantics
        String sanitizedValue = sanitizeQueryStringForFTS(aPrefixQuery);
        
        if (StringUtils.isBlank(sanitizedValue)) {
            returnEmptyResult = true;
        }

        String queryString = sanitizedValue.trim();

        // If the query string entered by the user does not end with a space character, then
        // we assume that the user may not yet have finished writing the word and add a
        // wildcard
        if (!aPrefixQuery.endsWith(" ")) {
            queryString += "*";
        }

        // Locate all entries where the label contains the prefix (using the FTS) and then
        // filter them by those which actually start with the prefix.
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                VAR_SUBJECT.has(FTS_LUCENE, bNode(LUCENE_QUERY, literalOf(queryString))
                        .andHas(LUCENE_PROPERTY, VAR_LABEL_PROPERTY))
                        .andHas(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                        .filter(startsWithPattern(VAR_LABEL_CANDIDATE, aPrefixQuery)));
    }
    
    private GraphPattern withLabelStartingWith_Fuseki_FTS(String aPrefixQuery)
    {
        // REC: Haven't been able to get this to work with server-side reduction, so implicitly
        // turning it off here.
        serverSideReduce = false;
        
        prefixes.add(PREFIX_FUSEKI_SEARCH);
        
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

        // Locate all entries where the label contains the prefix (using the FTS) and then
        // filter them by those which actually start with the prefix.
        return GraphPatterns.and(
                bindLabelProperties(VAR_LABEL_PROPERTY),
                VAR_SUBJECT.has(FUSEKI_QUERY, collectionOf(VAR_LABEL_PROPERTY, 
                                literalOf(queryString)))
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

    private String asRegexp(String aValue)
    {
        String value = aValue;
        // Escape metacharacters 
        // value = value.replaceAll("[{}()\\[\\].+*?^$\\\\|]", "\\\\\\\\$0");
        value = value.replaceAll("[{}()\\[\\].+*?^$\\\\|]+", ".+");
        // Replace consecutive whitespace or control chars with a whitespace matcher
        value = value.replaceAll("[\\p{Space}\\p{Cntrl}]+", "\\\\\\\\s+");
        return value;
    }
    
    private Expression<?> equalsPattern(Variable aVariable, String aValue, KnowledgeBase aKB)
    {
        String language = aKB.getDefaultLanguage();
        
        List<Expression<?>> expressions = new ArrayList<>();
        
        Operand variable = aVariable;
        
        String regexFlags = "";
        if (caseInsensitive) {
            regexFlags += "i";
        }
        
        // Match using REGEX to be resilient against extra whitespace
        // Match exactly
        String value = "^" + asRegexp(aValue) + "$";
        
        // Match with default language
        if (language != null) {
            expressions.add(and(
                    function(REGEX, variable, literalOf(value), literalOf(regexFlags)),
                    function(LANGMATCHES, function(LANG, aVariable), literalOf(language)))
                            .parenthesize());
        }
        
        // Match without language
        expressions.add(and(
                function(REGEX, variable, literalOf(value), literalOf(regexFlags)),
                function(LANGMATCHES, function(LANG, aVariable), EMPTY_STRING))
                        .parenthesize());
        
        return or(expressions.toArray(new Expression<?>[expressions.size()]));
    }

    private Expression<?> matchString(SparqlFunction aFunction, Variable aVariable, String aValue)
    {
        String language = kb.getDefaultLanguage();

        List<Expression<?>> expressions = new ArrayList<>();

        Operand variable = aVariable;
        
        String regexFlags = "";
        if (caseInsensitive) {
            regexFlags += "i";
        }
        
        String value;
        switch (aFunction) {
        // Match using REGEX to be resilient against extra whitespace
        case STRSTARTS:
            // Match at start
            value = "^" + asRegexp(aValue);
            break;
        case CONTAINS:
            // Match anywhere
            value = ".*" + asRegexp(aValue) + ".*";
            break;
        default:
            throw new IllegalArgumentException(
                    "Only STRSTARTS and CONTAINS are supported, but got [" + aFunction + "]");
        }
        
        // Match with default language
        if (language != null) {
            expressions.add(and(
                    function(REGEX, variable, literalOf(value), literalOf(regexFlags)),
                    function(LANGMATCHES, function(LANG, aVariable), literalOf(language)))
                            .parenthesize());
        }

        expressions.add(and(
                function(REGEX, variable, literalOf(value), literalOf(regexFlags)),
                function(LANGMATCHES, function(LANG, aVariable), EMPTY_STRING))
                        .parenthesize());

        // Match with any language (the reduce code doesn't handle this properly atm)
        // expressions.add(function(aFunction, variable, literalOf(value)));

        return or(expressions.toArray(new Expression<?>[expressions.size()]));
    }

    @Override
    public SPARQLQueryPrimaryConditions roots()
    {
        forceDisableFTS = true;
        
        addPattern(PRIMARY, mode.rootsPattern(kb));
        
        return this;
    }

    @Override
    public SPARQLQueryPrimaryConditions ancestorsOf(String aItemIri)
    {
        forceDisableFTS = true;
        
        Iri contextIri = iri(aItemIri);
        
        addPattern(PRIMARY, mode.ancestorsPattern(kb, contextIri));
        
        return this;
    }
    
    @Override
    public SPARQLQueryPrimaryConditions descendantsOf(String aClassIri)
    {
        forceDisableFTS = true;
        
        Iri contextIri = iri(aClassIri);
        
        addPattern(PRIMARY, mode.descendentsPattern(kb, contextIri));
        
        return this;
    }

    @Override
    public SPARQLQueryPrimaryConditions childrenOf(String aClassIri)
    {
        forceDisableFTS = true;
        
        Iri contextIri = iri(aClassIri);
        
        addPattern(PRIMARY, mode.childrenPattern(kb, contextIri));
        
        return this;
    }

    @Override
    public SPARQLQueryPrimaryConditions parentsOf(String aClassIri)
    {
        forceDisableFTS = true;
        
        Iri contextIri = iri(aClassIri);
        
        addPattern(PRIMARY, mode.parentsPattern(kb, contextIri));
        
        return this;
    }

    private void limitToClasses()
    {
        Iri classIri = iri(kb.getClassIri());
        Iri subClassProperty = iri(kb.getSubclassIri());
        Iri typeOfProperty = iri(kb.getTypeIri());

        List<GraphPattern> classPatterns = new ArrayList<>();
        
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
        if (OWL.CLASS.equals(kb.getClassIri())) {
            classPatterns.add(VAR_SUBJECT.has(
                    Path.of(OWL_INTERSECTIONOF, zeroOrMore(RDF_REST), RDF_FIRST),
                    bNode()));
        }
        
        addPattern(PRIMARY_RESTRICTIONS,
                filterExists(union(classPatterns.stream().toArray(GraphPattern[]::new))));
    }
    
    private void limitToInstances()
    {
        Iri classIri = iri(kb.getClassIri());
        Iri subClassProperty = iri(kb.getSubclassIri());
        Iri typeOfProperty = iri(kb.getTypeIri());
        
        // An item is a class if ...
        addPattern(PRIMARY_RESTRICTIONS, 
                filterExists(VAR_SUBJECT.has(typeOfProperty, bNode()))
                // ... it is explicitly defined as being a class
                .filterNotExists(VAR_SUBJECT.has(typeOfProperty, classIri))
                // ... it has any subclass
                .filterNotExists(bNode().has(subClassProperty, VAR_SUBJECT))
                // ... it has any superclass
                .filterNotExists(VAR_SUBJECT.has(subClassProperty, bNode())));
    }
        
    private void limitToProperties()
    {
        addPattern(PRIMARY_RESTRICTIONS, filterExists(isPropertyPattern()));
    }
    
    private GraphPattern isPropertyPattern()
    {
        Iri propertyIri = iri(kb.getPropertyTypeIri());
        Iri subPropertyProperty = iri(kb.getSubPropertyIri());
        Iri typeOfProperty = iri(kb.getTypeIri());
        Iri pSubClass = iri(kb.getSubclassIri()); 
        
        List<GraphPattern> propertyPatterns = new ArrayList<>();

        // An item is a property if ...
        // ... it is explicitly defined as being a property
        propertyPatterns
                .add(VAR_SUBJECT.has(Path.of(typeOfProperty, zeroOrMore(pSubClass)), propertyIri));
        // ... it has any subproperties
        propertyPatterns.add(bNode().has(subPropertyProperty, VAR_SUBJECT));
        // ... it has any superproperties
        propertyPatterns.add(VAR_SUBJECT.has(subPropertyProperty, bNode()));
        
        // This may be a bit too general... e.g. it takes forever to complete on YAGO
        //// ... or it essentially appears in the predicate position :)
        //propertyPatterns.add(bNode().has(VAR_SUBJECT, bNode()));
        
        return union(propertyPatterns.stream().toArray(GraphPattern[]::new));
    }

        
    @Override
    public SPARQLQueryOptionalElements retrieveLabel()
    {
        // If the label is already retrieved, do nothing
        if (labelImplicitlyRetrieved) {
            return this;
        }
        
        // Retain only the first description
        projections.add(getLabelProjection());
        
        String language = kb.getDefaultLanguage();
        
        List<GraphPattern> labelPatterns = new ArrayList<>();

        // Match with any language (the reduce code doesn't handle this properly atm)
        // labelPatterns.add(VAR_SUBJECT.has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE));
        
        // Find all labels without any language
        labelPatterns.add(VAR_SUBJECT.has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                .filter(function(LANGMATCHES, function(LANG, VAR_LABEL_CANDIDATE), EMPTY_STRING)));

        // Find all labels corresponding to the KB language
        if (language != null) {
            labelPatterns.add(VAR_SUBJECT.has(VAR_LABEL_PROPERTY, VAR_LABEL_CANDIDATE)
                    .filter(function(LANGMATCHES, function(LANG, VAR_LABEL_CANDIDATE), 
                            literalOf(language))));
        }

        addPattern(SECONDARY, bindLabelProperties(VAR_LABEL_PROPERTY));
        
        // Virtuoso has trouble with multiple OPTIONAL clauses causing results which would 
        // normally match to be removed from the results set. Using a UNION seems to address this
        //labelPatterns.forEach(pattern -> addPattern(Priority.SECONDARY, optional(pattern)));
        addPattern(SECONDARY,
                optional(union(labelPatterns.toArray(new GraphPattern[labelPatterns.size()]))));
        
        return this;
    }

    @Override
    public SPARQLQueryOptionalElements retrieveDescription()
    {
        // Retain only the first description
        projections.add(getDescriptionProjection());
        
        String language = kb.getDefaultLanguage();
        Iri descProperty = mode.getDescriptionProperty(kb);

        List<GraphPattern> descriptionPatterns = new ArrayList<>();

        // Find all descriptions corresponding to the KB language
        if (language != null) {
            descriptionPatterns.add(VAR_SUBJECT.has(descProperty, VAR_DESC_CANDIDATE)
                    .filter(function(LANGMATCHES, function(LANG, VAR_DESC_CANDIDATE), 
                            literalOf(language))));
        }

        // Match with any language (the reduce code doesn't handle this properly atm)
        // descriptionPatterns.add(VAR_SUBJECT.has(descProperty, VAR_DESC_CANDIDATE));
        
        // Find all descriptions without any language
        descriptionPatterns.add(VAR_SUBJECT.has(descProperty, VAR_DESC_CANDIDATE)
                .filter(function(LANGMATCHES, function(LANG, VAR_DESC_CANDIDATE), EMPTY_STRING)));

        // Virtuoso has trouble with multiple OPTIONAL clauses causing results which would 
        // normally match to be removed from the results set. Using a UNION seems to address this
        //descriptionPatterns.forEach(pattern -> addPattern(SECONDARY, optional(pattern)));
        addPattern(SECONDARY, optional(
                union(descriptionPatterns.toArray(new GraphPattern[descriptionPatterns.size()]))));
        
        return this;
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
    
    private int getLimit()
    {
        return limitOverride > 0 ? limitOverride : kb.getMaxResults();
    }
    
    @Override
    public SelectQuery selectQuery()
    {
        // Must add it anyway because we group by it
        projections.add(VAR_SUBJECT);

        SelectQuery query = Queries.SELECT().distinct();
        prefixes.forEach(query::prefix);
        projections.forEach(query::select);
        
        // First add the primary patterns and high-level restrictions (e.g. limits to classes or
        // instances) - this is important because Virtuoso has trouble when combining UNIONS,
        // property paths FILTERS and OPTIONALS (which we do a lot). It seems to help when we put
        // the FILTERS together with the primary part of the query into a group.
        // See: https://github.com/openlink/virtuoso-opensource/issues/831
        query.where(() -> SparqlBuilderUtils.getBracedString(
                GraphPatterns.and(concat(primaryPatterns.stream(), primaryRestrictions.stream())
                        .toArray(GraphPattern[]::new)).getQueryString()));
        
        // Then add the optional elements
        secondaryPatterns.stream().forEach(query::where);
        
        if (serverSideReduce) {
            query.groupBy(VAR_SUBJECT);
        }
        
        if (kb.getDefaultDatasetIri() != null) {
            query.from(SparqlBuilder.dataset(
                    SparqlBuilder.from(iri(kb.getDefaultDatasetIri()))));
        }
        
        int actualLimit = getLimit();
        
        if (!serverSideReduce) {
            // If we do not do a server-side reduce, then we may get two results for every item
            // from the server (one with and one without the language), so we need to double the
            // query limit and cut down results locally later.
            actualLimit = actualLimit * 2;
        }
        
        query.limit(actualLimit);
        
        return query;
    }
    
    @Override
    public List<KBHandle> asHandles(RepositoryConnection aConnection, boolean aAll)
    {
        long startTime = currentTimeMillis();
        String queryId = toHexString(hashCode());

        String queryString = selectQuery().getQueryString();
        //queryString = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, queryString, null)
        //        .toString();
        LOG.trace("[{}] Query: {}", queryId, queryString);

        List<KBHandle> results;
        if (returnEmptyResult) {
            results = emptyList();
            
            LOG.debug("[{}] Query was skipped because it would not return any results anyway",
                    queryId);
        }
        else {
            TupleQuery tupleQuery = aConnection.prepareTupleQuery(queryString);
            tupleQuery.setIncludeInferred(includeInferred);
            results = evaluateListQuery(tupleQuery, aAll);
            results.sort(Comparator.comparing(KBObject::getUiLabel, String.CASE_INSENSITIVE_ORDER));
            
            LOG.debug("[{}] Query returned {} results in {}ms", queryId, results.size(),
                    currentTimeMillis() - startTime);
        }

        return results;
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
        long startTime = currentTimeMillis();
        String queryId = toHexString(hashCode());

        limit(1);
        
        SelectQuery query = selectQuery();
        
        String queryString = query.getQueryString();
        LOG.trace("[{}] Query: {}", queryId, queryString);

        if (returnEmptyResult) {
            LOG.debug("[{}] Query was skipped because it would not return any results anyway",
                    queryId);
            
            return false;
        }
        else {
            TupleQuery tupleQuery = aConnection.prepareTupleQuery(queryString);
            boolean result = !evaluateListQuery(tupleQuery, aAll).isEmpty();
            
            LOG.debug("[{}] Query returned {} in {}ms", queryId, result,
                    currentTimeMillis() - startTime);
            
            return result;
        }
    }
    
    @Override
    public Optional<KBHandle> asHandle(RepositoryConnection aConnection, boolean aAll)
    {
        long startTime = currentTimeMillis();
        String queryId = toHexString(hashCode());

        limit(1);
        
        String queryString = selectQuery().getQueryString();
        LOG.trace("[{}] Query: {}", queryId, queryString);

        Optional<KBHandle> result;
        if (returnEmptyResult) {
            result = Optional.empty();
            
            LOG.debug("[{}] Query was skipped because it would not return any results anyway",
                    queryId);
        }
        else {
            TupleQuery tupleQuery = aConnection.prepareTupleQuery(queryString);
            tupleQuery.setIncludeInferred(includeInferred);
            result = evaluateListQuery(tupleQuery, aAll).stream().findFirst();
            
            LOG.debug("[{}] Query returned a result in {}ms", queryId,
                    currentTimeMillis() - startTime);
        }

        return result;
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
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            List<KBHandle> handles = new ArrayList<>();
            while (result.hasNext()) {
                BindingSet bindings = result.next();
                if (bindings.size() == 0) {
                    continue;
                }
                
                // LOG.trace("[{}] Bindings: {}", toHexString(hashCode()), bindings);
    
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
    }
    
    /**
     * Make sure that each result is only represented once, preferably in the default language.
     */
    private List<KBHandle> reduceRedundantResults(List<KBHandle> aHandles)
    {
        Map<String, KBHandle> cMap = new LinkedHashMap<>();
        for (KBHandle handle : aHandles) {
            KBHandle current = cMap.get(handle.getIdentifier());
            
            // Not recorded yet -> add it
            if (current == null) {
                cMap.put(handle.getIdentifier(), handle);
            }
            // Found one with a label while current one doesn't have one
            else if (current.getName() == null && handle.getName() != null) {
                cMap.put(handle.getIdentifier(), handle);
            }
            // Found an exact language match -> use that one instead
            // Note that having a language implies that there is a label!
            else if (kb.getDefaultLanguage().equals(handle.getLanguage())) {
                cMap.put(handle.getIdentifier(), handle);
            }
        }
        
        LOG.trace("Input: {}", aHandles);
        LOG.trace("Output: {}", cMap.values());

        return cMap.values().stream().limit(getLimit()).collect(Collectors.toList());
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
    }
    
    private void extractDescription(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        Binding description = aSourceBindings.getBinding(VAR_DESCRIPTION_NAME);
        Binding descCandidate = aSourceBindings.getBinding(VAR_DESCRIPTION_CANDIDATE_NAME);
        if (description != null) {
            aTargetHandle.setDescription(description.getValue().stringValue());
        }
        else if (descCandidate != null) {
            aTargetHandle.setDescription(descCandidate.getValue().stringValue());
        }
    }
    
    private void extractDomain(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        Binding domain = aSourceBindings.getBinding(VAR_DOMAIN_NAME);
        if (domain != null) {
            aTargetHandle.setDomain(domain.getValue().stringValue());
        }
    }

    private void extractRange(KBHandle aTargetHandle, BindingSet aSourceBindings)
    {
        Binding range = aSourceBindings.getBinding(VAR_RANGE_NAME);
        if (range != null) {
            aTargetHandle.setRange(range.getValue().stringValue());
        }
    }
    
    public static String sanitizeQueryStringForFTS(String aQuery)
    {
        return aQuery
                // character classes to replace with a simple space
                .replaceAll("[\\p{Punct}\\p{Space}\\p{Cntrl}[+*(){}\\[\\]]]+", " ")
                // character classes to remove from the query string
                // \u00AD : SOFT HYPHEN
                .replaceAll("[\\u00AD]", "")
                .trim();
    }
}
