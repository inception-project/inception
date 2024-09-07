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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static org.apache.commons.collections4.CollectionUtils.containsAny;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.persistence.NoResultException;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentServiceAutoConfiguration#documentAccess}.
 * </p>
 */
public class DocumentAccessImpl
    implements DocumentAccess
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final UserDao userService;
    private final ProjectService projectService;
    private final DocumentService documentService;

    public DocumentAccessImpl(ProjectService aProjectService, UserDao aUserService,
            DocumentService aDocumentService)
    {
        userService = aUserService;
        projectService = aProjectService;
        documentService = aDocumentService;
    }

    @Override
    public boolean canViewAnnotationDocument(String aProjectId, String aDocumentId, String aUser)
    {
        return canViewAnnotationDocument(userService.getCurrentUsername(), aProjectId,
                Long.valueOf(aDocumentId), aUser);
    }

    @Override
    public boolean canViewAnnotationDocument(String aSessionOwner, String aProjectId,
            long aDocumentId, String aAnnotator)
    {
        log.trace(
                "Permission check: canViewAnnotationDocument [aSessionOwner: {}] [project: {}] "
                        + "[document: {}] [annotator: {}]",
                aSessionOwner, aProjectId, aDocumentId, aAnnotator);

        try {
            var user = getUser(aSessionOwner);
            var project = getProject(aProjectId);

            var permissionLevels = projectService.listRoles(project, user);

            // Does the user have the permission to access the project at all?
            if (permissionLevels.isEmpty()) {
                log.trace("Access denied: User {} has no acccess to project {}", user, project);
                return false;
            }

            // Managers and curators can see anything
            if (containsAny(permissionLevels, MANAGER, CURATOR)) {
                log.trace("Access granted: User {} can view annotations [{}] as MANGER or CURATOR",
                        user, aDocumentId);
                return true;
            }

            // Annotators can only see their own documents
            if (!aSessionOwner.equals(aAnnotator)) {
                log.trace(
                        "Access denied: User {} tries to see annotations from [{}] but can only see own annotations",
                        user, aAnnotator);
                return false;
            }

            // Annotators cannot view blocked documents
            var doc = documentService.getSourceDocument(project.getId(), aDocumentId);
            if (documentService.existsAnnotationDocument(doc, aAnnotator)) {
                var aDoc = documentService.getAnnotationDocument(doc, aAnnotator);
                if (aDoc.getState() == AnnotationDocumentState.IGNORE) {
                    log.trace("Access denied: Document {} is locked (IGNORE) for user {}", aDoc,
                            aAnnotator);
                    return false;
                }
            }

            log.trace(
                    "Access granted: canViewAnnotationDocument [aSessionOwner: {}] [project: {}] "
                            + "[document: {}] [annotator: {}]",
                    aSessionOwner, aProjectId, aDocumentId, aAnnotator);
            return true;
        }
        catch (NoResultException | AccessDeniedException e) {
            log.trace("Access denied: prerequisites not met", e);
            // If any object does not exist, the user cannot view
            return false;
        }
    }

    @Override
    public boolean canEditAnnotationDocument(String aSessionOwner, String aProjectId,
            long aDocumentId, String aAnnotator)
    {
        log.trace(
                "Permission check: canEditAnnotationDocument [aSessionOwner: {}] [project: {}] "
                        + "[document: {}] [annotator: {}]",
                aSessionOwner, aProjectId, aDocumentId, aAnnotator);

        try {
            var sessionOwner = getUser(aSessionOwner);
            var project = getProject(aProjectId);
            var doc = documentService.getSourceDocument(project.getId(), aDocumentId);

            assertCanEditAnnotationDocument(sessionOwner, doc, aAnnotator);

            return true;
        }
        catch (NoResultException | AccessDeniedException e) {
            // If any object does not exist, the user cannot edit
            return false;
        }
    }

    @Override
    public void assertCanEditAnnotationDocument(User aSessionOwner, SourceDocument aDocument,
            String aDataOwner)
    {
        var project = aDocument.getProject();

        // Is the user a curator?
        if (projectService.hasRole(aSessionOwner, project, CURATOR)) {
            // If curation is already done, document is no longer editable
            if (CURATION_USER.equals(aDataOwner)) {
                if (aDocument.getState() == CURATION_FINISHED) {
                    throw new AccessDeniedException(
                            "Curation is already finished. You can put it back "
                                    + "into progress via the monitoring page.");
                }

                return; // Access granted
            }

            // Fall-through - user may still be an annotator
        }

        // Is the user an annotator?
        if (projectService.hasRole(aSessionOwner, project, ANNOTATOR)) {
            // Annotators can edit their own annotations
            if (!aSessionOwner.getUsername().equals(aDataOwner)) {
                throw new AccessDeniedException(
                        "Viewing another users annotations - document is read-only!");
            }

            // Blocked or finished documents cannot be edited
            if (documentService.existsAnnotationDocument(aDocument, aDataOwner)) {
                var aDoc = documentService.getAnnotationDocument(aDocument, aDataOwner);
                if (aDoc.getState() == AnnotationDocumentState.FINISHED) {
                    throw new AccessDeniedException("This document is already closed for user ["
                            + aDataOwner + "]. Please ask your "
                            + "project manager to re-open it via the monitoring page.");
                }

                if (aDoc.getState() == AnnotationDocumentState.IGNORE) {
                    throw new AccessDeniedException("This document is blocked for user ["
                            + aDataOwner + "]. Please ask your "
                            + "project manager if you believe this is wrong.");
                }
            }

            return; // Access granted
        }

        throw new AccessDeniedException("You have no permission to edit this document");
    }

    @Override
    public boolean canExportAnnotationDocument(User aSessionOwner, Project aProject)
    {
        if (aProject == null) {
            return false;
        }

        if (projectService.hasRole(aSessionOwner, aProject, MANAGER)) {
            return true;
        }

        return !aProject.isDisableExport();
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
