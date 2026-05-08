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
package de.tudarmstadt.ukp.inception.workload.matrix;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;

import java.util.Map;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.workload.matrix.config.MatrixWorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.matrix.trait.MatrixWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.matrix.trait.MatrixWorkloadTraitsEditor;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link MatrixWorkloadManagerAutoConfiguration#matrixWorkloadExtension}
 * </p>
 */
@Order(-10)
public class MatrixWorkloadExtensionImpl
    implements MatrixWorkloadExtension
{
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String MATRIX_WORKLOAD_MANAGER_EXTENSION_ID = "matrix";

    private final WorkloadManagementService workloadManagementService;
    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userRepository;

    public MatrixWorkloadExtensionImpl(WorkloadManagementService aWorkloadManagementService,
            DocumentService aDocumentService, ProjectService aProjectService,
            UserDao aUserRepository)
    {
        workloadManagementService = aWorkloadManagementService;
        documentService = aDocumentService;
        projectService = aProjectService;
        userRepository = aUserRepository;
    }

    @Override
    public String getId()
    {
        return MATRIX_WORKLOAD_MANAGER_EXTENSION_ID;
    }

    @Override
    public String getLabel()
    {
        return "Static assignment";
    }

    @Override
    public boolean isDocumentRandomAccessAllowed(Project aProject)
    {
        if (projectService.hasRole(userRepository.getCurrentUser(), aProject, CURATOR, MANAGER)) {
            return true;
        }

        WorkloadManager manager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(aProject);
        return readTraits(manager).isRandomDocumentAccessAllowed();
    }

    @Override
    @Transactional
    public MatrixWorkloadTraits readTraits(WorkloadManager aWorkloadManager)
    {
        MatrixWorkloadTraits traits = null;

        try {
            traits = JSONUtil.fromJsonString(MatrixWorkloadTraits.class,
                    aWorkloadManager.getTraits());
        }
        catch (Exception e) {
            this.log.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = new MatrixWorkloadTraits();
        }

        return traits;
    }

    @Override
    @Transactional
    public void writeTraits(WorkloadManager aWorkloadManager, MatrixWorkloadTraits aTraits)
    {
        try {
            aWorkloadManager.setTraits(JSONUtil.toJsonString(aTraits));
        }
        catch (Exception e) {
            this.log.error("Unable to write traits", e);
        }
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<WorkloadManager> aWorkloadManager)
    {
        return new MatrixWorkloadTraitsEditor(aId, aWorkloadManager);
    }

    @Override
    @Transactional
    public void writeTraits(MatrixWorkloadTraits aTraits, Project aProject)
    {
        try {
            WorkloadManager manager = workloadManagementService
                    .loadOrCreateWorkloadManagerConfiguration(aProject);
            this.writeTraits(manager, aTraits);
            workloadManagementService.saveConfiguration(manager);
        }
        catch (Exception e) {
            this.log.error("Unable to write traits", e);
        }
    }

    @Override
    @Transactional
    public ProjectState recalculate(Project aProject)
    {
        var annotators = projectService.listUsersWithRoleInProject(aProject, ANNOTATOR);
        var annDocs = documentService.listAnnotationDocuments(aProject);

        for (var doc : documentService.listSourceDocuments(aProject)) {
            if (isInCuration(doc)) {
                continue;
            }

            var stats = documentService.getAnnotationDocumentStats(doc, annDocs, annotators);

            setSourceDocumentStateBasedOnStats(doc, annotators.size(), stats);
        }

        // Refresh the project stats and recalculate them
        var project = projectService.getProject(aProject.getId());
        var stats = documentService.getSourceDocumentStats(project);
        projectService.setProjectState(aProject, stats.getProjectState());

        return project.getState();
    }

    @Override
    @Transactional
    public ProjectState freshenStatus(Project aProject)
    {
        var annotators = projectService.listUsersWithRoleInProject(aProject, ANNOTATOR);
        var annDocs = documentService.listAnnotationDocuments(aProject);

        // Update the annotation document and source document states for the abandoned documents
        for (var doc : documentService.listSourceDocumentsInState(aProject,
                ANNOTATION_IN_PROGRESS)) {
            if (isInCuration(doc)) {
                continue;
            }

            var stats = documentService.getAnnotationDocumentStats(doc, annDocs, annotators);

            setSourceDocumentStateBasedOnStats(doc, annotators.size(), stats);
        }

        // Refresh the project stats and recalculate them
        var project = projectService.getProject(aProject.getId());
        var stats = documentService.getSourceDocumentStats(project);
        projectService.setProjectState(aProject, stats.getProjectState());

        return project.getState();
    }

    @Override
    @Transactional
    public void updateDocumentState(SourceDocument aDocument, int aAnnotatorCount)
    {
        if (isInCuration(aDocument)) {
            return;
        }

        var stats = documentService.getAnnotationDocumentStats(aDocument);

        setSourceDocumentStateBasedOnStats(aDocument, aAnnotatorCount, stats);
    }

    /**
     * If the SOURCE document is already in curation, we do not touch the state anymore
     */
    private boolean isInCuration(SourceDocument aDocument)
    {
        return aDocument.getState() == CURATION_FINISHED
                || aDocument.getState() == CURATION_IN_PROGRESS;
    }

    private void setSourceDocumentStateBasedOnStats(SourceDocument aDocument, int aAnnotatorCount,
            Map<AnnotationDocumentState, Long> stats)
    {
        var ignoreCount = stats.get(AnnotationDocumentState.IGNORE);
        var finishedCount = stats.get(AnnotationDocumentState.FINISHED);
        var newCount = stats.get(AnnotationDocumentState.NEW);

        // If all documents are ignored or finished, we set the source document to finished
        if ((finishedCount + ignoreCount) == aAnnotatorCount) {
            documentService.setSourceDocumentState(aDocument, ANNOTATION_FINISHED);
        }
        // ... or we set it to new if there is at least one new document and the others are ignored
        else if ((newCount + ignoreCount) == aAnnotatorCount) {
            documentService.setSourceDocumentState(aDocument, SourceDocumentState.NEW);
        }
        else {
            documentService.setSourceDocumentState(aDocument, ANNOTATION_IN_PROGRESS);
        }
    }
}
