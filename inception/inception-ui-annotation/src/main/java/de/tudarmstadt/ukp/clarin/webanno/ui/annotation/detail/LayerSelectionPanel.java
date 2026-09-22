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

import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel.handleException;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnchoringModePrefs.KEY_ANCHORING_MODE;
import static de.tudarmstadt.ukp.inception.schema.api.layer.LayerTypes.CHAIN_LAYER_TYPE;
import static de.tudarmstadt.ukp.inception.schema.api.layer.LayerTypes.SPAN_LAYER_TYPE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Collections.emptyList;
import static org.apache.wicket.event.Broadcast.BREADTH;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.event.AnchoringModeChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.event.DefaultLayerChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;

public class LayerSelectionPanel
    extends GenericPanel<AnnotatorState>
{
    private static final long serialVersionUID = 7056096841332575514L;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserPreferencesService userPreferencesService;
    private @SpringBean UserDao userService;
    private @SpringBean AnnotationSchemaProperties annotationEditorProperties;
    private @SpringBean PreferencesService preferencesService;

    private final Label relationHint;
    private final DropDownChoice<AnnotationLayer> layerSelector;
    private final AnchoringModePanel anchoringModePanel;
    private final IModel<List<AnchoringMode>> allowedAnchoringModes;

    public LayerSelectionPanel(String aId, IModel<AnnotatorState> aModel)
    {
        super(aId, new CompoundPropertyModel<>(aModel));

        setOutputMarkupPlaceholderTag(true);

        add(layerSelector = createDefaultAnnotationLayerSelector());
        layerSelector.add(visibleWhen(this::isLayerChoiceAvailable));
        add(relationHint = createRelationHint());
        relationHint.add(visibleWhen(this::isLayerChoiceAvailable));

        add(enabledWhen(() -> findParent(AnnotationDetailEditorPanel.class).hasActiveEditor()));

        allowedAnchoringModes = Model.ofList(emptyList());

        anchoringModePanel = new AnchoringModePanel("anchoringMode", null, allowedAnchoringModes) //
                .onApplied(this::actionApplyAnchoringMode);
        add(anchoringModePanel);
    }

    private boolean isLayerChoiceAvailable()
    {
        return getSelectableLayers().size() > 1;
    }

    private List<AnnotationLayer> getSelectableLayers()
    {
        var layers = getModel().map(AnnotatorState::getSelectableLayers).getObject();
        if (layers != null && !layers.isEmpty()) {
            return layers;
        }

        var project = getModel().map(AnnotatorState::getProject).getObject();
        if (project == null) {
            return emptyList();
        }

        // Mirrors AnnotatorStateImpl.refreshSelectableLayers: enabled, writable, not blocked, and
        // only the types one can actually create by selecting a layer.
        return annotationService.listAnnotationLayer(project).stream() //
                .filter(AnnotationLayer::isEnabled) //
                .filter(layer -> !layer.isReadonly()) //
                .filter(layer -> !annotationEditorProperties.isLayerBlocked(layer)) //
                .filter(layer -> SPAN_LAYER_TYPE.equals(layer.getType())
                        || CHAIN_LAYER_TYPE.equals(layer.getType())) //
                .toList();
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        if (getModel().map(AnnotatorState::getDefaultAnnotationLayer).isPresent().getObject()) {
            allowedAnchoringModes.setObject(Stream.of(AnchoringMode.values()) //
                    .filter(getModel().getObject().getDefaultAnnotationLayer()
                            .getAnchoringMode()::allows) //
                    .toList());
        }
    }

    private Label createRelationHint()
    {
        var label = new Label("relationHint", Model.of());
        label.setOutputMarkupPlaceholderTag(true);
        label.add(LambdaBehavior.onConfigure(_this -> {
            if (layerSelector.getModelObject() != null) {
                var relLayers = annotationService
                        .listAttachedRelationLayers(layerSelector.getModelObject());
                if (relLayers.isEmpty()) {
                    _this.setVisible(false);
                }
                else if (relLayers.size() == 1) {
                    _this.setDefaultModelObject("Create a " + relLayers.get(0).getUiName()
                            + " relation by drawing an arc between annotations of this layer.");
                    _this.setVisible(true);
                }
                else {
                    _this.setVisible(false);
                }
            }
            else {
                _this.setVisible(false);
            }
        }));
        return label;
    }

    private DropDownChoice<AnnotationLayer> createDefaultAnnotationLayerSelector()
    {
        var selector = new DropDownChoice<AnnotationLayer>("defaultAnnotationLayer");
        selector.setChoices(this::getSelectableLayers);
        selector.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
        selector.setOutputMarkupId(true);
        selector.add(LambdaAjaxFormComponentUpdatingBehavior.onUpdate("change",
                this::actionChangeDefaultLayer));
        return selector;
    }

    private void actionChangeDefaultLayer(AjaxRequestTarget aTarget)
    {
        var sessionOwner = userService.getCurrentUser();
        var state = getModelObject();
        var currentDefaultLayer = state.getDefaultAnnotationLayer();

        aTarget.add(relationHint, anchoringModePanel);

        // Save the currently selected layer as a user preference so it is remains active when a
        // user leaves the application and later comes back to continue annotating
        var prevDefaultLayer = state.getPreferences().getDefaultLayer();
        if (currentDefaultLayer != null) {
            state.getPreferences().setDefaultLayer(state.getDefaultAnnotationLayer().getId());

            // Load the remembered anchoring mode preference or apply the default layer preference
            var anchoringPrefs = preferencesService.loadTraitsForUserAndProject(KEY_ANCHORING_MODE,
                    sessionOwner, state.getProject());
            state.syncAnchoringModeToDefaultLayer(anchoringPrefs);

            allowedAnchoringModes.setObject(Stream.of(AnchoringMode.values()) //
                    .filter(currentDefaultLayer.getAnchoringMode()::allows) //
                    .toList());
        }
        else {
            state.getPreferences().setDefaultLayer(-1);
        }

        if (prevDefaultLayer != state.getPreferences().getDefaultLayer()) {
            try {
                userPreferencesService.savePreferences(state.getProject(),
                        userService.getCurrentUsername(), state.getMode(), state.getPreferences());
            }
            catch (IOException e) {
                handleException(this, aTarget, e);
            }
        }

        // Sent LAST, once the preference and the anchoring mode have been brought in line - a
        // handler that reads either would otherwise see the value from before this change.
        send(getPage(), BREADTH, new DefaultLayerChangedEvent(layerSelector.getModelObject()));
    }

    private void actionApplyAnchoringMode(AjaxRequestTarget aTarget, AnchoringMode aMode)
    {
        var layer = layerSelector.getModelObject();
        if (layer == null) {
            return;
        }

        var project = getProjectForPreferences();
        if (project == null) {
            return;
        }

        var sessionOwner = userService.getCurrentUser();

        var anchoringPrefs = preferencesService.loadTraitsForUserAndProject(KEY_ANCHORING_MODE,
                sessionOwner, project);
        anchoringPrefs.setAnchoringModes(layer, aMode);
        preferencesService.saveTraitsForUserAndProject(KEY_ANCHORING_MODE, sessionOwner, project,
                anchoringPrefs);

        send(getPage(), BREADTH, new AnchoringModeChangedEvent(layer, aMode, anchoringPrefs));
    }

    private Project getProjectForPreferences()
    {
        return findParent(ProjectPageBase.class).getProject();
    }
}
