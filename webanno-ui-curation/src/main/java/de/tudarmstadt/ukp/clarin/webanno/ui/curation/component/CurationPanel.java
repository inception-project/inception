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

import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.selectSentenceAt;
import static org.apache.uima.fit.util.JCasUtil.selectFollowing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.action.ActionContext;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.exception.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.service.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.service.CuratorUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Main Panel for the curation page. It displays a box with the complete text on the left side and a
 * box for a selected sentence on the right side.
 *
 */
public class CurationPanel
    extends Panel
{
    private static final long serialVersionUID = -5128648754044819314L;

    private static final Log LOG = LogFactory.getLog(CurationPanel.class);

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    public final static String CURATION_USER = "CURATION_USER";

    public SuggestionViewPanel suggestionViewPanel;
    private BratAnnotator annotator;
    public AnnotationDetailEditorPanel editor;

    private final WebMarkupContainer sentencesListView;
    private final WebMarkupContainer corssSentAnnoView;

    private ActionContext bModel;

    private int fSn = 0;
    private int lSn = 0;
    private boolean firstLoad = true;
    private boolean annotate = false;
    /**
     * Map for tracking curated spans. Key contains the address of the span, the value contains the
     * username from which the span has been selected
     */
    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress = new HashMap<String, Map<Integer, AnnotationSelection>>();

    public SourceListView curationView;

    ListView<SourceListView> sentenceList;
    ListView<String> crossSentAnnoList;
    List<SourceListView> sourceListModel;

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

    public CurationPanel(String id, final IModel<CurationContainer> cCModel)
    {
        super(id, cCModel);
        WebMarkupContainer sidebarCell = new WebMarkupContainer("sidebarCell") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                aTag.put("width", bModel.getPreferences().getSidebarSize()+"%");
            }
        };
        add(sidebarCell);

        WebMarkupContainer annotationViewCell = new WebMarkupContainer("annotationViewCell") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                aTag.put("width", (100-bModel.getPreferences().getSidebarSize())+"%");
            }
        };
        add(annotationViewCell);
        
        // add container for list of sentences panel
        sentencesListView = new WebMarkupContainer("sentencesListView");
        sentencesListView.setOutputMarkupId(true);
        add(sentencesListView);

        // add container for the list of sentences where annotations exists crossing multiple
        // sentences
        // outside of the current page
        corssSentAnnoView = new WebMarkupContainer("corssSentAnnoView");
        corssSentAnnoView.setOutputMarkupId(true);
        annotationViewCell.add(corssSentAnnoView);

        bModel = getModelObject().getBratAnnotatorModel();

        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();
        CurationUserSegmentForAnnotationDocument curationUserSegmentForAnnotationDocument = new CurationUserSegmentForAnnotationDocument();
        if (bModel != null) {
            curationUserSegmentForAnnotationDocument
                    .setAnnotationSelectionByUsernameAndAddress(annotationSelectionByUsernameAndAddress);
            curationUserSegmentForAnnotationDocument.setBratAnnotatorModel(bModel);
            sentences.add(curationUserSegmentForAnnotationDocument);
        }
        // update source list model only first time.
        sourceListModel = sourceListModel == null ? getModelObject().getCurationViews()
                : sourceListModel;

        suggestionViewPanel = new SuggestionViewPanel("suggestionViewPanel",
                new Model<LinkedList<CurationUserSegmentForAnnotationDocument>>(sentences))
        {
            private static final long serialVersionUID = 2583509126979792202L;
            CurationContainer curationContainer = cCModel.getObject();

            @Override
            public void onChange(AjaxRequestTarget aTarget)
            {
                try {
                    // update begin/end of the curationsegment based on bratAnnotatorModel changes
                    // (like sentence change in auto-scroll mode,....
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    updatePanel(aTarget, curationContainer);
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
        };

        suggestionViewPanel.setOutputMarkupId(true);
        annotationViewCell.add(suggestionViewPanel);

        editor = new AnnotationDetailEditorPanel(
                "annotationDetailEditorPanel", new Model<ActionContext>(bModel))
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget, ActionContext aBModel)
            {
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                annotate = true;

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

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setEnabled(bModel.getDocument()!=null && !repository
                        .getSourceDocument(bModel.getDocument().getProject(),
                                bModel.getDocument().getName()).getState()
                        .equals(SourceDocumentState.CURATION_FINISHED));
            }
        };

        editor.setOutputMarkupId(true);
        sidebarCell.add(editor);

        annotator = new BratAnnotator("mergeView", new Model<ActionContext>(bModel),
                editor)
        {

            private static final long serialVersionUID = 7279648231521710155L;

            @Override
            public void onChange(AjaxRequestTarget aTarget, ActionContext bratAnnotatorModel)
            {
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                try {
                    updatePanel(aTarget, cCModel.getObject());
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
        };
        // reset sentenceAddress and lastSentenceAddress to the orginal once

        annotator.setOutputMarkupId(true);
        annotationViewCell.add(annotator);

        LoadableDetachableModel sentenceDiffModel = new LoadableDetachableModel()
        {

            @Override
            protected Object load()
            {
                int fSN = bModel.getFirstVisibleSentenceNumber();
                int lSN = bModel.getLastVisibleSentenceNumber();

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
            }
        };

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
                Label crossSentAnnoItem = new AjaxLabel("crossAnnoSent", crossSentAnno, click);
                item.add(crossSentAnnoItem);
            }

        };
        crossSentAnnoList.setOutputMarkupId(true);
        corssSentAnnoView.add(crossSentAnnoList);

        LoadableDetachableModel sentencesListModel = new LoadableDetachableModel()
        {

            @Override
            protected Object load()
            {

                return getModelObject().getCurationViews();
            }
        };

        sentenceList = new ListView<SourceListView>("sentencesList", sentencesListModel)
        {
            private static final long serialVersionUID = 8539162089561432091L;

            @Override
            protected void populateItem(ListItem<SourceListView> item)
            {
                final SourceListView curationViewItem = item.getModelObject();

                // ajax call when clicking on a sentence on the left side
                final AbstractDefaultAjaxBehavior click = new AbstractDefaultAjaxBehavior()
                {
                    private static final long serialVersionUID = 5803814168152098822L;

                    @Override
                    protected void respond(AjaxRequestTarget aTarget)
                    {
                        curationView = curationViewItem;
                        fSn = 0;
                        try {
                            JCas jCas = repository.readCurationCas(bModel.getDocument());
                            updateCurationView(cCModel.getObject(), curationViewItem, aTarget, jCas);
                            updatePanel(aTarget, cCModel.getObject());
                            bModel.setFocusSentenceNumber(curationViewItem.getSentenceNumber());

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

                };

                // add subcomponents to the component
                item.add(click);

                String cC = curationViewItem.getSentenceState().getValue();
                // mark current sentence in orange if disagree
                if (curationViewItem.getSentenceNumber() == bModel.getFocusSentenceNumber()) {
                    if (cC != null) {
                        item.add(AttributeModifier.append("class", "current-disagree"));
                    }
                }
                else if (cC != null) {
                    // disagree in range
                    if (curationViewItem.getSentenceNumber() >= fSn
                            && curationViewItem.getSentenceNumber() <= lSn) {
                        item.add(AttributeModifier.append("class", "range-disagree"));
                    }
                    else{
                        item.add(AttributeModifier.append("class", "disagree"));
                    }
                }
                // agree and in range
                else if (curationViewItem.getSentenceNumber() >= fSn
                        && curationViewItem.getSentenceNumber() <= lSn) {
                    item.add(AttributeModifier.append("class", "range-agree"));
                }
                else{
                    item.add(AttributeModifier.append("class", "agree"));
                }
                Label sentenceNumber = new AjaxLabel("sentenceNumber", curationViewItem
                        .getSentenceNumber().toString(), click);
                item.add(sentenceNumber);
            }
        };
        // add subcomponents to the component
        sentenceList.setOutputMarkupId(true);
        sentencesListView.add(sentenceList);
    }

    private void updateCurationView(final CurationContainer curationContainer,
            final SourceListView curationViewItem, AjaxRequestTarget aTarget, JCas jCas)
    {
        Sentence currentSent = BratAjaxCasUtil.getCurrentSentence(jCas, curationViewItem.getBegin(),
                curationViewItem.getEnd());
        bModel.setFirstVisibleSentence(BratAjaxCasUtil.findWindowStartCenteringOnSelection(jCas,
                currentSent, curationViewItem.getBegin(), bModel.getProject(), bModel.getDocument(),
                bModel.getPreferences().getWindowSize()));
        curationContainer.setBratAnnotatorModel(bModel);
        onChange(aTarget);
    }

    protected void onChange(AjaxRequestTarget aTarget)
    {

    }

    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead(response);

        if (firstLoad) {
            firstLoad = false;
        }
        else if (bModel.getProject() != null) {
            // mergeVisualizer.setModelObject(bratAnnotatorModel);
            annotator.setCollection("#" + bModel.getProject().getName() + "/");
            annotator.bratInitRenderLater(response);
        }
    }

    public void updatePanel(AjaxRequestTarget aTarget, CurationContainer aCC)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        JCas jCas = repository.readCurationCas(bModel.getDocument());

        final int sentenceAddress = getAddr(selectSentenceAt(jCas, bModel.getSentenceBeginOffset(),
                bModel.getSentenceEndOffset()));
        bModel.setFirstVisibleSentenceAddress(sentenceAddress);

        final Sentence sentence = selectByAddr(jCas, Sentence.class, sentenceAddress);
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
        Sentence fs = BratAjaxCasUtil.selectSentenceAt(jCas, bModel.getSentenceBeginOffset(),
                bModel.getSentenceEndOffset());
        Sentence ls = BratAjaxCasUtil.getLastSentenceInDisplayWindow(jCas, getAddr(fs), ws);
        fSn = BratAjaxCasUtil.getSentenceNumber(jCas, fs.getBegin());
        lSn = BratAjaxCasUtil.getSentenceNumber(jCas, ls.getBegin());

        sentencesListView.addOrReplace(sentenceList);
        aTarget.add(sentencesListView);

        /*
         * corssSentAnnoView.addOrReplace(crossSentAnnoList); aTarget.add(corssSentAnnoView);
         */
        aTarget.add(suggestionViewPanel);
        if (annotate) {
            annotator.bratRender(aTarget, editor.getCas(bModel));
            annotator.bratSetHighlight(aTarget, bModel.getSelection().getAnnotation());

        }
        else {
            annotator.bratRenderLater(aTarget);
        }
        annotate = false;
        CuratorUtil.updatePanel(aTarget, suggestionViewPanel, aCC, annotator, repository,
                annotationSelectionByUsernameAndAddress, curationView, annotationService,
                userRepository);
    }

    public void resetEditor(AjaxRequestTarget aTarget)
    {
        editor.reset(aTarget);
    }
    public void reloadEditorLayer(AjaxRequestTarget aTarget)
    {
        try {
			editor.refresh(aTarget);
		} catch (BratAnnotationException e) {
			// DO NOTHING
		}
    }
}
