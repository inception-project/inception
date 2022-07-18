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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference.SIDEBAR_SIZE_MAX;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference.SIDEBAR_SIZE_MIN;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
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

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorFactory;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategyType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ReadonlyColoringBehaviour;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationEditorProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationEditorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;

/**
 * Modal Window to configure layers, window size, etc.
 */
public class AnnotationPreferencesDialogContent
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Logger LOG = getLogger(AnnotationPreferencesDialogContent.class);

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean UserDao userDao;
    private @SpringBean UserPreferencesService userPreferencesService;
    private @SpringBean AnnotationEditorProperties annotationEditorProperties;
    private @SpringBean PreferencesService preferencesService;

    private final Form<Preferences> form;
    private final IModel<AnnotatorState> stateModel;
    private final List<Pair<String, String>> editorChoices;

    private final AjaxCallback onChangeAction;

    public AnnotationPreferencesDialogContent(String aId, IModel<AnnotatorState> aModel,
            AjaxCallback aOnChangeAction)
    {
        super(aId);

        stateModel = aModel;
        editorChoices = getEditorChoices();
        onChangeAction = aOnChangeAction;

        form = new Form<>("form", new CompoundPropertyModel<>(loadModel(stateModel.getObject())));

        NumberTextField<Integer> windowSizeField = new NumberTextField<>("windowSize");
        windowSizeField.setType(Integer.class);
        windowSizeField.setMinimum(1);
        form.add(windowSizeField);

        NumberTextField<Integer> sidebarSizeLeftField = new NumberTextField<>("sidebarSizeLeft");
        sidebarSizeLeftField.setType(Integer.class);
        sidebarSizeLeftField.setMinimum(SIDEBAR_SIZE_MIN);
        sidebarSizeLeftField.setMaximum(SIDEBAR_SIZE_MAX);
        form.add(sidebarSizeLeftField);

        NumberTextField<Integer> sidebarSizeRightField = new NumberTextField<>("sidebarSizeRight");
        sidebarSizeRightField.setType(Integer.class);
        sidebarSizeRightField.setMinimum(SIDEBAR_SIZE_MIN);
        sidebarSizeRightField.setMaximum(SIDEBAR_SIZE_MAX);
        form.add(sidebarSizeRightField);

        NumberTextField<Integer> fontZoomField = new NumberTextField<>("fontZoom");
        fontZoomField.setType(Integer.class);
        fontZoomField.setMinimum(AnnotationPreference.FONT_ZOOM_MIN);
        fontZoomField.setMaximum(AnnotationPreference.FONT_ZOOM_MAX);
        form.add(fontZoomField);

        AnnotationEditorState state = preferencesService.loadDefaultTraitsForProject(
                AnnotationPageBase.KEY_EDITOR_STATE, stateModel.getObject().getProject());

        DropDownChoice<Pair<String, String>> editor = new DropDownChoice<>("editor");
        editor.setChoiceRenderer(new ChoiceRenderer<>("value"));
        editor.setChoices(editorChoices);
        editor.add(
                visibleWhen(() -> state.getDefaultEditor() == null && editor.getChoices().size() > 1
                        && ANNOTATION.equals(stateModel.getObject().getMode())));
        form.add(editor);

        // Add layer check boxes and combo boxes
        form.add(createLayerContainer());

        // Add a check box to enable/disable automatic page navigations while annotating
        CheckBox scrollCheckBox = new CheckBox("scrollPage");
        scrollCheckBox.setOutputMarkupId(true);
        form.add(scrollCheckBox);

        // Add a check box to enable/disable arc collapsing
        CheckBox collapseCheckBox = new CheckBox("collapseArcs");
        collapseCheckBox.setOutputMarkupId(true);
        form.add(collapseCheckBox);

        CheckBox rememberCheckbox = new CheckBox("rememberLayer");
        rememberCheckbox.setOutputMarkupId(true);
        rememberCheckbox.setVisible(annotationEditorProperties.isRememberLayerEnabled());
        form.add(rememberCheckbox);

        // Add global read-only coloring strategy combo box
        DropDownChoice<ReadonlyColoringBehaviour> readOnlyColor = new DropDownChoice<>(
                "readonlyLayerColoringBehaviour");
        readOnlyColor.setChoices(asList(ReadonlyColoringBehaviour.values()));
        readOnlyColor.setChoiceRenderer(new ChoiceRenderer<>("descriptiveName"));
        form.add(readOnlyColor);

        queue(new LambdaAjaxButton<>("save", this::actionSave));
        queue(new LambdaAjaxLink("cancel", this::actionCancel));
        queue(new LambdaAjaxLink("closeDialog", this::actionCancel));

        add(form);
    }

    private List<Pair<String, String>> getEditorChoices()
    {
        var editors = annotationEditorRegistry.getEditorFactories().stream()
                .map(f -> Pair.of(f.getBeanName(), f.getDisplayName())) //
                .collect(toList());
        editors.add(0, Pair.of(null, "Auto (based on document format)"));
        return editors;
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
            prefs.setSidebarSizeLeft(model.sidebarSizeLeft);
            prefs.setSidebarSizeRight(model.sidebarSizeRight);
            prefs.setFontZoom(model.fontZoom);
            prefs.setColorPerLayer(model.colorPerLayer);
            prefs.setReadonlyLayerColoringBehaviour(model.readonlyLayerColoringBehaviour);
            prefs.setEditor(model.editor.getKey());
            prefs.setCollapseArcs(model.collapseArcs);

            state.setAllAnnotationLayers(annotationService.listAnnotationLayer(state.getProject()));
            state.setAnnotationLayers(model.annotationLayers.stream()
                    .filter(l -> !prefs.getHiddenAnnotationLayerIds().contains(l.getId()))
                    .collect(Collectors.toList()));

            // Make sure the visibility logic of the right sidebar sees if there are selectable
            // layers
            state.refreshSelectableLayers(annotationEditorProperties);

            userPreferencesService.savePreference(state, userDao.getCurrentUsername());
        }
        catch (IOException e) {
            error("Preference file not found");
        }

        onConfirmInternal(aTarget);
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        form.detach();
        onCancel(aTarget);
        findParent(ModalDialog.class).close(aTarget);
    }

    private Preferences loadModel(AnnotatorState state)
    {
        AnnotationPreference prefs = state.getPreferences();

        // Import current settings from the annotator
        Preferences model = new Preferences();
        model.windowSize = Math.max(prefs.getWindowSize(), 1);
        model.sidebarSizeLeft = prefs.getSidebarSizeLeft();
        model.sidebarSizeRight = prefs.getSidebarSizeRight();
        model.fontZoom = prefs.getFontZoom();
        model.scrollPage = prefs.isScrollPage();
        model.colorPerLayer = prefs.getColorPerLayer();
        model.readonlyLayerColoringBehaviour = prefs.getReadonlyLayerColoringBehaviour();
        model.rememberLayer = prefs.isRememberLayer();
        model.collapseArcs = prefs.isCollapseArcs();

        model.editor = editorChoices.stream().filter(
                editor -> Objects.equals(editor.getKey(), state.getPreferences().getEditor()))
                .findFirst().orElseGet(() -> {
                    AnnotationEditorFactory editorFactory = annotationEditorRegistry
                            .getDefaultEditorFactory();
                    return Pair.of(editorFactory.getBeanName(), editorFactory.getDisplayName());
                });

        model.annotationLayers = annotationService.listAnnotationLayer(state.getProject()).stream()
                // hide disabled Layers
                .filter(layer -> layer.isEnabled())
                // hide blocked layers
                .filter(layer -> !annotationEditorProperties.isLayerBlocked(layer))
                .filter(layer -> !(layer.getType().equals(CHAIN_TYPE)
                        && CURATION == state.getMode()))
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
                DropDownChoice<ColoringStrategyType> layerColor = new DropDownChoice<>(
                        "layercoloring");
                layerColor.setModel(Model.of(prefs.colorPerLayer.get(layer.getId())));
                layerColor.setChoiceRenderer(new ChoiceRenderer<>("descriptiveName"));
                layerColor.setChoices(asList(ColoringStrategyType.values()));
                layerColor.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                        _target -> prefs.colorPerLayer.put(layer.getId(),
                                layerColor.getModelObject())));
                aItem.add(layerColor);

                // add label
                aItem.add(new Label("annotationLayerDesc", layer.getUiName()));
            }
        };
    }

    protected void onCancel(AjaxRequestTarget aTarget)
    {
    }

    protected void onConfirmInternal(AjaxRequestTarget aTarget)
    {
        boolean closeOk = true;

        // Invoke callback if one is defined
        if (onChangeAction != null) {
            try {
                onChangeAction.accept(aTarget);
            }
            catch (Exception e) {
                // LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(),
                // e);
                // state.feedback = "Error: " + e.getMessage();
                // aTarget.add(getContent());
                closeOk = false;
            }
        }

        if (closeOk) {
            findParent(ModalDialog.class).close(aTarget);
        }
    }

    private static class Preferences
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private Pair<String, String> editor;
        private int windowSize;
        private int sidebarSizeLeft;
        private int sidebarSizeRight;
        private int fontZoom;
        private boolean scrollPage;
        private boolean rememberLayer;
        private List<AnnotationLayer> annotationLayers;
        private ReadonlyColoringBehaviour readonlyLayerColoringBehaviour;
        private Map<Long, ColoringStrategyType> colorPerLayer;
        private boolean collapseArcs;
    }
}
