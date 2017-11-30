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

  private static String SPARQL_INFERENCE_CLAUSE = "DEFINE input:inference 'instances'";

  private static String SPARQL_PREFIX = "PREFIX e:<http://www.wikidata.org/entity/>\n"
      + "        PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
      + "        PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
      + "        PREFIX base:<http://www.wikidata.org/ontology#>";

  private static String SPARQL_SELECT = "SELECT DISTINCT %queryvariables% WHERE";

  private static String SPARQL_ENTITY_LABEL = "{\n"
      + "              {GRAPH <http://wikidata.org/statements> { ?e2 e:P1549s/e:P1549v \"%demonym\"@de}} UNION\n"
      + "              {VALUES ?labelpredicate {rdfs:label skos:altLabel}\n"
      + "              GRAPH <http://wikidata.org/terms> {\n"
      + "                            ?e2 ?labelpredicate ?anylabel. ?anylabel bif:contains '\"%entitylabel\"'@de  }\n"
      + "                            FILTER ( lang(?anylabel) = \"de\" )\n"
      + "                            }\n" + "        }\n"
      + "        FILTER EXISTS { GRAPH <http://wikidata.org/statements> { ?e2 ?p ?v }}\n"
      + "        FILTER NOT EXISTS {\n"
      + "            VALUES ?topic {e:Q17442446 e:Q18616576 e:Q5707594 e:Q427626 e:Q16521 e:Q11173}\n"
      + "            GRAPH <http://wikidata.org/instances> {?e2 rdf:type ?topic}}\n"
      + "        BIND (STRLEN(?anylabel) as ?len)";

  private static String SPARQL_CANONICAL_LABEL_ENTITY = "{\n"
      + "        GRAPH <http://wikidata.org/terms> { ?e2 rdfs:label ?label }\n"
      + "        FILTER ( lang(?label) = \"de\" )\n" + "        }";

  private static String SPARQL_LIMIT = " \n LIMIT ";

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

    String queryString = entityQuery(mentionArray);
    TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    try (TupleQueryResult result = query.evaluate()) {
      while (result.hasNext()) {
        BindingSet solution = result.next();
        linkings.add(solution.getValue("e2").toString());
      }
    }
    return linkings;
  }

  public static String entityQuery(List<String> tokens) {
    String query = SPARQL_INFERENCE_CLAUSE;
    query += SPARQL_PREFIX + "\n";
    query += SPARQL_SELECT + "{";
    String SPARQL_ENTITY_LABEL_INST = (SPARQL_ENTITY_LABEL + SPARQL_CANONICAL_LABEL_ENTITY)
        .replace("%entitylabel", 
            ("'").concat(String.join(" ", tokens).concat("'")).replace("'", ""));
    if (tokens.size() == 1) {
      SPARQL_ENTITY_LABEL_INST = SPARQL_ENTITY_LABEL_INST.replace("%demonym", tokens.get(0));
    } else {
      SPARQL_ENTITY_LABEL_INST = SPARQL_ENTITY_LABEL_INST.replace(
          "{GRAPH <http://wikidata.org/statements> { ?e2 e:P1549s/e:P1549v \"%demonym\"@de}} UNION\n",
          "");
    }
    query += SPARQL_ENTITY_LABEL_INST;
    query += "}";
    query += SPARQL_LIMIT + "10";
    String variables = "".concat("?e2 ").concat("?anylabel ").concat("?label");
    query = query.replace("%queryvariables%", variables);
    return query;
  }

}