/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import static java.util.Arrays.asList;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import com.googlecode.wicket.jquery.ui.resource.JQueryUIResourceReference;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.ArcAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.LoadConfResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.SpanAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.WhoamiResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAjaxResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAnnotationLogResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAnnotatorUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratConfigurationResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratDispatcherResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratUrlMonitorResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratUtilResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratVisualizerResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratVisualizerUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQueryJsonResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQuerySvgDomResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQuerySvgResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.WebfontResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Brat annotator component.
 *
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 * @author Andreas Straninger
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
    private static final String PARAM_SPAN_TEXT = "spanText";
    private static final String PARAM_TARGET_SPAN_ID = "targetSpanId";
    private static final String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    private static final String PARAM_TARGET_TYPE = "targetType";
    private static final String PARAM_ORIGIN_TYPE = "originType";

    private static final String GHOST_PLACE_HOLDER = "###";
    private static final String GHOST_COLOR = "orange";

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private WebMarkupContainer vis;
    private AbstractAjaxBehavior controller;
    private String collection = "";

    /**
     * Data models for {@link BratAnnotator}
     *
     * @param aModel
     *            the model.
     */
    public void setModel(IModel<BratAnnotatorModel> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(BratAnnotatorModel aModel)
    {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<BratAnnotatorModel> getModel()
    {
        return (IModel<BratAnnotatorModel>) getDefaultModel();
    }

    public BratAnnotatorModel getModelObject()
    {
        return (BratAnnotatorModel) getDefaultModelObject();
    }

    public BratAnnotator(String id, IModel<BratAnnotatorModel> aModel,
            final AnnotationDetailEditorPanel aAnnotationDetailEditorPanel)
    {
        super(id, aModel);

        // Allow AJAX updates.
        setOutputMarkupId(true);

        // The annotator is invisible when no document has been selected. Make sure that we can
        // make it visible via AJAX once the document has been selected.
        setOutputMarkupPlaceholderTag(true);

        if (getModelObject().getDocument() != null) {
            collection = "#" + getModelObject().getProject().getName() + "/";
        }

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);

        controller = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
                final IRequestParameters request = getRequest().getPostParameters();

                Object result = null;

                BratAjaxCasController controller = new BratAjaxCasController(repository,
                        annotationService);

                // Get action
                String action = request.getParameterValue(PARAM_ACTION).toString();

                // Load the CAS if necessary
                boolean requiresCasLoading = action.equals(SpanAnnotationResponse.COMMAND)
                        || action.equals(ArcAnnotationResponse.COMMAND)
                        || action.equals(GetDocumentResponse.COMMAND);
                JCas jCas = null;
                if (requiresCasLoading) {
                    // Make sure we load the CAS only once here in case of an annotation action.
                    try {
                        jCas = getCas(getModelObject());
                    }
                    catch (ClassNotFoundException e) {
                        error("Invalid reader: " + e.getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                }

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
                    paramId = VID.parseOptional(request.getParameterValue(PARAM_ARC_ID).toString());
                }

                // HACK: If an arc was clicked that represents a link feature, then open the
                // associated span annotation instead.
                if (paramId.isSlotSet() && action.equals(ArcAnnotationResponse.COMMAND)) {
                    action = SpanAnnotationResponse.COMMAND;
                }
                
                try {
                    LOG.info("AJAX-RPC CALLED: [" + action + "]");

                    if (action.equals(WhoamiResponse.COMMAND)) {
                        result = controller.whoami();
                    }
                    else if (action.equals(SpanAnnotationResponse.COMMAND)) {
                        assert jCas != null;
                        
                        if (getModelObject().isSlotArmed() && paramId.isSet()) {
                            aAnnotationDetailEditorPanel.setSlot(aTarget, jCas, getModelObject(),
                                    paramId.getAnnotationId());
                        }
                        else {
                            // Doing anything but filling an armed slot will unarm it
                            getModelObject().clearArmedSlot();

                            getModelObject().setRelationAnno(false);
                            if (paramId == null) {
                                getModelObject().setSelectedAnnotationId(-1);
                                aAnnotationDetailEditorPanel.setLayerAndFeatureModels(jCas,
                                        getModelObject());
                            }
                            else if (paramId.isGhost()) {
                                LOG.info("This is ghost Annotation, select Layer and Feature to annotate.");
                                error("This is ghost Annotation, select Layer and Feature to annotate.");
                                aTarget.addChildren(getPage(), FeedbackPanel.class);
                                return;
                            }
                            else {
                                getModelObject().setSelectedAnnotationId(paramId.getAnnotationId());
                                aAnnotationDetailEditorPanel.setLayerAndFeatureModels(jCas,
                                        getModelObject());
                            }

                            if (getModelObject().getSelectedAnnotationId() == -1) {
                                String offsets = request.getParameterValue(PARAM_OFFSETS).toString();
                                OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(
                                        offsets, OffsetsList.class);
                                
                                Sentence sentence = BratAjaxCasUtil.selectSentenceAt(jCas,
                                        getModelObject().getSentenceBeginOffset(), getModelObject()
                                                .getSentenceEndOffset());
                                getModelObject().setBeginOffset(
                                        sentence.getBegin() + offsetLists.get(0).getBegin());
                                getModelObject().setEndOffset(
                                        sentence.getBegin()
                                                + offsetLists.get(offsetLists.size() - 1).getEnd());
                            }
                            else {
                                // get the begin/end from the annotation, no need to re-calculate
                                AnnotationFS fs = BratAjaxCasUtil.selectByAddr(jCas,
                                        getModelObject().getSelectedAnnotationId());
                                getModelObject().setBeginOffset(fs.getBegin());
                                getModelObject().setEndOffset(fs.getEnd());
                            }

                            getModelObject().setSelectedText(
                                    jCas.getDocumentText().substring(
                                            getModelObject().getBeginOffset(),
                                            getModelObject().getEndOffset()));

                            if (BratAnnotatorUtility.isDocumentFinished(repository,
                                    getModelObject())) {
                                error("This document is already closed. Please ask your project "
                                        + "manager to re-open it via the Montoring page");
                            }
                            
                            if (getModelObject().getSelectedAnnotationId() == -1) {
                                bratRenderHighlight(aTarget, getModelObject()
                                        .getSelectedAnnotationId());
                                bratRender(aTarget, jCas);
                                bratRenderGhostSpan(aTarget, jCas, getModelObject()
                                        .getBeginOffset(), getModelObject().getEndOffset());
                            }
                        }
                    }
                    else if (action.equals(ArcAnnotationResponse.COMMAND)) {
                        assert jCas != null;
                        getModelObject().setRelationAnno(true);

                        getModelObject().setOriginSpanType(
                                request.getParameterValue(PARAM_ORIGIN_TYPE).toString());
                        getModelObject().setOriginSpanId(
                                request.getParameterValue(PARAM_ORIGIN_SPAN_ID).toInteger());
                        getModelObject().setTargetSpanType(
                                request.getParameterValue(PARAM_TARGET_TYPE).toString());
                        getModelObject().setTargetSpanId(
                                request.getParameterValue(PARAM_TARGET_SPAN_ID).toInteger());

                        if (!paramId.isSet()) {
                            getModelObject().setSelectedAnnotationId(-1);
                            aAnnotationDetailEditorPanel.setLayerAndFeatureModels(jCas,
                                    getModelObject());
                        }
                        else if (paramId.isGhost()) {
                            LOG.info("This is ghost Annotation, select Layer and Feature to annotate.");
                            error("This is ghost Annotation, select Layer and Feature to annotate.");
                            aTarget.addChildren(getPage(), FeedbackPanel.class);
                            return;
                        }
                        else {
                            getModelObject().setSelectedAnnotationId(paramId.getAnnotationId());
                            aAnnotationDetailEditorPanel.setLayerAndFeatureModels(jCas,
                                    getModelObject());
                        }

                        if (BratAnnotatorUtility.isDocumentFinished(repository, getModelObject())) {
                            error("This document is already closed. Please ask admin to re-open");
                        }
                        if (getModelObject().getSelectedAnnotationId() == -1) {
                            bratRenderHighlight(aTarget, getModelObject().getSelectedAnnotationId());
                            bratRender(aTarget, jCas);
                            ghostArcAnnotationRender(aTarget, jCas);
                        }
                    }
                    else if (action.equals(LoadConfResponse.COMMAND)) {
                        result = controller.loadConf();
                    }
                    else if (action.equals(GetCollectionInformationResponse.COMMAND)) {
                        if (getModelObject().getProject() != null) {
                            result = controller.getCollectionInformation(getModelObject()
                                    .getAnnotationLayers());
                        }
                        else {
                            result = new GetCollectionInformationResponse();
                        }
                    }
                    else if (action.equals(GetDocumentResponse.COMMAND)) {
                        if (getModelObject().getProject() != null) {
                            result = controller
                                    .getDocumentResponse(getModelObject(), 0, jCas, true);
                        }
                        else {
                            result = new GetDocumentResponse();
                        }
                    }

                    LOG.info("AJAX-RPC DONE: [" + action + "]");
                }
                catch (ClassNotFoundException e) {
                    error("Invalid reader: " + e.getMessage());
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCauseMessage(e));
                }
                catch (IOException e) {
                    error(e.getMessage());
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
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                if (getModelObject().getSelectedAnnotationId() == -1) {
                    aAnnotationDetailEditorPanel.setAnnotationLayers(getModelObject());
                }
                aTarget.add(aAnnotationDetailEditorPanel);
            }
        };

        add(vis);
        add(controller);
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

        // Libraries
        aResponse.render(JavaScriptHeaderItem.forReference(JQueryUIResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQuerySvgResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQuerySvgDomResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQueryJsonResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(WebfontResourceReference.get()));

        // BRAT helpers
        aResponse
                .render(JavaScriptHeaderItem.forReference(BratConfigurationResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratUtilResourceReference.get()));
        aResponse
                .render(JavaScriptHeaderItem.forReference(BratAnnotationLogResourceReference.get()));

        // BRAT modules
        aResponse.render(JavaScriptHeaderItem.forReference(BratDispatcherResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratUrlMonitorResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAjaxResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratVisualizerResourceReference.get()));
        aResponse
                .render(JavaScriptHeaderItem.forReference(BratVisualizerUiResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAnnotatorUiResourceReference.get()));

        StringBuilder script = new StringBuilder();
        // REC 2014-10-18 - For a reason that I do not understand, the dispatcher cannot be a local
        // variable. If I put a "var" here, then communication fails with messages such as
        // "action 'openSpanDialog' returned result of action 'loadConf'" in the browsers's JS
        // console.
        script.append("(function() {");
        script.append("var dispatcher = new Dispatcher();");
        // Each visualizer talks to its own Wicket component instance
        script.append("dispatcher.ajaxUrl = '" + controller.getCallbackUrl() + "'; ");
        // We attach the JSON send back from the server to this HTML element
        // because we cannot directly pass it from Wicket to the caller in ajax.js.
        script.append("dispatcher.wicketId = '" + vis.getMarkupId() + "'; ");
        script.append("var ajax = new Ajax(dispatcher);");
        script.append("var visualizer = new Visualizer(dispatcher, '" + vis.getMarkupId() + "');");
        script.append("var visualizerUI = new VisualizerUI(dispatcher, visualizer.svg);");
        script.append("var annotatorUI = new AnnotatorUI(dispatcher, visualizer.svg);");
        script.append("var logger = new AnnotationLog(dispatcher);");
        script.append("dispatcher.post('init');");
        script.append("Wicket.$('" + vis.getMarkupId() + "').dispatcher = dispatcher;");
        script.append("Wicket.$('" + vis.getMarkupId() + "').visualizer = visualizer;");
        script.append("})();");

        // Must be OnDomReader so that this is rendered before all other Javascript that is
        // appended to the same AJAX request which turns the annotator visible after a document
        // has been chosen.
        aResponse.render(OnDomReadyHeaderItem.forScript(script.toString()));
    }

    private String bratInitCommand()
    {
        GetCollectionInformationResponse response = new GetCollectionInformationResponse();
        response.setEntityTypes(BratAjaxCasController.buildEntityTypes(getModelObject()
                .getAnnotationLayers(), annotationService));
        String json = toJson(response);
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('collectionLoaded', [" + json
                + "]);";
    }

    public String bratRenderCommand(JCas aJCas)
    {
        LOG.info("BEGIN bratRenderCommand");
        GetDocumentResponse response = new GetDocumentResponse();
        BratAjaxCasController.render(response, getModelObject(), aJCas, annotationService);
        String json = toJson(response);
        LOG.info("END bratRenderCommand");
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('renderData', [" + json
                + "]);";
    }

    public void bratRenderGhostSpan(AjaxRequestTarget aTarget, JCas aJCas, int aBeginOffset,
            int aEndOffset)
    {
        LOG.info("BEGIN ghostAnnoRender");
        GetDocumentResponse response = new GetDocumentResponse();
        // SpanAdapter.renderTokenAndSentence(aJCas, response, getModelObject());
        List<Offsets> offsets = new ArrayList<Offsets>();
        offsets.add(new Offsets(aBeginOffset, aEndOffset));
        response.addEntity(new Entity(VID.GHOST, GHOST_PLACE_HOLDER, offsets, GHOST_PLACE_HOLDER,
                GHOST_COLOR));

        BratAjaxCasController.render(response, getModelObject(), aJCas, annotationService);

        String json = toJson(response);
        aTarget.appendJavaScript("Wicket.$('" + vis.getMarkupId()
                + "').dispatcher.post('renderData', [" + json + "]);");
        LOG.info("END ghostAnnoRender");
    }

    public void ghostArcAnnotationRender(AjaxRequestTarget aTarget, JCas aJCas)
    {
        LOG.info("BEGIN ghostArcAnnoRender");
        GetDocumentResponse response = new GetDocumentResponse();
        List<Argument> argumentList = asList(new Argument("Arg1", getModelObject()
                .getOriginSpanId()), new Argument("Arg2", getModelObject().getTargetSpanId()));
        response.addRelation(new Relation(VID.GHOST, GHOST_PLACE_HOLDER, argumentList,
                GHOST_PLACE_HOLDER, GHOST_COLOR));

        BratAjaxCasController.render(response, getModelObject(), aJCas, annotationService);

        String json = toJson(response);
        aTarget.appendJavaScript("Wicket.$('" + vis.getMarkupId()
                + "').dispatcher.post('renderData', [" + json + "]);");
        LOG.info("END ghostArcAnnoRender");
    }

    /**
     * This triggers the loading of the metadata (colors, types, etc.)
     *
     * @return the init script.
     * @see BratAjaxConfiguration#buildEntityTypes
     */
    protected String bratInitLaterCommand()
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
    protected String bratRenderLaterCommand()
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
    public void bratInitRenderLater(IHeaderResponse aResponse)
    {
        aResponse.render(OnLoadHeaderItem.forScript(bratInitLaterCommand()));
        aResponse.render(OnLoadHeaderItem.forScript(bratRenderLaterCommand()));
    }

    /**
     * Reload {@link BratAnnotator} when the Correction/Curation page is opened
     *
     * @param aTarget
     *            the AJAX target.
     */
    public void bratInitRenderLater(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(bratInitLaterCommand());
        aTarget.appendJavaScript(bratRenderLaterCommand());
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
    public void bratRenderHighlight(AjaxRequestTarget aTarget, int aAnnotationId)
    {
        if (aAnnotationId < 0) {
            aTarget.appendJavaScript("Wicket.$('" + vis.getMarkupId()
                    + "').dispatcher.post('current', " + "['" + getCollection()
                    + "', '1234', {edited:[]}, false]);");
        }
        else {
            aTarget.appendJavaScript("Wicket.$('" + vis.getMarkupId()
                    + "').dispatcher.post('current', " + "['" + getCollection()
                    + "', '1234', {edited:[[" + aAnnotationId + "]]}, false]);");
        }
    }

    public void bratInit(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(bratInitCommand());
    }

    public String getCollection()
    {
        return collection;
    }

    public void setCollection(String collection)
    {
        this.collection = collection;
    }

    public void onChange(AjaxRequestTarget aTarget, BratAnnotatorModel aBratAnnotatorModel)
    {

    }

    public void onAnnotate(AjaxRequestTarget aTarget, BratAnnotatorModel aModel, int aStart,
            int aEnd)
    {
        // Overriden in AutomationPage
    }

    public void onDelete(AjaxRequestTarget aTarget, BratAnnotatorModel aModel, int aStart, int aEnd)
    {
        // Overriden in AutomationPage
    }

    private String toJson(Object result)
    {
        StringWriter out = new StringWriter();
        JsonGenerator jsonGenerator = null;
        try {
            jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
                    .createJsonGenerator(out);
            jsonGenerator.writeObject(result);
        }
        catch (IOException e) {
            error("Unable to produce JSON response " + ":" + ExceptionUtils.getRootCauseMessage(e));
        }
        return out.toString();
    }

    private JCas getCas(BratAnnotatorModel aBratAnnotatorModel)
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

    public static String generateMessage(AnnotationLayer aLayer, String aLabel, boolean aDeleted)
    {
        String action = aDeleted ? "deleted" : "created/updated";

        String msg = "The [" + aLayer.getUiName() + "] annotation has been " + action + ".";
        if (StringUtils.isNotBlank(aLabel)) {
            msg += " Label: [" + aLabel + "]";
        }
        return msg;
    }
}