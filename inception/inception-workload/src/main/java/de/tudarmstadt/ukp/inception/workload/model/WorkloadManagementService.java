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
package de.tudarmstadt.ukp.inception.workload.model;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtension;

/**
 * Interface for all required DB calls. Short explanation given in the Interface implementation
 * class WorkloadManagementServiceImpl
 */
public interface WorkloadManagementService
{
    /**
     * @return for a given project a WorkloadManager object. Also applicable for older INCEpTION
     *         version where the workload feature was not present. Also, if no entity can be found,
     *         a new entry will be created and returned.
     * @param aProject
     *            a project
     */
    WorkloadManager loadOrCreateWorkloadManagerConfiguration(Project aProject);

    WorkloadManagerExtension<?> getWorkloadManagerExtension(Project aProject);

    void saveConfiguration(WorkloadManager aManager);

    List<AnnotationDocument> listAnnotationDocumentsForSourceDocumentInState(
            SourceDocument aSourceDocument, AnnotationDocumentState aState);

    Long getNumberOfUsersWorkingOnADocument(SourceDocument aDocument);
}
