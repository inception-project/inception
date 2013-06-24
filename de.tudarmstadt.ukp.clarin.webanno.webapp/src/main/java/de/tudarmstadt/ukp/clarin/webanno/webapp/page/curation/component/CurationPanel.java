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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.beans.BeansException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorUIData;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationOption;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.BratCurationDocumentEditor;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.BratCurationVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationBuilder;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationSegmentForSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationUserSegmentForAnnotationDocument;

/**
 * Main Panel for the curation page. It displays a box with the complete text on the left side and a
 * box for a selected sentence on the right side.
 *
 * @author Andreas Straninger
 */
public class CurationPanel
    extends Panel
{
    private static final long serialVersionUID = -5128648754044819314L;

    private final static Log LOG = LogFactory.getLog(CurationPanel.class);

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    public final static String CURATION_USER = "CURATION_USER";

    /**
     * Map for tracking curated spans. Key contains the address of the span, the value contains the
     * username from which the span has been selected
     */
    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress = new HashMap<String, Map<Integer, AnnotationSelection>>();

    private ListView<CurationUserSegmentForAnnotationDocument> sentenceListView;

    private CurationSegmentForSourceDocument curationSegment;

    ListView<CurationSegmentForSourceDocument> textListView;

    /**
     * Class for combining an on click ajax call and a label
     */
    class AjaxLabel
        extends Label
    {

        private static final long serialVersionUID = -4528869530409522295L;
        private AbstractAjaxBehavior click;

        public AjaxLabel(String id, String label, AbstractAjaxBehavior click)
        {
            super(id, label);
            this.click = click;
        }

        @Override
        public void onComponentTag(ComponentTag tag)
        {
            // add onclick handler to the browser
            // if clicked in the browser, the function
            // click.response(AjaxRequestTarget target) is called on the server side
            tag.put("ondblclick", "wicketAjaxGet('" + click.getCallbackUrl() + "')");
            tag.put("onclick", "wicketAjaxGet('" + click.getCallbackUrl() + "')");
        }

    }

    public CurationPanel(String id, final CurationContainer curationContainer)
    {
        super(id);

        // add container for updating ajax
        final WebMarkupContainer textOuterView = new WebMarkupContainer("textOuterView");
        textOuterView.setOutputMarkupId(true);
        add(textOuterView);

        final WebMarkupContainer sentenceOuterView = new WebMarkupContainer("sentenceOuterView");
        sentenceOuterView.setOutputMarkupId(true);
        add(sentenceOuterView);

        final BratAnnotatorModel bratAnnotatorModel = curationContainer.getBratAnnotatorModel();

        final BratCurationDocumentEditor mergeVisualizer = new BratCurationDocumentEditor(
                "mergeView", new Model<BratAnnotatorModel>(bratAnnotatorModel))
        {

            private static final long serialVersionUID = 7279648231521710155L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                updateRightSide(aTarget, sentenceOuterView, curationContainer, this);
            }
        };
        // reset sentenceAddress and lastSentenceAddress to the orginal once

        mergeVisualizer.setOutputMarkupId(true);
        add(mergeVisualizer);

        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();
        // update list of brat embeddings
        sentenceListView = new ListView<CurationUserSegmentForAnnotationDocument>(
                "sentenceListView", sentences)
        {
            private static final long serialVersionUID = -5389636445364196097L;

            @Override
            protected void populateItem(ListItem<CurationUserSegmentForAnnotationDocument> item2)
            {
                final CurationUserSegmentForAnnotationDocument curationUserSegment = item2
                        .getModelObject();
                BratCurationVisualizer curationVisualizer = new BratCurationVisualizer("sentence",
                        new Model<CurationUserSegmentForAnnotationDocument>(curationUserSegment))
                {
                    private static final long serialVersionUID = -1205541428144070566L;

                    /**
                     * Method is called, if user has clicked on a span or an arc in the sentence
                     * panel. The span or arc respectively is identified and copied to the merge
                     * cas.
                     */
                    @Override
                    protected void onSelectAnnotationForMerge(AjaxRequestTarget aTarget)
                    {
                        final IRequestParameters request = getRequest().getPostParameters();

                        SourceDocument sourceDocument = bratAnnotatorModel.getDocument();
                        Project project = bratAnnotatorModel.getProject();
                        JCas mergeJCas = null;
                        try {
                            mergeJCas = repository.getCurationDocumentContent(sourceDocument);
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
                        StringValue action = request.getParameterValue("action");
                        AnnotationSelection annotationSelection = null;
                        Integer address = null;
                        String username = curationUserSegment.getUsername();
                        // check if clicked on a span
                        if (!action.isEmpty() && action.toString().equals("selectSpanForMerge")) {
                            // add span for merge
                            // get information of the span clicked
                            address = request.getParameterValue("id").toInteger();
                            annotationSelection = annotationSelectionByUsernameAndAddress.get(
                                    username).get(address);
                            if (annotationSelection != null) {
                                AnnotationDocument clickedAnnotationDocument = null;
                                List<AnnotationDocument> annotationDocuments = repository
                                        .listAnnotationDocument(project, sourceDocument);
                                for (AnnotationDocument annotationDocument : annotationDocuments) {
                                    if (annotationDocument.getUser().equals(username)) {
                                        clickedAnnotationDocument = annotationDocument;
                                        break;
                                    }
                                }
                                try {
                                    createSpan(request, bratAnnotatorModel, mergeJCas,
                                            clickedAnnotationDocument, address);
                                }
                                catch (UIMAException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                catch (ClassNotFoundException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }

                        }
                        // check if clicked on an arc
                        else if (!action.isEmpty() && action.toString().equals("selectArcForMerge")) {
                            // add span for merge
                            // get information of the span clicked
                            Integer addressOriginClicked = request
                                    .getParameterValue("originSpanId").toInteger();
                            Integer addressTargetClicked = request
                                    .getParameterValue("targetSpanId").toInteger();
                            String arcType = request.getParameterValue("type").toString();
                            AnnotationSelection annotationSelectionOrigin = annotationSelectionByUsernameAndAddress
                                    .get(username).get(addressOriginClicked);
                            AnnotationSelection annotationSelectionTarget = annotationSelectionByUsernameAndAddress
                                    .get(username).get(addressTargetClicked);
                            Integer addressOrigin = annotationSelectionOrigin
                                    .getAddressByUsername().get(CURATION_USER);
                            Integer addressTarget = annotationSelectionTarget
                                    .getAddressByUsername().get(CURATION_USER);

                            if (annotationSelectionOrigin != null
                                    && annotationSelectionTarget != null) {
                                BratAnnotatorUIData uIData = new BratAnnotatorUIData();
                                uIData.setjCas(mergeJCas);
                                uIData.setGetDocument(false);
                                // TODO no coloring is done at all for arc annotation.
                                // Do the same for arc colors (AGREE, USE,...
                                AnnotationDocument clickedAnnotationDocument = null;
                                List<AnnotationDocument> annotationDocuments = repository
                                        .listAnnotationDocument(project, sourceDocument);
                                for (AnnotationDocument annotationDocument : annotationDocuments) {
                                    if (annotationDocument.getUser().equals(username)) {
                                        clickedAnnotationDocument = annotationDocument;
                                        break;
                                    }
                                }
                                JCas clickedJCas = null;
                                try {
                                    clickedJCas = repository.getAnnotationDocumentContent(clickedAnnotationDocument);
                                }
                                catch (UIMAException e1) {
                                    // TODO Auto-generated catch block
                                    e1.printStackTrace();
                                }
                                catch (ClassNotFoundException e1) {
                                    // TODO Auto-generated catch block
                                    e1.printStackTrace();
                                }
                                catch (IOException e1) {
                                    // TODO Auto-generated catch block
                                    e1.printStackTrace();
                                }
                                AnnotationFS fsClicked = (AnnotationFS) clickedJCas.getLowLevelCas().ll_getFSForRef(
                                        addressOriginClicked);
                                arcType =  BratAjaxCasUtil.getAnnotationType(fsClicked.getType())+arcType;
                                uIData.setType(arcType);
                                uIData.setOrigin(addressOrigin);
                                uIData.setTarget(addressTarget);
                                BratAjaxCasController controller = new BratAjaxCasController(
                                        repository, annotationService);
                                try {
                                    controller.addArcToCas(bratAnnotatorModel, uIData);
                                    controller.createAnnotationDocumentContent(
                                            bratAnnotatorModel.getMode(),
                                            bratAnnotatorModel.getDocument(),
                                            bratAnnotatorModel.getUser(), mergeJCas);
                                }
                                catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }

                        updateRightSide(aTarget, sentenceOuterView, curationContainer,
                                mergeVisualizer);
                        // aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                };
                curationVisualizer.setOutputMarkupId(true);
                item2.add(curationVisualizer);
            }
        };
        sentenceListView.setOutputMarkupId(true);

        sentenceOuterView.add(sentenceListView);

        // List view for the complete text on the left side. Each item is a sentence of the text
        textListView = new ListView<CurationSegmentForSourceDocument>("textListView",
                curationContainer.getCurationSegments())
        {
            @Override
            protected void populateItem(ListItem<CurationSegmentForSourceDocument> item)
            {
                final CurationSegmentForSourceDocument curationSegmentItem = item.getModelObject();

                // ajax call when clicking on a sentence on the left side
                final AbstractDefaultAjaxBehavior click = new AbstractDefaultAjaxBehavior()
                {

                    @Override
                    protected void respond(AjaxRequestTarget target)
                    {
                        curationSegment = curationSegmentItem;
                        updateRightSide(target, sentenceOuterView, curationContainer,
                                mergeVisualizer);
                        List<CurationSegmentForSourceDocument> segments = curationContainer
                                .getCurationSegments();
                        for (CurationSegmentForSourceDocument segment : segments) {
                            segment.setCurrentSentence(curationSegmentItem.getSentenceNumber()
                                    .equals(segment.getSentenceNumber()));
                        }
                        textListView.setModelObject(segments);
                        textOuterView.addOrReplace(textListView);
                        target.add(textOuterView);
                        // target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }

                };

                // add subcomponents to the component
                item.add(click);
                String colorCode = curationSegmentItem.getSentenceState().getColorCode();
/*                if (curationSegmentItem.isCurrentSentence()) {
                    item.add(AttributeModifier.append("style", "border: 4px solid black;"));
                }*/
                if (colorCode != null) {
                    item.add(AttributeModifier.append("style", "background-color: "+colorCode+";"));
                }

                Label currentSentence = new AjaxLabel("sentence", curationSegmentItem.getText(),
                        click);
                item.add(currentSentence);

                Label sentenceNumber = new AjaxLabel("sentenceNumber", curationSegmentItem
                        .getSentenceNumber().toString(), click);
                item.add(sentenceNumber);
            }

        };
        // add subcomponents to the component
        textListView.setOutputMarkupId(true);
        textOuterView.add(textListView);

    }

    private void createSpan(IRequestParameters aRequest, BratAnnotatorModel aBratAnnotatorModel,
            JCas aMergeJCas, AnnotationDocument aAnnotationDocument, int aAddress)
        throws IOException, UIMAException, ClassNotFoundException
    {

        String spanType = aRequest.getParameterValue("type").toString()
                .replace("_(" + AnnotationState.AGREE.name() + ")", "")
                .replace("_(" + AnnotationState.USE.name() + ")", "")
                .replace("_(" + AnnotationState.DISAGREE.name() + ")", "")
                .replace("_(" + AnnotationState.DO_NOT_USE.name() + ")", "")
                .replace("_(" + AnnotationState.NOT_SUPPORTED.name() + ")", "");

        JCas clickedJCas = repository.getAnnotationDocumentContent(aAnnotationDocument);
        AnnotationFS fsClicked = (AnnotationFS) clickedJCas.getLowLevelCas().ll_getFSForRef(
                aAddress);
        // TODO temporarily solution to remove the the prefix from curation sentence annotation views
        spanType =  BratAjaxCasUtil.getAnnotationType(fsClicked.getType())+spanType;

        BratAnnotatorUIData uIData = new BratAnnotatorUIData();
        uIData.setjCas(aMergeJCas);
        uIData.setGetDocument(false);
        uIData.setType(spanType);
        uIData.setAnnotationOffsetStart(fsClicked.getBegin());
        uIData.setAnnotationOffsetEnd(fsClicked.getEnd());
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);

        controller.addSpanToCas(uIData);
        controller.createAnnotationDocumentContent(aBratAnnotatorModel.getMode(),
                aBratAnnotatorModel.getDocument(), aBratAnnotatorModel.getUser(), aMergeJCas);
    }

    protected void updateRightSide(AjaxRequestTarget target, MarkupContainer parent,
            CurationContainer curationContainer, BratCurationDocumentEditor mergeVisualizer)
    {
        SourceDocument sourceDocument = curationContainer.getBratAnnotatorModel().getDocument();
        Project project = curationContainer.getBratAnnotatorModel().getProject();
        List<AnnotationDocument> annotationDocuments = repository.listAnnotationDocument(project,
                sourceDocument);
        Map<String, JCas> jCases = new HashMap<String, JCas>();
        JCas mergeJCas = null;
        try {
            mergeJCas = repository.getCurationDocumentContent(sourceDocument);
        }
        catch (UIMAException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (ClassNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        User userLoggedIn = repository.getUser(SecurityContextHolder.getContext()
                .getAuthentication().getName());

        // get cases from repository
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            String username = annotationDocument.getUser();
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)
                    || username.equals(CURATION_USER)) {
                try {
                    JCas jCas = repository.getAnnotationDocumentContent(annotationDocument);
                    jCases.put(username, jCas);

                    // cleanup annotationSelections
                    annotationSelectionByUsernameAndAddress.put(username,
                            new HashMap<Integer, AnnotationSelection>());
                }
                catch (Exception e) {
                    LOG.error("Unable to load document [" + annotationDocument + "]", e);
                    error("Unable to load document [" + annotationDocument + "]: "
                            + ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }
        // add mergeJCas separately
        jCases.put(CURATION_USER, mergeJCas);

        // create cas for merge panel
        int numUsers = jCases.size();

        // get differing feature structures
        List<Type> entryTypes = CurationBuilder.getEntryTypes(mergeJCas,
                curationContainer.getBratAnnotatorModel());
        List<AnnotationOption> annotationOptions = null;
        try {
            annotationOptions = CasDiff.doDiff(entryTypes, jCases, curationSegment.getBegin(),
                    curationSegment.getEnd());
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // fill lookup variable for annotation selections
        for (AnnotationOption annotationOption : annotationOptions) {
            for (AnnotationSelection annotationSelection : annotationOption
                    .getAnnotationSelections()) {
                for (String username : annotationSelection.getAddressByUsername().keySet()) {
                    if (!username.equals(CURATION_USER)) {
                        Integer address = annotationSelection.getAddressByUsername().get(username);
                        annotationSelectionByUsernameAndAddress.get(username).put(address,
                                annotationSelection);
                    }
                }
            }
        }

        List<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();

        BratAnnotatorModel bratAnnotatorModel = new BratAnnotatorModel();// .getModelObject();
        bratAnnotatorModel.setDocument(sourceDocument);
        bratAnnotatorModel.setProject(sourceDocument.getProject());
        bratAnnotatorModel.setUser(userLoggedIn);
        bratAnnotatorModel.setFirstSentenceAddress(curationSegment.getSentenceAddress().get(
                CURATION_USER));
        bratAnnotatorModel.setLastSentenceAddress(curationSegment.getSentenceAddress().get(
                CURATION_USER));
        bratAnnotatorModel.setSentenceAddress(curationSegment.getSentenceAddress().get(
                CURATION_USER));
        AnnotationPreference preference = new AnnotationPreference();
        try {
            ApplicationUtils.setAnnotationPreference(preference, userLoggedIn.getUsername(), repository,
                    annotationService, bratAnnotatorModel, Mode.CURATION);
        }
        catch (BeansException e) {
           error(ExceptionUtils.getRootCause(e));
        }
        catch (FileNotFoundException e) {
            error(ExceptionUtils.getRootCause(e));
        }
        catch (IOException e) {
            error(ExceptionUtils.getRootCause(e));
        }
        bratAnnotatorModel.setMode(Mode.CURATION);

        boolean hasDiff = false;
        List<String> usernamesSorted = new LinkedList<String>(jCases.keySet());
        Collections.sort(usernamesSorted);
        for (String username : usernamesSorted) {
            if (!username.equals(CURATION_USER)) {
                Map<Integer, AnnotationSelection> annotationSelectionByAddress = new HashMap<Integer, AnnotationSelection>();
                for (AnnotationOption annotationOption : annotationOptions) {
                    for (AnnotationSelection annotationSelection : annotationOption
                            .getAnnotationSelections()) {
                        if (annotationSelection.getAddressByUsername().containsKey(username)) {
                            Integer address = annotationSelection.getAddressByUsername().get(
                                    username);
                            annotationSelectionByAddress.put(address, annotationSelection);
                        }
                    }
                }
                JCas jCas = jCases.get(username);

                GetDocumentResponse response = new GetDocumentResponse();
                BratAnnotatorUIData uIData = new BratAnnotatorUIData();
                uIData.setjCas(jCas);
                uIData.setGetDocument(true);

                BratAjaxCasController.addBratResponses(response, bratAnnotatorModel, uIData);

                CurationUserSegmentForAnnotationDocument curationUserSegment2 = new CurationUserSegmentForAnnotationDocument();
                curationUserSegment2.setCollectionData(getStringCollectionData(response, jCas,
                        annotationSelectionByAddress, username, numUsers));
                curationUserSegment2.setDocumentResponse(getStringDocumentResponse(response));
                curationUserSegment2.setUsername(username);

                sentences.add(curationUserSegment2);
            }
        }

        // update sentence list on the right side
        sentenceListView.setModelObject(sentences);

        /*
         * CurationUserSegment2 mergeUserSegment = new CurationUserSegment2(); GetDocumentResponse
         * response = getDocumentResponse(mergeJCas, CURATION_USER, bratAnnotatorModel);
         * //mergeUserSegment.setCollectionData(getStringCollectionData(response, mergeJCas,
         * addresses, username)); mergeUserSegment.setCollectionData("{}");
         * mergeUserSegment.setDocumentResponse(getStringDocumentResponse(response));
         */
        bratAnnotatorModel.setMode(Mode.CURATIONANNOTATION);
        mergeVisualizer.setModelObject(bratAnnotatorModel);
        mergeVisualizer.reloadContent(target);

        // send response to the client
        parent.addOrReplace(sentenceListView);
        target.add(parent);

    }

    private String getStringDocumentResponse(GetDocumentResponse aResponse)
    {
        String docData = "{}";
        // Serialize BRAT object model to JSON
        try {
            StringWriter out = new StringWriter();
            JsonGenerator jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
                    .createJsonGenerator(out);
            jsonGenerator.writeObject(aResponse);
            docData = out.toString();
        }
        catch (IOException e) {
            error(ExceptionUtils.getRootCauseMessage(e));
        }

        return docData;
    }

    private String getStringCollectionData(GetDocumentResponse response, JCas jCas,
            Map<Integer, AnnotationSelection> annotationSelectionByAddress, String username,
            int numUsers)
    {
        Map<String, Map<String, Object>> entityTypes = new HashMap<String, Map<String, Object>>();

        for (Entity entity : response.getEntities()) {
            // check if either address of entity has no changes ...
            // ... or if entity has already been clicked on
            int address = entity.getId();
            AnnotationSelection annotationSelection = annotationSelectionByAddress.get(address);
            AnnotationState newState = null;
            if (annotationSelection == null) {
                newState = AnnotationState.NOT_SUPPORTED;

            }
            else if (annotationSelection.getAddressByUsername().size() == numUsers) {
                newState = AnnotationState.AGREE;

            }
            else if (annotationSelection.getAddressByUsername().containsKey(CURATION_USER)) {
                newState = AnnotationState.USE;

            }
            else {
                boolean doNotUse = false;
                for (AnnotationSelection otherAnnotationSelection : annotationSelection
                        .getAnnotationOption().getAnnotationSelections()) {
                    if (otherAnnotationSelection.getAddressByUsername().containsKey(CURATION_USER)) {
                        doNotUse = true;
                        break;
                    }
                }
                if (doNotUse) {
                    newState = AnnotationState.DO_NOT_USE;
                }
                else {
                    newState = AnnotationState.DISAGREE;

                }
            }
            if (newState != null) {
                String type = entity.getType() + "_(" + newState.name() + ")";
                entity.setType(type);
                entityTypes.put(type, getEntity(type, newState));
            }

        }

        Map<Object, Object> collection = new HashMap<Object, Object>();
        collection.put("entity_types", entityTypes.values());

        String collData = "{}";
        try {
            StringWriter out = new StringWriter();
            JsonGenerator jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
                    .createJsonGenerator(out);
            jsonGenerator.writeObject(collection);
            collData = out.toString();
        }
        catch (IOException e) {
            error(ExceptionUtils.getRootCauseMessage(e));
        }
        return collData;
    }

    private Map<String, Object> getEntity(String type, AnnotationState annotationState)
    {
        Map<String, Object> entityType = new HashMap<String, Object>();
        entityType.put("type", type);
        entityType.put("labels", new String[] { type });
        String color = annotationState.getColorCode();
        entityType.put("bgColor", color);
        entityType.put("borderColor", "darken");
        return entityType;
    }

}
