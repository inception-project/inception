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
package de.tudarmstadt.ukp.inception.recommendation.imls.mira.pos;

import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;

/**
 * The base configuration for the implemented MIRA classification tool.
 * 
 * Uses the following parameters for the MIRA algorithm:
 * <ul>
 * <li>Beam Size = 0</li>
 * <li>Clip = 1</li>
 * <li>Max Posteriors = false</li>
 * <li>Template = null (has to be set later)</li>
 * <li>Iterations = 10</li>
 * </ul>
 * 
 *
 *
 */
public class BaseConfiguration
    extends ClassifierConfiguration<MiraConfigurationParameters>
{

    public BaseConfiguration()
    {
        MiraConfigurationParameters params = new MiraConfigurationParameters();
        params.setBeamSize(0);
        params.setClip(1);
        params.setMaxPosteriors(false);
        params.setTemplate(null);
        params.setIterations(10);
        this.setParams(params);
    }
    
    public BaseConfiguration(String feature)
    {
        this();
        setFeature(feature);
    }

    /**
     * Returns the default
     * <a href="https://github.com/benob/miralium/blob/master/examples/pos-simple.template" target="_blank">simple template</a>
     * 
     * @return An InputStream to the simple template used in the MIRA algorithm.
     */
    public static String getDefaultTemplate()
    {
        return "U%x[0,0]\nB";
    }
}
