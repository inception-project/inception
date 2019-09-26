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
package de.tudarmstadt.ukp.inception.conceptlinking.feature;

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_NUM_RELATIONS;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_SIGNATURE_OVERLAP;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_SIGNATURE_OVERLAP_SCORE;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingProperties;
import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;
import de.tudarmstadt.ukp.inception.conceptlinking.model.Property;
import de.tudarmstadt.ukp.inception.conceptlinking.model.SemanticSignature;
import de.tudarmstadt.ukp.inception.conceptlinking.util.FileUtils;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.event.KnowledgeBaseConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

/*
 * This feature generator tries to create a "semantic context" from the vicinity of candidates
 * so we can compare the mention context to the KB context. However, the present implementation
 * is very slow and doesn't help much. Before reenabling this we need to properly evaluate this, 
 * test this, and see how performance can be improved.
 */
//@Component
public class SemanticSignatureFeatureGenerator
    implements EntityRankingFeatureGenerator
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String SPARQL_PREFIX = String.join("\n",
            "PREFIX rdf: <" + RDF.NAMESPACE + ">",
            "PREFIX rdfs: <" + RDFS.NAMESPACE + ">",
            "PREFIX owl: <" + OWL.NAMESPACE + ">",
            "PREFIX skos:<" + SKOS.NAMESPACE + ">",
            "PREFIX e:<http://www.wikidata.org/entity/>",
            "PREFIX base:<http://www.wikidata.org/ontology#>",
            "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>");

    private final Map<String, Property> propertyWithLabels;
    private final Set<String> propertyBlacklist;
    private final Set<String> typeBlacklist = new HashSet<>(Arrays
            .asList("commonsmedia", "external-id", "globe-coordinate", "math", "monolingualtext",
                "quantity", "string", "url", "wikibase-property"));
    
    private final LoadingCache<SemanticSignatureCacheKey, SemanticSignature> semanticSignatureCache;

    private final EntityLinkingProperties properties;
    private final KnowledgeBaseService kbService;
    
    @Autowired
    public SemanticSignatureFeatureGenerator(KnowledgeBaseService aKbService,
            RepositoryProperties aRepoProperties, EntityLinkingProperties aProperties)
    {
        kbService = aKbService;
        properties = aProperties;

        semanticSignatureCache = Caffeine.newBuilder()
                .maximumSize(properties.getCacheSize())
                .build(key -> loadSemanticSignature(key));
        
        propertyBlacklist = FileUtils.loadPropertyBlacklist(
                new File(aRepoProperties.getPath(), "/resources/property_blacklist.txt"));
        propertyWithLabels = FileUtils.loadPropertyLabels(
                new File(aRepoProperties.getPath(), "/resources/properties_with_labels.txt"));
    }
    
    @Override
    public void apply(CandidateEntity aCandidate)
    {
        Optional<List<String>> optMentionContext = aCandidate.get(KEY_MENTION_CONTEXT);
        
        if (!optMentionContext.isPresent()) {
            return;
        }
        
        Set<String> mentionContext = new HashSet<>(optMentionContext.get());
        
        SemanticSignature sig = getSemanticSignature(aCandidate.getHandle().getKB(),
                aCandidate.getIRI());
        Set<String> relatedEntities = sig.getRelatedEntities();
        Set<String> signatureOverlap = new HashSet<>();
        for (String entityLabel : relatedEntities) {
            for (String token : entityLabel.split(" ")) {
                if (mentionContext.contains(token.toLowerCase(aCandidate.getLocale()))) {
                    signatureOverlap.add(entityLabel);
                    break;
                }
            }
        }
        
        aCandidate.put(KEY_SIGNATURE_OVERLAP, signatureOverlap);
        aCandidate.put(KEY_SIGNATURE_OVERLAP_SCORE, signatureOverlap.size());
        aCandidate.put(KEY_NUM_RELATIONS,
                (sig.getRelatedRelations() != null) ? sig.getRelatedRelations().size() : 0);
    }
    
    /**
     * Remove all cache entries of a specific project
     * @param aEvent
     *            The event containing the project
     */
    @EventListener
    public void onKnowledgeBaseConfigurationChangedEvent(
        KnowledgeBaseConfigurationChangedEvent aEvent)
    {
        // FIXME instead of maintaining one global cache, we might maintain a cascaded cache
        // where the top level is the project and then for each project we have sub-caches.
        // Then we could invalidate only a specific project's cache. However, right now,
        // we don't have that and there is no way to properly iterate over the caches and
        // invalidate only entries belonging to a specific project. Thus, we need to
        // invalidate all.
        semanticSignatureCache.invalidateAll();
    }
    /**
     * Retrieves the semantic signature of an entity. See documentation of SemanticSignature class.
     */
    private SemanticSignature getSemanticSignature(KnowledgeBase aKB, String aIri)
    {
        return semanticSignatureCache.get(new SemanticSignatureCacheKey(aKB, aIri));
    }

    private SemanticSignature loadSemanticSignature(SemanticSignatureCacheKey aKey)
    {
        Set<String> relatedRelations = new HashSet<>();
        Set<String> relatedEntities = new HashSet<>();
        try (RepositoryConnection conn = kbService.getConnection(aKey.getKnowledgeBase())) {
            TupleQuery query = generateSemanticSignatureQuery(conn,
                    aKey.getQuery(), properties.getSignatureQueryLimit(), aKey.getKnowledgeBase());
            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    BindingSet sol = result.next();
                    String propertyString = sol.getValue("p").stringValue();
                    String labelString = sol.getValue("label").stringValue();
                    if (propertyWithLabels != null) {
                        Property property = propertyWithLabels.get(labelString);
                        int frequencyThreshold = 0;
                        boolean isBlacklisted =
                            (propertyBlacklist != null && propertyBlacklist.contains(propertyString)
                            || (property != null && (typeBlacklist != null
                                && typeBlacklist.contains(property.getType()))));
                        boolean isUnfrequent = property != null
                            && property.getFreq() < frequencyThreshold;
                        if (isBlacklisted || isUnfrequent) {
                            continue;
                        }
                    }
                    relatedEntities.add(labelString);
                    relatedRelations.add(propertyString);
                }
            }
            catch (Exception e) {
                if (StringUtils.contains(e.getMessage(), "UTF-8 sequence")
                        && !log.isDebugEnabled()) {
                    // This is a comparatively common message - no need to always log the entire
                    // stack trace during production, but might still be reasonable to log a
                    // warning.
                    log.warn("Could not get semantic signature: {}", e.getMessage());
                }
                else {
                    log.error("Could not get semantic signature", e);
                }
            }
        }

        return new SemanticSignature(relatedEntities, relatedRelations);
    }

    private static class SemanticSignatureCacheKey
    {
        private final KnowledgeBase knowledgeBase;
        private final String query;

        public SemanticSignatureCacheKey(KnowledgeBase aKnowledgeBase, String aQuery)
        {
            knowledgeBase = aKnowledgeBase;
            query = aQuery;
        }

        public KnowledgeBase getKnowledgeBase()
        {
            return knowledgeBase;
        }

        public String getQuery()
        {
            return query;
        }

        @Override
        public boolean equals(final Object other)
        {
            if (!(other instanceof SemanticSignatureCacheKey)) {
                return false;
            }
            SemanticSignatureCacheKey castOther = (SemanticSignatureCacheKey) other;
            return new EqualsBuilder().append(knowledgeBase, castOther.knowledgeBase)
                    .append(query, castOther.query).isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder().append(knowledgeBase).append(query).toHashCode();
        }
    }
    
    /**
     *
     * @param aIri
     *            an IRI, e.g. "http://www.wikidata.org/entity/Q3"
     * @param aLimit
     *            maximum number of results
     * @param aKb
     *            the Knowledge Base
     * @return a query to retrieve the semantic signature
     */
    public static TupleQuery generateSemanticSignatureQuery(RepositoryConnection aConn, String aIri,
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
                "    ?e1 ?labelIri ?label. ", "  }",
                " LIMIT " + aLimit);

        TupleQuery tupleQuery = aConn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        tupleQuery.setBinding("language", vf.createLiteral(
                (aKb.getDefaultLanguage() != null) ? aKb.getDefaultLanguage() : "en"));
        tupleQuery.setBinding("e2", vf.createIRI(aIri));
        tupleQuery.setBinding("labelIri", aKb.getLabelIri());
        return tupleQuery;
    }
}
