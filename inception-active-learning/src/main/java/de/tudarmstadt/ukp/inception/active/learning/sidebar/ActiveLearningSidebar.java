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
import java.util.LinkedList;
import java.util.List;

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
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.active.learning.model.LearningRecord;
import de.tudarmstadt.ukp.inception.active.learning.service.LearningRecordService;
import de.tudarmstadt.ukp.inception.app.session.SessionMetaData;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommendationService;

public class ActiveLearningSidebar
    extends
    AnnotationSidebar_ImplBase
{

    private static final long serialVersionUID = -5312616540773904224L;
    private static final Logger logger = LoggerFactory.getLogger(ActiveLearningSidebar.class);
    private static final String RECOMMENDATION_EDITOR_EXTENSION = "recommendationEditorExtension";
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean RecommendationService recommendationService;
    private @SpringBean LearningRecordService learningRecordService;
    private @SpringBean DocumentService documentService;

    private AnnotatorState annotatorState;
    private AnnotationLayer selectedLayer;
    private Model<String> selectedLayerContent;
    private Project project;
    private String layerSelectionButtonValue;
    private boolean startLearning = false;
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
        mainContainer.add(createRecommendationOperationForm());
        mainContainer.add(createLearningHistory());
        // mainContainer.add(createCurveMockUp());
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
        if (project != null && project.getId() != 0) {
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
                    activeLearningRecommender = new ActiveLearningRecommender(
                        recommendationService, annotatorState, selectedLayer,
                        annotationService, learningRecordService);
                    currentDifference = activeLearningRecommender
                        .generateRecommendationWithLowestDifference(documentService);
                    showAndHighlightRecommendationAndJumpToRecommendationLocation(target);
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
            currentRecommendation = currentDifference.getRecommendation1();
            setShowingRecommendation();
            jumpToRecommendationLocation(target);
            highlightRecommendation();

        }
    }

    private void setShowingRecommendation()
    {
        recommendationCoveredText = currentRecommendation.getCoveredText();
        recommendationAnnotation = currentRecommendation.getAnnotation();
        recommendationConfidence = currentRecommendation.getConfidence();
        recommendationDifference = currentDifference.getDifference();
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

    private void highlightRecommendation()
    {
        VID vid = new VID (RECOMMENDATION_EDITOR_EXTENSION, (int) selectedLayer.getId(),
            currentRecommendation.getRecommenderId(), currentRecommendation.getId(), VID.NONE,
            VID.NONE);
        annotatorState.getSelection().setAnnotation(vid);
    }

    private Form<?> createRecommendationOperationForm()
    {
        Form<?> recommendationForm = new Form<Void>("recommendationForm")
        {
            private static final long serialVersionUID = -5278670036137074736L;

            @Override
            public boolean isVisible()
            {
                return startLearning;
            }
        };

        recommendationForm.add(new Label("recommendationTitle", "Recommendation"));
        recommendationForm.add(createRecommendationCoveredTextLink());
        recommendationForm.add(new Label("recommendedPredition",
                new PropertyModel<String>(this, "recommendationAnnotation")));
        recommendationForm.add(new Label("confidence", "confidence:"));
        recommendationForm.add(new Label("recommendedConfidence",
            new PropertyModel<String>(this, "recommendationConfidence")));
        recommendationForm.add(new Label("difference", "difference:"));
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
                    .generateRecommendationWithLowestDifference(documentService);

                showAndHighlightRecommendationAndJumpToRecommendationLocation(target);
                target.add(mainContainer);
            }
        };
        return acceptRecommendationButton;
    }

    private void writeLearningRecordInDatabase(String userAction)
    {
        LearningRecord record = new LearningRecord();
        record.setUser(annotatorState.getUser().toString());
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
        int id = adapter.add(jCas, tokenBegin, tokenEnd);
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(selectedLayer)) {
            adapter.setFeatureValue(feature, jCas, id, acceptedRecommendation.getAnnotation());
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
                    .skipOrRejectRecommendationAndGetNextWithRegardToDifferences(
                            currentRecommendation);
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
                    .skipOrRejectRecommendationAndGetNextWithRegardToDifferences(
                            currentRecommendation);
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
        learningHistoryForm.add(new Label("learningHistoryTitle", "Learning History"));
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
                LearningRecord learningHistory = (LearningRecord) item
                        .getModelObject();
                item.add(new Label("recommendedText", learningHistory.getTokenText()));
                item.add(new Label("recommendedAnnotation",
                        learningHistory.getAnnotation()));
                item.add(new Label("userAction", learningHistory.getUserAction()));
            }
        };
        learningHistory.setModel(LambdaModel.of(this::listLearningRecords));
        return learningHistory;
    }

    private List<LearningRecord> listLearningRecords() {
        return learningRecordService.getAllRecordsByDocumentAndUserAndLayer
            (annotatorState.getDocument(), annotatorState.getUser().toString(),
                selectedLayer);
    }

    private Image createCurveMockUp()
    {
        Image image = new Image("curveImage",
                new PackageResourceReference(ActiveLearningSidebar.class, "curveMockUp.png"))
        {
            private static final long serialVersionUID = -3292184972896341246L;

            @Override
            public boolean isVisible()
            {
                return false;
            }
        };
        return image;
    }

}
