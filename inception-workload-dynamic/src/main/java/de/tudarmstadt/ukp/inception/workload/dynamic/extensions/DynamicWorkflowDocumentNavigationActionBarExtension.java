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
package de.tudarmstadt.ukp.inception.workload.dynamic.extensions;

import java.util.ArrayList;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.docnav.DefaultDocumentNavigatorActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.WorkloadAndWorkflowService;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueOverviewDataProvider;

@Order(1000)
@Component
public class DynamicWorkflowDocumentNavigationActionBarExtension implements ActionBarExtension
{
    private final DocumentService documentService;
    private final WorkloadAndWorkflowService workloadAndWorkflowService;

    private final @PersistenceContext EntityManager entityManager;

    @Autowired
    public DynamicWorkflowDocumentNavigationActionBarExtension(
        WorkloadAndWorkflowService aWorkloadAndWorkflowService,
        DocumentService aDocumentService,
        EntityManager aEntityManager)
    {
        workloadAndWorkflowService = aWorkloadAndWorkflowService;
        documentService = aDocumentService;
        entityManager = aEntityManager;
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
    public boolean accepts(AnnotationPageBase aPage) {
        if (!workloadAndWorkflowService.getWorkflowManager(
            aPage.getModelObject().getProject()).equals("Default workflow manager")) {
            return true;
        } else {
            return false;
        }
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
        AnnotationQueueOverviewDataProvider prov =
            new AnnotationQueueOverviewDataProvider(
                new ArrayList<>(documentService.listAnnotatableDocuments
                    (aPage.getModelObject().getProject(),
                        aPage.getModelObject().getUser()).values()),
                documentService.listSourceDocuments(
                    aPage.getModelObject().getProject()), documentService, entityManager);
        SourceDocument doc = prov.getRandomDocument(aPage, new AnnotationDocument());
        if (doc == null) {
            aPage.setResponsePage(aPage.getApplication().getHomePage());
            aPage.getSession().info("There are no more documents to annotate available for you. Please contact your project supervisor.");
        } else {
            aPage.getModelObject().setDocument(doc, documentService.
                listSourceDocuments(aPage.getModelObject().getProject()));
            Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
            aPage.actionLoadDocument(target.orElse(null));
        }
    }

}
