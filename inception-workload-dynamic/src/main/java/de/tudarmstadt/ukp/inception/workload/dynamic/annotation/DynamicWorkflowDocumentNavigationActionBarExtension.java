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

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

@Component
@ConditionalOnProperty(prefix = "workload.dynamic", name = "enabled", havingValue = "true")
public class DynamicWorkflowDocumentNavigationActionBarExtension
    implements ActionBarExtension
{
    private final DocumentService documentService;
    private final WorkloadManagementService workloadManagementService;
    private final DynamicWorkloadExtension dynamicWorkloadExtension;
    private List<AnnotationDocument> annotationDocumentList;

    private @SpringBean EntityManager entityManager;

    private final ProjectService projectService;

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

    // Init of the page, select a random document
    @Override
    public void onInitialize(AnnotationPageBase aPage)
    {
        User user = aPage.getModelObject().getUser();
        Project project = aPage.getModelObject().getProject();

        // Check if there is a document in progress and return this one
        List<AnnotationDocument> inProgressDocuments = workloadManagementService
                .getAnnotationDocumentsForSpecificState(IN_PROGRESS, project, user);

        if (inProgressDocuments.size() > 0) {
            aPage.getModelObject().setDocument(inProgressDocuments.get(0).getDocument(),
                    documentService.listSourceDocuments(project));
            Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
            aPage.actionLoadDocument(target.orElse(null));
        }

        // Nothing in progress found
        if (aPage.getModelObject().getDocument() == null) {
            WorkloadManager currentWorkload = workloadManagementService
                    .getOrCreateWorkloadManagerConfiguration(project);

            switch (dynamicWorkloadExtension.readTraits(currentWorkload).getWorkloadType()) {
            case ("Randomized workflow"):
                // Go through all documents in a random order and check if there
                // is a Annotation document with the state NEW
                List<SourceDocument> randomList = documentService.listSourceDocuments(project);
                Collections.shuffle(randomList);
                Map<SourceDocument, AnnotationDocument> documentsOfCurrentUser = documentService
                        .listAnnotatableDocuments(project, user);
                for (SourceDocument doc : randomList) {
                    if ((workloadManagementService.getAmountOfUsersWorkingOnADocument(doc, project)
                            + 1) <= (dynamicWorkloadExtension.readTraits(currentWorkload)
                                    .getDefaultNumberOfAnnotations())
                            && (documentsOfCurrentUser.get(doc) == null
                                    || NEW.equals(documentsOfCurrentUser.get(doc).getState()))) {
                        aPage.getModelObject().setDocument(doc,
                                documentService.listSourceDocuments(project));
                        Optional<AjaxRequestTarget> target = RequestCycle.get()
                                .find(AjaxRequestTarget.class);
                        aPage.actionLoadDocument(target.orElse(null));
                        return;
                    }
                }
                // No documents left, return to homepage and show corressponding message
                redirectUSerToHomePage(aPage);
                break;

            default:
                // Default, simply go through the list and return the first document
                for (Map.Entry<SourceDocument, AnnotationDocument> entry : documentService
                        .listAnnotatableDocuments(project, user).entrySet()) {
                    // First check if too many users are already working on the document
                    if (((workloadManagementService
                            .getAmountOfUsersWorkingOnADocument(entry.getKey(), project))
                            + 1) <= dynamicWorkloadExtension.readTraits(currentWorkload)
                                    .getDefaultNumberOfAnnotations()) {
                        // Now check if there either is no annotation document yet created or its
                        // state is NEW
                        if (entry.getValue() == null || NEW.equals(entry.getValue().getState())) {
                            aPage.getModelObject().setDocument(entry.getKey(),
                                    documentService.listSourceDocuments(project));
                            Optional<AjaxRequestTarget> target = RequestCycle.get()
                                    .find(AjaxRequestTarget.class);
                            aPage.actionLoadDocument(target.orElse(null));
                            return;
                        }
                    }
                }
                // No documents left, return to homepage and show corressponding message
                redirectUSerToHomePage(aPage);
                break;
            }
        }
    }

    public void redirectUSerToHomePage(ApplicationPageBase aPage)
    {
        // Nothing left, so returning to homepage and showing hint
        aPage.setResponsePage(aPage.getApplication().getHomePage());
        aPage.getSession().info(
                "There are no more documents to annotate available for you. Please contact your project supervisor.");
    }
}
