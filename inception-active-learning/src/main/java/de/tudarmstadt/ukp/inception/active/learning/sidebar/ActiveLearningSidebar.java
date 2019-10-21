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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.active.learning.sidebar.ActiveLearningUserStateMetaData.CURRENT_AL_USER_STATE;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_CORRECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.CORRECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.SKIPPED;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VAnnotationMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VTextMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentResetEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningService;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningServiceImpl;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningServiceImpl.ActiveLearningUserState;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningRecommendationEvent;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSessionCompletedEvent;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSessionStartedEvent;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSuggestionOfferedEvent;
import de.tudarmstadt.ukp.inception.active.learning.strategy.UncertaintySamplingStrategy;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup.Delta;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.PredictionsSwitchedEvent;

public class ActiveLearningSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final String CID_EDITOR = "editor";

    private static final long serialVersionUID = -5312616540773904224L;

    private static final Logger LOG = LoggerFactory.getLogger(ActiveLearningSidebar.class);

    // Wicket component IDs used in the HTML file
    private static final String CID_MAIN_CONTAINER = "mainContainer";
    private static final String CID_HISTORY_LISTVIEW = "historyListview";
    private static final String CID_LEARNING_HISTORY_FORM = "learningHistoryForm";
    private static final String CID_REJECT_BUTTON = "rejectButton";
    private static final String CID_SKIP_BUTTON = "skipButton";
    private static final String CID_ANNOTATE_BUTTON = "annotateButton";
    private static final String CID_RECOMMENDATION_COVERED_TEXT_LINK = "recommendationCoveredTextLink";
    private static final String CID_RECOMMENDED_DIFFERENCE = "recommendedDifference";
    private static final String CID_RECOMMENDED_CONFIDENCE = "recommendedConfidence";
    private static final String CID_RECOMMENDED_PREDITION = "recommendedPredition";
    private static final String CID_RECOMMENDATION_FORM = "recommendationForm";
    private static final String CID_LEARN_SKIPPED_ONES = "learnSkippedOnes";
    private static final String CID_ONLY_SKIPPED_RECOMMENDATION_LABEL = "onlySkippedRecommendationLabel";
    private static final String CID_LEARN_FROM_SKIPPED_RECOMMENDATION_FORM = "learnFromSkippedRecommendationForm";
    private static final String CID_NO_RECOMMENDATION_LABEL = "noRecommendationLabel";
    private static final String CID_START_SESSION_BUTTON = "startSession";
    private static final String CID_STOP_SESSION_BUTTON = "stopSession";
    private static final String CID_SELECT_LAYER = "selectLayer";
    private static final String CID_SESSION_CONTROL_FORM = "sessionControlForm";
    private static final String CID_REMOVE_RECORD = "removeRecord";
    private static final String CID_USER_ACTION = "userAction";
    private static final String CID_RECOMMENDED_ANNOTATION = "recommendedAnnotation";
    private static final String CID_JUMP_TO_ANNOTATION = "jumpToAnnotation";
    private static final String CID_NO_RECOMMENDERS = "noRecommenders";
    private static final String CID_CONFIRMATION_DIALOG = "confirmationDialog";

    private @SpringBean ActiveLearningService activeLearningService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean RecommendationService recommendationService;
    private @SpringBean LearningRecordService learningRecordService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;
    private @SpringBean UserDao userDao;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private IModel<List<LearningRecord>> learningRecords;
    private CompoundPropertyModel<ActiveLearningServiceImpl.ActiveLearningUserState> alStateModel;

    private final WebMarkupContainer mainContainer;

    private AnnotationPage annotationPage;
    private ConfirmationDialog confirmationDialog;
    private FeatureEditor editor;
    private Form<Void> recommendationForm;

    private String highlightDocumentName;
    private VID highlightVID;
    private Offset highlightSpan;
    private boolean protectHighlight;
    
    public ActiveLearningSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);

        annotationPage = aAnnotationPage;

        // Instead of maintaining the AL state in the sidebar, we maintain it in the page because
        // that way we persists even if we switch to another sidebar tab
        alStateModel = new CompoundPropertyModel<>(LambdaModelAdapter.of(
            () -> aAnnotationPage.getMetaData(CURRENT_AL_USER_STATE),
            alState -> aAnnotationPage.setMetaData(CURRENT_AL_USER_STATE, alState)));

        // Set up the AL state in the page if it is not already there or if for some reason the
        // suggestions have completely disappeared (e.g. after a system restart)
        AnnotatorState state = getModelObject();
        Predictions predictions = recommendationService.getPredictions(state.getUser(),
                state.getProject());
        if (aAnnotationPage.getMetaData(CURRENT_AL_USER_STATE) == null || predictions == null) {
            ActiveLearningUserState alState = new ActiveLearningUserState();
            alState.setStrategy(new UncertaintySamplingStrategy());
            alStateModel.setObject(alState);;
        }
        
        mainContainer = new WebMarkupContainer(CID_MAIN_CONTAINER);
        mainContainer.setOutputMarkupId(true);
        mainContainer.add(createNoRecommendersMessage());
        mainContainer.add(createSessionControlForm());
        mainContainer.add(createNoRecommendationLabel());
        mainContainer.add(clearSkippedRecommendationForm());
        mainContainer.add(createRecommendationOperationForm());
        mainContainer.add(createLearningHistory());
        add(mainContainer);
        
        confirmationDialog = new ConfirmationDialog(CID_CONFIRMATION_DIALOG);
        confirmationDialog.setAutoSize(true);
        add(confirmationDialog);
    }

    private Label createNoRecommendersMessage()
    {
        if (!alStateModel.getObject().isSessionActive()) {
            // Use the currently selected layer from the annotation detail editor panel as the
            // default choice in the active learning mode.
            List<AnnotationLayer> layersWithRecommenders = listLayersWithRecommenders();
            if (layersWithRecommenders.contains(getModelObject().getDefaultAnnotationLayer())) {
                alStateModel.getObject()
                    .setLayer(getModelObject().getDefaultAnnotationLayer());
            }
            // If the currently selected layer has no recommenders, use the first one which has
            else if (!layersWithRecommenders.isEmpty()) {
                alStateModel.getObject().setLayer(layersWithRecommenders.get(0));
            }
            // If there are no layers with recommenders, then choose nothing and show no
            // recommenders message.
            else {
                alStateModel.getObject().setLayer(null);
                alStateModel.getObject().setDoExistRecommenders(false);
            }
        }
        Label noRecommendersMessage = new Label(CID_NO_RECOMMENDERS, "None of the layers have any "
            + "recommenders configured. Please set the recommenders first in the Project "
            + "Settings.");
        noRecommendersMessage.add(LambdaBehavior.onConfigure(component -> component.setVisible
            (!alStateModel.getObject().isDoExistRecommenders())));
        return noRecommendersMessage;
    }

    private Form<Void> createSessionControlForm()
    {
        Form<Void> form = new Form<>(CID_SESSION_CONTROL_FORM);

        DropDownChoice<AnnotationLayer> layersDropdown = new BootstrapSelect<>(CID_SELECT_LAYER);
        layersDropdown.setModel(alStateModel.bind("layer"));
        layersDropdown.setChoices(LoadableDetachableModel.of(this::listLayersWithRecommenders));
        layersDropdown.setChoiceRenderer(new LambdaChoiceRenderer<>(AnnotationLayer::getUiName));
        layersDropdown.add(LambdaBehavior.onConfigure(it -> it.setEnabled(!alStateModel
            .getObject().isSessionActive())));
        layersDropdown.setOutputMarkupId(true);
        layersDropdown.setRequired(true);
        form.add(layersDropdown);
        
        form.add(new LambdaAjaxSubmitLink(CID_START_SESSION_BUTTON, this::actionStartSession)
                .add(visibleWhen(() -> !alStateModel.getObject().isSessionActive())));
        form.add(new LambdaAjaxLink(CID_STOP_SESSION_BUTTON, this::actionStopSession)
            .add(visibleWhen(() -> alStateModel.getObject().isSessionActive())));
        form.add(visibleWhen(() -> alStateModel.getObject().isDoExistRecommenders()));

        return form;
    }

    private List<AnnotationLayer> listLayersWithRecommenders()
    {
        return recommendationService
                .listLayersWithEnabledRecommenders(getModelObject().getProject());
    }
    
    private void actionStartSession(AjaxRequestTarget target, Form<?> form)
    {
        ActiveLearningUserState alState = alStateModel.getObject();
        AnnotatorState state = getModelObject();
        
        // Start new session
        alState.setSessionActive(true);

        refreshSuggestions();

        moveToNextRecommendation(target, false);

        applicationEventPublisherHolder.get().publishEvent(new ActiveLearningSessionStartedEvent(
                this, state.getProject(), state.getUser().getUsername()));
    }
    
    private void actionStopSession(AjaxRequestTarget target)
    {
        ActiveLearningUserState alState = alStateModel.getObject();
        AnnotatorState state = getModelObject();

        target.add(mainContainer);

        // Stop current session
        alState.setSessionActive(false);

        applicationEventPublisherHolder.get().publishEvent(new ActiveLearningSessionCompletedEvent(
                this, state.getProject(), state.getUser().getUsername()));
    }

    private void setHighlight(AnnotationSuggestion aSuggestion)
    {
        if (protectHighlight) {
            LOG.trace("Active learning sidebar not updating protected highlights");
            protectHighlight = false;
            return;
        }
        
        LOG.trace("Active learning sidebar set highlight suggestion: {}", aSuggestion);
        highlightVID = aSuggestion.getVID();
        highlightSpan = new Offset(aSuggestion.getBegin(), aSuggestion.getEnd());
        highlightDocumentName = aSuggestion.getDocumentName();
    }
    
    private void setHighlight(LearningRecord aRecord)
    {
        LOG.trace("Active learning sidebar set highlight history record: {}", aRecord);
        highlightVID = null;
        highlightSpan = new Offset(aRecord.getOffsetCharacterBegin(),
                aRecord.getOffsetCharacterEnd());
        highlightDocumentName = aRecord.getSourceDocument().getName();
        // This is a bit of hack. Consider the following case:
        // - use removes an ACCEPT history item
        // - user clicks then on another history item
        // - ... but during the subsequent rendering the "moveToNextSuggestion" method sets or
        //   clears the highlight.
        protectHighlight = true;
    }
    
    private void setHighlight(SourceDocument aDocument, AnnotationFS aAnnotation)
    {
        LOG.trace("Active learning sidebar set highlight annotation: {}", aAnnotation);
        highlightVID = new VID(WebAnnoCasUtil.getAddr(aAnnotation));
        highlightSpan = new Offset(aAnnotation.getBegin(),
                aAnnotation.getEnd());
        highlightDocumentName = aDocument.getName();
        protectHighlight = false;
    }
    
    private void clearHighlight()
    {
        if (protectHighlight) {
            LOG.trace("Active learning sidebar not clearing protected highlights");
            protectHighlight = false;
            return;
        }
        
        LOG.trace("Active learning sidebar cleared highlights");
        highlightDocumentName = null;
        highlightSpan = null;
        highlightVID = null;
    }

    private Label createNoRecommendationLabel()
    {
        Label noRecommendation = new Label(CID_NO_RECOMMENDATION_LABEL,
                "There are no further suggestions.");
        noRecommendation.add(visibleWhen(() -> {
            ActiveLearningUserState alState = alStateModel.getObject();
            return alState.isSessionActive() 
                    && !alState.getSuggestion().isPresent()
                    && !activeLearningService.hasSkippedSuggestions(
                            getModelObject().getUser(), alState.getLayer());
        }));
        noRecommendation.setOutputMarkupPlaceholderTag(true);
        return noRecommendation;
    }

    private Form<Void> clearSkippedRecommendationForm()
    {
        Form<Void> form = new Form<>(CID_LEARN_FROM_SKIPPED_RECOMMENDATION_FORM);
        form.add(LambdaBehavior.visibleWhen(() -> {
            ActiveLearningUserState alState = alStateModel.getObject();
            return alState.isSessionActive() 
                    && !alState.getSuggestion().isPresent()
                    && activeLearningService.hasSkippedSuggestions(
                            getModelObject().getUser(), alState.getLayer());
        }));
        form.setOutputMarkupPlaceholderTag(true);
        form.add(new Label(CID_ONLY_SKIPPED_RECOMMENDATION_LABEL,
                "There are only skipped suggestions. Do you want to learn these again?"));
        form.add(new LambdaAjaxButton<>(CID_LEARN_SKIPPED_ONES,
                this::actionClearSkippedRecommendations));
        return form;
    }

    private void actionClearSkippedRecommendations(AjaxRequestTarget aTarget, Form<Void> aForm)
        throws IOException
    {
        learningRecordService.deleteSkippedSuggestions(getModelObject().getUser(),
                alStateModel.getObject().getLayer());

        ActiveLearningUserState alState = alStateModel.getObject();

        // The history records caused suggestions to disappear. Since visibility is only fully
        // recalculated when new predictions come in, we need to update the visibility explicitly
        // here
        alState.getSuggestions().stream()
            .flatMap(group -> group.stream())
            .forEach(suggestion -> suggestion.show(FLAG_SKIPPED));
        
        refreshSuggestions();
        moveToNextRecommendation(aTarget, false);
        
        aTarget.add(mainContainer);
    }

    private Form<Void> createRecommendationOperationForm()
    {
        recommendationForm = new Form<Void>(CID_RECOMMENDATION_FORM);
        recommendationForm.add(LambdaBehavior.visibleWhen(() -> {
            ActiveLearningUserState alState = alStateModel.getObject();
            return alState.isSessionActive() && alState.getSuggestion().isPresent();
        }));
        recommendationForm.setOutputMarkupPlaceholderTag(true);

        recommendationForm.add(createJumpToSuggestionLink());
        recommendationForm.add(new Label(CID_RECOMMENDED_PREDITION,
                LoadableDetachableModel.of(() -> alStateModel.getObject()
                        .getSuggestion().map(this::formatLabel).orElse(null))));
        recommendationForm.add(new Label(CID_RECOMMENDED_CONFIDENCE, () -> 
                alStateModel.getObject().getSuggestion()
                        .map(AnnotationSuggestion::getConfidence).orElse(null)));
        recommendationForm.add(new Label(CID_RECOMMENDED_DIFFERENCE, () -> 
                alStateModel.getObject().getCurrentDifference()
                        .map(Delta::getDelta).orElse(null)));
        recommendationForm.add((alStateModel.getObject().getLayer() != null
            && alStateModel.getObject().getSuggestion().isPresent()) ?
            initializeFeatureEditor() :
            new Label(CID_EDITOR).setVisible(false));

        recommendationForm.add(new LambdaAjaxButton<>(CID_ANNOTATE_BUTTON, this::actionAnnotate));
        recommendationForm.add(new LambdaAjaxLink(CID_SKIP_BUTTON, this::actionSkip));
        recommendationForm.add(new LambdaAjaxLink(CID_REJECT_BUTTON, this::actionReject));

        return recommendationForm;
    }

    private String formatLabel(AnnotationSuggestion aCurrentRecommendation)
    {
        AnnotationFeature feat = annotationService.getFeature(aCurrentRecommendation.getFeature(),
                alStateModel.getObject().getLayer());
        FeatureSupport<?> featureSupport = featureSupportRegistry.getFeatureSupport(feat);
        String labelValue = featureSupport.renderFeatureValue(feat,
                aCurrentRecommendation.getLabel());
        return labelValue;
    }

    private LambdaAjaxLink createJumpToSuggestionLink()
    {
        LambdaAjaxLink link = new LambdaAjaxLink(CID_RECOMMENDATION_COVERED_TEXT_LINK,
                this::actionJumpToSuggestion);
        link.add(new Label("leftContext",
                LoadableDetachableModel.of(() -> alStateModel.getObject().getLeftContext())));
        link.add(new Label("text", LoadableDetachableModel.of(() -> alStateModel.getObject()
                .getSuggestion().map(AnnotationSuggestion::getCoveredText).orElse(""))));
        link.add(new Label("rightContext",
                LoadableDetachableModel.of(() -> alStateModel.getObject().getRightContext())));
        return link;
    }

    private void actionJumpToSuggestion(AjaxRequestTarget aTarget)
        throws IOException
    {
        ActiveLearningUserState alState = alStateModel.getObject();
        
        AnnotationSuggestion suggestion = alState.getSuggestion().get();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Active suggestion: {}", suggestion);
            Optional<AnnotationSuggestion> updatedSuggestion = getMatchingSuggestion(
                    activeLearningService.getSuggestions(getModelObject().getUser(),
                            alState.getLayer()),
                    suggestion).stream().findFirst();
            updatedSuggestion.ifPresent(s -> LOG.debug("Update suggestion: {}", s));
        }

        actionShowSelectedDocument(aTarget,
                documentService.getSourceDocument(this.getModelObject().getProject(),
                        suggestion.getDocumentName()),
                suggestion.getBegin(), suggestion.getEnd());

        setHighlight(suggestion);
    }

    private Component initializeFeatureEditor()
    {
        editor = createFeatureEditor(alStateModel.getObject().getSuggestion().get());
        return editor;
    }

    private void refreshFeatureEditor(IPartialPageRequestHandler aTarget,
            AnnotationSuggestion aCurrentRecommendation)
    {
        editor = createFeatureEditor(aCurrentRecommendation);
        recommendationForm.addOrReplace(editor);
        aTarget.add(mainContainer);
    }
    
    private FeatureEditor createFeatureEditor(AnnotationSuggestion aCurrentRecommendation)
    {
        AnnotatorState state = ActiveLearningSidebar.this.getModelObject();
        ActiveLearningUserState alState = alStateModel.getObject();
        
        // Obtain the feature state which serves as a model to the editor
        AnnotationFeature feat = annotationService.getFeature(aCurrentRecommendation.getFeature(),
                alState.getLayer());
        FeatureSupport<?> featureSupport = featureSupportRegistry.getFeatureSupport(feat);
        // We get away with passing "null" here instead of the CAS because we currently 
        // have no recommenders for any feature types that actually need the CAS (i.e.
        // link feature types and the likes).
        Object wrappedFeatureValue = featureSupport.wrapFeatureValue(feat, null,
                aCurrentRecommendation.getLabel());
        FeatureState featureState = new FeatureState(aCurrentRecommendation.getVID(), 
            feat, (Serializable) wrappedFeatureValue);
        
        // Populate the tagset moving the tags with recommended labels to the top 
        List<Tag> tagList = annotationService.listTags(feat.getTagset());
        List<Tag> reorderedTagList = new ArrayList<>();
        if (tagList.size() > 0) {
            Predictions predictions = recommendationService.getPredictions(state.getUser(),
                    state.getProject());
            // get all the predictions
            List<AnnotationSuggestion> allRecommendations = predictions
                    .getPredictionsByTokenAndFeature(
                            aCurrentRecommendation.getDocumentName(),
                            alState.getLayer(),
                            aCurrentRecommendation.getBegin(),
                            aCurrentRecommendation.getEnd(),
                            aCurrentRecommendation.getFeature());
            // get all the label of the predictions (e.g. "NN")
            List<String> allRecommendationLabels = allRecommendations.stream()
                    .map(ao -> ao.getLabel())
                    .collect(Collectors.toList());
            
            for (Tag tag : tagList) {
                // add the tags which contain the prediction-labels to the beginning of a tagset
                if (allRecommendationLabels.contains(tag.getName())) {
                    tag.setReordered(true);
                    reorderedTagList.add(tag);
                }
            }
            
            // remove these tags containing the prediction-labels
            tagList.removeAll(reorderedTagList);
            
            // add the rest tags to the tagset after these
            reorderedTagList.addAll(tagList);
        }
        featureState.tagset = reorderedTagList;
        
        // Finally, create the editor
        FeatureEditor featureEditor = featureSupport.createEditor(CID_EDITOR, mainContainer,
                this.getActionHandler(), this.getModel(), Model.of(featureState));
        featureEditor.setOutputMarkupPlaceholderTag(true);
        featureEditor.add(visibleWhen(() -> alStateModel.getObject().getLayer() != null
                && alState.getSuggestion().isPresent()));
        return featureEditor;
    }
    
    private void writeLearningRecordInDatabaseAndEventLog(
            AnnotationSuggestion aCurrentRecommendation, LearningRecordType aUserAction)
    {
        writeLearningRecordInDatabaseAndEventLog(aCurrentRecommendation, aUserAction,
                aCurrentRecommendation.getLabel());
    }

    private void writeLearningRecordInDatabaseAndEventLog(AnnotationSuggestion aSuggestion,
            LearningRecordType aUserAction, String aAnnotationValue)
    {
        AnnotatorState state = ActiveLearningSidebar.this.getModelObject();
        ActiveLearningUserState alState = alStateModel.getObject();

        AnnotationFeature feat = annotationService.getFeature(aSuggestion.getFeature(),
                alState.getLayer());
        SourceDocument sourceDoc = documentService.getSourceDocument(state.getProject(),
                aSuggestion.getDocumentName());

        // Log the action to the learning record
        learningRecordService.logRecord(sourceDoc, state.getUser().getUsername(),
                aSuggestion, aAnnotationValue, alState.getLayer(), feat, aUserAction,
                LearningRecordChangeLocation.AL_SIDEBAR);

        // Send an application event that the suggestion has been rejected
        List<AnnotationSuggestion> alternativeSuggestions = recommendationService
                .getPredictions(state.getUser(), state.getProject())
                .getPredictionsByTokenAndFeature(aSuggestion.getDocumentName(), alState.getLayer(),
                        aSuggestion.getBegin(), aSuggestion.getEnd(), aSuggestion.getFeature());

        applicationEventPublisherHolder.get()
                .publishEvent(new ActiveLearningRecommendationEvent(this, sourceDoc, aSuggestion,
                        state.getUser().getUsername(), alState.getLayer(), aSuggestion.getFeature(),
                        aUserAction, alternativeSuggestions));
    }

    /**
     * Accept a suggestion or a corrected suggestion via the sidebar. If the value in the feature
     * editor corresponds to the originally suggested label, an acceptance is logged, otherwise
     * a correction is logged.
     * 
     * <ul>
     * <li>Creates a new annotation or updates an existing one with a new feature
     * value.</li>
     * <li>Marks the suggestions as hidden (not visible).</li>
     * <li>Logs the accepting to the learning log.</li>
     * <li>Sends events to the UI and application informing other components about the action.</li>
     * </ul>
     */    
    private void actionAnnotate(AjaxRequestTarget aTarget, Form<Void> aForm)
        throws IOException, AnnotationException
    {
        AnnotatorState state = ActiveLearningSidebar.this.getModelObject();
        ActiveLearningUserState alState = alStateModel.getObject();
        
        // There is always a current recommendation when we get here because if there is none, the
        // button to accept the recommendation is not visible.
        AnnotationSuggestion suggestion = alState.getSuggestion().get();
        
        // Create AnnotationFeature and FeatureSupport
        AnnotationFeature feat = annotationService.getFeature(suggestion.getFeature(),
                alState.getLayer());
        FeatureSupport featureSupport = featureSupportRegistry.getFeatureSupport(feat);

        // Load CAS in which to create the annotation. This might be different from the one that
        // is currently viewed by the user, e.g. if the user switched to another document after
        // the suggestion has been loaded into the sidebar.
        SourceDocument sourceDoc = documentService.getSourceDocument(state.getProject(),
                suggestion.getDocumentName());
        String username = state.getUser().getUsername();
        CAS cas = documentService.readAnnotationCas(sourceDoc, username);

        // Upsert an annotation based on the suggestion
        String selectedValue = (String) featureSupport.unwrapFeatureValue(feat, cas,
                editor.getModelObject().value);
        AnnotationLayer layer = annotationService
                .getLayer(state.getProject(), suggestion.getLayerId())
                .orElseThrow(() -> new IllegalArgumentException("No such layer: [" + 
                        suggestion.getLayerId() + "]"));
        AnnotationFeature feature = annotationService.getFeature(suggestion.getFeature(), layer);
        recommendationService.upsertFeature(annotationService, sourceDoc, username, cas, layer,
                feature, selectedValue, suggestion.getBegin(), suggestion.getEnd());
        
        // Save CAS after annotation has been created
        documentService.writeAnnotationCas(cas, sourceDoc, state.getUser(), true);
        
        // If the currently displayed document is the same one where the annotation was created,
        // then update timestamp in state to avoid concurrent modification errors
        if (Objects.equals(state.getDocument().getId(), sourceDoc.getId())) {
            Optional<Long> diskTimestamp = documentService.getAnnotationCasTimestamp(sourceDoc,
                    username);
            if (diskTimestamp.isPresent()) {
                state.setAnnotationDocumentTimestamp(diskTimestamp.get());
            }
        }

        boolean areLabelsEqual = suggestion.labelEquals(selectedValue);

        suggestion.hide((areLabelsEqual) ? FLAG_TRANSIENT_ACCEPTED
                : FLAG_TRANSIENT_CORRECTED);

        // Log the action to the learning record
        writeLearningRecordInDatabaseAndEventLog(suggestion,
                (areLabelsEqual) ? ACCEPTED
                        : CORRECTED,
                selectedValue);
        
        recommendationService.getPredictions(state.getUser(), state.getProject())
                .getPredictionsByTokenAndFeature(suggestion.getDocumentName(),
                        alStateModel.getObject().getLayer(), suggestion.getBegin(),
                        suggestion.getEnd(), feat.getName());

        moveToNextRecommendation(aTarget, false);
    }

    private void actionSkip(AjaxRequestTarget aTarget)
    {
        alStateModel.getObject().getSuggestion().ifPresent(rec -> {
            writeLearningRecordInDatabaseAndEventLog(rec, SKIPPED);
            moveToNextRecommendation(aTarget, false);
        });
    }

    private void actionReject(AjaxRequestTarget aTarget)
    {
        alStateModel.getObject().getSuggestion().ifPresent(rec -> {
            writeLearningRecordInDatabaseAndEventLog(rec, REJECTED);
            moveToNextRecommendation(aTarget, false);
        });
    }
    
    private void moveToNextRecommendation(IPartialPageRequestHandler aTarget, boolean aStay)
    {
        AnnotatorState state = getModelObject();
        ActiveLearningUserState alState = alStateModel.getObject();
        
        // Clear the annotation detail editor and the selection to avoid confusions with the 
        // highlight because the selection highlight from the right sidebar and the one from the
        // AL sidebar have the same color!
        if (!aStay) {
            state.getSelection().clear();
            aTarget.add((Component) getActionHandler());
        }

        Optional<Delta> recommendationDifference = activeLearningService
                .generateNextSuggestion(state.getUser(), alState);
        Optional<AnnotationSuggestion> prevSuggestion = alState.getSuggestion();
        alState.setCurrentDifference(recommendationDifference);
        
        // If the active suggestion has changed, inform the user
        if (prevSuggestion.isPresent() && (alState.getSuggestion().isPresent()
                && !alState.getSuggestion().get().equals(prevSuggestion.get()))) {
            String message = "Active learning has moved to next best suggestion.";
            // Avoid logging the message multiple times in case the move to the next suggestion has
            // been requested multiple times in a single request
            if (getFeedbackMessages().messages(msg -> msg.getMessage().equals(message)).isEmpty()) {
                info(message);
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }
        else if (prevSuggestion.isPresent() && !alState.getSuggestion().isPresent()) {
            String message = "There are no more recommendations right now.";
            // Avoid logging the message multiple times in case the move to the next suggestion has
            // been requested multiple times in a single request
            if (getFeedbackMessages().messages(msg -> msg.getMessage().equals(message)).isEmpty()) {
                info(message);
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }

        // If there is no new suggestion, nothing left to do here
        if (!alState.getSuggestion().isPresent()) {
            clearHighlight();
            aTarget.add(mainContainer);
            if (!aStay) {
                //Main editor
                annotationPage.actionRefreshDocument((AjaxRequestTarget) aTarget);
            }
            return;
        }

        // If there is one, open it in the sidebar and take the main editor to its location
        try {
            AnnotationSuggestion suggestion = alState.getSuggestion().get();
            SourceDocument sourceDocument = documentService.getSourceDocument(state.getProject(),
                    suggestion.getDocumentName());
            
            // Refresh feature editor
            refreshFeatureEditor(aTarget, suggestion);
            
            if (!aStay) {
                // Open the corresponding document in the main editor and jump to the respective
                // location
                actionShowSelectedDocument((AjaxRequestTarget) aTarget, sourceDocument,
                        suggestion.getBegin(), suggestion.getEnd());
            }
            setHighlight(suggestion);
            
            if (!aStay) {
                //Main editor
                annotationPage.actionRefreshDocument((AjaxRequestTarget) aTarget);
            }
            
            // Obtain some left and right context of the active suggestion while we have easy
            // access to the document which contains the current suggestion
            CAS cas;
            if (state.getDocument().getName().equals(suggestion.getDocumentName())) {
                cas = getCasProvider().get();
            }
            else {
                cas = documentService.readAnnotationCas(sourceDocument,
                        state.getUser().getUsername());
            }
            String text = cas.getDocumentText();
            alState.setLeftContext(
                    text.substring(Math.max(0, suggestion.getBegin() - 20), suggestion.getBegin()));
            alState.setRightContext(text.substring(suggestion.getEnd(),
                    Math.min(suggestion.getEnd() + 20, text.length())));
            
            // Send an application event that the suggestion has been rejected
            List<AnnotationSuggestion> alternativeSuggestions = recommendationService
                    .getPredictions(state.getUser(), state.getProject())
                    .getPredictionsByTokenAndFeature(suggestion.getDocumentName(),
                            alState.getLayer(), suggestion.getBegin(), suggestion.getEnd(),
                            suggestion.getFeature());

            applicationEventPublisherHolder.get()
                    .publishEvent(new ActiveLearningSuggestionOfferedEvent(this, sourceDocument,
                            suggestion, state.getUser().getUsername(), alState.getLayer(),
                            suggestion.getFeature(), alternativeSuggestions));
        }
        catch (IOException e) {
            LOG.info("Error reading CAS: {}", e.getMessage());
            error("Error reading CAS " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }
    
    private Form<?> createLearningHistory()
    {
        Form<?> learningHistoryForm = new Form<Void>(CID_LEARNING_HISTORY_FORM)
        {
            private static final long serialVersionUID = -961690443085882064L;
        };
        learningHistoryForm.add(LambdaBehavior.onConfigure(component -> component
            .setVisible(alStateModel.getObject().isSessionActive())));
        learningHistoryForm.setOutputMarkupPlaceholderTag(true);
        learningHistoryForm.setOutputMarkupId(true);

        learningHistoryForm.add(createLearningHistoryListView());
        return learningHistoryForm;
    }

    private ListView<LearningRecord> createLearningHistoryListView()
    {
        ListView<LearningRecord> learningHistory = new ListView<LearningRecord>(
                CID_HISTORY_LISTVIEW)
        {
            private static final long serialVersionUID = 5594228545985423567L;

            @Override
            protected void populateItem(ListItem<LearningRecord> item)
            {
                LearningRecord rec = item.getModelObject();
                AnnotationFeature recAnnotationFeature = rec.getAnnotationFeature();
                String recFeatureValue;
                if (recAnnotationFeature != null) {
                    FeatureSupport featureSupport = featureSupportRegistry
                        .getFeatureSupport(recAnnotationFeature);
                    recFeatureValue = featureSupport
                        .renderFeatureValue(recAnnotationFeature, rec.getAnnotation());
                }
                else {
                    recFeatureValue = rec.getAnnotation();
                }
                LambdaAjaxLink textLink = new LambdaAjaxLink(CID_JUMP_TO_ANNOTATION, _target -> 
                        actionSelectHistoryItem(_target, item.getModelObject()));
                textLink.setBody(rec::getTokenText);
                item.add(textLink);

                item.add(new Label(CID_RECOMMENDED_ANNOTATION, recFeatureValue));
                item.add(new Label(CID_USER_ACTION, rec.getUserAction()));
                item.add(
                    new LambdaAjaxLink(CID_REMOVE_RECORD, t -> actionRemoveHistoryItem(t, rec)));
            }
        };
        learningRecords = LambdaModel.of(this::listLearningRecords);
        learningHistory.setModel(learningRecords);
        return learningHistory;
    }

    /**
     * Select an item from the learning history. When the user clicks on an item from the learning
     * history, the following should happen:
     * <ul>
     * <li>the main editor should jump to the location of the history item</li>
     * <li>if there is an annotation which matches the history item in terms of layer and feature
     *     value, then this annotation should be highlighted.</li>
     * <li>if there is no matching annotation, then the text should be highlighted</li>
     * </ul>
     */
    private void actionSelectHistoryItem(AjaxRequestTarget aTarget, LearningRecord aRecord)
        throws IOException
    {
        actionShowSelectedDocument(aTarget, aRecord.getSourceDocument(),
                aRecord.getOffsetCharacterBegin(), aRecord.getOffsetCharacterEnd());
        
        // Since we have switched documents above (if it was necessary), the editor CAS should
        // now point to the correct one
        CAS cas = getCasProvider().get();

        // ... if a matching annotation exists, highlight the annotaiton
        Optional<AnnotationFS> annotation = getMatchingAnnotation(cas, aRecord);
        
        if (annotation.isPresent()) {
            setHighlight(aRecord.getSourceDocument(), annotation.get());
        }
        // ... otherwise highlight the text
        else {
            setHighlight(aRecord);
            
            info("No annotation could be highlighted.");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }
    
    private Optional<AnnotationFS> getMatchingAnnotation(CAS aCas, LearningRecord aRecord)
    {
        Type type = CasUtil.getType(aCas, alStateModel.getObject().getLayer().getName());
        Feature feature = type.getFeatureByBaseName(aRecord.getAnnotationFeature().getName());
        return selectAt(aCas, type, aRecord.getOffsetCharacterBegin(),
                aRecord.getOffsetCharacterEnd()).stream()
                .filter(fs -> aRecord.getAnnotation().equals(fs.getFeatureValueAsString(feature)))
                .findFirst();
    }

    private List<AnnotationSuggestion> getMatchingSuggestion(List<SuggestionGroup> aSuggestions,
            LearningRecord aRecord)
    {
        return getMatchingSuggestion(aSuggestions, aRecord.getSourceDocument().getName(),
                aRecord.getLayer().getId(), aRecord.getAnnotationFeature().getName(),
                aRecord.getOffsetCharacterBegin(), aRecord.getOffsetCharacterEnd(),
                aRecord.getAnnotation());
    }

    private List<AnnotationSuggestion> getMatchingSuggestion(List<SuggestionGroup> aSuggestions,
            AnnotationSuggestion aSuggestion)
    {
        return getMatchingSuggestion(aSuggestions, aSuggestion.getDocumentName(),
                aSuggestion.getLayerId(), aSuggestion.getFeature(), aSuggestion.getBegin(),
                aSuggestion.getEnd(), aSuggestion.getLabel());
    }

    private List<AnnotationSuggestion> getMatchingSuggestion(List<SuggestionGroup> aSuggestions,
            String aDocumentName, long aLayerId, String aFeature, int aBegin, int aEnd,
            String aLabel)
    {
        return aSuggestions.stream()
                .filter(group -> 
                        aDocumentName.equals(group.getDocumentName()) &&
                        aLayerId == group.getLayerId() &&
                        (aFeature == null || aFeature == group.getFeature()) &&
                        (aBegin == -1 || aBegin == group.getOffset().getBegin()) &&
                        (aEnd == -1 || aEnd == group.getOffset().getEnd()))
                .flatMap(group -> group.stream())
                .filter(suggestion ->
                        aLabel == null || aLabel.equals(suggestion.getLabel()))
                .collect(toList());
    }

    private List<LearningRecord> listLearningRecords()
    {
        return learningRecordService.listRecords(getModelObject().getUser().getUsername(),
                alStateModel.getObject().getLayer(), 50);
    }

    private void actionRemoveHistoryItem(AjaxRequestTarget aTarget, LearningRecord aRecord)
        throws IOException
    {
        aTarget.add(mainContainer);
        
        ActiveLearningUserState alState = alStateModel.getObject();
        
        annotationPage.actionRefreshDocument(aTarget);
        learningRecordService.delete(aRecord);
        
        // The history records caused suggestions to disappear. Since visibility is only fully
        // recalculated when new predictions come in, we need to update the visibility explicitly
        // here
        getMatchingSuggestion(alState.getSuggestions(), aRecord)
                .forEach(suggestion -> suggestion.show(FLAG_SKIPPED | FLAG_REJECTED));
        
        // Force the learning records model to be refreshed during rendering, showing the latest
        // state from the DB
        learningRecords.detach();

        if (asList(ACCEPTED, CORRECTED).contains(aRecord.getUserAction())) {
            // IMPORTANT: we must jump to the document which contains the annotation that is to
            // be deleted because deleteAnnotationByHistory will delete the annotation via the
            // methods provided by the AnnotationActionHandler and these operate ONLY on the
            // currently visible/selected document.
            CAS cas = documentService.readAnnotationCas(aRecord.getSourceDocument(),
                    aRecord.getUser());
            if (getMatchingAnnotation(cas, aRecord).isPresent()) {
                actionShowSelectedDocument(aTarget, aRecord.getSourceDocument(),
                        aRecord.getOffsetCharacterBegin(), aRecord.getOffsetCharacterEnd());
                confirmationDialog.setTitleModel(new StringResourceModel(
                        "alSidebar.history.delete.confirmation.title", this));
                confirmationDialog.setContentModel(new StringResourceModel(
                        "alSidebar.history.delete.confirmation.content", this, null));
                confirmationDialog.show(aTarget);
                confirmationDialog.setConfirmAction(t -> deleteAnnotationByHistory(t, aRecord));
            }
        }
        
        // If there is currently no suggestion (i.e. we ran out of suggestions before) there is a
        // good chance that deleting the history item makes suggestions become available again, so
        // we try to find a new one.
        if (!alState.getSuggestion().isPresent()) {
            refreshSuggestions();
            moveToNextRecommendation(aTarget, false);
        }
    }

    private void deleteAnnotationByHistory(AjaxRequestTarget aTarget, LearningRecord aRecord)
        throws IOException, AnnotationException
    {
        AnnotatorState state = getModelObject();
        
        CAS cas = this.getCasProvider().get();
        Optional<AnnotationFS> anno = getMatchingAnnotation(cas, aRecord);
        if (anno.isPresent()) {
            state.getSelection().selectSpan(new VID(anno.get()), cas,
                    aRecord.getOffsetCharacterBegin(), aRecord.getOffsetCharacterEnd());
            getActionHandler().actionDelete(aTarget);
        }
    }

    /**
     * Listens to the user setting a feature on an annotation in the main annotation editor. Mind
     * that we do not need to listen to the creation of annotations since they have no effect on 
     * the active learning sidebar as long as they have no features set.
     */
    @OnEvent
    public void onFeatureValueUpdated(FeatureValueUpdatedEvent aEvent)
    {
        AnnotatorState state = getModelObject();
        ActiveLearningUserState alState = alStateModel.getObject();

        // If the user creates a new annotation at the site of the suggestion that is currently
        // offered to the user, then the AL should move on to the next available suggestion
        if (
                alState.isSessionActive() &&
                (!alState.getSuggestion().isPresent() || (
                aEvent.getUser().equals(state.getUser().getUsername()) &&
                aEvent.getDocument().equals(state.getDocument()) &&
                aEvent.getFeature().getLayer().equals(alState.getLayer()) &&
                aEvent.getFeature().getName().equals(alState.getSuggestion().get().getFeature())))
        ) {
            reactToAnnotationsBeingCreatedOrDeleted(aEvent.getRequestTarget(),
                    aEvent.getFeature().getLayer());
        }
    }
    
    /**
     * Listens to the user deleting an annotation in the main annotation editor.
     */
    @OnEvent
    public void onAnnotationDeleted(SpanDeletedEvent aEvent)
    {
        AnnotatorState state = getModelObject();
        ActiveLearningUserState alState = alStateModel.getObject();

        // If the user creates a new annotation at the site of the suggestion that is currently
        // offered to the user, then the AL should move on to the next available suggestion
        if (
                alState.isSessionActive() &&
                (!alState.getSuggestion().isPresent() || (
                aEvent.getUser().equals(state.getUser().getUsername()) &&
                aEvent.getDocument().equals(state.getDocument()) &&
                aEvent.getLayer().equals(alState.getLayer())))
        ) {
            reactToAnnotationsBeingCreatedOrDeleted(aEvent.getRequestTarget(), aEvent.getLayer());
        }
    }
    
    private void reactToAnnotationsBeingCreatedOrDeleted(IPartialPageRequestHandler aTarget,
            AnnotationLayer aLayer)
    {
        try {
            AnnotatorState state = getModelObject();
            ActiveLearningUserState alState = alStateModel.getObject();
            
//            // Make sure we know about the current suggestions and their visibility state
//            refreshSuggestions();
    
            // Update visibility in case the annotation where the feature was set overlaps with 
            // any suggestions that need to be hidden now.
            recommendationService.calculateVisibility(getAnnotationPage().getEditorCas(),
                    state.getUser().getUsername(), aLayer, alState.getSuggestions(),
                    state.getWindowBeginOffset(), state.getWindowEndOffset());
    
            // Update the suggestion in the AL sidebar, but do not jump or touch the right
            // sidebar such that the user can happily continue to edit the annotation
            moveToNextRecommendation(aTarget, true);
        }
        catch (IOException e) {
            LOG.info("Error reading CAS: {}", e.getMessage());
            error("Error reading CAS " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    
    @OnEvent
    public void onRecommendationRejectEvent(AjaxRecommendationRejectedEvent aEvent)
    {
        AnnotatorState annotatorState = getModelObject();
        AnnotatorState eventState = aEvent.getAnnotatorState();

        Predictions predictions = recommendationService.getPredictions(annotatorState.getUser(),
                annotatorState.getProject());

        if (
                alStateModel.getObject().isSessionActive() && 
                eventState.getUser().equals(annotatorState.getUser()) && 
                eventState.getProject().equals(annotatorState.getProject())
        ) {
            SourceDocument doc = eventState.getDocument();
            VID vid = aEvent.getVid();
            Optional<AnnotationSuggestion> prediction = predictions.getPredictionByVID(doc, vid);

            if (!prediction.isPresent()) {
                LOG.error("Could not find prediction in [{}] with id [{}]", doc, vid);
                error("Could not find prediction");
                return;
            }

            AnnotationSuggestion rejectedRecommendation = prediction.get();
            applicationEventPublisherHolder.get().publishEvent(
                new ActiveLearningRecommendationEvent(this, eventState.getDocument(),
                    rejectedRecommendation, annotatorState.getUser().getUsername(),
                    eventState.getSelectedAnnotationLayer(), rejectedRecommendation.getFeature(),
                    REJECTED, predictions.getPredictionsByTokenAndFeature(
                    rejectedRecommendation.getDocumentName(),
                    eventState.getSelectedAnnotationLayer(),
                    rejectedRecommendation.getBegin(),
                    rejectedRecommendation.getEnd(),
                    rejectedRecommendation.getFeature())));

            if (
                    doc.equals(annotatorState.getDocument()) &&
                    vid.getLayerId() == alStateModel.getObject().getLayer().getId() && 
                    prediction.get().equals(
                            alStateModel.getObject().getSuggestion().orElse(null))
            ) {
                moveToNextRecommendation(aEvent.getTarget(), false);
            }
            aEvent.getTarget().add(mainContainer);
        }
    }

    /**
     * Listens to the user accepting a recommendation in the main annotation editor.
     */
    @OnEvent
    public void onRecommendationAcceptEvent(AjaxRecommendationAcceptedEvent aEvent)
    {
        AnnotatorState state = getModelObject();
        Predictions predictions = recommendationService.getPredictions(state.getUser(),
                state.getProject());
        AnnotatorState eventState = aEvent.getAnnotatorState();
        SourceDocument doc = state.getDocument();
        VID vid = aEvent.getVid();
        
        Optional<AnnotationSuggestion> oRecommendation = predictions.getPredictionByVID(doc, vid);
        if (!oRecommendation.isPresent()) {
            LOG.error("Could not find prediction in [{}] with id [{}]", doc, vid);
            error("Could not find prediction");
            aEvent.getTarget().addChildren(getPage(), IFeedback.class);
            return;
        }

        AnnotationSuggestion acceptedSuggestion = oRecommendation.get();
        
        applicationEventPublisherHolder.get().publishEvent(
            new ActiveLearningRecommendationEvent(this, eventState.getDocument(),
                acceptedSuggestion, state.getUser().getUsername(),
                eventState.getSelectedAnnotationLayer(), acceptedSuggestion.getFeature(),
                ACCEPTED, predictions.getPredictionsByTokenAndFeature(
                acceptedSuggestion.getDocumentName(),
                eventState.getSelectedAnnotationLayer(),
                acceptedSuggestion.getBegin(),
                acceptedSuggestion.getEnd(),
                acceptedSuggestion.getFeature())));

        // If the annotation that the user accepted is the one that is currently displayed in
        // the annotation sidebar, then we have to go and pick a new one
        ActiveLearningUserState alState = alStateModel.getObject();
        if (
                alState.isSessionActive() && 
                alState.getSuggestion().isPresent() && 
                eventState.getUser().equals(state.getUser()) && 
                eventState.getProject().equals(state.getProject())
        ) {
            AnnotationSuggestion suggestion = alState.getSuggestion().get();
            if (
                    acceptedSuggestion.getOffset().equals(suggestion.getOffset()) && 
                    vid.getLayerId() == suggestion.getLayerId() && 
                    acceptedSuggestion.getFeature().equals(suggestion.getFeature())
            ) {
                moveToNextRecommendation(aEvent.getTarget(), false);
            }
            aEvent.getTarget().add(mainContainer);
        }
    }

    @OnEvent
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        renderHighlights(aEvent.getVDocument());
    }
    
    private void renderHighlights(VDocument aVDoc)
    {
        LOG.trace("Active learning sidebar rendering highlights");

        // Clear any highlights that we may have added earlier in the rendering process and
        // recreate them because the VIDs may have changed
        aVDoc.getMarkers().removeIf(marker -> marker.getSource() == this);

        if (highlightDocumentName == null) {
            LOG.trace("Active learning sidebar has no highlights to render");
            return;
        }

        String currentDoc = getModelObject().getDocument().getName();
        if (!currentDoc.equals(highlightDocumentName)) {
            LOG.trace("Active learning sidebar highlights are in document [{}], not in [{}]",
                    highlightDocumentName, currentDoc);
            return;
        }
         
        if (highlightVID != null) {
            aVDoc.add(new VAnnotationMarker(this, VMarker.FOCUS, highlightVID));
        }
        else {
            LOG.trace(
                    "Active learning sidebar annotation highlight is not set");
        }

        if (highlightSpan != null) {
            AnnotatorState state = getModelObject();
            if (state.getWindowBeginOffset() <= highlightSpan.getBegin()
                    && highlightSpan.getEnd() <= state.getWindowEndOffset()) {
                aVDoc.add(new VTextMarker(this, VMarker.FOCUS,
                        highlightSpan.getBegin() - state.getWindowBeginOffset(),
                        highlightSpan.getEnd() - state.getWindowBeginOffset()));
            }
            else {
                LOG.trace("Active learning sidebar span highlight is outside visible area");
            }
        }
        else {
            LOG.trace("Active learning sidebar span highlight is not set");
        }
    }

    private void refreshSuggestions()
    {
        AnnotatorState state = getModelObject();
        ActiveLearningUserState alState = alStateModel.getObject();
        alState.setSuggestions(
                activeLearningService.getSuggestions(state.getUser(), alState.getLayer()));
    }
    
    @OnEvent
    public void onDocumentReset(AfterDocumentResetEvent aEvent)
    {
        reactToChangeInPredictions(aEvent.getRequestTarget());
    }
    
    @OnEvent
    public void onPredictionsSwitched(PredictionsSwitchedEvent aEvent)
    {
        reactToChangeInPredictions(aEvent.getRequestHandler());
        // As a reaction to the change in predictions, the highlights may have to be placed at 
        // a different location. This the prediction switch is announced late in the rendering
        // process and the highlights have already been added to the VDocument at this time, 
        // we need to remove and re-add them. renderHighlights() takes care of this.
        renderHighlights(aEvent.getVDocument());
    }
    
    private void reactToChangeInPredictions(IPartialPageRequestHandler aTarget)
    {
        ActiveLearningUserState alState = alStateModel.getObject();

        // If active learning is not active, nothing to do here
        if (!alState.isSessionActive()) {
            // Re-render the AL sidebar in case the session auto-terminated
            aTarget.add(mainContainer);
            return;
        }

        // Make sure we know about the current suggestions and their visibility state
        refreshSuggestions();
        
        // If there is currently a suggestion displayed in the sidebar, we need to check if it
        // is still relevant - if yes, we need to replace it with its current counterpart since.
        // if no counterpart exists in the current suggestions, then we need to load a 
        // suggestion from the current list.
        if (alState.getSuggestion().isPresent()) {
            AnnotationSuggestion activeSuggestion = alState.getSuggestion().get();
            // Find the groups which matches the active recommendation
            Optional<AnnotationSuggestion> updatedSuggestion = getMatchingSuggestion(
                    alState.getSuggestions(), activeSuggestion).stream().findFirst();
            
            if (updatedSuggestion.isPresent()) {
                LOG.debug("Replacing outdated suggestion {} with new suggestion {}",
                        alState.getCurrentDifference().get().getFirst().getId(),
                        updatedSuggestion.get().getId());

                // Update the highlight
                if (alState.getSuggestion().get().getVID().equals(highlightVID)) {
                    highlightVID = updatedSuggestion.get().getVID();
                }

                // We found a matching suggestion, but we look for its second-best. So for the 
                // moment we assume that the second-best has not changed and we simply fake
                // a delta
                Delta fakeDelta = new Delta(updatedSuggestion.get(),
                        alState.getCurrentDifference().get().getSecond().orElse(null));
                alState.setCurrentDifference(Optional.of(fakeDelta));
            }
            else {
                moveToNextRecommendation(aTarget, true);
            }
        }
        else {
            // Still need to re-render the AL sidebar so we can show stuff to the user like asking
            // whether to review skipped items or just informing that there is nothing more to do.
            aTarget.add(mainContainer);
        }
    }
}
