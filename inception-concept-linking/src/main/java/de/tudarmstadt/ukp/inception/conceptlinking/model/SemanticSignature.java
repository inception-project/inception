package de.tudarmstadt.ukp.inception.conceptlinking.model;

import java.util.Set;

/**
 * Captures the directly related entities and related relations of a given entity A.
 * An entity B is considered related to A iff A has B as an attribute in property P,
 * or if B has A as an attribute in property P.
 */
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
