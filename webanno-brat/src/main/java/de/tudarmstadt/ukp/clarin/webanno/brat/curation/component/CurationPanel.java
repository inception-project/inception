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
package de.tudarmstadt.ukp.clarin.webanno.brat.curation.component;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectSentenceAt;
import static org.apache.uima.fit.util.JCasUtil.selectFollowing;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;
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
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.SentenceState;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.CuratorUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Main Panel for the curation page. It displays a box with the complete text on the left side and a
 * box for a selected sentence on the right side.
 *
 * @author Andreas Straninger
 * @author Seid Muhie Yimam
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
    private BratAnnotator mergeVisualizer;
    private AnnotationDetailEditorPanel annotationDetailEditorPanel;

    private BratAnnotatorModel bModel;

    boolean firstLoad = true;

    /**
     * Map for tracking curated spans. Key contains the address of the span, the value contains the
     * username from which the span has been selected
     */
    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress = new HashMap<String, Map<Integer, AnnotationSelection>>();

    public SourceListView curationView;

    ListView<SourceListView> textListView;
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
        // add container for updating ajax
        final WebMarkupContainer textOuterView = new WebMarkupContainer("textOuterView");
        textOuterView.setOutputMarkupId(true);
        add(textOuterView);

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

                    mergeVisualizer.bratRenderLater(aTarget);
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
        add(suggestionViewPanel);

        annotationDetailEditorPanel = new AnnotationDetailEditorPanel(
                "annotationDetailEditorPanel", new Model<BratAnnotatorModel>(bModel))
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
            {
                aTarget.addChildren(getPage(), FeedbackPanel.class);

                try {
                    mergeVisualizer.bratRender(aTarget, getCas(aBModel));
                }
                catch (UIMAException | ClassNotFoundException | IOException e) {
                    LOG.info("Error reading CAS " + e.getMessage());
                    error("Error reading CAS " + e.getMessage());
                    return;
                }

                mergeVisualizer
                        .bratRenderHighlight(aTarget, aBModel.getSelection().getAnnotation());

                mergeVisualizer.onChange(aTarget, aBModel);
                mergeVisualizer.onAnnotate(aTarget, aBModel, aBModel.getSelection().getBegin(),
                        aBModel.getSelection().getEnd());
                if (!aBModel.getSelection().isAnnotate()) {
                    mergeVisualizer.onDelete(aTarget, aBModel, aBModel.getSelection().getBegin(),
                            aBModel.getSelection().getEnd());
                }
            }
        };

        annotationDetailEditorPanel.setOutputMarkupId(true);
        add(annotationDetailEditorPanel);

        mergeVisualizer = new BratAnnotator("mergeView", new Model<BratAnnotatorModel>(bModel),
                annotationDetailEditorPanel)
        {

            private static final long serialVersionUID = 7279648231521710155L;

            @Override
            public void onChange(AjaxRequestTarget aTarget, BratAnnotatorModel bratAnnotatorModel)
            {
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                aTarget.add(suggestionViewPanel);
                try {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
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

        mergeVisualizer.setOutputMarkupId(true);
        add(mergeVisualizer);

        LoadableDetachableModel sentencesListModel = new LoadableDetachableModel()
        {

            @Override
            protected Object load()
            {

                return getModelObject().getCurationViews();
            }
        };

        textListView = new ListView<SourceListView>("textListView", sentencesListModel)
        {
            private static final long serialVersionUID = 8539162089561432091L;
            int currentSentence = 0;

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
                        try {
                            JCas jCas = repository.readCurationCas(bModel.getDocument());
                            updateCurationView(cCModel.getObject(), curationViewItem, aTarget, jCas);
                            updatePanel(aTarget, cCModel.getObject());
                            currentSentence = curationViewItem.getSentenceNumber();

                            textOuterView.addOrReplace(textListView);
                            aTarget.add(textOuterView);
                            aTarget.add(suggestionViewPanel);

                            // Wicket-level rendering of annotator because it becomes visible
                            // after selecting a document
                            aTarget.add(mergeVisualizer);

                            // brat-level initialization and rendering of document
                            mergeVisualizer.bratInit(aTarget);
                            mergeVisualizer.bratRender(aTarget, jCas);

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
                String colorCode = curationViewItem.getSentenceState().getValue();
                if (colorCode != null) {
                    item.add(AttributeModifier.append("style", "background-color: " + colorCode
                            + ";"));
                }

                // mark current sentence in yellow
                if (curationViewItem.getSentenceNumber() == currentSentence) {
                    item.add(AttributeModifier.append("style", "background-color: "
                            + SentenceState.SELECTED.getValue() + ";"));
                }

                // mark border of sentences in the range of windows size in bold
                try {
                    markBorder(item, curationViewItem);
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

                String pad = getPad(curationViewItem);
                Label sentenceNumber = new AjaxLabel("sentenceNumber", pad
                        + curationViewItem.getSentenceNumber().toString(), click);
                item.add(sentenceNumber);
            }
        };
        // add subcomponents to the component
        textListView.setOutputMarkupId(true);
        textOuterView.add(textListView);
    }

    private void markBorder(ListItem<SourceListView> aItem, SourceListView aCurationViewItem)
        throws UIMAException, ClassNotFoundException, IOException
    {
        JCas jCas = repository.readCurationCas(bModel.getDocument());
        int ws = bModel.getPreferences().getWindowSize();
        Sentence fs = BratAjaxCasUtil.selectSentenceAt(jCas, bModel.getSentenceBeginOffset(),
                bModel.getSentenceEndOffset());

        int l = BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(jCas,
                getAddr(fs), ws);
        Sentence ls = (Sentence) selectByAddr(jCas, FeatureStructure.class, l);

        int fsn = BratAjaxCasUtil.getSentenceNumber(jCas, fs.getBegin());
        int lsn = BratAjaxCasUtil.getSentenceNumber(jCas, ls.getBegin());

        if (aCurationViewItem.getSentenceNumber() >= fsn
                && aCurationViewItem.getSentenceNumber() <= lsn) {
            aItem.add(AttributeModifier.append("style", "border-style: "
                    + SentenceState.DOTTED_BORDER.getValue() + ";" + "border-color: "
                    + SentenceState.BORDER_COLOR.getValue() + ";"));
        }
    }

    private void updateCurationView(final CurationContainer curationContainer,
            final SourceListView curationViewItem, AjaxRequestTarget aTarget, JCas jCas)
    {
        int currentSentAddress = BratAjaxCasUtil.getCurrentSentence(jCas,
                curationViewItem.getBegin(), curationViewItem.getEnd()).getAddress();
        bModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas, currentSentAddress,
                curationViewItem.getBegin(), bModel.getProject(), bModel.getDocument(), bModel
                        .getPreferences().getWindowSize()));

        Sentence sentence = selectByAddr(jCas, Sentence.class, bModel.getSentenceAddress());
        bModel.setSentenceBeginOffset(sentence.getBegin());
        bModel.setSentenceEndOffset(sentence.getEnd());

        // TODO .selection.setbegin/getbegin... should be used everywhere ..
        bModel.getSelection().setBegin(sentence.getBegin());
        bModel.getSelection().setEnd(sentence.getEnd());

        curationContainer.setBratAnnotatorModel(bModel);
        onChange(aTarget);
    }

    private String getPad(SourceListView curationViewItem)
    {
        if (curationViewItem.getSentenceNumber() < 10) {
            return "000";
        }
        if (curationViewItem.getSentenceNumber() < 100) {
            return "00";
        }
        if (curationViewItem.getSentenceNumber() < 1000) {
            return "0";
        }
        return "";
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
            mergeVisualizer.setCollection("#" + bModel.getProject().getName() + "/");
            mergeVisualizer.bratInitRenderLater(response);
        }
    }

    public void updatePanel(AjaxRequestTarget aTarget, CurationContainer aCC)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        JCas jCas = repository.readAnnotationCas(bModel.getDocument(), bModel.getUser());

        final int sentenceAddress = getAddr(selectSentenceAt(jCas, bModel.getSentenceBeginOffset(),
                bModel.getSentenceEndOffset()));
        bModel.setSentenceAddress(sentenceAddress);

        final Sentence sentence = selectByAddr(jCas, Sentence.class, sentenceAddress);
        List<Sentence> followingSentences = selectFollowing(jCas, Sentence.class, sentence, bModel
                .getPreferences().getWindowSize());
        // Check also, when getting the last sentence address in the display window, if this is the
        // last sentence or the ONLY sentence in the document
        Sentence lastSentenceAddressInDisplayWindow = followingSentences.size() == 0 ? sentence
                : followingSentences.get(followingSentences.size() - 1);
        curationView.setCurationBegin(sentence.getBegin());
        curationView.setCurationEnd(lastSentenceAddressInDisplayWindow.getEnd());

        CuratorUtil.updatePanel(aTarget, suggestionViewPanel, aCC, mergeVisualizer, repository,
                annotationSelectionByUsernameAndAddress, curationView, annotationService,
                userRepository);
    }
}
