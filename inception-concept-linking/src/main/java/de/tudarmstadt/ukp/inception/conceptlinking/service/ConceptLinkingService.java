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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.performance.Stopwatch;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import edu.stanford.nlp.util.StringUtils;
import de.tudarmstadt.ukp.inception.conceptlinking.model.Entity;
import de.tudarmstadt.ukp.inception.conceptlinking.model.Property;
import de.tudarmstadt.ukp.inception.conceptlinking.model.SemanticSignature;
import de.tudarmstadt.ukp.inception.conceptlinking.util.QueryUtil;
import de.tudarmstadt.ukp.inception.conceptlinking.util.Utils;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseExtension;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.Entity;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

@Component
public class ConceptLinkingService
    implements KnowledgeBaseExtension
{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private @Resource KnowledgeBaseService kbService;
    private @Resource SessionRegistry sessionRegistry;

    private final String[] PUNCTUATION_VALUES
        = new String[] { "``", "''", "(", ")", ",", ".", ":", "--" };

    private final Set<String> punctuations = new HashSet<>(Arrays.asList(PUNCTUATION_VALUES));

    private Set<String> stopwords;

    private Map<String, Integer> entityFrequencyMap;

    private Set<String> propertyBlacklist;

    private Set<String> typeBlacklist = new HashSet<>(Arrays
        .asList("commonsmedia", "external-id", "globe-coordinate", "math", "monolingualtext",
            "quantity", "string", "url", "wikibase-property"));

    private Map<String, Property> propertyWithLabels;

    private Map<String, ConceptLinkingUserState> states = new ConcurrentHashMap<>();

    @PostConstruct
    public void init()
    {
        DefaultResourceLoader loader = new DefaultResourceLoader();

        org.springframework.core.io.Resource stopwordsResource = loader
            .getResource("classpath:stopwords-de.txt");
        stopwords = Utils.readFile(stopwordsResource);

        org.springframework.core.io.Resource entityFrequencyMapResource = loader
            .getResource("classpath:wikidata_entity_freqs.map");
        entityFrequencyMap = Utils.loadEntityFrequencyMap(entityFrequencyMapResource);

        org.springframework.core.io.Resource propertyBlacklistResource = loader
            .getResource("classpath:property_blacklist.txt");
        propertyBlacklist = Utils.loadPropertyBlacklist(propertyBlacklistResource);

        org.springframework.core.io.Resource propertyWithLabelsResource = loader
            .getResource("classpath:properties_with_labels.txt");
        propertyWithLabels = Utils.loadPropertyLabels(propertyWithLabelsResource);
    }

    @Override
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
     * TODO lemmatize the mention if no candidates could be generated
     */
    private Set<Entity> generateCandidates(KnowledgeBase aKB, String aMention, IRI aConceptIri,
            String aLanguage)
    {
        double startTime = System.currentTimeMillis();
        Set<Entity> candidates = new HashSet<>();
        List<String> mentionArray = Arrays.asList(aMention.split(" "));

        ListIterator<String> it = mentionArray.listIterator();
        String current;
        while (it.hasNext()) {
            current = it.next();
            it.set(current);
            if (punctuations.contains(current)) {
                it.remove();
            }
        }

        boolean onlyStopwords = true;
        ListIterator<String> it2 = mentionArray.listIterator();
        while (it2.hasNext()) {
            current = it2.next();
            it2.set(current);
            if (!stopwords.contains(current)) {
                onlyStopwords = false;
                break;
            }
        }

        if (mentionArray.isEmpty() || onlyStopwords) {
            logger.error("Mention array is empty or consists of stopwords only - returning.");
            throw new IllegalStateException();
        }

        int candidateQueryLimit = 1000;
        String entityQueryString = QueryUtil
            .generateCandidateQuery(mentionArray, candidateQueryLimit, aConceptIri, aLanguage);
        
        try (RepositoryConnection conn = kbService.getConnection(aKB)) {
            TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, entityQueryString);
            try (TupleQueryResult entityResult = query.evaluate()) {
                while (entityResult.hasNext()) {
                    BindingSet solution = entityResult.next();
                    Value e2 = solution.getValue("e2");
                    Value label = solution.getValue("label");
                    Value altLabel = solution.getValue("altLabel");

                    Entity newEntity = new Entity((e2 != null) ? e2.stringValue() : "",
                                         (label != null) ? label.stringValue() : "",
                                      (altLabel != null) ? altLabel.stringValue() : "");

                    candidates.add(newEntity);
                }
            }
            catch (QueryEvaluationException e) {
                throw new QueryEvaluationException(e);
            }
            catch (NullPointerException e) {
                throw new NullPointerException();
            }
        }

        if (candidates.isEmpty()) {
            String[] split = aMention.split(" ");
            if (split.length > 1) {
                for (String s : split) {
                    candidates.addAll(generateCandidates(aKB, s, null, aLanguage));
                }
            }
        }
        logger.debug(System.currentTimeMillis() - startTime + "ms for generateCandidates method.");
        return candidates;
    }

    /*
     * Finds the position of a mention in a given sentence and returns the corresponding tokens of
     * the mention with <mentionContextSize> tokens before and <mentionContextSize> after the
     * mention
     *
     * FIXME there could also be multiple mentions in a sentence
     */
    private List<Token> getMentionContext(Sentence aSentence, List<String> aMention,
        int mentionContextSize)
    {
        List<Token> mentionSentence = new ArrayList<>();
        mentionSentence.addAll(JCasUtil.selectCovered(Token.class, aSentence));

        int start = 0, end = 0;
        int j = 0;
        boolean done = false;

        // Loop until mention end was found or sentence ends
        while (!done && j < mentionSentence.size()) {

            // Go to the position where the mention starts in the sentence
            for (int i = 0; i < aMention.size(); i++) {

                // is the word done? i-th word of mention contained in j-th token of sentence?
                if (!mentionSentence.get(j).getCoveredText()
                    .contains(aMention.get(i))) {
                    break;
                }

                // if this was the last word of mention, end loop
                if (i == aMention.size() - 1) {
                    start = j - (aMention.size() - 1) - mentionContextSize;
                    end = j + mentionContextSize + 1;
                    done = true;
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
    private List<Entity> rankCandidates(KnowledgeBase aKB, String mention,
            Set<Entity> candidates, JCas aJCas, int aBegin)
    {
        int mentionContextSize = 5;
        Sentence mentionSentence = getMentionSentence(aJCas, aBegin);
        if (mentionSentence == null) {
            throw new IllegalStateException();
        }
        List<String> splitMention = Arrays.asList(mention.split(" "));
        List<Token> mentionContext = getMentionContext(mentionSentence, splitMention,
                mentionContextSize);
        
        // TODO and t['ner'] not in {"ORDINAL", "MONEY", "TIME", "PERCENTAGE"}} \
        Set<String> sentenceContentTokens = new HashSet<>();
        for (Token t : JCasUtil.selectCovered(Token.class, mentionSentence)) {
            if (t.getPosValue() != null) {
                if ((t.getPosValue().startsWith("V")
                        || t.getPosValue().startsWith("N")
                        || t.getPosValue().startsWith("J"))
                    && !splitMention.contains(t.getCoveredText())
                    && (!Objects.requireNonNull(stopwords).contains(t.getCoveredText())
                        || !splitMention.contains(t.getCoveredText()))) {
                    sentenceContentTokens.add(t.getCoveredText());
                }
            }
        }

        double startLoop = System.currentTimeMillis();

        candidates.parallelStream().forEach(l -> {
            String wikidataId = l.getIRI().replace("http://www.wikidata.org/entity/", "");

            if (Objects.requireNonNull(entityFrequencyMap).get(wikidataId) != null) {
                l.setFrequency(entityFrequencyMap.get(wikidataId));
            }
            else {
                l.setFrequency(0);
            }
        });
        List<Entity> result = sortByFrequency(new ArrayList<>((candidates)));
        if (result.size() > 100) {
            result = result.subList(0, 100);
        }
        result.parallelStream().forEach( l -> {
            String wikidataId = l.getIRI().replace("http://www.wikidata.org/entity/", "");
            l.setIdRank(Math.log(Double.parseDouble(wikidataId.substring(1))));
            String altLabel = l.getAltLabel().toLowerCase();
            LevenshteinDistance lev = new LevenshteinDistance();
            l.setLevMatchLabel(lev.apply(mention, altLabel));
            l.setLevContext(lev.apply(tokensToString(mentionContext), altLabel));

            SemanticSignature sig = getSemanticSignature(aKB, wikidataId);
            Set<String> relatedEntities = sig.getRelatedEntities();
            Set<String> signatureOverlap = new HashSet<>();
            for (String s : relatedEntities) {
                if (sentenceContentTokens.contains(s)) {
                    signatureOverlap.add(s);
                }
            }
            l.setSignatureOverlapScore(splitMention.size() + signatureOverlap.size());
            l.setNumRelatedRelations(
                (sig.getRelatedRelations() != null) ? sig.getRelatedRelations().size() : 0);
        });
        result = sortCandidates(new ArrayList<>(candidates));
        logger.debug(System.currentTimeMillis() - startLoop + "ms until end loop.");
        return result;
    }

    /*
     * Sort candidates by frequency in descending order.
     */
    private List<Entity> sortByFrequency(List<Entity> candidates)
    {
        candidates.sort((e1, e2) -> new org.apache.commons.lang.builder.CompareToBuilder()
            .append(-e1.getFrequency(), -e2.getFrequency()).toComparison());
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
    private List<Entity> sortCandidates(List<Entity> candidates)
    {
        candidates.sort((e1, e2) -> new org.apache.commons.lang.builder.CompareToBuilder()
            .append(-e1.getSignatureOverlapScore(), -e2.getSignatureOverlapScore())
            .append(e1.getLevContext() + e1.getLevMatchLabel(),
                e2.getLevContext() + e2.getLevMatchLabel())
            .append(-e1.getFrequency(), -e2.getFrequency())
            .append(-e1.getNumRelatedRelations(), -e2.getNumRelatedRelations())
            .append(e1.getIdRank(), e2.getIdRank()).toComparison());
        return candidates;
    }

    /*
     * Concatenates the covered text of a list of Tokens to a String.
     */
    private String tokensToString(List<Token> aSentence)
    {
        StringBuilder builder = new StringBuilder();
        for (Token t : aSentence) {
            builder.append(t.getCoveredText()).append(" ");
        }
        return builder.substring(0, builder.length() - 1);
    }

    /*
     * Retrieves the semantic signature of an entity. See documentation of SemanticSignature class.
     */
    private SemanticSignature getSemanticSignature(KnowledgeBase aKB, String aWikidataId)
    {
        Set<String> relatedRelations = new HashSet<>();
        Set<String> relatedEntities = new HashSet<>();
        int signatureQueryLimit = 100;
        String queryString
            = QueryUtil.generateSemanticSignatureQuery(aWikidataId, signatureQueryLimit);
        try (RepositoryConnection conn = kbService.getConnection(aKB)) {
            TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    BindingSet sol = result.next();
                    String propertyString = sol.getValue("p").stringValue();
                    String labelString = sol.getValue("label").stringValue();
                    Property property = Objects.requireNonNull(propertyWithLabels).get(labelString);
                    int frequencyThreshold = 0;
                    if (Objects.requireNonNull(propertyBlacklist).contains(propertyString) || (
                        property != null && typeBlacklist.contains(property.getType())) || (
                        property != null && property.getFreq() < frequencyThreshold)) {
                        continue;
                    }
                    relatedEntities.add(labelString);
                    relatedRelations.add(propertyString);
                }
            }
            catch (Exception e) {
                logger.error("could not get semantic signature", e);
            }
        }
        
        return new SemanticSignature(relatedEntities, relatedRelations);
    }

    /**
     * Given a mention in the text, this method returns a list of ranked candidate entities
     * generated from a Knowledge Base. It only contains entities which are instances of a
     * pre-defined concept.
     *
     * @param aKB the KB used to generate candidates
     * @param aConceptIri the concept of which instances should be generated as candidates
     * @param aState AnnotatorState, used to get information about what surface form was marked
     * @param aActionHandler contains JCas, used to extract information about mention sentence
     *                       tokens
     * @return ranked list of entities, starting with the most probable entity
     */
    public List<Entity> disambiguate (KnowledgeBase aKB, IRI aConceptIri,
            AnnotatorState aState, AnnotationActionHandler aActionHandler)
    {
        List<Entity> candidates = new ArrayList<>();
        
        String mention = aState.getSelection().getText();
        User user = aState.getUser();

        ConceptLinkingUserState userState = getState(user.getUsername());
        String language = userState.getLanguage();

        try {
            JCas jCas = aActionHandler.getEditorCas();

            candidates = rankCandidates(aKB, mention,
                    generateCandidates(aKB, mention, aConceptIri, language), jCas,
                    aState.getSelection().getBegin());
        }
        catch (IOException e) {
            logger.error("Cannot get JCas", e);
        }

        return candidates;

    }

    /*
     * Clear user state when session ends
     */
    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onApplicationEvent(SessionDestroyedEvent event)
    {
        SessionInformation info = sessionRegistry.getSessionInformation(event.getId());
        // Could be an anonymous session without information.
        if (info != null) {
            String username = (String) info.getPrincipal();
            clearState(username);
        }
    }

    /*
     * Holds session-specific settings
     */
    private ConceptLinkingUserState getState(String aUsername)
    {
        synchronized (states) {
            ConceptLinkingUserState state;
            state = states.get(aUsername);
            if (state == null) {
                state = new ConceptLinkingUserState();
                states.put(aUsername, state);
            }
            return state;
        }
    }
    
    private void clearState(String aUsername)
    {
        synchronized (states) {
            states.remove(aUsername);
        }
    }
       
    private class ConceptLinkingUserState
    {
        private String language = "en";
        
        private String getLanguage()
        {
            return language;
        }

        private void setLanguage(String aLanguage)
        {
            language = aLanguage;
        }
    }
}
