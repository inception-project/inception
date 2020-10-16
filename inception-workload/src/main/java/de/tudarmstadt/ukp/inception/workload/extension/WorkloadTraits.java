/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.inception.workload.extension;

import java.io.Serializable;

/**
 * Traits for Workload
 */

public class WorkloadTraits implements Serializable
{
    private static final long serialVersionUID = -7075354549983394848L;

    private String workloadType;
    private int defaultNumberOfAnnotations;

    public WorkloadTraits()
    {
        workloadType = "default";
        defaultNumberOfAnnotations = 6;
    }

    public WorkloadTraits(String aWorkloadType, int aDefaultNumberOfAnnotations)
    {
        workloadType = aWorkloadType;
        defaultNumberOfAnnotations = aDefaultNumberOfAnnotations;
    }

    public String getWorkloadType()
    {
        return workloadType;
    }

    public void setWorkloadType(String workloadType)
    {
        this.workloadType = workloadType;
    }

    public int getDefaultNumberOfAnnotations()
    {
        return defaultNumberOfAnnotations;
    }

    public void setDefaultNumberOfAnnotations(int defaultNumberOfAnnotations)
    {
        this.defaultNumberOfAnnotations = defaultNumberOfAnnotations;
    }
}
