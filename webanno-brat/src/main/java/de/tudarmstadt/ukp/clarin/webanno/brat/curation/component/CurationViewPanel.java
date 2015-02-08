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

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ArcCrossedMultipleSentenceException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.BratCurationVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.NoOriginOrTargetAnnotationSelectedException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * A {@link MarkupContainer} for either curation users' sentence annotation (for
 * the lower panel) or the automated annotations
 *
 * @author Seid Muhie Yimam
 *
 */
public class CurationViewPanel extends WebMarkupContainer {
    private static final long serialVersionUID = 8736268179612831795L;
    private final ListView<CurationUserSegmentForAnnotationDocument> sentenceListView;
    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    /**
     * Data models for {@link BratAnnotator}
     * 
     * @param aModel the model.
     */
    public void setModel(
            IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> aModel) {
        setDefaultModel(aModel);
    }

    public void setModelObject(
            LinkedList<CurationUserSegmentForAnnotationDocument> aModel) {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> getModel() {
        return (IModel<LinkedList<CurationUserSegmentForAnnotationDocument>>) getDefaultModel();
    }

    @SuppressWarnings("unchecked")
    public LinkedList<CurationUserSegmentForAnnotationDocument> getModelObject() {
        return (LinkedList<CurationUserSegmentForAnnotationDocument>) getDefaultModelObject();
    }

    public CurationViewPanel(String id,
            IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> aModel) {
        super(id, aModel);
        // update list of brat embeddings
        sentenceListView = new ListView<CurationUserSegmentForAnnotationDocument>(
                "sentenceListView", aModel) {
            private static final long serialVersionUID = -5389636445364196097L;

            @Override
            protected void populateItem(
                    ListItem<CurationUserSegmentForAnnotationDocument> item2) {
                final CurationUserSegmentForAnnotationDocument curationUserSegment = item2
                        .getModelObject();
                BratCurationVisualizer curationVisualizer = new BratCurationVisualizer(
                        "sentence",
                        new Model<CurationUserSegmentForAnnotationDocument>(
                                curationUserSegment)) {
                    private static final long serialVersionUID = -1205541428144070566L;

                    /**
                     * Method is called, if user has clicked on a span or an arc
                     * in the sentence panel. The span or arc respectively is
                     * identified and copied to the merge cas.
                     */
                    @Override
                    protected void onSelectAnnotationForMerge(
                            AjaxRequestTarget aTarget) {
                        // TODO: chain the error from this component up in the
                        // CurationPage
                        // or CorrectionPage
                        if (BratAnnotatorUtility.isDocumentFinished(repository,
                                curationUserSegment.getBratAnnotatorModel())) {
                            aTarget.appendJavaScript("alert('This document is already closed."
                                    + " Please ask admin to re-open')");
                        } else {
                            try {
                                final IRequestParameters request = getRequest()
                                        .getPostParameters();
                                String username = SecurityContextHolder
                                        .getContext().getAuthentication()
                                        .getName();

                                User user = repository.getUser(username);

                                SourceDocument sourceDocument = curationUserSegment
                                        .getBratAnnotatorModel().getDocument();
                                JCas annotationJCas = null;

                                annotationJCas = (curationUserSegment
                                        .getBratAnnotatorModel().getMode()
                                        .equals(Mode.AUTOMATION) || curationUserSegment
                                        .getBratAnnotatorModel().getMode()
                                        .equals(Mode.CORRECTION)) ? repository
                                        .getAnnotationDocumentContent(repository
                                                .getAnnotationDocument(
                                                        sourceDocument, user))
                                        : repository
                                                .getCurationDocumentContent(sourceDocument);
                                StringValue action = request
                                        .getParameterValue("action");
                                // check if clicked on a span
                                if (!action.isEmpty()
                                        && action.toString().equals(
                                                "selectSpanForMerge")) {
                                    mergeSpan(request, curationUserSegment,
                                            annotationJCas, repository,
                                            annotationService);
                                }
                                // check if clicked on an arc
                                else if (!action.isEmpty()
                                        && action.toString().equals(
                                                "selectArcForMerge")) {
                                    // add span for merge
                                    // get information of the span clicked
                                    mergeArc(request, curationUserSegment,
                                            annotationJCas, repository,
                                            annotationService);
                                }
                                onChange(aTarget);
                            } catch (UIMAException e) {
                                aTarget.addChildren(getPage(), FeedbackPanel.class);
                                error(ExceptionUtils.getRootCauseMessage(e));
                            } catch (ClassNotFoundException e) {
                                aTarget.addChildren(getPage(), FeedbackPanel.class);
                                error(ExceptionUtils.getRootCauseMessage(e));
                            } catch (DataRetrievalFailureException e) {
                                aTarget.addChildren(getPage(), FeedbackPanel.class);
                                error(ExceptionUtils.getRootCauseMessage(e));
                            } catch (IOException e) {
                                aTarget.addChildren(getPage(), FeedbackPanel.class);
                                error(e.getMessage());
                            } catch (BratAnnotationException e) {
                                aTarget.addChildren(getPage(), FeedbackPanel.class);
                                error(e.getMessage());
                            }
                        }
                    }
                };
                curationVisualizer.setOutputMarkupId(true);
                item2.add(curationVisualizer);
            }
        };
        sentenceListView.setOutputMarkupId(true);
        add(sentenceListView);
    }

    protected void onChange(AjaxRequestTarget aTarget) {
        // Overriden in curationPanel
    }

    protected void isCorrection(AjaxRequestTarget aTarget) {
        // Overriden in curationPanel
    }

    private void mergeSpan(IRequestParameters aRequest,
            CurationUserSegmentForAnnotationDocument aCurationUserSegment,
            JCas aJcas, RepositoryService aRepository,
            AnnotationService aAnnotationService)
            throws BratAnnotationException, UIMAException,
            ClassNotFoundException, IOException {
        Integer address = aRequest.getParameterValue("id").toInteger();
        String spanType = removePrefix(aRequest.getParameterValue("type")
                .toString());

        String username = aCurationUserSegment.getUsername();
        AnnotationSelection annotationSelection = aCurationUserSegment
                .getAnnotationSelectionByUsernameAndAddress().get(username)
                .get(address);

        if (annotationSelection == null) {
            return;
        }

        SourceDocument sourceDocument = aCurationUserSegment
                .getBratAnnotatorModel().getDocument();

        AnnotationDocument clickedAnnotationDocument = null;
        List<AnnotationDocument> annotationDocuments = aRepository
                .listAnnotationDocuments(sourceDocument);
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            if (annotationDocument.getUser().equals(username)) {
                clickedAnnotationDocument = annotationDocument;
                break;
            }
        }

        createSpan(spanType, aCurationUserSegment.getBratAnnotatorModel(),
                aJcas, clickedAnnotationDocument, address, aRepository,
                aAnnotationService);
    }

    private void createSpan(String spanType, BratAnnotatorModel aBratAnnotatorModel,
            JCas aMergeJCas, AnnotationDocument aAnnotationDocument, int aAddress,
            RepositoryService aRepository, AnnotationService aAnnotationService)
        throws IOException, UIMAException, ClassNotFoundException, BratAnnotationException
    {
        JCas clickedJCas = getJCas(aBratAnnotatorModel, aAnnotationDocument);

        AnnotationFS fsClicked = selectByAddr(clickedJCas, aAddress);

        long layerId = TypeUtil.getLayerId(spanType);
        AnnotationLayer layer = annotationService.getLayer(layerId);
        SpanAdapter adapter = (SpanAdapter) getAdapter(layer);

        // Add annotation - we set no feature values yet.
        int selectedSpanId = adapter.add(aMergeJCas, fsClicked.getBegin(), fsClicked.getEnd(),
                null, null);
        
        // Set the feature values
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(layer)) {
            if (feature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
                continue;
            }
            if (feature.isEnabled()) {
                Feature uimaFeature = fsClicked.getType().getFeatureByBaseName(feature.getName());
                adapter.updateFeature(aMergeJCas, feature, selectedSpanId,
                        fsClicked.getFeatureValueAsString(uimaFeature));
            }
        }
        repository.updateJCas(aBratAnnotatorModel.getMode(),
                aBratAnnotatorModel.getDocument(),
                aBratAnnotatorModel.getUser(), aMergeJCas);

        // update timestamp
        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(clickedJCas,
                fsClicked.getBegin());
        aBratAnnotatorModel.getDocument().setSentenceAccessed(sentenceNumber);
        repository.updateTimeStamp(aBratAnnotatorModel.getDocument(),
                aBratAnnotatorModel.getUser(), aBratAnnotatorModel.getMode());

        if (aBratAnnotatorModel.isScrollPage()) {
            int address = BratAjaxCasUtil.selectSentenceAt(clickedJCas,
                    aBratAnnotatorModel.getSentenceBeginOffset(),
                    aBratAnnotatorModel.getSentenceEndOffset()).getAddress();
            aBratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil
                    .getSentenceBeginAddress(clickedJCas, address,
                            fsClicked.getBegin(),
                            aBratAnnotatorModel.getProject(),
                            aBratAnnotatorModel.getDocument(),
                            aBratAnnotatorModel.getWindowSize()));

            Sentence sentence = selectByAddr(clickedJCas, Sentence.class,
                    aBratAnnotatorModel.getSentenceAddress());
            aBratAnnotatorModel.setSentenceBeginOffset(sentence.getBegin());
            aBratAnnotatorModel.setSentenceEndOffset(sentence.getEnd());
        }
    }

    private void mergeArc(IRequestParameters aRequest,
            CurationUserSegmentForAnnotationDocument aCurationUserSegment,
            JCas aJcas, RepositoryService repository,
            AnnotationService annotationService)
            throws NoOriginOrTargetAnnotationSelectedException,
            ArcCrossedMultipleSentenceException, BratAnnotationException,
            IOException {
        Integer addressOriginClicked = aRequest.getParameterValue(
                "originSpanId").toInteger();
        Integer addressTargetClicked = aRequest.getParameterValue(
                "targetSpanId").toInteger();
        String arcType = removePrefix(aRequest.getParameterValue("type")
                .toString());
        Integer fsArcaddress = aRequest.getParameterValue(
                "arcId").toInteger();

        // add span for merge
        // get information of the span clicked
        String username = aCurationUserSegment.getUsername();
        SourceDocument sourceDocument = aCurationUserSegment
                .getBratAnnotatorModel().getDocument();

        AnnotationSelection annotationSelectionOrigin = aCurationUserSegment
                .getAnnotationSelectionByUsernameAndAddress().get(username)
                .get(addressOriginClicked);
        AnnotationSelection annotationSelectionTarget = aCurationUserSegment
                .getAnnotationSelectionByUsernameAndAddress().get(username)
                .get(addressTargetClicked);

        Integer addressOrigin = annotationSelectionOrigin
                .getAddressByUsername().get(username);
        Integer addressTarget = annotationSelectionTarget
                .getAddressByUsername().get(username);

        if (annotationSelectionOrigin == null
                || annotationSelectionTarget == null) {
            return;
        }

        AnnotationDocument clickedAnnotationDocument = null;
        List<AnnotationDocument> annotationDocuments = repository
                .listAnnotationDocuments(sourceDocument);
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            if (annotationDocument.getUser().equals(username)) {
                clickedAnnotationDocument = annotationDocument;
                break;
            }
        }

        JCas clickedJCas = null;
        try {
            clickedJCas = getJCas(aCurationUserSegment.getBratAnnotatorModel(),
                    clickedAnnotationDocument);
        } catch (IOException e1) {
            throw new IOException();
        }
        AnnotationFS originFsClicked = selectByAddr(clickedJCas, addressOrigin);
        AnnotationFS targetFsClicked = selectByAddr(clickedJCas, addressTarget);

        AnnotationFS originFs = BratAjaxCasUtil.selectSingleFsAt(aJcas,
                originFsClicked.getType(), originFsClicked.getBegin(),
                originFsClicked.getEnd());

        AnnotationFS targetFs = BratAjaxCasUtil.selectSingleFsAt(aJcas,
                targetFsClicked.getType(), targetFsClicked.getBegin(),
                targetFsClicked.getEnd());
        try {
            if (originFs == null | targetFs == null) {
                throw new NoOriginOrTargetAnnotationSelectedException(
                        "Either origin or target annotations not selected");
            }

            long layerId = TypeUtil.getLayerId(arcType);

            AnnotationLayer layer = annotationService.getLayer(layerId);
            AnnotationFS fsClicked = selectByAddr(clickedJCas, fsArcaddress);
            ArcAdapter adapter = (ArcAdapter) getAdapter(layer);

            // Add annotation - we set no feature values yet.
            int selectedSpanId = adapter.add(originFs, targetFs, aJcas,
                    aCurationUserSegment.getBratAnnotatorModel(), null, null);
            
            // Set the feature values
            for (AnnotationFeature feature : annotationService.listAnnotationFeature(layer)) {
                if (feature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
                    continue;
                }
                if (feature.isEnabled()) {
                    Feature uimaFeature = fsClicked.getType().getFeatureByBaseName(feature.getName());
                    adapter.updateFeature(aJcas, feature, selectedSpanId,
                            fsClicked.getFeatureValueAsString(uimaFeature));
                }
            }

            repository.updateJCas(aCurationUserSegment.getBratAnnotatorModel()
                    .getMode(), aCurationUserSegment.getBratAnnotatorModel()
                    .getDocument(), aCurationUserSegment
                    .getBratAnnotatorModel().getUser(), aJcas);

            // update timestamp
            int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(clickedJCas,
                    originFs.getBegin());
            aCurationUserSegment.getBratAnnotatorModel().getDocument()
                    .setSentenceAccessed(sentenceNumber);
            repository.updateTimeStamp(aCurationUserSegment
                    .getBratAnnotatorModel().getDocument(),
                    aCurationUserSegment.getBratAnnotatorModel().getUser(),
                    aCurationUserSegment.getBratAnnotatorModel().getMode());
        } catch (IOException e) {
            throw new IOException();
        }

        if (aCurationUserSegment.getBratAnnotatorModel().isScrollPage()) {
            int address = BratAjaxCasUtil.selectSentenceAt(
                    aJcas,
                    aCurationUserSegment.getBratAnnotatorModel()
                            .getSentenceBeginOffset(),
                    aCurationUserSegment.getBratAnnotatorModel()
                            .getSentenceEndOffset()).getAddress();
            aCurationUserSegment.getBratAnnotatorModel().setSentenceAddress(
                    BratAjaxCasUtil.getSentenceBeginAddress(aJcas, address,
                            originFs.getBegin(), aCurationUserSegment
                                    .getBratAnnotatorModel().getProject(),
                            aCurationUserSegment.getBratAnnotatorModel()
                                    .getDocument(), aCurationUserSegment
                                    .getBratAnnotatorModel().getWindowSize()));
            Sentence sentence = selectByAddr(aJcas, Sentence.class,
                    aCurationUserSegment.getBratAnnotatorModel()
                            .getSentenceAddress());
            aCurationUserSegment.getBratAnnotatorModel()
                    .setSentenceBeginOffset(sentence.getBegin());
            aCurationUserSegment.getBratAnnotatorModel().setSentenceEndOffset(
                    sentence.getEnd());
        }
    }

    private JCas getJCas(BratAnnotatorModel aModel, AnnotationDocument aDocument)
            throws IOException {
        try {
            if (aModel.getMode().equals(Mode.AUTOMATION)
                    || aModel.getMode().equals(Mode.CORRECTION)) {
                return repository.getCorrectionDocumentContent(aModel
                        .getDocument());
            } else {
                return repository.getAnnotationDocumentContent(aDocument);
            }
        } catch (UIMAException e) {
            throw new IOException(e);
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    /**
     * Removes a prefix that is added to brat visualization for different color
     * coded purpose.
     */
    private static String removePrefix(String aType) {
        return aType.replace("_(" + AnnotationState.AGREE.name() + ")", "")
                .replace("_(" + AnnotationState.USE.name() + ")", "")
                .replace("_(" + AnnotationState.DISAGREE.name() + ")", "")
                .replace("_(" + AnnotationState.DO_NOT_USE.name() + ")", "")
                .replace("_(" + AnnotationState.NOT_SUPPORTED.name() + ")", "");
    }
}
