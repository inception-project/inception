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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.layer;

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Collections.emptySet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.bootstrap.IconToggleBox;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class LayerVisibilitySidebar
    extends AnnotationSidebar_ImplBase
{
    private static final FontAwesome5IconType ICON_HIDDEN = FontAwesome5IconType.eye_slash_r;
    private static final FontAwesome5IconType ICON_VISIBLE = FontAwesome5IconType.eye_r;
    private static final FontAwesome5IconType ICON_PARTIALLY_VISIBLE = FontAwesome5IconType.low_vision_s;

    private static final long serialVersionUID = 6127948490101336779L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AnnotationSchemaProperties annotationEditorProperties;
    private @SpringBean UserPreferencesService userPreferencesService;
    private @SpringBean UserDao userService;

    private Map<AnnotationLayer, Boolean> layerCollapseState = new HashMap<>();

    public LayerVisibilitySidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        add(createLayerContainer("layer", LoadableDetachableModel.of(this::listLayers)));
    }

    private List<AnnotationLayer> listLayers()
    {
        return annotationService.listSupportedLayers(getModelObject().getProject()).stream() //
                .filter(AnnotationLayer::isEnabled) //
                .filter(layer -> !annotationEditorProperties.isLayerBlocked(layer)) //
                .toList();
    }

    private void actionToggleLayerVisibility(AnnotationLayer aLayer, boolean aHidden,
            AjaxRequestTarget aTarget)
        throws IOException
    {
        getModelObject().getPreferences().setLayerVisible(aLayer, !aHidden);

        saveVisibilityStateAndRerender(aTarget);
    }

    private void actionToggleFeatureVisibility(AnnotationFeature aFeature, boolean aHidden,
            AjaxRequestTarget aTarget)
        throws IOException
    {
        if (aFeature.getTagset() == null) {
            getModelObject().getPreferences().setFeatureVisible(aFeature, !aHidden);
        }
        else {
            if (!aHidden) {
                getModelObject().getPreferences().setFeatureVisible(aFeature, true);
            }

            // If the feature has a tagset, we toggle the visibility of all tags in that tagset
            // instead of just the feature itself.
            var tags = listSelectableTags(aFeature);
            for (var tag : tags) {
                getModelObject().getPreferences().setTagVisible(aFeature, tag, !aHidden);
            }
            aTarget.add(this);
        }

        saveVisibilityStateAndRerender(aTarget);
    }

    private void actionToggleTagVisibility(AnnotationFeature aFeature, Tag aTag, boolean aHidden,
            AjaxRequestTarget aTarget)
        throws IOException
    {
        getModelObject().getPreferences().setTagVisible(aFeature, aTag, !aHidden);

        saveVisibilityStateAndRerender(aTarget);
        aTarget.add(this);
    }

    private void actionFocusTag(AjaxRequestTarget aTarget, AnnotationFeature aFeature, Tag aTag)
        throws IOException
    {
        var preferences = getModelObject().getPreferences();

        var tags = listSelectableTags(aFeature);
        for (var tag : tags) {
            var showTag = tag.getName().equals(aTag.getName());
            preferences.setTagVisible(aFeature, tag, showTag);
        }

        saveVisibilityStateAndRerender(aTarget);
        aTarget.add(this);
    }

    private void actionCollapseLayer(AjaxRequestTarget aTarget, AnnotationLayer aLayer)
    {
        var oldState = layerCollapseState.getOrDefault(aLayer, false);
        layerCollapseState.put(aLayer, !oldState);
        aTarget.add(this);
    }

    private void saveVisibilityStateAndRerender(AjaxRequestTarget aTarget) throws IOException
    {
        var sessionOwner = userService.getCurrentUsername();
        userPreferencesService.savePreferences(getModelObject(), sessionOwner);
        userPreferencesService.loadPreferences(getModelObject(), sessionOwner);

        getAnnotationPage().actionRefreshDocument(aTarget);
    }

    private List<Tag> listSelectableTags(AnnotationFeature aFeature)
    {
        var tags = new ArrayList<Tag>();
        tags.add(new Tag("", ""));
        annotationService.listTags(aFeature.getTagset()).stream() //
                .forEach(tags::add);
        return tags;
    }

    private ListView<AnnotationLayer> createLayerContainer(String aId,
            IModel<List<AnnotationLayer>> aLayers)
    {
        return new ListView<AnnotationLayer>(aId, aLayers)
        {
            private static final long serialVersionUID = -1L;

            @Override
            protected void populateItem(ListItem<AnnotationLayer> aItem)
            {
                var prefs = LayerVisibilitySidebar.this.getModelObject().getPreferences();
                var layer = aItem.getModelObject();
                var hiddenLayerIds = prefs.getHiddenAnnotationLayerIds();

                var layerVisible = new IconToggleBox("visibleLayerToggle") //
                        .setCheckedIcon(ICON_VISIBLE) //
                        .setCheckedTitle(Model.of("Visible")) //
                        .setUncheckedIcon(ICON_HIDDEN) //
                        .setUncheckedTitle(Model.of("Hidden")) //
                        .setModel(Model.of(!hiddenLayerIds.contains(layer.getId())));
                layerVisible.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        _target -> actionToggleLayerVisibility(layer, layerVisible.getModelObject(),
                                _target)));
                aItem.add(layerVisible);

                aItem.add(new Label("layerName", layer.getUiName()));

                var features = annotationService.listEnabledFeatures(layer).stream() //
                        .filter(AnnotationFeature::isVisible) //
                        .toList();

                var collapseLayer = new LambdaAjaxLink("collapseLayer",
                        _target -> actionCollapseLayer(_target, layer));
                collapseLayer.add(new AttributeAppender("class",
                        () -> layerCollapseState.getOrDefault(layer, false) ? "group-collapsed"
                                : "",
                        " "));
                collapseLayer.add(visibleWhen(() -> !features.isEmpty()));
                aItem.add(collapseLayer);

                if (layerCollapseState.getOrDefault(layer, false)) {
                    aItem.add(new EmptyPanel("feature").setVisible(false));
                }
                else {
                    aItem.add(createFeatureContainer("feature", new ListModel<>(features)));
                }
            }
        };
    }

    protected ListView<AnnotationFeature> createFeatureContainer(String aId,
            ListModel<AnnotationFeature> aFeatures)
    {
        return new ListView<AnnotationFeature>(aId, aFeatures)
        {
            private static final long serialVersionUID = -1L;

            @Override
            protected void populateItem(ListItem<AnnotationFeature> aItem)
            {
                var prefs = LayerVisibilitySidebar.this.getModelObject().getPreferences();
                var feature = aItem.getModelObject();
                var hiddenFeatureIds = prefs.getHiddenAnnotationFeatureIds();

                var hiddenIcon = ICON_HIDDEN;
                boolean visibilityState;
                if (feature.getTagset() == null) {
                    visibilityState = !hiddenFeatureIds.contains(feature.getId());
                }
                else {
                    var hiddenTags = prefs.getHiddenTags().getOrDefault(feature.getId(),
                            emptySet());
                    visibilityState = hiddenTags.isEmpty();
                    if (!visibilityState) {
                        var selectableTags = listSelectableTags(feature).stream().map(Tag::getName)
                                .toList();
                        if (!hiddenTags.containsAll(selectableTags)) {
                            hiddenIcon = ICON_PARTIALLY_VISIBLE;
                        }
                    }
                }

                var featureVisible = new IconToggleBox("visibleFeatureToggle") //
                        .setCheckedIcon(ICON_VISIBLE) //
                        .setCheckedTitle(Model.of("Visible")) //
                        .setUncheckedIcon(hiddenIcon) //
                        .setUncheckedTitle(Model.of("Hidden")) //
                        .setModel(Model.of(visibilityState));
                featureVisible.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        _target -> actionToggleFeatureVisibility(feature,
                                featureVisible.getModelObject(), _target)));
                aItem.add(featureVisible);

                aItem.add(new Label("featureName", feature.getUiName()));

                if (feature.getTagset() == null) {
                    aItem.add(new EmptyPanel("tag").setVisible(false));
                }
                else {
                    var tags = listSelectableTags(feature).stream() //
                            .map(tag -> Pair.of(feature, tag)) //
                            .toList();

                    aItem.add(createTagContainer("tag", new ListModel<>(tags)));
                }
            }
        };
    }

    protected Component createTagContainer(String aId,
            ListModel<Pair<AnnotationFeature, Tag>> aTags)
    {
        return new ListView<Pair<AnnotationFeature, Tag>>(aId, aTags)
        {
            private static final long serialVersionUID = -1L;

            @Override
            protected void populateItem(ListItem<Pair<AnnotationFeature, Tag>> aItem)
            {
                var prefs = LayerVisibilitySidebar.this.getModelObject().getPreferences();
                var feature = aItem.getModelObject().getKey();
                var tag = aItem.getModelObject().getValue();
                var hiddenTags = prefs.getHiddenTags();

                var featureVisible = new IconToggleBox("visibleTagToggle") //
                        .setCheckedIcon(ICON_VISIBLE) //
                        .setCheckedTitle(Model.of("Visible")) //
                        .setUncheckedIcon(ICON_HIDDEN) //
                        .setUncheckedTitle(Model.of("Hidden")) //
                        .setModel(Model.of(!hiddenTags.getOrDefault(feature.getId(), emptySet())
                                .contains(tag.getName())));
                featureVisible.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        _target -> actionToggleTagVisibility(feature, tag,
                                featureVisible.getModelObject(), _target)));
                aItem.add(featureVisible);

                var focusTag = new LambdaAjaxLink("focusTag",
                        _target -> actionFocusTag(_target, feature, tag));
                aItem.add(focusTag);

                var tagName = tag.getName().isBlank() ? "<None>" : tag.getName();
                aItem.add(new Label("tagName", tagName));
            }
        };
    }
}
