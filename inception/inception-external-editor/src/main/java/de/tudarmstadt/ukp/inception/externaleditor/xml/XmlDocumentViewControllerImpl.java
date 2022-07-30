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
package de.tudarmstadt.ukp.inception.externaleditor.xml;

import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.ServletContextUtils.referenceToUrl;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.fit.util.JCasUtil.selectSingle;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.ServletContext;

import org.apache.uima.cas.CAS;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.externaleditor.ExternalAnnotationEditor;
import de.tudarmstadt.ukp.inception.externaleditor.ExternalAnnotationEditorFactory;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.Cas2SaxEvents;

@ConditionalOnWebApplication
@RestController
@RequestMapping(XmlDocumentViewController.BASE_URL)
public class XmlDocumentViewControllerImpl
    implements XmlDocumentViewController
{
    private static final String GET_DOCUMENT_PATH = "/p/{projectId}/xml/{documentId}";

    private static final String DOCUMENT = "document";
    private static final String BODY = "body";

    private final DocumentService documentService;
    private final DocumentImportExportService formatRegistry;
    private final ServletContext servletContext;
    private final AnnotationEditorRegistry annotationEditorRegistry;

    @Autowired
    public XmlDocumentViewControllerImpl(DocumentService aDocumentService,
            ServletContext aServletContext, AnnotationEditorRegistry aAnnotationEditorRegistry,
            DocumentImportExportService aFormatRegistry)
    {
        documentService = aDocumentService;
        servletContext = aServletContext;
        annotationEditorRegistry = aAnnotationEditorRegistry;
        formatRegistry = aFormatRegistry;
    }

    @Override
    public String getDocumentUrl(SourceDocument aDoc)
    {
        return servletContext.getContextPath() + BASE_URL
                + GET_DOCUMENT_PATH
                        .replace("{projectId}", String.valueOf(aDoc.getProject().getId()))
                        .replace("{documentId}", String.valueOf(aDoc.getId()));
    }

    private void renderXmlDeclaration(Writer out) throws IOException
    {
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    }

    private void renderXmlStylesheet(Writer out, String aStylesheetUrl) throws IOException
    {
        out.append("<?xml-stylesheet type=\"text/css\" href=\"" + aStylesheetUrl + "\"?>\n");
    }

    private void renderEditorStylesheets(Writer out, String aEditorId) throws IOException
    {
        ExternalAnnotationEditorFactory factory = (ExternalAnnotationEditorFactory) annotationEditorRegistry
                .getEditorFactory(aEditorId);
        for (String styleSheet : factory.getDescription().getStylesheets()) {
            renderXmlStylesheet(out, ExternalAnnotationEditor.getUrlForPluginAsset(servletContext,
                    factory.getDescription(), styleSheet));
        }
    }

    @PreAuthorize("@documentAccess.canViewAnnotationDocument(#aProjectId, #aDocumentId, #principal.name)")
    @Override
    @GetMapping(path = GET_DOCUMENT_PATH)
    public ResponseEntity<String> getDocument(@PathVariable("projectId") long aProjectId,
            @PathVariable("documentId") long aDocumentId,
            @RequestParam("editor") Optional<String> aEditor, Principal principal)
        throws Exception
    {
        SourceDocument doc = documentService.getSourceDocument(aProjectId, aDocumentId);

        CAS cas = documentService.createOrReadInitialCas(doc);

        try (Writer out = new StringWriter()) {
            renderXmlDeclaration(out);

            for (String cssUrl : formatRegistry.getFormatCssStylesheets(doc).stream()
                    .map(css -> referenceToUrl(servletContext, css)).collect(toList())) {
                renderXmlStylesheet(out, cssUrl);
            }

            if (aEditor.isPresent()) {
                renderEditorStylesheets(out, aEditor.get());
            }

            var maybeXmlDocument = cas.select(XmlDocument.class).findFirst();
            if (maybeXmlDocument.isEmpty()) {
                // Gracefully handle the case that the CAS does not contain any XML structure at all
                // and show only the document text in this case.
                ContentHandler ch = XmlCas2SaxEvents.makeSerializer(out);
                ch.startDocument();
                ch.startElement(null, null, DOCUMENT, null);
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute(null, null, XmlCas2SaxEvents.DATA_CAPTURE_ROOT, null, "");
                ch.startElement(null, null, BODY, attrs);
                String text = cas.getDocumentText();
                ch.characters(text.toCharArray(), 0, text.length());
                ch.endElement(null, null, BODY);
                ch.endElement(null, null, DOCUMENT);
                ch.endDocument();
            }
            else {
                XmlDocument xml = selectSingle(cas.getJCas(), XmlDocument.class);
                Cas2SaxEvents serializer = new XmlCas2SaxEvents(xml,
                        XmlCas2SaxEvents.makeSerializer(out));
                serializer.process(xml);
            }

            return ResponseEntity.ok() //
                    .contentType(MediaType.TEXT_XML) //
                    .body(out.toString());
        }
    }
}
