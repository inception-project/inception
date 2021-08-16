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

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class StatisticsResult
{

    private Double upperDocSize;
    private Double lowerDocSize;
    private User user;
    private Project project;

    private Map<String, Map<String, Double>> allResults;
    private Map<String, Map<String, Double>> nonTrivialResults;

    public StatisticsResult(StatisticRequest aStatisticRequest,
            Map<String, Map<String, Double>> allResults,
            Map<String, Map<String, Double>> nonTrivialResults)
    {
        upperDocSize = aStatisticRequest.getUpperDocumentSize();
        lowerDocSize = aStatisticRequest.getLowerDocumentSize();
        user = aStatisticRequest.getUser();
        project = aStatisticRequest.getProject();
        this.allResults = allResults;
        this.nonTrivialResults = nonTrivialResults;
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

    public Map<String, Double> getLayerResult(String layerName) throws ExecutionException
    {
        if (!allResults.keySet().contains(layerName)) {
            throw new ExecutionException("The layer" + layerName + "does not exist!");
        }
        return allResults.get(layerName);
    }

    public Project getProject()
    {
        return project;
    }

    public User getUser()
    {
        return user;
    }

    public Double getUpperDocSize()
    {
        return upperDocSize;
    }

    public Double getLowerDocSize()
    {
        return lowerDocSize;
    }

}
