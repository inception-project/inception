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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
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
import org.springframework.web.bind.annotation.RequestParam;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RBulkPredictionRequest;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RTaskState;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.inception.processing.recommender.BulkPredictionTask;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RemoteApiAutoConfiguration#aeroTaskController}.
 * </p>
 */
@Tag(name = "Task Management", description = "Management of long-runnig tasks.")
@ConditionalOnExpression("false") // Auto-configured - avoid package scanning
@Controller
@RequestMapping(AeroTaskController.API_BASE)
public class AeroTaskController
    extends AeroController_ImplBase
{
    private @Autowired RecommendationService recommendationService;
    private @Autowired AnnotationSchemaService schemaService;
    private @Autowired SchedulingService schedulingService;

    @Operation(summary = "Submit a task (non-AERO)")
    @PostMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + TASKS, //
            consumes = APPLICATION_JSON_VALUE, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RTaskState>> create(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId, //
            @RequestParam(PARAM_TASK) String aTask, //
            @RequestBody String aDetailsJson)
        throws Exception
    {
        var project = getProject(aProjectId);

        // Check if the user has permission to submit jobs
        var sessionOwner = getSessionOwner();
        assertPermission("User [" + sessionOwner.getUsername() + "] is not allowed to submit jobs",
                projectService.hasRole(sessionOwner, project, MANAGER)
                        || userRepository.isAdministrator(sessionOwner));

        if (!aTask.equals("BULK_PREDICTION")) {
            throw new IllegalArgumentException("Unknown task type [" + aTask + "]");
        }

        var request = JSONUtil.fromJsonString(RBulkPredictionRequest.class, aDetailsJson);

        var recommender = recommendationService.getRecommender(project, request.recommender());
        if (!recommender.isPresent()) {
            throw new IllegalArgumentException("Recommender [" + request.recommender()
                    + "] not found in project [" + aProjectId + "]");
        }

        var metadata = new HashMap<AnnotationFeature, Serializable>();
        if (request.metadata() != null) {
            for (var layerEntry : request.metadata().entrySet()) {
                var layer = schemaService.findLayer(project, layerEntry.getKey());
                if (layer == null) {
                    throw new IllegalArgumentException("Layer [" + layerEntry.getKey()
                            + "] not found in project [" + aProjectId + "]");
                }

                var featureValues = layerEntry.getValue();
                for (var featureValueEntry : featureValues.entrySet()) {
                    var feature = schemaService.getFeature(featureValueEntry.getKey(), layer);
                    if (feature == null) {
                        throw new IllegalArgumentException("Feature [" + featureValueEntry.getKey()
                                + "] not found in layer [" + layerEntry.getKey() + "]");
                    }

                    metadata.put(feature, featureValueEntry.getValue());
                }
            }
        }

        var task = BulkPredictionTask.builder() //
                .withSessionOwner(sessionOwner) //
                .withRecommender(recommender.get()) //
                .withTrigger("Remote API") //
                .withDataOwner(request.userId()) //
                .withProcessingMetadata(metadata) //
                .withStatesToProcess(request.statesToProcess()) //
                .withFinishDocumentsWithoutRecommendations(
                        request.finishDocumentsWithoutRecommendations()) //
                .build();
        schedulingService.enqueue(task);

        return ResponseEntity.ok(new RResponse<>(new RTaskState(task)));
    }
}
