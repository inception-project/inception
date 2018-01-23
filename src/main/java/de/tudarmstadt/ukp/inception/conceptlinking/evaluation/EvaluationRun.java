package de.tudarmstadt.ukp.inception.conceptlinking.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.UIMAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dailab.irml.gerned.NewsReader;
import de.dailab.irml.gerned.QueriesReader;
import de.dailab.irml.gerned.data.Query;
import de.tudarmstadt.ukp.inception.conceptlinking.model.Entity;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;

public class EvaluationRun
{
    private static Logger logger = LoggerFactory.getLogger(EvaluationRun.class);
    
    private static String SPARQL_ENDPOINT 
        = "http://knowledgebase.ukp.informatik.tu-darmstadt.de:8890/sparql";
    
    public static void main(String[] args)
    {
        
        ConceptLinkingService.init(SPARQL_ENDPOINT);
        
        QueriesReader reader = new QueriesReader();
        File answersFile = new File("../gerned/dataset/ANY_german_queries_with_answers.xml");
        List<Query> queries = reader.readQueries(answersFile);
    
        logger.info("Total number of queries: " + queries.size());
        logger.info("Candidate query limit: " + ConceptLinkingService.getCandidateQueryLimit());
        logger.info("Signature query limit: " + ConceptLinkingService.getSignatureQueryLimit());
        
        double correct = 0;
        double contain = 0;
        double total = 0;
        
        List<String> nil = new LinkedList<>();
        List<String> noIdInVirtuoso = new LinkedList<>();
        List<String> noCandidatesForId = new LinkedList<>();
        List<String> resultNotInCandidates = new LinkedList<>();
        Map<Integer, Set<String>> inFirstX = new HashMap<>();
        
        for (Query query : queries) {
            double startTime = System.currentTimeMillis();
            String docText = NewsReader
                    .readFile("../gerned/dataset/news/" + query.getDocid() + ".xml");
            
            logger.debug(query.getId());
            
            // These entities have no result
            if (query.getEntity().startsWith("NIL")) {
                nil.add(query.getId());
                logger.info("NIL query: " + query.getEntity());
                continue;
            }
            
            String expected = ConceptLinkingService.mapWikipediaUrlToWikidataUrl(query.getEntity());
            
            // Skip terms that are not in Virtuoso dump 
            if (expected == null) {
                noIdInVirtuoso.add(query.getId());
                logger.info("Mention " + query.getName() + "not in Virtuoso.");
                continue;
            }
            Set<Entity> linkings = ConceptLinkingService.linkMention(query.getName());
            
            try {
                List<Entity> sortedCandidates = ConceptLinkingService.computeCandidateScores(
                        query.getName().toLowerCase(), linkings,docText.toLowerCase());
                
                if (sortedCandidates == null || sortedCandidates.isEmpty()) {
                    noCandidatesForId.add(query.getId());
                    logger.info("No candidates for mention" + query.getName());
                }
                else {
                    String actual = sortedCandidates.get(0).getE2();
    
                    List<String> candidateIds = sortedCandidates.stream().map(e -> e.getE2())
                            .collect(Collectors.toList());
    
                    // The correct linking is included in the set of candidates
                    if (candidateIds.contains(expected)) {
                        contain++;
                    }
                    else {
                        resultNotInCandidates.add(query.getId());
                    }
    
                    for (int x = 0; x <= 9; x++) {
                        List<String> subList;
                        if (candidateIds.size() < x) {
                            subList = candidateIds.subList(0, candidateIds.size());
                        } else {
                            subList = candidateIds.subList(0, x);
                        }
                        if (subList.contains(expected)) {
                            Set<String> firstX = inFirstX.get(x + 1);
                            if (firstX == null) {
                                firstX = new HashSet<>();
                            }
                            firstX.add(query.getId());
                            inFirstX.put(x + 1, firstX);
                        }
                    }
                    
                    // The entity was linked correctly.
                    if (actual.equals(expected)) {
                        correct++;
                    }
                }
                
                total++;
                
                logger.info("\nNumber of terms in Virtuoso: " + total);
                logger.info("Number of correct linkings: " + correct);
                logger.info("Number of sets that contains the correct result: " + contain);
                logger.info("Proportion of correct linkings: " + correct / total);
                logger.info("Proportion of candidate sets containing the correct result: "
                        + contain / total);
            }
            catch (UIMAException | IOException e) {
                logger.error("Could not compute candidate scores: ", e);
            }
    
            logger.debug(System.currentTimeMillis() - startTime + "ms for this iteration.\n");
        }
        
        int totalSkipped = nil.size() + noIdInVirtuoso.size() + noCandidatesForId.size();
        logger.info("Evaluation finished. " + totalSkipped + " entities skipped.");
        logger.info(nil.size() + " entries are NIL.");
        logger.info(noIdInVirtuoso.size() + " entry Ids could not be resolved.");
        logger.info(noCandidatesForId.size() + " entries got no candidates.");
        logger.info("------------------------------------------------------------------------");
        logger.info("Entries with no Id:");
        logger.info(Arrays.toString(nil.toArray()));
        logger.info("------------------------------------------------------------------------");
        logger.info("Entries whose Ids could not be resolved:");
        logger.info(Arrays.toString(noIdInVirtuoso.toArray()));
        logger.info("------------------------------------------------------------------------");
        logger.info("Entries with no candidates found:");
        logger.info(Arrays.toString(noCandidatesForId.toArray()));
        logger.info("------------------------------------------------------------------------");
        logger.info("Entries where correct linking was not in candidates:");
        logger.info(Arrays.toString(noCandidatesForId.toArray()));
        logger.info("------------------------------------------------------------------------");
    
        for (int i = 1; i <= 10; i++) {
            double precisionAt = 0.0;
            if (inFirstX.get(i) != null) {
                precisionAt = inFirstX.get(i).size();
            }
            logger.info("Precision at " + i + ": " + precisionAt / total);
        }
        
    }

}
