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

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState.ACCEPTED_BY_CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState.ANNOTATORS_AGREE;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState.ANNOTATORS_DISAGREE;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState.ANNOTATORS_INCOMPLETE;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState.REJECTED_BY_CURATOR;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.SPAN_TYPE;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.fit.util.CasUtil.select;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.jquery.ui.widget.menu.IMenuItem;

import de.tudarmstadt.ukp.clarin.webanno.brat.schema.BratSchemaGenerator;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotatorSegmentState;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.render.AnnotationStateColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.render.CurationRenderer;
import de.tudarmstadt.ukp.inception.annotation.events.BulkAnnotationEvent;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.curation.merge.AlreadyMergedException;
import de.tudarmstadt.ukp.inception.curation.merge.CasMerge;
import de.tudarmstadt.ukp.inception.curation.merge.CasMergeOperationResult;
import de.tudarmstadt.ukp.inception.curation.merge.MergeConflictException;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.schema.api.feature.TypeUtil;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxComponentRespondListener;
import de.tudarmstadt.ukp.inception.support.wicket.ContextMenu;

/**
 * Panel with the annotator's annotations.
 */
public class AnnotatorsPanel
    extends Panel
{
    private static final Logger LOG = LoggerFactory.getLogger(AnnotatorsPanel.class);

    private static final String PARAM_TYPE = "type";
    private static final String PARAM_ID = "id";
    private static final String PARAM_ACTION = "action";

    private static final String ACTION_CONTEXT_MENU = "contextMenu";
    private static final String ACTION_SELECT_ARC_FOR_MERGE = "selectArcForMerge";
    private static final String ACTION_SELECT_SPAN_FOR_MERGE = "selectSpanForMerge";

    private static final long serialVersionUID = 8736268179612831795L;

    private final ListView<AnnotatorSegmentState> annotatorSegments;
    private final ContextMenu contextMenu;

    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean AnnotationSchemaService schemaService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;
    private @SpringBean UserDao userService;
    private @SpringBean CurationRenderer curationRenderer;
    private @SpringBean BratSchemaGenerator bratSchemaGenerator;
    private @SpringBean AnnotationSchemaProperties annotationEditorProperties;
    private @SpringBean DiffAdapterRegistry diffAdapterRegistry;

    public AnnotatorsPanel(String id, IModel<List<AnnotatorSegmentState>> aModel)
    {
        super(id, aModel);
        setOutputMarkupId(true);

        contextMenu = new ContextMenu("contextMenu");
        add(contextMenu);

        annotatorSegments = new ListView<AnnotatorSegmentState>("annotatorSegments", aModel)
        {
            private static final long serialVersionUID = -5389636445364196097L;

            @Override
            protected void populateItem(ListItem<AnnotatorSegmentState> aItem)
            {
                final AnnotatorSegmentState annotatorSegment = aItem.getModelObject();
                BratSuggestionVisualizer curationVisualizer = new BratSuggestionVisualizer(
                        "annotationViewer", new Model<>(annotatorSegment), aItem.getIndex())
                {
                    private static final long serialVersionUID = -1205541428144070566L;

                    @Override
                    protected void onClientEvent(AjaxRequestTarget aTarget) throws Exception
                    {
                        AnnotatorsPanel.this.onClientEvent(aTarget, annotatorSegment);
                    }
                };
                curationVisualizer.setOutputMarkupId(true);
                aItem.add(curationVisualizer);
            }
        };
        annotatorSegments.setOutputMarkupId(true);
        add(annotatorSegments);
    }

    /**
     * Method is called, if user has clicked on a span or an arc in the sentence panel. The span or
     * arc respectively is identified and copied to the merge CAS.
     */
    protected void onClientEvent(AjaxRequestTarget aTarget, AnnotatorSegmentState aSegment)
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
            CasMerge casMerge = new CasMerge(schemaService, applicationEventPublisher.get());
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

            AnnotationFS sourceAnnotation = ICasUtil.selectAnnotationByAddr(sourceCas,
                    sourceVid.getId());

            if (sourceState.getPreferences().isScrollPage()) {
                sourceState.getPagingStrategy().moveToOffset(sourceState, targetCas,
                        sourceAnnotation.getBegin(), CENTERED);
            }
        }
    }

    private void actionAcceptAll(AjaxRequestTarget aTarget, AnnotatorSegmentState aSegment,
            AnnotationLayer aLayer)
        throws IOException
    {
        var adapter = schemaService.getAdapter(aLayer);
        var sourceCas = readAnnotatorCas(aSegment);
        var maybeSourceType = adapter.getAnnotationType(sourceCas);
        if (maybeSourceType.isEmpty()) {
            return;
        }

        var targetCas = readEditorCas(aSegment.getAnnotatorState());
        var sourceState = aSegment.getAnnotatorState();

        var mergeConflict = 0;
        var alreadyMerged = 0;
        var updated = 0;
        var created = 0;
        var otherErrors = new LinkedHashSet<String>();

        var casMerge = new CasMerge(schemaService, applicationEventPublisher.get());
        casMerge.setSilenceEvents(true);

        nextAnnotation: for (var ann : select(sourceCas, maybeSourceType.get())) {
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

                switch (result.state()) {
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
    }

    private CasMergeOperationResult mergeSpan(CasMerge aCasMerge, CAS aTargetCas, CAS aSourceCas,
            VID aSourceVid, SourceDocument aSourceDocument, String aSourceUser,
            AnnotationLayer aLayer)
        throws AnnotationException, UIMAException, IOException
    {
        AnnotationFS sourceAnnotation = ICasUtil.selectAnnotationByAddr(aSourceCas,
                aSourceVid.getId());

        return aCasMerge.mergeSpanAnnotation(aSourceDocument, aSourceUser, aLayer, aTargetCas,
                sourceAnnotation);
    }

    private void mergeSlot(CasMerge aCasMerge, CAS aCas, CAS aSourceCas, VID aSourceVid,
            SourceDocument aSourceDocument, String aSourceUser, AnnotationLayer aLayer)
        throws AnnotationException, IOException
    {
        AnnotationFS sourceAnnotation = ICasUtil.selectAnnotationByAddr(aSourceCas,
                aSourceVid.getId());

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
        AnnotationFS sourceAnnotation = ICasUtil.selectAnnotationByAddr(aSourceCas,
                aSourceVid.getId());

        return aCasMerge.mergeRelationAnnotation(aSourceDocument, aSourceUser, aLayer, aCas,
                sourceAnnotation);
    }

    private CAS readEditorCas(AnnotatorState aState) throws IOException
    {
        return curationDocumentService.readCurationCas(aState.getDocument());
    }

    private void writeEditorCas(AnnotatorState state, CAS aCas) throws IOException
    {
        curationDocumentService.writeCurationCas(aCas, state.getDocument(), true);
        curationDocumentService.getCurationCasTimestamp(state.getDocument())
                .ifPresent(state::setAnnotationDocumentTimestamp);
    }

    private CAS readAnnotatorCas(AnnotatorSegmentState aSegment) throws IOException
    {
        return documentService.readAnnotationCas(aSegment.getAnnotatorState().getDocument(),
                AnnotationSet.forUser(aSegment.getUser()));
    }

    /**
     * Removes a prefix that is added to brat visualization for different color coded purpose.
     */
    private static String removePrefix(String aType)
    {
        return aType.replace("_(" + AnnotationState.ANNOTATORS_AGREE.name() + ")", "")
                .replace("_(" + AnnotationState.ACCEPTED_BY_CURATOR.name() + ")", "")
                .replace("_(" + AnnotationState.ANNOTATORS_DISAGREE.name() + ")", "")
                .replace("_(" + AnnotationState.REJECTED_BY_CURATOR.name() + ")", "")
                .replace("_(" + AnnotationState.ERROR.name() + ")", "");
    }

    /**
     * Initializes the user annotation segments later to be filled with content.
     */
    @SuppressWarnings("javadoc")
    public void init(AjaxRequestTarget aTarget, AnnotatorState aState) throws IOException
    {
        if (aState.getDocument() == null) {
            return;
        }

        Map<String, CAS> casses = getCasses(aState.getDocument());
        Map<String, Map<VID, AnnotationState>> annoStates = calculateAnnotationStates(aState,
                casses);

        List<AnnotatorSegmentState> segments = new ArrayList<>();
        for (String username : casses.keySet().stream().sorted().collect(toList())) {
            if (username.equals(CURATION_USER)) {
                continue;
            }

            CAS cas = casses.get(username);

            var annotationStates = annoStates.get(username);

            assert annotationStates != null : format(
                    "No annotation states for user [" + username + "]");

            // Create curation view for the current user
            AnnotatorSegmentState seg = new AnnotatorSegmentState();
            seg.setUser(userService.get(username));
            seg.setAnnotatorState(aState);
            renderSegment(aTarget, seg, cas, annotationStates);
            segments.add(seg);
        }

        annotatorSegments.setModelObject(segments);
        if (aTarget != null) {
            aTarget.add(this);
        }
    }

    private Map<String, CAS> getCasses(SourceDocument aDocument) throws IOException
    {
        var casses = new HashMap<String, CAS>();

        // This CAS is loaded writable - it is the one the annotations are merged into
        casses.put(CURATION_USER, curationDocumentService.readCurationCas(aDocument));

        // The source CASes from the annotators are all ready read-only / shared
        for (var user : curationDocumentService.listCuratableUsers(aDocument)) {
            casses.put(user.getUsername(),
                    documentService.readAnnotationCas(aDocument, AnnotationSet.forUser(user)));
        }

        return casses;
    }

    private Map<String, Map<VID, AnnotationState>> calculateAnnotationStates(AnnotatorState aState,
            Map<String, CAS> aCasses)
    {
        var adapters = diffAdapterRegistry.getDiffAdapters(aState.getAnnotationLayers());
        var diff = doDiff(adapters, aCasses, aState.getWindowBeginOffset(),
                aState.getWindowEndOffset()).toResult();

        var differingSets = diff.getDifferingConfigurationSetsWithExceptions(CURATION_USER)
                .values();
        var incompleteSets = diff.getIncompleteConfigurationSetsWithExceptions(CURATION_USER)
                .values();
        differingSets.removeAll(incompleteSets);

        var completeAgreementSets = new ArrayList<ConfigurationSet>();
        completeAgreementSets.addAll(diff.getConfigurationSets());
        completeAgreementSets.removeAll(differingSets);
        completeAgreementSets.removeAll(incompleteSets);

        var annoStates = new HashMap<String, Map<VID, AnnotationState>>();
        for (var casGroupId : aCasses.keySet()) {
            annoStates.put(casGroupId, new HashMap<>());
        }

        addSuggestionColor(aState.getProject(), aCasses, annoStates, differingSets,
                ConfigurationSetType.DISAGREEMENT_SET);
        addSuggestionColor(aState.getProject(), aCasses, annoStates, incompleteSets,
                ConfigurationSetType.INCOMPLETE_SET);
        addSuggestionColor(aState.getProject(), aCasses, annoStates, completeAgreementSets,
                ConfigurationSetType.COMPLETE_AGREEMENT_SET);

        return annoStates;
    }

    /**
     * @param aTarget
     *            the AJAX target.
     * @throws IOException
     *             hum?
     */
    private void updatePanel(AjaxRequestTarget aTarget, AnnotatorState aState) throws IOException
    {
        if (aState.getDocument() == null) {
            return;
        }

        var casses = getCasses(aState.getDocument());
        var annoStates = calculateAnnotationStates(aState, casses);

        // get differing feature structures
        annotatorSegments.visitChildren(BratSuggestionVisualizer.class, (v, visit) -> {
            var vis = (BratSuggestionVisualizer) v;
            var seg = vis.getModelObject();

            var cas = casses.get(seg.getUser().getUsername());

            if (cas == null) {
                // This may happen if a user has not yet finished document
                return;
            }

            var annotationStates = annoStates.get(seg.getUser().getUsername());
            seg.setAnnotatorState(aState);
            renderSegment(aTarget, seg, cas, annotationStates);

            if (isBlank(vis.getDocumentData())) {
                return;
            }

            vis.render(aTarget);
        });
    }

    private void renderSegment(AjaxRequestTarget aTarget, AnnotatorSegmentState aSegment, CAS aCas,
            Map<VID, AnnotationState> aAnnotationStates)
    {
        Validate.notNull(aAnnotationStates, "Parameter [aAnnotationStates] must not be null");

        // Create curation view for the current user
        try {
            var aState = aSegment.getAnnotatorState();
            var coloringStrategy = new AnnotationStateColoringStrategy(aAnnotationStates);
            var vDoc = curationRenderer.render(aCas, aState, coloringStrategy);
            aSegment.setVDocument(vDoc);
        }
        catch (Exception e) {
            // Cannot include the username in the messages logged to the user because the
            // curation might be anonymous and then we would leak the true name
            error("Unable to render: " + e.getMessage());
            LOG.error("Unable to render annotations for user {}", aSegment.getUser(), e);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    /**
     * For each {@link ConfigurationSet}, where there are some differences in users annotation and
     * the curation annotation.
     */
    private void addSuggestionColor(Project aProject, Map<String, CAS> aCasMap,
            Map<String, Map<VID, AnnotationState>> aAnnotationStatesForAllUsers,
            Collection<ConfigurationSet> aConfigurationSets, ConfigurationSetType aSetType)
    {
        for (ConfigurationSet configurationSet : aConfigurationSets) {
            for (String user : configurationSet.getCasGroupIds()) {
                if (CURATION_USER.equals(user)) {
                    continue;
                }

                Map<VID, AnnotationState> annotationStates = aAnnotationStatesForAllUsers.get(user);

                for (var configuration : configurationSet.getConfigurations(user)) {
                    for (var fs : configuration.getFses(user, aCasMap)) {
                        VID vid;
                        // link FS
                        if (configuration.getPosition().getLinkFeature() != null) {
                            var typeAdapter = schemaService.findAdapter(aProject, fs);
                            int fi = 0;
                            for (var f : typeAdapter.listFeatures()) {
                                if (f.getName()
                                        .equals(configuration.getPosition().getLinkFeature())) {
                                    break;
                                }
                                fi++;
                            }

                            vid = new VID(ICasUtil.getAddr(fs), fi,
                                    configuration.getAID(user).index);
                        }
                        else {
                            vid = new VID(ICasUtil.getAddr(fs));
                        }

                        // The curator has accepted this configuration
                        if (configuration.getCasGroupIds().contains(CURATION_USER)) {
                            annotationStates.put(vid, ACCEPTED_BY_CURATOR);
                            continue;
                        }

                        // The curator has accepted *another* configuration in this set
                        if (configurationSet.getCasGroupIds().contains(CURATION_USER)) {
                            annotationStates.put(vid, REJECTED_BY_CURATOR);
                            continue;
                        }

                        // All annotators participated and agree but the curator did not make a
                        // decision
                        // yet.
                        if (aSetType == ConfigurationSetType.COMPLETE_AGREEMENT_SET) {
                            annotationStates.put(vid, ANNOTATORS_AGREE);
                            continue;
                        }

                        // Annotators disagree and the curator has not made a choice yet
                        if (aSetType == ConfigurationSetType.DISAGREEMENT_SET) {
                            annotationStates.put(vid, ANNOTATORS_DISAGREE);
                            continue;
                        }

                        // Annotation is incomplete and the curator has not made a choice yet
                        annotationStates.put(vid, ANNOTATORS_INCOMPLETE);
                    }
                }
            }
        }
    }

    /**
     * Schedules a update call for this panel via at the end of the given AJAX cycle. This method
     * can be called multiple times, even for the same annotation editor, but only resulting in a
     * single update and rendering call.
     */
    @SuppressWarnings("javadoc")
    public final void requestRender(AjaxRequestTarget aTarget, AnnotatorState aState)
    {
        LOG.trace("request update");
        aTarget.registerRespondListener(new AjaxComponentRespondListener(this, _target -> {
            updatePanel(_target, aState);
        }));
    }

    private boolean isDocumentFinished(DocumentService aRepository, AnnotatorState aState)
    {
        try {
            // Load document freshly from DB so we get the latest state. The document state
            // in the annotator state might be stale.
            SourceDocument doc = aRepository.getSourceDocument(
                    aState.getDocument().getProject().getId(), aState.getDocument().getId());
            return doc.getState() == CURATION_FINISHED;
        }
        catch (Exception e) {
            return false;
        }
    }

    private static enum ConfigurationSetType
    {
        /**
         * Annotators do not agree.
         */
        DISAGREEMENT_SET,
        /**
         * Some annotators did not provide an annotation for this set.
         */
        INCOMPLETE_SET,
        /**
         * All annotators have participated (not {@link #INCOMPLETE_SET}) and they all agree (not
         * {@link #DISAGREEMENT_SET}).
         */
        COMPLETE_AGREEMENT_SET
    }
}
