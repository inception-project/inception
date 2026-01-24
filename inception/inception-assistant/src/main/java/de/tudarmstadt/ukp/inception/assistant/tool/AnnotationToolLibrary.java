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
package de.tudarmstadt.ukp.inception.assistant.tool;

import static de.tudarmstadt.ukp.inception.assistant.AssistantPredictionSources.ASSISTANT_SOURCE;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.NEW_ID;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolStatusResponse.success;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.assistant.CommandDispatcher;
import de.tudarmstadt.ukp.inception.assistant.model.MRefreshCommand;
import de.tudarmstadt.ukp.inception.assistant.recommender.AssistantRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationEditorContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.Tool;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolLibrary;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolParam;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolStatusResponse;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class AnnotationToolLibrary
    implements ToolLibrary
{
    private static final String CREATE_SPAN_SUGGESTIONS_DESCRIPTION = """
            Creates span annotation suggestions at specified character offsets in the current document.
            Suggestions appear in the editor for the user to review and accept or reject.
            Use this when the user asks you to annotate, mark, or identify entities, mentions, or spans of text.
            """;

    private final RecommendationService recommendationService;
    private final AnnotationSchemaService schemaService;
    private final UserDao userService;

    public AnnotationToolLibrary(RecommendationService aRecommendationService,
            AnnotationSchemaService aSchemaService, UserDao aUserService)
    {
        recommendationService = aRecommendationService;
        schemaService = aSchemaService;
        userService = aUserService;
    }

    @Override
    public String getId()
    {
        return getClass().getName();
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return true;
    }

    @Tool( //
            value = "create_span_suggestions", //
            actor = "AI Assistant", //
            description = CREATE_SPAN_SUGGESTIONS_DESCRIPTION)
    public ToolStatusResponse createSpanSuggestions( //
            AnnotationEditorContext aContext, //
            CommandDispatcher aCommandDispatcher, //
            @ToolParam(value = "layer", description = "Name of the annotation layer") //
            String aLayerName, //
            @ToolParam(value = "suggestions", description = "List of span suggestions with begin/end offsets and labels") //
            List<SpanSpec> aSuggestions)
        throws IOException
    {
        var sessionOwner = userService.getCurrentUser();
        var project = aContext.getProject();
        var document = aContext.getDocument();

        // Find the layer
        var layer = schemaService.findLayer(project, aLayerName);
        if (layer == null) {
            return ToolStatusResponse.error("Layer '" + aLayerName + "' not found in project");
        }

        // Get or create the assistant recommender for this layer
        var recommender = getOrCreateAssistantRecommender(project, layer);

        // Get predecessor predictions for inheritance
        var activePredictions = recommendationService.getPredictions(sessionOwner, project,
                ASSISTANT_SOURCE);
        var incomingPredictions = recommendationService.getIncomingPredictions(sessionOwner,
                project, ASSISTANT_SOURCE);
        var predecessor = incomingPredictions != null ? incomingPredictions : activePredictions;

        // Create new Predictions (copy predecessor to inherit existing suggestions)
        var predictions = predecessor != null ? new Predictions(predecessor)
                : new Predictions(sessionOwner, sessionOwner.getUsername(), project);

        // Build new suggestions
        var newSuggestions = new ArrayList<AnnotationSuggestion>();
        for (var spec : aSuggestions) {
            var suggestion = SpanSuggestion.builder() //
                    .withId(NEW_ID) //
                    .withGeneration(predictions.getGeneration()) //
                    .withRecommender(recommender) //
                    .withDocument(document) //
                    .withPosition(new Offset(spec.begin, spec.end)) //
                    .withLabel(spec.label) //
                    .build();
            newSuggestions.add(suggestion);
        }

        // Add new suggestions to predictions
        predictions.putSuggestions(newSuggestions.size(), 0, 0, newSuggestions);

        // Mark document as prediction completed
        predictions.markDocumentAsPredictionCompleted(document);

        // Queue for rendering
        recommendationService.putIncomingPredictions(sessionOwner, project, ASSISTANT_SOURCE,
                predictions);

        // Automatically refresh the editor
        if (aCommandDispatcher != null) {
            aCommandDispatcher.dispatch(new MRefreshCommand());
        }

        return success(
                "Created " + aSuggestions.size() + " suggestion(s) on layer '" + aLayerName + "'");
    }

    private synchronized Recommender getOrCreateAssistantRecommender(Project aProject,
            AnnotationLayer aLayer)
    {
        // Find existing assistant recommender for this project (any layer)
        var existing = recommendationService.listRecommenders(aProject).stream()
                .filter(r -> AssistantRecommenderFactory.ID.equals(r.getTool())).findFirst();

        if (existing.isPresent()) {
            var recommender = existing.get();
            // Reconfigure if layer/feature changed
            if (!recommender.getLayer().equals(aLayer)) {
                recommender.setLayer(aLayer);
                var features = schemaService.listSupportedFeatures(aLayer);
                var feature = features.isEmpty() ? null : features.get(0);
                recommender.setFeature(feature);
                recommendationService.createOrUpdateRecommender(recommender);
            }
            return recommender;
        }

        // Create new recommender (first use in this project)
        var features = schemaService.listSupportedFeatures(aLayer);
        var feature = features.isEmpty() ? null : features.get(0);
        var recommender = Recommender.builder() //
                .withProject(aProject) //
                .withLayer(aLayer) //
                .withFeature(feature) //
                .withName("AI Assistant") //
                .withTool(AssistantRecommenderFactory.ID) //
                .withEnabled(true) //
                .build();
        recommendationService.createOrUpdateRecommender(recommender);
        return recommender;
    }

    public static class SpanSpec
    {
        public int begin;
        public int end;
        public String label;
    }
}
