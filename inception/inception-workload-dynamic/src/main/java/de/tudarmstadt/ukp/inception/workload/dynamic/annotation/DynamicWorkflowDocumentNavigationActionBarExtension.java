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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension.DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.config.DynamicWorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

/**
 * This is only enabled for annotators of a project with the dynamic workload enabled. Upon entering
 * the annotation page (unlike in the default annotation flow before) the annotator cannot choose
 * which document he/she wants to annotate, but rather get one selected depending on the workflow
 * strategy
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DynamicWorkloadManagerAutoConfiguration#dynamicWorkflowDocumentNavigationActionBarExtension}
 * </p>
 */
@Order(ActionBarExtension.ORDER_WORKFLOW)
public class DynamicWorkflowDocumentNavigationActionBarExtension
    implements ActionBarExtension, Serializable
{
    private static final long serialVersionUID = -8123846972605546654L;

    private final DocumentService documentService;
    private final WorkloadManagementService workloadManagementService;
    private final DynamicWorkloadExtension dynamicWorkloadExtension;
    private final ProjectService projectService;
    private final UserDao userService;

    private AnnotatorState annotatorState;

    @Autowired
    public DynamicWorkflowDocumentNavigationActionBarExtension(DocumentService aDocumentService,
            WorkloadManagementService aWorkloadManagementService,
            DynamicWorkloadExtension aDynamicWorkloadExtension, ProjectService aProjectService,
            UserDao aUserService)
    {
        documentService = aDocumentService;
        workloadManagementService = aWorkloadManagementService;
        dynamicWorkloadExtension = aDynamicWorkloadExtension;
        projectService = aProjectService;
        userService = aUserService;
    }

    @Override
    public String getRole()
    {
        return ROLE_NAVIGATOR;
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
        var project = aPage.getModelObject().getProject();
        if (project == null) {
            return false;
        }

        var sessionOwner = userService.getCurrentUser();
        var workloadConfig = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(aPage.getModelObject().getProject());
        return DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID.equals(workloadConfig.getType())
                && !projectService.hasRole(sessionOwner, project, CURATOR);
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
        var user = annotatorState.getUser();
        var project = annotatorState.getProject();
        var target = RequestCycle.get().find(AjaxRequestTarget.class);

        // Assign a new document with actionLoadDocument
        var allDocuments = documentService.listSourceDocuments(project);
        var nextDocument = dynamicWorkloadExtension.nextDocumentToAnnotate(project, user);
        if (nextDocument.isPresent()) {
            var state = aPage.getModelObject();
            // This was the case, so load the document and return
            if (!nextDocument.get().equals(state.getDocument())) {
                // If the document is already loaded, do nothing (avoids an endless recursion
                // triggered by actionLoadDocument refreshing the action bar which then
                // calls onInitialize).
                state.setDocument(nextDocument.get(), allDocuments);
                aPage.actionLoadDocument(target.orElse(null));
            }
            return;
        }

        // Nothing left, so returning to home page and showing hint
        aPage.getSession().info("There are no more documents to annotate available for you.");
        aPage.setResponsePage(aPage.getApplication().getHomePage());
    }
}
