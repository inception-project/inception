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

import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.docnav.DefaultDocumentNavigatorActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.WorkflowProperties;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueOverviewDataProvider;


@Order(0)
@Component
public class DynamicWorkflowDocumentNavigationActionBarExtension implements ActionBarExtension
{

    private @Autowired WorkflowProperties workflowProperties;
    private @Autowired DocumentService documentService;
    private AnnotationPageBase annotationPageBase;

    @Override
    public String getRole() {
        return DefaultDocumentNavigatorActionBarExtension.class.getName();
    }

    @Override
    public int getPriority() {
        if (workflowProperties.isWorkflowManagerActive())
        {
            return 1;
        } else {
            return -1;
        }
    }

    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage) {
        return new DynamicDocumentNavigator(aId);
    }



    //Init of the page, select a random document
    @Override
    public void onInitialize(AnnotationPageBase aPage)
    {

        this.annotationPageBase = aPage;
        AnnotationQueueOverviewDataProvider prov =
            new AnnotationQueueOverviewDataProvider(
                documentService.listAnnotationDocuments
            (aPage.getModelObject().getProject()), documentService.
                listSourceDocuments(aPage.getModelObject().getProject()));
        SourceDocument doc = prov.getRandomDocument(aPage, new AnnotationDocument());
        if (doc == null)
        {
            aPage.setResponsePage(aPage.getApplication().getHomePage());

        } else {

            //Workaround to generate ajax request target on
            //page load for actionLoadDocument() method in webanno
            //TODO better solution for this needed

            aPage.getModelObject().setDocument(doc, documentService.
                listSourceDocuments(aPage.getModelObject().getProject()));

            aPage.add(new AbstractAjaxTimerBehavior(Duration.milliseconds(1)) {
                private static final long serialVersionUID = -2222252999587974771L;
                @Override
                protected void onTimer(AjaxRequestTarget aAjaxRequestTarget) {
                    aPage.actionLoadDocument(aAjaxRequestTarget);
                    stop(aAjaxRequestTarget);
                }
            });
        }
    }

}
