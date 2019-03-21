/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
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
import com.flipkart.zjsonpatch.JsonDiff;
import com.googlecode.wicket.jquery.ui.settings.JQueryUILibrarySettings;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.Selection;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.BratProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.ArcAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.DoActionResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.LoadConfResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.SpanAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics;
import de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratRenderer;
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

/**
 * Brat annotator component.
 */
public class BratAnnotationEditor
    extends AnnotationEditorBase
{
    private static final Logger LOG = LoggerFactory.getLogger(BratAnnotationEditor.class);
    private static final long serialVersionUID = -1537506294440056609L;

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ARC_ID = "arcId";
    private static final String PARAM_ID = "id";
    private static final String PARAM_OFFSETS = "offsets";
    private static final String PARAM_TARGET_SPAN_ID = "targetSpanId";
    private static final String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    private static final String PARAM_SPAN_TYPE = "type";

    private @SpringBean PreRenderer preRenderer;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean BratMetrics metrics;
    private @SpringBean BratProperties bratProperties;
    
    private WebMarkupContainer vis;
    private AbstractAjaxBehavior requestHandler;
    
    private String lastRenderedJson;

    public BratAnnotationEditor(String id, IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider)
    {
        super(id, aModel, aActionHandler, aCasProvider);
        
        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);
        autoAdd(vis, null);

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
                
                // We always refresh the feedback panel - only doing this in the case were actually
                // something worth reporting occurs is too much of a hassel...
                aTarget.addChildren(getPage(), IFeedback.class);

                final IRequestParameters request = getRequest().getPostParameters();
                
                // Get action from the request
                String action = request.getParameterValue(PARAM_ACTION).toString();
                LOG.debug("AJAX-RPC CALLED: [{}]", action);
                
                // Parse annotation ID if present in request
                VID paramId;
                if (!request.getParameterValue(PARAM_ID).isEmpty()
                        && !request.getParameterValue(PARAM_ARC_ID).isEmpty()) {
                    throw new IllegalStateException(
                            "[id] and [arcId] cannot be both set at the same time!");
                }
                else if (!request.getParameterValue(PARAM_ID).isEmpty()) {
                    paramId = VID.parseOptional(request.getParameterValue(PARAM_ID).toString());
                }
                else {
                    paramId = VID.parseOptional(request.getParameterValue(PARAM_ARC_ID).toString());
                }

                // Load the CAS if necessary
                // Make sure we load the CAS only once here in case of an annotation action.
                boolean requiresCasLoading = SpanAnnotationResponse.is(action)
                        || ArcAnnotationResponse.is(action) || GetDocumentResponse.is(action)
                        || DoActionResponse.is(action);
                CAS jCas = null;
                if (requiresCasLoading) {
                    try {
                        jCas = getJCasProvider().get();
                    }
                    catch (Exception e) {
                        LOG.error("Unable to load data", e);
                        error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                        return;
                    }
                }
                
                Object result = null;
                try {
                    // Whenever an action should be performed, do ONLY perform this action and
                    // nothing else, and only if the item actually is an action item
                    if (DoActionResponse.is(action)) {
                        if (paramId.isSynthetic()) {
                            Offsets offsets = getOffsetsFromRequest(request, jCas, paramId);
                            extensionRegistry.fireAction(getActionHandler(), getModelObject(),
                                    aTarget, jCas, paramId, action, offsets.getBegin(),
                                    offsets.getEnd());
                        }
                        else {
                            actionDoAction(aTarget, request, jCas, paramId);
                        }
                    }
                    else {
                        if (paramId.isSynthetic()) {
                            Offsets offsets = getOffsetsFromRequest(request, jCas, paramId);
                            extensionRegistry.fireAction(getActionHandler(), getModelObject(),
                                    aTarget, jCas, paramId, action, offsets.getBegin(),
                                    offsets.getEnd());
                        }
                        else {
                            // HACK: If an arc was clicked that represents a link feature, then 
                            // open the associated span annotation instead.
                            if (paramId.isSlotSet() && ArcAnnotationResponse.is(action)) {
                                action = SpanAnnotationResponse.COMMAND;
                                paramId = new VID(paramId.getId());
                            }

                            // Doing anything but selecting or creating a span annotation when a
                            // slot is armed will unarm it
                            if (getModelObject().isSlotArmed()
                                    && !SpanAnnotationResponse.is(action)) {
                                getModelObject().clearArmedSlot();
                            }
        
                            if (SpanAnnotationResponse.is(action)) {
                                result = actionSpan(aTarget, request, jCas, paramId);
                            }
                            else if (ArcAnnotationResponse.is(action)) {
                                result = actionArc(aTarget, request, jCas, paramId);
                            }
                            else if (LoadConfResponse.is(action)) {
                                result = new LoadConfResponse(bratProperties);
                            }
                            else if (GetCollectionInformationResponse.is(action)) {
                                result = actionGetCollectionInformation();
                            }
                            else if (GetDocumentResponse.is(action)) {
                                result = actionGetDocument(jCas);
                            }
                        }
                    }
                }
                catch (Exception e) {
                    error("Error: " + e.getMessage());
                    LOG.error("Error: {}", e.getMessage(), e);
                }

                // Serialize updated document to JSON
                if (result == null) {
                    LOG.debug("AJAX-RPC: Action [{}] produced no result!", action);
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
                    aTarget.prependJavaScript("Wicket.$('" + vis.getMarkupId() + "').temp = "
                            + json + ";");
                }
                
                LOG.debug("AJAX-RPC DONE: [{}] completed in {}ms", action,
                        (System.currentTimeMillis() - timerStart));
            }
        };

        add(requestHandler);
    }

    private Object actionDoAction(AjaxRequestTarget aTarget, IRequestParameters request, CAS jCas,
            VID paramId)
        throws IOException
    {
        StringValue layerParam = request.getParameterValue(PARAM_SPAN_TYPE);
        if (!layerParam.isEmpty()) {
            long layerId = Long.parseLong(layerParam.beforeFirst('_'));
            AnnotationLayer layer = annotationService.getLayer(layerId);
            if (!StringUtils.isEmpty(layer.getOnClickJavascriptAction())) {
                // parse the action
                List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);
                AnnotationFS anno = selectAnnotationByAddr(jCas, paramId.getId());
                Map<String, Object> functionParams = OnClickActionParser.parse(layer, features,
                        getModelObject().getDocument(), anno);
                // define anonymous function, fill the body and immediately execute
                String js = String.format("(function ($PARAM){ %s })(%s)",
                        layer.getOnClickJavascriptAction(), JSONUtil.toJsonString(functionParams));
                aTarget.appendJavaScript(js);
            }
        }
        
        return null;
    }
    
    private SpanAnnotationResponse actionSpan(AjaxRequestTarget aTarget, IRequestParameters request,
            CAS jCas, VID paramId)
        throws IOException, AnnotationException
    {
        Offsets offsets = getOffsetsFromRequest(request, jCas, paramId);

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();

        if (state.isSlotArmed()) {
            // When filling a slot, the current selection is *NOT* changed. The
            // Span annotation which owns the slot that is being filled remains
            // selected!
            getActionHandler().actionFillSlot(aTarget, jCas, offsets.getBegin(), offsets.getEnd(),
                    paramId);
        }
        else {
            if (!paramId.isSynthetic()) {
                selection.selectSpan(paramId, jCas, offsets.getBegin(), offsets.getEnd());

                if (selection.getAnnotation().isNotSet()) {
                    // Create new annotation
                    getActionHandler().actionCreateOrUpdate(aTarget, jCas);
                }
                else {
                    getActionHandler().actionSelect(aTarget, jCas);
                }
            }
        }

        return new SpanAnnotationResponse();
    }
    
    private ArcAnnotationResponse actionArc(AjaxRequestTarget aTarget, IRequestParameters request,
            CAS jCas, VID paramId)
        throws IOException, AnnotationException
    {
        AnnotationFS originFs = selectAnnotationByAddr(jCas,
                request.getParameterValue(PARAM_ORIGIN_SPAN_ID).toInt());
        AnnotationFS targetFs = selectAnnotationByAddr(jCas,
                request.getParameterValue(PARAM_TARGET_SPAN_ID).toInt());

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();
        selection.selectArc(paramId, originFs, targetFs);

        if (selection.getAnnotation().isNotSet()) {
            // Create new annotation
            getActionHandler().actionCreateOrUpdate(aTarget, jCas);
        }
        else {
            getActionHandler().actionSelect(aTarget, jCas);
        }

        return new ArcAnnotationResponse();
    }
    
    private GetCollectionInformationResponse actionGetCollectionInformation()
    {
        GetCollectionInformationResponse info = new GetCollectionInformationResponse();
        if (getModelObject().getProject() != null) {
            info.setEntityTypes(BratRenderer
                    .buildEntityTypes(getModelObject().getAnnotationLayers(), annotationService));
        }
        return info;
    }
    
    private String actionGetDocument(CAS jCas)
    {
        StopWatch timer = new StopWatch();
        timer.start();
        
        GetDocumentResponse response = new GetDocumentResponse();
        String json;
        if (getModelObject().getProject() != null) {
            render(response, jCas);
            json = toJson(response);
            lastRenderedJson = json;
        }
        else {
            json = toJson(response);
        }
        
        timer.stop();
        metrics.renderComplete(RenderType.FULL, timer.getTime(), json, null);
        
        return json;
    }
    
    /**
     * Extract offset information from the current request. These are either offsets of an existing
     * selected annotations or offsets contained in the request for the creation of a new
     * annotation.
     */
    private Offsets getOffsetsFromRequest(IRequestParameters request, CAS jCas, VID aVid)
        throws  IOException
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
            AnnotationFS fs = WebAnnoCasUtil.selectAnnotationByAddr(jCas, aVid.getId());
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
        //    JavaScriptHeaderItem.forReference(BratAnnotationLogResourceReference.get()));
        
        // BRAT modules
        aResponse.render(forReference(BratDispatcherResourceReference.get()));
        aResponse.render(forReference(BratAjaxResourceReference.get()));
        aResponse.render(forReference(BratVisualizerResourceReference.get()));
        aResponse.render(forReference(BratVisualizerUiResourceReference.get()));
        aResponse.render(forReference(BratAnnotatorUiResourceReference.get()));
        // aResponse.render(
        //     JavaScriptHeaderItem.forReference(BratUrlMonitorResourceReference.get()));
        
        // If the page is reloaded in the browser and a document was already open, we need
        // to render it. We use the "later" commands here to avoid polluting the Javascript
        // header items with document data and because loading times are not that critical
        // on a reload.
        // We only do this if we are *not* in a partial page reload. The case of a partial
        // page reload is covered in onAfterRender()
        Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (!target.isPresent() && getModelObject().getProject() != null) {
            bratInitRenderLater(aResponse);
        }
    }
    
    @Override
    protected void onAfterRender()
    {
        super.onAfterRender();
        
        // If we are in a partial page request, then trigger re-initialization of the brat 
        // rendering engine here. If we are in a full page reload, this is handled already
        // by renderHead()
        //
        // Mind that using AnnotationEditorBase#requestRender() is a better alternative to
        // adding the editor to the AJAX request because it creates less initialization 
        // overhead (e.g. it doesn't have to send the collection info again and doesn't require
        // a delay).
        RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(target -> {
            try {
                String script = "setTimeout(function() { " +
                        bratInitCommand() +
                        bratLoadCollectionCommand() +
                        // Even with a timeout, brat will try to grab too much space if the view
                        // contains a *very* long annotation which explodes the view (cf. 
                        // https://github.com/webanno/webanno/issues/500) - so as a last resort,
                        // we schedule a delayed rendering. Since this only happens on a document
                        // switch and after closing the preferences dialog, it is kind of
                        // acceptable, although actually a faster document switch would be
                        // desirable.
                        // bratRenderCommand(getJCasProvider().get()) +
                        bratRenderLaterCommand() +
                        "}, 0);";
                target.appendJavaScript(script);
                LOG.debug("Delayed rendering in partial page update...");
            }
            catch (Exception e) {
                LOG.error("Unable to load data", e);
                error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                target.addChildren(getPage(), IFeedback.class);
            }
        });
    }

    private String bratRenderCommand(CAS aCas)
    {
        StopWatch timer = new StopWatch();
        timer.start();
        
        GetDocumentResponse response = new GetDocumentResponse();
        render(response, aCas);
        String json = toJson(response);
        
        // By default, we do a full rendering...
        RenderType renderType = RenderType.FULL;
        String cmd = "renderData";
        String data = json;

        // ... try to render diff
        String diff = null;
        JsonNode current = null;
        JsonNode previous = null;
        try {
            ObjectMapper mapper = JSONUtil.getObjectMapper();
            current = mapper.readTree(json);
            previous = lastRenderedJson != null ? mapper.readTree(lastRenderedJson) : null;
        }
        catch (IOException e) {
            LOG.error("Unable to generate diff, falling back to full render.", e);
            // Fall-through
        }
        
        if (previous != null && current != null) {
            diff = JsonDiff.asJson(previous, current).toString();
 
            // Only sent a patch if it is smaller than sending the full data. E.g. when switching
            // pages, the patch usually ends up being twice as large as the full data.
            if (diff.length() < json.length()) {
                cmd = "renderDataPatch";
                data = diff;
                renderType = RenderType.DIFFERENTIAL;
            }
            
//            LOG.info("Diff:  " + diff);
//            LOG.info("Full: {}   Patch: {}   Diff time: {}", json.length(), diff.length(), timer);
        }
        
        // Storing the last rendered JSON as string because JsonNodes are not serializable.
        lastRenderedJson = json;
        
        timer.stop();

        metrics.renderComplete(renderType, timer.getTime(), json, diff);
        
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('" + cmd + "', [" + data
                + "]);";
    }

    private void render(GetDocumentResponse response, CAS aCas)
    {
        AnnotatorState aState = getModelObject();
        VDocument vdoc = render(aCas, aState.getWindowBeginOffset(), aState.getWindowEndOffset());
        BratRenderer.render(response, aState, vdoc, aCas, annotationService);
    }
    
    private String bratInitCommand()
    {
        // REC 2014-10-18 - For a reason that I do not understand, the dispatcher cannot be a local
        // variable. If I put a "var" here, then communication fails with messages such as
        // "action 'openSpanDialog' returned result of action 'loadConf'" in the browsers's JS
        // console.
        String script = "(function() {" +
            "var dispatcher = new Dispatcher();" +
            // Each visualizer talks to its own Wicket component instance
            "dispatcher.ajaxUrl = '" + requestHandler.getCallbackUrl() + "'; " +
            // We attach the JSON send back from the server to this HTML element
            // because we cannot directly pass it from Wicket to the caller in ajax.js.
            "dispatcher.wicketId = '" + vis.getMarkupId() + "'; " +
            "var ajax = new Ajax(dispatcher);" +
            "var visualizer = new Visualizer(dispatcher, '" + vis.getMarkupId() + "');" +
            "var visualizerUI = new VisualizerUI(dispatcher, visualizer.svg);" +
            "var annotatorUI = new AnnotatorUI(dispatcher, visualizer.svg);" +
            //script.append("var logger = new AnnotationLog(dispatcher);");
            "dispatcher.post('init');" +
            "Wicket.$('" + vis.getMarkupId() + "').dispatcher = dispatcher;" +
            "Wicket.$('" + vis.getMarkupId() + "').visualizer = visualizer;" +
            "})();";
        return script;
    }
    
    private String bratLoadCollectionCommand()
    {
        GetCollectionInformationResponse response = new GetCollectionInformationResponse();
        response.setEntityTypes(BratRenderer
                .buildEntityTypes(getModelObject().getAnnotationLayers(), annotationService));
        String json = toJson(response);
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('collectionLoaded', [" + json
                + "]);";
    }
    
//    /**
//     * This triggers the loading of the metadata (colors, types, etc.)
//     *
//     * @return the init script.
//     */
//    private String bratLoadCollectionLaterCommand()
//    {
//        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('ajax', "
//                + "[{action: 'getCollectionInformation',collection: '" + getCollection()
//                + "'}, 'collectionLoaded', {collection: '" + getCollection() + "',keep: true}]);";
//    }

    /**
     * This one triggers the loading of the actual document data
     *
     * @return brat
     */
    private String bratRenderLaterCommand()
    {
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('current', " + "['"
                + getCollection() + "', '1234', {}, true]);";
    }

    /**
     * Reload {@link BratAnnotationEditor} when the Correction/Curation page is opened
     *
     * @param aResponse
     *            the response.
     */
    private void bratInitRenderLater(IHeaderResponse aResponse)
    {
        // Must be OnDomReader so that this is rendered before all other Javascript that is
        // appended to the same AJAX request which turns the annotator visible after a document
        // has been chosen.
//        aResponse.render(OnDomReadyHeaderItem.forScript(bratInitCommand()));
//        aResponse.render(OnLoadHeaderItem.forScript(bratLoadCollectionLaterCommand()));
//        aResponse.render(OnLoadHeaderItem.forScript(bratRenderLaterCommand()));
        String script = "setTimeout(function() { " +
                bratInitCommand() +
                bratLoadCollectionCommand() +
                bratRenderLaterCommand() +
                "}, 0);";
        aResponse.render(OnDomReadyHeaderItem.forScript(script));
        
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        try {
            aTarget.appendJavaScript("setTimeout(function() { "
                    + bratRenderCommand(getJCasProvider().get()) + " }, 0);");
        }
        catch (IOException e) {
            LOG.error("Unable to load data", e);
            error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
            aTarget.addChildren(getPage(), IFeedback.class);
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
            json = JSONUtil.toInterpretableJsonString(result);
        }
        catch (IOException e) {
            error("Unable to produce JSON response " + ":" + ExceptionUtils.getRootCauseMessage(e));
        }
        return json;
    }    
}
