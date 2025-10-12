/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.active.learning.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.active.learning.sidebar.ActiveLearningUserStateMetaData.CURRENT_AL_USER_STATE;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.KEY_RECOMMENDER_GENERAL_SETTINGS;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.CORRECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.REJECTED;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.uima.cas.CAS;
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
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningService;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningServiceImpl;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningServiceImpl.ActiveLearningUserState;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningRecommendationEvent;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSessionCompletedEvent;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSessionStartedEvent;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSuggestionOfferedEvent;
import de.tudarmstadt.ukp.inception.active.learning.strategy.UncertaintySamplingStrategy;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.event.RelationCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.event.SpanCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.event.SpanDeletedEvent;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.event.AjaxRecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.event.PredictionsSwitchedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup.Delta;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VAnnotationMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VTextMarker;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

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
    private static final String CID_RECOMMENDED_SCORE = "recommendedScore";
    private static final String CID_RECOMMENDED_PREDITION = "recommendedPredition";
    private static final String CID_RECOMMENDATION_FORM = "recommendationForm";
    private static final String CID_LEARN_SKIPPED_ONES = "learnSkippedOnes";
    private static final String CID_ONLY_SKIPPED_RECOMMENDATION_LABEL = "onlySkippedRecommendationLabel";
    private static final String CID_LEARN_FROM_SKIPPED_RECOMMENDATION_FORM = "learnFromSkippedRecommendationForm";
    private static final String CID_NO_RECOMMENDATION_LABEL = "noRecommendationLabel";
    private static final String CID_SESSION_CONTROL_FORM = "sessionControlForm";
    private static final String CID_START_SESSION_BUTTON = "startSession";
    private static final String CID_STOP_SESSION_BUTTON = "stopSession";
    private static final String CID_SELECT_LAYER = "selectLayer";
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
    private @SpringBean UserDao userService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean PreferencesService preferencesService;

    private IModel<Boolean> recommendersAvailable;
    private IModel<List<LearningRecord>> learningRecords;
    private CompoundPropertyModel<ActiveLearningServiceImpl.ActiveLearningUserState> alStateModel;

    private final WebMarkupContainer alMainContainer;

    private AnnotationPageBase2 annotationPage;
    private BootstrapModalDialog dialog;
    private FeatureEditor editor;
    private Form<Void> recommendationForm;
    private Form<Void> sessionControlForm;

    private AnnotationSuggestion highlightSuggestion;
    private Long highlightDocumentId;
    private VID highlightVID;
    private Offset highlightSpan;
    private boolean protectHighlight;

    enum FeatureDetailEditorUpdateFlag
    {
        KEEP_SELECTED_ANNOTATION_AND_VIEW, CLEAR_SELECTED_ANNOTATION_AND_JUMP_TO_SUGGESTION
    }

    public ActiveLearningSidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        annotationPage = aAnnotationPage;

        // Instead of maintaining the AL state in the sidebar, we maintain it in the page because
        // that way we persists even if we switch to another sidebar tab
        alStateModel = new CompoundPropertyModel<>(
                LambdaModelAdapter.of(() -> aAnnotationPage.getMetaData(CURRENT_AL_USER_STATE),
                        alState -> aAnnotationPage.setMetaData(CURRENT_AL_USER_STATE, alState)));

        // Set up the AL state in the page if it is not already there or if for some reason the
        // suggestions have completely disappeared (e.g. after a system restart)
        var state = getModelObject();
        var predictions = state.getProject() != null
                ? recommendationService.getPredictions(state.getUser(), state.getProject())
                : null;
        if (aAnnotationPage.getMetaData(CURRENT_AL_USER_STATE) == null || predictions == null) {
            var alState = new ActiveLearningUserState();
            alState.setStrategy(new UncertaintySamplingStrategy());
            alStateModel.setObject(alState);
        }

        recommendersAvailable = LoadableDetachableModel.of(this::isRecommendersAvailable);

        var notAvailableNotice = new WebMarkupContainer("notAvailableNotice");
        notAvailableNotice.add(visibleWhenNot(recommendersAvailable));
        add(notAvailableNotice);

        sessionControlForm = createSessionControlForm();
        sessionControlForm.add(visibleWhen(() -> alStateModel.getObject().isDoExistRecommenders()
                && recommendersAvailable.getObject()));
        add(sessionControlForm);

        alMainContainer = new WebMarkupContainer(CID_MAIN_CONTAINER);
        alMainContainer.setOutputMarkupId(true);
        alMainContainer.add(createNoRecommendersMessage());
        alMainContainer.add(createNoRecommendationLabel());
        alMainContainer.add(clearSkippedRecommendationForm());
        alMainContainer.add(createRecommendationOperationForm());
        alMainContainer.add(createLearningHistory());
        alMainContainer.add(visibleWhen(recommendersAvailable));
        add(alMainContainer);

        dialog = new BootstrapModalDialog(CID_CONFIRMATION_DIALOG);
        dialog.trapFocus();
        add(dialog);
    }

    @Override
    protected void onDetach()
    {
        super.onDetach();
        recommendersAvailable.detach();
    }

    private boolean isRecommendersAvailable()
    {
        var state = getModelObject();
        var prefs = preferencesService.loadDefaultTraitsForProject(KEY_RECOMMENDER_GENERAL_SETTINGS,
                state.getProject());

        // Do not show predictions when viewing annotations of another user
        if (!prefs.isShowRecommendationsWhenViewingOtherUser()
                && !Objects.equals(state.getUser(), userService.getCurrentUser())) {
            return false;
        }

        // Do not show predictions when viewing annotations of curation user
        if (!prefs.isShowRecommendationsWhenViewingCurationUser()
                && Objects.equals(state.getUser(), userService.getCurationUser())) {
            return false;
        }

        return true;
    }

    private Label createNoRecommendersMessage()
    {
        if (!alStateModel.getObject().isSessionActive()) {
            // Use the currently selected layer from the annotation detail editor panel as the
            // default choice in the active learning mode.
            var layersWithRecommenders = listLayersWithRecommenders();
            if (layersWithRecommenders.contains(getModelObject().getDefaultAnnotationLayer())) {
                alStateModel.getObject().setLayer(getModelObject().getDefaultAnnotationLayer());
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

        var noRecommendersMessage = new Label(CID_NO_RECOMMENDERS, "None of the layers have any "
                + "recommenders configured. Please set the recommenders first in the Project "
                + "Settings.");
        noRecommendersMessage
                .add(visibleWhen(() -> !alStateModel.getObject().isDoExistRecommenders()));
        return noRecommendersMessage;
    }

    private Form<Void> createSessionControlForm()
    {
        var form = new Form<Void>(CID_SESSION_CONTROL_FORM);

        var layersDropdown = new DropDownChoice<AnnotationLayer>(CID_SELECT_LAYER);
        layersDropdown.setModel(alStateModel.bind("layer"));
        layersDropdown.setChoices(LoadableDetachableModel.of(this::listLayersWithRecommenders));
        layersDropdown.setChoiceRenderer(new LambdaChoiceRenderer<>(AnnotationLayer::getUiName));
        layersDropdown.add(LambdaBehavior
                .onConfigure(it -> it.setEnabled(!alStateModel.getObject().isSessionActive())));
        layersDropdown.setOutputMarkupId(true);
        layersDropdown.setRequired(true);
        form.add(layersDropdown);

        form.add(new LambdaAjaxSubmitLink<>(CID_START_SESSION_BUTTON, this::actionStartSession)
                .add(visibleWhen(() -> !alStateModel.getObject().isSessionActive())));
        form.add(new LambdaAjaxLink(CID_STOP_SESSION_BUTTON, this::actionStopSession)
                .add(visibleWhen(() -> alStateModel.getObject().isSessionActive())));

        return form;
    }

    private List<AnnotationLayer> listLayersWithRecommenders()
    {
        // We right now only support active learning with span recommenders.
        return recommendationService.listLayersWithEnabledRecommenders(getModelObject() //
                .getProject()) //
                .stream() //
                .filter(layer -> layer.getType().equals(SpanLayerSupport.TYPE)) //
                .collect(toList());
    }

    private void actionStartSession(AjaxRequestTarget aTarget, Form<?> form)
    {
        var alState = alStateModel.getObject();
        var state = getModelObject();
        var userName = state.getUser().getUsername();
        var project = state.getProject();

        recommendationService.setPredictForAllDocuments(userName, project, true);
        recommendationService.triggerPrediction(userName, "ActionStartActiveLearningSession",
                state.getDocument(), state.getUser().getUsername());

        // Start new session
        alState.setSessionActive(true);

        aTarget.add(sessionControlForm);

        refreshAvailableSuggestions();

        requestClearningSelectionAndJumpingToSuggestion();
        moveToNextSuggestion(aTarget);

        applicationEventPublisherHolder.get()
                .publishEvent(new ActiveLearningSessionStartedEvent(this, project, userName));
    }

    /**
     * Called by action handles from the active learning sidebar to indicate that during later
     * processing, the selection should automatically move to the next active learning suggestion.
     * If an action is performed via the main editor, this should not be called so the annotation
     * that was explicitly created and selected remains editable by the user.
     */
    private void requestClearningSelectionAndJumpingToSuggestion()
    {
        LOG.trace("Requesting clearing and jumping");
        RequestCycle.get().setMetaData(ClearSelectionAndJumpToSuggestionKey.INSTANCE, true);
    }

    private boolean shouldBeClearningSelectionAndJumpingToSuggestion()
    {
        Boolean flag = RequestCycle.get()
                .getMetaData(ClearSelectionAndJumpToSuggestionKey.INSTANCE);
        return flag != null ? flag : false;
    }

    private void actionStopSession(AjaxRequestTarget aTarget)
    {
        ActiveLearningUserState alState = alStateModel.getObject();
        AnnotatorState state = getModelObject();

        clearActiveLearningHighlight();

        // Stop current session
        alState.setSessionActive(false);

        String userName = state.getUser().getUsername();
        Project project = state.getProject();
        recommendationService.setPredictForAllDocuments(userName, project, false);
        applicationEventPublisherHolder.get()
                .publishEvent(new ActiveLearningSessionCompletedEvent(this, project, userName));

        aTarget.add(alMainContainer, sessionControlForm);
        annotationPage.actionRefreshDocument(aTarget);
    }

    private void setActiveLearningHighlight(SpanSuggestion aSuggestion)
    {
        assert aSuggestion.isVisible() : "Cannot highlight hidden suggestions";

        if (protectHighlight) {
            LOG.trace("Active learning sidebar not updating protected highlights");
            protectHighlight = false;
            return;
        }

        LOG.trace("Active learning sidebar set highlight suggestion: {}", aSuggestion);
        highlightSuggestion = aSuggestion;
        highlightVID = aSuggestion.getVID();
        highlightSpan = new Offset(aSuggestion.getBegin(), aSuggestion.getEnd());
        highlightDocumentId = aSuggestion.getDocumentId();
    }

    private void setActiveLearningHighlight(LearningRecord aRecord)
    {
        LOG.trace("Active learning sidebar set highlight history record: {}", aRecord);
        highlightSuggestion = null;
        highlightVID = null;
        highlightSpan = new Offset(aRecord.getOffsetBegin(), aRecord.getOffsetEnd());
        highlightDocumentId = aRecord.getSourceDocument().getId();
        // This is a bit of hack. Consider the following case:
        // - use removes an ACCEPT history item
        // - user clicks then on another history item
        // - ... but during the subsequent rendering the "moveToNextSuggestion" method sets or
        // clears the highlight.
        protectHighlight = true;
    }

    private void setActiveLearningHighlight(SourceDocument aDocument, AnnotationFS aAnnotation)
    {
        LOG.trace("Active learning sidebar set highlight annotation: {}", aAnnotation);
        highlightSuggestion = null;
        highlightVID = new VID(ICasUtil.getAddr(aAnnotation));
        highlightSpan = new Offset(aAnnotation.getBegin(), aAnnotation.getEnd());
        highlightDocumentId = aDocument.getId();
        protectHighlight = false;
    }

    private void clearActiveLearningHighlight()
    {
        if (protectHighlight) {
            LOG.trace("Active learning sidebar not clearing protected highlights");
            protectHighlight = false;
            return;
        }

        LOG.trace("Active learning sidebar cleared highlights");
        highlightDocumentId = null;
        highlightSpan = null;
        highlightVID = null;
    }

    private Label createNoRecommendationLabel()
    {
        Label noRecommendation = new Label(CID_NO_RECOMMENDATION_LABEL,
                "There are no further suggestions.");
        noRecommendation.add(visibleWhen(alStateModel
                .map(alState -> alState.isSessionActive() && !alState.getSuggestion().isPresent()
                        && !activeLearningService.hasSkippedSuggestions(
                                userService.getCurrentUsername(), getModelObject().getUser(),
                                alState.getLayer()))));
        noRecommendation.setOutputMarkupPlaceholderTag(true);
        return noRecommendation;
    }

    private Form<Void> clearSkippedRecommendationForm()
    {
        var form = new Form<Void>(CID_LEARN_FROM_SKIPPED_RECOMMENDATION_FORM);
        form.add(visibleWhen(alStateModel
                .map(alState -> alState.isSessionActive() && !alState.getSuggestion().isPresent()
                        && activeLearningService.hasSkippedSuggestions(
                                userService.getCurrentUsername(), getModelObject().getUser(),
                                alState.getLayer()))));
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
        learningRecordService.deleteSkippedSuggestions(userService.getCurrentUsername(),
                getModelObject().getUser(), alStateModel.getObject().getLayer());

        // The history records caused suggestions to disappear. Since visibility is only fully
        // recalculated when new predictions come in, we need to update the visibility explicitly
        // here
        alStateModel.getObject().getSuggestions().stream() //
                .flatMap(group -> group.stream())
                .forEach(suggestion -> suggestion.show(FLAG_SKIPPED));

        refreshAvailableSuggestions();
        requestClearningSelectionAndJumpingToSuggestion();
        moveToNextSuggestion(aTarget);
    }

    private Form<Void> createRecommendationOperationForm()
    {
        recommendationForm = new Form<Void>(CID_RECOMMENDATION_FORM);
        recommendationForm.add(visibleWhen(() -> {
            var alState = alStateModel.getObject();
            return alState.isSessionActive() && alState.getSuggestion().isPresent();
        }));
        recommendationForm.setOutputMarkupPlaceholderTag(true);

        recommendationForm.add(createJumpToSuggestionLink());
        recommendationForm.add(
                new Label(CID_RECOMMENDED_PREDITION, LoadableDetachableModel.of(() -> alStateModel
                        .getObject().getSuggestion().map(this::formatLabel).orElse(null))));
        recommendationForm.add(new Label(CID_RECOMMENDED_SCORE, () -> alStateModel.getObject()
                .getSuggestion().map(SpanSuggestion::getScore).orElse(null)));
        recommendationForm.add(new Label(CID_RECOMMENDED_DIFFERENCE, () -> alStateModel.getObject()
                .getCurrentDifference().map(Delta::getDelta).orElse(null)));
        recommendationForm.add((alStateModel.getObject().getLayer() != null
                && alStateModel.getObject().getSuggestion().isPresent()) ? initializeFeatureEditor()
                        : new Label(CID_EDITOR).setVisible(false));

        recommendationForm.add(new LambdaAjaxButton<>(CID_ANNOTATE_BUTTON, this::actionAnnotate));
        recommendationForm.add(new LambdaAjaxLink(CID_SKIP_BUTTON, this::actionSkip));
        recommendationForm.add(new LambdaAjaxLink(CID_REJECT_BUTTON, this::actionReject));

        return recommendationForm;
    }

    private String formatLabel(AnnotationSuggestion aCurrentRecommendation)
    {
        AnnotationFeature feat = annotationService.getFeature(aCurrentRecommendation.getFeature(),
                alStateModel.getObject().getLayer());
        FeatureSupport<?> featureSupport = featureSupportRegistry.findExtension(feat).orElseThrow();
        String labelValue = featureSupport.renderFeatureValue(feat,
                aCurrentRecommendation.getLabel());
        return labelValue;
    }

    private LambdaAjaxLink createJumpToSuggestionLink()
    {
        LambdaAjaxLink link = new LambdaAjaxLink(CID_RECOMMENDATION_COVERED_TEXT_LINK,
                this::actionJumpToSuggestion);
        link.add(new Label("leftContext",
                alStateModel.map(ActiveLearningUserState::getLeftContext)));
        link.add(new Label("text", LoadableDetachableModel.of(() -> alStateModel.getObject()
                .getSuggestion().map(SpanSuggestion::getCoveredText).orElse(""))));
        link.add(new Label("rightContext",
                alStateModel.map(ActiveLearningUserState::getRightContext)));
        return link;
    }

    private void actionJumpToSuggestion(AjaxRequestTarget aTarget) throws IOException
    {
        var alState = alStateModel.getObject();
        var suggestion = alState.getSuggestion().get();

        if (!suggestion.isVisible()) {
            error("Cannot jump to hidden suggestion");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Active suggestion: {}", suggestion);
            var updatedSuggestion = getMatchingSuggestion(activeLearningService
                    .getSuggestions(getModelObject().getUser(), alState.getLayer()), suggestion)
                            .stream().findFirst();
            updatedSuggestion.ifPresent(s -> LOG.debug("Update suggestion: {}", s));
        }

        protectHighlight = false;
        setActiveLearningHighlight(suggestion);

        // REC: Potential bug: jumping causes the document to re-renderer and therefore the
        // predictions to switch. If the suggestion is no longer visible in the switched predictions
        // (e.g. because it is no longer predicted), then we jump to nothing?
        getAnnotationPage().actionShowSelectedDocument(aTarget,
                documentService.getSourceDocument(this.getModelObject().getProject().getId(),
                        suggestion.getDocumentId()),
                suggestion.getBegin(), suggestion.getEnd());
    }

    private Component initializeFeatureEditor()
    {
        editor = createFeatureEditor(alStateModel.getObject().getSuggestion().get());
        return editor;
    }

    private void refreshActiveLearningFeatureEditor(IPartialPageRequestHandler aTarget,
            SpanSuggestion aCurrentRecommendation)
    {
        editor = createFeatureEditor(aCurrentRecommendation);
        recommendationForm.addOrReplace(editor);
        aTarget.add(alMainContainer);
    }

    private FeatureEditor createFeatureEditor(SpanSuggestion aSuggestion)
    {
        var alState = alStateModel.getObject();

        // Obtain the feature state which serves as a model to the editor
        var feat = annotationService.getFeature(aSuggestion.getFeature(), alState.getLayer());
        FeatureSupport<?> featureSupport = featureSupportRegistry.findExtension(feat).orElseThrow();

        // We get away with passing "null" here instead of the CAS because we currently have no
        // recommenders for any feature types that actually need the CAS (i.e. link feature types
        // and the likes).
        var wrappedFeatureValue = featureSupport.wrapFeatureValue(feat, null,
                aSuggestion.getLabel());
        var featureState = new FeatureState(aSuggestion.getVID(), feat,
                (Serializable) wrappedFeatureValue);

        // Populate the tagset moving the tags with recommended labels to the top
        var tagList = annotationService.listTagsReorderable(feat.getTagset());
        featureState.tagset = orderTagList(aSuggestion, tagList);

        // Finally, create the editor
        var featureEditor = featureSupport.createEditor(CID_EDITOR, alMainContainer,
                this.getActionHandler(), this.getModel(), Model.of(featureState));
        featureEditor.setOutputMarkupPlaceholderTag(true);
        featureEditor.add(visibleWhen(() -> alStateModel.getObject().getLayer() != null
                && alState.getSuggestion().isPresent()));

        // We do not want key bindings in the active learning sidebar
        featureEditor.visitChildren(KeyBindingsPanel.class,
                (c, v) -> c.setVisibilityAllowed(false));

        return featureEditor;
    }

    private List<ReorderableTag> orderTagList(SpanSuggestion aSuggestion,
            List<ReorderableTag> tagList)
    {
        var state = ActiveLearningSidebar.this.getModelObject();
        var reorderedTagList = new ArrayList<ReorderableTag>();

        if (tagList.size() > 0) {
            var predictions = recommendationService.getPredictions(state.getUser(),
                    state.getProject());
            // get all the predictions
            var alternativeSuggestions = predictions.getAlternativeSuggestions(aSuggestion);

            // Get all the label of the predictions (e.g. "NN").
            var allLabels = new LinkedHashMap<String, SpanSuggestion>();
            alternativeSuggestions.stream() //
                    .filter(AnnotationSuggestion::isVisible) //
                    // We filter for recommendations from the same recommender as the current
                    // suggestion to assess comes from because scores may not be comparable
                    // across recommenders
                    .filter(rec -> rec.getRecommenderId() == aSuggestion.getRecommenderId())
                    .forEachOrdered(rec -> {
                        if (Objects.equals(rec.getLabel(), aSuggestion.getLabel())) {
                            allLabels.put(rec.getLabel(), aSuggestion);
                        }
                        else {
                            var existingSuggestion = allLabels.get(rec.getLabel());
                            if (existingSuggestion == null
                                    || rec.getScore() > existingSuggestion.getScore()) {
                                allLabels.put(rec.getLabel(), rec);
                            }
                        }
                    });

            for (var tag : tagList) {
                // add the tags which contain the prediction-labels to the beginning of a tagset
                var suggestion = allLabels.get(tag.getName());
                if (suggestion != null) {
                    tag.setReordered(true);
                    tag.setScore(format("%.3f", suggestion.getScore()));
                    reorderedTagList.add(tag);
                }
            }

            // remove these tags containing the prediction-labels
            tagList.removeAll(reorderedTagList);

            // add the rest tags to the tagset after these
            reorderedTagList.addAll(tagList);
        }

        return reorderedTagList;
    }

    /**
     * Accept a suggestion or a corrected suggestion via the sidebar. If the value in the feature
     * editor corresponds to the originally suggested label, an acceptance is logged, otherwise a
     * correction is logged.
     * 
     * <ul>
     * <li>Creates a new annotation or updates an existing one with a new feature value.</li>
     * <li>Marks the suggestions as hidden (not visible).</li>
     * <li>Logs the accepting to the learning log.</li>
     * <li>Sends events to the UI and application informing other components about the action.</li>
     * </ul>
     */
    private void actionAnnotate(AjaxRequestTarget aTarget, Form<Void> aForm)
        throws IOException, AnnotationException
    {
        LOG.trace("actionAnnotate()");

        getAnnotationPage().ensureIsEditable();

        var state = getModelObject();
        var alState = alStateModel.getObject();

        // There is always a current recommendation when we get here because if there is none, the
        // button to accept the recommendation is not visible.
        var suggestion = alState.getSuggestion().get();

        // Request clearing selection and when onFeatureValueUpdated is triggered as a callback
        // from the update event created by acceptSuggestion/upsertSpanFeature.
        requestClearningSelectionAndJumpingToSuggestion();

        var project = state.getProject();
        var dataOwner = state.getUser();
        var document = documentService.getSourceDocument(state.getProject().getId(),
                suggestion.getDocumentId());
        var predictions = recommendationService.getPredictions(dataOwner, project);
        activeLearningService.acceptSpanSuggestion(document, state.getUser(), predictions,
                suggestion, editor.getModelObject().value);

        // If the currently displayed document is the same one where the annotation was created,
        // then update timestamp in state to avoid concurrent modification errors
        if (Objects.equals(state.getDocument().getId(), document.getId())) {
            documentService
                    .getAnnotationCasTimestamp(document, AnnotationSet.forUser(state.getUser()))
                    .ifPresent(state::setAnnotationDocumentTimestamp);
        }
    }

    private void actionSkip(AjaxRequestTarget aTarget) throws AnnotationException

    {
        LOG.trace("actionSkip()");

        getAnnotationPage().ensureIsEditable();

        var sessionOwner = userService.getCurrentUsername();

        var maybeSuggestion = alStateModel.getObject().getSuggestion();
        if (!maybeSuggestion.isPresent()) {
            return;
        }

        requestClearningSelectionAndJumpingToSuggestion();
        activeLearningService.skipSpanSuggestion(sessionOwner, getModelObject().getUser(),
                alStateModel.getObject().getLayer(), maybeSuggestion.get());
        moveToNextSuggestion(aTarget);
    }

    private void actionReject(AjaxRequestTarget aTarget) throws AnnotationException
    {
        LOG.trace("actionReject()");

        getAnnotationPage().ensureIsEditable();

        var maybeSuggestion = alStateModel.getObject().getSuggestion();
        if (!maybeSuggestion.isPresent()) {
            return;
        }

        requestClearningSelectionAndJumpingToSuggestion();
        activeLearningService.rejectSpanSuggestion(userService.getCurrentUsername(),
                getModelObject().getUser(), alStateModel.getObject().getLayer(),
                maybeSuggestion.get());
        moveToNextSuggestion(aTarget);
    }

    private void moveToNextSuggestion(AjaxRequestTarget aTarget)
    {
        LOG.trace("moveToNextSuggestion()");

        // Ensure that predictions are switched
        annotationPage.actionRefreshDocument(aTarget);

        var alState = alStateModel.getObject();
        var state = getModelObject();
        var project = state.getProject();
        var dataOwner = state.getUser();
        var sessionOwner = userService.getCurrentUser();

        // Generate the next recommendation but remember the current one
        var currentSuggestion = alState.getSuggestion();
        var nextSuggestion = activeLearningService
                .generateNextSuggestion(sessionOwner.getUsername(), dataOwner, alState);
        alState.setCurrentDifference(nextSuggestion);

        // If there is no new suggestion, nothing left to do here
        if (!alState.getSuggestion().isPresent()) {
            if (currentSuggestion.isPresent()) {
                infoOnce(aTarget, "There are no more recommendations right now.");
            }

            clearActiveLearningHighlight();
            aTarget.add(alMainContainer);

            return;
        }

        // If the active suggestion has changed, inform the user
        if (currentSuggestion.isPresent()
                && !alState.getSuggestion().get().equals(currentSuggestion.get())) {
            // infoOnce(aTarget, "Active learning has moved to next best suggestion.");
            LOG.trace("Moving from {} to {}", currentSuggestion.get(),
                    alState.getSuggestion().get());
        }

        // If there is a suggestion, open it in the sidebar and take the main editor to its location
        var suggestion = alState.getSuggestion().get();
        var document = documentService.getSourceDocument(project.getId(),
                suggestion.getDocumentId());
        loadSuggestionInActiveLearningSidebar(aTarget, suggestion, document);

        if (shouldBeClearningSelectionAndJumpingToSuggestion()) {
            clearSelectedAnnotationAndJumpToSuggestion(aTarget);
            LOG.trace("Clearing and jumping");
        }
        else {
            LOG.trace("Not clearing and jumping");
        }

        var alternativeSuggestions = recommendationService.getPredictions(dataOwner, project)
                .getAlternativeSuggestions(suggestion);
        applicationEventPublisherHolder.get()
                .publishEvent(new ActiveLearningSuggestionOfferedEvent(this, document, suggestion,
                        dataOwner.getUsername(), alState.getLayer(), suggestion.getFeature(),
                        alternativeSuggestions));
    }

    private void clearSelectedAnnotationAndJumpToSuggestion(AjaxRequestTarget aTarget)
    {
        var alState = alStateModel.getObject();
        if (!alState.getSuggestion().isPresent()) {
            return;
        }

        // If the user performed an action in the main editor, then we need to behave "as usual".
        // I.e. we must not make the editor/feature details jump to the next suggestion but rather
        // keep the view and selected annotation that the user has chosen to provide the opportunity
        // to the user to continue editing on it
        var suggestion = alState.getSuggestion().get();
        var state = getModelObject();
        var project = state.getProject();
        var user = state.getUser();
        var sourceDocument = documentService.getSourceDocument(project.getId(),
                suggestion.getDocumentId());

        LOG.trace("Jumping to {}", suggestion);

        try {
            // Clear the annotation detail editor and the selection to avoid confusions with the
            // highlight because the selection highlight from the right sidebar and the one from
            // the AL sidebar have the same color!
            state.getSelection().clear();
            aTarget.add((Component) getActionHandler());

            getAnnotationPage().actionShowSelectedDocument(aTarget, sourceDocument,
                    suggestion.getBegin(), suggestion.getEnd());

            // When the document is opened, the recommendation service defaults to only
            // predicting for the current document. Therefore, while in an AL session,
            // we kindly as again to predict for all documents
            // See also PredictionTask::execute where it is set again to predict on single documents
            recommendationService.setPredictForAllDocuments(user.getUsername(), project, true);
        }
        catch (IOException e) {
            LOG.error("Error reading CAS: {}", e.getMessage());
            error("Error reading CAS " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private void loadSuggestionInActiveLearningSidebar(AjaxRequestTarget aTarget,
            SpanSuggestion suggestion, SourceDocument sourceDocument)
    {
        aTarget.add(alMainContainer);
        setActiveLearningHighlight(suggestion);
        refreshActiveLearningFeatureEditor(aTarget, suggestion);

        // Obtain some left and right context of the active suggestion while we have easy
        // access to the document which contains the current suggestion
        try {
            var cas = documentService.readAnnotationCas(sourceDocument,
                    AnnotationSet.forUser(getModelObject().getUser()), AUTO_CAS_UPGRADE,
                    SHARED_READ_ONLY_ACCESS);
            var text = cas.getDocumentText();

            var alState = alStateModel.getObject();
            alState.setLeftContext(
                    text.substring(Math.max(0, suggestion.getBegin() - 20), suggestion.getBegin()));
            alState.setRightContext(text.substring(suggestion.getEnd(),
                    Math.min(suggestion.getEnd() + 20, text.length())));
        }
        catch (IOException e) {
            LOG.error("Error reading CAS: {}", e.getMessage());
            error("Error reading CAS " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private Form<?> createLearningHistory()
    {
        var learningHistoryForm = new Form<Void>(CID_LEARNING_HISTORY_FORM);
        learningHistoryForm.add(LambdaBehavior.onConfigure(
                component -> component.setVisible(alStateModel.getObject().isSessionActive())));
        learningHistoryForm.setOutputMarkupPlaceholderTag(true);
        learningHistoryForm.setOutputMarkupId(true);

        learningHistoryForm.add(createLearningHistoryListView());
        return learningHistoryForm;
    }

    private ListView<LearningRecord> createLearningHistoryListView()
    {
        var learningHistory = new ListView<LearningRecord>(CID_HISTORY_LISTVIEW)
        {
            private static final long serialVersionUID = 5594228545985423567L;

            @Override
            protected void populateItem(ListItem<LearningRecord> item)
            {
                var rec = item.getModelObject();
                var recAnnotationFeature = rec.getAnnotationFeature();
                String recFeatureValue;
                if (recAnnotationFeature != null) {
                    FeatureSupport<?> featureSupport = featureSupportRegistry
                            .findExtension(recAnnotationFeature).orElseThrow();
                    recFeatureValue = featureSupport.renderFeatureValue(recAnnotationFeature,
                            rec.getAnnotation());
                }
                else {
                    recFeatureValue = rec.getAnnotation();
                }

                var textLink = new LambdaAjaxLink(CID_JUMP_TO_ANNOTATION,
                        _target -> actionSelectHistoryItem(_target, item.getModelObject()));
                textLink.setBody(rec::getTokenText);
                item.add(textLink);

                if (isBlank(recFeatureValue)) {
                    recFeatureValue = "<no label>";
                }

                item.add(new Label(CID_RECOMMENDED_ANNOTATION, recFeatureValue));
                item.add(new Label(CID_USER_ACTION, rec.getUserAction()));
                item.add(new LambdaAjaxLink(CID_REMOVE_RECORD,
                        t -> actionRemoveHistoryItem(t, rec)));
            }
        };
        learningRecords = LoadableDetachableModel.of(this::listLearningRecords);
        learningHistory.setModel(learningRecords);
        return learningHistory;
    }

    /**
     * Select an item from the learning history. When the user clicks on an item from the learning
     * history, the following should happen:
     * <ul>
     * <li>the main editor should jump to the location of the history item</li>
     * <li>if there is an annotation which matches the history item in terms of layer and feature
     * value, then this annotation should be highlighted.</li>
     * <li>if there is no matching annotation, then the text should be highlighted</li>
     * </ul>
     */
    private void actionSelectHistoryItem(AjaxRequestTarget aTarget, LearningRecord aRecord)
        throws IOException
    {
        getAnnotationPage().actionShowSelectedDocument(aTarget, aRecord.getSourceDocument(),
                aRecord.getOffsetBegin(), aRecord.getOffsetEnd());

        // Since we have switched documents above (if it was necessary), the editor CAS should
        // now point to the correct one
        var cas = getCasProvider().get();

        // ... if a matching annotation exists, highlight the annotaiton
        var annotation = getMatchingAnnotation(cas, aRecord);

        if (annotation.isPresent()) {
            setActiveLearningHighlight(aRecord.getSourceDocument(), annotation.get());
        }
        // ... otherwise highlight the text
        else {
            setActiveLearningHighlight(aRecord);

            info("No annotation could be highlighted.");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private Optional<AnnotationFS> getMatchingAnnotation(CAS aCas, LearningRecord aRecord)
    {
        var type = CasUtil.getType(aCas, alStateModel.getObject().getLayer().getName());
        var feature = type.getFeatureByBaseName(aRecord.getAnnotationFeature().getName());
        return selectAt(aCas, type, aRecord.getOffsetBegin(), aRecord.getOffsetEnd()).stream()
                .filter(fs -> Objects.equals(aRecord.getAnnotation(),
                        fs.getFeatureValueAsString(feature)))
                .findFirst();
    }

    private List<SpanSuggestion> getMatchingSuggestion(
            List<SuggestionGroup<SpanSuggestion>> aSuggestions, LearningRecord aRecord)
    {
        return getMatchingSuggestion(aSuggestions, aRecord.getSourceDocument().getId(),
                aRecord.getLayer().getId(), aRecord.getAnnotationFeature().getName(),
                aRecord.getOffsetBegin(), aRecord.getOffsetEnd(), aRecord.getAnnotation());
    }

    private List<SpanSuggestion> getMatchingSuggestion(
            List<SuggestionGroup<SpanSuggestion>> aSuggestions, SpanSuggestion aSuggestion)
    {
        return getMatchingSuggestion(aSuggestions, aSuggestion.getDocumentId(),
                aSuggestion.getLayerId(), aSuggestion.getFeature(), aSuggestion.getBegin(),
                aSuggestion.getEnd(), aSuggestion.getLabel());
    }

    private List<SpanSuggestion> getMatchingSuggestion(
            List<SuggestionGroup<SpanSuggestion>> aSuggestions, long aDocumentId, long aLayerId,
            String aFeature, int aBegin, int aEnd, String aLabel)
    {
        return aSuggestions.stream() //
                .filter(group -> group.getPosition() instanceof Offset) //
                .filter(group -> aDocumentId == group.getDocumentId()
                        && aLayerId == group.getLayerId()
                        && (aFeature == null || aFeature.equals(group.getFeature()))
                        && (aBegin == -1 || aBegin == ((Offset) group.getPosition()).getBegin())
                        && (aEnd == -1 || aEnd == ((Offset) group.getPosition()).getEnd()))
                .flatMap(group -> group.stream()) //
                .filter(suggestion -> suggestion.isVisible()) //
                .filter(suggestion -> aLabel == null || aLabel.equals(suggestion.getLabel()))
                .collect(toList());
    }

    private List<LearningRecord> listLearningRecords()
    {
        var sessionOwner = userService.getCurrentUsername();
        return learningRecordService.listLearningRecords(sessionOwner,
                getModelObject().getUser().getUsername(), alStateModel.getObject().getLayer(), 50);
    }

    private void actionRemoveHistoryItem(AjaxRequestTarget aTarget, LearningRecord aRecord)
        throws IOException, AnnotationException
    {
        getAnnotationPage().ensureIsEditable();

        aTarget.add(alMainContainer);

        var alState = alStateModel.getObject();

        annotationPage.actionRefreshDocument(aTarget);
        learningRecordService.deleteLearningRecord(aRecord);

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
            var cas = documentService.readAnnotationCas(aRecord.getSourceDocument(),
                    AnnotationSet.forUser(aRecord.getUser()));
            if (getMatchingAnnotation(cas, aRecord).isPresent()) {
                setActiveLearningHighlight(aRecord);
                getAnnotationPage().actionShowSelectedDocument(aTarget, aRecord.getSourceDocument(),
                        aRecord.getOffsetBegin(), aRecord.getOffsetEnd());

                openHistoryItemRemovalConfirmationDialog(aTarget, aRecord);
            }
        }

        // If there is currently no suggestion (i.e. we ran out of suggestions before) there is a
        // good chance that deleting the history item makes suggestions become available again, so
        // we try to find a new one.
        if (!alState.getSuggestion().isPresent()) {
            refreshAvailableSuggestions();
            requestClearningSelectionAndJumpingToSuggestion();
            moveToNextSuggestion(aTarget);
        }
    }

    private void openHistoryItemRemovalConfirmationDialog(AjaxRequestTarget aTarget,
            LearningRecord aRecord)
    {
        var dialogContent = new HistoryItemDeleteConfirmationDialogPanel(
                BootstrapModalDialog.CONTENT_ID);

        dialogContent.setConfirmAction(_t -> {
            if (alStateModel.getObject().getSuggestion().isPresent()) {
                setActiveLearningHighlight(alStateModel.getObject().getSuggestion().get());
            }
            else {
                clearActiveLearningHighlight();
            }
            deleteAnnotationByHistory(_t, aRecord);
        });

        dialogContent.setCancelAction(_t -> {
            if (alStateModel.getObject().getSuggestion().isPresent()) {
                setActiveLearningHighlight(alStateModel.getObject().getSuggestion().get());
            }
            else {
                clearActiveLearningHighlight();
            }
            annotationPage.actionRefreshDocument(_t);
        });

        dialog.open(dialogContent, aTarget);
    }

    private void deleteAnnotationByHistory(AjaxRequestTarget aTarget, LearningRecord aRecord)
        throws IOException, AnnotationException
    {
        AnnotatorState state = getModelObject();

        CAS cas = this.getCasProvider().get();
        Optional<AnnotationFS> anno = getMatchingAnnotation(cas, aRecord);
        if (anno.isPresent()) {
            state.getSelection().selectSpan(VID.of(anno.get()), cas, aRecord.getOffsetBegin(),
                    aRecord.getOffsetEnd());
            getActionHandler().actionDelete(aTarget);
        }
    }

    @OnEvent
    public void onSpanCreated(SpanCreatedEvent aEvent)
    {
        // Is active learning on and is any suggestion currently displayed?
        ActiveLearningUserState alState = alStateModel.getObject();
        if (!alState.isSessionActive() || !alState.getSuggestion().isPresent()) {
            return;
        }

        // Does event match the current active learning configuration?
        if (!aEvent.getDocumentOwner().equals(getModelObject().getUser().getUsername())
                || !aEvent.getLayer().equals(alState.getLayer())) {
            return;
        }

        reactToAnnotationsBeingCreatedOrDeleted(aEvent.getRequestTarget().orElse(null),
                aEvent.getLayer(), aEvent.getDocument());
    }

    @OnEvent
    public void onRelationCreated(RelationCreatedEvent aEvent)
    {
        // Is active learning on and is any suggestion currently displayed?
        ActiveLearningUserState alState = alStateModel.getObject();
        if (!alState.isSessionActive() || !alState.getSuggestion().isPresent()) {
            return;
        }

        // Does event match the current active learning configuration?
        if (!aEvent.getDocumentOwner().equals(getModelObject().getUser().getUsername())
                || !aEvent.getLayer().equals(alState.getLayer())) {
            return;
        }

        reactToAnnotationsBeingCreatedOrDeleted(aEvent.getRequestTarget().get(), aEvent.getLayer(),
                aEvent.getDocument());
    }

    /**
     * Listens to the user setting a feature on an annotation in the main annotation editor. Mind
     * that we do not need to listen to the creation of annotations since they have no effect on the
     * active learning sidebar as long as they have no features set.
     * 
     * @param aEvent
     *            the event
     */
    @OnEvent
    public void onFeatureValueUpdated(FeatureValueUpdatedEvent aEvent)
    {
        LOG.trace("onFeatureValueUpdated()");

        // Is active learning on and is any suggestion currently displayed?
        ActiveLearningUserState alState = alStateModel.getObject();
        if (!alState.isSessionActive() || !alState.getSuggestion().isPresent()) {
            return;
        }

        // Does event match the current active learning configuration?
        if (!aEvent.getDocumentOwner().equals(getModelObject().getUser().getUsername())
                || !aEvent.getFeature().getLayer().equals(alState.getLayer())
                || !aEvent.getFeature().getName()
                        .equals(alState.getSuggestion().get().getFeature())) {
            return;
        }

        reactToAnnotationsBeingCreatedOrDeleted(aEvent.getRequestTarget().get(),
                aEvent.getFeature().getLayer(), aEvent.getDocument());
    }

    /**
     * Listens to the user deleting an annotation in the main annotation editor.
     * 
     * @param aEvent
     *            the event
     */
    @OnEvent
    public void onAnnotationDeleted(SpanDeletedEvent aEvent)
    {
        LOG.trace("onAnnotationDeleted()");

        // Is active learning on and is any suggestion currently displayed?
        ActiveLearningUserState alState = alStateModel.getObject();
        if (!alState.isSessionActive() || !alState.getSuggestion().isPresent()) {
            return;
        }

        // Does event match the current active learning configuration?
        if (!aEvent.getDocumentOwner().equals(getModelObject().getUser().getUsername())
                || !aEvent.getLayer().equals(alState.getLayer())) {
            return;
        }

        reactToAnnotationsBeingCreatedOrDeleted(aEvent.getRequestTarget().get(), aEvent.getLayer(),
                aEvent.getDocument());
    }

    private void reactToAnnotationsBeingCreatedOrDeleted(AjaxRequestTarget aTarget,
            AnnotationLayer aLayer, SourceDocument aDocument)
    {
        LOG.trace("reactToAnnotationsBeingCreatedOrDeleted()");

        try {
            var sessionOwner = userService.getCurrentUsername();
            var dataOwner = getModelObject().getUser();
            var predictions = recommendationService.getPredictions(dataOwner, aLayer.getProject());

            if (predictions == null) {
                return;
            }

            // Ensure that the predictions have been switched so we do not update visibility in an
            // outdated prediction state.
            annotationPage.actionRefreshDocument(aTarget);

            // Update visibility in case the that was created/deleted overlaps with any suggestions
            var cas = documentService.readAnnotationCas(aDocument,
                    AnnotationSet.forUser(dataOwner));
            var group = SuggestionDocumentGroup.groupsOfType(SpanSuggestion.class,
                    predictions.getPredictionsByDocument(aDocument.getId()));
            recommendationService.calculateSuggestionVisibility(sessionOwner, aDocument, cas,
                    dataOwner.getUsername(), aLayer, group, 0, cas.getDocumentText().length());

            moveToNextSuggestion(aTarget);
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
        LOG.trace("onRecommendationRejectEvent()");

        var annotatorState = getModelObject();
        var eventState = aEvent.getAnnotatorState();

        var predictions = recommendationService.getPredictions(annotatorState.getUser(),
                annotatorState.getProject());

        if (alStateModel.getObject().isSessionActive()
                && eventState.getUser().equals(annotatorState.getUser())
                && eventState.getProject().equals(annotatorState.getProject())) {
            var doc = eventState.getDocument();
            var vid = VID.parse(aEvent.getVid().getExtensionPayload());
            var prediction = predictions.getPredictionByVID(doc, vid) //
                    .filter(f -> f instanceof SpanSuggestion) //
                    .map(f -> (SpanSuggestion) f);

            if (!prediction.isPresent()) {
                LOG.error("Could not find prediction in [{}] with id [{}]", doc, vid);
                error("Could not find prediction");
                return;
            }

            var rejectedRecommendation = prediction.get();
            var alternativeSuggestions = predictions
                    .getAlternativeSuggestions(rejectedRecommendation);
            applicationEventPublisherHolder.get()
                    .publishEvent(new ActiveLearningRecommendationEvent(this,
                            eventState.getDocument(), rejectedRecommendation,
                            annotatorState.getUser().getUsername(),
                            eventState.getSelectedAnnotationLayer(),
                            rejectedRecommendation.getFeature(), REJECTED, alternativeSuggestions));

            if (doc.equals(annotatorState.getDocument())
                    && vid.getLayerId() == alStateModel.getObject().getLayer().getId() && prediction
                            .get().equals(alStateModel.getObject().getSuggestion().orElse(null))) {
                requestClearningSelectionAndJumpingToSuggestion();
                moveToNextSuggestion(aEvent.getTarget());
            }

            aEvent.getTarget().add(alMainContainer);
        }
    }

    /**
     * Listens to the user accepting a recommendation in the main annotation editor.
     * 
     * @param aEvent
     *            the event
     */
    @OnEvent
    public void onRecommendationAcceptEvent(AjaxRecommendationAcceptedEvent aEvent)
    {
        LOG.trace("onRecommendationAcceptEvent()");

        var state = getModelObject();
        var predictions = recommendationService.getPredictions(state.getUser(), state.getProject());
        var doc = state.getDocument();
        var vid = VID.parse(aEvent.getSuggestionVid().getExtensionPayload());

        var oRecommendation = predictions.getPredictionByVID(doc, vid) //
                .filter(f -> f instanceof SpanSuggestion) //
                .map(f -> (SpanSuggestion) f);
        if (!oRecommendation.isPresent()) {
            LOG.error("Could not find prediction in [{}] with id [{}]", doc, vid);
            error("Could not find prediction");
            aEvent.getTarget().addChildren(getPage(), IFeedback.class);
            return;
        }

        var acceptedSuggestion = oRecommendation.get();

        var alternativeSuggestions = predictions.getAlternativeSuggestions(acceptedSuggestion);
        applicationEventPublisherHolder.get()
                .publishEvent(new ActiveLearningRecommendationEvent(this, aEvent.getDocument(),
                        acceptedSuggestion, state.getUser().getUsername(), aEvent.getLayer(),
                        acceptedSuggestion.getFeature(), ACCEPTED, alternativeSuggestions));

        // If the annotation that the user accepted is the one that is currently displayed in
        // the annotation sidebar, then we have to go and pick a new one
        var alState = alStateModel.getObject();
        if (alState.isSessionActive() && alState.getSuggestion().isPresent()
                && aEvent.getDataOwner().equals(state.getUser())
                && aEvent.getProject().equals(state.getProject())) {
            SpanSuggestion suggestion = alState.getSuggestion().get();
            if (acceptedSuggestion.getPosition().equals(suggestion.getPosition())
                    && vid.getLayerId() == suggestion.getLayerId()
                    && acceptedSuggestion.getFeature().equals(suggestion.getFeature())) {
                requestClearningSelectionAndJumpingToSuggestion();
                moveToNextSuggestion(aEvent.getTarget());
            }
            aEvent.getTarget().add(alMainContainer);
        }
    }

    @OnEvent
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        renderHighlights(aEvent.getVDocument());
    }

    private void renderHighlights(VDocument aVDoc)
    {
        LOG.trace("Active learning sidebar rendering highlights: {}", highlightSuggestion);

        // Clear any highlights that we may have added earlier in the rendering process and
        // recreate them because the VIDs may have changed
        aVDoc.getMarkers().removeIf(marker -> marker.getSource() == this);

        if (highlightDocumentId == null) {
            LOG.trace("Active learning sidebar has no highlights to render");
            return;
        }

        var currentDoc = getModelObject().getDocument();
        if (!Objects.equals(currentDoc.getId(), highlightDocumentId)) {
            LOG.trace("Active learning sidebar highlights are in document [{}], not in [{}]",
                    highlightDocumentId, currentDoc);
            return;
        }

        if (highlightVID != null) {
            aVDoc.add(new VAnnotationMarker(this, VMarker.FOCUS, highlightVID));
        }
        else {
            LOG.trace("Active learning sidebar annotation highlight is not set");
        }

        if (highlightSpan != null) {
            Optional<VRange> range = VRange.clippedRange(aVDoc, highlightSpan.getBegin(),
                    highlightSpan.getEnd());

            if (range.isPresent()) {
                aVDoc.add(new VTextMarker(this, VMarker.FOCUS, range.get()));
            }
            else {
                LOG.trace("Active learning sidebar span highlight is outside visible area");
            }
        }
        else {
            LOG.trace("Active learning sidebar span highlight is not set");
        }
    }

    private void refreshAvailableSuggestions()
    {
        LOG.trace("refreshAvailableSuggestions()");

        var state = getModelObject();
        var alState = alStateModel.getObject();
        var suggestions = activeLearningService.getSuggestions(state.getUser(), alState.getLayer());
        alState.setSuggestions(suggestions);
    }

    @OnEvent
    public void onDocumentOpenedEvent(DocumentOpenedEvent aEvent)
    {
        // If active learning is not active, update the sidebar in case the session auto-terminated
        var alState = alStateModel.getObject();
        if (!alState.isSessionActive()) {
            aEvent.getRequestTarget().ifPresent(target -> target.add(alMainContainer));
            return;
        }

        // Make sure we know about the current suggestions and their visibility state
        refreshAvailableSuggestions();

        // Maybe the prediction switch has made a new suggestion available for us to go to
        if (alState.getSuggestion().isEmpty()) {
            moveToNextSuggestion(aEvent.getRequestTarget().get());
            return;
        }

        refreshCurrentSuggestionOrMoveToNextSuggestion(aEvent.getRequestTarget().get());
    }

    @OnEvent
    public void onPredictionsSwitched(PredictionsSwitchedEvent aEvent)
    {
        LOG.trace("onPredictionsSwitched()");

        // If active learning is not active, update the sidebar in case the session auto-terminated
        ActiveLearningUserState alState = alStateModel.getObject();
        if (!alState.isSessionActive()) {
            aEvent.getRequestTarget().ifPresent(target -> target.add(alMainContainer));
            return;
        }

        // Make sure we know about the current suggestions and their visibility state
        refreshAvailableSuggestions();

        // Maybe the prediction switch has made a new suggestion available for us to go to
        if (alState.getSuggestion().isEmpty()) {
            moveToNextSuggestion(aEvent.getRequestTarget().get());
            return;
        }

        refreshCurrentSuggestionOrMoveToNextSuggestion(aEvent.getRequestTarget().get());
    }

    private void refreshCurrentSuggestionOrMoveToNextSuggestion(AjaxRequestTarget aTarget)
    {
        LOG.trace("refreshCurrentSuggestionOrMoveToNextSuggestion()");

        // If there is currently a suggestion displayed in the sidebar, we need to check if it
        // is still relevant - if yes, we need to replace it with its current counterpart since.
        // if no counterpart exists in the current suggestions, then we need to load a
        // suggestion from the current list.
        var alState = alStateModel.getObject();
        var activeSuggestion = alState.getSuggestion().get();
        // Find the groups which matches the active recommendation
        var updatedSuggestion = getMatchingSuggestion(alState.getSuggestions(), activeSuggestion)
                .stream().findFirst();

        if (updatedSuggestion.isEmpty()) {
            moveToNextSuggestion(aTarget);
            return;
        }

        LOG.debug("Replacing outdated suggestion {} with new suggestion {}",
                alState.getCurrentDifference().get().getFirst(), updatedSuggestion.get());

        // Update the highlight
        if (alState.getSuggestion().get().getVID().equals(highlightVID)) {
            highlightVID = updatedSuggestion.get().getVID();
        }

        // We found a matching suggestion, but we look for its second-best. So for the moment we
        // assume that the second-best has not changed and we simply fake a delta
        alState.setCurrentDifference(Optional.of(new Delta<>(updatedSuggestion.get(),
                alState.getCurrentDifference().get().getSecond().orElse(null))));
    }

    private void infoOnce(IPartialPageRequestHandler aTarget, String aMessage)
    {
        info(aMessage);
        aTarget.addChildren(getPage(), IFeedback.class);

        // // Avoid logging the message multiple times in case the move to the next suggestion has
        // // been requested multiple times in a single request
        // if (getFeedbackMessages().messages(msg -> msg.getMessage().equals(aMessage)).isEmpty()) {
        // info(aMessage);
        // aTarget.addChildren(getPage(), IFeedback.class);
        // }
    }
}
