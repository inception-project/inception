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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameModifier;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueOverviewDataProvider;

public class DynamicAnnotatorWorkflowActionBarItemGroup extends Panel
{

    private static final long serialVersionUID = -292514874000914541L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;


    @PersistenceContext
    private EntityManager entityManager;

    private final AnnotationPageBase page;
    protected final ConfirmationDialog finishDocumentDialog;
    private final LambdaAjaxLink finishDocumentLink;
    private final AnnotationQueueOverviewDataProvider provider;

    public DynamicAnnotatorWorkflowActionBarItemGroup(
        String aId, AnnotationPageBase aPage, EntityManager aEntityManager)
    {
        super(aId);

        //Same as for the default
        page = aPage;
        entityManager = aEntityManager;

        provider = new AnnotationQueueOverviewDataProvider(
            new ArrayList<>(documentService.listAnnotatableDocuments(
                aPage.getModelObject().getProject(), aPage.getModelObject().getUser()).values()),
            documentService.listSourceDocuments(
                aPage.getModelObject().getProject()), documentService, entityManager);

        add(finishDocumentDialog = new ConfirmationDialog("finishDocumentDialog",
            new StringResourceModel("FinishDocumentDialog.title", this, null),
            new StringResourceModel("FinishDocumentDialog.text", this, null)));

        add(finishDocumentLink = new LambdaAjaxLink("showFinishDocumentDialog",
            this::actionFinishDocument));
        finishDocumentLink.setOutputMarkupId(true);
        finishDocumentLink.add(enabledWhen(() -> page.isEditable()));
        finishDocumentLink.add(new Label("state")
            .add(new CssClassNameModifier(LambdaModel.of(this::getStateClass))));
    }

    protected AnnotationPageBase getAnnotationPage()
    {
        return page;
    }

    public String getStateClass()
    {
        return FontAwesome5IconType.check_circle_r.cssClassName();
    }

    protected void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        finishDocumentDialog.setConfirmAction((_target) ->
        {
            page.actionValidateDocument(_target, page.getEditorCas());

            AnnotatorState state = page.getModelObject();
            AnnotationDocument annotationDocument = documentService
                .getAnnotationDocument(state.getDocument(), state.getUser());

            documentService.transitionAnnotationDocumentState(annotationDocument,
                ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED);

            documentService.createAnnotationDocument(annotationDocument);

            //Get a new random document from the list and open it
            System.out.println(entityManager.toString());
            SourceDocument doc = provider.getRandomDocument
                (getAnnotationPage(), annotationDocument);
            if (doc == null) {
                getAnnotationPage().setResponsePage(getAnnotationPage().
                    getApplication().getHomePage());
                getSession().info("There are no more documents to annotate available for you. Please contact your project supervisor.");
            } else {
                getAnnotationPage().getModelObject().setDocument(doc, documentService.
                    listSourceDocuments(getAnnotationPage().getModelObject().getProject()));
                getAnnotationPage().actionLoadDocument(_target);
                _target.add(page);
            }
        });
        finishDocumentDialog.show(aTarget);

    }
}
