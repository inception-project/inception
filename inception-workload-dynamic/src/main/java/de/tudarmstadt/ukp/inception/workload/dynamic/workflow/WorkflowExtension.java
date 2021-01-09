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
package de.tudarmstadt.ukp.inception.workload.dynamic.workflow;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint.Extension;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

/**
 * Extensions for the workflow.
 */
public interface WorkflowExtension
    extends Extension<Project>
{
    @Override
    default boolean accepts(Project project)
    {
        return true;
    }

    String getLabel();

    /**
     * @return List of {@link SourceDocument} changed as required by the specific workflow strategy
     */
    List<SourceDocument> rankDocuments(List<SourceDocument> aSourceDocuments);

    /**
     * @return true when a new document has been loaded, otherwise false
     *
     *         Next document will be loaded, same for all workflow extensions
     */
    default boolean loadNextDocument(List<SourceDocument> aSourceDocuments, Project aProject,
            WorkloadManager aCurrentWorkload, AnnotationPageBase aPage, AjaxRequestTarget aTarget,
            WorkloadManagementService workloadManagementService,
            DynamicWorkloadExtension dynamicWorkloadExtension, DocumentService documentService)
    {
        // Go through all documents of the list
        for (SourceDocument doc : aSourceDocuments) {
            // Check if there are less annotators working on the selected document than
            // the default number of annotation set by the project manager
            if ((workloadManagementService.getNumberOfUsersWorkingOnADocument(doc,
                    aProject)) < (dynamicWorkloadExtension.readTraits(aCurrentWorkload)
                            .getDefaultNumberOfAnnotations())) {
                // This was the case, so load the document and return
                aPage.getModelObject().setDocument(doc,
                        documentService.listSourceDocuments(aProject));
                aPage.actionLoadDocument(aTarget);
                return true;
            }
        }
        return false;
    }
}
