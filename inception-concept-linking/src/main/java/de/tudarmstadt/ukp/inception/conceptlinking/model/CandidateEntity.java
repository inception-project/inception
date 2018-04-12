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
     * edit distance between mention and candidate entity label
     */
    private int levMatchLabel;

    /**
     * edit distance between mention + context and candidate entity label
     */
    private int levContext;

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

    public CandidateEntity(String IRI, String label, String alternativeLabel)
    {
        this.IRI = IRI;
        this.label = label;
        this.alternativeLabel = alternativeLabel;
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
     * @param idRank logarithm of the wikidata ID - based on the assumption that lower IDs are more important
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

    /**
     * Weights learned by RankLib
     * @return
     */
    public double getCoordinateAscentScore()
    {
        final double ID_RANK_WEIGHT = -0.7575276861703767;
        final double ENTITY_FREQ_WEIGHT = 0.06811294352264444;
        final double LEV_MATCH_LABEL_WEIGHT = -0.11561875830516183;
        final double LEV_CONTEXT_WEIGHT = -0.021483371226337238;
        final double SIGNATURE_OVERLAP_SCORE_WEIGHT = 0.005528688443233935;
        final double NUM_RELATED_RELATIONS_WEIGHT = 0.031728552332245745;

        return ID_RANK_WEIGHT * getIdRank()
            + ENTITY_FREQ_WEIGHT * getFrequency()
            + LEV_MATCH_LABEL_WEIGHT * getLevMatchLabel()
            + LEV_CONTEXT_WEIGHT * getLevContext()
            + SIGNATURE_OVERLAP_SCORE_WEIGHT * getSignatureOverlapScore()
            + NUM_RELATED_RELATIONS_WEIGHT * getNumRelatedRelations();
    }

    public double getLinearRegressionScore()
    {
        final double BIAS = -0.022042922120240806;
        final double ID_RANK_WEIGHT = -0.022042922120240806;
        final double ENTITY_FREQ_WEIGHT = 2.3104427918317867E-5;
        final double LEV_MATCH_LABEL_WEIGHT = -6.414981822969735E-4;
        final double LEV_CONTEXT_WEIGHT = 1.2166468608500678E-4;
        final double SIGNATURE_OVERLAP_SCORE_WEIGHT = 0.04861988688865763;
        final double NUM_RELATED_RELATIONS_WEIGHT = 0.2847175878087973;

        return BIAS
            + ID_RANK_WEIGHT * getIdRank()
            + ENTITY_FREQ_WEIGHT * getFrequency()
            + LEV_MATCH_LABEL_WEIGHT * getLevMatchLabel()
            + LEV_CONTEXT_WEIGHT * getLevContext()
            + SIGNATURE_OVERLAP_SCORE_WEIGHT * getSignatureOverlapScore()
            + NUM_RELATED_RELATIONS_WEIGHT * getNumRelatedRelations();
    }
}
