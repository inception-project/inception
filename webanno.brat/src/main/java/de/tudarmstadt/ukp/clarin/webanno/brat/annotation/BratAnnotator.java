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

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.CloseButtonCallback;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Base class for displaying a BRAT visualization. Override methods {@link #getCollectionData()} and
 * {@link #getDocumentData()} to provide the actual data.
 *
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 * @author Andreas Straninger
 */
public class BratAnnotator
    extends Panel
{
    private static final String ACTION_ARC_OPEN_DIALOG = "arcOpenDialog";
    private static final String ACTION_GET_COLLECTION_INFORMATION = "getCollectionInformation";
    private static final String ACTION_GET_DOCUMENT = "getDocument";
    private static final String ACTION_LOAD_CONF = "loadConf";
    private static final String ACTION_SPAN_OPEN_DIALOG = "spanOpenDialog";
    private static final String ACTION_STORE_SVG = "storeSVG";
    private static final String ACTION_WHOAMI = "whoami";

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
    private Integer selectedSpanID, selectedArcId;

    private Integer originSpanId, targetSpanId;
    private boolean closeButtonClicked;// check if the annotation dialog has a change

    private String originSpanType = null, targetSpanType = null;

    private int beginOffset;
    private int endOffset;

    /**
     * Data models for {@link BratAnnotator}
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

        if (getModelObject().getDocument() != null) {
            collection = "#" + getModelObject().getProject().getName() + "/";

        }

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);

        final FeedbackPanel feedbackPanel = new FeedbackPanel("feedbackPanel");
        add(feedbackPanel);
        feedbackPanel.setOutputMarkupId(true);
        feedbackPanel.add(new AttributeModifier("class", "info"));
        feedbackPanel.add(new AttributeModifier("class", "error"));

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

                final IRequestParameters request = getRequest().getPostParameters();

                Object result = null;
                BratAjaxCasController controller = new BratAjaxCasController(repository,
                        annotationService);

                try {
                    final String action = request.getParameterValue(PARAM_ACTION).toString();

                    if (action.equals(ACTION_WHOAMI)) {
                        result = controller.whoami();
                    }
                    else if (action.equals(ACTION_STORE_SVG)) {
                        result = controller.storeSVG();
                    }
                    else if (action.equals(ACTION_SPAN_OPEN_DIALOG)) {
                        if (request.getParameterValue(PARAM_ID).toString() == null) {
                            selectedSpanID = -1;
                        }
                        else {
                            selectedSpanID = request.getParameterValue(PARAM_ID).toInt();
                        }

                        offsets = request.getParameterValue(PARAM_OFFSETS).toString();
                        OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(
                                offsets, OffsetsList.class);

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
                                    endOffset);
                        }
                        result = controller.loadConf();
                    }

                    else if (action.equals(ACTION_ARC_OPEN_DIALOG)) {

                        Session.get().getFeedbackMessages().clear();
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

                        result = controller.loadConf();
                    }
                    else if (action.equals(ACTION_LOAD_CONF)) {
                        result = controller.loadConf();
                    }
                    else if (action.equals(ACTION_GET_COLLECTION_INFORMATION)
                            && getModelObject().getProject() != null) {
                        result = controller.getCollectionInformation(getModelObject()
                                .getAnnotationLayers());

                    }
                    else if (action.equals(ACTION_GET_DOCUMENT)) {
                        result = controller.getDocumentResponse(getModelObject(), 0, jCas, true);
                    }
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
                StringWriter out = new StringWriter();
                JsonGenerator jsonGenerator = null;
                try {
                    jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
                            .createJsonGenerator(out);
                    if (result == null) {
                        result = "test";
                    }

                    jsonGenerator.writeObject(result);
                }
                catch (IOException e) {
                    error("Unable to produce JSON response " + ":"
                            + ExceptionUtils.getRootCauseMessage(e));
                }

                // Since we cannot pass the JSON directly to Brat, we attach it to the HTML element
                // into which BRAT renders the SVG. In our modified ajax.js, we pick it up from
                // there and then pass it on to BRAT to do the rendering.
                aTarget.prependJavaScript("Wicket.$('" + vis.getMarkupId() + "').temp = "
                        + out.toString() + ";");
                // getRequestCycle().scheduleRequestHandlerAfterCurrent(
                // new TextRequestHandler("application/json", "UTF-8", out.toString()));
                /*
                 * if (hasChanged) { onChange(aTarget); }
                 */
                aTarget.add(feedbackPanel);
            }
        };

        add(vis);
        add(controller);

    }

    /**
     * opens the {@link SpanAnnotationModalWindowPage} in a {@link ModalWindow}
     */

    private void openSpanAnnotationDialog(final ModalWindow openAnnotationDialog,
            AjaxRequestTarget aTarget, final int aBeginOffset, final int aEndOffset)
    {
        closeButtonClicked = false;
        openAnnotationDialog.setPageCreator(new ModalWindow.PageCreator()
        {
            private static final long serialVersionUID = -2827824968207807739L;

            @Override
            public Page createPage()
            {
                if (selectedSpanID == -1) {// new annotation
                    openAnnotationDialog.setTitle("New Span Annotation");
                    return new SpanAnnotationModalWindowPage(openAnnotationDialog,
                            getModelObject(), selectedSpanText, aBeginOffset, aEndOffset);
                }
                else {
                    openAnnotationDialog.setTitle("Edit Span Annotation");

                    return new SpanAnnotationModalWindowPage(openAnnotationDialog,
                            getModelObject(), selectedSpanID);
                }
            }

        });

        openAnnotationDialog.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = -1746088901018629567L;

            @Override
            public void onClose(AjaxRequestTarget aTarget)
            {
                // A hack to remember the wicket combobox DropDown display value
                HttpSession session = ((ServletWebRequest) RequestCycle.get().getRequest())
                        .getContainerRequest().getSession();
                BratAnnotatorModel model = (BratAnnotatorModel) session.getAttribute("model");
                if (model != null) {
                    // setModelObject(model);
                    getModelObject().setSentenceAddress(model.getSentenceAddress());

                    getModelObject().setSentenceBeginOffset(model.getSentenceBeginOffset());
                    getModelObject().setSentenceEndOffset(model.getSentenceEndOffset());

                    getModelObject().setRememberedSpanLayer(model.getRememberedSpanLayer());
                    getModelObject().setRememberedSpanFeatures(model.getRememberedSpanFeatures());

                    getModelObject().setAnnotate(model.isAnnotate());
                    getModelObject().setMessage(model.getMessage());

                }

                if (!closeButtonClicked) {
                    if (selectedSpanID == -1) {
                        onAnnotate(getModelObject(), beginOffset, endOffset);
                    }
                    if (!getModelObject().isAnnotate()
                            && getModelObject().getProject().getMode().equals(Mode.AUTOMATION)) {
                        onDelete(getModelObject(), beginOffset, endOffset);
                    }
                    onChange(aTarget, getModelObject());
                    reloadContent(aTarget);
                }

            }
        });
        // open the annotation dialog if only there is
        // span annotation layer (from the settings button) selected
        for (AnnotationLayer layer : getModelObject().getAnnotationLayers()) {
            /*
             * if (layer.getFeature() == null || layer.getLayer() == null) { continue; }
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
        openAnnotationDialog.setPageCreator(new ModalWindow.PageCreator()
        {
            private static final long serialVersionUID = -2827824968207807739L;

            @Override
            public Page createPage()
            {
                if (selectedArcId == -1) {// new annotation
                    openAnnotationDialog.setTitle("New Arc Annotation");
                    return new ArcAnnotationModalWindowPanel(openAnnotationDialog,
                            getModelObject(), originSpanId, originSpanType, targetSpanId,
                            targetSpanType);
                }
                else {
                    openAnnotationDialog.setTitle("Edit Arc Annotation");

                    return new ArcAnnotationModalWindowPanel(openAnnotationDialog,
                            getModelObject(), originSpanId, targetSpanId, selectedArcId);
                }
            }

        });

        openAnnotationDialog.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = -1746088901018629567L;

            @Override
            public void onClose(AjaxRequestTarget aTarget)
            {

             // A hack to remember the wicket combobox DropDown display value
                HttpSession session = ((ServletWebRequest) RequestCycle.get().getRequest())
                        .getContainerRequest().getSession();
                BratAnnotatorModel model = (BratAnnotatorModel) session.getAttribute("model");
                if (model != null) {
                    // setModelObject(model);
                    getModelObject().setSentenceAddress(model.getSentenceAddress());

                    getModelObject().setSentenceBeginOffset(model.getSentenceBeginOffset());
                    getModelObject().setSentenceEndOffset(model.getSentenceEndOffset());

                    getModelObject().setRememberedArcLayer(model.getRememberedArcLayer());
                    getModelObject().setRememberedArcFeatures(model.getRememberedArcFeatures());

                    getModelObject().setMessage(model.getMessage());

                }

                if (!closeButtonClicked) {
                    onChange(aTarget, getModelObject());
                    reloadContent(aTarget);
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

        String[] annotatorScript = new String[] { "dispatcher = new Dispatcher();"
                // Each visualizer talks to its own Wicket component instance
                + "dispatcher.ajaxUrl = '"
                + controller.getCallbackUrl()
                + "'; "
                // We attach the JSON send back from the server to this HTML element
                // because we cannot directly pass it from Wicket to the caller in ajax.js.
                + "dispatcher.wicketId = '" + vis.getMarkupId() + "'; "
                + "var urlMonitor = new URLMonitor(dispatcher); "
                + "var ajax = new Ajax(dispatcher);" + "var ajax_" + vis.getMarkupId() + " = ajax;"
                + "var visualizer = new Visualizer(dispatcher, '" + vis.getMarkupId() + "');"
                + "var visualizerUI = new VisualizerUI(dispatcher, visualizer.svg);"
                + "var annotatorUI = new AnnotatorUI(dispatcher, visualizer.svg);"
                + "var spinner = new Spinner(dispatcher, '#spinner');"
                + "var logger = new AnnotationLog(dispatcher);" + "dispatcher.post('init');" };

        String[] curatorScript = new String[] { "dispatcher = new Dispatcher();"
                // Each visualizer talks to its own Wicket component instance
                + "dispatcher.ajaxUrl = '"
                + controller.getCallbackUrl()
                + "'; "
                // We attach the JSON send back from the server to this HTML element
                // because we cannot directly pass it from Wicket to the caller in ajax.js.
                + "dispatcher.wicketId = '"
                + vis.getMarkupId()
                + "'; "
                // + "var urlMonitor = new URLMonitor(dispatcher); "
                + "var ajax = new Ajax(dispatcher);" + "var ajax_" + vis.getMarkupId() + " = ajax;"
                + "var visualizer = new Visualizer(dispatcher, '" + vis.getMarkupId() + "');"
                + "var visualizerUI = new VisualizerUI(dispatcher, visualizer.svg);"
                + "var annotatorUI = new AnnotatorUI(dispatcher, visualizer.svg);"
                + "var spinner = new Spinner(dispatcher, '#spinner');"
                + "var logger = new AnnotationLog(dispatcher);" + "dispatcher.post('init');" };

        if (getModelObject().getMode().equals(Mode.ANNOTATION)) { // works with the URLMonitor
            aResponse.renderOnLoadJavaScript("\n" + StringUtils.join(annotatorScript, "\n"));
        }
        else {
            aResponse.renderOnLoadJavaScript("\n" + StringUtils.join(curatorScript, "\n"));
        }
    }

    /**
     * Reload {@link BratAnnotator} when the Correction/Curation page is opened
     */
    public void reloadContent(IHeaderResponse aResponse)
    {
        String[] script = new String[] { "dispatcher.post('clearSVG', []);"
                + "dispatcher.post('current', ['"
                + getCollection()
                + "', '1234', {}, true]);"
                // start ajax call, which requests the collection (and the document) from the server
                // and renders the svg
                + "dispatcher.post('ajax', [{action: 'getCollectionInformation',collection: '"
                + getCollection() + "'}, 'collectionLoaded', {collection: '" + getCollection()
                + "',keep: true}]);"
        // + "dispatcher.post('collectionChanged');"
        };
        aResponse.renderOnLoadJavaScript("\n" + StringUtils.join(script, "\n"));
    }

    /**
     * Reload {@link BratAnnotator} when the Correction/Curation page is clicked for span/arc merge
     */
    public void reloadContent(AjaxRequestTarget aTarget)
    {
        String[] script = new String[] { "dispatcher.post('clearSVG', []);"
                + "dispatcher.post('current', ['"
                + getCollection()
                + "', '1234', {}, true]);"
                // start ajax call, which requests the collection (and the document) from the server
                // and renders the svg
                + "dispatcher.post('ajax', [{action: 'getCollectionInformation',collection: '"
                + getCollection() + "'}, 'collectionLoaded', {collection: '" + getCollection()
                + "',keep: true}]);"
        // + "dispatcher.post('collectionChanged');"
        };
        aTarget.appendJavaScript("\n" + StringUtils.join(script, "\n"));
    }

    @Override
    protected void onAfterRender()
    {
        super.onAfterRender();
        Session.get().getFeedbackMessages().clear();
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
}