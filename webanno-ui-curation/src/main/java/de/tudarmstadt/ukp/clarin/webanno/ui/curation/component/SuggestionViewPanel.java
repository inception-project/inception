/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.updateDocumentTimestampAfterWrite;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility.isDocumentFinished;
import static org.apache.uima.fit.util.CasUtil.select;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
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

import com.googlecode.wicket.jquery.ui.widget.menu.IMenuItem;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.BulkAnnotationEvent;
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
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.AlreadyMergedException;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMerge;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMergeOpertationResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.MergeConflictException;
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
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.BratSuggestionVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.UserAnnotationSegment;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A {@link MarkupContainer} for either curation users' sentence annotation (for the lower panel) or
 * the automated annotations
 */
public class SuggestionViewPanel
    extends WebMarkupContainer
{
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_ID = "id";
    private static final String PARAM_ACTION = "action";

    private static final String ACTION_CONTEXT_MENU = "contextMenu";
    private static final String ACTION_SELECT_ARC_FOR_MERGE = "selectArcForMerge";
    private static final String ACTION_SELECT_SPAN_FOR_MERGE = "selectSpanForMerge";

    private static final long serialVersionUID = 8736268179612831795L;

    private static final Logger LOG = LoggerFactory.getLogger(SuggestionViewPanel.class);

    private final ListView<UserAnnotationSegment> sentenceListView;
    private final ContextMenu contextMenu;

    private @SpringBean PreRenderer preRenderer;
    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean CorrectionDocumentService correctionDocumentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;

    public SuggestionViewPanel(String id, IModel<List<UserAnnotationSegment>> aModel)
    {
        super(id, aModel);
        setOutputMarkupId(true);

        contextMenu = new ContextMenu("contextMenu");
        add(contextMenu);

        sentenceListView = new ListView<UserAnnotationSegment>("sentenceListView", aModel)
        {
            private static final long serialVersionUID = -5389636445364196097L;

            @Override
            protected void populateItem(ListItem<UserAnnotationSegment> aItem)
            {
                final UserAnnotationSegment curationUserSegment = aItem.getModelObject();
                BratSuggestionVisualizer curationVisualizer = new BratSuggestionVisualizer(
                        "sentence", new Model<>(curationUserSegment), aItem.getIndex())
                {
                    private static final long serialVersionUID = -1205541428144070566L;

                    @Override
                    protected void onClientEvent(AjaxRequestTarget aTarget) throws Exception
                    {
                        SuggestionViewPanel.this.onClientEvent(aTarget, curationUserSegment);
                    }
                };
                curationVisualizer.setOutputMarkupId(true);
                aItem.add(curationVisualizer);
            }
        };
        sentenceListView.setOutputMarkupId(true);
        add(sentenceListView);
    }

    /**
     * Method is called, if user has clicked on a span or an arc in the sentence panel. The span or
     * arc respectively is identified and copied to the merge CAS.
     */
    protected void onClientEvent(AjaxRequestTarget aTarget, UserAnnotationSegment aSegment)
        throws UIMAException, IOException, AnnotationException
    {
        if (isDocumentFinished(documentService, aSegment.getAnnotatorState())) {
            error("This document is already closed. Please ask the project manager to re-open it.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        IRequestParameters request = getRequest().getPostParameters();
        StringValue action = request.getParameterValue(PARAM_ACTION);


        if (!action.isEmpty()) {
            String type = removePrefix(request.getParameterValue(PARAM_TYPE).toString());
            AnnotationLayer layer = annotationService.getLayer(TypeUtil.getLayerId(type));
            VID sourceVid = VID.parse(request.getParameterValue(PARAM_ID).toString());

            CAS targetCas = readEditorCas(aSegment.getAnnotatorState());
            CAS sourceCas = readAnnotatorCas(aSegment);
            AnnotatorState sourceState = aSegment.getAnnotatorState();

            if (CHAIN_TYPE.equals(layer.getType())) {
                error("Coreference annotations are not supported in curation");
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            if (ACTION_CONTEXT_MENU.equals(action.toString()) ) {
                // No bulk actions supports for slots at the moment.
                if (sourceVid.isSlotSet()) {
                    return;
                }
                
                List<IMenuItem> items = contextMenu.getItemList();
                items.clear();
                items.add(new LambdaMenuItem(String.format("Merge all %s", layer.getUiName()),
                    _target -> actionAcceptAll(_target, aSegment, layer)));

                contextMenu.onOpen(aTarget);
                return;
            }

            // check if clicked on a span
            CasMerge casMerge = new CasMerge(annotationService);
            if (ACTION_SELECT_SPAN_FOR_MERGE.equals(action.toString())) {
                mergeSpan(casMerge, targetCas, sourceCas, sourceVid, sourceState.getDocument(),
                        sourceState.getUser().getUsername(), layer);
            }
            // check if clicked on an arc (relation or slot)
            else if (ACTION_SELECT_ARC_FOR_MERGE.equals(action.toString())) {
                // this is a slot arc
                if (sourceVid.isSlotSet()) {
                    mergeSlot(casMerge, targetCas, sourceCas, sourceVid, sourceState.getDocument(),
                            sourceState.getUser().getUsername(), layer);
                }
                // normal relation annotation arc is clicked
                else {
                    mergeRelation(casMerge, targetCas, sourceCas, sourceVid,
                            sourceState.getDocument(), sourceState.getUser().getUsername(), layer);
                }
            }

            writeEditorCas(sourceState, targetCas);

            // Update timestamp
            AnnotationFS sourceAnnotation = selectAnnotationByAddr(sourceCas, sourceVid.getId());
            int sentenceNumber = getSentenceNumber(sourceAnnotation.getCAS(),
                    sourceAnnotation.getBegin());
            sourceState.getDocument().setSentenceAccessed(sentenceNumber);

            if (sourceState.getPreferences().isScrollPage()) {
                sourceState.getPagingStrategy().moveToOffset(sourceState, targetCas,
                        sourceAnnotation.getBegin(), CENTERED);
            }
            
            onChange(aTarget);
        }
    }

    private void actionAcceptAll(AjaxRequestTarget aTarget, UserAnnotationSegment aSegment,
            AnnotationLayer aLayer)
        throws IOException
    {
        CAS targetCas = readEditorCas(aSegment.getAnnotatorState());
        CAS sourceCas = readAnnotatorCas(aSegment);
        AnnotatorState sourceState = aSegment.getAnnotatorState();
        TypeAdapter adapter = annotationService.getAdapter(aLayer);

        int mergeConflict = 0;
        int alreadyMerged = 0;
        int updated = 0;
        int created = 0;
        Set<String> otherErrors = new LinkedHashSet<>();
        
        CasMerge casMerge = new CasMerge(annotationService);
        casMerge.setSilenceEvents(true);
        
        nextAnnotation: for (AnnotationFS ann : select(sourceCas,
                adapter.getAnnotationType(sourceCas))) {
            try {
                CasMergeOpertationResult result;

                switch (aLayer.getType()) {
                case SPAN_TYPE:
                    result = mergeSpan(casMerge, targetCas, sourceCas, new VID(ann),
                            sourceState.getDocument(), sourceState.getUser().getUsername(), aLayer);
                    break;
                case RELATION_TYPE:
                    result = mergeRelation(casMerge, targetCas, sourceCas, new VID(ann),
                            sourceState.getDocument(), sourceState.getUser().getUsername(), aLayer);
                    break;
                default:
                    continue nextAnnotation;
                }

                switch (result) {
                case CREATED:
                    created++;
                    break;
                case UPDATED:
                    updated++;
                    break;
                }
            }
            catch (AlreadyMergedException e) {
                alreadyMerged++;
            }
            catch (MergeConflictException e) {
                mergeConflict++;
            }
            catch (Exception e) {
                otherErrors.add(e.getMessage());
            }
        }
        
        writeEditorCas(sourceState, targetCas);
        
        int success = created + updated;
        if (success > 0) {
            success(String.format("Annotations were changed: %d (%d created, %d updated)",
                    success, created, updated));
        }
        else {
            info("No annotations were changed");
        }

        if (alreadyMerged > 0) {
            info("Annotations had already been merged: " + alreadyMerged);
        }

        if (mergeConflict > 0) {
            info("Annotations skipped due to conflicts: " + mergeConflict);
        }
        
        if (!otherErrors.isEmpty()) {
            otherErrors.forEach(this::error);
        }
        
        applicationEventPublisher.get()
                .publishEvent(new BulkAnnotationEvent(this, sourceState.getDocument(),
                        sourceState.getUser().getUsername(), adapter.getLayer()));
        
        aTarget.addChildren(getPage(), IFeedback.class);
        
        onChange(aTarget);
    }
    
    protected void onChange(AjaxRequestTarget aTarget)
    {
        // Overriden in curationPanel
    }

    private CasMergeOpertationResult mergeSpan(CasMerge aCasMerge, CAS aTargetCas, CAS aSourceCas,
            VID aSourceVid, SourceDocument aSourceDocument, String aSourceUser,
            AnnotationLayer aLayer)
        throws AnnotationException, UIMAException, IOException
    {
        AnnotationFS sourceAnnotation = selectAnnotationByAddr(aSourceCas, aSourceVid.getId());

        return aCasMerge.mergeSpanAnnotation(aSourceDocument, aSourceUser, aLayer, aTargetCas,
                sourceAnnotation, aLayer.isAllowStacking());
    }

    private void mergeSlot(CasMerge aCasMerge, CAS aCas, CAS aSourceCas, VID aSourceVid,
            SourceDocument aSourceDocument, String aSourceUser, AnnotationLayer aLayer)
        throws AnnotationException, IOException
    {
        AnnotationFS sourceAnnotation = selectAnnotationByAddr(aSourceCas, aSourceVid.getId());

        TypeAdapter adapter = annotationService.getAdapter(aLayer);
        AnnotationFeature feature = adapter.listFeatures().stream().sequential()
                .skip(aSourceVid.getAttribute()).findFirst().get();

        aCasMerge.mergeSlotFeature(aSourceDocument, aSourceUser, aLayer, aCas, sourceAnnotation,
                feature.getName(), aSourceVid.getSlot());
    }

    private CasMergeOpertationResult mergeRelation(CasMerge aCasMerge, CAS aCas, CAS aSourceCas,
            VID aSourceVid, SourceDocument aSourceDocument, String aSourceUser,
            AnnotationLayer aLayer)
        throws AnnotationException, IOException
    {
        AnnotationFS sourceAnnotation = selectAnnotationByAddr(aSourceCas, aSourceVid.getId());

        return aCasMerge.mergeRelationAnnotation(aSourceDocument, aSourceUser, aLayer, aCas,
                sourceAnnotation, aLayer.isAllowStacking());
    }

    private CAS readEditorCas(AnnotatorState aState) throws IOException
    {
        User user = userRepository.getCurrentUser();
        SourceDocument sourceDocument = aState.getDocument();
        return (aState.getMode().equals(Mode.AUTOMATION)
                || aState.getMode().equals(Mode.CORRECTION))
                        ? documentService.readAnnotationCas(
                                documentService.getAnnotationDocument(sourceDocument, user))
                        : curationDocumentService.readCurationCas(sourceDocument);
    }

    private void writeEditorCas(AnnotatorState state, CAS aCas)
        throws IOException
    {
        if (state.getMode().equals(Mode.ANNOTATION) || state.getMode().equals(Mode.AUTOMATION)
                || state.getMode().equals(Mode.CORRECTION)) {
            documentService.writeAnnotationCas(aCas, state.getDocument(), state.getUser(), true);

            updateDocumentTimestampAfterWrite(state, documentService
                    .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername()));
        }
        else if (state.getMode().equals(Mode.CURATION)) {
            curationDocumentService.writeCurationCas(aCas, state.getDocument(), true);

            updateDocumentTimestampAfterWrite(state,
                    curationDocumentService.getCurationCasTimestamp(state.getDocument()));
        }
    }

    private CAS readAnnotatorCas(UserAnnotationSegment aSegment) throws IOException
    {
        AnnotatorState state = aSegment.getAnnotatorState();

        if (state.getMode().equals(Mode.AUTOMATION) || state.getMode().equals(Mode.CORRECTION)) {
            return correctionDocumentService.readCorrectionCas(state.getDocument());
        }
        else {
            return documentService.readAnnotationCas(aSegment.getAnnotatorState().getDocument(),
                    aSegment.getUsername());
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

    private String render(CAS aCas, AnnotatorState aBratAnnotatorModel,
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
                aBratAnnotatorModel.getWindowEndOffset(), aCas, layersToRender);

        GetDocumentResponse response = new GetDocumentResponse();
        BratRenderer.render(response, aBratAnnotatorModel, vdoc, aCas, annotationService,
                aCurationColoringStrategy);
        return JSONUtil.toInterpretableJsonString(response);
    }

    private String getCollectionInformation(AnnotationSchemaService aAnnotationService,
            CurationContainer aCurationContainer)
        throws IOException
    {
        GetCollectionInformationResponse info = new GetCollectionInformationResponse();
        info.setEntityTypes(BratRenderer.buildEntityTypes(
                aCurationContainer.getState().getAnnotationLayers(), aAnnotationService));

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
        AnnotatorState state = aCurationContainer.getState();
        SourceDocument sourceDocument = state.getDocument();

        Map<String, CAS> casses = new HashMap<>();
        // This is the CAS that the user can actively edit
        CAS annotatorCas = getAnnotatorCas(state, aAnnotationSelectionByUsernameAndAddress,
                sourceDocument, casses);

        // We store the CAS that the user will edit as the "CURATION USER"
        casses.put(CURATION_USER, annotatorCas);

        // get differing feature structures
        Map<String, Map<VID, AnnotationState>> annoStates = calcColors(state, aCurationSegment,
                annotatorCas, casses);

        List<String> usernamesSorted = new ArrayList<>(casses.keySet());
        Collections.sort(usernamesSorted);

        final Mode mode = state.getMode();
        boolean isAutomationMode = mode.equals(Mode.AUTOMATION);
        boolean isCorrectionMode = mode.equals(Mode.CORRECTION);
        boolean isCurationMode = mode.equals(Mode.CURATION);

        List<UserAnnotationSegment> segments = new ArrayList<>();
        for (String username : usernamesSorted) {
            if ((!username.equals(CURATION_USER) && isCurationMode)
                    || (username.equals(CURATION_USER) && (isAutomationMode || isCorrectionMode))) {

                CAS cas = casses.get(username);

                // Set up coloring strategy
                ColoringStrategy curationColoringStrategy = makeColoringStrategy(
                        annoStates.get(username));

                // Create curation view for the current user
                UserAnnotationSegment seg = new UserAnnotationSegment();
                seg.setUsername(username);
                seg.setAnnotatorState(state);
                seg.setCollectionData(
                        getCollectionInformation(annotationService, aCurationContainer));
                seg.setDocumentResponse(render(cas, state, curationColoringStrategy));
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
    public void updatePanel(AjaxRequestTarget aTarget, CurationContainer aCurationContainer,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            SourceListView aCurationSegment)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        AnnotatorState state = aCurationContainer.getState();
        SourceDocument sourceDocument = state.getDocument();
        Map<String, CAS> casses = new HashMap<>();

        // This is the CAS that the user can actively edit
        CAS annotatorCas = getAnnotatorCas(state, aAnnotationSelectionByUsernameAndAddress,
                sourceDocument, casses);

        // We store the CAS that the user will edit as the "CURATION USER"
        casses.put(CURATION_USER, annotatorCas);

        // get differing feature structures
        Map<String, Map<VID, AnnotationState>> annoStates = calcColors(state, aCurationSegment,
                annotatorCas, casses);

        sentenceListView.visitChildren(BratSuggestionVisualizer.class, (v, visit) -> {
            BratSuggestionVisualizer vis = (BratSuggestionVisualizer) v;
            UserAnnotationSegment seg = vis.getModelObject();

            CAS cas = casses.get(seg.getUsername());

            // Set up coloring strategy
            ColoringStrategy curationColoringStrategy = makeColoringStrategy(
                    annoStates.get(seg.getUsername()));

            // Create curation view for the current user
            try {
                seg.setCollectionData(
                        getCollectionInformation(annotationService, aCurationContainer));
                seg.setDocumentResponse(render(cas, state, curationColoringStrategy));
                seg.setAnnotatorState(state);
                seg.setSelectionByUsernameAndAddress(aAnnotationSelectionByUsernameAndAddress);
            }
            catch (IOException e) {
                error("Unable to render: " + e.getMessage());
                LOG.error("Unable to render", e);
            }

            vis.requestRender(aTarget);
        });
    }

    private Map<String, Map<VID, AnnotationState>> calcColors(AnnotatorState state,
            SourceListView aCurationSegment, CAS annotatorCas, Map<String, CAS> aCasses)
    {
        // get differing feature structures
        List<Type> entryTypes = SuggestionBuilder.getEntryTypes(annotatorCas,
                state.getAnnotationLayers(), annotationService);

        Map<String, Map<VID, AnnotationState>> annoStates = new HashMap<>();

        DiffResult diff;
        if (state.getMode().equals(Mode.CURATION)) {
            diff = CasDiff.doDiffSingle(annotationService, state.getProject(), entryTypes,
                    LinkCompareBehavior.LINK_ROLE_AS_LABEL, aCasses,
                    aCurationSegment.getCurationBegin(), aCurationSegment.getCurationEnd());
        }
        else {
            diff = CasDiff.doDiffSingle(annotationService, state.getProject(), entryTypes,
                    LinkCompareBehavior.LINK_ROLE_AS_LABEL, aCasses, aCurationSegment.getBegin(),
                    aCurationSegment.getEnd());
        }

        Collection<ConfigurationSet> d = diff.getDifferingConfigurationSets().values();

        Collection<ConfigurationSet> i = diff.getIncompleteConfigurationSets().values();
        for (ConfigurationSet cfgSet : d) {
            if (i.contains(cfgSet)) {
                i.remove(cfgSet);
            }
        }

        addSuggestionColor(state.getProject(), state.getMode(), aCasses, annoStates, d, false,
                false);
        addSuggestionColor(state.getProject(), state.getMode(), aCasses, annoStates, i, true,
                false);

        List<ConfigurationSet> all = new ArrayList<>();
        all.addAll(diff.getConfigurationSets());
        all.removeAll(d);
        all.removeAll(i);

        addSuggestionColor(state.getProject(), state.getMode(), aCasses, annoStates, all, false,
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

                    AnnotationLayer layer = annotationService.findLayer(aProject,
                            fs.getType().getName());
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

    private CAS getAnnotatorCas(AnnotatorState aBModel,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            SourceDocument sourceDocument, Map<String, CAS> aCasses)
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
            AnnotationDocument annotationDocument = documentService
                    .getAnnotationDocument(sourceDocument, user);
            aCasses.put(user.getUsername(), documentService.readAnnotationCas(annotationDocument));
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
                    CAS cas = documentService.readAnnotationCas(annotationDocument);
                    aCasses.put(username, cas);

                    // cleanup annotationSelections
                    aAnnotationSelectionByUsernameAndAddress.put(username, new HashMap<>());
                }
            }
        }
        return annotatorCas;
    }
}
