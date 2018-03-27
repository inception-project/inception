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

public class MiraConfigurationParameters
{
    private String template;
    private int clip;
    private boolean maxPosteriors;
    private int beamSize;
    private int iterations;
    
    public String getTemplate()
    {
        return template;
    }
    public void setTemplate(String template)
    {
        this.template = template;
    }
    public int getClip()
    {
        return clip;
    }
    public void setClip(int clip)
    {
        this.clip = clip;
    }
    public boolean isMaxPosteriors()
    {
        return maxPosteriors;
    }
    public void setMaxPosteriors(boolean maxPosteriors)
    {
        this.maxPosteriors = maxPosteriors;
    }
    public int getBeamSize()
    {
        return beamSize;
    }
    public void setBeamSize(int beamSize)
    {
        this.beamSize = beamSize;
    }
    public int getIterations()
    {
        return iterations;
    }
    public void setIterations(int iterations)
    {
        this.iterations = iterations;
    }
    
}
