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
package de.tudarmstadt.ukp.inception.annotation.layer.document.sidebar;

import static de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport.FEATURE_NAME_ORDER;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.EXTENSION_ID;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.MAIN_EDITOR;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CLICK_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.wicket.event.Broadcast.BREADTH;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.event.DocumentMetadataEvent;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.event.AjaxRecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.model.MetadataSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.TypeUtil;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxEventBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

public class DocumentMetadataAnnotationSelectionPanel
    extends GenericPanel<AnnotatorState>
{
    private static final long serialVersionUID = 8318858582025740458L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CID_LABEL = "label";
    private static final String CID_SCORE = "score";
    private static final String CID_LAYER = "layer";
    private static final String CID_LAYERS = "layers";
    private static final String CID_LAYERS_CONTAINER = "layersContainer";
    private static final String CID_ANNOTATION = "annotation";
    private static final String CID_ANNOTATIONS = "annotations";
    private static final String CID_ANNOTATIONS_CONTAINER = "annotationsContainer";
    private static final String CID_ANNOTATION_DETAILS = "annotationDetails";
    private static final String CID_NO_LAYERS_WARNING = "noLayersWarning";
    private static final String CID_CREATE = "create";
    private static final String CID_DELETE = "delete";
    private static final String CID_ACCEPT = "accept";
    private static final String CID_REJECT = "reject";
    private static final String CID_OPEN = "open";
    private static final String CID_CLOSE = "close";

    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean FeatureSupportRegistry fsRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean RecommendationService recommendationService;
    private @SpringBean UserDao userService;

    private final AnnotationPageBase2 annotationPage;
    private final CasProvider casProvider;
    private final AnnotationActionHandler actionHandler;
    private final WebMarkupContainer layersContainer;

    private final IModel<AnnotationLayer> selectedLayer;
    private final IModel<List<LayerGroup>> layers;

    private WebMarkupContainer selectedAnnotation;
    private DocumentMetadataAnnotationDetailPanel selectedDetailPanel;
    private int createdAnnotationAddress;

    public DocumentMetadataAnnotationSelectionPanel(String aId, CasProvider aCasProvider,
            AnnotationPageBase2 aAnnotationPage, AnnotationActionHandler aActionHandler)
    {
        super(aId, aAnnotationPage.getModel());

        setOutputMarkupPlaceholderTag(true);

        annotationPage = aAnnotationPage;
        casProvider = aCasProvider;
        actionHandler = aActionHandler;

        selectedLayer = Model.of(listCreatableMetadataLayers().stream().findFirst().orElse(null));
        layers = LoadableDetachableModel.of(this::listLayers);
        var availableLayers = LoadableDetachableModel.of(this::listCreatableMetadataLayers);

        var content = new WebMarkupContainer("content");
        content.add(visibleWhenNot(layers.map(List::isEmpty).orElse(true)));
        add(content);

        var layer = new DropDownChoice<AnnotationLayer>(CID_LAYER);
        layer.setModel(selectedLayer);
        layer.setChoices(availableLayers);
        layer.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
        layer.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT));
        layer.add(visibleWhenNot(availableLayers.map(List::isEmpty).orElse(true)));
        content.add(layer);

        content.add(new LambdaAjaxLink(CID_CREATE, this::actionCreate)
                .add(enabledWhen(() -> annotationPage.isEditable())) //
                .add(visibleWhenNot(availableLayers.map(List::isEmpty).orElse(true))));

        layersContainer = new WebMarkupContainer(CID_LAYERS_CONTAINER);
        layersContainer.setOutputMarkupPlaceholderTag(true);
        layersContainer.add(createLayerGroupList());
        content.add(layersContainer);

        add(new WebMarkupContainer(CID_NO_LAYERS_WARNING)
                .add(visibleWhen(layers.map(List::isEmpty).orElse(true))));
    }

    private void actionAccept(AjaxRequestTarget aTarget, AnnotationListItem aItem)
    {
        try {
            annotationPage.ensureIsEditable();

            var page = (AnnotationPage) aTarget.getPage();
            var state = getModelObject();
            var dataOwner = state.getUser().getUsername();
            var sessionOwner = userService.getCurrentUser();
            var predictions = recommendationService.getPredictions(sessionOwner,
                    state.getProject());
            var suggestion = predictions.getPredictionByVID(state.getDocument(), aItem.vid).get();

            var aCas = casProvider.get();

            recommendationService.acceptSuggestion(sessionOwner.getUsername(), state.getDocument(),
                    dataOwner, aCas, predictions, suggestion, MAIN_EDITOR);

            page.writeEditorCas(aCas);

            page.send(page, BREADTH,
                    new AjaxRecommendationAcceptedEvent(aTarget, state, aItem.vid));
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }
    }

    private void actionReject(AjaxRequestTarget aTarget, AnnotationListItem aItem)
    {
        try {
            annotationPage.ensureIsEditable();

            var page = (AnnotationPage) aTarget.getPage();
            var state = getModelObject();
            var dataOwner = state.getUser().getUsername();
            var sessionOwner = userService.getCurrentUser();
            var suggestion = recommendationService.getPredictions(sessionOwner, state.getProject())
                    .getPredictionByVID(state.getDocument(), aItem.vid).get();

            var aCas = casProvider.get();

            recommendationService.rejectSuggestion(sessionOwner.getUsername(), state.getDocument(),
                    dataOwner, suggestion, MAIN_EDITOR);

            page.writeEditorCas(aCas);

            page.send(page, BREADTH,
                    new AjaxRecommendationRejectedEvent(aTarget, state, aItem.vid));
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }
    }

    private void actionCreate(AjaxRequestTarget aTarget) throws AnnotationException, IOException
    {
        try {
            annotationPage.ensureIsEditable();

            var state = getModelObject();
            var adapter = (DocumentMetadataLayerAdapter) annotationService
                    .getAdapter(selectedLayer.getObject());
            var cas = casProvider.get();
            var fs = adapter.add(state.getDocument(), state.getUser().getUsername(), cas);

            createdAnnotationAddress = fs.getAddress();
            annotationPage.writeEditorCas(cas);

            aTarget.add(layersContainer);

            annotationPage.actionRefreshDocument(aTarget);
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }
    }

    private void actionDelete(AjaxRequestTarget aTarget,
            DocumentMetadataAnnotationDetailPanel aDetailPanel)
    {
        try {
            annotationPage.ensureIsEditable();

            // Load the boiler-plate
            var state = getModelObject();
            var cas = casProvider.get();
            var fs = ICasUtil.selectFsByAddr(cas, aDetailPanel.getModelObject().getId());
            var adapter = annotationService.findAdapter(state.getProject(), fs);

            // Perform actual actions
            adapter.delete(state.getDocument(), state.getUser().getUsername(), cas, VID.of(fs));

            // persist changes
            annotationPage.writeEditorCas(cas);

            if (selectedDetailPanel == aDetailPanel) {
                selectedAnnotation = null;
                selectedDetailPanel = null;
            }

            remove(aDetailPanel);

            annotationPage.actionRefreshDocument(aTarget);
            aTarget.add(layersContainer);
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }
    }

    private void actionSelect(AjaxRequestTarget aTarget, WebMarkupContainer container,
            DocumentMetadataAnnotationDetailPanel detailPanel)
    {
        if (selectedAnnotation != null) {
            aTarget.add(selectedAnnotation);
        }

        if (selectedDetailPanel != null) {
            aTarget.add(selectedDetailPanel);
        }

        if (selectedAnnotation == container) {
            // if container is already selected, deselect and close annotation
            selectedAnnotation = null;
        }
        else {
            selectedAnnotation = container;
        }

        if (selectedDetailPanel == detailPanel) {
            // if detail panel is already selected, deselect and close annotation
            selectedDetailPanel = null;
        }
        else {
            selectedDetailPanel = detailPanel;
        }

        aTarget.add(container);
        aTarget.add(detailPanel);
    }

    private ListView<LayerGroup> createLayerGroupList()
    {
        return new ListView<LayerGroup>(CID_LAYERS, layers)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<LayerGroup> aItem)
            {
                var annotations = LoadableDetachableModel
                        .of(() -> listAnnotations(aItem.getModelObject().layer));

                aItem.add(new Label("layerName", aItem.getModelObject().layer.getUiName()));

                var container = new WebMarkupContainer(CID_ANNOTATIONS_CONTAINER);
                container.setOutputMarkupPlaceholderTag(true);
                container.add(createAnnotationList(annotations));
                aItem.add(container);

            }
        };
    }

    private ListView<AnnotationListItem> createAnnotationList(
            IModel<List<AnnotationListItem>> aAnnotations)
    {
        return new ListView<AnnotationListItem>(CID_ANNOTATIONS, aAnnotations)
        {
            private static final long serialVersionUID = -6833373063896777785L;

            @Override
            protected void populateItem(ListItem<AnnotationListItem> aItem)
            {
                aItem.setModel(CompoundPropertyModel.of(aItem.getModel()));

                var vid = aItem.getModelObject().vid;

                var container = new WebMarkupContainer(CID_ANNOTATION);
                container.add(visibleWhen(() -> !aItem.getModelObject().singleton));
                aItem.add(container);

                var detailPanel = new DocumentMetadataAnnotationDetailPanel(CID_ANNOTATION_DETAILS,
                        Model.of(vid), casProvider, annotationPage, actionHandler,
                        DocumentMetadataAnnotationSelectionPanel.this.getModel());
                aItem.add(detailPanel);

                var isSuggestion = EXTENSION_ID.equals(aItem.getModelObject().vid.getExtensionId());

                if (!isSuggestion) {
                    container.add(new LambdaAjaxEventBehavior(CLICK_EVENT,
                            $ -> actionSelect($, container, detailPanel)));
                }

                detailPanel.add(visibleWhen(() -> isExpanded(aItem, container)));

                if (createdAnnotationAddress == vid.getId()) {
                    createdAnnotationAddress = -1;
                    selectedAnnotation = container;
                    selectedDetailPanel = detailPanel;
                }

                var close = new WebMarkupContainer(CID_CLOSE);
                close.add(visibleWhen(() -> isExpanded(aItem, container) && !isSuggestion));
                close.setOutputMarkupId(true);
                container.add(close);

                var open = new WebMarkupContainer(CID_OPEN);
                open.add(visibleWhen(() -> !isExpanded(aItem, container) && !isSuggestion));
                open.setOutputMarkupId(true);
                container.add(open);

                container.add(new Label(CID_LABEL,
                        StringUtils.defaultIfEmpty(aItem.getModelObject().label, "[No label]"))
                                .add(visibleWhen(() -> !aItem.getModelObject().singleton
                                        && !isExpanded(aItem, container))));

                aItem.queue(new LambdaAjaxLink(CID_DELETE, $ -> actionDelete($, detailPanel))
                        .add(AttributeModifier.replace("title", aItem.getModelObject().vid))
                        .add(visibleWhen(() -> !aItem.getModelObject().singleton && !isSuggestion))
                        .add(enabledWhen(() -> annotationPage.isEditable()
                                && !aItem.getModelObject().layer.isReadonly())));

                aItem.queue(new Label(CID_SCORE, String.format(Session.get().getLocale(), "%.2f",
                        aItem.getModelObject().score)).add(visibleWhen(
                                () -> isSuggestion && aItem.getModelObject().score != 0.0d)));

                aItem.queue(
                        new LambdaAjaxLink(CID_ACCEPT, $ -> actionAccept($, aItem.getModelObject()))
                                .add(AttributeModifier.replace("title", aItem.getModelObject().vid))
                                .add(visibleWhen(() -> isSuggestion && annotationPage.isEditable()
                                        && !aItem.getModelObject().layer.isReadonly())));

                aItem.queue(
                        new LambdaAjaxLink(CID_REJECT, $ -> actionReject($, aItem.getModelObject()))
                                .add(AttributeModifier.replace("title", aItem.getModelObject().vid))
                                .add(visibleWhen(() -> isSuggestion && annotationPage.isEditable()
                                        && !aItem.getModelObject().layer.isReadonly())));

                aItem.setOutputMarkupId(true);
            }

            private boolean isExpanded(ListItem<AnnotationListItem> aItem,
                    WebMarkupContainer container)
            {
                return selectedAnnotation == container || aItem.getModelObject().singleton;
            }
        };
    }

    private List<AnnotationLayer> listMetadataLayers()
    {
        return annotationService.listAnnotationLayer(getModelObject().getProject()).stream()
                .filter(layer -> DocumentMetadataLayerSupport.TYPE.equals(layer.getType())
                        && layer.isEnabled()) //
                .toList();
    }

    private List<AnnotationLayer> listCreatableMetadataLayers()
    {
        return listMetadataLayers().stream() //
                .filter(layer -> !layer.isReadonly()) //
                .filter(layer -> !getLayerSupport(layer).readTraits(layer).isSingleton()) //
                .toList();
    }

    private DocumentMetadataLayerSupport getLayerSupport(AnnotationLayer aLayer)
    {
        return (DocumentMetadataLayerSupport) layerSupportRegistry.getLayerSupport(aLayer);
    }

    private List<LayerGroup> listLayers()
    {
        return listMetadataLayers().stream().map(LayerGroup::new).toList();
    }

    private List<AnnotationListItem> listAnnotations(AnnotationLayer aLayer)
    {
        CAS cas;
        try {
            cas = casProvider.get();
        }
        catch (IOException e) {
            LOG.error("Unable to load CAS", e);
            return emptyList();
        }

        // Bulk-load all the features of this layer to avoid having to do repeated DB accesses
        // later
        var features = annotationService.listSupportedFeatures(aLayer);
        var layerSupport = getLayerSupport(aLayer);
        var singleton = layerSupport.readTraits(aLayer).isSingleton();

        var items = new ArrayList<AnnotationListItem>();
        generateAnnotationItems(aLayer, layerSupport, singleton, cas, items, features);
        generateSuggestionItems(aLayer, layerSupport, singleton, cas, items, features);
        return items;
    }

    private void generateAnnotationItems(AnnotationLayer aLayer,
            DocumentMetadataLayerSupport aLayerSupport, boolean aSingleton, CAS aCas,
            List<AnnotationListItem> aItems, List<AnnotationFeature> aFeatures)
    {
        var adapter = annotationService.getAdapter(aLayer);
        var renderer = aLayerSupport.createRenderer(aLayer,
                () -> annotationService.listAnnotationFeature(aLayer));

        var maybeType = adapter.getAnnotationType(aCas);
        if (!maybeType.isPresent()) {
            return;
        }

        var type = maybeType.get();
        var annotations = aCas.select(type).asList();
        var hasOrderFeature = type.getFeatureByBaseName(FEATURE_NAME_ORDER) != null;
        if (hasOrderFeature) {
            annotations.sort(comparing(fs -> getFeature(fs, FEATURE_NAME_ORDER, Integer.class)));
        }

        for (var fs : annotations) {
            var renderedFeatures = renderer.renderLabelFeatureValues(adapter, fs, aFeatures);
            var labelText = TypeUtil.getUiLabelText(renderedFeatures);
            var order = hasOrderFeature ? getFeature(fs, FEATURE_NAME_ORDER, Integer.class) : 0;
            aItems.add(
                    new AnnotationListItem(VID.of(fs), labelText, aLayer, aSingleton, 0.0d, order));
        }
    }

    private void generateSuggestionItems(AnnotationLayer aLayer,
            DocumentMetadataLayerSupport aLayerSupport, boolean aSingleton, CAS aCas,
            List<AnnotationListItem> aItems, List<AnnotationFeature> aFeatures)
    {
        var state = getModelObject();
        var featuresIndex = aFeatures.stream()
                .collect(toMap(AnnotationFeature::getName, identity()));
        var predictions = recommendationService.getPredictions(state.getUser(), state.getProject());
        if (predictions != null) {
            var predictionsByDocument = predictions
                    .getPredictionsByDocument(state.getDocument().getId());

            var group = SuggestionDocumentGroup.groupsOfType(MetadataSuggestion.class,
                    predictionsByDocument);

            recommendationService.calculateSuggestionVisibility(userService.getCurrentUsername(),
                    state.getDocument(), aCas, state.getUser().getUsername(), aLayer, group, -1,
                    -1);

            var pref = recommendationService.getPreferences(state.getUser(), state.getProject());

            for (var suggestion : predictionsByDocument) {
                if ((!suggestion.isVisible() && !pref.isShowAllPredictions())
                        || !Objects.equals(suggestion.getLayerId(), aLayer.getId())) {
                    continue;
                }

                if (suggestion instanceof MetadataSuggestion metadataSuggestion) {
                    var feature = featuresIndex.get(suggestion.getFeature());

                    // Retrieve the UI display label for the given feature value
                    var featureSupport = fsRegistry.findExtension(feature).orElseThrow();
                    var annotation = featureSupport.renderFeatureValue(feature,
                            suggestion.getLabel());
                    aItems.add(new AnnotationListItem(suggestion.getVID(), annotation, aLayer,
                            false, suggestion.getScore(), aItems.size()));
                }
            }
        }
    }

    @OnEvent
    public void onDocumentMetadataEvent(DocumentMetadataEvent aEvent)
    {
        aEvent.getRequestTarget().ifPresent(target -> target.add(layersContainer));
        annotationPage.actionRefreshDocument(aEvent.getRequestTarget().orElse(null));
    }

    @OnEvent
    public void onFeatureValueUpdated(FeatureValueUpdatedEvent aEvent)
    {
        var vid = VID.of(aEvent.getFS());
        layersContainer.visitChildren(DocumentMetadataAnnotationDetailPanel.class, (c, v) -> {
            var detailPanel = (DocumentMetadataAnnotationDetailPanel) c;
            if (detailPanel.getModelObject().getId() == vid.getId()) {
                aEvent.getRequestTarget()
                        .ifPresent(target -> target.add(detailPanel.findParent(ListItem.class)));
            }
        });

        annotationPage.actionRefreshDocument((aEvent.getRequestTarget().orElse(null)));
    }

    protected static void handleException(Component aComponent, AjaxRequestTarget aTarget,
            Exception aException)
    {
        if (aTarget != null) {
            aTarget.addChildren(aComponent.getPage(), IFeedback.class);
        }

        try {
            throw aException;
        }
        catch (AnnotationException e) {
            aComponent.error("Error: " + e.getMessage());
            LOG.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (UIMAException e) {
            aComponent.error("Error: " + ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (Exception e) {
            aComponent.error("Error: " + e.getMessage());
            LOG.error("Error: " + e.getMessage(), e);
        }
    }

    private record LayerGroup(AnnotationLayer layer)
        implements Serializable
    {}

    private record AnnotationListItem(VID vid, String label, AnnotationLayer layer,
            boolean singleton, double score, int order)
        implements Serializable
    {}
}
