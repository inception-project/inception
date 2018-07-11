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
package de.tudarmstadt.ukp.inception.recommendation.imls.dl4j.pos;

import java.io.File;

import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;

/**
 * The base configuration for the implemented DL4J classification tool. It sets up the path to the
 * tagset file (dl4j_tagset.bin), initially and every time the model file changes.
 * 
 *
 *
 */
public class BaseConfiguration
    extends ClassifierConfiguration<DL4JConfigurationParameters>
{
    public BaseConfiguration(long aRecommenderId)
    {
        DL4JConfigurationParameters params = new DL4JConfigurationParameters();
        params.setTagsetFile(new File(super.getModelFile().getParentFile(), "dl4j_tageset.bin"));
        this.setParams(params);
        setRecommenderId(aRecommenderId);
    }

    @Override
    public void setModelFile(File modelFile)
    {
        super.setModelFile(modelFile);
        this.getParams()
                .setTagsetFile(new File(super.getModelFile().getParentFile(), "dl4j_tageset.bin"));
    }
}
