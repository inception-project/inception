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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil.getAdapter;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.findWindowStartCenteringOnSelection;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentenceAt;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.core.JsonGenerator;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.TypeRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratRenderer;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationOption;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.BratSuggestionVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.util.MergeCas;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A {@link MarkupContainer} for either curation users' sentence annotation (for the lower panel) or
 * the automated annotations
 */
public class SuggestionViewPanel
        extends WebMarkupContainer
{
    private static final long serialVersionUID = 8736268179612831795L;

    private static final Logger LOG = LoggerFactory.getLogger(SuggestionViewPanel.class);
    
    private final ListView<CurationUserSegmentForAnnotationDocument> sentenceListView;
    
    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean CorrectionDocumentService correctionDocumentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;

    /**
     * Data models for the annotation editor
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

    @SuppressWarnings("unchecked")
    public IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> getModel()
    {
        return (IModel<LinkedList<CurationUserSegmentForAnnotationDocument>>) getDefaultModel();
    }

    @SuppressWarnings("unchecked")
    public LinkedList<CurationUserSegmentForAnnotationDocument> getModelObject()
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
                     */
                    @Override protected void onSelectAnnotationForMerge(AjaxRequestTarget aTarget)
                            throws UIMAException, ClassNotFoundException, IOException,
                            AnnotationException
                    {
                        // TODO: chain the error from this component up in the
                        // CurationPage
                        // or CorrectionPage
                        if (BratAnnotatorUtility.isDocumentFinished(documentService,
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
                                documentService.readAnnotationCas(
                                        documentService.getAnnotationDocument(sourceDocument, user)) :
                                            curationDocumentService.readCurationCas(sourceDocument);
                        StringValue action = request.getParameterValue("action");
                        // check if clicked on a span
                        if (!action.isEmpty() && action.toString().equals("selectSpanForMerge")) {                    
                            mergeSpan(request, curationUserSegment, annotationJCas);
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
            CurationUserSegmentForAnnotationDocument aCurationUserSegment, JCas aJcas)
            throws AnnotationException, UIMAException, ClassNotFoundException, IOException
    {
        Integer address = aRequest.getParameterValue("id").toInteger();
        String spanType = removePrefix(aRequest.getParameterValue("type").toString());

        String username = aCurationUserSegment.getUsername();

        SourceDocument sourceDocument = aCurationUserSegment.getBratAnnotatorModel().getDocument();

        AnnotationDocument clickedAnnotationDocument = null;
        List<AnnotationDocument> annotationDocuments = documentService
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

    private void createSpan(String spanType, AnnotatorState aBModel, JCas aMergeJCas,
            AnnotationDocument aAnnotationDocument, int aAddress)
            throws IOException, UIMAException, ClassNotFoundException, AnnotationException
    {
        JCas clickedJCas = getJCas(aBModel, aAnnotationDocument);

        AnnotationFS fsClicked = selectByAddr(clickedJCas, aAddress);

       	if(isCorefType(fsClicked)){
       		
    		throw new AnnotationException(" Coreference Annotation not supported in curation");
    	}
        long layerId = TypeUtil.getLayerId(spanType);

        AnnotationLayer layer = annotationService.getLayer(layerId);
        MergeCas.addSpanAnnotation(annotationService, layer, aMergeJCas, fsClicked,
                layer.isAllowStacking());

        writeEditorCas(aBModel, aMergeJCas);

        // update timestamp
        int sentenceNumber = getSentenceNumber(clickedJCas, fsClicked.getBegin());
        aBModel.setFocusUnitIndex(sentenceNumber);
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);

        if (aBModel.getPreferences().isScrollPage()) {
            Sentence sentence = selectSentenceAt(aMergeJCas, aBModel.getFirstVisibleUnitBegin(),
                    aBModel.getFirstVisibleUnitEnd());
            sentence = findWindowStartCenteringOnSelection(aMergeJCas, sentence,
                    fsClicked.getBegin(), aBModel.getProject(), aBModel.getDocument(),
                    aBModel.getPreferences().getWindowSize());
            aBModel.setFirstVisibleUnit(sentence);
        }
    }


    private void mergeArc(IRequestParameters aRequest,
            CurationUserSegmentForAnnotationDocument aCurationUserSegment, JCas aJcas)
            throws AnnotationException, IOException, UIMAException, ClassNotFoundException
    {
        Integer addressOriginClicked = aRequest.getParameterValue("originSpanId").toInteger();
        Integer addressTargetClicked = aRequest.getParameterValue("targetSpanId").toInteger();

        String arcType = removePrefix(aRequest.getParameterValue("type").toString());
        String fsArcaddress = aRequest.getParameterValue("arcId").toString();

        String username = aCurationUserSegment.getUsername();
        AnnotatorState bModel = aCurationUserSegment.getBratAnnotatorModel();
        SourceDocument sourceDocument = bModel.getDocument();

        JCas clickedJCas = null;
        
        // for correction and automation, the lower panel is the clickedJcase, from the suggestions
        if (!aCurationUserSegment.getBratAnnotatorModel().getMode().equals(Mode.CURATION)) {
        	clickedJCas = correctionDocumentService.readCorrectionCas(sourceDocument);
        }
        else{
        AnnotationDocument clickedAnnotationDocument = documentService
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
        TypeAdapter adapter = TypeUtil.getAdapter(annotationService, layer);
        int address = Integer.parseInt(fsArcaddress.split("\\.")[0]);
        AnnotationFS clickedFS = selectByAddr(clickedJCas, address);

     	if(isCorefType(clickedFS)){
       		
    		throw new AnnotationException(" Coreference Annotation not supported in curation");
    	}
     	
        MergeCas.addArcAnnotation(adapter, aJcas, addressOriginClicked, addressTargetClicked,
                fsArcaddress, clickedJCas, clickedFS);
        writeEditorCas(bModel, aJcas);

        int sentenceNumber = getSentenceNumber(clickedJCas, clickedFS.getBegin());
        bModel.setFocusUnitIndex(sentenceNumber);
        
        // Update timestamp
        bModel.getDocument().setSentenceAccessed(sentenceNumber);

        if (bModel.getPreferences().isScrollPage()) {
            Sentence sentence = selectSentenceAt(aJcas, bModel.getFirstVisibleUnitBegin(),
                    bModel.getFirstVisibleUnitEnd());
            sentence = findWindowStartCenteringOnSelection(aJcas, sentence,
                    clickedFS.getBegin(), bModel.getProject(), bModel.getDocument(),
                    bModel.getPreferences().getWindowSize());
            bModel.setFirstVisibleUnit(sentence);
        }
    }

    private JCas getJCas(AnnotatorState aState, AnnotationDocument aDocument)
        throws IOException
    {
        if (aState.getMode().equals(Mode.AUTOMATION) || aState.getMode().equals(Mode.CORRECTION)) {
            return correctionDocumentService.readCorrectionCas(aState.getDocument());
        }
        else {
            return documentService.readAnnotationCas(aDocument);
        }
    }
    
    private void writeEditorCas(AnnotatorState aState, JCas aJCas)
        throws IOException
    {
        if (aState.getMode().equals(Mode.ANNOTATION) || aState.getMode().equals(Mode.AUTOMATION)
                || aState.getMode().equals(Mode.CORRECTION)) {
            documentService.writeAnnotationCas(aJCas, aState.getDocument(), aState.getUser(), true);
        }
        else if (aState.getMode().equals(Mode.CURATION)) {
            curationDocumentService.writeCurationCas(aJCas, aState.getDocument(), true);
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
    
    public final static String CURATION_USER = "CURATION_USER";

    private void populateCurationSentences(
            Map<String, JCas> aJCases,
            List<CurationUserSegmentForAnnotationDocument> aSentences,
            AnnotatorState aBratAnnotatorModel,
            final List<AnnotationOption> aAnnotationOptions,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            AnnotationSchemaService aAnnotationService, CurationContainer aCurationContainer,
            final Map<VID, AnnotationState> aStates)
        throws IOException
    {
        List<String> usernamesSorted = new ArrayList<String>(aJCases.keySet());
        Collections.sort(usernamesSorted);

        final Mode mode = aBratAnnotatorModel.getMode();
        boolean isAutomationMode = mode.equals(Mode.AUTOMATION);
        boolean isCorrectionMode = mode.equals(Mode.CORRECTION);
        boolean isCurationMode = mode.equals(Mode.CURATION);

        String annotatorCasUser;
        switch (mode) {
        case AUTOMATION: // fall-through
        case CORRECTION:
            annotatorCasUser = SecurityContextHolder.getContext().getAuthentication().getName();
            break;
        case CURATION:
            annotatorCasUser = CURATION_USER;
            break;
        default:
            throw new IllegalStateException("Illegal mode [" + mode + "]");
        }

        LOG.debug("mode = [" + mode + "]");
        LOG.debug("all users is  " + usernamesSorted);
        LOG.debug("annotator CAS is for user [" + annotatorCasUser + "]");

        for (String username : usernamesSorted) {
            if ((!username.equals(CURATION_USER) && isCurationMode)
                    || (username.equals(CURATION_USER) && (isAutomationMode || isCorrectionMode))) {

                JCas jCas = aJCases.get(username);
                // Set up coloring strategy
                ColoringStrategy curationColoringStrategy = new ColoringStrategy()
                {
                    @Override
                    public String getColor(VID aVid, String aLabel)
                    {
                        if (aStates.get(aVid)==null){
                            return AnnotationState.NOT_SUPPORTED.getColorCode();
                        }
                        return aStates.get(aVid).getColorCode();
                    }
                };

                // Create curation view for the current user
                CurationUserSegmentForAnnotationDocument curationUserSegment2 = new CurationUserSegmentForAnnotationDocument();
                curationUserSegment2.setCollectionData(getCollectionInformation(aAnnotationService,
                        aCurationContainer));
                curationUserSegment2.setDocumentResponse(render(jCas, aBratAnnotatorModel,
                        curationColoringStrategy));
                curationUserSegment2.setUsername(username);
                curationUserSegment2.setBratAnnotatorModel(aBratAnnotatorModel);
                curationUserSegment2
                        .setAnnotationSelectionByUsernameAndAddress(aAnnotationSelectionByUsernameAndAddress);
                aSentences.add(curationUserSegment2);
            }
        }
    }

    private String render(JCas aJcas, AnnotatorState aBratAnnotatorModel,
            ColoringStrategy aCurationColoringStrategy)
        throws IOException
    {
        AnnotationSchemaService aAnnotationService = annotationService;
        
        GetDocumentResponse response = new GetDocumentResponse();
        response.setRtlMode(ScriptDirection.RTL.equals(aBratAnnotatorModel.getScriptDirection()));

        // Render invisible baseline annotations (sentence, tokens)
        BratRenderer.renderTokenAndSentence(aJcas, response, aBratAnnotatorModel);

        // Render visible (custom) layers
        for (AnnotationLayer layer : aBratAnnotatorModel.getAnnotationLayers()) {
            if (layer.getName().equals(Token.class.getName())
                    || layer.getName().equals(Sentence.class.getName())
                    || WebAnnoConst.CHAIN_TYPE.equals(layer.getType())) {
                continue;
            }

            List<AnnotationFeature> features = aAnnotationService.listAnnotationFeature(layer);
            List<AnnotationFeature> invisibleFeatures = new ArrayList<AnnotationFeature>();
            for (AnnotationFeature feature : features) {
                if (!feature.isVisible()) {
                    invisibleFeatures.add(feature);
                }
            }
            features.removeAll(invisibleFeatures);
            TypeAdapter adapter = getAdapter(aAnnotationService, layer);
            TypeRenderer renderer = BratRenderer.getRenderer(adapter);
            renderer.render(aJcas, features, response, aBratAnnotatorModel,
                    aCurationColoringStrategy);
        }

        return JSONUtil.toInterpretableJsonString(response);
    }

    private String getCollectionInformation(AnnotationSchemaService aAnnotationService,
            CurationContainer aCurationContainer)
        throws IOException
    {
        GetCollectionInformationResponse info = new GetCollectionInformationResponse();
        info.setEntityTypes(BratRenderer.buildEntityTypes(aCurationContainer
                .getBratAnnotatorModel().getAnnotationLayers(), aAnnotationService));

        StringWriter out = new StringWriter();
        JsonGenerator jsonGenerator = JSONUtil.getJsonConverter().getObjectMapper()
                .getFactory().createGenerator(out);
        jsonGenerator.writeObject(info);
        return out.toString();
    }

    /**
     * @param aTarget
     *            the AJAX target.
     * @param aCurationContainer
     *            the container.
     * @param aMergeVisualizer
     *            the annotator component.
     * @param aAnnotationSelectionByUsernameAndAddress
     *            selections by user.
     * @param aCurationSegment
     *            the segment.
     * @throws UIMAException
     *             hum?
     * @throws ClassNotFoundException
     *             hum?
     * @throws IOException
     *             hum?
     * @throws AnnotationException
     *             hum?
     */
    public void updatePanel(
            AjaxRequestTarget aTarget,
            CurationContainer aCurationContainer,
            AnnotationEditorBase aMergeVisualizer,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            SourceListView aCurationSegment)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        AnnotatorState bModel = aCurationContainer.getBratAnnotatorModel();
        SourceDocument sourceDocument = bModel.getDocument();
        Map<String, JCas> jCases = new HashMap<String, JCas>();

        // This is the CAS that the user can actively edit
        JCas annotatorCas = getAnnotatorCas(bModel, aAnnotationSelectionByUsernameAndAddress,
                sourceDocument, jCases);

        // We store the CAS that the user will edit as the "CURATION USER"
        jCases.put(CURATION_USER, annotatorCas);

        // get differing feature structures
        List<Type> entryTypes = SuggestionBuilder.getEntryTypes(annotatorCas,
                bModel.getAnnotationLayers(), annotationService);
        List<AnnotationOption> annotationOptions = null;

        Map<VID, AnnotationState> annoStates = new HashMap<>();

        DiffResult diff;

        if (bModel.getMode().equals(Mode.CURATION)) {
            diff = CasDiff2.doDiffSingle(annotationService, bModel.getProject(), entryTypes,
                    LinkCompareBehavior.LINK_ROLE_AS_LABEL, jCases,
                    aCurationSegment.getCurationBegin(), aCurationSegment.getCurationEnd());
        }
        else {
            diff = CasDiff2.doDiffSingle(annotationService, bModel.getProject(), entryTypes,
                    LinkCompareBehavior.LINK_ROLE_AS_LABEL, jCases, aCurationSegment.getBegin(),
                    aCurationSegment.getEnd());
        }

        Collection<ConfigurationSet> d = diff.getDifferingConfigurationSets().values();

        Collection<ConfigurationSet> i = diff.getIncompleteConfigurationSets().values();
        for (ConfigurationSet cfgSet : d) {
            if (i.contains(cfgSet)) {
                i.remove(cfgSet);
            }
        }

        addSuggestionColor(bModel.getProject(), bModel.getMode(), jCases, annoStates, d, false, false);
        addSuggestionColor(bModel.getProject(), bModel.getMode(), jCases, annoStates, i, true, false);

        List<ConfigurationSet> all = new ArrayList<>();

        for (ConfigurationSet a : diff.getConfigurationSets()) {
            all.add(a);
        }
        for (ConfigurationSet cfgSet : d) {
            all.remove(cfgSet);
        }
        for (ConfigurationSet cfgSet : i) {
            all.remove(cfgSet);
        }

        addSuggestionColor(bModel.getProject(), bModel.getMode(), jCases, annoStates, all, false, true);

        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();

        populateCurationSentences(jCases, sentences, bModel, annotationOptions,
                aAnnotationSelectionByUsernameAndAddress, annotationService, aCurationContainer,
                annoStates);

        // update sentence list on the right side
        this.setModelObject(sentences);

        aTarget.add(this);
    }

    /**
     * For each {@link ConfigurationSet}, where there are some differences in users annotation and
     * the curation annotation.
     */
    private void addSuggestionColor(Project aProject, Mode aMode, Map<String, JCas> aCasMap,
            Map<VID, AnnotationState> aSuggestionColors, Collection<ConfigurationSet> aCfgSet,
            boolean aI, boolean aAgree)
    {
        for (ConfigurationSet cs : aCfgSet) {
            boolean use = false;
            for (String u : cs.getCasGroupIds()) {
                for (Configuration c : cs.getConfigurations(u)) {

                    FeatureStructure fs = c.getFs(u, aCasMap);
                    
                    AnnotationLayer layer = annotationService.getLayer(fs.getType().getName(), aProject);
                    TypeAdapter typeAdapter = TypeUtil.getAdapter(annotationService, layer);
                    
                    VID vid;
                    // link FS
                    if (c.getPosition().getFeature() != null) {
                        int fi = 0;
                        for (AnnotationFeature f : typeAdapter.listFeatures()) {
                            if (f.getName().equals(c.getPosition().getFeature())) {
                                break;
                            }
                            fi++;
                        }
                        
                        vid = new VID(WebAnnoCasUtil.getAddr(fs), fi, c.getAID(u).index);
                    }
                    else {
                        vid = new VID(WebAnnoCasUtil.getAddr(fs));
                    }
                    
                    if (aAgree) {
                        aSuggestionColors.put(vid, AnnotationState.AGREE);
                        continue;
                    }
                    // automation and correction projects
                    if (!aMode.equals(Mode.CURATION) && !aAgree) {
                        if (cs.getCasGroupIds().size() == 2) {
                            aSuggestionColors.put(vid, AnnotationState.DO_NOT_USE);
                        }
                        else {
                            aSuggestionColors.put(vid, AnnotationState.DISAGREE);
                        }
                        continue;
                    }

                    // this set agree with the curation annotation
                    if (c.getCasGroupIds().contains(CURATION_USER)) {
                        use = true;
                    }
                    else {
                        use = false;
                    }
                    // this curation view
                    if (u.equals(CURATION_USER)) {
                        continue;
                    }

                    if (aAgree) {
                        aSuggestionColors.put(vid, AnnotationState.AGREE);
                    }
                    else if (use) {
                        aSuggestionColors.put(vid, AnnotationState.USE);
                    }
                    else if (aI) {
                        aSuggestionColors.put(vid, AnnotationState.DISAGREE);
                    }
                    else if (!cs.getCasGroupIds().contains(CURATION_USER)) {
                        aSuggestionColors.put(vid, AnnotationState.DISAGREE);
                    }
                    else {
                        aSuggestionColors.put(vid, AnnotationState.DO_NOT_USE);
                    }
                }
            }
        }
    }

    private JCas getAnnotatorCas(
            AnnotatorState aBModel,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            SourceDocument sourceDocument,
            Map<String, JCas> jCases)
        throws UIMAException, IOException, ClassNotFoundException
    {
        JCas annotatorCas;
        if (aBModel.getMode().equals(Mode.AUTOMATION) || aBModel.getMode().equals(Mode.CORRECTION)) {
            // If this is a CORRECTION or AUTOMATION project, then we get the CORRECTION document
            // and put it in as the single document to compare with. Basically what we do is that
            // we treat consider this scenario as a curation scenario where the CORRECTION document
            // is the only document we compare with.

            // The CAS the user can edit is the one from the virtual CORRECTION USER
            annotatorCas = correctionDocumentService.readCorrectionCas(sourceDocument);

            User user = userRepository.get(SecurityContextHolder.getContext().getAuthentication()
                    .getName());
            AnnotationDocument annotationDocument = documentService.getAnnotationDocument(
                    sourceDocument, user);
            jCases.put(user.getUsername(), documentService.readAnnotationCas(annotationDocument));
            aAnnotationSelectionByUsernameAndAddress.put(CURATION_USER,
                    new HashMap<Integer, AnnotationSelection>());
        }
        else {
            // If this is a true CURATION then we get all the annotation documents from all the
            // active users.

            // The CAS the user can edit is the one from the virtual CURATION USER
            annotatorCas = curationDocumentService.readCurationCas(sourceDocument);

            // Now we get all the other CASes from the repository
            List<AnnotationDocument> annotationDocuments = documentService
                    .listAnnotationDocuments(sourceDocument);
            for (AnnotationDocument annotationDocument : annotationDocuments) {
                String username = annotationDocument.getUser();
                if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)
                        || username.equals(CURATION_USER)) {
                    JCas jCas = documentService.readAnnotationCas(annotationDocument);
                    jCases.put(username, jCas);

                    // cleanup annotationSelections
                    aAnnotationSelectionByUsernameAndAddress.put(username,
                            new HashMap<Integer, AnnotationSelection>());
                }
            }
        }
        return annotatorCas;
    }
}
