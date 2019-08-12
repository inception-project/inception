/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getDocumentTitle;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_ACCEPTED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.event.PredictionsSwitchedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderEvaluationResultEvent;

public class RecommenderInfoPanel
    extends Panel
{
    private static final long serialVersionUID = -5921076859026638039L;

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean UserDao userService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private WebMarkupContainer resultsContainer;
    private ListView<EvaluatedRecommender> recommenderGroups; 
    
    // map recommender id to most recent evaluation result and recommender info
    private IModel<HashMap<Long,EvaluatedRecommender>> recommenderEvals;
    
    public RecommenderInfoPanel(String aId, IModel<AnnotatorState> aModel)
    {
        super(aId, aModel);
        setOutputMarkupId(true);
        
        WebMarkupContainer mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);
        
        recommenderEvals = new Model<HashMap<Long,EvaluatedRecommender>>(initRecommenderEvals());
        recommenderGroups = new ListView<EvaluatedRecommender>("recommender")
        {
            private static final long serialVersionUID = -631500052426449048L;

            @Override
            protected void populateItem(ListItem<EvaluatedRecommender> item)
            {
                EvaluatedRecommender evalRecommender = item.getModelObject();
                Optional<EvaluationResult> evalResult = evalRecommender.getEvaluationResult();
                Recommender recommender = evalRecommender.getRecommender();
                item.add(new Label("name", recommender.getName()));
                item.add(new Label("state", evalResult.isPresent() ? "active" : "off"));

                item.add(new LambdaAjaxLink("acceptAll", _target -> 
                        actionAcceptAll(_target, recommender)));
                
                resultsContainer = createResultsContainer(evalResult);
                item.add(resultsContainer);
            }
        };
        recommenderGroups.setDefaultModel(LoadableDetachableModel.of(this::getEvaluationResults));
        recommenderGroups.setOutputMarkupId(true);
        mainContainer.add(recommenderGroups);
        
        add(new WebSocketBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onConnect(ConnectedMessage aMessage)
            {
                super.onConnect(aMessage);
                log.debug(String.format("User with sessionID %s connected.",
                        aMessage.getSessionId()));
            }

            @Override
            protected void onPush(WebSocketRequestHandler aHandler, IWebSocketPushMessage aMessage)
            {
                if (aMessage instanceof RecommenderEvaluationResultEvent) {
                    log.debug(String.format("Received event: %s", aMessage.toString()));
                    RecommenderEvaluationResultEvent resultEvent = 
                            (RecommenderEvaluationResultEvent) aMessage;
                    Recommender recommender = resultEvent.getRecommender();
                    // update list of evaluated recommenders with their results and re-render
                    recommenderEvals.getObject().put(recommender.getId(),
                            resultEvent.getEvaluatedRecommender());
                    aHandler.add(mainContainer);
                }
            }
        });
    }

    
    @Override
    protected void onDetach()
    {
        super.onDetach();
        if (recommenderEvals != null) {
            recommenderEvals.detach();
        }
    }


    private List<EvaluatedRecommender> getEvaluationResults()
    {
        return new ArrayList<EvaluatedRecommender>(recommenderEvals.getObject().values());
    }
    
    /**
     * Initialize a map with previously evaluated recommenders of the current user and project 
     * and their results.
     */
    private HashMap<Long, EvaluatedRecommender> initRecommenderEvals()
    {
        HashMap<Long, EvaluatedRecommender> evals = new HashMap<>();
        for (Recommender recommender : recommendationService
                .listEnabledRecommenders(getPanelModelObject().getProject())) {
            
//            if (recommenderEvals != null &&
//                    recommenderEvals.containsKey(recommender.getId())) {
//                continue;
//            }
            
            List<EvaluatedRecommender> activeRecommenders = recommendationService
                    .getActiveRecommenders(getPanelModelObject().getUser(), recommender.getLayer());

            boolean foundEval = false;
            for (EvaluatedRecommender evalRecommender : activeRecommenders) {
                if (evalRecommender.getRecommender().equals(recommender)) { // this recommender has
                                                                            // a valid result
                    evals.put(recommender.getId(), evalRecommender);
                    foundEval = true;
                }
            }
            if (foundEval) {
                continue;
            }
            // FIXME: will this also add recommenders from other users?
            evals.put(recommender.getId(), new EvaluatedRecommender(recommender));
        }
        return evals;
    }

    private WebMarkupContainer createResultsContainer(
            Optional<EvaluationResult> evalResult)
    {
        WebMarkupContainer resultsContainer = new WebMarkupContainer("resultsContainer");
        resultsContainer.setVisible(evalResult.isPresent());
        resultsContainer.add(new Label("f1Score",
                evalResult.map(EvaluationResult::computeF1Score).orElse(0.0d)));
        resultsContainer.add(new Label("accuracy",
                evalResult.map(EvaluationResult::computeAccuracyScore).orElse(0.0d)));
        resultsContainer.add(new Label("precision",
                evalResult.map(EvaluationResult::computePrecisionScore).orElse(0.0d)));
        resultsContainer.add(new Label("recall",
                evalResult.map(EvaluationResult::computeRecallScore).orElse(0.0d)));
        
        return resultsContainer;
    }
    
    public AnnotatorState getPanelModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }
    
    @OnEvent
    public void onRenderAnnotations(PredictionsSwitchedEvent aEvent)
    {
        aEvent.getRequestHandler().add(this);
    }
    
    private void actionAcceptAll(AjaxRequestTarget aTarget, Recommender aRecommender)
        throws AnnotationException, IOException
    {
        AnnotatorState state = getPanelModelObject();
        User user = state.getUser();
        
        AnnotationPageBase page = findParent(AnnotationPageBase.class);
        
        CAS cas = page.getEditorCas();
        Predictions predictions = recommendationService.getPredictions(user, state.getProject());

        // TODO #176 use the document Id once it it available in the CAS
        String sourceDocumentName = CasMetadataUtils.getSourceDocumentName(cas)
                .orElse(getDocumentTitle(cas));
        
        // Extract all predictions for the current document / recommender
        List<AnnotationSuggestion> suggestions = predictions.getPredictions().entrySet().stream()
                .filter(f -> f.getKey().getDocumentName().equals(sourceDocumentName))
                .filter(f -> f.getKey().getRecommenderId() == aRecommender.getId().longValue())
                .map(Map.Entry::getValue)
                .filter(s -> s.isVisible())
                .collect(Collectors.toList());

        int accepted = 0;
        int skippedDueToConflict = 0;
        for (AnnotationSuggestion suggestion : suggestions) {
            try {
                // Upsert an annotation based on the suggestion
                AnnotationLayer layer = annotationService.getLayer(suggestion.getLayerId());
                AnnotationFeature feature = annotationService.getFeature(suggestion.getFeature(),
                        layer);
                recommendationService.upsertFeature(annotationService,
                        state.getDocument(), state.getUser().getUsername(), cas, layer, feature,
                        suggestion.getLabel(), suggestion.getBegin(), suggestion.getEnd());
        
                // Hide the suggestion. This is faster than having to recalculate the visibility
                // status for the entire document or even for the part visible on screen.
                suggestion.hide(FLAG_TRANSIENT_ACCEPTED);
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
            // we don't add the feedback panel to the AJAX target here because that happens during
            // the actionRefreshDocument() below anyway and adding it here already would make
            // the message disappear immediately
        }

        if (skippedDueToConflict > 0) {
            warn(String.format("Skipped %d suggestions due to conflicts", skippedDueToConflict));
            // we don't add the feedback panel to the AJAX target here because that happens during
            // the actionRefreshDocument() below anyway and adding it here already would make
            // the message disappear immediately
        }
        
        page.actionRefreshDocument(aTarget);
    }
}
