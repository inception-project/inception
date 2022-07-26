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
package de.tudarmstadt.ukp.inception.workload.matrix.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;

import java.io.IOException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameModifier;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.finish.FinishDocumentDialogContent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.finish.FinishDocumentDialogModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.ValidationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.matrix.trait.MatrixWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

public class MatrixWorkflowActionBarItemGroup
    extends Panel
{
    private static final long serialVersionUID = 4139817495914347777L;

    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userRepository;
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean MatrixWorkloadExtension matrixWorkloadExtension;

    private final AnnotationPageBase page;
    protected ModalDialog finishDocumentDialog;
    private final ChallengeResponseDialog resetDocumentDialog;
    private final LambdaAjaxLink resetDocumentLink;

    public MatrixWorkflowActionBarItemGroup(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        page = aPage;

        finishDocumentDialog = new BootstrapModalDialog("finishDocumentDialog");
        finishDocumentDialog.setContent(new FinishDocumentDialogContent(ModalDialog.CONTENT_ID,
                Model.of(new FinishDocumentDialogModel()),
                this::actionFinishDocumentDialogSubmitted));
        add(finishDocumentDialog);

        add(createToggleDocumentStateLink("toggleDocumentState"));

        IModel<String> documentNameModel = PropertyModel.of(page.getModel(), "document.name");
        add(resetDocumentDialog = new ChallengeResponseDialog("resetDocumentDialog",
                new StringResourceModel("ResetDocumentDialog.title", this),
                new StringResourceModel("ResetDocumentDialog.text", this) //
                        .setModel(page.getModel()) //
                        .setParameters(documentNameModel),
                documentNameModel));
        resetDocumentDialog.setConfirmAction(this::actionResetDocument);

        add(resetDocumentLink = new LambdaAjaxLink("showResetDocumentDialog",
                resetDocumentDialog::show));
        resetDocumentLink.add(enabledWhen(() -> page.isEditable()));
    }

    private LambdaAjaxLink createToggleDocumentStateLink(String aId)
    {
        MatrixWorkloadTraits traits = matrixWorkloadExtension.readTraits(workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(page.getModelObject().getProject()));

        LambdaAjaxLink link;
        if (traits.isReopenableByAnnotator()) {
            link = new LambdaAjaxLink(aId, this::actionToggleDocumentState);
        }
        else {
            link = new LambdaAjaxLink(aId, this::actionRequestFinishDocumentConfirmation);
        }
        link.setOutputMarkupId(true);
        link.add(enabledWhen(() -> page.isEditable() || traits.isReopenableByAnnotator()));
        link.add(new Label("state")
                .add(new CssClassNameModifier(LambdaModel.of(this::getStateClass))));
        return link;
    }

    protected AnnotationPageBase getAnnotationPage()
    {
        return page;
    }

    public String getStateClass()
    {
        AnnotatorState state = page.getModelObject();

        if (documentService.isAnnotationFinished(state.getDocument(), state.getUser())) {
            return FontAwesome5IconType.lock_s.cssClassName();
        }
        else {
            return FontAwesome5IconType.lock_open_s.cssClassName();
        }
    }

    protected void actionRequestFinishDocumentConfirmation(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        try {
            page.actionValidateDocument(aTarget, page.getEditorCas());
        }
        catch (ValidationException e) {
            page.error("Document cannot be marked as finished: " + e.getMessage());
            aTarget.addChildren(page, IFeedback.class);
            return;
        }

        finishDocumentDialog.open(aTarget);
    }

    private void actionFinishDocumentDialogSubmitted(AjaxRequestTarget aTarget,
            Form<FinishDocumentDialogModel> aForm)
    {
        AnnotatorState state = page.getModelObject();

        var newState = aForm.getModelObject().getState();

        AnnotationDocument annotationDocument = documentService
                .getAnnotationDocument(state.getDocument(), state.getUser());
        annotationDocument.setAnnotatorComment(aForm.getModelObject().getComment());
        documentService.setAnnotationDocumentState(annotationDocument, newState,
                EXPLICIT_ANNOTATOR_USER_ACTION);

        if (newState == AnnotationDocumentState.IGNORE) {
            state.reset();
            state.setDocument(null, null);
        }

        aTarget.add(page);
    }

    private void actionToggleDocumentState(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = page.getModelObject();
        SourceDocument document = state.getDocument();
        var annDoc = documentService.getAnnotationDocument(document, state.getUser());

        if (annDoc.getAnnotatorState() != annDoc.getState()) {
            error("Annotation state has been overridden by a project manager or curator. "
                    + "You cannot change it.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        // We look at the annotator state here because annotators should only be able to re-open
        // documents that they closed themselves - not documents that were closed e.g. by a manager
        var annState = annDoc.getAnnotatorState();
        switch (annState) {
        case IN_PROGRESS:
            // curation sidebar: need to update source doc state as well to finished
            if (state.getUser().getUsername().equals(CURATION_USER)) {
                documentService.setSourceDocumentState(document, CURATION_FINISHED);
            }
            else {
                documentService.setAnnotationDocumentState(annDoc, FINISHED,
                        EXPLICIT_ANNOTATOR_USER_ACTION);
            }
            aTarget.add(page);
            break;
        case FINISHED:
            // curation sidebar: need to update source doc state as well to finished
            if (state.getUser().getUsername().equals(CURATION_USER)) {
                documentService.setSourceDocumentState(document, CURATION_IN_PROGRESS);
            }
            else {
                documentService.setAnnotationDocumentState(annDoc, IN_PROGRESS,
                        EXPLICIT_ANNOTATOR_USER_ACTION);
            }
            aTarget.add(page);
            break;
        default:
            error("Can only change document state for documents that are finished or in progress, "
                    + "but document is in state [" + annState + "]");
            aTarget.addChildren(getPage(), IFeedback.class);
            break;
        }
    }

    protected void actionResetDocument(AjaxRequestTarget aTarget) throws Exception
    {
        AnnotatorState state = page.getModelObject();
        documentService.resetAnnotationCas(state.getDocument(), state.getUser(),
                EXPLICIT_ANNOTATOR_USER_ACTION);
        page.actionLoadDocument(aTarget);
    }
}
