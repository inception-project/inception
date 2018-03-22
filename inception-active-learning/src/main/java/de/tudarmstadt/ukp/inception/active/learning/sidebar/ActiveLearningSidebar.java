/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
 */
package de.tudarmstadt.ukp.inception.active.learning.sidebar;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VAnnotationMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VTextMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningRecommendationEvent;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSessionCompletedEvent;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSessionStartedEvent;
import de.tudarmstadt.ukp.inception.app.session.SessionMetaData;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.Offset;
import de.tudarmstadt.ukp.inception.recommendation.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.service.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommendationService;

public class ActiveLearningSidebar
    extends
    AnnotationSidebar_ImplBase
{

    private static final long serialVersionUID = -5312616540773904224L;
    private static final Logger logger = LoggerFactory.getLogger(ActiveLearningSidebar.class);
    private static final String RECOMMENDATION_EDITOR_EXTENSION = "recommendationEditorExtension";
    private static final String ANNOTATION_MARKER = "VAnnotationMarker";
    private static final String TEXT_MARKER = "VTextMarker";
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean RecommendationService recommendationService;
    private @SpringBean LearningRecordService learningRecordService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;

    private AnnotatorState annotatorState;
    private AnnotationLayer selectedLayer;
    private Model<String> selectedLayerContent;
    private Project project;
    private String layerSelectionButtonValue;
    private boolean startLearning = false;
    private boolean hasUnseenRecommendation = false;
    private boolean hasSkippedRecommendation = false;
    private final WebMarkupContainer mainContainer;
    private static final String MAIN_CONTAINER = "mainContainer";
    private ActiveLearningRecommender activeLearningRecommender;
    private AnnotationObject currentRecommendation;
    private RecommendationDifference currentDifference;
    private String recommendationCoveredText;
    private String recommendationAnnotation;
    private double recommendationConfidence;
    private double recommendationDifference;
    private AnnotationPage annotationPage;
    private IModel<List<LearningRecord>> learningRecords;
    private Predictions predictionModel;
    private String vMarkerType = "";
    private VID highlightVID;
    private LearningRecord selectedRecord;
    private Date learnSkippedRecommendationTime;

    public ActiveLearningSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, JCasProvider aJCasProvider,
            AnnotationPage aAnnotationPage)
    {

        super(aId, aModel, aActionHandler, aJCasProvider, aAnnotationPage);

        annotationPage = aAnnotationPage;
        annotatorState = this.getModelObject();
        selectedLayer = annotatorState.getDefaultAnnotationLayer();
        selectedLayerContent = Model.of(selectedLayer.getUiName());
        project = Model.of(Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT)).getObject();
        mainContainer = new WebMarkupContainer(MAIN_CONTAINER);
        mainContainer.setOutputMarkupId(true);
        mainContainer.add(createLayerSelection());
        mainContainer.add(createNoRecommendationLabel());
        mainContainer.add(createLearnFromSkippedRecommendationForm());
        mainContainer.add(createRecommendationOperationForm());
        mainContainer.add(createLearningHistory());
        add(mainContainer);
    }

    private Form<?> createLayerSelection()
    {
        Form<?> layerSelectionForm = new Form<Void>("layerSelectionForm")
        {
            private static final long serialVersionUID = -4742012941499717823L;
        };

        final List<AnnotationLayer> layerList = this.getLayerChoices();
        layerSelectionForm.add(createLayerSelectionDropDownChoice(layerList));
        layerSelectionForm.add(createSelectedLayerLabel());
        layerSelectionForm.add(createLayerSelectionAndLearningStartTerminateButton());

        return layerSelectionForm;
    }

    private List<AnnotationLayer> getLayerChoices()
    {
        List<AnnotationLayer> layers = new LinkedList<>();
        if (project != null && project.getId() != null) {
            layers = annotationService.listAnnotationLayer(project);
        }
        return layers;

    }

    private DropDownChoice<AnnotationLayer> createLayerSelectionDropDownChoice(
            final List<AnnotationLayer> layerList)
    {
        DropDownChoice<AnnotationLayer> layerSelectionDropDownChoice =
            new DropDownChoice<AnnotationLayer>("selectLayer",
                new PropertyModel<AnnotationLayer>(this, "selectedLayer"),
                layerList,
                new ChoiceRenderer<>("uiName"))
        {
            private static final long serialVersionUID = 4656742112202628590L;

            @Override
            public boolean isVisible()
            {
                return !startLearning;
            }
        };
        return layerSelectionDropDownChoice;
    }

    private Label createSelectedLayerLabel()
    {
        Label selectedLayerLabel = new Label("showSelectedLayer", selectedLayerContent)
        {
            private static final long serialVersionUID = -4341859569165590267L;

            @Override
            public boolean isVisible()
            {
                return startLearning;
            }
        };
        return selectedLayerLabel;
    }

    private AjaxButton createLayerSelectionAndLearningStartTerminateButton()
    {
        AjaxButton layerSelectionAndLearningStartTerminateButton = new AjaxButton(
                "layerSelectionButton",
                new PropertyModel<String>(this, "layerSelectionButtonValue"))
        {
            private static final long serialVersionUID = -2488664683153797206L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form)
            {
                startLearning = !startLearning;
                layerSelectionButtonValue = startLearning ? "Terminate" : "Start";
                selectedLayerContent.setObject(selectedLayer.getUiName());
                annotatorState.setSelectedAnnotationLayer(selectedLayer);

                if (startLearning) {
                    learnSkippedRecommendationTime = null;
                    applicationEventPublisherHolder.get().publishEvent(
                        new ActiveLearningSessionStartedEvent(this, project,
                            annotatorState.getUser().getUsername()));
                    activeLearningRecommender = new ActiveLearningRecommender(recommendationService,
                        annotatorState, selectedLayer, annotationService, learningRecordService);
                    currentDifference = activeLearningRecommender
                        .generateRecommendationWithLowestDifference
                            (documentService, learnSkippedRecommendationTime);
                    showAndHighlightRecommendationAndJumpToRecommendationLocation(target);
                }

                if (!startLearning) {
                    applicationEventPublisherHolder.get().publishEvent(new
                        ActiveLearningSessionCompletedEvent(this, project, annotatorState
                        .getUser().getUsername()));
                }
                target.add(mainContainer);
            }
        };
        return layerSelectionAndLearningStartTerminateButton;
    }

    private void showAndHighlightRecommendationAndJumpToRecommendationLocation (AjaxRequestTarget
                                                                                    target)
    {
        if (currentDifference != null) {
            hasUnseenRecommendation = true;
            currentRecommendation = currentDifference.getRecommendation1();
            setShowingRecommendation();
            jumpToRecommendationLocation(target);
            highlightRecommendation();
        }
        else if (learnSkippedRecommendationTime == null) {
            hasUnseenRecommendation = false;
            hasSkippedRecommendation = activeLearningRecommender
                    .hasRecommendationWhichIsSkipped();
        }
        else {
            hasUnseenRecommendation = false;
            hasSkippedRecommendation = false;
        }
    }

    private void setShowingRecommendation()
    {
        recommendationCoveredText = currentRecommendation.getCoveredText();
        recommendationAnnotation = currentRecommendation.getAnnotation();
        recommendationConfidence = currentRecommendation.getConfidence();
        recommendationDifference = currentDifference.getDifference();
        writeLearningRecordInDatabase("shown");
        applicationEventPublisherHolder.get().publishEvent(
            new ActiveLearningRecommendationEvent(this, documentService.
                getSourceDocument(project, currentRecommendation.getDocumentName()),
                currentRecommendation, annotatorState.getUser().getUsername(), selectedLayer));
    }

    private void jumpToRecommendationLocation(AjaxRequestTarget target)
    {
        try {
            actionShowSelectedDocument(target, documentService.getSourceDocument
                    (project, currentRecommendation.getDocumentName()),
                currentRecommendation.getOffset().getBeginCharacter());
        } catch (IOException e) {
            logger.error("Error: " + e.getMessage(), e);
            error("Error: " + e.getMessage());
            target.addChildren(getPage(), IFeedback.class);
        }
    }

    private void highlightRecommendation() {
        predictionModel = recommendationService.getPredictions(annotatorState.getUser(),
            annotatorState.getProject());
        if (predictionModel != null) {
            AnnotationObject aoForVID = predictionModel.getPrediction(currentRecommendation
                    .getOffset(),
                currentRecommendation.getAnnotation());
            highlightVID = new VID(RECOMMENDATION_EDITOR_EXTENSION, selectedLayer.getId(),
                (int) aoForVID.getRecommenderId(), aoForVID.getId(),
                VID.NONE, VID.NONE);
            vMarkerType = ANNOTATION_MARKER;
        }
    }

    private Label createNoRecommendationLabel()
    {
        Label noRecommendation = new Label("noRecommendationLabel", "There are no further" +
            " suggestions.")
        {
            private static final long serialVersionUID = -4803396750094360587L;

            @Override
            public boolean isVisible()
            {
                return startLearning && !hasUnseenRecommendation && !hasSkippedRecommendation;
            }
        };
        return noRecommendation;
    }

    private Form<?> createLearnFromSkippedRecommendationForm()
    {
        Form<?> learnFromSkippedRecommendationForm = new Form<Void>("learnFromSkippedRecommendationForm")
        {
            private static final long serialVersionUID = -4803396750094360587L;

            @Override
            public boolean isVisible()
            {
                return startLearning && !hasUnseenRecommendation && hasSkippedRecommendation;
            }
        };
        learnFromSkippedRecommendationForm.add(new Label("onlySkippedRecommendationLabel", "There "
            + "are only skipped suggestions. Do you want to learn these again?"));
        learnFromSkippedRecommendationForm.add(new LambdaAjaxButton<>("learnSkippedOnes",
            this::learnSkippedRecommendations));
        return learnFromSkippedRecommendationForm;
    }


    private void learnSkippedRecommendations(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        learnSkippedRecommendationTime = new Date();
        currentDifference = activeLearningRecommender
            .generateRecommendationWithLowestDifference(documentService,
                learnSkippedRecommendationTime);

        showAndHighlightRecommendationAndJumpToRecommendationLocation(aTarget);
        aTarget.add(mainContainer);
    }

    private Form<?> createRecommendationOperationForm()
    {
        Form<?> recommendationForm = new Form<Void>("recommendationForm")
        {
            private static final long serialVersionUID = -5278670036137074736L;

            @Override
            public boolean isVisible()
            {
                return startLearning && hasUnseenRecommendation;
            }
        };

        recommendationForm.add(createRecommendationCoveredTextLink());
        recommendationForm.add(new Label("recommendedPredition",
                new PropertyModel<String>(this, "recommendationAnnotation")));
        recommendationForm.add(new Label("recommendedConfidence",
            new PropertyModel<String>(this, "recommendationConfidence")));
        recommendationForm.add(new Label("recommendedDifference",
            new PropertyModel<String>(this, "recommendationDifference")));
        recommendationForm.add(createAcceptRecommendationButton());
        recommendationForm.add(createSkipRecommendationButton());
        recommendationForm.add(createRejectRecommendationButton());
        return recommendationForm;
    }

    private LambdaAjaxLink createRecommendationCoveredTextLink()
    {
        LambdaAjaxLink recommendationCoveredTextLink =
            new LambdaAjaxLink("recommendationCoveredTextLink",
            this::jumpToRecommendationLocationAndHighlightRecommendation);

        recommendationCoveredTextLink.add(new Label("recommendedToken", new
            PropertyModel<String>(this, "recommendationCoveredText")));
        return recommendationCoveredTextLink;
    }

    private void jumpToRecommendationLocationAndHighlightRecommendation(AjaxRequestTarget aTarget)
        throws IOException {
        actionShowSelectedDocument(aTarget, documentService.getSourceDocument
                    (project, currentRecommendation.getDocumentName()),
                currentRecommendation.getOffset().getBeginCharacter());
        highlightRecommendation();
    }

    private AjaxButton createAcceptRecommendationButton()
    {
        AjaxButton acceptRecommendationButton = new AjaxButton("acceptButton")
        {

            private static final long serialVersionUID = 558230909116668597L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form)
            {
                writeLearningRecordInDatabase("accepted");

                try {
                    acceptRecommendationInAnnotationPage(target);
                }
                catch (AnnotationException | IOException e) {
                    logger.error("Error: " + e.getMessage(), e);
                    error("Error: " + e.getMessage());
                    target.addChildren(getPage(), IFeedback.class);
                }
                currentDifference = activeLearningRecommender
                    .generateRecommendationWithLowestDifference
                        (documentService, learnSkippedRecommendationTime);

                showAndHighlightRecommendationAndJumpToRecommendationLocation(target);
                target.add(mainContainer);
            }
        };
        return acceptRecommendationButton;
    }

    private void writeLearningRecordInDatabase(String userAction)
    {
        LearningRecord record = new LearningRecord();
        record.setUser(annotatorState.getUser().getUsername());
        record.setSourceDocument(annotatorState.getDocument());
        record.setTokenText(recommendationCoveredText);
        record.setUserAction(userAction);
        record.setOffsetTokenBegin(currentRecommendation.getOffset().getBeginToken());
        record.setOffsetTokenEnd(currentRecommendation.getOffset().getEndToken());
        record.setOffsetCharacterBegin(
            currentRecommendation.getOffset().getBeginCharacter());
        record.setOffsetCharacterEnd(currentRecommendation.getOffset().getEndCharacter());
        record.setAnnotation(recommendationAnnotation);
        record.setLayer(selectedLayer);
        record.setChangeLocation(LearningRecordChangeLocation.AL_SIDEBAR);

        learningRecordService.create(record);
    }

    private void acceptRecommendationInAnnotationPage(AjaxRequestTarget aTarget)
        throws AnnotationException, IOException
    {
        JCas jCas = this.getJCasProvider().get();

        SpanAdapter adapter = (SpanAdapter) annotationService.getAdapter(selectedLayer);
        AnnotationObject acceptedRecommendation = currentRecommendation;
        int tokenBegin = acceptedRecommendation.getOffset().getBeginCharacter();
        int tokenEnd = acceptedRecommendation.getOffset().getEndCharacter();
        int id = adapter.add(annotatorState, jCas, tokenBegin, tokenEnd);
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(selectedLayer)) {
            adapter.setFeatureValue(annotatorState, jCas, id, feature,
                    acceptedRecommendation.getAnnotation());
        }
        VID vid = new VID(id);
        annotatorState.getSelection().selectSpan(vid, jCas, tokenBegin, tokenEnd);
        AnnotationActionHandler aActionHandler = this.getActionHandler();
        aActionHandler.actionSelect(aTarget, jCas);
        aActionHandler.actionCreateOrUpdate(aTarget, jCas);
    }

    private AjaxButton createSkipRecommendationButton()
    {
        AjaxButton skipRecommendationButton = new AjaxButton("skipButton") {

            private static final long serialVersionUID = 402196878010685583L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form)
            {
                writeLearningRecordInDatabase("skipped");
                annotationPage.actionRefreshDocument(target);
                currentDifference = activeLearningRecommender
                    .generateRecommendationWithLowestDifference
                        (documentService, learnSkippedRecommendationTime);
                showAndHighlightRecommendationAndJumpToRecommendationLocation(target);
                target.add(mainContainer);
            }
        };

        return skipRecommendationButton;
    }

    private AjaxButton createRejectRecommendationButton()
    {
        AjaxButton rejectRecommendationButton = new AjaxButton("rejectButton")
        {
            private static final long serialVersionUID = 763990795394269L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form)
            {
                writeLearningRecordInDatabase("rejected");
                annotationPage.actionRefreshDocument(target);
                currentDifference = activeLearningRecommender
                    .generateRecommendationWithLowestDifference
                        (documentService, learnSkippedRecommendationTime);
                showAndHighlightRecommendationAndJumpToRecommendationLocation(target);
                target.add(mainContainer);
            }
        };
        return rejectRecommendationButton;
    }

    private Form<?> createLearningHistory()
    {
        Form<?> learningHistoryForm = new Form<Void>("learningHistoryForm")
        {

            private static final long serialVersionUID = -961690443085882064L;

            @Override
            public boolean isVisible()
            {
                return startLearning;
            }
        };

        learningHistoryForm.setOutputMarkupId(true);

        learningHistoryForm.add(createLearningHistoryListView());
        return learningHistoryForm;
    }

    private ListView<LearningRecord> createLearningHistoryListView()
    {
        ListView<LearningRecord> learningHistory = new ListView<LearningRecord>("historyListview")
        {
            private static final long serialVersionUID = 5594228545985423567L;

            @Override
            protected void populateItem(ListItem<LearningRecord> item)
            {
                LambdaAjaxLink jumpToAnnotationFromTokenTextLink = new LambdaAjaxLink(
                    "jumpToAnnotation",
                    t -> jumpAndHighlightFromLearningHistory(t, item.getModelObject()));

                jumpToAnnotationFromTokenTextLink.add(new Label("record",
                    item.getModelObject().getTokenText()));

                item.add(jumpToAnnotationFromTokenTextLink);
                item.add(new Label("record", item.getModelObject().getTokenText()));
                item.add(new Label("recommendedAnnotation",
                    item.getModelObject().getAnnotation()));
                item.add(new Label("userAction", item.getModelObject().getUserAction()));
                item.add(new LambdaAjaxLink("removeRecord",
                    t -> deleteRecordAndUpdateHistoryAndRedrawnMainPage(t, item.getModelObject())));
            }
        };
        learningRecords = LambdaModel.of(this::listLearningRecords);
        learningHistory.setModel(learningRecords);
        return learningHistory;
    }

    private void jumpAndHighlightFromLearningHistory(AjaxRequestTarget aTarget,
        LearningRecord record)
        throws IOException
    {
        actionShowSelectedDocument(aTarget, record.getSourceDocument(),
            record.getOffsetCharacterBegin());

        if (record.getUserAction().equals("rejected")) {
            highlightTextAndDisplayMessage(record);
        }
        // if the suggestion still exists, highlight that suggestion.
        else if (activeLearningRecommender.checkRecommendationExist(documentService, record)) {
            predictionModel = recommendationService
                .getPredictions(annotatorState.getUser(), annotatorState.getProject());

            Offset recordOffset = new Offset(record.getOffsetCharacterBegin(),
                record.getOffsetCharacterEnd(), record.getOffsetTokenBegin(),
                record.getOffsetTokenEnd());

            if (predictionModel != null) {
                AnnotationObject aoForVID = predictionModel
                    .getPrediction(recordOffset, record.getAnnotation());
                highlightVID = new VID(RECOMMENDATION_EDITOR_EXTENSION, selectedLayer.getId(),
                    (int) aoForVID.getRecommenderId(), aoForVID.getId(), VID.NONE, VID.NONE);
                vMarkerType = ANNOTATION_MARKER;
            }
        }
        // if the suggestion doesn't exit -> if that suggestion is accepted and annotated,
        // highlight the annotation.
        // else, highlight the text.
        else if (!isAnnotatedInCas(record)) {
            highlightTextAndDisplayMessage(record);
        }
    }

    private boolean isAnnotatedInCas(LearningRecord aRecord)
        throws IOException
    {
        JCas aJcas = this.getJCasProvider().get();
        Type type = CasUtil.getType(aJcas.getCas(), selectedLayer.getName());
        AnnotationFS annotationFS = WebAnnoCasUtil
            .selectSingleFsAt(aJcas, type, aRecord.getOffsetCharacterBegin(),
                aRecord.getOffsetCharacterEnd());
        if (annotationFS != null) {
            for (AnnotationFeature annotationFeature : annotationService
                .listAnnotationFeature(selectedLayer)) {
                String annotatedValue = WebAnnoCasUtil
                    .getFeature(annotationFS, annotationFeature.getName());
                if (annotatedValue.equals(aRecord.getAnnotation())) {
                    highlightVID = new VID(WebAnnoCasUtil.getAddr(annotationFS));
                    vMarkerType = ANNOTATION_MARKER;
                    return true;
                }
            }
        }
        return false;
    }

    private void highlightTextAndDisplayMessage(LearningRecord record) {
        selectedRecord = record;
        vMarkerType = TEXT_MARKER;
        error("No annotation could be highlighted.");
    }


    private List<LearningRecord> listLearningRecords() {
        return learningRecordService.getAllRecordsByDocumentAndUserAndLayer
            (annotatorState.getDocument(), annotatorState.getUser().getUsername(),
                selectedLayer);
    }

    private void deleteRecordAndUpdateHistoryAndRedrawnMainPage(AjaxRequestTarget aTarget,
                                                                LearningRecord aRecord) {
        learningRecordService.delete(aRecord);
        learningRecords.detach();
        aTarget.add(mainContainer);
        annotationPage.actionRefreshDocument(aTarget);
    }

    @OnEvent
    public void onRecommendationRejectEvent(AjaxRecommendationRejectedEvent aEvent)
    {
        predictionModel = recommendationService.getPredictions(annotatorState.getUser(),
            annotatorState.getProject());
        AnnotatorState state = aEvent.getAnnotatorState();
        if (startLearning && state.getUser().equals(annotatorState.getUser()) && state.getProject
            ().equals(this.project)) {
            if (state.getDocument().equals(annotatorState.getDocument()) &&
                aEvent.getVid().getLayerId() == selectedLayer.getId() &&
                predictionModel.getPredictionByVID(aEvent.getVid()).equals(currentRecommendation)) {
                currentDifference = activeLearningRecommender
                    .generateRecommendationWithLowestDifference
                        (documentService, learnSkippedRecommendationTime);
                showAndHighlightRecommendationAndJumpToRecommendationLocation(aEvent.getTarget())
                ;
            }
            aEvent.getTarget().add(mainContainer);
        }
    }

    @OnEvent
    public void onRecommendationAcceptEvent(AjaxRecommendationAcceptedEvent aEvent)
    {
        predictionModel = recommendationService.getPredictions(annotatorState.getUser(),
            annotatorState.getProject());
        AnnotatorState state = aEvent.getAnnotatorState();
        AnnotationObject acceptedRecommendation =
            predictionModel.getPredictionByVID(aEvent.getVid());

        LearningRecord record = new LearningRecord();
        record.setUser(state.getUser().getUsername());
        record.setSourceDocument(state.getDocument());
        record.setTokenText(acceptedRecommendation.getCoveredText());
        record.setUserAction("accepted");
        record.setOffsetTokenBegin(acceptedRecommendation.getOffset().getBeginToken());
        record.setOffsetTokenEnd(acceptedRecommendation.getOffset().getEndToken());
        record.setOffsetCharacterBegin(
            acceptedRecommendation.getOffset().getBeginCharacter());
        record.setOffsetCharacterEnd(acceptedRecommendation.getOffset().getEndCharacter());
        record.setAnnotation(acceptedRecommendation.getAnnotation());
        record.setLayer(annotationService.getLayer(aEvent.getVid().getLayerId()));
        record.setChangeLocation(LearningRecordChangeLocation.MAIN_EDITOR);
        learningRecordService.create(record);

        if (startLearning && state.getUser().equals(annotatorState.getUser()) && state.getProject
            ().equals(this.project)) {
            if (acceptedRecommendation.getOffset().equals(currentRecommendation.getOffset())) {
                if (!acceptedRecommendation.equals(currentRecommendation)) {
                    writeLearningRecordInDatabase("rejected");
                }
                currentDifference = activeLearningRecommender
                    .generateRecommendationWithLowestDifference
                        (documentService, learnSkippedRecommendationTime);
                showAndHighlightRecommendationAndJumpToRecommendationLocation(aEvent.getTarget());
            }
            aEvent.getTarget().add(mainContainer);
        }
    }


    @OnEvent public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        if (vMarkerType.equals(ANNOTATION_MARKER)) {
            if (highlightVID != null) {
                aEvent.getVDocument().add(new VAnnotationMarker(VMarker.FOCUS, highlightVID));
            }
        }
        else if (vMarkerType.equals(TEXT_MARKER)) {
            if (selectedRecord != null) {
                if (annotatorState.getWindowBeginOffset() <= selectedRecord
                    .getOffsetCharacterBegin()
                    && selectedRecord.getOffsetCharacterEnd() <= annotatorState
                    .getWindowEndOffset()) {
                    aEvent.getVDocument().add(new VTextMarker(VMarker.FOCUS,
                        selectedRecord.getOffsetCharacterBegin() - annotatorState
                            .getWindowBeginOffset(),
                        selectedRecord.getOffsetCharacterEnd() - annotatorState
                            .getWindowBeginOffset()));
                }
            }
        }
    }
}
