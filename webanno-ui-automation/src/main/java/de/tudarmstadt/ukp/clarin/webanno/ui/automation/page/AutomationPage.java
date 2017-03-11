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
package de.tudarmstadt.ukp.clarin.webanno.ui.automation.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentenceAt;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectFollowing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.PreferencesUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.AnnotationPreferencesModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.ExportModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.FinishImage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.GuidelineModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.ui.automation.util.AutomationUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.SuggestionViewPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.service.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.service.CuratorUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * This is the main class for the Automation page. Displays in the lower panel the Automatically
 * annotated document and in the upper panel the annotation pane to trigger automation on the lower
 * pane.
 */
@MountPath("/automation.html")
public class AutomationPage
    extends AnnotationPageBase
{
    private static final Logger LOG = LoggerFactory.getLogger(AutomationPage.class);

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

    private ModalWindow openDocumentsModal;

    private FinishImage finishDocumentIcon;
    private ConfirmationDialog finishDocumentDialog;
    private LambdaAjaxLink finishDocumentLink;

    private BratAnnotator annotationEditor;
    private AnnotationDetailEditorPanel detailEditor;    
    private SuggestionViewPanel suggestionView;
    
    private final Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress = new HashMap<String, Map<Integer, AnnotationSelection>>();

    private final SourceListView curationSegment = new SourceListView();

    @SpringBean(name = "automationService")
    private AutomationService automationService;

    private CurationContainer curationContainer;

    public AutomationPage()
    {
        commonInit();
    }
    
    private void commonInit()
    {
        setVersioned(false);
        
        setModel(Model.of(new AnnotatorStateImpl(Mode.AUTOMATION)));

        WebMarkupContainer sidebarCell = new WebMarkupContainer("sidebarCell") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                AnnotatorState state = AutomationPage.this.getModelObject();
                aTag.put("width", state.getPreferences().getSidebarSize()+"%");
            }
        };
        sidebarCell.setOutputMarkupId(true);
        add(sidebarCell);

        WebMarkupContainer annotationViewCell = new WebMarkupContainer("annotationViewCell") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                AnnotatorState state = AutomationPage.this.getModelObject();
                aTag.put("width", (100-state.getPreferences().getSidebarSize())+"%");
            }
        };
        annotationViewCell.setOutputMarkupId(true);
        add(annotationViewCell);
        
        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();
        CurationUserSegmentForAnnotationDocument curationUserSegmentForAnnotationDocument = new CurationUserSegmentForAnnotationDocument();
        if (getModelObject().getDocument() != null) {
            curationUserSegmentForAnnotationDocument
                    .setAnnotationSelectionByUsernameAndAddress(annotationSelectionByUsernameAndAddress);
            curationUserSegmentForAnnotationDocument.setBratAnnotatorModel(getModelObject());
            sentences.add(curationUserSegmentForAnnotationDocument);
        }
        suggestionView = new SuggestionViewPanel("automateView",
                new Model<LinkedList<CurationUserSegmentForAnnotationDocument>>(sentences))
        {
            private static final long serialVersionUID = 2583509126979792202L;

            @Override
            public void onChange(AjaxRequestTarget aTarget)
            {
                try {
                    // update begin/end of the curation segment based on bratAnnotatorModel changes
                    // (like sentence change in auto-scroll mode,....
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    AnnotatorState state = AutomationPage.this.getModelObject();
                    curationContainer.setBratAnnotatorModel(state);
                    JCas editorCas = getEditorCas();
                    setCurationSegmentBeginEnd(editorCas);

                    CuratorUtil.updatePanel(aTarget, this, curationContainer, annotationEditor,
                            repository, annotationSelectionByUsernameAndAddress, curationSegment,
                            annotationService, userRepository);
                    
                    annotationEditor.bratRender(aTarget, editorCas);
                    aTarget.add(numberOfPages);
                    update(aTarget);
                }
                catch (Exception e) {
                    handleException(aTarget, e);
                }
            }
        };
        suggestionView.setOutputMarkupId(true);
        annotationViewCell.add(suggestionView);

        sidebarCell.add(detailEditor = createDetailEditor());

        annotationEditor = new BratAnnotator("mergeView", getModel(), detailEditor, 
                () -> { return getEditorCas(); });
        annotationViewCell.add(annotationEditor);

        curationContainer = new CurationContainer();
        curationContainer.setBratAnnotatorModel(getModelObject());

        add(documentNamePanel = new DocumentNamePanel("documentNamePanel", getModel()));

        add(numberOfPages = createPositionInfoLabel());

        add(openDocumentsModal = new ModalWindow("openDocumentsModal"));
        openDocumentsModal.setOutputMarkupId(true);

        openDocumentsModal.setInitialWidth(620);
        openDocumentsModal.setInitialHeight(440);
        openDocumentsModal.setResizable(true);
        openDocumentsModal.setWidthUnit("px");
        openDocumentsModal.setHeightUnit("px");
        openDocumentsModal.setTitle("Open document");

        add(new AnnotationPreferencesModalPanel("annotationLayersModalPanel", getModel(), detailEditor)
        {
            private static final long serialVersionUID = -4657965743173979437L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                actionCompletePreferencesChange(aTarget);
            }
        });

        add(new ExportModalPanel("exportModalPanel", getModel()){
            private static final long serialVersionUID = -468896211970839443L;

            {
                setOutputMarkupId(true);
                setOutputMarkupPlaceholderTag(true);
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                AnnotatorState state = AutomationPage.this.getModelObject();
                setVisible(state.getProject() != null
                        && (SecurityUtil.isAdmin(state.getProject(), repository, state.getUser())
                                || !state.getProject().isDisableExport()));
            }
        });

        Form<Void> gotoPageTextFieldForm = new Form<Void>("gotoPageTextFieldForm");
        gotoPageTextField = new NumberTextField<Integer>("gotoPageText", Model.of(1), Integer.class);
        // FIXME minimum and maximum should be obtained from the annotator state
        gotoPageTextField.setMinimum(1); 
        gotoPageTextField.setOutputMarkupId(true); 
        gotoPageTextFieldForm.add(gotoPageTextField);
        gotoPageTextFieldForm.add(new LambdaAjaxSubmitLink("gotoPageLink", gotoPageTextFieldForm,
                this::actionGotoPage));
        add(gotoPageTextFieldForm);

        add(new LambdaAjaxLink("showOpenDocumentModal", this::actionShowOpenDocumentDialog));
       
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

        add(new LambdaAjaxLink("toggleScriptDirection", this::actionToggleScriptDirection));
        
        add(new GuidelineModalPanel("guidelineModalPanel", getModel()));
        
        add(createOrGetResetDocumentDialog());
        add(createOrGetResetDocumentLink());
        
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
                AnnotatorState state = AutomationPage.this.getModelObject();
                setEnabled(state.getDocument() != null
                        && !repository.isAnnotationFinished(state.getDocument(), state.getUser()));
            }
        });
        finishDocumentIcon = new FinishImage("finishImage", getModel());
        finishDocumentIcon.setOutputMarkupId(true);
        finishDocumentLink.add(finishDocumentIcon);
    }
    
    private Label createPositionInfoLabel()
    {
        return new Label("numberOfPages", new StringResourceModel("PositionInfo.text", 
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
    
    private AnnotationDetailEditorPanel createDetailEditor()
    {
        return new AnnotationDetailEditorPanel("annotationDetailEditorPanel", getModel())
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = getModelObject();
                
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                
                try {
                    annotationEditor.bratRender(aTarget, getEditorCas());
                    annotationEditor.bratSetHighlight(aTarget, state.getSelection().getAnnotation());
                }
                catch (Exception e) {
                    handleException(this, aTarget, e);
                    return;
                }

                try {
                    SuggestionBuilder builder = new SuggestionBuilder(repository,
                            annotationService, userRepository);
                    curationContainer = builder.buildCurationContainer(state);
                    setCurationSegmentBeginEnd(getEditorCas());
                    curationContainer.setBratAnnotatorModel(state);

                    CuratorUtil.updatePanel(aTarget, suggestionView, curationContainer, annotationEditor,
                            repository, annotationSelectionByUsernameAndAddress, curationSegment,
                            annotationService, userRepository);
                    
                    update(aTarget);
                }
                catch (Exception e) {
                    handleException(this, aTarget, e);
                }
            }
            
            @Override
            public void onAnnotate(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = getModelObject();
                
                if(state.isForwardAnnotation()){
                    return;
                }
                AnnotationLayer layer = state.getSelectedAnnotationLayer();
                int address = state.getSelection().getAnnotation().getId();
                try {
                    JCas jCas = getEditorCas();
                    AnnotationFS fs = selectByAddr(jCas, address);

                    for (AnnotationFeature f : annotationService.listAnnotationFeature(layer)) {
                        Type type = CasUtil.getType(fs.getCAS(), layer.getName());
                        Feature feat = type.getFeatureByBaseName(f.getName());
                        if (!automationService.existsMiraTemplate(f)) {
                            continue;
                        }
                        if (!automationService.getMiraTemplate(f).isAnnotateAndRepeat()) {
                            continue;
                        }
                        TagSet tagSet = f.getTagset();
                        boolean isRepeatable = false;
                        // repeat only if the value is in the tagset
                        for (Tag tag : annotationService.listTags(tagSet)) {
                            if (fs.getFeatureValueAsString(feat) == null) {
                                break; // this is new annotation without values
                            }
                            if (fs.getFeatureValueAsString(feat).equals(tag.getName())) {
                                isRepeatable = true;
                                break;
                            }
                        }
                        if (automationService.getMiraTemplate(f) != null && isRepeatable) {

                            if (layer.getType().endsWith(WebAnnoConst.RELATION_TYPE)) {                               
                                AutomationUtil.repeateRelationAnnotation(state, repository,
                                        annotationService, fs, f, fs.getFeatureValueAsString(feat));
                                update(aTarget);
                                break;
                            }
                            else if (layer.getType().endsWith(WebAnnoConst.SPAN_TYPE)) {
                                AutomationUtil.repeateSpanAnnotation(state, repository,
                                        annotationService, fs.getBegin(), fs.getEnd(), f,
                                        fs.getFeatureValueAsString(feat));
                                update(aTarget);
                                break;
                            }

                        }
                    }
                }
                catch (Exception e) {
                    handleException(this, aTarget, e);
                }
            }

            @Override
            protected void onAutoForward(AjaxRequestTarget aTarget)
            {
                try {
                    annotationEditor.bratRender(aTarget, getEditorCas());
                }
                catch (Exception e) {
                    handleException(this, aTarget, e);
                }
            }
            
            @Override
            public void onDelete(AjaxRequestTarget aTarget, AnnotationFS aFS)
            {
                AnnotatorState state = getModelObject();
                AnnotationLayer layer = state.getSelectedAnnotationLayer();
                for (AnnotationFeature f : annotationService.listAnnotationFeature(layer)) {
                    if (!automationService.existsMiraTemplate(f)) {
                        continue;
                    }
                    if (!automationService.getMiraTemplate(f).isAnnotateAndRepeat()) {
                        continue;
                    }
                    try {
                        Type type = CasUtil.getType(aFS.getCAS(), layer.getName());
                        Feature feat = type.getFeatureByBaseName(f.getName());
                        if (layer.getType().endsWith(WebAnnoConst.RELATION_TYPE)) {
                            AutomationUtil.deleteRelationAnnotation(state, repository,
                                    annotationService, aFS, f, aFS.getFeatureValueAsString(feat));
                        }
                        else {
                            AutomationUtil.deleteSpanAnnotation(state, repository,
                                    annotationService, aFS.getBegin(), aFS.getEnd(), f,
                                    aFS.getFeatureValueAsString(feat));
                        }
                        update(aTarget);
                    }
                    catch (Exception e) {
                        handleException(this, aTarget, e);
                    }
                }
            }
        };
    }

    @Override
    protected List<SourceDocument> getListOfDocs()
    {
        AnnotatorState state = getModelObject();
        return new ArrayList<>(
                repository.listAnnotatableDocuments(state.getProject(), state.getUser()).keySet());
    }

    /**
     * for the first time the page is accessed, open the <b>open document dialog</b>
     */
    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead(response);

        if (firstLoad) {
            response.render(OnLoadHeaderItem
                    .forScript("jQuery('#showOpenDocumentModal').trigger('click');"));
            firstLoad = false;
        }
    }
    
    @Override
    protected JCas getEditorCas()
        throws IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        SourceDocument aDocument = getModelObject().getDocument();

        AnnotationDocument annotationDocument = repository.getAnnotationDocument(aDocument,
                state.getUser());

        // If there is no CAS yet for the annotation document, create one.
        return repository.readAnnotationCas(annotationDocument);
    }
    
    private void setCurationSegmentBeginEnd(JCas aEditorCas)
        throws UIMAException, ClassNotFoundException, IOException
    {
        AnnotatorState state = getModelObject();

        final int sentenceAddress = getAddr(selectSentenceAt(aEditorCas, state.getFirstVisibleSentenceBegin(),
                state.getFirstVisibleSentenceEnd()));

        final Sentence sentence = selectByAddr(aEditorCas, Sentence.class, sentenceAddress);
        List<Sentence> followingSentences = selectFollowing(aEditorCas, Sentence.class, sentence, state
                .getPreferences().getWindowSize());
        // Check also, when getting the last sentence address in the display window, if this is the
        // last sentence or the ONLY sentence in the document
        Sentence lastSentenceAddressInDisplayWindow = followingSentences.size() == 0 ? sentence
                : followingSentences.get(followingSentences.size() - 1);
        curationSegment.setBegin(sentence.getBegin());
        curationSegment.setEnd(lastSentenceAddressInDisplayWindow.getEnd());
    }

    private void update(AjaxRequestTarget target)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        CuratorUtil.updatePanel(target, suggestionView, curationContainer, annotationEditor, repository,
                annotationSelectionByUsernameAndAddress, curationSegment, annotationService,
                userRepository);

        gotoPageTextField.setModelObject(getModelObject().getFirstVisibleSentenceNumber());

        target.add(gotoPageTextField);
        target.add(suggestionView);
        target.add(numberOfPages);
    }

    
    private void actionShowOpenDocumentDialog(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        state.getSelection().clear();
        openDocumentsModal.setContent(new OpenModalWindowPanel(openDocumentsModal.getContentId(),
                state, openDocumentsModal, state.getMode()));
        openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = -1746088901018629567L;

            @Override
            public void onClose(AjaxRequestTarget aCallbackTarget)
            {
                if (state.getDocument() == null) {
                    setResponsePage(getApplication().getHomePage());
                    return;
                }

                try {
                    aCallbackTarget.addChildren(getPage(), FeedbackPanel.class);

                    String username = SecurityContextHolder.getContext().getAuthentication()
                            .getName();

                    actionLoadDocument(aCallbackTarget);
                    User user = userRepository.get(username);
                    detailEditor.setEnabled(!FinishImage.isFinished(new Model<AnnotatorState>(state),
                            user, repository));
                    detailEditor.loadFeatureEditorModels(aCallbackTarget);
                }
                catch (Exception e) {
                    handleException(aCallbackTarget, e);
                }
                finishDocumentIcon.setModelObject(state);
                aCallbackTarget.add(finishDocumentIcon.setOutputMarkupId(true));
                aCallbackTarget.appendJavaScript(
                        "Wicket.Window.unloadConfirmation=false;window.location.reload()");
                aCallbackTarget.add(documentNamePanel.setOutputMarkupId(true));
                aCallbackTarget.add(numberOfPages);
            }
        });
        openDocumentsModal.show(aTarget);
    }

    private void actionGotoPage(AjaxRequestTarget aTarget, Form<?> aForm)
        throws Exception
    {
        AnnotatorState state = getModelObject();
        
        JCas editorCas = getEditorCas();
        List<Sentence> sentences = new ArrayList<>(select(editorCas, Sentence.class));
        int selectedSentence = gotoPageTextField.getModelObject();
        selectedSentence = Math.min(selectedSentence, sentences.size());
        gotoPageTextField.setModelObject(selectedSentence);
        
        state.setFirstVisibleSentence(sentences.get(selectedSentence - 1));
        state.setFocusSentenceNumber(selectedSentence);        
        
        aTarget.add(gotoPageTextField);
        
        SuggestionBuilder builder = new SuggestionBuilder(repository, annotationService,
                userRepository);
        curationContainer = builder.buildCurationContainer(state);
        setCurationSegmentBeginEnd(editorCas);
        curationContainer.setBratAnnotatorModel(state);
        update(aTarget);
        
        annotationEditor.bratRender(aTarget, editorCas);
    }

    private void actionToggleScriptDirection(AjaxRequestTarget aTarget)
            throws Exception
    {
        getModelObject().toggleScriptDirection();
        annotationEditor.bratRenderLater(aTarget);

        curationContainer.setBratAnnotatorModel(getModelObject());
        CuratorUtil.updatePanel(aTarget, suggestionView, curationContainer, annotationEditor,
                repository, annotationSelectionByUsernameAndAddress, curationSegment,
                annotationService, userRepository);
    }
    
    private void actionCompletePreferencesChange(AjaxRequestTarget aTarget)
    {
        try {
            AnnotatorState state = getModelObject();
            curationContainer.setBratAnnotatorModel(state);

            JCas editorCas = getEditorCas();
            setCurationSegmentBeginEnd(editorCas);
            
            // The number of visible sentences may have changed - let the state recalculate 
            // the visible sentences 
            Sentence sentence = selectByAddr(editorCas, Sentence.class,
                    state.getFirstVisibleSentenceAddress());
            state.setFirstVisibleSentence(sentence);
            
            update(aTarget);
            aTarget.appendJavaScript(
                    "Wicket.Window.unloadConfirmation = false;window.location.reload()");
            
            // Re-render the whole page because the width of the sidebar may have changed
            aTarget.add(this);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }
    
    private void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        finishDocumentDialog.setConfirmAction((target) -> {
            AnnotatorState state = getModelObject();
            AnnotationDocument annotationDocument = repository.getAnnotationDocument(
                    state.getDocument(), state.getUser());

            annotationDocument.setState(AnnotationDocumentStateTransition.transition(
                    AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED));
            
            // manually update state change!! No idea why it is not updated in the DB
            // without calling createAnnotationDocument(...)
            repository.createAnnotationDocument(annotationDocument);
            
            target.add(finishDocumentIcon);
            target.add(finishDocumentLink);
            target.add(detailEditor);
            target.add(createOrGetResetDocumentLink());
        });
        finishDocumentDialog.show(aTarget);
    }
    
    @Override
    protected void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        LOG.info("BEGIN LOAD_DOCUMENT_ACTION");

        AnnotatorState state = getModelObject();
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);

        state.setUser(user);
        state.setProject(state.getProject());
        state.setDocument(state.getDocument(), getListOfDocs());

        try {
            // Check if there is an annotation document entry in the database. If there is none,
            // create one.
            AnnotationDocument annotationDocument = repository
                    .createOrGetAnnotationDocument(state.getDocument(), user);

            // Read the correction CAS - if it does not exist yet, from the initial CAS
            JCas correctionCas;
            if (repository.existsCorrectionCas(state.getDocument())) {
                correctionCas = repository.readCorrectionCas(state.getDocument());
            }
            else {
                correctionCas = repository.createOrReadInitialCas(state.getDocument());
            }

            // Read the annotation CAS or create an annotation CAS from the initial CAS by stripping
            // annotations
            JCas editorCas;
            if (repository.existsCas(state.getDocument(), user.getUsername())) {
                editorCas = repository.readAnnotationCas(annotationDocument);
            }
            else {
                editorCas = repository.createOrReadInitialCas(state.getDocument());
                // In automation mode, we do not remove the existing annotations from the documents
                // annotationCas = BratAnnotatorUtility.clearJcasAnnotations(annotationCas,
                //        state.getDocument(), user, repository);
            }

            // Update the CASes
            repository.upgradeCas(editorCas.getCas(), annotationDocument);
            repository.upgradeCorrectionCas(correctionCas.getCas(), state.getDocument());

            // After creating an new CAS or upgrading the CAS, we need to save it
            repository.writeAnnotationCas(editorCas.getCas().getJCas(),
                    annotationDocument.getDocument(), user);
            repository.writeCorrectionCas(correctionCas, state.getDocument(), user);

            // (Re)initialize brat model after potential creating / upgrading CAS
            state.clearAllSelections();

            // Load constraints
            state.setConstraints(repository.loadConstraints(state.getProject()));

            // Load user preferences
            PreferencesUtil.loadPreferences(username, repository, annotationService, state,
                    state.getMode());

            // Initialize the visible content
            state.setFirstVisibleSentence(WebAnnoCasUtil.getFirstSentence(editorCas));
            
            // if project is changed, reset some project specific settings
            if (currentprojectId != state.getProject().getId()) {
                state.clearRememberedFeatures();
            }

            currentprojectId = state.getProject().getId();

            LOG.debug("Configured BratAnnotatorModel for user [" + state.getUser() + "] f:["
                    + state.getFirstVisibleSentenceNumber() + "] l:["
                    + state.getLastVisibleSentenceNumber() + "] s:["
                    + state.getFocusSentenceNumber() + "]");

            gotoPageTextField.setModelObject(1);

            setCurationSegmentBeginEnd(editorCas);
            update(aTarget);

            // Re-render the whole page because the font size
            if (aTarget != null) {
                aTarget.add(this);
            }

            // Update document state
            if (state.getDocument().getState().equals(SourceDocumentState.NEW)) {
                state.getDocument().setState(SourceDocumentStateTransition
                        .transition(SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS));
                repository.createSourceDocument(state.getDocument());
            }
            
            // Reset the editor
            detailEditor.reset(aTarget);
            // Populate the layer dropdown box
            detailEditor.loadFeatureEditorModels(editorCas, aTarget);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }

        LOG.info("END LOAD_DOCUMENT_ACTION");
    }
    
    @Override
    protected void actionRefreshDocument(AjaxRequestTarget aTarget, JCas aEditorCas)
    {
        try {
            AnnotatorState state = getModelObject();
            SuggestionBuilder builder = new SuggestionBuilder(repository, annotationService,
                    userRepository);
            curationContainer = builder.buildCurationContainer(state);
            setCurationSegmentBeginEnd(aEditorCas);
            curationContainer.setBratAnnotatorModel(state);
            update(aTarget);
            annotationEditor.bratRender(aTarget, aEditorCas);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }
}
