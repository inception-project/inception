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
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

/**
 * Extensions for the workload manager. Also has a readTraits and writeTraits property for the DB
 * entry traits
 * 
 * @param <T>
 *            traits type
 */
public interface WorkloadManagerExtension<T>
    extends Extension<Project>
{
    public static final String WORKLOAD_ACTION_BAR_ROLE = "Workload";

    @Override
    default boolean accepts(Project project)
    {
        return true;
    }

    String getLabel();

    T readTraits(WorkloadManager aWorkloadManager);

    void writeTraits(T aTrait, Project aProject);

    /**
     * Ask the workload manager to immediately recalculate the state of all documents in the project
     * and of the project itself. This is necessary when switching from one workload manager to
     * another.
     * 
     * @param aProject
     *            a project
     * @return the state of the project after the recalculation has been completed
     */
    ProjectState recalculate(Project aProject);

    /**
     * Ask the workload manager to immediately refresh the state of the documents and overall
     * project. This can be called immediately before fetching the project status in order to ensure
     * that the project status is reliable.
     * 
     * @param aProject
     *            a project
     * @return the state of the project after the freshening has been completed
     */
    ProjectState freshenStatus(Project aProject);

    /**
     * @return whether the current user can access documents in any order or if the workload manager
     *         assigns the order.
     * 
     *         <b>NOTE:</b> This is currently used to control the visibiltiy of the activities
     *         dashlet on on the project dashboard. A better approach would be to modularize the
     *         dashboard and then have some factory in the workload modules inject the dashlet
     *         instead.
     * 
     * @param aProject
     *            a project
     */
    boolean isDocumentRandomAccessAllowed(Project aProject);
}
