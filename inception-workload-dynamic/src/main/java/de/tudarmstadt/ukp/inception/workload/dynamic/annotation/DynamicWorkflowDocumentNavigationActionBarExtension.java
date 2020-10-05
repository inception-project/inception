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
package de.tudarmstadt.ukp.inception.workload.dynamic.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension.DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID;
import static de.tudarmstadt.ukp.inception.workload.dynamic.workflow.DynamicRandomizedWorkflowTypeExtension.RANDOMIZED_WORKFLOW_EXTENSION_ID;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.docnav.DefaultDocumentNavigatorActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.workload.dynamic.model.DynamicWorkflowManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

public class DynamicWorkflowDocumentNavigationActionBarExtension
    implements ActionBarExtension
{
    private final DocumentService documentService;
    private final WorkloadManagementService workloadManagementService;
    private final DynamicWorkflowManagementService dynamicWorkflowManagementService;
    private List<AnnotationDocument> annotationDocumentList;

    private final @PersistenceContext EntityManager entityManager;

    @Autowired
    public DynamicWorkflowDocumentNavigationActionBarExtension(DocumentService aDocumentService,
            EntityManager aEntityManager, WorkloadManagementService aWorkloadManagementService,
            DynamicWorkflowManagementService aDynamicWorkflowManagementService)
    {
        documentService = aDocumentService;
        entityManager = aEntityManager;
        workloadManagementService = aWorkloadManagementService;
        dynamicWorkflowManagementService = aDynamicWorkflowManagementService;
    }

    @Override
    public String getRole()
    {
        return DefaultDocumentNavigatorActionBarExtension.class.getName();
    }

    @Override
    public int getPriority()
    {
        return 1;
    }

    @Override
    public boolean accepts(AnnotationPageBase aPage)
    {
        // #Issue 1813 fix
        if (aPage.getModelObject().getProject() == null) {
            return false;
        }
        return DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID.equals(workloadManagementService.
            getOrCreateWorkloadManagerConfiguration(aPage.getModelObject().getProject())
            .getType());
    }

    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage)
    {
        return new DynamicDocumentNavigator(aId);
    }

    // Init of the page, select a random document
    @Override
    public void onInitialize(AnnotationPageBase aPage)
    {
        User user = aPage.getModelObject().getUser();
        Project project = aPage.getModelObject().getProject();
        annotationDocumentList = documentService.listAnnotationDocuments(project, user);
        // Check if there is a document in progress and return this one
        for (AnnotationDocument annotationDocument : annotationDocumentList) {
            // There was one in progress, load it
            if (annotationDocument.getState().equals(IN_PROGRESS)) {
                aPage.getModelObject().setDocument(annotationDocument.getDocument(),
                        documentService.listSourceDocuments(project));
                Optional<AjaxRequestTarget> target = RequestCycle.get()
                        .find(AjaxRequestTarget.class);
                aPage.actionLoadDocument(target.orElse(null));
                return;
            }
        }

        // Nothing in progress found
        if (aPage.getModelObject().getDocument() == null) {
            // Check which workflow type is active (switch used for easily adding new types)
            switch (dynamicWorkflowManagementService.getOrCreateWorkflowEntry(project)
                    .getWorkflow()) {
            case (RANDOMIZED_WORKFLOW_EXTENSION_ID):
                // Go through all documents in a random order and check if there
                // is a Annotation document with the state NEW
                String query = "FROM SourceDocument " + "WHERE project = :project "
                        + "ORDER BY rand()";
                for (SourceDocument doc : entityManager.createQuery(query, SourceDocument.class)
                        .setParameter("project", project).getResultList()) {
                    if ((getUsersWorkingOnTheDocument(doc) + 1) <= dynamicWorkflowManagementService
                            .getOrCreateWorkflowEntry(project).getDefaultAnnotations()) {
                        if (documentService.listAnnotatableDocuments(project, user).get(doc) == null
                                || (documentService.listAnnotatableDocuments(project, user).get(doc)
                                        .getState().equals(NEW))) {
                            aPage.getModelObject().setDocument(doc,
                                    documentService.listSourceDocuments(project));
                            Optional<AjaxRequestTarget> target = RequestCycle.get()
                                    .find(AjaxRequestTarget.class);
                            aPage.actionLoadDocument(target.orElse(null));
                            return;
                        }
                    }
                }

                // No documents left
                aPage.setResponsePage(aPage.getApplication().getHomePage());
                aPage.getSession().info(
                        "There are no more documents to annotate available for you. Please contact your project supervisor.");

            default:
                // Default, simply go through the list and return the first document
                for (Map.Entry<SourceDocument, AnnotationDocument> entry : documentService
                        .listAnnotatableDocuments(project, user).entrySet()) {
                    // First check if too many users are already working on the document
                    if (((getUsersWorkingOnTheDocument(entry.getKey())
                            + 1) <= dynamicWorkflowManagementService
                                    .getOrCreateWorkflowEntry(project).getDefaultAnnotations())) {
                        // Now check if there either is no annotation document yet created or its
                        // state is NEW
                        if (entry.getValue() == null || entry.getValue().getState().equals(NEW)) {
                            aPage.getModelObject().setDocument(entry.getKey(),
                                    documentService.listSourceDocuments(project));
                            Optional<AjaxRequestTarget> target = RequestCycle.get()
                                    .find(AjaxRequestTarget.class);
                            aPage.actionLoadDocument(target.orElse(null));
                            return;
                        }
                    }
                }
            }
        }
    }

    public int getUsersWorkingOnTheDocument(SourceDocument aDocument)
    {
        return annotationDocumentList.stream()
                .filter(d -> d.getDocument().equals(aDocument) && !d.getState().equals(NEW)
                        && !d.getState().equals(IGNORE))
                .map(AnnotationDocument::getUser).sorted().collect(Collectors.joining(", "))
                .length();
    }
}
