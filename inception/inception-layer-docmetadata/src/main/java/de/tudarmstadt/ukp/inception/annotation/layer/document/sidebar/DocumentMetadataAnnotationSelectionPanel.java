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

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.forUser;
import static de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport.FEATURE_NAME_ORDER;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.MAIN_EDITOR;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CLICK_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Optional.empty;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.wicket.event.Broadcast.BREADTH;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.cas.AnnotationBase;
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
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.CreateDocumentAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerTraits;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.event.DocumentMetadataEvent;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.curation.api.Position;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.event.AjaxRecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.model.MetadataSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
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

public class DocumentMetadataAnnotationSelectionPanel
    extends GenericPanel<AnnotatorState>
{
    private static final long serialVersionUID = 8318858582025740458L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CURATION_EXTENSION_ID = "meta-cur";

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
    private @SpringBean CurationSessionService curationSessionService;
    private @SpringBean DiffAdapterRegistry diffAdapterRegistry;
    private @SpringBean DocumentService documentService;
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

            switch (aItem.kind) {
            case RECOMMENDATION -> acceptSuggestion(aTarget, aItem);
            case CURATION -> mergeCuration(aTarget, aItem);
            case ANNOTATION -> {
                // Nothing to do
            }
            }
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }
    }

    private void acceptSuggestion(AjaxRequestTarget aTarget, AnnotationListItem aItem)
        throws AnnotationException, IOException
    {
        var state = getModelObject();
        var dataOwner = state.getUser().getUsername();
        var sessionOwner = userService.getCurrentUser();
        var maybeSuggestion = recommendationService.getSuggestionByVID(sessionOwner,
                state.getDocument(), aItem.vid);

        if (maybeSuggestion.isEmpty()) {
            if (aTarget != null) {
                aTarget.addChildren(getPage(), IFeedback.class);
            }
            error("Suggestion no longer available, please refresh.");
            return;
        }

        var cas = casProvider.get();

        recommendationService.acceptSuggestion(sessionOwner.getUsername(), state.getDocument(),
                dataOwner, cas, maybeSuggestion.get().getKey(), maybeSuggestion.get().getValue(),
                MAIN_EDITOR);

        annotationPage.writeEditorCas(cas);

        send(annotationPage, BREADTH,
                new AjaxRecommendationAcceptedEvent(aTarget, state, aItem.vid));
    }

    private void mergeCuration(AjaxRequestTarget aTarget, AnnotationListItem aItem)
        throws AnnotationException, IOException
    {
        var state = getModelObject();
        var curationReference = parseCurationReference(aItem.vid)
                .orElseThrow(() -> new IllegalStateException("Invalid curation item identifier"));

        var sourceCas = documentService.readAnnotationCas(state.getDocument(),
                AnnotationSet.forUser(curationReference.sourceUser()));
        var sourceFs = selectFsByAddr(sourceCas, curationReference.sourceVid().getId());

        if (!(sourceFs instanceof AnnotationBase sourceAnnotation)) {
            throw new IllegalStateException("Curation source annotation could not be resolved");
        }

        var targetCas = casProvider.get();
        mergeDocumentMetadataAnnotation(state, aItem.layer, targetCas,
                curationReference.sourceUser(), sourceAnnotation);
        annotationPage.writeEditorCas(targetCas);
        aTarget.add(layersContainer);
        annotationPage.actionRefreshDocument(aTarget);
    }

    private void actionRejectSuggestion(AjaxRequestTarget aTarget, AnnotationListItem aItem)
    {
        try {
            annotationPage.ensureIsEditable();

            var state = getModelObject();
            var dataOwner = state.getUser().getUsername();
            var sessionOwner = userService.getCurrentUser();
            var maybeSuggestion = recommendationService
                    .getSuggestionByVID(sessionOwner, state.getDocument(), aItem.vid)
                    .map(Pair::getValue);

            if (maybeSuggestion.isEmpty()) {
                return;
            }

            var aCas = casProvider.get();

            recommendationService.rejectSuggestion(sessionOwner.getUsername(), state.getDocument(),
                    dataOwner, maybeSuggestion.get(), MAIN_EDITOR);

            annotationPage.writeEditorCas(aCas);

            send(annotationPage, BREADTH,
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
            var fs = selectFsByAddr(cas, aDetailPanel.getModelObject().getId());
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
            // if container is already selected, de-select and close annotation
            selectedAnnotation = null;
        }
        else {
            selectedAnnotation = container;
        }

        if (selectedDetailPanel == detailPanel) {
            // if detail panel is already selected, de-select and close annotation
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
                var annotations = Model.ofList(aItem.getModelObject().annotations);

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
                var itemState = aItem.getModelObject();

                var container = new WebMarkupContainer(CID_ANNOTATION);
                container.add(visibleWhen(
                        () -> !itemState.singleton || itemState.kind != ItemKind.ANNOTATION));
                aItem.add(container);

                Component detailPanel;
                if (itemState.kind == ItemKind.ANNOTATION) {
                    detailPanel = new DocumentMetadataAnnotationDetailPanel(CID_ANNOTATION_DETAILS,
                            Model.of(vid), casProvider, annotationPage, actionHandler,
                            DocumentMetadataAnnotationSelectionPanel.this.getModel());
                }
                else {
                    detailPanel = new WebMarkupContainer(CID_ANNOTATION_DETAILS);
                    detailPanel.setVisible(false);
                }
                aItem.add(detailPanel);

                var isRecommendation = itemState.kind == ItemKind.RECOMMENDATION;
                var isCuration = itemState.kind == ItemKind.CURATION;
                var isSuggestion = isRecommendation || isCuration;

                if (itemState.kind == ItemKind.ANNOTATION) {
                    container.add(new LambdaAjaxEventBehavior(CLICK_EVENT, $ -> actionSelect($,
                            container, (DocumentMetadataAnnotationDetailPanel) detailPanel)));
                }

                if (detailPanel instanceof DocumentMetadataAnnotationDetailPanel metadataDetailPanel) {
                    metadataDetailPanel.add(visibleWhen(() -> isExpanded(aItem, container)));
                }

                if (itemState.kind == ItemKind.ANNOTATION
                        && createdAnnotationAddress == vid.getId()) {
                    createdAnnotationAddress = -1;
                    selectedAnnotation = container;
                    selectedDetailPanel = (DocumentMetadataAnnotationDetailPanel) detailPanel;
                }

                var close = new WebMarkupContainer(CID_CLOSE);
                close.add(visibleWhen(() -> isExpanded(aItem, container)
                        && itemState.kind == ItemKind.ANNOTATION));
                close.setOutputMarkupId(true);
                container.add(close);

                var open = new WebMarkupContainer(CID_OPEN);
                open.add(visibleWhen(() -> !isExpanded(aItem, container)
                        && itemState.kind == ItemKind.ANNOTATION));
                open.setOutputMarkupId(true);
                container.add(open);

                container.add(new Label(CID_LABEL, defaultIfEmpty(itemState.label, "[No label]"))
                        .add(visibleWhen(() -> itemState.kind != ItemKind.ANNOTATION
                                || (!itemState.singleton && !isExpanded(aItem, container)))));

                aItem.queue(new LambdaAjaxLink(CID_DELETE,
                        $ -> actionDelete($, (DocumentMetadataAnnotationDetailPanel) detailPanel))
                                .add(AttributeModifier.replace("title", itemState.vid))
                                .add(visibleWhen(() -> itemState.kind == ItemKind.ANNOTATION
                                        && !itemState.singleton))
                                .add(enabledWhen(() -> annotationPage.isEditable()
                                        && !itemState.layer.isReadonly())));

                aItem.queue(new Label(CID_SCORE,
                        format(Session.get().getLocale(), "%.2f", itemState.score))
                                .add(visibleWhen(() -> isSuggestion && itemState.score != 0.0d)));

                aItem.queue(new LambdaAjaxLink(CID_ACCEPT, $ -> actionAccept($, itemState))
                        .add(AttributeModifier.replace("title", itemState.vid))
                        .add(visibleWhen(() -> isSuggestion && annotationPage.isEditable()
                                && !itemState.layer.isReadonly())));

                aItem.queue(
                        new LambdaAjaxLink(CID_REJECT, $ -> actionRejectSuggestion($, itemState))
                                .add(AttributeModifier.replace("title", itemState.vid))
                                .add(visibleWhen(
                                        () -> isRecommendation && annotationPage.isEditable()
                                                && !itemState.layer.isReadonly())));

                aItem.setOutputMarkupId(true);
            }

            private boolean isExpanded(ListItem<AnnotationListItem> aItem,
                    WebMarkupContainer container)
            {
                return aItem.getModelObject().kind == ItemKind.ANNOTATION
                        && (selectedAnnotation == container || aItem.getModelObject().singleton);
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
        CAS cas;
        try {
            cas = casProvider.get();
        }
        catch (IOException e) {
            LOG.error("Unable to load CAS", e);
            return emptyList();
        }

        var curationCandidatesByLayer = listCurationCandidatesByLayer(cas);

        return listMetadataLayers().stream() //
                .map(layer -> new LayerGroup(layer, listAnnotations(layer, cas,
                        curationCandidatesByLayer.getOrDefault(layer.getName(), emptyList()))))
                .toList();
    }

    private List<AnnotationListItem> listAnnotations(AnnotationLayer aLayer, CAS aCas,
            List<CurationCandidate> aCurationCandidates)
    {
        // Bulk-load all the features of this layer to avoid having to do repeated DB accesses
        // later
        var features = annotationService.listSupportedFeatures(aLayer);
        var layerSupport = getLayerSupport(aLayer);
        var singleton = layerSupport.readTraits(aLayer).isSingleton();

        var items = new ArrayList<AnnotationListItem>();
        generateAnnotationItems(aLayer, layerSupport, singleton, aCas, items, features);
        generateSuggestionItems(aLayer, layerSupport, singleton, aCas, items, features);
        generateCurationItems(aLayer, singleton, aCas, items, aCurationCandidates);
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
            aItems.add(new AnnotationListItem(VID.of(fs), labelText, aLayer, aSingleton, 0.0d,
                    order, ItemKind.ANNOTATION));
        }
    }

    private void generateSuggestionItems(AnnotationLayer aLayer,
            DocumentMetadataLayerSupport aLayerSupport, boolean aSingleton, CAS aCas,
            List<AnnotationListItem> aItems, List<AnnotationFeature> aFeatures)
    {
        var state = getModelObject();
        var predictions = recommendationService.getPredictions(state.getUser(), state.getProject())
                .values();
        for (var preds : predictions) {
            generateSuggestionItems(preds, aLayer, aLayerSupport, aSingleton, aCas, aItems,
                    aFeatures);
        }
    }

    private void generateSuggestionItems(Predictions predictions, AnnotationLayer aLayer,
            DocumentMetadataLayerSupport aLayerSupport, boolean aSingleton, CAS aCas,
            List<AnnotationListItem> aItems, List<AnnotationFeature> aFeatures)
    {
        var state = getModelObject();
        var featuresIndex = aFeatures.stream()
                .collect(toMap(AnnotationFeature::getName, identity()));

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
                    var feature = featuresIndex.get(metadataSuggestion.getFeature());

                    // Retrieve the UI display label for the given feature value
                    var featureSupport = fsRegistry.findExtension(feature).orElseThrow();
                    var annotation = featureSupport.renderFeatureValue(feature,
                            metadataSuggestion.getLabel());
                    aItems.add(new AnnotationListItem(metadataSuggestion.getVID(), annotation,
                            aLayer, false, metadataSuggestion.getScore(), aItems.size(),
                            ItemKind.RECOMMENDATION));
                }
            }
        }
    }

    private void generateCurationItems(AnnotationLayer aLayer, boolean aSingleton, CAS aTargetCas,
            List<AnnotationListItem> aItems, List<CurationCandidate> aCurationCandidates)
    {
        if (aCurationCandidates.isEmpty()) {
            return;
        }

        var adapter = (DocumentMetadataLayerAdapter) annotationService.getAdapter(aLayer);

        for (var candidate : aCurationCandidates) {
            if (isBlank(candidate.label()) && existsAnyAnnotation(aTargetCas, adapter)) {
                continue;
            }

            aItems.add(new AnnotationListItem(
                    createCurationVid(candidate.sourceUser(), candidate.sourceVid()),
                    candidate.label(), aLayer, aSingleton, candidate.score(), candidate.order(),
                    ItemKind.CURATION));
        }
    }

    private Map<String, List<CurationCandidate>> listCurationCandidatesByLayer(CAS aTargetCas)
    {
        if (!getPage().getPageClass().getName().contains("Curation")) {
            // Hack to only show curation items on the curation page without introducing a
            // circular dependency
            return Map.of();
        }

        var state = getModelObject();
        var sessionOwner = userService.getCurrentUsername();
        if (!curationSessionService.existsSession(sessionOwner, state.getProject().getId())) {
            return Map.of();
        }

        var selectedUsers = curationSessionService.listUsersReadyForCuration(sessionOwner,
                state.getProject(), state.getDocument());
        if (selectedUsers.isEmpty()) {
            return Map.of();
        }

        var targetUser = state.getUser().getUsername();
        var selectedUsernames = selectedUsers.stream() //
                .map(User::getUsername) //
                .filter(username -> !targetUser.equals(username)) //
                .distinct() //
                .sorted() //
                .toList();

        Map<String, CAS> casses = new LinkedHashMap<>();
        casses.put(targetUser, aTargetCas);

        for (var user : selectedUsers) {
            if (targetUser.equals(user.getUsername())) {
                continue;
            }

            try {
                var sourceCas = documentService.readAnnotationCas(state.getDocument(),
                        forUser(user));
                casses.put(user.getUsername(), sourceCas);
            }
            catch (IOException e) {
                LOG.error("Unable to read source CAS for curation user [{}]", user.getUsername(),
                        e);
            }
        }

        if (casses.size() <= 1) {
            return Map.of();
        }

        var metadataLayers = listMetadataLayers();
        var adapters = diffAdapterRegistry.getDiffAdapters(metadataLayers);
        var diff = doDiff(adapters, casses).toResult();
        var layersByName = metadataLayers.stream()
                .collect(toMap(AnnotationLayer::getName, identity()));
        var candidatesByLayer = new LinkedHashMap<String, List<CurationCandidate>>();
        var totalAnnotatorCount = selectedUsernames.size();

        for (var cfgSet : diff.getConfigurationSets()) {
            var layer = layersByName.get(cfgSet.getPosition().getType());
            if (layer == null || !isDocumentMetadataBasePosition(layer, cfgSet.getPosition())) {
                continue;
            }

            var adapter = (DocumentMetadataLayerAdapter) annotationService.getAdapter(layer);
            var features = annotationService.listSupportedFeatures(layer);
            var renderer = getLayerSupport(layer).createRenderer(layer,
                    () -> annotationService.listAnnotationFeature(layer));
            var candidates = candidatesByLayer.computeIfAbsent(layer.getName(),
                    $ -> new ArrayList<>());

            for (var cfg : cfgSet.getConfigurations()) {
                if (cfg.getCasGroupIds().contains(targetUser)) {
                    continue;
                }

                var representativeFs = cfg.getRepresentative(casses);
                if (!(representativeFs instanceof AnnotationBase representative)) {
                    continue;
                }

                var renderedFeatures = renderer.renderLabelFeatureValues(adapter, representative,
                        features);
                var labelText = TypeUtil.getUiLabelText(renderedFeatures);
                var order = representative.getType()
                        .getFeatureByBaseName(FEATURE_NAME_ORDER) != null
                                ? getFeature(representative, FEATURE_NAME_ORDER, Integer.class)
                                : candidates.size();
                var score = totalAnnotatorCount > 0
                        ? (double) cfg.getCasGroupIds().stream()
                                .filter(user -> !targetUser.equals(user)).count()
                                / (double) totalAnnotatorCount
                        : 0.0d;

                candidates.add(new CurationCandidate(cfg.getRepresentativeCasGroupId(),
                        VID.of(representative), labelText, score, order));
            }
        }

        return candidatesByLayer;
    }

    private boolean isDocumentMetadataBasePosition(AnnotationLayer aLayer, Position aPosition)
    {
        return Objects.equals(aLayer.getName(), aPosition.getType())
                && !aPosition.isLinkFeaturePosition();
    }

    private void mergeDocumentMetadataAnnotation(AnnotatorState aState, AnnotationLayer aLayer,
            CAS aTargetCas, String aSourceUser, AnnotationBase aSourceAnnotation)
        throws AnnotationException
    {
        var adapter = (DocumentMetadataLayerAdapter) annotationService.getAdapter(aLayer);
        var document = aState.getDocument();
        var dataOwner = aState.getUser().getUsername();

        try (var eventBatch = adapter.batchEvents()) {
            if (existsEquivalent(aTargetCas, adapter, aSourceAnnotation)) {
                throw new AnnotationException(
                        "The annotation already exists in the target document.");
            }

            var allowStacking = !adapter.getTraits(DocumentMetadataLayerTraits.class)
                    .map(DocumentMetadataLayerTraits::isSingleton).orElse(false);

            var targetType = adapter.getAnnotationType(aTargetCas)
                    .orElseThrow(() -> new IllegalStateException(
                            "Target CAS does not define the document metadata type"));
            var existingAnnotations = aTargetCas.select(targetType).asList();

            AnnotationBase targetAnnotation;
            var annotationCreated = false;
            if (existingAnnotations.isEmpty() || allowStacking) {
                targetAnnotation = adapter.handle(CreateDocumentAnnotationRequest.builder() //
                        .withDocument(document, dataOwner, aTargetCas) //
                        .build());
                annotationCreated = true;
            }
            else {
                targetAnnotation = (AnnotationBase) existingAnnotations.get(0);
            }

            try {
                copyCuratableFeatures(document, dataOwner, adapter, targetAnnotation,
                        aSourceAnnotation);
            }
            catch (AnnotationException e) {
                if (annotationCreated) {
                    adapter.delete(document, dataOwner, aTargetCas, VID.of(targetAnnotation));
                }
                throw e;
            }

            eventBatch.commit();
        }
    }

    private void copyCuratableFeatures(SourceDocument aDocument, String aDataOwner,
            DocumentMetadataLayerAdapter aAdapter, FeatureStructure aTargetAnnotation,
            FeatureStructure aSourceAnnotation)
        throws AnnotationException
    {
        for (var feature : annotationService.listSupportedFeatures(aAdapter.getLayer())) {
            if (!feature.isCuratable()) {
                continue;
            }

            var sourceType = aAdapter.getAnnotationType(aSourceAnnotation.getCAS())
                    .orElseThrow(() -> new IllegalStateException(
                            "Source CAS does not define the document metadata type"));
            if (sourceType.getFeatureByBaseName(feature.getName()) == null) {
                throw new IllegalStateException("Source CAS type [" + sourceType.getName()
                        + "] does not define a feature named [" + feature.getName() + "]");
            }

            if (!aAdapter.getFeatureSupport(feature.getName())
                    .map(fs -> fs.isCopyOnCurationMerge(feature)).orElse(false)) {
                continue;
            }

            var value = aAdapter.getFeatureValue(feature, aSourceAnnotation);
            aAdapter.setFeatureValue(aDocument, aDataOwner, aTargetAnnotation.getCAS(),
                    getAddr(aTargetAnnotation), feature, value);
        }
    }

    private boolean existsEquivalent(CAS aTargetCas, DocumentMetadataLayerAdapter aAdapter,
            AnnotationBase aSourceAnnotation)
    {
        var targetType = aAdapter.getAnnotationType(aTargetCas);
        if (targetType.isEmpty()) {
            return false;
        }

        return aTargetCas.<AnnotationBase> select(targetType.get())
                .anyMatch(fs -> aAdapter.isEquivalentAnnotation(fs, aSourceAnnotation));
    }

    private boolean existsAnyAnnotation(CAS aTargetCas, DocumentMetadataLayerAdapter aAdapter)
    {
        var targetType = aAdapter.getAnnotationType(aTargetCas);
        if (targetType.isEmpty()) {
            return false;
        }

        return aTargetCas.select(targetType.get()).findAny().isPresent();
    }

    private VID createCurationVid(String aSourceUser, VID aSourceVid)
    {
        return VID.builder() //
                .withAnnotationId(aSourceVid.getId()) //
                .withExtensionId(CURATION_EXTENSION_ID) //
                .withExtensionPayload(aSourceUser + "!" + aSourceVid.toString()) //
                .build();
    }

    private Optional<CurationReference> parseCurationReference(VID aVid)
    {
        if (!CURATION_EXTENSION_ID.equals(aVid.getExtensionId())
                || isBlank(aVid.getExtensionPayload())) {
            return empty();
        }

        var separator = aVid.getExtensionPayload().indexOf('!');
        if (separator < 0) {
            return empty();
        }

        var sourceUser = aVid.getExtensionPayload().substring(0, separator);
        var sourceVid = VID.parse(aVid.getExtensionPayload().substring(separator + 1));
        return Optional.of(new CurationReference(sourceUser, sourceVid));
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
        aEvent.getRequestTarget().ifPresent(target -> target.add(layersContainer));
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

    private record LayerGroup(AnnotationLayer layer, List<AnnotationListItem> annotations)
        implements Serializable
    {}

    private record CurationReference(String sourceUser, VID sourceVid)
        implements Serializable
    {}

    private record CurationCandidate(String sourceUser, VID sourceVid, String label, double score,
            int order)
        implements Serializable
    {}

    private enum ItemKind
    {
        ANNOTATION, RECOMMENDATION, CURATION
    }

    private record AnnotationListItem(VID vid, String label, AnnotationLayer layer,
            boolean singleton, double score, int order, ItemKind kind)
        implements Serializable
    {}
}
