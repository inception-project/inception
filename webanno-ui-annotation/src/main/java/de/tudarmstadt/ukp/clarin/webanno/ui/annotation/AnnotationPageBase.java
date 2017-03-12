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

import java.io.IOException;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.webapp.core.app.ApplicationPageBase;

public abstract class AnnotationPageBase
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -1133219266479577443L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;
    
    private ChallengeResponseDialog resetDocumentDialog;
    private LambdaAjaxLink resetDocumentLink;
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
            numberOfPages = new Label("numberOfPages", new StringResourceModel("PositionInfo.text", 
                    this, getModel(), 
                    PropertyModel.of(getModel(), "firstVisibleSentenceNumber"),
                    PropertyModel.of(getModel(), "lastVisibleSentenceNumber"),
                    PropertyModel.of(getModel(), "numberOfSentences"),
                    PropertyModel.of(getModel(), "documentIndex"),
                    PropertyModel.of(getModel(), "numberOfDocuments"))) {
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
                    new StringResourceModel("ResetDocumentDialog.title", this, null),
                    new StringResourceModel("ResetDocumentDialog.text", this, getModel(),
                            documentNameModel),
                    documentNameModel);
            resetDocumentDialog.setConfirmAction(this::actionResetDocument);
        }
        return resetDocumentDialog;
    }

    protected LambdaAjaxLink createOrGetResetDocumentLink()
    {
        if (resetDocumentLink == null) {
            resetDocumentLink = new LambdaAjaxLink("showResetDocumentDialog",
                    t -> { resetDocumentDialog.show(t); })
            {
                private static final long serialVersionUID = 874573384012299998L;
    
                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    AnnotatorState state = AnnotationPageBase.this.getModelObject();
                    setEnabled(state.getDocument() != null && !repository
                            .isAnnotationFinished(state.getDocument(), state.getUser()));
                }
            };
            resetDocumentLink.setOutputMarkupId(true);
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
        actionRefreshDocument(aTarget, jcas);
    }

    protected void actionShowNextPage(AjaxRequestTarget aTarget)
        throws Exception
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToNextPage(jcas);
        actionRefreshDocument(aTarget, jcas);
    }

    protected void actionShowFirstPage(AjaxRequestTarget aTarget)
        throws Exception
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToFirstPage(jcas);
        actionRefreshDocument(aTarget, jcas);
    }

    protected void actionShowLastPage(AjaxRequestTarget aTarget)
        throws Exception
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToLastPage(jcas);
        actionRefreshDocument(aTarget, jcas);
    }
    
    protected void actionResetDocument(AjaxRequestTarget aTarget)
        throws Exception
    {
        AnnotatorState state = getModelObject();
        JCas jcas = repository.createOrReadInitialCas(state.getDocument());
        repository.writeAnnotationCas(jcas, state.getDocument(), state.getUser());
        actionLoadDocument(aTarget);
    }

    protected void handleException(AjaxRequestTarget aTarget, Exception aException)
    {
        LoggerFactory.getLogger(getClass()).error("Error: " + aException.getMessage(), aException);
        error("Error: " + aException.getMessage());
        if (aTarget != null) {
            aTarget.addChildren(getPage(), FeedbackPanel.class);
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
    protected abstract void actionRefreshDocument(AjaxRequestTarget aTarget, JCas aJcas);
}
