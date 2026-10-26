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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.actionbar;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.FILL_ONLY;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.RECREATE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;

import java.io.IOException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameModifier;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome7IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarContext;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.DocumentStateHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.ValidationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.MergeDialog;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.ui.curation.page.CuratableDocumentPage;

public class CuratorWorkflowActionBarItemGroup
    extends Panel
    implements DocumentStateHandler
{
    private static final long serialVersionUID = 8596786586955459711L;

    private @SpringBean DocumentService documentService;
    private @SpringBean CurationService curationService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean UserDao userRepository;
    private @SpringBean CurationSessionService curationSessionService;

    private final AnnotationPageBase page;
    private final DocumentEditor editorContext;
    // private final ConfirmationDialog finishDocumentDialog;
    private final LambdaAjaxLink toggleCurationStateLink;
    private final IModel<CurationWorkflow> curationWorkflowModel;
    private MergeDialog resetDocumentDialog;
    private LambdaAjaxLink resetDocumentLink;

    public CuratorWorkflowActionBarItemGroup(String aId, ActionBarContext aContext)
    {
        super(aId);

        page = aContext.page();
        editorContext = aContext.editor();

        // add(finishDocumentDialog = new ConfirmationDialog("finishDocumentDialog",
        // new StringResourceModel("FinishDocumentDialog.title", this, null),
        // new StringResourceModel("FinishDocumentDialog.text", this, null)));

        add(toggleCurationStateLink = new LambdaAjaxLink("toggleCurationState",
                this::actionToggleCurationState));
        toggleCurationStateLink.setOutputMarkupId(true);
        toggleCurationStateLink.add(AttributeModifier.replace("title",
                LoadableDetachableModel.of(() -> getString(getStateTooltipKey()))));
        toggleCurationStateLink.add(new Label("state")
                .add(new CssClassNameModifier(LambdaModel.of(this::getStateClass))));

        curationWorkflowModel = Model.of(curationService
                .readOrCreateCurationWorkflow(editorContext.getAnnotatorState().getProject()));
        IModel<String> documentNameModel = PropertyModel.of(editorContext.getStateModel(),
                "document.name");
        add(resetDocumentDialog = new MergeDialog("resetDocumentDialog",
                new ResourceModel("reMergeDialog.title"), new ResourceModel("reMergeDialog.text"),
                documentNameModel, curationWorkflowModel));
        resetDocumentDialog.setConfirmAction(this::actionResetDocument);
        resetDocumentDialog.setDataOwnerSource(() -> curationSessionService
                .listCuratableDataOwners(editorContext.getAnnotatorState().getDocument()));
        resetDocumentDialog.setSidebarSelectionSource(() -> {
            var state = editorContext.getAnnotatorState();
            return curationSessionService.listDataOwnersReadyForCuration(
                    userRepository.getCurrentUsername(), state.getProject(), state.getDocument());
        });

        add(resetDocumentLink = new LambdaAjaxLink("showResetDocumentDialog",
                resetDocumentDialog::show));
        resetDocumentLink.add(enabledWhen(this::isEditable));
    }

    /**
     * Like the annotator's finish button on the annotation page, the button shows the state that
     * clicking it leads to (the tooltip says the current one), so the symbols are the other way
     * round than in status displays such as the monitoring page.
     */
    public String getStateClass()
    {
        var state = editorContext.getAnnotatorState();

        if (curationDocumentService.isCurationFinished(state.getDocument())) {
            return FontAwesome7IconType.clipboard_s.cssClassName();
        }
        else {
            return FontAwesome7IconType.clipboard_check_s.cssClassName();
        }
    }

    private String getStateTooltipKey()
    {
        var state = editorContext.getAnnotatorState();

        if (curationDocumentService.isCurationFinished(state.getDocument())) {
            return "stateToggle.curationFinished";
        }
        else {
            return "stateToggle.curationInProgress";
        }
    }

    protected boolean isEditable()
    {
        var state = editorContext.getAnnotatorState();
        if (state.getProject() == null || state.getDocument() == null) {
            return false;
        }

        var sourceDocument = documentService.getSourceDocument(state.getDocument().getProject(),
                state.getDocument().getName());
        return sourceDocument.getState() != CURATION_FINISHED;
    }

    protected void actionToggleCurationState(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        var state = editorContext.getAnnotatorState();
        var sourceDocument = state.getDocument();
        var latest = documentService.getSourceDocument(sourceDocument.getProject().getId(),
                sourceDocument.getId());

        // The click is meant for the state the curator sees, which is the editor's copy of the
        // document. If somebody changed the state in the meantime (another curator, or a reset on
        // the monitoring page), acting on either state may do something the curator did not
        // intend - e.g. finish a curation that was just reset - so refuse instead.
        if (latest.getState() != sourceDocument.getState()) {
            page.error("The state of document [" + sourceDocument.getName()
                    + "] has been changed in the meantime. Reload the page to see its current "
                    + "state.");
            aTarget.addChildren(page, IFeedback.class);
            return;
        }

        // Write through the latest document, not the editor's copy, so the change is announced
        // with the correct previous state and no other stale fields are written back
        var docState = latest.getState();

        switch (docState) {
        case CURATION_IN_PROGRESS:
            try {
                page.actionValidateDocument(aTarget, editorContext);
            }
            catch (ValidationException e) {
                page.error("Document cannot be marked as finished: " + e.getMessage());
                aTarget.addChildren(page, IFeedback.class);
                return;
            }

            documentService.setSourceDocumentState(latest, CURATION_FINISHED);
            sourceDocument.copyStateFrom(latest);
            editorContext.refreshAfterDocumentStateChange(aTarget);
            break;
        case CURATION_FINISHED:
            documentService.setSourceDocumentState(latest, CURATION_IN_PROGRESS);
            sourceDocument.copyStateFrom(latest);
            editorContext.refreshAfterDocumentStateChange(aTarget);
            break;
        default:
            page.error(
                    "Can only change document state for documents that are finished or in progress, "
                            + "but document is in state [" + docState + "]");
            aTarget.addChildren(getPage(), IFeedback.class);
            break;
        }
    }

    @Override
    public void actionFinishOrReopenDocument(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        if (editorContext.getAnnotatorState().getDocument() == null) {
            return;
        }

        actionToggleCurationState(aTarget);
    }

    protected void actionResetDocument(AjaxRequestTarget aTarget, Form<MergeDialog.State> aForm)
        throws Exception
    {
        if (!isEditable()) {
            throw new NotEditableException("Curation is already finished. You can put it back "
                    + "into progress via the monitoring page.");
        }

        MergeStrategyFactory<?> mergeStrategyFactory = curationService
                .getMergeStrategyFactory(curationWorkflowModel.getObject());
        MergeStrategy mergeStrategy = curationService
                .getMergeStrategy(curationWorkflowModel.getObject());

        var state = editorContext.getAnnotatorState();

        // The dialog offers the annotators that were curatable when it was opened. Some may have
        // stopped being curatable since, e.g. because they were reset on the monitoring page.
        // Check this before saving anything, and close the dialog so that reopening it offers the
        // current annotators.
        var curatableDataOwners = curationSessionService
                .listCuratableDataOwners(state.getDocument());
        var dataOwners = aForm.getModelObject().getDataOwners().stream() //
                .filter(curatableDataOwners::contains) //
                .toList();
        if (dataOwners.isEmpty()) {
            getPage().error("Unable to merge: none of the selected annotators has annotations "
                    + "that can be curated any more.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (aForm.getModelObject().isSaveSettingsAsDefault()) {
            curationService.createOrUpdateCurationWorkflow(curationWorkflowModel.getObject());
            getPage().success("Updated project merge strategy settings");
            aTarget.addChildren(getPage(), IFeedback.class);
        }

        var mergeMode = aForm.getModelObject().isClearTargetCas() ? RECREATE : FILL_ONLY;

        ((CuratableDocumentPage) page).readOrCreateCurationCas(state.getDocument(), state,
                mergeStrategy, mergeMode, dataOwners);

        // ... and load it
        editorContext.actionLoadDocument(aTarget);

        getPage().success("Re-merge using [" + mergeStrategyFactory.getLabel() + "] finished!");
        aTarget.addChildren(getPage(), IFeedback.class);
    }
}
