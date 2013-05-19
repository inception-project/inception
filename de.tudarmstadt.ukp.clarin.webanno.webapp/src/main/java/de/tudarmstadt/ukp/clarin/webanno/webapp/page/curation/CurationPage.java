/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.beans.BeansException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.context.SecurityContextHolder;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenDocumentModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.YesNoModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationBuilder;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome.WelcomePage;

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

    private CurationPanel curationPanel;
    private OpenDocumentModel openDataModel;

    private CurationContainer curationContainer;
    private BratAnnotatorModel bratAnnotatorModel;
    private Label documentNameLabel;

    private long currentDocumentId;
    private long currentprojectId;

    // Open the dialog window on first load
    boolean firstLoad = true;

    public CurationPage()
    {
        openDataModel = new OpenDocumentModel();
        bratAnnotatorModel = new BratAnnotatorModel();

        curationContainer = new CurationContainer();
        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
        curationPanel = new CurationPanel("curationPanel", curationContainer);
        curationPanel.setOutputMarkupId(true);
        add(curationPanel);

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

        add(new AjaxLink<Void>("showOpenDocumentModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                openDocumentsModal.setContent(new OpenModalWindowPanel(openDocumentsModal
                        .getContentId(), openDataModel, openDocumentsModal, Mode.CURATION));
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
                        if (openDataModel.getDocument() != null
                                && repository.existsFinishedAnnotation(openDataModel.getDocument(),
                                        openDataModel.getProject())
                                && !openDataModel.getDocument().getState()
                                        .equals(SourceDocumentState.CURATION_FINISHED)) {
                            // Update source document state to CURRATION_INPROGRESS
                            openDataModel.getDocument().setState(
                                    SourceDocumentState.CURATION_INPROGRESS);
                            try {
                                repository.createSourceDocument(openDataModel.getDocument(), user);
                            }
                            catch (IOException e) {
                                error("Unable to update source document "
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }

                            // Get settings from preferences, if available
                            // TEST - set window size to 10

                            bratAnnotatorModel.setDocument(openDataModel.getDocument());
                            bratAnnotatorModel.setProject(openDataModel.getProject());

                            try {
                                initBratAnnotatorDataModel();
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
                            // transform jcas to objects for wicket components
                            CurationBuilder builder = new CurationBuilder(repository);
                            curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                            curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                            updatePanel(curationContainer);
                            // target.add(curationPanel) should work!
                            target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

                        }
                        else if (openDataModel.getDocument() != null
                                && openDataModel.getDocument().getState()
                                        .equals(SourceDocumentState.CURATION_FINISHED)) {
                            target.appendJavaScript("alert('Curation Has been closed. Document has been closed for curation. Ask admin to re-open!')");
                        }
                        else if (openDataModel.getDocument() == null) {
                            setResponsePage(WelcomePage.class);
                        }
                        else {
                            target.appendJavaScript("alert('Annotation in progress for document ["
                                    + openDataModel.getDocument().getName() + "]')");
                        }
                        target.add(documentNameLabel);
                    }
                });
                openDocumentsModal.show(aTarget);
            }
        });

        final ModalWindow yesNoModal;
        add(yesNoModal = new ModalWindow("yesNoModal"));
        yesNoModal.setOutputMarkupId(true);

        yesNoModal.setInitialWidth(400);
        yesNoModal.setInitialHeight(50);
        yesNoModal.setResizable(true);
        yesNoModal.setWidthUnit("px");
        yesNoModal.setHeightUnit("px");
        yesNoModal.setTitle("Are you sure you want to finish curating?");

        add(new AjaxLink<Void>("showYesNoModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                yesNoModal.setContent(new YesNoModalPanel(yesNoModal.getContentId(), openDataModel,
                        yesNoModal, Mode.CURATION));
                yesNoModal.show(target);
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
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
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

                     // transform jcas to objects for wicket components
                        CurationBuilder builder = new CurationBuilder(repository);
                        curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                        updatePanel(curationContainer);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
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
                        mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
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
                     // transform jcas to objects for wicket components
                        CurationBuilder builder = new CurationBuilder(repository);
                        curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                        updatePanel(curationContainer);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
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
                    bratAnnotatorModel
                            .setSentenceAddress(bratAnnotatorModel
                                    .getFirstSentenceAddress());

                 // transform jcas to objects for wicket components
                    CurationBuilder builder = new CurationBuilder(repository);
                    curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                    curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                    updatePanel(curationContainer);
                    target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

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
                    mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
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
                 // transform jcas to objects for wicket components
                    CurationBuilder builder = new CurationBuilder(repository);
                    curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                    curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                    updatePanel(curationContainer);
                    target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

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
    // Update the curation panel.

    private void updatePanel(CurationContainer aCurationContainer)
    {
        // remove old panel, create new one, add it
        remove(curationPanel);
        curationPanel = new CurationPanel("curationPanel", aCurationContainer);
        curationPanel.setOutputMarkupId(true);
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

    @SuppressWarnings("unchecked")
    private void initBratAnnotatorDataModel()
        throws UIMAException, IOException, ClassNotFoundException
    {

        JCas mergeJCas = null;
        try {
            mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel.getDocument());
        }
        catch (UIMAException e) {
           throw e;
        }
        catch (ClassNotFoundException e) {
            throw e;
        }
        catch (IOException e) {
            throw e;
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (bratAnnotatorModel.getSentenceAddress() == -1
                || bratAnnotatorModel.getDocument().getId() != currentDocumentId
                || bratAnnotatorModel.getProject().getId() != currentprojectId) {

            try {
                bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil
                        .getFirstSenetnceAddress(mergeJCas));
                bratAnnotatorModel.setLastSentenceAddress(BratAjaxCasUtil
                        .getLastSenetnceAddress(mergeJCas));
                bratAnnotatorModel.setFirstSentenceAddress(bratAnnotatorModel.getSentenceAddress());

                AnnotationPreference preference = new AnnotationPreference();
                ApplicationUtils.setAnnotationPreference(preference, username, repository,
                        annotationService, bratAnnotatorModel, Mode.CURATION);
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
        // This is a Curation Operation, add to the data model a CURATION Mode
        bratAnnotatorModel.setMode(Mode.CURATION);

        User userLoggedIn = repository.getUser(SecurityContextHolder.getContext()
                .getAuthentication().getName());
        bratAnnotatorModel.setUser(userLoggedIn);

        currentprojectId = bratAnnotatorModel.getProject().getId();
        currentDocumentId = bratAnnotatorModel.getDocument().getId();
    }
}
