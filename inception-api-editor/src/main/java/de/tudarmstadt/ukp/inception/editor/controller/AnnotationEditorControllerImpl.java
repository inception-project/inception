package de.tudarmstadt.ukp.inception.editor.controller;

import static de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType.DIFFERENTIAL;
import static de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType.FULL;
import static de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType.SKIP;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.*;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.Renderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VLazyDetailResult;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.OnClickActionParser;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorProperties;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.*;
import de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratRenderer;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.NormalizationQueryResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.model.*;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.events.Event;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter.decodeTypeName;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType.*;
import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil.serverTiming;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.SetUtils.unmodifiableSet;

@RestController
@RequestMapping(value = "/annotation/data")
public class AnnotationEditorControllerImpl implements AnnotationEditorController {


    private static final Logger LOG = LoggerFactory.getLogger(AnnotationEditorControllerImpl.class);


    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ARC_ID = "arcId";
    private static final String PARAM_ID = "id";
    private static final String PARAM_OFFSETS = "offsets";
    private static final String PARAM_TARGET_SPAN_ID = "targetSpanId";
    private static final String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    private static final String PARAM_SPAN_TYPE = "type";

    private static final String ACTION_CONTEXT_MENU = "contextMenu";

    private final ServletContext servletContext;

    private final DocumentService documentService;
    private final ProjectService projectService;
    private final AnnotationSchemaService annotationService;
    private final ColoringService coloringService;
    private final AnnotationEditorExtensionRegistry extensionRegistry;
    private final LayerSupportRegistry layerSupportRegistry;
    private final FeatureSupportRegistry featureSupportRegistry;
    private final BratMetrics metrics;
    private final BratAnnotationEditorProperties bratProperties;
    private CasProvider casProvider;

    private User currentUser;
    private SourceDocument currentSourceDocument;
    private Project currentProject;


    private transient JsonNode lastRenederedJsonParsed;
    private String lastRenderedJson;
    private int lastRenderedWindowStart = -1;


    private final Set<String> annotationEvents = unmodifiableSet( //
        SpanCreatedEvent.class.getSimpleName(), //
        SpanDeletedEvent.class.getSimpleName(), //
        RelationCreatedEvent.class.getSimpleName(), //
        RelationDeletedEvent.class.getSimpleName(), //
        ChainLinkCreatedEvent.class.getSimpleName(), //
        ChainLinkDeletedEvent.class.getSimpleName(), //
        ChainSpanCreatedEvent.class.getSimpleName(), //
        ChainSpanDeletedEvent.class.getSimpleName(), //
        FeatureValueUpdatedEvent.class.getSimpleName(), //
        "DocumentMetadataCreatedEvent", //
        "DocumentMetadataDeletedEvent");

    @Autowired
    public AnnotationEditorControllerImpl(DocumentService aDocumentService, ProjectService aProjectService, ServletContext aServletContext,
                                          AnnotationSchemaService aAnnotationService,
                                          ColoringService aColoringService,
                                          AnnotationEditorExtensionRegistry aExtensionRegistry,
                                          LayerSupportRegistry aLayerSupportRegistry,
                                          FeatureSupportRegistry aFeatureSupportRegistry,
                                          BratMetrics aMetrics,
                                          BratAnnotationEditorProperties aBratProperties) {
        super();
        annotationService = aAnnotationService;
        coloringService = aColoringService;
        extensionRegistry = aExtensionRegistry;
        layerSupportRegistry = aLayerSupportRegistry;
        featureSupportRegistry = aFeatureSupportRegistry;
        metrics = aMetrics;
        bratProperties = aBratProperties;
        servletContext = aServletContext;
        documentService = aDocumentService;
        projectService = aProjectService;

        currentProject = projectService.getProject("Annotation Study");
    }

    @Override
    public void initController(User aUser, Project aProject, SourceDocument aSourceDocument, CasProvider aCasProvider) {
        currentProject = aProject;
        currentSourceDocument = aSourceDocument;
        currentUser = aUser;
        casProvider = aCasProvider;
    }


    @Override
    public void getRequest(Event aEvent) {
        //get the request and perform corresponing action
        switch (aEvent.getType()) {
            case (""):
                break;
            default:
        }
    }


    @Override
    public void updateDocumentService(SourceDocument aSourceDocument) {
        currentSourceDocument = aSourceDocument;
    }

    @Override
    public CAS getEditorCasService() throws IOException {
        return getEditorCas();
    }

    @GetMapping("/editor/document/CAS")
    @ResponseBody
    public CAS getEditorCas() throws IOException {
        return documentService.readAnnotationCas(currentSourceDocument,
            currentUser.getUsername());
    }

    @Override
    public void createAnnotationService(CAS aCas, Type aType, int aBegin, int aEnd) {
        /* TESTING only
        servletContext.getContextPath() + BASE_URL
            + CREATE_ANNOTATION_PATH.replace("{projectId}", String.valueOf(currentUsername));

         */
        createAnnotation(aCas, aType, aBegin, aEnd);
    }

    @GetMapping(CREATE_ANNOTATION_PATH)
    @ResponseBody
    public void createAnnotation(CAS aCas, Type aType, int aBegin, int aEnd) {
        aCas.createAnnotation(aType, aBegin, aEnd);
    }

    @Override
    public AnnotationDocument getDocumentService(String aProject, String aDocument) {
        return getDocument(aProject, aDocument);
    }

    @GetMapping(DOCUMENT_PATH)
    @ResponseBody
    public AnnotationDocument getDocument(@RequestParam String aProject, @RequestParam String aDocument) {
        AnnotationDocument doc = documentService.
            getAnnotationDocument(documentService.getSourceDocument(projectService.getProject(aProject), aDocument), "admin");
        return doc;
    }

    //---------------------------------------------//

    private Object actionLookupNormData(AjaxRequestTarget aTarget, IRequestParameters request,
                                        VID paramId)
        throws AnnotationException, IOException {
        NormDataResponse response = new NormDataResponse();

        // We interpret the databaseParam as the feature which we need to look up the feature
        // support
        StringValue databaseParam = request.getParameterValue("database");

        // We interpret the key as the feature value or as a kind of query to be handled by the
        // feature support
        StringValue keyParam = request.getParameterValue("key");

        StringValue layerParam = request.getParameterValue(PARAM_SPAN_TYPE);

        if (layerParam.isEmpty() || databaseParam.isEmpty()) {
            return response;
        }

        String database = databaseParam.toString();
        long layerId = decodeTypeName(layerParam.toString());
        AnnotationLayer layer = annotationService.getLayer(currentProject, layerId)
            .orElseThrow(() -> new AnnotationException("Layer with ID [" + layerId
                + "] does not exist in project [" + currentProject.getName() + "]("
                + currentProject.getId() + ")"));

        // Check where the query needs to be routed: to an editor extension or to a feature support
        if (paramId.isSynthetic()) {
            if (keyParam.isEmpty()) {
                return response;
            }

            AnnotationFeature feature = annotationService.getFeature(database, layer);

            String extensionId = paramId.getExtensionId();
            response.setResults(extensionRegistry.getExtension(extensionId)
                .renderLazyDetails(currentSourceDocument, currentUser, paramId, feature,
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
                    .renderLazyDetails(casProvider.get(), paramId);
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    private void actionOpenContextMenu(AjaxRequestTarget aTarget, IRequestParameters request,
                                       CAS aCas, VID paramId) {
        /* TODO open Context
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

         */
    }

    private Object actionDoAction(AjaxRequestTarget aTarget, IRequestParameters request, CAS aCas,
                                  VID paramId)
        throws AnnotationException, IOException {
        StringValue layerParam = request.getParameterValue(PARAM_SPAN_TYPE);

        if (!layerParam.isEmpty()) {
            long layerId = decodeTypeName(layerParam.toString());
            AnnotationLayer layer = annotationService.getLayer(currentProject, layerId)
                .orElseThrow(() -> new AnnotationException("Layer with ID [" + layerId
                    + "] does not exist in project [" + currentProject + "]("
                    + currentProject.getId() + ")"));
            if (!StringUtils.isEmpty(layer.getOnClickJavascriptAction())) {
                // parse the action
                List<AnnotationFeature> features = annotationService.listSupportedFeatures(layer);
                AnnotationFS anno = selectAnnotationByAddr(aCas, paramId.getId());
                Map<String, Object> functionParams = OnClickActionParser.parse(layer, features,
                    currentSourceDocument, anno);
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
        throws IOException, AnnotationException {
        // This is the span the user has marked in the browser in order to create a new slot-filler
        // annotation OR the span of an existing annotation which the user has selected.
        Offsets optUserSelectedSpan = getOffsetsFromRequest(request, aCas, aSelectedAnnotation);

        Offsets userSelectedSpan = optUserSelectedSpan;

        //TODO AnnotationActionHandler replace with event
        /*
        Selection selection = state.getSelection();

        if (state.isSlotArmed()) {
            // When filling a slot, the current selection is *NOT* changed. The
            // Span annotation which owns the slot that is being filled remains
            // selected!
            getActionHandler().actionFillSlot(aTarget, aCas, userSelectedSpan.getBegin(),
                userSelectedSpan.getEnd(), aSelectedAnnotation);
        } else {
            if (!aSelectedAnnotation.isSynthetic()) {
                selection.selectSpan(aSelectedAnnotation, aCas, userSelectedSpan.getBegin(),
                    userSelectedSpan.getEnd());

                if (selection.getAnnotation().isNotSet()) {
                    // Create new annotation
                    getActionHandler().actionCreateOrUpdate(aTarget, aCas);
                } else {
                    getActionHandler().actionSelect(aTarget);
                }
            }
        }

         */

        return new SpanAnnotationResponse();
    }

    private ArcAnnotationResponse actionArc(AjaxRequestTarget aTarget, IRequestParameters request,
                                            CAS aCas, VID paramId)
        throws IOException, AnnotationException {
        AnnotationFS originFs = selectAnnotationByAddr(aCas,
            request.getParameterValue(PARAM_ORIGIN_SPAN_ID).toInt());
        AnnotationFS targetFs = selectAnnotationByAddr(aCas,
            request.getParameterValue(PARAM_TARGET_SPAN_ID).toInt());

        //TODO AnnotationActionHandler replace with event
        /*
        Selection selection = state.getSelection();
        selection.selectArc(paramId, originFs, targetFs);

        if (selection.getAnnotation().isNotSet()) {
            // Create new annotation
            getActionHandler().actionCreateOrUpdate(aTarget, aCas);
        } else {
            getActionHandler().actionSelect(aTarget);
        }

         */

        return new ArcAnnotationResponse();
    }

    private void actionArcRightClick(AjaxRequestTarget aTarget, VID paramId)
        throws IOException, AnnotationException {

        /* TODO
        if (!getModelObject().getSelection().isSpan()) {
            return;
        }

         */

        CAS cas;
        try {
            cas = casProvider.get();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Currently selected span
        //TODO correct value
        AnnotationFS originFs = selectAnnotationByAddr(cas,
            12); //getModelObject().getSelection().getAnnotation().getId());

        // Target span of the relation
        AnnotationFS targetFs = selectAnnotationByAddr(cas, paramId.getId());

        //TODO AnnotationActionHandler replace with event
        /*
        Selection selection = state.getSelection();
        selection.selectArc(VID.NONE_ID, originFs, targetFs);

        // Create new annotation
        getActionHandler().actionCreateOrUpdate(aTarget, cas);

         */
    }

    private GetCollectionInformationResponse actionGetCollectionInformation() {
        GetCollectionInformationResponse info = new GetCollectionInformationResponse();
        if (currentProject != null) {
            //TODO AnnotationLayers
            /*
            info.setEntityTypes(BratRenderer.buildEntityTypes(currentProject,
                getModelObject().getAnnotationLayers(), annotationService));
            info.getVisualOptions()
                .setArcBundle(getModelObject().getPreferences().isCollapseArcs()
                    ? VisualOptions.ARC_BUNDLE_ALL
                    : VisualOptions.ARC_BUNDLE_NONE);

             */
        }
        return info;
    }

    private String actionGetDocument(CAS aCas) {
        StopWatch timer = new StopWatch();
        timer.start();

        GetDocumentResponse response = new GetDocumentResponse();
        String json;
        if (currentProject != null) {
            render(response, aCas);
            json = toJson(response);
            lastRenderedJson = json;
            lastRenederedJsonParsed = null;
        } else {
            json = toJson(response);
        }

        timer.stop();
        metrics.renderComplete(BratMetrics.RenderType.FULL, timer.getTime(), json, null);
        serverTiming("Brat-JSON", "Brat JSON generation (FULL)", timer.getTime());

        return json;
    }

    /**
     * Extract offset information from the current request. These are either offsets of an existing
     * selected annotations or offsets contained in the request for the creation of a new
     * annotation.
     */
    private Offsets getOffsetsFromRequest(IRequestParameters request, CAS aCas, VID aVid)
        throws IOException {
        if (aVid.isNotSet() || aVid.isSynthetic()) {
            // Create new span annotation - in this case we get the offset information from the
            // request
            String offsets = request.getParameterValue(PARAM_OFFSETS).toString();

            OffsetsList offsetLists = JSONUtil.getObjectMapper().readValue(offsets,
                OffsetsList.class);

            /* TODO offset
            int annotationBegin = getModelObject().getWindowBeginOffset()
                + offsetLists.get(0).getBegin();
            int annotationEnd = getModelObject().getWindowBeginOffset()
                + offsetLists.get(offsetLists.size() - 1).getEnd();
            return new Offsets(annotationBegin, annotationEnd);

             */
            return null;

        } else {
            // Edit existing span annotation - in this case we look up the offsets in the CAS
            // Let's not trust the client in this case.
            AnnotationFS fs = WebAnnoCasUtil.selectAnnotationByAddr(aCas, aVid.getId());
            return new Offsets(fs.getBegin(), fs.getEnd());
        }
    }


    private String getCollection() {
        if (currentProject != null) {
            return "#" + currentProject.getName() + "/";
        } else {
            return "";
        }
    }

    private String toJson(Object result) {
        String json = "[]";
        try {
            if (result instanceof JsonNode) {
                json = JSONUtil.toInterpretableJsonString((JsonNode) result);
            } else {
                json = JSONUtil.toInterpretableJsonString(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }


    private Optional<String> bratRenderCommand(CAS aCas) {

        StopWatch timer = new StopWatch();
        timer.start();

        GetDocumentResponse response = new GetDocumentResponse();
        render(response, aCas);

        ObjectMapper mapper = JSONUtil.getObjectMapper();
        JsonNode current = mapper.valueToTree(response);
        String json = toJson(current);

        // By default, we do a full rendering...
        BratMetrics.RenderType renderType = FULL;
        String cmd = "renderData";
        String responseJson = json;
        JsonNode diff;
        String diffJsonStr = null;

        // Here, we try to balance server CPU load against network load. So if we have a chance
        // of significantly reducing the data sent to the client via a differential update, then
        // we try that. However, if it is pretty obvious that we won't save a lot, then we will
        // not even try. I.e. we apply some heuristics to see if large parts of the editor have
        // changed.

        /*
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
                } else {
                    previous = lastRenderedJson != null ? mapper.readTree(lastRenderedJson) : null;
                }
            } catch (IOException e) {
                LOG.error("Unable to generate diff, falling back to full render.", e);
                // Fall-through
            }

            if (previous != null && current != null) {
                diff = JsonDiff.asJson(previous, current);
                diffJsonStr = diff.toString();

                if (diff instanceof ArrayNode && ((ArrayNode) diff).isEmpty()) {
                    // No difference? Well, don't render at all :)
                    renderType = SKIP;
                } else if (diffJsonStr.length() < json.length()) {
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

         */

        // Storing the last rendered JSON as string because JsonNodes are not serializable.
        lastRenderedJson = json;
        lastRenederedJsonParsed = current;

        timer.stop();

        metrics.renderComplete(renderType, timer.getTime(), json, diffJsonStr);
        serverTiming("Brat-JSON", "Brat-JSON generation (" + renderType + ")", timer.getTime());

        if (SKIP.equals(renderType)) {
            return Optional.empty();
        }

        return Optional.of("Wicket.$('').dispatcher.post('" + cmd + "', ["
            + responseJson + "]);");
    }

    private void render(GetDocumentResponse response, CAS aCas) {
        /* TODO
        VDocument vdoc = render(aCas, aState.getWindowBeginOffset(), aState.getWindowEndOffset());
        BratRenderer renderer = new BratRenderer(annotationService, coloringService);
        renderer.render(response, aState, vdoc, aCas);
         */
    }

    private String bratInitCommand() {

        // REC 2014-10-18 - For a reason that I do not understand, the dispatcher cannot be a local
        // variable. If I put a "var" here, then communication fails with messages such as
        // "action 'openSpanDialog' returned result of action 'loadConf'" in the browsers's JS
        // console.

        StringBuilder js = new StringBuilder();
/*
        js.append("(function() {");
        if (bratProperties.isClientSideTraceLog()) {
            js.append("  console.log('Initializing ()...');");
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

         */

        return js.toString();
    }


    private void handleError(String aMessage, Exception e) {
        //TODO error handling overhaul
        RequestCycle requestCycle = RequestCycle.get();
        /*
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

         */

    }

}
