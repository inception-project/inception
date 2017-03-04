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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getFirstSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getNextPageFirstSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getNumberOfPages;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentenceAt;

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
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.PreferencesUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.AnnotationPreferencesModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.ExportModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.GuidelineModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.YesNoFinishModalPanel;
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

    private ReMergeCasModel reMerge;
    private CurationContainer curationContainer;

    private int gotoPageAddress;
    private int totalNumberOfSentence;
    private long currentprojectId;
    private List<String> crossAnnoSentList;

    // Open the dialog window on first load
    private boolean firstLoad = true;

    private NumberTextField<Integer> gotoPageTextField;
    private Label numberOfPages;
    private DocumentNamePanel documentNamePanel;
    private CurationPanel curationPanel;
    private WebMarkupContainer finish;
    private AjaxLink<Void> showreCreateMergeCasModal;
    private ModalWindow openDocumentsModal;
    private ModalWindow finishCurationModal;
    private ModalWindow reCreateMergeCas;

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

        finish = new WebMarkupContainer("finishImage");
        finish.setOutputMarkupId(true);
        finish.add(new AttributeModifier("src", new LoadableDetachableModel<String>()
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

        add(finishCurationModal = new ModalWindow("finishCurationModal"));
        finishCurationModal.setOutputMarkupId(true);
        finishCurationModal.setInitialWidth(650);
        finishCurationModal.setInitialHeight(40);
        finishCurationModal.setResizable(true);
        finishCurationModal.setWidthUnit("px");
        finishCurationModal.setHeightUnit("px");

        AjaxLink<Void> showFinishCurationModal;
        add(showFinishCurationModal = new AjaxLink<Void>("showFinishCurationModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                actionFinishDocument(target);
            }
        });

        showFinishCurationModal.add(finish);

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
        AnnotatorState state = getModelObject();
        // List of all Source Documents in the project
        List<SourceDocument> listOfSourceDocuements = repository
                .listSourceDocuments(state.getProject());
        List<SourceDocument> sourceDocumentsNotFinished = new ArrayList<SourceDocument>();
        for (SourceDocument sourceDocument : listOfSourceDocuements) {
            if (!repository.existFinishedDocument(sourceDocument, state.getProject())) {
                sourceDocumentsNotFinished.add(sourceDocument);
            }
        }

        listOfSourceDocuements.removeAll(sourceDocumentsNotFinished);
        return listOfSourceDocuements;
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

                        loadDocumentAction(target);
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

    private void actionShowPreviousDocument(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        curationPanel.editor.reset(aTarget);
        // List of all Source Documents in the project
        List<SourceDocument> listOfSourceDocuements = getListOfDocs();

        // Index of the current source document in the list
        int currentDocumentIndex = listOfSourceDocuements.indexOf(state.getDocument());

        // If the first the document
        if (currentDocumentIndex == 0) {
            aTarget.appendJavaScript("alert('This is the first document!')");
        }
        else {
            state.setDocument(listOfSourceDocuements.get(currentDocumentIndex - 1));
            try {
                repository.upgradeCasAndSave(state.getDocument(), state.getMode(),
                        state.getUser().getUsername());

                loadDocumentAction(aTarget);
            }
            catch (Exception e) {
                aTarget.add(getFeedbackPanel());
                LOG.error("Unable to load data", e);
                error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    private void actionShowNextDocument(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        curationPanel.editor.reset(aTarget);
        // List of all Source Documents in the project
        List<SourceDocument> listOfSourceDocuements = getListOfDocs();

        // Index of the current source document in the list
        int currentDocumentIndex = listOfSourceDocuements.indexOf(state.getDocument());

        // If the first document
        if (currentDocumentIndex == listOfSourceDocuements.size() - 1) {
            aTarget.appendJavaScript("alert('This is the last document!')");
        }
        else {
            state.setDocument(listOfSourceDocuements.get(currentDocumentIndex + 1));

            try {
                aTarget.add(getFeedbackPanel());
                repository.upgradeCasAndSave(state.getDocument(), state.getMode(),
                        state.getUser().getUsername());

                loadDocumentAction(aTarget);
            }
            catch (Exception e) {
                aTarget.add(getFeedbackPanel());
                LOG.error("Unable to load data", e);
                error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
            }
        }
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
    {
        AnnotatorState state = getModelObject();
        if (state.getDocument() != null) {

            JCas mergeJCas = null;
            try {
                aTarget.add(getFeedbackPanel());
                mergeJCas = repository.readCurationCas(state.getDocument());
                int previousSentenceAddress = WebAnnoCasUtil
                        .getPreviousDisplayWindowSentenceBeginAddress(mergeJCas,
                                state.getFirstVisibleSentenceAddress(),
                                state.getPreferences().getWindowSize());
                if (state.getFirstVisibleSentenceAddress() != previousSentenceAddress) {

                    updateSentenceNumber(mergeJCas, previousSentenceAddress);

                    aTarget.add(numberOfPages);
                    curationPanel.updatePanel(aTarget, curationContainer);
                    updatePanel(curationContainer, aTarget);
                }
                else {
                    aTarget.appendJavaScript("alert('This is first page!')");
                }
            }
            catch (Exception e) {
                aTarget.add(getFeedbackPanel());
                LOG.error("Unable to load data", e);
                error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
            }
        }
        else {
            aTarget.appendJavaScript("alert('Please open a document first!')");
        }
    }

    private void actionShowNextPage(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        if (state.getDocument() != null) {
            JCas mergeJCas = null;
            try {
                mergeJCas = repository.readCurationCas(state.getDocument());
                int nextSentenceAddress = getNextPageFirstSentenceAddress(mergeJCas,
                        state.getFirstVisibleSentenceAddress(),
                        state.getPreferences().getWindowSize());
                if (state.getFirstVisibleSentenceAddress() != nextSentenceAddress) {
                    aTarget.add(getFeedbackPanel());

                    updateSentenceNumber(mergeJCas, nextSentenceAddress);

                    aTarget.add(numberOfPages);
                    curationPanel.updatePanel(aTarget, curationContainer);
                    updatePanel(curationContainer, aTarget);
                }

                else {
                    aTarget.appendJavaScript("alert('This is last page!')");
                }
            }
            catch (Exception e) {
                aTarget.add(getFeedbackPanel());
                LOG.error("Unable to load data", e);
                error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
            }
        }
        else {
            aTarget.appendJavaScript("alert('Please open a document first!')");
        }
    }

    private void actionShowFirstPage(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        if (state.getDocument() != null) {
            JCas mergeJCas = null;
            try {
                aTarget.add(getFeedbackPanel());
                mergeJCas = repository.readCurationCas(state.getDocument());

                int address = getAddr(
                        selectSentenceAt(mergeJCas, state.getFirstVisibleSentenceBegin(),
                                state.getFirstVisibleSentenceEnd()));
                int firstAddress = getFirstSentenceAddress(mergeJCas);

                if (firstAddress != address) {

                    updateSentenceNumber(mergeJCas, firstAddress);

                    aTarget.add(numberOfPages);
                    curationPanel.updatePanel(aTarget, curationContainer);
                    updatePanel(curationContainer, aTarget);
                }
                else {
                    aTarget.appendJavaScript("alert('This is first page!')");
                }
            }
            catch (Exception e) {
                aTarget.add(getFeedbackPanel());
                LOG.error("Unable to load data", e);
                error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
            }
        }
        else {
            aTarget.appendJavaScript("alert('Please open a document first!')");
        }
    }

    private void actionShowLastPage(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        if (state.getDocument() != null) {
            JCas mergeJCas = null;
            try {
                aTarget.add(getFeedbackPanel());
                mergeJCas = repository.readCurationCas(state.getDocument());
                int lastDisplayWindowBeginingSentenceAddress = WebAnnoCasUtil
                        .getLastDisplayWindowFirstSentenceAddress(mergeJCas,
                                state.getPreferences().getWindowSize());
                if (lastDisplayWindowBeginingSentenceAddress != state
                        .getFirstVisibleSentenceAddress()) {

                    updateSentenceNumber(mergeJCas, lastDisplayWindowBeginingSentenceAddress);

                    aTarget.add(numberOfPages);
                    curationPanel.updatePanel(aTarget, curationContainer);
                    updatePanel(curationContainer, aTarget);
                }
                else {
                    aTarget.appendJavaScript("alert('This is last page!')");
                }
            }
            catch (Exception e) {
                aTarget.add(getFeedbackPanel());
                LOG.error("Unable to load data", e);
                error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
            }
        }
        else {
            aTarget.appendJavaScript("alert('Please open a document first!')");
        }
    }

    private void actionToggleScriptDirection(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        state.toggleScriptDirection();
        
        try {
            curationPanel.updatePanel(aTarget, curationContainer);
            updatePanel(curationContainer, aTarget);
        }
        catch (Exception e) {
            aTarget.add(getFeedbackPanel());
            LOG.error("Unable to load data", e);
            error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        if (state.getDocument() != null
                && state.getDocument().getState().equals(SourceDocumentState.CURATION_FINISHED)) {
            finishCurationModal.setTitle(
                    "Curation was finished. Are you sure you want to re-open document for curation?");
            // Change size if you change text here
            finishCurationModal.setInitialWidth(650);
        }
        else {
            finishCurationModal.setTitle("Are you sure you want to finish curating?");
            // Change size if you change text here
            finishCurationModal.setInitialWidth(370);

        }
        finishCurationModal.setContent(new YesNoFinishModalPanel(finishCurationModal.getContentId(),
                state, finishCurationModal, state.getMode()));
        finishCurationModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = -1746088901018629567L;

            @Override
            public void onClose(AjaxRequestTarget target)
            {
                target.add(finish);
                target.appendJavaScript(
                        "Wicket.Window.unloadConfirmation=false;window.location.reload()");
            }
        });
        finishCurationModal.show(aTarget);
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
                        loadDocumentAction(aTarget);

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

    private void loadDocumentAction(AjaxRequestTarget aTarget)
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

        aTarget.add(finish);
        aTarget.add(numberOfPages);
        aTarget.add(documentNamePanel);
        aTarget.add(showreCreateMergeCasModal);
    }
}
