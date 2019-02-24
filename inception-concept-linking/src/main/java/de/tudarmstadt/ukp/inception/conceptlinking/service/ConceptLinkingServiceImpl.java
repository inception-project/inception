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
package de.tudarmstadt.ukp.inception.conceptlinking.service;

import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_VIRTUOSO;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.PREFIX_WIKIDATA_ENTITY;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.UKP_WIKIDATA_SPARQL_ENDPOINT;
import static de.tudarmstadt.ukp.inception.kb.RepositoryType.REMOTE;
import static de.tudarmstadt.ukp.inception.kb.SPARQLQueryStore.searchItemsContaining;
import static de.tudarmstadt.ukp.inception.kb.SPARQLQueryStore.searchItemsExactLabelMatch;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.uima.jcas.JCas;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingProperties;
import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;
import de.tudarmstadt.ukp.inception.conceptlinking.service.feature.EntityRankingFeatureGenerator;
import de.tudarmstadt.ukp.inception.conceptlinking.util.FileUtils;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.SPARQLQueryStore;
import de.tudarmstadt.ukp.inception.kb.event.KnowledgeBaseConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

@Component
public class ConceptLinkingServiceImpl
    implements InitializingBean, ConceptLinkingService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final KnowledgeBaseService kbService;
    private final EntityLinkingProperties properties;

    @org.springframework.beans.factory.annotation.Value
        (value = "${repository.path}/resources/stopwords-en.txt")
    private File stopwordsFile;
    private Set<String> stopwords;

    @org.springframework.beans.factory.annotation.Value
        (value = "${repository.path}/resources/wikidata_entity_freqs.map")
    private File entityFrequencyFile;
    private Map<String, Integer> entityFrequencyMap;

    // A cache for candidates retrieved by fulltext-search
    private LoadingCache<CandidateCacheKey, Set<CandidateEntity>> candidateFullTextCache;

    private static final Comparator<CandidateEntity> SORT_CANDIDATES_BY_FREQUENCY = (e1,
            e2) -> Comparator.comparingInt(CandidateEntity::getFrequency).reversed().compare(e1,
                    e2);
    
    private final List<EntityRankingFeatureGenerator> featureGeneratorsProxy;
    private List<EntityRankingFeatureGenerator> featureGenerators;

    
    @Autowired
    public ConceptLinkingServiceImpl(KnowledgeBaseService aKbService,
            EntityLinkingProperties aProperties,
            @Lazy @Autowired(required = false) List<EntityRankingFeatureGenerator> 
                    aFeatureGenerators)
    {
        Validate.notNull(aKbService);
        Validate.notNull(aProperties);
        
        kbService = aKbService;
        properties = aProperties;
        featureGeneratorsProxy = aFeatureGenerators;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        if (stopwordsFile != null) {
            stopwords = FileUtils.loadStopwordFile(stopwordsFile);
        }
        else {
            stopwords = emptySet();
        }
        
        if (entityFrequencyFile != null) {
            entityFrequencyMap = FileUtils.loadEntityFrequencyMap(entityFrequencyFile);
        }
        else {
            entityFrequencyMap = emptyMap();
        }

        candidateFullTextCache = Caffeine.newBuilder()
                .maximumSize(properties.getCacheSize())
                .build(key -> loadCandidatesFullText(key));
    }
    
    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    /* package private */ void init()
    {
        List<EntityRankingFeatureGenerator> generators = new ArrayList<>();

        if (featureGeneratorsProxy != null) {
            generators.addAll(featureGeneratorsProxy);
            AnnotationAwareOrderComparator.sort(generators);
        
            for (EntityRankingFeatureGenerator generator : generators) {
                log.info("Found entity ranking feature generator: {}",
                        ClassUtils.getAbbreviatedName(generator.getClass(), 20));
            }
        }

        featureGenerators = unmodifiableList(generators);
    }

    public Set<KBHandle> generateCandidates(KnowledgeBase aKB, String aConceptScope,
            ConceptFeatureValueType aValueType, String aQuery, String aMention)
    {
        long startTime = currentTimeMillis();
        Set<KBHandle> result = new HashSet<>();
        
        try (RepositoryConnection conn = kbService.getConnection(aKB)) {
            // Collect exact matches - although exact matches are theoretically contained in the
            // set of containing matches, due to the ranking performed by the KB/FTS, we might not
            // actually see the exact matches within the first N results. So we query for the
            // exact matches separately to ensure we have them.
            List<KBHandle> exactMatches = searchItemsExactLabelMatch(aKB, conn, aQuery, aMention);
            log.debug("Found [{}] candidates exactly matching terms [{}], [{}]",
                    exactMatches.size(), aQuery, aMention);
            result.addAll(exactMatches);

            // Collect containing matches
            List<KBHandle> containingMatches = searchItemsContaining(aKB, conn, aQuery, aMention);
            log.debug("Found [{}] candidates using containing terms [{}], [{}]",
                    exactMatches.size(), aQuery, aMention);
            result.addAll(containingMatches);
        }

        log.debug("Generated [{}] candidates in [{}] ms", result.size(),
                currentTimeMillis() - startTime);

        return result;
    }
    
    @Override
    public List<KBHandle> disambiguate(KnowledgeBase aKB, String aConceptScope,
            ConceptFeatureValueType aValueType, String aQuery, String aMention,
            int aMentionBeginOffset, JCas aJcas)
    {
        Set<KBHandle> candidates = generateCandidates(aKB, aConceptScope, aValueType, aQuery,
                aMention);
        return rankCandidates(aQuery, aMention, candidates, aJcas, aMentionBeginOffset);
    }

    /**
     * Retrieve a set of candidate entities via full-text search from a Knowledge Base.
     * May lead to recursive calls if first search does not yield any results.
     *
     */
    private Set<CandidateEntity> loadCandidatesFullText(CandidateCacheKey aKey)
    {
        if (!aKey.getKnowledgeBase().isSupportConceptLinking()) {
            return Collections.emptySet();
        }
        Set<CandidateEntity> candidates = new HashSet<>();

        try (RepositoryConnection conn = kbService.getConnection(aKey.getKnowledgeBase())) {
            for (KBHandle handle : SPARQLQueryStore.searchItemsContaining(aKey.getKnowledgeBase(),
                    conn, aKey.getQuery())) {
                candidates.add(new CandidateEntity(handle));
            }
        }
        catch (QueryEvaluationException e) {
            log.error("Query evaluation was unsuccessful: ", e);
        }

        return candidates;
    }

    /**
     * Retrieves the sentence containing the mention
     */
    private synchronized Sentence getMentionSentence(JCas aJcas, int aBegin)
    {
        return WebAnnoCasUtil.getSentence(aJcas, aBegin);
    }

    @Override
    public List<KBHandle> rankCandidates(String aQuery, String aMention, Set<KBHandle> aCandidates,
            JCas aJCas, int aBegin)
    {
        long startTime = currentTimeMillis();
        
        List<CandidateEntity> candidates = aCandidates.stream()
                .map(CandidateEntity::new)
                .map(candidate -> {
                    candidate.put(CandidateEntity.KEY_MENTION, aMention);
                    
                    if (aJCas != null) {
                        Sentence sentence = getMentionSentence(aJCas, aBegin);
                        if (sentence != null) {
                            List<String> mentionContext = new ArrayList<>();
                            List<Token> tokens = selectCovered(Token.class, sentence);
                            // Collect left context
                            tokens.stream()
                                    .filter(t -> t.getEnd() <= aBegin)
                                    .sorted(Comparator.comparingInt(Token::getBegin).reversed())
                                    .limit(properties.getMentionContextSize())
                                    .map(t -> t.getCoveredText().toLowerCase(candidate.getLocale()))
                                    .filter(s -> !stopwords.contains(s))
                                    .forEach(mentionContext::add);
                            // Collect right context
                            tokens.stream()
                                    .filter(t -> t.getBegin() >= (aBegin + aMention.length()))
                                    .limit(properties.getMentionContextSize())
                                    .map(t -> t.getCoveredText().toLowerCase(candidate.getLocale()))
                                    .filter(s -> !stopwords.contains(s))
                                    .forEach(mentionContext::add);
                            candidate.put(CandidateEntity.KEY_MENTION_CONTEXT, mentionContext);
                        }
                        else {
                            log.warn("Mention sentence could not be determined. Skipping.");
                        }
                    }
                    
                    return candidate;
                })
                .collect(Collectors.toCollection(ArrayList::new));
        
        // Set frequency
        setFrequenciesForCandidates(candidates);

        // Sort full-text matching candidates by frequency and do cutoff by a threshold
        List<CandidateEntity> frequencySortedCandidates = candidates.stream()
                .sorted(SORT_CANDIDATES_BY_FREQUENCY)
                .limit(properties.getCandidateFrequencyThreshold())
                .collect(Collectors.toList());

        // Add exact matching candidates
        frequencySortedCandidates.addAll(candidates);

        // Set the feature values
        frequencySortedCandidates.parallelStream().forEach(candidate -> {
            for (EntityRankingFeatureGenerator generator : featureGenerators) {
                generator.apply(candidate);
            }
        });

        // Do the main ranking
        List<CandidateEntity> rankedCandidates = sortCandidates(frequencySortedCandidates);
        
        List<KBHandle> results = rankedCandidates.stream()
                .map(candidate -> {
                    KBHandle handle = candidate.getHandle();
                    if (log.isDebugEnabled()) {
                        handle.setDescription(
                                handle.getDescription() + "\n" + candidate.getFeatures());
                    }
                    return handle;
                })
                // .distinct()
                // .filter(h -> h.getIdentifier().contains(":"))
                .limit(properties.getCandidateDisplayLimit())
                .collect(Collectors.toList());
         
        log.debug("Ranked [{}] candidates for mention [{}] and query [{}] in [{}] ms",
                 results.size(), aMention, aQuery, currentTimeMillis() - startTime);
         
        return results;
    }

    /**
     * Set frequencies for CandidateEntities from full-text-search
     */
    private void setFrequenciesForCandidates(Collection<CandidateEntity> aCandidatesFullText)
    {
        // Set frequency
        if (entityFrequencyMap != null) {
            for (CandidateEntity l : aCandidatesFullText) {
                KnowledgeBase kb = l.getHandle().getKB();
                // For UKP Wikidata
                if (kb.getType() == REMOTE && FTS_VIRTUOSO.equals(kb.getFullTextSearchIri())) {
                    RepositoryImplConfig cfg = kbService.getKnowledgeBaseConfig(kb);
                    if (((SPARQLRepositoryConfig) cfg).getQueryEndpointUrl()
                            .equals(UKP_WIKIDATA_SPARQL_ENDPOINT)) {
                        String key = l.getIRI();
                        key = key.replace(PREFIX_WIKIDATA_ENTITY, "");
                        if (entityFrequencyMap.get(key) != null) {
                            l.setFrequency(entityFrequencyMap.get(key));
                        }
                    }
                }
            }
        }
    }

    /**
     * Sort candidates by multiple keys.
     * The edit distance between query and label is given high importance
     * to push the exact matching candidates to the top.
     * A high signature overlap score is preferred.
     * A low edit distance is preferred.
     * A high entity frequency is preferred.
     * A high number of related relations is preferred.
     * A low wikidata ID rank is preferred.
     */
    private List<CandidateEntity> sortCandidates(List<CandidateEntity> candidates)
    {
        candidates.sort((e1, e2) -> new CompareToBuilder()
            .append(e1.getLevQuery(), e2.getLevQuery())
            .append(e2.getSignatureOverlapScore(), e1.getSignatureOverlapScore())
            .append(e1.getLevContext() + e1.getLevMatchLabel(),
                e2.getLevContext() + e2.getLevMatchLabel())
            .append(e2.getFrequency(), e1.getFrequency())
            .append(e2.getNumRelatedRelations(), e1.getNumRelatedRelations())
            .append(e1.getIdRank(), e2.getIdRank()).toComparison());
        return candidates;
    }

    @Override
    public List<KBHandle> getLinkingInstancesInKBScope(String aRepositoryId, String aConceptScope,
            ConceptFeatureValueType aValueType, String aQuery, String aMention,
            int aMentionBeginOffset, JCas aJCas, Project aProject)
    {
        // Sanitize query by removing typical wildcard characters
        String query = aQuery.replaceAll("[*?]", "").trim();
        
        // Determine which knowledge bases to query
        List<KnowledgeBase> knowledgeBases = new ArrayList<>();
        if (aRepositoryId != null) {
            kbService.getKnowledgeBaseById(aProject, aRepositoryId).filter(KnowledgeBase::isEnabled)
                    .ifPresent(knowledgeBases::add);
        }
        else {
            knowledgeBases.addAll(kbService.getEnabledKnowledgeBases(aProject));
        }
        
        // Query the knowledge bases for candidates
        Set<KBHandle> candidates = new HashSet<>();
        for (KnowledgeBase kb : knowledgeBases) {
            candidates.addAll(
                    generateCandidates(kb, aConceptScope, aValueType, query, aMention));
        }
        
        // Rank the candidates and return them
        return rankCandidates(query, aMention, candidates, aJCas, aMentionBeginOffset);
    }

    @Override
    public List<KBHandle> searchEntitiesFullText(KnowledgeBase aKB, String aQuery)
    {
        if (StringUtils.isBlank(aQuery)) {
            return emptyList();
        }
        
        return generateCandidates(aKB, null, ConceptFeatureValueType.ANY_OBJECT, aQuery, null)
                .stream()
                .map(CandidateEntity::new)
                .sorted(SORT_CANDIDATES_BY_FREQUENCY)
                .limit(properties.getCandidateFrequencyThreshold())
                .map(c -> new KBHandle(c.getIRI(), c.getLabel(), c.getDescription()))
                .distinct()
                .limit(properties.getCandidateDisplayLimit())
                .filter(h -> h.getIdentifier().contains(":")).collect(Collectors.toList());
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
        candidateFullTextCache.invalidateAll();
    }

    private static class CandidateCacheKey
    {
        private final KnowledgeBase knowledgeBase;
        private final String query;

        public CandidateCacheKey(KnowledgeBase aKnowledgeBase, String aQuery)
        {
            super();
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
            if (!(other instanceof CandidateCacheKey)) {
                return false;
            }
            CandidateCacheKey castOther = (CandidateCacheKey) other;
            return new EqualsBuilder().append(knowledgeBase, castOther.knowledgeBase)
                    .append(query, castOther.query).isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder().append(knowledgeBase).append(query).toHashCode();
        }
    }
}
