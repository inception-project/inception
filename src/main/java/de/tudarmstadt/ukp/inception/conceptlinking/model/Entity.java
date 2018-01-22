package de.tudarmstadt.ukp.inception.conceptlinking.model;

import java.util.Set;

/*
 * Stores information about entities retrieved from a knowledge base 
 * Needed to compute a candidate ranking score
 */
public class Entity
{

    private String e2;
    private String anylabel;
    private String label;
    private Set<String> signatureOverlap;
    private int levMainLabel;
    private int levMatchLabel;
    private int levSentence;
    private int numRelatedRelations;
    private int signatureOverlapScore;
    private double idRank;
    private int frequency;

    public Entity(String e2, String label, String anylabel)
    {
        this.e2 = e2;
        this.label = label;
        this.anylabel = anylabel;
    }

    public String getE2()
    {
        return e2;
    }

    public String getLabel()
    {
        return label;
    }

    public String getAnyLabel()
    {
        return anylabel;
    }

    public Set<String> getSignatureOverlap()
    {
        return signatureOverlap;
    }

    public void setSignatureOverlap(Set<String> signatureOverlap)
    {
        this.signatureOverlap = signatureOverlap;
    }

    public int getLevMainLabel()
    {
        return levMainLabel;
    }

    public void setLevMainLabel(int levMainLabel)
    {
        this.levMainLabel = levMainLabel;
    }

    public int getLevMatchLabel()
    {
        return levMatchLabel;
    }

    public void setLevMatchLabel(int levMatchLabel)
    {
        this.levMatchLabel = levMatchLabel;
    }

    public int getLevSentence()
    {
        return levSentence;
    }

    public void setLevSentence(int levSentence)
    {
        this.levSentence = levSentence;
    }

    public void setNumRelatedRelations(int i)
    {
        this.numRelatedRelations = i;
    }

    public int getNumRelatedRelations()
    {
        return numRelatedRelations;
    }

    public void setSignatureOverlapScore(int aScore)
    {
        this.signatureOverlapScore = aScore;
    }

    public int getSignatureOverlapScore()
    {
        return signatureOverlapScore;
    }

    public void setIdRank(double idRank)
    {
        this.idRank = idRank;
    }

    public double getIdRank()
    {
        return idRank;
    }

    public void setFrequency(int frequency)
    {
        this.frequency = frequency;
    }
    
    public int getFrequency()
    {
        return frequency;
    }

}
