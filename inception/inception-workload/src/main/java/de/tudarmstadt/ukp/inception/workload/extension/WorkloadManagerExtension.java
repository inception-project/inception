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
package de.tudarmstadt.ukp.inception.workload.extension;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint.Extension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

/**
 * Extensions for the workload manager. Also has a readTraits and writeTraits property for the DB
 * entry traits
 */
public interface WorkloadManagerExtension<T>
    extends Extension<Project>
{
    @Override
    default boolean accepts(Project project)
    {
        return true;
    }

    String getLabel();

    T readTraits(WorkloadManager aWorkloadManager);

    void writeTraits(WorkloadManagementService aWorkloadManagementService, T aTrait,
            Project aProject);

    /**
     * Ask the workload manager to immediately refresh the state of the documents and overall
     * project. This can be called immediately before fetching the project status in order to ensure
     * that the project status is reliable.
     */
    ProjectState freshenStatus(Project aProject);
}
