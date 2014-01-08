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

import java.util.ArrayList;

/**
 * This is a class representing the bean objects to store users preference of annotation settings
 * such as annotation layers, number of sentence to display at a time, visibility of lemma and
 * whether to allow auto page scrolling.
 *
 * @author Seid Muhie Yimam
 */
public class AnnotationPreference
{
    // Id of annotation layers, to be stored in the properties file comma separated: 12, 34,....
    private ArrayList<Long> annotationLayers;
    private int windowSize = 10;
    private boolean scrollPage;

    private boolean predictInThisPage;
    private boolean useExistingModel;
    private long trainLayer = -1;

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

    public ArrayList<Long> getAnnotationLayers()
    {
        return annotationLayers;
    }

    public void setAnnotationLayers(ArrayList<Long> aAnnotationLayers)
    {
        annotationLayers = aAnnotationLayers;
    }

    public int getWindowSize()
    {
        return windowSize;
    }

    public void setWindowSize(int aWindowSize)
    {
        windowSize = aWindowSize;
    }

    public boolean isScrollPage()
    {
        return scrollPage;
    }

    public void setScrollPage(boolean aScrollPage)
    {
        scrollPage = aScrollPage;
    }

    public boolean isPredictInThisPage()
    {
        return predictInThisPage;
    }

    public void setPredictInThisPage(boolean predictInThisPage)
    {
        this.predictInThisPage = predictInThisPage;
    }

    public boolean isUseExistingModel()
    {
        return useExistingModel;
    }

    public void setUseExistingModel(boolean useExistingModel)
    {
        this.useExistingModel = useExistingModel;
    }

    public long getTrainLayer()
    {
        return trainLayer;
    }

    public void setTrainLayer(long trainLayerId)
    {
        this.trainLayer = trainLayerId;
    }

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


}
