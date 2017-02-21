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

import java.io.IOException;
import java.util.Locale;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.head.CssContentHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import com.googlecode.wicket.jquery.ui.resource.JQueryUIResourceReference;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.ArcAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.LoadConfResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.SpanAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.WhoamiResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratRenderer;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAjaxResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAnnotatorUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratConfigurationResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratDispatcherResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratUtilResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratVisualizerResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratVisualizerUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQueryJsonResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQuerySvgDomResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQuerySvgResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

/**
 * Brat annotator component.
 *
 */
public class BratAnnotator
    extends Panel
{
    private static final Log LOG = LogFactory.getLog(BratAnnotator.class);
    private static final long serialVersionUID = -1537506294440056609L;

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ARC_ID = "arcId";
    private static final String PARAM_ID = "id";
    private static final String PARAM_OFFSETS = "offsets";
    private static final String PARAM_TARGET_SPAN_ID = "targetSpanId";
    private static final String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    private static final String PARAM_TARGET_TYPE = "targetType";
    private static final String PARAM_ORIGIN_TYPE = "originType";

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private WebMarkupContainer vis;
    private AbstractAjaxBehavior requestHandler;
    private AnnotationActionHandler detailPanel;

    @Deprecated
    public AnnotatorStateImpl getModelObject()
    {
        return detailPanel.getModelObject();
    }

    public BratAnnotator(String id, IModel<AnnotatorStateImpl> aModel,
            final AnnotationActionHandler aEditor)
    {
        super(id, aModel);
        this.detailPanel = aEditor;
        // Allow AJAX updates.
        setOutputMarkupId(true);

        // The annotator is invisible when no document has been selected. Make sure that we can
        // make it visible via AJAX once the document has been selected.
        setOutputMarkupPlaceholderTag(true);

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);
        add(vis);

        requestHandler = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
                long timerStart = System.currentTimeMillis();
                
                // We always refresh the feedback panel - only doing this in the case were actually
                // something worth reporting occurs is too much of a hassel...
                aTarget.addChildren(getPage(), FeedbackPanel.class);

                final IRequestParameters request = getRequest().getPostParameters();
                
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
                
                // Get action from the request
                String action = request.getParameterValue(PARAM_ACTION).toString();
                
                // Record the action in the action context (which currently is persistent...)
                getModelObject().setUserAction(action);
                
                // Ensure that the user action is cleared *AFTER* rendering so that for AJAX
                // calls that do not go through this AjaxBehavior do not see an active user action.
                RequestCycle.get().getListeners().add(new AbstractRequestCycleListener() {
                    @Override
                    public void onEndRequest(RequestCycle aCycle)
                    {
                        BratAnnotator.this.getModelObject().clearUserAction();
                    }
                });

                // Load the CAS if necessary
                // Make sure we load the CAS only once here in case of an annotation action.
                boolean requiresCasLoading = SpanAnnotationResponse.is(action)
                        || ArcAnnotationResponse.is(action) || GetDocumentResponse.is(action);
                JCas jCas = null;
                if (requiresCasLoading) {
                    try {
                        jCas = getCas(getModelObject());
                    }
                    catch (Exception e) {
                        LOG.error("Unable to load data", e);
                        error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                        return;
                    }
                }

                // HACK: If an arc was clicked that represents a link feature, then open the
                // associated span annotation instead.
                if (paramId.isSlotSet() && ArcAnnotationResponse.is(action)) {
                    action = SpanAnnotationResponse.COMMAND;
                    paramId = new VID(paramId.getId());
                }

                // Doing anything but a span annotation when a slot is armed will unarm it
                if (getModelObject().isSlotArmed() && !SpanAnnotationResponse.is(action)) {
                    getModelObject().clearArmedSlot();
                }

                Object result = null;
                try {
                    LOG.info("AJAX-RPC CALLED: [" + action + "]");

                    if (WhoamiResponse.is(action)) {
                        result = new WhoamiResponse(
                                SecurityContextHolder.getContext().getAuthentication().getName());
                    }
                    else if (SpanAnnotationResponse.is(action)) {
                        Offsets offsets = getOffsetsFromRequest(request, jCas, paramId);
                        detailPanel.actionSpanAnnotation(aTarget, jCas, offsets.getBegin(),
                                offsets.getEnd(), paramId);
                        result = new SpanAnnotationResponse();
                    }
                    else if (ArcAnnotationResponse.is(action)) {
                        String originType = request.getParameterValue(PARAM_ORIGIN_TYPE).toString();
                        int originSpanId = request.getParameterValue(PARAM_ORIGIN_SPAN_ID).toInt();
                        String targetType = request.getParameterValue(PARAM_TARGET_TYPE).toString();
                        int targetSpanId = request.getParameterValue(PARAM_TARGET_SPAN_ID).toInt();
                        detailPanel.actionArcAnnotation(aTarget, jCas, paramId, originType,
                                originSpanId, targetType, targetSpanId);
                        result = new ArcAnnotationResponse();
                    }
                    else if (LoadConfResponse.is(action)) {
                        result = new LoadConfResponse();
                    }
                    else if (GetCollectionInformationResponse.is(action)) {
                        GetCollectionInformationResponse info = new GetCollectionInformationResponse();
                        if (getModelObject().getProject() != null) {
                            info.setEntityTypes(BratRenderer.buildEntityTypes(
                                    getModelObject().getAnnotationLayers(), annotationService));
                        }
                        result = info;
                    }
                    else if (GetDocumentResponse.is(action)) {
                        GetDocumentResponse response = new GetDocumentResponse();
                        if (getModelObject().getProject() != null) {
                            BratRenderer.render(response, getModelObject(), jCas, annotationService);
                        }
                        result = response;
                    }

                }
                catch (ClassNotFoundException e) {
                    LOG.error("Invalid reader: " + e.getMessage(), e);
                    error("Invalid reader: " + e.getMessage());
                }
                catch (Exception e) {
                    error("Error: " + e.getMessage());
                    LOG.error(e);
                }

                // Serialize updated document to JSON
                if (result == null) {
                    LOG.warn("AJAX-RPC: Action [" + action + "] produced no result!");
                }
                else {
                    String json = toJson(result);
                    // Since we cannot pass the JSON directly to Brat, we attach it to the HTML
                    // element into which BRAT renders the SVG. In our modified ajax.js, we pick it
                    // up from there and then pass it on to BRAT to do the rendering.
                    aTarget.prependJavaScript("Wicket.$('" + vis.getMarkupId() + "').temp = "
                            + json + ";");
                }
                
                // If we created a new annotation, then refresh the available annotation layers
                // in the detail panel.
                if (getModelObject().getSelection().getAnnotation().isNotSet()) {
                    detailPanel.refreshAnnotationLayers(getModelObject());
                }
                
                LOG.info("AJAX-RPC DONE: [" + action + "] completed in "
                        + (System.currentTimeMillis() - timerStart) + "ms");
            }
        };

        add(requestHandler);
    }

    /**
     * Extract offset information from the current request. These are either offsets of an existing
     * selected annotations or offsets contained in the request for the creation of a new
     * annotation.
     */
    private Offsets getOffsetsFromRequest(IRequestParameters request, JCas jCas, VID aVid)
        throws  IOException
    {
        if (aVid.isNotSet()) {
            // Create new span annotation - in this case we get the offset information from the
            // request
            String offsets = request.getParameterValue(PARAM_OFFSETS).toString();
            OffsetsList offsetLists = JSONUtil.getJsonConverter().getObjectMapper()
                    .readValue(offsets, OffsetsList.class);

            int annotationBegin = getModelObject().getWindowBeginOffset()
                    + offsetLists.get(0).getBegin();
            int annotationEnd = getModelObject().getWindowBeginOffset()
                    + offsetLists.get(offsetLists.size() - 1).getEnd();
            return new Offsets(annotationBegin, annotationEnd);
        }
        else {
            // Edit existing span annotation - in this case we look up the offsets in the CAS
            // Let's not trust the client in this case.
            AnnotationFS fs = WebAnnoCasUtil.selectByAddr(jCas, aVid.getId());
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

        double textFontSize = getModelObject().getPreferences().getFontSize();
        double spanFontSize = 10 * (textFontSize / (float) AnnotationPreference.FONT_SIZE_DEFAULT);
        double arcFontSize = 9 * (textFontSize / (float) AnnotationPreference.FONT_SIZE_DEFAULT);
        
        aResponse.render(CssContentHeaderItem.forCSS(String.format(Locale.US,
                ".span text { font-size: %.1fpx; }\n" +
                ".arcs text { font-size: %.1fpx; }\n" +
                "text { font-size: %.1fpx; }\n", 
                spanFontSize, arcFontSize, textFontSize), 
                "brat-font"));
        
        // Libraries
        aResponse.render(JavaScriptHeaderItem.forReference(JQueryUIResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQuerySvgResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQuerySvgDomResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQueryJsonResourceReference.get()));

        // BRAT helpers
        aResponse.render(JavaScriptHeaderItem.forReference(BratConfigurationResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratUtilResourceReference.get()));
        //aResponse.render(JavaScriptHeaderItem.forReference(BratAnnotationLogResourceReference.get()));

        // BRAT modules
        aResponse.render(JavaScriptHeaderItem.forReference(BratDispatcherResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAjaxResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratVisualizerResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratVisualizerUiResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAnnotatorUiResourceReference.get()));
        //aResponse.render(JavaScriptHeaderItem.forReference(BratUrlMonitorResourceReference.get()));

        StringBuilder script = new StringBuilder();
        // REC 2014-10-18 - For a reason that I do not understand, the dispatcher cannot be a local
        // variable. If I put a "var" here, then communication fails with messages such as
        // "action 'openSpanDialog' returned result of action 'loadConf'" in the browsers's JS
        // console.
        script.append("(function() {");
        script.append("var dispatcher = new Dispatcher();");
        // Each visualizer talks to its own Wicket component instance
        script.append("dispatcher.ajaxUrl = '" + requestHandler.getCallbackUrl() + "'; ");
        // We attach the JSON send back from the server to this HTML element
        // because we cannot directly pass it from Wicket to the caller in ajax.js.
        script.append("dispatcher.wicketId = '" + vis.getMarkupId() + "'; ");
        script.append("var ajax = new Ajax(dispatcher);");
        script.append("var visualizer = new Visualizer(dispatcher, '" + vis.getMarkupId() + "');");
        script.append("var visualizerUI = new VisualizerUI(dispatcher, visualizer.svg);");
        script.append("var annotatorUI = new AnnotatorUI(dispatcher, visualizer.svg);");
        //script.append("var logger = new AnnotationLog(dispatcher);");
        script.append("dispatcher.post('init');");
        script.append("Wicket.$('" + vis.getMarkupId() + "').dispatcher = dispatcher;");
        script.append("Wicket.$('" + vis.getMarkupId() + "').visualizer = visualizer;");
        script.append("})();");

        // Must be OnDomReader so that this is rendered before all other Javascript that is
        // appended to the same AJAX request which turns the annotator visible after a document
        // has been chosen.
        aResponse.render(OnDomReadyHeaderItem.forScript(script.toString()));
        
        // If the page is reloaded in the browser and a document was already open, we need
        // to render it. We use the "later" commands here to avoid polluting the Javascript
        // header items with document data and because loading times are not that critical
        // on a reload.
        if (getModelObject().getProject() != null) {
            bratInitRenderLater(aResponse);
        }
    }

//    private String bratInitCommand()
//    {
//        GetCollectionInformationResponse response = new GetCollectionInformationResponse();
//        response.setEntityTypes(BratRenderer.buildEntityTypes(getModelObject()
//                .getAnnotationLayers(), annotationService));
//        String json = toJson(response);
//        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('collectionLoaded', [" + json
//                + "]);";
//    }
    
//    public void bratInit(AjaxRequestTarget aTarget)
//    {
//        aTarget.appendJavaScript(bratInitCommand());
//    }

    private String bratRenderCommand(JCas aJCas)
    {
        LOG.info("BEGIN bratRenderCommand");
        GetDocumentResponse response = new GetDocumentResponse();
        BratRenderer.render(response, getModelObject(), aJCas, annotationService);
        String json = toJson(response);
        LOG.info("END bratRenderCommand");
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('renderData', [" + json
                + "]);";
    }

    /**
     * This triggers the loading of the metadata (colors, types, etc.)
     *
     * @return the init script.
     */
    private String bratInitLaterCommand()
    {
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('ajax', "
                + "[{action: 'getCollectionInformation',collection: '" + getCollection()
                + "'}, 'collectionLoaded', {collection: '" + getCollection() + "',keep: true}]);";
    }

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
     * Reload {@link BratAnnotator} when the Correction/Curation page is opened
     *
     * @param aResponse
     *            the response.
     */
    private void bratInitRenderLater(IHeaderResponse aResponse)
    {
        aResponse.render(OnLoadHeaderItem.forScript(bratInitLaterCommand()));
        aResponse.render(OnLoadHeaderItem.forScript(bratRenderLaterCommand()));
    }

    /**
     * Render content in a separate request.
     *
     * @param aTarget
     *            the AJAX target.
     */
    public void bratRenderLater(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(bratRenderLaterCommand());
    }

    /**
     * Render content as part of the current request.
     *
     * @param aTarget
     *            the AJAX target.
     * @param aJCas
     *            the CAS to render.
     */
    public void bratRender(AjaxRequestTarget aTarget, JCas aJCas)
    {
        aTarget.appendJavaScript(bratRenderCommand(aJCas));
    }
    
    /**
     * Render content as part of the current request.
     *
     * @param aTarget
     *            the AJAX target.
     * @param aAnnotationId
     *            the annotation ID to highlight.
     */
    public void bratSetHighlight(AjaxRequestTarget aTarget, VID aAnnotationId)
    {
        if (!aAnnotationId.isSet()) {
            aTarget.appendJavaScript("Wicket.$('" + vis.getMarkupId()
                    + "').dispatcher.post('current', " + "['" + getCollection()
                    + "', '1234', {edited:[]}, false]);");
        }
        else {
            aTarget.appendJavaScript("Wicket.$('" + vis.getMarkupId()
                    + "').dispatcher.post('current', " + "['" + getCollection()
                    + "', '1234', {edited:[[\"" + aAnnotationId + "\"]]}, false]);");
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

    private JCas getCas(AnnotatorStateImpl aBratAnnotatorModel)
        throws UIMAException, IOException, ClassNotFoundException
    {
        if (aBratAnnotatorModel.getMode().equals(Mode.ANNOTATION)
                || aBratAnnotatorModel.getMode().equals(Mode.AUTOMATION)
                || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)
                || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION_MERGE)) {

            return repository.readAnnotationCas(aBratAnnotatorModel.getDocument(),
                    aBratAnnotatorModel.getUser());
        }
        else {
            return repository.readCurationCas(aBratAnnotatorModel.getDocument());
        }
    }    
}
