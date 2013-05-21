/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.persistence.NoResultException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.beans.BeansException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * Base class for displaying a BRAT visualization. Override methods {@link #getCollectionData()} and
 * {@link #getDocumentData()} to provide the actual data.
 *
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
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

    public Label numberOfPages;
    public Label documentNameLabel;
    private int sentenceNumber = 1;
    private int totalNumberOfSentence;

    private long currentDocumentId;
    private long currentprojectId;

    /**
     * Data models for {@link BratAnnotator}
     */
    public BratAnnotatorModel bratAnnotatorModel = new BratAnnotatorModel();

    public BratAnnotator(String id, IModel<?> aModel)
    {
        super(id, aModel);

        // This is an Annotation Operation, set model to ANNOTATION mode
        bratAnnotatorModel.setMode(Mode.ANNOTATION);

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);

        final FeedbackPanel feedbackPanel = new FeedbackPanel("feedbackPanel");
        add(feedbackPanel);
        feedbackPanel.setOutputMarkupId(true);
        feedbackPanel.add(new SimpleAttributeModifier("class", "info"));
        feedbackPanel.add(new SimpleAttributeModifier("class", "error"));

        // Add project and document information at the top

        add(documentNameLabel = (Label) new Label("doumentName", new LoadableDetachableModel()
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected String load()
            {
                String projectName;
                String documentName;
                if (bratAnnotatorModel.getProject() == null) {
                    projectName = "/";
                }
                else {
                    projectName = bratAnnotatorModel.getProject().getName() + "/";
                }
                if (bratAnnotatorModel.getDocument() == null) {
                    documentName = "";
                }
                else {
                    documentName = bratAnnotatorModel.getDocument().getName();
                }
                return projectName + documentName;

            }
        }).setOutputMarkupId(true));

        add(numberOfPages = (Label) new Label("numberOfPages", new LoadableDetachableModel()
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected String load()
            {
                if (bratAnnotatorModel.getDocument() != null) {
                    try {
                        JCas jCas = getCas(bratAnnotatorModel.getProject(),
                                bratAnnotatorModel.getUser(), bratAnnotatorModel.getDocument());
                        totalNumberOfSentence = BratAjaxCasUtil.getNumberOfPages(jCas,
                                bratAnnotatorModel.getWindowSize());

                        // If only one page, start displaying from sentence 1
                        if (totalNumberOfSentence == 1) {
                            bratAnnotatorModel.setSentenceAddress(bratAnnotatorModel
                                    .getFirstSentenceAddress());
                        }
                        sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas,
                                bratAnnotatorModel.getSentenceAddress());
                        int firstSentenceNumber = sentenceNumber + 1;
                        int lastSentenceNumber;
                        if (firstSentenceNumber + bratAnnotatorModel.getWindowSize() - 1 < totalNumberOfSentence) {
                            lastSentenceNumber = firstSentenceNumber
                                    + bratAnnotatorModel.getWindowSize() - 1;
                        }
                        else {
                            lastSentenceNumber = totalNumberOfSentence;
                        }

                        return "showing " + firstSentenceNumber + "-" + lastSentenceNumber + " of "
                                + totalNumberOfSentence + " sentences";
                    }
                    // No need to report error, already reported in getDocument below
                    catch (DataRetrievalFailureException ex) {
                        // error(ExceptionUtils.getRootCauseMessage(ex));
                        return "";
                    }
                    // No need to report error, already reported in getDocument below
                    catch (UIMAException e) {
                        // error(ExceptionUtils.getRootCauseMessage(e));
                        return "";
                    }
                    catch (IOException e) {
                        return "";
                    }

                }
                else {
                    return "";// no document yet selected
                }

            }
        }).setOutputMarkupId(true));

        controller = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
                BratAnnotatorUIData uIData = new BratAnnotatorUIData();
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                bratAnnotatorModel.setUser(repository.getUser(username));

                final IRequestParameters request = getRequest().getPostParameters();

                Object result = null;
                BratAjaxCasController controller = new BratAjaxCasController(
                        repository, annotationService);

                if (request.getParameterValue("action").toString().equals("whoami")) {
                    result = controller.whoami();
                }
                else if (request.getParameterValue("action").toString().equals("storeSVG")) {
                    result = controller.storeSVG();
                }
                else if (request.getParameterValue("action").toString().equals("loadConf")) {
                    result = controller.loadConf();
                }
                else if (request.getParameterValue("action").toString()
                        .equals("getCollectionInformation")) {
                    try {
                        setAttributesForGetCollection(request.getParameterValue("collection")
                                .toString());
                    }
                    catch (IOException e) {
                        error("unable to find annotation preferences from file "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    result = controller.getCollectionInformation(
                            request.getParameterValue("collection").toString(),
                            bratAnnotatorModel.getAnnotationLayers());

                }

                else if (request.getParameterValue("action").toString().equals("getDocument")) {
                    String collection = request.getParameterValue("collection").toString();
                    String documentName;
                    if (bratAnnotatorModel.getDocumentName() == null) {
                        documentName = request.getParameterValue("document").toString();
                    }
                    else {
                        documentName = bratAnnotatorModel.getDocumentName();
                    }

                    boolean firstTimeDocumentOpened = isDocumentOpenedFirstTime(collection,
                            documentName);

                    result = getDocument(collection, documentName, bratAnnotatorModel.getUser(),
                            uIData);

                    if (firstTimeDocumentOpened) {
                        info("Document is opened for the first time. "
                                + "Initial conversion from <"
                                + bratAnnotatorModel.getDocument().getFormat()
                                + "> has been performed.");
                    }
                    // update source document
                    bratAnnotatorModel.setDocument(repository.getSourceDocument(documentName,
                            bratAnnotatorModel.getProject()));
                }
                else if (request.getParameterValue("action").toString().equals("createSpan")) {
                    try {
                        if (!isDocumentFinished()) {
                            result = createSpan(request, bratAnnotatorModel.getUser(), uIData);
                            info("Annotation [" + request.getParameterValue("type").toString()
                                    + "]has been created");
                        }
                        else {
                            error("This document is already closed. Please ask admin to re-open");
                            String collection = request.getParameterValue("collection").toString();
                            String documentName = request.getParameterValue("document").toString();
                            result = getDocument(collection, documentName,
                                    bratAnnotatorModel.getUser(), uIData);
                        }
                    }

                    catch (Exception e) {
                        info(e);
                        String collection = request.getParameterValue("collection").toString();
                        String documentName = request.getParameterValue("document").toString();
                        result = getDocument(collection, documentName,
                                bratAnnotatorModel.getUser(), uIData);
                    }

                }

                else if (request.getParameterValue("action").toString().equals("createArc")) {
                    if (!isDocumentFinished()) {
                        result = createArc(request, bratAnnotatorModel.getUser(), uIData);
                        info("Annotation [" + request.getParameterValue("type").toString()
                                + "]has been created");
                    }
                    else {
                        error("This document is already closed. Please ask admin to re-open");
                        String collection = request.getParameterValue("collection").toString();
                        String documentName = request.getParameterValue("document").toString();
                        result = getDocument(collection, documentName,
                                bratAnnotatorModel.getUser(), uIData);
                    }
                }

                else if (request.getParameterValue("action").toString().equals("reverseArc")) {
                    if (!isDocumentFinished()) {
                        result = reverseArc(request, bratAnnotatorModel.getUser(), uIData);
                        info("Annotation [" + request.getParameterValue("type").toString()
                                + "]has been reversed");
                    }
                    else {
                        error("This document is already closed. Please ask admin to re-open");
                        String collection = request.getParameterValue("collection").toString();
                        String documentName = request.getParameterValue("document").toString();
                        result = getDocument(collection, documentName,
                                bratAnnotatorModel.getUser(), uIData);
                    }
                }
                else if (request.getParameterValue("action").toString().equals("deleteSpan")) {
                    if (!isDocumentFinished()) {
                        result = deleteSpan(request, bratAnnotatorModel.getUser(), uIData);
                        info("Annotation [" + request.getParameterValue("type").toString()
                                + "]has been deleted");
                    }
                    else {
                        error("This document is already closed. Please ask admin to re-open");
                        String collection = request.getParameterValue("collection").toString();
                        String documentName = request.getParameterValue("document").toString();
                        result = getDocument(collection, documentName,
                                bratAnnotatorModel.getUser(), uIData);
                    }
                }

                else if (request.getParameterValue("action").toString().equals("deleteArc")) {
                    if (!isDocumentFinished()) {
                        result = deleteArc(request, bratAnnotatorModel.getUser(), uIData);
                        info("Annotation [" + request.getParameterValue("type").toString()
                                + "]has been deleted");
                    }
                    else {
                        error("This document is already closed. Please ask admin to re-open");
                        String collection = request.getParameterValue("collection").toString();
                        String documentName = request.getParameterValue("document").toString();
                        result = getDocument(collection, documentName,
                                bratAnnotatorModel.getUser(), uIData);
                    }
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
                aTarget.add(numberOfPages);
                aTarget.add(documentNameLabel);
                aTarget.add(feedbackPanel);
            }
        };

        add(vis);
        add(controller);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        String[] script = new String[] { "dispatcher = new Dispatcher();"
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

        // This doesn't work with head.js because the onLoad event is fired before all the
        // JavaScript references are loaded.
        aResponse.renderOnLoadJavaScript("\n" + StringUtils.join(script, "\n"));
    }

    private Object getDocument(String aCollection, String aDocumentName, User aUser,
            BratAnnotatorUIData aUIData)
    {
        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController( repository,
                annotationService);

        try {
            {
                setAttributesForDocument( aUIData);
            }
            aUIData.setGetDocument(true);
            result = controller.getDocumentResponse(bratAnnotatorModel, aUIData);
            aUIData.setGetDocument(false);
        }
        catch (UIMAException e) {
            error("Error while Processing the CAS object " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (IOException e) {
            error("Error while getting/setting the annotation/source document from File " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (ClassNotFoundException e) {
            error("The Class name in the properties is not found " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (DataRetrievalFailureException ex) {
            error(ExceptionUtils.getRootCauseMessage(ex));
        }
        return result;
    }

    private Object createSpan(IRequestParameters aRequest, User aUser, BratAnnotatorUIData aUIData)
        throws Exception
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController( repository,
                annotationService);
        String offsets = aRequest.getParameterValue("offsets").toString();
        OffsetsList offsetList = null;
        try {
            offsetList = jsonConverter.getObjectMapper().readValue(offsets, OffsetsList.class);
        }
        catch (JsonParseException e1) {
            error("Inavlid Json Object sent from Brat :" + ExceptionUtils.getRootCauseMessage(e1));
        }
        catch (JsonMappingException e1) {
            error("Inavlid Json Object sent from Brat :" + ExceptionUtils.getRootCauseMessage(e1));
        }
        catch (IOException e1) {
            error("Inavlid Json Object sent from Brat :" + ExceptionUtils.getRootCauseMessage(e1));
        }

        try {
            OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(offsets,
                    OffsetsList.class);
            int start = offsetLists.get(0).getBegin();
            int end = offsetLists.get(0).getEnd();
            setAttributesForDocument(aUIData);
            aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress())
                    + start);
            aUIData.setAnnotationOffsetEnd(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress())
                    + end);
            aUIData.setType(aRequest.getParameterValue("type").toString());

            if (!BratAjaxCasUtil.offsetsInOneSentences(aUIData.getjCas(),
                    aUIData.getAnnotationOffsetStart(), aUIData.getAnnotationOffsetEnd())) {
                throw new Exception(
                        "Annotation coveres multiple sentence, Limit your annotation to single sentence");
            }
            result = controller.createSpanResponse(bratAnnotatorModel, aUIData);
            if (bratAnnotatorModel.isScrollPage()) {
                bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                        aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                        aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                        bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
            }
        }
        catch (JsonParseException e) {
            error("Error while parsing the JSON value sent from Brat " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (JsonMappingException e) {
            error("Error while Mapping JSON value to OffsetsLists " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (UIMAException e) {
            error("Error while Processing the CAS object " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (IOException e) {
            error("Error while getting/setting the annotation/source document from File " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (DataRetrievalFailureException ex) {
            error(ExceptionUtils.getRootCauseMessage(ex));
        }
        return result;
    }

    private Object createArc(IRequestParameters aRequest, User aUser, BratAnnotatorUIData aUIData)
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController( repository,
                annotationService);
        try {
            setAttributesForDocument(aUIData);
            aUIData.setOrigin(aRequest.getParameterValue("origin").toInt());
            aUIData.setTarget(aRequest.getParameterValue("target").toInt());
            aUIData.setType(aRequest.getParameterValue("type").toString());
            aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), aUIData.getOrigin()));
            result = controller.createArcResponse(bratAnnotatorModel, aUIData);
            if (bratAnnotatorModel.isScrollPage()) {
                bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                        aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                        aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                        bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
            }
        }
        catch (UIMAException e) {
            error("Error while Processing the CAS object " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (IOException e) {
            error("Error while getting/setting the annotation/source document from File " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        return result;
    }

    private Object reverseArc(IRequestParameters aRequest, User aUser, BratAnnotatorUIData aUIData)
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController( repository,
                annotationService);

        try {
            setAttributesForDocument(aUIData);
            aUIData.setOrigin(aRequest.getParameterValue("origin").toInt());
            aUIData.setTarget(aRequest.getParameterValue("target").toInt());
            aUIData.setType(aRequest.getParameterValue("type").toString());
            aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), aUIData.getOrigin()));

            String annotationType = aUIData.getType().substring(0,
                    aUIData.getType().indexOf(AnnotationTypeConstant.PREFIX) + 1);
            if (annotationType.equals(AnnotationTypeConstant.POS_PREFIX)) {
                result = controller.reverseArcResponse(bratAnnotatorModel, aUIData);
                if (bratAnnotatorModel.isScrollPage()) {
                    bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                            aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                            aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                            bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
                }
            }
        }
        catch (UIMAException e) {
            error("Error while Processing the CAS object " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (IOException e) {
            error("Error while getting/setting the annotation/source document from File " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        return result;
    }

    private Object deleteSpan(IRequestParameters aRequest, User aUser, BratAnnotatorUIData aUIData)
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository,
                annotationService);

        try {
            String offsets = aRequest.getParameterValue("offsets").toString();
            int id = aRequest.getParameterValue("id").toInt();
            OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(offsets,
                    OffsetsList.class);
            int start = offsetLists.get(0).getBegin();
            int end = offsetLists.get(0).getEnd();
            setAttributesForDocument( aUIData);
            aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress())
                    + start);
            aUIData.setAnnotationOffsetEnd(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress())
                    + end);
            aUIData.setType(aRequest.getParameterValue("type").toString());
            result = controller.deleteSpanResponse(bratAnnotatorModel, id, aUIData);
            if (bratAnnotatorModel.isScrollPage()) {
                bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                        aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                        aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                        bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
            }

        }
        catch (JsonParseException e) {
            error("Error while parsing the JSON value sent from Brat " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (JsonMappingException e) {
            error("Error while Mapping JSON value to OffsetsLists " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (UIMAException e) {
            error("Error while Processing the CAS object " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (IOException e) {
            error("Error while getting/setting the annotation/source document from File " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        return result;
    }

    private Object deleteArc(IRequestParameters aRequest, User aUser, BratAnnotatorUIData aUIData)
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository,
                annotationService);

        try {
            setAttributesForDocument(aUIData);
            aUIData.setOrigin(aRequest.getParameterValue("origin").toInt());
            aUIData.setTarget(aRequest.getParameterValue("target").toInt());
            aUIData.setType(aRequest.getParameterValue("type").toString());
            aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), aUIData.getOrigin()));

            result = controller.deleteArcResponse(bratAnnotatorModel, aUIData);
            if (bratAnnotatorModel.isScrollPage()) {
                bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                        aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                        aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                        bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
            }

        }
        catch (UIMAException e) {
            error("Error while Processing the CAS object " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (IOException e) {
            error("Error while getting/setting the annotation/source document from File " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        return result;
    }

    /**
     * Set different attributes for
     * {@link BratAjaxCasController#getDocument(int, Project, SourceDocument, User, int, int, boolean, ArrayList)}
     *
     * @throws UIMAException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void setAttributesForDocument(
            BratAnnotatorUIData aUIData)
        throws UIMAException, IOException
    {

        aUIData.setjCas(getCas(bratAnnotatorModel.getProject(), bratAnnotatorModel.getUser(),
                bratAnnotatorModel.getDocument()));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (bratAnnotatorModel.getSentenceAddress() == -1
                || bratAnnotatorModel.getDocument().getId() != currentDocumentId
                || bratAnnotatorModel.getProject().getId() != currentprojectId) {

            try {
                bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil
                        .getFirstSenetnceAddress(aUIData.getjCas()));
                bratAnnotatorModel.setLastSentenceAddress(BratAjaxCasUtil
                        .getLastSenetnceAddress(aUIData.getjCas()));
                bratAnnotatorModel.setFirstSentenceAddress(bratAnnotatorModel.getSentenceAddress());

                AnnotationPreference preference = new AnnotationPreference();
                ApplicationUtils.setAnnotationPreference(preference, username,repository,
                        annotationService, bratAnnotatorModel, Mode.ANNOTATION);
            }
            catch (DataRetrievalFailureException ex) {
                throw ex;
            }
            catch (BeansException e) {
                throw e;
            }
            catch (FileNotFoundException e) {
                throw e;
            }
            catch (IOException e) {
                throw e;
            }
        }

        currentprojectId = bratAnnotatorModel.getProject().getId();
        currentDocumentId = bratAnnotatorModel.getDocument().getId();
    }

    /**
     * Set different attributes for
     * {@link BratAjaxCasController#getCollectionInformation(String, ArrayList) }
     *
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void setAttributesForGetCollection(String aProjectName)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!aProjectName.equals("/")) {
            bratAnnotatorModel.setProject(repository.getProject(aProjectName.replace("/", "")));

            if (bratAnnotatorModel.getProject().getId() != currentprojectId) {
                AnnotationPreference preference = new AnnotationPreference();
                try {
                    ApplicationUtils.setAnnotationPreference(preference, username,repository,
                            annotationService, bratAnnotatorModel, Mode.ANNOTATION);
                }
                catch (BeansException e) {
                    throw e;
                }
                catch (FileNotFoundException e) {
                    throw e;
                }
                catch (IOException e) {
                    throw e;
                }
            }
            currentprojectId = bratAnnotatorModel.getProject().getId();
        }
    }

    boolean isDocumentOpenedFirstTime(String aCollection, String adocumentName)
    {
        bratAnnotatorModel.setProject(repository.getProject(aCollection.replace("/", "")));
        bratAnnotatorModel.setDocument(repository.getSourceDocument(adocumentName,
                bratAnnotatorModel.getProject()));

        try {
            repository.getAnnotationDocument(bratAnnotatorModel.getDocument(),
                    bratAnnotatorModel.getUser());
            return false;
        }
        catch (NoResultException e) {
            return true;
        }
    }

    private JCas getCas(Project aProject, User user, SourceDocument aDocument)
        throws UIMAException, IOException
    {
        JCas jCas = null;
        try {
            BratAjaxCasController controller = new BratAjaxCasController(repository,
                    annotationService);
            jCas = controller.getJCas(aDocument, aProject, user);
        }
        catch (UIMAException e) {
            error("CAS object not found :" + ExceptionUtils.getRootCauseMessage(e));
            throw e;
        }
        catch (IOException e) {
            error("Unable to read CAS object: " + ExceptionUtils.getRootCauseMessage(e));
            throw e;
        }
        catch (ClassNotFoundException e) {
            error("The Class name in the properties is not found " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (DataRetrievalFailureException ex) {
            throw ex;
        }
        return jCas;
    }

    private boolean isDocumentFinished()
    {
        // if annotationDocument is finished, disable editing
        boolean finished = false;
        try {
            if (repository
                    .getAnnotationDocument(bratAnnotatorModel.getDocument(),
                            bratAnnotatorModel.getUser()).getState()
                    .equals(AnnotationDocumentState.FINISHED)
                    || bratAnnotatorModel.getDocument().getState()
                            .equals(SourceDocumentState.CURATION_FINISHED)
                    || bratAnnotatorModel.getDocument().getState()
                            .equals(SourceDocumentState.CURATION_INPROGRESS)) {
                finished = true;
            }
        }
        catch (Exception e) {
            finished = false;
        }

        return finished;
    }
}
