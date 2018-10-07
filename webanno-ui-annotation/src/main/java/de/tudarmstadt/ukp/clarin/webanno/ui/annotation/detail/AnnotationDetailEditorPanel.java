/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSame;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAt;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormValidatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.Selection;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.Evaluator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.RulesIndicator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ValuesGenerator;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.event.AjaxAfterAnnotationUpdateEvent;

/**
 * Annotation Detail Editor Panel.
 */
public class AnnotationDetailEditorPanel
    extends Panel
    implements AnnotationActionHandler
{
    private static final long serialVersionUID = 7324241992353693848L;
    private static final Logger LOG = LoggerFactory.getLogger(AnnotationDetailEditorPanel.class);

    private @SpringBean ProjectService projectService;
    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    
    private AnnotationFeatureForm annotationFeatureForm;

    public AnnotationDetailEditorPanel(String id, IModel<AnnotatorState> aModel)
    {
        super(id, aModel);
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        setMarkupId("annotationDetailEditorPanel");
        add(createAnnotationFeatureForm());
    }

    private Component createAnnotationFeatureForm()
    {
        annotationFeatureForm = new AnnotationFeatureForm(this, "annotationFeatureForm",
                getModel());
        annotationFeatureForm.setOutputMarkupId(true);
        annotationFeatureForm.add(new AjaxFormValidatingBehavior("submit") {
            private static final long serialVersionUID = -5642108496844056023L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget) {
                try {
                    JCas jCas = getEditorCas();
                    actionCreateOrUpdate(aTarget, jCas);
                }
                catch (Exception e) {
                    handleException(annotationFeatureForm, aTarget, e);
                }
            }
        });
        return annotationFeatureForm;
    }

    boolean isAnnotationFinished()
    {
        AnnotatorState state = getModelObject();

        if (state.getMode().equals(Mode.CURATION)) {
            return state.getDocument().getState().equals(SourceDocumentState.CURATION_FINISHED);
        }
        else {
            return documentService.isAnnotationFinished(state.getDocument(), state.getUser());
        }
    }

    private void createNewAnnotation(AjaxRequestTarget aTarget, TypeAdapter aAdapter, JCas aJCas)
        throws AnnotationException, IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getSelection().isArc()) {
            if (aAdapter instanceof SpanAdapter) {
                error("Layer [" + aAdapter.getLayer().getUiName()
                    + "] does not support arc annotation.");
                aTarget.addChildren(getPage(), IFeedback.class);
            }
            else if (aAdapter instanceof ArcAdapter) {
                createNewRelationAnnotation((ArcAdapter) aAdapter, aJCas);
            }
            else if (aAdapter instanceof ChainAdapter) {
                createNewChainLinkAnnotation((ChainAdapter) aAdapter, aJCas);
            }
            else {
                throw new IllegalStateException("I don't know how to use ["
                    + aAdapter.getClass().getSimpleName() + "] in this situation.");
            }
        }
        else {
            if (aAdapter instanceof SpanAdapter) {
                createNewSpanAnnotation(aTarget, (SpanAdapter) aAdapter, aJCas);
            }
            else if (aAdapter instanceof ChainAdapter) {
                createNewChainElement((ChainAdapter) aAdapter, aJCas);
            }
            else {
                throw new IllegalStateException("I don't know how to use ["
                    + aAdapter.getClass().getSimpleName() + "] in this situation.");
            }
        }
    }

    private void createNewRelationAnnotation(ArcAdapter aAdapter,
            JCas aJCas)
        throws AnnotationException
    {
        LOG.trace("createNewRelationAnnotation()");

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();

        AnnotationFS originFs = selectByAddr(aJCas, selection.getOrigin());
        AnnotationFS targetFs = selectByAddr(aJCas, selection.getTarget());

        // Creating a relation
        AnnotationFS arc = aAdapter.add(originFs, targetFs, aJCas, state.getWindowBeginOffset(),
            state.getWindowEndOffset());
        selection.selectArc(new VID(arc), originFs, targetFs);
    }

    private void createNewSpanAnnotation(AjaxRequestTarget aTarget, SpanAdapter aAdapter,
        JCas aJCas)
        throws IOException, AnnotationException
    {
        LOG.trace("createNewSpanAnnotation()");

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();
        List<FeatureState> featureStates = state.getFeatureStates();

        if ((aAdapter.isTokensAnchoringMode() || aAdapter.isSingleTokenAnchoringMode())
                && selection.getBegin() == selection.getEnd()) {
            throw new AnnotationException(
                    "Cannot create zero-width annotation on layers that lock to token boundaries.");
        }
        
        for (FeatureState featureState : featureStates) {
            Serializable spanValue = aAdapter.getSpan(aJCas, selection.getBegin(),
                selection.getEnd(), featureState.feature, null);
            if (spanValue != null) {
                // allow modification for forward annotation
                if (state.isForwardAnnotation()) {
                    featureState.value = spanValue;
                    featureStates.get(0).value = spanValue;
                    String selectedTag = annotationFeatureForm.getBindTags()
                        .entrySet().stream().filter(e -> e.getValue().equals(spanValue))
                        .map(Map.Entry::getKey).findFirst().orElse(null);
                    annotationFeatureForm.setSelectedTag(selectedTag);
                }
                else {
                    actionClear(aTarget);
                    throw new AnnotationException("Cannot create another annotation of layer ["
                        + state.getSelectedAnnotationLayer().getUiName() + "] at this"
                        + " location - stacking is not enabled for this layer.");
                }
            }
        }
        int annoId = aAdapter.add(state, aJCas, selection.getBegin(), selection.getEnd());
        AnnotationFS annoFs = WebAnnoCasUtil.selectByAddr(aJCas, annoId);
        selection.selectSpan(new VID(annoId), aJCas, annoFs.getBegin(), annoFs.getEnd());
    }

    private void createNewChainElement(ChainAdapter aAdapter,
            JCas aJCas)
        throws AnnotationException
    {
        LOG.trace("createNewChainElement()");

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();
        List<FeatureState> featureStates = state.getFeatureStates();

        for (FeatureState featureState : featureStates) {
            Serializable spanValue = aAdapter.getSpan(aJCas,
                selection.getBegin(), selection.getEnd(), featureState.feature, null);
            if (spanValue != null) {
                // allow modification for forward annotation
                if (state.isForwardAnnotation()) {
                    featureState.value = spanValue;
                    featureStates.get(0).value = spanValue;
                    String selectedTag = annotationFeatureForm.getBindTags()
                        .entrySet().stream().filter(e -> e.getValue().equals(spanValue))
                        .map(Map.Entry::getKey).findFirst().orElse(null);
                    annotationFeatureForm.setSelectedTag(selectedTag);
                }
            }
        }
        
        selection.setAnnotation(new VID(
            aAdapter.addSpan(aJCas, selection.getBegin(), selection.getEnd())));
        selection.setText(
            aJCas.getDocumentText().substring(selection.getBegin(), selection.getEnd()));
    }

    private void createNewChainLinkAnnotation(ChainAdapter aAdapter,
            JCas aJCas) {
        LOG.trace("createNewChainLinkAnnotation()");

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();

        AnnotationFS originFs = selectByAddr(aJCas, selection.getOrigin());
        AnnotationFS targetFs = selectByAddr(aJCas, selection.getTarget());

        // Creating a new chain link
        int addr = aAdapter.addArc(aJCas, originFs, targetFs);
        selection.selectArc(new VID(addr), originFs, targetFs);
    }

    @Override
    public void actionFillSlot(AjaxRequestTarget aTarget, JCas aJCas, int aBegin, int aEnd,
        VID aVID)
        throws AnnotationException, IOException
    {
        assert aJCas != null;

        AnnotatorState state = getModelObject();
    
        // REC: I'm not sure this should be fired here. Leaving it commented in case we need it.
        // Otherwise, should be removed in due time.
        // extensionRegistry.fireAction(AnnotationDetailEditorPanel.this, getModelObject(), aTarget,
        // aJCas, aVID, aBegin, aEnd);
        
        // If this method is called when no slot is armed, it must be a bug!
        if (!state.isSlotArmed()) {
            throw new IllegalStateException("No slot is armed.");
        }

        // Fill slot with new annotation (only works if a concrete type is set for the link feature!
        int id;
        if (aVID.isNotSet()) {
            if (!CAS.TYPE_NAME_ANNOTATION.equals(state.getArmedFeature().getType())) {
                SpanAdapter adapter = (SpanAdapter) annotationService.getAdapter(annotationService
                        .getLayer(state.getArmedFeature().getType(), state.getProject()));

                id = adapter.add(state, aJCas, aBegin, aEnd);
            }
            else {
                throw new AnnotationException(
                    "Unable to create annotation of type [" + CAS.TYPE_NAME_ANNOTATION
                        + "]. Please click an annotation in stead of selecting new text.");
            }
        }
        else {
            id = aVID.getId();
        }

        // Fill the annotation into the slow
        try {
            setSlot(aTarget, aJCas, id);
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }
    }

    @Override
    public void actionSelect(AjaxRequestTarget aTarget, JCas aJCas)
        throws AnnotationException
    {
        // Edit existing annotation
        loadFeatureEditorModels(aJCas, aTarget);

        // Ensure we re-render and update the highlight
        onChange(aTarget);
    }

    @Override
    public void actionCreateOrUpdate(AjaxRequestTarget aTarget, JCas aJCas)
        throws IOException, AnnotationException
    {
        LOG.trace("actionAnnotate");

        if (isAnnotationFinished()) {
            throw new AnnotationException("This document is already closed. Please ask your "
                + "project manager to re-open it via the Monitoring page");
        }

        AnnotatorState state = getModelObject();

        // Note that refresh changes the selected layer if a relation is created. Then the layer
        // switches from the selected span layer to the relation layer that is attached to the span
        if (state.getSelection().isArc()) {
            LOG.trace("actionAnnotate() relation annotation - looking for attached layer");

            // FIXME REC I think this whole section which meddles around with the selected
            // annotation layer should be moved out of there to the place where we originally set
            // the annotation layer...!
            AnnotationFS originFS = selectByAddr(aJCas, state.getSelection().getOrigin());
            AnnotationLayer spanLayer = annotationService.getLayer(state.getProject(), originFS);
            if (
                    state.getPreferences().isRememberLayer() &&
                    state.getSelection().getAnnotation().isNotSet() && // i.e. new annotation
                    !spanLayer.equals(state.getDefaultAnnotationLayer())
            ) {
                throw new AnnotationException("No relation annotation allowed on layer ["
                        + state.getDefaultAnnotationLayer().getUiName() + "]");
            }

            AnnotationLayer previousLayer = state.getSelectedAnnotationLayer();

            // If we are creating a relation annotation, we have to set the current layer depending
            // on the type of relation that is permitted between the source/target span. This is
            // necessary because we have no separate UI control to set the relation annotation type.
            // It is possible because currently only a single relation layer is allowed to attach to
            // any given span layer.

            // If we drag an arc in a chain layer, then the arc is of the same layer as the span
            // Chain layers consist of arcs and spans
            if (spanLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                // one layer both for the span and arc annotation
                state.setSelectedAnnotationLayer(spanLayer);
            }
            // Otherwise, look up the possible relation layer(s) in the database.
            else {
                for (AnnotationLayer l : annotationService.listAnnotationLayer(state
                    .getProject())) {
                    if (
                            (l.getAttachType() != null && l.getAttachType().equals(spanLayer)) ||
                            (l.getAttachFeature() != null && 
                                    l.getAttachFeature().getType().equals(spanLayer.getName()))
                    ) {
                        if (state.getAnnotationLayers().contains(l)) {
                            state.setSelectedAnnotationLayer(l);
                        }
                        else {
                            state.setSelectedAnnotationLayer(null);
                        }
                        break;
                    }
                }
            }

            state.setDefaultAnnotationLayer(spanLayer);

            // If we switched layers, we need to initialize the feature editors for the new layer
            if (!Objects.equals(previousLayer, state.getSelectedAnnotationLayer())) {
                LOG.trace("Layer changed from {} to {} - need to reload feature editors",
                        previousLayer, state.getSelectedAnnotationLayer());
                loadFeatureEditorModels(aJCas, aTarget);
            }
        }
        else {
            // Re-set the selected layer from the drop-down since it might have changed if we
            // have previously created a relation annotation
            state.setSelectedAnnotationLayer(
                    annotationFeatureForm.getLayerSelector().getModelObject());
        }
        
        internalCommitAnnotation(aTarget, aJCas);

        internalCompleteAnnotation(aTarget, aJCas);
    }
    
    @Override
    public void actionCreateForward(AjaxRequestTarget aTarget, JCas aJCas)
        throws IOException, AnnotationException
    {
        actionCreateForward(aTarget, aJCas, false);
    }

    private void actionCreateForward(AjaxRequestTarget aTarget, JCas aJCas, boolean aIsForwarded)
        throws IOException, AnnotationException 
    {
        LOG.trace("actionForward(isForwarded: {})", aIsForwarded);

        if (isAnnotationFinished()) {
            throw new AnnotationException("This document is already closed. Please ask your "
                    + "project manager to re-open it via the Monitoring page");
        }

        AnnotatorState state = getModelObject();

        // Re-set the selected layer from the drop-down since it might have changed if we
        // have previously created a relation annotation
        state.setSelectedAnnotationLayer(
                annotationFeatureForm.getLayerSelector().getModelObject());

        internalCommitAnnotation(aTarget, aJCas);

        if (!aIsForwarded) {
            if (state.getSelection().getEnd() >= state.getWindowEndOffset()) {
                autoScroll(aJCas, true);
            }

            List<FeatureState> featureStates = getModelObject().getFeatureStates();
            if (featureStates.get(0).value != null) {
                LOG.info("BEGIN auto-forward annotation for tagset-based annotation");

                AnnotationFS nextToken = WebAnnoCasUtil.getNextToken(aJCas,
                        state.getSelection().getBegin(), state.getSelection().getEnd());
                if (nextToken != null) {
                    if (getModelObject().getWindowEndOffset() > nextToken.getBegin()) {
                        state.getSelection().selectSpan(aJCas, nextToken.getBegin(),
                                nextToken.getEnd());
                        actionCreateForward(aTarget, aJCas, true);
                    }
                }
                LOG.info("END auto-forward annotation for tagset-based annotation");
            }
            else {
                LOG.info("BEGIN auto-forward annotation for free-text annotation");

                // If the annotation value was cleared or not filled in by the user, then we
                // remove the entire annotation.
                if (featureStates.get(0).value == null) {
                    TypeAdapter adapter = annotationService
                            .getAdapter(state.getSelectedAnnotationLayer());
                    AnnotationFS fs = selectByAddr(aJCas,
                            state.getSelection().getAnnotation().getId());
                    deleteAnnotation(aJCas, state, fs, featureStates.get(0).feature.getLayer(),
                            adapter);
                }

                AnnotationFS nextToken = WebAnnoCasUtil.getNextToken(aJCas,
                        state.getSelection().getBegin(), state.getSelection().getEnd());
                if (nextToken != null) {
                    if (getModelObject().getWindowEndOffset() > nextToken.getBegin()) {
                        state.getSelection().selectSpan(aJCas, nextToken.getBegin(),
                                nextToken.getEnd());
                        actionCreateForward(aTarget, aJCas, true);
                    }
                }
                
                LOG.info("END auto-forward annotation for free-text annotation"); 
            }

            LOG.trace("onAutoForward()");
            onAutoForward(aTarget);
        }
        
        aTarget.add(annotationFeatureForm);
        
        internalCompleteAnnotation(aTarget, aJCas);
    }
    
    private void internalCompleteAnnotation(AjaxRequestTarget aTarget, JCas aJCas)
    {
        AnnotatorState state = getModelObject();
        
        // Perform auto-scroll if it is enabled
        if (state.getPreferences().isScrollPage()) {
            autoScroll(aJCas, false);
        }

        annotationFeatureForm.getForwardAnnotationText().setModelObject(null);

        LOG.trace("onChange()");
        onChange(aTarget);

        // If we created a new annotation, then refresh the available annotation layers in the
        // detail panel.
        if (state.getSelection().getAnnotation().isNotSet()) {
            refresh(state);
        }
    }

    private void refresh(AnnotatorState state) 
    {
        // This already happens in loadFeatureEditorModels() above - probably not needed
        // here again
        // annotationFeatureForm.updateLayersDropdown();

        LOG.trace("actionAnnotate() setting selected layer (not sure why)");
        if (annotationFeatureForm.getAnnotationLayers().isEmpty()) {
            state.setSelectedAnnotationLayer(new AnnotationLayer());
        }
        else if (state.getSelectedAnnotationLayer() == null) {
            if (state.getRememberedSpanLayer() == null) {
                state.setSelectedAnnotationLayer(annotationFeatureForm.getAnnotationLayers()
                    .get(0));
            }
            else {
                state.setSelectedAnnotationLayer(state.getRememberedSpanLayer());
            }
        }
        LOG.trace("actionAnnotate() selectedLayer: {}",
            state.getSelectedAnnotationLayer().getUiName());

        // Actually not sure why we would want to clear these here - in fact, they should
        // still be around for the rendering phase of the feature editors...
        //clearFeatureEditorModels(aTarget);

        // This already happens in loadFeatureEditorModels() above - probably not needed
        // here again
        // annotationFeatureForm.updateRememberLayer();
    }
    
    private void internalCommitAnnotation(AjaxRequestTarget aTarget, JCas aJCas)
        throws AnnotationException, IOException
    {
        AnnotatorState state = getModelObject();
        
        LOG.trace("actionAnnotate() selectedLayer: {}",
                state.getSelectedAnnotationLayer().getUiName());
        LOG.trace("actionAnnotate() defaultLayer: {}",
                state.getDefaultAnnotationLayer().getUiName());

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

        // Verify if input is valid according to tagset
        LOG.trace("actionAnnotate() verifying feature values in editors");
        List<FeatureState> featureStates = getModelObject().getFeatureStates();
        for (FeatureState featureState : featureStates) {
            AnnotationFeature feature = featureState.feature;
            if (CAS.TYPE_NAME_STRING.equals(feature.getType())) {
                String value = (String) featureState.value;

                // Check if tag is necessary, set, and correct
                if (
                    value != null &&
                        feature.getTagset() != null &&
                        !feature.getTagset().isCreateTag() &&
                        !annotationService.existsTag(value, feature.getTagset())
                    ) {
                    error("[" + value
                        + "] is not in the tag list. Please choose from the existing tags");
                    return;
                }
            }
        }

        // #186 - After filling a slot, the annotation detail panel is not updated
        aTarget.add(annotationFeatureForm.getFeatureEditorPanel());

        TypeAdapter adapter = annotationService.getAdapter(state.getSelectedAnnotationLayer());

        // If this is an annotation creation action, create the annotation
        if (state.getSelection().getAnnotation().isNotSet()) {
            // Load the feature editors with the remembered values (if any)
            loadFeatureEditorModels(aJCas, aTarget);
            createNewAnnotation(aTarget, adapter, aJCas);
        }

        // Update the features of the selected annotation from the values presently in the
        // feature editors
        writeFeatureEditorModelsToCas(adapter, aJCas);

        // Notify other UI elements (e.g. sidebars) about the update so that they can react to it
        send(getPage(), Broadcast.BREADTH, new AjaxAfterAnnotationUpdateEvent(aTarget, state));

        // Update progress information
        LOG.trace("actionAnnotate() updating progress information");
        int sentenceNumber = getSentenceNumber(aJCas, state.getSelection().getBegin());
        state.setFocusUnitIndex(sentenceNumber);
        state.getDocument().setSentenceAccessed(sentenceNumber);

        // persist changes
        writeEditorCas(aJCas);

        // Remember the current feature values independently for spans and relations
        LOG.trace("actionAnnotate() remembering feature editor values");
        state.rememberFeatures();

        // Loading feature editor values from CAS
        loadFeatureEditorModels(aJCas, aTarget);

        // onAnnotate callback
        LOG.trace("onAnnotate()");
        onAnnotate(aTarget);
    }

    public AttachStatus checkAttachStatus(AjaxRequestTarget aTarget, Project aProject,
            AnnotationFS aFS)
    {
        AnnotationLayer layer = annotationService.getLayer(aProject, aFS);
        
        AttachStatus attachStatus = new AttachStatus();
        
        Set<AnnotationFS> attachedRels = getAttachedRels(aFS, layer);
        boolean attachedToReadOnlyRels = attachedRels.stream().anyMatch(relFS -> {
            AnnotationLayer relLayer = annotationService.getLayer(aProject, relFS);
            return relLayer.isReadonly();
        });
        if (attachedToReadOnlyRels) {
            attachStatus.readOnlyAttached |= true;
        }
        attachStatus.attachCount += attachedRels.size();
        
        // We do not count these atm since they only exist for built-in layers and are not 
        // visible in the UI for the user.
        /*
        Set<AnnotationFS> attachedSpans = getAttachedSpans(aFS, layer);
        boolean attachedToReadOnlySpans = attachedSpans.stream().anyMatch(relFS -> {
            AnnotationLayer relLayer = annotationService.getLayer(aProject, relFS);
            return relLayer.isReadonly();
        });
        if (attachedToReadOnlySpans) {
            attachStatus.readOnlyAttached |= true;
        }
        attachStatus.attachCount += attachedSpans.size();
        */

        Set<AnnotationFS> attachedLinks = getAttachedLinks(aFS, layer);
        boolean attachedToReadOnlyLinks = attachedLinks.stream().anyMatch(relFS -> {
            AnnotationLayer relLayer = annotationService.getLayer(aProject, relFS);
            return relLayer.isReadonly();
        });
        if (attachedToReadOnlyLinks) {
            attachStatus.readOnlyAttached |= true;
        }
        attachStatus.attachCount += attachedLinks.size();
        
        return attachStatus;
    }
    
    @Override
    public void actionDelete(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        JCas jCas = getEditorCas();

        AnnotatorState state = getModelObject();

        AnnotationFS fs = selectByAddr(jCas, state.getSelection().getAnnotation().getId());
        AnnotationLayer layer = annotationService.getLayer(state.getProject(), fs);
        TypeAdapter adapter = annotationService.getAdapter(layer);

        if (layer.isReadonly()) {
            error("Cannot delete an annotation on a read-only layer.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        
        if (checkAttachStatus(aTarget, state.getProject(), fs).readOnlyAttached) {
            error("Cannot delete an annotation to which annotations on read-only layers attach.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        
        deleteAnnotation(jCas, state, fs, layer, adapter);

        // Store CAS again
        writeEditorCas(jCas);

        // Update progress information
        int sentenceNumber = getSentenceNumber(jCas, state.getSelection().getBegin());
        state.setFocusUnitIndex(sentenceNumber);
        state.getDocument().setSentenceAccessed(sentenceNumber);

        // Auto-scroll
        if (state.getPreferences().isScrollPage()) {
            autoScroll(jCas, false);
        }

        state.rememberFeatures();

        info(generateMessage(state.getSelectedAnnotationLayer(), null, true));

        state.getSelection().clear();

        // after delete will follow annotation
        aTarget.add(annotationFeatureForm);

        onChange(aTarget);
        onDelete(aTarget, fs);
    }

    private void deleteAnnotation(JCas jCas, AnnotatorState state, AnnotationFS fs,
            AnnotationLayer layer, TypeAdapter adapter) {
        // == DELETE ATTACHED RELATIONS ==
        // If the deleted FS is a span, we must delete all relations that
        // point to it directly or indirectly via the attachFeature.
        //
        // NOTE: It is important that this happens before UNATTACH SPANS since the attach feature
        // is no longer set after UNATTACH SPANS!
        if (adapter instanceof SpanAdapter) {
            for (AnnotationFS attachedFs : getAttachedRels(fs, layer)) {
                jCas.getCas().removeFsFromIndexes(attachedFs);
                info("The attached annotation for relation type [" + annotationService
                    .getLayer(attachedFs.getType().getName(), state.getProject()).getUiName()
                    + "] is deleted");
            }
        }

        // == DELETE ATTACHED SPANS ==
        // This case is currently not implemented because WebAnno currently does not allow to
        // create spans that attach to other spans. The only span type for which this is relevant
        // is the Token type which cannot be deleted.

        // == UNATTACH SPANS ==
        // If the deleted FS is a span that is attached to another span, the
        // attachFeature in the other span must be set to null. Typical example: POS is deleted, so
        // the pos feature of Token must be set to null. This is a quick case, because we only need
        // to look at span annotations that have the same offsets as the FS to be deleted.
        if (
                adapter instanceof SpanAdapter && 
                layer.getAttachType() != null && 
                layer.getAttachFeature() != null
        ) {
            Type spanType = CasUtil.getType(jCas.getCas(), layer.getAttachType().getName());
            Feature attachFeature = spanType.getFeatureByBaseName(layer.getAttachFeature()
                .getName());
            for (AnnotationFS attachedFs : getAttachedSpans(fs, layer)) {
                attachedFs.setFeatureValue(attachFeature, null);
                LOG.debug("Unattached [" + attachFeature.getShortName() + "] on annotation ["
                    + getAddr(attachedFs) + "]");
            }
        }

        // == CLEAN UP LINK FEATURES ==
        // If the deleted FS is a span that is the target of a link feature, we must unset that
        // link and delete the slot if it is a multi-valued link. Here, we have to scan all
        // annotations from layers that have link features that could point to the FS
        // to be deleted: the link feature must be the type of the FS or it must be generic.
        if (adapter instanceof SpanAdapter) {
            for (AnnotationFeature linkFeature : annotationService
                    .listAttachedLinkFeatures(layer)) {
                Type linkType = CasUtil.getType(jCas.getCas(), linkFeature.getLayer().getName());

                for (AnnotationFS linkFS : CasUtil.select(jCas.getCas(), linkType)) {
                    List<LinkWithRoleModel> links = adapter.getFeatureValue(linkFeature, linkFS);
                    Iterator<LinkWithRoleModel> i = links.iterator();
                    boolean modified = false;
                    while (i.hasNext()) {
                        LinkWithRoleModel link = i.next();
                        if (link.targetAddr == getAddr(fs)) {
                            i.remove();
                            LOG.debug("Cleared slot [" + link.role + "] in feature ["
                                + linkFeature.getName() + "] on annotation [" + getAddr(linkFS)
                                + "]");
                            modified = true;
                        }
                    }
                    if (modified) {
                        setFeature(linkFS, linkFeature, links);
                    }
                }
            }
        }

        // If the deleted FS is a relation, we don't have to do anything. Nothing can point to a
        // relation.
        if (adapter instanceof ArcAdapter) {
            // Do nothing ;)
        }

        // Actually delete annotation
        adapter.delete(state, jCas, state.getSelection().getAnnotation());
    }

    @Override
    public void actionReverse(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        aTarget.addChildren(getPage(), IFeedback.class);
        
        JCas jCas = getEditorCas();

        AnnotatorState state = getModelObject();

        AnnotationFS idFs = selectByAddr(jCas, state.getSelection().getAnnotation().getId());

        jCas.removeFsFromIndexes(idFs);

        AnnotationFS originFs = selectByAddr(jCas, state.getSelection().getOrigin());
        AnnotationFS targetFs = selectByAddr(jCas, state.getSelection().getTarget());

        List<FeatureState> featureStates = getModelObject().getFeatureStates();

        TypeAdapter adapter = annotationService.getAdapter(state.getSelectedAnnotationLayer());
        if (adapter instanceof ArcAdapter) {
            // If no features, still create arc #256
            AnnotationFS arc = ((ArcAdapter) adapter).add(targetFs, originFs, jCas,
                    state.getWindowBeginOffset(), state.getWindowEndOffset());
            state.getSelection().setAnnotation(new VID(getAddr(arc)));
            
            for (FeatureState featureState : featureStates) {
                adapter.setFeatureValue(state, jCas, getAddr(arc), featureState.feature,
                        featureState.value);
            }
        }
        else {
            error("chains cannot be reversed");
            return;
        }

        // persist changes
        writeEditorCas(jCas);
        int sentenceNumber = getSentenceNumber(jCas, originFs.getBegin());
        state.setFocusUnitIndex(sentenceNumber);
        state.getDocument().setSentenceAccessed(sentenceNumber);

        if (state.getPreferences().isScrollPage()) {
            autoScroll(jCas, false);
        }

        info("The arc has been reversed");
        state.rememberFeatures();

        // in case the user re-reverse it
        state.getSelection().reverseArc();

        onChange(aTarget);
    }

    @Override
    public void actionClear(AjaxRequestTarget aTarget)
        throws AnnotationException
    {
        reset(aTarget);
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(annotationFeatureForm);
        onChange(aTarget);
    }

    @Override
    public JCas getEditorCas()
        throws IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getMode().equals(Mode.ANNOTATION) || state.getMode().equals(Mode.AUTOMATION)
                || state.getMode().equals(Mode.CORRECTION)) {

            return documentService.readAnnotationCas(state.getDocument(), state.getUser());
        }
        else {
            return curationDocumentService.readCurationCas(state.getDocument());
        }
    }

    private void writeEditorCas(JCas aJCas)
        throws IOException
    {
        AnnotatorState state = getModelObject();
        if (state.getMode().equals(Mode.ANNOTATION) || state.getMode().equals(Mode.AUTOMATION)
            || state.getMode().equals(Mode.CORRECTION)) {
            documentService.writeAnnotationCas(aJCas, state.getDocument(), state.getUser(), true);
        }
        else if (state.getMode().equals(Mode.CURATION)) {
            curationDocumentService.writeCurationCas(aJCas, state.getDocument(), true);
        }
    }

    /**
     * Scroll the window of visible annotations.
     * @param aForward
     *            instead of centering on the sentence that had the last editor, just scroll down
     *            one sentence. This is for forward-annotation mode.
     */
    private void autoScroll(JCas jCas, boolean aForward)
    {
        if (aForward) {
            getModelObject().moveForward(jCas);
        }
        else {
            getModelObject().moveToSelection(jCas);
        }
    }

    @SuppressWarnings("unchecked")
    private void setSlot(AjaxRequestTarget aTarget, JCas aJCas, int aAnnotationId)
    {
        AnnotatorState state = getModelObject();

        // Set an armed slot
        if (!state.getSelection().isArc() && state.isSlotArmed()) {
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) state.getFeatureState(state
                .getArmedFeature()).value;
            LinkWithRoleModel link = links.get(state.getArmedSlot());
            link.targetAddr = aAnnotationId;
            link.label = selectByAddr(aJCas, aAnnotationId).getCoveredText();
        }

        // Auto-commit if working on existing annotation
        if (state.getSelection().getAnnotation().isSet()) {
            try {
                actionCreateOrUpdate(aTarget, aJCas);
            }
            catch (Exception e) {
                handleException(this, aTarget, e);
            }
        }

        state.clearArmedSlot();
    }

    public void loadFeatureEditorModels(AjaxRequestTarget aTarget)
        throws AnnotationException
    {
        try {
            JCas annotationCas = getEditorCas();
            loadFeatureEditorModels(annotationCas, aTarget);
        }
        catch (AnnotationException e) {
            throw e;
        }
        catch (Exception e) {
            throw new AnnotationException(e);
        }
    }

    public void loadFeatureEditorModels(JCas aJCas, AjaxRequestTarget aTarget)
        throws AnnotationException
    {
        LOG.trace("loadFeatureEditorModels()");

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();

        List<FeatureState> featureStates = state.getFeatureStates();
        for (FeatureState featureState : featureStates) {
            if (StringUtils.isNotBlank(featureState.feature.getLinkTypeName())) {
                featureState.value = new ArrayList<>();
            }
        }

        try {
            if (selection.isSpan()) {
                annotationFeatureForm.updateLayersDropdown();
            }

            if (selection.getAnnotation().isSet()) {
                // If an existing annotation was selected, take the feature editor model values from
                // there
                AnnotationFS annoFs = selectByAddr(aJCas, state.getSelection().getAnnotation()
                    .getId());

                // Try obtaining the layer from the feature structure
                AnnotationLayer layer;
                try {
                    layer = annotationService.getLayer(state.getProject(), annoFs);
                    state.setSelectedAnnotationLayer(layer);
                    LOG.trace(String.format(
                        "loadFeatureEditorModels() selectedLayer set from selection: %s",
                        state.getSelectedAnnotationLayer().getUiName()));
                }
                catch (NoResultException e) {
                    clearFeatureEditorModels(aTarget);
                    throw new IllegalStateException(
                        "Unknown layer [" + annoFs.getType().getName() + "]", e);
                }

                // If remember layer is off, then the current layer follows the selected annotations
                // This is only relevant for span annotations because we only have these in the
                // dropdown - relation annotations are automatically determined based on the
                // selected span annotation
                if (!selection.isArc() && !state.getPreferences().isRememberLayer()) {
                    state.setSelectedAnnotationLayer(layer);
                }

                loadFeatureEditorModelsCommon(aTarget, aJCas, layer, annoFs, null);
            }
            else {
                // If a new annotation is being created, populate the feature editors from the
                // remembered values (if any)

                if (selection.isArc()) {
                    // Avoid creation of arcs on locked layers
                    if (state.getSelectedAnnotationLayer() != null
                        && state.getSelectedAnnotationLayer().isReadonly()) {
                        state.setSelectedAnnotationLayer(new AnnotationLayer());
                    }
                    else {
                        loadFeatureEditorModelsCommon(aTarget, aJCas,
                            state.getSelectedAnnotationLayer(), null,
                            state.getRememberedArcFeatures());
                    }
                }
                else {
                    loadFeatureEditorModelsCommon(aTarget, aJCas,
                        state.getSelectedAnnotationLayer(), null,
                        state.getRememberedSpanFeatures());
                }
            }

            annotationFeatureForm.updateRememberLayer();

            if (aTarget != null) {
                aTarget.add(annotationFeatureForm);
            }
        }
        catch (Exception e) {
            throw new AnnotationException(e);
        }
    }

    private void loadFeatureEditorModelsCommon(AjaxRequestTarget aTarget, JCas aJCas,
            AnnotationLayer aLayer, FeatureStructure aFS,
            Map<AnnotationFeature, Serializable> aRemembered)
    {
        clearFeatureEditorModels(aTarget);

        AnnotatorState state = AnnotationDetailEditorPanel.this.getModelObject();

        // Populate from feature structure
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(aLayer)) {
            if (!feature.isEnabled()) {
                continue;
            }

            Serializable value = null;
            if (aFS != null) {
                value = annotationService.getAdapter(aLayer).getFeatureValue(feature, aFS);
            }
            else if (aRemembered != null) {
                value = aRemembered.get(feature);
            }

            FeatureState featureState = null;
            if (WebAnnoConst.CHAIN_TYPE.equals(feature.getLayer().getType())) {
                if (state.getSelection().isArc()) {
                    if (feature.getLayer().isLinkedListBehavior()
                        && WebAnnoConst.COREFERENCE_RELATION_FEATURE.equals(feature
                        .getName())) {
                        featureState = new FeatureState(feature, value);
                    }
                }
                else {
                    if (WebAnnoConst.COREFERENCE_TYPE_FEATURE.equals(feature.getName())) {
                        featureState = new FeatureState(feature, value);
                    }
                }

            }
            else {
                featureState = new FeatureState(feature, value);
            }

            if (featureState != null) {
                state.getFeatureStates().add(featureState);

                // verification to check whether constraints exist for this project or NOT
                if (state.getConstraints() != null
                        && state.getSelection().getAnnotation().isSet()) {
                    // indicator.setRulesExist(true);
                    populateTagsBasedOnRules(aJCas, featureState);
                }
                else {
                    // indicator.setRulesExist(false);
                    featureState.tagset = annotationService
                            .listTags(featureState.feature.getTagset());
                }
            }
        }
    }

    private void writeFeatureEditorModelsToCas(TypeAdapter aAdapter, JCas aJCas)
        throws IOException
    {
        AnnotatorState state = getModelObject();
        List<FeatureState> featureStates = state.getFeatureStates();

        LOG.trace("writeFeatureEditorModelsToCas()");
        List<AnnotationFeature> features = new ArrayList<>();
        for (FeatureState featureState : featureStates) {
            features.add(featureState.feature);
            
            // For string features with extensible tagsets, extend the tagset
            if (CAS.TYPE_NAME_STRING.equals(featureState.feature.getType())) {
                String value = (String) featureState.value;
               
                if (value != null && featureState.feature.getTagset() != null
                        && featureState.feature.getTagset().isCreateTag()
                        && !annotationService.existsTag(value, featureState.feature.getTagset())) {
                    Tag selectedTag = new Tag();
                    selectedTag.setName(value);
                    selectedTag.setTagSet(featureState.feature.getTagset());
                    annotationService.createTag(selectedTag);
                }
            }
            
            LOG.trace("writeFeatureEditorModelsToCas() "
                + featureState.feature.getUiName() + " = " + featureState.value);
            aAdapter.setFeatureValue(state, aJCas, state.getSelection().getAnnotation().getId(),
                    featureState.feature, featureState.value);
        }

        // Generate info message
        if (state.getSelection().getAnnotation().isSet()) {
            String bratLabelText = TypeUtil.getUiLabelText(aAdapter,
                selectByAddr(aJCas, state.getSelection().getAnnotation().getId()), features);
            info(generateMessage(state.getSelectedAnnotationLayer(), bratLabelText, false));
        }
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        // Only show sidebar if a document is selected
        setVisible(getModelObject() != null && getModelObject().getDocument() != null);
    }

    protected void onChange(AjaxRequestTarget aTarget)
    {
        // Overriden in CurationPanel
    }

    protected void onAutoForward(AjaxRequestTarget aTarget)
    {
        // Overriden in CurationPanel
    }

    protected void onAnnotate(AjaxRequestTarget aTarget)
    {
        // Overriden in AutomationPage
    }

    protected void onDelete(AjaxRequestTarget aTarget, AnnotationFS aFs)
    {
        // Overriden in AutomationPage
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    /**
     * Clear the values from the feature editors.
     */
    void clearFeatureEditorModels(AjaxRequestTarget aTarget)
    {
        LOG.trace("clearFeatureEditorModels()");
        getModelObject().getFeatureStates().clear();
        if (aTarget != null) {
            aTarget.add(annotationFeatureForm);
        }
    }

    /**
     * Adds and sorts tags based on Constraints rules
     */
    private void populateTagsBasedOnRules(JCas aJCas, FeatureState aModel)
    {
        LOG.trace("populateTagsBasedOnRules(feature: " + aModel.feature.getUiName() + ")");

        AnnotatorState state = getModelObject();

        // Add values from rules
        String restrictionFeaturePath;
        switch (aModel.feature.getLinkMode()) {
        case WITH_ROLE:
            restrictionFeaturePath = aModel.feature.getName() + "."
                    + aModel.feature.getLinkTypeRoleFeatureName();
            break;
        case NONE:
            restrictionFeaturePath = aModel.feature.getName();
            break;
        default:
            throw new IllegalArgumentException(
                    "Unsupported link mode [" + aModel.feature.getLinkMode() + "] on feature ["
                            + aModel.feature.getName() + "]");
        }

        aModel.indicator.reset();

        // Fetch possible values from the constraint rules
        List<PossibleValue> possibleValues;
        try {
            FeatureStructure featureStructure = selectByAddr(aJCas, state.getSelection()
                .getAnnotation().getId());

            Evaluator evaluator = new ValuesGenerator();
            //Only show indicator if this feature can be affected by Constraint rules!
            aModel.indicator.setAffected(evaluator.isThisAffectedByConstraintRules(
                featureStructure, restrictionFeaturePath, state.getConstraints()));

            possibleValues = evaluator.generatePossibleValues(
                featureStructure, restrictionFeaturePath, state.getConstraints());

            LOG.debug("Possible values for [" + featureStructure.getType().getName() + "] ["
                + restrictionFeaturePath + "]: " + possibleValues);
        }
        catch (Exception e) {
            error("Unable to evaluate constraints: " + ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Unable to evaluate constraints: " + e.getMessage(), e);
            possibleValues = new ArrayList<>();
        }

        // Fetch actual tagset
        List<Tag> valuesFromTagset = annotationService.listTags(aModel.feature.getTagset());

        // First add tags which are suggested by rules and exist in tagset
        List<Tag> tagset = compareSortAndAdd(possibleValues, valuesFromTagset, aModel.indicator);

        // Then add the remaining tags
        for (Tag remainingTag : valuesFromTagset) {
            if (!tagset.contains(remainingTag)) {
                tagset.add(remainingTag);
            }
        }

        // Record the possible values and the (re-ordered) tagset in the feature state
        aModel.possibleValues = possibleValues;
        aModel.tagset = tagset;
    }

    /*
     * Compares existing tagset with possible values resulted from rule evaluation Adds only which
     * exist in tagset and is suggested by rules. The remaining values from tagset are added
     * afterwards.
     */
    private static List<Tag> compareSortAndAdd(List<PossibleValue> possibleValues,
        List<Tag> valuesFromTagset, RulesIndicator rulesIndicator)
    {
        //if no possible values, means didn't satisfy conditions
        if (possibleValues.isEmpty()) {
            rulesIndicator.didntMatchAnyRule();
        }
        
        List<Tag> returnList = new ArrayList<>();
        // Sorting based on important flag
        // possibleValues.sort(null);
        // Comparing to check which values suggested by rules exists in existing
        // tagset and adding them first in list.
        for (PossibleValue value : possibleValues) {
            for (Tag tag : valuesFromTagset) {
                if (value.getValue().equalsIgnoreCase(tag.getName())) {
                    //Matching values found in tagset and shown in dropdown
                    rulesIndicator.rulesApplied();
                    // HACK BEGIN
                    tag.setReordered(true);
                    // HACK END
                    //Avoid duplicate entries
                    if (!returnList.contains(tag)) {
                        returnList.add(tag);
                    }
                }
            }
        }
        
        //If no matching tags found
        if (returnList.isEmpty()) {
            rulesIndicator.didntMatchAnyTag();
        }
        
        return returnList;
    }

    public void reset(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        state.getSelection().clear();
        clearFeatureEditorModels(aTarget);
    }

    private Set<AnnotationFS> getAttachedLinks(AnnotationFS aFs, AnnotationLayer aLayer)
    {
        CAS cas = aFs.getCAS();
        Set<AnnotationFS> attachedLinks = new HashSet<>();
        TypeAdapter adapter = annotationService.getAdapter(aLayer);
        if (adapter instanceof SpanAdapter) {
            for (AnnotationFeature linkFeature : annotationService
                    .listAttachedLinkFeatures(aLayer)) {
                if (MultiValueMode.ARRAY.equals(linkFeature.getMultiValueMode())
                        && LinkMode.WITH_ROLE.equals(linkFeature.getLinkMode())) {
                    // Fetch slot hosts that could link to the current FS and check if any of
                    // them actually links to the current FS
                    Type linkType = CasUtil.getType(cas, linkFeature.getLayer().getName());
                    for (AnnotationFS linkFS : CasUtil.select(cas, linkType)) {
                        List<LinkWithRoleModel> links = adapter.getFeatureValue(linkFeature,
                                linkFS);
                        for (int li = 0; li < links.size(); li++) {
                            LinkWithRoleModel link = links.get(li);
                            AnnotationFS linkTarget = selectByAddr(cas, AnnotationFS.class,
                                    link.targetAddr);
                            // If the current annotation fills a slot, then add the slot host to
                            // our list of attached links.
                            if (isSame(linkTarget, aFs)) {
                                attachedLinks.add(linkFS);
                            }
                        }
                    }
                }
            }
        }
        return attachedLinks;
    }
    
    private Set<AnnotationFS> getAttachedSpans(AnnotationFS aFs, AnnotationLayer aLayer)
    {
        CAS cas = aFs.getCAS();
        Set<AnnotationFS> attachedSpans = new HashSet<>();
        TypeAdapter adapter = annotationService.getAdapter(aLayer);
        if (adapter instanceof SpanAdapter && aLayer.getAttachType() != null) {
            Type spanType = CasUtil.getType(cas, aLayer.getAttachType().getName());
            Feature attachFeature = spanType.getFeatureByBaseName(aLayer.getAttachFeature()
                .getName());

            for (AnnotationFS attachedFs : selectAt(cas, spanType, aFs.getBegin(), aFs.getEnd())) {
                if (isSame(attachedFs.getFeatureValue(attachFeature), aFs)) {
                    attachedSpans.add(attachedFs);
                }
            }
        }
        return attachedSpans;
    }
    
    public Set<AnnotationFS> getAttachedRels(AnnotationFS aFs, AnnotationLayer aLayer)
    {
        CAS cas = aFs.getCAS();
        Set<AnnotationFS> toBeDeleted = new HashSet<>();
        for (AnnotationLayer relationLayer : annotationService
            .listAttachedRelationLayers(aLayer)) {
            ArcAdapter relationAdapter = (ArcAdapter) annotationService.getAdapter(relationLayer);
            Type relationType = CasUtil.getType(cas, relationLayer.getName());
            Feature sourceFeature = relationType.getFeatureByBaseName(relationAdapter
                .getSourceFeatureName());
            Feature targetFeature = relationType.getFeatureByBaseName(relationAdapter
                .getTargetFeatureName());

            // This code is already prepared for the day that relations can go between
            // different layers and may have different attach features for the source and
            // target layers.
            Feature relationSourceAttachFeature = null;
            Feature relationTargetAttachFeature = null;
            if (relationAdapter.getAttachFeatureName() != null) {
                relationSourceAttachFeature = sourceFeature.getRange().getFeatureByBaseName(
                    relationAdapter.getAttachFeatureName());
                relationTargetAttachFeature = targetFeature.getRange().getFeatureByBaseName(
                    relationAdapter.getAttachFeatureName());
            }

            for (AnnotationFS relationFS : CasUtil.select(cas, relationType)) {
                // Here we get the annotations that the relation is pointing to in the UI
                FeatureStructure sourceFS;
                if (relationSourceAttachFeature != null) {
                    sourceFS = relationFS.getFeatureValue(sourceFeature).getFeatureValue(
                        relationSourceAttachFeature);
                }
                else {
                    sourceFS = relationFS.getFeatureValue(sourceFeature);
                }

                FeatureStructure targetFS;
                if (relationTargetAttachFeature != null) {
                    targetFS = relationFS.getFeatureValue(targetFeature).getFeatureValue(
                        relationTargetAttachFeature);
                }
                else {
                    targetFS = relationFS.getFeatureValue(targetFeature);
                }

                if (isSame(sourceFS, aFs) || isSame(targetFS, aFs)) {
                    toBeDeleted.add(relationFS);
                    LOG.debug("Deleted relation [" + getAddr(relationFS) + "] from layer ["
                        + relationLayer.getName() + "]");
                }
            }
        }

        return toBeDeleted;
    }

    public AnnotationFeatureForm getAnnotationFeatureForm()
    {
        return annotationFeatureForm;
    }

    protected static void handleException(Component aComponent, AjaxRequestTarget aTarget,
            Exception aException)
    {
        try {
            throw aException;
        }
        catch (AnnotationException e) {
            if (aTarget != null) {
                aTarget.prependJavaScript("alert('Error: " + e.getMessage() + "')");
            }
            else {
                aComponent.error("Error: " + e.getMessage());
            }
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

    private static String generateMessage(AnnotationLayer aLayer, String aLabel, boolean aDeleted)
    {
        String action = aDeleted ? "deleted" : "created/updated";

        String msg = "The [" + aLayer.getUiName() + "] annotation has been " + action + ".";
        if (StringUtils.isNotBlank(aLabel)) {
            msg += " Label: [" + aLabel + "]";
        }
        return msg;
    }

    Logger getLog() {
        return LOG;
    }

    AnnotationSchemaService getAnnotationService() {
        return annotationService;
    }
    
    public static class AttachStatus {
        boolean readOnlyAttached;
        int attachCount;
    }
}
