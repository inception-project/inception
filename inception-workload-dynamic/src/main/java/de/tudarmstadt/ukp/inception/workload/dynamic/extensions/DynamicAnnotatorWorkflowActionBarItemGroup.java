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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;

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
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;

public class DynamicAnnotatorWorkflowActionBarItemGroup extends Panel
{
    private static final long serialVersionUID = -292514874000914541L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @PersistenceContext EntityManager entityManager;
    private final AnnotationPageBase page;
    private final LambdaAjaxLink finishDocumentLink;
    protected final ConfirmationDialog finishDocumentDialog;

    public DynamicAnnotatorWorkflowActionBarItemGroup(
            String aId, AnnotationPageBase aPage, EntityManager aEntityManager)
    {
        super(aId);

        //Same as for the default
        page = aPage;
        entityManager = aEntityManager;

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
            User user = state.getUser();
            Project project = state.getProject();
            SourceDocument document = state.getDocument();

            AnnotationDocument annotationDocument = documentService
                .getAnnotationDocument(document, user);

            documentService.transitionAnnotationDocumentState(annotationDocument,
                ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED);

            //Go through all documents in a random order and check if there is a Annotation document
            //with the state NEW
            String query =  "FROM SourceDocument " +
                            "WHERE project = :project " +
                            "ORDER BY rand()";
            for (SourceDocument doc: entityManager.createQuery(query, SourceDocument.class)
                .setParameter("project", project).getResultList()) {
                //Check if it even exists or is state NEW
                if (documentService.listAnnotatableDocuments(project,user).get(doc) == null ||
                    documentService.listAnnotatableDocuments(project,user).get(doc).
                        getState().equals(NEW)) {
                    getAnnotationPage().getModelObject().setDocument(doc, documentService.
                        listSourceDocuments(project));
                    //This document had the state NEW, load it
                    getAnnotationPage().actionLoadDocument(_target);
                    _target.add(page);
                    return;
                }
            }

            //Check if no new document has been selected, return to homepage
            if (getAnnotationPage().getModelObject().getDocument().equals(document)) {
                getAnnotationPage().setResponsePage(getAnnotationPage().getApplication().
                    getHomePage());
                getSession().info("There are no more documents to annotate available for you. Please contact your project supervisor.");
            }
        });
        finishDocumentDialog.show(aTarget);
    }
}
