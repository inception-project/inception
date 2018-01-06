package evaluation;

import java.util.Set;

public class SemanticSignature
{

    private Set<String> relatedRelations;
    private Set<String> relatedEntities;

    public SemanticSignature(Set<String> relatedEntities, Set<String> relatedRelations)
    {
        this.relatedEntities = relatedEntities;
        this.relatedRelations = relatedRelations;
    }

    public Set<String> getRelatedRelations()
    {
        return relatedRelations;
    }

    public void setRelatedRelations(Set<String> relatedRelations)
    {
        this.relatedRelations = relatedRelations;
    }

    public Set<String> getRelatedEntities()
    {
        return relatedEntities;
    }

    public void setRelatedEntities(Set<String> relatedEntities)
    {
        this.relatedEntities = relatedEntities;
    }

}
