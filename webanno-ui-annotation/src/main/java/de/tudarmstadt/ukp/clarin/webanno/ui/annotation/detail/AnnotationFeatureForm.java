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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel.handleException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.kendo.ui.form.TextField;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.LinkFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.event.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.event.LinkFeatureDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.Selection;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel.AttachStatus;

// The name "AnnotationFeatureForm" is historic. This class should be refactored into a Panel
public class AnnotationFeatureForm
    extends WebMarkupContainer
{
    private static final Logger LOG = LoggerFactory.getLogger(AnnotationFeatureForm.class);
    
    public static final String ID_PREFIX = "featureEditorHead";
    
    private static final long serialVersionUID = 3635145598405490893L;

    // Top-level containers
    private final WebMarkupContainer noAnnotationWarning;
    private final WebMarkupContainer noFeaturesWarning;
    private final LayerSelectionPanel layerContainer;
    private final WebMarkupContainer buttonContainer;
    private final AnnotationInfoPanel infoContainer;
    private final WebMarkupContainer featureEditorContainer;

    // Parent
    private final AnnotationDetailEditorPanel editorPanel;

    // Components
    private final ConfirmationDialog deleteAnnotationDialog;
    private final ConfirmationDialog replaceAnnotationDialog;
    private final FeatureEditorPanelContent featureEditorPanelContent;
    
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserPreferencesService userPreferencesService;
    private @SpringBean UserDao userDao;

    AnnotationFeatureForm(AnnotationDetailEditorPanel aEditorPanel, String id,
        IModel<AnnotatorState> aState)
    {
        super(id, new CompoundPropertyModel<>(aState));
        editorPanel = aEditorPanel;

        add(deleteAnnotationDialog = createDeleteDialog());
        add(replaceAnnotationDialog = createReplaceDialog());
        
        noAnnotationWarning = new WebMarkupContainer("noAnnotationWarning");
        noAnnotationWarning.setOutputMarkupPlaceholderTag(true);
        noAnnotationWarning
                .add(visibleWhen(() -> !getModelObject().getSelection().getAnnotation().isSet()));
        add(noAnnotationWarning);
        
        noFeaturesWarning = new WebMarkupContainer("noFeaturesWarning");
        noFeaturesWarning.setOutputMarkupPlaceholderTag(true);
        noFeaturesWarning.add(visibleWhen(() -> 
                getModelObject().getSelection().getAnnotation().isSet() && 
                getModelObject().getFeatureStates().isEmpty()));
        add(noFeaturesWarning);

        add(layerContainer = new LayerSelectionPanel("layerContainer", getModel(), this));
        add(infoContainer = new AnnotationInfoPanel("infoContainer", getModel()));
        
        featureEditorContainer = new WebMarkupContainer("featureEditorContainer");
        featureEditorContainer.setOutputMarkupPlaceholderTag(true);
        featureEditorContainer.add(featureEditorPanelContent = createFeatureEditorPanelContent());
        featureEditorContainer.add(createFocusResetHelper());
        featureEditorContainer.add(visibleWhen(() -> 
                getModelObject().getSelection().getAnnotation().isSet() && 
                !getModelObject().getFeatureStates().isEmpty()));
        add(featureEditorContainer);
        
        buttonContainer = new WebMarkupContainer("buttonContainer");
        buttonContainer.setOutputMarkupPlaceholderTag(true);
        buttonContainer.add(createDeleteButton());
        buttonContainer.add(createReverseButton());
        buttonContainer.add(createClearButton());
        add(buttonContainer);
    }
    
    public AnnotatorState getModelObject() {
        return (AnnotatorState) getDefaultModelObject();
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel() {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    private TextField<String> createFocusResetHelper()
    {
        TextField<String> textfield = new TextField<>("focusResetHelper");
        textfield.setModel(Model.of());
        textfield.setOutputMarkupId(true);
        textfield.add(new AjaxFormComponentUpdatingBehavior("focus")
        {
            private static final long serialVersionUID = -3030093250599939537L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                List<FeatureEditor> editors = new ArrayList<>();
                featureEditorPanelContent.getItems().next()
                        .visitChildren(FeatureEditor.class, (e, visit) -> {
                            editors.add((FeatureEditor) e);
                            visit.dontGoDeeper();
                        });
                
                if (!editors.isEmpty()) {
                    aTarget.focusComponent(editors.get(editors.size() - 1).getFocusComponent());
                }
            }
        });
        
        return textfield;
    }

    private WebMarkupContainer createNoFeaturesWarningLabel()
    {
        WebMarkupContainer warning = new WebMarkupContainer("noFeaturesWarning");
        warning.add(visibleWhen(() -> getModelObject().getFeatureStates().isEmpty()));
        return warning;
    }

    private FeatureEditorPanelContent createFeatureEditorPanelContent()
    {
        return new FeatureEditorPanelContent("featureEditors");
    }
    
    public Optional<FeatureEditor> getFirstFeatureEditor()
    {
        Iterator<Item<FeatureState>> itemIterator = featureEditorPanelContent.getItems();
        if (!itemIterator.hasNext()) {
            return Optional.empty();
        }
        else {
            return Optional.ofNullable((FeatureEditor) itemIterator.next().get("editor"));
        }
    }

    private ConfirmationDialog createDeleteDialog()
    {
        return new ConfirmationDialog("deleteAnnotationDialog",
                new StringResourceModel("DeleteDialog.title", this, null));
    }

    private ConfirmationDialog createReplaceDialog()
    {
        return new ConfirmationDialog("replaceAnnotationDialog",
                new StringResourceModel("ReplaceDialog.title", this, null),
                new StringResourceModel("ReplaceDialog.text", this, null));
    }

    private LambdaAjaxLink createClearButton()
    {
        LambdaAjaxLink link = new LambdaAjaxLink("clear", editorPanel::actionClear);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()
                && editorPanel.getEditorPage().isEditable()));
        return link;
    }

    private Component createReverseButton()
    {
        LambdaAjaxLink link = new LambdaAjaxLink("reverse", editorPanel::actionReverse);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(LambdaBehavior.onConfigure(_this -> {
            AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
            
            _this.setVisible(state.getSelection().getAnnotation().isSet() 
                    && state.getSelection().isArc()
                    && RELATION_TYPE.equals(state.getSelectedAnnotationLayer().getType())
                    && editorPanel.getEditorPage().isEditable());

            // Avoid reversing in read-only layers
            _this.setEnabled(state.getSelectedAnnotationLayer() != null
                    && !state.getSelectedAnnotationLayer().isReadonly());
        }));
        return link;
    }

    private LambdaAjaxLink createDeleteButton()
    {
        LambdaAjaxLink link = new LambdaAjaxLink("delete", this::actionDelete);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()
                && editorPanel.getEditorPage().isEditable()));
        // Avoid deleting in read-only layers
        link.add(enabledWhen(() -> getModelObject().getSelectedAnnotationLayer() != null
                && !getModelObject().getSelectedAnnotationLayer().isReadonly()));
        return link;
    }
    
    private void actionDelete(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
        
        AnnotationLayer layer = state.getSelectedAnnotationLayer();
        TypeAdapter adapter = annotationService.getAdapter(layer);

        CAS cas = editorPanel.getEditorCas();
        AnnotationFS fs = selectAnnotationByAddr(cas,
                state.getSelection().getAnnotation().getId());
        
        if (layer.isReadonly()) {
            error("Cannot replace an annotation on a read-only layer.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        
        AttachStatus attachStatus = editorPanel.checkAttachStatus(aTarget, state.getProject(), fs);
        if (attachStatus.readOnlyAttached) {
            error("Cannot delete an annotation to which annotations on read-only layers attach.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }        
        
        if (adapter instanceof SpanAdapter && attachStatus.attachCount > 0) {
            deleteAnnotationDialog.setContentModel(
                    new StringResourceModel("DeleteDialog.text", this, Model.of(layer))
                            .setParameters(attachStatus.attachCount));
            deleteAnnotationDialog.setConfirmAction((aCallbackTarget) -> {
                editorPanel.actionDelete(aCallbackTarget);
            });
            deleteAnnotationDialog.show(aTarget);
        }
        else {
            editorPanel.actionDelete(aTarget);
        }
    }
    
    private void actionReplace(AjaxRequestTarget aTarget) throws IOException
    {
        AnnotatorState state = AnnotationFeatureForm.this.getModelObject();

        AnnotationLayer newLayer = state.getDefaultAnnotationLayer();

        CAS cas = editorPanel.getEditorCas();
        AnnotationFS fs = selectAnnotationByAddr(cas,
                state.getSelection().getAnnotation().getId());
        AnnotationLayer currentLayer = annotationService.findLayer(state.getProject(), fs);
        
        if (currentLayer.isReadonly()) {
            error("Cannot replace an annotation on a read-only layer.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        
        AttachStatus attachStatus = editorPanel.checkAttachStatus(aTarget, state.getProject(), fs);
        if (attachStatus.readOnlyAttached) {
            error("Cannot replace an annotation to which annotations on read-only layers attach.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        
        replaceAnnotationDialog.setContentModel(
                new StringResourceModel("ReplaceDialog.text", AnnotationFeatureForm.this)
                        .setParameters(currentLayer.getUiName(), newLayer.getUiName(),
                                attachStatus.attachCount));
        replaceAnnotationDialog.setConfirmAction((_target) -> {
            // The delete action clears the selection, but we need it to create
            // the new annotation - so we save it.
            Selection savedSel = editorPanel.getModelObject().getSelection().copy();

            // Delete current annotation
            editorPanel.actionDelete(_target);

            // Set up the action to create the replacement annotation
            AnnotationLayer layer = state.getDefaultAnnotationLayer();
            state.getSelection().set(savedSel);
            state.getSelection().setAnnotation(VID.NONE_ID);
            state.setSelectedAnnotationLayer(layer);
            state.setDefaultAnnotationLayer(layer);
            editorPanel.loadFeatureEditorModels(_target);

            // Create the replacement annotation
            editorPanel.actionCreateOrUpdate(_target, editorPanel.getEditorCas());
            _target.add(AnnotationFeatureForm.this);
        });
        replaceAnnotationDialog.setCancelAction((_target) -> {
            state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
            _target.add(AnnotationFeatureForm.this);
        });
        replaceAnnotationDialog.show(aTarget);
    }

    /**
     * Part of <i>forward annotation</i> mode: move focus to the hidden forward annotation input
     * field or to the free text component at the end of the rendering process
     * 
     * @param aResetSelectedTag
     *            whether to clear {@code selectedTag} if the forward features has a tagset. Has
     *            no effect if the forward feature is a free text feature.
     */
    public void focusForwardAnnotationComponent(AjaxRequestTarget aTarget,
            boolean aResetSelectedTag)
    {
        AnnotatorState state = getModelObject();
        if (!state.isForwardAnnotation()) {
            return;
        }
        
        List<AnnotationFeature> features = getEnabledAndVisibleFeatures(
                AnnotationFeatureForm.this.getModelObject()
                        .getDefaultAnnotationLayer());
        if (features.size() != 1) {
            // should not come here in the first place (controlled during
            // forward annotation process)
            return;
        }
        
        AnnotationFeature feature = features.get(0);
        
        // Check if this is a free text annotation or a tagset is attached. Use the hidden
        // forwardAnnotationText element only for tagset based forward annotations
        if (feature.getTagset() == null) {
            getFirstFeatureEditor()
                    .ifPresent(_editor -> autoFocus(aTarget, _editor.getFocusComponent()));
        }
        else {
            aTarget.focusComponent(editorPanel.getForwardAnnotationTextField());
            if (aResetSelectedTag) {
                editorPanel.setForwardAnnotationKeySequence("", "resetting on forward");
            }
        }
    }

    /**
     * Returns all enabled and visible features of the given annotation layer.
     */
    private List<AnnotationFeature> getEnabledAndVisibleFeatures(AnnotationLayer aLayer)
    {
        return annotationService.listAnnotationFeature(aLayer).stream()
                .filter(f -> f.isEnabled())
                .filter(f -> f.isVisible())
                .collect(Collectors.toList());
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        // set read only if annotation is finished or the user is viewing other's work
        setEnabled(editorPanel.getEditorPage().isEditable());
    }

    void updateRememberLayer()
    {
        AnnotatorState state = getModelObject();
        if (state.getPreferences().isRememberLayer()) {
            if (state.getDefaultAnnotationLayer() == null) {
                state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
            }
        }
        else if (!state.getSelection().isArc()) {
            state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
        }
    }

    private class FeatureEditorPanelContent
        extends RefreshingView<FeatureState>
    {
        private static final long serialVersionUID = -8359786805333207043L;

        FeatureEditorPanelContent(String aId)
        {
            super(aId);
            setOutputMarkupId(true);
            // This strategy caches items as long as the panel exists. This is important to
            // allow the Kendo ComboBox datasources to be re-read when constraints change the
            // available tags.
            setItemReuseStrategy(new CachingReuseStrategy());
        }
        
        @Override
        protected void onAfterRender()
        {
            super.onAfterRender();
            
            RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(_target -> {
                // Put focus on hidden input field if we are in forward-mode unless the user has
                // selected an annotation which is not on the forward-mode layer
                AnnotatorState state = getModelObject();
                AnnotationLayer layer = state.getSelectedAnnotationLayer();
                if (
                        getModelObject().isForwardAnnotation() &&
                        layer != null &&
                        layer.equals(state.getDefaultAnnotationLayer())
                ) {
                    focusForwardAnnotationComponent(_target, false);
                }
                // If the user selects or creates an annotation then we put the focus on the
                // first of the feature editors
                else if (!Objects.equals(getRequestCycle().getMetaData(IsSidebarAction.INSTANCE),
                        true)) {
                    getFirstFeatureEditor().ifPresent(_editor -> 
                            autoFocus(_target, _editor.getFocusComponent()));
                }
            });
        }

        @Override
        protected void populateItem(final Item<FeatureState> item)
        {
            LOG.trace("FeatureEditorPanelContent.populateItem("
                + item.getModelObject().feature.getUiName() + ": "
                + item.getModelObject().value + ")");

            // Feature editors that allow multiple values may want to update themselves,
            // e.g. to add another slot.
            item.setOutputMarkupId(true);

            final FeatureState featureState = item.getModelObject();
            final FeatureEditor editor;
            
            // Look up a suitable editor and instantiate it
            FeatureSupport featureSupport = featureSupportRegistry
                    .getFeatureSupport(featureState.feature);
            editor = featureSupport.createEditor("editor", featureEditorContainer, editorPanel,
                    AnnotationFeatureForm.this.getModel(), item.getModel());

            // We need to enable the markup ID here because we use it during the AJAX behavior
            // that automatically saves feature editors on change/blur. 
            // Check addAnnotateActionBehavior.
            editor.setOutputMarkupId(true);
            editor.setOutputMarkupPlaceholderTag(true);
            
            // Ensure that markup IDs of feature editor focus components remain constant across
            // refreshes of the feature editor panel. This is required to restore the focus.
            editor.getFocusComponent().setOutputMarkupId(true);
            editor.getFocusComponent()
                    .setMarkupId(ID_PREFIX + editor.getModelObject().feature.getId());
            
            if (!featureState.feature.getLayer().isReadonly()) {
                AnnotatorState state = getModelObject();

                // Whenever it is updating an annotation, it updates automatically when a
                // component for the feature lost focus - but updating is for every component
                // edited LinkFeatureEditors must be excluded because the auto-update will break
                // the ability to add slots. Adding a slot is NOT an annotation action.
                if (state.getSelection().getAnnotation().isSet()
                    && !(editor instanceof LinkFeatureEditor)) {
                    addAnnotateActionBehavior(editor);
                }
                else if (!(editor instanceof LinkFeatureEditor)) {
                    addRefreshFeaturePanelBehavior(editor);
                }

                // Add tooltip on label
                StringBuilder tooltipTitle = new StringBuilder();
                tooltipTitle.append(featureState.feature.getUiName());
                if (featureState.feature.getTagset() != null) {
                    tooltipTitle.append(" (");
                    tooltipTitle.append(featureState.feature.getTagset().getName());
                    tooltipTitle.append(')');
                }

                Component labelComponent = editor.getLabelComponent();
//                labelComponent.setMarkupId(
//                        ID_PREFIX + editor.getModelObject().feature.getId() + "-w-lbl");
                labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
                labelComponent.add(new DescriptionTooltipBehavior(tooltipTitle.toString(),
                    featureState.feature.getDescription()));
            }
            else {
                editor.getFocusComponent().setEnabled(false);
            }
            
            item.add(editor);
        }

        private void addRefreshFeaturePanelBehavior(final FeatureEditor aFrag)
        {
            aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    AnnotationFeatureForm.this.refresh(aTarget);
                }
            });
        }

        private void addAnnotateActionBehavior(final FeatureEditor aFrag)
        {
            aFrag.addFeatureUpdateBehavior();
        }
        
        @Override
        protected Iterator<IModel<FeatureState>> getItemModels()
        {
            List<FeatureState> featureStates = getModelObject().getFeatureStates();

            return new ModelIteratorAdapter<FeatureState>(
                featureStates)
            {
                @Override
                protected IModel<FeatureState> model(FeatureState aObject)
                {
                    return FeatureStateModel.of(getModel(), aObject);
                }
            };
        }
    }

    private void actionFeatureUpdate(Component aComponent, AjaxRequestTarget aTarget)
    {
        try {
            AnnotatorState state = getModelObject();

            if (state.getConstraints() != null) {
                // Make sure we update the feature editor panel because due to
                // constraints the contents may have to be re-rendered
                AnnotationFeatureForm.this.refresh(aTarget);
            }
            
            // When updating an annotation in the sidebar, we must not force a
            // re-focus after rendering
            getRequestCycle().setMetaData(IsSidebarAction.INSTANCE, true);
            
            CAS cas = editorPanel.getEditorCas();
            AnnotationLayer layer = state.getSelectedAnnotationLayer();
            if (
                    state.isForwardAnnotation() &&
                    layer != null &&
                    layer.equals(state.getDefaultAnnotationLayer())
            ) {
                editorPanel.actionCreateForward(aTarget, cas);
            } else {
                editorPanel.actionCreateOrUpdate(aTarget, cas);
            }
            
            // If the focus was lost during the update, then try force-focusing the
            // next editor or the first one if we are on the last one.
            if (aTarget.getLastFocusedElementId() == null) {
                List<FeatureEditor> allEditors = new ArrayList<>();
                featureEditorPanelContent.visitChildren(FeatureEditor.class,
                    (editor, visit) -> {
                        allEditors.add((FeatureEditor) editor);
                        visit.dontGoDeeper();
                    });

                if (!allEditors.isEmpty()) {
                    FeatureEditor currentEditor = aComponent instanceof FeatureEditor
                            ? (FeatureEditor) aComponent
                            : aComponent.findParent(FeatureEditor.class);
                    
                    int i = allEditors.indexOf(currentEditor);
                    
                    // If the current editor cannot be found then move the focus to the
                    // first editor
                    if (i == -1) {
                        autoFocus(aTarget, allEditors.get(0).getFocusComponent());
                    }
                    // ... if it is the last one, say at the last one
                    else if (i >= (allEditors.size() - 1)) {
                        autoFocus(aTarget, allEditors.get(allEditors.size() - 1)
                                .getFocusComponent());
                    }
                    // ... otherwise move the focus to the next editor
                    else {
                        autoFocus(aTarget, allEditors.get(i + 1).getFocusComponent());
                    }
                }
            }
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }
    }

    public LayerSelectionPanel getLayerContainer()
    {
        return layerContainer;
    }
    
    public void refresh(AjaxRequestTarget aTarget)
    {
        aTarget.add(layerContainer, buttonContainer, infoContainer, featureEditorContainer,
                noAnnotationWarning, noFeaturesWarning);
    }
    
    private static final class IsSidebarAction extends MetaDataKey<Boolean> {
        private static final long serialVersionUID = 1L;
        
        public final static IsSidebarAction INSTANCE = new IsSidebarAction();
    }
    
    public void autoFocus(AjaxRequestTarget aTarget, Component aComponent)
    {
        // Check if any of the features suppresses auto-focus...
        for (FeatureState fstate : getModelObject().getFeatureStates()) {
            AnnotationFeature feature = fstate.getFeature();
            FeatureSupport<?> fs = featureSupportRegistry.getFeatureSupport(feature);
            if (fs.suppressAutoFocus(feature)) {
                return;
            }
        }
        
        aTarget.focusComponent(aComponent);
    }
    
    @OnEvent(stop = true)
    public void onFeatureUpdatedEvent(FeatureEditorValueChangedEvent aEvent)
    {
        AjaxRequestTarget target = aEvent.getTarget();
        actionFeatureUpdate(aEvent.getEditor(), target);
    }
    
    @OnEvent(stop = true)
    public void onLinkFeatureDeletedEvent(LinkFeatureDeletedEvent aEvent)
    {
        AjaxRequestTarget target = aEvent.getTarget();
        // Auto-commit if working on existing annotation
        if (getModelObject().getSelection().getAnnotation().isSet()) {
            try {
                editorPanel.actionCreateOrUpdate(target, editorPanel.getEditorCas());
            }
            catch (Exception e) {
                handleException(this, target, e);
            }
        }
    }
    
    @OnEvent
    public void onDefaultLayerChangedEvent(DefaultLayerChangedEvent aEvent)
    {
        AnnotatorState state = getModelObject();
        
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class).get();

        
        // If "remember layer" is set, the we really just update the selected layer...
        // we do not touch the selected annotation not the annotation detail panel
        if (!state.getPreferences().isRememberLayer()) {

            // If "remember layer" is not set, then changing the layer means that we
            // want to change the type of the currently selected annotation
            if (!Objects
                    .equals(state.getSelectedAnnotationLayer(), state.getDefaultAnnotationLayer())
                    && state.getSelection().getAnnotation().isSet()) {
                try {
                    if (state.getSelection().isArc()) {
                        editorPanel.actionClear(target);
                    }
                    else {
                        actionReplace(target);
                    }
                }
                catch (Exception e) {
                    handleException(this, target, e);
                }
            }
            // If no annotation is selected, then prime the annotation detail panel for the new type
            else {
                state.setSelectedAnnotationLayer(state.getDefaultAnnotationLayer());
                target.add(infoContainer);
                editorPanel.clearFeatureEditorModels(target);
            }
        }
    }
}
