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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import java.io.Serializable;

import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;

/**
 * Data model for the {@link Automation} module<br>
 * Prefixes to be used as a features for MIRA prediction<br>
 * Example, for the word <b>human</b>, prefix1 will be <b>h</b>, prefix2 will be <b>hu</b>....<br>
 * Suffixes to be used as features for MIRA prediction <br>
 * Example, for the word <b>human</b>, suffix1 will be <b>n</b>, suffix2 will be <b>an<b>....
 *
 * @author Seid Muhie Yimam
 *
 */
public class AutomationModel
    implements Serializable
{
    private static final long serialVersionUID = 3926526929200593949L;

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
     * Determine if MIRA automation is applicable to the annotator/correction view and automation view too
     */
    private boolean predictAnnotator = false;
    private boolean predictAutomator = true;

    /**
     * Limit prediction only to this page while automatic annotation
     */

    private boolean predictInThisPage;

    /**
     * Train {@link TagSet} used for MIRA prediction
     */
    private TagSet trainTagSet;
    /**
     * {@link TagSet} used as a feature for the trainTagSet
     */
    private TagSet featureTagSet;

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

    public boolean isPredictAnnotator()
    {
        return predictAnnotator;
    }

    public void setPredictAnnotator(boolean predictAnnotator)
    {
        this.predictAnnotator = predictAnnotator;
    }

    public boolean isPredictAutomator()
    {
        return predictAutomator;
    }

    public void setPredictAutomator(boolean predictAutomator)
    {
        this.predictAutomator = predictAutomator;
    }

    public TagSet getFeatureTagSet()
    {
        return featureTagSet;
    }

    public void setFeatureTagSet(TagSet featureTagSet)
    {
        this.featureTagSet = featureTagSet;
    }

    

}
