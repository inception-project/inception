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
package de.tudarmstadt.ukp.inception.workload.matrix.service;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.Validate;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

public class MatrixWorkloadServiceImpl
    implements MatrixWorkloadService
{
    private final DocumentService documentService;
    private final ProjectService projectService;

    public MatrixWorkloadServiceImpl(DocumentService aDocumentService,
            ProjectService aProjectService)
    {
        documentService = aDocumentService;
        projectService = aProjectService;
    }

    @Override
    public void assignWorkload(Project aProject, int aAnnotatorsPerDocument, boolean aReopen)
    {
        Validate.validState(aAnnotatorsPerDocument > 0, "Annotators per document must be positive");

        var documents = documentService.listSourceDocuments(aProject);

        var documentsPerUser = new LinkedHashMap<String, AtomicInteger>();

        // Pre-load the map so we have a stable order
        var annotators = projectService.listUsersWithRoleInProject(aProject, ANNOTATOR).stream() //
                .map(User::getUsername) //
                .sorted() //
                .toList();
        for (var annotator : annotators) {
            documentsPerUser.put(annotator, new AtomicInteger(0));
        }

        for (var document : documents) {
            var annDocs = documentService.listAllAnnotationDocuments(document);
            var annDocByUser = new HashMap<String, AnnotationDocument>();

            var takenCount = 0;
            var takenUsers = new HashSet<>();
            for (var annDoc : annDocs) {
                annDocByUser.put(annDoc.getUser(), annDoc);

                // Tally the documents already assigned to the annotators
                if (annDoc.getState().isTaken()) {
                    var tally = documentsPerUser.get(annDoc.getUser());
                    if (tally != null) {
                        tally.incrementAndGet();
                        takenUsers.add(annDoc.getUser());
                        takenCount++;
                    }
                }
            }

            // Find users to "assign" the current document to if necessary. We do not actually
            // assign it, but we don't lock the document for these users.
            var annDocsToUnlock = new ArrayList<AnnotationDocument>();
            while (takenCount < aAnnotatorsPerDocument) {
                var maybeUserToAssign = annotators.stream() //
                        .filter($ -> !takenUsers.contains($)) //
                        .filter($ -> aReopen || !isLocked($, annDocByUser))
                        .sorted(comparing($ -> documentsPerUser.get($).get())) //
                        .findFirst();

                if (maybeUserToAssign.isEmpty()) {
                    // No more users we can assign the document to - give up
                    break;
                }

                var userToAssign = maybeUserToAssign.get();

                var annDoc = annDocByUser.get(userToAssign);
                if (annDoc != null && annDoc.getState() == IGNORE) {
                    annDocsToUnlock.add(annDoc);
                }

                documentsPerUser.get(userToAssign).incrementAndGet();
                takenUsers.add(userToAssign);
                takenCount++;
            }

            // Lock remaining documents
            var annDocsToLock = new ArrayList<AnnotationDocument>();
            for (var annotator : annotators) {
                if (takenUsers.contains(annotator)) {
                    continue;
                }

                var annDoc = annDocByUser.get(annotator);
                if (annDoc == null) {
                    annDoc = new AnnotationDocument(annotator, document);
                }
                annDocsToLock.add(annDoc);
            }

            documentService.bulkSetAnnotationDocumentState(annDocsToUnlock, NEW);
            documentService.bulkSetAnnotationDocumentState(annDocsToLock, IGNORE);
        }
    }

    private boolean isLocked(String aDataOwner, HashMap<String, AnnotationDocument> aAnnDocByUser)
    {
        var annDoc = aAnnDocByUser.get(aDataOwner);

        if (annDoc == null) {
            return false;
        }

        return annDoc.getState() == IGNORE;
    }
}
