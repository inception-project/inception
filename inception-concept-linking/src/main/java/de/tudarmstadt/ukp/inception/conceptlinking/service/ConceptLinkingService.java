package de.tudarmstadt.ukp.inception.conceptlinking.service;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
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
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterAnnotationUpdateEvent;
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
    
    private AnalysisEngine pipeline;
    private AnalysisEngine coreNlpPipeline;
    
    private final static String WORKING_DIRECTORY = "workspace/inception-application/inception-concept-linking/";
    
    private final String[] PUNCTUATION_VALUES 
            = new String[] { "``", "''", "(", ")", ",", ".", ":", "--" };

    private final Set<String> punctuations = new HashSet<>(
            Arrays.asList(PUNCTUATION_VALUES));

    private final Set<String> stopwords 
            = Utils.readFile(WORKING_DIRECTORY + "resources/stopwords-de.txt");

    private int candidateQueryLimit = 200;
    private int signatureQueryLimit = 10;
    
    private final Map<String, Integer> entityFrequencyMap
            = Utils.loadEntityFrequencyMap(WORKING_DIRECTORY + "resources/wikidata_entity_freqs.map");

    private Map<String, ConceptLinkingUserState> states = new ConcurrentHashMap<>();
    
    @Override
    public String getBeanName()
    {
        return "conceptLinkingService";
    }
    
    @PostConstruct
    public void init()
    {
        AnalysisEngineDescription desc;
        try {
            desc = createEngineDescription(
                    createEngineDescription(Stopwatch.class, 
                            Stopwatch.PARAM_TIMER_NAME, "stanfordNlpTimer"),
                    createEngineDescription(StanfordSegmenter.class),
                    createEngineDescription(StanfordPosTagger.class,
                            StanfordSegmenter.PARAM_LANGUAGE_FALLBACK, "en"),
                    createEngineDescription(
                            Stopwatch.class,
                            Stopwatch.PARAM_TIMER_NAME, "stanfordNlpTimer",
                            Stopwatch.PARAM_OUTPUT_FILE, "target/stanfordnlpPipeline.txt"
                    ));
            pipeline = AnalysisEngineFactory.createEngine(desc);
            
            desc = createEngineDescription(
                    createEngineDescription(Stopwatch.class, 
                            Stopwatch.PARAM_TIMER_NAME, "coreNlpTimer"),
                    createEngineDescription(CoreNlpSegmenter.class),
                    createEngineDescription(CoreNlpPosTagger.class,
                            CoreNlpSegmenter.PARAM_LANGUAGE, "en"),
                    createEngineDescription(
                            Stopwatch.class,
                            Stopwatch.PARAM_TIMER_NAME, "coreNlpTimer",
                            Stopwatch.PARAM_OUTPUT_FILE, "target/coreNlpPipeline.txt"
                    ));
            coreNlpPipeline = AnalysisEngineFactory.createEngine(desc);
        }
        catch (ResourceInitializationException e) {
            logger.error("Could not create AnalysisEngine!", e);
        }
        
    }

    /*
     * Retrieves the first sentence containing the mention as Tokens
     */
    private synchronized List<Token> getMentionSentence(JCas mentionSentence, String mention, 
            String language)
        throws UIMAException, IOException
    {
        double startTime = System.currentTimeMillis();
        String sentenceText = findMentionSentenceInDoc(mentionSentence, mention);
        mentionSentence.reset();
        mentionSentence.setDocumentText(sentenceText);
        mentionSentence.setDocumentLanguage(language);
        
        pipeline.process(mentionSentence);
        pipeline.collectionProcessComplete();
        logger.debug(System.currentTimeMillis() - startTime + "ms for processing text.");

        for (Sentence s : JCasUtil.select(mentionSentence, Sentence.class)) {
            List<Token> sentence = new LinkedList<>();
            for (Token t : JCasUtil.selectCovered(Token.class, s)) {
                sentence.add(t);
            }
            return sentence;
        }
        logger.info("Could not return mention sentence.");
        throw new IllegalStateException();
    }

    private synchronized List<Token> getMentionSentenceWithCorenlp(JCas mentionSentence, 
            String mention) 
        throws UIMAException, IOException
    {
        double startTime = System.currentTimeMillis();
        String sentenceText = findMentionSentenceInDoc(mentionSentence, mention);
        mentionSentence.reset();
        mentionSentence.setDocumentText(sentenceText);
        mentionSentence.setDocumentLanguage("en");
        
        coreNlpPipeline.process(mentionSentence);
        logger.debug(System.currentTimeMillis() - startTime + "ms for processing text.");

        for (Sentence s : JCasUtil.select(mentionSentence, Sentence.class)) {
            List<Token> sentence = new LinkedList<>();
            for (Token t : JCasUtil.selectCovered(Token.class, s)) {
                sentence.add(t);
            }
            return sentence;
        }
        logger.info("Could not return mention sentence.");
        throw new IllegalStateException();
    }
    
    
    /**
     * Return sentence containing the mention
     * @param docText
     * @param mention
     * @return
     * @throws IOException
     */
    private String findMentionSentenceInDoc(JCas aJcas, String mention) 
            throws IOException
    {    
        // Detecting the sentence
        Collection<Sentence> sentences = JCasUtil.select(aJcas, Sentence.class);

        // Check whether mention occurs in this sentence
        for (Sentence sent : sentences) {
            if (sent.getCoveredText().contains(mention)) {
                return sent.getCoveredText();
            }
        }
        logger.info("Mention " + mention + " could not be found in docText.");
        throw new IllegalStateException();
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
            if (!stopwords.contains(current)) {
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
                    Value anylabel = solution.getValue("anylabel");
                    linkings.add(new Entity((e2 != null) ? e2.toString() : "",
                                         (label != null) ? label.toString() : "",
                                      (anylabel != null) ? anylabel.toString() : ""));
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
     * TODO what happens if there are multiple mentions in a sentence?
     */
    private List<Token> getMentionContext(List<Token> mentionSentence, List<String> mention,
            int mentionContextSize)
    {
        int start = 0, end = 0;
        int j = 0;
        boolean done = false;
        while (done == false && j < mentionSentence.size()) {
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
     * @param taggedText:
     *            the current text as a list of tagged token
     * @return
     * @throws UIMAException
     */
    private List<Entity> computeCandidateScores(KnowledgeBase aKB, String mention, 
            Set<Entity> linkings, JCas jCas, String aLanguage)
        throws UIMAException, IOException
    {
        int mentionContextSize = 2;
        List<Token> mentionSentence = getMentionSentence(jCas, mention, aLanguage);
        if (mentionSentence == null) {
            throw new IllegalStateException();
        }
        List<String> splitMention = Arrays.asList(mention.split(" "));
        List<Token> mentionContext = getMentionContext(mentionSentence, splitMention,
                mentionContextSize);

        // TODO and t['ner'] not in {"ORDINAL", "MONEY", "TIME", "PERCENTAGE"}} \
        Set<String> sentenceContentTokens = new HashSet<>();
        for (Token t : mentionSentence) {
            if ((t.getPos().getPosValue().startsWith("V")
                    || t.getPos().getPosValue().startsWith("N")
                    || t.getPos().getPosValue().startsWith("J"))
                && !splitMention.contains(t.getCoveredText())
                && (!stopwords.contains(t.getCoveredText())
                    || !splitMention.contains(t.getCoveredText()))) {
                sentenceContentTokens.add(t.getCoveredText());
            }
        }

        double startLoop = System.currentTimeMillis();
        
        linkings.parallelStream().forEach( l -> {
            String wikidataId = l.getE2().replace("http://www.wikidata.org/entity/", "");
            String anylabel = l.getAnyLabel().toLowerCase();

            l.setIdRank(Math.log(Double.parseDouble(wikidataId.substring(1))));
            
            if (entityFrequencyMap.get(wikidataId) != null) {
                l.setFrequency(entityFrequencyMap.get(wikidataId));
            } else {
                l.setFrequency(0);
            }
            
            LevenshteinDistance lev = new LevenshteinDistance();
            l.setLevMatchLabel(lev.apply(mention, anylabel).intValue());
            l.setLevSentence(lev.apply(tokensToString(mentionContext), anylabel).intValue());
            l.setNumRelatedRelations(0);

            SemanticSignature sig = getSemanticSignature(aKB, wikidataId);
            Set<String> relatedEntities = sig.getRelatedEntities();
            Set<String> signatureOverlap = new HashSet<>();
            for (String s : relatedEntities) {
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
                        .append(e1.getLevSentence() + e1.getLevMatchLabel(),
                                e2.getLevSentence() + e2.getLevMatchLabel())
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
            builder.append(t.getCoveredText() + " ");
        }
        return builder.toString();
    }

    // TODO filter against blacklist
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
                    relatedEntities.add(sol.getValue("label").stringValue());
                    String p = sol.getValue("p").stringValue();
                    relatedRelations.add(p.substring(0, p.length() - 2));
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

        try {        
            ConceptLinkingUserState userState = getState(user.getUsername());
            JCas jCas = aActionHandler.getEditorCas();
            String language = userState.getLanguage();

            try {
                candidates = computeCandidateScores(aKB, mention,
                        linkMention(aKB, mention, conceptIri, language), jCas, language);
            } catch (IOException | UIMAException e) {
                logger.error("Could not compute candidate scores: ", e);
            }
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
    
    @EventListener
    public void afterAnnotationUpdate(AfterAnnotationUpdateEvent aEvent) throws Exception
    {
        User user = userRepository.get(aEvent.getDocument().getUser());
        ConceptLinkingUserState state = getState(user.getUsername());
        AnnotationDocument annoDoc = aEvent.getDocument();
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
