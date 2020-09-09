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
package de.tudarmstadt.ukp.inception.workload.dynamic.workflow;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.inception.workload.dynamic.extension.DynamicWorkloadExtension.EXTENSION_ID;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.docnav.DefaultDocumentNavigatorActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.registry.WorkloadRegistry;

@Component
public class DynamicWorkflowDocumentNavigationActionBarExtension implements ActionBarExtension
{
    private final DocumentService documentService;
    private final WorkloadRegistry workloadRegistry;
    private final WorkloadManagementService workloadManagementService;

    private final @PersistenceContext EntityManager entityManager;

    @Autowired
    public DynamicWorkflowDocumentNavigationActionBarExtension(
        DocumentService aDocumentService,
        EntityManager aEntityManager,
        WorkloadRegistry aWorkloadRegistry, WorkloadManagementService aWorkloadManagementService)
    {
        documentService = aDocumentService;
        entityManager = aEntityManager;
        workloadRegistry = aWorkloadRegistry;
        workloadManagementService = aWorkloadManagementService;
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
    public boolean accepts (AnnotationPageBase aPage)
    {
        return EXTENSION_ID.equals(workloadManagementService.
            getOrCreateWorkloadManagerConfiguration(aPage.getModelObject().getProject())
            .getExtensionPointID());
    }

    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage)
    {
        return new DynamicDocumentNavigator(aId);
    }

    //Init of the page, select a random document
    @Override
    public void onInitialize(AnnotationPageBase aPage)
    {
        User user = aPage.getModelObject().getUser();
        Project project = aPage.getModelObject().getProject();
        //Check if there is a document in progress and return this one
        for (AnnotationDocument annotationDocument:
            documentService.listAnnotationDocuments(project,user)) {
            //There was one in progress, load it
            if (annotationDocument.getState().equals(IN_PROGRESS)) {
                aPage.getModelObject().setDocument(annotationDocument.getDocument(),
                    documentService.listSourceDocuments(project));
                Optional<AjaxRequestTarget> target = RequestCycle.get().
                    find(AjaxRequestTarget.class);
                aPage.actionLoadDocument(target.orElse(null));
                return;
            }
        }
        //Nothing in progress found, get a random document
        if (aPage.getModelObject().getDocument() == null) {
            //Go through all documents in a random order and check if there is a Annotation document
            //with the state NEW
            String query =  "FROM SourceDocument " +
                            "WHERE project = :project " +
                            "ORDER BY rand()";
            for (SourceDocument doc: entityManager.createQuery(query,SourceDocument.class)
                .setParameter("project", project).getResultList()) {
                //Check if it exist or is NEW
                if (documentService.listAnnotatableDocuments(project,user).get(doc) == null ||
                    documentService.listAnnotatableDocuments(project,user).get(doc).
                        getState().equals(NEW)) {
                    //This document had the state NEW, load it
                    aPage.getModelObject().setDocument(doc, documentService.
                        listSourceDocuments(project));

                    Optional<AjaxRequestTarget> target = RequestCycle.get().
                        find(AjaxRequestTarget.class);
                    aPage.actionLoadDocument(target.orElse(null));
                    return;
                }
            }
        }
        //No documents left
        if (aPage.getModelObject().getDocument() == null) {
            aPage.setResponsePage(aPage.getApplication().getHomePage());
            aPage.getSession().info("There are no more documents to annotate available for you. Please contact your project supervisor.");
        }
    }
}
