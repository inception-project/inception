/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.actionbar;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.CURATION_IN_PROGRESS_TO_CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameModifier;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.CurationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.MergeDialog;

public class CuratorWorkflowActionBarItemGroup
    extends Panel
{
    private static final long serialVersionUID = 8596786586955459711L;
    
    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean UserDao userRepository;

    private final AnnotationPageBase page;
    protected final ConfirmationDialog finishDocumentDialog;
    private final LambdaAjaxLink finishDocumentLink;
    private MergeDialog resetDocumentDialog;
    private LambdaAjaxLink resetDocumentLink;

    public CuratorWorkflowActionBarItemGroup(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        page = aPage;

        add(finishDocumentDialog = new ConfirmationDialog("finishDocumentDialog",
                new StringResourceModel("FinishDocumentDialog.title", this, null),
                new StringResourceModel("FinishDocumentDialog.text", this, null)));
        
        add(finishDocumentLink = new LambdaAjaxLink("showFinishDocumentDialog",
                this::actionFinishDocument));
        finishDocumentLink.setOutputMarkupId(true);
        finishDocumentLink.add(enabledWhen(this::isEditable));
        finishDocumentLink.add(new Label("state")
                .add(new CssClassNameModifier(LambdaModel.of(this::getStateClass))));

        IModel<String> documentNameModel = PropertyModel.of(page.getModel(), "document.name");
        add(resetDocumentDialog = new MergeDialog("resetDocumentDialog",
                new StringResourceModel("ResetDocumentDialog.title", this),
                new StringResourceModel("ResetDocumentDialog.text", this)
                        .setModel(page.getModel()).setParameters(documentNameModel),
                documentNameModel));
        resetDocumentDialog.setConfirmAction(this::actionResetDocument);

        add(resetDocumentLink = new LambdaAjaxLink("showResetDocumentDialog",
                resetDocumentDialog::show));
        resetDocumentLink.add(enabledWhen(this::isEditable));
    }
    
    public String getStateClass()
    {
        AnnotatorState state = page.getModelObject();
        
        if (curationDocumentService.isCurationFinished(state.getDocument())) {
            return FontAwesome5IconType.lock_s.cssClassName();
        }
        else {
            return FontAwesome5IconType.lock_open_s.cssClassName();
        }
    }

    protected boolean isEditable()
    {
        AnnotatorState state = page.getModelObject();
        return state.getProject() != null && state.getDocument() != null
                && !documentService
                        .getSourceDocument(state.getDocument().getProject(),
                                state.getDocument().getName())
                        .getState().equals(CURATION_FINISHED);
    }
    
    protected void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        finishDocumentDialog.setConfirmAction((_target) -> {
            page.actionValidateDocument(_target, page.getEditorCas());
            
            AnnotatorState state = page.getModelObject();
            SourceDocument sourceDocument = state.getDocument();
            
            if (!curationDocumentService.isCurationFinished(sourceDocument)) {
                documentService.transitionSourceDocumentState(sourceDocument,
                        CURATION_IN_PROGRESS_TO_CURATION_FINISHED);
            }
            
            _target.add(page);
        });
        finishDocumentDialog.show(aTarget);
    }

    protected void actionResetDocument(AjaxRequestTarget aTarget, Form<MergeDialog.State> aForm)
        throws Exception
    {
        ((CurationPage) page)
                .readOrCreateMergeCas(aForm.getModelObject().isMergeIncompleteAnnotations(), true);
        
        // ... and load it
        page.actionLoadDocument(aTarget);

        success("Re-merge finished!");
        aTarget.add(page.getFeedbackPanel());
    }
}
