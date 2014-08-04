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

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormValidatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.springframework.beans.BeansException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.context.SecurityContextHolder;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationBuilder;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.ProjectUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.project.page.SettingsPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.ReCreateMergeCASModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.ReMergeCasModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.YesNoFinishModalPanel;
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
    extends SettingsPageBase
{
    private static final long serialVersionUID = 1378872465851908515L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private CurationPanel curationPanel;

    private ReMergeCasModel reMerge;

    private CurationContainer curationContainer;
    private BratAnnotatorModel bratAnnotatorModel;

    public Label numberOfPages;
    private DocumentNamePanel documentNamePanel;

    private int sentenceNumber = 1;
    private int totalNumberOfSentence;

    private long currentDocumentId;
    private long currentprojectId;

    // Open the dialog window on first load
    boolean firstLoad = true;

    private NumberTextField<Integer> gotoPageTextField;
    private int gotoPageAddress;

    WebMarkupContainer finish;

    @SuppressWarnings("deprecation")
    public CurationPage()
    {
        bratAnnotatorModel = new BratAnnotatorModel();
        bratAnnotatorModel.setMode(Mode.CURATION);
        reMerge = new ReMergeCasModel();

        curationContainer = new CurationContainer();
        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
        curationPanel = new CurationPanel("curationPanel", curationContainer);
        curationPanel.setOutputMarkupId(true);
        add(curationPanel);

        add(documentNamePanel = new DocumentNamePanel("documentNamePanel",
                new Model<BratAnnotatorModel>(bratAnnotatorModel)));

        add(numberOfPages = (Label) new Label("numberOfPages",
                new LoadableDetachableModel<String>()
                {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String load()
                    {
                        if (bratAnnotatorModel.getDocument() != null) {

                            JCas mergeJCas = null;
                            try {
                                mergeJCas = repository
                                        .getCurationDocumentContent(bratAnnotatorModel
                                                .getDocument());

                                totalNumberOfSentence = BratAjaxCasUtil.getNumberOfPages(mergeJCas);

                                // If only one page, start displaying from
                                // sentence 1
                                if (totalNumberOfSentence == 1) {
                                    bratAnnotatorModel.setSentenceAddress(bratAnnotatorModel
                                            .getFirstSentenceAddress());
                                }
                                sentenceNumber = BratAjaxCasUtil.getFirstSentenceNumber(mergeJCas,
                                        bratAnnotatorModel.getSentenceAddress());
                                int firstSentenceNumber = sentenceNumber + 1;
                                int lastSentenceNumber;
                                if (firstSentenceNumber + bratAnnotatorModel.getWindowSize() - 1 < totalNumberOfSentence) {
                                    lastSentenceNumber = firstSentenceNumber
                                            + bratAnnotatorModel.getWindowSize() - 1;
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
                        .getContentId(), bratAnnotatorModel, openDocumentsModal, Mode.CURATION));
                openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();

                        if (bratAnnotatorModel.getDocument() == null) {
                            setResponsePage(WelcomePage.class);
                            return;
                        }

                        User user = repository.getUser(username);
                        // Update source document state to
                        // CURRATION_INPROGRESS, if it was not
                        // ANNOTATION_FINISHED
                        if (!bratAnnotatorModel.getDocument().getState()
                                .equals(SourceDocumentState.CURATION_FINISHED)) {

                            bratAnnotatorModel.getDocument().setState(
                                    SourceDocumentState.CURATION_IN_PROGRESS);
                        }
                        try {
                            repository.createSourceDocument(bratAnnotatorModel.getDocument(), user);
                            repository.upgradeCasAndSave(bratAnnotatorModel.getDocument(),
                                    Mode.CURATION, username);

                            loadDocumentAction();
                            CurationBuilder builder = new CurationBuilder(repository);
                            curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                            curationContainer.setBratAnnotatorModel(bratAnnotatorModel);

                            updatePanel(curationContainer, target);
                            target.add(numberOfPages);
                            target.add(finish.setOutputMarkupId(true));

                            target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

                        }
                        catch (UIMAException e) {
                            target.add(getFeedbackPanel());
                            error(ExceptionUtils.getRootCause(e));
                        }
                        catch (ClassNotFoundException e) {
                            target.add(getFeedbackPanel());
                            error(e.getMessage());
                        }
                        catch (DataRetrievalFailureException e) {
                            target.add(getFeedbackPanel());
                            error(e.getCause().getMessage());
                        }
                        catch (IOException e) {
                            target.add(getFeedbackPanel());
                            error(e.getMessage());
                        }
                        catch (BratAnnotationException e) {
                            target.add(getFeedbackPanel());
                            error(e.getMessage());
                        }

                        target.add(documentNamePanel.setOutputMarkupId(true));
                    }
                });
                openDocumentsModal.show(aTarget);
            }
        });

        add(new AnnotationLayersModalPanel("annotationLayersModalPanel",
                new Model<BratAnnotatorModel>(bratAnnotatorModel))
        {
            private static final long serialVersionUID = -4657965743173979437L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                CurationBuilder builder = new CurationBuilder(repository);
                try {

                    aTarget.add(getFeedbackPanel());
                    curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                    curationContainer.setBratAnnotatorModel(bratAnnotatorModel);

                    updatePanel(curationContainer, aTarget);
                    aTarget.add(numberOfPages);
                    aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

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
                catch (BratAnnotationException e) {
                    error(e.getMessage());
                }
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
                List<SourceDocument> listOfSourceDocuements = repository
                        .listSourceDocuments(bratAnnotatorModel.getProject());

                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);

                List<SourceDocument> sourceDocumentsinIgnorState = new ArrayList<SourceDocument>();
                for (SourceDocument sourceDocument : listOfSourceDocuements) {
                    if (!ProjectUtil.existFinishedDocument(sourceDocument, user, repository,
                            bratAnnotatorModel.getProject())) {
                        sourceDocumentsinIgnorState.add(sourceDocument);
                    }
                }

                listOfSourceDocuements.removeAll(sourceDocumentsinIgnorState);

                // Index of the current source document in the list
                int currentDocumentIndex = listOfSourceDocuements.indexOf(bratAnnotatorModel
                        .getDocument());

                // If the first the document
                if (currentDocumentIndex == 0) {
                    aTarget.appendJavaScript("alert('This is the first document!')");
                }
                else {
                    bratAnnotatorModel.setDocumentName(listOfSourceDocuements.get(
                            currentDocumentIndex - 1).getName());
                    bratAnnotatorModel.setDocument(listOfSourceDocuements
                            .get(currentDocumentIndex - 1));
                    try {
                        aTarget.add(getFeedbackPanel());
                        repository.upgradeCasAndSave(bratAnnotatorModel.getDocument(),
                                Mode.CURATION, bratAnnotatorModel.getUser().getUsername());
                        loadDocumentAction();
                        CurationBuilder builder = new CurationBuilder(repository);
                        curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);

                        updatePanel(curationContainer, aTarget);
                        aTarget.add(numberOfPages);
                        aTarget.add(finish.setOutputMarkupId(true));
                        aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (DataRetrievalFailureException e) {
                        error(e.getCause().getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
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
                List<SourceDocument> listOfSourceDocuements = repository
                        .listSourceDocuments(bratAnnotatorModel.getProject());

                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);

                List<SourceDocument> sourceDocumentsinIgnorState = new ArrayList<SourceDocument>();
                for (SourceDocument sourceDocument : listOfSourceDocuements) {
                    if (!ProjectUtil.existFinishedDocument(sourceDocument, user, repository,
                            bratAnnotatorModel.getProject())) {
                        sourceDocumentsinIgnorState.add(sourceDocument);
                    }
                }

                listOfSourceDocuements.removeAll(sourceDocumentsinIgnorState);

                // Index of the current source document in the list
                int currentDocumentIndex = listOfSourceDocuements.indexOf(bratAnnotatorModel
                        .getDocument());

                // If the first document
                if (currentDocumentIndex == listOfSourceDocuements.size() - 1) {
                    aTarget.appendJavaScript("alert('This is the last document!')");
                }
                else {
                    bratAnnotatorModel.setDocumentName(listOfSourceDocuements.get(
                            currentDocumentIndex + 1).getName());
                    bratAnnotatorModel.setDocument(listOfSourceDocuements
                            .get(currentDocumentIndex + 1));

                    try {
                        aTarget.add(getFeedbackPanel());
                        repository.upgradeCasAndSave(bratAnnotatorModel.getDocument(),
                                Mode.CURATION, bratAnnotatorModel.getUser().getUsername());
                        loadDocumentAction();
                        CurationBuilder builder = new CurationBuilder(repository);
                        curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);

                        updatePanel(curationContainer, aTarget);
                        aTarget.add(numberOfPages);
                        aTarget.add(finish.setOutputMarkupId(true));
                        aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (DataRetrievalFailureException e) {
                        error(e.getCause().getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                    }

                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_down }, EventType.click)));

        add(new ExportModalPanel("exportModalPanel", new Model<BratAnnotatorModel>(
                bratAnnotatorModel)));

        gotoPageTextField = (NumberTextField<Integer>) new NumberTextField<Integer>("gotoPageText",
                new Model<Integer>(0));
        Form<Void> gotoPageTextFieldForm = new Form<Void>("gotoPageTextFieldForm");
        gotoPageTextFieldForm.add(new AjaxFormValidatingBehavior(gotoPageTextFieldForm, "onsubmit")
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
                    mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                            .getDocument());
                    if (bratAnnotatorModel.getSentenceAddress() != gotoPageAddress) {
                        bratAnnotatorModel.setSentenceAddress(gotoPageAddress);

                        Sentence sentence = selectByAddr(mergeJCas, Sentence.class, gotoPageAddress);
                        bratAnnotatorModel.setSentenceBeginOffset(sentence.getBegin());
                        bratAnnotatorModel.setSentenceEndOffset(sentence.getEnd());

                        CurationBuilder builder = new CurationBuilder(repository);
                        curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);

                        updatePanel(curationContainer, aTarget);
                        aTarget.add(numberOfPages);
                        aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
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

            @Override
            protected CharSequence getEventHandler()
            {
                AppendingStringBuffer handler = new AppendingStringBuffer();
                handler.append(super.getEventHandler());
                handler.append("; return false;");
                return handler;
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
                    mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                            .getDocument());
                    gotoPageAddress = BratAjaxCasUtil.getSentenceAddress(mergeJCas,
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
                if (bratAnnotatorModel.getDocument() == null) {
                    aTarget.appendJavaScript("alert('Please open a document first!')");
                    return;
                }

                JCas mergeJCas = null;
                try {
                    aTarget.add(getFeedbackPanel());
                    mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                            .getDocument());
                    if (bratAnnotatorModel.getSentenceAddress() != gotoPageAddress) {
                        bratAnnotatorModel.setSentenceAddress(gotoPageAddress);

                        Sentence sentence = selectByAddr(mergeJCas, Sentence.class, gotoPageAddress);
                        bratAnnotatorModel.setSentenceBeginOffset(sentence.getBegin());
                        bratAnnotatorModel.setSentenceEndOffset(sentence.getEnd());

                        CurationBuilder builder = new CurationBuilder(repository);
                        curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);

                        updatePanel(curationContainer, aTarget);
                        aTarget.add(numberOfPages);
                        aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
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
        finish.add(new AttributeModifier("src", true, new LoadableDetachableModel<String>()
        {
            private static final long serialVersionUID = 1562727305401900776L;

            @Override
            protected String load()
            {

                if (bratAnnotatorModel.getProject() != null
                        && bratAnnotatorModel.getDocument() != null) {
                    if (repository
                            .getSourceDocument(bratAnnotatorModel.getDocument().getProject(),
                                    bratAnnotatorModel.getDocument().getName()).getState()
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
                if (bratAnnotatorModel.getDocument() != null
                        && bratAnnotatorModel.getDocument().getState()
                                .equals(SourceDocumentState.CURATION_FINISHED)) {
                    finishCurationModal
                            .setTitle("Curation was finished. Are you sure you want to re-open document for curation?");
                }
                else {
                    finishCurationModal.setTitle("Are you sure you want to finish curating?");
                }
                finishCurationModal.setContent(new YesNoFinishModalPanel(finishCurationModal
                        .getContentId(), bratAnnotatorModel, finishCurationModal, Mode.CURATION));
                finishCurationModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        target.add(finish.setOutputMarkupId(true));
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                });
                finishCurationModal.show(target);
            }
        });

        showFinishCurationModal.add(finish);

        add(new GuidelineModalPanel("guidelineModalPanel", new Model<BratAnnotatorModel>(
                bratAnnotatorModel)));

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

        add(new AjaxLink<Void>("showreCreateMergeCasModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

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
                                repository.removeCurationDocumentContent(bratAnnotatorModel
                                        .getDocument(), bratAnnotatorModel.getUser().getUsername());
                                loadDocumentAction();
                                CurationBuilder builder = new CurationBuilder(repository);
                                curationContainer = builder
                                        .buildCurationContainer(bratAnnotatorModel);
                                curationContainer.setBratAnnotatorModel(bratAnnotatorModel);

                                updatePanel(curationContainer, aTarget);
                                aTarget.add(numberOfPages);
                                aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                                aTarget.appendJavaScript("alert('remerege finished!')");
                            }
                            catch (UIMAException e) {
                                error(ExceptionUtils.getRootCauseMessage(e));
                            }
                            catch (ClassNotFoundException e) {
                                error(e.getMessage());
                            }
                            catch (DataRetrievalFailureException e) {
                                error(e.getCause().getMessage());
                            }
                            catch (IOException e) {
                                error(e.getMessage());
                            }
                            catch (BratAnnotationException e) {
                                error(e.getMessage());
                            }
                        }
                    }
                });
                reCreateMergeCas.show(target);
            }

            @Override
            public boolean isEnabled()
            {
                return bratAnnotatorModel.getDocument() != null
                        && !bratAnnotatorModel.getDocument().getState()
                                .equals(SourceDocumentState.CURATION_FINISHED);
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
                if (bratAnnotatorModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                                .getDocument());
                        int nextSentenceAddress = BratAjaxCasUtil
                                .getNextDisplayWindowSentenceBeginAddress(mergeJCas,
                                        bratAnnotatorModel.getSentenceAddress(),
                                        bratAnnotatorModel.getWindowSize());
                        if (bratAnnotatorModel.getSentenceAddress() != nextSentenceAddress) {
                            aTarget.add(getFeedbackPanel());
                            bratAnnotatorModel.setSentenceAddress(nextSentenceAddress);

                            Sentence sentence = selectByAddr(mergeJCas, Sentence.class,
                                    nextSentenceAddress);
                            bratAnnotatorModel.setSentenceBeginOffset(sentence.getBegin());
                            bratAnnotatorModel.setSentenceEndOffset(sentence.getEnd());

                            CurationBuilder builder = new CurationBuilder(repository);

                            curationContainer = builder.buildCurationContainer(bratAnnotatorModel);

                            curationContainer.setBratAnnotatorModel(bratAnnotatorModel);

                            updatePanel(curationContainer, aTarget);
                            aTarget.add(numberOfPages);
                            aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
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
                if (bratAnnotatorModel.getDocument() != null) {

                    JCas mergeJCas = null;
                    try {
                        aTarget.add(getFeedbackPanel());
                        mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                                .getDocument());
                        int previousSentenceAddress = BratAjaxCasUtil
                                .getPreviousDisplayWindowSentenceBeginAddress(mergeJCas,
                                        bratAnnotatorModel.getSentenceAddress(),
                                        bratAnnotatorModel.getWindowSize());
                        if (bratAnnotatorModel.getSentenceAddress() != previousSentenceAddress) {
                            bratAnnotatorModel.setSentenceAddress(previousSentenceAddress);

                            Sentence sentence = selectByAddr(mergeJCas, Sentence.class,
                                    previousSentenceAddress);
                            bratAnnotatorModel.setSentenceBeginOffset(sentence.getBegin());
                            bratAnnotatorModel.setSentenceEndOffset(sentence.getEnd());

                            CurationBuilder builder = new CurationBuilder(repository);
                            curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                            curationContainer.setBratAnnotatorModel(bratAnnotatorModel);

                            updatePanel(curationContainer, aTarget);
                            aTarget.add(numberOfPages);
                            aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
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
                if (bratAnnotatorModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        aTarget.add(getFeedbackPanel());
                        mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                                .getDocument());

                        int address = BratAjaxCasUtil.selectSentenceAt(mergeJCas,
                                bratAnnotatorModel.getSentenceBeginOffset(),
                                bratAnnotatorModel.getSentenceEndOffset()).getAddress();
                        int firstAddress = BratAjaxCasUtil.getFirstSentenceAddress(mergeJCas);

                        if (firstAddress != address) {
                            bratAnnotatorModel.setSentenceAddress(firstAddress);

                            Sentence sentence = selectByAddr(mergeJCas, Sentence.class,
                                    firstAddress);
                            bratAnnotatorModel.setSentenceBeginOffset(sentence.getBegin());
                            bratAnnotatorModel.setSentenceEndOffset(sentence.getEnd());

                            CurationBuilder builder = new CurationBuilder(repository);
                            curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                            curationContainer.setBratAnnotatorModel(bratAnnotatorModel);

                            updatePanel(curationContainer, aTarget);
                            aTarget.add(numberOfPages);
                            aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

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
                if (bratAnnotatorModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        aTarget.add(getFeedbackPanel());
                        mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                                .getDocument());
                        int lastDisplayWindowBeginingSentenceAddress = BratAjaxCasUtil
                                .getLastDisplayWindowFirstSentenceAddress(mergeJCas,
                                        bratAnnotatorModel.getWindowSize());
                        if (lastDisplayWindowBeginingSentenceAddress != bratAnnotatorModel
                                .getSentenceAddress()) {
                            bratAnnotatorModel
                                    .setSentenceAddress(lastDisplayWindowBeginingSentenceAddress);

                            Sentence sentence = selectByAddr(mergeJCas, Sentence.class,
                                    lastDisplayWindowBeginingSentenceAddress);
                            bratAnnotatorModel.setSentenceBeginOffset(sentence.getBegin());
                            bratAnnotatorModel.setSentenceEndOffset(sentence.getEnd());

                            CurationBuilder builder = new CurationBuilder(repository);
                            curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                            curationContainer.setBratAnnotatorModel(bratAnnotatorModel);

                            updatePanel(curationContainer, aTarget);
                            aTarget.add(numberOfPages);
                            aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

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
            mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel.getDocument());
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
        // remove old panel, create new one, add it
        remove(curationPanel);
        curationPanel = new CurationPanel("curationPanel", aCurationContainer)
        {
            private static final long serialVersionUID = 2175915644696513166L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                JCas mergeJCas = null;
                try {
                    mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                            .getDocument());
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
                gotoPageTextField.setModelObject(BratAjaxCasUtil.getFirstSentenceNumber(mergeJCas,
                        bratAnnotatorModel.getSentenceAddress()) + 1);
                gotoPageAddress = BratAjaxCasUtil.getSentenceAddress(mergeJCas,
                        gotoPageTextField.getModelObject());
                aTarget.add(gotoPageTextField);
            }
        };

        gotoPageTextField.setModelObject(BratAjaxCasUtil.getFirstSentenceNumber(mergeJCas,
                bratAnnotatorModel.getSentenceAddress()) + 1);
        gotoPageAddress = BratAjaxCasUtil.getSentenceAddress(mergeJCas,
                gotoPageTextField.getModelObject());
        curationPanel.setOutputMarkupId(true);
        aTarget.add(gotoPageTextField);
        add(curationPanel);
    }

    /**
     * for the first time, open the <b>open document dialog</b>
     */
    @Override
    public void renderHead(IHeaderResponse response)
    {
        String jQueryString = "";
        if (firstLoad) {
            jQueryString += "jQuery('#showOpenDocumentModal').trigger('click');";
            firstLoad = false;
        }
        response.renderOnLoadJavaScript(jQueryString);
    }

    private void loadDocumentAction()
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        List<AnnotationDocument> finishedAnnotationDocuments = new ArrayList<AnnotationDocument>();

        for (AnnotationDocument annotationDocument : repository
                .listAnnotationDocuments(bratAnnotatorModel.getDocument())) {
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                finishedAnnotationDocuments.add(annotationDocument);
            }
        }
        CurationBuilder cb = new CurationBuilder(repository);
        AnnotationDocument randomAnnotationDocument = null;
        if (finishedAnnotationDocuments.size() > 0) {
            randomAnnotationDocument = finishedAnnotationDocuments.get(0);
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User userLoggedIn = repository.getUser(SecurityContextHolder.getContext()
                .getAuthentication().getName());
        bratAnnotatorModel.setUser(userLoggedIn);
        ProjectUtil.setAnnotationPreference(username, repository, annotationService,
                bratAnnotatorModel, Mode.CURATION);
        Map<String, JCas> jCases = cb.listJcasesforCuration(finishedAnnotationDocuments,
                randomAnnotationDocument, Mode.CURATION);
        JCas mergeJCas = cb.getMergeCas(bratAnnotatorModel, bratAnnotatorModel.getDocument(),
                jCases, randomAnnotationDocument);

        if (bratAnnotatorModel.getSentenceAddress() == -1
                || bratAnnotatorModel.getDocument().getId() != currentDocumentId
                || bratAnnotatorModel.getProject().getId() != currentprojectId) {

            try {
                bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil
                        .getFirstSentenceAddress(mergeJCas));
                bratAnnotatorModel.setLastSentenceAddress(BratAjaxCasUtil
                        .getLastSentenceAddress(mergeJCas));
                bratAnnotatorModel.setFirstSentenceAddress(bratAnnotatorModel.getSentenceAddress());

                Sentence sentence = selectByAddr(mergeJCas, Sentence.class,
                        bratAnnotatorModel.getSentenceAddress());
                bratAnnotatorModel.setSentenceBeginOffset(sentence.getBegin());
                bratAnnotatorModel.setSentenceEndOffset(sentence.getEnd());

            }
            catch (DataRetrievalFailureException ex) {
                throw ex;
            }
            catch (BeansException e) {
                throw e;
            }
        }
        // if project is changed, reset some project specific settings
        if (currentprojectId != bratAnnotatorModel.getProject().getId()) {
            bratAnnotatorModel.setRememberedArcFeatures(null);
            bratAnnotatorModel.setRememberedArcLayer(null);
            bratAnnotatorModel.setRememberedSpanFeatures(null);
            bratAnnotatorModel.setRememberedSpanLayer(null);
            bratAnnotatorModel.setMessage(null);
        }

        currentprojectId = bratAnnotatorModel.getProject().getId();
        currentDocumentId = bratAnnotatorModel.getDocument().getId();
    }
}
