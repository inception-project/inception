package evaluation;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.Math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.LevenshteinDistance;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import de.dailab.irml.gerned.NewsReader;
import de.dailab.irml.gerned.QueriesReader;
import de.dailab.irml.gerned.data.Query;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

public class LinkMention {

  private static Logger logger = LoggerFactory.getLogger(LinkMention.class);
  
  private static RepositoryConnection conn;
  
  private final static String[] PUNCTUATION_VALUES 
          = new String[] {"``", "''", "(", ")", ",", ".", ":", "--"};
  
  private static final Set<String> punctuations 
          = new HashSet<>(Arrays.asList(PUNCTUATION_VALUES));
  
  private static final Set<String> stopwords 
          = Utils.readFile("resources/stopwords-de.txt");

  private static String SPARQL_ENDPOINT = 
		  "http://knowledgebase.ukp.informatik.tu-darmstadt.de:8890/sparql";
//		  "https://query.wikidata.org/sparql?query=SPARQL";
  public static void main(String[] args) {

    initializeConnection();

    QueriesReader reader = new QueriesReader();
    File answersFile = new File("../gerned/dataset/ANY_german_queries_with_answers.xml");
    List<Query> queries = reader.readQueries(answersFile);

    System.out.println("Total number of queries: "+ queries.size());
    Map<String, Set<Entity>> entities = new HashMap<>();
    
    int counter = 0;
    int coverageCounter = 0;
    int i = 1;
    
    for (Query query : queries) {
      String docText = NewsReader.readFile("../gerned/dataset/news/"+ query.getDocid()+".xml");
//      try {
//        List<Token> taggedText = getMentionSentence(docText, query.getName());
        String expected = mapWikipediaUrlToWikidataUrl(query.getEntity());
        Set<Entity> linkings = linkMention(query.getName());
        try {
            List<Entity> sortedCandidates = 
                 computeCandidateScores(query.getName(), linkings, docText);
        }
        catch (UIMAException e) {
            logger.error("Could not compute candidate scores: ", e);
        }
//        String actual = sortedCandidates.get(0); 
        entities.put(query.getId(), linkings);
        Set<String> linkingsAsStrings = 
            linkings.stream().map(e->e.getE2()).collect(Collectors.toSet());
        if (linkingsAsStrings.contains(expected)) {
          counter++;
        }

        if (!linkings.isEmpty()) {
        	coverageCounter++;
        }
        i++;
//        System.out.println(expected);
//        System.out.println(actual);
//        System.out.println(linkings);
//        System.out.println("Results unempty : " + (double) coverageCounter/i);
//        System.out.println("Results contain correct answer : " + (double) counter/i);
//      } catch (UIMAException e) {
//        logger.error("Could not parse Text", e);
//      }
    }    
  }

  public static void initializeConnection()
  {
    SPARQLRepository repo = new SPARQLRepository(SPARQL_ENDPOINT);
    repo.initialize();
    conn = repo.getConnection();
  }

  public static String mapWikipediaUrlToWikidataUrl(String url) {
    String wikidataQueryString = QueryUtil.mapWikipediaUrlToWikidataUrlQuery(url);
    TupleQuery wikidataIdQuery = 
        conn.prepareTupleQuery(QueryLanguage.SPARQL, wikidataQueryString);
    try (TupleQueryResult wikidataIdResult = wikidataIdQuery.evaluate()) {
      while (wikidataIdResult.hasNext()) {
        BindingSet sol = wikidataIdResult.next();
        return sol.getValue("e2").toString();
      }
    } catch(Exception e) {
        logger.error("could not map wikipedia URL to Wikidata Id", e);
    }
    return null;
  }

  /*
   * Retrieves the sentence containing the mention as Tokens
   */
  public static List<Token> getMentionSentence(String docText, String mention) throws UIMAException{
    JCas doc = JCasFactory.createText(docText, "en");
    AnalysisEngineDescription desc = createEngineDescription(
        createEngineDescription(StanfordSegmenter.class),
        createEngineDescription(StanfordPosTagger.class, 
            StanfordSegmenter.PARAM_LANGUAGE_FALLBACK, "en"));
    AnalysisEngine pipeline = AnalysisEngineFactory.createEngine(desc);
    pipeline.process(doc);
    
    for (Sentence s : JCasUtil.select(doc, Sentence.class)) {
      List<Token> sentence = new LinkedList<>();
      boolean containsMention = false;
      for (Token t : JCasUtil.selectCovered(Token.class, s)) {
        sentence.add(t);
        if(t.getCoveredText().toLowerCase().equals(mention)) {
          containsMention = true;
        }
      }
      if (containsMention) {
        return sentence;
      }
    }
    return null;
}
  
  public static Set<Entity> linkMention(String mention) {
    Set<Entity> linkings = new HashSet<>();
    List<String> mentionArray = Arrays.asList(mention.split(" "));

    ListIterator<String> it = mentionArray.listIterator();
    String current;
    while (it.hasNext()) {
      current = it.next().toLowerCase();
      it.set(current);
      if (punctuations.contains(current)) {
        it.remove();
      }
    }
    
    boolean onlyStopwords = true;
    ListIterator<String> it2 = mentionArray.listIterator();
    while (it2.hasNext()) {
      current = it2.next().toLowerCase();
      it2.set(current);
      if (!stopwords.contains(current)) {
        onlyStopwords = false;
        break;
      }
    }

    if (mentionArray.isEmpty() || onlyStopwords) {
      return null;
    }

    String entityQueryString = QueryUtil.entityQuery(mentionArray, 1000);
    TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, entityQueryString);
    try (TupleQueryResult entityResult = query.evaluate()) {
      while (entityResult.hasNext()) {
        BindingSet solution = entityResult.next();
            linkings.add(new Entity(solution.getValue("e2").toString(),
                    solution.getValue("label").toString(),
                    solution.getValue("anylabel").toString()));
      }
    } catch (QueryEvaluationException e) {
    	throw new QueryEvaluationException(e);
    }
    return linkings;
  }
  
  /**
   * Finds the position of a mention in a given sentence and
   * returns the corresponding tokens of the mention with 
   * <mentionContextSize> tokens before and 
   * <mentionContextSize> after the mention
   * 
   * TODO what happens if there are multiple mentions in a sentence?
   */
  public static List<Token> getMentionContext(List<Token> mentionSentence, List<String> mention, 
		  int mentionContextSize)
  {
	  int start = 0, end = 0;
	  int j = 0;
	  boolean done = false;
	  while (done == false && j < mentionSentence.size()) {
		for (int i = 0; i < mention.size(); i++) {
	      if (!mentionSentence.get(j).getCoveredText().equals(mention.get(i))) {
		 	break;
	      }
	      if (i == mention.size()-1) {
            start = j - (mention.size() - 1) - mentionContextSize;
            end = j + mentionContextSize + 1;
            done = true;
	      }
	    }
		j++;
	  }
	  
	  if (start == end) {
		  throw new IllegalStateException("Mention not found in sentence!");
	  }
	  if (start < 0) {
		  start = 0;
	  }
	  if (end > mentionSentence.size()) {
		  end = mentionSentence.size();
	  }
	  return mentionSentence.subList(start, end);
  }
  
  public static List<Token> tokenizeMention(String mention) throws UIMAException
  {
	  JCas doc = JCasFactory.createText(mention, "en");
	    AnalysisEngineDescription desc = createEngineDescription(
	        createEngineDescription(StanfordSegmenter.class),
				createEngineDescription(StanfordPosTagger.class,
						StanfordSegmenter.PARAM_LANGUAGE_FALLBACK, "en"));
		AnalysisEngine pipeline = AnalysisEngineFactory.createEngine(desc);
		pipeline.process(doc);

		List<Token> tokenizedMention = new LinkedList<>();
		for (Sentence s : JCasUtil.select(doc, Sentence.class)) {
			for (Token t : JCasUtil.selectCovered(Token.class, s)) {
				tokenizedMention.add(t);
			}
		}
		return tokenizedMention;
  }
  /**
   * The method should compute scores for each candidate linking for the given entity 
   * and sort the candidates so that the most probable candidate comes first. 
   *
   * @param mention
   * @param linkings
   * @param taggedText: the current text as a list of tagged token
   * @return
 * @throws UIMAException 
   */
  public static List<Entity> computeCandidateScores(String mention, Set<Entity> linkings, 
      String text) throws UIMAException {

    mention = mention.toLowerCase();
    int mentionContextSize = 2;
    List<Token> mentionSentence = getMentionSentence(text, mention);
    List<String> splitMention = Arrays.asList(mention.split(" "));
    List<Token> mentionContext = getMentionContext(mentionSentence, splitMention, 
    			mentionContextSize);
   
    // TODO and t['ner'] not in {"ORDINAL", "MONEY", "TIME", "PERCENTAGE"}} \
    Set<String> sentenceContentTokens = new HashSet<>();
    for (Token t: mentionSentence) {
        if ((t.getPos().getPosValue().startsWith("V") 
          || t.getPos().getPosValue().startsWith("N")
          || t.getPos().getPosValue().startsWith("J"))
                && !splitMention.contains(t.getCoveredText().toLowerCase())) {
            sentenceContentTokens.add(t.getCoveredText().toLowerCase());
        }
    }
    sentenceContentTokens = sentenceContentTokens.stream()
            //correct?
            .filter(f -> !stopwords.contains(f.toLowerCase()) 
                    || !splitMention.contains(f.toLowerCase()))
            .collect(Collectors.toSet());
    
    for (Entity l: linkings) {
        String wikidataId = l.getE2().replace("http://www.wikidata.org/entity/", "");
    	String anylabel = l.getAnyLabel().toLowerCase();
    	
    	l.setIdRank(Math.log(Double.parseDouble(wikidataId)));
    	
        LevenshteinDistance lev = new LevenshteinDistance();
        //TODO adjustable costs
        l.setLevMatchLabel(lev.apply(mention, anylabel).intValue());
        l.setLevSentence(lev.apply(tokensToString(mentionContext), anylabel).intValue());
        l.setNumRelatedRelations(0);

        Set<String> semanticSignature = getSemanticSignature(wikidataId);
        Set<String> signatureOverlap = new HashSet<>();
        for (String s: semanticSignature) {
            if (sentenceContentTokens.contains(s))
            signatureOverlap.add(s);
        }
        l.setSignatureOverlapScore(splitMention.size() + signatureOverlap.size());
    }
    List<Entity> result = sortCandidates(new ArrayList<>(linkings));
    return result;
    
  }

  private static List<Entity> sortCandidates(List<Entity> candidates)
{
      Collections.sort(candidates, new Comparator<Entity>() {

          @Override
          public int compare(Entity e1, Entity e2) {

            return new org.apache.commons.lang.builder.CompareToBuilder()
                .append(-e1.getSignatureOverlapScore(), -e2.getSignatureOverlapScore())
                .append(e1.getLevSentence()+ e1.getLevMatchLabel(), 
                        e2.getLevSentence() + e2.getLevMatchLabel())
                //TODO -entity['freqs']
                //TODO .append(-e1.getNumRelatedRelations(), -e2.getNumRelatedRelations())
                .append(e1.getIdRank(), e2.getIdRank())
                .toComparison();
          }
        });
    return candidates;
}

private static String tokensToString(List<Token> sentence) {
	String result ="";
	  for(Token t: sentence) {
	    result.concat(t.getCoveredText());
	  }
	return result;
  }
  
  // TODO include relations
  // TODO filter against blacklist
  public static Set<String> getSemanticSignature(String wikidataId) {
      Set<String> semanticSignature = new HashSet<>();
      String queryString = QueryUtil.semanticSignatureQuery(wikidataId);
      TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
      try (TupleQueryResult result = query.evaluate()) {
        while (result.hasNext()) {
          BindingSet sol = result.next();
          semanticSignature.add(sol.getValue("label").toString());
        }
      } catch(Exception e) {
          logger.error("could not get semantic signature", e);
      }
      return semanticSignature;
  }

}