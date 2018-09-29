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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel.handleException;
import static java.util.Objects.isNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;

import com.googlecode.wicket.kendo.ui.form.TextField;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.LinkFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.Selection;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.JavascriptUtils;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel.AttachStatus;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class AnnotationFeatureForm
    extends Form<AnnotatorState>
{
    public static final String ID_PREFIX = "featureEditorHead";
    
    private static final long serialVersionUID = 3635145598405490893L;

    // Add "featureEditorPanel" to AjaxRequestTargets instead of "featureEditorPanelContent"
    private WebMarkupContainer featureEditorPanel;
    private FeatureEditorPanelContent featureEditorPanelContent;

    private String selectedTag = "";
    private Label selectedAnnotationLayer;
    private CheckBox forwardAnnotation;
    private TextField<String> forwardAnnotationText;
    private ConfirmationDialog deleteAnnotationDialog;
    private ConfirmationDialog replaceAnnotationDialog;
    private Label relationHint;
    private LayerSelector layerSelector;
    private List<AnnotationLayer> annotationLayers = new ArrayList<>();

    private final AnnotationDetailEditorPanel editorPanel;
    
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;

    AnnotationFeatureForm(AnnotationDetailEditorPanel aEditorPanel, String id,
        IModel<AnnotatorState> aBModel)
    {
        super(id, new CompoundPropertyModel<>(aBModel));
        editorPanel = aEditorPanel;
        add(forwardAnnotationText = createForwardAnnotationTextField());
        add(forwardAnnotation = createForwardAnnotationCheckBox());
        add(createNoAnnotationWarningLabel());
        add(deleteAnnotationDialog = createDeleteDialog());
        add(replaceAnnotationDialog = createReplaceDialog());
        add(createDeleteButton());
        add(createReverseButton());
        add(createClearButton());
        add(relationHint = createRelationHint());
        add(layerSelector = createDefaultAnnotationLayerSelector());
        add(featureEditorPanel = createFeatureEditorPanel());
    }

    private WebMarkupContainer createFeatureEditorPanel()
    {
        WebMarkupContainer container = new WebMarkupContainer("featureEditorsContainer")
        {
            private static final long serialVersionUID = 8908304272310098353L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(getModelObject().getSelection().getAnnotation().isSet());
            }
        };

        // Add placeholder since wmc might start out invisible. Without the placeholder we
        // cannot make it visible in an AJAX call
        container.setOutputMarkupPlaceholderTag(true);
        container.setOutputMarkupId(true);

        container.add(createNoFeaturesWarningLabel());
        container.add(featureEditorPanelContent = createFeatureEditorPanelContent());
        container.add(createSelectedTextLabel());
        container.add(selectedAnnotationLayer = createSelectedAnnotationLayerLabel());

        return container;
    }

    private Label createNoFeaturesWarningLabel()
    {
        return new Label("noFeaturesWarning", "No features available!")
        {
            private static final long serialVersionUID = 4398704672665066763L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(getModelObject().getFeatureStates().isEmpty());
            }
        };
    }

    private FeatureEditorPanelContent createFeatureEditorPanelContent()
    {
        return new FeatureEditorPanelContent("featureValues");
    }

    private TextField<String> createForwardAnnotationTextField()
    {
        TextField<String> textfield = new TextField<>("forwardAnno");
        textfield.setOutputMarkupId(true);
        textfield.add(new AjaxFormComponentUpdatingBehavior("keyup")
        {
            private static final long serialVersionUID = 4554834769861958396L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
            {
                super.updateAjaxAttributes(attributes);

                IAjaxCallListener listener = new AjaxCallListener()
                {
                    private static final long serialVersionUID = -7968540662654079601L;

                    @Override
                    public CharSequence getPrecondition(Component component)
                    {
                        return "var keycode = Wicket.Event.keyCode(attrs.event);    return true;";
                    }
                };
                attributes.getAjaxCallListeners().add(listener);

                attributes.getDynamicExtraParameters().add("var eventKeycode = Wicket.Event" +
                        ".keyCode(attrs.event);return {keycode: eventKeycode};");
                attributes.setPreventDefault(false);
            }

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                final Request request = RequestCycle.get().getRequest();
                final String jsKeycode = request.getRequestParameters()
                        .getParameterValue("keycode").toString("");
                if (jsKeycode.equals("32")) {
                    try {
                        JCas jCas = editorPanel.getEditorCas();
                        editorPanel.actionCreateForward(aTarget, jCas);
                        selectedTag = "";
                    }
                    catch (Exception e) {
                        handleException(textfield, aTarget, e);
                    }
                    return;
                }
                if (jsKeycode.equals("13")) {
                    selectedTag = "";
                    return;
                }
                selectedTag = (textfield.getModelObject() == null ? ""
                        : textfield.getModelObject().charAt(0)) + selectedTag;
                Map<String, String> bindTags = getBindTags();
                if (!bindTags.isEmpty()) {
                    List<FeatureState> featureStates = getModelObject().getFeatureStates();
                    featureStates.get(0).value = getKeyBindValue(selectedTag, bindTags);
                }
                
                aTarget.add(textfield);
                aTarget.add(featureEditorPanelContent.iterator().next());
            }
        });
        textfield.add(new AttributeAppender("style", "opacity:0", ";"));
        // forwardAnno.add(new AttributeAppender("style", "filter:alpha(opacity=0)", ";"));
        return textfield;
    }
    
    public FeatureEditor getFirstFeatureEditor()
    {
        Iterator<Item<FeatureState>> itemIterator = featureEditorPanelContent.getItems();
        if (!itemIterator.hasNext()) {
            return null;
        }
        else {
            return (FeatureEditor) itemIterator.next().get("editor");
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

    private Label createSelectedAnnotationLayerLabel()
    {
        return new Label("selectedAnnotationLayer", new Model<String>())
        {
            private static final long serialVersionUID = 4059460390544343324L;

            {
                setOutputMarkupId(true);
            }
            
            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(getModelObject().getPreferences().isRememberLayer());
            }
        };
    }

    private Label createRelationHint()
    {
        return new Label("relationHint", Model.of()) {
            private static final long serialVersionUID = 1L;
            
            {
                setOutputMarkupId(true);
                setOutputMarkupPlaceholderTag(true);
                setEscapeModelStrings(false);
            }
            
            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                
                if (layerSelector.getModelObject() != null) {
                    List<AnnotationLayer> relLayers = annotationService
                            .listAttachedRelationLayers(layerSelector.getModelObject());
                    if (relLayers.isEmpty()) {
                        setVisible(false);
                    }
                    else if (relLayers.size() == 1) {
                        setDefaultModelObject("Create a <b>" + relLayers.get(0).getUiName()
                                + "</b> relation by drawing an arc between annotations of this layer.");
                        setVisible(true);
                    }
                    else {
                        setDefaultModelObject(
                                "Whoops! Found more than one relation layer attaching to this span layer!");
                        setVisible(true);
                    }
                }
                else {
                    setVisible(false);
                }
            }
        };
    }
    
    private LayerSelector createDefaultAnnotationLayerSelector()
    {
        return new LayerSelector("defaultAnnotationLayer",
            new PropertyModel<>(this, "annotationLayers"));
    }

    private Label createSelectedTextLabel()
    {
        Label selectedTextLabel = new Label("selectedText", PropertyModel.of(getModelObject(),
                "selection.text"));
        selectedTextLabel.setOutputMarkupId(true);
        return selectedTextLabel;
    }

    private LambdaAjaxLink createClearButton()
    {
        return new LambdaAjaxLink("clear", editorPanel::actionClear).onConfigure((_this) -> {
            _this.setVisible(AnnotationFeatureForm.this.getModelObject().getSelection()
                    .getAnnotation().isSet());
        });
    }

    private Component createReverseButton()
    {
        return new LambdaAjaxLink("reverse", editorPanel::actionReverse)
            .onConfigure((_this) -> {
                AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
    
                _this.setVisible(state.getSelection().getAnnotation().isSet()
                        && state.getSelection().isArc() && state.getSelectedAnnotationLayer()
                                .getType().equals(WebAnnoConst.RELATION_TYPE));
    
                // Avoid reversing in read-only layers
                _this.setEnabled(state.getSelectedAnnotationLayer() != null
                        && !state.getSelectedAnnotationLayer().isReadonly());
            })
            .setOutputMarkupPlaceholderTag(true);
    }

    private LambdaAjaxLink createDeleteButton()
    {
        return new LambdaAjaxLink("delete", this::actionDelete).onConfigure((_this) -> {
            AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
            _this.setVisible(state.getSelection().getAnnotation().isSet());
            // Avoid deleting in read-only layers
            _this.setEnabled(state.getSelectedAnnotationLayer() != null
                    && !state.getSelectedAnnotationLayer().isReadonly());
        });
    }
    
    private void actionDelete(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
        
        AnnotationLayer layer = state.getSelectedAnnotationLayer();
        TypeAdapter adapter = annotationService.getAdapter(layer);

        JCas jCas = editorPanel.getEditorCas();
        AnnotationFS fs = selectByAddr(jCas, state.getSelection().getAnnotation().getId());
        
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

        AnnotationLayer newLayer = layerSelector.getModelObject();

        JCas jCas = editorPanel.getEditorCas();
        AnnotationFS fs = selectByAddr(jCas, state.getSelection().getAnnotation().getId());
        AnnotationLayer currentLayer = annotationService.getLayer(state.getProject(), fs);
        
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
            AnnotationLayer layer = layerSelector.getModelObject();
            state.getSelection().set(savedSel);
            state.getSelection().setAnnotation(VID.NONE_ID);
            state.setSelectedAnnotationLayer(layer);
            state.setDefaultAnnotationLayer(layer);
            selectedAnnotationLayer.setDefaultModelObject(layer.getUiName());
            editorPanel.loadFeatureEditorModels(_target);

            // Create the replacement annotation
            editorPanel.actionCreateOrUpdate(_target, editorPanel.getEditorCas());
            layerSelector.modelChanged();
            _target.add(AnnotationFeatureForm.this);
        });
        replaceAnnotationDialog.setCancelAction((_target) -> {
            state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
            _target.add(AnnotationFeatureForm.this);
        });
        replaceAnnotationDialog.show(aTarget);
    }

    private Label createNoAnnotationWarningLabel()
    {
        return new Label("noAnnotationWarning", "No annotation selected!")
        {
            private static final long serialVersionUID = -6046409838139863541L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(!getModelObject().getSelection().getAnnotation().isSet());
            }
        };
    }

    private CheckBox createForwardAnnotationCheckBox()
    {
        return new CheckBox("forwardAnnotation")
        {
            private static final long serialVersionUID = 8908304272310098353L;

            {
                setOutputMarkupId(true);
                add(new AjaxFormComponentUpdatingBehavior("change")
                {
                    private static final long serialVersionUID = 5179816588460867471L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget aTarget) {
                        if (AnnotationFeatureForm.this.getModelObject().isForwardAnnotation()) {
                            List<AnnotationFeature> features = getEnabledFeatures(
                                    AnnotationFeatureForm.this.getModelObject()
                                            .getSelectedAnnotationLayer());
                            if (features.size() > 1) {
                                // should not come here in the first place (controlled during
                                // forward annotation process checking)
                                return;
                            }
                            
                            // Check if this is a free text annotation or a tagset is attached. Use
                            // the hidden forwardAnnotationText element only for tagset based
                            // forward annotations
                            if (!features.isEmpty() && features.get(0).getTagset() == null) {
                                FeatureEditor editor = getFirstFeatureEditor();
                                if (editor != null) {
                                    aTarget.focusComponent(editor.getFocusComponent());
                                }
                            } else {
                                aTarget.appendJavaScript(
                                        JavascriptUtils.getFocusScript(forwardAnnotationText));
                                selectedTag = "";
                            }
                        }
                    }
                });
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(isForwardable());
                if (!isForwardable()) {
                    AnnotationFeatureForm.this.getModelObject().setForwardAnnotation(false);
                }
            }
        };
    }

    private List<AnnotationFeature> getEnabledFeatures(AnnotationLayer aLayer) {
        return annotationService.listAnnotationFeature(aLayer).stream().filter(f -> f.isEnabled())
                .filter(f -> f.isVisible()).collect(Collectors.toList());
    }

    private boolean isForwardable()
    {
        AnnotatorState state = getModelObject();
        AnnotationLayer selectedLayer = state.getSelectedAnnotationLayer();

        if (isNull(selectedLayer) || isNull(selectedLayer.getId())) {
            return false;
        }

        if (!selectedLayer.getType().equals(WebAnnoConst.SPAN_TYPE)) {
            return false;
        }

        if (!selectedLayer.isLockToTokenOffset()) {
            return false;
        }

        // no forward annotation for multi-feature and zero-feature  layers (where features count
        // which are are both enabled and visible).
        if (getEnabledFeatures(selectedLayer).size() != 1) {
            return false;
        }

        // we allow forward annotation only for a feature with a tagset
        if (annotationService.listAnnotationFeature(selectedLayer).get(0).getTagset() != null) {
            // there should be at least one tag in the tagset
            TagSet tagSet = annotationService.listAnnotationFeature(selectedLayer).get(0)
                    .getTagset();
            return !annotationService.listTags(tagSet).isEmpty();
        }

        // Or layers with a single visible/enabled free-text feature.
        return true;
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        // Avoid reversing in read-only layers
        setEnabled(getModelObject().getDocument() != null && !editorPanel.isAnnotationFinished());
    }

    private String getKeyBindValue(String aKey, Map<String, String> aBindTags)
    {
        // check if all the key pressed are the same character
        // if not, just check a Tag for the last char pressed
        if (aKey.isEmpty()) {
            return aBindTags.get(aBindTags.keySet().iterator().next());
        }
        char prevC = aKey.charAt(0);
        for (char ch : aKey.toCharArray()) {
            if (ch != prevC) {
                break;
            }
        }

        if (aBindTags.get(aKey) != null) {
            return aBindTags.get(aKey);
        }
        // re-cycle suggestions
        if (aBindTags.containsKey(aKey.substring(0, 1))) {
            selectedTag = aKey.substring(0, 1);
            return aBindTags.get(aKey.substring(0, 1));
        }
        // set it to the first in the tag list , when arbitrary key is pressed
        return aBindTags.get(aBindTags.keySet().iterator().next());
    }

    Map<String, String> getBindTags()
    {
        AnnotationFeature f = annotationService
                .listAnnotationFeature(getModelObject().getSelectedAnnotationLayer()).get(0);
        TagSet tagSet = f.getTagset();
        Map<Character, String> tagNames = new LinkedHashMap<>();
        Map<String, String> bindTag2Key = new LinkedHashMap<>();
        for (Tag tag : annotationService.listTags(tagSet)) {
            if (tagNames.containsKey(tag.getName().toLowerCase().charAt(0))) {
                String oldBinding = tagNames.get(tag.getName().toLowerCase().charAt(0));
                String newBinding = oldBinding + tag.getName().toLowerCase().charAt(0);
                tagNames.put(tag.getName().toLowerCase().charAt(0), newBinding);
                bindTag2Key.put(newBinding, tag.getName());
            }
            else {
                tagNames.put(tag.getName().toLowerCase().charAt(0),
                    tag.getName().toLowerCase().substring(0, 1));
                bindTag2Key.put(tag.getName().toLowerCase().substring(0, 1), tag.getName());
            }
        }
        return bindTag2Key;

    }

    public void updateLayersDropdown()
    {
        editorPanel.getLog().trace("updateLayersDropdown()");

        AnnotatorState state = getModelObject();
        annotationLayers.clear();
        AnnotationLayer l = null;
        for (AnnotationLayer layer : state.getAnnotationLayers()) {
            if (!layer.isEnabled() || layer.isReadonly()
                || layer.getName().equals(Token.class.getName())) {
                continue;
            }
            if (layer.getType().equals(WebAnnoConst.SPAN_TYPE)) {
                annotationLayers.add(layer);
                l = layer;
            }
            // manage chain type
            else if (layer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                for (AnnotationFeature feature : annotationService.listAnnotationFeature(layer)) {
                    if (!feature.isEnabled()) {
                        continue;
                    }
                    if (feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                        annotationLayers.add(layer);
                    }

                }
            }
            // chain
        }
        if (state.getDefaultAnnotationLayer() != null) {
            state.setSelectedAnnotationLayer(state.getDefaultAnnotationLayer());
        }
        else if (l != null) {
            state.setSelectedAnnotationLayer(l);
        }
    }

    void updateRememberLayer()
    {
        editorPanel.getLog().trace("updateRememberLayer()");

        AnnotatorState state = getModelObject();
        if (state.getPreferences().isRememberLayer()) {
            if (state.getDefaultAnnotationLayer() == null) {
                state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
            }
        }
        else if (!state.getSelection().isArc()) {
            state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
        }

        // if no layer is selected in Settings
        if (state.getSelectedAnnotationLayer() != null) {
            selectedAnnotationLayer.setDefaultModelObject(
                state.getSelectedAnnotationLayer().getUiName());
        }
    }

    protected class LayerSelector
        extends DropDownChoice<AnnotationLayer>
    {
        private static final long serialVersionUID = 2233133653137312264L;

        LayerSelector(String aId, IModel<List<? extends AnnotationLayer>> aChoices)
        {
            super(aId, aChoices, new ChoiceRenderer<>("uiName"));
            setOutputMarkupId(true);
            add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    AnnotatorState state = AnnotationFeatureForm.this.getModelObject();

                    aTarget.add(relationHint);
                    aTarget.add(forwardAnnotation);
                    
                    // If forward annotation was enabled, disable it
                    if (state.isForwardAnnotation()) {
                        state.setForwardAnnotation(false);
                    }
                    
                    // If "remember layer" is set, the we really just update the selected
                    // layer...
                    // we do not touch the selected annotation not the annotation detail panel
                    if (state.getPreferences().isRememberLayer()) {
                        state.setSelectedAnnotationLayer(getModelObject());
                    }
                    
                    // If "remember layer" is not set, then changing the layer means that we
                    // want to change the type of the currently selected annotation
                    else if (!state.getSelectedAnnotationLayer().equals(getModelObject())
                        && state.getSelection().getAnnotation().isSet()) {
                        try {
                            if (state.getSelection().isArc()) {
                                editorPanel.actionClear(aTarget);
                            }
                            else {
                                actionReplace(aTarget);
                            }
                        }
                        catch (Exception e) {
                            handleException(AnnotationFeatureForm.LayerSelector.this,
                                aTarget, e);
                        }
                    }
                    // If no annotation is selected, then prime the annotation detail panel for
                    // the new type
                    else {
                        state.setSelectedAnnotationLayer(getModelObject());
                        selectedAnnotationLayer.setDefaultModelObject(getModelObject().getUiName());
                        aTarget.add(selectedAnnotationLayer);
                        editorPanel.clearFeatureEditorModels(aTarget);
                    }
                }
            });
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
            
            RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(target -> {
                // Put focus on hidden input field if we are in forward-mode
                if (getModelObject().isForwardAnnotation()) {
                    List<AnnotationFeature> features = getEnabledFeatures(
                            AnnotationFeatureForm.this.getModelObject()
                                    .getSelectedAnnotationLayer());
                    if (features.size() > 1) {
                        // should not come here in the first place (controlled during
                        // forward annotation process)
                        return;
                    }
                    // Check if this is a free text annotation or a tagset is attached. Use
                    // the hidden forwardAnnotationText element only for tagset based
                    // forward annotations
                    if (features.size() > 0 && features.get(0).getTagset() == null) {
                        FeatureEditor editor = getFirstFeatureEditor();
                        if (editor != null) {
                            target.focusComponent(editor.getFocusComponent());
                        }
                    } 
                    else {
                        target.focusComponent(forwardAnnotationText);
                    }
                }
                // If the user selects or creates an annotation then we put the focus on the
                // first of the feature editors
                else if (!Objects.equals(getRequestCycle().getMetaData(IsSidebarAction.INSTANCE),
                        true)) {
                    FeatureEditor editor = getFirstFeatureEditor();
                    if (editor != null) {
                        target.focusComponent(editor.getFocusComponent());
                    }
                }
            });
        }

        @Override
        protected void populateItem(final Item<FeatureState> item)
        {
            editorPanel.getLog().trace("FeatureEditorPanelContent.populateItem("
                + item.getModelObject().feature.getUiName() + ": "
                + item.getModelObject().value + ")");

            // Feature editors that allow multiple values may want to update themselves,
            // e.g. to add another slot.
            item.setOutputMarkupId(true);

            final FeatureState featureState = item.getModelObject();
            final FeatureEditor frag;
            
            // Look up a suitable editor and instantiate it
            FeatureSupport featureSupport = featureSupportRegistry
                    .getFeatureSupport(featureState.feature);
            frag = featureSupport.createEditor("editor", AnnotationFeatureForm.this, editorPanel,
                    AnnotationFeatureForm.this.getModel(), item.getModel());

            if (!featureState.feature.getLayer().isReadonly()) {
                AnnotatorState state = getModelObject();

                // Whenever it is updating an annotation, it updates automatically when a
                // component for the feature lost focus - but updating is for every component
                // edited LinkFeatureEditors must be excluded because the auto-update will break
                // the ability to add slots. Adding a slot is NOT an annotation action.
                if (state.getSelection().getAnnotation().isSet()
                    && !(frag instanceof LinkFeatureEditor)) {
                    addAnnotateActionBehavior(frag);
                }
                else if (!(frag instanceof LinkFeatureEditor)) {
                    addRefreshFeaturePanelBehavior(frag);
                }

                // Add tooltip on label
                StringBuilder tooltipTitle = new StringBuilder();
                tooltipTitle.append(featureState.feature.getUiName());
                if (featureState.feature.getTagset() != null) {
                    tooltipTitle.append(" (");
                    tooltipTitle.append(featureState.feature.getTagset().getName());
                    tooltipTitle.append(')');
                }

                Component labelComponent = frag.getLabelComponent();
                labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
                labelComponent.add(new DescriptionTooltipBehavior(tooltipTitle.toString(),
                    featureState.feature.getDescription()));
            }
            else {
                frag.getFocusComponent().setEnabled(false);
            }

            // We need to enable the markup ID here because we use it during the AJAX behavior
            // that automatically saves feature editors on change/blur. 
            // Check addAnnotateActionBehavior.
            frag.setOutputMarkupId(true);
            frag.setOutputMarkupPlaceholderTag(true);
            
            // Ensure that markup IDs of feature editor focus components remain constant across
            // refreshes of the feature editor panel. This is required to restore the focus.
            frag.getFocusComponent().setOutputMarkupId(true);
            frag.getFocusComponent().setMarkupId(ID_PREFIX + frag.getModelObject().feature.getId());
            
            item.add(frag);
        }

        private void addRefreshFeaturePanelBehavior(final FeatureEditor aFrag)
        {
            aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    aTarget.add(featureEditorPanel);
                }
            });
        }

        private void addAnnotateActionBehavior(final FeatureEditor aFrag)
        {
            aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes aAttributes)
                {
                    super.updateAjaxAttributes(aAttributes);
                    // When focus is on a feature editor and the user selects a new annotation,
                    // there is a race condition between the saving the value of the feature
                    // editor and the loading of the new annotation. Delay the feature editor
                    // save to give preference to loading the new annotation.
                    aAttributes.setThrottlingSettings(new ThrottlingSettings(getMarkupId(),
                        Duration.milliseconds(250), true));
                    aAttributes.getAjaxCallListeners().add(new AjaxCallListener()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public CharSequence getPrecondition(Component aComponent)
                        {
                            // If the panel refreshes because the user selects a new annotation,
                            // the annotation editor panel is updated for the new annotation
                            // first (before saving values) because of the delay set above. When
                            // the delay is over, we can no longer save the value because the
                            // old component is no longer there. We use the markup id of the
                            // editor fragments to check if the old component is still there
                            // (i.e. if the user has just tabbed to a new field) or if the old
                            // component is gone (i.e. the user selected/created another
                            // annotation). If the old component is no longer there, we abort
                            // the delayed save action.
                            return "return $('#" + aFrag.getMarkupId() + "').length > 0;";
                        }
                    });
                }

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    try {
                        AnnotatorState state = getModelObject();

                        if (state.getConstraints() != null) {
                            // Make sure we update the feature editor panel because due to
                            // constraints the contents may have to be re-rendered
                            aTarget.add(featureEditorPanel);
                        }
                        
                        // When updating an annotation in the sidebar, we must not force a
                        // re-focus after rendering
                        getRequestCycle().setMetaData(IsSidebarAction.INSTANCE, true);
                        
                        JCas jCas = editorPanel.getEditorCas();
                        if (state.isForwardAnnotation()) {
                            editorPanel.actionCreateForward(aTarget, jCas);
                        } else {
                            editorPanel.actionCreateOrUpdate(aTarget, jCas);
                        }
                    }
                    catch (Exception e) {
                        handleException(AnnotationFeatureForm.FeatureEditorPanelContent.this,
                            aTarget, e);
                    }
                }
            });
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

    protected void setSelectedTag(String aSelectedTag)
    {
        selectedTag = aSelectedTag;
    }

    protected TextField<String> getForwardAnnotationText()
    {
        return forwardAnnotationText;
    }

    protected List<AnnotationLayer> getAnnotationLayers()
    {
        return annotationLayers;
    }

    protected WebMarkupContainer getFeatureEditorPanel()
    {
        return featureEditorPanel;
    }
    
    protected LayerSelector getLayerSelector()
    {
        return layerSelector;
    }
    
    private static final class IsSidebarAction extends MetaDataKey<Boolean> {
        private static final long serialVersionUID = 1L;
        
        public final static IsSidebarAction INSTANCE = new IsSidebarAction();
    }
}
