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
import static java.lang.Character.isWhitespace;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse;
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
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.uima.Range;

public class AnnotationToolLibrary
    implements ToolLibrary
{
    private static final String CREATE_SPAN_SUGGESTIONS_DESCRIPTION = """
            Creates span annotation suggestions.
            Suggestions appear in the editor for the user to review and accept or reject. \
            Use this when the user asks you to annotate, mark, or identify entities, mentions, or spans of text.

            Omit the parameter `document` unless the user explicitly asks for a specific document.
            The parameter `layer` name must match one of the available annotation layers in the project schema.
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
    public MCallResponse.Builder<?> createSpanSuggestions( //
            AnnotationEditorContext aContext, //
            CommandDispatcher aCommandDispatcher, //
            @ToolParam(value = "document", description = "Name of the document to read (optional)") String aDocumentName,
            @ToolParam(value = "layer", description = "Name of the annotation layer") //
            String aLayerName, //
            @ToolParam(value = "suggestions", description = //
            "List of span suggestions with the fields `text`, `before` (for context before), " //
                    + "`after` (for context after) and `label`. " //
                    + "Use the context only is text is ambiguous, otherwise leave it empty.") //
            List<MatchSpec> aSuggestions)
        throws IOException
    {
        var sessionOwner = userService.getCurrentUser();
        var project = aContext.getProject();

        var document = aContext.getDocument();

        if (isNotBlank(aDocumentName)) {
            var documents = documentService.listAnnotatableDocuments(project, sessionOwner);
            var maybeDocument = documents.keySet().stream() //
                    .filter(d -> d.getName().equals(aDocumentName)) //
                    .findFirst();

            // Safeguard to ensure the session owner has access to the document
            if (maybeDocument.isEmpty()) {
                return MCallResponse.builder(String.class).withPayload(
                        "Error: Document [" + aDocumentName + "] does not exist in the project.");
            }

            document = maybeDocument.get();
        }

        // Find the layer
        var layer = schemaService.findLayer(project, aLayerName);
        if (layer == null) {
            return MCallResponse.builder(String.class)
                    .withPayload("Layer [" + aLayerName + "] not found in project");
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

        // Build new suggestions and track failures
        var newSuggestions = new ArrayList<AnnotationSuggestion>();
        var failedSpecs = new ArrayList<MatchSpec>();
        for (var spec : aSuggestions) {
            var ranges = findTextWithStepwiseContext(documentText, spec, 3);

            if (ranges.isEmpty()) {
                failedSpecs.add(spec);
            }

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

        // Build detailed status message
        var message = new StringBuilder();
        message.append("Processed ").append(aSuggestions.size()).append(" spec(s): ");
        message.append(newSuggestions.size()).append(" suggestion(s) created");

        if (!failedSpecs.isEmpty()) {
            message.append(", ").append(failedSpecs.size()).append(" spec(s) failed to match");
            message.append(".\n\nFailed specs (no matches found):");
            for (var spec : failedSpecs) {
                message.append("\n- text: \"").append(spec.text()).append("\"");
                if (!spec.before().isEmpty()) {
                    message.append(", before: \"").append(spec.before()).append("\"");
                }
                if (!spec.after().isEmpty()) {
                    message.append(", after: \"").append(spec.after()).append("\"");
                }
                message.append(", label: \"").append(spec.label()).append("\"");
            }
            message.append("\n\nNote: Specs may fail due to: word boundary violations, ")
                    .append("whitespace differences (normalized), missing punctuation in context, ")
                    .append("or text not present in document.");
        }

        return MCallResponse.builder(String.class) //
                .withActor("Annotated " + aSuggestions.size() + " spans")
                .withPayload(message.toString());
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

        // FIXME: This is not the greatest design. Probably we should allow the feature in
        // recommenders to be nullable. But let's look into that later.
        if (features.isEmpty()) {
            throw new IllegalStateException(
                    "Project must have at least one layer with one feature.");
        }

        var feature = features.get(0);
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

        // 2. Find all initial raw candidates with whitespace normalization and word boundaries
        var candidateRanges = new ArrayList<Range>();

        // Normalize the search text (collapse multiple spaces to single space)
        var normalizedText = text.replaceAll("\\s+", " ");

        // Search through the document
        int docIndex = 0;
        while (docIndex < aDocument.length()) {
            // Find potential match position
            int startPos = aDocument.indexOf(normalizedText.charAt(0), docIndex);
            if (startPos < 0) {
                break;
            }

            // Check if this could be a word boundary match
            // Must be at start of document or preceded by non-word character
            boolean validStart = startPos == 0
                    || !Character.isLetterOrDigit(aDocument.charAt(startPos - 1));

            if (validStart) {
                // Try to match the normalized text with whitespace normalization
                int textIdx = 0;
                int matchIdx = startPos;
                boolean matched = true;

                while (textIdx < normalizedText.length() && matchIdx < aDocument.length()) {
                    char textChar = normalizedText.charAt(textIdx);
                    char docChar = aDocument.charAt(matchIdx);

                    if (isWhitespace(textChar)) {
                        // Text expects whitespace - consume all whitespace in document
                        if (!isWhitespace(docChar)) {
                            matched = false;
                            break;
                        }
                        // Skip all consecutive whitespace in both text and document
                        while (textIdx < normalizedText.length()
                                && isWhitespace(normalizedText.charAt(textIdx))) {
                            textIdx++;
                        }
                        while (matchIdx < aDocument.length()
                                && isWhitespace(aDocument.charAt(matchIdx))) {
                            matchIdx++;
                        }
                    }
                    else {
                        // Regular character - must match exactly
                        if (textChar != docChar) {
                            matched = false;
                            break;
                        }
                        textIdx++;
                        matchIdx++;
                    }
                }

                if (matched && textIdx == normalizedText.length()) {
                    // Check word boundary at end
                    boolean validEnd = matchIdx >= aDocument.length()
                            || !Character.isLetterOrDigit(aDocument.charAt(matchIdx));

                    if (validEnd) {
                        candidateRanges.add(new Range(startPos, matchIdx));
                    }
                }
            }

            docIndex = startPos + 1;
        }

        if (candidateRanges.isEmpty()) {
            return emptyList();
        }

        // If no context provided, return all candidates
        if (leftContext.isEmpty() && rightContext.isEmpty()) {
            return candidateRanges;
        }

        // If we have context, we need to filter even if there's only one candidate
        // (that candidate might not match the context)

        // 3. Stepwise Disambiguation Loop
        int maxLen = Math.max(leftContext.length(), rightContext.length());
        int currentLen = stepSize;

        // Loop while we have candidates AND we haven't exhausted matchable context
        // Continue until all context has been checked or we've filtered down to final matches

        while (!candidateRanges.isEmpty()) {

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
            var nextCandidates = new ArrayList<Range>();
            for (Range range : candidateRanges) {
                if (matchesContext(aDocument, range.getBegin(), range.getEnd() - range.getBegin(),
                        partialLeft, partialRight)) {
                    nextCandidates.add(range);
                }
            }

            candidateRanges = nextCandidates;
            currentLen += stepSize;

            // If we've checked all the context, stop
            if (currentLen > maxLen && maxLen > 0) {
                break;
            }
        }

        return candidateRanges;
    }

    static boolean matchesContext(String aText, int aStart, int aLength, String aLeft,
            String aRight)
    {
        // Check Left Context
        if (!aLeft.isEmpty()) {
            // Normalize whitespace within the context (collapse consecutive whitespace to single
            // space)
            var normalizedLeft = aLeft.replaceAll("\\s+", " ").trim();

            // Skip any whitespace at the boundary between context and match
            int contextEnd = aStart;
            while (contextEnd > 0 && isWhitespace(aText.charAt(contextEnd - 1))) {
                contextEnd--;
            }

            // Find the actual length of context in the document
            int actualLeftLength = findActualContextLength(aText, contextEnd, normalizedLeft, true);
            if (actualLeftLength < 0) {
                return false; // Context doesn't match
            }
        }

        // Check Right Context
        if (!aRight.isEmpty()) {
            // Normalize whitespace within the context (collapse consecutive whitespace to single
            // space)
            var normalizedRight = aRight.replaceAll("\\s+", " ").trim();

            // Skip any whitespace at the boundary between match and context
            int contextStart = aStart + aLength;
            while (contextStart < aText.length() && isWhitespace(aText.charAt(contextStart))) {
                contextStart++;
            }

            // Find the actual length of context in the document
            int actualRightLength = findActualContextLength(aText, contextStart, normalizedRight,
                    false);
            if (actualRightLength < 0) {
                return false; // Context doesn't match
            }
        }

        return true;
    }

    /**
     * Finds the actual length in the document that corresponds to the normalized context string,
     * accounting for whitespace variations.
     * 
     * @param aText
     *            the document text
     * @param aPosition
     *            the position in the document (for left context: end position where context ends,
     *            for right context: start position where context begins)
     * @param aNormalizedContext
     *            the normalized context string to match
     * @param aIsLeftContext
     *            true if matching left context (backwards), false for right context (forwards)
     * @return the actual number of characters in the document that match the context, or -1 if no
     *         match
     */
    private static int findActualContextLength(String aText, int aPosition,
            String aNormalizedContext, boolean aIsLeftContext)
    {
        int textIdx = aIsLeftContext ? aNormalizedContext.length() - 1 : 0;
        int docIdx = aIsLeftContext ? aPosition - 1 : aPosition;
        int direction = aIsLeftContext ? -1 : 1;
        int startDocIdx = docIdx;

        while (textIdx >= 0 && textIdx < aNormalizedContext.length()) {
            if (docIdx < 0 || docIdx >= aText.length()) {
                return -1; // Out of bounds - no match
            }

            char textChar = aNormalizedContext.charAt(textIdx);
            char docChar = aText.charAt(docIdx);

            if (isWhitespace(textChar)) {
                // Context expects whitespace - consume all whitespace in document
                if (!isWhitespace(docChar)) {
                    return -1; // Expected whitespace but found non-whitespace
                }
                // Skip all consecutive whitespace in both context and document
                while (textIdx >= 0 && textIdx < aNormalizedContext.length()
                        && isWhitespace(aNormalizedContext.charAt(textIdx))) {
                    textIdx += direction;
                }
                while (docIdx >= 0 && docIdx < aText.length()
                        && isWhitespace(aText.charAt(docIdx))) {
                    docIdx += direction;
                }
            }
            else {
                // Regular character - must match exactly
                if (textChar != docChar) {
                    return -1; // Character mismatch
                }
                textIdx += direction;
                docIdx += direction;
            }
        }

        // Check if we consumed all of the context string
        boolean fullyMatched = aIsLeftContext ? textIdx < 0
                : textIdx >= aNormalizedContext.length();

        if (!fullyMatched) {
            return -1;
        }

        // Calculate actual length consumed in document
        return aIsLeftContext ? (startDocIdx - docIdx) : (docIdx - startDocIdx);
    }
}
