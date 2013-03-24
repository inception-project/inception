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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import javax.persistence.NoResultException;

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
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
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
    private Project project;
    private SourceDocument document;
    private User user;
    private int sentenceAddress = -1;
    private int lastSentenceAddress;
    private int firstSentenceAddress;

    // Annotation preferences
    private HashSet<TagSet> annotationLayers = new HashSet<TagSet>();
    private int windowSize;
    private boolean isDisplayLemmaSelected;
    private boolean scrollPage;
    /**
     * store document and project names to compare with the current and previous document/project
     */
    private long currentDocumentId;
    private long currentprojectId;

    private transient JCas jCas;
    private int annotationOffsetStart;
    private int annotationOffsetEnd;
    private String type;
    private String origin;
    private String target;

    // If Brat action is getdocument, no aut-scroll at all
    private boolean isGetDocument;

    public BratAnnotator(String id, IModel<?> aModel)
    {
        super(id, aModel);

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);

        final FeedbackPanel feedbackPanel = new FeedbackPanel("bratAnnotatorfeedBackPanel");
        add(feedbackPanel);
        feedbackPanel.setOutputMarkupId(true);

        add(numberOfPages = (Label) new Label("numberOfPages", new LoadableDetachableModel()
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected String load()
            {
                if (document != null) {
                    try {
                        totalPageNumber = BratAjaxCasUtil.getNumberOfPages(jCas, windowSize);

                        // If only one page, start displaying from sentence 1
                        if (totalPageNumber == 1) {
                            sentenceAddress = firstSentenceAddress;
                        }
                        pageNumber = BratAjaxCasUtil.getPageNumber(jCas, windowSize,
                                sentenceAddress);
                        return pageNumber + " of " + totalPageNumber + " pages";
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
                setUser(repository.getUser(username));

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

                    boolean firstTimeDocumentOpened = isDocumentOpenedFirstTime(collection,
                            documentName);

                    result = getDocument(request, user);

                    if (firstTimeDocumentOpened) {
                        info("Document is opened for the first time. "
                                + "Initial conversion from <" + document.getFormat()
                                + "> has been performed.");
                    }
                }
                else if (request.getParameterValue("action").toString().equals("createSpan")) {
                    result = createSpan(request, user);
                    info("Annotation [" + request.getParameterValue("type").toString()
                            + "]has been created");
                }

                else if (request.getParameterValue("action").toString().equals("createArc")) {
                    result = createArc(request, user);
                    info("Annotation [" + request.getParameterValue("type").toString()
                            + "]has been created");
                }

                else if (request.getParameterValue("action").toString().equals("reverseArc")) {
                    result = reverseArc(request, user);
                    info("Annotation [" + request.getParameterValue("type").toString()
                            + "]has been reversed");
                }
                else if (request.getParameterValue("action").toString().equals("deleteSpan")) {
                    result = deleteSpan(request, user);
                    info("Annotation [" + request.getParameterValue("type").toString()
                            + "]has been deleted");
                }

                else if (request.getParameterValue("action").toString().equals("deleteArc")) {
                    result = deleteArc(request, user);
                    info("Annotation [" + request.getParameterValue("type").toString()
                            + "]has been deleted");
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
                + "var logger = new AnnotationLog(dispatcher);" + " dispatcher.post('init');" };

        // This doesn't work with head.js because the onLoad event is fired before all the
        // JavaScript references are loaded.
        aResponse.renderOnLoadJavaScript("\n" + StringUtils.join(script, "\n"));
    }

    private Object getDocument(IRequestParameters aRequest, User aUser)
    {
        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(jsonConverter, repository,
                annotationService);
        String collection = aRequest.getParameterValue("collection").toString();
        String documentName = aRequest.getParameterValue("document").toString();

        try {
            {
                setAttributesForGetDocument(collection, documentName);
            }
            setGetDocument(true);
            result = controller.getDocument(this);
            setGetDocument(false);
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

    private Object createSpan(IRequestParameters aRequest, User aUser)
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(jsonConverter, repository,
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
            setAnnotationOffsetStart(BratAjaxCasUtil
                    .getAnnotationBeginOffset(jCas, sentenceAddress) + start);
            setAnnotationOffsetEnd(BratAjaxCasUtil.getAnnotationBeginOffset(jCas, sentenceAddress)
                    + end);
            setType(aRequest.getParameterValue("type").toString());

            result = controller.createSpan(this);
            if (scrollPage) {
                setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas, sentenceAddress,
                        annotationOffsetStart, project, document, windowSize));
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

    private Object createArc(IRequestParameters aRequest, User aUser)
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(jsonConverter, repository,
                annotationService);
        try {
            setOrigin(aRequest.getParameterValue("origin").toString());
            setTarget(aRequest.getParameterValue("target").toString());
            setType(aRequest.getParameterValue("type").toString());
            setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                    Integer.parseInt(origin)));
            result = controller.createArc(this);
            if (scrollPage) {
                setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas, sentenceAddress,
                        annotationOffsetStart, project, document, windowSize));
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

    private Object reverseArc(IRequestParameters aRequest, User aUser)
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(jsonConverter, repository,
                annotationService);

        try {
            setOrigin(aRequest.getParameterValue("origin").toString());
            setTarget(aRequest.getParameterValue("target").toString());
            setType(aRequest.getParameterValue("type").toString());
            setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                    Integer.parseInt(origin)));

            String annotationType = type.substring(0, type.indexOf(AnnotationType.PREFIX) + 1);
            if (annotationType.equals(AnnotationType.POS_PREFIX)) {
                result = controller.reverseArc(this);
                if (scrollPage) {
                    setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas,
                            sentenceAddress, annotationOffsetStart, project, document, windowSize));
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

    private Object deleteSpan(IRequestParameters aRequest, User aUser)
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(jsonConverter, repository,
                annotationService);

        try {
            String offsets = aRequest.getParameterValue("offsets").toString();
            String id = aRequest.getParameterValue("id").toString();
            OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(offsets,
                    OffsetsList.class);
            int start = offsetLists.get(0).getBegin();
            int end = offsetLists.get(0).getEnd();
            setAnnotationOffsetStart(BratAjaxCasUtil
                    .getAnnotationBeginOffset(jCas, sentenceAddress) + start);
            setAnnotationOffsetEnd(BratAjaxCasUtil.getAnnotationBeginOffset(jCas, sentenceAddress)
                    + end);
            setType(aRequest.getParameterValue("type").toString());
            result = controller.deleteSpan(this, id);
            if (scrollPage) {
                setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas, sentenceAddress,
                        annotationOffsetStart, project, document, windowSize));
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

    private Object deleteArc(IRequestParameters aRequest, User aUser)
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(jsonConverter, repository,
                annotationService);

        try {

            setOrigin(aRequest.getParameterValue("origin").toString());
            setTarget(aRequest.getParameterValue("target").toString());
            setType(aRequest.getParameterValue("type").toString());
            setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                    Integer.parseInt(origin)));

            result = controller.deleteArc(this);
            if (scrollPage) {
                setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas, sentenceAddress,
                        annotationOffsetStart, project, document, windowSize));
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
     */
    @SuppressWarnings("unchecked")
    public void setAttributesForGetDocument(String aProjectName, String aDocumentName)
        throws UIMAException
    {

        setjCas(getCas(project, user, document));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (sentenceAddress == -1 || document.getId() != currentDocumentId
                || project.getId() != currentprojectId) {

            try {
                setSentenceAddress(BratAjaxCasUtil.getFirstSenetnceAddress(jCas));
                setLastSentenceAddress(lastSentenceAddress = BratAjaxCasUtil
                        .getLastSenetnceAddress(jCas));
                setFirstSentenceAddress(getSentenceAddress());
                // Get preferences first from the properties file
                try {
                    AnnotationPreference preference = new AnnotationPreference();
                    setAnnotationPreference(preference, username);
                    setWindowSize(preference.getWindowSize());
                    setScrollPage(preference.isScrollPage());
                    setDisplayLemmaSelected(preference.isDisplayLemmaSelected());
                    // Get tagset using the id, from the properties file
                    for (Long id : preference.getAnnotationLayers()) {
                        getAnnotationLayers().add(annotationService.getTagSet(id));
                    }
                }
                // No setting saved yet, set default
                catch (Exception e) {
                    setWindowSize(10);
                    setDisplayLemmaSelected(true);
                    setAnnotationLayers(new HashSet<TagSet>(annotationService.listTagSets(project)));

                }
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
            setProject(repository.getProject(aProjectName.replace("/", "")));

            if (project.getId() != currentprojectId) {
                annotationLayers.clear();
                setAnnotationLayers(new HashSet<TagSet>(annotationService.listTagSets(project)));
            }
            currentprojectId = project.getId();
        }
    }

    boolean isDocumentOpenedFirstTime(String aCollection, String adocumentName)
    {
        setProject(repository.getProject(aCollection.replace("/", "")));
        setDocument(repository.getSourceDocument(adocumentName, project));

        try {
            repository.getAnnotationDocument(document, user);
            return false;
        }
        catch (NoResultException e) {
            return true;
        }
    }

    private JCas getCas(Project aProject, User user, SourceDocument aDocument)
        throws UIMAException
    {
        JCas jCas = null;
        try {
            BratAjaxCasController controller = new BratAjaxCasController(jsonConverter, repository,
                    annotationService);
            jCas = controller.getJCas(aDocument, aProject, user);
        }
        catch (UIMAException e) {
            error("CAS object not found :" + ExceptionUtils.getRootCauseMessage(e));
            throw e;
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

    private void setAnnotationPreference(AnnotationPreference aPreference, String aUsername)
        throws BeansException, FileNotFoundException, IOException
    {
        BeanWrapper wrapper = new BeanWrapperImpl(aPreference);
        for (Entry<Object, Object> entry : repository.loadUserSettings(aUsername, project,
                "annotation").entrySet()) {
            String propertyName = entry.getKey().toString();
            int index = propertyName.lastIndexOf(".");
            propertyName = propertyName.substring(index + 1);
            if (wrapper.isWritableProperty(propertyName)) {
                List<String> value = Arrays.asList(entry.getValue().toString().replace("[", "")
                        .replace("]", "").split(","));
                if (value.size() > 1) {
                    wrapper.setPropertyValue(propertyName, value);
                }
                else {
                    wrapper.setPropertyValue(propertyName, entry.getValue());
                }
            }
        }
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

    public User getUser()
    {
        return user;
    }

    public void setUser(User aUser)
    {
        user = aUser;
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

    public HashSet<TagSet> getAnnotationLayers()
    {
        return annotationLayers;
    }

    public void setAnnotationLayers(HashSet<TagSet> aAnnotationLayers)
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

    public JCas getjCas()
    {
        return jCas;
    }

    public void setjCas(JCas aJCas)
    {
        jCas = aJCas;
    }

    public int getAnnotationOffsetStart()
    {
        return annotationOffsetStart;
    }

    public void setAnnotationOffsetStart(int aAnnotationOffsetStart)
    {
        annotationOffsetStart = aAnnotationOffsetStart;
    }

    public int getAnnotationOffsetEnd()
    {
        return annotationOffsetEnd;
    }

    public void setAnnotationOffsetEnd(int aAnnotationOffsetEnd)
    {
        annotationOffsetEnd = aAnnotationOffsetEnd;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public String getOrigin()
    {
        return origin;
    }

    public void setOrigin(String aOrigin)
    {
        origin = aOrigin;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget(String aTarget)
    {
        target = aTarget;
    }

    public boolean isGetDocument()
    {
        return isGetDocument;
    }

    public void setGetDocument(boolean aIsGetDocument)
    {
        isGetDocument = aIsGetDocument;
    }

}
