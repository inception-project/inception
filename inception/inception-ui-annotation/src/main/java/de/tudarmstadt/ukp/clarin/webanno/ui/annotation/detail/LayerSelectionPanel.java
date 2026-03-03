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
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Collections.emptyList;
import static org.apache.wicket.event.Broadcast.BUBBLE;

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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.event.DefaultLayerChangedEvent;
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

        // Visible if there is more than one selectable layer and if the document is editable
        // (meaning we need to be able to change the layer)
        add(layerSelector = createDefaultAnnotationLayerSelector());
        layerSelector.add(visibleWhen(() -> layerSelector.getChoicesModel() //
                .map(layerChoices -> layerChoices.size() > 1) //
                .orElse(false).getObject()));
        add(relationHint = createRelationHint());
        relationHint.add(visibleWhen(() -> layerSelector.getChoicesModel() //
                .map(layerChoices -> layerChoices.size() > 1) //
                .orElse(false).getObject()));

        add(visibleWhen(() -> getEditorPage().isEditable()));

        allowedAnchoringModes = Model.ofList(emptyList());

        anchoringModePanel = new AnchoringModePanel("anchoringMode", null, allowedAnchoringModes) //
                .onApplied(this::actionApplyAnchoringMode);
        add(anchoringModePanel);
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

    public AnnotationPageBase getEditorPage()
    {
        return (AnnotationPageBase) getPage();
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
        selector.setChoices(getModel().map(AnnotatorState::getSelectableLayers));
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

        send(this, BUBBLE, new DefaultLayerChangedEvent(layerSelector.getModelObject()));

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
    }

    private void actionApplyAnchoringMode(AjaxRequestTarget Target, AnchoringMode aMode)
    {
        var state = getModelObject();
        var sessionOwner = userService.getCurrentUser();

        var anchoringPrefs = preferencesService.loadTraitsForUserAndProject(KEY_ANCHORING_MODE,
                sessionOwner, state.getProject());
        anchoringPrefs.setAnchoringModes(layerSelector.getModelObject(), aMode);
        preferencesService.saveTraitsForUserAndProject(KEY_ANCHORING_MODE, sessionOwner,
                state.getProject(), anchoringPrefs);
    }
}
