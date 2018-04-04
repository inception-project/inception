/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.inception.conceptlinking.model;

import java.util.Set;

/**
 * Stores information about entities retrieved from a knowledge base 
 * Needed to rank candidates
 */
public class Entity
{

    /**
     * The IRI of this entity
     */
    private String IRI;

    /**
     * The main label of this entity
     */
    private String label;

    /**
     * The alternative label of this particular entity
     */
    private String alternativeLabel;

    /**
     * edit distance between mention and candidate entity label
     */
    private int levMatchLabel;

    /**
     * edit distance between mention + context and candidate entity label
     */
    private int levContext;

    /**
     * set of directly related entities (IRI Strings)
     */
    
    private Set<String> signatureOverlap;
    /**
     * number of distinct relations to other entities
     */
    private int numRelatedRelations;

    /**
     * number of related entities whose entity label occurs in <i>content tokens</i>
     * <i>content tokens</i> consist of tokens in mention sentence annotated as nouns, verbs or
     * adjectives
     */
    private int signatureOverlapScore;

    /**
     * logarithm of the wikidata ID
     * based on the asumption that lower IDs are more important
     */
    private double idRank;

    /**
     * in-link count of wikipedia article of IRI
     */
    private int frequency;

    public Entity(String IRI, String label, String alternativeLabel)
    {
        this.IRI = IRI;
        this.label = label;
        this.alternativeLabel = alternativeLabel;
    }

    public String getIRI()
    {
        return IRI;
    }

    public String getLabel()
    {
        return label;
    }

    public String getAltLabel()
    {
        return alternativeLabel;
    }

    public Set<String> getSignatureOverlap()
    {
        return signatureOverlap;
    }

    public void setSignatureOverlap(Set<String> signatureOverlap)
    {
        this.signatureOverlap = signatureOverlap;
    }

    public int getLevMatchLabel()
    {
        return levMatchLabel;
    }

    public void setLevMatchLabel(int levMatchLabel)
    {
        this.levMatchLabel = levMatchLabel;
    }

    public int getLevContext()
    {
        return levContext;
    }

    public void setLevContext(int levContext)
    {
        this.levContext = levContext;
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
