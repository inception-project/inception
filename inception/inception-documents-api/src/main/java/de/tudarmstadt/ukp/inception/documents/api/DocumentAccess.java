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
package de.tudarmstadt.ukp.inception.documents.api;

import org.springframework.security.access.AccessDeniedException;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.AccessCheckingBean;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public interface DocumentAccess
    extends AccessCheckingBean
{
    boolean canViewAnnotationDocument(String aProjectId, String aDocumentId, String aUser);

    boolean canViewAnnotationDocument(String aUser, String aProjectId, long aDocumentId,
            String aAnnotator);

    boolean canEditAnnotationDocument(String aUser, String aProjectId, long aDocumentId,
            String aAnnotator);

    void assertCanEditAnnotationDocument(User aSessionOwner, SourceDocument aDocument,
            String aDataOwner)
        throws AccessDeniedException;

    boolean canExportAnnotationDocument(User aUser, Project aProject);

}
