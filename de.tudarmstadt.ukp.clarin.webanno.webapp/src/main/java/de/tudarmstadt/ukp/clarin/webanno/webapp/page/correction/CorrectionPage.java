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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.correction;

import static org.uimafit.util.JCasUtil.select;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.beans.BeansException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenDocumentModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.AnnotationLayersModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.ExportModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.FinishImage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.FinishLink;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.BratCuratorUtility;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.CurationSegmentPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationBuilder;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationSegmentForSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.project.SettingsPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome.WelcomePage;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * This is the main class for the correction page. Displays in the lower panel the Automatically
 * annotated document and in the upper panel the corrected annotation
 *
 * @author Seid Muhie Yimam
 */
public class CorrectionPage
    extends SettingsPageBase
{
    private static final long serialVersionUID = 1378872465851908515L;

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;
    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private OpenDocumentModel openDataModel;

    private CurationContainer curationContainer;
    private BratAnnotatorModel bratAnnotatorModel;

    private Label numberOfPages;
    private Label documentNameLabel;

    private int sentenceNumber = 1;
    private int totalNumberOfSentence;

    private long currentDocumentId;
    private long currentprojectId;

    // Open the dialog window on first load
    boolean firstLoad = true;

    private NumberTextField<Integer> gotoPageTextField;
    private int gotoPageAddress = -1;

    private FinishImage finish;

    private CurationSegmentPanel sentenceOuterView;
    private BratAnnotator mergeVisualizer;

    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress = new HashMap<String, Map<Integer, AnnotationSelection>>();

    private CurationSegmentForSourceDocument curationSegment = new CurationSegmentForSourceDocument();

    @SuppressWarnings("deprecation")
    public CorrectionPage()
    {
        openDataModel = new OpenDocumentModel();
        bratAnnotatorModel = new BratAnnotatorModel();
        bratAnnotatorModel.setMode(Mode.CORRECTION);

        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();
        CurationUserSegmentForAnnotationDocument curationUserSegmentForAnnotationDocument = new CurationUserSegmentForAnnotationDocument();
        if (bratAnnotatorModel.getDocument() != null) {
            curationUserSegmentForAnnotationDocument
                    .setAnnotationSelectionByUsernameAndAddress(annotationSelectionByUsernameAndAddress);
            curationUserSegmentForAnnotationDocument.setBratAnnotatorModel(bratAnnotatorModel);
            sentences.add(curationUserSegmentForAnnotationDocument);
        }
        sentenceOuterView = new CurationSegmentPanel("sentenceOuterView",
                new Model<LinkedList<CurationUserSegmentForAnnotationDocument>>(sentences))
        {
            private static final long serialVersionUID = 2583509126979792202L;

            @Override
            public void onChange(AjaxRequestTarget aTarget)
            {
                try {
                    BratCuratorUtility.updatePanel(aTarget, this, curationContainer,
                            mergeVisualizer, repository, annotationSelectionByUsernameAndAddress,
                            curationSegment, annotationService, jsonConverter);
                }
                catch (UIMAException e) {
                    ExceptionUtils.getRootCause(e);
                }
                catch (ClassNotFoundException e) {
                    e.getMessage();
                }
                catch (IOException e) {
                    e.getMessage();
                }
            }
        };

        sentenceOuterView.setOutputMarkupId(true);
        add(sentenceOuterView);
        mergeVisualizer = new BratAnnotator("mergeView", new Model<BratAnnotatorModel>(
                bratAnnotatorModel))
        {

            private static final long serialVersionUID = 7279648231521710155L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                aTarget.add(sentenceOuterView);
                try {
                    BratCuratorUtility.updatePanel(aTarget, sentenceOuterView, curationContainer,
                            this, repository, annotationSelectionByUsernameAndAddress,
                            curationSegment, annotationService, jsonConverter);
                }
                catch (UIMAException e) {
                    ExceptionUtils.getRootCause(e);
                }
                catch (ClassNotFoundException e) {
                    e.getMessage();
                }
                catch (IOException e) {
                    e.getMessage();
                }
            }
        };
        // reset sentenceAddress and lastSentenceAddress to the orginal once

        mergeVisualizer.setOutputMarkupId(true);
        add(mergeVisualizer);

        curationContainer = new CurationContainer();
        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);

        add(documentNameLabel = (Label) new Label("doumentName", new LoadableDetachableModel()
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected String load()
            {
                String projectName;
                String documentName;
                if (bratAnnotatorModel.getProject() == null) {
                    projectName = "/";
                }
                else {
                    projectName = bratAnnotatorModel.getProject().getName() + "/";
                }
                if (bratAnnotatorModel.getDocument() == null) {
                    documentName = "";
                }
                else {
                    documentName = bratAnnotatorModel.getDocument().getName();
                }
                return projectName + documentName;

            }
        }).setOutputMarkupId(true));

        add(numberOfPages = (Label) new Label("numberOfPages", new LoadableDetachableModel()
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected String load()
            {
                if (bratAnnotatorModel.getDocument() != null) {

                    JCas mergeJCas = null;
                    try {
                        mergeJCas = repository.getCorrectionDocumentContent(bratAnnotatorModel
                                .getDocument());

                        totalNumberOfSentence = BratAjaxCasUtil.getNumberOfPages(mergeJCas,
                                bratAnnotatorModel.getWindowSize());

                        // If only one page, start displaying from sentence 1
                        if (totalNumberOfSentence == 1) {
                            bratAnnotatorModel.setSentenceAddress(bratAnnotatorModel
                                    .getFirstSentenceAddress());
                        }
                        sentenceNumber = BratAjaxCasUtil.getSentenceNumber(mergeJCas,
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

                        return "showing " + firstSentenceNumber + "-" + lastSentenceNumber + " of "
                                + totalNumberOfSentence + " sentences";
                    }
                    catch (UIMAException e) {
                        return "";
                    }
                    catch (ClassNotFoundException e) {
                        return "";
                    }
                    catch (FileNotFoundException e) {
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
                        .getContentId(), openDataModel, openDocumentsModal, Mode.CORRECTION));
                openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();

                        User user = repository.getUser(username);
                        // If this source document has at least one annotation document "FINISHED",
                        // and curation not yet
                        // finished on it
                        if (openDataModel.getDocument() != null) {

                            // Get settings from preferences, if available
                            // TEST - set window size to 10

                            try {

                                bratAnnotatorModel.setDocument(openDataModel.getDocument());
                                bratAnnotatorModel.setProject(openDataModel.getProject());

                                init();
                                setCurationSegmentBeginEnd();
                                update(target);

                            }
                            catch (UIMAException e) {
                                error(ExceptionUtils.getRootCause(e));
                            }
                            catch (ClassNotFoundException e) {
                                error(ExceptionUtils.getRootCause(e));
                            }
                            catch (IOException e) {
                                error(ExceptionUtils.getRootCause(e));
                            }

                            finish.setModelObject(bratAnnotatorModel);
                            target.add(finish.setOutputMarkupId(true));
                            target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

                        }
                        else if (openDataModel.getDocument() == null) {
                            setResponsePage(WelcomePage.class);
                        }
                        else {
                            target.appendJavaScript("alert('Annotation in progress for document ["
                                    + openDataModel.getDocument().getName() + "]')");
                        }
                        target.add(documentNameLabel);
                        target.add(numberOfPages);
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
                curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                try {
                    setCurationSegmentBeginEnd();
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
                update(aTarget);
            }
        });

        add(new ExportModalPanel("exportModalPanel", new Model<BratAnnotatorModel>(
                bratAnnotatorModel)));

        gotoPageTextField = (NumberTextField<Integer>) new NumberTextField<Integer>("gotoPageText",
                new Model<Integer>(10));
        gotoPageTextField.setType(Integer.class);
        add(gotoPageTextField);
        gotoPageTextField.add(new AjaxFormComponentUpdatingBehavior("onchange")
        {

            @Override
            protected void onUpdate(AjaxRequestTarget target)
            {
                JCas mergeJCas = null;
                try {
                    mergeJCas = repository.getCorrectionDocumentContent(bratAnnotatorModel
                            .getDocument());
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                gotoPageAddress = BratAjaxCasUtil.getSentenceAddress(mergeJCas,
                        gotoPageTextField.getModelObject());

            }
        });

        add(new AjaxLink<Void>("gotoPageLink")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (gotoPageAddress == -2) {
                    target.appendJavaScript("alert('This sentence number is either negative or beyond the last sentence number!')");
                }
                else if (bratAnnotatorModel.getDocument() != null) {

                    if (gotoPageAddress == -1) {
                        // Not Updated, default used
                        JCas mergeJCas = null;
                        try {
                            mergeJCas = repository.getCorrectionDocumentContent(bratAnnotatorModel
                                    .getDocument());
                        }
                        catch (UIMAException e) {
                            error(ExceptionUtils.getRootCause(e));
                        }
                        catch (ClassNotFoundException e) {
                            error(ExceptionUtils.getRootCause(e));
                        }
                        catch (IOException e) {
                            error(e.getMessage());
                        }
                        gotoPageAddress = BratAjaxCasUtil.getSentenceAddress(mergeJCas, 10);
                    }
                    if (bratAnnotatorModel.getSentenceAddress() != gotoPageAddress) {
                        bratAnnotatorModel.setSentenceAddress(gotoPageAddress);

                        CurationBuilder builder = new CurationBuilder(repository, annotationService);
                        try {
                            curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                            setCurationSegmentBeginEnd();
                        }
                        catch (UIMAException e) {
                            ExceptionUtils.getRootCauseMessage(e);
                        }
                        catch (ClassNotFoundException e) {
                            error(e.getMessage());
                        }
                        catch (IOException e) {
                            error(e.getMessage());
                        }
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                        update(target);
                    }
                    else {
                        target.appendJavaScript("alert('This sentence is on the same page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        });

        finish = new FinishImage("finishImage", new LoadableDetachableModel<BratAnnotatorModel>()
        {
            private static final long serialVersionUID = -2737326878793568454L;

            @Override
            protected BratAnnotatorModel load()
            {
                return bratAnnotatorModel;
            }
        });

        add(new FinishLink("showYesNoModalPanel",
                new Model<BratAnnotatorModel>(bratAnnotatorModel), finish)
        {
            private static final long serialVersionUID = -4657965743173979437L;
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
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        mergeJCas = repository.getCorrectionDocumentContent(bratAnnotatorModel
                                .getDocument());
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (IOException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    int nextSentenceAddress = BratAjaxCasUtil
                            .getNextDisplayWindowSentenceBeginAddress(mergeJCas,
                                    bratAnnotatorModel.getSentenceAddress(),
                                    bratAnnotatorModel.getWindowSize());
                    if (bratAnnotatorModel.getSentenceAddress() != nextSentenceAddress) {
                        bratAnnotatorModel.setSentenceAddress(nextSentenceAddress);

                        CurationBuilder builder = new CurationBuilder(repository, annotationService);
                        try {
                            curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                            setCurationSegmentBeginEnd();
                        }
                        catch (UIMAException e) {
                            ExceptionUtils.getRootCauseMessage(e);
                        }
                        catch (ClassNotFoundException e) {
                            error(e.getMessage());
                        }
                        catch (IOException e) {
                            error(e.getMessage());
                        }
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                        update(target);
                    }

                    else {
                        target.appendJavaScript("alert('This is last page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_down }, EventType.click)));

        // SHow the previous page of this document
        add(new AjaxLink<Void>("showPrevious")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {

                    JCas mergeJCas = null;
                    try {
                        mergeJCas = repository.getCorrectionDocumentContent(bratAnnotatorModel
                                .getDocument());
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (IOException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }

                    int previousSentenceAddress = BratAjaxCasUtil
                            .getPreviousDisplayWindowSentenceBeginAddress(mergeJCas,
                                    bratAnnotatorModel.getSentenceAddress(),
                                    bratAnnotatorModel.getWindowSize());
                    if (bratAnnotatorModel.getSentenceAddress() != previousSentenceAddress) {
                        bratAnnotatorModel.setSentenceAddress(previousSentenceAddress);

                        CurationBuilder builder = new CurationBuilder(repository, annotationService);
                        try {
                            curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                            setCurationSegmentBeginEnd();
                        }
                        catch (UIMAException e) {
                            ExceptionUtils.getRootCauseMessage(e);
                        }
                        catch (ClassNotFoundException e) {
                            error(e.getMessage());
                        }
                        catch (IOException e) {
                            error(e.getMessage());
                        }
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                        update(target);
                    }
                    else {
                        target.appendJavaScript("alert('This is First Page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_up }, EventType.click)));

        add(new AjaxLink<Void>("showFirst")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {
                    if (bratAnnotatorModel.getFirstSentenceAddress() != bratAnnotatorModel
                            .getSentenceAddress()) {
                        bratAnnotatorModel.setSentenceAddress(bratAnnotatorModel
                                .getFirstSentenceAddress());

                        CurationBuilder builder = new CurationBuilder(repository, annotationService);
                        try {
                            curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                            setCurationSegmentBeginEnd();
                        }
                        catch (UIMAException e) {
                            ExceptionUtils.getRootCauseMessage(e);
                        }
                        catch (ClassNotFoundException e) {
                            error(e.getMessage());
                        }
                        catch (IOException e) {
                            error(e.getMessage());
                        }
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                        update(target);
                    }
                    else {
                        target.appendJavaScript("alert('This is first page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Home }, EventType.click)));

        add(new AjaxLink<Void>("showLast")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        mergeJCas = repository.getCorrectionDocumentContent(bratAnnotatorModel
                                .getDocument());
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (IOException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    int lastDisplayWindowBeginingSentenceAddress = BratAjaxCasUtil
                            .getLastDisplayWindowFirstSentenceAddress(mergeJCas,
                                    bratAnnotatorModel.getWindowSize());
                    if (lastDisplayWindowBeginingSentenceAddress != bratAnnotatorModel
                            .getSentenceAddress()) {
                        bratAnnotatorModel
                                .setSentenceAddress(lastDisplayWindowBeginingSentenceAddress);

                        CurationBuilder builder = new CurationBuilder(repository, annotationService);
                        try {
                            curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                            setCurationSegmentBeginEnd();
                        }
                        catch (UIMAException e) {
                            ExceptionUtils.getRootCauseMessage(e);
                        }
                        catch (ClassNotFoundException e) {
                            error(e.getMessage());
                        }
                        catch (IOException e) {
                            error(e.getMessage());
                        }
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                        update(target);
                    }
                    else {
                        target.appendJavaScript("alert('This is last Page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.End }, EventType.click)));
    }

    /**
     * for the first time the page is accessed, open the <b>open document dialog</b>
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
        if(bratAnnotatorModel.getProject() != null){

            mergeVisualizer.setModelObject(bratAnnotatorModel);
            mergeVisualizer.setCollection("#" + bratAnnotatorModel.getProject().getName() + "/");
            mergeVisualizer.reloadContent(response);

        }

    }

    @SuppressWarnings("unchecked")
    private void init()
        throws UIMAException, ClassNotFoundException, IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User logedInUser = repository.getUser(SecurityContextHolder.getContext()
                .getAuthentication().getName());
        JCas jCas = null;
        try {
            AnnotationDocument logedInUserAnnotationDocument = repository.getAnnotationDocument(
                    bratAnnotatorModel.getDocument(), logedInUser);
            jCas = repository.getAnnotationDocumentContent(logedInUserAnnotationDocument);

        }
        catch (UIMAException e) {
            throw e;
        }
        catch (ClassNotFoundException e) {
            throw e;
        }
        // First time the Merge Cas is opened
        catch (IOException e) {
            throw e;
        }
        // Get information to be populated to bratAnnotatorModel from the JCAS of the logged in user
        //
        catch (DataRetrievalFailureException e) {
            BratAjaxCasController controller = new BratAjaxCasController(repository,
                    annotationService);
            jCas = controller.getJCas(bratAnnotatorModel.getDocument(), bratAnnotatorModel
                    .getDocument().getProject(), logedInUser);
            // This is the auto annotation, save it under CURATION_USER
            repository.createCorrectionDocumentContent(jCas, bratAnnotatorModel.getDocument(),
                    logedInUser);
            // remove all annotation so that the user can correct from the auto annotation
            clearJcasAnnotations(jCas, bratAnnotatorModel.getDocument(), logedInUser);
        }
        catch (NoResultException e) {
            BratAjaxCasController controller = new BratAjaxCasController(repository,
                    annotationService);
            jCas = controller.getJCas(bratAnnotatorModel.getDocument(), bratAnnotatorModel
                    .getDocument().getProject(), logedInUser);
            // This is the auto annotation, save it under CURATION_USER
            repository.createCorrectionDocumentContent(jCas, bratAnnotatorModel.getDocument(),
                    logedInUser);
            // remove all annotation so that the user can correct from the auto annotation
            clearJcasAnnotations(jCas, bratAnnotatorModel.getDocument(), logedInUser);
        }

        if (bratAnnotatorModel.getSentenceAddress() == -1
                || bratAnnotatorModel.getDocument().getId() != currentDocumentId
                || bratAnnotatorModel.getProject().getId() != currentprojectId) {

            try {
                bratAnnotatorModel
                        .setSentenceAddress(BratAjaxCasUtil.getFirstSenetnceAddress(jCas));
                bratAnnotatorModel.setLastSentenceAddress(BratAjaxCasUtil
                        .getLastSenetnceAddress(jCas));
                bratAnnotatorModel.setFirstSentenceAddress(bratAnnotatorModel.getSentenceAddress());

                AnnotationPreference preference = new AnnotationPreference();
                ApplicationUtils.setAnnotationPreference(preference, username, repository,
                        annotationService, bratAnnotatorModel, Mode.CORRECTION);
            }
            catch (DataRetrievalFailureException ex) {
                throw ex;
            }
            catch (BeansException e) {
                throw e;
            }
            catch (FileNotFoundException e) {
                throw e;
            }
            catch (IOException e) {
                throw e;
            }
        }
        bratAnnotatorModel.setUser(logedInUser);

        currentprojectId = bratAnnotatorModel.getProject().getId();
        currentDocumentId = bratAnnotatorModel.getDocument().getId();
    }

    private void setCurationSegmentBeginEnd()
        throws UIMAException, ClassNotFoundException, IOException
    {
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);
        JCas jCas = controller.getJCas(bratAnnotatorModel.getDocument(),
                bratAnnotatorModel.getProject(), bratAnnotatorModel.getUser());
        int lastSentenceAddressInDisplayWindow = BratAjaxCasUtil
                .getNextDisplayWindowSentenceBeginAddress(jCas,
                        bratAnnotatorModel.getSentenceAddress(), bratAnnotatorModel.getWindowSize());
        curationSegment.setBegin(BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                bratAnnotatorModel.getSentenceAddress()));
        curationSegment.setEnd(BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                lastSentenceAddressInDisplayWindow));

    }

    private void clearJcasAnnotations(JCas aJCas, SourceDocument aSourceDocument, User aUser)
        throws IOException
    {
        List<Annotation> annotationsToRemove = new ArrayList<Annotation>();
        for (Annotation a : select(aJCas, Annotation.class)) {
            if (!(a instanceof Token || a instanceof Sentence || a instanceof DocumentMetaData)) {
                annotationsToRemove.add(a);
            }
        }
        for (Annotation annotation : annotationsToRemove) {
            aJCas.removeFsFromIndexes(annotation);
        }
        repository.createAnnotationDocumentContent(aJCas, aSourceDocument, aUser);
    }

    private void update(AjaxRequestTarget target)
    {
        try {
            BratCuratorUtility.updatePanel(target, sentenceOuterView, curationContainer,
                    mergeVisualizer, repository, annotationSelectionByUsernameAndAddress,
                    curationSegment, annotationService, jsonConverter);
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
        target.add(sentenceOuterView);
        target.add(numberOfPages);

        mergeVisualizer.setModelObject(bratAnnotatorModel);
        mergeVisualizer.setCollection("#" + bratAnnotatorModel.getProject().getName() + "/");
        mergeVisualizer.reloadContent(target);
    }

}
