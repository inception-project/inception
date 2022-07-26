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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;

import java.io.IOException;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameModifier;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.finish.FinishDocumentDialogContent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.finish.FinishDocumentDialogModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.ValidationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.trait.DynamicWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

/**
 * This is only enabled for annotators of a project with the dynamic workload enabled. An annotator
 * is no more able to switch between documents before finishing the current one. This increases the
 * usablility of the data, as all documents are finished and not only "started". Depending on the
 * workflow strategy selected by the project manager, the document distribution varies.
 */
public class DynamicAnnotatorWorkflowActionBarItemGroup
    extends Panel
{
    private static final long serialVersionUID = 9215276761731631710L;

    private AnnotationPageBase page;

    protected ModalDialog finishDocumentDialog;

    private final LambdaAjaxLink finishDocumentLink;

    // SpringBeans
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean DynamicWorkloadExtension dynamicWorkloadExtension;
    private @SpringBean EntityManager entityManager;
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean WorkflowExtensionPoint workflowExtensionPoint;

    public DynamicAnnotatorWorkflowActionBarItemGroup(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        page = aPage;

        finishDocumentDialog = new BootstrapModalDialog("finishDocumentDialog");
        finishDocumentDialog.setContent(new FinishDocumentDialogContent(ModalDialog.CONTENT_ID,
                Model.of(new FinishDocumentDialogModel()),
                this::actionFinishDocumentDialogSubmitted));
        add(finishDocumentDialog);

        queue(finishDocumentLink = new LambdaAjaxLink("showFinishDocumentDialog",
                this::actionFinishDocument));
        finishDocumentLink.setOutputMarkupId(true);
        finishDocumentLink.add(enabledWhen(page::isEditable));

        queue(new Label("state")
                .add(new CssClassNameModifier(LambdaModel.of(this::getStateClass))));
    }

    public AnnotatorState getModelObject()
    {
        return page.getModelObject();
    }

    protected AnnotationPageBase getAnnotationPage()
    {
        return page;
    }

    public String getStateClass()
    {
        return FontAwesome5IconType.check_circle_r.cssClassName();
    }

    /**
     * This method represents the opening dialog upon clicking "Finish" for the current document.
     */
    private void actionFinishDocument(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        WorkloadManager manager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(getModelObject().getProject());
        DynamicWorkloadTraits traits = dynamicWorkloadExtension.readTraits(manager);

        try {
            page.actionValidateDocument(aTarget, page.getEditorCas());
        }
        catch (ValidationException e) {
            page.error("Document cannot be marked as finished: " + e.getMessage());
            aTarget.addChildren(page, IFeedback.class);
            return;
        }

        if (traits.isConfirmFinishingDocuments()) {
            finishDocumentDialog.open(aTarget);
        }
        else {
            actionFinishDocumentConfirmedNoDialog(aTarget);
        }
    }

    private void actionFinishDocumentDialogSubmitted(AjaxRequestTarget aTarget,
            Form<FinishDocumentDialogModel> aForm)
        throws IOException, AnnotationException
    {
        AnnotatorState state = getModelObject();

        var newState = aForm.getModelObject().getState();

        AnnotationDocument annotationDocument = documentService
                .getAnnotationDocument(state.getDocument(), state.getUser());
        annotationDocument.setAnnotatorComment(aForm.getModelObject().getComment());
        documentService.setAnnotationDocumentState(annotationDocument, newState,
                EXPLICIT_ANNOTATOR_USER_ACTION);

        goToNextDocument(aTarget);
    }

    private void actionFinishDocumentConfirmedNoDialog(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        AnnotatorState state = getModelObject();

        AnnotationDocument annotationDocument = documentService
                .getAnnotationDocument(state.getDocument(), state.getUser());
        annotationDocument.setAnnotatorComment(null);
        documentService.setAnnotationDocumentState(annotationDocument, FINISHED,
                EXPLICIT_ANNOTATOR_USER_ACTION);

        goToNextDocument(aTarget);
    }

    private void goToNextDocument(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        User user = state.getUser();
        Project project = state.getProject();
        Optional<SourceDocument> nextDocument = dynamicWorkloadExtension
                .nextDocumentToAnnotate(project, user);

        if (!nextDocument.isPresent()) {
            // Nothing left, so returning to homepage and showing hint
            page.getSession().info("There are no more documents to annotate available for you.");
            page.setResponsePage(getAnnotationPage().getApplication().getHomePage());
            return;
        }

        // Assign a new document with actionLoadDocument
        page.getModelObject().setDocument(nextDocument.get(),
                documentService.listSourceDocuments(nextDocument.get().getProject()));
        page.actionLoadDocument(aTarget);
        aTarget.add(page);
    }
}
