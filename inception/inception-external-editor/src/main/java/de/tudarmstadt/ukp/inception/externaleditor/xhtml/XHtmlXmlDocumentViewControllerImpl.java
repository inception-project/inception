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
package de.tudarmstadt.ukp.inception.externaleditor.xhtml;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.OK;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Principal;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;

import org.apache.uima.cas.CAS;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.externaleditor.XmlDocumentViewControllerImplBase;
import de.tudarmstadt.ukp.inception.externaleditor.policy.DefaultHtmlDocumentPolicy;
import de.tudarmstadt.ukp.inception.externaleditor.policy.SafetyNetDocumentPolicy;
import de.tudarmstadt.ukp.inception.externaleditor.xml.XmlCas2SaxEvents;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.Cas2SaxEvents;
import de.tudarmstadt.ukp.inception.support.wicket.ServletContextUtils;
import jakarta.servlet.ServletContext;

@ConditionalOnWebApplication
@RestController
@RequestMapping(XHtmlXmlDocumentViewController.BASE_URL)
public class XHtmlXmlDocumentViewControllerImpl
    extends XmlDocumentViewControllerImplBase
    implements XHtmlXmlDocumentViewController
{
    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final String GET_DOCUMENT_PATH = "/p/{projectId}/d/{documentId}/xml";
    private static final String GET_RESOURCE_PATH = "/p/{projectId}/d/{documentId}/res";

    private static final String XHTML_NS_URI = "http://www.w3.org/1999/xhtml";
    private static final String HTML = "html";
    private static final String BODY = "body";
    private static final String HEAD = "head";
    private static final String P = "p";

    private final DocumentService documentService;
    private final DocumentImportExportService formatRegistry;
    private final ServletContext servletContext;

    @Autowired
    public XHtmlXmlDocumentViewControllerImpl(DocumentService aDocumentService,
            AnnotationEditorRegistry aAnnotationEditorRegistry, ServletContext aServletContext,
            DocumentImportExportService aFormatRegistry, DefaultHtmlDocumentPolicy aDefaultPolicy,
            SafetyNetDocumentPolicy aSafetyNetPolicy)
    {
        super(aDefaultPolicy, aSafetyNetPolicy, aFormatRegistry, aAnnotationEditorRegistry);

        documentService = aDocumentService;
        servletContext = aServletContext;
        formatRegistry = aFormatRegistry;
    }

    @Override
    public String getDocumentUrl(SourceDocument aDoc)
    {
        return servletContext.getContextPath() + BASE_URL + GET_DOCUMENT_PATH //
                .replace("{projectId}", String.valueOf(aDoc.getProject().getId()))
                .replace("{documentId}", String.valueOf(aDoc.getId()));
    }

    private void renderXmlStylesheet(ContentHandler ch, String aStylesheetUrl) throws SAXException
    {
        AttributesImpl attr = new AttributesImpl();
        attr.addAttribute(null, null, "rel", null, "stylesheet");
        attr.addAttribute(null, null, "type", null, "text/css");
        attr.addAttribute(null, null, "href", null, aStylesheetUrl);
        ch.startElement(null, null, "link", attr);
        ch.endElement(null, null, "link");
    }

    @PreAuthorize("@documentAccess.canViewAnnotationDocument(#aProjectId, #aDocumentId, #principal.name)")
    @Override
    @GetMapping(path = GET_DOCUMENT_PATH)
    public ResponseEntity<String> getDocument(@PathVariable("projectId") long aProjectId,
            @PathVariable("documentId") long aDocumentId,
            @RequestParam("editor") Optional<String> aEditor, Principal principal)
        throws Exception
    {
        var doc = documentService.getSourceDocument(aProjectId, aDocumentId);

        var cas = documentService.createOrReadInitialCas(doc);

        try (var out = new StringWriter()) {
            Optional<XmlDocument> maybeXmlDocument;
            if (cas.getTypeSystem().getType(XmlDocument._TypeName) != null) {
                maybeXmlDocument = cas.select(XmlDocument.class).findFirst();
            }
            else {
                maybeXmlDocument = Optional.empty();
            }

            var casContainsHtml = maybeXmlDocument.map(XmlDocument::getRoot) //
                    .map(XmlElement::getQName) //
                    .map(qname -> HTML.equals(qname.toLowerCase(Locale.ROOT))) //
                    .orElse(false);

            var rawHandler = XmlCas2SaxEvents.makeSerializer(out);
            var sanitizingHandler = applySanitizers(aEditor, doc, rawHandler);
            var resourceFilteringHandler = applyHtmlResourceUrlFilter(doc, sanitizingHandler);
            var finalHandler = resourceFilteringHandler;

            // If the CAS contains an actual HTML structure, then we send that. Mind that we do
            // not inject format-specific CSS then!
            if (casContainsHtml) {
                var xml = maybeXmlDocument.get();
                startXHtmlDocument(rawHandler);

                var serializer = new XmlCas2SaxEvents(xml, finalHandler);
                serializer.process(xml.getRoot());

                endXHtmlDocument(rawHandler);
                return toResponse(out);
            }

            startXHtmlDocument(rawHandler);

            rawHandler.startElement(null, null, HTML, null);

            renderHead(doc, rawHandler);

            rawHandler.startElement(null, null, BODY, null);
            if (maybeXmlDocument.isEmpty()) {
                // Gracefully handle the case that the CAS does not contain any XML structure at all
                // and show only the document text in this case.
                renderTextContent(cas, finalHandler);
            }
            else {
                var formatPolicy = formatRegistry.getFormatPolicy(doc);
                var defaultNamespace = formatPolicy.flatMap(policy -> policy.getDefaultNamespace());

                if (defaultNamespace.isPresent()) {
                    finalHandler.startPrefixMapping(XMLConstants.DEFAULT_NS_PREFIX,
                            defaultNamespace.get());
                }

                renderXmlContent(doc, finalHandler, aEditor, maybeXmlDocument.get());

                if (defaultNamespace.isPresent()) {
                    finalHandler.endPrefixMapping(XMLConstants.DEFAULT_NS_PREFIX);
                }
            }
            rawHandler.endElement(null, null, BODY);

            rawHandler.endElement(null, null, HTML);

            endXHtmlDocument(rawHandler);

            return toResponse(out);
        }
    }

    private void endXHtmlDocument(ContentHandler ch) throws SAXException
    {
        ch.endPrefixMapping("");
        ch.endDocument();
    }

    private void startXHtmlDocument(ContentHandler ch) throws SAXException
    {
        ch.startDocument();
        ch.startPrefixMapping("", XHTML_NS_URI);
    }

    private void renderHead(SourceDocument doc, ContentHandler ch) throws SAXException
    {
        ch.startElement(null, null, HEAD, null);
        for (String cssUrl : formatRegistry.getFormatCssStylesheets(doc).stream()
                .map(css -> ServletContextUtils.referenceToUrl(servletContext, css))
                .collect(Collectors.toList())) {
            renderXmlStylesheet(ch, cssUrl);
        }
        ch.endElement(null, null, HEAD);
    }

    private void renderXmlContent(SourceDocument doc, ContentHandler ch, Optional<String> aEditor,
            XmlDocument aXmlDocument)
        throws IOException, SAXException
    {
        Cas2SaxEvents serializer = new XmlCas2SaxEvents(aXmlDocument, ch);
        serializer.process(aXmlDocument.getRoot());
    }

    private void renderTextContent(CAS cas, ContentHandler ch) throws SAXException
    {
        var text = cas.getDocumentText().toCharArray();
        ch.startElement(null, null, P, null);
        var lineBreakSequenceLength = 0;
        for (int i = 0; i < text.length; i++) {
            if (text[i] == '\n') {
                lineBreakSequenceLength++;
            }
            else if (text[i] != '\r') {
                if (lineBreakSequenceLength > 1) {
                    ch.endElement(null, null, P);
                    ch.startElement(null, null, P, null);
                }

                lineBreakSequenceLength = 0;
            }

            ch.characters(text, i, 1);
        }
        ch.endElement(null, null, P);
    }

    @PreAuthorize("@documentAccess.canViewAnnotationDocument(#aProjectId, #aDocumentId, #principal.name)")
    @Override
    @GetMapping(path = GET_RESOURCE_PATH)
    public ResponseEntity<InputStreamResource> getResource(
            @PathVariable("projectId") long aProjectId,
            @PathVariable("documentId") long aDocumentId, @RequestParam("resId") String aResourceId,
            Principal principal)
        throws Exception
    {
        var srcDoc = documentService.getSourceDocument(aProjectId, aDocumentId);

        var maybeFormatSupport = formatRegistry.getFormatById(srcDoc.getFormat());
        if (maybeFormatSupport.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var srcDocFile = documentService.getSourceDocumentFile(srcDoc);

        var formatSupport = maybeFormatSupport.get();

        if (!formatSupport.hasResources()
                || !formatSupport.isAccessibleResource(srcDocFile, aResourceId)) {
            LOG.debug("Resource [{}] for document {} not found", aResourceId, srcDoc);
            return ResponseEntity.notFound().build();
        }

        try {
            var inputStream = formatSupport.openResourceStream(srcDocFile, aResourceId);
            var httpHeaders = new HttpHeaders();
            return new ResponseEntity<>(new InputStreamResource(inputStream), httpHeaders, OK);
        }
        catch (FileNotFoundException e) {
            LOG.debug("Resource [{}] for document {} not found", aResourceId, srcDoc);
            return ResponseEntity.notFound().build();
        }
        catch (Exception e) {
            LOG.debug("Unable to load resource [{}] for document {}", aResourceId, srcDoc, e);
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<String> toResponse(StringWriter aOut)
    {
        return ResponseEntity.ok() //
                .contentType(MediaType.APPLICATION_XHTML_XML) //
                .body(aOut.toString());
    }
}
