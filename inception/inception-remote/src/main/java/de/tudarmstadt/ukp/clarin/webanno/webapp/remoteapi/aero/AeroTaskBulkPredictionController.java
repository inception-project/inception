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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero;

import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_REQUIRED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.Serializable;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RBulkPredictionRequest;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RTaskState;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.inception.processing.recommender.BulkPredictionTask;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.TaskAccess;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RemoteApiAutoConfiguration#aeroTaskController}.
 * </p>
 */
@Tag(name = "Task Management (non-AERO)", description = "Management of long-runnig tasks.")
@ConditionalOnExpression("false") // Auto-configured - avoid package scanning
@Controller
@RequestMapping(AeroTaskBulkPredictionController.API_BASE)
public class AeroTaskBulkPredictionController
    extends AeroController_ImplBase
{
    private @Autowired RecommendationService recommendationService;
    private @Autowired AnnotationSchemaService schemaService;
    private @Autowired SchedulingService schedulingService;
    private @Autowired TaskAccess taskAccess;

    @Operation(summary = "Submit prediction task")
    @PostMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + TASKS + "/predict", //
            consumes = APPLICATION_JSON_VALUE, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RTaskState>> create(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId, //
            @RequestBody RBulkPredictionRequest aRequest)
        throws Exception
    {
        var project = getProject(aProjectId);
        var sessionOwner = getSessionOwner();

        taskAccess.assertCanManageTasks(sessionOwner, project);

        var recommender = recommendationService.getRecommender(project, aRequest.recommender())
                .orElseThrow(() -> new IllegalArgumentException("Recommender ["
                        + aRequest.recommender() + "] not found in project [" + aProjectId + "]"));

        assertRecommenderCanBeTriggered(aProjectId, aRequest, recommender);

        var metadata = convertMetadata(aProjectId, aRequest, project);

        var task = BulkPredictionTask.builder() //
                .withSessionOwner(sessionOwner) //
                .withRecommender(recommender) //
                .withTrigger("Remote API") //
                .withDataOwner(aRequest.userId()) //
                .withProcessingMetadata(metadata) //
                .withStatesToProcess(aRequest.statesToProcess()) //
                .withFinishDocumentsWithoutRecommendations(
                        aRequest.finishDocumentsWithoutRecommendations()) //
                .build();
        schedulingService.enqueue(task);

        return ResponseEntity.ok(new RResponse<>(new RTaskState(task)));
    }

    private void assertRecommenderCanBeTriggered(long aProjectId, RBulkPredictionRequest aRequest,
            Recommender recommender)
    {
        if (!recommender.isEnabled()) {
            throw new IllegalArgumentException("Recommender [" + aRequest.recommender()
                    + "] in project [" + aProjectId + "] is not enabled.");
        }

        var factory = recommendationService.getRecommenderFactory(recommender)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recommender [" + aRequest.recommender() + "] is not supported"));

        var engine = factory.build(recommender);
        if (engine.getTrainingCapability() == TRAINING_REQUIRED) {
            // If a recommender requires training, it would yield no results if the user has not yet
            // annotated any documents. So in this case, we do currently not offer it for
            // processing.
            // It could be considered to also offer such recommenders for cases where there user has
            // already marked some documents as finished so that the remaining documents could be
            // annotated based on the training data from the finished documents...
            //
            // see
            // de.tudarmstadt.ukp.inception.processing.recommender.BulkRecommenderPanel.listRecommenders()
            throw new IllegalArgumentException("Recommender [" + aRequest.recommender()
                    + "] requires training and cannot be triggered in this way");
        }
    }

    private HashMap<AnnotationFeature, Serializable> convertMetadata(long aProjectId,
            RBulkPredictionRequest aRequest, Project project)
    {
        var metadata = new HashMap<AnnotationFeature, Serializable>();
        if (aRequest.metadata() != null) {
            for (var annotation : aRequest.metadata()) {
                var layer = schemaService.findLayer(project, annotation.type());
                if (layer == null) {
                    throw new IllegalArgumentException("Layer [" + annotation.type()
                            + "] not found in project [" + aProjectId + "]");
                }

                for (var featureValueEntry : annotation.features().entrySet()) {
                    var feature = schemaService.getFeature(featureValueEntry.getKey(), layer);
                    if (feature == null) {
                        throw new IllegalArgumentException("Feature [" + featureValueEntry.getKey()
                                + "] not found in layer [" + annotation.type() + "]");
                    }

                    metadata.put(feature, featureValueEntry.getValue());
                }
            }
        }
        return metadata;
    }
}
