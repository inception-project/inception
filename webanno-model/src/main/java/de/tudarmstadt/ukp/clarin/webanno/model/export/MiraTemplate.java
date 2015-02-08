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
package de.tudarmstadt.ukp.clarin.webanno.model.export;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * All required contents of a {@link de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate}  to be exported.
 *
 * @author Seid Muhie Yimam
 *
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiraTemplate
{
    @JsonProperty("automation_started")
    private boolean automationStarted = false;

    @JsonProperty("predict_in_this_page")
    private boolean predictInThisPage;

    @JsonProperty("train_feature")
    private AnnotationFeature trainFeature;

    @JsonProperty("other_features")
    private Set<AnnotationFeature> otherFeatures = new HashSet<AnnotationFeature>();

    @JsonProperty("current_layer")
    private boolean currentLayer = false;

    @JsonProperty("annotate_and_predict")
    private boolean annotateAndPredict = true;

    @JsonProperty("result")
    private String result = "";

    public boolean isAutomationStarted()
    {
        return automationStarted;
    }

    public void setAutomationStarted(boolean automationStarted)
    {
        this.automationStarted = automationStarted;
    }

    public boolean isPredictInThisPage()
    {
        return predictInThisPage;
    }

    public void setPredictInThisPage(boolean predictInThisPage)
    {
        this.predictInThisPage = predictInThisPage;
    }

    public AnnotationFeature getTrainFeature()
    {
        return trainFeature;
    }

    public void setTrainFeature(AnnotationFeature trainFeature)
    {
        this.trainFeature = trainFeature;
    }

    public Set<AnnotationFeature> getOtherFeatures()
    {
        return otherFeatures;
    }

    public void setOtherFeatures(Set<AnnotationFeature> otherFeatures)
    {
        this.otherFeatures = otherFeatures;
    }

    public boolean isCurrentLayer()
    {
        return currentLayer;
    }

    public void setCurrentLayer(boolean currentLayer)
    {
        this.currentLayer = currentLayer;
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


}
