/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getFirstSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getFirstSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getNextPageFirstSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getNumberOfPages;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectSentenceAt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;
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
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.PreferencesUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.ReCreateMergeCASModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.ReMergeCasModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.YesNoFinishModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.home.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.AnnotationLayersModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.ExportModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.GuidelineModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome.WelcomePage;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * This is the main class for the curation page. It contains an interface which displays differences
 * between user annotations for a specific document. The interface provides a tool for merging these
 * annotations and storing them as a new annotation.
 *
 * @author Andreas Straninger
 * @author Seid Muhie Yimam
 */
public class CurationPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 1378872465851908515L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private ReMergeCasModel reMerge;
    private CurationContainer curationContainer;
    private BratAnnotatorModel bModel;

    private int gotoPageAddress;
    private int sentenceNumber = 1;
    private int totalNumberOfSentence;
    private long currentprojectId;
    List<String> crossAnnoSentList;

    // Open the dialog window on first load
    private boolean firstLoad = true;

    private NumberTextField<Integer> gotoPageTextField;
    private Label numberOfPages;
    private DocumentNamePanel documentNamePanel;
    private CurationPanel curationPanel;
    private WebMarkupContainer finish;
    private AjaxLink<Void> showreCreateMergeCasModal;

    @SuppressWarnings("deprecation")
    public CurationPage()
    {
        bModel = new BratAnnotatorModel();
        bModel.setMode(Mode.CURATION);
        reMerge = new ReMergeCasModel();

        curationContainer = new CurationContainer();
        curationContainer.setBratAnnotatorModel(bModel);

        curationPanel = new CurationPanel("curationPanel", new Model<CurationContainer>(
                curationContainer))
        {
            private static final long serialVersionUID = 2175915644696513166L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                JCas mergeJCas = null;
                try {
                    mergeJCas = repository.readCurationCas(bModel.getDocument());
                }
                catch (UIMAException e) {
                    error(e.getMessage());
                }
                catch (ClassNotFoundException e) {
                    error(e.getMessage());
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                aTarget.add(numberOfPages);
                gotoPageTextField.setModelObject(getFirstSentenceNumber(mergeJCas,
                        bModel.getSentenceAddress()) + 1);
                gotoPageAddress = getSentenceAddress(mergeJCas, gotoPageTextField.getModelObject());
                aTarget.add(gotoPageTextField);
                aTarget.add(curationPanel);
            }
        };

        curationPanel.setOutputMarkupId(true);
        add(curationPanel);

        add(documentNamePanel = new DocumentNamePanel("documentNamePanel",
                new Model<BratAnnotatorModel>(bModel)));
        documentNamePanel.setOutputMarkupId(true);

        add(numberOfPages = (Label) new Label("numberOfPages",
                new LoadableDetachableModel<String>()
                {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String load()
                    {
                        if (bModel.getDocument() != null) {

                            JCas mergeJCas = null;
                            try {
                                mergeJCas = repository.readCurationCas(bModel.getDocument());

                                totalNumberOfSentence = getNumberOfPages(mergeJCas);

                                // If only one page, start displaying from
                                // sentence 1
                                if (totalNumberOfSentence == 1) {
                                    bModel.setSentenceAddress(bModel.getFirstSentenceAddress());
                                }
                                sentenceNumber = getFirstSentenceNumber(mergeJCas,
                                        bModel.getSentenceAddress());
                                int firstSentenceNumber = sentenceNumber + 1;
                                int lastSentenceNumber;
                                if (firstSentenceNumber + bModel.getPreferences().getWindowSize()
                                        - 1 < totalNumberOfSentence) {
                                    lastSentenceNumber = firstSentenceNumber
                                            + bModel.getPreferences().getWindowSize() - 1;
                                }
                                else {
                                    lastSentenceNumber = totalNumberOfSentence;
                                }

                                return "showing " + firstSentenceNumber + "-" + lastSentenceNumber
                                        + " of " + totalNumberOfSentence + " sentences";
                            }
                            catch (UIMAException e) {
                                return "";
                            }
                            catch (ClassNotFoundException e) {
                                return "";
                            }
                            catch (IOException e) {
                                return "";
                            }

                        }
                        else {
                            return "";// no document yet selected
                        }

                    }
                }).setOutputMarkupId(true));

        final ModalWindow openDocumentsModal;
        add(openDocumentsModal = new ModalWindow("openDocumentsModal"));
        openDocumentsModal.setOutputMarkupId(true);

        openDocumentsModal.setInitialWidth(500);
        openDocumentsModal.setInitialHeight(300);
        openDocumentsModal.setResizable(true);
        openDocumentsModal.setWidthUnit("px");
        openDocumentsModal.setHeightUnit("px");
        openDocumentsModal.setTitle("Open document");

        // Add project and document information at the top
        add(new AjaxLink<Void>("showOpenDocumentModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                openDocumentsModal.setContent(new OpenModalWindowPanel(openDocumentsModal
                        .getContentId(), bModel, openDocumentsModal, Mode.CURATION));
                openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();

                        if (bModel.getDocument() == null) {
                            setResponsePage(WelcomePage.class);
                            return;
                        }

                        User user = userRepository.get(username);
                        // Update source document state to
                        // CURRATION_INPROGRESS, if it was not
                        // ANNOTATION_FINISHED
                        if (!bModel.getDocument().getState()
                                .equals(SourceDocumentState.CURATION_FINISHED)) {

                            bModel.getDocument().setState(SourceDocumentState.CURATION_IN_PROGRESS);
                        }

                        try {
                            repository.createSourceDocument(bModel.getDocument(), user);
                            repository.upgradeCasAndSave(bModel.getDocument(), Mode.CURATION,
                                    username);

                            loadDocumentAction(target);
                        }
                        catch (IOException e) {
                            target.add(getFeedbackPanel());
                            error(e.getCause().getMessage());
                        }
                    }
                });
                openDocumentsModal.show(aTarget);
            }
        });

        add(new AnnotationLayersModalPanel("annotationLayersModalPanel",
                new Model<BratAnnotatorModel>(bModel))
        {
            private static final long serialVersionUID = -4657965743173979437L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                aTarget.add(getFeedbackPanel());
                aTarget.add(numberOfPages);
                updatePanel(curationContainer, aTarget);
            }
        });

        // Show the previous document, if exist
        add(new AjaxLink<Void>("showPreviousDocument")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                // List of all Source Documents in the project
                List<SourceDocument> listOfSourceDocuements = repository.listSourceDocuments(bModel
                        .getProject());

                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = userRepository.get(username);

                List<SourceDocument> sourceDocumentsinIgnorState = new ArrayList<SourceDocument>();
                for (SourceDocument sourceDocument : listOfSourceDocuements) {
                    if (!repository
                            .existFinishedDocument(sourceDocument, user, bModel.getProject())) {
                        sourceDocumentsinIgnorState.add(sourceDocument);
                    }
                }

                listOfSourceDocuements.removeAll(sourceDocumentsinIgnorState);

                // Index of the current source document in the list
                int currentDocumentIndex = listOfSourceDocuements.indexOf(bModel.getDocument());

                // If the first the document
                if (currentDocumentIndex == 0) {
                    aTarget.appendJavaScript("alert('This is the first document!')");
                }
                else {
                    bModel.setDocumentName(listOfSourceDocuements.get(currentDocumentIndex - 1)
                            .getName());
                    bModel.setDocument(listOfSourceDocuements.get(currentDocumentIndex - 1));
                    try {
                        repository.upgradeCasAndSave(bModel.getDocument(), Mode.CURATION, bModel
                                .getUser().getUsername());

                        loadDocumentAction(aTarget);
                    }
                    catch (IOException e) {
                        aTarget.add(getFeedbackPanel());
                        error(e.getCause().getMessage());
                    }
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_up }, EventType.click)));

        // Show the next document if exist
        add(new AjaxLink<Void>("showNextDocument")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                // List of all Source Documents in the project
                List<SourceDocument> listOfSourceDocuements = repository.listSourceDocuments(bModel
                        .getProject());

                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = userRepository.get(username);

                List<SourceDocument> sourceDocumentsinIgnorState = new ArrayList<SourceDocument>();
                for (SourceDocument sourceDocument : listOfSourceDocuements) {
                    if (!repository
                            .existFinishedDocument(sourceDocument, user, bModel.getProject())) {
                        sourceDocumentsinIgnorState.add(sourceDocument);
                    }
                }

                listOfSourceDocuements.removeAll(sourceDocumentsinIgnorState);

                // Index of the current source document in the list
                int currentDocumentIndex = listOfSourceDocuements.indexOf(bModel.getDocument());

                // If the first document
                if (currentDocumentIndex == listOfSourceDocuements.size() - 1) {
                    aTarget.appendJavaScript("alert('This is the last document!')");
                }
                else {
                    bModel.setDocumentName(listOfSourceDocuements.get(currentDocumentIndex + 1)
                            .getName());
                    bModel.setDocument(listOfSourceDocuements.get(currentDocumentIndex + 1));

                    try {
                        aTarget.add(getFeedbackPanel());
                        repository.upgradeCasAndSave(bModel.getDocument(), Mode.CURATION, bModel
                                .getUser().getUsername());

                        loadDocumentAction(aTarget);
                    }
                    catch (IOException e) {
                        aTarget.add(getFeedbackPanel());
                        error(e.getCause().getMessage());
                    }
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_down }, EventType.click)));

        add(new ExportModalPanel("exportModalPanel", new Model<BratAnnotatorModel>(bModel)));

        gotoPageTextField = (NumberTextField<Integer>) new NumberTextField<Integer>("gotoPageText",
                new Model<Integer>(0));
        Form<Void> gotoPageTextFieldForm = new Form<Void>("gotoPageTextFieldForm");
        gotoPageTextFieldForm.add(new AjaxFormSubmitBehavior(gotoPageTextFieldForm, "onsubmit")
        {
            private static final long serialVersionUID = -4549805321484461545L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                if (gotoPageAddress == 0) {
                    aTarget.appendJavaScript("alert('The sentence number entered is not valid')");
                    return;
                }
                JCas mergeJCas = null;
                try {
                    aTarget.add(getFeedbackPanel());
                    mergeJCas = repository.readCurationCas(bModel.getDocument());
                    if (bModel.getSentenceAddress() != gotoPageAddress) {

                        ubdateSentenceNumber(mergeJCas, gotoPageAddress);

                        aTarget.add(numberOfPages);
                        updatePanel(curationContainer, aTarget);
                    }
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCauseMessage(e));
                }
                catch (ClassNotFoundException e) {
                    error(e.getMessage());
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
            }
        });

        gotoPageTextField.setType(Integer.class);
        gotoPageTextField.setMinimum(1);
        gotoPageTextField.setDefaultModelObject(1);
        add(gotoPageTextFieldForm.add(gotoPageTextField));
        gotoPageTextField.add(new AjaxFormComponentUpdatingBehavior("onchange")
        {
            private static final long serialVersionUID = 1244526899787707931L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                JCas mergeJCas = null;
                try {
                    aTarget.add(getFeedbackPanel());
                    mergeJCas = repository.readCurationCas(bModel.getDocument());
                    gotoPageAddress = getSentenceAddress(mergeJCas,
                            gotoPageTextField.getModelObject());

                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(e.getMessage());
                }
                catch (IOException e) {
                    error(e.getMessage());
                }

            }
        });

        add(new AjaxLink<Void>("gotoPageLink")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {

                if (gotoPageAddress == 0) {
                    aTarget.appendJavaScript("alert('The sentence number entered is not valid')");
                    return;
                }
                if (bModel.getDocument() == null) {
                    aTarget.appendJavaScript("alert('Please open a document first!')");
                    return;
                }

                JCas mergeJCas = null;
                try {
                    aTarget.add(getFeedbackPanel());
                    mergeJCas = repository.readCurationCas(bModel.getDocument());
                    if (bModel.getSentenceAddress() != gotoPageAddress) {

                        ubdateSentenceNumber(mergeJCas, gotoPageAddress);

                        aTarget.add(numberOfPages);
                        curationPanel.updatePanel(aTarget, curationContainer);
                    }
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCauseMessage(e));
                }
                catch (ClassNotFoundException e) {
                    error(e.getMessage());
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                catch (BratAnnotationException e) {
                    error(e.getMessage());
                }
            }
        });

        finish = new WebMarkupContainer("finishImage");
        finish.setOutputMarkupId(true);
        finish.add(new AttributeModifier("src", true, new LoadableDetachableModel<String>()
        {
            private static final long serialVersionUID = 1562727305401900776L;

            @Override
            protected String load()
            {

                if (bModel.getProject() != null && bModel.getDocument() != null) {
                    if (repository
                            .getSourceDocument(bModel.getDocument().getProject(),
                                    bModel.getDocument().getName()).getState()
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

        final ModalWindow finishCurationModal;
        add(finishCurationModal = new ModalWindow("finishCurationModal"));
        finishCurationModal.setOutputMarkupId(true);

        finishCurationModal.setInitialWidth(700);
        finishCurationModal.setInitialHeight(50);
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
                if (bModel.getDocument() != null
                        && bModel.getDocument().getState()
                                .equals(SourceDocumentState.CURATION_FINISHED)) {
                    finishCurationModal
                            .setTitle("Curation was finished. Are you sure you want to re-open document for curation?");
                }
                else {
                    finishCurationModal.setTitle("Are you sure you want to finish curating?");
                }
                finishCurationModal.setContent(new YesNoFinishModalPanel(finishCurationModal
                        .getContentId(), bModel, finishCurationModal, Mode.CURATION));
                finishCurationModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        target.add(finish);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                });
                finishCurationModal.show(target);
            }
        });

        showFinishCurationModal.add(finish);

        add(new GuidelineModalPanel("guidelineModalPanel", new Model<BratAnnotatorModel>(bModel)));

        final ModalWindow reCreateMergeCas;
        add(reCreateMergeCas = new ModalWindow("reCreateMergeCasModal"));
        reCreateMergeCas.setOutputMarkupId(true);

        reCreateMergeCas.setInitialWidth(400);
        reCreateMergeCas.setInitialHeight(50);
        reCreateMergeCas.setResizable(true);
        reCreateMergeCas.setWidthUnit("px");
        reCreateMergeCas.setHeightUnit("px");
        reCreateMergeCas
                .setTitle("are you sure? all curation annotations for this document will be lost");

        add(showreCreateMergeCasModal = new AjaxLink<Void>("showreCreateMergeCasModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            protected void onConfigure()
            {
                setEnabled(bModel.getDocument() != null
                        && !bModel.getDocument().getState()
                                .equals(SourceDocumentState.CURATION_FINISHED));
            }

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                reCreateMergeCas.setContent(new ReCreateMergeCASModalPanel(reCreateMergeCas
                        .getContentId(), reCreateMergeCas, reMerge));
                reCreateMergeCas.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {

                    private static final long serialVersionUID = 4816615910398625993L;

                    @Override
                    public void onClose(AjaxRequestTarget aTarget)
                    {
                        if (reMerge.isReMerege()) {
                            try {
                                aTarget.add(getFeedbackPanel());
                                repository.removeCurationDocumentContent(bModel.getDocument(),
                                        bModel.getUser().getUsername());
                                loadDocumentAction(aTarget);

                                aTarget.appendJavaScript("alert('remerege finished!')");
                            }
                            catch (IOException e) {
                                aTarget.add(getFeedbackPanel());
                                error(e.getCause().getMessage());
                            }
                        }
                    }
                });
                reCreateMergeCas.show(target);
            }
        });
        // Show the next page of this document
        add(new AjaxLink<Void>("showNext")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                if (bModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        mergeJCas = repository.readCurationCas(bModel.getDocument());
                        int nextSentenceAddress = getNextPageFirstSentenceAddress(mergeJCas,
                                bModel.getSentenceAddress(), bModel.getPreferences()
                                        .getWindowSize());
                        if (bModel.getSentenceAddress() != nextSentenceAddress) {
                            aTarget.add(getFeedbackPanel());

                            ubdateSentenceNumber(mergeJCas, nextSentenceAddress);

                            aTarget.add(numberOfPages);
                            curationPanel.updatePanel(aTarget, curationContainer);
                            updatePanel(curationContainer, aTarget);
                        }

                        else {
                            aTarget.appendJavaScript("alert('This is last page!')");
                        }
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                    }
                }
                else {
                    aTarget.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_down }, EventType.click)));

        // SHow the previous page of this document
        add(new AjaxLink<Void>("showPrevious")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                if (bModel.getDocument() != null) {

                    JCas mergeJCas = null;
                    try {
                        aTarget.add(getFeedbackPanel());
                        mergeJCas = repository.readCurationCas(bModel.getDocument());
                        int previousSentenceAddress = BratAjaxCasUtil
                                .getPreviousDisplayWindowSentenceBeginAddress(mergeJCas, bModel
                                        .getSentenceAddress(), bModel.getPreferences()
                                        .getWindowSize());
                        if (bModel.getSentenceAddress() != previousSentenceAddress) {

                            ubdateSentenceNumber(mergeJCas, previousSentenceAddress);

                            aTarget.add(numberOfPages);
                            curationPanel.updatePanel(aTarget, curationContainer);
                            updatePanel(curationContainer, aTarget);
                        }
                        else {
                            aTarget.appendJavaScript("alert('This is First Page!')");
                        }
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                    }
                }
                else {
                    aTarget.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_up }, EventType.click)));

        add(new AjaxLink<Void>("showFirst")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                if (bModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        aTarget.add(getFeedbackPanel());
                        mergeJCas = repository.readCurationCas(bModel.getDocument());

                        int address = getAddr(selectSentenceAt(mergeJCas,
                                bModel.getSentenceBeginOffset(), bModel.getSentenceEndOffset()));
                        int firstAddress = getFirstSentenceAddress(mergeJCas);

                        if (firstAddress != address) {

                            ubdateSentenceNumber(mergeJCas, firstAddress);

                            aTarget.add(numberOfPages);
                            curationPanel.updatePanel(aTarget, curationContainer);
                            updatePanel(curationContainer, aTarget);
                        }
                        else {
                            aTarget.appendJavaScript("alert('This is first page!')");
                        }
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                    }
                }
                else {
                    aTarget.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Home }, EventType.click)));

        add(new AjaxLink<Void>("showLast")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                if (bModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        aTarget.add(getFeedbackPanel());
                        mergeJCas = repository.readCurationCas(bModel.getDocument());
                        int lastDisplayWindowBeginingSentenceAddress = BratAjaxCasUtil
                                .getLastDisplayWindowFirstSentenceAddress(mergeJCas, bModel
                                        .getPreferences().getWindowSize());
                        if (lastDisplayWindowBeginingSentenceAddress != bModel.getSentenceAddress()) {

                            ubdateSentenceNumber(mergeJCas,
                                    lastDisplayWindowBeginingSentenceAddress);

                            aTarget.add(numberOfPages);
                            curationPanel.updatePanel(aTarget, curationContainer);
                            updatePanel(curationContainer, aTarget);
                        }
                        else {
                            aTarget.appendJavaScript("alert('This is last Page!')");
                        }
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                    }
                }
                else {
                    aTarget.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.End }, EventType.click)));
    }

    // Update the curation panel.

    private void updatePanel(CurationContainer aCurationContainer, AjaxRequestTarget aTarget)
    {
        JCas mergeJCas = null;
        try {
            mergeJCas = repository.readCurationCas(bModel.getDocument());
        }
        catch (UIMAException e) {
            error(e.getMessage());
        }
        catch (ClassNotFoundException e) {
            error(e.getMessage());
        }
        catch (IOException e) {
            error(e.getMessage());
        }
        gotoPageTextField.setModelObject(getFirstSentenceNumber(mergeJCas,
                bModel.getSentenceAddress()) + 1);
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

    private void ubdateSentenceNumber(JCas aJCas, int aAddress)
    {
        bModel.setSentenceAddress(aAddress);
        Sentence sentence = selectByAddr(aJCas, Sentence.class, aAddress);
        bModel.setSentenceBeginOffset(sentence.getBegin());
        bModel.setSentenceEndOffset(sentence.getEnd());
        bModel.setSentenceNumber(BratAjaxCasUtil.getSentenceNumber(aJCas, sentence.getBegin()));

        Sentence firstSentence = selectSentenceAt(aJCas, bModel.getSentenceBeginOffset(),
                bModel.getSentenceEndOffset());
        int lastAddressInPage = getLastSentenceAddressInDisplayWindow(aJCas,
                getAddr(firstSentence), bModel.getPreferences().getWindowSize());
        // the last sentence address in the display window
        Sentence lastSentenceInPage = (Sentence) selectByAddr(aJCas, FeatureStructure.class,
                lastAddressInPage);
        bModel.setFSN(BratAjaxCasUtil.getSentenceNumber(aJCas, firstSentence.getBegin()));
        bModel.setLSN(BratAjaxCasUtil.getSentenceNumber(aJCas, lastSentenceInPage.getBegin()));
    }

    private void loadDocumentAction(AjaxRequestTarget aTarget)
    {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User userLoggedIn = userRepository.get(SecurityContextHolder.getContext()
                    .getAuthentication().getName());
            bModel.setUser(userLoggedIn);

            List<AnnotationDocument> finishedAnnotationDocuments = new ArrayList<AnnotationDocument>();

            for (AnnotationDocument annotationDocument : repository.listAnnotationDocuments(bModel
                    .getDocument())) {
                if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                    finishedAnnotationDocuments.add(annotationDocument);
                }
            }

            SuggestionBuilder cb = new SuggestionBuilder(repository, annotationService,
                    userRepository);
            AnnotationDocument randomAnnotationDocument = null;
            if (finishedAnnotationDocuments.size() > 0) {
                randomAnnotationDocument = finishedAnnotationDocuments.get(0);
            }

            // upgrade CASes for each user, what if new type is added once the user finished
            // annotation
            for (AnnotationDocument ad : finishedAnnotationDocuments) {
                repository.upgradeCasAndSave(ad.getDocument(), Mode.CURATION, ad.getUser());
            }
            Map<String, JCas> jCases = cb.listJcasesforCuration(finishedAnnotationDocuments,
                    randomAnnotationDocument, Mode.CURATION);
            JCas mergeJCas = cb.getMergeCas(bModel, bModel.getDocument(), jCases,
                    randomAnnotationDocument);

            // (Re)initialize brat model after potential creating / upgrading CAS
            bModel.initForDocument(mergeJCas);

            // Load user preferences
            PreferencesUtil.setAnnotationPreference(username, repository, annotationService,
                    bModel, Mode.CURATION);
            bModel.getPreferences().setCurationWindowSize(
                    BratAjaxCasUtil.getSentenceSize(mergeJCas));
            // if project is changed, reset some project specific settings
            if (currentprojectId != bModel.getProject().getId()) {
                bModel.initForProject();
            }

            currentprojectId = bModel.getProject().getId();

            SuggestionBuilder builder = new SuggestionBuilder(repository, annotationService,
                    userRepository);
            curationContainer = builder.buildCurationContainer(bModel);
            curationContainer.setBratAnnotatorModel(bModel);
            curationPanel.updatePanel(aTarget, curationContainer);
            updatePanel(curationContainer, aTarget);

        }
        catch (Exception e) {
            aTarget.add(getFeedbackPanel());
            error(ExceptionUtils.getRootCause(e));
        }

        aTarget.add(finish);
        aTarget.add(numberOfPages);
        aTarget.add(documentNamePanel);
        aTarget.add(showreCreateMergeCasModal);
    }
}
