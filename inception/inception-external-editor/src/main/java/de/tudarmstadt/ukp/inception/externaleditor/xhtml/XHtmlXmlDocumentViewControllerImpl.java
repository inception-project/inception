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

import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.BODY;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.HEAD;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.HTML;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.P;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.SPAN;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.XHTML_NS_URI;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Optional.ofNullable;
import static javax.xml.XMLConstants.DEFAULT_NS_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_GIF;
import static org.springframework.http.MediaType.IMAGE_JPEG;
import static org.springframework.http.MediaType.IMAGE_PNG;

import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.security.Principal;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
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
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.externaleditor.XmlDocumentViewControllerImplBase;
import de.tudarmstadt.ukp.inception.externaleditor.policy.DefaultHtmlDocumentPolicy;
import de.tudarmstadt.ukp.inception.externaleditor.policy.SafetyNetDocumentPolicy;
import de.tudarmstadt.ukp.inception.externaleditor.xml.XmlCas2SaxEvents;
import de.tudarmstadt.ukp.inception.support.wicket.ServletContextUtils;
import jakarta.servlet.ServletContext;

@ConditionalOnWebApplication
@RestController
@RequestMapping(XHtmlXmlDocumentViewController.BASE_URL)
public class XHtmlXmlDocumentViewControllerImpl
    extends XmlDocumentViewControllerImplBase
    implements XHtmlXmlDocumentViewController
{
    private static final MediaType IMAGE_SVG = MediaType.parseMediaType("image/svg+xml");

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final String GET_DOCUMENT_PATH = "/p/{projectId}/d/{documentId}/xml";
    private static final String GET_RESOURCE_PATH = "/p/{projectId}/d/{documentId}/res";

    private final DocumentService documentService;
    private final DocumentStorageService documentStorageService;
    private final DocumentImportExportService formatRegistry;
    private final ServletContext servletContext;

    @Autowired
    public XHtmlXmlDocumentViewControllerImpl(DocumentService aDocumentService,
            DocumentStorageService aDocumentStorageService,
            AnnotationEditorRegistry aAnnotationEditorRegistry, ServletContext aServletContext,
            DocumentImportExportService aFormatRegistry, DefaultHtmlDocumentPolicy aDefaultPolicy,
            SafetyNetDocumentPolicy aSafetyNetPolicy)
    {
        super(aDefaultPolicy, aSafetyNetPolicy, aFormatRegistry, aAnnotationEditorRegistry);

        documentService = aDocumentService;
        documentStorageService = aDocumentStorageService;
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
        var attr = new AttributesImpl();
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
                startXHtmlDocument(rawHandler);
                renderXmlContent(finalHandler, maybeXmlDocument.get());
                endXHtmlDocument(rawHandler);
                return toResponse(out);
            }

            startXHtmlDocument(rawHandler);

            rawHandler.startElement(null, null, HTML, null);
            renderHead(doc, rawHandler);

            if (maybeXmlDocument.isEmpty()) {
                // renderTextContent(cas, finalHandler);
                renderMarkdownContent(cas, finalHandler);
            }
            else {
                finalHandler.startElement(null, null, BODY, null);

                var formatPolicy = formatRegistry.getFormatPolicy(doc);
                var defaultNamespace = formatPolicy.flatMap(policy -> policy.getDefaultNamespace());

                if (defaultNamespace.isPresent()) {
                    finalHandler.startPrefixMapping(DEFAULT_NS_PREFIX, defaultNamespace.get());
                }

                renderXmlContent(finalHandler, maybeXmlDocument.get());

                if (defaultNamespace.isPresent()) {
                    finalHandler.endPrefixMapping(DEFAULT_NS_PREFIX);
                }

                finalHandler.endElement(null, null, BODY);
            }

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
        for (var cssUrl : formatRegistry.getFormatCssStylesheets(doc).stream()
                .map(css -> ServletContextUtils.referenceToUrl(servletContext, css)).toList()) {
            renderXmlStylesheet(ch, cssUrl);
        }
        ch.endElement(null, null, HEAD);
    }

    private void renderXmlContent(ContentHandler ch, XmlDocument aXmlDocument) throws SAXException
    {
        var serializer = new XmlCas2SaxEvents(aXmlDocument, ch);
        serializer.process(aXmlDocument.getRoot());
    }

    private void renderTextContent(CAS cas, ContentHandler ch) throws SAXException
    {
        // Gracefully handle the case that the CAS does not contain any XML structure at all
        // and show only the document text in this case.
        var atts = new AttributesImpl();
        atts.addAttribute("", "", "class", "CDATA", "i7n-plain-text-document");
        ch.startElement(null, null, BODY, atts);

        var lineAttribs = new AttributesImpl();
        lineAttribs.addAttribute("", "", "class", "CDATA", "data-i7n-tracking");

        var text = cas.getDocumentText().toCharArray();
        ch.startElement(null, null, P, null);
        ch.startElement(null, null, SPAN, lineAttribs);

        var lineBreakSequenceLength = 0;
        for (int i = 0; i < text.length; i++) {
            if (text[i] == '\n') {
                lineBreakSequenceLength++;
                ch.endElement(null, null, SPAN);
                ch.startElement(null, null, SPAN, lineAttribs);
            }
            else if (text[i] != '\r') {
                if (lineBreakSequenceLength > 1) {
                    ch.endElement(null, null, SPAN);
                    ch.endElement(null, null, P);
                    ch.startElement(null, null, P, null);
                    ch.startElement(null, null, SPAN, lineAttribs);
                }

                lineBreakSequenceLength = 0;
            }

            ch.characters(text, i, 1);
        }

        ch.endElement(null, null, SPAN);
        ch.endElement(null, null, P);

        ch.endElement(null, null, BODY);
    }

    private void renderMarkdownContent(CAS cas, ContentHandler ch) throws SAXException
    {
        var atts = new AttributesImpl();
        atts.addAttribute("", "", "class", "CDATA", "i7n-markdown-document");
        ch.startElement(null, null, BODY, atts);

        new XHtmlXmlMarkdownProcessor().process(ch, cas.getDocumentText());

        ch.endElement(null, null, BODY);
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

        var srcDocFile = documentStorageService.getSourceDocumentFile(srcDoc);

        var formatSupport = maybeFormatSupport.get();

        if (!formatSupport.hasResources()
                || !formatSupport.isAccessibleResource(srcDocFile, aResourceId)) {
            LOG.debug("Resource [{}] for document {} not found", aResourceId, srcDoc);
            return ResponseEntity.notFound().build();
        }

        try {
            var inputStream = formatSupport.openResourceStream(srcDocFile, aResourceId);
            var httpHeaders = new HttpHeaders();

            getContentType(aResourceId).ifPresent(httpHeaders::setContentType);

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

    private Optional<MediaType> getContentType(String aResourceId)
    {
        var suffix = StringUtils.substringAfterLast(aResourceId, ".");

        if (suffix == null) {
            return Optional.empty();
        }

        return ofNullable(switch (suffix) {
        case "svg" -> IMAGE_SVG;
        case "png" -> IMAGE_PNG;
        case "gif" -> IMAGE_GIF;
        case "jpg" -> IMAGE_JPEG;
        case "jpeg" -> IMAGE_JPEG;
        default -> null;
        });
    }

    private ResponseEntity<String> toResponse(StringWriter aOut)
    {
        return ResponseEntity.ok() //
                .contentType(MediaType.APPLICATION_XHTML_XML) //
                .body(aOut.toString());
    }
}
