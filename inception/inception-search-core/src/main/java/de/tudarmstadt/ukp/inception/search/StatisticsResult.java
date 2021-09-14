/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.search;

import java.util.ArrayList;
import java.util.Map;
import java.util.OptionalInt;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class StatisticsResult
{

    private OptionalInt minTokenPerDoc;
    private OptionalInt maxTokenPerDoc;
    private User user;
    private Project project;
    private String query;
    private Map<String, LayerStatistics> allResults;
    private Map<String, LayerStatistics> nonTrivialResults;

    public StatisticsResult(StatisticRequest aStatisticRequest,
            Map<String, LayerStatistics> allResults, Map<String, LayerStatistics> nonTrivialResults)
    {
        maxTokenPerDoc = aStatisticRequest.getMaxTokenPerDoc();
        minTokenPerDoc = aStatisticRequest.getMinTokenPerDoc();
        user = aStatisticRequest.getUser();
        project = aStatisticRequest.getProject();
        this.allResults = allResults;
        this.nonTrivialResults = nonTrivialResults;
        query = aStatisticRequest.getQuery();

    }

    public ArrayList<String> getAllLayers()
    {
        return new ArrayList<String>(allResults.keySet());
    }

    public Map<String, LayerStatistics> getAllResults()
    {
        return allResults;
    }

    public Map<String, LayerStatistics> getNonNullResults()
    {
        return nonTrivialResults;
    }

    public ArrayList<String> getNonNullLayers()
    {
        return new ArrayList<String>(nonTrivialResults.keySet());
    }

    public LayerStatistics getLayerResult(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        String fullName = aLayer.getUiName() + "." + aFeature.getUiName();
        if (!allResults.keySet().contains(fullName)) {
            throw new ExecutionException("No results for layer " + fullName);
        }
        return allResults.get(fullName);
    }

    public LayerStatistics getTokenResult()
    {
        return allResults.get("Token Count");
    }

    public LayerStatistics getSentenceResult()
    {
        return allResults.get("Sentence Count");
    }

    public LayerStatistics getQueryResult() throws ExecutionException
    {
        if (query == null) {
            throw new ExecutionException("No query was given!");
        }
        return allResults.get(query);
    }

    public Project getProject()
    {
        return project;
    }

    public User getUser()
    {
        return user;
    }

    public OptionalInt getMaxTokenPerDoc()
    {
        return maxTokenPerDoc;
    }

    public OptionalInt getMinTokenPerDoc()
    {
        return minTokenPerDoc;
    }

    public long getTotal(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return allResults.get(aLayer.getUiName() + "." + aFeature.getUiName()).getTotal();
    }

    public long getMinimum(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return allResults.get(aLayer.getUiName() + "." + aFeature.getUiName()).getMinimum();
    }

    public long getMaximum(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return allResults.get(aLayer.getUiName() + "." + aFeature.getUiName()).getMaximum();
    }

    public double getMean(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return allResults.get(aLayer.getUiName() + "." + aFeature.getUiName()).getMean();
    }

    public double getMedian(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return allResults.get(aLayer.getUiName() + "." + aFeature.getUiName()).getMedian();
    }

    public double getStandardDeviation(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return allResults.get(aLayer.getUiName() + "." + aFeature.getUiName())
                .getStandardDeviation();
    }

    public double getMinimumPerSentence(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return allResults.get(aLayer.getUiName() + "." + aFeature.getUiName())
                .getMinimumPerSentence();
    }

    public double getMaximumPerSentence(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return allResults.get(aLayer.getUiName() + "." + aFeature.getUiName())
                .getMaximumPerSentence();
    }

    public double getMeanPerSentence(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return allResults.get(aLayer.getUiName() + "." + aFeature.getUiName()).getMeanPerSentence();
    }

    public double getMedianPerSentence(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return allResults.get(aLayer.getUiName() + "." + aFeature.getUiName())
                .getMedianPerSentence();
    }

    public double getStandardDeviationPerSentence(AnnotationLayer aLayer,
            AnnotationFeature aFeature)
        throws ExecutionException
    {
        return allResults.get(aLayer.getUiName() + "." + aFeature.getUiName())
                .getStandardDeviationPerSentence();
    }

}
