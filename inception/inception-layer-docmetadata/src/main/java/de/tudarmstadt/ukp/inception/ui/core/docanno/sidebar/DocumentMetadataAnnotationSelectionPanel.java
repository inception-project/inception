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
package de.tudarmstadt.ukp.inception.ui.core.docanno.sidebar;

import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.EXTENSION_ID;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.MAIN_EDITOR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.ACCEPTED;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CLICK;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
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
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.model.MetadataSuggestion;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.TypeUtil;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxEventBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.ui.core.docanno.event.DocumentMetadataEvent;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;

public class DocumentMetadataAnnotationSelectionPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 8318858582025740458L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CID_LABEL = "label";
    private static final String CID_SCORE = "score";
    private static final String CID_LAYERS = "layers";
    private static final String CID_ANNOTATIONS = "annotations";
    private static final String CID_LAYER = "layer";
    private static final String CID_CREATE = "create";
    private static final String CID_ANNOTATIONS_CONTAINER = "annotationsContainer";
    private static final String CID_ANNOTATION_DETAILS = "annotationDetails";
    private static final String CID_DELETE = "delete";
    private static final String CID_ACCEPT = "accept";
    private static final String CID_REJECT = "reject";

    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean FeatureSupportRegistry fsRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean RecommendationService recommendationService;
    private @SpringBean LearningRecordService learningRecordService;
    private @SpringBean UserDao userService;

    private final AnnotationPage annotationPage;
    private final CasProvider jcasProvider;
    private final IModel<SourceDocument> sourceDocument;
    private final IModel<User> user;
    private final IModel<AnnotationLayer> selectedLayer;
    private final AnnotationActionHandler actionHandler;
    private WebMarkupContainer selectedAnnotation;
    private WebMarkupContainer layersContainer;
    private DocumentMetadataAnnotationDetailPanel selectedDetailPanel;
    private int createdAnnotationAddress;
    private final IModel<AnnotatorState> state;

    private final IModel<List<LayerGroup>> layers;

    public DocumentMetadataAnnotationSelectionPanel(String aId, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage, AnnotationActionHandler aActionHandler,
            IModel<AnnotatorState> aState)
    {
        super(aId, aState.map(AnnotatorState::getProject));

        setOutputMarkupPlaceholderTag(true);

        state = aState;

        sourceDocument = aState.map(AnnotatorState::getDocument);
        user = aState.map(AnnotatorState::getUser);

        annotationPage = aAnnotationPage;
        jcasProvider = aCasProvider;
        selectedLayer = Model.of(listCreatableMetadataLayers().stream().findFirst().orElse(null));
        var availableLayers = LoadableDetachableModel.of(this::listCreatableMetadataLayers);
        actionHandler = aActionHandler;

        layers = LoadableDetachableModel.of(this::listLayers);

        var content = new WebMarkupContainer("content");
        add(content);

        var layer = new DropDownChoice<AnnotationLayer>(CID_LAYER);
        layer.setModel(selectedLayer);
        layer.setChoices(availableLayers);
        layer.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
        layer.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        layer.add(visibleWhen(() -> !availableLayers.map(List::isEmpty).orElse(true).getObject()));
        content.add(layer);

        content.add(new LambdaAjaxLink(CID_CREATE, this::actionCreate)
                .add(enabledWhen(() -> annotationPage.isEditable())) //
                .add(visibleWhenNot(availableLayers.map(List::isEmpty).orElse(true))));

        layersContainer = new WebMarkupContainer("layersContainer");
        layersContainer.setOutputMarkupPlaceholderTag(true);
        layersContainer.add(createLayerGroupList());
        layersContainer.add(visibleWhenNot(availableLayers.map(List::isEmpty).orElse(true)));
        content.add(layersContainer);

        var noLayersWarning = new WebMarkupContainer("noLayersWarning");
        noLayersWarning.add(visibleWhen(availableLayers.map(List::isEmpty).orElse(true)));
        add(noLayersWarning);
    }

    private void actionAccept(AjaxRequestTarget aTarget, AnnotationListItem aItem)
    {
        try {
            var page = (AnnotationPage) aTarget.getPage();
            var dataOwner = state.getObject().getUser().getUsername();
            var sessionOwner = userService.getCurrentUser();
            var suggestion = (MetadataSuggestion) recommendationService
                    .getPredictions(sessionOwner, state.getObject().getProject())
                    .getPredictionByVID(state.getObject().getDocument(), aItem.vid).get();
            var layer = annotationService.getLayer(suggestion.getLayerId());
            var feature = annotationService.getFeature(suggestion.getFeature(), layer);
            var adapter = (DocumentMetadataLayerAdapter) annotationService.getAdapter(layer);

            var aCas = jcasProvider.get();

            var annotation = new DocumentMetadataRecommendationSupport(recommendationService)
                    .acceptSuggestion(sessionOwner.getUsername(), state.getObject().getDocument(),
                            dataOwner, aCas, adapter, feature, suggestion, MAIN_EDITOR, ACCEPTED);

            page.writeEditorCas(aCas);

            // Set selection to the accepted annotation and select it and load it into the detail
            // editor
            // state.getObject().getSelection().set(adapter.select(VID.of(annotation), annotation));

            // Send a UI event that the suggestion has been accepted
            page.send(page, BREADTH,
                    new AjaxRecommendationAcceptedEvent(aTarget, state.getObject(), aItem.vid));
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }
    }

    private void actionReject(AjaxRequestTarget a$, AnnotationListItem aAnnotationListItem)
    {
    }

    private void actionCreate(AjaxRequestTarget aTarget) throws AnnotationException, IOException
    {
        try {
            annotationPage.ensureIsEditable();

            var adapter = (DocumentMetadataLayerAdapter) annotationService
                    .getAdapter(selectedLayer.getObject());
            var cas = jcasProvider.get();
            var fs = adapter.add(sourceDocument.getObject(), user.getObject().getUsername(), cas);

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
            var project = getModelObject();
            var cas = jcasProvider.get();
            var fs = ICasUtil.selectFsByAddr(cas, aDetailPanel.getModelObject().getId());
            var layer = annotationService.findLayer(project, fs);
            var adapter = annotationService.getAdapter(layer);

            // Perform actual actions
            adapter.delete(sourceDocument.getObject(), user.getObject().getUsername(), cas,
                    VID.of(fs));

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
        var availableLayers = listCreatableMetadataLayers();

        return new ListView<LayerGroup>(CID_LAYERS, layers)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<LayerGroup> aItem)
            {
                var annotations = LoadableDetachableModel
                        .of(() -> listAnnotations(aItem.getModelObject().layer));

                aItem.add(new Label("layerName", aItem.getModelObject().layer.getUiName()));

                var annotationsContainer = new WebMarkupContainer(CID_ANNOTATIONS_CONTAINER);
                annotationsContainer.setOutputMarkupPlaceholderTag(true);
                annotationsContainer.add(createAnnotationList(annotations));
                annotationsContainer.add(visibleWhen(() -> !availableLayers.isEmpty()));
                aItem.add(annotationsContainer);

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

                var container = new WebMarkupContainer("annotation");
                aItem.add(container);

                var detailPanel = new DocumentMetadataAnnotationDetailPanel(CID_ANNOTATION_DETAILS,
                        Model.of(vid), jcasProvider, annotationPage, actionHandler, state);
                aItem.add(detailPanel);

                var isSuggestion = EXTENSION_ID.equals(aItem.getModelObject().vid.getExtensionId());

                if (!isSuggestion) {
                    container.add(new LambdaAjaxEventBehavior(CLICK,
                            $ -> actionSelect($, container, detailPanel)));
                }

                detailPanel.add(visibleWhen(() -> isExpanded(aItem, container)));

                if (createdAnnotationAddress == vid.getId()) {
                    createdAnnotationAddress = -1;
                    selectedAnnotation = container;
                    selectedDetailPanel = detailPanel;
                }

                var close = new WebMarkupContainer("close");
                close.add(visibleWhen(() -> isExpanded(aItem, container) && !isSuggestion));
                close.setOutputMarkupId(true);
                container.add(close);

                var open = new WebMarkupContainer("open");
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
        return annotationService.listAnnotationLayer(getModelObject()).stream()
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
            cas = jcasProvider.get();
        }
        catch (IOException e) {
            LOG.error("Unable to load CAS", e);
            return emptyList();
        }

        var items = new ArrayList<AnnotationListItem>();

        // --- Populate with annotations ---

        // Bulk-load all the features of this layer to avoid having to do repeated DB accesses
        // later
        var features = annotationService.listSupportedFeatures(aLayer);
        var featuresIndex = features.stream()
                .collect(toMap(AnnotationFeature::getName, identity()));
        var adapter = annotationService.getAdapter(aLayer);
        LayerSupport<?, ?> layerSupport = layerSupportRegistry.getLayerSupport(aLayer);
        var renderer = layerSupport.createRenderer(aLayer,
                () -> annotationService.listAnnotationFeature(aLayer));
        var singleton = getLayerSupport(aLayer).readTraits(aLayer).isSingleton();

        for (var fs : cas.select(adapter.getAnnotationType(cas))) {
            var renderedFeatures = renderer.renderLabelFeatureValues(adapter, fs, features);
            var labelText = TypeUtil.getUiLabelText(renderedFeatures);
            items.add(new AnnotationListItem(VID.of(fs), labelText, aLayer, singleton, 0.0d));
        }

        // --- Populate with predictions ---
        var predictions = recommendationService.getPredictions(user.getObject(), getModelObject());
        if (predictions != null) {
            for (var suggestion : predictions
                    .getPredictionsByDocument(sourceDocument.getObject().getName())) {
                if (suggestion instanceof MetadataSuggestion metadataSuggestion
                        && Objects.equals(suggestion.getLayerId(), aLayer.getId())) {
                    var feature = featuresIndex.get(suggestion.getFeature());

                    // Retrieve the UI display label for the given feature value
                    var featureSupport = fsRegistry.findExtension(feature).orElseThrow();
                    var annotation = featureSupport.renderFeatureValue(feature,
                            suggestion.getLabel());
                    items.add(new AnnotationListItem(suggestion.getVID(), annotation, aLayer,
                            singleton, suggestion.getScore()));
                }
            }
        }

        return items;
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

    private class LayerGroup
        implements Serializable
    {
        private static final long serialVersionUID = -1798740501968780059L;

        final AnnotationLayer layer;

        public LayerGroup(AnnotationLayer aLayer)
        {
            layer = aLayer;
        }
    }

    private class AnnotationListItem
        implements Serializable
    {
        private static final long serialVersionUID = -8505492366690693091L;

        final VID vid;
        final String label;
        final AnnotationLayer layer;
        final boolean singleton;
        final double score;

        public AnnotationListItem(VID aVid, String aLabel, AnnotationLayer aLayer,
                boolean aSingleton, double aScore)
        {
            vid = aVid;
            label = aLabel;
            layer = aLayer;
            singleton = aSingleton;
            score = aScore;
        }
    }
}
