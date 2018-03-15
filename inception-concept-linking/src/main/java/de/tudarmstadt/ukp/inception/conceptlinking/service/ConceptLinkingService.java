package de.tudarmstadt.ukp.inception.conceptlinking.service;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

import javax.annotation.Resource;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.uima.UIMAException;
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
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.DocumentOpenedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
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

    private @Resource DocumentService docService;
    private @Resource KnowledgeBaseService kbService;
    private @Resource UserDao userRepository;
    private @Resource SessionRegistry sessionRegistry;
    
    private final static String WORKING_DIRECTORY 
        = "workspace/inception-application/inception-concept-linking/";
    
    private final String[] PUNCTUATION_VALUES 
            = new String[] { "``", "''", "(", ")", ",", ".", ":", "--" };

    private final Set<String> punctuations = new HashSet<>(
            Arrays.asList(PUNCTUATION_VALUES));

    private final Set<String> stopwords 
            = Utils.readFile(WORKING_DIRECTORY + "resources/stopwords-de.txt");

    private int candidateQueryLimit = 1000;
    private int signatureQueryLimit = 100;
    
    private final Map<String, Integer> entityFrequencyMap = Utils
            .loadEntityFrequencyMap(WORKING_DIRECTORY + "resources/wikidata_entity_freqs.map");

    private final Set<String> propertyBlacklist = Utils
            .loadPropertyBlacklist(WORKING_DIRECTORY + "resources/property_blacklist.txt");
    
    private final Set<String> typeBlacklist = new HashSet<>(Arrays
        .asList("commonsmedia", "external-id", "globe-coordinate", "math", "monolingualtext",
            "quantity", "string", "url", "wikibase-property"));
    private final Map<String, Property> propertyWithLabels = 
            Utils.loadPropertyLabels(WORKING_DIRECTORY + "resources/properties_with_labels.txt");
    
    private Map<String, ConceptLinkingUserState> states = new ConcurrentHashMap<>();
    
    @Override
    public String getBeanName()
    {
        return "conceptLinkingService";
    }

    /*
     * Retrieves the first sentence containing the mention as Tokens
     */
    private synchronized Sentence getMentionSentence(JCas aJcas, int aBegin)
    {
        return WebAnnoCasUtil.getSentence(aJcas, aBegin);
    }    

    // TODO lemmatization
    private Set<Entity> linkMention(KnowledgeBase aKB, String mention, IRI conceptIri, 
            String aLanguage)
    {
        double startTime = System.currentTimeMillis();
        Set<Entity> linkings = new HashSet<>();
        List<String> mentionArray = Arrays.asList(mention.split(" "));

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
            if (!Objects.requireNonNull(stopwords).contains(current)) {
                onlyStopwords = false;
                break;
            }
        }

        if (mentionArray.isEmpty() || onlyStopwords) {
            logger.error("Mention array is empty or consists of stopwords only - returning.");
            throw new IllegalStateException();
        }

        String entityQueryString = 
                QueryUtil.entityQuery(mentionArray, candidateQueryLimit, conceptIri, aLanguage);
        
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

                    linkings.add(newEntity);
                }
            }
            catch (QueryEvaluationException e) {
                throw new QueryEvaluationException(e);
            }
            catch (NullPointerException e) {
                throw new NullPointerException();
            }
        }

        if (linkings.isEmpty()) {
            String[] split = mention.split(" ");
            if (split.length > 1) {
                for (String s : split) {
                    linkings.addAll(linkMention(aKB, s, null, aLanguage));
                }
            }
        }
        logger.debug(System.currentTimeMillis() - startTime + "ms for linkMention method.");
        return linkings;
    }

    /**
     * Finds the position of a mention in a given sentence and returns the corresponding tokens of
     * the mention with <mentionContextSize> tokens before and <mentionContextSize> after the
     * mention
     * 
     */
    private List<Token> getMentionContext(Sentence sentence, List<String> mention,
            int mentionContextSize)
    {
        List<Token> mentionSentence = new ArrayList<>();
        Collections.addAll(mentionSentence, (Token) JCasUtil.selectCovered(Token.class, sentence));

        int start = 0, end = 0;
        int j = 0;
        boolean done = false;
        while (!done && j < mentionSentence.size()) {
            for (int i = 0; i < mention.size(); i++) {
                if (!mentionSentence.get(j).getCoveredText()
                        .contains(mention.get(i))) {
                    break;
                }
                j++;
                if (i == mention.size() - 1) {
                    start = j - (mention.size() - 1) - mentionContextSize;
                    end = j + mentionContextSize + 1;
                    done = true;
                }
            }
            j++;
        }

        if (start == end) {
            logger.warn("Mention not found in sentence!");
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

    /**
     * The method should compute scores for each candidate linking for the given entity and sort the
     * candidates so that the most probable candidate comes first.
     *
     * @param mention
     * @param linkings
     *            the current text as a list of tagged token
     * @return
     * @throws UIMAException
     */
    private List<Entity> computeCandidateScores(KnowledgeBase aKB, String mention, 
            Set<Entity> linkings, JCas aJCas, int aBegin)
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
        
        linkings.parallelStream().forEach( l -> {
            String wikidataId = l.getIRI().replace("http://www.wikidata.org/entity/", "");
            String altLabel = l.getAnyLabel().toLowerCase();

            l.setIdRank(Math.log(Double.parseDouble(wikidataId.substring(1))));

            if (Objects.requireNonNull(entityFrequencyMap).get(wikidataId) != null) {
                l.setFrequency(entityFrequencyMap.get(wikidataId));
            } else {
                l.setFrequency(0);
            }

            LevenshteinDistance lev = new LevenshteinDistance();
            l.setLevMatchLabel(lev.apply(mention, altLabel));
            l.setLevContext(lev.apply(tokensToString(mentionContext), altLabel));
            l.setNumRelatedRelations(0);

            SemanticSignature sig = getSemanticSignature(aKB, wikidataId);
            Set<String> relatedEntities = sig.getRelatedEntities();
            Set<String> signatureOverlap = new HashSet<>();
            for (String s : relatedEntities) {
                // if(sentenceContentTokens.contains(s) || s.contains(sentenceContentTokens) {
                if (sentenceContentTokens.contains(s)) {
                    signatureOverlap.add(s);
                }
            }
            l.setSignatureOverlapScore(splitMention.size() + signatureOverlap.size());
            l.setNumRelatedRelations(sig.getRelatedRelations().size());
        });
        List<Entity> result = sortCandidates(new ArrayList<>(linkings));
        logger.debug(System.currentTimeMillis() - startLoop + "ms until end loop.");
        return result;
    }

    private static List<Entity> sortCandidates(List<Entity> candidates)
    {
        Collections.sort(candidates, new Comparator<Entity>()
        {
            @Override
            public int compare(Entity e1, Entity e2)
            {
                return new org.apache.commons.lang.builder.CompareToBuilder()
                        .append(-e1.getSignatureOverlapScore(), -e2.getSignatureOverlapScore())
                        .append(e1.getLevContext() + e1.getLevMatchLabel(),
                                e2.getLevContext() + e2.getLevMatchLabel())
                        .append(-e1.getFrequency(), -e2.getFrequency())
                        .append(-e1.getNumRelatedRelations(), -e2.getNumRelatedRelations())
                        .append(e1.getIdRank(), e2.getIdRank()).toComparison();
            }
        });
        return candidates;
    }

    private String tokensToString(List<Token> sentence)
    {
        StringBuilder builder = new StringBuilder();
        for (Token t : sentence) {
            builder.append(t.getCoveredText()).append(" ");
        }
        return builder.toString();
    }

    private SemanticSignature getSemanticSignature(KnowledgeBase aKB, String wikidataId)
    {
        Set<String> relatedRelations = new HashSet<>();
        Set<String> relatedEntities = new HashSet<>();
        String queryString = QueryUtil.semanticSignatureQuery(wikidataId, signatureQueryLimit);
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

    public List<Entity> disambiguate (KnowledgeBase aKB, IRI conceptIri, 
            AnnotatorState aState, AnnotationActionHandler aActionHandler)
    {
        List<Entity> candidates = new ArrayList<>();
        
        String mention = aState.getSelection().getText();
        User user = aState.getUser();

        ConceptLinkingUserState userState = getState(user.getUsername());
        String language = userState.getLanguage();

        try {
            JCas jCas = aActionHandler.getEditorCas();

            candidates = computeCandidateScores(aKB, mention,
                    linkMention(aKB, mention, conceptIri, language), jCas,
                    aState.getSelection().getBegin());
        }
        catch (IOException e) {
            logger.error("Cannot get JCas", e);
        }

        return candidates;

    }

    public int getCandidateQueryLimit()
    {
        return candidateQueryLimit;
    }

    public int getSignatureQueryLimit()
    {
        return signatureQueryLimit;
    }

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
    
    @EventListener
    private void onDocumentOpen(DocumentOpenedEvent aEvent) throws Exception
    {
        User user = userRepository.get(aEvent.getUser());
        ConceptLinkingUserState state = getState(user.getUsername());
        SourceDocument doc = aEvent.getDocument();
        AnnotationDocument annoDoc = docService.createOrGetAnnotationDocument(doc, user);
        JCas jCas;
        try {
            jCas = docService.readAnnotationCas(annoDoc);
            state.setJCas(jCas);
        }
        catch (IOException e) {
            logger.error("Cannot read annotation CAS.", e);
        }
    }
    
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
       
    private static class ConceptLinkingUserState
    {
        private String language = "en";
        private JCas jCas;

        
        private String getLanguage()
        {
            return language;
        }

        private void setLanguage(String aLanguage)
        {
            language = aLanguage;
        }

        private JCas getJcas() {
            return jCas;
        }
        
        private void setJCas(JCas aJcas) {
            jCas = aJcas;
        }
    }
}
