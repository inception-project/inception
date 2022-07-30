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
package de.tudarmstadt.ukp.inception.documents;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;

import java.util.List;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentServiceAutoConfiguration#documentAccess}.
 * </p>
 */
public class DocumentAccess
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final UserDao userService;
    private final ProjectService projectService;
    private final DocumentService documentService;

    public DocumentAccess(ProjectService aProjectService, UserDao aUserService,
            DocumentService aDocumentService)
    {
        userService = aUserService;
        projectService = aProjectService;
        documentService = aDocumentService;
    }

    public boolean canViewAnnotationDocument(String aProjectId, String aDocumentId, String aUser)
    {
        return canViewAnnotationDocument(userService.getCurrentUsername(), aProjectId,
                Long.valueOf(aDocumentId), aUser);
    }

    public boolean canViewAnnotationDocument(String aUser, String aProjectId, long aDocumentId,
            String aAnnotator)
    {
        log.trace(
                "Permission check: canViewAnnotationDocument [user: {}] [project: {}] [document: {}] [annotator: {}]",
                aUser, aProjectId, aDocumentId, aAnnotator);

        try {
            User user = getUser(aUser);
            Project project = getProject(aProjectId);

            List<PermissionLevel> permissionLevels = projectService.listRoles(project, user);

            // Does the user have the permission to access the project at all?
            if (permissionLevels.isEmpty()) {
                return false;
            }

            // Users can see their own annotations and manager can see annotations of all users
            if (!aUser.equals(aAnnotator) && !permissionLevels.contains(MANAGER)) {
                return false;
            }

            // Blocked documents cannot be viewed
            SourceDocument doc = documentService.getSourceDocument(project.getId(), aDocumentId);
            if (documentService.existsAnnotationDocument(doc, aAnnotator)) {
                AnnotationDocument aDoc = documentService.getAnnotationDocument(doc, aAnnotator);
                if (aDoc.getState() == AnnotationDocumentState.IGNORE) {
                    return false;
                }
            }

            return true;
        }
        catch (NoResultException | AccessDeniedException e) {
            // If any object does not exist, the user cannot view
            return false;
        }
    }

    public boolean canEditAnnotationDocument(String aUser, String aProjectId, long aDocumentId,
            String aAnnotator)
    {
        log.trace(
                "Permission check: canEditAnnotationDocument [user: {}] [project: {}] [document: {}] [annotator: {}]",
                aUser, aProjectId, aDocumentId, aAnnotator);

        try {
            User user = getUser(aUser);
            Project project = getProject(aProjectId);

            // Does the user have the permission to access the project at all?
            if (!projectService.hasRole(user, project, PermissionLevel.ANNOTATOR)) {
                return false;
            }

            // Users can edit their own annotations
            if (!aUser.equals(aAnnotator)) {
                return false;
            }

            // Blocked documents cannot be edited
            SourceDocument doc = documentService.getSourceDocument(project.getId(), aDocumentId);
            if (documentService.existsAnnotationDocument(doc, aAnnotator)) {
                AnnotationDocument aDoc = documentService.getAnnotationDocument(doc, aAnnotator);
                if (aDoc.getState() == AnnotationDocumentState.IGNORE) {
                    return false;
                }
            }

            return true;
        }
        catch (NoResultException | AccessDeniedException e) {
            // If any object does not exist, the user cannot edit
            return false;
        }
    }

    private Project getProject(String aProjectId)
    {
        try {
            if (StringUtils.isNumeric(aProjectId)) {
                return projectService.getProject(Long.valueOf(aProjectId));
            }

            return projectService.getProjectBySlug(aProjectId);
        }
        catch (NoResultException e) {
            throw new AccessDeniedException("Project [" + aProjectId + "] does not exist");
        }
    }

    private User getUser(String aUser)
    {
        User user = userService.get(aUser);

        // Does the user exist and is enabled?
        if (user == null || !user.isEnabled()) {
            throw new AccessDeniedException(
                    "User [" + aUser + "] does not exist or is not enabled");
        }

        return user;
    }
}
