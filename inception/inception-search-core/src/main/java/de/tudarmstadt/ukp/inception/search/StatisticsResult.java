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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class StatisticsResult
    implements Serializable
{
    private static final long serialVersionUID = 8500724321700472414L;

    private final int minTokenPerDoc;
    private final int maxTokenPerDoc;
    private final User user;
    private final Project project;
    private final String query;
    private final Map<String, LayerStatistics> results;
    private final Map<String, LayerStatistics> nonNullResults;
    private final Set<AnnotationFeature> features;

    public StatisticsResult(StatisticRequest aStatisticRequest,
            Map<String, LayerStatistics> aResults, Set<AnnotationFeature> aFeatures)
    {
        this(aStatisticRequest, aResults, null, aFeatures);
    }

    public StatisticsResult(StatisticRequest aStatisticRequest,
            Map<String, LayerStatistics> aResults, Map<String, LayerStatistics> aNonNullResults,
            Set<AnnotationFeature> aFeatures)
    {
        maxTokenPerDoc = aStatisticRequest.getMaxTokenPerDoc();
        minTokenPerDoc = aStatisticRequest.getMinTokenPerDoc();
        user = aStatisticRequest.getUser();
        project = aStatisticRequest.getProject();
        results = aResults;
        nonNullResults = aNonNullResults;
        query = aStatisticRequest.getQuery();
        features = aFeatures;
    }

    public ArrayList<String> getAllLayerNames()
    {
        return new ArrayList<String>(results.keySet());
    }

    public Map<String, LayerStatistics> getResults()
    {
        return results;
    }

    public Map<String, LayerStatistics> getNonZeroResults()
    {
        return nonNullResults;
    }

    public void featureResultExists(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        String fullName = aLayer.getUiName() + "." + aFeature.getUiName();
        featureResultExists(fullName);
    }

    public void featureResultExists(String featureName) throws ExecutionException
    {
        if (!results.keySet().contains(featureName)) {
            throw new ExecutionException("No statistic results for key: " + featureName);
        }
    }

    public LayerStatistics getLayerResult(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        featureResultExists(aLayer, aFeature);
        return results.get(aLayer.getUiName() + "." + aFeature.getUiName());
    }

    public LayerStatistics getTokenResult() throws ExecutionException
    {
        featureResultExists("Segmentation.token");
        return results.get("Segmentation.token");
    }

    public LayerStatistics getSentenceResult() throws ExecutionException
    {
        featureResultExists("Segmentation.sentence");
        return results.get("Segmentation.sentence");
    }

    public LayerStatistics getQueryResult() throws ExecutionException
    {
        if (query == null) {
            throw new ExecutionException("No query was given!");
        }
        return results.get("query." + query);
    }

    public Project getProject()
    {
        return project;
    }

    public User getUser()
    {
        return user;
    }

    public int getMaxTokenPerDoc()
    {
        return maxTokenPerDoc;
    }

    public int getMinTokenPerDoc()
    {
        return minTokenPerDoc;
    }

    public Set<AnnotationFeature> getFeatures()
    {
        return features;
    }

    public Set<AnnotationLayer> getLayers()
    {
        Set<AnnotationLayer> layers = new HashSet<AnnotationLayer>();
        for (AnnotationFeature feature : getFeatures()) {
            layers.add(feature.getLayer());
        }
        return layers;
    }

    public double getSum(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return getLayerResult(aLayer, aFeature).getSum();
    }

    public double getMinimum(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return getLayerResult(aLayer, aFeature).getMinimum();
    }

    public double getMaximum(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return getLayerResult(aLayer, aFeature).getMaximum();
    }

    public double getMean(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return getLayerResult(aLayer, aFeature).getMean();
    }

    public double getMedian(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return getLayerResult(aLayer, aFeature).getMedian();
    }

    public double getStandardDeviation(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return getLayerResult(aLayer, aFeature).getStandardDeviation();
    }

    public double getMinimumPerSentence(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return getLayerResult(aLayer, aFeature).getMinimumPerSentence();
    }

    public double getMaximumPerSentence(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return getLayerResult(aLayer, aFeature).getMaximumPerSentence();
    }

    public double getMeanPerSentence(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return getLayerResult(aLayer, aFeature).getMeanPerSentence();
    }

    public double getMedianPerSentence(AnnotationLayer aLayer, AnnotationFeature aFeature)
        throws ExecutionException
    {
        return getLayerResult(aLayer, aFeature).getMedianPerSentence();
    }

    public double getStandardDeviationPerSentence(AnnotationLayer aLayer,
            AnnotationFeature aFeature)
        throws ExecutionException
    {
        return getLayerResult(aLayer, aFeature).getStandardDeviationPerSentence();
    }

}
