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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorUIData;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * Base class for displaying a BRAT visualization. Override methods {@link #getCollectionData()} and
 * {@link #getDocumentData()} to provide the actual data.
 *
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 * @author Andreas Straninger
 */
public class BratCurationDocumentEditor
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

    private String rewriteUrl = "";

    private String collection = "";

    private String document = "";

    /**
     * Data models for {@link BratCurationDocumentEditor}
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


    public BratCurationDocumentEditor(String id, IModel<BratAnnotatorModel> aModel)
    {
        super(id, aModel);

        if(getModelObject().getDocument() != null) {
            collection = "#" + getModelObject().getProject().getName() + "/";
            document = getModelObject().getDocument().getName();
            this.rewriteUrl = collection + document;
        }

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);


        controller = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
            	boolean hasChanged = false;
                BratAnnotatorUIData uIData = new BratAnnotatorUIData();
                try {
					uIData.setjCas(getCas());
				} catch (UIMAException e1) {
					error(ExceptionUtils.getRootCause(e1));
				} catch (IOException e1) {
				    error(ExceptionUtils.getRootCause(e1));
				}

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
                    result = controller.getCollectionInformation(
                    		getModelObject().getProject().getName(),
                            getModelObject().getAnnotationLayers());

                }

                else if (request.getParameterValue("action").toString().equals("getDocument")) {
                    result = getDocument(getModelObject().getDocument(), getModelObject().getUser(), uIData);

                }
                else if (request.getParameterValue("action").toString().equals("createSpan")) {
                    try {
                            result = createSpan(request, getModelObject().getUser(), uIData);
                            info("Annotation [" + request.getParameterValue("type").toString()
                                    + "]has been created");
                            hasChanged = true;
                    }

                    catch (Exception e) {
                        info(e);
                        result = getDocument(request, getModelObject().getUser(), uIData);
                    }

                }

                else if (request.getParameterValue("action").toString().equals("createArc")) {
                        result = createArc(request, getModelObject().getUser(), uIData);
                        info("Annotation [" + request.getParameterValue("type").toString()
                                + "]has been created");
                        hasChanged = true;
                }

                else if (request.getParameterValue("action").toString().equals("reverseArc")) {
                        result = reverseArc(request, getModelObject().getUser(), uIData);
                        info("Annotation [" + request.getParameterValue("type").toString()
                                + "]has been reversed");
                        hasChanged = true;
                }
                else if (request.getParameterValue("action").toString().equals("deleteSpan")) {
                        result = deleteSpan(request, getModelObject().getUser(), uIData);
                        info("Annotation [" + request.getParameterValue("type").toString()
                                + "]has been deleted");
                        hasChanged = true;
                }

                else if (request.getParameterValue("action").toString().equals("deleteArc")) {
                        result = deleteArc(request, getModelObject().getUser(), uIData);
                        info("Annotation [" + request.getParameterValue("type").toString()
                                + "]has been deleted");
                        hasChanged = true;
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
                if(hasChanged) {
                	onChange(aTarget);
                }
            }
        };

        add(vis);
        add(controller);

    }

    protected void onChange(AjaxRequestTarget aTarget) {
    	// Overriden in curationPanel
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
//                + "var urlMonitor = new URLMonitor(dispatcher); "
                + "var ajax = new Ajax(dispatcher);" + "var ajax_" + vis.getMarkupId() + " = ajax;"
                + "var visualizer = new Visualizer(dispatcher, '" + vis.getMarkupId() + "');"
                + "var visualizerUI = new VisualizerUI(dispatcher, visualizer.svg);"
                + "var annotatorUI = new AnnotatorUI(dispatcher, visualizer.svg);"
                + "var spinner = new Spinner(dispatcher, '#spinner');"
                + "var logger = new AnnotationLog(dispatcher);" + "dispatcher.post('init');"
                //+ "window.location.hash = '" + rewriteUrl + "';"
                //+ "dispatcher.post('setCollection', ['"+collection+"', '"+document+"', {}]);"
//                + "dispatcher.post('clearSVG', []);"
//                + "dispatcher.post('current', ['"+collection+"', '1234', {}, true]);"
//                + "dispatcher.post('ajax', [{action: 'getCollectionInformation',collection: '"+collection+"'}, 'collectionLoaded', {collection: '"+collection+"',keep: true}]);"
                //+ "dispatcher.post('collectionChanged');"
                };

        // This doesn't work with head.js because the onLoad event is fired before all the
        // JavaScript references are loaded.
        aResponse.renderOnLoadJavaScript("\n" + StringUtils.join(script, "\n"));
    }

    public void reloadContent(AjaxRequestTarget aTarget) {
        String[] script = new String[] { "dispatcher.post('clearSVG', []);"
                + "dispatcher.post('current', ['"+collection+"', '1234', {}, true]);"
                // start ajax call, which requests the collection (and the document) from the server and renders the svg
                + "dispatcher.post('ajax', [{action: 'getCollectionInformation',collection: '"+collection+"'}, 'collectionLoaded', {collection: '"+collection+"',keep: true}]);"
                //+ "dispatcher.post('collectionChanged');"
                };
    	aTarget.appendJavaScript("\n" + StringUtils.join(script, "\n"));
    }

    private Object getDocument(IRequestParameters aRequest, User aUser, BratAnnotatorUIData aUIData)
    {
        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(
                repository, annotationService);
        String collection = aRequest.getParameterValue("collection").toString();
        String documentName = aRequest.getParameterValue("document").toString();

        try {
            aUIData.setGetDocument(true);
            result = controller.getDocumentResponse(getModelObject(), aUIData);
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

    private Object getDocument(SourceDocument aSourceDocument, User aUser, BratAnnotatorUIData aUIData)
    {
    	Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(
                repository, annotationService);

    	try {
    		aUIData.setGetDocument(true);
    		result = controller.getDocumentResponse(getModelObject(), aUIData);
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
        BratAjaxCasController controller = new BratAjaxCasController(
                repository, annotationService);
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
            aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), getModelObject().getSentenceAddress()) + start);
            aUIData.setAnnotationOffsetEnd(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), getModelObject().getSentenceAddress()) + end);
            aUIData.setType(aRequest.getParameterValue("type").toString());

            if (!BratAjaxCasUtil.offsetsInOneSentences(aUIData.getjCas(),
                    aUIData.getAnnotationOffsetStart(),
                    aUIData.getAnnotationOffsetEnd())) {
                throw new Exception(
                        "Annotation coveres multiple sentences, limit your annotation to single sentence!");
            }
            result = controller.createSpanResponse(getModelObject(), aUIData);
            if (getModelObject().isScrollPage()) {
                getModelObject().setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                        aUIData.getjCas(), getModelObject().getSentenceAddress(),
                        aUIData.getAnnotationOffsetStart(),
                        getModelObject().getProject(), getModelObject().getDocument(),
                        getModelObject().getWindowSize()));
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
        BratAjaxCasController controller = new BratAjaxCasController(
                repository, annotationService);
        try {
            aUIData.setOrigin(aRequest.getParameterValue("origin").toInt());
            aUIData.setTarget(aRequest.getParameterValue("target").toInt());
            aUIData.setType(aRequest.getParameterValue("type").toString());
            aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), aUIData.getOrigin()));
            result = controller.createArcResponse(getModelObject(), aUIData);
            if (getModelObject().isScrollPage()) {
                getModelObject().setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                        aUIData.getjCas(), getModelObject().getSentenceAddress(),
                        aUIData.getAnnotationOffsetStart(),
                        getModelObject().getProject(), getModelObject().getDocument(),
                        getModelObject().getWindowSize()));
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
        BratAjaxCasController controller = new BratAjaxCasController(
                repository, annotationService);

        try {
            aUIData.setOrigin(aRequest.getParameterValue("origin").toInt());
            aUIData.setTarget(aRequest.getParameterValue("target").toInt());
            aUIData.setType(aRequest.getParameterValue("type").toString());
            aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), aUIData.getOrigin()));

            String annotationType = aUIData.getType().substring(0,
                    aUIData.getType().indexOf(AnnotationTypeConstant.PREFIX) + 1);
            if (annotationType.equals(AnnotationTypeConstant.POS_PREFIX)) {
                result = controller.reverseArcResponse(getModelObject(), aUIData);
                if (getModelObject().isScrollPage()) {
                    getModelObject().setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                            aUIData.getjCas(), getModelObject().getSentenceAddress(),
                            aUIData.getAnnotationOffsetStart(),
                            getModelObject().getProject(), getModelObject().getDocument(),
                            getModelObject().getWindowSize()));
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
        BratAjaxCasController controller = new BratAjaxCasController(
                repository, annotationService);

        try {
            String offsets = aRequest.getParameterValue("offsets").toString();
            int id = aRequest.getParameterValue("id").toInt();
            OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(offsets,
                    OffsetsList.class);
            int start = offsetLists.get(0).getBegin();
            int end = offsetLists.get(0).getEnd();
            aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), getModelObject().getSentenceAddress()) + start);
            aUIData.setAnnotationOffsetEnd(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), getModelObject().getSentenceAddress()) + end);
            aUIData.setType(aRequest.getParameterValue("type").toString());
            result = controller.deleteSpanResponse(getModelObject(), id, aUIData);
            if (getModelObject().isScrollPage()) {
                getModelObject().setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                        aUIData.getjCas(), getModelObject().getSentenceAddress(),
                        aUIData.getAnnotationOffsetStart(),
                        getModelObject().getProject(), getModelObject().getDocument(),
                        getModelObject().getWindowSize()));
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
        BratAjaxCasController controller = new BratAjaxCasController(
                repository, annotationService);

        try {
            aUIData.setOrigin(aRequest.getParameterValue("origin").toInt());
            aUIData.setTarget(aRequest.getParameterValue("target").toInt());
            aUIData.setType(aRequest.getParameterValue("type").toString());
            aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                    aUIData.getjCas(), aUIData.getOrigin()));

            result = controller.deleteArcResponse(getModelObject(), aUIData);
            if (getModelObject().isScrollPage()) {
                getModelObject().setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                        aUIData.getjCas(), getModelObject().getSentenceAddress(),
                        aUIData.getAnnotationOffsetStart(),
                        getModelObject().getProject(), getModelObject().getDocument(),
                        getModelObject().getWindowSize()));
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

    private JCas getCas()
            throws UIMAException, IOException
        {
            JCas jCas = null;
            try {
                BratAjaxCasController controller = new BratAjaxCasController(
                        repository, annotationService);
                if(getModelObject().getDocument() != null) {
                	jCas = repository.getCurationDocumentContent(getModelObject().getDocument());
                }
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

}
