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
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static wicket.contrib.input.events.EventType.click;
import static wicket.contrib.input.events.key.KeyType.Ctrl;
import static wicket.contrib.input.events.key.KeyType.End;

import java.io.IOException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameModifier;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.ValidationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.LegacyCurationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.MergeDialog;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class CuratorWorkflowActionBarItemGroup
    extends Panel
{
    private static final long serialVersionUID = 8596786586955459711L;

    private @SpringBean DocumentService documentService;
    private @SpringBean CurationService curationService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean UserDao userRepository;

    private final AnnotationPageBase page;
    // private final ConfirmationDialog finishDocumentDialog;
    private final LambdaAjaxLink toggleCurationStateLink;
    private final IModel<CurationWorkflow> curationWorkflowModel;
    private MergeDialog resetDocumentDialog;
    private LambdaAjaxLink resetDocumentLink;

    public CuratorWorkflowActionBarItemGroup(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        page = aPage;

        // add(finishDocumentDialog = new ConfirmationDialog("finishDocumentDialog",
        // new StringResourceModel("FinishDocumentDialog.title", this, null),
        // new StringResourceModel("FinishDocumentDialog.text", this, null)));

        add(toggleCurationStateLink = new LambdaAjaxLink("toggleCurationState",
                this::actionToggleCurationState));
        toggleCurationStateLink.setOutputMarkupId(true);
        toggleCurationStateLink.add(new Label("state")
                .add(new CssClassNameModifier(LambdaModel.of(this::getStateClass))));
        toggleCurationStateLink.add(new InputBehavior(new KeyType[] { Ctrl, End }, click));

        curationWorkflowModel = Model.of(
                curationService.readOrCreateCurationWorkflow(page.getModelObject().getProject()));
        IModel<String> documentNameModel = PropertyModel.of(page.getModel(), "document.name");
        add(resetDocumentDialog = new MergeDialog("resetDocumentDialog",
                new ResourceModel("ResetDocumentDialog.title"),
                new ResourceModel("ResetDocumentDialog.text"), documentNameModel,
                curationWorkflowModel));
        resetDocumentDialog.setConfirmAction(this::actionResetDocument);

        add(resetDocumentLink = new LambdaAjaxLink("showResetDocumentDialog",
                resetDocumentDialog::show));
        resetDocumentLink.add(enabledWhen(this::isEditable));
    }

    public String getStateClass()
    {
        AnnotatorState state = page.getModelObject();

        if (curationDocumentService.isCurationFinished(state.getDocument())) {
            return FontAwesome5IconType.clipboard_s.cssClassName();
        }
        else {
            return FontAwesome5IconType.clipboard_check_s.cssClassName();
        }
    }

    protected boolean isEditable()
    {
        AnnotatorState state = page.getModelObject();
        if (state.getProject() == null || state.getDocument() == null) {
            return false;
        }

        SourceDocument sourceDocument = documentService
                .getSourceDocument(state.getDocument().getProject(), state.getDocument().getName());
        return sourceDocument.getState() != CURATION_FINISHED;
    }

    protected void actionToggleCurationState(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        var state = page.getModelObject();
        var sourceDocument = state.getDocument();
        var docState = sourceDocument.getState();

        switch (docState) {
        case CURATION_IN_PROGRESS:
            try {
                page.actionValidateDocument(aTarget, page.getEditorCas());
            }
            catch (ValidationException e) {
                page.error("Document cannot be marked as finished: " + e.getMessage());
                aTarget.addChildren(page, IFeedback.class);
                return;
            }

            documentService.setSourceDocumentState(sourceDocument, CURATION_FINISHED);
            aTarget.add(page);
            break;
        case CURATION_FINISHED:
            documentService.setSourceDocumentState(sourceDocument, CURATION_IN_PROGRESS);
            aTarget.add(page);
            break;
        default:
            error("Can only change document state for documents that are finished or in progress, "
                    + "but document is in state [" + docState + "]");
            aTarget.addChildren(getPage(), IFeedback.class);
            break;
        }
    }

    protected void actionResetDocument(AjaxRequestTarget aTarget, Form<MergeDialog.State> aForm)
        throws Exception
    {
        MergeStrategyFactory<?> mergeStrategyFactory = curationService
                .getMergeStrategyFactory(curationWorkflowModel.getObject());
        MergeStrategy mergeStrategy = curationService
                .getMergeStrategy(curationWorkflowModel.getObject());

        if (aForm.getModelObject().isSaveSettingsAsDefault()) {
            curationService.createOrUpdateCurationWorkflow(curationWorkflowModel.getObject());
            getPage().success("Updated project merge strategy settings");
            aTarget.addChildren(getPage(), IFeedback.class);
        }

        ((LegacyCurationPage) page).readOrCreateCurationCas(mergeStrategy, true);

        // ... and load it
        page.actionLoadDocument(aTarget);

        getPage().success("Re-merge using [" + mergeStrategyFactory.getLabel() + "] finished!");
        aTarget.addChildren(getPage(), IFeedback.class);
    }
}
