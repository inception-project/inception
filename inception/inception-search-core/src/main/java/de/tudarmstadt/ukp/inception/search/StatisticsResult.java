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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class StatisticsResult
{

    private OptionalInt minTokenPerDoc;
    private OptionalInt maxTokenPerDoc;
    private User user;
    private Project project;
    private List<String> metrics;

    private Map<String, Map<String, Double>> allResults;
    private Map<String, Map<String, Double>> nonTrivialResults;

    public StatisticsResult(StatisticRequest aStatisticRequest,
            Map<String, Map<String, Double>> allResults,
            Map<String, Map<String, Double>> nonTrivialResults)
    {
        maxTokenPerDoc = aStatisticRequest.getMaxTokenPerDoc();
        minTokenPerDoc = aStatisticRequest.getMinTokenPerDoc();
        user = aStatisticRequest.getUser();
        project = aStatisticRequest.getProject();
        this.allResults = allResults;
        this.nonTrivialResults = nonTrivialResults;
        //metrics = Arrays.asList(aStatisticRequest.getStatistic().split(","));
        for (String key: allResults.keySet()) {
            for (String metric: allResults.get(key).keySet()) {
                metrics.add(metric);
            }
            break;
        }
    }

    public ArrayList<String> getAllLayers()
    {
        return new ArrayList<String>(allResults.keySet());
    }

    public Map<String, Map<String, Double>> getAllResults()
    {
        return allResults;
    }

    public Map<String, Map<String, Double>> getNonNullResults()
    {
        return nonTrivialResults;
    }

    public ArrayList<String> getNonNullLayers()
    {
        return new ArrayList<String>(nonTrivialResults.keySet());
    }

    public Map<String, Double> getLayerResult(String aLayerName, String aFeatureName) throws ExecutionException
    {
        String fullName = aLayerName + "." + aFeatureName;
        if (!allResults.keySet().contains(fullName)) {
            throw new ExecutionException("The layer " + fullName + " does not exist!");
        }
        return allResults.get(fullName);
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

    public List<String> getMetrics() {return metrics;}

    public Double getStatistic(String aMetric, String aLayerName, String aFeatureName) throws ExecutionException {
        if (!metrics.contains(aMetric)) {
            throw new ExecutionException("The given metric is not valid: " + aMetric);
        }
        return getLayerResult(aLayerName, aFeatureName).get(aMetric);
    }

}
