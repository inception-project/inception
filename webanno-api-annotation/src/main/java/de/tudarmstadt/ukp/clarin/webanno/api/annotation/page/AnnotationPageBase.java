/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentenceCovering;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.uima.fit.util.CasUtil.select;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.SentenceOrientedPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.ActionBarLink;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

public abstract class AnnotationPageBase
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -1133219266479577443L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean UserPreferencesService userPreferenceService;
    
    private ChallengeResponseDialog resetDocumentDialog;
    private ActionBarLink resetDocumentLink;
    private Component numberOfPages;

    public AnnotationPageBase()
    {
        super();
    }

    protected AnnotationPageBase(PageParameters aParameters)
    {
        super(aParameters);
    }

    public void setModel(IModel<AnnotatorState> aModel)
    {
        setDefaultModel(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public void setModelObject(AnnotatorState aModel)
    {
        setDefaultModelObject(aModel);
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }
    
    protected Component getOrCreatePositionInfoLabel()
    {
        if (numberOfPages == null) {
            numberOfPages = new SentenceOrientedPagingStrategy().createPositionLabel("numberOfPages",
                    getModel());
            numberOfPages.add(visibleWhen(() -> getModelObject().getDocument() != null));
        }
        return numberOfPages;
    }    
    
    protected ChallengeResponseDialog createOrGetResetDocumentDialog()
    {
        if (resetDocumentDialog == null) {
            IModel<String> documentNameModel = PropertyModel.of(getModel(), "document.name");
            resetDocumentDialog = new ChallengeResponseDialog("resetDocumentDialog",
                    new StringResourceModel("ResetDocumentDialog.title", this),
                    new StringResourceModel("ResetDocumentDialog.text", this).setModel(getModel())
                            .setParameters(documentNameModel),
                    documentNameModel);
            resetDocumentDialog.setConfirmAction(this::actionResetDocument);
        }
        return resetDocumentDialog;
    }

    protected ActionBarLink createOrGetResetDocumentLink()
    {
        if (resetDocumentLink == null) {
            resetDocumentLink = new ActionBarLink("showResetDocumentDialog", t -> 
                resetDocumentDialog.show(t));
            resetDocumentLink.onConfigure(_this -> {
                AnnotatorState state = AnnotationPageBase.this.getModelObject();
                _this.setEnabled(state.getDocument() != null && !documentService
                        .isAnnotationFinished(state.getDocument(), state.getUser()));
            });
        }
        return resetDocumentLink;
    }

    /**
     * Show the previous document, if exist
     */
    protected void actionShowPreviousDocument(AjaxRequestTarget aTarget)
    {
        getModelObject().moveToPreviousDocument(getListOfDocs());
        actionLoadDocument(aTarget);
    }

    /**
     * Show the next document if exist
     */
    protected void actionShowNextDocument(AjaxRequestTarget aTarget)
    {
        getModelObject().moveToNextDocument(getListOfDocs());
        actionLoadDocument(aTarget);
    }

    protected void actionResetDocument(AjaxRequestTarget aTarget)
        throws Exception
    {
        AnnotatorState state = getModelObject();
        documentService.resetAnnotationCas(state.getDocument(), state.getUser());
        actionLoadDocument(aTarget);
    }

    /**
     * Show the specified document.
     * 
     * @return whether the document had to be switched or not.
     */
    public boolean actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
    {
        if (!Objects.equals(aDocument.getId(), getModelObject().getDocument().getId())) {
            getModelObject().setDocument(aDocument, getListOfDocs());
            actionLoadDocument(aTarget);
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Show the next document if it exists, starting in a certain begin offset
     */
    public void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            int aBegin, int aEnd)
        throws IOException
    {
        boolean switched = actionShowSelectedDocument(aTarget, aDocument);

        AnnotatorState state = getModelObject();

        // If the document was not switched and the requested offset is already visible on screen,
        // then there is no need to change the screen contents
        if (switched || !(state.getWindowBeginOffset() <= aBegin
                && aEnd <= state.getWindowEndOffset())) {
            CAS cas = getEditorCas();
            state.setFirstVisibleUnit(selectSentenceCovering(cas, aBegin));
            state.setFocusUnitIndex(getSentenceNumber(cas, aBegin));
        }
        
        actionRefreshDocument(aTarget);
    }

    protected void handleException(AjaxRequestTarget aTarget, Exception aException)
    {
        LoggerFactory.getLogger(getClass()).error("Error: " + aException.getMessage(), aException);
        error("Error: " + aException.getMessage());
        if (aTarget != null) {
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    protected abstract List<SourceDocument> getListOfDocs();

    public abstract CAS getEditorCas() throws IOException;
    
    public abstract void writeEditorCas(CAS aCas) throws IOException;
    
    /**
     * Open a document or to a different document. This method should be used only the first time
     * that a document is accessed. It reset the annotator state and upgrades the CAS.
     */
    protected abstract void actionLoadDocument(AjaxRequestTarget aTarget);

    /**
     * Re-render the document and update all related UI elements.
     * 
     * This method should be used while the editing process is ongoing. It does not upgrade the CAS
     * and it does not reset the annotator state.
     */
    public abstract void actionRefreshDocument(AjaxRequestTarget aTarget);

    /**
     * Checks if all required features on all annotations are set. If a required feature value is
     * missing, then the method scrolls to that location and schedules a re-rendering. In such
     * a case, an {@link IllegalStateException} is thrown.
     */
    protected void validateRequiredFeatures(AjaxRequestTarget aTarget, CAS aCas,
            TypeAdapter aAdapter)
    {
        AnnotatorState state = getModelObject();
        
        CAS editorCas = aCas;
        AnnotationLayer layer = aAdapter.getLayer();
        List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);
        
        // If no feature is required, then we can skip the whole procedure
        if (features.stream().allMatch((f) -> !f.isRequired())) {
            return;
        }

        // Check each feature structure of this layer
        for (AnnotationFS fs : select(editorCas, aAdapter.getAnnotationType(editorCas))) {
            for (AnnotationFeature f : features) {
                if (WebAnnoCasUtil.isRequiredFeatureMissing(f, fs)) {
                    // Find the sentence that contains the annotation with the missing
                    // required feature value
                    AnnotationFS s = WebAnnoCasUtil.selectSentenceCovering(aCas, fs.getBegin());
                    // Put this sentence into the focus
                    state.setFirstVisibleUnit(s);
                    actionRefreshDocument(aTarget);
                    // Inform the user
                    throw new IllegalStateException(
                            "Document cannot be marked as finished. Annotation with ID ["
                                    + WebAnnoCasUtil.getAddr(fs) + "] on layer ["
                                    + layer.getUiName() + "] is missing value for feature ["
                                    + f.getUiName() + "].");
                }
            }
        }
    }
    
    protected void actionValidateDocument(AjaxRequestTarget aTarget, CAS aCas)
    {
        AnnotatorState state = getModelObject();
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(state.getProject())) {
            if (!layer.isEnabled()) {
                // No validation for disabled layers since there is nothing the annotator could do
                // about fixing annotations on disabled layers.
                continue;
            }
            
            if (ValidationMode.NEVER.equals(layer.getValidationMode())) {
                // If validation is disabled, then skip it
                continue;
            }
            
            TypeAdapter adapter = annotationService.getAdapter(layer);
            
            validateRequiredFeatures(aTarget, aCas, adapter);
            
            List<Pair<LogMessage, AnnotationFS>> messages = adapter.validate(aCas);
            if (!messages.isEmpty()) {
                LogMessage message = messages.get(0).getLeft();
                AnnotationFS fs = messages.get(0).getRight();
                
                // Find the sentence that contains the annotation with the missing
                // required feature value and put this sentence into the focus
                AnnotationFS s = WebAnnoCasUtil.selectSentenceCovering(aCas, fs.getBegin());
                state.setFirstVisibleUnit(s);
                actionRefreshDocument(aTarget);
                
                // Inform the user
                throw new IllegalStateException(
                        "Document cannot be marked as finished. Annotation with ID ["
                                + WebAnnoCasUtil.getAddr(fs) + "] on layer ["
                                + layer.getUiName() + "] is invalid: " + message.getMessage());
            }
        }
    }
    
    /**
     * Load the user preferences. A side-effect of this method is that the active annotation layer
     * is refreshed based on the visibility preferences and based on the project to which the 
     * document being edited belongs.
     */
    protected void loadPreferences() throws BeansException, IOException
    {
        AnnotatorState state = getModelObject();
        PreferencesUtil.loadPreferences(userPreferenceService, annotationService,
                state, state.getUser().getUsername());
    }
}
