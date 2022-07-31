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
package de.tudarmstadt.ukp.inception.recommendation.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getDocumentTitle;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_ACCEPTED;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.repeat;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.TempFileResource;
import de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Progress;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup.GroupKey;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.event.PredictionsSwitchedEvent;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

public class RecommenderInfoPanel
    extends Panel
{
    private static final long serialVersionUID = -5921076859026638039L;

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean UserDao userService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;

    private ModalDialog detailsDialog;

    public RecommenderInfoPanel(String aId, IModel<AnnotatorState> aModel)
    {
        super(aId, aModel);

        setOutputMarkupId(true);

        User user = aModel.getObject().getUser();

        detailsDialog = new BootstrapModalDialog("detailsDialog").trapFocus().closeOnEscape()
                .closeOnClick();
        add(detailsDialog);

        add(new Label("progress", LoadableDetachableModel.of(() -> {
            Progress p = recommendationService.getProgressTowardsNextEvaluation(user,
                    aModel.getObject().getProject());
            return repeat("<i class=\"fas fa-circle\"></i>&nbsp;", p.getDone())
                    + repeat("<i class=\"far fa-circle\"></i>&nbsp;", p.getTodo());
        })).setEscapeModelStrings(false));

        WebMarkupContainer recommenderContainer = new WebMarkupContainer("recommenderContainer");
        add(recommenderContainer);

        ListView<Recommender> searchResultGroups = new ListView<Recommender>("recommender")
        {
            private static final long serialVersionUID = -631500052426449048L;

            @Override
            protected void populateItem(ListItem<Recommender> item)
            {
                Recommender recommender = item.getModelObject();
                Optional<EvaluatedRecommender> evaluatedRecommender = recommendationService
                        .getEvaluatedRecommender(user, recommender);
                item.add(new Label("name", recommender.getName()));

                WebMarkupContainer state = new WebMarkupContainer("state");
                if (evaluatedRecommender.isPresent()) {
                    EvaluatedRecommender evalRec = evaluatedRecommender.get();
                    if (evalRec.isActive()) {
                        state.add(new Icon("icon", FontAwesome5IconType.play_circle_s));
                        state.add(AttributeModifier.replace("title", "[Active]"));
                        state.add(AttributeModifier.append("title", evalRec.getReasonForState()));
                        state.add(AttributeModifier.append("class", "bg-success"));
                    }
                    else {
                        state.add(new Icon("icon", FontAwesome5IconType.stop_circle_s));
                        state.add(AttributeModifier.replace("title", "[Inactive]"));
                        state.add(AttributeModifier.append("title", evalRec.getReasonForState()));
                        state.add(AttributeModifier.append("style", "; cursor: help"));
                        state.add(AttributeModifier.append("class", "bg-danger"));
                    }
                }
                else {
                    state.add(new Icon("icon", FontAwesome5IconType.hourglass_half_s));
                    state.add(AttributeModifier.replace("title", "Pending..."));
                    state.add(AttributeModifier.append("class", "bg-light"));
                }
                item.add(state);

                Optional<EvaluationResult> evalResult = evaluatedRecommender
                        .map(EvaluatedRecommender::getEvaluationResult);
                WebMarkupContainer resultsContainer = new WebMarkupContainer("resultsContainer");
                // Show results only if the evaluation was not skipped (and of course only if the
                // result is actually present).
                resultsContainer.setVisible(evalResult.map(r -> !r.isEvaluationSkipped())
                        .orElse(evalResult.isPresent()));
                resultsContainer.add(new Label("f1Score",
                        evalResult.map(EvaluationResult::computeF1Score).orElse(0.0d)));
                resultsContainer.add(new Label("accuracy",
                        evalResult.map(EvaluationResult::computeAccuracyScore).orElse(0.0d)));
                resultsContainer.add(new Label("precision",
                        evalResult.map(EvaluationResult::computePrecisionScore).orElse(0.0d)));
                resultsContainer.add(new Label("recall",
                        evalResult.map(EvaluationResult::computeRecallScore).orElse(0.0d)));
                resultsContainer.add(new Label("sampleUnit",
                        evalResult.map(EvaluationResult::getSampleUnit).orElse("")));
                resultsContainer.add(new Label("trainingSampleCount",
                        evalResult.map(EvaluationResult::getTrainingSetSize).orElse(0)));
                resultsContainer.add(new Label("testSampleCount",
                        evalResult.map(EvaluationResult::getTestSetSize).orElse(0)));

                resultsContainer.add(new LambdaAjaxLink("acceptBest",
                        _tgt -> actionAcceptBest(_tgt, recommender))
                                .setVisible(evaluatedRecommender.map(EvaluatedRecommender::isActive)
                                        .orElse(false)));

                resultsContainer.add(new LambdaAjaxLink("showDetails",
                        _tgt -> actionShowDetails(_tgt, recommender)));

                AjaxDownloadLink exportModel = new AjaxDownloadLink("exportModel",
                        LoadableDetachableModel.of(() -> exportModelName(recommender)),
                        LoadableDetachableModel.of(() -> exportModel(user, recommender)));
                exportModel.add(visibleWhen(
                        () -> recommendationService.getRecommenderFactory(recommender).isPresent()
                                && recommendationService.getRecommenderFactory(recommender).get()
                                        .isModelExportSupported()));
                resultsContainer.add(exportModel);

                item.add(resultsContainer);

                item.add(
                        new Label("noEvaluationMessage",
                                evaluatedRecommender.map(EvaluatedRecommender::getReasonForState)
                                        .orElse("")).add(
                                                visibleWhen(() -> !resultsContainer.isVisible())));
            }
        };
        IModel<List<Recommender>> recommenders = LoadableDetachableModel
                .of(() -> recommendationService
                        .listEnabledRecommenders(aModel.getObject().getProject()));
        searchResultGroups.setModel(recommenders);

        recommenderContainer.add(visibleWhen(() -> !recommenders.getObject().isEmpty()));
        recommenderContainer.add(searchResultGroups);
    }

    private String exportModelName(Recommender aRecommender)
    {
        return recommendationService.getRecommenderFactory(aRecommender)
                .map(e -> e.getExportModelName(aRecommender)).orElse(null);
    }

    private IResourceStream exportModel(User aUser, Recommender aRecommender)
    {
        Optional<RecommendationEngineFactory<?>> maybeEngine = recommendationService
                .getRecommenderFactory(aRecommender);

        if (maybeEngine.isEmpty()) {
            error("No factory found for " + aRecommender.getName());
            return null;
        }

        RecommendationEngine engine = maybeEngine.get().build(aRecommender);

        Optional<RecommenderContext> context = recommendationService.getContext(aUser,
                aRecommender);

        if (context.isEmpty()) {
            error("No model trained yet.");
            return null;
        }

        return new TempFileResource((os) -> engine.exportModel(context.get(), os));
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    @OnEvent
    public void onRenderAnnotations(PredictionsSwitchedEvent aEvent)
    {
        aEvent.getRequestHandler().add(this);
    }

    private void actionShowDetails(AjaxRequestTarget aTarget, Recommender aRecommender)
    {
        RecommenderStatusDetailPanel panel = new RecommenderStatusDetailPanel(
                ModalDialog.CONTENT_ID,
                LoadableDetachableModel.of(() -> recommendationService
                        .getEvaluatedRecommender(userService.getCurrentUser(), aRecommender).get())
                        .map(EvaluatedRecommender::getEvaluationResult));
        detailsDialog.open(panel, aTarget);
    }

    private void actionAcceptBest(AjaxRequestTarget aTarget, Recommender aRecommender)
        throws AnnotationException, IOException
    {
        User user = userService.getCurrentUser();
        AnnotatorState state = getModelObject();

        AnnotationPageBase page = findParent(AnnotationPageBase.class);

        CAS cas = page.getEditorCas();

        Predictions predictions = recommendationService.getPredictions(user, state.getProject());
        if (predictions == null) {
            error("Recommenders did not yet provide any suggestions.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        Preferences pref = recommendationService.getPreferences(user, state.getProject());

        // TODO #176 use the document Id once it it available in the CAS
        String sourceDocumentName = CasMetadataUtils.getSourceDocumentName(cas)
                .orElse(getDocumentTitle(cas));

        // Extract all predictions for the current document / recommender
        Collection<SuggestionGroup<SpanSuggestion>> suggestionGroups = predictions
                .getPredictionsByRecommenderAndDocument(aRecommender, sourceDocumentName).stream()
                .filter(f -> f instanceof SpanSuggestion).map(f -> (SpanSuggestion) f)
                .filter(s -> s.isVisible() && s.getScore() >= pref.getScoreThreshold())
                .collect(groupingBy(GroupKey::new, TreeMap::new, SuggestionGroup.collector()))
                .values();

        int accepted = 0;
        int skippedDueToConflict = 0;
        int skippedDueToScoreTie = 0;
        for (SuggestionGroup<SpanSuggestion> suggestionGroup : suggestionGroups) {
            // We only want to accept the best suggestions
            List<SpanSuggestion> suggestions = suggestionGroup.bestSuggestions(pref);
            if (suggestions.size() > 1
                    && suggestions.get(0).getScore() == suggestions.get(1).getScore()) {
                skippedDueToScoreTie++;
                continue;
            }

            SpanSuggestion suggestion = suggestions.get(0);

            try {
                // Upsert an annotation based on the suggestion
                AnnotationLayer layer = annotationService.getLayer(suggestion.getLayerId());
                AnnotationFeature feature = annotationService.getFeature(suggestion.getFeature(),
                        layer);
                // int address =
                recommendationService.upsertSpanFeature(annotationService, state.getDocument(),
                        state.getUser().getUsername(), cas, layer, feature, suggestion.getLabel(),
                        suggestion.getBegin(), suggestion.getEnd());

                // Hide the suggestion. This is faster than having to recalculate the visibility
                // status for the entire document or even for the part visible on screen.
                suggestion.hide(FLAG_TRANSIENT_ACCEPTED);

                // // Log the action to the learning record
                // learningRecordService.logRecord(document, aState.getUser().getUsername(),
                // suggestion, layer, feature, ACCEPTED, MAIN_EDITOR);
                //
                // // Send an application event that the suggestion has been accepted
                // AnnotationFS fs = WebAnnoCasUtil.selectByAddr(aCas, AnnotationFS.class, address);
                // applicationEventPublisher.publishEvent(new RecommendationAcceptedEvent(this,
                // document, aState.getUser().getUsername(), fs, feature, suggestion.getLabel()));
                //
                // // Send a UI event that the suggestion has been accepted
                // aTarget.getPage().send(aTarget.getPage(), Broadcast.BREADTH,
                // new AjaxRecommendationAcceptedEvent(aTarget, aState, aVID)); }
                accepted++;
            }
            catch (AnnotationException e) {
                // FIXME We assume that any exception thrown here is because of a conflict with
                // an existing annotation - but actually it would be good to have proper
                // subclasses of the AnnotationException for different cases such that we can
                // provide a better account of why certain suggestions were not accepted.
                skippedDueToConflict++;
            }
        }

        // Save CAS after annotations have been created
        page.writeEditorCas(cas);

        if (accepted > 0) {
            success(String.format("Accepted %d suggestions", accepted));
            aTarget.addChildren(getPage(), IFeedback.class);
        }

        if (skippedDueToConflict > 0) {
            warn(String.format("Skipped %d suggestions due to conflicts", skippedDueToConflict));
            aTarget.addChildren(getPage(), IFeedback.class);
        }

        if (skippedDueToScoreTie > 0) {
            warn(String.format(
                    "Skipped %d suggestions due to score ties - annotate more, then retry",
                    skippedDueToScoreTie));
            aTarget.addChildren(getPage(), IFeedback.class);
        }

        page.actionRefreshDocument(aTarget);
    }
}
