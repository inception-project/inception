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
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
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
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.beans.BeansException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
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
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationBuilder;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationSegmentForSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.SentenceContainer;

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

    private SentenceContainer sentenceOuterView;
    private BratAnnotator mergeVisualizer;

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

        /*
         * final WebMarkupContainer sentenceOuterView = new WebMarkupContainer("sentenceOuterView");
         * sentenceOuterView.setOutputMarkupId(true); add(sentenceOuterView);
         */

        final BratAnnotatorModel bratAnnotatorModel = curationContainer.getBratAnnotatorModel();

        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();
        CurationUserSegmentForAnnotationDocument curationUserSegmentForAnnotationDocument = new CurationUserSegmentForAnnotationDocument();
        if (bratAnnotatorModel != null) {
            curationUserSegmentForAnnotationDocument
                    .setAnnotationSelectionByUsernameAndAddress(annotationSelectionByUsernameAndAddress);
            curationUserSegmentForAnnotationDocument.setBratAnnotatorModel(bratAnnotatorModel);
            sentences.add(curationUserSegmentForAnnotationDocument);
        }
        sentenceOuterView = new SentenceContainer("sentenceOuterView",
                new Model<LinkedList<CurationUserSegmentForAnnotationDocument>>(sentences))
        {
            private static final long serialVersionUID = 2583509126979792202L;
            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
               // aTarget.add(mergeVisualizer);
                updateRightSide(aTarget, this, curationContainer, mergeVisualizer);
            }
        };

        sentenceOuterView.setOutputMarkupId(true);
        add(sentenceOuterView);

         mergeVisualizer = new BratAnnotator("mergeView",
                new Model<BratAnnotatorModel>(bratAnnotatorModel))
        {

            private static final long serialVersionUID = 7279648231521710155L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                aTarget.add(sentenceOuterView);
                updateRightSide(aTarget, sentenceOuterView, curationContainer, this);
            }
        };
        // reset sentenceAddress and lastSentenceAddress to the orginal once

        mergeVisualizer.setOutputMarkupId(true);
        add(mergeVisualizer);

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
                        target.add(sentenceOuterView);
                        // target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }

                };

                // add subcomponents to the component
                item.add(click);
                String colorCode = curationSegmentItem.getSentenceState().getColorCode();
                /*
                 * if (curationSegmentItem.isCurrentSentence()) {
                 * item.add(AttributeModifier.append("style", "border: 4px solid black;")); }
                 */
                if (colorCode != null) {
                    item.add(AttributeModifier.append("style", "background-color: " + colorCode
                            + ";"));
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

    protected void updateRightSide(AjaxRequestTarget target, SentenceContainer parent,
            CurationContainer curationContainer, BratAnnotator mergeVisualizer)
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

        // get cases from repository
        getCases(jCases, annotationDocuments);
        // add mergeJCas separately
        jCases.put(CURATION_USER, mergeJCas);

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
        fillLookupVariables(annotationOptions);

        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();

        BratAnnotatorModel bratAnnotatorModel = setBratAnnotatorModel(sourceDocument);

        populateCurationSentences(jCases, sentences, bratAnnotatorModel, annotationOptions);
        // update sentence list on the right side
        parent.setModelObject(sentences);

        bratAnnotatorModel.setMode(Mode.MERGE);
        mergeVisualizer.setModelObject(bratAnnotatorModel);
        mergeVisualizer.reloadContent(target);

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
                String label = entity.getType();
                entity.setType(type);
                entityTypes.put(type, getEntity(type, label, newState));
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

    private Map<String, Object> getEntity(String type, String label, AnnotationState annotationState)
    {
        Map<String, Object> entityType = new HashMap<String, Object>();
        entityType.put("type", type);
        entityType.put("labels", new String[] { label });
        String color = annotationState.getColorCode();
        entityType.put("bgColor", color);
        entityType.put("borderColor", "darken");
        return entityType;
    }

    private void getCases(Map<String, JCas> aJCases, List<AnnotationDocument> aAnnotationDocuments)
    {
        for (AnnotationDocument annotationDocument : aAnnotationDocuments) {
            String username = annotationDocument.getUser();
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)
                    || username.equals(CURATION_USER)) {
                try {
                    JCas jCas = repository.getAnnotationDocumentContent(annotationDocument);
                    aJCases.put(username, jCas);

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
    }

    private void fillLookupVariables(List<AnnotationOption> aAnnotationOptions)
    {
        // fill lookup variable for annotation selections
        for (AnnotationOption annotationOption : aAnnotationOptions) {
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
    }

    private void populateCurationSentences(Map<String, JCas> aJCases,
            List<CurationUserSegmentForAnnotationDocument> aSentences,
            BratAnnotatorModel bratAnnotatorModel, List<AnnotationOption> aAnnotationOptions)
    {
        List<String> usernamesSorted = new LinkedList<String>(aJCases.keySet());
        Collections.sort(usernamesSorted);
        int numUsers = aJCases.size();
        for (String username : usernamesSorted) {
            if (!username.equals(CURATION_USER)) {
                Map<Integer, AnnotationSelection> annotationSelectionByAddress = new HashMap<Integer, AnnotationSelection>();
                for (AnnotationOption annotationOption : aAnnotationOptions) {
                    for (AnnotationSelection annotationSelection : annotationOption
                            .getAnnotationSelections()) {
                        if (annotationSelection.getAddressByUsername().containsKey(username)) {
                            Integer address = annotationSelection.getAddressByUsername().get(
                                    username);
                            annotationSelectionByAddress.put(address, annotationSelection);
                        }
                    }
                }
                JCas jCas = aJCases.get(username);

                GetDocumentResponse response = new GetDocumentResponse();

                BratAjaxCasController.addBratResponses(response, bratAnnotatorModel, 0, jCas, true);

                CurationUserSegmentForAnnotationDocument curationUserSegment2 = new CurationUserSegmentForAnnotationDocument();
                curationUserSegment2.setCollectionData(getStringCollectionData(response, jCas,
                        annotationSelectionByAddress, username, numUsers));
                curationUserSegment2.setDocumentResponse(getStringDocumentResponse(response));
                curationUserSegment2.setUsername(username);
                curationUserSegment2.setBratAnnotatorModel(bratAnnotatorModel);
                curationUserSegment2
                        .setAnnotationSelectionByUsernameAndAddress(annotationSelectionByUsernameAndAddress);

                aSentences.add(curationUserSegment2);
            }
        }
    }

    public BratAnnotatorModel setBratAnnotatorModel(SourceDocument aSourceDocument)
    {
        User userLoggedIn = repository.getUser(SecurityContextHolder.getContext()
                .getAuthentication().getName());
        BratAnnotatorModel bratAnnotatorModel = new BratAnnotatorModel();// .getModelObject();
        bratAnnotatorModel.setDocument(aSourceDocument);
        bratAnnotatorModel.setProject(aSourceDocument.getProject());
        bratAnnotatorModel.setUser(userLoggedIn);
        bratAnnotatorModel.setFirstSentenceAddress(curationSegment.getSentenceAddress().get(
                CURATION_USER));
        bratAnnotatorModel.setLastSentenceAddress(curationSegment.getSentenceAddress().get(
                CURATION_USER));
        bratAnnotatorModel.setSentenceAddress(curationSegment.getSentenceAddress().get(
                CURATION_USER));
        bratAnnotatorModel.setMode(Mode.CURATION);
        AnnotationPreference preference = new AnnotationPreference();
        try {
            ApplicationUtils.setAnnotationPreference(preference, userLoggedIn.getUsername(),
                    repository, annotationService, bratAnnotatorModel, Mode.CURATION);
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
        return bratAnnotatorModel;
    }
}
