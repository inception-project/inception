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

import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RMessageLevel.INFO;
import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.ANNOTATION_STATE_COMPLETE;
import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.ANNOTATION_STATE_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.ANNOTATION_STATE_LOCKED;
import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.ANNOTATION_STATE_NEW;
import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.annotationDocumentStateToString;
import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.parseAnnotationDocumentState;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RAnnotation;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.inception.remoteapi.Controller_ImplBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping(AeroAnnotationController.API_BASE)
public class AeroAnnotationController
    extends Controller_ImplBase
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Operation(summary = "List annotations of a document in a project")
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<List<RAnnotation>>> list( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId, //
            @PathVariable(PARAM_DOCUMENT_ID) //
            @Schema(description = """
                    Document identifier.
                    """) //
            long aDocumentId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var doc = getDocument(project, aDocumentId);

        var annotations = documentService.listAnnotationDocuments(doc);

        var annotationList = new ArrayList<RAnnotation>();
        for (var annotation : annotations) {
            annotationList.add(new RAnnotation(annotation));
        }

        return ResponseEntity.ok(new RResponse<>(annotationList));
    }

    @Operation(summary = "Create or update annotations for a document in a project")
    @PostMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{" //
                    + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS + "/{" + PARAM_ANNOTATOR_ID + "}", //
            consumes = MULTIPART_FORM_DATA_VALUE, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RAnnotation>> create( //
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
            @RequestParam(PARAM_FORMAT) //
            @Schema(description = """
                    The document format.
                    Valid values typically include (unless disabled by the administrator):

                    - `text`: Plain text format (UTF-8).
                    - `xmi`: UIMA CAS XMI (XML 1.0) format.
                    - `jsoncas`: UIMA CAS JSON 0.4.0 format.

                    Additional format identifiers can be found in the format section of the user's guide.
                    """) //
            Optional<String> aFormat, //
            @RequestParam(PARAM_STATE) //
            @Schema(description = """
                    The annotator-level state that should be set.
                    """, //
                    allowableValues = { //
                            ANNOTATION_STATE_NEW, //
                            ANNOTATION_STATE_IN_PROGRESS, //
                            ANNOTATION_STATE_COMPLETE, //
                            ANNOTATION_STATE_LOCKED }) //
            Optional<String> aState, //
            @RequestPart(PARAM_CONTENT) //
            MultipartFile aFile, //
            UriComponentsBuilder aUcb)
        throws Exception
    {
        var annotator = getUser(aAnnotatorId);
        var project = getProject(aProjectId);
        var document = getDocument(project, aDocumentId);
        var anno = getAnnotation(document, aAnnotatorId, true);

        var annotationCas = createCompatibleCas(aProjectId, aDocumentId, aFile, aFormat);

        // If they are compatible, then we can store the new annotations
        documentService.writeAnnotationCas(annotationCas, document, annotator);

        // Set state if one was provided
        if (aState.isPresent()) {
            anno.setState(parseAnnotationDocumentState(aState.get()));
            documentService.createOrUpdateAnnotationDocument(anno);
        }

        var response = new RResponse<>(new RAnnotation(anno));

        if (aState.isPresent()) {
            response.addMessage(INFO,
                    "State of annotations of user [" + aAnnotatorId + "] on document ["
                            + document.getId() + "] set to ["
                            + annotationDocumentStateToString(anno.getState()) + "]");
        }

        return ResponseEntity.created(aUcb
                .path(API_BASE + "/" + PROJECTS + "/{pid}/" + DOCUMENTS + "/{did}/" + ANNOTATIONS
                        + "/{aid}")
                .buildAndExpand(project.getId(), document.getId(), annotator.getUsername()).toUri())
                .body(response);
    }

    @Operation(summary = "Get annotations of a document in a project")
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = byte.class))))
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS + "/{" + PARAM_ANNOTATOR_ID + "}", //
            produces = { APPLICATION_OCTET_STREAM_VALUE, APPLICATION_JSON_VALUE })
    public ResponseEntity<byte[]> read( //
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
            @RequestParam(PARAM_FORMAT) //
            @Schema(description = """
                    The document format.
                    Valid values typically include (unless disabled by the administrator):

                    - `text`: Plain text format (UTF-8).
                    - `xmi`: UIMA CAS XMI (XML 1.0) format.
                    - `jsoncas`: UIMA CAS JSON 0.4.0 format.

                    Additional format identifiers can be found in the format section of the user's guide.
                    """) //
            Optional<String> aFormat)
        throws Exception
    {
        return readAnnotation(aProjectId, aDocumentId, aAnnotatorId, Mode.ANNOTATION, aFormat);
    }

    @Operation(summary = "Delete a user's annotations of one document from a project")
    @DeleteMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}/" + ANNOTATIONS + "/{" + PARAM_ANNOTATOR_ID + "}", //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<Void>> delete( //
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
            String aAnnotatorId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var doc = getDocument(project, aDocumentId);
        var anno = getAnnotation(doc, aAnnotatorId, false);
        documentService.removeAnnotationDocument(anno);
        documentService.deleteAnnotationCas(anno);

        return ResponseEntity
                .ok(new RResponse<>(INFO, "Annotations of user [" + aAnnotatorId + "] on document ["
                        + aDocumentId + "] deleted from project [" + aProjectId + "]."));
    }
}
