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

    /*
     * Generate a set of candidate entities from a Knowledge Base for a mention.
     * It only contains entities which are instances of a pre-defined concept.
     */
    private Set<CandidateEntity> generateCandidates(KnowledgeBase aKB, String aMention)
    {
        if (aMention == null || aMention.isEmpty()) {
            return Collections.emptySet();
        }

        Set<CandidateEntity> candidates = new HashSet<>();
        List<String> mentionList = Arrays.asList(aMention.split(" "));

        // Remove any character that is not a letter
        mentionList = mentionList.stream().map(m -> m.replaceAll("[^\\p{L}^\\d]", ""))
            .collect(Collectors.toList());

        if (stopwords != null) {
            if (stopwords.containsAll(mentionList)) {
                logger.error("Mention [{}] consists of stopwords only - returning.", aMention);
                return Collections.emptySet();
            }
        }

        String processedMention = String.join(" ", mentionList);
        if (processedMention.isEmpty()) {
            logger.error("Mention is empty!");
            return Collections.emptySet();
        }

        ImmutablePair<Project, String> pair = new ImmutablePair<>(aKB.getProject(), processedMention);
        if (candidateCache.containsKey(pair)) {
            return candidateCache.get(pair);
        }


        try (RepositoryConnection conn = kbService.getConnection(aKB)) {
            TupleQuery query = QueryUtil.generateCandidateQuery(conn, processedMention,
                properties.getCandidateQueryLimit(), aKB.getDescriptionIri());
            try (TupleQueryResult entityResult = query.evaluate()) {
                while (entityResult.hasNext()) {
                    BindingSet solution = entityResult.next();
                    Value e2 = solution.getValue("e2");
                    Value label = solution.getValue("label");
                    Value altLabel = solution.getValue("altLabel");
                    Value description = solution.getValue("description");

                    CandidateEntity newEntity = new CandidateEntity(
                        (e2 != null) ? e2.stringValue() : "",
                        (label != null) ? label.stringValue() : "",
                        (altLabel != null) ? altLabel.stringValue() : "",
                        (description != null) ? description.stringValue() : "");

                    candidates.add(newEntity);
                }
            }
        }
        catch (QueryEvaluationException e) {
            logger.error("Query evaluation was unsuccessful: ", e);
        }

        if (candidates.isEmpty()) {
            String[] split = processedMention.split(" ");
            if (split.length > 1) {
                for (String s : split) {
                    candidates.addAll(generateCandidates(aKB, s));
                }
            }
        }

        candidateCache.put(pair, candidates);
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
     * This method does the actual ranking of the candidate entity set.
     * It returns the candidates by descending probability.
     */
    private List<CandidateEntity> rankCandidates(KnowledgeBase aKB, String mention,
            Set<CandidateEntity> candidates, JCas aJCas, int aBegin)
    {
        long startTime = System.currentTimeMillis();

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

        candidates.forEach(l -> {
            String wikidataId = l.getIRI().replace(WIKIDATA_PREFIX, "");

            if (entityFrequencyMap != null && entityFrequencyMap.get(wikidataId) != null) {
                l.setFrequency(entityFrequencyMap.get(wikidataId));
            }
            else {
                l.setFrequency(0);
            }

        });

        List<CandidateEntity> result = sortByFrequency(new ArrayList<>(candidates)).stream()
            .limit(properties.getCandidateFrequencyThreshold()).collect(Collectors.toList());

        result.parallelStream().forEach(l -> {
            String wikidataId = l.getIRI().replace(WIKIDATA_PREFIX, "");
            
            l.setIdRank(Math.log(Double.parseDouble(wikidataId.substring(1))));
            String altLabel = l.getAltLabel().toLowerCase(Locale.ENGLISH);
            LevenshteinDistance lev = new LevenshteinDistance();
            l.setLevMatchLabel(lev.apply(mention, altLabel));
            l.setLevContext(lev.apply(tokensToString(mentionContext), altLabel));

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
        result = sortCandidates(result);
        logger.debug("It took [{}] ms to rank candidates",
            System.currentTimeMillis() - startTime);
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
     * A high signature overlap score is preferred.
     * A low edit distance is preferred.
     * A high entity frequency is preferred.
     * A high number of related relations is preferred.
     * A low wikidata ID rank is preferred.
     */
    private List<CandidateEntity> sortCandidates(List<CandidateEntity> candidates)
    {
        candidates.sort((e1, e2) -> new CompareToBuilder()
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
     * generated from a Knowledge Base. It only contains entities which are instances of a
     * pre-defined concept.
     *
     * @param aKB the KB used to generate candidates
     * @param aTypedString What the user has typed so far in the text field. Might be null.
     * @param aMention AnnotatorState, used to get information about what surface form was
     *                     marked
     * @param aMentionBeginOffset the offset where the mention begins in the text
     * @param aJcas used to extract information about mention sentence
     *                       tokens
     * @return ranked list of entities, starting with the most probable entity
     */
    public List<KBHandle> disambiguate(KnowledgeBase aKB, String aTypedString, String
        aMention, int aMentionBeginOffset, JCas aJcas)
    {
        long startTime = System.currentTimeMillis();

        Set<CandidateEntity> candidates = new HashSet<>();

        aMention = aMention.toLowerCase(Locale.ENGLISH);

        if (aTypedString != null) {
            aTypedString = aTypedString.toLowerCase(Locale.ENGLISH);
            if (!aMention.startsWith(aTypedString)) {
                candidates.addAll(generateCandidates(aKB, aTypedString));
                logger.debug("It took [{}] ms to retrieve candidates for typed string [{}]", System
                    .currentTimeMillis() - startTime, aTypedString);
            }
            else {
                candidates.addAll(generateCandidates(aKB, aMention));
                logger.debug("It took [{}] ms to retrieve candidates for mention [{}]", System
                    .currentTimeMillis() - startTime, aMention);
            }
        }
        else {
            candidates.addAll(generateCandidates(aKB, aMention));
            logger.debug("It took [{}] ms to retrieve candidates for mention [{}]", System
                .currentTimeMillis() - startTime, aMention);
        }
        
        List<CandidateEntity> rankedCandidates = rankCandidates(aKB, aMention, candidates, aJcas,
            aMentionBeginOffset);

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
