package de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation;

import java.util.List;

import de.tudarmstadt.ukp.inception.experimental.api.model.Relation;

public class AllRelationResponse
{

    private List<Relation> relations;

    public AllRelationResponse(List<Relation> aRelations) {
        relations = aRelations;
    }

    public List<Relation> getRelations()
    {
        return relations;
    }

    public void setRelations(List<Relation> aRelations)
    {
        relations = aRelations;
    }
}
