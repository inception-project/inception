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
public class CandidateEntity
{

    /**
     * The IRI String of this entity
     */
    private String IRI;

    /**
     * The main label of this entity
     */
    private String label;

    /**
     * An alternative label (alias) of this entity
     */
    private String alternativeLabel;

    /**
     * A description for this entity
     */
    private String description;

    /**
     * edit distance between mention and candidate entity label
     */
    private int levMatchLabel;

    /**
     * edit distance between mention + context and candidate entity label
     */
    private int levContext;

    /**
     * edit distance between typed string and candidate entity label
     */
    private int levTypedString;

    /**
     * set of directly related entities as IRI Strings
     */
    private Set<String> signatureOverlap;

    /**
     * number of distinct relations to other entities
     */
    private int numRelatedRelations;

    /**
     * number of related entities whose entity label occurs in <i>content tokens</i>
     * <i>Content tokens</i> consist of tokens in mention sentence annotated as nouns, verbs or
     * adjectives
     */
    private int signatureOverlapScore;

    /**
     * logarithm of the wikidata ID - based on the assumption that lower IDs are more important
     */
    private double idRank;

    /**
     * in-link count of wikipedia article of IRI
     */
    private int frequency;

    public CandidateEntity(String IRI, String label, String alternativeLabel, String description)
    {
        this.IRI = IRI;
        this.label = label;
        this.alternativeLabel = alternativeLabel;
        this.description = description;
    }

    /**
     * @return The IRI String of this entity
     */
    public String getIRI()
    {
        return IRI;
    }

    /**
     * @return The main label of this entity
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * @return An alternative label (alias) of this entity
     */
    public String getAltLabel()
    {
        return alternativeLabel;
    }

    /**
     * Get a description for this entity
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Set a description for this entity
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return set of directly related entities as IRI Strings
     */
    public Set<String> getSignatureOverlap()
    {
        return signatureOverlap;
    }

    /**
     * @param signatureOverlap set of directly related entities as IRI Strings
     */
    public void setSignatureOverlap(Set<String> signatureOverlap)
    {
        this.signatureOverlap = signatureOverlap;
    }

    /**
     * @return edit distance between mention and candidate entity label
     */
    public int getLevMatchLabel()
    {
        return levMatchLabel;
    }

    /**
     * @param levMatchLabel edit distance between mention and candidate entity label
     */
    public void setLevMatchLabel(int levMatchLabel)
    {
        this.levMatchLabel = levMatchLabel;
    }

    /**
     * @return edit distance between mention + context and candidate entity label
     */
    public int getLevContext()
    {
        return levContext;
    }

    /**
     * @param aLevTypedString edit distance between typed string and candidate entity label
     */
    public void setLevTypedString(int aLevTypedString)
    {
        levTypedString = aLevTypedString;
    }

    public int getLevTypedString()
    {
        return levTypedString;
    }

    /**
     * @param levContext edit distance between mention + context and candidate entity label
     */
    public void setLevContext(int levContext)
    {
        this.levContext = levContext;
    }

    /**
     * @param numRelatedRelations number of distinct relations to other entities
     */
    public void setNumRelatedRelations(int numRelatedRelations)
    {
        this.numRelatedRelations = numRelatedRelations;
    }

    /**
     * @return number of distinct relations to other entities
     */
    public int getNumRelatedRelations()
    {
        return numRelatedRelations;
    }

    /**
     * @param aScore number of related entities whose entity label occurs in <i>content tokens</i>.
     * <i>Content tokens</i> consist of tokens in mention sentence annotated as nouns, verbs or
     * adjectives
     */
    public void setSignatureOverlapScore(int aScore)
    {
        this.signatureOverlapScore = aScore;
    }

    /**
     * @return number of related entities whose entity label occurs in <i>content tokens</i>.
     * <i>Content tokens</i> consist of tokens in mention sentence annotated as nouns, verbs or
     * adjectives
     */
    public int getSignatureOverlapScore()
    {
        return signatureOverlapScore;
    }

    /**
     * @param idRank
     *            logarithm of the wikidata ID - based on the assumption that lower IDs are more
     *            important
     */
    public void setIdRank(double idRank)
    {
        this.idRank = idRank;
    }

    /**
     * @return logarithm of the wikidata ID - based on the assumption that lower IDs are more
     * important
     */
    public double getIdRank()
    {
        return idRank;
    }

    /**
     * @param frequency in-link count of wikipedia article of IRI
     */
    public void setFrequency(int frequency)
    {
        this.frequency = frequency;
    }

    /**
     * @return in-link count of wikipedia article of IRI
     */
    public int getFrequency()
    {
        return frequency;
    }
    
}
