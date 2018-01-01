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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentenceAt;
import static org.apache.uima.fit.util.JCasUtil.selectFollowing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotationEditor;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Main Panel for the curation page. It displays a box with the complete text on the left side and a
 * box for a selected sentence on the right side.
 */
public class CurationPanel
    extends Panel
{
    private static final long serialVersionUID = -5128648754044819314L;

    private static final Logger LOG = LoggerFactory.getLogger(CurationPanel.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;

    public SuggestionViewPanel suggestionViewPanel;
    private AnnotationEditorBase annotationEditor;
    public AnnotationDetailEditorPanel editor;

    private final WebMarkupContainer sentencesListView;
    private final WebMarkupContainer corssSentAnnoView;

    private AnnotatorState bModel;

    private int fSn = 0;
    private int lSn = 0;
    private boolean firstLoad = true;
    private boolean annotate = false;
    /**
     * Map for tracking curated spans. Key contains the address of the span, the value contains the
     * username from which the span has been selected
     */
    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress =
            new HashMap<>();

    public SourceListView curationView;

    ListView<String> crossSentAnnoList;
    List<SourceListView> sourceListModel;

    // CurationContainer curationContainer;

    public CurationPanel(String id, final IModel<CurationContainer> cCModel)
    {
        super(id, cCModel);
        
        setOutputMarkupId(true);
        
        WebMarkupContainer sidebarCell = new WebMarkupContainer("rightSidebar");    
        sidebarCell.setOutputMarkupId(true);
        // Override sidebar width from preferences
        sidebarCell.add(new AttributeModifier("style", LambdaModel.of(() -> String
                .format("flex-basis: %d%%;", bModel.getPreferences().getSidebarSize()))));
        add(sidebarCell);
        
        // add container for list of sentences panel
        sentencesListView = new WebMarkupContainer("sentencesListView");
        sentencesListView.setOutputMarkupId(true);
        add(sentencesListView);
    
        // add container for the list of sentences where annotations exists crossing multiple
        // sentences outside of the current page
        corssSentAnnoView = new WebMarkupContainer("corssSentAnnoView");
        corssSentAnnoView.setOutputMarkupId(true);
        add(corssSentAnnoView);
    
        bModel = getModelObject().getBratAnnotatorModel();
    
        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<>();
        CurationUserSegmentForAnnotationDocument curationUserSegmentForAnnotationDocument = 
                new CurationUserSegmentForAnnotationDocument();
        if (bModel != null) {
            curationUserSegmentForAnnotationDocument.setSelectionByUsernameAndAddress(
                    annotationSelectionByUsernameAndAddress);
            curationUserSegmentForAnnotationDocument.setBratAnnotatorModel(bModel);
            sentences.add(curationUserSegmentForAnnotationDocument);
        }
        // update source list model only first time.
        sourceListModel = sourceListModel == null ? getModelObject().getCurationViews()
                : sourceListModel;
    
        suggestionViewPanel = new SuggestionViewPanel("suggestionViewPanel",
                new Model<>(sentences))
        {
            private static final long serialVersionUID = 2583509126979792202L;
            CurationContainer curationContainer = cCModel.getObject();
    
            @Override
            public void onChange(AjaxRequestTarget aTarget)
            {
                try {
                    // update begin/end of the curationsegment based on bratAnnotatorModel changes
                    // (like sentence change in auto-scroll mode,....
                    aTarget.addChildren(getPage(), IFeedback.class);
                    CurationPanel.this.updatePanel(aTarget, curationContainer);
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException | AnnotationException | IOException e) {
                    error("Error: " + e.getMessage());
                }
            }
        };
    
        suggestionViewPanel.setOutputMarkupId(true);
        add(suggestionViewPanel);
    
        editor = new AnnotationDetailEditorPanel(
                "annotationDetailEditorPanel", new Model<>(bModel))
        {
            private static final long serialVersionUID = 2857345299480098279L;
    
            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                aTarget.addChildren(getPage(), IFeedback.class);
                annotate = true;
    
                try {
                    updatePanel(aTarget, cCModel.getObject());
                }
                catch (UIMAException e) {
                    LOG.error("Error: " + e.getMessage(), e);
                    error("Error: " + ExceptionUtils.getRootCauseMessage(e));
                }
                catch (Exception e) {
                    LOG.error("Error: " + e.getMessage(), e);
                    error("Error: " + e.getMessage());
                }
            }
    
            @Override
            protected void onAutoForward(AjaxRequestTarget aTarget)
            {
                annotationEditor.requestRender(aTarget);
            }
    
            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setEnabled(bModel.getDocument() != null && !documentService
                        .getSourceDocument(bModel.getDocument().getProject(),
                                bModel.getDocument().getName())
                        .getState().equals(SourceDocumentState.CURATION_FINISHED));
            }
        };
        sidebarCell.add(editor);
    
        annotationEditor = new BratAnnotationEditor("mergeView", new Model<>(bModel), editor,
            this::getEditorCas);
        annotationEditor.setHighlightEnabled(false);
        // reset sentenceAddress and lastSentenceAddress to the orginal once
        add(annotationEditor);
    
        IModel<List<String>> sentenceDiffModel = LambdaModel.of(() -> {
            int fSN = bModel.getFirstVisibleUnitIndex();
            int lSN = bModel.getLastVisibleUnitIndex();

            List<String> crossSentAnnos = new ArrayList<>();
            if (SuggestionBuilder.crossSentenceLists != null) {
                for (int sn : SuggestionBuilder.crossSentenceLists.keySet()) {
                    if (sn >= fSN && sn <= lSN) {
                        List<Integer> cr = new ArrayList<>();
                        for (int c : SuggestionBuilder.crossSentenceLists.get(sn)) {
                            if (c < fSN || c > lSN) {
                                cr.add(c);
                            }
                        }
                        if (!cr.isEmpty()) {
                            crossSentAnnos.add(sn + "-->" + cr);
                        }
                    }
                }
            }

            return crossSentAnnos;
        });
    
        crossSentAnnoList = new ListView<String>("crossSentAnnoList", sentenceDiffModel)
        {
            private static final long serialVersionUID = 8539162089561432091L;
    
            @Override
            protected void populateItem(ListItem<String> item)
            {
                String crossSentAnno = item.getModelObject();
    
                // ajax call when clicking on a sentence on the left side
                final AbstractDefaultAjaxBehavior click = new AbstractDefaultAjaxBehavior()
                {
                    private static final long serialVersionUID = 5803814168152098822L;
    
                    @Override
                    protected void respond(AjaxRequestTarget aTarget)
                    {
                        // Expand curation view
                    }
                };
    
                // add subcomponents to the component
                item.add(click);
                item.add(new AjaxLabel("crossAnnoSent", crossSentAnno, click));
            }
    
        };
        crossSentAnnoList.setOutputMarkupId(true);
        corssSentAnnoView.add(crossSentAnnoList);
    
        // add subcomponents to the component
        sentencesListView.add(new ListView<SourceListView>("sentencesList",
               LambdaModel.of(() -> getModelObject().getCurationViews()))
        {
            private static final long serialVersionUID = 8539162089561432091L;
    
            @Override
            protected void populateItem(ListItem<SourceListView> item)
            {
                item.add(new SentenceLink("sentenceNumber", item.getModel()));
            }
        });
    }
    
    public class SentenceLink extends AjaxLink<SourceListView>
    {
        private static final long serialVersionUID = 4558300090461815010L;

        public SentenceLink(String aId, IModel<SourceListView> aModel)
        {
            super(aId, aModel);
            setBody(Model.of(aModel.getObject().getSentenceNumber().toString()));
        }
        
        @Override
        protected void onComponentTag(ComponentTag aTag)
        {
            super.onComponentTag(aTag);
            
            final SourceListView curationViewItem = getModelObject();
            
            // Is in focus?
            if (curationViewItem.getSentenceNumber() == bModel.getFocusUnitIndex()) {
                aTag.append("class", "current", " ");
            }
            
            // Agree or disagree?
            String cC = curationViewItem.getSentenceState().getValue();
            if (cC != null) {
                aTag.append("class", "disagree", " ");
            }
            else {
                aTag.append("class", "agree", " ");
            }
            
            // In range or not?
            if (curationViewItem.getSentenceNumber() >= fSn
                    && curationViewItem.getSentenceNumber() <= lSn) {
                aTag.append("class", "in-range", " ");
            }
            else {
                aTag.append("class", "out-range", " ");
            }
        }
        
        @Override
        protected void onAfterRender()
        {
            super.onAfterRender();
            
            // The sentence list is refreshed using AJAX. Unfortunately, the renderHead() method
            // of the AjaxEventBehavior created by AjaxLink does not seem to be called by Wicket
            // during an AJAX rendering, causing the sentence links to loose their functionality.
            // Here, we ensure that the callback scripts are attached to the sentence links even
            // during AJAX updates.
            if (isEnabledInHierarchy()) {
                RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(_target -> {
                    for (AjaxEventBehavior b : getBehaviors(AjaxEventBehavior.class)) {
                        _target.appendJavaScript(b.getCallbackScript());
                    }
                });
            }
        }
        
        @Override
        public void onClick(AjaxRequestTarget aTarget)
        {
            final SourceListView curationViewItem = getModelObject();
            curationView = curationViewItem;
            fSn = 0;
            try {
                JCas jCas = curationDocumentService.readCurationCas(bModel.getDocument());
                updateCurationView(CurationPanel.this.getModelObject(), curationViewItem, aTarget,
                        jCas);
                updatePanel(aTarget, CurationPanel.this.getModelObject());
                bModel.setFocusUnitIndex(curationViewItem.getSentenceNumber());
            }
            catch (UIMAException e) {
                error("Error: " + ExceptionUtils.getRootCauseMessage(e));
            }
            catch (ClassNotFoundException | AnnotationException | IOException e) {
                error("Error: " + e.getMessage());
            }
        }
    }

    public void setModel(IModel<CurationContainer> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(CurationContainer aModel)
    {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<CurationContainer> getModel()
    {
        return (IModel<CurationContainer>) getDefaultModel();
    }

    public CurationContainer getModelObject()
    {
        return (CurationContainer) getDefaultModelObject();
    }

    private void updateCurationView(final CurationContainer curationContainer,
            final SourceListView curationViewItem, AjaxRequestTarget aTarget, JCas jCas)
    {
        Sentence currentSent = WebAnnoCasUtil.getCurrentSentence(jCas, curationViewItem.getBegin(),
                curationViewItem.getEnd());
        bModel.setFirstVisibleUnit(WebAnnoCasUtil.findWindowStartCenteringOnSelection(jCas,
                currentSent, curationViewItem.getBegin(), bModel.getProject(), bModel.getDocument(),
                bModel.getPreferences().getWindowSize()));
        curationContainer.setBratAnnotatorModel(bModel);
        onChange(aTarget);
    }

    protected void onChange(AjaxRequestTarget aTarget)
    {

    }

    protected JCas getEditorCas()
        throws IOException
    {
        if (bModel.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        return curationDocumentService.readCurationCas(bModel.getDocument());
    }

    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead(response);

        if (firstLoad) {
            firstLoad = false;
        }
    }

    public void updatePanel(AjaxRequestTarget aTarget, CurationContainer aCC)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        JCas jCas = curationDocumentService.readCurationCas(bModel.getDocument());

        final Sentence sentence = selectSentenceAt(jCas, bModel.getFirstVisibleUnitBegin(),
                bModel.getFirstVisibleUnitEnd());
        bModel.setFirstVisibleUnit(sentence);

        List<Sentence> followingSentences = selectFollowing(jCas, Sentence.class, sentence, bModel
                .getPreferences().getWindowSize());
        // Check also, when getting the last sentence address in the display window, if this is the
        // last sentence or the ONLY sentence in the document
        Sentence lastSentenceAddressInDisplayWindow = followingSentences.size() == 0 ? sentence
                : followingSentences.get(followingSentences.size() - 1);
        if (curationView == null) {
            curationView = new SourceListView();
        }
        curationView.setCurationBegin(sentence.getBegin());
        curationView.setCurationEnd(lastSentenceAddressInDisplayWindow.getEnd());

        int ws = bModel.getPreferences().getWindowSize();
        Sentence fs = WebAnnoCasUtil.selectSentenceAt(jCas, bModel.getFirstVisibleUnitBegin(),
                bModel.getFirstVisibleUnitEnd());
        Sentence ls = WebAnnoCasUtil.getLastSentenceInDisplayWindow(jCas, getAddr(fs), ws);
        fSn = WebAnnoCasUtil.getSentenceNumber(jCas, fs.getBegin());
        lSn = WebAnnoCasUtil.getSentenceNumber(jCas, ls.getBegin());

        aTarget.add(sentencesListView);

        /*
         * corssSentAnnoView.addOrReplace(crossSentAnnoList); aTarget.add(corssSentAnnoView);
         */
        aTarget.add(suggestionViewPanel);
        annotationEditor.requestRender(aTarget);
        
        annotate = false;
        suggestionViewPanel.updatePanel(aTarget, aCC, annotationEditor,
                annotationSelectionByUsernameAndAddress, curationView);
    }

    // CurationContainer curationContainer;
    
    /**
     * Class for combining an on click ajax call and a label
     */
    class AjaxLabel
        extends Label
    {
    
        private static final long serialVersionUID = -4528869530409522295L;
        private AbstractAjaxBehavior click;
    
        public AjaxLabel(String id, String label, AbstractAjaxBehavior click)
        {
            super(id, label);
            this.click = click;
        }
    
        @Override
        public void onComponentTag(ComponentTag tag)
        {
            // add onclick handler to the browser
            // if clicked in the browser, the function
            // click.response(AjaxRequestTarget target) is called on the server side
            tag.put("ondblclick", "Wicket.Ajax.get({'u':'" + click.getCallbackUrl() + "'})");
            tag.put("onclick", "Wicket.Ajax.get({'u':'" + click.getCallbackUrl() + "'})");
        }
    
    }
}
