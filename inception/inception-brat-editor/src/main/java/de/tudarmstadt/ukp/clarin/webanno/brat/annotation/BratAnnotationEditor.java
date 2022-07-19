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
import static de.tudarmstadt.ukp.clarin.webanno.brat.annotation.RenderType.FULL;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Request;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratSerializer;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratCssReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.schema.BratSchemaGenerator;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
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
    private static final String BRAT_EVENT_COLLECTION_LOADED = "collectionLoaded";
    private static final String BRAT_EVENT_LOAD_ANNOTATIONS = "loadAnnotations";
    private static final String BRAT_EVENT_RENDER_DATA_PATCH = "renderDataPatch";
    private static final String BRAT_EVENT_RENDER_DATA = "renderData";

    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = -1537506294440056609L;

    private final ContextMenu contextMenu;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean BratMetrics metrics;
    private @SpringBean BratAnnotationEditorProperties bratProperties;
    private @SpringBean EditorAjaxRequestHandlerExtensionPoint handlers;
    private @SpringBean BratSerializer bratSerializer;
    private @SpringBean BratSchemaGenerator bratSchemaGenerator;

    private DiamAjaxBehavior diamBehavior;
    private WebMarkupContainer vis;

    private GetCollectionInformationHandler collectionInformationHandler;

    private DifferentialRenderingSupport diffRenderSupport;

    public BratAnnotationEditor(String id, IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider)
    {
        super(id, aModel, aActionHandler, aCasProvider);

        add(visibleWhen(getModel().map(AnnotatorState::getProject).isPresent()));

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);
        add(vis);

        LOG.trace("[{}][{}] BratAnnotationEditor", getMarkupId(), vis.getMarkupId());

        contextMenu = new ContextMenu("contextMenu");
        add(contextMenu);

        diffRenderSupport = new DifferentialRenderingSupport(metrics);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        collectionInformationHandler = new GetCollectionInformationHandler(vis,
                bratSchemaGenerator);

        diamBehavior = new DiamAjaxBehavior();
        diamBehavior.addPriorityHandler(new LoadConfHandler(vis, bratProperties));
        diamBehavior.addPriorityHandler(collectionInformationHandler);
        diamBehavior.addPriorityHandler(new GetDocumentHandler(vis, diffRenderSupport));
        diamBehavior.addPriorityHandler(new ShowContextMenuHandler());
        add(diamBehavior);
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

    private String bratDispatcherPost(String cmd, String responseJson)
    {
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('" + cmd + "', ["
                + responseJson + "]);";
    }

    private GetDocumentResponse render(CAS aCas)
    {
        LOG.trace("[{}][{}] render", getMarkupId(), vis.getMarkupId());
        AnnotatorState aState = getModelObject();
        return render(aCas, aState.getWindowBeginOffset(), aState.getWindowEndOffset(),
                bratSerializer);
    }

    private String bratInitCommand()
    {
        LOG.trace("[{}][{}] bratInitCommand", getMarkupId(), vis.getMarkupId());

        StringBuilder js = new StringBuilder();
        js.append("(function() {");
        js.append("  Brat('" + vis.getMarkupId() + "', '" + diamBehavior.getCallbackUrl() + "')");
        js.append("})();");
        return js.toString();
    }

    private String bratLoadCollectionCommand()
    {
        LOG.trace("[{}][{}] bratLoadCollectionCommand", getMarkupId(), vis.getMarkupId());
        var collInfo = collectionInformationHandler.getCollectionInformation(getModelObject());
        return bratDispatcherPost(BRAT_EVENT_COLLECTION_LOADED, toJson(collInfo));
    }

    /**
     * This one triggers the loading of the actual document data
     *
     * @return brat
     */
    private String bratRenderLaterCommand()
    {
        LOG.trace("[{}][{}] bratRenderLaterCommand", getMarkupId(), vis.getMarkupId());
        return bratDispatcherPost(BRAT_EVENT_LOAD_ANNOTATIONS, "[]");
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        LOG.trace("[{}][{}] render (AJAX)", getMarkupId(), vis.getMarkupId());

        try {
            var bratDocModel = render(getCasProvider().get());
            diffRenderSupport.differentialRendering(bratDocModel).ifPresent(rr -> {
                StringBuilder js = new StringBuilder();

                if (bratProperties.isClientSideProfiling()) {
                    js.append("Util.profileEnable(true);");
                    js.append("Util.profileClear();");
                }

                js.append(bratDispatcherPost( //
                        rr.getRenderType() == FULL //
                                ? BRAT_EVENT_RENDER_DATA //
                                : BRAT_EVENT_RENDER_DATA_PATCH, //
                        rr.getJsonStr()));

                if (bratProperties.isClientSideProfiling()) {
                    js.append("Util.profileReport();");
                }

                aTarget.appendJavaScript(js);
            });
        }
        catch (IOException e) {
            handleError("Unable to load data", e);
        }
        catch (Exception e) {
            handleError("Unable to render document", e);
        }
    }

    private String toJson(Object result)
    {
        String json = "[]";
        try {
            json = JSONUtil.toInterpretableJsonString(result);
        }
        catch (IOException e) {
            handleError("Unable to produce JSON response", e);
        }
        return json;
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

    private class GetDocumentHandler
        extends EditorAjaxRequestHandlerBase
        implements Serializable
    {

        private static final long serialVersionUID = 1601968431851817445L;

        private final Component vis;
        private final DifferentialRenderingSupport diffRenderSupport;

        public GetDocumentHandler(Component aVis, DifferentialRenderingSupport aDiffRenderSupport)
        {
            vis = aVis;
            diffRenderSupport = aDiffRenderSupport;
        }

        @Override
        public String getCommand()
        {
            return GetDocumentResponse.COMMAND;
        }

        @Override
        public AjaxResponse handle(AjaxRequestTarget aTarget, Request aRequest)
        {
            try {
                var cas = getCasProvider().get();
                var response = render(cas);
                var result = diffRenderSupport.fullRendering(response).getJsonStr();
                BratRequestUtils.attachResponse(aTarget, vis, result);
                return new DefaultAjaxResponse(getAction(aRequest));
            }
            catch (Exception e) {
                return handleError("Unable to load annotations", e);
            }
        }
    }

    private class ShowContextMenuHandler
        extends EditorAjaxRequestHandlerBase
        implements Serializable
    {
        private static final long serialVersionUID = 2566256640285857435L;

        @Override
        public String getCommand()
        {
            return ACTION_CONTEXT_MENU;
        }

        @Override
        public boolean accepts(Request aRequest)
        {
            final VID paramId = getVid(aRequest);
            return super.accepts(aRequest) && paramId.isSet() && !paramId.isSynthetic()
                    && !paramId.isSlotSet();
        }

        @Override
        public AjaxResponse handle(AjaxRequestTarget aTarget, Request aRequest)
        {
            try {
                List<IMenuItem> items = contextMenu.getItemList();
                items.clear();

                if (getModelObject().getSelection().isSpan()) {
                    VID vid = getVid(aRequest);
                    items.add(new LambdaMenuItem("Link to ...",
                            _target -> actionArcRightClick(_target, vid)));
                }

                extensionRegistry.generateContextMenuItems(items);

                if (!items.isEmpty()) {
                    contextMenu.onOpen(aTarget);
                }
            }
            catch (Exception e) {
                handleError("Unable to populate context menu", e);
            }

            return new DefaultAjaxResponse(getAction(aRequest));
        }
    }
}
