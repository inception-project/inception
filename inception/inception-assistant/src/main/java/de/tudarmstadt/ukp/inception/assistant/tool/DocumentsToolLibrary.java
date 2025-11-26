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
import static java.lang.String.join;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse;
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse.Builder;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationEditorContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.Tool;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolLibrary;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolParam;

public class DocumentsToolLibrary
    implements ToolLibrary
{
    private static final String LIST_DOCUMENTS_TOOL_DESCRIPTION = """
            Provides a list of the documents in the project.
            """;

    private static final String READ_DOCUMENT_TOOL_DESCRIPTION = """
            Allows to read a document from the project.
            """;

    private static final String PARAM_DOCUMENT_DESCRIPTION = """
            Name of the document to read.
            """;

    private static final String PARAM_START_DESCRIPTION = """
            Number of the first line to read (starting with 0).
            """;

    private static final String PARAM_LINES_DESCRIPTION = """
            How many lines to read.
            """;

    private final DocumentService documentService;
    private final UserDao userService;

    public DocumentsToolLibrary(DocumentService aDocumentService, UserDao aUserService)
    {
        documentService = aDocumentService;
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
            value = "list_documents", //
            actor = "List documents", //
            description = LIST_DOCUMENTS_TOOL_DESCRIPTION)
    public MCallResponse.Builder<Map<String, List<String>>> listDocuments( //
            AnnotationEditorContext aContext)
        throws IOException
    {
        var project = aContext.getProject();
        var sessionOwner = userService.get(aContext.getSessionOwner());
        var documents = documentService.listAnnotatableDocuments(project, sessionOwner);

        var payload = documents.keySet().stream() //
                .map(SourceDocument::getName) //
                .sorted() //
                .toList();

        Builder<Map<String, List<String>>> callResponse = MCallResponse.builder();
        callResponse.withPayload(Map.of("documents", payload));
        return callResponse;
    }

    @Tool( //
            value = "read_document", //
            actor = "Read document", //
            description = READ_DOCUMENT_TOOL_DESCRIPTION)
    public MCallResponse.Builder<String> readDocument( //
            AnnotationEditorContext aContext,
            @ToolParam(value = "document", description = PARAM_DOCUMENT_DESCRIPTION) String aDocument,
            @ToolParam(value = "from", description = PARAM_START_DESCRIPTION) int aStart,
            @ToolParam(value = "count", description = PARAM_LINES_DESCRIPTION) int aLines)
        throws IOException
    {
        if (aStart < 0) {
            return MCallResponse.builder(String.class)
                    .withPayload("Error: The 'from' parameter (start line) must be >= 0.");
        }

        if (aLines <= 0) {
            return MCallResponse.builder(String.class)
                    .withPayload("Error: The 'count' parameter (number of lines) must be > 0.");
        }

        var project = aContext.getProject();
        var sessionOwner = userService.get(aContext.getSessionOwner());
        var documents = documentService.listAnnotatableDocuments(project, sessionOwner);

        var document = documents.keySet().stream().filter(d -> d.getName().equals(aDocument))
                .findFirst();

        // Safeguard to ensure the session owner has access to the document
        if (document.isEmpty()) {
            return MCallResponse.builder(String.class).withPayload(
                    "Error: Document '" + aDocument + "' does not exist in the project.");
        }

        try (var session = CasStorageSession.openNested()) {
            var cas = documentService.createOrReadInitialCas(document.get(), AUTO_CAS_UPGRADE,
                    SHARED_READ_ONLY_ACCESS);
            var lines = cas.getDocumentText().split("\\r?\\n|\\r");
            lines = ArrayUtils.subarray(lines, aStart, aStart + aLines);
            return MCallResponse.builder(String.class).withPayload(join("\n", lines));
        }
    }
}
