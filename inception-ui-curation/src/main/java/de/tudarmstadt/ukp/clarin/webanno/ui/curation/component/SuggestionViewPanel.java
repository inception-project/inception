/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
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
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility.isDocumentFinished;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiffSingle;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getDiffAdapters;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_ROLE_AS_LABEL;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState.AGREE;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState.DISAGREE;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState.DO_NOT_USE;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState.USE;
import static org.apache.commons.lang3.StringUtils.isBlank;
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
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRules;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.BulkAnnotationEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratRenderer;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.AlreadyMergedException;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMerge;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMergeOperationResult;
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
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxComponentRespondListener;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.BratSuggestionVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SourceListView;
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
    private @SpringBean AnnotationSchemaService schemaService;
    private @SpringBean ColoringService coloringService;
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
            AnnotationLayer layer = schemaService.getLayer(TypeUtil.getLayerId(type));
            VID sourceVid = VID.parse(request.getParameterValue(PARAM_ID).toString());

            CAS targetCas = readEditorCas(aSegment.getAnnotatorState());
            CAS sourceCas = readAnnotatorCas(aSegment);
            AnnotatorState sourceState = aSegment.getAnnotatorState();

            if (CHAIN_TYPE.equals(layer.getType())) {
                error("Coreference annotations are not supported in curation");
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            if (ACTION_CONTEXT_MENU.equals(action.toString())) {
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
            CasMerge casMerge = new CasMerge(schemaService);
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

            AnnotationFS sourceAnnotation = selectAnnotationByAddr(sourceCas, sourceVid.getId());

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
        TypeAdapter adapter = schemaService.getAdapter(aLayer);

        int mergeConflict = 0;
        int alreadyMerged = 0;
        int updated = 0;
        int created = 0;
        Set<String> otherErrors = new LinkedHashSet<>();

        CasMerge casMerge = new CasMerge(schemaService);
        casMerge.setSilenceEvents(true);

        nextAnnotation: for (AnnotationFS ann : select(sourceCas,
                adapter.getAnnotationType(sourceCas))) {
            try {
                CasMergeOperationResult result;

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

                switch (result.getState()) {
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
            success(String.format("Annotations were changed: %d (%d created, %d updated)", success,
                    created, updated));
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

    private CasMergeOperationResult mergeSpan(CasMerge aCasMerge, CAS aTargetCas, CAS aSourceCas,
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

        TypeAdapter adapter = schemaService.getAdapter(aLayer);
        AnnotationFeature feature = adapter.listFeatures().stream().sequential()
                .skip(aSourceVid.getAttribute()).findFirst().get();

        aCasMerge.mergeSlotFeature(aSourceDocument, aSourceUser, aLayer, aCas, sourceAnnotation,
                feature.getName(), aSourceVid.getSlot());
    }

    private CasMergeOperationResult mergeRelation(CasMerge aCasMerge, CAS aCas, CAS aSourceCas,
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
        return curationDocumentService.readCurationCas(aState.getDocument());
    }

    private void writeEditorCas(AnnotatorState state, CAS aCas) throws IOException
    {
        switch (state.getMode()) {
        case ANNOTATION:
            documentService.writeAnnotationCas(aCas, state.getDocument(), state.getUser(), true);

            updateDocumentTimestampAfterWrite(state, documentService
                    .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername()));
            break;
        case CURATION:
            curationDocumentService.writeCurationCas(aCas, state.getDocument(), true);

            updateDocumentTimestampAfterWrite(state,
                    curationDocumentService.getCurationCasTimestamp(state.getDocument()));
            break;
        default:
            throw new IllegalStateException("Unknown mode [" + state.getMode() + "]");
        }
    }

    private CAS readAnnotatorCas(UserAnnotationSegment aSegment) throws IOException
    {
        return documentService.readAnnotationCas(aSegment.getAnnotatorState().getDocument(),
                aSegment.getUsername());
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
        BratRenderer renderer = new BratRenderer(schemaService, coloringService);
        renderer.render(response, aBratAnnotatorModel, vdoc, aCas, aCurationColoringStrategy);
        return JSONUtil.toInterpretableJsonString(response);
    }

    private String getCollectionInformation(AnnotationSchemaService aAnnotationService,
            CurationContainer aCurationContainer)
        throws IOException
    {
        GetCollectionInformationResponse info = new GetCollectionInformationResponse();
        info.setEntityTypes(
                BratRenderer.buildEntityTypes(aCurationContainer.getState().getProject(),
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
        List<DiffAdapter> adapters = getDiffAdapters(schemaService, state.getAnnotationLayers());

        Map<String, Map<VID, AnnotationState>> annoStates1 = new HashMap<>();

        Project project = state.getProject();
        Mode mode1 = state.getMode();

        DiffResult diff;
        if (mode1.equals(CURATION)) {
            diff = doDiffSingle(adapters, LINK_ROLE_AS_LABEL, casses,
                    aCurationSegment.getCurationBegin(), aCurationSegment.getCurationEnd())
                            .toResult();
        }
        else {
            diff = doDiffSingle(adapters, LINK_ROLE_AS_LABEL, casses, aCurationSegment.getBegin(),
                    aCurationSegment.getEnd()).toResult();
        }

        Collection<ConfigurationSet> d = diff.getDifferingConfigurationSets().values();

        Collection<ConfigurationSet> i = diff.getIncompleteConfigurationSets().values();
        for (ConfigurationSet cfgSet : d) {
            if (i.contains(cfgSet)) {
                i.remove(cfgSet);
            }
        }

        addSuggestionColor(project, mode1, casses, annoStates1, d, false, false);
        addSuggestionColor(project, mode1, casses, annoStates1, i, true, false);

        List<ConfigurationSet> all = new ArrayList<>();
        all.addAll(diff.getConfigurationSets());
        all.removeAll(d);
        all.removeAll(i);

        addSuggestionColor(project, mode1, casses, annoStates1, all, false, true);

        // get differing feature structures
        Map<String, Map<VID, AnnotationState>> annoStates = annoStates1;

        List<String> usernamesSorted = new ArrayList<>(casses.keySet());
        Collections.sort(usernamesSorted);

        final Mode mode = state.getMode();
        boolean isCurationMode = mode.equals(Mode.CURATION);

        List<UserAnnotationSegment> segments = new ArrayList<>();
        for (String username : usernamesSorted) {
            if ((!username.equals(CURATION_USER) && isCurationMode)) {
                CAS cas = casses.get(username);

                // Set up coloring strategy
                ColoringStrategy curationColoringStrategy = makeColoringStrategy(
                        annoStates.get(username));

                // Create curation view for the current user
                UserAnnotationSegment seg = new UserAnnotationSegment();
                seg.setUsername(username);
                seg.setAnnotatorState(state);
                seg.setCollectionData(getCollectionInformation(schemaService, aCurationContainer));
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
    private void updatePanel(AjaxRequestTarget aTarget, CurationContainer aCurationContainer,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            SourceListView aCurationSegment)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        LOG.trace("call update");
        AnnotatorState state = aCurationContainer.getState();

        if (state.getDocument() == null) {
            return;
        }

        SourceDocument sourceDocument = state.getDocument();
        Map<String, CAS> casses = new HashMap<>();

        // This is the CAS that the user can actively edit
        CAS annotatorCas = getAnnotatorCas(state, aAnnotationSelectionByUsernameAndAddress,
                sourceDocument, casses);

        // We store the CAS that the user will edit as the "CURATION USER"
        casses.put(CURATION_USER, annotatorCas);
        List<DiffAdapter> adapters = getDiffAdapters(schemaService, state.getAnnotationLayers());

        Map<String, Map<VID, AnnotationState>> annoStates = new HashMap<>();

        Project project = state.getProject();
        Mode mode = state.getMode();

        DiffResult diff;
        if (mode.equals(CURATION)) {
            diff = doDiffSingle(adapters, LINK_ROLE_AS_LABEL, casses,
                    aCurationSegment.getCurationBegin(), aCurationSegment.getCurationEnd())
                            .toResult();
        }
        else {
            diff = doDiffSingle(adapters, LINK_ROLE_AS_LABEL, casses, aCurationSegment.getBegin(),
                    aCurationSegment.getEnd()).toResult();
        }

        Collection<ConfigurationSet> d = diff.getDifferingConfigurationSets().values();

        Collection<ConfigurationSet> i = diff.getIncompleteConfigurationSets().values();
        for (ConfigurationSet cfgSet : d) {
            if (i.contains(cfgSet)) {
                i.remove(cfgSet);
            }
        }

        addSuggestionColor(project, mode, casses, annoStates, d, false, false);
        addSuggestionColor(project, mode, casses, annoStates, i, true, false);

        List<ConfigurationSet> all = new ArrayList<>();
        all.addAll(diff.getConfigurationSets());
        all.removeAll(d);
        all.removeAll(i);

        addSuggestionColor(project, mode, casses, annoStates, all, false, true);

        // get differing feature structures
        sentenceListView.visitChildren(BratSuggestionVisualizer.class, (v, visit) -> {
            BratSuggestionVisualizer vis = (BratSuggestionVisualizer) v;
            UserAnnotationSegment seg = vis.getModelObject();

            CAS cas = casses.get(seg.getUsername());

            if (cas == null) {
                // This may happen if a user has not yet finished document
                return;
            }

            // Set up coloring strategy
            ColoringStrategy curationColoringStrategy = makeColoringStrategy(
                    annoStates.get(seg.getUsername()));

            // Create curation view for the current user
            try {
                seg.setCollectionData(getCollectionInformation(schemaService, aCurationContainer));
                seg.setDocumentResponse(render(cas, state, curationColoringStrategy));
                seg.setAnnotatorState(state);
                seg.setSelectionByUsernameAndAddress(aAnnotationSelectionByUsernameAndAddress);
            }
            catch (IOException e) {
                error("Unable to render: " + e.getMessage());
                LOG.error("Unable to render", e);
            }

            if (isBlank(vis.getDocumentData())) {
                return;
            }

            vis.render(aTarget);
        });
    }

    private ColoringStrategy makeColoringStrategy(Map<VID, AnnotationState> aColors)
    {
        return new ColoringStrategy()
        {
            @Override
            public String getColor(VObject aVObject, String aLabel, ColoringRules aColoringRules)
            {
                if (aColors.get(aVObject.getVid()) == null) {
                    return AnnotationState.NOT_SUPPORTED.getColorCode();
                }
                return aColors.get(aVObject.getVid()).getColorCode();
            }
        };
    }

    /**
     * For each {@link ConfigurationSet}, where there are some differences in users annotation and
     * the curation annotation.
     */
    private void addSuggestionColor(Project aProject, Mode aMode, Map<String, CAS> aCasMap,
            Map<String, Map<VID, AnnotationState>> aSuggestionColors,
            Collection<ConfigurationSet> aCfgSet, boolean aDisagree, boolean aAgree)
    {
        for (ConfigurationSet cs : aCfgSet) {
            boolean use = false;
            for (String u : cs.getCasGroupIds()) {
                Map<VID, AnnotationState> colors = aSuggestionColors.computeIfAbsent(u,
                        k -> new HashMap<>());

                for (Configuration c : cs.getConfigurations(u)) {

                    FeatureStructure fs = c.getFs(u, aCasMap);

                    AnnotationLayer layer = schemaService.findLayer(aProject,
                            fs.getType().getName());
                    TypeAdapter typeAdapter = schemaService.getAdapter(layer);

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
                        colors.put(vid, AGREE);
                        continue;
                    }
                    // automation and correction projects
                    if (!aMode.equals(CURATION) && !aAgree) {
                        if (cs.getCasGroupIds().size() == 2) {
                            colors.put(vid, DO_NOT_USE);
                        }
                        else {
                            colors.put(vid, DISAGREE);
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
                        colors.put(vid, AGREE);
                    }
                    else if (use) {
                        colors.put(vid, USE);
                    }
                    else if (aDisagree) {
                        colors.put(vid, DISAGREE);
                    }
                    else if (!cs.getCasGroupIds().contains(CURATION_USER)) {
                        colors.put(vid, DISAGREE);
                    }
                    else {
                        colors.put(vid, DO_NOT_USE);
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
        // The CAS the user can edit is the one from the virtual CURATION USER
        CAS annotatorCas = curationDocumentService.readCurationCas(sourceDocument);

        // If this is a true CURATION then we get all the annotation documents from all the
        // active users.
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
        return annotatorCas;
    }

    /**
     * Schedules a update call for this panel via at the end of the given AJAX cycle. This method
     * can be called multiple times, even for the same annotation editor, but only resulting in a
     * single update and rendering call.
     */
    public final void requestUpdate(AjaxRequestTarget aTarget, CurationContainer aCurationContainer,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            SourceListView aCurationSegment)
    {
        LOG.trace("request update");
        aTarget.registerRespondListener(new AjaxComponentRespondListener(this, _target -> {
            updatePanel(_target, aCurationContainer, aAnnotationSelectionByUsernameAndAddress,
                    aCurationSegment);
        }));
    }
}
