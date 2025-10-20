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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationEditorManagerPrefs.KEY_ANNOTATION_EDITOR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnchoringModePrefs.KEY_ANCHORING_MODE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.COREFERENCE_TYPE_FEATURE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.isSame;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.CasUtil.selectAt;
import static wicket.contrib.input.events.EventType.click;
import static wicket.contrib.input.events.key.KeyType.Delete;
import static wicket.contrib.input.events.key.KeyType.Escape;
import static wicket.contrib.input.events.key.KeyType.Left;
import static wicket.contrib.input.events.key.KeyType.Right;
import static wicket.contrib.input.events.key.KeyType.Shift;
import static wicket.contrib.input.events.key.KeyType.Space;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ConstraintsEvaluator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.RulesIndicator;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.events.BulkAnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.CreateRelationAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.selection.SelectionChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.AttachedAnnotation;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import jakarta.persistence.NoResultException;
import wicket.contrib.input.events.key.KeyType;

/**
 * Annotation Detail Editor Panel.
 */
public abstract class AnnotationDetailEditorPanel
    extends GenericPanel<AnnotatorState>
    implements AnnotationActionHandler
{
    private static final long serialVersionUID = 7324241992353693848L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaProperties schemaProperties;
    private @SpringBean UserDao userService;
    private @SpringBean PreferencesService preferencesService;

    // Top-level containers
    private final LayerSelectionPanel layerSelectionPanel;
    private final FeatureEditorListPanel featureEditorListPanel;
    private final WebMarkupContainer buttonContainer;
    private final WebMarkupContainer navContainer;
    private final AttachedAnnotationListPanel relationListPanel;

    // Components
    private final BootstrapModalDialog confirmationDialog;
    private final AnnotationPageBase editorPage;

    public AnnotationDetailEditorPanel(String id, AnnotationPageBase aPage,
            IModel<AnnotatorState> aModel)
    {
        super(id, new CompoundPropertyModel<>(aModel));

        editorPage = aPage;

        setOutputMarkupPlaceholderTag(true);
        setMarkupId("annotationDetailEditorPanel");

        queue(new WebMarkupContainer("header")
                .add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet())));
        queue(new Label("layerType", getModel().map(m -> m.getSelectedAnnotationLayer())
                .map(l -> l.getType()).map(t -> getString(t))).setOutputMarkupId(true));

        confirmationDialog = new BootstrapModalDialog("deleteAnnotationDialog");
        confirmationDialog.trapFocus();
        queue(confirmationDialog);

        queue(layerSelectionPanel = new LayerSelectionPanel("layerContainer", getModel()));
        queue(new AnnotationInfoPanel("infoContainer", getModel(), this));
        queue(featureEditorListPanel = new FeatureEditorListPanel("featureEditorListPanel",
                getModel(), this));
        queue(relationListPanel = new AttachedAnnotationListPanel("relationListContainer", aPage,
                this, aModel));
        relationListPanel.setOutputMarkupPlaceholderTag(true);

        buttonContainer = new WebMarkupContainer("buttonContainer");
        buttonContainer.setOutputMarkupPlaceholderTag(true);
        queue(createDeleteButton());
        queue(createReverseButton());
        queue(createClearButton());
        queue(buttonContainer);

        navContainer = new WebMarkupContainer("navContainer");
        navContainer
                .add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()));
        navContainer.setOutputMarkupPlaceholderTag(true);
        queue(createNextAnnotationButton());
        queue(createPreviousAnnotationButton());
        queue(navContainer);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(
                JavaScriptHeaderItem.forReference(AnnotationDetailEditorJSResourceReference.get()));
    }

    private LambdaAjaxLink createNextAnnotationButton()
    {
        var link = new LambdaAjaxLink("nextAnnotation", this::actionNextAnnotation);
        link.add(new InputBehavior(new KeyType[] { Shift, Right }, click));
        return link;
    }

    private LambdaAjaxLink createPreviousAnnotationButton()
    {
        var link = new LambdaAjaxLink("previousAnnotation", this::actionPreviousAnnotation);
        link.add(new InputBehavior(new KeyType[] { Shift, Left }, click));
        return link;
    }

    private void actionNextAnnotation(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        var sel = getModelObject().getSelection();
        if (!sel.isSet()) {
            return;
        }

        var cas = getEditorCas();
        var cur = selectByAddr(cas, AnnotationFS.class, sel.getAnnotation().getId());
        var next = WebAnnoCasUtil.getNext(cur);

        if (next != null) {
            actionSelectAndJump(aTarget, next);
        }
        else {
            info("There is no next annotation");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private void actionPreviousAnnotation(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        var sel = getModelObject().getSelection();
        if (!sel.isSet()) {
            return;
        }

        var cas = getEditorCas();
        var cur = selectByAddr(cas, AnnotationFS.class, sel.getAnnotation().getId());
        var prev = WebAnnoCasUtil.getPrev(cur);

        if (prev != null) {
            actionSelectAndJump(aTarget, prev);
        }
        else {
            info("There is no previous annotation");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private Selection createNewAnnotation(AjaxRequestTarget aTarget, TypeAdapter aAdapter, CAS aCas)
        throws AnnotationException, IOException
    {
        var state = getModelObject();
        var selection = state.getSelection();

        if (state.getSelection().isArc()) {
            var originFs = selectAnnotationByAddr(aCas, selection.getOrigin());
            var targetFs = selectAnnotationByAddr(aCas, selection.getTarget());
            var request = new CreateRelationAnnotationRequest(state.getDocument(),
                    state.getUser().getUsername(), aCas, originFs, targetFs);

            if (aAdapter instanceof RelationAdapter relationAdapter) {
                var ann = relationAdapter.handle(request);
                return relationAdapter.select(VID.of(ann), ann);
            }

            if (aAdapter instanceof ChainAdapter chainAdapter) {
                var ann = chainAdapter.handle(request);
                return chainAdapter.selectLink(ann);
            }

            throw new IllegalStateException("I don't know how to use ["
                    + aAdapter.getClass().getSimpleName() + "] in this situation.");
        }

        var request = CreateSpanAnnotationRequest.builder() //
                .withDocument(state.getDocument(), state.getUser().getUsername(), aCas) //
                .withRange(selection.getBegin(), selection.getEnd()) //
                .build();

        if (aAdapter instanceof SpanAdapter spanAdapter) {
            var ann = spanAdapter.handle(request);
            return spanAdapter.select(VID.of(ann), ann);
        }

        if (aAdapter instanceof ChainAdapter chainAdapter) {
            var ann = chainAdapter.handle(request);
            return chainAdapter.selectSpan(ann);
        }

        throw new IllegalStateException("I don't know how to use ["
                + aAdapter.getClass().getSimpleName() + "] in this situation.");
    }

    @Override
    public void actionFillSlot(AjaxRequestTarget aTarget, CAS aCas, int aSlotFillerBegin,
            int aSlotFillerEnd)
        throws AnnotationException, IOException
    {
        var state = getModelObject();

        if (CAS.TYPE_NAME_ANNOTATION.equals(state.getArmedFeature().feature.getType())) {
            throw new IllegalPlacementException(
                    "Unable to create annotation of type [" + CAS.TYPE_NAME_ANNOTATION
                            + "]. Please click an annotation in stead of selecting new text.");
        }

        var adapter = (SpanAdapter) annotationService.getAdapter(annotationService
                .findLayer(state.getProject(), state.getArmedFeature().feature.getType()));

        var slotFillerVid = VID.of(adapter.handle(CreateSpanAnnotationRequest.builder() //
                .withDocument(state.getDocument(), state.getUser().getUsername(), aCas) //
                .withRange(aSlotFillerBegin, aSlotFillerEnd) //
                .withAnchoringMode(state.getAnchoringMode()) //
                .build()));

        actionFillSlot(aTarget, aCas, slotFillerVid);
    }

    @Override
    public void actionFillSlot(AjaxRequestTarget aTarget, CAS aCas, VID aExistingSlotFillerId)
        throws AnnotationException, IOException
    {
        assert aCas != null;

        var state = getModelObject();

        editorPage.ensureIsEditable();

        // If this method is called when no slot is armed, it must be a bug!
        if (!state.isSlotArmed()) {
            throw new IllegalStateException("No slot is armed.");
        }

        // If the user did not select an existing annotation but simply marked a span of text to
        // fill a slot, then we try to create a new span annotation corresponding to the slot
        // filler type defined in the feature. However, this only works if the slot filler is a
        // concrete span type defined in the project, not if the user simply defined
        // CAS.TYPE_NAME_ANNOTATION to allow for arbitrary slot fillers. In the latter case, we
        // abort the operation with an IllegalPlacementException.
        var slotFillerAddr = aExistingSlotFillerId.getId();

        // Inject the slot filler into the respective slot
        var armedFeature = state.getArmedFeature();
        var slotHostFS = selectFsByAddr(aCas, armedFeature.vid.getId());
        var slotHostLayer = annotationService.findLayer(state.getProject(), slotHostFS);
        var slotHostAdapter = annotationService.getAdapter(slotHostLayer);
        @SuppressWarnings("unchecked")
        var links = (List<LinkWithRoleModel>) armedFeature.value;
        var link = links.get(state.getArmedSlot());
        link.targetAddr = slotFillerAddr;
        link.label = selectAnnotationByAddr(aCas, slotFillerAddr).getCoveredText();
        commitFeatureStates(aTarget, state.getDocument(), state.getUser().getUsername(), aCas,
                armedFeature.vid.getId(), slotHostAdapter, asList(armedFeature));

        // NOTE: we do NOT delegate to actionCreateOrUpdate here because most of the things
        // that actionCreateOrUpdate does are not required for slot filling and we also because
        // slot filling requires special treatment. This also means, we don't delegate to
        // internalCommitAnnotation and repeat the necessary code here. However, we DO delegate
        // to internalCompleteAnnotation to save the CAS after the annotation.

        internalCompleteAnnotation(aTarget, aCas);

        // If the armed slot is located in the annotation detail editor panel (right side) update
        // the annotator state with the changes that we made to the CAS
        if (state.getSelection().getAnnotation().equals(armedFeature.vid)) {
            // Loading feature editor values from CAS
            loadFeatureEditorModels(aTarget);
        }
        // ... if the SLOT HOST annotation is NOT open in the detail panel on the right, then
        // select SLOT FILLER an open it there
        else {
            state.getSelection().selectSpan(selectAnnotationByAddr(aCas, slotFillerAddr));
            actionSelect(aTarget);
        }

        state.clearArmedSlot();
    }

    @Override
    public void actionSelect(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        loadFeatureEditorModels(aTarget);

        if (aTarget != null) {
            refresh(aTarget);
        }
    }

    @Deprecated
    @Override
    public void actionSelect(AjaxRequestTarget aTarget, AnnotationFS annoFs)
        throws IOException, AnnotationException
    {
        actionSelect(aTarget, new VID(annoFs));
    }

    @Override
    public void actionSelect(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        var annoFs = selectAnnotationByAddr(editorPage.getEditorCas(), aVid.getId());
        var state = getModelObject();

        var adapter = annotationService
                .getAdapter(annotationService.findLayer(state.getProject(), annoFs));

        state.getSelection().set(adapter.select(aVid, annoFs));
        actionSelect(aTarget);
    }

    @Deprecated
    @Override
    public void actionJump(AjaxRequestTarget aTarget, AnnotationFS aFS)
        throws IOException, AnnotationException
    {
        editorPage.actionShowSelectedDocument(aTarget, getModelObject().getDocument(),
                aFS.getBegin(), aFS.getEnd());
    }

    @Override
    public void actionJump(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        actionJump(aTarget, selectAnnotationByAddr(editorPage.getEditorCas(), aVid.getId()));
    }

    @Override
    public void actionJump(AjaxRequestTarget aTarget, int aBegin, int aEnd)
        throws IOException, AnnotationException
    {
        editorPage.actionShowSelectedDocument(aTarget, getModelObject().getDocument(), aBegin,
                aEnd);
    }

    @Deprecated
    @Override
    public void actionSelectAndJump(AjaxRequestTarget aTarget, AnnotationFS annoFs)
        throws IOException, AnnotationException
    {
        actionSelect(aTarget, annoFs);
        editorPage.actionShowSelectedDocument(aTarget, getModelObject().getDocument(),
                annoFs.getBegin(), annoFs.getEnd());
    }

    @Override
    public void actionSelectAndJump(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        var cas = editorPage.getEditorCas();
        var targetFs = ICasUtil.selectFsByAddr(cas, aVid.getId());
        if (targetFs instanceof AnnotationFS) {
            actionSelectAndJump(aTarget, (AnnotationFS) targetFs);
        }
    }

    @Override
    @Deprecated
    public void actionCreateOrUpdate(AjaxRequestTarget aTarget, CAS aCas)
        throws IOException, AnnotationException
    {
        LOG.trace("actionAnnotate");

        editorPage.ensureIsEditable();

        var state = getModelObject();
        if (!state.getSelection().isSet()) {
            return;
        }

        // Creating or updating an annotation should not change the current default layer - even
        // though it might temporarily do so as e.g. a relation is created.
        var savedDefaultLayer = state.getDefaultAnnotationLayer();

        // Note that refresh changes the selected layer if a relation is created. Then the layer
        // switches from the selected span layer to the relation layer that is attached to the span
        try {
            if (state.getSelection().isArc()) {
                prepareCreateOrUpdateRelation(aTarget, aCas, state);
            }
            else {
                // Re-set the selected layer from the drop-down since it might have changed if we
                // have previously created a relation annotation
                state.setSelectedAnnotationLayer(state.getDefaultAnnotationLayer());
            }

            // Can check state only now because the methods above may juggle the selection.. argh
            if (state.getSelectableLayers().isEmpty()) {
                info("No text-level annotation layers are available in this project.");
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            if (state.getSelectedAnnotationLayer() == null) {
                error("No layer is selected. First select a layer.");
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            if (state.getSelectedAnnotationLayer().isReadonly()) {
                error("Layer is not editable.");
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            LOG.trace("actionAnnotate() selectedLayer: {}",
                    state.getSelectedAnnotationLayer().getUiName());
            LOG.trace("actionAnnotate() defaultLayer: {}",
                    state.getDefaultAnnotationLayer().getUiName());

            internalCommitAnnotation(aTarget, aCas);

            internalCompleteAnnotation(aTarget, aCas);

            if (aTarget != null) {
                refresh(aTarget);
            }

            state.clearArmedSlot();
        }
        finally {
            state.setDefaultAnnotationLayer(savedDefaultLayer);
        }
    }

    /**
     * @deprecated To be removed without replacement.
     */
    @Deprecated
    private void prepareCreateOrUpdateRelation(AjaxRequestTarget aTarget, CAS aCas,
            AnnotatorState state)
        throws IllegalPlacementException, IOException, AnnotationException
    {
        LOG.trace("actionAnnotate() relation annotation - looking for attached layer");

        // FIXME REC I think this whole section which meddles around with the selected
        // annotation layer should be moved out of there to the place where we originally
        // set the annotation layer...!

        // Fetch the annotation representing the origin endpoint of the relation
        var originFS = selectAnnotationByAddr(aCas, state.getSelection().getOrigin());
        var targetFS = selectAnnotationByAddr(aCas, state.getSelection().getTarget());

        if (!schemaProperties.isCrossLayerRelationsEnabled()
                && !originFS.getType().equals(targetFS.getType())) {
            reset(aTarget);
            throw new IllegalPlacementException(
                    "Cannot create relation between spans on different layers");
        }

        // Fetch the annotation layer for the origin annotation
        var originLayer = annotationService.findLayer(state.getProject(), originFS);

        var previousLayer = state.getSelectedAnnotationLayer();

        // If we are creating a relation annotation, we have to set the current layer
        // depending on the type of relation that is permitted between the source/target
        // span. This is necessary because we have no separate UI control to set the
        // relation annotation type.
        // It is possible because currently only a single relation layer is allowed to
        // attach to any given span layer.

        // If we drag an arc in a chain layer, then the arc is of the same layer as the span
        // Chain layers consist of arcs and spans
        if (ChainLayerSupport.TYPE.equals(originLayer.getType())) {
            // one layer both for the span and arc annotation
            state.setSelectedAnnotationLayer(originLayer);
        }
        // Otherwise, look up the possible relation layer(s) in the database.
        else {
            var viableRelationLayers = annotationService.getRelationLayersFor(originLayer);
            if (viableRelationLayers.isEmpty()) {
                throw new IllegalPlacementException(
                        "There are no relation layers that can be created between these endpoints");
            }
            if (viableRelationLayers.size() == 1) {
                var relationLayer = viableRelationLayers.get(0);
                state.setSelectedAnnotationLayer(relationLayer);
            }
        }

        state.setDefaultAnnotationLayer(originLayer);

        // If we switched layers, we need to initialize the feature editors for the new layer
        if (!Objects.equals(previousLayer, state.getSelectedAnnotationLayer())) {
            LOG.trace("Layer changed from {} to {} - need to reload feature editors", previousLayer,
                    state.getSelectedAnnotationLayer());
            loadFeatureEditorModels(aTarget);
        }
    }

    /**
     * Persists the potentially modified CAS, remembers feature values, reloads the feature editors
     * using the latest info from the CAS, updates the sentence number and focus unit, performs
     * auto-scrolling.
     */
    void internalCompleteAnnotation(AjaxRequestTarget aTarget, CAS aCas)
        throws IOException, AnnotationException
    {
        var state = getModelObject();

        // persist changes
        editorPage.writeEditorCas(aCas);

        // Remember the current feature values independently for spans and relations
        state.rememberFeatures();

        // Loading feature editor values from CAS
        loadFeatureEditorModels(aTarget);

        autoScroll(aCas);

        state.clearArmedSlot();
    }

    /**
     * Creates an annotation using the information from the feature editors.
     */
    private void internalCommitAnnotation(AjaxRequestTarget aTarget, CAS aCas)
        throws AnnotationException, IOException
    {
        var state = getModelObject();

        // internalCommitAnnotation is used to update an existing annotation as well as to create
        // a new one. In either case, the selectedAnnotationLayer indicates the layer type! Do not
        // use the defaultAnnotationLayer here as e.g. when creating relation annotations, it would
        // point to the span type to which the relation attaches, not to the relation type!
        var adapter = annotationService.getAdapter(state.getSelectedAnnotationLayer());

        // If no annotation is selected, then we assume this is an annotation creation action,
        // create the annotation
        if (state.getSelection().getAnnotation().isNotSet()) {
            // Load the feature editors with the remembered values (if any)
            loadFeatureEditorModels(aTarget);
            var selection = createNewAnnotation(aTarget, adapter, aCas);
            state.getSelection().set(selection);
            loadFeatureEditorModels(aTarget);
        }

        // Update the features of the selected annotation from the values presently in the
        // feature editors
        commitFeatureStates(aTarget, state.getDocument(), state.getUser().getUsername(), aCas,
                state.getSelection().getAnnotation().getId(), adapter, state.getFeatureStates());
    }

    /**
     * Commits the values from the given feature states into the annotation with the given target FS
     * address in the given target CAS using the provided type adapter.
     */
    void commitFeatureStates(AjaxRequestTarget aTarget, SourceDocument aDocment, String aDataOwner,
            CAS aTargetCas, int aTargetFsAddr, TypeAdapter aAdapter,
            List<FeatureState> aFeatureStates)
    {
        for (var featureState : aFeatureStates) {
            try {
                LOG.trace("Committing feature states to CAS: {} = {}",
                        featureState.feature.getUiName(), featureState.value);
                aAdapter.setFeatureValue(aDocment, aDataOwner, aTargetCas, aTargetFsAddr,
                        featureState.feature, featureState.value);
            }
            catch (Exception e) {
                error("Cannot set feature [" + featureState.feature.getUiName() + "]: "
                        + e.getMessage());
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }
    }

    private AttachStatus checkAttachStatus(AjaxRequestTarget aTarget, Project aProject,
            AnnotationFS aFS)
    {
        var layer = annotationService.findLayer(aProject, aFS);

        var attachStatus = new AttachStatus();

        var attachedRels = annotationService.getAttachedRels(layer, aFS);
        var attachedToReadOnlyRels = attachedRels.stream()
                .anyMatch(rel -> rel.getLayer().isReadonly());
        if (attachedToReadOnlyRels) {
            attachStatus.readOnlyAttached |= true;
        }
        attachStatus.attachCount += attachedRels.size();

        // We do not count these atm since they only exist for built-in layers and are not
        // visible in the UI for the user.
        // Set<AnnotationFS> attachedSpans = getAttachedSpans(aFS, layer);
        // boolean attachedToReadOnlySpans = attachedSpans.stream().anyMatch(relFS -> {
        // AnnotationLayer relLayer = annotationService.getLayer(aProject, relFS);
        // return relLayer.isReadonly();
        // });
        // if (attachedToReadOnlySpans) {
        // attachStatus.readOnlyAttached |= true;
        // }
        // attachStatus.attachCount += attachedSpans.size();

        var attachedLinks = annotationService.getAttachedLinks(layer, aFS);
        var attachedToReadOnlyLinks = attachedLinks.stream()
                .anyMatch(rel -> rel.getLayer().isReadonly());
        if (attachedToReadOnlyLinks) {
            attachStatus.readOnlyAttached |= true;
        }
        attachStatus.attachCount += attachedLinks.size();

        return attachStatus;
    }

    @Override
    public void actionDelete(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        var state = getModelObject();
        if (state.getSelection().getAnnotation().isNotSet()) {
            error("No annotation selected.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var cas = getEditorCas();

        var vid = state.getSelection().getAnnotation();
        var fs = selectAnnotationByAddr(cas, vid.getId());
        var layer = annotationService.findLayer(state.getProject(), fs);
        var adapter = annotationService.getAdapter(layer);

        if (layer.isReadonly()) {
            error("Cannot delete an annotation on a read-only layer.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var attachStatus = checkAttachStatus(aTarget, state.getProject(), fs);
        if (attachStatus.readOnlyAttached) {
            error("Cannot delete an annotation to which annotations on read-only layers attach.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (adapter instanceof SpanAdapter && attachStatus.attachCount > 0) {
            var sessionOwner = userService.getCurrentUser();
            var confirmationPrefs = preferencesService.loadTraitsForUserAndProject(
                    KEY_ANNOTATION_EDITOR_MANAGER_PREFS, sessionOwner, state.getProject());

            if (confirmationPrefs.isShowDeleteAnnotationConfirmation()) {
                var dialogContent = new DeleteAnnotationConfirmationDialogPanel(
                        BootstrapModalDialog.CONTENT_ID, Model.of(layer), Model.of(attachStatus));
                dialogContent.setConfirmAction(_target -> doDelete(_target, layer, vid));
                confirmationDialog.open(dialogContent, aTarget);
                return;
            }
        }

        doDelete(aTarget, layer, vid);
    }

    private void doDelete(AjaxRequestTarget aTarget, AnnotationLayer layer, VID aVid)
        throws IOException, AnnotationException
    {
        CAS cas = getEditorCas();
        AnnotatorState state = getModelObject();
        TypeAdapter adapter = annotationService.getAdapter(layer);

        deleteAnnotation(cas, state, aVid, layer, adapter);

        // Store CAS again
        editorPage.writeEditorCas(cas);

        // Update progress information
        int sentenceNumber = getSentenceNumber(cas, state.getSelection().getBegin());
        state.setFocusUnitIndex(sentenceNumber);

        autoScroll(cas);

        state.rememberFeatures();

        reset(aTarget);
    }

    private void deleteAnnotation(CAS aCas, AnnotatorState state, VID aVid, AnnotationLayer layer,
            TypeAdapter adapter)
        throws AnnotationException
    {
        RequestCycle.get().find(AjaxRequestTarget.class)
                .ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));

        AnnotationFS fs = selectAnnotationByAddr(aCas, aVid.getId());

        // == DELETE ATTACHED SPANS ==
        // This case is currently not implemented because WebAnno currently does not allow to
        // create spans that attach to other spans. The only span type for which this is relevant
        // is the Token type which cannot be deleted.
        if (adapter instanceof SpanAdapter) {
            for (AnnotationFeature attachFeature : annotationService
                    .listAttachedSpanFeatures(adapter.getLayer())) {
                // AnnotationFS attachedFs = adapter.getFeatureValue(attachFeature, fs);
                AnnotationFS attachedFs = FSUtil.getFeature(fs, attachFeature.getName(),
                        AnnotationFS.class);
                if (attachedFs == null) {
                    continue;
                }

                TypeAdapter attachedSpanLayerAdapter = annotationService
                        .findAdapter(state.getProject(), attachedFs);

                deleteAnnotation(aCas, state, VID.of(attachedFs),
                        attachedSpanLayerAdapter.getLayer(), attachedSpanLayerAdapter);
            }
        }

        // == DELETE ATTACHED RELATIONS ==
        // If the deleted FS is a span, we must delete all relations that
        // point to it directly or indirectly via the attachFeature.
        if (adapter instanceof SpanAdapter) {
            for (AttachedAnnotation rel : annotationService.getAttachedRels(layer, fs)) {
                RelationAdapter relationAdapter = (RelationAdapter) annotationService
                        .findAdapter(state.getProject(), rel.getRelation());

                relationAdapter.delete(state.getDocument(), state.getUser().getUsername(), aCas,
                        VID.of(rel.getRelation()));
            }
        }

        // == CLEAN UP LINK FEATURES ==
        // If the deleted FS is a span that is the target of a link feature, we must unset that
        // link and delete the slot if it is a multi-valued link. Here, we have to scan all
        // annotations from layers that have link features that could point to the FS
        // to be deleted: the link feature must be the type of the FS or it must be generic.
        if (adapter instanceof SpanAdapter) {
            cleanUpLinkFeatures(aCas, fs, (SpanAdapter) adapter, state);
        }

        // If the deleted FS is a relation, we don't have to do anything. Nothing can point to a
        // relation.
        if (adapter instanceof RelationAdapter) {
            // Do nothing ;)
        }

        // Actually delete annotation
        adapter.delete(state.getDocument(), state.getUser().getUsername(), aCas, aVid);
    }

    private void cleanUpLinkFeatures(CAS aCas, FeatureStructure fs, SpanAdapter adapter,
            AnnotatorState state)
    {
        for (var linkFeature : annotationService.listAttachedLinkFeatures(adapter.getLayer())) {
            var linkHostType = CasUtil.getType(aCas, linkFeature.getLayer().getName());

            for (var linkHostFS : aCas.select(linkHostType)) {
                List<LinkWithRoleModel> links = adapter.getFeatureValue(linkFeature, linkHostFS);
                var i = links.iterator();
                var modified = false;
                while (i.hasNext()) {
                    var link = i.next();
                    if (link.targetAddr == getAddr(fs)) {
                        i.remove();
                        info("Cleared slot [" + link.role + "] in feature ["
                                + linkFeature.getUiName() + "] on ["
                                + linkFeature.getLayer().getUiName() + "]");
                        LOG.debug("Cleared slot [{}] in feature [{}] on annotation [{}]", link.role,
                                linkFeature.getName(), getAddr(linkHostFS));
                        modified = true;
                    }
                }
                if (modified) {
                    try {
                        adapter.setFeatureValue(state.getDocument(), state.getUser().getUsername(),
                                aCas, getAddr(linkHostFS), linkFeature, links);
                    }
                    catch (AnnotationException e) {
                        error("Unable to clean slots in feature [" + linkFeature.getUiName()
                                + "] on [" + linkFeature.getLayer().getUiName() + "]");
                        LOG.error("Unable to clean slots in feature [{}] on annotation [{}]",
                                linkFeature.getName(), getAddr(linkHostFS));
                    }

                    // If the currently armed slot is part of this link, then we disarm the slot
                    // to avoid the armed slot no longer pointing at the index which the user
                    // had selected it to point at.
                    var armedFeature = state.getArmedFeature();
                    if (armedFeature != null
                            && ICasUtil.getAddr(linkHostFS) == armedFeature.vid.getId()
                            && armedFeature.feature.equals(linkFeature)) {
                        state.clearArmedSlot();
                    }
                }
            }
        }
    }

    @Override
    public void actionReverse(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        // FIXME: This would be much better handled inside the RelationAdapter by simply reversing
        // the relation end-points instead of deleting/adding
        var state = getModelObject();

        var adapter = annotationService.getAdapter(state.getSelectedAnnotationLayer());
        if (!(adapter instanceof RelationAdapter)) {
            error("Only relations can be reversed");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var cas = getEditorCas();
        var document = state.getDocument();
        var selection = state.getSelection();
        var dataOwner = state.getUser().getUsername();

        // Remove old relation
        var oldRelation = selectAnnotationByAddr(cas, selection.getAnnotation().getId());
        adapter.delete(document, dataOwner, cas, selection.getAnnotation());

        // Create new relation with reversed endpoints
        var relAdapter = (RelationAdapter) adapter;
        var newSource = relAdapter.getTargetAnnotation(oldRelation);
        var newTarget = relAdapter.getSourceAnnotation(oldRelation);
        var newRelation = relAdapter.add(document, dataOwner, newSource, newTarget, cas);

        // Apply the features values of the old relation to the reversed relation
        commitFeatureStates(aTarget, document, dataOwner, cas, ICasUtil.getAddr(newRelation),
                adapter, getModelObject().getFeatureStates());

        internalCompleteAnnotation(aTarget, cas);

        selection.selectArc(newRelation);
    }

    @Override
    public void actionClear(AjaxRequestTarget aTarget) throws AnnotationException
    {
        reset(aTarget);
        aTarget.add(this);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    /**
     * Scroll the window of visible annotations if auto-scrolling is enabled.
     */
    private void autoScroll(CAS aCas)
    {
        var state = getModelObject();
        if (state.getPreferences().isScrollPage()) {
            getModelObject().moveToSelection(aCas);
        }
    }

    /**
     * Loads the feature states either from the CAS (if an annotation is selected) or from the
     * remembered values (if no annotation is selected).
     */
    private void loadFeatureEditorModels(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        LOG.trace("loadFeatureEditorModels()");

        var cas = getEditorCas();
        var state = getModelObject();
        var selection = state.getSelection();

        try {
            // If we reset the layers while doing a relation, we won't be able to complete the
            // relation - so in this case, we leave the layers alone...
            if (!selection.isArc()) {
                state.refreshSelectableLayers(schemaProperties::isLayerBlocked);

                if (state.getDefaultAnnotationLayer() != null) {
                    var sessionOwner = userService.getCurrentUser();
                    var anchoringPrefs = preferencesService.loadTraitsForUserAndProject(
                            KEY_ANCHORING_MODE, sessionOwner, state.getProject());
                    state.syncAnchoringModeToDefaultLayer(anchoringPrefs);
                }
            }

            if (selection.getAnnotation().isSet()) {
                // Updating existing annotation
                // Load feature states from existing annotation
                var anno = selectAnnotationByAddr(cas, selection.getAnnotation().getId());

                // Try obtaining the layer from the feature structure
                AnnotationLayer layer;
                try {
                    layer = annotationService.findLayer(state.getProject(), anno);
                    state.setSelectedAnnotationLayer(layer);
                    LOG.trace("loadFeatureEditorModels() selectedLayer set from selection: {}",
                            state.getSelectedAnnotationLayer().getUiName());
                }
                catch (NoResultException e) {
                    reset(aTarget);
                    throw new IllegalStateException(
                            "Unknown layer [" + anno.getType().getName() + "]", e);
                }

                loadFeatureEditorModelsCommon(aTarget, cas, layer, anno, null);
            }
            else {
                // Creating new annotation
                // Populate the feature editors from the remembered values (if any)

                if (selection.isArc()) {
                    if (state.getSelectedAnnotationLayer() != null
                            && state.getSelectedAnnotationLayer().isReadonly()) {
                        // Avoid creation of arcs on locked layers
                        state.setSelectedAnnotationLayer(new AnnotationLayer());
                    }
                    else {
                        loadFeatureEditorModelsCommon(aTarget, cas,
                                state.getSelectedAnnotationLayer(), null,
                                state.getRememberedArcFeatures());
                    }
                }
                else {
                    loadFeatureEditorModelsCommon(aTarget, cas, state.getSelectedAnnotationLayer(),
                            null, state.getRememberedSpanFeatures());
                }
            }
        }
        catch (Exception e) {
            throw new AnnotationException(e);
        }
    }

    private void loadFeatureEditorModelsCommon(AjaxRequestTarget aTarget, CAS aCas,
            AnnotationLayer aLayer, FeatureStructure aFS,
            Map<AnnotationFeature, Serializable> aRemembered)
    {
        var state = getModelObject();

        state.getFeatureStates().clear();

        var adapter = annotationService.getAdapter(aLayer);

        for (var feature : annotationService.listEnabledFeatures(aLayer)) {
            if (isFeatureSuppressed(state, feature)) {
                continue;
            }

            if (aFS != null && aFS.getType().getFeatureByBaseName(feature.getName()) == null) {
                error("The annotation typesystem seems to be out of date, try re-opening the document!");
                LOG.error("Unable to find [{}] in the current CAS typesystem", feature.getName());
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            FeatureState featureState;
            if (aFS != null) {
                featureState = adapter.getFeatureState(feature, aFS);
            }
            else if (aRemembered != null) {
                var value = aRemembered.get(feature);
                featureState = new FeatureState(null, feature, value);
            }
            else {
                featureState = new FeatureState(null, feature, null);
            }

            populateTagset(aCas, state, featureState);

            state.getFeatureStates().add(featureState);
        }
    }

    private boolean isFeatureSuppressed(AnnotatorState state, AnnotationFeature feature)
    {
        if (ChainLayerSupport.TYPE.equals(feature.getLayer().getType())) {
            // For chain layers, we only want to show the "type" and "relation" features...
            // FIXME: This would probably be better handled by introducing special FeatureSupports
            // for these two features and implementing the isAccessible() method accordingly

            if (state.getSelection().isArc()) {
                if (feature.getLayer().isLinkedListBehavior()
                        && COREFERENCE_RELATION_FEATURE.equals(feature.getName())) {
                    // Only show the chain relation feature if the linked-list behavior is active
                    return false;
                }

                return true;
            }

            if (COREFERENCE_TYPE_FEATURE.equals(feature.getName())) {
                return false;
            }

            return true;
        }

        return false;
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        // Only show sidebar if a document is selected
        setVisible(getModelObject() != null && getModelObject().getDocument() != null);

        // Set read only if annotation is finished or the user is viewing other's work
        var selectedLayerIsReadOnly = getModel() //
                .map(AnnotatorState::getSelectedAnnotationLayer) //
                .map(AnnotationLayer::isReadonly) //
                .orElse(true) //
                .getObject();
        setEnabled(editorPage.isEditable() && !selectedLayerIsReadOnly);
    }

    /**
     * Re-render the sidebar if the selection has changed.
     */
    @SuppressWarnings("javadoc")
    @OnEvent
    public void onSelectionChangedEvent(SelectionChangedEvent aEvent)
    {
        if (aEvent.getRequestHandler() != null) {
            try {
                refresh(aEvent.getRequestHandler());
            }
            catch (Exception e) {
                handleException(this, aEvent.getRequestHandler(), e);
            }
        }
    }

    private boolean annotationEventAffectsSelectedAnnotation(AnnotationEvent aEvent)
    {
        var state = getModelObject();
        var selection = state.getSelection();
        if (selection.getAnnotation().isNotSet()) {
            return false;
        }

        if (!state.getUser().getUsername().equals(aEvent.getDocumentOwner())) {
            return false;
        }

        return true;
    }

    @OnEvent
    public void onBulkAnnotationEvent(BulkAnnotationEvent aEvent)
    {
        if (!annotationEventAffectsSelectedAnnotation(aEvent)) {
            return;
        }

        try {
            var selection = getModelObject().getSelection();
            var id = selection.getAnnotation().getId();
            var annotationStillExists = getEditorCas().select(Annotation.class) //
                    .at(selection.getBegin(), selection.getEnd()) //
                    .anyMatch(ann -> ann._id() == id);

            if (!annotationStillExists) {
                selection.clear();
                aEvent.getRequestTarget().ifPresent(this::refresh);
            }
        }
        catch (Exception e) {
            handleException(this, aEvent.getRequestTarget().orElse(null), e);
        }
    }

    private void populateTagset(CAS aCas, AnnotatorState aState, FeatureState aFeatureState)
    {
        var tagset = aFeatureState.feature.getTagset();
        if (tagset == null) {
            return;
        }

        // verification to check whether constraints exist for this project or NOT
        if (aState.getConstraints() != null && aState.getSelection().getAnnotation().isSet()) {
            // indicator.setRulesExist(true);
            populateTagsetBasedOnRules(aCas, aFeatureState);
            return;
        }

        // indicator.setRulesExist(false);
        aFeatureState.tagset = annotationService.listTagsReorderable(tagset);
    }

    /**
     * Adds and sorts tags based on Constraints rules
     */
    private void populateTagsetBasedOnRules(CAS aCas, FeatureState aModel)
    {
        var state = getModelObject();

        aModel.indicator.reset();

        // Fetch possible values from the constraint rules
        List<PossibleValue> possibleValues;
        try {
            var fs = selectFsByAddr(aCas, state.getSelection().getAnnotation().getId());

            var evaluator = new ConstraintsEvaluator();
            // Only show indicator if this feature can be affected by Constraint rules!
            aModel.indicator.setAffected(evaluator
                    .isPathUsedInAnyRestriction(state.getConstraints(), fs, aModel.feature));

            possibleValues = evaluator.generatePossibleValues(state.getConstraints(), fs,
                    aModel.feature);

            LOG.debug("Possible values for [{}] : {}", fs.getType().getName(), aModel.feature,
                    possibleValues);
        }
        catch (Exception e) {
            error("Unable to evaluate constraints: " + ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Unable to evaluate constraints: " + e.getMessage(), e);
            possibleValues = new ArrayList<>();
        }

        // Fetch actual tagset
        var tags = annotationService.listTagsReorderable(aModel.feature.getTagset());

        // First add tags which are suggested by rules and exist in tagset
        var tagset = compareSortAndAdd(possibleValues, tags, aModel.indicator);

        // Record the possible values and the (re-ordered) tagset in the feature state
        aModel.possibleValues = possibleValues;
        aModel.tagset = tagset;
    }

    /*
     * Compares existing tagset with possible values resulted from rule evaluation Adds only which
     * exist in tagset and is suggested by rules. The remaining values from tagset are added
     * afterwards.
     */
    private static List<ReorderableTag> compareSortAndAdd(List<PossibleValue> aPossibleValues,
            List<ReorderableTag> aTags, RulesIndicator aRulesIndicator)
    {
        var returnList = new ArrayList<ReorderableTag>();

        // if no possible values, means didn't satisfy conditions
        if (aPossibleValues.isEmpty()) {
            aRulesIndicator.didntMatchAnyRule();
            return aTags;
        }

        var tagIndex = new LinkedHashMap<String, ReorderableTag>();
        for (ReorderableTag tag : aTags) {
            tagIndex.put(tag.getName(), tag);
        }

        for (var value : aPossibleValues) {
            var tag = tagIndex.get(value.getValue());
            if (tag == null) {
                continue;
            }

            // Matching values found in tagset and shown in dropdown
            aRulesIndicator.rulesApplied();
            // HACK BEGIN
            tag.setReordered(true);
            // HACK END
            returnList.add(tag);
            // Avoid duplicate entries
            tagIndex.remove(value.getValue());
        }

        // If no matching tags found
        if (returnList.isEmpty()) {
            aRulesIndicator.didntMatchAnyTag();
        }

        // Add all remaining non-matching tags to the list
        returnList.addAll(tagIndex.values());

        return returnList;
    }

    /**
     * Clears the selection in the {@link AnnotatorState} and clears the feature editors. Also
     * refreshes the selectable layers dropdown.
     * 
     * @param aTarget
     *            (optional) current AJAX target
     */
    public void reset(AjaxRequestTarget aTarget)
    {
        var state = getModelObject();

        // Clear selection and feature states
        state.getFeatureStates().clear();
        state.getSelection().clear();

        // Refresh the selectable layers dropdown
        state.refreshSelectableLayers(schemaProperties::isLayerBlocked);

        if (state.getDefaultAnnotationLayer() != null) {
            var sessionOwner = userService.getCurrentUser();
            var anchoringPrefs = preferencesService.loadTraitsForUserAndProject(KEY_ANCHORING_MODE,
                    sessionOwner, state.getProject());
            state.syncAnchoringModeToDefaultLayer(anchoringPrefs);
        }

        if (aTarget != null) {
            aTarget.add(layerSelectionPanel);
        }
    }

    // Used in commented-out code that we might want to comment back in again later
    @SuppressWarnings("unused")
    private static Set<AnnotationFS> getAttachedSpans(AnnotationSchemaService aAS, AnnotationFS aFs,
            AnnotationLayer aLayer)
    {
        var cas = aFs.getCAS();
        var attachedSpans = new HashSet<AnnotationFS>();
        var adapter = aAS.getAdapter(aLayer);
        if (adapter instanceof SpanAdapter && aLayer.getAttachType() != null) {
            var spanType = CasUtil.getType(cas, aLayer.getAttachType().getName());
            var attachFeature = spanType.getFeatureByBaseName(aLayer.getAttachFeature().getName());
            var type = spanType;

            for (var attachedFs : selectAt(cas, type, aFs.getBegin(), aFs.getEnd())) {
                if (isSame(attachedFs.getFeatureValue(attachFeature), aFs)) {
                    attachedSpans.add(attachedFs);
                }
            }
        }
        return attachedSpans;
    }

    protected static void handleException(Component aComponent, AjaxRequestTarget aTarget,
            Exception aException)
    {
        if (aTarget != null) {
            aTarget.addChildren(aComponent.getPage(), IFeedback.class);
        }

        try {
            throw aException;
        }
        catch (AnnotationException e) {
            aComponent.error("Error: " + e.getMessage());
            LOG.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (UIMAException e) {
            aComponent.error("Error: " + ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (Exception e) {
            aComponent.error("Error: " + e.getMessage());
            LOG.error("Error: " + e.getMessage(), e);
        }
    }

    private LambdaAjaxLink createClearButton()
    {
        var link = new LambdaAjaxLink("clear", this::actionClear);
        link.setOutputMarkupPlaceholderTag(true);
        link.setAlwaysEnabled(true); // Not to be disabled when document is read-only
        link.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()));
        link.add(new InputBehavior(new KeyType[] { Shift, Escape }, click));
        return link;
    }

    private Component createReverseButton()
    {
        var link = new LambdaAjaxLink("reverse", this::actionReverse);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(LambdaBehavior.onConfigure(_this -> {
            var state = getModelObject();

            _this.setVisible(
                    state.getSelection().getAnnotation().isSet() && state.getSelection().isArc()
                            && RelationLayerSupport.TYPE
                                    .equals(state.getSelectedAnnotationLayer().getType())
                            && editorPage.isEditable());
        }));
        link.add(new InputBehavior(new KeyType[] { Shift, Space }, click));
        return link;
    }

    private LambdaAjaxLink createDeleteButton()
    {
        var link = new LambdaAjaxLink("delete", this::actionDelete);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()
                && editorPage.isEditable()));
        link.add(new InputBehavior(new KeyType[] { Shift, Delete }, click));
        return link;
    }

    public FeatureEditorListPanel getFeatureEditorListPanel()
    {
        return featureEditorListPanel;
    }

    public void refresh(AjaxRequestTarget aTarget)
    {
        // We need to add the entire ADEP because we set the enabled state of the whole ADEP
        // hierarchy in its configure method for read-only layers or documents
        aTarget.add(this);
    }

    @OnEvent(stop = true)
    public void onLinkFeatureDeletedEvent(LinkFeatureDeletedEvent aEvent)
    {
        if (getModelObject().getSelection().getAnnotation().isNotSet()) {
            return;
        }

        // Auto-commit if working on existing annotation
        var target = aEvent.getTarget();
        try {
            var cas = getEditorCas();
            internalCommitAnnotation(target, cas);
            internalCompleteAnnotation(target, cas);
            refresh(target);
        }
        catch (Exception e) {
            handleException(this, target, e);
        }
    }

    static class AttachStatus
        implements Serializable
    {
        private static final long serialVersionUID = -8359575377186677974L;

        boolean readOnlyAttached;
        int attachCount;

        public int getAttachCount()
        {
            return attachCount;
        }
    }
}
