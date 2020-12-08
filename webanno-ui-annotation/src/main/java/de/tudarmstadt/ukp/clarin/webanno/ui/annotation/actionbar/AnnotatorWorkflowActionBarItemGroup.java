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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameModifier;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;

public class AnnotatorWorkflowActionBarItemGroup
    extends Panel
{
    private static final long serialVersionUID = 4139817495914347777L;

    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userRepository;

    private final AnnotationPageBase page;
    protected final ConfirmationDialog finishDocumentDialog;
    private final LambdaAjaxLink finishDocumentLink;
    private ChallengeResponseDialog resetDocumentDialog;
    private LambdaAjaxLink resetDocumentLink;

    public AnnotatorWorkflowActionBarItemGroup(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        page = aPage;

        add(finishDocumentDialog = new ConfirmationDialog("finishDocumentDialog",
                new StringResourceModel("FinishDocumentDialog.title", this, null),
                new StringResourceModel("FinishDocumentDialog.text", this, null)));
        
        add(finishDocumentLink = new LambdaAjaxLink("showFinishDocumentDialog",
                this::actionFinishDocument));
        finishDocumentLink.setOutputMarkupId(true);
        finishDocumentLink.add(enabledWhen(() -> page.isEditable()));
        finishDocumentLink.add(new Label("state")
                .add(new CssClassNameModifier(LambdaModel.of(this::getStateClass))));

        IModel<String> documentNameModel = PropertyModel.of(page.getModel(), "document.name");
        add(resetDocumentDialog = new ChallengeResponseDialog("resetDocumentDialog",
                new StringResourceModel("ResetDocumentDialog.title", this),
                new StringResourceModel("ResetDocumentDialog.text", this)
                        .setModel(page.getModel()).setParameters(documentNameModel),
                documentNameModel));
        resetDocumentDialog.setConfirmAction(this::actionResetDocument);

        add(resetDocumentLink = new LambdaAjaxLink("showResetDocumentDialog",
                resetDocumentDialog::show));
        resetDocumentLink.add(enabledWhen(() -> page.isEditable()));
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

    protected void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        finishDocumentDialog.setConfirmAction((_target) -> {
            page.actionValidateDocument(_target, page.getEditorCas());

            AnnotatorState state = page.getModelObject();
            AnnotationDocument annotationDocument = documentService
                    .getAnnotationDocument(state.getDocument(), state.getUser());

            documentService.transitionAnnotationDocumentState(annotationDocument,
                    ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED);

            // manually update state change!! No idea why it is not updated in the DB
            // without calling createAnnotationDocument(...)
            documentService.createAnnotationDocument(annotationDocument);
            
            // curation sidebar: need to update source doc state as well to finished
            if (state.getUser().getUsername().equals(WebAnnoConst.CURATION_USER)) {
                documentService.transitionSourceDocumentState(state.getDocument(),
                        SourceDocumentStateTransition.CURATION_IN_PROGRESS_TO_CURATION_FINISHED);
            }

            _target.add(page);
        });
        finishDocumentDialog.show(aTarget);
    }

    protected void actionResetDocument(AjaxRequestTarget aTarget) throws Exception
    {
        AnnotatorState state = page.getModelObject();
        documentService.resetAnnotationCas(state.getDocument(), state.getUser());
        page.actionLoadDocument(aTarget);
    }
}
