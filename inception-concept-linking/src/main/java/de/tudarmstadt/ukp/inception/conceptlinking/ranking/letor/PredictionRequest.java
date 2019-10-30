package de.tudarmstadt.ukp.inception.conceptlinking.ranking.letor;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

public class PredictionRequest {

    private String mention;
    private String context;
    private List<KBHandle> candidates;

    public PredictionRequest(String aMention, String aContext, List<KBHandle> aCandidates)
    {
        mention = aMention;
        context = aContext;
        candidates = aCandidates;
    }

    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();

        result.put("mention", mention);
        result.put("context", context);

        ArrayNode candidatesNode = result.putArray("candidates");

        for (KBHandle candidate : candidates) {
            ObjectNode candidateNode = mapper.createObjectNode();

            candidateNode.put("iri", candidate.getIdentifier());
            candidateNode.put("label", candidate.getName());
            candidateNode.put("description", candidate.getDescription());
            candidatesNode.add(candidateNode);
        }

        return result.toPrettyString();
    }
}
