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
package de.tudarmstadt.ukp.inception.workload.dynamic.manager;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public interface WorkloadAndWorkflowService
{
    String SERVICE_NAME = "workloadAndWorkflow";

    String getWorkflowManager(Project aProject);
    String getWorkloadManager(Project aProject);
    String getWorkflowType(Project aProject);
    Integer getDefaultAnnotations(Project aProject);

    void setWorkflowManager(String aType, Project aProject);
    void setWorkloadManager(String aType, Project aProject);
    void setWorkflowType(String aType, Project aProject);
    void setDefaultAnnotations(int aDefaultAnnotationsNumber, Project aProject);
}
