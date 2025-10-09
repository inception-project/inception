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
package de.tudarmstadt.ukp.inception.conceptlinking.service;

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY_BEST_MATCH_TERM_NC;
import static de.tudarmstadt.ukp.inception.kb.RepositoryType.LOCAL;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparingInt;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toCollection;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingProperties;
import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingPropertiesImpl;
import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.conceptlinking.feature.EntityRankingFeatureGenerator;
import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;
import de.tudarmstadt.ukp.inception.conceptlinking.ranking.BaselineRankingStrategy;
import de.tudarmstadt.ukp.inception.conceptlinking.util.FileUtils;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder;
import de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryPrimaryConditions;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;
import de.tudarmstadt.ukp.inception.support.wicket.WicketUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link EntityLinkingServiceAutoConfiguration#conceptLinkingService}.
 * </p>
 */
public class ConceptLinkingServiceImpl
    implements InitializingBean, ConceptLinkingService
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final KnowledgeBaseService kbService;
    private final EntityLinkingProperties properties;
    private final RepositoryProperties repoProperties;

    private Set<String> stopwords;

    private final List<EntityRankingFeatureGenerator> featureGeneratorsProxy;
    private List<EntityRankingFeatureGenerator> featureGenerators;

    @Autowired
    public ConceptLinkingServiceImpl(KnowledgeBaseService aKbService,
            EntityLinkingPropertiesImpl aProperties, RepositoryProperties aRepoProperties,
            @Lazy @Autowired(required = false) List<EntityRankingFeatureGenerator> aFeatureGenerators)
    {
        Objects.requireNonNull(aKbService, "Parameter [kbService] has to be specified");
        Objects.requireNonNull(aProperties, "Parameter [properties] has to be specified");

        kbService = aKbService;
        properties = aProperties;
        featureGeneratorsProxy = aFeatureGenerators;
        repoProperties = aRepoProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        var stopwordsFile = new File(repoProperties.getPath(), "resources/stopwords-en.txt");
        stopwords = FileUtils.loadStopwordFile(stopwordsFile);
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    void init()
    {
        var generators = new ArrayList<EntityRankingFeatureGenerator>();

        if (featureGeneratorsProxy != null) {
            generators.addAll(featureGeneratorsProxy);
            AnnotationAwareOrderComparator.sort(generators);

            for (EntityRankingFeatureGenerator generator : generators) {
                LOG.debug("Found entity ranking feature generator: {}",
                        ClassUtils.getAbbreviatedName(generator.getClass(), 20));
            }
        }

        BaseLoggers.BOOT_LOG.info("Found [{}] entity ranking feature generators",
                generators.size());

        featureGenerators = unmodifiableList(generators);
    }

    private SPARQLQueryPrimaryConditions newQueryBuilder(ConceptFeatureValueType aValueType,
            KnowledgeBase aKB)
    {
        switch (aValueType) {
        case ANY_OBJECT:
            return SPARQLQueryBuilder.forItems(aKB);
        case CONCEPT:
            return SPARQLQueryBuilder.forClasses(aKB);
        case INSTANCE:
            return SPARQLQueryBuilder.forInstances(aKB);
        case PROPERTY:
            return SPARQLQueryBuilder.forProperties(aKB);
        default:
            throw new IllegalArgumentException("Unknown item type: [" + aValueType + "]");
        }
    }

    private Set<KBHandle> generateCandidates(KnowledgeBase aKB, String aConceptScope,
            ConceptFeatureValueType aValueType, String aQuery, String aMention)
    {
        // If the query of the user is smaller or equal to this threshold, then we only use it
        // for exact matching. If it is longer, we look for concepts which start with or which
        // contain the users input. This is meant as a performance optimization for large KBs
        // where we want to avoid long reaction times when there is large number of candidates
        // (which is very likely when e.g. searching for all items starting with or containing a
        // specific letter.
        final var threshold = aKB.getType() == LOCAL ? 0 : 3;

        var preResolver = newQueryBuilder(aValueType, aKB);
        Set<String> prefLabelProperties;
        Set<String> additionalMatchProperties;
        try (var conn = kbService.getConnection(aKB)) {
            prefLabelProperties = preResolver.resolvePrefLabelProperties(conn);
            additionalMatchProperties = preResolver.resolveAdditionalMatchingProperties(conn);
        }

        var results = new LinkedHashSet<KBHandle>();

        var startTime = currentTimeMillis();
        try {
            if (aQuery != null) {
                var exactMatches = findExactIriMatches(aKB, aConceptScope, aValueType, aQuery,
                        prefLabelProperties, additionalMatchProperties);

                // If there was an exact IRI match, there is probably little point in searching for
                // matching labels... I mean, who would use an IRI as a concept label...?
                if (!exactMatches.isEmpty()) {
                    return exactMatches;
                }
            }

            // Collect exact matches - although exact matches are theoretically contained in the
            // set of containing matches, due to the ranking performed by the KB/FTS, we might
            // not actually see the exact matches within the first N results. So we query for
            // the exact matches separately to ensure we have them.
            // Mind, we use the query and the mention text here - of course we don't only want
            // exact matches of the query but also of the mention :)
            var exactMatches = supplyAsync(() -> findExactMatches(aKB, aConceptScope, aValueType,
                    aQuery, aMention, prefLabelProperties, additionalMatchProperties));

            // Next we also do a "starting with" search - but only if the user's query is longer
            // than the threshold - this is because for short queries, we'd get way too many results
            // which would be slow - and also the results would likely not be very accurate
            var startingWithMatches = supplyAsync(() -> findStartingWithMatches(aKB, aConceptScope,
                    aValueType, aQuery, threshold, prefLabelProperties, additionalMatchProperties));

            // Finally, we use the query and mention also for a "containing" search - but only if
            // they are longer than the threshold. Again, for very short query/mention, we'd
            // otherwise get way too many matches, being slow and not accurate.
            var containingMatches = supplyAsync(
                    () -> findContainingMatches(aKB, aConceptScope, aValueType, aQuery, aMention,
                            threshold, prefLabelProperties, additionalMatchProperties));

            results.addAll(exactMatches.join());
            results.addAll(startingWithMatches.join());
            results.addAll(containingMatches.join());
        }
        finally {
            long duration = currentTimeMillis() - startTime;
            LOG.debug("Generated [{}] candidates from {} in {}ms", results.size(), aKB, duration);
            WicketUtil.serverTiming("generateCandidates", duration);
        }

        return results;
    }

    private List<KBHandle> findExactMatches(KnowledgeBase aKB, String aConceptScope,
            ConceptFeatureValueType aValueType, String aQuery, String aMention,
            Set<String> aPrefLabelProperties, Set<String> aAdditionalMatchProperties)
    {
        var exactLabels = asList(aQuery, aMention).stream() //
                .filter(StringUtils::isNotBlank) //
                .toArray(String[]::new);

        if (exactLabels.length == 0) {
            return emptyList();
        }

        var startTime = currentTimeMillis();

        var query = newQueryBuilder(aValueType, aKB);
        query.withPrefLabelProperties(aPrefLabelProperties);
        query.withAdditionalMatchingProperties(aAdditionalMatchProperties);

        if (aConceptScope != null) {
            // Scope-limiting must always happen before label matching!
            query.descendantsOf(aConceptScope);
        }

        query.withLabelMatchingExactlyAnyOf(exactLabels);

        query.retrieveLabel().retrieveDescription().retrieveDeprecation();

        List<KBHandle> result;
        if (aKB.isReadOnly()) {
            result = kbService.listHandlesCaching(aKB, query, true);
        }
        else {
            result = kbService.read(aKB, conn -> query.asHandles(conn, true));
        }

        var duration = currentTimeMillis() - startTime;
        LOG.debug("Found [{}] candidates exactly matching {} in {}ms", result.size(),
                asList(exactLabels), duration);
        WicketUtil.serverTiming("findExactMatches", duration);

        return result;
    }

    private List<KBHandle> findContainingMatches(KnowledgeBase aKB, String aConceptScope,
            ConceptFeatureValueType aValueType, String aQuery, String aMention, int aThreshold,
            Set<String> aPrefLabelProperties, Set<String> aAdditionalMatchProperties)
    {
        var longLabels = asList(aQuery, aMention).stream() //
                .filter(Objects::nonNull) //
                .map(s -> s.trim()) //
                .filter(s -> s.length() >= aThreshold) //
                .toArray(String[]::new);

        if (longLabels.length == 0) {
            LOG.debug(
                    "Not searching for candidates containing query/mention because they are too short");
            return emptyList();
        }

        var startTime = currentTimeMillis();

        // Collect containing matches
        var query = newQueryBuilder(aValueType, aKB);
        query.withPrefLabelProperties(aPrefLabelProperties);
        query.withAdditionalMatchingProperties(aAdditionalMatchProperties);

        if (aConceptScope != null) {
            // Scope-limiting must always happen before label matching!
            query.descendantsOf(aConceptScope);
        }

        if (aKB.isUseFuzzy()) {
            query.withLabelMatchingAnyOf(longLabels);
        }
        else {
            query.withLabelContainingAnyOf(longLabels);
        }

        query.retrieveLabel().retrieveDescription().retrieveDeprecation();

        List<KBHandle> result;
        if (aKB.isReadOnly()) {
            result = kbService.listHandlesCaching(aKB, query, true);
        }
        else {
            result = kbService.read(aKB, conn -> query.asHandles(conn, true));
        }

        var duration = currentTimeMillis() - startTime;
        LOG.debug("Found [{}] candidates containing {} in {}ms", result.size(), asList(longLabels),
                duration);
        WicketUtil.serverTiming("findContainingMatches", duration);

        return result;
    }

    private List<KBHandle> findStartingWithMatches(KnowledgeBase aKB, String aConceptScope,
            ConceptFeatureValueType aValueType, String aQuery, final int threshold,
            Set<String> aPrefLabelProperties, Set<String> aAdditionalMatchProperties)
    {
        if (aQuery == null || aQuery.trim().length() < threshold) {
            LOG.debug("Not searching for candidates matching query because it is too short");
            return emptyList();
        }

        var startTime = currentTimeMillis();

        var query = newQueryBuilder(aValueType, aKB);
        query.withPrefLabelProperties(aPrefLabelProperties);
        query.withAdditionalMatchingProperties(aAdditionalMatchProperties);

        if (aConceptScope != null) {
            // Scope-limiting must always happen before label matching!
            query.descendantsOf(aConceptScope);
        }

        // Collect matches starting with the query - this is the main driver for the
        // auto-complete functionality
        query.withLabelStartingWith(aQuery);

        query.retrieveLabel().retrieveDescription().retrieveDeprecation();

        List<KBHandle> result;
        if (aKB.isReadOnly()) {
            result = kbService.listHandlesCaching(aKB, query, true);
        }
        else {
            result = kbService.read(aKB, conn -> query.asHandles(conn, true));
        }

        var duration = currentTimeMillis() - startTime;
        LOG.debug("Found [{}] candidates starting with [{}] in {}ms", result.size(), aQuery,
                duration);
        WicketUtil.serverTiming("findStartingWithMatches", duration);

        return result;
    }

    private Set<KBHandle> findExactIriMatches(KnowledgeBase aKB, String aConceptScope,
            ConceptFeatureValueType aValueType, String aQuery, Set<String> aPrefLabelProperties,
            Set<String> aAdditionalMatchProperties)
    {
        var startTime = currentTimeMillis();

        ParsedIRI iri = null;
        try {
            iri = new ParsedIRI(aQuery);
        }
        catch (URISyntaxException | NullPointerException e) {
            // Skip match by IRI.
        }

        if (iri == null || !iri.isAbsolute()) {
            return emptySet();
        }

        var iriMatchBuilder = newQueryBuilder(aValueType, aKB).withIdentifier(aQuery);
        iriMatchBuilder.withPrefLabelProperties(aPrefLabelProperties);
        iriMatchBuilder.withAdditionalMatchingProperties(aAdditionalMatchProperties);

        if (aConceptScope != null) {
            iriMatchBuilder.descendantsOf(aConceptScope);
        }

        iriMatchBuilder.retrieveLabel().retrieveDescription().retrieveDeprecation();

        var iriMatches = new LinkedHashSet<KBHandle>();
        if (aKB.isReadOnly()) {
            iriMatches.addAll(kbService.listHandlesCaching(aKB, iriMatchBuilder, true));
        }
        else {
            iriMatches.addAll(kbService.read(aKB, conn -> iriMatchBuilder.asHandles(conn, true)));
        }

        var duration = currentTimeMillis() - startTime;
        LOG.debug("Found [{}] candidates exactly matching IRI [{}] in {}ms", iriMatches.size(),
                aQuery, duration);
        WicketUtil.serverTiming("findExactIriMatches", duration);

        return iriMatches;
    }

    @Override
    public List<KBHandle> disambiguate(KnowledgeBase aKB, String aConceptScope,
            ConceptFeatureValueType aValueType, String aQuery, String aMention,
            int aMentionBeginOffset, CAS aCas)
    {
        var candidates = generateCandidates(aKB, aConceptScope, aValueType, aQuery, aMention);
        return rankCandidates(aQuery, aMention, candidates, aCas, aMentionBeginOffset);
    }

    private CandidateEntity fillMentionContext(CandidateEntity candidate, String aMention, CAS aCas,
            int aBegin)
    {
        if (aCas != null && aMention != null) {
            var sentence = selectSentenceCovering(aCas, aBegin);
            if (sentence != null) {
                var mentionContext = new ArrayList<String>();
                var tokens = selectTokensCovered(sentence);
                // Collect left context
                tokens.stream().filter(t -> t.getEnd() <= aBegin)
                        .sorted(comparingInt(AnnotationFS::getBegin).reversed())
                        .limit(properties.getMentionContextSize())
                        .map(t -> t.getCoveredText().toLowerCase(candidate.getLocale()))
                        .filter(s -> !stopwords.contains(s)) //
                        .forEach(mentionContext::add);
                // Collect right context
                tokens.stream().filter(t -> t.getBegin() >= (aBegin + aMention.length()))
                        .limit(properties.getMentionContextSize())
                        .map(t -> t.getCoveredText().toLowerCase(candidate.getLocale()))
                        .filter(s -> !stopwords.contains(s)) //
                        .forEach(mentionContext::add);
                candidate.put(KEY_MENTION_CONTEXT, mentionContext);
            }
            else {
                LOG.warn("Mention sentence could not be determined. Skipping.");
            }
        }

        return candidate;
    }

    /**
     * This method does the actual ranking of the candidate entities. First the candidates from
     * full-text matching are sorted by frequency cutoff after a threshold because they are more
     * numerous. Then the candidates from exact matching are added and sorted by multiple keys.
     * 
     * @param aQuery
     *            the input made by the user into the feature editor (can be null)
     * @param aMention
     *            the mention
     * @param aCandidates
     *            the linking candidate handles
     * @param aCas
     *            the CAS containing the mention
     * @param aBegin
     *            the begin offset of the mention in the document
     * @return the ranked handles
     */
    private List<KBHandle> rankCandidates(String aQuery, String aMention, Set<KBHandle> aCandidates,
            CAS aCas, int aBegin)
    {
        var startTime = currentTimeMillis();

        // Set the feature values
        var candidates = aCandidates.stream() //
                .map(CandidateEntity::new) //
                .map(candidate -> candidate.withQuery(aQuery)) //
                .map(candidate -> candidate.withMention(aMention)) //
                .map(candidate -> fillMentionContext(candidate, aMention, aCas, aBegin))
                .map(candidate -> {
                    for (var generator : featureGenerators) {
                        generator.apply(candidate);
                    }
                    return candidate;
                }) //
                .collect(toCollection(ArrayList::new));

        // Do the main ranking
        // Sort candidates by multiple keys.
        candidates.sort(BaselineRankingStrategy.getInstance());

        var results = candidates.stream() //
                .map(candidate -> {
                    var handle = candidate.getHandle();
                    handle.setDebugInfo(candidate.getFeaturesAsString());
                    candidate.get(KEY_QUERY_BEST_MATCH_TERM_NC)
                            .filter(t -> !t.equalsIgnoreCase(handle.getUiLabel()))
                            .ifPresent(handle::setQueryBestMatchTerm);
                    return handle;
                }) //
                .toList();

        var rank = 1;
        for (var handle : results) {
            handle.setRank(rank);
            rank++;
        }

        var duration = currentTimeMillis() - startTime;
        LOG.debug("Ranked [{}] candidates for mention [{}] and query [{}] in [{}] ms",
                results.size(), aMention, aQuery, duration);

        WicketUtil.serverTiming("rankCandidates", duration);

        return results;
    }

    @Override
    public List<KBHandle> getLinkingInstancesInKBScope(String aRepositoryId, String aConceptScope,
            ConceptFeatureValueType aValueType, String aQuery, String aMention,
            int aMentionBeginOffset, CAS aCas, Project aProject)
    {
        // Sanitize query by removing typical wildcard characters
        var query = aQuery.replaceAll("[*?]", "").trim();

        // Determine which knowledge bases to query
        var knowledgeBases = new ArrayList<KnowledgeBase>();
        if (aRepositoryId != null) {
            kbService.getKnowledgeBaseById(aProject, aRepositoryId) //
                    .filter(KnowledgeBase::isEnabled) //
                    .ifPresent(knowledgeBases::add);
        }
        else {
            knowledgeBases.addAll(kbService.getEnabledKnowledgeBases(aProject));
        }

        // Query the knowledge bases for candidates
        var candidates = new HashSet<KBHandle>();
        for (var kb : knowledgeBases) {
            candidates.addAll(generateCandidates(kb, aConceptScope, aValueType, query, aMention));
        }

        // Rank the candidates and return them
        return rankCandidates(query, aMention, candidates, aCas, aMentionBeginOffset);
    }

    /**
     * Find KB items (classes and instances) matching the given query.
     */
    @Override
    public List<KBHandle> searchItems(KnowledgeBase aKB, String aQuery)
    {
        return disambiguate(aKB, null, ConceptFeatureValueType.ANY_OBJECT, aQuery, null, 0, null);
    }

    /**
     * Get the sentence based on the annotation begin offset
     *
     * @param aCas
     *            the CAS.
     * @param aBegin
     *            the begin offset.
     * @return the sentence.
     */
    private static AnnotationFS selectSentenceCovering(CAS aCas, int aBegin)
    {
        for (var sentence : select(aCas, getType(aCas, Sentence.class))) {
            if (sentence.getBegin() <= aBegin && sentence.getEnd() > aBegin) {
                return sentence;
            }
        }
        return null;
    }

    private static Collection<AnnotationFS> selectTokensCovered(AnnotationFS aCover)
    {
        return CasUtil.selectCovered(aCover.getCAS(), getType(aCover.getCAS(), Token.class),
                aCover);
    }
}
