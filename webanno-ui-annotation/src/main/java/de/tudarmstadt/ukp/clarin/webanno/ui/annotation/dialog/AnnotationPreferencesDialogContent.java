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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CORRECTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorFactory;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy.ColoringStrategyType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy.ReadonlyColoringBehaviour;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
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
    private final Form<Preferences> form;
    private final IModel<AnnotatorState> stateModel;

    public AnnotationPreferencesDialogContent(String aId, final ModalWindow aModalWindow,
            IModel<AnnotatorState> aModel)
    {
        super(aId);
        
        stateModel = aModel;
        modalWindow = aModalWindow;
        
        form = new Form<>("form", new CompoundPropertyModel<>(loadModel(stateModel.getObject())));

        NumberTextField<Integer> windowSizeField = new NumberTextField<>("windowSize");
        windowSizeField.setType(Integer.class);
        windowSizeField.setMinimum(1);
        form.add(windowSizeField);

        NumberTextField<Integer> sidebarSizeField = new NumberTextField<>("sidebarSize");
        sidebarSizeField.setType(Integer.class);
        sidebarSizeField.setMinimum(AnnotationPreference.SIDEBAR_SIZE_MIN);
        sidebarSizeField.setMaximum(AnnotationPreference.SIDEBAR_SIZE_MAX);
        form.add(sidebarSizeField);

        NumberTextField<Integer> fontZoomField = new NumberTextField<>("fontZoom");
        fontZoomField.setType(Integer.class);
        fontZoomField.setMinimum(AnnotationPreference.FONT_ZOOM_MIN);
        fontZoomField.setMaximum(AnnotationPreference.FONT_ZOOM_MAX);
        form.add(fontZoomField);

        List<Pair<String, String>> editorChoices = annotationEditorRegistry.getEditorFactories()
                .stream()
                .map(f -> Pair.of(f.getBeanName(), f.getDisplayName()))
                .collect(Collectors.toList());
        DropDownChoice<Pair<String, String>> editor = new BootstrapSelect<>("editor");
        editor.setChoiceRenderer(new ChoiceRenderer<>("value"));
        editor.setChoices(editorChoices);
        editor.add(visibleWhen(() -> editor.getChoices().size() > 1));
        form.add(editor);

        // Add layer check boxes and combo boxes
        form.add(createLayerContainer());

        // Add a check box to enable/disable automatic page navigations while annotating
        form.add(new CheckBox("scrollPage"));
        
        form.add(new CheckBox("rememberLayer"));

        // Add global read-only coloring strategy combo box
        DropDownChoice<ReadonlyColoringBehaviour> readOnlyColor = new BootstrapSelect<>(
                "readonlyLayerColoringBehaviour");
        readOnlyColor.setChoices(asList(ReadonlyColoringBehaviour.values()));
        readOnlyColor.setChoiceRenderer(new ChoiceRenderer<>("descriptiveName"));
        form.add(readOnlyColor);

        form.add(new LambdaAjaxButton<>("save", this::actionSave));
        form.add(new LambdaAjaxLink("cancel", this::actionCancel));
        
        add(form);
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Preferences> aForm)
    {
        try {
            AnnotatorState state = stateModel.getObject();
            Preferences model = form.getModelObject();

            AnnotationPreference prefs = state.getPreferences();
            prefs.setScrollPage(model.scrollPage);
            prefs.setRememberLayer(model.rememberLayer);
            prefs.setWindowSize(model.windowSize);
            prefs.setSidebarSize(model.sidebarSize);
            prefs.setFontZoom(model.fontZoom);
            prefs.setColorPerLayer(model.colorPerLayer);
            prefs.setReadonlyLayerColoringBehaviour(model.readonlyLayerColoringBehaviour);
            prefs.setEditor(model.editor.getKey());

            state.setAnnotationLayers(model.annotationLayers.stream()
                    .filter(l -> !prefs.getHiddenAnnotationLayerIds().contains(l.getId()))
                    .collect(Collectors.toList()));
            
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

    private Preferences loadModel(AnnotatorState state)
    {
        AnnotationPreference prefs = state.getPreferences();

        // Import current settings from the annotator
        Preferences model = new Preferences();
        model.windowSize = Math.max(prefs.getWindowSize(), 1);
        model.sidebarSize = prefs.getSidebarSize();
        model.fontZoom = prefs.getFontZoom();
        model.scrollPage = prefs.isScrollPage();
        model.colorPerLayer = prefs.getColorPerLayer();
        model.readonlyLayerColoringBehaviour = prefs.getReadonlyLayerColoringBehaviour();
        model.rememberLayer = prefs.isRememberLayer();

        AnnotationEditorFactory editorFactory = annotationEditorRegistry
                .getEditorFactory(state.getPreferences().getEditor());
        if (editorFactory == null) {
            editorFactory = annotationEditorRegistry.getDefaultEditorFactory();
        }
        model.editor = Pair.of(editorFactory.getBeanName(), editorFactory.getDisplayName());

        model.annotationLayers = annotationService.listAnnotationLayer(state.getProject()).stream()
                // hide disabled Layers
                .filter(layer -> layer.isEnabled())
                // hide Token layer
                .filter(layer -> !Token.class.getName().equals(layer.getName()))
                .filter(layer -> !(layer.getType().equals(CHAIN_TYPE)
                        && (state.getMode().equals(CORRECTION)
                                // disable coreference annotation for correction/curation pages
                                || state.getMode().equals(CURATION))))
                .collect(Collectors.toList());

        return model;
    }
    
    private ListView<AnnotationLayer> createLayerContainer()
    {
        return new ListView<AnnotationLayer>("annotationLayers")
        {
            private static final long serialVersionUID = -4040731191748923013L;

            @Override
            protected void populateItem(ListItem<AnnotationLayer> aItem)
            {
                Preferences prefs = form.getModelObject();
                AnnotationLayer layer = aItem.getModelObject();
                Set<Long> hiddenLayerIds = stateModel.getObject().getPreferences()
                        .getHiddenAnnotationLayerIds();
                
                // add visibility checkbox
                CheckBox layerVisible = new CheckBox("annotationLayerActive",
                        Model.of(!hiddenLayerIds.contains(layer.getId())));

                layerVisible.add(new LambdaAjaxFormComponentUpdatingBehavior("change", _target -> {
                    if (!layerVisible.getModelObject()) {
                        hiddenLayerIds.add(layer.getId());
                    }
                    else {
                        hiddenLayerIds.remove(layer.getId());
                    }
                }));
                aItem.add(layerVisible);

                // add coloring strategy choice
                DropDownChoice<ColoringStrategyType> layerColor = new BootstrapSelect<>(
                        "layercoloring");
                layerColor.setModel(Model.of(prefs.colorPerLayer.get(layer.getId())));
                layerColor.setChoiceRenderer(new ChoiceRenderer<>("descriptiveName"));
                layerColor.setChoices(asList(ColoringStrategyType.values()));
                layerColor.add(new LambdaAjaxFormComponentUpdatingBehavior("change", _target ->
                        prefs.colorPerLayer.put(layer.getId(), layerColor.getModelObject())));
                aItem.add(layerColor);

                // add label
                aItem.add(new Label("annotationLayerDesc", layer.getUiName()));
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
