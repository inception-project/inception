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
package de.tudarmstadt.ukp.inception.externaleditor;

import java.io.IOException;
import java.util.Optional;

import org.xml.sax.ContentHandler;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.externaleditor.policy.DefaultHtmlDocumentPolicy;
import de.tudarmstadt.ukp.inception.externaleditor.policy.SafetyNetDocumentPolicy;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.SanitizingContentHandler;

public abstract class XmlDocumentViewControllerImplBase
{
    private final DefaultHtmlDocumentPolicy defaultPolicy;
    private final SafetyNetDocumentPolicy safetyNetPolicy;
    private final DocumentImportExportService formatRegistry;
    private final AnnotationEditorRegistry annotationEditorRegistry;

    public XmlDocumentViewControllerImplBase(DefaultHtmlDocumentPolicy aDefaultPolicy,
            SafetyNetDocumentPolicy aSafetyNetPolicy, DocumentImportExportService aFormatRegistry,
            AnnotationEditorRegistry aAnnotationEditorRegistry)
    {
        defaultPolicy = aDefaultPolicy;
        safetyNetPolicy = aSafetyNetPolicy;
        formatRegistry = aFormatRegistry;
        annotationEditorRegistry = aAnnotationEditorRegistry;
    }

    protected ContentHandler applySanitizers(Optional<String> aEditor, SourceDocument doc,
            ContentHandler aCh)
        throws IOException
    {
        // Apply safety net
        var ch = new SanitizingContentHandler(aCh, safetyNetPolicy.getPolicy());

        // Apply editor policy if it exists
        var editorPolicy = getEditorPolicy(aEditor);
        if (editorPolicy.isPresent()) {
            ch = new SanitizingContentHandler(ch, editorPolicy.get());
        }

        // Apply format policy if it exists
        var formatPolicy = formatRegistry.getFormatPolicy(doc);
        if (formatPolicy.isPresent()) {
            ch = new SanitizingContentHandler(ch, formatPolicy.get());
        }

        // If neither a format nor an editor policy exists, apply the default policy
        if (editorPolicy.isEmpty() && formatPolicy.isEmpty()) {
            ch = new SanitizingContentHandler(ch, defaultPolicy.getPolicy());
        }
        return ch;
    }

    private Optional<PolicyCollection> getEditorPolicy(Optional<String> aEditor) throws IOException
    {
        if (!aEditor.isPresent()) {
            return Optional.empty();
        }

        var factory = annotationEditorRegistry.getEditorFactory(aEditor.get());
        if (factory instanceof ExternalAnnotationEditorFactory) {
            return ((ExternalAnnotationEditorFactory) factory).getPolicy();
        }

        return Optional.empty();
    }
}
