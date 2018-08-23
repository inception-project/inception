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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingProperties;
import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;
import de.tudarmstadt.ukp.inception.conceptlinking.model.Property;
import de.tudarmstadt.ukp.inception.conceptlinking.model.SemanticSignature;
import de.tudarmstadt.ukp.inception.conceptlinking.util.FileUtils;
import de.tudarmstadt.ukp.inception.conceptlinking.util.LRUCache;
import de.tudarmstadt.ukp.inception.conceptlinking.util.QueryUtil;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.event.KnowledgeBaseConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

@Component
public class ConceptLinkingService
{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private @Resource KnowledgeBaseService kbService;
    private @Resource EntityLinkingProperties properties;

    @org.springframework.beans.factory.annotation.Value
        (value = "${repository.path}/resources/stopwords-en.txt")
    private File stopwordsFile;
    private Set<String> stopwords;

    @org.springframework.beans.factory.annotation.Value
        (value = "${repository.path}/resources/wikidata_entity_freqs.map")
    private File entityFrequencyFile;
    private Map<String, Integer> entityFrequencyMap;

    @org.springframework.beans.factory.annotation.Value
        (value = "${repository.path}/resources/property_blacklist.txt")
    private File propertyBlacklistFile;
    private Set<String> propertyBlacklist;

    @org.springframework.beans.factory.annotation.Value
        (value = "${repository.path}/resources/properties_with_labels.txt")
    private File propertyWithLabelsFile;
    private Map<String, Property> propertyWithLabels;

    private Set<String> typeBlacklist = new HashSet<>(Arrays
        .asList("commonsmedia", "external-id", "globe-coordinate", "math", "monolingualtext",
            "quantity", "string", "url", "wikibase-property"));

    private static final String WIKIDATA_PREFIX = "http://www.wikidata.org/entity/";
    private static final String POS_VERB_PREFIX = "V";
    private static final String POS_NOUN_PREFIX = "N";
    private static final String POS_ADJECTIVE_PREFIX = "J";

    private Map<ImmutablePair<Project, String>, Set<CandidateEntity>> candidateCache;
    private Map<ImmutablePair<Project, String>, SemanticSignature> semanticSignatureCache;

    @PostConstruct
    public void init()
    {
        stopwords = FileUtils.loadStopwordFile(stopwordsFile);
        entityFrequencyMap = FileUtils.loadEntityFrequencyMap(entityFrequencyFile);
        propertyBlacklist = FileUtils.loadPropertyBlacklist(propertyBlacklistFile);
        propertyWithLabels = FileUtils.loadPropertyLabels(propertyWithLabelsFile);
      
        candidateCache = Collections.synchronizedMap(new LRUCache<>(properties.getCacheSize()));
        semanticSignatureCache = Collections
            .synchronizedMap(new LRUCache<>(properties.getCacheSize()));
    }

    public String getBeanName()
    {
        return "ConceptLinkingService";
    }

    /*
     * Retrieves the sentence containing the mention
     */
    private synchronized Sentence getMentionSentence(JCas aJcas, int aBegin)
    {
        return WebAnnoCasUtil.getSentence(aJcas, aBegin);
    }    

    /**
     * Retrieve a set of candidate entities via full-text search from a Knowledge Base.
     * May lead to recursive calls if first search does not yield any results.
     *
     * @param aKB the Knowledge Base in which to search.
     * @param aTypedString typed string from the user
     * @param aMention the marked surface form
     */
    private Set<CandidateEntity> retrieveCandidatesFullText(KnowledgeBase aKB, String aTypedString,
        String aMention)
    {
        Set<CandidateEntity> candidatesFullText = new HashSet<>();
        
        ImmutablePair<Project, String> mentionPair = new ImmutablePair<>(aKB.getProject(),
            aMention);

        ImmutablePair<Project, String> typedStringPair = new ImmutablePair<>(aKB.getProject(),
            aTypedString);

        try (RepositoryConnection conn = kbService.getConnection(aKB)) {
            addFullTextCandidates(aKB, candidatesFullText, aMention, mentionPair, conn);
            addFullTextCandidates(aKB, candidatesFullText, aTypedString, typedStringPair, conn);
        }
        catch (QueryEvaluationException e) {
            logger.error("Query evaluation was unsuccessful: ", e);
        }

        if (candidatesFullText.isEmpty()) {
            String[] split = aMention.split(" ");
            if (split.length > 1) {
                for (String s : split) {
                    candidatesFullText.addAll(retrieveCandidatesFullText(aKB, s, aMention));
                }
            }
        }

        return candidatesFullText;
    }

    /**
     * Retrieve a set of candidate entities from a Knowledge Base
     *
     * @param aKB the Knowledge Base in which to search.
     * @param aTypedString typed string from the user
     * @param aMention the marked surface form, which is pre-processed first.
     */
    private Set<CandidateEntity> retrieveCandidatesExact(KnowledgeBase aKB, String aTypedString,
        String aMention)
    {
        Set<CandidateEntity> candidates = new HashSet<>();

        try (RepositoryConnection conn = kbService.getConnection(aKB)) {
            TupleQuery exactQuery = QueryUtil
                .generateCandidateExactQuery(conn, aTypedString, aMention, aKB.getDescriptionIri(),
                    aKB.getFtsIri());
            candidates.addAll(processCandidateQuery(exactQuery));
        }
        catch (QueryEvaluationException e) {
            logger.error("Query evaluation was unsuccessful: ", e);
        }
        return candidates;
    }

    private void addFullTextCandidates(KnowledgeBase aKB, Set<CandidateEntity> aFullTextCandidates,
        String aString, ImmutablePair<Project, String> aPair,
        RepositoryConnection aConn)
    {
        if (candidateCache.containsKey(aPair)) {
            aFullTextCandidates.addAll(candidateCache.get(aPair));
        }
        else {
            TupleQuery fullTextQueryMention = QueryUtil
                .generateCandidateFullTextQuery(aConn, aString,
                    properties.getCandidateQueryLimit(), aKB.getDescriptionIri(), aKB.getFtsIri());
            aFullTextCandidates.addAll(processCandidateQuery(fullTextQueryMention));
            candidateCache.put(aPair, aFullTextCandidates);
        }
    }

    /*
     * Add all results from a query to a set of candidates
     */
    private Set<CandidateEntity> processCandidateQuery(TupleQuery aTupleQuery)
    {
        Set<CandidateEntity> candidates = new HashSet<>();
        try (TupleQueryResult entityResult = aTupleQuery.evaluate()) {
            while (entityResult.hasNext()) {
                BindingSet solution = entityResult.next();
                Value e2 = solution.getValue("e2");
                Value label = solution.getValue("label");
                Value altLabel = solution.getValue("altLabel");
                Value description = solution.getValue("description");

                CandidateEntity newEntity = new CandidateEntity(
                    (e2 != null) ? e2.stringValue() : "",
                    (label != null) ? label.stringValue() : "",
                    (altLabel != null) ? altLabel.stringValue() :
                        // Exact matching does not use altLabel
                        (label != null) ? label.stringValue() : "",
                    (description != null) ? description.stringValue() : "");

                candidates.add(newEntity);
            }
        }
        return candidates;
    }

    /*
     * Finds the position of a mention in a given sentence and returns the corresponding tokens of
     * the mention with <mentionContextSize> tokens before and <mentionContextSize> after the
     * mention
     *
     */
    private List<Token> getMentionContext(Sentence aSentence, List<String> aMention,
        int mentionContextSize)
    {
        List<Token> mentionSentence = new ArrayList<>(
            JCasUtil.selectCovered(Token.class, aSentence));

        int start = 0, end = 0;
        int j = 0;

        // Loop until mention end was found or sentence ends
        done: while (j < mentionSentence.size()) {

            // Go to the position where the mention starts in the sentence
            for (int i = 0; i < aMention.size(); i++) {

                // is the word done? i-th word of mention contained in j-th token of sentence?
                if (!mentionSentence.get(j).getCoveredText().toLowerCase(Locale.ENGLISH)
                    .contains(aMention.get(i))) {
                    break;
                }

                // if this was the last word of mention, end loop
                if (i == aMention.size() - 1) {
                    start = j - (aMention.size() - 1) - mentionContextSize;
                    end = j + mentionContextSize + 1;
                    break done;
                } else {
                    j++;
                }
            }
            j++;
        }


        if (start == end) {
            logger.error("Mention not found in sentence!");
            return mentionSentence;
        }
        if (start < 0) {
            start = 0;
        }
        if (end > mentionSentence.size()) {
            end = mentionSentence.size();
        }
        return mentionSentence.subList(start, end);
    }

    /*
     * This method does the actual ranking of the candidate entities.
     * First the candidates from full-text matching are sorted by frequency cutoff after a
     * threshold because they are more numerous.
     * Then the candidates from exact matching are added and sorted by multiple keys.
     */
    private List<CandidateEntity> rankCandidates(KnowledgeBase aKB, String aTypedString,
        String mention, Set<CandidateEntity> aCandidatesExact,
        Set<CandidateEntity> aCandidatesFullText, JCas aJCas, int aBegin)
    {
        Sentence mentionSentence = getMentionSentence(aJCas, aBegin);
        Validate.notNull(mentionSentence, "Mention sentence could not be determined.");

        List<String> splitMention = Arrays.asList(mention.split(" "));
        List<Token> mentionContext = getMentionContext(mentionSentence, splitMention,
            properties.getMentionContextSize());

        Set<String> sentenceContentTokens = new HashSet<>();
        for (Token t : JCasUtil.selectCovered(Token.class, mentionSentence)) {
            boolean isNotPartOfMention = !splitMention.contains(t.getCoveredText());
            boolean isNotStopword = (stopwords == null) || (stopwords != null && !stopwords
                .contains(t.getCoveredText().toLowerCase(Locale.ENGLISH)));
            if (isNotPartOfMention && isNotStopword) {
                sentenceContentTokens.add(t.getCoveredText().toLowerCase(Locale.ENGLISH));
            }
        }

        // Set frequency
        aCandidatesFullText.forEach(l -> {
            String wikidataId = l.getIRI().replace(WIKIDATA_PREFIX, "");

            if (entityFrequencyMap != null && entityFrequencyMap.get(wikidataId) != null) {
                l.setFrequency(entityFrequencyMap.get(wikidataId));
            }

        });

        // Sort full-text matching candidates by frequency and do cutoff by a threshold
        List<CandidateEntity> result = sortByFrequency(new ArrayList<>(aCandidatesFullText))
            .stream().limit(properties.getCandidateFrequencyThreshold())
            .collect(Collectors.toList());

        // Add exact matching candidates
        result.addAll(aCandidatesExact);

        // Set the feature values
        result.parallelStream().forEach(l -> {
            String wikidataId = l.getIRI().replace(WIKIDATA_PREFIX, "");
            
            l.setIdRank(Math.log(Double.parseDouble(wikidataId.substring(1))));

            String altLabel = l.getAltLabel();
            LevenshteinDistance lev = new LevenshteinDistance();
            l.setLevMatchLabel(lev.apply(mention, altLabel));
            l.setLevContext(lev.apply(tokensToString(mentionContext), altLabel));
            l.setLevTypedString(lev.apply(aTypedString, altLabel));

            SemanticSignature sig = getSemanticSignature(aKB, wikidataId);
            Set<String> relatedEntities = sig.getRelatedEntities();
            Set<String> signatureOverlap = new HashSet<>();
            for (String entityLabel : relatedEntities) {
                for (String token: entityLabel.split(" ")) {
                    if (sentenceContentTokens.contains(token)) {
                        signatureOverlap.add(entityLabel);
                        break;
                    }
                }
            }
            l.setSignatureOverlap(signatureOverlap);
            l.setSignatureOverlapScore(signatureOverlap.size());
            l.setNumRelatedRelations(
                (sig.getRelatedRelations() != null) ? sig.getRelatedRelations().size() : 0);
        });

        // Do the main ranking
        result = sortCandidates(result);
        return result;
    }

    /*
     * Sort candidates by frequency in descending order.
     */
    private List<CandidateEntity> sortByFrequency(List<CandidateEntity> candidates)
    {
        candidates.sort((e1, e2) ->
            Comparator.comparingInt(CandidateEntity::getFrequency)
                .reversed().compare(e1, e2));
        return candidates;
    }

    /*
     * Sort candidates by multiple keys.
     * The edit distance between typed string and label is given high importance
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
            .append(e1.getLevTypedString(), e2.getLevTypedString())
            .append(e2.getSignatureOverlapScore(), e1.getSignatureOverlapScore())
            .append(e1.getLevContext() + e1.getLevMatchLabel(),
                e2.getLevContext() + e2.getLevMatchLabel())
            .append(e2.getFrequency(), e1.getFrequency())
            .append(e2.getNumRelatedRelations(), e1.getNumRelatedRelations())
            .append(e1.getIdRank(), e2.getIdRank()).toComparison());
        return candidates;
    }

    /*
     * Concatenates the covered text of a list of Tokens to a String.
     */
    private String tokensToString(List<Token> aSentence)
    {
        StringJoiner joiner = new StringJoiner(" ");
        for (Token t : aSentence) {
            joiner.add(t.getCoveredText());
        }
        return joiner.toString().substring(0, joiner.length() - 1);
    }

    /*
     * Retrieves the semantic signature of an entity. See documentation of SemanticSignature class.
     */
    private SemanticSignature getSemanticSignature(KnowledgeBase aKB, String aWikidataId)
    {
        ImmutablePair<Project, String> pair = new ImmutablePair<>(aKB.getProject(), aWikidataId);

        if (semanticSignatureCache.containsKey(pair)) {
            return semanticSignatureCache.get(pair);
        }

        Set<String> relatedRelations = new HashSet<>();
        Set<String> relatedEntities = new HashSet<>();
        try (RepositoryConnection conn = kbService.getConnection(aKB)) {
            TupleQuery query = QueryUtil.generateSemanticSignatureQuery(conn, aWikidataId,
                properties.getSignatureQueryLimit());
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
                logger.error("could not get semantic signature", e);
            }
        }

        SemanticSignature ss = new SemanticSignature(relatedEntities, relatedRelations);
        semanticSignatureCache.put(pair, ss);
        return ss;
    }

    /**
     * Given a mention in the text, this method returns a list of ranked candidate entities
     * generated from a Knowledge Base.
     *
     * The candidates are retrieved in two separate queries, because of the higher number of results
     * returned by full-text matching, which are filtered first.
     * To not possible lose any of the candidates from the exact matching results,
     * the latter are added to the ranking afterwards and given top priority.
     *
     * @param aKB the KB used to generate candidates.
     * @param aTypedString What the user has typed so far in the text field. Might be null.
     * @param aMention Marked Surface form of an entity to be linked.
     * @param aMentionBeginOffset the offset where the mention begins in the text.
     * @param aJcas used to extract information about mention sentence tokens.
     * @return a ranked list of entities.
     */
    public List<KBHandle> disambiguate(KnowledgeBase aKB, String aTypedString, String
        aMention, int aMentionBeginOffset, JCas aJcas)
    {
        long startTime = System.currentTimeMillis();

        Set<CandidateEntity> candidatesExact = retrieveCandidatesExact(aKB, aTypedString, aMention);

        Set<CandidateEntity> candidatesFullText = new HashSet<>();
        if (!aTypedString.isEmpty()) {
            candidatesFullText = retrieveCandidatesFullText(aKB, aTypedString, aMention);
        }

        long afterRetrieval = System.currentTimeMillis();

        logger
            .debug("It took [{}] ms to retrieve candidates for mention [{}] and typed string [{}]",
                afterRetrieval - startTime, aMention, aTypedString);

        List<CandidateEntity> rankedCandidates = rankCandidates(aKB, aTypedString, aMention,
            candidatesExact, candidatesFullText, aJcas, aMentionBeginOffset);

        logger
            .debug("It took [{}] ms to rank candidates for mention [{}] and typed string [{}]",
                System.currentTimeMillis() - afterRetrieval, aMention, aTypedString);


        return rankedCandidates.stream()
            .map(c -> new KBHandle(c.getIRI(), c.getLabel(), c.getDescription()))
            .distinct()
            .limit(properties.getCandidateDisplayLimit())
            .filter(h -> h.getIdentifier().contains(":"))
            .collect(Collectors.toList());
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
        for (Map.Entry<ImmutablePair<Project, String>, Set<CandidateEntity>> pair :
            candidateCache.entrySet()) {
            if (pair.getKey().getLeft().equals(aEvent.getProject())) {
                candidateCache.remove(pair.getKey());
            }
        }
        for (Map.Entry<ImmutablePair<Project, String>, SemanticSignature> pair :
            semanticSignatureCache.entrySet()) {
            if (pair.getKey().getLeft().equals(aEvent.getProject())) {
                semanticSignatureCache.remove(pair.getKey());
            }
        }
    }

}
