package evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import de.dailab.irml.gerned.NewsReader;
import de.dailab.irml.gerned.QueriesReader;
import de.dailab.irml.gerned.data.Query;

public class LinkMention {

  private static Logger logger = LoggerFactory.getLogger(LinkMention.class);
  
  private static RepositoryConnection conn;
  
  // TODO
  private static String punctuations = "";
  private static String stopwords = "";

  private static String SPARQL_ENDPOINT = "http://knowledgebase.ukp.informatik.tu-darmstadt.de:8890/sparql";

  public static void main(String[] args) {

    SPARQLRepository repo = new SPARQLRepository(SPARQL_ENDPOINT);
    repo.initialize();
    conn = repo.getConnection();

    QueriesReader reader = new QueriesReader();
    // File queriesFile = new File("../gerned/dataset/ANY_german_queries.xml");
    File answersFile = new File("../gerned/dataset/ANY_german_queries_with_answers.xml");
    List<Query> queries = reader.readQueries(answersFile);

    Map<String, Set<String>> entities = new HashMap<String, Set<String>>();
    
    int counter = 0;
    for (Query query : queries) {
      String expected = mapWikipediaUrlToWikidataUrl(query.getEntity());
        Set<String> linkings = linkMention(query.getName());
          entities.put(query.getId(), linkings);
          if(linkings.contains(expected)) {
            counter++;
          }
           System.out.println(expected);
           System.out.println(linkings);
    }
    System.out.println(counter/queries.size());
    
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

  public static Set<String> linkMention(String mention) {
    Set<String> linkings = new HashSet<String>();
    List<String> mentionArray = Arrays.asList(mention.split(" "));

    ListIterator<String> it = mentionArray.listIterator();
    String current;
    while (it.hasNext()) {
      current = it.next().toLowerCase();
      it.set(current);
      if (stopwords.contains(current) || punctuations.contains(current)) {
        it.remove();
      }
    }

    if (mentionArray.isEmpty()) {
      return null;
    }

    String entityQueryString = QueryUtil.entityQuery(mentionArray, 10);
    TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, entityQueryString);
    try (TupleQueryResult entityResult = query.evaluate()) {
      while (entityResult.hasNext()) {
        BindingSet solution = entityResult.next();
        linkings.add(solution.getValue("e2").toString());
      }
    }
    return linkings;
  }

}