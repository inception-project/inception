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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.OffsetsList;
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
    private int pageNumber = 1;
    private int totalPageNumber;
    private ArrayList<TagSet> annotationLayers = new ArrayList<TagSet>();
    private Project project;
    private SourceDocument document;

    private int sentenceAddress = -1;
    private int lastSentenceAddress;
    private int firstSentenceAddress;
    private int windowSize = 10;
    private boolean isDisplayLemmaSelected = true;
    private boolean scrollPage = false;
    /**
     * store document and project names to compare with the current and previous document/project
     */
    private long currentDocumentId;
    private long currentprojectId;

    public BratAnnotator(String id, IModel<?> aModel)
    {
        super(id, aModel);

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);

        add(numberOfPages = (Label) new Label("numberOfPages", new LoadableDetachableModel()
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected String load()
            {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);
                JCas jCas = null;
                if (document != null) {
                    try {
                        BratAjaxCasController controller = new BratAjaxCasController(jsonConverter,
                                repository, annotationService);
                        jCas = controller.getJCas(document, project, user);
                        totalPageNumber = BratAjaxCasUtil.getNumberOfPages(jCas, windowSize);

                        // If only one page, start displaying from sentence 1
                        if (totalPageNumber == 1) {
                            sentenceAddress = firstSentenceAddress;
                        }
                        pageNumber = BratAjaxCasUtil.getPageNumber(jCas, windowSize,
                                sentenceAddress);
                        return pageNumber + " of " + totalPageNumber + " pages";
                    }
                    catch (UIMAException e) {
                        error("CAS object not found :" + ExceptionUtils.getRootCauseMessage(e));
                        return "";
                    }
                    catch (IOException e) {
                        error("Unable to get CAS object :" + ExceptionUtils.getRootCauseMessage(e));
                        return "";
                    }
                    catch (ClassNotFoundException e) {
                        error("The Class name in the properties is not found " + ":"
                                + ExceptionUtils.getRootCauseMessage(e));
                        return "";
                    }

                    catch (DataRetrievalFailureException ex) {
                        error(ExceptionUtils.getRootCauseMessage(ex));
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

                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);

                final IRequestParameters request = getRequest().getPostParameters();

                Object result = null;
                BratAjaxCasController controller = new BratAjaxCasController(jsonConverter,
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
                    setAttributesForGetCollection(request.getParameterValue("collection")
                            .toString());
                    result = controller.getCollectionInformation(
                            request.getParameterValue("collection").toString(), annotationLayers);
                }

                else if (request.getParameterValue("action").toString().equals("getDocument")) {
                    String collection = request.getParameterValue("collection").toString();
                    String documentName = request.getParameterValue("document").toString();

                    try {
                        setAttributesForGetDocument(collection, documentName);
                        result = controller.getDocument(windowSize, project, document, user,
                                sentenceAddress, lastSentenceAddress, isDisplayLemmaSelected,
                                annotationLayers);
                    }
                    catch (UIMAException e) {
                        error("Error while Processing the CAS object " + ":"
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (IOException e) {
                        error("Error while getting/setting the annotation/source document from File "
                                + ":" + ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        error("The Class name in the properties is not found " + ":"
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (DataRetrievalFailureException ex) {
                        error(ExceptionUtils.getRootCauseMessage(ex));
                    }
                }
                else if (request.getParameterValue("action").toString().equals("createSpan")) {

                    String offsets = request.getParameterValue("offsets").toString();
                    OffsetsList offsetList = null;
                    try {
                        offsetList = jsonConverter.getObjectMapper().readValue(offsets,
                                OffsetsList.class);
                    }
                    catch (JsonParseException e1) {
                        error("Inavlid Json Object sent from Brat :"
                                + ExceptionUtils.getRootCauseMessage(e1));
                    }
                    catch (JsonMappingException e1) {
                        error("Inavlid Json Object sent from Brat :"
                                + ExceptionUtils.getRootCauseMessage(e1));
                    }
                    catch (IOException e1) {
                        error("Inavlid Json Object sent from Brat :"
                                + ExceptionUtils.getRootCauseMessage(e1));
                    }
                    String type = request.getParameterValue("type").toString();

                    try {
                        OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(
                                offsets, OffsetsList.class);
                        int start = offsetLists.get(0).getBegin();
                        int end = offsetLists.get(0).getEnd();
                        JCas jCas = getCas(project, user, document);
                        int annotationOffsetStart = BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                                sentenceAddress) + start;
                        int annotationOffsetEnd = BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                                sentenceAddress) + end;

                        result = controller.createSpan(windowSize, project, document, user,
                                sentenceAddress, lastSentenceAddress, annotationOffsetStart,
                                annotationOffsetEnd, type, isDisplayLemmaSelected,
                                annotationLayers, jCas, scrollPage);
                        if (scrollPage) {
                            setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas,
                                    sentenceAddress, annotationOffsetStart, project, document,
                                    windowSize));
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
                        error("Error while getting/setting the annotation/source document from File "
                                + ":" + ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (DataRetrievalFailureException ex) {
                        error(ExceptionUtils.getRootCauseMessage(ex));
                    }
                }

                else if (request.getParameterValue("action").toString().equals("createArc")) {
                    String origin = request.getParameterValue("origin").toString();
                    String target = request.getParameterValue("target").toString();
                    String type = request.getParameterValue("type").toString();

                    try {
                        JCas jCas = getCas(project, user, document);
                        int annotationOffsetStart = BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                                Integer.parseInt(origin));
                        result = controller.createArc(windowSize, project, document, user,
                                sentenceAddress, lastSentenceAddress, annotationOffsetStart,
                                origin, target, type, isDisplayLemmaSelected, annotationLayers,
                                windowSize, jCas, scrollPage);
                        if (scrollPage) {
                            setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas,
                                    sentenceAddress, annotationOffsetStart, project, document,
                                    windowSize));
                        }
                    }
                    catch (UIMAException e) {
                        error("Error while Processing the CAS object " + ":"
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (IOException e) {
                        error("Error while getting/setting the annotation/source document from File "
                                + ":" + ExceptionUtils.getRootCauseMessage(e));
                    }
                }

                else if (request.getParameterValue("action").toString().equals("reverseArc")) {
                    String origin = request.getParameterValue("origin").toString();
                    String target = request.getParameterValue("target").toString();
                    String type = request.getParameterValue("type").toString();

                    try {
                        JCas jCas = getCas(project, user, document);
                        int annotationOffsetStart = BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                                Integer.parseInt(origin));

                        String annotationType = type.substring(0,
                                type.indexOf(AnnotationType.PREFIX) + 1);
                        if (annotationType.equals(AnnotationType.POS_PREFIX)) {
                            result = controller.reverseArc(windowSize, project, document, user,
                                    sentenceAddress, lastSentenceAddress, annotationOffsetStart,
                                    origin, target, type, isDisplayLemmaSelected, annotationLayers,
                                    windowSize, jCas, scrollPage);
                            if (scrollPage) {
                                setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas,
                                        sentenceAddress, annotationOffsetStart, project, document,
                                        windowSize));
                            }
                        }
                    }
                    catch (UIMAException e) {
                        error("Error while Processing the CAS object " + ":"
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (IOException e) {
                        error("Error while getting/setting the annotation/source document from File "
                                + ":" + ExceptionUtils.getRootCauseMessage(e));
                    }
                }

                else if (request.getParameterValue("action").toString().equals("deleteSpan")) {
                    String offsets = request.getParameterValue("offsets").toString();
                    String type = request.getParameterValue("type").toString();
                    String id = request.getParameterValue("id").toString();

                    try {
                        OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(
                                offsets, OffsetsList.class);
                        int start = offsetLists.get(0).getBegin();
                        int end = offsetLists.get(0).getEnd();
                        JCas jCas = getCas(project, user, document);
                        int annotationOffsetStart = BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                                sentenceAddress) + start;
                        int annotationOffsetEnd = BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                                sentenceAddress) + end;

                        result = controller.deleteSpan(windowSize, project, document, user,
                                sentenceAddress, lastSentenceAddress, annotationOffsetStart,
                                annotationOffsetEnd, type, id, isDisplayLemmaSelected,
                                annotationLayers, windowSize, jCas, scrollPage);
                        if (scrollPage) {
                            setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas,
                                    sentenceAddress, annotationOffsetStart, project, document,
                                    windowSize));
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
                        error("Error while getting/setting the annotation/source document from File "
                                + ":" + ExceptionUtils.getRootCauseMessage(e));
                    }
                }

                else if (request.getParameterValue("action").toString().equals("deleteArc")) {

                    String origin = request.getParameterValue("origin").toString();
                    String target = request.getParameterValue("target").toString();
                    String type = request.getParameterValue("type").toString();

                    try {
                        JCas jCas = getCas(project, user, document);
                        int annotationOffsetStart = BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                                Integer.parseInt(origin));

                        result = controller.deleteArc(windowSize, project, document, user,
                                sentenceAddress, lastSentenceAddress, annotationOffsetStart,
                                origin, target, type, isDisplayLemmaSelected, annotationLayers,
                                windowSize, jCas, scrollPage);
                        if (scrollPage) {
                            setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas,
                                    sentenceAddress, annotationOffsetStart, project, document,
                                    windowSize));
                        }

                    }
                    catch (UIMAException e) {
                        error("Error while Processing the CAS object " + ":"
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (IOException e) {
                        error("Error while getting/setting the annotation/source document from File "
                                + ":" + ExceptionUtils.getRootCauseMessage(e));
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
                + "var logger = new AnnotationLog(dispatcher);" + " dispatcher.post('init');" };

        // This doesn't work with head.js because the onLoad event is fired before all the
        // JavaScript references are loaded.
        aResponse.renderOnLoadJavaScript("\n" + StringUtils.join(script, "\n"));
    }

    /**
     * Set different attributes for
     * {@link BratAjaxCasController#getDocument(int, Project, SourceDocument, User, int, int, boolean, ArrayList)}
     */
    @SuppressWarnings("unchecked")
    public void setAttributesForGetDocument(String aProjectName, String aDocumentName)
    {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = repository.getUser(username);

        setProject(repository.getProjects(aProjectName.replace("/", "")).get(0));
        setDocument(repository.getSourceDocument(aDocumentName, project));

        if (sentenceAddress == -1 || document.getId() != currentDocumentId
                || project.getId() != currentprojectId) {
            try {
                JCas jCas = getCas(project, user, document);
                setSentenceAddress(BratAjaxCasUtil.getFirstSenetnceAddress(jCas));
                setLastSentenceAddress(lastSentenceAddress = BratAjaxCasUtil
                        .getLastSenetnceAddress(jCas));
                setFirstSentenceAddress(getSentenceAddress());
                setAnnotationLayers((ArrayList<TagSet>) annotationService.listTagSets(project));
            }
            catch (DataRetrievalFailureException ex) {
                throw ex;
            }
        }

        currentprojectId = project.getId();
        currentDocumentId = document.getId();

    }

    /**
     * Set different attributes for
     * {@link BratAjaxCasController#getCollectionInformation(String, ArrayList) }
     */
    @SuppressWarnings("unchecked")
    public void setAttributesForGetCollection(String aProjectName)
    {
        if (!aProjectName.equals("/")) {
            setProject(repository.getProjects(aProjectName.replace("/", "")).get(0));

            if (project.getId() != currentprojectId) {
                setAnnotationLayers((ArrayList<TagSet>) annotationService.listTagSets(project));
            }
            currentprojectId = project.getId();
        }
    }

    private JCas getCas(Project aProject, User user, SourceDocument aDocument)
    {
        JCas jCas = null;
        try {
            BratAjaxCasController controller = new BratAjaxCasController(jsonConverter, repository,
                    annotationService);
            jCas = controller.getJCas(aDocument, aProject, user);
        }
        catch (UIMAException e) {
            error("CAS object not found :" + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (IOException e) {
            error("Unable to read CAS object: " + ExceptionUtils.getRootCauseMessage(e));
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

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public void setDocument(SourceDocument aDocument)
    {
        document = aDocument;
    }

    public int getWindowSize()
    {
        return windowSize;
    }

    public void setWindowSize(int aWindowSize)
    {
        windowSize = aWindowSize;
    }

    public int getSentenceAddress()
    {
        return sentenceAddress;
    }

    public void setSentenceAddress(int aSentenceAddress)
    {
        sentenceAddress = aSentenceAddress;
    }

    public int getLastSentenceAddress()
    {
        return lastSentenceAddress;
    }

    public void setLastSentenceAddress(int aLastSentenceAddress)
    {
        lastSentenceAddress = aLastSentenceAddress;
    }

    public int getFirstSentenceAddress()
    {
        return firstSentenceAddress;
    }

    public void setFirstSentenceAddress(int aFirstSentenceAddress)
    {
        firstSentenceAddress = aFirstSentenceAddress;
    }

    public ArrayList<TagSet> getAnnotationLayers()
    {
        return annotationLayers;
    }

    public void setAnnotationLayers(ArrayList<TagSet> aAnnotationLayers)
    {
        annotationLayers = aAnnotationLayers;
    }

    public boolean isDisplayLemmaSelected()
    {
        return isDisplayLemmaSelected;
    }

    public void setDisplayLemmaSelected(boolean aIsDisplayLemmaSelected)
    {
        isDisplayLemmaSelected = aIsDisplayLemmaSelected;
    }

    public boolean isScrollPage()
    {
        return scrollPage;
    }

    public void setScrollPage(boolean aMovePage)
    {
        scrollPage = aMovePage;
    }

}
