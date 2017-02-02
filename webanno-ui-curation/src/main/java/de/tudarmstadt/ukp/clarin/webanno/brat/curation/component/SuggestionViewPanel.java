/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.brat.curation.component;

import static de.tudarmstadt.ukp.clarin.webanno.brat.adapter.TypeUtil.getAdapter;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.MergeCas;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.brat.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.adapter.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.BratSuggestionVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * A {@link MarkupContainer} for either curation users' sentence annotation (for the lower panel) or
 * the automated annotations
 */
public class SuggestionViewPanel
        extends WebMarkupContainer
{
    private static final long serialVersionUID = 8736268179612831795L;
    private final ListView<CurationUserSegmentForAnnotationDocument> sentenceListView;
    @SpringBean(name = "documentRepository") private RepositoryService repository;

    @SpringBean(name = "annotationService") private AnnotationService annotationService;

    @SpringBean(name = "userRepository") private UserDao userRepository;

    /**
     * Data models for {@link BratAnnotator}
     *
     * @param aModel the model.
     */
    public void setModel(IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(LinkedList<CurationUserSegmentForAnnotationDocument> aModel)
    {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked") public IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> getModel()
    {
        return (IModel<LinkedList<CurationUserSegmentForAnnotationDocument>>) getDefaultModel();
    }

    @SuppressWarnings("unchecked") public LinkedList<CurationUserSegmentForAnnotationDocument> getModelObject()
    {
        return (LinkedList<CurationUserSegmentForAnnotationDocument>) getDefaultModelObject();
    }

    public SuggestionViewPanel(String id,
            IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> aModel)
    {
        super(id, aModel);
        // update list of brat embeddings
        sentenceListView = new ListView<CurationUserSegmentForAnnotationDocument>(
                "sentenceListView", aModel)
        {
            private static final long serialVersionUID = -5389636445364196097L;

            @Override protected void populateItem(
                    ListItem<CurationUserSegmentForAnnotationDocument> item2)
            {
                final CurationUserSegmentForAnnotationDocument curationUserSegment = item2
                        .getModelObject();
                BratSuggestionVisualizer curationVisualizer = new BratSuggestionVisualizer(
                        "sentence",
                        new Model<CurationUserSegmentForAnnotationDocument>(curationUserSegment))
                {
                    private static final long serialVersionUID = -1205541428144070566L;

                    /**
                     * Method is called, if user has clicked on a span or an arc in the sentence
                     * panel. The span or arc respectively is identified and copied to the merge
                     * cas.
                     * @throws IOException
                     * @throws ClassNotFoundException
                     * @throws UIMAException
                     * @throws BratAnnotationException
                     */
                    @Override protected void onSelectAnnotationForMerge(AjaxRequestTarget aTarget)
                            throws UIMAException, ClassNotFoundException, IOException,
                            BratAnnotationException
                    {
                        // TODO: chain the error from this component up in the
                        // CurationPage
                        // or CorrectionPage
                        if (BratAnnotatorUtility.isDocumentFinished(repository,
                                curationUserSegment.getBratAnnotatorModel())) {
                            aTarget.appendJavaScript("alert('This document is already closed."
                                    + " Please ask admin to re-open')");
                            return;
                        }
                        final IRequestParameters request = getRequest().getPostParameters();
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();

                        User user = userRepository.get(username);

                        SourceDocument sourceDocument = curationUserSegment.getBratAnnotatorModel()
                                .getDocument();
                        JCas annotationJCas = null;

                        annotationJCas = (curationUserSegment.getBratAnnotatorModel().getMode()
                                .equals(Mode.AUTOMATION) || curationUserSegment
                                .getBratAnnotatorModel().getMode().equals(Mode.CORRECTION)) ?
                                repository.readAnnotationCas(
                                        repository.getAnnotationDocument(sourceDocument, user)) :
                                repository.readCurationCas(sourceDocument);
                        StringValue action = request.getParameterValue("action");
                        // check if clicked on a span
                        if (!action.isEmpty() && action.toString().equals("selectSpanForMerge")) {                    
                            mergeSpan(request, curationUserSegment, annotationJCas, repository,
                                    annotationService);
                        }
                        // check if clicked on an arc
                        else if (!action.isEmpty() && action.toString()
                                .equals("selectArcForMerge")) {
                            // add span for merge
                            // get information of the span clicked
                            mergeArc(request, curationUserSegment, annotationJCas);
                        }
                        onChange(aTarget);
                    }
                };
                curationVisualizer.setOutputMarkupId(true);
                item2.add(curationVisualizer);
            }
        };
        sentenceListView.setOutputMarkupId(true);
        add(sentenceListView);
    }

	boolean isCorefType(AnnotationFS aFS) {
		for (Feature f : MergeCas.getAllFeatures(aFS)) {
			if (f.getShortName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE)
					|| f.getShortName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
				return true;
			}
		}
		return false;
	}
    protected void onChange(AjaxRequestTarget aTarget)
    {
        // Overriden in curationPanel
    }

    protected void isCorrection(AjaxRequestTarget aTarget)
    {
        // Overriden in curationPanel
    }

    private void mergeSpan(IRequestParameters aRequest,
            CurationUserSegmentForAnnotationDocument aCurationUserSegment, JCas aJcas,
            RepositoryService aRepository, AnnotationService aAnnotationService)
            throws BratAnnotationException, UIMAException, ClassNotFoundException, IOException
    {
        Integer address = aRequest.getParameterValue("id").toInteger();
        String spanType = removePrefix(aRequest.getParameterValue("type").toString());

        String username = aCurationUserSegment.getUsername();

        SourceDocument sourceDocument = aCurationUserSegment.getBratAnnotatorModel().getDocument();

        AnnotationDocument clickedAnnotationDocument = null;
        List<AnnotationDocument> annotationDocuments = aRepository
                .listAnnotationDocuments(sourceDocument);
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            if (annotationDocument.getUser().equals(username)) {
                clickedAnnotationDocument = annotationDocument;
                break;
            }
        }

        createSpan(spanType, aCurationUserSegment.getBratAnnotatorModel(), aJcas,
                clickedAnnotationDocument, address);
    }

    private void createSpan(String spanType, BratAnnotatorModel aBModel, JCas aMergeJCas,
            AnnotationDocument aAnnotationDocument, int aAddress)
            throws IOException, UIMAException, ClassNotFoundException, BratAnnotationException
    {
        JCas clickedJCas = getJCas(aBModel, aAnnotationDocument);

        AnnotationFS fsClicked = selectByAddr(clickedJCas, aAddress);

       	if(isCorefType(fsClicked)){
       		
    		throw new BratAnnotationException(" Coreference Annotation not supported in curation");
    	}
        long layerId = TypeUtil.getLayerId(spanType);

        MergeCas.addSpanAnnotation(aMergeJCas, fsClicked, annotationService.getLayer(layerId).isAllowStacking());

        repository
                .writeCas(aBModel.getMode(), aBModel.getDocument(), aBModel.getUser(), aMergeJCas);

        // update timestamp
        int sentenceNumber = getSentenceNumber(clickedJCas, fsClicked.getBegin());
        aBModel.setSentenceNumber(sentenceNumber);
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);

        if (aBModel.getPreferences().isScrollPage()) {
            int address = getAddr(selectSentenceAt(clickedJCas, aBModel.getSentenceBeginOffset(),
                    aBModel.getSentenceEndOffset()));
            aBModel.setSentenceAddress(
                    getSentenceBeginAddress(clickedJCas, address, fsClicked.getBegin(),
                            aBModel.getProject(), aBModel.getDocument(),
                            aBModel.getPreferences().getWindowSize()));

            Sentence sentence = selectByAddr(clickedJCas, Sentence.class,
                    aBModel.getSentenceAddress());
            aBModel.setSentenceBeginOffset(sentence.getBegin());
            aBModel.setSentenceEndOffset(sentence.getEnd());

            Sentence firstSentence = selectSentenceAt(clickedJCas, aBModel.getSentenceBeginOffset(),
                    aBModel.getSentenceEndOffset());
            int lastAddressInPage = getLastSentenceAddressInDisplayWindow(clickedJCas,
                    getAddr(firstSentence), aBModel.getPreferences().getWindowSize());
            // the last sentence address in the display window
            Sentence lastSentenceInPage = (Sentence) selectByAddr(clickedJCas,
                    FeatureStructure.class, lastAddressInPage);
            aBModel.setFSN(getSentenceNumber(clickedJCas, firstSentence.getBegin()));
            aBModel.setLSN(getSentenceNumber(clickedJCas, lastSentenceInPage.getBegin()));
        }
    }


    private void mergeArc(IRequestParameters aRequest,
            CurationUserSegmentForAnnotationDocument aCurationUserSegment, JCas aJcas)
            throws BratAnnotationException, IOException, UIMAException, ClassNotFoundException
    {
        Integer addressOriginClicked = aRequest.getParameterValue("originSpanId").toInteger();
        Integer addressTargetClicked = aRequest.getParameterValue("targetSpanId").toInteger();

        String arcType = removePrefix(aRequest.getParameterValue("type").toString());
        String fsArcaddress = aRequest.getParameterValue("arcId").toString();

        String username = aCurationUserSegment.getUsername();
        BratAnnotatorModel bModel = aCurationUserSegment.getBratAnnotatorModel();
        SourceDocument sourceDocument = bModel.getDocument();

        JCas clickedJCas = null;
        
        // for correction and automation, the lower panel is the clickedJcase, from the suggestions
        if (!aCurationUserSegment.getBratAnnotatorModel().getMode().equals(Mode.CURATION)) {
        	clickedJCas = repository.readCorrectionCas(sourceDocument);
        }
        else{
        AnnotationDocument clickedAnnotationDocument = repository
                .listAnnotationDocuments(sourceDocument).stream()
                .filter(an -> an.getUser().equals(username)).findFirst().get();


        try {
            clickedJCas = getJCas(bModel, clickedAnnotationDocument);
        }
        catch (IOException e1) {
            throw new IOException();
        }
       }

        long layerId = TypeUtil.getLayerId(arcType);

        AnnotationLayer layer = annotationService.getLayer(layerId);
        int address = Integer.parseInt(fsArcaddress.split("\\.")[0]);
        AnnotationFS clickedFS = selectByAddr(clickedJCas, address);

     	if(isCorefType(clickedFS)){
       		
    		throw new BratAnnotationException(" Coreference Annotation not supported in curation");
    	}
     	
        MergeCas.addArcAnnotation(aJcas, addressOriginClicked, addressTargetClicked, fsArcaddress,
                clickedJCas, annotationService.listAnnotationFeature(layer), clickedFS,
                layer.getAttachType()!=null, layer.isAllowStacking());
        repository.writeCas(bModel.getMode(), bModel.getDocument(), bModel.getUser(), aJcas);

        // update timestamp
        int sentenceNumber = getSentenceNumber(clickedJCas, clickedFS.getBegin());
        bModel.setSentenceNumber(sentenceNumber);
        bModel.getDocument().setSentenceAccessed(sentenceNumber);

        if (bModel.getPreferences().isScrollPage()) {
            address = getAddr(selectSentenceAt(aJcas, bModel.getSentenceBeginOffset(),
                    bModel.getSentenceEndOffset()));
            bModel.setSentenceAddress(getSentenceBeginAddress(aJcas, address, clickedFS.getBegin(),
                    bModel.getProject(), bModel.getDocument(),
                    bModel.getPreferences().getWindowSize()));
            Sentence sentence = selectByAddr(aJcas, Sentence.class, bModel.getSentenceAddress());
            bModel.setSentenceBeginOffset(sentence.getBegin());
            bModel.setSentenceEndOffset(sentence.getEnd());

            Sentence firstSentence = selectSentenceAt(clickedJCas, bModel.getSentenceBeginOffset(),
                    bModel.getSentenceEndOffset());
            int lastAddressInPage = getLastSentenceAddressInDisplayWindow(clickedJCas,
                    getAddr(firstSentence), bModel.getPreferences().getWindowSize());
            // the last sentence address in the display window
            Sentence lastSentenceInPage = (Sentence) selectByAddr(clickedJCas,
                    FeatureStructure.class, lastAddressInPage);
            bModel.setFSN(getSentenceNumber(clickedJCas, firstSentence.getBegin()));
            bModel.setLSN(getSentenceNumber(clickedJCas, lastSentenceInPage.getBegin()));
        }
    }


    private JCas getJCas(BratAnnotatorModel aModel, AnnotationDocument aDocument)
            throws IOException
    {
        try {
            if (aModel.getMode().equals(Mode.AUTOMATION) || aModel.getMode()
                    .equals(Mode.CORRECTION)) {
                return repository.readCorrectionCas(aModel.getDocument());
            }
            else {
                return repository.readAnnotationCas(aDocument);
            }
        }
        catch (UIMAException | ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    /**
     * Removes a prefix that is added to brat visualization for different color coded purpose.
     */
    private static String removePrefix(String aType)
    {
        return aType.replace("_(" + AnnotationState.AGREE.name() + ")", "")
                .replace("_(" + AnnotationState.USE.name() + ")", "")
                .replace("_(" + AnnotationState.DISAGREE.name() + ")", "")
                .replace("_(" + AnnotationState.DO_NOT_USE.name() + ")", "")
                .replace("_(" + AnnotationState.NOT_SUPPORTED.name() + ")", "");
    }
}
