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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationEditorManagerPrefs.KEY_ANNOTATION_EDITOR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference.FONT_ZOOM_MAX;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference.FONT_ZOOM_MIN;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreferencesChangedEvent;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactory;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringStrategyType;
import de.tudarmstadt.ukp.inception.rendering.coloring.ReadonlyColoringStrategy;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

/**
 * Modal Window to configure layers, window size, etc.
 */
public class AnnotationPreferencesDialogContent
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Logger LOG = getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean UserDao userDao;
    private @SpringBean UserPreferencesService userPreferencesService;
    private @SpringBean AnnotationSchemaProperties annotationEditorProperties;
    private @SpringBean PreferencesService preferencesService;

    private final Form<Preferences> form;
    private final IModel<Project> projectModel;
    private final Mode mode;
    private final List<Pair<String, String>> editorChoices;

    private final AjaxCallback onChangeAction;

    public AnnotationPreferencesDialogContent(String aId, IModel<Project> aProject, Mode aMode,
            AjaxCallback aOnChangeAction)
    {
        super(aId);

        projectModel = aProject;
        mode = aMode;
        editorChoices = getEditorChoices();
        onChangeAction = aOnChangeAction;

        form = new Form<>("form", new CompoundPropertyModel<>(loadPreferences()));

        var windowSizeField = new NumberTextField<Integer>("windowSize");
        windowSizeField.setType(Integer.class);
        windowSizeField.setMinimum(1);
        form.add(windowSizeField);

        var fontZoomField = new NumberTextField<Integer>("fontZoom");
        fontZoomField.setType(Integer.class);
        fontZoomField.setMinimum(FONT_ZOOM_MIN);
        fontZoomField.setMaximum(FONT_ZOOM_MAX);
        form.add(fontZoomField);

        var state = preferencesService
                .loadDefaultTraitsForProject(KEY_ANNOTATION_EDITOR_MANAGER_PREFS, getProject());

        var editor = new DropDownChoice<Pair<String, String>>("editor");
        editor.setChoiceRenderer(new ChoiceRenderer<>("value"));
        editor.setChoices(editorChoices);
        editor.add(visibleWhen(() -> state.getDefaultEditor() == null
                && editor.getChoices().size() > 1 && ANNOTATION == mode));
        form.add(editor);

        // Add layer check boxes and combo boxes
        form.add(createLayerContainer());

        // Add a check box to enable/disable automatic page navigations while annotating
        var scrollCheckBox = new CheckBox("scrollPage");
        scrollCheckBox.setOutputMarkupId(true);
        form.add(scrollCheckBox);

        // Add a check box to enable/disable arc collapsing
        var collapseCheckBox = new CheckBox("collapseArcs");
        collapseCheckBox.setOutputMarkupId(true);
        form.add(collapseCheckBox);

        // Add global read-only coloring strategy combo box
        var readOnlyColor = new DropDownChoice<ReadonlyColoringStrategy>(
                "readonlyLayerColoringBehaviour");
        readOnlyColor.setChoices(asList(ReadonlyColoringStrategy.values()));
        readOnlyColor.setChoiceRenderer(new ChoiceRenderer<>("descriptiveName"));
        form.add(readOnlyColor);

        queue(new LambdaAjaxButton<>("save", this::actionSave));
        queue(new LambdaAjaxLink("cancel", this::actionCancel));
        queue(new LambdaAjaxLink("closeDialog", this::actionCancel));

        add(form);
    }

    private List<Pair<String, String>> getEditorChoices()
    {
        var editors = annotationEditorRegistry.getEditorFactories().stream() //
                .filter(AnnotationEditorFactory::isUserSelectable) //
                .map(f -> Pair.of(f.getBeanName(), f.getDisplayName())) //
                .collect(toList());
        editors.add(0, Pair.of(null, "Auto (based on document format)"));
        return editors;
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Preferences> aForm)
    {
        try {
            var sessionOwner = userDao.getCurrentUsername();
            var project = getProject();
            var model = form.getModelObject();

            var prefs = userPreferencesService.loadPreferences(project, sessionOwner, mode);

            var editorStructureAffected = !Objects.equals(prefs.getEditor(), model.editor.getKey())
                    || prefs.getWindowSize() != model.windowSize;

            prefs.setScrollPage(model.scrollPage);
            prefs.setWindowSize(model.windowSize);
            prefs.setFontZoom(model.fontZoom);
            prefs.setColorPerLayer(model.colorPerLayer);
            prefs.setReadonlyLayerColoringBehaviour(model.readonlyLayerColoringBehaviour);
            prefs.setEditor(model.editor.getKey());
            prefs.setCollapseArcs(model.collapseArcs);

            userPreferencesService.savePreferences(project, sessionOwner, mode, prefs);

            send(getPage(), Broadcast.BREADTH, new AnnotationPreferencesChangedEvent(project,
                    sessionOwner, aTarget, editorStructureAffected));
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

    private Project getProject()
    {
        return projectModel.getObject();
    }

    private Preferences loadPreferences()
    {
        var prefs = loadStoredPreferences();

        // Import current settings from the annotator
        var model = new Preferences();
        model.windowSize = Math.max(prefs.getWindowSize(), 1);
        model.fontZoom = prefs.getFontZoom();
        model.scrollPage = prefs.isScrollPage();
        model.colorPerLayer = prefs.getColorPerLayer();
        model.readonlyLayerColoringBehaviour = prefs.getReadonlyLayerColoringBehaviour();
        model.collapseArcs = prefs.isCollapseArcs();

        model.editor = editorChoices.stream()
                .filter(editor -> Objects.equals(editor.getKey(), prefs.getEditor())) //
                .findFirst().orElseGet(() -> {
                    AnnotationEditorFactory editorFactory = annotationEditorRegistry
                            .getDefaultEditorFactory();
                    return Pair.of(editorFactory.getBeanName(), editorFactory.getDisplayName());
                });

        model.annotationLayers = annotationService.listAnnotationLayer(getProject()).stream()
                // hide disabled Layers
                .filter(layer -> layer.isEnabled())
                // hide blocked layers
                .filter(layer -> !annotationEditorProperties.isLayerBlocked(layer))
                // chain layers cannot be curated
                .filter(layer -> !(CHAIN_TYPE.equals(layer.getType()) && CURATION == mode))
                .collect(Collectors.toList());

        return model;
    }

    private AnnotationPreference loadStoredPreferences()
    {
        try {
            return userPreferencesService.loadPreferences(getProject(),
                    userDao.getCurrentUsername(), mode);
        }
        catch (IOException e) {
            LOG.error("Unable to load annotation preferences", e);
            return new AnnotationPreference();
        }
    }

    private ListView<AnnotationLayer> createLayerContainer()
    {
        return new ListView<AnnotationLayer>("annotationLayers")
        {
            private static final long serialVersionUID = -4040731191748923013L;

            @Override
            protected void populateItem(ListItem<AnnotationLayer> aItem)
            {
                var prefs = form.getModelObject();
                var layer = aItem.getModelObject();

                // add coloring strategy choice
                var layerColor = new DropDownChoice<ColoringStrategyType>("layercoloring");
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
        private int fontZoom;
        private boolean scrollPage;
        private List<AnnotationLayer> annotationLayers;
        private ReadonlyColoringStrategy readonlyLayerColoringBehaviour;
        private Map<Long, ColoringStrategyType> colorPerLayer;
        private boolean collapseArcs;
    }
}
