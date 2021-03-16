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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.CasMetadataUtils;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.TempFileResource;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup.SuggestionGroupKey;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.event.PredictionsSwitchedEvent;

public class RecommenderInfoPanel
    extends Panel
{
    private static final long serialVersionUID = -5921076859026638039L;

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean UserDao userService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;

    public RecommenderInfoPanel(String aId, IModel<AnnotatorState> aModel)
    {
        super(aId, aModel);

        setOutputMarkupId(true);

        WebMarkupContainer recommenderContainer = new WebMarkupContainer("recommenderContainer");
        add(recommenderContainer);

        ListView<Recommender> searchResultGroups = new ListView<Recommender>("recommender")
        {
            private static final long serialVersionUID = -631500052426449048L;

            @Override
            protected void populateItem(ListItem<Recommender> item)
            {
                User user = userService.getCurrentUser();
                Recommender recommender = item.getModelObject();
                List<EvaluatedRecommender> recommenders = recommendationService
                        .getEvaluatedRecommenders(user, recommender.getLayer());
                Optional<EvaluatedRecommender> evaluatedRecommender = recommenders.stream()
                        .filter(r -> r.getRecommender().equals(recommender)).findAny();
                item.add(new Label("name", recommender.getName()));

                Label state = new Label("state");
                if (evaluatedRecommender.isPresent()) {
                    EvaluatedRecommender evalRec = evaluatedRecommender.get();
                    if (evalRec.isActive()) {
                        state.setDefaultModel(Model.of("active"));
                        state.add(AttributeAppender.append("class", "badge-success"));
                    }
                    else {
                        state.setDefaultModel(Model.of("inactive"));
                        state.add(new AttributeModifier("title", evalRec.getDeactivationReason()));
                        state.add(AttributeAppender.append("class", "badge-danger"));
                    }
                }
                else {
                    state.setDefaultModel(Model.of("pending..."));
                    state.add(AttributeAppender.append("class", "badge-light"));
                }
                item.add(state);

                item.add(new LambdaAjaxLink("acceptBest",
                        _tgt -> actionAcceptBest(_tgt, recommender))
                                .setVisible(evaluatedRecommender.map(EvaluatedRecommender::isActive)
                                        .orElse(false)));

                AjaxDownloadLink exportModel = new AjaxDownloadLink("exportModel",
                        LoadableDetachableModel.of(() -> exportModelName(recommender)),
                        LoadableDetachableModel.of(() -> exportModel(user, recommender)));
                exportModel.add(visibleWhen(() -> recommendationService
                        .getRecommenderFactory(recommender).isModelExportSupported()));
                item.add(exportModel);

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
                item.add(resultsContainer);
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
        RecommendationEngineFactory factory = recommendationService
                .getRecommenderFactory(aRecommender);
        return factory.getExportModelName(aRecommender);
    }

    private IResourceStream exportModel(User aUser, Recommender aRecommender)
    {
        RecommendationEngine engine = recommendationService.getRecommenderFactory(aRecommender)
                .build(aRecommender);
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

    private void actionAcceptBest(AjaxRequestTarget aTarget, Recommender aRecommender)
        throws AnnotationException, IOException
    {
        User user = userService.getCurrentUser();
        AnnotatorState state = getModelObject();

        AnnotationPageBase page = findParent(AnnotationPageBase.class);

        CAS cas = page.getEditorCas();

        // SourceDocument document = state.getDocument();
        Predictions predictions = recommendationService.getPredictions(user, state.getProject());
        Preferences pref = recommendationService.getPreferences(user, state.getProject());

        // TODO #176 use the document Id once it it available in the CAS
        String sourceDocumentName = CasMetadataUtils.getSourceDocumentName(cas)
                .orElse(getDocumentTitle(cas));

        // Extract all predictions for the current document / recommender
        Collection<SuggestionGroup> suggestionGroups = predictions
                .getPredictionsByRecommenderAndDocument(aRecommender, sourceDocumentName).stream()
                .filter(s -> s.isVisible() && s.getConfidence() >= pref.getConfidenceThreshold())
                .collect(groupingBy(SuggestionGroupKey::new, TreeMap::new,
                        SuggestionGroup.collector()))
                .values();

        int accepted = 0;
        int skippedDueToConflict = 0;
        for (SuggestionGroup suggestionGroup : suggestionGroups) {
            // We only want to accept the best suggestions
            AnnotationSuggestion suggestion = suggestionGroup.bestSuggestions(pref).get(0);

            try {
                // Upsert an annotation based on the suggestion
                AnnotationLayer layer = annotationService.getLayer(suggestion.getLayerId());
                AnnotationFeature feature = annotationService.getFeature(suggestion.getFeature(),
                        layer);
                int address = recommendationService.upsertFeature(annotationService,
                        state.getDocument(), state.getUser().getUsername(), cas, layer, feature,
                        suggestion.getLabel(), suggestion.getBegin(), suggestion.getEnd());

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

        page.actionRefreshDocument(aTarget);
    }
}
