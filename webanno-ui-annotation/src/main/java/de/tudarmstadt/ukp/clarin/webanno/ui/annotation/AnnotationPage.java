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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getLastSentenceInDisplayWindow;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.selectSentenceAt;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.action.ActionContext;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.exception.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ParseException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;
import de.tudarmstadt.ukp.clarin.webanno.constraints.visitor.ParserVisitor;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.AnnotationLayersModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.ExportModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.FinishImage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.FinishLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.GuidelineModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.core.app.ApplicationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * A wicket page for the Brat Annotation/Visualization page. Included components for pagination,
 * annotation layer configuration, and Exporting document
 *
 *
 */
@MountPath("/annotation.html")
public class AnnotationPage
    extends ApplicationPageBase
{
    private static final Log LOG = LogFactory.getLog(AnnotationPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private BratAnnotator annotator;

    private FinishImage finish;

    private NumberTextField<Integer> gotoPageTextField;
    private AnnotationDetailEditorPanel editor;

    private int gotoPageAddress;

    // Open the dialog window on first load
    private boolean firstLoad = true;

    private Label numberOfPages;
    private DocumentNamePanel documentNamePanel;

    private long currentprojectId;

    private int totalNumberOfSentence;

    private boolean closeButtonClicked;
    private ActionContext bModel = new ActionContext();

    private WebMarkupContainer sidebarCell;
    private WebMarkupContainer annotationViewCell;
    
    public AnnotationPage()
    {
        sidebarCell = new WebMarkupContainer("sidebarCell") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                aTag.put("width", bModel.getPreferences().getSidebarSize()+"%");
            }
        };
        sidebarCell.setOutputMarkupId(true);
        add(sidebarCell);

        annotationViewCell = new WebMarkupContainer("annotationViewCell") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                aTag.put("width", (100-bModel.getPreferences().getSidebarSize())+"%");
            }
        };
        annotationViewCell.setOutputMarkupId(true);
        add(annotationViewCell);

        editor = new AnnotationDetailEditorPanel(
                "annotationDetailEditorPanel", new Model<ActionContext>(bModel))
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget, ActionContext aBModel)
            {
                aTarget.addChildren(getPage(), FeedbackPanel.class);

                try {
                    annotator.bratRender(aTarget, getCas(aBModel));
                }
                catch (UIMAException | ClassNotFoundException | IOException e) {
                    LOG.info("Error reading CAS " + e.getMessage());
                    error("Error reading CAS " + e.getMessage());
                    return;
                }

                annotator.bratSetHighlight(aTarget, aBModel.getSelection().getAnnotation());

                annotator.onChange(aTarget, aBModel);
            }

            @Override
            protected void onAutoForward(AjaxRequestTarget aTarget, ActionContext aBModel)
            {
                try {
                    annotator.autoForward(aTarget, getCas(aBModel));
                }
                catch (UIMAException | ClassNotFoundException | IOException | BratAnnotationException e) {
                    LOG.info("Error reading CAS " + e.getMessage());
                    error("Error reading CAS " + e.getMessage());
                    return;
                }
            }
        };
        editor.setOutputMarkupId(true);
        sidebarCell.add(editor);
        
        annotator = new BratAnnotator("embedder1", new Model<ActionContext>(bModel),
                editor)
        {
            private static final long serialVersionUID = 7279648231521710155L;

            @Override
            public void onChange(AjaxRequestTarget aTarget, ActionContext aBratAnnotatorModel)
            {
                bModel = aBratAnnotatorModel;
                aTarget.add(numberOfPages);
            }

            @Override
            public void renderHead(IHeaderResponse aResponse)
            {
                super.renderHead(aResponse);

                // If the page is reloaded in the browser and a document was already open, we need
                // to render it. We use the "later" commands here to avoid polluting the Javascript
                // header items with document data and because loading times are not that critical
                // on a reload.
                if (getModelObject().getProject() != null) {
                    // We want to trigger a late rendering only on a page reload, but not on a
                    // Ajax request.
                    if (!aResponse.getResponse().getClass().getName().endsWith("AjaxResponse")) {
                        aResponse.render(OnLoadHeaderItem.forScript(bratInitLaterCommand()));
                        aResponse.render(OnLoadHeaderItem.forScript(bratRenderLaterCommand()));
                    }
                }
            }
        };
        annotationViewCell.add(annotator);

        // This is an Annotation Operation, set model to ANNOTATION mode
        bModel.setMode(Mode.ANNOTATION);

        add(documentNamePanel = (DocumentNamePanel) new DocumentNamePanel("documentNamePanel",
                new Model<ActionContext>(bModel)).setOutputMarkupId(true));

        numberOfPages = new Label("numberOfPages", new Model<String>());
        numberOfPages.setOutputMarkupId(true);
        add(numberOfPages);

        final ModalWindow openDocumentsModal;
        add(openDocumentsModal = new ModalWindow("openDocumentsModal"));
        openDocumentsModal.setOutputMarkupId(true);

        openDocumentsModal.setInitialWidth(620);
        openDocumentsModal.setInitialHeight(440);
        openDocumentsModal.setResizable(true);
        openDocumentsModal.setWidthUnit("px");
        openDocumentsModal.setHeightUnit("px");
        openDocumentsModal.setTitle("Open document");
        openDocumentsModal.setCloseButtonCallback(new ModalWindow.CloseButtonCallback()
        {
            private static final long serialVersionUID = -5423095433535634321L;

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget aTarget)
            {
                closeButtonClicked = true;
                return true;
            }
        });

        add(new AjaxLink<Void>("showOpenDocumentModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
            	bModel.getSelection().clear();             
                closeButtonClicked = false;
                openDocumentsModal.setContent(new OpenModalWindowPanel(openDocumentsModal
                        .getContentId(), bModel, openDocumentsModal, Mode.ANNOTATION)
                {

                    private static final long serialVersionUID = -3434069761864809703L;

                    @Override
                    protected void onCancel(AjaxRequestTarget aTarget)
                    {
                        closeButtonClicked = true;
                    };
                });
                openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        // A hack, the dialog opens for the first time, and if no document is
                        // selected window will be "blind down". Something in the brat js causes
                        // this!
                        if (bModel.getProject() == null || bModel.getDocument() == null) {
                            setResponsePage(getApplication().getHomePage());
                        }

                        // Dialog was cancelled rather that a document was selected.
                        if (closeButtonClicked) {
                            return;
                        }

                        loadDocumentAction(target);
                        try {
							editor.refresh(target);
						} catch (BratAnnotationException e) {
							error("Error loading layers"+e.getMessage());
						}
                    }
                });
                // target.appendJavaScript("Wicket.Window.unloadConfirmation = false;");
                openDocumentsModal.show(aTarget);
            }
        });

        add(new AnnotationLayersModalPanel("annotationLayersModalPanel",
                new Model<ActionContext>(bModel), editor)
        {
            private static final long serialVersionUID = -4657965743173979437L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {

                try {
                    // Re-render the whole page because the width of the sidebar may have changed
                    aTarget.add(AnnotationPage.this);
                    JCas jCas = getJCas();
                    annotator.bratRender(aTarget, jCas);
                    updateSentenceAddress(jCas, aTarget);
                }
                catch (UIMAException | ClassNotFoundException | IOException e) {
                    LOG.info("Error reading CAS " + e.getMessage());
                    error("Error reading CAS " + e.getMessage());
                    return;
                }

            }
        });

        add(new ExportModalPanel("exportModalPanel", new Model<ActionContext>(bModel)){
            private static final long serialVersionUID = -468896211970839443L;
            
            {
                setOutputMarkupId(true);
                setOutputMarkupPlaceholderTag(true);
            }
            
            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(bModel.getProject() != null
                        && (SecurityUtil.isAdmin(bModel.getProject(), repository, bModel.getUser())
                                || !bModel.getProject().isDisableExport()));
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
                editor.reset(aTarget);
                // List of all Source Documents in the project
                List<SourceDocument> listOfSourceDocuements=   getListOfDocs();

                // Index of the current source document in the list
                int currentDocumentIndex = listOfSourceDocuements.indexOf(bModel.getDocument());

                // If the first the document
                if (currentDocumentIndex == 0) {
                    aTarget.appendJavaScript("alert('This is the first document!')");
                    return;
                }
                bModel.setDocument(listOfSourceDocuements.get(currentDocumentIndex - 1));

                loadDocumentAction(aTarget);
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
                editor.reset(aTarget);
                // List of all Source Documents in the project
                List<SourceDocument> listOfSourceDocuements=   getListOfDocs();

                // Index of the current source document in the list
                int currentDocumentIndex = listOfSourceDocuements.indexOf(bModel.getDocument());

                // If the first document
                if (currentDocumentIndex == listOfSourceDocuements.size() - 1) {
                    aTarget.appendJavaScript("alert('This is the last document!')");
                    return;
                }
                bModel.setDocument(listOfSourceDocuements.get(currentDocumentIndex + 1));

                loadDocumentAction(aTarget);
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_down }, EventType.click)));

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
                try {
                    if (bModel.getDocument() != null) {
                        JCas jCas = getJCas();
                        int nextSentenceAddress = BratAjaxCasUtil.getNextPageFirstSentenceAddress(
                                jCas, bModel.getSentenceAddress(), bModel.getPreferences()
                                        .getWindowSize());
                        if (bModel.getSentenceAddress() != nextSentenceAddress) {

                            updateSentenceNumber(jCas, nextSentenceAddress);

                            aTarget.addChildren(getPage(), FeedbackPanel.class);
                            annotator.bratRenderLater(aTarget);
                            gotoPageTextField.setModelObject(BratAjaxCasUtil
                                    .getFirstSentenceNumber(jCas, bModel.getSentenceAddress()) + 1);
                            updateSentenceAddress(jCas, aTarget);
                        }

                        else {
                            aTarget.appendJavaScript("alert('This is last page!')");
                        }
                    }
                    else {
                        aTarget.appendJavaScript("alert('Please open a document first!')");
                    }
                }
                catch (Exception e) {
                    error(e.getMessage());
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_down }, EventType.click)));

        // Show the previous page of this document
        add(new AjaxLink<Void>("showPrevious")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                try {
                    if (bModel.getDocument() != null) {

                        JCas jCas = getJCas();
                        int firstSentenceAddress = BratAjaxCasUtil.getFirstSentenceAddress(jCas);

                        int previousSentenceAddress = BratAjaxCasUtil
                                .getPreviousDisplayWindowSentenceBeginAddress(jCas, bModel
                                        .getSentenceAddress(), bModel.getPreferences()
                                        .getWindowSize());
                        //Since BratAjaxCasUtil.getPreviousDisplayWindowSentenceBeginAddress returns same address 
                        //if there are not much sentences to go back to as defined in windowSize
                        if (
                                previousSentenceAddress == bModel.getSentenceAddress() && 
                                // Check whether it's not the beginning of document
                                bModel.getSentenceAddress() != firstSentenceAddress
                        ) {
                            previousSentenceAddress = firstSentenceAddress;
                        }
                        
                        if (bModel.getSentenceAddress() != previousSentenceAddress) {
                            updateSentenceNumber(jCas, previousSentenceAddress);

                            aTarget.addChildren(getPage(), FeedbackPanel.class);
                            annotator.bratRenderLater(aTarget);
                            gotoPageTextField.setModelObject(BratAjaxCasUtil
                                    .getFirstSentenceNumber(jCas, bModel.getSentenceAddress()) + 1);
                            updateSentenceAddress(jCas, aTarget);
                        }
                        else {
                            aTarget.appendJavaScript("alert('This is First Page!')");
                        }
                    }
                    else {
                        aTarget.appendJavaScript("alert('Please open a document first!')");
                    }
                }
                catch (Exception e) {
                    error(e.getMessage());
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_up }, EventType.click)));

        add(new AjaxLink<Void>("showFirst")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                try {
                    if (bModel.getDocument() != null) {

                        JCas jCas = getJCas();
                        int firstSentenceAddress = BratAjaxCasUtil.getFirstSentenceAddress(jCas);

                        if (firstSentenceAddress != bModel.getSentenceAddress()) {
                            updateSentenceNumber(jCas, firstSentenceAddress);

                            aTarget.addChildren(getPage(), FeedbackPanel.class);
                            annotator.bratRenderLater(aTarget);
                            gotoPageTextField.setModelObject(BratAjaxCasUtil
                                    .getFirstSentenceNumber(jCas, bModel.getSentenceAddress()) + 1);
                            updateSentenceAddress(jCas, aTarget);
                        }
                        else {
                            aTarget.appendJavaScript("alert('This is first page!')");
                        }
                    }
                    else {
                        aTarget.appendJavaScript("alert('Please open a document first!')");
                    }
                }
                catch (Exception e) {
                    error(e.getMessage());
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Home }, EventType.click)));

        add(new AjaxLink<Void>("showLast")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                try {
                    if (bModel.getDocument() != null) {

                        JCas jCas = getJCas();

                        int lastDisplayWindowBeginingSentenceAddress = BratAjaxCasUtil
                                .getLastDisplayWindowFirstSentenceAddress(jCas, bModel
                                        .getPreferences().getWindowSize());
                        if (lastDisplayWindowBeginingSentenceAddress != bModel.getSentenceAddress()) {

                            updateSentenceNumber(jCas, lastDisplayWindowBeginingSentenceAddress);

                            aTarget.addChildren(getPage(), FeedbackPanel.class);
                            annotator.bratRenderLater(aTarget);
                            gotoPageTextField.setModelObject(BratAjaxCasUtil
                                    .getFirstSentenceNumber(jCas, bModel.getSentenceAddress()) + 1);
                            updateSentenceAddress(jCas, aTarget);
                        }
                        else {
                            aTarget.appendJavaScript("alert('This is last Page!')");
                        }
                    }
                    else {
                        aTarget.appendJavaScript("alert('Please open a document first!')");
                    }
                }
                catch (Exception e) {
                    error(e.getMessage());
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.End }, EventType.click)));

        add(new AjaxLink<Void>("toggleScriptDirection")
        {
            private static final long serialVersionUID = -4332566542278611728L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                if (ScriptDirection.LTR.equals(bModel.getScriptDirection())) {
                    bModel.setScriptDirection(ScriptDirection.RTL);
                }
                else {
                    bModel.setScriptDirection(ScriptDirection.LTR);
                }
                annotator.bratRenderLater(aTarget);
            }
        });
        add(new GuidelineModalPanel("guidelineModalPanel", new Model<ActionContext>(bModel)));

        gotoPageTextField = (NumberTextField<Integer>) new NumberTextField<Integer>("gotoPageText",
                new Model<Integer>(0));
        Form<Void> gotoPageTextFieldForm = new Form<Void>("gotoPageTextFieldForm");
        gotoPageTextFieldForm.add(new AjaxFormSubmitBehavior(gotoPageTextFieldForm, "submit")
        {
            private static final long serialVersionUID = -4549805321484461545L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                try {
                    if (gotoPageAddress == 0) {
                        aTarget.appendJavaScript("alert('The sentence number entered is not valid')");
                        return;
                    }
                    if (bModel.getSentenceAddress() != gotoPageAddress) {
                        JCas jCas = getJCas();

                        updateSentenceNumber(jCas, gotoPageAddress);

                        aTarget.addChildren(getPage(), FeedbackPanel.class);
                        annotator.bratRenderLater(aTarget);
                        aTarget.add(numberOfPages);
                        gotoPageTextField.setModelObject(BratAjaxCasUtil.getFirstSentenceNumber(
                                jCas, bModel.getSentenceAddress()) + 1);
                        aTarget.add(gotoPageTextField);
                    }
                }
                catch (Exception e) {
                    error(e.getMessage());
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                }
            }
        });
        gotoPageTextField.setType(Integer.class);
        gotoPageTextField.setMinimum(1);
        gotoPageTextField.setDefaultModelObject(1);
        add(gotoPageTextFieldForm.add(gotoPageTextField));

        gotoPageTextField.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = 56637289242712170L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                try {
                    if (gotoPageTextField.getModelObject() < 1) {
                        aTarget.appendJavaScript("alert('Page number shouldn't be less than 1')");
                    }
                    else {
                        updateSentenceAddress(getJCas(), aTarget);
                    }
                }
                catch (Exception e) {
                    error(e.getMessage());
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                }
            }
        });

        add(new AjaxLink<Void>("gotoPageLink")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                try {
                    if (gotoPageAddress == 0) {
                        aTarget.appendJavaScript("alert('The sentence number entered is not valid')");
                        return;
                    }
                    if (bModel.getDocument() == null) {
                        aTarget.appendJavaScript("alert('Please open a document first!')");
                        return;
                    }
                    if (bModel.getSentenceAddress() != gotoPageAddress) {
                        JCas jCas = getJCas();
                        updateSentenceNumber(jCas, gotoPageAddress);
                        updateSentenceAddress(jCas, aTarget);
                        annotator.bratRenderLater(aTarget);
                    }
                }
                catch (Exception e) {
                    error(e.getMessage());
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                }
            }
        });

        finish = new FinishImage("finishImage", new Model<ActionContext>(bModel));
        finish.setOutputMarkupId(true);

        add(new FinishLink("showYesNoModalPanel", new Model<ActionContext>(bModel), finish)
        {
            private static final long serialVersionUID = -4657965743173979437L;
            
            @Override
            public void onClose(AjaxRequestTarget aTarget)
            {
                super.onClose(aTarget);
                aTarget.add(editor);
            }
        });
    }
    
	private List<SourceDocument> getListOfDocs() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userRepository.get(username);
		// List of all Source Documents in the project
		List<SourceDocument> listOfSourceDocuements = repository.listSourceDocuments(bModel.getProject());
		List<SourceDocument> sourceDocumentsInIgnoreState = new ArrayList<SourceDocument>();
		for (SourceDocument sourceDocument : listOfSourceDocuements) {
			if (repository.existsAnnotationDocument(sourceDocument, user) && repository
					.getAnnotationDocument(sourceDocument, user).getState().equals(AnnotationDocumentState.IGNORE)) {
				sourceDocumentsInIgnoreState.add(sourceDocument);
			}
		}

		listOfSourceDocuements.removeAll(sourceDocumentsInIgnoreState);
		return listOfSourceDocuements;
	}

    private void updateSentenceAddress(JCas aJCas, AjaxRequestTarget aTarget)
        throws UIMAException, IOException, ClassNotFoundException
    {
        gotoPageAddress = BratAjaxCasUtil.getSentenceAddress(aJCas,
                gotoPageTextField.getModelObject());

        String labelText = "";
        if (bModel.getDocument() != null) {
        	
        	List<SourceDocument> listofDoc = getListOfDocs();
        	
        	int docIndex = listofDoc.indexOf(bModel.getDocument())+1;
        	
            totalNumberOfSentence = BratAjaxCasUtil.getNumberOfPages(aJCas);

            // If only one page, start displaying from sentence 1
            if (totalNumberOfSentence == 1) {
                int firstSentenceAddress = BratAjaxCasUtil.getFirstSentenceAddress(aJCas);
                bModel.setSentenceAddress(firstSentenceAddress);
            }
            int sentenceNumber = BratAjaxCasUtil.getFirstSentenceNumber(aJCas,
                    bModel.getSentenceAddress());
            int firstSentenceNumber = sentenceNumber + 1;
            int lastSentenceNumber;
            if (firstSentenceNumber + bModel.getPreferences().getWindowSize() - 1 < totalNumberOfSentence) {
                lastSentenceNumber = firstSentenceNumber + bModel.getPreferences().getWindowSize()
                        - 1;
            }
            else {
                lastSentenceNumber = totalNumberOfSentence;
            }

            labelText = "showing " + firstSentenceNumber + "-" + lastSentenceNumber + " of "
                    + totalNumberOfSentence + " sentences [document "
            		+ docIndex +" of "+ listofDoc.size()+"]";

        }
        else {
            labelText = "";// no document yet selected
        }

        numberOfPages.setDefaultModelObject(labelText);
        aTarget.add(numberOfPages);
        aTarget.add(gotoPageTextField);
    }

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

    private JCas getJCas()
        throws UIMAException, IOException, ClassNotFoundException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);

        SourceDocument aDocument = bModel.getDocument();

        AnnotationDocument annotationDocument = repository.getAnnotationDocument(aDocument, user);

        // If there is no CAS yet for the annotation document, create one.
        return repository.readAnnotationCas(annotationDocument);
    }

    private void updateSentenceNumber(JCas aJCas, int aAddress)
    {
        bModel.setSentenceAddress(aAddress);
        Sentence sentence = selectByAddr(aJCas, Sentence.class, aAddress);
        bModel.setSentenceBeginOffset(sentence.getBegin());
        bModel.setSentenceEndOffset(sentence.getEnd());
        bModel.setFocusSentenceNumber(BratAjaxCasUtil.getSentenceNumber(aJCas, sentence.getBegin()));

        Sentence firstSentence = selectSentenceAt(aJCas, bModel.getSentenceBeginOffset(),
                bModel.getSentenceEndOffset());
        Sentence lastSentenceInPage = getLastSentenceInDisplayWindow(aJCas,
                getAddr(firstSentence), bModel.getPreferences().getWindowSize());
        bModel.setFirstVisibleSentenceNumber(BratAjaxCasUtil.getSentenceNumber(aJCas, firstSentence.getBegin()));
        bModel.setLastVisibleSentenceNumber(BratAjaxCasUtil.getSentenceNumber(aJCas, lastSentenceInPage.getBegin()));
    }

    private void loadDocumentAction(AjaxRequestTarget aTarget)
    {
        LOG.info("BEGIN LOAD_DOCUMENT_ACTION");
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        
        bModel.setUser(userRepository.get(username));

        try {
            // Check if there is an annotation document entry in the database. If there is none,
            // create one.
            AnnotationDocument annotationDocument = repository.createOrGetAnnotationDocument(
                    bModel.getDocument(), user);

            // Read the CAS
            JCas jcas = repository.readAnnotationCas(annotationDocument);

            // Update the annotation document CAS
            repository.upgradeCas(jcas.getCas(), annotationDocument);

            // After creating an new CAS or upgrading the CAS, we need to save it
            repository.writeAnnotationCas(jcas.getCas().getJCas(),
                    annotationDocument.getDocument(), user);

            // (Re)initialize brat model after potential creating / upgrading CAS
            bModel.initForDocument(jcas, repository);

            // Load constraints
            bModel.setConstraints(loadConstraints(aTarget, bModel.getProject()));

            // Load user preferences
            PreferencesUtil.setAnnotationPreference(username, repository, annotationService,
                    bModel, Mode.ANNOTATION);
            

            // if project is changed, reset some project specific settings
            if (currentprojectId != bModel.getProject().getId()) {
                bModel.clearRememberedFeatures();
            }

            currentprojectId = bModel.getProject().getId();

            LOG.debug("Configured BratAnnotatorModel for user [" + bModel.getUser() + "] f:["
                    + bModel.getFirstVisibleSentenceNumber() + "] l:["
                    + bModel.getLastVisibleSentenceNumber() + "] s:["
                    + bModel.getFocusSentenceNumber() + "]");

            gotoPageTextField.setModelObject(1);

            updateSentenceAddress(jcas, aTarget);

//            // Update dynamic elements in action bar
//            aTarget.add(finish);
//            aTarget.add(numberOfPages);
//            aTarget.add(documentNamePanel);
//            // Wicket-level rendering of annotator because it becomes visible
//            // after selecting a document
//            aTarget.add(annotator);
//            // Resize areas according to preferences
//            aTarget.add(sidebarCell);
//            aTarget.add(annotationViewCell);
//            // brat-level initialization and rendering of document
//            annotator.bratInit(aTarget);
//            annotator.bratRender(aTarget, jcas);
            
            // Re-render the whole page because the font size
            aTarget.add(AnnotationPage.this);
            
            // Update document state
            if (bModel.getDocument().getState().equals(SourceDocumentState.NEW)) {
                bModel.getDocument().setState(SourceDocumentStateTransition.transition(
                        SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS));
                repository.createSourceDocument(bModel.getDocument(), user);
            }
        }
        catch (UIMAException e) {
            LOG.error("Error", e);
            aTarget.addChildren(getPage(), FeedbackPanel.class);
            error(ExceptionUtils.getRootCauseMessage(e));
        }
        catch (Exception e) {
            LOG.error("Error", e);
            aTarget.addChildren(getPage(), FeedbackPanel.class);
            error(e.getMessage());
        }

        LOG.info("END LOAD_DOCUMENT_ACTION");
    }

    private ParsedConstraints loadConstraints(AjaxRequestTarget aTarget, Project aProject)
        throws IOException
    {
        ParsedConstraints merged = null;

        for (ConstraintSet set : repository.listConstraintSets(aProject)) {
            try {
                String script = repository.readConstrainSet(set);
                ConstraintsGrammar parser = new ConstraintsGrammar(new StringReader(script));
                Parse p = parser.Parse();
                ParsedConstraints constraints = p.accept(new ParserVisitor());

                if (merged == null) {
                    merged = constraints;
                }
                else {
                    // Merge imports
                    for(Entry<String,String> e: constraints.getImports().entrySet()){
                        //Check if the value already points to some other feature in previous constraint file(s).
                        if(merged.getImports().containsKey(e.getKey()) && !e.getValue().equalsIgnoreCase(merged.getImports().get(e.getKey()))){
                            //If detected, notify user with proper message and abort merging
                            StringBuffer errorMessage = new StringBuffer();
                            errorMessage.append("Conflict detected in imports for key \"");
                            errorMessage.append(e.getKey());
                            errorMessage.append("\", conflicting values are \"");
                            errorMessage.append(e.getValue());
                            errorMessage.append("\" & \"");
                            errorMessage.append(merged.getImports().get(e.getKey()));
                            errorMessage.append("\". Please contact Project Admin for correcting this. Constraints feature may not work.");
                            errorMessage.append("\nAborting Constraint rules merge!");
                            LOG.error(errorMessage.toString());
                            error(errorMessage.toString());
                            break; 
                        }
                    }
                    merged.getImports().putAll(constraints.getImports());

                    // Merge scopes
                    for (Scope scope : constraints.getScopes()) {
                        Scope target = merged.getScopeByName(scope.getScopeName());
                        if (target == null) {
                            // Scope does not exist yet
                            merged.getScopes().add(scope);
                        }
                        else {
                            // Scope already exists
                            target.getRules().addAll(scope.getRules());
                        }
                    }
                }
            }
            catch (ParseException e) {
                LOG.error("Error", e);
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                error(e.getMessage());
            }
        }

        return merged;
    }
}
