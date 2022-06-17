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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratRequestUtils.getActionFromRequest;
import static de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratRequestUtils.getVidFromRequest;
import static de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType.DIFFERENTIAL;
import static de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType.FULL;
import static de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType.SKIP;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.googlecode.wicket.jquery.ui.widget.menu.IMenuItem;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.Selection;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorProperties;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.LoadConfResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.VisualOptions;
import de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics;
import de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics.RenderType;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratSerializer;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratCssReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.schema.BratSchemaGenerator;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.ServerTimingWatch;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerBase;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerExtensionPoint;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;

/**
 * Brat annotator component.
 */
public class BratAnnotationEditor
    extends AnnotationEditorBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = -1537506294440056609L;

    private final ContextMenu contextMenu;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean BratMetrics metrics;
    private @SpringBean BratAnnotationEditorProperties bratProperties;
    private @SpringBean EditorAjaxRequestHandlerExtensionPoint handlers;
    private @SpringBean BratSerializer bratSerializer;
    private @SpringBean BratSchemaGenerator bratSchemaGenerator;

    private WebMarkupContainer vis;
    private AbstractAjaxBehavior requestHandler;

    private transient JsonNode lastRenderedJsonParsed;
    private String lastRenderedJson;
    private int lastRenderedWindowStart = -1;

    private GetCollectionInformationHandler collectionInformationHandler = new GetCollectionInformationHandler();
    private ShowContextMenuHandler contextMenuHandler = new ShowContextMenuHandler();
    private LoadConfHandler loadConfHandler = new LoadConfHandler();

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

                Object result = null;
                try {
                    result = handleRequest(aTarget);
                }
                catch (Exception e) {
                    handleError("Error: " + getRootCauseMessage(e), e);
                }

                // Serialize updated document to JSON
                if (result != null) {
                    try {
                        BratRequestUtils.attachResponse(aTarget, vis, result);
                    }
                    catch (IOException e) {
                        handleError("Unable to produce JSON response", e);
                    }
                }
            }

            private Object handleRequest(AjaxRequestTarget aTarget) throws IOException
            {
                IRequestParameters requestParameters = getRequest().getPostParameters();
                String action = getActionFromRequest(requestParameters);

                try (var watch = new ServerTimingWatch("brat", "brat (" + action + ")")) {
                    if (loadConfHandler.accepts(getRequest())) {
                        return loadConfHandler.handle(aTarget, getRequest());
                    }

                    if (collectionInformationHandler.accepts(getRequest())) {
                        return collectionInformationHandler.handle(aTarget, getRequest());
                    }

                    if (GetDocumentResponse.is(action)) {
                        return actionGetDocument();
                    }

                    // FIXME Should we un-arm the active slot when the context menu is opened?
                    final VID paramId = getVidFromRequest(requestParameters);
                    if (contextMenuHandler.accepts(getRequest()) && !paramId.isSlotSet()) {
                        return contextMenuHandler.handle(aTarget, getRequest());
                    }

                    return handlers.getHandler(getRequest()) //
                            .map(handler -> handler.handle(aTarget, getRequest())) //
                            .orElse(null);
                }
            }
        };

        add(requestHandler);
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

    private String actionGetDocument() throws IOException
    {
        if (getModelObject().getProject() == null) {
            return toJson(new GetDocumentResponse());
        }

        try (var watch = new ServerTimingWatch("brat-json", "brat JSON generation (FULL)")) {
            final CAS cas = getCasProvider().get();

            String json = toJson(render(cas));
            lastRenderedJson = json;
            lastRenderedJsonParsed = null;

            metrics.renderComplete(RenderType.FULL, watch.stop(), json, null);
            return json;
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
        aResponse.render(CssHeaderItem.forReference(BratCssReference.get()));
        aResponse.render(JavaScriptReferenceHeaderItem.forReference(BratResourceReference.get()));

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

        try (var watch = new ServerTimingWatch("brat-json")) {
            GetDocumentResponse response = render(aCas);

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
                    if (lastRenderedJsonParsed != null) {
                        previous = lastRenderedJsonParsed;
                    }
                    else {
                        previous = lastRenderedJson != null ? mapper.readTree(lastRenderedJson)
                                : null;
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
                        // switching pages, the patch usually ends up being twice as large as the
                        // full
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
            lastRenderedJsonParsed = current;
            lastRenderedWindowStart = aState.getWindowBeginOffset();

            watch.setDescription("Brat-JSON generation (" + renderType + ")");
            metrics.renderComplete(renderType, watch.stop(), json, diffJsonStr);

            if (SKIP.equals(renderType)) {
                return Optional.empty();
            }

            return Optional.of("Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('" + cmd
                    + "', [" + responseJson + "]);");
        }
    }

    private GetDocumentResponse render(CAS aCas)
    {
        AnnotatorState aState = getModelObject();
        return render(aCas, aState.getWindowBeginOffset(), aState.getWindowEndOffset(),
                bratSerializer);
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
        js.append("  Brat('" + vis.getMarkupId() + "', '" + requestHandler.getCallbackUrl() + "')");
        js.append("})();");

        return js.toString();
    }

    private String bratLoadCollectionCommand()
    {
        LOG.trace("[{}][{}] bratLoadCollectionCommand", getMarkupId(), vis.getMarkupId());

        GetCollectionInformationResponse response = collectionInformationHandler
                .getCollectionInformation();
        StringBuilder js = new StringBuilder();
        if (bratProperties.isClientSideTraceLog()) {
            js.append("console.log('Loading collection (" + vis.getMarkupId() + ")...');");
        }
        js.append("Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('collectionLoaded', ["
                + toJson(response) + "]);");
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

        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('current', [{}, true]);";
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
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

    private class LoadConfHandler
        extends EditorAjaxRequestHandlerBase
    {

        @Override
        public String getCommand()
        {
            return LoadConfResponse.COMMAND;
        }

        @Override
        public AjaxResponse handle(AjaxRequestTarget aTarget, Request aRequest)
        {
            return new LoadConfResponse(bratProperties);
        }
    }

    private class GetCollectionInformationHandler
        extends EditorAjaxRequestHandlerBase
    {
        @Override
        public String getCommand()
        {
            return GetCollectionInformationResponse.COMMAND;
        }

        @Override
        public AjaxResponse handle(AjaxRequestTarget aTarget, Request aRequest)
        {
            return getCollectionInformation();
        }

        public GetCollectionInformationResponse getCollectionInformation()
        {
            GetCollectionInformationResponse info = new GetCollectionInformationResponse();
            if (getModelObject().getProject() != null) {
                info.setEntityTypes(bratSchemaGenerator.buildEntityTypes(
                        getModelObject().getProject(), getModelObject().getAnnotationLayers()));
                info.getVisualOptions()
                        .setArcBundle(getModelObject().getPreferences().isCollapseArcs()
                                ? VisualOptions.ARC_BUNDLE_ALL
                                : VisualOptions.ARC_BUNDLE_NONE);
            }
            return info;
        }
    }

    private class ShowContextMenuHandler
        extends EditorAjaxRequestHandlerBase
    {
        @Override
        public String getCommand()
        {
            return ACTION_CONTEXT_MENU;
        }

        @Override
        public AjaxResponse handle(AjaxRequestTarget aTarget, Request aRequest)
        {
            VID vid = VID.parseOptional(
                    aRequest.getRequestParameters().getParameterValue(PARAM_ID).toOptionalString());

            if (vid.isNotSet() || vid.isSynthetic()) {
                return new DefaultAjaxResponse(getAction(aRequest));
            }

            try {
                List<IMenuItem> items = contextMenu.getItemList();
                items.clear();

                if (getModelObject().getSelection().isSpan()) {
                    items.add(new LambdaMenuItem("Link to ...",
                            _target -> actionArcRightClick(_target, vid)));
                }

                extensionRegistry.generateContextMenuItems(items);

                if (!items.isEmpty()) {
                    contextMenu.onOpen(aTarget);
                }
            }
            catch (Exception e) {
                handleError("Unable to load data", e);
            }

            return new DefaultAjaxResponse(getAction(aRequest));
        }

    }
}
