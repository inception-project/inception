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
import org.apache.wicket.spring.injection.annot.SpringBean;
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
        
        ListView<Recommender> searchResultGroups = new ListView<Recommender>("recommender")
        {
            private static final long serialVersionUID = -631500052426449048L;

            @Override
            protected void populateItem(ListItem<Recommender> item)
            {
                User user = userService.getCurrentUser();
                Recommender recommender = item.getModelObject();
                List<EvaluatedRecommender> activeRecommenders = recommendationService
                        .getActiveRecommenders(user, recommender.getLayer());
                Optional<EvaluationResult> evalResult = activeRecommenders.stream()
                        .filter(r -> r.getRecommender().equals(recommender))
                        .map(EvaluatedRecommender::getEvaluationResult)
                        .findAny();
                item.add(new Label("name", recommender.getName()));
                item.add(new Label("state", evalResult.isPresent() ? "active" : "off"));

                item.add(new LambdaAjaxLink("acceptAll", _target -> 
                        actionAcceptAll(_target, recommender)));
                
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
        searchResultGroups.setModel(LoadableDetachableModel.of(() -> recommendationService
                .listEnabledRecommenders(aModel.getObject().getProject())));
        add(searchResultGroups);
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
    
    private void actionAcceptAll(AjaxRequestTarget aTarget, Recommender aRecommender)
        throws AnnotationException, IOException
    {
        User user = userService.getCurrentUser();
        AnnotatorState state = getModelObject();
        
        AnnotationPageBase page = findParent(AnnotationPageBase.class);
        
        CAS cas = page.getEditorCas();
        
        //SourceDocument document = state.getDocument();
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
                int address = recommendationService.upsertFeature(annotationService,
                        state.getDocument(), state.getUser().getUsername(), cas, layer, feature,
                        suggestion.getLabel(), suggestion.getBegin(), suggestion.getEnd());
        
                // Hide the suggestion. This is faster than having to recalculate the visibility
                // status for the entire document or even for the part visible on screen.
                suggestion.hide(FLAG_TRANSIENT_ACCEPTED);
            
//               // Log the action to the learning record
//               learningRecordService.logRecord(document, aState.getUser().getUsername(),
//                       suggestion, layer, feature, ACCEPTED, MAIN_EDITOR);
//            
//               // Send an application event that the suggestion has been accepted
//               AnnotationFS fs = WebAnnoCasUtil.selectByAddr(aCas, AnnotationFS.class, address);
//               applicationEventPublisher.publishEvent(new RecommendationAcceptedEvent(this,
//                   document, aState.getUser().getUsername(), fs, feature, suggestion.getLabel()));
//            
//               // Send a UI event that the suggestion has been accepted
//               aTarget.getPage().send(aTarget.getPage(), Broadcast.BREADTH,
//                       new AjaxRecommendationAcceptedEvent(aTarget, aState, aVID));    }
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
