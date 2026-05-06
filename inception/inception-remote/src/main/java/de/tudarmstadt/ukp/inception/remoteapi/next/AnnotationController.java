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
package de.tudarmstadt.ukp.inception.remoteapi.next;

import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RMessageLevel.INFO;
import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.ANNOTATION_STATE_COMPLETE;
import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.ANNOTATION_STATE_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.ANNOTATION_STATE_LOCKED;
import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.ANNOTATION_STATE_NEW;
import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.annotationDocumentStateToString;
import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.parseAnnotationDocumentState;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RAnnotation;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.inception.remoteapi.Controller_ImplBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RemoteApiAutoConfiguration#aeroAnnotationController}.
 * </p>
 */
@Tag(name = "Annotation Management", description = "Management of annotated documents.")
@ConditionalOnExpression("false") // Auto-configured - avoid package scanning
@Controller
@RequestMapping(AnnotationController.API_BASE)
public class AnnotationController
    extends Controller_ImplBase
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Operation(summary = "Update annotation state for a document in a project (non-AERO)")
    @PostMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{" //
                    + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS + "/{" + PARAM_ANNOTATOR_ID + "}/"
                    + STATE, //
            consumes = ALL_VALUE, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RAnnotation>> updateState( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId, //
            @PathVariable(PARAM_DOCUMENT_ID) //
            @Schema(description = """
                    Document identifier.
                    """) //
            long aDocumentId, //
            @PathVariable(PARAM_ANNOTATOR_ID) //
            @Schema(description = """
                    Username of the annotator.
                    """) //
            String aAnnotatorId, //
            @RequestParam(name = PARAM_STATE) //
            @Schema(description = """
                    New annotation state.
                    """, //
                    allowableValues = { //
                            ANNOTATION_STATE_NEW, //
                            ANNOTATION_STATE_IN_PROGRESS, //
                            ANNOTATION_STATE_COMPLETE, //
                            ANNOTATION_STATE_LOCKED }) //
            String aState)
        throws Exception
    {
        var project = getProject(aProjectId);
        var document = getDocument(project, aDocumentId);

        var anno = getAnnotation(document, aAnnotatorId, false);
        documentService.setAnnotationDocumentState(anno, parseAnnotationDocumentState(aState));
        documentService.createOrUpdateAnnotationDocument(anno);

        var response = new RResponse<>(new RAnnotation(anno));
        response.addMessage(INFO,
                "State of annotations of user [" + aAnnotatorId + "] on document ["
                        + document.getId() + "] set to ["
                        + annotationDocumentStateToString(anno.getState()) + "]");

        return ResponseEntity.ok(response);
    }
}
