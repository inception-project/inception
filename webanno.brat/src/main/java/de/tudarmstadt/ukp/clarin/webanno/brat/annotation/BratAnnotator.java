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

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.CloseButtonCallback;
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
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.ArcOpenDialogResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.LoadConfResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.SpanOpenDialogResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.StoreSvgResponse;
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
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
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
    
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ARC_ID = "arcId";
    private static final String PARAM_ID = "id";
    private static final String PARAM_OFFSETS = "offsets";
    private static final String PARAM_SPAN_TEXT = "spanText";
    private static final String PARAM_TARGET_SPAN_ID = "targetSpanId";
    private static final String PARAM_TARGET_TYPE = "targetType";
    private static final String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    private static final String PARAM_ORIGIN_TYPE = "originType";

    private static final long serialVersionUID = -1537506294440056609L;

    private WebMarkupContainer vis;

    private AbstractAjaxBehavior controller;

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private String collection = "";

    private String selectedSpanText, offsets;
    private Integer selectedArcId;

    private Integer originSpanId, targetSpanId;
    private boolean closeButtonClicked;// check if the annotation dialog has a change

    private String originSpanType = null, targetSpanType = null;

    private int beginOffset;
    private int endOffset;

    /**
     * Data models for {@link BratAnnotator}
     * 
     * @param aModel the model.
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

    public BratAnnotator(String id, IModel<BratAnnotatorModel> aModel)
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

        final ModalWindow openAnnotationDialog;
        add(openAnnotationDialog = new ModalWindow("openAnnotationDialog"));
        openAnnotationDialog.setOutputMarkupId(true);
        openAnnotationDialog.setInitialWidth(550);
        openAnnotationDialog.setInitialHeight(340);
        openAnnotationDialog.setResizable(true);
        openAnnotationDialog.setWidthUnit("px");
        openAnnotationDialog.setHeightUnit("px");
        openAnnotationDialog.setCloseButtonCallback(new CloseButtonCallback()
        {
            private static final long serialVersionUID = -5423095433535634321L;

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget aTarget)
            {
                closeButtonClicked = true;
                return true;
            }
        });

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
                
                final String action = request.getParameterValue(PARAM_ACTION).toString();

                try {
                    LOG.info("AJAX-RPC CALLED: [" + action + "]");
                    
                    if (action.equals(WhoamiResponse.COMMAND)) {
                        result = controller.whoami();
                    }
                    else if (action.equals(StoreSvgResponse.COMMAND)) {
                        result = controller.storeSVG();
                    }
                    else if (action.equals(SpanOpenDialogResponse.COMMAND)) {
                        int selectedSpanID;
                        if (request.getParameterValue(PARAM_ID).toString() == null) {
                            selectedSpanID = -1;
                        }
                        else {
                            selectedSpanID = request.getParameterValue(PARAM_ID).toInt();
                        }

                        offsets = request.getParameterValue(PARAM_OFFSETS).toString();
                        OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(
                                offsets, OffsetsList.class);

                        JCas jCas = getJCas();
                        if (selectedSpanID == -1) {
                            Sentence sentence = BratAjaxCasUtil.selectSentenceAt(jCas,
                                    getModelObject().getSentenceBeginOffset(), getModelObject()
                                            .getSentenceEndOffset());
                            beginOffset = sentence.getBegin() + offsetLists.get(0).getBegin();
                            endOffset = sentence.getBegin()
                                    + offsetLists.get(offsetLists.size() - 1).getEnd();
                        }

                        // get the begin/end from the annotation, no need to re-calculate
                        else {
                            AnnotationFS fs = BratAjaxCasUtil.selectByAddr(jCas, selectedSpanID);
                            beginOffset = fs.getBegin();
                            endOffset = fs.getEnd();
                        }

                        selectedSpanText = request.getParameterValue(PARAM_SPAN_TEXT).toString();
                        /*
                         * selectedSpan = BratAjaxCasUtil .getSelectedText(jCas, beginOffset,
                         * endOffset);
                         */

                        if (BratAnnotatorUtility.isDocumentFinished(repository, getModelObject())) {
                            error("This document is already closed. Please ask admin to re-open");
                        }
                        else {
                            openSpanAnnotationDialog(openAnnotationDialog, aTarget, beginOffset,
                                    endOffset, selectedSpanID);
                        }
                        result = new SpanOpenDialogResponse();
                    }
                    else if (action.equals(ArcOpenDialogResponse.COMMAND)) {
                        originSpanType = request.getParameterValue(PARAM_ORIGIN_TYPE).toString();
                        originSpanId = request.getParameterValue(PARAM_ORIGIN_SPAN_ID).toInteger();
                        targetSpanType = request.getParameterValue(PARAM_TARGET_TYPE).toString();
                        targetSpanId = request.getParameterValue(PARAM_TARGET_SPAN_ID).toInteger();

                        if (request.getParameterValue(PARAM_ARC_ID).toString() == null) {
                            selectedArcId = -1;
                        }
                        else {
                            selectedArcId = request.getParameterValue(PARAM_ARC_ID).toInt();
                        }

                        if (BratAnnotatorUtility.isDocumentFinished(repository, getModelObject())) {
                            error("This document is already closed. Please ask admin to re-open");
                        }

                        else {
                            openArcAnnotationDialog(openAnnotationDialog, aTarget);
                        }

                        result = new ArcOpenDialogResponse();
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
                            result = controller.getDocumentResponse(getModelObject(), 0, getJCas(),
                                    true);
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
    
    /**
     * opens the {@link SpanAnnotationModalWindowPage} in a {@link ModalWindow}
     */
    private void openSpanAnnotationDialog(final ModalWindow openAnnotationDialog,
            AjaxRequestTarget aTarget, final int aBeginOffset, final int aEndOffset,
            int aSelectedSpanId)
    {
        closeButtonClicked = false;
        if (aSelectedSpanId == -1) {// new annotation
            openAnnotationDialog.setTitle("New Span Annotation");
            openAnnotationDialog.setContent(new  SpanAnnotationModalWindowPage(openAnnotationDialog
                    .getContentId(),openAnnotationDialog,
                    getModelObject(), selectedSpanText, aBeginOffset, aEndOffset));
        }
        else {
            openAnnotationDialog.setTitle("Edit Span Annotation");
            openAnnotationDialog.setContent(new SpanAnnotationModalWindowPage(openAnnotationDialog
                    .getContentId(), openAnnotationDialog,
                    getModelObject(), aSelectedSpanId));
        }

        openAnnotationDialog.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = -1746088901018629567L;

            @Override
            public void onClose(AjaxRequestTarget aTarget)
            {
                if (!closeButtonClicked) {
                    aTarget.appendJavaScript(bratRenderCommand());
                    onChange(aTarget, getModelObject());
                }
            }
        });
        // open the annotation dialog if only there is
        // span annotation layer (from the settings button) selected
        for (AnnotationLayer layer : getModelObject().getAnnotationLayers()) {
            /*
             * if (layer.getFeature() == null) { continue; }
             */
            if (layer.getType().equals(WebAnnoConst.SPAN_TYPE)
                    || layer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                openAnnotationDialog.show(aTarget);
                break;
            }
        }
    }

    /**
     * opens the {@link ArcAnnotationModalWindowPanel} in a {@link ModalWindow}
     */
    private void openArcAnnotationDialog(final ModalWindow openAnnotationDialog,
            AjaxRequestTarget aTarget)
    {

        closeButtonClicked = false;
        if (selectedArcId == -1) {// new annotation
            openAnnotationDialog.setTitle("New Arc Annotation");
            openAnnotationDialog.setContent(new ArcAnnotationModalWindowPanel(openAnnotationDialog
                    .getContentId(), openAnnotationDialog, getModelObject(), originSpanId,
                    originSpanType, targetSpanId, targetSpanType));
        }
        else {
            openAnnotationDialog.setTitle("Edit Arc Annotation");
            openAnnotationDialog.setContent(new ArcAnnotationModalWindowPanel(openAnnotationDialog
                    .getContentId(), openAnnotationDialog, getModelObject(), originSpanId,
                    targetSpanId, selectedArcId));
        }

        openAnnotationDialog.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = -1746088901018629567L;

            @Override
            public void onClose(AjaxRequestTarget aTarget)
            {
                if (!closeButtonClicked) {
                    aTarget.appendJavaScript(bratRenderCommand());
                    onChange(aTarget, getModelObject());
                }

            }
        });
        // open the annotation dialog if only there is
        // span annotation layer (from the settings button) selected
        for (AnnotationLayer layer : getModelObject().getAnnotationLayers()) {
            /*
             * if (layer.getFeature() == null) { continue; }
             */
            if (layer.getType().equals(WebAnnoConst.SPAN_TYPE)
                    || layer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                openAnnotationDialog.show(aTarget);
                break;
            }
        }
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
        aResponse.render(JavaScriptHeaderItem.forReference(BratConfigurationResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratUtilResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAnnotationLogResourceReference.get()));
        
        // BRAT modules
        aResponse.render(JavaScriptHeaderItem.forReference(BratDispatcherResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratUrlMonitorResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAjaxResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratVisualizerResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratVisualizerUiResourceReference.get()));
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
        script.append("})();");
        
        // Must be OnDomReader so that this is rendered before all other Javascript that is
        // appended to the same AJAX request which turns the annotator visible after a document
        // has been chosen.
        aResponse.render(OnDomReadyHeaderItem.forScript(script.toString()));
    }

    private String bratInitCommand()
    {
        GetCollectionInformationResponse response = new GetCollectionInformationResponse();
        response.setEntityTypes(BratAjaxConfiguration.buildEntityTypes(getModelObject()
                .getAnnotationLayers(), annotationService));        
        String json = toJson(response);
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('collectionLoaded', [" + json
                + "]);";
    }
    
    private String bratRenderCommand()
    {
        GetDocumentResponse response = new GetDocumentResponse();
        BratAjaxCasController.render(response, getModelObject(), getJCas(), annotationService);
        String json = toJson(response);
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('renderData', [" + json
                + "]);";
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
                + "[{action: 'getCollectionInformation',collection: '"
                + getCollection() + "'}, 'collectionLoaded', {collection: '" + getCollection()
                + "',keep: true}]);";
    }
    
    /**
     * This one triggers the loading of the actual document data
     *
     * @return brat 
     */
    protected String bratRenderLaterCommand()
    {
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('current', "
                + "['" + getCollection() + "', '1234', {}, true]);";
    }
    
    /**
     * Reload {@link BratAnnotator} when the Correction/Curation page is opened
     * 
     * @param aResponse the response.
     */
    public void bratInitRenderLater(IHeaderResponse aResponse)
    {
        aResponse.render(OnLoadHeaderItem.forScript(bratInitLaterCommand()));
        aResponse.render(OnLoadHeaderItem.forScript(bratRenderLaterCommand()));
    }

    /**
     * Reload {@link BratAnnotator} when the Correction/Curation page is opened
     * 
     * @param aTarget the AJAX target.
     */
    public void bratInitRenderLater(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(bratInitLaterCommand());
        aTarget.appendJavaScript(bratRenderLaterCommand());
    }

    /**
     * Render content in a separate request.
     * 
     * @param aTarget the AJAX target.
     */
    public void bratRenderLater(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(bratRenderLaterCommand());
    }

    /**
     * Render content as part of the current request.
     * 
     * @param aTarget the AJAX target.
     */
    public void bratRender(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(bratRenderCommand());
    }

    public void bratInit(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(bratInitCommand());
    }

    private JCas getCas(Project aProject, User user, SourceDocument aDocument, Mode aMode)
        throws UIMAException, IOException, ClassNotFoundException
    {
        if (aMode.equals(Mode.ANNOTATION) || aMode.equals(Mode.AUTOMATION)
                || aMode.equals(Mode.CORRECTION) || aMode.equals(Mode.CORRECTION_MERGE)) {

            return repository.readJCas(aDocument, aProject, user);
        }
        else {
            return repository.getCurationDocumentContent(getModelObject().getDocument());
        }
    }

    public String getCollection()
    {
        return collection;
    }

    public void setCollection(String collection)
    {
        this.collection = collection;
    }

    protected void onChange(AjaxRequestTarget aTarget, BratAnnotatorModel aBratAnnotatorModel)
    {

    }

    protected void onAnnotate(BratAnnotatorModel aModel, int aStart, int aEnd)
    {
        // Overriden in AutomationPage
    }

    protected void onDelete(BratAnnotatorModel aModel, int aStart, int aEnd)
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
            error("Unable to produce JSON response " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }     
        return out.toString();
    }
    
    private JCas getJCas()
    {
        JCas jCas = null;
        if (getModelObject().getDocument() != null) {
            try {
                jCas = getCas(getModelObject().getProject(), getModelObject().getUser(),
                        getModelObject().getDocument(), getModelObject().getMode());
            }
            catch (UIMAException e1) {
                error(ExceptionUtils.getRootCause(e1));
            }
            catch (IOException e1) {
                error(ExceptionUtils.getRootCause(e1));
            }
            catch (ClassNotFoundException e1) {
                error(ExceptionUtils.getRootCause(e1));
            }
        }
        return jCas;
    }
}