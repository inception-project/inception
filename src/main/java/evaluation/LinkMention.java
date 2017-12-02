package evaluation;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;

import de.dailab.irml.gerned.QueriesReader;
import de.dailab.irml.gerned.data.Query;;

public class LinkMention {

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
    File queriesFile = new File("../gerned/dataset/ANY_german_queries.xml");
    // File answersFile = new File("../gerned/dataset/ANY_german_queries.tab");
    List<Query> queries = reader.readQueries(queriesFile);

    Map<String, Set<String>> entities = new HashMap<String, Set<String>>();

    for (Query query : queries) {
      Set<String> e = linkMention(query.getName());
      entities.put(query.getId(), e);
      // System.out.println(e);
    }
    
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

    String queryString = QueryUtil.entityQuery(mentionArray);
    TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    try (TupleQueryResult result = query.evaluate()) {
      while (result.hasNext()) {
        BindingSet solution = result.next();
        linkings.add(solution.getValue("e2").toString());
      }
    }
    return linkings;
  }

}