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
import java.util.Objects;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.event.LinkFeatureDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.Selection;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel.AttachStatus;

// The name "AnnotationFeatureForm" is historic. This class should be refactored into a Panel
public class AnnotationFeatureForm
    extends WebMarkupContainer
{
    private static final long serialVersionUID = 3635145598405490893L;

    // Top-level containers
    private final LayerSelectionPanel layerSelectionPanel;
    private final AnnotationInfoPanel selectedAnnotationInfoPanel;
    private final FeatureEditorPanel featureEditorContainer;
    private final WebMarkupContainer buttonContainer;

    // Parent
    private final AnnotationDetailEditorPanel editorPanel;

    // Components
    private final ConfirmationDialog deleteAnnotationDialog;
    private final ConfirmationDialog replaceAnnotationDialog;
    
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserPreferencesService userPreferencesService;
    private @SpringBean UserDao userDao;

    AnnotationFeatureForm(AnnotationDetailEditorPanel aEditorPanel, String id,
        IModel<AnnotatorState> aState)
    {
        super(id, new CompoundPropertyModel<>(aState));
        editorPanel = aEditorPanel;

        add(deleteAnnotationDialog = new ConfirmationDialog("deleteAnnotationDialog",
                new StringResourceModel("DeleteDialog.title", this, null)));
        add(replaceAnnotationDialog = new ConfirmationDialog("replaceAnnotationDialog",
                new StringResourceModel("ReplaceDialog.title", this, null),
                new StringResourceModel("ReplaceDialog.text", this, null)));
        add(layerSelectionPanel = new LayerSelectionPanel("layerContainer", getModel(), this));
        add(selectedAnnotationInfoPanel = new AnnotationInfoPanel("infoContainer", getModel()));
        add(featureEditorContainer = new FeatureEditorPanel("featureEditorContainer", getModel(),
                this));
        
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

    public LayerSelectionPanel getLayerContainer()
    {
        return layerSelectionPanel;
    }
    
    public FeatureEditorPanel getFeatureEditorContainer()
    {
        return featureEditorContainer;
    }
    
    public void refresh(AjaxRequestTarget aTarget)
    {
        aTarget.add(layerSelectionPanel, buttonContainer, selectedAnnotationInfoPanel,
                featureEditorContainer);
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
                target.add(selectedAnnotationInfoPanel);
                editorPanel.clearFeatureEditorModels(target);
            }
        }
    }
}
