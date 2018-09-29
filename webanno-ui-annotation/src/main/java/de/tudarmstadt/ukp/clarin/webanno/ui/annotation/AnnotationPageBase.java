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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation;

import static org.apache.uima.fit.util.CasUtil.select;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.ActionBarLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public abstract class AnnotationPageBase
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -1133219266479577443L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    
    private ChallengeResponseDialog resetDocumentDialog;
    private ActionBarLink resetDocumentLink;
    private Label numberOfPages;
    
    protected AnnotationPageBase()
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
    
    protected Label getOrCreatePositionInfoLabel()
    {
        if (numberOfPages == null) {
            numberOfPages = new Label("numberOfPages",
                    new StringResourceModel("PositionInfo.text", this).setModel(getModel())
                            .setParameters(
                                    PropertyModel.of(getModel(), "firstVisibleUnitIndex"),
                                    PropertyModel.of(getModel(), "lastVisibleUnitIndex"),
                                    PropertyModel.of(getModel(), "unitCount"),
                                    LambdaModel.of(() -> getModelObject().getDocumentIndex() + 1),
                                    PropertyModel.of(getModel(), "numberOfDocuments")))
            {
                private static final long serialVersionUID = 7176610419683776917L;

                {
                    setOutputMarkupId(true);
                    setOutputMarkupPlaceholderTag(true);
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(getModelObject().getDocument() != null);
                }
            };
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

    protected void actionShowPreviousPage(AjaxRequestTarget aTarget)
        throws Exception
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToPreviousPage(jcas);
        actionRefreshDocument(aTarget);
    }

    protected void actionShowNextPage(AjaxRequestTarget aTarget)
        throws Exception
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToNextPage(jcas);
        actionRefreshDocument(aTarget);
    }

    protected void actionShowFirstPage(AjaxRequestTarget aTarget)
        throws Exception
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToFirstPage(jcas);
        actionRefreshDocument(aTarget);
    }

    protected void actionShowLastPage(AjaxRequestTarget aTarget)
        throws Exception
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToLastPage(jcas);
        actionRefreshDocument(aTarget);
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
     */
    public void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
    {
        if (!Objects.equals(aDocument.getId(), getModelObject().getDocument().getId())) {
            getModelObject().setDocument(aDocument, getListOfDocs());
            actionLoadDocument(aTarget);
        }
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

    protected abstract JCas getEditorCas()
        throws IOException;

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
    protected abstract void actionRefreshDocument(AjaxRequestTarget aTarget);

    /**
     * Checks if all required features on all annotations are set. If a required feature value is
     * missing, then the method scrolls to that location and schedules a re-rendering. In such
     * a case, an {@link IllegalStateException} is thrown.
     */
    protected void ensureRequiredFeatureValuesSet(AjaxRequestTarget aTarget, JCas aJcas)
    {
        AnnotatorState state = getModelObject();
        CAS editorCas = aJcas.getCas();
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(state.getProject())) {
            TypeAdapter adapter = annotationService.getAdapter(layer);
            List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);
            
            // If no feature is required, then we can skip the whole procedure
            if (features.stream().allMatch((f) -> !f.isRequired())) {
                continue;
            }

            // Check each feature structure of this layer
            for (AnnotationFS fs : select(editorCas, adapter.getAnnotationType(editorCas))) {
                for (AnnotationFeature f : features) {
                    if (WebAnnoCasUtil.isRequiredFeatureMissing(f, fs)) {
                        // Find the sentence that contains the annotation with the missing
                        // required feature value
                        Sentence s = WebAnnoCasUtil.getSentence(aJcas, fs.getBegin());
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
    }
}
