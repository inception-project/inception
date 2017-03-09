/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getNumberOfPages;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ParseException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.PreferencesUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.AnnotationPreferencesModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.ExportModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.GuidelineModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.dialog.ReCreateMergeCASModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.dialog.ReMergeCasModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.core.app.ApplicationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * This is the main class for the curation page. It contains an interface which displays differences
 * between user annotations for a specific document. The interface provides a tool for merging these
 * annotations and storing them as a new annotation.
 *
 */
@MountPath("/curation.html")
public class CurationPage
    extends ApplicationPageBase
{
    private final static Logger LOG = LoggerFactory.getLogger(CurationPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private NumberTextField<Integer> gotoPageTextField;
    private Label numberOfPages;
    private DocumentNamePanel documentNamePanel;
    
    private long currentprojectId;

    // Open the dialog window on first load
    private boolean firstLoad = true;

    private int gotoPageAddress;

    private ModalWindow openDocumentsModal;

    private ReMergeCasModel reMerge;
    private CurationContainer curationContainer;

    private int totalNumberOfSentence;
    private List<String> crossAnnoSentList;

    private CurationPanel curationPanel;
    private AjaxLink<Void> showreCreateMergeCasModal;
    private ModalWindow reCreateMergeCas;

    private WebMarkupContainer finishDocumentIcon;
    private ConfirmationDialog finishDocumentDialog;
    private LambdaAjaxLink finishDocumentLink;
    
    public CurationPage()
    {
        setModel(Model.of(new AnnotatorStateImpl(Mode.CURATION)));
        reMerge = new ReMergeCasModel();

        curationContainer = new CurationContainer();
        curationContainer.setBratAnnotatorModel(getModelObject());

        curationPanel = new CurationPanel("curationPanel", new Model<CurationContainer> (
                curationContainer))
        {
            private static final long serialVersionUID = 2175915644696513166L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = CurationPage.this.getModelObject();
                JCas mergeJCas = null;
                try {
                    mergeJCas = repository.readCurationCas(state.getDocument());
                }
                catch (Exception e) {
                    aTarget.add(getFeedbackPanel());
                    LOG.error("Unable to load data", e);
                    error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                }
                aTarget.add(numberOfPages);
                gotoPageTextField.setModelObject(state.getFirstVisibleSentenceNumber());
                gotoPageAddress = getSentenceAddress(mergeJCas, gotoPageTextField.getModelObject());
                aTarget.add(gotoPageTextField);
                aTarget.add(curationPanel);
            }
        };

        curationPanel.setOutputMarkupId(true);
        add(curationPanel);

        add(documentNamePanel = new DocumentNamePanel("documentNamePanel", getModel()));
        documentNamePanel.setOutputMarkupId(true);

        add(numberOfPages = (Label) new Label("numberOfPages",
                new LoadableDetachableModel<String>()
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String load()
                    {
                        AnnotatorState state = CurationPage.this.getModelObject();
                        if (state.getDocument() != null) {

                            JCas mergeJCas = null;
                            try {
                                mergeJCas = repository.readCurationCas(state.getDocument());

                                totalNumberOfSentence = getNumberOfPages(mergeJCas);

                                // If only one page, start displaying from
                                // sentence 1
                                if (totalNumberOfSentence == 1) {
                                    state.setFirstVisibleSentence(
                                            WebAnnoCasUtil.getFirstSentence(mergeJCas));
                                }
                                List<SourceDocument> listofDoc = getListOfDocs();
                            	
                            	int docIndex = listofDoc.indexOf(state.getDocument())+1;
                               
                                return "showing " + state.getFirstVisibleSentenceNumber() + "-"
                                        + state.getLastVisibleSentenceNumber() + " of "
                                        + totalNumberOfSentence + " sentences [document " + docIndex
                                        + " of " + listofDoc.size() + "]";
                            }
                            catch (Exception e) {
                                return "";
                            }
                        }
                        else {
                            return "";// no document yet selected
                        }

                    }
                }).setOutputMarkupId(true));

        add(openDocumentsModal = new ModalWindow("openDocumentsModal"));
        openDocumentsModal.setOutputMarkupId(true);
        openDocumentsModal.setInitialWidth(620);
        openDocumentsModal.setInitialHeight(440);
        openDocumentsModal.setResizable(true);
        openDocumentsModal.setWidthUnit("px");
        openDocumentsModal.setHeightUnit("px");
        openDocumentsModal.setTitle("Open document");

        add(new AnnotationPreferencesModalPanel("annotationLayersModalPanel", getModel(),
                curationPanel.editor)
        {
            private static final long serialVersionUID = -4657965743173979437L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = CurationPage.this.getModelObject();
                
                // Re-render the whole page because the width of the sidebar may have changed
                aTarget.add(CurationPage.this);
                
                aTarget.add(numberOfPages);
                JCas mergeJCas = null;
                try {
                    aTarget.add(getFeedbackPanel());
                    mergeJCas = repository.readCurationCas(state.getDocument());
                    curationPanel.updatePanel(aTarget, curationContainer);
                    updatePanel(curationContainer, aTarget);
                    updateSentenceNumber(mergeJCas, state.getFirstVisibleSentenceAddress());
                }
                catch (Exception e) {
                    aTarget.add(getFeedbackPanel());
                    LOG.error("Unable to load data", e);
                    error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                }
            }
        });

        add(new ExportModalPanel("exportModalPanel", getModel())
        {
            private static final long serialVersionUID = -468896211970839443L;

            {
                setOutputMarkupId(true);
                setOutputMarkupPlaceholderTag(true);
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                AnnotatorState state = CurationPage.this.getModelObject();
                setVisible(state.getProject() != null
                        && (SecurityUtil.isAdmin(state.getProject(), repository, state.getUser())
                                || !state.getProject().isDisableExport()));
            }
        });

        gotoPageTextField = (NumberTextField<Integer>) new NumberTextField<Integer>("gotoPageText",
                new Model<Integer>(0));
        Form<Void> gotoPageTextFieldForm = new Form<Void>("gotoPageTextFieldForm");
        gotoPageTextFieldForm.add(new AjaxFormSubmitBehavior(gotoPageTextFieldForm, "submit")
        {
            private static final long serialVersionUID = -4549805321484461545L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                if (gotoPageAddress == 0) {
                    aTarget.appendJavaScript("alert('The sentence number entered is not valid')");
                    return;
                }
                AnnotatorState state = CurationPage.this.getModelObject();
                JCas mergeJCas = null;
                try {
                    aTarget.add(getFeedbackPanel());
                    mergeJCas = repository.readCurationCas(state.getDocument());
                    if (state.getFirstVisibleSentenceAddress() != gotoPageAddress) {

                        updateSentenceNumber(mergeJCas, gotoPageAddress);

                        aTarget.add(numberOfPages);
                        updatePanel(curationContainer, aTarget);
                    }
                }
                catch (Exception e) {
                    aTarget.add(getFeedbackPanel());
                    LOG.error("Unable to load data", e);
                    error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                }
            }
        });

        gotoPageTextField.setType(Integer.class);
        gotoPageTextField.setMinimum(1);
        gotoPageTextField.setDefaultModelObject(1);
        add(gotoPageTextFieldForm.add(gotoPageTextField));
        gotoPageTextField.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = 1244526899787707931L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = CurationPage.this.getModelObject();
                JCas mergeJCas = null;
                try {
                    aTarget.add(getFeedbackPanel());
                    mergeJCas = repository.readCurationCas(state.getDocument());
                    gotoPageAddress = getSentenceAddress(mergeJCas,
                            gotoPageTextField.getModelObject());
                }
                catch (Exception e) {
                    aTarget.add(getFeedbackPanel());
                    LOG.error("Unable to load data", e);
                    error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                }
            }
        });

        add(reCreateMergeCas = new ModalWindow("reCreateMergeCasModal"));
        reCreateMergeCas.setOutputMarkupId(true);
        //Change size if you change text here
        reCreateMergeCas.setInitialWidth(580);
        reCreateMergeCas.setInitialHeight(40);
        reCreateMergeCas.setResizable(true);
        reCreateMergeCas.setWidthUnit("px");
        reCreateMergeCas.setHeightUnit("px");
        reCreateMergeCas
                .setTitle("Are you sure? All curation annotations for this document will be lost.");

        add(showreCreateMergeCasModal = new AjaxLink<Void>("showreCreateMergeCasModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            protected void onConfigure()
            {
                AnnotatorState state = CurationPage.this.getModelObject();
                setEnabled(state.getDocument() != null
                        && !state.getDocument().getState()
                                .equals(SourceDocumentState.CURATION_FINISHED));
            }

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                actionRemergeDocument(aTarget);
            }
        });
        showreCreateMergeCasModal.setOutputMarkupId(true);
        
        add(new GuidelineModalPanel("guidelineModalPanel", getModel()));        
        
        add(new LambdaAjaxLink("showOpenDocumentModal", this::actionOpenDocument));
        
        add(new LambdaAjaxLink("showPreviousDocument", this::actionShowPreviousDocument)
                .add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_up },
                        EventType.click)));

        add(new LambdaAjaxLink("showNextDocument", this::actionShowNextDocument)
                .add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_down },
                        EventType.click)));

        add(new LambdaAjaxLink("showNext", this::actionShowNextPage)
                .add(new InputBehavior(new KeyType[] { KeyType.Page_down }, EventType.click)));

        add(new LambdaAjaxLink("showPrevious", this::actionShowPreviousPage)
                .add(new InputBehavior(new KeyType[] { KeyType.Page_up }, EventType.click)));

        add(new LambdaAjaxLink("showFirst", this::actionShowFirstPage)
                .add(new InputBehavior(new KeyType[] { KeyType.Home }, EventType.click)));

        add(new LambdaAjaxLink("showLast", this::actionShowLastPage)
                .add(new InputBehavior(new KeyType[] { KeyType.End }, EventType.click)));

        add(new LambdaAjaxLink("gotoPageLink", this::actionGotoPage));

        add(new LambdaAjaxLink("toggleScriptDirection", this::actionToggleScriptDirection));
        
        add(finishDocumentDialog = new ConfirmationDialog("finishDocumentDialog",
                new StringResourceModel("FinishDocumentDialog.title", this, null),
                new StringResourceModel("FinishDocumentDialog.text", this, null)));
        add(finishDocumentLink = new LambdaAjaxLink("showFinishDocumentDialog",
                this::actionFinishDocument)
        {
            private static final long serialVersionUID = 874573384012299998L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                AnnotatorState state = CurationPage.this.getModelObject();
                setEnabled(state.getProject() != null && state.getDocument() != null
                        && !repository
                                .getSourceDocument(state.getDocument().getProject(),
                                        state.getDocument().getName())
                                .getState().equals(SourceDocumentState.CURATION_FINISHED));
            }
        });
        finishDocumentIcon = new WebMarkupContainer("finishImage");
        finishDocumentIcon.setOutputMarkupId(true);
        finishDocumentIcon.add(new AttributeModifier("src", new LoadableDetachableModel<String>()
        {
            private static final long serialVersionUID = 1562727305401900776L;

            @Override
            protected String load()
            {
                AnnotatorState state = CurationPage.this.getModelObject();
                if (state.getProject() != null && state.getDocument() != null) {
                    if (repository
                            .getSourceDocument(state.getDocument().getProject(),
                                    state.getDocument().getName()).getState()
                            .equals(SourceDocumentState.CURATION_FINISHED)) {
                        return "images/accept.png";
                    }
                    else {
                        return "images/inprogress.png";
                    }
                }
                else {
                    return "images/inprogress.png";
                }
            }
        }));
        finishDocumentLink.add(finishDocumentIcon);
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
    
    private List<SourceDocument> getListOfDocs()
    {
        return repository.listCuratableSourceDocuments(getModelObject().getProject());
    }

    // Update the curation panel.

    private void updatePanel(CurationContainer aCurationContainer, AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        JCas mergeJCas = null;
        try {
            mergeJCas = repository.readCurationCas(state.getDocument());
        }
        catch (Exception e) {
            aTarget.add(getFeedbackPanel());
            LOG.error("Unable to load data", e);
            error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
        }
        gotoPageTextField.setModelObject(state.getFirstVisibleSentenceNumber());
        gotoPageAddress = getSentenceAddress(mergeJCas, gotoPageTextField.getModelObject());
        curationPanel.setOutputMarkupId(true);
        aTarget.add(gotoPageTextField);
        curationPanel.setDefaultModelObject(curationContainer);
        aTarget.add(curationPanel);
    }

    /**
     * for the first time, open the <b>open document dialog</b>
     */
    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead(response);

        String jQueryString = "";
        if (firstLoad) {
            jQueryString += "jQuery('#showOpenDocumentModal').trigger('click');";
            firstLoad = false;
        }
        response.render(OnLoadHeaderItem.forScript(jQueryString));
    }

    private JCas getEditorCas()
        throws IOException, UIMAException, ClassNotFoundException
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        return repository.readCurationCas(state.getDocument());
    }

    private void updateSentenceNumber(JCas aJCas, int aAddress)
    {
        AnnotatorState state = getModelObject();
        Sentence sentence = selectByAddr(aJCas, Sentence.class, aAddress);
        state.setFirstVisibleSentence(sentence);
        state.setFocusSentenceNumber(WebAnnoCasUtil.getSentenceNumber(aJCas, sentence.getBegin()));
    }

    private void actionOpenDocument(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        state.getSelection().clear();
        openDocumentsModal.setContent(new OpenModalWindowPanel(openDocumentsModal.getContentId(),
                state, openDocumentsModal, state.getMode()));
        openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = -1746088901018629567L;

            @Override
            public void onClose(AjaxRequestTarget target)
            {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                /*
                 * Changed for #152, getDocument was returning null even after opening a document
                 * Also, surrounded following code into if block to avoid error.
                 */
                if (state.getProject() == null) {
                    setResponsePage(getApplication().getHomePage());
                    return;
                }
                if (state.getDocument() != null) {
                    try {
                        repository.createSourceDocument(state.getDocument());
                        repository.upgradeCasAndSave(state.getDocument(), state.getMode(), username);

                        actionLoadDocument(target);
                        curationPanel.editor.loadFeatureEditorModels(target);
                    }
                    catch (Exception e) {
                        LOG.error("Unable to load data", e);
                        error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            }
        });
        openDocumentsModal.show(aTarget);
    }

    /**
     * Show the previous document, if exist
     */
    private void actionShowPreviousDocument(AjaxRequestTarget aTarget)
        throws IOException, DataRetrievalFailureException, UIMAException, ClassNotFoundException,
        AnnotationException
    {
        AnnotatorState state = getModelObject();
        state.moveToPreviousDocument(getListOfDocs());
        
        repository.upgradeCasAndSave(state.getDocument(), state.getMode(),
                state.getUser().getUsername());
        actionLoadDocument(aTarget);
    }

    /**
     * Show the next document if exist
     */
    private void actionShowNextDocument(AjaxRequestTarget aTarget)
        throws IOException, DataRetrievalFailureException, UIMAException, ClassNotFoundException,
        AnnotationException
    {
        AnnotatorState state = getModelObject();
        state.moveToNextDocument(getListOfDocs());

        repository.upgradeCasAndSave(state.getDocument(), state.getMode(),
                state.getUser().getUsername());
        actionLoadDocument(aTarget);
    }

    private void actionGotoPage(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        if (gotoPageAddress == 0) {
            aTarget.appendJavaScript("alert('The sentence number entered is not valid')");
            return;
        }
        if (state.getDocument() == null) {
            aTarget.appendJavaScript("alert('Please open a document first!')");
            return;
        }

        JCas mergeJCas = null;
        try {
            aTarget.add(getFeedbackPanel());
            mergeJCas = repository.readCurationCas(state.getDocument());
            if (state.getFirstVisibleSentenceAddress() != gotoPageAddress) {

                updateSentenceNumber(mergeJCas, gotoPageAddress);

                aTarget.add(numberOfPages);
                curationPanel.updatePanel(aTarget, curationContainer);
            }
        }
        catch (Exception e) {
            aTarget.add(getFeedbackPanel());
            LOG.error("Unable to load data", e);
            error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void actionShowPreviousPage(AjaxRequestTarget aTarget)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToPreviousPage(jcas);
        actionRefreshDocument(aTarget);
    }

    private void actionShowNextPage(AjaxRequestTarget aTarget)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToNextPage(jcas);
        actionRefreshDocument(aTarget);
    }

    private void actionShowFirstPage(AjaxRequestTarget aTarget)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToFirstPage(jcas);
        actionRefreshDocument(aTarget);
    }

    private void actionShowLastPage(AjaxRequestTarget aTarget)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToLastPage(jcas);
        actionRefreshDocument(aTarget);
    }
    
    private void actionToggleScriptDirection(AjaxRequestTarget aTarget)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        getModelObject().toggleScriptDirection();

        curationPanel.updatePanel(aTarget, curationContainer);
        updatePanel(curationContainer, aTarget);
    }

    private void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        finishDocumentDialog.setConfirmAction((target) -> {
            AnnotatorState state = getModelObject();
            
            SourceDocument sourceDocument = state.getDocument();

            if (sourceDocument.getState().equals(SourceDocumentState.CURATION_FINISHED)) {
                sourceDocument.setState(SourceDocumentStateTransition.transition(
                        SourceDocumentStateTransition.CURATION_FINISHED_TO_CURATION_IN_PROGRESS));
            }
            else {
                sourceDocument.setState(SourceDocumentStateTransition.transition(
                        SourceDocumentStateTransition.CURATION_IN_PROGRESS_TO_CURATION_FINISHED));
                sourceDocument.setProcessed(false);
            }
            
            repository.createSourceDocument(sourceDocument);
            
            target.add(finishDocumentIcon);
            target.add(finishDocumentLink);
            target.add(curationPanel.editor);
            target.add(showreCreateMergeCasModal);
        });
        finishDocumentDialog.show(aTarget);
    }

    private void actionRemergeDocument(AjaxRequestTarget aTarget)
    {
        reCreateMergeCas.setContent(new ReCreateMergeCASModalPanel(reCreateMergeCas.getContentId(),
                reCreateMergeCas, reMerge));
        reCreateMergeCas.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = 4816615910398625993L;

            @Override
            public void onClose(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = CurationPage.this.getModelObject();
                if (reMerge.isReMerege()) {
                    try {
                        aTarget.add(getFeedbackPanel());
                        repository.removeCurationDocumentContent(state.getDocument(),
                                state.getUser().getUsername());
                        actionLoadDocument(aTarget);

                        aTarget.appendJavaScript("alert('Re-merge finished!')");
                    }
                    catch (Exception e) {
                        aTarget.add(getFeedbackPanel());
                        LOG.error("Unable to load data", e);
                        error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            }
        });
        reCreateMergeCas.show(aTarget);
    }

    /**
     * Open a document or to a different document. This method should be used only the first time
     * that a document is accessed. It reset the annotator state and upgrades the CAS.
     */
    private void actionLoadDocument(AjaxRequestTarget aTarget)
        throws DataRetrievalFailureException, IOException, UIMAException, ClassNotFoundException,
        AnnotationException
    {
        AnnotatorState state = getModelObject();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User userLoggedIn = userRepository
                .get(SecurityContextHolder.getContext().getAuthentication().getName());

        // Update source document state to CURRATION_INPROGRESS, if it was not ANNOTATION_FINISHED
        if (!state.getDocument().getState().equals(SourceDocumentState.CURATION_FINISHED)) {
            state.getDocument().setState(SourceDocumentStateTransition.transition(
                    SourceDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS));
            repository.createSourceDocument(state.getDocument());
        }

        state.setUser(userLoggedIn);
        // Load user preferences
        PreferencesUtil.setAnnotationPreference(username, repository, annotationService, state,
                state.getMode());
        // Re-render whole page as sidebar size preference may have changed
        aTarget.add(CurationPage.this);

        List<AnnotationDocument> finishedAnnotationDocuments = new ArrayList<AnnotationDocument>();

        for (AnnotationDocument annotationDocument : repository
                .listAnnotationDocuments(state.getDocument())) {
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                finishedAnnotationDocuments.add(annotationDocument);
            }
        }

        SuggestionBuilder cb = new SuggestionBuilder(repository, annotationService, userRepository);
        AnnotationDocument randomAnnotationDocument = null;
        if (finishedAnnotationDocuments.size() > 0) {
            randomAnnotationDocument = finishedAnnotationDocuments.get(0);
        }

        // upgrade CASes for each user, what if new type is added once the user finished
        // annotation
        for (AnnotationDocument ad : finishedAnnotationDocuments) {
            repository.upgradeCasAndSave(ad.getDocument(), state.getMode(), ad.getUser());
        }
        Map<String, JCas> jCases = cb.listJcasesforCuration(finishedAnnotationDocuments,
                randomAnnotationDocument, state.getMode());
        JCas mergeJCas = cb.getMergeCas(state, state.getDocument(), jCases,
                randomAnnotationDocument);

        // (Re)initialize brat model after potential creating / upgrading CAS
        state.initForDocument(mergeJCas, repository);
        state.getPreferences().setCurationWindowSize(WebAnnoCasUtil.getSentenceSize(mergeJCas));

        // if project is changed, reset some project specific settings
        if (currentprojectId != state.getProject().getId()) {
            state.clearRememberedFeatures();
        }

        currentprojectId = state.getProject().getId();

        SuggestionBuilder builder = new SuggestionBuilder(repository, annotationService,
                userRepository);
        curationContainer = builder.buildCurationContainer(state);
        curationContainer.setBratAnnotatorModel(state);
        curationPanel.editor.reset(aTarget);
        curationPanel.updatePanel(aTarget, curationContainer);
        updatePanel(curationContainer, aTarget);
        updateSentenceNumber(mergeJCas, state.getFirstVisibleSentenceAddress());

        
        // Load constraints
        try {
            state.setConstraints(repository.loadConstraints(state.getProject()));
        }
        catch (ParseException e) {
            LOG.error("Error", e);
            // aTarget.addChildren(getPage(), FeedbackPanel.class);
            error(e.getMessage());
        }

        aTarget.add(numberOfPages);
        aTarget.add(documentNamePanel);
        aTarget.add(showreCreateMergeCasModal);
        aTarget.add(finishDocumentLink);
    }

    private void actionRefreshDocument(AjaxRequestTarget aTarget)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        aTarget.add(numberOfPages);
        curationPanel.updatePanel(aTarget, curationContainer);
        updatePanel(curationContainer, aTarget);
    }
}
