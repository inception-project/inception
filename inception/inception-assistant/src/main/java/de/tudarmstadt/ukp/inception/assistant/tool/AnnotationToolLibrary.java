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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.assistant.AssistantPredictionSources.ASSISTANT_SOURCE;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.NEW_ID;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolStatusResponse.success;
import static java.util.Collections.emptyList;
import static org.apache.wicket.util.lang.Objects.defaultIfNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.assistant.CommandDispatcher;
import de.tudarmstadt.ukp.inception.assistant.model.MRefreshCommand;
import de.tudarmstadt.ukp.inception.assistant.recommender.AssistantRecommenderFactory;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
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
import de.tudarmstadt.ukp.inception.support.uima.Range;

public class AnnotationToolLibrary
    implements ToolLibrary
{
    private static final String CREATE_SPAN_SUGGESTIONS_DESCRIPTION = """
            Creates span annotation suggestions at specified character offsets in the CURRENT DOCUMENT \
            being edited. Suggestions appear in the editor for the user to review and accept or reject. \
            Use this when the user asks you to annotate, mark, or identify entities, mentions, or spans \
            of text. The layer name must match one of the available annotation layers in the project schema.
            """;

    private final RecommendationService recommendationService;
    private final AnnotationSchemaService schemaService;
    private final DocumentService documentService;
    private final UserDao userService;

    public AnnotationToolLibrary(RecommendationService aRecommendationService,
            AnnotationSchemaService aSchemaService, UserDao aUserService,
            DocumentService aDocumentService)
    {
        recommendationService = aRecommendationService;
        schemaService = aSchemaService;
        userService = aUserService;
        documentService = aDocumentService;
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

    @Override
    public Collection<String> getSystemPrompts(Project aProject)
    {
        var prompts = new ArrayList<String>();

        prompts.add("""
                When asked to annotate or create suggestions, you should use the \
                create_span_suggestions tool. You will receive information about the \
                available annotation layers and features in the current project in each \
                conversation. Use the exact layer names provided when calling the tool.
                """);

        var layers = schemaService.listAnnotationLayer(aProject).stream() //
                .filter(AnnotationLayer::isEnabled) //
                .toList();

        if (!layers.isEmpty()) {
            var schema = new StringBuilder();
            schema.append("# Annotation Schema\n\n");
            schema.append("The following annotation layers are available:\n\n");

            for (var layer : layers) {
                schema.append("## Layer: ").append(layer.getName()).append("\n");
                schema.append("- **UI Name**: ").append(layer.getUiName()).append("\n");
                schema.append("- **Type**: ").append(layer.getType()).append("\n");

                var features = schemaService.listSupportedFeatures(layer).stream() //
                        .filter(AnnotationFeature::isEnabled) //
                        .toList();

                if (!features.isEmpty()) {
                    schema.append("- **Features**:\n");
                    for (var feature : features) {
                        schema.append("  - `").append(feature.getName()).append("`");
                        schema.append(" (").append(feature.getType()).append(")");

                        var tagset = feature.getTagset();
                        if (tagset != null) {
                            var tags = schemaService.listTags(tagset);
                            if (!tags.isEmpty()) {
                                schema.append(" - Valid values: ");
                                schema.append(tags.stream() //
                                        .map(tag -> "`" + tag.getName() + "`") //
                                        .reduce((a, b) -> a + ", " + b) //
                                        .orElse(""));
                            }
                        }
                        schema.append("\n");
                    }
                }
                schema.append("\n");
            }
            prompts.add(schema.toString());
        }

        return prompts;
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
            @ToolParam(value = "suggestions", description = //
            "List of span suggestions with the fields `text`, `left` (for left context), " //
                    + "`right` (for right context) and `label`. " //
                    + "Use the context only for ambiguous entites, otherwise leave it empty.") //
            List<MatchSpec> aSuggestions)
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
        if (predecessor != null) {
            predictions.inheritSuggestions(predecessor.getPredictionsByDocument(document.getId()));
        }

        String documentText;
        try (var session = CasStorageSession.openNested()) {
            var cas = documentService.createOrReadInitialCas(document, AUTO_CAS_UPGRADE,
                    SHARED_READ_ONLY_ACCESS);
            documentText = cas.getDocumentText();
        }

        // Build new suggestions
        var newSuggestions = new ArrayList<AnnotationSuggestion>();
        for (var spec : aSuggestions) {
            var ranges = findTextWithStepwiseContext(documentText, spec, 3);

            for (var range : ranges) {
                newSuggestions.add(SpanSuggestion.builder() //
                        .withId(NEW_ID) //
                        .withGeneration(predictions.getGeneration()) //
                        .withRecommender(recommender) //
                        .withDocument(document) //
                        .withPosition(new Offset(range.getBegin(), range.getEnd())) //
                        .withLabel(spec.label()) //
                        .build());
            }
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

    /**
     * Finds text using stepwise context expansion.
     * 
     * @param aDocument
     *            The full text.
     * @param aMatch
     *            The span information.
     * @param aStepSize
     *            Number of characters to expand context by in each iteration (e.g., 3).
     * @return List of matching ranges.
     */
    static List<Range> findTextWithStepwiseContext(String aDocument, MatchSpec aMatch,
            int aStepSize)
    {
        var text = aMatch.text();

        // 1. Input Validation
        if (aDocument == null || text == null || text.isEmpty()) {
            return emptyList();
        }

        var leftContext = defaultIfNull(aMatch.before(), "");
        var rightContext = defaultIfNull(aMatch.after(), "");
        var stepSize = Math.max(aStepSize, 1);

        // 2. Find all initial raw candidates
        var candidates = new ArrayList<Integer>();
        int index = aDocument.indexOf(text);
        while (index >= 0) {
            candidates.add(index);
            index = aDocument.indexOf(text, index + 1);
        }

        if (candidates.isEmpty()) {
            return emptyList();
        }
        if (candidates.size() == 1) {
            return toRanges(candidates, text.length());
        }

        // 3. Stepwise Disambiguation Loop
        int maxLen = Math.max(leftContext.length(), rightContext.length());
        int currentLen = stepSize;

        // Loop while we have >1 candidate AND we haven't exhausted matchable context
        // We use a 'do-while' logic or check 'currentLen' logic.
        // We must ensure we run at least once if there is context,
        // and we continue until context is exhausted or candidates == 1.

        while (candidates.size() > 1) {

            // Check if we have exceeded both contexts.
            // However, we must allow the loop to run even if currentLen > maxLen
            // IF the previous iteration was < maxLen (to catch the final tail).
            // A simpler check: if the previous iteration already used the full context, stop.
            // Here, we simply cap matching at the string length.

            boolean contextExhausted = (currentLen - stepSize) >= maxLen;
            if (contextExhausted) {
                break;
            }

            // Determine partial context strings
            // Left context: We want the *end* of the string (immediate left).
            // e.g., if Left="Hello World", len=3, we want "rld".
            String partialLeft = "";
            if (!leftContext.isEmpty()) {
                int startIdx = Math.max(0, leftContext.length() - currentLen);
                partialLeft = leftContext.substring(startIdx);
            }

            // Right context: We want the *start* of the string (immediate right).
            // e.g., if Right="Hello World", len=3, we want "Hel".
            String partialRight = "";
            if (!rightContext.isEmpty()) {
                int endIdx = Math.min(rightContext.length(), currentLen);
                partialRight = rightContext.substring(0, endIdx);
            }

            // Filter candidates
            var nextCandidates = new ArrayList<Integer>();
            for (int start : candidates) {
                if (matchesContext(aDocument, start, text.length(), partialLeft, partialRight)) {
                    nextCandidates.add(start);
                }
            }

            // If filtering killed all matches, it means the provided context
            // doesn't match the document. We usually return the last valid set (candidates)
            // or empty. Strict logic implies empty.
            if (nextCandidates.isEmpty()) {
                return emptyList();
            }

            candidates = nextCandidates;
            currentLen += stepSize;
        }

        return toRanges(candidates, text.length());
    }

    static boolean matchesContext(String aText, int aStart, int aLength, String aLeft,
            String aRight)
    {
        // Check Left
        if (!aLeft.isEmpty()) {
            int docLeftStart = aStart - aLeft.length();
            if (docLeftStart < 0) {
                return false; // Out of bounds
            }
            if (!aText.substring(docLeftStart, aStart).equals(aLeft)) {
                return false;
            }
        }

        // Check Right
        if (!aRight.isEmpty()) {
            int docRightStart = aStart + aLength;
            int docRightEnd = docRightStart + aRight.length();
            if (docRightEnd > aText.length()) {
                return false; // Out of bounds
            }
            if (!aText.substring(docRightStart, docRightEnd).equals(aRight)) {
                return false;
            }
        }

        return true;
    }

    static List<Range> toRanges(List<Integer> aStarts, int aLen)
    {
        var list = new ArrayList<Range>();
        for (int s : aStarts) {
            list.add(new Range(s, s + aLen));
        }
        return list;
    }
}
