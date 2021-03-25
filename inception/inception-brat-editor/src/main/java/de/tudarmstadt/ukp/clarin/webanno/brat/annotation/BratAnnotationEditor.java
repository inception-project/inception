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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter.decodeTypeName;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType.DIFFERENTIAL;
import static de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType.FULL;
import static de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType.SKIP;
import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil.serverTiming;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.googlecode.wicket.jquery.ui.settings.JQueryUILibrarySettings;
import com.googlecode.wicket.jquery.ui.widget.menu.IMenuItem;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRenderedMetaDataKey;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.Selection;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.Renderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VLazyDetailResult;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorProperties;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.ArcAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.DoActionResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.LoadConfResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.NormDataResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.SpanAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.VisualOptions;
import de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics;
import de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratRenderer;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.NormalizationQueryResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAjaxResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAnnotatorUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratConfigurationResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratCssUiReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratCssVisReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratDispatcherResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratUtilResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratVisualizerResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratVisualizerUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQueryJsonResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQueryScrollbarWidthReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQuerySvgDomResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQuerySvgResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JSONPatchResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;

/**
 * Brat annotator component.
 */
public class BratAnnotationEditor
    extends AnnotationEditorBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = -1537506294440056609L;

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ARC_ID = "arcId";
    private static final String PARAM_ID = "id";
    private static final String PARAM_OFFSETS = "offsets";
    private static final String PARAM_TARGET_SPAN_ID = "targetSpanId";
    private static final String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_LAZY_DETAIL_DATABASE = "database";
    private static final String PARAM_LAZY_DETAIL_KEY = "key";

    private static final String ACTION_CONTEXT_MENU = "contextMenu";

    private final ContextMenu contextMenu;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ColoringService coloringService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean BratMetrics metrics;
    private @SpringBean BratAnnotationEditorProperties bratProperties;

    private WebMarkupContainer vis;
    private AbstractAjaxBehavior requestHandler;

    private transient JsonNode lastRenederedJsonParsed;
    private String lastRenderedJson;
    private int lastRenderedWindowStart = -1;

    public BratAnnotationEditor(String id, IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider)
    {
        super(id, aModel, aActionHandler, aCasProvider);

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);
        add(vis);

        LOG.trace("[{}][{}] BratAnnotationEditor", getMarkupId(), vis.getMarkupId());

        contextMenu = new ContextMenu("contextMenu");
        add(contextMenu);

        requestHandler = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
                if (getModelObject().getDocument() == null) {
                    return;
                }

                long timerStart = System.currentTimeMillis();

                final IRequestParameters request = getRequest().getPostParameters();

                // Get action from the request
                String action = request.getParameterValue(PARAM_ACTION).toString();
                LOG.trace("AJAX-RPC CALLED: [{}]", action);

                // Parse annotation ID if present in request
                final VID paramId;
                if (!request.getParameterValue(PARAM_ID).isEmpty()
                        && !request.getParameterValue(PARAM_ARC_ID).isEmpty()) {
                    throw new IllegalStateException(
                            "[id] and [arcId] cannot be both set at the same time!");
                }
                else if (!request.getParameterValue(PARAM_ID).isEmpty()) {
                    paramId = VID.parseOptional(request.getParameterValue(PARAM_ID).toString());
                }
                else {
                    VID arcId = VID
                            .parseOptional(request.getParameterValue(PARAM_ARC_ID).toString());
                    // HACK: If an arc was clicked that represents a link feature, then
                    // open the associated span annotation instead.
                    if (arcId.isSlotSet() && ArcAnnotationResponse.is(action)) {
                        action = SpanAnnotationResponse.COMMAND;
                        paramId = new VID(arcId.getId());
                    }
                    else {
                        paramId = arcId;
                    }
                }

                // Load the CAS if necessary
                // Make sure we load the CAS only once here in case of an annotation action.
                boolean requiresCasLoading = SpanAnnotationResponse.is(action)
                        || ArcAnnotationResponse.is(action) || GetDocumentResponse.is(action)
                        || DoActionResponse.is(action);
                final CAS cas;
                if (requiresCasLoading) {
                    try {
                        cas = getCasProvider().get();
                    }
                    catch (Exception e) {
                        handleError("Unable to load data: " + getRootCauseMessage(e), e);
                        return;
                    }
                }
                else {
                    cas = null;
                }

                Object result = null;
                try {
                    // Whenever an action should be performed, do ONLY perform this action and
                    // nothing else, and only if the item actually is an action item
                    if (NormDataResponse.is(action)) {
                        result = actionLookupNormData(aTarget, request, paramId);
                    }
                    else if (DoActionResponse.is(action)) {
                        if (paramId.isSynthetic()) {
                            extensionRegistry.fireAction(getActionHandler(), getModelObject(),
                                    aTarget, cas, paramId, action);
                        }
                        else {
                            actionDoAction(aTarget, request, cas, paramId);
                        }
                    }
                    else {
                        if (paramId.isSynthetic()) {
                            extensionRegistry.fireAction(getActionHandler(), getModelObject(),
                                    aTarget, cas, paramId, action);
                        }
                        else {
                            // Doing anything but selecting or creating a span annotation when a
                            // slot is armed will unarm it
                            if (getModelObject().isSlotArmed()
                                    && !SpanAnnotationResponse.is(action)) {
                                getModelObject().clearArmedSlot();
                            }

                            if (ACTION_CONTEXT_MENU.equals(action.toString())
                                    && !paramId.isSlotSet()) {
                                actionOpenContextMenu(aTarget, request, cas, paramId);
                            }
                            else if (SpanAnnotationResponse.is(action)) {
                                result = actionSpan(aTarget, request, cas, paramId);
                            }
                            else if (ArcAnnotationResponse.is(action)) {
                                result = actionArc(aTarget, request, cas, paramId);
                            }
                            else if (LoadConfResponse.is(action)) {
                                result = new LoadConfResponse(bratProperties);
                            }
                            else if (GetCollectionInformationResponse.is(action)) {
                                result = actionGetCollectionInformation();
                            }
                            else if (GetDocumentResponse.is(action)) {
                                result = actionGetDocument(cas);
                            }
                        }
                    }
                }
                catch (Exception e) {
                    handleError("Error: " + getRootCauseMessage(e), e);
                }

                // Serialize updated document to JSON
                if (result == null) {
                    LOG.trace("AJAX-RPC: Action [{}] produced no result!", action);
                }
                else {
                    String json;
                    if (result instanceof String) {
                        json = (String) result;
                    }
                    else {
                        json = toJson(result);
                    }

                    // Since we cannot pass the JSON directly to Brat, we attach it to the HTML
                    // element into which BRAT renders the SVG. In our modified ajax.js, we pick it
                    // up from there and then pass it on to BRAT to do the rendering.
                    aTarget.prependJavaScript(
                            "Wicket.$('" + vis.getMarkupId() + "').temp = " + json + ";");
                }

                long duration = System.currentTimeMillis() - timerStart;
                LOG.trace("AJAX-RPC DONE: [{}] completed in {}ms", action, duration);

                serverTiming("Brat-AJAX", "Brat-AJAX (" + action + ")", duration);
            }
        };

        add(requestHandler);
    }

    private Object actionLookupNormData(AjaxRequestTarget aTarget, IRequestParameters request,
            VID paramId)
        throws AnnotationException, IOException
    {
        NormDataResponse response = new NormDataResponse();

        // We interpret the databaseParam as the feature which we need to look up the feature
        // support
        StringValue databaseParam = request.getParameterValue(PARAM_LAZY_DETAIL_DATABASE);

        // We interpret the key as the feature value or as a kind of query to be handled by the
        // feature support
        StringValue keyParam = request.getParameterValue(PARAM_LAZY_DETAIL_KEY);

        StringValue layerParam = request.getParameterValue(PARAM_TYPE);

        if (layerParam.isEmpty() || databaseParam.isEmpty()) {
            return response;
        }

        String database = databaseParam.toString();
        long layerId = decodeTypeName(layerParam.toString());
        AnnotatorState state = getModelObject();
        AnnotationLayer layer = annotationService.getLayer(state.getProject(), layerId)
                .orElseThrow(() -> new AnnotationException("Layer with ID [" + layerId
                        + "] does not exist in project [" + state.getProject().getName() + "]("
                        + state.getProject().getId() + ")"));

        // Check where the query needs to be routed: to an editor extension or to a feature support
        if (paramId.isSynthetic()) {
            if (keyParam.isEmpty()) {
                return response;
            }

            AnnotationFeature feature = annotationService.getFeature(database, layer);

            String extensionId = paramId.getExtensionId();
            response.setResults(extensionRegistry.getExtension(extensionId)
                    .renderLazyDetails(state.getDocument(), state.getUser(), paramId, feature,
                            keyParam.toString())
                    .stream().map(d -> new NormalizationQueryResult(d.getLabel(), d.getValue()))
                    .collect(Collectors.toList()));
            return response;
        }

        try {
            List<VLazyDetailResult> details;

            // Is it a layer-level lazy detail?
            if (Renderer.QUERY_LAYER_LEVEL_DETAILS.equals(database)) {
                details = layerSupportRegistry.getLayerSupport(layer)
                        .createRenderer(layer, () -> annotationService.listAnnotationFeature(layer))
                        .renderLazyDetails(getCasProvider().get(), paramId);
            }
            // Is it a feature-level lazy detail?
            else {
                AnnotationFeature feature = annotationService.getFeature(database, layer);
                details = featureSupportRegistry.findExtension(feature).renderLazyDetails(feature,
                        keyParam.toString());
            }

            response.setResults(details.stream()
                    .map(d -> new NormalizationQueryResult(d.getLabel(), d.getValue()))
                    .collect(toList()));
        }
        catch (Exception e) {
            handleError("Unable to load data", e);
        }

        return response;
    }

    private void actionOpenContextMenu(AjaxRequestTarget aTarget, IRequestParameters request,
            CAS aCas, VID paramId)
    {
        List<IMenuItem> items = contextMenu.getItemList();
        items.clear();

        if (getModelObject().getSelection().isSpan()) {
            items.add(new LambdaMenuItem("Link to ...",
                    _target -> actionArcRightClick(_target, paramId)));
        }

        extensionRegistry.generateContextMenuItems(items);

        if (!items.isEmpty()) {
            contextMenu.onOpen(aTarget);
        }
    }

    private Object actionDoAction(AjaxRequestTarget aTarget, IRequestParameters request, CAS aCas,
            VID paramId)
        throws AnnotationException, IOException
    {
        StringValue layerParam = request.getParameterValue(PARAM_TYPE);

        if (!layerParam.isEmpty()) {
            long layerId = decodeTypeName(layerParam.toString());
            AnnotatorState state = getModelObject();
            AnnotationLayer layer = annotationService.getLayer(state.getProject(), layerId)
                    .orElseThrow(() -> new AnnotationException("Layer with ID [" + layerId
                            + "] does not exist in project [" + state.getProject().getName() + "]("
                            + state.getProject().getId() + ")"));
            if (!StringUtils.isEmpty(layer.getOnClickJavascriptAction())) {
                // parse the action
                List<AnnotationFeature> features = annotationService.listSupportedFeatures(layer);
                AnnotationFS anno = selectAnnotationByAddr(aCas, paramId.getId());
                Map<String, Object> functionParams = OnClickActionParser.parse(layer, features,
                        getModelObject().getDocument(), anno);
                // define anonymous function, fill the body and immediately execute
                String js = String.format("(function ($PARAM){ %s })(%s)",
                        WicketUtil.wrapInTryCatch(layer.getOnClickJavascriptAction()),
                        JSONUtil.toJsonString(functionParams));
                aTarget.appendJavaScript(js);
            }
        }

        return null;
    }

    private SpanAnnotationResponse actionSpan(AjaxRequestTarget aTarget, IRequestParameters request,
            CAS aCas, VID aSelectedAnnotation)
        throws IOException, AnnotationException
    {
        // This is the span the user has marked in the browser in order to create a new slot-filler
        // annotation OR the span of an existing annotation which the user has selected.
        Offsets optUserSelectedSpan = getOffsetsFromRequest(request, aCas, aSelectedAnnotation);

        Offsets userSelectedSpan = optUserSelectedSpan;
        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();

        if (state.isSlotArmed()) {
            // When filling a slot, the current selection is *NOT* changed. The
            // Span annotation which owns the slot that is being filled remains
            // selected!
            getActionHandler().actionFillSlot(aTarget, aCas, userSelectedSpan.getBegin(),
                    userSelectedSpan.getEnd(), aSelectedAnnotation);
        }
        else {
            if (!aSelectedAnnotation.isSynthetic()) {
                selection.selectSpan(aSelectedAnnotation, aCas, userSelectedSpan.getBegin(),
                        userSelectedSpan.getEnd());

                if (selection.getAnnotation().isNotSet()) {
                    // Create new annotation
                    getActionHandler().actionCreateOrUpdate(aTarget, aCas);
                }
                else {
                    getActionHandler().actionSelect(aTarget);
                }
            }
        }

        return new SpanAnnotationResponse();
    }

    private ArcAnnotationResponse actionArc(AjaxRequestTarget aTarget, IRequestParameters request,
            CAS aCas, VID paramId)
        throws IOException, AnnotationException
    {
        AnnotationFS originFs = selectAnnotationByAddr(aCas,
                request.getParameterValue(PARAM_ORIGIN_SPAN_ID).toInt());
        AnnotationFS targetFs = selectAnnotationByAddr(aCas,
                request.getParameterValue(PARAM_TARGET_SPAN_ID).toInt());

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();
        selection.selectArc(paramId, originFs, targetFs);

        if (selection.getAnnotation().isNotSet()) {
            // Create new annotation
            getActionHandler().actionCreateOrUpdate(aTarget, aCas);
        }
        else {
            getActionHandler().actionSelect(aTarget);
        }

        return new ArcAnnotationResponse();
    }

    private void actionArcRightClick(AjaxRequestTarget aTarget, VID paramId)
        throws IOException, AnnotationException
    {
        if (!getModelObject().getSelection().isSpan()) {
            return;
        }

        CAS cas;
        try {
            cas = getCasProvider().get();
        }
        catch (Exception e) {
            handleError("Unable to load data", e);
            return;
        }

        // Currently selected span
        AnnotationFS originFs = selectAnnotationByAddr(cas,
                getModelObject().getSelection().getAnnotation().getId());

        // Target span of the relation
        AnnotationFS targetFs = selectAnnotationByAddr(cas, paramId.getId());

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();
        selection.selectArc(VID.NONE_ID, originFs, targetFs);

        // Create new annotation
        getActionHandler().actionCreateOrUpdate(aTarget, cas);
    }

    private GetCollectionInformationResponse actionGetCollectionInformation()
    {
        GetCollectionInformationResponse info = new GetCollectionInformationResponse();
        if (getModelObject().getProject() != null) {
            info.setEntityTypes(BratRenderer.buildEntityTypes(getModelObject().getProject(),
                    getModelObject().getAnnotationLayers(), annotationService));
            info.getVisualOptions()
                    .setArcBundle(getModelObject().getPreferences().isCollapseArcs()
                            ? VisualOptions.ARC_BUNDLE_ALL
                            : VisualOptions.ARC_BUNDLE_NONE);
        }
        return info;
    }

    private String actionGetDocument(CAS aCas)
    {
        StopWatch timer = new StopWatch();
        timer.start();

        GetDocumentResponse response = new GetDocumentResponse();
        String json;
        if (getModelObject().getProject() != null) {
            render(response, aCas);
            json = toJson(response);
            lastRenderedJson = json;
            lastRenederedJsonParsed = null;
        }
        else {
            json = toJson(response);
        }

        timer.stop();
        metrics.renderComplete(RenderType.FULL, timer.getTime(), json, null);
        serverTiming("Brat-JSON", "Brat JSON generation (FULL)", timer.getTime());

        return json;
    }

    /**
     * Extract offset information from the current request. These are either offsets of an existing
     * selected annotations or offsets contained in the request for the creation of a new
     * annotation.
     */
    private Offsets getOffsetsFromRequest(IRequestParameters request, CAS aCas, VID aVid)
        throws IOException
    {
        if (aVid.isNotSet() || aVid.isSynthetic()) {
            // Create new span annotation - in this case we get the offset information from the
            // request
            String offsets = request.getParameterValue(PARAM_OFFSETS).toString();

            OffsetsList offsetLists = JSONUtil.getObjectMapper().readValue(offsets,
                    OffsetsList.class);

            int annotationBegin = getModelObject().getWindowBeginOffset()
                    + offsetLists.get(0).getBegin();
            int annotationEnd = getModelObject().getWindowBeginOffset()
                    + offsetLists.get(offsetLists.size() - 1).getEnd();
            return new Offsets(annotationBegin, annotationEnd);
        }
        else {
            // Edit existing span annotation - in this case we look up the offsets in the CAS
            // Let's not trust the client in this case.
            AnnotationFS fs = WebAnnoCasUtil.selectAnnotationByAddr(aCas, aVid.getId());
            return new Offsets(fs.getBegin(), fs.getEnd());
        }
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(getModelObject() != null && getModelObject().getProject() != null);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        // CSS
        aResponse.render(CssHeaderItem.forReference(BratCssVisReference.get()));
        aResponse.render(CssHeaderItem.forReference(BratCssUiReference.get()));
        aResponse.render(CssHeaderItem
                .forReference(new WebjarsCssResourceReference("animate.css/current/animate.css")));

        // Libraries
        aResponse.render(forReference(JQueryUILibrarySettings.get().getJavaScriptReference()));
        aResponse.render(forReference(JQuerySvgResourceReference.get()));
        aResponse.render(forReference(JQuerySvgDomResourceReference.get()));
        aResponse.render(forReference(JQueryJsonResourceReference.get()));
        aResponse.render(forReference(JQueryScrollbarWidthReference.get()));
        aResponse.render(forReference(JSONPatchResourceReference.get()));

        // BRAT helpers
        aResponse.render(forReference(BratConfigurationResourceReference.get()));
        aResponse.render(forReference(BratUtilResourceReference.get()));
        // aResponse.render(
        // JavaScriptHeaderItem.forReference(BratAnnotationLogResourceReference.get()));

        // BRAT modules
        aResponse.render(forReference(BratDispatcherResourceReference.get()));
        aResponse.render(forReference(BratAjaxResourceReference.get()));
        aResponse.render(forReference(BratVisualizerResourceReference.get()));
        aResponse.render(forReference(BratVisualizerUiResourceReference.get()));
        aResponse.render(forReference(BratAnnotatorUiResourceReference.get()));
        // aResponse.render(
        // JavaScriptHeaderItem.forReference(BratUrlMonitorResourceReference.get()));

        // When the page is re-loaded or when the component is added to the page, we need to
        // initialize the brat stuff.
        StringBuilder js = new StringBuilder();
        js.append(bratInitCommand());
        js.append(bratLoadCollectionCommand());

        // If a document is already open, we also need to render the document. This happens either
        // when a page is freshly loaded or when e.g. the whole editor is added to the page or
        // when it is added to a partial page update (AJAX request).
        // If the editor is part of a full or partial page update, then it needs to be
        // reinitialized. So we need to use deferred rendering. The render() method checks the
        // partial page update to see if the editor is part of it and if so, it skips itself so
        // no redundant rendering is performed.
        if (getModelObject().getProject() != null) {
            js.append(bratRenderLaterCommand());
        }
        aResponse.render(OnDomReadyHeaderItem.forScript(js));
    }

    private Optional<String> bratRenderCommand(CAS aCas)
    {
        LOG.trace("[{}][{}] bratRenderCommand", getMarkupId(), vis.getMarkupId());

        StopWatch timer = new StopWatch();
        timer.start();

        GetDocumentResponse response = new GetDocumentResponse();
        render(response, aCas);

        ObjectMapper mapper = JSONUtil.getObjectMapper();
        JsonNode current = mapper.valueToTree(response);
        String json = toJson(current);

        // By default, we do a full rendering...
        RenderType renderType = FULL;
        String cmd = "renderData";
        String responseJson = json;
        JsonNode diff;
        String diffJsonStr = null;

        // Here, we try to balance server CPU load against network load. So if we have a chance
        // of significantly reducing the data sent to the client via a differential update, then
        // we try that. However, if it is pretty obvious that we won't save a lot, then we will
        // not even try. I.e. we apply some heuristics to see if large parts of the editor have
        // changed.
        AnnotatorState aState = getModelObject();
        boolean tryDifferentialUpdate = lastRenderedWindowStart >= 0
                // Check if we did a far scroll or switch pages
                && Math.abs(lastRenderedWindowStart - aState.getWindowBeginOffset()) < aState
                        .getPreferences().getWindowSize() / 3;

        if (tryDifferentialUpdate) {
            // ... try to render diff
            JsonNode previous = null;
            try {
                if (lastRenederedJsonParsed != null) {
                    previous = lastRenederedJsonParsed;
                }
                else {
                    previous = lastRenderedJson != null ? mapper.readTree(lastRenderedJson) : null;
                }
            }
            catch (IOException e) {
                LOG.error("Unable to generate diff, falling back to full render.", e);
                // Fall-through
            }

            if (previous != null && current != null) {
                diff = JsonDiff.asJson(previous, current);
                diffJsonStr = diff.toString();

                if (diff instanceof ArrayNode && ((ArrayNode) diff).isEmpty()) {
                    // No difference? Well, don't render at all :)
                    renderType = SKIP;
                }
                else if (diffJsonStr.length() < json.length()) {
                    // Only sent a patch if it is smaller than sending the full data. E.g. when
                    // switching pages, the patch usually ends up being twice as large as the full
                    // data.
                    cmd = "renderDataPatch";
                    responseJson = diffJsonStr;
                    renderType = DIFFERENTIAL;
                }

                // LOG.info("Diff: " + diff);
                // LOG.info("Full: {} Patch: {} Diff time: {}", json.length(), diff.length(),
                // timer);
            }
        }

        // Storing the last rendered JSON as string because JsonNodes are not serializable.
        lastRenderedJson = json;
        lastRenederedJsonParsed = current;
        lastRenderedWindowStart = aState.getWindowBeginOffset();

        timer.stop();

        metrics.renderComplete(renderType, timer.getTime(), json, diffJsonStr);
        serverTiming("Brat-JSON", "Brat-JSON generation (" + renderType + ")", timer.getTime());

        if (SKIP.equals(renderType)) {
            return Optional.empty();
        }

        return Optional.of("Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('" + cmd + "', ["
                + responseJson + "]);");
    }

    private void render(GetDocumentResponse response, CAS aCas)
    {
        AnnotatorState aState = getModelObject();
        VDocument vdoc = render(aCas, aState.getWindowBeginOffset(), aState.getWindowEndOffset());
        BratRenderer renderer = new BratRenderer(annotationService, coloringService,
                bratProperties);
        renderer.render(response, aState, vdoc, aCas);
    }

    private String bratInitCommand()
    {
        LOG.trace("[{}][{}] bratInitCommand", getMarkupId(), vis.getMarkupId());

        // REC 2014-10-18 - For a reason that I do not understand, the dispatcher cannot be a local
        // variable. If I put a "var" here, then communication fails with messages such as
        // "action 'openSpanDialog' returned result of action 'loadConf'" in the browsers's JS
        // console.
        StringBuilder js = new StringBuilder();

        js.append("(function() {");
        if (bratProperties.isClientSideTraceLog()) {
            js.append("  console.log('Initializing (" + vis.getMarkupId() + ")...');");
        }
        js.append("  var dispatcher = new Dispatcher();");
        // Each visualizer talks to its own Wicket component instance
        js.append("  dispatcher.ajaxUrl = '" + requestHandler.getCallbackUrl() + "'; ");
        // We attach the JSON send back from the server to this HTML element
        // because we cannot directly pass it from Wicket to the caller in ajax.js.
        js.append("  dispatcher.wicketId = '" + vis.getMarkupId() + "'; ");
        js.append("  var ajax = new Ajax(dispatcher);");
        js.append("  var visualizer = new Visualizer(dispatcher, '" + vis.getMarkupId() + "');");
        js.append("  var visualizerUI = new VisualizerUI(dispatcher, visualizer.svg);");
        js.append("  var annotatorUI = new AnnotatorUI(dispatcher, visualizer.svg);");
        // js.append(("var logger = new AnnotationLog(dispatcher);");
        js.append("  dispatcher.post('init');");
        js.append("  Wicket.$('" + vis.getMarkupId() + "').dispatcher = dispatcher;");
        js.append("  Wicket.$('" + vis.getMarkupId() + "').visualizer = visualizer;");
        js.append("})();");

        return js.toString();
    }

    private String bratLoadCollectionCommand()
    {
        LOG.trace("[{}][{}] bratLoadCollectionCommand", getMarkupId(), vis.getMarkupId());

        GetCollectionInformationResponse response = actionGetCollectionInformation();
        response.setEntityTypes(BratRenderer.buildEntityTypes(getModelObject().getProject(),
                getModelObject().getAnnotationLayers(), annotationService));
        String json = toJson(response);

        StringBuilder js = new StringBuilder();
        if (bratProperties.isClientSideTraceLog()) {
            js.append("console.log('Loading collection (" + vis.getMarkupId() + ")...');");
        }
        js.append("Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('collectionLoaded', ["
                + json + "]);");
        return js.toString();
    }

    /**
     * This one triggers the loading of the actual document data
     *
     * @return brat
     */
    private String bratRenderLaterCommand()
    {
        LOG.trace("[{}][{}] bratRenderLaterCommand", getMarkupId(), vis.getMarkupId());

        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('current', " + "["
                + toJson(getCollection()) + ", '1234', {}, true]);";
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        // Check if this editor has already been rendered in the current request cycle and if this
        // is the case, skip rendering.
        RequestCycle requestCycle = RequestCycle.get();
        Set<String> renderedEditors = requestCycle
                .getMetaData(AnnotationEditorRenderedMetaDataKey.INSTANCE);
        if (renderedEditors == null) {
            renderedEditors = new HashSet<>();
            requestCycle.setMetaData(AnnotationEditorRenderedMetaDataKey.INSTANCE, renderedEditors);
        }

        if (renderedEditors.contains(getMarkupId())) {
            LOG.trace("[{}][{}] render (AJAX) - was already rendered in this cycle - skipping",
                    getMarkupId(), vis.getMarkupId());
            return;
        }

        renderedEditors.add(getMarkupId());

        // Check if the editor or any of its parents has been added to a partial page update. If
        // this is the case, then deferred rendering in renderHead kicks in and we do not need to
        // render here.
        Set<Component> components = new HashSet<>(aTarget.getComponents());
        boolean deferredRenderingRequired = components.contains(this)
                || visitParents(MarkupContainer.class, (aParent, aVisit) -> {
                    if (components.contains(aParent)) {
                        aVisit.stop(aParent);
                    }
                }) != null;
        if (deferredRenderingRequired) {
            LOG.trace("[{}][{}] render (AJAX) - deferred rendering will trigger - skipping",
                    getMarkupId(), vis.getMarkupId());
            return;
        }

        LOG.trace("[{}][{}] render (AJAX)", getMarkupId(), vis.getMarkupId());

        try {
            bratRenderCommand(getCasProvider().get()).ifPresent(cmd -> {
                StringBuilder js = new StringBuilder();

                if (bratProperties.isDeferredRendering()) {
                    js.append("setTimeout(function() {");
                }

                if (bratProperties.isClientSideProfiling()) {
                    js.append("Util.profileEnable(true);");
                    js.append("Util.profileClear();");
                }

                if (bratProperties.isClientSideTraceLog()) {
                    js.append("console.log('Rendering (" + vis.getMarkupId() + ")...');");
                }

                js.append(cmd);

                if (bratProperties.isClientSideProfiling()) {
                    js.append("Util.profileReport();");
                }

                if (bratProperties.isDeferredRendering()) {
                    js.append("}, 1);");
                }

                aTarget.appendJavaScript(js);
            });
        }
        catch (IOException e) {
            handleError("Unable to load data", e);
        }
    }

    private String getCollection()
    {
        if (getModelObject().getProject() != null) {
            return "#" + getModelObject().getProject().getName() + "/";
        }
        else {
            return "";
        }
    }

    private String toJson(Object result)
    {
        String json = "[]";
        try {
            if (result instanceof JsonNode) {
                json = JSONUtil.toInterpretableJsonString((JsonNode) result);
            }
            else {
                json = JSONUtil.toInterpretableJsonString(result);
            }
        }
        catch (IOException e) {
            handleError("Unable to produce JSON response", e);
        }
        return json;
    }

    private void handleError(String aMessage, Exception e)
    {
        RequestCycle requestCycle = RequestCycle.get();
        requestCycle.find(AjaxRequestTarget.class)
                .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));

        if (e instanceof AnnotationException) {
            // These are common exceptions happening as part of the user interaction. We do
            // not really need to log their stack trace to the log.
            error(aMessage + ": " + e.getMessage());
            // If debug is enabled, we'll also write the error to the log just in case.
            if (LOG.isDebugEnabled()) {
                LOG.error("{}: {}", aMessage, e.getMessage(), e);
            }
            return;
        }

        LOG.error("{}", aMessage, e);
        error(aMessage);
    }
}
