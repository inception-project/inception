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
package de.tudarmstadt.ukp.inception.workload.dynamic.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension.DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID;

import java.io.Serializable;
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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types.DefaultWorkflowExtension;
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
    implements ActionBarExtension, Serializable
{
    private static final long serialVersionUID = -8123846972605546654L;

    private final DocumentService documentService;
    private final WorkloadManagementService workloadManagementService;
    private final DynamicWorkloadExtension dynamicWorkloadExtension;
    private final ProjectService projectService;
    private final WorkflowExtensionPoint workflowExtensionPoint;

    private AnnotatorState annotatorState;

    // SpringBeans
    private @SpringBean EntityManager entityManager;

    @Autowired
    public DynamicWorkflowDocumentNavigationActionBarExtension(DocumentService aDocumentService,
            WorkloadManagementService aWorkloadManagementService,
            DynamicWorkloadExtension aDynamicWorkloadExtension, ProjectService aProjectService,
            WorkflowExtensionPoint aWorkflowExtensionPoint)
    {
        documentService = aDocumentService;
        workloadManagementService = aWorkloadManagementService;
        dynamicWorkloadExtension = aDynamicWorkloadExtension;
        projectService = aProjectService;
        workflowExtensionPoint = aWorkflowExtensionPoint;
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
                .equals(workloadManagementService.loadOrCreateWorkloadManagerConfiguration(
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
        annotatorState = aPage.getModelObject();
        User user = annotatorState.getUser();
        Project project = annotatorState.getProject();
        Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);

        // Check if there is a document in progress and return this one
        List<AnnotationDocument> inProgressDocuments = workloadManagementService
                .getAnnotationDocumentListForUserWithState(project, user, IN_PROGRESS);

        // Assign a new document with actionLoadDocument

        // First, check if there are other documents which have been in the state INPROGRESS
        // Load the first one found
        if (!inProgressDocuments.isEmpty()) {
            annotatorState.setDocument(inProgressDocuments.get(0).getDocument(),
                    documentService.listSourceDocuments(project));
            aPage.actionLoadDocument(target.orElse(null));
            return;
        }

        // No annotation documents in the state INPROGRESS, now select a new one
        // depending on the workload strategy selected
        WorkloadManager currentWorkload = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);

        // Get all documents for which the state is NEW, or which have not been created yet.
        List<SourceDocument> sourceDocuments = workloadManagementService
                .getAnnotationDocumentListForUser(project, user);

        // If there are no traits set yet, use the DefaultWorkflowExtension
        // otherwise select the current one
        WorkflowExtension currentWorkflowExtension = new DefaultWorkflowExtension();
        for (WorkflowExtension extension : workflowExtensionPoint.getExtensions()) {
            if (extension.getId().equals(
                    dynamicWorkloadExtension.readTraits(currentWorkload).getWorkflowType())) {
                currentWorkflowExtension = extension;
                break;
            }
        }
        // Rearrange list of documents according to current workflow
        sourceDocuments = currentWorkflowExtension.rankDocuments(sourceDocuments);

        // Load the new document, if loadNextDocument() returns false, redirect the user to the
        // homepage
        if (!currentWorkflowExtension.loadNextDocument(sourceDocuments, project, currentWorkload,
                aPage, target.orElse(null), workloadManagementService, dynamicWorkloadExtension,
                documentService)) {
            redirectUserToHomePage(aPage);
        }
    }

    public void redirectUserToHomePage(ApplicationPageBase aPage)
    {
        // Nothing left, so returning to homepage and showing hint
        aPage.getSession().info(
                "There are no more documents to annotate available for you. Please contact your project supervisor.");
        aPage.setResponsePage(aPage.getApplication().getHomePage());
    }
}
