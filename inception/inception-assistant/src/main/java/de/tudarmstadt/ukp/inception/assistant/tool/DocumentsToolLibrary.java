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
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.join;
import static org.apache.commons.lang3.ArrayUtils.subarray;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.IOException;
import java.util.Map;

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
    private static final int READ_DOCUMENT_CHARACTER_LIMIT = 64_000;

    private static final String GET_CURRENT_DOCUMENT_TOOL_DESCRIPTION = """
            Get the name of the current document.
            """;

    private static final String LIST_DOCUMENTS_TOOL_DESCRIPTION = """
            List the documents in the project.
            """;

    private static final String READ_DOCUMENT_TOOL_DESCRIPTION = """
            Read lines from a document.
            You can read at most 200 lines at a time.

            Omit the parameter `document` unless the user explicitly asks for a specific document.
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
            value = "get_current_document", //
            actor = "Get current document", //
            description = GET_CURRENT_DOCUMENT_TOOL_DESCRIPTION)
    public MCallResponse.Builder<?> getCurrentDocument( //
            AnnotationEditorContext aContext)
        throws IOException
    {
        Builder<Map<String, Object>> callResponse = MCallResponse.builder();
        callResponse.withPayload(Map.of( //
                "document_name", aContext.getDocument().getName()));
        return callResponse;
    }

    @Tool( //
            value = "list_documents", //
            actor = "List documents", //
            description = LIST_DOCUMENTS_TOOL_DESCRIPTION)
    public MCallResponse.Builder<?> listDocuments( //
            AnnotationEditorContext aContext)
        throws IOException
    {
        var project = aContext.getProject();
        var sessionOwner = aContext.getSessionOwner();
        var documents = documentService.listAnnotatableDocuments(project, sessionOwner);

        var payload = documents.keySet().stream() //
                .map(SourceDocument::getName) //
                .sorted() //
                .toList();

        Builder<Map<String, Object>> callResponse = MCallResponse.builder();
        callResponse.withPayload(Map.of( //
                "current_document", aContext.getDocument().getName(), //
                "document_count", payload.size(), //
                "documents", payload));
        return callResponse;
    }

    @Tool( //
            value = "read_document", //
            actor = "Read document", //
            description = READ_DOCUMENT_TOOL_DESCRIPTION)
    public MCallResponse.Builder<String> readDocument( //
            AnnotationEditorContext aContext,
            @ToolParam(value = "document", description = "Name of the document to read (optional)") String aDocumentName,
            @ToolParam(value = "start_line", description = "First line to read (1-based)") int aStartLine,
            @ToolParam(value = "end_line", description = "Last line to read (1-based)") int aEndLine)
        throws IOException
    {
        if (aStartLine < 1) {
            return MCallResponse.builder(String.class)
                    .withPayload("Error: The 'start_line' parameter must be >= 1.");
        }

        if (aEndLine < 1) {
            return MCallResponse.builder(String.class)
                    .withPayload("Error: The 'end_line' parameter must be >= 1.");
        }

        var project = aContext.getProject();

        var startLine = min(aStartLine, aEndLine);
        var endLine = max(aStartLine, aEndLine);

        if (endLine - startLine + 1 > 200) {
            endLine = startLine + 200 - 1;
        }

        var sessionOwner = aContext.getSessionOwner();
        var documents = documentService.listAnnotatableDocuments(project, sessionOwner);

        var docName = defaultIfBlank(aDocumentName, aContext.getDocument().getName());

        var doc = documents.keySet().stream() //
                .filter(d -> d.getName().equals(docName)) //
                .findFirst();

        // Safeguard to ensure the session owner has access to the document
        if (doc.isEmpty()) {
            return MCallResponse.builder(String.class).withPayload(
                    "Error: Document [" + docName + "] does not exist in the project.");
        }

        try (var session = CasStorageSession.openNested()) {
            var cas = documentService.createOrReadInitialCas(doc.get(), AUTO_CAS_UPGRADE,
                    SHARED_READ_ONLY_ACCESS);
            var lines = cas.getDocumentText().split("\\r?\\n|\\r");
            var totalLines = lines.length;
            lines = subarray(lines, startLine - 1, endLine);
            var joinedLines = join("\n", lines);
            if (joinedLines.length() > READ_DOCUMENT_CHARACTER_LIMIT) {
                joinedLines = "ERROR: Selected lines exceed the limit of "
                        + READ_DOCUMENT_CHARACTER_LIMIT + " characters. Try reading fewer lines.";
            }
            return MCallResponse.builder(String.class) //
                    .withActor("Read " + docName + " (lines " + startLine + "-"
                            + (startLine + lines.length - 1) + " of " + totalLines + ")")
                    .withPayload("---\n" + //
                            "total_lines: " + totalLines + "\n" + //
                            "start_line: " + startLine + "\n" + //
                            "end_line: " + (startLine + lines.length - 1) + "\n" + //
                            "---\n" + //
                            joinedLines);
        }
    }
}
