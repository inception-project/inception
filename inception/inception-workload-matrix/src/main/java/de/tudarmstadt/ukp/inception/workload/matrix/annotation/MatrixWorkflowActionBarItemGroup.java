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
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static wicket.contrib.input.events.EventType.click;
import static wicket.contrib.input.events.key.KeyType.Ctrl;
import static wicket.contrib.input.events.key.KeyType.End;

import java.io.IOException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameModifier;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.finish.FinishDocumentDialogContent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.finish.FinishDocumentDialogModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.ValidationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.matrix.trait.MatrixWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.ui.ResetAnnotationDocumentConfirmationDialogContentPanel;
import wicket.contrib.input.events.key.KeyType;

public class MatrixWorkflowActionBarItemGroup
    extends Panel
{
    private static final long serialVersionUID = 4139817495914347777L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean MatrixWorkloadExtension matrixWorkloadExtension;
    private @SpringBean PreferencesService preferencesService;

    private final AnnotationPageBase page;
    private final ModalDialog dialog;
    private final IModel<MatrixWorkloadTraits> traits;

    public MatrixWorkflowActionBarItemGroup(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        page = aPage;

        traits = LoadableDetachableModel.of(() -> matrixWorkloadExtension
                .readTraits(workloadManagementService.loadOrCreateWorkloadManagerConfiguration(
                        page.getModelObject().getProject())));

        dialog = new BootstrapModalDialog("dialog");
        add(dialog);

        add(createToggleDocumentStateLink("toggleDocumentState"));

        add(createResetDocumentLink("showResetDocumentDialog"));
    }

    private Component createResetDocumentLink(String aString)
    {
        var link = new LambdaAjaxLink(aString, this::actionRequestResetDocumentConfirmation);
        link.add(enabledWhen(() -> page.isEditable()));
        link.add(visibleWhen(
                traits.map(MatrixWorkloadTraits::isDocumentResetAllowed).orElse(false)));
        return link;
    }

    private LambdaAjaxLink createToggleDocumentStateLink(String aId)
    {
        LambdaAjaxLink link;
        if (isReopenableByUser()) {
            link = new LambdaAjaxLink(aId, this::actionToggleDocumentState);
        }
        else {
            link = new LambdaAjaxLink(aId, this::actionRequestFinishDocumentConfirmation);
        }
        link.setOutputMarkupId(true);
        link.add(enabledWhen(() -> page.isEditable() || isReopenableByUser()));
        link.add(new InputBehavior(new KeyType[] { Ctrl, End }, click));
        var stateLabel = new Label("state");
        stateLabel.add(new CssClassNameModifier(LoadableDetachableModel.of(this::getStateClass)));
        stateLabel.add(AttributeModifier.replace("title", LoadableDetachableModel.of(() -> {
            var tooltip = this.getStateTooltip();
            return tooltip.wrapOnAssignment(stateLabel).getObject();
        })));
        link.add(stateLabel);
        return link;
    }

    private boolean isReopenableByUser()
    {
        // Curators can re-open documents anyway via the monitoring page, so we can always allow
        // the re-open documents here as well
        var state = page.getModelObject();
        if (projectService.hasRole(userRepository.getCurrentUsername(), state.getProject(),
                CURATOR)) {
            return true;
        }

        return traits.getObject().isReopenableByAnnotator();
    }

    protected AnnotationPageBase getAnnotationPage()
    {
        return page;
    }

    public ResourceModel getStateTooltip()
    {
        var state = page.getModelObject();

        // Curation sidebar: when writing to the curation document, we need to update the document
        if (CURATION_USER.equals(state.getUser().getUsername())) {
            if (state.getDocument().getState() == SourceDocumentState.CURATION_FINISHED) {
                return new ResourceModel("stateToggle.curationFinished");
            }
            else {
                return new ResourceModel("stateToggle.curationInProgress");
            }
        }

        if (documentService.isAnnotationFinished(state.getDocument(), state.getUser())) {
            return new ResourceModel("stateToggle.annotationFinished");
        }
        else {
            return new ResourceModel("stateToggle.annotationInProgress");
        }
    }

    public String getStateClass()
    {
        var state = page.getModelObject();

        // Curation sidebar: when writing to the curation document, we need to update the document
        if (state.getUser().getUsername().equals(CURATION_USER)) {
            if (state.getDocument().getState() == SourceDocumentState.CURATION_FINISHED) {
                // SourceDocumentState.CURATION_FINISHED.symbol()
                return FontAwesome5IconType.clipboard_check_s.cssClassName();
            }
            else {
                // SourceDocumentState.CURATION_IN_PROGRESS.symbol()
                return FontAwesome5IconType.clipboard_s.cssClassName();
            }
        }

        if (documentService.isAnnotationFinished(state.getDocument(), state.getUser())) {
            // AnnotationDocumentState.FINISHED.symbol();
            return FontAwesome5IconType.play_circle_r.cssClassName();
        }
        else {
            // AnnotationDocumentState.IN_PROGRESS.symbol();
            return FontAwesome5IconType.check_circle_r.cssClassName();
        }
    }

    protected void actionRequestResetDocumentConfirmation(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        var content = new ResetAnnotationDocumentConfirmationDialogContentPanel(
                ModalDialog.CONTENT_ID);

        content.setExpectedResponseModel(
                page.getModel().map(AnnotatorState::getDocument).map(SourceDocument::getName));
        content.setConfirmAction(_target -> {
            var state = page.getModelObject();
            documentService.resetAnnotationCas(state.getDocument(), state.getUser(),
                    EXPLICIT_ANNOTATOR_USER_ACTION);
            page.actionLoadDocument(_target);
        });

        dialog.open(content, aTarget);
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

        var dialogContent = new FinishDocumentDialogContent(ModalDialog.CONTENT_ID,
                Model.of(new FinishDocumentDialogModel()),
                this::actionFinishDocumentDialogSubmitted);

        dialog.open(dialogContent, aTarget);
    }

    private void actionFinishDocumentDialogSubmitted(AjaxRequestTarget aTarget,
            Form<FinishDocumentDialogModel> aForm)
    {
        var state = page.getModelObject();

        var newState = aForm.getModelObject().getState();

        var annotationDocument = documentService.getAnnotationDocument(state.getDocument(),
                state.getUser());
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
        // state instead
        var state = page.getModelObject();
        var document = state.getDocument();

        // Curation sidebar: when writing to the curation document, we need to update the docuement
        if (CURATION_USER.equals(state.getUser().getUsername())) {
            switch (document.getState()) {
            case CURATION_FINISHED:
                documentService.setSourceDocumentState(document, CURATION_IN_PROGRESS);
                aTarget.add(page);
                break;
            default:
                documentService.setSourceDocumentState(document, CURATION_FINISHED);
                aTarget.add(page);
                break;
            }
            return;
        }

        var sessionOwner = userRepository.getCurrentUser();
        var annDoc = documentService.getAnnotationDocument(document, state.getUser());
        if (annDoc.getAnnotatorState() != annDoc.getState()
                && !projectService.hasRole(sessionOwner, state.getProject(), CURATOR, MANAGER)) {
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
            documentService.setAnnotationDocumentState(annDoc, FINISHED,
                    EXPLICIT_ANNOTATOR_USER_ACTION);
            aTarget.add(page);
            break;
        case FINISHED:
            documentService.setAnnotationDocumentState(annDoc, IN_PROGRESS,
                    EXPLICIT_ANNOTATOR_USER_ACTION);
            aTarget.add(page);
            break;
        default:
            error("Can only change document state for documents that are finished or in progress, "
                    + "but document is in state [" + annState + "]");
            aTarget.addChildren(getPage(), IFeedback.class);
            break;
        }
    }
}
