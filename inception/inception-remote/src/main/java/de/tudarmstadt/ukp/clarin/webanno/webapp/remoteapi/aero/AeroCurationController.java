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
import static de.tudarmstadt.ukp.inception.remoteapi.SourceDocumentStateUtils.parseSourceDocumentState;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.IllegalObjectStateException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RAnnotation;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.remoteapi.Controller_ImplBase;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RemoteApiAutoConfiguration#aeroCurationController}.
 * </p>
 */
@Tag(name = "Curated Annotations Management", description = "Management of curated documents.")
@ConditionalOnExpression("false") // Auto-configured - avoid package scanning
@Controller
@RequestMapping(AeroCurationController.API_BASE)
public class AeroCurationController
    extends Controller_ImplBase
{
    private final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired CurationDocumentService curationService;

    @Operation(summary = "Create curation for a document in a project")
    @PostMapping(//
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + //
                    "/{" + PARAM_DOCUMENT_ID + "}/" + CURATION, //
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
            @RequestPart(value = PARAM_CONTENT) //
            @Schema(description = """
                    The file to upload.
                    """) //
            MultipartFile aFile, //
            @RequestParam(PARAM_FORMAT) //
            @Schema(description = """
                    Format of the file being uploaded.

                    Valid values typically include (unless disabled by the administrator):

                    - `text`: Plain text format (UTF-8).
                    - `xmi`: UIMA CAS XMI (XML 1.0) format.
                    - `jsoncas`: UIMA CAS JSON 0.4.0 format.

                    Additional format identifiers can be found in the format section of the user's guide.
                    """) //
            Optional<String> aFormat, //
            @RequestParam(PARAM_STATE) //
            @Schema(description = """
                    The document-level state that should be set.
                    """, //
                    allowableValues = { "CURATION-COMPLETE", "CURATION-IN-PROGRESS" }) //
            Optional<String> aState, //
            UriComponentsBuilder aUcb)
        throws Exception
    {
        var project = getProject(aProjectId);
        var document = getDocument(project, aDocumentId);

        var annotationCas = createCompatibleCas(aProjectId, aDocumentId, aFile, aFormat);

        // If they are compatible, then we can store the new annotations
        curationService.writeCurationCas(annotationCas, document, false);

        var resultState = AnnotationDocumentState.IN_PROGRESS;
        if (aState.isPresent()) {
            var state = parseSourceDocumentState(aState.get());
            switch (state) {
            case CURATION_IN_PROGRESS:
                resultState = AnnotationDocumentState.IN_PROGRESS;
                document.setState(state);
                documentService.createSourceDocument(document);
                break;
            case CURATION_FINISHED:
                resultState = AnnotationDocumentState.FINISHED;
                document.setState(state);
                documentService.createSourceDocument(document);
                break;
            case NEW: // fallthrough
            case ANNOTATION_IN_PROGRESS: // fallthrough
            case ANNOTATION_FINISHED: // fallthrough
            default:
                throw new IllegalObjectStateException(
                        "State [%s] not valid when uploading a curation.", aState.get());
            }
        }
        else {
            document.setState(SourceDocumentState.CURATION_IN_PROGRESS);
            documentService.createSourceDocument(document);
        }

        var response = new RResponse<>(
                new RAnnotation(WebAnnoConst.CURATION_USER, resultState, new Date()));
        return ResponseEntity.created(
                aUcb.path(API_BASE + "/" + PROJECTS + "/{pid}/" + DOCUMENTS + "/{did}/" + CURATION)
                        .buildAndExpand(project.getId(), document.getId()).toUri())
                .body(response);
    }

    @Operation(summary = "Get curated annotations of a document in a project")
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = byte.class))))
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}/" + CURATION, //
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
            @RequestParam(value = PARAM_FORMAT) //
            @Schema(description = """
                    The export format.
                    Valid values typically include (unless disabled by the administrator):

                    - `text`: Plain text format (UTF-8).
                    - `xmi`: UIMA CAS XMI (XML 1.0) format.
                    - `jsoncas`: UIMA CAS JSON 0.4.0 format.

                    Additional format identifiers can be found in the format section of the user's guide.
                    """) //
            Optional<String> aFormat)
        throws Exception
    {
        return readAnnotation(aProjectId, aDocumentId, WebAnnoConst.CURATION_USER, Mode.CURATION,
                aFormat);
    }

    @Operation(summary = "Delete a user's annotations of one document from a project")
    @DeleteMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}/" + CURATION, //
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
            long aDocumentId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var doc = getDocument(project, aDocumentId);
        curationService.deleteCurationCas(doc);

        // If we delete the curation, it cannot be any longer in-progress or finished. The best
        // guess is that we set the state back to annotation-in-progress.
        switch (doc.getState()) {
        case CURATION_IN_PROGRESS: // Fall-through
        case CURATION_FINISHED:
            doc.setState(SourceDocumentState.ANNOTATION_IN_PROGRESS);
            documentService.createSourceDocument(doc);
            break;
        default:
            // Nothing to do
        }

        return ResponseEntity.ok(new RResponse<>(INFO, "Curated annotations for document ["
                + aDocumentId + "] deleted from project [" + aProjectId + "]."));
    }
}
