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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension.DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID;
import static de.tudarmstadt.ukp.inception.workload.workflow.types.RandomizedWorkflowExtension.RANDOMIZED_WORKFLOW;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.docnav.DefaultDocumentNavigatorActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

/**
 * This is only enabled for annotators of a project with the dynamic workload enabled. Upon entering
 * the annotation page (unlike in the default annotation flow before) the annotator cannot choose
 * which document he/she wants to annotate, but rather get one selected depending on the workflow
 * strategy
 */
@Component
@ConditionalOnProperty(prefix = "workload.dynamic", name = "enabled", havingValue = "true")
public class DynamicWorkflowDocumentNavigationActionBarExtension
    implements ActionBarExtension
{
    private final DocumentService documentService;
    private final WorkloadManagementService workloadManagementService;
    private final DynamicWorkloadExtension dynamicWorkloadExtension;
    private final ProjectService projectService;

    // SpringBeans
    private @SpringBean EntityManager entityManager;

    @Autowired
    public DynamicWorkflowDocumentNavigationActionBarExtension(DocumentService aDocumentService,
            WorkloadManagementService aWorkloadManagementService,
            DynamicWorkloadExtension aDynamicWorkloadExtension, ProjectService aProjectService)
    {
        documentService = aDocumentService;
        workloadManagementService = aWorkloadManagementService;
        dynamicWorkloadExtension = aDynamicWorkloadExtension;
        projectService = aProjectService;
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
        // Curator are excluded from the feature
        return DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID
                .equals(workloadManagementService.getOrCreateWorkloadManagerConfiguration(
                        aPage.getModelObject().getProject()).getType())
                && !projectService.isCurator(aPage.getModelObject().getProject(),
                        aPage.getModelObject().getUser());
    }

    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage)
    {
        return new DynamicDocumentNavigator(aId);
    }

    // Init of the page, select a document
    @Override
    public void onInitialize(AnnotationPageBase aPage)
    {
        User user = aPage.getModelObject().getUser();
        Project project = aPage.getModelObject().getProject();
        Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);

        // Check if there is a document in progress and return this one
        List<AnnotationDocument> inProgressDocuments = workloadManagementService
                .getAnnotationDocumentListForUserWithState(project, user, IN_PROGRESS);

        // Assign a new document with actionLoadDocument

        // First, check if there are other documents which have been in the state INPROGRESS
        // Load the first one found
        for (AnnotationDocument a : inProgressDocuments) {
            if (a != null && IN_PROGRESS.equals(a.getState())) {
                aPage.getModelObject().setDocument(a.getDocument(),
                        documentService.listSourceDocuments(project));
                aPage.actionLoadDocument(target.orElse(null));
                return;
            }
        }

        // No annotation documents in the state INPROGRESS, now select a new one
        // depending on the workload strategy selected
        WorkloadManager currentWorkload = workloadManagementService
                .getOrCreateWorkloadManagerConfiguration(project);

        // Get all documents for which the state is NEW, or which have not been created yet.
        List<AnnotationDocument> annotationDocuments = workloadManagementService
                .getAnnotationDocumentListForUserWithState(project, user, NEW);

        // Switch for all workflow types which are available. If a new one is created.
        // simply add a new case here.
        switch (dynamicWorkloadExtension.readTraits(currentWorkload).getWorkflowType()) {

        // Go through all documents in a random order
        case (RANDOMIZED_WORKFLOW):
            // Shuffle the List then call loadNewDocument
            Collections.shuffle(annotationDocuments);
            loadNewDocument(annotationDocuments, project, currentWorkload, aPage);
            break;

        // Default workflow selected, nothing to change in the list
        default:
            loadNewDocument(annotationDocuments, project, currentWorkload, aPage);
            break;
        }
    }

    private void loadNewDocument(List<AnnotationDocument> aAnnotationDocuments, Project aProject,
            WorkloadManager aCurrentWorkload, AnnotationPageBase aPage)
    {
        Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);

        // Go through all documents of the list
        for (AnnotationDocument doc : aAnnotationDocuments) {
            if (doc != null) {
                SourceDocument sourceDocumentForCurrentAnnotationDocument = documentService
                    .getSourceDocument(aProject, doc.getName());
                // Check if there are less annotators working on the selected document than
                // the default number of annotation set by the project manager
                if ((workloadManagementService.getAmountOfUsersWorkingOnADocument(
                    sourceDocumentForCurrentAnnotationDocument, aProject)
                    + 1) <= (dynamicWorkloadExtension.readTraits(aCurrentWorkload)
                    .getDefaultNumberOfAnnotations())) {
                    // This was the case, so load the document and return
                    aPage.getModelObject().setDocument(sourceDocumentForCurrentAnnotationDocument,
                        documentService.listSourceDocuments(aProject));
                    aPage.actionLoadDocument(target.orElse(null));
                    return;
                }
            }
        }
        // No documents left, return to homepage and show corresponding message
        redirectUSerToHomePage(aPage);

    }

    public void redirectUSerToHomePage(ApplicationPageBase aPage)
    {
        // Nothing left, so returning to homepage and showing hint
        aPage.setResponsePage(aPage.getApplication().getHomePage());
        aPage.getSession().info(
                "There are no more documents to annotate available for you. Please contact your project supervisor.");
    }
}
