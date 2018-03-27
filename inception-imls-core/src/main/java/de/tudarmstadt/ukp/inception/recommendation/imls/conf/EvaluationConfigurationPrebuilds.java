/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.imls.conf;

public class EvaluationConfigurationPrebuilds
{
    
    /**
     * Provides convenience functions to get default EvaluationConfigurations. 
     */
    private EvaluationConfigurationPrebuilds()
    {
    }
    
    public static EvaluationConfiguration getDefaultConfiguration() {
        return new EvaluationConfiguration();
    }
    
    public static EvaluationConfiguration getLimitedTrainingSetConfiguration(int limit) {
        EvaluationConfiguration conf = new EvaluationConfiguration();
        conf.setTrainingSetSizeLimit(limit);
        conf.setUseHoldout(false);
        conf.setIncrementStrategy("fibonacciIncrementStrategy");
        return conf;
    }
    
    public static EvaluationConfiguration getShuffledTrainingSetConfiguration() {
        EvaluationConfiguration conf = new EvaluationConfiguration();
        conf.setShuffleTrainingSet(true);
        conf.setUseHoldout(false);
        conf.setIncrementStrategy("fibonacciIncrementStrategy");
        return conf;
    }
}
