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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.updateDocumentTimestampAfterWrite;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.findWindowStartCenteringOnSelection;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentenceAt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
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

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
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
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.BratSuggestionVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.UserAnnotationSegment;
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
    private static final String PARAM_TARGET_SPAN_ID = "targetSpanId";
    private static final String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_ID = "id";
    private static final String PARAM_ARC_ID = "arcId";
    private static final String PARAM_ACTION = "action";

    private static final String ACTION_SELECT_ARC_FOR_MERGE = "selectArcForMerge";
    private static final String ACTION_SELECT_SPAN_FOR_MERGE = "selectSpanForMerge";

    private static final long serialVersionUID = 8736268179612831795L;

    private static final Logger LOG = LoggerFactory.getLogger(SuggestionViewPanel.class);
    
    private final ListView<UserAnnotationSegment> sentenceListView;
    
    private @SpringBean PreRenderer preRenderer;
    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean CorrectionDocumentService correctionDocumentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;

    public SuggestionViewPanel(String id,
            IModel<List<UserAnnotationSegment>> aModel)
    {
        super(id, aModel);
        setOutputMarkupId(true);

        sentenceListView = new ListView<UserAnnotationSegment>("sentenceListView", aModel)
        {
            private static final long serialVersionUID = -5389636445364196097L;

            @Override
            protected void populateItem(ListItem<UserAnnotationSegment> aItem)
            {
                final UserAnnotationSegment curationUserSegment = aItem.getModelObject();
                BratSuggestionVisualizer curationVisualizer = new BratSuggestionVisualizer(
                        "sentence", new Model<>(curationUserSegment))
                {
                    private static final long serialVersionUID = -1205541428144070566L;

                    /**
                     * Method is called, if user has clicked on a span or an arc in the sentence
                     * panel. The span or arc respectively is identified and copied to the merge
                     * CAS.
                     */
                    @Override
                    protected void onSelectAnnotationForMerge(AjaxRequestTarget aTarget)
                        throws UIMAException, ClassNotFoundException, IOException,
                        AnnotationException
                    {
                        // TODO: chain the error from this component up in the
                        // CurationPage or CorrectionPage
                        if (BratAnnotatorUtility.isDocumentFinished(documentService,
                                curationUserSegment.getAnnotatorState())) {
                            aTarget.appendJavaScript("alert('This document is already closed."
                                    + " Please ask admin to re-open')");
                            return;
                        }
                        User user = userRepository.getCurrentUser();

                        SourceDocument sourceDocument = curationUserSegment.getAnnotatorState()
                                .getDocument();
                        CAS annotationJCas = (curationUserSegment.getAnnotatorState().getMode()
                                .equals(Mode.AUTOMATION)
                                || curationUserSegment.getAnnotatorState().getMode()
                                        .equals(Mode.CORRECTION))
                                                ? documentService.readAnnotationCas(
                                                        documentService.getAnnotationDocument(
                                                                sourceDocument, user))
                                                : curationDocumentService
                                                        .readCurationCas(sourceDocument);
                                                        
                        final IRequestParameters request = getRequest().getPostParameters();
                        StringValue action = request.getParameterValue(PARAM_ACTION);
                        // check if clicked on a span
                        if (!action.isEmpty()
                                && ACTION_SELECT_SPAN_FOR_MERGE.equals(action.toString())) {
                            mergeSpan(request, curationUserSegment, annotationJCas);
                        }
                        // check if clicked on an arc
                        else if (!action.isEmpty()
                                && ACTION_SELECT_ARC_FOR_MERGE.equals(action.toString())) {
                            // add span for merge
                            // get information of the span clicked
                            mergeArc(request, curationUserSegment, annotationJCas);
                        }
                        onChange(aTarget);
                    }
                };
                curationVisualizer.setOutputMarkupId(true);
                aItem.add(curationVisualizer);
            }
        };
        sentenceListView.setOutputMarkupId(true);
        add(sentenceListView);
    }
    
    private boolean isCorefType(AnnotationFS aFS)
    {
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

    private void mergeSpan(IRequestParameters aRequest,
            UserAnnotationSegment aCurationUserSegment, CAS aJcas)
            throws AnnotationException, UIMAException, ClassNotFoundException, IOException
    {
        AnnotationDocument clickedAnnotationDocument;
        AnnotatorState state = aCurationUserSegment.getAnnotatorState();
        if (state.getMode().equals(Mode.AUTOMATION) || state.getMode().equals(Mode.CORRECTION)) {
            // createSpan / getJCas do not require an annotation document in this mode
            clickedAnnotationDocument = null;
        }
        else {
            SourceDocument sourceDocument = aCurationUserSegment.getAnnotatorState().getDocument();
            clickedAnnotationDocument = documentService.getAnnotationDocument(sourceDocument,
                    aCurationUserSegment.getUsername());
        }
        
        int address = aRequest.getParameterValue(PARAM_ID).toInt();
        String spanType = removePrefix(aRequest.getParameterValue(PARAM_TYPE).toString());

        createSpan(spanType, state, aJcas, clickedAnnotationDocument, address);
    }

    private void createSpan(String spanType, AnnotatorState aBModel, CAS aMergeJCas,
            AnnotationDocument aAnnotationDocument, int aAddress)
            throws IOException, UIMAException, ClassNotFoundException, AnnotationException
    {
        CAS clickedJCas = getJCas(aBModel, aAnnotationDocument);

        AnnotationFS fsClicked = selectAnnotationByAddr(clickedJCas, aAddress);

        if (isCorefType(fsClicked)) {
            throw new AnnotationException("Coreference Annotation not supported in curation");
        }
        long layerId = TypeUtil.getLayerId(spanType);

        AnnotationLayer layer = annotationService.getLayer(layerId);
        MergeCas.addSpanAnnotation(aBModel, annotationService, layer, aMergeJCas, fsClicked,
                layer.isAllowStacking());

        writeEditorCas(aBModel, aMergeJCas);

        // update timestamp
        int sentenceNumber = getSentenceNumber(clickedJCas, fsClicked.getBegin());
        aBModel.setFocusUnitIndex(sentenceNumber);
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);

        if (aBModel.getPreferences().isScrollPage()) {
            AnnotationFS sentence = selectSentenceAt(aMergeJCas, aBModel.getFirstVisibleUnitBegin(),
                    aBModel.getFirstVisibleUnitEnd());
            sentence = findWindowStartCenteringOnSelection(aMergeJCas, sentence,
                    fsClicked.getBegin(), aBModel.getProject(), aBModel.getDocument(),
                    aBModel.getPreferences().getWindowSize());
            aBModel.setFirstVisibleUnit(sentence);
        }
    }

    private void mergeArc(IRequestParameters aRequest,
            UserAnnotationSegment aCurationUserSegment, CAS aJcas)
            throws AnnotationException, IOException, UIMAException, ClassNotFoundException
    {
        int addressOriginClicked = aRequest.getParameterValue(PARAM_ORIGIN_SPAN_ID).toInt();
        int addressTargetClicked = aRequest.getParameterValue(PARAM_TARGET_SPAN_ID).toInt();

        String arcType = removePrefix(aRequest.getParameterValue(PARAM_TYPE).toString());
        String fsArcaddress = aRequest.getParameterValue(PARAM_ARC_ID).toString();

        AnnotatorState bModel = aCurationUserSegment.getAnnotatorState();
        SourceDocument sourceDocument = bModel.getDocument();
        
        // for correction and automation, the lower panel is the clickedJcase, from the suggestions
        CAS clickedJCas;
        if (!aCurationUserSegment.getAnnotatorState().getMode().equals(Mode.CURATION)) {
            clickedJCas = correctionDocumentService.readCorrectionCas(sourceDocument);
        }
        else {
            User user = userRepository.get(aCurationUserSegment.getUsername());
            AnnotationDocument clickedAnnotationDocument = documentService
                    .getAnnotationDocument(sourceDocument, user);
            clickedJCas = getJCas(bModel, clickedAnnotationDocument);
        }

        long layerId = TypeUtil.getLayerId(arcType);

        AnnotationLayer layer = annotationService.getLayer(layerId);
        TypeAdapter adapter = annotationService.getAdapter(layer);
        int address = Integer.parseInt(fsArcaddress.split("\\.")[0]);
        AnnotationFS clickedFS = selectAnnotationByAddr(clickedJCas, address);

        if (isCorefType(clickedFS)) {
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
            AnnotationFS sentence = selectSentenceAt(aJcas, bModel.getFirstVisibleUnitBegin(),
                    bModel.getFirstVisibleUnitEnd());
            sentence = findWindowStartCenteringOnSelection(aJcas, sentence,
                    clickedFS.getBegin(), bModel.getProject(), bModel.getDocument(),
                    bModel.getPreferences().getWindowSize());
            bModel.setFirstVisibleUnit(sentence);
        }
    }

    private CAS getJCas(AnnotatorState aState, AnnotationDocument aDocument)
        throws IOException
    {
        if (aState.getMode().equals(Mode.AUTOMATION) || aState.getMode().equals(Mode.CORRECTION)) {
            return correctionDocumentService.readCorrectionCas(aState.getDocument());
        }
        else {
            return documentService.readAnnotationCas(aDocument);
        }
    }
    
    private void writeEditorCas(AnnotatorState aState, CAS aJCas)
        throws IOException
    {
        if (aState.getMode().equals(Mode.ANNOTATION) || aState.getMode().equals(Mode.AUTOMATION)
                || aState.getMode().equals(Mode.CORRECTION)) {
            documentService.writeAnnotationCas(aJCas, aState.getDocument(), aState.getUser(), true);

            updateDocumentTimestampAfterWrite(aState, documentService.getAnnotationCasTimestamp(
                    aState.getDocument(), aState.getUser().getUsername()));
        }
        else if (aState.getMode().equals(Mode.CURATION)) {
            curationDocumentService.writeCurationCas(aJCas, aState.getDocument(), true);

            updateDocumentTimestampAfterWrite(aState, curationDocumentService
                    .getCurationCasTimestamp(aState.getDocument()));
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

    private String render(CAS aJcas, AnnotatorState aBratAnnotatorModel,
            ColoringStrategy aCurationColoringStrategy)
        throws IOException
    {
        List<AnnotationLayer> layersToRender = new ArrayList<>();
        for (AnnotationLayer layer : aBratAnnotatorModel.getAnnotationLayers()) {
            boolean isSegmentationLayer = layer.getName().equals(Token.class.getName())
                    || layer.getName().equals(Sentence.class.getName());
            boolean isUnsupportedLayer = layer.getType().equals(CHAIN_TYPE);
            
            if (layer.isEnabled() && !isSegmentationLayer && !isUnsupportedLayer) {
                layersToRender.add(layer);
            }
        }
        
        VDocument vdoc = new VDocument();
        preRenderer.render(vdoc, aBratAnnotatorModel.getWindowBeginOffset(),
                aBratAnnotatorModel.getWindowEndOffset(), aJcas, layersToRender);
        
        GetDocumentResponse response = new GetDocumentResponse();
        BratRenderer.render(response, aBratAnnotatorModel, vdoc, aJcas, annotationService,
                aCurationColoringStrategy);
        return JSONUtil.toInterpretableJsonString(response);
    }

    private String getCollectionInformation(AnnotationSchemaService aAnnotationService,
            CurationContainer aCurationContainer)
        throws IOException
    {
        GetCollectionInformationResponse info = new GetCollectionInformationResponse();
        info.setEntityTypes(BratRenderer.buildEntityTypes(aCurationContainer
                .getAnnotatorState().getAnnotationLayers(), aAnnotationService));

        return JSONUtil.toInterpretableJsonString(info);
    }
    
    /**
     * Initializes the user annotation segments later to be filled with content.
     */
    public void init(AjaxRequestTarget aTarget, CurationContainer aCurationContainer,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            SourceListView aCurationSegment)
        throws UIMAException, ClassNotFoundException, IOException
    {
        AnnotatorState state = aCurationContainer.getAnnotatorState();
        SourceDocument sourceDocument = state.getDocument();
        
        Map<String, CAS> jCases = new HashMap<>();
        // This is the CAS that the user can actively edit
        CAS annotatorCas = getAnnotatorCas(state, aAnnotationSelectionByUsernameAndAddress,
                sourceDocument, jCases);

        // We store the CAS that the user will edit as the "CURATION USER"
        jCases.put(CURATION_USER, annotatorCas);
        
        // get differing feature structures
        Map<String, Map<VID, AnnotationState>> annoStates = calcColors(state, aCurationSegment,
                annotatorCas, jCases);
        
        List<String> usernamesSorted = new ArrayList<>(jCases.keySet());
        Collections.sort(usernamesSorted);

        final Mode mode = state.getMode();
        boolean isAutomationMode = mode.equals(Mode.AUTOMATION);
        boolean isCorrectionMode = mode.equals(Mode.CORRECTION);
        boolean isCurationMode = mode.equals(Mode.CURATION);

        List<UserAnnotationSegment> segments = new ArrayList<>();
        for (String username : usernamesSorted) {
            if ((!username.equals(CURATION_USER) && isCurationMode)
                    || (username.equals(CURATION_USER) && (isAutomationMode || isCorrectionMode))) {
                
                CAS jCas = jCases.get(username);
                
                // Set up coloring strategy
                ColoringStrategy curationColoringStrategy = makeColoringStrategy(
                        annoStates.get(username));
                
                // Create curation view for the current user
                UserAnnotationSegment seg = 
                        new UserAnnotationSegment();
                seg.setUsername(username);
                seg.setAnnotatorState(state);
                seg.setCollectionData(
                        getCollectionInformation(annotationService, aCurationContainer));
                seg.setDocumentResponse(
                        render(jCas, state, curationColoringStrategy));
                seg.setSelectionByUsernameAndAddress(aAnnotationSelectionByUsernameAndAddress);
                segments.add(seg);
            }
        }
        
        sentenceListView.setModelObject(segments);
        if (aTarget != null) {
            aTarget.add(this);
        }
    }

    /**
     * @param aTarget
     *            the AJAX target.
     * @param aCurationContainer
     *            the container.
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
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            SourceListView aCurationSegment)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        AnnotatorState state = aCurationContainer.getAnnotatorState();
        SourceDocument sourceDocument = state.getDocument();
        Map<String, CAS> jCases = new HashMap<>();

        // This is the CAS that the user can actively edit
        CAS annotatorCas = getAnnotatorCas(state, aAnnotationSelectionByUsernameAndAddress,
                sourceDocument, jCases);

        // We store the CAS that the user will edit as the "CURATION USER"
        jCases.put(CURATION_USER, annotatorCas);

        // get differing feature structures
        Map<String, Map<VID, AnnotationState>> annoStates = calcColors(state, aCurationSegment,
                annotatorCas, jCases);

        sentenceListView.visitChildren(BratSuggestionVisualizer.class, (v, visit) -> {
            BratSuggestionVisualizer vis = (BratSuggestionVisualizer) v;
            UserAnnotationSegment seg = vis.getModelObject();
            
            CAS jCas = jCases.get(seg.getUsername());
            
            // Set up coloring strategy
            ColoringStrategy curationColoringStrategy = makeColoringStrategy(
                    annoStates.get(seg.getUsername()));

            // Create curation view for the current user
            try {
                seg.setCollectionData(
                        getCollectionInformation(annotationService, aCurationContainer));
                seg.setDocumentResponse(
                        render(jCas, state, curationColoringStrategy));
                seg.setAnnotatorState(state);
                seg.setSelectionByUsernameAndAddress(
                        aAnnotationSelectionByUsernameAndAddress);
            }
            catch (IOException e) {
                error("Unable to render: " + e.getMessage());
                LOG.error("Unable to render", e);
            }
            
            vis.requestRender(aTarget);
        });
    }
    
    private Map<String, Map<VID, AnnotationState>> calcColors(AnnotatorState state,
            SourceListView aCurationSegment, CAS annotatorCas, Map<String, CAS> jCases)
    {
        // get differing feature structures
        List<Type> entryTypes = SuggestionBuilder.getEntryTypes(annotatorCas,
                state.getAnnotationLayers(), annotationService);

        Map<String, Map<VID, AnnotationState>> annoStates = new HashMap<>();

        DiffResult diff;
        if (state.getMode().equals(Mode.CURATION)) {
            diff = CasDiff2.doDiffSingle(annotationService, state.getProject(), entryTypes,
                    LinkCompareBehavior.LINK_ROLE_AS_LABEL, jCases,
                    aCurationSegment.getCurationBegin(), aCurationSegment.getCurationEnd());
        }
        else {
            diff = CasDiff2.doDiffSingle(annotationService, state.getProject(), entryTypes,
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

        addSuggestionColor(state.getProject(), state.getMode(), jCases, annoStates, d, false,
                false);
        addSuggestionColor(state.getProject(), state.getMode(), jCases, annoStates, i, true, false);

        List<ConfigurationSet> all = new ArrayList<>();
        all.addAll(diff.getConfigurationSets());
        all.removeAll(d);
        all.removeAll(i);

        addSuggestionColor(state.getProject(), state.getMode(), jCases, annoStates, all, false,
                true);
        return annoStates;
    }
    
    private ColoringStrategy makeColoringStrategy(Map<VID, AnnotationState> aColors)
    {
        return new ColoringStrategy()
        {
            @Override
            public String getColor(VID aVid, String aLabel)
            {
                if (aColors.get(aVid) == null) {
                    return AnnotationState.NOT_SUPPORTED.getColorCode();
                }
                return aColors.get(aVid).getColorCode();
            }
        };
    }

    /**
     * For each {@link ConfigurationSet}, where there are some differences in users annotation and
     * the curation annotation.
     */
    private void addSuggestionColor(Project aProject, Mode aMode, Map<String, CAS> aCasMap,
            Map<String, Map<VID, AnnotationState>> aSuggestionColors,
            Collection<ConfigurationSet> aCfgSet, boolean aI, boolean aAgree)
    {
        for (ConfigurationSet cs : aCfgSet) {
            boolean use = false;
            for (String u : cs.getCasGroupIds()) {
                Map<VID, AnnotationState> colors = aSuggestionColors.get(u);
                if (colors == null) {
                    colors = new HashMap<>();
                    aSuggestionColors.put(u, colors);
                }

                for (Configuration c : cs.getConfigurations(u)) {

                    FeatureStructure fs = c.getFs(u, aCasMap);
                    
                    AnnotationLayer layer = annotationService.getLayer(fs.getType().getName(),
                            aProject);
                    TypeAdapter typeAdapter = annotationService.getAdapter(layer);
                    
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
                        colors.put(vid, AnnotationState.AGREE);
                        continue;
                    }
                    // automation and correction projects
                    if (!aMode.equals(Mode.CURATION) && !aAgree) {
                        if (cs.getCasGroupIds().size() == 2) {
                            colors.put(vid, AnnotationState.DO_NOT_USE);
                        }
                        else {
                            colors.put(vid, AnnotationState.DISAGREE);
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
                        colors.put(vid, AnnotationState.AGREE);
                    }
                    else if (use) {
                        colors.put(vid, AnnotationState.USE);
                    }
                    else if (aI) {
                        colors.put(vid, AnnotationState.DISAGREE);
                    }
                    else if (!cs.getCasGroupIds().contains(CURATION_USER)) {
                        colors.put(vid, AnnotationState.DISAGREE);
                    }
                    else {
                        colors.put(vid, AnnotationState.DO_NOT_USE);
                    }
                }
            }
        }
    }

    private CAS getAnnotatorCas(
            AnnotatorState aBModel,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            SourceDocument sourceDocument,
            Map<String, CAS> jCases)
        throws UIMAException, IOException, ClassNotFoundException
    {
        CAS annotatorCas;
        if (aBModel.getMode().equals(Mode.AUTOMATION)
                || aBModel.getMode().equals(Mode.CORRECTION)) {
            // If this is a CORRECTION or AUTOMATION project, then we get the CORRECTION document
            // and put it in as the single document to compare with. Basically what we do is that
            // we treat consider this scenario as a curation scenario where the CORRECTION document
            // is the only document we compare with.

            // The CAS the user can edit is the one from the virtual CORRECTION USER
            annotatorCas = correctionDocumentService.readCorrectionCas(sourceDocument);

            User user = userRepository.getCurrentUser();
            AnnotationDocument annotationDocument = documentService.getAnnotationDocument(
                    sourceDocument, user);
            jCases.put(user.getUsername(), documentService.readAnnotationCas(annotationDocument));
            aAnnotationSelectionByUsernameAndAddress.put(CURATION_USER, new HashMap<>());
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
                    CAS jCas = documentService.readAnnotationCas(annotationDocument);
                    jCases.put(username, jCas);

                    // cleanup annotationSelections
                    aAnnotationSelectionByUsernameAndAddress.put(username,
                        new HashMap<>());
                }
            }
        }
        return annotatorCas;
    }
}
