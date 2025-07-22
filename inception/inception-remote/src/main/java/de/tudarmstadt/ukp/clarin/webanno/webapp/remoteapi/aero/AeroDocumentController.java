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
import static de.tudarmstadt.ukp.inception.remoteapi.SourceDocumentStateUtils.DOCUMENT_STATE_ANNOTATION_COMPLETE;
import static de.tudarmstadt.ukp.inception.remoteapi.SourceDocumentStateUtils.DOCUMENT_STATE_ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.remoteapi.SourceDocumentStateUtils.DOCUMENT_STATE_CURATION_COMPLETE;
import static de.tudarmstadt.ukp.inception.remoteapi.SourceDocumentStateUtils.DOCUMENT_STATE_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.remoteapi.SourceDocumentStateUtils.DOCUMENT_STATE_NEW;
import static de.tudarmstadt.ukp.inception.remoteapi.SourceDocumentStateUtils.parseSourceDocumentState;
import static de.tudarmstadt.ukp.inception.remoteapi.SourceDocumentStateUtils.sourceDocumentStateToString;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.IllegalNameException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.IllegalObjectStateException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.UnsupportedFormatException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
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
 * {@link RemoteApiAutoConfiguration#aeroDocumentController}.
 * </p>
 */
@Tag(name = "Document Management", description = "Management of documents.")
@ConditionalOnExpression("false") // Auto-configured - avoid package scanning
@Controller
@RequestMapping(AeroDocumentController.API_BASE)
public class AeroDocumentController
    extends Controller_ImplBase
{
    private final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired DocumentStorageService documentStorageService;

    @Operation(summary = "List documents in a project")
    @GetMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/"
            + DOCUMENTS, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<List<RDocument>>> list( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var documents = documentService.listSourceDocuments(project);

        var documentList = new ArrayList<RDocument>();
        for (SourceDocument document : documents) {
            documentList.add(new RDocument(document));
        }

        return ResponseEntity.ok(new RResponse<>(documentList));
    }

    @Operation(summary = "Create a new document in a project")
    @PostMapping(//
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS, //
            consumes = MULTIPART_FORM_DATA_VALUE, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RDocument>> create( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId, //
            @RequestParam(PARAM_NAME) //
            @Schema(description = """
                    Document name.
                    """) //
            String aName, //
            @RequestParam(PARAM_FORMAT) //
            @Schema(description = """
                    The document format.
                    Valid values typically include (unless disabled by the administrator):

                    - `text`: Plain text format (UTF-8).
                    - `xmi`: UIMA CAS XMI (XML 1.0) format.
                    - `jsoncas`: UIMA CAS JSON 0.4.0 format.

                    Additional format identifiers can be found in the format section of the user's guide.
                    """) //
            String aFormat, //
            @RequestParam(PARAM_STATE) //
            @Schema(description = """
                    The initial state for the imported document.
                    """, //
                    allowableValues = { DOCUMENT_STATE_NEW, DOCUMENT_STATE_ANNOTATION_IN_PROGRESS,
                            DOCUMENT_STATE_ANNOTATION_COMPLETE, DOCUMENT_STATE_CURATION_COMPLETE,
                            DOCUMENT_STATE_CURATION_IN_PROGRESS }) //
            Optional<String> aState, //
            @RequestParam(PARAM_CONTENT) //
            MultipartFile aFile, //
            UriComponentsBuilder aUcb)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        // Check if the format is supported
        if (!importExportService.getReadableFormatById(aFormat).isPresent()) {
            throw new UnsupportedFormatException(
                    "Format [%s] not supported. Acceptable formats are %s.", aFormat,
                    importExportService.getReadableFormats().stream().map(FormatSupport::getId)
                            .sorted().collect(Collectors.toList()));
        }

        if (!documentService.isValidDocumentName(aName)) {
            throw new IllegalNameException("Illegal document name [%s]", aName);
        }

        // Meta data entry to the database
        var document = new SourceDocument();
        document.setProject(project);
        document.setName(aName);
        document.setFormat(aFormat);

        // Set state if one was provided
        if (aState.isPresent()) {
            var state = parseSourceDocumentState(aState.get());
            switch (state) {
            case NEW: // fallthrough
            case ANNOTATION_IN_PROGRESS: // fallthrough
            case ANNOTATION_FINISHED: // fallthrough
                document.setState(state);
                documentService.createSourceDocument(document);
                break;
            case CURATION_IN_PROGRESS: // fallthrough
            case CURATION_FINISHED:
            default:
                throw new IllegalObjectStateException(
                        "State [%s] not valid when uploading a document. These states are "
                                + "automatically determined by the system",
                        aState.get());
            }
        }

        // Import source document to the project repository folder
        try (var is = aFile.getInputStream()) {
            documentService.uploadSourceDocument(is, document);
        }

        var rDocument = new RResponse<>(new RDocument(document));

        if (aState.isPresent()) {
            rDocument.addMessage(INFO, "State of document [" + document.getId() + "] set to ["
                    + sourceDocumentStateToString(document.getState()) + "]");
        }

        return ResponseEntity
                .created(aUcb.path(API_BASE + "/" + PROJECTS + "/{pid}/" + DOCUMENTS + "/{did}")
                        .buildAndExpand(project.getId(), document.getId()).toUri())
                .body(rDocument);
    }

    @Operation(summary = "Get a document from a project")
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = byte.class))))
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}", //
            produces = { APPLICATION_OCTET_STREAM_VALUE, APPLICATION_JSON_VALUE })
    public ResponseEntity<?> read( //
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
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var doc = getDocument(project, aDocumentId);

        boolean originalFile;
        String formatId;
        if (aFormat.isPresent()) {
            if (VAL_ORIGINAL.equals(aFormat.get())) {
                formatId = doc.getFormat();
                originalFile = true;
            }
            else {
                formatId = aFormat.get();
                originalFile = doc.getFormat().equals(formatId);
            }
        }
        else {
            formatId = doc.getFormat();
            originalFile = true;
        }

        if (originalFile) {
            // Export the original file - no temporary file created here, we export directly from
            // the file system
            var docFile = documentStorageService.getSourceDocumentFile(doc);
            var resource = new FileSystemResource(docFile);
            var httpHeaders = new HttpHeaders();
            httpHeaders.setContentLength(resource.contentLength());
            httpHeaders.set("Content-Disposition",
                    "attachment; filename=\"" + doc.getName() + "\"");
            return new ResponseEntity<org.springframework.core.io.Resource>(resource, httpHeaders,
                    OK);
        }

        // Export a converted file - here we first export to a local temporary file and then
        // send that back to the client

        // Check if the format is supported
        var format = importExportService.getWritableFormatById(formatId)
                .orElseThrow(() -> new UnsupportedFormatException(
                        "Format [%s] cannot be exported. Exportable formats are %s.", formatId,
                        importExportService.getWritableFormats().stream().map(FormatSupport::getId)
                                .sorted().collect(Collectors.toList()).toString()));

        // Create a temporary export file from the annotations
        var cas = documentService.createOrReadInitialCas(doc);

        File exportedFile = null;
        try {
            // Load the converted file into memory
            exportedFile = importExportService.exportCasToFile(cas, doc, INITIAL_CAS_PSEUDO_USER,
                    format);
            var resource = FileUtils.readFileToByteArray(exportedFile);

            // Send it back to the client
            var httpHeaders = new HttpHeaders();
            httpHeaders.setContentLength(resource.length);
            httpHeaders.set("Content-Disposition",
                    "attachment; filename=\"" + exportedFile.getName() + "\"");

            return new ResponseEntity<>(resource, httpHeaders, OK);
        }
        finally {
            if (exportedFile != null) {
                FileUtils.forceDelete(exportedFile);
            }
        }
    }

    @Operation(summary = "Delete a document from a project")
    @DeleteMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + DOCUMENTS + "/{"
                    + PARAM_DOCUMENT_ID + "}", //
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
        documentService.removeSourceDocument(doc);

        return ResponseEntity.ok(new RResponse<>(INFO,
                "Document [" + aDocumentId + "] deleted from project [" + aProjectId + "]."));
    }
}
