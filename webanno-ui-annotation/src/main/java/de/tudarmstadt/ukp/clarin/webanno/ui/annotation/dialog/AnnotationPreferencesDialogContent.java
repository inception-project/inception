/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorFactory;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy.ColoringStrategyType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy.ReadonlyColoringBehaviour;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.PreferencesUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Modal Window to configure layers, window size, etc.
 */
public class AnnotationPreferencesDialogContent
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Logger LOG = LoggerFactory
            .getLogger(AnnotationPreferencesDialogContent.class);
    
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;

    private final ModalWindow modalWindow;
    private final AnnotationLayerDetailForm form;
    private final IModel<AnnotatorState> stateModel;

    public AnnotationPreferencesDialogContent(String aId, final ModalWindow aModalWindow,
            IModel<AnnotatorState> aModel)
    {
        super(aId);
        stateModel = aModel;
        modalWindow = aModalWindow;
        add(form = new AnnotationLayerDetailForm("form"));
    }
    
    private class AnnotationLayerDetailForm
        extends Form<Preferences>
    {
        private static final long serialVersionUID = -683824912741426241L;

        public AnnotationLayerDetailForm(String id)
        {
            super(id);
            setModel(new CompoundPropertyModel<>(loadModel(stateModel.getObject())));

            NumberTextField<Integer> windowSizeField = new NumberTextField<>("windowSize");
            windowSizeField.setType(Integer.class);
            windowSizeField.setMinimum(1);
            add(windowSizeField);

            NumberTextField<Integer> sidebarSizeField = new NumberTextField<>("sidebarSize");
            sidebarSizeField.setType(Integer.class);
            sidebarSizeField.setMinimum(AnnotationPreference.SIDEBAR_SIZE_MIN);
            sidebarSizeField.setMaximum(AnnotationPreference.SIDEBAR_SIZE_MAX);
            add(sidebarSizeField);

            NumberTextField<Integer> fontZoomField = new NumberTextField<>("fontZoom");
            fontZoomField.setType(Integer.class);
            fontZoomField.setMinimum(AnnotationPreference.FONT_ZOOM_MIN);
            fontZoomField.setMaximum(AnnotationPreference.FONT_ZOOM_MAX);
            add(fontZoomField);

            List<Pair<String, String>> editorChoices = annotationEditorRegistry.getEditorFactories()
                    .stream().map(f -> Pair.of(f.getBeanName(), f.getDisplayName()))
                    .collect(Collectors.toList());
            DropDownChoice<Pair<String, String>> editor = new BootstrapSelect<>("editor",
                    editorChoices, new ChoiceRenderer<>("value"));
            editor.setVisible(editorChoices.size() > 1);
            add(editor);

            // Add layer check boxes and combo boxes
            add(createLayerContainer());

            // Add a check box to enable/disable automatic page navigations while annotating
            add(new CheckBox("scrollPage"));
            
            add(new CheckBox("rememberLayer"));

            // Add global read-only coloring strategy combo box
            ChoiceRenderer<ReadonlyColoringBehaviour> choiceRenderer = new ChoiceRenderer<>(
                    "descriptiveName");
            List<ReadonlyColoringBehaviour> choices = new ArrayList<ReadonlyColoringBehaviour>(
                    EnumSet.allOf(ReadonlyColoringBehaviour.class));
            Model<ReadonlyColoringBehaviour> initialSelected = Model
                    .of(getModelObject().readonlyLayerColoringBehaviour);
            DropDownChoice<ReadonlyColoringBehaviour> rolayer_color = new BootstrapSelect<>(
                    "readonlylayercoloring", initialSelected, choices, choiceRenderer);
            rolayer_color.add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = 1060397773470276585L;

                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    ReadonlyColoringBehaviour selected = rolayer_color.getModelObject();
                    getModelObject().readonlyLayerColoringBehaviour = selected;
                }
            });
            add(rolayer_color);

            add(new LambdaAjaxButton<>("save",
                    AnnotationPreferencesDialogContent.this::actionSave));
            add(new LambdaAjaxLink("cancel",
                    AnnotationPreferencesDialogContent.this::actionCancel));
        }
    }

    private void actionSave(AjaxRequestTarget aTarget,
            Form<Preferences> aForm)
    {
        try {
            AnnotatorState state = stateModel.getObject();
            commitModel(state);
            PreferencesUtil.savePreference(state, projectService);
        }
        catch (IOException e) {
            error("Preference file not found");
        }
        modalWindow.close(aTarget);
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        form.detach();
        onCancel(aTarget);
        modalWindow.close(aTarget);
    }

    private Preferences loadModel(AnnotatorState bModel)
    {
        Preferences model = new Preferences();

        // Import current settings from the annotator
        model.windowSize = bModel.getPreferences().getWindowSize() < 1 ? 1
                : bModel.getPreferences().getWindowSize();
        model.sidebarSize = bModel.getPreferences().getSidebarSize();
        model.fontZoom = bModel.getPreferences().getFontZoom();
        model.scrollPage = bModel.getPreferences().isScrollPage();
        model.colorPerLayer = bModel.getPreferences().getColorPerLayer();
        model.readonlyLayerColoringBehaviour = bModel.getPreferences()
                .getReadonlyLayerColoringBehaviour();
        model.rememberLayer = bModel.getPreferences().isRememberLayer();

        String editorId = bModel.getPreferences().getEditor();
        AnnotationEditorFactory editorFactory = annotationEditorRegistry.getEditorFactory(editorId);
        if (editorFactory == null) {
            editorFactory = annotationEditorRegistry.getDefaultEditorFactory();
        }
        model.editor = Pair.of(editorFactory.getBeanName(), editorFactory.getDisplayName());

        model.annotationLayers = annotationService.listAnnotationLayer(bModel.getProject()).stream()
                // hide disabled Layers
                .filter(layer -> layer.isEnabled())
                // hide Token layer
                .filter(layer -> !Token.class.getName().equals(layer.getName()))
                .filter(layer -> !(layer.getType().equals(WebAnnoConst.CHAIN_TYPE)
                        && (bModel.getMode().equals(Mode.CORRECTION)
                                // disable coreference annotation for correction/curation pages
                                || bModel.getMode().equals(Mode.CURATION))))
                .collect(Collectors.toList());

        return model;
    }
    
    private void commitModel(AnnotatorState state)
    {
        AnnotationPreference prefs = state.getPreferences();
        Preferences model = form.getModelObject();

        List<Long> hiddenLayerIds = state.getPreferences().getHiddenAnnotationLayerIds();
        state.setAnnotationLayers(
                model.annotationLayers.stream()
                .filter(l -> !hiddenLayerIds.contains(l.getId()))
                .collect(Collectors.toList()));

        prefs.setScrollPage(model.scrollPage);
        prefs.setRememberLayer(model.rememberLayer);
        prefs.setWindowSize(model.windowSize);
        prefs.setSidebarSize(model.sidebarSize);
        prefs.setFontZoom(model.fontZoom);
        prefs.setColorPerLayer(model.colorPerLayer);
        prefs.setReadonlyLayerColoringBehaviour(model.readonlyLayerColoringBehaviour);
        prefs.setEditor(model.editor.getKey());
        
        // Make sure the currently selected layer (layer selection dropdown) isn't a layer we
        // have just hidden.
        if (!state.getAnnotationLayers().contains(state.getSelectedAnnotationLayer())) {
            state.setSelectedAnnotationLayer(
                    state.getAnnotationLayers().size() > 0 ? state.getAnnotationLayers().get(0)
                            : null);
            state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
        }
    }

    private ListView<AnnotationLayer> createLayerContainer()
    {
        return new ListView<AnnotationLayer>("annotationLayers")
        {
            private static final long serialVersionUID = -4040731191748923013L;

            @Override
            protected void populateItem(ListItem<AnnotationLayer> item)
            {
                // add checkbox
                // get initial state
                AnnotationPreference pref = stateModel.getObject().getPreferences();
                List<Long> hiddenLayerIds = pref.getHiddenAnnotationLayerIds();
                boolean isPreferredToShow = !hiddenLayerIds.contains(item.getModelObject().getId());
                
                CheckBox layer_cb = new CheckBox("annotationLayerActive",
                        Model.of(isPreferredToShow));
                
                layer_cb.add(new AjaxEventBehavior("change")
                {
                    private static final long serialVersionUID = 8378489004897115519L;

                    @Override
                    protected void onEvent(AjaxRequestTarget target)
                    {
                        // check state & live update preferences
                        List<Long> hiddenLayerIds = stateModel.getObject()
                                .getPreferences().getHiddenAnnotationLayerIds();
                        // get and switch state of checkbox
                        boolean isPreferredToShow = layer_cb.getModelObject();
                        layer_cb.setModelObject(!isPreferredToShow);
                        // live update preferences
                        if (isPreferredToShow) {
                            // prefer to deactivate layer
                            hiddenLayerIds.add(item.getModelObject().getId());
                        }
                        else {
                            // prefer to activate layer
                            hiddenLayerIds.remove(item.getModelObject().getId());
                        }
                    }
                });
                item.add(layer_cb);

                // add coloring strategy combobox
                ChoiceRenderer<ColoringStrategyType> choiceRenderer = new ChoiceRenderer<>(
                        "descriptiveName");
                List<ColoringStrategyType> choices = new ArrayList<ColoringStrategyType>(
                        EnumSet.allOf(ColoringStrategyType.class));
                Model<ColoringStrategyType> initialSelected = Model
                        .of(form.getModelObject().colorPerLayer
                                .get(item.getModelObject().getId()));
                DropDownChoice<ColoringStrategyType> layer_color = new BootstrapSelect<>(
                        "layercoloring", initialSelected, choices, choiceRenderer);
                layer_color.add(new AjaxFormComponentUpdatingBehavior("change")
                {
                    private static final long serialVersionUID = 1060397773470276585L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target)
                    {
                        AnnotationLayer current_layer = item.getModelObject();
                        ColoringStrategyType selectedColor = layer_color.getModelObject();
                        form.getModelObject().colorPerLayer
                                .put(current_layer.getId(), selectedColor);
                    }
                });
                item.add(layer_color);

                // add label
                Label lbl = new Label("annotationLayerDesc", item.getModelObject().getUiName());
                item.add(lbl);
            }
        };
    }

    protected void onCancel(AjaxRequestTarget aTarget)
    {
    }

    private static class Preferences
        implements Serializable
    {
        private static final long serialVersionUID = -1L;
        private Pair<String, String> editor;
        private int windowSize;
        private int sidebarSize;
        private int fontZoom;
        private boolean scrollPage;
        private boolean rememberLayer;
        private List<AnnotationLayer> annotationLayers;
        private ReadonlyColoringBehaviour readonlyLayerColoringBehaviour;
        private Map<Long, ColoringStrategyType> colorPerLayer;
    }
}
