/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.automation.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;

/**
 * A persistence object for MIRA template configurations
 *
 * @author Seid Muhie Yimam
 *
 */
@Entity
@Table(name = "mira_template", uniqueConstraints = { @UniqueConstraint(columnNames = { "trainTagSet" }) })
public class MiraTemplate
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue
    private long id;

    private boolean capitalized;
    private boolean containsNumber = true;

    private boolean prefix1 = true;
    private boolean prefix2 = true;
    private boolean prefix3 = true;
    private boolean prefix4 = true;
    private boolean prefix5 = true;

    private boolean suffix1 = true;
    private boolean suffix2 = true;
    private boolean suffix3 = true;
    private boolean suffix4 = true;
    private boolean suffix5 = true;

    private int ngram = 3;
    private int bigram;

    /**
     * Limit prediction only to this page while automatic annotation
     */

    private boolean predictInThisPage;

    /**
     * Train {@link TagSet} used for MIRA prediction
     */
    @ManyToOne
    @JoinColumn(name = "trainTagSet")
    private TagSet trainTagSet;
    /**
     * {@link TagSet} used as a feature for the trainTagSet
     */
    @ManyToMany
    private Set<TagSet> featureTagSets = new HashSet<TagSet>();
    /**
     * Annotate the Automate view while annotating
     */
    private boolean annotateAndPredict = true;

    /**
     * Results comprising of the tarining accuracy and number of examples used
     */
    private String result = "";

    public boolean isCapitalized()
    {
        return capitalized;
    }

    public void setCapitalized(boolean capitalized)
    {
        this.capitalized = capitalized;
    }

    public boolean isContainsNumber()
    {
        return containsNumber;
    }

    public void setContainsNumber(boolean containsNumber)
    {
        this.containsNumber = containsNumber;
    }

    public boolean isPrefix1()
    {
        return prefix1;
    }

    public void setPrefix1(boolean prefix1)
    {
        this.prefix1 = prefix1;
    }

    public boolean isPrefix2()
    {
        return prefix2;
    }

    public void setPrefix2(boolean prefix2)
    {
        this.prefix2 = prefix2;
    }

    public boolean isPrefix3()
    {
        return prefix3;
    }

    public void setPrefix3(boolean prefix3)
    {
        this.prefix3 = prefix3;
    }

    public boolean isPrefix4()
    {
        return prefix4;
    }

    public void setPrefix4(boolean prefix4)
    {
        this.prefix4 = prefix4;
    }

    public boolean isPrefix5()
    {
        return prefix5;
    }

    public void setPrefix5(boolean prefix5)
    {
        this.prefix5 = prefix5;
    }

    public boolean isSuffix1()
    {
        return suffix1;
    }

    public void setSuffix1(boolean suffix1)
    {
        this.suffix1 = suffix1;
    }

    public boolean isSuffix2()
    {
        return suffix2;
    }

    public void setSuffix2(boolean suffix2)
    {
        this.suffix2 = suffix2;
    }

    public boolean isSuffix3()
    {
        return suffix3;
    }

    public void setSuffix3(boolean suffix3)
    {
        this.suffix3 = suffix3;
    }

    public boolean isSuffix4()
    {
        return suffix4;
    }

    public void setSuffix4(boolean suffix4)
    {
        this.suffix4 = suffix4;
    }

    public boolean isSuffix5()
    {
        return suffix5;
    }

    public void setSuffix5(boolean suffix5)
    {
        this.suffix5 = suffix5;
    }

    public int getNgram()
    {
        return ngram;
    }

    public void setNgram(int ngram)
    {
        this.ngram = ngram;
    }

    public int getBigram()
    {
        return bigram;
    }

    public void setBigram(int bigram)
    {
        this.bigram = bigram;
    }

    public boolean isPredictInThisPage()
    {
        return predictInThisPage;
    }

    public void setPredictInThisPage(boolean predictInThisPage)
    {
        this.predictInThisPage = predictInThisPage;
    }

    public TagSet getTrainTagSet()
    {
        return trainTagSet;
    }

    public void setTrainTagSet(TagSet trainTagSet)
    {
        this.trainTagSet = trainTagSet;
    }

    public Set<TagSet> getFeatureTagSets()
    {
        return featureTagSets;
    }

    public void setFeatureTagSets(Set<TagSet> featureTagSets)
    {
        this.featureTagSets = featureTagSets;
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public boolean isAnnotateAndPredict()
    {
        return annotateAndPredict;
    }

    public void setAnnotateAndPredict(boolean annotateAndPredict)
    {
        this.annotateAndPredict = annotateAndPredict;
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((trainTagSet == null) ? 0 : trainTagSet.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MiraTemplate other = (MiraTemplate) obj;
        if (id != other.id) {
            return false;
        }
        if (trainTagSet == null) {
            if (other.trainTagSet != null) {
                return false;
            }
        }
        else if (!trainTagSet.equals(other.trainTagSet)) {
            return false;
        }
        return true;
    }

}
