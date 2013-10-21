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
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
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
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

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
    private String document = "";

    String selectedSpan, offsets, selectedSpanType, selectedArcType;
    Integer selectedSpanID, selectedArcId;

    Integer originSpanId, targetSpanId;

    private String originSpanType = null, targetSpanType = null;

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
            document = getModelObject().getDocument().getName();

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
        openAnnotationDialog.setInitialWidth(450);
        openAnnotationDialog.setInitialHeight(280);
        openAnnotationDialog.setResizable(true);
        openAnnotationDialog.setWidthUnit("px");
        openAnnotationDialog.setHeightUnit("px");

        controller = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
                boolean hasChanged = false;
                final BratAnnotatorUIData uIData = new BratAnnotatorUIData();
                if (getModelObject().getDocument() != null) {
                    try {
                        uIData.setjCas(getCas(getModelObject().getProject(), getModelObject()
                                .getUser(), getModelObject().getDocument(), getModelObject()
                                .getMode()));
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
                    if (request.getParameterValue("action").toString().equals("whoami")) {
                        result = controller.whoami();
                    }
                    else if (request.getParameterValue("action").toString().equals("storeSVG")) {
                        result = controller.storeSVG();
                    }
                    else if (request.getParameterValue("action").toString()
                            .equals("spanOpenDialog")) {

                        selectedSpan = request.getParameterValue("spanText").toString();
                        offsets = request.getParameterValue("offsets").toString();

                        JCas jCas = getCas(getModelObject());

                        OffsetsList offsetLists = (OffsetsList) jsonConverter.getObjectMapper()
                                .readValue(offsets, OffsetsList.class);

                        int start = getModelObject().getSentenceBeginOffset()
                                + ((Offsets) offsetLists.get(0)).getBegin();

                        int end = getModelObject().getSentenceBeginOffset()
                                + ((Offsets) offsetLists.get(0)).getEnd();

                         if (!BratAjaxCasUtil.offsetsInOneSentences(jCas, start, end)) {
                             aTarget.appendJavaScript("alert('Annotation coveres multiple sentences,"
                                     + " limit your annotation to single sentence!')");
                             }
                         else{

                        if (request.getParameterValue("id").toString() == null) {
                            selectedSpanID = -1;
                        }
                        else {
                            selectedSpanID = request.getParameterValue("id").toInt();
                            selectedSpanType = request.getParameterValue("type").toString();
                        }
                        if (BratAnnotatorUtility.isDocumentFinished(repository, getModelObject())) {
                            error("This document is already closed. Please ask admin to re-open");
                        }
                        else {
                            openSpanAnnotationDialog(openAnnotationDialog, aTarget);
                        }
                         }
                        result = controller.loadConf();
                        hasChanged = true;
                    }

                    else if (request.getParameterValue("action").toString().equals("arcOpenDialog")) {

                        originSpanType = request.getParameterValue("originType").toString();
                        originSpanId = request.getParameterValue("originSpanId").toInteger();
                        selectedArcType = request.getParameterValue("arcType").toString();
                        targetSpanType = request.getParameterValue("targetType").toString();
                        targetSpanId = request.getParameterValue("targetSpanId").toInteger();

                        if (request.getParameterValue("arcId").toString() == null) {
                            selectedArcId = -1;
                        }
                        else {
                            selectedArcId = request.getParameterValue("arcId").toInt();
                        }

                        if (BratAnnotatorUtility.isDocumentFinished(repository, getModelObject())) {
                            error("This document is already closed. Please ask admin to re-open");
                        }
                        else {
                            openArcAnnotationDialog(openAnnotationDialog, aTarget);
                        }


                        result = controller.loadConf();
                        hasChanged = true;
                    }
                    else if (request.getParameterValue("action").toString().equals("loadConf")) {
                        result = controller.loadConf();
                    }
                    else if (request.getParameterValue("action").toString()
                            .equals("getCollectionInformation")
                            && getModelObject().getProject() != null) {
                        result = controller.getCollectionInformation(getModelObject().getProject()
                                .getName(), getModelObject().getAnnotationLayers());

                    }

                    else if (request.getParameterValue("action").toString().equals("getDocument")) {
                        result = BratAnnotatorUtility.getDocument(uIData, repository,
                                annotationService, getModelObject());
                    }
                    else if (!BratAnnotatorUtility.isDocumentFinished(repository, getModelObject())) {
                        if (request.getParameterValue("action").toString().equals("createSpan")) {
                            try {
                                result = BratAnnotatorUtility.createSpan(request, getModelObject()
                                        .getUser(), uIData, repository, annotationService,
                                        getModelObject(), jsonConverter);

                                info("Annotation [" + request.getParameterValue("type").toString()
                                        + "]has been created");
                                hasChanged = true;
                            }

                            catch (Exception e) {
                                info(e);
                                result = BratAnnotatorUtility.getDocument(uIData, repository,
                                        annotationService, getModelObject());
                            }
                        }

                        else if (request.getParameterValue("action").toString().equals("createArc")) {

                            result = BratAnnotatorUtility.createArc(request, getModelObject()
                                    .getUser(), uIData, repository, annotationService,
                                    getModelObject());
                            info("Annotation [" + request.getParameterValue("type").toString()
                                    + "]has been created");
                            hasChanged = true;
                        }

                        else if (request.getParameterValue("action").toString()
                                .equals("reverseArc")) {
                            result = BratAnnotatorUtility.reverseArc(request, getModelObject()
                                    .getUser(), uIData, repository, annotationService,
                                    getModelObject());
                            info("Annotation [" + request.getParameterValue("type").toString()
                                    + "]has bratAnnotatorModelbeen reversed");
                            hasChanged = true;
                        }
                        else if (request.getParameterValue("action").toString()
                                .equals("deleteSpan")) {
                            String type = request.getParameterValue("type").toString();
                            String getLabelPrefix = BratAjaxCasUtil.getLabelPrefix(type);
                            if (getLabelPrefix.equals(AnnotationTypeConstant.POS_PREFIX)) {
                                result = BratAnnotatorUtility.getDocument(uIData, repository,
                                        annotationService, getModelObject());
                                info("POS annotations can't be deleted!");
                            }
                            else {
                                result = BratAnnotatorUtility.deleteSpan(request, getModelObject()
                                        .getUser(), uIData, repository, annotationService,
                                        getModelObject(), jsonConverter);
                                info("Annotation [" + request.getParameterValue("type").toString()
                                        + "]has been deleted");
                                hasChanged = true;
                            }
                        }

                        else if (request.getParameterValue("action").toString().equals("deleteArc")) {
                            result = BratAnnotatorUtility.deleteArc(request, getModelObject()
                                    .getUser(), uIData, repository, annotationService,
                                    getModelObject());
                            info("Annotation [" + request.getParameterValue("type").toString()
                                    + "]has been deleted");
                            hasChanged = true;
                        }
                    }
                    else {
                        result = BratAnnotatorUtility.getDocument(uIData, repository,
                                annotationService, getModelObject());
                        error("This document is already closed. Please ask admin to re-open");
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
      /*          if (hasChanged) {
                    onChange(aTarget);
                }*/
                aTarget.add(feedbackPanel);
            }
        };

        add(vis);
        add(controller);

    }

    private void openSpanAnnotationDialog(final ModalWindow openAnnotationDialog,
            AjaxRequestTarget aTarget)
    {
        openAnnotationDialog.setPageCreator(new ModalWindow.PageCreator()
        {
            private static final long serialVersionUID = -2827824968207807739L;

            @Override
            public Page createPage()
            {
                if (selectedSpanID == -1) {// new annotation
                    openAnnotationDialog.setTitle("New Span Annotation");
                    return new SpanAnnotationModalWindowPage(openAnnotationDialog,
                            getModelObject(), selectedSpan, offsets);
                }
                else {
                    openAnnotationDialog.setTitle("Edit Span Annotation");
                    return new SpanAnnotationModalWindowPage(openAnnotationDialog,
                            getModelObject(), selectedSpan, offsets, selectedSpanType,
                            selectedSpanID);
                }
            }

        });

        openAnnotationDialog.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = -1746088901018629567L;

            @Override
            public void onClose(AjaxRequestTarget aTarget)
            {
                // A hack to rememeber the Visural DropDown display value
                HttpSession session = ((ServletWebRequest) RequestCycle.get().getRequest())
                        .getContainerRequest().getSession();
                BratAnnotatorModel model = (BratAnnotatorModel) session.getAttribute("model");
                if (model != null) {
                    setModelObject(model);
                }
                onChange(aTarget, model);
                reloadContent(aTarget);

            }
        });
        // open the annotation dialog if only there is
        // span annotation layer (from the settings button) selected
        for (TagSet tagSet : getModelObject().getAnnotationLayers()) {
            if (tagSet.getType().getType().equals(AnnotationTypeConstant.SPAN_TYPE)) {
                openAnnotationDialog.show(aTarget);
                break;
            }
        }
    }

    private void openArcAnnotationDialog(final ModalWindow openAnnotationDialog,
            AjaxRequestTarget aTarget)
    {

        if (selectedArcId == -1) {// new annotation
            openAnnotationDialog.setTitle("New Arc Annotation");
            openAnnotationDialog.setContent(new ArcAnnotationModalWindowPage(openAnnotationDialog
                    .getContentId(), openAnnotationDialog, getModelObject(), originSpanId,
                    originSpanType, targetSpanId, targetSpanType));
        }
        else {
            openAnnotationDialog.setTitle("Edit Arc Annotation");
            openAnnotationDialog.setContent(new ArcAnnotationModalWindowPage(openAnnotationDialog
                    .getContentId(), openAnnotationDialog, getModelObject(), originSpanId,
                    originSpanType, targetSpanId, targetSpanType, selectedArcId, selectedArcType));
        }

        openAnnotationDialog.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = -1746088901018629567L;

            @Override
            public void onClose(AjaxRequestTarget aTarget)
            {
                onChange(aTarget, getModelObject());
                reloadContent(aTarget);

            }
        });
        // open the annotation dialog if only there is
        // span annotation layer (from the settings button) selected
        for (TagSet tagSet : getModelObject().getAnnotationLayers()) {
            if (tagSet.getType().getType().equals(AnnotationTypeConstant.SPAN_TYPE)) {
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

    private JCas getCas(Project aProject, User user, SourceDocument aDocument, Mode aMode)
        throws UIMAException, IOException, ClassNotFoundException
    {
        if (aMode.equals(Mode.ANNOTATION) || aMode.equals(Mode.CORRECTION)
                || aMode.equals(Mode.CORRECTION_MERGE)) {
            BratAjaxCasController controller = new BratAjaxCasController(repository,
                    annotationService);

            return controller.getJCas(aDocument, aProject, user);
        }
        else {
            return repository.getCurationDocumentContent(getModelObject().getDocument());
        }
    }

    public static class MultipleSentenceCoveredException
        extends Exception
    {
        private static final long serialVersionUID = 1280015349963924638L;

        public MultipleSentenceCoveredException(String message)
        {
            super(message);
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
        // TODO Auto-generated method stub

    }

    private JCas getCas(BratAnnotatorModel aBratAnnotatorModel)
            throws UIMAException, IOException, ClassNotFoundException
        {

            if (aBratAnnotatorModel.getMode().equals(Mode.ANNOTATION)
                    || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)
                    || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION_MERGE)) {
                BratAjaxCasController controller = new BratAjaxCasController(repository,
                        annotationService);

                return controller.getJCas(aBratAnnotatorModel.getDocument(),
                        aBratAnnotatorModel.getProject(), aBratAnnotatorModel.getUser());
            }
            else {
                return repository.getCurationDocumentContent(getModelObject().getDocument());
            }
        }
}