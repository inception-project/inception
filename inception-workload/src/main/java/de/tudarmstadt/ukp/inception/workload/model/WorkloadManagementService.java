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
package de.tudarmstadt.ukp.inception.workload.model;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public interface WorkloadManagementService
{
    WorkloadManager getOrCreateWorkloadManagerConfiguration(Project aProject);

    void setWorkloadManagerConfiguration(String aWorkloadType, Project aProject);

    void setWorkloadManagerConfiguration(String aWorkloadType, String traits, Project aProject);

    List<AnnotationDocument> getAnnotationDocumentsForSpecificState(AnnotationDocumentState aState,
            Project aProject, User aUser);

    List<User> getUsersForSpecificDocumentAndState(AnnotationDocumentState aState,
            SourceDocument aSourceDocumentt, Project aProject);

    int getAmountOfUsersWorkingOnADocument(SourceDocument aDocument, Project aProject);

}
