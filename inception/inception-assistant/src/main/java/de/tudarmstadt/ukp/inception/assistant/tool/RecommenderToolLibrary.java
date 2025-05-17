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

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolStatusResponse.error;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolStatusResponse.success;

import java.io.IOException;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationEditorContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.Tool;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolLibrary;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolParam;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolStatusResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptingMode;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ExtractionMode;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.tasks.PredictionTask;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;

public class RecommenderToolLibrary
    implements ToolLibrary
{
    private static final String RESULT_DESCRIPTION = """
            The entity suggestions are now being generated in the background.
            The results will not be given to you but directly to the user.
            The user can monitor the progress via the progress indicator in the page footer.
            After a short while, the refresh icon in the action bar above the document view will start to wiggle.
            The user can then display the suggestions by clicking on it.
            """;

    private static final String TOOL_DESCRIPTION = """
            Annotates entities or suggests entity annotations in the current document as a background process.
            Use this when the user asks you to annotate, suggest or identify things.
            The tool has access to the document currently open in the annotation editor.
            Returns a status message.""";

    private static final String PARAM_TASK_DESCRIPTION = "description of kind of entites that should be annotated";

    private final DocumentAccess documentAccess;
    private final RecommendationService recommendationService;
    private final UserDao userService;
    private final SchedulingService schedulingService;

    public RecommenderToolLibrary(RecommendationService aRecommendationService,
            UserDao aUserService, DocumentAccess aDocumentAccess,
            SchedulingService aSchedulingService)
    {
        recommendationService = aRecommendationService;
        userService = aUserService;
        documentAccess = aDocumentAccess;
        schedulingService = aSchedulingService;
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
            value = "annotate", //
            actor = "Recommender tool", //
            description = TOOL_DESCRIPTION)
    public ToolStatusResponse suggestAnnotations( //
            AnnotationEditorContext aContext, //
            @ToolParam(value = "task_description", description = PARAM_TASK_DESCRIPTION) //
            String aTaskDescription)
        throws IOException
    {
        var sessionOwner = userService.getCurrentUser();
        var dataOwner = aContext.getDataOwner();

        documentAccess.assertCanEditAnnotationDocument(sessionOwner, aContext.getDocument(),
                dataOwner);

        var maybeRecommender = recommendationService.listEnabledRecommenders(aContext.getProject())
                .stream() //
                .filter(rec -> recommendationService.getRecommenderFactory(rec) //
                        .map(factory -> factory.isInteractive(rec)
                                && factory.createTraits() instanceof LlmRecommenderTraits) //
                        .orElse(false)) //
                .findFirst();

        if (maybeRecommender.isEmpty()) {
            return error("There is no interactive recommender configured that can be used.");
        }

        var recommender = maybeRecommender.get();
        var factory = recommendationService.getRecommenderFactory(recommender).get();
        var traits = (LlmRecommenderTraits) factory.readTraits(recommender);

        traits.setExtractionMode(ExtractionMode.MENTIONS_FROM_JSON);
        traits.setPromptingMode(PromptingMode.PER_SENTENCE);
        traits.setPrompt(aTaskDescription);

        factory.writeTraits(recommender, traits);

        var predictionTask = PredictionTask.builder() //
                .withSessionOwner(sessionOwner) //
                .withTrigger("Interactive recommender") //
                .withCurrentDocument(aContext.getDocument()) //
                .withDataOwner(dataOwner) //
                .withRecommender(recommender) //
                .build(); //

        schedulingService.enqueue(predictionTask);

        return success(RESULT_DESCRIPTION);
    }
}
